---
lesson_id: L-027
objective_ids: [OBJ-27]
claimed_cells: [A:SysOssUploadController.init,signParts,parts,complete,abort, B:SysOssUploadController.complete / abort, D:主路径 OSS 直传, D:失败路径 OSS abort]
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: five-windows
    minutes: 10
  - segment: complete-abort-rollback
    minutes: 10
  - segment: visuals-and-worked-examples
    minutes: 8
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 3
expression_level: eli5
coverage_depth: deep
source_ids: [S-004, S-008, S-L027-01, S-L027-02, S-L027-03, S-L027-04, S-L027-05, S-L027-06, S-L027-07, S-L027-08]
---

# Lesson 027：直传控制面的宏观地图——`init` / `signParts` / `parts` / `complete` / `abort`

## 学完你能做什么

打开 `backend/wta-modules/wta-system/src/main/java/org/namewta/system/controller/system/SysOssUploadController.java`，你能**口述浏览器直传的五扇 JSON 窗**，以及 complete 失败时对象和票据怎么回滚。口试名单就是矩阵这四格，不是对象列表窗、不是配置切换、不是浏览器 adapter：

1. **`A:SysOssUploadController.init, signParts, parts, complete, abort`**：门牌 `/resource/oss/uploads`。磁盘上正好这五个映射，外加一只 `@ExceptionHandler`。文件字节**不**经过这扇门。
2. **`B:SysOssUploadController.complete / abort`**：complete 校验通过才插入 `sys_oss` 临时行并返回 ossId 字符串；校验失败在服务端自己 `abort`+删对象+撕票据，不留这行。HTTP `abort` 清未完成会话；已经 COMPLETED 的只摘补偿索引，不删对象。
3. **`D:主路径 OSS 直传`**：init 开票 → 浏览器按预签名 PUT 对象（数据面，本课只认它不经过 Controller）→ complete 落临时引用 → 业务表以后再挂 ossId。
4. **`D:失败路径 OSS abort`**：complete 失败或用户取消 → Provider 中止分片、删对象、Redis 会话消失；没有业务引用。

`wta-system` 在登记表是 **classic**。这条线是 `Controller → OssUploadService`，**不是** `ISysOssService`，也**没有** UseCase 目录。不要把 `SysOssController`（`/resource/oss` 列表/下载/删除）和本课五扇窗并成一个类。

本课**不讲** `SysOssConfigController` 切默认桶（L-025）、`SysOssController.list/listByIds/downloadUrl/remove`（L-026）、`SysOssMigrationController`（L-028）、`createOssUploadClient` / `useDirectOssUpload` / IndexedDB 续传（L-029）。今天只认：**票房怎么开票、怎么盖章、怎么作废。**

## 先把宏观地图放在桌上

L-005 已经把「浏览器 `PUT` 对象是非 CRUD 例外」点名，并把走查留给本课。L-007 把厅堂网关插在 `systemService.resources.oss` 上，但那是组合点。L-026 才是对象目录的列表窗。本课站在 **`SysOssUploadController` 这一头**：只发 JSON，把短时钥匙交给浏览器，自己不扛行李箱。

2026-09-16 工作树里，五扇窗**共用一块牌子** `/resource/oss/uploads`，权限注解也共用 `system:oss:upload`：

```text
浏览器 / adapter（PUT 字节是 L-029，本课只认它不进这扇门）
        │
        ├─ POST   /resource/oss/uploads                         init
        │           JSON：policy / fileName / fileSize / contentType / fingerprint
        │           开 Redis 票 + 可能带回一张 SINGLE 预签名 PUT
        │
        ├─ POST   /resource/oss/uploads/{uploadToken}/parts/sign   signParts
        │           JSON：partNumbers[]（去重、排序、≤ maxSignParts）
        │           只对 MULTIPART；状态改成 UPLOADING
        │
        ├─ GET    /resource/oss/uploads/{uploadToken}/parts        parts
        │           query：fingerprint（必填）
        │           Java 方法名 parts，服务方法名 resume
        │
        ├─ POST   /resource/oss/uploads/{uploadToken}/complete     complete
        │           JSON：parts 可缺；返回 R<String> 的 ossId
        │           校验失败：服务端回滚，不靠前端再打一枪才删对象
        │
        └─ DELETE /resource/oss/uploads/{uploadToken}              abort
                  未完成：abortMultipart + deleteObject + 撕票
                  已完成：只摘 cleanup 索引
```

| Java 方法 | HTTP | 路径 | 入参 | 成功返回 | 改了什么 |
| --- | --- | --- | --- | --- | --- |
| `init` | `POST` | `/resource/oss/uploads` | `InitRequest` | `R.ok(InitResponse)` | Redis 票 + 补偿记录；Provider 上 prepare（SINGLE 预签名或 createMultipart） |
| `signParts` | `POST` | `/{uploadToken}/parts/sign` | `SignPartsRequest` | `R.ok(SignPartsResponse)` | 票状态 `UPLOADING`；短时 Part 预签名（不存进票） |
| `parts` | `GET` | `/{uploadToken}/parts` | `fingerprint` | `R.ok(ResumeResponse)` | **不改**票；SINGLE 会**新签**一张 PUT |
| `complete` | `POST` | `/{uploadToken}/complete` | `CompleteRequest` 可空 | `R.data(String ossId)` | 临时 `sys_oss` 行；票 `COMPLETED`；摘补偿索引 |
| `abort` | `DELETE` | `/{uploadToken}` | 无 body | `R.ok()` | 未完成则清对象+撕票；已完成只摘索引 |

