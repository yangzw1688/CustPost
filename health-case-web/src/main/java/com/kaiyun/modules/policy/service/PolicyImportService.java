package com.kaiyun.modules.policy.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaiyun.common.exception.BusinessException;
import com.kaiyun.modules.policy.entity.ServicePolicy;
import com.kaiyun.modules.policy.entity.ServicePolicyOperationLog;
import com.kaiyun.modules.policy.mapper.ServicePolicyMapper;
import com.kaiyun.modules.policy.mapper.ServicePolicyOperationLogMapper;
import com.kaiyun.modules.project.service.ProjectService;
import lombok.Value;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellReference;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.kaiyun.modules.policy.service.PolicyImportField.*;

@Service
public class PolicyImportService {
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final int MAX_ROWS = 1000;
    private static final int MAX_COLUMNS = 100;
    private static final int PREVIEW_ROWS = 20;
    private static final int PREVIEW_ERRORS = 50;
    private static final DateTimeFormatter TEXT_DATE = DateTimeFormatter.ofPattern("uuuu-M-d")
            .withResolverStyle(ResolverStyle.STRICT);

    static {
        ZipSecureFile.setMaxEntrySize(16L * 1024 * 1024);
        ZipSecureFile.setMaxFileCount(128);
    }

    private final ServicePolicyMapper policies;
    private final ServicePolicyOperationLogMapper logs;
    private final ObjectMapper json;
    private final ProjectService projects;

    public PolicyImportService(ServicePolicyMapper policies, ServicePolicyOperationLogMapper logs, ObjectMapper json, ProjectService projects) {
        this.policies = policies;
        this.logs = logs;
        this.json = json;
        this.projects = projects;
    }

