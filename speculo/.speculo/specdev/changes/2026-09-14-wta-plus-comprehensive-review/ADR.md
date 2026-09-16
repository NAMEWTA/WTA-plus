# ADR 提案集：WTA-plus 全面审查整改

> 以下决策均为 `Proposed`。本文件记录取舍，不能授予实现、提交、发布或永久知识写入权限。

## ADR-CR-001：SSO bearer 使用 CSPRNG 与安全会话属性

- **Status:** Proposed
- **Context:** 授权 code 和 SSO session 当前使用 ThreadLocalRandom/雪花ID拼接；Cookie 需要生产 Secure，Origin/CORS 需要与独立 SSO Origin 一起裁决。
- **Decision:** 生产 bearer/code 使用至少32字节CSPRNG opaque值；保留PKCE、精确redirect、单次consume和TTL；生产Cookie Secure/HttpOnly/SameSite与受信Origin显式化，旧会话处理策略单独批准。
- **Rejected:** 把数据库ID、前端private key、通配credentialed CORS当安全边界。
- **Migration/Verification:** <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/06-secure-sso-session.md</Path>；黑盒不可预测、属性、跨Origin/失败回调矩阵。

## ADR-CR-002：统一日志副本脱敏

- **Status:** Proposed
- **Context:** HTTP query 与 OperLog response 由不同 module 策略持久化，SSO access_token可进入OperLog。
- **Decision:** 一个日志策略 module 处理 query/form/header/JSON/response 副本；凭据、code、verifier、Location等上下文敏感值失败关闭；保留元数据和非敏感字段。
- **Rejected:** 关闭所有日志或恢复原值排障例外。
- **Migration/Verification:** T-02，canary不得进入HTTP sink、OperLog和DB；历史日志轮换另批。

## ADR-CR-003：可信代理来源IP

- **Status:** Proposed
- **Context:** 安全工具优先读取X-Forwarded-For，Nginx追加外来链，Client白名单/限流依赖结果。
- **Decision:** 以socket peer和配置CIDR判定可信代理，逐hop解析；直连仅用peer；白名单、限流和审计复用唯一结果。
- **Rejected:** 任意客户端header或所有私网默认可信。
- **Migration/Verification:** T-04；需生产代理CIDR和双代理隔离验证。

## ADR-CR-004：浏览器不承载共享响应私钥

- **Status:** Proposed
- **Context:** Vite将响应private key注入Admin/Home bundle，adapter使用AES-ECB。
- **Decision:** 推荐TLS-only浏览器传输并删除共享响应privateKey/ECB；保留现有Sa-Token目标Client合同，Cookie会话是另需CSRF/OAuth评审的可选改造。只有明确威胁模型和服务端密钥生命周期时才另立AEAD协议。
- **Rejected:** 将静态bundle秘密当机密或以双协议无限兼容收尾。
- **Migration/Verification:** T-11，生产bundle secret/ECB负向扫描及登录错误回归。

## ADR-CR-005：显式构建/发布清单与原子产物

- **Status:** Proposed
- **Context:** 目录自动发现、dev覆写production、core/full断言漂移、partial混源和CI/Compose镜像漂移。
- **Decision:** App、bundle、外部镜像和artifact source/digest由显式manifest拥有；完整产物先stage全验再单次promotion；局部build不可成为deployable current。
- **Rejected:** 复制更多current目录、改名或扩大bundle掩盖来源缺失。
- **Migration/Verification:** T-08/T-09/T-10；失败注入与before/after digest。

## ADR-CR-006：事实与门禁单一权威

- **Status:** Proposed
- **Context:** CI文件缺失、旧submodule脚本、Profile数字/拓扑过时、41份generic AGENTS和SpecDev旧cwd。
- **Decision:** project profile/module map与可执行manifest从源码生成/验证；README仅导航；generic AGENTS按owner合并/删除；门禁记录真实命令，远程 required 需Actions/branch protection证据。
- **Rejected:** 以不存在文件、缓存目录存在或旧目录命名作为通过条件。
- **Migration/Verification:** T-01/T-29；clean clone、link/cwd/inventory复核。

## ADR-CR-007：会话、导航、上传与流程状态的幂等生命周期

- **Status:** Proposed
- **Context:** logout恢复、空roles哨兵、旧task提交、上传remove/object URL等状态由调用者散落管理。
- **Decision:** 每个module拥有generation/owner/teardown seam；UI隐藏不替代服务端授权；引用移除与物理删除分开。
- **Rejected:** 复制管理端页面、以roles空值当初始化状态、旧响应覆盖新状态。
- **Migration/Verification:** T-12/T-14/T-16/T-18/T-19/T-21。

## ADR-CR-008：无兼容升级的公开合同删除门槛

- **Status:** Proposed
- **Context:** 用户要求无兼容升级；Profile旧接口、CRUD PUT/DELETE、Demo TODO和strict豁免均涉及潜在删除。
- **Decision:** 目标可无兼容，但每个公开 Java/HTTP/SQL合同先经过调用方清零、替代稳定、版本/迁移/发布授权和回归证据；没有证据保持draft，不把@Deprecated当已删除。
- **Rejected:** 全仓立即删除、双协议长期兼容、删除测试或放宽规则。
- **Migration/Verification:** T-15/T-20/T-26/T-27；java-api-compatibility及完整消费者扫描。

## ADR-CR-009：通知与第三方并发状态持久化

- **Status:** Proposed
- **Context:** Notify三表写回非原子、callback内存seenEvents、local wake可能同步发送；Third semaphore无租约，动态限额需实测。
- **Decision:** provider I/O在事务外；结果/Attempt/Outbox/receipt短事务和唯一身份；lease owner/token/TTL；本地wake仅有界或删除，Redis wake+poll为恢复通道。
- **Rejected:** 用application event伪装跨进程保证、无界executor、exactly-once无证据承诺。
- **Migration/Verification:** T-22/T-23/T-24/T-28/T-31，真实MySQL/Redis/Provider kill/rollback矩阵；Profile对QUEUED与短信投递确认的合同必须一致，跨Redis challenge/DB transfer不虚称原子事务。
