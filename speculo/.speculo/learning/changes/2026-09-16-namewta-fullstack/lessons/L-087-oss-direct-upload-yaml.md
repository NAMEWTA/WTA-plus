---
lesson_id: L-087
objective_ids: [OBJ-25, OBJ-27, OBJ-29]
estimated_minutes: 37
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: two-trees-and-read-write
    minutes: 8
  - segment: field-by-field
    minutes: 12
  - segment: storage-config-key
    minutes: 7
  - segment: pause
    minutes: 2
  - segment: recap-and-transfer
    minutes: 4
expression_level: eli5
coverage_depth: deep
source_ids: [S-L087-01, S-L087-02, S-L087-03, S-L087-04, S-L087-05, S-L087-06, S-L087-07]
---

# Lesson 087：`application.yml` 里的 OSS 段——谁读、谁写、`minio` 为什么写死

## 学完你能做什么

打开 `backend/wta-admin/src/main/resources/application.yml` 第 82–218 行，你能把这一段拆成两棵树，并说出每个键被谁读取、运行时写出的是什么、文件本身会不会被改。

你还能回答：十条上传策略的 `storage-config-key` 都写成 `minio`，换存储时要不要改。结论先放在这里：这个字段必须有；它绑定的是数据库名牌的 `configKey`，不是 MinIO 这个软件的品牌。只改那一行的 endpoint、桶和钥匙时，yml 不用动。新建或改名一张名牌、又希望新上传走进去时，必须改对应策略的这一列，并重启进程。已有对象不会跟着走。

本课不替换 L-025 到 L-029，也不进入原来的 85 课 Goal 链。

## 先把宏观地图放在桌上

这段 YAML **不是**管理端「对象存储配置」页面。页面写的是表 `sys_oss_config`。这里写的是进程启动时装进两个 Java 配置对象的规则：下载签名能活多久，以及浏览器说「我要用 image」时，服务端必须把文件送到哪张名牌、什么前缀、多大、什么类型。

```text
application.yml 第 82–218 行          只读进内存，进程不写回这个文件
        |
        +-- oss.lifecycle                 OssLifecycleProperties
        |     私有下载签名活多久
        |     其中只有 download-ttl 允许被 Nacos 换掉内存值
        |
        +-- oss.direct-upload             OssUploadProperties
              全局：票、预签名、清理开关
              policies.<名字>：十条命名上传策略

浏览器只提交策略名，例如 image
        |
        v
OssUploadService.init
        |  读 policies.image
        |  读 storage-config-key = minio
        v
OssFactory.instance("minio")          名牌在 sys_oss_config / Redis
        |
        v
对象键 = 名牌 prefix + direct/image + 日期 + uuid
sys_oss.service = minio               这是运行时写入，不是写 yml
```

**图题 / caption：** YAML 只提供规则，名牌和对象目录在别处被写入。alt：左侧 yml 两棵树只被读取；右侧 Redis 票、MinIO 对象和 sys_oss.service 才是运行时写入。

**文字等价物：** 改 yml 不会立刻改数据库，也不会改已经传上去的对象。Spring 在启动时绑定这两个 `@ConfigurationProperties`。绑定失败或 `afterPropertiesSet` 校验失败，进程起不来。直传策略整段不在 Nacos 那只「只刷新 download-ttl」的键集合里。所以改完 `storage-config-key` 不重启，正在跑的进程仍用旧值。

**类比：** yml 是食堂窗口的菜单。`image` 这道菜规定「送到 1 号库房、放在 direct/image 架、只收图片、最大 20MB」。1 号库房的地址贴在另一本账 `sys_oss_config` 上，账上的名字就叫 `minio`。你把 1 号库房的门锁和街道换了，菜单仍写 1 号，不用改。你新开了 2 号库房，菜单却还写 1 号，新菜继续送去 1 号。

**类比失效处：** 菜单不是给人点库房号的。浏览器不能提交 `minio`。另外，默认库房开关 `status=Y` 只给没写 configKey 的 `OssFactory.instance()` 用，直传不看它。

## 核心概念与机制

### 直觉讲解

这一段里几乎所有键都是**读**。应用不把上传结果写回 `application.yml`。

真正被写的是三处别的地方：Redis 里的上传票和清理记录、MinIO 里的对象、MySQL `sys_oss` 行。`storage-config-key` 的值会被抄进票和 `sys_oss.service`。`object-prefix` 会被抄进对象键。`presign-ttl` 和 `ticket-ttl` 变成过期时间。它们不是配置文件的回写。

