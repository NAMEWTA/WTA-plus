# Backend review — wta-api / wta-modules

> 2026-09-18 已按当前工作树重新核对并修订建议。原报告中的2026-09-14命令属于历史记录；当前命令结果见 [re-review-command-results.json](re-review-command-results.json)，逐票结论见 [re-review.md](re-review.md)。本次单人串行，仅改change；无旧版兼容要求。


审查日期：2026-09-14。范围是 `backend/wta-api`、`backend/wta-modules/**`（system、workflow、job、demo、ai、profile/person、profile/enterprise、notify、sso、third）及它们实际调用的 common/API seam。只读审查；未修改产品代码、既有文档或构建输出，也未运行 Maven/build。源码、POM、XML、测试和初始化 DML 是事实源；grep 线索没有作为确认漏洞。

## 覆盖矩阵与方法

| 模块 | 入口/实现覆盖 | 结果 |
|---|---|---|
| `wta-api` | public DTO、System/Workflow/Notify/Third/SSO contracts 及调用方向抽查 | 未发现可确认破坏性 API；需补 API contract 自动检查 |
| `wta-system` | SSO 组装、用户/角色/部门事务及数据权限热点、OperLog 持久链 | 发现系统性日志/代理地址问题（根审查 R-01/R-03）；部门循环与自服务授权需纳入 ticket |
| `wta-workflow` | `FlwTaskController`、`FlwTaskServiceImpl` 任务读/写/节点读取及 Warm-Flow PermissionHandler | 发现两个读取入口缺显式 owner 校验、CRUD legacy method |
| `wta-profile/person` | 申请、材料要求、工作流 listener/usecase、兼容构造/default 方法 | 发现兼容浅接口和 UI 所需材料路径断裂（前端代理已确认） |
| `wta-profile/enterprise` | 申请/材料/转移分层及 API 依赖 | 结构 validator 通过；共享材料/UI 缺口见跨链 ticket |
| `wta-notify` | Intent/Delivery/Outbox claim/worker/dispatch/callback/时间转换 | 发现回调内存幂等、非原子结果写回、同步 wake，以及分层检查误报 |
| `wta-sso` | authorize/token/revoke、PKCE、Redis 会话、Cookie、admin identity | 发现 bearer 随机性、redirect 编码、Cookie secure 与日志风险 |
| `wta-third` | Gateway、动态 path/header/body、credential、retry、Redisson 限流 | 发现 semaphore 生命周期/配置动态性风险；retry HTTP 状态合同留作验收问题 |
| `wta-job`/`wta-ai`/`wta-demo` | 入口、样例任务及 Demo CRUD | Demo 明确保留 legacy；随机任务不能作为生产可靠性样例 |

已运行的非写入检查：`node .agents/skills/namewta-fullstack-development/scripts/validate-module-mode.mjs backend/wta-modules/wta-profile/wta-profile-person --mode layered`（exit 0，168 Java）；enterprise（exit 0，130）；third（exit 0，71）；notify（exit 1，见 B-09）；sso（exit 1，见 B-09）。未运行 `./mvnw test`、package、Docker/Redis/MySQL/HTTP/browser 集成和 UI E2E。

## Findings（按优先级）

### B-01 — SSO/授权码和会话 bearer 使用非密码学随机源

