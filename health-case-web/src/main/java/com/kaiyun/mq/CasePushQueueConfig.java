package com.kaiyun.mq;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

@Configuration
@EnableScheduling
class CasePushQueueConfig {
    @Bean
    @ConditionalOnProperty(name = "app.case-tasks.initialize-schema", matchIfMissing = true)
    ApplicationRunner initializeCasePushQueue(DataSource dataSource) {
        return args -> {
            new ResourceDatabasePopulator(new ClassPathResource("db/messaging/case_push_queue.sql")).execute(dataSource);
            try (Connection connection = dataSource.getConnection()) {
                addColumn(connection, "dispatch_after", "DATETIME NULL");
                addColumn(connection, "receiver_fingerprint", "VARCHAR(64) NULL");
                addColumn(connection, "task_order", "BIGINT NOT NULL AUTO_INCREMENT UNIQUE");
                addColumn(connection, "trigger_type", "VARCHAR(20) NOT NULL DEFAULT 'MANUAL'");
                addColumn(connection, "case_status", "VARCHAR(20) NULL");
                migrateLegacyKey(connection);
            }
        };
    }

    private void addColumn(Connection connection, String column, String definition) throws SQLException {
        if (hasColumn(connection, column)) return;
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE case_push_task ADD COLUMN " + column + " " + definition);
        } catch (SQLException error) {
            if (!"42S21".equals(error.getSQLState())) throw error;
        }
    }

    private void migrateLegacyKey(Connection connection) throws SQLException {
        if (!caseIdIsPrimaryKey(connection)) return;
        try (Statement statement = connection.createStatement()) {
            try {
                statement.executeUpdate("CREATE INDEX idx_case_push_case_order ON case_push_task(case_id, task_order)");
            } catch (SQLException error) {
                if (error.getErrorCode() != 1061 && !"42S11".equals(error.getSQLState())) throw error;
            }
            String product = connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
            // MySQL 原子切换主键，保留历史任务 ID 及收到的回执。
            if (product.contains("mysql") || product.contains("mariadb")) {
                statement.executeUpdate("ALTER TABLE case_push_task DROP PRIMARY KEY, ADD PRIMARY KEY (task_id)");
            } else {
                statement.executeUpdate("ALTER TABLE case_push_task DROP PRIMARY KEY");
                statement.executeUpdate("ALTER TABLE case_push_task ADD PRIMARY KEY (task_id)");
            }
        }
    }

    private boolean caseIdIsPrimaryKey(Connection connection) throws SQLException {
        for (String table : new String[]{"case_push_task", "CASE_PUSH_TASK"}) {
            try (ResultSet keys = connection.getMetaData().getPrimaryKeys(connection.getCatalog(), null, table)) {
                while (keys.next()) if ("case_id".equalsIgnoreCase(keys.getString("COLUMN_NAME"))) return true;
            }
        }
        return false;
    }

    private boolean hasColumn(Connection connection, String column) throws SQLException {
        for (String table : new String[]{"case_push_task", "CASE_PUSH_TASK"}) {
            try (ResultSet rows = connection.getMetaData().getColumns(connection.getCatalog(), null, table, "%")) {
                while (rows.next()) {
                    if ("case_push_task".equalsIgnoreCase(rows.getString("TABLE_NAME"))
                            && column.equalsIgnoreCase(rows.getString("COLUMN_NAME"))) return true;
                }
            }
        }
        return false;
    }
}
