# 架构检查工具

`@namewta/architecture` 是已激活的私有工作区工具，用于确定性检查多 App 领域架构。

## 检查范围

- 只发现含真实 `package.json` 的工作区目录，并校验目录类型、私有性、公开 exports、内部 `workspace:*` 依赖与根级聚合脚本。
- 使用 TypeScript AST 和 Vue SFC 解析器识别真实导入，检查依赖方向、深层导入、跨包相对导入、循环和终端纯度。
- 结构化解析 `pnpm-workspace.yaml` 与 lockfile，检查工作区 glob、catalog、importer 和依赖规格漂移。
- 检查 domain/platform 中未经遮蔽的浏览器、DOM、存储、网络、Worker 与渲染全局。
- 检查已激活 App 只通过 domain 公开服务完成后端能力组合。
- 检查 Admin 动态页面只经所选 manifest 和共享菜单运行时解析，权限指令只从 Web Kit 安装，已退役的私有 owner 与 fallback 不得回流。
- 将当前发现与 `baseline.json` 的精确身份比较；新增、过期或扩大的基线都失败关闭。
- 确保移动 Web、小程序和 Taro 适配器在激活前只含 README。

## 边界

本工具不进入产品运行时，不改写源码或清单，不替代 typecheck、行为测试、构建和浏览器验收，也不提供宽泛忽略。

## 命令

```bash
pnpm architecture:check
pnpm architecture:test
pnpm --filter @namewta/architecture exec node ./src/cli.mjs baseline --root ../..
```

基线命令只打印测量结果，不写文件。任何基线变化都必须逐条审查，已解决条目必须删除。