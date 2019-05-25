package org.jxnu.stu.config;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

//当Spring容器内没有TomcatEmbeddedServletContainerFactory这个bean的时候，会把此bean加载进spring
//SpringBoot2.1.4当前是没有的，所以一定会加载
@Component
public class WebServerConfiguration implements WebServerFactoryCustomizer<ConfigurableWebServerFactory> {
    @Override
    public void customize(ConfigurableWebServerFactory factory) {
        //使用工厂类提供的接口定制化我们的tomcat connecter
        ((TomcatServletWebServerFactory)factory).addConnectorCustomizers(new TomcatConnectorCustomizer() {
            @Override
            public void customize(Connector connector) {
                Http11NioProtocol protocolHandler = (Http11NioProtocol) connector.getProtocolHandler();
                //定制化最长空闲时间为30秒
                protocolHandler.setKeepAliveTimeout(30 * 1000);
                //定制化客户端发送超过10000个请求的时候断开keep-alive连接
                protocolHandler.setMaxKeepAliveRequests(10000);
            }
        });
    }
}
