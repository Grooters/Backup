
import org.apache.commons.net.ftp.FTPClient;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Backup {
    private static String PATH="D:\\Blog\\source";
    private boolean compare;
    private PrintStream printStream;
    private BufferedReader reader;
    private List<String> changedFile;
    private String host,number,password;
    private File temp;
    private int period;
    public static void main(String args[]){
        Backup backup=new Backup();
        backup.changedFile=new ArrayList<>();
        List<String> info=backup.getConfig();
        backup.host=info.get(0);
        backup.number=info.get(1);
        backup.password=info.get(2);
        PATH=info.get(3);
        backup.period=Integer.parseInt(info.get(4));
        backup.temp=new File(System.getProperty("user.dir")+"\\temp.txt");
        try {
            backup.printStream=new PrintStream(new FileOutputStream(backup.temp));
            backup.reader=new BufferedReader(new InputStreamReader(new FileInputStream(backup.temp)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        backup.compare=false;
        Timer timer=new Timer();
        System.out.println("\n开始监控...\n");
        TimerTask task=new TimerTask() {
            @Override
            public void run() {
                backup.findFile(PATH);
                if(backup.changedFile.size()!=0){
                    backup.loadFile();
                    try {
                        backup.printStream=new PrintStream(new FileOutputStream(backup.temp));
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    backup.changedFile.clear();
                    backup.compare=false;
                }else {
                    backup.compare=true;
                    try {
                        backup.reader=new BufferedReader(new InputStreamReader(new FileInputStream(backup.temp)));
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        timer.schedule(task,0,backup.period*1000);
    }
    private void findFile(String path){
        File origin=new File(path);
        File[] files=origin.listFiles();
        if(files==null)
            return;
        for(File file:files){
            if(file.isDirectory()){
                //递归遍历文件夹
                findFile(file.getAbsolutePath());
            }else {
                //判断是否有文件发生变化
                if(compare){
                    try {
                        String str=reader.readLine();
                        if(str!=null){
                            String strs[]=str.split("\\*:");
                            String paths[]=strs[0].split("source\\\\");
                            long time=file.lastModified()-Long.parseLong(strs[1]);
                            if(time!=0){
                                System.out.println(file.getName()+"被修改了\n");
                                changedFile.add(paths[paths.length-1]);
                            }
                            //创建了新文件
                        }else {
                            printStream.append(file.getAbsolutePath()+"*:"+file.lastModified());
                            System.out.println("新文件："+file.getName()+"产生了\n");
                            changedFile.add(file.getAbsolutePath().split("source\\\\")[1]);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    //产生新的对照文件表
                }else{
                    long lastModify=file.lastModified();
                    printStream.append(file.getAbsolutePath());
                    printStream.append("*:");
                    printStream.append(String.valueOf(lastModify));
                    //append必须通过System.getProperty("line.separator")此方式实现换行
                    printStream.append(System.getProperty("line.separator"));
                }
            }
        }
    }
    private void loadFile(){
        FTPClient ftpClient=connectFTP();
        ftpClient.setControlEncoding("UTF-8"); // 中文支持
        try {
            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
            ftpClient.enterLocalPassiveMode();
            FileInputStream inputStream=null;
            for(String file:changedFile){
                ftpClient.changeWorkingDirectory(PATH+"\\"+getPath(file));
                inputStream=new FileInputStream(PATH+"\\"+file);
                String fileName=new String((file.getBytes("GBK")),StandardCharsets.ISO_8859_1);
                ftpClient.storeFile("source\\"+fileName,inputStream);

            }
            if(inputStream!=null){
                System.out.print("文件上传成功\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private FTPClient connectFTP(){
        FTPClient client=new FTPClient();
        try {
            client.connect(host,21);
            if(client.login(number,password)){
                System.out.println("连接成功\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return client;
    }
    private StringBuffer getPath(String file){
        StringBuffer path=new StringBuffer();
        String strs[]=file.split("\\\\");
        for(int i=0;i<strs.length-1;i++){
            path.append(strs[i]);
        }
        return path;
    }
    /**
     ftp_host:
     ftp_number:
     ftp_password:
     root_path:
     **/
    private List<String> getConfig(){
        File file=new File(System.getProperty("user.dir")+"\\config.ini");
        if(!file.exists()){
            System.err.println("文件丢失！");
            return null;
        }
        List<String> info=null;
        try {
            BufferedReader reader=new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String str;
            String strs[];
            info=new ArrayList<>();
            while ((str=reader.readLine())!=null){
                strs=str.split("\\*:");
                info.add(strs[1]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return info;
    }
}
