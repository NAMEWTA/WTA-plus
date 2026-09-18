---
lesson_id: L-029
objective_ids: [OBJ-29]
claimed_cells:
  - A:createOssUploadClient.upload
  - B:createOssUploadClient.upload
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: three-layers-ticket
    minutes: 10
  - segment: progress-abort-resume
    minutes: 11
  - segment: visuals-and-worked-examples
    minutes: 7
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 3
expression_level: eli5
coverage_depth: deep
source_ids: [S-005, S-006, S-L029-01, S-L029-02, S-L029-03, S-L029-04, S-L029-05, S-L029-06, S-L029-07, S-L029-08]
---

# Lesson 029：拿票自己送货——浏览器直传 MinIO 的宏观三层

## 学完你能做什么

打开三份磁盘，你能**口述浏览器怎么拿 ticket、自己把字节 PUT 进 MinIO、进度怎么报、取消怎么撕票**。口试名单就是矩阵两格，外加 OBJ-29 点名的两扇门面；方法名以**磁盘**为准：

1. **`A:createOssUploadClient.upload`**（包 `@namewta/adapter-oss-upload-browser`，`src/client.ts`）：工厂返回的 `UploadClient.upload`。它算指纹、查 IndexedDB 续传、调网关 `initUpload` / `resumeUpload` / `signParts` / `completeUpload`，再把文件字节交给 `transferToOss`。**不**自己拼 `/resource/oss/uploads` 字符串。
2. **`B:createOssUploadClient.upload`**（矩阵原文：浏览器直传；progress；abort）：同一函数的性状。进度来自 `xhr.upload.onprogress`；取消来自必选 `AbortSignal`。信号已中止且手里有 `uploadToken` 时，才 `abortUpload` 并撕本地续传条。complete 校验失败**不会**由本函数再打一枪 DELETE——那是 L-027 服务端 `cleanupAfterFailure`。

OBJ-29 还要你能顺着两扇门面把票送到 `upload`：

- **`useDirectOssUpload`**：admin-web 的兼容门面，**不是** Vue composable。真正干活的是 `uploadDirectToOss` → 厅堂单例 `ossUploadClient.upload`。`directOssUploadRequest` 只是给 `el-upload` 的假 XHR，`abort()` 转到 `AbortController`。
- **`FileUpload`**：web-kit 壳 `@namewta/web-kit-file-upload`。它不拥有票据。App 包装器 `apps/admin-web/src/components/FileUpload/index.vue` 把 `ossUploadClient` 塞进 `client`。默认策略是 **`document`**，不是 adapter 缺省的 `general`。

`createOssUploadClient` 还实现 `resolve` / `remove`。本课认它们是 `UploadClient` 合同上的邻居，口试格子仍是 **`upload` 的直传、进度、取消**。列表/下载/删除的 HTTP 形状留给 L-026。

本课**不宣称**你会拆 `SysOssUploadController` 五扇窗（L-027）、对象卡片柜（L-026）、配置切换（L-025）、搬家批次（L-028），或把厅堂 `services.ts` 十二个工厂再讲一遍（L-007）。今天只认：**浏览器这一头怎么拿票、怎么送货、进度从哪来、取消撕哪几张纸。**

## 先把宏观地图放在桌上

L-007 已经把插头插上：`ossUploadClient = createOssUploadClient({ clientId, gateway: systemService.resources.oss, getToken: session.getToken, transfer: 开发代理? })`。L-010 把 adapter 和 web-kit 分成两间作坊，并把分片/续传/abort 留给本课。L-027 是票房：init / signParts / parts / complete / abort。本课站在**旅客自己扛箱子**这一头。

2026-09-16 工作树里，直传不是「页面 → Controller → MultipartFile」。字节**不进** Spring。宏观三层是：

```text
页面 / 控件
  FileUpload.vue（web-kit，默认 policy=document）
  ImageUpload.vue（默认 policy=image）
  useDirectOssUpload.directOssUploadRequest（默认 policy=general）
  Editor / userAvatar / richTextAssets（直接 ossUploadClient.upload）
        │  每条路都造 AbortController，把 onProgress 转给 el-upload 或自己吞掉
        v
厅堂单例  apps/admin-web/src/application/services.ts
  ossUploadClient = createOssUploadClient(...)
        │  UploadClient.upload(file, { policy, signal, onProgress })
        v
适配器    packages/adapters/oss-upload-browser/src/client.ts
  指纹 → IndexedDB 续传? → gateway.initUpload / resumeUpload
        │
        ├─ 控制面 JSON（经 domain-system createOssService → adminHttp）
        │     POST /resource/oss/uploads
        │     GET  /resource/oss/uploads/{token}/parts?fingerprint=
        │     POST /resource/oss/uploads/{token}/parts/sign
        │     POST /resource/oss/uploads/{token}/complete
        │     DELETE /resource/oss/uploads/{token}
        │
        └─ 数据面字节  transferToOss = XMLHttpRequest
              PUT 预签名 url + requiredHeaders
              ──► MinIO / OSS（生产直连；DEV 可改写到 VITE_APP_OSS_PROXY_PREFIX）
```

