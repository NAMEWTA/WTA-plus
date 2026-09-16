---
artifact: architecture-review
change: 2026-09-14-wta-plus-comprehensive-review
status: draft
---

# WTA-plus 全面架构审查

- **决策记录：** <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/architecture-review.md</Path>
- **审查入口：** <Path>{roots.workflows}/specdev/R-review-architecture/R-review-architecture.md</Path>
- **审查准则：** <Path>{roots.workflows}/specdev/R-review-architecture/review-rubric.md</Path>
- **基线：** `19620c745bb341b0e19fd5872ce02f01f50f9471`；工作树另有用户/其他任务未提交变更，本审查未接管。
- **状态：** draft；只记录候选，不发起单个候选的用户设计访谈，不实现代码。

## 1. 审查压力与范围

用户指定整个 WTA-plus：backend Java/Spring/MyBatis/Redis/OSS/Notify/SSO/Profile/Workflow/Third/System/API/common，frontend 三 App、domains/web-domains/platform/adapters/web-kit/tooling，根 docs/scripts/release/Skill/Agent/CI/Compose/Nginx/SQL/SpecDev 文档。专项审查报告分别位于：

- <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/platform-security-review.md</Path>：公共安全、日志、资源与 IP/防重。
- <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/backend-review.md</Path>：wta-api、业务 modules、事务/数据/公共合同。
- <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/frontend-review.md</Path>：三 App、前端 modules、权限/SSO/上传/工作流/UI/UX/a11y。
- <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/docs-delivery-review.md</Path>：事实、目录、门禁、构建、发布、文档清理。

未运行：Maven、pnpm、浏览器/E2E、Docker、真实 MySQL/Redis/MinIO/HTTP provider、Nginx 生产流量、GitHub Actions/branch protection、覆盖率/漏洞扫描。报告中的 `confirmed` 是静态调用链成立；`likely/needs-runtime` 仍需环境证据。

## 2. 当前结构地图

```text
Browser App (admin/home/sso)
  -> web-domain (Vue/UX/manifest)
  -> domain (transport mapping/business state)
  -> platform contracts (HTTP/auth/storage/permission)
  -> adapters (axios/crypto/OSS/storage)
  -> backend HTTP
       -> controller/listener/API adapter
       -> classic ServiceImpl OR layered UseCase -> Service -> DAO -> Mapper -> XML
       -> MySQL/Redis/OSS/Notify/Workflow/Third/SSO
Release manifest -> App/bundle/SQL/Nginx/Compose -> staged artifact
```

前端 App、web-domain、domain、platform、adapter 方向由架构检查工具维护；后端 classic/layered 由模式登记表维护。当前主要 depth/seam 问题不是“文件夹数量少”，而是认证、日志、状态、lease、发布来源和事实权威分散到多个调用者。

### 结构性压力统计

- 结构清单：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/repository-inventory.json</Path>（tracked 4679；backend POM 50；backend Java test 247；all tracked Java test including SpecDev example 248；Vue 130；frontend tests 101；可构建 App 3）。
- 700 行以上 tracked 文件 21 个（含 2 个 frontend E2E/测试文件；其余为生产源码），其中 <Path>backend/wta-common/wta-common-oss/src/main/java/org/namewta/common/oss/client/AbstractOssClientImpl.java</Path> 1932 行、<Path>frontend/packages/web-domains/system/src/role/RolePage.vue</Path> 1272 行、<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/service/impl/SysUserServiceImpl.java</Path> 1192 行；行数只触发职责审查，不直接授权拆分。
- 59 个 PUT/PATCH/DELETE controller mapping 候选分布在 27 文件，清单见 <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/legacy-crud-candidates.json</Path>；它们必须先区分 CRUD 与合法非 CRUD 协议。

## 3. 高置信候选索引

每一项都有文件、结构问题、代码 judo、删除复杂性、dependency class、strength、ADR关系、验证和用户状态；详细字段在专项报告中，不以摘要替代原始证据。

