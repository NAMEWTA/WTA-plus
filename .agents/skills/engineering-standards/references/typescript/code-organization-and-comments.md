# TypeScript / Vue 代码组织、命名与注释

适用 `module:plus-ui-namewta`。当前架构以工作区包所有权为主轴；根级 `src/` 与 `gen/` 已退役。

### TS-ORG-001 代码按所有权落位

Scope: `path:plus-ui-namewta/apps/**`, `path:plus-ui-namewta/packages/**`, `path:plus-ui-namewta/tooling/**`

Level: MUST

Source: `repository-fact`（工作区目录、包 README、架构检查）

Rule: 终端组合/布局归 `apps`，无界面业务归 `domains`，Vue 领域表现归 `web-domains`，跨领域最小合同归 `platform`，具体运行时归 `adapters`，经过多消费者证明的 Web 机制归 `web-kit`，生成 transport 归 `api-contracts`。不得恢复根级产品 `src` 或前端模板 `gen`。

Verification: review 新文件 owner；`pnpm architecture:check`; `pnpm architecture:test`; 受影响包 lint/typecheck/test/build。

### TS-ORG-002 按角色命名

Scope: `module:plus-ui-namewta`

Level: SHOULD

Source: `repository-fact` + `builder-baseline`

Rule: 工作区目录和包名使用 kebab-case；公开 Vue 组件优先 PascalCase，页面入口可用 `index.vue`，组合式函数用 `useXxx.ts`，测试与被测模块相邻用 `*.test.ts`，浏览器 E2E 用 `e2e/**/*.spec.ts`。现有局部命名不为风格统一批量改写。

Verification: 对照同包相邻文件；检查 exports、组件注册、route name 和 import 大小写；`pnpm typecheck`; `git diff --summary`。

### TS-ORG-003 公开入口不是通用 barrel

Scope: `module:plus-ui-namewta`

Level: MUST

Source: `repository-fact`（package exports）+ architecture gate

Rule: 每个包只公开必要根入口或明确子路径；禁止 catch-all barrel、包深层导入、跨工作区相对导入、隐藏循环依赖和顶层自动注册副作用。App 在组合入口显式选择能力。

Verification: review `package.json#exports` 与消费者 import；`pnpm architecture:check`; `pnpm typecheck`; `pnpm build:prod`。

### TS-ORG-004 测试命名与边界一致

Scope: `path:plus-ui-namewta/**/*.test.ts`, `path:plus-ui-namewta/e2e/**/*.spec.ts`

Level: SHOULD

Source: `repository-fact`

Rule: domain 测试覆盖模型、映射和业务规则；web-domain 测试覆盖 manifest 与页面行为；App 测试覆盖组合、Client 与宿主；架构工具测试覆盖依赖规则。测试名表达公开行为，不以 App 测试替代下层合同测试。

Verification: `pnpm test`; 按风险运行 `pnpm test:e2e`; review runner discovery 与被测公开行为。

### TS-ORG-005 注释解释原因与边界

Scope: `module:plus-ui-namewta`

Level: SHOULD

Source: `builder-baseline` + repository docs

Rule: 面向维护者的注释使用中文并解释 WHY、兼容、安全、异步或生命周期约束；技术标识保持原文。公共合同可用简短 JSDoc，不逐行旁白，不添加统一版权头或无 owner/删除条件的 TODO。包职责写入 README，历史迁移过程留在 Speculo 证据。

Verification: review 注释与真实合同；搜索旧根路径/旧 Skill/Ticket 长期说明；不手改生成声明和 OpenAPI 生成文件。
