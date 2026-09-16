---
schema_version: 3
plan_contract_version: 1
skill_scan: "已扫描项目 .agents/skills/**/SKILL.md；仅绑定本票命中的入口，执行前按入口继续展开适用 references"
skill_bindings: [{"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "ef0588daecab40e12e49e99d29a12bf107e887ab432e10a790bbc893fc42b0cf", "phase": "plan", "operation": "read-scope-contract-and-bind-acceptance", "inputs": ["当前审查候选、Ticket路径和上游ADR"], "outputs": ["实现前边界、验证与失败停止条件"], "required": true, "on_failure": "block-ticket"}]
resource_claims: ["finding:D-05", "finding:D-06", "contract:AC-029"]
artifact: ticket
change: 2026-09-14-wta-plus-comprehensive-review
id: T-29
title: 清理过时文档与重复AGENTS权威
status: draft
planning_depth: deep
planning_depth_reason: "本票涉及 D-05, D-06；需先完成证据/高影响取舍，不能直接实现。"
ready: false
risk: medium
blocked_by: ["T-01"]
contract_ids: [AC-029]
owner: user-review
expected_changes: ["<Path>backend/AGENTS.md</Path>", "<Path>backend/wta-admin/AGENTS.md</Path>", "<Path>backend/wta-api/AGENTS.md</Path>", "<Path>backend/wta-common/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-ai/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-bom/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-core/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-doc/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-elasticsearch/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-encrypt/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-excel/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-job/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-json/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-liteflow/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-log/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-mail/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-mcp/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-mqtt/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-mybatis/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-nacos/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-notify/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-openapi/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-oss/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-push/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-redis/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-satoken/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-security/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-sensitive/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-sms/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-social/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-translation/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-web/AGENTS.md</Path>", "<Path>backend/wta-extend/AGENTS.md</Path>", "<Path>backend/wta-extend/wta-monitor-admin/AGENTS.md</Path>", "<Path>backend/wta-extend/wta-snailai-server/AGENTS.md</Path>", "<Path>backend/wta-extend/wta-snailjob-server/AGENTS.md</Path>", "<Path>backend/wta-modules/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-ai/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-job/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-profile/wta-profile-bom/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-workflow/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-demo/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-notify/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-profile/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-profile/wta-profile-enterprise/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-profile/wta-profile-person/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-sso/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-system/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-third/AGENTS.md</Path>", "<Path>scripts/README.md</Path>", "<Path>.agents/skills/engineering-standards/references/project/00-project-profile.md</Path>", "<Path>.agents/skills/engineering-standards/references/project/01-module-map.md</Path>", "<Path>.agents/skills/engineering-standards/references/project/02-decisions-and-exceptions.md</Path>", "<Path>README.md</Path>", "<Path>frontend/README.md</Path>", "<Path>frontend/AGENTS.md</Path>", "<Path>frontend/apps/README.md</Path>", "<Path>frontend/docs/architecture-baseline.md</Path>", "<Path>backend/README.md</Path>", "<Path>docs/README.md</Path>", "<Path>docs/namewta-enhancements.md</Path>", "<Path>docs/runtime-nacos-hard-cut.md</Path>", "<Path>docs/oss-public-private-operations.md</Path>", "<Path>release-artifacts/README.md</Path>", "<Path>.agents/skills/engineering-standards/references/project/03-backend-module-modes.md</Path>"]
writable_paths: ["<Path>backend/AGENTS.md</Path>", "<Path>backend/wta-admin/AGENTS.md</Path>", "<Path>backend/wta-api/AGENTS.md</Path>", "<Path>backend/wta-common/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-ai/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-bom/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-core/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-doc/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-elasticsearch/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-encrypt/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-excel/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-job/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-json/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-liteflow/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-log/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-mail/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-mcp/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-mqtt/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-mybatis/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-nacos/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-notify/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-openapi/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-oss/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-push/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-redis/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-satoken/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-security/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-sensitive/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-sms/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-social/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-translation/AGENTS.md</Path>", "<Path>backend/wta-common/wta-common-web/AGENTS.md</Path>", "<Path>backend/wta-extend/AGENTS.md</Path>", "<Path>backend/wta-extend/wta-monitor-admin/AGENTS.md</Path>", "<Path>backend/wta-extend/wta-snailai-server/AGENTS.md</Path>", "<Path>backend/wta-extend/wta-snailjob-server/AGENTS.md</Path>", "<Path>backend/wta-modules/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-ai/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-job/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-profile/wta-profile-bom/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-workflow/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-demo/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-notify/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-profile/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-profile/wta-profile-enterprise/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-profile/wta-profile-person/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-sso/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-system/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-third/AGENTS.md</Path>", "<Path>scripts/README.md</Path>", "<Path>.agents/skills/engineering-standards/references/project/00-project-profile.md</Path>", "<Path>.agents/skills/engineering-standards/references/project/01-module-map.md</Path>", "<Path>.agents/skills/engineering-standards/references/project/02-decisions-and-exceptions.md</Path>", "<Path>README.md</Path>", "<Path>frontend/README.md</Path>", "<Path>frontend/AGENTS.md</Path>", "<Path>frontend/apps/README.md</Path>", "<Path>frontend/docs/architecture-baseline.md</Path>", "<Path>backend/README.md</Path>", "<Path>docs/README.md</Path>", "<Path>docs/namewta-enhancements.md</Path>", "<Path>docs/runtime-nacos-hard-cut.md</Path>", "<Path>docs/oss-public-private-operations.md</Path>", "<Path>release-artifacts/README.md</Path>", "<Path>.agents/skills/engineering-standards/references/project/03-backend-module-modes.md</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/</Path>", "<Path>{roots.state}/specdev/adr/</Path>"]
shared_paths: ["<Path>scripts/README.md</Path>", "<Path>release-artifacts/README.md</Path>"]
shared_path_owners: ["<Path>scripts/README.md</Path> => Lead", "<Path>release-artifacts/README.md</Path> => Lead"]
---

# T-29：清理过时文档与重复AGENTS权威

> 本 Ticket 是 <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/</Path> 内的待审实施计划。它不授权修改产品代码；只有用户明确接受该 Ticket 后，Lead 才能按 project Skill 建立实施 Evidence。

## 1. 战略与来源

- **Finding：** D-05, D-06。详细源码证据见专项审查报告。
- **目标：** 删除真实结构复杂性并恢复可验证的行为/安全/交付合同；不以移动代码、放宽门禁、删除测试或无限兼容层制造绿色。
- **可观察产出：** 每个删除文件有owner、内容迁移落点和无丢失硬约束证据

## 2. 决策状态

- **当前状态：** draft，ready=false；高影响决策：ADR-CR-006：重复事实收敛；本change只提出未来永久知识修订，不越权覆盖现有ADR/context。
- **已锁定：** 本轮只写 change；实现、提交、发布、远程写入和永久知识修改均未授权。
- **未决：** 用户是否接受本票、外部合同/数据迁移/运行环境参数及是否进入下一 Work。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 按cleanup-plan与精确清理清单逐文件比对独有内容，不能以31行或旧名称自动删除；项目事实从POM/package/发布清单生成或验证，Profile/Module Map为导航权威；合并generic-only AGENTS至最近父owner，保留Profile/Notify/SSO/Third等真实硬约束和模块差异 | 现有模块边界、权限、日志/错误合同中未被本票改变者 | 其他 finding 的实现、无证据的全仓重写、提交/部署、历史数据清理 |

## 4. 行为与实现契约

- **入口或 seam：** <Path>.agents/skills/</Path>；由该 module 的公开 interface 接受输入。
- **输入/输出：** 输入、错误和状态必须保持对应现有合同；任何外部行为变化先更新 Spec/ADR并阻塞本票。
- **不变量：** 失败关闭、权限/Client上下文、资源所有权、事务/幂等和可观测性按适用工程规则成立。
- **错误：** 50POM/3App/247后端测试基线与最终源码变化一致（最终重算）

## 5. 执行路线

1. 按cleanup-plan与精确清理清单逐文件比对独有内容，不能以31行或旧名称自动删除。
2. 项目事实从POM/package/发布清单生成或验证，Profile/Module Map为导航权威。
3. 合并generic-only AGENTS至最近父owner，保留Profile/Notify/SSO/Third等真实硬约束和模块差异；41份候选中4份聚合导航REWRITE、37份VERIFY后才可能REMOVE，8份special KEEP。复核原文SHA，不按31行删除；精确清单见<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/cleanup-plan.md</Path>。
4. 重写旧目录、唯一Admin、46POM/176测试、缺失CI/plan/.vscode等current描述；补wta-third模式登记及wta-common-richtext真实模块事实。
5. README留任务导航与真实命令，不复制易漂移数量/历史完成叙述。
6. 保留许可证、供应商包名/schema、历史archive和有激活门槛的placeholder；永久知识只走归档网关。

## 6. 路径访问与所有权

- **可写候选：** frontmatter的writable_paths为精确实施边界；当前仍只写change。T-29同时受cleanup-plan逐文件分类约束，KEEP文件不能因出现在写集便直接删除。
- **共享路径：** frontmatter列明的交集由Lead唯一整合。T-01 → T-09 → T-10 → T-08按依赖串行，T-29收敛文档；发现额外交集时先更新Map，不能各自覆盖同一脚本。
- **不动：** 供应商/历史archive、永久 namespace、用户未提交改动和生成物，除非本票修订后有明确 owner。

## 7. 验证矩阵

| 行为/风险 | 接缝与方法 | 预期 | Evidence |
|---|---|---|---|
| AC 场景 1 | <Path>.agents/skills/</Path> 定向测试/静态或隔离运行 | 每个删除文件有owner、内容迁移落点和无丢失硬约束证据 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-29.md</Path> |
| AC 场景 2 | <Path>.agents/skills/</Path> 定向测试/静态或隔离运行 | 50POM/3App/247后端测试基线与最终源码变化一致（最终重算） | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-29.md</Path> |
| AC 场景 3 | <Path>.agents/skills/</Path> 定向测试/静态或隔离运行 | 所有当前引用和cwd命令可解析 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-29.md</Path> |
| AC 场景 4 | <Path>.agents/skills/</Path> 定向测试/静态或隔离运行 | 文档不把候选CI、未跑服务或未批准设计写成已完成 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-29.md</Path> |

- **Workspace checks（当前 not-run）：** cwd `<Path>.</Path>`：`node .agents/skills/engineering-standards/scripts/validate-skill-facts.mjs`、`node docs/fm/scripts/validate.mjs`；cwd `<Path>.</Path>`：link/command/inventory/owner audit。文档清理不得把未运行验证写成通过。
- **E2E disposition：** 按实际跨边界风险决定；未获用户批准前不声明通过。

## 8. 迁移、发布与恢复

- **迁移/兼容：** ADR-CR-006：重复事实收敛；本change只提出未来永久知识修订，不越权覆盖现有ADR/context。
- **回滚：** 先保留基线 SHA、产物/数据库备份及可验证恢复点；任何不可逆删除、DDL、凭据轮换或发布需独立批准。
- **停止条件：** 证据缺失、路径越界、父分支漂移、权限/安全合同不明、测试未运行或出现未批准偏差时停票。

## 9. 验收标准

- [ ] `AC-029`：每个删除文件有owner、内容迁移落点和无丢失硬约束证据。
- [ ] `AC-029`：50POM/3App/247后端测试基线与最终源码变化一致（最终重算）。
- [ ] `AC-029`：所有当前引用和cwd命令可解析。
- [ ] `AC-029`：文档不把候选CI、未跑服务或未批准设计写成已完成。
- [ ] Ticket、Map、Spec、ADR 与 Evidence 状态一致；本票保持 draft/ready=false 直到用户接受并完成计划质量复核。
- [ ] 未通过删除测试、关闭类型/安全规则、吞错误、扩大权限或伪造运行证据获得通过。

## 10. Skill 调用计划

- 计划绑定：以frontmatter的skill_bindings为完整集合，进入实现前重新读取真实Skill入口；T-01的全栈Skill用于模块模式validator，T-09用于App与构建合同。新路径或Skill先由Lead同步Map。
- 失败动作：必需入口、验证工具或安全/数据证据不可用则 block-ticket；不得以其他工具静默替换。

## 11. 交付与恢复

- **依赖：** T-01，与frontmatter一致。可先清理已确认旧事实；下游未实施能力只写current/target，由后续票owner同步，T-30最终重验。
- **检查点：** 记录实现前 base SHA、工作树、命令/退出码、偏差、最终 result SHA 及未运行项。
- **完成出口：** 只在用户批准、所有适用 AC/E2E/Evidence 通过、父分支包含结果后进入 done；当前审查阶段不进入 done。

## 12. 验证与范围校正

blocked_by=T-01，正文同样只依赖T-01；不能在正文强制所有28张票完成。先收敛当前可证事实，未来未实现能力写target，代码票实施时同步对应事实，T-30最终再验。精确清理分类见<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/cleanup-plan.md</Path>。41个generic不是自动删除：4个REWRITE，37个VERIFY；8个特殊手册KEEP。数字50/3/247按最终源码重新统计并保留口径。

本票仍为draft / ready=false；实施验证not-run。恢复时按Map → 适用Project Skill → Ticket → 源码证据读取。
