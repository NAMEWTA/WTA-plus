# T-41 本人分页收件箱安全设计复核（只读）

固定审读点：当前 HEAD `d7d534cb03aa000d603a53c092c4bf1d48bb5cc3`，Ticket `41-paged-personal-inbox.md`/AC-041，`/tmp/wta-t41-implementation-design.md` 与 `/tmp/wta-t41-real-environment-outline.md`。本稿审设计及当前源，不是候选代码审查或验收；没有仓库写入、Maven、服务、浏览器或真实凭据操作。

## 结论

**主体方向可实施，编码前必须冻结三个缺口：会话 owner 的前端可观测来源、收件时间排序/显示的一致定义，以及固定排序和极大页码的实际 PageQuery 使用。** 其余已拟定的本人 JOIN、全局 unreadTotal、全量 read-all、10 条顶部摘要和详情独立 GET 能满足本票安全边界。不要用页内未读数、公告管理权限或单纯 messageId 来代替本人关系。

## 生产不变量与精确反例

1. **同一可见集合。** `rows`、`total`、`unreadTotal` 必须对 `notify_message_recipient r JOIN notify_message m ON m.message_id=r.message_id` 施加相同 `r.user_id=LoginHelper.getUserId()`，唯一 `(message_id,user_id)` 索引已在六 SQL。`unreadTotal` 再加 `r.read_time IS NULL`；`seen_time` 不使它减少。列表/总数不能先对 relation 分页再过滤不存在的 message，否则孤儿关系会造成短页和错误 total。详情必须以 **单次本人 JOIN** 按 `messageId` 取正文；不存在与他人 ID 统一无可探测的失败形状，不能先 `message(id)` 再靠前端隐藏。列表只投影摘要、状态/时间/必要跳转，不投影 `content`/扩展全文；完整正文只在本人详情返回。三次独立 SELECT 的谓词同义是最低要求；若产品要声称三值在并发写入时属于同一时刻，则需短只读一致性快照，否则明确为最终一致并在读/推送后刷新，不能把不同时间点差值误当越权或最终统计。
2. **权限和 Client。** 当前 `NotifyInboxController` 类级 `@SaCheckLogin` 给本人读；`seen`、`read`、`read-all` 分别有现成 `notify:inbox:seen/read`。保持 list/detail 的本人关系授权与现有登录 Client 策略，不擅自加公告管理 permission，也不引入未声明的 per-app owner。写操作继续从服务端 token 身份取 userId，`read-all` DAO 保持 `WHERE user_id=?` 且无页参数；第 501 条与 B 用户关系不因 A 的操作变化。`@Log` 只记录安全元数据，禁存消息正文/完整响应。当前单条不存在关系为空操作成功，若改为统一拒绝应同时更新现有 HTTP 合同和测试；详情的不存在/他人同形是独立要求。对于跨 Client token，只测所选登录/Client策略及 userId 归属，不把“非 Admin Client 必须拒绝”凭空升格为 AC。
3. **有界分页与固定排序。** 当前 `PageQuery.DEFAULT_PAGE_SIZE=Integer.MAX_VALUE`，`getFirstNum()` 用 `int` 乘法并可溢出；不能直接绑定原始 `PageQuery` 后调用它的默认/偏移。入口明确默认 `pageNum=1,pageSize=20`、`1<=pageSize<=100`、`pageNum>=1`，非法值拒绝。用 `long` 计算 `(pageNum-1)*pageSize`；`pageNum=Integer.MAX_VALUE,pageSize=100` 必须安全返回空 rows 与真实 total/unreadTotal（或提前明确拒绝），绝不能回卷第一页、产生负 offset 或发无界查询。计数后若 offset>=total 可短路列表 SQL。通用 `PageQuery` 还暴露 `orderByColumn/isAsc`；本入口应只从已验证数字构造一个无用户排序项的 PageQuery，SQL 固定 `r.create_time DESC,r.message_id DESC`，传入 `orderByColumn` 不得改变顺序。`pageSize` 上限限制返回量，不自动限制恶意巨大 offset 的数据库扫描。
4. **排序时间的表意必须统一。** 当前 DAO 按收件关系 `r.create_time/message_id` 排序，`NotifyInboxMessageVo.createTime` 却取 `m.create_time`，顶部 `noticeStore.sortNotices()`又按该 VO 时间重排。若保留建议中的 relation 排序，应投影/展示 relation 时间，或者至少不在客户端按不同消息时间重排顶部 10 条；否则顶部显示顺序、完整页和时间标签彼此矛盾。同时间、不同消息/收件时间、逆序插入 ID 的夹具要固定这个合同。若产品选择消息时间排序，须显式修订 Ticket/前后端并用 `m.create_time,m.message_id` 一致实现，不能静默混用。
5. **列表与详情、会话 owner 分开。** 完整页自己持有页码/总数/全局未读，顶部只拉第一页 10 条摘要；顶部 badge 从服务端 `unreadTotal`，`read-all` 按该总数可用，即使顶部 10 条全已读；tab 数字若仍来自 10 条只能标“最近显示”。完整页和顶部点击详情均先清旧详情、单独 GET 本人正文，并有 loading/error；旧请求晚到不能把 A 正文写进 B 会话。当前 `InboxPage.vue` 只有列表 generation+unmount guard，`NotifyWebRuntime` 没有身份代次/owner port。若页面可在 token 更换期间保持 mounted，单纯列表/详情局部 generation 不足以识别旧身份；最小方案是宿主提供不含 secret 的 session epoch/identity key，所有 list/detail/mark 完成前核对，切换时立即清 rows/detail/unread 和增长 generation。若实现方证明每次身份切换在任何响应回填前必定 unmount，也应把该断言纳入测试，不能只假设。为宿主 port 需先登记 `frontend/packages/web-domains/notify/src/runtime.ts` 和 `frontend/apps/admin-web/src/router/adminManifestRegistry.ts`（或选择已预登记范围内等价入口）。顶部已有 token/inboxRequest/dirty/identityVersion 保护；把 `unreadTotal` 随 beginLoad 新 token 和 logout 一同清零，不让旧请求成功/失败回写新 owner。
6. **合同/回归边界。** Domain `inbox.list({pageNum,pageSize})` 用 GET query、`inbox.get(positiveMessageId)` 用 GET；三写动作保留 POST。平坦 `R<{rows,total,unreadTotal}>` 应在当前 full-JAR live OpenAPI 生成后一次更新全部消费者。现有 T-34 真浏览器 `inbox-real.e2e.ts` 期待 `data` 数组和列表中完整 body；迁移时改为页 `data.rows` 摘要、正文只在详情 GET 检查，保留真实登录、零 push ticket/stream 和单份正文断言。各默认 mock E2E 中 `/notify/inbox` 数组 fixture 也需枚举迁移；不能仅让新 T-41 专用例通过。新真实 501 场景仍用 owned 六 SQL、MySQL/Redis/JAR/Admin/Chrome，不触生产库或外部供应商；T-34 旧脚本的 secret argv/匿名卷清理局限不应复制进新 runner。

