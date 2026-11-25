package com.kaiyun.modules.project.dto;

import com.kaiyun.common.exception.BusinessException;
import lombok.Data;

@Data
public class ManagementQuery {
    private int page = 1;
    private int size = 10;
    private String keyword = "";
    private String status = "";
    private Long insurerId;
    public int getSize() { return Math.max(1, Math.min(size, 100)); }
    public int pageWithin(long total) { return (int) Math.max(1, Math.min(page, (total + getSize() - 1) / getSize())); }
    public void validate() {
        keyword = keyword == null ? "" : keyword.trim();
        status = status == null ? "" : status.trim();
        if (keyword.length() > 120 || status.length() > 24 || insurerId != null && insurerId <= 0) {
            throw new BusinessException(400, "查询条件不正确");
        }
    }
}
