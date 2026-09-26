# T42 02B 独立只读审查：唯一键竞态与提交者身份

固定输入：`6f5ba38ba7f562adebaad8191dd2288b493baac1`，相对 `764820dd`。仅以 `git show`/固定 diff 审源码；没有读取后续工作树作为本候选、运行 Maven/服务或修改仓库。Mail 27 真实运行在审查时尚未返回，因此本结论是**源码可验、运行待定**。

固定文件 SHA256：`NotifyMailAttachmentIntegrationTest.java` `c3343ecb136ec57c416f086847ba76025cce67bf5c7e056258ea35cec4c8fb02`；`OwnedAttachmentJdbcFaults.java` `869e156255551d2f2134ff60b590fcfe7d0529a66df4ef7f7ffe8ad2967725cf`；`OwnedAttachmentSendReservationSqlProbe.java` `bb2647ee7d1d89ce67c971da298f0406243c4aaf99fa5e0d2737a2cf1d930b5a`。

## 判定

未见新增 25–27 用例的源码阻断。`#25` 经生产 `notifications.submit` 和真实 owned MySQL：A 执行 `notify_intent` 插入后卡住物理连接 `commit`，B 通过前置普通查询后到达真实 `INSERT` 才放行 A；两结果同一 intent ID，拦截器只在 B 的 `FOR UPDATE` 当前读完成时计数，随后检验 intent/附件关系/outbox/源引用各一条及原 source ID。两个 Future 有 20 秒界限，最终强制释放闸门、停线程池并等待，线程未停止即毒化 fixture，避免继续清理活事务。这个测试证明本夹具下的唯一键冲突恢复；不将它泛化为全部并发调度顺序的证明。

`#26/#27` 复用**同 key、同原附件列表和正文**，分别改变已认证用户、已认证 Client。`loginAs` 用生产 `LoginHelper` 建立 Sa-Token 请求上下文，并核 `LoginHelper.getLoginUser` 与生产 `NotifyContextResolver` 的 userId/clientPk；各自先以另一主体自有源文件和新 key 成功提交，证明另一身份可用，再对原 key 发起冲突请求。断言精确 `ServiceException` 文案“同一幂等键的附件或提交者不一致”、原 intent 保存原 actor、原关系指向原 source、四类 owned DB 事实仍各一条。生产 `NotificationApplicationRuntimeService.requireSameAttachments` 对用户、Client、附件顺序同时核验，和用例路径吻合。此用例是完整应用 Bean 与真实会话上下文验证；它不宣称通过浏览器或 HTTP 权限层重新登录。

原 20 个 `@Test` 保留既有顺序与断言；此次将原 `submit` 构造命令抽成同字段的 `mailCommand`，原默认 `login()` 委派同值 `loginAs`。新增 `#21–24` 与 `#25–27` 共 27 个方法。邮件真实发送器仍仅为最终 SMTP 捕获替身；owned OSS/MySQL/Redis、生产 submit/DAO/dispatcher 保留。

三处 103→104 均是单个数字最小修改：`.agents/skills/engineering-standards/references/project/00-project-profile.md:85`、`.agents/skills/namewta-fullstack-development/references/backend/mapper-and-sql.md:141`、`release-artifacts/README.md:226`。原 AI 退役、旧 AI 数据保留、已有库不得重放等硬边界均原样保留。固定源码 `EXPECTED_TABLES=104` 与新增 `notify_intent_attachment` 表一致。

## 运行证据界限

待 Lead 的固定 `6f5ba38` Mail 27 结果证明 27/0/0/0、新鲜 XML、源码前后 clean 与 owned 清理；本静态审查不代称运行通过。测试直接合成第二身份所需的源文件元数据与生产 Sa-Token 登录，不覆盖 HTTP 入口的具体授权规则；该范围与本次竞态/幂等身份用例目的相符。
