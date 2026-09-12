# 端口与路由台账

## 当前分配

| 服务 | env 变量 | 默认宿主机端口 | 容器端口 | 路径 |
|---|---|---:|---:|---|
| HTTP LB | `LB_HTTP_PORT` | 40080 | 80 | 所有 App 前缀 |
| TLS LB | `LB_HTTPS_PORT` | 40443 | 443 | `tls` profile |
| Admin Web | `ADMIN_WEB_PORT` | 41080 | 80 | `/${ADMIN_WEB_PREFIX}/` |
| Monitor Admin | 固定映射 | 49090 | 9090 | `/admin/` |
| SnailJob | 固定映射 | 48800 | 8800 | `/snail-job/` |
| SnailAI | 固定映射 | 48900 | 8900 | `/snail-ai/` |
| 后端实例 | 固定映射 | 48080/48081 | 8080 | App Nginx 内部 upstream |

## 分配规则

1. 新 App 独立宿主机端口从 41081 起递增，不能占用已有 4xxxx 端口。
2. App 容器内部统一监听 80，LB 使用 Docker 服务名寻址。
3. 前缀只允许字母、数字和连字符，不含首尾斜杠。
4. `admin`、`monitor`、`snail-job`、`snail-ai`、`dev-api`、`prod-api` 是保留前缀。
5. App 名 `foo-bar` 对应 `FOO_BAR_PREFIX` 与 `FOO_BAR_PORT`。
6. 敏感管理端使用随机前缀，真实值只写忽略的 `.env`。

实时台账：

```bash
python release-artifacts/skills/wta-namewta-nginx-config/scripts/add_app.py --list
```
