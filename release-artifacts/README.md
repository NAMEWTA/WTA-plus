# NAMEWTA Release Artifacts

本目录是 WTA-plus monorepo 的发布事实源，包含可复现构建、四类 Docker Compose、Nginx 多 App 入口、运行配置样例、可观测配置与配套 Skill。

## 目录职责

- `builds/versions/<ID>/`：不可变完整版本；`current_dev`、`current_prod` 是唯一选择指针，`pins/` 保留备份引用，`development/` 仅放局部开发输出；构建产物不提交。
- `bundles/`：可传输部署包；不提交。
- `scripts/release-manage.sh`：构建前端、后端或全部服务，完整构建生成版本，`stage --release ID` 原子选择版本；另提供备份引用、MySQL 基座只读校验与部署包生成。
- `scripts/docker-manage.sh`：按 `infrastructure`、`observability`、`backend`、`frontend` 四类管理 Compose。
- `apps.json`：显式 App 发布清单，登记 package、shipped、prefix/Origin/port 变量、API、callback、模板及入口。
- `docker/`：镜像、Compose、Nginx、基础设施和可观测配置。
- `docker/frontend/nginx/html/nginx-lb/`：统一 LB 的内部静态维护页；上游返回 404 或常见 5xx 时保留原状态码展示。
- `skills/wta-namewta-nginx-config/`：新增前端 App 与维护 LB/Nginx 的项目内动态 Skill。

所有示例命令默认从仓根执行（明确 `cd backend` 的子 shell 除外）。发布清单和本地夹具验证不代表已提交的干净版本或已部署环境；以具体 manifest、验收证据与批准记录判定交付状态。

## 配置

真实密码只写入未纳入版本管理的 env 文件。私有路径前缀从该文件输入，构建后会出现在浏览器资产和产物 manifest 中，不写回源码。先复制变量名并替换全部占位值：

```bash
cp release-artifacts/.env.example release-artifacts/.env
```

`ADMIN_WEB_PREFIX` 不含首尾斜杠。构建脚本会将其转换为 Vite 的 `/<prefix>/` 和 `/<prefix>/<env>-api`，不修改前端源码环境文件。

发布基座 SQL 仍包含当前版本必须保留的初始化账号/客户端默认凭据（例如初始密码和演示客户端密钥）。这些值只用于首次初始化兼容性，已在发布风险清单中标记；生产环境必须在首次登录后立即轮换，并通过未入库的 env/密钥管理覆盖，禁止在新配置、脚本或文档中新增明文密钥。

完整构建以 `apps.json` 的显式 shipped 清单决定发布面；目录扫描仅检测未登记或缺失 App。当前发布清单登记 admin-web、home-web、sso-web，逐项校验 package、构建命令、Compose 服务及挂载、Nginx、env、端口和入口。`release-manage.sh check-apps` 可只读检查配套。缺项或漂移在构建/晋升前失败，不改变已选版本。

运行 env 必须填写三个 `*_WEB_ORIGIN`（仅 scheme、hostname、可选 port）和 `*_WEB_PREFIX`。生产 Origin 必须 HTTPS，SSO 与业务 App 必须使用不同 hostname；仅换端口不能隔离 Cookie。真实 DNS、TLS 和端口由部署环境确定，样例中的 `.invalid` 占位值不能用于构建。manifest 的 `appOrigins` 和 `applicationMatrix` 保存规范化 Origin、base、API、callback/authorize URL，需将精确 callback URL 登记到后端对应 SSO Client。

## 构建

```bash
# 完整生产构建，默认使用后端 bundle-full
bash release-artifacts/scripts/release-manage.sh build \
  --target all --env prod --env-file release-artifacts/.env

# 仅构建前端或后端：写入 development，不能 stage，不继承 current
bash release-artifacts/scripts/release-manage.sh build --target frontend --env prod
bash release-artifacts/scripts/release-manage.sh build --target backend --env prod

# 为当前版本增加备份引用；不移动或清空 current
bash release-artifacts/scripts/release-manage.sh backup

# 使用完整 build 最后一行输出的 ID（环境、源码前缀、完整 manifest SHA-256）
bash release-artifacts/scripts/release-manage.sh stage --env prod --release <ID>

# 只读校验六份有序 MySQL 初始化 SQL
bash release-artifacts/scripts/release-manage.sh stage-mysql

# 生成独立部署包
bash release-artifacts/scripts/release-manage.sh bundle --env prod
```

