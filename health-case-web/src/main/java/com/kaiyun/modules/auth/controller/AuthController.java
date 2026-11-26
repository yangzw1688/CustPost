package com.kaiyun.modules.auth.controller;

import com.kaiyun.common.web.Result;
import com.kaiyun.modules.auth.service.LoginService;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final LoginService loginService;
    public AuthController(LoginService loginService) {
        this.loginService = loginService;
    }

    public static class LoginRequest {
        private String phone;
        private String code;
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
    }

    @PostMapping("/send-code")
    public Result<Map<String, Object>> sendCode(@RequestBody LoginRequest input,
            HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return Result.success(loginService.sendCode(input.getPhone(), request.getRemoteAddr()));
    }

    @PostMapping("/login")
    public Result<Map<String, String>> login(@RequestBody LoginRequest input,
            HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        loginService.validateCredentials(input.getPhone(), input.getCode());
        loginService.login(input.getPhone(), request);
        return Result.success(loginService.currentUser(request));
    }

    @GetMapping("/me")
    public Result<Map<String, String>> me(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return Result.success(loginService.currentUser(request));
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        loginService.logout(request);
        return Result.success();
    }
}
