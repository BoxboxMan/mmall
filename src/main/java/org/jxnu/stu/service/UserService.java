package org.jxnu.stu.service;

import org.jxnu.stu.common.BusinessException;
import org.jxnu.stu.common.ServerResponse;
import org.jxnu.stu.controller.vo.UserVo;
import org.jxnu.stu.dao.pojo.User;
import org.jxnu.stu.service.bo.UserBo;

import javax.jws.soap.SOAPBinding;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

public interface UserService {
    UserBo login(String username, String password) throws Exception;

    void register(User user) throws BusinessException, UnsupportedEncodingException, NoSuchAlgorithmException;

    void checkValid(String str, String type) throws BusinessException;

    String forgetGetQuestion(String username) throws BusinessException;

    String forgetCheckAnswer(String username, String question, String answer, HttpSession session) throws BusinessException;

    void forgetResetPassword(String username, String newPassword, String forgetToken) throws BusinessException, UnsupportedEncodingException, NoSuchAlgorithmException;

    void resetPassword(String passwordOld, String passwordNew, HttpServletRequest request) throws BusinessException, UnsupportedEncodingException, NoSuchAlgorithmException;

    void updateInformation(User user, HttpServletRequest request) throws BusinessException;


}
