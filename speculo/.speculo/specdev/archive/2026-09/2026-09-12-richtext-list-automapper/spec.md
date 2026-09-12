---
schema_version: 3
artifact: spec
change: 2026-09-12-richtext-list-automapper
status: ready
ready_for_tickets: true
sources:
  - USER-DECISION:Lead-skip-S-R-diagnosis-is-approved-plan
  - DIAGNOSIS:2026-09-12-richtext-list-automapper
  - CODE:ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-demo/src/main/java/org/dromara/demo/domain/vo/TestRichTextSummaryVo.java
  - CODE:ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-demo/src/main/java/org/dromara/demo/domain/vo/TestDemoVo.java
  - CODE:ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-demo/src/main/java/org/dromara/demo/service/impl/TestRichTextServiceImpl.java
  - ARCHIVE:speculo/.speculo/specdev/archive/2026-09/2026-09-08-richtext-third-oss
---

# Spec: 富文本 list SummaryVo AutoMapper 修复

- **Spec：** `<Path>{roots.state}/specdev/changes/2026-09-12-richtext-list-automapper/spec.md</Path>`
- **上游诊断：** `<Path>{roots.state}/specdev/changes/2026-09-12-richtext-list-automapper/diagnosis.md</Path>`
- **说明：** Lead 授权跳过正式 S/R 访谈；本文是诊断修复契约的工件化镜像，供 Tickets/`ticket-control` 追溯 AC，不另开设计决策。

## 1. 问题与目标

### 问题陈述

富文本演示页 create 成功后，`GET /demo/rich-text/list` 在结果非空时返回应用码 500（错误编号 `85384381`）。根因是 `TestRichTextSummaryVo` 缺少 `@AutoMapper(target = TestRichText.class)`，`selectVoPage` → `MapstructUtils.convert` 抛出 ConvertException。

### 目标用户与场景

- 演示/联调用户：保存富文本后刷新列表应成功看到摘要行。
- 实现者：按同模块 Vo 惯例补注解并留下非空 list 回归。

### 成功标准

- SummaryVo 具备 Entity→VO converter 注解。
- 非空 list 应用码成功路径，无 ConvertException。
- 定向回归修复前红、修复后绿。
- 不改归档、不改全局 MapStruct/MyBatis 基类。

### 非目标

见 **OUT** / **OOS-001**～**OOS-003**。

## 2. 解决方案与外部行为

### 解决方案摘要

给 `TestRichTextSummaryVo` 增加 `@AutoMapper(target = TestRichText.class)`（对照 `TestDemoVo`/`TestTreeVo`）。建议同步为 `TestRichTextVo` 补 `@AutoMapper` 与 `contentHtml→html` 的 `@AutoMapping`，但不强制改 create/get 手工 `view()`。补模块级非空 list 回归测。

### 主要流程

1. 用户 create 富文本成功。
2. 用户请求 list（库中至少一条可见记录）。
3. `TestRichTextServiceImpl.list` → `mapper.selectVoPage` 转换 SummaryVo 成功，返回分页摘要。

### 边界、失败与稳定错误行为

- 空列表：保持早退成功，不强制失败。
- 缺 Client 等会话问题：保留既有业务错误，非本 Spec 修复面。
- 修复前基线：非空 list 确定性 ConvertException。

### 状态转换与不变量

- Client+用户归属隔离不变。
- SummaryVo 字段集（`richTextId/title/version/updateTime`）与实体同名对齐；摘要无 html。

### 未决问题

无。

## 3. 用户故事

- **US-001**：作为演示用户，我希望保存富文本后列表能刷新成功，以便确认记录已写入。
- **US-002**：作为维护者，我希望有非空 list 回归测，以便防止 MapStruct 注解再次缺失。

## 4. 验收合同

| ID | 前置条件 | 动作或事件 | 可观察结果 | 验证接缝 |
|---|---|---|---|---|
| AC-001 | 工作树可编译 | 为 SummaryVo 按同模块惯例补 `@AutoMapper(target=TestRichText.class)` | 源码含该注解；生成 Entity→SummaryVo converter | SummaryVo 源码 + 编译/转换 |
| AC-002 | 至少一条当前用户+client 可见富文本 | 调用 list / 非空 `selectVoPage` | 不抛 ConvertException；修复前红 / 修复后绿 | `TestRichTextListAutoMapperTest` 或等价 |
| AC-003 | 本 change 选择修改 `TestRichTextVo` | 添加 `@AutoMapper` | 必须同时有 `contentHtml→html` `@AutoMapping`（或等价）；未改 Vo 则 Evidence 声明可选未做 | Vo 源码审查 |
| AC-004 | 任意实现 diff | 范围审查 | 未改归档 `2026-09-08-richtext-third-oss`、未改 BaseMapperPlus/MapstructUtils、未扩 OSS/三方 | git diff / 路径守门 |

## 5. 范围

### IN

- SummaryVo AutoMapper；建议 Vo+AutoMapping；list 非空回归测。

### REUSE

- 现有归属模型、手工 `view()`、`selectVoPage` 调用链、同模块 AutoMapper 模式。

### OUT

- **OOS-001**：不改 BaseMapperPlus / MapstructUtils 全局行为。
- **OOS-002**：不扩 OSS CORS / 三方 observability；不 reopen 归档 change。
- **OOS-003**：不改前端页面与登录/Client 绑定模型。

## 6. 已锁定实现约束

- **DEC-001**：主修 SummaryVo `@AutoMapper(target = TestRichText.class)`。来源：`diagnosis.md` §6。
- **DEC-002**：回归必须覆盖非空 list 转换。来源：`diagnosis.md` §6。
- **DEC-003**：Vo 的 AutoMapper 可选但若添加则强制 AutoMapping contentHtml→html。来源：`diagnosis.md` §6。

## 7. 数据、接口与兼容

- **公共接口变化：** 无 HTTP/权限契约变化。
- **数据模型与持久化：** 无表结构变化。
- **兼容要求：** 与同模块已有 AutoMapper Vo 一致。
- **迁移要求：** 无。
- **发布或运维影响：** 需重新编译以生成 mapstruct-plus converter。

## 8. 非功能要求

- **NFR-001 安全与隐私：** 不适用：不改变鉴权与归属过滤。
- **NFR-002 性能与容量：** 不适用：局部注解修复。
- **NFR-003 可用性与可靠性：** 非空 list 从确定性失败恢复为成功。
- **NFR-004 可观测性与运营：** 不适用：不新增指标。

## 9. 验证策略

| 接缝 | 层级 | 覆盖合同 | 现有先例或命令 | Evidence 类型 |
|---|---|---|---|---|
| SummaryVo 注解 | 静态 | AC-001 | `rg '@AutoMapper' TestRichTextSummaryVo.java` | Ticket Evidence |
| 非空 list 转换 | 模块测 | AC-002 | `mvn -pl ruoyi-modules/ruoyi-demo -am -Dtest=TestRichTextListAutoMapperTest test` | Ticket Evidence |
| Vo 映射（可选） | 静态/单测 | AC-003 | 源码审查 | Ticket Evidence |
| 范围守门 | 审查 | AC-004 | diff 不含归档/全局工具 | Ticket Evidence |
| 诊断红灯基线 | 静态+日志 | AC-002 前红 | diagnosis `red_command` | diagnostics |

## 10. 开放问题与确认记录

### 未决问题

无。

### 确认记录

- Lead：无需正式 S/R；诊断修复契约即规划输入；直接 T-tickets。