`minio` 四个字母出现十次，是因为十条策略都声明「请使用名叫 minio 的那张名牌」。它不是探测本机有没有装 MinIO。名牌可以指向阿里云，只要那一行的 `config_key` 仍叫 `minio`。反过来，数据库里真有一个 MinIO，但 `config_key` 改成了 `wta-plus-local`，而这列仍写 `minio`，直传会去找一个不存在或不是你刚配的那张名牌。

### 精确定义与 English term

- **配置绑定（configuration properties binding）**：`@ConfigurationProperties(prefix = "oss.lifecycle")` 与 `prefix = "oss.direct-upload"`。YAML 的 kebab-case 对上 Java 的 camelCase。
- **命名上传策略（upload policy）**：`policies` 下面的键，如 `image`、`document`。浏览器 init 的 `policy` 只能是这些键。
- **存储名牌键（storage config key）**：`storage-config-key`。必须匹配 `sys_oss_config.config_key`。正则是 2 到 20 位，字符为字母、数字、`.`、`_`、`-`。
- **期望访问类型（expected access policy）**：`PRIVATE` 或 `PUBLIC_READ`。必须和那张名牌上的 `access_policy` 以及 readiness 快照一致。
- **对象前缀（object prefix）**：`object-prefix`。不能以 `/` 开头或结尾，不能含 `..`。
- **下载 TTL（download TTL）**：私有对象签名 URL 的有效期。默认策略、`preview`、`extended-preview` 是三档不同的时长，不是三条上传策略。

### 机制/因果链

启动时 `OssLifecycleProperties.validate()` 和 `OssUploadProperties.validate()` 各跑一次。越界就抛异常，应用不会带着半套策略继续服务。直传至少要有一条策略，否则启动失败。缺 `storage-config-key` 或 `expected-access-policy` 同样启动失败。所以这个字段不是可选项。

每次 `POST /resource/oss/uploads`：

1. 控制器先要求权限 `system:oss:upload`。这道门写在 Java 注解上，**不读** yml。
2. `requirePolicy` 按请求里的名字取策略。名字不存在或 `enabled=false` 则拒绝。yml 没写 `enabled`，默认 true。
3. `authorizePolicy` 再查策略上的 `required-permission`。富文本四条写的是 `common:richtext:upload`。调用方必须**先过控制器的 `system:oss:upload`，再过这一条**。只改 yml 不能让一个只有富文本权限的人绕过控制器。
4. 若策略配了 `allowed-client-pks`（这段 yml 没配，默认空集，等于不限制 Client），当前 Client 主键必须在集合里。
5. 比对 `max-size` 和 `allowed-content-types`。
6. 用 `storage-config-key` 查 readiness。不是 `SERVING`，或快照里的访问类型不等于 `expected-access-policy`，就拒绝。
7. `resolveMode`：`SINGLE` 永远单请求；`MULTIPART` 永远分片；`AUTO` 时文件大小 **大于等于** `multipart-threshold` 才分片。
8. `OssFactory.instance(storage-config-key)` 拿客户端。对象键用名牌自己的 `prefix` 接上 `object-prefix`，再接 `年/月/日/uuid.后缀`。
9. 票在 Redis 里活 `ticket-ttl`。清理记录活 `cleanup-record-ttl`，且必须不短于票。预签名 PUT 只活 `presign-ttl`。
10. complete 成功后，`sys_oss.service` 写成票里冻结的那个 configKey，也就是当时的 `storage-config-key`。

下载是另一条读路径。`resolveAccessUrl` 对私有对象调用 `resolveDownloadTtl(null)`，用的是 `download-ttl`（这里是 2 分钟），**不用** `preview` 那两档。`presignDownload(ossId, 策略名)` 才会读 `download-policies`。当前生产代码里没有调用方传入 `preview` 或 `extended-preview`；这两档是留给显式传入名字的服务端代码，浏览器不能自己挑。

未完成上传的清扫任务每 `oss.direct-upload.cleanup-delay` 跑一次，这段 yml 没写，默认 `PT10M`。`cleanup-enabled: false` 时任务直接返回。改成 true 且 `cleanup-dry-run: true` 时只打日志，不删对象。两行都放开才会对过期且未登记的对象做 abort 和 delete。这和迁移控制器的 cleanup、也和 `oss.lifecycle` 上另一套默认关闭的临时对象清理，不是同一个开关。

