# 创新支付服务平台
适用于医疗类消费理赔的创新支付系统

创新支付（Innovative Payment）是指在传统支付体系基础上，通过支付技术、资金来源、支付模式及结算机制的创新，整合医疗机构、保险公司、药企及第三方服务平台等多方资源，构建更加灵活、高效、多元化的支付体系。在医疗健康及商业保险领域，主要通过商保直付、多方联合支付、按疗效付费、健康权益抵扣等方式，降低患者医疗费用负担和资金垫付压力，提高医疗服务及创新药品的可及性，实现医疗、医药、保险与患者之间的资源协同和价值共享。


启动项目前需要配置数据库、华为云obs、rabbitmq、阿里云短信服务当前暂未完全配置，若为学习使用可切换为QQ邮箱发送，内部不涉及支付功能

用户端、管理端和 Java 后端是三个平级项目。两个 Vue 项目各自维护源码、依赖锁文件和构建配置，可以独立运行与打包。根目录也提供将两端构建产物合入一个 JAR 的部署方式。

## 2026-11-24 复原记录

当前目录根据归档的原始源码、补丁和浏览器缓存重建。部分用户端和管理端页面重新实现，依赖锁文件从本机离线缓存重新生成。用户端轮播素材从下载目录找回，并同时放回前端的公共资源目录和后端的素材导入目录。

- 两个 Vue 前端分别启动时，用户端为 `http://localhost:8081/`，管理端为 `http://localhost:8082/admin/`。Java 后端默认端口为 `8099`。
- 已恢复病例资料提交、PDF 和图片附件规则、保单查询、领款人审核、线下赔付确认及已赔付台账。用户端页面可通过管理端可视化编辑器维护，并在发布前预览。缺少首页配置的数据库会初始化默认首页。
- 已使用隔离的 H2 内存数据库和合成数据验证两端静态资源、登录、病例详情、审核、赔付确认及台账。真实 MySQL、OBS、RabbitMQ 和 Nacos 仍需按部署环境联调。
- 原有数据库和附件数据需从原部署环境或备份取得。现存 SQL 主要覆盖增量配置表，首次运行应复用已有的业务表结构。

在项目根目录使用 Java 8、Maven 和 Node.js 构建。现有 `yarn.lock` 用于安装两端依赖。

```powershell
mvn.cmd clean package
java -jar .\health-case-web\target\health-case.jar
```

启动前配置 `DB_URL`、`DB_USERNAME` 和 `DB_PASSWORD`。病例附件上传需要 `OBS_ENDPOINT`、`OBS_BUCKET`、`OBS_ACCESS_KEY` 和 `OBS_SECRET_KEY`。初始化主超级管理员需要 `ADMIN_ROOT_PHONE` 和 `ADMIN_ROOT_PASSWORD`。本地测试登录使用 `AUTH_TEST_CODE_ENABLED` 和 `AUTH_TEST_CODE`。

用户端默认使用真实短信登录。阿里云签名、模板和本地连接配置见[短信登录说明](health-case-web/LOGIN.md)。

真实连接信息可写在运行目录下的 `config/application-standalone.properties`，公共本地配置可写在 `config/application.properties`。两个文件均由 Git 忽略。如果本机 Maven 曾把仓库设在已删除的目录，可使用 `-Dmaven.repo.local=实际仓库路径` 覆盖。

## 代码目录

后端位于 `health-case-web/src/main/java/com/kaiyun`，按业务模块组织，同一功能的接口、业务处理、数据访问和数据对象放在一起：

