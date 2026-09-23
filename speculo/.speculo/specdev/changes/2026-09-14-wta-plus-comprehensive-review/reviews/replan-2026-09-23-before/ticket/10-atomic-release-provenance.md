---
schema_version: 3
plan_contract_version: 1
skill_scan: "2026-09-18枚举.agents/skills入口并按本票真实路径/领域绑定；Map为最低集合"
skill_bindings: [{"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/10-atomic-release-provenance.md</Path>", "<Path>release-artifacts/scripts/release-manage.sh</Path>", "<Path>release-artifacts/scripts/verify-release.sh</Path>"], "outputs": ["T-10的架构/权限/数据边界检查与定向实现diff"], "required": true, "on_failure": "block-ticket"}, {"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "verify", "operation": "verify-affected-contract-and-quality-gates", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/10-atomic-release-provenance.md</Path>", "current-workspace实际diff及本票验证矩阵"], "outputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-10.md</Path>：命令、退出码、测试数、AC与Skill Execution Records"], "required": true, "on_failure": "block-ticket"}]
resource_claims: ["workspace:current-exclusive", "finding:D-03", "finding:D-10", "contract:AC-010"]
artifact: ticket
change: 2026-09-14-wta-plus-comprehensive-review
id: T-10
title: 删除混源局部发布并实现原子stage
status: "review"
planning_depth: "deep"
planning_depth_reason: "安全/鉴权、公共合同、数据一致性或共享核心路径变更：删除混源局部发布并实现原子stage"
ready: true
risk: high
blocked_by: ["T-09"]
contract_ids: [AC-010]
owner: single-agent
expected_changes: ["<Path>release-artifacts/scripts/release-manage.sh</Path>", "<Path>release-artifacts/scripts/verify-release.sh</Path>", "<Path>release-artifacts/tests/</Path>", "<Path>release-artifacts/README.md</Path>", "<Path>release-artifacts/docker/docker-compose-backend.yml</Path>", "<Path>release-artifacts/docker/docker-compose-frontend.yml</Path>", "<Path>release-artifacts/scripts/docker-manage.sh</Path>", "<Path>release-artifacts/scripts/release-state.py</Path>", "<Path>release-artifacts/skills/wta-namewta-nginx-config/SKILL.md</Path>", "<Path>release-artifacts/skills/wta-namewta-nginx-config/scripts/add_app.py</Path>", "<Path>.agents/skills/deploy-namewta-environment/references/build-transfer-release.md</Path>", "<Path>.agents/skills/engineering-standards/references/project/00-project-profile.md</Path>", "<Path>scripts/ci/verify-admin-bundle.sh</Path>"]
writable_paths: ["<Path>release-artifacts/scripts/release-manage.sh</Path>", "<Path>release-artifacts/scripts/verify-release.sh</Path>", "<Path>release-artifacts/tests/</Path>", "<Path>release-artifacts/README.md</Path>", "<Path>release-artifacts/docker/docker-compose-backend.yml</Path>", "<Path>release-artifacts/docker/docker-compose-frontend.yml</Path>", "<Path>release-artifacts/scripts/docker-manage.sh</Path>", "<Path>release-artifacts/scripts/release-state.py</Path>", "<Path>release-artifacts/skills/wta-namewta-nginx-config/SKILL.md</Path>", "<Path>release-artifacts/skills/wta-namewta-nginx-config/scripts/add_app.py</Path>", "<Path>.agents/skills/deploy-namewta-environment/references/build-transfer-release.md</Path>", "<Path>.agents/skills/engineering-standards/references/project/00-project-profile.md</Path>", "<Path>scripts/ci/verify-admin-bundle.sh</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/</Path>", "<Path>{roots.state}/specdev/adr/</Path>"]
shared_paths: ["<Path>release-artifacts/scripts/release-manage.sh</Path>", "<Path>release-artifacts/tests/</Path>", "<Path>release-artifacts/README.md</Path>", "<Path>release-artifacts/docker/docker-compose-frontend.yml</Path>", "<Path>release-artifacts/skills/wta-namewta-nginx-config/scripts/add_app.py</Path>", "<Path>.agents/skills/engineering-standards/references/project/00-project-profile.md</Path>", "<Path>scripts/ci/verify-admin-bundle.sh</Path>"]
shared_path_owners: ["<Path>release-artifacts/scripts/release-manage.sh</Path> => single-agent (Lead; serial T-10 turn)", "<Path>release-artifacts/tests/</Path> => single-agent (Lead; serial T-10 turn)", "<Path>release-artifacts/README.md</Path> => single-agent (Lead; serial T-10 turn)", "<Path>release-artifacts/docker/docker-compose-frontend.yml</Path> => single-agent (Lead; serial T-10 turn)", "<Path>release-artifacts/skills/wta-namewta-nginx-config/scripts/add_app.py</Path> => single-agent (Lead; serial T-10 turn)", "<Path>.agents/skills/engineering-standards/references/project/00-project-profile.md</Path> => single-agent (Lead; serial T-10 turn)", "<Path>scripts/ci/verify-admin-bundle.sh</Path> => single-agent (Lead; serial T-10 turn)"]
---

