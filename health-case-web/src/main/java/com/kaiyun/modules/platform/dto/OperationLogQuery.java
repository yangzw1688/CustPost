package com.kaiyun.modules.platform.dto;

import com.kaiyun.common.exception.BusinessException;
import com.kaiyun.infrastructure.logging.AuditAction;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.util.Arrays;

@Data
public class OperationLogQuery {
    private int page = 1;
    private int size = 10;
    private String module = "";
    private String action = "";
    private String operator = "";
    private String target = "";
    private String result = "";
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate from;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate to;

    public int getSize() { return Math.max(1, Math.min(size, 100)); }

    public int pageWithin(long total) {
        return (int) Math.max(1, Math.min(page, (total + getSize() - 1) / getSize()));
    }

    public void validate() {
        module = text(module, 32);
        action = text(action, 64);
        operator = text(operator, 32).replaceFirst("^(\\d{3})\\d{4}(\\d{4})$", "$1****$2");
        target = text(target, 160).replaceAll("(?<!\\d)(\\d{3})\\d{4}(\\d{4})(?!\\d)", "$1****$2");
        result = text(result, 16);
        if (!module.isEmpty() && Arrays.stream(AuditAction.values()).noneMatch(value -> value.getModule().equals(module))) {
            throw invalid("请选择有效的操作模块");
        }
        if (!action.isEmpty() && Arrays.stream(AuditAction.values()).noneMatch(value -> value.name().equals(action))) {
            throw invalid("请选择有效的操作类型");
        }
        if (!result.isEmpty() && !"SUCCESS".equals(result) && !"FAILURE".equals(result)) throw invalid("请选择有效的操作结果");
        if (from != null && to != null && to.isBefore(from)) throw invalid("结束日期不能早于开始日期");
        if (to != null && to.getYear() >= 9999) throw invalid("结束日期超出支持范围");
    }

    private static String text(String value, int max) {
        String result = value == null ? "" : value.trim();
        if (result.length() > max) throw invalid("查询条件过长");
        return result;
    }

    private static BusinessException invalid(String message) { return new BusinessException(400, message); }
}
