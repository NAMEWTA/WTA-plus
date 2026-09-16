---
schema_version: 3
plan_contract_version: 1
skill_scan: "已扫描项目 .agents/skills/**/SKILL.md；仅绑定本票命中的入口，执行前按入口继续展开适用 references"
skill_bindings: [{"id":"engineering-standards","path":"<Path>.agents/skills/engineering-standards/SKILL.md</Path>","sha256":"ef0588daecab40e12e49e99d29a12bf107e887ab432e10a790bbc893fc42b0cf","phase":"plan","operation":"read-scope-contract-and-bind-acceptance","inputs":["当前审查候选、Ticket路径和上游ADR"],"outputs":["实现前边界、验证与失败停止条件"],"required":true,"on_failure":"block-ticket"}]
resource_claims: ["finding:F-13", "contract:AC-021"]
artifact: ticket
change: 2026-09-14-wta-plus-comprehensive-review
id: T-21
title: 统一公开页面的可访问交互基线
status: draft
planning_depth: standard
planning_depth_reason: "本票涉及 F-13；需先完成证据/高影响取舍，不能直接实现。"
ready: false
risk: medium
blocked_by: ["T-13", "T-14"]
contract_ids: [AC-021]
owner: user-review
expected_changes: ["<Path>frontend/packages/web-domains/admin/src/auth/LoginPage.vue</Path>", "<Path>frontend/apps/home-web/src/views/RegisterPage.vue</Path>", "<Path>frontend/apps/sso-web/src/views/AuthorizePage.vue</Path>", "<Path>frontend/apps/home-web/src/views/SsoCallbackPage.vue</Path>", "<Path>frontend/e2e/</Path>", "<Path>frontend/apps/home-web/src/App.vue</Path>"]
writable_paths: ["<Path>frontend/packages/web-domains/admin/src/auth/LoginPage.vue</Path>", "<Path>frontend/apps/home-web/src/views/RegisterPage.vue</Path>", "<Path>frontend/apps/sso-web/src/views/AuthorizePage.vue</Path>", "<Path>frontend/apps/home-web/src/views/SsoCallbackPage.vue</Path>", "<Path>frontend/e2e/</Path>", "<Path>frontend/apps/home-web/src/App.vue</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/</Path>", "<Path>{roots.state}/specdev/adr/</Path>"]
shared_paths: []
shared_path_owners: []
---

# T-21：统一公开页面的可访问交互基线

> 本 Ticket 是 <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/</Path> 内的待审实施计划。它不授权修改产品代码；只有用户明确接受该 Ticket 后，Lead 才能按 project Skill 建立实施 Evidence。

## 1. 战略与来源

- **Finding：** F-13。详细源码证据见专项审查报告。
- **目标：** 删除真实结构复杂性并恢复可验证的行为/安全/交付合同；不以移动代码、放宽门禁、删除测试或无限兼容层制造绿色。
- **可观察产出：** 只用键盘可完成公开流程，焦点可见且错误能被读屏发现

## 2. 决策状态

- **当前状态：** draft，ready=false；高影响决策：本票不预设全站换皮；具体视觉调整按用户审核后的设计基线。
- **已锁定：** 本轮只写 change；实现、提交、发布、远程写入和永久知识修改均未授权。
- **未决：** 用户是否接受本票、外部合同/数据迁移/运行环境参数及是否进入下一 Work。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 列登录/注册/授权/资料表单的label、状态提示、焦点和键盘动作；为输入建立可关联标签，错误用role=alert或aria-live，导航标记active/aria-current；仅共享稳定的语义表单primitive，各App品牌token保留owner并明确默认值 | 现有模块边界、权限、日志/错误合同中未被本票改变者 | 其他 finding 的实现、无证据的全仓重写、提交/部署、历史数据清理 |

## 4. 行为与实现契约

- **入口或 seam：** <Path>frontend/apps/home-web/src/</Path>；由该 module 的公开 interface 接受输入。
- **输入/输出：** 输入、错误和状态必须保持对应现有合同；任何外部行为变化先更新 Spec/ADR并阻塞本票。
- **不变量：** 失败关闭、权限/Client上下文、资源所有权、事务/幂等和可观测性按适用工程规则成立。
- **错误：** 移动/放大页面无遮挡必要动作和横向不可达内容

## 5. 执行路线

1. 保留SSO包裹input的label和Home nav/aside语义；优先补Home登录所需--client-* token fallback与SSO动态status播报。
2. 针对已证实缺项添加status aria-live/可键盘重试；输入关联标签与导航语义作为回归，不重复改已有正确实现。
3. 仅共享稳定的语义表单primitive，各App品牌token保留owner并明确默认值。
4. 展开Home RegisterPage.vue的单行template/script/style，保持既有品牌布局意图；共享LoginPage.vue的--client-* token需由Home App定义或有明确fallback。材料功能由T14负责，本票只处理已证实的公共页面token/status与可审查性。
5. 在320/768/1440视口、200%缩放、键盘与读屏语义下验收；颜色对比需真实渲染测量。

