---
schema_version: 3
plan_contract_version: 1
skill_scan: "已扫描项目 .agents/skills/**/SKILL.md；仅绑定本票命中的入口，执行前按入口继续展开适用 references"
skill_bindings: [{"id":"engineering-standards","path":"<Path>.agents/skills/engineering-standards/SKILL.md</Path>","sha256":"ef0588daecab40e12e49e99d29a12bf107e887ab432e10a790bbc893fc42b0cf","phase":"plan","operation":"read-scope-contract-and-bind-acceptance","inputs":["当前审查候选、Ticket路径和上游ADR"],"outputs":["实现前边界、验证与失败停止条件"],"required":true,"on_failure":"block-ticket"},{"id":"namewta-fullstack-development","path":"<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>","sha256":"dd3e42720caa894bcb31ba1503b8211f669b3bbc2bb9ea3e40590a3a9edea395","phase":"plan","operation":"read-scope-contract-and-bind-acceptance","inputs":["当前审查候选、Ticket路径和上游ADR"],"outputs":["实现前边界、验证与失败停止条件"],"required":true,"on_failure":"block-ticket"}]
resource_claims: ["finding:B-02", "finding:F-09", "contract:AC-007"]
artifact: ticket
change: 2026-09-14-wta-plus-comprehensive-review
id: T-07
title: 修复SSO回调编码与可恢复登录旅程
status: draft
planning_depth: deep
planning_depth_reason: "本票涉及 B-02, F-09；需先完成证据/高影响取舍，不能直接实现。"
ready: false
risk: high
blocked_by: ["T-06"]
contract_ids: [AC-007]
owner: user-review
expected_changes: ["<Path>backend/wta-modules/wta-sso/src/main/java/org/namewta/sso/service/SsoAuthorizationService.java</Path>", "<Path>backend/wta-modules/wta-sso/src/test/</Path>", "<Path>frontend/apps/admin-web/src/views/sso-callback.vue</Path>", "<Path>frontend/apps/home-web/src/views/SsoCallbackPage.vue</Path>", "<Path>frontend/apps/admin-web/src/application/sso.ts</Path>", "<Path>frontend/apps/home-web/src/application/sso.ts</Path>", "<Path>frontend/apps/sso-web/src/ssoApi.ts</Path>", "<Path>frontend/apps/sso-web/src/ssoApi.test.ts</Path>", "<Path>frontend/packages/platform/auth/</Path>", "<Path>frontend/e2e/sso-three-gates.spec.ts</Path>", "<Path>frontend/e2e/sso-admin-config.spec.ts</Path>", "<Path>frontend/apps/sso-web/src/views/AuthorizePage.vue</Path>"]
writable_paths: ["<Path>backend/wta-modules/wta-sso/src/main/java/org/namewta/sso/service/SsoAuthorizationService.java</Path>", "<Path>backend/wta-modules/wta-sso/src/test/</Path>", "<Path>frontend/apps/admin-web/src/views/sso-callback.vue</Path>", "<Path>frontend/apps/home-web/src/views/SsoCallbackPage.vue</Path>", "<Path>frontend/apps/admin-web/src/application/sso.ts</Path>", "<Path>frontend/apps/home-web/src/application/sso.ts</Path>", "<Path>frontend/apps/sso-web/src/ssoApi.ts</Path>", "<Path>frontend/apps/sso-web/src/ssoApi.test.ts</Path>", "<Path>frontend/packages/platform/auth/</Path>", "<Path>frontend/e2e/sso-three-gates.spec.ts</Path>", "<Path>frontend/e2e/sso-admin-config.spec.ts</Path>", "<Path>frontend/apps/sso-web/src/views/AuthorizePage.vue</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/</Path>", "<Path>{roots.state}/specdev/adr/</Path>"]
shared_paths: []
shared_path_owners: []
---

# T-07：修复SSO回调编码与可恢复登录旅程

> 本 Ticket 是 <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/</Path> 内的待审实施计划。它不授权修改产品代码；只有用户明确接受该 Ticket 后，Lead 才能按 project Skill 建立实施 Evidence。

## 1. 战略与来源

