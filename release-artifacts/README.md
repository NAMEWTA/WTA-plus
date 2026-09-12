# NAMEWTA Release Artifacts

本目录是 WTA-plus monorepo 的发布事实源，包含可复现构建、四类 Docker Compose、Nginx 多 App 入口、运行配置样例、可观测配置与配套 Skill。

## 目录职责

- `builds/`：`current_dev`、`current_prod` 与日期备份；构建产物不提交。
- `bundles/`：可传输部署包；不提交。
- `scripts/release-manage.sh`：构建前端、后端或全部服务，并执行备份、Docker 上下文准备、MySQL 基座只读校验与部署包生成。
- `scripts/docker-manage.sh`：按 `infrastructure`、`observability`、`backend`、`frontend` 四类管理 Compose。
- `docker/`：镜像、Compose、Nginx、基础设施和可观测配置。
- `docker/frontend/nginx/html/nginx-lb/`：统一 LB 的内部静态维护页；上游返回 404 或常见 5xx 时保留原状态码展示。
- `skills/wta-namewta-nginx-config/`：新增前端 App 与维护 LB/Nginx 的项目内动态 Skill。

## 配置

真实密码和私有路径前缀只写入未纳入版本管理的 env 文件。先复制变量名并替换全部占位值：

```bash
cp release-artifacts/.env.example release-artifacts/.env
```

`ADMIN_WEB_PREFIX` 不含首尾斜杠。构建脚本会将其转换为 Vite 的 `/<prefix>/` 和 `/<prefix>/<env>-api`，不修改前端源码环境文件。

发布基座 SQL 仍包含当前版本必须保留的初始化账号/客户端默认凭据（例如初始密码和演示客户端密钥）。这些值只用于首次初始化兼容性，已在发布风险清单中标记；生产环境必须在首次登录后立即轮换，并通过未入库的 env/密钥管理覆盖，禁止在新配置、脚本或文档中新增明文密钥。

当前项目只发布 `apps/admin-web` 管理端和 `apps/home-web` 用户端；未激活终端不参与发布资产。

## 构建

```bash
# 完整生产构建，默认使用后端 bundle-full
bash release-artifacts/scripts/release-manage.sh build \
  --target all --env prod --env-file release-artifacts/.env

# 仅构建前端或后端
bash release-artifacts/scripts/release-manage.sh build --target frontend --env prod
bash release-artifacts/scripts/release-manage.sh build --target backend --env prod

# 备份所有 current_<env>
bash release-artifacts/scripts/release-manage.sh backup

# 将 current_prod 投放到 Docker 构建上下文与 Nginx 静态目录
bash release-artifacts/scripts/release-manage.sh stage --env prod

# 只读校验六份有序 MySQL 初始化 SQL
bash release-artifacts/scripts/release-manage.sh stage-mysql

# 生成独立部署包
bash release-artifacts/scripts/release-manage.sh bundle --env prod
```

构建目录内的 `release-manifest.json` 记录本 monorepo HEAD SHA、环境、产物和 SHA-256，便于追溯。

提交或交付前执行发布目录自检；本机有 Docker Compose 时会额外解析四份 Compose：

```bash
bash release-artifacts/scripts/verify-release.sh
```

Nacos 的真实运行验收会创建并销毁独立的 MySQL、Redis、Nacos 与双应用实例。它不读取
`release-artifacts/.env`，也不复用现有容器或数据卷；必须显式确认后运行：

```bash
./mvnw -f backend/pom.xml -Pbundle-full -DskipTests package
NACOS_E2E_CONFIRM=1 \
  bash release-artifacts/scripts/verify-nacos.sh

# 仅在排障时保留失败现场；成功时仍会自动清理
NACOS_E2E_CONFIRM=1 \
  bash release-artifacts/scripts/verify-nacos.sh --keep-on-failure
```

该门禁覆盖默认关闭、稀疏覆盖、部署参数优先、即时/重启键分类、非法候选原子拒绝、
双实例收敛、运行断连、离线重启本地回退、恢复重连和 MySQL 持久化。服务器上运行时，
可用 `NACOS_E2E_WORK_PARENT` 指向明确的测试目录，用 `NACOS_E2E_APP_JAR` 指向待验收 jar。

## Docker Compose 分类

| 分类 | Compose | 主要服务 |
|---|---|---|
| 基础设施 | `docker-compose-infrastructure.yml` | MySQL、Redis、MinIO；可选 Elasticsearch |
| 日志监控 | `docker-compose-observability.yml` | Monitor Admin、Loki、Alloy、Grafana；可选 Prometheus/exporters |
| 后端 | `docker-compose-backend.yml` | 双实例 Admin、SnailJob、SnailAI |
| 前端 | `docker-compose-frontend.yml` | Nginx LB、各 App 独立 Nginx、可选 LB TLS |

四类 Compose 共享 external bridge network `namewta-network`，管理脚本会在启动前幂等创建。默认宿主机端口只绑定 `127.0.0.1`；需要内网访问时显式设置 `NAMEWTA_BIND_HOST`。

```bash
# 校验全部配置
bash release-artifacts/scripts/docker-manage.sh config all

# 准备产物、构建应用镜像并按顺序启动
bash release-artifacts/scripts/release-manage.sh stage --env prod
bash release-artifacts/scripts/docker-manage.sh build-images
bash release-artifacts/scripts/docker-manage.sh up all

# 启用统一入口 TLS
bash release-artifacts/scripts/docker-manage.sh up frontend --profile tls

# Linux 主机指标与容器指标
bash release-artifacts/scripts/docker-manage.sh up observability --profile metrics
```

