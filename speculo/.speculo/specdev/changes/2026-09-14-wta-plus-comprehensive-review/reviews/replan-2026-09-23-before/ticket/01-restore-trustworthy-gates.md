---
schema_version: 3
plan_contract_version: 1
skill_scan: "2026-09-18枚举.agents/skills入口并按本票真实路径/领域绑定；Map为最低集合"
skill_bindings: [{"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/01-restore-trustworthy-gates.md</Path>", "<Path>.agents/skills/engineering-standards/scripts/validate-skill-facts.mjs</Path>", "<Path>.agents/skills/engineering-standards/scripts/validate-skill-facts.test.mjs</Path>"], "outputs": ["T-01的架构/权限/数据边界检查与定向实现diff"], "required": true, "on_failure": "block-ticket"}, {"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "verify", "operation": "verify-affected-contract-and-quality-gates", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/01-restore-trustworthy-gates.md</Path>", "current-workspace实际diff及本票验证矩阵"], "outputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-01.md</Path>：命令、退出码、测试数、AC与Skill Execution Records"], "required": true, "on_failure": "block-ticket"}]
resource_claims: ["workspace:current-exclusive", "finding:D-01", "finding:D-07", "finding:D-11", "finding:B-09", "contract:AC-001"]
artifact: ticket
change: 2026-09-14-wta-plus-comprehensive-review
id: T-01
title: 恢复可信的仓库门禁与治理入口
status: "review"
planning_depth: "standard"
planning_depth_reason: "沿用现有模块的多文件行为修复：恢复可信的仓库门禁与治理入口"
ready: true
risk: medium
blocked_by: []
contract_ids: [AC-001]
owner: single-agent
expected_changes: ["<Path>.agents/skills/engineering-standards/scripts/validate-skill-facts.mjs</Path>", "<Path>.agents/skills/engineering-standards/scripts/validate-skill-facts.test.mjs</Path>", "<Path>.agents/skills/namewta-fullstack-development/scripts/validate-module-mode.mjs</Path>", "<Path>.agents/skills/namewta-fullstack-development/scripts/validate-module-mode.test.mjs</Path>", "<Path>scripts/ci/verify-submodules.sh</Path>", "<Path>scripts/ci/verify-dev-build-guard.sh</Path>", "<Path>scripts/README.md</Path>", "<Path>.github/workflows/quality-gates.yml</Path>", "<Path>{roots.state}/specdev/config.json</Path>", "<Path>release-artifacts/tests/release-integration-contract.test.mjs</Path>", "<Path>release-artifacts/tests/wta-rename-keep-contract.test.mjs</Path>", "<Path>.agents/skills/engineering-standards/references/project/00-project-profile.md</Path>", "<Path>.agents/skills/engineering-standards/references/project/01-module-map.md</Path>", "<Path>.agents/skills/engineering-standards/references/project/03-backend-module-modes.md</Path>", "<Path>release-artifacts/scripts/verify-release.sh</Path>", "<Path>.agents/skills/engineering-standards/references/rules/architecture-and-boundaries.md</Path>", "<Path>speculo/workflows/specdev/common/tools/validate-specdev.mjs</Path>", "<Path>speculo/workflows/specdev/common/tools/validate-specdev.test.mjs</Path>", "<Path>backend/wta-modules/wta-notify/src/test/java/org/namewta/notify/NotifyOutboxWakeScopeGateTest.java</Path>"]
writable_paths: ["<Path>.agents/skills/engineering-standards/scripts/validate-skill-facts.mjs</Path>", "<Path>.agents/skills/engineering-standards/scripts/validate-skill-facts.test.mjs</Path>", "<Path>.agents/skills/namewta-fullstack-development/scripts/validate-module-mode.mjs</Path>", "<Path>.agents/skills/namewta-fullstack-development/scripts/validate-module-mode.test.mjs</Path>", "<Path>scripts/ci/verify-submodules.sh</Path>", "<Path>scripts/ci/verify-dev-build-guard.sh</Path>", "<Path>scripts/README.md</Path>", "<Path>.github/workflows/quality-gates.yml</Path>", "<Path>{roots.state}/specdev/config.json</Path>", "<Path>release-artifacts/tests/release-integration-contract.test.mjs</Path>", "<Path>release-artifacts/tests/wta-rename-keep-contract.test.mjs</Path>", "<Path>.agents/skills/engineering-standards/references/project/00-project-profile.md</Path>", "<Path>.agents/skills/engineering-standards/references/project/01-module-map.md</Path>", "<Path>.agents/skills/engineering-standards/references/project/03-backend-module-modes.md</Path>", "<Path>release-artifacts/scripts/verify-release.sh</Path>", "<Path>.agents/skills/engineering-standards/references/rules/architecture-and-boundaries.md</Path>", "<Path>speculo/workflows/specdev/common/tools/validate-specdev.mjs</Path>", "<Path>speculo/workflows/specdev/common/tools/validate-specdev.test.mjs</Path>", "<Path>backend/wta-modules/wta-notify/src/test/java/org/namewta/notify/NotifyOutboxWakeScopeGateTest.java</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/</Path>", "<Path>{roots.state}/specdev/adr/</Path>"]
shared_paths: ["<Path>scripts/ci/verify-submodules.sh</Path>", "<Path>scripts/ci/verify-dev-build-guard.sh</Path>", "<Path>scripts/README.md</Path>", "<Path>release-artifacts/tests/release-integration-contract.test.mjs</Path>", "<Path>release-artifacts/tests/wta-rename-keep-contract.test.mjs</Path>", "<Path>backend/wta-modules/wta-notify/src/test/java/org/namewta/notify/NotifyOutboxWakeScopeGateTest.java</Path>"]
shared_path_owners: ["<Path>scripts/ci/verify-submodules.sh</Path> => single-agent (Lead; serial T-01 turn)", "<Path>scripts/ci/verify-dev-build-guard.sh</Path> => single-agent (Lead; serial T-01 turn)", "<Path>scripts/README.md</Path> => single-agent (Lead; serial T-01 turn)", "<Path>release-artifacts/tests/release-integration-contract.test.mjs</Path> => single-agent (Lead; serial T-01 turn)", "<Path>release-artifacts/tests/wta-rename-keep-contract.test.mjs</Path> => single-agent (Lead; serial T-01 turn)", "<Path>backend/wta-modules/wta-notify/src/test/java/org/namewta/notify/NotifyOutboxWakeScopeGateTest.java</Path> => single-agent (Lead; serial T-01 then T-28 turns)"]
---

