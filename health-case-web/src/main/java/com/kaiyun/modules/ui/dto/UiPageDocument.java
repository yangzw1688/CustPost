package com.kaiyun.modules.ui.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/** 可发布的页面内容。模块类型限定为系统支持的组件，文本按普通文字显示。 */
@Data
public class UiPageDocument {
    private String title;
    private String description = "";
    private String primaryColor = "#1677ff";
    private Boolean showNavigation = true;
    private String homeLabel = "首页";
    private String mineLabel = "个人信息";
    private Notice notice = new Notice();
    private List<Block> blocks = new ArrayList<>();

    @Data
    public static class Notice {
        private boolean enabled;
        private String title = "";
        private String message = "";
    }

    @Data
    public static class Block {
        private String id;
        private String type;
        private String title = "";
        private String description = "";
        private String text = "";
        private Boolean enabled = true;
        private String layout = "grid";
        private Integer columns = 4;
        private Long imageId;
        private String linkUrl = "";
        private String linkType = "auto";
        private Boolean showMore = false;
        private List<Link> items = new ArrayList<>();
    }

    @Data
    public static class Link {
        private String id;
        private String title = "";
        private String description = "";
        private String linkUrl = "";
        private String linkType = "auto";
        private String icon = "Document";
        private String tone = "blue";
        private Boolean enabled = true;
    }
}
