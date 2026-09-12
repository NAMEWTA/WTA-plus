---
schema_version: 3
artifact: tickets-map
change: 2026-08-24-oss-eli5
status: completed
---

# Tickets Map: OSS 公共桶与私有桶增强

- **Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **Ticket 目录：** `<Path>{roots.state}/specdev/changes/{change}/ticket/</Path>`
- **Evidence 目录：** `<Path>{roots.state}/specdev/changes/{change}/evidence/</Path>`
- **推荐下一步：** `<Path>{roots.workflows}/specdev/P-goal-plan/P-goal-plan.md</Path>`

## 1. 目标与拆分策略

八张 Ready/Deep Ticket 共同增强现有 OSS：公共对象可使用稳定匿名读 URL，私有对象只返回有期限的服务端签名 URL；上传由服务端 uploadPolicy 路由到固定存储配置，配置、Provider 能力、存量对象迁移和最终发布均失败关闭。

- T-01 是 expand prefactor：先建立 PRIVATE/PUBLIC_READ、命名 TTL 和 uploadPolicy 存储绑定的共享配置合同，不激活公共流量。
- T-02 是 schema/config migrate：建立保守的访问类型编码、配置变更限制与可审计迁移基线。
- T-03 是 observe/gate：以只读 Provider 诊断和 readiness 阻止错误 Bucket/Policy 对外服务。
- T-04/T-05 是两个可并行垂直切片：分别完成访问 URL 解析和上传存储路由。
- T-06 是 data migrate：复制、校验并原子切换 `sys_oss.service`，源对象清理由独立批准控制。
- T-07 是 consumer/contract migrate：配对更新 OpenAPI 生成物、前端 transport 和 OSS 配置管理页。
- T-08 是 release gate：使用真实 MySQL、MinIO、HTTP 与浏览器链路关闭安全、兼容和恢复证据。

本 Map 固化规划拓扑。用户于 2026-08-31 确认全部 Ticket、选择独立 worktree，并授权 implementation commit 与 Lead-owned 本地 candidate integration/parent update；source cleanup、远程动作、部署和生产迁移仍未授权。

## 2. 执行清单

| ID | Ticket | 可观察产出 | Blocked By | Depth | Risk | Ready | Owner | Contract IDs | Wave/Gate | Status |
|---|---|---|---|---|---|---|---|---|---|---|
| T-01 | `<Path>{roots.state}/specdev/changes/{change}/ticket/01-expand-oss-access-configuration.md</Path>` | 双类型配置、命名 TTL、uploadPolicy 存储绑定 | — | deep | high | yes | codex:/root | AC-003,007,013,023 | W1/G-10 | done |
| T-02 | `<Path>{roots.state}/specdev/changes/{change}/ticket/02-safe-oss-config-upgrade.md</Path>` | 配置治理、保守升级与迁移台账 schema | T-01 | deep | high | yes | codex:/root | AC-013,017,018 | W2/G-20 | done |
| T-03 | `<Path>{roots.state}/specdev/changes/{change}/ticket/03-provider-readiness.md</Path>` | Provider 只读诊断与 readiness 门禁 | T-01,T-02 | deep | critical | yes | codex:/root | AC-005,014,016,021 | W3/G-30 | done |
| T-04 | `<Path>{roots.state}/specdev/changes/{change}/ticket/04-unified-access-url.md</Path>` | 公共稳定 URL、私有命名签名 URL 与统一解析门面 | T-01,T-03 | deep | critical | yes | codex:/root | AC-004,006-012,015,021,024 | W4/G-40 | done |
| T-05 | `<Path>{roots.state}/specdev/changes/{change}/ticket/05-upload-policy-storage-routing.md</Path>` | 上传按服务端策略进入固定公共/私有存储 | T-01,T-03 | deep | high | yes | codex:/root | AC-001-003,022,023 | W4/G-40 | done |
| T-06 | `<Path>{roots.state}/specdev/changes/{change}/ticket/06-storage-boundary-migration.md</Path>` | dry-run、复制校验、原子切换、重试/回滚/清理 | T-02,T-03,T-04 | deep | critical | yes | codex:/root | AC-017-020 | W5/G-50 | done |
| T-07 | `<Path>{roots.state}/specdev/changes/{change}/ticket/07-openapi-admin-oss-ui.md</Path>` | OpenAPI、前端 transport 与 OSS 配置 UI 配对更新 | T-02,T-04,T-06 | deep | medium | yes | codex:/root | AC-003,010,012-014,018 | W6/G-60 | done |
| T-08 | `<Path>{roots.state}/specdev/changes/{change}/ticket/08-dual-bucket-release-gate.md</Path>` | 双 Bucket E2E、运维 Runbook 与发布批准点 | T-04,T-05,T-06,T-07 | deep | high | yes | codex:/root | AC-005,006,008,009,016,019,020,022 | W7/G-70 | done |

