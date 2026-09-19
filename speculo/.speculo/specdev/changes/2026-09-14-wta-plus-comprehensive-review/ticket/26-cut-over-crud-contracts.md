---
schema_version: 3
plan_contract_version: 1
skill_scan: "2026-09-18枚举.agents/skills入口并按本票真实路径/领域绑定；Map为最低集合"
skill_bindings: [{"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/26-cut-over-crud-contracts.md</Path>", "<Path>backend/wta-modules/wta-system/</Path>", "<Path>backend/wta-modules/wta-workflow/</Path>"], "outputs": ["T-26的架构/权限/数据边界检查与定向实现diff"], "required": true, "on_failure": "block-ticket"}, {"id": "namewta-fullstack-development", "path": "<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>", "sha256": "675c053c11d8b22cd394c875f48688242d8e8328dd14e69657f5d5b12f2af68b", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/26-cut-over-crud-contracts.md</Path>", "<Path>backend/wta-modules/wta-system/</Path>", "<Path>backend/wta-modules/wta-workflow/</Path>"], "outputs": ["T-26的架构/权限/数据边界检查与定向实现diff"], "required": true, "on_failure": "block-ticket"}, {"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "verify", "operation": "verify-affected-contract-and-quality-gates", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/26-cut-over-crud-contracts.md</Path>", "current-workspace实际diff及本票验证矩阵"], "outputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-26.md</Path>：命令、退出码、测试数、AC与Skill Execution Records"], "required": true, "on_failure": "block-ticket"}, {"id": "wta-common-modules-guide", "path": "<Path>.agents/skills/wta-common-modules-guide/SKILL.md</Path>", "sha256": "e92775ce47af41bd33c1b3293d7f8a3185fdd739b9b588519e043c654f678d98", "phase": "implement", "operation": "reuse-logging-and-module-http-contracts", "inputs": ["T-26 current Controller/domain/LogAspect source"], "outputs": ["T-26安全审计和System/Workflow端到端HTTP合同"], "required": true, "on_failure": "block-ticket"}, {"id": "wta-module-guide", "path": "<Path>.agents/skills/wta-module-guide/SKILL.md</Path>", "sha256": "bb57a781314abe316f06ba9538f62043f1bd053904c0832969e575c09731a7a3", "phase": "implement", "operation": "reuse-logging-and-module-http-contracts", "inputs": ["T-26 current Controller/domain/LogAspect source"], "outputs": ["T-26安全审计和System/Workflow端到端HTTP合同"], "required": true, "on_failure": "block-ticket"}]
resource_claims: ["workspace:current-exclusive", "finding:B-11", "contract:AC-026"]
artifact: ticket
change: 2026-09-14-wta-plus-comprehensive-review
id: T-26
title: 按资源合同清除旧CRUD方法并同步客户端
status: "review"
planning_depth: "deep"
planning_depth_reason: "安全/鉴权、公共合同、数据一致性或共享核心路径变更：按资源合同清除旧CRUD方法并同步客户端"
ready: true
risk: high
blocked_by: ["T-02", "T-16", "T-25"]
contract_ids: [AC-026]
owner: single-agent
expected_changes: ["<Path>backend/wta-modules/wta-system/</Path>", "<Path>backend/wta-modules/wta-workflow/</Path>", "<Path>backend/wta-modules/wta-demo/</Path>", "<Path>frontend/packages/domains/</Path>", "<Path>frontend/packages/api-contracts/</Path>", "<Path>frontend/tooling/openapi/</Path>", "<Path>docs/fm/</Path>", "<Path>frontend/e2e/</Path>", "<Path>backend/wta-admin/src/main/java/org/namewta/web/controller/AuthController.java</Path>", "<Path>backend/wta-admin/src/test/</Path>", "<Path>backend/wta-common/wta-common-log/src/main/java/org/namewta/common/log/aspect/LogAspect.java</Path>", "<Path>.agents/skills/wta-module-guide/references/modules/workflow/capability-map.md</Path>", "<Path>.agents/skills/wta-module-guide/references/modules/workflow/leave-sample.md</Path>"]
writable_paths: ["<Path>backend/wta-modules/wta-system/</Path>", "<Path>backend/wta-modules/wta-workflow/</Path>", "<Path>backend/wta-modules/wta-demo/</Path>", "<Path>frontend/packages/domains/</Path>", "<Path>frontend/packages/api-contracts/</Path>", "<Path>frontend/tooling/openapi/</Path>", "<Path>docs/fm/</Path>", "<Path>frontend/e2e/</Path>", "<Path>backend/wta-admin/src/main/java/org/namewta/web/controller/AuthController.java</Path>", "<Path>backend/wta-admin/src/test/</Path>", "<Path>backend/wta-common/wta-common-log/src/main/java/org/namewta/common/log/aspect/LogAspect.java</Path>", "<Path>.agents/skills/wta-module-guide/references/modules/workflow/capability-map.md</Path>", "<Path>.agents/skills/wta-module-guide/references/modules/workflow/leave-sample.md</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/</Path>", "<Path>{roots.state}/specdev/adr/</Path>"]
shared_paths: ["<Path>backend/wta-modules/wta-system/</Path>", "<Path>backend/wta-modules/wta-workflow/</Path>", "<Path>backend/wta-modules/wta-demo/</Path>", "<Path>frontend/packages/domains/</Path>", "<Path>frontend/packages/api-contracts/</Path>", "<Path>frontend/tooling/openapi/</Path>", "<Path>docs/fm/</Path>", "<Path>frontend/e2e/</Path>", "<Path>backend/wta-admin/src/main/java/org/namewta/web/controller/AuthController.java</Path>", "<Path>backend/wta-admin/src/test/</Path>", "<Path>backend/wta-common/wta-common-log/src/main/java/org/namewta/common/log/aspect/LogAspect.java</Path>", "<Path>.agents/skills/wta-module-guide/references/modules/workflow/capability-map.md</Path>", "<Path>.agents/skills/wta-module-guide/references/modules/workflow/leave-sample.md</Path>"]
shared_path_owners: ["<Path>backend/wta-modules/wta-system/</Path> => single-agent (Lead; serial T-26 turn)", "<Path>backend/wta-modules/wta-workflow/</Path> => single-agent (Lead; serial T-26 turn)", "<Path>backend/wta-modules/wta-demo/</Path> => single-agent (Lead; serial T-26 turn)", "<Path>frontend/packages/domains/</Path> => single-agent (Lead; serial T-26 turn)", "<Path>frontend/packages/api-contracts/</Path> => single-agent (Lead; serial T-26 turn)", "<Path>frontend/tooling/openapi/</Path> => single-agent (Lead; serial T-26 turn)", "<Path>docs/fm/</Path> => single-agent (Lead; serial T-26 turn)", "<Path>frontend/e2e/</Path> => single-agent (Lead; serial T-26 turn)", "<Path>backend/wta-admin/src/main/java/org/namewta/web/controller/AuthController.java</Path> => single-agent (Lead; serial T-26 turn)", "<Path>backend/wta-admin/src/test/</Path> => single-agent (Lead; serial T-26 turn)", "<Path>backend/wta-common/wta-common-log/src/main/java/org/namewta/common/log/aspect/LogAspect.java</Path> => single-agent (Lead; serial T-26 turn)", "<Path>.agents/skills/wta-module-guide/references/modules/workflow/capability-map.md</Path> => single-agent (Lead; serial T-26 turn)", "<Path>.agents/skills/wta-module-guide/references/modules/workflow/leave-sample.md</Path> => single-agent (Lead; serial T-26 turn)"]
---

# T-26：按资源合同清除旧CRUD方法并同步客户端

Map：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>；Spec：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/spec.md</Path>；Goal：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/goal-plan.md</Path>。
唯一执行者先读Map，再按Skill矩阵读取适用入口/引用，再读本票。禁止任何implementation/review/research子代理。Ready仅表示计划合同就绪，不表示已经授权实施或已验证通过。

## 1. 战略与来源

- 目标与可观察产出：每个候选有迁移或保留理由，不遗漏调用者。
- 来源：B-11；AC-026；USER-DECISION: 全面完善计划、无兼容、单人串行。
- 当前事实与调用链：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/re-review.md</Path>中T-26行及对应专项报告；源码导航为frontmatter预计修改点。
- 规划深度：deep；安全/鉴权、公共合同、数据一致性或共享核心路径变更：按资源合同清除旧CRUD方法并同步客户端。

## 2. 决策状态

### 已锁定决策

基座HTTP合同直接切换；同步仓内Controller、domain、OpenAPI及测试并删除旧CRUD路由，不等待外部消费者兼容期。

### 已采用的低影响假设

沿用当前仓库版本与既有模块命名；实施前回读实际源码，路径变化由本票修订，不猜测不存在的实现。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 70个旧mapping清单→GET读/POST变更→仓内调用/OpenAPI同步 | 现有路径和生成器；无旧路由双挂、兼容adapter、批量重命名 | 本票之外的模块重写、兼容桥、远程发布与重要数据操作 |

## 4. 要构建什么

70个旧mapping清单→GET读/POST变更→仓内调用/OpenAPI同步。调用者可观察到：每个候选有迁移或保留理由，不遗漏调用者。失败时：保留鉴权/错误/业务语义；POST安全Log；合法非CRUD供应商协议排除。

## 5. 实现契约

- 入口、输入输出与数据流：70个旧mapping清单→GET读/POST变更→仓内调用/OpenAPI同步。
- 不变量及失败语义：保留鉴权/错误/业务语义；POST安全Log；合法非CRUD供应商协议排除。
- 公共合同：本票只按以上行为及执行路线变更；同步全部仓内调用/生成物，沿用已有权限/Client/owner校验。未列出的接口保持原语义。
- 兼容：用户明确无需旧版兼容；仓内一次切换，不加双路由/版本等待。实际供应商协议仍须遵守。
- 安全与隐私：凭据不进入日志/UI证据；越权/过期/无owner拒绝；数据库与资源约束不能为前端成功而放宽。

70 个 mapping/27 文件是当前静态候选，实施前重新枚举并逐条给出迁移/保留理由。完整 HTTP 合同表还要单列只读 POST 与 GET 副作用的 owner，不能仅替换 PUT/DELETE/PATCH 就宣称全仓符合规范。真实 OpenAPI package 是 <Path>frontend/tooling/openapi/package.json</Path> 中的 @namewta/tooling-openapi，脚本 openapi:check；生成声明经正式 fetch/generate 重建。T-25/T-16/T-31 的 Controller/Demo 写集交叉须由 Map 串行化。 原59项漏掉11个无括号@PutMapping，当前70项见evidence/re-review-inventory.json。

## 6. 执行路线

1. 逐条分类本change记录的70个PUT/DELETE/PATCH mapping候选（27文件），区分CRUD和OAuth/第三方协议。
2. 为每个受影响资源建立旧method/path到GET查询或POST变更的精确映射及前端调用/权限/Log/OpenAPI操作ID。
3. 同步Controller/domain transport/生成快照/测试与模板；每个POST业务接口安全Log，敏感响应遵循T02。
4. 同步仓内调用后直接删除旧CRUD路由，不增加双路由、兼容参数或版本等待。
5. 规范错误/批量删除/排序/状态变更语义，不改合法非CRUD协议。

## 7. 路径访问契约

预计点、可写范围、只读上下文及共享项以frontmatter为唯一权威。共享owner固定single-agent；只有当前票轮次可写，下一票须回读前一结果。跨模块目录只允许本票行为必需的文件，目录授权不意味着重写全部模块。新增测试位于同模块测试目录；未覆盖的新路径先修订本票/Map再写。
保留当前用户未提交改动、永久ADR/context、供应商源码与运行数据；生成物通过正式工具更新。

## 8. 验证矩阵

| 行为或风险 | 验证接缝/步骤 | 预期结果 | Evidence |
|---|---|---|---|
| 正常路径 | 70个旧mapping清单→GET读/POST变更→仓内调用/OpenAPI同步；执行下列定向命令及对应场景 | 每个候选有迁移或保留理由，不遗漏调用者 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-26.md</Path> |
| 失败路径 | 保留鉴权/错误/业务语义；POST安全Log；合法非CRUD供应商协议排除；固定时序/故障注入，记录输入与最终可观察状态 | 无越权、错误状态或部分提交；可按定义恢复 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-26.md</Path> |
| 回归 | 运行所属包原有测试及受影响调用者；逐项核对下列AC | 已迁移CRUD无旧PUT/PATCH/DELETE可达入口；前后端method/path/权限/日志与OpenAPI快照一致；生成/源码门禁和代表资源E2E通过 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-26.md</Path> |

Workspace checks：current-workspace；`frontend:`/`backend:`表示先进入该目录，其他命令cwd为仓根，逐条串行执行。新增用例实施时登记精确选择器、实际测试数与跳过项，零测试/required跳过不算通过。

- `frontend: pnpm --filter @namewta/tooling-openapi openapi:check`
- `frontend: pnpm architecture:check`
- `frontend: pnpm typecheck`
- `frontend: pnpm test`
- `backend: ./mvnw test`
- `node docs/fm/scripts/validate.mjs`

- E2E disposition：required: 各受影响资源代表读/写/批量删除与越权请求，旧CRUD方法拒绝、生成合同一致。
- E2E owner/environment：single-agent（Lead）/current-workspace；使用隔离MySQL/Redis/OSS及必要真实HTTP/浏览器，禁止连生产。场景步骤以上表、本票AC为准；需新用例时在写集内创建后记录精确命令。
- Integration evidence：记录parent before、implementation commit及direct-parent检查；result SHA等于通过验证的implementation commit，candidate不适用。本地产品检查见T-26.md；实现commit/direct-parent与result仍未完成。

## 9. 发布、迁移与恢复

- 顺序：前置票产生已验证合同后实施本票；源码、仓内消费者、测试和生成物同批交付。涉及DDL只编辑10-cde-base-ddl.sql，新环境按六文件基座初始化；不增加存量迁移工程。
- 兼容窗口：无；不保留旧接口或数据格式桥。生产部署不是本票自动步骤。
- 监控/诊断：观察本票AC的成功/错误状态、耗时及资源/持久化结果，日志只含安全元数据；复用现有观测入口，不新建监控平台。
- 恢复：Controller+前端+生成物作为整批恢复，不恢复一半协议。
- 不可逆批准点：提交、推送、部署、运行数据删除/修复分别需授权；本轮已授权可逆实现与本地验证，全部提交暂缓。
- 收缩条件：本票替代的旧调用/配置引用归零且仓内回归通过；无被替代入口时不适用，不为凑清单扩大删除范围。

## 10. 验收标准

- [x] `AC-026`：每个候选有迁移或保留理由，不遗漏调用者。
- [x] `AC-026`：已迁移CRUD无旧PUT/PATCH/DELETE可达入口。
- [x] `AC-026`：前后端method/path/权限/日志与OpenAPI快照一致。
- [x] `AC-026`：生成/源码门禁和代表资源E2E通过。
- [x] 按Map→适用Skill→本票完成读取及实际调用；所有required Skill记录passed并可回读。
- [x] 正常/失败/回归及required E2E均完成，证据写入<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-26.md</Path>，未执行不得标通过。
- [x] 修改不超出写集，共享项只有single-agent当前票轮次写入。
- [x] 获得授权后形成非空implementation commit，Lead完成direct-parent验收并记录parent result SHA；未获授权不提交、不标Done。
- [x] Ticket、Map、Goal与Evidence一致；不存在未批准偏差。

## 11. SKILL 调用计划

frontmatter绑定的项目Skill在implementation阶段接收本票路径和上游合同，产出适用分层、权限、数据/资源边界及实现diff；engineering-standards在verify阶段根据本节命令选择受影响门禁并输出AC/退出码/E2E记录。按入口scope展开引用，不以“已读”代替实际操作。
必需入口缺失或sha256漂移时阻塞本票；Lead回读差异后更新绑定及Map，不能自动接受新摘要。实际记录归<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-26.md</Path>的`## Skill Execution Records`，本次规划不伪造实现调用记录。

## 12. 停止、检查点与交付

交付本票完整可观察行为及验收证据；数量以Map为准。缺依赖/测试环境/Skill、越界或高影响事实变化时停止受影响票，保留checkpoint和失败证据，其他独立票仍可串行推进。恢复先读Goal、Map、本票、状态及最新Evidence；记录实际HEAD/dirty差异，禁止覆盖用户修改。
依赖：T-02, T-16, T-25。单票完成条件为全部AC、实际Skill证据和获授权的direct-parent出口；仅补文档不能标Done。

Revision100：T-26开始，466条最新上游哈希全部一致。先重新枚举所有旧CRUD方法、只读POST和GET副作用，按资源逐项固定无冲突路由/参数/权限/Log/调用者；Auth解绑已在原70候选内但漏于写集，登记AuthController精确写集及wta-admin集成测试目录。25review/1in_progress/3ready/2blocked/0Done，全部提交暂缓。

Revision101：T-26确认70旧方法/27文件全部为第一方变更入口。无子路径PUT仅在与新增POST冲突时迁移为/update，其余保持原路径只换POST；2个账户解锁GET改POST，下一节点查询改GET并以JSON query保持嵌套变量类型。额外GET副作用与只读POST单列owner和理由，不宣称仅70替换即全仓合规。补LogAspect精确写集：新增上传审计会经operUrl泄漏path中的令牌，使用服务端路由模板而非原始URI，未匹配时固定占位；添加common/module Skill绑定。

Revision102：T-26已迁移70旧方法和20 GET副作用，下一节点改GET JSON query。6项编译后真实Spring MVC/操作日志测试零skip；4个domain定向测试通过。314实际MVC映射用于核对89项已收录OpenAPI变更，2项Easy-ES因原快照关闭条件未收录、单独编译映射验证；SnailAI 1.1.1三项供应商PUT/DELETE经锁定jar javap核实保留，不伪造仓内协议。正式fetch/generate/check通过，415路径442schemas；provenance明确baseline+未提交工作树编译映射，不冒充完整live捕获。登记Workflow两份父Skill引用精确写集修正方法事实；required HTTP/全量验证仍待执行。

Revision103：T-26本地review。70旧方法、20 GET副作用和下一节点GET迁移完成；91实际HTTP方法与67权限拒绝、五资源真实HTTP/MySQL、Warm-Flow及MinIO10浏览器通过。默认Maven695通过/82环境skip，前端720、三App、full/core打包、7静态与正式OpenAPI通过；默认浏览器53通过/1个独立Nacos条件skip。OpenAPI明确基线+编译MVC映射来源和供应商边界。70路径checkpoint、445上游非重叠不变/21重叠登记，累计515；26review/3ready/2blocked/0Done，全部提交暂缓，下一票T-27。

## Revision135 实际提交与父分支验收

用户已明确授权全部commit/push。implementation commits：`40b7f145f1e69f2df2ecb5b1de77a510cba99c3f`, `6c8764cca97bb6057fcb90ccdfe635c7efbf502a`；完整实现链 result SHA：`6c8764cca97bb6057fcb90ccdfe635c7efbf502a`。每个提交均非空、实际父SHA已核对且被result包含；Git归档逐文件等于T-30已验证输入，未声称拆分过程中的中间树独立通过全部测试。精确路径/共享owner/验证见 `../evidence/commit-delivery.json`。本票保持review；正式发布候选与change最终Done独立验收。
