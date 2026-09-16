---
schema_version: 3
artifact: spec
change: 2026-09-14-wta-plus-comprehensive-review
status: draft
ready_for_tickets: false
sources:
  - USER-DECISION: 只写新 change，全面审查后由用户亲自审核；不修改代码
  - CODE: <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/</Path>
  - BASELINE: <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/worktree-baseline.json</Path>
---

# Spec：WTA-plus 全面架构、代码、目录、文档与交付审查整改提案

## 1. 问题与目标

当前仓库已经具备前后端 monorepo、分层试点、动态路由、SSO、通知 Outbox、OpenAPI HMAC、OSS 引用生命周期和静态模板等真实能力，但源码、公共 module、发布资产、验证器和文档之间存在可追踪的结构压力与合同漂移。本 Spec 的目标是把已确认/待运行证据转换成可审查的无兼容升级候选：删除浅 interface、重复事实、无 owner 的状态和隐式发布组合；修复认证、日志、数据一致性、树结构及 UI/UX 关键路径；把每一项改造拆成可验收 Ticket。

本 Spec 不是实现授权，也不宣称任何 finding 已修复。当前目标状态需用户确认，尤其是公开 Java API/HTTP/数据库/发布合同的不兼容删除。

### 成功标准

- 每个高置信 finding 都能追溯到源码路径、触发、反证、目标、删除复杂性和验收；未知环境明确 `needs-runtime/not-run`。
- 31 张 Ticket 的依赖无环，写集和 Skill 入口明确，全部保持 draft/ready=false。
- 实施后，安全、Client、事务、资源、类型、路由、构建和发布证据可由命令/隔离服务/浏览器重跑；不通过删除测试或放宽门禁。
- 当前事实只保留在一个可验证 owner；历史、供应商和永久知识按既有网关保留。

### 非目标

- 本轮不编辑 `backend/**`、`frontend/**`、`docs/**`、`release-artifacts/**`、`.agents/**` 或全局 SpecDev status/config。
- 不执行提交、推送、合并、部署、远程 Issue、归档、永久 ADR/context 提升或历史凭据清理。
- 不因 `ruoyi/dromara` 名称、第三方 schema、许可证、placeholder README 或文件超过 1k 行就自动删除。

## 2. 解决方案与外部行为

以“证据冻结 → 安全/数据合同 → 前端/后端纵切片 → 发布与文档收敛 → 整体验收”为顺序。目标行为包括：凭据不进入任何日志 adapter；请求体在认证前有界；SSO bearer 使用 CSPRNG、回调参数 RFC 编码、Cookie/Origin 合同明确；自助认证可以完成材料闭环；流程弹窗只提交当前 task；退出时本地会话和动态路由幂等清理；上传区分引用移除与物理删除；通知回写和 callback 幂等持久化；部门树无环；发布产物同源且原子 promotion。

失败行为一律可观察且失败关闭：缺权限/Client/owner、过期 lease、超预算正文、非法来源、过期 captcha、过期 SSO、坏 artifact、缺模板或未知消息不得静默成功。现有正确的 PKCE、nonce、HMAC、Outbox claim/fence、OSS 引用保护、domain 依赖方向和 MySQL 8.4 owner 必须保留。

## 3. 用户故事与验收合同

- **US-31**：作为企业绑定发起人，我希望合法发码能排队并最终激活确认挑战，失败能恢复。

