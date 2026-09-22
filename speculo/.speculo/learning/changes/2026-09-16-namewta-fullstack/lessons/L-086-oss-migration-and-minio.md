---
lesson_id: L-086
objective_ids: [OBJ-25, OBJ-27, OBJ-28, OBJ-29]
estimated_minutes: 36
time_budget:
  - segment: orientation-and-map
    minutes: 5
  - segment: migration-counter
    minutes: 7
  - segment: two-upload-planes
    minutes: 8
  - segment: minio-create-checklist
    minutes: 9
  - segment: pause
    minutes: 2
  - segment: recap-and-transfer
    minutes: 5
expression_level: eli5
coverage_depth: deep
source_ids: [S-010, S-L086-01, S-L086-02, S-L086-03, S-L086-04, S-L086-05, S-L086-06, S-L086-07, S-L086-08, S-L086-09, S-L086-10]
---

# Lesson 086：搬家柜台不是上传开关——迁移控制器、两条字节路、MinIO 该怎么配

## 学完你能做什么

打开 `SysOssMigrationController`，你能说出它**不上传文件**。它只处理一件已经发生过的事：把目录里仍指向 **PRIVATE** 配置的正常对象，复制到另一份 **PUBLIC_READ** 配置上，再决定什么时候才许删来源副本。

你还能分开三张容易被叫成「上传策略」的牌：

1. 浏览器真正在用的直传：`oss.direct-upload.policies` 里的命名策略，例如 `general`、`avatar`。文件字节不进 Spring。
2. 管理端「对象存储配置」那一行 `sys_oss_config`。它是仓库名牌（endpoint、桶、钥匙、访问类型），不是浏览器上传模式。
3. 代码里还留着的 `ISysOssService.upload(File)`。它会让应用服务器自己把字节交给默认 OSS 客户端。当前工作树里**没有调用方**，管理端 HTTP 也不再收 `MultipartFile`。

最后你能按 MinIO 举例，列出「在页面上新建一条配置」之外还必须先备好的东西。少任何一件，直传都会在别的层失败：桶不存在、匿名策略对不上、没有诊断对象、浏览器够不着预签名主机、CORS 不认你的页面来源。

本课把 L-025 到 L-029 串成一条因果链。它不替换那五课的方法口试，也不把 Goal 的 85 课链改掉。2026-09-16 那几课里「abort 是 DELETE」已经和当前磁盘不一致：现在 abort 是 `POST /resource/oss/uploads/{uploadToken}`。

## 先把宏观地图放在桌上

NAMEWTA 把文件字节放在 MinIO 或别的 S3 兼容存储里，把「这个文件是谁」写在 MySQL 表 `sys_oss`。业务表只记 `ossId`。列 `sys_oss.service` 才是「它现在住在哪一张仓库名牌上」的唯一路由键。名牌表是 `sys_oss_config`。访问类型只允许两档：`0` = `PRIVATE`，`2` = `PUBLIC_READ`。没有 `1`。默认名牌（`status='Y'`）必须是 PRIVATE，而且全表只能有一张默认。

浏览器上传、服务端自己推字节、搬家，是三条不同的门，最后才在 `sys_oss` 和 `OssFactory` 汇合。

```text
浏览器页面
  |  JSON + 登录 Token，不带文件字节
  v
SysOssUploadController          /resource/oss/uploads
  |  校验命名策略、人、权限、readiness
  |  签发短时 PUT
  v
浏览器 --PUT 字节--> MinIO Bucket
  |
  |  再回来 complete
  v
sys_oss 临时行（service = 策略写死的 configKey）

应用服务器里的 File（当前没有生产调用方）
  |
  v
ISysOssService.upload(File) --> OssFactory.instance() 默认名牌
  |
  v
同一张 sys_oss，但不走直传票、不走命名策略

已经在 PRIVATE 名牌上的 ACTIVE 对象
  |
  v
SysOssMigrationController       /resource/oss/migrations
  |  复制到 PUBLIC_READ 名牌，再 CAS 改 sys_oss.service
  v
来源对象先留着；cleanup 批准且过窗口后才删
```

**图题 / caption：** 三条门汇合到同一本目录。alt：直传控制面、无调用方的服务端 upload(File)、迁移柜台三条线，都指向 sys_oss，但只有直传和迁移会碰浏览器或运维 HTTP。