## 6. 路径访问与所有权

- **可写候选：** <Path>frontend/packages/web-domains/admin/src/auth/LoginPage.vue</Path>, <Path>frontend/apps/home-web/src/views/RegisterPage.vue</Path>, <Path>frontend/apps/sso-web/src/views/AuthorizePage.vue</Path>, <Path>frontend/apps/home-web/src/views/SsoCallbackPage.vue</Path>, <Path>frontend/e2e/</Path>, <Path>frontend/apps/home-web/src/App.vue</Path>（仅用户批准后；越界必须停止）。
- **共享路径：** 无；若实现时发现与其他票交集，先由 Lead 串行化并修订 Map。
- **不动：** 供应商/历史archive、永久 namespace、用户未提交改动和生成物，除非本票修订后有明确 owner。

## 7. 验证矩阵

| 行为/风险 | 接缝与方法 | 预期 | Evidence |
|---|---|---|---|
| AC 场景 1 | <Path>frontend/apps/home-web/src/</Path> 定向测试/静态或隔离运行 | 只用键盘可完成公开流程，焦点可见且错误能被读屏发现 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-21.md</Path> |
| AC 场景 2 | <Path>frontend/apps/home-web/src/</Path> 定向测试/静态或隔离运行 | 移动/放大页面无遮挡必要动作和横向不可达内容 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-21.md</Path> |
| AC 场景 3 | <Path>frontend/apps/home-web/src/</Path> 定向测试/静态或隔离运行 | 不同App品牌差异不被当成bug强制同化 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-21.md</Path> |
| AC 场景 4 | <Path>frontend/apps/home-web/src/</Path> 定向测试/静态或隔离运行 | 报告记录真实截图/可访问性结果，静态检查不冒充视觉通过 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-21.md</Path> |

- **Workspace checks：** 在frontend目录运行 `pnpm lint`、`pnpm build:prod`、`pnpm exec playwright test --config playwright.sso.config.ts`；默认Admin配置不覆盖Home/SSO回调。Home登录/注册与SSO授权的键盘、焦点、读屏、320/768/1440px、200%缩放和颜色对比为needs-runtime，记录真实浏览器证据。本轮未运行实现验证。
- **E2E disposition：** 按实际跨边界风险决定；未获用户批准前不声明通过。

## 8. 迁移、发布与恢复

- **迁移/兼容：** 本票不预设全站换皮；具体视觉调整按用户审核后的设计基线。
- **回滚：** 先保留基线 SHA、产物/数据库备份及可验证恢复点；任何不可逆删除、DDL、凭据轮换或发布需独立批准。
- **停止条件：** 证据缺失、路径越界、父分支漂移、权限/安全合同不明、测试未运行或出现未批准偏差时停票。

## 9. 验收标准

- [ ] `AC-021`：只用键盘可完成公开流程，焦点可见且错误能被读屏发现。
- [ ] `AC-021`：移动/放大页面无遮挡必要动作和横向不可达内容。
- [ ] `AC-021`：不同App品牌差异不被当成bug强制同化。
- [ ] `AC-021`：报告记录真实截图/可访问性结果，静态检查不冒充视觉通过。
- [ ] `AC-021`：保留SSO包裹式label与Home已有nav/aside语义；补Home token owner/fallback和SSO动态status live region；视觉结论以真实浏览器证据为准。
- [ ] Ticket、Map、Spec、ADR 与 Evidence 状态一致；本票保持 draft/ready=false 直到用户接受并完成计划质量复核。
- [ ] 未通过删除测试、关闭类型/安全规则、吞错误、扩大权限或伪造运行证据获得通过。

## 10. Skill 调用计划

- 计划绑定：engineering-standards。每项入口摘要、references、phase和失败动作以 frontmatter 为最低合同；执行前必须读取当前 Ticket、Map 与实际命中 references。
- 失败动作：必需入口、验证工具或安全/数据证据不可用则 block-ticket；不得以其他工具静默替换。

## 11. 交付与恢复

- **依赖：** T-13, T-14；完成顺序由 Tickets Map 控制。
- **检查点：** 记录实现前 base SHA、工作树、命令/退出码、偏差、最终 result SHA 及未运行项。
- **完成出口：** 只在用户批准、所有适用 AC/E2E/Evidence 通过、父分支包含结果后进入 done；当前审查阶段不进入 done。

## 12. 最新证据校正与具体检查

本票不预设全站换皮；具体视觉调整按用户审核后的设计基线。

- 只用键盘可完成公开流程，焦点可见且错误能被读屏发现。
- 移动/放大页面无遮挡必要动作和横向不可达内容。
- 不同App品牌差异不被当成bug强制同化。
- 报告记录真实截图/可访问性结果，静态检查不冒充视觉通过。

实际命令候选（实施前对照script重新解析，当前not-run）：

- `frontend: pnpm lint; pnpm exec playwright test --config playwright.sso.config.ts; pnpm build:prod`
- `browser: keyboard/zoom/viewport/accessibility checks`