需要 Linux、Bash、Git、JDK/Maven Wrapper、Node.js >=20.19、pnpm、Docker Compose，以及发布锁使用的 `flock`（util-linux）。完整构建拒绝 tracked/untracked 脏源码，将同一 Git revision 归档到本次临时目录，从该快照完成后端 clean package、依赖安装和逐 App 构建；不创建 Git worktree，不复用原工作树 target/dist。局部开发构建不产生可晋升的 manifest。

`release-manifest.json` schema 2 记录唯一 source revision、tree、源码归档摘要、构建模式、三端 Origin/入口矩阵和每文件的来源、SHA-256、大小、可执行位。版本内同时包含四个 JAR、各 App、六份 SQL 快照、Nginx/Compose 配置与运行脚本。SQL 快照是构建输出，唯一编辑源仍是本仓六份基座。manifest 校验用于检测错件、损坏和混源，不替代可信发布者签名或真实 MySQL schema 门禁。

build 完成后仍不改变 current。stage 重新校验完整版本，再在同一目录内用一次 rename 替换符号链接；此前失败或中断保持旧指针和所有源 Docker 上下文不变。跨过该原子边界后看到完整新版本；不能承诺进程在切换后被强杀仍返回成功。发布操作通过固定 inode 的 `flock` 串行，失败只清理本次随机临时目录，不扫描删除历史或他人的 stage。SIGKILL 留下的临时目录不自动清理，需核对 owner 后单独处理。旧的实体 current 目录被明确拒绝，不自动迁移或删除。

恢复使用同一 `stage --env prod --release <旧ID>` 选择已校验历史版本，再显式重建对应容器。backup 只增加引用，bundle 只封装已校验的固定版本，不复制运行 env、日志、证书或原 Docker 上下文。解包后创建本地 `.env`；`resolve --env prod` 可重新验证并输出固定版本路径。

提交或交付前执行发布目录自检；本机有 Docker Compose 时会额外解析四份 Compose：

```bash
bash release-artifacts/scripts/verify-release.sh
```

Nacos 的真实运行验收会创建并销毁独立的 MySQL、Redis、Nacos 与双应用实例。它不读取
`release-artifacts/.env`，也不复用现有容器或数据卷；必须显式确认后运行：

```bash
(cd backend && ./mvnw -Pbundle-full -DskipTests package)
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
| 基础设施 | `docker-compose-infrastructure.yml` | MySQL、Redis、MinIO；可选 Nacos、RustFS、Elasticsearch |
| 日志监控 | `docker-compose-observability.yml` | Monitor Admin、Loki、Alloy、Grafana；可选 Prometheus/exporters |
| 后端 | `docker-compose-backend.yml` | 双实例 Admin、SnailJob |
| 前端 | `docker-compose-frontend.yml` | 业务 App LB、各 App 独立 Nginx、可选 LB/独立 SSO TLS |

四类 Compose 共享 external bridge network `namewta-network`，管理脚本会在启动前幂等创建。默认宿主机端口只绑定 `127.0.0.1`；需要内网访问时显式设置 `NAMEWTA_BIND_HOST`。

```bash
# 校验全部配置
bash release-artifacts/scripts/docker-manage.sh config all

# 选择完整版本并按顺序启动（实际部署需单独授权）
bash release-artifacts/scripts/release-manage.sh stage --env prod --release <ID>
bash release-artifacts/scripts/docker-manage.sh build-images
bash release-artifacts/scripts/docker-manage.sh up all

# 启用业务 LB 与独立 SSO TLS
bash release-artifacts/scripts/docker-manage.sh up frontend --profile tls

# Linux 主机指标与容器指标
bash release-artifacts/scripts/docker-manage.sh up observability --profile metrics

