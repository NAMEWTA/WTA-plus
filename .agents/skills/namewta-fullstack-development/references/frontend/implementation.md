# 领域能力与终端实现

## 新增或迁移领域能力

1. 确认唯一后端 Maven 模块、Controller 类和 `@RequestMapping` base path。
2. CRUD、树表或资源纵切片必须先读取 `docs/fm/README.md`、`catalog.json`、`context-contract.md` 与相关 `docs/fm/vue/*.ftl`，再对照同 owner 成熟实现。模板不足或落后于当前架构时同步更新模板，不在新资源中继续复制旧结构。
3. 如需生成传输类型，更新 `packages/api-contracts` 与 `tooling/openapi` 的来源、快照和生成结果；生成 transport 进入 domain 时映射为 domain-owned model。
4. 在 `packages/domains/<module>/src/<resource>` 放置领域类型、transport mapper、service/factory 和资源元数据；只创建实际需要的文件。
5. Web 能力放入 `packages/web-domains/<module>/src/<resource>`，通过类型化 runtime 端口获取字典、反馈、弹窗、下载和导航等宿主能力。tree 构造、循环/孤儿裁决和领域排序归 domain，页面只管理展开、选择和交互状态。
6. 在两个包的 `package.json#exports` 公开资源子路径；包根只保留显式 domain/manifest 组合及确有调用方的兼容 facade，禁止 catch-all exports、深层导入和跨工作区相对导入。
7. 更新包 README 中的职责、后端模块、Controller 映射、公开入口和验证命令，并扩展 `tooling/architecture/test/domain-layout.test.mjs` 的资源清单。
8. 在目标 App 同步完成三点选择：依赖声明、service 创建、domain/web-domain manifest 与 runtime 组合。禁止通过顶层副作用自动注册。
9. 测试 domain transport/规则、web-domain registration/页面状态和 App 组合；验证未选择的能力不可见，重复键、缺失 runtime/domain 和未知 registration 均失败关闭。
10. 涉及动态菜单时验证 `getInfo -> getRouters -> addRoute -> replace` 顺序、当前 Client 隔离、manifest componentKey 解析和失败关闭；涉及菜单图标时验证 `local-name`、`tabler:name`、外部 Iconify、旧无前缀兼容与 fallback。

完整文件职责、模板落位和迁移清单见 [crud-resource-slices.md](crud-resource-slices.md)。

## 新增 App

新 App 必须独立拥有：

- 包清单和入口；
- ClientId、ClientContext、环境变量与会话存储命名空间；
- 所选 domain/web-domain 及 manifest 组合；
- 路由、布局、主题、运行时适配器、构建和部署合同。

新终端只组合所需能力，不复制 Admin 全量 API、Store 或页面。启用 Client Web、移动 Web 或小程序前，先通过独立规格确定框架版本、目标平台、安全能力、领域选择和不可用的 Web/DOM 依赖，再把 README 占位转换为工作区包。

## 验证选择

先使用 `pnpm --filter <package-name> <script>` 取得快速反馈，再执行工程规范项目画像中适用的根级命令。涉及认证、动态菜单、权限或终端隔离时追加对应 Playwright 场景；第二个 App 激活后，E2E 必须覆盖 App 间隔离，不能只验证 Admin。
