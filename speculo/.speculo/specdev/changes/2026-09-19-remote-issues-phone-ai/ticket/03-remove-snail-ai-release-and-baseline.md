---
schema_version: 3
plan_contract_version: 1
skill_scan: "已枚举项目唯一根 .agents/skills 下7个真实入口；按账号/AI退出/构建scope绑定匹配项，未使用Speculo Skill伪装项目Skill。"
skill_bindings: [{"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "verify", "operation": "verify-scope-contracts-and-quality-gates", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/spec.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/ticket/03-remove-snail-ai-release-and-baseline.md</Path>", "当前Ticket diff与已核验源码/调用方"], "outputs": ["本票范围和调用映射", "实际执行记录、验证命令/退出码及停止点"], "required": true, "on_failure": "block-ticket", "references": []}, {"id": "namewta-fullstack-development", "path": "<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>", "sha256": "675c053c11d8b22cd394c875f48688242d8e8328dd14e69657f5d5b12f2af68b", "phase": "implement", "operation": "implement-vertical-slice-and-verify-boundaries", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/spec.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/ticket/03-remove-snail-ai-release-and-baseline.md</Path>", "当前Ticket diff与已核验源码/调用方"], "outputs": ["本票范围和调用映射", "实际执行记录、验证命令/退出码及停止点"], "required": true, "on_failure": "block-ticket", "references": []}, {"id": "wta-module-guide", "path": "<Path>.agents/skills/wta-module-guide/SKILL.md</Path>", "sha256": "bb57a781314abe316f06ba9538f62043f1bd053904c0832969e575c09731a7a3", "phase": "implement", "operation": "resolve-module-owner-and-public-entrypoints", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/spec.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/ticket/03-remove-snail-ai-release-and-baseline.md</Path>", "当前Ticket diff与已核验源码/调用方"], "outputs": ["本票范围和调用映射", "实际执行记录、验证命令/退出码及停止点"], "required": true, "on_failure": "block-ticket", "references": []}, {"id": "wta-common-modules-guide", "path": "<Path>.agents/skills/wta-common-modules-guide/SKILL.md</Path>", "sha256": "d7b7e105c37499e0e0f8ad9b1e2dbf379100df5b6a47e0d6affb04483f8b162c", "phase": "implement", "operation": "resolve-common-validation-and-vendor-dependency-boundaries", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/spec.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/ticket/03-remove-snail-ai-release-and-baseline.md</Path>", "当前Ticket diff与已核验源码/调用方"], "outputs": ["本票范围和调用映射", "实际执行记录、验证命令/退出码及停止点"], "required": true, "on_failure": "block-ticket", "references": []}]
resource_claims: ["backend-root-dependency-management", "release-backend-inventory", "mysql-six-file-baseline", "openapi-current-projection", "project-skill-facts"]
artifact: "ticket"
change: "2026-09-19-remote-issues-phone-ai"
id: "T-03"
title: "退出 Snail AI 服务与发布资产并验证最终候选"
status: "done"
kind: "refactor"
planning_depth: "deep"
planning_depth_reason: "独立运行面退出涉及构建、不可变产物manifest、六份SQL与历史数据保护，以及两票共享生成合同。"
ready: false
risk: "high"
blocked_by: ["T-01", "T-02"]
contract_ids: ["AC-009", "AC-010", "AC-011", "AC-012"]
owner: "codex-root"
expected_changes: ["<Path>backend/pom.xml</Path>", "<Path>backend/wta-extend/pom.xml</Path>", "<Path>backend/wta-extend/AGENTS.md</Path>"]
writable_paths: ["<Path>backend/pom.xml</Path>", "<Path>backend/wta-extend/pom.xml</Path>", "<Path>backend/wta-extend/AGENTS.md</Path>", "<Path>backend/wta-extend/wta-snailai-server/**</Path>", "<Path>backend/wta-admin/Dockerfile</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/security/TrustedClientAddressIntegrationTest.java</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/airetirement/**</Path>", "<Path>release-artifacts/**</Path>", "<Path>scripts/ci/verify-admin-bundle.sh</Path>", "<Path>scripts/ci/run-external-services.sh</Path>", "<Path>frontend/packages/api-contracts/**</Path>", "<Path>frontend/tooling/openapi/README.md</Path>", "<Path>README.md</Path>", "<Path>backend/README.md</Path>", "<Path>.agents/skills/engineering-standards/references/project/00-project-profile.md</Path>", "<Path>.agents/skills/engineering-standards/references/project/01-module-map.md</Path>", "<Path>.agents/skills/engineering-standards/references/project/03-backend-module-modes.md</Path>", "<Path>.agents/skills/engineering-standards/scripts/validate-skill-facts.mjs</Path>", "<Path>.agents/skills/engineering-standards/scripts/validate-skill-facts.test.mjs</Path>", "<Path>.agents/skills/wta-common-modules-guide/SKILL.md</Path>", "<Path>.agents/skills/wta-common-modules-guide/references/module-map.md</Path>", "<Path>.agents/skills/namewta-fullstack-development/references/backend/mapper-and-sql.md</Path>", "<Path>.agents/skills/engineering-standards/references/java/persistence-transactions-and-ddl.md</Path>", "<Path>.agents/skills/namewta-fullstack-development/references/backend/architecture.md</Path>", "<Path>.agents/skills/wta-module-guide/references/modules/system/how-other-modules-call.md</Path>", "<Path>.agents/skills/deploy-namewta-environment/assets/templates/admin-web.env.production.local.template</Path>", "<Path>.agents/skills/deploy-namewta-environment/assets/templates/admin-web.env.development.local.template</Path>", "<Path>.agents/skills/deploy-namewta-environment/assets/templates/deployment-profile.json.template</Path>", "<Path>.agents/skills/deploy-namewta-environment/references/existing-site-takeover.md</Path>", "<Path>.agents/skills/deploy-namewta-environment/references/middleware-database-oss.md</Path>", "<Path>.agents/skills/deploy-namewta-environment/references/verification-and-troubleshooting.md</Path>", "<Path>.agents/skills/deploy-namewta-environment/scripts/render-local-config.mjs</Path>", "<Path>.agents/skills/deploy-namewta-environment/scripts/generate-deployment-report.mjs</Path>"]
read_only_paths: ["<Path>AGENTS.md</Path>", "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "<Path>backend/wta-common/wta-common-core/src/main/java/org/namewta/common/core/validation/ValidationUtils.java</Path>"]
shared_paths: ["<Path>backend/pom.xml</Path>", "<Path>release-artifacts/**</Path>", "<Path>frontend/packages/api-contracts/**</Path>", "<Path>.agents/skills/engineering-standards/references/project/00-project-profile.md</Path>", "<Path>.agents/skills/engineering-standards/references/project/01-module-map.md</Path>", "<Path>.agents/skills/wta-common-modules-guide/SKILL.md</Path>"]
shared_path_owners: ["<Path>backend/pom.xml</Path> => T-03", "<Path>release-artifacts/**</Path> => T-03", "<Path>frontend/packages/api-contracts/**</Path> => T-03", "<Path>.agents/skills/engineering-standards/references/project/00-project-profile.md</Path> => T-03", "<Path>.agents/skills/engineering-standards/references/project/01-module-map.md</Path> => T-03", "<Path>.agents/skills/wta-common-modules-guide/SKILL.md</Path> => T-03"]
---

# Ticket T-03: 退出 Snail AI 服务与发布资产并验证最终候选

- **Ticket：** <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/ticket/03-remove-snail-ai-release-and-baseline.md</Path>
- **总体 Map：** <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/tickets-map.md</Path>
- **Spec：** <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/spec.md</Path>
- **Evidence：** <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/evidence/T-03.md</Path>

Lead/implementation owner 按 Map → 适用项目 Skill → 当前 Ticket 的顺序读取；Map 是最低路由，frontmatter 是本票真实调用绑定。新触发的硬约束先由 Lead 解析、同步、校验。

## 1. 战略与来源

- **目标与可观察产出：** 最终源码与本地发布候选不产出或启动SnailAI；新库无vendor表，旧数据原样保留，当前API/基座/文档与两个前置切片一致。
- **来源：** AC-009, AC-010, AC-011, AC-012；LOG-003/004/005/006；<Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/grounding.md</Path>。
- **规划基线事实（已由本票收缩）：** release-state原固定四个Java JAR和六个SQL，原AI基座有23张sai_*表；最终保留三个Java应用、六SQL，40为合法无vendor占位。新旧应用入口已由前置票退出，才能安全收缩独立server。
- **Planning Depth 原因：** 独立运行面退出涉及构建、不可变产物manifest、六份SQL与历史数据保护，以及两票共享生成合同。

## 2. 决策状态

### 已锁定决策

删除SnailAI server源码/聚合、根vendor版本和managed starters；release库存变为真实仍存在的Java应用，不简化来源/摘要/必需文件验证。移除Compose/Nginx/upstream/回调端口/env/日志接入和仅供AI使用的外部处理配置。40-cde-ai.sql保留仅SET NAMES utf8mb4;，六文件和其它基座完整；50文件统一删除旧AI初始化菜单并补齐新建示例手机号。已有运行库与历史release/资源不操作。正式抓取最终后端commit对应OpenAPI并生成当前transport，保护历史版本。

### 已采用的低影响假设

沿用现有命名、错误映射与测试接缝；frontmatter 中尚不存在的测试/纯policy路径是允许的新增落点，不要求机械创建。没有价值的抽象不新增。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 本票可观察产出、必要源码/测试/局部说明 | 既有Client/RBAC、错误、事务、模块与工具合同 | Go/Python、真实环境数据修改、部署、推送；其他票独占共享文件 |

## 4. 要构建什么

最终源码与本地发布候选不产出或启动SnailAI；新库无vendor表，旧数据原样保留，当前API/基座/文档与两个前置切片一致。 入口和失败结果遵守Spec对应AC。每个失败必须能够由调用者或测试观察，不能只扫描内部符号。

## 5. 实现契约

删除SnailAI server源码/聚合、根vendor版本和managed starters；release库存变为真实仍存在的Java应用，不简化来源/摘要/必需文件验证。移除Compose/Nginx/upstream/回调端口/env/日志接入和仅供AI使用的外部处理配置。40-cde-ai.sql保留仅SET NAMES utf8mb4;，六文件和其它基座完整；50文件统一删除旧AI初始化菜单并补齐新建示例手机号。已有运行库与历史release/资源不操作。正式抓取最终后端commit对应OpenAPI并生成当前transport，保护历史版本。

- **安全与兼容：** 不改变无关Client/权限/密码策略；不得把手机号或旧vendor凭据写进新日志。公共Java签名、配置所有权和数据保留按Spec约束。
- **失败与状态：** 本票验收失败不标Done，不开始依赖它的票；不通过空响应、吞错、删测试、放宽规则获得绿色。

## 6. 执行路线

1. 从已验收T-01/T-02父checkpoint重建完整退出清单、共享变更输入和保护清单；先读取实际release/SQL测试，固定不削弱的来源与回滚合同。
2. 退出独立server和所有受管构建/部署接入，保留SnailJob、Monitor、MCP、NAMEWTA OpenAPI；按真实应用列表更新manifest库存和精确负向断言。
3. 统一修改40/50基座；更新所有依赖实际表数的初始化器/fixtures为真实集合，保留权限、SQL分类与受保护初始化检查；不生成运行库DROP/DELETE迁移。
4. 据含已授权后端变更的真实commit与本地启动导出的OpenAPI，调用官方仓内fetch/generate/check；记录source摘要和backend commit，旧revisions不可改。
5. 同步项目画像/模块地图和命中的父Skill事实；技能自身摘要变化先由Lead审阅、刷新实际绑定再进入verify，不伪造旧hash。
6. 在实现提交已获授权且全仓受管修改及非忽略的未跟踪文件均为空后，按下述真实本地发布路线构建 prod/full/all 候选，保存实际版本与manifest并调用现有verify函数；缺任一条件则阻塞AC-009，不以fixture替代。
7. Lead使用隔离新MySQL和旧sai_*哨兵场景验证无创建/无清理；运行全套受影响构建、release和浏览器/真实服务门禁，核对同一最终候选。

## 7. 路径访问契约

frontmatter 列出唯一可写集和共享owner；预计修改点仅导航。依赖/缓存、target、dist、node_modules、T-02拥有的受管application-local.yml、非受管本机配置、私有.env、历史不可变release、旧OpenAPI revisions和其他change均不属于可写源。T-03的release资产广域授权仅允许已确认的SnailAI退出闭包、相关fixture和文档，禁止顺手重构；新发现范围先修订精确写集。

T-01/T-02不得修改T-03拥有的API生成物或SQL；交回有来源的合同差异。T-02独占前端lock及Admin组合。相邻测试需要超出本票写集时先由Lead更新owner/Map，不直接越界。

## 8. 验证矩阵

| 行为或风险 | 命令或步骤与cwd | 预期结果 | Evidence |
|---|---|---|---|
| 发布脚本与基座合同 | root: bash release-artifacts/scripts/verify-release.sh；node .agents/skills/engineering-standards/scripts/validate-skill-facts.mjs；node scripts/ci/verify-agent-handbooks.mjs | 合同测试通过但不代替真实构建；六个SQL合法，无sai_*初始化；禁止放宽manifest/安全断言 | <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/evidence/T-03.md</Path> |
| 生成与前后端 | frontend: corepack pnpm --filter @namewta/tooling-openapi openapi:fetch -- --source <已核验本地OpenAPI文件或URL> --backend-commit <最终后端40位commit>；openapi:generate；openapi:check（后两条同filter）；corepack pnpm lint；corepack pnpm typecheck；corepack pnpm test；corepack pnpm build:dev；corepack pnpm build:prod | 来源与实际后端一致；当前snapshot和generated匹配；所有消费者类型及构建通过，历史revisions不改 | <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/evidence/T-03.md</Path> |
| 最终后端双bundle | backend: ./mvnw test；./mvnw clean package -DskipTests，随后root: bash scripts/ci/verify-admin-bundle.sh full；backend: ./mvnw clean package -Pbundle-core -Dmaven.test.skip=true，随后root: bash scripts/ci/verify-admin-bundle.sh core | 测试实际执行；两包无SnailAI vendor依赖；full保留wta-ai/common-ai占位的既有可达性，core保持既有选择 | <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/evidence/T-03.md</Path> |
| 真实数据与集成 | root: bash scripts/ci/run-external-services.sh；扩展同一隔离harness的AiRetirement数据库用例；frontend: corepack pnpm test:e2e --workers=1 | 新库六文件初始化无sai_*；旧库哨兵内容前后摘要一致，不重放基座；全部required场景零skip并保存退出/登录证据 | <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/evidence/T-03.md</Path> |

### 真实本地发布候选（AC-009 必须执行）

前置条件：已取得实现、实现提交和本地 direct-parent 验收授权；当前 HEAD 包含三票最终源码及正式 OpenAPI 生成结果，Git 受管修改及非忽略的未跟踪文件均为空。原有用户改动未解决时不得擅自 stash、提交或删除，应保留为构建阻塞。release 构建器从此真实 commit 的 Git archive 构建，记录 40 位 source SHA、tree 与 archive 摘要。

1. 在系统临时目录用 `mktemp -d -t wta-phone-ai-release.XXXXXX` 创建任务目录，shell 变量名为 `wta_release_tmp`；其中 `bin/` 与 `release.env` 仅供本次本地构建，不作为源文件交付。先执行 `mkdir -p "$wta_release_tmp/bin"`，再用 `corepack enable --install-directory "$wta_release_tmp/bin" pnpm` 安装任务专用 shim，并仅对当前构建进程前置该目录到 PATH；在 frontend cwd 验证 `pnpm --version` 为 10.34.5。release 构建器会调用裸 pnpm，故不能仅在前台命令使用 corepack 后便省略此步骤。
2. 以 <Path>release-artifacts/.env.example</Path> 为模板创建权限 0600 的隔离 `release.env`，填写本地测试地址、隔离凭据与各注册 App 的合成 origin；不读取或复用生产/用户私有 .env。固定 env=prod、bundle=full、target=all，按现有 compose config 与环境检查要求补齐值。prod 是本地构建模式，未授权部署。
3. 从项目根执行 `PATH="$wta_release_tmp/bin:$PATH" bash release-artifacts/scripts/release-manage.sh build --target all --env prod --bundle full --env-file "$wta_release_tmp/release.env"`，记录退出码及 stdout 返回的真实版本 ID。此命令写入 <Path>release-artifacts/builds/versions/</Path> 下的本地不可变产物，不 stage、不切换 current、不启动服务。
4. 用 Python `importlib.util.spec_from_file_location` 加载 <Path>release-artifacts/scripts/release-state.py</Path>，对步骤3返回的目录调用实际存在的 `verify(Path("release-artifacts/builds/versions") / release_id, "prod")`，记录返回值和退出码；不存在 verify CLI，不调用虚构子命令，也不通过 stage/resolve 借道验证。核对真实 manifest 的来源/摘要、现存 Java 应用、两个占位、六个 SQL，以及不存在 SnailAI 产物/启动声明。
5. Evidence 保存版本 ID、源码 SHA、manifest 定位和摘要、构建/verify 退出码、工具链版本、负向库存检查，以及 current 未改变的前后观测。环境文件内容和凭据不进入 Evidence。构建失败保留失败位置及隔离临时目录定位；不得修改历史版本来补齐。

本节保留原执行计划；实际运行、命令/退出码和同源发布验收见 evidence/T-03.md，所有必需门禁已完成。frontend cwd固定使用corepack pnpm 10.34.5（已实测）；系统默认pnpm 9不能作为执行入口。精确新增测试名在实现形成后冻结并重验实际发现数；Maven reactor上无匹配模块可跳过，但负责该合同的模块不得零测试。

- **Workspace checks：** current-workspace执行非E2E检查；父分支main，单writer串行。
- **E2E disposition：** required：Lead在current-workspace验证最终浏览器、真实MySQL新/旧库及保留服务启动边界；不得把配置静态检查等同真实初始化。
- **Integration evidence：** 非空implementation/source commit、parent_before、direct-parent验证、result SHA与包含关系；当前策略candidate字段不适用。

## 9. 发布、迁移与恢复

- **迁移顺序/兼容窗口：** 这是源码/构建/初始化声明的contract收缩，不是生产迁移。新库顺序10→20→30→40→50→60；已有库禁止重放。release构建可逆地产生本地候选，current切换/容器部署另行授权。前置票与此票全部同源完成后才可声明change本地验收完成。
- **监控/回滚/前向恢复：** 保留原不可变release及旧数据；出现构建/退出/数据保留失败时不stage、不部署、不删资源，修正受影响声明后重跑。真实升级必须另行固定源/目标Tag、备份与隔离演练，本次不虚构这些事实。
- **不可逆操作与批准点：** 实现、commit、父分支推进、push、正式发布、部署、生产数据操作分别核对真实授权；用户已激活 I 并授权本地实施，本地提交与 current/direct-parent 验收已按 LOG-011 获用户授权。未授权的提交和外部动作不执行。
- **收缩条件：** 本票退出/校验合同全部通过；最终共享合同收缩和完整产物由T-03及整体Gate负责，其他票不得提前声明全change交付。

## 10. 验收标准

- [x] AC-009：Spec对应可观察结果由本票验证矩阵证明。
- [x] AC-010：Spec对应可观察结果由本票验证矩阵证明。
- [x] AC-011：Spec对应可观察结果由本票验证矩阵证明。
- [x] AC-012：Spec对应可观察结果由本票验证矩阵证明。
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