**文字等价物：** 用户选文件时，只走最上那条。应用自己拿着一个 `java.io.File` 时，才会走中间那条，而且当前仓库搜不到这样的调用。搬家柜台不接收新文件，只改已经登记过的对象住在哪张名牌上。三条线都依赖 `sys_oss_config` 里的 endpoint、桶和钥匙，但直传还额外依赖 `application.yml` 的命名策略和 readiness 诊断对象。迁移还额外要求来源 PRIVATE、目标 PUBLIC_READ，而且两边都是 `SERVING`。

**类比：** 学校有一间上锁库房（PRIVATE）和一间临街橱窗（PUBLIC_READ）。学生交作业是自己把本子放进库房（直传），教务处只发一张短时门卡，不替学生抱本子。后勤把整箱旧本子从库房复制到橱窗（迁移），目录卡改指向橱窗之后，库房原件还要再观察一阵才许扔掉。中间那条「老师替学生抱进库房」的窗口还印在规章里，但现在没有老师走那扇窗。

**类比失效处：** 门卡不是 MinIO 的 root 密码。学生交作业失败，常常是库房门牌、匿名锁、CORS 或诊断样本没备好，不是搬家公司没开工。搬家公司也不会因为你新建了一条 MinIO 配置就自动把旧文件搬过去。

| 你以为的名字 | 磁盘上它实际是 | 会不会搬文件字节 | 会不会改 `sys_oss.service` |
| --- | --- | --- | --- |
| 浏览器直传 | `oss.direct-upload.policies.<名字>` + `/resource/oss/uploads` | 浏览器 PUT 到 MinIO | 插入新行时写成策略里的 `storage-config-key` |
| 后端上传 | `ISysOssService.upload(File)`，无 HTTP，无当前调用方 | 应用进程上传到**默认**名牌 | 插入新行时写成 `OssFactory.instance()` 的 configKey |
| 管理端新建 OSS 配置 | `sys_oss_config` 一行 | 不搬、不建桶、不改桶策略 | 不改已有对象 |
| 迁移控制器 | `/resource/oss/migrations` | 服务端把已有对象从来源复制到目标 | CAS：来源 configKey → 目标 configKey |

## 核心概念与机制

### 直觉讲解

`SysOssMigrationController` 是搬家工单窗口，不是上传页。你必须已经知道工单号才能看进度。它没有「列出全部批次」，没有 PUT，没有 DELETE。

直传和「后端上传」不是页面上二选一的开关。现在用户路径只有直传。后端那条 `upload(File)` 还在，是因为旧的「应用服务器收 Multipart 再转发」被裁掉之后，Java 方法还留着；`SysOssController` 已经不准再出现 `upload`、`download` 或 `MultipartFile`。两条路如果有一天同时有人用，它们也只是共享仓库和目录，不共享票据、前缀和权限注解。

新建 MinIO 配置只是把名牌写进数据库和 Redis。桶、桶上的匿名锁、给 readiness 摸的那一个样本对象、浏览器能不能打开预签名 URL、CORS 认不认你的 Origin，都要你在 MinIO 和运行配置里另外做完。应用声明自己不创建 Bucket、不改 Policy。

### 精确定义与 English term

- **对象名牌（storage config）**：表 `sys_oss_config` 的一行。`configKey` 是稳定名字，例如 `minio`。字段包括 `accessKey`、`secretKey`、`bucketName`、`prefix`、`endpoint`、`domainUrl`、`isHttps`、`region`、`accessPolicy`、`status`。`status` 只表示是不是**唯一默认**（`Y`/`N`），不是启停。
- **访问策略（access policy）**：`AccessPolicy`。`PRIVATE(0)` 与 `PUBLIC_READ(2)`。`SysOssConfigBo` 用正则 `[02]` 拒绝别的值。默认配置必须是 `0`。
- **命名上传策略（upload policy）**：`oss.direct-upload.policies` 下的键，例如 `general`。浏览器 init 时提交的是这个名字，**不能**提交 configKey、Bucket 或 TTL。策略自己写死 `storage-config-key` 和 `expected-access-policy`。
- **直传控制面（direct-upload control plane）**：`SysOssUploadController`，前缀 `/resource/oss/uploads`。只收 JSON。权限是 `system:oss:upload`，富文本策略在 yml 里改成 `common:richtext:upload`。
- **数据面（data plane）**：浏览器按预签名请求直接访问 MinIO。MinIO 不看业务 `Authorization`。
- **预签名请求（presigned request）**：短时 PUT（或分片 PUT）。当前 `presign-ttl` 默认 5 分钟。票本身 `ticket-ttl` 默认 24 小时。
- **就绪快照（readiness）**：`OssStorageReadinessService` 对每个必检 configKey 做只读诊断。只有 `SERVING` 才允许直传 init 和迁移开跑。
- **迁移批次（migration batch）**：`sys_oss_migration_batch` / `sys_oss_migration_item`。HTTP dry-run **不**插入批次。

