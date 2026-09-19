---
schema_version: 3
plan_contract_version: 1
skill_scan: "2026-09-18枚举.agents/skills入口并按本票真实路径/领域绑定；Map为最低集合"
skill_bindings: [{"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/04-trusted-client-address.md</Path>", "<Path>backend/wta-common/wta-common-core/src/main/java/org/namewta/common/core/utils/ServletUtils.java</Path>", "<Path>backend/wta-common/wta-common-security/</Path>"], "outputs": ["T-04的架构/权限/数据边界检查与定向实现diff"], "required": true, "on_failure": "block-ticket"}, {"id": "wta-common-modules-guide", "path": "<Path>.agents/skills/wta-common-modules-guide/SKILL.md</Path>", "sha256": "e92775ce47af41bd33c1b3293d7f8a3185fdd739b9b588519e043c654f678d98", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/04-trusted-client-address.md</Path>", "<Path>backend/wta-common/wta-common-core/src/main/java/org/namewta/common/core/utils/ServletUtils.java</Path>", "<Path>backend/wta-common/wta-common-security/</Path>"], "outputs": ["T-04的架构/权限/数据边界检查与定向实现diff"], "required": true, "on_failure": "block-ticket"}, {"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "verify", "operation": "verify-affected-contract-and-quality-gates", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/04-trusted-client-address.md</Path>", "current-workspace实际diff及本票验证矩阵"], "outputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-04.md</Path>：命令、退出码、测试数、AC与Skill Execution Records"], "required": true, "on_failure": "block-ticket"}]
resource_claims: ["workspace:current-exclusive", "finding:R-03", "contract:AC-004"]
artifact: ticket
change: 2026-09-14-wta-plus-comprehensive-review
id: T-04
title: 建立可信代理来源IP合同
status: "review"
planning_depth: "deep"
planning_depth_reason: "安全/鉴权、公共合同、数据一致性或共享核心路径变更：建立可信代理来源IP合同"
ready: true
risk: high
blocked_by: []
contract_ids: [AC-004]
owner: single-agent
expected_changes: ["<Path>backend/wta-common/wta-common-core/src/main/java/org/namewta/common/core/utils/ServletUtils.java</Path>", "<Path>backend/wta-common/wta-common-security/</Path>", "<Path>backend/wta-common/wta-common-redis/</Path>", "<Path>release-artifacts/docker/frontend/nginx/</Path>", "<Path>backend/wta-common/wta-common-core/src/test/</Path>", "<Path>backend/wta-common/wta-common-web/src/</Path>", "<Path>backend/wta-admin/src/main/resources/application.yml</Path>", "<Path>backend/wta-common/wta-common-core/src/main/java/org/namewta/common/core/utils/NetUtils.java</Path>", "<Path>backend/wta-common/wta-common-core/src/main/java/org/namewta/common/core/utils/ip/ClientAddressResolver.java</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/security/TrustedClientAddressIntegrationTest.java</Path>", "<Path>release-artifacts/skills/wta-namewta-nginx-config/scripts/add_app.py</Path>", "<Path>release-artifacts/tests/nacos-proxy-config.test.mjs</Path>", "<Path>release-artifacts/tests/trusted-client-address.test.mjs</Path>", "<Path>.agents/skills/wta-common-modules-guide/references/core-utils.md</Path>"]
writable_paths: ["<Path>backend/wta-common/wta-common-core/src/main/java/org/namewta/common/core/utils/ServletUtils.java</Path>", "<Path>backend/wta-common/wta-common-security/</Path>", "<Path>backend/wta-common/wta-common-redis/</Path>", "<Path>release-artifacts/docker/frontend/nginx/</Path>", "<Path>backend/wta-common/wta-common-core/src/test/</Path>", "<Path>backend/wta-common/wta-common-web/src/</Path>", "<Path>backend/wta-admin/src/main/resources/application.yml</Path>", "<Path>backend/wta-common/wta-common-core/src/main/java/org/namewta/common/core/utils/NetUtils.java</Path>", "<Path>backend/wta-common/wta-common-core/src/main/java/org/namewta/common/core/utils/ip/ClientAddressResolver.java</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/security/TrustedClientAddressIntegrationTest.java</Path>", "<Path>release-artifacts/skills/wta-namewta-nginx-config/scripts/add_app.py</Path>", "<Path>release-artifacts/tests/nacos-proxy-config.test.mjs</Path>", "<Path>release-artifacts/tests/trusted-client-address.test.mjs</Path>", "<Path>.agents/skills/wta-common-modules-guide/references/core-utils.md</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/</Path>", "<Path>{roots.state}/specdev/adr/</Path>"]
shared_paths: ["<Path>backend/wta-common/wta-common-core/src/main/java/org/namewta/common/core/utils/ServletUtils.java</Path>", "<Path>backend/wta-common/wta-common-security/</Path>", "<Path>backend/wta-common/wta-common-redis/</Path>", "<Path>release-artifacts/docker/frontend/nginx/</Path>", "<Path>backend/wta-common/wta-common-core/src/test/</Path>", "<Path>backend/wta-common/wta-common-web/src/</Path>", "<Path>backend/wta-admin/src/main/resources/application.yml</Path>", "<Path>backend/wta-common/wta-common-core/src/main/java/org/namewta/common/core/utils/NetUtils.java</Path>", "<Path>backend/wta-common/wta-common-core/src/main/java/org/namewta/common/core/utils/ip/ClientAddressResolver.java</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/security/TrustedClientAddressIntegrationTest.java</Path>", "<Path>release-artifacts/skills/wta-namewta-nginx-config/scripts/add_app.py</Path>", "<Path>release-artifacts/tests/nacos-proxy-config.test.mjs</Path>", "<Path>release-artifacts/tests/trusted-client-address.test.mjs</Path>"]
shared_path_owners: ["<Path>backend/wta-common/wta-common-core/src/main/java/org/namewta/common/core/utils/ServletUtils.java</Path> => single-agent (Lead; serial T-04 turn)", "<Path>backend/wta-common/wta-common-security/</Path> => single-agent (Lead; serial T-04 turn)", "<Path>backend/wta-common/wta-common-redis/</Path> => single-agent (Lead; serial T-04 turn)", "<Path>release-artifacts/docker/frontend/nginx/</Path> => single-agent (Lead; serial T-04 turn)", "<Path>backend/wta-common/wta-common-core/src/test/</Path> => single-agent (Lead; serial T-04 turn)", "<Path>backend/wta-common/wta-common-web/src/</Path> => single-agent (Lead; serial T-04 turn)", "<Path>backend/wta-admin/src/main/resources/application.yml</Path> => single-agent (Lead; serial T-04 turn)", "<Path>backend/wta-common/wta-common-core/src/main/java/org/namewta/common/core/utils/NetUtils.java</Path> => single-agent (Lead; serial T-04 turn)", "<Path>backend/wta-common/wta-common-core/src/main/java/org/namewta/common/core/utils/ip/ClientAddressResolver.java</Path> => single-agent (Lead; serial T-04 turn)", "<Path>backend/wta-admin/src/test/java/org/namewta/test/security/TrustedClientAddressIntegrationTest.java</Path> => single-agent (Lead; serial T-04 turn)", "<Path>release-artifacts/skills/wta-namewta-nginx-config/scripts/add_app.py</Path> => single-agent (Lead; serial T-04 turn)", "<Path>release-artifacts/tests/nacos-proxy-config.test.mjs</Path> => single-agent (Lead; serial T-04 turn)", "<Path>release-artifacts/tests/trusted-client-address.test.mjs</Path> => single-agent (Lead; serial T-04 turn)"]
---

# T-04：建立可信代理来源IP合同

Map：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>；Spec：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/spec.md</Path>；Goal：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/goal-plan.md</Path>。
唯一执行者先读Map，再按Skill矩阵读取适用入口/引用，再读本票。禁止任何implementation/review/research子代理。Ready仅表示计划合同就绪，不表示已经授权实施或已验证通过。

## 1. 战略与来源

- 目标与可观察产出：任意外来XFF不改变直连或正常入口的授权结果。
- 来源：R-03；AC-004；USER-DECISION: 全面完善计划、无兼容、单人串行。
- 当前事实与调用链：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/re-review.md</Path>中T-04行及对应专项报告；源码导航为frontmatter预计修改点。
- 规划深度：deep；安全/鉴权、公共合同、数据一致性或共享核心路径变更：建立可信代理来源IP合同。

## 2. 决策状态

### 已锁定决策

ADR-CR-003：算法以显式可信CIDR为输入，默认空集合只信socket peer；隔离拓扑提供测试CIDR，真实部署值在G-release关闭前实测。

### 已采用的低影响假设

沿用当前仓库版本与既有模块命名；实施前回读实际源码，路径变化由本票修订，不猜测不存在的实现。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| socket peer+显式可信CIDR+转发链→规范客户端IP | ServletUtils与现有IP/限流消费；无可信配置时只信peer，不默认所有私网 | 本票之外的模块重写、兼容桥、远程发布与重要数据操作 |

## 4. 要构建什么

socket peer+显式可信CIDR+转发链→规范客户端IP。调用者可观察到：任意外来XFF不改变直连或正常入口的授权结果。失败时：从右向左跳过可信hop；不可信peer忽略转发头；非法链不能放行白名单。

## 5. 实现契约

- 入口、输入输出与数据流：socket peer+显式可信CIDR+转发链→规范客户端IP。
- 不变量及失败语义：从右向左跳过可信hop；不可信peer忽略转发头；非法链不能放行白名单。
- 公共合同：本票只按以上行为及执行路线变更；同步全部仓内调用/生成物，沿用已有权限/Client/owner校验。未列出的接口保持原语义。
- 兼容：用户明确无需旧版兼容；仓内一次切换，不加双路由/版本等待。实际供应商协议仍须遵守。
- 安全与隐私：凭据不进入日志/UI证据；越权/过期/无owner拒绝；数据库与资源约束不能为前端成功而放宽。

<Path>backend/wta-common/wta-common-core/src/main/java/org/namewta/common/core/utils/ServletUtils.java</Path> 被白名单与 RateLimiterAspect 同时使用。按实际 peer/CIDR 取证，不能默认所有私网可信；公网入口覆盖不可信头，可信内层保留/追加，应用从 socket peer 向可信 hop 边界解析。覆盖 IPv4/IPv6、单/双代理、缺头、非法值与伪造最左值；nginx -t 只验证配置，不能代替伪造头请求矩阵。没有实际入口证据时 R-03 保持 likely。

## 6. 执行路线

1. 先在隔离单/双Nginx链复现伪造XFF影响白名单；若外层已有清洗记录反证再收窄。
2. 列真实peer/proxy/client地址及可信代理CIDR，不按所有私网默认可信。
3. 公网入口覆盖不可信转发头；应用仅在peer可信时按可信hop解析，直连仅用socket peer。
4. 白名单、限流、审计复用来源解析，删多头任意fallback。
5. 分别验证IPv4/IPv6、多级链与部署环境正常用户不误封。

## 7. 路径访问契约

预计点、可写范围、只读上下文及共享项以frontmatter为唯一权威。共享owner固定single-agent；只有当前票轮次可写，下一票须回读前一结果。跨模块目录只允许本票行为必需的文件，目录授权不意味着重写全部模块。新增测试位于同模块测试目录；未覆盖的新路径先修订本票/Map再写。
保留当前用户未提交改动、永久ADR/context、供应商源码与运行数据；生成物通过正式工具更新。

## 8. 验证矩阵

| 行为或风险 | 验证接缝/步骤 | 预期结果 | Evidence |
|---|---|---|---|
| 正常路径 | socket peer+显式可信CIDR+转发链→规范客户端IP；执行下列定向命令及对应场景 | 任意外来XFF不改变直连或正常入口的授权结果 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-04.md</Path> |
| 失败路径 | 从右向左跳过可信hop；不可信peer忽略转发头；非法链不能放行白名单；固定时序/故障注入，记录输入与最终可观察状态 | 无越权、错误状态或部分提交；可按定义恢复 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-04.md</Path> |
| 回归 | 运行所属包原有测试及受影响调用者；逐项核对下列AC | 可信双代理解析一致，非法值失败关闭；白名单与限流使用同一个规范来源；未获得环境资料时不得把likely改成已复现越权 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-04.md</Path> |

Workspace checks：current-workspace；`frontend:`/`backend:`表示先进入该目录，其他命令cwd为仓根，逐条串行执行。新增用例实施时登记精确选择器、实际测试数与跳过项，零测试/required跳过不算通过。

- `backend: ./mvnw -pl wta-common/wta-common-core,wta-common/wta-common-web,wta-common/wta-common-security,wta-common/wta-common-redis -am test`

- E2E disposition：required: 隔离单/双Nginx链发IPv4/IPv6和伪造XFF请求，比较白名单、限流、审计来源。
- E2E owner/environment：single-agent（Lead）/current-workspace；使用隔离MySQL/Redis/OSS及必要真实HTTP/浏览器，禁止连生产。场景步骤以上表、本票AC为准；需新用例时在写集内创建后记录精确命令。
- Integration evidence：记录parent before、implementation commit及direct-parent检查；result SHA等于通过验证的implementation commit，candidate不适用。当前全部产品检查not-run。

## 9. 发布、迁移与恢复

- 顺序：前置票产生已验证合同后实施本票；源码、仓内消费者、测试和生成物同批交付。涉及DDL只编辑10-cde-base-ddl.sql，新环境按六文件基座初始化；不增加存量迁移工程。
- 兼容窗口：无；不保留旧接口或数据格式桥。生产部署不是本票自动步骤。
- 监控/诊断：观察本票AC的成功/错误状态、耗时及资源/持久化结果，日志只含安全元数据；复用现有观测入口，不新建监控平台。
- 恢复：可信CIDR/入口配置与代码一起恢复；实际生产拓扑验收属于发布Gate。
- 不可逆批准点：提交、推送、部署、运行数据删除/修复分别需授权；最新用户已授权实现和本地验证；全change暂不提交。
- 收缩条件：本票替代的旧调用/配置引用归零且仓内回归通过；无被替代入口时不适用，不为凑清单扩大删除范围。

## 10. 验收标准

- [x] `AC-004`：任意外来XFF不改变直连或正常入口的授权结果。
- [x] `AC-004`：可信双代理解析一致，非法值失败关闭。
- [x] `AC-004`：白名单与限流使用同一个规范来源。
- [x] `AC-004`：未获得环境资料时不得把likely改成已复现越权。
- [x] 按Map→适用Skill→本票完成读取及实际调用；所有required Skill记录passed并可回读。
- [x] 正常/失败/回归及required E2E均完成，证据写入<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-04.md</Path>，未执行不得标通过。
- [x] 修改不超出写集，共享项只有single-agent当前票轮次写入。
- [x] 获得授权后形成非空implementation commit，Lead完成direct-parent验收并记录parent result SHA；未获授权不提交、不标Done。
- [x] Ticket、Map、Goal与Evidence一致；不存在未批准偏差。

## 11. SKILL 调用计划

frontmatter绑定的项目Skill在implementation阶段接收本票路径和上游合同，产出适用分层、权限、数据/资源边界及实现diff；engineering-standards在verify阶段根据本节命令选择受影响门禁并输出AC/退出码/E2E记录。按入口scope展开引用，不以“已读”代替实际操作。
必需入口缺失或sha256漂移时阻塞本票；Lead回读差异后更新绑定及Map，不能自动接受新摘要。实际记录归<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-04.md</Path>的`## Skill Execution Records`，本次规划不伪造实现调用记录。

## 12. 停止、检查点与交付

交付本票完整可观察行为及验收证据；数量以Map为准。缺依赖/测试环境/Skill、越界或高影响事实变化时停止受影响票，保留checkpoint和失败证据，其他独立票仍可串行推进。恢复先读Goal、Map、本票、状态及最新Evidence；记录实际HEAD/dirty差异，禁止覆盖用户修改。
依赖：无。单票完成条件为全部AC、实际Skill证据和获授权的direct-parent出口；仅补文档不能标Done。

### 本地实施决策

只解析显式可信CIDR下的XFF，默认空配置，其他头不能作为fallback。web入口过滤器在业务处理前计算一次并保存server-only request attribute，ServletUtils消费该值，未经过滤器的调用仅取peer；无core到web反向依赖。非法可信链返回400且不回退代理地址，XFF限制32 hop/4096字符。应用禁止native/framework转发地址重写以保留真实socket peer；环境可信CIDR发布前仍需实测。IPv6白名单精确匹配按地址字节比较，配置CIDR仅接受数字字面量、不解析DNS。保留现有Java方法签名，调用方自定义转发头参数明确拒绝；当前仓内无此调用，无旧多头兼容桥。公网LB覆盖XFF，内部App代理继续追加，add_app生成器同步该入口合同。

### 本地验证检查点

56项受影响Maven测试、12项定向含真实Nginx/Jetty/IPv4/IPv6测试、45项发布合同及四类Compose解析均通过。真实HTTP发现并修复Jetty方括号IPv6 peer，外来XFF仍严格校验。证据见T-04.md及T-04-checkpoint.json；所有提交仍暂停，commit/result为null，正式Done/生产拓扑验收未关闭。

## Revision134 最终本地验收补记

真实单/双Nginx、IPv4/IPv6、伪造XFF及统一来源矩阵已复验。实际生产CIDR/拓扑未验证，likely不改写为生产已复现越权。 实际证据：T-30-http-v1.json, T-30-v2-release.json；源码路径及hash见T-30-completion-audit-revision134.json。实施提交/direct-parent/result继续未勾选。

## Revision135 实际提交与父分支验收

用户已明确授权全部commit/push。implementation commits：`d3fb6abbd17c032a2d8040c6e0ab8ab110481bee`；完整实现链 result SHA：`6c8764cca97bb6057fcb90ccdfe635c7efbf502a`。每个提交均非空、实际父SHA已核对且被result包含；Git归档逐文件等于T-30已验证输入，未声称拆分过程中的中间树独立通过全部测试。精确路径/共享owner/验证见 `../evidence/commit-delivery.json`。本票保持review；正式发布候选与change最终Done独立验收。
