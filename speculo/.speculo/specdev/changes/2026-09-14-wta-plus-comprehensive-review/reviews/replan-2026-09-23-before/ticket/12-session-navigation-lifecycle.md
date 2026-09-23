---
schema_version: 3
plan_contract_version: 1
skill_scan: "2026-09-18枚举.agents/skills入口并按本票真实路径/领域绑定；Map为最低集合"
skill_bindings: [{"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/12-session-navigation-lifecycle.md</Path>", "<Path>frontend/apps/admin-web/src/store/</Path>", "<Path>frontend/apps/admin-web/src/permission.ts</Path>"], "outputs": ["T-12的架构/权限/数据边界检查与定向实现diff"], "required": true, "on_failure": "block-ticket"}, {"id": "namewta-fullstack-development", "path": "<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>", "sha256": "675c053c11d8b22cd394c875f48688242d8e8328dd14e69657f5d5b12f2af68b", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/12-session-navigation-lifecycle.md</Path>", "<Path>frontend/apps/admin-web/src/store/</Path>", "<Path>frontend/apps/admin-web/src/permission.ts</Path>"], "outputs": ["T-12的架构/权限/数据边界检查与定向实现diff"], "required": true, "on_failure": "block-ticket"}, {"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "verify", "operation": "verify-affected-contract-and-quality-gates", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/12-session-navigation-lifecycle.md</Path>", "current-workspace实际diff及本票验证矩阵"], "outputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-12.md</Path>：命令、退出码、测试数、AC与Skill Execution Records"], "required": true, "on_failure": "block-ticket"}]
resource_claims: ["workspace:current-exclusive", "finding:F-04", "finding:F-05", "contract:AC-012"]
artifact: ticket
change: 2026-09-14-wta-plus-comprehensive-review
id: T-12
title: 统一幂等会话清理与导航恢复状态
status: "review"
planning_depth: "standard"
planning_depth_reason: "沿用现有模块的多文件行为修复：统一幂等会话清理与导航恢复状态"
ready: true
risk: high
blocked_by: ["T-07"]
contract_ids: [AC-012]
owner: single-agent
expected_changes: ["<Path>frontend/apps/admin-web/src/store/</Path>", "<Path>frontend/apps/admin-web/src/permission.ts</Path>", "<Path>frontend/apps/admin-web/src/application/</Path>", "<Path>frontend/apps/home-web/src/store/</Path>", "<Path>frontend/apps/home-web/src/router/</Path>", "<Path>frontend/e2e/</Path>", "<Path>frontend/apps/admin-web/src/router/</Path>", "<Path>frontend/apps/home-web/src/application/session.ts</Path>", "<Path>frontend/apps/home-web/src/application/http.ts</Path>", "<Path>frontend/apps/home-web/src/layout/HomeShell.vue</Path>", "<Path>frontend/apps/admin-web/src/layout/components/Navbar.vue</Path>", "<Path>frontend/packages/domains/admin/src/index.ts</Path>", "<Path>frontend/packages/domains/admin/src/index.test.ts</Path>", "<Path>frontend/packages/adapters/axios-browser/</Path>", "<Path>frontend/packages/platform/auth/src/index.ts</Path>", "<Path>frontend/packages/platform/auth/src/index.test.ts</Path>", "<Path>frontend/packages/platform/app-runtime/src/navigationRecovery.ts</Path>", "<Path>frontend/packages/platform/app-runtime/src/navigationRecovery.test.ts</Path>", "<Path>frontend/playwright.lifecycle.config.ts</Path>", "<Path>frontend/playwright.config.ts</Path>", "<Path>frontend/apps/admin-web/src/views/system/user/profile/index.vue</Path>", "<Path>.agents/skills/namewta-fullstack-development/references/frontend/permission-routing.md</Path>"]
writable_paths: ["<Path>frontend/apps/admin-web/src/store/</Path>", "<Path>frontend/apps/admin-web/src/permission.ts</Path>", "<Path>frontend/apps/admin-web/src/application/</Path>", "<Path>frontend/apps/home-web/src/store/</Path>", "<Path>frontend/apps/home-web/src/router/</Path>", "<Path>frontend/e2e/</Path>", "<Path>frontend/apps/admin-web/src/router/</Path>", "<Path>frontend/apps/home-web/src/application/session.ts</Path>", "<Path>frontend/apps/home-web/src/application/http.ts</Path>", "<Path>frontend/apps/home-web/src/layout/HomeShell.vue</Path>", "<Path>frontend/apps/admin-web/src/layout/components/Navbar.vue</Path>", "<Path>frontend/packages/domains/admin/src/index.ts</Path>", "<Path>frontend/packages/domains/admin/src/index.test.ts</Path>", "<Path>frontend/packages/adapters/axios-browser/</Path>", "<Path>frontend/packages/platform/auth/src/index.ts</Path>", "<Path>frontend/packages/platform/auth/src/index.test.ts</Path>", "<Path>frontend/packages/platform/app-runtime/src/navigationRecovery.ts</Path>", "<Path>frontend/packages/platform/app-runtime/src/navigationRecovery.test.ts</Path>", "<Path>frontend/playwright.lifecycle.config.ts</Path>", "<Path>frontend/playwright.config.ts</Path>", "<Path>frontend/apps/admin-web/src/views/system/user/profile/index.vue</Path>", "<Path>.agents/skills/namewta-fullstack-development/references/frontend/permission-routing.md</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/</Path>", "<Path>{roots.state}/specdev/adr/</Path>"]
shared_paths: ["<Path>frontend/apps/admin-web/src/application/</Path>", "<Path>frontend/apps/home-web/src/store/</Path>", "<Path>frontend/apps/home-web/src/router/</Path>", "<Path>frontend/e2e/</Path>", "<Path>frontend/apps/admin-web/src/router/</Path>", "<Path>frontend/apps/home-web/src/application/http.ts</Path>", "<Path>frontend/apps/home-web/src/layout/HomeShell.vue</Path>", "<Path>frontend/apps/admin-web/src/layout/components/Navbar.vue</Path>", "<Path>frontend/packages/domains/admin/src/index.ts</Path>", "<Path>frontend/packages/domains/admin/src/index.test.ts</Path>", "<Path>frontend/packages/adapters/axios-browser/</Path>", "<Path>frontend/packages/platform/auth/src/index.ts</Path>", "<Path>frontend/packages/platform/auth/src/index.test.ts</Path>", "<Path>frontend/packages/platform/app-runtime/src/navigationRecovery.ts</Path>", "<Path>frontend/packages/platform/app-runtime/src/navigationRecovery.test.ts</Path>", "<Path>frontend/playwright.lifecycle.config.ts</Path>", "<Path>frontend/playwright.config.ts</Path>", "<Path>frontend/apps/admin-web/src/views/system/user/profile/index.vue</Path>", "<Path>.agents/skills/namewta-fullstack-development/references/frontend/permission-routing.md</Path>"]
shared_path_owners: ["<Path>frontend/apps/admin-web/src/application/</Path> => single-agent (Lead; serial T-12 turn)", "<Path>frontend/apps/home-web/src/store/</Path> => single-agent (Lead; serial T-12 turn)", "<Path>frontend/apps/home-web/src/router/</Path> => single-agent (Lead; serial T-12 turn)", "<Path>frontend/e2e/</Path> => single-agent (Lead; serial T-12 turn)", "<Path>frontend/apps/admin-web/src/router/</Path> => single-agent (Lead; serial T-12 turn)", "<Path>frontend/apps/home-web/src/application/http.ts</Path> => single-agent (Lead; serial T-12 turn)", "<Path>frontend/apps/home-web/src/layout/HomeShell.vue</Path> => single-agent (Lead; serial T-12 turn)", "<Path>frontend/apps/admin-web/src/layout/components/Navbar.vue</Path> => single-agent (Lead; serial T-12 turn)", "<Path>frontend/packages/domains/admin/src/index.ts</Path> => single-agent (Lead; serial T-12 turn)", "<Path>frontend/packages/domains/admin/src/index.test.ts</Path> => single-agent (Lead; serial T-12 turn)", "<Path>frontend/packages/adapters/axios-browser/</Path> => single-agent (Lead; serial T-12 turn)", "<Path>frontend/packages/platform/auth/src/index.ts</Path> => single-agent (Lead; serial T-12 turn)", "<Path>frontend/packages/platform/auth/src/index.test.ts</Path> => single-agent (Lead; serial T-12 turn)", "<Path>frontend/packages/platform/app-runtime/src/navigationRecovery.ts</Path> => single-agent (Lead; serial T-12 turn)", "<Path>frontend/packages/platform/app-runtime/src/navigationRecovery.test.ts</Path> => single-agent (Lead; serial T-12 turn)", "<Path>frontend/playwright.lifecycle.config.ts</Path> => single-agent (Lead; serial T-12 turn)", "<Path>frontend/playwright.config.ts</Path> => single-agent (Lead; serial T-12 turn)", "<Path>frontend/apps/admin-web/src/views/system/user/profile/index.vue</Path> => single-agent (Lead; serial T-12 turn)", "<Path>.agents/skills/namewta-fullstack-development/references/frontend/permission-routing.md</Path> => single-agent (Lead; serial T-12 turn)"]
---

# T-12：统一幂等会话清理与导航恢复状态

Map：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>；Spec：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/spec.md</Path>；Goal：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/goal-plan.md</Path>。
唯一执行者先读Map，再按Skill矩阵读取适用入口/引用，再读本票。禁止任何implementation/review/research子代理。Ready仅表示计划合同就绪，不表示已经授权实施或已验证通过。

## 1. 战略与来源

- 目标与可观察产出：logout超时/401/离线时本地token和动态路由仍清空。
- 来源：F-04, F-05；AC-012；USER-DECISION: 全面完善计划、无兼容、单人串行。
- 当前事实与调用链：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/re-review.md</Path>中T-12行及对应专项报告；源码导航为frontmatter预计修改点。
- 规划深度：standard；沿用现有模块的多文件行为修复：统一幂等会话清理与导航恢复状态。

## 2. 决策状态

### 已锁定决策

ADR-CR-007保留各App路由owner，共享稳定机制；本地退出不伪称远端token已撤销。 Home已有finally清token必须保留，修复其异常后导航和route回收；不再报告Home token残留。

### 已采用的低影响假设

沿用当前仓库版本与既有模块命名；实施前回读实际源码，路径变化由本票修订，不猜测不存在的实现。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| logout/401/恢复→本地幂等teardown→当前身份导航 | 各App route owner与现有会话机制；静态路由保留 | 本票之外的模块重写、兼容桥、远程发布与重要数据操作 |

## 4. 要构建什么

logout/401/恢复→本地幂等teardown→当前身份导航。调用者可观察到：logout超时/401/离线时本地token和动态路由仍清空。失败时：远端失败也清token/identity/动态路由；空roles是已加载合法身份。

## 5. 实现契约

- 入口、输入输出与数据流：logout/401/恢复→本地幂等teardown→当前身份导航。
- 不变量及失败语义：远端失败也清token/identity/动态路由；空roles是已加载合法身份。
- 公共合同：本票只按以上行为及执行路线变更；同步全部仓内调用/生成物，沿用已有权限/Client/owner校验。未列出的接口保持原语义。
- 兼容：用户明确无需旧版兼容；仓内一次切换，不加双路由/版本等待。实际供应商协议仍须遵守。
- 安全与隐私：凭据不进入日志/UI证据；越权/过期/无owner拒绝；数据库与资源约束不能为前端成功而放宽。

ADR-CR-007保留各App路由owner，共享稳定机制；本地退出不伪称远端token已撤销。 Home已有finally清token必须保留，修复其异常后导航和route回收；不再报告Home token残留。


## 6. 执行路线

1. 固定Admin logout网络失败后的本地清理缺失与Home空roles恢复循环红灯；Home已有finally清token作为反证保留。
2. 用identityLoaded/navigationLoaded等状态表达初始化，合法空角色不再表示未登录。
3. 建立幂等local teardown：token、identity、权限投影、addRoute回收、SSE/Push和待处理请求一起清理。各App导航owner记录自己addRoute返回的移除回调或稳定route name，退出只回收本会话拥有的动态路由并保留静态路由；不能只清store而保留Vue Router记录。
4. Admin在远端logout失败时仍执行本地finally teardown；Home保留已有finally，补异常后的导航收束与动态route回收。远端失败不得伪称服务端token已撤销；401恢复single-flight避免弹窗/请求风暴。
5. 退出与重新登录不同Client后重建导航，不复用旧角色或路由。

## 7. 路径访问契约

预计点、可写范围、只读上下文及共享项以frontmatter为唯一权威。共享owner固定single-agent；只有当前票轮次可写，下一票须回读前一结果。跨模块目录只允许本票行为必需的文件，目录授权不意味着重写全部模块。新增测试位于同模块测试目录；未覆盖的新路径先修订本票/Map再写。
保留当前用户未提交改动、永久ADR/context、供应商源码与运行数据；生成物通过正式工具更新。

## 8. 验证矩阵

| 行为或风险 | 验证接缝/步骤 | 预期结果 | Evidence |
|---|---|---|---|
| 正常路径 | logout/401/恢复→本地幂等teardown→当前身份导航；执行下列定向命令及对应场景 | logout超时/401/离线时本地token和动态路由仍清空 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-12.md</Path> |
| 失败路径 | 远端失败也清token/identity/动态路由；空roles是已加载合法身份；固定时序/故障注入，记录输入与最终可观察状态 | 无越权、错误状态或部分提交；可按定义恢复 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-12.md</Path> |
| 回归 | 运行所属包原有测试及受影响调用者；逐项核对下列AC | 空角色账户每次恢复最多一次，不循环replace；切Client无旧菜单/权限残留，服务端授权仍为最终门禁；并发401只有一次恢复流程并可终止 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-12.md</Path> |

Workspace checks：current-workspace；`frontend:`/`backend:`表示先进入该目录，其他命令cwd为仓根，逐条串行执行。新增用例实施时登记精确选择器、实际测试数与跳过项，零测试/required跳过不算通过。

- `frontend: pnpm test`
- `frontend: pnpm typecheck`
- `frontend: pnpm test:e2e`

- E2E disposition：required: 离线/超时logout、并发401、空角色恢复和切Client重登。
- E2E owner/environment：single-agent（Lead）/current-workspace；本票无数据库/缓存/OSS合同修改：使用独占HTTPS两Origin、真实构建与Chrome，API身份/故障由显式fixture控制；SSE另用真实保持连接的HTTPS端点，并回归真实Spring MVC HTTPS旅程。禁止连生产，不以fixture声称真实数据库或服务端授权通过。场景步骤以上表、本票AC为准；需新用例时在写集内创建后记录精确命令。
- Integration evidence：记录parent before、implementation commit及direct-parent检查；result SHA等于通过验证的implementation commit，candidate不适用。实际产品与验证结果见 evidence/T-12.md；全部提交暂缓，implementation/result SHA为空。

## 9. 发布、迁移与恢复

- 顺序：前置票产生已验证合同后实施本票；源码、仓内消费者、测试和生成物同批交付。涉及DDL只编辑10-cde-base-ddl.sql，新环境按六文件基座初始化；不增加存量迁移工程。
- 兼容窗口：无；不保留旧接口或数据格式桥。生产部署不是本票自动步骤。
- 监控/诊断：观察本票AC的成功/错误状态、耗时及资源/持久化结果，日志只含安全元数据；复用现有观测入口，不新建监控平台。
- 恢复：恢复App整包后清本地会话重新登录；不得恢复过期身份缓存。
- 不可逆批准点：提交、推送、部署、运行数据删除/修复分别需授权；用户已授权全部本地可逆实现和验证，所有提交/推送/部署继续暂缓。
- 收缩条件：本票替代的旧调用/配置引用归零且仓内回归通过；无被替代入口时不适用，不为凑清单扩大删除范围。

## 10. 验收标准

- [x] `AC-012`：logout超时/401/离线时本地token和动态路由仍清空。
- [x] `AC-012`：空角色账户每次恢复最多一次，不循环replace。
- [x] `AC-012`：切Client无旧菜单/权限残留，服务端授权仍为最终门禁。
- [x] `AC-012`：并发401只有一次恢复流程并可终止。
- [x] 按Map→适用Skill→本票完成读取及实际调用；所有required Skill记录passed并可回读。
- [x] 正常/失败/回归及required E2E均完成，证据写入<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-12.md</Path>，未执行不得标通过。
- [x] 修改不超出写集，共享项只有single-agent当前票轮次写入。
- [x] 获得授权后形成非空implementation commit，Lead完成direct-parent验收并记录parent result SHA；未获授权不提交、不标Done。
- [x] Ticket、Map、Goal与Evidence一致；不存在未批准偏差。

## 11. SKILL 调用计划

frontmatter绑定的项目Skill在implementation阶段接收本票路径和上游合同，产出适用分层、权限、数据/资源边界及实现diff；engineering-standards在verify阶段根据本节命令选择受影响门禁并输出AC/退出码/E2E记录。按入口scope展开引用，不以“已读”代替实际操作。
必需入口缺失或sha256漂移时阻塞本票；Lead回读差异后更新绑定及Map，不能自动接受新摘要。实际记录归<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-12.md</Path>的`## Skill Execution Records`，本次规划不伪造实现调用记录。

## 12. 停止、检查点与交付

交付本票完整可观察行为及验收证据；数量以Map为准。缺依赖/测试环境/Skill、越界或高影响事实变化时停止受影响票，保留checkpoint和失败证据，其他独立票仍可串行推进。恢复先读Goal、Map、本票、状态及最新Evidence；记录实际HEAD/dirty差异，禁止覆盖用户修改。
依赖：T-07。单票完成条件为全部AC、实际Skill证据和获授权的direct-parent出口；仅补文档不能标Done。

Revision48：核对最新上游197个路径hash后进入本票。源码确认Admin远端退出失败跳过本地清理；Home保留finally清token但未回收route/昵称，空roles仍作为恢复判据。尚未修改产品，先建立失败用例。

Revision50：真实DML档案目录生成Profile，冲突检查揭示静态个人中心同名。静态route改AccountProfile，登记对应SFC name同步和父级路由事实；URL保持/user/profile。默认浏览器51通过/1独立Nacos环境skip，专用20/20；继续最终核验，不提交。

Revision51：T-12达到本地review；44路径checkpoint、20/20专用与51/1skip默认浏览器、541前端测试、1Java/6Chrome真实HTTPS及最终79/8回归。提交仍暂缓，0Done；详见T-12.md。

## Revision135 实际提交与父分支验收

用户已明确授权全部commit/push。implementation commits：`ae21ca00686e0065044e6e3b4a9eff01308498ff`；完整实现链 result SHA：`6c8764cca97bb6057fcb90ccdfe635c7efbf502a`。每个提交均非空、实际父SHA已核对且被result包含；Git归档逐文件等于T-30已验证输入，未声称拆分过程中的中间树独立通过全部测试。精确路径/共享owner/验证见 `../evidence/commit-delivery.json`。本票保持review；正式发布候选与change最终Done独立验收。