| 符号 | 磁盘 | 拥有什么 | 不拥有什么 |
| --- | --- | --- | --- |
| `createOssUploadClient` | `frontend/packages/adapters/oss-upload-browser/src/client.ts` | 指纹、续传、SINGLE/MULTIPART、progress、abort | HTTP 路径字符串、Vue、Element Plus、`Admin-Token` 抽屉 |
| `OssUploadGateway` | 同包 `types.ts`；实现是 `createOssService` | 八口：init/resume/sign/complete/abort + downloadUrl/listByIds/delete | 文件字节 |
| `transferToOss` | `src/transport.ts` | 一只真 XHR 的 PUT 与 `upload.onprogress` | 票据、策略、指纹 |
| `useDirectOssUpload` | `apps/admin-web/src/hooks/oss/useDirectOssUpload.ts` | 把 `{id,name,url}` 改写成 `{ossId,fileName,url}`；假 XHR | 自己的上传状态机 |
| `FileUpload` | `packages/web-kit/file-upload/src/FileUpload.vue` | 选文件、本地校验、v-model 序列化 ossId | 开票、PUT、IndexedDB |
| App 包装 | `apps/admin-web/src/components/FileUpload/index.vue` | 注入 `client: ossUploadClient` 和 feedback | 策略默认值（仍走 web-kit 的 `document`） |

**图题 / caption：** 浏览器直传宏观三层。alt：控件和门面汇入厅堂单例；adapter 分控制面 JSON 与数据面 PUT；MinIO 不经过 Spring。

**文字等价物：** 厅堂只造一个 `ossUploadClient`。谁要传文件，最终都进 `upload`。`upload` 向 system 资源口要短时票和预签名 URL，再让浏览器 XHR 把文件打到对象存储。Spring 那头只看见 JSON。home-web **没有** `createOssUploadClient`。

**类比：** 火车站不帮你扛箱子。售票窗（L-027）只给一张寄存条（`uploadToken`）和一把几分钟就失效的仓库钥匙（预签名 PUT）。你自己把箱子推进仓库（`transferToOss`）。车上的里程表是进度。半路不想存了，把条子还回去（`abortUpload`）。冰箱上的便利贴（IndexedDB）写着「上次那张条子还在」，刷新页面先去问窗口这张条还作不作数。

**类比失效处：**

1. 条子在 Redis，TTL 默认 24 小时；钥匙 TTL 默认 5 分钟。便利贴上**没有**钥匙，只有 token 和过期时间。
2. 进度不是 GPS，是 `xhr.upload.onprogress`，而且只在 `lengthComputable` 时才回调。
3. 「取消」只认 `AbortSignal`。complete 时仓库拒收假封条，窗口自己把格子清掉，旅客**不必**再还条子。
4. 便利贴的主键不是文件名，是 `SHA-256(clientId:token:policy:fingerprint)`。
5. FileUpload 柜台会先用尺子量箱子（扩展名、逗号、MB），量不过根本不会去售票窗。

## 核心概念与机制

### 直觉讲解

先分清三张嘴，别把它们说成同一个函数：

- **厨房网关** `systemService.resources.oss` 会说人话 `initUpload`。这是 L-019 抽屉里的整对象，不是裁成四口的小插头。厅堂把**整份** `resources.oss` 塞进 adapter。
- **适配器** `createOssUploadClient(...).upload` 会说旅客话：拿文件、拿策略、拿取消信号、拿进度回调，还你 `{ id, name, url }`。
- **门面** `uploadDirectToOss` 还你 `{ ossId, fileName, url }`，只为旧调用方。头像页真正上传走 `ossUploadClient.upload`，只借用 `getDirectOssUploadErrorMessage`。

再分清两辆车：

- **假 XHR**：`createUploadRequest` / `directOssUploadRequest` `new XMLHttpRequest()` 之后立刻改写 `abort`，**从不** `send`。它只是 Element Plus `http-request` 要的那根「取消手柄」。
- **真 XHR**：`transferToOss` 里那只。`xhr.open(request.method, request.url)`，把头设成 `requiredHeaders`，`xhr.send(body)`。生产上 `url` 是 MinIO 预签名地址，**不带** `Authorization: Bearer`。登录票只活在控制面那几枪 JSON 里。

### 精确定义与 English term

