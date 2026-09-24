---
schema_version: 3
plan_contract_version: 1
skill_scan: "2026-09-23已枚举.agents/skills全部入口；命中scope读取入口，common入口变化已复核，历史Skill Evidence不改写"
skill_bindings: [{"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/44-optional-oss-diagnostics.md</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/service/</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/readiness/</Path>"], "outputs": ["T-44的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "namewta-fullstack-development", "path": "<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>", "sha256": "675c053c11d8b22cd394c875f48688242d8e8328dd14e69657f5d5b12f2af68b", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/44-optional-oss-diagnostics.md</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/service/</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/readiness/</Path>"], "outputs": ["T-44的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "wta-module-guide", "path": "<Path>.agents/skills/wta-module-guide/SKILL.md</Path>", "sha256": "bb57a781314abe316f06ba9538f62043f1bd053904c0832969e575c09731a7a3", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/44-optional-oss-diagnostics.md</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/service/</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/readiness/</Path>"], "outputs": ["T-44的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "wta-common-modules-guide", "path": "<Path>.agents/skills/wta-common-modules-guide/SKILL.md</Path>", "sha256": "d7b7e105c37499e0e0f8ad9b1e2dbf379100df5b6a47e0d6affb04483f8b162c", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/44-optional-oss-diagnostics.md</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/service/</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/readiness/</Path>"], "outputs": ["T-44 OssFactory/RedisUtils复用、缓存与业务调用边界检查"], "required": true, "on_failure": "block-ticket"}, {"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "verify", "operation": "verify-affected-contract-and-quality-gates", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/44-optional-oss-diagnostics.md</Path>", "当前diff与验证矩阵"], "outputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-44-replan-2026-09-23.md</Path>"], "required": true, "on_failure": "block-ticket"}]
resource_claims: ["workspace:current-exclusive", "oss:readiness", "oss:access-route", "health:core", "contract:AC-044"]
artifact: "ticket"
change: "2026-09-14-wta-plus-comprehensive-review"
id: "T-44"
title: "上传下载与核心启动脱离 OSS 诊断前置条件"
status: "in_progress"
kind: "bug"
planning_depth: "deep"
planning_depth_reason: "公共合同/事务/安全/数据及恢复边界"
ready: true
risk: "high"
blocked_by: []
contract_ids: ["AC-044"]
owner: "single-agent"
expected_changes: ["<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/service/</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/readiness/</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/upload/</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/migration/</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/runner/SystemApplicationRunner.java</Path>", "<Path>backend/wta-admin/src/main/resources/application.yml</Path>", "<Path>backend/wta-modules/wta-system/AGENTS.md</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/oss/</Path>", "<Path>backend/README.md</Path>", "<Path>release-artifacts/docker/</Path>", "<Path>.agents/skills/wta-module-guide/references/modules/system/</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/service/impl/SysOssConfigServiceImpl.java</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/listener/OssConfigChangeListener.java</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/controller/system/SysOssConfigController.java</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/domain/vo/OssStorageDiagnosticVo.java</Path>", "<Path>backend/wta-admin/src/main/java/org/namewta/NamewtaApplication.java</Path>", "<Path>frontend/packages/api-contracts/openapi/current.json</Path>", "<Path>frontend/packages/api-contracts/openapi/revisions/</Path>", "<Path>frontend/packages/api-contracts/generated/openapi.ts</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/profile/material/ProfileSelfMaterialsBrowserIntegrationTest.java</Path>", "<Path>backend/wta-common/wta-common-json/src/main/java/org/namewta/common/json/utils/LogSanitizer.java</Path>", "<Path>backend/wta-common/wta-common-json/src/test/java/org/namewta/common/json/utils/LogSanitizerTest.java</Path>", "<Path>backend/wta-common/wta-common-mybatis/src/main/java/org/namewta/common/mybatis/interceptor/SqlLogInterceptor.java</Path>", "<Path>backend/wta-common/wta-common-mybatis/src/main/java/org/namewta/common/mybatis/config/properties/SqlLogProperties.java</Path>", "<Path>backend/wta-common/wta-common-web/src/test/java/org/namewta/common/web/logging/SysLogFilterTest.java</Path>", "<Path>backend/wta-admin/src/main/resources/application-dev.yml</Path>"]
writable_paths: ["<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/service/</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/readiness/</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/upload/</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/migration/</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/runner/SystemApplicationRunner.java</Path>", "<Path>backend/wta-admin/src/main/resources/application.yml</Path>", "<Path>backend/wta-modules/wta-system/AGENTS.md</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/oss/</Path>", "<Path>backend/README.md</Path>", "<Path>release-artifacts/docker/</Path>", "<Path>.agents/skills/wta-module-guide/references/modules/system/</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/service/impl/SysOssConfigServiceImpl.java</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/listener/OssConfigChangeListener.java</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/controller/system/SysOssConfigController.java</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/domain/vo/OssStorageDiagnosticVo.java</Path>", "<Path>backend/wta-admin/src/main/java/org/namewta/NamewtaApplication.java</Path>", "<Path>frontend/packages/api-contracts/openapi/current.json</Path>", "<Path>frontend/packages/api-contracts/openapi/revisions/</Path>", "<Path>frontend/packages/api-contracts/generated/openapi.ts</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/profile/material/ProfileSelfMaterialsBrowserIntegrationTest.java</Path>", "<Path>backend/wta-common/wta-common-json/src/main/java/org/namewta/common/json/utils/LogSanitizer.java</Path>", "<Path>backend/wta-common/wta-common-json/src/test/java/org/namewta/common/json/utils/LogSanitizerTest.java</Path>", "<Path>backend/wta-common/wta-common-mybatis/src/main/java/org/namewta/common/mybatis/interceptor/SqlLogInterceptor.java</Path>", "<Path>backend/wta-common/wta-common-mybatis/src/main/java/org/namewta/common/mybatis/config/properties/SqlLogProperties.java</Path>", "<Path>backend/wta-common/wta-common-web/src/test/java/org/namewta/common/web/logging/SysLogFilterTest.java</Path>", "<Path>backend/wta-admin/src/main/resources/application-dev.yml</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/source.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ADR.md</Path>", "<Path>{roots.state}/specdev/adr/</Path>", "<Path>{roots.state}/specdev/changes/2026-09-21-wta-sso-oidc-upgrade/</Path>"]
shared_paths: ["<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/service/</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/readiness/</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/upload/</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/migration/</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/runner/SystemApplicationRunner.java</Path>", "<Path>backend/wta-admin/src/main/resources/application.yml</Path>", "<Path>backend/wta-modules/wta-system/AGENTS.md</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/oss/</Path>", "<Path>backend/README.md</Path>", "<Path>release-artifacts/docker/</Path>", "<Path>.agents/skills/wta-module-guide/references/modules/system/</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/service/impl/SysOssConfigServiceImpl.java</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/listener/OssConfigChangeListener.java</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/controller/system/SysOssConfigController.java</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/domain/vo/OssStorageDiagnosticVo.java</Path>", "<Path>backend/wta-admin/src/main/java/org/namewta/NamewtaApplication.java</Path>", "<Path>frontend/packages/api-contracts/openapi/current.json</Path>", "<Path>frontend/packages/api-contracts/openapi/revisions/</Path>", "<Path>frontend/packages/api-contracts/generated/openapi.ts</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/profile/material/ProfileSelfMaterialsBrowserIntegrationTest.java</Path>", "<Path>backend/wta-common/wta-common-json/src/main/java/org/namewta/common/json/utils/LogSanitizer.java</Path>", "<Path>backend/wta-common/wta-common-json/src/test/java/org/namewta/common/json/utils/LogSanitizerTest.java</Path>", "<Path>backend/wta-common/wta-common-mybatis/src/main/java/org/namewta/common/mybatis/interceptor/SqlLogInterceptor.java</Path>", "<Path>backend/wta-common/wta-common-mybatis/src/main/java/org/namewta/common/mybatis/config/properties/SqlLogProperties.java</Path>", "<Path>backend/wta-common/wta-common-web/src/test/java/org/namewta/common/web/logging/SysLogFilterTest.java</Path>", "<Path>backend/wta-admin/src/main/resources/application-dev.yml</Path>"]
shared_path_owners: ["<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/service/</Path> => single-agent (Lead; exclusive current workspace; cors_audit sole delegated product writer for T-44)", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/readiness/</Path> => single-agent (Lead; exclusive current workspace; cors_audit sole delegated product writer for T-44)", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/upload/</Path> => single-agent (Lead; exclusive current workspace; cors_audit sole delegated product writer for T-44)", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/migration/</Path> => single-agent (Lead; exclusive current workspace; cors_audit sole delegated product writer for T-44)", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/runner/SystemApplicationRunner.java</Path> => single-agent (Lead; exclusive current workspace; cors_audit sole delegated product writer for T-44)", "<Path>backend/wta-admin/src/main/resources/application.yml</Path> => single-agent (Lead; exclusive current workspace; cors_audit sole delegated product writer for T-44)", "<Path>backend/wta-modules/wta-system/AGENTS.md</Path> => single-agent (Lead; exclusive current workspace; cors_audit sole delegated product writer for T-44)", "<Path>backend/wta-admin/src/test/java/org/namewta/test/oss/</Path> => single-agent (Lead; exclusive current workspace; cors_audit sole delegated product writer for T-44)", "<Path>backend/README.md</Path> => single-agent (Lead; exclusive current workspace; cors_audit sole delegated product writer for T-44)", "<Path>release-artifacts/docker/</Path> => single-agent (Lead; exclusive current workspace; cors_audit sole delegated product writer for T-44)", "<Path>.agents/skills/wta-module-guide/references/modules/system/</Path> => single-agent (Lead; exclusive current workspace; cors_audit sole delegated product writer for T-44)", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/service/impl/SysOssConfigServiceImpl.java</Path> => single-agent (Lead; exclusive current workspace; cors_audit sole delegated product writer for T-44)", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/listener/OssConfigChangeListener.java</Path> => single-agent (Lead; exclusive current workspace; cors_audit sole delegated product writer for T-44)", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/controller/system/SysOssConfigController.java</Path> => single-agent (Lead; exclusive current workspace; cors_audit sole delegated product writer for T-44)", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/domain/vo/OssStorageDiagnosticVo.java</Path> => single-agent (Lead; exclusive current workspace; cors_audit sole delegated product writer for T-44)", "<Path>backend/wta-admin/src/main/java/org/namewta/NamewtaApplication.java</Path> => single-agent (Lead; exclusive current workspace; cors_audit sole delegated product writer for T-44)", "<Path>frontend/packages/api-contracts/openapi/current.json</Path> => single-agent (Lead; exclusive current workspace; cors_audit sole delegated product writer for T-44)", "<Path>frontend/packages/api-contracts/openapi/revisions/</Path> => single-agent (Lead; exclusive current workspace; cors_audit sole delegated product writer for T-44)", "<Path>frontend/packages/api-contracts/generated/openapi.ts</Path> => single-agent (Lead; exclusive current workspace; cors_audit sole delegated product writer for T-44)", "<Path>backend/wta-admin/src/test/java/org/namewta/test/profile/material/ProfileSelfMaterialsBrowserIntegrationTest.java</Path> => single-agent (Lead; cors_audit sole delegated product writer for T-44)", "<Path>backend/wta-common/wta-common-json/src/main/java/org/namewta/common/json/utils/LogSanitizer.java</Path> => single-agent (Lead; cors_audit sole delegated product writer for T-44)", "<Path>backend/wta-common/wta-common-json/src/test/java/org/namewta/common/json/utils/LogSanitizerTest.java</Path> => single-agent (Lead; cors_audit sole delegated product writer for T-44)", "<Path>backend/wta-common/wta-common-mybatis/src/main/java/org/namewta/common/mybatis/interceptor/SqlLogInterceptor.java</Path> => single-agent (Lead; cors_audit sole delegated product writer for T-44)", "<Path>backend/wta-common/wta-common-mybatis/src/main/java/org/namewta/common/mybatis/config/properties/SqlLogProperties.java</Path> => single-agent (Lead; cors_audit sole delegated product writer for T-44)", "<Path>backend/wta-common/wta-common-web/src/test/java/org/namewta/common/web/logging/SysLogFilterTest.java</Path> => single-agent (Lead; cors_audit sole delegated product writer for T-44)", "<Path>backend/wta-admin/src/main/resources/application-dev.yml</Path> => single-agent (Lead; cors_audit sole delegated product writer for T-44)"]
---