- **严重度/置信度**：P1 / confirmed。
- **证据**：<Path>backend/wta-modules/wta-sso/src/main/java/org/namewta/sso/service/SsoAuthorizationService.java</Path> 178-181 的 `newCode()` 使用 `ThreadLocalRandom.current()`；<Path>backend/wta-modules/wta-sso/src/main/java/org/namewta/sso/adapter/store/RedisSsoSessionStore.java</Path> 28-31 将两个 `IdGeneratorUtil.nextLongId()` 拼为会话 ID。`IdGeneratorUtil` 的默认实现是可按时间/worker 推断的雪花 ID，适合数据库主键而非 bearer。
- **触发/后果**：会话 ID 的实现没有提供 bearer 所需的密码学不可预测性保证，带来 session guessing 风险；若会话被猜中，该身份可被用于发起新的 PKCE 授权。授权码使用 ThreadLocalRandom 也不满足 CSPRNG 要求，但单独猜中 code 仍受 verifier 防护，不能声称可直接兑换他人的 code。未执行攻击验证，也没有宣称观察任意 ID 即可成功预测 token。
- **反证检查**：PKCE S256、client/redirect/version 条件在 <Path>backend/wta-modules/wta-sso/src/main/java/org/namewta/sso/service/SsoAuthorizationService.java</Path> 103-136 确实存在；它降低离线 code 猜测收益，不能证明 bearer 随机性安全。当前测试仅验证协议行为，未做熵/不可预测性验收。
- **改造**：仅将授权码与SSO session随机源替换为JDK SecureRandom的32字节URL-safe值，保留现有存储、TTL与一次性consume。不要求哈希存储迁移、新令牌框架或统计碰撞证明。数据库主键不改。
- **架构评估**：保留 `SsoSessionPort` 的小 interface，直接使用 JDK `SecureRandom`；没有必要再造身份生成 wrapper。dependency class=进程内；strength=Strong。
- **ADR/ticket**：与 <Path>{roots.state}/specdev/adr/0069-oauth-authorization-code-pkce-s256.md</Path>、<Path>{roots.state}/specdev/adr/0070-access-token-is-sa-token-target-client-extras.md</Path> 一致（强化实现，不改合同）。建议 `SEC-SSO-001`。
- **验收**：审查真实生成路径采用CSPRNG；验证长度、编码和协议负向场景。有限黑盒抽样或100k无碰撞不能证明密码学不可预测性。

### B-02 — SSO redirect URI 返回值直接拼接，state 未 URL 编码

- **严重度/置信度**：P2 / confirmed。
- **证据**：<Path>backend/wta-modules/wta-sso/src/main/java/org/namewta/sso/service/SsoAuthorizationService.java</Path> 184-187 的 `appendQuery` 直接拼接 `code` 和 `state`；`redirectUri` 只做 trim 和白名单精确比较。
- **触发/后果**：state 为 `a&b=c+1#x` 时，返回 query 的 state 不再与原值相同，回调会被拆分或截断；合规客户端应拒绝，表现为 SSO 登录失败。当前自身 SPA 生成 URL-safe state，降低触发概率，但 public interface 并未限制该字符集。没有将此问题认定为可确认开放重定向漏洞。
- **反证检查**：`requireExactRedirect` 拒绝 `*` 并精确匹配，故不是“任意 redirect”漏洞；问题限于构造回调 URI。
- **改造**：规范构造query并逐参数编码，保留已有合法query，拒绝fragment与冲突保留参数。authorize当前还对state调用trim；state应作为opaque值精确回传，覆盖首尾空格与特殊字符。
- **架构评估**：小型纯函数是合适内部 seam，删除 ad-hoc branch 可提升 locality；dependency class=进程内；strength=Strong。
- **ADR/ticket**：`0069` 要求 authorization code+PKCE；建议 `SEC-SSO-002`，与 `R-01` 日志脱敏 ticket 联动。

### B-03 — SSO Cookie 未设置 Secure，跨 HTTP 部署会泄露会话

- **严重度/置信度**：P1 / likely（生产 TLS/代理终止需 runtime 验证）。
- **证据**：<Path>backend/wta-modules/wta-sso/src/main/java/org/namewta/sso/support/SsoSessionCookie.java</Path> 24-31 仅设置 HttpOnly、Path、SameSite=Lax、Max-Age；没有 `secure(true)`，`SsoProperties` 也无 secure 配置。
- **触发/后果**：浏览器在 HTTP origin 或错误降级链路发送 `Sso-Token`；攻击者可被动窃取 8 小时 SSO session。HttpOnly 只防脚本读取，不能防网络窃听。
- **反证检查**：`release-artifacts` compose 常以 TLS 代理外置，当前源码没有可信 proxy/TLS invariant；无法把“部署一定 HTTPS”当证据。
- **改造**：生产启用Secure，开发HTTP使用明确配置；Cookie保持host-only、Path=/、HttpOnly、SameSite=Lax。创建/注销一致。SSO独立Origin采用同源/sso反代，不因“跨Origin”自动引入跨站Cookie、共享domain或SameSite=None。
- **ADR/ticket**：与 <Path>{roots.state}/specdev/adr/0069-oauth-authorization-code-pkce-s256.md</Path>/<Path>{roots.state}/specdev/adr/0074-independent-sso-admin-dual-surface.md</Path> 一致；建议 `SEC-SSO-003`。

