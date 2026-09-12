---
schema_version: 1
artifact: upstream-fork-assessment
change: 2026-08-24-upstream-fork-upgrade-assessment
status: final
backend_head: 58aaf342100a2cfc2988e01b257f7468bb2bbad9
backend_upstream: 2933badb9182aaecfd5a45ce09444b8ac59576bb
frontend_head: f7d116f6e2b6b61239afc86cbcb860a07530abad
frontend_upstream: 0870ce17514895854ccff03600e102546d8c5046
assessed_at: 2026-08-24T01:12:00+08:00
---

# NAMEWTA 二次开发与上游 Fork 升级完整评估

## 1. 执行摘要

本轮二次开发不是局部 CRUD，而是把上游 6.0.0 基座改造成了一个以 **Client 为认证授权上下文、以 OSS 为独立数据面、以统一通知为跨业务能力、以业务 owner 管理附件生命周期** 的平台基座。

综合判断：**架构方向正确，可持续 fork 适配度为 74/100（中上，可持续，但治理欠账明显）**。

主要正向结论：

1. 后端 178/263 个 fork-side 变更文件是新增文件，说明 OSS、通知和测试多数采用 additive extension，而不是大面积改写上游。
2. Client/RBAC 采用失败关闭，不再保留无 Client、无登录域或跨 Client fallback；安全语义比上游更严格。
3. OSS 浏览器直传把文件字节从业务后端移出，后端只保留认证、策略、签名、完成确认、引用和生命周期控制。
4. `ruoyi-common-notify` 是薄合同层，Mail/SMS 仍保持原子 Adapter；同步发送、Redis 幂等、附件快照与异步监控的职责清晰。
5. 当前缓存上游只有一个后端独有提交，且与 NAMEWTA 变更文件零重叠；前端缓存点没有上游独有提交，短期同步冲突很低。
6. 当前后端 41 模块测试与 package 通过，129 个测试 0 失败；前端 lint、build 和 4 个 Vitest 测试通过。

阻止其成为稳定长期基线的主要问题：

1. 父仓库仍固定后端 `704d87a6`，实际后端 `main` 已到 `58aaf3421`，当前组合不可由父仓库 HEAD 复现。
2. 没有 CI；后端默认 Maven 会跳过测试；前端测试和 typecheck 没有 script，`vue-tsc` 还被大小写冲突阻断。
3. Client/RBAC、OSS、通知均缺少真实 MySQL/Redis/S3/browser E2E；3 个后端集成测试被环境条件跳过。
4. NAMEWTA SQL 明确只维护 MySQL，低于上游多数据库能力；应明确产品边界或补齐方言迁移。
5. 上游热点清单仍只覆盖 Client/RBAC，未覆盖 OSS、统一通知、业务附件 owner 和 `bundle-full`；工程知识仍写 25 个 common artifact，实际已经是 26 个。

## 2. 评估边界与可信度

### 2.1 固定点

| Repository | 当前 HEAD | 上游固定点 | 共同祖先 | 左右差异 |
|---|---|---|---|---|
| Parent `<Path>.</Path>` | `aa4752fe2d51ce705d442c9567e8e33f3676b736` | 无独立 upstream | 不适用 | 本地比 `origin/main` 领先 1 |
| Backend `<Path>ruoyi-vue-plus-namewta/</Path>` | `58aaf342100a2cfc2988e01b257f7468bb2bbad9` | `2933badb9182aaecfd5a45ce09444b8ac59576bb` | `387c4f0a20e9232f44e762ef5a46c462f54bd464` | upstream-only 1 / fork-only 42 |
| Frontend `<Path>plus-ui-namewta/</Path>` | `f7d116f6e2b6b61239afc86cbcb860a07530abad` | `0870ce17514895854ccff03600e102546d8c5046` | 同 upstream 固定点 | upstream-only 0 / fork-only 20 |

后端公开 `6.X` 提交页在评估日仍以 `2933bad` 为最新提交：<Url>https://github.com/dromara/RuoYi-Vue-Plus/commits/6.X</Url>。前端本地 `upstream/6.X-Vue` 为 `0870ce1`，但 GitHub 页面返回 429，不能声明已实时确认；其公开仓库为 <Url>https://github.com/CrazyLionCat/plus-ui/tree/6.X-Vue</Url>。

### 2.2 网络限制

