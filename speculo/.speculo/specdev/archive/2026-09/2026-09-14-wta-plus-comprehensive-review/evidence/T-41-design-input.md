# T-41 实施设计（只读，未激活）

基线：`/srv/WTA-plus` HEAD `d7d534cb03aa000d603a53c092c4bf1d48bb5cc3`，2026-09-23。输入为 Ticket41、`/tmp/wta-t41-current-audit.md`、`/tmp/wta-t41-next-boundaries.md` 和当前源码；T50 已关闭。本稿没有改仓库、运行构建或服务。适用入口：engineering-standards、namewta-fullstack-development、wta-module-guide/notify、java-api-compatibility；当前 `wta-notify` 是 layered，前端为 App→web-domain→domain→contracts。

## 冻结的 HTTP/权限合同

- `GET /notify/inbox?pageNum=&pageSize=`：继续由类级 `@SaCheckLogin` 与 `LoginHelper.getUserId()` 定位本人，默认 `1/20`、页大小 `1..100`；无效页码或页大小明确拒绝。不能把 `PageQuery.DEFAULT_PAGE_SIZE=Integer.MAX_VALUE` 直接放行。返回 `R<{rows,total,unreadTotal}>`，三个字段为同一用户、同一“有实际消息的收件关系”集合；`unreadTotal` 由全量 `read_time IS NULL` 的独立查询取得，不从当前页推算。列表只传摘要字段（`message`、标题、分类、时间、已见/已读），完整 `content` 只在详情返回。现有关系顺序为**收件关系** `create_time DESC,message_id DESC`；建议保留这个可验证的既有顺序，并在用例中同时间打乱插入顺序。当前 VO 的 `createTime` 来自消息表，若产品意图改为消息创建时间排序，须在编码前明确变更排序合同，不能静默切换。
- `GET /notify/inbox/{messageId}`：同样只需登录，不借用公告管理权限。单条 JOIN 同时要求 `recipient.user_id=currentUser`、`recipient.message_id=messageId`，才返回含完整正文的 `NotifyInboxMessageVo`；不存在和他人消息使用同一不可探测失败形状。`GET /notify/inbox/read-all` 不存在，仍用原 POST。
- `POST /{messageId}/seen`、`/{messageId}/read`、`/read-all` 保留当前权限 `notify:inbox:seen/read`、幂等和“他人 ID 无修改”语义。补安全 `@Log(title=通知收件箱, businessType=UPDATE, isSaveRequestData=false, isSaveResponseData=false)`；日志仅操作元数据，不落消息正文。`read-all` 不带 page 参数，DAO 保留 `WHERE user_id=?` 全量更新，必须覆盖第 501 条。当前单条 mark 对不存在关系返回成功空操作；若要改成拒绝，需同步 HTTP/前端合同，AC 仅要求不可改他人。
- `PageQuery` 用于本入口分页计算/绑定，`PageResult` 承载 `rows/total`；可在 `domain/vo` 新增 `NotifyInboxPageVo extends PageResult<NotifyInboxMessageVo>` 只添 `unreadTotal`，形成平坦 JSON。旧 `/notify/inbox` 的数组合同与所有消费者同一候选切换，不手造兼容桥；该 HTTP/生成类型变化须按 Java API Skill 核调用方和序列化，虽不改 `wta-api`。

## 最小分层施工

1. Controller 只做有界参数验证、当前身份和 HTTP 包装；UseCase 委托 Service；Service 负责摘要/详情映射及 read 状态；DAO 唯一使用 Mapper/PageQuery。依 BE-CRUD-003 查询阶梯先用**类型化 MPJ**，不预设 XML：`NotifyMessageRecipientMapper` 沿用 `BaseMapperPlus` 并加 `MPJBaseMapper`；每次构造新 `QueryBuilder.lambdaJoin("r", NotifyMessageRecipient.class)`，用 `leftJoin(NotifyMessage.class,"m",...)` 与 `isNotNull("m",NotifyMessage::getMessageId)` 得到内连接的有效收件集合，再加 `r.user_id=?`。本地 MPJ 1.5.9 的 `JoinMapper` 确有 `selectJoinPage`、`selectJoinCount`、`selectJoinOne`，现有 QueryBuilder 确有别名 `select/selectAs/eq/isNull/isNotNull/orderByDesc`；简单 1:1 所有权联表无需 XML。`PageQuery(pageSize,pageNum).build()` 的 Page 关闭自动 count，另用同样联表条件 fresh wrapper 调两次 `selectJoinCount` 分别取 total 与 `read_time IS NULL` 的 unreadTotal；列表只投影摘要，详情单条 JOIN 投影完整 content。`uk_notify_message_recipient(message_id,user_id)` 和消息主键保证每个收件关系最多一条连接行；orphan 不会被计入页或计数。若真实集成证明 MPJ 的别名/count/分页 SQL 无法清晰安全表达，再记录具体 SQL/失败证据后考虑已登记 XML 路径。`pageNum` 的偏移计算必须用 `long`，不能经 `PageQuery.getFirstNum()` 的 int 乘法溢出。每页最多100条；不存在页返回空 rows、真实 total/unreadTotal。
   内部查询投影需单独的 `domain/dto/NotifyInboxRow.java`：显式容纳数据库 `channelsJson` 字符串、收件关系和消息各自的 createTime、正文等字段；Mapper 投影 DTO，Service 把 JSON 字符串转换为公开 VO 的 `channels` 列表。列表不选 `content`，详情才选。不要把原始列塞公开 VO 的隐藏字段、也不要让 Service 直接持有联表/Mapper 形状。此新路径尚未在 Ticket 写集内，须先登记。