### B-04 — Notify Dispatch 的 Delivery、Attempt、Outbox 完成写回不是原子操作

- **严重度/置信度**：P1 / confirmed。
- **证据**：<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/DispatchNotificationService.java</Path> 148-192 依次执行 `renewLease`、`dao.update(delivery)`、`dao.insert(attempt)`、再次 `dao.update(delivery)` 和 `dao.finishOutbox(outbox)`；`dispatch` 方法及其调用的 DAO 没有 `@DSTransactional`。<Path>backend/wta-modules/wta-notify/src/main/resources/mapper/notify/NotifyOutboxMapper.xml</Path> 31-39 的 finish 仅以 owner/token 条件更新 outbox。
- **触发/后果**：若 Delivery 的第一次 `update` 成功后 `insert(attempt)` 失败，记录永久缺少 Attempt；Outbox 仍为 PROCESSING。后续 reclaim 发现 Delivery 不再 PENDING 会直接 mark DONE（58-62），从而没有重建 Attempt/refreshIntent，聚合状态可能永久陈旧。若重试状态恢复前中断，失败任务也可能被误当完成。结果写回前的崩溃还可能导致重发，但 provider 幂等性须单独验收。
- **反证检查**：owner/token fence 能阻止旧 worker 在 finish 阶段覆盖新 owner；它不能回滚不同表已提交的写入。Delivery/Intent 的普通 updateById 没有该 fence，更新的 version 字段也没有 `@Version`。代码注释声称“短事务更新”，实际调用链没有该事务。
- **改造**：结果、Attempt、Outbox及Intent聚合由代理UseCase的短DSTransactional提交，Provider I/O在外。所有结果写入先在事务内核验owner/token/状态/未过期lease，finish=0回滚；dispatch与callback保持相同锁序和单调状态。当前deliveryId已经作为requestId/idempotencyKey，不新造同义键。renew/finish SQL没有过期条件也需修复，禁止旧owner在TTL后自行复活。
- **ADR/ticket**：符合 <Path>{roots.state}/specdev/adr/0044-outbox-write-wake-and-slow-poll.md</Path>、<Path>{roots.state}/specdev/adr/0045-redis-cross-process-outbox-wake-reuses-claim.md</Path> 的 lease safety 要求；建议 `NOTIFY-OUTBOX-001`。

### B-05 — Notify 回调幂等状态只在进程内，且在数据库提交前记事件

- **严重度/置信度**：P1 / confirmed。
- **证据**：<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/ProviderCallbackService.java</Path> 27 使用 `ConcurrentHashMap seenEvents`；59-60 在 `dao.updateDeliveryStatus` 成功后先记 event，再 refresh aggregate；外层 `@DSTransactional` 位于 <Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/ProviderCallbackUseCase.java</Path> 25。若随后聚合刷新或事务提交失败，内存 map 仍保留 event；实例重启、扩容或跨实例重复不会共享。eventId 也没有 provider/channel 命名空间。
- **触发/后果**：事务回滚后相同 event 在 10 分钟内被错误丢弃，而签名时间窗只有 5 分钟，原回执可能无法恢复；两个 provider 都使用 eventId=`1` 时，后到的有效回执也被丢弃。跨实例幂等现主要依赖 SQL 单向状态条件，不能把此 map 宣称为全局幂等。
- **反证检查**：`updateDeliveryStatus` 有 from-status 条件且状态等级单向升级，能防部分乱序；不能替代 durable event unique key 和原子更新。
- **改造**：新增归属 Notify 的 callback-event 表（`provider_key,event_id` 唯一）；在同一事务插入 event，冲突即 no-op；聚合刷新只在成功更新后执行。删除 `ConcurrentHashMap` 和过期清理 branch（代码 judo）。
- **ADR/ticket**：符合通知规范要求 callback eventId 幂等；建议 `NOTIFY-CB-001`，补 MySQL 并发/回滚/多实例验收。

### B-06 — Third semaphore permit 无租约/TTL，进程崩溃后可永久耗尽并阻断出站

