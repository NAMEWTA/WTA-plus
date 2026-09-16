---
schema_version: 3
plan_contract_version: 1
skill_scan: "已扫描项目 .agents/skills/**/SKILL.md；仅绑定本票命中的入口，执行前按入口继续展开适用 references"
skill_bindings: [{"id":"engineering-standards","path":"<Path>.agents/skills/engineering-standards/SKILL.md</Path>","sha256":"ef0588daecab40e12e49e99d29a12bf107e887ab432e10a790bbc893fc42b0cf","phase":"plan","operation":"read-scope-contract-and-bind-acceptance","inputs":["当前审查候选、Ticket路径和上游ADR"],"outputs":["实现前边界、验证与失败停止条件"],"required":true,"on_failure":"block-ticket"},{"id":"namewta-fullstack-development","path":"<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>","sha256":"dd3e42720caa894bcb31ba1503b8211f669b3bbc2bb9ea3e40590a3a9edea395","phase":"plan","operation":"read-scope-contract-and-bind-acceptance","inputs":["当前审查候选、Ticket路径和上游ADR"],"outputs":["实现前边界、验证与失败停止条件"],"required":true,"on_failure":"block-ticket"}]
resource_claims: ["finding:F-04", "finding:F-05", "contract:AC-012"]
artifact: ticket
change: 2026-09-14-wta-plus-comprehensive-review
id: T-12
title: 统一幂等会话清理与导航恢复状态
status: draft
planning_depth: standard
planning_depth_reason: "本票涉及 F-04, F-05；需先完成证据/高影响取舍，不能直接实现。"
ready: false
risk: high
blocked_by: ["T-07"]
contract_ids: [AC-012]
owner: user-review
expected_changes: ["<Path>frontend/apps/admin-web/src/store/</Path>", "<Path>frontend/apps/admin-web/src/permission.ts</Path>", "<Path>frontend/apps/admin-web/src/application/</Path>", "<Path>frontend/apps/home-web/src/store/</Path>", "<Path>frontend/apps/home-web/src/router/</Path>", "<Path>frontend/e2e/</Path>", "<Path>frontend/apps/admin-web/src/router/</Path>", "<Path>frontend/apps/home-web/src/application/session.ts</Path>"]
writable_paths: ["<Path>frontend/apps/admin-web/src/store/</Path>", "<Path>frontend/apps/admin-web/src/permission.ts</Path>", "<Path>frontend/apps/admin-web/src/application/</Path>", "<Path>frontend/apps/home-web/src/store/</Path>", "<Path>frontend/apps/home-web/src/router/</Path>", "<Path>frontend/e2e/</Path>", "<Path>frontend/apps/admin-web/src/router/</Path>", "<Path>frontend/apps/home-web/src/application/session.ts</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/</Path>", "<Path>{roots.state}/specdev/adr/</Path>"]
shared_paths: []
shared_path_owners: []
---

# T-12：统一幂等会话清理与导航恢复状态

> 本 Ticket 是 <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/</Path> 内的待审实施计划。它不授权修改产品代码；只有用户明确接受该 Ticket 后，Lead 才能按 project Skill 建立实施 Evidence。

## 1. 战略与来源

- **Finding：** F-04, F-05。详细源码证据见专项审查报告。
- **目标：** 删除真实结构复杂性并恢复可验证的行为/安全/交付合同；不以移动代码、放宽门禁、删除测试或无限兼容层制造绿色。
- **可观察产出：** logout超时/401/离线时本地token和动态路由仍清空

## 2. 决策状态

- **当前状态：** draft，ready=false；高影响决策：ADR-CR-007保留各App路由owner，共享稳定机制；本地退出不伪称远端token已撤销。
- **已锁定：** 本轮只写 change；实现、提交、发布、远程写入和永久知识修改均未授权。
- **未决：** 用户是否接受本票、外部合同/数据迁移/运行环境参数及是否进入下一 Work。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 固定logout网络失败后token残留及Home空roles恢复次数红灯；用identityLoaded/navigationLoaded等状态表达初始化，合法空角色不再表示未登录；建立幂等local teardown：token、identity、权限投影、addRoute回收、SSE/Push和待处理请求一起清理 | 现有模块边界、权限、日志/错误合同中未被本票改变者 | 其他 finding 的实现、无证据的全仓重写、提交/部署、历史数据清理 |

## 4. 行为与实现契约

- **入口或 seam：** <Path>frontend/apps/admin-web/src/store/</Path>；由该 module 的公开 interface 接受输入。
- **输入/输出：** 输入、错误和状态必须保持对应现有合同；任何外部行为变化先更新 Spec/ADR并阻塞本票。
- **不变量：** 失败关闭、权限/Client上下文、资源所有权、事务/幂等和可观测性按适用工程规则成立。
- **错误：** 空角色账户每次恢复最多一次，不循环replace

## 5. 执行路线

