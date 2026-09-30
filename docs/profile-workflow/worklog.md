# 跨域、个人中心与跨客户端审核

## Goal

落实已确认方案：CORS 精确与通配配置贯通；密码初始化备注准确；Admin/Home 登录布局、组件、图标一致；个人中心认证状态闭环；通用节点 Client 审核并接入个人、企业认证。

## Current status

实现与本轮验证完成。保留任务开始前的工作树改动，未提交、未部署。

## Decisions

- 每个审核节点一个 Client，角色或直接指定用户都遵守该 Client；申请节点固定发起 Client。
- 使用 node.ext 配置和实例节点 Client 快照表，不改第三方流程表。
- 任一级不通过退回申请人，修改后从头审核。
- Home 登录后共享 Admin 壳层展示，路由和会话仍由各 App 拥有。
- 新摘要与任务审核 API 保持本人/任务范围，管理覆盖决定保留独立合同。

## Files changed

- Common Web、release manifest/环境示例：精确 Origin、IP/域名通配及 `*` 混合配置。
- `50-cde-base-dml.sql`：准确密码策略备注、Home 非默认审核角色/菜单、Admin 任务审核权限、认证申请人节点和审核节点 Client。
- `wta-api`、System、Workflow：按 Client 配置选人目录、实例节点范围快照、任务/历史校验及跨端办理。
- Profile person/enterprise：本人摘要、有效认证回显、固定提交快照任务审核及材料读取端口。
- 前端共享 UI 壳、图标、两 App 布局、个人中心与认证/审核页面、Home 待办/已办组合。
- Skill 事实、Profile 构建导读、本文档和相关发布/后端/前端验证。

## Remaining work

本轮授权的代码、测试与本地构建无剩余项。部署及存量实例 Client 回填属于后续按环境执行的升级工作，见 README。

## Verification

已通过（退出码均 0）：

- `backend/`：Common Web CORS 定向 13 项；Profile 本人摘要/任务审核/材料/网关定向 43 项，包含独立 MySQL 真实 HTTP→UseCase→Mapper/XML 链，外部 Workflow/材料端口为显式替身。日志 `/tmp/wta-cors-java-tests.log`、`/tmp/profile-task-review-tests.log`。
- `frontend/`：`pnpm lint`、`pnpm typecheck`、`pnpm test`、`pnpm build:prod` 全工作区通过，测试含 699 项 Vitest 与 108 项 Node 测试，日志 `/tmp/wta-profile-frontend-{lint,typecheck,test,build}.log`。
- `frontend/`：`UI_PLAYWRIGHT_CONFIG=playwright.profile-ui.config.ts A11Y_EVIDENCE_DIR=/tmp/profile-ui-evidence node e2e/run-public-accessibility.mjs`，真实 Chrome 10 用例全部通过；使用最终产物与模拟 API。截图 `/tmp/profile-ui-evidence/run-Knjbcx`、日志 `/tmp/profile-ui-browser-final.log`，已人工查看桌面/移动端与关闭动画后的认证状态标签。
- 真实 Workflow 引擎与 System 定向 25 项全部通过，日志 `/tmp/workflow-cross-client-test.log`：MySQL/Redis/WarmFlow 实际执行 A→B→C→退回A→B→C→D；测试显式替换用户目录/业务事件下游，不把分段验证称为生产环境整链验收。
- OpenAPI 增量来自 `ProfileReviewOpenApiContractTest` 实际 Java Controller/模型导出，与原 HTTP 快照合并后通过 fetch/generate/check；当前 457 路径、474 模型，含 59 个 Profile 路径，本轮新增 8 条。provenance 如实记录反射增量来源；不是本轮启动完整服务抓取 `/v3/api-docs`。生成后再次 `pnpm typecheck` 通过，日志 `/tmp/wta-profile-frontend-contract-typecheck.log`。
- 仓库根：release 配置/基座合同 27 项通过，日志 `/tmp/wta-profile-final-release-tests.log`；CORS atomic-release 18 项及最终通配/旧 manifest 回归 3 项通过。
- 独立 `profile_workflow_baseline_test` 数据库完整导入业务基座 `10/20/30/40/50`：108 表、无断裂 Profile 菜单父节点、Home 默认角色审核权限为 0、审核员拥有 4 菜单、两认证流程各含 2 人工节点，所有人工节点具备 Client 配置。
- 两个 Profile 子域 layered 检查、Skill 事实检查和 AGENTS 导读检查通过。

后端全量复跑 `backend/./mvnw test`（附 `profile.openapi.output` 和本任务隔离库 `password.migration.mysql.integration.*` 参数）退出 0：295 个测试类、1,326 项，0 失败/错误、259 项按既有环境条件跳过。日志 `/tmp/wta-profile-backend-test-final.log`。其中密码迁移真实 MySQL、最后补充的第三方写入旁路边界 3 项、OpenAPI 导出均未跳过；本次相关的 Profile MySQL 与 Workflow 引擎另有上文未跳过的定向证据。其余第三方/外部环境集成没有在本轮逐项启用。

本次自建 MySQL/Redis 两个容器及其测试数据库已清理；对应匿名卷凭 Docker unmount 事件中的本任务容器 ID 确认归属后逐个删除（证据 `/tmp/wta-profile-owned-volume-events.json`）；未操作原有容器、数据库或部署。

后端两种组合均通过：

- `backend/./mvnw clean package -DskipTests`，退出 0，日志 `/tmp/wta-profile-backend-full-package.log`；`scripts/ci/verify-admin-bundle.sh full` 退出 0。
- `backend/./mvnw clean package -Pbundle-core -Dmaven.test.skip=true`，退出 0，日志 `/tmp/wta-profile-backend-core-package.log`；`scripts/ci/verify-admin-bundle.sh core` 退出 0。
- core 检查后将已验证的 full JAR 恢复到 `backend/wta-admin/target/wta-admin.jar`，再验证 full 组成通过；本地最终可运行 JAR 包含 Workflow。打包跳过测试由前述完整 Maven 测试承担。
- `git diff --check` 通过。第一轮全量检查发现密码 SQL 测试未按既有 END 标记截取，以及流程图三节点旧坐标断言。已限定密码迁移块的测试范围，并让四节点种子与图形坐标、连线断言一致，保留并增加任务权限/申请节点合同断言。