- **US-01**：作为维护者/调用者/终端用户，我希望“恢复可信的仓库门禁与治理入口”，以便得到可证明且可恢复的行为。
- **US-02**：作为维护者/调用者/终端用户，我希望“消除HTTP与操作日志中的凭据副本”，以便得到可证明且可恢复的行为。
- **US-03**：作为维护者/调用者/终端用户，我希望“统一有界请求体采集与验签缓存”，以便得到可证明且可恢复的行为。
- **US-04**：作为维护者/调用者/终端用户，我希望“建立可信代理来源IP合同”，以便得到可证明且可恢复的行为。
- **US-05**：作为维护者/调用者/终端用户，我希望“修复防重键过期后的所有权竞态”，以便得到可证明且可恢复的行为。
- **US-06**：作为维护者/调用者/终端用户，我希望“强化SSO令牌随机性与Cookie安全”，以便得到可证明且可恢复的行为。
- **US-07**：作为维护者/调用者/终端用户，我希望“修复SSO回调编码与可恢复登录旅程”，以便得到可证明且可恢复的行为。
- **US-08**：作为维护者/调用者/终端用户，我希望“补齐SSO独立Origin发布合同”，以便得到可证明且可恢复的行为。
- **US-09**：作为维护者/调用者/终端用户，我希望“统一App模式、bundle与依赖服务验证矩阵”，以便得到可证明且可恢复的行为。
- **US-10**：作为维护者/调用者/终端用户，我希望“删除混源局部发布并实现原子stage”，以便得到可证明且可恢复的行为。
- **US-11**：作为维护者/调用者/终端用户，我希望“删除浏览器共享私钥与ECB传输包装”，以便得到可证明且可恢复的行为。
- **US-12**：作为维护者/调用者/终端用户，我希望“统一幂等会话清理与导航恢复状态”，以便得到可证明且可恢复的行为。
- **US-13**：作为维护者/调用者/终端用户，我希望“完成Home注册开关与验证码重试交互”，以便得到可证明且可恢复的行为。
- **US-14**：作为维护者/调用者/终端用户，我希望“补齐个人与企业自助认证材料闭环”，以便得到可证明且可恢复的行为。
- **US-15**：作为维护者/调用者/终端用户，我希望“移除Profile过渡接口与测试构造桥”，以便得到可证明且可恢复的行为。
- **US-16**：作为维护者/调用者/终端用户，我希望“保证流程任务读取和办理对象一致”，以便得到可证明且可恢复的行为。
- **US-17**：作为维护者/调用者/终端用户，我希望“收紧流程设计器消息来源”，以便得到可证明且可恢复的行为。
- **US-18**：作为维护者/调用者/终端用户，我希望“明确上传完成、引用移除和导入失败生命周期”，以便得到可证明且可恢复的行为。
- **US-19**：作为维护者/调用者/终端用户，我希望“收敛System大页面中的异步状态与重复封装”，以便得到可证明且可恢复的行为。
- **US-20**：作为维护者/调用者/终端用户，我希望“按边界落实完整TypeScript严格目标”，以便得到可证明且可恢复的行为。
- **US-21**：作为维护者/调用者/终端用户，我希望“统一公开页面的可访问交互基线”，以便得到可证明且可恢复的行为。
- **US-22**：作为维护者/调用者/终端用户，我希望“原子提交通知投递结果与lease fence”，以便得到可证明且可恢复的行为。
- **US-23**：作为维护者/调用者/终端用户，我希望“将供应商回调幂等纳入持久事务”，以便得到可证明且可恢复的行为。
- **US-24**：作为维护者/调用者/终端用户，我希望“恢复Third并发租约并明确限额热更新”，以便得到可证明且可恢复的行为。
- **US-25**：作为维护者/调用者/终端用户，我希望“保证部门移动无环且并发一致”，以便得到可证明且可恢复的行为。
- **US-26**：作为维护者/调用者/终端用户，我希望“按资源合同清除旧CRUD方法并同步客户端”，以便得到可证明且可恢复的行为。
- **US-27**：作为维护者/调用者/终端用户，我希望“修复Demo树样例并删除误导占位实现”，以便得到可证明且可恢复的行为。
- **US-28**：作为维护者/调用者/终端用户，我希望“让通知提交后唤醒保持短路径”，以便得到可证明且可恢复的行为。
- **US-29**：作为维护者/调用者/终端用户，我希望“清理过时文档与重复AGENTS权威”，以便得到可证明且可恢复的行为。
- **US-30**：作为维护者/调用者/终端用户，我希望“完成升级整体验收与可审查交付”，以便得到可证明且可恢复的行为。