| 中文说法 | English term | 磁盘定义 |
| --- | --- | --- |
| 浏览器直传 | direct / browser-side upload | 文件字节由浏览器 PUT 到对象存储；本课 adapter 是旅客，不是票房 |
| 上传客户端 | `UploadClient` | `platform-contracts`：`upload` / `resolve` / `remove`。`signal` **必选** |
| 上传票 | upload ticket / `uploadToken` | 控制面 Redis 会话的钥匙。adapter 把它写进 IndexedDB 续传条 |
| 预签名请求 | `OssPresignedRequest` | `method` + `url` + `requiredHeaders` + `expiresAt`。必须原样带请求头 |
| 传输函数 | `OssTransfer` / `transferToOss` | `(request, body, signal, onProgress?) => ETag?`。默认实现是 XHR |
| 指纹 | fingerprint | 默认 `v1:` + SHA-256(文件名、大小、lastModified、头 1MiB 摘要、尾 1MiB 摘要)。**不是**整文件哈希 |
| 续传抽屉 | resume store / IndexedDB | 库名 `wta-oss-upload`，store `sessions`，keyPath `fingerprint`（实际存的是身份哈希） |
| 分片窗口 | sign window | 前端常量 `SIGN_WINDOW = 8`。后端一次最多签 20（L-027 `maxSignParts`）。口试前端说 8 |
| 进度 | progress | `onProgress(percent: number)`，百分数。MULTIPART 用 `Math.max` 防止回退 |
| 取消 | abort | `AbortSignal` → 真 XHR `abort()` → `DOMException('AbortError')` → 若已有 token 则 `gateway.abortUpload` |
| 策略 | policy | 字符串。adapter 缺省 `general`；FileUpload 缺省 `document`；ImageUpload 缺省 `image` |
| 开发代理 | OSS proxy prefix | 仅 `import.meta.env.DEV` 且配置了 `VITE_APP_OSS_PROXY_PREFIX` 时，把预签名 URL 改写到当前 Origin |

错误对象：适配器自己的 `OssUploadError`，`code` 是 `'aborted' | 'contract' | 'network' | 'server' | 'unknown'`。取消文案是「上传已取消」。网关 HTTP 失败走 axios 那条河，`message` 里可能出现 L-027 的枚举名或中文「上传会话不存在」——`findResume` 靠这两份名单判断「这张旧票作废，另开一张」。

### 机制/因果链

#### 0. 工厂：还没选文件就已经能炸掉

`createOssUploadClient` 第一行 `requireClientId`。`VITE_APP_CLIENT_ID` 空字符串时抛 `OSS upload clientId is required`，发生在 `services.ts` 模块求值，**早于** `createIdentityAccessService`。这是接线失败，不是上传失败。L-007 已钉；口试本课不要说成「登录成功但传不了文件」。

缺省依赖：

- `fingerprint = createOssFileFingerprint`
- `resumeStore = createIndexedDbResumeStore()`
- `transfer = transferToOss`

厅堂可覆盖 `transfer`：开发代理把 `signedUrl.pathname + search` 接到 `/${prefix}/...`，再调用同一个 `transferToOss`。生产不走这根线。

网关八口对照（token 会 `encodeURIComponent`，测试用 `upload/token` → `upload%2Ftoken`）：

| Gateway | HTTP |
| --- | --- |
| `initUpload` | `POST /resource/oss/uploads` |
| `resumeUpload(token, fingerprint)` | `GET .../{token}/parts?fingerprint=` |
| `signParts(token, partNumbers)` | `POST .../{token}/parts/sign` body `{ partNumbers }` |
| `completeUpload(token, parts)` | `POST .../{token}/complete` body `{ parts }` |
| `abortUpload(token)` | `DELETE .../{token}` |
| `downloadUrl` / `listByIds` / `delete` | L-026 卡片柜；`upload` 成功后用 downloadUrl 填 `UploadResult.url` |

`initUpload` / `signParts` / `resumeUpload` 会 `validatePresigned`：URL 必须是 http(s)、不许带用户名密码。`javascript:` 直接 `ResourceSecurityError`。adapter 信任过关后的 `url`。

#### 1. `upload` 主路径——格子 (a)

```text
fingerprint(file)
  → policy = options.policy || 'general'
  → storageKey = SHA-256(clientId : getToken()||'anonymous' : policy : fingerprint)
  → session = findResume(...) || initialize(...)
  → 若 resume.state === 'COMPLETED'：撕便利贴，downloadUrl，return
  → SINGLE → uploadSingle；否则 uploadMultipart
  → completeUpload(token, parts)
  → 要到非空 ossId
  → 撕便利贴
  → downloadUrl；失败则 URL.createObjectURL(file)
```

**指纹。** 头尾各切最多 1MiB 做 SHA-256，再和名字、大小、`lastModified` 拼起来做一次 SHA-256，前缀 `v1:`。改一个字节但不在头尾 1MiB、同时名字大小修改时间都不变，前端会当成同一文件。这是近似指纹，不是内容寻址。

**续传查找 `findResume`。** IndexedDB `get` 抛错 → 当作没有续传（当前页仍可传）。记录过期 → `remove` 后当作没有。有记录就 `resumeUpload(token, fingerprint)`。返回的 `fileName`/`fileSize` 对不上当前 `File` → 撕条，当作没有。网关报 `SESSION_NOT_FOUND` / `SESSION_EXPIRED` / `INVALID_STATE` / `FINGERPRINT_MISMATCH` / `SESSION_OWNER_MISMATCH`，或中文「上传会话不存在」等五句 → 撕条，当作没有，**再走 initialize**。其它错误原样抛出，**不会**偷偷新开票。