| ID | 级别/置信度 | 模块/结构类别 | 删除/深化方向 | 详细报告 |
|---|---|---|---|---|
| R-01 | P1 confirmed | HTTP/操作日志；boundary drift | 单一脱敏module删除原query/响应凭据副本 | platform-security-review |
| R-02 | P1 confirmed | 认证前无界读取；sequential orchestration | 有界采集seam，删除readAllBytes整副本 | platform-security-review |
| R-03 | P1 likely | 可信代理/IP；wrong-layer trust | 唯一可信来源解析，删除任意header fallback | platform-security-review |
| R-04 | P2 confirmed | Redis lease ownership；spaghetti race | owner token + 原子compare-delete，删除无主释放 | platform-security-review |
| B-01 | P1 confirmed | SSO bearer entropy | CSPRNG opaque值，保留PKCE | backend-review |
| B-02 | P2 confirmed | URI protocol serialization | URI builder，删除手工query拼接 | backend-review |
| B-03 | P1 likely | SSO Cookie transport | production Secure/SameSite/Origin合同 | backend-review |
| B-04 | P1 confirmed | Notify multi-table state | 短事务+fence，删除裸顺序写回 | backend-review |
| B-05 | P1 confirmed | Notify callback idempotency | durable receipt，删除seenEvents map | backend-review |
| B-06 | P1 likely | Third Redis semaphore lease | expirable owner permit，删除永久permit | backend-review |
| B-07 | P3 needs-runtime | Third limiter hot update | 版本化key或明确生效合同；先验证 | backend-review |
| B-08 | P2 confirmed | Profile shallow compatibility interface | 迁移消费者后删除throwing defaults/桥 | backend-review |
| B-09 | P2 confirmed | layered validator drift | canonical namespace/AST scanner | backend-review |
| B-10 | P1 likely | Workflow read authorization | 统一read-access seam，保留handler | backend-review |
| B-11 | P2 confirmed | legacy CRUD HTTP contract | 逐资源GET/POST收缩旧mapping | backend-review |
| B-12 | P1 confirmed | department tree cycle | 事务重检/锁定/无环更新 | backend-review |
| B-13 | P2 likely | Notify after-commit wake | Redis wake + 有界本地信号或删除同步I/O | backend-review |
| B-14 | P2 confirmed | Demo tree pseudo-contract | 实现最小不变量或移除误导surface | backend-review |
| B-15 | P1 confirmed | Profile→Notify status contract | QUEUED/ACTIVE可靠异步，不假称SYNC | backend-review |
| B-16 | P3 speculative rejected | Demo broad POM | 无unused/压力证据，保留 | backend-review |
| B-17 | P2 confirmed | module mode fact drift | 唯一登记表补Third，删重复名单 | backend-review |
| F-01 | P1 confirmed | browser crypto seam | 删除shared privateKey/ECB，TLS-only候选 | frontend-review |
| F-02 | P1 confirmed | Profile self vertical slice | 注入材料目录/上传/引用/提交 | frontend-review |
| F-03 | P1 confirmed | workflow task state seam | generation/abort/taskId guard | frontend-review |
| F-04 | P1 likely | App session teardown | finally本地清理+route reset | frontend-review |
| F-05 | P2 confirmed static | Home navigation sentinel | loaded状态替代空roles | frontend-review |
| F-06 | P2 confirmed | captcha/register UX | refresh/error/registration gate | frontend-review |
| F-07 | P2 confirmed | system import adapter | 统一port与error/abort终态 | frontend-review |
| F-08 | P2 confirmed static | list race + large SFC | generation/取消与职责提取 | frontend-review |
| F-09 | P2 confirmed UX | SSO subpath/returnTo | base-aware callback + recoverable error | frontend-review |
| F-10 | P2 confirmed | iframe message seam | origin/source/schema/nonce | frontend-review |
| F-11 | P2 confirmed | object URL ownership | dispose owner，detach/delete另证 | frontend-review |
| F-12 | P3 confirmed | TypeScript boundary drift | Ratchet strict边界，消除双cast | frontend-review |
| F-13 | P3 confirmed static | Home token/status/a11y | token fallback、live region、SFC可审 | frontend-review |
| D-01 | P1 confirmed | CI/submodule dead interface | 删除死门禁，显式gate manifest | docs-delivery-review |
| D-02 | P1 confirmed/needs-runtime | SSO release surface | 显式App release manifest + origin链 | docs-delivery-review |
| D-03 | P1 confirmed | partial provenance | 禁混源deployable current | docs-delivery-review |
| D-04 | P1 likely | full/core bundle drift | effective POM/JAR allow/deny manifest | docs-delivery-review |
| D-05 | P2 confirmed | facts duplication | profile/module map唯一事实 | docs-delivery-review |
| D-06 | P2 confirmed | generic AGENTS shallow copies | 逐文件归并/删除，保留special | docs-delivery-review |
| D-07 | P2 confirmed | SpecDev stale cwd/scope | 真实命令矩阵（未来用户批准） | docs-delivery-review |
| D-08 | P2 confirmed | dev build overwritten by production | per-App per-mode一次构建 | docs-delivery-review |
| D-09 | P2 confirmed/needs-runtime | CI/Compose image drift | image manifest/digest | docs-delivery-review |
| D-10 | P2 confirmed | non-atomic release stage | temp stage一次promotion | docs-delivery-review |
| D-11 | P2 confirmed | skill validator private-dir precondition | 去目录存在硬门禁 | docs-delivery-review |