| ID | 前置条件 | 动作或事件 | 可观察结果 | 验证接缝 |
|---|---|---|---|---|
| AC-001 | 已接受 T-01 及其上游证据 | 按 Ticket 执行并记录 Evidence | 干净clone不创建temp/release也通过事实检查 | <Path>.agents/skills/engineering-standards/scripts/validate-skill-facts.mjs</Path> |
| AC-002 | 已接受 T-02 及其上游证据 | 按 Ticket 执行并记录 Evidence | canary不出现在HTTP sink、OperLogEvent、数据库或错误日志 | <Path>backend/wta-common/wta-common-log/</Path> |
| AC-003 | 已接受 T-03 及其上游证据 | 按 Ticket 执行并记录 Evidence | 大小边界前/等于/超限一字节结果可判定 | <Path>backend/wta-common/wta-common-web/</Path> |
| AC-004 | 已接受 T-04 及其上游证据 | 按 Ticket 执行并记录 Evidence | 任意外来XFF不改变直连或正常入口的授权结果 | <Path>backend/wta-common/wta-common-core/src/main/java/org/namewta/common/core/utils/ServletUtils.java</Path> |
| AC-005 | 已接受 T-05 及其上游证据 | 按 Ticket 执行并记录 Evidence | A失败不得删除B的键 | <Path>backend/wta-common/wta-common-redis/src/main/java/org/namewta/common/redis/aspectj/RepeatSubmitAspect.java</Path> |
| AC-006 | 已接受 T-06 及其上游证据 | 按 Ticket 执行并记录 Evidence | 生产实现不含ThreadLocalRandom/雪花ID作为bearer | <Path>backend/wta-modules/wta-sso/</Path> |
| AC-007 | 已接受 T-07 及其上游证据 | 按 Ticket 执行并记录 Evidence | 复杂state往返相等且无重复code/state参数 | <Path>backend/wta-modules/wta-sso/src/main/java/org/namewta/sso/service/SsoAuthorizationService.java</Path> |
| AC-008 | 已接受 T-08 及其上游证据 | 按 Ticket 执行并记录 Evidence | 每个shipped App均有且仅有完整配套，缺项在promotion前失败 | <Path>release-artifacts/docker/</Path> |
| AC-009 | 已接受 T-09 及其上游证据 | 按 Ticket 执行并记录 Evidence | build:dev最终三个App均development，build:prod均production | <Path>frontend/package.json</Path> |
| AC-010 | 已接受 T-10 及其上游证据 | 按 Ticket 执行并记录 Evidence | manifest每个artifact的digest和source可追溯 | <Path>release-artifacts/scripts/release-manage.sh</Path> |
| AC-011 | 已接受 T-11 及其上游证据 | 按 Ticket 执行并记录 Evidence | 生产bundle不再携带该共享响应私钥或ECB路径 | <Path>frontend/packages/adapters/crypto-browser/</Path> |
| AC-012 | 已接受 T-12 及其上游证据 | 按 Ticket 执行并记录 Evidence | logout超时/401/离线时本地token和动态路由仍清空 | <Path>frontend/apps/admin-web/src/store/</Path> |
| AC-013 | 已接受 T-13 及其上游证据 | 按 Ticket 执行并记录 Evidence | 服务端关闭注册时入口与页面均准确，后端仍拒绝直接调用 | <Path>frontend/apps/home-web/src/views/RegisterPage.vue</Path> |
| AC-014 | 已接受 T-14 及其上游证据 | 按 Ticket 执行并记录 Evidence | 新个人CN_RESIDENT_ID上传正反面后完成提交 | <Path>frontend/packages/web-domains/profile/src/self/</Path> |
| AC-015 | 已接受 T-15 及其上游证据 | 按 Ticket 执行并记录 Evidence | 旧入口引用为零且替代能力覆盖完整 | <Path>backend/wta-modules/wta-profile/</Path> |
| AC-016 | 已接受 T-16 及其上游证据 | 按 Ticket 执行并记录 Evidence | B失败绝不发出A的审批请求 | <Path>frontend/packages/web-domains/workflow/src/components/ProcessActionDialog.vue</Path> |
| AC-017 | 已接受 T-17 及其上游证据 | 按 Ticket 执行并记录 Evidence | 错误origin、错误source、未知payload不能关闭标签 | <Path>frontend/packages/web-domains/workflow/src/definition/DesignPage.vue</Path> |
| AC-018 | 已接受 T-18 及其上游证据 | 按 Ticket 执行并记录 Evidence | 有业务引用对象的表单移除不先调用物理删除 | <Path>frontend/packages/web-kit/file-upload/</Path> |
| AC-019 | 已接受 T-19 及其上游证据 | 按 Ticket 执行并记录 Evidence | 快速筛选时旧响应不能覆盖新列表，失败/loading可恢复 | <Path>frontend/packages/web-domains/system/src/user/</Path> |
| AC-020 | 已接受 T-20 及其上游证据 | 按 Ticket 执行并记录 Evidence | 目标三个开关为true且全量typecheck通过 | <Path>frontend/tsconfig.json</Path> |
| AC-021 | 已接受 T-21 及其上游证据 | 按 Ticket 执行并记录 Evidence | 只用键盘可完成公开流程，焦点可见且错误能被读屏发现 | <Path>frontend/apps/home-web/src/</Path> |
| AC-022 | 已接受 T-22 及其上游证据 | 按 Ticket 执行并记录 Evidence | 任意一条SQL失败不会留下Delivery/Attempt/Outbox不一致 | <Path>backend/wta-modules/wta-notify/src/</Path> |
| AC-023 | 已接受 T-23 及其上游证据 | 按 Ticket 执行并记录 Evidence | 回滚后相同事件可重试成功 | <Path>backend/wta-modules/wta-notify/</Path> |
| AC-024 | 已接受 T-24 及其上游证据 | 按 Ticket 执行并记录 Evidence | 崩溃后permit在规定上限内恢复，不依赖人工删key | <Path>backend/wta-modules/wta-third/src/</Path> |
| AC-025 | 已接受 T-25 及其上游证据 | 按 Ticket 执行并记录 Evidence | 自父/后代父/并发互移均不能形成环 | <Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/controller/system/SysDeptController.java</Path> |
| AC-026 | 已接受 T-26 及其上游证据 | 按 Ticket 执行并记录 Evidence | 每个候选有迁移或保留理由，不遗漏调用者 | <Path>backend/wta-modules/wta-system/</Path> |
| AC-027 | 已接受 T-27 及其上游证据 | 按 Ticket 执行并记录 Evidence | 保存/删除路径不再有虚假校验TODO | <Path>backend/wta-modules/wta-demo/</Path> |
| AC-028 | 已接受 T-28 及其上游证据 | 按 Ticket 执行并记录 Evidence | 慢provider不阻塞业务提交线程 | <Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotifyOutboxWakePublisher.java</Path> |
| AC-029 | 已接受 T-29 及其上游证据 | 按 Ticket 执行并记录 Evidence | 每个删除文件有owner、内容迁移落点和无丢失硬约束证据 | <Path>.agents/skills/</Path> |
| AC-030 | 已接受 T-30 及其上游证据 | 按 Ticket 执行并记录 Evidence | 全部已接受AC均有实际命令/退出码/环境/源码checkpoint | <Path>frontend/e2e/</Path> |

