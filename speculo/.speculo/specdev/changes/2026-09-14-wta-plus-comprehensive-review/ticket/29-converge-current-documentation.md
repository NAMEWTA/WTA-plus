---
schema_version: 3
plan_contract_version: 1
skill_scan: "2026-09-18枚举.agents/skills入口并按本票真实路径/领域绑定；Map为最低集合"
skill_bindings: [{"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/29-converge-current-documentation.md</Path>", "<Path>backend/AGENTS.md</Path>", "<Path>backend/wta-admin/AGENTS.md</Path>"], "outputs": ["T-29的架构/权限/数据边界检查与定向实现diff"], "required": true, "on_failure": "block-ticket"}, {"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "verify", "operation": "verify-affected-contract-and-quality-gates", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/29-converge-current-documentation.md</Path>", "current-workspace实际diff及本票验证矩阵"], "outputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-29.md</Path>：命令、退出码、测试数、AC与Skill Execution Records"], "required": true, "on_failure": "block-ticket"}]
resource_claims: ["workspace:current-exclusive", "finding:D-05", "finding:D-06", "finding:B-17", "contract:AC-029"]
artifact: ticket
change: 2026-09-14-wta-plus-comprehensive-review
id: T-29
title: 清理过时文档与重复AGENTS权威
status: "ready"
planning_depth: "deep"
planning_depth_reason: "安全/鉴权、公共合同、数据一致性或共享核心路径变更：清理过时文档与重复AGENTS权威"
ready: true
risk: medium
blocked_by: ["T-01"]
contract_ids: [AC-029]
owner: single-agent
expected_changes: ["<Path>backend/AGENTS.md</Path>", "<Path>backend/wta-admin/AGENTS.md</Path>", "<Path>backend/wta-api/AGENTS.md</Path>", "<Path>backend/wta-common/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-ai/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-bom/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-core/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-doc/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-elasticsearch/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-encrypt/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-excel/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-job/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-json/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-liteflow/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-log/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-mail/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-mcp/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-mqtt/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-mybatis/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-nacos/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-notify/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-openapi/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-oss/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-push/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-redis/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-satoken/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-security/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-sensitive/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-sms/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-social/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-translation/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-web/AGENTS.md</Path>", "<Path>backend/wta-extend/AGENTS.md</Path>", "<Path>backend/wta-extend/wta-monitor-admin/AGENTS.md</Path>", "<Path>backend/wta-extend/wta-snailai-server/AGENTS.md</Path>", "<Path>backend/wta-extend/wta-snailjob-server/AGENTS.md</Path>", "<Path>backend/wta-modules/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-ai/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-job/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-profile/wta-profile-bom/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-workflow/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-demo/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-notify/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-profile/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-profile/wta-profile-enterprise/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-profile/wta-profile-person/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-sso/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-system/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-third/AGENTS.md</Path>", "<Path>scripts/README.md</Path>", "<Path>.agents/skills/engineering-standards/references/project/00-project-profile.md</Path>", "<Path>.agents/skills/engineering-standards/references/project/01-module-map.md</Path>", "<Path>.agents/skills/engineering-standards/references/project/02-decisions-and-exceptions.md</Path>", "<Path>README.md</Path>", "<Path>frontend/README.md</Path>", "<Path>frontend/AGENTS.md</Path>", "<Path>frontend/apps/README.md</Path>", "<Path>frontend/docs/architecture-baseline.md</Path>", "<Path>backend/README.md</Path>", "<Path>docs/README.md</Path>", "<Path>docs/namewta-enhancements.md</Path>", "<Path>docs/runtime-nacos-hard-cut.md</Path>", "<Path>docs/oss-public-private-operations.md</Path>", "<Path>release-artifacts/README.md</Path>", "<Path>.agents/skills/engineering-standards/references/project/03-backend-module-modes.md</Path>"]
writable_paths: ["<Path>backend/AGENTS.md</Path>", "<Path>backend/wta-admin/AGENTS.md</Path>", "<Path>backend/wta-api/AGENTS.md</Path>", "<Path>backend/wta-common/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-ai/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-bom/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-core/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-doc/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-elasticsearch/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-encrypt/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-excel/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-job/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-json/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-liteflow/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-log/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-mail/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-mcp/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-mqtt/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-mybatis/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-nacos/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-notify/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-openapi/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-oss/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-push/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-redis/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-satoken/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-security/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-sensitive/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-sms/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-social/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-translation/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-web/AGENTS.md</Path>", "<Path>backend/wta-extend/AGENTS.md</Path>", "<Path>backend/wta-extend/wta-monitor-admin/AGENTS.md</Path>", "<Path>backend/wta-extend/wta-snailai-server/AGENTS.md</Path>", "<Path>backend/wta-extend/wta-snailjob-server/AGENTS.md</Path>", "<Path>backend/wta-modules/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-ai/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-job/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-profile/wta-profile-bom/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-workflow/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-demo/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-notify/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-profile/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-profile/wta-profile-enterprise/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-profile/wta-profile-person/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-sso/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-system/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-third/AGENTS.md</Path>", "<Path>scripts/README.md</Path>", "<Path>.agents/skills/engineering-standards/references/project/00-project-profile.md</Path>", "<Path>.agents/skills/engineering-standards/references/project/01-module-map.md</Path>", "<Path>.agents/skills/engineering-standards/references/project/02-decisions-and-exceptions.md</Path>", "<Path>README.md</Path>", "<Path>frontend/README.md</Path>", "<Path>frontend/AGENTS.md</Path>", "<Path>frontend/apps/README.md</Path>", "<Path>frontend/docs/architecture-baseline.md</Path>", "<Path>backend/README.md</Path>", "<Path>docs/README.md</Path>", "<Path>docs/namewta-enhancements.md</Path>", "<Path>docs/runtime-nacos-hard-cut.md</Path>", "<Path>docs/oss-public-private-operations.md</Path>", "<Path>release-artifacts/README.md</Path>", "<Path>.agents/skills/engineering-standards/references/project/03-backend-module-modes.md</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/</Path>", "<Path>{roots.state}/specdev/adr/</Path>"]
shared_paths: ["<Path>backend/wta-api/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-encrypt/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-log/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-openapi/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-redis/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-security/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-web/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-profile/wta-profile-bom/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-workflow/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-demo/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-notify/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-profile/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-profile/wta-profile-enterprise/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-profile/wta-profile-person/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-sso/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-system/AGENTS.md</Path>", "<Path>scripts/README.md</Path>", "<Path>.agents/skills/engineering-standards/references/project/02-decisions-and-exceptions.md</Path>", "<Path>release-artifacts/README.md</Path>"]
shared_path_owners: ["<Path>backend/wta-api/AGENTS.md</Path> => single-agent (Lead; serial T-29 turn)", "<Path>backend/wta-common/wta-common-encrypt/AGENTS.md</Path> => single-agent (Lead; serial T-29 turn)", "<Path>backend/wta-common/wta-common-log/AGENTS.md</Path> => single-agent (Lead; serial T-29 turn)", "<Path>backend/wta-common/wta-common-openapi/AGENTS.md</Path> => single-agent (Lead; serial T-29 turn)", "<Path>backend/wta-common/wta-common-redis/AGENTS.md</Path> => single-agent (Lead; serial T-29 turn)", "<Path>backend/wta-common/wta-common-security/AGENTS.md</Path> => single-agent (Lead; serial T-29 turn)", "<Path>backend/wta-common/wta-common-web/AGENTS.md</Path> => single-agent (Lead; serial T-29 turn)", "<Path>backend/wta-modules/wta-profile/wta-profile-bom/AGENTS.md</Path> => single-agent (Lead; serial T-29 turn)", "<Path>backend/wta-modules/wta-workflow/AGENTS.md</Path> => single-agent (Lead; serial T-29 turn)", "<Path>backend/wta-modules/wta-demo/AGENTS.md</Path> => single-agent (Lead; serial T-29 turn)", "<Path>backend/wta-modules/wta-notify/AGENTS.md</Path> => single-agent (Lead; serial T-29 turn)", "<Path>backend/wta-modules/wta-profile/AGENTS.md</Path> => single-agent (Lead; serial T-29 turn)", "<Path>backend/wta-modules/wta-profile/wta-profile-enterprise/AGENTS.md</Path> => single-agent (Lead; serial T-29 turn)", "<Path>backend/wta-modules/wta-profile/wta-profile-person/AGENTS.md</Path> => single-agent (Lead; serial T-29 turn)", "<Path>backend/wta-modules/wta-sso/AGENTS.md</Path> => single-agent (Lead; serial T-29 turn)", "<Path>backend/wta-modules/wta-system/AGENTS.md</Path> => single-agent (Lead; serial T-29 turn)", "<Path>scripts/README.md</Path> => single-agent (Lead; serial T-29 turn)", "<Path>.agents/skills/engineering-standards/references/project/02-decisions-and-exceptions.md</Path> => single-agent (Lead; serial T-29 turn)", "<Path>release-artifacts/README.md</Path> => single-agent (Lead; serial T-29 turn)"]
---

