# OSS 公共私有配置、外部调用与 Service 使用

先说结论：这次改动确实改变了 OSS 的访问判断、部分 HTTP 合同、数据库约束和启动检查，但没有把对象存储换成另一套服务，也没有让应用自动创建 Bucket 或修改 Bucket Policy。

公共桶表示“拿到对象 URL 的任何人都能读取”，不是“任何人都能上传”。私有桶表示“原始 URL 不能匿名读取，业务系统授权后才返回有到期时间的签名 URL”。业务数据库仍然只保存 `ossId`，不保存长期 URL。

## 先看全图

```text
初始化与运行

[数据库基础脚本]
        |
        v
[NAMEWTA OSS DDL/DML] ---> [所有旧配置先变成 PRIVATE]
        |
        v
[准备私有 Bucket]     [准备公共只读 Bucket]
        |                       |
        +-----------+-----------+
                    v
          [配置 sys_oss_config]
                    |
                    v
      [配置上传策略和诊断对象]
                    |
                    v
       [应用启动并做只读健康检查]
                    |
          +---------+---------+
          |                   |
          v                   v
 [私有对象返回签名 URL]  [公共对象返回稳定 URL]
 [URL 有 expiresAt]      [expiresAt 为空]
```

业务代码的调用关系如下：

```text
[业务 Controller / Service]
          |
          +-- 先检查这条业务数据能不能看
          |
          v
 [ruoyi-api 的 OssService]
          |
          v
 [system 模块中的实现]
          |
          +-- 根据 sys_oss.service 找到真实配置
          +-- 检查该配置当前是否可服务
          +-- 判断 PRIVATE 或 PUBLIC_READ
          |
          v
 [返回 URL、访问类型、到期时间和文件名]
```

这里最重要的一条规则是：知道 `ossId` 不等于拥有文件权限。业务模块必须先完成自己的权限检查，再调用 `OssService`。

## 一步一步看

### 第一步：外部服务调用发生了哪些变化

#### 1. 对象存储 Provider 会收到新的只读诊断请求

应用启动、OSS 配置变化以及之后每个刷新周期，都会检查实际 Bucket 是否符合声明的访问类型。默认刷新周期是 1 分钟，快照最多保留 5 分钟。

```text
[应用中的 readiness]
          |
          +-- 带凭据 HEAD 诊断对象
          +-- 读取 Bucket Policy
          +-- 读取 Bucket ACL
          +-- 不带凭据 HEAD 诊断对象
          +-- 不带凭据 Range GET 1 字节
          |
          v
 [判断匿名读是否符合 PRIVATE / PUBLIC_READ]
          |
          +-- 符合并且没有公共写权限 --> SERVING
          |
          +-- 不符合、超时或无法确认 --> NOT_SERVING
```

这些调用只读取状态。应用不会：

- 创建或删除 Bucket；
- 修改 Bucket Policy；
- 为诊断自动创建公共对象；
- 用一次真实匿名写入来“试试看”。

匿名写是否被允许由 Policy 和 ACL 的只读结果判断，生产发布前仍要由运维人员独立验证匿名 `PUT/POST/DELETE` 被拒绝。

因此，Provider 凭据除原有对象上传、下载和分片能力外，还要允许读取诊断对象、Bucket Policy 和 Bucket ACL。Provider 不支持这些只读检查时，对应 configKey 会保持 `UNVERIFIED/NOT_SERVING`，不会猜测为安全。

#### 2. Java `OssService` 增加了统一访问方法

公开给其他业务模块的接口仍是 `org.dromara.system.api.OssService`。本次新增：

| 方法 | 现在的用途 |
| --- | --- |
| `resolveAccessUrl(ossId)` | 推荐的新入口。公共对象返回稳定 URL，私有对象返回默认短时签名 URL。 |
| `presignDownload(ossId, policyName)` | 只给私有对象使用，并由服务端命名策略选择时长。 |
| `OssAccessUrl` | 同时返回 `accessType`、`url`、可空的 `expiresAt` 和 `fileName`。 |

旧方法没有删除：

