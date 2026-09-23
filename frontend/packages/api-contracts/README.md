# API 传输合同

## 当前状态

`@namewta/api-contracts` 已激活，保存经 `tooling/openapi` 确定性生成并提交的 TypeScript 传输类型与客户端。

## 职责与边界

本包只描述 HTTP 线上结构，不拥有领域模型、页面模型、业务映射、认证策略或具体运行时适配器。domain 必须在边界处把生成传输对象映射为自己拥有的模型，禁止页面直接依赖生成器内部文件。

生成目录由工具维护，不应手工编辑。仅从 `package.json` 声明的公开入口消费。

## 当前合同来源

当前版本由 `openapi/current.json` 指向；对应 `openapi/revisions/<revision>/provenance.json` 保存后端提交、运行端点与源文档摘要，`source.json` 保留该次真实 full JAR 的完整 `/v3/api-docs` 原文。以这些文件为唯一来源，不在本说明中重复固定提交号或路径/schema数量。历史 revisions 保持不可变。

通知提交当前仅支持 ALL、ASYNC 和 priority=0；HTTP JSON 必须显式传入 `priority`，生成类型也将其标为必填。历史策略枚举可读取，但不代表新提交支持对应编排；`RetryReceipt.queuedCount` 是整数。

注册 `RegisterBody.phoneNumber` 已必填；当前合同无 Snail AI 路径或 schema，NAMEWTA `/system/openApi/**` 管理合同保留。机器调用网关使用业务 URL 上的签名头，不以 `/openapi` 前缀判断是否存在。domain 在资源边界映射 transport，不直接用作页面状态；生成器从该不可变 source 正式生成并检查。

本次运行关闭 SnailJob 客户端、Nacos 和 Monitor 客户端；Easy-ES 等条件关闭的接口不会被 Springdoc 枚举。快照描述该真实运行组合，不能推导所有可选能力均已启动。

## 验证

使用 `openapi:check` 检查激活快照、来源信息和生成漂移，并通过工作区 typecheck、架构检查与消费方测试。