# T-01：恢复可信的仓库门禁与治理入口

Map：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>；Spec：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/spec.md</Path>；Goal：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/goal-plan.md</Path>。
唯一执行者先读Map，再按Skill矩阵读取适用入口/引用，再读本票。禁止任何implementation/review/research子代理。Ready仅表示计划合同就绪，不表示已经授权实施或已验证通过。

## 1. 战略与来源

- 目标与可观察产出：干净clone不创建temp/release也通过事实检查。
- 来源：D-01, D-07, D-11, B-09；AC-001；USER-DECISION: 全面完善计划、无兼容、单人串行。
- 当前事实与调用链：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/re-review.md</Path>中T-01行及对应专项报告；源码导航为frontmatter预计修改点。
- 规划深度：standard；沿用现有模块的多文件行为修复：恢复可信的仓库门禁与治理入口。

## 2. 决策状态

### 已锁定决策

ADR-CR-006：恢复仓内GitHub Actions候选配置与本地门禁合同；启用远程Actions/required checks不属于本票自动授权。

### 已采用的低影响假设

沿用当前仓库版本与既有模块命名；实施前回读实际源码，路径变化由本票修订，不猜测不存在的实现。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 仓库本地检查入口→真实目录/namespace检查及候选CI | 既有node测试和release脚本；不删除供应商Skill，不修改远程required checks | 本票之外的模块重写、兼容桥、远程发布与重要数据操作 |

## 4. 要构建什么

仓库本地检查入口→真实目录/namespace检查及候选CI。调用者可观察到：干净clone不创建temp/release也通过事实检查。失败时：注释和供应商坐标通过；非法跨层import与缺产物必须失败。

## 5. 实现契约

- 入口、输入输出与数据流：仓库本地检查入口→真实目录/namespace检查及候选CI。
- 不变量及失败语义：注释和供应商坐标通过；非法跨层import与缺产物必须失败。
- 公共合同：本票只按以上行为及执行路线变更；同步全部仓内调用/生成物，沿用已有权限/Client/owner校验。未列出的接口保持原语义。
- 兼容：用户明确无需旧版兼容；仓内一次切换，不加双路由/版本等待。实际供应商协议仍须遵守。
- 安全与隐私：凭据不进入日志/UI证据；越权/过期/无owner拒绝；数据库与资源约束不能为前端成功而放宽。

保留分层硬约束：修复Javadoc误报与org.namewta漏检；SSO Cookie渲染归HTTP adapter（T-06），Notify wake归event/worker adapter（T-28），两项真实结构命中不能作为误报放行。新增检查器夹具是计划文件，创建后登记精确node --test命令。全部5个layered模块必须复核。现有9条命令结果见 reviews/re-review-command-results.json。 新增validator测试文件及可选恢复CI文件当前不存在，均为计划路径；不为迎合dev-build-guard而创建空.vscode/settings.json，保留真正构建锁/JAR检查并解除编辑器文件前置。

