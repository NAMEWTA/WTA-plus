---
schema_version: 3
plan_contract_version: 1
skill_scan: "2026-09-18枚举.agents/skills入口并按本票真实路径/领域绑定；Map为最低集合"
skill_bindings: [{"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/09-coherent-build-matrix.md</Path>", "<Path>frontend/package.json</Path>", "<Path>frontend/apps/admin-web/package.json</Path>"], "outputs": ["T-09的架构/权限/数据边界检查与定向实现diff"], "required": true, "on_failure": "block-ticket"}, {"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "verify", "operation": "verify-affected-contract-and-quality-gates", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/09-coherent-build-matrix.md</Path>", "current-workspace实际diff及本票验证矩阵"], "outputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-09.md</Path>：命令、退出码、测试数、AC与Skill Execution Records"], "required": true, "on_failure": "block-ticket"}, {"id": "namewta-fullstack-development", "path": "<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>", "sha256": "675c053c11d8b22cd394c875f48688242d8e8328dd14e69657f5d5b12f2af68b", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql</Path>", "当前Notify实体、DAO、common SPI与历史ADR只读证据"], "outputs": ["T-09旧Notify空schema收缩判断及全新MySQL初始化证据"], "required": true, "on_failure": "block-ticket"}, {"id": "wta-module-guide", "path": "<Path>.agents/skills/wta-module-guide/SKILL.md</Path>", "sha256": "bb57a781314abe316f06ba9538f62043f1bd053904c0832969e575c09731a7a3", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql</Path>", "当前Notify实体、DAO、common SPI与历史ADR只读证据"], "outputs": ["T-09旧Notify空schema收缩判断及全新MySQL初始化证据"], "required": true, "on_failure": "block-ticket"}]
resource_claims: ["workspace:current-exclusive", "finding:D-04", "finding:D-08", "finding:D-09", "contract:AC-009"]
artifact: ticket
change: 2026-09-14-wta-plus-comprehensive-review
id: T-09
title: 统一App模式、bundle与依赖服务验证矩阵
status: "review"
planning_depth: "deep"
planning_depth_reason: "安全/鉴权、公共合同、数据一致性或共享核心路径变更：统一App模式、bundle与依赖服务验证矩阵"
ready: true
risk: high
blocked_by: ["T-01"]
contract_ids: [AC-009]
owner: single-agent
expected_changes: ["<Path>frontend/package.json</Path>", "<Path>frontend/apps/admin-web/package.json</Path>", "<Path>frontend/apps/home-web/package.json</Path>", "<Path>frontend/apps/sso-web/package.json</Path>", "<Path>backend/pom.xml</Path>", "<Path>backend/wta-admin/pom.xml</Path>", "<Path>scripts/ci/verify-admin-bundle.sh</Path>", "<Path>scripts/ci/run-external-services.sh</Path>", "<Path>release-artifacts/scripts/release-manage.sh</Path>", "<Path>release-artifacts/docker/docker-compose-infrastructure.yml</Path>", "<Path>release-artifacts/tests/</Path>", "<Path>frontend/apps/admin-web/vite.config.ts</Path>", "<Path>frontend/apps/home-web/vite.config.ts</Path>", "<Path>frontend/apps/sso-web/vite.config.ts</Path>", "<Path>scripts/README.md</Path>", "<Path>frontend/packages/web-domains/system/src/sso-app/SsoAppPage.vue</Path>", "<Path>frontend/packages/web-domains/notify/src/ConfigPage.vue</Path>", "<Path>release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/migration/BusinessMenuRetirementMySqlIntegrationTest.java</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/notify/monitor/NotifyMonitorMySqlIntegrationTest.java</Path>", "<Path>scripts/ci/verify-external-tests.py</Path>", "<Path>.agents/skills/namewta-fullstack-development/references/backend/mapper-and-sql.md</Path>"]
writable_paths: ["<Path>frontend/package.json</Path>", "<Path>frontend/apps/admin-web/package.json</Path>", "<Path>frontend/apps/home-web/package.json</Path>", "<Path>frontend/apps/sso-web/package.json</Path>", "<Path>backend/pom.xml</Path>", "<Path>backend/wta-admin/pom.xml</Path>", "<Path>scripts/ci/verify-admin-bundle.sh</Path>", "<Path>scripts/ci/run-external-services.sh</Path>", "<Path>release-artifacts/scripts/release-manage.sh</Path>", "<Path>release-artifacts/docker/docker-compose-infrastructure.yml</Path>", "<Path>release-artifacts/tests/</Path>", "<Path>frontend/apps/admin-web/vite.config.ts</Path>", "<Path>frontend/apps/home-web/vite.config.ts</Path>", "<Path>frontend/apps/sso-web/vite.config.ts</Path>", "<Path>scripts/README.md</Path>", "<Path>frontend/packages/web-domains/system/src/sso-app/SsoAppPage.vue</Path>", "<Path>frontend/packages/web-domains/notify/src/ConfigPage.vue</Path>", "<Path>release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/migration/BusinessMenuRetirementMySqlIntegrationTest.java</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/notify/monitor/NotifyMonitorMySqlIntegrationTest.java</Path>", "<Path>scripts/ci/verify-external-tests.py</Path>", "<Path>.agents/skills/namewta-fullstack-development/references/backend/mapper-and-sql.md</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/</Path>", "<Path>{roots.state}/specdev/adr/</Path>"]
shared_paths: ["<Path>frontend/package.json</Path>", "<Path>frontend/apps/admin-web/package.json</Path>", "<Path>frontend/apps/home-web/package.json</Path>", "<Path>frontend/apps/sso-web/package.json</Path>", "<Path>scripts/ci/verify-admin-bundle.sh</Path>", "<Path>scripts/ci/run-external-services.sh</Path>", "<Path>release-artifacts/scripts/release-manage.sh</Path>", "<Path>release-artifacts/tests/</Path>", "<Path>frontend/apps/admin-web/vite.config.ts</Path>", "<Path>frontend/apps/home-web/vite.config.ts</Path>", "<Path>frontend/apps/sso-web/vite.config.ts</Path>", "<Path>scripts/README.md</Path>", "<Path>frontend/packages/web-domains/system/src/sso-app/SsoAppPage.vue</Path>", "<Path>frontend/packages/web-domains/notify/src/ConfigPage.vue</Path>", "<Path>release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/migration/BusinessMenuRetirementMySqlIntegrationTest.java</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/notify/monitor/NotifyMonitorMySqlIntegrationTest.java</Path>", "<Path>scripts/ci/verify-external-tests.py</Path>", "<Path>.agents/skills/namewta-fullstack-development/references/backend/mapper-and-sql.md</Path>"]
shared_path_owners: ["<Path>frontend/package.json</Path> => single-agent (Lead; serial T-09 turn)", "<Path>frontend/apps/admin-web/package.json</Path> => single-agent (Lead; serial T-09 turn)", "<Path>frontend/apps/home-web/package.json</Path> => single-agent (Lead; serial T-09 turn)", "<Path>frontend/apps/sso-web/package.json</Path> => single-agent (Lead; serial T-09 turn)", "<Path>scripts/ci/verify-admin-bundle.sh</Path> => single-agent (Lead; serial T-09 turn)", "<Path>scripts/ci/run-external-services.sh</Path> => single-agent (Lead; serial T-09 turn)", "<Path>release-artifacts/scripts/release-manage.sh</Path> => single-agent (Lead; serial T-09 turn)", "<Path>release-artifacts/tests/</Path> => single-agent (Lead; serial T-09 turn)", "<Path>frontend/apps/admin-web/vite.config.ts</Path> => single-agent (Lead; serial T-09 turn)", "<Path>frontend/apps/home-web/vite.config.ts</Path> => single-agent (Lead; serial T-09 turn)", "<Path>frontend/apps/sso-web/vite.config.ts</Path> => single-agent (Lead; serial T-09 turn)", "<Path>scripts/README.md</Path> => single-agent (Lead; serial T-09 turn)", "<Path>frontend/packages/web-domains/system/src/sso-app/SsoAppPage.vue</Path> => single-agent (Lead; serial T-09 turn)", "<Path>frontend/packages/web-domains/notify/src/ConfigPage.vue</Path> => single-agent (Lead; serial T-09 turn)", "<Path>release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql</Path> => single-agent (Lead; serial T-09 turn)", "<Path>backend/wta-admin/src/test/java/org/namewta/test/migration/BusinessMenuRetirementMySqlIntegrationTest.java</Path> => single-agent (Lead; serial T-09 turn)", "<Path>backend/wta-admin/src/test/java/org/namewta/test/notify/monitor/NotifyMonitorMySqlIntegrationTest.java</Path> => single-agent (Lead; serial T-09 turn)", "<Path>scripts/ci/verify-external-tests.py</Path> => single-agent (Lead; serial T-09 turn)", "<Path>.agents/skills/namewta-fullstack-development/references/backend/mapper-and-sql.md</Path> => single-agent (Lead; serial T-09 turn)"]
---

