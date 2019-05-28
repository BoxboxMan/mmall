package org.jxnu.stu.util;

import lombok.extern.slf4j.Slf4j;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

@Slf4j
public class Base64Helper {

    private static BASE64Encoder encoder = new BASE64Encoder();
    private static BASE64Decoder decoder = new BASE64Decoder();

    /**
     * 图片转换为二进制流
     * @param targetFile 图片文件
     * @return
     */
    public static String image2Binary(File targetFile){
        String targetFileName = targetFile.getName();
        String suffix = targetFileName.substring(targetFileName.lastIndexOf(".") - 1);
        BufferedImage bufferedImage;
        try {
            bufferedImage = ImageIO.read(targetFile);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage,suffix,outputStream);
            byte[] bytes = outputStream.toByteArray();
            return new String(bytes);
        } catch (IOException e) {
            e.printStackTrace();
            log.info("图片转换二进码失败！",e);
        }
        return null;
    }


    /**
     * 二进制转换为图片,jpg格式
     * @param imageBinary 图片的二进制流
     * @param fileDir   生成的图片所处的文件夹
     * @param fileName  生成图片的名字
     * @return
     */
    public static File binary2Image(String imageBinary,File fileDir,String fileName){
        try {
            byte[] bytes = decoder.decodeBuffer(imageBinary);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            BufferedImage bufferedImage = ImageIO.read(inputStream);
            File resFile = new File(fileDir,fileName);
            ImageIO.write(bufferedImage,"jpg",resFile);
            return resFile;
        } catch (IOException e) {
            e.printStackTrace();
            log.info("二进制码转换图片失败！",e);
        }
        return null;
    }

}
