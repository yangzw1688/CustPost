package com.kaiyun.modules.user.service;

import com.kaiyun.common.exception.BusinessException;
import com.kaiyun.common.pagination.AdminListPage;
import com.kaiyun.modules.user.dto.UserQuery;
import com.kaiyun.modules.user.vo.CustomerUserVO;
import com.kaiyun.modules.user.vo.UserCaseVO;
import com.kaiyun.modules.user.vo.UserPolicyVO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.sql.Timestamp;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

@Service
public class CustomerUserService {
    // UNION 按手机号去重，历史联系人无需制造登录记录，也能在管理端查询。
    private static final String PHONES = "SELECT phone FROM customer_user UNION SELECT phone FROM service_record "
            + "UNION SELECT applicant_phone AS phone FROM service_policy UNION SELECT insured_phone AS phone FROM service_policy";
    private static final String BASE = " FROM (" + PHONES + ") u LEFT JOIN customer_user a ON a.phone = u.phone";
    private static final String VALID_PHONE = " u.phone REGEXP '^1[3-9][0-9]{9}$'";
    private static final String COUNTS = " LEFT JOIN (SELECT phone, COUNT(*) AS case_count, "
            + "SUM(CASE WHEN status IN ('PENDING','PROCESSING') THEN 1 ELSE 0 END) AS pending_count, "
            + "SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed_count, MAX(create_time) AS latest_case "
            + "FROM service_record GROUP BY phone) c ON c.phone = u.phone "
            + "LEFT JOIN (SELECT phone, COUNT(*) AS policy_count FROM (SELECT id, applicant_phone AS phone FROM service_policy "
            + "UNION SELECT id, insured_phone AS phone FROM service_policy) contacts GROUP BY phone) p ON p.phone = u.phone";
    private static final String COLUMNS = "SELECT u.phone, a.first_login_at, a.last_login_at, COALESCE(a.login_count,0) AS login_count, "
            + "COALESCE(c.case_count,0) AS case_count, COALESCE(c.pending_count,0) AS pending_count, "
            + "COALESCE(c.completed_count,0) AS completed_count, c.latest_case, COALESCE(p.policy_count,0) AS policy_count";
    private final JdbcTemplate jdbc;
    private final RowMapper<CustomerUserVO> userMapper = (row, index) -> {
        CustomerUserVO user = new CustomerUserVO();
        user.setPhone(row.getString("phone"));
        Timestamp first = row.getTimestamp("first_login_at");
        Timestamp last = row.getTimestamp("last_login_at");
        Timestamp latest = row.getTimestamp("latest_case");
        user.setRegistered(first != null);
        user.setFirstLoginAt(first == null ? null : first.toLocalDateTime());
        user.setLastLoginAt(last == null ? null : last.toLocalDateTime());
        user.setLatestCaseAt(latest == null ? null : latest.toLocalDateTime());
        user.setLoginCount(row.getLong("login_count"));
        user.setPolicyCount(row.getLong("policy_count"));
        user.setCaseCount(row.getLong("case_count"));
        user.setPendingCaseCount(row.getLong("pending_count"));
        user.setCompletedCaseCount(row.getLong("completed_count"));
        return user;
    };

    public CustomerUserService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    /** 原子递增成功登录次数；发送验证码和登录失败不创建用户档案。 */
    @Transactional
    public void recordLogin(String phone) {
        jdbc.update("INSERT INTO customer_user (phone, first_login_at, last_login_at, login_count) "
                + "VALUES (?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1) ON DUPLICATE KEY UPDATE "
                + "last_login_at = CURRENT_TIMESTAMP, login_count = login_count + 1", phone);
    }

    @Transactional(readOnly = true)
    public AdminListPage<CustomerUserVO> page(UserQuery query) {
        query.validate();
        StringBuilder where = new StringBuilder(" WHERE " + VALID_PHONE);
        List<Object> args = new ArrayList<>();
        if (!query.getPhone().isEmpty()) { where.append(" AND LOCATE(?, u.phone) > 0"); args.add(query.getPhone()); }
        if ("LOGIN".equals(query.getSource())) where.append(" AND a.phone IS NOT NULL");
        if ("BUSINESS".equals(query.getSource())) where.append(" AND a.phone IS NULL");
        if (query.getFrom() != null) { where.append(" AND a.last_login_at >= ?"); args.add(Timestamp.valueOf(query.getFrom().atStartOfDay())); }
        if (query.getTo() != null) { where.append(" AND a.last_login_at < ?"); args.add(Timestamp.valueOf(query.getTo().plusDays(1).atStartOfDay())); }
        long total = jdbc.queryForObject("SELECT COUNT(*)" + BASE + where, Long.class, args.toArray());
        int page = query.pageWithin(total);
        args.add(query.getSize()); args.add((page - 1L) * query.getSize());
        List<CustomerUserVO> rows = jdbc.query(COLUMNS + BASE + COUNTS + where
                + " ORDER BY CASE WHEN a.last_login_at IS NULL THEN 1 ELSE 0 END, a.last_login_at DESC, u.phone ASC LIMIT ? OFFSET ?",
                userMapper, args.toArray());
        return new AdminListPage<>(page, total, rows);
    }