# T-44：上传下载与核心启动脱离 OSS 诊断前置条件

Map：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>；Spec：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/spec.md</Path>；Goal：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/goal-plan.md</Path>。
唯一执行者先完整读Map→命中项目Skill入口/按scope引用→本票与上游。用户已激活Goal并授权本地实施/commit/direct-parent；current单writer串行、无新worktree。cors_audit为委派产品writer，Lead独占治理、提交、构建与真实服务验收，只读审查可并行。

## 1. 战略与来源

- 来源：R64-O-01；AC-044；本轮用户请求与<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/source.md</Path>。
- 当前事实：启动同步 refresh；下载/直传调用 readinessRegistry.requireServing，缺canary/过期快照可阻断合法对象。System AGENTS 硬规则目前也要求该门禁。
- 可观察产出：业务仅校验当前对象/配置/权限/预期访问类型，远端操作按实际结果反馈；管理员诊断独立，坏的可选存储不阻断核心就绪。

## 2. 决策状态

### 已锁定决策

保留工程分层、Client/权限、资源owner、安全日志、真实供应商协议和唯一六SQL基座。用户当前Goal授权实施与本地提交；原规划授权历史不覆盖当前授权。

### 已确认方案

2026-09-23用户已逐项接受D-002—009并确认整体共识（LOG-010—018）；本票按已接受ADR定稿。 完整决定及来源以<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ADR.md</Path>为准。

