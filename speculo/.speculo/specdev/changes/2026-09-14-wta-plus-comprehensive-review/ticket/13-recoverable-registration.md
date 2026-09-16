---
schema_version: 3
plan_contract_version: 1
skill_scan: "已扫描项目 .agents/skills/**/SKILL.md；仅绑定本票命中的入口，执行前按入口继续展开适用 references"
skill_bindings: [{"id":"engineering-standards","path":"<Path>.agents/skills/engineering-standards/SKILL.md</Path>","sha256":"ef0588daecab40e12e49e99d29a12bf107e887ab432e10a790bbc893fc42b0cf","phase":"plan","operation":"read-scope-contract-and-bind-acceptance","inputs":["当前审查候选、Ticket路径和上游ADR"],"outputs":["实现前边界、验证与失败停止条件"],"required":true,"on_failure":"block-ticket"},{"id":"namewta-fullstack-development","path":"<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>","sha256":"dd3e42720caa894bcb31ba1503b8211f669b3bbc2bb9ea3e40590a3a9edea395","phase":"plan","operation":"read-scope-contract-and-bind-acceptance","inputs":["当前审查候选、Ticket路径和上游ADR"],"outputs":["实现前边界、验证与失败停止条件"],"required":true,"on_failure":"block-ticket"}]
resource_claims: ["finding:F-06", "contract:AC-013"]
artifact: ticket
change: 2026-09-14-wta-plus-comprehensive-review
id: T-13
title: 完成Home注册开关与验证码重试交互
status: draft
planning_depth: standard
planning_depth_reason: "本票涉及 F-06；需先完成证据/高影响取舍，不能直接实现。"
ready: false
risk: medium
blocked_by: ["T-12"]
contract_ids: [AC-013]
owner: user-review
expected_changes: ["<Path>frontend/apps/home-web/src/views/RegisterPage.vue</Path>", "<Path>frontend/apps/home-web/src/router/</Path>", "<Path>frontend/packages/domains/admin/</Path>", "<Path>frontend/e2e/</Path>"]
writable_paths: ["<Path>frontend/apps/home-web/src/views/RegisterPage.vue</Path>", "<Path>frontend/apps/home-web/src/router/</Path>", "<Path>frontend/packages/domains/admin/</Path>", "<Path>frontend/e2e/</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/</Path>", "<Path>{roots.state}/specdev/adr/</Path>"]
shared_paths: []
shared_path_owners: []
---

# T-13：完成Home注册开关与验证码重试交互

> 本 Ticket 是 <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/</Path> 内的待审实施计划。它不授权修改产品代码；只有用户明确接受该 Ticket 后，Lead 才能按 project Skill 建立实施 Evidence。

## 1. 战略与来源

- **Finding：** F-06。详细源码证据见专项审查报告。
- **目标：** 删除真实结构复杂性并恢复可验证的行为/安全/交付合同；不以移动代码、放宽门禁、删除测试或无限兼容层制造绿色。
- **可观察产出：** 服务端关闭注册时入口与页面均准确，后端仍拒绝直接调用

## 2. 决策状态

- **当前状态：** draft，ready=false；高影响决策：不改变密码策略和匿名注册权限，仅兑现已存在Client context合同。
- **已锁定：** 本轮只写 change；实现、提交、发布、远程写入和永久知识修改均未授权。
- **未决：** 用户是否接受本票、外部合同/数据迁移/运行环境参数及是否进入下一 Work。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 读取Client context后才展示可用注册入口，关闭时显示明确状态且不发送注册；提供键盘可用的验证码刷新，替换uuid同时清code；对验证码错/过期/网络失败按错误码恢复，保留非敏感已填字段 | 现有模块边界、权限、日志/错误合同中未被本票改变者 | 其他 finding 的实现、无证据的全仓重写、提交/部署、历史数据清理 |

## 4. 行为与实现契约

- **入口或 seam：** <Path>frontend/apps/home-web/src/views/RegisterPage.vue</Path>；由该 module 的公开 interface 接受输入。
- **输入/输出：** 输入、错误和状态必须保持对应现有合同；任何外部行为变化先更新 Spec/ADR并阻塞本票。
- **不变量：** 失败关闭、权限/Client上下文、资源所有权、事务/幂等和可观测性按适用工程规则成立。
- **错误：** 验证码错误后无需整页刷新即可再次成功

## 5. 执行路线

1. 使用现有prepareLogin登录准备返回的Client context判定registerEnabled；它不是专用注册准备API。状态未加载或失败时入口失败关闭，后端仍独立拒绝禁用注册。
2. 提供键盘可用的验证码刷新，替换uuid同时清code。
3. 对验证码错/过期/网络失败按错误码恢复，保留非敏感已填字段。
4. 展开单行SFC并给输入label、错误aria-live及loading禁用。
5. 验证快速刷新只应用最新captcha：先递增generation，旧慢响应不得覆盖新uuid；一次性captcha消费导致错误后换uuid并清code；卸载或重复提交可取消。登录与SSO/config响应合同保持回归覆盖。

