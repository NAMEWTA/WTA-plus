---
schema_version: 3
plan_contract_version: 1
skill_scan: "2026-09-18枚举.agents/skills入口并按本票真实路径/领域绑定；Map为最低集合"
skill_bindings: [{"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/30-integrated-upgrade-acceptance.md</Path>", "<Path>frontend/e2e/</Path>", "<Path>backend/wta-admin/src/test/</Path>", "<Path>backend/wta-modules/wta-profile/wta-profile-enterprise/src/test/java/org/namewta/profile/enterprise/service/impl/EnterpriseApplicationMySqlE2ETest.java</Path>"], "outputs": ["T-30的架构/权限/数据边界检查与定向实现diff"], "required": true, "on_failure": "block-ticket"}, {"id": "namewta-fullstack-development", "path": "<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>", "sha256": "675c053c11d8b22cd394c875f48688242d8e8328dd14e69657f5d5b12f2af68b", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/30-integrated-upgrade-acceptance.md</Path>", "<Path>frontend/e2e/</Path>", "<Path>backend/wta-admin/src/test/</Path>", "<Path>backend/wta-modules/wta-profile/wta-profile-enterprise/src/test/java/org/namewta/profile/enterprise/service/impl/EnterpriseApplicationMySqlE2ETest.java</Path>"], "outputs": ["T-30的架构/权限/数据边界检查与定向实现diff"], "required": true, "on_failure": "block-ticket"}, {"id": "wta-module-guide", "path": "<Path>.agents/skills/wta-module-guide/SKILL.md</Path>", "sha256": "bb57a781314abe316f06ba9538f62043f1bd053904c0832969e575c09731a7a3", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/30-integrated-upgrade-acceptance.md</Path>", "<Path>frontend/e2e/</Path>", "<Path>backend/wta-admin/src/test/</Path>", "<Path>backend/wta-modules/wta-profile/wta-profile-enterprise/src/test/java/org/namewta/profile/enterprise/service/impl/EnterpriseApplicationMySqlE2ETest.java</Path>"], "outputs": ["T-30的架构/权限/数据边界检查与定向实现diff"], "required": true, "on_failure": "block-ticket"}, {"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "verify", "operation": "verify-affected-contract-and-quality-gates", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/30-integrated-upgrade-acceptance.md</Path>", "current-workspace实际diff及本票验证矩阵"], "outputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-30.md</Path>：命令、退出码、测试数、AC与Skill Execution Records"], "required": true, "on_failure": "block-ticket"}]
resource_claims: ["workspace:current-exclusive", "contract:AC-030"]
artifact: ticket
change: 2026-09-14-wta-plus-comprehensive-review
id: T-30
title: 完成升级整体验收与可审查交付
status: "review"
planning_depth: "deep"
planning_depth_reason: "安全/鉴权、公共合同、数据一致性或共享核心路径变更：完成升级整体验收与可审查交付"
ready: true
risk: high
blocked_by: ["T-01", "T-02", "T-03", "T-04", "T-05", "T-06", "T-07", "T-08", "T-09", "T-10", "T-11", "T-12", "T-13", "T-14", "T-15", "T-16", "T-17", "T-18", "T-19", "T-20", "T-21", "T-22", "T-23", "T-24", "T-25", "T-26", "T-27", "T-28", "T-29", "T-31"]
contract_ids: [AC-030]
owner: single-agent
expected_changes: ["<Path>frontend/e2e/</Path>", "<Path>backend/wta-admin/src/test/</Path>", "<Path>scripts/ci/</Path>", "<Path>release-artifacts/tests/</Path>", "<Path>backend/wta-modules/wta-profile/wta-profile-enterprise/src/test/java/org/namewta/profile/enterprise/service/impl/EnterpriseApplicationMySqlE2ETest.java</Path>"]
writable_paths: ["<Path>frontend/e2e/</Path>", "<Path>backend/wta-admin/src/test/</Path>", "<Path>scripts/ci/</Path>", "<Path>release-artifacts/tests/</Path>", "<Path>backend/wta-modules/wta-profile/wta-profile-enterprise/src/test/java/org/namewta/profile/enterprise/service/impl/EnterpriseApplicationMySqlE2ETest.java</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/</Path>", "<Path>{roots.state}/specdev/adr/</Path>"]
shared_paths: ["<Path>frontend/e2e/</Path>", "<Path>backend/wta-admin/src/test/</Path>", "<Path>scripts/ci/</Path>", "<Path>release-artifacts/tests/</Path>", "<Path>backend/wta-modules/wta-profile/wta-profile-enterprise/src/test/java/org/namewta/profile/enterprise/service/impl/EnterpriseApplicationMySqlE2ETest.java</Path>"]
shared_path_owners: ["<Path>frontend/e2e/</Path> => single-agent (Lead; serial T-30 turn)", "<Path>backend/wta-admin/src/test/</Path> => single-agent (Lead; serial T-30 turn)", "<Path>scripts/ci/</Path> => single-agent (Lead; serial T-30 turn)", "<Path>release-artifacts/tests/</Path> => single-agent (Lead; serial T-30 turn)", "<Path>backend/wta-modules/wta-profile/wta-profile-enterprise/src/test/java/org/namewta/profile/enterprise/service/impl/EnterpriseApplicationMySqlE2ETest.java</Path> => single-agent (Lead; serial T-30 turn)"]
---

