package com.kaiyun.modules.ui.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;

@Component
public class UiSchemaInitializer implements ApplicationRunner {
    private final DataSource dataSource;
    private final boolean enabled;
    public UiSchemaInitializer(DataSource dataSource, @Value("${app.ui.initialize-schema:true}") boolean enabled) {
        this.dataSource = dataSource;
        this.enabled = enabled;
    }
    @Override public void run(ApplicationArguments args) throws Exception {
        if (!enabled) return;
        // 仅首次建表时写入默认配置，重启时不恢复管理员已经删除的内容。
        try (Connection connection = dataSource.getConnection();
             ResultSet tables = connection.getMetaData().getTables(connection.getCatalog(), null, "ui", new String[] {"TABLE"})) {
            if (!tables.next()) {
                ResourceDatabasePopulator populator = new ResourceDatabasePopulator(new ClassPathResource("db/ui/ui.sql"));
                populator.setSqlScriptEncoding("UTF-8");
                populator.execute(dataSource);
            }
        }
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(new ClassPathResource("db/ui/ui_page.sql"));
        populator.setSqlScriptEncoding("UTF-8");
        populator.execute(dataSource);
    }
}