**开票 `initialize`。** `initUpload({ policy, fileName, fileSize, contentType, fingerprint })`。`contentType` 空则 `'application/octet-stream'`。`response.data` 空 → `OssUploadError('初始化 OSS 上传失败', 'contract')`。然后尽力 `store.put`：字段名叫 `fingerprint`，值却是 **storageKey**，外加 token、expiresAt、fileName、fileSize、contentType。IndexedDB 写失败吞掉，当前页继续。

**SINGLE。** 必须有 `presignedRequest`。一次 `transfer(request, file, signal, …)`。进度：`loaded / file.size * 100`。返回空 `parts[]`。complete 仍会 POST `{ parts: [] }`。

**MULTIPART。** 要 `partSize`、`partCount`。resume 带来的 `uploadedParts` 填进已完成表，进度从已上传字节起跳。缺的 part 按 1..N 排队，每次切 `SIGN_WINDOW=8` 个号去 `signParts`。同一窗 `Promise.allSettled` 并行 PUT；有失败先把成功的记进 map，再把失败原因抛出。单片最多 3 次：失败且不是 abort，就**只给这个 partNumber 重新签名**再 PUT。成功必须拿到 ETag，否则 `OSS 响应未暴露 ETag`。最后按 partNumber 手工插入排序（注释写明避开 `toSorted`，兼容 Chrome 87），交给 complete。

**完成。** `complete.data` 必须是非空字符串或数字。`null` → 「完成 OSS 上传失败」，**不**打 downloadUrl。成功后才 `resolveUploadResult`：`downloadUrl` 失败就用本地 blob URL。那只 blob URL **不会**写进 `sys_oss.url`（L-026 的目录行 `url` 仍是空串）。

测试钉死的两件正事：分片第 1 片第一次 PUT 失败会再 `signParts('upload-1', [1])`；已完成 resume **零次** `transfer`、**零次** `completeUpload`。

#### 2. 进度与取消——格子 (b)

**进度从哪来。** 只有 `transferToOss` 听 `xhr.upload.onprogress`。`event.lengthComputable` 为假则不回调。adapter 把 `{loaded,total}` 收成百分数：SINGLE 用文件大小做分母；MULTIPART 把每个 part 当前 `loaded` 加起来，再 `stableProgress` 取历史最大值，避免并行分片回调把条子往回拨。

FileUpload / `directOssUploadRequest` 再把百分数包装成 Element Plus 的 `UploadProgressEvent`：`ProgressEvent` 的 `total=100`、`loaded=percent`，并写 `event.percent`。这是给 `el-upload` 的合同，不是第二只真 XHR。

FileUpload 默认 `:show-file-list="false"`，列表自己画、**没有**进度条。加载态是 feedback `'正在上传文件，请稍候...'`。所以口试「有 progress」指的是 **`upload` 回调和假事件还在传**，不是页面上一定有百分数。

**取消怎么走。**

1. 控件返回的假 XHR 被 Element Plus 调 `abort()`，或 Editor `onBeforeUnmount` 对 `activeUploads` 里每只 `AbortController.abort()`。
2. `signal` 触发 `transferToOss` 的 listener → `xhr.abort()`。
3. `xhr.onabort` 拒绝一个 `DOMException('上传已取消', 'AbortError')`。若 `send` 前信号已经 aborted，同样走这条。
4. `upload` 的 `catch`：`uploadOptions.signal.aborted && uploadToken` 才 `gateway.abortUpload(token).catch(() => undefined)`，并 `safeRemoveResume`。abort HTTP 失败被吞掉，避免掩盖「上传已取消」。
5. 若错误是 `AbortError`，改抛 `OssUploadError('上传已取消', 'aborted')`。

**取消边界（口试容易栽）。**

- `uploadToken` 在 `findResume || initialize` **返回之后**才赋值。开票 HTTP 还在飞时用户取消：本地抛错，**不一定**打 DELETE。票可能留在 Redis 直到 TTL。不要说「任何取消都会 abortUpload」。
- 非取消失败（签名过期、网络、缺 ETag、complete 合同失败）**不会**走 `abortUpload`。MULTIPART 半成功的片留在 Provider 上，下一次 resume 靠服务端 `listParts`。IndexedDB 条还在。
- complete 魔数/大小失败：服务端自己删对象撕票（L-027）。前端 catch 时 `signal.aborted` 为假，不打 DELETE。
- `abortUpload` 已 COMPLETED 的票：服务端只摘补偿索引，不删对象。本课 catch 主要发生在未完成阶段；不要把「取消」说成「已盖章的文件会从桶里消失」。
- 头像页 `uploadImg` 每次 `new AbortController()`，**没有**把它交给可点的取消按钮。裁剪提交途中关对话框，信号不会 abort。

`client.test.ts` 四例覆盖：分片重签、已完成 resume、complete 无 id、过期会话改走 init。**没有** abort / progress 用例。`transport.ts` / `fingerprint.ts` / `resume-store.ts` 没有自己的测试文件。格子 (b) 的证据是实现，不是前端单测。口试按源码，不要假装有 abort 单测。

#### 3. `useDirectOssUpload`——名字像 hook，磁盘是门面