**图题 / caption：** 直传控制面五扇窗的宏观地图。alt：同一前缀下 POST init、POST sign、GET parts、POST complete、DELETE abort；字节走预签名 URL 不进 Controller。

**文字等价物：** 票房在 `wta-system` 的 `SysOssUploadController`。浏览器先用 JSON 换一张 `uploadToken`。真正的文件字节走 MinIO/OSS 的预签名 URL，方法通常是 `PUT`，**不**打 `/resource/oss/uploads`。传完再回来 complete，服务端 HEAD 对象、对一下大小/类型/指纹/魔数，才插入 `sys_oss`。半路作废打 DELETE abort。`parts` 是续传查询，不是上传分片本身。

类上**没有** `BaseController`，五个映射都**没有** `@Log`。这是 API-005「变更 POST + `@Log`」的存量偏差：init/signParts/complete 是 POST 无日志切面；abort 是 DELETE。口试按磁盘，不把偏差改口成规范已经满足。

## 核心概念与机制

### 直觉讲解

把直传想成**火车站行李寄存**：

- 窗口不帮你扛箱子。窗口只开一张纸条（`uploadToken`），再给你一把**几分钟就失效**的仓库钥匙（预签名 URL）。
- 你自己把箱子推进仓库（浏览器 `PUT` MinIO）。窗口从未碰箱子里的衣服。
- 你拿纸条回来盖章（`complete`）。窗口派人去仓库看箱子是否还在、重量对不对、封条指纹对不对、打开一条缝看魔数。合格才在目录上写一行临时编号（`ossId`）。
- 你不想存了，或盖章时发现是空箱/假封条：窗口把仓库格子清掉，纸条作废（`abort` / complete 失败回滚）。目录上**不会**出现那一行。

**类比失效处：** 纸条不是纸，是 Redis JSON，TTL 默认 24 小时；钥匙 TTL 默认 5 分钟，续传 SINGLE 时窗口会**新配一把钥匙**，不会把旧钥匙写进纸条。仓库和窗口不是同一栋楼。complete 写入的是 **`isTemp=Y` 的临时目录行**，不是用户头像、不是公告附件；业务表以后自己挂这个 ossId（L-026 对账，L-022 头像是其中一个消费者）。abort 已经 COMPLETED 的票**不会**把已经编目的箱子扔掉。

### 精确定义与 English term

| 中文说法 | English term | 磁盘定义 |
| --- | --- | --- |
| 浏览器直传 | direct upload / browser-side upload | 文件字节由浏览器发到对象存储；本课 Controller 只做 JSON 控制面 |
| 控制面 / 数据面 | control plane / data plane | 控制面：`/resource/oss/uploads/**`。数据面：`OssPresignedRequest.url` 上的 PUT |
| 预签名请求 | presigned request | `method` + `url` + `requiredHeaders` + `expiresAt`。调用方必须原样带请求头 |
| 上传票 | upload ticket | Redis `OssUploadTicket`：token、policy、mode、state、objectKey、fingerprint、userId、clientPk…**不含**可长期复用的签名 URL |
| 分片上传 | multipart upload | Provider `createMultipartUpload` + 按 partNumber 预签名 + `completeMultipartUpload` |
| 指纹 | fingerprint | 客户端提交的字符串（≤512）。票里原样保存，对象元数据写它的 SHA-256，键名 `upload-fingerprint` |
| 完成 / 中止 | complete / abort | complete：校验对象并 `registerTemporary`。abort：清未完成会话 |
| 临时对象 | temporary object | `sys_oss.isTemp='Y'`，`ext1.source=directUpload`，`url=""`，到期 `oss.lifecycle.temp-retention`（默认 24h） |
| 幂等完成 | idempotent complete | 已 COMPLETED 或 objectKey 已有行 → 直接返回已有 ossId，不再 insert |

错误码是枚举 `OssUploadError` 的**名字**，经 `handleUploadException` 放进 `ErrorResponse.error`，和 `msg` 中文并列。前端靠名字区分 `FINGERPRINT_MISMATCH` 和 `SESSION_EXPIRED`，不要只读句子。

### 机制/因果链

五扇窗都先过 `@SaCheckPermission("system:oss:upload")`，再进 `OssUploadService`。策略自己还有一把锁：`policy.requiredPermission`（默认也是 `system:oss:upload`；`application.yml` 里 `richtext-*` 是 `common:richtext:upload`）。所以富文本策略要**两把钥匙都有**：进门注解 + 策略权限。`allowedClientPks` 非空时，当前 `LoginUser.clientPk` 还必须在名单里。

