# 端口与路由台账

## 当前分配

| 服务 | env 变量 | 默认宿主机端口 | 容器端口 | 路径 |
|---|---|---:|---:|---|
| HTTP LB | `LB_HTTP_PORT` | 40080 | 80 | 已登记业务 App 前缀 |
| TLS LB | `LB_HTTPS_PORT` | 40443 | 443 | `tls` profile |
| Admin Web | `ADMIN_WEB_PORT` | 41080 | 80 | `/${ADMIN_WEB_PREFIX}/` |
| Home Web | `HOME_WEB_PORT` | 41082 | 80 | `/${HOME_WEB_PREFIX}/` |
| SSO Web | `SSO_WEB_PORT` | 41083 | 80 | 独立 Origin `/${SSO_WEB_PREFIX}/` 与 `/sso` |
| SSO TLS | `SSO_WEB_HTTPS_PORT` | 41483 | 443 | 独立 Origin，`tls` profile |
| Monitor Admin | 固定映射 | 49090 | 9090 | `/admin/` |
| SnailJob | 固定映射 | 48800 | 8800 | `/snail-job/` |
| SnailAI | 固定映射 | 48900 | 8900 | `/snail-ai/` |
| 后端实例 | 固定映射 | 48080/48081 | 8080 | App Nginx 内部 upstream |

## 分配规则

1. 新 App 独立宿主机端口从 41080 起选择未占用值，不能占用已有 4xxxx 端口。
2. 业务 App 和 HTTP SSO 内部监听80，TLS SSO 监听443并保留仅内部80健康端口；业务 LB 使用 Docker 服务名寻址，禁止加入 SSO。
3. 前缀只允许字母、数字和连字符，不含首尾斜杠。
4. `admin`、`monitor`、`snail-job`、`snail-ai`、`dev-api`、`prod-api`、`actuator` 是保留前缀；SSO 另禁止静态 prefix 为 `sso`。
5. App 名 `foo-bar` 对应 `FOO_BAR_PREFIX`、`FOO_BAR_PORT` 与 `FOO_BAR_ORIGIN`。
6. 敏感管理端使用随机前缀，真实值只写忽略的 `.env`。

实时台账：

```bash
python release-artifacts/skills/wta-namewta-nginx-config/scripts/add_app.py --list
```

端口/服务/模板以 apps.json 与 Compose 双向校验为准；真实运行端口可由 env 指定，Origin 必须与浏览器实际入口匹配。SSO hostname 必须与业务 App 分离，不能仅换端口。
