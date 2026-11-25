package com.kaiyun.modules.dashboard.controller;

import com.kaiyun.common.web.Result;
import com.kaiyun.modules.auth.service.AdminPermissionService;
import lombok.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

import static com.kaiyun.modules.auth.service.AdminModules.DASHBOARD;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final JdbcTemplate jdbc;
    private final AdminPermissionService permissions;

    public DashboardController(JdbcTemplate jdbc, AdminPermissionService permissions) {
        this.jdbc = jdbc;
        this.permissions = permissions;
    }

    @GetMapping("/summary")
    public Result<Summary> summary(HttpServletRequest request) {
        permissions.requireModule(request, DASHBOARD);
        Summary summary = jdbc.queryForObject(
                "SELECT COUNT(*) AS total, "
                        + "COALESCE(SUM(CASE WHEN r.status IN ('PENDING','PROCESSING') AND pr.case_id IS NULL THEN 1 ELSE 0 END),0) AS pending, "
                        + "COALESCE(SUM(CASE WHEN r.status = 'PROCESSING' THEN 1 ELSE 0 END),0) AS processing, "
                        + "COALESCE(SUM(CASE WHEN r.status = 'COMPLETED' THEN 1 ELSE 0 END),0) AS completed, "
                        + "(SELECT COUNT(*) FROM service_policy) AS policies "
                        + "FROM service_record r LEFT JOIN case_payment_review pr ON pr.case_id = r.id",
                (row, index) -> new Summary(row.getLong("total"), row.getLong("pending"),
                        row.getLong("processing"), row.getLong("completed"), row.getLong("policies")));
        return Result.success(summary);
    }

    @Value
    public static class Summary {
        long total;
        long pending;
        long processing;
        long completed;
        long policies;
    }
}

