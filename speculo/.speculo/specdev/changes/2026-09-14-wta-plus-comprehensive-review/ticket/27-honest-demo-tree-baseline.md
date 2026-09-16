---
schema_version: 3
plan_contract_version: 1
skill_scan: "已扫描项目 .agents/skills/**/SKILL.md；仅绑定本票命中的入口，执行前按入口继续展开适用 references"
skill_bindings: [{"id":"engineering-standards","path":"<Path>.agents/skills/engineering-standards/SKILL.md</Path>","sha256":"ef0588daecab40e12e49e99d29a12bf107e887ab432e10a790bbc893fc42b0cf","phase":"plan","operation":"read-scope-contract-and-bind-acceptance","inputs":["当前审查候选、Ticket路径和上游ADR"],"outputs":["实现前边界、验证与失败停止条件"],"required":true,"on_failure":"block-ticket"},{"id":"namewta-fullstack-development","path":"<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>","sha256":"dd3e42720caa894bcb31ba1503b8211f669b3bbc2bb9ea3e40590a3a9edea395","phase":"plan","operation":"read-scope-contract-and-bind-acceptance","inputs":["当前审查候选、Ticket路径和上游ADR"],"outputs":["实现前边界、验证与失败停止条件"],"required":true,"on_failure":"block-ticket"}]
resource_claims: ["finding:B-14", "contract:AC-027"]
artifact: ticket
change: 2026-09-14-wta-plus-comprehensive-review
id: T-27
title: 修复Demo树样例并删除误导占位实现
status: draft
planning_depth: standard
planning_depth_reason: "本票涉及 B-14；需先完成证据/高影响取舍，不能直接实现。"
ready: false
risk: medium
blocked_by: ["T-26"]
contract_ids: [AC-027]
owner: user-review
expected_changes: ["<Path>backend/wta-modules/wta-demo/</Path>", "<Path>frontend/packages/domains/demo/</Path>", "<Path>frontend/packages/web-domains/demo/</Path>", "<Path>docs/fm/</Path>", "<Path>backend/wta-admin/src/test/</Path>"]
writable_paths: ["<Path>backend/wta-modules/wta-demo/</Path>", "<Path>frontend/packages/domains/demo/</Path>", "<Path>frontend/packages/web-domains/demo/</Path>", "<Path>docs/fm/</Path>", "<Path>backend/wta-admin/src/test/</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/</Path>", "<Path>{roots.state}/specdev/adr/</Path>"]
shared_paths: []
shared_path_owners: []
---

# T-27：修复Demo树样例并删除误导占位实现

> 本 Ticket 是 <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/</Path> 内的待审实施计划。它不授权修改产品代码；只有用户明确接受该 Ticket 后，Lead 才能按 project Skill 建立实施 Evidence。

## 1. 战略与来源

- **Finding：** B-14。详细源码证据见专项审查报告。
- **目标：** 删除真实结构复杂性并恢复可验证的行为/安全/交付合同；不以移动代码、放宽门禁、删除测试或无限兼容层制造绿色。
- **可观察产出：** 保存/删除路径不再有虚假校验TODO

## 2. 决策状态

- **当前状态：** draft，ready=false；高影响决策：ADR-CR-008推荐修好样例；移除demo/default bundle是另一个待审产品选择。
- **已锁定：** 本轮只写 change；实现、提交、发布、远程写入和永久知识修改均未授权。
- **未决：** 用户是否接受本票、外部合同/数据迁移/运行环境参数及是否进入下一 Work。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 确认Demo仍是现有bundle-full中的主动示例，不因名称demo直接删产品能力；推荐实现最小保存/删除不变量并删除TODO分支：合法parent、无环、有子节点删除策略；明确重复名称是否真业务规则，未声明则不强加唯一性 | 现有模块边界、权限、日志/错误合同中未被本票改变者 | 其他 finding 的实现、无证据的全仓重写、提交/部署、历史数据清理 |

## 4. 行为与实现契约

- **入口或 seam：** <Path>backend/wta-modules/wta-demo/</Path>；由该 module 的公开 interface 接受输入。
- **输入/输出：** 输入、错误和状态必须保持对应现有合同；任何外部行为变化先更新 Spec/ADR并阻塞本票。
- **不变量：** 失败关闭、权限/Client上下文、资源所有权、事务/幂等和可观测性按适用工程规则成立。
- **错误：** 非法parent、环、带子节点删除按明确合同处理

## 5. 执行路线

1. 确认Demo仍是现有bundle-full中的主动示例，不因名称demo直接删产品能力。
2. 推荐实现最小保存/删除不变量并删除TODO分支：合法parent、无环、有子节点删除策略。
3. 明确重复名称是否真业务规则，未声明则不强加唯一性。
4. 使静态模板与示例的树合同一致，新增负向样例供其他AI参考。
5. 若用户选择移出默认bundle，另同步POM/菜单/App与文档，不在此草案默认移除。

## 6. 路径访问与所有权