1. 固定Admin logout网络失败后的本地清理缺失与Home空roles恢复循环红灯；Home已有finally清token作为反证保留。
2. 用identityLoaded/navigationLoaded等状态表达初始化，合法空角色不再表示未登录。
3. 建立幂等local teardown：token、identity、权限投影、addRoute回收、SSE/Push和待处理请求一起清理。各App导航owner记录自己addRoute返回的移除回调或稳定route name，退出只回收本会话拥有的动态路由并保留静态路由；不能只清store而保留Vue Router记录。
4. Admin在远端logout失败时仍执行本地finally teardown；Home保留已有finally，补异常后的导航收束与动态route回收。远端失败不得伪称服务端token已撤销；401恢复single-flight避免弹窗/请求风暴。
5. 退出与重新登录不同Client后重建导航，不复用旧角色或路由。

## 6. 路径访问与所有权

- **可写候选：** <Path>frontend/apps/admin-web/src/store/</Path>, <Path>frontend/apps/admin-web/src/permission.ts</Path>, <Path>frontend/apps/admin-web/src/application/</Path>, <Path>frontend/apps/home-web/src/store/</Path>, <Path>frontend/apps/home-web/src/router/</Path>, <Path>frontend/e2e/</Path>, <Path>frontend/apps/admin-web/src/router/</Path>, <Path>frontend/apps/home-web/src/application/session.ts</Path>（仅用户批准后；越界必须停止）。
- **共享路径：** 无；若实现时发现与其他票交集，先由 Lead 串行化并修订 Map。
- **不动：** 供应商/历史archive、永久 namespace、用户未提交改动和生成物，除非本票修订后有明确 owner。

## 7. 验证矩阵

| 行为/风险 | 接缝与方法 | 预期 | Evidence |
|---|---|---|---|
| AC 场景 1 | <Path>frontend/apps/admin-web/src/store/</Path> 定向测试/静态或隔离运行 | logout超时/401/离线时本地token和动态路由仍清空 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-12.md</Path> |
| AC 场景 2 | <Path>frontend/apps/admin-web/src/store/</Path> 定向测试/静态或隔离运行 | 空角色账户每次恢复最多一次，不循环replace | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-12.md</Path> |
| AC 场景 3 | <Path>frontend/apps/admin-web/src/store/</Path> 定向测试/静态或隔离运行 | 切Client无旧菜单/权限残留，服务端授权仍为最终门禁 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-12.md</Path> |
| AC 场景 4 | <Path>frontend/apps/admin-web/src/store/</Path> 定向测试/静态或隔离运行 | 并发401只有一次恢复流程并可终止 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-12.md</Path> |

- **Workspace checks：** frontend: pnpm test; pnpm typecheck; pnpm test:e2e。本轮未运行实现验证。
- **E2E disposition：** required：跨模块/浏览器/外部服务行为必须由 Lead 在 current workspace 或 parent-candidate 验收。

## 8. 迁移、发布与恢复

- **迁移/兼容：** ADR-CR-007保留各App路由owner，共享稳定机制；本地退出不伪称远端token已撤销。
- **回滚：** 先保留基线 SHA、产物/数据库备份及可验证恢复点；任何不可逆删除、DDL、凭据轮换或发布需独立批准。
- **停止条件：** 证据缺失、路径越界、父分支漂移、权限/安全合同不明、测试未运行或出现未批准偏差时停票。

## 9. 验收标准

- [ ] `AC-012`：logout超时/401/离线时本地token和动态路由仍清空。
- [ ] `AC-012`：空角色账户每次恢复最多一次，不循环replace。
- [ ] `AC-012`：切Client无旧菜单/权限残留，服务端授权仍为最终门禁。
- [ ] `AC-012`：并发401只有一次恢复流程并可终止。
- [ ] `AC-012`：Admin远端logout失败仍清本地；Home保留既有finally。每个动态route有创建owner与回收记录，退出后静态route保留；合法零角色会话只初始化一次。
- [ ] Ticket、Map、Spec、ADR 与 Evidence 状态一致；本票保持 draft/ready=false 直到用户接受并完成计划质量复核。
- [ ] 未通过删除测试、关闭类型/安全规则、吞错误、扩大权限或伪造运行证据获得通过。

## 10. Skill 调用计划

- 计划绑定：namewta-fullstack-development。每项入口摘要、references、phase和失败动作以 frontmatter 为最低合同；执行前必须读取当前 Ticket、Map 与实际命中 references。
- 失败动作：必需入口、验证工具或安全/数据证据不可用则 block-ticket；不得以其他工具静默替换。

## 11. 交付与恢复

- **依赖：** T-07；完成顺序由 Tickets Map 控制。
- **检查点：** 记录实现前 base SHA、工作树、命令/退出码、偏差、最终 result SHA 及未运行项。
- **完成出口：** 只在用户批准、所有适用 AC/E2E/Evidence 通过、父分支包含结果后进入 done；当前审查阶段不进入 done。

## 12. 最新证据校正与具体检查

ADR-CR-007保留各App路由owner，共享稳定机制；本地退出不伪称远端token已撤销。 Home已有finally清token必须保留，修复其异常后导航和route回收；不再报告Home token残留。

- logout超时/401/离线时本地token和动态路由仍清空。
- 空角色账户每次恢复最多一次，不循环replace。
- 切Client无旧菜单/权限残留，服务端授权仍为最终门禁。
- 并发401只有一次恢复流程并可终止。

实际命令候选（实施前对照script重新解析，当前not-run）：

- `frontend: pnpm test; pnpm typecheck; pnpm test:e2e`
