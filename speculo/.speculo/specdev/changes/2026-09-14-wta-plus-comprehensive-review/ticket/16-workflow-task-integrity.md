---
schema_version: 3
plan_contract_version: 1
skill_scan: "已扫描项目 .agents/skills/**/SKILL.md；仅绑定本票命中的入口，执行前按入口继续展开适用 references"
skill_bindings: [{"id":"engineering-standards","path":"<Path>.agents/skills/engineering-standards/SKILL.md</Path>","sha256":"ef0588daecab40e12e49e99d29a12bf107e887ab432e10a790bbc893fc42b0cf","phase":"plan","operation":"read-scope-contract-and-bind-acceptance","inputs":["当前审查候选、Ticket路径和上游ADR"],"outputs":["实现前边界、验证与失败停止条件"],"required":true,"on_failure":"block-ticket"},{"id":"namewta-fullstack-development","path":"<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>","sha256":"dd3e42720caa894bcb31ba1503b8211f669b3bbc2bb9ea3e40590a3a9edea395","phase":"plan","operation":"read-scope-contract-and-bind-acceptance","inputs":["当前审查候选、Ticket路径和上游ADR"],"outputs":["实现前边界、验证与失败停止条件"],"required":true,"on_failure":"block-ticket"},{"id":"wta-module-guide","path":"<Path>.agents/skills/wta-module-guide/SKILL.md</Path>","sha256":"f2e7e020552795c02285df65a941609fcd93692298951c012adbc555c20ac87f","phase":"plan","operation":"read-scope-contract-and-bind-acceptance","inputs":["当前审查候选、Ticket路径和上游ADR"],"outputs":["实现前边界、验证与失败停止条件"],"required":true,"on_failure":"block-ticket"}]
resource_claims: ["finding:F-03", "finding:B-10", "contract:AC-016"]
artifact: ticket
change: 2026-09-14-wta-plus-comprehensive-review
id: T-16
title: 保证流程任务读取和办理对象一致
status: draft
planning_depth: deep
planning_depth_reason: "本票涉及 F-03, B-10；需先完成证据/高影响取舍，不能直接实现。"
ready: false
risk: high
blocked_by: []
contract_ids: [AC-016]
owner: user-review
expected_changes: ["<Path>frontend/packages/web-domains/workflow/src/components/ProcessActionDialog.vue</Path>", "<Path>frontend/packages/web-domains/workflow/src/</Path>", "<Path>backend/wta-modules/wta-workflow/</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/</Path>", "<Path>frontend/e2e/</Path>"]
writable_paths: ["<Path>frontend/packages/web-domains/workflow/src/components/ProcessActionDialog.vue</Path>", "<Path>frontend/packages/web-domains/workflow/src/</Path>", "<Path>backend/wta-modules/wta-workflow/</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/</Path>", "<Path>frontend/e2e/</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/</Path>", "<Path>{roots.state}/specdev/adr/</Path>"]
shared_paths: []
shared_path_owners: []
---

# T-16：保证流程任务读取和办理对象一致

> 本 Ticket 是 <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/</Path> 内的待审实施计划。它不授权修改产品代码；只有用户明确接受该 Ticket 后，Lead 才能按 project Skill 建立实施 Evidence。

## 1. 战略与来源

- **Finding：** F-03, B-10。详细源码证据见专项审查报告。
- **目标：** 删除真实结构复杂性并恢复可验证的行为/安全/交付合同；不以移动代码、放宽门禁、删除测试或无限兼容层制造绿色。
- **可观察产出：** B失败绝不发出A的审批请求

## 2. 决策状态

- **当前状态：** draft，ready=false；高影响决策：B-10为likely，必须先证实WarmFlow权限行为再决定增补位置；不直接更换工作流引擎。
- **已锁定：** 本轮只写 change；实现、提交、发布、远程写入和永久知识修改均未授权。
- **未决：** 用户是否接受本票、外部合同/数据迁移/运行环境参数及是否进入下一 Work。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 固定A已打开、B加载失败后仍提交A的红灯；并独立验证getNextNodeList/listVariable是否被WarmFlow handler保护；每次open清空task，generation/AbortController绑定当前taskId，失败禁用所有动作；所有并发response与complete/back/operation检查当前generation和taskId；提交single-flight | 现有模块边界、权限、日志/错误合同中未被本票改变者 | 其他 finding 的实现、无证据的全仓重写、提交/部署、历史数据清理 |

## 4. 行为与实现契约

- **入口或 seam：** <Path>frontend/packages/web-domains/workflow/src/components/ProcessActionDialog.vue</Path>；由该 module 的公开 interface 接受输入。
- **输入/输出：** 输入、错误和状态必须保持对应现有合同；任何外部行为变化先更新 Spec/ADR并阻塞本票。
- **不变量：** 失败关闭、权限/Client上下文、资源所有权、事务/幂等和可观测性按适用工程规则成立。
- **错误：** 陌生用户读取任务节点/变量被拒，合法办理人/发起人按既有规则可读

## 5. 执行路线

1. 固定A已打开、B加载失败后仍提交A的红灯；并独立验证getNextNodeList/listVariable是否被WarmFlow handler保护。
2. 每次open先将task.value设为undefined，再递增generation绑定当前taskId；取消若走AbortController，需先证实现有HTTP端口是否透传signal。加载中或失败禁用所有动作，旧请求的finally不能解除新请求的loading。
3. 所有并发response与complete/back/operation检查当前generation和taskId，按后端实际合同校验任务版本；提交single-flight。F-03证明的是旧任务被误操作风险，是否越权必须单独验证后端权限，不能由前端竞态推断。
4. 统一任务详情/节点/变量读取的checkTaskReadAccess，若现有handler已覆盖则保留并补合同测试而不重复授权逻辑。
5. 服务端动作验证当前任务状态/操作者/版本，UI隐藏不替代权限。
6. 保持审批历史/附件/下一节点语义，关闭或切换时清资源。