# T-30：完成升级整体验收与可审查交付

Map：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>；Spec：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/spec.md</Path>；Goal：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/goal-plan.md</Path>。
唯一执行者先读Map，再按Skill矩阵读取适用入口/引用，再读本票。禁止任何implementation/review/research子代理。Ready仅表示计划合同就绪，不表示已经授权实施或已验证通过。

## 1. 战略与来源

- 目标与可观察产出：全部已接受AC均有实际命令/退出码/环境/源码checkpoint。
- 来源：全部已纳入票据的最终合同；AC-030；USER-DECISION: 全面完善计划、无兼容、单人串行。
- 当前事实与调用链：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/re-review.md</Path>中T-30行及对应专项报告；源码导航为frontmatter预计修改点。
- 规划深度：deep；安全/鉴权、公共合同、数据一致性或共享核心路径变更：完成升级整体验收与可审查交付。

## 2. 决策状态

### 已锁定决策

本票已完成全部责任票本地合同闭合后的可逆整体验收；654累计路径与3577源码路径核对一致，完整覆盖见T-30-coverage-final.json。用户保持提交暂缓，正式Done及可发布版本出口未关闭。

### 已采用的低影响假设

沿用当前仓库版本与既有模块命名；实施前回读实际源码，路径变化由本票修订，不猜测不存在的实现。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 同一候选→跨模块/E2E/产物验收→可审查交付 | 各票已建测试与真实服务；不以纯Evidence/空commit冒充实现 | 本票之外的模块重写、兼容桥、远程发布与重要数据操作 |

## 4. 要构建什么

同一候选→跨模块/E2E/产物验收→可审查交付。调用者可观察到：全部已接受AC均有实际命令/退出码/环境/源码checkpoint。失败时：任一required未跑/失败不算通过；full JAR先验后core clean。

## 5. 实现契约

- 入口、输入输出与数据流：同一候选→跨模块/E2E/产物验收→可审查交付。
- 不变量及失败语义：任一required未跑/失败不算通过；full JAR先验后core clean。
- 公共合同：本票只按以上行为及执行路线变更；同步全部仓内调用/生成物，沿用已有权限/Client/owner校验。未列出的接口保持原语义。
- 兼容：用户明确无需旧版兼容；仓内一次切换，不加双路由/版本等待。实际供应商协议仍须遵守。
- 安全与隐私：凭据不进入日志/UI证据；越权/过期/无owner拒绝；数据库与资源约束不能为前端成功而放宽。

最终验收使用同一源码候选和完整产物；full JAR在core clean前验证。默认Playwright不含SSO专用用例，单列playwright.sso.config.ts与新增self材料用例；现有profile-management测试不能自动代表新self流程。各责任票已分别保存本地验证；本轮按当前源码指纹完整运行实际门禁；历史结果保留，未执行的新门禁不算通过。

## 6. 执行路线

