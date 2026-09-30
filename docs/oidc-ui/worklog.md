# OIDC 与统一 UI 工作记录

## Goal

实现已确认的最简第三方 OIDC 接入及 Admin/Home/SSO 统一 UI。正常 WTA 账户为权威身份；管理员逐字段开放账户、个人与企业档案资料，完整证件须显式授权。保留第一方 SSO。

## Current status

- 2026-09-30：完成参考仓库 `8cdc1cb`、本仓身份/档案/UI 审计，参考仓 47 项定向测试通过。
- 实现及验收完成：共享 UI、OIDC 后端/管理前端、账户/档案公开接口、SQL、部署配置和生成合同均已落地。
- 正式初始化流程在隔离 MySQL 创建 107 表通过；真实 MySQL/Redis/MinIO、后端和 HTTPS 网关已在验收后清理，原 38888 环境未改动。
- 实测修复了 Spring GET 恢复查询参数、退出表单 Origin/CSP 兼容问题；独立复审补齐管理策略变更与授权持久化的串行控制、关闭 SSO 的兼容装配、有界请求正文和过期清理。
- 旧中央会话缺认证时间时保留原 SSO 使用能力，进入 OIDC 明确要求重新登录；框架过滤链测试验证不会在登录页与授权端点之间循环。

## Decisions

- 仅机密 Web 客户端；独立 OIDC 应用目录；code flow、PKCE 默认开启、Basic 默认认证。
- 创建必填名称和精确回调；Secret 仅一次显示并保存摘要；基本 RP logout，不包含刷新、Broker 或退出通知。
- 信息字段归应用策略，授权快照与当前配置取交集；资料从当前有效账户/档案读取。
- 个人和法人完整证件可显式勾选，默认关闭；不开放材料、草稿、历史与内部权限。
- 持久 RSA 文件密钥、固定 HTTPS Issuer，opaque access token 与业务 Sa-Token 分离。
- 共用 Admin 蓝白主题；品牌蓝与可访问的实底操作蓝分离；共享包不拥有 App 路由和业务状态。

## Files changed

- 后端：`wta-oidc`；`wta-api` 的账户/会话/档案合同；Admin 账户适配器；Person/Enterprise 白名单查询；SSO 固定认证时间；common-web 精确协议 SPI。
- 前端：`domains/oidc`、`web-domains/oidc`、`web-kit/ui-element`；三个 App 的共享主题与认证页；Admin 菜单组合；SSO 授权恢复；正式 OpenAPI 快照和生成类型。
- 发布：既有 SQL 基线新增三表与六项权限；SSO Nginx；容器 OIDC 配置/密钥挂载；full/core 产物合同；密钥初始化器和验收脚本；相关 Skill 事实同步。
- 已有 `speculo/.speculo/learning/**` 修改属于任务前工作树，保持原状。

## Remaining work

本次实现与隔离验收没有剩余项。实际环境启用时按 [README](README.md) 配置固定 Issuer、持久密钥和数据库升级；本次未执行生产部署。

## Verification