`git fetch upstream --prune --tags` 在两个子仓库均因 GitHub 443 连接 75 秒超时失败。报告没有覆盖本地 upstream refs；后端通过公开提交页交叉确认，前端保留缓存时效风险。

### 2.3 统计口径

- Fork-side 变更使用三点差异：共同祖先到当前 HEAD，只统计 NAMEWTA 一侧。
- Snapshot 差异使用上游 tip 到当前 HEAD 的两点差异，包含尚未吸收的上游变化。
- 固定基线标签不移动：后端 `<Path>ruoyi-vue-plus-namewta</Path>` 的 `namewta-base-upstream-6x=a16f249`，前端 `namewta-base-upstream-6x-vue=0870ce1`。
- 后端从固定基线到当前 HEAD 共 43 个提交，其中包含一次 upstream merge；前端为 20 个提交。

## 3. 差异规模

| 指标 | Backend fork-side | Backend snapshot vs upstream | Frontend fork-side |
|---|---:|---:|---:|
| Changed files | 263 | 264 | 59 |
| Insertions | 14,535 | 14,549 | 2,985 |
| Deletions | 653 | 655 | 264 |
| Added files | 178 | 包含 upstream 独有差异 | 22 |
| Modified files | 84 | 包含 `SqlLogInterceptor.java` | 37 |
| Deleted files | 1 | 1 | 0 |

后端文件分布：

| Area | Files | Interpretation |
|---|---:|---|
| `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/</Path>` | 109 | Client/RBAC、OSS 控制面、引用生命周期、通知监控的主要 owner |
| `<Path>ruoyi-vue-plus-namewta/ruoyi-admin/</Path>` | 55 | 认证组装、通知上下文、配置和集中测试 |
| `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-notify/</Path>` | 41 | 全新增薄通知合同与实现核心 |
| `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-oss/</Path>` | 15 | S3 能力、Multipart、预签名和对象元数据合同 |
| `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-sms/</Path>` | 8 | SMS Adapter 与 provider 解析 |
| `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/</Path>` | 7 | Client-scoped 办理人、统一通知和历史附件 owner |
| `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-mail/</Path>` | 5 | Mail Adapter 与附件快照消费 |
| `<Path>ruoyi-vue-plus-namewta/script/sql/namewta/</Path>` | 5 | MySQL append-only DDL/DSL 和 ID 工具 |

前端文件分布：12 个 system API、10 个 system 页面、8 个 business 占位文件、6 个 OSS hook/utility、5 个通知监控文件，以及登录/注册和共享上传组件等上游核心文件。

## 4. 业务流程改变

### 4.1 登录与身份：从用户单值类型到 Client 登录域

旧路径把 `user_type` 当成用户自身单值属性。新路径引入 `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/domain/SysUserType.java</Path>` 与 `sys_user_type_rel`：

```text
OAuth clientId string
  -> 查询 sys_client
  -> 检查 Client 状态、grant type、user_type_id
  -> 认证用户
  -> ClientUserTypeAccessService.requireLoginAccess
  -> 校验登录域存在/启用 + 用户关系存在
  -> Token extra 固化 userType + clientPk
```

业务收益：

- 一个用户可以属于多个登录域，不需要复制账号。
- Client 决定允许的登录域，用户关系决定是否准入，两层都必须成立。
- 五种认证策略统一走相同准入，不会出现密码登录安全、短信或社交登录绕过的分叉。
- `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-satoken/src/main/java/org/dromara/common/satoken/utils/LoginHelper.java</Path>` 只读 Token 快照，不再从 loginId 或用户对象回退推断。
- 用户停用/删除按已有登录域清理会话，避免只清一个类型留下存量 Token。

前端 `<Path>plus-ui-namewta/src/views/login.vue</Path>`、`<Path>plus-ui-namewta/src/views/register.vue</Path>` 和 `<Path>plus-ui-namewta/src/api/login.ts</Path>` 在提交前读取 `/auth/client/context`；仅接受精确 JSON Boolean，加载失败或畸形时禁用登录/注册。

### 4.2 权限、角色与动态路由：全部按 Client PK 收敛

新不变量是：

```text
effective permissions = userId + sys_client.id + explicit roles + valid default role
```

具体改变：

