---
schema_version: 3
plan_contract_version: 1
skill_scan: "已扫描项目 .agents/skills/**/SKILL.md；仅绑定本票命中的入口，执行前按入口继续展开适用 references"
skill_bindings: [{"id":"engineering-standards","path":"<Path>.agents/skills/engineering-standards/SKILL.md</Path>","sha256":"ef0588daecab40e12e49e99d29a12bf107e887ab432e10a790bbc893fc42b0cf","phase":"plan","operation":"read-scope-contract-and-bind-acceptance","inputs":["当前审查候选、Ticket路径和上游ADR"],"outputs":["实现前边界、验证与失败停止条件"],"required":true,"on_failure":"block-ticket"},{"id":"namewta-fullstack-development","path":"<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>","sha256":"dd3e42720caa894bcb31ba1503b8211f669b3bbc2bb9ea3e40590a3a9edea395","phase":"plan","operation":"read-scope-contract-and-bind-acceptance","inputs":["当前审查候选、Ticket路径和上游ADR"],"outputs":["实现前边界、验证与失败停止条件"],"required":true,"on_failure":"block-ticket"}]
resource_claims: ["finding:F-08", "contract:AC-019"]
artifact: ticket
change: 2026-09-14-wta-plus-comprehensive-review
id: T-19
title: 收敛System大页面中的异步状态与重复封装
status: draft
planning_depth: standard
planning_depth_reason: "本票涉及 F-08；需先完成证据/高影响取舍，不能直接实现。"
ready: false
risk: medium
blocked_by: ["T-18"]
contract_ids: [AC-019]
owner: user-review
expected_changes: ["<Path>frontend/packages/web-domains/system/src/user/</Path>", "<Path>frontend/packages/web-domains/system/src/role/</Path>", "<Path>frontend/packages/web-domains/system/src/menu/</Path>", "<Path>frontend/packages/web-domains/system/src/composables.ts</Path>", "<Path>frontend/apps/admin-web/src/hooks/async/useLoading.ts</Path>", "<Path>frontend/apps/admin-web/src/hooks/dialog/useDialogState.ts</Path>", "<Path>frontend/apps/admin-web/src/hooks/dialog/useFormDialog.ts</Path>", "<Path>frontend/apps/admin-web/src/hooks/form/useSearchReset.ts</Path>", "<Path>frontend/packages/web-domains/workflow/src/composables.ts</Path>", "<Path>frontend/packages/web-domains/demo/src/composables.ts</Path>", "<Path>frontend/packages/adapters/axios-browser/</Path>", "<Path>frontend/e2e/</Path>"]
writable_paths: ["<Path>frontend/packages/web-domains/system/src/user/</Path>", "<Path>frontend/packages/web-domains/system/src/role/</Path>", "<Path>frontend/packages/web-domains/system/src/menu/</Path>", "<Path>frontend/packages/web-domains/system/src/composables.ts</Path>", "<Path>frontend/apps/admin-web/src/hooks/async/useLoading.ts</Path>", "<Path>frontend/apps/admin-web/src/hooks/dialog/useDialogState.ts</Path>", "<Path>frontend/apps/admin-web/src/hooks/dialog/useFormDialog.ts</Path>", "<Path>frontend/apps/admin-web/src/hooks/form/useSearchReset.ts</Path>", "<Path>frontend/packages/web-domains/workflow/src/composables.ts</Path>", "<Path>frontend/packages/web-domains/demo/src/composables.ts</Path>", "<Path>frontend/packages/adapters/axios-browser/</Path>", "<Path>frontend/e2e/</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/</Path>", "<Path>{roots.state}/specdev/adr/</Path>"]
shared_paths: []
shared_path_owners: []
---

# T-19：收敛System大页面中的异步状态与重复封装

> 本 Ticket 是 <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/</Path> 内的待审实施计划。它不授权修改产品代码；只有用户明确接受该 Ticket 后，Lead 才能按 project Skill 建立实施 Evidence。

## 1. 战略与来源