身份来自 `DefaultOssUploadIdentityResolver`：`LoginHelper.getLoginUser()`，没有 `userId` 就 `ACCESS_DENIED`。票上冻结的是 **userId + clientPk**。之后每一枪 `requireOwner`：换用户或换 Client 都是 `SESSION_OWNER_MISMATCH`。

#### 1. `init`——开票，还没有业务引用

`POST /resource/oss/uploads`，body：

```text
InitRequest { policy, fileName, fileSize>0, contentType, fingerprint }
```

服务顺序（失败必须在 Provider/票有副作用**之前**停住的，测试已经钉死）：

1. 校验文件名：非空、≤255、不含 `/` `\`、不含 ISO 控制符；后缀 ≤10。指纹非空 ≤512。
2. `properties.requirePolicy(policy)`：没有或未启用 → `INVALID_POLICY`。
3. `authorizePolicy`：缺策略权限或 Client 不在白名单 → `ACCESS_DENIED`。此时还没有票。
4. 规范化 Content-Type（去参数、小写）；超 `maxSize` 或不在允许类型 → `INVALID_FILE`。
5. `requireStorageRoute`：就绪表该 `storageConfigKey` 必须 `SERVING`，且 `accessPolicy` 等于策略的 `expectedAccessPolicy`。未就绪 → `STORAGE_NOT_SERVING`；策略不一致 → `STORAGE_ACCESS_POLICY_MISMATCH`。**prepareCalls=0，tickets 空。**
6. `policy.resolveMode(fileSize)`：`AUTO` 时 `fileSize >= multipartThreshold` 才 MULTIPART，否则 SINGLE。写死 SINGLE/MULTIPART 则不看阈值。
7. `objectStore.prepare(...)`：按**策略上的** `storageConfigKey` 取 `OssFactory`。SINGLE：`presignPut`，带 Content-Type 和 `x-amz-meta-upload-fingerprint`。MULTIPART：`createMultipartUpload`，InitResponse **没有** `presignedRequest`。
8. 造 `uploadToken = UUID`，票状态 `INITIALIZED`，TTL `oss.direct-upload.ticket-ttl`（默认 24h）。补偿记录 TTL 默认 7 天。
9. `ticketStore.create` 失败：`cleanupPrepared`（abort+delete，吞异常）再抛 `STATE_STORE_FAILURE`。不要留下一个没票的 multipart。

返回 `InitResponse`：`uploadToken`、`mode`、`expiresAt`、可选 `presignedRequest`、MULTIPART 才有的 `partSize`/`partCount`。票**冻结**当时的 `service`（存储配置键）。之后改 yaml 的 `storage-config-key`，resume/abort 仍打冻结的那条路由。

默认 yaml 的一个坑：`general.max-size=10MiB`，`multipart-threshold=100MiB`。AUTO 在这套数字下**永远走 SINGLE**，因为合法文件到不了阈值。不要看见 AUTO 就口述「大文件一定分片」。`image` / `editor-video` / `document` 的阈值才可能被 maxSize 够到。

#### 2. `signParts`——只给 MULTIPART 开一小扇签名窗

`POST .../{uploadToken}/parts/sign`。全程 `ticketStore.locked`。

- 票必须仍属于当前人、未过期、不是 ABORTED/EXPIRED。
- `mode` 必须是 MULTIPART 且有 `uploadId`，否则 `INVALID_STATE`。
- `partNumbers` 非空、去重后数量不变（禁止重复）、每个 ∈ `[1, partCount]`、数量 ≤ `maxSignParts`（默认 20）。
- 把票写成 `UPLOADING`，再按编号 `presignUploadPart`。返回的 `SignedPart` 字段就是 `partNumber/method/url/requiredHeaders/expiresAt`，**没有**嵌套的 `OssPresignedRequest`（HTTP 合同测试钉死）。

签名 URL 不写回 Redis。过期了再 sign 一次。窗口故意很小，避免一次把一万个 part 的 URL 吐给浏览器。

#### 3. `parts`——续传查询，Java 名叫 parts，服务名叫 resume

`GET .../{uploadToken}/parts?fingerprint=`。`@NotBlank`。

这是五扇里唯一的 GET。它**不** `saveTicket`。

- fingerprint 必须与开票时**原串**相等，不是比 SHA-256。错了 → `FINGERPRINT_MISMATCH`。
- 已 `COMPLETED` 且有 ossId：返回 `completedOssId`，**不再**发 PUT 预签名（`presignSingleCalls=0`）。
- SINGLE 未完成：现场 `presignSingle` 一张新 PUT（票 24h、钥匙 5min，续传要换钥匙）。
- MULTIPART 未完成：`listParts` 映射成 `uploadedParts{partNumber,eTag,size}`，`presignedRequest=null`。下一步仍是 signParts，不是这条 GET 附赠 URL。

前端厨房把这扇窗叫做 `resumeUpload`。口试后端时说 `parts` → `uploadService.resume`。不要把 Java 方法名改口成 resume。

#### 4. `complete`——(b) 落临时引用；失败自己回滚

`POST .../{uploadToken}/complete`。body `required = false`；`null` 当成 `CompleteRequest(List.of())`。返回类型是 `R<String>`，Controller 走 `R.data(...)`（与 `R.ok(data)` 同为成功码 +「操作成功」，合同测试认 data=`"9001"`）。

`completeLocked` 因果链：

1. 已 COMPLETED 且有 ossId：摘掉 cleanup，直接返回该 ossId。第二次 complete **不再** `registerTemporary`。
2. `metadataStore.findByObject(service, objectKey)` 已有行：`markCompleted` 后返回已有 id。这是「票写成 COMPLETED 失败、但 MySQL 已插入」的恢复门。
3. 否则票改 `COMPLETING`。
4. `headIfPresent`：
   - MULTIPART 且对象还没有：必须提交**全部** part（数量=`partCount`、编号 1..N 连续、ETag 非空）。再和 Provider `listParts` 逐个对编号/ETag（引号可剥），非末片大小必须等于 `partSize`，总和必须等于 `fileSize`。然后 `completeMultipartUpload`，再 HEAD。
   - SINGLE 若 `request.parts` 非空 → `INVALID_PARTS`（「SINGLE Complete 不接受 Part」）。
5. 没有对象 → `COMPLETE_VALIDATION_FAILED`「OSS 对象不存在」。
6. `validateCompletedObject`：size、Content-Type、元数据 `upload-fingerprint` 必须等于票里的 SHA-256。对 png/jpeg/gif/pdf/zip/ogg/webm/webp/mp4/mpeg/wav 再读最多 16 字节魔数。对不上 → 同一错误码。
7. `registerTemporary`：按 `(service, fileName=objectKey)` 查重；没有则 insert `sys_oss`：`url=""`，`isTemp="Y"`，`ext1` JSON 含 `fileSize`/`contentType`/`source=directUpload`/`isTemp=true`/`uploaderClientPk`，`expireTime=now+tempRetention`。
8. `markCompleted`：票带 ossId 写成 COMPLETED，`removeCompletedCleanup`。若这一步 Redis 失败 → `STATE_STORE_FAILURE`，**不**走下面的 cleanupAfterFailure（对象和 MySQL 行都要留着，重试走第 2 步）。

**失败回滚（本课 (b) 与 D abort 的服务端半边）：** 第 4–7 步的 `OssUploadException` 或 Provider `RuntimeException` 都进 `cleanupAfterFailure` → `cleanupSession(ticket, false)`：

- `objectStore.abort`：仅 MULTIPART 且有 uploadId 时 `abortMultipartUpload`。
- `objectStore.deleteObject`：`delete(objectKey)`。
- `ticketStore.removeSession`：删 `oss:upload:ticket:`、`oss:upload:cleanup:`、到期索引。
- Provider 清理失败：**不**把异常再抛给 complete 调用方（`propagateFailure=false`），只 `scheduleCleanup` 把索引时间拨到现在。complete 仍然把原来的校验错误抛出去。
- 单测 `shouldDeleteBadMagicAndNeverRegisterMetadata`：坏 PNG 魔数 → `COMPLETE_VALIDATION_FAILED`，`deleteCalls=1`，`registerCalls=0`，`tickets.get(token)=null`。

所以矩阵那句「complete 失败 → abort，不留业务引用」**不是**「前端必须再打 DELETE 才算 abort」。服务端 complete 失败路径已经做了和 abort 相同的 Provider 清理。L-029 的 adapter 只在 `AbortSignal` 取消时才调 `abortUpload`；校验失败那条由本课服务自己收摊。

complete **不算**业务引用写完。它只交出一个可被引用的临时 ossId。用户头像、富文本、流程附件把 ossId 写进自己的表，再 `reconcileReferences`，是 L-026 / L-022 / L-029 消费者的事。本课 D 主路径里的「业务引用」停在：**目录上出现临时行，调用方拿到 id；用户表还没改。**

#### 5. `abort`——(b) 清票据；已完成的箱子留下

`DELETE .../{uploadToken}`。也在锁里。

1. `ticketStore.get` 为 null → 直接成功（幂等空操作）。
2. `requireOwner`：别人的票不能帮你撕。
3. 状态已 COMPLETED：`removeCompletedCleanup`，**return**。不 `abortMultipart`，不 `deleteObject`，不 `removeSession` 里的对象侧。
4. 否则 `cleanupSession(ticket, true)`：Provider 失败或撕票失败会抛 `PROVIDER_FAILURE` / `STATE_STORE_FAILURE`（与 complete 失败不同，HTTP abort 要把清理失败告诉调用方）。

主动清理任务 `OssUploadCleanupTask` **不是**本课 (a) 方法。默认 `oss.direct-upload.cleanup-enabled=false` 且 `cleanup-dry-run=true`。`cleanupExpired` 遇到 COMPLETED 或 MySQL 已有该 objectKey 的行，**禁止**删对象。不要把定时器说成生产默认会扫桶。

#### Redis 三键 + 一把锁

| 键 | 作用 |
| --- | --- |
| `oss:upload:ticket:{token}` | 票 JSON，TTL=ticketTtl |
| `oss:upload:cleanup:{token}` | 补偿记录（mode/service/objectKey/uploadId），TTL=cleanupRecordTtl |
| `oss:upload:lock:{token}` | Redisson 锁；signParts / complete / abort / cleanupExpired 都经 `locked` |
| `oss:upload:expire-index` | 按 expiresAt 打分的有序集合，给清理任务找过期 token |

`create` 用 `setIfAbsent`；token 撞车 → `STATE_STORE_FAILURE`，并回滚刚写的 cleanup/索引。票里故意不存预签名 URL，避免 24h 票把 5min 钥匙变成「看起来还能用」。

`OssUploadState` 枚举有 `ABORTED` / `EXPIRED`，但 HTTP abort 的主路径是 **删键**，不是把状态写成 ABORTED 再留着。`requireOwned` 若读到这两种状态会 `INVALID_STATE`。口试成功 abort 之后：`get(token)==null`。

### 图、表或文本图

**图题 / caption：** D 主路径：开票、自己送货、盖章拿临时号。alt：init 写 Redis 与预签名；浏览器 PUT MinIO；complete HEAD 后 insert sys_oss isTemp=Y 返回 ossId。

```text
[控制面 JSON]                         [数据面字节]
浏览器 ──POST init──▶ 票房
          │              ├─ 查策略 / 就绪 / 身份
          │              ├─ prepare：预签名或 createMultipart
          │              └─ Redis ticket + cleanup
          ◀─ uploadToken + (SINGLE 的 PUT 钥匙)
          │
          │  PUT url + requiredHeaders
          └──────────────────────────────▶ MinIO/OSS
                                           objectKey + metadata fingerprint
          │
          ──POST complete──▶ 票房
                              ├─ HEAD / magic / 对大小
                              ├─ registerTemporary → sys_oss(isTemp=Y)
                              ├─ 票 COMPLETED，摘 cleanup
                              └─ R.data(ossId)
