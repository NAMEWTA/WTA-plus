---
schema_version: 3
artifact: ticket
change: 2026-09-08-richtext-third-oss
id: T-01
title: 富文本演示在缺少 Client 上下文时保持可诊断且不崩溃
status: done
planning_depth: standard
planning_depth_reason: 跨前端页面和后端 demo 服务的垂直修复，需要补单元验证，但不涉及 schema、迁移或外部系统改造。
ready: true
risk: medium
blocked_by: []
contract_ids: [AC-001]
owner: grok-build
expected_changes:
  - "<Path>plus-ui-namewta/packages/web-domains/demo/src/test-rich-text/RichTextPage.vue</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-demo/src/main/java/org/dromara/demo/service/impl/TestRichTextServiceImpl.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-demo/src/test/java/org/dromara/demo/service/impl/TestRichTextServiceImplTest.java</Path>"
writable_paths:
  - "<Path>plus-ui-namewta/packages/web-domains/demo/src/test-rich-text/RichTextPage.vue</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-demo/src/main/java/org/dromara/demo/service/impl/TestRichTextServiceImpl.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-demo/src/test/java/org/dromara/demo/service/impl/TestRichTextServiceImplTest.java</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-demo/src/main/java/org/dromara/demo/controller/TestRichTextController.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/system/api/model/LoginUser.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysLoginService.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysMenuController.java</Path>"
  - "<Path>plus-ui-namewta/packages/domains/demo/src/rich-text.ts</Path>"
shared_paths: []
shared_path_owners: []
---