# 启用备用对象存储 RustFS。不替代 MinIO，也不复制已有对象。
bash release-artifacts/scripts/docker-manage.sh up infrastructure --profile rustfs
```

每条 docker-manage 命令先解析并校验一次 current，整个命令固定该版本的 Compose、镜像上下文和只读挂载；`up` 显式使用 `--build --force-recreate`，应用镜像标签包含版本 ID。已运行容器的 bind mount 不会自动跟随指针。多服务的运行时升级不是原子事务；失败需选择旧版本并重建受影响服务，再验证健康。

运行前缀及 Origin 必须与 manifest 一致，否则 Docker 操作前拒绝。docker-manage 从固定版本导出精确 `WEB_CORS_ALLOWED_ORIGINS` 和 `SSO_WEB_ORIGIN`，后端 Compose 传入配置；`SSO_WEB_BASE_PATH` 从固定版本的 SSO prefix 派生为 `/<prefix>/`，真实 `/auth/client/context` 将它与独立 Origin 组合为授权页面地址。该 base 不进入 CORS Origin。`TRUSTED_PROXY_CIDRS` 仍需按真实拓扑显式配置，不能猜测可信网段。`RELEASE_ENV` 默认 `prod`，`RELEASE_ENV_FILE` 指定运行 env。持久数据默认仍位于发布根的 `docker/runtime`，相对 `NAMEWTA_DATA_ROOT` 也锚定发布根的 docker 目录；Nginx 日志在其 `nginx/log/` 下。证书由 `NAMEWTA_CERT_ROOT` 指定，默认发布根的 `docker/frontend/nginx/cert`。这些运行目录不进入不可变版本，不随版本切换迁移。

不要使用 `docker compose down -v`。MySQL、Redis、MinIO、RustFS、Loki、Grafana 和 Prometheus 数据均需按 `NAMEWTA_DATA_ROOT` 单独备份。

## 可选 RustFS 备用对象存储

RustFS 提供与 MinIO 同类的 S3 API 和控制台，只在 profile `rustfs` 下启动，不替代默认 OSS。镜像钉死 `rustfs/rustfs:1.0.0`。S3 API 宿主机端口 `49002`，控制台 `49003`，只绑定 `NAMEWTA_BIND_HOST`。数据在 `NAMEWTA_DATA_ROOT/rustfs/data`，日志在 `NAMEWTA_DATA_ROOT/rustfs/logs`。进程用户是 `10001:10001`；`rustfs-perms` 会在启动前把这两个目录改成该属主。

这是单目录部署，不能原地扩成多盘，也不会复制 MinIO 里已有的对象。切换业务流量前，先迁桶和对象，再手改 `sys_oss_config` 的 endpoint、访问密钥和桶，并保持 `access_policy=0`。不要把 RustFS 设成第二个 `status=Y` 的默认配置；初始化合同仍然只允许一个名为 `minio` 的私有默认 OSS。`RUSTFS_ACCESS_KEY` 或 `RUSTFS_SECRET_KEY` 为空时容器拒绝启动。

## MySQL 单库初始化

WTA、SnailJob、WarmFlow、AI 与 NAMEWTA 统一初始化到业务库 `wta-plus`，不按模块拆业务库。初始化顺序固定为：

```text
10-cde-base-ddl.sql
20-cde-job.sql
30-cde-workflow.sql
40-cde-ai.sql
50-cde-base-dml.sql
60-cde-nacos.sql
```

上述目录是项目唯一 SQL 事实源，全部由 Git 跟踪。产品表结构只直接改 `10-cde-base-ddl.sql`，产品数据只直接改 `50-cde-base-dml.sql`；job/workflow/ai 为上游快照。`60-cde-nacos.sql` 初始化独立库 `nacos`，不把 Nacos 表建进 `wta-plus`。不要新增版本、临时、备份 SQL 或 `migrate/` 目录，也不要在后端仓库恢复 `script/` 或其他数据库方言。

已有共享 MySQL 容器时，先准备 SQL，再运行受保护的初始化脚本。脚本拒绝已存在的数据库或应用账号，失败时只清理本次新建的 `wta-plus` 和 `namewta_app`：

```bash
bash release-artifacts/scripts/release-manage.sh stage-mysql
release_version="$(bash release-artifacts/scripts/release-manage.sh resolve --env prod)"
bash release-artifacts/scripts/init-mysql-container.sh \
  --sql-dir "${release_version}/docker/infrastructure/mysql/init" \
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

