package org.jxnu.stu.controller.backend;

import org.apache.commons.lang3.StringUtils;
import org.jxnu.stu.common.*;
import org.jxnu.stu.controller.portal.UserController;
import org.jxnu.stu.controller.vo.UserVo;
import org.jxnu.stu.service.UserService;
import org.jxnu.stu.service.bo.UserBo;
import org.jxnu.stu.util.CookieHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.concurrent.TimeUnit;

@Controller
@RequestMapping("/manage/user")
public class UserManageController {

    @Autowired
    private UserService userService;
    @Autowired
    private ValidationImpl validatior;
    @Autowired
    private UserController userController;
    @Autowired
    private RedisTemplate<String,Object> redisTemplate;

    @RequestMapping(value = "/login",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<UserVo> login(String username, String password, HttpSession session, HttpServletResponse response) throws Exception{
        if(StringUtils.isBlank(username)){
            throw new BusinessException(ReturnCode.PARAMETER_VALUE_ERROR,"用户名不能为空!");
        }
        if(StringUtils.isBlank(password)){
            throw new BusinessException(ReturnCode.PARAMETER_VALUE_ERROR,"密码不能为空!");
        }
        UserBo userBo = userService.login(username, password);
        if (userBo == null || userBo.getRole() == Constant.USER_ORDINARY) {
            throw new BusinessException(ReturnCode.USER_LOGIN_FAILED);
        }
        UserVo userVo = userController.coverUserVoFromUserBo(userBo);
        String token = session.getId();
        CookieHelper.writeLoggingToken(response,token);
        redisTemplate.opsForValue().set(token,userVo,Constant.Time.SESSION_TIME_OUT, TimeUnit.SECONDS);
        return ServerResponse.createServerResponse(ReturnCode.SUCCESS.getCode(),ReturnCode.USER_LOGIN_SUCCESS.getMsg(),userVo);
    }
}
