---
schema_version: 3
artifact: ticket
change: 2026-08-24-oss-eli5
id: T-06
title: 实现可审计的存储边界迁移
status: done
planning_depth: deep
planning_depth_reason: 跨 Bucket 复制、校验与 sys_oss.service 原子切换涉及数据完整性、公开边界、幂等和可恢复迁移。
ready: true
risk: critical
blocked_by: [T-02, T-03, T-04]
contract_ids: [AC-017, AC-018, AC-019, AC-020]
owner: codex:/root
expected_changes:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/migration/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/mapper/SysOssMapper.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/resources/mapper/system/SysOssMapper.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysOssMigrationController.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/migration/**</Path>"
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/migration/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/mapper/SysOssMapper.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/resources/mapper/system/SysOssMapper.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysOssMigrationController.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/migration/**</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DDL.sql</Path>"
  - "<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DML.sql</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/readiness/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/system/api/OssService.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-oss/**</Path>"
shared_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/mapper/SysOssMapper.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/resources/mapper/system/SysOssMapper.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/migration/**</Path>"
shared_path_owners:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/mapper/SysOssMapper.java</Path> => T-06"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/resources/mapper/system/SysOssMapper.xml</Path> => T-06"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/migration/**</Path> => T-06"
---

# Ticket T-06: 实现可审计的存储边界迁移

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/06-storage-boundary-migration.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-06.md</Path>`

## 1. 战略与来源

- **目标：** 为 Business OSS Owner 审核清单提供可 dry-run、可审计、幂等且可回滚的 PRIVATE -> PUBLIC_READ 存储迁移。
- **可观察产出：** 操作员可以创建 dry-run、启动批次、查询进度、重试失败项、回滚已切换项和在批准后清理源对象；ossId/业务引用保持不变。
- **来源：** `US-006`、`AC-017`、`AC-018`、`AC-019`、`AC-020`、`ADR-005`、`ADR-013`。
- **当前事实：** common OssClient 已支持 copy/head/download，sys_oss.service 是归属权威；不存在迁移状态机、审计 entity 或管理入口。
- **Planning Depth 原因：** 跨系统数据复制与元数据切换有部分失败、重试、并发和误公开事故半径。

## 2. 决策状态

### 已锁定决策

- 管理入口固定在 `/resource/oss/migrations/**`：GET 查询；dry-run/start/retry/rollback/cleanup 使用 POST、权限和准确 `@Log`。
- 输入只接受已审核 ossId 集合与目标 PUBLIC_READ configKey；每个对象当前来源必须 PRIVATE，源/目标均 SERVING。
- 状态按 `PREFLIGHT -> COPIED -> CONTENT_VERIFIED -> SERVICE_SWITCHED -> ACCESS_VERIFIED -> CLEANUP_ELIGIBLE -> COMPLETED` 推进；失败记录阶段/清洗原因。
- 复制和校验完成前不修改 `sys_oss.service`；切换以行锁和 compare-and-set 保证 source service 未漂移。
- 业务读取验收失败时恢复旧 service；源对象在安全窗口和人工批准前不删除。
- 重试按 batch/item 幂等；目标同 key 内容匹配可复用，不匹配为冲突，不覆盖未知对象。
- readiness required-set contributor 暴露所有 active batch 的源/目标 configKey。

### 已采用的低影响假设

- 内容验证优先使用 size + Provider checksum/ETag；不具备可信 checksum 时执行有界流式摘要，批次大小和并发由服务端配置限制。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| 两表映射、状态机、copy/verify/CAS switch、dry-run/retry/rollback/cleanup API、readiness contributor、故障注入 | T-02 schema、OssFactory/OssClient、T-03 registry、T-04 resolver、现有 ossId/ref | 自动选择公开对象、先删源、修改业务表、生产执行、CDN purge |

## 4. 要构建什么

操作员先提交审核 ossId 清单执行 dry-run，得到来源、目标、冲突和不可迁移原因，不产生 Provider/DB 状态变化。正式启动后每项按状态机推进；复制/校验成功才 CAS 切换 service，并用 T-04 解析和匿名读取验收。失败可查询和重试，切换后失败可恢复旧 service；源清理是延迟且需批准的独立动作。

## 5. 实现契约

- **入口或接缝：** `SysOssMigrationController`、migration service/store、`SysOssMapper` CAS/lock、OssClient copy/head/read、readiness contributor。
- **输入与输出：** ossIds + target config -> dry-run report/batch id；batch/item GET -> 阶段、结果、错误和时间。
- **公共接口变化：** 新增管理员 HTTP 管理面，不进入 ruoyi-api，不提供匿名或普通业务调用。
- **不变量：** ossId/ref 不变；目标验证前 service 不变；源对象在批准前存在；单 item 同一时刻单 owner 推进。
- **状态或数据流：** preflight -> target copy -> content verify -> transactional CAS service -> access verify -> cleanup eligible -> approved cleanup。
- **错误与失败行为：** source/target readiness、复制、校验、CAS、访问验收、清理失败逐阶段记录；不得把失败对象标完成或吞错。
- **兼容要求：** 非迁移对象和既有 OSS 生命周期不受影响；配置普通编辑保护继续生效。
- **安全与隐私要求：** Controller 需专用权限；日志只记录 batch/item/configKey/阶段，不记录对象内容、Secret 或签名 URL。

## 6. 执行路线

1. 映射 T-02 两表并先建立纯状态机/幂等/权限/日志测试。
2. 实现 dry-run preflight 和只读报告，证明零 Provider/元数据 mutation。
3. 实现有界复制、内容验证和带源 service 条件的事务 CAS 切换。
4. 接入 T-04 访问验收、失败恢复、重试和 readiness active contributor。
5. 实现人工批准后的延迟 cleanup，并故障注入每个阶段。
6. 运行 unit/DB/双 Bucket integration、并发重试和既有 OSS 回归。

## 7. 路径访问契约

- **预计修改点/可写范围：** 新 migration 包/Controller、SysOss Mapper 接缝和专用测试。
- **只读上下文：** T-02 SQL、T-03 readiness、T-04 OssService 和 common-oss。
- **共享路径：** migration 包与 SysOss Mapper 由 T-06 唯一修改；T-07 只生成其 OpenAPI 投影。
- **保留或不动：** SQL、业务 Owner 表、`sys_oss_ref`、配置 CRUD 和前端页面。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| dry-run 零副作用 | service/controller test | 提交合法/冲突清单并检查 DB/Provider spy | 只返回报告，无批次外写/复制/switch | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-06.md</Path>` |
| 正常迁移 | DB + dual Bucket | 运行 `OssStorageMigrationIntegrationTest` | 内容匹配、service 切换、ossId/ref 不变、公共访问成立 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-06.md</Path>` |
| 各阶段失败/重试/回滚 | fault injection | 复制、校验、CAS、访问、cleanup 分别失败 | 阶段可查、源可用、重复执行不重复切换 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-06.md</Path>` |
| 并发与漂移 | transaction test | 并发 retry、外部 service 变化 | 单 owner 成功；CAS 冲突失败且不覆盖 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-06.md</Path>` |
| 权限与日志 | MockMvc/captured log | 无权限调用和敏感扫描 | 后端拒绝，POST 均有安全 `@Log` | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-06.md</Path>` |

- **Workspace checks：** current workspace 或 source worktree 运行状态机、Mapper/MockMvc 与静态检查；外部 E2E 只由 Lead 运行。
- **E2E disposition：** required：迁移必须在真实 MySQL 和双 Bucket candidate 状态验证复制、CAS、访问、恢复与延迟清理。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate；隔离 MySQL/MinIO，默认不执行 cleanup，禁止生产迁移。
- **Integration evidence：** implementation/source commit、candidate/result SHA、audit rows、service/ref 查询和 Bucket 内容摘要。

## 9. 发布、迁移与恢复

- **迁移顺序：** Provider/readiness 与 T-02 schema -> 默认关闭代码 -> dry-run 审核 -> 小批正式迁移 -> 安全窗口 -> 单独批准 cleanup。
- **兼容窗口：** 迁移中旧 service/源对象持续可用；SERVICE_SWITCHED 后由新 service 提供访问，失败则恢复旧 service。
- **监控信号：** batch/item 阶段计数、失败类别、重试次数、耗时、readiness 和 cleanup eligibility。
- **回滚或前向恢复：** switch 前直接重试；switch 后在源对象存在时 CAS 恢复旧 service；源已清理后只允许从目标前向恢复。
- **不可逆操作与批准点：** 生产正式迁移、rollback 和源 cleanup 均未授权；cleanup 必须在安全窗口后独立人工批准。
- **收缩条件：** 批次所有 item COMPLETED/ROLLED_BACK，无 active readiness contributor；源清理证据和业务验收完整。

## 10. 验收标准

- [x] `AC-017` 至 `AC-020` 的正常、故障、幂等与恢复矩阵通过。
- [x] dry-run 零副作用，正式迁移保持 ossId 和 `sys_oss_ref`。
- [x] 源对象未在目标/业务验收及批准前删除。
- [x] 验证矩阵记录到 `<Path>{roots.state}/specdev/changes/{change}/evidence/T-06.md</Path>`。
- [x] 实际修改未超出 writable_paths，共享路径只由 T-06 修改。
- [x] 形成非空 implementation/source commit，经 direct-parent 或 candidate 验证并记录 result SHA。
- [x] 未发生未批准偏差，Ticket、Map 与 Evidence 一致。