不要使用 `docker compose down -v`。MySQL、Redis、MinIO、Loki、Grafana 和 Prometheus 数据均需按 `NAMEWTA_DATA_ROOT` 单独备份。

## MySQL 单库初始化

WTA、SnailJob、WarmFlow、AI 与 NAMEWTA 增量统一初始化到 `ry-namewta`，不按模块拆库。初始化顺序固定为：

```text
10-wta-base.sql
20-ry-job.sql
30-ry-workflow.sql
40-ry-ai.sql
50-namewta-ddl.sql
60-namewta-dml.sql
```

上述目录是项目唯一 SQL 事实源，六份文件均为可直接修改的当前完整 MySQL 8.4 基座，并全部由 Git 跟踪。`50-namewta-ddl.sql` 保存 NAMEWTA 结构，`60-namewta-dml.sql` 保存 NAMEWTA 数据（包括三方接口管理菜单及权限）；Nacos schema 保持在 `init/nacos/` 独立初始化，不并入业务基座。不要新增版本、临时、备份 SQL，也不要在后端仓库恢复 `script/` 或其他数据库方言。

已有共享 MySQL 容器时，先准备 SQL，再运行受保护的初始化脚本。脚本拒绝已存在的数据库或应用账号，失败时只清理本次新建的 `ry-namewta` 和 `namewta_app`：

```bash
bash release-artifacts/scripts/release-manage.sh stage-mysql
bash release-artifacts/scripts/init-mysql-container.sh \
  --container namewta-data-mysql \
  --env-file release-artifacts/.env
```

运行密钥只进入被忽略且权限为 `0600` 的 `.env`，不会写回六份 SQL。共享 MySQL 的其他数据库及其 entrypoint 初始化文件不属于该脚本的操作范围。

已有数据库不得执行上述初始化流程。升级必须明确源 Git Tag 与目标 Git Tag，先备份，再比较两 Tag 下六份基座并形成临时差异 SQL；完成评审和隔离演练后才能执行。差异 SQL、账密、备份位置和执行记录写入被忽略的 `temp/release/`，不作为第七份基座提交。

初始化脚本会将 `minio` 设置为唯一启用的默认 OSS，并强制使用 `access_policy=0`
（`PRIVATE`）；当前枚举中的 `access_policy=2` 表示 `PUBLIC_READ`，不能用于默认私桶。
`.env` 还必须配置 `MINIO_ENDPOINT`、`MINIO_BUCKET` 和 `MINIO_DIAGNOSTIC_OBJECT`。
后端就绪检查会读取该私有探针对象；部署时应同时验证匿名访问被拒绝，以及短时签名链接
在有效期内成功、过期后失效。公共资源应使用独立的公共配置或桶，不得将默认私桶改为公共读。

## Nginx 请求链路

```text
浏览器 /<app-prefix>/
  -> namewta-nginx-lb 剥离 App 前缀
  -> nginx-<app> 根路径静态资源
  -> /dev-api 或 /prod-api 再剥离
  -> namewta-server1 / namewta-server2
```

每个 App Nginx 也映射独立宿主机端口，可直接访问 `http://<host>:<app-port>/<app-prefix>/`。证书只允许运维投放到 `docker/frontend/nginx/cert/`，不得提交私钥。

## 可选 Nacos 配置中心

Nacos 固定使用官方 `nacos/nacos-server:v2.5.4`。客户端默认关闭；准备好数据库、管理员密码、
namespace 和同源代理后，再通过 `docker/overrides/nacos-enabled.yml` 同时开启两套后端实例：

```bash
bash release-artifacts/scripts/release-manage.sh stage-mysql
bash release-artifacts/scripts/docker-manage.sh up infrastructure --profile nacos
docker compose --env-file release-artifacts/.env \
  -f release-artifacts/docker/docker-compose-backend.yml \
  -f release-artifacts/docker/overrides/nacos-enabled.yml up -d namewta-server1 namewta-server2
```

首次启动后必须在受信网络内调用 Nacos 的管理员初始化接口设置强密码，再创建与当前
Spring profile 对应的 namespace。应用账号只需要配置读取权限；控制台管理员凭据不得注入
前端或 Nginx。Nacos 2.5.4 的控制台/API 链路仍是 HTTP 明文协议，因此宿主机端口应保持
`127.0.0.1` 绑定，跨主机访问应置于受信内网或 TLS 反向代理之后。

远程配置是稀疏覆盖，并非任意 YAML 都能无条件热更新。验证码、通知幂等窗口和 OSS 下载
TTL 会即时生效；其他允许键只记录为需重启，`nacos.config.*` 与 `spring.profiles.*` 会被拒绝。
部署环境变量优先级高于远程值。删除配置会回到本地基线；运行中断连保留最后一次有效内存
配置，而 Nacos 不可用时重启应用不会读取磁盘快照，会从本地基线启动并等待服务恢复。

回退时先删除远程覆盖或确认其为空，再把两实例的 `NACOS_CONFIG_ENABLED` 设为 `false` 并
逐一重启。确认业务回到本地配置后，可隐藏系统管理下的 Nacos 菜单并停止 Nacos；数据库
和数据目录应保留，以便审计或前向恢复，禁止使用 `docker compose down -v`。

## 外部依赖

SnailAI 的 Docling 与 PaddleOCR 地址通过 env 注入，本目录不伪造未在当前仓库维护的镜像。SMTP、短信、第三方 OSS 等集成同样由目标环境配置提供。