### 图、表或文本图

| 键 | 这段里的值 | 谁读 | 运行时写出什么 | 不做什么 |
| --- | --- | --- | --- | --- |
| `oss.lifecycle.download-ttl` | `2m` | 私有访问 URL、无名字的下载签名；可被 Nacos 换内存值 | 签名的 `expiresAt` | 不决定上传目录 |
| `download-ttl-min` / `max` | `1m` / `10m` | 只做校验边界 | 不写 | 任何一档 TTL 超出这个闭区间，启动失败 |
| `download-policies.preview.ttl` | `5m` | 仅当代码传入策略名 `preview` | 那次签名的到期时间 | 当前没有生产调用方 |
| `download-policies.extended-preview.ttl` | `10m` | 仅当代码传入 `extended-preview` | 同上 | 同上；10 分钟已经顶到 max |
| `ticket-ttl` | `24h` | 建票 | Redis 票 TTL，响应里的 `expiresAt` | 允许范围 5 分钟到 7 天 |
| `presign-ttl` | `5m` | 每次签发 PUT | 预签名 URL 的有效期 | 允许 1 到 30 分钟；过期后要重新 sign，票还可以还在 |
| `cleanup-record-ttl` | `7d` | 建清理记录 | Redis 清理记录 TTL | 必须 ≥ `ticket-ttl`，且 ≤ 30 天 |
| `max-sign-parts` | `20` | `signParts` | 不写存储 | 一次最多收 20 个分片号；允许 1 到 100 |
| `cleanup-enabled` | `false` | 直传清理定时任务 | false 时不扫 | 与迁移 cleanup、临时对象 cleanup 无关 |
| `cleanup-dry-run` | `true` | 同上，且 enabled 为 true 时才有意义 | true 只记日志 | 不是迁移的 dry-run 接口 |
| `policies.<名>.storage-config-key` | 全部 `minio` | init、readiness 必检集合、客户端工厂 | 抄进票和 `sys_oss.service` | 不读取「当前默认配置」 |
| `expected-access-policy` | 全部 `PRIVATE` | 与名牌、readiness 比对 | 不改名牌上的访问类型 | 不能靠它把桶改成公开 |
| `max-size` | 见下表 | init | 不写 | 字节上限，不是分片大小 |
| `allowed-content-types` | 见 yml 名单 | init，以及 complete 时再对 HEAD 的类型 | 不写 | 不看扩展名；`image/*` 这种前缀写法代码支持，这段名单用的是完整类型 |
| `object-prefix` | `direct/...` | `buildPathKey` | 对象键的中间一段 | 不替换名牌上的 `prefix`，而是接在它后面 |
| `mode` | 多数 `AUTO`，`avatar` 为 `SINGLE` | `resolveMode` | 票里的 SINGLE 或 MULTIPART | `avatar` 即使超过阈值也不分片 |
| `multipart-threshold` | 10MB、50MB 或 100MB | 仅 `AUTO` | 不写 | `SINGLE` 时这列不参与决定 |
| `part-size` | 5MB、8MB 或 16MB | 分片 | 票里的 partSize | 合法范围 5MB 到 5GB；`max-size / part-size` 不能超过 10000 片 |
| `required-permission` | `system:oss:upload` 或 `common:richtext:upload` | init 的第二道权限 | 不写 | 放宽不了控制器上写死的第一道 |

十条策略的大小，按 1024 进位：

| 策略名 | 谁默认提交这个名字 | 最大体积 | AUTO 分片阈值 | 分片大小 | 对象前缀 | 第二道权限 |
| --- | --- | --- | --- | --- | --- | --- |
| `general` | 上传客户端没传 policy 时；home-web 材料 | 10MB | 100MB，所以 10MB 上限内始终是单请求 | 16MB | `direct/general` | `system:oss:upload` |
| `avatar` | 头像页 | 10MB | 不使用，模式锁死 SINGLE | 5MB | `direct/avatar` | 同上 |
| `image` | `ImageUpload` | 20MB | 10MB | 5MB | `direct/image` | 同上 |
| `document` | `FileUpload` | 100MB | 50MB | 8MB | `direct/document` | 同上 |
| `editor-image` | 这段 yml 有定义；是否有页面传入要看调用点 | 20MB | 10MB | 5MB | `direct/editor/image` | 同上 |
| `editor-video` | 同上 | 500MB | 100MB | 16MB | `direct/editor/video` | 同上 |
| `richtext-image` / `audio` / `video` / `file` | 富文本按种类拼出 `richtext-<种类>`，附件用 `richtext-file` | 20MB / 100MB / 500MB / 100MB | 10MB / 50MB / 100MB / 50MB | 5MB / 8MB / 16MB / 8MB | `direct/richtext/...` | `common:richtext:upload` |

