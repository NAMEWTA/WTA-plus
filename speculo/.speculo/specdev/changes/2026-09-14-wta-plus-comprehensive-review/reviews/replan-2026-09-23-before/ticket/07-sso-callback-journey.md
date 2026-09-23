---
schema_version: 3
plan_contract_version: 1
skill_scan: "2026-09-18枚举.agents/skills入口并按本票真实路径/领域绑定；Map为最低集合"
skill_bindings: [{"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/07-sso-callback-journey.md</Path>", "<Path>backend/wta-modules/wta-sso/src/main/java/org/namewta/sso/service/SsoAuthorizationService.java</Path>", "<Path>backend/wta-modules/wta-sso/src/test/</Path>"], "outputs": ["T-07的架构/权限/数据边界检查与定向实现diff"], "required": true, "on_failure": "block-ticket"}, {"id": "namewta-fullstack-development", "path": "<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>", "sha256": "675c053c11d8b22cd394c875f48688242d8e8328dd14e69657f5d5b12f2af68b", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/07-sso-callback-journey.md</Path>", "<Path>backend/wta-modules/wta-sso/src/main/java/org/namewta/sso/service/SsoAuthorizationService.java</Path>", "<Path>backend/wta-modules/wta-sso/src/test/</Path>"], "outputs": ["T-07的架构/权限/数据边界检查与定向实现diff"], "required": true, "on_failure": "block-ticket"}, {"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "verify", "operation": "verify-affected-contract-and-quality-gates", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/07-sso-callback-journey.md</Path>", "current-workspace实际diff及本票验证矩阵"], "outputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-07.md</Path>：命令、退出码、测试数、AC与Skill Execution Records"], "required": true, "on_failure": "block-ticket"}]
resource_claims: ["workspace:current-exclusive", "finding:B-02", "finding:F-09", "contract:AC-007"]
artifact: ticket
change: 2026-09-14-wta-plus-comprehensive-review
id: T-07
title: 修复SSO回调编码与可恢复登录旅程
status: "review"
planning_depth: "deep"
planning_depth_reason: "安全/鉴权、公共合同、数据一致性或共享核心路径变更：修复SSO回调编码与可恢复登录旅程"
ready: true
risk: high
blocked_by: ["T-06"]
contract_ids: [AC-007]
owner: single-agent
expected_changes: ["<Path>backend/wta-modules/wta-sso/src/main/java/org/namewta/sso/service/SsoAuthorizationService.java</Path>", "<Path>backend/wta-modules/wta-sso/src/test/</Path>", "<Path>frontend/apps/admin-web/src/views/sso-callback.vue</Path>", "<Path>frontend/apps/home-web/src/views/SsoCallbackPage.vue</Path>", "<Path>frontend/apps/admin-web/src/application/sso.ts</Path>", "<Path>frontend/apps/home-web/src/application/sso.ts</Path>", "<Path>frontend/apps/sso-web/src/ssoApi.ts</Path>", "<Path>frontend/apps/sso-web/src/ssoApi.test.ts</Path>", "<Path>frontend/packages/platform/auth/</Path>", "<Path>frontend/e2e/sso-three-gates.spec.ts</Path>", "<Path>frontend/e2e/sso-admin-config.spec.ts</Path>", "<Path>frontend/apps/sso-web/src/views/AuthorizePage.vue</Path>", "<Path>frontend/playwright.sso.config.ts</Path>", "<Path>frontend/apps/admin-web/src/views/login.vue</Path>", "<Path>frontend/apps/home-web/src/router/homeManifestRegistry.ts</Path>", "<Path>frontend/apps/home-web/src/views/SsoCallbackPage.test.ts</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/sso/SsoHttpsSessionIntegrationTest.java</Path>", "<Path>frontend/e2e/sso-callback-journey.spec.ts</Path>"]
writable_paths: ["<Path>backend/wta-modules/wta-sso/src/main/java/org/namewta/sso/service/SsoAuthorizationService.java</Path>", "<Path>backend/wta-modules/wta-sso/src/test/</Path>", "<Path>frontend/apps/admin-web/src/views/sso-callback.vue</Path>", "<Path>frontend/apps/home-web/src/views/SsoCallbackPage.vue</Path>", "<Path>frontend/apps/admin-web/src/application/sso.ts</Path>", "<Path>frontend/apps/home-web/src/application/sso.ts</Path>", "<Path>frontend/apps/sso-web/src/ssoApi.ts</Path>", "<Path>frontend/apps/sso-web/src/ssoApi.test.ts</Path>", "<Path>frontend/packages/platform/auth/</Path>", "<Path>frontend/e2e/sso-three-gates.spec.ts</Path>", "<Path>frontend/e2e/sso-admin-config.spec.ts</Path>", "<Path>frontend/apps/sso-web/src/views/AuthorizePage.vue</Path>", "<Path>frontend/playwright.sso.config.ts</Path>", "<Path>frontend/apps/admin-web/src/views/login.vue</Path>", "<Path>frontend/apps/home-web/src/router/homeManifestRegistry.ts</Path>", "<Path>frontend/apps/home-web/src/views/SsoCallbackPage.test.ts</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/sso/SsoHttpsSessionIntegrationTest.java</Path>", "<Path>frontend/e2e/sso-callback-journey.spec.ts</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/</Path>", "<Path>{roots.state}/specdev/adr/</Path>"]
shared_paths: ["<Path>backend/wta-modules/wta-sso/src/main/java/org/namewta/sso/service/SsoAuthorizationService.java</Path>", "<Path>backend/wta-modules/wta-sso/src/test/</Path>", "<Path>frontend/apps/home-web/src/views/SsoCallbackPage.vue</Path>", "<Path>frontend/apps/admin-web/src/application/sso.ts</Path>", "<Path>frontend/apps/sso-web/src/ssoApi.ts</Path>", "<Path>frontend/apps/sso-web/src/ssoApi.test.ts</Path>", "<Path>frontend/e2e/sso-three-gates.spec.ts</Path>", "<Path>frontend/e2e/sso-admin-config.spec.ts</Path>", "<Path>frontend/apps/sso-web/src/views/AuthorizePage.vue</Path>", "<Path>frontend/playwright.sso.config.ts</Path>", "<Path>frontend/apps/admin-web/src/views/login.vue</Path>", "<Path>frontend/apps/home-web/src/router/homeManifestRegistry.ts</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/sso/SsoHttpsSessionIntegrationTest.java</Path>", "<Path>frontend/e2e/sso-callback-journey.spec.ts</Path>"]
shared_path_owners: ["<Path>backend/wta-modules/wta-sso/src/main/java/org/namewta/sso/service/SsoAuthorizationService.java</Path> => single-agent (Lead; serial T-07 turn)", "<Path>backend/wta-modules/wta-sso/src/test/</Path> => single-agent (Lead; serial T-07 turn)", "<Path>frontend/apps/home-web/src/views/SsoCallbackPage.vue</Path> => single-agent (Lead; serial T-07 turn)", "<Path>frontend/apps/admin-web/src/application/sso.ts</Path> => single-agent (Lead; serial T-07 turn)", "<Path>frontend/apps/sso-web/src/ssoApi.ts</Path> => single-agent (Lead; serial T-07 turn)", "<Path>frontend/apps/sso-web/src/ssoApi.test.ts</Path> => single-agent (Lead; serial T-07 turn)", "<Path>frontend/e2e/sso-three-gates.spec.ts</Path> => single-agent (Lead; serial T-07 turn)", "<Path>frontend/e2e/sso-admin-config.spec.ts</Path> => single-agent (Lead; serial T-07 turn)", "<Path>frontend/apps/sso-web/src/views/AuthorizePage.vue</Path> => single-agent (Lead; serial T-07 turn)", "<Path>frontend/playwright.sso.config.ts</Path> => single-agent (Lead; serial T-07 turn)", "<Path>frontend/apps/admin-web/src/views/login.vue</Path> => single-agent (Lead; serial T-07 turn)", "<Path>frontend/apps/home-web/src/router/homeManifestRegistry.ts</Path> => single-agent (Lead; serial T-07 turn)", "<Path>backend/wta-admin/src/test/java/org/namewta/test/sso/SsoHttpsSessionIntegrationTest.java</Path> => single-agent (Lead; serial T-07 turn)", "<Path>frontend/e2e/sso-callback-journey.spec.ts</Path> => single-agent (Lead; serial T-07 turn)"]
---

# T-07：修复SSO回调编码与可恢复登录旅程

Map：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>；Spec：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/spec.md</Path>；Goal：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/goal-plan.md</Path>。
唯一执行者先读Map，再按Skill矩阵读取适用入口/引用，再读本票。禁止任何implementation/review/research子代理。Ready仅表示计划合同就绪，不表示已经授权实施或已验证通过。

## 1. 战略与来源

- 目标与可观察产出：复杂state往返相等且无重复code/state参数。
- 来源：B-02, F-09；AC-007；USER-DECISION: 全面完善计划、无兼容、单人串行。
- 当前事实与调用链：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/re-review.md</Path>中T-07行及对应专项报告；源码导航为frontmatter预计修改点。
- 规划深度：deep；安全/鉴权、公共合同、数据一致性或共享核心路径变更：修复SSO回调编码与可恢复登录旅程。

## 2. 决策状态

### 已锁定决策

ADR-CR-001/002；后端已精确校验redirect，本票不是虚构开放重定向修复。

### 已采用的低影响假设

沿用当前仓库版本与既有模块命名；实施前回读实际源码，路径变化由本票修订，不猜测不存在的实现。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 原state/注册redirect→URI编码→App callback→安全returnTo | 精确redirect白名单与现有PKCE一次消费；不引入外域returnTo | 本票之外的模块重写、兼容桥、远程发布与重要数据操作 |

## 4. 要构建什么

原state/注册redirect→URI编码→App callback→安全returnTo。调用者可观察到：复杂state往返相等且无重复code/state参数。失败时：state原样保留；拒绝fragment/冲突保留参数；失败重新授权而非重用code。

## 5. 实现契约

- 入口、输入输出与数据流：原state/注册redirect→URI编码→App callback→安全returnTo。
- 不变量及失败语义：state原样保留；拒绝fragment/冲突保留参数；失败重新授权而非重用code。
- 公共合同：本票只按以上行为及执行路线变更；同步全部仓内调用/生成物，沿用已有权限/Client/owner校验。未列出的接口保持原语义。
- 兼容：用户明确无需旧版兼容；仓内一次切换，不加双路由/版本等待。实际供应商协议仍须遵守。
- 安全与隐私：凭据不进入日志/UI证据；越权/过期/无owner拒绝；数据库与资源约束不能为前端成功而放宽。

SsoAuthorizationService.authorize除appendQuery未编码外，还对state调用trim；同时修复精确往返。前端默认Playwright配置不覆盖SSO专用用例，使用playwright.sso.config.ts且先启动隔离服务。returnTo仅接受当前App内路径。

## 6. 执行路线

1. 固定包含&/%/Unicode及已有query的state序列化红灯，保留后端精确redirect白名单。
2. 用规范URI构造替代手工拼接；明确fragment/重复参数拒绝语义。
3. 由origin与规范化VITE_APP_CONTEXT_PATH构造callback，验证/admin/与/home/子路径，不能固定origin根/sso/callback。
4. 记录安全的App内returnTo与一次性PKCE状态，不允许外域跳转。
5. callback展示结构化过期/state/网络错误与重新授权入口，不静默退回登录首页。
6. 避免消费同一code盲目重试；重新授权时生成新state/verifier并清除旧临时状态。 用 ssoApi 与 platform-auth 的既有测试接缝验证 state/PKCE 一次性消费；授权页错误保留可重新发起入口。默认 playwright.config.ts 会排除 SSO three-gates/admin-config specs，必须使用专用配置。 state是opaque值：校验非空不应trim后再回传；覆盖首尾空格、+、&、%、Unicode及已有query，拒绝注册fragment/冲突保留参数。

## 7. 路径访问契约

预计点、可写范围、只读上下文及共享项以frontmatter为唯一权威。共享owner固定single-agent；只有当前票轮次可写，下一票须回读前一结果。跨模块目录只允许本票行为必需的文件，目录授权不意味着重写全部模块。新增测试位于同模块测试目录；未覆盖的新路径先修订本票/Map再写。
保留当前用户未提交改动、永久ADR/context、供应商源码与运行数据；生成物通过正式工具更新。

## 8. 验证矩阵

| 行为或风险 | 验证接缝/步骤 | 预期结果 | Evidence |
|---|---|---|---|
| 正常路径 | 原state/注册redirect→URI编码→App callback→安全returnTo；执行下列定向命令及对应场景 | 复杂state往返相等且无重复code/state参数 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-07.md</Path> |
| 失败路径 | state原样保留；拒绝fragment/冲突保留参数；失败重新授权而非重用code；固定时序/故障注入，记录输入与最终可观察状态 | 无越权、错误状态或部分提交；可按定义恢复 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-07.md</Path> |
| 回归 | 运行所属包原有测试及受影响调用者；逐项核对下列AC | 过期、错state、错误verifier均失败关闭且用户可重新授权；成功回到原App内路径，带外域returnTo被拒；日志与UI不暴露code/verifier/token | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-07.md</Path> |

Workspace checks：current-workspace；`frontend:`/`backend:`表示先进入该目录，其他命令cwd为仓根，逐条串行执行。新增用例实施时登记精确选择器、实际测试数与跳过项，零测试/required跳过不算通过。

- `backend: ./mvnw -pl wta-modules/wta-sso -am test`
- `frontend: pnpm exec playwright test --config playwright.sso.config.ts --grep T-07`（由隔离fixture提供环境参数）

- E2E disposition：required: 专用SSO Playwright跑/admin与/home base、复杂state、过期与重新登录。
- E2E owner/environment：single-agent（Lead）/current-workspace；使用隔离MySQL/Redis/OSS及必要真实HTTP/浏览器，禁止连生产。场景步骤以上表、本票AC为准；需新用例时在写集内创建后记录精确命令。
- Integration evidence：记录parent before、implementation commit及direct-parent检查；result SHA等于通过验证的implementation commit，candidate不适用。本票实际检查见T-07.md；Admin全量typecheck剩8条其他页面诊断归T-20，commit/result仍空。

## 9. 发布、迁移与恢复

- 顺序：前置票产生已验证合同后实施本票；源码、仓内消费者、测试和生成物同批交付。涉及DDL只编辑10-cde-base-ddl.sql，新环境按六文件基座初始化；不增加存量迁移工程。
- 兼容窗口：无；不保留旧接口或数据格式桥。生产部署不是本票自动步骤。
- 监控/诊断：观察本票AC的成功/错误状态、耗时及资源/持久化结果，日志只含安全元数据；复用现有观测入口，不新建监控平台。
- 恢复：前后端整体回退同一版本；清理当前浏览器临时PKCE状态后重新授权。
- 不可逆批准点：提交、推送、部署、运行数据删除/修复分别需授权；最新用户授权本地实现与验证，全change暂不提交。
- 收缩条件：本票替代的旧调用/配置引用归零且仓内回归通过；无被替代入口时不适用，不为凑清单扩大删除范围。

## 10. 验收标准

- [x] `AC-007`：复杂state往返相等且无重复code/state参数。
- [x] `AC-007`：过期、错state、错误verifier均失败关闭且用户可重新授权。
- [x] `AC-007`：成功回到原App内路径，带外域returnTo被拒。
- [x] `AC-007`：日志与UI不暴露code/verifier/token。
- [x] 按Map→适用Skill→本票完成读取及实际调用；所有required Skill记录passed并可回读。
- [x] 正常/失败/回归及required E2E均完成，证据写入<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-07.md</Path>，未执行不得标通过。
- [x] 修改不超出写集，共享项只有single-agent当前票轮次写入。
- [x] 获得授权后形成非空implementation commit，Lead完成direct-parent验收并记录parent result SHA；未获授权不提交、不标Done。
- [x] Ticket、Map、Goal与Evidence一致；不存在未批准偏差。

## 11. SKILL 调用计划

frontmatter绑定的项目Skill在implementation阶段接收本票路径和上游合同，产出适用分层、权限、数据/资源边界及实现diff；engineering-standards在verify阶段根据本节命令选择受影响门禁并输出AC/退出码/E2E记录。按入口scope展开引用，不以“已读”代替实际操作。
必需入口缺失或sha256漂移时阻塞本票；Lead回读差异后更新绑定及Map，不能自动接受新摘要。实际记录归<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-07.md</Path>的`## Skill Execution Records`，本次规划不伪造实现调用记录。

## 12. 停止、检查点与交付

交付本票完整可观察行为及验收证据；数量以Map为准。缺依赖/测试环境/Skill、越界或高影响事实变化时停止受影响票，保留checkpoint和失败证据，其他独立票仍可串行推进。恢复先读Goal、Map、本票、状态及最新Evidence；记录实际HEAD/dirty差异，禁止覆盖用户修改。
依赖：T-06。单票完成条件为全部AC、实际Skill证据和获授权的direct-parent出口；仅补文档不能标Done。

### 本地实施决策

T-06以已验证未提交检查点为输入。补充两个App实际SSO启动调用点到写集，用现有redirect查询记录App内returnTo；不改非SSO本地登录、路由权限或会话注销合同。

验收接缝补充：复用T-06的自建HTTPS/Redis/MySQL fixture，并增加两个独立端口的/admin、/home构建产物；SSO独立localhost Origin，两个App使用127.0.0.1 Origin。专用配置新增T-07浏览器场景，保留原three-gates/admin-config测试文件；后两者包含完整System准入/管理写入，归T-30真实整合环境执行，不用其默认18080端口探测或修改现有服务。T-07的身份/菜单API和业务Token签发为显式fixture，SSO协议、MySQL消费、Redis会话和App浏览器路径真实执行。


第一轮浏览器Admin6通过，Home6失败已定位为fixture误用componentKey而非真实菜单component字段。复核确认Home createReplacement原本已保留hash，未修改该生产路由文件；此前缺hash判断撤回，以实际源码为准。

## Revision135 实际提交与父分支验收

用户已明确授权全部commit/push。implementation commits：`5aadc6453405694c2e2066ef551c61374a2f5914`, `2b93d2d5407a7e341076f771a19ab5292b567cf8`；完整实现链 result SHA：`6c8764cca97bb6057fcb90ccdfe635c7efbf502a`。每个提交均非空、实际父SHA已核对且被result包含；Git归档逐文件等于T-30已验证输入，未声称拆分过程中的中间树独立通过全部测试。精确路径/共享owner/验证见 `../evidence/commit-delivery.json`。本票保持review；正式发布候选与change最终Done独立验收。
