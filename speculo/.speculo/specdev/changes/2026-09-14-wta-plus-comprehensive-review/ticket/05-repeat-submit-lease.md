---
schema_version: 3
plan_contract_version: 1
skill_scan: "2026-09-18枚举.agents/skills入口并按本票真实路径/领域绑定；Map为最低集合"
skill_bindings: [{"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/05-repeat-submit-lease.md</Path>", "<Path>backend/wta-common/wta-common-redis/src/main/java/org/namewta/common/redis/aspectj/RepeatSubmitAspect.java</Path>", "<Path>backend/wta-common/wta-common-redis/src/main/java/org/namewta/common/redis/utils/RedisUtils.java</Path>"], "outputs": ["T-05的架构/权限/数据边界检查与定向实现diff"], "required": true, "on_failure": "block-ticket"}, {"id": "wta-common-modules-guide", "path": "<Path>.agents/skills/wta-common-modules-guide/SKILL.md</Path>", "sha256": "e92775ce47af41bd33c1b3293d7f8a3185fdd739b9b588519e043c654f678d98", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/05-repeat-submit-lease.md</Path>", "<Path>backend/wta-common/wta-common-redis/src/main/java/org/namewta/common/redis/aspectj/RepeatSubmitAspect.java</Path>", "<Path>backend/wta-common/wta-common-redis/src/main/java/org/namewta/common/redis/utils/RedisUtils.java</Path>"], "outputs": ["T-05的架构/权限/数据边界检查与定向实现diff"], "required": true, "on_failure": "block-ticket"}, {"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "verify", "operation": "verify-affected-contract-and-quality-gates", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/05-repeat-submit-lease.md</Path>", "current-workspace实际diff及本票验证矩阵"], "outputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-05.md</Path>：命令、退出码、测试数、AC与Skill Execution Records"], "required": true, "on_failure": "block-ticket"}]
resource_claims: ["workspace:current-exclusive", "finding:R-04", "contract:AC-005"]
artifact: ticket
change: 2026-09-14-wta-plus-comprehensive-review
id: T-05
title: 修复防重键过期后的所有权竞态
status: "ready"
planning_depth: "deep"
planning_depth_reason: "安全/鉴权、公共合同、数据一致性或共享核心路径变更：修复防重键过期后的所有权竞态"
ready: true
risk: high
blocked_by: []
contract_ids: [AC-005]
owner: single-agent
expected_changes: ["<Path>backend/wta-common/wta-common-redis/src/main/java/org/namewta/common/redis/aspectj/RepeatSubmitAspect.java</Path>", "<Path>backend/wta-common/wta-common-redis/src/main/java/org/namewta/common/redis/utils/RedisUtils.java</Path>", "<Path>backend/wta-common/wta-common-redis/src/test/</Path>"]
writable_paths: ["<Path>backend/wta-common/wta-common-redis/src/main/java/org/namewta/common/redis/aspectj/RepeatSubmitAspect.java</Path>", "<Path>backend/wta-common/wta-common-redis/src/main/java/org/namewta/common/redis/utils/RedisUtils.java</Path>", "<Path>backend/wta-common/wta-common-redis/src/test/</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/</Path>", "<Path>{roots.state}/specdev/adr/</Path>"]
shared_paths: ["<Path>backend/wta-common/wta-common-redis/src/main/java/org/namewta/common/redis/aspectj/RepeatSubmitAspect.java</Path>", "<Path>backend/wta-common/wta-common-redis/src/main/java/org/namewta/common/redis/utils/RedisUtils.java</Path>", "<Path>backend/wta-common/wta-common-redis/src/test/</Path>"]
shared_path_owners: ["<Path>backend/wta-common/wta-common-redis/src/main/java/org/namewta/common/redis/aspectj/RepeatSubmitAspect.java</Path> => single-agent (Lead; serial T-05 turn)", "<Path>backend/wta-common/wta-common-redis/src/main/java/org/namewta/common/redis/utils/RedisUtils.java</Path> => single-agent (Lead; serial T-05 turn)", "<Path>backend/wta-common/wta-common-redis/src/test/</Path> => single-agent (Lead; serial T-05 turn)"]
---

# T-05：修复防重键过期后的所有权竞态

Map：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>；Spec：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/spec.md</Path>；Goal：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/goal-plan.md</Path>。
唯一执行者先读Map，再按Skill矩阵读取适用入口/引用，再读本票。禁止任何implementation/review/research子代理。Ready仅表示计划合同就绪，不表示已经授权实施或已验证通过。

## 1. 战略与来源

- 目标与可观察产出：A失败不得删除B的键。
- 来源：R-04；AC-005；USER-DECISION: 全面完善计划、无兼容、单人串行。
- 当前事实与调用链：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/re-review.md</Path>中T-05行及对应专项报告；源码导航为frontmatter预计修改点。
- 规划深度：deep；安全/鉴权、公共合同、数据一致性或共享核心路径变更：修复防重键过期后的所有权竞态。

## 2. 决策状态

### 已锁定决策

不改变防重窗口的业务含义；若引入嵌套支持需单独定界。

### 已采用的低影响假设

沿用当前仓库版本与既有模块命名；实施前回读实际源码，路径变化由本票修订，不猜测不存在的实现。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| RepeatSubmitAspect获取key/owner→执行→失败比较删除或成功留TTL | 现有Redis Lua/原子能力；不扩展嵌套异步语义或持久幂等 | 本票之外的模块重写、兼容桥、远程发布与重要数据操作 |

## 4. 要构建什么

RepeatSubmitAspect获取key/owner→执行→失败比较删除或成功留TTL。调用者可观察到：A失败不得删除B的键。失败时：A过期后不能删除B；未获取租约不能释放；线程复用无上下文残留。

## 5. 实现契约

- 入口、输入输出与数据流：RepeatSubmitAspect获取key/owner→执行→失败比较删除或成功留TTL。
- 不变量及失败语义：A过期后不能删除B；未获取租约不能释放；线程复用无上下文残留。
- 公共合同：本票只按以上行为及执行路线变更；同步全部仓内调用/生成物，沿用已有权限/Client/owner校验。未列出的接口保持原语义。
- 兼容：用户明确无需旧版兼容；仓内一次切换，不加双路由/版本等待。实际供应商协议仍须遵守。
- 安全与隐私：凭据不进入日志/UI证据；越权/过期/无owner拒绝；数据库与资源约束不能为前端成功而放宽。

<Path>backend/wta-common/wta-common-redis/src/main/java/org/namewta/common/redis/aspectj/RepeatSubmitAspect.java</Path> 的 lease 由随机 owner token 绑定；<Path>backend/wta-common/wta-common-redis/src/main/java/org/namewta/common/redis/utils/RedisUtils.java</Path> 必须原子 compare-and-delete，不能先 GET 再 DEL。A 超时→B 取得同 key→A 失败→C 请求以屏障控制时序；同时覆盖未取得 lease、业务异常、非成功 R、成功 TTL 与复用线程。嵌套/异步支持另行定界，不宣称持久 exactly-once。

## 6. 执行路线

1. 复现A超TTL、B取得同名键、A失败删除B、C进入的时序。
2. 把key与随机owner token作为同一lease保存。
3. 失败释放使用原子compare-and-delete，未取得lease不释放。
4. 保留成功防重TTL语义；明确嵌套/异步是否支持并清理线程上下文。
5. 用真实Redis证明比较删除原子性，不宣称业务exactly-once。

## 7. 路径访问契约

预计点、可写范围、只读上下文及共享项以frontmatter为唯一权威。共享owner固定single-agent；只有当前票轮次可写，下一票须回读前一结果。跨模块目录只允许本票行为必需的文件，目录授权不意味着重写全部模块。新增测试位于同模块测试目录；未覆盖的新路径先修订本票/Map再写。
保留当前用户未提交改动、永久ADR/context、供应商源码与运行数据；生成物通过正式工具更新。

## 8. 验证矩阵

| 行为或风险 | 验证接缝/步骤 | 预期结果 | Evidence |
|---|---|---|---|
| 正常路径 | RepeatSubmitAspect获取key/owner→执行→失败比较删除或成功留TTL；执行下列定向命令及对应场景 | A失败不得删除B的键 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-05.md</Path> |
| 失败路径 | A过期后不能删除B；未获取租约不能释放；线程复用无上下文残留；固定时序/故障注入，记录输入与最终可观察状态 | 无越权、错误状态或部分提交；可按定义恢复 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-05.md</Path> |
| 回归 | 运行所属包原有测试及受影响调用者；逐项核对下列AC | 正常失败可重试，正常成功TTL内被拒；异常/线程复用无ThreadLocal遗留；DB唯一约束和通知业务幂等不被此注解替代 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-05.md</Path> |

Workspace checks：current-workspace；`frontend:`/`backend:`表示先进入该目录，其他命令cwd为仓根，逐条串行执行。新增用例实施时登记精确选择器、实际测试数与跳过项，零测试/required跳过不算通过。

- `backend: ./mvnw -pl wta-common/wta-common-redis -am test`

- E2E disposition：required: 真实Redis以屏障复现A过期/B接管/A失败/C被拒及正常失败重试。
- E2E owner/environment：single-agent（Lead）/current-workspace；使用隔离MySQL/Redis/OSS及必要真实HTTP/浏览器，禁止连生产。场景步骤以上表、本票AC为准；需新用例时在写集内创建后记录精确命令。
- Integration evidence：记录parent before、implementation commit及direct-parent检查；result SHA等于通过验证的implementation commit，candidate不适用。当前全部产品检查not-run。

## 9. 发布、迁移与恢复

- 顺序：前置票产生已验证合同后实施本票；源码、仓内消费者、测试和生成物同批交付。涉及DDL只编辑10-cde-base-ddl.sql，新环境按六文件基座初始化；不增加存量迁移工程。
- 兼容窗口：无；不保留旧接口或数据格式桥。生产部署不是本票自动步骤。
- 监控/诊断：观察本票AC的成功/错误状态、耗时及资源/持久化结果，日志只含安全元数据；复用现有观测入口，不新建监控平台。
- 恢复：修复失败回退本票代码并停止使用错误释放路径；不批量清理Redis业务键。
- 不可逆批准点：提交、推送、部署、运行数据删除/修复分别需授权；本轮只有计划文档授权。
- 收缩条件：本票替代的旧调用/配置引用归零且仓内回归通过；无被替代入口时不适用，不为凑清单扩大删除范围。

## 10. 验收标准

- [ ] `AC-005`：A失败不得删除B的键。
- [ ] `AC-005`：正常失败可重试，正常成功TTL内被拒。
- [ ] `AC-005`：异常/线程复用无ThreadLocal遗留。
- [ ] `AC-005`：DB唯一约束和通知业务幂等不被此注解替代。
- [ ] 按Map→适用Skill→本票完成读取及实际调用；所有required Skill记录passed并可回读。
- [ ] 正常/失败/回归及required E2E均完成，证据写入<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-05.md</Path>，未执行不得标通过。
- [ ] 修改不超出写集，共享项只有single-agent当前票轮次写入。
- [ ] 获得授权后形成非空implementation commit，Lead完成direct-parent验收并记录parent result SHA；未获授权不提交、不标Done。
- [ ] Ticket、Map、Goal与Evidence一致；不存在未批准偏差。

## 11. SKILL 调用计划

frontmatter绑定的项目Skill在implementation阶段接收本票路径和上游合同，产出适用分层、权限、数据/资源边界及实现diff；engineering-standards在verify阶段根据本节命令选择受影响门禁并输出AC/退出码/E2E记录。按入口scope展开引用，不以“已读”代替实际操作。
必需入口缺失或sha256漂移时阻塞本票；Lead回读差异后更新绑定及Map，不能自动接受新摘要。实际记录归<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-05.md</Path>的`## Skill Execution Records`，本次规划不伪造实现调用记录。

## 12. 停止、检查点与交付

交付本票完整可观察行为及验收证据；数量以Map为准。缺依赖/测试环境/Skill、越界或高影响事实变化时停止受影响票，保留checkpoint和失败证据，其他独立票仍可串行推进。恢复先读Goal、Map、本票、状态及最新Evidence；记录实际HEAD/dirty差异，禁止覆盖用户修改。
依赖：无。单票完成条件为全部AC、实际Skill证据和获授权的direct-parent出口；仅补文档不能标Done。