### 机制/因果链

#### 1. 迁移七扇窗，当前线程里做完

`wta-system` 是 classic。链是 `SysOssMigrationController → OssStorageMigrationService → OssMigrationStore / OssMigrationObjectStore / OssMigrationAccessVerifier`。没有 UseCase。

| 方法 | HTTP | 权限 | 写入 |
| --- | --- | --- | --- |
| `batch` | `GET /resource/oss/migrations/{batchId}` | `system:ossMigration:list` | 不写 |
| `items` | `GET /resource/oss/migrations/{batchId}/items` | `system:ossMigration:list` | 不写 |
| `dryRun` | `POST /resource/oss/migrations/dry-run` | `system:ossMigration:execute` | 不建批次、不复制、不改 `service`、不删源 |
| `start` | `POST /resource/oss/migrations/start` | `system:ossMigration:execute` | 再预检；全绿才建批次，并在**这条 HTTP 请求里**逐件 `process`。返回批次号 `Long` |
| `retry` | `POST /resource/oss/migrations/{batchId}/retry` | `system:ossMigration:execute` | 只重做 `FAILED` 且 `lastErrorStage != COMPLETED` 的明细 |
| `rollback` | `POST /resource/oss/migrations/{batchId}/rollback` | `system:ossMigration:rollback` | 还没终态的明细，把 `service` 拨回来源。不删目标副本，不删源对象 |
| `cleanup` | `POST /resource/oss/migrations/{batchId}/cleanup` | `system:ossMigration:cleanup` | 身体必须 `approved=true`。窗口未到就拒绝。然后删**来源**桶里的对象 |

`process` 一件的顺序是固定的：

1. `claim` 这一行，避免两个人同时搬同一件。
2. `transferAndVerify`：从来源复制到目标，对大小和校验。目标上已有**不同内容**则是冲突，不覆盖。
3. `compareAndSetService`：只有当前 `service` 仍是来源，才改成目标。别人先改过就是 `SERVICE_DRIFT`。
4. `verifyPublic`：按新名牌做公开访问核验。失败就把 `service` 拨回来源，这一件记失败。
5. 成功则状态变成 `CLEANUP_ELIGIBLE`，`cleanupEligibleTime = now + cleanupDelay`。默认延迟 24 小时，允许范围 1 分钟到 30 天。

预检硬条件：对象存在、`deleteState=ACTIVE`、来源名牌 readiness 为 `SERVING` 且访问类型 `PRIVATE`、目标名牌 `SERVING` 且 `PUBLIC_READ`、来源和目标 configKey 不同、来源对象真的在桶里、目标没有内容冲突。一批最多 `maxBatchSize`，默认 100。

基座菜单种子有 `system:oss:*` 和 `system:ossConfig:*`，**没有** `system:ossMigration:*`。注解在控制器上，菜单没挂，调用方就算知道 URL 也会被 Sa-Token 拒绝，除非另有人发过这四颗权限。

清理失败不会走 `retry`：`retry` 明确跳过 `lastErrorStage == COMPLETED`。那种失败要另查，不能当成「再搬一次」。

#### 2. 浏览器直传现在怎么走完

前端 `createOssUploadClient` 默认策略名是 `general`。头像页传 `avatar`，富文本按种类拼成 `richtext-image` 这类名字。init 身体只有 `policy`、`fileName`、`fileSize`、`contentType`、`fingerprint`。

服务端 `OssUploadService.init`：

