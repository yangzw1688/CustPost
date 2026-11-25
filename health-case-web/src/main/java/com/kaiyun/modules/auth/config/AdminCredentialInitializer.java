package com.kaiyun.modules.auth.config;

import com.kaiyun.modules.auth.service.AdminPermissionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class AdminCredentialInitializer implements ApplicationRunner {
    private final AdminPermissionService permissions;
    private final String initialPassword;

    public AdminCredentialInitializer(AdminPermissionService permissions,
            @Value("${app.admin.initial-password:}") String initialPassword) {
        this.permissions = permissions;
        this.initialPassword = initialPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        permissions.initializeRoot(initialPassword);
    }
}