# Ticket T-01: 富文本演示在缺少 Client 上下文时保持可诊断且不崩溃

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-09-08-richtext-third-oss/ticket/01-richtext-client-context-stability.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-09-08-richtext-third-oss/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-09-08-richtext-third-oss/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-09-08-richtext-third-oss/evidence/T-01.md</Path>`

实现本 Ticket 时，Lead 与 implementation subagent 必须按顺序完整读取总体 Map、其中适用于 `ALL`/`T-01` 的项目 Skill，再读取本 Ticket 与其他上游工件。

## 1. 战略与来源

- **目标：** 把富文本演示页对会话 Client 的隐式假设变成显式、可诊断的业务错误，同时让页面本身捕获失败，不再输出未处理 promise rejection。
- **可观察产出：** 缺少 Client 时，列表刷新或保存会显示明确错误；会话正常时，富文本创建/编辑/删除仍可用。
- **来源：** `AC-001`、`DIAG-2026-09-08-richtext-third-oss`、`CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-demo/src/main/java/org/dromara/demo/service/impl/TestRichTextServiceImpl.java</Path>`、`CODE:<Path>plus-ui-namewta/packages/web-domains/demo/src/test-rich-text/RichTextPage.vue</Path>`
- **当前事实：** 服务层多处依赖 `LoginHelper.getUserId()` 与 `clientPk()`，页面在 mounted/save/remove 路径上没有统一失败捕获。
- **Planning Depth 原因：** 这是跨前后端的垂直修复，需要把显式业务错误和页面容错一起落地，但不涉及 schema、迁移或 shared owner。

## 2. 决策状态

### 已锁定决策

- 富文本仍按当前用户 + Client 归属隔离。
- 缺少上下文时返回业务错误，不再让页面看到泛化 500。
- 页面必须捕获加载/保存/删除失败，避免未处理 promise rejection。

### 已采用的低影响假设

- 现有页面上的错误提示可以继续使用 Element Plus 消息组件。
- 后端单测可以通过 `mockStatic(LoginHelper.class)` 直接控制登录上下文。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| 富文本服务的上下文校验收口、页面失败捕获、单测 | 现有富文本归属模型、OSS 引用对账、现有页面结构 | 登录流重做、Client 绑定模型重写、OSS 权限模型改造 |

## 4. 要构建什么

用户进入富文本演示页、点击新建或保存时，如果会话里没有足够的 Client 上下文，页面不应再抛出未处理异常或把页面状态打断。相反，系统应该返回一个能直接指向会话问题的错误提示，页面把它展示出来并保持可继续操作的状态。会话正常时，创建、修改、删除和刷新列表仍然按原有归属规则运行。

## 5. 实现契约

- **入口或接缝：** `RichTextPage.vue` 的 `load()` / `save()` / `remove()`，以及 `TestRichTextServiceImpl` 的会话上下文读取与归属校验。
- **输入与输出：** 输入是当前会话、富文本标题和 HTML；输出是可诊断的业务错误或正常的富文本视图。
- **公共接口变化：** 无。
- **不变量：** 仍必须按当前用户 + Client 访问自己的富文本数据；空上下文不能绕过归属检查。
- **状态或数据流：** 先校验会话，再做归属校验，最后再持久化或刷新列表。
- **错误与失败行为：** 缺少 Client 或用户上下文时必须返回稳定业务错误，前端必须吞掉 promise rejection 并显示错误。
- **兼容要求：** 既有富文本数据、OSS 资源和编辑流程不受影响。
- **安全与隐私要求：** 不暴露他人富文本内容，不记录密钥或签名 URL。

## 6. 执行路线

1. 收口富文本服务里的会话上下文读取，避免 `LoginHelper.getUserId().equals(...)` 这类空指针路径。
2. 给页面的加载、保存和删除路径加统一失败捕获，保证错误可见、页面不崩。
3. 增加针对缺少 Client 上下文的后端回归测试，必要时补一条浏览器级 smoke / Playwright 场景。
4. 跑定向验证并确认成功路径未退化。

## 7. 路径访问契约

- **预计修改点：** 与 `expected_changes` 对齐，仅作导航。
- **可写范围：** 与 `writable_paths` 对齐；越界前必须停止。
- **只读上下文：** 与 `read_only_paths` 对齐。
- **共享路径：** 无。
- **保留或不动：** 富文本列表、OSS 引用和页面布局结构。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | 富文本服务 + 页面 | `mvn -pl ruoyi-modules/ruoyi-demo -am -Dtest=TestRichTextServiceImplTest test` | 正常上下文仍可创建/刷新 | `<Path>{roots.state}/specdev/changes/2026-09-08-richtext-third-oss/evidence/T-01.md</Path>` |
| 失败路径 | 富文本服务 | 静态 mock 缺少 Client 上下文并调用 `list` / `create` | 返回明确业务错误，不是未知 500 | `<Path>{roots.state}/specdev/changes/2026-09-08-richtext-third-oss/evidence/T-01.md</Path>` |
| 回归 | 浏览器页 | 新增 Playwright / smoke 验证保存失败路径被捕获 | 页面不再出现未处理 promise rejection | `<Path>{roots.state}/specdev/changes/2026-09-08-richtext-third-oss/evidence/T-01.md</Path>` |

- **Workspace checks：** current-workspace，先跑后端单测，再跑页面或浏览器 smoke。
- **E2E disposition：** required: 这是用户可见页面的失败捕获修复，必须在浏览器层确认不再炸页。
- **E2E owner/environment：** Lead / current-workspace；场景是富文本页在模拟后端失败或缺少 Client 时仍可稳定展示错误。
- **Integration evidence：** implementation commit、direct-parent 验证结果和父分支 result SHA 在完成时写入 Evidence；当前 draft 阶段仅保留计划，不伪造 SHA。

## 9. 发布、迁移与恢复

- **迁移顺序：** 不适用：无数据迁移。
- **兼容窗口：** 不适用：不改公共接口。
- **监控信号：** 富文本页的 500 / 未处理 promise rejection 计数应下降。
- **回滚或前向恢复：** 回滚时恢复原有服务校验和页面逻辑。
- **不可逆操作与批准点：** 无。
- **收缩条件：** 不适用：没有旧契约收缩。

## 10. 验收标准

- [ ] `AC-001`：缺少 Client 上下文时，富文本页返回明确业务错误且不再出现未处理 promise rejection。
- [ ] 实现开始前已完整读取 Tickets Map 及其中适用于 `ALL`/`T-01` 的项目 Skill。
- [ ] 验证矩阵全部执行并记录到 `<Path>{roots.state}/specdev/changes/2026-09-08-richtext-third-oss/evidence/T-01.md</Path>`。
- [ ] 实际项目修改未超出 `writable_paths`。
- [ ] Ticket、Tickets Map 和 Evidence 状态一致。

