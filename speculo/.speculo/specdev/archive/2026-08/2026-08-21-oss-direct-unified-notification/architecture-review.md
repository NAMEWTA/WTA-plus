---
artifact: architecture-review
change: 2026-08-21-oss-direct-unified-notification
status: consensus
---

# Architecture Review: OSS 直传与统一对外通知

- **决策记录：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/architecture-review.md</Path>`
- **可视化报告：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/architecture-review.html</Path>`

## 1. 审查压力与范围

- **触发目标：** 对已完成实现做架构健康扫描，寻找下一次 OSS 或通知变化会重复付出协调成本的位置。
- **审查入口：** 用户指定 change；以 backend `1914c4917..d836894f9` 的 17 个提交、frontend `adf5a0c..866e5ba` 的 3 个提交为压力证据。
- **相关行为或 Ticket：** T-03 OSS 生命周期、T-05 前端直传迁移、T-08 通知附件、T-09 监控、T-10 调用迁移。
- **领域/ADR 输入：** 当前 change 的 CONTEXT 与 ADR-001 至 ADR-009；永久 context/ADR namespace 为空。
- **不审查范围：** RBAC 历史实现、支付 change、Bucket/Provider 部署配置、生产数据迁移、无近期变化压力的其他模块。
- **成功标准：** 候选必须有真实调用或测试证据，通过删除测试，并能以 locality、leverage 或测试表面说明实际收益。
- **热点依据：** 用户指定；Git 历史只用于确认变化压力，不扩大范围。

## 2. 当前结构地图

### Modules 与 Interfaces

- `NotifyDispatcher` module 以 `NotifyClient.send` 作为单一外部 interface，内部协调 Channel adapter、幂等、附件快照、上下文与 Event；它已经具备较高 depth。
- `OssUploadService` module 以 init/sign/resume/complete/abort 控制面为 interface，内部使用 Ticket store、Provider adapter、元数据 adapter 与身份解析 seam。
- `OssLifecycleManager` module 拥有 TEMP、引用、清理与短时下载实现，但其 `bind/unbind` interface 依赖每个业务写入 module 主动、正确且成对调用。
- 前端 `uploadDirectToOss` module 以一次上传为 interface，隐藏 SINGLE/MULTIPART、断点续传、Part 重签、进度与取消实现；四类调用者复用同一实现。

### 数据、控制与错误流及 Seams

- 上传流：前端上传 module -> 控制面 HTTP -> `OssUploadService` -> Provider adapter -> TEMP `sys_oss`。
- 引用流：业务写入 module -> 业务表保存 -> 可选的 `OssService.bind/unbind`；此处是当前最明显的 leaking seam。
- 通知流：调用者 -> `NotifyClient` -> Dispatcher -> Channel adapter -> Event -> monitor persistence。
- 附件流：调用者权限 -> snapshot module -> Provider copy adapter -> Dispatcher -> Event -> monitor module 绑定引用；授权、复制和生命周期分布在多个位置。

### 变化热点、Locality 与测试表面

- backend change 新增约 9,968 行并触及 179 个文件；frontend change 新增约 1,321 行并触及 17 个文件。
- `fc902e082` 在 `SysUserServiceImpl` 和 `SysNoticeServiceImpl` 分别实现旧新 ossId 差异、`bind/unbind` 与事务协调；相同生命周期规则已经开始在业务 modules 之间重复。
- `b2d50248a` 的验证码审计收紧同时触及 Captcha 调用者、`NotifyRequest`、Dispatcher 正规化和 monitor persistence，显示审计策略穿过了通知发送 seam。
- 附件授权同时存在于 Demo Controller 与 snapshot module，显示授权知识没有单一 locality。
- 排除 `NotifyDispatcher`：删除后验证、幂等、快照、Provider 结果聚合与 Event 会扩散到所有调用者，说明当前 module 已提供真实 leverage。
- 排除前端 `uploadDirectToOss`：删除后续传、重签、并发 Part 和取消会扩散到 FileUpload、ImageUpload、Editor 与头像调用者，说明当前 module 已提供真实 leverage。

## 3. 候选提案

### AR-001: Deepen business-owned OSS references

- **文件：** `<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/system/api/OssService.java</Path>`；`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/service/OssLifecycleManager.java</Path>`；`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysUserServiceImpl.java</Path>`；`<Path>plus-ui-namewta/src/views/system/user/profile/userAvatar.vue</Path>`
- **问题：** TEMP 到业务引用的转换依赖每个业务写入 module 记住旧新 ossId 差异、`bind/unbind` 顺序、真实表名和失败语义；编译器不会暴露遗漏。`fc902e082` 已在用户头像与公告 module 分别手写同一协调规则，形成直接重复压力。
- **解决方案：** 将业务记录保存、旧新附件差异和引用生命周期放进同一个 deep module，使调用者不再协调两个独立 interface；报告阶段不决定具体 interface。
- **收益：** locality 集中在业务写入 module；一次实现覆盖新增/替换/删除；测试穿过一个 interface；TEMP 误清理风险下降。
- **建议强度：** Strong
- **依赖类别：** mock
- **删除测试：** 删除现有调用者纪律不会产生编译失败，只会让已保存 ossId 保持 TEMP；若删除深化后的 module，保存、差异、引用与补偿复杂性会重新扩散到每个写入调用者。
- **ADR 冲突：** 无；该候选落实 ADR-005，而不改变引用不是 ACL、无通用 bind、真实物理表名等决定。

#### Before / After

- **Before：** 上传返回 ossId；业务保存与生命周期 interface 分离，调用者必须知道调用顺序、refType/refId 和旧值。
- **After：** 一个业务写入 deep module 隐藏记录写入、附件差异、bind/unbind 与补偿，外部仍只观察业务结果。

