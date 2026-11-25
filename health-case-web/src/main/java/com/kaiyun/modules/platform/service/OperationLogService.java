package com.kaiyun.modules.platform.service;

import com.kaiyun.common.exception.BusinessException;
import com.kaiyun.infrastructure.logging.PublicIpAddress;
import com.kaiyun.common.pagination.AdminListPage;
import com.kaiyun.modules.platform.dto.OperationLogQuery;
import com.kaiyun.modules.platform.vo.OperationLogVO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Service
public class OperationLogService {
    private final JdbcTemplate jdbc;
    private final RowMapper<OperationLogVO> mapper = (row, index) -> {
        OperationLogVO value = new OperationLogVO();
        value.setId(row.getLong("id"));
        value.setModule(row.getString("module"));
        value.setModuleName(row.getString("module_name"));
        value.setAction(row.getString("action"));
        value.setActionName(row.getString("action_name"));
        value.setOperator(row.getString("operator"));
        value.setTarget(row.getString("target"));
        value.setVersion(row.getObject("version", Long.class));
        value.setPublishedVersion(row.getObject("published_version", Long.class));
        value.setSuccessful(row.getBoolean("successful"));
        value.setHttpStatus(row.getInt("http_status"));
        value.setDurationMs(row.getLong("duration_ms"));
        value.setRequestMethod(row.getString("request_method"));
        value.setRequestPath(row.getString("request_path"));
        value.setClientIp(row.getString("client_ip"));
        String publicIp = PublicIpAddress.parse(row.getString("public_ip"));
        String source = row.getString("ip_source");
        if (publicIp == null) {
            publicIp = PublicIpAddress.parse(value.getClientIp());
            if (publicIp != null) source = "REQUEST";
        }
        value.setPublicIp(publicIp == null ? "" : publicIp);
        value.setIpSource(publicIp == null ? "UNAVAILABLE" : source);
        value.setDetails(row.getString("details"));
        value.setErrorMessage(row.getString("error_message"));
        value.setCreatedAt(row.getTimestamp("created_at").toLocalDateTime());
        return value;
    };

    public OperationLogService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    /** 独立事务保存操作结果，使被拒绝或已回滚的业务请求仍有审计记录。 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void append(OperationLogVO value) {
        jdbc.update("INSERT INTO platform_operation_log (module, module_name, action, action_name, operator, target, "
                        + "version, published_version, successful, http_status, duration_ms, request_method, request_path, "
                        + "client_ip, details, error_message, public_ip, ip_source) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                value.getModule(), value.getModuleName(), value.getAction(), value.getActionName(), value.getOperator(),
                value.getTarget(), value.getVersion(), value.getPublishedVersion(), value.isSuccessful(), value.getHttpStatus(),
                value.getDurationMs(), value.getRequestMethod(), value.getRequestPath(), value.getClientIp(),
                value.getDetails(), value.getErrorMessage(), value.getPublicIp(), value.getIpSource());
    }

    @Transactional(readOnly = true)
    public AdminListPage<OperationLogVO> page(OperationLogQuery query) {
        query.validate();
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        equal(where, args, "module", query.getModule());
        equal(where, args, "action", query.getAction());
        contains(where, args, "operator", query.getOperator());
        contains(where, args, "target", query.getTarget());
        if (!query.getResult().isEmpty()) {
            where.append(" AND successful = ?"); args.add("SUCCESS".equals(query.getResult()));
        }
        if (query.getFrom() != null) {
            where.append(" AND created_at >= ?"); args.add(Timestamp.valueOf(query.getFrom().atStartOfDay()));
        }
        if (query.getTo() != null) {
            where.append(" AND created_at < ?"); args.add(Timestamp.valueOf(query.getTo().plusDays(1).atStartOfDay()));
        }
        long total = jdbc.queryForObject("SELECT COUNT(*) FROM platform_operation_log" + where, Long.class, args.toArray());
        int page = query.pageWithin(total);
        args.add(query.getSize()); args.add((page - 1L) * query.getSize());
        List<OperationLogVO> rows = jdbc.query("SELECT * FROM platform_operation_log" + where
                + " ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?", mapper, args.toArray());
        return new AdminListPage<>(page, total, rows);
    }

    public OperationLogVO detail(long id) {
        if (id <= 0) throw new BusinessException(400, "日志编号不正确");
        List<OperationLogVO> values = jdbc.query("SELECT * FROM platform_operation_log WHERE id = ?", mapper, id);
        if (values.isEmpty()) throw new BusinessException(404, "操作日志不存在");
        return values.get(0);
    }

    private static void equal(StringBuilder where, List<Object> args, String column, String value) {
        if (!value.isEmpty()) { where.append(" AND ").append(column).append(" = ?"); args.add(value); }
    }

    private static void contains(StringBuilder where, List<Object> args, String column, String value) {
        if (!value.isEmpty()) { where.append(" AND LOCATE(?, ").append(column).append(") > 0"); args.add(value); }
    }
}
