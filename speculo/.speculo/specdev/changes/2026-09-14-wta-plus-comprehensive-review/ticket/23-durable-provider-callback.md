---
schema_version: 3
plan_contract_version: 1
skill_scan: "已扫描项目 .agents/skills/**/SKILL.md；仅绑定本票命中的入口，执行前按入口继续展开适用 references"
skill_bindings: [{"id":"engineering-standards","path":"<Path>.agents/skills/engineering-standards/SKILL.md</Path>","sha256":"ef0588daecab40e12e49e99d29a12bf107e887ab432e10a790bbc893fc42b0cf","phase":"plan","operation":"read-scope-contract-and-bind-acceptance","inputs":["当前审查候选、Ticket路径和上游ADR"],"outputs":["实现前边界、验证与失败停止条件"],"required":true,"on_failure":"block-ticket"},{"id":"namewta-fullstack-development","path":"<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>","sha256":"dd3e42720caa894bcb31ba1503b8211f669b3bbc2bb9ea3e40590a3a9edea395","phase":"plan","operation":"read-scope-contract-and-bind-acceptance","inputs":["当前审查候选、Ticket路径和上游ADR"],"outputs":["实现前边界、验证与失败停止条件"],"required":true,"on_failure":"block-ticket"},{"id":"wta-module-guide","path":"<Path>.agents/skills/wta-module-guide/SKILL.md</Path>","sha256":"f2e7e020552795c02285df65a941609fcd93692298951c012adbc555c20ac87f","phase":"plan","operation":"read-scope-contract-and-bind-acceptance","inputs":["当前审查候选、Ticket路径和上游ADR"],"outputs":["实现前边界、验证与失败停止条件"],"required":true,"on_failure":"block-ticket"}]
resource_claims: ["finding:B-05", "contract:AC-023"]
artifact: ticket
change: 2026-09-14-wta-plus-comprehensive-review
id: T-23
title: 将供应商回调幂等纳入持久事务
status: draft
planning_depth: deep
planning_depth_reason: "本票涉及 B-05；需先完成证据/高影响取舍，不能直接实现。"
ready: false
risk: high
blocked_by: ["T-22"]
contract_ids: [AC-023]
owner: user-review
expected_changes: ["<Path>backend/wta-modules/wta-notify/</Path>", "<Path>release-artifacts/docker/infrastructure/mysql/init/50-namewta-ddl.sql</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/notify/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/ProviderCallbackService.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/ProviderCallbackUseCase.java</Path>"]
writable_paths: ["<Path>backend/wta-modules/wta-notify/</Path>", "<Path>release-artifacts/docker/infrastructure/mysql/init/50-namewta-ddl.sql</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/notify/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/ProviderCallbackService.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/ProviderCallbackUseCase.java</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/</Path>", "<Path>{roots.state}/specdev/adr/</Path>"]
shared_paths: []
shared_path_owners: []
---

# T-23：将供应商回调幂等纳入持久事务

> 本 Ticket 是 <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/</Path> 内的待审实施计划。它不授权修改产品代码；只有用户明确接受该 Ticket 后，Lead 才能按 project Skill 建立实施 Evidence。

## 1. 战略与来源

- **Finding：** B-05。详细源码证据见专项审查报告。
- **目标：** 删除真实结构复杂性并恢复可验证的行为/安全/交付合同；不以移动代码、放宽门禁、删除测试或无限兼容层制造绿色。
- **可观察产出：** 回滚后相同事件可重试成功

## 2. 决策状态

- **当前状态：** draft，ready=false；高影响决策：ADR-CR-009推荐MySQL receipt唯一键；唯一身份与保留期需按实际供应商合同审核。
- **已锁定：** 本轮只写 change；实现、提交、发布、远程写入和永久知识修改均未授权。
- **未决：** 用户是否接受本票、外部合同/数据迁移/运行环境参数及是否进入下一 Work。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 固定事务提交失败后seenEvents仍抑制重试及跨provider事件ID碰撞红灯；按provider/account/channel与eventId明确唯一业务身份，必要时包括delivery，避免碰撞；在同事务写事件receipt与状态推进/聚合；唯一冲突按已经完成的结果返回 | 现有模块边界、权限、日志/错误合同中未被本票改变者 | 其他 finding 的实现、无证据的全仓重写、提交/部署、历史数据清理 |

## 4. 行为与实现契约

- **入口或 seam：** <Path>backend/wta-modules/wta-notify/</Path>；由该 module 的公开 interface 接受输入。
- **输入/输出：** 输入、错误和状态必须保持对应现有合同；任何外部行为变化先更新 Spec/ADR并阻塞本票。
- **不变量：** 失败关闭、权限/Client上下文、资源所有权、事务/幂等和可观测性按适用工程规则成立。
- **错误：** 跨实例相同事件不重复生效，不同provider同ID互不影响

## 5. 执行路线

1. 固定事务提交失败后seenEvents仍抑制重试及跨provider事件ID碰撞红灯。
2. 按provider/account/channel与eventId明确唯一业务身份，必要时包括delivery，避免碰撞。
3. 在同事务写事件receipt与状态推进/聚合；唯一冲突按已经完成的结果返回。
4. 删除进程内seenEvents及其过期清理分支。
5. 保留签名校验、单向状态等级和旧事件忽略语义。
6. 定义receipt保留与清理期限及对重放窗口的影响，清理不改变未完成事件。

