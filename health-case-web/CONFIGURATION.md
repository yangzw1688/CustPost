# 配置文件使用说明

仓库中的配置文件是部署模板。真实连接信息放在运行目录的 `config/`，或通过环境变量提供。

配置文件统一使用 **UTF-8（无 BOM）**。若注释显示为 `ã`、`å¿` 等字符，是编辑器按其他编码读取了 UTF-8 文件，请以 UTF-8 **重新加载**文件，不要将当前乱码另存为新编码。IDEA 在 `Settings → Editor → File Encodings` 中将 Project Encoding 和 Default encoding for properties files 均设为 UTF-8，关闭 Transparent native-to-ascii conversion；已有打开的文件在右下角选择 UTF-8 后选择 **Reload**，或关闭后重新打开项目。VS Code 使用右下角编码菜单的“通过编码重新打开 → UTF-8”。项目已补充 IDEA、EditorConfig 与 VS Code 编码设置；无需为此修改数据库或 OBS 参数。

全部配置文件已放入 `health-case-web/src/main/resources`：

```text
resources/
  application.properties                 公共配置模板
  application-standalone.properties      独立部署配置模板（从环境读取连接信息）
  nacos/
    bootstrap.properties                 Nacos 连接模板
    health-case-web-prod.yaml             发布到 Nacos 的业务配置模板
  db/ui/ui.sql                              ui 表建表脚本
```

两种部署方式任选一种，**无需两套都填写**。OBS Endpoint 与桶所在区域保持一致。

| 文件 | 放在哪里 | 手动填写内容 |
| --- | --- | --- |
| `nacos/health-case-web-prod.yaml` | 复制完整内容到 Nacos 控制台并发布 | 数据库连接、OBS 桶与 AK/SK、Cookie 访问方式；按需填页面来源、界面管理员手机号 |
| `nacos/bootstrap.properties` | 作为模板由 `-Pnacos` 构建打包；实际连接使用环境变量或 `config/bootstrap.properties` | Nacos 地址、命名空间 ID；开启鉴权时填 Nacos 账号密码 |
| `application-standalone.properties` | 真实值放在运行目录的 `config/application-standalone.properties` | 数据库、OBS 等业务配置；支持环境变量覆盖 |
| `application.properties` | 留在应用本地，作为公共基础配置 | 通常不改；不是需要上传到 Nacos 的文件 |
| `db/ui/ui.sql` | 留在项目内或按需手工在数据库执行 | 不上传到 Nacos；只创建界面配置表 |

## Nacos 模式的手动替换清单

设置 Nacos 环境变量，或把连接模板复制为 `config/bootstrap.properties` 后填写。

| 本地连接项 | 是否必须 | 你需要提供的值 |
| --- | --- | --- |
| `NACOS_SERVER_ADDR` | 必须 | Nacos 配置服务主机和实际端口，例如 `主机:8848`；不是本项目的 8099 |
| `NACOS_NAMESPACE` | 使用自定义命名空间时必须 | 控制台复制的 Namespace **ID**；Public 留空，不填写显示名称 |
| `NACOS_GROUP` | 默认不用改 | 默认 `HEALTH_CASE`；修改时必须与控制台配置的 Group 一致 |
| `NACOS_USERNAME` / `NACOS_PASSWORD` | 开启 Nacos 鉴权时必须 | Nacos 登录凭据；不填数据库账号或 OBS 密钥 |

远程 `nacos/health-case-web-prod.yaml` 中需要替换的内容如下，实际值直接写在 YAML 中即可，不要求额外配置 DB_URL 等环境变量。

| 标记或配置项 | 是否必须 | 替换说明 |
| --- | --- | --- |
| `DB_URL` | 必须 | 数据库主机、已存在的数据库名；非 3306 端口时同时修改端口 |
| `DB_USERNAME` / `DB_PASSWORD` | 必须 | 数据库账号、密码 |
| `obs.endpoint` | 必须检查 | 填写桶所在区域的 HTTPS Endpoint |
| `OBS_BUCKET` | 必须 | OBS 桶名称，不能填完整 URL |
| `OBS_ACCESS_KEY` / `OBS_SECRET_KEY` | 必须 | 有该桶权限的华为云 AK / SK |
| `server.servlet.session.cookie.secure` | 必须检查 | HTTP 访问设 `false`；HTTPS 访问设 `true` |
| `CORS_ALLOWED_ORIGINS` | 跨域时必须 | 页面来源（协议+主机+端口），不带路径；同源单端口可统一填实际来源，例如 `http://localhost:8099` |
| `UI_ADMIN_PHONES` | 维护界面图片时必须 | 允许操作轮播的登录手机号，多个用英文逗号分隔；不需要时设为空字符串 `''` |
| `app.ui.initialize-schema` | 按实际情况 | 有 CREATE 权限且需自动建 ui 表时为 `true`；已手工建表时可设 `false` |
| `file.upload-path` | 存在历史本地附件时 | 应用服务器上的实际文件目录；新上传使用 OBS，与此目录无关 |

应用名 `health-case-web`、profile `prod`、格式 `yaml` 通常保持不变，这三项对应 Data ID **health-case-web-prod.yaml**。端口 8099、连接池、Mapper 配置、签名链接时长及固定验证码开关通常不需要改。

