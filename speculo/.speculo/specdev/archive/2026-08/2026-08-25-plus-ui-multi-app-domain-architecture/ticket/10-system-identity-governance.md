---
schema_version: 3
artifact: ticket
change: 2026-08-25-plus-ui-multi-app-domain-architecture
id: T-10
title: 迁移 system-admin 身份与组织治理
status: done
planning_depth: deep
planning_depth_reason: Client、用户、角色、菜单和组织模型共同参与授权，需要实现既有 user public seam 并保持权限语义与跨领域兼容
ready: true
risk: high
blocked_by: [T-09]
contract_ids: [AC-009, AC-010, AC-017, AC-018, AC-021]
owner: codex:/root
expected_changes: ["<Path>plus-ui-namewta/packages/domains/system-admin/**</Path>", "<Path>plus-ui-namewta/packages/web-domains/system-admin/**</Path>", "<Path>plus-ui-namewta/src/api/system/**</Path>", "<Path>plus-ui-namewta/src/views/system/**</Path>"]
writable_paths: ["<Path>plus-ui-namewta/packages/domains/system-admin/**</Path>", "<Path>plus-ui-namewta/packages/web-domains/system-admin/**</Path>", "<Path>plus-ui-namewta/src/api/system/client/**</Path>", "<Path>plus-ui-namewta/src/api/system/user/**</Path>", "<Path>plus-ui-namewta/src/api/system/userType/**</Path>", "<Path>plus-ui-namewta/src/api/system/role/**</Path>", "<Path>plus-ui-namewta/src/api/system/menu/**</Path>", "<Path>plus-ui-namewta/src/api/system/dept/**</Path>", "<Path>plus-ui-namewta/src/api/system/post/**</Path>", "<Path>plus-ui-namewta/src/views/system/client/**</Path>", "<Path>plus-ui-namewta/src/views/system/user/**</Path>", "<Path>plus-ui-namewta/src/views/system/userType/**</Path>", "<Path>plus-ui-namewta/src/views/system/role/**</Path>", "<Path>plus-ui-namewta/src/views/system/menu/**</Path>", "<Path>plus-ui-namewta/src/views/system/dept/**</Path>", "<Path>plus-ui-namewta/src/views/system/post/**</Path>", "<Path>plus-ui-namewta/e2e/system-identity.spec.ts</Path>"]
read_only_paths: ["<Path>plus-ui-namewta/packages/domains/system-admin/public/user/**</Path>", "<Path>plus-ui-namewta/packages/domains/identity-access/**</Path>", "<Path>plus-ui-namewta/src/permission.ts</Path>"]
shared_paths: []
shared_path_owners: []
---

# Ticket T-10: 迁移 system-admin 身份与组织治理

- **工件：** Ticket `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/ticket/10-system-identity-governance.md</Path>`；Map `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/tickets-map.md</Path>`；Spec `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/spec.md</Path>`；Evidence `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-10.md</Path>`。

## 1. 战略与来源

- **目标/产出：** system-admin 的 Client、用户、用户类型、角色、菜单、部门、岗位形成可组合的身份治理纵切，并实现 T-09 的用户查询公开合同。
- **来源：** `US-004`、`US-005`、`US-007`、`AC-009`、`AC-010`、`AC-017`、`AC-018`、`AC-021`、`ADR-003`、`ADR-005`。
- **当前事实：** 对应 API/views 分散在根 src；workflow/devtools 消费其中用户、菜单能力。
- **Planning Depth 原因：** 涉及授权管理、高敏用户数据和既有跨领域公共 seam。

## 2. 决策状态

### 已锁定决策

- system-admin 公开最小 user/menu contracts，其他领域不 deep import 管理实现。
- T-09 user public exports 是既有合同，本 Ticket 实现它而不破坏签名；扩展需向后兼容。
- superadmin/admin alias 与多 Client 语义服从 T-07 的 auth matrix。
- `DEV-T10-001`：T-10 可向根 `package.json` 只添加实际消费的 system-admin/domain-web-domain `workspace:*`，机械同步匹配的 root lock specifiers 与两个 T-10 package importers；可在 `src/router/adminManifestRegistry.ts` 仅追加 system domain/manifest、selected IDs 和七个迁移切片所需的最小 typed host ports，并在对应 registry test 只追加 selected/unselected 断言。既有 identity/demo/workflow composition、resolver、全局 permission、workspace/catalog、root scripts、外部版本、既有 resolutions 和无关 lock 节点继续只读；Lead 必须证明 T-09 `public/user/**` 字节不变。
- `DEV-T10-002`：本 change 不修改后端，七组既有 API 中的 `PUT/DELETE` 路径和方法必须原样迁移并由 transport tests 锁定；不得发明平行 endpoint 或单边规范化为 POST。例外仅在后端协调迁移并同步全部消费者后到期。

