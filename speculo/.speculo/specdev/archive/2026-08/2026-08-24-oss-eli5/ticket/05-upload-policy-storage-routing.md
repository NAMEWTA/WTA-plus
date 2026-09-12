---
schema_version: 3
artifact: ticket
change: 2026-08-24-oss-eli5
id: T-05
title: 按 uploadPolicy 路由公共与私有存储
status: done
planning_depth: deep
planning_depth_reason: 上传初始化决定对象进入公共或私有 Bucket，属于安全路由和在途状态兼容边界。
ready: true
risk: high
blocked_by: [T-01, T-03]
contract_ids: [AC-001, AC-002, AC-003, AC-022, AC-023]
owner: codex:/root
expected_changes:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/upload/OssUploadObjectStore.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/upload/DefaultOssUploadObjectStore.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/upload/OssUploadService.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/upload/OssUploadError.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/upload/OssUploadServiceUnitTest.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/upload/OssUploadConfigurationUnitTest.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/upload/OssUploadStorageRoutingMinioIntegrationTest.java</Path>"
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/upload/OssUploadObjectStore.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/upload/DefaultOssUploadObjectStore.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/upload/OssUploadService.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/upload/OssUploadError.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/upload/OssUploadServiceUnitTest.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/upload/OssUploadConfigurationUnitTest.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/upload/OssUploadStorageRoutingMinioIntegrationTest.java</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/upload/OssUploadProperties.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/upload/OssUploadTicket.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/readiness/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-oss/src/main/java/org/dromara/common/oss/factory/OssFactory.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysOssUploadController.java</Path>"
shared_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/upload/OssUploadObjectStore.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/upload/DefaultOssUploadObjectStore.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/upload/OssUploadService.java</Path>"
shared_path_owners:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/upload/OssUploadObjectStore.java</Path> => T-05"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/upload/DefaultOssUploadObjectStore.java</Path> => T-05"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/upload/OssUploadService.java</Path> => T-05"
---

