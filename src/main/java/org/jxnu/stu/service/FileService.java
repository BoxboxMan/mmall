package org.jxnu.stu.service;

import org.jxnu.stu.common.BusinessException;
import org.jxnu.stu.controller.vo.ProductVo;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public interface FileService {

    String upload(MultipartFile file, String path, HttpServletRequest request) throws BusinessException;

}