## 6. 路径访问与所有权

- **可写候选：** <Path>frontend/packages/web-domains/workflow/src/components/ProcessActionDialog.vue</Path>, <Path>frontend/packages/web-domains/workflow/src/</Path>, <Path>backend/wta-modules/wta-workflow/</Path>, <Path>backend/wta-admin/src/test/java/org/namewta/test/</Path>, <Path>frontend/e2e/</Path>（仅用户批准后；越界必须停止）。
- **共享路径：** 无；若实现时发现与其他票交集，先由 Lead 串行化并修订 Map。
- **不动：** 供应商/历史archive、永久 namespace、用户未提交改动和生成物，除非本票修订后有明确 owner。

- **新增测试边界：** backend测试父目录候选只覆盖本票新建的workflow读取/办理合同用例；当前不存在test/workflow子目录，实施前按项目测试路由登记新测试文件，不借父目录范围修改其他业务测试。

## 7. 验证矩阵

| 行为/风险 | 接缝与方法 | 预期 | Evidence |
|---|---|---|---|
| AC 场景 1 | <Path>frontend/packages/web-domains/workflow/src/components/ProcessActionDialog.vue</Path> 定向测试/静态或隔离运行 | B失败绝不发出A的审批请求 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-16.md</Path> |
| AC 场景 2 | <Path>frontend/packages/web-domains/workflow/src/components/ProcessActionDialog.vue</Path> 定向测试/静态或隔离运行 | 陌生用户读取任务节点/变量被拒，合法办理人/发起人按既有规则可读 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-16.md</Path> |
| AC 场景 3 | <Path>frontend/packages/web-domains/workflow/src/components/ProcessActionDialog.vue</Path> 定向测试/静态或隔离运行 | 双击/过期任务不重复推进流程 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-16.md</Path> |
| AC 场景 4 | <Path>frontend/packages/web-domains/workflow/src/components/ProcessActionDialog.vue</Path> 定向测试/静态或隔离运行 | 快速切换、关闭、网络乱序与服务端失败可恢复 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-16.md</Path> |

- **Workspace checks：** 在frontend目录运行 `pnpm test`、`pnpm exec playwright test e2e/workflow-runtime.spec.ts e2e/workflow-definition.spec.ts`；在backend目录运行 `./mvnw -pl wta-modules/wta-workflow,wta-admin -am test`。本轮未运行实现验证。
- **E2E disposition：** required：跨模块/浏览器/外部服务行为必须由 Lead 在 current workspace 或 parent-candidate 验收。

## 8. 迁移、发布与恢复

- **迁移/兼容：** B-10为likely，必须先证实WarmFlow权限行为再决定增补位置；不直接更换工作流引擎。
- **回滚：** 先保留基线 SHA、产物/数据库备份及可验证恢复点；任何不可逆删除、DDL、凭据轮换或发布需独立批准。
- **停止条件：** 证据缺失、路径越界、父分支漂移、权限/安全合同不明、测试未运行或出现未批准偏差时停票。

## 9. 验收标准

- [ ] `AC-016`：B失败绝不发出A的审批请求。
- [ ] `AC-016`：陌生用户读取任务节点/变量被拒，合法办理人/发起人按既有规则可读。
- [ ] `AC-016`：双击/过期任务不重复推进流程。
- [ ] `AC-016`：快速切换、关闭、网络乱序与服务端失败可恢复。
- [ ] `AC-016`：打开新任务先清task并绑定新generation；旧响应/旧finally不写新会话，所有提交检查当前taskId及后端可用版本；B加载失败时无A提交请求。
- [ ] Ticket、Map、Spec、ADR 与 Evidence 状态一致；本票保持 draft/ready=false 直到用户接受并完成计划质量复核。
- [ ] 未通过删除测试、关闭类型/安全规则、吞错误、扩大权限或伪造运行证据获得通过。

## 10. Skill 调用计划

- 计划绑定：namewta-fullstack-development, wta-module-guide。每项入口摘要、references、phase和失败动作以 frontmatter 为最低合同；执行前必须读取当前 Ticket、Map 与实际命中 references。
- 失败动作：必需入口、验证工具或安全/数据证据不可用则 block-ticket；不得以其他工具静默替换。

## 11. 交付与恢复

- **依赖：** 无；完成顺序由 Tickets Map 控制。
- **检查点：** 记录实现前 base SHA、工作树、命令/退出码、偏差、最终 result SHA 及未运行项。
- **完成出口：** 只在用户批准、所有适用 AC/E2E/Evidence 通过、父分支包含结果后进入 done；当前审查阶段不进入 done。

## 12. 最新证据校正与具体检查

B-10为likely，必须先证实WarmFlow权限行为再决定增补位置；不直接更换工作流引擎。

- B失败绝不发出A的审批请求。
- 陌生用户读取任务节点/变量被拒，合法办理人/发起人按既有规则可读。
- 双击/过期任务不重复推进流程。
- 快速切换、关闭、网络乱序与服务端失败可恢复。

实际命令候选（实施前对照script重新解析，当前not-run）：

- `frontend: pnpm test; pnpm exec playwright test e2e/workflow-runtime.spec.ts e2e/workflow-definition.spec.ts`
- `backend: ./mvnw -pl wta-modules/wta-workflow,wta-admin -am test`