- **Finding：** B-02, F-09。详细源码证据见专项审查报告。
- **目标：** 删除真实结构复杂性并恢复可验证的行为/安全/交付合同；不以移动代码、放宽门禁、删除测试或无限兼容层制造绿色。
- **可观察产出：** 复杂state往返相等且无重复code/state参数

## 2. 决策状态

- **当前状态：** draft，ready=false；高影响决策：ADR-CR-001/002；后端已精确校验redirect，本票不是虚构开放重定向修复。
- **已锁定：** 本轮只写 change；实现、提交、发布、远程写入和永久知识修改均未授权。
- **未决：** 用户是否接受本票、外部合同/数据迁移/运行环境参数及是否进入下一 Work。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 固定包含&/%/Unicode及已有query的state序列化红灯，保留后端精确redirect白名单；用规范URI构造替代手工拼接；明确fragment/重复参数拒绝语义；记录安全的App内returnTo与一次性PKCE状态，不允许外域跳转 | 现有模块边界、权限、日志/错误合同中未被本票改变者 | 其他 finding 的实现、无证据的全仓重写、提交/部署、历史数据清理 |

## 4. 行为与实现契约

- **入口或 seam：** <Path>backend/wta-modules/wta-sso/src/main/java/org/namewta/sso/service/SsoAuthorizationService.java</Path>；由该 module 的公开 interface 接受输入。
- **输入/输出：** 输入、错误和状态必须保持对应现有合同；任何外部行为变化先更新 Spec/ADR并阻塞本票。
- **不变量：** 失败关闭、权限/Client上下文、资源所有权、事务/幂等和可观测性按适用工程规则成立。
- **错误：** 过期、错state、错误verifier均失败关闭且用户可重新授权

## 5. 执行路线

1. 固定包含&/%/Unicode及已有query的state序列化红灯，保留后端精确redirect白名单。
2. 用规范URI构造替代手工拼接；明确fragment/重复参数拒绝语义。
3. 由origin与规范化VITE_APP_CONTEXT_PATH构造callback，验证/admin/与/home/子路径，不能固定origin根/sso/callback。
4. 记录安全的App内returnTo与一次性PKCE状态，不允许外域跳转。
5. callback展示结构化过期/state/网络错误与重新授权入口，不静默退回登录首页。
6. 避免消费同一code盲目重试；重新授权时生成新state/verifier并清除旧临时状态。
7. 用 ssoApi 与 platform-auth 的既有测试接缝验证 state/PKCE 一次性消费；授权页错误保留可重新发起入口。默认 playwright.config.ts 会排除 SSO three-gates/admin-config specs，必须使用专用配置。

## 6. 路径访问与所有权

- **可写候选：** <Path>backend/wta-modules/wta-sso/src/main/java/org/namewta/sso/service/SsoAuthorizationService.java</Path>, <Path>backend/wta-modules/wta-sso/src/test/</Path>, <Path>frontend/apps/admin-web/src/views/sso-callback.vue</Path>, <Path>frontend/apps/home-web/src/views/SsoCallbackPage.vue</Path>, <Path>frontend/apps/admin-web/src/application/sso.ts</Path>, <Path>frontend/apps/home-web/src/application/sso.ts</Path>, <Path>frontend/apps/sso-web/src/ssoApi.ts</Path>, <Path>frontend/apps/sso-web/src/ssoApi.test.ts</Path>, <Path>frontend/packages/platform/auth/</Path>, <Path>frontend/e2e/sso-three-gates.spec.ts</Path>, <Path>frontend/e2e/sso-admin-config.spec.ts</Path>, <Path>frontend/apps/sso-web/src/views/AuthorizePage.vue</Path>（仅用户批准后；越界必须停止）。
- **共享路径：** 无；若实现时发现与其他票交集，先由 Lead 串行化并修订 Map。
- **不动：** 供应商/历史archive、永久 namespace、用户未提交改动和生成物，除非本票修订后有明确 owner。

## 7. 验证矩阵

