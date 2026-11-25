package com.kaiyun.modules.platform.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

@Component
@Order(0)
@ConditionalOnProperty(name = "app.audit.initialize-schema", matchIfMissing = true)
public class OperationLogSchemaInitializer implements ApplicationRunner {
    private final DataSource dataSource;

    public OperationLogSchemaInitializer(DataSource dataSource) { this.dataSource = dataSource; }

    @Override
    public void run(ApplicationArguments args) throws SQLException {
        ResourceDatabasePopulator script = new ResourceDatabasePopulator(new ClassPathResource("db/platform/operation_log.sql"));
        script.setSqlScriptEncoding("UTF-8");
        script.execute(dataSource);
        // 兼容已有日志表，仅增加缺失的 IP 字段，不改写或删除历史记录。
        try (Connection connection = dataSource.getConnection()) {
            Set<String> columns = new HashSet<>();
            for (String table : new String[]{"platform_operation_log", "PLATFORM_OPERATION_LOG"}) {
                try (ResultSet rows = connection.getMetaData().getColumns(connection.getCatalog(), null, table, "%")) {
                    while (rows.next()) {
                        if ("platform_operation_log".equalsIgnoreCase(rows.getString("TABLE_NAME"))) columns.add(rows.getString("COLUMN_NAME").toLowerCase(java.util.Locale.ROOT));
                    }
                }
            }
            try (Statement statement = connection.createStatement()) {
                if (!columns.contains("public_ip")) add(statement, "public_ip VARCHAR(64) NOT NULL DEFAULT ''");
                if (!columns.contains("ip_source")) add(statement, "ip_source VARCHAR(16) NOT NULL DEFAULT 'UNAVAILABLE'");
            }
        }
    }

    private static void add(Statement statement, String definition) throws SQLException {
        try { statement.executeUpdate("ALTER TABLE platform_operation_log ADD COLUMN " + definition); }
        catch (SQLException error) { if (!"42S21".equals(error.getSQLState())) throw error; }
    }
}
