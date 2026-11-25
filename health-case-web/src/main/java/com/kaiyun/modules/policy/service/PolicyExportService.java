package com.kaiyun.modules.policy.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kaiyun.common.exception.BusinessException;
import com.kaiyun.modules.policy.entity.ServicePolicy;
import com.kaiyun.modules.policy.mapper.ServicePolicyMapper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.kaiyun.modules.policy.service.PolicyImportField.*;

@Service
public class PolicyExportService {
    private static final int MAX_ROWS = 10_000;
    private static final DateTimeFormatter CREATED_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final LocalDate EARLIEST_DATE = LocalDate.of(1000, 1, 1);
    private static final LocalDate LATEST_DATE = LocalDate.of(9999, 12, 31);
    private static final Set<PolicyImportField> PERSONAL_NUMBERS = EnumSet.of(
            APPLICANT_PHONE, APPLICANT_ID_NO, INSURED_PHONE, INSURED_ID_NO);

    private final ServicePolicyMapper policies;

    public PolicyExportService(ServicePolicyMapper policies) {
        this.policies = policies;
    }

    public byte[] export(String policyNo, String insuredName, String status, LocalDate from, LocalDate to)
            throws IOException {
        policyNo = policyNo.trim();
        insuredName = insuredName.trim();
        status = status.trim();
        if (policyNo.length() > 64 || insuredName.length() > 100 || status.length() > 30) {
            throw new BusinessException(400, "查询条件过长");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(400, "创建时间结束日期不能早于开始日期");
        }
        if ((from != null && (from.isBefore(EARLIEST_DATE) || from.isAfter(LATEST_DATE)))
                || (to != null && (to.isBefore(EARLIEST_DATE) || to.isAfter(LATEST_DATE)))) {
            throw new BusinessException(400, "创建时间超出支持范围");
        }
        LocalDateTime upperBound = to == null || to.equals(LATEST_DATE) ? null : to.plusDays(1).atStartOfDay();

        LambdaQueryWrapper<ServicePolicy> query = new LambdaQueryWrapper<>();
        query.apply(!policyNo.isEmpty(), "policy_no LIKE {0} ESCAPE '!'", literalPattern(policyNo))
                .apply(!insuredName.isEmpty(), "insured_name LIKE {0} ESCAPE '!'", literalPattern(insuredName))
                .eq(!status.isEmpty(), ServicePolicy::getStatus, status)
                .ge(from != null, ServicePolicy::getCreateTime, from == null ? null : from.atStartOfDay())
                .lt(upperBound != null, ServicePolicy::getCreateTime, upperBound)
                .orderByDesc(ServicePolicy::getCreateTime, ServicePolicy::getId)
                .last("LIMIT " + (MAX_ROWS + 1));
        List<ServicePolicy> records = policies.selectList(query);
        if (records.isEmpty()) throw new BusinessException(404, "没有符合条件的保单");
        if (records.size() > MAX_ROWS) throw new BusinessException(400, "最多导出 10000 条，请缩小筛选范围");

        PolicyImportField[] columns = values();
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("保单");
            sheet.createFreezePane(0, 1);
            Row header = sheet.createRow(0);
            for (int i = 0; i < columns.length; i++) {
                sheet.setColumnWidth(i, 22 * 256);
                header.createCell(i).setCellValue(columns[i].getLabel());
            }
            sheet.setColumnWidth(columns.length, 22 * 256);
            header.createCell(columns.length).setCellValue("创建时间");
            for (int i = 0; i < records.size(); i++) {
                Row row = sheet.createRow(i + 1);
                ServicePolicy policy = records.get(i);
                for (int column = 0; column < columns.length; column++) {
                    PolicyImportField field = columns[column];
                    Object value = field.read(policy);
                    if (value == null) continue;
                    Cell cell = row.createCell(column);
                    if (PERSONAL_NUMBERS.contains(field)) {
                        cell.setCellValue(maskNumber(value.toString()));
                    } else if (value instanceof BigDecimal) {
                        cell.setCellValue(((BigDecimal) value).doubleValue());
                    } else {
                        // 文本单元格保留保单号的前导零，也避免把保单文字写成公式。
                        cell.setCellValue(value.toString());
                    }
                }
                if (policy.getCreateTime() != null) {
                    row.createCell(columns.length).setCellValue(CREATED_TIME.format(policy.getCreateTime()));
                }
            }
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private static String maskNumber(String value) {
        String text = value.trim();
        int length = text.length();
        boolean showEnds = length > 7;
        char[] masked = new char[showEnds ? length - 7 : length];
        Arrays.fill(masked, '*');
        String stars = new String(masked);
        return showEnds ? text.substring(0, 3) + stars + text.substring(length - 4) : stars;
    }

    private static String literalPattern(String value) {
        return "%" + value.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }
}
