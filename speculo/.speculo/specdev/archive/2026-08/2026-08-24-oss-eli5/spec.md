---
schema_version: 3
artifact: spec
change: 2026-08-24-oss-eli5
status: ready
ready_for_tickets: true
sources:
  - "USER-DECISION:2026-08-31-public-and-controlled-oss-consensus"
  - "CHANGE-ADR:ADR-001-to-ADR-013"
  - "PERMANENT-ADR:ADR-0002-ADR-0003-ADR-0004-ADR-0005-ADR-0010"
  - "CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/system/api/OssService.java</Path>"
  - "CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/upload/OssUploadProperties.java</Path>"
  - "CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/upload/DefaultOssUploadObjectStore.java</Path>"
  - "CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/service/OssLifecycleManager.java</Path>"
  - "CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysOssConfigServiceImpl.java</Path>"
  - "CODE:<Path>plus-ui-namewta/packages/web-domains/system/src/oss-config/OssConfigPage.vue</Path>"
---

# Spec: OSS 公共只读与受控短时访问增强

- **Spec：** `<Path>{roots.state}/specdev/changes/2026-08-24-oss-eli5/spec.md</Path>`
- **当前 ADR：** `<Path>{roots.state}/specdev/changes/2026-08-24-oss-eli5/ADR.md</Path>`
- **当前领域上下文：** `<Path>{roots.state}/specdev/changes/2026-08-24-oss-eli5/CONTEXT.md</Path>`

## 1. 问题与目标

### 问题陈述

当前 OSS 已具备浏览器直传、Redis UploadTicket、SINGLE/MULTIPART、TEMP 生命周期、业务引用和短时下载签名，但还没有形成公共资源与受控资源的完整访问合同：

- `sys_oss_config.access_policy`、Java `AccessPolicy` 和管理界面对同一值存在 `public/custom/PUBLIC_READ_WRITE/PUBLIC_READ` 等不同解释，不能作为可靠的安全边界。
- 直传初始化固定使用默认 `OssFactory.instance()`，命名 `uploadPolicy` 不能决定公共或受控存储配置；虽然 UploadTicket 已冻结 `service` 和 Bucket，初始化阶段仍缺少选择能力。
- `OssLifecycleManager.presignDownload` 对所有对象统一生成默认 2 分钟签名，门户等公开内容无法获得稳定、可缓存的公共 URL。
- `OssService` 没有按对象存储类型统一解析 URL 的结构化方法。业务模块若自行读取 OSS 配置或拼接 URL，会复制安全判断并越过 system 模块边界。
- 配置保存和删除没有保护已有对象的 Bucket/访问类型边界；旧 `access_policy` 又存在歧义，直接升级可能批量误公开历史业务附件。
- 现有诊断只检查默认 Bucket 的直传 CORS/Lifecycle 并记录告警，不能证明 public-read/private 声明与 Provider 实际匿名访问能力一致，也不能阻止错误配置承载流量。

因此，当前私有短时下载能力基本可用，但“公共资源稳定直链”和“按策略选择公共/受控存储”尚未端到端成立。

### 目标用户与场景

- 公共内容的 Business OSS Owner：上传门户图片、公开文档等资源，并随已发布业务数据返回稳定公共 URL。
- 匿名访问者：无需登录即可使用已发布的稳定 URL 预览或下载公共资源。
- 受控业务用户：在业务权限和数据权限通过后获得短时下载 URL，并在过期后失去该 URL 的访问能力。
- 跨模块业务开发者：只依赖 `ruoyi-api` 的 `OssService`，不读取 system Mapper、OSS 配置或 Provider SDK。
- OSS 配置管理员：以一致的 PRIVATE/PUBLIC_READ 语义维护配置，不能用普通编辑意外改变历史对象访问边界。
- 部署与存储运维人员：准备独立公共/受控 Bucket、公共域名和 Provider Policy，并从 readiness/诊断中获得可操作结果。
- 存量迁移操作人员：按业务审核清单把特定对象迁入公共存储，保持 ossId 和业务引用稳定，并能够审计、重试或回滚。

### 成功标准

- 至少一组 PUBLIC_READ configKey/Bucket 和一组 PRIVATE configKey/Bucket 可同时工作；单个配置不混放两种访问语义。
- 公共对象返回无签名参数、无过期时间的稳定 URL；匿名 GET/HEAD 成功，匿名 PUT、覆盖和删除失败。
- 受控对象原始 Provider 地址不可匿名读取；业务授权后返回短时签名 URL，默认有效期 2 分钟，命名访问策略可由服务端受限覆盖，过期后 Provider 拒绝访问。
- 命名 uploadPolicy 固定选择服务端 storageConfigKey；客户端不能选择 configKey、Bucket、访问类型或 TTL。
- `OssService` 集中解析访问类型和 URL；公共业务只发布自身已获准公开的对象，不存在按任意 ossId 匿名查询的通用平台接口。
- Provider Policy 或生产公共域名与声明不一致时，对应存储配置不能承载上传或读取，readiness 明确失败；应用不修改外部 Policy。
- 所有升级前对象默认保持受控；没有对象因旧 `access_policy` 值被自动公开。
- 有对象的配置不能通过普通编辑改变 Bucket 或访问类型；显式迁移保持 ossId、业务引用和失败恢复能力。
- 现有直传、TEMP、引用、删除、管理权限和文件字节不经过后端的行为保持成立。

### 非目标