文件在 `apps/admin-web/src/hooks/oss/useDirectOssUpload.ts`。导出四个符号，**没有** `useDirectOssUpload()` 函数，**没有** `ref` / `onUnmounted`。

| 符号 | 做什么 |
| --- | --- |
| `uploadDirectToOss(file, options)` | `ossUploadClient.upload` 然后映射 `{ ossId: result.id, fileName: result.name, url: result.url }` |
| `getDirectOssUploadErrorMessage(error, fallback)` | `error.isHandled` → `undefined`（axios 已经弹过）；`Error.message` 非空用它；否则 fallback |
| `directOssUploadRequest(options, policy='general')` | 假 XHR + `uploadDirectToOss`；`onSuccess` 收到的是 **ossId 形状**，不是 `UploadResult` |
| `createDirectOssUploadRequest(policy)` | 把 policy 冻进 `http-request` |

单测只钉两件事：映射形状；`isHandled: true` 时不返回文案。它**不**测 PUT、不测 abort。

头像页：`requestUpload` 是空函数（裁剪器自己读本地图）。提交时 `ossUploadClient.upload(..., policy: 'avatar')`，再用 `systemService.users.updateProfile({ avatar: result.id })`。业务引用这一跳不是本课格子，但要能指出来：complete 只给临时 ossId，头像表另写。

#### 4. `FileUpload`——店徽壳，不卖票

web-kit `FileUpload.vue`：

- `el-upload` `action="#"` + `:http-request="uploadRequest"`。`createUploadRequest(props.client, props.policy, pendingΔ)`。
- 默认 `policy: 'document'`、`limit: 5`、`fileSize: 5`、类型 `doc/docx/xls/xlsx/ppt/pptx/txt/pdf`。
- `before-upload`：扩展名必须在名单里；**文件名不能有英文逗号**（v-model 用逗号拼 id）；`size/1024/1024 >= fileSize` 就拒，等于上限也拒。
- 成功：要 `result.id`；追加列表；`serializeUploadItems` 成字符串 `emit('update:modelValue')`。
- 删除：`client.remove(id)`。回显：`client.resolve(ids)` ← 网关 `listByIds`，每个 id 再 downloadUrl。
- `ossExt` 出现在 props 类型上，组件**不用**它。
- `pending` 计数：第一个文件 `loading`，最后一个 `finally` 才 `closeLoading`。

App 包装只用 `h(SharedFileUpload, { ...attrs, client: ossUploadClient, feedback })`。workflow 对话框 `<FileUpload v-model="fileId" :file-size="20" />` **不传 client**——它用的是 runtime 里那只厅堂包装。home-web 没有这只包装，也没有直传客户端。

`ImageUpload` 同壳不同皮：默认 `policy: 'image'`，可压缩。压缩函数由 admin-web 包装注入 `image-conversion`。

### 图、表或文本图

**图题 / caption：** `upload` 主路径与 (b) 取消岔路。alt：指纹与续传之后 SINGLE 或 8 片窗口 PUT；成功 complete；信号中止才 abortUpload。

```text
[upload(file, {policy, signal, onProgress})]
        │
        ├─ 指纹 v1:sha
        ├─ storageKey = sha(clientId:token:policy:fingerprint)
        │
        ├─ IndexedDB get ──resumeUpload──► 票还活着？
        │         │ 否 / 过期 / 名单内陈旧码
        │         v
        └─ initUpload ──► 票 + (SINGLE 的 PUT 钥匙)
                  │
     state=COMPLETED? ──是──► downloadUrl，结束（零 PUT）
                  │否
        ┌─────────┴─────────┐
        │ SINGLE            │ MULTIPART
        │ PUT 整文件        │ 每次 sign 最多 8 号
        │ onProgress l/n    │ 每号最多 3 次重签
        └─────────┬─────────┘
                  │
         signal.abort? ──是──► DELETE abort + 撕 IDB
                  │否
                  v
         POST complete {parts}
                  │
         ossId → downloadUrl → {id,name,url}
```

**文字等价物：** 先问自己有没有旧条子。没有或条子作废，再去窗口开票。SINGLE 一把钥匙一次 PUT。MULTIPART 窗口是 8 不是 20。百分数来自真 XHR。只有用户取消且已经拿到 token，前端才 DELETE。盖章成功后便利贴必须撕掉，避免下次把已完成票当成进行中。

**图题 / caption：** 假 XHR 与真 XHR。alt：el-upload 拿到的 XMLHttpRequest 只提供 abort；真正 PUT 在 transferToOss。

```text
el-upload http-request
    │
    ├─ handle = new XMLHttpRequest()     ← 假；从不 send
    │    handle.abort = () => controller.abort()
    │
    └─ client.upload(..., signal: controller.signal)
             │
             └─ transferToOss
                    xhr.open(PUT, presigned)
                    xhr.send(blob)          ← 真；打 MinIO
                    xhr.upload.onprogress
```

**文字等价物：** 你在控制台里看到「上传用了 XMLHttpRequest」时，要问是哪一只。取消手柄是假的那只；对象存储那只才带预签名 URL。假的那只没有 `onprogress`。

### 正例、反例与边界

