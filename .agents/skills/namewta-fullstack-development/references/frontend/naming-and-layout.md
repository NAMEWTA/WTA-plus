# 文件、目录与命名

## 落位判断

先判断代码是否依赖 Vue/DOM、是否只服务单一 App、是否属于单一业务领域、是否已有多个真实消费者。完整目录职责与依赖方向见 [architecture.md](architecture.md)。不能因为“可能复用”就提前放入 `platform` 或 `web-kit`。

根级没有产品 `src` 或前端模板 `gen`。

## AGENTS.md 索引

- 根 `frontend/AGENTS.md` 描述工作区；每个含 `package.json` 的 App、domain、web-domain、platform、adapter、web-kit、合同或 tooling 包各有一份同目录 `AGENTS.md`。
- 索引只写当前包的用途、组成、公开入口、依赖方向、验证命令和 `Read Next` 链接，不复制架构规范、README 全文或源码实现。
- 读取代码时使用最近的 `AGENTS.md`；子包可以补充本地事实，但不能放宽父级硬边界。README、`package.json#exports`、入口和测试变化必须同步检查索引。
- `node_modules`、`dist`、`coverage`、`target`、生成快照和 README-only 占位目录不创建索引；尚未激活的未来 App/适配器继续使用已有 README。

已激活 App 不得建立 `src/api` 复制后端接口或包装 domain 返回值。App 在 `src/application` 中装配所选 domain service、终端适配器和宿主行为；共享接口与模型进入 domain，共享 Vue 表现进入 web-domain。

## 资源目录

- 一级包由后端模块 owner 确定，例如 `wta-profile` 对应 `domains/profile` 与 `web-domains/profile`。
- `src` 下资源名由实际消费的 Controller base path 和同包既有分组共同确定。去掉模块前缀和 Java 实现前缀后使用 kebab-case；保留 `monitor/*`、`person/*`、`enterprise/*` 等能表达 owner 的稳定层级。
- 同一 Controller 资源的 `index.ts`、`types.ts`、可选 `transport.ts`/`service.ts` 与测试同目录。存在 Vue 页面时建立相应 web-domain 资源；只有 headless、回调或服务端专用合同则不建立空 Web 对称目录。
- 每个公开资源必须在包 `package.json#exports` 中声明 `./<resource>`；消费者只从该子路径或必要根入口导入。
- package 根可以保留兼容 facade，但只显式聚合稳定合同。禁止 `export *` 把所有资源类型合并为 catch-all 表面。

## 薄入口

“薄”表示职责单一，不是固定行数：

- 资源 `index.ts` 只显式导出公共类型、service/mapper 和 Controller 元数据。
- 包根 `index.ts` 只承载 domain module、兼容 facade 或 manifest 组合。
- 不在 `index.ts` 实现 HTTP 请求、运行时校验、完整模型集合、页面异步状态或自动注册副作用。
- 需要拆分时按 `types.ts`、`transport.ts`、`service.ts` 和有业务名称的文件提取；不要为减少行数制造无 owner 的 `helpers.ts`/`common.ts`。

## 命名

- 工作区包使用 `@namewta/<role-name>`，目录使用 kebab-case。
- Vue 组件文件遵循所在包既有模式；公开组件优先 PascalCase，页面入口可使用 `index.vue`，不要为统一大小写批量重命名。
- 组合式函数使用 `useXxx.ts`，测试与被测模块相邻使用 `*.test.ts`，浏览器测试使用 `e2e/**/*.spec.ts`。
- domain 与 web-domain 使用相同领域标识；manifest 的组件键必须保持后端菜单合同精确一致。
- 包只公开必要根入口或明确子路径，禁止 catch-all barrel 和深层导入。

## 新增文件决策

移动 Web、小程序和 Taro 适配器在激活前只允许 README；真实包命名、源码与构建脚本必须在独立规格内确定。
