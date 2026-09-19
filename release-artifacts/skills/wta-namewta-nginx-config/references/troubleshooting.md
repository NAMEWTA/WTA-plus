# Nginx 排障

| 症状 | 常见原因 | 检查与处理 |
|---|---|---|
| 页面白屏或 assets 404 | 构建 base 与入口前缀不同 | 检查manifest 的 apps/applicationMatrix 和 `index.html`；用同一 env 文件重新构建、stage |
| 接口全部 404 | API base 未包含 App 前缀，或 App Nginx 未剥离 env-api | 检查 `/<prefix>/<env>-api`、LB 尾斜杠和 App template 两组 API location |
| LB 返回 502 | App/后端不在共享 network、服务未启动或名字不一致 | `docker network inspect`、`docker compose ps`、容器内解析服务名 |
| 独立端口正常，LB 404 | LB template 缺 upstream/location，或容器未重新创建 | 运行 `add_app.py --list`、Compose config、重建 LB |
| LB 正常，独立端口 404 | App template 缺带前缀 rewrite | 检查 `location /${APP_PREFIX}/` 位于通用 `location /` 前 |
| HTTP 请求打到 TLS 端口出现 400 | 协议/端口映射错误 | 使用 HTTPS，或通过 HTTP LB 端口访问；不要把公网 HTTP 转到容器 443 |
| 跳转包含内部端口 | Nginx 生成绝对重定向 | 保留 redirect 三件套，跳转使用显式路径，不使用 `$server_port` |
| TLS 容器反复重启 | 缺 `fullchain.pem`/`privkey.pem` 或证书不可读 | 检查 NAMEWTA_CERT_ROOT 下 `lb/` 或 `sso-web/`，先用非 TLS LB 验证其他链路 |
| `/admin/`、`/snail-job/`、`/snail-ai/` 不通 | 对应后端服务未启动或路径被 App 前缀覆盖 | 检查保留路由顺序和 observability/backend Compose |

## 检查顺序

```bash
bash release-artifacts/scripts/verify-release.sh
bash release-artifacts/scripts/docker-manage.sh config frontend
bash release-artifacts/scripts/docker-manage.sh ps all
docker exec namewta-nginx-admin-web nginx -t
docker exec namewta-nginx-lb nginx -t
curl -I http://127.0.0.1:41080/<prefix>/
curl -I http://127.0.0.1:40080/<prefix>/
```

资源请求若返回 `text/html`，通常是静态文件未 stage 或前缀剥离错误。修改 301 后应使用无痕窗口复验浏览器缓存。

SSO 页面或刷新404：检查 router 是否使用 Vite BASE_URL、SSO模板是否使用 @sso_spa 内部回退，根 location 的404应保留。SSO登录循环：检查 VITE_SSO_API 为空、/sso 原路径反代、APP_ORIGIN 与实际入口严格一致、Cookie为Secure/HttpOnly及独立hostname。跨Origin403不能靠放开通配CORS修复。