- 前端目录执行 `pnpm lint`、`pnpm typecheck`、`pnpm test`、`pnpm architecture:test` 均 exit 0；完整工作区 Vitest 686 项，架构 101 项。
- `pnpm build:dev`、`pnpm build:prod` exit 0；最终生成合同及 mapper 收尾后又执行完整 typecheck、Admin 生产构建和 OpenAPI check，均 exit 0。
- `bash release-artifacts/scripts/verify-release.sh` exit 0，130 项；首轮因 fake bundle fixture 缺新模块失败，补 fixture 后全量重跑通过。
- `node scripts/ci/verify-agent-handbooks.mjs`、Skill facts、密钥初始化测试 exit 0。
- 公共 UI 浏览器 99 项通过；检查浅色/暗色、宽窄屏、缩放、键盘操作、控件对比度与独立退出卡片。最后主题文字色调整后相关 88 项复跑通过，不能将复跑重复累计为新增覆盖。
- 真实 Admin OIDC 管理浏览器 8 组通过：创建、字段批量/敏感单选、明暗宽窄屏、一次性密钥关闭与重开、编辑、停启用、重置、删除；没有 console/page error，自有应用已清理。
- 最终 full 产物上的两个独立 HTTPS RP 共 19 组通过：Basic/Post 两种客户端认证、code/S256/RS256/UserInfo、稳定 sub 与会话复用、非法回调、错误 verifier、重放及撤销、跨客户端/业务 token 隔离、策略交集、停用、RP logout、prompt none/login、无角色/登录域正常账户、个人/企业脱敏和完整证件发布、密钥轮换、旧 Admin/Home SSO 登录及客户端隔离。
- SSO/Person/Enterprise 模块 219 项无失败，普通轮有 2 个依赖外部 MySQL 的跳过；另起自有 MySQL 定向 14 项通过且 0 跳过。新增账户边界 2 项通过。
- OIDC 模块当前 26 个测试报告通过，含 6 个真实 MySQL 测试；最后新增旧会话回归所在完整 Spring 授权链 5 项通过。公共协议隐私 1 项、common-web 完整 56 项、common-json 12 项、common-core 1 项通过。layered 检查覆盖新模块 56 个 Java 文件。
- 最终 backend `./mvnw -Pdev,bundle-core -pl wta-admin -am package -Dmaven.test.skip=true`、`./mvnw -Pprod,bundle-full -pl wta-admin -am package -DskipTests` 与根目录 `bash scripts/ci/verify-admin-bundle.sh core`、`bash scripts/ci/verify-admin-bundle.sh full` 全部 exit 0。包矩阵不冒充测试；前述测试独立执行。初次 core 用 `-DskipTests` 会编译引用被 core 排除模块的既有测试而失败，随后使用仓库正式 CI 的 `-Dmaven.test.skip=true` 通过，未改测试或依赖来掩盖失败。
- SSO HTTP/TLS 模板以 nginx:1.31.1 真实容器分别运行 `nginx -t`，均 exit 0；容器自动删除。
- OpenAPI 最终从自有后端 `18888/v3/api-docs` 获取并由正式生成器生成/校验：449 paths、464 schemas。provenance 的 commit 为工作树基线 `4c9697abdb2366a76468681cccd51050d8cdc83c`，实际 runtime 包含本次未提交实现；不能将其误称为该 Git commit 的纯净构建。

### 重跑入口与证据

```bash
# 配置文件只指向本次隔离数据库，禁止指向业务库。
./backend/mvnw -f backend/pom.xml -pl wta-modules/wta-oidc -am \
  '-Dtest=Oidc*Test' "-Doidc.test.config=${OIDC_TEST_CONFIG}" \
  -Dsurefire.failIfNoSpecifiedTests=false test -q
./backend/mvnw -f backend/pom.xml -pl wta-common/wta-common-web -am test -q

# HTTPS 网关/浏览器脚本要求已准备独立后端、生产 dist 和私有验收配置目录。
OIDC_ACCEPTANCE_DIR=/tmp/namewta-oidc-real-example node frontend/e2e/oidc-owned-gateway.mjs
OIDC_ACCEPTANCE_DIR=/tmp/namewta-oidc-real-example node frontend/e2e/run-oidc-acceptance.mjs
OIDC_ACCEPTANCE_DIR=/tmp/namewta-oidc-real-example node frontend/e2e/run-oidc-admin-acceptance.mjs
```

`OIDC_TEST_CONFIG` 读取配置文件的 `spring.datasource.dynamic.datasource.master`，不在命令中传数据库密码。真实验收目录为 `/tmp/namewta-oidc-real-tvskvnnn`；`oidc-acceptance-results.json`、`admin-ui-evidence/verification.json`、`matrix-final-results.json`、`cleanup-results.json` 为本轮结果。敏感配置仍为 0600；数据库及运行进程已清理。Maven 结果位于受影响模块 `target/surefire-reports/`；根流程日志使用 `/tmp/namewta-oidc-*.log`。

没有提交、推送、生产部署或修改既有数据库。没有替实际第三方产品执行接入，互操作验收使用本次自有的两个机密 RP。
