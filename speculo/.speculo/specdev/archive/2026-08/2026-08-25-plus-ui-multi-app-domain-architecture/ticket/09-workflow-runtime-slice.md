---
schema_version: 3
artifact: ticket
change: 2026-08-25-plus-ui-multi-app-domain-architecture
id: T-09
title: 迁移工作流运行时并建立 system user 公共接缝
status: done
planning_depth: deep
planning_depth_reason: 任务、实例和请假跨多个页面组件并依赖 system 用户模型，需要先定义跨领域公开合同避免循环依赖
ready: true
risk: high
blocked_by: [T-08]
contract_ids: [AC-006, AC-007, AC-009, AC-010, AC-021]
owner: native:/root/t01
expected_changes: ["<Path>plus-ui-namewta/packages/domains/workflow/**</Path>", "<Path>plus-ui-namewta/packages/web-domains/workflow/**</Path>", "<Path>plus-ui-namewta/packages/domains/system-admin/README.md</Path>", "<Path>plus-ui-namewta/packages/domains/system-admin/package.json</Path>", "<Path>plus-ui-namewta/packages/domains/system-admin/tsconfig.json</Path>", "<Path>plus-ui-namewta/packages/domains/system-admin/public/user/**</Path>", "<Path>plus-ui-namewta/src/api/workflow/**</Path>", "<Path>plus-ui-namewta/src/views/workflow/**</Path>", "<Path>plus-ui-namewta/src/components/Process/**</Path>", "<Path>plus-ui-namewta/src/components/UserSelect/**</Path>", "<Path>plus-ui-namewta/src/components/TreePanel/**</Path>", "<Path>plus-ui-namewta/src/router/adminManifestRegistry.ts</Path>", "<Path>plus-ui-namewta/pnpm-lock.yaml</Path>"]
writable_paths: ["<Path>plus-ui-namewta/packages/domains/workflow/**</Path>", "<Path>plus-ui-namewta/packages/web-domains/workflow/**</Path>", "<Path>plus-ui-namewta/packages/domains/system-admin/README.md</Path>", "<Path>plus-ui-namewta/packages/domains/system-admin/package.json</Path>", "<Path>plus-ui-namewta/packages/domains/system-admin/tsconfig.json</Path>", "<Path>plus-ui-namewta/packages/domains/system-admin/public/user/**</Path>", "<Path>plus-ui-namewta/src/api/workflow/**</Path>", "<Path>plus-ui-namewta/src/views/workflow/**</Path>", "<Path>plus-ui-namewta/src/components/Process/**</Path>", "<Path>plus-ui-namewta/src/components/UserSelect/**</Path>", "<Path>plus-ui-namewta/src/components/TreePanel/**</Path>", "<Path>plus-ui-namewta/src/router/adminManifestRegistry.ts</Path>", "<Path>plus-ui-namewta/e2e/workflow-runtime.spec.ts</Path>", "<Path>plus-ui-namewta/pnpm-lock.yaml</Path>"]
read_only_paths: ["<Path>plus-ui-namewta/packages/domains/identity-access/**</Path>", "<Path>plus-ui-namewta/src/api/system/user/**</Path>", "<Path>plus-ui-namewta/src/api/system/dept/**</Path>", "<Path>plus-ui-namewta/package.json</Path>"]
shared_paths: ["<Path>plus-ui-namewta/packages/domains/system-admin/README.md</Path>", "<Path>plus-ui-namewta/packages/domains/system-admin/package.json</Path>", "<Path>plus-ui-namewta/packages/domains/system-admin/tsconfig.json</Path>", "<Path>plus-ui-namewta/packages/domains/system-admin/public/user/**</Path>", "<Path>plus-ui-namewta/src/router/adminManifestRegistry.ts</Path>", "<Path>plus-ui-namewta/pnpm-lock.yaml</Path>"]
shared_path_owners: ["<Path>plus-ui-namewta/packages/domains/system-admin/README.md</Path> => T-10 package owner; T-09 scoped activation writer for the minimum public user seam only (DEV-T09-001)", "<Path>plus-ui-namewta/packages/domains/system-admin/package.json</Path> => T-10 package owner; T-09 scoped activation writer for the minimum public user seam only (DEV-T09-001)", "<Path>plus-ui-namewta/packages/domains/system-admin/tsconfig.json</Path> => T-10 package owner; T-09 scoped activation writer for the minimum public user seam only (DEV-T09-001)", "<Path>plus-ui-namewta/packages/domains/system-admin/public/user/**</Path> => T-09 public seam owner; T-10 consumer/implementation owner must preserve exports", "<Path>plus-ui-namewta/src/router/adminManifestRegistry.ts</Path> => T-08 composition owner and T-15 convergence owner; T-09 may only inject FileUpload/close-current-page host ports and expose the composed workflow runtime for compatibility facades (DEV-T09-002)", "<Path>plus-ui-namewta/pnpm-lock.yaml</Path> => T-03 policy owner; T-09 scoped writer for the system-admin importer and workflow dependency specifier only (DEV-T09-001)"]
---