# T-29：清理过时文档与重复AGENTS权威

Map：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>；Spec：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/spec.md</Path>；Goal：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/goal-plan.md</Path>。
唯一执行者先读Map，再按Skill矩阵读取适用入口/引用，再读本票。禁止任何implementation/review/research子代理。Ready仅表示计划合同就绪，不表示已经授权实施或已验证通过。

## 1. 战略与来源

- 目标与可观察产出：每个删除文件有owner、内容迁移落点和无丢失硬约束证据。
- 来源：D-05, D-06, B-17；AC-029；USER-DECISION: 全面完善计划、无兼容、单人串行。
- 当前事实与调用链：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/re-review.md</Path>中T-29行及对应专项报告；源码导航为frontmatter预计修改点。
- 规划深度：deep；安全/鉴权、公共合同、数据一致性或共享核心路径变更：清理过时文档与重复AGENTS权威。

## 2. 决策状态

### 已锁定决策

ADR-CR-006：重复事实收敛；本change只提出未来永久知识修订，不越权覆盖现有ADR/context。

### 已采用的低影响假设

沿用当前仓库版本与既有模块命名；实施前回读实际源码，路径变化由本票修订，不猜测不存在的实现。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 逐文件规则比对→父owner收敛→真实导航/事实检查 | 现有项目Skill与模块手册；不创建同义副本、不改永久知识 | 本票之外的模块重写、兼容桥、远程发布与重要数据操作 |

