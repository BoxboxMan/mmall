package org.jxnu.stu.controller.common;

import com.alibaba.druid.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.jxnu.stu.common.Constant;
import org.jxnu.stu.controller.vo.UserVo;
import org.jxnu.stu.util.CookieHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.CrossOrigin;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@WebFilter( urlPatterns = "/*",filterName = "sessionExpireFilter")
@CrossOrigin(allowCredentials = "true", origins = "*")
public class SessionExpireFilter implements Filter {

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException { }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
//        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        String loggingToken = CookieHelper.readLoggingToken(httpServletRequest);
        if(!StringUtils.isEmpty(loggingToken)){
            UserVo userVo = (UserVo) redisTemplate.opsForValue().get(loggingToken);
            if(userVo != null){
                redisTemplate.expire(loggingToken, Constant.Time.SESSION_TIME_OUT, TimeUnit.SECONDS);
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() { }
}
