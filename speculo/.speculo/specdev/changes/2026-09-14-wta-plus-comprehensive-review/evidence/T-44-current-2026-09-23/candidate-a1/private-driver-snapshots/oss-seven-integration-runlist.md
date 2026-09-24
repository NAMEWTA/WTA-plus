# T44：现有 7 个 OSS IntegrationTest 的真实零 skip 运行清单（只读核查）

核查日期：2026-09-23。未运行 Maven、Docker、MinIO、Redis、MySQL 或测试。此清单是当前源码的输入合同；T44 writer 未冻结的改动须在最终 SHA 再核一次。

| 类（`backend/wta-admin/src/test/java/org/namewta/test/oss/` 下） | 测试数 | 必需服务/额外输入 | 关键边界 |
|---|---:|---|---|
| `readiness/OssStorageReadinessMinioIntegrationTest` | 1 | MinIO endpoint、bootstrap key/secret | 创建私有/公开双桶、写/读 bucket policy、匿名 HEAD/GET/PUT 与只读诊断；需要测试引导管理员身份，不能用 T44 受限 app 身份。 |
| `access/OssAccessUrlMinioIntegrationTest` | 1 | 同上 | 公开无签名 URL、私有短时签名与真实过期 GET；自己建桶/写公开策略。 |
| `client/MinioOssClientIntegrationTest` | 1 | 同上 | 单文件/Multipart、HEAD/GET、策略与清理；自己建桶/写公开策略。 |
| `migration/OssStorageMigrationIntegrationTest` | 1 | MinIO + **独占新 MySQL 数据库** + SQL 基座目录 | `prepareDatabase()` 开始和 finally 均 `dropTables()` 五张 `sys_oss*` 表，再从 DDL 重建；绝不能指向 103 表的 `wta-plus` 或应用正在使用的 DB。迁移主要是 T46 交界，不等于 T44 全应用验收。 |
| `upload/OssUploadStorageRoutingMinioIntegrationTest` | 1 | MinIO endpoint、bootstrap key/secret | 双桶真实 PUT/HEAD，Ticket/Metadata 是内存实现；不能代替真实 Admin HTTP/Redis/MySQL。 |
| `upload/BrowserUploadLifecycleIntegrationTest` | 1 Maven 用例，内含 10 个 Playwright case | MinIO；`frontend/apps/admin-web/dist/index.html`、frontend node_modules、Chrome、`corepack/pnpm`；另需显式 browser 开关及仓库根路径 | 自建桶和本地静态 HTTP，控制面经 Playwright route 模拟，真实对象字节落 MinIO；不是 T44 后端控制面。子进程结果须另核 10 case/0 skip。 |
| `upload/RedisOssUploadTicketStoreIntegrationTest` | 1 | **独立无认证 Redis** 的随机 loopback 端口 | Redisson `StringCodec`，仅 `.setAddress(redis://127.0.0.1:<port>)`，没有 password 配置；不能复用 T44 full-app 的有密码 Redis。 |

七类均 `@Tag("dev")`，需后端 `-Pdev`；Maven selector 在 `backend` 下为 `-pl wta-admin -am -Dtest=OssStorageReadinessMinioIntegrationTest,OssAccessUrlMinioIntegrationTest,MinioOssClientIntegrationTest,OssStorageMigrationIntegrationTest,OssUploadStorageRoutingMinioIntegrationTest,BrowserUploadLifecycleIntegrationTest,RedisOssUploadTicketStoreIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test`。这里只列选择器，不给含密钥命令。Maven 父 POM 将 `groups` 绑定到 `profiles.active`；未显式 `-Pdev` 会有筛选风险。

精确 Java System properties：

| 属性 | 使用方 | 约束 |
|---|---|---|
| `oss.minio.integration.endpoint` | 六个 MinIO 类 | `http://127.0.0.1:<owned-random-port>`；五类在缺失时 `Assumptions` skip，浏览器类在启用后会直接失败。 |
| `oss.minio.integration.access-key`, `oss.minio.integration.secret-key` | 六个 MinIO 类 | 当前源码均有 `namewta`/`namewta123` 默认值，且直接读 `System.getProperty`。这些类自行建桶、设置/删除 public bucket policy，必须用**仅本次 owned MinIO 的 bootstrap 身份**；受限 app 身份不能满足这些旧夹具。不得把现场或生产凭据传入属性。 |
| `oss.migration.mysql.integration.url`, `.username`, `.password` | 迁移类 | URL 指向新建的专用空库（例如 `jdbc:mysql://127.0.0.1:<owned-port>/t44_oss_migration?...`）；类会 DROP/CREATE 五表。用户名默认 `root`、密码默认空串，但应绑定 owned DB 私有输入。 |
| `namewta.sql.root` | 迁移类经 `SqlBaselinePaths` | 建议显式 `/srv/WTA-plus/release-artifacts/docker/infrastructure/mysql/init`；在 backend 聚合仓库运行也可自动解析。 |
| `oss.upload.redis.integration.port` | Redis 类 | 真实无密码 Redis 随机 loopback 端口；缺失或非正数即 skip。 |
| `browser.upload.integration` | 浏览器类 | 必须为 `true`，否则 `@EnabledIfSystemProperty` skip。 |
| `namewta.repo.root` | 浏览器类 | `/srv/WTA-plus`；用来找 Admin `dist`，缺失则失败。 |

当前代码没有为这些旧类提供读取私有凭据文件或环境变量的入口。将随机密钥直接作为 `-D...` 写进 Maven argv 会进入进程列表及可能的测试报告；`scripts/ci/run-external-services.sh` 正是固定 throwaway 凭据 argv 模式，不能原样声称为 T44 私有凭据门禁。正式执行可先由 Lead 决定：在精确测试写集内增加私有文件/env seam，或仅以一次性、公开可弃的 owned bootstrap 凭据运行旧七类并将其与 T44 受限 app 身份验收明确分开。此处不改测试。

已存在的隔离资产：

- `scripts/ci/run-external-services.sh`：可直接运行的通用外部服务脚本，但固定凭据/argv、MinIO 旧镜像、测试选集仅包含上述 **RedisOssUploadTicketStoreIntegrationTest** 与 **MinioOssClientIntegrationTest**；不满足本票七类 fresh 零 skip 和私有凭据要求。
- `scripts/ci/verify-external-tests.mjs`：接受 `backend`、开始时间纳秒、七个类名，逐个要求 fresh Surefire XML 中 tests>0 且 fail/error/skip 均 0。它只验证 Maven XML；浏览器类内部的 Playwright 10 case 仍需单独核其零 skip/失败。
- `frontend/e2e/run-notice-retraction-real.py`：已审 owned 双标签 Docker、六 SQL、全 ID/卷/PGID/port 清理、exact clean/JAR proof、MinIO/MySQL/Redis 健康等 helper，可复用**资源生命周期实现**，但其 Redis 带密码，不能不改旧 Redis 测试直接复用。`/tmp/wta-t44/run-minio-real.py` 则是 T44 full-app 受限身份驱动，已冻结且未执行，不应为七类测试在执行时混改。

最终 Lead 需在无并行 writer/build 的固定源码点启动独立 owned 服务，运行七类并留存每类 fresh XML、退出码、零 skip；迁移 DB 和 Redis 要按上述边界独立分配。不要把旧测试的人为 registry `SERVING`、内存 Ticket/Metadata 或 browser route 模拟解读为 T44 新生产路径验收。
