---
schema_version: 3
artifact: ticket
change: 2026-08-30-openapi-common-module
id: T-02
title: 在统一 HTTP 与操作日志中屏蔽 OpenAPI 敏感材料
status: done
planning_depth: deep
planning_depth_reason: 修改统一日志安全边界并处理凭据、签名、JSON、二进制和流式正文，属于隐私高风险路径。
ready: true
risk: critical
blocked_by: []
contract_ids: [AC-028, AC-029]
owner: codex:/root
expected_changes:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-web/src/main/java/org/dromara/common/web/logging/SysLogFilter.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-web/src/main/java/org/dromara/common/web/logging/SysLogBodySanitizer.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-web/src/test/java/org/dromara/common/web/logging/SysLogFilterTest.java</Path>"
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-web/src/main/java/org/dromara/common/web/logging/SysLogFilter.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-web/src/main/java/org/dromara/common/web/logging/SysLogBodySanitizer.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-web/src/test/java/org/dromara/common/web/logging/SysLogFilterTest.java</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-web/src/main/java/org/dromara/common/web/filter/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-log/**</Path>"
shared_paths: []
shared_path_owners: []
---

# Ticket T-02: 在统一 HTTP 与操作日志中屏蔽 OpenAPI 敏感材料

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/02-redact-openapi-logging.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-02.md</Path>`

## 1. 战略与来源

- **目标：** 在开放流量进入系统前扩展既有唯一日志策略，杜绝 AppKey/AppSecret/签名/内部 Token 泄漏。
- **可观察产出：** 捕获的统一 HTTP 事件对协议 headers 和嵌套敏感 JSON 均脱敏，非 JSON/二进制/流只保留元数据。
- **来源：** `US-008`、`AC-028`、`AC-029`、`ADR-015`。
- **当前事实：** `SysLogFilter` 已拥有 1 MiB 有界采集、媒体策略、递归 JSON 脱敏和失败不影响响应的测试接缝。
- **Planning Depth 原因：** 统一过滤器事故半径覆盖全部请求，错误放宽会泄露生产凭据。

## 2. 决策状态

### 已锁定决策

- 复用 `SysLogFilter`，不建立 OpenAPI 原始正文表或第二条日志管线。
- `X-App-Key`、`X-Signature`、普通 Token、Cookie、AppSecret 和内部机器 Token 永不记录原值。
- JSON 只记录上限内已递归脱敏内容；非法/截断 JSON 整段隐藏；其他媒体只记元数据。
- `@Log` 的凭据变更接口必须关闭敏感请求/响应保存；具体注解由 T-06 实现。

### 已采用的低影响假设

- OpenAPI 新 header 使用大小写不敏感匹配并沿用现有 `[REDACTED]` 占位符。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 敏感 header/JSON 名单、媒体边界和日志回归测试 | `SysLogFilter`、`RepeatedlyRequestWrapper`、现有事件格式 | 新日志表、原文复制、改变业务响应 |

## 4. 要构建什么

运维查看同一套请求/响应事件时仍能获得请求 ID、路径、媒体类型、长度、状态与耗时，但任何 OpenAPI 认证材料和敏感 JSON 值均不可恢复；日志编码失败不改变 Controller 的响应。

## 5. 实现契约

- **入口或接缝：** `SysLogFilter` header/body 投影与 `SysLogFilterTest` 捕获事件。
- **输入与输出：** Servlet 请求/响应 -> 有界、安全事件 Map。
- **公共接口变化：** 仅扩展敏感字段策略，不改变事件外形。
- **不变量：** 请求/响应内容不被日志副本修改；上限、MDC 恢复和失败隔离保持。
- **状态或数据流：** filter 读取既有 repeatable wrapper 前缀，清洗后交给现有 sink。
- **错误与失败行为：** 非法/截断 JSON fail closed；sink/编码失败保持业务状态与正文。
- **兼容要求：** 普通 HTTP 日志字段和现有敏感头行为不退化。
- **安全与隐私要求：** 原始值不得进入事件、异常、断言消息或快照。

## 6. 执行路线

1. 扩展测试使 OpenAPI headers、嵌套 JSON、非法/超限 JSON 和媒体矩阵先失败。
2. 在唯一敏感策略入口增加大小写不敏感协议字段，避免 OpenAPI 特例散落。
3. 验证二进制、multipart、SSE/流式响应只产生元数据。
4. 反向验证日志 sink 失败不改变业务响应并保持 MDC 清理。
5. 运行 common-web 定向测试与后端回归。

## 7. 路径访问契约

- **预计修改点/可写范围：** 仅 `SysLogFilter`、同包递归正文策略 `SysLogBodySanitizer` 及现有测试。
- **只读上下文：** repeatable wrapper 与操作日志切面。
- **共享路径：** 无；T-02 是统一 HTTP 日志策略的唯一 owner。
- **保留或不动：** 正文上限、响应包装顺序、事件 schema 与业务请求对象。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 敏感 JSON/header | `SysLogFilterTest` | `./mvnw -pl ruoyi-common/ruoyi-common-web -am test` | 全部原值缺席且安全字段保留 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-02.md</Path>` |
| 非 JSON/流式 | 媒体参数化测试 | 同上 | 只记录类型、长度、状态、耗时 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-02.md</Path>` |
| 日志失败回归 | sink 故障测试 | 同上 | 业务响应、正文、request ID 不变 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-02.md</Path>` |

- **Workspace checks：** current-workspace 或 source-worktree 执行 common-web 测试；集成后执行受影响 reactor 测试。
- **E2E disposition：** not-required：统一 filter 的真实 Servlet wrapper 行为已由模块测试覆盖，最终应用组合由 T-12 验证。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate。
- **Integration evidence：** implementation/source commit、direct-parent/candidate/result SHA 与父分支包含关系。

## 9. 发布、迁移与恢复

- **迁移顺序：** 必须在 T-09 开放签名入口前集成。
- **兼容窗口：** additive redaction，无调用方迁移。
- **监控信号：** 抽查安全测试事件中敏感原值为零。
- **回滚或前向恢复：** 不回滚为泄密策略；出现误报以前向调整字段匹配并保留 fail closed。
- **不可逆操作与批准点：** 无；实现提交与集成仍需授权。
- **收缩条件：** 不适用。

## 10. 验收标准

- [ ] `AC-028`、`AC-029` 自动测试通过。
- [ ] OpenAPI 认证材料在 header、JSON、错误和 sink 失败路径均不泄露。
- [ ] 既有 MDC、媒体、上限与响应不变测试保持绿色。
- [ ] Evidence、commit、direct-parent/candidate/result 与 Map 状态完整一致。
