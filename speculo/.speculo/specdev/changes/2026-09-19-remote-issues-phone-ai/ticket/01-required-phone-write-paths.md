---
schema_version: 3
plan_contract_version: 1
skill_scan: "已枚举项目唯一根 .agents/skills 下7个真实入口；按账号/AI退出/构建scope绑定匹配项，未使用Speculo Skill伪装项目Skill。"
skill_bindings: [{"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "verify", "operation": "verify-scope-contracts-and-quality-gates", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/spec.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/ticket/01-required-phone-write-paths.md</Path>", "当前Ticket diff与已核验源码/调用方"], "outputs": ["本票范围和调用映射", "实际执行记录、验证命令/退出码及停止点"], "required": true, "on_failure": "block-ticket", "references": []}, {"id": "namewta-fullstack-development", "path": "<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>", "sha256": "675c053c11d8b22cd394c875f48688242d8e8328dd14e69657f5d5b12f2af68b", "phase": "implement", "operation": "implement-vertical-slice-and-verify-boundaries", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/spec.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/ticket/01-required-phone-write-paths.md</Path>", "当前Ticket diff与已核验源码/调用方"], "outputs": ["本票范围和调用映射", "实际执行记录、验证命令/退出码及停止点"], "required": true, "on_failure": "block-ticket", "references": []}, {"id": "java-api-compatibility", "path": "<Path>.agents/skills/java-api-compatibility/SKILL.md</Path>", "sha256": "b90f5592e75b3f757f52649a16f78e92850fee0ccac9f12619d3d7aa94bd7aca", "phase": "implement", "operation": "preserve-public-signatures-and-review-validation-evolution", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/spec.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/ticket/01-required-phone-write-paths.md</Path>", "当前Ticket diff与已核验源码/调用方"], "outputs": ["本票范围和调用映射", "实际执行记录、验证命令/退出码及停止点"], "required": true, "on_failure": "block-ticket", "references": []}, {"id": "wta-module-guide", "path": "<Path>.agents/skills/wta-module-guide/SKILL.md</Path>", "sha256": "bb57a781314abe316f06ba9538f62043f1bd053904c0832969e575c09731a7a3", "phase": "implement", "operation": "resolve-module-owner-and-public-entrypoints", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/spec.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/ticket/01-required-phone-write-paths.md</Path>", "当前Ticket diff与已核验源码/调用方"], "outputs": ["本票范围和调用映射", "实际执行记录、验证命令/退出码及停止点"], "required": true, "on_failure": "block-ticket", "references": []}, {"id": "wta-common-modules-guide", "path": "<Path>.agents/skills/wta-common-modules-guide/SKILL.md</Path>", "sha256": "e92775ce47af41bd33c1b3293d7f8a3185fdd739b9b588519e043c654f678d98", "phase": "implement", "operation": "resolve-common-validation-and-vendor-dependency-boundaries", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/spec.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/ticket/01-required-phone-write-paths.md</Path>", "当前Ticket diff与已核验源码/调用方"], "outputs": ["本票范围和调用映射", "实际执行记录、验证命令/退出码及停止点"], "required": true, "on_failure": "block-ticket", "references": []}]
resource_claims: ["account-phone-write-contract", "registration-http-domain-contract"]
artifact: "ticket"
change: "2026-09-19-remote-issues-phone-ai"
id: "T-01"
title: "手机号在注册与资料写入时必填，保留存量登录"
status: "in_progress"
kind: "bug"
planning_depth: "deep"
planning_depth_reason: "涉及公开 DTO/HTTP、资料部分更新、导入持久化、权限与敏感字段。"
ready: true
risk: "high"
blocked_by: []
contract_ids: ["AC-001", "AC-002", "AC-003", "AC-004", "AC-005"]
owner: "codex-root"
expected_changes: ["<Path>backend/wta-api/src/main/java/org/namewta/system/api/model/RegisterBody.java</Path>", "<Path>backend/wta-admin/src/main/java/org/namewta/web/service/SysRegisterService.java</Path>", "<Path>backend/wta-admin/src/main/java/org/namewta/web/controller/AuthController.java</Path>"]
writable_paths: ["<Path>backend/wta-api/src/main/java/org/namewta/system/api/model/RegisterBody.java</Path>", "<Path>backend/wta-admin/src/main/java/org/namewta/web/service/SysRegisterService.java</Path>", "<Path>backend/wta-admin/src/main/java/org/namewta/web/controller/AuthController.java</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/domain/bo/SysUserBo.java</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/domain/bo/SysUserProfileBo.java</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/controller/system/SysUserController.java</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/controller/system/SysProfileController.java</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/service/impl/SysUserServiceImpl.java</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/listener/SysUserImportListener.java</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/domain/policy/UserPhonePolicy.java</Path>", "<Path>backend/wta-modules/wta-system/src/test/java/org/namewta/system/phone/**</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/web/service/SysRegisterServiceRegistrationUnitTest.java</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/password/write/**</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/phone/**</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/security/BrowserHttpsTransportIntegrationTest.java</Path>", "<Path>frontend/packages/domains/admin/src/index.ts</Path>", "<Path>frontend/packages/domains/admin/src/index.test.ts</Path>", "<Path>frontend/packages/domains/system/src/user/**</Path>", "<Path>frontend/packages/domains/system/src/profile/**</Path>", "<Path>frontend/packages/web-domains/system/src/user/**</Path>", "<Path>frontend/apps/admin-web/src/views/register.vue</Path>", "<Path>frontend/apps/admin-web/src/views/system/user/profile/**</Path>", "<Path>frontend/apps/home-web/src/views/RegisterPage.vue</Path>", "<Path>frontend/apps/home-web/src/views/RegisterPage.test.ts</Path>", "<Path>frontend/e2e/recoverable-registration.spec.ts</Path>", "<Path>frontend/e2e/browser-https-transport.spec.ts</Path>", "<Path>frontend/e2e/app-runtime-baseline.spec.ts</Path>", "<Path>frontend/e2e/client-auth-context.spec.ts</Path>", "<Path>frontend/e2e/public-accessibility.spec.ts</Path>", "<Path>frontend/e2e/required-phone.spec.ts</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/oss/owner/system/SystemUserAvatarOssOwnerUnitTest.java</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/web/service/impl/PasswordAuthStrategyTemporaryUnitTest.java</Path>", "<Path>frontend/playwright.phone.config.ts</Path>", "<Path>frontend/playwright.config.ts</Path>"]
read_only_paths: ["<Path>AGENTS.md</Path>", "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "<Path>backend/wta-common/wta-common-core/src/main/java/org/namewta/common/core/validation/ValidationUtils.java</Path>", "<Path>frontend/packages/api-contracts/**</Path>", "<Path>release-artifacts/**</Path>"]
shared_paths: ["<Path>backend/wta-api/src/main/java/org/namewta/system/api/model/RegisterBody.java</Path>", "<Path>frontend/packages/domains/admin/src/index.ts</Path>"]
shared_path_owners: ["<Path>backend/wta-api/src/main/java/org/namewta/system/api/model/RegisterBody.java</Path> => T-01", "<Path>frontend/packages/domains/admin/src/index.ts</Path> => T-01"]
---

# Ticket T-01: 手机号在注册与资料写入时必填，保留存量登录

- **Ticket：** <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/ticket/01-required-phone-write-paths.md</Path>
- **总体 Map：** <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/tickets-map.md</Path>
- **Spec：** <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/spec.md</Path>
- **Evidence：** <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/evidence/T-01.md</Path>

Lead/implementation owner 按 Map → 适用项目 Skill → 当前 Ticket 的顺序读取；Map 是最低路由，frontmatter 是本票真实调用绑定。新触发的硬约束先由 Lead 解析、同步、校验。

## 1. 战略与来源

- **目标与可观察产出：** 注册、管理新增/编辑、个人资料与新增/覆盖导入形成一致手机号写入合同，旧空号用户仍可登录。
- **来源：** AC-001, AC-002, AC-003, AC-004, AC-005；LOG-003/004/005/006；<Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/grounding.md</Path>。
- **当前事实：** 公开注册两端无手机号表单且 domain 映射不传字段；BO 对空值放行；导入复用同一 BO；资料 null/省略不更新而空串可写空。详见 Grounding 的手机号表。
- **Planning Depth 原因：** 涉及公开 DTO/HTTP、资料部分更新、导入持久化、权限与敏感字段。

## 2. 决策状态

### 已锁定决策

新增必须有有效手机号；更新先读取已授权目标，按 null/省略保留旧值的现有语义合并，再保证最终号码有效。空串或纯空白拒绝；拒绝不得修改其他资料。非空值标准化、格式检查、唯一性检查与最终写入必须使用同一号码。保持现有大陆格式与各入口唯一性策略，不新增索引或全量补录；独立登录、密码、禁用操作不扩大约束。

### 已采用的低影响假设

沿用现有命名、错误映射与测试接缝；frontmatter 中尚不存在的测试/纯policy路径是允许的新增落点，不要求机械创建。没有价值的抽象不新增。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 本票可观察产出、必要源码/测试/局部说明 | 既有Client/RBAC、错误、事务、模块与工具合同 | Go/Python、真实环境数据修改、部署、推送；其他票独占共享文件 |

## 4. 要构建什么

注册、管理新增/编辑、个人资料与新增/覆盖导入形成一致手机号写入合同，旧空号用户仍可登录。 入口和失败结果遵守Spec对应AC。每个失败必须能够由调用者或测试观察，不能只扫描内部符号。

## 5. 实现契约

新增必须有有效手机号；更新先读取已授权目标，按 null/省略保留旧值的现有语义合并，再保证最终号码有效。空串或纯空白拒绝；拒绝不得修改其他资料。非空值标准化、格式检查、唯一性检查与最终写入必须使用同一号码。保持现有大陆格式与各入口唯一性策略，不新增索引或全量补录；独立登录、密码、禁用操作不扩大约束。

- **安全与兼容：** 不改变无关Client/权限/密码策略；不得把手机号或旧vendor凭据写进新日志。公共Java签名、配置所有权和数据保留按Spec约束。
- **失败与状态：** 本票验收失败不标Done，不开始依赖它的票；不通过空响应、吞错、删测试、放宽规则获得绿色。

## 6. 执行路线

1. 用真实 Bean Validation、导入 listener 和既有注册接缝建立红灯矩阵，覆盖新建及原空/原有效两类更新。
2. 保持 Java 公开字段与签名，分别处理新增验证和更新合并；复用 Service/校验机制，不能给共享 BO 简单全局 @NotBlank 误伤部分更新。
3. 同步 Admin/Home 表单、domain 显式 HTTP 映射、管理/个人页面规则和相应成功 fixture；保护失败后的输入和安全错误。
4. 验证注册开关、密码/验证码、当前 Client 与导入失败不落库；任何新增 @Log 使用现有脱敏策略。
5. 执行后端/前端定向与必要回归，Lead 做浏览器验收。向 T-03 交回最终 API 约束与初始化账号 fixture 要求，不修改其共享文件。

## 7. 路径访问契约

frontmatter 列出唯一可写集和共享owner；预计修改点仅导航。依赖/缓存、target、dist、node_modules、application-local.yml、.env、历史不可变release、旧OpenAPI revisions和其他change均不属于可写源。T-03的release资产广域授权仅允许已确认的SnailAI退出闭包、相关fixture和文档，禁止顺手重构；新发现范围先修订精确写集。

T-01/T-02不得修改T-03拥有的API生成物或SQL；交回有来源的合同差异。T-02独占前端lock及Admin组合。相邻测试需要超出本票写集时先由Lead更新owner/Map，不直接越界。

## 8. 验证矩阵

| 行为或风险 | 命令或步骤与cwd | 预期结果 | Evidence |
|---|---|---|---|
| 手机号新建/更新/导入 | backend: ./mvnw -pl wta-admin -am test -Dtest=SysRegisterServiceRegistrationUnitTest,PasswordImportUnitTest,PasswordWritePathUnitTest,*Phone*Test -Dsurefire.failIfNoSpecifiedTests=false | 真实校验器；合法/非法、省略/null、原空/原有效矩阵；受影响模块测试必须实际发现并执行，零匹配不是通过 | <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/evidence/T-01.md</Path> |
| HTTP与页面传输 | frontend: corepack pnpm test；corepack pnpm typecheck；corepack pnpm lint；corepack pnpm build:prod | 两端输入映射一致；相关页面/domain测试通过，类型与构建无回归 | <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/evidence/T-01.md</Path> |
| 身份与登录回归 | backend: ./mvnw test；frontend: corepack pnpm exec playwright test --config playwright.phone.config.ts；corepack pnpm exec playwright test --config playwright.registration.config.ts（任务自有HTTPS双App）；corepack pnpm exec playwright test --config playwright.transport.config.ts（真实Spring/HTTPS）；corepack pnpm test:e2e client-auth-context.spec.ts --workers=1 | 旧空号仍登录；注册开关、Token失效、默认/显式角色、多Client/缺clientPk/越权负向场景保持 | <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/evidence/T-01.md</Path> |

实际执行、结果与未验证项见本票 Evidence。frontend cwd固定使用corepack pnpm 10.34.5（已实测）；系统默认pnpm 9不能作为执行入口。精确新增测试名在实现形成后冻结并重验实际发现数；Maven reactor上无匹配模块可跳过，但负责该合同的模块不得零测试。

- **Workspace checks：** current-workspace执行非E2E检查；父分支main，单writer串行。
- **E2E disposition：** required：Admin/Home 注册与资料保存跨 UI、domain、HTTP；Lead 在 current-workspace 运行成功、失败恢复、旧用户登录场景。
- **Integration evidence：** 非空implementation/source commit、parent_before、direct-parent验证、result SHA与包含关系；当前策略candidate字段不适用。

## 9. 发布、迁移与恢复

- **迁移顺序/兼容窗口：** 同票原子切换服务端约束与仓内调用者，登录兼容保留。当前 DTO 签名不变；旧 OpenAPI 不可变快照只读，最终当前指针/生成物由 T-03 据最终后端 commit 统一刷新。T-03 前不是可发布的最终 change。
- **监控/回滚/前向恢复：** 回退本票的服务端/客户端成对变更并保留新填写号码，不回滚用户数据；观察字段错误、导入失败和登录回归。新旧前端混版不作为兼容保证。
- **不可逆操作与批准点：** 实现、commit、父分支推进、push、正式发布、部署、生产数据操作分别核对真实授权；用户已激活 I 并授权本地实施，提交/集成方案待确认。未授权的提交和外部动作不执行。
- **收缩条件：** 本票退出/校验合同全部通过；最终共享合同收缩和完整产物由T-03及整体Gate负责，其他票不得提前声明全change交付。

## 10. 验收标准

- [ ] AC-001：Spec对应可观察结果由本票验证矩阵证明。
- [ ] AC-002：Spec对应可观察结果由本票验证矩阵证明。
- [ ] AC-003：Spec对应可观察结果由本票验证矩阵证明。
- [ ] AC-004：Spec对应可观察结果由本票验证矩阵证明。
- [ ] AC-005：Spec对应可观察结果由本票验证矩阵证明。
- [ ] 已按Map→Skill→Ticket调用真实项目Skill，并在Evidence记录匹配phase/operation/hash。
- [ ] 正常、失败、回归与E2E实际执行，含cwd、版本、命令、退出码、用例/skip数量及失败分类。
- [ ] 所有写入在授权路径，shared path由指定owner修改。
- [ ] 已获相应执行授权并形成非空implementation commit；Lead的current-workspace direct-parent验收通过，父分支result可回读。
- [ ] 未完成项/残余风险如实记录；不把未实施、无修改或仅Evidence票标Done。

## 11. SKILL 调用计划

- engineering-standards / verify / verify-scope-contracts-and-quality-gates：输入为本票Spec、实际源码及diff；执行入口定义的范围检查/实现或验证步骤，输出合同/依赖映射与可回读证据；失败固定block-ticket。
- namewta-fullstack-development / implement / implement-vertical-slice-and-verify-boundaries：输入为本票Spec、实际源码及diff；执行入口定义的范围检查/实现或验证步骤，输出合同/依赖映射与可回读证据；失败固定block-ticket。
- java-api-compatibility / implement / preserve-public-signatures-and-review-validation-evolution：输入为本票Spec、实际源码及diff；执行入口定义的范围检查/实现或验证步骤，输出合同/依赖映射与可回读证据；失败固定block-ticket。
- wta-module-guide / implement / resolve-module-owner-and-public-entrypoints：输入为本票Spec、实际源码及diff；执行入口定义的范围检查/实现或验证步骤，输出合同/依赖映射与可回读证据；失败固定block-ticket。
- wta-common-modules-guide / implement / resolve-common-validation-and-vendor-dependency-boundaries：输入为本票Spec、实际源码及diff；执行入口定义的范围检查/实现或验证步骤，输出合同/依赖映射与可回读证据；失败固定block-ticket。

engineering-standards入口同时在开工前完整读取并按scope加载规则；verify绑定代表完成门禁，不替代开工规范。命中菜单读取fullstack permission-routing、contract-mapping、mapper-and-sql；命中事务/SQL读取工程persistence-transactions-and-ddl。Skill自身由后续票修改时Lead先核对事实、更新绑定摘要再验证，不静默用旧摘要。

## 12. 停止、检查点与交付

- **用户交付要求与数量：** 本change交付手机号必填与SnailAI退出两项行为；Java占位模块恰好2个，SQL基座保持6份；Ticket数量不是额外业务产物数量。
- **必需Skill/引用/命令缺失：** 停止受影响票，保留错误与恢复条件，不降低门禁。
- **归属/环境冲突：** 不接管其他change或本地配置；只阻塞相交资源与依赖闭包。
- **检查点：** 保存base/实际写集/Skill摘要/完成步骤/非E2E结果/待Lead验收，后续恢复先回读真实源。
- **完成出口：** 按Goal Plan的current/direct-parent验收回到总控；commit或required验证缺失则不Done。
