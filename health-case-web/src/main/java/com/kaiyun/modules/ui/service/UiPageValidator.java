package com.kaiyun.modules.ui.service;

import com.kaiyun.common.exception.BusinessException;
import com.kaiyun.modules.ui.dto.UiPageDocument;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

final class UiPageValidator {
    private static final Set<String> TYPES = new HashSet<>(Arrays.asList("heading", "banner", "links", "text", "image", "button"));
    private static final Set<String> ICONS = new HashSet<>(Arrays.asList("Money", "Document", "Tickets", "Search", "Service", "FirstAidKit", "House", "User"));
    private static final Set<String> TONES = new HashSet<>(Arrays.asList("blue", "green", "red", "orange", "purple", "pink"));

    private UiPageValidator() { }

    static void slug(String slug) {
        if (slug == null || !slug.matches("[a-z][a-z0-9-]{0,47}")) throw invalid("页面标识仅支持小写字母、数字和横线，最多48个字符");
    }

    static void validate(UiPageDocument page, boolean publishing) {
        if (page == null) throw invalid("请填写页面内容");
        page.setTitle(text(page.getTitle(), 80));
        if (page.getTitle().isEmpty()) throw invalid("请输入页面名称");
        page.setDescription(text(page.getDescription(), 200));
        if (page.getPrimaryColor() == null || !page.getPrimaryColor().matches("#[0-9a-fA-F]{6}")) throw invalid("请选择有效的主题颜色");
        if (page.getShowNavigation() == null) throw invalid("请选择导航显示状态");
        page.setHomeLabel(text(page.getHomeLabel(), 20));
        page.setMineLabel(text(page.getMineLabel(), 20));
        if (page.getHomeLabel().isEmpty() || page.getMineLabel().isEmpty()) throw invalid("请填写底部导航名称");
        validateNotice(page, publishing);
        if (page.getBlocks() == null || page.getBlocks().size() > 30) throw invalid("每个页面最多配置30个模块");
        Set<String> blockIds = new HashSet<>();
        for (UiPageDocument.Block block : page.getBlocks()) {
            if (block == null || !blockIds.add(identifier(block.getId())) || !TYPES.contains(block.getType())) throw invalid("页面模块标识重复或类型不正确");
            if (block.getEnabled() == null || block.getShowMore() == null) throw invalid("请填写模块显示状态");
            block.setTitle(text(block.getTitle(), 80));
            block.setDescription(text(block.getDescription(), 200));
            block.setText(text(block.getText(), 5000));
            block.setLinkUrl(text(block.getLinkUrl(), 512));
            validateLink(block.getLinkUrl(), block.getLinkType());
            if (!Arrays.asList("grid", "list").contains(block.getLayout()) || block.getColumns() == null
                    || block.getColumns() < 2 || block.getColumns() > 4) throw invalid("请选择有效的入口布局和列数");
            if (block.getImageId() != null && block.getImageId() <= 0) throw invalid("图片编号不正确");
            if (block.getItems() == null || block.getItems().size() > 20) throw invalid("每个入口模块最多配置20个入口");
            Set<String> itemIds = new HashSet<>();
            for (UiPageDocument.Link link : block.getItems()) {
                if (link == null || !itemIds.add(identifier(link.getId())) || link.getEnabled() == null) throw invalid("服务入口标识重复或显示状态不正确");
                link.setTitle(text(link.getTitle(), 40));
                link.setDescription(text(link.getDescription(), 200));
                link.setLinkUrl(text(link.getLinkUrl(), 512));
                validateLink(link.getLinkUrl(), link.getLinkType());
                if (!ICONS.contains(link.getIcon()) || !TONES.contains(link.getTone())) throw invalid("请选择支持的入口图标和颜色");
                if (publishing && block.getEnabled() && link.getEnabled()
                        && (link.getTitle().isEmpty() || link.getLinkUrl().isEmpty())) throw invalid("发布前请填写启用入口的名称和跳转地址");
            }
            if (!publishing || !block.getEnabled()) continue;
            if ("image".equals(block.getType()) && block.getImageId() == null) throw invalid("发布前请为图片模块选择图片");
            if ("button".equals(block.getType()) && (block.getTitle().isEmpty() || block.getLinkUrl().isEmpty())) throw invalid("发布前请填写按钮文字和跳转地址");
            if ("heading".equals(block.getType()) && block.getTitle().isEmpty()) throw invalid("发布前请填写标题模块的标题");
            if ("text".equals(block.getType()) && block.getText().isEmpty()) throw invalid("发布前请填写文字模块的内容");
        }
    }

    private static void validateNotice(UiPageDocument page, boolean publishing) {
        UiPageDocument.Notice notice = page.getNotice();
        if (notice == null) {
            page.setNotice(new UiPageDocument.Notice());
            return;
        }
        notice.setTitle(text(notice.getTitle(), 80));
        notice.setMessage(text(notice.getMessage(), 2000));
        if (publishing && notice.isEnabled() && (notice.getTitle().isEmpty() || notice.getMessage().isEmpty())) {
            throw invalid("发布已开启的通知弹窗前，请填写通知标题和正文");
        }
    }

    private static String identifier(String value) {
        if (value == null || !value.matches("[a-zA-Z0-9_-]{1,64}")) throw invalid("模块或入口标识格式不正确");
        return value;
    }

    private static void validateLink(String link, String type) {
        UiValidator.validateLink(link);
        if (!Arrays.asList("auto", "url", "page").contains(type)) throw invalid("跳转类型不正确");
        if ("page".equals(type) && !link.isEmpty() && !link.startsWith("/")) throw invalid("页面跳转请选择站内页面");
    }

    private static String text(String value, int max) {
        String result = value == null ? "" : value.trim();
        if (result.length() > max) throw invalid("页面文字超出长度限制");
        return result;
    }

    private static BusinessException invalid(String message) {
        return new BusinessException(400, message);
    }
}