- `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysPermissionServiceImpl.java</Path>` 的角色/菜单读取显式要求 Client PK。
- `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysMenuServiceImpl.java</Path>` 即使超管也只读取当前 Client 菜单，不再返回全局并集。
- 默认角色运行时合并，不写入 `sys_user_role`；无效默认角色失败关闭。
- `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysTaskAssigneeServiceImpl.java</Path>` 将工作流角色办理人查询也收敛到 Client，堵住外围调用绕过。
- 前端 `<Path>plus-ui-namewta/src/views/system/user/index.vue</Path>` 按用户登录域对应的启用 Client 顺序加载角色上下文，全部成功后才打开编辑框。
- 前端不重复过滤菜单；`getRouters -> addRoute` 消费后端已裁剪结果。

这部分的核心优化不是新增字段，而是取消所有 userId-only 和跨 Client fallback，使授权错误从“悄悄扩大权限”变成“明确失败”。

### 4.3 注册策略：从全局开关移动到 Client

- `registerEnabled` 成为 `sys_client` Boolean 合同，不再读取全局配置。
- 注册请求可选携带手机/邮箱；非空时执行格式与唯一性校验。
- 已存在账号提示登录，不隐式追加登录域。
- 当前公开前端仍只提交核心字段，避免未经产品设计扩大注册表单。

### 4.4 OSS 上传：从后端字节代理改成控制面/数据面分离

```text
Browser
  -> POST control-plane init/sign/complete/cancel
  -> direct PUT or Multipart bytes to S3-compatible OSS
  -> complete confirmation returns ossId
Business Service
  -> reconcileReferences(refType, refId, previousIds, currentIds)
  -> authorized short-lived download URL
```

关键实现：

- `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-oss/src/main/java/org/dromara/common/oss/client/OssClient.java</Path>` 和 `AbstractOssClientImpl.java` 增加 Multipart、HEAD、copy、capabilities、presigned request 等 S3 合同。
- `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysOssUploadController.java</Path>` 暴露控制面。
- `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/upload/OssUploadService.java</Path>` 负责命名策略、身份、Ticket、签名、完成校验、取消和诊断。
- Redis Ticket 默认 24 小时，签名默认 5 分钟；Part 签名有批次上限，配置在启动时强校验。
- 前端 `<Path>plus-ui-namewta/src/hooks/oss/useDirectOssUpload.ts</Path>` 集中单 PUT/Multipart、进度、取消、重试和恢复；`transport.ts` 与主 Axios 鉴权客户端隔离，避免把业务 Header 泄漏到 OSS。
- `<Path>plus-ui-namewta/src/components/FileUpload/index.vue</Path>`、`ImageUpload/index.vue`、`Editor/index.vue` 只消费 hook，返回值统一为 `ossId`。
- 下载通过短时 URL，文件字节不再经过业务 Controller。

业务收益：降低 JVM 内存和双向带宽，支持大文件、断点续传和失败 Part 重试，同时保留后端授权与策略控制。

### 4.5 OSS 生命周期：引用归业务 owner，引用不等于 ACL

`sys_oss_ref` 保存真实物理表名和真实主键，只用于生命周期与反向定位，不做动态查表或权限判定。

- `TEMP` 对象无引用时按保留期清理。
- 对象删除采用 `ACTIVE -> PENDING -> provider delete -> metadata delete` 的可恢复过程。
- 通用 API 只暴露集合式 `reconcileReferences`，不再鼓励调用方逐个 bind/unbind。
- 当前 owner 已覆盖 `sys_user` 头像、`sys_notice` 内容附件、`sys_notify_log` 附件快照和 `flow_his_task` 历史附件。
- `<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/owner/BusinessOssOwnerArchitectureUnitTest.java</Path>` 形成 ratchet，阻止新增富文本/OSS 字段绕过 owner。

这是一项重要架构优化：业务授权仍由业务 Service 完成，OSS 层只管理对象状态，避免把存储模块演变成通用权限引擎。

### 4.6 通知：从直接 Builder/SDK 调用到统一 Dispatcher

```text
Caller -> NotifyClient/NotifyDispatcher
       -> validate + context snapshot + attachment snapshot
       -> Redis idempotency claim
       -> channel registry -> Mail/SMS adapter -> Provider
       -> NotifyResult (per target, partial failure capable)
       -> best-effort ApplicationEvent -> system monitor tables
```

关键实现：

