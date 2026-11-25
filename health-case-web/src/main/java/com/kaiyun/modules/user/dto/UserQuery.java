package com.kaiyun.modules.user.dto;

import com.kaiyun.common.exception.BusinessException;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@Data
public class UserQuery {
    private int page = 1;
    private int size = 10;
    private String phone = "";
    private String source = "";
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate from;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate to;

    public int getSize() { return Math.max(1, Math.min(size, 100)); }
    public int pageWithin(long total) {
        return (int) Math.max(1, Math.min(page, (total + getSize() - 1) / getSize()));
    }

    public void validate() {
        phone = phone == null ? "" : phone.trim();
        source = source == null ? "" : source.trim();
        if (!phone.isEmpty() && !phone.matches("[0-9]{1,11}")) throw new BusinessException(400, "手机号筛选请输入1至11位数字");
        if (!source.isEmpty() && !"LOGIN".equals(source) && !"BUSINESS".equals(source)) {
            throw new BusinessException(400, "请选择有效的用户来源");
        }
        if (from != null && to != null && to.isBefore(from)) throw new BusinessException(400, "结束日期不能早于开始日期");
        if ((from != null && from.getYear() < 1000) || (to != null && (to.getYear() < 1000 || to.getYear() >= 9999))) {
            throw new BusinessException(400, "登录日期超出支持范围");
        }
    }
}
