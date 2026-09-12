---
schema_version: 3
artifact: ticket
change: 2026-08-21-oss-direct-unified-notification
id: T-22
title: 建立 Business OSS Owner 清单与架构棘轮
status: done
planning_depth: deep
planning_depth_reason: 全仓 owner 门禁、公共 API contract 收缩与 TEMP 清理发布前验证
ready: true
risk: high
blocked_by: [T-20, T-21]
contract_ids: [AC-008, AC-009, AC-010, AC-011]
owner: codex
expected_changes: ["<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/resources/oss/business-oss-owners.json</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/owner/BusinessOssOwnerArchitectureUnitTest.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/owner/BusinessOssFreshBaselineUnitTest.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/lifecycle/OssLifecycleContractUnitTest.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/system/api/OssService.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysOssServiceImpl.java</Path>"]
writable_paths: ["<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/resources/oss/business-oss-owners.json</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/owner/BusinessOssOwnerArchitectureUnitTest.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/owner/BusinessOssFreshBaselineUnitTest.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/lifecycle/OssLifecycleContractUnitTest.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/system/api/OssService.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysOssServiceImpl.java</Path>"]
read_only_paths: ["<Path>ruoyi-vue-plus-namewta/ruoyi-modules/**/src/main/java/**</Path>", "<Path>ruoyi-vue-plus-namewta/script/sql/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/application.yml</Path>", "<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/ADR.md</Path>"]
shared_paths: ["<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/system/api/OssService.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysOssServiceImpl.java</Path>"]
shared_path_owners: ["<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/system/api/OssService.java</Path> => T-22 contract owner after T-19/T-20/T-21", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysOssServiceImpl.java</Path> => T-22 contract owner after T-19/T-20/T-21"]
---

# Ticket T-22: 建立 Business OSS Owner 清单与架构棘轮

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/ticket/22-business-oss-owner-ratchet.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-22.md</Path>`

## 1. 战略与来源

- **目标：** 把 ADR-010 的显式 Owner ownership 变成可执行交付门禁，并在所有业务迁移后收缩绕过集合协调的公共 API。
- **可观察产出：** 全仓存在机器可读 Owner 清单；新增持久化 OSS carrier 或协调调用未登记时测试失败；公共 OssService 不再暴露原始 bind/unbind；fresh baseline 生命周期门禁通过。
- **来源：** `AC-008`、`AC-009`、`AC-010`、`AC-011`、`ADR-005`、`ADR-010`、`LOG-063`、`LOG-064`、`USER-DECISION`。
- **当前事实：** 架构共识已要求显式清单和聚焦测试，但仓库没有 manifest 或未来变化 ratchet，原始公共 bind/unbind 允许后续业务绕过集合协调。
- **Planning Depth 原因：** 建立跨模块静态门禁并 contract 公共 API，同时承担 TEMP 清理启用前的 fresh baseline 核验。

## 2. 决策状态

### 已锁定决策

- manifest 固定为测试资源 JSON，不进入生产 classpath 的运行时注册、注解扫描或动态回调流程。
- 每个条目至少包含 module、物理表、持久化 carrier/编码、真实主键、Owner 服务、insert/update/delete/restore 策略和合同测试路径。
- 首个基线登记四类 Owner：`sys_user.avatar`、`sys_notice.notice_content`、`sys_notify_log.attachment_oss_ids`、`flow_his_task.ext`。
- 架构测试组合两类 ratchet：扫描持久化实体/schema 的 OSS/附件候选 carrier 是否被登记；扫描 `reconcileReferences` 生产调用方是否能映射到 manifest Owner。明确 allowlist 必须带原因，不能静默排除。
- T-20/T-21 证明旧调用点归零后，从公共 `OssService` 及其实现删除 `bind/unbind`；生命周期内部低层方法可以继续由协调器复用。
- fresh baseline 测试证明已引用对象不会成为清理候选、无引用 TEMP 对象会成为 dry-run 候选；配置继续保持 `cleanup-enabled:false`、`cleanup-dry-run:true`。

### 已采用的低影响假设

- 使用 ruoyi-admin 既有测试依赖解析 JSON 和扫描源码，不新增生产依赖或构建插件。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| Owner JSON、架构/基线测试、公共 bind/unbind contract、cleanup 配置断言 | T-19 协调 API、T-20/T-21 Owner 合同、现有生命周期/清理测试工具 | 运行时注册中心、注解扫描、动态业务回调、历史回填、启用 cleanup、产品 schema |

## 4. 要构建什么

开发者新增或修改持久化 OSS carrier 时，必须在 manifest 中声明所属业务 module、真实引用身份、全生命周期策略和测试证据。架构测试在合并前将生产调用点及持久化候选与清单双向核对，遗漏或陈旧条目都会失败。完成当前 Owner 迁移后，业务只能通过集合协调契约维护引用；fresh baseline 测试同时证明引用保护与 dry-run 候选边界，但不会实际开启清理。

## 5. 实现契约

- **入口或接缝：** ruoyi-admin 测试资源与 JUnit 门禁；T-22 contract 后的 OssService。
- **输入与输出：** manifest JSON 输入，测试输出明确到未登记表/字段/调用类或无证据条目；公共 API 只保留 `reconcileReferences`。
- **公共接口变化：** 删除 `OssService.bind/unbind` 及 SysOssServiceImpl 对应公开实现。
- **不变量：** 清单是设计/交付门禁而非运行时配置；Owner 仍在业务 module；无动态查表或 ACL 推断。
- **状态或数据流：** parse/validate manifest -> scan candidates/callers -> cross-check evidence -> run fresh lifecycle assertions；生产调用始终 business owner -> reconcile -> lifecycle internals。
- **错误与失败行为：** manifest 缺字段、重复 owner、无测试路径、未知 carrier、未登记调用方或遗留公共 bind/unbind 时测试失败并指出具体位置。
- **兼容要求：** 不提供旧 API 兼容或历史数据迁移；基座系统直接收缩到目标公共接口。
- **安全与隐私要求：** manifest 不保存 OSS 内容、Secret 或 ACL；引用元数据仍不能作为访问授权。

## 6. 执行路线

1. 创建四类 Owner 的 manifest，并以 schema-like 测试固定必填字段、唯一性和证据路径存在性。
2. 建立持久化 carrier 候选与 reconcile 调用方双向扫描，加入有理由的基础设施/非持久化 allowlist。
3. 验证 T-20/T-21 后全仓公共 bind/unbind 调用为零，再从 OssService/SysOssServiceImpl contract 旧方法。
4. 建立 fresh baseline 测试，覆盖引用保护、最后解绑 TEMP 和 dry-run 候选，同时断言清理仍 disabled/dry-run。
5. 运行架构测试、全仓调用扫描、ruoyi-admin opt-in tests 和 reactor package，记录 ratchet Evidence。

## 7. 路径访问契约

- **预计修改点：** 两份 Owner 门禁测试、一份 JSON manifest、OssService 与 SysOssServiceImpl contract。
- **可写范围：** 仅 frontmatter 指定文件；发现 Owner 行为缺口必须退回 T-20/T-21，不在本 Ticket 越界修复业务代码。
- **只读上下文：** 全模块生产源码/schema、cleanup 配置、生命周期测试和 ADR-010。
- **共享路径：** OssService 与 SysOssServiceImpl；只有 T-19 expand、T-20/T-21 migrate 完成后 T-22 才拥有 contract 写权限。
- **保留或不动：** application.yml 清理值、所有产品 schema、业务 Owner 实现、Provider 对象。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | Owner 架构门禁 | `./mvnw -pl ruoyi-admin -am -Dmaven.test.skip=false -Dtest='BusinessOssOwnerArchitectureUnitTest,BusinessOssFreshBaselineUnitTest' -Dsurefire.failIfNoSpecifiedTests=false test` | 四类 Owner、调用方、证据与 fresh baseline 全部匹配 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-22.md</Path>` |
| 失败路径 | ratchet fixture/负例 | 测试中构造缺 manifest、重复条目、未知调用方和配置误启用案例 | 每类遗漏均 fail-closed 且报告具体 owner/caller | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-22.md</Path>` |
| 回归 | 全仓扫描与后端门禁 | 扫描公共 `bind/unbind` 和 cleanup 配置，再执行 opt-in test/package | 无业务绕过；清理 disabled/dry-run；后端门禁通过 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-22.md</Path>` |

- **Workspace checks：** 按新 Goal Plan 执行定向测试、`./mvnw -Dmaven.test.skip=false test` 和 `./mvnw -DskipTests package`。
- **E2E disposition：** not-required：这是静态架构、公共 Java API 和生命周期基线门禁，无 UI/外部协议；用户已确认不建设 E2E。
- **E2E owner/environment：** Lead / current-workspace。
- **Integration evidence：** 记录 implementation commit、parent before/result SHA、manifest/scan 输出、清理配置断言和父分支包含关系。

## 9. 发布、迁移与恢复

- **迁移顺序：** T-19 expand -> T-20/T-21 migrate -> 本 Ticket manifest/verify/contract。
- **兼容窗口：** 不适用：无历史负担，公共旧方法在同一代码基线直接删除。
- **监控信号：** CI/本地架构测试、调用扫描、fresh baseline lifecycle assertions；不新增运行时扫描或指标。
- **回滚或前向恢复：** contract 前保留测试证明调用点为零；若遗漏则恢复编译并前向登记/迁移 Owner，不引入兼容适配层。
- **不可逆操作与批准点：** 本 Ticket 禁止启用或执行 TEMP 主动清理；未来启用仍需独立发布批准。
- **收缩条件：** T-20/T-21 done、公共 bind/unbind 调用为零、manifest 与 fresh baseline 门禁全部通过。

## 10. 验收标准

- [x] `AC-008`：fresh baseline 中无引用 TEMP 对象只作为 dry-run 候选，清理仍 disabled。
- [x] `AC-009`：四类 Owner 均登记真实表名/主键、生命周期策略和可执行证据。
- [x] `AC-010`：基线测试覆盖多引用、差集更新和最后解绑 TEMP 语义。
- [x] `AC-011`：有引用对象不成为删除/清理候选且通用删除保护保持通过。
- [x] 公共 OssService 不再暴露 bind/unbind，业务只能通过集合协调契约维护引用。
- [x] 新持久化 carrier 或 reconcile 调用方未登记时架构测试明确失败。
- [x] application.yml 保持 `cleanup-enabled:false`、`cleanup-dry-run:true`。
- [x] 验证矩阵全部记录到 `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-22.md</Path>`。
- [x] 修改未超出获批 `writable_paths`，并按新 Goal Plan 形成非空 implementation commit。