## 6. 路径访问与所有权

- **可写候选：** <Path>frontend/apps/home-web/src/views/RegisterPage.vue</Path>, <Path>frontend/apps/home-web/src/router/</Path>, <Path>frontend/packages/domains/admin/</Path>, <Path>frontend/e2e/</Path>（仅用户批准后；越界必须停止）。
- **共享路径：** 无；若实现时发现与其他票交集，先由 Lead 串行化并修订 Map。
- **不动：** 供应商/历史archive、永久 namespace、用户未提交改动和生成物，除非本票修订后有明确 owner。

## 7. 验证矩阵

| 行为/风险 | 接缝与方法 | 预期 | Evidence |
|---|---|---|---|
| AC 场景 1 | <Path>frontend/apps/home-web/src/views/RegisterPage.vue</Path> 定向测试/静态或隔离运行 | 服务端关闭注册时入口与页面均准确，后端仍拒绝直接调用 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-13.md</Path> |
| AC 场景 2 | <Path>frontend/apps/home-web/src/views/RegisterPage.vue</Path> 定向测试/静态或隔离运行 | 验证码错误后无需整页刷新即可再次成功 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-13.md</Path> |
| AC 场景 3 | <Path>frontend/apps/home-web/src/views/RegisterPage.vue</Path> 定向测试/静态或隔离运行 | 慢旧captcha不能覆盖新uuid | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-13.md</Path> |
| AC 场景 4 | <Path>frontend/apps/home-web/src/views/RegisterPage.vue</Path> 定向测试/静态或隔离运行 | 提交/取消/失败后按钮状态恢复 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-13.md</Path> |

- **Workspace checks：** 在frontend目录运行 `pnpm --filter @namewta/home-web test`、`pnpm typecheck`；SSO/config回归运行 `pnpm exec playwright test --config playwright.sso.config.ts`。默认Admin E2E不证明Home注册旅程通过；需增加明确Home入口用例或提供真实Home浏览器验收。本轮未运行实现验证。
- **E2E disposition：** 按实际跨边界风险决定；未获用户批准前不声明通过。

## 8. 迁移、发布与恢复

- **迁移/兼容：** 不改变密码策略和匿名注册权限，仅兑现已存在Client context合同。
- **回滚：** 先保留基线 SHA、产物/数据库备份及可验证恢复点；任何不可逆删除、DDL、凭据轮换或发布需独立批准。
- **停止条件：** 证据缺失、路径越界、父分支漂移、权限/安全合同不明、测试未运行或出现未批准偏差时停票。

## 9. 验收标准

- [ ] `AC-013`：服务端关闭注册时入口与页面均准确，后端仍拒绝直接调用。
- [ ] `AC-013`：验证码错误后无需整页刷新即可再次成功。
- [ ] `AC-013`：慢旧captcha不能覆盖新uuid。
- [ ] `AC-013`：提交/取消/失败后按钮状态恢复。
- [ ] `AC-013`：captcha错/过期后刷新uuid与清code；旧慢响应不覆盖新uuid，registerEnabled未知或失败时不发送注册；登录与SSO配置合同无回归。
- [ ] Ticket、Map、Spec、ADR 与 Evidence 状态一致；本票保持 draft/ready=false 直到用户接受并完成计划质量复核。
- [ ] 未通过删除测试、关闭类型/安全规则、吞错误、扩大权限或伪造运行证据获得通过。

## 10. Skill 调用计划

- 计划绑定：namewta-fullstack-development。每项入口摘要、references、phase和失败动作以 frontmatter 为最低合同；执行前必须读取当前 Ticket、Map 与实际命中 references。
- 失败动作：必需入口、验证工具或安全/数据证据不可用则 block-ticket；不得以其他工具静默替换。

## 11. 交付与恢复

- **依赖：** T-12；完成顺序由 Tickets Map 控制。
- **检查点：** 记录实现前 base SHA、工作树、命令/退出码、偏差、最终 result SHA 及未运行项。
- **完成出口：** 只在用户批准、所有适用 AC/E2E/Evidence 通过、父分支包含结果后进入 done；当前审查阶段不进入 done。

## 12. 最新证据校正与具体检查

不改变密码策略和匿名注册权限，仅兑现已存在Client context合同。

- 服务端关闭注册时入口与页面均准确，后端仍拒绝直接调用。
- 验证码错误后无需整页刷新即可再次成功。
- 慢旧captcha不能覆盖新uuid。
- 提交/取消/失败后按钮状态恢复。

实际命令候选（实施前对照script重新解析，当前not-run）：

- `frontend: pnpm --filter @namewta/home-web test; pnpm typecheck; pnpm exec playwright test --config playwright.sso.config.ts`