# Ticket T-09: 迁移工作流运行时并建立 system user 公共接缝

- **工件：** Ticket `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/ticket/09-workflow-runtime-slice.md</Path>`；Map `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/tickets-map.md</Path>`；Spec `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/spec.md</Path>`；Evidence `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-09.md</Path>`。

## 1. 战略与来源

- **目标/产出：** task、instance、leave、Process 和用户选择完成 workflow 纵切；workflow 只依赖 system-admin 的最小 user public seam。
- **来源：** `US-004`、`US-005`、`US-012`、`AC-006`、`AC-007`、`AC-009`、`AC-010`、`AC-021`、`ADR-002`、`ADR-003`。
- **当前事实：** workflow 直接引用 system user types 和通用 Process/UserSelect/TreePanel，盲目搬迁会产生 domain 循环或 UI 下沉。
- **Planning Depth 原因：** 跨领域公共合同、复杂审批交互和共享组件迁移需要明确 owner、兼容与恢复。

## 2. 决策状态

### 已锁定决策

- workflow domain 仅从 `system-admin/public/user` 导入 `UserSummary/UserQueryPort` 等最小合同，不 deep import system 实现。
- Vue Process/UserSelect/TreePanel 属 workflow web-domain 或 web-kit 合适边界，不进入无头 domain。
- T-09 拥有该最小 seam；T-10 复用并在 system 内部实现，不破坏它。
- `DEV-T09-001`：T-09 可激活 `@namewta/domain-system-admin` 的 `README.md`、`package.json`、`tsconfig.json` 与 `public/user/**`，并只在 workflow manifest 添加该包的 `workspace:*` 依赖及机械同步 lock 中 system-admin importer/workflow specifier。根 `package.json`、admin registry、workspace/catalog、root scripts、外部版本、既有 resolution 和无关 lock 节点继续只读。
- `DEV-T09-002`：双轴审查证明真实附件上传、完成后关闭当前页和旧 Process 兼容外观必须由 admin composition 注入宿主能力。T-09 可在 `src/router/adminManifestRegistry.ts` 仅注入根 `FileUpload` 组件、`closeCurrentPage` 端口并公开同一个 workflow runtime 给旧路径 facade；根上传实现、resolver/route assembler、既有 manifest 选择和其他宿主语义保持只读。修复还必须恢复实例变量更新、请假时间范围/自动天数、办理完成导航、撤销原因、候选人逗号字符串查询合同、管理员干预独立动作策略和对应 E2E，不得以手填 ID、全按钮夹具或 README 声明替代行为。
- `DEV-T09-003`：迁移其余 Process 历史/流程图组件时，web-domain 不能合法导入根 Token、OSS 与 download 实现。T-09 可在同一 admin composition 只追加 `chartUrl`、`resolveAttachments`、`downloadAttachment` 宿主端口，并迁移六个组件后把全部旧 Process 路径变为 facade；根 Token/OSS/download 实现与其他 runtime 行为只读。
- `DEV-T09-004`：前三次 candidate 均未推进 `main@262c06d38be0d123e4ec07d8905fc13f87a8fcab`，结果依次为 full E2E `18/29`、workflow `6/11`、workflow `8/11`；根因从双 App 环境与 locator/fixture 缺口收敛为候选过滤同 tick props 竞态、错误提示重复匹配和不变请求触发 repeat-submit。只有 source 追加修复、双轴复审通过后才允许第 4 次 candidate；第 4 次必须先过 workflow `11/11`，再过 full `29/29` 与完整 Gate，方可推进父分支。