- **严重度/置信度**：P1 / likely（需 Redis crash/restart runtime 验证）。
- **证据**：<Path>backend/wta-modules/wta-third/src/main/java/org/namewta/third/adapter/resilience/ThirdResiliencePolicyAdapter.java</Path> 52-59 使用 Redisson `RSemaphore.tryAcquire()`，仅在正常 `finally` 通过 `ThirdLimitLease.close()` release；没有 lease watchdog、TTL 或 owner fencing。
- **触发/后果**：进程在acquire后被kill且未执行finally时，已扣permit没有过期回收，可持续耗尽并拒绝调用。正常异常已有release；网络分区或线程停顿是否永久泄漏取决于恢复结果，不能一概断言永久。
- **反证检查**：正常异常路径会 release；这只能覆盖有机会运行 finally 的情况。`RRateLimiter` 也依赖 Redis 可用性，但不会持有永久 semaphore permit。
- **改造**：优先使用现有Redisson可过期permit及permitId释放，TTL覆盖有界HTTP与重试总预算。仅在已证实长调用需求下续租，不新增watchdog/reclaim框架。kill、部分申请失败、迟到释放用真实Redis验证；本地租约不能保证远端处理在超时后终止。
- **ADR/ticket**：建议 `THIRD-RESILIENCE-001`；不改 `ThirdPartyGateway` public interface，仅替换 adapter。

### B-07 — Third semaphore/rate-limit 参数的动态配置需要明确失效边界

- **严重度/置信度**：P3 / needs-runtime（不作为已确认故障）。
- **证据**：<Path>backend/wta-modules/wta-third/src/main/java/org/namewta/third/service/ThirdProviderService.java</Path> 29-51 与 <Path>backend/wta-modules/wta-third/src/main/java/org/namewta/third/service/ThirdEndpointService.java</Path> 33-83 保存 rate/concurrency/retry 后调用 `configCache.evict`；<Path>backend/wta-modules/wta-third/src/main/java/org/namewta/third/adapter/resilience/ThirdResiliencePolicyAdapter.java</Path> 54-63 使用 Redisson `trySetPermits/trySetRate`（只在 key 尚未初始化时生效）。
- **触发/后果**：已存在的 Redis limiter key 是否随管理配置更新收敛，取决于 cache snapshot、Redisson key 生命周期和实例顺序；若不收敛，运维修改限额后仍按旧阈值运行。
- **反证检查**：服务层明确 evict，说明设计意图是下一次读取新配置；源码未提供更新既有limiter状态的路径；固定key的配置缺口可见，具体Redisson行为与切换时序仍需Redis集成验证。
- **改造**：保留稳定provider/endpoint限额key，实测后在配置更新路径原子调整限额并明确生效时点。禁止每配置版本生成一份完整新额度，也禁止每请求DEL/reset。无需新建分布式配置发布系统。

### B-08 — Profile UseCase 为过渡测试保留大量 default throwing 方法，形成浅 interface 和运行时陷阱

- **严重度/置信度**：P2 / confirmed（代码 judo 候选）。
- **证据**：<Path>backend/wta-modules/wta-profile/wta-profile-person/src/main/java/org/namewta/profile/person/usecase/PersonRebindUseCase.java</Path> 16-27 有 4 个 `@Deprecated` default 方法抛 `UnsupportedOperationException`，36-48 的新签名再委托旧方法，56 仍有旧事件入口抛异常；`PersonApplicationUseCase`、`PersonAdminUseCase` 及 enterprise 对应接口有同一模式。生产实现同时保留旧构造桥，如 <Path>backend/wta-modules/wta-profile/wta-profile-person/src/main/java/org/namewta/profile/person/service/PersonApplicationService.java</Path> 76-96。
- **触发/后果**：任何漏传 userId/operatorId 的调用编译可通过却在运行时失败；接口方法数量、适配器桥和测试 fixture 复杂度扩大，真实 seam 不清晰。
- **反证检查**：当前 Controller/Listener 使用显式 owner 方法，未发现生产调用旧 default；这支持“可删除”而非“已被触发漏洞”。
- **改造**：同步仓内调用与测试后直接删除旧default/兼容重载；显式身份方法变为必需抽象实现。保留Clock/port注入等仍有真实测试用途的构造，不因存在第二构造就删除。用户已排除旧版兼容，无compatibility adapter或等待版本。
- **ADR/ticket**：符合 `EX-002 Profile 测试兼容构造桥` 的删除条件；建议 `PROFILE-ARCH-001`，先静态调用图再移除。

