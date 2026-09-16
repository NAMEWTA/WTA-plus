---
schema_version: 3
plan_contract_version: 1
skill_scan: "已扫描项目 .agents/skills/**/SKILL.md；仅绑定本票命中的入口，执行前按入口继续展开适用 references"
skill_bindings: [{"id":"engineering-standards","path":"<Path>.agents/skills/engineering-standards/SKILL.md</Path>","sha256":"ef0588daecab40e12e49e99d29a12bf107e887ab432e10a790bbc893fc42b0cf","phase":"plan","operation":"read-scope-contract-and-bind-acceptance","inputs":["当前审查候选、Ticket路径和上游ADR"],"outputs":["实现前边界、验证与失败停止条件"],"required":true,"on_failure":"block-ticket"},{"id":"java-api-compatibility","path":"<Path>.agents/skills/java-api-compatibility/SKILL.md</Path>","sha256":"8a9f4384522e4e7561113f05fa36afd402dad6e82140ee95300d3e8406267c14","phase":"plan","operation":"read-scope-contract-and-bind-acceptance","inputs":["当前审查候选、Ticket路径和上游ADR"],"outputs":["实现前边界、验证与失败停止条件"],"required":true,"on_failure":"block-ticket"},{"id":"namewta-fullstack-development","path":"<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>","sha256":"dd3e42720caa894bcb31ba1503b8211f669b3bbc2bb9ea3e40590a3a9edea395","phase":"plan","operation":"read-scope-contract-and-bind-acceptance","inputs":["当前审查候选、Ticket路径和上游ADR"],"outputs":["实现前边界、验证与失败停止条件"],"required":true,"on_failure":"block-ticket"}]
resource_claims: ["finding:B-08", "contract:AC-015"]
artifact: ticket
change: 2026-09-14-wta-plus-comprehensive-review
id: T-15
title: 移除Profile过渡接口与测试构造桥
status: draft
planning_depth: deep
planning_depth_reason: "本票涉及 B-08；需先完成证据/高影响取舍，不能直接实现。"
ready: false
risk: high
blocked_by: ["T-14"]
contract_ids: [AC-015]
owner: user-review
expected_changes: ["<Path>backend/wta-modules/wta-profile/</Path>", "<Path>backend/wta-api/</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/profile/</Path>", "<Path>.agents/skills/engineering-standards/references/project/02-decisions-and-exceptions.md</Path>"]
writable_paths: ["<Path>backend/wta-modules/wta-profile/</Path>", "<Path>backend/wta-api/</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/profile/</Path>", "<Path>.agents/skills/engineering-standards/references/project/02-decisions-and-exceptions.md</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/</Path>", "<Path>{roots.state}/specdev/adr/</Path>"]
shared_paths: []
shared_path_owners: []
---

# T-15：移除Profile过渡接口与测试构造桥

> 本 Ticket 是 <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/</Path> 内的待审实施计划。它不授权修改产品代码；只有用户明确接受该 Ticket 后，Lead 才能按 project Skill 建立实施 Evidence。

## 1. 战略与来源

- **Finding：** B-08。详细源码证据见专项审查报告。
- **目标：** 删除真实结构复杂性并恢复可验证的行为/安全/交付合同；不以移动代码、放宽门禁、删除测试或无限兼容层制造绿色。
- **可观察产出：** 旧入口引用为零且替代能力覆盖完整

## 2. 决策状态

- **当前状态：** draft，ready=false；高影响决策：ADR-CR-008：用户希望无兼容升级，但Java公共删除门槛是硬约束；移除版本、外部消费者和发布授权待审，不以本票草案视为已批准。
- **已锁定：** 本轮只写 change；实现、提交、发布、远程写入和永久知识修改均未授权。
- **未决：** 用户是否接受本票、外部合同/数据迁移/运行环境参数及是否进入下一 Work。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 枚举全部Deprecated default throwing、旧构造、I*兼容接口、codec和调用方，记录方法级删除映射；将测试fixture迁到正式port与显式userId/operatorId合同，保持所有原有行为断言；先替换所有仓内消费者，再一次性删除无调用旧签名/default链，调用遗漏应编译失败 | 现有模块边界、权限、日志/错误合同中未被本票改变者 | 其他 finding 的实现、无证据的全仓重写、提交/部署、历史数据清理 |

## 4. 行为与实现契约

- **入口或 seam：** <Path>backend/wta-modules/wta-profile/</Path>；由该 module 的公开 interface 接受输入。
- **输入/输出：** 输入、错误和状态必须保持对应现有合同；任何外部行为变化先更新 Spec/ADR并阻塞本票。
- **不变量：** 失败关闭、权限/Client上下文、资源所有权、事务/幂等和可观测性按适用工程规则成立。
- **错误：** 全Profile及主要消费者编译/测试通过

## 5. 执行路线

1. 枚举全部Deprecated default throwing、旧构造、I*兼容接口、codec和调用方，记录方法级删除映射。
2. 将测试fixture迁到正式port与显式userId/operatorId合同，保持所有原有行为断言。
3. 先替换所有仓内消费者，再一次性删除无调用旧签名/default链，调用遗漏应编译失败。
4. 撤销EX-002兼容构造例外并同步真实Skill事实。
5. 确认没有删除公开安全/事务/工作流事件能力，仅减少旧重载和空实现。

## 6. 路径访问与所有权