Ticket frontmatter 是状态、依赖、深度和路径访问契约的权威；本表是同步投影，不得独立形成另一套状态。

## 3. 依赖 DAG

```text
T-01 [access/config prefactor]
  └─→ T-02 [safe config/schema upgrade]
        └─→ T-03 [Provider diagnosis + readiness]
              ├─→ T-04 [unified public/private URL] ─→ T-06 [audited migration] ─→ T-07 [OpenAPI + UI] ─┐
              └─→ T-05 [uploadPolicy storage routing] ────────────────────────────────────────────────┤
                                                                                                      └─→ T-08 [release gate]
```

T-04 与 T-05 在 T-03 关闭后可并行。T-06 必须等待统一访问解析可用，确保 `sys_oss.service` 切换后立即通过同一门面解析；T-07 等待后端 wire format 与迁移管理接口稳定；T-08 是全部运行路径的唯一最终汇合点。

## 4. 合同覆盖矩阵

| Contract ID | 覆盖 Ticket | 验证接缝 | 状态 | 说明 |
|---|---|---|---|---|
| AC-001 | T-05 | uploadPolicy service test | covered | 门户上传只落公共配置 |
| AC-002 | T-05 | uploadPolicy service test | covered | 权限上传只落私有配置 |
| AC-003 | T-01,T-05,T-07 | config/transport matrix | covered | 客户端不能选择 Bucket、configKey 或访问类型 |
| AC-004 | T-04 | `OssService` contract test | covered | `sys_oss.service` 是访问解析唯一存储来源 |
| AC-005 | T-03,T-08 | readiness + real Provider E2E | covered | readiness 失败时上传和 URL 解析均拒绝 |
| AC-006 | T-04,T-08 | anonymous HTTP E2E | covered | 公共 URL 可匿名 GET/HEAD |
| AC-007 | T-01,T-04 | lifecycle URL test | covered | 生产公共配置要求稳定 domainUrl |
| AC-008 | T-04,T-08 | expiry E2E | covered | 私有签名 URL 在 TTL 内有效、过期失效 |
| AC-009 | T-04,T-08 | repeated resolve E2E | covered | 重新解析产生新的有效签名 URL |
| AC-010 | T-04,T-07 | controller/transport test | covered | 管理列表不批量返回签名 URL |
| AC-011 | T-04 | service/controller negative test | covered | 公共对象拒绝显式 private presign |
| AC-012 | T-04,T-07 | permission + route test | covered | 无通用匿名 ossId 签名端点 |
| AC-013 | T-01,T-02,T-07 | enum/schema/UI test | covered | 仅 PRIVATE/PUBLIC_READ 且默认私有 |
| AC-014 | T-03,T-07 | diagnostics/UI test | covered | 诊断可观察但不修改 Provider Policy |
| AC-015 | T-04 | Java API compatibility test | covered | 保留旧 `OssService` 方法 |
| AC-016 | T-03,T-08 | provider matrix | covered | 不支持诊断的 Provider 为 UNVERIFIED/不可服务 |
| AC-017 | T-02,T-06 | migration baseline/dry-run | covered | 存量对象全部按私有解释 |
| AC-018 | T-02,T-06,T-07 | config/migration API test | covered | 有对象的配置不得原地改变 Bucket/类型 |
| AC-019 | T-06,T-08 | copy/check/switch E2E | covered | 复制校验成功后原子切换 service |
| AC-020 | T-06,T-08 | retry/rollback/cleanup E2E | covered | 迁移幂等且源清理需要独立批准 |
| AC-021 | T-03,T-04 | readiness propagation test | covered | URL 解析消费实时 readiness |
| AC-022 | T-05,T-08 | in-flight upload regression | covered | Ticket 冻结目标 configKey/Bucket |
| AC-023 | T-01,T-05 | configuration/request whitelist test | covered | storage 路由完全由服务端策略决定 |
| AC-024 | T-04 | structured result test | covered | 统一门面返回 URL、类型与到期元数据 |

