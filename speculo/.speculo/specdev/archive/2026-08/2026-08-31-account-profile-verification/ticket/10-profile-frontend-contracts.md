---
schema_version: 3
artifact: ticket
change: 2026-08-31-account-profile-verification
id: T-10
title: 建立前端 Profile Domain、Transport 与包级公共合同
status: done
planning_depth: deep
planning_depth_reason: 公共 transport、运行时端口和 package exports 同时约束三个管理面，错误会放大为跨票据返工。
ready: true
risk: high
blocked_by: [T-04, T-05, T-06, T-07, T-08, T-09]
contract_ids: [AC-009, AC-010, AC-024, AC-031, AC-032, AC-034, AC-038, AC-039, AC-040, AC-041]
owner: codex:/root
expected_changes:
  - "<Path>plus-ui-namewta/packages/domains/profile/**</Path>"
  - "<Path>plus-ui-namewta/packages/web-domains/profile/package.json</Path>"
  - "<Path>plus-ui-namewta/packages/web-domains/profile/tsconfig.json</Path>"
  - "<Path>plus-ui-namewta/packages/web-domains/profile/src/index.ts</Path>"
  - "<Path>plus-ui-namewta/packages/web-domains/profile/src/runtime.ts</Path>"
  - "<Path>plus-ui-namewta/apps/admin-web/package.json</Path>"
  - "<Path>plus-ui-namewta/pnpm-lock.yaml</Path>"
  - "<Path>plus-ui-namewta/tooling/architecture/test/domain-layout.test.mjs</Path>"
writable_paths:
  - "<Path>plus-ui-namewta/packages/domains/profile/**</Path>"
  - "<Path>plus-ui-namewta/packages/web-domains/profile/package.json</Path>"
  - "<Path>plus-ui-namewta/packages/web-domains/profile/tsconfig.json</Path>"
  - "<Path>plus-ui-namewta/packages/web-domains/profile/README.md</Path>"
  - "<Path>plus-ui-namewta/packages/web-domains/profile/src/index.ts</Path>"
  - "<Path>plus-ui-namewta/packages/web-domains/profile/src/runtime.ts</Path>"
  - "<Path>plus-ui-namewta/packages/web-domains/profile/src/page-types.d.ts</Path>"
  - "<Path>plus-ui-namewta/apps/admin-web/package.json</Path>"
  - "<Path>plus-ui-namewta/pnpm-lock.yaml</Path>"
  - "<Path>plus-ui-namewta/tooling/architecture/test/domain-layout.test.mjs</Path>"
read_only_paths:
  - "<Path>plus-ui-namewta/packages/domains/system/**</Path>"
  - "<Path>plus-ui-namewta/packages/web-domains/system/**</Path>"
  - "<Path>plus-ui-namewta/apps/admin-web/src/router/adminManifestRegistry.ts</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/**</Path>"
shared_paths:
  - "<Path>plus-ui-namewta/packages/web-domains/profile/package.json</Path>"
  - "<Path>plus-ui-namewta/packages/web-domains/profile/src/index.ts</Path>"
  - "<Path>plus-ui-namewta/apps/admin-web/package.json</Path>"
  - "<Path>plus-ui-namewta/pnpm-lock.yaml</Path>"
shared_path_owners:
  - "<Path>plus-ui-namewta/packages/web-domains/profile/package.json</Path> => T-10"
  - "<Path>plus-ui-namewta/packages/web-domains/profile/src/index.ts</Path> => T-10"
  - "<Path>plus-ui-namewta/apps/admin-web/package.json</Path> => T-10"
  - "<Path>plus-ui-namewta/pnpm-lock.yaml</Path> => T-10"
---