- **可写候选：** <Path>backend/wta-modules/wta-demo/</Path>, <Path>frontend/packages/domains/demo/</Path>, <Path>frontend/packages/web-domains/demo/</Path>, <Path>docs/fm/</Path>, <Path>backend/wta-admin/src/test/</Path>（仅后续用户批准实施时；本轮只修改 Ticket）。
- **共享路径：** 与 Map 中占用相同模块、DDL、测试或 App 的票存在交集；以 Map 的资源 owner 和串行阶段为准，进入 Ready 前必须明确写集，不能据本票空 shared_paths 推断可并行。
- **不动：** 供应商/历史archive、永久 namespace、用户未提交改动和生成物，除非本票修订后有明确 owner。

## 7. 验证矩阵

| 行为/风险 | 接缝与方法 | 预期 | Evidence |
|---|---|---|---|
| AC 场景 1 | <Path>backend/wta-modules/wta-demo/</Path> 定向测试/静态或隔离运行 | 保存/删除路径不再有虚假校验TODO | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-27.md</Path> |
| AC 场景 2 | <Path>backend/wta-modules/wta-demo/</Path> 定向测试/静态或隔离运行 | 非法parent、环、带子节点删除按明确合同处理 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-27.md</Path> |
| AC 场景 3 | <Path>backend/wta-modules/wta-demo/</Path> 定向测试/静态或隔离运行 | 模板代表输出可编译且与前后端树语义一致 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-27.md</Path> |
| AC 场景 4 | <Path>backend/wta-modules/wta-demo/</Path> 定向测试/静态或隔离运行 | 保留演示所需权限/日志，不把示例缺陷推广到System | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-27.md</Path> |

- **Workspace checks：** 见 §12 的实际工作目录、命令和隔离验收边界；本轮全部为计划，未执行 Maven、构建或外部服务测试。
- **E2E disposition：** 按实际跨边界风险决定；未获用户批准前不声明通过。

## 8. 迁移、发布与恢复

- **迁移/兼容：** ADR-CR-008推荐修好样例；移除demo/default bundle是另一个待审产品选择。
- **回滚：** 先保留基线 SHA、产物/数据库备份及可验证恢复点；任何不可逆删除、DDL、凭据轮换或发布需独立批准。
- **停止条件：** 证据缺失、路径越界、父分支漂移、权限/安全合同不明、测试未运行或出现未批准偏差时停票。

## 9. 验收标准

- [ ] `AC-027`：保存/删除路径不再有虚假校验TODO。
- [ ] `AC-027`：非法parent、环、带子节点删除按明确合同处理。
- [ ] `AC-027`：模板代表输出可编译且与前后端树语义一致。
- [ ] `AC-027`：保留演示所需权限/日志，不把示例缺陷推广到System。
- [ ] Ticket、Map、Spec、ADR 与 Evidence 状态一致；本票保持 draft/ready=false 直到用户接受并完成计划质量复核。
- [ ] 未通过删除测试、关闭类型/安全规则、吞错误、扩大权限或伪造运行证据获得通过。

## 10. Skill 调用计划

- 计划绑定：namewta-fullstack-development。每项入口摘要、references、phase和失败动作以 frontmatter 为最低合同；执行前必须读取当前 Ticket、Map 与实际命中 references。
- 失败动作：必需入口、验证工具或安全/数据证据不可用则 block-ticket；不得以其他工具静默替换。

## 11. 交付与恢复

- **依赖：** T-26；完成顺序由 Tickets Map 控制。
- **检查点：** 记录实现前 base SHA、工作树、命令/退出码、偏差、最终 result SHA 及未运行项。
- **完成出口：** 只在用户批准、所有适用 AC/E2E/Evidence 通过、父分支包含结果后进入 done；当前审查阶段不进入 done。

## 12. 实施前源码定位与验证命令

具体 TODO 位于 <Path>backend/wta-modules/wta-demo/src/main/java/org/namewta/demo/service/impl/TestTreeServiceImpl.java</Path>。规则至少包含有效 parent、拒绝环、有子节点删除策略及并发重验；不能仅删除 TODO 或加前端判断。名称唯一性不是现有事实，未声明不强加。<Path>docs/fm/</Path> 代表输出需要编译与负向场景，静态模板 validate 不能代替输出验证。Demo 仍在 bundle-full；B-16 过宽依赖候选已 rejected，不借本票再次拆包。

命令计划（全部 not-run）：

- 在 <Path>backend/</Path> 的 PowerShell 执行 `.\mvnw.cmd -pl wta-modules/wta-demo,wta-admin -am test`；Bash 使用 `./mvnw` 同参数入口。
- <Path>backend/pom.xml</Path> 的 Surefire groups 取 profiles.active，排除 exclude；核对目标 @Tag/环境门控及报告实际测试数。默认 test 退出 0 不等于隔离 e2e 已运行，SQL/Redis/Provider 故障矩阵须另建立真实测试并记录命令、环境、未跳过数和退出码。
- 在 <Path>frontend/</Path> 逐条执行：`pnpm --filter @namewta/domain-demo test`、`pnpm --filter @namewta/web-domain-demo test`。
- 在仓库根执行 `node docs/fm/scripts/validate.mjs`，并按模板代表输出的真实 POM/TS 配置编译。