1. 回读所有已接受Ticket与ADR，未接受项明确移出实施范围并保留理由，不默认为完成。
2. 在同一候选版本运行前端architecture/lint/typecheck/unit/dev-prod与后端测试/full-core构建。
3. 隔离MySQL/Redis/MinIO/双后端验证SSO、Profile材料、workflow动作/权限、通知fence/callback、Third crash/限额、部门树。
4. 校验release artifact source/digest、stage失败不污染、三Origin Nginx/Compose与UI/a11y。
5. 复核所有旧入口/兼容桥删除映射及重要数据迁移dry-run、恢复演练。
6. 交付完整证据、残余风险和具体发布候选，提交/推送/部署均在用户明确批准后执行。

## 7. 路径访问契约

预计点、可写范围、只读上下文及共享项以frontmatter为唯一权威。共享owner固定single-agent；只有当前票轮次可写，下一票须回读前一结果。跨模块目录只允许本票行为必需的文件，目录授权不意味着重写全部模块。新增测试位于同模块测试目录；未覆盖的新路径先修订本票/Map再写。
保留当前用户未提交改动、永久ADR/context、供应商源码与运行数据；生成物通过正式工具更新。

## 8. 验证矩阵

| 行为或风险 | 验证接缝/步骤 | 预期结果 | Evidence |
|---|---|---|---|
| 正常路径 | 同一候选→跨模块/E2E/产物验收→可审查交付；执行下列定向命令及对应场景 | 全部已接受AC均有实际命令/退出码/环境/源码checkpoint | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-30.md</Path> |
| 失败路径 | 任一required未跑/失败不算通过；full JAR先验后core clean；固定时序/故障注入，记录输入与最终可观察状态 | 无越权、错误状态或部分提交；可按定义恢复 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-30.md</Path> |
| 回归 | 运行所属包原有测试及受影响调用者；逐项核对下列AC | required E2E与失败注入全部完成，not-run不能被标通过；未增加安全/类型豁免或删除测试制造绿色；用户能据artifact digest批准明确候选，未自动发布 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-30.md</Path> |

Workspace checks：current-workspace；`frontend:`/`backend:`表示先进入该目录，其他命令cwd为仓根，逐条串行执行。新增用例实施时登记精确选择器、实际测试数与跳过项，零测试/required跳过不算通过。

- `frontend: pnpm architecture:check`
- `frontend: pnpm lint`
- `frontend: pnpm typecheck`
- `frontend: pnpm test`
- `frontend: pnpm test:e2e`
- `frontend: pnpm build:dev`
- `frontend: pnpm build:prod`
- `frontend: pnpm exec playwright test --config playwright.sso.config.ts`
- `backend: ./mvnw test`
- `backend: ./mvnw clean package -DskipTests`
- `bash scripts/ci/verify-admin-bundle.sh full`
- `backend: ./mvnw clean package -Pbundle-core -Dmaven.test.skip=true`
- `bash scripts/ci/verify-admin-bundle.sh core`
- `bash release-artifacts/scripts/verify-release.sh`
- `bash scripts/ci/run-external-services.sh`

- E2E disposition：required: 同一候选完整运行SSO/Profile/workflow/Notify/Third/树/三App发布及失败恢复。
- E2E owner/environment：single-agent（Lead）/current-workspace；使用隔离MySQL/Redis/OSS及必要真实HTTP/浏览器，禁止连生产。场景步骤以上表、本票AC为准；需新用例时在写集内创建后记录精确命令。
- Integration evidence：记录parent before、implementation commit及direct-parent检查；result SHA等于通过验证的implementation commit，candidate不适用。本票本地整体验收已完成review；源码哈希和验证结果不产生implementation commit或结果SHA。

## 9. 发布、迁移与恢复

