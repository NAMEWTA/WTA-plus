---
schema_version: 3
plan_contract_version: 1
skill_scan: "已扫描项目 .agents/skills/**/SKILL.md；仅绑定本票命中的入口，执行前按入口继续展开适用 references"
skill_bindings: [{"id":"engineering-standards","path":"<Path>.agents/skills/engineering-standards/SKILL.md</Path>","sha256":"ef0588daecab40e12e49e99d29a12bf107e887ab432e10a790bbc893fc42b0cf","phase":"plan","operation":"read-scope-contract-and-bind-acceptance","inputs":["当前审查候选、Ticket路径和上游ADR"],"outputs":["实现前边界、验证与失败停止条件"],"required":true,"on_failure":"block-ticket"},{"id":"namewta-fullstack-development","path":"<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>","sha256":"dd3e42720caa894bcb31ba1503b8211f669b3bbc2bb9ea3e40590a3a9edea395","phase":"plan","operation":"read-scope-contract-and-bind-acceptance","inputs":["当前审查候选、Ticket路径和上游ADR"],"outputs":["实现前边界、验证与失败停止条件"],"required":true,"on_failure":"block-ticket"}]
resource_claims: ["finding:F-10", "contract:AC-017"]
artifact: ticket
change: 2026-09-14-wta-plus-comprehensive-review
id: T-17
title: 收紧流程设计器消息来源
status: draft
planning_depth: standard
planning_depth_reason: "本票涉及 F-10；需先完成证据/高影响取舍，不能直接实现。"
ready: false
risk: medium
blocked_by: []
contract_ids: [AC-017]
owner: user-review
expected_changes: ["<Path>frontend/packages/web-domains/workflow/src/definition/DesignPage.vue</Path>", "<Path>frontend/packages/web-domains/workflow/src/designer.ts</Path>", "<Path>frontend/packages/web-domains/workflow/src/runtime.ts</Path>", "<Path>frontend/apps/admin-web/src/router/adminManifestRegistry.ts</Path>", "<Path>frontend/e2e/</Path>"]
writable_paths: ["<Path>frontend/packages/web-domains/workflow/src/definition/DesignPage.vue</Path>", "<Path>frontend/packages/web-domains/workflow/src/designer.ts</Path>", "<Path>frontend/packages/web-domains/workflow/src/runtime.ts</Path>", "<Path>frontend/apps/admin-web/src/router/adminManifestRegistry.ts</Path>", "<Path>frontend/e2e/</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/</Path>", "<Path>{roots.state}/specdev/adr/</Path>"]
shared_paths: []
shared_path_owners: []
---

# T-17：收紧流程设计器消息来源

> 本 Ticket 是 <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/</Path> 内的待审实施计划。它不授权修改产品代码；只有用户明确接受该 Ticket 后，Lead 才能按 project Skill 建立实施 Evidence。

## 1. 战略与来源

- **Finding：** F-10。详细源码证据见专项审查报告。
- **目标：** 删除真实结构复杂性并恢复可验证的行为/安全/交付合同；不以移动代码、放宽门禁、删除测试或无限兼容层制造绿色。
- **可观察产出：** 错误origin、错误source、未知payload不能关闭标签

## 2. 决策状态

- **当前状态：** draft，ready=false；高影响决策：协议nonce支持取决于实际WarmFlow版本；当前必须的origin/source检查不等待扩展协议。
- **已锁定：** 本轮只写 change；实现、提交、发布、远程写入和永久知识修改均未授权。
- **未决：** 用户是否接受本票、外部合同/数据迁移/运行环境参数及是否进入下一 Work。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 复现非目标iframe或window发送close导致当前标签关闭；由App提供设计器允许origin，保留iframe WindowProxy作为source；在接收端精确校验origin/source及消息schema；若上游支持nonce则握手绑定，否则不虚构协议 | 现有模块边界、权限、日志/错误合同中未被本票改变者 | 其他 finding 的实现、无证据的全仓重写、提交/部署、历史数据清理 |

## 4. 行为与实现契约

- **入口或 seam：** <Path>frontend/packages/web-domains/workflow/src/definition/DesignPage.vue</Path>；由该 module 的公开 interface 接受输入。
- **输入/输出：** 输入、错误和状态必须保持对应现有合同；任何外部行为变化先更新 Spec/ADR并阻塞本票。
- **不变量：** 失败关闭、权限/Client上下文、资源所有权、事务/幂等和可观测性按适用工程规则成立。
- **错误：** 合法设计器close仍工作

## 5. 执行路线

1. 复现非目标iframe或window发送close导致当前标签关闭。
2. 由App提供设计器允许origin，保留iframe WindowProxy作为source。
3. 在接收端精确校验origin/source与close-only消息schema；根据当前WarmFlow版本核对nonce支持，支持时握手绑定，不支持时保留精确origin/source检查并记录版本限制。不得添加尚无消费者的save/publish协议。
4. 只有已知close事件可驱动导航，拒绝未知消息。
5. 卸载移除listener，重新加载iframe时更新身份。