- 不新增按任意 ossId 匿名查询 URL 或元数据的通用 HTTP 接口。
- 不由应用创建、修改或放宽 Bucket Policy，不自动开通 CDN、DNS、证书或云资源。
- 不在同一 Bucket 中使用对象级 ACL 混放公共和受控对象，不支持 PUBLIC_READ_WRITE 或匿名上传。
- 不让客户端提交 configKey、Bucket、对象访问类型、签名 TTL 或 Provider Policy。
- 不重写现有 SINGLE/MULTIPART、UploadTicket、TEMP、业务引用和两阶段删除子系统。
- 不把 `sys_oss_ref` 变成 ACL、动态外键或通用业务授权引擎。
- 不为当前仓库虚构门户 App 或匿名门户业务端点；具体门户/官网业务 Owner 在自身 change 中复用本 Spec 的平台 Service 合同。
- 不建设病毒扫描、内容审核、数字版权或公共内容撤回工作流。
- 不自动把全部历史图片或附件迁入公共存储。

## 2. 解决方案与外部行为

### 解决方案摘要

在现有 OSS 控制面/数据面分离基础上增加“存储配置决定读取语义”的双类型模型。PUBLIC_READ 与 PRIVATE 使用独立 configKey 和独立 Bucket；`sys_oss.service` 继续记录对象实际归属，并作为访问类型推导入口，不增加对象级访问类型副本。

命名 uploadPolicy 增加服务端 storageConfigKey 绑定。上传初始化从策略选择目标 OssClient，并把实际 service/Bucket 冻结在现有 UploadTicket 中；后续签名、完成、取消、恢复和清理继续按 Ticket 执行。

`ruoyi-api` 的 `OssService` 增加 `resolveAccessUrl(ossId)` 结构化合同：公共对象使用受管 domainUrl/CDN 生成稳定 URL，受控对象使用短时预签名 GET。旧 URL 查询方法委托统一解析，管理列表继续隐藏 URL。私有强制签名保留显式方法，并支持服务端命名访问策略。

配置治理把应用语义收敛到 PRIVATE 与 PUBLIC_READ，并增加启动校验、Provider Policy/readiness 诊断、生产公共域名门禁和有对象配置的边界变更保护。存量对象默认私有，只有审核清单内对象通过显式迁移进入公共存储。

### 主要流程

#### 新对象上传与存储路由

1. 浏览器继续提交命名 uploadPolicy 和现有文件元数据，不提交 configKey、Bucket 或访问类型。
2. 服务端读取类型化策略，完成现有权限、Client 准入、大小、Content-Type 和模式校验，再取得策略绑定的 storageConfigKey。
3. 目标配置必须存在、处于可服务状态、诊断通过且与策略声明的 PUBLIC/PRIVATE 用途一致；不满足时 init 失败，不创建 Ticket 或 Provider 上传。
4. 上传适配器使用目标 configKey 创建 SINGLE 或 MULTIPART 上传，并把实际 service、Bucket、objectKey 和 uploadId 冻结到现有 UploadTicket。
5. resume/sign/complete/abort/cleanup 继续以 Ticket 内冻结的 service 为权威，不重新选择默认配置，也不受后续策略编辑影响。
6. Complete 继续执行现有 HEAD、大小、Content-Type、magic bytes 和幂等登记；`sys_oss.service` 保存实际 configKey，业务只保存 ossId。

#### 公共对象发布与匿名读取

1. Business OSS Owner 完成自身业务发布判断后，以 ossId 调用 `OssService.resolveAccessUrl`。
2. Service 读取 `sys_oss.service` 对应配置。PUBLIC_READ 对象返回 `accessType=PUBLIC`、稳定 URL、`expiresAt=null` 和文件名。
3. 生产稳定 URL 必须由配置的 domainUrl/CDN 与正确编码的 objectKey 组成；开发环境只有显式允许时才能回退 Provider Bucket URL。
4. Business OSS Owner 将稳定 URL 随公开业务数据返回。平台不提供匿名 ossId 查询，公共对象发现仍由业务发布状态决定。
5. 匿名访问者直接向公共域名/Provider 执行 GET 或 HEAD；任何匿名写、覆盖或删除均由 Provider 拒绝。

#### 受控对象授权读取

1. Business OSS Owner 先完成自己的登录、权限、角色和数据权限校验；`OssService` 不替业务判断 ACL。
2. `resolveAccessUrl` 对 PRIVATE 对象返回 `accessType=PRIVATE`、预签名 URL、实际 `expiresAt` 和文件名；默认 TTL 为 2 分钟。
3. 需要不同窗口的服务端调用方只能选择已配置的命名访问策略；策略 TTL 必须位于服务端安全上下限内。外部请求不能直接提交 TTL。
4. 未授权请求不得调用签名边界或返回 URL。即使知道 ossId，PRIVATE Bucket 的原始地址仍拒绝匿名访问。
5. 到达 expiresAt 后，同一签名 URL 由 Provider 拒绝；重新访问必须重新完成业务授权并签发新 URL。

#### 配置启动、诊断与运行门禁

