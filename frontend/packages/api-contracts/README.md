# API 传输合同

## 当前状态

`@namewta/api-contracts` 已激活，保存经 `tooling/openapi` 确定性生成并提交的 TypeScript 传输类型与客户端。

## 职责与边界

本包只描述 HTTP 线上结构，不拥有领域模型、页面模型、业务映射、认证策略或具体运行时适配器。domain 必须在边界处把生成传输对象映射为自己拥有的模型，禁止页面直接依赖生成器内部文件。

生成目录由工具维护，不应手工编辑。仅从 `package.json` 声明的公开入口消费。

## 当前合同来源

激活快照包含Profile的50条路径。domain在资源边界映射生成transport，不将生成类型直接用作页面状态。第一方System/Workflow/Demo变更接口使用POST，查询使用GET；SnailAI第三方接口保留供应商方法。

当前快照由历史schema与实际编译后的Spring MVC映射核对后，经正式fetch/generate/check生成。provenance记录未提交工作树来源、base HEAD与证据摘要；base HEAD不表示本轮实现已提交，也不代表全量live /v3/api-docs采集。Easy-ES条件关闭时不在该来源快照内。

## 验证

使用 `openapi:check` 检查激活快照、来源信息和生成漂移，并通过工作区 typecheck、架构检查与消费方测试。
