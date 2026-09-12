---
schema_version: 3
artifact: ticket
change: 2026-08-28-user-password-policy-temporary-credentials
id: T-07
title: 交付用户管理重置与临时密码工作流
status: done
planning_depth: deep
planning_depth_reason: 管理员页面直接展示和提交敏感凭据，新增独立权限、一次性展示、复制与服务端候选编辑交互。
ready: true
risk: high
blocked_by: [T-04, T-05, T-06]
contract_ids: [AC-001, AC-002, AC-003, AC-005, AC-006, AC-017, AC-023]
owner: codex:lead
expected_changes: ["<Path>plus-ui-namewta/packages/domains/system/src/index.ts</Path>", "<Path>plus-ui-namewta/packages/domains/system/src/index.test.ts</Path>", "<Path>plus-ui-namewta/packages/domains/system/src/transport.ts</Path>", "<Path>plus-ui-namewta/packages/domains/system/src/transport.test.ts</Path>", "<Path>plus-ui-namewta/packages/domains/system/src/user/index.ts</Path>", "<Path>plus-ui-namewta/packages/domains/system/src/user/types.ts</Path>", "<Path>plus-ui-namewta/packages/web-domains/system/src/user/**</Path>", "<Path>plus-ui-namewta/packages/web-domains/system/src/runtime.ts</Path>", "<Path>plus-ui-namewta/packages/web-domains/system/src/index.ts</Path>", "<Path>plus-ui-namewta/packages/web-domains/system/src/index.test.ts</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/router/adminManifestRegistry.ts</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/router/adminManifestRegistry.test.ts</Path>", "<Path>plus-ui-namewta/e2e/system-identity.spec.ts</Path>", "<Path>plus-ui-namewta/e2e/system-resources.spec.ts</Path>"]
writable_paths: ["<Path>plus-ui-namewta/packages/domains/system/src/index.ts</Path>", "<Path>plus-ui-namewta/packages/domains/system/src/index.test.ts</Path>", "<Path>plus-ui-namewta/packages/domains/system/src/transport.ts</Path>", "<Path>plus-ui-namewta/packages/domains/system/src/transport.test.ts</Path>", "<Path>plus-ui-namewta/packages/domains/system/src/user/index.ts</Path>", "<Path>plus-ui-namewta/packages/domains/system/src/user/types.ts</Path>", "<Path>plus-ui-namewta/packages/web-domains/system/src/user/**</Path>", "<Path>plus-ui-namewta/packages/web-domains/system/src/runtime.ts</Path>", "<Path>plus-ui-namewta/packages/web-domains/system/src/index.ts</Path>", "<Path>plus-ui-namewta/packages/web-domains/system/src/index.test.ts</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/router/adminManifestRegistry.ts</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/router/adminManifestRegistry.test.ts</Path>", "<Path>plus-ui-namewta/e2e/system-identity.spec.ts</Path>", "<Path>plus-ui-namewta/e2e/system-resources.spec.ts</Path>"]
read_only_paths: ["<Path>plus-ui-namewta/packages/api-contracts/**</Path>", "<Path>plus-ui-namewta/packages/domains/admin/src/index.ts</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/application/services.ts</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/router/index.ts</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysUserCredentialController.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysTemporaryPasswordController.java</Path>"]
shared_paths: []
shared_path_owners: []
---

