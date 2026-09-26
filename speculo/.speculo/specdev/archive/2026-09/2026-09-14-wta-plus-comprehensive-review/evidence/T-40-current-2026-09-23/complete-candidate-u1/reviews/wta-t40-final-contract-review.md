# T40 R3 完整合同只读审查（浏览器结果待定）

固定候选 `378f8481deae56bf7b6971ef1b1b2e66c47ca8fe`，tree `37a8e633d9f78d6285bf0dba51c51b053e3b981b`。**静态产品结论：未发现新的 AC-040 实现阻断；整票验收仍待 R3 当前候选真实 Chrome 两例、exact-source/JAR 与清理结果。** 本报告只读固定提交及已保存的安全证据；未构建、测试、启服务或改仓库。当前工作树有 Lead 构建产生的 `auto-imports.d.ts` dirty，不能拿工作树代替固定候选或声称浏览器 source-after clean。

## 固定差异与复用边界

`8a387192..378f8481` 的产品差异仅 `frontend/e2e/run-notice-retraction-real.py`、其离线测试及 README；backend、应用 TS/Vue、配置、SQL、依赖和 Skill 输入未变。`/tmp/wta-t40/r3-source-equivalence.json` 声明：R2 clean 的默认后端 889 执行通过/216 环境 skip、真实 Notice 18、共享通知 135、core/static 可依同字节输入复用；C1 的前端 760 tests 与三 App 产物按既存等价证据复用。此为**原候选结果的输入等价复用**，不是说 R3 在其 SHA 重新跑了这些门禁。R3 尚须新 Python25、exact-source full package/bundle、真实 Chrome 2 项零 skip/零 retry、source/JAR 一致与 owned 清理；Lead 提供离线25/full bundle 已通过的进度，但本报告不代替最终结果。

## AC-040 对照

| 合同 | 已有可核证的当前产品输入证据 | 尚待/界限 |
|---|---|---|
| 撤回先于发送执行权，当前版本无新请求；旧快照不变；重发布不受前版本影响 | `complete-candidate-r2/real18/result.json`：固定 R2 clean，18/0 failure/error/skip、Maven0、cleanup[]。方法含 `newlyCreatedExternalReadyTaskRetractedBeforeGateClosesWithoutProviderCall`、`realIntentRowLockMakesWorkerObserveCommittedRetractionOnAnotherConnection`、`retractionBeforeSecondThreadGateStopsItsClaimWithoutInAppSideEffects`、`republishedVersionCanDeliverWhileRetractedVersionAndSnapshotStaySeparate`、晚期 SQL 回滚与重复/手工 retry。A4 审查确认两个 JDBC 连接与真实 `SELECT ... FOR UPDATE` 阻塞，结果服务按 Notice→Intent→Outbox→Delivery 锁和租约栅栏结算。 | 外部 Provider 是测试替身；零调用断言证明本服务没调用 adapter，不证明任何远端供应商行为。R3 完整 Spring 的真实 IN_APP publish→Worker→retract 已在 R2 browser 前置达到并存了关系/Outbox/Delivery/三处 path 事实，但 R2 在合成 seed 排序阶段退出，不能当浏览器完成。 |
| 已进入外部 I/O 或已接受的结果不可追回；已送达站内信仍可读 | 真实 DB 18 例的 `retractionAfterProviderGatePreservesActualExternalResult` 三参数保留 ACCEPTED/UNKNOWN/UNSENT_TERMINAL、Attempt/Delivery/Outbox 与 provider ID；`legacyExternalWithoutUnsentProvenanceWaitsForReconciliationInsteadOfResend` 不把未知历史当未发；`oldLeaseOwnerCannotSettle...` 防旧 owner。R2 `browser-failed/result.json` 在 seed 前已记真 HTTP 发布、实际 Worker 给 A 的 IN_APP V1、随后撤回，且本人/非本人关系计数与路径事实通过。 | 类型化供应商结果来自 fixture，不宣称真实 SMTP/SMS 已受理或真实供应商回执；R2 浏览器未读取撤回后的本人详情。R3 Chrome 第一例必须对真实 V1（非合成 filler）从离页 deep link 取得保存时正文，并在撤回后仍可见。 |
| 普通本人可读快照、非本人/不存在一致拒绝；不扩大管理权限 | 固定后端 `NotifyInboxController` 以会话取本人，DAO detail 经本人 recipient JOIN；Snapshot/Intent/template path 使用实际 IN_APP receipt ID，历史管理 link 仅在已有本人 messageId 上转换。R2 前置真实 HTTP 对 A/B 管理 Notice GET 均 R403；R3 E2E 第二例预设 B 本人正控、外人 V1 与不存在 ID 响应三元组相等，非法 ID 零详情请求，且两例均跟踪零管理 GET。第一例还预设真实后端详情字节迟到、同页 query 与 A→B session 切换。 | 这些 Playwright 断言目前只是源代码及离线选集，**未取得 R3 成功执行证据**。R2 在 seed 前失败，Chrome 0 项；不能把 SFC mock/host tests 说成浏览器权限与会话 E2E。 |

