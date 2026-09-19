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
status: "review"
planning_depth: "deep"
planning_depth_reason: "安全/鉴权、公共合同、数据一致性或共享核心路径变更：统一有界请求体采集与验签缓存"
ready: true
risk: high
blocked_by: ["T-02"]
contract_ids: [AC-003]
owner: single-agent
expected_changes: ["<Path>backend/wta-common/wta-common-web/</Path>", "<Path>backend/wta-common/wta-common-openapi/</Path>", "<Path>backend/wta-common/wta-common-encrypt/</Path>", "<Path>backend/wta-admin/src/main/resources/application.yml</Path>", "<Path>backend/wta-common/wta-common-core/src/</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/requestbody/</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/openapi/</Path>", "<Path>backend/README.md</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/security/BrowserHttpsTransportIntegrationTest.java</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/logging/LogRedactionHttpMySqlIntegrationTest.java</Path>"]
writable_paths: ["<Path>backend/wta-common/wta-common-web/</Path>", "<Path>backend/wta-common/wta-common-openapi/</Path>", "<Path>backend/wta-common/wta-common-encrypt/</Path>", "<Path>backend/wta-admin/src/main/resources/application.yml</Path>", "<Path>backend/wta-common/wta-common-core/src/</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/requestbody/</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/openapi/</Path>", "<Path>backend/README.md</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/security/BrowserHttpsTransportIntegrationTest.java</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/logging/LogRedactionHttpMySqlIntegrationTest.java</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/</Path>", "<Path>{roots.state}/specdev/adr/</Path>"]
shared_paths: ["<Path>backend/wta-common/wta-common-web/</Path>", "<Path>backend/wta-common/wta-common-openapi/</Path>", "<Path>backend/wta-common/wta-common-encrypt/</Path>", "<Path>backend/wta-admin/src/main/resources/application.yml</Path>", "<Path>backend/wta-common/wta-common-core/src/</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/requestbody/</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/openapi/</Path>", "<Path>backend/README.md</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/security/BrowserHttpsTransportIntegrationTest.java</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/logging/LogRedactionHttpMySqlIntegrationTest.java</Path>"]
shared_path_owners: ["<Path>backend/wta-common/wta-common-web/</Path> => single-agent (Lead; serial T-03 turn)", "<Path>backend/wta-common/wta-common-openapi/</Path> => single-agent (Lead; serial T-03 turn)", "<Path>backend/wta-common/wta-common-encrypt/</Path> => single-agent (Lead; serial T-03 turn)", "<Path>backend/wta-admin/src/main/resources/application.yml</Path> => single-agent (Lead; serial T-03 turn)", "<Path>backend/wta-common/wta-common-core/src/</Path> => single-agent (Lead; serial T-03 turn)", "<Path>backend/wta-admin/src/test/java/org/namewta/test/requestbody/</Path> => single-agent (Lead; serial T-03 turn)", "<Path>backend/wta-admin/src/test/java/org/namewta/test/openapi/</Path> => single-agent (Lead; serial T-03 turn)", "<Path>backend/README.md</Path> => single-agent (Lead; serial T-03 turn)", "<Path>backend/wta-admin/src/test/java/org/namewta/test/security/BrowserHttpsTransportIntegrationTest.java</Path> => single-agent (Lead; serial T-03 turn)", "<Path>backend/wta-admin/src/test/java/org/namewta/test/logging/LogRedactionHttpMySqlIntegrationTest.java</Path> => single-agent (Lead; serial T-03 turn)"]
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

用户明确授权无需合法样本及目标内存/并发预算，先选通用初值：普通JSON/需要缓存的可记录正文与机器正文分别默认2MiB（2,097,152 bytes），独立可配置；日志前缀保持独立1MiB预算。超限HTTP 413、业务零执行。初值不声称适合已实测生产容量。

### 已采用的低影响假设

沿用当前仓库版本与既有模块命名；实施前回读实际源码，路径变化由本票修订，不猜测不存在的实现。

### 未决问题

无。

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

RepeatableFilter与SysLogFilter已复用RepeatedlyRequestWrapper，不能按两个filter重复计缓存。OpenAPI目前不依赖common-web，优先复用common-core中的小型受限读取能力及请求属性保存原始正文，避免新模块或循环依赖；原始字节与XSS视图分别拥有；T-11已移除解密链。OpenApiRequest的防御性复制先按峰值验证，不为减少复制公开可变byte[]。用户授权采用2MiB初始默认，运行时可通过namewta.web.request-body.max-size与openapi.max-body-size分别覆盖；非法或无界值启动失败。

## 6. 执行路线

1. 按新用户决定固定2MiB两类默认与独立日志前缀，配置绑定及正数/可分配范围在启动验证；用合成精确字节夹具验证，无须等待脱敏业务样本。
2. 固定未知长度/chunked超限以及伪签名头大请求的红灯。
3. 构造受限采集owner，Content-Length提前检查且实际读取计数，超限稳定413。
4. 复用精确原始字节，删除无界readAllBytes和不必要全数组复制；日志/XSS顺序独立验证，机器认证先于可改写视图。
5. 验证正常JSON共享wrapper；上传和SSE继续流式，不强行进入普通正文缓存。
6. 记录并发堆峰值、GC和取消资源释放，确认压测只在隔离环境。
7. 超限异常不得被SysLogFilter.prepareRequest的宽泛catch吞掉后继续业务。对XssHttpServletRequestWrapper清洗视图同样检查边界，已删除解密视图仅核对不复活，日志失败与入口413分开处理。

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
- Integration evidence：记录parent before、implementation commit及direct-parent检查；result SHA等于通过验证的implementation commit，candidate不适用。当前本地验证见T-03.md；实现commit/result按用户指令暂缓。