### 已采用的低影响假设

- 领域包内部按 identity/organization/menu 子模块组织，外部只从根或显式 public exports 消费。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 七组治理 API/models/pages、manifest、public seam 实现、兼容入口 | identity access、platform、T-09 user seam | dict/config/OSS/message、devtools、全局 guard |

## 4. 要构建什么

授权管理员在 admin 中管理不同 Client 下的用户、角色、菜单和组织关系；workflow 的用户选择继续经稳定 public seam 工作。未授权操作不呈现且服务端拒绝仍是最终权威，client-web 未选择 system-admin 时不注册管理页面。

## 5. 实现契约

- **入口/输入输出：** SystemIdentity services、UserQueryPort implementation、system manifest；现有 DTO/VO 输入输出保持。
- **公共接口变化：** system-admin exports；T-09 public seam 保持兼容；旧 src facade。
- **不变量/数据流：** web -> domain -> HttpClient；workflow -> public user port -> system service；无 system->workflow 反向边。
- **失败行为：** 无权限、跨 Client 不一致、用户查询失败明确返回且不使用其他 Client 数据。
- **兼容/安全：** 用户数据最小暴露；权限判断不扩大；旧 component keys/endpoints 不变。

## 6. 执行路线

1. 盘点七组 API/views、权限、keys 和 workflow/devtools 消费。
2. 在不改 T-09 签名下实现 system user public port，补合同测试。
3. 提取 system identity/organization domain 并建立 public exports。
4. 迁移页面与 manifest，旧路径改 facade。
5. 运行授权/Client 失败矩阵、graph/type/build 和 Lead E2E，记录 Gate G4。

## 7. 路径访问契约

- **可写：** system-admin 新包除 T-09 public user 目录、七组旧 API/views、专用 E2E。
- **只读：** T-09 seam、identity 核心、全局 permission；不得写 workflow 或 T-07 shared paths。
- **共享路径：** 无；对 public seam 的实现通过包内 adapter 接入，不修改其 owner 文件。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | service/page E2E | CRUD/query + workflow user contract | 管理路径和跨域查询可用 | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-10.md</Path>` |
| 失败路径 | auth/Client tests | 无权限、错误 Client、查询失败 | 不越权、不串 Client、错误稳定 | 同上 |
| 回归 | graph/type/build | no cycle/deep；双 App build | client 未选不注册，workflow 不回归 | 同上 |

- **Workspace checks：** source-worktree/current-workspace 运行 unit、architecture、lint、typecheck、双 build。
- **E2E disposition：** required：授权管理、Client 和跨域用户查询是安全相关全链路。
- **E2E owner/environment：** Lead / parent-candidate 或 current-workspace；覆盖用户/角色/菜单关键路径和拒绝场景。
- **Integration evidence：** source commit、parent before、candidate/result SHA、Gate G4。

## 9. 发布、迁移与恢复

- **迁移/兼容：** public adapter -> domain -> pages -> facades；旧入口到 T-15。
- **监控/回滚：** 403、Client mismatch、user port、route errors；manifest 可回切旧 views。
- **批准点/收缩：** Gate G4 后 T-11；旧 imports 由 T-15 清零，T-09 public contract 不删除。

## 10. 验收标准

- [ ] `AC-009/AC-010`：system-admin 职责可追踪且只在选中 App 注册。
- [ ] `AC-017/AC-018`：权限/超管与 Client 隔离语义保持。
- [ ] `AC-021`：全门禁和 E2E 真实通过，失败保留旧入口。
- [ ] 跨域合同、commit/candidate/result SHA 写入 `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-10.md</Path>`。
