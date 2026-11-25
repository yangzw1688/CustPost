# OBS 图片存储

后端启动前设置环境变量：

- OBS_ENDPOINT：桶所在区域的 HTTPS Endpoint，例如 https://obs.cn-north-4.myhuaweicloud.com
- OBS_BUCKET：桶名称
- OBS_ACCESS_KEY：访问密钥 AK
- OBS_SECRET_KEY：访问密钥 SK
- OBS_URL_EXPIRY_SECONDS：图片链接有效秒数，默认 3600

使用私有桶，服务账号需要对象上传、读取和删除权限。无需开放公共读权限。
新图片保存为 case/{服务记录ID}/{随机文件名}，file_path 存储对象键，file_url 存储 obs:// 对象标识；详情查询时生成签名链接。页面停留超过有效期需重新获取详情。
历史本地图片保持原路径，不自动迁移。保留本地资源映射用于读取旧图片。
数据库事务回滚时尝试删除已上传对象；若删除失败，日志记录对象键供后续清理。进程异常退出等情况可能留下孤立对象。
