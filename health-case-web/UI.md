# 数据库管理界面图片

首页轮播现在由 MySQL 的 `ui` 表提供，图片文件复用现有华为云 OBS 私有桶存储。默认轮播尚未导入 OBS 时，首页使用随应用发布的 `/images/caringhealth.png`。管理员导入素材后，首页使用 OBS 签名链接。

## 首次启用

1. 在启动后端的环境中配置 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`，以及现有的 `OBS_ENDPOINT`、`OBS_BUCKET`、`OBS_ACCESS_KEY`、`OBS_SECRET_KEY`。
2. 配置 `UI_ADMIN_PHONES` 为允许维护界面内容的登录手机号，多人用英文逗号分隔。默认空列表，普通用户不能修改首页内容。本地调试可使用已配置的测试验证码。
3. 启动后端。默认 `UI_INITIALIZE_SCHEMA=true`，若当前数据库还没有 `ui` 表，会自动执行 `src/main/resources/db/ui/ui.sql`，创建表并初始化三条首页轮播配置。重启不会覆盖文案、排序和启停状态，也不会恢复已删除的轮播。如果数据库账号没有建表权限，请用数据库管理工具手动执行该 SQL，再设 `UI_INITIALIZE_SCHEMA=false`。
4. 登录管理端，进入“平台管理 → 界面图片”，点击“导入原有首页图片”。后端把原图上传 OBS 并将对象键写入 `ui` 表。此操作可重复点击；已有图片时不会覆盖。
5. 打开或刷新用户端首页，图片、文案和顺序均从接口读取。未导入图片时，默认轮播仍有渐变背景和文案。

当前开发终端未提供数据库及 OBS 环境变量，因此代码验证没有连接实际数据库、执行远端建表或真实上传；以上步骤需要在配置相应环境后执行。

## 表结构与访问方式

- `id`：主键。
- `asset_key`：唯一配置标识，创建后不可更改。
- `slot`：显示位置，当前首页为 `home_banner`。
- `title`、`description`、`link_url`：标题、说明、点击跳转地址。
- `tone`：默认、蓝色或绿色背景，用于没有图片的轮播。
- `object_key`：OBS 对象键，不保存会过期的签名 URL，也不返回给用户端。
- `sort_order`、`enabled`：排序与启用状态。
- `create_time`、`update_time`：创建及更新时间。

当前每个位置最多返回100条配置，按排序、主键升序显示。列表读取临时生成 OBS 签名地址，默认有效期1小时。图片过期或加载失败时，用户端提供重新加载按钮。图标仍使用现有代码组件；浏览器 favicon 保留静态入口。

| 接口 | 用途 | 权限 |
| --- | --- | --- |
| GET /api/ui?slot=home_banner | 启用的前端展示数据 | 已登录 |
| GET /api/ui/manage?slot=home_banner | 管理列表，含停用项 | UI_ADMIN_PHONES |
| POST /api/ui/save | multipart 配置及可选 file，id为空时新增 | UI_ADMIN_PHONES |
| DELETE /api/ui/{id} | 删除配置 | UI_ADMIN_PHONES |
| POST /api/ui/import-defaults | 导入原有首页图片 | UI_ADMIN_PHONES |

图片上传验证大小、MIME 和文件头；替换时数据库提交成功后再删除旧图。数据库回滚则清理新图，提交结果未知或 OBS 清理失败时保留对象并记录日志供核对。新图对象键使用 `ui/` 前缀，病例图片仍使用 `case/`，不会混用。

部署用户端时代理 `/api` 到后端即可；图片由浏览器访问返回的 HTTPS OBS 签名地址。OBS 所需权限沿用 [OBS.md](OBS.md)。