1. 每个配置只声明 PRIVATE 或 PUBLIC_READ；现有 `status=Y/N` 继续只表示“是否默认”，不作为可用/禁用开关，且唯一默认配置必须是 PRIVATE。
2. 应用启动时校验 configKey 唯一、Bucket/类型不混用、生产 PUBLIC_READ 具有 domainUrl、uploadPolicy 引用存在且类型匹配、命名访问策略合法。
3. readiness 必检集合包括：默认配置、被启用 uploadPolicy 引用的配置、被现有 `sys_oss.service` 引用的配置，以及进行中迁移的源/目标配置。未被引用的占位配置不承载流量，也不因远程探测失败单独阻断整体 readiness。
4. 应用对必检集合执行只读 Provider 诊断。PUBLIC_READ 必须允许匿名 GET/HEAD 且拒绝匿名写；PRIVATE 必须拒绝匿名读取。
5. 诊断不能修改 Policy、创建公开业务对象或输出 Secret。无法确认、超时、权限不足或结果不一致均按 fail-closed 处理。
6. 失败配置不进入可服务集合，不能被上传策略或 URL 解析选中；整体 readiness 呈现失败原因，供部署系统修正外部配置。

#### 存量保护与显式迁移

1. 升级时所有已有 configKey/对象按 PRIVATE 基线处理；旧 `access_policy` 的 public/custom 数值不能触发自动公开。
2. 如果旧 Bucket 实际允许匿名读取，部署必须先收紧 Policy 或把对象迁入已验证的 PRIVATE Bucket；在此之前 readiness 失败。
3. 公共迁移只接受 Business OSS Owner 审核后的 ossId 清单和目标 PUBLIC_READ configKey。
4. 迁移先校验源/目标配置与目标 readiness，再复制对象并验证内容；验证成功后原子切换 `sys_oss.service`，业务 ossId 和 `sys_oss_ref` 不变。
5. 切换后验证业务访问结果和目标匿名只读行为。源对象只在成功验证和安全窗口后清理，不先删源对象。
6. 任一步失败都保留或恢复旧 service 归属和源对象；迁移记录可定位批次、对象、阶段、错误和重试结果。

### 边界、失败与稳定错误行为

| 边界或失败 | 稳定行为 |
|---|---|
| uploadPolicy 未绑定 storageConfigKey，目标配置不存在、不可服务或类型不匹配 | 启动校验失败；运行时不得回退默认配置。 |
| 客户端提交或伪造 configKey、Bucket、访问类型或 TTL | 请求字段不属于公共合同；若出现则拒绝或忽略，不得影响服务端决策。 |
| 对象不存在、配置不存在/不可服务或对象处于 PENDING 删除 | 不返回公共或签名 URL，使用现有统一响应和可区分 OSS 错误类别。 |
| PUBLIC_READ 在生产缺少 domainUrl | 配置不能进入可服务集合，readiness 失败；不得隐式返回 Provider URL。 |
| Provider Policy 与 PRIVATE/PUBLIC_READ 声明不一致或无法确认 | 对应配置不可服务，readiness 失败；应用不尝试修改 Policy。 |
| 匿名访问 PRIVATE 原始 URL | Provider 拒绝；应用不提供匿名签名兜底。 |
| 业务授权失败 | 业务接口拒绝且不调用签名 Service，不泄露对象存在性、URL 或配置元数据。 |
| 私有签名过期 | Provider 拒绝原 URL；必须重新授权和签发。 |
| 命名访问策略未知、禁用、TTL 非法或越过安全上下限 | 签名前失败，不回退全局默认，也不接受客户端 TTL。 |
| 对 PUBLIC 对象调用“强制受控签名”方法 | 明确拒绝，避免调用方把公开对象误认为受签名保护。 |
| 有存量对象的配置普通编辑 Bucket 或访问类型 | 保存失败并提示必须使用迁移流程；配置和缓存保持旧值。 |
| 旧 accessPolicy 含义不明 | 保守归类 PRIVATE 或阻止该配置进入可服务集合，绝不自动归类 PUBLIC_READ。 |
| 对象迁移复制、验证、切换或读取验收失败 | 迁移失败可观察；旧 ossId、业务引用、旧 service 和源对象保持或恢复可用。 |
| 公共对象撤下业务发布 | 业务接口不再返回 URL；已知稳定 URL 仍可能可读，直至对象迁回 PRIVATE、删除或外部 Policy 收紧。 |

错误继续使用项目统一 `R<T>`、异常映射和现有 OSS typed error 模式。本 Spec 规定可区分的失败条件，不虚构数值错误码、HTTP 状态或 Provider 专属错误文本。

### 状态转换与不变量

```text
UploadPolicy --server storageConfigKey--> UploadTicket(service + bucket frozen)
                                                |
                                                v
                                  Complete -> sys_oss.service
                                                |
                      +-------------------------+-------------------------+
                      |                                                   |
                 PUBLIC_READ                                          PRIVATE
                      |                                                   |
              stable public URL                                  short-lived URL
              expiresAt = null                                   expiresAt != null

Migration: source verified -> target copied -> content verified
           -> sys_oss.service switched -> business access verified
           -> source cleanup eligible
```

- `sys_oss.service` 与其存储配置是对象访问类型的唯一权威；不根据 `sys_oss.url`、URL 查询参数、文件前缀或调用方输入猜测类型。
- 单个 configKey/Bucket 只承载一种访问类型；PUBLIC_READ 永不授予匿名写。
- 全局默认 OSS 配置必须是 PRIVATE；需要公开的上传只能通过明确的公共 uploadPolicy 路由。
- UploadTicket 一旦创建，service/Bucket/objectKey 不因默认配置、命名策略或配置缓存变化而改变。
- 公共结果 `expiresAt` 必须为 null；受控结果 `expiresAt` 必须等于实际签名过期时间。
- 签名 URL 不写入业务数据、`sys_oss.url` 或长期缓存；业务继续只持久化 ossId。
- `sys_oss_ref` 只管理生命周期和反向定位，不参与访问类型或下载授权。
- 管理列表和对象元数据查询不自动生成 URL；知道 ossId 不等于能够匿名发现或读取对象。
- 配置边界迁移保持 ossId 和业务引用稳定；在目标验证完成前不得删除源对象。