## 4. 要构建什么

逐文件规则比对→父owner收敛→真实导航/事实检查。调用者可观察到：每个删除文件有owner、内容迁移落点和无丢失硬约束证据。失败时：SHA变化须重读；无独有硬约束丢失；历史/许可证/供应商保持。

## 5. 实现契约

- 入口、输入输出与数据流：逐文件规则比对→父owner收敛→真实导航/事实检查。
- 不变量及失败语义：SHA变化须重读；无独有硬约束丢失；历史/许可证/供应商保持。
- 公共合同：本票只按以上行为及执行路线变更；同步全部仓内调用/生成物，沿用已有权限/Client/owner校验。未列出的接口保持原语义。
- 兼容：用户明确无需旧版兼容；仓内一次切换，不加双路由/版本等待。实际供应商协议仍须遵守。
- 安全与隐私：凭据不进入日志/UI证据；越权/过期/无owner拒绝；数据库与资源约束不能为前端成功而放宽。

blocked_by=T-01，正文同样只依赖T-01；不能在正文强制所有28张票完成。先收敛当前可证事实，未来未实现能力写target，代码票实施时同步对应事实，T-30最终再验。精确清理分类见<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/cleanup-plan.md</Path>。41个generic不是自动删除：4个REWRITE，37个VERIFY；8个特殊手册KEEP。数字50/3/247按最终源码重新统计并保留口径。

## 6. 执行路线

1. 按cleanup-plan与精确清理清单逐文件比对独有内容，不能以31行或旧名称自动删除。
2. 项目事实从POM/package/发布清单生成或验证，Profile/Module Map为导航权威。
3. 合并generic-only AGENTS至最近父owner，保留Profile/Notify/SSO/Third等真实硬约束和模块差异；41份候选中4份聚合导航REWRITE、37份VERIFY后才可能REMOVE，8份special KEEP。复核原文SHA，不按31行删除；精确清单见<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/cleanup-plan.md</Path>。
4. 重写旧目录、唯一Admin、46POM/176测试、缺失CI/plan/.vscode等current描述；补wta-third模式登记及wta-common-richtext真实模块事实。
5. README留任务导航与真实命令，不复制易漂移数量/历史完成叙述。
6. 保留许可证、供应商包名/schema、历史archive和有激活门槛的placeholder；永久知识只走归档网关。

## 7. 路径访问契约

预计点、可写范围、只读上下文及共享项以frontmatter为唯一权威。共享owner固定single-agent；只有当前票轮次可写，下一票须回读前一结果。跨模块目录只允许本票行为必需的文件，目录授权不意味着重写全部模块。新增测试位于同模块测试目录；未覆盖的新路径先修订本票/Map再写。
保留当前用户未提交改动、永久ADR/context、供应商源码与运行数据；生成物通过正式工具更新。

## 8. 验证矩阵

