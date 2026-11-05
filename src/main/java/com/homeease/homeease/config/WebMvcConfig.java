package com.homeease.homeease.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private CacheControlInterceptor cacheControlInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Apply cache control headers to all authenticated pages
        registry.addInterceptor(cacheControlInterceptor)
                .addPathPatterns("/admin/**", "/user/**", "/worker/**");
    }

    @Override
    public void addResourceHandlers(@org.springframework.lang.NonNull org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/ProfilePhoto/**")
                .addResourceLocations("file:" + System.getProperty("user.dir") + "/ProfilePhoto/");
        
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}