```text
com/kaiyun/
├── Application.java          # 单体应用启动入口
├── modules/
│   ├── auth/                 # 用户登录、管理员登录、模块权限和账号初始化
│   ├── cases/                # 案件提交、详情、附件与推送入口
│   ├── policy/               # 保单查询、操作记录、Excel 导入与导出
│   ├── ui/                   # 用户端页面配置、发布、图片与轮播管理
│   ├── user/                 # 用户登录档案、用户目录与关联保单/案件查询
│   ├── project/              # 保险公司、分环境对接配置、项目及保单归属
│   └── platform/             # 平台操作日志的建表、持久化和只读查询
├── common/
│   ├── exception/            # 业务异常与统一异常响应
│   ├── pagination/           # 通用分页查询和结果
│   └── web/                  # 通用接口返回结构
├── infrastructure/
│   ├── database/             # 数据库连接配置检查
│   ├── logging/              # 平台操作采集、脱敏与控制台日志
│   ├── storage/              # 华为云 OBS 文件存储
│   └── web/                  # 登录拦截、跨域、静态页面与单端口路由
└── mq/                       # RabbitMQ 发布、消费、重试及接收接口
```

业务模块内部按需使用 `controller`、`service`、`service/impl`、`mapper`、`entity`、`dto`、`vo`、`config` 子目录。新增功能优先放入对应业务模块；公共分页、异常和存储能力使用现有公共目录。新增包含 Mapper 的业务模块时，在 `Application` 的 `@MapperScan` 中补充该模块的 Mapper 类。

SQL 在 `health-case-web/src/main/resources/db` 下按 `auth`、`cases`、`ui`、`user`、`project`、`platform`、`messaging` 分类；XML 映射在 `resources/mapper` 下按模块分类。运行配置仍在 `resources`，本地配置和 Nacos 模板的填写方式见下文。

用户端 `health-case-front/src/views` 按 `home`、`login`、`submit`、`record`、`policy`、`mine`、`ui`、`error` 分类，页面内容渲染组件放在 `src/components/ui`。管理端 `health-case-admin/src/views` 按业务模块分类，模块专用组件放在该模块的 `components` 中。两个项目各自在 `src/shared` 维护登录、HTTP 和附件工具。

## 独立构建和启动 Vue

在对应的 Vue 项目目录安装依赖，然后启动开发服务器。

```powershell
yarn.cmd install --frozen-lockfile
npm.cmd run serve
```

| 项目目录 | 前端入口 |
| --- | --- |
| `health-case-front` | http://localhost:8081/ |
| `health-case-admin` | http://localhost:8082/admin/ |

在任一项目目录运行 `npm.cmd run build`，会生成该项目的 `dist`。单独部署静态文件时，用户端挂载在 `/`，管理端挂载在 `/admin/`，各自的页面路由回退到对应的 `index.html`。两端把 `/api` 和 `/upload` 转发到 Java 后端，开发时可用 `VUE_APP_API_TARGET` 配置后端地址，默认为 `http://localhost:8099`。

## 合包部署

构建需要 JDK 8、Maven、Node.js 和 npm（已在 Node.js 22 下验证）。在本目录执行：

```powershell
mvn clean package
java -jar health-case-web/target/health-case.jar
```

Maven 会依次构建两个 Vue 应用，再将页面、后端和运行依赖一起打入 `health-case-web/target/health-case.jar`。首次构建缺少前端依赖时，会通过 npm 调用固定版本 Yarn 1.22.22，按现有 `yarn.lock` 安装，需要访问包仓库，无需预装全局 Yarn。如果以后改用 `package-lock.json`，则自动使用 `npm ci`。已有依赖升级后请在对应前端目录重新安装依赖。

默认部署使用 standalone。启动前在运行目录的 `config/application-standalone.properties` 中填写数据库和 OBS 信息，或提供对应的 DB_*、OBS_* 环境变量。IDE 重新加载 Maven 项目，取消之前手动勾选的 nacos profile，并清除运行参数或环境变量中遗留的 prod profile，再编译运行 Application。

| 入口 | 地址 |
| --- | --- |
| 用户端 | http://localhost:8099/ |
| 管理端 | http://localhost:8099/admin/ |
| 后端接口 | http://localhost:8099/api/… |

