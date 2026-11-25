package com.kaiyun.modules.ui.service;

import com.kaiyun.common.exception.BusinessException;
import com.kaiyun.modules.ui.dto.UiSaveDTO;
import java.net.URI;

public final class UiValidator {
    private UiValidator() { }
    public static void slot(String value) {
        if (value == null || !value.matches("[a-z][a-z0-9_-]{0,31}")) throw invalid("图片位置标识格式不正确");
    }
    public static void validate(UiSaveDTO input) {
        if (input == null) throw invalid("请填写图片配置");
        if (input.getId() != null && input.getId() <= 0) throw invalid("图片编号不正确");
        if (input.getAssetKey() == null || !input.getAssetKey().matches("[a-z][a-z0-9_-]{0,63}")) throw invalid("配置标识仅支持小写字母、数字、横线和下划线");
        slot(input.getSlot());
        input.setTitle(text(input.getTitle(), 80));
        if (input.getTitle().isEmpty()) throw invalid("请输入图片标题");
        input.setDescription(text(input.getDescription(), 200));
        input.setLinkUrl(text(input.getLinkUrl(), 512));
        validateLink(input.getLinkUrl());
        if (!"".equals(input.getTone()) && !"blue".equals(input.getTone()) && !"green".equals(input.getTone())) throw invalid("请选择有效的背景样式");
        if (input.getSortOrder() == null || input.getSortOrder() < 0 || input.getSortOrder() > 9999) throw invalid("排序必须为0至9999");
        if (input.getEnabled() == null) throw invalid("请选择启用状态");
    }
    private static String text(String value, int max) {
        String result = value == null ? "" : value.trim();
        if (result.length() > max) throw invalid("配置内容超出长度限制");
        return result;
    }
    public static void validateLink(String link) {
        if (link.isEmpty()) return;
        if (link.contains("\\") || link.chars().anyMatch(Character::isISOControl)) throw invalid("跳转地址不正确");
        try {
            URI uri = URI.create(link);
            if (link.startsWith("/") && !link.startsWith("//") && uri.getRawAuthority() == null) return;
            if (("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null && uri.getUserInfo() == null) return;
        } catch (IllegalArgumentException ignored) { }
        throw invalid("跳转地址必须是站内路径或 HTTP/HTTPS 地址");
    }
    private static BusinessException invalid(String message) { return new BusinessException(400, message); }
}