| 行为或风险 | 验证接缝/步骤 | 预期结果 | Evidence |
|---|---|---|---|
| 正常路径 | 逐文件规则比对→父owner收敛→真实导航/事实检查；执行下列定向命令及对应场景 | 每个删除文件有owner、内容迁移落点和无丢失硬约束证据 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-29.md</Path> |
| 失败路径 | SHA变化须重读；无独有硬约束丢失；历史/许可证/供应商保持；固定时序/故障注入，记录输入与最终可观察状态 | 无越权、错误状态或部分提交；可按定义恢复 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-29.md</Path> |
| 回归 | 运行所属包原有测试及受影响调用者；逐项核对下列AC | 50POM/3App/247后端测试基线与最终源码变化一致（最终重算）；所有当前引用和cwd命令可解析；文档不把候选CI、未跑服务或未批准设计写成已完成 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-29.md</Path> |

Workspace checks：current-workspace；`frontend:`/`backend:`表示先进入该目录，其他命令cwd为仓根，逐条串行执行。新增用例实施时登记精确选择器、实际测试数与跳过项，零测试/required跳过不算通过。

- `node .agents/skills/engineering-standards/scripts/validate-skill-facts.mjs`
- `node docs/fm/scripts/validate.mjs`

- E2E disposition：not-required: 逐文件hash/规则去向/路径引用和事实检查直接覆盖文档交付。
- E2E owner/environment：single-agent（Lead）/current-workspace；使用隔离MySQL/Redis/OSS及必要真实HTTP/浏览器，禁止连生产。场景步骤以上表、本票AC为准；需新用例时在写集内创建后记录精确命令。
- Integration evidence：记录parent before、implementation commit及direct-parent检查；result SHA等于通过验证的implementation commit，candidate不适用。当前全部产品检查not-run。

## 9. 发布、迁移与恢复

- 顺序：前置票产生已验证合同后实施本票；源码、仓内消费者、测试和生成物同批交付。涉及DDL只编辑10-cde-base-ddl.sql，新环境按六文件基座初始化；不增加存量迁移工程。
- 兼容窗口：无；不保留旧接口或数据格式桥。生产部署不是本票自动步骤。
- 监控/诊断：观察本票AC的成功/错误状态、耗时及资源/持久化结果，日志只含安全元数据；复用现有观测入口，不新建监控平台。
- 恢复：从逐文件补丁恢复内容及引用；保留原SHA/去向证据，不删用户改动。
- 不可逆批准点：提交、推送、部署、运行数据删除/修复分别需授权；本轮只有计划文档授权。
- 收缩条件：本票替代的旧调用/配置引用归零且仓内回归通过；无被替代入口时不适用，不为凑清单扩大删除范围。

## 10. 验收标准

- [ ] `AC-029`：每个删除文件有owner、内容迁移落点和无丢失硬约束证据。
- [ ] `AC-029`：50POM/3App/247后端测试基线与最终源码变化一致（最终重算）。
- [ ] `AC-029`：所有当前引用和cwd命令可解析。
- [ ] `AC-029`：文档不把候选CI、未跑服务或未批准设计写成已完成。
- [ ] 按Map→适用Skill→本票完成读取及实际调用；所有required Skill记录passed并可回读。
- [ ] 正常/失败/回归及required E2E均完成，证据写入<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-29.md</Path>，未执行不得标通过。
- [ ] 修改不超出写集，共享项只有single-agent当前票轮次写入。
- [ ] 获得授权后形成非空implementation commit，Lead完成direct-parent验收并记录parent result SHA；未获授权不提交、不标Done。
- [ ] Ticket、Map、Goal与Evidence一致；不存在未批准偏差。

## 11. SKILL 调用计划

frontmatter绑定的项目Skill在implementation阶段接收本票路径和上游合同，产出适用分层、权限、数据/资源边界及实现diff；engineering-standards在verify阶段根据本节命令选择受影响门禁并输出AC/退出码/E2E记录。按入口scope展开引用，不以“已读”代替实际操作。
必需入口缺失或sha256漂移时阻塞本票；Lead回读差异后更新绑定及Map，不能自动接受新摘要。实际记录归<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-29.md</Path>的`## Skill Execution Records`，本次规划不伪造实现调用记录。

## 12. 停止、检查点与交付

交付本票完整可观察行为及验收证据；数量以Map为准。缺依赖/测试环境/Skill、越界或高影响事实变化时停止受影响票，保留checkpoint和失败证据，其他独立票仍可串行推进。恢复先读Goal、Map、本票、状态及最新Evidence；记录实际HEAD/dirty差异，禁止覆盖用户修改。
依赖：T-01。单票完成条件为全部AC、实际Skill证据和获授权的direct-parent出口；仅补文档不能标Done。
