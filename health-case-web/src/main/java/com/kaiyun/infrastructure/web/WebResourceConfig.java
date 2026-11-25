package com.kaiyun.infrastructure.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/** Compatibility mapping for attachments uploaded before OBS was introduced. */
@Configuration
public class WebResourceConfig implements WebMvcConfigurer {
    private final String uploadPath;

    public WebResourceConfig(@Value("${file.upload-path:D:/work/upload/}") String uploadPath) {
        this.uploadPath = uploadPath;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.noCache())
                .resourceChain(false)
                .addResolver(new SpaResourceResolver());
        String location = Paths.get(uploadPath).toAbsolutePath().normalize().toUri().toString();
        if (!location.endsWith("/")) location += "/";
        registry.addResourceHandler("/upload/case/**").addResourceLocations(location);
    }
}