### 已采用的低影响假设

沿用当前模块与测试基座；测试样本为隔离合成数据，不作为生产容量或SLO。

### 执行前置

G整体共识及Goal激活均已确认；基线8db922e1971b4781b2b53f8db837c06f7b60c4e7，main/current clean。先测试红灯，随后分段实现；验收前固定不可变源码并核隔离资源。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 本票可观察产出及所列消费者、验证、文档 | 既有wta-api/common、模块模式、事务/权限/生命周期与测试 | 无关模块重写、新队列/锁平台、真实数据修复、远程发布及相邻OIDC实现 |

## 4. 要构建什么

业务仅校验当前对象/配置/权限/预期访问类型，远端操作按实际结果反馈；管理员诊断独立，坏的可选存储不阻断核心就绪。 正常、失败、越权和竞争路径按本票验收断言共同交付，不把前后端或测试分成无价值空票。

## 5. 实现契约

- 入口与输入输出：OssStorageReadiness*、OssLifecycle*、OssUpload*；核心启动＋最小权限MinIO与health组。
- 外部行为：业务仅校验当前对象/配置/权限/预期访问类型，远端操作按实际结果反馈；管理员诊断独立，坏的可选存储不阻断核心就绪。
- 不变量：current workspace单writer；UseCase→Service→DAO→Mapper；classic保留；公开数据只经wta-api；GET查询/POST变更且安全@Log。Notify外部I/O不在结果事务内，IN_APP按确认后的短事务合同处理。
- 失败边界：不吞SQL/HTTP/Provider错误，不将UNKNOWN当成功或盲目可重试；页面旧响应不覆盖新会话。具体负向断言见第8/10节。
- 兼容：沿用用户此前明确的基座仓内直接切换决定，同步真实消费者/生成物；不放宽第三方协议。公共API技能用于调用方与影响核对，不重新增加已被用户排除的兼容桥。
- 安全：仅隔离合成测试；secret不进日志/截图/证据。对象/Client授权在后端实施，页面隐藏不替代权限。

