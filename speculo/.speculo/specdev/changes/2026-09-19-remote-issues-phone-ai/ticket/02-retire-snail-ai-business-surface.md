---
schema_version: 3
plan_contract_version: 1
skill_scan: "已枚举项目唯一根 .agents/skills 下7个真实入口；按账号/AI退出/构建scope绑定匹配项，未使用Speculo Skill伪装项目Skill。"
skill_bindings: [{"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "verify", "operation": "verify-scope-contracts-and-quality-gates", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/spec.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/ticket/02-retire-snail-ai-business-surface.md</Path>", "当前Ticket diff与已核验源码/调用方"], "outputs": ["本票范围和调用映射", "实际执行记录、验证命令/退出码及停止点"], "required": true, "on_failure": "block-ticket", "references": []}, {"id": "namewta-fullstack-development", "path": "<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>", "sha256": "675c053c11d8b22cd394c875f48688242d8e8328dd14e69657f5d5b12f2af68b", "phase": "implement", "operation": "implement-vertical-slice-and-verify-boundaries", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/spec.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/ticket/02-retire-snail-ai-business-surface.md</Path>", "当前Ticket diff与已核验源码/调用方"], "outputs": ["本票范围和调用映射", "实际执行记录、验证命令/退出码及停止点"], "required": true, "on_failure": "block-ticket", "references": []}, {"id": "wta-module-guide", "path": "<Path>.agents/skills/wta-module-guide/SKILL.md</Path>", "sha256": "bb57a781314abe316f06ba9538f62043f1bd053904c0832969e575c09731a7a3", "phase": "implement", "operation": "resolve-module-owner-and-public-entrypoints", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/spec.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/ticket/02-retire-snail-ai-business-surface.md</Path>", "当前Ticket diff与已核验源码/调用方"], "outputs": ["本票范围和调用映射", "实际执行记录、验证命令/退出码及停止点"], "required": true, "on_failure": "block-ticket", "references": []}, {"id": "wta-common-modules-guide", "path": "<Path>.agents/skills/wta-common-modules-guide/SKILL.md</Path>", "sha256": "e92775ce47af41bd33c1b3293d7f8a3185fdd739b9b588519e043c654f678d98", "phase": "implement", "operation": "resolve-common-validation-and-vendor-dependency-boundaries", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/spec.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/ticket/02-retire-snail-ai-business-surface.md</Path>", "当前Ticket diff与已核验源码/调用方"], "outputs": ["本票范围和调用映射", "实际执行记录、验证命令/退出码及停止点"], "required": true, "on_failure": "block-ticket", "references": []}]
resource_claims: ["snail-ai-business-retirement", "admin-retired-ai-navigation", "frontend-package-lock"]
artifact: "ticket"
change: "2026-09-19-remote-issues-phone-ai"
id: "T-02"
title: "退出 Snail AI 业务入口并保留 Java 占位"
status: "done"
kind: "refactor"
planning_depth: "deep"
planning_depth_reason: "移除 HTTP 能力、前端导航/权限组合与第三方依赖，需要保护旧数据库菜单和其他功能。"
ready: false
risk: "high"
blocked_by: []
contract_ids: ["AC-006", "AC-007", "AC-008"]
owner: "codex-root"
expected_changes: ["<Path>backend/wta-modules/wta-ai/**</Path>", "<Path>backend/wta-common/wta-common-ai/**</Path>", "<Path>backend/wta-admin/src/main/resources/application.yml</Path>"]
writable_paths: ["<Path>frontend/e2e/ai-domain.spec.ts</Path>", "<Path>frontend/apps/admin-web/src/types/auto-imports.d.ts</Path>", "<Path>backend/wta-modules/wta-ai/**</Path>", "<Path>backend/wta-common/wta-common-ai/**</Path>", "<Path>backend/wta-admin/src/main/resources/application.yml</Path>", "<Path>backend/wta-admin/src/main/resources/application-local.yml</Path>", "<Path>backend/wta-admin/src/main/resources/application-dev.yml</Path>", "<Path>backend/wta-admin/src/main/resources/application-prod.yml</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/ai/**</Path>", "<Path>frontend/packages/domains/ai/**</Path>", "<Path>frontend/packages/web-domains/ai/**</Path>", "<Path>frontend/packages/domains/system/src/monitor/**</Path>", "<Path>frontend/apps/admin-web/package.json</Path>", "<Path>frontend/apps/admin-web/src/application/services.ts</Path>", "<Path>frontend/apps/admin-web/src/router/adminManifestRegistry.ts</Path>", "<Path>frontend/apps/admin-web/src/router/adminManifestRegistry.test.ts</Path>", "<Path>frontend/apps/admin-web/src/store/modules/navigation.ts</Path>", "<Path>frontend/apps/admin-web/src/store/modules/navigation.test.ts</Path>", "<Path>frontend/apps/admin-web/src/types/env.d.ts</Path>", "<Path>frontend/apps/admin-web/src/views/monitor/external/index.vue</Path>", "<Path>frontend/tooling/architecture/test/domain-layout.test.mjs</Path>", "<Path>frontend/pnpm-lock.yaml</Path>", "<Path>frontend/e2e/snail-ai-retirement.spec.ts</Path>"]
read_only_paths: ["<Path>AGENTS.md</Path>", "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "<Path>backend/wta-common/wta-common-core/src/main/java/org/namewta/common/core/validation/ValidationUtils.java</Path>", "<Path>frontend/packages/api-contracts/**</Path>", "<Path>release-artifacts/**</Path>"]
shared_paths: ["<Path>frontend/apps/admin-web/package.json</Path>", "<Path>frontend/apps/admin-web/src/application/services.ts</Path>", "<Path>frontend/apps/admin-web/src/router/adminManifestRegistry.ts</Path>", "<Path>frontend/pnpm-lock.yaml</Path>"]
shared_path_owners: ["<Path>frontend/apps/admin-web/package.json</Path> => T-02", "<Path>frontend/apps/admin-web/src/application/services.ts</Path> => T-02", "<Path>frontend/apps/admin-web/src/router/adminManifestRegistry.ts</Path> => T-02", "<Path>frontend/pnpm-lock.yaml</Path> => T-02"]
---

# Ticket T-02: 退出 Snail AI 业务入口并保留 Java 占位

- **Ticket：** <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/ticket/02-retire-snail-ai-business-surface.md</Path>
- **总体 Map：** <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/tickets-map.md</Path>
- **Spec：** <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/spec.md</Path>
- **Evidence：** <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/evidence/T-02.md</Path>

Lead/implementation owner 按 Map → 适用项目 Skill → 当前 Ticket 的顺序读取；Map 是最低路由，frontmatter 是本票真实调用绑定。新触发的硬约束先由 Lead 解析、同步、校验。

## 1. 战略与来源

- **目标与可观察产出：** Admin 不再呈现聊天/控制台，也不访问旧注册桥；Java 两个 AI artifact 仅保留可构建占位。
- **来源：** AC-006, AC-007, AC-008；LOG-003/004/005/006；<Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/grounding.md</Path>。
- **当前事实：** 旧 domain 注册桥→Java SnailAiController→vendor client；前端 iframe带旧凭据。普通未知菜单仍保留诊断页，不能仅删 manifest 后任由旧 AI 项继续显示。
- **Planning Depth 原因：** 移除 HTTP 能力、前端导航/权限组合与第三方依赖，需要保护旧数据库菜单和其他功能。

## 2. 决策状态

### 已锁定决策

删除 SnailAI 专属 Controller、vendor starter和自动装配，保留两个 Maven artifact与现有full/core组装选择。清除AI页面/domain服务/客户端依赖。Admin只过滤明确退役组件键 ai/chat/index 与 monitor/snailai/index，不隐藏其他未知键；旧菜单可留库但不会注册/呈现失效功能。保留Client裁剪、身份恢复顺序与无关诊断合同。SnailJob、Spring AI BOM/common-mcp及NAMEWTA OpenAPI不属于删除目标。

### 已采用的低影响假设

沿用现有命名、错误映射与测试接缝；frontmatter 中尚不存在的测试/纯policy路径是允许的新增落点，不要求机械创建。没有价值的抽象不新增。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 本票可观察产出、必要源码/测试/局部说明 | 既有Client/RBAC、错误、事务、模块与工具合同 | Go/Python、真实环境数据修改、部署、推送；其他票独占共享文件 |

## 4. 要构建什么

Admin 不再呈现聊天/控制台，也不访问旧注册桥；Java 两个 AI artifact 仅保留可构建占位。 入口和失败结果遵守Spec对应AC。每个失败必须能够由调用者或测试观察，不能只扫描内部符号。

## 5. 实现契约

删除 SnailAI 专属 Controller、vendor starter和自动装配，保留两个 Maven artifact与现有full/core组装选择。清除AI页面/domain服务/客户端依赖。Admin只过滤明确退役组件键 ai/chat/index 与 monitor/snailai/index，不隐藏其他未知键；旧菜单可留库但不会注册/呈现失效功能。保留Client裁剪、身份恢复顺序与无关诊断合同。SnailJob、Spring AI BOM/common-mcp及NAMEWTA OpenAPI不属于删除目标。

- **安全与兼容：** 不改变无关Client/权限/密码策略；不得把手机号或旧vendor凭据写进新日志。公共Java签名、配置所有权和数据保留按Spec约束。
- **失败与状态：** 本票验收失败不标Done，不开始依赖它的票；不通过空响应、吞错、删测试、放宽规则获得绿色。

## 6. 执行路线

1. 建立旧桥未映射、旧AI菜单不呈现、无关未知菜单仍诊断的退出合同，复用真实manifest/navigation接缝。
2. 退出所有前端/Java业务调用者与iframe；两个Java模块保留有明确README说明的无vendor实现占位，验证full/core角色不变。
3. 同步Admin组合、monitor target、受管应用配置与依赖锁；受管且起始干净的 application-local.yml 仅移除 Snail AI 配置段，其他内容保留。非受管本机配置、秘密和用户已有改动不得改动或提交。
4. 只对两条退役AI键处理存量菜单，测试不破坏无关路由、权限和状态恢复。
5. 运行应用层退出与构建回归，记录仍待T-03退出的独立server、根dependencyManagement、SQL菜单和发布资产，不能提前宣称全链退出。

## 7. 路径访问契约

frontmatter 列出唯一可写集和共享owner；预计修改点仅导航。依赖/缓存、target、dist、node_modules、非受管本机配置、私有.env、历史不可变release、旧OpenAPI revisions和其他change均不属于可写源。T-03的release资产广域授权仅允许已确认的SnailAI退出闭包、相关fixture和文档，禁止顺手重构；新发现范围先修订精确写集。

受管 application-local.yml 的 writable 授权仅限 Snail AI 段；其他 local 配置内容不在本票可写范围。

T-01/T-02不得修改T-03拥有的API生成物或SQL；交回有来源的合同差异。T-02独占前端lock及Admin组合。相邻测试需要超出本票写集时先由Lead更新owner/Map，不直接越界。

## 8. 验证矩阵

| 行为或风险 | 命令或步骤与cwd | 预期结果 | Evidence |
|---|---|---|---|
| 退出桥和占位 | backend: ./mvnw -pl wta-admin -am test -Dtest=*SnailAi*Test,*AiRetirement*Test -Dsurefire.failIfNoSpecifiedTests=false；./mvnw -pl wta-admin -am -DskipTests package | 必须实际执行新增退出合同，旧桥不映射；保留业务应用不要求AI配置 | <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/evidence/T-02.md</Path> |
| 导航/依赖 | frontend: corepack pnpm architecture:check；corepack pnpm architecture:test；corepack pnpm test；corepack pnpm lint；corepack pnpm typecheck；corepack pnpm build:prod | 旧AI菜单隐藏；非AI未知键仍诊断；依赖和锁一致 | <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/evidence/T-02.md</Path> |
| 浏览器旧菜单 | frontend: corepack pnpm test:e2e snail-ai-retirement.spec.ts --workers=1 | Lead检查新/旧菜单响应，禁止发旧注册或iframe请求；Client与导航恢复负向保持 | <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/evidence/T-02.md</Path> |

上述命令的实际执行与最终验收见 Evidence。frontend cwd固定使用corepack pnpm 10.34.5（已实测）；系统默认pnpm 9不能作为执行入口。精确新增测试名在实现形成后冻结并重验实际发现数；Maven reactor上无匹配模块可跳过，但负责该合同的模块不得零测试。

- **Workspace checks：** current-workspace执行非E2E检查；父分支main，单writer串行。
- **E2E disposition：** required：真实Admin导航恢复与历史菜单响应；Lead在current-workspace观察不呈现旧功能、没有旧请求/iframe。
- **Integration evidence：** 非空implementation/source commit、parent_before、direct-parent验证、result SHA与包含关系；当前策略candidate字段不适用。

## 9. 发布、迁移与恢复

- **迁移顺序/兼容窗口：** 先退出消费者与业务自动配置，再由T-03退出独立server及共享SQL/发布资产。旧生成OpenAPI版本可保留作历史来源；T-03统一更新当前版本，禁止手改旧快照。删除退出功能专属测试前须有实际运行的负向替代合同，不靠删测试求绿。
- **监控/回滚/前向恢复：** 本票可回退到原应用消费者版本；历史数据与服务尚未处理。观察导航诊断、旧URL请求和启动依赖；发生无关菜单被隐藏立即回退精确过滤改动。
- **不可逆操作与批准点：** 实现、commit、父分支推进、push、正式发布、部署、生产数据操作分别核对真实授权；用户已激活 I 并授权本地实施，本地提交与 current/direct-parent 验收已按 LOG-011 获用户授权。未授权的提交和外部动作不执行。
- **收缩条件：** 本票退出/校验合同全部通过；最终共享合同收缩和完整产物由T-03及整体Gate负责，其他票不得提前声明全change交付。

## 10. 验收标准

- [x] AC-006：Spec对应可观察结果由本票验证矩阵证明。
- [x] AC-007：Spec对应可观察结果由本票验证矩阵证明。
- [x] AC-008：Spec对应可观察结果由本票验证矩阵证明。
- [x] 已按Map→Skill→Ticket调用真实项目Skill，并在Evidence记录匹配phase/operation/hash。
- [x] 正常、失败、回归与E2E实际执行，含cwd、版本、命令、退出码、用例/skip数量及失败分类。
- [x] 所有写入在授权路径，shared path由指定owner修改。
- [x] 已获相应执行授权并形成非空implementation commit；Lead的current-workspace direct-parent验收通过，父分支result可回读。
- [x] 未完成项/残余风险如实记录；不把未实施、无修改或仅Evidence票标Done。

## 11. SKILL 调用计划

- engineering-standards / verify / verify-scope-contracts-and-quality-gates：输入为本票Spec、实际源码及diff；执行入口定义的范围检查/实现或验证步骤，输出合同/依赖映射与可回读证据；失败固定block-ticket。
- namewta-fullstack-development / implement / implement-vertical-slice-and-verify-boundaries：输入为本票Spec、实际源码及diff；执行入口定义的范围检查/实现或验证步骤，输出合同/依赖映射与可回读证据；失败固定block-ticket。
- wta-module-guide / implement / resolve-module-owner-and-public-entrypoints：输入为本票Spec、实际源码及diff；执行入口定义的范围检查/实现或验证步骤，输出合同/依赖映射与可回读证据；失败固定block-ticket。
- wta-common-modules-guide / implement / resolve-common-validation-and-vendor-dependency-boundaries：输入为本票Spec、实际源码及diff；执行入口定义的范围检查/实现或验证步骤，输出合同/依赖映射与可回读证据；失败固定block-ticket。

engineering-standards入口同时在开工前完整读取并按scope加载规则；verify绑定代表完成门禁，不替代开工规范。命中菜单读取fullstack permission-routing、contract-mapping、mapper-and-sql；命中事务/SQL读取工程persistence-transactions-and-ddl。Skill自身由后续票修改时Lead先核对事实、更新绑定摘要再验证，不静默用旧摘要。

## 12. 停止、检查点与交付

- **用户交付要求与数量：** 本change交付手机号必填与SnailAI退出两项行为；Java占位模块恰好2个，SQL基座保持6份；Ticket数量不是额外业务产物数量。
- **必需Skill/引用/命令缺失：** 停止受影响票，保留错误与恢复条件，不降低门禁。
- **归属/环境冲突：** 不接管其他change或本地配置；只阻塞相交资源与依赖闭包。
- **检查点：** 保存base/实际写集/Skill摘要/完成步骤/非E2E结果/待Lead验收，后续恢复先回读真实源。
- **完成出口：** 按Goal Plan的current/direct-parent验收回到总控；commit或required验证缺失则不Done。