1. 策略必须存在且 `enabled`。
2. 当前登录用户要过策略上的 `required-permission`。
3. 大小和 Content-Type 必须落在策略名单里。
4. `storage-config-key` 的 readiness 必须是 `SERVING`，而且快照里的访问类型必须等于策略的 `expected-access-policy`。对不上就是 `STORAGE_NOT_SERVING` 或 `STORAGE_ACCESS_POLICY_MISMATCH`。
5. 按文件大小决定 `SINGLE` 或 `MULTIPART`。`AUTO` 时，大于等于 `multipart-threshold` 才分片。`avatar` 被写成强制 `SINGLE`。
6. 在该 configKey 的桶里准备对象键，前缀来自策略的 `object-prefix`（例如 `direct/general`），**不是**名牌上的 `prefix` 列。
7. 票和补偿记录写入 Redis。`SINGLE` 的响应里带一张预签名 PUT。

浏览器接着：

- `SINGLE`：按 `presignedRequest` 的方法、URL、`requiredHeaders` 把整个文件 PUT 到 MinIO。
- `MULTIPART`：按窗口向 `POST .../parts/sign` 要分片 URL，PUT 分片，从响应读 `ETag`。续传是 `GET .../parts?fingerprint=`。
- 然后 `POST .../complete`。服务端 HEAD 对象，对大小、Content-Type、指纹元数据 `upload-fingerprint`，并对若干类型读一小段 magic bytes。通过才 `registerTemporary`：插入 `sys_oss`，`is_temp='Y'`，`service` 是票里冻结的 configKey，`url` 先是空串。
- 校验失败时服务端自己 abort 分片、删对象、撕票，不留这行。
- 用户取消时前端 `POST /resource/oss/uploads/{uploadToken}`，不是 DELETE。

完成之后业务保存再调用 `OssService.reconcileReferences`，把临时对象挂到真实表。管理列表不把可直接打开的 URL 填回去；下载走 `GET /resource/oss/{ossId}/download-url`。

开发环境只有在设置了 `VITE_APP_OSS_PROXY_PREFIX` 时，才会把预签名 URL 改写到当前页面源站再代理。当前 `frontend/apps/admin-web/.env.development` **没有**这项，所以浏览器会直连签名里的主机。

#### 3. 「后端上传」还在哪，为什么说它独立但不在岗

`SysOssServiceImpl.upload(File, SysOssExt)` 用 `OssFactory.instance()`，也就是 Redis 里那把默认 configKey，自己 `buildPathKey` 再 `upload`。它不读 `oss.direct-upload.policies`，不建 Redis 票，不检查命名策略的 Content-Type 名单，也不走迁移控制器。

`OssProtocolCutoverUnitTest` 锁死了：`SysOssController` 不得再有名为 `upload` 或 `download` 的方法，不得接收 `MultipartFile`，不得返回 `ResponseEntity`。全仓库对 `upload(File` 的引用只剩接口和这个实现。所以：

- 和直传**独立**：控制面、前缀、权限、是否要求 readiness，都不是同一套。
- 和直传**不并立成产品开关**：用户上传不会悄悄落到这条路上。
- 和直传**不隔离存储**：一旦有人以后调用它，新行仍进 `sys_oss`，`service` 是当时的默认名牌。那张名牌若是 `minio`，对象就和直传对象住在同一个桶的不同键前缀上。

不要把「后端签发预签名」说成后端上传。签发是直传的控制面，字节仍然不经过应用服务器。

#### 4. 新建一条 MinIO 名牌时，应用实际做了什么

`insertByBo` 只做这些事：空的 `status` 归一成 `N`；校验访问类型只能是 `0` 或 `2`；若你把 `status` 设成 `Y`，则必须是 PRIVATE，并清掉其他行的默认标记；插入数据库；提交后写 Redis 缓存、丢掉旧的 `OssFactory` 客户端，并立刻 `readinessService.refresh()`。

它**不会**：

- 在 MinIO 上 `createBucket`；
- 给桶套匿名读或匿名写策略；
- 上传诊断样本；
- 改 `application.yml` 里的 `storage-config-key`；
- 迁移已有对象；
- 给浏览器配置 CORS。

