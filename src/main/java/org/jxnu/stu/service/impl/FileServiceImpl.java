package org.jxnu.stu.service.impl;

import com.google.common.collect.Lists;
import org.jxnu.stu.common.BusinessException;
import org.jxnu.stu.common.Constant;
import org.jxnu.stu.common.ReturnCode;
import org.jxnu.stu.controller.vo.ProductVo;
import org.jxnu.stu.controller.vo.UserVo;
import org.jxnu.stu.service.FileService;
import org.jxnu.stu.util.Base64Helper;
import org.jxnu.stu.util.CookieHelper;
import org.jxnu.stu.util.DateTimeHelper;
import org.jxnu.stu.util.FTPHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class FileServiceImpl implements FileService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public String upload(MultipartFile file, String path, HttpServletRequest request) throws BusinessException{
        String originalFilename = file.getOriginalFilename();
        String extensionName = originalFilename.substring(originalFilename.lastIndexOf("."));
        //上传文件名拼接逻辑：当前时间+用户id
        String now = DateTimeHelper.dateToString(new Date()).replaceAll(" ","").replaceAll("-","").replaceAll(":","");
        String loggingToken = CookieHelper.readLoggingToken(request);
        UserVo userVo = (UserVo) redisTemplate.opsForValue().get(loggingToken);
        String userId = String.valueOf(userVo.getId());
        String uploadFileName = now + userId + extensionName;
        File fileDir = new File(path);
        if(!fileDir.exists()){
            fileDir.setWritable(true);//设置写权限
            fileDir.mkdirs();
        }
        File targetFile = new File(path,uploadFileName);
        try {
            file.transferTo(targetFile);
            //此时上传成功，然后需要异步上传到 vsftp
            FTPHelper.uploadFile(Lists.newArrayList(targetFile));
            //上传完成之后要删除该文件夹下的文件
            targetFile.delete();
        } catch (IOException e) {
            e.printStackTrace();
            throw new BusinessException(ReturnCode.ERROR,"上传文件到服务器传输异常");
        }
        return targetFile.getName();
    }

    @Override
    public String uploadImgByBinary(String imageBinary, String path, HttpServletRequest request) throws BusinessException {
        String now = DateTimeHelper.dateToString(new Date()).replaceAll("-", "").replaceAll(" ", "").replaceAll(".", "");
        String loggingToken = CookieHelper.readLoggingToken(request);
        UserVo userVo = (UserVo) redisTemplate.opsForValue().get(loggingToken);
        String userId = String.valueOf(userVo.getId());
        String uploadFileName = now + userId + ".img";
        File fileDir = new File(path);
        if(!fileDir.exists()){
            fileDir.setWritable(true);
            fileDir.mkdirs();
        }
        File targetFile = Base64Helper.binary2Image(imageBinary,fileDir,uploadFileName);
        if(targetFile == null){
            throw new BusinessException(ReturnCode.PRODUCT_IMAGE_UPLOAD_FAILD);
        }
        List<File> uploadFileList = new ArrayList<>();
        uploadFileList.add(targetFile);
        try {
            //上传文件
            FTPHelper.uploadFile(uploadFileList);
            //上传完成后删除本地文件
            targetFile.delete();
        } catch (IOException e) {
            e.printStackTrace();
            throw new BusinessException(ReturnCode.ERROR,"上传文件到服务器传输异常");
        }
        return targetFile.getName();
    }

}
