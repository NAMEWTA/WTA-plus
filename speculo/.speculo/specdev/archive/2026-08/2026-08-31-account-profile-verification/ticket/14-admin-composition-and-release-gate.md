---
schema_version: 3
artifact: ticket
change: 2026-08-31-account-profile-verification
id: T-14
title: 完成 admin-web 显式组合与 Profile 发布验收
status: done
planning_depth: deep
planning_depth_reason: 最终汇合后端装配、数据库种子、动态路由、权限、workflow、材料访问和三管理面，承担发布阻断职责。
ready: true
risk: critical
blocked_by: [T-02, T-11, T-12, T-13]
contract_ids: [AC-001, AC-002, AC-005, AC-007, AC-024, AC-025, AC-031, AC-034, AC-038, AC-039, AC-040, AC-041, AC-042, AC-043, AC-044]
owner: codex:/root
expected_changes:
  - "<Path>plus-ui-namewta/apps/admin-web/src/**</Path>"
  - "<Path>plus-ui-namewta/e2e/profile-management.spec.ts</Path>"
  - "<Path>plus-ui-namewta/packages/domains/profile/src/**</Path>"
  - "<Path>plus-ui-namewta/packages/web-domains/profile/src/**</Path>"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/60-namewta-dml.sql</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/profile/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/resources/oss/business-oss-owners.json</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/system/api/UserService.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysUserServiceImpl.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/listener/WorkflowGlobalListener.java</Path>"
writable_paths:
  - "<Path>plus-ui-namewta/apps/admin-web/src/**</Path>"
  - "<Path>plus-ui-namewta/e2e/profile-management.spec.ts</Path>"
  - "<Path>plus-ui-namewta/packages/domains/profile/src/**</Path>"
  - "<Path>plus-ui-namewta/packages/web-domains/profile/src/**</Path>"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/60-namewta-dml.sql</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/profile/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/resources/oss/business-oss-owners.json</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/system/api/UserService.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysUserServiceImpl.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/listener/WorkflowGlobalListener.java</Path>"
read_only_paths: []
shared_paths:
  - "<Path>plus-ui-namewta/apps/admin-web/src/application/services.ts</Path>"
  - "<Path>plus-ui-namewta/apps/admin-web/src/router/adminManifestRegistry.ts</Path>"
shared_path_owners:
  - "<Path>plus-ui-namewta/apps/admin-web/src/application/services.ts</Path> => T-14"
  - "<Path>plus-ui-namewta/apps/admin-web/src/router/adminManifestRegistry.ts</Path> => T-14"
---

