package org.jxnu.stu.controller.common;

import com.alibaba.druid.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.jxnu.stu.common.BusinessException;
import org.jxnu.stu.common.Constant;
import org.jxnu.stu.common.ReturnCode;
import org.jxnu.stu.common.ServerResponse;
import org.jxnu.stu.controller.vo.UserVo;
import org.jxnu.stu.util.CookieHelper;
import org.jxnu.stu.util.JsonHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

@Slf4j
@Component
public class LoginIntercept implements HandlerInterceptor {

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 全局拦截器，登录及权限校验，以及请求参数展示
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURL = request.getRequestURL().toString();
        //封装请求方法名和请求参数
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        String methodName = handlerMethod.getMethod().getName();
        String className = handlerMethod.getBean().getClass().getSimpleName();
        Map<String, String[]> parameterMap = request.getParameterMap();
        Iterator<String> iterator = parameterMap.keySet().iterator();
        StringBuilder parameters = new StringBuilder();
        while (iterator.hasNext()){
            String key = iterator.next();
            String[] values = parameterMap.get(key);
            String value = Arrays.toString(values);
            parameters.append(key+"="+value+";");
        }
        log.info("请求的URL为{},请求的类名为{},请求的方法名为{},请求的参数为{}",requestURL,className,methodName,parameters.toString());
        //获取当前用户信息
        String loggingToken = CookieHelper.readLoggingToken(request);
        UserVo userVo = null;
        if(!StringUtils.isEmpty(loggingToken)){
            userVo = (UserVo) redisTemplate.opsForValue().get(loggingToken);
        }
        //用户未登录，或者为后台请求，则必须为管理员身份
        if(userVo == null || (requestURL.matches("/manage") && userVo.getRole() != Constant.USER_ADMIN)){
            //设置PrintWriter
            response.reset();
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json;charset=UTF-8");
            PrintWriter writer = response.getWriter();
            if(userVo == null){
                log.info("用户未登录！");
                writer.print(JsonHelper.obj2string(ServerResponse.createServerResponse(ReturnCode.USER_NOT_LOGIN.getCode(),ReturnCode.USER_NOT_LOGIN.getMsg())));
            }else if(requestURL.matches("/manage") && userVo.getRole() != Constant.USER_ADMIN){
                log.info("用户ID:{},无权限访问",userVo.getId());
                writer.print(JsonHelper.obj2string(ServerResponse.createServerResponse(ReturnCode.USER_HAS_NO_PERMISSION.getCode(),ReturnCode.USER_HAS_NO_PERMISSION.getMsg())));
            }
            writer.flush();
            writer.close();
            return false;
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
    }
}
