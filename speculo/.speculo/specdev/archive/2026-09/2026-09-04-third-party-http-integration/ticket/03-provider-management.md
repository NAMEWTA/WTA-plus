---
schema_version: 3
artifact: ticket
change: 2026-09-04-third-party-http-integration
id: T-03
title: 交付 Provider 管理与供应商级状态边界
status: done
planning_depth: deep
planning_depth_reason: Provider 是所有请求的安全目标和聚合根，涉及 trusted base URL、权限、启停优先级和受控逻辑删除。
ready: true
risk: high
blocked_by: [T-01, T-02]
contract_ids: [AC-001, AC-016, AC-017]
owner: codex:/root
expected_changes:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/controller/admin/provider/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/usecase/provider/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/service/provider/**</Path>"
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/controller/admin/provider/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/usecase/provider/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/service/provider/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/domain/bo/provider/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/domain/vo/provider/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/test/java/org/dromara/third/provider/**</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/domain/entity/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/mapper/**</Path>"
shared_paths: []
shared_path_owners: []
---

# Ticket T-03: 交付 Provider 管理与供应商级状态边界

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/03-provider-management.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-03.md</Path>`

## 1. 战略与来源

- **目标：** 让管理员安全维护供应商聚合根、trusted base URL、共享策略上限和启停/删除状态。
- **可观察产出：** 有权限管理员可查询、新增、编辑、启停和受控删除 Provider；无权限、非法 URL、重复 code 或仍有 Endpoint 时明确拒绝。
- **来源：** `US-002`、`US-008`、`AC-001`、`AC-016`、`AC-017`、`ADR-002`、`ADR-004`。
- **当前事实：** T-01/T-02 仅提供模块/API 与数据基座，不提供管理行为。
- **Planning Depth 原因：** Provider 定义出站信任目标，错误校验会形成 SSRF 或全局启停失效。

## 2. 决策状态

### 已锁定决策

- 查询为 GET、变更为 POST；每个 POST 使用安全 `@Log`，UseCase 承担事务。
- base URL 仅允许受信任 HTTP(S) origin，禁止 user-info、fragment 和不受支持 scheme；不把完整 URL 下放 Endpoint。
- Provider 关闭优先于所有 Endpoint；只允许 disabled 且无未删除 Endpoint 的 Provider 逻辑删除。

### 已采用的低影响假设

- 分页、BO/VO、MapStruct、重复提交与权限注解沿用 system 相邻 CRUD 风格。

### 未决问题

无。

## 2.1 必须加载的 Skill 与工程基线

- **必须加载：** <Path>.agents/skills/engineering-standards/SKILL.md</Path>、<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>、<Path>.agents/skills/ruoyi-module-guide/SKILL.md</Path>、<Path>.agents/skills/ruoyi-common-modules-guide/SKILL.md</Path>。
- **必须先读的参考：** <Path>.agents/skills/engineering-standards/references/project/00-project-profile.md</Path>、<Path>.agents/skills/engineering-standards/references/project/01-module-map.md</Path>、<Path>.agents/skills/engineering-standards/references/project/03-backend-module-modes.md</Path>、<Path>.agents/skills/namewta-fullstack-development/references/backend/module-layout.md</Path>、<Path>.agents/skills/namewta-fullstack-development/references/backend/implementation.md</Path>、<Path>.agents/skills/namewta-fullstack-development/references/backend/api-errors-resources.md</Path>、<Path>.agents/skills/namewta-fullstack-development/references/backend/framework-usage.md</Path>。
- **目录与代码最低要求：** 使用 controller/admin/provider、usecase/impl、service、dao、mapper 的 layered 主轴；Controller 只注入 UseCase，Service 不互相调用，BO/VO 与 Entity 分离。查询一律 GET，状态/删除/保存一律 POST；每个 POST 使用安全 @Log，事务命令使用 @DSTransactional。
- **权限与数据要求：** 后端 @SaCheckPermission 是权威；分页入口解包 PageQuery，UseCase/Service 不导入 PageQuery/MyBatis Page；Mapper 查询返回 read Row 后由 Service 映射 VO。复用准确的 R/PageResult、Bean Validation、@RepeatSubmit 和 common FQN。
- **执行停止条件：** 新建 service/impl、Controller 直连 Mapper、把 base URL 当普通字符串不校验、在 @Log 中记录配置/凭据、或修改 system 权限实现而非 third owner 时立即停止。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| Provider Admin CRUD、校验、状态机、权限、删除保护 | T-02 Mapper、R/PageResult、Sa-Token、Log、DSTransactional | Endpoint、凭据明文、缓存、HTTP 调用 |

## 4. 要构建什么

管理员在 `/third/provider` 下分页查看供应商并维护基础配置；更新 code 冲突或危险 base URL 时零写入，关闭后新调用将由下游按快照拒绝；删除动作只有在 disabled 且没有未删除 Endpoint 时成功，历史调用仍能按编码查询。

## 5. 实现契约

- **入口或接缝：** Provider Admin Controller → UseCase → Service → DAO/Mapper。
- **输入与输出：** 受校验 BO/分页条件，返回不含 credential 的 VO 和稳定结果。
- **公共接口变化：** 无。
- **不变量：** providerCode 全局唯一且不可空；base URL 是可信 origin；关闭状态优先。
- **状态或数据流：** POST 在 UseCase 事务内写 DB；缓存失效留给 T-06 接入。
- **错误与失败行为：** 重复 code、非法 origin、非法状态迁移、仍有 Endpoint、无权限均无部分写入。
- **兼容要求：** 不改变 system 菜单/用户/日志 API。
- **安全与隐私要求：** Controller 日志排除敏感字段；VO 不出现 secret/key/ciphertext。

## 6. 执行路线

1. 建立 MVC 权限、URL 负向和删除保护测试。
2. 实现 Provider BO/VO、查询 Service 和事务 UseCase。
3. 实现 GET/POST Admin Controller、权限码和安全日志。
4. 覆盖重复 code、并发更新、启停和逻辑删除。
5. 运行 MVC、Service 与 MySQL 集成回归。

## 7. 路径访问契约

- **预计修改点/可写范围：** provider controller/usecase/service/BO/VO/test 子树。
- **只读上下文：** T-02 entity/mapper 与现有 system CRUD 先例。
- **共享路径：** 无；全局 POM、SQL 和 API 不得修改。
- **保留或不动：** Endpoint/Credential 业务、Redis、出站 HTTP。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | MVC/MySQL | Provider 新增、查询、编辑、关闭、删除 | 状态和响应稳定，审计列完整 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-03.md</Path>` |
| 失败路径 | MVC/Service | 非法 origin、重复 code、越权、有 Endpoint 删除 | 明确拒绝且 DB 零部分更新 | 同上 |
| 回归 | module test | `./mvnw -pl :ruoyi-third -am test` | module 与已有 Admin 行为通过 | 同上 |

- **Workspace checks：** source/current workspace 运行 provider 定向测试、Checkstyle/编译。
- **E2E disposition：** required：权限过滤、事务和真实 MySQL 唯一/删除保护跨越 HTTP 与数据库。
- **E2E owner/environment：** Lead / parent-candidate 或 current-workspace；执行授权/越权及状态生命周期场景。
- **Integration evidence：** implementation/source commit、parent before、candidate/result SHA 和父分支包含关系。

## 9. 发布、迁移与恢复

- **迁移顺序：** T-02 schema → Provider API；上线后再开放 Endpoint。
- **兼容窗口：** additive 路由，默认无 Provider。
- **监控信号：** 非法 URL、状态冲突、删除拒绝和 DB 唯一冲突。
- **回滚或前向恢复：** 禁用 Provider 即可撤入口；保留数据前向修复。
- **不可逆操作与批准点：** 无物理删除或生产启停；生产操作另行批准。
- **收缩条件：** 不适用：无旧 Provider 管理入口。

## 10. 验收标准

- [x] `AC-001`：Provider 管理、唯一性与启停成立。
- [x] `AC-016/017`：前后端权限所需 API 和受控删除后端合同可判定。
- [x] URL、权限、事务与历史保留失败路径有 Evidence。
- [x] required E2E、提交与集成证据完整。