    public Columns columns(MultipartFile file, int sheetNumber, int headerRow) {
        return readWorkbook(file, workbook -> {
            Sheet sheet = sheet(workbook, sheetNumber, headerRow);
            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            Row header = sheet.getRow(headerRow - 1);
            List<Column> columns = new ArrayList<>();
            Set<PolicyImportField> suggested = EnumSet.noneOf(PolicyImportField.class);
            int width = header == null ? 0 : Math.max(0, header.getLastCellNum());
            if (width > MAX_COLUMNS) throw invalid("最多支持 " + MAX_COLUMNS + " 列");
            for (int i = 0; i < width; i++) {
                String name = formatter.formatCellValue(header.getCell(i)).trim();
                if (name.length() > 100) name = name.substring(0, 100);
                PolicyImportField match = null;
                if (!name.isEmpty()) {
                    for (PolicyImportField field : values()) {
                        if (field.matches(name) && suggested.add(field)) {
                            match = field;
                            break;
                        }
                    }
                }
                String label = CellReference.convertNumToColString(i);
                columns.add(new Column(i, name.isEmpty() ? label : label + " · " + name,
                        match == null ? null : match.getKey()));
            }
            List<String> sheets = new ArrayList<>();
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) sheets.add(workbook.getSheetName(i));
            List<Field> fields = Arrays.stream(values()).map(f -> new Field(f.getKey(), f.getLabel(), f.isRequired()))
                    .collect(Collectors.toList());
            return new Columns(sheets, columns, fields);
        });
    }

    public Preview preview(MultipartFile file, int sheetNumber, int headerRow, String mapping) {
        Batch batch = parse(file, sheetNumber, headerRow, mapping);
        List<PreviewRow> sample = batch.rows.stream().limit(PREVIEW_ROWS)
                .map(row -> new PreviewRow(row.number, row.policy.getPolicyNo(), row.policy.getProductName(),
                        row.policy.getInsuredName(), row.policy.getStatus(), row.policy.getStartDate(), row.policy.getEndDate()))
                .collect(Collectors.toList());
        return new Preview(batch.total, batch.errors.size(), batch.errors.stream().limit(PREVIEW_ERRORS)
                .collect(Collectors.toList()), sample);
    }

    @Transactional
    public int save(MultipartFile file, int sheetNumber, int headerRow, String mapping, String operator, Long projectId) {
        if (projectId != null) projects.lockActive(projectId);
        Batch batch = parse(file, sheetNumber, headerRow, mapping);
        if (!batch.errors.isEmpty()) throw new BusinessException(400, batch.errors.get(0));
        LocalDateTime now = LocalDateTime.now();
        for (ImportRow row : batch.rows) {
            ServicePolicy policy = row.policy;
            policy.setProjectId(projectId);
            policy.setCreateTime(now);
            policy.setUpdateTime(now);
            try {
                policies.insert(policy);
            } catch (DuplicateKeyException exception) {
                throw new BusinessException(409, rowError(row.number, "保单号已存在，请重新预览"));
            }
            ServicePolicyOperationLog log = new ServicePolicyOperationLog();
            log.setPolicyId(policy.getId());
            log.setOperationType("CREATE");
            log.setOperationContent("通过 Excel 导入保单");
            log.setOperator(operator);
            log.setCreateTime(now);
            logs.insert(log);
        }
        return batch.rows.size();
    }

    private Batch parse(MultipartFile file, int sheetNumber, int headerRow, String rawMapping) {
        Map<PolicyImportField, Integer> mapping = mapping(rawMapping);
        Batch batch = readWorkbook(file, workbook -> {
            Sheet sheet = sheet(workbook, sheetNumber, headerRow);
            Row header = sheet.getRow(headerRow - 1);
            if (header == null || header.getLastCellNum() < 1) throw invalid("选定的表头行没有列");
            int headerWidth = header.getLastCellNum();
            if (mapping.values().stream().anyMatch(column -> column >= headerWidth)) {
                throw invalid("列映射超出表头范围，请重新读取表头");
            }
            if (sheet.getLastRowNum() - headerRow + 1 > MAX_ROWS) {
                throw invalid("每次最多导入 " + MAX_ROWS + " 行，请拆分文件");
            }
            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            Batch result = new Batch();
            Set<String> seen = new HashSet<>();
            for (int index = headerRow; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                if (row == null) continue;
                int number = index + 1;
                try {
                    Map<PolicyImportField, String> values = new EnumMap<>(PolicyImportField.class);
                    for (Map.Entry<PolicyImportField, Integer> entry : mapping.entrySet()) {
                        values.put(entry.getKey(), cellValue(row.getCell(entry.getValue()), entry.getKey(), formatter));
                    }
                    if (values.values().stream().allMatch(String::isEmpty)) continue;
                    ServicePolicy policy = toPolicy(values);
                    if (!seen.add(policy.getPolicyNo().toLowerCase(Locale.ROOT))) throw invalid("文件内保单号重复");
                    result.rows.add(new ImportRow(number, policy));
                } catch (BusinessException exception) {
                    result.errors.add(rowError(number, exception.getMessage()));
                }
            }
            if (result.rows.isEmpty() && result.errors.isEmpty()) throw invalid("表头下方没有可导入的保单");
            result.total = result.rows.size() + result.errors.size();
            return result;
        });
        if (!batch.rows.isEmpty()) {
            List<String> numbers = batch.rows.stream().map(row -> row.policy.getPolicyNo()).collect(Collectors.toList());
            Set<String> existing = policies.selectExistingPolicyNumbers(numbers).stream()
                    .map(number -> number.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
            for (ImportRow row : batch.rows) {
                if (existing.contains(row.policy.getPolicyNo().toLowerCase(Locale.ROOT))) {
                    batch.errors.add(rowError(row.number, "保单号已存在"));
                }
            }
        }
        return batch;
    }

    private Map<PolicyImportField, Integer> mapping(String value) {
        if (value.length() > 2000) throw invalid("列映射格式不正确");
        Map<String, Integer> submitted;
        try {
            submitted = json.readValue(value, new TypeReference<Map<String, Integer>>() {});
        } catch (IOException exception) {
            throw invalid("列映射格式不正确");
        }
        if (submitted == null) throw invalid("请选择 Excel 列");
        Map<PolicyImportField, Integer> result = new EnumMap<>(PolicyImportField.class);
        Set<Integer> used = new HashSet<>();
        for (PolicyImportField field : values()) {
            Integer index = submitted.get(field.getKey());
            if (index == null) {
                if (field.isRequired()) throw invalid("请选择“" + field.getLabel() + "”对应的列");
                continue;
            }
            if (index < 0 || index >= MAX_COLUMNS || !used.add(index)) throw invalid("每个字段须对应不同的有效列");
            result.put(field, index);
        }
        return result;
    }

    private static String cellValue(Cell cell, PolicyImportField field, DataFormatter formatter) {
        if (cell == null || cell.getCellType() == CellType.BLANK) return "";
        if (cell.getCellType() == CellType.FORMULA || cell.getCellType() == CellType.ERROR) {
            throw invalid(field.getLabel() + "须为固定值，请将公式粘贴为值");
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            if (field == START_DATE || field == END_DATE) {
                if (!DateUtil.isCellDateFormatted(cell)) throw invalid(field.getLabel() + "须使用日期格式");
                return cell.getLocalDateTimeCellValue().toLocalDate().toString();
            }
            if (field == APPLICANT_ID_NO || field == INSURED_ID_NO || field == POLICY_NO) {
                throw invalid(field.getLabel() + "须使用文本格式，以保留前导零和长号码");
            }
            if (field == PREMIUM) return BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
        }
        return formatter.formatCellValue(cell).trim();
    }

    private static ServicePolicy toPolicy(Map<PolicyImportField, String> data) {
        ServicePolicy policy = new ServicePolicy();
        policy.setPolicyNo(text(data, POLICY_NO, 64));
        policy.setProductName(text(data, PRODUCT_NAME, 200));
        policy.setApplicantName(text(data, APPLICANT_NAME, 100));
        policy.setApplicantPhone(phone(data, APPLICANT_PHONE));
        policy.setApplicantIdType(text(data, APPLICANT_ID_TYPE, 30));
        policy.setApplicantIdNo(text(data, APPLICANT_ID_NO, 100));
        policy.setInsuredName(text(data, INSURED_NAME, 100));
        policy.setInsuredPhone(phone(data, INSURED_PHONE));
        policy.setInsuredIdType(text(data, INSURED_ID_TYPE, 30));
        policy.setInsuredIdNo(text(data, INSURED_ID_NO, 100));
        policy.setStatus(status(text(data, STATUS, 30)));
        policy.setPremium(premium(text(data, PREMIUM, 30)));
        policy.setStartDate(date(text(data, START_DATE, 20), START_DATE));
        policy.setEndDate(date(text(data, END_DATE, 20), END_DATE));
        if (policy.getStartDate() != null && policy.getEndDate() != null
                && policy.getEndDate().isBefore(policy.getStartDate())) throw invalid("失效日期不能早于生效日期");
        return policy;
    }

    private static String text(Map<PolicyImportField, String> data, PolicyImportField field, int max) {
        String value = data.get(field);
        if (value == null || value.isEmpty()) {
            if (field.isRequired()) throw invalid(field.getLabel() + "不能为空");
            return null;
        }
        if (value.length() > max) throw invalid(field.getLabel() + "最多 " + max + " 个字符");
        return value;
    }

    private static String phone(Map<PolicyImportField, String> data, PolicyImportField field) {
        String phone = text(data, field, 30);
        if (phone != null && !phone.matches("1[3-9][0-9]{9}")) throw invalid(field.getLabel() + "须为 11 位手机号");
        return phone;
    }

    private static String status(String value) {
        switch (value.toUpperCase(Locale.ROOT)) {
            case "ACTIVE": case "VALID": case "生效中": case "保障中": case "有效": case "已生效": return "ACTIVE";
            case "PENDING": case "待生效": return "PENDING";
            case "EXPIRED": case "已到期": case "已失效": return "EXPIRED";
            case "CANCELLED": case "已取消": return "CANCELLED";
            default: throw invalid("保单状态须为生效中、待生效、已到期或已取消");
        }
    }

    private static BigDecimal premium(String value) {
        if (value == null) return null;
        if (!value.matches("[0-9]{1,10}(\\.[0-9]{1,2})?")) throw invalid("保费须为非负金额，最多两位小数");
        return new BigDecimal(value);
    }

    private static LocalDate date(String value, PolicyImportField field) {
        if (value == null) return null;
        try {
            LocalDate date = LocalDate.parse(value.replace('/', '-'), TEXT_DATE);
            if (date.getYear() >= 1000 && date.getYear() <= 9999) return date;
        } catch (java.time.format.DateTimeParseException ignored) {
            // 统一返回行号和字段提示。
        }
        throw invalid(field.getLabel() + "须为有效日期（YYYY-MM-DD）");
    }

    private static Sheet sheet(Workbook workbook, int sheetNumber, int headerRow) {
        if (sheetNumber < 1 || sheetNumber > workbook.getNumberOfSheets()) throw invalid("请选择文件中的工作表");
        if (headerRow < 1 || headerRow > MAX_ROWS) throw invalid("表头行须在第 1 至 " + MAX_ROWS + " 行");
        return workbook.getSheetAt(sheetNumber - 1);
    }

    private static <T> T readWorkbook(MultipartFile file, Function<Workbook, T> read) {
        if (file.isEmpty() || file.getSize() > MAX_FILE_SIZE) throw invalid("请选择不超过 5 MB 的 Excel 文件");
        try (InputStream input = new BufferedInputStream(file.getInputStream());
                Workbook workbook = WorkbookFactory.create(input)) {
            return read.apply(workbook);
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw invalid("无法读取 Excel 文件，请检查文件格式、内容或是否加密");
        }
    }

    private static String rowError(int row, String message) {
        return "第 " + row + " 行：" + message;
    }

    private static BusinessException invalid(String message) {
        return new BusinessException(400, message);
    }

    @Value public static class Columns {
        List<String> sheets;
        List<Column> columns;
        List<Field> fields;
    }

    @Value public static class Column {
        int index;
        String label;
        String suggestedField;
    }

    @Value public static class Field {
        String key;
        String label;
        boolean required;
    }

    @Value public static class Preview {
        int total;
        int errorCount;
        List<String> errors;
        List<PreviewRow> rows;
    }

    @Value public static class PreviewRow {
        int row;
        String policyNo;
        String productName;
        String insuredName;
        String status;
        LocalDate startDate;
        LocalDate endDate;
    }

    @Value private static class ImportRow {
        int number;
        ServicePolicy policy;
    }

    private static class Batch {
        int total;
        final List<ImportRow> rows = new ArrayList<>();
        final List<String> errors = new ArrayList<>();
    }
}