# Ticket T-14: 完成 admin-web 显式组合与 Profile 发布验收

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/14-admin-composition-and-release-gate.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-14.md</Path>`

## 1. 战略与来源

- **目标：** 把 profile service、domain 和三个管理面显式组合进 admin-web，并以完整后端/前端/E2E 证据作为发布 Gate。
- **可观察产出：** 数据迁移后 bundle-core/full 均可启动；动态菜单可解析三页面和两类审核上下文；授权、材料、workflow、历史不删除和 system 无副作用在真实链路通过。
- **来源：** `US-001`、`US-002`、`US-011`、`US-017`、`US-020` 至 `US-022` 及所列 AC。
- **当前事实：** 前序票据各自交付后端能力和页面，但 admin App 尚未显式选择 profile。
- **Planning Depth 原因：** 这是所有共享路径与跨模块合同的最终集成阻断点。

## 2. 决策状态

### 已锁定决策

- admin-web 显式创建 profile service/runtime 并把 domain module/manifest 加入 composeAppRuntime。
- 三菜单来自 DML 动态路由；组件注册必须覆盖材料标签、个人/企业列表详情和两个 workflow formPath。
- bundle-core 没有 workflow 时应用、查询、草稿可用，提交失败关闭；bundle-full 完成两套流程。
- E2E 使用实际 MySQL、后端、admin-web 与 workflow/OSS 接缝；关键失败行为必须留 evidence。
- 不因验收修改 system 角色、菜单授予、Client 或登录域合同。

### 已采用的低影响假设

- 端到端数据通过专用测试 fixture 创建并以逻辑状态收口，不删除业务历史/OSS 引用。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| App service/runtime/manifest 组合、assembly test、Playwright 发布 Gate | T-01 至 T-13 成果、现有 E2E 基建 | 新业务能力、申请人页面、生产部署/授权 |

## 4. 要构建什么

在 admin-web App 层完成 profile 的唯一显式组合，增加注册诊断；建立后端 assembly 测试和覆盖三管理面、审核、权限、材料、终态与回归合同的 E2E，所有发布级证据汇总到本票 Evidence。

## 5. 实现契约

- **入口或接缝：** admin `services.ts`、`adminManifestRegistry.ts`、后端 profile assembly context 与 Playwright。
- **输入与输出：** profile service/runtime + domain/manifest -> 可解析动态路由和完整用户管理流程。
- **公共接口变化：** admin-web 选择已存在 profile 包，不新增跨域合同。
- **不变量：** 所有动态 componentKey 唯一；无隐式全量 glob；core/full 装配合同都成立。
- **状态或数据流：** DDL/DML -> backend assembly -> app compose -> dynamic route -> real API/workflow/OSS。
- **错误与失败行为：** 缺 workflow、无权限、流程终止失败、材料非法、revoked 写入和迟到事件按合同失败，不跳过 Gate。
- **兼容要求：** 现有 admin/system/workflow/ai/demo 注册和 system 登录授权回归通过。
- **安全与隐私要求：** 明文/材料正反权限、日志无敏感原文、无数据权限误过滤均需集成验证。

## 6. 执行路线

1. 增加 profile service 和运行时适配，显式组合 domain module/manifest。
2. 扩展 manifest registry 诊断与 componentKey/权限注册测试。
3. 建立 core/full 后端装配、ProfileService 和 workflow 可选性测试。
4. 在真实环境执行个人/企业/标签管理及权限/材料/审核关键 E2E。
5. 执行 SQL/日志/DELETE/system 回归扫描，记录 candidate/result SHA 和发布判定。

## 7. 路径访问契约

- **预计修改点/可写范围：** admin App 两个共享组合文件及测试、单一 profile E2E；后端 assembly 只运行 T-01 已交付的测试。
- **只读上下文：** 全部 profile 包、模块和 SQL。
- **共享路径：** App service/manifest 由 T-14 唯一持有。
- **保留或不动：** 前序票据实现、package/lock、生产数据与配置。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常 | MySQL/backend/admin/Playwright | 标签、个人、企业、审核、明文/材料、ProfileService | 三管理面和跨模块合同完成 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-14.md</Path>` |
| 失败 | auth/workflow/OSS/race | 无权、缺 workflow、终止失败、材料非法、终态写入 | 失败关闭且无部分状态/泄漏 | 同上 |
| 回归 | bundle/system/log/SQL | core/full 启动、认证后 system diff、日志与删除扫描 | 旧功能稳定，无敏感日志/物理删除 | 同上 |

- **Workspace checks：** 后端 Maven 定向+reactor、前端 lint/typecheck/test/build、Playwright profile flow、SQL/log/static scan。
- **E2E disposition：** required：本票是正式集成与发布 Gate。
- **E2E owner/environment：** Lead / Goal Plan 选定的 parent-candidate 与真实 MySQL/workflow/OSS 测试环境。
- **Integration evidence：** ticket commits、parent/candidate/result SHA、测试命令/输出、关键状态快照和发布判定。

## 9. 发布、迁移与恢复

- **迁移顺序：** DDL -> DML -> 后端 core/full -> 前端 App 组合 -> 权限人工赋予 -> observe。
- **兼容窗口：** 菜单未赋权时新 UI 不可见；后端公开服务可先部署。
- **监控信号：** 启动失败、未知 componentKey、profile 4xx/5xx、workflow 启停失败、override/材料访问和日志告警。
- **回滚或前向恢复：** 先撤菜单/前端组合，再回退后端；已执行档案/绑定/版本不删除，使用前向处置。
- **不可逆操作与批准点：** 生产 DDL/DML、菜单赋权、revoke/override 和发布均未由票据授权。
- **收缩条件：** 不适用：全新能力，可通过不赋权和移除组合关闭入口。

## 10. 验收标准

- [x] 三动态菜单、个人/企业审核 formPath、权限按钮和 API 合同均可用。
- [x] core/full、workflow/OSS、明文/材料、历史不删除和 system 回归 Gate 全部通过。
- [x] required E2E、Evidence、提交和 parent/candidate/result SHA 已完整；生产发布仍需单独授权。
