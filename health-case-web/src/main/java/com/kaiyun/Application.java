package com.kaiyun;

import com.kaiyun.infrastructure.database.DatabaseConfigurationValidator;
import com.kaiyun.modules.cases.mapper.ServiceRecordMapper;
import com.kaiyun.modules.policy.mapper.ServicePolicyMapper;
import com.kaiyun.modules.ui.mapper.UiMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.util.Collections;

@SpringBootApplication
// 按业务模块注册 Mapper，以类定位包，避免包名调整后扫描失效。
@MapperScan(basePackageClasses = {ServiceRecordMapper.class, ServicePolicyMapper.class, UiMapper.class})
public class Application {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Application.class);
        // 默认读取本地 standalone 配置；显式启用 Nacos 构建时由 bootstrap 指定 prod。
        application.setDefaultProperties(Collections.singletonMap("spring.profiles.default", "standalone"));
        application.addInitializers(new DatabaseConfigurationValidator());
        application.run(args);
    }
}