部署机器只需要 Java、这个 JAR 以及可用的数据库和 OBS 配置，无需运行 `npm run serve`，也无需单独部署前端服务器或复制 lib 目录。修改端口可追加 `--server.port=8080`，三个入口一起切换。

页面深层链接可以直接访问和刷新，例如 `/submit`、`/admin/case/list`。两端同源，分别校验登录身份。管理端使用手机号和管理员密码，用户端测试模式使用本地配置的验证码。

## 开发与可选配置

- 后端业务代码在 `health-case-web`，用户端在 `health-case-front`，管理端在 `health-case-admin`。
- IDE 启动 `com.kaiyun.Application` 前先执行一次 `mvn package`，让页面进入后端 classpath。前端修改后重新构建页面才能在单端口模式看到变化。
- 分别在 `health-case-front` 和 `health-case-admin` 目录运行 `npm run serve`，可启动 8081 用户端和 8082 管理端。两个前端默认把 API 请求转发到 8099。
- 仅编译后端可执行 `mvn compile -DskipFrontend=true`；交付构建使用完整的 `mvn clean package`。
- 默认 `mvn clean package` 不包含 Nacos 客户端，也不会把 bootstrap.properties 复制到根 classpath；IDE 和 JAR 默认加载 standalone。
- 将来需要 Nacos 时，再使用 `mvn clean package -Pnacos` 构建并配置 Nacos 连接，读取线上 health-case-web-prod.yaml。

详细配置见 [health-case-web/CONFIGURATION.md](health-case-web/CONFIGURATION.md)，病例提交见 [health-case-web/CASE_SUBMISSION.md](health-case-web/CASE_SUBMISSION.md)。

管理端案件和保单列表按查询条件由数据库分页。保单管理提供查询、详情和 Excel 导入，保单操作记录可单独查看。

### 用户端页面管理

管理端打开「平台管理 → 用户端页面」，可编辑首页，或创建地址为 `/pages/页面标识` 的内容页。沿用原有的 `uiManagement` 权限，权限页中的开关名称更新为「用户端页面管理」，同时需要平台管理模块权限。

页面可组合标题、轮播、服务入口、文字、图片和按钮模块，支持模块上下移动、启停、主题颜色、底部导航名称，以及入口的宫格或列表布局和每行列数。新增页面后，可在首页的服务入口或按钮中选择该页面作为跳转地址。页面文字按普通文字显示，图片通过原有 OBS 上传功能管理。

服务入口、图片和按钮的跳转地址支持直接输入，输入框会提示已有页面路径。手动输入的地址保存为「直接链接」，点击后在当前窗口直接打开填写的地址。下拉选择已有页面时保存为「页面跳转」，通过前端路由进入所选页面，输入框下方会提示当前跳转方式。可填写 `/submit`、`/pages/service-guide?from=home#notice` 等站内地址，或完整的 `http://`、`https://` 地址。旧配置缺少跳转类型时按原有规则处理，再次编辑地址后记录明确的跳转类型。

页面编辑默认同时展示编辑区和实时预览。点击「编辑页面」后可修改文字、模块顺序、布局或显示状态，预览自动同步。桌面端采用左右分栏，窄屏采用上下布局，可通过「收起预览 / 显示预览」切换。点击「保存」后需再次确认，确认后保存为草稿并退出编辑状态。点击「取消编辑」可放弃未保存的修改。已保存的草稿可通过「发布」及二次确认更新到用户端。自定义页面支持下线和删除，首页保持发布状态。多个管理员同时编辑时，版本冲突会提示重新加载，避免覆盖其他人的修改。首页轮播是共用素材，在「首页轮播」页签保存并确认后直接更新轮播内容，页面素材保存和图片上传也需要确认。