## 6. 执行路线

1. 记录真实命令/cwd/工具版本/失败分类；先保存现有红灯基线，不把检查器误报当产品缺陷。
2. 删除无submodule仍强求快照的死检查与调用映射；明确重建CI为本地候选配置。
3. 模块分层检查以org.namewta自有namespace和词法/AST角色判断，排除Javadoc伪注解，保留第三方org.dromara依赖。
4. 用最小正负夹具覆盖非法Mapper import、错误层事务、合法注释及有意供应商坐标；不通过扩大allowlist变绿。
5. 去掉忽略目录temp/release必须存在的条件，检查ignore规则与输出owner即可。
6. 按真实monorepo cwd更新SpecDev verification，并将外部服务、类型、OpenAPI/架构检查分别登记。裁决 scripts/ci/verify-dev-build-guard.sh 对缺失 .vscode/settings.json 的前置条件，不能删除构建锁或JAR完整性断言。
7. 发布合同本次为43项、41通过、2失败：缺失CI与speculo/skills/upstream-fork-sync存在断言。核对后者的Speculo所有权并修正产品测试的范围，不删除供应商Skill来迎合断言。

## 7. 路径访问契约

预计点、可写范围、只读上下文及共享项以frontmatter为唯一权威。共享owner固定single-agent；只有当前票轮次可写，下一票须回读前一结果。跨模块目录只允许本票行为必需的文件，目录授权不意味着重写全部模块。新增测试位于同模块测试目录；未覆盖的新路径先修订本票/Map再写。
保留当前用户未提交改动、永久ADR/context、供应商源码与运行数据；生成物通过正式工具更新。

## 8. 验证矩阵

| 行为或风险 | 验证接缝/步骤 | 预期结果 | Evidence |
|---|---|---|---|
| 正常路径 | 仓库本地检查入口→真实目录/namespace检查及候选CI；执行下列定向命令及对应场景 | 干净clone不创建temp/release也通过事实检查 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-01.md</Path> |
| 失败路径 | 注释和供应商坐标通过；非法跨层import与缺产物必须失败；固定时序/故障注入，记录输入与最终可观察状态 | 无越权、错误状态或部分提交；可按定义恢复 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-01.md</Path> |
| 回归 | 运行所属包原有测试及受影响调用者；逐项核对下列AC | 注释中DSTransactional不触发越层误报，真实非法import必须失败；不存在CI文件时文档不得宣称active；远程required状态有独立证据；每个声明命令可在对应cwd解析，Maven/前端测试与package分开记录 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-01.md</Path> |

Workspace checks：current-workspace；`frontend:`/`backend:`表示先进入该目录，其他命令cwd为仓根，逐条串行执行。新增用例实施时登记精确选择器、实际测试数与跳过项，零测试/required跳过不算通过。

- `node .agents/skills/engineering-standards/scripts/validate-skill-facts.mjs`
- `node .agents/skills/namewta-fullstack-development/scripts/validate-module-mode.mjs backend/wta-modules/wta-sso --mode layered`

- E2E disposition：not-required: 仓库静态/脚本合同由正负夹具及本地workflow检查覆盖，无在线业务边界。
- E2E owner/environment：single-agent（Lead）/current-workspace；使用隔离MySQL/Redis/OSS及必要真实HTTP/浏览器，禁止连生产。场景步骤以上表、本票AC为准；需新用例时在写集内创建后记录精确命令。
- Integration evidence：记录parent before、implementation commit及direct-parent检查；result SHA等于通过验证的implementation commit，candidate不适用。当前全部产品检查not-run。

## 9. 发布、迁移与恢复

- 顺序：前置票产生已验证合同后实施本票；源码、仓内消费者、测试和生成物同批交付。涉及DDL只编辑10-cde-base-ddl.sql，新环境按六文件基座初始化；不增加存量迁移工程。
- 兼容窗口：无；不保留旧接口或数据格式桥。生产部署不是本票自动步骤。
- 监控/诊断：观察本票AC的成功/错误状态、耗时及资源/持久化结果，日志只含安全元数据；复用现有观测入口，不新建监控平台。
- 恢复：撤回本票检查器/CI候选diff；保留修复前失败基线。
- 不可逆批准点：提交、推送、部署、运行数据删除/修复分别需授权；最新Goal已授权实现及本地验证，commit授权待具体结果审阅。
- 收缩条件：本票替代的旧调用/配置引用归零且仓内回归通过；无被替代入口时不适用，不为凑清单扩大删除范围。

## 10. 验收标准

