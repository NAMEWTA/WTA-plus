# OSS 二次开发与数据流

先说结论：upstream 已经有一套可用的 OSS 管理和上传能力，但文件字节要经过 Java 后端。NAMEWTA 这次二次开发把它改造成了“后端管权限和票据，浏览器直接搬文件”，并补上大文件分片、断点续传、临时文件、业务引用、授权下载和可恢复删除。

本次文件差异以后端当前 `main@af7c27caa694` 对它与 upstream `6.X` 的共同固定点 `387c4f0a20e9`，以前端当前 `main@0bf978670e79` 对 upstream `6.X-Vue@0870ce175148`。截至当前已缓存的 upstream 后端 `b37db3fd01a8`，后来新增的 upstream 提交不涉及 OSS，因此下面的 OSS 结论不受影响。

## 先看全图

```text
upstream 原来的上传

[浏览器]
    |  文件字节
    v
[Java 后端] -- 文件字节 --> [对象存储]
    |
    +-- 写入 sys_oss


NAMEWTA 现在的上传

[浏览器] -- 文件说明 --> [Java 后端控制面]
    ^                         |
    |                         +-- 检查权限、策略和文件信息
    |                         +-- 决定对象 Key
    |                         +-- 保存 Redis 上传票据
    |                         +-- 返回短期签名
    |
    +------ 短期签名 ---------+
    |
    +-- 文件字节 -----------> [对象存储数据面]
                                  |
                                  v
                     [后端确认后登记 sys_oss]
                                  |
                                  v
                      [业务保存后登记 sys_oss_ref]
```

最重要的区别是：文件字节不再穿过 Java 后端。后端仍然掌握安全决定，只是不再亲自搬运文件。

整个改动可以分成六层：

```text
[前端上传组件]
      |
      v
[上传控制面 API] ---> [Redis 上传票据]
      |                       |
      v                       v
[公共 OSS 客户端] ------> [S3 / MinIO]
      |
      v
[sys_oss 元数据] <----> [sys_oss_ref 业务引用]
      |
      v
[头像、公告、通知附件、工作流附件]
```

## 一步一步看

### 第一步：upstream 原来怎样使用 OSS

upstream 的浏览器把文件包装成表单，调用：

```text
POST /resource/oss/upload
             |
             v
[SysOssController 接收 MultipartFile]
             |
             v
[SysOssServiceImpl 调 OssClient.upload]
             |
             v
[后端把字节写入 OSS，并登记 sys_oss]
```

下载则调用 `GET /resource/oss/download/{ossId}`。后端先从 OSS 读取完整文件，再以 `ResponseEntity<byte[]>` 返回浏览器。

这种方式简单，但大文件会同时占用后端的入口带宽、出口带宽、连接和 JVM 内存。上传失败也没有完整的分片续传会话。

### 第二步：公共 OSS 客户端增加了哪些底层能力

这层位于后端 `ruoyi-common/ruoyi-common-oss`，它不知道用户、业务表或 Controller，只负责把 S3 能力包装成稳定 Java 接口。

| 文件 | 相对 upstream 的变化 |
| --- | --- |
| `client/OssClient.java` | 增加预签名 PUT/GET、创建分片上传、给 Part 签名、查询 Part、完成/中止分片、HEAD 对象、复制对象、读取 Bucket 配置和能力探测。 |
| `client/AbstractOssClientImpl.java` | 用 AWS S3 SDK 实现上述能力，统一 Provider 错误，并兼容默认桶与显式桶两套调用。 |
| `exception/OssErrorCode.java` | 新增稳定的 OSS 错误分类。 |
| `exception/S3StorageException.java` | 扩展底层异常信息和错误转换。 |
| `model/OssPresignedRequest.java` | 表示短期签名的 URL、HTTP 方法、必须携带的 Header 和过期时间。 |
| `model/OssMultipartUpload.java`、`OssMultipartPart.java`、`OssCompletedPart.java`、`OssMultipartCompleteResult.java` | 表示分片会话、已上传 Part、完成请求和完成结果。 |
| `model/OssObjectOptions.java`、`OssObjectStat.java`、`OssChecksumAlgorithm.java` | 表示上传约束、HEAD 结果和校验和。 |
| `model/OssClientCapabilities.java`、`OssBucketConfiguration.java` | 描述 Provider 支持什么，并只读检查 Bucket CORS、ETag 暴露和未完成分片清理配置。 |
| `model/OssCopyResult.java` | 支持通知附件快照等服务端对象复制。 |

