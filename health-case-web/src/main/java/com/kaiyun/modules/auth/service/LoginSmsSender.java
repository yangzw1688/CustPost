package com.kaiyun.modules.auth.service;

public interface LoginSmsSender {
    void checkConfiguration();

    void send(String phone, String code);
}