**图题 / caption：** 策略表只区分「浏览器报哪个名字」，不区分 MinIO 品牌。alt：十行策略的大小、分片、前缀和权限；storage-config-key 列全部是 minio，所以没有单独画出来。

**文字等价物：** 体积和前缀按策略名变化，目标名牌在这段文件里没有变化。`general` 的阈值是 100MB，但最大只允许 10MB，因此它的 AUTO 实际上总是单请求上传。`image` 的阈值是 10MB，20MB 的图会分片。头像即使文件大也强制单请求，超过 10MB 会在大小检查被拒绝，不会改走分片。

这段没有写出、但类里仍有默认值的键：直传 `cleanup-batch-size` 默认 100；生命周期 `temp-retention` 默认 24 小时，complete 后的临时对象用它算 `expire_time`；生命周期自己的 `cleanup-enabled` 默认 false。它们不在 82–218 行里。

### 正例、反例与边界

**正例。** 名牌 `config_key=minio` 的 endpoint 从 `127.0.0.1:9000` 改成 `172.16.105.9:9000`，桶改成 `wta-plus-local`，访问类型仍是 PRIVATE。十条策略继续写 `storage-config-key: minio`。保存名牌并等 readiness 通过后，新的 image 上传仍走 `policies.image`，对象键仍以 `direct/image` 开头，只是主机和桶变成新地址。不必改 yml。

**反例。** 新建名牌 `config_key=aliyun`，把默认开关拨到它上面，希望照片自动进阿里云。直传仍读 `storage-config-key: minio`。默认开关不影响它。新照片继续进入名叫 `minio` 的那张名牌；那张如果已被引用，还不能直接改桶。阿里云那一行一直空着。

**边界。**

- 可以只改一条。例如只把 `document.storage-config-key` 改成另一张已经 `SERVING`、且访问类型等于 `PRIVATE` 的名牌。图片仍进 `minio`。这正是每条策略各写一列的原因。
- 改完 yml 要重启。这段不是 `oss.lifecycle.download-ttl` 那种 Nacos 热更新键。
- 改列只影响之后的 init。旧行的 `sys_oss.service` 仍是当初的 `minio`。下载继续找旧名牌。迁移控制器只接受 PRIVATE 到 PUBLIC_READ，不是「从 minio 这个名字搬到 aliyun 这个名字」的通用搬家，除非目标名牌恰好是 PUBLIC_READ 且来源是 PRIVATE。
- 目标名牌的访问类型若是 PUBLIC_READ，而策略仍写 `expected-access-policy: PRIVATE`，init 会 `STORAGE_ACCESS_POLICY_MISMATCH`。两列要一起改，不能只改名字。
- 删掉 `storage-config-key` 不会变成「走默认配置」。校验直接拒绝启动。
- 控制器权限和策略权限是两道。富文本策略写 `common:richtext:upload`，并不替换接口上的 `system:oss:upload`。

## 变式与迁移

换云厂商有两种变式。

第一种：沿用名字 `minio`。只改这张名牌的 endpoint、钥匙、桶。前提是这张名牌还没有被对象引用；一旦有 `sys_oss.service=minio` 的行，普通编辑不能改桶名和访问类型。此时 yml 一个字都不用动。

第二种：新名字。插入新名牌，把需要搬走的**新流量**所在策略的 `storage-config-key` 改成新名字，核对 `expected-access-policy`，重启。旧对象留在旧名字上，直到另有经过批准的迁移或业务重传。

不要把 `object-prefix` 当成换厂商的开关。`direct/image` 只是桶里的目录。换厂商换的是名牌键和名牌上的 endpoint。

## 常见误区