新增方法的核心不是“再封装一次上传”，而是让上层可以只签发通行证，不接触文件字节。

### 第三步：后端增加上传控制面

主要入口从一个上传接口变成一组会话接口：

```text
POST   /resource/oss/uploads
       初始化 SINGLE 或 MULTIPART 会话

POST   /resource/oss/uploads/{token}/parts/sign
       为一批缺失 Part 签发短期地址

GET    /resource/oss/uploads/{token}/parts?fingerprint=...
       核对文件并查询已经上传的 Part

POST   /resource/oss/uploads/{token}/complete
       校验对象并生成 ossId

DELETE /resource/oss/uploads/{token}
       取消会话并安排清理
```

对应文件如下：

| 文件或目录 | 负责什么 |
| --- | --- |
| `controller/system/SysOssUploadController.java` | 暴露上面五个 HTTP 接口，并统一返回上传错误。 |
| `oss/upload/OssUploadContracts.java` | 定义初始化、签名、续传、完成等请求和响应结构。 |
| `oss/upload/OssUploadService.java` | 上传状态机核心：验证、初始化、签名、续传、完成、取消和过期清理。 |
| `oss/upload/OssUploadProperties.java` | 读取并严格校验命名上传策略。 |
| `oss/upload/OssUploadTicket.java`、`OssUploadState.java`、`OssUploadMode.java` | 保存会话快照，并定义 SINGLE/MULTIPART 与各个状态。 |
| `oss/upload/RedisOssUploadTicketStore.java`、`OssUploadTicketStore.java` | 把会话、幂等结果和清理索引放入 Redis，并对同一 token 加锁。 |
| `oss/upload/DefaultOssUploadObjectStore.java`、`OssUploadObjectStore.java` | 把控制面动作翻译成公共 `OssClient` 调用。 |
| `oss/upload/DefaultOssUploadMetadataStore.java`、`OssUploadMetadataStore.java` | 完成校验后写入 TEMP 状态的 `sys_oss`。 |
| `oss/upload/DefaultOssUploadIdentityResolver.java`、`OssUploadIdentityResolver.java` | 取得当前 userId、Client 主键和权限，防止别人复用上传会话。 |
| `oss/upload/OssUploadCleanupTask.java`、`OssUploadCleanupRecord.java` | 处理过期或失败的上传会话。 |
| `oss/upload/OssUploadDiagnostics.java` | 只读检查 Bucket CORS、Expose ETag 和 Multipart Lifecycle，不替应用修改 Bucket。 |
| `oss/upload/OssUploadError.java`、`OssUploadException.java` | 给前端稳定、可识别的错误。 |
| `ruoyi-admin/src/main/resources/application.yml` | 定义 `general`、`avatar`、`image`、`editor-image`、`editor-video`、`document` 六种策略。 |

命名策略决定最大文件、允许的 Content-Type、对象前缀、单次/分片模式、分片大小、权限和可选 Client 准入。浏览器只提交策略名，不能自选 Bucket、对象 Key、Provider 或 uploadId。

当前配置还要特别注意：上传会话默认保存 24 小时，签名默认 5 分钟；主动清理目前是 `cleanup-enabled: false`、`cleanup-dry-run: true`。也就是说清理机制已经有代码，但生产启用仍需要单独审核 Bucket 和发布配置。

### 第四步：浏览器怎样完成单文件和大文件上传

小文件走单次直传：

```text
[选择文件]
    |
    +-- 计算文件指纹
    v
[后端 init：检查策略、权限、大小和类型]
    |
    +-- Redis 保存 UploadTicket
    +-- 返回 5 分钟签名
    v
[浏览器直接 PUT 到 OSS]
    |
    v
[后端 complete：HEAD 校验大小、类型、指纹和部分文件头]
    |
    v
[写入 TEMP sys_oss] -> [返回 ossId]
```

大文件走分片：

```text
[文件切成 Part 1、2、3...]
          |
          v
[后端分批签名，最多一个受控窗口]
          |
          v
[浏览器并发 PUT 各 Part 到 OSS]
          |
          +-- 失败 Part：重新签名，只重传这一块
          |
          +-- 页面刷新：IndexedDB 找回 token
          |              + Redis 找回 Ticket
          |              + OSS ListParts 找回已完成块
          v
[提交连续的 partNumber + ETag]
          |
          v
[后端与 Provider 的真实 Part 逐项核对]
          |
          v
[Complete Multipart + HEAD 校验 + 返回 ossId]
```

