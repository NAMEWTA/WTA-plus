# OSS 登录与浏览器直传排障手册

本文总结 NAMEWTA 管理端登录、OSS 浏览器直传和本次 MinIO CORS 故障的处理要点。文档只记录拓扑、配置键、协议和验证方法，不记录任何服务器、数据库、Redis 或 MinIO 密码。

## 1. 先理解两条链路

登录和文件上传是两条不同的链路。登录成功只说明浏览器拿到了业务会话，不代表浏览器一定可以直接访问对象存储。

```text
浏览器
  |
  | 1. /auth/client/context、/auth/code、/auth/login
  |    Authorization: Bearer <业务 Token>
  |    clientid: <Client 字符串标识>
  v
本地 Vite -> 后端 Spring Boot -> MySQL / Redis
                              |
                              | 2. /resource/oss/uploads
                              |    校验用户、Client、权限、策略和文件元数据
                              v
                         预签名 PUT
                              |
                              | 3. 浏览器按签名请求直接 PUT
                              |    到 MinIO，必须通过 Bucket CORS 预检
                              v
                            MinIO
                              |
                              | 4. /resource/oss/uploads/{token}/complete
                              v
                    后端校验对象并写入 OSS 元数据
```

### 1.1 登录控制面

- 前端 API 基址通常为 `VITE_APP_BASE_API=/dev-api`，Vite 将它代理到后端，例如 `http://localhost:18080`。
- 登录前会读取 `/auth/client/context`，再按后端返回决定是否读取 `/auth/code` 验证码。
- 密码登录请求为 `/auth/login`，请求体包含 `clientId` 字符串标识和 `grantType=password`。
- 登录后的业务请求同时携带 `Authorization: Bearer <Token>` 和 `clientid: <Client 字符串标识>`。
- 浏览器会话存储使用 `Admin-Token`。后端或前端重启后，如果会话失效，先重新登录再判断业务接口是否正常。

`clientId` 和 `clientPk` 不能混用：

| 名称 | 类型 | 用途 |
|---|---|---|
| `clientId` | OAuth 风格字符串 | 登录 body 和 `clientid` header，标识当前客户端入口 |
| `clientPk` | `sys_client.id` Long | Token extra、角色、菜单和数据权限隔离 |

缺少或混淆 Client 上下文时，服务端应拒绝请求，不能回退到全局用户、角色或菜单。

### 1.2 OSS 数据面

1. 浏览器调用 `POST /resource/oss/uploads`，提交命名策略、文件名、大小、Content-Type 和文件指纹。
2. 后端校验登录身份、`system:oss:upload` 权限、当前 Client、存储 readiness、访问策略、文件大小、扩展名和允许的 Content-Type。
3. 后端返回短时 `presignedRequest`。浏览器使用其中的 HTTP 方法、URL 和 `requiredHeaders` 直接访问 MinIO。
4. 单文件上传完成后，浏览器调用 `POST /resource/oss/uploads/{uploadToken}/complete`；Multipart 还要先调用 Part 签名接口，并从 OSS 响应读取 `ETag`。
5. 上传取消或失败时，前端会调用 `DELETE /resource/oss/uploads/{uploadToken}`，后端尝试终止 Multipart 并清理临时对象。

预签名 URL 是一次性的短期授权材料，不应复制到工单、日志或聊天记录中。MinIO 不需要也不应该接收业务 `Authorization` Token；它只校验签名 URL 和签名中要求的请求头。

## 2. 当前项目的有效配置

### 2.1 前端开发配置

管理端当前使用以下配置关系：

```dotenv
VITE_APP_PORT=5177
VITE_APP_BASE_API=/dev-api
VITE_APP_PROXY_TARGET=http://localhost:18080
VITE_APP_CLIENT_ID=<公开的 Client 字符串标识>
```

注意：

- `Origin` 是页面的完整来源，协议、主机和端口都参与匹配。`http://localhost:5177`、`http://localhost:5175` 和 `http://127.0.0.1:5177` 是三个不同 Origin。
- `VITE_*` 会进入浏览器构建产物，全部视为公开配置。不得把 MinIO root 密码、数据库密码、Redis 密码或其他 Secret 放入其中。
- RSA 配置如果以 `VITE_*` 形式存在，也不能当作浏览器端秘密；需要真正保密的私钥必须移到服务端。
- 不要用 `file://` 打开页面测试。必须从 Vite 或正式 HTTP 入口访问，否则 Origin 和代理行为都不符合实际部署。

### 2.2 后端直传策略

后端配置位于 `wta-admin/src/main/resources/application.yml`，关键默认值如下：

