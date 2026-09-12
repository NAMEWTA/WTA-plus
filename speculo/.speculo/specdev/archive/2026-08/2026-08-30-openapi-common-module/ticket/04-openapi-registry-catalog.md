---
schema_version: 3
artifact: ticket
change: 2026-08-30-openapi-common-module
id: T-04
title: 建立真实 HandlerMapping 注册表与授权一致的接口目录
status: done
planning_depth: deep
planning_depth_reason: 将真实 Spring MVC 映射、SpringDoc schema 与 Sa-Token 权限语义组合为公共目录合同，错误会暴露接口或产生文档漂移。
ready: true
risk: high
blocked_by: [T-01, T-03]
contract_ids: [AC-002, AC-014, AC-020, AC-021, AC-022]
owner: codex:/root
expected_changes:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-openapi/src/main/java/org/dromara/common/openapi/registry/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-openapi/src/main/java/org/dromara/common/openapi/catalog/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/openapi/catalog/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/openapi/**</Path>"
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-openapi/src/main/java/org/dromara/common/openapi/registry/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-openapi/src/main/java/org/dromara/common/openapi/catalog/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-openapi/src/test/java/org/dromara/common/openapi/registry/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-openapi/src/test/java/org/dromara/common/openapi/catalog/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/openapi/catalog/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/openapi/SysOpenApiCatalogController.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/openapi/catalog/**</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-security/src/main/java/org/dromara/common/security/handler/AllUrlHandler.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-doc/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/openapi/authorization/**</Path>"
shared_paths: []
shared_path_owners: []
---

# Ticket T-04: 建立真实 HandlerMapping 注册表与授权一致的接口目录

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/04-openapi-registry-catalog.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-04.md</Path>`

## 1. 战略与来源

- **目标：** 让真实调用、本人目录和管理员目标用户预览共享同一映射注册表与权限判断。
- **可观察产出：** 即使用户没有凭据也可查看理论可调用目录；目录 method/path/权限/schema 与真实 HandlerMethod、SpringDoc 完全一致。
- **来源：** `US-003`、`US-004`、`AC-002`、`AC-014`、`AC-020` 至 `AC-022`、`ADR-012`。
- **当前事实：** `AllUrlHandler` 已读取真实 `RequestMappingHandlerMapping`，`common-doc` 已解析 Sa-Token annotation metadata，但尚无方法级开放注册表或目标用户目录。
- **Planning Depth 原因：** 注册遗漏、类级误开放或权限 matcher 漂移会直接扩大可调用面。

## 2. 决策状态

### 已锁定决策

- 只收录 HandlerMethod 自身直接声明 `@OpenApi` 的实际 MVC mapping；类级注解无效。
- Spring MVC 决定 method/path，SpringDoc 决定参数/schema，注册表补充说明与原始权限/角色元数据。
- 目录与真实调用使用同一个支持 Sa-Token AND/OR/orRole 语义的 matcher。
- self 从当前用户 ID 取主体；admin 仅超管可显式选择 targetUserId；查看者权限不得进入目标目录。
- 无凭据用户仍可预览，且预览不创建机器 Session。

### 已采用的低影响假设

- `interfaceId` 由稳定 method+path 身份生成，不泄露 Java 类名且映射变化时显式变化。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 注册表、权限 matcher、目录/详情/示例、self/admin GET API | HandlerMapping、SpringDoc、T-03 快照、R envelope | 凭据写、Session、签名网关、手写 schema |

## 4. 要构建什么

用户或超管读取 `/system/openApi/.../interfaces` 时，服务端按目标用户当前权威快照过滤实际开放方法，并返回参数、schema、响应以及严格遵循 v1 的占位 cURL/Java 示例。无法解析 mapping/schema 时不猜测、不发布该条目。

## 5. 实现契约

- **入口或接缝：** 启动期 registry、权限 matcher、system catalog service/controller。
- **输入与输出：** current/target userId、interfaceId -> 过滤后的摘要或详情 `R`。
- **公共接口变化：** 新增 Spec 锁定的四个目录 GET 路径。
- **不变量：** 目录不依赖 credential；详情必须属于同一用户可见集合；示例只含占位 secret。
- **状态或数据流：** HandlerMapping + SpringDoc -> immutable registry；userId -> T-03 snapshot -> matcher -> DTO。
- **错误与失败行为：** 未解析 schema、未知 interfaceId、非超管 target 请求失败关闭；不泄露内部 handler 或 secret。
- **兼容要求：** 普通 SpringDoc 与现有 Controller mapping 不变。
- **安全与隐私要求：** 后端独立校验 self/admin scope；无 Session/伪登录副作用。

## 6. 执行路线

1. 用测试 Controller 建立方法级、类级、未标注和复杂 Sa-Token metadata 红灯矩阵。
2. 构建只读 registry 和共享 permission matcher，复用当前 SpringDoc 模型。
3. 实现 system catalog service 与 self/admin GET controllers。
4. 生成占位 cURL/Java 示例并与 T-01 固定协议字段交叉验证。
5. 运行 Spring context、controller 与普通 SpringDoc 回归。

## 7. 路径访问契约

- **预计修改点/可写范围：** common registry/catalog、新 system catalog owner 包和专用 Controller。
- **只读上下文：** AllUrlHandler、common-doc、T-03 resolver。
- **共享路径：** 无；不得修改 common-doc 或现有 HandlerMapping helper。
- **保留或不动：** 普通 api-docs、动态路由、credential/Session 状态。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 方法级注册 | Spring context registry test | `./mvnw -pl ruoyi-common/ruoyi-common-openapi -am test -Dtest='*Registry*'` | 仅方法级注解进入且 method/path 唯一 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-04.md</Path>` |
| target 权限目录 | catalog service/controller test | `./mvnw -pl ruoyi-admin -am test -Dtest='*OpenApiCatalog*'` | 仅目标用户能力，无 credential/Session 副作用 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-04.md</Path>` |
| schema/调用一致 | registry contract test | 比较 registry、SpringDoc 与测试 Handler 调用 | method/path/权限/schema 一致，失败关闭 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-04.md</Path>` |

- **Workspace checks：** current-workspace 或 source-worktree 跑模块测试；集成态跑 Spring context/controller 回归。
- **E2E disposition：** not-required：本切片的外部 GET 行为由 MockMvc/Spring context 覆盖，不要求真实 MySQL/Redis。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate。
- **Integration evidence：** implementation/source commit、direct-parent/candidate/result SHA 和父分支包含关系。

## 9. 发布、迁移与恢复

- **迁移顺序：** T-03 resolver 先集成；registry/catalog additive 发布且受 T-12 feature flag 控制。
- **兼容窗口：** 无旧目录；真实映射变化会自然产生目录变化。
- **监控信号：** registry 条目数、不可解析条目数、目录拒绝类别。
- **回滚或前向恢复：** 关闭 OpenAPI flag 隐藏目录；映射/schema 漂移以前向修复。
- **不可逆操作与批准点：** 无；实施与集成需授权。
- **收缩条件：** 不适用。

## 10. 验收标准

- [x] `AC-002`、`AC-014`、`AC-020` 至 `AC-022` 均有自动证据。
- [x] self/admin/真实调用共享 registry、resolver 与 matcher，预览无 Session 副作用。
- [x] 无法解析的 mapping/schema 不出现在目录中且有可观测失败。
- [x] Evidence、commit、集成 SHA、E2E disposition 与 Map 同步。
