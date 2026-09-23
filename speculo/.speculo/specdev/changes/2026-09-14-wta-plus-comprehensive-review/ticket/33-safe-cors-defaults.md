---
schema_version: 3
plan_contract_version: 1
skill_scan: "2026-09-23已枚举.agents/skills全部入口；命中scope读取入口，common入口变化已复核，历史Skill Evidence不改写"
skill_bindings: [{"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/33-safe-cors-defaults.md</Path>", "<Path>backend/wta-common/wta-common-web/src/main/java/org/namewta/common/web/config/ResourcesConfig.java</Path>", "<Path>backend/wta-common/wta-common-web/src/main/java/org/namewta/common/web/config/properties/CorsProperties.java</Path>"], "outputs": ["T-33的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "wta-common-modules-guide", "path": "<Path>.agents/skills/wta-common-modules-guide/SKILL.md</Path>", "sha256": "d7b7e105c37499e0e0f8ad9b1e2dbf379100df5b6a47e0d6affb04483f8b162c", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/33-safe-cors-defaults.md</Path>", "<Path>backend/wta-common/wta-common-web/src/main/java/org/namewta/common/web/config/ResourcesConfig.java</Path>", "<Path>backend/wta-common/wta-common-web/src/main/java/org/namewta/common/web/config/properties/CorsProperties.java</Path>"], "outputs": ["T-33的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "verify", "operation": "verify-affected-contract-and-quality-gates", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/33-safe-cors-defaults.md</Path>", "当前diff与验证矩阵"], "outputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-33-replan-2026-09-23.md</Path>"], "required": true, "on_failure": "block-ticket"}]
resource_claims: ["workspace:current-exclusive", "config:cors", "http:origin-trust", "contract:AC-033"]
artifact: "ticket"
change: "2026-09-14-wta-plus-comprehensive-review"
id: "T-33"
title: "恢复精确来源的 CORS 默认边界"
status: "in_progress"
kind: "bug"
planning_depth: "deep"
planning_depth_reason: "公共合同/事务/安全/数据及恢复边界"
ready: true
risk: "high"
blocked_by: []
contract_ids: ["AC-033"]
owner: "single-agent"
expected_changes: ["<Path>backend/wta-common/wta-common-web/src/main/java/org/namewta/common/web/config/ResourcesConfig.java</Path>", "<Path>backend/wta-common/wta-common-web/src/main/java/org/namewta/common/web/config/properties/CorsProperties.java</Path>", "<Path>backend/wta-common/wta-common-web/src/test/</Path>", "<Path>backend/wta-admin/src/main/resources/application.yml</Path>", "<Path>backend/wta-admin/src/main/resources/application-prod.yml</Path>", "<Path>backend/wta-admin/src/main/resources/application-local.example.yml</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/sso/</Path>", "<Path>backend/README.md</Path>"]
writable_paths: ["<Path>backend/wta-common/wta-common-web/src/main/java/org/namewta/common/web/config/ResourcesConfig.java</Path>", "<Path>backend/wta-common/wta-common-web/src/main/java/org/namewta/common/web/config/properties/CorsProperties.java</Path>", "<Path>backend/wta-common/wta-common-web/src/test/</Path>", "<Path>backend/wta-admin/src/main/resources/application.yml</Path>", "<Path>backend/wta-admin/src/main/resources/application-prod.yml</Path>", "<Path>backend/wta-admin/src/main/resources/application-local.example.yml</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/sso/</Path>", "<Path>backend/README.md</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/source.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ADR.md</Path>", "<Path>{roots.state}/specdev/adr/</Path>", "<Path>{roots.state}/specdev/changes/2026-09-21-wta-sso-oidc-upgrade/</Path>"]
shared_paths: ["<Path>backend/wta-common/wta-common-web/src/main/java/org/namewta/common/web/config/ResourcesConfig.java</Path>", "<Path>backend/wta-common/wta-common-web/src/main/java/org/namewta/common/web/config/properties/CorsProperties.java</Path>", "<Path>backend/wta-common/wta-common-web/src/test/</Path>", "<Path>backend/wta-admin/src/main/resources/application.yml</Path>", "<Path>backend/wta-admin/src/main/resources/application-prod.yml</Path>", "<Path>backend/wta-admin/src/main/resources/application-local.example.yml</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/sso/</Path>", "<Path>backend/README.md</Path>"]
shared_path_owners: ["<Path>backend/wta-common/wta-common-web/src/main/java/org/namewta/common/web/config/ResourcesConfig.java</Path> => single-agent (Lead; exclusive current workspace; T-33 turn only)", "<Path>backend/wta-common/wta-common-web/src/main/java/org/namewta/common/web/config/properties/CorsProperties.java</Path> => single-agent (Lead; exclusive current workspace; T-33 turn only)", "<Path>backend/wta-common/wta-common-web/src/test/</Path> => single-agent (Lead; exclusive current workspace; T-33 turn only)", "<Path>backend/wta-admin/src/main/resources/application.yml</Path> => single-agent (Lead; exclusive current workspace; T-33 turn only)", "<Path>backend/wta-admin/src/main/resources/application-prod.yml</Path> => single-agent (Lead; exclusive current workspace; T-33 turn only)", "<Path>backend/wta-admin/src/main/resources/application-local.example.yml</Path> => single-agent (Lead; exclusive current workspace; T-33 turn only)", "<Path>backend/wta-admin/src/test/java/org/namewta/test/sso/</Path> => single-agent (Lead; exclusive current workspace; T-33 turn only)", "<Path>backend/README.md</Path> => single-agent (Lead; exclusive current workspace; T-33 turn only)"]
---