| AC-031 | 合法企业转移发码 | 真实Profile→Notify入队与最终投递 | QUEUED不被误判立即失败，challenge按明确状态激活 | <Path>backend/wta-modules/wta-profile/</Path> |

## 4. 范围

### IN

31 张 Ticket 及专项 review 中列出的源码、测试、构建、发布和文档路径；具体写集以 Ticket frontmatter 为准。清理建议必须逐文件确认 owner、内容迁移和删除门槛。

### REUSE

现有项目目录主轴、classic/layered 登记、wta-api/common/SPI、MySQL 初始化 owner、OpenAPI HMAC、PKCE、Sa-Token Client 隔离、Outbox wake/lease/fence、OSS lifecycle、静态 docs/fm 模板和第三方 schema。

### OUT

- **OOS-001**：本轮不实施任何产品代码或配置修改。
- **OOS-002**：不把未实跑的浏览器、Docker、MySQL、Redis、Nginx、远程 Actions 结果写成通过。
- **OOS-003**：不在没有外部消费者、版本、备份和用户批准时删除公开 API、数据库字段、历史记录或兼容桥。

## 5. 当前必须遵守的约束与待审目标

- **DEC-CR-001**：安全凭据不得持久化到 HTTP/操作日志；来源：R-01、ADR-0025。
- **DEC-CR-002**：认证前正文读取必须有界且超限失败；来源：R-02。
- **DEC-CR-003**：Client/IP 权限使用可信来源与当前 Client 上下文；来源：R-03、安全规则。
- **DEC-CR-004**：无 owner 的防重释放不得继续存在；来源：R-04。
以下目标尚未批准：