业务表以后挂 ossId（非本课五方法）
```

**文字等价物：** 主路径三跳。第一跳只开票和（SINGLE）发钥匙。第二跳浏览器直连对象存储，Controller 代码路径上没有 `MultipartFile`、没有 `InputStream` 整文件。第三跳 complete 才碰 MySQL。ossId 进业务表是第四跳，本课不写那张表。

**图题 / caption：** D 失败路径：盖章失败或取消，不留目录行。alt：complete 校验失败 cleanupAfterFailure；用户取消 DELETE abort；COMPLETED abort 只摘索引。

```text
                    complete 校验/HEAD/魔数失败
                              │
                              v
                    cleanupAfterFailure
                       abortMultipart?
                       deleteObject
                       removeSession
                    ──▶ 无 sys_oss 行，票键消失

用户点取消 / AbortSignal
                              │
                              v
                    DELETE abort
                       票不存在 → 200 空成功
                       COMPLETED → 只摘 cleanup 索引（对象和行留下）
                       其它状态 → 与上框相同的 Provider 清理
                                  但 propagateFailure=true
```

**文字等价物：** 两条失败入口，一扇服务端内部，一扇 HTTP DELETE。未完成会话的对象都要从桶里拿掉，Redis 票要消失，MySQL 不应出现该 objectKey。已经盖章成功的票再 abort，只是告诉清理工「别把这件当成垃圾」。定时清理默认关闭；即便打开，见到 COMPLETED 或已有 metadata 也不得删对象。

### 正例、反例与边界

**正例 1：** 合同测试 `shouldExposeOnlyFixedControlPlaneRoutes`：根路径 `/resource/oss/uploads`；init 的 PostMapping 值为空串；signParts `/ {uploadToken}/parts/sign`；parts GET `/ {uploadToken}/parts`；complete `/ {uploadToken}/complete`；abort DELETE `/ {uploadToken}`。没有第六个业务映射。

**正例 2：** SINGLE 开票返回 `method=PUT`，`requiredHeaders` 含 `x-amz-meta-upload-fingerprint`。complete 两次得到同一个 `"1001"`，`registerCalls=1`，`completeMultipart` 次数为 0，cleanup 记录被摘掉。

**正例 3：** 强制 MULTIPART 后 `signParts([2,1])` 返回的编号是排过序的 `[1,2]`。resume 指纹不对立刻 `FINGERPRINT_MISMATCH`。complete 时请求 ETag 带引号 `"etag-1"` 与 Provider 不带引号仍算相同。

**正例 4：** 换 `userId` 或换 `clientPk` 再 resume → `SESSION_OWNER_MISMATCH`。开票时 `allowedClientPks={100}` 而当前 200 → `ACCESS_DENIED` 且 tickets 仍空。

**正例 5：** 坏魔数 complete：删对象、不登记、票消失。这是 D abort 在 complete 方法体内的样子。

**正例 6：** Redis 在 `markCompleted` 时倒下：抛 `STATE_STORE_FAILURE`；再 complete 一次靠 `findByObject` 找回 `"1001"`，`registerCalls` 仍是 1。清理任务就算索引残留也不得删这个对象。

**正例 7：** 票冻结 `service=minio` 之后改策略到 `portal`，abort 仍打 minio。路由在 init 定死。

**正例 8：** Controller `handleUploadException`：`FINGERPRINT_MISMATCH` 的 JSON data 是 `{ error: "FINGERPRINT_MISMATCH" }`，msg 是「文件指纹不匹配」。机器码和人话分开。

**正例 9：** 前端 `resource-service.ts` 五枪路径与本课对齐：`initUpload` POST 根；`signParts` POST `.../parts/sign`；`resumeUpload` GET `.../parts`；`completeUpload` POST `.../complete`；`abortUpload` DELETE 根 token。token 会 `encodeURIComponent`。这是对照，格子仍归本课后端 + L-029 厨房。

**反例 1：** 「字节从 `SysOssUploadController` 进 Spring 再转存 MinIO。」五个方法没有任何文件参数。

**反例 2：** 「`parts` 用来上传分片。」分片 PUT 走 signParts 给的 URL。GET parts 只是续传查询。

**反例 3：** 「Java 方法叫 `resume`。」Controller 映射方法叫 `parts`。服务才是 `resume`。

**反例 4：** 「complete 用 `R.ok`，和 init 一样。」complete 源码是 `R.data`；合同测试认返回类型 `R<String>`。成功语义相同，口试按调用。

**反例 5：** 「complete 等于业务引用已经挂上用户。」它 insert 的是临时行，`url` 空串。头像页还要再 `updateProfile({ avatar: ossId })`。

**反例 6：** 「complete 失败必须再打 abort 才删桶。」服务端已经 `cleanupAfterFailure`。缺的是调用方看到错误码，不是缺第二枪 HTTP。

**反例 7：** 「abort 已完成的票会把文件从 MinIO 删掉。」只摘 cleanup。

**反例 8：** 「`/resource/oss` 和 `/resource/oss/uploads` 是同一个 Controller。」前者 `SysOssController`（list/listByIds/download-url/remove），后者本课。

**反例 9：** 「这是 layered，UseCase 在 `oss/upload`。」`OssUploadService` 是 `@Service`，登记表 classic。`DefaultOssUploadMetadataStore` 自己持有 `SysOssMapper`，不经过 `ISysOssService`。

**反例 10：** 「五扇窗都有 `@Log`，所以操作日志里看得到每次直传。」磁盘零 `@Log`。

**反例 11：** 「abort 是 POST，因为 API-005 说变更用 POST。」映射是 `@DeleteMapping`。

**反例 12：** 「`OssUploadMode.AUTO` 会出现在票上。」`resolveMode` 之后票里只有 SINGLE 或 MULTIPART。AUTO 只活在策略配置。

**反例 13：** 「init 之后改 yaml 存储键，续传会跟到新桶。」票里的 `service` 冻结。

**反例 14：** 「`handleUploadException` 是第六个业务 API。」它是异常映射，不是 `@RequestMapping` 资源。

**反例 15：** 「清理任务默认会把过期未完成上传扫掉。」`cleanup-enabled: false`。

**反例 16：** 「指纹比对的是 SHA-256。」resume 比的是客户端提交的原串；SHA-256 写在对象元数据和票的 `fingerprintDigest`，给 complete 的 HEAD 用。

**反例 17：** 「richtext-image 只需 `common:richtext:upload`。」还要跨过方法上的 `system:oss:upload`。

**反例 18：** 「`createOssUploadClient` 属于 OBJ-27。」OBJ-29 / L-029。本课只保证它打的五条 URL 在这份 Controller 上找得到。

**边界：**

- complete 的数据面校验读对象前缀最多 16 字节，这是控制面唯一一次读对象内容。不是把整文件拉进应用。
- SINGLE 的 init 预签名与 resume 预签名都可能过期；过期后要再 GET parts，不要重放旧 URL。
- `maxSignParts=20` 与对象 partCount 上限（策略校验避免 >10000）不是一回事。一次签名窗很小，整文件仍可很多片。
- `url=""` 的临时行不能当永久下载地址。短时下载在 L-026 的 `download-url`。
- `OssUploadCleanupTask` 与 `OssLifecycleManager` 临时对象过期是两条线：前者清**未 complete** 的票/对象；后者清**已登记但仍 isTemp** 的行。不要并成一个 cron。
- 异常若不是 `OssUploadException`，不会走本类 handler，会进全局异常处理。
- 本课不覆盖 CORS/Bucket Lifecycle 怎么配；yaml 注释要求生产先审 dry-run，那是运维门闩，不是第五个 HTTP 方法。

## 变式与迁移

- **变式 A：小图头像。** 策略 `avatar`，yaml 写死 `mode: SINGLE`，max 10MiB，类型 jpeg/png/webp。init 必带回 PUT。complete body 空 parts。不要走 signParts。

- **变式 B：超过阈值的视频。** 选 `editor-video` / `richtext-video` 这类 maxSize 能跨过 threshold 的策略。init 无 presignedRequest，有 partSize/partCount。循环 signParts（每窗 ≤20 个号）→ PUT → complete 带齐 ETag。

- **变式 C：刷新页面后续传。** 同一 token + 同一 fingerprint 打 GET parts。SINGLE 拿新 PUT；MULTIPART 看 `uploadedParts` 再签缺失片。指纹换了就是另一个文件，服务拒绝，不是「帮你接着传」。

- **变式 D：complete 时对象还没 PUT。** HEAD 空 → `COMPLETE_VALIDATION_FAILED`，随后删（可能本来就没有）并撕票。不要指望再 complete 一次奇迹出现。

- **变式 E：用户取消。** DELETE abort。未完成：与 complete 失败同样清桶。已完成：箱子留下，只是补偿工不再盯这张票。

- **变式 F：Redis 在盖章最后一步宕了。** 调用方看到 `STATE_STORE_FAILURE`。对象和 `sys_oss` 行在。重试 complete 走 findByObject。不要为了「失败就要 abort」把已经登记的对象删掉。

- **变式 G：策略指向的桶未 SERVING。** init 在 prepare 前失败，零票零对象。去 L-025 的就绪/配置，不在本课五扇窗里「重试 signParts」。

- **变式 H：换 Client 拿着旧 token。** 任何后续枪 `SESSION_OWNER_MISMATCH`。开新票。不要把 token 当成全局文件 id。

- **迁移口诀：** 先数五扇窗四格矩阵 → 分清控制面 JSON 与数据面 PUT → complete 才 insert 临时行 → 失败回滚在服务端 complete 里就已经 abort → HTTP abort 管取消和幂等空票 → 已完成 abort 不删对象。跳步会出现「以为文件经过 Spring」「把 GET parts 当成上传」「把临时 ossId 说成头像已换」「complete 失败还要再打一枪才算清理」。

## 常见误区

1. **「OBJ-27 包含 `createOssUploadClient.upload`。」** 那是 OBJ-29。本课认 Controller 五方法 + complete/abort 性状 + 两条 D。
2. **「`SysOssController` 的 upload 方法就是直传。」** 对象窗是 list/listByIds/downloadUrl/remove。直传另有 `SysOssUploadController`。
3. **「Java `parts` = 上传 part。」** 它是 resume 查询。
4. **「票里存着可用的签名 URL。」** 记录注释写明不包含可长期复用的签名 URL。
5. **「AUTO 策略大文件一定 MULTIPART。」** 还要看 maxSize 有没有够到 threshold。默认 `general` 不够。
6. **「complete 写业务表。」** 写 `sys_oss` 临时行。业务表是调用方。
7. **「abort 已完成 = 删除文件。」** 只摘补偿索引。
8. **「complete 失败对象还在，等前端 abort。」** `cleanupAfterFailure` 已经删。
9. **「五扇窗权限各不相同。」** 注解都是 `system:oss:upload`；策略权限是第二道。
10. **「`R.data` 和失败码混用。」** complete 成功才 `R.data`；失败走 handler 的 `R.fail(msg, ErrorResponse)`。
11. **「这是 layered UseCase。」** classic + `OssUploadService`。
12. **「定时清理默认开启。」** yaml `cleanup-enabled: false`。
13. **「指纹校验用元数据摘要做 resume。」** resume 用原串；complete HEAD 用摘要。
14. **「init 失败也会留下票。」** 就绪/权限/文件校验在 create 之前；create 失败会 cleanupPrepared。
15. **「`handleUploadException` 要算进矩阵 (a) 第六法。」** 不算。口试可以提它是错误码出口。
16. **「DELETE abort 已经符合 API-005。」** 规范偏好变更 POST+`@Log`；这是存量 DELETE 且无 `@Log`。

## 非评分暂停

打开磁盘，不要凭记忆默写路径。不要改文件。没有标准答案栏，没有分数。

1. 打开 `SysOssUploadController.java`。圈 `@RequestMapping("/resource/oss/uploads")`。数 `@PostMapping` / `@GetMapping` / `@DeleteMapping`，应能指到 init / signParts / parts / complete / abort。圈每扇上的 `system:oss:upload`。确认没有 `@Log`、没有 `BaseController`。
2. 对照 `OssUploadHttpContractUnitTest`：根路径、五条 path、complete 的 `R<String>`、`SignedPart` 五字段、`FINGERPRINT_MISMATCH` 的 data.error。
3. 在 `OssUploadService.init` 圈 `requireStorageRoute` 相对 `objectStore.prepare` 的前后。再圈 `ticketStore.create` 失败时的 `cleanupPrepared`。
4. 在 `completeLocked` 圈两处早退（已 COMPLETED、findByObject），圈 `cleanupAfterFailure` 的 catch，圈 `markCompleted` 在 try **外面**。
5. 在 `abort` 圈 `ticket==null` 的空成功，圈 `COMPLETED` 只 `removeCompletedCleanup`。
6. 打开 `application.yml` 的 `oss.direct-upload`：圈 `cleanup-enabled: false`；对照 `general` 的 max-size 与 multipart-threshold；对照 `richtext-image.required-permission`。

## 总结、词汇表与下一步

- **一块牌子五扇窗：** `/resource/oss/uploads` 上的 init / signParts / parts / complete / abort。矩阵 (a) 与磁盘一致。
- **控制面 vs 数据面：** JSON 票房在 Spring；字节 PUT 在预签名 URL。
- **(b) complete：** 校验通过 → 临时 `sys_oss` + ossId；校验失败 → 服务端 abort 清理，不留行。幂等：已有状态或已有行则返回旧 id。
- **(b) abort：** 无票成功；未完成清桶撕票；已完成只摘补偿索引。
- **D 主路径：** init 票 → 浏览器 PUT → complete 临时引用。业务表挂 id 不在五方法里。
- **D 失败路径：** complete 失败或 DELETE abort → 无业务引用。不要把「前端必须补 abort」说成唯一实现。
- **两把锁：** HTTP `system:oss:upload` + 策略 `requiredPermission`；票还锁 userId+clientPk。
- **默认定时清理关闭。** 不要把 cron 说成本课第五扇半窗。

词汇表：`SysOssUploadController` / `OssUploadService` / `uploadToken` / `OssUploadTicket` / `OssUploadMode` / `OssUploadState` / `presignedRequest` / `signParts` / `parts`/`resume` / `complete` / `abort` / `OssUploadError` / `registerTemporary` / `isTemp` / `cleanupAfterFailure` / `oss:upload:ticket:` / control plane / data plane。

下一步：L-026 才是对象列表/下载/删除与对账；L-028 才是桶间迁移；L-029 才把 `createOssUploadClient` 的窗口、IndexedDB、取消时 `abortUpload` 对上这五条 URL。L-007 已认厅堂把整份 `resources.oss` 注入网关。本课结束仍不是掌握证明；要练习请之后主动激活 Homework。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-004 | `backend/wta-modules/**` Controller | 直传入口在 `wta-system` 的 `SysOssUploadController`，不是 admin 里的 Auth | `controller/system/SysOssUploadController.java` | 2026-09-16 |
| S-008 | `.agents/skills/engineering-standards/references/project/03-backend-module-modes.md` | `wta-system` = classic；不按 layered UseCase 口述 | 登记表 classic 行 | 2026-09-16 |
| S-L027-01 | `SysOssUploadController.java` | 五映射 + `handleUploadException`；权限 `system:oss:upload`；complete 用 `R.data`；abort 为 DELETE；无 `@Log`、无 `BaseController` | 类与五个方法 | 2026-09-16 |
| S-L027-02 | `OssUploadContracts.java`；`OssUploadHttpContractUnitTest.java` | Init/Sign/Resume/Complete JSON 形状；根路径与五条 path；`SignedPart` 五字段；错误码可机器区分；complete 返回 ossId 字符串 | 记录类型；合同测试 | 2026-09-16 |
| S-L027-03 | `OssUploadService.java`；`OssUploadServiceUnitTest.java` | init 就绪门闩；SINGLE/MULTIPART；resume 指纹与续签；complete 幂等/魔数回滚/COMPLETED 写入失败恢复；票冻结存储路由 | `init`/`signParts`/`resume`/`completeLocked`/`abort`/`cleanupAfterFailure` | 2026-09-16 |
| S-L027-04 | `RedisOssUploadTicketStore.java`；`OssUploadTicket.java`；`OssUploadCleanupRecord.java` | 三键一锁一索引；票不含长期 URL；create 失败回滚；abort/complete 摘键语义 | `TICKET_PREFIX` 等常量 | 2026-09-16 |
| S-L027-05 | `DefaultOssUploadObjectStore.java`；`DefaultOssUploadMetadataStore.java`；`SysOss.java`；`SysOssExt.java` | prepare/sign/head/completeMultipart/abort/delete；临时行 `isTemp`、`source=directUpload`、`url=""` | `registerTemporary`；`PreparedUpload` | 2026-09-16 |
| S-L027-06 | `OssUploadProperties.java`；`backend/wta-admin/src/main/resources/application.yml` | ticket/presign/cleanup TTL；maxSignParts=20；cleanup 默认关；策略 general/avatar/richtext-* 与双权限 | `oss.direct-upload` 段 | 2026-09-16 |
| S-L027-07 | `DefaultOssUploadIdentityResolver.java`；`OssUploadError.java`；`OssUploadState.java`；`OssUploadMode.java`；`OssUploadCleanupTask.java` | 登录身份；错误枚举；状态枚举；AUTO 只在策略；定时清理需显式打开 | 各类型与 `@Scheduled` | 2026-09-16 |
| S-L027-08 | `frontend/packages/domains/system/src/resource-service.ts`；`oss-upload/index.ts`（仅边界） | 厨房五枪 URL 与本课对齐；`resumeUpload` 对 GET parts；adapter 取消才 abortUpload（L-029 格子） | `createOssService` 的 oss 段；`systemOssUploadResource.basePath` | 2026-09-16 |