- **Finding：** F-08。详细源码证据见专项审查报告。
- **目标：** 删除真实结构复杂性并恢复可验证的行为/安全/交付合同；不以移动代码、放宽门禁、删除测试或无限兼容层制造绿色。
- **可观察产出：** 快速筛选时旧响应不能覆盖新列表，失败/loading可恢复

## 2. 决策状态

- **当前状态：** draft，ready=false；高影响决策：ADR-CR-007保留domain/web-domain/App主轴；实际竞态需代表页面定向证实。
- **已锁定：** 本轮只写 change；实现、提交、发布、远程写入和永久知识修改均未授权。
- **未决：** 用户是否接受本票、外部合同/数据迁移/运行环境参数及是否进入下一 Work。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 分别列User/Role/Menu的查询、表单、权限树、导入与选择状态及其owner；先修真实乱序覆盖与loading竞态，不以行数目标机械拆文件；按query/form/permission/import等可命名职责提取局部module，保留页面组合与业务所有权 | 现有模块边界、权限、日志/错误合同中未被本票改变者 | 其他 finding 的实现、无证据的全仓重写、提交/部署、历史数据清理 |

## 4. 行为与实现契约

- **入口或 seam：** <Path>frontend/packages/web-domains/system/src/user/</Path>；由该 module 的公开 interface 接受输入。
- **输入/输出：** 输入、错误和状态必须保持对应现有合同；任何外部行为变化先更新 Spec/ADR并阻塞本票。
- **不变量：** 失败关闭、权限/Client上下文、资源所有权、事务/幂等和可观测性按适用工程规则成立。
- **错误：** 权限树与用户编辑状态相互独立

## 5. 执行路线

1. 分别列User/Role/Menu的查询、表单、权限树、导入与选择状态及其owner。
2. 当前优先用generation绑定查询参数与Client，只有当前generation可以写入list、error或结束loading。HttpRequest当前无signal；若需真实取消，再列明platform/contracts/src/index.ts与axios adapter的接口变更并修订写集，不能把AbortController当作已存在能力。
3. 分别固定User/Role/Menu的A Client慢响应晚于B Client响应返回的红灯，确认旧响应不能覆盖新列表或结束新loading；再拆职责，不以行数目标机械拆文件。
4. 按query/form/permission/import等可命名职责提取局部module，保留页面组合与业务所有权。
5. 对system/workflow/demo composables.ts及Admin useLoading/useDialogState/useFormDialog/useSearchReset逐个比较调用者和取消、错误语义；只在真实同语义消费者层提共享primitive。web-kit与domains/system不作为默认写集，不上收所有表单状态。
6. 删除已被新owner替代的旧分支与无用barrel，保留公开exports/manifest。

## 6. 路径访问与所有权

- **可写候选：** <Path>frontend/packages/web-domains/system/src/user/</Path>, <Path>frontend/packages/web-domains/system/src/role/</Path>, <Path>frontend/packages/web-domains/system/src/menu/</Path>, <Path>frontend/packages/web-domains/system/src/composables.ts</Path>, <Path>frontend/apps/admin-web/src/hooks/async/useLoading.ts</Path>, <Path>frontend/apps/admin-web/src/hooks/dialog/useDialogState.ts</Path>, <Path>frontend/apps/admin-web/src/hooks/dialog/useFormDialog.ts</Path>, <Path>frontend/apps/admin-web/src/hooks/form/useSearchReset.ts</Path>, <Path>frontend/packages/web-domains/workflow/src/composables.ts</Path>, <Path>frontend/packages/web-domains/demo/src/composables.ts</Path>, <Path>frontend/packages/adapters/axios-browser/</Path>, <Path>frontend/e2e/</Path>（仅用户批准后；越界必须停止）。
- **共享路径：** 无；若实现时发现与其他票交集，先由 Lead 串行化并修订 Map。
- **不动：** 供应商/历史archive、永久 namespace、用户未提交改动和生成物，除非本票修订后有明确 owner。