    @Transactional(readOnly = true)
    public CustomerUserVO detail(String phone) {
        validatePhone(phone);
        List<CustomerUserVO> rows = jdbc.query(COLUMNS + BASE + COUNTS + " WHERE u.phone = ?", userMapper, phone);
        if (rows.isEmpty()) throw new BusinessException(404, "用户不存在");
        return rows.get(0);
    }

    @Transactional(readOnly = true)
    public AdminListPage<UserPolicyVO> policies(String phone, UserQuery query) {
        requireExists(phone);
        long total = jdbc.queryForObject("SELECT COUNT(*) FROM service_policy WHERE applicant_phone = ? OR insured_phone = ?", Long.class, phone, phone);
        int page = query.pageWithin(total);
        List<UserPolicyVO> rows = jdbc.query("SELECT id, policy_no, product_name, applicant_name, insured_name, applicant_phone, "
                + "insured_phone, status, start_date, end_date FROM service_policy WHERE applicant_phone = ? OR insured_phone = ? "
                + "ORDER BY create_time DESC, id DESC LIMIT ? OFFSET ?", (row, index) -> {
            UserPolicyVO value = new UserPolicyVO();
            value.setId(row.getLong("id")); value.setPolicyNo(row.getString("policy_no"));
            value.setProductName(row.getString("product_name")); value.setApplicantName(row.getString("applicant_name"));
            value.setInsuredName(row.getString("insured_name")); value.setStatus(row.getString("status"));
            boolean applicant = phone.equals(row.getString("applicant_phone"));
            boolean insured = phone.equals(row.getString("insured_phone"));
            value.setRelation(applicant && insured ? "投保人及被保险人" : applicant ? "投保人" : "被保险人");
            Date start = row.getDate("start_date"), end = row.getDate("end_date");
            value.setStartDate(start == null ? null : start.toLocalDate()); value.setEndDate(end == null ? null : end.toLocalDate());
            return value;
        }, phone, phone, query.getSize(), (page - 1L) * query.getSize());
        return new AdminListPage<>(page, total, rows);
    }

    @Transactional(readOnly = true)
    public AdminListPage<UserCaseVO> cases(String phone, UserQuery query) {
        requireExists(phone);
        long total = jdbc.queryForObject("SELECT COUNT(*) FROM service_record WHERE phone = ?", Long.class, phone);
        int page = query.pageWithin(total);
        List<UserCaseVO> rows = jdbc.query("SELECT r.id, r.name, r.case_title, r.status, r.visit_date, r.create_time, p.policy_no "
                + "FROM service_record r LEFT JOIN service_policy p ON p.id = r.policy_id WHERE r.phone = ? "
                + "ORDER BY r.create_time DESC, r.id DESC LIMIT ? OFFSET ?", (row, index) -> {
            UserCaseVO value = new UserCaseVO();
            value.setId(row.getLong("id")); value.setName(row.getString("name")); value.setCaseTitle(row.getString("case_title"));
            value.setStatus(row.getString("status")); value.setPolicyNo(row.getString("policy_no"));
            Date visit = row.getDate("visit_date"); Timestamp created = row.getTimestamp("create_time");
            value.setVisitDate(visit == null ? null : visit.toLocalDate()); value.setCreateTime(created == null ? null : created.toLocalDateTime());
            return value;
        }, phone, query.getSize(), (page - 1L) * query.getSize());
        return new AdminListPage<>(page, total, rows);
    }

    private void requireExists(String phone) {
        validatePhone(phone);
        if (jdbc.queryForObject("SELECT COUNT(*)" + BASE + " WHERE u.phone = ?", Long.class, phone) == 0) {
            throw new BusinessException(404, "用户不存在");
        }
    }

    private static void validatePhone(String phone) {
        if (phone == null || !phone.matches("1[3-9][0-9]{9}")) throw new BusinessException(400, "用户手机号不正确");
    }
}