这里有三份不同位置的“便签”：

| 位置 | 保存什么 | 为什么需要 |
| --- | --- | --- |
| 浏览器 IndexedDB | 文件指纹、uploadToken、策略和到期时间 | 页面刷新后找到旧会话。 |
| Redis UploadTicket | 用户、Client、策略、对象 Key、uploadId、大小、类型和状态 | 后端不相信浏览器，所有安全字段以 Ticket 为准。 |
| OSS Provider | 实际对象或已经上传的 Part | 恢复时以真实 Part 为准，不只相信浏览器记录。 |

对应前端文件是：

| 文件 | 相对 upstream 的变化 |
| --- | --- |
| `src/api/system/oss/index.ts` | 删除旧 `uploadOss(FormData)`，增加 init、Part 签名、resume、complete、abort 和 download-url API。 |
| `src/api/system/oss/types.ts` | 增加会话、模式、状态、签名、Part 和下载地址类型；OSS 管理对象增加生命周期字段。 |
| `src/hooks/oss/useDirectOssUpload.ts` | 新的前端上传状态机，统一 SINGLE/MULTIPART、重试、恢复、取消、进度和错误。 |
| `src/hooks/oss/useDirectOssUpload.test.ts` | 覆盖分片失败重试和断点恢复等关键行为。 |
| `src/utils/oss/fingerprint.ts` | 生成用于识别“是不是同一个文件”的指纹。 |
| `src/utils/oss/resumeStore.ts` | 用 IndexedDB 保存可恢复会话。 |
| `src/utils/oss/transport.ts` | 独立 XHR 数据面，只把签名要求的 Header 和文件字节发给 OSS。 |
| `src/components/FileUpload/index.vue` | 保持原 `modelValue` 的 ossId 合同，但内部切到 `document` 策略直传。 |
| `src/components/ImageUpload/index.vue` | 内部切到 `image` 策略直传。 |
| `src/components/Editor/index.vue` | 图片、视频分别使用 `editor-image`、`editor-video`，正文保存 `oss://<id>` 标记。 |
| `src/views/system/user/profile/userAvatar.vue` | 头像改用 `avatar` 策略。 |
| `src/views/system/oss/index.vue` | 管理页增加 TEMP/BOUND/PENDING、过期时间和引用数展示。 |
| `src/utils/ossContent.ts`、`src/views/system/notice/index.vue` | 公告展示前通过有权限的业务接口把 `oss://<id>` 换成短期 URL。 |

因此大多数使用 `FileUpload` 或 `ImageUpload` 的业务页面不用自己理解分片。它们仍然收发 ossId，只是组件内部的数据流完全变了。

### 第五步：上传完成后，文件为什么还是 TEMP

“上传完成”只说明 OSS 中已经有对象，不说明某条业务数据真的保存成功。

```text
[complete 成功]
       |
       v
[sys_oss: TEMP，有过期时间，没有业务引用]
       |
       +-- 业务保存成功 --> [sys_oss_ref 新增引用] --> [BOUND，长期保留]
       |
       +-- 用户放弃保存 --> [一直无引用] --> [到期清理候选]
```

生命周期层修改或新增的文件是：

| 文件或目录 | 负责什么 |
| --- | --- |
| `ruoyi-api/.../OssService.java` | 给其他业务模块公开 `reconcileReferences`、生命周期快照和预签名下载，不暴露 system 内部 Mapper。 |
| `domain/SysOss.java`、`bo/SysOssBo.java`、`vo/SysOssVo.java` | 增加 `isTemp`、`expireTime`、`deleteState`、引用数和引用明细。 |
| `oss/domain/SysOssRef.java` | 表示一条“哪个真实业务表、哪条真实记录正在使用哪个 ossId”的关系。 |
| `oss/mapper/SysOssRefMapper.java`、`resources/mapper/system/SysOssRefMapper.xml` | 增删、恢复和反向查询引用。 |
| `mapper/SysOssMapper.java`、`SysOssMapper.xml` | 锁定对象、切换 TEMP、查询过期对象和维护删除状态。 |
| `oss/service/OssLifecycleManager.java` | 绑定、解绑、旧新集合协调、下载签名和两阶段删除的核心。 |
| `oss/service/OssTempCleanupTask.java` | 扫描到期 TEMP/PENDING 对象并重试清理。 |
| `oss/provider/OssObjectStore.java`、`DefaultOssObjectStore.java` | 生命周期层访问 Provider 的小接口。 |
| `oss/config/OssLifecycleProperties.java` | TEMP 保留期、下载签名期和清理批次配置。 |
| `oss/exception/OssLifecycleError.java`、`OssLifecycleException.java` | 有引用、删除中、对象不存在等稳定错误。 |
| `SysOssController.java` | 移除旧字节上传/下载接口，增加管理面 `/{ossId}/download-url`。 |
| `ISysOssService.java`、`SysOssServiceImpl.java` | 删除旧浏览器字节代理逻辑，接入生命周期管理器。 |
| `script/sql/namewta/DDL.sql`、`DML.sql` | 给 `sys_oss` 增加 TEMP/过期/删除状态，新增 `sys_oss_ref` 及索引；不回写 upstream 的 `ry_vue.sql`。 |

