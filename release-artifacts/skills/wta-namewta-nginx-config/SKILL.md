---
name: wta-namewta-nginx-config
description: 维护 wta-vue-plus-docs 的 release-artifacts 多 App Nginx 部署体系，包括统一 nginx-lb、每 App 独立 HTTP Nginx、路径前缀、独立端口、可选 TLS、docker-compose-frontend.yml、发布构建前缀与新增 App 自动化。处理新增或删除 plus-ui-namewta/apps 前端 App、Nginx 404/400/502、静态资源或 API 前缀错误、LB 重定向、证书、端口台账、add_app.py 或 release-artifacts 前端容器时使用。
---

# NAMEWTA 多 App Nginx 配置

统一入口 `namewta-nginx-lb` 按 App 路径前缀剥离后转发到每个 App 的独立 HTTP Nginx。每个 App 容器同时支持 LB 根路径请求和独立宿主机端口的带前缀访问。

修改前读取：

- 拓扑、请求链路和配置不变量：[references/architecture.md](references/architecture.md)
- 当前端口、前缀变量和保留路由：[references/port-registry.md](references/port-registry.md)
- 400/404/502、资源、接口和证书排障：[references/troubleshooting.md](references/troubleshooting.md)

## 新增 App

前置条件：`plus-ui-namewta/apps/<app>/package.json` 存在并提供 `build:dev`、`build:prod`。

```bash
# 公开 App
python release-artifacts/skills/wta-namewta-nginx-config/scripts/add_app.py \
  --app admin-web --prefix admin

# 敏感 App：生成 10 位私有前缀；提交配置只保存占位值，真实值写本地 .env
python release-artifacts/skills/wta-namewta-nginx-config/scripts/add_app.py \
  --app secret-web --sensitive

# 预览和台账
python release-artifacts/skills/wta-namewta-nginx-config/scripts/add_app.py --list
python release-artifacts/skills/wta-namewta-nginx-config/scripts/add_app.py \
  --app admin-web --prefix admin --dry-run
```

脚本只修改 `release-artifacts/`，不改 App 源码 `.env.*`。它会幂等维护：

1. App Nginx template 与 html/cert 目录。
2. `docker-compose-frontend.yml` 的 LB env 和 App 服务。
3. HTTP/TLS LB template 的 upstream 与路径路由。
4. `.env.example`；本地 `.env` 存在时同步真实前缀和端口。

## 构建与部署

```bash
bash release-artifacts/scripts/release-manage.sh build \
  --target frontend --env prod --env-file release-artifacts/.env
bash release-artifacts/scripts/release-manage.sh stage --env prod
bash release-artifacts/scripts/docker-manage.sh config frontend
bash release-artifacts/scripts/docker-manage.sh up frontend
```

发布脚本通过进程环境覆盖 Vite 的 `VITE_APP_CONTEXT_PATH` 和 `VITE_APP_BASE_API`。不要为部署前缀修改 `plus-ui-namewta/apps/*/.env.*`。

## 不变量

1. 构建 base 为 `/<prefix>/`，API base 为 `/<prefix>/<dev|prod>-api`。
2. LB `proxy_pass http://app_xxx/;` 保留尾斜杠，负责剥离 App 前缀。
3. App Nginx 根路径服务 LB 请求，带前缀路径服务独立端口请求。
4. 默认只在 LB 终止 TLS；证书和私钥不入库。
5. `/admin/`、`/snail-job/`、`/snail-ai/` 和 actuator 规则是保留路由。
6. 管理端私有前缀不得由根路径跳转公开。

## 验证

```bash
bash release-artifacts/scripts/verify-release.sh
docker compose --env-file release-artifacts/.env \
  -f release-artifacts/docker/docker-compose-frontend.yml config --quiet
docker exec namewta-nginx-admin-web nginx -t
docker exec namewta-nginx-lb nginx -t
```
