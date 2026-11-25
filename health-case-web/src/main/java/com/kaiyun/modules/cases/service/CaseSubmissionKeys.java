package com.kaiyun.modules.cases.service;

import com.kaiyun.common.exception.BusinessException;
import com.kaiyun.modules.cases.dto.ServiceRecordSaveDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;

@Component
public class CaseSubmissionKeys {
    private final JdbcTemplate jdbc;

    public CaseSubmissionKeys(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Long reserve(String key, ServiceRecordSaveDTO form, LocalDate visitDate, MultipartFile[] files) throws IOException {
        if (key == null) return null;
        if (!key.matches("[0-9a-f]{32}")) throw new BusinessException(400, "提交编号无效");
        String digest = fingerprint(form, visitDate, files);
        // 同一事务里并发请求等待相同键。如果原事务回滚，下一次可以重新提交。
        jdbc.update("INSERT INTO case_submission (phone, request_id, fingerprint) VALUES (?, ?, ?) "
                        + "ON DUPLICATE KEY UPDATE request_id = VALUES(request_id)", form.getPhone(), key, digest);
        return jdbc.queryForObject("SELECT fingerprint, case_id FROM case_submission "
                        + "WHERE phone = ? AND request_id = ? FOR UPDATE", (row, index) -> {
                    if (!digest.equals(row.getString("fingerprint"))) throw new BusinessException(409, "提交内容已改变，请重新提交");
                    long caseId = row.getLong("case_id");
                    return row.wasNull() ? null : caseId;
                }, form.getPhone(), key);
    }

    public void complete(String key, String phone, long caseId) {
        if (key != null && jdbc.update("UPDATE case_submission SET case_id = ? WHERE phone = ? AND request_id = ?",
                caseId, phone, key) != 1) {
            throw new IllegalStateException("病例提交编号未能保存");
        }
    }

    private static String fingerprint(ServiceRecordSaveDTO form, LocalDate date, MultipartFile[] files) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException(error);
        }
        OutputStream sink = new OutputStream() {
            @Override public void write(int value) { }
            @Override public void write(byte[] bytes, int start, int length) { }
        };
        try (DataOutputStream out = new DataOutputStream(new DigestOutputStream(sink, digest))) {
            out.writeLong(form.getPolicyId());
            out.writeUTF(form.getName());
            out.writeUTF(form.getCaseTitle());
            out.writeUTF(form.getCaseDescription());
            out.writeUTF(date == null ? "" : date.toString());
            out.writeInt(files.length);
            byte[] buffer = new byte[8192];
            for (MultipartFile file : files) {
                String name = file.getOriginalFilename();
                out.writeUTF(name == null ? "" : name.substring(0, Math.min(name.length(), 1000)));
                out.writeLong(file.getSize());
                try (InputStream in = file.getInputStream()) {
                    for (int read; (read = in.read(buffer)) != -1;) out.write(buffer, 0, read);
                }
            }
            // 老客户端未传报案人时沿用原有摘要，允许已有请求安全重试。
            if (form.getReporterName() != null) out.writeUTF(form.getReporterName());
        }
        StringBuilder hex = new StringBuilder(64);
        for (byte value : digest.digest()) hex.append(Character.forDigit((value >> 4) & 15, 16))
                .append(Character.forDigit(value & 15, 16));
        return hex.toString();
    }
}