# T-10：删除混源局部发布并实现原子stage

Map：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>；Spec：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/spec.md</Path>；Goal：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/goal-plan.md</Path>。
唯一执行者先读Map，再按Skill矩阵读取适用入口/引用，再读本票。禁止任何implementation/review/research子代理。Ready仅表示计划合同就绪，不表示已经授权实施或已验证通过。

## 1. 战略与来源

- 目标与可观察产出：manifest每个artifact的digest和source可追溯。
- 来源：D-03, D-10；AC-010；USER-DECISION: 全面完善计划、无兼容、单人串行。
- 当前事实与调用链：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/re-review.md</Path>中T-10行及对应专项报告；源码导航为frontmatter预计修改点。
- 规划深度：deep；安全/鉴权、公共合同、数据一致性或共享核心路径变更：删除混源局部发布并实现原子stage。

## 2. 决策状态

### 已锁定决策

ADR-CR-005：取消partial deployable promotion，只允许同一干净源码完整产物切换指针。

### 已采用的低影响假设

沿用当前仓库版本与既有模块命名；实施前回读实际源码，路径变化由本票修订，不猜测不存在的实现。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 干净单源码完整构建→不可变版本→校验→单指针promotion | 已有SHA-256 manifest和Linux/Bash；不承诺多容器原子热更新 | 本票之外的模块重写、兼容桥、远程发布与重要数据操作 |

## 4. 要构建什么

干净单源码完整构建→不可变版本→校验→单指针promotion。调用者可观察到：manifest每个artifact的digest和source可追溯。失败时：缺件/混源/中断不改current；读取方固定已解析版本；partial不可晋升。

## 5. 实现契约

- 入口、输入输出与数据流：干净单源码完整构建→不可变版本→校验→单指针promotion。
- 不变量及失败语义：缺件/混源/中断不改current；读取方固定已解析版本；partial不可晋升。
- 公共合同：本票只按以上行为及执行路线变更；同步全部仓内调用/生成物，沿用已有权限/Client/owner校验。未列出的接口保持原语义。
- 兼容：用户明确无需旧版兼容；仓内一次切换，不加双路由/版本等待。实际供应商协议仍须遵守。
- 安全与隐私：凭据不进入日志/UI证据；越权/过期/无owner拒绝；数据库与资源约束不能为前端成功而放宽。

release-manage.sh当前stage依次写后端、清空前端、最后校验SQL，失败会留下部分新资产。修改release-artifacts/tests中的合同测试；不以逐目录rename冒充整套原子切换，不要求运行容器通过symlink自动更新。

## 6. 执行路线

