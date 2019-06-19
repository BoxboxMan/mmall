package org.jxnu.stu.controller.portal;

import org.apache.commons.lang3.StringUtils;
import org.jxnu.stu.common.*;
import org.jxnu.stu.controller.vo.UserVo;
import org.jxnu.stu.dao.UserMapper;
import org.jxnu.stu.dao.pojo.User;
import org.jxnu.stu.service.UserService;
import org.jxnu.stu.service.bo.UserBo;
import org.jxnu.stu.util.CookieHelper;
import org.jxnu.stu.util.DateTimeHelper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Controller
@RequestMapping("/user")
@CrossOrigin(allowCredentials = "true")
public class UserController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private ValidationImpl validator;

    @Autowired
    private RedisTemplate redisTemplate;

    @RequestMapping(value = "/login",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<UserVo> login(String username, String password, HttpSession session, HttpServletResponse response) throws Exception {
        if(StringUtils.isBlank(username)){
            throw new BusinessException(ReturnCode.PARAMETER_VALUE_ERROR,"用户名不能为空");
        }
        if(StringUtils.isBlank(password)){
            throw new BusinessException(ReturnCode.PARAMETER_VALUE_ERROR,"密码不能为空");
        }
        UserBo userBo = userService.login(username, password);
        UserVo userVo = coverUserVoFromUserBo(userBo);
        CookieHelper.writeLoggingToken(response, session.getId());//写入客户端
        redisTemplate.opsForValue().set(session.getId(),userVo,Constant.Time.SESSION_TIME_OUT, TimeUnit.SECONDS);//写入redis
        return ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode(),ReturnCode.USER_LOGIN_SUCCESS.getMsg(),userVo);
    }

    @RequestMapping(value = "/register",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> register(@Valid User user) throws BusinessException, NoSuchAlgorithmException, UnsupportedEncodingException {
        ValidationResult validationResult = validator.validate(user);
        if(validationResult.isHasError()){
            throw new BusinessException(ReturnCode.PARAMETER_VALUE_ERROR,validationResult.getErrMsg());
        }
        userService.register(user);
        return ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode(),"注册成功！");
    }

    @RequestMapping(value = "/check_valid",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> checkValid(String str,String type) throws BusinessException {
        if(StringUtils.isBlank(str)){
            throw new BusinessException(ReturnCode.PARAMETER_VALUE_ERROR,"用户名不能为空");
        }
        if(StringUtils.isBlank(type)){
            throw new BusinessException(ReturnCode.PARAMETER_VALUE_ERROR,"用户名类型不能为空");
        }
        userService.checkValid(str,type);
        return ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode());
    }

    @RequestMapping(value = "/get_user_info",method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<UserVo> getUserInfo(HttpServletRequest request) throws Exception {
        UserVo userVo = (UserVo) redisTemplate.opsForValue().get(CookieHelper.readLoggingToken(request));
        return ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode(),userVo);
    }

    @RequestMapping(value = "/forget_get_question",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> forgetGetQuestion(String username) throws Exception {
        if(StringUtils.isBlank(username)){
            throw new BusinessException(ReturnCode.PARAMETER_VALUE_ERROR,"请输入用户名");
        }
        String question = userService.forgetGetQuestion(username);
        return ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode(),null,question);
    }

    @RequestMapping(value = "/forget_check_answer",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> forgetCheckAnswer(String username,String question,String answer,HttpSession session) throws Exception{
        if(StringUtils.isBlank(username)){
            throw new BusinessException(ReturnCode.PARAMETER_VALUE_ERROR,"用户名不能为空");
        }
        if(StringUtils.isBlank(question)){
            throw new BusinessException(ReturnCode.PARAMETER_VALUE_ERROR,"密保问题不能为空");
        }
        if(StringUtils.isBlank(answer)){
            throw new BusinessException(ReturnCode.PARAMETER_VALUE_ERROR,"密保回答不能为空");
        }
        String token = userService.forgetCheckAnswer(username, question, answer, session);
        return ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode(),null,token);
    }

    @RequestMapping(value = "/forget_reset_password",method = RequestMethod.POST)
    @ResponseBody
        public ServerResponse<String> forgetResetPassword(String username,String newPassword,String forgetToken) throws Exception {
        if(StringUtils.isBlank(username)){
            throw new BusinessException(ReturnCode.PARAMETER_VALUE_ERROR,"用户名不能为空!");
        }
        if(StringUtils.isBlank(newPassword)){
            throw new BusinessException(ReturnCode.PARAMETER_VALUE_ERROR,"密保问题不能为空");
        }
        if(StringUtils.isBlank(forgetToken)){
            throw new BusinessException(ReturnCode.PARAMETER_VALUE_ERROR,"forgetToken为空");
        }
        userService.forgetResetPassword(username,newPassword,forgetToken);
        return ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode(),"修改密码成功");
    }

    @RequestMapping(value = "/reset_password",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> resetPassword(String passwordOld,String passwordNew,HttpServletRequest request) throws Exception {
        if(StringUtils.isBlank(passwordOld)){
            throw new BusinessException(ReturnCode.PARAMETER_VALUE_ERROR,"旧密码不能为空");
        }
        if(StringUtils.isBlank(passwordOld)){
            throw new BusinessException(ReturnCode.PARAMETER_VALUE_ERROR,"新密码不能为空");
        }
        userService.resetPassword(passwordOld,passwordNew,request);
        return ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode(),"修改密码成功");
    }

    @RequestMapping(value = "/update_information",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> updateInformation(User user,HttpServletRequest request) throws Exception {
        userService.updateInformation(user,request);
        return ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode(),"更新个人信息成功");
    }

    @RequestMapping(value = "/getInformation",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<UserVo> getInformation(HttpServletRequest request) throws Exception{
        UserVo userVo = (UserVo) redisTemplate.opsForValue().get(CookieHelper.readLoggingToken(request));
        return ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode(),userVo);
    }

    @RequestMapping(value = "/logout",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> logout(HttpServletRequest request,HttpServletResponse response) throws BusinessException {
        String loggingToken = CookieHelper.readLoggingToken(request);
        if(redisTemplate.delete(loggingToken)){
            CookieHelper.delLoggingToken(request,response);
            return ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode(),"退出成功");
        }
        return ServerResponse.createServerResponse(ReturnCode.ERROR.getCode(),"服务器异常");
    }

    public UserVo coverUserVoFromUserBo(UserBo userBo) throws BusinessException {
        if(userBo == null){
            return null;
        }
        UserVo userVo = new UserVo();
        BeanUtils.copyProperties(userBo,userVo);
        userVo.setCreateTime(DateTimeHelper.dateToString(userBo.getCreateTime()));
        userVo.setUpdateTime(DateTimeHelper.dateToString(userBo.getUpdateTime()));
        return userVo;
    }

}
