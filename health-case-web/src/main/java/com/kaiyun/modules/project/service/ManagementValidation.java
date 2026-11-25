package com.kaiyun.modules.project.service;

import com.kaiyun.common.exception.BusinessException;
import java.net.URI;
import java.util.Arrays;

final class ManagementValidation {
    private ManagementValidation() { }
    static String text(String value, int max) {
        String text = value == null ? "" : value.trim();
        if (text.length() > max || text.chars().anyMatch(ch -> ch < 32 && ch != '\n' && ch != '\t')) throw bad("填写内容超出长度或包含无效字符");
        return text;
    }
    static String required(String value, int max, String label) {
        String text = text(value, max);
        if (text.isEmpty()) throw bad("请填写" + label);
        return text;
    }
    static String code(String value) {
        String code = text(value, 40);
        if (!code.matches("[A-Za-z][A-Za-z0-9_-]{0,39}")) throw bad("编码须以字母开头，仅支持字母、数字、横线和下划线");
        return code;
    }
    static String option(String value, String... choices) {
        if (!Arrays.asList(choices).contains(value)) throw bad("请选择有效的状态或配置选项");
        return value;
    }
    static String phone(String value) {
        String phone = text(value, 32);
        if (!phone.isEmpty() && !phone.matches("[0-9+() -]{5,32}")) throw bad("联系电话格式不正确");
        return phone;
    }
    static void version(Long expected, Long actual) {
        if (expected == null || !expected.equals(actual)) throw new BusinessException(409, "数据已被修改，请刷新后重新编辑");
    }
    static String environment(String value) { return option(value, "TEST", "PROD"); }
    static String endpoint(String value) {
        String endpoint = text(value, 512);
        if (endpoint.isEmpty()) return endpoint;
        try {
            URI uri = URI.create(endpoint);
            if ("https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null && uri.getUserInfo() == null
                    && uri.getRawQuery() == null && uri.getFragment() == null
                    && endpoint.chars().noneMatch(Character::isWhitespace)) return endpoint;
        } catch (IllegalArgumentException ignored) { }
        throw bad("接收地址须为 HTTPS 地址，不能在地址中填写账号、令牌、查询参数或锚点");
    }
    static BusinessException bad(String message) { return new BusinessException(400, message); }
}