# T-09：统一App模式、bundle与依赖服务验证矩阵

Map：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>；Spec：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/spec.md</Path>；Goal：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/goal-plan.md</Path>。
唯一执行者先读Map，再按Skill矩阵读取适用入口/引用，再读本票。禁止任何implementation/review/research子代理。Ready仅表示计划合同就绪，不表示已经授权实施或已验证通过。

## 1. 战略与来源

- 目标与可观察产出：build:dev最终三个App均development，build:prod均production。
- 来源：D-04, D-08, D-09；AC-009；USER-DECISION: 全面完善计划、无兼容、单人串行。
- 当前事实与调用链：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/re-review.md</Path>中T-09行及对应专项报告；源码导航为frontmatter预计修改点。
- 规划深度：deep；安全/鉴权、公共合同、数据一致性或共享核心路径变更：统一App模式、bundle与依赖服务验证矩阵。

## 2. 决策状态

### 已锁定决策

ADR-CR-005：显式产品构建名单；保持当前Compose镜像版本，不将镜像升级纳入本票。

### 已采用的低影响假设

沿用当前仓库版本与既有模块命名；实施前回读实际源码，路径变化由本票修订，不猜测不存在的实现。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| build:dev/prod与full/core入口→准确模式和产品组合 | wta-admin产品合同、Compose镜像和现有CI脚本；不引入镜像配置生成器 | 本票之外的模块重写、兼容桥、远程发布与重要数据操作 |

