# ADR-0002: OSS 控制面与数据面分离

- **Status:** Accepted
- **Date:** 2026-08-23
- **Source:** `2026-08-21-oss-direct-unified-notification` ADR-002

## Context

后端代理浏览器文件上传和下载会占用应用带宽，并可能把完整对象载入 JVM 内存。

## Decision

浏览器使用短时预签名请求直接与 OSS 交换字节。后端只负责认证授权、对象 Key、签名、UploadTicket、完成校验、`sys_oss` 元数据和业务访问控制；业务只持久化 `ossId`。管理下载与普通业务下载分别执行管理权限和业务对象授权，知道 `ossId` 不代表有访问权。

## Consequences

前后端协议必须配对发布，Bucket CORS/ETag/Lifecycle 必须在目标环境配置和验收。`sys_oss` 是全局元数据，但不等于公开资源。