| 旧方法 | 兼容情况与注意点 |
| --- | --- |
| `selectUrlByIds(String)` | 保留，但现在会逐个执行公共/私有统一解析。它只返回逗号字符串，会丢失类型和到期时间，新代码不优先使用。 |
| `selectByIds(String)` | 保留，返回的 URL 也改为按对象实际类型解析。批量私有对象会批量生成短时 URL。 |
| `presignDownload(ossId)` | 保留，默认私有签名时长为 2 分钟；公共对象调用它会明确失败。 |
| `reconcileReferences(...)` | 保留，用于业务保存后维护真实引用。 |
| `snapshot(ossId)` | 保留，用于查看临时状态、到期时间和业务引用。 |

#### 3. HTTP 合同有几处可观察变化

| HTTP 入口 | 变化 |
| --- | --- |
| `GET /resource/oss/{ossId}/download-url` | 仍需 `system:oss:download`，响应由单纯私有签名改为 `OssAccessUrl`；公共返回 `accessType=PUBLIC, expiresAt=null`，私有返回 `accessType=PRIVATE` 和到期时间。 |
| `POST /resource/oss/config/edit` | OSS 配置修改由旧 `PUT` 改为 `POST`。 |
| `POST /resource/oss/config/remove/{ids}` | OSS 配置删除由旧 `DELETE` 改为 `POST`。 |
| `POST /resource/oss/config/changeStatus` | 默认配置切换由旧 `PUT` 改为 `POST`。 |
| `/resource/oss/migrations/**` | 新增迁移预检、启动、查看、重试、回滚和单独批准的源清理接口。 |

浏览器直传初始化仍只允许提交：上传策略名、文件名、文件大小、内容类型和文件指纹。客户端没有新增 configKey、Bucket、访问类型或 TTL 字段。

系统也没有新增“任何人拿 ossId 就能查 URL”的匿名接口。公共 URL 一旦由合法业务页面返回，任何拿到该 URL 的人都能读取；但“哪个公开内容应该返回哪个 URL”仍由门户业务决定。

### 第二步：数据库配置发生了什么变化

`sys_oss_config.access_policy` 现在只有两个合法值：

```text
0 ---> PRIVATE     ---> 原始 URL 匿名读取必须失败
2 ---> PUBLIC_READ ---> 稳定 URL 允许匿名 GET / HEAD
```

旧的 `1=public`、custom 和未知值不再被接受。NAMEWTA DML 会把所有历史值先保守回填成 `0=PRIVATE`，不会因为升级而自动公开历史对象。

同时增加两个迁移审计表：

- `sys_oss_migration_batch`：记录一次迁移批次；
- `sys_oss_migration_item`：记录每个 ossId 的复制、校验、切换、重试和清理阶段。

OSS 配置还增加了以下运行时保护：

- 必须且只能有一个 `status=Y` 的默认配置；
- 默认配置必须是 `PRIVATE`；
- `PUBLIC_READ` 不能切成默认配置；
- 已经被 `sys_oss.service` 引用的配置，不能通过普通编辑改变 configKey、Bucket 或访问类型；
- 已被对象引用的配置不能删除，只能通过受审计迁移改变对象归属。

### 第三步：初始化项目后按什么顺序配置

#### 1. 先正确初始化数据库

MySQL 新环境的顺序是：

```text
[导入 script/sql/ry_vue.sql]
              |
              v
[执行 script/sql/namewta/DDL.sql]
              |
              v
[执行 script/sql/namewta/DML.sql]
              |
              v
[检查恰好一个默认 OSS 配置，且 access_policy=0]
```

不能只导入 `ry_vue.sql`。基础脚本里的历史 OSS 示例仍使用旧编码 `1`，NAMEWTA DDL/DML 才会收紧字段并把旧值安全回填为 `PRIVATE`。

升级已有数据库前先备份 `sys_oss_config`、`sys_oss`、`sys_oss_ref` 和迁移表。DDL 块不是可随意重复执行的脚本，生产执行仍需单独 DBA 批准。

#### 2. 在 Provider 侧准备两个物理 Bucket

推荐最小布局：