共享 `complete-candidate-r2/shared135/result.json` 为八类合计 135/0/0/0、Maven0、固定 clean R2、cleanup[]，证明通知 deadline/原子结果/手工 retry/SMS/Redis 幂等等回归；它不单独验收 T40 浏览器。真实 Notice18 用真实 MySQL/Redis 和 SQL 事务/Worker，但在受控 Spring/test 容器中手工组装部分组件、外部供应商与部分用户服务替身；其正常/故障/并发矩阵可信，**不能替代完整 Spring Boot 的 JSON 配置与实际登录/路由**。R1 曾因此出现完整 HTTP 发布 R500，而 R2 实际 HTTP 已穿过大 ID JSON Fence 并发布成功；这正是要求 R3 继续跑完整服务的原因。

## R3 runner 增量

R3 `seed_plan` 用真实 V1/A `notify_message_recipient.create_time + 1s` 放置 22 条**明确合成**消息，`verify_seed_anchor` 先要求唯一且时间非空，随后 `verify_seed` 继续严格核 A22/B1、22合成/1真实/0不存在、20 首屏、真实 V1 离页及 legacy 前十。没有改真 V1、Notice、关系或应用时区；缺/歧义/空 anchor 和不符计数均失败。新 `OwnedSeedFailure` 只出 0..1000 有界计数、首屏 0..20 与布尔位置，不回显 SQL/正文/凭据。离线测试新增时钟错位、anchor/排序/数量/脱敏负例。这能修正 R2 的合成排序夹具问题，但离线25通过不能证明真实库时间和浏览器选择器已通过。

## 当前决定

无需再扩大生产代码或用供应商替身冒称远程调用。**R3 当前只能给静态产品 PASS、整票 acceptance pending。** Lead 须依据其独占真实 runner 核对：固定 SHA/tree 前后 clean、完整 Admin JAR 精确证明、真实发布/Worker/撤回与本人/外人 HTTP 事实、两条 Chromium case 各仅一次且零 skip、前后 DB 计数/快照与 owned PGID/容器/卷/端口清理。任一失败应保留原始尝试，不从先前 R2 绿灯追认 R3 浏览器验收。

## 追加：R3 浏览器实际结果（原静态检查点保留）

Lead 返回 `/tmp/wta-t40/browser-runs/f3ff781212c46752/result.json` 后，我只读核对其安全摘要：固定 `378f8481`/tree `37a8e633` 的 source before/after 均 clean，完整 JAR 前后哈希记录存在；真实控制发布→Worker 给 A 的 V1 送达→撤回已通过，seed 前置也达到 A22/B1、22 合成/1 真实、真实 V1 离页及 legacy 前十。两条 Chromium case 各尝试一次，**1 passed、1 timedOut、0 skipped、0 flaky，Playwright exit 1、整次 acceptance=false/exit 1**。通过的是 B 本人正控、foreign/absent 同形拒绝那例；A 的离页真实 V1/同页迟到/旧管理链接/会话切换综合例未完成，安全诊断位置为 `notice-retraction-real.e2e.ts:175:18` 的 `finally` 中 `page.unroute`，仅凭该位置不能断定最初等待点或产品缺陷。owned 容器、匿名卷、五个 loopback 端口和进程清理记录为 `errors=[]`、无残留。

因此上述“待浏览器”现已转为**浏览器实际失败**：AC-040 的 A 本人完整 Chrome 旅程及整票 required E2E 未通过，不能以 B 通过、真实前置通过或先前单位/SFC/数据库绿灯追认整票完成。原静态产品结论仅说明固定源码未见独立阻断，不是 R3 验收 PASS；本报告不改写先前检查时点。三次后复盘和新候选判定由 Lead 执行，本审查没有改仓库或运行服务。


## 追加：U1 最终当前候选验收（保留 R3 历史失败）

最终当前候选为 `26f04b94db68701ade80038a763eca0ffde83918`，tree `ad6f9ed73ff4c437741372149670d824327ec98a`。本段只读核对固定 `git diff 378f8481..26f04b94`、Ticket AC-040、`/tmp/wta-t40/u1-source-equivalence.json`、`u1-write-scope-and-ancestry.json`、U1 gate 摘要及实际 `/tmp/wta-t40/browser-runs/dd86442542876baf/result.json`。前文 R3 的 acceptance=false、A timedOut/B passed 是当时真实失败，**不追认为通过**；U1 是后续独立 clean 候选与独立真实运行。

