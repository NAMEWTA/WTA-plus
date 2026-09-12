---
schema_version: 3
artifact: ticket
change: 2026-08-28-user-password-policy-temporary-credentials
id: T-06
title: 在注册与个人改密采用公开密码策略
status: done
planning_depth: deep
planning_depth_reason: 该 Ticket 更新 OpenAPI 生成合同、未认证注册边界和敏感表单交互，需同步后端最终 API 并保持失败关闭。
ready: true
risk: high
blocked_by: [T-04, T-05]
contract_ids: [AC-003, AC-015, AC-016, AC-018]
owner: codex:lead
expected_changes: ["<Path>plus-ui-namewta/packages/api-contracts/**</Path>", "<Path>plus-ui-namewta/packages/domains/admin/**</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/views/register.vue</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/views/system/user/profile/resetPwd.vue</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/lang/**</Path>", "<Path>plus-ui-namewta/e2e/client-auth-context.spec.ts</Path>"]
writable_paths: ["<Path>plus-ui-namewta/packages/api-contracts/**</Path>", "<Path>plus-ui-namewta/packages/domains/admin/**</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/views/register.vue</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/views/system/user/profile/resetPwd.vue</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/lang/**</Path>", "<Path>plus-ui-namewta/e2e/client-auth-context.spec.ts</Path>"]
read_only_paths: ["<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/java/org/dromara/web/controller/AuthController.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/java/org/dromara/web/domain/vo/AuthClientContextVo.java</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/application/services.ts</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/router/index.ts</Path>", "<Path>plus-ui-namewta/packages/domains/system/src/index.ts</Path>"]
shared_paths: ["<Path>plus-ui-namewta/packages/api-contracts/**</Path>"]
shared_path_owners: ["<Path>plus-ui-namewta/packages/api-contracts/**</Path> => T-06"]
---