- **推荐：** 最佳推荐。近期修复已证明每个带附件业务都会重复面对同一协调问题。
- **访谈状态：** consensus
- **用户结论：** 用户选择 AR-001。Round 12 确认 Business OSS Owner 位于各业务写入 module；共享能力只承载机械规则。项目为无历史负担的基座，只验证全仓 owner 清单与 fresh baseline，不做兼容或回填。Round 13 确认同事务 fail-closed；逻辑删除按 owner 的真实恢复合同决定解绑或保留。Round 14 确认以显式 owner 清单和聚焦架构/契约测试约束未来新增持久化 ossId owner，不使用运行时扫描或动态回调。frontier 为空后，用户明确确认最终共识。
- **ADR 影响：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/ADR.md</Path>` 中的 ADR-010

### AR-002: Localize notification audit policy

- **文件：** `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-notify/src/main/java/org/dromara/common/notify/model/NotifyAuditPolicy.java</Path>`；`<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-notify/src/main/java/org/dromara/common/notify/model/NotifyRequest.java</Path>`；`<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-notify/src/main/java/org/dromara/common/notify/core/NotifyDispatcher.java</Path>`；`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/notify/service/impl/SysNotifyMonitorServiceImpl.java</Path>`；`<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/java/org/dromara/web/controller/CaptchaController.java</Path>`
- **问题：** 当前敏感审计收紧让持久化策略从 Captcha 调用者穿过 `NotifyRequest` 和 Dispatcher 正规化，再由 monitor module 解释；发送调用者因此必须了解监控存储，而一次策略变化跨越三个 modules。
- **解决方案：** 将敏感分类、保留决策、脱敏与持久化集中到一个 deep monitoring module，使 delivery interface 只承载发送与可观察结果；报告阶段不决定具体 interface。
- **收益：** locality 集中在监控策略；发送 interface 缩小；策略测试命中一个表面；安全取舍与 ADR 同处一地。
- **建议强度：** Worth exploring
- **依赖类别：** in-process
- **删除测试：** 删除 `NotifyAuditPolicy` 会从调用者、Dispatcher 和 monitor 同时消除分支，但敏感保留需求会重新散落；深化后删除 monitoring module 才会让策略复杂性重新出现。
- **ADR 冲突：** ADR-008 明确要求 OTP、Token、正文和完整 Target 明文永久保存；当前 `REDACT_SENSITIVE` 改动反转该决定，值得在深化前重审而不是静默迁移。

#### Before / After

- **Before：** caller 选择存储策略，delivery request 携带策略，Dispatcher 保留策略，monitor persistence 执行策略。
- **After：** delivery module 发布稳定事实；deep monitoring module 内部完成分类、保留、脱敏和落库。

- **推荐：** 先解决 ADR 冲突，再决定是否深化；不要把当前未提交改动直接固化成新的公共 interface。
- **访谈状态：** unselected
- **用户结论：** 待选择
- **ADR 影响：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/ADR.md</Path>` 中的 ADR-008

### AR-003: Deepen authorized attachment snapshots

- **文件：** `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-demo/src/main/java/org/dromara/demo/controller/MailSendController.java</Path>`；`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/notify/attachment/SystemNotifyAttachmentSnapshotService.java</Path>`；`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/notify/attachment/NotifyAttachmentObjectStore.java</Path>`；`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/notify/service/impl/SysNotifyMonitorServiceImpl.java</Path>`
- **问题：** 附件使用权同时在 Controller 和 snapshot module 检查，snapshot implementation 直接读取 ambient `StpUtil`，而复制、补偿和最终引用绑定分散在同步发送与 best-effort monitor 两段。
- **解决方案：** 深化现有 snapshot module，使授权后的源解析、Provider copy、补偿和快照生命周期拥有单一 locality；Provider 仍作为内部 adapter，报告阶段不决定具体 interface。
- **收益：** 授权知识集中；ambient seam 不再泄漏；复制补偿测试命中一个 interface；调用者不重复权限字符。
- **建议强度：** Worth exploring
- **依赖类别：** mock
- **删除测试：** 删除现有 snapshot module 会把源查询、复制、元数据、物化和补偿扩散到 Dispatcher 或调用者，说明应深化现有 module；当前双重权限检查则可以删除一处而不消除复杂性，证明 locality 仍不完整。
- **ADR 冲突：** 无；必须保留 ADR-009 的发送前复制、失败零 Provider 调用、Provider 失败保留快照和异步落库失败可 TEMP 清理。

#### Before / After

- **Before：** Controller 权限 -> snapshot 权限 -> Provider copy -> Dispatcher -> Event -> monitor bind；同一附件生命周期跨多个 seams。
- **After：** caller 穿过一个授权快照 seam；deep module 内部协调 source、copy adapter、补偿和生命周期，Dispatcher 只消费已准备资源。

- **推荐：** 在 AR-001 的业务引用形状明确后再探索，避免两个生命周期 module 形成重叠 ownership。
- **访谈状态：** unselected
- **用户结论：** 待选择
- **ADR 影响：** 无

## 4. 最佳推荐

首先探索：AR-001。原因：近期提交已在两个业务 modules 中重复实现旧新引用协调，而每新增一个带附件业务都会继续承担同一不可由编译器验证的责任。

## 5. 下一步

- AR-001 的完整 frontier 已遍历，用户已明确确认最终共识。
- 未选择候选不提前设计 interface，也不转为执行 Ticket。
- 下一 work 为 `<Path>{roots.workflows}/specdev/T-tickets/T-tickets.md</Path>`，用于拆解 AR-001；本次不自动执行。
