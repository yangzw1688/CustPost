package com.kaiyun.modules.cases.service;

import com.kaiyun.common.exception.BusinessException;
import com.kaiyun.modules.cases.dto.ServiceRecordSaveDTO;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/** 在数据库和 OBS 写入前完成整批校验，避免上传到一半才发现参数错误。 */
public final class ServiceRecordValidator {
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    private ServiceRecordValidator() { }

    public static LocalDate validateForm(ServiceRecordSaveDTO dto) {
        if (dto == null) throw invalid("提交内容不能为空");
        dto.setName(required(dto.getName(), "患者姓名", 20));
        if (dto.getReporterName() != null) dto.setReporterName(required(dto.getReporterName(), "报案人姓名", 20));
        dto.setPhone(required(dto.getPhone(), "手机号", 11));
        if (!dto.getPhone().matches("^1[3-9]\\d{9}$")) throw invalid("手机号格式不正确");
        dto.setCaseTitle(required(dto.getCaseTitle(), "病例名称", 100));
        dto.setCaseDescription(required(dto.getCaseDescription(), "病例描述", 2000));
        if (dto.getCaseDescription().length() < 5) throw invalid("病例描述请至少填写5个字符");
        if (!StringUtils.hasText(dto.getVisitDate())) return null;
        try {
            LocalDate date = LocalDate.parse(dto.getVisitDate().trim());
            if (date.isAfter(LocalDate.now())) throw invalid("就诊日期不能晚于今天");
            return date;
        } catch (DateTimeParseException exception) {
            throw invalid("就诊日期格式不正确，请使用 yyyy-MM-dd");
        }
    }

    public static List<String> validateImages(MultipartFile[] files) throws IOException {
        return validateFiles(files, false);
    }

    public static List<String> validateAttachments(MultipartFile[] files) throws IOException {
        return validateFiles(files, true);
    }

    private static List<String> validateFiles(MultipartFile[] files, boolean allowPdf) throws IOException {
        // 界面图片只允许图片；病例附件额外允许 PDF，二者复用大小和文件头校验。
        if (files == null || files.length == 0) throw invalid("请至少上传一份资料");
        if (files.length > 9) throw invalid("最多上传9个文件");
        List<String> extensions = new ArrayList<>(files.length);
        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            String label = "第" + (i + 1) + "个文件";
            if (file == null || file.isEmpty()) throw invalid(label + "不能为空");
            if (file.getSize() > MAX_FILE_SIZE) throw invalid(label + "不能超过10MB");
            // 不能只相信客户端提供的文件名或 MIME，同时检查实际文件头。
            byte[] header = new byte[12];
            int length = 0;
            try (InputStream input = file.getInputStream()) {
                while (length < header.length) {
                    int read = input.read(header, length, header.length - length);
                    if (read < 0) break;
                    if (read == 0) break;
                    length += read;
                }
            }
            String extension = detectImage(header, length, file.getContentType());
            if (extension == null && allowPdf && isPdf(header, length, file)) extension = ".pdf";
            if (extension == null) throw invalid(label + (allowPdf
                    ? "格式无效，仅支持 PDF、JPEG、PNG、GIF、WebP" : "格式无效，仅支持 JPEG、PNG、GIF、WebP"));
            extensions.add(extension);
        }
        return extensions;
    }

    private static boolean isPdf(byte[] bytes, int length, MultipartFile file) {
        String type = file.getContentType();
        String name = file.getOriginalFilename();
        boolean pdfType = "application/pdf".equals(type);
        boolean genericType = type == null || type.isEmpty() || "application/octet-stream".equals(type);
        boolean pdfName = name != null && name.toLowerCase(java.util.Locale.ROOT).endsWith(".pdf");
        return (pdfType || genericType && pdfName) && length >= 8
                && bytes[0] == '%' && bytes[1] == 'P' && bytes[2] == 'D' && bytes[3] == 'F' && bytes[4] == '-'
                && bytes[5] >= '1' && bytes[5] <= '2' && bytes[6] == '.' && bytes[7] >= '0' && bytes[7] <= '9';
    }

    private static String detectImage(byte[] bytes, int length, String type) {
        if ("image/jpeg".equals(type) && length >= 3 && (bytes[0] & 255) == 255
                && (bytes[1] & 255) == 216 && (bytes[2] & 255) == 255) return ".jpg";
        if ("image/png".equals(type) && length >= 8 && (bytes[0] & 255) == 137
                && bytes[1] == 80 && bytes[2] == 78 && bytes[3] == 71
                && bytes[4] == 13 && bytes[5] == 10 && bytes[6] == 26 && bytes[7] == 10) return ".png";
        if ("image/gif".equals(type) && length >= 6 && bytes[0] == 'G' && bytes[1] == 'I'
                && bytes[2] == 'F' && bytes[3] == '8' && (bytes[4] == '7' || bytes[4] == '9')
                && bytes[5] == 'a') return ".gif";
        if ("image/webp".equals(type) && length >= 12 && bytes[0] == 'R' && bytes[1] == 'I'
                && bytes[2] == 'F' && bytes[3] == 'F' && bytes[8] == 'W' && bytes[9] == 'E'
                && bytes[10] == 'B' && bytes[11] == 'P') return ".webp";
        return null;
    }

    private static String required(String value, String label, int maxLength) {
        if (!StringUtils.hasText(value)) throw invalid("请输入" + label);
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) throw invalid(label + "不能超过" + maxLength + "个字符");
        return trimmed;
    }

    private static BusinessException invalid(String message) {
        return new BusinessException(400, message);
    }
}