**当前候选安全/实施合同结论：PASS，AC-040 三项均有与其风险相称的当前候选证据；未见剩余阻断。** R3→U1 只改 E2E/runner/离线测试与说明，后端、Admin 产品 TS/Vue、六 SQL、依赖和 Skill 输入按 `u1-source-equivalence.json` 与实际 diff 等价。测试增量为有界动作/清理、精确 Request 身份匹配、固定阶段诊断和可验证的旧 A 请求取消；没有修改产品撤回/授权或删掉旧正负断言。U1 离线 Python 26 项 exit0、E2E TypeScript exit0；完整前端 652 Vitest +108 Node=760 项、architecture/lint/typecheck/OpenAPI check exit0，三 App 生产构建 exit0，产物清单 Admin311/Home14/SSO4 共329文件。U1 自身 full package/bundle exit0；full JAR SHA-256 为 `efafa17218f8b4ccdf964a21751179cbde0505e2162d1d52b0e6080cc58087e1`，构建时 clean U1。R2 后端 default 889 执行通过/216 环境 skip、真实 Notice18、共享通知135、core/static 是**同字节产品输入复用**，不冒称在 U1 SHA 重跑。

AC-040 对照：

- **先撤回、版本隔离、快照不变**：先前固定 R2 真实 MySQL/Redis 18 项覆盖撤回前栅栏、双 JDBC 行锁竞争、旧 owner、重复撤回、V1/V2 重发布和 SQL 回滚；U1 后端产品输入未变。U1 完整应用在隔离六 SQL/真实 Worker 下先发布 V1、实际送达 A 后撤回，`real_publication.worker_delivered_before_retract=true` 且 `retracted_after_delivery=true`；A/B seed 前后计数 22/1 相同、真实 V1 仍离首页，Chrome A 通过原正文详情和迟到请求隔离断言。未发外部任务“零 provider 调用”的证明来自受控真实 DB 测试的 adapter 调用计数，而非浏览器中未配置的外部账号。
- **外部执行权/已受理事实保留、已送达可读**：R2 真实 DB 矩阵以类型化 provider 夹具分别保存 ACCEPTED/UNKNOWN/UNSENT_TERMINAL、Attempt/Delivery/Outbox 与关联 ID；不会把已进入 I/O 的结果伪写成未发。U1 真实控制发布、Worker 实送 IN_APP、撤回后 A 普通用户由离页深链读到原 V1 正文，且没有管理 GET。外部供应商为夹具，故本证据不证明真实 SMS/MAIL 供应商受理或撤回远端请求；AC 所要求的应用本地保守结算边界已有真实 DB 测试。
- **本人读与越权拒绝**：U1 A、B 两名收件人及控制账号真实登录；A 本人撤回后的 V1 原内容可见，同页晚到详情不覆盖当前详情，跨 A→B 会话旧请求被取消、B 正控详情保持，`last_started_phase=verify_old_cancel` 且整例 passed。B 对 A 的 V1 与不存在 ID 的详情响应同形拒绝、非法 ID 不发详情请求；普通收件人两次管理公告 GET 被拒绝，未扩管理权限。A/B 两条 Chromium 各 `attempts=1`，`expected=2, unexpected=0, skipped=0, flaky=0`，Playwright exit0、总 `acceptance=true/exit_code=0`。合成 filler 仅用于本人分页定位；真实 V1 来自 HTTP 发布→Worker，不由 SQL 合成。

来源/清理：U1 浏览器 run `dd86442542876baf` 的 source before/after 均 clean、HEAD/tree 相同，JAR 前后哈希一致；3 容器、2 匿名卷、3 进程组和 5 个 loopback 端口均报告已回收、`cleanup.errors=[]`。Ticket base `601b927313e0dfae16e75a848a97d9f06afb69bb` 是 U1 祖先；`u1-write-scope-and-ancestry.json` 列 38 条产品路径，`outside_declared=[]`。之前 C1/C2/C3/R1/R2/R3 与后续恢复失败记录仍各自有效，不能拿最终绿灯覆盖或改写其当时判断。

残余边界：默认 reactor 的 216 项是明确环境 skip，故相关真实服务合同以单独 Notice18/共享135 与 U1 完整 Chrome 两例承接；此审查没有操作生产数据、真实外部 SMS/MAIL 供应商或部署。报告支持 Lead 按既定 Goal/SpecDev 流程把 **T40 当前候选**置 done；不自行改 Ticket 状态、归档整个 change 或声称生产已发布。此段未重跑任何测试/服务，也未修改仓库。
