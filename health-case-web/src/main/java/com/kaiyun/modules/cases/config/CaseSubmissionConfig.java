package com.kaiyun.modules.cases.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

@Configuration
public class CaseSubmissionConfig {
    @Bean
    @ConditionalOnProperty(name = "app.case-tasks.initialize-schema", matchIfMissing = true)
    public ApplicationRunner initializeCaseSubmission(DataSource dataSource) {
        return args -> new ResourceDatabasePopulator(new ClassPathResource("db/cases/case_submission.sql"),
                new ClassPathResource("db/cases/case_payment_review.sql")).execute(dataSource);
    }
}