- **DEC-CR-005**：发布面、构建矩阵、artifact provenance 使用显式 manifest；来源：D-02/D-03/D-04/D-08/D-09/D-10。
- **DEC-CR-006**：前后端、SpecDev 和文档验证命令只报告真实 cwd、退出码和环境；来源：D-01/D-05/D-06/D-07/D-11。
- **DEC-CR-007**：公开 Java/HTTP/SQL 删除或不兼容改造必须先过 `java-api-compatibility` 与用户批准，不以“无兼容”覆盖硬门槛。

## 6. 数据、接口与兼容

- **公共接口变化：** 预期涉及 SSO callback/Cookie、日志策略、请求超限状态、CRUD method、上传 port、workflow 消息、Profile self material、发布 manifest；全部是待批准目标。
- **数据模型与持久化：** Notify callback receipt/唯一键、树一致性、可能的 release manifest；DDL 只由 release-artifacts MySQL owner 管理，需备份、差异、隔离演练。
- **兼容要求：** 用户希望无兼容升级，但现有公开 API 删除门槛仍适用；每个 Ticket 必须列外部调用方、替代 API、切换版本和回滚。没有证据不删除。
- **迁移要求：** Expand→Migrate→Contract 仅适用于确有数据/消费者迁移的 Ticket；安全会话可采用强制失效但需独立批准。

## 7. 非功能要求

- **NFR-001 安全与隐私：** 日志、SSO、Cookie、Origin、Client、IP、上传、SSRF/外部凭据和OpenAPI协议负向验证。
- **NFR-002 性能与容量：** 请求正文预算、通知提交线程、Redis lease、页面竞态、发布产物不混源；用隔离压测与指标证据。
- **NFR-003 可用性与可靠性：** 失败可恢复、幂等、事务回滚、stage 原子性、网络/服务不可用状态可操作。
- **NFR-004 可观测性与运营：** 保留关联ID、状态、耗时和失败类别；不得以原值换排障信息。

## 8. 验证策略

| 接缝 | 层级 | 覆盖 | 命令/方法 | Evidence |
|---|---|---|---|---|
| module interface | 单元/静态 | 代码 judo、类型、依赖、日志规则 | 项目真实 lint/typecheck/定向 Maven test | Ticket Evidence |
| HTTP/JSON | 集成 | 错误、权限、Client、编码、超限、脱敏 | MockMvc/隔离服务/合同测试 | Ticket Evidence |
| browser seam | E2E | SSO、Profile、workflow、upload、a11y | Playwright + 320/768/1440 视口 | Ticket Evidence |
| release seam | 失败注入 | manifest、digest、stage 回滚 | release verify、Compose/Nginx、before/after SHA | Ticket Evidence |

## 9. 风险、假设与未决问题

### 风险

公开 API/数据库/SSO 行为的无兼容切换会影响外部消费者和已有会话；通知 receipt/树修复可能需要数据迁移；新正文上限可能拒绝现有大请求；发布清单可能暴露当前产品“源码存在但未发布”的决策缺口。

### 已采用的低影响假设

本 change 目录是唯一写集；当前 main HEAD 和初始逐文件 SHA 是审查基线；所有 runtime 结论未验证时按 likely/needs-runtime 处理；用户会在实现前亲自审核。

### 未决问题

1. SSO 是否正式 shipped，生产 origin、TLS、cookie domain、回调和端口矩阵是什么？
2. 无兼容升级的发布版本、外部 Java/API 消费者和旧会话处理窗口是什么？
3. 最大合法普通 JSON/机器正文、可信代理 CIDR、Notify receipt 保留期和第三方租约上限是什么？
4. 是否恢复 GitHub Actions，并将哪些检查设为 required？
5. Home/SSO 品牌 token 和视觉目标由谁拥有，是否需要正式设计基线？
6. 用户接受哪些 proposed ADR 后，哪些 Ticket 才可转 Ready？