- [x] `AC-001`：干净clone不创建temp/release也通过事实检查。
- [x] `AC-001`：注释中DSTransactional不触发越层误报，真实非法import必须失败。
- [x] `AC-001`：不存在CI文件时文档不得宣称active；远程required状态有独立证据。
- [x] `AC-001`：每个声明命令可在对应cwd解析，Maven/前端测试与package分开记录。
- [x] 按Map→适用Skill→本票完成读取及实际调用；所有required Skill记录passed并可回读。
- [x] 正常/失败/回归及required E2E均完成，证据写入<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-01.md</Path>，未执行不得标通过。
- [x] 修改不超出写集，共享项只有single-agent当前票轮次写入。
- [x] 获得授权后形成非空implementation commit，Lead完成direct-parent验收并记录parent result SHA；未获授权不提交、不标Done。
- [x] Ticket、Map、Goal与Evidence一致；不存在未批准偏差。

## 11. SKILL 调用计划

frontmatter绑定的项目Skill在implementation阶段接收本票路径和上游合同，产出适用分层、权限、数据/资源边界及实现diff；engineering-standards在verify阶段根据本节命令选择受影响门禁并输出AC/退出码/E2E记录。按入口scope展开引用，不以“已读”代替实际操作。
必需入口缺失或sha256漂移时阻塞本票；Lead回读差异后更新绑定及Map，不能自动接受新摘要。实际记录归<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-01.md</Path>的`## Skill Execution Records`，本次规划不伪造实现调用记录。

## 12. 停止、检查点与交付

交付本票完整可观察行为及验收证据；数量以Map为准。缺依赖/测试环境/Skill、越界或高影响事实变化时停止受影响票，保留checkpoint和失败证据，其他独立票仍可串行推进。恢复先读Goal、Map、本票、状态及最新Evidence；记录实际HEAD/dirty差异，禁止覆盖用户修改。
依赖：无。单票完成条件为全部AC、实际Skill证据和获授权的direct-parent出口；仅补文档不能标Done。

## 2026-09-18 执行准备修订

最新用户请求授权本 change 全部实现与本地验证；先完成 T-01 门禁准备。工作树基线为 `76dbbe84a34624234e379661a57b232529e34ed3`，无既有脏改动。工程规范要求事实同步，因此补充 Project Profile/Module Map/模式登记表写集；发布测试以 `--test-concurrency=1` 串行运行。T-03/T-23 未决不以 T-01 的局部通过消除，G-plan 和各票提交出口仍保持未完成。

### T-01 门禁准备补充

当前源码证明ARCH-001仍要求Git submodule，与100644跟踪的backend/pom.xml、frontend/package.json及无.gitmodules冲突，按事实优先修正规则。P校验器把stage=goal-plan等同于多change，导致单change无法校验；同时缺失单change goal-plan.md未被明确拒绝。扩充以上三份写集，修复模式路由并用单/多change正负夹具证明缺件、无效父计划和缺失Goal仍失败。不改变schema、agent上限、Ready/授权或完成条件；这是对既有工具缺陷的局部修复，不是为本change放宽校验。供应商其余文件与永久知识保持只读。

T-02完整Maven发现追加门禁缺陷：旧NotifyOutboxWakeScopeGateTest绑定不存在的历史commit和后端独立Git根。按归档2026-09-11原Spec AC-006及ADR-0006/0007保留长期合同，改为当前Outbox组件不耦合配置控制面/渠道直投，以及common Dispatcher同步行为；增加违规依赖夹具。原change写集审查由归档证据拥有，不用永远变化的全仓diff模拟永久合同。此文件已纳入本票写集，业务分层与运行时修复仍由T-28/T-22负责。

## Revision134 最终本地验收补记

原SSO/Notify分层、P-validator和T-03/T-23未知均已由责任票闭合并在T-30复验。远程CI/required checks继续明确未运行，仓内CI只作候选；不能把候选配置等同远程启用。 实际证据：T-30-final-quality.json, T-30-v2-release.json, T-30-final-governance.json；源码路径及hash见T-30-completion-audit-revision134.json。实施提交/direct-parent/result继续未勾选。

## Revision135 实际提交与父分支验收

用户已明确授权全部commit/push。implementation commits：`3aa047b85033afd7588923ffa7b37481dc2d1645`；完整实现链 result SHA：`6c8764cca97bb6057fcb90ccdfe635c7efbf502a`。每个提交均非空、实际父SHA已核对且被result包含；Git归档逐文件等于T-30已验证输入，未声称拆分过程中的中间树独立通过全部测试。精确路径/共享owner/验证见 `../evidence/commit-delivery.json`。本票保持review；正式发布候选与change最终Done独立验收。
