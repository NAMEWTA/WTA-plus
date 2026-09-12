---
schema_version: 1
artifact: source
change: 2026-09-08-richtext-third-oss
source_type: conversation
canonical_locator: null
captured_at: 2026-09-08T11:28:11+08:00
content_sha256: 97c549b4c9e24a5327b4ca9ff91ca3eed823c8e9188d09d7872408e0d00656e8
remote_state: open
close_capability: not-applicable
---

# Source

## Capture Metadata

- 入口：用户对话
- 范围：富文本保存、三方调用明细/统计、OSS 直传 CORS/IP 问题
- 冻结方式：用户原始报障要点归一化为三条可核验症状

## Original Content

1. 测试菜单-富文本演示-点击新建并保存，/dev-api/demo/rich-text/list 返回 500，前端报 TransportError。
2. 三方接口调用明细 / 调用统计页面进入即 500，/dev-api/third/invocation/list 和 /dev-api/third/statistics/list 返回 500。
3. OSS 上传时预签名 PUT 被 MinIO CORS 拦截，希望不要再绑定 IP，能否有更简单的配置方式。

## Source Comments

- 这三个症状可以拆成两个实现票和一个运维票。
- 富文本和三方问题都表现为后端契约与前端默认行为不一致。
- OSS 问题核心是 Origin / 公共域名策略，不是密钥缺失。
