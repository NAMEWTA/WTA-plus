# ADR-0003: Redis UploadTicket 与双层清理

- **Status:** Accepted
- **Date:** 2026-08-23
- **Source:** `2026-08-21-oss-direct-unified-notification` ADR-003

## Context

浏览器 Multipart 上传需要保存 uploadId、对象 Key、Part 和恢复信息，但这些临时状态不应进入 `sys_oss`。

## Decision

SINGLE 与 MULTIPART 上传均由 Redis UploadTicket 管理，默认续传窗口 24 小时；Part URL 短时签名并按并发窗口申请。重新选择相同文件时以指纹和 ListParts 续传。应用按过期索引主动 Abort，Bucket Lifecycle 只作兜底；应用诊断但不修改 Bucket policy。

## Consequences

Redis Ticket 丢失后会话不可自动恢复。完成操作和清理必须幂等；完整性基线使用 HTTPS、Part ETag、Complete 与最终 HEAD size，Multipart ETag 不当作整文件 MD5。