1. **这段 yml 就是 MinIO 控制台配置。** 它不建桶、不写 CORS、不保存钥匙。钥匙在 `sys_oss_config`。
2. **`storage-config-key: minio` 表示只能用 MinIO 软件。** 它表示「使用 config_key 等于 minio 的那一行」。那一行可以是任何 S3 兼容地址。
3. **换默认 OSS 后直传会跟着走。** 直传不读默认指针。十行不改，就还去 `minio`。
4. **这个字段多余，删了更灵活。** 删了进程起不来。留着，是为了不让浏览器指定桶，并允许不同策略指向不同名牌。
5. **改 yml 会把旧文件搬到新桶。** 不会。它只影响新 init 冻结下来的 `service`。
6. **`preview: 5m` 会让图片预览签名变成 5 分钟。** 现在的访问 URL 走的是无名的 `download-ttl`，2 分钟。`preview` 要有代码把这个名字传进 `presignDownload`。
7. **`cleanup-dry-run: true` 就是迁移预检。** 不是。它只约束直传过期票的定时清理，而且在 `cleanup-enabled: false` 时根本不会跑。
8. **富文本权限写在 yml 里，所以接口不再要 `system:oss:upload`。** 控制器注解仍要这颗权限，yml 是附加的第二道。

## 非评分暂停

用自己的话指一下，不必交卷：一段新的 `image` 上传，`minio` 这个字符串先后出现在 yml、Redis 票和 `sys_oss.service` 的哪一次；如果明天把名牌改名为 `photos` 但 yml 不动，失败发生在找客户端之前还是 PUT 到 MinIO 之后。

## 总结、词汇表与下一步

第 82–218 行是两份只读规则。`oss.lifecycle` 规定私有下载签名的时长，眼下实际用到的是 2 分钟；`preview` 和 `extended-preview` 已配置，但要有代码按名字调用。`oss.direct-upload` 规定票、预签名和十条上传策略。文件不被运行时写回。被写的是 Redis、对象存储和 `sys_oss.service`。

`storage-config-key` 必须写。它把公开的策略名钉到一张服务器才知道的名牌上。现在十行都钉在 `minio`。换同一张名牌的地址不用改它；换一张新名牌接收新上传时，要改对应行并重启。旧对象不自动跟随。默认配置开关代替不了这个字段。

下一步若要看名牌保存和 readiness，回到 L-025 和 L-086；若要看 init 到 complete 的失败回滚，回到 L-027。本课不布置作业，也不表示这些目标已经掌握。

| 词 | 在这段里指什么 |
| --- | --- |
| upload policy | `general`、`image`、`document` 这类名字 |
| storage-config-key | 名牌的 `config_key`，当前字面量是 `minio` |
| object-prefix | 桶内目录，如 `direct/image` |
| download policy | `preview` 这种下载时长名字，不是上传策略 |
| ticket | Redis 里的上传会话，不是 yml |
| expected-access-policy | 要求名牌必须是 PRIVATE 或 PUBLIC_READ |

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-L087-01 | 工作树 | 第 82–218 行的字面值 | `backend/wta-admin/src/main/resources/application.yml` | 2026-09-22 |
| S-L087-02 | 工作树 | 直传字段校验、`requirePolicy`、模式选择 | `OssUploadProperties.java` | 2026-09-22 |
| S-L087-03 | 工作树 | init 读取策略、权限第二道、TTL、readiness 与 configKey | `OssUploadService.java`、`DefaultOssUploadIdentityResolver.java`、`SysOssUploadController.java` | 2026-09-22 |
| S-L087-04 | 工作树 | 对象键拼接与 `OssFactory.instance(configKey)` | `DefaultOssUploadObjectStore.java`、`AbstractOssClientImpl.buildPathKey` | 2026-09-22 |
| S-L087-05 | 工作树 | 下载 TTL、命名下载策略、只有 `download-ttl` 进入 Nacos 精确键 | `OssLifecycleProperties.java`、`OssLifecycleManager.java` | 2026-09-22 |
| S-L087-06 | 工作树 | 直传清理开关与 dry-run 不删对象 | `OssUploadCleanupTask.java`、`OssUploadService.cleanupExpired` | 2026-09-22 |
| S-L087-07 | 工作树 | readiness 把每条启用策略的 `storage-config-key` 列入必检 | `OssStorageReadinessService.requiredConfigs` | 2026-09-22 |

未在本课连上运行中的 Nacos，不能证明你的环境此刻有没有用配置中心覆盖 `download-ttl`。生产调用方检索没有发现传入 `preview` 或 `extended-preview` 的 `presignDownload`；若以后有人加上，这两档才会生效。