本项目只使用 Nacos 配置中心，没有引入 Nacos 服务注册发现。`-Pnacos` 是 **Maven 构建选项**，不能用 `--spring.profiles.active=nacos` 替代。

## 独立运行配置

把 `src/main/resources/application-standalone.properties` 复制到运行目录的 `config/application-standalone.properties` 后填写数据库和 OBS。也可以直接提供对应的环境变量。公共本地覆盖可写在 `config/application.properties`。

在项目根目录构建：

```powershell
mvn clean package
```

构建会自动编译用户端和管理端，生成包含页面与依赖的可执行 JAR。在项目根目录启动：

```powershell
java -jar health-case-web/target/health-case.jar
```

JAR 包含模板。IDE 和命令行默认加载 standalone。真实配置由 Spring 从运行目录的 `config/` 或环境变量读取，修改外置配置后重启应用即可。

连接配置缺失时，应用会在启动时提示具体配置项。首次部署前应准备实际数据库和业务表。

## Nacos 部署

1. 通过环境变量或 `config/bootstrap.properties` 设置 Nacos 的地址、Namespace ID、Group 和凭据。
2. 把 `src/main/resources/nacos/health-case-web-prod.yaml` 复制到 `config/nacos/health-case-web-prod.yaml`，按需填写实际值。在 Nacos 的目标命名空间中新建 Data ID **health-case-web-prod.yaml**，Group **HEALTH_CASE**，格式 **YAML**，将文件完整内容粘贴并发布。不要将 bootstrap.properties 或 application*.properties 一起上传。
3. 在项目根目录执行：

```powershell
mvn clean package -Pnacos
```

4. 在项目根目录启动。如果连接参数仍使用占位符，先设置相应环境变量：

```powershell
$env:NACOS_SERVER_ADDR = '你的Nacos地址:8848'
$env:NACOS_NAMESPACE = '线上命名空间的ID'
$env:NACOS_GROUP = 'HEALTH_CASE'
$env:NACOS_USERNAME = '你的Nacos账号'
$env:NACOS_PASSWORD = '你的Nacos密码'
java -jar health-case-web/target/health-case.jar
```

普通构建不含 Nacos 客户端，`nacos/bootstrap.properties` 只是 resources 的嵌套文件，不会自动启用。`-Pnacos` 构建会加入 Nacos Config 2.2.6.RELEASE 客户端，并将同一份 bootstrap 配置额外复制到 JAR 根目录，确保当前 Spring Boot 2.3.2 在启动时加载它。默认 profile 为 prod，按应用名读取 health-case-web-prod.yaml。

本地 resources 中的 Nacos YAML 是待发布内容，放进代码不会自动上传到配置中心；应用运行时仍从 Nacos 服务读取已发布内容。Namespace 填 ID 而非显示名称，Public 可留空；未启用鉴权时账号密码可留空。配置发布后重启后端生效。

修改 Nacos 配置后发布并重启。修改外置 bootstrap 文件或环境变量后重启即可。切换到 Nacos 构建时需启用 Maven 的 nacos profile 并重新编译。

切换构建方式请使用 clean，避免 target 中残留另一方式的 bootstrap 文件和依赖。JAR 内已包含全部运行依赖，不再需要外部 lib 目录。生成的 ZIP 只是 JAR 与配置说明的便利副本。

两种构建方式都只启动一个应用端口（默认 8099）：用户端 `/`，管理端 `/admin/`，接口 `/api/**`。无需再启动两个 Vue 开发服务器。配置数据库和 OBS 后即可使用完整业务功能。

## 本项目功能设置

- standalone 默认 Cookie 允许本地 HTTP；线上 HTTPS 设置 `SESSION_COOKIE_SECURE=true`。Nacos 线上 YAML 模板仍使用 secure=true。
- 仅在本地测试配置 `AUTH_TEST_CODE_ENABLED=true` 和六位数字 `AUTH_TEST_CODE`。管理端使用手机号与管理员密码。
- 管理员密码哈希保存在 `admin_permission.password_hash`。首次初始化时设置 `ADMIN_ROOT_PHONE` 和 `ADMIN_ROOT_PASSWORD`，初始化后可移除初始密码。保留主管理员手机号配置以识别主账号。
- 启动时按需创建 `admin_permission` 表，并为旧表补齐模块权限列。首次创建可由数据库管理员执行 `db/auth/admin_permissions.sql`。升级旧表需要应用账号具备 `ALTER` 权限，或由数据库管理员先补齐该脚本定义的模块权限列，再设置 `ADMIN_INITIALIZE_SCHEMA=false`。
- `app.ui.initialize-schema=true` 时首次启动创建不存在的 ui 表。已手动执行 db/ui/ui.sql 或应用账号没有 CREATE 权限时可设为 false。
- 启动后在管理端“平台管理 → 界面图片”点击“导入原有首页图片”。OBS 权限需覆盖 ui/* 和 case/* 的上传、读取、删除及设置对象 ACL。
- ZIP 的 config-templates 目录提供模板和说明。真实配置由运行目录的 `config/` 或环境变量提供。

参考：[Spring Cloud Alibaba 2.2.x 官方 Nacos 配置说明](https://sca.aliyun.com/en/docs/2.2.x/user-guide/nacos/advanced-guide/)。