### B-09 — layered validator 对注释/命名误报，且生产包名改名后仍漏检旧依赖

- **严重度/置信度**：P2 / confirmed（门禁工具缺陷）。
- **证据**：<Path>.agents/skills/namewta-fullstack-development/scripts/validate-module-mode.mjs</Path> 约 90-145 的 `forbiddenByLayer` 和 `isBoundaryImport` 大量匹配 `org.dromara.*`；生产已使用 `org.namewta.*`。同脚本约 240-251 事务检查直接对完整 source 用 `/@DSTransactional\b/`，所以 `wta-notify` 的 Javadoc“当前 `@DSTransactional` 内”被报告为真实注解；service 目录检查又将语义明确的 runtime service/support publisher 视为违规。SSO 还被 support 使用 `ResponseCookie` 报错。
- **触发/后果**：门禁可能 exit 0 却漏掉 `org.namewta` 越层依赖；也可能在真实结构无问题时 exit 1，开发者会绕过或关闭门禁，降低 leverage/locality。
- **反证检查**：逐项命令结果：person/enterprise/third exit 0；notify exit 1 两条，其中第一条是注释误报；sso exit 1 的 `SsoSessionCookie` 依赖规则与项目确有架构争议，但应由 adapter/transport 设计明确裁决。该 finding 不把 validator 输出直接当产品漏洞。
- **改造**：脚本去掉注释/字符串后再做 annotation 规则；项目前缀必须匹配 `org.namewta`，保留真正上游 `org.dromara.warm.flow` 的身份。通过 fixtures 检验正反例。另两条真实规则命中按原合同修复：将 Cookie 渲染移入 HTTP adapter，wake publisher 移入 event adapter；不把命名失败全部视作误报，也不放宽 support 禁止 Spring 的硬约束。
- **ADR/ticket**：建议 `ARCH-GATE-001`，同步更新 `engineering-standards` 的验证证据；不修改业务模式登记。

### B-10 — Workflow 任务节点读取入口没有统一调用 `checkTaskReadAccess`

- **严重度/置信度**：P1 / likely（需 MockMvc 与 Warm-Flow 权限 handler runtime 验证）。
- **证据**：<Path>backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/service/impl/FlwTaskServiceImpl.java</Path> 343-397 的 `getBackTaskNode` 直接按 taskId 读取流程定义、历史任务和可驳回节点；569-611 的 `getNextNodeList` 直接按 taskId 合并实例变量并返回节点/办理人。显式 `checkTaskReadAccess` 位于 749-775，仅被 `selectById`（约 520）和 `currentTaskAllUser`（约 725）调用。Controller 的 `/getNextNodeList`、`/getBackTaskNode/{taskId}` 没有独立 `@SaCheckPermission`。
- **触发/后果**：已登录但非办理人、发起人或监控角色的用户若猜中 taskId，可能枚举流程节点、变量派生办理人或可驳回目标，形成跨流程信息泄露并帮助后续操作；是否可进一步写入取决于 Warm-Flow 内部授权。
- **反证检查**：`WorkflowPermissionHandler` 只负责办理阶段的 `permissions/getHandler/convertPermissions`；不能证明这两个读取 API 会调用它。需运行真实依赖确认。
- **改造**：节点读取入口复用本类已有checkTaskReadAccess，不为两个调用新增“深模块”。保留办理handler与已有任务锁；用非参与人/发起人/监控及Client负向场景确认真实效果。
- **ADR/ticket**：建议 `WORKFLOW-AUTH-001`；与 workflow classic 冻结不冲突，只收紧入口授权。

### B-11 — Classic Demo/Workflow/System 仍暴露 PUT/DELETE CRUD，违反当前无兼容升级目标

