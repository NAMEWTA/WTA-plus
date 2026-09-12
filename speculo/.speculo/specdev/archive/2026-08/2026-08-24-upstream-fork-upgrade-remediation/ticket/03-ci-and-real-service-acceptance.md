---
schema_version: 3
artifact: ticket
change: 2026-08-24-upstream-fork-upgrade-remediation
id: T-03
title: CI 与真实外部服务验收
status: done
planning_depth: standard
planning_depth_reason: 新增仓库级 CI 和一次性 Redis/MySQL/MinIO 集成接缝，并修复真实浏览器验收发现的既有 OSS 直传合同偏差
ready: true
risk: high
blocked_by: [T-01, T-02]
contract_ids: [AC-003]
owner: codex
expected_changes: ["<Path>.github/workflows/quality-gates.yml</Path>", "<Path>scripts/ci/**</Path>", "<Path>scripts/start-dev.sh</Path>", "<Path>scripts/README.md</Path>", "<Path>plus-ui-namewta/src/api/system/oss/types.ts</Path>", "<Path>plus-ui-namewta/src/hooks/oss/useDirectOssUpload.ts</Path>", "<Path>plus-ui-namewta/src/hooks/oss/useDirectOssUpload.test.ts</Path>", "<Path>plus-ui-namewta/src/components/FileUpload/index.vue</Path>", "<Path>plus-ui-namewta/src/components/ImageUpload/index.vue</Path>", "<Path>plus-ui-namewta/src/views/system/user/profile/userAvatar.vue</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/client/MinioOssClientIntegrationTest.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/upload/OssUploadHttpContractUnitTest.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/upload/OssUploadServiceUnitTest.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/notify/monitor/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/application-local.yml</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysOssUploadController.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/upload/OssUploadContracts.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/upload/OssUploadService.java</Path>", "<Path>ruoyi-vue-plus-namewta/script/docker/docker-compose.yml</Path>", "<Path>ruoyi-vue-plus-namewta/script/docker/.env.example</Path>", "<Path>ruoyi-vue-plus-namewta/script/docker/README.md</Path>", "<Path>ruoyi-vue-plus-namewta/script/docker/mysql/conf/namewta-client.cnf</Path>", "<Path>ruoyi-vue-plus-namewta/script/docker/redis/conf/redis.conf</Path>"]
writable_paths: ["<Path>.github/workflows/**</Path>", "<Path>scripts/ci/**</Path>", "<Path>scripts/start-dev.sh</Path>", "<Path>scripts/README.md</Path>", "<Path>plus-ui-namewta/src/api/system/oss/types.ts</Path>", "<Path>plus-ui-namewta/src/hooks/oss/useDirectOssUpload.ts</Path>", "<Path>plus-ui-namewta/src/hooks/oss/useDirectOssUpload.test.ts</Path>", "<Path>plus-ui-namewta/src/components/FileUpload/index.vue</Path>", "<Path>plus-ui-namewta/src/components/ImageUpload/index.vue</Path>", "<Path>plus-ui-namewta/src/views/system/user/profile/userAvatar.vue</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/client/MinioOssClientIntegrationTest.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/upload/OssUploadHttpContractUnitTest.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/upload/OssUploadServiceUnitTest.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/notify/monitor/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/application-local.yml</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysOssUploadController.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/upload/OssUploadContracts.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/upload/OssUploadService.java</Path>", "<Path>ruoyi-vue-plus-namewta/script/docker/docker-compose.yml</Path>", "<Path>ruoyi-vue-plus-namewta/script/docker/.env.example</Path>", "<Path>ruoyi-vue-plus-namewta/script/docker/README.md</Path>", "<Path>ruoyi-vue-plus-namewta/script/docker/mysql/conf/namewta-client.cnf</Path>", "<Path>ruoyi-vue-plus-namewta/script/docker/redis/conf/redis.conf</Path>"]
read_only_paths: ["<Path>ruoyi-vue-plus-namewta/pom.xml</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/pom.xml</Path>"]
shared_paths: []
shared_path_owners: []
---

# Ticket T-03: CI 与真实外部服务验收

- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-24-upstream-fork-upgrade-remediation/evidence/T-03.md</Path>`

## 1. 战略与来源

- **目标：** 把本地门禁和已有但默认跳过的真实集成测试变成 PR 可重复执行的质量合同。
- **来源：** CR-001 repository/E2E findings、CR-002 test/browser finding；AC-003。

## 2. 决策状态

- CI 分为 snapshot、frontend、backend、external-services 四个可定位 job。
- Redis/MySQL 激活现有三项测试；MinIO 新测试通过真实 presigned PUT/GET 和 multipart 生命周期验证 S3 数据面。
- 主 Compose 的显式容器名统一使用 `ruoyi-namewta-` 前缀，与上游默认实例并存时可直接区分。
- 主 Compose 使用独立 bridge 网络和 4xxxx 宿主机端口；middleware 默认启用，frontend/backend 通过 profile 显式启用。
- 部署 secret 只来自仓库外 env 文件，Compose 不保留示例弱口令。
- secret 使用 CI 临时值，不进入生产配置；所有测试对象按唯一 key 清理。
- 未决问题：无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| parent CI、submodule check、MinIO test、主 Compose 容器命名/端口/初始化、远程 middleware 验收、本地前后端启动、一键前台启动脚本、OSS 直传完成响应与同票据恢复 | 已有 Redis/MySQL tests、SQL、两端 scripts 和 OSS 上传状态机 | frontend/backend 容器部署、其他 Compose 文件、远程 required check 设置、全局内容去重 |

## 4. 要构建什么

每个 PR/主分支候选都有可定位的 snapshot、frontend、backend、external-services 结果；真实 Redis/MySQL/S3 失败会使验收 job 失败，而不是被 Assumption 隐藏。

主 Compose 的所有容器使用唯一的 `ruoyi-namewta-*` 名称和 4xxxx 宿主机端口，不与原上游 Compose 容器名或端口混淆；本次远程只部署 MySQL、Redis 和 MinIO。

## 5. 实现契约

- snapshot job 拒绝未初始化或 SHA 漂移的 submodule。
- frontend/backend job 调用项目脚本，不复制隐藏参数。
- external-services job 等待健康后才执行；服务不可用时失败而非 skip。
- MinIO 测试只在显式 system properties 存在时运行，本地默认套件仍可用。
- `<Path>ruoyi-vue-plus-namewta/script/docker/docker-compose.yml</Path>` 的每个 `container_name` 必须唯一且以 `ruoyi-namewta-` 开头，每个宿主机端口唯一且位于 40000-49999。
- 删除会使端口映射失效的 host 网络，改用独立 bridge 网络；不改变容器内部监听端口。
- MySQL fresh 数据目录按权威顺序执行上游与 NAMEWTA SQL；已有数据目录不得自动重放。
- 远程 secret 不写入仓库、命令参数、Evidence 或日志；旧 `cde-infra` 容器和数据保持只读。
- 本地后端显式激活 `dev,local`，由被 Git 忽略的 `application-local.yml` 最后覆盖 dev 配置，不把远程 secret 写入受版本控制文件；前端仍代理本地 `8080`。
- Complete 成功响应必须把字符串 `ossId` 放在 `data`，不得因 `R.ok(String)` 重载落入 `msg`。
- 已完成票据的 Resume 必须返回完成状态与同一 `ossId`；前端清理遗留恢复记录并直接返回，不重复 PUT 或 Complete。
- 上传错误提示必须保留本地状态机错误；request adapter 已提示的服务端错误不得重复弹窗。
- `<Path>scripts/start-dev.sh</Path>` 必须只有“启动前端”和“启动后端”两个菜单选项；非法或 EOF 输入非零退出。
- 前端使用 lockfile 固定的 pnpm 安装后以前台 Vite 进程运行；后端先刷新本地 reactor，再以前台 `dev,local` Spring Boot 进程运行。
- 启动脚本不得后台化、吞日志、自动杀端口进程或输出本地配置内容；最终服务进程使用 `exec` 接管当前脚本进程。

## 6. 执行路线

1. 建立 submodule 断言脚本与 MinIO 测试 red 证据。
2. 创建 CI jobs 和临时服务，注入已有测试要求的 properties。
3. 为主 Compose 增加 NAMEWTA 命名空间、bridge 网络、4xxxx 映射、profiles、healthcheck、fresh SQL 和外置 secret。
4. 远程只读预检并只部署 MySQL、Redis、MinIO，验证不影响旧 `cde-infra`。
5. 本地与远程运行 Compose、数据库、Redis、MinIO 和定向集成验收。
6. 使用 `dev,local` profiles 启动本地后端并启动前端 development server，执行登录前接口 smoke。
7. 为 Complete 响应、已完成票据 Resume 和前端恢复短路建立回归测试，再修复生产实现。
8. 运行完整两端门禁和 diff 审计。
9. 新增根目录可调用的两选项前台启动脚本，验证端口保护、实时日志和 `Ctrl+C` 生命周期。

## 7. 路径访问契约

- **可写范围：** CI、父仓 CI scripts、本地前台启动脚本及 scripts 说明、OSS 直传前后端实现与定向测试、上传组件错误展示、单个 MinIO integration test、主 Compose、env 示例、MySQL/Redis 配置和 Docker 部署说明。
- **只读上下文：** 两端 scripts/POM 和现有集成测试。
- **共享路径：** 无。
- **保留或不动：** 生产配置、secret、数据库记录、OSS 管理列表 URL 脱敏语义和全局内容去重策略。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| SHA 漂移 | submodule script | `<Path>scripts/ci/verify-submodules.sh</Path>` | 任一 `+/-/U` 状态非零退出 | `<Path>{roots.state}/specdev/changes/2026-08-24-upstream-fork-upgrade-remediation/evidence/T-03.md</Path>` |
| Redis/MySQL | Maven properties | integration test command | 三项测试执行且零 skipped | `<Path>{roots.state}/specdev/changes/2026-08-24-upstream-fork-upgrade-remediation/evidence/T-03.md</Path>` |
| S3 | MinIO | MinIO integration test | single/multipart/sign/download/cleanup 通过 | `<Path>{roots.state}/specdev/changes/2026-08-24-upstream-fork-upgrade-remediation/evidence/T-03.md</Path>` |
| 容器隔离 | Compose model + remote runtime | `docker compose config`、名称/端口断言、远程 `docker compose ps` | 名称唯一；17 个宿主机端口均为 4xxxx；只运行 3 个 middleware | `<Path>{roots.state}/specdev/changes/2026-08-24-upstream-fork-upgrade-remediation/evidence/T-03.md</Path>` |
| fresh 初始化 | MySQL + SQL inventory | 空数据目录启动、表/菜单断言 | 上游与 NAMEWTA SQL 顺序执行，业务菜单退役块生效 | `<Path>{roots.state}/specdev/changes/2026-08-24-upstream-fork-upgrade-remediation/evidence/T-03.md</Path>` |
| 本地前后端 | Spring `dev,local` profiles + Vite development | 后端启动、前端启动、proxy API smoke | secret 不受 Git 跟踪；8080 与前端端口监听；登录前接口经 `/dev-api` 可用 | `<Path>{roots.state}/specdev/changes/2026-08-24-upstream-fork-upgrade-remediation/evidence/T-03.md</Path>` |
| 一键前台启动 | root Bash launcher | 菜单 1/2、非法输入、端口占用、`Ctrl+C` | 仅两个选项；前后端实时日志留在当前终端；无后台残留；不泄露配置 | `<Path>{roots.state}/specdev/changes/2026-08-24-upstream-fork-upgrade-remediation/evidence/T-03.md</Path>` |
| OSS 直传恢复 | controller/service/hook tests + 本地真实服务 | Complete、Resume、同文件重试 | `data` 返回 `ossId`；已完成票据不重复传输；错误信息可定位且无双 toast | `<Path>{roots.state}/specdev/changes/2026-08-24-upstream-fork-upgrade-remediation/evidence/T-03.md</Path>` |
| 回归 | workflow jobs | workflow command review | lint/typecheck/test/build/full/core 全覆盖 | `<Path>{roots.state}/specdev/changes/2026-08-24-upstream-fork-upgrade-remediation/evidence/T-03.md</Path>` |

- **E2E disposition：** required；Lead 在 current workspace/CI 等价服务环境运行。
- **E2E owner/environment：** Lead / current-workspace，远程重放环境为 parent direct branch CI。
- **集成出口：** CI 远程运行只能在后续 commit/push 后发生，本轮本地验证并记录该限制。

## 9. 发布、迁移与恢复

- **迁移顺序：** 先确保 Compose/config 稳定，再初始化独立 fresh 数据目录，最后运行本地消费者验收。
- **回滚或前向恢复：** 停止新 middleware 并恢复 Compose；保留独立数据目录，不使用 `down -v`，旧 `cde-infra` 不参与回滚。
- **不可逆操作与批准点：** required check 设置不在本轮范围。

## 10. 验收标准

- [x] AC-003 的本地与远程 CI 部分全部通过。
- [x] 真实浏览器验收发现的 OSS Complete 响应和已完成票据恢复回归通过。
- [x] 外部服务集成测试五项实际执行，未通过 Assumption 静默跳过。
- [x] Evidence 区分 executed、skipped、not-run。
- [x] 根目录两选项启动脚本的前后端真实启动、实时日志和中断行为通过。