# Ticket T-05: 按 uploadPolicy 路由公共与私有存储

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/05-upload-policy-storage-routing.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-05.md</Path>`

## 1. 战略与来源

- **目标：** 复用现有直传协议，让命名 uploadPolicy 在可信服务端选择目标 storage config，并保持在途 Ticket 路由冻结。
- **可观察产出：** 公共和私有策略分别把 SINGLE/MULTIPART 对象写入对应 Bucket，UploadTicket 与 `sys_oss.service` 记录实际 configKey；后续动作不受默认/策略变化影响。
- **来源：** `US-001`、`US-007`、`AC-001`、`AC-002`、`AC-003`、`AC-022`、`AC-023`、`ADR-002`、`ADR-011`。
- **当前事实：** 初始化固定 `OssFactory.instance()`；prepare 已返回并冻结 service/Bucket，后续动作已按 Ticket.service 选择 client。
- **Planning Depth 原因：** 只需改变初始化接缝，但错误目标会让私有附件进入公共 Bucket，且必须保持状态机兼容。

## 2. 决策状态

### 已锁定决策

- 初始化使用 T-01 policy 的 storageConfigKey 和预期访问类型，不读取客户端字段。
- 目标配置必须存在、SERVING 且访问类型匹配；失败不得回退默认配置、创建 Ticket 或启动 Provider 上传。
- `prepare` 接收可信 configKey 并通过 `OssFactory.instance(configKey)` 创建 key/上传；返回实际 clientId/Bucket。
- Ticket 创建后 resume/sign/complete/abort/cleanup 继续只读冻结的 service/Bucket/objectKey。
- HTTP 路径与 InitRequest/响应不增加 configKey、Bucket、访问类型或 TTL。
- SINGLE/MULTIPART、HEAD/magic、幂等 Complete、TEMP、清理和字节直传行为保持。

### 已采用的低影响假设

- policy 配置的预期访问类型用于启动和运行双重防御；configKey 实际类型始终以服务端配置为准。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| 初始化按 configKey 选 client、readiness/type 检查、Ticket 冻结回归、双 Bucket 上传集成 | 现有 UploadTicket 字段、Redis store、完成校验、OssFactory 多配置能力 | 请求新增存储字段、重写上传协议、对象级 ACL、后续重新路由 |

## 4. 要构建什么

用户仍只提交策略名和文件元数据。服务端授权该策略后取得固定 storageConfigKey，要求其 readiness/type 合法，再生成目标 Bucket 的预签名上传并创建 Ticket。Ticket 一旦创建，所有恢复、签名、完成和清理动作只使用冻结路由，即使管理员随后改变默认配置或策略映射。

## 5. 实现契约

- **入口或接缝：** `OssUploadService.init`、`OssUploadObjectStore.prepare`、T-03 readiness registry。
- **输入与输出：** 服务端 Policy + 文件元数据 -> 目标 client PreparedUpload + 冻结 Ticket；非法目标 -> typed upload failure。
- **公共接口变化：** 无浏览器 wire format 变化；内部 prepare 增加可信 configKey/期望类型。
- **不变量：** 客户端不能选择存储；Ticket 路由不可变；metadata 的 service 等于实际 clientId。
- **状态或数据流：** require policy/identity -> require config SERVING/type -> prepare target -> create Ticket/cleanup -> complete register service。
- **错误与失败行为：** 未知/NOT_SERVING/类型不匹配在 Provider/Ticket 副作用前失败；Ticket 保存失败按目标 service 清理。
- **兼容要求：** 既有 policy 绑定 PRIVATE 默认配置；状态、错误协议和上传请求字段保持。
- **安全与隐私要求：** 响应不回显 configKey/Bucket；错误不泄露 Provider 凭据或内部 Policy。

## 6. 执行路线

1. 扩展 upload unit fake，先证明公共/私有策略、类型不匹配和默认变化场景失败。
2. 扩展 prepare 接缝按 storageConfigKey 获取 client，并在任何 Provider 副作用前查询 readiness/type。
3. 让 init 传入服务器 policy 路由，保持 Ticket/cleanup 使用实际返回值。
4. 覆盖在途策略/default/cache 改变后 resume/complete/abort/cleanup 仍使用冻结 service。
5. 运行上传单元/HTTP 合同、双 Bucket MinIO 和现有 OSS 回归。

## 7. 路径访问契约

- **预计修改点/可写范围：** upload object-store/service/error 与专用测试。
- **只读上下文：** T-01 properties、Ticket record、T-03 readiness、OssFactory 和 Controller 请求合同。
- **共享路径：** upload store/service 由 T-05 唯一修改；T-08 只运行其回归，不修改。
- **保留或不动：** `OssUploadTicket` 序列化、Controller 路径、Redis store、metadata store 和前端请求类型。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 公共/私有策略路由 | upload unit | 运行 `OssUploadServiceUnitTest` | Ticket.service/Bucket 和完成 metadata 指向目标配置 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-05.md</Path>` |
| 注入/NOT_SERVING/类型不匹配 | negative unit/HTTP | 运行配置/HTTP 合同测试 | 无 Ticket/Provider 请求且不回退默认 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-05.md</Path>` |
| 在途配置变化 | state-machine unit | 创建 Ticket 后切换 policy/default/cache | 所有动作仍调用冻结 service | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-05.md</Path>` |
| 真实双 Bucket | MinIO integration | Lead 运行 `OssUploadStorageRoutingMinioIntegrationTest` | SINGLE/MULTIPART 字节进入正确 Bucket | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-05.md</Path>` |
| 既有状态机回归 | OSS suite | 运行 `OssUpload*Test`、lifecycle/owner tests | Complete/TEMP/引用/删除保持 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-05.md</Path>` |

- **Workspace checks：** current workspace 或 source worktree 运行上传 unit/HTTP/静态扫描及 reactor package。
- **E2E disposition：** required：必须用真实双 Bucket 验证签名上传和最终对象位置。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate；隔离 MinIO，源 worktree 不声明 E2E。
- **Integration evidence：** implementation/source commit、candidate/result SHA、Bucket object listing 和父分支包含关系。

## 9. 发布、迁移与恢复

- **迁移顺序：** 先配置 PRIVATE/公共策略和 readiness，再发布路由代码；既有策略全部明确绑定 PRIVATE。
- **兼容窗口：** 旧客户端无感；没有完成 storageConfigKey 配置的环境启动失败，不允许隐式兼容。
- **监控信号：** policy、配置状态、类型不匹配、Provider 初始化失败和冻结 service；不记录签名 URL。
- **回滚或前向恢复：** 未创建 Ticket 时修正配置重试；已创建 Ticket 始终按冻结目标完成或取消，不动态搬迁。
- **不可逆操作与批准点：** 无生产上传或 Bucket 变更授权；实际公共策略启用需部署批准。
- **收缩条件：** 初始化路径不存在无参数 `OssFactory.instance()`，所有 enabled policy 均有匹配且 SERVING 的 configKey。

## 10. 验收标准

- [x] `AC-001`、`AC-002`、`AC-003`、`AC-022`、`AC-023` 全部通过。
- [x] 客户端 transport 未增加存储路由字段。
- [x] 目标失败时没有 Ticket、Multipart 或对象副作用。
- [x] 验证矩阵记录到 `<Path>{roots.state}/specdev/changes/{change}/evidence/T-05.md</Path>`。
- [x] 实际修改未超出 writable_paths，共享路径只由 T-05 修改。
- [x] 形成非空 implementation/source commit，经 candidate 验证并记录 result SHA。
- [x] 未发生未批准偏差，Ticket、Map 与 Evidence 一致。