# T-33：恢复精确来源的 CORS 默认边界

Map：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>；Spec：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/spec.md</Path>；Goal：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/goal-plan.md</Path>。
唯一执行者先完整读Map→命中项目Skill入口/按scope引用→本票与上游。用户已激活Goal，允许gpt-6-sol/xhigh子代理。T-33产品写集由cors_audit独占，Lead负责状态与验收；current串行，无新worktree。

## 1. 战略与来源

- 来源：R64-S-02；AC-033；本轮用户请求与<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/source.md</Path>。
- 当前事实：公共 application.yml 与 ResourcesConfig 允许通配 origin pattern 携带 credentials；Java 默认空列表被 YAML 覆盖。
- 可观察产出：默认同源访问正常；跨域仅接受显式受信 Origin。生产带凭证通配配置拒绝启动；本地开发复用 Vite 同源代理。

## 2. 决策状态

### 已锁定决策

保留工程分层、Client/权限、资源owner、安全日志、真实供应商协议和唯一六SQL基座。用户已授权本地实施、提交与验收。

### 已确认方案

2026-09-23用户已逐项接受D-002—009并确认整体共识（LOG-010—018）；本票按已接受ADR定稿。 完整决定及来源以<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ADR.md</Path>为准。

### 已采用的低影响假设

沿用当前模块与测试基座；测试样本为隔离合成数据，不作为生产容量或SLO。

### 执行前置

G整体共识已确认；执行前仍需复核当前HEAD/归属、实际实施与commit授权及必要测试环境。Ready不代替Goal激活。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 本票可观察产出及所列消费者、验证、文档 | 既有wta-api/common、模块模式、事务/权限/生命周期与测试 | 无关模块重写、新队列/锁平台、真实数据修复、远程发布及相邻OIDC实现 |

## 4. 要构建什么

默认同源访问正常；跨域仅接受显式受信 Origin。生产带凭证通配配置拒绝启动；本地开发复用 Vite 同源代理。 正常、失败、越权和竞争路径按本票验收断言共同交付，不把前后端或测试分成无价值空票。

## 5. 实现契约

- 入口与输入输出：真实 Servlet/CorsFilter 测试与 Spring profile 绑定。
- 外部行为：默认同源访问正常；跨域仅接受显式受信 Origin。生产带凭证通配配置拒绝启动；本地开发复用 Vite 同源代理。
- 不变量：current workspace单writer；UseCase→Service→DAO→Mapper；classic保留；公开数据只经wta-api；GET查询/POST变更且安全@Log。Notify外部I/O不在结果事务内，IN_APP按确认后的短事务合同处理。
- 失败边界：不吞SQL/HTTP/Provider错误，不将UNKNOWN当成功或盲目可重试；页面旧响应不覆盖新会话。具体负向断言见第8/10节。
- 兼容：沿用用户此前明确的基座仓内直接切换决定，同步真实消费者/生成物；不放宽第三方协议。公共API技能用于调用方与影响核对，不重新增加已被用户排除的兼容桥。
- 安全：仅隔离合成测试；secret不进日志/截图/证据。对象/Client授权在后端实施，页面隐藏不替代权限。

## 6. 执行路线

1. 用真实 CorsFilter HTTP 用例固定恶意 Origin 被反射的红灯。
2. 将公共 YAML 收敛为空/显式列表；local 示例仅精确开发 Origin，prod 拒绝 credentials+通配。
3. 同步 CorsProperties 校验及 ResourcesConfig；保持预检、头/方法和同源请求规则。
4. 覆盖可信/不可信/无 Origin/null/带凭证与不带凭证请求；分别测试 profile 配置绑定。
5. 文档分清应用 CORS 与桶 CORS，检查 SSO 当前明确来源不回归。

## 7. 路径访问契约