- `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-notify/</Path>` 新增 41 个文件，拥有请求、内容、目标、逐目标结果、异常、Adapter SPI、Registry、Dispatcher、幂等和事件合同。
- `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-mail/src/main/java/org/dromara/common/mail/notify/MailNotifyChannelAdapter.java</Path>` 与 SMS Adapter 保留渠道原子性。
- Redis 幂等键使用 channel + idempotency key，摘要包含业务、目标、内容、附件和 metadata；相同键不同载荷冲突，Provider 成功结果可复用。
- 附件在发送前复制为通知快照，避免业务源文件后续变更破坏审计。
- 发送保持同步；监控事件发布或落库失败只记录告警，不改写 Provider 发送结果。这是明确的 best-effort 语义，不是可靠队列。
- `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/notify/service/impl/SysNotifyMonitorServiceImpl.java</Path>` 记录逻辑通知与逐目标 Delivery，支持清洗、脱敏、完整详情、附件授权下载和物理清理。
- 前端 `<Path>plus-ui-namewta/src/views/monitor/notify/index.vue</Path>` 与 `detailDrawer.vue` 提供全局运维视图，并用 `system:notify:*` 权限保护。

### 4.7 工作流与现有调用方迁移

- 验证码、Demo Mail/SMS、公告与 Warm-Flow 消息从直接 Provider 调用迁移到统一通知入口。
- 工作流办理人角色查询增加 Client 上下文。
- `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/oss/WorkflowHistoryOssOwner.java</Path>` 在任务历史创建、替换和删除时维护附件引用。
- 业务模块仍通过 `<Path>ruoyi-vue-plus-namewta/ruoyi-api/</Path>` 调 system/workflow 门面，没有新增对 system 内部实现的跨模块依赖。

### 4.8 部署组装

最新 `58aaf3421` 将 job、AI、demo、workflow、gen 从固定依赖移动到默认 `bundle-full` profile，保持全量部署默认行为，并为未来组合部署留出 composition root。

当前实际效果是“默认全量部署已验证”；注释中的 `bundle-platform-x` 仍是示例，并不是已经可交付的裁剪方案。

## 5. 文件与代码优化矩阵

| 流程/边界 | 关键文件 | 代码级改变 |
|---|---|---|
| Auth/Client context | `<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/java/org/dromara/web/controller/AuthController.java</Path>`, `IAuthStrategy.java`, five `*AuthStrategy.java` | 公开 Client Boolean 上下文；五策略统一登录域准入；Token 写入 `clientPk`/`userType` |
| Register/session | `SysRegisterService.java`, `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/ClientSessionService.java</Path>` | Client 注册开关；可选身份唯一性；按登录域清 Session |
| User type | `SysUserType*`, `SysUserTypeRel*` entity/BO/VO/mapper/service/controller | 新增登录域定义、关系 CRUD、状态和 options API |
| RBAC | `SysRoleController.java`, `SysRoleServiceImpl.java`, `SysRoleMapper.java`, `SysPermissionServiceImpl.java` | 角色读写和权限计算携带 Client PK；默认角色只运行时合并 |
| Menus | `SysMenuController.java`, `SysMenuServiceImpl.java`, `SysMenuMapper.java` | 超管也按 Client；无 Client 返回空/拒绝；动态路由不跨 Client |
| Direct OSS SPI | `OssClient.java`, `AbstractOssClientImpl.java`, `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-oss/src/main/java/org/dromara/common/oss/model/</Path>` | Multipart/HEAD/copy/presign/capability 和结构化错误 |
| OSS control plane | `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/upload/</Path>` | 25 个左右新增类型覆盖策略、Ticket、签名、完成、清理与诊断 |
| OSS lifecycle | `OssLifecycleManager.java`, `SysOssRef.java`, `SysOssRefMapper.java`, `OssTempCleanupTask.java` | TEMP、引用 reconcile、下载签名、可恢复删除与清理 |
| Notify core | `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-notify/</Path>` | 薄合同、同步 Dispatcher、Adapter、幂等、附件快照、事件 |
| Notify adapters | `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-mail/src/main/java/org/dromara/common/mail/notify/</Path>`, `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-sms/src/main/java/org/dromara/common/sms/notify/</Path>` | Mail/SMS Provider 适配与逐目标结果 |
| Notify monitoring | `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/notify/</Path>` | 双层日志、脱敏/清洗、附件 owner、查询/删除/清空 |
| Workflow ownership | `WorkflowHistoryOssOwner.java`, `FlwCommonServiceImpl.java`, `FlwTaskServiceImpl.java` | 统一通知，历史附件在真实 `flow_his_task` 主键上绑定 |
| SQL | `<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DDL.sql</Path>`, `DML.sql` | 2 个登录域表、Client/role/menu 字段、OSS 引用、2 个通知表、菜单与默认角色 |
| Frontend Client/RBAC | login/register, `<Path>plus-ui-namewta/src/views/system/user/</Path>`, role/menu/client/userType API/pages | 严格 Client context、分域用户、Client-scoped role/menu 管理 |
| Frontend OSS | `<Path>plus-ui-namewta/src/hooks/oss/</Path>`, `<Path>plus-ui-namewta/src/utils/oss/</Path>`, three upload components, download plugin | 直传、续传、隔离 transport、授权下载、生命周期状态 |
| Frontend monitoring | `<Path>plus-ui-namewta/src/api/monitor/notify/</Path>`, `<Path>plus-ui-namewta/src/views/monitor/notify/</Path>` | 全局检索、详情、完整投递、附件授权下载、双确认清空 |
| Deployment | `<Path>ruoyi-vue-plus-namewta/ruoyi-admin/pom.xml</Path>` | 默认 `bundle-full` 组合和未来 bundle 扩展点 |

