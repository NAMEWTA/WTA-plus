---
artifact: review
change: 2026-09-08-richtext-third-oss
scope: specdev/diagnose-bugs
---

# D-diagnose-bugs Review

## 结论

三个用户症状均已被 change 覆盖，T-01/T-02/T-03 的行为目标与 AC-001/002/003 对应。当前工件可作为实现输入，但原诊断的在线复现证据不足，且部分路径与源码调用链不完整；已在 Ticket T-02 中补齐。

## 必须修正的事实

1. `diagnosis.md` 的红灯命令只是静态 grep 断言，不能证明三个 HTTP/浏览器症状已运行复现。实现前必须补充后端契约红测，或提供登录态/MinIO 的结构化 HITL 捕获；不能把 grep 输出当作在线复现证据。
2. T-02 的实际链路是 Controller → `ThirdObservabilityUseCaseImpl` → Service → DAO → Mapper。空值动态 SQL 需要 XML 或 `@SelectProvider`，不能只把 `@RequestParam` 改成可选。
3. Spec 原引用的 `ADR.md`、`CONTEXT.md` 不存在，已删除悬空引用。

## Ticket 完整解决方案

- T-01：服务端统一校验 user/client，上层返回可识别业务错误；页面对 load/save/remove 统一捕获；后端单测覆盖缺失上下文和正常归属，浏览器 smoke 覆盖未处理 rejection。
- T-02：参数可选并在 use case/service 统一归一；两个 Mapper 用动态 SQL 支持无过滤和精确过滤；契约测试覆盖 null/空白/有值及 200 响应，Playwright 覆盖页面直开。
- T-03：仅开发 compose 默认放宽 CORS；`.env.example` 与排障文档明确生产必须稳定 HTTPS `domainUrl` + 显式白名单，并以 compose config 和真实 OPTIONS/PUT 预检验收。

## 状态建议

Ticket 目前已具备实现所需决策，但 Map 的 Ready 列与 Ticket frontmatter 的 `status: draft` 不一致；进入 I-implement 前应统一为 `status: ready`，并在每票写入 owner 与实际 Evidence。