业务引用不是访问权限。`sys_oss_ref` 只回答“谁在使用它、能不能清理”，不能回答“当前用户能不能下载它”。

### 第六步：哪些业务开始真正拥有 OSS 文件

二次开发后，保存 ossId 的业务 Service 也要维护引用。当前显式登记了四类 Owner：

| 业务文件 | 保存位置 | 改动 |
| --- | --- | --- |
| `SysUserServiceImpl.java` | `sys_user.avatar` | 新增、换头像、删用户时协调旧新 ossId。 |
| `SysNoticeServiceImpl.java`、`SysNoticeController.java` | `sys_notice.notice_content` | 从富文本的 `oss://<id>` 提取引用；新增、修改、删除时协调；下载先走公告权限。 |
| `SysNotifyMonitorServiceImpl.java` 与 `system/notify/attachment/*` | `sys_notify_log.attachment_oss_ids` | 通知发送前复制附件快照并绑定；删除通知日志时解绑。 |
| `WorkflowHistoryOssOwner.java` | `flow_his_task.ext` | 流程历史附件新增、更新和删除时维护引用。 |

工作流的 `CompleteExecuteComponent.java`、`InstanceDeleteExecuteComponent.java`、`FlwInstanceServiceImpl.java` 和 `FlwTaskServiceImpl.java` 接入了 `WorkflowHistoryOssOwner`。完整 Owner 清单和防遗漏检查位于 `ruoyi-admin/src/test/resources/oss/business-oss-owners.json` 及 `ruoyi-admin/src/test/java/org/dromara/test/oss/owner/**`。

业务保存和引用协调处于同一个数据库事务：如果引用失败，业务写入也回滚。新增保存 ossId 的业务字段时，开发者不能只改表和页面，还要把这个业务登记为新的 OSS Owner。

### 第七步：下载和删除的数据流也变了

upstream 下载是后端返回文件字节。现在只返回短期通行证：

```text
[用户请求下载某条业务附件]
          |
          v
[业务 Service 检查业务权限和数据权限]
          |
          v
[OssService 生成短期下载 URL]
          |
          v
[浏览器直接从 OSS 下载字节]
```

OSS 管理员走 `system:oss:download`。普通业务必须先证明用户有权访问那条业务记录，再在内部调用 `OssService.presignDownload`。知道一个 ossId 并不自动拥有下载权，短期 URL 也不应存进业务表。

删除从“一次动作”变成可恢复的两阶段：

```text
[最后一条引用解除]
          |
          v
[对象重新变 TEMP，获得新的宽限期]
          |
          v
[清理任务把 ACTIVE 改为 PENDING]
          |
          v
[调用 Provider 删除对象]
          |
          +-- 失败 --> [保留 PENDING，下次重试]
          |
          v
[删除 sys_oss 元数据]
```

如果 PENDING 对象在删除前又被合法业务绑定，行锁内可以取消 PENDING。这样 Provider 短暂失败不会造成数据库先删、对象还在，或者对象先删、数据库仍说可用的难解释状态。

### 第八步：使用方式到底发生了什么变化