## 3. 用户故事

- **US-001**：作为公共内容的 Business OSS Owner，我希望用命名策略把资源上传到公共存储并获得稳定 URL，以便门户或公开页面长期展示资源。
- **US-002**：作为匿名访问者，我希望直接打开已发布的公共 URL，以便无需登录即可预览或下载公开内容。
- **US-003**：作为受控业务用户，我希望在业务授权后获得有限时长的下载 URL，以便访问附件且链接过期后不能继续使用。
- **US-004**：作为跨模块业务开发者，我希望通过统一 `OssService` 获取正确类型的访问结果，以便不复制配置查询、URL 拼接和签名逻辑。
- **US-005**：作为 OSS/部署管理员，我希望配置语义一致并在 Provider Policy 或域名错误时 fail-closed，以便错误配置不会误公开数据或承载流量。
- **US-006**：作为存量迁移操作人员，我希望按审核清单安全迁移公共资源并保留 ossId，以便业务引用稳定且失败可恢复。
- **US-007**：作为现有 OSS 调用方，我希望旧 Service、直传、TEMP 和管理功能保持兼容，以便本次增强不会迫使无关业务重写。

## 4. 验收合同

用户故事覆盖：US-001 -> AC-001/AC-004/AC-005；US-002 -> AC-005/AC-010；US-003 -> AC-006/AC-007/AC-008/AC-009；US-004 -> AC-004/AC-006/AC-011/AC-021；US-005 -> AC-013 至 AC-018；US-006 -> AC-017/AC-019/AC-020；US-007 -> AC-011/AC-012/AC-022/AC-023。

| ID | 前置条件 | 动作或事件 | 可观察结果 | 验证接缝 |
|---|---|---|---|---|
| AC-001 | 公共 uploadPolicy 绑定已就绪 PUBLIC_READ configKey | 初始化并完成 SINGLE 或 MULTIPART 上传 | Ticket 与 `sys_oss.service` 均记录目标公共 configKey；文件字节仍直接进入目标 Bucket | UploadPolicy/UploadService 单元与 S3 集成 |
| AC-002 | 受控 uploadPolicy 绑定已就绪 PRIVATE configKey | 初始化并完成上传 | Ticket 与 `sys_oss.service` 记录目标私有 configKey；不使用当前默认公共配置 | UploadPolicy/UploadService 单元与 DB 集成 |
| AC-003 | 客户端尝试附带 configKey、Bucket、访问类型或 TTL | 调用上传初始化或下载入口 | 不可信值不能改变路由或签名窗口；无额外 Ticket、对象或 URL 泄露 | Controller 合同与请求模型测试 |
| AC-004 | `sys_oss.service` 指向 PUBLIC_READ 配置 | 调用 `resolveAccessUrl(ossId)` | 返回 PUBLIC、稳定 URL、原文件名且 `expiresAt=null`；重复调用 URL 不因签名时间变化 | OssService/URL resolver 单元测试 |
| AC-005 | 公共对象已发布且 Provider Policy 正确 | 匿名执行 GET/HEAD，再尝试匿名 PUT/DELETE | GET/HEAD 成功且内容正确；写、覆盖、删除失败 | 双 Bucket S3-compatible 集成与真实 URL 检查 |
| AC-006 | `sys_oss.service` 指向 PRIVATE 配置且调用方已授权 | 调用默认 `resolveAccessUrl` | 返回 PRIVATE、文件名、签名 URL 和约 2 分钟实际 expiresAt；原始地址仍不可匿名读取 | OssService 单元与 Provider 集成 |
| AC-007 | 服务端配置合法命名访问策略 | 服务端调用显式私有签名方法并选择策略名 | 使用该策略 TTL；未知/禁用/越界策略失败；外部客户端不能直接指定 TTL | 访问策略配置与签名单元测试 |
| AC-008 | 已签发 PRIVATE URL | 在有效期内和 expiresAt 后请求同一 URL | 有效期内成功，过期后 Provider 拒绝；重新签发得到新的 expiresAt | 可控时钟单元与 Provider 集成 |
| AC-009 | 用户没有业务对象访问权或只知道 ossId | 请求业务附件或直接访问 PRIVATE 原始 URL | 业务接口不签发 URL，Provider 原始读取失败，不泄露对象或配置详情 | Business Owner 权限合同与 Provider 集成 |
| AC-010 | 匿名调用者知道公共或私有 ossId | 尝试通过平台通用接口发现 URL/元数据 | 不存在匿名 ossId URL 查询面；只有业务发布响应可发现公共 URL | Controller 路由/权限扫描 |
| AC-011 | 现有调用方使用 `selectUrlByIds` 或 `selectByIds` | 查询混合 PUBLIC/PRIVATE ids | 方法签名保持；公共项为稳定 URL，私有项为短时 URL；`selectUrlByIds` 遇到不存在对象保持失败，`selectByIds` 保持过滤不存在项 | SysOssService 合同测试与调用点扫描 |
| AC-012 | 管理员查询 OSS 列表或 listByIds | 获取管理数据 | URL 仍为 null；只有专用、已授权下载入口生成类型化访问结果 | Controller/Service 单元与前端 transport 测试 |
| AC-013 | 新增、编辑或展示 OSS 配置 | 使用管理 API/UI 和数据库增量 | 只出现 PRIVATE 与 PUBLIC_READ；无 public-write/custom 歧义，默认配置只能为 PRIVATE | 配置 Service、SQL 与 Vue 组件测试 |
| AC-014 | 生产 profile 的必检 PUBLIC_READ 配置缺少有效 domainUrl | 启动或由策略/对象引用该配置 | 配置不可服务且 readiness 明确失败；不回退 Provider URL | ApplicationContext 配置门禁测试 |
| AC-015 | 开发 profile 显式允许 Provider URL 回退 | 解析公共对象 URL | 可生成正确编码的 Provider URL；未显式允许时与生产一样失败 | URL resolver 参数化单元测试 |
| AC-016 | Provider Policy 正确、错误、超时或无法确认 | 启动/健康诊断 | 正确配置就绪；其余配置 fail-closed、readiness 给出非敏感原因，且应用不产生 Policy 修改调用 | Health/diagnostic 单元与 Provider 集成 |
| AC-017 | 数据库含升级前配置和对象 | 执行增量迁移并启动 | 历史对象全部按 PRIVATE 访问；歧义旧值不会自动公开，实际公开的旧 Bucket 会阻止 readiness 直至修正 | MySQL upgrade fixture 与 Provider 验收 |
| AC-018 | configKey 已有对象 | 普通编辑 Bucket 或 PRIVATE/PUBLIC_READ 类型；再轮换凭据/domainUrl | 边界字段更新失败且缓存不变；不改变归属的轮换可成功 | 配置 Service/Mapper 集成测试 |
| AC-019 | 审核清单、源 PRIVATE 和目标 PUBLIC_READ 均有效 | 执行对象迁移 | 复制与内容验证后切换 `sys_oss.service`；ossId/业务引用不变，公共解析和匿名只读成立 | 迁移状态机、DB 与双 Bucket 集成 |
| AC-020 | 迁移在复制、验证、切换或业务验收阶段失败 | 重试或回滚 | 失败阶段和原因可查询；源对象与旧 service 保持/恢复可用；重复执行不产生重复对象或错误切换 | 故障注入与迁移幂等测试 |
| AC-021 | 对象不存在、配置不可服务/缺失或对象处于 PENDING 删除 | 解析或签发访问 URL | 返回可区分 OSS 失败且无 URL；不得回退默认配置或旧 `sys_oss.url` | OssLifecycle/OssService 失败路径单元测试 |
| AC-022 | 现有 SINGLE/MULTIPART、TEMP、引用和删除用例 | 执行既有回归 | 原状态机、完成幂等、引用保护和两阶段删除继续通过；业务只保存 ossId | 现有 OSS 后端/前端回归套件 |
| AC-023 | Ticket 创建后默认配置、策略或缓存发生变化 | resume/sign/complete/abort/cleanup | 全部动作仍使用 Ticket 冻结的 service/Bucket，不跨 Bucket 或重新路由 | UploadTicket 状态机测试 |
| AC-024 | PUBLIC 对象被传入显式强制私有签名方法 | 调用 `presignDownload` | 明确拒绝而不是返回带签名但实际仍公开的 URL | OssService 安全合同单元测试 |