`OssFactory` 组客户端时：`isHttps=Y` 才用 `https`，否则强制 `http`，endpoint 上原有的协议头会被剥掉再重贴。`region` 空白则用 `us-east-1`。endpoint 字符串里不含 `aliyun`、`qcloud`、`qiniu`、`obs` 时，按路径风格访问（`http://主机:端口/桶名/对象键`）。MinIO 需要的就是路径风格。预签名器优先用 `domainUrl`；PRIVATE 默认名牌可以把 `domainUrl` 留空，这时签名主机就是 endpoint。

readiness 对这个 configKey 说 `SERVING` 之前，还要同时满足：

- `accessPolicy` 能解析成 `0` 或 `2`；
- 若是 `PUBLIC_READ`，`domainUrl` 不能空，除非 `oss.readiness.allow-endpoint-domain-fallback=true`（默认 false）；
- `oss.readiness.diagnostic-objects.<configKey>` 有一个对象键，凭据对它 `HEAD` 成功；
- 匿名读是否允许，必须和名牌上的访问类型一致；
- 匿名写必须被拒绝。写得开就是 `MISMATCH`，不是「更方便」。

`application.yml` 没有写 `diagnostic-objects`。`application-local.yml` 只为键 `minio` 准备了 `.well-known/oss-readiness/private-canary.txt`。没配这个键时，原因是 `DIAGNOSTIC_OBJECT_MISSING`，直传 init 直接不可服务。样本对象要事先放进桶里。应用不负责创建它。

启动时 `OssUploadDiagnostics` 还会另打一行 CORS/Lifecycle 辅助检查。当前这份 MinIO 镜像用标准 `PutBucketLifecycle` 配不出 `AbortIncompleteMultipartUpload`。那一行警告**不等于** CORS 失败，也不等于 readiness 失败。直传自己仍有取消 abort 和 Redis 票过期清理。`cleanup-enabled` 默认 false，`cleanup-dry-run` 默认 true，不会为了消警告去删业务对象。

### 图、表或文本图

```text
新建 MinIO 名牌要齐的四层

[A 名牌]  sys_oss_config
          configKey=minio
          endpoint=浏览器和 JVM 都够得着的主机:端口
          bucket=已经存在的桶
          accessPolicy=0
          status=Y 时必须是全表唯一默认，且必须 PRIVATE

[B 桶事实]  MinIO 里先有这个桶
            PRIVATE：匿名 GET/HEAD 拒绝，匿名写拒绝
            桶里已有 diagnostic-objects.minio 指向的那个对象

[C 上传策略] application.yml
            policies.*.storage-config-key=minio
            expected-access-policy=PRIVATE
            浏览器只提交 policy 名字

[D 浏览器]  页面 Origin 在 MINIO_API_CORS_ALLOW_ORIGIN 里
            预签名主机从浏览器可达
            PUT 被允许，ETag 被暴露
            请求头至少盖得住 content-type 和
            x-amz-meta-upload-fingerprint
```

**图题 / caption：** MinIO 能接上直传之前的四层，缺一层就会在另一层报错。alt：A 数据库名牌、B 桶和样本对象、C yml 命名策略、D 浏览器 Origin 与预签名主机。

**文字等价物：** A 只回答「应用用哪把钥匙、哪个桶、这种桶算私有还是公开读」。B 回答「桶是否真按这个访问类型锁好，以及 readiness 要摸的样本在不在」。C 回答「浏览器说 general 时，必须落到哪张名牌，以及这张名牌必须是 PRIVATE」。D 回答「签名发出去之后，浏览器能不能跨域 PUT 成功」。迁移控制器不在这四层里。它要的是第二张已经 SERVING 的 PUBLIC_READ 名牌，以及一批已经存在的 PRIVATE 对象。

Compose 里的 MinIO 把容器 `9000` 映到宿主机 `127.0.0.1:49000`（控制台是 `49001`）。种子 SQL 写的 endpoint 是 `127.0.0.1:9000`。进程若跑在宿主机上、MinIO 只按 Compose 发布，名牌应写 `127.0.0.1:49000`，不要照抄种子的 `9000`。种子里的 `access_policy='1'` 会在同一份初始化脚本后面被改成 `'0'`；服务代码不接受 `'1'`。种子里的 `wta` / `wta123` 只是基座占位，不等于 Compose 要求的 `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD`。名牌上的钥匙必须是这只 MinIO 上真实能 HEAD/PUT 的用户。这些秘密只放服务端配置或数据库，不能写进任何 `VITE_*`。

