package com.kaiyun.modules.project.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.sql.*;

@Component
@Order(0)
@ConditionalOnProperty(name = "app.project.initialize-schema", matchIfMissing = true)
public class ProjectSchemaInitializer implements ApplicationRunner {
    private final DataSource dataSource;
    public ProjectSchemaInitializer(DataSource dataSource) { this.dataSource = dataSource; }

    @Override
    public void run(ApplicationArguments args) throws SQLException {
        new ResourceDatabasePopulator(new ClassPathResource("db/project/project.sql")).execute(dataSource);
        try (Connection connection = dataSource.getConnection()) {
            String table = null;
            boolean hasProject = false;
            for (String candidate : new String[]{"service_policy", "SERVICE_POLICY"}) {
                try (ResultSet columns = connection.getMetaData().getColumns(connection.getCatalog(), null, candidate, "%")) {
                    while (columns.next()) {
                        if (!"service_policy".equalsIgnoreCase(columns.getString("TABLE_NAME"))) continue;
                        table = columns.getString("TABLE_NAME");
                        hasProject |= "project_id".equalsIgnoreCase(columns.getString("COLUMN_NAME"));
                    }
                }
            }
            if (table != null && !hasProject) {
                try (Statement statement = connection.createStatement()) {
                    try { statement.executeUpdate("ALTER TABLE service_policy ADD COLUMN project_id BIGINT NULL"); }
                    catch (SQLException error) { if (!"42S21".equals(error.getSQLState())) throw error; }
                }
            }
            if (table == null) return;
            boolean indexed = false;
            try (ResultSet indexes = connection.getMetaData().getIndexInfo(connection.getCatalog(), null, table, false, false)) {
                while (indexes.next()) indexed |= "project_id".equalsIgnoreCase(indexes.getString("COLUMN_NAME"));
            }
            if (!indexed) {
                try (Statement statement = connection.createStatement()) {
                    try { statement.executeUpdate("CREATE INDEX idx_policy_project ON service_policy(project_id)"); }
                    catch (SQLException error) { if (error.getErrorCode() != 1061 && !"42S11".equals(error.getSQLState())) throw error; }
                }
            }
        }
    }
}