## 4. 代码 judo 总结

推荐优先深化/删除：统一日志策略（R-01）、有界正文采集（R-02）、SSO CSPRNG（B-01）、Notify durable state（B-04/B-05）、Profile→Notify准确状态（B-15）、工作流当前task（F-03）、显式release/provenance（D-02/D-03）和唯一事实/gate（D-01/D-05）。这些候选会删除分散复杂性并提高 locality/leverage。

先探索再决定：可信代理CIDR（R-03）、第三方租约/热更新（B-06/B-07）、Workflow读权限（B-10）、SSO Cookie跨Origin（B-03）、OSS引用删除（F-11）和视觉/a11y视觉断点。它们存在实际压力，但最终行为依赖 runtime/外部合同。

拒绝机械清理：B-16 broad Demo POM、仅凭行数拆 OSS/QueryBuilder/ExcelBuilder、删除第三方 `org.dromara`/schema、删除历史 archive/许可证、把 placeholder README 当源码、以全局 `any` 或关闭测试换绿色。这些不通过删除测试。

## 5. 推荐顺序与用户访谈

唯一最佳推荐：先以 T-02（日志凭据）建立失败关闭的安全边界，再以 T-10（发布来源/原子 stage）建立可回滚交付边界；其余候选按依赖 DAG 排队，不能并列默认接受。当前不自动调用 Grill Work；本报告的所有候选均 `unselected`，没有 `user conclusion`。用户可以指定一个候选进入 `<Path>{roots.workflows}/specdev/G-grill-with-docs/G-grill-with-docs.md</Path>`，一次只访谈一个，不批量假设接受。

## 6. 交付/实现边界

Spec、ADR、Tickets 和 cleanup plan 是草案。Ticket frontmatter 全部 `status: draft`, `ready: false`；没有 goal-plan、implementation-map、Evidence/Ticket 完成记录。实施需用户接受目标行为后进入 Spec/Tickets quality gate，再按正确项目 Skill 创建写集；提交、推送、发布和永久知识提升仍须单独授权。

## 7. 架构候选卡（供逐项设计访谈）

以下卡片把索引压缩为可审议的架构决策单元；每卡均保留证据、删除测试和状态。`unselected` 表示尚未进入设计访谈。