业务 App Nginx 也映射独立宿主机端口，可直接访问 `http://<host>:<app-port>/<app-prefix>/`。证书由运维投放到上述独立 `NAMEWTA_CERT_ROOT`，不得提交私钥。

SSO 使用独立入口，**不加入业务 LB**：`/<SSO_WEB_PREFIX>/authorize` 提供页面与刷新回退，根 `/sso/*` 保持原路径反代后端。构建设置 `VITE_SSO_API` 为空，路由和资源共用 Vite base。根 `/` 不跳转到登录页；`/healthz` 返回健康状态。SSO 入口拒绝非自身 Origin 的浏览器 API 请求，Cookie 保持后端默认 Secure、HttpOnly、host-only，不传给其他 hostname。

| App | HTTP 变量/默认端口 | HTTPS 入口 | API |
|---|---|---|---|
| admin-web | `ADMIN_WEB_PORT` / 41080 | 业务 LB `LB_HTTPS_PORT` / 40443 | `/<prefix>/<env>-api` |
| home-web | `HOME_WEB_PORT` / 41082 | 业务 LB `LB_HTTPS_PORT` / 40443 | `/<prefix>/<env>-api` |
| sso-web | `SSO_WEB_PORT` / 41083 | `SSO_WEB_HTTPS_PORT` / 41483 | `/sso` |

SSO TLS 服务读取 `${NAMEWTA_CERT_ROOT}/sso-web/fullchain.pem`、`privkey.pem`；业务 LB 读取 `lb/`。默认 HTTP 端口只绑定 loopback，可供可信 TLS 入口转发；生产浏览器只能使用配置的 HTTPS Origin。SSO TLS 的 HTTP 健康端口只供容器内部探测。access log 仅记录方法、路径与状态，省略授权查询参数；这不代表所有 Nginx error log 已完成凭据脱敏。

串行三 Origin 本地验证（需要 Docker、JDK、Node/pnpm、Chrome、OpenSSL）可运行：

```bash
bash scripts/sso-hard-e2e.sh --release-origin --evidence /tmp/namewta-sso-release-result.json
```

它构建三 App，使用实际发布 Nginx 模板、SSO Controller/UseCase、Redis 和 MySQL，通过专用 Playwright 配置验证登录、刷新、过期重登、health 和 Cookie 边界。System 身份/菜单与业务 token 签发为显式夹具，不冒充完整 System 验收。脚本不读取真实运行 env、不构建或晋升 deployable 版本；仅删除本次记录 ID 的容器、卷和网络，并核对资源清单。真实 clean 源码完整版本和生产 DNS/TLS 仍按发布 Gate 验收。

## 可选 Nacos 配置中心

当前 Data ID 固定为 `wta-namewta.yml`（`NacosConfigConstants.DEFAULT_DATA_ID`），不双读旧 `ruoyi-namewta.yml`。Docker 产品服务保持 `namewta-*`，容器工作目录位于 `/wta/...`；升级前按当前配置准备新 Data ID，不以旧名称作为自动回退。

Nacos 固定使用官方 `nacos/nacos-server:v2.5.4`。客户端默认关闭；准备好数据库、管理员密码、
namespace 和同源代理后，再通过 `docker/overrides/nacos-enabled.yml` 同时开启两套后端实例：

```bash
bash release-artifacts/scripts/release-manage.sh stage-mysql
bash release-artifacts/scripts/docker-manage.sh up infrastructure --profile nacos
bash release-artifacts/scripts/docker-manage.sh up backend --nacos
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

Snail AI 服务及其 Docling/PaddleOCR 接入已退出；两份 Java Maven 占位保留。40-cde-ai.sql 仅声明字符集，六份 SQL 初始化新业务库103张表，已有 AI 数据保留且不迁移，禁止重放基座。SMTP、短信、第三方 OSS 等集成同样由目标环境配置提供。