1. 固定partial复制旧current再覆盖一半的混源场景；当前manifest已有每文件SHA-256，问题是整体HEAD/bundle无法证明继承产物来源。
2. 可发布产物只来自同一干净源码快照的完整构建；单目标构建留在开发输出，不进入deployable current。monorepo使用一个source revision。
3. 完整发布目录包含JAR、各App、SQL快照、Nginx/Compose配置及manifest；受管SQL源仍只有六份基座，产物副本不可成为新编辑源。
4. 先构造不可变版本目录并验证完整性，再切换一个发布指针；消费端固定解析出的版本路径。发布操作串行，失败只清理本次临时目录。
5. 原子性限定为发布产物可见性，不宣称多容器运行时原子升级。已运行Docker bind mount不会自动跟随新指针；部署阶段显式重建对应服务。
6. 针对实际Linux/Bash发布入口测试缺件、混源、stage失败与恢复。没有Windows直接部署消费者，不增加跨平台热切换层。

## 7. 路径访问契约

预计点、可写范围、只读上下文及共享项以frontmatter为唯一权威。共享owner固定single-agent；只有当前票轮次可写，下一票须回读前一结果。跨模块目录只允许本票行为必需的文件，目录授权不意味着重写全部模块。新增测试位于同模块测试目录；未覆盖的新路径先修订本票/Map再写。
保留当前用户未提交改动、永久ADR/context、供应商源码与运行数据；生成物通过正式工具更新。

## 8. 验证矩阵

| 行为或风险 | 验证接缝/步骤 | 预期结果 | Evidence |
|---|---|---|---|
| 正常路径 | 干净单源码完整构建→不可变版本→校验→单指针promotion；执行下列定向命令及对应场景 | manifest每个artifact的digest和source可追溯 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-10.md</Path> |
| 失败路径 | 缺件/混源/中断不改current；读取方固定已解析版本；partial不可晋升；固定时序/故障注入，记录输入与最终可观察状态 | 无越权、错误状态或部分提交；可按定义恢复 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-10.md</Path> |
| 回归 | 运行所属包原有测试及受影响调用者；逐项核对下列AC | 缺模板/坏SQL/坏JAR/中断时current/context SHA256完全不变；单目标构建不能stage为完整release；恢复只操作本次stage，不触及他人文件或历史发布 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-10.md</Path> |

Workspace checks：current-workspace；`frontend:`/`backend:`表示先进入该目录，其他命令cwd为仓根，逐条串行执行。新增用例实施时登记精确选择器、实际测试数与跳过项，零测试/required跳过不算通过。

- `bash release-artifacts/scripts/verify-release.sh`

- E2E disposition：required: 隔离文件系统完整stage、缺件/中断/混源注入、固定版本消费者与容器重建恢复。
- E2E owner/environment：single-agent（Lead）/current-workspace；使用隔离MySQL/Redis/OSS及必要真实HTTP/浏览器，禁止连生产。场景步骤以上表、本票AC为准；需新用例时在写集内创建后记录精确命令。
- Integration evidence：记录parent before、implementation commit及direct-parent检查；result SHA等于通过验证的implementation commit，candidate不适用。当前全部产品检查not-run。

## 9. 发布、迁移与恢复

- 顺序：前置票产生已验证合同后实施本票；源码、仓内消费者、测试和生成物同批交付。涉及DDL只编辑10-cde-base-ddl.sql，新环境按六文件基座初始化；不增加存量迁移工程。
- 兼容窗口：无；不保留旧接口或数据格式桥。生产部署不是本票自动步骤。
- 监控/诊断：观察本票AC的成功/错误状态、耗时及资源/持久化结果，日志只含安全元数据；复用现有观测入口，不新建监控平台。
- 恢复：指针切回已验证完整版本并重建对应容器；仅清本次临时目录。
- 不可逆批准点：提交、推送、部署、运行数据删除/修复分别需授权；本地实现与隔离验证已授权；本change全部提交由用户暂缓，不执行真实发布或部署。
- 收缩条件：本票替代的旧调用/配置引用归零且仓内回归通过；无被替代入口时不适用，不为凑清单扩大删除范围。

## 10. 验收标准

