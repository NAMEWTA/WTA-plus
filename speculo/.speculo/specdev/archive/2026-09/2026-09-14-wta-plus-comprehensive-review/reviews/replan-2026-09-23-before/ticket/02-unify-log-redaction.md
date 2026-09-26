---
schema_version: 3
plan_contract_version: 1
skill_scan: "2026-09-18枚举.agents/skills入口并按本票真实路径/领域绑定；Map为最低集合"
skill_bindings: [{"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/02-unify-log-redaction.md</Path>", "<Path>backend/wta-common/wta-common-log/</Path>", "<Path>backend/wta-common/wta-common-web/</Path>"], "outputs": ["T-02的架构/权限/数据边界检查与定向实现diff"], "required": true, "on_failure": "block-ticket"}, {"id": "wta-common-modules-guide", "path": "<Path>.agents/skills/wta-common-modules-guide/SKILL.md</Path>", "sha256": "e92775ce47af41bd33c1b3293d7f8a3185fdd739b9b588519e043c654f678d98", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/02-unify-log-redaction.md</Path>", "<Path>backend/wta-common/wta-common-log/</Path>", "<Path>backend/wta-common/wta-common-web/</Path>"], "outputs": ["T-02的架构/权限/数据边界检查与定向实现diff"], "required": true, "on_failure": "block-ticket"}, {"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "verify", "operation": "verify-affected-contract-and-quality-gates", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/02-unify-log-redaction.md</Path>", "current-workspace实际diff及本票验证矩阵"], "outputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-02.md</Path>：命令、退出码、测试数、AC与Skill Execution Records"], "required": true, "on_failure": "block-ticket"}]
resource_claims: ["workspace:current-exclusive", "finding:R-01", "contract:AC-002"]
artifact: ticket
change: 2026-09-14-wta-plus-comprehensive-review
id: T-02
title: 消除HTTP与操作日志中的凭据副本
status: "review"
planning_depth: "deep"
planning_depth_reason: "安全/鉴权、公共合同、数据一致性或共享核心路径变更：消除HTTP与操作日志中的凭据副本"
ready: true
risk: high
blocked_by: []
contract_ids: [AC-002]
owner: single-agent
expected_changes: ["<Path>backend/wta-common/wta-common-log/</Path>", "<Path>backend/wta-common/wta-common-web/</Path>", "<Path>backend/wta-common/wta-common-core/src/main/java/org/namewta/common/core/constant/SystemConstants.java</Path>", "<Path>backend/wta-modules/wta-sso/src/main/java/org/namewta/sso/controller/anonymous/SsoOAuthController.java</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/service/impl/SysOperLogServiceImpl.java</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/</Path>", "<Path>backend/wta-common/wta-common-json/src/</Path>"]
writable_paths: ["<Path>backend/wta-common/wta-common-log/</Path>", "<Path>backend/wta-common/wta-common-web/</Path>", "<Path>backend/wta-common/wta-common-core/src/main/java/org/namewta/common/core/constant/SystemConstants.java</Path>", "<Path>backend/wta-modules/wta-sso/src/main/java/org/namewta/sso/controller/anonymous/SsoOAuthController.java</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/service/impl/SysOperLogServiceImpl.java</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/</Path>", "<Path>backend/wta-common/wta-common-json/src/</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/</Path>", "<Path>{roots.state}/specdev/adr/</Path>"]
shared_paths: ["<Path>backend/wta-common/wta-common-log/</Path>", "<Path>backend/wta-common/wta-common-web/</Path>", "<Path>backend/wta-common/wta-common-core/src/main/java/org/namewta/common/core/constant/SystemConstants.java</Path>", "<Path>backend/wta-modules/wta-sso/src/main/java/org/namewta/sso/controller/anonymous/SsoOAuthController.java</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/service/impl/SysOperLogServiceImpl.java</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/</Path>"]
shared_path_owners: ["<Path>backend/wta-common/wta-common-log/</Path> => single-agent (Lead; serial T-02 turn)", "<Path>backend/wta-common/wta-common-web/</Path> => single-agent (Lead; serial T-02 turn)", "<Path>backend/wta-common/wta-common-core/src/main/java/org/namewta/common/core/constant/SystemConstants.java</Path> => single-agent (Lead; serial T-02 turn)", "<Path>backend/wta-modules/wta-sso/src/main/java/org/namewta/sso/controller/anonymous/SsoOAuthController.java</Path> => single-agent (Lead; serial T-02 turn)", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/service/impl/SysOperLogServiceImpl.java</Path> => single-agent (Lead; serial T-02 turn)", "<Path>backend/wta-admin/src/test/java/org/namewta/test/</Path> => single-agent (Lead; serial T-02 turn)"]
---

# T-02：消除HTTP与操作日志中的凭据副本

Map：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>；Spec：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/spec.md</Path>；Goal：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/goal-plan.md</Path>。
唯一执行者先读Map，再按Skill矩阵读取适用入口/引用，再读本票。禁止任何implementation/review/research子代理。Ready仅表示计划合同就绪，不表示已经授权实施或已验证通过。

## 1. 战略与来源

- 目标与可观察产出：canary不出现在HTTP sink、OperLogEvent、数据库或错误日志。
- 来源：R-01；AC-002；USER-DECISION: 全面完善计划、无兼容、单人串行。
- 当前事实与调用链：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/re-review.md</Path>中T-02行及对应专项报告；源码导航为frontmatter预计修改点。
- 规划深度：deep；安全/鉴权、公共合同、数据一致性或共享核心路径变更：消除HTTP与操作日志中的凭据副本。

## 2. 决策状态

### 已锁定决策

ADR-0025已禁止泄漏；新的默认日志保留策略见ADR-CR-002当前计划合同。

### 已采用的低影响假设

沿用当前仓库版本与既有模块命名；实施前回读实际源码，路径变化由本票修订，不猜测不存在的实现。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| HTTP/操作日志入口→脱敏副本→事件→日志/数据库 | common-json及现有日志adapter；不建新模块、不清理历史数据 | 本票之外的模块重写、兼容桥、远程发布与重要数据操作 |

## 4. 要构建什么

HTTP/操作日志入口→脱敏副本→事件→日志/数据库。调用者可观察到：canary不出现在HTTP sink、OperLogEvent、数据库或错误日志。失败时：畸形JSON输出安全摘要；敏感签发响应不记录正文；业务原始字节不变。

## 5. 实现契约

- 入口、输入输出与数据流：HTTP/操作日志入口→脱敏副本→事件→日志/数据库。
- 不变量及失败语义：畸形JSON输出安全摘要；敏感签发响应不记录正文；业务原始字节不变。
- 公共合同：本票只按以上行为及执行路线变更；同步全部仓内调用/生成物，沿用已有权限/Client/owner校验。未列出的接口保持原语义。
- 兼容：用户明确无需旧版兼容；仓内一次切换，不加双路由/版本等待。实际供应商协议仍须遵守。
- 安全与隐私：凭据不进入日志/UI证据；越权/过期/无owner拒绝；数据库与资源约束不能为前端成功而放宽。

两个日志模块目前都依赖 common-json；日志副本策略优先放入已有 <Path>backend/wta-common/wta-common-json/src/</Path>，由 common-log/web 调用，不令 common-log 反向依赖 common-web。对嵌套 JSON、数组、form、重复 query、非法 JSON 和异常日志分别验收；在截断前脱敏，失败输出安全摘要。OAuth code 按路由处理，不全局删除业务 code；签名原始字节与业务响应不受日志转换影响。

## 6. 执行路线

1. 先固定换票响应access_token进入OperLogEvent的红灯，以及原queryString泄漏canary的红灯。
2. 立即令凭据签发接口不记录响应正文；保留审计元数据。
3. 由现有common拥有一个日志副本策略，HTTP/操作日志adapter复用；删除原始query输出和未经策略处理的response序列化。
4. 按OAuth路由上下文处理code/verifier/redirectUri，不全局删除业务code字段；处理form/header/非法JSON。
5. 跨事件到数据库落点验证，确保安全修复未改变Servlet原始字节或业务响应。

## 7. 路径访问契约

预计点、可写范围、只读上下文及共享项以frontmatter为唯一权威。共享owner固定single-agent；只有当前票轮次可写，下一票须回读前一结果。跨模块目录只允许本票行为必需的文件，目录授权不意味着重写全部模块。新增测试位于同模块测试目录；未覆盖的新路径先修订本票/Map再写。
保留当前用户未提交改动、永久ADR/context、供应商源码与运行数据；生成物通过正式工具更新。

## 8. 验证矩阵

| 行为或风险 | 验证接缝/步骤 | 预期结果 | Evidence |
|---|---|---|---|
| 正常路径 | HTTP/操作日志入口→脱敏副本→事件→日志/数据库；执行下列定向命令及对应场景 | canary不出现在HTTP sink、OperLogEvent、数据库或错误日志 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-02.md</Path> |
| 失败路径 | 畸形JSON输出安全摘要；敏感签发响应不记录正文；业务原始字节不变；固定时序/故障注入，记录输入与最终可观察状态 | 无越权、错误状态或部分提交；可按定义恢复 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-02.md</Path> |
| 回归 | 运行所属包原有测试及受影响调用者；逐项核对下列AC | 普通字段/操作者/耗时/失败状态仍可观测；签名、加解密、SSE与正常token响应保持正确；旧日志处置与凭据轮换另有批准记录，本票不自动删历史数据 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-02.md</Path> |

Workspace checks：current-workspace；`frontend:`/`backend:`表示先进入该目录，其他命令cwd为仓根，逐条串行执行。新增用例实施时登记精确选择器、实际测试数与跳过项，零测试/required跳过不算通过。

- `backend: ./mvnw -pl wta-common/wta-common-json,wta-common/wta-common-web,wta-common/wta-common-log,wta-modules/wta-sso,wta-modules/wta-system,wta-admin -am test`

- E2E disposition：required: 用唯一凭据canary调用签发/失败接口，检查HTTP sink、OperLogEvent与数据库均不含明文。
- E2E owner/environment：single-agent（Lead）/current-workspace；使用隔离MySQL/Redis/OSS及必要真实HTTP/浏览器，禁止连生产。场景步骤以上表、本票AC为准；需新用例时在写集内创建后记录精确命令。
- Integration evidence：记录parent before、implementation commit及direct-parent检查；result SHA等于通过验证的implementation commit，candidate不适用。HTTP/操作日志两项红灯已修复；33项定向测试与真实HTTP/MySQL验收通过，零跳过。完整选集仍受Notify旧Git范围门禁阻塞，见T-02 Evidence。

## 9. 发布、迁移与恢复

- 顺序：前置票产生已验证合同后实施本票；源码、仓内消费者、测试和生成物同批交付。涉及DDL只编辑10-cde-base-ddl.sql，新环境按六文件基座初始化；不增加存量迁移工程。
- 兼容窗口：无；不保留旧接口或数据格式桥。生产部署不是本票自动步骤。
- 监控/诊断：观察本票AC的成功/错误状态、耗时及资源/持久化结果，日志只含安全元数据；复用现有观测入口，不新建监控平台。
- 恢复：恢复须保留签发接口禁记正文的安全收口，不能重新开启明文日志。
- 不可逆批准点：提交、推送、部署、运行数据删除/修复分别需授权；本地实现/验证已授权；最新用户指令明确本 change 所有提交暂缓。
- 收缩条件：本票替代的旧调用/配置引用归零且仓内回归通过；无被替代入口时不适用，不为凑清单扩大删除范围。

## 10. 验收标准

- [x] `AC-002`：canary不出现在HTTP sink、OperLogEvent、数据库或错误日志。
- [x] `AC-002`：普通字段/操作者/耗时/失败状态仍可观测。
- [x] `AC-002`：签名、加解密、SSE与正常token响应保持正确。
- [ ] `AC-002`：旧日志处置与凭据轮换另有批准记录，本票不自动删历史数据。
- [x] 按Map→适用Skill→本票完成读取及实际调用；所有required Skill记录passed并可回读。
- [x] 正常/失败/回归及required E2E均完成，证据写入<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-02.md</Path>，未执行不得标通过。
- [x] 修改不超出写集，共享项只有single-agent当前票轮次写入。
- [x] 获得授权后形成非空implementation commit，Lead完成direct-parent验收并记录parent result SHA；未获授权不提交、不标Done。
- [x] Ticket、Map、Goal与Evidence一致；不存在未批准偏差。

## 11. SKILL 调用计划

frontmatter绑定的项目Skill在implementation阶段接收本票路径和上游合同，产出适用分层、权限、数据/资源边界及实现diff；engineering-standards在verify阶段根据本节命令选择受影响门禁并输出AC/退出码/E2E记录。按入口scope展开引用，不以“已读”代替实际操作。
必需入口缺失或sha256漂移时阻塞本票；Lead回读差异后更新绑定及Map，不能自动接受新摘要。实际记录归<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-02.md</Path>的`## Skill Execution Records`，本次规划不伪造实现调用记录。

## 12. 停止、检查点与交付

交付本票完整可观察行为及验收证据；数量以Map为准。缺依赖/测试环境/Skill、越界或高影响事实变化时停止受影响票，保留checkpoint和失败证据，其他独立票仍可串行推进。恢复先读Goal、Map、本票、状态及最新Evidence；记录实际HEAD/dirty差异，禁止覆盖用户修改。
依赖：无。单票完成条件为全部AC、实际Skill证据和获授权的direct-parent出口；仅补文档不能标Done。

## Revision134 最终本地验收补记

HTTP sink、OperLogEvent和真实MySQL canary及普通字段/失败元数据已复验；全量旧Notify错误已关闭。历史日志处置/凭据轮换在本票OUT且未获授权，未执行，也不存在可伪造的批准记录；该外部批准项继续未勾选，不是待执行的本地实现。 实际证据：T-30-extra-services-v3.json, T-30-http-v1.json, T-30-v3-backend-tests.json；源码路径及hash见T-30-completion-audit-revision134.json。实施提交/direct-parent/result继续未勾选。

## Revision135 实际提交与父分支验收

用户已明确授权全部commit/push。implementation commits：`4cfb7eea819c142ccdbcc29004a86868b86e2551`；完整实现链 result SHA：`6c8764cca97bb6057fcb90ccdfe635c7efbf502a`。每个提交均非空、实际父SHA已核对且被result包含；Git归档逐文件等于T-30已验证输入，未声称拆分过程中的中间树独立通过全部测试。精确路径/共享owner/验证见 `../evidence/commit-delivery.json`。本票保持review；正式发布候选与change最终Done独立验收。
