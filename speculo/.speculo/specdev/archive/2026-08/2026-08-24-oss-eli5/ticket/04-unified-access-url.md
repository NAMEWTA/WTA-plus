---
schema_version: 3
artifact: ticket
change: 2026-08-24-oss-eli5
id: T-04
title: 统一解析公共与私有访问 URL
status: done
planning_depth: deep
planning_depth_reason: 扩展 ruoyi-api 公共合同并改变下载授权语义，错误实现会泄露私有对象或生成不可控 bearer URL。
ready: true
risk: critical
blocked_by: [T-01, T-03]
contract_ids: [AC-004, AC-006, AC-007, AC-008, AC-009, AC-010, AC-011, AC-012, AC-015, AC-021, AC-024]
owner: codex:/root
expected_changes:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/system/api/OssService.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/service/OssLifecycleManager.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/provider/OssObjectStore.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/provider/DefaultOssObjectStore.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/exception/OssLifecycleError.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysOssServiceImpl.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysOssController.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/notify/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/access/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/lifecycle/OssLifecycleManagerUnitTest.java</Path>"
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/system/api/OssService.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/service/OssLifecycleManager.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/provider/OssObjectStore.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/provider/DefaultOssObjectStore.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/exception/OssLifecycleError.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysOssServiceImpl.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysOssController.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/notify/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/access/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/lifecycle/OssLifecycleManagerUnitTest.java</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-oss/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/readiness/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/config/OssLifecycleProperties.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-translation/src/main/java/org/dromara/common/translation/core/impl/OssUrlTranslationImpl.java</Path>"
shared_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/system/api/OssService.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/service/OssLifecycleManager.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysOssServiceImpl.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysOssController.java</Path>"
shared_path_owners:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/system/api/OssService.java</Path> => T-04"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/service/OssLifecycleManager.java</Path> => T-04"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysOssServiceImpl.java</Path> => T-04"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysOssController.java</Path> => T-04"
---

# Ticket T-04: 统一解析公共与私有访问 URL

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/04-unified-access-url.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-04.md</Path>`

## 1. 战略与来源

- **目标：** 让所有跨模块调用通过一个安全入口按 `sys_oss.service` 解析公共稳定 URL 或私有短时 URL。
- **可观察产出：** `resolveAccessUrl` 返回 accessType/url/expiresAt/fileName；管理下载端点返回同一类型化结果；旧查询方法签名保持并委托统一解析。
- **来源：** `US-002`、`US-003`、`US-004`、`US-007`、`AC-004`、`AC-006` 至 `AC-012`、`AC-015`、`AC-021`、`AC-024`、`ADR-003`、`ADR-004`、`ADR-009`、`ADR-010`。
- **当前事实：** 生命周期层对全部对象签 2 分钟 URL；`selectUrlByIds/selectByIds` 重复签名；管理列表已隐藏 URL；没有统一结构化访问类型。
- **Planning Depth 原因：** ruoyi-api、授权边界、URL 生命周期和兼容调用同时变化。

## 2. 决策状态

### 已锁定决策

- `OssAccessUrl(accessType,url,expiresAt,fileName)` 为推荐跨模块结果；PUBLIC 的 expiresAt 必须 null，PRIVATE 必须等于实际签名过期时间。
- `resolveAccessUrl(Long)` 默认私有 TTL 2 分钟；显式 `presignDownload(Long,String)` 只接受服务端命名策略。
- `presignDownload(Long)` 和命名重载遇到 PUBLIC 对象明确拒绝。
- `selectUrlByIds` 保持不存在对象失败；`selectByIds` 保持过滤不存在对象；二者按对象类型调用统一解析。
- 管理列表/listByIds 继续 URL=null；只有有权限的 download-url 端点生成访问结果。
- 不新增匿名 ossId 查询、公开目录或业务 ACL；调用方必须先完成业务权限校验。
- 对象缺失、PENDING、配置缺失/NOT_SERVING 时无 URL，且不回退 `sys_oss.url` 或默认配置。

### 已采用的低影响假设

- accessType 使用稳定字符串 `PUBLIC/PRIVATE`；公共 URL 通过已验证配置的 domainUrl 与结构化编码 objectKey 生成。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| ruoyi-api 结果/方法、system resolver、公共 URL、命名 TTL、兼容委托、管理端点与调用点迁移 | `sys_oss.service`、readiness registry、OssClient 签名、生命周期 PENDING 检查 | 匿名平台端点、业务 ACL、签名 URL持久化、门户页面、Provider Policy 修改 |

## 4. 要构建什么

业务 Owner 授权后传入 ossId。Resolver 读取对象与其 service，要求配置 SERVING，再按配置类型生成稳定公共 URL 或短时私有签名。管理下载接口复用相同结果；公共业务可发布稳定 URL，但对象发现仍由业务记录控制。强制私有调用若收到公共对象立即失败。

## 5. 实现契约

- **入口或接缝：** `OssService.resolveAccessUrl`、两个 `presignDownload`、旧批量方法和 `/resource/oss/{ossId}/download-url`。
- **输入与输出：** ossId/可选服务端策略名 -> `OssAccessUrl`；无效状态 -> typed OSS failure，无 URL。
- **公共接口变化：** additive ruoyi-api 方法/record；旧签名保留；HTTP 响应增加 accessType 且 expiresAt 可空。
- **不变量：** 类型只来自 service 对应配置；公共不签名；私有不生成稳定原始地址；URL 不入库或长期缓存。
- **状态或数据流：** require object/PENDING -> resolve service config -> require SERVING -> type dispatch -> URL result。
- **错误与失败行为：** 未知/禁用命名策略、PUBLIC 强制签名、缺对象/配置/readiness、PENDING 均稳定失败且不泄露元数据。
- **兼容要求：** 旧方法源码/二进制签名保留；通知等私有调用显式使用强制私有方法；翻译调用无需依赖 system 内部类型。
- **安全与隐私要求：** Service 不绕过 Business Owner 授权；完整签名查询串不进入日志、持久化或 Evidence。

## 6. 执行路线

1. 先建立 ruoyi-api/System 合同测试，覆盖 PUBLIC/PRIVATE、PENDING、NOT_SERVING、强制私有拒绝和混合批量。
2. additive 扩展 OssService 结果与方法，保留旧方法签名。
3. 在 lifecycle/provider 接缝实现稳定公共 URL、可控时钟 TTL 和 readiness 检查。
4. 将 SysOssService、管理端点与已知调用点委托统一入口，保持管理列表 URL=null。
5. 运行单元/合同、真实 Provider 过期验证、调用点/匿名路由扫描和 reactor 回归。

## 7. 路径访问契约

- **预计修改点/可写范围：** ruoyi-api OssService、system lifecycle/provider/service/controller、通知调用与专用测试。
- **只读上下文：** T-01 配置、T-03 readiness、common-oss 与 translation 调用。
- **共享路径：** OssService、LifecycleManager、SysOssServiceImpl、SysOssController 由 T-04 唯一修改。
- **保留或不动：** 业务表、`sys_oss_ref`、前端、SQL 和匿名路由。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 公共/私有解析 | unit/contract | 运行 `OssAccessUrl*Test` 与 `OssLifecycleManagerUnitTest` | PUBLIC 稳定/null expiry；PRIVATE 实际 expiry | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-04.md</Path>` |
| PENDING/缺配置/强制私有公共对象 | negative unit | 同套件失败矩阵 | typed failure，无 fallback/URL | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-04.md</Path>` |
| 旧方法与管理列表 | service/controller contract | 混合 ids、缺失 id、列表 URL=null、权限测试 | 保持既有过滤/失败与管理授权 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-04.md</Path>` |
| 签名有效期和匿名边界 | Provider integration | Lead 运行 `OssAccessUrlMinioIntegrationTest` | 私有有效期内成功、过期拒绝；公共 URL 无签名 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-04.md</Path>` |
| 禁止能力 | repository scan | 扫描匿名 OSS Controller、URL 持久化与旧独立签名分支 | 不存在新增匿名发现面或第二套分类逻辑 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-04.md</Path>` |

