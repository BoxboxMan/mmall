package org.jxnu.stu.config;

import org.jxnu.stu.controller.common.LoginIntercept;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class MvcConfig extends WebMvcConfigurationSupport {

    @Autowired
    private LoginIntercept loginIntercept;

    @Override
    protected void addInterceptors(InterceptorRegistry registry) {
        List<String> patterns = new ArrayList<>();
        patterns.add("/shipping/**");
        patterns.add("/order/**");
        patterns.add("/cart/**");
        patterns.add("/user/get_user_info");
        patterns.add("/user/reset_password");
        patterns.add("/user/update_information");
        patterns.add("/user/get_information");
        patterns.add("/user/logout");
        patterns.add("/manage/**");
        List<String> excludePatterns = new ArrayList<>();
        excludePatterns.add("/order/alipay_callback");
        excludePatterns.add("/manage/user/login");
        excludePatterns.add("/user/login");
        registry.addInterceptor(loginIntercept).addPathPatterns(patterns).excludePathPatterns(excludePatterns);
        super.addInterceptors(registry);
    }
}