## 7. 验证矩阵

| 行为/风险 | 接缝与方法 | 预期 | Evidence |
|---|---|---|---|
| AC 场景 1 | <Path>frontend/packages/web-domains/system/src/user/</Path> 定向测试/静态或隔离运行 | 快速筛选时旧响应不能覆盖新列表，失败/loading可恢复 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-19.md</Path> |
| AC 场景 2 | <Path>frontend/packages/web-domains/system/src/user/</Path> 定向测试/静态或隔离运行 | 权限树与用户编辑状态相互独立 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-19.md</Path> |
| AC 场景 3 | <Path>frontend/packages/web-domains/system/src/user/</Path> 定向测试/静态或隔离运行 | 提取前后功能/权限/排序/分页行为相同 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-19.md</Path> |
| AC 场景 4 | <Path>frontend/packages/web-domains/system/src/user/</Path> 定向测试/静态或隔离运行 | 每个超过1k行文件有职责删除或保留理由，无同义薄wrapper堆叠 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-19.md</Path> |

- **Workspace checks：** frontend: pnpm architecture:check; pnpm lint; pnpm typecheck; pnpm test; pnpm build:prod。本轮未运行实现验证。
- **E2E disposition：** 按实际跨边界风险决定；未获用户批准前不声明通过。

## 8. 迁移、发布与恢复

- **迁移/兼容：** ADR-CR-007保留domain/web-domain/App主轴；实际竞态需代表页面定向证实。
- **回滚：** 先保留基线 SHA、产物/数据库备份及可验证恢复点；任何不可逆删除、DDL、凭据轮换或发布需独立批准。
- **停止条件：** 证据缺失、路径越界、父分支漂移、权限/安全合同不明、测试未运行或出现未批准偏差时停票。

## 9. 验收标准

- [ ] `AC-019`：快速筛选时旧响应不能覆盖新列表，失败/loading可恢复。
- [ ] `AC-019`：权限树与用户编辑状态相互独立。
- [ ] `AC-019`：提取前后功能/权限/排序/分页行为相同。
- [ ] `AC-019`：每个超过1k行文件有职责删除或保留理由，无同义薄wrapper堆叠。
- [ ] `AC-019`：User/Role/Menu分别覆盖A Client慢响应晚于B返回，旧请求不能覆盖B列表或结束B loading；取消合同若扩展必须先补精确写集。
- [ ] Ticket、Map、Spec、ADR 与 Evidence 状态一致；本票保持 draft/ready=false 直到用户接受并完成计划质量复核。
- [ ] 未通过删除测试、关闭类型/安全规则、吞错误、扩大权限或伪造运行证据获得通过。

## 10. Skill 调用计划

- 计划绑定：namewta-fullstack-development。每项入口摘要、references、phase和失败动作以 frontmatter 为最低合同；执行前必须读取当前 Ticket、Map 与实际命中 references。
- 失败动作：必需入口、验证工具或安全/数据证据不可用则 block-ticket；不得以其他工具静默替换。

## 11. 交付与恢复

- **依赖：** T-18；完成顺序由 Tickets Map 控制。
- **检查点：** 记录实现前 base SHA、工作树、命令/退出码、偏差、最终 result SHA 及未运行项。
- **完成出口：** 只在用户批准、所有适用 AC/E2E/Evidence 通过、父分支包含结果后进入 done；当前审查阶段不进入 done。

## 12. 最新证据校正与具体检查

ADR-CR-007保留domain/web-domain/App主轴；实际竞态需代表页面定向证实。

- 快速筛选时旧响应不能覆盖新列表，失败/loading可恢复。
- 权限树与用户编辑状态相互独立。
- 提取前后功能/权限/排序/分页行为相同。
- 每个超过1k行文件有职责删除或保留理由，无同义薄wrapper堆叠。

实际命令候选（实施前对照script重新解析，当前not-run）：

- `frontend: pnpm architecture:check; pnpm lint; pnpm typecheck; pnpm test; pnpm build:prod`