## 5. 范围

### IN

- OSS 存储配置的 PRIVATE/PUBLIC_READ 双类型语义、启动校验、管理 API/UI 和 NAMEWTA 增量 SQL。
- 独立公共/受控 configKey 与 Bucket 的运行时选择和 Provider Policy/readiness 诊断。
- 命名 uploadPolicy 的 storageConfigKey 绑定，以及初始化阶段按配置选择 OssClient。
- UploadTicket 冻结路由的端到端保持；现有直传协议请求字段不新增客户端 configKey。
- `ruoyi-api` 统一结构化访问结果、公共 URL 解析、私有默认/命名 TTL 签名和旧 Service 方法兼容。
- 管理下载入口类型化响应和管理列表继续隐藏 URL。
- 历史对象 PRIVATE 基线、有对象配置变更保护、审核清单驱动的存储边界迁移和审计/回滚证据。
- 后端、前端配置界面、SQL、Provider 集成和运维发布门禁的验证。

### REUSE

- 复用现有 `OssFactory.instance(configKey)`、`OssClient` 预签名与自定义域名基础能力，不新增第二套 Provider SDK 封装。
- 复用现有命名 uploadPolicy、`OssUploadService`、Redis UploadTicket、SINGLE/MULTIPART、resume/complete/abort/cleanup 协议；只补存储路由字段和校验。
- 复用 UploadTicket 已有 `service`、`bucket`、`objectKey` 冻结字段及 `sys_oss.service`，不增加对象级访问类型副本。
- 复用 `OssLifecycleManager` 的对象存在/PENDING 检查、默认 2 分钟签名和 typed error 模式。
- 复用 `ruoyi-api` 的 `OssService` 跨模块边界、现有 Business OSS Owner 授权与引用协调合同。
- 复用现有 `SysOssConfig` CRUD、缓存变更事件、管理权限、前端配置页、Actuator health 和统一响应/异常映射。
- 复用 `<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DDL.sql</Path>` 与 `<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DML.sql</Path>` 的增量发布约定。

### OUT