- 顺序：前置票产生已验证合同后实施本票；源码、仓内消费者、测试和生成物同批交付。涉及DDL只编辑10-cde-base-ddl.sql，新环境按六文件基座初始化；不增加存量迁移工程。
- 兼容窗口：无；不保留旧接口或数据格式桥。生产部署不是本票自动步骤。
- 监控/诊断：观察本票AC的成功/错误状态、耗时及资源/持久化结果，日志只含安全元数据；复用现有观测入口，不新建监控平台。
- 恢复：保留失败候选与日志；回到责任票修复再重跑受影响闭包，不自动部署。
- 不可逆批准点：提交、推送、部署、运行数据删除/修复分别需授权；本轮实现和本地检查已授权；用户要求全部提交暂缓，不推送或部署。
- 收缩条件：本票替代的旧调用/配置引用归零且仓内回归通过；无被替代入口时不适用，不为凑清单扩大删除范围。

## 10. 验收标准

- [x] `AC-030`：全部已接受AC均有实际命令/退出码/环境/源码checkpoint。
- [x] `AC-030`：required E2E与失败注入全部完成，not-run不能被标通过。
- [x] `AC-030`：未增加安全/类型豁免或删除测试制造绿色。
- [ ] `AC-030`：用户能据artifact digest批准明确候选，未自动发布。
- [x] 按Map→适用Skill→本票完成读取及实际调用；所有required Skill记录passed并可回读。
- [x] 正常/失败/回归及required E2E均完成，证据写入<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-30.md</Path>，未执行不得标通过。
- [x] 修改不超出写集，共享项只有single-agent当前票轮次写入。
- [x] 获得授权后形成非空implementation commit，Lead完成direct-parent验收并记录parent result SHA；未获授权不提交、不标Done。
- [x] Ticket、Map、Goal与Evidence一致；不存在未批准偏差。

## 11. SKILL 调用计划

frontmatter绑定的项目Skill在implementation阶段接收本票路径和上游合同，产出适用分层、权限、数据/资源边界及实现diff；engineering-standards在verify阶段根据本节命令选择受影响门禁并输出AC/退出码/E2E记录。按入口scope展开引用，不以“已读”代替实际操作。
必需入口缺失或sha256漂移时阻塞本票；Lead回读差异后更新绑定及Map，不能自动接受新摘要。实际记录归<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-30.md</Path>的`## Skill Execution Records`，本次规划不伪造实现调用记录。

## 12. 停止、检查点与交付

交付本票完整可观察行为及验收证据；数量以Map为准。缺依赖/测试环境/Skill、越界或高影响事实变化时停止受影响票，保留checkpoint和失败证据，其他独立票仍可串行推进。恢复先读Goal、Map、本票、状态及最新Evidence；记录实际HEAD/dirty差异，禁止覆盖用户修改。
依赖：T-01, T-02, T-03, T-04, T-05, T-06, T-07, T-08, T-09, T-10, T-11, T-12, T-13, T-14, T-15, T-16, T-17, T-18, T-19, T-20, T-21, T-22, T-23, T-24, T-25, T-26, T-27, T-28, T-29, T-31。单票完成条件为全部AC、实际Skill证据和获授权的direct-parent出口；仅补文档不能标Done。

Revision113：T-30可逆准备完成，正式整体验收not-run/AC未勾选。589上游路径一致，3549源码路径指纹保存；10份Playwright配置最终枚举163用例（仅list，产品执行0），workflow专用临时配置另列；16核心门禁及真实服务追加矩阵已保存。17源文件刷新T-03/T-23事实，缺业务/供应商依据仍阻塞。28review/1ready/2blocked/0Done；无暂存/提交/推送/部署。

Revision128：T-30进入同一工作树候选整体验收。30个前置票已完成本地review，648累计路径全部核对；计划T/P与ticket-control均0error，T-03/T-23原未知关闭。源码88a5a9a25f9c3d88def978ac6aac64d60522638cfa43150e0cfb3f4924a9fee0，重建正式门禁/环境/浏览器矩阵，旧准备日志保留不覆盖。30review/1in_progress/0Done；提交/推送/部署全部暂缓，正式交付出口仍未完成。

Revision129：T-30真实依赖门禁发现T-23新增receipt表未同步受保护初始化器：实际126表/预期125，测试尚未开始即exit1，owned资源已恢复。回到T-23补修，先登记初始化脚本及两份发布合同测试精确写集；T-30暂停为ready，29review/1in_progress/1ready/0Done。保留旧源码88a5a9a的通过与失败证据；修正后重新冻结输入并运行受影响发布/真实服务门禁。全部提交继续暂缓。

