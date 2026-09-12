---
schema_version: 3
artifact: ticket
change: 2026-08-24-oss-eli5
id: T-03
title: 建立 Provider 只读诊断与 readiness 门禁
status: done
planning_depth: deep
planning_depth_reason: Provider 匿名读写能力决定数据是否误公开，且 readiness 是多个实现 Ticket 共用的安全核心路径。
ready: true
risk: critical
blocked_by: [T-01, T-02]
contract_ids: [AC-005, AC-014, AC-016, AC-021]
owner: codex:/root
expected_changes:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-oss/src/main/java/org/dromara/common/oss/client/OssClient.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-oss/src/main/java/org/dromara/common/oss/client/AbstractOssClientImpl.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-oss/src/main/java/org/dromara/common/oss/model/OssClientCapabilities.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-oss/src/main/java/org/dromara/common/oss/model/OssAccessDiagnostic.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/readiness/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/upload/OssUploadDiagnostics.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/listener/OssConfigChangeListener.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/runner/SystemApplicationRunner.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/readiness/**</Path>"
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-oss/src/main/java/org/dromara/common/oss/client/OssClient.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-oss/src/main/java/org/dromara/common/oss/client/AbstractOssClientImpl.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-oss/src/main/java/org/dromara/common/oss/model/OssClientCapabilities.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-oss/src/main/java/org/dromara/common/oss/model/OssAccessDiagnostic.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/readiness/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/upload/OssUploadDiagnostics.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/listener/OssConfigChangeListener.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/runner/SystemApplicationRunner.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/readiness/**</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-oss/src/main/java/org/dromara/common/oss/factory/OssFactory.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/upload/OssUploadProperties.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/mapper/SysOssMapper.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/mapper/SysOssConfigMapper.java</Path>"
shared_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-oss/src/main/java/org/dromara/common/oss/client/OssClient.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-oss/src/main/java/org/dromara/common/oss/client/AbstractOssClientImpl.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-oss/src/main/java/org/dromara/common/oss/model/OssClientCapabilities.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/readiness/**</Path>"
shared_path_owners:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-oss/src/main/java/org/dromara/common/oss/client/OssClient.java</Path> => T-03"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-oss/src/main/java/org/dromara/common/oss/client/AbstractOssClientImpl.java</Path> => T-03"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-oss/src/main/java/org/dromara/common/oss/model/OssClientCapabilities.java</Path> => T-03"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/readiness/**</Path> => T-03"
---

# Ticket T-03: 建立 Provider 只读诊断与 readiness 门禁

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/03-provider-readiness.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-03.md</Path>`

## 1. 战略与来源

- **目标：** 以只读 Provider 诊断建立 configKey 可服务状态，使公共/私有声明、域名与真实匿名能力不一致时 fail-closed。
- **可观察产出：** Actuator readiness 按 configKey 返回非敏感结果；只有必检且通过诊断的配置可被 URL 解析、上传或迁移选择。
- **来源：** `US-002`、`US-005`、`AC-005`、`AC-014`、`AC-016`、`AC-021`、`ADR-007`、`ADR-008`。
- **当前事实：** `OssUploadDiagnostics` 只检查默认 Bucket 的 CORS/Lifecycle并记录日志；无可服务 registry，也未核验匿名读取或公开域名。
- **Planning Depth 原因：** 该门禁直接控制是否允许公开或签发访问，无法确认即放行会造成关键安全事故。

## 2. 决策状态

### 已锁定决策

- 必检集合为唯一默认配置、启用 uploadPolicy 引用、现有 `sys_oss.service` 引用和 contributor 提供的进行中迁移源/目标配置。
- Provider 探测只读取 Policy/能力和部署提供的诊断对象；不得修改 Bucket Policy、创建公开测试对象或输出 Secret。
- PUBLIC_READ 必须证明匿名 GET/HEAD 可用且匿名写被拒绝；PRIVATE 必须证明匿名读取被拒绝。
- Provider 不支持安全的只读证明、超时、权限不足或结果矛盾时状态为不可服务，不以警告后继续。
- 生产 PUBLIC_READ 缺少有效 domainUrl 直接不可服务；开发回退只在 T-01 显式开关开启时允许。
- readiness registry 提供窄查询与 contributor SPI；URL、上传、迁移消费者不得每次同步重新探测 Provider。

### 已采用的低影响假设

- S3-compatible 基线优先使用只读 Bucket Policy/canary HEAD；具体 Provider 无法实现时返回 `UNVERIFIED` 并 fail-closed。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| common-oss 诊断能力、必检集合、serviceability registry、Actuator health、启动/配置刷新 | `OssFactory.instance(configKey)`、现有配置 Mapper、Spring Health | 修改 Policy、同步每请求探测、URL/上传路由、云厂商自动修复 |

## 4. 要构建什么

应用启动或 OSS 配置提交后，系统重新计算必检配置并执行有界只读诊断。每个结果进入内存服务状态快照和 readiness payload。消费者按 configKey 查询快照；缺失、过期、失败或未验证状态一律拒绝服务，不回退默认配置或旧缓存。

## 5. 实现契约

- **入口或接缝：** common `OssClient` 诊断方法、system readiness registry/contributor、配置事件和 Actuator health。
- **输入与输出：** configKey + 声明类型 +环境 -> `SERVING/NOT_SERVING` 与清洗原因；必检集合 -> readiness 总状态。
- **公共接口变化：** 扩展 common-oss Provider 能力，不改变 ruoyi-api 或匿名 HTTP 面。
- **不变量：** 不修改外部 Policy；NOT_SERVING 不进入数据面；未引用占位配置不单独拉低整体 readiness。
- **状态或数据流：** DB/static contributors -> required set -> bounded diagnostic -> immutable registry snapshot -> health/consumer query。
- **错误与失败行为：** 配置缺失、domain 无效、探测不支持/超时/异常均 NOT_SERVING；日志只含 configKey 和清洗类别。
- **兼容要求：** 现有 CORS/Lifecycle 诊断保留为辅助项，但不能替代访问 Policy 门禁。
- **安全与隐私要求：** 不记录访问密钥、Policy 全文、对象内容或签名查询串；健康端点不泄露 endpoint 内部凭据。

## 6. 执行路线

1. 建立 fake Provider 和 health 测试，覆盖正确、误公开、匿名写、超时、权限不足及未引用配置。
2. 扩展 common-oss 只读诊断结果与 capability，明确无法证明的失败语义。
3. 实现 required-set contributor、registry、启动/配置刷新和 Actuator readiness。
4. 将旧上传诊断接入新快照，确保不再只记录默认 Bucket 告警。
5. 运行 unit/context、MinIO 诊断 E2E 和后端聚合回归。

## 7. 路径访问契约

- **预计修改点/可写范围：** common Provider 诊断、system readiness/runner/listener 与专用测试。
- **只读上下文：** OssFactory、T-01 properties、对象/配置 Mapper。
- **共享路径：** Provider 诊断 API 和 readiness package 由 T-03 唯一修改；T-04/T-05/T-06 只消费。
- **保留或不动：** Bucket Policy、业务对象、SQL、ruoyi-api 和公开 Controller。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正确 public/private 配置 | readiness unit/context | 运行 `OssStorageReadiness*Test` | 必检配置 SERVING，整体 readiness UP | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-03.md</Path>` |
| Policy/domain/超时异常 | fake Provider matrix | 同套件注入各失败 | 配置 NOT_SERVING，原因非敏感且无 mutation 调用 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-03.md</Path>` |
| 真实匿名能力 | MinIO integration | Lead 运行 `OssStorageReadinessMinioIntegrationTest` | public GET/HEAD、private 拒读、匿名写拒绝与快照一致 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-03.md</Path>` |
| 未引用占位配置 | required-set unit | 添加失败占位配置但不引用 | 单项可观察，整体 readiness 不被无关配置阻断 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-03.md</Path>` |

- **Workspace checks：** 在 current workspace 或 source worktree 运行 fake/context 非 E2E 测试和 reactor package。
- **E2E disposition：** required：真实 Provider 匿名边界是 readiness 声明成立的必要证据。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate；使用隔离 MinIO 双 Bucket 与部署 canary，不在 source worktree 声明通过。
- **Integration evidence：** implementation/source commit、candidate/result SHA、health payload、HTTP 状态与零 mutation spy。

## 9. 发布、迁移与恢复

- **迁移顺序：** Provider/Bucket/canary 先准备，T-02 数据基线后生成 required set，再让消费者接入 registry。
- **兼容窗口：** readiness 上线后旧的“只告警继续用”行为立即收缩；未通过配置不得服务。
- **监控信号：** configKey 状态、失败类别、最近探测时间、required-set 来源和 readiness 总状态。
- **回滚或前向恢复：** 通过修正 Provider/domain 后刷新快照前向恢复；不得通过放宽 fail-closed 回滚。
- **不可逆操作与批准点：** 无外部写；生产 Provider/域名变更仍由部署流程另行批准。
- **收缩条件：** URL、上传和迁移调用点均只通过 registry 判断可服务，旧 warning-only 分支调用点为零。

## 10. 验收标准

- [x] `AC-005`、`AC-014`、`AC-016`、`AC-021` 的静态、失败和真实 Provider 场景通过。
- [x] 不存在 Bucket Policy 修改或公开测试对象创建调用。
- [x] readiness 原因可操作且不包含 Secret、Policy 全文或签名 URL。
- [x] 验证矩阵记录到 `<Path>{roots.state}/specdev/changes/{change}/evidence/T-03.md</Path>`。
- [x] 实际修改未超出 writable_paths，共享路径只由 T-03 修改。
- [x] 形成非空 implementation/source commit，经 candidate 验证并记录 result SHA。
- [x] 未发生未批准偏差，Ticket、Map 与 Evidence 一致。