页面配置保存到 `ui_page` 表，图片继续使用 `ui` 表和 OBS。默认启动时创建页面表并初始化现有首页布局，已有首页配置在重启时保留。关闭 `UI_INITIALIZE_SCHEMA` 的环境需先执行 `health-case-web/src/main/resources/db/ui/ui_page.sql`，脚本包含初始首页配置。首次部署还需执行原有的 `db/ui/ui.sql`。更新代码后重新打包并重启，之后修改页面内容可通过管理端发布。

实时预览由管理端自己的 `/admin/page-preview` 页面渲染。管理端从后端读取页面配置和素材，编辑内容在同源预览窗口中实时更新。

确认保存、发布、下线、删除页面，以及保存或删除轮播和图片素材时，后端会保存平台操作日志，控制台同时输出「平台操作成功 / 失败」。页面输入和实时预览在浏览器中同步，操作请求会在二次确认后提交。

## 平台操作日志

管理端「平台管理 → 操作日志」提供只读列表和详情，支持按模块、操作类型、操作人、目标、结果和日期筛选，以及服务端分页。查看列表、筛选选项和详情均要求平台管理模块权限；没有该权限的账号不显示菜单，直接调用接口也会被拒绝。日志不提供修改、删除或清空接口。

目前记录页面及素材管理、管理员登录与退出、密码修改、管理员账号与权限变更、保单导入导出、赔案推送与重试。推送日志表示管理员提交任务的结果，后台投递和消费状态仍在赔案推送页面查看。普通查询、导入预览、浏览器实时预览不生成操作记录。历史控制台输出不会自动补录。

记录保存到 `platform_operation_log`，包含脱敏账号、目标、页面版本、操作结果、HTTP 状态、耗时、连接地址及允许记录的摘要。权限变更记录各模块开关和是否设置密码，不记录密码内容；请求体、会话、验证码、文件内容及 OBS 签名地址不记录。未通过身份校验的请求，其操作人标为 `-`。

日志列表显示「网络 IP」，优先显示公网地址，未获取公网地址时显示原始连接 IP。详情保留网络 IP、公网 IP 来源及原始连接 IP。管理员登录不再请求设备定位授权，不采集经纬度，缺少定位权限或定位信息也能按正常账号密码流程登录。