- **OOS-001**：不提供匿名 `/resource/oss/{ossId}` URL 查询或公开对象目录。
- **OOS-002**：不自动管理 Bucket Policy、CORS、Lifecycle、CDN、DNS 或证书。
- **OOS-003**：不支持同 Bucket 对象 ACL 混合、PUBLIC_READ_WRITE 或公共匿名上传。
- **OOS-004**：不新增 `sys_oss.access_type` 或把稳定/签名 URL 持久化为对象访问权威。
- **OOS-005**：不改变 Business OSS Owner、`sys_oss_ref`、TEMP 默认 24 小时和主动清理审批合同。
- **OOS-006**：不在本 change 中新增具体门户页面或业务发布端点；当前仓库只有 admin App，业务接入另行指定 Owner。
- **OOS-007**：不自动迁移全部旧对象，不依据文件后缀、目录或旧 accessPolicy 猜测哪些资源应公开。
- **OOS-008**：不建设内容审核、病毒扫描、稳定公共 URL 的一次性撤销 Token 或用户绑定 URL。

## 6. 已锁定实现约束

- **DEC-001**：OSS 对象具有公共与受控两类读取语义；公共只匿名读，受控必须业务授权后短时签名。来源：`ADR-001`。
- **DEC-002**：公共与受控对象使用独立 configKey 和独立 Bucket，单配置不得混用对象 ACL。来源：`ADR-002`。
- **DEC-003**：公共 URL 只由 Business OSS Owner 随业务数据发布，不新增通用匿名 ossId 查询。来源：`ADR-003`。
- **DEC-004**：`OssService` 增加统一结构化 URL 解析并保留显式私有签名方法；Service 不替业务授权。来源：`ADR-004`。
- **DEC-005**：所有存量对象默认保持受控，只有审核清单内资源显式迁移到公共存储。来源：`ADR-005`。
- **DEC-006**：对象访问类型由 `sys_oss.service` 对应存储配置唯一决定，不增加对象级快照。来源：`ADR-006`。
- **DEC-007**：应用诊断但不修改 Provider Policy；不一致、不可确认或探测失败均 fail-closed。来源：`ADR-007`。
- **DEC-008**：生产 PUBLIC_READ 必须使用受管 domainUrl/CDN；开发 Provider URL 回退必须显式开启。来源：`ADR-008`。
- **DEC-009**：`selectUrlByIds/selectByIds` 保持签名并委托统一解析；管理列表不批量生成 URL。来源：`ADR-009`。
- **DEC-010**：PRIVATE 默认 TTL 为 2 分钟；服务端命名访问策略可在安全上下限内覆盖，客户端不能提交 TTL。来源：`ADR-010`。
- **DEC-011**：命名 uploadPolicy 固定绑定 storageConfigKey，UploadTicket 冻结实际路由。来源：`ADR-011`。
- **DEC-012**：应用只支持 PRIVATE 与 PUBLIC_READ，匿名写永不开放；旧值显式、安全迁移。来源：`ADR-012`。
- **DEC-013**：有存量对象的配置不能普通修改 Bucket 或访问类型，必须走可审计、可验证、可回滚的显式迁移。来源：`ADR-013`。

## 7. 数据、接口与兼容

### 公共接口变化

`ruoyi-api` 的 `OssService` 增加或演进以下稳定语义：

- `resolveAccessUrl(Long ossId) -> OssAccessUrl`。结果至少包含 `accessType`、`url`、可空 `expiresAt` 和 `fileName`。
- `presignDownload(Long ossId)` 保持默认 2 分钟 PRIVATE 下载；新增 `presignDownload(Long ossId, String accessPolicyKey)` 供服务端调用方选择命名访问策略。PUBLIC 对象调用这两个边界都必须拒绝。
- `selectUrlByIds(String)` 与 `selectByIds(String)` 方法签名保留，内部按对象类型统一解析。
- 公共访问类型序列化为 `PUBLIC`，受控类型序列化为 `PRIVATE`；PUBLIC 的 expiresAt 为 null，PRIVATE 非空。

现有 `GET /resource/oss/{ossId}/download-url` 继续要求 `system:oss:download`，返回类型化访问结果；现有前端只读取 `url/expiresAt/fileName` 的调用保持兼容。`GET /resource/oss/list` 与 `listByIds` 继续不携带可用 URL。不新增匿名 OSS Controller。

上传初始化、Part 签名、resume、complete 和 abort 的浏览器 HTTP 路径与请求合同保持；storageConfigKey 只出现在服务端 uploadPolicy 配置和 Ticket 内，不进入客户端请求。

### 数据模型与持久化

- `sys_oss.service` 继续保存对象实际 storage configKey，是对象访问类型和 Provider 选择的唯一入口。
- `sys_oss` 不增加访问类型字段；新直传对象继续不依赖持久化 URL，签名 URL不得写入 `url` 或 `ext1`。
- `sys_oss_config` 的访问策略在数据库、Java、OpenAPI 和管理 UI 中统一为 PRIVATE/PUBLIC_READ 两种语义；具体物理编码必须由增量迁移明确，不能沿用歧义标签。
- `sys_oss_config.status` 保持“唯一默认配置”语义，不复用为启用开关；默认配置必须是 PRIVATE。公共或其他非默认配置由命名策略、对象归属或迁移任务显式引用，并通过 readiness 判定是否可服务。
- 命名 uploadPolicy 增加 storageConfigKey；命名私有访问策略保存服务端 TTL。配置在启动时完成引用、状态、类型和时长边界校验。
- UploadTicket 继续持久化实际 service/Bucket，不需要保存访问类型副本或长期签名 URL。
- 存储边界迁移必须持久化可审计的批次/对象进度，至少能定位源/目标配置、ossId、当前阶段、验证结果、错误、重试和完成时间；具体表拆分由 Ticket 决定。