无 `uncovered` 或 `deferred` 合同。

## 5. 并行与路径所有权

- Ticket owner 统一为 Lead `codex:/root`；implementation owner 由执行期动态派单，Lead 始终拥有状态、Evidence、E2E 与集成。
- workspace 固定为 `required`，integration 固定为 `candidate-merge`；本 Plan 只记录逻辑 workspace，不在规划阶段创建实际 worktree 或 candidate branch。
- review/research/test-observation 保持只读，不写项目路径或 SpecDev 状态。
- `writable_paths` 以 Ticket frontmatter 为准；共享文件只允许下表唯一 owner 写入。

| Ticket A | Ticket B | Writable 交集 | 真实依赖 | 处理 |
|---|---|---|---|---|
| T-04 | T-05 | 无 | 否 | W4 可并行，共同只读消费 T-03 readiness |
| T-05 | T-06 | 无 | 否 | T-06 可在 T-04 关闭后开始，不需要等待 T-05 |
| T-06 | T-07 | 无 | 是 | 串行，T-07 消费迁移管理 HTTP 合同 |
| T-07 | T-08 | 无 | 是 | 串行，T-08 只读验证最终前后端合同 |

| 共享路径或边界 | 唯一 Owner | 约束 |
|---|---|---|
| `AccessPolicy`、OSS 配置模型、lifecycle/upload properties、`application.yml` | T-01 | 后续 Ticket 只读消费双类型和策略合同 |
| OSS config domain/service/controller、NAMEWTA DDL/DML | T-02 | SQL 只追加；不改上游 `ry_vue.sql`，不执行生产 SQL |
| common Provider capabilities/diagnostics、system readiness | T-03 | 只读探测，禁止自动修改 Bucket Policy 或创建公共测试对象 |
| `OssService`、lifecycle/object store、`SysOssServiceImpl`、访问 Controller | T-04 | URL 类型与授权语义的唯一 Java/HTTP owner |
| upload object store/service | T-05 | T-01 配置只读，客户端请求合同不扩展存储字段 |
| migration 包、`SysOssMapper`、migration Controller | T-06 | 原子切换和迁移状态唯一 owner；DDL/DML 只读消费 T-02 |
| OpenAPI snapshot/generated、system resource transport、OSS 配置页 | T-07 | OpenAPI 只经 tooling 更新，禁止手改 generated 文件 |
| 既有 MinIO integration test、system-resources E2E、OSS Runbook | T-08 | 只增加发布证据，不修改实现语义 |

## 6. Gate、Wave 与集成点

