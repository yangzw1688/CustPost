package com.kaiyun.modules.ui.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaiyun.common.exception.BusinessException;
import com.kaiyun.modules.ui.dto.UiPageDocument;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
final class UiPublishedPages {
    private static final Pattern PAGE_PATH = Pattern.compile("/pages/([a-z][a-z0-9-]{0,47})/?", Pattern.CASE_INSENSITIVE);
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    UiPublishedPages(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    // 调用方在事务内按统一顺序锁定页面，避免发布与下线、素材停用相互穿插。
    Map<String, UiPageDocument> lockPublishedPages() {
        return jdbc.query("SELECT slug, published_json FROM ui_page ORDER BY slug FOR UPDATE", rows -> {
            Map<String, UiPageDocument> pages = new LinkedHashMap<>();
            while (rows.next()) {
                String published = rows.getString("published_json");
                if (published != null) pages.put(rows.getString("slug"), parse(published));
            }
            return pages;
        });
    }

    void requirePublishedTargets(Map<String, UiPageDocument> pages, String slug, UiPageDocument document) {
        for (String target : pageTargets(document)) {
            if (!slug.equals(target) && !pages.containsKey(target)) {
                throw new BusinessException(400, "链接目标 /pages/" + target + " 尚未发布或不存在，请先发布目标页面或修改链接");
            }
        }
    }

    void requireUnreferencedPage(String slug) {
        for (Map.Entry<String, UiPageDocument> page : lockPublishedPages().entrySet()) {
            if (!slug.equals(page.getKey()) && pageTargets(page.getValue()).contains(slug)) {
                throw referenced(page.getKey(), "页面链接");
            }
        }
    }

    void requireUnreferencedImage(Long imageId) {
        for (Map.Entry<String, UiPageDocument> page : lockPublishedPages().entrySet()) {
            if (imageIds(page.getValue()).contains(imageId)) throw referenced(page.getKey(), "图片素材");
        }
    }

    static Set<Long> imageIds(UiPageDocument document) {
        return document.getBlocks().stream().filter(block -> Boolean.TRUE.equals(block.getEnabled()) && "image".equals(block.getType())
                && block.getImageId() != null).map(UiPageDocument.Block::getImageId).collect(Collectors.toSet());
    }

    private Set<String> pageTargets(UiPageDocument document) {
        return document.getBlocks().stream().filter(block -> Boolean.TRUE.equals(block.getEnabled()))
                .flatMap(block -> {
                    if ("links".equals(block.getType())) {
                        return block.getItems().stream().filter(item -> Boolean.TRUE.equals(item.getEnabled())).map(UiPageDocument.Link::getLinkUrl);
                    }
                    if ("button".equals(block.getType()) || "image".equals(block.getType())) return Stream.of(block.getLinkUrl());
                    return Stream.empty();
                }).map(this::managedPageSlug).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private String managedPageSlug(String url) {
        if (url == null || !url.startsWith("/") || url.startsWith("//")) return null;
        try {
            Matcher path = PAGE_PATH.matcher(URI.create(url).normalize().getPath());
            return path.matches() ? path.group(1) : null;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private UiPageDocument parse(String value) {
        try { return json.readValue(value, UiPageDocument.class); }
        catch (JsonProcessingException error) { throw new IllegalStateException("已发布页面配置读取失败", error); }
    }

    private BusinessException referenced(String slug, String label) {
        return new BusinessException(409, "已发布页面「" + slug + "」仍在使用该" + label + "，请先移除引用并重新发布");
    }
}