### 兼容要求

- 现有 `selectUrlByIds/selectByIds/presignDownload` 调用方不因方法删除而中断；新增结构化方法是推荐入口。
- 旧方法返回 URL 的生命周期按对象类型变化：公共稳定、受控短时。调用方不得把二者持久化或用查询参数猜测类型。
- 现有管理前端 `OssDownloadUrl` 接收新增 accessType 时保持向后兼容；URL 安全校验和错误可见性不退化。
- 现有 FileUpload/ImageUpload/Editor、SINGLE/MULTIPART、断点恢复、TEMP 和业务引用合同不变。
- 服务端内部未显式选择公共策略的既有上传继续进入 PRIVATE 默认配置；默认配置不得设置为 PUBLIC_READ。
- 仓库外调用方如依赖旧 `access_policy` 数值含义，必须在发布说明中迁移；不提供 PUBLIC_READ_WRITE/custom 兼容模式。

### 迁移要求

- NAMEWTA 增量只追加到 `<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DDL.sql</Path>` 和 `<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DML.sql</Path>`；不修改冻结的 `<Path>ruoyi-vue-plus-namewta/script/sql/ry_vue.sql</Path>`。
- Fresh 安装和既有数据库都必须得到相同的 PRIVATE/PUBLIC_READ 语义、必要索引/约束和管理字典。
- 所有升级前 `sys_oss` 行先保持 PRIVATE。旧配置值不得自动公开；无法确定的配置不得进入可服务集合，并要求运维明确处理。
- 发布前对每个旧 Bucket 验证匿名读取。实际公开但承载历史对象的 Bucket 必须先收紧 Policy 或显式迁入 PRIVATE Bucket。
- 公共迁移采用审核 ossId 清单，保持业务 ossId 与 `sys_oss_ref` 不变；复制、内容验证、service 切换、业务访问验证和源清理均可恢复。
- 迁移 dry-run 必须在修改对象或元数据前输出候选、来源、目标、冲突和不可迁移原因；正式迁移必须有审计证据。

### 发布或运维影响

- 部署必须提供独立 PUBLIC_READ 和 PRIVATE Bucket/configKey；PUBLIC_READ 仅匿名 GET/HEAD，PRIVATE 禁止匿名读取，两者都禁止匿名写。
- 生产 PUBLIC_READ 必须配置 domainUrl/CDN；DNS、证书、缓存和回源 Policy 由部署系统管理。
- 应用 readiness 纳入默认、启用策略引用、存量对象引用和进行中迁移涉及配置的静态校验与 Provider Policy 诊断。失败配置不得通过默认配置或旧缓存继续服务。
- 部署配置命名 uploadPolicy 的 storageConfigKey，以及 PRIVATE 命名访问策略和安全 TTL 上下限；Secret 继续由现有安全配置来源提供。
- 发布顺序必须先准备/验证 Provider 与域名，再发布应用配置和代码，最后执行审核后的公共对象迁移；不得先把历史 Bucket 改为 public-read。

## 8. 非功能要求

- **NFR-001 安全与隐私：** 公共只表示匿名读取对象内容，不公开 sys_oss 索引、业务记录、配置元数据或写能力。受控签名前必须由 Business OSS Owner 授权。客户端不能决定 configKey/TTL；Secret、签名 URL 和 Provider 内部错误不得持久化或进入诊断日志。
- **NFR-002 性能与容量：** 公共和受控文件字节继续直接在客户端与 OSS 之间传输。URL 解析不得代理对象字节；批量兼容方法应有界读取配置并避免重复远程诊断。Provider Policy 诊断不得在每次 URL 请求同步执行，而使用经健康门禁的配置状态。
- **NFR-003 可用性与可靠性：** 配置、路由和迁移 fail-closed。UploadTicket、Complete、迁移重试和 service 切换必须幂等。Provider/域名暂时异常不得把 PRIVATE 降级为 PUBLIC 或把目标配置回退为默认配置。
- **NFR-004 可观测性与运营：** readiness 能按 configKey 给出非敏感失败原因；上传/解析失败能区分策略、配置、对象状态和 Provider 类别；迁移可观察批次进度、失败阶段、重试和回滚。应用不得记录 AccessKey、SecretKey 或完整签名查询串。
- **NFR-005 兼容与发布：** 数据迁移、后端、OpenAPI/前端类型和配置 UI 必须配对发布；在 Provider/域名门禁通过前不允许公共流量。现有 PRIVATE 对象和业务引用不得出现公开窗口或 ossId 变化。

## 9. 验证策略

