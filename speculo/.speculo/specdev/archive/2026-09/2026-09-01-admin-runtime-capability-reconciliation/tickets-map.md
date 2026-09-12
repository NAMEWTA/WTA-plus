---
schema_version: 3
artifact: tickets-map
change: 2026-09-01-admin-runtime-capability-reconciliation
status: completed
---

# Tickets Map: Admin 运行能力收敛

- **Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **Ticket 目录：** `<Path>{roots.state}/specdev/changes/{change}/ticket/</Path>`
- **Evidence 目录：** `<Path>{roots.state}/specdev/changes/{change}/evidence/</Path>`
- **Goal Plan：** `<Path>{roots.state}/specdev/changes/{change}/goal-plan.md</Path>`

## 1. 目标与拆分策略

两个 Ready/Deep Ticket 共同修复 `US-001..US-006`：T-01 先交付可重复、冲突安全且具备明确前向恢复边界的数据库最终态，T-02 再通过受管发布显式启用既有 OpenAPI 能力并验收真实 Admin 体验。拆分边界按可观察发布阶段而不是前后端水平层划分；用户明确无备份的数据库迁移与最终态仍是双实例启用的真实前置，因此 `T-01 -> T-02`。

不需要 prefactor 或 expand-contract：既有 OpenAPI/Nacos 前后端能力和生成器退役源码都已存在。SQL 采用新的 append-only contract 块完成历史状态到最终态的收缩。T-02 首实例真实启用后发现 OpenAPI 自动配置未限定业务 `RequestMappingHandlerMapping`，按运行时证据将该精确接缝及回归测试纳入原 Ticket，不拆分新的产品能力。

## 2. 执行清单

| ID | Ticket | 可观察产出 | Blocked By | Depth | Risk | Ready | Owner | Contract IDs | Wave/Gate | Status |
|---|---|---|---|---|---|---|---|---|---|---|
| T-01 | `<Path>{roots.state}/specdev/changes/{change}/ticket/01-reconcile-admin-runtime-database.md</Path>` | OpenAPI/Nacos/生成器 schema/menu 收敛 | — | deep | critical | yes | codex:/root | AC-005..011,013 | W1/G1-G2 | done |
| T-02 | `<Path>{roots.state}/specdev/changes/{change}/ticket/02-enable-openapi-release-and-verify-admin.md</Path>` | 双实例 OpenAPI 显式启用与 Admin 最终体验 | T-01 | deep | critical | yes | codex:/root | AC-001..004,012,014..016 | W2/G3-G4 | done |

Ticket frontmatter 是状态、依赖、深度和路径访问契约的权威；本表是同步投影。

## 3. 依赖 DAG

```text
G0 Spec/authorization/baseline
  -> T-01 database reconciliation
       -> G1 isolated SQL contract
       -> G2 explicitly no-backup development DB final state
            -> T-02 release enablement
                 -> G3 static/runtime release readiness
                 -> G4 dual-instance + Admin browser final state
```

关键路径是 `T-01 -> T-02`。依赖边表示真实发布开始条件：T-02 的开发环境滚动不得在用户无备份批准登记、数据库硬检查和最终态之前执行。

## 4. 合同覆盖矩阵

| Contract ID | 覆盖 Ticket | 验证接缝 | 状态 | 说明 |
|---|---|---|---|---|
| AC-001 | T-02 | Spring assembly | covered | 未配置保持 default-off |
| AC-002 | T-02 | Spring assembly/package | covered | 合法配置唯一装配 |
| AC-003 | T-02 | 真实双实例 HTTP | covered | 不再业务 404 |
| AC-004 | T-02 | startup failure matrix | covered | 配置失败关闭且脱敏 |
| AC-005 | T-01 | SQL contract + MySQL | covered | 六条 OpenAPI 菜单和最终主菜单 |
| AC-006 | T-01 | SQL contract + MySQL | covered | Nacos 名称/父菜单/稳定属性 |
| AC-007 | T-01 | MySQL upgrade/replay | covered | 生成器菜单/角色关系清零 |
| AC-008 | T-01 | MySQL DDL/replay | covered | 两张生成器表删除 |
| AC-009 | T-01 | 目标 DB 诊断 | covered | 当前混合状态四项全真 |
| AC-010 | T-01 | MySQL replay | covered | 重放稳定 |
| AC-011 | T-01 | MySQL conflict sentinel | covered | 写前失败 |
| AC-012 | T-02 | Node/Compose static | covered | 双实例同源透传且无真实 secret |
| AC-013 | T-01 | SQL contract + DB query | covered | 不新增普通角色授权 |
| AC-014 | T-02 | Vitest/build/browser | covered | component 可解析且生成器关闭 |
| AC-015 | T-02 | takeover rollout | covered | 双实例逐个验证 |
| AC-016 | T-02 | 登录浏览器 | covered | 四项用户体验最终态 |

