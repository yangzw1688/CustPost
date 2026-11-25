package com.kaiyun.infrastructure.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import javax.servlet.http.HttpServletRequest;

@Controller
public class PageController {
    /** 根路径需要显式转发，深层页面由 SpaResourceResolver 处理。 */
    @GetMapping("/")
    public String home() {
        return "forward:/index.html";
    }

    @GetMapping({"/admin", "/admin/"})
    public String admin(HttpServletRequest request) {
        // Spring 兼容尾斜杠匹配，区分两种地址以避免循环重定向。
        return request.getRequestURI().endsWith("/")
                ? "forward:/admin/index.html" : "redirect:/admin/";
    }
}
