package org.jxnu.stu;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hello world!
 *
 */
@SpringBootApplication(scanBasePackages = "org.jxnu.stu")
@MapperScan(basePackages = "org.jxnu.stu.dao")
@RestController
public class App {

//    @Override
//    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
//        return builder.sources(App.class);
//    }

    public static void main(String[] args )
    {
        System.out.println( "Hello World!" );
        SpringApplication.run(App.class,args);
    }


}