无 `uncovered` 或 `deferred` 合同。

## 5. 并行与路径所有权

- Goal Plan 采用 current workspace，两个 Ticket 严格串行，当前同时 implementation writer 固定为 1。
- config 的 implementation subagent 上限为 3，但本次协作约束和用户未要求派单，因此 Lead 自行实施，不创建 subagent。
- T-01 独占 backend NAMEWTA DDL/DML；T-02 独占 parent release Compose/env/test，并拥有 OpenAPI HandlerMapping 选择、缺失凭据语义及其 Admin 回归测试。两者无 writable 交集。
- Lead 是 SpecDev 状态、Evidence、direct-parent、E2E 与开发环境接管的唯一 owner。

| Ticket A | Ticket B | Writable 交集 | 真实依赖 | 处理 |
|---|---|---|---|---|
| T-01 | T-02 | 无 | 是 | 严格按 T-01 database Gate 后执行 T-02 rollout |

| 共享路径 | 唯一 Owner | 约束 |
|---|---|---|
| `<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DDL.sql</Path>`、`<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DML.sql</Path>` | T-01 | 仅追加新块，不改历史 |
| `<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/openapi/credential/OpenApiCredentialSqlContractTest.java</Path>` | T-01 | 以实施前 backend HEAD 完整 SQL 重建不可变 append-only 前缀门 |
| `<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/migration/password/PasswordMigrationUnitTest.java</Path>` | T-01 | 只同步实施前 HEAD 已失真的固定 15,370 字节 DML 前缀哈希，保持长度和算法不变 |
| `<Path>release-artifacts/docker/docker-compose-backend.yml</Path>`、`<Path>release-artifacts/.env.example</Path>`、`<Path>release-artifacts/tests/release-config.test.mjs</Path>` | T-02 | default-off、双实例同源、无真实 secret |
| `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-openapi/src/main/java/org/dromara/common/openapi/config/OpenApiAutoConfiguration.java</Path>`、`<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/openapi/assembly/OpenApiAssemblyContextTest.java</Path>` | T-02 | 明确选择业务 MVC mapping，并覆盖 Actuator 第二候选回归 |
| `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/openapi/SysOpenApiCredentialController.java</Path>`、`<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/openapi/credential/SysOpenApiCredentialControllerContractTest.java</Path>` | T-02 | 缺失凭据使用业务 404，使 UI 区分空状态与能力不可用 |

## 6. Gate、Wave 与集成点

| Wave | 可执行 Ticket | 进入条件 | 行为/集成 Gate |
|---|---|---|---|
| W1 | T-01 | G0 Spec/授权/clean child baseline | G1 SQL 矩阵与 commit；G2 用户明确无备份的开发库最终态 |
| W2 | T-02 | G2 关闭 | G3 release/Spring/frontend gates；G4 双实例与浏览器最终验收 |

Goal Plan 固定 `ticket_workspace_policy: current`、`integration_gate: direct-parent`、Lead `codex:/root`。implementation commit 与本地 direct-parent 已授权；开发环境数据库/配置/滚动动作仅在 Ticket 批准点满足后授权。远程 push、CDE、生产和 cleanup 未授权。

## 7. 横切契约与风险

- **数据：** 固定 ID 精确收敛；冲突先失败；用户已豁免备份，生成器物理删除前仍必须为 0 行且对象身份匹配。
- **安全：** OpenAPI 默认关闭；真实 KEK 只在权限受限私密配置；普通角色不自动授权；输出全部脱敏。
- **兼容：** HTTP/path/component/permission 不变；fresh 执行完整 SQL，upgrade 只执行新块；默认部署无需 secret。
- **发布：** 数据库先于 backend enable；双实例逐个滚动；第一个失败不推进；同机 CDE 始终排除。
- **恢复：** 数据库无本 change 备份，只允许停止 rollout 并前向修复；OpenAPI rollout 可先关闭开关并恢复上一实例配置。

## 8. 同步规则

- Ticket 状态变化后同步执行清单；Ticket frontmatter 为局部状态权威。
- Goal Plan 是 Wave、Gate、Lead 和授权边界的编排权威。
- 每个 Ticket 必须形成非空 implementation commit、direct-parent result 和 Lead Evidence。
- 依赖、合同、路径或状态变化后重新运行 Speculo validator。
- 内部工件继续使用完整根变量 Path 标签；secret、连接凭据和数据正文禁止写入工件。