| 字段 | 本地 MinIO 建议怎么填 | 填错时你看到的不是「保存失败」 |
| --- | --- | --- |
| `configKey` | 跟 yml 一致，现成策略全是 `minio` | 直传找不着名牌，或 readiness `CONFIG_MISSING` |
| `endpoint` | 宿主机访问用发布端口，例如 `127.0.0.1:49000`；不要带路径 | JVM 连不上，或浏览器打开的是容器内才认识的地址 |
| `domainUrl` | PRIVATE 默认可空。PUBLIC_READ 必须是浏览器和匿名客户端都能打开的域名 | 公开配置 `DOMAIN_REQUIRED`，一直 `NOT_SERVING` |
| `isHttps` | 本地 HTTP 用 `N` | 签名变成 https，浏览器或 JVM 证书/端口对不上 |
| `region` | 留空即可，客户端用 `us-east-1` | 一般不是第一故障点 |
| `bucketName` | 事先建好的桶，例如 `wta` | HEAD 诊断对象失败 |
| `prefix` | 可空。它不决定直传键 | 你以为改了前缀，直传对象仍在 `direct/...` |
| `accessPolicy` | 默认桶填 `0`。公开读另建一行填 `2`，且不要设成默认 | 保存即拒绝；或和 yml 的 `PRIVATE` 不一致导致 `STORAGE_ACCESS_POLICY_MISMATCH` |
| `status` | 第一张私有桶 `Y`，其余 `N` | 启动时「必须且只能存在一个默认配置」；公开桶不能当默认 |
| `accessKey` / `secretKey` | 这只 MinIO 的真实用户 | 诊断和签名都失败。编辑时空白 secret 会沿用旧值 |

### 正例、反例与边界

**正例。** 本地只做私有直传：MinIO 已有桶 `wta`，里面已有 `.well-known/oss-readiness/private-canary.txt`；桶拒绝匿名读写；名牌 `configKey=minio`、`accessPolicy=0`、`status=Y`、`endpoint` 是浏览器也能访问的 `127.0.0.1:49000`、`isHttps=N`、`domainUrl` 空；`application-local.yml` 把诊断对象指到那个键；yml 策略仍是 `storage-config-key: minio` 且 `expected-access-policy: PRIVATE`；`MINIO_API_CORS_ALLOW_ORIGIN` 包含页面真实 Origin，例如 `http://localhost:5177`，并且暴露 `ETag`。保存后 listener 刷新 readiness。日志出现该 configKey readiness 通过。页面用 `general` 或 `avatar` 上传。字节 PUT 到 MinIO，complete 得到 `ossId`。

**反例。** 在管理端再建一条 `configKey=minio-public`、`accessPolicy=2`，期望新上传自动进公开桶。直传不会去。现成策略仍点名 `minio` + `PRIVATE`。公开桶又没填 `domainUrl`，readiness 直接 `DOMAIN_REQUIRED`。迁移也不会自动开始。要公开旧对象，得另有一张已经 `SERVING` 的 PUBLIC_READ 名牌，再对指定 `ossId` 调 dry-run，通过后才 start。

**边界。**

- 名牌已被 `sys_oss.service` 引用时，普通编辑不能改 `configKey`、`bucketName`、`accessPolicy`。想换桶或换公私，是新名牌加迁移，不是改原行。
- 不能靠把默认行编辑成 `status=N` 来取消默认。要换默认，走切换操作，目标行必须仍是 PRIVATE。
- `start` 在当前 HTTP 线程里搬完这一批。批很大时请求会一直占着，它不是丢进后台队列就返回。
- `rollback` 不删已经复制到目标桶的对象。`cleanup` 删的是来源，不是目标。
- CORS 辅助检查失败，而 readiness 已通过时，浏览器 PUT 仍可能因 Origin 不对而失败。两条日志要分开读。
- `localhost` 和 `127.0.0.1`、不同端口，是不同 Origin。
- 本课不给出生产桶策略 JSON，也不代替 `docs/oss-public-private-operations.md` 里的发布批准。那份手册写明：应用不创建桶、不改 Policy。

## 变式与迁移