| 使用者 | upstream | NAMEWTA 当前版本 |
| --- | --- | --- |
| 普通前端页面 | 调 `uploadOss(FormData)`，文件发给后端 | 继续使用 FileUpload/ImageUpload，组件内部自动直传；页面仍拿 ossId。 |
| 自定义前端上传 | 组装 Multipart 表单 | 选择命名策略并复用 `createDirectOssUploadRequest`，不要自己拼签名或对象 Key。 |
| 后端业务模块 | 保存 ossId 即结束 | 在同一事务内调用 `OssService.reconcileReferences` 维护旧新引用。 |
| 普通业务下载 | 可能复用通用 OSS 地址 | 业务先鉴权，再调用内部 `presignDownload`，浏览器直连 OSS。 |
| OSS 管理 | 看文件元数据并直接下载/删 | 还能看 TEMP/BOUND/PENDING、过期时间和引用；有引用时通用删除拒绝。 |
| 运维 | 配 OSS Provider 即可 | 还要配置命名策略、Bucket CORS、Expose ETag、Multipart Lifecycle，并先观察 dry-run。 |

这次改动的真正中心不只是“直传”，而是把 OSS 从一个上传工具升级成了有控制面、数据面和生命周期的文件基础设施。

### 仍然存在的边界

```text
[已经实现]
  +-- 单次直传、Multipart、重试、同设备恢复
  +-- TEMP、引用、短期下载、可恢复删除
  +-- MinIO/单元/架构合同测试

[没有承诺]
  +-- Redis Ticket 丢失后的跨设备无感恢复
  +-- 杀毒、恶意文件扫描、内容隔离
  +-- Client 级 OSS 数据隔离
  +-- 已启用的生产主动清理
  +-- 浏览器真实 OSS 上传角色矩阵 E2E
```

后端聚焦 OSS 差异约为 67 个生产/测试文件、5803 行新增和 179 行删除；前端核心 OSS 与消费者差异为 14 个文件左右。数字用于说明改动规模，不等于 67 个独立功能。

## 术语小词典

- 对象存储（OSS）：专门保存图片、附件和大文件的服务，例如 MinIO、Amazon S3、阿里云 OSS。
- 控制面：决定谁能上传、文件放哪里、票据多久有效的后端逻辑。
- 数据面：真正搬运文件字节的通道；现在是浏览器和 OSS 之间直连。
- 短期通行证（预签名请求）：只允许在短时间内执行指定 PUT 或 GET 的 URL 和 Header 合同。
- 对象 Key：文件在 Bucket 内的唯一地址，由后端生成，不由浏览器决定。
- 单次直传（SINGLE）：一个请求直接上传整个小文件。
- 分片上传（MULTIPART）：把大文件切成多块上传，失败时只重传缺失块。
- 文件指纹：用文件名、大小、类型和局部内容计算的标识，用来核对重新选择的是不是同一文件。
- 上传票据（UploadTicket）：Redis 中的服务端会话便签，冻结用户、Client、策略、Key、uploadId 和状态。
- 临时文件（TEMP）：已经上传成功，但还没有业务记录引用的对象。
- 业务引用（sys_oss_ref）：记录真实业务表和真实主键正在使用某个 ossId；它保护生命周期，但不授予访问权限。
- 两阶段删除（PENDING）：先把“准备删除”写进数据库，再删 Provider 对象；失败后可以重试。
- 文件标记（`oss://<id>`）：富文本中长期保存的稳定 ossId 标记，展示时才换成短期 URL。

## 你现在能复述什么

1. upstream 的 OSS 已经能上传和管理文件，但文件字节需要经过 Java 后端；NAMEWTA 把字节通道改成浏览器直连 OSS。
2. 后端没有退出流程，而是变成控制面：检查权限和命名策略、保存 Redis Ticket、签发短期地址、校验完成结果并登记 ossId。
3. 小文件单次 PUT，大文件分片 PUT；浏览器 IndexedDB、Redis Ticket 和 OSS ListParts 一起支持同设备断点续传。
4. complete 后文件先是 TEMP；业务保存成功后，业务 Service 用 `sys_oss_ref` 把它变成被引用的长期文件。
5. 下载前仍需业务鉴权，只是后端返回短期 URL，文件字节直接来自 OSS。
6. 头像、公告、通知附件和工作流历史附件已经接入引用生命周期；以后新增保存 ossId 的业务也必须成为明确 Owner。
7. 清理与删除机制已经实现，但当前主动清理默认关闭且处于 dry-run 配置，生产启用仍需单独验收。

事实依据：两个产品仓库的固定点 Git diff；当前 `OssClient`、上传控制面、生命周期、业务 Owner 和前端直传源码；`<Path>speculo/.speculo/specdev/archive/2026-08/2026-08-21-oss-direct-unified-notification/spec.md</Path>`、ADR、Final Audit 和 T-17/T-18 Evidence。
