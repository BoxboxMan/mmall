package org.jxnu.stu.util;

import org.jxnu.stu.common.BusinessException;
import org.jxnu.stu.common.CommonReturn;
import org.jxnu.stu.common.ReturnCode;
import sun.misc.BASE64Encoder;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Md5Helper {

    public static String encode(String password) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        if(password == null){
            return null;
        }
        //确定加密算法
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        BASE64Encoder base64Encoder = new BASE64Encoder();
        //进行加密
        String newPassword = base64Encoder.encode(md5.digest(password.getBytes("UTF-8")));
        return newPassword;
    }
}