完整文件清单可用以下固定命令重现：

```bash
git -C ruoyi-vue-plus-namewta diff --name-status 387c4f0a20e9232f44e762ef5a46c462f54bd464...58aaf342100a2cfc2988e01b257f7468bb2bbad9
git -C plus-ui-namewta diff --name-status 0870ce17514895854ccff03600e102546d8c5046...f7d116f6e2b6b61239afc86cbcb860a07530abad
```

## 6. 当前架构评价

### 6.1 形成的平台分层

```text
Vue / Browser
  |-- Client-aware Auth + RBAC UI
  |-- OSS direct-transfer data plane
  `-- Notification operations UI

ruoyi-admin (composition root)
  |-- auth/register/captcha assembly
  `-- bundle-full application assembly

ruoyi-api (cross-module stable facades)
  |-- System APIs
  `-- Workflow APIs/events

ruoyi-common-* (technical contracts/adapters)
  |-- common-notify
  |-- common-oss
  |-- common-mail / common-sms adapters
  `-- satoken / redis / mybatis / web

ruoyi-system (platform ownership)
  |-- identity, Client, RBAC, menus
  |-- OSS control plane + lifecycle
  `-- notification monitoring

business/workflow owners
  `-- authorize business records, then reconcile OSS references
```

### 6.2 架构优点

- **边界语义清楚：** Client 是登录授权上下文，不是租户；`client_pk` 在通知中只是审计来源。
- **公共依赖方向正确：** 业务通过 `ruoyi-api` 或 common SPI，未依赖 system 内部 domain/service。
- **数据面分离：** 浏览器和 OSS 交换字节，后端掌握控制权但不承担文件流量。
- **可靠性语义诚实：** 同步通知结果与异步 best-effort 监控分开，不把 Application Event 假装成消息队列。
- **生命周期归 owner：** 业务授权、存储状态和引用管理不互相越权。
- **测试形态较好：** 新增大量 contract/architecture/negative-path 测试，能保护安全不变量和未来 owner 接入。

### 6.3 架构压力点

- `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/</Path>` 承担 109 个变更文件，已经从系统管理模块向平台 kernel 扩展。当前仍可接受，但以后每增加一个横向能力都塞入 system 会形成 divergent change。
- Client 隔离必须修改上游核心 User/Role/Menu/Auth 文件，无法完全 additive；这些文件是长期同步热点。
- `ruoyi-common-notify` 新增后，知识地图和质量门禁没有同步升级，出现“代码边界已形成，治理边界未跟上”。
- Append-only SQL 有审计价值，但没有数据库级 migration ledger、checksum、环境已执行状态与自动 dry-run，规模继续增长后人工判断风险会上升。
- `bundle-full` 是良好 composition root 起点，但还没有实际多 bundle 产品矩阵。

当前不建议立刻把 OSS/Notify 从 system 再拆仓或拆服务。先补齐 CI、迁移、E2E、文档和发布治理；只有出现独立部署、独立扩缩容或多个应用复用且生命周期不同的真实需求时，再拆 `platform-storage` / `platform-notify`。

