# 切换新环境后的部署注意事项

这份说明给换一套运行环境、准备上生产时对照。它只记录当前仓库里已经固定的合同和必须改的配置，不授权执行生产 SQL、切流量、改对象存储策略或发布。那些动作仍要按 [OSS 公私桶运维手册](../../docs/oss-public-private-operations.md) 单独批准。

更长的排障过程见 [OSS 登录与浏览器直传排障](../../docs/error/oss-login-and-direct-upload-troubleshooting.md)。那份手册里的开发代理端口 `18080` 已经过时；当前开发后端端口是 `38888`。

## 后端和三个前端怎么配合

后端是一个 Java 21、Spring Boot 4.1 应用。`backend/wta-admin` 只负责组装，业务在 `wta-modules`，跨模块合同在 `wta-api`，OSS、通知和 HTTP 日志在 `wta-common`。一套后端同时服务管理端、用户门户和第一方 SSO。三个应用的登录域、菜单、角色和 Token 按 Client 分开，不共用会话。

数据是一个 MySQL 8.4 库 `wta-plus`，加上 Redis 和 MinIO。全新空库按六份 SQL 初始化；已经有数据的库不能重放这六份脚本。


| 前端        | 开发时浏览器打开                 | 开发时代理到后端                              | 生产时浏览器打开                   | 生产时接口怎么到后端                         |
| --------- | ------------------------ | ------------------------------------- | -------------------------- | ---------------------------------- |
| admin-web | `http://127.0.0.1:5177/` | `/dev-api` → `http://127.0.0.1:38888` | 业务负载均衡上的 `/<管理端前缀>/`       | `/<前缀>/prod-api`，由 Nginx 去掉前缀后转到后端 |
| home-web  | `http://127.0.0.1:5175/` | 同上                                    | 同一个业务负载均衡上的 `/<门户前缀>/`     | 同上                                 |
| sso-web   | `http://127.0.0.1:4176/` | 同源 `/sso` → `http://127.0.0.1:38888`  | 独立入口 `/<SSO 前缀>/`，不进业务负载均衡 | 页面走 SSO 前缀，接口保持 `/sso`             |


管理端和门户的公开 Client 标识写在各自的 `.env.development` 里，分别是 `e5cd7e4891bf95d1d19206ce24a7b32e` 和 `428a8310cd442757ae699df5d894f051`。它们不是密钥，但必须和库里的 `sys_client.client_id` 一致。SSO 页面不使用这两个标识；它的接口基址是空的 `VITE_SSO_API`，表示走同源 `/sso`。

不要为了换环境去改 `frontend/apps/*/.env.production`。发布脚本会按 env 里覆盖 `VITE_APP_CONTEXT_PATH` 和 `VITE_APP_BASE_API`。`VITE_*` 会进浏览器包，不能放数据库、Redis 或 MinIO 密码。

## 这几种地址不是一回事


| 名称                            | 开发时的值                                 | 它决定什么                                                              |
| ----------------------------- | ------------------------------------- | ------------------------------------------------------------------ |
| 页面 Origin                     | `http://127.0.0.1:5177`、`5175`、`4176` | 浏览器页面的协议、主机和端口。`localhost` 和 `127.0.0.1` 是不同来源，路径和结尾 `/` 不算 Origin |
| 业务接口前缀                        | `/dev-api`，生产是 `/<前缀>/prod-api`       | 只到 Spring Boot。浏览器不把这个前缀写成对象存储地址                                   |
| `WEB_CORS_ALLOWED_ORIGINS`    | 公开模板默认三个 `127.0.0.1` 入口               | 只决定浏览器能不能读取后端的跨源响应。接口仍要登录和权限                                       |
| `SSO_WEB_ORIGIN`              | 发布样例是独立的 SSO 主机                       | SSO 认人页的来源。SSO 路径前缀不放进这条 CORS 名单                                   |
| `MINIO_API_CORS_ALLOW_ORIGIN` | 样例默认 `*`                              | 对象存储自己的浏览器来源白名单。上传时浏览器会直接访问它                                       |
| `sys_oss_config.endpoint`     | 基座先写本机占位，初始化后改成 env 里的值               | 后端连对象存储，也是预签名 URL 里的主机。浏览器必须能够访问它                                  |
| `sys_oss_config.domain_url`   | 初始化不填                                 | 只给以后单独做的公共读配置用。默认私有对象不靠它                                           |