| 策略 | 存储配置 | 访问策略 | 最大大小 | Multipart 阈值 | 备注 |
|---|---|---:|---:|---:|---|
| `general` | `minio` | `PRIVATE` | 10 MB | 100 MB | 常规图片、文档和文本 |
| `avatar` | `minio` | `PRIVATE` | 10 MB | 10 MB | `SINGLE`，只允许头像图片 |
| `image` | `minio` | `PRIVATE` | 20 MB | 10 MB | 图片可自动切 Multipart |
| `document` | `minio` | `PRIVATE` | 100 MB | 50 MB | 档案和办公文档 |

个人档案页面传入的是 `document` 策略，但页面组件额外限制为最多 10 份、单文件小于 10 MB，并只接受 `png/jpg/jpeg/pdf/doc/docx` 扩展名。前端大小判断使用 `>=`，因此恰好 10 MB 也会被拒绝；测试时使用明显小于 10 MB 的文件。

后端仍会再次校验，不能把前端 `accept`、文件扩展名或页面提示当作安全边界。PNG、JPEG、GIF、PDF、ZIP 等类型还会检查有限的 magic bytes。

### 2.3 MinIO 配置

Compose 中的关键环境变量为：

```dotenv
MINIO_ROOT_USER=<仅服务端使用>
MINIO_ROOT_PASSWORD=<仅服务端使用>
MINIO_ENDPOINT=<后端和浏览器可达的 MinIO 地址>
MINIO_BUCKET=wta
MINIO_API_CORS_ALLOW_ORIGIN=*
```