## 7. 持续 Fork 适配度

| Dimension | Score | Evidence |
|---|---:|---|
| Upstream freshness | 17/20 | 后端仅落后 1 个无重叠提交；前端缓存无落后，但实时确认受限 |
| Additive isolation | 16/20 | 后端 67.7% 文件为新增；common-notify/oss upload/monitor/test 多为新目录；前端修改型文件占比偏高 |
| Contract and regression safety | 17/20 | 后端 129 tests、架构 ratchet、前端 4 tests；但缺 E2E、CI、绿色 typecheck |
| Reproducibility and release | 11/20 | child main 同步 origin；父 gitlink 落后、根本地领先、无 fork release 版本和 CI |
| Documentation and operations | 13/20 | ADR/Spec/Evidence 完整；customization map、module guide、跨库迁移和 runbook 不完整 |
| **Total** | **74/100** | **可持续，但应先清治理欠账再扩大业务面** |

立即同步冲突很低：后端 upstream-only 的 `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-mybatis/src/main/java/org/dromara/common/mybatis/interceptor/SqlLogInterceptor.java</Path>` 与 fork-side 变更零重叠。长期冲突为中等，主要来自 84 个后端修改文件和 37 个前端修改文件。

## 8. 当前健康状态

| Check | Result | Notes |
|---|---|---|
| Backend tests | PASS | `sh mvnw -Dmaven.test.skip=false test`; 41 reactor entries; 129 run, 0 failure, 0 error, 3 skipped |
| Backend package | PASS | `sh mvnw package -DskipTests`; 41/41 SUCCESS; `ruoyi-admin.jar` repackaged |
| Frontend lint | PASS | `pnpm lint` |
| Frontend production build | PASS | `pnpm build:prod`; 3,368 modules transformed |
| Frontend Vitest supplemental | PASS | `pnpm exec vitest run`; 2 files, 4 tests passed |
| Frontend type supplemental | FAIL | `pnpm exec vue-tsc --noEmit`; inherited TS1149 `loginInfo/logininfo` casing conflict |
| Diff whitespace | PASS | backend and frontend `git diff --check` |
| CI | ABSENT | No supported workflow/Jenkins/GitLab configuration found |
| Parent reproducibility | FAIL | backend gitlink `704d87a6` != backend HEAD `58aaf3421` |
| Live upstream refresh | PARTIAL | git fetch failed; backend web confirmed; frontend live confirmation rate-limited |

三个跳过测试是依赖真实外部环境的 Redis/MySQL integration cases，不能写成通过。既有归档也明确未执行 Client/RBAC 与 OSS/通知的完整 runtime E2E。

## 9. 需要升级的地方

### P0：立即处理，建立可信基线

1. **修复父仓库快照。** 将 backend gitlink 从 `704d87a6` 更新到 `58aaf3421`，核对 frontend `f7d116f6`，将根仓库本地领先提交和本 change 分范围提交。当前报告不含提交授权。
2. **吸收后端 upstream `2933bad`.** 先 fast-forward 冻结 `6.X` 镜像，再集成到 product `main`；当前没有重叠文件，风险低。
3. **更新 fork 热点与工程知识。** `<Path>docs/upstream/customization-map.md</Path>` 增加 OSS、notify、workflow attachment owner、POM bundle；common 模块地图从 25 更新为 26，reactor 事实更新为 41。
4. **固定可发布版本。** 为 backend/frontend/parent 打同一 release train 的 NAMEWTA tag，并记录 child SHA、数据库变更块和回滚点。

### P1：下一迭代，补质量与运行时证据

1. **建立 CI。** 必跑 backend explicit tests + package、frontend lint + build + Vitest + typecheck、diff check、submodule SHA check；不能依赖默认 Maven 测试行为。
2. **修复大小写类型基线。** 统一 `loginInfo` / `logininfo` 目录与 import，增加 `typecheck` script。
3. **接入前端测试脚本。** 增加非写入式 `test`、`typecheck`、必要的 `format:check`，按 ratchet 推进，不用放宽 tsconfig 换绿色。
4. **建设 disposable E2E。** 最少覆盖 MySQL + Redis + S3-compatible storage + browser：多 Client 登录、跨 Client 负向授权、单 PUT、Multipart 失败恢复、TEMP 清理、通知部分失败、附件授权和监控落库。
5. **形成 OSS 运维 runbook。** CORS、Lifecycle、Bucket capability、Ticket TTL、dry-run、active cleanup、PENDING 删除恢复、监控和告警必须有部署前检查。
6. **形成通知数据治理。** 明确日志保留期、敏感 audit policy、清理任务、查看完整目标的权限审批和数据量容量模型。

