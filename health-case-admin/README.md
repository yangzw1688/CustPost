# 健康服务管理端

管理端可独立安装和构建。开发时访问 `http://localhost:8082/admin/`，默认将 `/api` 和 `/upload` 请求转发到 `http://localhost:8099`。通过环境变量 `VUE_APP_API_TARGET` 可以指定 Java 后端地址。

```sh
yarn install --frozen-lockfile
npm run serve
```

用户端页面编辑器通过本项目的 `/admin/page-preview` 显示手机预览。运行 `npm test` 检查登录会话和病例附件规则。运行 `npm run build` 生成 `dist`，静态部署路径为 `/admin/`。