frontmatter为预计点、硬写集与共享owner权威。目录写集仅授权本票行为所需文件；新增测试在声明根内，新增生产类须符合已有层次。不存在的新文件为计划创建，不声称已实现。共享物理/语义资源由single-agent在本票轮次独占；不同票不并行，跨change冲突仅暂停相关分支。

本票状态和Evidence仅由Lead写当前change；永久ADR/context及相邻SSO change只读。越界先修订Ticket/Map，禁止先改后报。

## 8. 验证矩阵

| 场景 | 接缝/步骤 | 预期 | Evidence |
|---|---|---|---|
| 正常 | 真实 Servlet/CorsFilter 测试与 Spring profile 绑定 | 恶意 Origin 无许可响应头，受信来源与同源开发可用 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-33-replan-2026-09-23.md</Path> |
| 失败/竞争 | 生产通配+credentials 明确配置失败，无按请求 Origin 自动加入白名单 | 明确失败/安全恢复，无伪成功、越权及部分提交 | 同上，记录故障注入与状态 |
| 回归 | 现有同域测试＋消费者＋适用静态门禁 | SSO Cookie/预检回归通过，桶权限不因后端 CORS 改动而扩大 | 同上，记录测试数/skip/源码 |

命令在仓根执行，`cd backend`表示该条命令切cwd；每条独立运行。以下为实施期命令，本轮未执行：

- `cd backend && ./mvnw -pl wta-common/wta-common-web,wta-admin -am test`

- Workspace checks：current-workspace，所列命令加命中工程Skill质量门禁。
- E2E disposition：required: 真实 Servlet/CorsFilter 测试与 Spring profile 绑定。
- E2E owner/environment：single-agent（Lead）/current-workspace；真实MySQL/Redis/MinIO必须为本任务隔离资源，必要服务缺失则阻塞对应验收。
- 真实服务启用方法与零skip要求：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/verification.md</Path>；新增用例必须保存精确选择器与实际计数，不能只运行mock或test list。
- Integration evidence：非空implementation commit、parent before、clean exact HEAD/tree时点的direct-parent和适用E2E、不可变result及父链；required模式不适用，不创建candidate worktree。

## 9. 发布、迁移与恢复

- 迁移顺序：同批修改源码、消费者、测试及生成合同；无需新增数据库迁移。涉及已有库时必须Tag差异、备份、隔离演练，不重放基座。
- 兼容窗口：基座仓内同步切换，无未声明双写/双协议；外部现有协议保持。
- 监控：记录本票可观察失败/状态/耗时及资源数量，不记录敏感正文；不新增监控平台。
- 恢复：保存上个不可变候选及失败证据；停止受影响任务再核对外部副作用。不得通过恢复已披露secret、放宽权限或重发UNKNOWN恢复。
- 不可逆批准：产品commit/父分支更新及远程push、部署、轮换/真实数据操作、归档分别核对本轮授权。本地实施提交已授权，环境操作按核实的用户范围，归档另批。
- 收缩条件：旧消费者/废弃字段/不必要配置引用清零且新合同验证通过；不适用的删除不人为增加。

## 10. 验收标准

- [ ] `AC-033`：恶意 Origin 无许可响应头，受信来源与同源开发可用。
- [ ] `AC-033`：生产通配+credentials 明确配置失败，无按请求 Origin 自动加入白名单。
- [ ] `AC-033`：SSO Cookie/预检回归通过，桶权限不因后端 CORS 改动而扩大。
- [ ] 实际调用已绑定Skill，记录摘要/输入/步骤/输出；不是只“读过”。
- [ ] 正常、失败、回归和required E2E有当前候选证据，未运行不勾选。
- [ ] 写集、共享owner、合同和生成物一致；无未批准偏差。
- [ ] 真实commit/direct-parent/result出口已满足或按Goal对历史无需新实施票作有证据的取消裁决。

## 11. SKILL 调用计划

frontmatter每个必需绑定在implement阶段输入本票、真实调用方和diff，按scope执行约束检查与实现；verify阶段由engineering-standards执行适用门禁。实际Skill Execution Records写<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-33-replan-2026-09-23.md</Path>，包含id/phase/operation/sha256/status/evidence。入口摘要变化先读diff并重新绑定，不改旧历史记录。当前规划仅完成元数据/入口及相关规范路由，未伪造实施passed。

## 12. 停止、检查点与交付

交付本票完整可观察行为，数量以Map为准，不能以样例替代。缺高影响决定、必需Skill/引用/测试，或owner冲突，停止该票和依赖闭包；无依赖票仅在已获执行授权后继续。保留HEAD、diff、已跑命令、失败类别、待完成动作；相同失败无新证据或达到3次集成尝试先复盘。验收后回交Goal，全部票done仍不等于change可归档。