- **严重度/置信度**：P2 / confirmed（合同/清理项；不是所有旧协议都应立即改）。
- **证据**：<Path>backend/wta-modules/wta-demo/src/main/java/org/namewta/demo/controller/TestTreeController.java</Path> 110-145 使用 `@PutMapping` edit 与 `@DeleteMapping` remove；<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/controller/system/SysDeptController.java</Path> 91-142 使用 PUT/DELETE；<Path>backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/controller/FlwTaskController.java</Path> 204-214 使用 PUT updateAssignee。工程合同 API-005 规定 CRUD 查询 GET、变更 POST、每个 POST 有准确 `@Log`，但模块登记允许 classic 存量暂时保留。
- **触发/后果**：新前端/SDK 复制旧 Controller 即继续产生不一致 method；无兼容升级目标要求清理旧 surface 时，文档/菜单/OpenAPI 会存在两套语义。
- **反证检查**：这些模块已登记 classic，工程规则要求“触及范围迁移”而非全仓机械重写；因此建议按垂直切片迁移，不能报告为当前线上漏洞。
- **改造**：先建立 route->consumer->test map，将实际 CRUD endpoint 统一为 POST 并补 `@Log` 敏感字段排除；同步前端/文档/OpenAPI，删除旧 mapping，不保留双路由兼容。Demo 仅作为模板样例时应直接重写或移入 docs/fm。
- **ADR/ticket**：`DEC-006`/`API-005`；建议 `API-MIGRATION-001` 分 system/workflow/demo 子票。

### B-12 — System 部门移动未拒绝选择自身后代，树 ancestors 可能形成环

- **严重度/置信度**：P1 / confirmed（代码路径成立；未运行真实 DB 树回归）。
- **证据**：<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/controller/system/SysDeptController.java</Path> 99-108 只检查 `dept.getParentId().equals(deptId)`（自父）及禁用条件；<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/service/impl/SysDeptServiceImpl.java</Path> 337-371 的 `updateDept` 读取新父 ancestors 并 `updateDeptChildren`，未见“新父是当前节点后代”的拒绝。规则只更新祖先串，不能阻止环。
- **触发/后果**：将 A 移到 A/B 的后代 B 下，A ancestors 包含 B，子树更新后形成循环；`find_in_set` 查询、权限数据范围和递归菜单可能无限/错配，且后续删除/移动无法恢复。
- **反证检查**：控制器拒绝自父，删除也检查 children；未发现 descendant check。需构造三层树在 MySQL 验证。
- **改造**：updateDept已有Spring @Transactional，本次实质修改按规范统一为DSTransactional并核对整条事务链。优先以简单同树修改锁串行化，锁后重新检查父存在、自父及后代关系，再原子更新ancestors。相关移动入口遵守同一锁；不添加任意深度限制或通用树框架。
- **ADR/ticket**：`SEC-004`/BE-CRUD-008 树不变量；建议 `SYSTEM-DEPT-001`。

### B-13 — Notify 本地 after-commit wake 在同一 JVM 同步执行，可能把 Redis/claim/dispatch 复杂度压入提交线程

- **严重度/置信度**：P2 / likely（Spring event multicaster 配置需 runtime 验证）。
- **证据**：<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotifyOutboxWakePublisher.java</Path> 43-70 在 `@DsTxEventListener(AFTER_COMMIT)` 中发布 Redis 后同步 `localEvents.publishEvent(signal)`；<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/adapter/worker/NotifyOutboxWorker.java</Path> 59-84 的 listener 直接 drain、claim 并 dispatch，后者可能等待供应商 I/O。
- **触发/后果**：若默认 multicaster 为同步，业务 POST 在 commit 后仍等待 outbox claim、限流和第三方通知；高延迟供应商会占满请求线程，且提交后副作用失败只靠日志/轮询，难以观测首投延迟。
- **反证检查**：ADR-0044/0045 允许同 JVM 辅信号且要求跨进程 wake；代码没有显式 `@Async` 或 TaskExecutor 证据，故标 likely 而非 confirmed。
- **改造**：保留 Redis 主唤醒；本地事件仅投递到受控 bounded executor，或直接删除本地信号（由 Redis+poll 保证）；明确 executor owner、队列上限、拒绝策略和 shutdown。禁止 after-commit 线程执行 provider I/O。
- **ADR/ticket**：必须与 `0044`/`0045` 一起验收，建议 `NOTIFY-WAKE-001`。

### B-14 — Demo Tree 保存/删除校验仍是 TODO，不能作为 CRUD/树结构参考实现

