---
schema_version: 3
plan_contract_version: 1
skill_scan: "2026-09-23已枚举.agents/skills全部入口；命中scope读取入口，common入口变化已复核，历史Skill Evidence不改写"
skill_bindings: [{"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/02-unify-log-redaction.md</Path>", "<Path>backend/wta-common/wta-common-log/</Path>", "<Path>backend/wta-common/wta-common-web/</Path>"], "outputs": ["T-02的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "wta-common-modules-guide", "path": "<Path>.agents/skills/wta-common-modules-guide/SKILL.md</Path>", "sha256": "d7b7e105c37499e0e0f8ad9b1e2dbf379100df5b6a47e0d6affb04483f8b162c", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/02-unify-log-redaction.md</Path>", "<Path>backend/wta-common/wta-common-log/</Path>", "<Path>backend/wta-common/wta-common-web/</Path>"], "outputs": ["T-02的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "verify", "operation": "verify-affected-contract-and-quality-gates", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/02-unify-log-redaction.md</Path>", "当前diff与验证矩阵"], "outputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-02-replan-2026-09-23.md</Path>"], "required": true, "on_failure": "block-ticket"}]
resource_claims: ["workspace:current-exclusive", "contract:AC-002"]
artifact: "ticket"
change: "2026-09-14-wta-plus-comprehensive-review"
id: "T-02"
title: "消除HTTP与操作日志中的凭据副本"
status: "in_progress"
kind: "bug"
planning_depth: "deep"
planning_depth_reason: "公共合同/事务/安全/数据及恢复边界"
ready: true
risk: "high"
blocked_by: []
contract_ids: ["AC-002"]
owner: "single-agent"
expected_changes: ["<Path>backend/wta-common/wta-common-log/</Path>", "<Path>backend/wta-common/wta-common-web/</Path>", "<Path>backend/wta-common/wta-common-core/src/main/java/org/namewta/common/core/constant/SystemConstants.java</Path>", "<Path>backend/wta-modules/wta-sso/src/main/java/org/namewta/sso/controller/anonymous/SsoOAuthController.java</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/service/impl/SysOperLogServiceImpl.java</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/</Path>", "<Path>backend/wta-common/wta-common-json/src/</Path>"]
writable_paths: ["<Path>backend/wta-common/wta-common-log/</Path>", "<Path>backend/wta-common/wta-common-web/</Path>", "<Path>backend/wta-common/wta-common-core/src/main/java/org/namewta/common/core/constant/SystemConstants.java</Path>", "<Path>backend/wta-modules/wta-sso/src/main/java/org/namewta/sso/controller/anonymous/SsoOAuthController.java</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/service/impl/SysOperLogServiceImpl.java</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/</Path>", "<Path>backend/wta-common/wta-common-json/src/</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/source.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ADR.md</Path>", "<Path>{roots.state}/specdev/adr/</Path>", "<Path>{roots.state}/specdev/changes/2026-09-21-wta-sso-oidc-upgrade/</Path>"]
shared_paths: ["<Path>backend/wta-common/wta-common-log/</Path>", "<Path>backend/wta-common/wta-common-web/</Path>", "<Path>backend/wta-common/wta-common-core/src/main/java/org/namewta/common/core/constant/SystemConstants.java</Path>", "<Path>backend/wta-modules/wta-sso/src/main/java/org/namewta/sso/controller/anonymous/SsoOAuthController.java</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/service/impl/SysOperLogServiceImpl.java</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/</Path>", "<Path>backend/wta-common/wta-common-json/src/</Path>"]
shared_path_owners: ["<Path>backend/wta-common/wta-common-log/</Path> => single-agent (Lead; exclusive current workspace; T-02 turn only; delegated sole product writer cors_audit)", "<Path>backend/wta-common/wta-common-web/</Path> => single-agent (Lead; exclusive current workspace; T-02 turn only; delegated sole product writer cors_audit)", "<Path>backend/wta-common/wta-common-core/src/main/java/org/namewta/common/core/constant/SystemConstants.java</Path> => single-agent (Lead; exclusive current workspace; T-02 turn only; delegated sole product writer cors_audit)", "<Path>backend/wta-modules/wta-sso/src/main/java/org/namewta/sso/controller/anonymous/SsoOAuthController.java</Path> => single-agent (Lead; exclusive current workspace; T-02 turn only; delegated sole product writer cors_audit)", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/service/impl/SysOperLogServiceImpl.java</Path> => single-agent (Lead; exclusive current workspace; T-02 turn only; delegated sole product writer cors_audit)", "<Path>backend/wta-admin/src/test/java/org/namewta/test/</Path> => single-agent (Lead; exclusive current workspace; T-02 turn only; delegated sole product writer cors_audit)", "<Path>backend/wta-common/wta-common-json/src/</Path> => single-agent (Lead; exclusive current workspace; T-02 turn only; delegated sole product writer cors_audit)"]
---

# T-02：消除HTTP与操作日志中的凭据副本

Map：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>；Spec：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/spec.md</Path>；Goal：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/goal-plan.md</Path>。
唯一执行者先完整读Map→命中项目Skill入口/按scope引用→本票与上游。用户已激活Goal并授权gpt-6-sol/xhigh子代理；current/main单产品writer cors_audit，无新worktree，Lead治理/提交/真实验收。

## 1. 战略与来源

- 来源：R-01；AC-002；本轮用户请求与<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/source.md</Path>。
- 当前事实：日志脱敏保持；公开local配置披露由T-32新增整改，不能用日志测试替代凭据轮换。
- 可观察产出：canary不出现在HTTP sink、OperLogEvent、数据库或错误日志

## 2. 决策状态

### 已锁定决策

保留工程分层、Client/权限、资源owner、安全日志、真实供应商协议和唯一六SQL基座。用户已授权本地实施、逐票提交与direct-parent；外部发布及真实数据处置不由此推断。

### 已确认方案

既有合同保持；本轮重新评审与验收；日志脱敏保持；公开local配置披露由T-32新增整改，不能用日志测试替代凭据轮换。 完整决定及来源以<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ADR.md</Path>为准。

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

canary不出现在HTTP sink、OperLogEvent、数据库或错误日志 正常、失败、越权和竞争路径按本票验收断言共同交付，不把前后端或测试分成无价值空票。

## 5. 实现契约

- 入口与输入输出：required: 用唯一凭据canary调用签发/失败接口，检查HTTP sink、OperLogEvent与数据库均不含明文。
- 外部行为：canary不出现在HTTP sink、OperLogEvent、数据库或错误日志
- 不变量：current workspace单writer；UseCase→Service→DAO→Mapper；classic保留；公开数据只经wta-api；GET查询/POST变更且安全@Log。Notify外部I/O不在结果事务内，IN_APP按确认后的短事务合同处理。
- 失败边界：不吞SQL/HTTP/Provider错误，不将UNKNOWN当成功或盲目可重试；页面旧响应不覆盖新会话。具体负向断言见第8/10节。
- 兼容：沿用用户此前明确的基座仓内直接切换决定，同步真实消费者/生成物；不放宽第三方协议。公共API技能用于调用方与影响核对，不重新增加已被用户排除的兼容桥。
- 安全：仅隔离合成测试；secret不进日志/截图/证据。对象/Client授权在后端实施，页面隐藏不替代权限。

## 6. 执行路线

1. 回读本票历史Evidence和实际实现提交，使用当前源码核对本票验收合同；旧施工步骤仅在before快照保存，不重复实施。
2. 比较历史候选与当前写集；对后续提交或新票触及的行为逐一标记需要重跑的测试。
3. 执行本票正常/失败/回归及E2E要求；零用例或required skip不算通过，结果写新的带日期证据，不覆盖原始记录。
4. 若发现退化，先在本票写集内固定红灯；超出范围或与新票重叠时由Lead修订owner，禁止重复改动。
5. 核验历史非空提交和父链；缺少真实clean exact-HEAD证明不得事后补造。按Goal历史票关闭程序处置。

## 7. 路径访问契约

frontmatter为预计点、硬写集与共享owner权威。目录写集仅授权本票行为所需文件；新增测试在声明根内，新增生产类须符合已有层次。不存在的新文件为计划创建，不声称已实现。共享物理/语义资源由single-agent在本票轮次独占；不同票不并行，跨change冲突仅暂停相关分支。

本票状态和Evidence仅由Lead写当前change；永久ADR/context及相邻SSO change只读。越界先修订Ticket/Map，禁止先改后报。

## 8. 验证矩阵

| 场景 | 接缝/步骤 | 预期 | Evidence |
|---|---|---|---|
| 正常 | required: 用唯一凭据canary调用签发/失败接口，检查HTTP sink、OperLogEvent与数据库均不含明文 | canary不出现在HTTP sink、OperLogEvent、数据库或错误日志 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-02-replan-2026-09-23.md</Path> |
| 失败/竞争 | 普通字段/操作者/耗时/失败状态仍可观测 | 明确失败/安全恢复，无伪成功、越权及部分提交 | 同上，记录故障注入与状态 |
| 回归 | 现有同域测试＋消费者＋适用静态门禁 | 签名、加解密、SSE与正常token响应保持正确；旧日志处置与凭据轮换另有批准记录，本票不自动删历史数据 | 同上，记录测试数/skip/源码 |

命令在仓根执行，`cd backend`表示该条命令切cwd；每条独立运行。以下为实施期命令，本轮未执行：

- `cd backend && ./mvnw -pl wta-common/wta-common-json,wta-common/wta-common-web,wta-common/wta-common-log,wta-modules/wta-sso,wta-modules/wta-system,wta-admin -am test`

- Workspace checks：current-workspace，所列命令加命中工程Skill质量门禁。
- E2E disposition：required: 用唯一凭据canary调用签发/失败接口，检查HTTP sink、OperLogEvent与数据库均不含明文。
- E2E owner/environment：single-agent（Lead）/current-workspace；真实MySQL/Redis/MinIO必须为本任务隔离资源，必要服务缺失则阻塞对应验收。
- 真实服务启用方法与零skip要求：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/verification.md</Path>；新增用例必须保存精确选择器与实际计数，不能只运行mock或test list。
- Integration evidence：非空implementation commit、parent before、clean exact HEAD/tree时点的direct-parent和适用E2E、不可变result及父链；required模式不适用，不创建candidate worktree。

## 9. 发布、迁移与恢复

- 迁移顺序：保持历史已交付行为；仅对当前复验发现的真实退化实施最小修复。已有数据不得重放基座。
- 兼容窗口：基座仓内同步切换，无未声明双写/双协议；外部现有协议保持。
- 监控：记录本票可观察失败/状态/耗时及资源数量，不记录敏感正文；不新增监控平台。
- 恢复：保存上个不可变候选及失败证据；停止受影响任务再核对外部副作用。不得通过恢复已披露secret、放宽权限或重发UNKNOWN恢复。
- 不可逆批准：产品commit/父分支更新及远程push、部署、轮换/真实数据操作、归档分别核对本轮授权。本地实施、逐票提交与direct-parent已授权；推送、部署、真实数据修改及归档须独立授权。
- 收缩条件：旧消费者/废弃字段/不必要配置引用清零且新合同验证通过；不适用的删除不人为增加。

## 10. 验收标准

- [ ] `AC-002`：canary不出现在HTTP sink、OperLogEvent、数据库或错误日志。
- [ ] `AC-002`：普通字段/操作者/耗时/失败状态仍可观测。
- [ ] `AC-002`：签名、加解密、SSE与正常token响应保持正确。
- [ ] `AC-002`：旧日志处置与凭据轮换另有批准记录，本票不自动删历史数据。
- [ ] 实际调用已绑定Skill，记录摘要/输入/步骤/输出；不是只“读过”。
- [ ] 正常、失败、回归和required E2E有当前候选证据，未运行不勾选。
- [ ] 写集、共享owner、合同和生成物一致；无未批准偏差。
- [ ] 真实commit/direct-parent/result出口已满足或按Goal对历史无需新实施票作有证据的取消裁决。

## 11. SKILL 调用计划

frontmatter每个必需绑定在implement阶段输入本票、真实调用方和diff，按scope执行约束检查与实现；verify阶段由engineering-standards执行适用门禁。实际Skill Execution Records写<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-02-replan-2026-09-23.md</Path>，包含id/phase/operation/sha256/status/evidence。入口摘要变化先读diff并重新绑定，不改旧历史记录。当前规划仅完成元数据/入口及相关规范路由，未伪造实施passed。

## 12. 停止、检查点与交付

交付本票完整可观察行为，数量以Map为准，不能以样例替代。缺高影响决定、必需Skill/引用/测试，或owner冲突，停止该票和依赖闭包；无依赖票仅在已获执行授权后继续。保留HEAD、diff、已跑命令、失败类别、待完成动作；相同失败无新证据或达到3次集成尝试先复盘。验收后回交Goal，全部票done仍不等于change可归档。

## 历史实现保留及本轮处置

历史implementation commit：`4cfb7eea819c142ccdbcc29004a86868b86e2551`；历史result：`6c8764cca97bb6057fcb90ccdfe635c7efbf502a`。提交存在及祖先关系见<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/legacy-ticket-audit.json</Path>。

<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-02.md</Path>及其引用日志是历史证据，本轮未重跑业务测试。原Ticket全文见<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/replan-2026-09-23-before/ticket/02-unify-log-redaction.md</Path>，不得按旧“尚未实现/提交暂缓”描述重复执行。

旧计划把所有票result设为同一整批提交且当前worktrees为空，不满足现行逐票验收记录合同；Lead须查原始记录。不能补造当时clean状态，不能为关闭历史票创建空commit。若现代码满足合同且无需新实现，经当前行为证据及明确处置可cancelled并保留AC由T-30覆盖；否则按真实修复重新形成产品提交，既有历史证据仍不删。

## revision161 当前实际缺口与最小修复

base `c16966167526f9b6ab6eb213265034b3bbe53e46`。T37已在3a87bf7通过187+13+67零skip，current单产品writer转交cors_audit实施T02；Lead独占治理、提交、隔离真实验收。

当前真实链：UserLoginSuccessListener把已签发tokenValue放入UserOnlineDTO.tokenId，前端在线设备操作把它放入 /monitor/online/{tokenId} 与 /monitor/online/myself/{tokenId}；SysLogFilter原始path和GlobalExceptionHandler原始URI会复制凭据。LogAspect目前使用匹配路由模板，未证实OperLogEvent/DB已有该泄漏，不扩大断言。历史T02不是无需施工票，保留旧实现/证据，新增真实修复提交及当前验收；历史canonical先按原字节保存在evidence/T-02-history-before-2026-09-23.md，原引用日志不变；验收完成后当前canonical使用验证器要求的evidence/T-02.md，replan入口指向它，不补造旧时点。

复用已有LogSanitizer提供共享安全路径策略，HTTP sink和异常日志使用；必要时操作日志统一策略但保留路由模板。精确处理上述正常凭据路径及实际context/path语义，保留 /monitor/online/list、普通路由、操作者、耗时、失败状态。不得以改前端/HTTP接口/会话存储规避日志缺陷，不发动对任意用户可控元数据的无限脱敏；X-Request-Id格式加固仅建议，不是本票新增blocker。

先取得真实可观察日志红灯，再最小实现。当前候选验收需真实签发或既有真实token生产链的canary通过正常在线操作URL进入HTTP，检查HTTP sink、异常日志、OperLogEvent和真实MySQL行零凭据且审计非空，正常响应仍保留可用token。隔离测试若采用替身必须明确边界，不把任意塞入metadata的字符串冒称生产凭据链；原有签名/加解密/SSE/正文边界消费者回归。仅生产代码白名单路径的最小修复，测试均在现有admin测试根及common根，无需改业务API。

允许测试读取合成MySQL密码的子进程环境，避免JVM系统属性/日志泄漏；环境root/app凭据不进入argv/XML/Evidence。真实服务与Maven由Lead协调，不并行构建。不得删除历史日志/业务表或轮换凭据；G-security-external已独立关闭，用户AI/qcloud撤销确认不重新索要。治理原始日志按字节保留含Maven尾空格，产品与治理源文件的diff-check独立通过，不修改原日志制造全量空格绿灯。

### revision161 同一在线会话响应字段闭环

补充当前源码：SysUserOnline.tokenId未经JSON忽略直接随GET /monitor/online/list及GET /monitor/online返回，LogSanitizer敏感名当前不含tokenid，且两条GET不省略响应日志，因此同一个有效token还会经在线列表HTTP响应正文复制。既有LogSanitizer字段策略需补tokenId，仅改变日志副本，业务响应仍保留可用于正常设备操作的tokenId；canary验收覆盖列表响应→正常操作URL，而非仅路径静态替换。未扩大写集或改业务API。