优先采用服务端请求中识别出的公网地址；本机或局域网登录时，管理端提交登录前通过 [ipify](https://www.ipify.org/) 查询浏览器网络出口地址。查询请求不携带本系统账号、密码、Cookie 或页面来源；1.5 秒未完成或查询失败仍会继续登录，此时日志使用服务端识别的连接地址。可在构建管理端前通过 `VUE_APP_PUBLIC_IP_LOOKUP_URL` 指定支持跨域 JSON `{ "ip": "地址" }` 响应的 HTTPS 服务，或通过 `VUE_APP_PUBLIC_IP_LOOKUP_ENABLED=false` 关闭浏览器查询。

浏览器报告值仅作审计参考，不参与身份验证或权限控制。后续已认证操作可引用同一会话登录时查询的 IP，页面标注「登录时浏览器查询（参考）」；失败登录不会借用旧会话 IP，退出或修改密码会清除该会话信息。控制台输出 `publicIp`、`ipSource` 和 `clientIp`。

升级后默认自动为已有日志表补充公网 IP 字段并保留历史记录。关闭日志建表迁移的环境，确认缺少对应列后先手工执行；已存在的列不要重复添加：

```sql
ALTER TABLE platform_operation_log ADD COLUMN public_ip VARCHAR(64) NOT NULL DEFAULT '';
ALTER TABLE platform_operation_log ADD COLUMN ip_source VARCHAR(16) NOT NULL DEFAULT 'UNAVAILABLE';
```

重启后强制刷新管理端并重新登录，新登录记录才会包含本次查询结果。历史回环地址无法补查当时的公网 IP；公网查询失败时，localhost 登录会显示回环地址。已部署过设备定位版本的数据库可以保留旧位置列，本版本不再写入或读取这些字段，无需删表。

经 Nginx 等反向代理部署时，可在确认代理会覆盖客户端转发头后，使用 Tomcat 的可信代理处理，让请求中的真实公网地址优先于浏览器报告。下面示例仅信任本机代理；其他部署应替换为实际代理地址的精确正则：

```properties
server.forward-headers-strategy=NATIVE
server.tomcat.remoteip.remote-ip-header=X-Forwarded-For
server.tomcat.remoteip.internal-proxies=127\\.0\\.0\\.1|::1|0:0:0:0:0:0:0:1
```

应用默认不会自行信任任意客户端提供的 `X-Forwarded-For`。公网 IP 表示网络出口，不表示设备的精确地理位置。

启动时默认自动建表。无建表权限的环境先执行 `health-case-web/src/main/resources/db/platform/operation_log.sql`，再设置 `AUDIT_INITIALIZE_SCHEMA=false`。操作结束后通过独立事务写入，业务回滚或权限拒绝也会留下失败记录；数据库不可用等原因导致日志落库失败时，控制台输出错误，已完成的业务响应保持原结果。

控制台成功记录为 `INFO`，失败为 `WARN`，可通过 `PLATFORM_OPERATION_LOG_LEVEL` 设置级别，默认 `INFO`，兼容原有 `UI_OPERATION_LOG_LEVEL`。列表接口为 `GET /api/platform/operation-logs`，详情为 `GET /api/platform/operation-logs/{id}`。

## 用户列表与详情

「用户中心 → 用户列表」按手机号汇总用户登录档案、案件提交手机号、保单投保人及被保险人手机号。相同手机号合并为一位用户，同一份保单中的两个角色使用相同手机号时只统计一份保单。列表支持手机号或尾号、用户来源、最近登录日期筛选和服务端分页；打开详情再返回列表会保留筛选条件。

成功的用户端验证码登录会原子更新 `customer_user` 中的首次登录时间、最近登录时间和登录次数。发送验证码、登录失败及管理端账号登录不会创建用户档案。历史保单和案件中的联系人可直接查询，尚未登录时标记为「业务关联用户」，不补造历史登录时间。一个账号可能代不同患者提交案件，因此账号资料不自动套用患者或被保险人的姓名、证件资料。

用户详情提供账号资料、关联保单与案件数量、待完成和已完成案件数量，并支持对关联记录独立分页及进入业务详情。用户列表和账号概览要求用户中心模块权限；关联保单还需保单管理权限，关联案件还需案件管理权限，后端接口同样校验。手机号默认脱敏显示，详情页可按需展开完整号码。当前提供只读查询，不删除用户或关联业务资料。

首次启动默认创建 `customer_user` 表。无建表权限时先执行 `health-case-web/src/main/resources/db/user/customer_user.sql`，再设置 `USER_INITIALIZE_SCHEMA=false`。首次、最近登录时间及次数从更新后的首次成功登录开始记录。查询接口为 `GET /api/users`、`GET /api/users/{phone}`，关联记录为 `GET /api/users/{phone}/policies` 和 `GET /api/users/{phone}/cases`。

## 项目与保险公司

「项目管理」包含「项目列表」和「保险公司管理」。公司资料支持名称、唯一编码、联系人、电话、地址、备注、启用状态和当前生效环境；测试与生产环境分别维护对接状态、HTTPS 接收地址及鉴权方式。已有公司和项目编码固定，修改资料及对接配置需要显式保存、二次确认，并使用版本号防止覆盖其他管理员的修改。公司与项目通过停用、暂停或结束保留历史资料，不提供删除接口。

一个保险公司可以有多个项目，一个项目可以有多份保单，案件从所选保单确定所属项目和保险公司。项目有草稿、运行中、暂停、结束状态，支持有效期、负责人、联系方式、说明及资料清单。Excel 保单导入时可选择有效项目；也可在项目详情关联未分配项目且没有案件的已有保单。已有案件的保单不允许重新分配项目，已有保单的项目不允许换保险公司，以保持历史归属稳定。未分配项目的历史保单继续使用原接收方配置。

公司及项目管理要求项目管理模块权限；关联保单还要求保单权限，关联案件及公司推送记录还要求案件权限。公司详情可查看关联项目和推送记录，平台操作日志记录公司、对接配置、项目及保单关联的修改。保单详情、案件详情显示所属项目及保险公司。

当前保险公司通知协议沿用现有接口：HTTPS POST，JSON 中包含 `taskId` 和字符串 `caseId`，使用 `Idempotency-Key` 请求头，可选择无鉴权或 Bearer 令牌。此接口是赔案任务通知，不自动转换保险公司专有的完整理赔报文、签名或回调协议。标记「已对接」表示管理员允许按该配置发送通知，不代表系统已经验证保险公司的接口可用；保存配置不主动发送测试请求。正式联调需按保险公司接口文档确认协议。RabbitMQ 继续负责内部任务发布与消费，消费工作器按案件所属公司及生效环境选择接收方。

推送任务保存接收方指纹。自动重试时如果公司环境或接收地址变化，会停止任务并提示核对后手动重试，避免旧任务被自动改投。显式重试后才使用当前接收方；暂停的项目、停用的公司和未完成或暂停的对接配置不会发出通知。

Bearer 令牌经 AES-GCM 加密后存入数据库，管理接口仅返回是否已配置，留空保存沿用原令牌。选择无鉴权并保存会清除该环境的令牌。部署时必须设置稳定的 `INTEGRATION_ENCRYPTION_KEY`，内容为 Base64 编码的16或32字节随机密钥。密钥应单独备份并在重启后保持不变；变更密钥后需要重新设置已有令牌。不要把实际密钥放入对接地址、操作日志或聊天。

默认启动创建 `insurance_company`、`insurer_connection`、`service_project`，为已有 `service_policy` 补充可空的 `project_id` 及索引，并为 `case_push_task` 补充 `receiver_fingerprint`。无建表/改表权限时，执行 `health-case-web/src/main/resources/db/project/project.sql`；确认缺少对应列后分别执行：

```sql
ALTER TABLE service_policy ADD COLUMN project_id BIGINT NULL;
CREATE INDEX idx_policy_project ON service_policy(project_id);
ALTER TABLE case_push_task ADD COLUMN receiver_fingerprint VARCHAR(64) NULL;
```

然后设置 `PROJECT_INITIALIZE_SCHEMA=false`；关闭任务表初始化时也需按已有消息队列说明维护任务表。新增任务表的完整脚本为 `db/messaging/case_push_queue.sql`。运行配置中的 `app.integration.encryption-key` 默认读取 `INTEGRATION_ENCRYPTION_KEY`，也可以在本地配置或 Nacos 中设置该属性。

### 报案审核与赔付确认

用户登录后提交患者信息、报案人信息、关联保单和病例资料，报案联系电话采用登录手机号。保存后，案件进入管理端「案件管理 → 领款人审核」。审核通过时必须登记大于零且最多两位小数的人民币赔付金额，案件进入待线下赔付；审核驳回必须填写用户可见的原因。

经办人员在系统外完成实际赔付后，在「案件管理 → 案件维护」选择「确认已赔付」，核对金额并确认，案件才变为已完成。系统保存审核人、审核时间、审核金额以及赔付确认人和确认时间。历史处理中案件如果缺少审核记录，可先在领款人审核页补审。已有已完成案件保留原状态。

「案件管理 → 理赔」为已赔付报案台账，从现有案件和赔付确认记录读取，按确认时间倒序展示。支持按报案人、保单号、赔付确认日期查询及分页。缺少赔付确认记录的历史已完成案件不计入台账。

审核和线下赔付确认产生的案件状态变更继续自动创建赔案推送任务。应用默认创建 `case_payment_review` 表。关闭 `CASE_TASKS_INITIALIZE_SCHEMA` 的环境，部署前需手动执行 `health-case-web/src/main/resources/db/cases/case_payment_review.sql`。

### 赔案推送

管理端在「案件维护」保存新的案件状态时，会在同一数据库事务中创建推送任务。仅编辑处理结果不会创建任务。每次实际状态变更都有独立任务和任务 ID，历史保留。对接配置有问题时，任务记录失败原因，案件状态仍可保存。

「案件管理 → 赔案推送」支持单条或批量手动推送。存在待发送、发送中或等待重试的任务时，不允许再手动创建任务。已受理后可再次手动推送，失败后可在列表或「推送记录」中重试指定任务。列表显示最近创建的任务，记录窗口按创建时间顺序倒序显示最近 100 条。两种触发方式使用相同的通知格式和重试机制，推送结果不改变案件审核状态。

默认使用本项目的 `POST /api/messaging/case-push/receive` 接收赔案通知。RabbitMQ 消费者通过本机回调访问实际应用端口，校验任务与案件对应关系后，将接收结果写入 `case_push_receipt`。同一个 `taskId` 只保留一条接收记录，重复请求返回成功。该接口使用独立 Bearer 令牌校验，令牌为空时由应用在每次启动时随机生成并在进程内共享。本地模式下可保持 `CASE_PUSH_RECEIVER_URL` 和 `CASE_PUSH_RECEIVER_TOKEN` 为空。

接收接口要求 JSON 字段 `taskId`、`caseId`（字符串），`Idempotency-Key` 请求头须与 `taskId` 相同。接收成功表示通知已持久化，案件审核继续由管理端操作。网络错误、408、429 和 5xx 最多尝试五次，其他响应记录为失败。超时或多次失败后重试仍沿用原任务 ID。连接外部接收方时，配置 `CASE_PUSH_RECEIVER_URL`（HTTPS）和 `CASE_PUSH_RECEIVER_TOKEN`（Bearer 令牌）并重启。外部接收方需支持相同的请求格式和幂等规则，确认受理后返回 2xx。若切换接收地址，先核对待发送及待重试任务。

RabbitMQ 发布、后台消费、接收接口及 HTTP 投递集中在 Java 包 `com.kaiyun.mq`，随单体应用一同打包。案件接口先把任务写入 MySQL，再由后台发布至 RabbitMQ。消息只包含 `taskId` 和 `caseId`。发布需要 Broker 确认并成功路由，消费在处理状态写入 MySQL 后发送 ACK。发布中断、消息未被消费或执行超时后，应用按任务状态重新调度，沿用原任务 ID。

RabbitMQ 的地址、端口、账号、密码和 vhost 可写在 `config/application.properties`，也可通过 `RABBITMQ_*` 环境变量设置。按服务端要求开启 TLS。使用外置 `application-standalone.properties` 时，也可以用以下属性覆盖。

```properties
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=
spring.rabbitmq.password=
spring.rabbitmq.virtual-host=/
spring.rabbitmq.ssl.enabled=false
```

启动消费时，应用会声明持久队列 `custpost.case-push`，可通过 `CASE_PUSH_RABBIT_QUEUE` 修改队列名。RabbitMQ 账号需要在对应 vhost 下具备声明队列、发布消息和消费消息的权限。测试和生产应分别使用各自的 vhost 或队列。连接参数填好后重启应用，即可由本机接口接收推送。RabbitMQ 账号或密码留空时，仍可查看案件和既有任务，新推送暂不可提交。设置 `CASE_PUSH_TRANSPORT=mysql` 可切回 MySQL 后台消费模式。

后台任务和接收回执保存到 MySQL。应用默认创建或升级任务表，已有任务 ID 和回执都会保留。升级前先停止旧版本实例。如果关闭了 `CASE_TASKS_INITIALIZE_SCHEMA`，新库需先执行 `health-case-web/src/main/resources/db/messaging/case_push_queue.sql` 和 `health-case-web/src/main/resources/db/cases/case_submission.sql`。旧表需确认已有 `dispatch_after`、`receiver_fingerprint`，并按实际缺失情况迁移：

```sql
ALTER TABLE case_push_task ADD COLUMN task_order BIGINT NOT NULL AUTO_INCREMENT UNIQUE;
ALTER TABLE case_push_task ADD COLUMN trigger_type VARCHAR(20) NOT NULL DEFAULT 'MANUAL';
ALTER TABLE case_push_task ADD COLUMN case_status VARCHAR(20) NULL;
CREATE INDEX idx_case_push_case_order ON case_push_task(case_id, task_order);
ALTER TABLE case_push_task DROP PRIMARY KEY, ADD PRIMARY KEY (task_id);
```

### 保单导入与导出

保单导入与导出需要管理员权限，由超级管理员在「平台管理 → 权限管理」分配。

管理端打开「保单管理 → 保单导入与导出」，点击「导入 Excel」。上传 `.xls` 或 `.xlsx` 后，选择工作表与表头行，再将 Excel 列映射到保单字段。保单号、保险产品和保单状态必填，投保人、被保人、证件信息、保费和保障日期按需映射。保单号和证件号须为文本单元格。文件上限为 5 MB，每次最多 1000 行。

预览会提示重复保单号和无效字段所在的行。通过校验并确认后，整批保单和创建记录一同写入数据库。已存在的保单号会阻止整批导入。

导出支持按保单号、被保人、保单状态和创建日期筛选，留空时导出全部保单。单次最多导出 10,000 条到 `.xlsx`。投保人和被保人的手机号、证件号均在服务端脱敏，保留前 3 位和后 4 位，中间以星号替代，7 位及以下全部遮盖。

### 权限管理

所有超级管理员均可在「平台管理 → 权限管理」新增超级管理员。主超级管理员由 `ADMIN_ROOT_PHONE` 指定，可调整现有账号的超级管理员角色及撤销超级管理员。超级管理员拥有全部业务权限，可管理普通管理员，也可修改自己的密码。管理员密码哈希保存在数据库 `admin_permission.password_hash`。新增账号须设置至少 12 个字符的密码，编辑权限时留空可保留原密码。

新数据库首次部署时，可临时设置 `ADMIN_INITIAL_PASSWORD`（8 至 128 个字符）并启动一次。服务会为尚未存在的超级管理员创建记录，已有密码继续以数据库为准。初始化成功后移除该环境变量。

在「平台管理 → 权限管理」编辑账号，可分别开启数据统计、案件管理、保单管理、项目管理、平台管理和用户中心。关闭的模块会从菜单隐藏，直接访问也会被拦截。保单导入导出和界面图片管理仍需对应的专项开关。超级管理员拥有全部模块权限。升级时自动为权限表补列，已有专项授权继续有效，其余模块等待超级管理员分配。

用户端服务记录按登录手机号查询，匹配病例提交时填写的联系电话。管理端的全量案件及详情查询需要案件管理权限。

超级管理员可在「平台管理 → 权限管理」按手机号分配、修改和撤销保单导入与导出、界面图片管理权限，并为受权账号设置专用密码。受权账号以手机号和该密码登录管理端。撤销账号或重设密码后，原管理端登录失效。权限保存到 `admin_permission`。启动时会按需建表。无建表权限时，可先执行 `health-case-web/src/main/resources/db/auth/admin_permissions.sql`，再设置 `ADMIN_INITIALIZE_SCHEMA=false`。

旧的保单和界面图片手机号白名单已由权限页取代。如有需要，请在权限页为原有人员重新授权。固定验证码登录适用于本地测试环境。

自动化测试源码、测试夹具和测试专用依赖已按要求移除。领款人审核、赔案推送和保单操作记录汇总页面已清除演示数据；这些页面尚无对应业务接口，暂时显示空列表，操作不会伪造成功结果。固定验证码登录继续按既定要求保留。