| 接缝 | 层级 | 覆盖合同 | 现有先例或命令 | Evidence 类型 |
|---|---|---|---|---|
| UploadPolicy、目标 OssClient 与 Ticket 冻结 | Java 单元 | AC-001/AC-002/AC-003/AC-023 | 扩展 `<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/upload/OssUploadPropertiesUnitTest.java</Path>` 与 `<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/upload/OssUploadServiceUnitTest.java</Path>`；cwd `<Path>ruoyi-vue-plus-namewta/</Path>` 运行 `./mvnw -pl ruoyi-admin -am -Dmaven.test.skip=false -Dtest='OssUpload*UnitTest' -Dsurefire.failIfNoSpecifiedTests=false test` | test output |
| 访问解析、TTL、PENDING 与强制私有边界 | Java 单元/合同 | AC-004/AC-006/AC-007/AC-008/AC-021/AC-024 | 扩展 `<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/lifecycle/OssLifecycleManagerUnitTest.java</Path>` 并新增 OssService 合同测试；同 Maven 定向测试模式 | test output |
| 配置语义、边界编辑和 readiness | ApplicationContext + Service/Health | AC-013/AC-014/AC-015/AC-016/AC-017/AC-018 | 配置绑定失败、缓存不变、Actuator health 和零 Provider mutation 替身测试；cwd `<Path>ruoyi-vue-plus-namewta/</Path>` 运行受影响 `ruoyi-admin` reactor 测试 | test output + health payload |
| 公共/私有真实访问矩阵 | S3-compatible 集成 | AC-001/AC-002/AC-005/AC-006/AC-008/AC-009/AC-016 | 扩展 `<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/client/MinioOssClientIntegrationTest.java</Path>`，使用独立 public/private Bucket 验证未签名与签名请求；外部环境缺失时不得宣称 E2E 通过 | integration output + HTTP status |
| Service/Controller 兼容与权限 | Spring MVC/Java 合同 | AC-003/AC-010/AC-011/AC-012/AC-021 | 现有 `/resource/oss` 权限矩阵、序列化结果和调用点扫描；管理列表断言 URL 为 null | test output + scan output |
| 配置管理前端与 transport | Vitest/类型检查 | AC-010 至 AC-015 | 扩展 `<Path>plus-ui-namewta/packages/domains/system/src/resource-service.test.ts</Path>` 和 web-domain 配置测试；cwd `<Path>plus-ui-namewta/</Path>` 运行 `pnpm --filter @namewta/domain-system test`、`pnpm --filter @namewta/web-domain-system test`、相应 typecheck | test/typecheck output |
| 管理配置与下载行为 | Playwright | AC-012/AC-013/AC-018 | 扩展 `<Path>plus-ui-namewta/e2e/system-resources.spec.ts</Path>`；cwd `<Path>plus-ui-namewta/</Path>` 运行定向 `pnpm exec playwright test e2e/system-resources.spec.ts` | trace/screenshot + test output |
| SQL fresh/upgrade 与存量安全 | MySQL migration | AC-013/AC-017/AC-018/AC-019/AC-020 | Fresh DDL -> DML、旧值 fixture、对象引用 fixture、迁移 dry-run/rollback；查询 schema、配置类型、service 和引用不变量 | query transcript + migration report |
| 存储边界迁移状态机 | Java + DB + 双 Bucket 集成 | AC-019/AC-020 | 故障注入复制、校验、切换、业务验收与重复执行，确认源清理延后 | test output + audit rows |
| 既有 OSS 回归与组装 | Maven/Vitest/build | AC-011/AC-012/AC-022/AC-023 | cwd `<Path>ruoyi-vue-plus-namewta/</Path>`：`./mvnw -pl ruoyi-admin -am -Dmaven.test.skip=false test`、`./mvnw -pl ruoyi-admin -am -DskipTests package`；cwd `<Path>plus-ui-namewta/</Path>`：受影响 test/typecheck/build | command output |
| 路由与禁止能力静态扫描 | 仓库检查 | AC-003/AC-010/AC-013/AC-022 | 扫描匿名 OSS Controller、PUBLIC_READ_WRITE/custom、客户端 configKey/TTL、默认 OssClient 直传初始化和管理列表 URL 回填 | scan output |

## 10. 风险、假设与未决问题

### 风险

- 不同 S3-compatible Provider 对 Bucket Policy 查询、匿名 HEAD、路径风格和自定义域名的支持不同。诊断适配必须用 capability 明确表达“已验证/无法确认”，不能把不支持误判为安全。
- 稳定公共 URL 是长期 bearer URL。业务撤下记录只能停止发现，无法使已经传播的 URL立即失效；需要紧急撤回时必须迁回 PRIVATE、删除对象或由部署系统收紧 Policy/CDN。
- domainUrl/CDN 可能缓存旧内容或旧 404；发布和迁移需要包含缓存键、失效和回源权限验收，但本 Spec 不规定某一家 CDN API。
- 旧 Bucket 的真实 Policy 可能与数据库 accessPolicy 不一致。上线前若跳过 Provider 门禁，历史附件存在误公开风险，因此 readiness 和迁移顺序属于发布阻断条件。
- 兼容方法会返回不同生命周期的 URL；不了解 accessType 的仓库外调用方可能继续持久化 URL，发布说明和调用点扫描不能省略。
- 跨 Bucket 迁移可能遇到大对象、Provider 限流、网络中断或校验能力差异；必须批量、有界、幂等且延迟清理源对象。

### 已采用的低影响假设

- 命名私有访问策略的具体键和 TTL 安全上下限属于服务端部署配置，不构成客户端 API；固定外部合同是默认 2 分钟、配置必须受上下限校验、客户端不能提交 TTL。
- Provider Policy 的具体探测实现由各 Provider capability 决定；外部合同是不能确认即不可服务，且诊断不修改 Policy 或业务对象。
- 迁移审计可复用项目现有基础字段和批处理惯例；具体表名、批次大小、重试退避和源对象安全窗口由 Tickets 在不改变本 Spec 恢复合同的前提下决定。
- 当前仓库只有 `@namewta/admin-web`，没有具体门户 App；本次以 `OssService` 和真实公共 URL Provider 集成作为平台验收，不虚构业务端点。

### 未决问题

无。
