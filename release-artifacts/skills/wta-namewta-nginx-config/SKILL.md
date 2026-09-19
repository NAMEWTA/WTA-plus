---
name: wta-namewta-nginx-config
description: 维护 wta-vue-plus-docs 的 release-artifacts 多 App Nginx 部署体系，包括统一 nginx-lb、每 App 独立 HTTP Nginx、路径前缀、独立端口、可选 TLS、docker-compose-frontend.yml、发布构建前缀与新增 App 自动化。处理新增或删除 frontend/apps 前端 App、Nginx 404/400/502、静态资源或 API 前缀错误、LB 重定向、证书、端口台账、add_app.py 或 release-artifacts 前端容器时使用。
---

# NAMEWTA 多 App Nginx 配置

`apps.json` 是显式发布清单。业务 App 由 `namewta-nginx-lb` 按前缀剥离后转发，也支持独立端口带前缀访问；sso-web 使用专用 HTTP/TLS 服务，不加入共享 LB，生产使用不同 hostname 的 HTTPS Origin。

修改前读取：

- 拓扑、请求链路和配置不变量：[references/architecture.md](references/architecture.md)
- 当前端口、前缀变量和保留路由：[references/port-registry.md](references/port-registry.md)
- 400/404/502、资源、接口和证书排障：[references/troubleshooting.md](references/troubleshooting.md)

## 新增 App

前置条件：`frontend/apps/<app>/package.json` 存在并提供 `build:dev`、`build:prod`。

```bash
# 公开 App
python release-artifacts/skills/wta-namewta-nginx-config/scripts/add_app.py \
  --app home-web --prefix portal

# 敏感 App：生成 10 位私有前缀；提交配置只保存占位值，真实值由操作者写本地 .env
python release-artifacts/skills/wta-namewta-nginx-config/scripts/add_app.py \
  --app secret-web --sensitive

# 预览和台账
python release-artifacts/skills/wta-namewta-nginx-config/scripts/add_app.py --list
python release-artifacts/skills/wta-namewta-nginx-config/scripts/add_app.py \
  --app home-web --prefix portal --dry-run
```

脚本只修改 `release-artifacts/`，不改 App 源码 `.env.*`。它会幂等维护：

1. App Nginx template 与 html/cert 目录。
2. `docker-compose-frontend.yml` 的 LB env 和 App 服务。
3. HTTP/TLS LB template 的 upstream 与路径路由。
4. `apps.json`、`.env.example` 中的配套与占位配置。运行 `.env` 从不自动修改。

新增项默认注册为 business App；已有 sso-web 维护专用服务及 Origin 绑定，缺专用模板时拒绝，不能复制业务模板代替。再次执行应不产生变化；端口调整同步清单与 Compose。

## 构建与部署

```bash
bash release-artifacts/scripts/release-manage.sh build \
  --target all --env prod --env-file release-artifacts/.env
bash release-artifacts/scripts/release-manage.sh stage --env prod --release <完整build输出的ID>
bash release-artifacts/scripts/docker-manage.sh config frontend
bash release-artifacts/scripts/docker-manage.sh up frontend
```

局部 frontend 构建只供开发，不能晋升。完整发布来自干净 Git 快照，stage 仅切换不可变版本指针，docker-manage 固定该版本并显式重建容器。Nginx 日志使用 NAMEWTA_DATA_ROOT，证书使用 NAMEWTA_CERT_ROOT，不写入版本目录。

发布脚本通过进程环境覆盖 Vite 的 `VITE_APP_CONTEXT_PATH` 和业务 `VITE_APP_BASE_API`，SSO 的 `VITE_SSO_API` 显式为空、API 保持 `/sso`。不要为部署前缀修改 `frontend/apps/*/.env.*`。

## 不变量

1. 三 App 构建资源和 router base 均为 `/<prefix>/`；业务 API 为 `/<prefix>/<dev|prod>-api`，SSO API 为 `/sso`。SSO 静态 prefix 不能是 `sso`。
2. LB `proxy_pass http://app_xxx/;` 保留尾斜杠，负责剥离 App 前缀。
3. 业务 App Nginx 根路径服务 LB 请求，带前缀路径服务独立端口请求。SSO 根路径返回404，子路径经内部 SPA 回退，不能跳到根 location。
4. 业务 TLS 在 LB 终止；SSO 有独立 TLS 服务，使用 NAMEWTA_CERT_ROOT/sso-web。证书和私钥不入库，生产浏览器 Origin 必须 HTTPS。
5. `/admin/`、`/snail-job/` 和 actuator 规则是保留路由；退役的 `snail-ai` 仍禁止作为 App 前缀，以免旧链接指向新业务，不再注册 upstream 或服务路由。
6. 管理端私有前缀不得由根路径跳转公开。
7. manifest 保存精确 Origin/入口矩阵，运行参数必须匹配；SSO 与业务 hostname 必须不同（端口不能隔离 Cookie）。SSO 入口仅接受自身 Origin 或无 Origin 的 API 请求。
8. access log 省略查询参数；不把这项规则外推为所有 error log 已脱敏。health 不写 access log。

## 验证

```bash
bash release-artifacts/scripts/verify-release.sh
docker compose --env-file release-artifacts/.env \
  -f release-artifacts/docker/docker-compose-frontend.yml config --quiet
docker exec namewta-nginx-admin-web nginx -t
docker exec namewta-nginx-lb nginx -t
```