```text
[private-files]
  +-- 禁止匿名 GET / HEAD
  +-- 禁止匿名写
  +-- 放置一个私有诊断对象

[portal-public-files]
  +-- 只允许匿名 GetObject
  +-- 不允许匿名 ListBucket
  +-- 禁止匿名写和删除
  +-- 放置一个公共诊断对象
```

如果浏览器要直传，还要按当前前端来源配置 CORS，并允许所需的 `PUT/HEAD`；分片上传要暴露 `ETag` 并配置未完成分片的生命周期清理。公共 Bucket 的 DNS、TLS、CDN 和 `domainUrl` 由部署系统准备，应用不会代办。

#### 3. 配置 `sys_oss_config`

可以通过“系统管理 -> OSS 配置”页面完成。至少准备：

| 配置 | PRIVATE 示例 | PUBLIC_READ 示例 |
| --- | --- | --- |
| configKey | `minio` | `portal-public` |
| bucketName | `private-files` | `portal-public-files` |
| endpoint | Provider 的 S3 endpoint | Provider 的 S3 endpoint |
| domainUrl | 可按私有 Provider 需要配置 | 生产必须是可公开访问的域名 |
| accessPolicy | `PRIVATE`，数据库值 `0` | `PUBLIC_READ`，数据库值 `2` |
| status | `Y`，唯一默认配置 | `N`，公共配置不能默认 |

AccessKey 和 SecretKey 只能通过受控配置入口或密钥系统管理，不要写进本文档、日志或代码仓库。

#### 4. 在现有 `oss:` 配置块中加入 readiness 和上传路由

下面是示意值。要合并进现有 `oss:` 块，不要在同一个 YAML 文件重复创建第二个顶层 `oss:`：

```yaml
oss:
  readiness:
    diagnostic-timeout: 3s
    refresh-interval: 1m
    max-snapshot-age: 5m
    allow-endpoint-domain-fallback: false
    diagnostic-objects:
      minio: diagnostics/private-ready.txt
      portal-public: diagnostics/public-ready.txt

  lifecycle:
    download-ttl: 2m
    download-ttl-min: 1m
    download-ttl-max: 10m
    download-policies:
      preview:
        ttl: 5m
      extended-preview:
        ttl: 10m

  direct-upload:
    policies:
      portal-public:
        storage-config-key: portal-public
        expected-access-policy: PUBLIC_READ
        max-size: 20971520
        allowed-content-types: [image/jpeg, image/png, image/webp]
        object-prefix: portal/public
        mode: AUTO
        multipart-threshold: 10485760
        part-size: 5242880
        required-permission: system:oss:upload
```

每个真正会被使用的 configKey 都要有一个已存在的诊断对象，包括：默认配置、启用的上传策略、历史 `sys_oss.service` 和活动迁移涉及的配置。缺少诊断对象时应用仍可能启动，但该配置会是 `NOT_SERVING`，上传、访问 URL 和迁移会失败关闭。

当前已有的 `general`、`avatar`、`image`、`editor-image`、`editor-video`、`document` 策略都默认指向 `minio + PRIVATE`。只有显式新增并使用类似 `portal-public` 的服务端策略，新上传对象才会进入公共 Bucket。

下载 TTL 的有效范围是 1 到 10 分钟。客户端不能传 TTL；只能由后端选择默认 2 分钟、`preview` 5 分钟或 `extended-preview` 10 分钟等命名策略。

主动清理默认仍是关闭并处于 dry-run。正式启用 TEMP 清理、上传会话清理或迁移源清理，需要独立审核，不属于项目初始化的自动动作。

#### 5. 启动后检查 readiness

```text
[启动应用]
      |
      v
[加载所有 sys_oss_config 到缓存]
      |
      v
[立即执行一次 Provider 只读诊断]
      |
      v
[观察 actuator health 的 ossStorageReadiness]
      |
      +-- 所需 configKey 全部 SERVING --> 可以启用对应上传和访问
      |
      +-- 任一所需 configKey NOT_SERVING --> 先修配置或 Provider
```

至少观察两个刷新周期，确认 `checkedAt` 持续推进。不要通过关闭 readiness、放宽类型或删除诊断对象检查来让状态变绿。