| Wave | 可执行 Ticket | 进入条件 | 行为/集成 Gate |
|---|---|---|---|
| W1 | T-01 | Tickets ready 且实现获授权 | 双类型、TTL、storageConfigKey 配置矩阵通过 |
| W2 | T-02 | T-01 accepted | 保守编码、CRUD 限制与 additive SQL 通过 |
| W3 | T-03 | T-02 accepted | Provider 匿名 GET/HEAD/写诊断与 readiness 矩阵通过 |
| W4 | T-04,T-05 | T-03 accepted | 公私 URL 与上传路由两个垂直切片通过 |
| W5 | T-06 | T-04 accepted | dry-run、复制校验、原子切换与恢复通过 |
| W6 | T-07 | T-06 accepted | OpenAPI 无 drift，前端 transport/UI 与后端配对通过 |
| W7 | T-08 | T-04/T-05/T-06/T-07 accepted | 真实双 Bucket E2E、Runbook 与人工批准点关闭 |

Goal Plan 已固定 `required + candidate-merge`：每张 Ticket 使用独立 source worktree，T-04/T-05 可并行实现，所有 candidate 由 Lead 按 `T-01→T-02→T-03→T-04→T-05→T-06→T-07→T-08` 串行集成。实现 agent 上限 3，integration attempt 不设次数上限但持续累计审计并服从停止/偏差控制；以 `<Path>{roots.state}/specdev/changes/{change}/goal-plan.md</Path>` 为编排权威。

每张实现票必须记录 implementation/source commit、parent before、适用 candidate/result SHA 和父分支包含关系。T-08 只有在真实 MySQL/MinIO/浏览器环境实际执行后才能声明通过；环境缺失必须记为未验证，不得用 mock 代替发布结论。

## 7. 横切契约与风险

- **安全：** 公共仅匿名 GET/HEAD，任何匿名写都拒绝；私有签名 URL 是短期 bearer URL，日志不得记录完整 URL、签名、AccessKey 或 SecretKey。
- **权威来源：** `sys_oss.service` 唯一确定对象存储配置；调用方不能提交 configKey、Bucket、访问类型或 TTL。
- **Provider：** 应用只诊断并失败关闭，不负责修改外部 Bucket Policy；不支持安全诊断的 Provider 不进入服务态。
- **数据：** 访问类型物理编码固定为 `0=PRIVATE`、`2=PUBLIC_READ`；旧值 `0/1/2` 和未知值升级时均按 PRIVATE 解释，再由显式迁移改变边界。
- **迁移：** 配置有对象引用时禁止原地改变 Bucket/类型；迁移按 dry-run、复制、校验、原子切换、观察、批准清理执行并保留逐项审计。
- **兼容：** 旧 Java 方法和旧上传 HTTP 合同保留；升级默认仍私有；管理列表不批量生成签名 URL。
- **前端：** App 复用 system web-domain/domain；本变更不新建门户 App，不提供客户端可选 TTL 或 Bucket 控件。
- **恢复：** 公共 readiness 失败时停止新上传/URL 解析；数据已复制但未切换可安全重试，已切换优先前向修复或依据审计台账回滚 service。
- **残余风险：** 生产 Bucket Policy、DNS/CDN、自定义域名和 Provider 权限属于外部状态，必须通过 T-08 Runbook 和人工批准确认。

## 8. 同步规则

- Ticket 状态变化后同步执行清单；
- Ticket ID、路径、依赖或 frontmatter 不一致时，以 Ticket 文件为权威并修复本 Map；
- Goal Plan 存在时，Wave、Gate、owner 和 workspace 以 `<Path>{roots.state}/specdev/changes/{change}/goal-plan.md</Path>` 为编排权威；
- 依赖、合同覆盖或路径所有权变化后运行 `<Path>{roots.workflows}/specdev/common/tools/validate-specdev.mjs</Path>`；
- 内部工件不得使用相对 Markdown 链接；
- `P-goal-plan` 不自动开始实现；`I-implement` 可依据已记录的本地实现/集成授权启动，清理、远程操作、部署和生产迁移仍需另行明确批准。