## 4. 要构建什么

build:dev/prod与full/core入口→准确模式和产品组合。调用者可观察到：build:dev最终三个App均development，build:prod均production。失败时：每App每模式只构建一次；full/core缺必需或含禁用模块失败。

## 5. 实现契约

- 入口、输入输出与数据流：build:dev/prod与full/core入口→准确模式和产品组合。
- 不变量及失败语义：每App每模式只构建一次；full/core缺必需或含禁用模块失败。
- 公共合同：本票只按以上行为及执行路线变更；同步全部仓内调用/生成物，沿用已有权限/Client/owner校验。未列出的接口保持原语义。
- 兼容：用户明确无需旧版兼容；仓内一次切换，不加双路由/版本等待。实际供应商协议仍须遵守。
- 安全与隐私：凭据不进入日志/UI证据；越权/过期/无owner拒绝；数据库与资源约束不能为前端成功而放宽。

`full|core`仅为枚举，不是可复制shell命令。正确顺序：cwd <Path>backend</Path>运行`./mvnw test`并保存证据；运行`./mvnw clean package -DskipTests`，随后cwd <Path>.</Path>运行`bash scripts/ci/verify-admin-bundle.sh full`并保存JAR清单；再cwd <Path>backend</Path>运行`./mvnw clean package -Pbundle-core -Dmaven.test.skip=true`，随后cwd <Path>.</Path>运行`bash scripts/ci/verify-admin-bundle.sh core`。不得先clean覆盖full产物才验证full。required名单补齐wta-notify与profile-person/enterprise，保留已校验third/sso。App开发构建后先保存mode marker，再构建prod；源码active与release shipped分开。真实外部服务命令是cwd <Path>.</Path>的`bash scripts/ci/run-external-services.sh`，仅由Lead在专属隔离Docker运行；创建/销毁容器，已运行三轮，最终7类8项零跳过通过，详见T-09 Evidence。

## 6. 执行路线

1. 每个App在每个模式只构建一次；取消build:dev后递归production覆盖Home/SSO。保留依赖构建顺序。
2. 以wta-admin/pom.xml显式产品组合为合同，验证实际JAR；required补wta-notify、profile-person、profile-enterprise。预期名单不能完全由待测JAR/POM即时生成，否则无法发现错误组合。
3. 完整测试后分别clean打包full/core，并在下一次clean前验证和保存当前JAR清单；core使用-Dmaven.test.skip=true，不能删除测试或扩大bundle。
4. 以现有Compose镜像为运行时来源，由CI读取或以小型一致性检查防漂移；本次不强制引入额外镜像manifest/生成器，不盲目升级镜像。

## 7. 路径访问契约

