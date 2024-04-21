package com.bilanee.octopus.config;

import com.bilanee.octopus.basic.TokenInterceptor;
import lombok.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

@Configuration
@EnableWebSocket
public class WebConfig implements WebMvcConfigurer{

//    @Value("${server.port}")
//    private int serverPort;
//
//    @Value("${server.http.port}")
//    private int serverHttpPort;

//    @Bean
//    public ServletWebServerFactory servletWebServerFactory() {
//        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory() {
//            @Override
//            protected void postProcessContext(Context context) {
//                SecurityConstraint securityConstraint = new SecurityConstraint();
//                securityConstraint.setUserConstraint("CONFIDENTIAL");
//                SecurityCollection collection = new SecurityCollection();
//                collection.addPattern("/*");
//                securityConstraint.addCollection(collection);
//                context.addConstraint(securityConstraint);
//            }
//        };
//        tomcat.addAdditionalTomcatConnectors(redirectConnector());
//        return tomcat;
//    }

//    private Connector redirectConnector() {
//        Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
//        connector.setScheme("http");
//        connector.setPort(serverHttpPort);
//        connector.setSecure(false);
//        connector.setRedirectPort(serverPort);
//        return connector;
//    }


    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/**")
                .allowCredentials(true)
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "HEAD", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("*");
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(new TokenInterceptor("token"))
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/user/login")
                .excludePathPatterns("/api/user/adminLogin");

        registry.addInterceptor(new TokenInterceptor("adminToken"))
                .addPathPatterns("/manage/**")
                .excludePathPatterns("/manage/listUserVOs");
    }




//    @Override
//    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
//        registry.addResourceHandler("/**").addResourceLocations("file:C:/Users/Administrator/octopus/static/").addResourceLocations("classpath:static/");
//    }

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**").addResourceLocations("file:/home/sjtu/octopus/static");
    }


    @Bean
    public ServerEndpointExporter serverEndpointExporter()  {
        return new ServerEndpointExporter();
    }

//    @Bean
//    public TomcatContextCustomizer tomcatConnectorCustomizer() {
//        return context -> context.addServletContainerInitializer(new WsSci(), null);
//    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}