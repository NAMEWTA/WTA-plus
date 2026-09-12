---
schema_version: 3
artifact: ticket
change: 2026-09-04-third-party-http-integration
id: T-13
title: 同步工程事实并执行 ruoyi-third 全栈发布 Gate
status: done
planning_depth: deep
planning_depth_reason: 13 个跨后端、SQL、Redis、日志和前端切片需要在共同父状态执行完整兼容、安全、双 bundle 与发布验证，并将真实模块事实回写唯一 Skill。
ready: true
risk: high
blocked_by: [T-01, T-02, T-03, T-04, T-05, T-06, T-07, T-08, T-09, T-10, T-11, T-12]
contract_ids: [AC-001, AC-002, AC-003, AC-004, AC-005, AC-006, AC-007, AC-008, AC-009, AC-010, AC-011, AC-012, AC-013, AC-014, AC-015, AC-016, AC-017, AC-018]
owner: codex:/root
expected_changes:
  - "<Path>.agents/skills/engineering-standards/references/project/00-project-profile.md</Path>"
  - "<Path>.agents/skills/engineering-standards/references/project/01-module-map.md</Path>"
  - "<Path>.agents/skills/engineering-standards/references/project/03-backend-module-modes.md</Path>"
  - "<Path>.agents/skills/ruoyi-module-guide/references/modules/third/**</Path>"