# Ticket T-10: 建立前端 Profile Domain、Transport 与包级公共合同

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/10-profile-frontend-contracts.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-10.md</Path>`

## 1. 战略与来源

- **目标：** 固化 profile 管理端共享模型、HTTP transport、权限常量、运行时端口和 WebDomainManifest 包接缝。
- **可观察产出：** 个人、企业、材料标签三个页面可在不修改公共包入口和锁文件的前提下独立实现，并与后端明文/材料/权限合同一致。
- **来源：** `US-011`、`US-016`、`US-021`、`US-022` 及所列 AC。
- **当前事实：** admin-web 已采用 domain -> web-domain -> App 显式组合；尚无 profile 包。
- **Planning Depth 原因：** 这是三个页面票据共享的 compile-time 和 runtime 边界。

## 2. 决策状态

### 已锁定决策

- 新增 `@namewta/domain-profile` 和 `@namewta/web-domain-profile`，不把 profile API 放回 system domain。
- transport 按完整业务能力提供闭合命令；页面不得依赖额外隐藏权限。
- 普通探测响应类型不含主体 ID/字段；个人换绑掩码只存在于完整匹配专用响应。
- manifest 预注册材料标签、个人列表/详情、企业列表/详情和 workflow formPath componentKey。
- package exports 在本票一次声明后冻结，T-11/T-12/T-13 只写各自子目录。

### 已采用的低影响假设

- 使用现有 service transport、`R` 解包、权限 evaluator、OSS 下载端口和运行时反馈模式。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| profile types/service/permissions、web runtime、manifest skeleton、包依赖 | system/workflow domain 范式、platform runtime | 具体页面、App 组合、申请人端 UI |

## 4. 要构建什么

建立无 Vue 的 profile domain 合同与 transport，再建立薄 web-domain 运行时/清单入口；公共接口完整覆盖查询、材料、审核、管理和 override，但不在 transport 中弱化后端授权或脱敏规则。

## 5. 实现契约

- **入口或接缝：** `@namewta/domain-profile`、`@namewta/web-domain-profile` exports 与 `createProfileWebDomain(runtime)`。
- **输入与输出：** 后端 DTO/分页/命令 -> 类型安全 service；runtime 提供 service、权限、确认、反馈、OSS 下载及 workflow 宿主动作。
- **公共接口变化：** 新增两个 workspace package 并由 admin-web 声明依赖。
- **不变量：** domain 无 Vue；web-domain 无 App 别名；探测类型不允许意外回显；无 export 功能。
- **状态或数据流：** page -> runtime service -> profile HTTP -> typed result。
- **错误与失败行为：** 后端拒绝原样成为页面可处理失败；不把无权/不存在转成有数据结果。
- **兼容要求：** 不改变现有 domain/web-domain exports 和动态菜单解析。
- **安全与隐私要求：** 明文 DTO 只在管理服务；敏感命令参数不得由通用错误/调试输出持久化。

## 6. 执行路线

1. 从已实现后端合同提取共享 DTO、命令和权限 ID。
2. 建立 profile service resources 与 transport schema 测试。
3. 建立 web runtime、componentKey 清单及稳定 exports。
4. 更新 admin-web package 依赖和 pnpm lock。
5. 运行 domain/web-domain/admin 的 typecheck、lint 和定向测试。

## 7. 路径访问契约

- **预计修改点/可写范围：** profile domain 全包、web-domain 根合同文件、admin package 和 lock。
- **只读上下文：** system/workflow 前端范式和后端 HTTP 合同。
- **共享路径：** web-domain 入口、App package 与 lock 由 T-10 唯一持有。
- **保留或不动：** T-11/T-12/T-13 页面子树、App runtime 组合、其他 domains。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常 | Vitest/typecheck | service DTO、manifest registrations、exports | 类型和 componentKey 稳定 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-10.md</Path>` |
| 失败 | transport contract | 探测/无权/错误 DTO | 不扩展敏感响应，不吞拒绝 | 同上 |
| 回归 | workspace package graph | filters + admin typecheck | 旧包与动态路由无回归 | 同上 |

- **Workspace checks：** `pnpm --filter @namewta/domain-profile test/typecheck`、web-domain test/typecheck、admin typecheck。
- **E2E disposition：** not-required：本票只建立公共合同，真实管理流程由 T-14 统一 E2E。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate；仅执行 package contract 检查，真实 E2E 归 T-14。
- **Integration evidence：** 包图、transport/manifest 测试与锁文件 diff。

## 9. 发布、迁移与恢复

- **迁移顺序：** 后端 API 后安装包，页面和 App 组合最后开放。
- **兼容窗口：** 新包未被 App 组合时不影响现有路由。
- **监控信号：** transport 4xx/5xx 与未知 componentKey。
- **回滚或前向恢复：** 移除 App 组合后新包可留存；合同修复前向发布。
- **不可逆操作与批准点：** 无生产数据操作。
- **收缩条件：** 不适用：全新包。

## 10. 验收标准

- [x] 所列 AC 的 transport、权限、隐私和 manifest 合同有自动化验证。
- [x] T-11/T-12/T-13 无需改公共入口、App package 或锁文件即可实现。
- [x] Evidence、提交和集成结果完整。