### 第四步：业务模块应该怎样调用 `OssService`

#### 1. Maven 只依赖 `ruoyi-api`

```xml
<dependency>
    <groupId>org.dromara</groupId>
    <artifactId>ruoyi-api</artifactId>
</dependency>
```

不要让普通业务模块依赖 `ruoyi-system`，也不要注入 `ISysOssService`、Mapper、Controller 或 system domain。运行时由 `ruoyi-admin` 把 `OssService` 实现装入同一个 Spring 容器。

#### 2. 普通公共/私有访问统一调用 `resolveAccessUrl`

```java
import lombok.RequiredArgsConstructor;
import org.dromara.system.api.OssService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BusinessAttachmentService {

    private final OssService ossService;

    public OssService.OssAccessUrl accessUrl(Long businessId, Long ossId) {
        requireBusinessReadPermission(businessId, ossId);
        return ossService.resolveAccessUrl(ossId);
    }

    private void requireBusinessReadPermission(Long businessId, Long ossId) {
        // 查询真实业务记录，并检查当前用户是否能读取它。
    }
}
```

调用方不需要先查 Bucket 类型：

```text
resolveAccessUrl(ossId)
          |
          +-- 对象属于 PUBLIC_READ
          |       +-- accessType = PUBLIC
          |       +-- url = 稳定公共 URL
          |       +-- expiresAt = null
          |
          +-- 对象属于 PRIVATE
                  +-- accessType = PRIVATE
                  +-- url = 短时签名 URL
                  +-- expiresAt = 实际到期时间
```

门户发布页面可以在确认内容已经公开后返回公共对象的稳定 URL。权限业务则在检查登录用户、角色、数据范围或业务归属后返回私有签名 URL。两者都不应把解析后的 URL重新写回业务表。

#### 3. 明确只接受私有对象时才调用 `presignDownload`

```java
OssService.OssDownloadUrl defaultUrl = ossService.presignDownload(ossId);
OssService.OssDownloadUrl previewUrl = ossService.presignDownload(ossId, "preview");
```

第一个使用默认 2 分钟；第二个使用服务端配置的 `preview` 5 分钟。若 ossId 实际属于公共配置，这两个方法会拒绝，不会偷偷给公共对象再签一次私有 URL。

#### 4. 保存业务 ossId 时同步维护引用

```java
import com.baomidou.dynamic.datasource.annotation.DSTransactional;

@DSTransactional
public void saveBusinessFile(Long businessId,
                             java.util.Collection<Long> oldOssIds,
                             java.util.Collection<Long> newOssIds) {
    saveBusinessRecord(businessId, newOssIds);
    ossService.reconcileReferences(
        "biz_document",
        String.valueOf(businessId),
        oldOssIds,
        newOssIds
    );
}
```

`refType` 必须是真实物理表名，`refId` 必须是真实业务主键。业务写入和引用协调应在同一个动态数据源事务中，避免业务保存成功但 OSS 引用没有登记。

#### 5. 上传应该走哪条路

```text
[普通前端页面]
      |
      +-- FileUpload / ImageUpload 等现有组件
      |
      v
[组件提交服务端 policy 名]
      |
      v
[后端决定 configKey、Bucket、类型和对象 Key]
      |
      v
[浏览器使用签名直接上传到 Provider]
```

普通业务模块的 `OssService` 没有公开“后端上传文件”方法，这是有意的边界。新业务优先复用前端直传组件；自定义前端也应调用 `/resource/oss/uploads` 会话 API，只提交 policy 和文件元数据。

如果确实需要服务器后台生成文件并上传，应先形成明确的跨模块上传合同，不能为了省事让业务模块依赖 `ISysOssService`、`OssFactory` 或 system Mapper。

公共 Bucket 只开放匿名读取，上传控制面仍要求 `system:oss:upload`。当前实现不支持游客匿名上传；若门户需要游客上传，那是另一项需要独立授权、限流和内容安全设计的能力。

### 第五步：常见错误应该怎样判断

