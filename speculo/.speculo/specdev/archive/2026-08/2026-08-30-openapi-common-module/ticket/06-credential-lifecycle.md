---
schema_version: 3
artifact: ticket
change: 2026-08-30-openapi-common-module
id: T-06
title: 交付每用户唯一 OpenAPI 凭据与 self/admin 生命周期 API
status: done
planning_depth: deep
planning_depth_reason: 新增项目自有 schema、加密 secret、唯一性并发和高权限管理 API，涉及数据迁移与不可恢复凭据展示。
ready: true
risk: critical
blocked_by: [T-01, T-05]
contract_ids: [AC-008, AC-009, AC-010, AC-011, AC-024, AC-026]
owner: codex:/root
expected_changes:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/openapi/credential/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/openapi/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DDL.sql</Path>"
  - "<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DML.sql</Path>"
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/openapi/credential/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/openapi/SysOpenApiCredentialController.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/openapi/credential/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DDL.sql</Path>"
  - "<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DML.sql</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/domain/SysClient.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysProfileController.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-openapi/src/main/java/org/dromara/common/openapi/session/**</Path>"
shared_paths:
  - "<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DDL.sql</Path>"
  - "<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DML.sql</Path>"
shared_path_owners:
  - "<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DDL.sql</Path> => T-06"
  - "<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DML.sql</Path> => T-06"
---

# Ticket T-06: 交付每用户唯一 OpenAPI 凭据与 self/admin 生命周期 API

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/06-credential-lifecycle.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-06.md</Path>`

## 1. 战略与来源

- **目标：** 让本人和超级管理员安全管理每用户唯一凭据，并为 common 验签提供唯一 system 实现。
- **可观察产出：** 创建/重置仅一次返回 secret；查询永不回显；禁用/删除立即拒绝且注销机器 Session。
- **来源：** `US-002`、`US-003`、`AC-008` 至 `AC-011`、`AC-024`、`AC-026`、`ADR-006`、`ADR-007`。
- **当前事实：** system 已有标准 entity/BO/VO/Mapper/service/controller 与 `@DSTransactional` 基线；NAMEWTA SQL 仅允许 append-only DDL/DML。
- **Planning Depth 原因：** schema 唯一性、KEK、一次性 secret 和跨用户管理错误不可通过 UI 隐藏补救。

## 2. 决策状态

### 已锁定决策

- 一用户最多一条未删除凭据，只绑定 userId；AppKey 全局唯一。
- AppSecret 至少 256 bit CSPRNG，AES-256-GCM + nonce/tag + `kek_version` 保存，仅成功创建/重置响应显示一次。
- reset 保留 AppKey、立即切换 secret；delete 后重建产生新 AppKey；enable 不恢复旧 Session。
- self 必须有 `system:openApi:self` 且 owner 从登录上下文取；admin 还必须后端确认超级管理员。
- 所有 POST 使用准确 `@Log` 并关闭敏感请求/响应保存；service 采用 `@DSTransactional`。

### 已采用的低影响假设

- 表名使用 system 前缀且主键为完整业务名 `_id`；具体稳定名称由同模块命名规则确定。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| schema/entity/BO/VO/mapper/service、KEK crypto、credential SPI、self/admin GET/POST、菜单权限 SQL | BaseEntity、optimistic lock、logic delete、R、LoginHelper、T-05 invalidator | 多凭据、OAuth2、双 secret、IP 白名单、旧数据迁移 |

## 4. 要构建什么

当前用户或超管可以查询安全摘要并执行 create/reset/enable/disable/delete；并发创建最终只有一个成功响应含 secret。任何 owner 篡改、无权限访问、失效/过期凭据或 KEK 错误均失败关闭。

## 5. 实现契约

- **入口或接缝：** Spec 锁定的 credential GET/POST 路径与 T-01 credential resolver SPI。
- **输入与输出：** 应用名/过期时间/备注/action -> 安全摘要或一次性 secret DTO。
- **公共接口变化：** 新增 self/admin 凭据 HTTP 合同与一张项目自有表。
- **不变量：** owner/appKey 未删除唯一；密文和内部 Token 永不响应；status/expiry 每次解析校验。
- **状态或数据流：** Controller scope -> transactional service -> crypto/mapper -> T-05 invalidator -> safe response。
- **错误与失败行为：** 并发唯一冲突返回统一 conflict；认证失败不泄露 appKey 存在性；注销失败使写操作显式失败。
- **兼容要求：** 无旧表/凭据；`ry_vue.sql` 不修改。
- **安全与隐私要求：** KEK 只从 secret provider；密钥字节及时丢弃；日志与操作记录禁存 secret/signature/token。

## 6. 执行路线

1. 生成 SQL timestamp/Snowflake ID，先写 schema/生命周期/并发/owner 失败测试。
2. 追加符合七基础字段、中文注释、唯一索引的 DDL 与菜单/按钮 DML。
3. 实现 credential domain、版本化 KEK 加密和 resolver SPI。
4. 实现 self/admin controllers、事务状态机和 T-05 Session 注销顺序。
5. 运行 service/controller/SQL review 与 reactor 测试。

## 7. 路径访问契约

- **预计修改点/可写范围：** 新 credential owner 包、单一 Controller、测试与 SQL 末尾追加块。
- **只读上下文：** system 成熟 CRUD、profile owner 获取、T-05 invalidator。
- **共享路径：** DDL/DML 仅 T-06 追加，历史字节不得修改。
- **保留或不动：** `ry_vue.sql`、普通 Client credential、LoginUser 与浏览器 Token。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 生命周期/一次性 secret | service/controller module test | `./mvnw -pl ruoyi-modules/ruoyi-system,ruoyi-admin -am test -Dtest='*OpenApiCredential*'` | 状态机、AppKey 与 secret 合同成立 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-06.md</Path>` |
| 并发/owner/KEK 失败 | test double + MockMvc | 同上 | 单条成功、跨 owner/坏 KEK fail closed | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-06.md</Path>` |
| schema/菜单回归 | SQL diff/preflight review | 检查 append-only、七字段、唯一索引、权限/component key | fresh/upgrade 顺序和回滚说明完整 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-06.md</Path>` |