应用 CORS 和对象存储 CORS 要分别改。改了 `WEB_CORS_ALLOWED_ORIGINS`，不会自动放行浏览器向 MinIO 的 PUT。

生产的 `WEB_CORS_ALLOWED_ORIGINS` 必须是逗号分隔的精确 `https://主机[:端口]`。不能是 `*`、不能带路径、不能写通配子域，配错会导致后端启动失败。本机被 Git 忽略的 `application-local.yml` 如果使用单独的 `*`，那只属于当前开发进程，不能抄进生产 env。公开模板 `application-local.example.yml` 列出的是 `http://127.0.0.1:5177,http://127.0.0.1:5175,http://127.0.0.1:4176`。

生产 Origin 必须是 HTTPS。SSO 和两个业务应用必须使用不同主机名，只改端口不能隔离 Cookie。`TRUSTED_PROXY_CIDRS` 按真实网关网段填写，不要猜。

## 上生产时 OSS 要注意什么

文件不经过应用服务器转发。浏览器先调用后端拿到短时预签名地址，再直接 PUT 到对象存储。因此前端构建里不应该、也没有 OSS 地址、桶名或密钥。

后端合同：

- 全库只能有一条启用中的默认配置，配置键是 `minio`，访问类型必须是 `PRIVATE`（`access_policy=0`）。`2` 表示 `PUBLIC_READ`，不能用来当这条默认配置。
- `application.yml` 里的直传策略（general、avatar、image、document 等）都要求私有桶。客户端不能自己指定桶或访问类型。
- 业务表只保存 `ossId`，不保存对象 URL。私有下载使用短时签名，默认下载有效期约 2 分钟，预签名上传约 5 分钟。
- 直传清理默认 `cleanup-enabled=false`、`cleanup-dry-run=true`。不要为了消除 Lifecycle 告警，给整个桶加对象过期规则，否则可能删掉业务文件。
- 就绪检查会读取 `MINIO_DIAGNOSTIC_OBJECT`。部署后要确认匿名访问这个对象被拒绝，签名链接在有效期内能打开、过期后失效。`/actuator/health/ossdiagnostics` 只是观察结果，不代替上述验收。

浏览器和对象存储：

- `MINIO_API_CORS_ALLOW_ORIGIN` 在 `.env.example` 里默认是 `*`，只适合本机。生产改成三个前端真实的 `https://主机`，逗号分隔，不要带路径或结尾 `/`。
- `endpoint` 必须是浏览器能打开的地址。写成仅容器内网可解析的主机名时，后端仍能签发 URL，浏览器 PUT 会失败。
- `init-mysql-container.sh` 会把 `minio` 和 `image` 两行的 `is_https` 写成 `N`。对象存储如果对外是 HTTPS，初始化完成后要把这一列改为 `Y`，否则签名出来的是 HTTP。`MINIO_ENDPOINT` 本身不带 `http://`。
- 分片上传要求对象存储的 CORS 暴露 `ETag`，并允许 `PUT`。签名要求的请求头至少包括 `content-type` 和 `x-amz-meta-upload-fingerprint`，以接口返回的 `requiredHeaders` 为准。
- 公共读必须另建一条非默认配置，并填写浏览器能匿名访问的 `domain_url`。不要把默认私有桶改成公共读。创建桶、Policy、DNS 和证书都不由应用自动完成。