**正例 1：** 厅堂 `services.ts` 37–42 行：`clientId: import.meta.env.VITE_APP_CLIENT_ID`，`gateway: systemService.resources.oss`，`getToken: session.getToken`，`transfer: createDevelopmentOssTransfer()`。生产 `DEV` 为假时 `transfer` 是 `undefined`，adapter 用默认 `transferToOss`。

**正例 2：** `client.test.ts` 分片：`partSize=3`、`partCount=2`，第一枪 `signParts` 给 [1,2]，第 1 片第一次 transfer 失败，第二次 `signParts('upload-1', [1])`，complete 收到按编号排好的 `{partNumber:1,eTag:'etag-1'}, {partNumber:2,eTag:'etag-2'}`。

**正例 3：** 已完成 resume：`transfer` 零次、`completeUpload` 零次，结果 id=`9002`。

**正例 4：** 陈旧「上传会话不存在」：撕条，`initUpload` 一次，新 token `fresh-upload`。

**正例 5：** FileUpload 包装把 `ossUploadClient` 写死注入。workflow `ProcessActionDialog` 不 import adapter，只画 `<FileUpload>`。

**正例 6：** Editor `policy: 'editor-image' | 'editor-video'`，卸载时 `activeUploads.forEach(abort)`，这是 (b) 在厅堂私货组件上的样子。

**正例 7：** `uploadDirectToOss` 把 `id: '9001'` 映射成 `ossId: '9001'`。旧调用方不认识 `UploadResult`。

**正例 8：** 网关测试：`resumeUpload('upload/token', 'fingerprint')` 的 URL 是 `/resource/oss/uploads/upload%2Ftoken/parts`。

**反例 1：** 「字节经过 `SysOssUploadController`。」Controller 没有文件参数。PUT 目标是预签名 URL。

**反例 2：** 「`FileUpload` 自己 init。」壳只调 `props.client.upload`。没有 client 的 web-kit 实例传不了。

**反例 3：** 「`useDirectOssUpload` 是 `const { upload } = useDirectOssUpload()`。」磁盘没有这个函数。

**反例 4：** 「FileUpload 默认策略 `general`。」默认 `document`。`general` 是 adapter 和 `directOssUploadRequest` 的缺省。

**反例 5：** 「前端一次 signParts 20 个号。」常量是 `8`。20 是后端上限。

**反例 6：** 「进度用 axios `onUploadProgress`。」控制面走 axios；数据面走 XHR。axios 看不见 PUT 百分比。

**反例 7：** 「complete 失败前端必须 abort。」L-027 服务端已经清理。本课 catch 不因校验失败打 DELETE。

**反例 8：** 「取消一定 `abortUpload`。」没有 token（指纹/开票中）不会。abort HTTP 还被 `.catch` 吞掉。

**反例 9：** 「IndexedDB 坏了就不能传。」`get`/`put`/`remove` 都尽量吞错，当前页继续。

**反例 10：** 「续传键是文件指纹原串。」键是身份哈希；记录字段名叫 `fingerprint` 但存 storageKey。

**反例 11：** 「`parts` 网关方法用来 PUT 分片。」前端方法名 `resumeUpload`；PUT 走 `signParts` 给的 URL。

**反例 12：** 「预签名 PUT 带着 `Admin-Token`。」`requiredHeaders` 是开票时冻结的 Content-Type 和元数据；登录票只在 JSON 枪。

**反例 13：** 「home-web 也能直传。」工作树 home-web 没有 `createOssUploadClient`。

**反例 14：** 「`resolve` 的 url 就是 `sys_oss.url`。」listByIds 会再要 download-url；目录列本身是空串。

**反例 15：** 「adapter 依赖 Vue。」`package.json` 只有 `@namewta/platform-contracts`。

**反例 16：** 「有 abort 单测所以 (b) 已由测试闭合。」没有。实现在 `client.ts` catch 与 `transport.ts` `onabort`。

**边界：**

- CORS：`xhr.onerror` 文案点名 Origin、PUT、签名请求头。MinIO 还要 **暴露 ETag**，否则 MULTIPART 合同失败。开发代理把 PUT 变同源，这条 CORS 在 DEV+prefix 时可以绕开。
- `getToken()` 进续传身份。换用户或登出再传同一文件，storageKey 变，不会误续别人的票。服务端还有 `SESSION_OWNER_MISMATCH`。
- 策略权限是后端的事（`system:oss:upload` + 策略 `requiredPermission`）。前端 FileUpload 的 MB/扩展名只是柜台尺子，不是授权。
- `createObjectURL` 失败回退只服务当前页预览，刷新即失效。
- 并发多文件：假 XHR 每份一只 AbortController；`pending` 计数共用一个 loading 文案。
- `watch(modelValue)` 用 `generation` 丢弃过期的 `resolve`，避免慢网关把新列表打回旧值。

## 变式与迁移

- **变式 A：流程附件。** 厅堂包装 FileUpload，不传 policy → `document`。本地 20MB 尺子可以比策略 maxSize 更紧或更松；最终仍以后端 `INVALID_FILE` 为准。v-model 是逗号分隔 ossId。

