package org.jxnu.stu.util;

import com.alibaba.druid.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
public class CookieHelper {

    private final static String COOKIE_NAME = "mmall_login_token";
    private final static String COOKIE_DOMAIN = "awei.com";//写在顶级域名才可以被二级域名读取，同级域名是无法读取的

    public static void writeLoggingToken(HttpServletResponse response,String token){
        Cookie cookie = new Cookie(COOKIE_NAME,token);
        cookie.setDomain(COOKIE_DOMAIN);
        cookie.setPath("/");//这里设置的根目录及其子目录的请求都可以访问这个cookie
        cookie.setMaxAge(60 * 60 * 24 * 30);//设置这个cookie存活时间为30天
        cookie.setHttpOnly(true);//防止脚本获取cookie，提高一定的安全系数
        log.info("write cookieName:{} cookieValue:{}",cookie.getName(),cookie.getValue());
        response.addCookie(cookie);
    }

    public static String readLoggingToken(HttpServletRequest request){
        Cookie[] cookies = request.getCookies();
        if(cookies != null){
            for(Cookie cookie:cookies){
                if(StringUtils.equals(cookie.getName(),COOKIE_NAME)){//如果有一个cookie的名字是mmall_login_toke则返回它的value用于redis查询
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    public static void delLoggingToken(HttpServletRequest request,HttpServletResponse response){
        Cookie[] cookies = request.getCookies();
        if(cookies == null){
            return;
        }
        for(Cookie cookie:cookies){
            if(StringUtils.equals(cookie.getName(),COOKIE_NAME)){
                cookie.setPath("/");
                cookie.setDomain(COOKIE_DOMAIN);
                cookie.setMaxAge(0);
                log.info("delete Cookie! cookieName:{},cookieValue:{}",cookie.getName(),cookie.getValue());
                response.addCookie(cookie);
                return;
            }
        }
    }

}
