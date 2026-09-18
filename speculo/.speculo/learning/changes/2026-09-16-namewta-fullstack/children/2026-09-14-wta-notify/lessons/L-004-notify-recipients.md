---
lesson_id: L-004
objective_ids: [OBJ-04]
estimated_minutes: 35
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: deep-explanation
    minutes: 14
  - segment: visuals-and-worked-examples
    minutes: 8
  - segment: pause
    minutes: 4
  - segment: recap-and-transfer
    minutes: 5
expression_level: eli5
coverage_depth: deep
source_ids: [S-N-04, S-N-09, S-N-10]
---

# Lesson 004：收件人目录端口，不把 system 用户合同挂到 HTTP

## 学完你能做什么

能口述 `GET /notify/recipients/search` 与 `GET /notify/recipients/by-ids`：`NotifyRecipientController` 注入 `NotifyRecipientUseCase`；UseCase 唯一字段是 `NotifyRecipientDirectoryPort directory`。能说明空关键字立刻空页、`pageSize` 被 `Math.clamp` 到 1..50、非法 ID 在 Controller 变成 `R.fail("用户编号格式错误")`。能指出端口的实现类是 `NotifyRecipientDirectoryService`，它再调 `UserService`。

## 先把宏观地图放在桌上

把收件人搜索当成「学校门卫室的访客名单」，不是把整本学生档案室的钥匙交给通知页。通知模块只问两句：按关键字找活跃用户、按 ID 列表回填已选中的人。门卫室对内打电话给 system 的 `UserService`，对外只交出最小视图 `NotifyRecipientUserVo`。

```text
公告编辑页（需要 notify:notice:add 或 edit，OR）
  GET /notify/recipients/search?keyword=&pageSize=20
  GET /notify/recipients/by-ids?userIds=1,2,3
           |
           v
NotifyRecipientController
  注入 NotifyRecipientUseCase recipientUseCase
           |
           v
NotifyRecipientUseCase
  注入 NotifyRecipientDirectoryPort directory
           |
           +-- search: 空白 -> 空 PageResult
           |           否则 clamp(pageSize,1,50) -> directory.search(strip, bounded)
           +-- byIds:  空白 -> List.of()
                       逗号拆分 Long.parse -> directory.byIds
                       NumberFormatException -> IllegalArgumentException
           |
           v
NotifyRecipientDirectoryService  implements NotifyRecipientDirectoryPort
  注入 UserService userService
           |
           +-- searchActiveUsers(keyword, limit) -> NotifyRecipientUserVo.from
           +-- selectNotificationUsers(ids)      -> from
```

**类比失效处：** 门卫比喻不包含数据权限的 SQL 细节。`UserService` 内部如何过滤部门，本课不展开；通知模块保证自己不直接依赖用户 Mapper。

## 核心概念与机制

### 直觉讲解

如果通知 Controller 直接注入 `UserService`，以后 system 用户字段一改，通知 HTTP 就会跟着裂。所以中间放端口：UseCase 只认识 `search(String, int)` 和 `byIds(Collection<Long>)`。

Controller 权限不是 `notify:recipients:*`，而是公告编辑权：`@SaCheckPermission(value = {"notify:notice:add", "notify:notice:edit"}, mode = SaMode.OR)`。没有公告起草权，就不能搜人。

### 精确定义与 English term

| 中文 | English | 精确定义（本课） |
| --- | --- | --- |
| 目录端口 | directory port | `NotifyRecipientDirectoryPort`：隔离通知与 system 用户合同 |
| 最小用户视图 | recipient user view | `NotifyRecipientUserVo`：`userId`、`userName`、`nickName`、`phoneNumber`、`status`。没有 email 字段 |
| 关键字搜索 | search | 空白立即空页；否则 `keyword.strip()` + 上限 50 |
| 按 ID 回填 | by-ids | 逗号分隔字符串 → `List<Long>`；格式错误对 HTTP 是失败而不是空列表 |
| 活跃用户搜索 | searchActiveUsers | 端口实现调用的 system API；通知侧不再自己写用户 Wrapper |

规范：`recipientType=ALL` 时不把全量用户加载到前端。本接口也没有「列出全部用户」的无关键字全表。空白关键字被故意设计成空结果。

### 机制/因果链

1. **search。** 参数 `keyword` 必填（Spring `@RequestParam String keyword`，缺了是 400）。UseCase：`keyword == null || isBlank` → `PageResult.build(List.of(), 0)`。否则 `boundedSize = Math.clamp(pageSize, 1, 50)`（默认 pageSize=20）。`directory.search(keyword.strip(), boundedSize)`。返回的 `PageResult.total` 等于**本页行数**，不是数据库里的匹配总数。
2. **by-ids。** `userIds` 空白 → 空列表。否则按逗号 split、trim、丢掉空段、`Long.valueOf`。任何一段不是数字：`IllegalArgumentException("用户编号格式错误")`。Controller 捕获后 `R.fail`，不再变成 500。
3. **端口实现。** `NotifyRecipientDirectoryService.search` → `userService.searchActiveUsers` → 每条 `NotifyRecipientUserVo.from(UserDTO)`。`byIds` → `userService.selectNotificationUsers`。`from` 拷贝五个字段；email 即使 DTO 有也不进入 Vo。
4. **和公告草稿的关系。** 编辑器搜到的 ID 写入 `NotifyNoticeBo.recipientIds`。保存时 `NotifyNoticeService.toEntity` 会再调一次 `selectNotificationUsers`，不能靠绕过 search 接口把越权 ID 写进草稿（L-001）。

