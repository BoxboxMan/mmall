package org.jxnu.stu.util;

import org.apache.commons.net.ftp.FTPClient;
import org.jxnu.stu.common.BusinessException;
import org.jxnu.stu.common.ReturnCode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

public class FTPHelper {

    private static String ftpIp = PropertiesHelper.getProperties("ftp.server.ip");

    private static String ftpUser = PropertiesHelper.getProperties("ftp.username");;

    private static String ftpPassword = PropertiesHelper.getProperties("ftp.password");;

    private static int ftpPort = Integer.valueOf(PropertiesHelper.getProperties("ftp.ftpPort"));

    private String ip;
    private int port;
    private String user;
    private String pwd;
    private FTPClient ftpClient;

    public FTPHelper(){

    }
    public FTPHelper(String ip,int port,String user,String pwd){
        this.ip = ip;
        this.port = port;
        this.user = user;
        this.pwd = pwd;
    }

    public static boolean uploadFile(List<File> fileList) throws IOException, BusinessException {
        FTPHelper ftpHelper = new FTPHelper(ftpIp,ftpPort,ftpUser,ftpPassword);
        boolean result = ftpHelper.uploadFile("img", fileList);
        return result;
    }

    private boolean uploadFile(String remotePath,List<File> fileList) throws IOException, BusinessException {
        boolean uploaded = false;
        FileInputStream fileInputStream = null;
        //连接ftp服务器
        if(connectServer(this.ip,this.port,this.user,this.pwd)){
            try {
                ftpClient.changeWorkingDirectory(remotePath);//更改ftp文件目录
                ftpClient.setBufferSize(1024);//设置缓冲大小
                ftpClient.setControlEncoding("UTF-8");//设置编码方式
                ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);//设置文件格式为二进制防止乱码
                ftpClient.enterLocalPassiveMode();//开启本地的被动模式
                for(File file:fileList){
                    fileInputStream = new FileInputStream(file);
                    ftpClient.storeFile(file.getName(),fileInputStream);
                }
                uploaded = true;
            } catch (IOException e) {
                System.out.println("IO异常");
                e.printStackTrace();
            }finally {
                fileInputStream.close();
                ftpClient.disconnect();
            }
        }
        return uploaded;
    }

    private boolean connectServer(String ip,int port,String user,String password) throws BusinessException {
        ftpClient = new FTPClient();
        boolean connectSuccess = false;
        try {
            ftpClient.connect(ip);
            connectSuccess = ftpClient.login(user,password);
        } catch (IOException e) {
            System.out.println("连接服务器失败");
            e.printStackTrace();
            throw new BusinessException(ReturnCode.ERROR,"连接服务器失败");
        }
        return connectSuccess;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public FTPClient getFtpClient() {
        return ftpClient;
    }

    public void setFtpClient(FTPClient ftpClient) {
        this.ftpClient = ftpClient;
    }
}
