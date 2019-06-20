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

    /**
     * 用户登陆
     * @param username
     * @param password
     * @param session
     * @param response
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/login",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<UserVo> login(String username, String password, HttpSession session, HttpServletResponse response,HttpServletRequest request) throws Exception {
        if(StringUtils.isBlank(username)){
            throw new BusinessException(ReturnCode.PARAMETER_VALUE_ERROR,"用户名不能为空");
        }
        if(StringUtils.isBlank(password)){
            throw new BusinessException(ReturnCode.PARAMETER_VALUE_ERROR,"密码不能为空");
        }
        UserBo userBo = userService.login(username, password);
        UserVo userVo = coverUserVoFromUserBo(userBo);
       // CookieHelper.delLoggingToken(request,response);//确保每次登陆的时候都写入的是最新的cookie
        CookieHelper.writeLoggingToken(response, session.getId());//写入客户端
        redisTemplate.opsForValue().set(session.getId(),userVo,Constant.Time.SESSION_TIME_OUT, TimeUnit.SECONDS);//写入redis
        return ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode(),ReturnCode.USER_LOGIN_SUCCESS.getMsg(),userVo);
    }

    /**
     * 新用户注册
     * @param user
     * @return
     * @throws BusinessException
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
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

    /**
     * 校验用户的用户名、或者邮箱是否重复
     * @param str
     * @param type
     * @return
     * @throws BusinessException
     */
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

    /**
     * 获取当前登陆用户信息
     * @param request
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/get_user_info",method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<UserVo> getUserInfo(HttpServletRequest request) throws Exception {
        UserVo userVo = (UserVo) redisTemplate.opsForValue().get(CookieHelper.readLoggingToken(request));
        return ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode(),userVo);
    }

    /**
     * 根据用户名获取密保问题
     * @param username
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/forget_get_question",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> forgetGetQuestion(String username) throws Exception {
        if(StringUtils.isBlank(username)){
            throw new BusinessException(ReturnCode.PARAMETER_VALUE_ERROR,"请输入用户名");
        }
        String question = userService.forgetGetQuestion(username);
        return ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode(),null,question);
    }

    /**
     * 校验指定用户所对应的密保的答案是否正确。
     * @param username
     * @param question
     * @param answer
     * @param session
     * @return
     * @throws Exception
     */
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

    /**
     * 密保答案校验通过后进行该用户的密码重置，注意：重置的用户账号必须为发起忘记密码流程的用户。
     * @param username
     * @param newPassword
     * @param forgetToken
     * @return
     * @throws Exception
     */
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

    /**
     * 用户在登陆状态下重置密码
     * @param passwordOld
     * @param passwordNew
     * @param request
     * @return
     * @throws Exception
     */
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

    /**
     * 更新当前登陆用户的个人信息
     * @param user
     * @param request
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/update_information",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> updateInformation(User user,HttpServletRequest request) throws Exception {
        userService.updateInformation(user,request);
        return ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode(),"更新个人信息成功");
    }

    /**
     * 获取当前登陆用户个人信息
     * @param request
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/getInformation",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<UserVo> getInformation(HttpServletRequest request) throws Exception{
        UserVo userVo = (UserVo) redisTemplate.opsForValue().get(CookieHelper.readLoggingToken(request));
        return ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode(),userVo);
    }

    /**
     * 当前登陆用户退出登录
     * @param request
     * @param response
     * @return
     * @throws BusinessException
     */
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