- [x] `AC-010`：manifest每个artifact的digest和source可追溯。
- [x] `AC-010`：缺模板/坏SQL/坏JAR/中断时current/context SHA256完全不变。
- [x] `AC-010`：单目标构建不能stage为完整release。
- [x] `AC-010`：恢复只操作本次stage，不触及他人文件或历史发布。
- [x] 按Map→适用Skill→本票完成读取及实际调用；所有required Skill记录passed并可回读。
- [x] 正常/失败/回归及required E2E均完成，证据写入<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-10.md</Path>，未执行不得标通过。
- [x] 修改不超出写集，共享项只有single-agent当前票轮次写入。
- [x] 获得授权后形成非空implementation commit，Lead完成direct-parent验收并记录parent result SHA；未获授权不提交、不标Done。
- [x] Ticket、Map、Goal与Evidence一致；不存在未批准偏差。

## 11. SKILL 调用计划

frontmatter绑定的项目Skill在implementation阶段接收本票路径和上游合同，产出适用分层、权限、数据/资源边界及实现diff；engineering-standards在verify阶段根据本节命令选择受影响门禁并输出AC/退出码/E2E记录。按入口scope展开引用，不以“已读”代替实际操作。
必需入口缺失或sha256漂移时阻塞本票；Lead回读差异后更新绑定及Map，不能自动接受新摘要。实际记录归<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-10.md</Path>的`## Skill Execution Records`，本次规划不伪造实现调用记录。

## 12. 停止、检查点与交付

交付本票完整可观察行为及验收证据；数量以Map为准。缺依赖/测试环境/Skill、越界或高影响事实变化时停止受影响票，保留checkpoint和失败证据，其他独立票仍可串行推进。恢复先读Goal、Map、本票、状态及最新Evidence；记录实际HEAD/dirty差异，禁止覆盖用户修改。
依赖：T-09。单票完成条件为全部AC、实际Skill证据和获授权的direct-parent出口；仅补文档不能标Done。

### 实施入口

T-09十八路径检查点已逐一回读hash；默认Maven、双bundle与三App模式证据可复核。当前工作树未提交，真实deployable构建必须保留干净源码要求，不能把脏树标成干净；本票可逆实现/失败注入在一次性测试夹具中验证，不晋升现有发布指针或重建已有环境。

### T-10实现细化（revision 31）

增加标准库Python发布状态辅助脚本，Shell保留正式入口。完整构建先拒绝脏源码，再将单一Git revision归档到本次临时构建目录（不创建Git worktree）；只从该快照构建并记录每文件sourceRevision/digest。build只生成不可变版本，stage必须显式指定版本ID并只原子替换current符号链接。Docker入口单次解析并校验该版本，将持久数据/证书/日志留在独立运行目录；up显式build/recreate。局部构建仅写development目录，不能stage。正式工作树不提交、不构建deployable候选、不部署；夹具中的合成Git历史仅为测试输入。

Revision 32：同步实际调用方的构建/stage说明及工程画像，修正add_app生成的日志挂载到独立运行目录；不执行部署Skill操作。SQL初始化工具仍接受显式sql-dir/schema-file，部署文档从一次解析后的固定版本传入；源基座stage-mysql保持只读。

Revision 33：普通用户隔离验证发现verify-admin-bundle即使传入ADMIN_ARTIFACT仍无条件查询调用目录Git；接管该检查器的一处前置条件修复，明确artifact时不读Git。只调整入口定位，不放宽8+4 bundle内容断言，不设置safe.directory全局例外。

### 本地审查检查点

T-10.md/T-10-checkpoint.json记录15路径、100项发布合同、16项普通用户测试、5个真实Nginx HTTP观察及五份真实JAR验证。AC为本地实现验证；implementation/result仍空，不提交、不Done。指针原子边界及stub/真实验证区分见Evidence，T-08/T-30真实完整发布责任未提前关闭。

## Revision135 实际提交与父分支验收

用户已明确授权全部commit/push。implementation commits：`e730305a50d9065fdc8af454770b3267983ae982`；完整实现链 result SHA：`6c8764cca97bb6057fcb90ccdfe635c7efbf502a`。每个提交均非空、实际父SHA已核对且被result包含；Git归档逐文件等于T-30已验证输入，未声称拆分过程中的中间树独立通过全部测试。精确路径/共享owner/验证见 `../evidence/commit-delivery.json`。本票保持review；正式发布候选与change最终Done独立验收。