2. Domain `NotifyInboxPage={rows,total,unreadTotal}`，`inbox.list({pageNum,pageSize})` 明确 GET query；`inbox.get(messageId)` 明确 GET + 正整数 ID 校验。`seen/read/readAll` 继续 POST。Generated OpenAPI 先从固定实现 commit 的真实 full JAR `/v3/api-docs` 原字节抓取，再 fetch→generate→check，绝不手写 snapshot/types。
3. `InboxPage.vue` 自有 `pageNum=1,pageSize=20,rows,total,unreadTotal,loading,error`；Element Plus 分页翻到26取第501条，不复用顶部行缓存。列表行只显示摘要；打开详情单独 GET，并为详情/列表/mark-read 分别做 generation 与卸载保护。读成功重新载当前页并触发宿主 `inboxChanged`；失败保留错误，不把旧会话读失败/详情回填到新会话。订阅实时刷新仍只是触发 REST 查询。页面可显示本人全局未读数，但分页 total 是历史总数。
4. 顶部 `push.ts` 固定请求第1页的小页（建议10条），`notice.ts` 保存服务端 `unreadTotal`，仍用原 `unreadCount` getter 供 Navbar，切 token 时立即清零；保持 T34 pending/dirty/token/generation 保护。顶部 tabs 的数字明确标为“最近显示的摘要数”，不能冒充历史总数；“全部已读（含历史消息）”按钮按 `unreadTotal>0` 可用、调用现有全量 POST，并刷新顶部与完整页。顶部详情也必须 GET 本人正文，不能假定摘要页包含 `content`；旧详情在身份切换时关闭。`Navbar.vue` 若沿用 getter 无需修改。

## 实施前写集修订

Ticket41 已声明 Controller/UseCase/Service/DAO/VO 目录/RecipientMapper/XML、notify domain 目录、`InboxPage.vue`、`push.ts`、notice component 目录、api-contracts、OpenAPI tooling、admin notify test 根及 `frontend/e2e/`。另需**先登记**：

- `frontend/apps/admin-web/src/store/modules/notice.ts`：全局 unreadTotal 的状态 owner；建议同目录新增 `notice.test.ts` 也登记。
- `frontend/apps/admin-web/src/utils/push.test.ts`：现有 T34 会话/失败/合并查询测试要迁移页响应；`push.ts` 不自动授权其 sibling test。
- `frontend/packages/web-domains/notify/src/InboxPage.test.ts`：新真实 SFC 分页/详情/过期响应测试，因现票仅列 `InboxPage.vue`。
- `backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/domain/dto/NotifyInboxRow.java`：MPJ 内部读投影（当前仅 VO 目录在写集内），必须先登记；不扩为公开 API。若需要 `backend/wta-modules/wta-notify/src/test/java/...` 单测，也先精确加路径；目前可以仅在已声明 admin notify 集成测试根验证 DAO/HTTP。
- 已有 `backend/wta-modules/wta-notify/src/main/resources/mapper/notify/` 写集无需因本票预先修改；只有带证据证明 MPJ 不能满足 BE-CRUD-003 时才转 XML。
- `frontend/apps/admin-web/src/layout/components/Navbar.vue` 只有改 badge 表现才加；只复用 getter 则不改。`frontend/e2e/` 已覆盖各 mock spec 和新的专用 T41 真实 runner/config/test；当前 `inbox-real.e2e.ts`、`inbox-without-realtime.spec.ts` 以及多个其他 `*.spec.ts` 的 `/notify/inbox` fixture 仍回旧数组，必须逐个盘点迁移，不只更新 T34 两文件。`BrowserHttpsTransportIntegrationTest` 仅有 `/notify/inbox` stub，若受影响亦在 admin test 根内。

