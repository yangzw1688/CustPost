package com.kaiyun.modules.auth.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
@Order(0)
@ConditionalOnProperty(name = "app.admin.initialize-schema", matchIfMissing = true)
public class AdminPermissionSchemaInitializer implements ApplicationRunner {
    private final DataSource dataSource;

    public AdminPermissionSchemaInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            Set<String> columns = new HashSet<>();
            try (ResultSet rows = connection.getMetaData().getColumns(connection.getCatalog(), null, "admin_permission", "%")) {
                while (rows.next()) {
                    if ("admin_permission".equalsIgnoreCase(rows.getString("TABLE_NAME"))) {
                        columns.add(rows.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
                    }
                }
            }
            if (columns.isEmpty()) {
                try (ResultSet rows = connection.getMetaData().getColumns(connection.getCatalog(), null, "ADMIN_PERMISSION", "%")) {
                    while (rows.next()) {
                        if ("admin_permission".equalsIgnoreCase(rows.getString("TABLE_NAME"))) {
                            columns.add(rows.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
                        }
                    }
                }
            }
            if (columns.isEmpty()) {
                new ResourceDatabasePopulator(new ClassPathResource("db/auth/admin_permissions.sql"))
                        .populate(connection);
                return;
            }
            List<String> additions = new ArrayList<>();
            for (String column : new String[] {"dashboard_access", "case_access", "policy_access",
                    "project_access", "platform_access", "user_access", "super_admin"}) {
                if (!columns.contains(column)) additions.add(column);
            }
            // 单列迁移兼容 MySQL 与临时测试库，已存在的授权列保持原值。
            try (Statement statement = connection.createStatement()) {
                for (String column : additions) {
                    try {
                        String definition = "super_admin".equals(column) ? " BOOLEAN NOT NULL DEFAULT FALSE" : " BOOLEAN";
                        statement.executeUpdate("ALTER TABLE admin_permission ADD COLUMN " + column + definition);
                    } catch (SQLException error) {
                        if (!"42S21".equals(error.getSQLState())) throw error;
                    }
                }
            }
        }
    }
}