### 已采用的低影响假设

- 当前 workflow API schema 和 component keys 保持，局部组件最终归属依据消费者数量决定但必须满足依赖方向。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| task/instance/leave APIs/pages、workflow common、Process/user selection、system user seam | T-08 definition exports、identity/platform、后端 workflow | system 用户管理 UI、其他 system 能力、流程后端改造 |

## 4. 要构建什么

已授权用户从待办进入任务，查看流程实例、选择办理人并完成任务或请假流程；所有页面由 workflow manifest 注册。用户查询经 system public port，未选择 workflow 的 App 不包含运行时页面，旧 admin 路径持续可用。

## 5. 实现契约

- **入口/输入输出：** WorkflowTask/Instance/Leave services、UserQueryPort、workflow manifest；业务/用户查询输入，任务状态/视图或稳定错误输出。
- **公共接口变化：** workflow runtime exports 和 system user public seam；旧 workflow/component 路径 facade。
- **不变量/数据流：** route -> workflow web -> domain service -> HttpClient；user selection -> public port -> system adapter；无反向 system->workflow 依赖。
- **失败行为：** 用户查询/任务提交失败保留表单状态并明确提示；权限拒绝不渲染授权操作。
- **兼容/安全：** backend businessId/task semantics 不变；用户最小字段，不泄露额外个人信息；旧入口至 T-15。

## 6. 执行路线

1. 盘点 workflow runtime 行为、component keys 和 system user 真实字段依赖。
2. 先定义最小 system user public seam 和合同测试，拒绝 deep import。
3. 提取 workflow runtime domain APIs/models/services。
4. 迁移 pages/components 到 web-domain/web-kit 并扩展 manifest；由 admin composition 注入真实附件上传和关闭当前页宿主端口。
5. 建立旧 Process/UserSelect/view/API 路径 facade，恢复实例变量更新、请假时间与天数、办理完成导航、撤销原因、候选人查询和管理员干预策略，运行 task/leave/user failure tests、graph、双 build 和 Lead E2E。
6. Gate G3 后开放 T-10/T-12/T-14 扇出。

## 7. 路径访问契约

- **可写：** workflow 新包/旧路径、三个组件、system user public seam、专用 E2E；**只读：** identity、旧 system user/dept、根配置。
- **共享路径：** system user public seam 唯一 owner `T-09`；`DEV-T09-001` 允许最小包根激活和对应 lock 机械更新，T-10 不得破坏其 exports。
- **保留或不动：** system 管理页面和全局 auth/router。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | service/component/E2E | task/instance/leave tests；Lead 办理、真实附件、退回附件、变量更新、请假保存/发起/完成导航与撤销路径；候选人 URL 与管理员干预动作断言 | 流程和用户选择完整可用且无行为降级 | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-09.md</Path>` |
| 失败路径 | API/permission tests | 用户查询失败、提交失败、无权限 | 保留状态、明确错误、不越权 | 同上 |
| 回归 | graph/type/build | no deep/cycle；双 App build | seam 单向且旧路径兼容 | 同上 |

- **Workspace checks：** source-worktree/current-workspace 运行 unit/component、architecture、lint、typecheck、双 build。
- **E2E disposition：** required：审批任务、用户选择、网络和动态路由是高价值跨边界行为。
- **E2E owner/environment：** Lead / parent-candidate 或 current-workspace；覆盖待办到完成及失败保留。
- **Integration evidence：** source commit、parent before、candidate/result SHA、Gate G3 与扇出批准。

## 9. 发布、迁移与恢复

- **迁移/兼容：** public seam expand -> runtime migrate -> facades；旧入口到 T-15。
- **监控/回滚：** task API、user query、missing key 和 E2E；manifest 可回切旧 views。
- **批准点：** Gate G3 通过才扇出；**收缩条件：** T-10 合同测试通过且 T-15 扫描零旧 imports。

## 10. 验收标准

- [x] `AC-006/AC-007`：workflow 无头层无 Web 依赖，只从 system public export 消费且无循环。
- [x] `AC-009/AC-010`：workflow 完整职责可追踪且 runtime 只由选中 manifest 注册。
- [x] `AC-021`：unit/component/graph/build/E2E 有真实结果，失败保留旧入口。
- [x] shared seam、commit/candidate/result SHA 写入 `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-09.md</Path>`。
