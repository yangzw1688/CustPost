package com.kaiyun.infrastructure.database;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import java.util.Arrays;

/** 在创建连接池和启动 Web 服务前检查配置，避免把未替换的占位符交给驱动。 */
public class DatabaseConfigurationValidator implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext context) {
        Environment environment = context.getEnvironment();
        for (String key : new String[]{"spring.datasource.url", "spring.datasource.username", "spring.datasource.password"}) {
            String value;
            try {
                value = environment.getProperty(key);
            } catch (IllegalArgumentException exception) {
                throw missing(key, environment);
            }
            if (value == null || value.contains("${") || value.contains("CHANGE_ME")
                    || !key.endsWith("password") && value.trim().isEmpty()) {
                throw missing(key, environment);
            }
        }
    }

    private IllegalStateException missing(String key, Environment environment) {
        // 只输出配置项名称，不记录数据库地址、账号或密码的实际值。
        String[] profiles = environment.getActiveProfiles().length == 0
                ? environment.getDefaultProfiles() : environment.getActiveProfiles();
        if (!Arrays.asList(profiles).contains("standalone")) {
            String serverAddress;
            try {
                serverAddress = environment.getProperty("spring.cloud.nacos.config.server-addr");
            } catch (IllegalArgumentException exception) {
                serverAddress = null;
            }
            if (serverAddress == null || serverAddress.trim().isEmpty()
                    || serverAddress.contains("${") || serverAddress.contains("CHANGE_ME")) {
                return new IllegalStateException("未提供 Nacos 连接地址，无法读取线上数据库配置。请在 "
                        + "health-case-web/src/main/resources/nacos/bootstrap.properties 中填写 "
                        + "spring.cloud.nacos.config.server-addr，或在 IDEA 运行配置的环境变量中设置 "
                        + "NACOS_SERVER_ADDR=实际主机:端口。自定义命名空间及启用鉴权时，"
                        + "还需填写 Namespace ID 和 Nacos 账号密码。修改本地文件后重新编译；"
                        + "IDEA 请重新加载 Maven，并确认 target/classes/bootstrap.properties 已生成。");
            }
            return new IllegalStateException("数据库连接配置未完成（" + key + "），当前 profile="
                    + Arrays.toString(profiles) + "。当前使用 Nacos 配置模式，请检查本地 Nacos 连接参数，"
                    + "并在对应命名空间发布 health-case-web-prod.yaml（Group 默认 HEALTH_CASE），"
                    + "填写真实的 spring.datasource 配置。发布后重启应用，无需填写本地 standalone 文件。");
        }
        return new IllegalStateException("数据库连接配置未完成（" + key + "），当前使用 standalone 本地模式。请填写 "
                + "health-case-web/src/main/resources/application-standalone.properties 中的 CHANGE_ME 项，"
                + "或设置 DB_URL、DB_USERNAME、DB_PASSWORD 环境变量。修改 resources 后请重新构建再启动。");
    }
}