# Ticket T-06: 在注册与个人改密采用公开密码策略

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/ticket/06-adopt-policy-in-registration-profile.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/evidence/T-06.md</Path>`

## 1. 战略与来源

- **目标：** 让公开注册和当前用户改密使用服务端公开投影构建一致即时校验，并保持后端失败为最终权威。
- **可观察产出：** 注册/个人改密对长度、四类字符和允许 specials 给出具体反馈；策略不可用时注册失败关闭；存量弱用户仍能登录并在改密时收敛。
- **来源：** `US-004`、`US-007`、`AC-003`、`AC-015`、`AC-016`、`AC-018`。
- **当前事实：** admin domain context 只有两个 boolean；register 为 5-20 + 禁用字符，profile 为 6-20；OpenAPI snapshot 尚无新字段/API。
- **Planning Depth 原因：** 未认证公共 transport、敏感表单、OpenAPI provenance 和安全错误呈现共同变化。

## 2. 决策状态

### 已锁定决策

- T-06 唯一更新本 change 的 OpenAPI snapshot/current/generated files，快照必须来自已集成 T-04/T-05 backend commit 并保留 provenance。
- domain-admin 把 transport narrowing 为 domain-owned PasswordPolicy；未知/缺字段失败关闭，不默认放宽到 5/6 位。
- 注册和 profile 复用 `domain-admin` 拥有的纯 PasswordPolicy validator，App 在 `<Path>plus-ui-namewta/apps/admin-web/src/lang/**</Path>` 映射可展示消息，不复制 regex；服务端 violations 仍直接可展示。
- 注册与 profile 保持 Admin App 私有静态页面；manifest-only 约束适用于服务端业务菜单，本 Ticket 不把它们迁入 web-domain，也不修改 `router/index.ts`。
- UI 不接触 mode/fixed/generator，且不把公开投影当授权边界。

### 已采用的低影响假设

- 个人中心可通过现有 identityAccessService 获取已缓存/重新获取的 Client context，不新增第二个 HTTP adapter。验证：请求次数组件测试。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| OpenAPI 同步、admin transport/model、注册/profile 动态规则、i18n、E2E | identityAccessService、Element Plus form、system updatePassword | 不做用户管理页，不生成密码，不在浏览器接收内部配置 |

## 4. 要构建什么

注册页加载 Client context 后才开放注册，使用公开策略逐项反馈。个人改密也使用同一规则，在前端可快速纠正，但直接 API 失败仍展示服务端稳定原因。响应缺失/畸形策略时不回退旧弱规则。页面不展示配置 mode、fixedValue 或字符池。

## 5. 实现契约

- **入口或接缝：** domain-admin getClientContext/transport、register.vue、profile resetPwd.vue、Playwright client context。
- **输入与输出：** transport projection -> immutable domain policy -> Element Plus validation errors；server violations -> user messages。
- **公共接口变化：** frontend domain `ClientAuthContext` additive `passwordPolicy`；OpenAPI 生成物同步 backend。
- **不变量：** 规则来自 context；两页一致；策略缺失失败关闭；confirm mismatch 独立于 policy。
- **状态或数据流：** GET context -> parse -> form rules -> submit -> server authoritative result。
- **错误与失败行为：** context malformed/unavailable 禁止注册/保存并提示配置不可用；网络/取消后 loading 终态。
- **兼容要求：** Client/register enabled 既有行为保持；旧存量登录不受前端规则影响。
- **安全与隐私要求：** 不持久化输入密码，不输出 console/log；生成 bundle 无内部策略字段。

## 6. 执行路线

1. 从已集成 backend OpenAPI fetch/generate，审查只包含预期 contract diff，并运行 `pnpm --filter @namewta/tooling-openapi openapi:check` 验证 provenance 与生成漂移。
2. 扩展 domain-admin transport narrowing 和恶意/畸形响应测试。
3. 建立可测试的公开策略 validator/message 投影并接入注册表单。
4. 接入个人改密，保持旧密码、新旧相同和确认密码既有语义。
5. 覆盖服务端 violations、context failure、重复请求/loading 和无敏感字段。
6. 运行 package/App/unit/architecture/type/build 与浏览器 E2E。

## 7. 路径访问契约

- **预计修改点：** api-contracts、domain-admin、register/profile SFC、`apps/admin-web/src/lang/**`、client context E2E。
- **可写范围：** frontmatter 所列前端路径。
- **只读上下文：** backend contract、App services、system domain updatePassword。
- **共享路径：** api-contracts 仅 T-06 写；T-07 只读生成类型。
- **保留或不动：** backend、system web-domain UserPage、锁文件/依赖。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | Vitest + browser | 合规注册/改密 | 两页同规则，通过并保持既有流程 | `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/evidence/T-06.md</Path>` |
| 失败路径 | transport/component | 缺字段、每类违规、server violations、请求失败 | fail-close、具体反馈、状态复位 | 同上 |
| 回归 | frontend gates | `pnpm --filter @namewta/tooling-openapi openapi:check`、`pnpm architecture:check`、`pnpm architecture:test`、`pnpm lint`、`pnpm typecheck`、`pnpm test`、`pnpm build:dev`、`pnpm build:prod` | provenance/manifest-only 依赖/类型/双模式构建绿色 | 同上 |

- **Workspace checks：** Goal Plan current/source-worktree 非 E2E gates。
- **E2E disposition：** required：公开未认证 context -> 注册和登录后 profile 是真实浏览器安全路径。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate；运行 client-auth-context 和 profile 场景，截图/网络断言。
- **Integration evidence：** frontend commit、backend provenance SHA、parent/candidate/result SHA、父 gitlink。

## 9. 发布、迁移与恢复

- **迁移顺序：** T-04/T-05 backend 先进入父状态，再 fetch OpenAPI 和发布前端。
- **兼容窗口：** backend additive 先行；新前端不兼容缺少 policy 的旧 backend，禁止前端先发。
- **监控信号：** client-context unavailable、registration violations、profile failure，不含密码。
- **回滚或前向恢复：** 前端可单独回滚到旧版本但会失去即时强校验，后端仍阻止弱写；优先前向修复 UI。
- **不可逆操作与批准点：** 无；OpenAPI 快照和 frontend commit/integration 需授权。
- **收缩条件：** 旧 5/6-20 静态规则在两个页面零匹配。

## 10. 验收标准

- [x] `AC-015` 投影被严格解析且内部字段不可见。
- [x] `AC-003`、`AC-016`、`AC-018` 在注册/profile UI 与直接 API 组合成立。
- [x] OpenAPI provenance 指向已集成 backend，check 无 drift。
- [x] frontend gates/E2E、commit、Lead 集成和父状态完整。