```text
[调用 resolveAccessUrl 失败]
          |
          +-- STORAGE_NOT_SERVING
          |       +-- 查诊断对象、Policy、ACL、domainUrl、checkedAt
          |
          +-- OBJECT_NOT_FOUND / DELETE_PENDING
          |       +-- 查 ossId 和生命周期状态
          |
          +-- ACCESS_POLICY_INVALID
          |       +-- 查 sys_oss.service 对应配置是否只有 0 或 2
          |
          +-- PROVIDER_ACCESS_FAILED
                  +-- 查 endpoint、域名、凭据和 Provider 状态
```

不要把完整签名 URL、AccessKey、SecretKey、Authorization 或 Bucket Policy 正文写进日志。日志只记录 ossId、configKey、阶段、稳定错误码和 traceId。

## 术语小词典

- 文件仓库（对象存储 Provider）：真正保存文件字节的外部服务，例如 MinIO、S3 或云厂商 OSS。
- 文件柜（Bucket）：对象存储中物理隔离的一组文件；本方案用独立公共柜和私有柜。
- 长期公开地址（稳定公共 URL）：没有签名到期参数，任何拿到地址的人都能读取公共对象。
- 临时通行证（签名 URL）：只在指定时间内有效的私有对象访问地址。
- 访问类型（Access Policy）：本项目只允许 `PRIVATE` 和 `PUBLIC_READ`。
- 配置代号（configKey）：连接某个 Provider、Bucket 和访问类型的一组配置名称，例如 `minio`。
- 文件编号（ossId）：业务系统长期保存的稳定编号；不是 URL，也不是访问权限。
- 存储归属（`sys_oss.service`）：某个 ossId 实际属于哪个 configKey，是访问判断的唯一来源。
- 服务端上传规则（upload policy）：后端预先定义的上传用途、大小、类型、Bucket 路由和权限规则。
- 健康门卫（readiness）：定期只读检查 Provider 是否真的符合声明，失败时阻止上传、URL 解析和迁移。
- 诊断对象（diagnostic object）：部署方预先放进 Bucket 的小文件，供 readiness 验证匿名读边界。
- 到期时间（TTL）：签名或票据还能使用多久；由服务端配置，不由客户端选择。
- 业务引用（`sys_oss_ref`）：记录哪张真实业务表的哪条数据正在使用某个 ossId，负责生命周期保护，不负责授权。
- 物理迁移：复制对象、校验、原子切换 `sys_oss.service`，而不是直接编辑一个已有配置的 Bucket 类型。

## 你现在能复述什么

1. 这次外部变化不只是多一个字段：应用会定期只读检查 Provider，下载接口会区分公共稳定 URL 和私有短时 URL，配置命令和迁移 API 也有变化。
2. 应用不会创建 Bucket 或修改 Policy；公共/私有 Bucket、公共只读 Policy、诊断对象、域名和 CORS 都要由部署方准备。
3. 初始化不能只导入 `ry_vue.sql`，还要依次执行 NAMEWTA DDL/DML，让旧访问类型全部先收敛为 PRIVATE。
4. 数据库必须恰好有一个 PRIVATE 默认配置；公共配置只能是非默认，并使用 `2=PUBLIC_READ`。
5. 每个实际使用的 configKey 都要配置诊断对象，否则它会是 NOT_SERVING，上传和 URL 解析不会继续。
6. 新业务模块只依赖 `ruoyi-api` 并注入 `OssService`；普通访问优先调用 `resolveAccessUrl`。
7. 调用 OSS 前必须先检查真实业务权限；知道 ossId 本身不等于有权下载。
8. 业务表保存 ossId，不保存解析后的 URL；保存业务数据时用 `reconcileReferences` 同步维护 OSS 引用。
9. 公共桶允许任何拿到 URL 的人读取，但不支持匿名上传；上传仍由受权限保护的服务端 policy 控制。

事实依据：最终 backend `main@7ea0de75e17483040411a136ca1199dbb76b6d8b`、frontend `main@338c6b08dc6918b33248180bdfe3eb826099a901`、`<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/system/api/OssService.java</Path>`、`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/</Path>`、`<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DDL.sql</Path>`、`<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DML.sql</Path>` 与 `<Path>docs/oss-public-private-operations.md</Path>`。