### AR-01 安全副本与输入边界（R-01/R-02/R-03/R-04）
- **Evidence:** `backend/wta-common/wta-common-log/.../LogAspect.java`、`backend/wta-common/wta-common-openapi/.../RepeatedlyRequestWrapper.java`、`backend/wta-common/wta-common-core/.../ServletUtils.java`、`backend/wta-common/wta-common-redis/.../RepeatSubmitAspect.java`；详见 platform-security-review。
- **Before → After:** 多处原值日志/无界读取/任意 XFF/无主释放 → 一个失败关闭的 redaction 与 bounded-body seam、trusted-hop resolver、owner compare-delete。
- **Deletion test:** 删除旧 raw response/query 副本、`readAllBytes` 整体克隆、任意 header fallback、无 owner delete 后，审计与限流仍有元数据且重试正确。
- **Dependency class / strength:** security boundary；R-01/R-02/R-04 confirmed，R-03 likely。
- **ADR conflict:** ADR-0025 日志禁凭据；ADR-CR-002/003/009 提案；无已接受冲突。
- **Interview:** unselected；user conclusion pending。

### AR-02 SSO 会话与回调（B-01/B-02/B-03/F-09）
- **Evidence:** `backend/wta-modules/wta-sso/...`、`frontend/packages/platform/auth/src/index.ts`、`frontend/apps/*/src/application/sso.ts`。
- **Before → After:** 可预测 bearer、手工 query、Cookie/returnTo 环境差异 → CSPRNG opaque、URI builder、base-aware callback 与显式生产 cookie/origin 合同。
- **Deletion test:** 删除 ThreadLocalRandom/雪花 bearer 与手拼参数后，PKCE、单次兑换、失败可重试和同 App 回跳保持成立。
- **Dependency class / strength:** public auth contract；B-01/B-02 confirmed，B-03 likely，F-09 confirmed UX。
- **ADR conflict:** ADR-CR-001；公开会话删除受 ADR-CR-008 门槛约束。
- **Interview:** unselected；user conclusion pending。

### AR-03 Notify 与跨存储状态（B-04/B-05/B-13/B-15）
- **Evidence:** `backend/wta-modules/wta-notify/...`、`backend/wta-modules/wta-profile/.../EnterpriseTransfer*`、`PersonRebindNotificationService`。
- **Before → After:** 多表裸写、内存 seenEvents、同步 wake、SYNC 误判为已送达 → 短事务+唯一 receipt/outbox、可恢复 poll、Profile 显式 QUEUED/ACTIVE/REVOKED 状态。
- **Deletion test:** 删除 `seenEvents` 和同步 provider 路径后，事务回滚可重放、重复事件幂等、用户可重试。
- **Dependency class / strength:** distributed consistency；B-04/B-05/B-15 confirmed，B-13 likely。
- **ADR conflict:** ADR-CR-009；Redis challenge 与 MySQL transfer 不得虚称同事务。
- **Interview:** unselected；user conclusion pending。

### AR-04 Frontend state ownership（F-03/F-04/F-05/F-06/F-07/F-08/F-10/F-11）
- **Evidence:** `frontend/apps/*`、`frontend/packages/platform/*`、workflow/system/profile pages；详见 frontend-review。
- **Before → After:** stale task/response、空 roles sentinel、上传 loading/object URL 泄漏、iframe 未校验 → generation+abort/owner teardown、loaded sentinel、统一 upload port、origin/source/schema/nonce 校验。
- **Deletion test:** 删除调用者散落的 loading/URL 清理与旧 task fallback 后，失败重试、取消、跨窗口消息和引用生命周期仍有单一 owner。
- **Dependency class / strength:** UI state/resource boundary；F-03/F-05/F-06/F-07/F-08/F-10/F-11 confirmed，F-04 likely。
- **ADR conflict:** ADR-CR-007；不得以 UI 隐藏替代后端授权。
- **Interview:** unselected；user conclusion pending。

### AR-05 Profile 自助材料与公共桥（F-02/B-08）
- **Evidence:** `frontend/packages/web-domains/profile/...`、`backend/wta-modules/wta-profile/...`、`frontend/packages/domain-profile/...`。
- **Before → After:** 新提交缺必需材料、deprecated throwing default → materials catalog/upload/reference seam、消费者迁移后删除桥接默认。
- **Deletion test:** 删除旧 bridge 后所有 App/domain 编译与 contract tests 仍通过；无材料提交必须给出可恢复错误。
- **Dependency class / strength:** domain/public API；F-02/B-08 confirmed。
- **ADR conflict:** ADR-CR-008；需 java-api-compatibility 清零消费者。
- **Interview:** unselected；user conclusion pending。