- **变式 B：头像。** 不走 FileUpload。`policy: 'avatar'`，yaml 写死 SINGLE。complete 后还要 `updateProfile`。错误用 `getDirectOssUploadErrorMessage`，避免和 axios 已提示的 `isHandled` 叠两层红字。

- **变式 C：编辑器视频。** `editor-video` 才可能跨过多段阈值。看 `signParts` 是否出现。卸载页必须 abort 进行中的 controller，否则会把文件传到一半还 complete。

- **变式 D：刷新后续传。** 同一登录、同一 policy、同一文件（指纹命中）→ IndexedDB 有条 → `resumeUpload`。SINGLE 会拿到**新** PUT 钥匙。MULTIPART 跳过 `uploadedParts`。换策略等于换 storageKey，不会续到另一条业务线上。

- **变式 E：用户在列表里点掉正在传的文件。** Element Plus 调假 XHR `abort` → 真 PUT 停 → 有 token 则 DELETE。不要去 `SysOssController.remove` 删一个还不存在的 ossId。

- **变式 F：IndexedDB 被浏览器禁用。** 开票仍成功，只是刷新不能续。不要把「无痕模式传失败」默认诊断成 MinIO 挂了。

- **变式 G：开发环境 CORS 红了。** 先看有没有 `VITE_APP_OSS_PROXY_PREFIX`。有则 PUT 应打当前 Origin。没有则必须配 MinIO CORS，而不是把文件改成打 `/resource/oss/uploads`。

- **变式 H：产品要在 FileUpload 上画百分数。** 合同已有 `onProgress`。改壳即可，不要新开一条 axios 上传。

- **迁移口诀：** 先数三层（壳 / 厅堂单例 / adapter）→ 再数两辆 XHR（假 abort、真 PUT）→ 再数五枪网关与 L-027 对齐 → 进度只出真 XHR → 取消只在有 token 时撕票 → FileUpload 默认 document、门面默认 general。跳步会出现「以为文件进了 Spring」「把 20 说成前端窗口」「把 complete 失败再 abort 说成前端职责」。

## 常见误区

1. **「OBJ-29 的格子是 FileUpload.vue。」** 矩阵 (a)(b) 都是 `createOssUploadClient.upload`。壳和门面是口述走查，不是另两行符号。
2. **「`useDirectOssUpload` 自己实现直传。」** 一行委托 `ossUploadClient.upload`。
3. **「web-kit 拥有 OSS 票据。」** L-010：web-kit 不拥有网关。没有注入的 `client` 它只是一只空柜台。
4. **「厅堂只注入了 init/sign/complete/abort 四口。」** 注入整份 `resources.oss`，含 resume/listByIds/downloadUrl/delete。
5. **「SIGN_WINDOW=20。」** 8。
6. **「AUTO 大文件一定走分片。」** 策略在服务端解析。默认 `general` 的 maxSize 够不到阈值（L-027）。前端只听 `session.mode`。
7. **「指纹是整个文件的 SHA。」** 头尾 1MiB。
8. **「PUT 要带登录头。」** 不要。
9. **「取消 = `client.remove(ossId)`。」** remove 删的是已经编目的对象。取消走 `abortUpload(token)`。
10. **「progress 在 FileUpload 列表上可见。」** 默认不展示 el-upload 文件列表。
11. **「adapter 会写业务表。」** 只还 ossId。头像/富文本/流程自己挂。
12. **「home-web 对称有一份 ossUploadClient。」** 没有。
13. **「`directOssUploadRequest` 的 onSuccess 拿到 `{id,name,url}`。」** 拿到 `{ossId,fileName,url}`。web-kit 那条才是 `UploadResult`。
14. **「IndexedDB 库名是文件名。」** `wta-oss-upload` / `sessions`。
15. **「本课覆盖 MinIO CORS 怎么配成生产。」** 只认失败文案和开发代理。配桶是运维，不是 `upload` 的返回值。
16. **「L-027 的 (b) 和本课 (b) 是同一格。」** 那边是 complete/abort **服务端**性状；这边是浏览器 progress/abort。

## 非评分暂停

打开磁盘，不要凭记忆默写路径。不要改文件。没有标准答案栏，没有分数。

1. 打开 `frontend/packages/adapters/oss-upload-browser/src/client.ts`。圈 `SIGN_WINDOW = 8`、`MAX_PART_ATTEMPTS = 3`。圈 `upload` 里 `findResume || initialize`、`uploadSingle` / `uploadMultipart`、`completeUpload`。圈 `catch` 里 `signal.aborted && uploadToken` 才 `abortUpload`。
2. 打开 `src/transport.ts`。圈真 XHR：`open`、`requiredHeaders`、`upload.onprogress`、`getResponseHeader('ETag')`、`onabort` 的 `AbortError`。确认没有 `Authorization`。
3. 打开 `src/resume-store.ts` 与 `fingerprint.ts`。圈库名 `wta-oss-upload`、`SAMPLE_SIZE = 1024 * 1024`、`v1:` 前缀。回到 `resumeKey`，确认哈希材料含 clientId、token、policy、fingerprint。
4. 打开 `apps/admin-web/src/application/services.ts` 的 `createOssUploadClient` 调用。圈 `gateway: systemService.resources.oss`。再打开 `createDevelopmentOssTransfer`，圈 `import.meta.env.DEV`。
5. 打开 `packages/domains/system/src/resource-service.ts` 的 `createOssService`。五枪路径与 L-027 对表：uploads、parts/sign、parts、complete、DELETE token。
6. 打开 `web-kit/file-upload/src/upload-request.ts` 与 `FileUpload.vue`。圈假 XHR 的 `handle.abort`。圈默认 `policy: 'document'`。打开 `useDirectOssUpload.ts`，确认没有 `export function useDirectOssUpload`，圈默认 `'general'`。

