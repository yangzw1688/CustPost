package com.kaiyun.infrastructure.logging;

/** 审计动作使用固定名称，列表筛选与记录展示共用同一份定义。 */
public enum AuditAction {
    PAGE_CREATE("UI", "用户端页面", "新增页面"),
    PAGE_SAVE("UI", "用户端页面", "保存页面草稿"),
    PAGE_PUBLISH("UI", "用户端页面", "发布页面"),
    PAGE_UNPUBLISH("UI", "用户端页面", "下线页面"),
    PAGE_DELETE("UI", "用户端页面", "删除页面"),
    ASSET_SAVE("UI", "用户端页面", "保存图片素材"),
    ASSET_DELETE("UI", "用户端页面", "删除图片素材"),
    ASSET_IMPORT("UI", "用户端页面", "导入默认图片"),
    ADMIN_LOGIN("AUTH", "管理员账号", "管理员登录"),
    ADMIN_LOGOUT("AUTH", "管理员账号", "管理员退出"),
    ADMIN_PASSWORD("AUTH", "管理员账号", "修改管理员密码"),
    ADMIN_GRANT("PERMISSION", "管理员与权限", "保存管理员与权限"),
    ADMIN_REVOKE("PERMISSION", "管理员与权限", "撤销管理员"),
    POLICY_IMPORT("POLICY", "保单管理", "导入保单"),
    POLICY_EXPORT("POLICY", "保单管理", "导出保单"),
    CASE_PROGRESS("CASES", "案件管理", "更新案件处理进度"),
    CASE_REVIEW("CASES", "案件管理", "审核报案"),
    CASE_PUSH("CASES", "案件管理", "提交赔案推送"),
    CASE_RETRY("CASES", "案件管理", "重试赔案推送"),
    INSURER_SAVE("PROJECT", "项目管理", "保存保险公司"),
    INSURER_CONNECTION("PROJECT", "项目管理", "保存保险公司对接配置"),
    PROJECT_SAVE("PROJECT", "项目管理", "保存项目"),
    PROJECT_ASSIGN("PROJECT", "项目管理", "关联项目保单");

    private final String module;
    private final String moduleName;
    private final String label;

    AuditAction(String module, String moduleName, String label) {
        this.module = module;
        this.moduleName = moduleName;
        this.label = label;
    }

    public String getModule() { return module; }
    public String getModuleName() { return moduleName; }
    public String getLabel() { return label; }
}