### AR-06 Workflow 读写权限与 Demo 树（B-10/B-12/B-14）
- **Evidence:** `backend/wta-modules/wta-workflow/...`、`backend/wta-modules/wta-system/...department...`、`backend/wta-modules/wta-demo/...`。
- **Before → After:** task/variable 读取 seam 不统一、部门可成环、Demo TODO → handler 统一授权、事务环检测、最小可执行示例或移除误导 surface。
- **Deletion test:** 删除重复 controller-side checks/TODO 后，越权和环输入仍被拒。
- **Dependency class / strength:** authorization/tree invariant；B-10 likely，B-12/B-14 confirmed。
- **ADR conflict:** ADR-CR-008/工程分层硬约束；不得删除真实授权门禁。
- **Interview:** unselected；user conclusion pending。

### AR-07 Third leases and runtime limits（B-06/B-07）
- **Evidence:** `backend/wta-modules/wta-third/...` semaphore/limiter runtime。
- **Before → After:** 永久 permit、动态限额生效未证 → owner token+TTL、版本化配置并补运行时验证；B-07 保持 needs-runtime。
- **Deletion test:** 删除无 TTL permit 后进程崩溃可恢复；未复现前不删除现有 limiter。
- **Dependency class / strength:** distributed resource; B-06 likely，B-07 needs-runtime。
- **ADR conflict:** ADR-CR-009；需 Redis kill/clock skew 证据。
- **Interview:** unselected；user conclusion pending。

### AR-08 HTTP CRUD 与 strict（B-09/B-11/F-12）
- **Evidence:** 27 controllers / 59 legacy mappings 清单、layered validator、frontend tsconfig 与双 cast。
- **Before → After:** 旧 PUT/PATCH/DELETE 与 validator 漂移、strict 豁免 → 资源逐项 GET/POST 迁移、AST namespace scanner、分层 strict ratchet。
- **Deletion test:** 每个旧 mapping 有零消费者与 OpenAPI/E2E 证据；非法 import 仍 fail，注释不误报。
- **Dependency class / strength:** public transport/build contract；B-09/B-11/F-12 confirmed。
- **ADR conflict:** API-005 与 ADR-CR-008；不得删除合法非 CRUD。
- **Interview:** unselected；user conclusion pending。

### AR-09 Release/provenance/gates（D-01/D-02/D-03/D-04/D-08/D-09/D-10/D-11）
- **Evidence:** `release-artifacts/`、Compose/Nginx、scripts/ci、frontend package scripts；详见 docs-delivery-review。
- **Before → After:** 自动发现/混源/非原子 stage/镜像漂移 → 显式 manifest、source+digest、一次 promotion、失败注入与 clean clone gate。
- **Deletion test:** 删除死 submodule gate、dev 覆写链和多 current 目录后，三 App 产物与回滚证据仍可生成。
- **Dependency class / strength:** delivery boundary；D-01/D-03/D-04/D-08/D-10/D-11 confirmed，D-02/D-09 needs-runtime qualified。
- **ADR conflict:** ADR-CR-005/006；远程 required 需外部证据。
- **Interview:** unselected；user conclusion pending。

### AR-10 Facts and document locality（D-05/D-06/D-07/B-17）
- **Evidence:** project profile/module map、41 generic AGENTS + 8 special keep、SpecDev command docs、Third mode registry。
- **Before → After:** 重复/过时事实与 cwd → 单一生成事实、owner 文档、真实命令矩阵、Third 登记。
- **Deletion test:** 删除 generic 副本后所有硬约束可由 owner 文档与链接覆盖；历史归档/许可证保留。
- **Dependency class / strength:** documentation/governance；D-05/D-06/D-07/B-17 confirmed。
- **ADR conflict:** ADR-CR-006；不修改永久知识直至用户批准。
- **Interview:** unselected；user conclusion pending。