## 6. 路径访问与所有权

- **可写候选：** <Path>backend/wta-modules/wta-notify/</Path>, <Path>release-artifacts/docker/infrastructure/mysql/init/50-namewta-ddl.sql</Path>, <Path>backend/wta-admin/src/test/java/org/namewta/test/notify/</Path>, <Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/ProviderCallbackService.java</Path>, <Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/ProviderCallbackUseCase.java</Path>（仅后续用户批准实施时；本轮只修改 Ticket）。
- **共享路径：** 与 Map 中占用相同模块、DDL、测试或 App 的票存在交集；以 Map 的资源 owner 和串行阶段为准，进入 Ready 前必须明确写集，不能据本票空 shared_paths 推断可并行。
- **不动：** 供应商/历史archive、永久 namespace、用户未提交改动和生成物，除非本票修订后有明确 owner。

## 7. 验证矩阵

| 行为/风险 | 接缝与方法 | 预期 | Evidence |
|---|---|---|---|
| AC 场景 1 | <Path>backend/wta-modules/wta-notify/</Path> 定向测试/静态或隔离运行 | 回滚后相同事件可重试成功 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-23.md</Path> |
| AC 场景 2 | <Path>backend/wta-modules/wta-notify/</Path> 定向测试/静态或隔离运行 | 跨实例相同事件不重复生效，不同provider同ID互不影响 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-23.md</Path> |
| AC 场景 3 | <Path>backend/wta-modules/wta-notify/</Path> 定向测试/静态或隔离运行 | 乱序/伪签名/迟到回调按现有规则失败关闭或no-op | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-23.md</Path> |
| AC 场景 4 | <Path>backend/wta-modules/wta-notify/</Path> 定向测试/静态或隔离运行 | 重启不丢幂等记录，聚合与delivery一致 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-23.md</Path> |

- **Workspace checks：** 见 §12 的实际工作目录、命令和隔离验收边界；本轮全部为计划，未执行 Maven、构建或外部服务测试。
- **E2E disposition：** required：跨模块/浏览器/外部服务行为必须由 Lead 在 current workspace 或 parent-candidate 验收。

## 8. 迁移、发布与恢复

- **迁移/兼容：** ADR-CR-009推荐MySQL receipt唯一键；唯一身份与保留期需按实际供应商合同审核。
- **回滚：** 先保留基线 SHA、产物/数据库备份及可验证恢复点；任何不可逆删除、DDL、凭据轮换或发布需独立批准。
- **停止条件：** 证据缺失、路径越界、父分支漂移、权限/安全合同不明、测试未运行或出现未批准偏差时停票。

## 9. 验收标准

- [ ] `AC-023`：回滚后相同事件可重试成功。
- [ ] `AC-023`：跨实例相同事件不重复生效，不同provider同ID互不影响。
- [ ] `AC-023`：乱序/伪签名/迟到回调按现有规则失败关闭或no-op。
- [ ] `AC-023`：重启不丢幂等记录，聚合与delivery一致。
- [ ] Ticket、Map、Spec、ADR 与 Evidence 状态一致；本票保持 draft/ready=false 直到用户接受并完成计划质量复核。
- [ ] 未通过删除测试、关闭类型/安全规则、吞错误、扩大权限或伪造运行证据获得通过。

## 10. Skill 调用计划

- 计划绑定：namewta-fullstack-development, wta-module-guide。每项入口摘要、references、phase和失败动作以 frontmatter 为最低合同；执行前必须读取当前 Ticket、Map 与实际命中 references。
- 失败动作：必需入口、验证工具或安全/数据证据不可用则 block-ticket；不得以其他工具静默替换。

## 11. 交付与恢复

- **依赖：** T-22；完成顺序由 Tickets Map 控制。
- **检查点：** 记录实现前 base SHA、工作树、命令/退出码、偏差、最终 result SHA 及未运行项。
- **完成出口：** 只在用户批准、所有适用 AC/E2E/Evidence 通过、父分支包含结果后进入 done；当前审查阶段不进入 done。

## 12. 实施前源码定位与验证命令

<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/ProviderCallbackUseCase.java</Path> 继续验 HMAC 原文、providerKey/eventId/时间窗。相同 receipt 身份但不同 payload 必须拒绝或审计；CAS 失败后重读状态，不能先写已处理 receipt 吞掉重试。未找到 providerMessageId、Worker 尚未写回、commit 失败和多实例重启均可重试/对账。receipt/Delivery/Intent 原子，与 T-22 使用一致锁序；按供应商重试/签名窗口定义 retention。

命令计划（全部 not-run）：

- 在 <Path>backend/</Path> 的 PowerShell 执行 `.\mvnw.cmd -pl wta-modules/wta-notify,wta-admin -am test`；Bash 使用 `./mvnw` 同参数入口。
- <Path>backend/pom.xml</Path> 的 Surefire groups 取 profiles.active，排除 exclude；核对目标 @Tag/环境门控及报告实际测试数。默认 test 退出 0 不等于隔离 e2e 已运行，SQL/Redis/Provider 故障矩阵须另建立真实测试并记录命令、环境、未跳过数和退出码。