- **Workspace checks：** current-workspace/source-worktree 跑定向测试与 SQL 静态 review；集成态跑 reactor package。
- **E2E disposition：** not-required：用户批准轻量门禁，真实 MySQL 唯一竞争明确保留为 residual risk。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate。
- **Integration evidence：** implementation/source、parent before、candidate/result SHA 与父分支包含关系。

## 9. 发布、迁移与恢复

- **迁移顺序：** 先备份/追加 DDL，再 DML，再部署默认关闭代码，最后由 T-12 配置启用。
- **兼容窗口：** additive 新表/菜单；旧应用忽略它们。
- **监控信号：** 唯一冲突、KEK 版本错误、生命周期拒绝和 Session 注销失败类别。
- **回滚或前向恢复：** 默认保留 additive schema；停用 OpenAPI，修正配置/数据后前向恢复；secret 无法恢复只能 reset。
- **不可逆操作与批准点：** 生产执行 DDL/DML 与 KEK 配置必须另行批准；本 Ticket 不执行迁移。
- **收缩条件：** 不适用：无旧 credential schema。

## 10. 验收标准

- [x] `AC-008` 至 `AC-011`、`AC-024`、`AC-026` 后端合同通过。
- [x] 表具备七基础字段、正确主键/注释/唯一性，SQL 仅追加且不触碰 `ry_vue.sql`。
- [x] secret 只显示一次，所有写 API 使用 POST、安全 `@Log` 与后端 scope 校验。
- [x] Evidence 明确真实 MySQL/Redis 未验证风险，commit 与集成 SHA/Map 一致。