## 必要负向测试（实现前固定红灯，候选后实测零 skip）

- 同一时间的 A 501 条本人关系，B-only 消息 ID 位于 A 分页边界附近，A/B 共享同一 message。A 页 1..26 拼接恰 501 个唯一 ID，页 26 恰 1 条，A 的 `total=501`、独立 `unreadTotal` 正确，B-only 不会先占分页名额；插入一条孤儿 relation 后，rows/total/unread 使用同一 JOIN 集合。让前 10 条都已读、页外仍未读，顶部 badge 仍显示全局数，`read-all` 按钮仍可用。
- 未登录 list/detail 被拒；A 请求 B-only detail 与不存在 ID 返回同一形状、无 B 正文，A POST read/seen B-only ID 后 B 的 read/seen 不变；没有写权限的登录用户 POST 403/零修改，但可按现有合同 GET 本人详情。A 第501条正文只有详情 GET可见；列表响应和顶部摘要不含它。重复 read/read-all 幂等，A read-all 后全 501 条 `read_time` 非空、`unreadTotal=0`，B 两条关系未变。页外 read 后当前页码不跳回 1，顶部与页的未读数最终相同。
- 缺省 page 参数得到1/20；`pageSize=0,101,-1`、`pageNum=0,-1`、非数字或超过整数绑定范围拒绝；`pageNum=2147483647,pageSize=100` 不溢出、不返回第一页、无无界结果；`orderByColumn/isAsc` 不改变固定序；同时间 `message_id DESC` 稳定。
- 保持同一个 InboxPage 实例，先发 A 的列表/详情/mark 请求，再切 B 身份并让旧响应成功、失败、finally 依次晚到：B 页/详情/badge/错误提示不泄 A 数据、不被旧 finally 关闭 loading；切换后新请求可恢复。顶部 T-34 dirty refresh、旧身份失败和 push-disabled 用例同样保留。详情打开后切页/再次点行、关闭/卸载时旧详情不得覆盖当前内容；read 失败不得虚标已读。

上述是设计约束与应写测试，不是当前行为已实现或任何验证通过记录。
