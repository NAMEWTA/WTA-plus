---
schema_version: 3
plan_contract_version: 1
skill_scan: "2026-09-18枚举.agents/skills入口并按本票真实路径/领域绑定；Map为最低集合"
skill_bindings: [{"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/08-complete-sso-release.md</Path>", "<Path>release-artifacts/docker/docker-compose-frontend.yml</Path>", "<Path>release-artifacts/docker/frontend/nginx/</Path>"], "outputs": ["T-08的架构/权限/数据边界检查与定向实现diff"], "required": true, "on_failure": "block-ticket"}, {"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "verify", "operation": "verify-affected-contract-and-quality-gates", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/08-complete-sso-release.md</Path>", "current-workspace实际diff及本票验证矩阵"], "outputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-08.md</Path>：命令、退出码、测试数、AC与Skill Execution Records"], "required": true, "on_failure": "block-ticket"}, {"id": "namewta-fullstack-development", "path": "<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>", "sha256": "675c053c11d8b22cd394c875f48688242d8e8328dd14e69657f5d5b12f2af68b", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>release-artifacts/apps.json</Path>", "<Path>frontend/apps/sso-web/</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/sso/SsoHttpsSessionIntegrationTest.java</Path>"], "outputs": ["独立SSO Origin发布配套和真实SSO/Redis/MySQL/浏览器验证，不修改业务认证授权策略"], "required": true, "on_failure": "block-ticket"}]
resource_claims: ["workspace:current-exclusive", "finding:D-02", "contract:AC-008"]
artifact: ticket
change: 2026-09-14-wta-plus-comprehensive-review
id: T-08
title: 补齐SSO独立Origin发布合同
status: "review"
planning_depth: "deep"
planning_depth_reason: "安全/鉴权、公共合同、数据一致性或共享核心路径变更：补齐SSO独立Origin发布合同"
ready: true
risk: high
blocked_by: ["T-07", "T-09", "T-10"]
contract_ids: [AC-008]
owner: single-agent
expected_changes: ["<Path>release-artifacts/docker/docker-compose-frontend.yml</Path>", "<Path>release-artifacts/docker/frontend/nginx/</Path>", "<Path>release-artifacts/.env.example</Path>", "<Path>release-artifacts/scripts/release-manage.sh</Path>", "<Path>release-artifacts/tests/</Path>", "<Path>release-artifacts/skills/wta-namewta-nginx-config/</Path>", "<Path>release-artifacts/README.md</Path>", "<Path>frontend/apps/sso-web/</Path>", "<Path>frontend/e2e/</Path>", "<Path>frontend/playwright.sso.config.ts</Path>", "<Path>scripts/sso-hard-e2e.sh</Path>", "<Path>release-artifacts/apps.json</Path>", "<Path>release-artifacts/scripts/release-state.py</Path>", "<Path>release-artifacts/scripts/verify-release.sh</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/security/TrustedClientAddressIntegrationTest.java</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/sso/SsoHttpsSessionIntegrationTest.java</Path>", "<Path>.agents/skills/engineering-standards/references/project/00-project-profile.md</Path>", "<Path>release-artifacts/docker/docker-compose-backend.yml</Path>", "<Path>release-artifacts/scripts/docker-manage.sh</Path>", "<Path>backend/wta-modules/wta-sso/src/main/java/org/namewta/sso/config/SsoProperties.java</Path>", "<Path>backend/wta-admin/src/main/java/org/namewta/web/controller/AuthController.java</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/web/controller/AuthClientContextSsoUnitTest.java</Path>", "<Path>backend/wta-admin/src/main/resources/application.yml</Path>"]
writable_paths: ["<Path>release-artifacts/docker/docker-compose-frontend.yml</Path>", "<Path>release-artifacts/docker/frontend/nginx/</Path>", "<Path>release-artifacts/.env.example</Path>", "<Path>release-artifacts/scripts/release-manage.sh</Path>", "<Path>release-artifacts/tests/</Path>", "<Path>release-artifacts/skills/wta-namewta-nginx-config/</Path>", "<Path>release-artifacts/README.md</Path>", "<Path>frontend/apps/sso-web/</Path>", "<Path>frontend/e2e/</Path>", "<Path>frontend/playwright.sso.config.ts</Path>", "<Path>scripts/sso-hard-e2e.sh</Path>", "<Path>release-artifacts/apps.json</Path>", "<Path>release-artifacts/scripts/release-state.py</Path>", "<Path>release-artifacts/scripts/verify-release.sh</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/security/TrustedClientAddressIntegrationTest.java</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/sso/SsoHttpsSessionIntegrationTest.java</Path>", "<Path>.agents/skills/engineering-standards/references/project/00-project-profile.md</Path>", "<Path>release-artifacts/docker/docker-compose-backend.yml</Path>", "<Path>release-artifacts/scripts/docker-manage.sh</Path>", "<Path>backend/wta-modules/wta-sso/src/main/java/org/namewta/sso/config/SsoProperties.java</Path>", "<Path>backend/wta-admin/src/main/java/org/namewta/web/controller/AuthController.java</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/web/controller/AuthClientContextSsoUnitTest.java</Path>", "<Path>backend/wta-admin/src/main/resources/application.yml</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/</Path>", "<Path>{roots.state}/specdev/adr/</Path>"]
shared_paths: ["<Path>release-artifacts/docker/docker-compose-frontend.yml</Path>", "<Path>release-artifacts/docker/frontend/nginx/</Path>", "<Path>release-artifacts/scripts/release-manage.sh</Path>", "<Path>release-artifacts/tests/</Path>", "<Path>release-artifacts/README.md</Path>", "<Path>frontend/apps/sso-web/</Path>", "<Path>frontend/e2e/</Path>", "<Path>frontend/playwright.sso.config.ts</Path>", "<Path>release-artifacts/scripts/release-state.py</Path>", "<Path>release-artifacts/scripts/verify-release.sh</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/security/TrustedClientAddressIntegrationTest.java</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/sso/SsoHttpsSessionIntegrationTest.java</Path>", "<Path>.agents/skills/engineering-standards/references/project/00-project-profile.md</Path>", "<Path>release-artifacts/docker/docker-compose-backend.yml</Path>", "<Path>release-artifacts/scripts/docker-manage.sh</Path>", "<Path>backend/wta-modules/wta-sso/src/main/java/org/namewta/sso/config/SsoProperties.java</Path>", "<Path>backend/wta-admin/src/main/java/org/namewta/web/controller/AuthController.java</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/web/controller/AuthClientContextSsoUnitTest.java</Path>", "<Path>backend/wta-admin/src/main/resources/application.yml</Path>"]
shared_path_owners: ["<Path>release-artifacts/docker/docker-compose-frontend.yml</Path> => single-agent (Lead; serial T-08 turn)", "<Path>release-artifacts/docker/frontend/nginx/</Path> => single-agent (Lead; serial T-08 turn)", "<Path>release-artifacts/scripts/release-manage.sh</Path> => single-agent (Lead; serial T-08 turn)", "<Path>release-artifacts/tests/</Path> => single-agent (Lead; serial T-08 turn)", "<Path>release-artifacts/README.md</Path> => single-agent (Lead; serial T-08 turn)", "<Path>frontend/apps/sso-web/</Path> => single-agent (Lead; serial T-08 turn)", "<Path>frontend/e2e/</Path> => single-agent (Lead; serial T-08 turn)", "<Path>frontend/playwright.sso.config.ts</Path> => single-agent (Lead; serial T-08 turn)", "<Path>release-artifacts/scripts/release-state.py</Path> => single-agent (Lead; serial T-08 turn)", "<Path>release-artifacts/scripts/verify-release.sh</Path> => single-agent (Lead; serial T-08 turn)", "<Path>backend/wta-admin/src/test/java/org/namewta/test/security/TrustedClientAddressIntegrationTest.java</Path> => single-agent (Lead; serial T-08 turn)", "<Path>backend/wta-admin/src/test/java/org/namewta/test/sso/SsoHttpsSessionIntegrationTest.java</Path> => single-agent (Lead; serial T-08 turn)", "<Path>.agents/skills/engineering-standards/references/project/00-project-profile.md</Path> => single-agent (Lead; serial T-08 turn)", "<Path>release-artifacts/docker/docker-compose-backend.yml</Path> => single-agent (Lead; serial T-08 turn)", "<Path>release-artifacts/scripts/docker-manage.sh</Path> => single-agent (Lead; serial T-08 turn)", "<Path>backend/wta-modules/wta-sso/src/main/java/org/namewta/sso/config/SsoProperties.java</Path> => single-agent (Lead; serial T-08 turn)", "<Path>backend/wta-admin/src/main/java/org/namewta/web/controller/AuthController.java</Path> => single-agent (Lead; serial T-08 turn)", "<Path>backend/wta-admin/src/test/java/org/namewta/web/controller/AuthClientContextSsoUnitTest.java</Path> => single-agent (Lead; serial T-08 turn)", "<Path>backend/wta-admin/src/main/resources/application.yml</Path> => single-agent (Lead; serial T-08 turn)"]
---

# T-08：补齐SSO独立Origin发布合同

Map：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>；Spec：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/spec.md</Path>；Goal：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/goal-plan.md</Path>。
唯一执行者先读Map，再按Skill矩阵读取适用入口/引用，再读本票。禁止任何implementation/review/research子代理。Ready仅表示计划合同就绪，不表示已经授权实施或已验证通过。

## 1. 战略与来源

- 目标与可观察产出：每个shipped App均有且仅有完整配套，缺项在promotion前失败。
- 来源：D-02；AC-008；USER-DECISION: 全面完善计划、无兼容、单人串行。
- 当前事实与调用链：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/re-review.md</Path>中T-08行及对应专项报告；源码导航为frontmatter预计修改点。
- 规划深度：deep；安全/鉴权、公共合同、数据一致性或共享核心路径变更：补齐SSO独立Origin发布合同。

## 2. 决策状态

### 已锁定决策

兑现ADR-0073/0074的独立SSO Origin；本地隔离三Origin验证，真实域名/TLS/端口只作为发布Gate输入。

### 已采用的低影响假设

沿用当前仓库版本与既有模块命名；实施前回读实际源码，路径变化由本票修订，不猜测不存在的实现。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 显式三App发布清单→Compose/Nginx/env→独立SSO Origin | T-09构建名单、T-10 manifest与现有Compose；无生产DNS/TLS推测 | 本票之外的模块重写、兼容桥、远程发布与重要数据操作 |

## 4. 要构建什么

显式三App发布清单→Compose/Nginx/env→独立SSO Origin。调用者可观察到：每个shipped App均有且仅有完整配套，缺项在promotion前失败。失败时：缺App/template/env在promotion前失败；VITE_SSO_API控制/sso反代。

## 5. 实现契约

- 入口、输入输出与数据流：显式三App发布清单→Compose/Nginx/env→独立SSO Origin。
- 不变量及失败语义：缺App/template/env在promotion前失败；VITE_SSO_API控制/sso反代。
- 公共合同：本票只按以上行为及执行路线变更；同步全部仓内调用/生成物，沿用已有权限/Client/owner校验。未列出的接口保持原语义。
- 兼容：用户明确无需旧版兼容；仓内一次切换，不加双路由/版本等待。实际供应商协议仍须遵守。
- 安全与隐私：凭据不进入日志/UI证据；越权/过期/无owner拒绝；数据库与资源约束不能为前端成功而放宽。

正确SSO浏览器命令为cwd <Path>frontend</Path>：`pnpm exec playwright test --config playwright.sso.config.ts`。默认配置忽略SSO测试，不能使用默认pnpm test:e2e冒充。现有SSO config无webServer；Lead先提供隔离服务与真实origin/callback矩阵。生产域名与TLS未确定则保持not-ready。SSO独立Origin不是同站点一个新prefix。只修改.env.example占位；真实.env/证书/DNS不在写集。

## 6. 执行路线

1. 将三App的package、prefix、origin、port、API路由、callback、template、shipped状态写入显式发布清单；复用T-09构建矩阵和T-10 release manifest，独立Origin必须改变scheme/host/port之一，不能以同Origin路径冒充ADR-0073；生产SSO还必须与业务App使用不同hostname，因为host-only Cookie不以端口隔离。
2. 移除目录扫描隐式决定发布面的行为；未登记App不得悄悄进入产物。
3. 补SSO Compose服务、Nginx模板、/sso反代、health/env与端口台账；<Path>frontend/apps/sso-web/src/ssoApi.ts</Path> 使用VITE_SSO_API和/sso/*，通用VITE_APP_BASE_API不会自动配置SSO API。
4. 验证静态部署子路径、HTTPS Cookie和三App独立存储/会话恢复。
5. 在本地隔离环境完成模板/Compose和浏览器验收，真实DNS/TLS/部署留到具体产物批准。

## 7. 路径访问契约

预计点、可写范围、只读上下文及共享项以frontmatter为唯一权威。共享owner固定single-agent；只有当前票轮次可写，下一票须回读前一结果。跨模块目录只允许本票行为必需的文件，目录授权不意味着重写全部模块。新增测试位于同模块测试目录；未覆盖的新路径先修订本票/Map再写。
保留当前用户未提交改动、永久ADR/context、供应商源码与运行数据；生成物通过正式工具更新。

## 8. 验证矩阵

| 行为或风险 | 验证接缝/步骤 | 预期结果 | Evidence |
|---|---|---|---|
| 正常路径 | 显式三App发布清单→Compose/Nginx/env→独立SSO Origin；执行下列定向命令及对应场景 | 每个shipped App均有且仅有完整配套，缺项在promotion前失败 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-08.md</Path> |
| 失败路径 | 缺App/template/env在promotion前失败；VITE_SSO_API控制/sso反代；固定时序/故障注入，记录输入与最终可观察状态 | 无越权、错误状态或部分提交；可按定义恢复 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-08.md</Path> |
| 回归 | 运行所属包原有测试及受影响调用者；逐项核对下列AC | SSO三端跳转、刷新、过期、跨OriginCookie可运行；已发布current在配置失败时不改变；发布清单与Compose、Nginx、文档一致 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-08.md</Path> |

Workspace checks：current-workspace；`frontend:`/`backend:`表示先进入该目录，其他命令cwd为仓根，逐条串行执行。新增用例实施时登记精确选择器、实际测试数与跳过项，零测试/required跳过不算通过。

- `bash release-artifacts/scripts/verify-release.sh`
- `frontend: pnpm exec playwright test --config playwright.sso.config.ts`

- E2E disposition：required: 本地三Origin HTTPS部署候选，登录、刷新、过期恢复与health可用。
- E2E owner/environment：single-agent（Lead）/current-workspace；使用隔离MySQL/Redis/OSS及必要真实HTTP/浏览器，禁止连生产。场景步骤以上表、本票AC为准；需新用例时在写集内创建后记录精确命令。
- Integration evidence：记录parent before、implementation commit及direct-parent检查；result SHA等于通过验证的implementation commit，candidate不适用。本地检查完成，见T-08.md；提交/direct-parent出口按用户指令暂缓。

## 9. 发布、迁移与恢复

- 顺序：前置票产生已验证合同后实施本票；源码、仓内消费者、测试和生成物同批交付。涉及DDL只编辑10-cde-base-ddl.sql，新环境按六文件基座初始化；不增加存量迁移工程。
- 兼容窗口：无；不保留旧接口或数据格式桥。生产部署不是本票自动步骤。
- 监控/诊断：观察本票AC的成功/错误状态、耗时及资源/持久化结果，日志只含安全元数据；复用现有观测入口，不新建监控平台。
- 恢复：保持旧current；候选验证后才切指针，运行容器显式重建，生产另批。
- 不可逆批准点：提交、推送、部署、运行数据删除/修复分别需授权；本地实现与隔离验证已获授权；用户暂缓本change全部提交，不执行真实发布/部署。
- 收缩条件：本票替代的旧调用/配置引用归零且仓内回归通过；无被替代入口时不适用，不为凑清单扩大删除范围。

## 10. 验收标准

- [x] `AC-008`：每个shipped App均有且仅有完整配套，缺项在promotion前失败。
- [x] `AC-008`：SSO三端跳转、刷新、过期、跨OriginCookie可运行。
- [x] `AC-008`：已发布current在配置失败时不改变。
- [x] `AC-008`：发布清单与Compose、Nginx、文档一致。
- [x] 按Map→适用Skill→本票完成读取及实际调用；所有required Skill记录passed并可回读。
- [x] 正常/失败/回归及required E2E均完成，证据写入<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-08.md</Path>，未执行不得标通过。
- [x] 修改不超出写集，共享项只有single-agent当前票轮次写入。
- [x] 获得授权后形成非空implementation commit，Lead完成direct-parent验收并记录parent result SHA；未获授权不提交、不标Done。
- [x] Ticket、Map、Goal与Evidence一致；不存在未批准偏差。

## 11. SKILL 调用计划

frontmatter绑定的项目Skill在implementation阶段接收本票路径和上游合同，产出适用分层、权限、数据/资源边界及实现diff；engineering-standards在verify阶段根据本节命令选择受影响门禁并输出AC/退出码/E2E记录。按入口scope展开引用，不以“已读”代替实际操作。
必需入口缺失或sha256漂移时阻塞本票；Lead回读差异后更新绑定及Map，不能自动接受新摘要。实际记录归<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-08.md</Path>的`## Skill Execution Records`，本次规划不伪造实现调用记录。

## 12. 停止、检查点与交付

交付本票完整可观察行为及验收证据；数量以Map为准。缺依赖/测试环境/Skill、越界或高影响事实变化时停止受影响票，保留checkpoint和失败证据，其他独立票仍可串行推进。恢复先读Goal、Map、本票、状态及最新Evidence；记录实际HEAD/dirty差异，禁止覆盖用户修改。
依赖：T-07, T-09, T-10。单票完成条件为全部AC、实际Skill证据和获授权的direct-parent出口；仅补文档不能标Done。

### 实施入口（revision 35）

已按T-07→T-09→T-10顺序合并共享路径检查点并逐一核对hash。T-10现将状态逻辑置于release-state.py，故补充该实际写集及apps.json显式清单、verify-release入口，不修改T-10只允许干净单源码完整版本的合同。真实生产域名/TLS继续留在发布Gate；本轮只构建可逆代码与隔离三Origin验证资产，不晋升真实current。

Revision 36：登记已有T-04/T-07两处真实集成夹具的精确修改点；T-04只适配显式BACKEND_SERVER变量，T-07夹具扩展Nginx三Origin验收模式，生产认证实现不改。增加fullstack绑定与工程画像同步。TLS/数据库/容器只使用owned隔离fixture，不采用真实.env或生产凭据。

Revision 37：Chrome无网络Cookie选择实验实际证实localhost不同端口均携带同一Secure HttpOnly Cookie，127.0.0.1则不携带（T-08-cookie-origin-probe.json）。生产Origin校验增加SSO hostname与业务App不同，避免把端口隔离当Cookie隔离；这是Cookie安全合同落实，不选择真实域名或改永久ADR。

Revision 38：源码确认backend Compose未传入已存在的SSO_WEB_ORIGIN/WEB_CORS_ALLOWED_ORIGINS/TRUSTED_PROXY_CIDRS配置；补充Compose及docker-manage精确写集。消费端从已固定版本矩阵导出精确CORS Origin，SSO入口额外限定自己的Origin，避免给业务App开放带SSO Cookie的跨Origin调用；代理CIDR保持显式运行参数，不猜测可信网络。

### 本地验收（revision 40）

116项发布合同、三Origin真实Nginx/Chrome 5场景、T-06默认根路径2场景、T-07回调12场景、33项后端定向与T-04真实代理回归通过；SSO 5项单测/typecheck/lint/build及事实/文档门禁通过。32路径checkpoint见T-08-checkpoint.json；current未晋升，所有commit/result为空，本票review非Done。真实System身份/token签发、同一干净源码完整候选及生产DNS/TLS仍按T-30/G-release验收。

### 实际消费者复核（revision 42）

T-11读取真实AuthController时发现clientContext仍拼接Origin+/authorize，遗漏发布SSO静态prefix；T-08旧浏览器夹具模拟clientContext而未覆盖该出口。重新打开T-08，登记四处精确后端写集，补web-base-path配置与固定版本导出，并让发布浏览器经过真实AuthController.clientContext。历史5/5仅证明当时已声明边界，不能支持真实配置出口；修复后再review。T-11尚无产品修改，暂回ready。

Revision43：真实AuthController的Origin/base闭环已完成；clientContext不再由浏览器模拟。5浏览器/3配置单元/1真实SSO集成、默认Maven674（646pass/28属性skip）与116发布合同通过；36路径v2 checkpoint，恢复review仍未提交。

## Revision135 实际提交与父分支验收

用户已明确授权全部commit/push。implementation commits：`0b71f34a414946a3d203ba84889bbd088035abd2`, `f661de8a81ed96662c3accb8d48a89fbe81bbe3b`；完整实现链 result SHA：`6c8764cca97bb6057fcb90ccdfe635c7efbf502a`。每个提交均非空、实际父SHA已核对且被result包含；Git归档逐文件等于T-30已验证输入，未声称拆分过程中的中间树独立通过全部测试。精确路径/共享owner/验证见 `../evidence/commit-delivery.json`。本票保持review；正式发布候选与change最终Done独立验收。