`MINIO_API_CORS_ALLOW_ORIGIN` 是逗号分隔的 Origin 白名单。当前项目使用 MinIO 的全局 CORS 环境变量；官方文档也说明该配置支持逗号分隔来源，并且环境变量优先于客户端配置。[MinIO CORS Configuration](https://docs.min.io/aistor/administration/cors-configuration/) 和 [MinIO mc admin config](https://docs.min.io/aistor/reference/cli/admin/mc-admin-config/)

白名单要遵循最小权限：

- 只添加真实的前端 Origin，不要写路径，不要加结尾 `/`。
- 开发端口变化时同步更新，尤其区分 `localhost` 和 `127.0.0.1`。
- `*` 仅允许用于本地开发/联调默认值；共享或生产环境必须覆盖为稳定 HTTPS 域名白名单，不能沿用该默认值。
- 需要浏览器读取 Multipart 的 `ETag` 时，CORS 必须暴露 `ETag` 响应头。
- 允许的方法至少包含 `PUT`；浏览器的 `OPTIONS` 预检由 MinIO 根据 CORS 配置响应。
- `Access-Control-Allow-Headers` 必须覆盖签名返回的请求头。当前实现至少会使用 `content-type` 和 `x-amz-meta-upload-fingerprint`，实际以 `presignedRequest.requiredHeaders` 为准。

## 3. 本次故障复盘

### 3.1 根因

浏览器实际从 `http://localhost:5177` 发起预签名 PUT，而现场 MinIO 只允许 `http://localhost:5175` 和 `http://127.0.0.1:5175`。因此：

1. 后端可以正常登录、生成预签名 URL，说明登录控制面和 MinIO 服务端凭据可用。
2. 浏览器向 MinIO 发起跨域 `OPTIONS` 预检时，Origin 不在白名单中。
3. XMLHttpRequest 以浏览器网络错误结束，前端显示“OSS 网络请求失败，请检查 Bucket CORS”。
4. 这不是登录 Token 错误，也不是 MinIO root 密码错误。

### 3.2 本次修复

- 实际 NAMEWTA 部署根目录为 `/data/namewta-plus`，Compose 项目为 `namewta-plus-infrastructure`，MinIO 容器为 `namewta-plus-minio`；不能照搬过时的 `/data/namewta-data` 路径。
- 修改远端受管环境文件中的 `MINIO_API_CORS_ALLOW_ORIGIN`，追加 `http://localhost:5177`。
- 修改前备份环境文件，并只重建 NAMEWTA 的 MinIO 服务；没有触碰同机其他项目容器。
- 重建后用实际浏览器请求头验证了 `OPTIONS` 预检：状态为 `204`，允许来源为 `http://localhost:5177`，允许方法为 `PUT`，允许请求头包含 `content-type,x-amz-meta-upload-fingerprint`。
- 本地后端和前端随后重新启动，管理端页面保持在 `/profile/person`。

### 3.3 Lifecycle 诊断告警

后端启动时可能仍出现：

```text
OSS Direct Upload配置 [minio] CORS/Lifecycle 辅助检查未通过
```

该检查同时读取 CORS 和 Bucket Lifecycle。当前 MinIO 版本通过标准 `PutBucketLifecycle` 不支持 `AbortIncompleteMultipartUpload`，而 `mc ilm rule add --expire-days` 生成的是完整对象过期规则；不能为了消除告警而盲目添加全桶过期策略，否则可能删除业务档案。官方兼容性说明见 [S3 API Compatibility](https://docs.min.io/aistor/developers/s3-api-compatibility/)。

当前应用仍有以下清理路径：

- 用户取消上传时主动调用 abort。
- Redis 中的 UploadTicket 到期后由应用清理任务处理。
- `cleanup-enabled=false`、`cleanup-dry-run=true` 时不会主动执行破坏性清理。

因此，Lifecycle 告警与本次 CORS 失败要分开判断。真正的生产环境应由对象存储负责人单独评估未完成 Multipart 的清理策略，不能直接套用完整对象过期规则。

## 4. 发布或重启顺序

### 4.1 修改 MinIO CORS

1. 先确认目标是 NAMEWTA 的 Compose 项目、服务和持久化目录，不要对同机其他项目执行 `down` 或批量重建。
2. 备份当前环境文件，并保留原权限（通常为 `0600`）。
3. 只修改 `MINIO_API_CORS_ALLOW_ORIGIN`，保留已有 Origin 和其他环境变量。
4. 先执行 Compose 配置校验：

   ```bash
   docker compose --project-name namewta-plus-infrastructure \
     --env-file /data/namewta-plus/namewta-release.env \
     -f /data/namewta-plus/release-current/docker/docker-compose-infrastructure.yml \
     -f /data/namewta-plus/release-current/compose/docker-compose-infrastructure.override.yml \
     config --quiet
   ```

5. 只重建 MinIO：

   ```bash
   docker compose --project-name namewta-plus-infrastructure \
     --env-file /data/namewta-plus/namewta-release.env \
     -f /data/namewta-plus/release-current/docker/docker-compose-infrastructure.yml \
     -f /data/namewta-plus/release-current/compose/docker-compose-infrastructure.override.yml \
     up -d --no-deps minio
   ```

6. 等待 MinIO healthcheck 为 `healthy`，再重启依赖它的后端实例。禁止使用 `docker compose down -v`，它可能删除持久化数据。

### 4.2 本地前后端

```powershell
# 后端，工作目录为 wta-vue-plus-namewta/wta-admin
java -jar target\wta-admin.jar --server.port=18080 --openapi.enabled=false

# 前端，工作目录为 plus-ui-namewta/apps/admin-web
pnpm exec vite serve --mode development --port 5177 --host 0.0.0.0
```

启动后先验证：

```powershell
Invoke-WebRequest -UseBasicParsing http://127.0.0.1:18080/
Invoke-WebRequest -UseBasicParsing http://localhost:5177/
Invoke-WebRequest -UseBasicParsing http://127.0.0.1:18080/actuator/health
```

前两个请求应返回 `200`，健康检查的总状态和 `ossStorageReadiness` 应为 `UP`/`SERVING`。若健康检查未通过，先处理后端 readiness、数据库、Redis 或 MinIO 连接，不要先改前端错误提示。

## 5. 可复现验证清单

### 5.1 CORS 预检

把 `MINIO_HOST:PORT`、Bucket 和对象路径替换为实际值，不要把签名 URL 或凭据写入脚本：

```powershell
curl.exe -i -X OPTIONS "http://MINIO_HOST:PORT/BUCKET/OBJECT_PATH" `
  -H "Origin: http://localhost:5177" `
  -H "Access-Control-Request-Method: PUT" `
  -H "Access-Control-Request-Headers: content-type,x-amz-meta-upload-fingerprint"
```

至少应看到：

```text
HTTP/1.1 204 No Content
Access-Control-Allow-Origin: http://localhost:5177
Access-Control-Allow-Methods: PUT
Access-Control-Allow-Headers: content-type,x-amz-meta-upload-fingerprint
```

如果 `Allow-Origin` 缺失或不是当前页面 Origin，先修 MinIO CORS；如果预检正常而实际 PUT 返回 `403`，再检查签名过期、签名主机、Content-Type 和所有 `requiredHeaders` 是否逐字匹配。

### 5.2 浏览器手工验证

在 `http://localhost:5177/profile/person`：

1. 登录成功后确认个人档案页面能加载材料标签。
2. 添加材料行，先选择材料标签，再选择一个小于 10 MB 的 `png/jpg/jpeg/pdf/doc/docx` 文件。
3. Network 面板应依次看到：
   - `/resource/oss/uploads`：后端 `2xx`，返回 `uploadToken` 和 `presignedRequest`。
   - MinIO 预签名 `PUT`：`2xx`，响应可读取 `ETag`。
   - `/resource/oss/uploads/{token}/complete`：后端 `2xx`，返回 `ossId`。
4. 再提交个人档案，确认材料引用使用 `ossId`，而不是把原始 MinIO URL 写进业务表。
5. 删除或取消上传后，确认前端没有重复提交，后端日志没有记录完整签名 URL。

### 5.3 登录验证

- `/auth/client/context` 和 `/auth/code` 是否返回 `2xx`。
- `/auth/login` 的 body `clientId` 与前端当前 Client 配置一致。
- 登录后的业务请求是否同时携带 Bearer Token 和 `clientid`。
- Token 过期时只出现一次重新登录流程，不要通过刷新页面或重复点击掩盖 `401`。
- 切换 Client 后，旧 Client 的菜单、角色、OSS 上传策略和 UploadTicket 不得继续复用。

## 6. 常见错误定位

| 现象 | 优先检查 | 说明 |
|---|---|---|
| `OSS 网络请求失败，请检查 Bucket CORS...` | 页面 Origin、MinIO CORS、预检方法和请求头 | 浏览器通常把 CORS 拒绝表现为网络错误，未必有 HTTP 状态码 |
| MinIO PUT `403` | 预签名是否过期、主机是否被改写、Content-Type/metadata 是否缺失、系统时钟 | 预检成功不代表签名请求一定正确 |
| MinIO PUT `404` 或 `NoSuchBucket` | endpoint、Bucket 名、反向代理路径 | 浏览器必须能访问签名 URL 中的真实主机和端口 |
| `/resource/oss/uploads` 返回 `STORAGE_NOT_SERVING` | `/actuator/health` 的 `ossStorageReadiness`、MinIO 私有探针、后端 OSS 配置 | 后端会在签名前拒绝未就绪的存储目标 |
| `STORAGE_ACCESS_POLICY_MISMATCH` | upload policy 的 `expected-access-policy` 与 `sys_oss_config.access_policy` | 默认直传目标应为 `PRIVATE` |
| `ACCESS_DENIED` | 当前用户权限 `system:oss:upload`、当前 Client 和策略白名单 | 前端隐藏按钮不能代替后端授权 |
| `INVALID_FILE` | 文件大小、扩展名、Content-Type、magic bytes、文件名 | 前端和后端限制都要满足；文件名不能包含路径或控制字符 |
| `SESSION_EXPIRED` / `SESSION_OWNER_MISMATCH` | UploadTicket TTL、浏览器会话、Client 是否切换 | 不要跨用户、跨 Client 复用 IndexedDB 中的恢复记录 |
| 登录后立即重新登录 | Token、`clientid`、Client 上下文、后端 Redis | 先看第一个 `401` 请求，不要只看最后一条弹窗 |
| 后端日志只有 Lifecycle 告警 | Bucket Lifecycle 诊断和 MinIO 版本兼容性 | 与 CORS 失败分开处理，禁止为消除告警添加全桶过期策略 |

## 7. 安全与运维底线

- MinIO root 用户、root 密码、数据库密码、Redis 密码和 SSH 凭据只放在受保护的服务器环境或凭据管理器中；不进入 Git、浏览器 bundle、前端 `.env`、日志、截图或工单。
- 不记录完整预签名 URL、`X-Amz-Signature`、Authorization header、Cookie、Token 或对象内容。排障时只记录 host、bucket、阶段、HTTP 状态、稳定错误码和 requestId。
- Bucket 默认保持 `PRIVATE`。业务下载通过后端授权后生成短时签名 URL；不能把私有对象改成匿名公共读来绕过上传或下载故障。
- CORS 是浏览器访问控制，不是对象存储授权。CORS 放行不会授予写权限，签名和后端策略仍必须有效；反过来，签名有效也不能绕过浏览器 CORS。
- 远程变更前先做精确备份并核对 Compose 项目、容器名、挂载和持久化路径。只操作授权的 NAMEWTA 资源，不能批量重启同机其他项目。
- 任何完整对象过期、Multipart 清理、Bucket Policy、匿名访问或数据迁移动作，都要单独评审影响范围、观察窗口和回滚办法。

## 8. 交付前最小检查

```text
[ ] 当前页面 Origin 已加入 MinIO CORS，且没有多余通配符
[ ] OPTIONS 预检返回 204（或兼容的 2xx），PUT、签名请求头和 ETag 均符合预期
[ ] 后端 /actuator/health 为 UP，minio readiness 为 SERVING
[ ] 登录请求的 clientId、clientid 和 Token 来自同一 Client
[ ] upload policy 的 Bucket、访问策略、权限、大小和 Content-Type 已核对
[ ] 预签名 PUT 使用原始 method、URL 和 requiredHeaders，没有被代理或代码改写
[ ] complete 成功后业务只保存 ossId，不保存 Secret 或原始签名 URL
[ ] 取消、过期、重复点击和 401 重新登录路径已手工验证
[ ] 远端环境文件已备份，Compose config 校验通过，未执行 down -v
[ ] 日志、截图和文档中没有密码、Token、签名 URL 或对象内容
```
