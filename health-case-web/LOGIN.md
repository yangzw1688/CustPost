# 用户端手机号短信登录

用户端选择「获取验证码」后，后端通过阿里云短信服务发送六位验证码。发送请求被接受后，验证码摘要写入 Redis，TTL 为 5 分钟。有效期内最多校验 5 次，校验成功即删除。每个手机号间隔至少 60 秒可重发，24 小时最多请求 5 次。后端也按连接地址和全站限额控制发送频率。

## 本地配置

在项目根目录运行 Java 时，将真实值写入 Git 忽略的 `config/application-standalone.properties`，或在 Java 进程环境中设置对应变量。

| 本地配置项 | 环境变量 | 用途 |
| --- | --- | --- |
| `app.auth.sms.access-key-id` | `ALIBABA_CLOUD_ACCESS_KEY_ID` | 有短信发送权限的 RAM 用户 AccessKey ID |
| `app.auth.sms.access-key-secret` | `ALIBABA_CLOUD_ACCESS_KEY_SECRET` | 同一 RAM 用户的 AccessKey Secret |
| `app.auth.sms.sign-name` | `ALIYUN_SMS_SIGN_NAME` | 审核通过的短信签名名称，填写名称本身 |
| `app.auth.sms.template-code` | `ALIYUN_SMS_TEMPLATE_CODE` | 审核通过的验证码模板编号，形如 `SMS_...` |
| `app.auth.sms.code-parameter` | `ALIYUN_SMS_CODE_PARAMETER` | 模板内的验证码变量名，默认为 `code` |
| `app.auth.sms.otp-secret` | `SMS_OTP_SECRET` | 验证码摘要密钥，使用独立生成的随机值，至少 32 字节 |
| `app.auth.sms.daily-limit` | `SMS_DAILY_LIMIT` | 全站 24 小时发送上限，默认 1000 |

使用只包含一个验证码变量的模板，例如 `您的登录验证码为${code}，5分钟内有效。`。保存配置后重启后端 `Application`，从用户端请求一次短信并使用手机收到的验证码登录。

Redis 默认连接 `127.0.0.1:6379` 的数据库 0。可在本地配置文件中设置 `spring.redis.host`、`spring.redis.port`、`spring.redis.password` 和 `spring.redis.database`，或使用对应的 `REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD` 和 `REDIS_DATABASE` 环境变量。也可用 `REDIS_URL` 指定连接地址，按部署要求设置 `REDIS_SSL`。

验证码、重发等待和发送限额通过 Redis 自动过期。多实例使用同一 Redis 数据库和同一验证码摘要密钥。部署在反向代理后时，应由服务端配置可信代理的客户端地址处理，避免所有访客共用连接来源的短信限额。

## 接口

| 方法 | 路径 | 行为 |
| --- | --- | --- |
| POST | `/api/auth/send-code` | 提交 `phone`，返回发送提示和重发等待时间 |
| POST | `/api/auth/login` | 提交 `phone` 和 `code`，校验成功后创建 Session |
| GET | `/api/auth/me` | 获取当前会话的手机号，未登录返回 HTTP 401 |
| POST | `/api/auth/logout` | 退出登录，可重复调用 |

真实短信模式下，验证码只通过短信发送。前端刷新页面后通过 `/api/auth/me` 恢复会话。管理端继续使用管理员密码登录。

独立测试环境可显式设置 `AUTH_TEST_CODE_ENABLED=true` 和 `AUTH_TEST_CODE` 使用固定测试码。真实短信环境使用 `AUTH_TEST_CODE_ENABLED=false`。

参考 [阿里云 SendSms 接口说明](https://help.aliyun.com/zh/sms/developer-reference/api-dysmsapi-2017-05-25-sendsms)。