## 总结、词汇表与下一步

- **宏观三层：** 壳/门面 → 厅堂 `ossUploadClient` → `createOssUploadClient.upload`。票房仍是 L-027。
- **(a) `upload`：** 指纹 → 续传或 init → SINGLE 一次 PUT 或 MULTIPART 窗口 8 → complete → ossId → downloadUrl。
- **(b) progress：** 真 XHR `upload.onprogress` → 百分数；MULTIPART 不回退。FileUpload 不一定画出条。
- **(b) abort：** 假 XHR 转 `AbortSignal` → 真 XHR 停 → 有 token 才 DELETE abort + 撕 IndexedDB。开票中途取消可能留票。complete 失败不靠这枪清理。
- **两扇门面：** `useDirectOssUpload` 改写字段名并给旧 `http-request`；`FileUpload` 是注入 `UploadClient` 的店徽，默认策略 `document`。
- **两辆 XHR：** 假的只负责取消手柄；真的才打 MinIO。
- **续传抽屉可缺席：** IndexedDB 失败不阻断当前页。

词汇表：`createOssUploadClient` / `UploadClient.upload` / `OssUploadGateway` / `transferToOss` / `uploadToken` / `OssPresignedRequest` / `SIGN_WINDOW` / `createIndexedDbResumeStore` / `createOssFileFingerprint` / `uploadDirectToOss` / `directOssUploadRequest` / `FileUpload` / `AbortSignal` / `OssUploadError` / control plane / data plane。

下一步：L-026 才是卡片柜 list/download-url/remove；L-027 才是票房五扇与 complete 回滚；L-076 才是 demo 富文本资产解析。L-007 已认接线。本课结束仍不是掌握证明；要练习请之后主动激活 Homework。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-005 | `frontend/apps/{admin-web,home-web,sso-web}` | 直传单例只在 admin-web `services.ts`；home-web 无 `createOssUploadClient` | `application/services.ts` | 2026-09-16 |
| S-006 | `frontend/packages/{adapters,web-kit,domains,platform}` | adapter / web-kit / domain 分家；`UploadClient` 在 platform-contracts | 各包 `package.json` 与 `src` | 2026-09-16 |
| S-L029-01 | `packages/adapters/oss-upload-browser/src/client.ts` | 工厂、`upload` 因果链、SIGN_WINDOW=8、三轮重签、abort 条件、COMPLETED resume | `createOssUploadClient` / `upload` / `uploadMultipart` | 2026-09-16 |
| S-L029-02 | `src/transport.ts` | 真 XHR PUT、progress、AbortError、CORS 文案、ETag | `transferToOss` | 2026-09-16 |
| S-L029-03 | `src/fingerprint.ts`；`src/resume-store.ts` | v1 头尾 1MiB；IndexedDB `wta-oss-upload`/`sessions` | `createOssFileFingerprint`；`createIndexedDbResumeStore` | 2026-09-16 |
| S-L029-04 | `src/types.ts`；`domains/system/src/resource-service.ts` | 网关八口；五枪 URL 与 `encodeURIComponent`；`validatePresigned` | `OssUploadGateway`；`createOssService` | 2026-09-16 |
| S-L029-05 | `web-kit/file-upload/src/FileUpload.vue`；`upload-request.ts` | 默认 policy=document；假 XHR abort；本地尺子；v-model 序列化 | props 默认值；`createUploadRequest` | 2026-09-16 |
| S-L029-06 | `apps/admin-web/src/hooks/oss/useDirectOssUpload.ts` 及其测试 | 门面映射 ossId；`isHandled` 抑制；假 XHR 默认 general；无 `useDirectOssUpload()` | 四个导出；测试两例 | 2026-09-16 |
| S-L029-07 | `apps/admin-web/src/application/services.ts`；`components/FileUpload/index.vue`；`userAvatar.vue`；`components/Editor/index.vue` | 厅堂注入整份 oss 网关；包装器写死 client；头像/编辑器直调 `upload`；卸载 abort | `ossUploadClient`；`uploadToOss`；`uploadImg` | 2026-09-16 |
| S-L029-08 | `adapters/oss-upload-browser/src/client.test.ts` | 分片重签、完成 resume 零 PUT、缺 ossId、陈旧会话改 init；**无** abort/progress 例 | 四个 `it` | 2026-09-16 |
