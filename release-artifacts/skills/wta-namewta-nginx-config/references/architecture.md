# 架构与请求链路

## 拓扑

```text
HTTP/TLS
  -> namewta-nginx-lb
       /<prefix>/ -> nginx-<app>:80（剥离 prefix）
       /admin/     -> namewta-monitor-admin:9090
       /snail-job/ -> namewta-snailjob-server:8800
       /snail-ai/  -> namewta-snailai-server:8900

nginx-<app>:80
  /                     -> SPA 静态文件
  /<env>-api/*          -> namewta-server1/2
  /<prefix>/*           -> 独立端口访问时剥离 prefix
  /<prefix>/<env>-api/* -> 独立端口访问时剥离 prefix 与 env-api
```

四类 Compose 使用同一个 external bridge network，通过服务名寻址。不得把参考 CDE 配置中的固定服务器 IP、host network 或 `privileged` 复制进来。

## 两种 App 访问方式

统一入口：

```text
GET /private-admin/assets/app.js
  -> LB proxy_pass http://app_admin_web/
  -> App Nginx GET /assets/app.js
```

独立端口：

```text
GET :41080/private-admin/assets/app.js
  -> App Nginx rewrite /private-admin/assets/app.js -> /assets/app.js
```

App 根路径不能强制跳到前缀，否则 LB 已剥离前缀的请求会循环跳转。

## 构建合同

发布脚本对每个可构建 App 读取 `<APP_NAME>_PREFIX`：

```text
VITE_APP_CONTEXT_PATH=/<prefix>/
VITE_APP_BASE_API=/<prefix>/<env>-api
```

Vite 会优先使用进程环境，因此无需修改 App 的 `.env.development` 或 `.env.production`。构建产物保存 `.release-prefix` 供部署和检查使用。

## TLS

- 默认 `namewta-nginx-lb` 只监听 HTTP。
- `tls` profile 启动 `namewta-nginx-lb-tls`，读取 `cert/lb/fullchain.pem` 与 `privkey.pem`。
- `error_page 497` 不得携带内部监听端口。
- 保留 `absolute_redirect off`、`port_in_redirect off`、`server_name_in_redirect off`。
- 单 App 独立 TLS 应增加独立 service/template/宿主机端口，不能让多个容器争用同一宿主机 443。

## 新 App 修改面

`add_app.py` 维护五个位置：

1. `docker/frontend/nginx/apps/nginx-<app>.conf.template`
2. `docker/frontend/nginx/html/<app>` 与 `cert/<app>`
3. `docker/docker-compose-frontend.yml`
4. HTTP/TLS LB templates
5. `.env.example` 和已有本地 `.env`

脚本完成后仍需运行发布验证、Compose 解析和容器内 `nginx -t`。
