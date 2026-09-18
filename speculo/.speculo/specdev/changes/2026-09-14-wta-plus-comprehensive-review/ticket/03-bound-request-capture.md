---
schema_version: 3
plan_contract_version: 1
skill_scan: "2026-09-18枚举.agents/skills入口并按本票真实路径/领域绑定；Map为最低集合"
skill_bindings: [{"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/03-bound-request-capture.md</Path>", "<Path>backend/wta-common/wta-common-web/</Path>", "<Path>backend/wta-common/wta-common-openapi/</Path>"], "outputs": ["T-03的架构/权限/数据边界检查与定向实现diff"], "required": true, "on_failure": "block-ticket"}, {"id": "wta-common-modules-guide", "path": "<Path>.agents/skills/wta-common-modules-guide/SKILL.md</Path>", "sha256": "e92775ce47af41bd33c1b3293d7f8a3185fdd739b9b588519e043c654f678d98", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/03-bound-request-capture.md</Path>", "<Path>backend/wta-common/wta-common-web/</Path>", "<Path>backend/wta-common/wta-common-openapi/</Path>"], "outputs": ["T-03的架构/权限/数据边界检查与定向实现diff"], "required": true, "on_failure": "block-ticket"}, {"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "verify", "operation": "verify-affected-contract-and-quality-gates", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/03-bound-request-capture.md</Path>", "current-workspace实际diff及本票验证矩阵"], "outputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-03.md</Path>：命令、退出码、测试数、AC与Skill Execution Records"], "required": true, "on_failure": "block-ticket"}]
resource_claims: ["workspace:current-exclusive", "finding:R-02", "contract:AC-003"]
artifact: ticket
change: 2026-09-14-wta-plus-comprehensive-review
id: T-03
title: 统一有界请求体采集与验签缓存
status: "blocked"
planning_depth: "deep"
planning_depth_reason: "安全/鉴权、公共合同、数据一致性或共享核心路径变更：统一有界请求体采集与验签缓存"
ready: false
risk: high
blocked_by: ["T-02"]
contract_ids: [AC-003]
owner: single-agent
expected_changes: ["<Path>backend/wta-common/wta-common-web/</Path>", "<Path>backend/wta-common/wta-common-openapi/</Path>", "<Path>backend/wta-common/wta-common-encrypt/</Path>", "<Path>backend/wta-admin/src/main/resources/application.yml</Path>", "<Path>backend/wta-common/wta-common-core/src/</Path>"]
writable_paths: ["<Path>backend/wta-common/wta-common-web/</Path>", "<Path>backend/wta-common/wta-common-openapi/</Path>", "<Path>backend/wta-common/wta-common-encrypt/</Path>", "<Path>backend/wta-admin/src/main/resources/application.yml</Path>", "<Path>backend/wta-common/wta-common-core/src/</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/</Path>", "<Path>{roots.state}/specdev/adr/</Path>"]
shared_paths: ["<Path>backend/wta-common/wta-common-web/</Path>", "<Path>backend/wta-common/wta-common-openapi/</Path>", "<Path>backend/wta-common/wta-common-encrypt/</Path>", "<Path>backend/wta-admin/src/main/resources/application.yml</Path>", "<Path>backend/wta-common/wta-common-core/src/</Path>"]
shared_path_owners: ["<Path>backend/wta-common/wta-common-web/</Path> => single-agent (Lead; serial T-03 turn)", "<Path>backend/wta-common/wta-common-openapi/</Path> => single-agent (Lead; serial T-03 turn)", "<Path>backend/wta-common/wta-common-encrypt/</Path> => single-agent (Lead; serial T-03 turn)", "<Path>backend/wta-admin/src/main/resources/application.yml</Path> => single-agent (Lead; serial T-03 turn)", "<Path>backend/wta-common/wta-common-core/src/</Path> => single-agent (Lead; serial T-03 turn)"]
---

# T-03：统一有界请求体采集与验签缓存

Map：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>；Spec：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/spec.md</Path>；Goal：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/goal-plan.md</Path>。
唯一执行者先读Map，再按Skill矩阵读取适用入口/引用，再读本票。禁止任何implementation/review/research子代理。Ready仅表示计划合同就绪，不表示已经授权实施或已验证通过。

## 1. 战略与来源

- 目标与可观察产出：大小边界前/等于/超限一字节结果可判定。
- 来源：R-02；AC-003；USER-DECISION: 全面完善计划、无兼容、单人串行。
- 当前事实与调用链：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/re-review.md</Path>中T-03行及对应专项报告；源码导航为frontmatter预计修改点。
- 规划深度：deep；安全/鉴权、公共合同、数据一致性或共享核心路径变更：统一有界请求体采集与验签缓存。

## 2. 决策状态

### 已锁定决策

建议普通JSON/机器正文2MiB为评估起点，最终以真实最大请求决定；新增413属于当前计划合同外部合同。

### 已采用的低影响假设

沿用当前仓库版本与既有模块命名；实施前回读实际源码，路径变化由本票修订，不猜测不存在的实现。

### 未决问题

普通 JSON/机器请求的硬预算尚未由合法请求样本和入口内存预算确定；2 MiB 仅为候选。关闭条件：列出普通JSON、OpenAPI、解密/XSS各视图的字节预算与最大合法样本，确定超限413合同及配置默认值后回写Spec/ADR。上传/SSE不纳入普通缓存。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 原始请求流→受限采集→认证/验签→业务 | common-core小型读取能力与request属性；保留上传/SSE流式和验签字节 | 本票之外的模块重写、兼容桥、远程发布与重要数据操作 |

## 4. 要构建什么

原始请求流→受限采集→认证/验签→业务。调用者可观察到：大小边界前/等于/超限一字节结果可判定。失败时：未知长度同样计数；超限413且业务未执行；日志错误处理不得吞掉413。

## 5. 实现契约

- 入口、输入输出与数据流：原始请求流→受限采集→认证/验签→业务。
- 不变量及失败语义：未知长度同样计数；超限413且业务未执行；日志错误处理不得吞掉413。
- 公共合同：本票只按以上行为及执行路线变更；同步全部仓内调用/生成物，沿用已有权限/Client/owner校验。未列出的接口保持原语义。
- 兼容：用户明确无需旧版兼容；仓内一次切换，不加双路由/版本等待。实际供应商协议仍须遵守。
- 安全与隐私：凭据不进入日志/UI证据；越权/过期/无owner拒绝；数据库与资源约束不能为前端成功而放宽。

RepeatableFilter与SysLogFilter已复用RepeatedlyRequestWrapper，不能按两个filter重复计缓存。OpenAPI目前不依赖common-web，优先复用common-core中的小型受限读取能力及请求属性保存原始正文，避免新模块或循环依赖；原始字节与XSS/解密后的视图分别拥有。OpenApiRequest的防御性复制先按峰值验证，不为减少复制公开可变byte[]。2 MiB是测量起点，不是已批准生产限制。

## 6. 执行路线

1. 测量最大合法普通JSON与机器请求，批准请求预算和日志前缀预算。
2. 固定未知长度/chunked超限以及伪签名头大请求的红灯。
3. 构造受限采集owner，Content-Length提前检查且实际读取计数，超限稳定413。
4. 复用精确原始字节，删除无界readAllBytes和不必要全数组复制；日志/解密/XSS顺序独立验证。
5. 验证正常JSON共享wrapper；上传和SSE继续流式，不强行进入普通正文缓存。
6. 记录并发堆峰值、GC和取消资源释放，确认压测只在隔离环境。
7. 超限异常不得被SysLogFilter.prepareRequest的宽泛catch吞掉后继续业务。对XssHttpServletRequestWrapper及解密视图同样检查边界，日志失败与入口413分开处理。

## 7. 路径访问契约

预计点、可写范围、只读上下文及共享项以frontmatter为唯一权威。共享owner固定single-agent；只有当前票轮次可写，下一票须回读前一结果。跨模块目录只允许本票行为必需的文件，目录授权不意味着重写全部模块。新增测试位于同模块测试目录；未覆盖的新路径先修订本票/Map再写。
保留当前用户未提交改动、永久ADR/context、供应商源码与运行数据；生成物通过正式工具更新。

## 8. 验证矩阵

| 行为或风险 | 验证接缝/步骤 | 预期结果 | Evidence |
|---|---|---|---|
| 正常路径 | 原始请求流→受限采集→认证/验签→业务；执行下列定向命令及对应场景 | 大小边界前/等于/超限一字节结果可判定 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-03.md</Path> |
| 失败路径 | 未知长度同样计数；超限413且业务未执行；日志错误处理不得吞掉413；固定时序/故障注入，记录输入与最终可观察状态 | 无越权、错误状态或部分提交；可按定义恢复 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-03.md</Path> |
| 回归 | 运行所属包原有测试及受影响调用者；逐项核对下列AC | 未知长度无法绕过限制，413时业务尚未执行；OpenAPI签名测试逐字节通过；同一正文不会因观察者线性增加完整副本，SSE/上传可取消 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-03.md</Path> |

Workspace checks：current-workspace；`frontend:`/`backend:`表示先进入该目录，其他命令cwd为仓根，逐条串行执行。新增用例实施时登记精确选择器、实际测试数与跳过项，零测试/required跳过不算通过。

- `backend: ./mvnw -pl wta-common/wta-common-web,wta-common/wta-common-openapi,wta-common/wta-common-encrypt -am test`

- E2E disposition：required: 通过真实HTTP发送定长/chunked边界请求、伪签名大正文及正常签名正文。
- E2E owner/environment：single-agent（Lead）/current-workspace；使用隔离MySQL/Redis/OSS及必要真实HTTP/浏览器，禁止连生产。场景步骤以上表、本票AC为准；需新用例时在写集内创建后记录精确命令。
- Integration evidence：记录parent before、implementation commit及direct-parent检查；result SHA等于通过验证的implementation commit，candidate不适用。当前全部产品检查not-run。

## 9. 发布、迁移与恢复

- 顺序：前置票产生已验证合同后实施本票；源码、仓内消费者、测试和生成物同批交付。涉及DDL只编辑10-cde-base-ddl.sql，新环境按六文件基座初始化；不增加存量迁移工程。
- 兼容窗口：无；不保留旧接口或数据格式桥。生产部署不是本票自动步骤。
- 监控/诊断：观察本票AC的成功/错误状态、耗时及资源/持久化结果，日志只含安全元数据；复用现有观测入口，不新建监控平台。
- 恢复：配置与代码作为同一候选恢复；不能退回无界readAllBytes。
- 不可逆批准点：提交、推送、部署、运行数据删除/修复分别需授权；本轮只有计划文档授权。
- 收缩条件：本票替代的旧调用/配置引用归零且仓内回归通过；无被替代入口时不适用，不为凑清单扩大删除范围。

## 10. 验收标准

- [ ] `AC-003`：大小边界前/等于/超限一字节结果可判定。
- [ ] `AC-003`：未知长度无法绕过限制，413时业务尚未执行。
- [ ] `AC-003`：OpenAPI签名测试逐字节通过。
- [ ] `AC-003`：同一正文不会因观察者线性增加完整副本，SSE/上传可取消。
- [ ] 按Map→适用Skill→本票完成读取及实际调用；所有required Skill记录passed并可回读。
- [ ] 正常/失败/回归及required E2E均完成，证据写入<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-03.md</Path>，未执行不得标通过。
- [ ] 修改不超出写集，共享项只有single-agent当前票轮次写入。
- [ ] 获得授权后形成非空implementation commit，Lead完成direct-parent验收并记录parent result SHA；未获授权不提交、不标Done。
- [ ] Ticket、Map、Goal与Evidence一致；不存在未批准偏差。

## 11. SKILL 调用计划

frontmatter绑定的项目Skill在implementation阶段接收本票路径和上游合同，产出适用分层、权限、数据/资源边界及实现diff；engineering-standards在verify阶段根据本节命令选择受影响门禁并输出AC/退出码/E2E记录。按入口scope展开引用，不以“已读”代替实际操作。
必需入口缺失或sha256漂移时阻塞本票；Lead回读差异后更新绑定及Map，不能自动接受新摘要。实际记录归<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-03.md</Path>的`## Skill Execution Records`，本次规划不伪造实现调用记录。

## 12. 停止、检查点与交付

交付本票完整可观察行为及验收证据；数量以Map为准。缺依赖/测试环境/Skill、越界或高影响事实变化时停止受影响票，保留checkpoint和失败证据，其他独立票仍可串行推进。恢复先读Goal、Map、本票、状态及最新Evidence；记录实际HEAD/dirty差异，禁止覆盖用户修改。
依赖：T-02。单票完成条件为全部AC、实际Skill证据和获授权的direct-parent出口；仅补文档不能标Done。
