package com.kaiyun.modules.ui.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaiyun.common.exception.BusinessException;
import com.kaiyun.modules.ui.dto.UiPageDocument;
import com.kaiyun.modules.ui.vo.UiVO;
import lombok.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 草稿和已发布内容分别保存，通过版本号防止多名管理员互相覆盖。 */
@Service
public class UiPageService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final UiService images;
    private final UiPublishedPages publishedPages;

    public UiPageService(JdbcTemplate jdbc, ObjectMapper json, UiService images, UiPublishedPages publishedPages) {
        this.jdbc = jdbc;
        this.json = json;
        this.images = images;
        this.publishedPages = publishedPages;
    }

    public List<Summary> list() {
        return jdbc.query("SELECT slug, title, version, published_version, updated_at FROM ui_page "
                        + "ORDER BY CASE WHEN slug = 'home' THEN 0 ELSE 1 END, updated_at DESC, slug",
                (row, index) -> new Summary(row.getString(1), row.getString(2), row.getLong(3), row.getLong(4),
                        row.getTimestamp(5).toLocalDateTime()));
    }

    public Detail detail(String slug) {
        PageRow page = require(slug, false);
        return new Detail(slug, page.version, page.publishedVersion, document(page.draft));
    }

    @Transactional
    public Detail create(String slug, UiPageDocument document) {
        UiPageValidator.slug(slug);
        UiPageValidator.validate(document, false);
        if ("home".equals(slug)) throw new BusinessException(409, "首页已由系统初始化");
        if (jdbc.queryForObject("SELECT COUNT(*) FROM ui_page", Integer.class) >= 100) throw new BusinessException(400, "最多创建100个页面");
        jdbc.update("INSERT INTO ui_page (slug, title, draft_json, version, published_version, updated_at) "
                + "VALUES (?, ?, ?, 1, 0, CURRENT_TIMESTAMP)", slug, document.getTitle(), encode(document));
        return detail(slug);
    }

    @Transactional
    public Detail save(String slug, Long version, UiPageDocument document) {
        UiPageValidator.slug(slug);
        UiPageValidator.validate(document, false);
        version(version);
        int changed = jdbc.update("UPDATE ui_page SET title = ?, draft_json = ?, version = version + 1, updated_at = CURRENT_TIMESTAMP "
                + "WHERE slug = ? AND version = ?", document.getTitle(), encode(document), slug, version);
        if (changed != 1) throw conflict();
        return detail(slug);
    }

    @Transactional
    public Detail publish(String slug, Long version) {
        UiPageValidator.slug(slug);
        version(version);
        Map<String, UiPageDocument> published = publishedPages.lockPublishedPages();
        PageRow row = require(slug, true);
        if (row.version != version) throw conflict();
        UiPageDocument document = document(row.draft);
        UiPageValidator.validate(document, true);
        publishedPages.requirePublishedTargets(published, slug, document);
        Set<Long> imageIds = UiPublishedPages.imageIds(document);
        if (images.pageImages(imageIds).size() != imageIds.size()) throw new BusinessException(400, "页面引用的图片不存在、未上传或已停用，请重新选择");
        jdbc.update("UPDATE ui_page SET published_json = ?, version = version + 1, published_version = version + 1, "
                + "updated_at = CURRENT_TIMESTAMP WHERE slug = ?", encode(document), slug);
        return detail(slug);
    }

    @Transactional
    public Detail unpublish(String slug, Long version) {
        UiPageValidator.slug(slug);
        if ("home".equals(slug)) throw new BusinessException(400, "首页需要保持发布状态");
        version(version);
        publishedPages.requireUnreferencedPage(slug);
        if (jdbc.update("UPDATE ui_page SET published_json = NULL, published_version = 0, version = version + 1, "
                + "updated_at = CURRENT_TIMESTAMP WHERE slug = ? AND version = ?", slug, version) != 1) throw conflict();
        return detail(slug);
    }

    @Transactional
    public void delete(String slug, Long version) {
        UiPageValidator.slug(slug);
        if ("home".equals(slug)) throw new BusinessException(400, "首页不能删除");
        version(version);
        publishedPages.requireUnreferencedPage(slug);
        if (jdbc.update("DELETE FROM ui_page WHERE slug = ? AND version = ?", slug, version) != 1) throw conflict();
    }

    @Transactional(readOnly = true)
    public Published content(String slug) {
        PageRow row = require(slug, false);
        if (row.published == null) throw new BusinessException(404, "页面尚未发布或已下线");
        UiPageDocument document = document(row.published);
        Map<String, String> urls = new LinkedHashMap<>();
        for (UiVO image : images.pageImages(UiPublishedPages.imageIds(document))) urls.put(image.getId().toString(), image.getImageUrl());
        boolean hasBanner = document.getBlocks().stream().anyMatch(block -> block.getEnabled() && "banner".equals(block.getType()));
        return new Published(slug, row.publishedVersion, document, urls,
                hasBanner ? images.list("home_banner", false) : Collections.emptyList());
    }

    private PageRow require(String slug, boolean lock) {
        UiPageValidator.slug(slug);
        List<PageRow> rows = jdbc.query("SELECT draft_json, published_json, version, published_version FROM ui_page WHERE slug = ?"
                        + (lock ? " FOR UPDATE" : ""),
                (row, index) -> new PageRow(row.getString(1), row.getString(2), row.getLong(3), row.getLong(4)), slug);
        if (rows.isEmpty()) throw new BusinessException(404, "页面不存在");
        return rows.get(0);
    }

    private UiPageDocument document(String value) {
        try { return json.readValue(value, UiPageDocument.class); }
        catch (JsonProcessingException error) { throw new IllegalStateException("页面配置读取失败", error); }
    }

    private String encode(UiPageDocument value) {
        try { return json.writeValueAsString(value); }
        catch (JsonProcessingException error) { throw new IllegalStateException("页面配置保存失败", error); }
    }

    private void version(Long version) {
        if (version == null || version < 1) throw new BusinessException(400, "请提供页面版本号");
    }

    private BusinessException conflict() {
        return new BusinessException(409, "页面已被其他操作修改，请重新加载后再编辑");
    }

    @Value private static class PageRow {
        String draft;
        String published;
        long version;
        long publishedVersion;
    }

    @Value public static class Summary {
        String slug;
        String title;
        long version;
        long publishedVersion;
        LocalDateTime updatedAt;
    }

    @Value public static class Detail {
        String slug;
        long version;
        long publishedVersion;
        UiPageDocument document;
    }

    @Value public static class Published {
        String slug;
        long version;
        UiPageDocument document;
        Map<String, String> images;
        List<UiVO> banners;
    }
}
