# API 传输合同

## 当前状态

`@namewta/api-contracts` 已激活，保存经 `tooling/openapi` 确定性生成并提交的 TypeScript 传输类型与客户端。

## 职责与边界

本包只描述 HTTP 线上结构，不拥有领域模型、页面模型、业务映射、认证策略或具体运行时适配器。domain 必须在边界处把生成传输对象映射为自己拥有的模型，禁止页面直接依赖生成器内部文件。

生成目录由工具维护，不应手工编辑。仅从 `package.json` 声明的公开入口消费。

## 当前 Profile 状态

`openapi/current.json` 当前不包含 `/profile/**`，`generated/openapi.ts` 因此没有 Profile 传输类型。Profile domain 暂以资源内的类型化 HTTP service 固定已核实的 URL、方法和字段，并在边界映射为领域模型；页面不得直接引用 generated 文件。

待后端快照纳入 Profile 后，使用 `tooling/openapi` 的 `openapi:fetch`、`openapi:generate`、`openapi:check` 更新和校验合同，禁止手工编辑生成结果。迁移完成后删除 Profile service 的暂态合同分支。

## 验证

使用 `openapi:check` 检查激活快照、来源信息和生成漂移，并通过工作区 typecheck、架构检查与消费方测试。
