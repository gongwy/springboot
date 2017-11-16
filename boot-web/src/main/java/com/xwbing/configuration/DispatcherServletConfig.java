package com.xwbing.configuration;

import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import com.xwbing.handler.LoginInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * 说明: servlet配置
 * 项目名称: boot-module-demo
 * 创建时间: 2017/5/10 16:36
 * 作者:  xiangwb
 */
@Configuration
@EnableWebMvc
//@ComponentScan(basePackages = {"com.xwbing.controller"}, includeFilters = {@ComponentScan.Filter(type = FilterType.ANNOTATION, value = RestController.class)})
public class DispatcherServletConfig extends WebMvcConfigurerAdapter {
    private final Logger logger = LoggerFactory.getLogger(DispatcherServletConfig.class);

    /***
     * 添加拦截器
     * @param registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //拦截器1:登录判断
        registry.addInterceptor(new LoginInterceptor()).addPathPatterns("/**");
//        registry.addInterceptor(new LoginInterceptor())
//                .addPathPatterns("/**").excludePathPatterns("/user/login", "/servlet/captchaCode", "/swagger-ui.html");
        //拦截器2...
        super.addInterceptors(registry);
    }

    /**
     * 配置静态访问资源
     * 默认:
     * classpath:/static
     * classpath:/public
     * classpath:/resources
     * classpath:/META-INF/resources
     *
     * @param registry
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        //swagger
        registry.addResourceHandler("swagger-ui.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
        //
        super.addResourceHandlers(registry);
    }

    /**
     * 扩展消息转换器，增加fastjson
     *
     * @param converters
     */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(getFastJsonHttpMessageConverter());
        super.configureMessageConverters(converters);
    }

    /**
     * 请求处理程序映射适配器,使用fastJson
     *
     * @return
     */
    @Bean
    public HttpMessageConverter getFastJsonHttpMessageConverter() {
        FastJsonHttpMessageConverter httpMessageConverter = new FastJsonHttpMessageConverter();
        List<MediaType> mediaTypes = new ArrayList<>();
        mediaTypes.add(MediaType.TEXT_HTML);//避免IE出现下载JSON文件的情况
        mediaTypes.add(MediaType.APPLICATION_JSON);
        mediaTypes.add(MediaType.APPLICATION_FORM_URLENCODED);
        mediaTypes.add(MediaType.APPLICATION_OCTET_STREAM);
        httpMessageConverter.setSupportedMediaTypes(mediaTypes);
        return httpMessageConverter;
    }
}