## 9. 发布、迁移与恢复

容量边界：原样本/容量等待已被用户最新授权替代，不再是实施阻塞。目标生产容量仍未知，在交付风险中保留；使用隔离测试环境记录实际并发/堆/GC。T-11已移除浏览器解密链，不重新引入；普通上传/SSE不纳入正文缓存。

- 顺序：前置票产生已验证合同后实施本票；源码、仓内消费者、测试和生成物同批交付。涉及DDL只编辑10-cde-base-ddl.sql，新环境按六文件基座初始化；不增加存量迁移工程。
- 兼容窗口：无；不保留旧接口或数据格式桥。生产部署不是本票自动步骤。
- 监控/诊断：观察本票AC的成功/错误状态、耗时及资源/持久化结果，日志只含安全元数据；复用现有观测入口，不新建监控平台。
- 恢复：配置与代码作为同一候选恢复；不能退回无界readAllBytes。
- 不可逆批准点：提交、推送、部署、运行数据删除/修复分别需授权；本轮实现和本地测试已授权；用户要求本change全部提交暂缓。
- 收缩条件：本票替代的旧调用/配置引用归零且仓内回归通过；无被替代入口时不适用，不为凑清单扩大删除范围。

## 10. 验收标准

- [x] `AC-003`：大小边界前/等于/超限一字节结果可判定。
- [x] `AC-003`：未知长度无法绕过限制，413时业务尚未执行。
- [x] `AC-003`：OpenAPI签名测试逐字节通过。
- [x] `AC-003`：同一正文不会因观察者线性增加完整副本，SSE/上传可取消。
- [x] 按Map→适用Skill→本票完成读取及实际调用；所有required Skill记录passed并可回读。
- [x] 正常/失败/回归及required E2E均完成，证据写入<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-03.md</Path>，未执行不得标通过。
- [x] 修改不超出写集，共享项只有single-agent当前票轮次写入。
- [x] 获得授权后形成非空implementation commit，Lead完成direct-parent验收并记录parent result SHA；未获授权不提交、不标Done。
- [x] Ticket、Map、Goal与Evidence一致；不存在未批准偏差。

## 11. SKILL 调用计划

frontmatter绑定的项目Skill在implementation阶段接收本票路径和上游合同，产出适用分层、权限、数据/资源边界及实现diff；engineering-standards在verify阶段根据本节命令选择受影响门禁并输出AC/退出码/E2E记录。按入口scope展开引用，不以“已读”代替实际操作。
必需入口缺失或sha256漂移时阻塞本票；Lead回读差异后更新绑定及Map，不能自动接受新摘要。实际记录归<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-03.md</Path>的`## Skill Execution Records`，本次规划不伪造实现调用记录。

## 12. 停止、检查点与交付

交付本票完整可观察行为及验收证据；数量以Map为准。缺依赖/测试环境/Skill、越界或高影响事实变化时停止受影响票，保留checkpoint和失败证据，其他独立票仍可串行推进。恢复先读Goal、Map、本票、状态及最新Evidence；记录实际HEAD/dirty差异，禁止覆盖用户修改。
依赖：T-02。单票完成条件为全部AC、实际Skill证据和获授权的direct-parent出口；仅补文档不能标Done。

Revision113只读复核：当前源码证据见<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/planning-blocker-source-review-current.md</Path>及17文件哈希。保留原blocked状态，未新增参数默认值、DDL或完成声明；所需业务/供应商事实已询问，尚无答复。

Revision117：T-03开始。用户授权无需样本/目标容量，JSON/机器默认各2MiB且独立可配置，日志前缀独立；原预算等待关闭。追加admin真实HTTP/现有OpenAPI测试与backend配置说明精确写集，593上游哈希已核对。机器原始验签先于XSS改写，公共缓存保持只读、不得暴露可变数组；413不可吞、普通上传/SSE不缓存。T-23用户已授权常见供应商预置，下一票由官方协议与实际SDK自主取证，不再等待用户原问题。28review/1in_progress/1ready/1blocked（T-23协议决策待研究）/0Done；全部提交暂缓。

Revision118：T-03构造器调用扫描确认两份admin真实HTTP夹具直接构造Repeatable/SysLog过滤器；先登记精确写集，以便同步显式请求预算参数，不保留旧构造器兼容桥。原有canary/HTTPS测试语义不变。

Revision119：T-03本地review。JSON/机器各2MiB独立预算、原始缓存共享与XSS独立视图、验签先于改写、稳定413已完成。83/83 common、814默认测试（717通过/97环境skip）、11/11真实HTTP与512MiB堆/8并发/2MiB请求探针通过；full/core清理构建和清单同源验证通过。26路径checkpoint、584上游非重叠不变/9重叠，累计610。29review/1ready/1blocked（T-23协议研究）/0Done；全部提交暂缓。

## Revision135 实际提交与父分支验收

用户已明确授权全部commit/push。implementation commits：`214de538267a60fa527b1213f7dd9139622f4a2e`；完整实现链 result SHA：`6c8764cca97bb6057fcb90ccdfe635c7efbf502a`。每个提交均非空、实际父SHA已核对且被result包含；Git归档逐文件等于T-30已验证输入，未声称拆分过程中的中间树独立通过全部测试。精确路径/共享owner/验证见 `../evidence/commit-delivery.json`。本票保持review；正式发布候选与change最终Done独立验收。
