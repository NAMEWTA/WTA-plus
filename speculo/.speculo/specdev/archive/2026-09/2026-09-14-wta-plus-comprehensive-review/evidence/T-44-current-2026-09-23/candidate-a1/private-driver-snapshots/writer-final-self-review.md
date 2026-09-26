# T-44 原实现者最终只读自查

身份与范围：我是本票产品代码的原 writer；这份是自查，**不是独立审查或正式验收**。只读比较 `16c32a4c..e11c1b6f3a4a4a8bdc1746044d65fe8445c0865c` 的 33 条路径；未修改仓库、运行测试、构建或服务。检查时 HEAD `e11c1b6f3a4a4a8bdc1746044d65fe8445c0865c` 且工作树干净。

## 合同逐项核对

| 边界 | 当前实现证据 | 自查结论与限制 |
|---|---|---|
| 当前对象与权限 | `SysOssController` 下载端点保留 `system:oss:download`；`OssLifecycleManager.requireDownloadable` 在调用 Provider 前拒绝不存在、`DELETE_PENDING`、非 `ACTIVE` 或空 service；`accessPolicy` 取对象原 service 的实际配置，PUBLIC 走公开 URL，PRIVATE 走签名且拒绝对 PUBLIC 私签。 | 缺/过期诊断不再拒绝合法对象；对象状态、配置及访问类型仍失败关闭。业务调用路径的 owner/Client 授权由既有上层保持，本票没有放宽控制器。实际最小权限 MinIO 整体矩阵仍待 Lead 完成。 |
| 直传 | `OssUploadService.init` 先校验请求、策略、身份和 Client，再要求非空默认 key；`DefaultOssUploadObjectStore.prepare` 从当前 `OssFactory.instance(key)` 取配置，比较实际 access policy 与服务端期望，随后签名/发起 Multipart；已有 ticket 固定 service/owner/client。 | 移除的仅是 registry 快照判断；默认缺失、配置缺失、policy 不匹配、Provider 出错仍拒绝，不能把诊断通过误当可上传证明。 |
| 迁移 | `OssStorageMigrationService.dryRun` 拒绝无效/非 ACTIVE 对象、空 source、同源同目标；`DefaultOssMigrationObjectStore.inspect` 和 `transferAndVerify` 均用当前 `OssFactory` 客户端校验 source `PRIVATE`、target `PUBLIC_READ`，并检查真实对象/冲突/内容；后续有 CAS 与审核窗口。 | 旧快照不再阻断，但预期访问类型和真实对象失败仍关闭；真实供应商副作用由 Lead 的 MinIO 证据决定。 |
| 启动与缓存 | `SystemApplicationRunner` 仅调用 `SysOssConfigServiceImpl.init`；后者先成功读 DB，再清 `SYS_OSS_CONFIG` 专用缓存与默认 Redis 指针，只加载本地结构合法行，仅唯一合法 PRIVATE 默认写指针。`OssUploadDiagnostics` 的 `ApplicationReadyEvent` 远端检查与 readiness 的 `@Scheduled` 探测已移除。 | 可选坏配置、空/重复/坏默认不会因 OSS 诊断阻断核心；DB/Redis 真故障没有被吞。源码所见启动链不创建 OSS 客户端、不做远端 I/O；Lead 已报告核心启动场景的 `startup_minio_calls=0`，但不能由此宣称所有真实 MinIO 业务场景通过。 |
| 全局调度与健康组 | `@EnableScheduling` 移至 `NamewtaApplication`，避免关闭 Notify 等非 OSS 计划任务；`application.yml` 的 `liveness`、`readiness`、`core` 排除 OSS 诊断，`ossdiagnostics` 单独包含观察快照。Docker 的 8080 TCP 检查已标明只是进程探针；README 明确 Actuator Basic Auth。 | 核心健康不被 OSS 快照拖低；根 `/actuator/health` 不等同核心组，部署方应使用明确路径。 |
| 显式诊断与并发 | `POST /resource/oss/config/diagnose/{ossConfigId}` 要求 `system:ossConfig:list`、正 ID；`@Log` 不存请求/响应；VO 只含状态、固定原因、检查时间。配置提交后的 listener 只失效旧/新 key；`configRevision` 拒绝诊断期间配置变更后的迟到结果；诊断不参与业务门禁。 | 可选 Duration 以原始字符串绑定，坏值变固定诊断原因而不使 Spring Binder 中断核心；每个远端步骤上限 3 秒、最多五个顺序步骤，**不是整个请求 3 秒上限**。全局 revision 会使两个不同 key 的并发诊断之一保守返回 STALE，属于安全降级。 |

## 已见验证与剩余风险

- Lead 保存的同源码 `green02-source-and-counts.json` 显示 OSS 选集 148/148、0 failure/error/skip，前后均 clean；Lead 另报告默认后端 899 pass/216 环境 skip、full 打包成功、六个核心真实启动场景通过。这些是 Lead 执行结果，非我本轮重跑。
- 真实 MinIO v4 及本票 required 的完整最小权限上传、签名、下载、失败分类尚未形成全部通过记录；不能勾选整票验收。默认测试中的环境 skip 也不能代替真实服务验收。
- 静态残留：`OssLifecycleManager`、`OssUploadService`、`OssStorageMigrationService` 保留兼容构造器/注入的 registry 引用但业务不读取；`OssStorageReadinessService.refresh()` 和 registry 的 `replace/requireServing` 仍是公开内部方法，但当前生产调用检索只有显式单配置诊断，既无启动调用也无周期调用。未来若重新接入全量 `refresh`，需重新评估它与配置版本的并发边界。
- 既有 `OssStorageReadinessHealthIndicator` 详细信息仍包含 config key 和访问类型枚举；它不含 secret、endpoint 或 Provider 原文，且健康端点受现有 Basic Auth。它与新诊断 VO 的“三字段”承诺是不同公开面，不能把 VO 的去敏断言套用到 Actuator 详情。
- 历史重复 `configKey` 的 DB 行可在启动缓存重建时互相覆盖；这是邻票 T49 的去重责任。当前访问仍通过 `OssFactory` 的现存缓存配置及实际 policy 校验，可能使配置不可用，但不应把它描述为已解决的重复配置迁移。

本次自查未发现应立即阻断固定候选的新增代码缺陷；最终裁决须以 Lead 的真实 MinIO v4、权限/失败负例、源与 JAR 一致性及清理证据为准。