### 图、表或文本图

**图题 / caption：** 通知 HTTP 与 system 用户 API 之间的端口。

```text
[公告页] --HTTP--> NotifyRecipientController
                         |
                         v
                  NotifyRecipientUseCase
                         |  只依赖 Port
                         v
              NotifyRecipientDirectoryPort
                         |
                         |  运行时实现
                         v
         NotifyRecipientDirectoryService
                         |  只依赖 UserService
                         v
                   system.api.UserService
                         |
                         v
                    用户库（通知模块不直接 Mapper）
```

**文字等价物：** 浏览器只打通知模块的两个 GET。Controller 不注入 `UserService`。UseCase 不注入实现类，只注入端口。Spring 把 `NotifyRecipientDirectoryService` 填进端口。真正查用户的是 system API。这样通知模块可以在测试里替换端口，而不加载用户表。

**图的边界：** 不画出用户状态码含义、部门树、数据范围 SQL。`phoneNumber` 出现在 Vo 里，前端展示策略不是本课范围。

### 正例、反例与边界

**正例 1：** `keyword=zhang`、`pageSize=20`。UseCase clamp 后仍为 20，`directory.search("zhang", 20)`，包装成 PageResult。

**正例 2：** `pageSize=999`。`Math.clamp` 变成 50，端口最多要 50 行。

**正例 3：** `userIds=10, 20,30`。trim 后三个 Long，交给 `directory.byIds`。

**反例 1：** `keyword=` 空串。不查库，直接 total=0。不要以为这是「默认列出前 20 个用户」。

**反例 2：** `userIds=10,abc`。UseCase 抛 `IllegalArgumentException`；Controller `R.fail("用户编号格式错误")`。不会把 10 半包返回。

**反例 3：** 通知模块 `import` 用户 Mapper 自己搜。分层与端口就是为了禁止这条路。

**边界：** `pageSize=0` 会被 clamp 成 1，不是空。`by-ids` 不在 UseCase 里校验「这些 ID 是否仍有通知权限」——那是 `selectNotificationUsers` 的职责。Vo 不含 email，因此这个接口不能当邮件通讯录。

## 变式与迁移

- **变式 A：回填已选用户。** 编辑页重开草稿，拿 `recipientIds` 调 `by-ids`，而不是再 search 一次碰运气。
- **变式 B：只有 add 没有 edit 权限。** `SaMode.OR` 仍然能搜。两个权限都没有则 403。
- **迁移到发布：** 搜到的是候选人；发布时 Publisher 再解析一次，可能因为用户被禁用而变成空集，从而 `ServiceException("所选范围没有可接收通知的正常用户")`。
- **迁移到应用 API：** `NotificationCommand.recipientType=USER` 的 ID 不走这个 HTTP，业务模块直接持有用户 ID 调 `NotificationApplicationService`（L-007）。本接口是给人点选的。

## 常见误区

1. **「UseCase 注入了 DirectoryService。」** 字段类型是 `NotifyRecipientDirectoryPort directory`。实现类是运行时细节。
2. **「PageResult.total 是全库命中数。」** 这里 total = `rows.size()`，没有第二轮 count 查询。
3. **「空白关键字列出全部活跃用户。」** 与规范「ALL 不把全量用户加载到前端」一致：本接口拒绝无关键字浏览。
4. **「by-ids 格式错误返回空数组。」** HTTP 层是 fail 字符串，和空白参数的空列表不同。
5. **「这就是发送接口。」** 它不写 Outbox，不改公告。只读目录。

## 非评分暂停

想一想：若前端把 `pageSize` 设成 -1，Java 17+ 的 `Math.clamp` 会把它变成哪一端？你怎么用源码里的 `1, 50` 两个边界回答？

再想：为什么 `by-ids` 的格式错误要在 Controller 转成 `R.fail`，而 search 空白只返回空页？两种「不好的输入」为什么待遇不同？

## 总结、词汇表与下一步

- Controller → `NotifyRecipientUseCase` → `NotifyRecipientDirectoryPort`（实现 `NotifyRecipientDirectoryService` → `UserService`）。
- search 空白即空；pageSize 1..50；by-ids 非法数字失败。
- 权限挂在公告 add/edit 上。
- 词汇：directory port、recipient view、clamp、OR permission。

下一步：L-005 用监控接口看已经 submit 的意图和投递行，而不经过公告草稿。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-N-04 | `NotifyRecipientController.java`、`NotifyRecipientUseCase.java` | 路径、OR 权限、空白/clamp/格式错误 | `controller/admin`、`usecase` | 2026-09-14 |
| S-N-09 | `NotifyRecipientDirectoryPort.java`、`NotifyRecipientDirectoryService.java`、`NotifyRecipientUserVo.java` | 端口、UserService、Vo 字段 | `port/`、`service/runtime/`、`domain/vo/` | 2026-09-14 |
| S-N-10 | `notification.md` | ALL 不把全量用户加载到前端；USER 在服务端校验 | 「目标与渠道」 | 2026-09-14 |