## 可观察红灯与验收矩阵

| 接缝 | 红灯（旧 API 可编译）／绿灯断言 |
|---|---|
| 真实 MySQL+HTTP | Owned fresh 六 SQL，登录 A/B，给 A 501 对 message+recipient、给 B 至少1条，含相同 `r.create_time` 与逆序 ID。旧 API 上先请求 `GET /notify/inbox?pageNum=26&pageSize=20` 并断言 JSON `data.rows[0]` 为第501条、`total=501`、`unreadTotal` 独立准确；旧实现返回数组且上限500，应是**业务断言**红灯，环境/鉴权失败不算。绿灯拼接各页 ID 无重无漏、每页<=20、B 不出现，非法 pageSize 拒绝。 |
| 权限/详情/已读 | 未登录 list/detail 拒绝；A 的 B-only detail 与 read 不返回 B 正文、不改 B 关系；A 可 GET 本人第501条完整 content。先把第501条标读，页1不变、A unreadTotal 减1；POST read-all 后 A 所有隐藏页 unreadTotal=0，B 状态不变。多次 POST 幂等，权限不足的 read 零写。 |
| 前端单元/SFC | Domain transport 断言 query 和 `{rows,total,unreadTotal}`，详情 path/方法；InboxPage 真 SFC 页1→26、详情、loading/error/empty、旧请求晚到/卸载、mark 后保持页码并双面刷新；store/push 测试第1页10摘要但全局 unreadTotal=例如300，Navbar getter为300；read-all 一次且无 page 参数；T34 push-disabled、旧身份失败、dirty refresh 不倒退。 |
| 真浏览器 | 独立 owned T41 Admin 登录+MySQL/Redis+full/core适用 JAR+Chrome：501条真实数据、页面翻到末页/详情、顶部仅最近10条且 badge 显示全部未读、off-page read 后顶部与页一致、all-read 后 SQL/HTTP 双核 A=0/B未变，零伪造 API。T34 旧单条真实脚本随新响应更新并单独复跑。截图只含合成消息，不含登录凭据。 |

后端定向 `cd backend && ./mvnw -Pdev -pl wta-modules/wta-notify,wta-admin -am -Dtest=<精确类> -Dsurefire.failIfNoSpecifiedTests=false test`，真实外部用例由 Lead 在 owned 资源上启用且 XML 零 skip。前端逐条运行 `pnpm --dir frontend --filter @namewta/domain-notify test`、`pnpm --dir frontend --filter @namewta/domain-notify typecheck`、`pnpm --dir frontend --filter @namewta/web-domain-notify test`、`pnpm --dir frontend --filter @namewta/web-domain-notify typecheck`、`pnpm --dir frontend --filter @namewta/admin-web test`、`pnpm --dir frontend --filter @namewta/admin-web typecheck`，再按范围运行 lint、architecture:check、适用 build 与 mock/真 Playwright。pnpm 必须经仓库固定的 Corepack `pnpm@10.34.5`，不得使用当前 PATH 上的 11.x。正式合同：固定源码的 OpenAPI live fetch/generate/check、notify layered、skill-facts/module-mode、全量适用门禁。所有具体命令/退出码/测试数以实施后的固定候选记录，本稿不把任何检查写作已通过。

## 主要边界

- 当前只按关系表 `read_time` 判本人未读，不能用当前页可见未读数替代；同一个响应的三个计数应使用同一 JOIN 语义。并发插入或 read 与三次查询之间可变化，若产品要求强快照，需显式短只读事务/一致性读；先用真实并发测试确定是否属于本票 AC，不新增计数状态机。
- `PageQuery` 默认无上界；必须在 controller/service 入口拒绝超大 size。`message.content` 为 longtext，列表不批量发送，详情仍经本人关系验证。POST 日志不得收集正文。T40 撤回后的已送达快照应仍可从本人详情读；本票不重写公告管理权限。
- 旧 T34 真实浏览器只有一条公告、当前 mock browser 多处假定数组；它们是回归基座，不是501验收。T41 需要独立真实 501 场景和精确 source/JAR/数据库/进程/容器清理证据。
