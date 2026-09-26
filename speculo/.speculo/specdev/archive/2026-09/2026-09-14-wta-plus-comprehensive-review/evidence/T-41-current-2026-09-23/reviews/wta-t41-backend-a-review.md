# T-41 后端 Source A 固定审查（只读）

固定 source `b08105fb00ce2006bfd7a4083758033035d632b1`，相对行为红灯 `8a90e404fcd899aa68482f3f52bb902521dc6aa0` 的 backend 精确 10 路径；本报告只以 `git show`/三点差异与只读日志审查，不混入之后的工作树。前端/生成物未实施，此处绝不是完整 T-41 候选。没有修改仓库、target 或运行构建/服务。

## 总判定

**Source A 验收拒绝，主因隔离测试夹具缺 MPJ 查询拦截器；生产路径静态结构基本符合本人分页合同，仍须新固定候选实测。** Lead 已结束的一类 owned run `/tmp/wta-t41/runs/3149ce13e6dfd617/result.json`：Maven exit 1、3 tests/3 failures/0 errors/0 skips，`acceptance=false`；fresh XML 三例首个新列表/详情请求预期业务 200 实为 500，安全日志只记录 `java.lang.ClassCastException`。source before/after 均 clean b08105f/tree `277f52485132756bf4f82c0ab8e0ef296f90bded`，owned PGID/容器/匿名卷/32846、32847 端口全清，`cleanup.errors=[]`。该记录不是源 A 功能通过，不能用红灯的旧 8a 结果替代。

## 已实证失败：MPJ fixture 与生产装配不等价

`NotifyAtomicResultIntegrationTest.java:126–139` 在手建 MyBatis factory 加了 `MPJSqlInjector` 和 `PaginationInnerInterceptor(overflow=true)`，却没有 `MPJInterceptor`。本地 MPJ 1.5.9 源码：`MPJInterceptor` 在 `Executor.query` 为 `selectJoinPage/One` 切换动态 resultType；只装 injector 时投影仍可按主实体 `NotifyMessageRecipient` 映射，`NotifyInboxService.toVo(NotifyInboxRow)` 遂强转失败。主例 HTTP500 位于新测试 `:118`，详情也相同。生产依赖 `mybatis-plus-join-boot-starter` 的 `MybatisPlusJoinAutoConfiguration` 注册 `MPJInterceptor` 和 `MPJInterceptorConfig`，后者把拦截器加入 SqlSessionFactory；`MybatisPlusConfig` 本身确有分页 `overflow=true` 和乐观锁链。**最小修正是隔离夹具按实际生产 MPJ interceptor 链装配并保留生产 DAO/Service；不得为使测试通过弱化本人 JOIN、改成 list 后 Java 过滤或移除 PageQuery。** Boot 自动配置在最终 full-JAR 的真实激活仍需直接集成证明，静态 starter 源码不是 live 装配证据。

## 生产合同静态核查

