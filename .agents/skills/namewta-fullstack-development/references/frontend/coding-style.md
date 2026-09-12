# 编码风格与工具链

## 当前工具

- 根 `package.json` 使用 pnpm workspace 聚合脚本；Node 要求 `>=20.19.0`，锁定 pnpm 版本见 `packageManager`。
- `pnpm lint` 使用 Oxlint，并继续调用各激活包的 lint。
- `pnpm fmt` 使用 Oxfmt 写入 `apps packages tooling e2e` 等受管路径，不是只读门禁。
- `pnpm typecheck`、`pnpm test`、`pnpm build:dev`、`pnpm build:prod` 都先经过或间接经过架构检查。
- 仓库没有 ESLint/Prettier 工程；Oxlint 的兼容插件名不代表另有 ESLint 配置。

## 配置事实

- `.editorconfig`：UTF-8、LF、2 空格、文件末尾换行。
- `.oxlintrc.json` 只忽略构建和覆盖率输出；项目开发 Skill 统一位于父工作区 `.agents/skills`，不在前端仓库维护工具专属 Skill 副本。
- `.oxfmtrc.json` 忽略各 App 的自动生成声明和 OpenAPI 生成/快照文件；生成声明和生成合同应通过其配置或生成器更新。
- 根 `tsconfig.json` 提供共享 TypeScript 配置，各 App/包拥有自己的 `tsconfig` 与路径边界；导入按包 `exports` 和当前 App 的局部别名解析。

## 编码原则

- TypeScript 边界优先显式类型，生成 transport 进入 domain 时必须映射为 domain-owned model。
- 公共资源按 `package.json#exports` 暴露；资源 `index.ts` 和包根 `index.ts` 只做显式导出、元数据或组合，不放 HTTP 实现、完整类型集合和页面状态。
- 网络 mapper 对 `unknown` 或生成 transport 执行明确投影、拒绝或默认策略；类型断言不能替代异常/null 边界处理。
- Vue 页面通过 web-domain runtime 取得反馈、字典、下载、导航等宿主能力，不创建全局请求/Router/Store 单例。
- Promise 必须 await、return 或由明确 owner 监督；快速查询和组件卸载必须防止旧响应覆盖，重叠请求不能只靠单个布尔 `loading` 表达所有权。
- domain/platform 不引用 Vue、DOM、浏览器存储或具体请求库。
- 资源状态先留在本资源；同包至少两个真实资源共用后再提升到包级 runtime/composable，跨包多个稳定消费者存在后才考虑 `web-kit`/`platform`。
- 保持当前文件的引号、导入排序和 SFC 风格；不要顺手格式化无关包。

## 根级命令

```bash
pnpm architecture:check
pnpm architecture:test
pnpm lint
pnpm typecheck
pnpm test
pnpm build:dev
pnpm build:prod
```

需要快速反馈时先使用 `pnpm --filter <package-name> <script>`。远程分支保护和门禁状态仍以工程规范的项目画像为准。