- **严重度/置信度**：P2 / confirmed（示例模块质量问题）。
- **证据**：<Path>backend/wta-modules/wta-demo/src/main/java/org/namewta/demo/service/impl/TestTreeServiceImpl.java</Path> 104-121 的 `validEntityBeforeSave` 和删除 `isValid` 分支均仅 TODO；Controller 又暴露完整新增/编辑/删除。
- **触发/后果**：无效parent、成环移动、删除有子节点等行为缺少规则；作为参考会复制cycle/delete guard缺失。名称唯一性不是已声明业务规则，不凭TODO新增唯一约束。
- **反证检查**：Demo 被登记为 classic 示例，非核心生产域；因此优先清理示例或明确标注，而非把 TODO 当整个 system 的漏洞。
- **改造**：优先完善 TestTree 的真实树不变量，让示例具备可复制价值；如果不保留该样例则同时删除其 controller/前端菜单/seed/测试夹具，而不是保留 TODO surface。`wta-demo` 中 rich-text、OSS 和 OpenAPI 样例具有真实测试用途，不因本问题删除整个 module。
- **ADR/ticket**：`BE-CRUD-008`/`DEC-006`；建议 `DEMO-CLEANUP-001`。

### B-15 — Enterprise transfer 期待同步投递成功，而 Notify 已改为总是入队，转移发码无法成功

- **严重度/置信度**：P1 / confirmed。
- **证据**：<Path>backend/wta-modules/wta-profile/wta-profile-enterprise/src/main/java/org/namewta/profile/enterprise/usecase/impl/EnterpriseTransferUseCaseImpl.java</Path> 22-27 给 `send` 加 `@DSTransactional`；<Path>backend/wta-modules/wta-profile/wta-profile-enterprise/src/main/java/org/namewta/profile/enterprise/service/EnterpriseTransferService.java</Path> 121-134 以 `NotificationMode.SYNC` 调用 `notify.submit` 后，仅在 status 为 ACCEPTED/DELIVERED 才继续（helper 293-296）。但 <Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotificationApplicationRuntimeService.java</Path> 43-136 始终写 Intent=QUEUED、Delivery=PENDING 和 Outbox，mode仅在62存库，submit不调用Provider。303-310仅ASYNC分支设置queued标志，不改变真实QUEUED status。
- **触发/后果**：即使源/目标用户和手机号均合法、SMS配置正常，首次 transfer send 仍收到 QUEUED，立即 revoke challenge 并抛 ENTERPRISE_TRANSFER_DELIVERY_FAILED；外层事务随后回滚通知/Outbox，短信根本无法发送，企业转移流程无法进入确认。
- **反证检查**：不是“SYNC一定做网络I/O”的推断；已追完整生产返回路径。<Path>backend/wta-modules/wta-profile/wta-profile-enterprise/src/test/java/org/namewta/profile/enterprise/service/impl/EnterpriseTransferServiceTest.java</Path> 117 和 <Path>backend/wta-modules/wta-profile/wta-profile-enterprise/src/test/java/org/namewta/profile/enterprise/service/impl/EnterpriseTransferMySqlRedisE2ETest.java</Path> 173 都用 stub 固定返回 ACCEPTED，没有覆盖真实 Notify interface 的 QUEUED 语义。
- **同类调用方映射**：<Path>backend/wta-modules/wta-profile/wta-profile-person/src/main/java/org/namewta/profile/person/service/PersonRebindNotificationService.java</Path> 148-164 也将真实 QUEUED 返回判成 FAILED，实际通知之后可能成功但 Profile 记录错误。<Path>backend/wta-admin/src/main/java/org/namewta/web/controller/CaptchaController.java</Path> 69-79、106-117 则忽略 receipt 并缓存验证码，不能把它描述为必然失败；但该消费者也必须纳入 SYNC 删除/状态语义变更影响清单。<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/NotifyTestSendService.java</Path> 77-89 返回 QUEUED，需要界面呈现为“提交待投递”而非“已送达”。
- **改造**：保留Redis PENDING_DELIVERY挑战；transfer记录与Notify intent/outbox同一MySQL事务保存notificationId关联。send接受QUEUED。状态查询/confirm经Notify公开query取得ACCEPTED或DELIVERED后才幂等激活，保持原有效期；QUEUED等待、失败撤销并重新发码。确认同时检查已提交transfer、用户/绑定版本与验证码，复用DB CAS和Redis消费token。数据库回滚残留Redis挑战不可确认，消费后回滚可重新发码恢复。不新增投影Outbox、事件总线或持久验证码副本。
- **架构评估**：dependency class=进程内（Profile→Notify interface）+本地可替换（MySQL/Redis）+真正的外部依赖（SMS Mock）；strength=Strong。删除“模式名为SYNC却仍入队”的shallow contract，保持小而准确的submit/status interface；增加真实跨module contract test而非再加thin wrapper。
- **ADR/ticket**：与 <Path>{roots.state}/specdev/adr/0044-outbox-write-wake-and-slow-poll.md</Path>、<Path>{roots.state}/specdev/adr/0045-redis-cross-process-outbox-wake-reuses-claim.md</Path> 的可靠异步方向一致；变更挑战激活时序须写本change ADR。建议 `PROFILE-TRANSFER-001`，与 `NOTIFY-OUTBOX-001` 联动。

