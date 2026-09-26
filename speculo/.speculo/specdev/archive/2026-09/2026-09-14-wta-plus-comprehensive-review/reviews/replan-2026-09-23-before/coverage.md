# 复核覆盖

| 范围 | 核对内容 | 边界 |
|---|---|---|
| 31张票据及Spec/ADR/Map | 发现、方案、写集、依赖、验收和状态 | 全部重新核对；不是产品实现 |
| backend | 日志/正文/IP/防重、SSO、Notify、Profile转移、Third、Workflow、部门、Demo调用链 | 源码/SQL/检查器证据；未运行Maven及真实服务 |
| frontend | 三App认证、self材料、任务弹窗、iframe、上传、列表、strict、可访问性 | 静态确认；未运行浏览器或类型诊断 |
| 交付/治理 | POM/package、release脚本、Compose、Nginx、SQL owner、AGENTS、清理清单 | 已跑发布合同/文档/分层检查；未部署 |
| 保留项 | PKCE/HMAC、Client、OSS引用、供应商schema、历史证据与许可证 | 未作独立全协议/法律审计 |

confirmed、likely和needs-runtime含义见reviews/re-review.md。目录清单不代表逐行审计；没有为补齐覆盖率虚构新问题。