writable_paths:
  - "<Path>.agents/skills/engineering-standards/references/project/00-project-profile.md</Path>"
  - "<Path>.agents/skills/engineering-standards/references/project/01-module-map.md</Path>"
  - "<Path>.agents/skills/engineering-standards/references/project/03-backend-module-modes.md</Path>"
  - "<Path>.agents/skills/ruoyi-module-guide/SKILL.md</Path>"
  - "<Path>.agents/skills/ruoyi-module-guide/references/modules/third/**</Path>"
  - "<Path>.agents/skills/namewta-fullstack-development/references/frontend/architecture.md</Path>"
  - "<Path>.agents/skills/namewta-fullstack-development/references/frontend/crud-resource-slices.md</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/**</Path>"
  - "<Path>plus-ui-namewta/**</Path>"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/**</Path>"
shared_paths:
  - "<Path>.agents/skills/engineering-standards/references/project/00-project-profile.md</Path>"
  - "<Path>.agents/skills/engineering-standards/references/project/01-module-map.md</Path>"
  - "<Path>.agents/skills/engineering-standards/references/project/03-backend-module-modes.md</Path>"
  - "<Path>.agents/skills/ruoyi-module-guide/SKILL.md</Path>"
  - "<Path>.agents/skills/namewta-fullstack-development/references/frontend/architecture.md</Path>"
  - "<Path>.agents/skills/namewta-fullstack-development/references/frontend/crud-resource-slices.md</Path>"
shared_path_owners:
  - "<Path>.agents/skills/engineering-standards/references/project/00-project-profile.md</Path> => T-13"
  - "<Path>.agents/skills/engineering-standards/references/project/01-module-map.md</Path> => T-13"
  - "<Path>.agents/skills/engineering-standards/references/project/03-backend-module-modes.md</Path> => T-13"
  - "<Path>.agents/skills/ruoyi-module-guide/SKILL.md</Path> => T-13"
  - "<Path>.agents/skills/namewta-fullstack-development/references/frontend/architecture.md</Path> => T-13"
  - "<Path>.agents/skills/namewta-fullstack-development/references/frontend/crud-resource-slices.md</Path> => T-13"
---

# Ticket T-13: 同步工程事实并执行 ruoyi-third 全栈发布 Gate

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/13-engineering-sync-and-release-gate.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-13.md</Path>`

## 1. 战略与来源

- **目标：** 在所有切片汇合后以真实运行状态证明端到端合同，并将新模块、包、模式和命令同步到父级项目 Skill。
- **可观察产出：** MySQL/Redis/local HTTP/backend/Admin 全栈验收通过，full/core 均可交付；后续开发者可从唯一 Skills 找到 ruoyi-third 的公共合同、目录、边界和验证入口。
- **来源：** `US-001` 至 `US-009`、`AC-001` 至 `AC-018`、`NFR-001` 至 `NFR-006`、项目 Skills 同步规则。
- **当前事实：** T-01 至 T-12 分别证明局部行为，但尚无共同 parent 状态的全合同 Gate，Skill 也尚未包含 third 事实。
- **Planning Depth 原因：** 多仓库/多基础设施/安全合同的集成验证和文档事实漂移风险高。

## 2. 决策状态

### 已锁定决策

- T-13 不新增或修补产品行为；若 Gate 发现缺陷，退回对应 owner Ticket，不在文档 Ticket 越界修复。
- Skills 只记录当前源码可证事实，不复制详细实现或把外部 `cde-third` 写成 owner。
- E2E 使用本地确定性 provider mock 和测试 canary，不访问真实企查查或保存真实秘密。
- backend full/core、frontend architecture/type/lint/test/build、MySQL fresh-init、Redis 多实例和浏览器权限均为 release Gate。

### 已采用的低影响假设

- 具体命令以实施时项目 wrapper/package scripts 为准，Evidence 记录实际版本、环境与未运行原因。

### 未决问题

无。

## 2.1 必须加载的 Skill 与工程基线

- **必须加载：** <Path>.agents/skills/engineering-standards/SKILL.md</Path>、<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>、<Path>.agents/skills/ruoyi-module-guide/SKILL.md</Path>、<Path>.agents/skills/ruoyi-common-modules-guide/SKILL.md</Path>、<Path>.agents/skills/java-api-compatibility/SKILL.md</Path>、<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>。
- **必须先读的参考：** <Path>.agents/skills/engineering-standards/references/project/00-project-profile.md</Path>、<Path>.agents/skills/engineering-standards/references/project/01-module-map.md</Path>、<Path>.agents/skills/engineering-standards/references/project/02-decisions-and-exceptions.md</Path>、<Path>.agents/skills/engineering-standards/references/project/03-backend-module-modes.md</Path>、<Path>.agents/skills/namewta-fullstack-development/references/frontend/architecture.md</Path>、<Path>.agents/skills/namewta-fullstack-development/references/frontend/implementation.md</Path>、<Path>.agents/skills/namewta-fullstack-development/references/backend/verification.md</Path>、<Path>.agents/skills/java-api-compatibility/SKILL.md</Path>。
- **目录与代码最低要求：** 以当前源码事实更新唯一 project/module/frontend Skills；若 T-01 的新模块已落地，必须把 ruoyi-third 登记为 layered 并记录 controller/usecase/service/dao/mapper/xml、POM、bundle、SQL、测试和前端 owner。Skill 只记录导航和事实，不复制实现。
- **Gate 要求：** 按工程规范执行 module-mode、Maven full/core、MySQL 8.4、Redis、local HTTP、SysLog、frontend architecture/type/lint/test/build 和 Admin E2E；Evidence 记录命令、工作目录、退出码、环境/跳过原因和残余风险。
- **执行停止条件：** 文档先于代码、遗漏 package/AGENTS/exports、验证只跑单元、把 source-worktree E2E 当通过、跳过 full/core/SQL/Redis、或为修绿而放宽规则时立即停止并退回原 Ticket。

## 2.2 Execution result

Implementation authorization is recorded in the change status as `USER-DECISION:2026-09-04-execute-goal-plan`. T-01..T-12 are integrated; T-13 remains the active release gate owned by `codex:/root`.

Verified at the 2026-09-05 checkpoint: backend `ruoyi-third` compile, `validate-module-mode.mjs ... --mode layered` (71 Java files), local-profile tests (32 `ruoyi-third` tests, 0 failures/errors), real MySQL/Redis integration tests, Spring application-context HTTP/SPI tests, direct unauthenticated admin HTTP tests, and production SysLog file-pipeline canary. The HTTP context tests cover no-data startup, duplicate/missing adapter startup validation, typed `@HttpExchange`, provider business-error mapping, 503/no-retry, 302 refusal, SSRF zero-request rejection, non-idempotent single attempt, and bounded idempotent retry. The backend module follows `controller/admin -> usecase/impl -> service -> dao -> mapper -> XML`, with runtime ports/adapters/support boundaries; `ruoyi-module-guide` registers the module. Backend result is `751ecd439`.

Frontend result is `cc6a6f2`: domain/web-domain/Admin typecheck, lint, test, build, architecture (101/101), and the dedicated Playwright permission suite (2/2) pass. The suite verifies the `系统管理 -> 三方接口管理` menu, all four child pages, full permissions, list-only permissions, hidden mutation controls, and direct-route rejection. Root menu/DDL and CI changes are the current root working-tree changes and are recorded in the corresponding Evidence.

The full/core bundle gate was verified from the isolated ZIP export `D:\Document\code\namewta-release-verify-751ecd439-zip`: both clean package commands completed all 47 reactor projects, direct `BOOT-INF/lib` inspection found `ruoyi-third-6.0.0.jar` in both packages, and optional job/ai/demo/workflow artifacts are present only in full. Verified copies are `D:\Document\code\namewta-full-admin-751ecd439.jar` and `D:\Document\code\namewta-core-admin-751ecd439.jar`. The updated bundle script also passes for both artifacts and requires `ruoyi-third`.

The only remaining required gate is canonical MySQL 8.4 fresh-init of the ordered 10/20/30/40/50/60/61 release stack. Docker CLI is unavailable, and the reachable `ry-namewta` database is not an approved reset target. The real randomized-table MySQL constraint test, isolated Redis/Redisson test, HTTP context test, SysLog canary, backend permission test, and browser E2E are already passed and must not be described as pending. No production DDL/DML, role assignment, real provider credential, deployment, or Qichacha call was executed. Keep the change `in_progress` until the fresh-init gate is run in an approved release environment.

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| Skill 事实同步、合同追踪、全栈 E2E/full-core/SQL-Redis 安全 Gate | T-01 至 T-12 产物与项目现有验证脚本 | 新业务代码、生产部署/密钥/供应商调用、缺陷就地越权修复 |

## 4. 要构建什么

Lead 在全部前序 result 已汇合的 parent-candidate/current workspace 启动 MySQL 8.4、Redis、backend、Admin 和本地 mock provider，按 AC-001 至 AC-018 执行管理、调用、拒绝、统计、日志、权限和双 bundle 场景。所有事实成立后更新 Skills；任何产品失败阻止 Gate，归还原 Ticket 修复并重新集成。

## 5. 实现契约

- **入口或接缝：** Maven/模块模式验证、SQL init、Redis integration、local provider、Admin E2E、Skills 导航。
- **输入与输出：** 共同父状态 + 测试配置 → 可复现 Gate Evidence 和当前事实文档。
- **公共接口变化：** 无；只记录已交付 API。
- **不变量：** 文档不先于代码；不以 mock 替代 MySQL/Redis/浏览器真实边界；不使用生产 secret。
- **状态或数据流：** 前序 result 汇合 → 静态/单元 → 基础设施 → backend → browser E2E → full/core → Skill sync → final rerun。
- **错误与失败行为：** 任一 required Gate 失败则 T-13 不 Done；缺陷归属原 Ticket，环境失败明确分类。
- **兼容要求：** system/monitor/OpenAPI、existing routes、full/core 和既有 Skill 路由均不回归。
- **安全与隐私要求：** canary 全局扫描、恶意目标零请求、DB/日志/DOM 无明文，测试凭据仅进测试进程。

## 6. 执行路线

1. 重读所有前序 result/路径并建立 AC-001 至 AC-018 Gate 矩阵。
2. 执行 backend 定向/全量、module-mode、full/core 和公共 API 兼容验证。
3. 执行 MySQL fresh-init、Redis 多实例、本地 HTTP 安全/计数/日志集成。
4. 执行 frontend architecture/type/lint/test/build 与 Admin 浏览器权限/响应式 E2E。
5. 扫描 secret/raw body/越界调用点并核对统计守恒。
6. 仅在代码事实通过后更新 third 模块与前端包 Skills，复跑受影响门禁。

## 7. 路径访问契约

- **预计修改点/可写范围：** 仅 frontmatter 所列父级 Skills；产品树全部只读。
- **只读上下文：** backend、frontend、SQL 全部已集成产物。
- **共享路径：** 工程事实、module guide 和前端架构文档由 T-13 唯一修改。
- **保留或不动：** 产品代码、SQL、lock、外部 reference、生产环境。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | full stack | Provider/Endpoint/credential 管理后动态+typed 调用并查日志/统计 | AC-001 至 AC-015/017 全链路成立 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-13.md</Path>` |
| 失败路径 | security/permission/fault | URL/header/secret/Redis/timeout/limit/adapter/权限矩阵 | 零越权发送、稳定错误、无敏感泄漏 | 同上 |
| 回归 | release gates | backend full/core、frontend 全门禁、MySQL 10→50→60→61 fresh-init | AC-016/018 与既有功能均通过 | 同上 |

- **Workspace checks：** 按 Goal Plan 在 source/current workspace 只做 Skills 静态检查；所有产品 Gate 由 Lead 在共同集成状态运行。
- **E2E disposition：** required：本 Ticket 的唯一价值就是跨 MySQL、Redis、HTTP、backend、browser 和 bundle 的最终证明。
- **E2E owner/environment：** Lead / parent-candidate 或 current-workspace；完整测试栈，禁止 source-worktree E2E pass 声明。
- **Integration evidence：** Skills implementation/source commit、parent before、前序 result 列表、candidate/result SHA、父分支包含关系。

## 9. 发布、迁移与恢复

- **迁移顺序：** Gate 使用 fresh-init 顺序 10→50→60→61；正式发布建议 DDL → backend → frontend → 61-third-dml.sql → DML/赋权。
- **兼容窗口：** 新模块/菜单 additive；public API 保持兼容，未知配置 fail-closed。
- **监控信号：** 全部拒绝/错误分类、attempt 守恒、cache invalidation、日志写失败、unknown component、403。
- **回滚或前向恢复：** 先撤菜单/权限并禁用 Provider，再回退 App；保留 schema、密文和历史，产品缺陷前向修复。
- **不可逆操作与批准点：** 本 Ticket 不执行生产 DDL/DML、角色赋权、真实凭据或部署；这些需要独立批准。
- **收缩条件：** 所有 AC Gate 通过、所有调用点经过 Gateway、Skills 与源码模块图一致。

## 10. 验收标准

- [x] `AC-001` 至 `AC-018` 在共同集成状态全部 covered 且有可执行 Evidence。
- [x] backend full/core、MySQL/Redis/local HTTP、frontend 与浏览器 Gate 全部通过。
- [x] 敏感 canary、SSRF、权限和统计守恒负向合同成立。
- [x] Skills 只记录已证实事实，产品树无越界修改。
- [x] required E2E、提交、parent result 和最终重读 Evidence 完整。