Revision130：T-23初始化补修完成review：受保护六文件初始化实际126表，发布117项及真实MySQL/Redis/MinIO八项均零skip通过，资源恢复；仅初始化脚本和两处旧计数断言变化。T-23-checkpoint-v2共50路径，累计649路径；源码1dff1a345e1979d809bb547f3060645d86b4508f3df3aad5fd227de53ab2c504。T-30继续整体验收，30review/1in_progress/0Done；未受影响的前端/后端源树逐文件相同，旧验证证据按明确输入等价关系关联，受影响release/external已重跑。全部提交暂缓。

Revision131：T-30补跑默认环境skip发现5个夹具错误：Admin菜单使用失效裸图标，两个OSS测试从旧标记截取至EOF导致重复建表，Profile以分号直接切SQL破坏坐标字面量且截取后续无关域。在本票既有admin测试写集内登记4测试及1共用SQL执行工具：复用真实基座DDL、限定片段、使用Spring SQL脚本解析，Profile在owned空数据库完整初始化五份业务基座并清理全部所建表；保留所有权限/数据/失败关闭断言，生产SQL不放宽。保留T-30-extra-services-v1的15项/5错误，修复后重跑受影响闭包；30review/1in_progress/0Done，全部提交暂缓。

Revision132：T-30补查全部环境门控/Tag发现9个Profile e2e类未被默认Maven选择；真实MySQL补跑15项，13通过/2失败/零skip。企业申请夹具credit(suffix)拼接任意末位，不满足当前统一社会信用代码校验码合同，save在业务入口即被拒绝。先追加该EnterpriseApplicationMySqlE2ETest.java精确写集，仅修正合成合法身份数据并增加响应code断言，保留发布/重新认证/唯一约束/工作流回滚断言及生产校验。浏览器163项及额外10工作流弹窗已实际通过；30review/1in_progress/0Done，全部提交暂缓。

Revision133：T-30本地整体验收完成review。最终源码bc561a9c45850bdb0a8783a7d9700ef299a2d0bd774fecbebb55fb3dc108b3ba，3577源码/654累计owned路径；6测试修复、1上游重叠/648非重叠不变。18核心门禁通过，前端722、浏览器163+工作流10=173；默认后端740通过/117环境skip均有专项零skip闭合，46环境类/264测试源逐类对应，额外非默认选集53通过/1既有教学Disabled占位。最终full/core构建/清单、117发布合同、真实依赖与发布恢复、五分层/facts检查器通过。全部31票review、0Done；仅本地产物可审查，干净提交/正式发布candidate/direct-parent出口因用户全change提交暂缓保持未完成。最终治理结果见T-30-final-governance.json。

本地JAR/三App摘要已提供，但正式可发布candidate依赖干净提交，依用户指令继续暂缓；相应验收项保持未勾选，不冒充发布完成。

Revision134：完成逐票出口复核与本地产物重新验hash，3577源码/654owned路径无漂移，两JAR及三App保留副本一致、HEAD不变且index为空。T-01/T-02/T-04/T-05早期验收勾选尚未承接最终实际证据，已按各项源码/测试报告补齐并注明旧失败由后续票关闭；T-02历史日志处置/凭据轮换属于OUT且无批准，继续明确未执行。31review/0Done；非空implementation/result、direct-parent和干净正式发布候选仍受用户全change不提交约束。没有新产品改动，不重跑已证明输入未变的业务测试。

## Revision135 实际提交与父分支验收

用户已明确授权全部commit/push。implementation commits：`91d4c2ea6356c74d3174f63f2665485484f0a103`；完整实现链 result SHA：`6c8764cca97bb6057fcb90ccdfe635c7efbf502a`。每个提交均非空、实际父SHA已核对且被result包含；Git归档逐文件等于T-30已验证输入，未声称拆分过程中的中间树独立通过全部测试。精确路径/共享owner/验证见 `../evidence/commit-delivery.json`。本票保持review；正式发布候选与change最终Done独立验收。