把 MinIO 换成别的 S3 兼容存储时，四层还在，只有推断规则变一点。endpoint 里若包含 `aliyun`、`qcloud`、`qiniu`、`obs`，客户端改走虚拟主机风格，桶名会进主机名而不是路径。自建 MinIO 不要把 endpoint 写成带这些字的域名，否则路径风格判断会翻掉。

公开读是变式，不是把私有桶的 `accessPolicy` 改成 `2`。正确变式是：第二张名牌、独立桶、匿名只读且拒绝匿名写、可公开的 `domainUrl`、readiness `SERVING`，然后要么给**新**上传单独加一条 `expected-access-policy: PUBLIC_READ` 的 yml 策略，要么只对存量对象走迁移柜台。默认名牌保持 PRIVATE。历史对象升级脚本把未知或旧的 `0/1/2` 都收成 `0`，不会自动变公开。

阿里云、七牛、腾讯云的种子行是占位钥匙，`status=N`。它们不会因为排在种子里就成为直传目标。

## 常见误区

1. **迁移控制器负责上传或切换默认桶。** 上传在 `/resource/oss/uploads`。默认桶在配置控制器的切换。迁移只搬已经登记的对象，而且方向被写成 PRIVATE → PUBLIC_READ。
2. **页面上可以选「浏览器直传 / 后端上传」。** 没有这个开关。用户路径只有直传。`upload(File)` 没有调用方，也没有 HTTP。
3. **保存 OSS 配置就会建桶、开公开读、配 CORS。** 保存只写库、缓存和 readiness 刷新。桶和锁要事先在 MinIO 上成立。
4. **种子 endpoint `127.0.0.1:9000` 就是 Compose 地址。** Compose 把 API 发布在宿主机 `49000`。哪边连 MinIO，endpoint 就写哪边能打开的地址。签名主机还要给浏览器用。
5. **`prefix` 列决定直传目录。** 直传目录是策略的 `object-prefix`。
6. **`status=N` 表示配置停用，上传会跳过它。** `N` 只表示「不是默认」。直传看的是策略点名的 configKey 是否 `SERVING`，不看它是不是默认。默认指针只给没写 configKey 的 `OssFactory.instance()` 用，也就是那条没人调用的 `upload(File)`。
7. **readiness 警告和 CORS 警告是同一件事。** readiness 看诊断对象和匿名读写是否匹配。CORS 辅助检查看浏览器跨域前置。Lifecycle 在这版 MinIO 上失败，不应靠给整桶加过期规则来消掉。
8. **dry-run 会留下批次，失败了再 rollback。** dry-run 零写入。没有批次就没有 rollback。
9. **把业务 Token 发给 MinIO 才能上传。** MinIO 只认预签名和签名要求的头。Token 只出现在打到 Spring 的 JSON 请求上。
10. **L-027 仍写 abort 是 DELETE，所以现在还是 DELETE。** 以当前 `SysOssUploadController` 和前端 `abortUpload` 为准，两者都是 POST。

## 非评分暂停

先别往下看总结。用自己的话走一遍，不要求写下来交给谁：

- 一个新头像从按钮到 `ossId`，字节经过哪几个主机，哪一步才插入 `sys_oss`。
- 为什么只在管理端把访问类型改成公开读，新上传仍进不了那个桶。
- 迁移 `cleanup` 删的是哪一边的对象，为什么 `retry` 不管清理失败。

说的时候如果把「保存配置」和「建桶」说成同一步，回到上面的四层图，指出缺的是 B 还是 D。

## 总结、词汇表与下一步

`SysOssMigrationController` 是 PRIVATE 对象搬到 PUBLIC_READ 名牌的工单窗口：预检不落库，启动才建批次并在当前请求里复制、校验、改目录指针；回滚只拨指针；清源要再次批准并等窗口。它不接收上传。

浏览器直传和 `upload(File)` 是两套独立控制面，共享目录和客户端工厂，但后者当前没有调用方，也没有管理端入口。用户上传只走命名策略加预签名 PUT。

MinIO 要能接上，名牌、桶事实、yml 策略、浏览器可达性和 CORS 必须一起成立。只填管理端表单不够。默认私有桶用 `accessPolicy=0`；公开读是另一张名牌，不是上传模式开关。