预计点、可写范围、只读上下文及共享项以frontmatter为唯一权威。共享owner固定single-agent；只有当前票轮次可写，下一票须回读前一结果。跨模块目录只允许本票行为必需的文件，目录授权不意味着重写全部模块。新增测试位于同模块测试目录；未覆盖的新路径先修订本票/Map再写。
保留当前用户未提交改动、永久ADR/context、供应商源码与运行数据；生成物通过正式工具更新。

## 8. 验证矩阵

| 行为或风险 | 验证接缝/步骤 | 预期结果 | Evidence |
|---|---|---|---|
| 正常路径 | build:dev/prod与full/core入口→准确模式和产品组合；执行下列定向命令及对应场景 | build:dev最终三个App均development，build:prod均production | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-09.md</Path> |
| 失败路径 | 每App每模式只构建一次；full/core缺必需或含禁用模块失败；固定时序/故障注入，记录输入与最终可观察状态 | 无越权、错误状态或部分提交；可按定义恢复 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-09.md</Path> |
| 回归 | 运行所属包原有测试及受影响调用者；逐项核对下列AC | core/full产物必需与禁用列表逐项断言；测试证据与打包候选匹配，不以skip冒充测试通过；CI与部署镜像版本相同或有明确差异测试 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-09.md</Path> |

Workspace checks：current-workspace；`frontend:`/`backend:`表示先进入该目录，其他命令cwd为仓根，逐条串行执行。新增用例实施时登记精确选择器、实际测试数与跳过项，零测试/required跳过不算通过。

- `frontend: pnpm build:dev`
- `frontend: pnpm build:prod`
- `backend: ./mvnw test`
- `backend: ./mvnw clean package -DskipTests`
- `bash scripts/ci/verify-admin-bundle.sh full`
- `backend: ./mvnw clean package -Pbundle-core -Dmaven.test.skip=true`
- `bash scripts/ci/verify-admin-bundle.sh core`

- E2E disposition：not-required: 构建产物扫描与脚本负向夹具覆盖；线上组合由T-08/T-30验收。
- E2E owner/environment：single-agent（Lead）/current-workspace；使用隔离MySQL/Redis/OSS及必要真实HTTP/浏览器，禁止连生产。场景步骤以上表、本票AC为准；需新用例时在写集内创建后记录精确命令。
- Integration evidence：记录parent before、implementation commit及direct-parent检查；result SHA等于通过验证的implementation commit，candidate不适用。本地构建/真实服务验证完成，证据见T-09.md；正式commit/result仍为空。

## 9. 发布、迁移与恢复

- 顺序：前置票产生已验证合同后实施本票；源码、仓内消费者、测试和生成物同批交付。涉及DDL只编辑10-cde-base-ddl.sql，新环境按六文件基座初始化；不增加存量迁移工程。
- 兼容窗口：无；不保留旧接口或数据格式桥。生产部署不是本票自动步骤。
- 监控/诊断：观察本票AC的成功/错误状态、耗时及资源/持久化结果，日志只含安全元数据；复用现有观测入口，不新建监控平台。
- 恢复：恢复构建脚本/POM并清理本次输出后重建；不覆盖已发布版本。
- 不可逆批准点：提交、推送、部署、运行数据删除/修复分别需授权；最新用户授权本地实现与验证，全change暂不提交。
- 收缩条件：本票替代的旧调用/配置引用归零且仓内回归通过；无被替代入口时不适用，不为凑清单扩大删除范围。

## 10. 验收标准

- [x] `AC-009`：build:dev最终三个App均development，build:prod均production。
- [x] `AC-009`：core/full产物必需与禁用列表逐项断言。
- [x] `AC-009`：测试证据与打包候选匹配，不以skip冒充测试通过。
- [x] `AC-009`：CI与部署镜像版本相同或有明确差异测试。
- [x] 按Map→适用Skill→本票完成读取及实际调用；所有required Skill记录passed并可回读。
- [x] 正常/失败/回归及required E2E均完成，证据写入<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-09.md</Path>，未执行不得标通过。
- [x] 修改不超出写集，共享项只有single-agent当前票轮次写入。
- [x] 获得授权后形成非空implementation commit，Lead完成direct-parent验收并记录parent result SHA；未获授权不提交、不标Done。
- [x] Ticket、Map、Goal与Evidence一致；不存在未批准偏差。

## 11. SKILL 调用计划

