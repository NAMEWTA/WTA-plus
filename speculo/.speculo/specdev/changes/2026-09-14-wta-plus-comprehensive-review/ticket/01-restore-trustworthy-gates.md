---
schema_version: 3
plan_contract_version: 1
skill_scan: "已扫描项目 .agents/skills/**/SKILL.md；仅绑定本票命中的入口，执行前按入口继续展开适用 references"
skill_bindings: [{"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "ef0588daecab40e12e49e99d29a12bf107e887ab432e10a790bbc893fc42b0cf", "phase": "plan", "operation": "read-scope-contract-and-bind-acceptance", "inputs": ["当前审查候选、Ticket路径和上游ADR"], "outputs": ["实现前边界、验证与失败停止条件"], "required": true, "on_failure": "block-ticket"}, {"id": "namewta-fullstack-development", "path": "<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>", "sha256": "dd3e42720caa894bcb31ba1503b8211f669b3bbc2bb9ea3e40590a3a9edea395", "phase": "plan", "operation": "read-scope-contract-and-bind-acceptance", "inputs": ["当前审查候选、Ticket路径和上游ADR"], "outputs": ["实现前边界、验证与失败停止条件"], "required": true, "on_failure": "block-ticket"}]
resource_claims: ["finding:D-01", "finding:D-07", "finding:D-11", "finding:B-09", "contract:AC-001"]
artifact: ticket
change: 2026-09-14-wta-plus-comprehensive-review
id: T-01
title: 恢复可信的仓库门禁与治理入口
status: draft
planning_depth: standard
planning_depth_reason: "本票涉及 D-01, D-07, D-11, B-09；需先完成证据/高影响取舍，不能直接实现。"
ready: false
risk: medium
blocked_by: []
contract_ids: [AC-001]
owner: user-review
expected_changes: ["<Path>.agents/skills/engineering-standards/scripts/validate-skill-facts.mjs</Path>", "<Path>.agents/skills/engineering-standards/scripts/validate-skill-facts.test.mjs</Path>", "<Path>.agents/skills/namewta-fullstack-development/scripts/validate-module-mode.mjs</Path>", "<Path>.agents/skills/namewta-fullstack-development/scripts/validate-module-mode.test.mjs</Path>", "<Path>scripts/ci/verify-submodules.sh</Path>", "<Path>scripts/ci/verify-dev-build-guard.sh</Path>", "<Path>scripts/README.md</Path>", "<Path>.github/workflows/quality-gates.yml</Path>", "<Path>.vscode/settings.json</Path>", "<Path>{roots.state}/specdev/config.json</Path>"]
writable_paths: ["<Path>.agents/skills/engineering-standards/scripts/validate-skill-facts.mjs</Path>", "<Path>.agents/skills/engineering-standards/scripts/validate-skill-facts.test.mjs</Path>", "<Path>.agents/skills/namewta-fullstack-development/scripts/validate-module-mode.mjs</Path>", "<Path>.agents/skills/namewta-fullstack-development/scripts/validate-module-mode.test.mjs</Path>", "<Path>scripts/ci/verify-submodules.sh</Path>", "<Path>scripts/ci/verify-dev-build-guard.sh</Path>", "<Path>scripts/README.md</Path>", "<Path>.github/workflows/quality-gates.yml</Path>", "<Path>.vscode/settings.json</Path>", "<Path>{roots.state}/specdev/config.json</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/</Path>", "<Path>{roots.state}/specdev/adr/</Path>"]
shared_paths: ["<Path>scripts/README.md</Path>"]
shared_path_owners: ["<Path>scripts/README.md</Path> => Lead"]
---

# T-01：恢复可信的仓库门禁与治理入口

> 本 Ticket 是 <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/</Path> 内的待审实施计划。它不授权修改产品代码；只有用户明确接受该 Ticket 后，Lead 才能按 project Skill 建立实施 Evidence。

## 1. 战略与来源

- **Finding：** D-01, D-07, D-11, B-09。详细源码证据见专项审查报告。
- **目标：** 删除真实结构复杂性并恢复可验证的行为/安全/交付合同；不以移动代码、放宽门禁、删除测试或无限兼容层制造绿色。
- **可观察产出：** 干净clone不创建temp/release也通过事实检查

## 2. 决策状态

- **当前状态：** draft，ready=false；高影响决策：是否恢复GitHub Actions由ADR-CR-006待审；全局SpecDev配置属于未来写集，本轮不改。
- **已锁定：** 本轮只写 change；实现、提交、发布、远程写入和永久知识修改均未授权。
- **未决：** 用户是否接受本票、外部合同/数据迁移/运行环境参数及是否进入下一 Work。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 记录真实命令/cwd/工具版本/失败分类；先保存现有红灯基线，不把检查器误报当产品缺陷；删除无submodule仍强求快照的死检查与调用映射；明确重建CI为本地候选配置；模块分层检查以org.namewta自有namespace和词法/AST角色判断，排除Javadoc伪注解，保留第三方org.dromara依赖 | 现有模块边界、权限、日志/错误合同中未被本票改变者 | 其他 finding 的实现、无证据的全仓重写、提交/部署、历史数据清理 |

## 4. 行为与实现契约

- **入口或 seam：** <Path>.agents/skills/engineering-standards/scripts/validate-skill-facts.mjs</Path>；由该 module 的公开 interface 接受输入。
- **输入/输出：** 输入、错误和状态必须保持对应现有合同；任何外部行为变化先更新 Spec/ADR并阻塞本票。
- **不变量：** 失败关闭、权限/Client上下文、资源所有权、事务/幂等和可观测性按适用工程规则成立。
- **错误：** 注释中的DSTransactional不触发越层误报，真实非法import必须失败；clean clone不需创建temp/release；不存在CI时只能报告local gate。

## 5. 执行路线

1. 记录真实命令/cwd/工具版本/失败分类；先保存现有红灯基线，不把检查器误报当产品缺陷。
2. 删除无submodule仍强求快照的死检查与调用映射；明确重建CI为本地候选配置。
3. 模块分层检查以org.namewta自有namespace和词法/AST角色判断，排除Javadoc伪注解，保留第三方org.dromara依赖。
4. 用最小正负夹具覆盖非法Mapper import、错误层事务、合法注释及有意供应商坐标；不通过扩大allowlist变绿。
5. 去掉忽略目录temp/release必须存在的条件，检查ignore规则与输出owner即可。
6. 按真实monorepo cwd更新SpecDev verification，并将外部服务、类型、OpenAPI/架构检查分别登记。裁决 scripts/ci/verify-dev-build-guard.sh 对缺失 .vscode/settings.json 的前置条件，不能删除构建锁或JAR完整性断言。

## 6. 路径访问与所有权

- **可写候选：** frontmatter的writable_paths为精确实施边界；当前仍只写change。T-29同时受cleanup-plan逐文件分类约束，KEEP文件不能因出现在写集便直接删除。
- **共享路径：** frontmatter列明的交集由Lead唯一整合。T-01 → T-09 → T-10 → T-08按依赖串行，T-29收敛文档；发现额外交集时先更新Map，不能各自覆盖同一脚本。
- **不动：** 供应商/历史archive、永久 namespace、用户未提交改动和生成物，除非本票修订后有明确 owner。

## 7. 验证矩阵

| 行为/风险 | 接缝与方法 | 预期 | Evidence |
|---|---|---|---|
| AC 场景 1 | <Path>.agents/skills/engineering-standards/scripts/validate-skill-facts.mjs</Path> 定向测试/静态或隔离运行 | 干净clone不创建temp/release也通过事实检查 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-01.md</Path> |
| AC 场景 2 | <Path>.agents/skills/engineering-standards/scripts/validate-skill-facts.mjs</Path> 定向测试/静态或隔离运行 | 注释中DSTransactional不触发越层误报，真实非法import必须失败 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-01.md</Path> |
| AC 场景 3 | <Path>.agents/skills/engineering-standards/scripts/validate-skill-facts.mjs</Path> 定向测试/静态或隔离运行 | 不存在CI文件时文档不得宣称active；远程required状态有独立证据 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-01.md</Path> |
| AC 场景 4 | <Path>.agents/skills/engineering-standards/scripts/validate-skill-facts.mjs</Path> 定向测试/静态或隔离运行 | 每个声明命令可在对应cwd解析，Maven/前端测试与package分开记录 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-01.md</Path> |

- **Workspace checks（当前 not-run）：** cwd `<Path>.</Path>`：`node .agents/skills/engineering-standards/scripts/validate-skill-facts.mjs`；cwd `<Path>.</Path>`：`node .agents/skills/namewta-fullstack-development/scripts/validate-module-mode.mjs backend/wta-modules/wta-sso --mode layered`。实施前按真实脚本重新解析；本票草案不声称通过。
- **E2E disposition：** 按实际跨边界风险决定；未获用户批准前不声明通过。

## 8. 迁移、发布与恢复

- **迁移/兼容：** 是否恢复GitHub Actions由ADR-CR-006待审；全局SpecDev配置属于未来写集，本轮不改。
- **回滚：** 先保留基线 SHA、产物/数据库备份及可验证恢复点；任何不可逆删除、DDL、凭据轮换或发布需独立批准。
- **停止条件：** 证据缺失、路径越界、父分支漂移、权限/安全合同不明、测试未运行或出现未批准偏差时停票。

## 9. 验收标准

- [ ] `AC-001`：干净clone不创建temp/release也通过事实检查。
- [ ] `AC-001`：注释中DSTransactional不触发越层误报，真实非法import必须失败。
- [ ] `AC-001`：不存在CI文件时文档不得宣称active；远程required状态有独立证据。
- [ ] `AC-001`：每个声明命令可在对应cwd解析，Maven/前端测试与package分开记录。
- [ ] Ticket、Map、Spec、ADR 与 Evidence 状态一致；本票保持 draft/ready=false 直到用户接受并完成计划质量复核。
- [ ] 未通过删除测试、关闭类型/安全规则、吞错误、扩大权限或伪造运行证据获得通过。

## 10. Skill 调用计划

- 计划绑定：以frontmatter的skill_bindings为完整集合，进入实现前重新读取真实Skill入口；T-01的全栈Skill用于模块模式validator，T-09用于App与构建合同。新路径或Skill先由Lead同步Map。
- 失败动作：必需入口、验证工具或安全/数据证据不可用则 block-ticket；不得以其他工具静默替换。

## 11. 交付与恢复

- **依赖：** 无；完成顺序由 Tickets Map 控制。
- **检查点：** 记录实现前 base SHA、工作树、命令/退出码、偏差、最终 result SHA 及未运行项。
- **完成出口：** 只在用户批准、所有适用 AC/E2E/Evidence 通过、父分支包含结果后进入 done；当前审查阶段不进入 done。

## 12. 验证与范围校正

新增的validator *.test.mjs是计划文件，当前不存在；创建后由Lead将精确node --test命令加入Evidence，不能当现有门禁。facts正负样本与module-mode词法/import正负样本分别验证，不能只对facts入口做测试。layered检查至少逐个覆盖person、enterprise、notify、sso、third，按最终模式登记裁决；现有产品违规交回所属票。build-guard缺失settings必须解决环境合同，不能删除锁与JAR完整性保护。

本票仍为draft / ready=false；实施验证not-run。恢复时按Map → 适用Project Skill → Ticket → 源码证据读取。