下一步如果要口试单扇窗的失败码，回到 L-025（默认切换）、L-027（complete 回滚）、L-028（迁移阶段）和 L-029（浏览器客户端）。本课不布置作业，也不表示这些目标已经掌握。

| 词 | 指什么 |
| --- | --- |
| configKey | 名牌名字，直传策略和 `sys_oss.service` 都用它 |
| upload policy | yml 里的 `general` 这类名字，浏览器只许提交这个 |
| access policy | 桶的 `0` 或 `2`，不是上传模式 |
| readiness `SERVING` | 诊断对象和匿名行为已核对，才许 init 或迁移 |
| presigned PUT | 浏览器把字节交给 MinIO 的那一下 |
| CAS | 只有 `service` 仍是预期旧值才更新 |
| cleanup | 删来源对象，不是删目标，也不是删数据库行 |

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-010 | 基座 DDL/DML | 表结构、种子 endpoint、访问类型回填为 `0`、迁移菜单不在种子里 | `release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql`、`50-cde-base-dml.sql` | 2026-09-22 |
| S-L086-01 | 工作树 | 迁移七个 HTTP 方法与权限 | `backend/wta-modules/wta-system/src/main/java/org/namewta/system/controller/system/SysOssMigrationController.java` | 2026-09-22 |
| S-L086-02 | 工作树 | dry-run 零写入、PRIVATE→PUBLIC_READ、复制/CAS/公开核验/回滚/清理窗口 | `.../oss/migration/OssStorageMigrationService.java`、`OssStorageMigrationProperties.java` | 2026-09-22 |
| S-L086-03 | 工作树 | 直传五窗口；abort 为 POST；不收文件字节 | `.../controller/system/SysOssUploadController.java` | 2026-09-22 |
| S-L086-04 | 工作树 | init 绑定策略与 readiness；complete 校验后登记临时行 | `.../oss/upload/OssUploadService.java`、`OssUploadProperties.java`、`DefaultOssUploadMetadataStore.java` | 2026-09-22 |
| S-L086-05 | 工作树 | `upload(File)` 走默认客户端；HTTP 已禁止 Multipart | `SysOssServiceImpl.java`、`SysOssController.java`、`OssProtocolCutoverUnitTest.java` | 2026-09-22 |
| S-L086-06 | 工作树 | 保存配置不建桶；默认必须 PRIVATE；引用后不能改桶和访问类型；提交后刷新缓存与 readiness | `SysOssConfigServiceImpl.java`、`OssConfigChangeListener.java` | 2026-09-22 |
| S-L086-07 | 工作树 | 诊断对象缺失则不可服务；公开读缺 domain 则不可服务；匿名写打开算不匹配 | `OssStorageReadinessService.java`、`AbstractOssClientImpl.diagnoseAccess`、`application.yml`、`application-local.yml` | 2026-09-22 |
| S-L086-08 | 工作树 | 路径风格推断、endpoint 协议重写、预签名优先 domain | `OssClientConfig.java`、`DefaultOssClientImpl.java`、`OssConstant.CLOUD_SERVICE` | 2026-09-22 |
| S-L086-09 | 工作树 | 浏览器只提交策略名；开发代理默认未启用；abort 为 POST | `frontend/packages/adapters/oss-upload-browser/src/client.ts`、`frontend/packages/domains/system/src/resource-service.ts`、`frontend/apps/admin-web/.env.development`、`frontend/apps/admin-web/src/application/services.ts` | 2026-09-22 |
| S-L086-10 | 项目文档 | 应用不创建桶、不改 Policy；CORS 与 Lifecycle 告警要分开；Compose 端口与 CORS 环境变量 | `docs/oss-public-private-operations.md`、`docs/error/oss-login-and-direct-upload-troubleshooting.md`、`release-artifacts/docker/docker-compose-infrastructure.yml` | 2026-09-22 |

不确定、本课没有当成事实的部分：你这台机器上的 MinIO 桶策略和诊断对象是否已经放好，没有在本课里连上去看。`application.yml` 未提供 `diagnostic-objects` 时，没加载 `application-local.yml` 的进程会一直 `DIAGNOSTIC_OBJECT_MISSING`。生产 Origin 白名单以实际部署环境文件为准，不在本仓库的 Compose 默认值 `*` 里。