## 6. 执行路线

1. 固定空存储、缺canary、旧快照、非默认坏配置及MinIO离线的启动/业务矩阵。
2. 显式按 ADR 替代 System AGENTS readiness硬门禁；本地权限/状态/PRIVATE预期校验保留失败关闭。
3. 移除 requireDownloadable/直传/迁移路由对诊断缓存依赖，按对象实际 service 读取权威配置，历史对象不随默认桶移动。
4. 去掉启动全配置同步远端诊断，诊断由管理操作触发；health核心组仅含真实核心依赖，可选诊断独立。
5. 更新canary配置说明和相关架构测试为新合同；不删除测试以规避失败。
6. 最小权限真实MinIO验证预签名/上传实际访问及失败分类，核心站内信/无附件邮件保持零存储调用。

## 7. 路径访问契约

frontmatter为预计点、硬写集与共享owner权威。目录写集仅授权本票行为所需文件；新增测试在声明根内，新增生产类须符合已有层次。不存在的新文件为计划创建，不声称已实现。共享物理/语义资源由single-agent在本票轮次独占；不同票不并行，跨change冲突仅暂停相关分支。

本票状态和Evidence仅由Lead写当前change；永久ADR/context及相邻SSO change只读。越界先修订Ticket/Map，禁止先改后报。

## 8. 验证矩阵