- **本人集合/计数：** `NotifyNotificationDao.java:207–253` 抽出同一个 `inboxJoin(userId)`：收件关系 `r.user_id=userId` LEFT JOIN 消息主键后以 `m.message_id IS NOT NULL` 排除孤儿。`inboxTotal`、附加 `r.read_time IS NULL` 的 `inboxUnreadTotal`、列表及详情复用该谓词；消息 PK 与关系 `(message_id,user_id)` 唯一约束保证每个本人关系不会因 join 倍增。三次 SELECT 是最终一致，不保证同时刻快照，符合当前 T-41 revision173。第三测试有孤儿关系/极大页码负例；当前 run 未到业务断言。
- **分页、排序、投影：** `NotifyInboxService.java:25–35` 明确默认 1/20，拒绝 pageNum<1、pageSize<1或>100及空 userId；`NotifyNotificationDao.java:220–227` 先以 `long` 算偏移，超过 `total` 则不发页 SQL，`PageQuery(pageSize,pageNum)` 为本地新对象，无用户 `orderByColumn/isAsc`，固定 `r.create_time DESC,r.message_id DESC`。`PageQuery.build()` 的 `Page` 页码/大小为 long，且未调用其会 `int` 溢出的 `getFirstNum()`。`inboxProjection(false)` 不选 `m.content`，VO `@JsonInclude(NON_NULL)` 避免向列表输出空字段；`selectAs(r.createTime,NotifyInboxRow.createTime)` 令公开 createTime 与排序均为收件时间。详情才选择正文。需在修夹具后用 HTTP payload 和 SQL/Mapper 投影验证，静态不冒称真实 501、COUNT 或 content omission 已过。
- **详情与权限：** `NotifyInboxController.java:20–35` 保留类级 `@SaCheckLogin`，list/detail 只传 `LoginHelper.getUserId()`；`NotifyInboxService.java:38–42` 对空/非法 ID 和本人 JOIN 不命中均抛同一 `ServiceException("消息不存在")`，没有先按 messageId 取他人消息的旁路。三个 POST 保留 `notify:inbox:seen/read` 权限并新增禁请求/响应正文的 `@Log`。新真实 HTTP 测试包含未登录、A 请求 B-only 与不存在同形、无读权限拒绝、A 的 B-only read 不改变 B；但全部因夹具 500 尚未运行到对应断言。测试使用默认 SaInterceptor 而非生产 `SecurityConfig` Client header/extra 检查，完整 Client 负向矩阵仍需要独立真实应用/浏览器验证。
- **全量已读：** `NotifyInboxService.java:64–75` 和 `NotifyNotificationDao.java:270–279` 没有 page 条件，按服务端 userId 更新其全部关系，A/B 隔离；同请求重复执行在 seen/read 均非空后零更新，预期幂等。**遗留准确性缺口（建议本票修并加断言）：** `markAllMessages` 选择 seen 或 read 任一为空的行，却无条件把两个时间都改成 now，已见但未读的 first-seen 时间会被覆盖。此问题来自旧实现，本次保留；若 AC-041 的“准确读取状态”包含原 seenTime 历史，不能只断言 unreadTotal=0，应保存已有 seenTime 并只补缺失时间。单条 `mark()` 已按字段空值分别赋时，可作为语义基线。孤儿关系虽不进入 page/unreadTotal，但同 user 的 read-all 仍会标记它；不会越权，若产品要求严格只标“可见集合”需另定语义。

## 后续必要验证与边界

新固定修订先修 MPJ fixture，重复**同一** owned one-class，fresh XML 3 项正数/0 fail/error/skip、HTTP200 的 rows/total/unreadTotal、A501/B2/孤儿、详情同形、极大页码、权限和 DB read-all 才能成立。还应补 `seen_time` 非空但 `read_time` 为空时 read-all 保留 first-seen 的回归，若决定按上述准确语义修复。Production `MPJInterceptorConfig`/数据权限链应在 full-JAR 当前候选验证；默认单元、Java public API/OpenAPI、全部前端消费者及真实 Admin 浏览器须待完整 T-41 后续阶段，均未由 Source A 覆盖。

## 后续独立修正进展（不追认 A 通过）

Lead 后续固定 A2 `662b351fca76a9e50ae8fba699e9e26aa65225f8`：`b08105f..662b351` 仅两测试文件，Atomic 手建 factory 后加 `MPJInterceptorConfig(List.of(sessionFactory),new MPJInterceptor(),false)`，新类在 HTTP 前增加 DAO 本人 501/500/page26/detail 正控制，没有删旧断言。只读核 `/tmp/wta-t41/runs/ff7664968f5f9976/result.json`：同一 owned one-class 3/0fail/0error/0skip，Maven/driver exit0，source 前后同 clean 662b351/tree `d8e35fd68052d2c5f8211ccc13de5309e4a320f4`，PGID/容器/匿名卷/32848、32849 端口全清、cleanup.errors=[]。这关闭了 **A 的夹具 ClassCast 根因**，不是 A 原运行变绿，也不是完整 T-41 验收。Lead 已决定在后续产品修正 `markAllMessages` 保留已有 seen/read 时间并加真实 HTTP 负向；该修正尚不在 A2，不能把本报告的遗留准确性 finding 标已关闭。