frontmatter绑定的项目Skill在implementation阶段接收本票路径和上游合同，产出适用分层、权限、数据/资源边界及实现diff；engineering-standards在verify阶段根据本节命令选择受影响门禁并输出AC/退出码/E2E记录。按入口scope展开引用，不以“已读”代替实际操作。
必需入口缺失或sha256漂移时阻塞本票；Lead回读差异后更新绑定及Map，不能自动接受新摘要。实际记录归<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-09.md</Path>的`## Skill Execution Records`，本次规划不伪造实现调用记录。

## 12. 停止、检查点与交付

交付本票完整可观察行为及验收证据；数量以Map为准。缺依赖/测试环境/Skill、越界或高影响事实变化时停止受影响票，保留checkpoint和失败证据，其他独立票仍可串行推进。恢复先读Goal、Map、本票、状态及最新Evidence；记录实际HEAD/dirty差异，禁止覆盖用户修改。
依赖：T-01。单票完成条件为全部AC、实际Skill证据和获授权的direct-parent出口；仅补文档不能标Done。

### 本地实施决策

实际缺口：build:dev的non-admin递归build会再次使用production脚本；bundle required漏Notify/Profile person/enterprise；CI MinIO为2026-08-04而Compose为2026-04-17，本票保持Compose版本。三个Vite入口在构建产物写入实际mode标记，根入口显式构建三个active App；shipped名单仍由T-08负责。外部服务脚本改为随机唯一资源、本机随机端口、按本轮创建ID清理，禁止清理名称碰撞的既有资源。补充scripts/README.md及三个Vite配置到写集。

构建基线实际退出2，System SsoAppPage clientId联合类型阻塞依赖构建；Admin还报告Notify/System表格槽行类型共7处。将这两页局部类型修正从T-20调查项转交T-09作为构建前置，写集仅这两文件；使用组件泛型和显式显示转换，不放宽编译规则。T-20其余范围/依赖保持，受影响闭包T-09/T-10/T-08/T-30。

Revision 26：T-09真实外部服务在六文件初始化时失败（127 vs 125），将T-22已登记的两张无owner旧表收缩前置转交本票。写集增加唯一基座DDL，仅移除sys_notify_log/sys_notify_delivery_log建表；生产Java/XML、初始化DML无引用，当前11张notify_*表保留。历史永久ADR-0009描述旧sys_notify_log附件方案，与当前无持久化消费者的SPI/新Notify模型不一致；保留历史文档只读，本票不声称恢复旧附件功能，不触及任何既有数据库。T-22仍负责原子写回/lease fence，其余DAG不变；T-09/T-10/T-08/T-30与T-22后续需回读新schema检查点。

Revision27：完整初始化已通过125表，外部服务执行7项（6通过、1旧菜单DSL-003测试失败），NotifyMonitorMySqlIntegrationTest不存在导致零执行。增加两个真实MySQL测试的精确写集：菜单测试验证当前DSL-004完整删除及保留行为，新增当前Notify监控UseCase/Service/DAO/Mapper读取/过滤/500上限测试；CI独立XML结果门禁拒绝missing/stale/zero/skip/failure，报告检查器负向夹具放既有release测试目录。前两轮不同根因已复盘，下次具体改变为当前合同fixture与完整7类非跳过报告验证；不盲目重复、不删除选择器。

Revision28：根据用户AGENTS“Skill与源码冲突同步修正父级Skill”，修正mapper-and-sql reference中已不存在的System通知XML入口，指向当前Notify DAO/Mapper与Outbox XML；不删除或放宽任何硬约束。Skill主入口SHA保持一致。

### 本地审查检查点

AC-009已由实际三App双模式、默认50模块Maven、full/core JAR与7类8项真实服务验证。83项发布合同、28项页面测试及三App类型/lint通过。此前旧Notify schema/八条页面类型阻塞已修复；NotifyOutboxWakePublisher分层现存失败仍归T-28。18文件hash与全部退出码见T-09-checkpoint.json/T-09.md。review仅表示本地实现可审查，提交/result暂缓、0 Done。

## Revision135 实际提交与父分支验收

用户已明确授权全部commit/push。implementation commits：`1b5872750ba8a074ddef150400322f8b287ae69f`, `f4ea9c5a6b2e7dbb5dd859dc5e955b0a59252981`；完整实现链 result SHA：`6c8764cca97bb6057fcb90ccdfe635c7efbf502a`。每个提交均非空、实际父SHA已核对且被result包含；Git归档逐文件等于T-30已验证输入，未声称拆分过程中的中间树独立通过全部测试。精确路径/共享owner/验证见 `../evidence/commit-delivery.json`。本票保持review；正式发布候选与change最终Done独立验收。