### P2：中期，提升迁移和产品完整度

1. **数据库能力二选一。** 若产品只支持 MySQL，修改所有对外能力声明；若保留上游多库能力，为 NAMEWTA DDL/DSL 提供 PostgreSQL/Oracle/SQL Server 方言和自动验证。
2. **引入版本化 migration ledger。** 记录 change id、checksum、执行时间、状态和补偿，不仅依赖人工阅读 append-only 文件。
3. **实现真实 bundle。** 至少定义并验证 platform-only、workflow-enabled、full 等支持组合；启动时检查缺失 Bean 和配置。
4. **完成或隐藏四个业务工作台。** `<Path>plus-ui-namewta/src/views/business/</Path>` 当前仅是可导航占位，不能当作已交付业务能力。
5. **发布治理。** 增加 changelog、SBOM、依赖漏洞扫描、artifact checksum、数据库兼容矩阵和回滚演练。

### P3：需求触发后再做的演进

1. 只有通知需要可靠必达、自动重试或跨服务消费时，才引入 outbox/queue；当前同步发送 + best-effort audit 不应被无条件复杂化。
2. 只有 OSS/Notify 需要独立部署或扩缩容时，才从 system 拆成独立 platform module/service。
3. 对上游通用修复尽量回馈 upstream；NAMEWTA 业务语义保留在 fork，减少永久 patch 面。
4. 对核心热点建立 contract-first adapter：认证策略、Client context、上传 hook、通知 SPI 已是良好起点，后续不再把业务条件散落到更多 Controller/Page。

## 10. 推荐的持续同步流程

```text
1. fetch upstream + 记录 upstream SHA/日期
2. frozen 6.X / 6.X-Vue 只 fast-forward
3. 计算 merge-base、upstream-only、fork-only、overlap paths
4. 先审 customization map 与 permanent ADR
5. 在临时候选点合并 upstream，不直接污染 product main
6. 运行 backend tests/package + frontend lint/test/type/build
7. 跑 Client/RBAC、OSS、Notify 定向 E2E
8. 更新 parent gitlinks，验证 child HEAD == tree SHA
9. 生成 release notes、migration list、rollback point
10. 经确认后 commit/push/release
```

建议每周或每两周同步一次，不要等 upstream 累积数十个核心文件后再处理。同步报告至少保留：base/upstream/head SHA、左右提交数、overlap 文件、冲突解决、验证命令和 parent result SHA。

## 11. 最终判断

本轮二次开发最有价值的不是新增了多少页面，而是建立了三条平台级不变量：

1. **身份和权限必须有明确 Client 上下文，并失败关闭。**
2. **文件字节走 OSS 数据面，业务与后端只管理授权、策略、状态和引用。**
3. **对外通知走统一合同，但不虚构可靠队列语义。**

这些方向与上游模块化结构兼容，且大量实现采用新增模块/目录，说明 fork 仍处于可治理阶段。未来最优先的不是继续加功能，而是把父快照、upstream mirror、CI、typecheck、E2E、迁移和文档治理补齐。完成 P0/P1 后，适配度可从 74/100 提升到约 88/100；在此之前扩大业务面会让当前可控的 121 个修改型热点逐渐变成升级负担。

## 12. 审查与证据索引

- Backend review: `<Path>{roots.state}/specdev/changes/2026-08-24-upstream-fork-upgrade-assessment/reviews/CR-001.md</Path>`
- Frontend review: `<Path>{roots.state}/specdev/changes/2026-08-24-upstream-fork-upgrade-assessment/reviews/CR-002.md</Path>`
- Frozen source: `<Path>{roots.state}/specdev/changes/2026-08-24-upstream-fork-upgrade-assessment/source.md</Path>`
- Triage: `<Path>{roots.state}/specdev/changes/2026-08-24-upstream-fork-upgrade-assessment/triage.md</Path>`
- Long-term ADRs: `<Path>{roots.state}/specdev/adr/</Path>`
- Existing fork hotspot map: `<Path>docs/upstream/customization-map.md</Path>`
