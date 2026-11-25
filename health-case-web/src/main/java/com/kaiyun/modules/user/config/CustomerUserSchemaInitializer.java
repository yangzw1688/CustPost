package com.kaiyun.modules.user.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;

@Component
@Order(0)
@ConditionalOnProperty(name = "app.user.initialize-schema", matchIfMissing = true)
public class CustomerUserSchemaInitializer implements ApplicationRunner {
    private final DataSource dataSource;

    public CustomerUserSchemaInitializer(DataSource dataSource) { this.dataSource = dataSource; }

    @Override
    public void run(ApplicationArguments args) {
        new ResourceDatabasePopulator(new ClassPathResource("db/user/customer_user.sql")).execute(dataSource);
    }
}