| 场景 | 接缝/步骤 | 预期 | Evidence |
|---|---|---|---|
| 正常 | OssStorageReadiness*、OssLifecycle*、OssUpload*；核心启动＋最小权限MinIO与health组 | 缺canary/诊断过期不阻断合法签名/上传；DB元数据仍零OSS调用 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-44-replan-2026-09-23.md</Path> |
| 失败/竞争 | 空/非默认坏存储不阻断核心启动，真实文件失败仍准确报错 | 明确失败/安全恢复，无伪成功、越权及部分提交 | 同上，记录故障注入与状态 |
| 回归 | 现有同域测试＋消费者＋适用静态门禁 | 无权用户/错误访问类型仍拒绝；启动不遍历存储作远端巡检 | 同上，记录测试数/skip/源码 |

命令在仓根执行，`cd backend`表示该条命令切cwd；每条独立运行。以下为实施期命令，本轮未执行：

- `cd backend && ./mvnw -pl wta-modules/wta-system,wta-admin -am test`

- Workspace checks：current-workspace，所列命令加命中工程Skill质量门禁。
- E2E disposition：required: OssStorageReadiness*、OssLifecycle*、OssUpload*；核心启动＋最小权限MinIO与health组。
- E2E owner/environment：single-agent（Lead）/current-workspace；真实MySQL/Redis/MinIO必须为本任务隔离资源，必要服务缺失则阻塞对应验收。
- 真实服务启用方法与零skip要求：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/verification.md</Path>；新增用例必须保存精确选择器与实际计数，不能只运行mock或test list。
- Integration evidence：非空implementation commit、parent before、clean exact HEAD/tree时点的direct-parent和适用E2E、不可变result及父链；required模式不适用，不创建candidate worktree。

## 9. 发布、迁移与恢复

- 迁移顺序：先修并验证本地访问校验，再去诊断门禁；部署前核对实际探针组，不能将本地配置当生产探针事实。
- 兼容窗口：基座仓内同步切换，无未声明双写/双协议；外部现有协议保持。
- 监控：记录本票可观察失败/状态/耗时及资源数量，不记录敏感正文；不新增监控平台。
- 恢复：保存上个不可变候选及失败证据；停止受影响任务再核对外部副作用。不得通过恢复已披露secret、放宽权限或重发UNKNOWN恢复。
- 不可逆批准：产品commit/父分支更新及远程push、部署、轮换/真实数据操作、归档分别核对本轮授权。本地产品commit/direct-parent已授权；push、部署、生产修复与归档未授权。
- 收缩条件：旧消费者/废弃字段/不必要配置引用清零且新合同验证通过；不适用的删除不人为增加。

## 10. 验收标准

- [ ] `AC-044`：缺canary/诊断过期不阻断合法签名/上传；DB元数据仍零OSS调用。
- [ ] `AC-044`：空/非默认坏存储不阻断核心启动，真实文件失败仍准确报错。
- [ ] `AC-044`：无权用户/错误访问类型仍拒绝；启动不遍历存储作远端巡检。
- [ ] 实际调用已绑定Skill，记录摘要/输入/步骤/输出；不是只“读过”。
- [ ] 正常、失败、回归和required E2E有当前候选证据，未运行不勾选。
- [ ] 写集、共享owner、合同和生成物一致；无未批准偏差。
- [ ] 真实commit/direct-parent/result出口已满足或按Goal对历史无需新实施票作有证据的取消裁决。

## 11. SKILL 调用计划

frontmatter每个必需绑定在implement阶段输入本票、真实调用方和diff，按scope执行约束检查与实现；verify阶段由engineering-standards执行适用门禁。实际Skill Execution Records写<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-44-replan-2026-09-23.md</Path>，包含id/phase/operation/sha256/status/evidence。入口摘要变化先读diff并重新绑定，不改旧历史记录。当前规划仅完成元数据/入口及相关规范路由，未伪造实施passed。

## 12. 停止、检查点与交付

交付本票完整可观察行为，数量以Map为准，不能以样例替代。缺高影响决定、必需Skill/引用/测试，或owner冲突，停止该票和依赖闭包；无依赖票仅在已获执行授权后继续。保留HEAD、diff、已跑命令、失败类别、待完成动作；相同失败无新证据或达到3次集成尝试先复盘。验收后回交Goal，全部票done仍不等于change可归档。

## revision199 — T-44启动与精确写集