## 6. 路径访问与所有权

- **可写候选：** <Path>frontend/packages/web-domains/workflow/src/definition/DesignPage.vue</Path>, <Path>frontend/packages/web-domains/workflow/src/designer.ts</Path>, <Path>frontend/packages/web-domains/workflow/src/runtime.ts</Path>, <Path>frontend/apps/admin-web/src/router/adminManifestRegistry.ts</Path>, <Path>frontend/e2e/</Path>（仅用户批准后；越界必须停止）。
- **共享路径：** 无；若实现时发现与其他票交集，先由 Lead 串行化并修订 Map。
- **不动：** 供应商/历史archive、永久 namespace、用户未提交改动和生成物，除非本票修订后有明确 owner。

## 7. 验证矩阵

| 行为/风险 | 接缝与方法 | 预期 | Evidence |
|---|---|---|---|
| AC 场景 1 | <Path>frontend/packages/web-domains/workflow/src/definition/DesignPage.vue</Path> 定向测试/静态或隔离运行 | 错误origin、错误source、未知payload不能关闭标签 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-17.md</Path> |
| AC 场景 2 | <Path>frontend/packages/web-domains/workflow/src/definition/DesignPage.vue</Path> 定向测试/静态或隔离运行 | 合法设计器close仍工作 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-17.md</Path> |
| AC 场景 3 | <Path>frontend/packages/web-domains/workflow/src/definition/DesignPage.vue</Path> 定向测试/静态或隔离运行 | 设计器刷新/卸载无重复listener | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-17.md</Path> |
| AC 场景 4 | <Path>frontend/packages/web-domains/workflow/src/definition/DesignPage.vue</Path> 定向测试/静态或隔离运行 | 不把未来save/publish消息自动加入允许列表 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-17.md</Path> |

- **Workspace checks：** 在frontend目录运行 `pnpm test`、`pnpm typecheck`、`pnpm exec playwright test e2e/workflow-definition.spec.ts --grep designer`，实际测试名包含designer。本轮未运行实现验证。
- **E2E disposition：** 按实际跨边界风险决定；未获用户批准前不声明通过。

## 8. 迁移、发布与恢复

- **迁移/兼容：** 协议nonce支持取决于实际WarmFlow版本；当前必须的origin/source检查不等待扩展协议。
- **回滚：** 先保留基线 SHA、产物/数据库备份及可验证恢复点；任何不可逆删除、DDL、凭据轮换或发布需独立批准。
- **停止条件：** 证据缺失、路径越界、父分支漂移、权限/安全合同不明、测试未运行或出现未批准偏差时停票。

## 9. 验收标准

- [ ] `AC-017`：错误origin、错误source、未知payload不能关闭标签。
- [ ] `AC-017`：合法设计器close仍工作。
- [ ] `AC-017`：设计器刷新/卸载无重复listener。
- [ ] `AC-017`：不把未来save/publish消息自动加入允许列表。
- [ ] `AC-017`：错误origin、同origin错误source、未知payload和旧iframe消息均无导航副作用；合法close保持可用，nonce只按当前WarmFlow真实协议实现。
- [ ] Ticket、Map、Spec、ADR 与 Evidence 状态一致；本票保持 draft/ready=false 直到用户接受并完成计划质量复核。
- [ ] 未通过删除测试、关闭类型/安全规则、吞错误、扩大权限或伪造运行证据获得通过。

## 10. Skill 调用计划

- 计划绑定：namewta-fullstack-development。每项入口摘要、references、phase和失败动作以 frontmatter 为最低合同；执行前必须读取当前 Ticket、Map 与实际命中 references。
- 失败动作：必需入口、验证工具或安全/数据证据不可用则 block-ticket；不得以其他工具静默替换。

## 11. 交付与恢复

- **依赖：** 无；完成顺序由 Tickets Map 控制。
- **检查点：** 记录实现前 base SHA、工作树、命令/退出码、偏差、最终 result SHA 及未运行项。
- **完成出口：** 只在用户批准、所有适用 AC/E2E/Evidence 通过、父分支包含结果后进入 done；当前审查阶段不进入 done。

## 12. 最新证据校正与具体检查

协议nonce支持取决于实际WarmFlow版本；当前必须的origin/source检查不等待扩展协议。

- 错误origin、错误source、未知payload不能关闭标签。
- 合法设计器close仍工作。
- 设计器刷新/卸载无重复listener。
- 不把未来save/publish消息自动加入允许列表。

实际命令候选（实施前对照script重新解析，当前not-run）：

- `frontend: pnpm test; pnpm typecheck; pnpm exec playwright test e2e/workflow-definition.spec.ts --grep designer`
