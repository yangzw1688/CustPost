# 手机号验证码登录（测试模式）

用户端为 `health-case-front`，接口由 `health-case-web` 提供。无需新建用户表或修改现有数据库结构。

开启测试登录并配置 `AUTH_TEST_CODE` 后，输入合法的中国大陆手机号和配置的验证码即可登录，不要求先点击获取验证码；获取验证码接口只返回测试提示，不发送短信。

| 方法 | 路径 | 行为 |
| --- | --- | --- |
| POST | /api/auth/send-code | JSON `{ "phone": "示例手机号" }`，返回测试提示 |
| POST | /api/auth/login | JSON `{ "phone": "示例手机号", "code": "配置的验证码" }`，创建 Session |
| GET | /api/auth/me | 返回当前登录手机号；未登录时 HTTP 401 |
| POST | /api/auth/logout | 注销 Session，可重复调用 |

登录 Cookie 名称 `HEALTH_SESSION`，为 HttpOnly，空闲超时为2小时。刷新页面通过 `/me` 恢复身份，不信任前端本地存储的手机号。成功登录轮换 Session ID，退出后旧 Session 立即失效。登录状态存储在单个服务进程内，重启需要重新登录；多实例部署需另行配置共享 Session。

用户端未登录会跳转 `/login` 并保留目标路径，登录后回到原页面；API 返回401时也会重新要求登录。个人信息页显示脱敏手机号，提供退出按钮。沿用原有登录页和业务页结构。

所有 `/api/**` 业务接口及历史本地图片 `/upload/case/**` 要求已登录，仅登录、获取测试验证码、退出和 OPTIONS 预检公开。后台 `health-case-admin` 的登录页未改造；它共用这些业务接口，也需要有效 Session。测试时可在同一浏览器、相同主机名（例如均为 localhost）通过用户端登录后访问后台；独立管理员身份与权限不在此次范围内。页面经同源代理访问后端即可携带 Cookie，生产部署需代理 `/api` 和 `/upload` 到后端。可通过 `VUE_APP_API_TARGET` 调整开发代理，默认仍为 `http://localhost:8099`。

此次增加身份登录校验，未改变现有业务数据查询范围，也未实现按手机号划分数据所有权。

`AUTH_TEST_CODE_ENABLED` 默认 `true`；设为 `false` 会关闭固定验证码入口并返回503。当前没有真实短信供应商实现。HTTPS 部署时可配置 `SESSION_COOKIE_SECURE=true`。

业务运行需按 [CONFIGURATION.md](CONFIGURATION.md) 配置数据库和 OBS。自动化测试源码已按要求移除。