revision199：T44已激活，19条写集及5项Skill绑定已登记；先补诊断阻断的红灯测试，再实施业务解耦、容错启动、单配置诊断和健康组分离。14done/2cancelled/T44 in_progress/33ready；Goal active，未完成或归档change。

基线 8db922e1971b4781b2b53f8db837c06f7b60c4e7。用户已批准替代System AGENTS旧规则“readiness未达到可服务状态时不得签发访问URL”；新规则保留业务owner/Client、ACTIVE、对象自身service、当前配置和预期访问类型失败关闭。新增单配置POST `/resource/oss/config/diagnose/{ossConfigId}` 使用既有 `system:ossConfig:list`，安全@Log关闭请求/响应记录，输出VO仅status/reason/checkedAt。OpenAPI由真实full JAR捕获后正式生成，revisions只新增不可变source/provenance；不手改生成物。

启动只初始化DB配置，不做远端诊断；无/重复/坏默认清理陈旧Redis默认指针，管理唯一PRIVATE默认约束保留。可选诊断Duration错误不能阻断核心启动；管理员调用有有限超时。无条件调度移至NamewtaApplication，真实Notify Redis wake丢失后的定时兜底必验。Docker现有TCP8080探针不能冒称HTTP readiness。T45供应商策略解释/T46清理锁/T49配置去重均留给各责任票。

## revision200 — 红灯与Dispatch01B

revision200：T44定向红灯已证实，固定11960110的53例为49pass/1failure/3error/0skip；三条诊断前置阻断及空service仍进入Provider的缺陷均可重现。Dispatch01B开始产品实现，完整候选attempts0；14done/2cancelled/T44 in_progress/33ready，Goal active。

`red01` Maven exit1、编译成功，源码前后clean同值。生命周期/直传/迁移各因旧readiness门禁抛错；空service负例在objectStore.accessPolicy被调用后失败，确认须补本地路由校验。其余49项通过；未把预期红灯算成验收或完整候选失败。XML/命令/源码/两份独立审查已保存red01/manifest.json。

01B在原19条写集内完成：业务诊断解耦而授权/ACTIVE/service/policy不放松；DB成功读取后清SYS_OSS_CONFIG专用缓存和默认指针，再填合法当前行，DB故障仍核心报错；单配置管理员诊断、配置变更只失效、无启动远端探测、常驻应用调度、core和ossdiagnostics健康组及规范同步。可选诊断timeout冻结为每网络步骤100ms–3s，最多5个顺序步骤，网络等待预算最多15s，不称整个请求3s；配置无效返回固定诊断配置错误，核心仍启动。不得起未回收后台任务制造表面超时。若需严格单个总deadline或common路径先回Lead登记，T45策略解释未提前改动。

## revision201 — 原始诊断属性的测试消费者同步

为避免Spring Binder在可选Duration词法错误时阻断核心，三项诊断配置改同型String JavaBean并在诊断时解析。仓内唯一超出现有admin OSS测试根的消费者为 `backend/wta-admin/src/test/java/org/namewta/test/profile/material/ProfileSelfMaterialsBrowserIntegrationTest.java`，已事前登记为第20条精确写集，仅同步setMaxSnapshotAge的配置字面值/必要编译消费，不改Profile业务。writer尚未改此文件；红灯与AC/候选attempts不变。

## revision202 — A1真实验收与日志缺陷补修

revision202：T44 A1源码e11c1b6f定向148全过，默认1115中899pass/216环境skip，full包通过；七组核心启动/Notify轮询真实场景通过。MinIO功能链已通过但日志检测发现access key与secret key，A1安全验收失败，整票仍in_progress。26条精确写集登记后补日志红灯与修复；14done/2cancelled/1in_progress/33ready，Goal active。

源码e12df121为01B产品实现；green01在编译期发现新增测试4处泛型断言歧义，保留exit1/零执行证据，e11c1b6f修复。green02为33类148例零skip；默认261类1115例899pass/216环境skip、零失败；full JAR SHA256 `835b4de84b911ff77ba20ba003218f30af015559dccfadcdbc30fdf1d8d069a8`，源码tree `92781356146df461ab9815a13aa2884effc0740e`。这些不是环境跳过项的通过证明。