# Ticket T-07: 交付用户管理重置与临时密码工作流

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/ticket/07-deliver-admin-user-credential-workflows.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/evidence/T-07.md</Path>`

## 1. 战略与来源

- **目标：** 在用户管理页把服务端重置候选与临时密码签发变成安全、可理解、权限独立的完整管理员操作。
- **可观察产出：** 重置对话框预填服务端候选、允许编辑并显示具体规则；临时密码按钮仅有独立权限时可见，签发后一次展示、可复制并显示 60 秒有效期。
- **来源：** `US-001`、`US-002`、`US-009`、`AC-001` 至 `AC-003`、`AC-005`、`AC-006`、`AC-017`、`AC-023`。
- **当前事实：** UserPage 当前用本地 prompt 要求 5-20 位并直接 reset；没有临时按钮；`createSystemWebDomain` manifest 只声明 `system:user:resetPwd`；SystemWebRuntime 没有 passwordPolicy port。当前业务菜单已强制 manifest-only，Admin registry 只组合 manifest/host runtime，`router/index.ts` 不拥有用户管理业务路由。
- **Planning Depth 原因：** 敏感值显示/复制、权限失败关闭、domain/web-domain/App runtime 组合和复杂页面 Ratchet 同时发生。

## 2. 决策状态

### 已锁定决策

- system domain 定义 candidate/temporary API 领域模型，不把 OpenAPI transport 直接泄漏到页面。
- SystemWebRuntime 通过最小异步 port 提供公开 PasswordPolicy/validator，Admin 在 `adminManifestRegistry.ts` 从 identityAccessService 显式注入；web-domain 不依赖 domain-admin。
- 重置先请求 candidate，显示/编辑/确认，最终调用 reset；关闭/取消不提交。
- 临时按钮使用 `system:user:temporaryPassword`，重置仍用 `system:user:resetPwd`；权限互不包含，二者都由 `createSystemWebDomain` manifest 声明，不在 App 私有权限表或静态路由重复登记。
- 临时明文只在本次成功 modal 生命周期展示，提供复制图标/tooltip 和 60 秒提示，关闭后不保存在 store/localStorage。

### 已采用的低影响假设

- 在 UserPage 同目录提取 page-local credential dialog/composable，避免继续扩大大 SFC；是否组件或 composable按可测试职责决定。验证：依赖与组件测试。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| system domain APIs、runtime policy port、重置/临时 UI、权限 manifest、copy/expiry、E2E | T-06 policy model/identity service、Element Plus、现有 host feedback | 不实现“查看已有临时密码”、倒计时续期、自动登录或永久密码会话注销 |

## 4. 要构建什么

有 reset 权限的管理员点击钥匙图标，页面先获取候选并打开可编辑确认；不合规时就地显示各具体原因，确认成功后提示完成。具有 temporaryPassword 权限时出现独立临时凭据操作；签发成功 modal 显示一次密码、复制按钮和 60 秒说明，关闭即清内存。任何 API/权限失败不暴露空密码、不误报成功、不改变表格布局。

## 5. 实现契约

- **入口或接缝：** system domain `users` service、SystemWebRuntime、UserPage 行操作、manifest permissions、Playwright。
- **输入与输出：** userId -> candidate/temporary response；policy -> form violations；commands -> feedback。
- **公共接口变化：** `SystemService.users` additive methods 与 user-owned 类型；`SystemWebRuntime` additive passwordPolicy port；system web-domain manifest 新权限。App router 无公共接口变化。
- **不变量：** candidate 来自 server；最终值再提交；temporary 一次展示且不持久化；两个权限独立；服务端仍最终鉴权。
- **状态或数据流：** click -> request -> modal state -> validate/confirm or copy/close -> clear sensitive refs。
- **错误与失败行为：** stale row/permission/network/server violations 均恢复 loading/modal；复制失败有反馈；重复点击不产生混乱响应。
- **兼容要求：** 用户列表、add/edit/delete/authRole/import 既有流程不变；reset path/method 与 backend 对齐。
- **安全与隐私要求：** 不 console/log/telemetry/store 明文；DOM 关闭后引用清理；只有授权按钮可见且后端独立校验。

## 6. 执行路线

1. 在 domain-system 为两个 API 建立 URL/method/encryption/no-cache transport tests。
2. 扩展 SystemWebRuntime policy port，并只在 Admin manifest registry 注入宿主实现，保持包依赖方向和 manifest-only 导航。
3. 提取可测试的用户凭据交互边界并替换旧 reset prompt。
4. 增加临时按钮、一次展示、复制、60 秒说明、关闭清理和独立权限。
5. 覆盖 candidate/submit 错误、server violations、重复点击、copy failure 和权限组合。
6. 运行 domain/web-domain/App gates 与真实浏览器流程。

## 7. 路径访问契约

- **预计修改点：** domain-system root/user types、web-domain system user/runtime/manifest、Admin manifest registry 及其测试、system identity E2E。
- **可写范围：** frontmatter 精确路径。
- **只读上下文：** T-06 api-contracts/admin policy、backend controllers。
- **共享路径：** 无；api-contracts 由 T-06 独占。
- **保留或不动：** register/profile、backend、DML、`apps/admin-web/src/router/index.ts` 静态路由、根依赖与 lockfile。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | domain/component/browser | 重置候选编辑确认、临时签发复制关闭 | API 精确、结果可见、关闭清理、权限正确 | `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/evidence/T-07.md</Path>` |
| 失败路径 | component/E2E | 无权限、违规、network、重复、copy failure | 失败关闭、具体反馈、无秘密残留/假成功 | 同上 |
| 回归 | frontend gates | `pnpm architecture:check`、`pnpm architecture:test`、`pnpm lint`、`pnpm typecheck`、`pnpm test`、`pnpm build:dev`、`pnpm build:prod` | system 领域和 Admin 组合绿色 | 同上 |

- **Workspace checks：** Goal Plan current/source-worktree 运行非 E2E gates。
- **E2E disposition：** required：敏感值一次展示、权限可见性和完整用户管理操作只能由浏览器边界充分证明。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate；运行 `system-identity.spec.ts` 新场景并保存截图/网络断言。
- **Integration evidence：** frontend commit、parent/candidate/result SHA、父 gitlink。

## 9. 发布、迁移与恢复

- **迁移顺序：** T-04/T-05 backend 与 T-06 OpenAPI/policy 先集成；T-08 权限 DML 后开放入口。
- **兼容窗口：** 新前端必须连接新 backend；backend 可先行且 additive。
- **监控信号：** candidate/issue API failures、权限拒绝、复制失败；不采集明文。
- **回滚或前向恢复：** 前端回滚只隐藏新能力，backend endpoint 可保留；残留 temp key 60 秒自清。
- **不可逆操作与批准点：** 永久 reset 仍需管理员确认；提交/集成/发布需授权。
- **收缩条件：** 旧 5-20 reset prompt 与 `initPassword` UI 读取零匹配。

## 10. 验收标准

- [x] `AC-001` 至 `AC-003` 的候选、编辑、最终校验 UI 成立。
- [x] `AC-005`、`AC-006` 独立权限和一次展示成立。
- [x] `AC-017` 管理员新增/重置 UI 不再产生弱值，`AC-023` 前端无审计泄露。
- [x] frontend gates/E2E、commit、Lead 集成和父状态完整。
