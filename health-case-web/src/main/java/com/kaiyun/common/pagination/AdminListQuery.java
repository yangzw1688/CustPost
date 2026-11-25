package com.kaiyun.common.pagination;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AdminListQuery {
    private int page = 1;
    private int size = 10;
    private String name = "";
    private String phone = "";
    private String caseTitle = "";
    private String policyNo = "";
    private String insuredName = "";
    private String status = "";
    private boolean reviewPending;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate from;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate to;

    public int getSize() { return Math.max(1, Math.min(size, 100)); }

    public int pageWithin(long total) {
        return (int) Math.max(1, Math.min(page, (total + getSize() - 1) / getSize()));
    }

    public LocalDateTime getFromTime() { return from == null ? null : from.atStartOfDay(); }
    public LocalDateTime getUntilTime() { return to == null ? null : to.plusDays(1).atStartOfDay(); }
}