七组真实核心验收均绑定同一clean源码/JAR：empty `de1c3f734c322a79`、bad-nondefault `7704e6179563c466`、duplicate-default `003f22307c765c71`、bad-default `37e0ba0f8a622025`、invalid-diagnostic `e3e150d8206beeb2`、minio-offline `c8511c8ff56072a3`、Notify fallback `637717597daa1c8a`。前六验证核心UP、真实登录/菜单及适用的缓存/诊断失败关闭；counted端点均零调用，offline为不可连接回环端口、未伪造计数。最后一项用Redis ACL selector仅拒绝notify:outbox:wake的PUBLISH并允许缓存失效通道，真实公告发布形成READY，唤醒失败后POLL领取，Outbox DONE/Delivery DELIVERED/Attempt/Message/用户关系均1，后续两次tick不重复；无外部SMS/MAIL调用，所有资源cleanup零错误。v2全局禁PUBLISH破坏缓存登录和v3忽略channel型ACL拒绝的夹具失败均保留，不能当成产品兜底失败或通过。

MinIO驱动v1/v2因空hex SQL失败，v4到诊断审计因SQL排序规则失败；均保留并在v5前复盘。v5/v6完整走通最小权限A/B上传下载、公开无canary GET、权限/状态/坏配置负例、单桶诊断、3个非法ID零远端、诊断STALE后下载、默认B与旧A、实际OpenAPI436路径；但postcheck未通过。v6 `4ca003409fc458ce` 的安全摘要记录app access key 7次、app secret key 1次，其余8类0；不保留原值/上下文。前置logger归属可能沿用此前HTTP行，不能据此断定SQL stderr也是HTTP logger。源码证实HTTP JSON字符串url未检查签名query，且dev启用SqlLogInterceptor，后者插值实际配置参数并打印原异常消息。A1作为一次真实安全验收失败记录，整票AC未勾；已捕获OpenAPI仅属该源码事实，不是完整通过。所有MinIO运行cleanup零错误。

事前新增6个精确路径：common-json的LogSanitizer及其测试、common-mybatis的SqlLogInterceptor/SqlLogProperties、common-web的SysLogFilterTest，以及application-dev.yml的SQL日志说明。加上原20条共26条；不改数据库、业务返回、权限、URL有效性或执行SQL，不新增公共API/依赖。共用日志副本须隐藏带签名/凭据的URL与上传令牌路径，同时保留普通公开URL；SQL保留结构、mapper、耗时和异常类型，禁止写入绑定值、原错误消息或秘密。先用真实日志入口补红灯，再实现；既有敏感HTTP和操作日志合同保持。实现细节不得通过关闭整套日志或绕过安全扫描过门禁。

证据见candidate-a1/manifest.json；仅精确测试JWT模式脱敏，保留源/留存hash及替换数，失败/skip不改写。真实运行私有raw日志不纳入仓库。OpenAPI正式生成、前端/core/静态及修复后真实验收仍待完成，不归档。

## revision203 — 日志红灯与Dispatch02B

revision203：T44日志回归红灯固定于d4a15669，6例/6失败/零error与skip；A1安全失败仍保留。02B按签名URL、uploadToken和SQL固定元数据合同补修。14done/2cancelled/T44 in_progress/33ready，完整候选attempts仍1，Goal active。

日志红灯见 evidence/T-44-current-2026-09-23/logging-red01/manifest.json；这是缺陷复现，不计为新增完整候选失败，也不勾选AC。

SQL补充源码事实：BoundSql.getSql已经展开动态片段（含现有Mapper的${ew.customSqlSegment}），即便不插值绑定参数，也可能含字面量或注释中的secret。因此revision202的“SQL结构”具体落实为SqlCommandType固定枚举、经字符/长度限制的Mapper ID、耗时和异常类名；不输出原SQL文本、参数、异常message/stack。不新增SQL解析器或依赖，不关闭日志。原Statement执行、结果及异常传播保持，console/log两种配置均适用。

HTTP日志只改副本：所有嵌套/数组文本值中的签名或凭据URL隐藏；uploadToken属性、OSS上传路由令牌及签名查询字段隐藏；普通公开URL和业务code维持现有合同。真实响应和路由不改，操作日志既有服务端路由模板继续有效。写集仍26条，无公共API/数据库变更。

Dispatch02B：cors_audit为唯一产品writer，可在已登记LogSanitizer、SqlLogInterceptor、SqlLogProperties、application-dev.yml的SQL注释以及02A测试路径落实修复；不运行Maven/服务/提交，不改治理。Lead审查并冻结源码后执行定向绿灯、default/full、真实MinIO与Notify兜底；实际OpenAPI正式生成、前端/core/静态门禁仍待完成。