| 行为/风险 | 接缝与方法 | 预期 | Evidence |
|---|---|---|---|
| AC 场景 1 | <Path>backend/wta-modules/wta-sso/src/main/java/org/namewta/sso/service/SsoAuthorizationService.java</Path> 定向测试/静态或隔离运行 | 复杂state往返相等且无重复code/state参数 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-07.md</Path> |
| AC 场景 2 | <Path>backend/wta-modules/wta-sso/src/main/java/org/namewta/sso/service/SsoAuthorizationService.java</Path> 定向测试/静态或隔离运行 | 过期、错state、错误verifier均失败关闭且用户可重新授权 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-07.md</Path> |
| AC 场景 3 | <Path>backend/wta-modules/wta-sso/src/main/java/org/namewta/sso/service/SsoAuthorizationService.java</Path> 定向测试/静态或隔离运行 | 成功回到原App内路径，带外域returnTo被拒 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-07.md</Path> |
| AC 场景 4 | <Path>backend/wta-modules/wta-sso/src/main/java/org/namewta/sso/service/SsoAuthorizationService.java</Path> 定向测试/静态或隔离运行 | 日志与UI不暴露code/verifier/token | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-07.md</Path> |

- **Workspace checks：** 在backend目录运行 `./mvnw -pl wta-modules/wta-sso -am test`；在frontend目录运行 `pnpm exec playwright test --config playwright.sso.config.ts`，该配置实际匹配sso-three-gates.spec.ts与sso-admin-config.spec.ts，无需附加grep。本轮未运行实现验证。
- **E2E disposition：** required：跨模块/浏览器/外部服务行为必须由 Lead 在 current workspace 或 parent-candidate 验收。

## 8. 迁移、发布与恢复

- **迁移/兼容：** ADR-CR-001/002；后端已精确校验redirect，本票不是虚构开放重定向修复。
- **回滚：** 先保留基线 SHA、产物/数据库备份及可验证恢复点；任何不可逆删除、DDL、凭据轮换或发布需独立批准。
- **停止条件：** 证据缺失、路径越界、父分支漂移、权限/安全合同不明、测试未运行或出现未批准偏差时停票。

## 9. 验收标准

- [ ] `AC-007`：复杂state往返相等且无重复code/state参数。
- [ ] `AC-007`：过期、错state、错误verifier均失败关闭且用户可重新授权。
- [ ] `AC-007`：成功回到原App内路径，带外域returnTo被拒。
- [ ] `AC-007`：日志与UI不暴露code/verifier/token。
- [ ] `AC-007`：origin根路径、/admin/与/home/ context path均回到正确callback；失效或重复code后通过新state/verifier重新授权，临时状态与returnTo均有owner。
- [ ] Ticket、Map、Spec、ADR 与 Evidence 状态一致；本票保持 draft/ready=false 直到用户接受并完成计划质量复核。
- [ ] 未通过删除测试、关闭类型/安全规则、吞错误、扩大权限或伪造运行证据获得通过。

## 10. Skill 调用计划

- 计划绑定：namewta-fullstack-development。每项入口摘要、references、phase和失败动作以 frontmatter 为最低合同；执行前必须读取当前 Ticket、Map 与实际命中 references。
- 失败动作：必需入口、验证工具或安全/数据证据不可用则 block-ticket；不得以其他工具静默替换。

## 11. 交付与恢复

- **依赖：** T-06；完成顺序由 Tickets Map 控制。
- **检查点：** 记录实现前 base SHA、工作树、命令/退出码、偏差、最终 result SHA 及未运行项。
- **完成出口：** 只在用户批准、所有适用 AC/E2E/Evidence 通过、父分支包含结果后进入 done；当前审查阶段不进入 done。

## 12. 最新证据校正与具体检查

ADR-CR-001/002；后端已精确校验redirect，本票不是虚构开放重定向修复。

- 复杂state往返相等且无重复code/state参数。
- 过期、错state、错误verifier均失败关闭且用户可重新授权。
- 成功回到原App内路径，带外域returnTo被拒。
- 日志与UI不暴露code/verifier/token。

实际命令候选（实施前对照script重新解析，当前not-run）：

- `backend: ./mvnw -pl wta-modules/wta-sso -am test`
- `frontend: pnpm exec playwright test --config playwright.sso.config.ts`
