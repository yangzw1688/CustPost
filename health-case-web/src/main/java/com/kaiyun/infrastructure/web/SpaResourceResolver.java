package com.kaiyun.infrastructure.web;

import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/** 支持 Vue 深层路由刷新；缺失的接口和静态文件继续返回 404。 */
public class SpaResourceResolver extends PathResourceResolver {
    @Override
    protected Resource getResource(String resourcePath, Resource location) throws IOException {
        // 优先返回真实文件，只有页面路由才能回退到对应的 index.html。
        Resource resource = super.getResource(resourcePath, location);
        if (resource != null) return resource;

        if (resourcePath.startsWith("admin/") && !resourcePath.contains(".")
                && !resourcePath.matches("admin/(js|css|img|fonts)(/.*)?")) {
            return super.getResource("admin/index.html", location);
        }
        if (resourcePath.matches("(login|submit|service-record|mine|policy|page-preview)/?")
                || resourcePath.matches("pages/[a-z][a-z0-9-]{0,47}/?")
                || resourcePath.matches("service-detail/[0-9]+/?")) {
            return super.getResource("index.html", location);
        }
        return null;
    }
}