基座 SQL 里还有默认不启用的七牛、阿里云、腾讯云占位行。不要用那些占位密钥把它们启用成默认配置。换到 RustFS 或其他存储时，先迁走桶和对象，再改 `sys_oss_config` 的 endpoint、密钥和桶名，并继续只保留一个名为 `minio` 的私有默认配置。RustFS 不会复制 MinIO 里已有的对象。

## 换一套新的空环境

1. 复制 `release-artifacts/.env.example`，落到未被 Git 跟踪、权限 `0600` 的 env 文件。替换全部占位口令，至少包括数据库、Redis、MinIO，以及 `MINIO_ENDPOINT`、`MINIO_BUCKET`、`MINIO_DIAGNOSTIC_OBJECT`。
2. 填写三个 `*_WEB_PREFIX`、三个 `*_WEB_ORIGIN`、证书目录 `NAMEWTA_CERT_ROOT` 和 `NAMEWTA_BIND_HOST`。管理端前缀不要使用 `admin`、`monitor`、`snail-job` 或 `snail-ai`。样例里的 `.invalid` 主机不能用于真正构建。
3. 确认目标 MySQL 里还没有 `wta-plus` 库，也没有同名应用账号。初始化脚本遇到已存在的库或账号会拒绝执行。
4. 按这个顺序导入，且只导入这些文件：
   ```text
    10-cde-base-ddl.sql
    20-cde-job.sql
    30-cde-workflow.sql
    40-cde-ai.sql
    50-cde-base-dml.sql
    60-cde-nacos.sql
   ```

    产品表结构只改第 1 份，产品数据只改第 5 份。第 6 份初始化独立库 `nacos`，不把 Nacos 表建进 `wta-plus`。不要另加第七份迁移脚本。
5. 导入后，`init-mysql-container.sh` 用 env 覆盖 `minio` 和 `image` 的访问密钥、桶名和 endpoint，并检查启用中的默认配置恰好是私有的 `minio`。然后在对象存储里建好对应的桶和私有探针对象，再把 MinIO CORS 从 `*` 改成三个真实 Origin。
6. 后端使用 `SPRING_PROFILES_ACTIVE=prod`。生产不要关闭 SSO 的 Secure Cookie。OpenAPI 若启用，替换开发用的 KEK，不要沿用仓库里的默认值。
7. 邮件、短信和第三方对象存储不在这六份 SQL 里，由目标环境单独配置。Nacos 默认关闭；要启用时先准备独立库、管理员密码和与 Spring profile 对应的 namespace，再按发布说明打开。

构建和启动命令以 [发布说明](../README.md) 为准。不要使用 `docker compose down -v`，它可能删掉 MySQL、Redis 和 MinIO 的数据。

## 已经有数据的环境

不要再跑上面的初始化。先备份 `wta-plus`，以及 `sys_oss_config`、`sys_oss`、`sys_oss_ref`。升级时指定源 Git 标签和目标 Git 标签，比较两套六份 SQL，形成临时差异脚本，评审并在隔离环境演练后再执行。差异脚本不要提交成新的基座文件。

对象已经写进旧桶之后，不能只改配置行就当作迁移完成。有对象引用的配置不能原地改桶或访问类型。换 endpoint 或换存储前，先迁对象，再改配置。

## 对照时看这些文件


| 要确认的事          | 文件                                                                                                                            |
| -------------- | ----------------------------------------------------------------------------------------------------------------------------- |
| 发布变量样例         | `release-artifacts/.env.example`                                                                                              |
| 新库初始化和 OSS 行覆盖 | `release-artifacts/scripts/init-mysql-container.sh`                                                                           |
| 表结构和种子数据       | `release-artifacts/docker/infrastructure/mysql/init/`                                                                         |
| 直传策略和清理开关      | `backend/wta-admin/src/main/resources/application.yml`                                                                        |
| 本机 CORS 模板     | `backend/wta-admin/src/main/resources/application-local.example.yml`                                                          |
| 三个前端的开发端口和代理   | `frontend/apps/admin-web/.env.development`、`frontend/apps/home-web/.env.development`、`frontend/apps/sso-web/.env.development` |