- **Workspace checks：** current workspace 或 source worktree 运行定向单元/MockMvc、调用点扫描和 reactor package。
- **E2E disposition：** required：真实签名有效期、公共稳定 URL 和私有原始拒绝必须跨 Provider 边界证明。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate；隔离 MinIO 双 Bucket，可控短 TTL，不在 source worktree 声明 E2E。
- **Integration evidence：** implementation/source commit、candidate/result SHA、HTTP 状态、expiresAt 对照和父分支包含关系。

## 9. 发布、迁移与恢复

- **迁移顺序：** T-01/T-03 先提供配置与可服务快照，再 additive 发布新方法，随后迁移内部消费者并保留旧方法。
- **兼容窗口：** 旧批量/签名方法保留；仓库外调用方需读取发布说明，不能持久化不同生命周期 URL。
- **监控信号：** 按失败类别统计对象、配置、readiness、策略与 Provider 错误；日志不含完整 URL 查询。
- **回滚或前向恢复：** 公共流量启用前可关闭公共策略；启用后优先将配置标记 NOT_SERVING/修正域名并前向恢复。
- **不可逆操作与批准点：** 无数据迁移；生产公共域名和流量启用未授权。
- **收缩条件：** 所有 URL 生成调用点委托统一 resolver，旧独立分类/固定签名分支为零且合同扫描通过。

## 10. 验收标准

- [x] frontmatter 所列 11 个 AC 的正常、失败和兼容场景通过。
- [x] 公共结果无签名参数且 expiresAt=null；私有结果包含真实 expiresAt。
- [x] 管理列表 URL=null，不存在匿名按 ossId 查询面。
- [x] 验证矩阵记录到 `<Path>{roots.state}/specdev/changes/{change}/evidence/T-04.md</Path>`。
- [x] 实际修改未超出 writable_paths，共享路径只由 T-04 修改。
- [x] 形成非空 implementation/source commit，经 candidate 验证并记录 result SHA。
- [x] 已批准路径修正记录在 change status，Ticket、Map 与 Evidence 一致。