### B-16 — 已拒绝候选：仅凭 wta-demo 的 broad POM 不能认定设计冗余

- **级别/置信度**：P3观察 / needs-runtime；不计入高置信finding或强制修复ticket。
- **证据**：<Path>backend/wta-modules/wta-demo/pom.xml</Path> 同时依赖20余common，但该module本就是多能力演示集合；<Path>backend/wta-admin/pom.xml</Path> 的full包含、core排除demo，已有产品组合seam。
- **反证/裁决**：没有逐一证明哪项依赖无消费者，也没有context启动压力或CVE证据，因此“删POM即可改善”的删除测试不成立。默认保留现有示例module；禁止据此直接拆成一批shallow module。
- **后续条件**：只有未使用依赖扫描、真实启动失败或明确产品组合需求证明收益时，再创建独立候选。dependency class=进程内；strength=Speculative。<Path>{roots.state}/specdev/adr/0018-static-crud-templates-over-runtime-generator.md</Path> 并不要求删除真实集成示例。

### B-17 — wta-third 已按 layered 实现却未登记在唯一后端模式表

- **严重度/置信度**：P2 / confirmed（文档/架构事实漂移）。
- **证据**：<Path>backend/wta-modules/wta-third/AGENTS.md</Path> 明确写 `controller/admin -> usecase/impl -> service -> dao -> mapper -> XML`；<Path>backend/wta-modules/wta-third</Path> 运行 layered validator exit 0（71 Java）。但唯一登记入口 <Path>.agents/skills/engineering-standards/references/project/03-backend-module-modes.md</Path> 的 layered 表只有 `wta-profile`、`wta-notify`、`wta-sso`，没有 `wta-third`。
- **触发/后果**：审查者、迁移工具和后续 ticket 无法判断 Third 是新 layered 模块还是“未登记默认 layered”；迁移策略、owner、兼容例外和验收门禁在文档中丢失，容易把第三方 adapter 错当 classic 依赖。
- **反证检查**：模式规则确实声明“未登记默认 layered”，所以这不是当前调用链失效；问题是唯一事实索引不完整，属于必须修复的架构文档漂移。
- **改造**：在模式表增加 `wta-modules/wta-third`、owner、layered 迁移与 validator 验证；同步 <Path>.agents/skills/engineering-standards/references/project/01-module-map.md</Path>、<Path>backend/wta-modules/wta-third/AGENTS.md</Path> 和 change/ADR 索引。删除重复的“各模块自行声明模式”段落，保留唯一登记入口。
- **架构评估**：dependency class=进程内（事实文档/静态检查）；strength=Strong。删除重复模式名单比增加另一份导航更有 locality；不改变 Third public interface。
- **ADR/ticket**：建议 `ARCH-DOC-001`，与 <Path>{roots.state}/specdev/adr/0054-public-repo-wta-plus-layout.md</Path> 及工程模式登记规则一致。

## 实施与验证

修订后的逐票写集、步骤和验收以 [tickets-map](../tickets-map.md) 及对应Ticket为准，不在专项报告中重复维护另一套计划。2026-09-18复核矩阵见 [re-review](re-review.md)，本次运行记录见 [re-review-command-results.json](re-review-command-results.json)。

confirmed表示源码链成立，不代表线上事故已复现。未运行Maven/pnpm/浏览器或真实数据库、Redis、Provider；不得将计划验收写成通过。仅修改本change，产品实现未开始。
