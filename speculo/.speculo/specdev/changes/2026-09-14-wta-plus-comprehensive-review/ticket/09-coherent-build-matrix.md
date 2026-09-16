---
schema_version: 3
plan_contract_version: 1
skill_scan: "已扫描项目 .agents/skills/**/SKILL.md；仅绑定本票命中的入口，执行前按入口继续展开适用 references"
skill_bindings: [{"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "ef0588daecab40e12e49e99d29a12bf107e887ab432e10a790bbc893fc42b0cf", "phase": "plan", "operation": "read-scope-contract-and-bind-acceptance", "inputs": ["当前审查候选、Ticket路径和上游ADR"], "outputs": ["实现前边界、验证与失败停止条件"], "required": true, "on_failure": "block-ticket"}, {"id": "namewta-fullstack-development", "path": "<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>", "sha256": "dd3e42720caa894bcb31ba1503b8211f669b3bbc2bb9ea3e40590a3a9edea395", "phase": "plan", "operation": "read-scope-contract-and-bind-acceptance", "inputs": ["当前审查候选、Ticket路径和上游ADR"], "outputs": ["实现前边界、验证与失败停止条件"], "required": true, "on_failure": "block-ticket"}]
resource_claims: ["finding:D-04", "finding:D-08", "finding:D-09", "contract:AC-009"]
artifact: ticket
change: 2026-09-14-wta-plus-comprehensive-review
id: T-09
title: 统一App模式、bundle与依赖服务验证矩阵
status: draft
planning_depth: deep
planning_depth_reason: "本票涉及 D-04, D-08, D-09；需先完成证据/高影响取舍，不能直接实现。"
ready: false
risk: high
blocked_by: ["T-01"]
contract_ids: [AC-009]
owner: user-review
expected_changes: ["<Path>frontend/package.json</Path>", "<Path>frontend/apps/admin-web/package.json</Path>", "<Path>frontend/apps/home-web/package.json</Path>", "<Path>frontend/apps/sso-web/package.json</Path>", "<Path>backend/pom.xml</Path>", "<Path>backend/wta-admin/pom.xml</Path>", "<Path>scripts/ci/verify-admin-bundle.sh</Path>", "<Path>scripts/ci/run-external-services.sh</Path>", "<Path>release-artifacts/scripts/release-manage.sh</Path>", "<Path>release-artifacts/docker/docker-compose-infrastructure.yml</Path>", "<Path>release-artifacts/tests/</Path>"]
writable_paths: ["<Path>frontend/package.json</Path>", "<Path>frontend/apps/admin-web/package.json</Path>", "<Path>frontend/apps/home-web/package.json</Path>", "<Path>frontend/apps/sso-web/package.json</Path>", "<Path>backend/pom.xml</Path>", "<Path>backend/wta-admin/pom.xml</Path>", "<Path>scripts/ci/verify-admin-bundle.sh</Path>", "<Path>scripts/ci/run-external-services.sh</Path>", "<Path>release-artifacts/scripts/release-manage.sh</Path>", "<Path>release-artifacts/docker/docker-compose-infrastructure.yml</Path>", "<Path>release-artifacts/tests/</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/</Path>", "<Path>{roots.state}/specdev/adr/</Path>"]
shared_paths: ["<Path>frontend/apps/sso-web/package.json</Path>", "<Path>release-artifacts/scripts/release-manage.sh</Path>", "<Path>release-artifacts/tests/</Path>"]
shared_path_owners: ["<Path>frontend/apps/sso-web/package.json</Path> => Lead", "<Path>release-artifacts/scripts/release-manage.sh</Path> => Lead", "<Path>release-artifacts/tests/</Path> => Lead"]
---

# T-09：统一App模式、bundle与依赖服务验证矩阵

> 本 Ticket 是 <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/</Path> 内的待审实施计划。它不授权修改产品代码；只有用户明确接受该 Ticket 后，Lead 才能按 project Skill 建立实施 Evidence。

## 1. 战略与来源

- **Finding：** D-04, D-08, D-09。详细源码证据见专项审查报告。
- **目标：** 删除真实结构复杂性并恢复可验证的行为/安全/交付合同；不以移动代码、放宽门禁、删除测试或无限兼容层制造绿色。
- **可观察产出：** build:dev最终三个App均development，build:prod均production

## 2. 决策状态

- **当前状态：** draft，ready=false；高影响决策：ADR-CR-005：推荐唯一显式构建清单；镜像升级具体版本以兼容验证决定，不盲追最新。
- **已锁定：** 本轮只写 change；实现、提交、发布、远程写入和永久知识修改均未授权。
- **未决：** 用户是否接受本票、外部合同/数据迁移/运行环境参数及是否进入下一 Work。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 建立三App development/production执行矩阵，每个环境每App仅调用一次对应mode；删除dev路径中的隐式production递归覆盖；保留工作区依赖构建顺序；固定full/core required/forbidden artifact，加入Profile两jar断言，核对effective POM和实际jar | 现有模块边界、权限、日志/错误合同中未被本票改变者 | 其他 finding 的实现、无证据的全仓重写、提交/部署、历史数据清理 |

## 4. 行为与实现契约

- **入口或 seam：** <Path>frontend/package.json</Path>；由该 module 的公开 interface 接受输入。
- **输入/输出：** 输入、错误和状态必须保持对应现有合同；任何外部行为变化先更新 Spec/ADR并阻塞本票。
- **不变量：** 失败关闭、权限/Client上下文、资源所有权、事务/幂等和可观测性按适用工程规则成立。
- **错误：** core/full产物必需与禁用列表逐项断言

## 5. 执行路线

1. 建立三App development/production执行矩阵，每个环境每App仅调用一次对应mode。
2. 删除dev路径中的隐式production递归覆盖；保留工作区依赖构建顺序。
3. 固定full/core required/forbidden artifact，加入Profile两jar断言，核对effective POM和实际jar。
4. 区分-DskipTests仍编译测试与-Dmaven.test.skip=true完全跳过；core构建复用同候选已测试证据，不删除测试或扩大bundle。
5. 统一CI/Compose MySQL/Redis/MinIO镜像与可得digest；不同版本必须显式声明兼容矩阵。
6. 保存两种clean构建及外部服务的证据来源。

## 6. 路径访问与所有权

- **可写候选：** frontmatter的writable_paths为精确实施边界；当前仍只写change。T-29同时受cleanup-plan逐文件分类约束，KEEP文件不能因出现在写集便直接删除。
- **共享路径：** frontmatter列明的交集由Lead唯一整合。T-01 → T-09 → T-10 → T-08按依赖串行，T-29收敛文档；发现额外交集时先更新Map，不能各自覆盖同一脚本。
- **不动：** 供应商/历史archive、永久 namespace、用户未提交改动和生成物，除非本票修订后有明确 owner。

## 7. 验证矩阵

| 行为/风险 | 接缝与方法 | 预期 | Evidence |
|---|---|---|---|
| AC 场景 1 | <Path>frontend/package.json</Path> 定向测试/静态或隔离运行 | build:dev最终三个App均development，build:prod均production | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-09.md</Path> |
| AC 场景 2 | <Path>frontend/package.json</Path> 定向测试/静态或隔离运行 | core/full产物必需与禁用列表逐项断言 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-09.md</Path> |
| AC 场景 3 | <Path>frontend/package.json</Path> 定向测试/静态或隔离运行 | 测试证据与打包候选匹配，不以skip冒充测试通过 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-09.md</Path> |
| AC 场景 4 | <Path>frontend/package.json</Path> 定向测试/静态或隔离运行 | CI与部署镜像版本相同或有明确差异测试 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-09.md</Path> |

- **Workspace checks（当前 not-run）：** cwd `<Path>frontend</Path>`：`pnpm build:dev`、`pnpm build:prod`；cwd `<Path>backend</Path>`：`./mvnw test`、`./mvnw clean package -DskipTests`、`./mvnw clean package -Pbundle-core -Dmaven.test.skip=true`；cwd `<Path>.</Path>`：`bash scripts/ci/verify-admin-bundle.sh full`（分别在对应 clean 产物后执行）。本票未运行实现验证。
- **E2E disposition：** required：跨模块/浏览器/外部服务行为必须由 Lead 在 current workspace 或 parent-candidate 验收。

## 8. 迁移、发布与恢复

- **迁移/兼容：** ADR-CR-005：推荐唯一显式构建清单；镜像升级具体版本以兼容验证决定，不盲追最新。
- **回滚：** 先保留基线 SHA、产物/数据库备份及可验证恢复点；任何不可逆删除、DDL、凭据轮换或发布需独立批准。
- **停止条件：** 证据缺失、路径越界、父分支漂移、权限/安全合同不明、测试未运行或出现未批准偏差时停票。

## 9. 验收标准

- [ ] `AC-009`：build:dev最终三个App均development，build:prod均production。
- [ ] `AC-009`：core/full产物必需与禁用列表逐项断言。
- [ ] `AC-009`：测试证据与打包候选匹配，不以skip冒充测试通过。
- [ ] `AC-009`：CI与部署镜像版本相同或有明确差异测试。
- [ ] Ticket、Map、Spec、ADR 与 Evidence 状态一致；本票保持 draft/ready=false 直到用户接受并完成计划质量复核。
- [ ] 未通过删除测试、关闭类型/安全规则、吞错误、扩大权限或伪造运行证据获得通过。

## 10. Skill 调用计划

- 计划绑定：以frontmatter的skill_bindings为完整集合，进入实现前重新读取真实Skill入口；T-01的全栈Skill用于模块模式validator，T-09用于App与构建合同。新路径或Skill先由Lead同步Map。
- 失败动作：必需入口、验证工具或安全/数据证据不可用则 block-ticket；不得以其他工具静默替换。

## 11. 交付与恢复

- **依赖：** T-01；完成顺序由 Tickets Map 控制。
- **检查点：** 记录实现前 base SHA、工作树、命令/退出码、偏差、最终 result SHA 及未运行项。
- **完成出口：** 只在用户批准、所有适用 AC/E2E/Evidence 通过、父分支包含结果后进入 done；当前审查阶段不进入 done。

## 12. 验证与范围校正

`full|core`仅为枚举，不是可复制shell命令。正确顺序：cwd <Path>backend</Path>运行`./mvnw test`并保存证据；运行`./mvnw clean package -DskipTests`，随后cwd <Path>.</Path>运行`bash scripts/ci/verify-admin-bundle.sh full`并保存JAR清单；再cwd <Path>backend</Path>运行`./mvnw clean package -Pbundle-core -Dmaven.test.skip=true`，随后cwd <Path>.</Path>运行`bash scripts/ci/verify-admin-bundle.sh core`。不得先clean覆盖full产物才验证full。required名单补齐wta-notify与profile-person/enterprise，保留已校验third/sso。App开发构建后先保存mode marker，再构建prod；源码active与release shipped分开。真实外部服务命令是cwd <Path>.</Path>的`bash scripts/ci/run-external-services.sh`，仅由Lead在专属隔离Docker运行；创建/销毁容器，当前not-run。

本票仍为draft / ready=false；实施验证not-run。恢复时按Map → 适用Project Skill → Ticket → 源码证据读取。