- **可写候选：** <Path>backend/wta-modules/wta-profile/</Path>, <Path>backend/wta-api/</Path>, <Path>backend/wta-admin/src/test/java/org/namewta/test/profile/</Path>, <Path>.agents/skills/engineering-standards/references/project/02-decisions-and-exceptions.md</Path>（仅后续用户批准实施时；本轮只修改 Ticket）。
- **共享路径：** 与 Map 中占用相同模块、DDL、测试或 App 的票存在交集；以 Map 的资源 owner 和串行阶段为准，进入 Ready 前必须明确写集，不能据本票空 shared_paths 推断可并行。
- **不动：** 供应商/历史archive、永久 namespace、用户未提交改动和生成物，除非本票修订后有明确 owner。

## 7. 验证矩阵

| 行为/风险 | 接缝与方法 | 预期 | Evidence |
|---|---|---|---|
| AC 场景 1 | <Path>backend/wta-modules/wta-profile/</Path> 定向测试/静态或隔离运行 | 旧入口引用为零且替代能力覆盖完整 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-15.md</Path> |
| AC 场景 2 | <Path>backend/wta-modules/wta-profile/</Path> 定向测试/静态或隔离运行 | 全Profile及主要消费者编译/测试通过 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-15.md</Path> |
| AC 场景 3 | <Path>backend/wta-modules/wta-profile/</Path> 定向测试/静态或隔离运行 | 当前接口不再让缺owner上下文的调用编译通过 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-15.md</Path> |
| AC 场景 4 | <Path>backend/wta-modules/wta-profile/</Path> 定向测试/静态或隔离运行 | 删除清单逐项有替代入口/调用方/验证证据 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-15.md</Path> |

- **Workspace checks：** 见 §12 的实际工作目录、命令和隔离验收边界；本轮全部为计划，未执行 Maven、构建或外部服务测试。
- **E2E disposition：** required：跨模块/浏览器/外部服务行为必须由 Lead 在 current workspace 或 parent-candidate 验收。

## 8. 迁移、发布与恢复

- **迁移/兼容：** ADR-CR-008：用户希望无兼容升级，但Java公共删除门槛是硬约束；移除版本、外部消费者和发布授权待审，不以本票草案视为已批准。
- **回滚：** 先保留基线 SHA、产物/数据库备份及可验证恢复点；任何不可逆删除、DDL、凭据轮换或发布需独立批准。
- **停止条件：** 证据缺失、路径越界、父分支漂移、权限/安全合同不明、测试未运行或出现未批准偏差时停票。

## 9. 验收标准

- [ ] `AC-015`：旧入口引用为零且替代能力覆盖完整。
- [ ] `AC-015`：全Profile及主要消费者编译/测试通过。
- [ ] `AC-015`：当前接口不再让缺owner上下文的调用编译通过。
- [ ] `AC-015`：删除清单逐项有替代入口/调用方/验证证据。
- [ ] Ticket、Map、Spec、ADR 与 Evidence 状态一致；本票保持 draft/ready=false 直到用户接受并完成计划质量复核。
- [ ] 未通过删除测试、关闭类型/安全规则、吞错误、扩大权限或伪造运行证据获得通过。

## 10. Skill 调用计划

- 计划绑定：java-api-compatibility, namewta-fullstack-development。每项入口摘要、references、phase和失败动作以 frontmatter 为最低合同；执行前必须读取当前 Ticket、Map 与实际命中 references。
- 失败动作：必需入口、验证工具或安全/数据证据不可用则 block-ticket；不得以其他工具静默替换。

## 11. 交付与恢复

- **依赖：** T-14；完成顺序由 Tickets Map 控制。
- **检查点：** 记录实现前 base SHA、工作树、命令/退出码、偏差、最终 result SHA 及未运行项。
- **完成出口：** 只在用户批准、所有适用 AC/E2E/Evidence 通过、父分支包含结果后进入 done；当前审查阶段不进入 done。

## 12. 实施前源码定位与验证命令

I* 命名本身不是删除理由；清单逐方法列声明、实现、调用、反射/序列化、替代能力与验证。Profile person/enterprise 均需模块模式与合同验证；不能只验 person。EX-002 只有旧桥真正清零才更新，公共 wta-api 删除仍需明确移除版本、外部消费者与发布门槛；本 Ticket 不是实施或发布授权。

命令计划（全部 not-run）：

- 在 <Path>backend/</Path> 的 PowerShell 执行 `.\mvnw.cmd -pl wta-modules/wta-profile,wta-admin -am test`；Bash 使用 `./mvnw` 同参数入口。
- <Path>backend/pom.xml</Path> 的 Surefire groups 取 profiles.active，排除 exclude；核对目标 @Tag/环境门控及报告实际测试数。默认 test 退出 0 不等于隔离 e2e 已运行，SQL/Redis/Provider 故障矩阵须另建立真实测试并记录命令、环境、未跳过数和退出码。
- 在仓库根分别执行 `node .agents/skills/namewta-fullstack-development/scripts/validate-module-mode.mjs backend/wta-modules/wta-profile/wta-profile-person --mode layered` 和 `node .agents/skills/namewta-fullstack-development/scripts/validate-module-mode.mjs backend/wta-modules/wta-profile/wta-profile-enterprise --mode layered`。
- 后续 Skill 例外事实更新后，在仓库根执行 `node .agents/skills/engineering-standards/scripts/validate-skill-facts.mjs`。
