---
schema_version: 3
artifact: ticket
change: 2026-09-04-third-party-http-integration
id: T-04
title: 交付 Endpoint 元数据、继承覆盖与安全路径管理
status: done
planning_depth: deep
planning_depth_reason: 数据库驱动 method/path/参数结构会直接控制出站请求，需在管理写入与调用前共同锁定 URI、header 和模板安全边界。
ready: true
risk: high
blocked_by: [T-03]
contract_ids: [AC-002, AC-003, AC-006, AC-007, AC-016, AC-017]
owner: codex:/root
expected_changes:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/controller/admin/endpoint/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/usecase/endpoint/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/service/endpoint/**</Path>"
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/controller/admin/endpoint/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/usecase/endpoint/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/service/endpoint/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/domain/bo/endpoint/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/domain/vo/endpoint/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/test/java/org/dromara/third/endpoint/**</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/service/provider/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/domain/entity/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/mapper/**</Path>"
shared_paths: []
shared_path_owners: []
---

# Ticket T-04: 交付 Endpoint 元数据、继承覆盖与安全路径管理

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/04-endpoint-management.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-04.md</Path>`

## 1. 战略与来源

- **目标：** 让管理员在 Provider 下维护安全、可继承、可单独启停的标准 Endpoint 元数据。
- **可观察产出：** Endpoint 可按 providerCode+endpointCode 管理；合法 JSON/query/form 和 JSON/text/bytes 合同可保存，危险 URI/header/template 在落库前拒绝。
- **来源：** `US-003`、`US-008`、`AC-002`、`AC-003`、`AC-006`、`AC-007`、`AC-016`、`AC-017`、`ADR-002`、`ADR-004`、`ADR-009`。
- **当前事实：** Provider 已成为聚合根，但尚无 Endpoint 管理与元数据白名单。
- **Planning Depth 原因：** 动态 path/schema 触及 SSRF、请求走私和任意执行风险。

## 2. 决策状态

### 已锁定决策

- Endpoint 只保存校验后的相对 path、白名单 method/参数/header 名和结构化 body 变量，不保存完整 URL、脚本、SpEL 或任意类名。
- 拒绝绝对 URL、`//host`、路径穿越、反斜线/控制字符、任意 header 和跨域 redirect 配置。
- base URL、共享认证和策略默认继承 Provider；Endpoint 只能在 Provider 上限内覆盖；Provider 关闭优先。
- Endpoint 覆盖首期只允许服务端共享请求头 `{\"headers\":{...}}`；base URL 永不由 Endpoint 元数据覆盖。
- 删除仅允许 disabled Endpoint，历史调用保留编码。

### 已采用的低影响假设

- 元数据 JSON 采用显式版本字段以支持后续兼容扩展。

### 未决问题

无。

## 2.1 必须加载的 Skill 与工程基线

- **必须加载：** <Path>.agents/skills/engineering-standards/SKILL.md</Path>、<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>、<Path>.agents/skills/ruoyi-module-guide/SKILL.md</Path>、<Path>.agents/skills/ruoyi-common-modules-guide/SKILL.md</Path>。
- **必须先读的参考：** <Path>.agents/skills/engineering-standards/references/project/00-project-profile.md</Path>、<Path>.agents/skills/engineering-standards/references/project/01-module-map.md</Path>、<Path>.agents/skills/engineering-standards/references/project/03-backend-module-modes.md</Path>、<Path>.agents/skills/namewta-fullstack-development/references/backend/module-layout.md</Path>、<Path>.agents/skills/namewta-fullstack-development/references/backend/implementation.md</Path>、<Path>.agents/skills/namewta-fullstack-development/references/backend/security-and-data.md</Path>、<Path>.agents/skills/namewta-fullstack-development/references/backend/mapper-and-sql.md</Path>。
- **目录与代码最低要求：** Endpoint 业务仍使用 layered 五层，路径/schema/header validator 属于 Service 或明确 policy 辅助，不得在 Controller 拼接 URI。BO/VO/Row 分离，GET 查询、POST 变更、POST 使用 @Log、数据库事务使用 @DSTransactional。
- **安全与 JSON 要求：** 复用 JsonUtils/Bean Validation 等项目公共入口；metadata 必须版本化、结构化和显式白名单，禁止 ObjectMapper/SpEL/脚本/任意反射。校验逻辑需同时有管理写入和运行时消费两条负向证据。
- **执行停止条件：** 把完整 URL、任意 header、模板表达式、跨域 redirect 或未验证 schema 写入 DB，或在 endpoint 子树外修改共享路径时立即停止并回到 T-04/Lead。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| Endpoint CRUD、schema 校验、继承覆盖、状态/删除、权限 | Provider Service、T-02 Mapper、JsonUtils/Bean Validation | HTTP 发送、复杂模板、multipart、SPI 行为 |

## 4. 要构建什么

管理员在 `/third/endpoint` 下选择 Provider 并维护 method、相对 path、请求/响应格式、变量白名单、幂等和静态策略；保存时即规范化并验证，运行时仍需 T-07 二次验证。重复编码、越过 Provider 上限、危险路径或禁用状态下的非法删除均零写入。

## 5. 实现契约

- **入口或接缝：** Endpoint Admin Controller → UseCase → Service → Mapper。
- **输入与输出：** 版本化元数据 BO，返回继承来源明确但不含凭据的 VO。
- **公共接口变化：** 无。
- **不变量：** provider+endpoint code 唯一；只有固定 method/格式/header 白名单；Endpoint 上限不超过 Provider。
- **状态或数据流：** 写入前规范化、结构验证和 Provider 状态/上限检查，在 UseCase 事务内持久化。
- **错误与失败行为：** 危险 URI、未知变量、任意 header、未知 schema 版本、冲突 code、非法删除明确拒绝。
- **兼容要求：** 旧 metadata version 后续只能通过显式兼容读取或迁移，不静默重解释。
- **安全与隐私要求：** 管理响应不含凭据；内置 header 黑名单不可配置关闭。

## 6. 执行路线

1. 建立路径/header/schema 负向矩阵和 Provider 上限测试。
2. 实现版本化 BO/VO 与规范化校验服务。
3. 实现事务 UseCase、GET/POST Controller 和权限。
4. 覆盖继承展示、唯一冲突、启停与受控删除。
5. 使用 MVC/MySQL 测试证明危险输入零写入。

## 7. 路径访问契约

- **预计修改点/可写范围：** endpoint controller/usecase/service/BO/VO/test 子树。
- **只读上下文：** Provider 服务、T-02 模型/Mapper。
- **共享路径：** 无；POM、API、DDL 和 provider 子树不改。
- **保留或不动：** 凭据实现、缓存、执行 pipeline、真实供应商配置。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | MVC/MySQL | 创建继承 Provider 的 JSON/query/form Endpoint 并启停 | 元数据规范化、继承来源和状态正确 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-04.md</Path>` |
| 失败路径 | validator/MVC | 绝对 URL、//host、穿越、任意 header、未知变量/版本 | 请求拒绝且 DB 零写入 | 同上 |
| 回归 | module test | `./mvnw -pl :ruoyi-third -am test` | Provider 生命周期和模块测试仍通过 | 同上 |

- **Workspace checks：** source/current workspace 运行 Endpoint 定向测试、编译和静态检查。
- **E2E disposition：** required：需通过真实 MVC、MySQL 与零外部 HTTP 断言验证危险元数据不能生效。
- **E2E owner/environment：** Lead / parent-candidate 或 current-workspace；覆盖正常 CRUD、权限和 URI 负向场景。
- **Integration evidence：** implementation/source commit、parent before、candidate/result SHA 和父分支包含关系。

## 9. 发布、迁移与恢复

- **迁移顺序：** Provider 可用 → Endpoint 管理 → 凭据/缓存 → executor。
- **兼容窗口：** metadata 带版本，未知版本 fail-closed。
- **监控信号：** 校验拒绝原因、schema 版本异常和上限冲突。
- **回滚或前向恢复：** 禁用 Endpoint 撤入口；保留元数据前向修复。
- **不可逆操作与批准点：** 无物理删除/生产调用；生产启用另行批准。
- **收缩条件：** 不适用：无旧 Endpoint 模型。

## 10. 验收标准

- [x] `AC-002/003`：Endpoint 管理、唯一性、继承覆盖和 Provider 优先级成立。
- [x] `AC-006/007`：格式/变量受限且危险 URL/header/script 全部拒绝。
- [x] `AC-016/017`：权限与受控删除后端合同成立。
- [x] required E2E、提交、路径和集成 Evidence 完整。
