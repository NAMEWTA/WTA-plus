---
lesson_id: L-054
objective_ids: [OBJ-54]
claimed_cells:
  - A:createNotifyWebDomain
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: factory-and-runtime
    minutes: 8
  - segment: pages-consume-service
    minutes: 11
  - segment: visuals-and-worked-examples
    minutes: 8
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 4
expression_level: eli5
coverage_depth: deep
source_ids: [S-005, S-006, S-010, S-013, S-L054-01, S-L054-02, S-L054-03, S-L054-04, S-L054-05, S-L054-06, S-L054-07, S-L054-08]
---

# Lesson 054：宏观四键通知菜单——`createNotifyWebDomain` 如何消费 `notificationService`

## 学完你能做什么

打开厅堂菜单工厂 `frontend/packages/web-domains/notify/src/index.ts` 的 `createNotifyWebDomain`，再打开四张页和 runtime 托盘，你能**口述浏览器怎么把通知纸条贴到墙上**：服务员（Vue）只对托盘喊 `runtime.service.notices.*` / `inbox.*` / `config.*` / `deliveries(...)`，地址写在厨房 `createNotificationService`。不是 system 监控四页（L-024），不是顶栏消息盒子自己画的菜单，也不是「厨房被裁过所以没有 snapshot」。

口试名单就是矩阵 **(a)** 这一格，符号以**磁盘**为准：

1. **`A:createNotifyWebDomain`**（包 `@namewta/web-domain-notify`）：工厂返回冻住的 `WebDomainManifest`。id 是 `web-domain-notify`，`domainId` 是 `'notify'`。磁盘上正好 **4** 条 `componentKey`（测试锁死顺序）：`notify/monitor/index`、`notify/notice/index`、`notify/inbox/index`、`notify/config/index`。它**不** `addRoute`，**不**读 `Admin-Token`，**不**自己拼 `/notify/*` 字符串。

OBJ-54 还要你顺着四张门脸，把厨房方法喊到人脸上：

- **公告** `NoticePage`：`runtime.service.notices.list/get/save/publish/retract/remove`；指定用户走 `runtime.directory`，不是厨房上的第四只抽屉。
- **收件箱** `InboxPage`：`runtime.service.inbox.list` / `inbox.read`；读完喊 `runtime.inboxChanged`；推送刷新靠 `runtime.subscribeInbox`。
- **配置** `ConfigPage`：`runtime.service.config.accounts/addAccount/editAccount/changeStatus/removeAccount/scenes/saveScene/testAccount/testTemplate`。邮件有正文，短信禁止自由正文。
- **监控** `NotificationPage.vue`（`componentName` 却是 `NotificationMonitor`）：只喊顶层 `runtime.service.deliveries(query)`。**不**喊 `snapshot`。

2026-09-17 工作树先钉死**包边界**（口试先数包，再数函数）：

| 你可能以为的名字 | 磁盘事实 |
| --- | --- |
| `@namewta/web-domain-notify-monitor` 四个包 | **没有。** 一个包 `@namewta/web-domain-notify`，子路径 `./monitor` `./notice` `./inbox` `./config` 只是把 Vue 再 export 一次 |
| `createSystemWebDomain` 的通知键 | **没有通知键。** system 监控是 `monitor/{online,cache,operlog,logininfo}/index`（L-024） |
| `notificationService.monitor.deliveries` | **没有这层。** 监控两枪挂在厨房**根上**：`service.snapshot` / `service.deliveries`；公告才是 `service.notices`（复数） |
| `NotificationPage.vue` = `NotificationController` | **不是。** 文件名像应用 API，键却是 `notify/monitor/index`。submit/query/retry/cancel **不在**这座厨房对象上 |
| 页面 `import { notificationService } from '@/application/services'` | **四张 web-domain 页都不。** 厅堂把单例塞进 `runtime.service`；顶栏消息盒子和 `push.ts` 才直接 import 单例 |
| home 对称一份 `createNotifyWebDomain` | **没有。** `apps/home-web` 搜不到这个工厂 |
| `createNotifyWebDomain(undefined)` 会像档案楼那样当场关门 | **2026-09-17 没有 `requireNotifyWebRuntime`。** 测试甚至把 `service: {} as never` 传进去也能冻出四键 |
| 包内 `AGENTS.md` 写三页 | **过期。** 磁盘工厂、测试、菜单种子都是**四**页；缺的是配置页。以工作树为准 |

本课**不宣称**你会拆 `createNotificationService` 的 URL 全表与 `notificationDirectory` 三口（L-053）、八扇 Java 窗（父课 L-045…L-052 / 子课 notify L-001…L-008）、厅堂 `services.ts` 十二个工厂（L-007）、或把九个 web-domain 工厂一行标 covered（GP-L-020）。今天只认：**管理端这份通知菜单工厂怎样把 `notificationService` 喂进四页，四页实际扣了哪几枪，厨房里哪些枪还没人扣扳机。**

矩阵 (a) 那一行把九个工厂写在一起。本课只给 **`createNotifyWebDomain` 这一颗**当证据，**不要**把整行九厂标成 covered。

## 先把宏观地图放在桌上

L-007 已经把插头插进厅堂：`export const notificationService = createNotificationService(domainHttp)`，旁边另写 `notificationDirectory`。L-020 讲导航 host 怎样用 `sys_menu.component` 对上 `registration.load`。L-024 是**另一栋楼**的系统监控四页。子课 `children/2026-09-14-wta-notify/` 把后端八切片走完；本 Goal 把它们升级成 L-045…L-052，厨房是 L-053，菜单是本课。本课站在**已经登录的管理员浏览器**这一头。

三条河都叫 notify，货不一样：

| 名字听起来像 | 实际是什么 | 本课认不认 |
| --- | --- | --- |
| `createMonitorWebDomain` / `monitor/online/index` | system 保安亭四页（L-024） | 邻居：不要走错楼 |
| `createNotificationService` | 浏览器通知厨房：公告 / 收件箱 / 配置 / 监控两枪 | **厨房是 L-053**；本课认页面怎么喊它 |
| `notificationDirectory` | 厅堂自写的选人口：search / by-ids / userTypes | **runtime 邻口**；不是 `service` 上的字段 |
| `createNotifyWebDomain` | 管理端四键菜单 | **本课格子** |
| 顶栏 `layout/components/notice` + `utils/push.ts` | 消息盒子 / SSE；直接 import 厅堂单例 | **对照：同一厨房，不是本工厂的四键** |
| `NotifyNoticeController` 等八扇 | 后端 layered | L-045…L-052 |
| `NotificationController` submit/query/retry/cancel | 应用通知 API | L-051；**厨房对象上没有这些方法，也没有 Vue** |
| `ProviderCallbackController` | 供应商门铃 | L-050；**没有前端页** |

2026-09-17 工作树：权威厨房是 `frontend/packages/domains/notify/src/transport.ts` 的 `createNotificationService`。权威菜单包是 `frontend/packages/web-domains/notify/`。厅堂接线是 `apps/admin-web/src/application/services.ts` 第 70 行与 `apps/admin-web/src/router/adminManifestRegistry.ts` 第 291 行。`selectedManifestIds` 含 `'web-domain-notify'`。home **不**选这份 id。

菜单种子在 `50-cde-base-dml.sql`：目录 `通知中心`（`menu_id` `2100600000000000001`）下四张 C 型页，外加一批 F 型按钮。另有一块更早的补偿种子，把**同一张**监控页挂到「日志管理」下面。两颗种子的 `component` 都是 `notify/monitor/index`。

```text
已登录的管理员（Admin-Token）
        │
        ├─ 厅堂厨房
        │     notificationService = createNotificationService(domainHttp)
        │     notificationDirectory = { searchUsers, usersByIds, userTypes }  ← App 自写
        │
        ├─ 菜单工厂
        │     createNotifyWebDomain({
        │       service: notificationService,     ← 本课消费点
        │       directory: notificationDirectory,
        │       subscribeInbox, inboxChanged, hasPermission, navigate, dicts
        │     })
        │           id = web-domain-notify
        │           键：monitor / notice / inbox / config
        │
        └─ 顶栏消息盒子（不是本工厂的键）
              notificationService.inbox.list / read / readAll
              window 事件 'notify:inbox-updated'
```

往下走不要跳层：

```text
Vue 通知页（只收 runtime）
    └─ web-domain 外包一层 h(page, { runtime })
          └─ domain 工厂拼 URL / method
                └─ App 的 domainHttp（axios + Admin-Token）
                      └─ 后端 Notify* / NotificationMonitor Controller（L-045…L-049）
```

**类比失效边界：** 「宏观四键菜单」**不**等于「厨房里只有这四道菜」。厨房根上还有 `snapshot`，收件箱还有 `seen` / `readAll`，配置还有 `account(id)`。产品闸在 **页面喊哪些方法**，不在工厂里 `delete` 抽屉。类比也**不**等于「home 选了 `web-domain-notify` 就会少两道菜」——home 根本不选这份 id。谁在页面里喊了 `notificationService.notices.list`，卡车照样开。类比还不等于「菜单种子有 `notify:notification:submit` 就一定有提交页」——那是 F 型权限行，厨房和 Vue 都还没接。

## 核心概念与机制

### 直觉讲解

把 `createNotifyWebDomain` 想成**邮局大厅墙上的点菜单**，不是后厨本身。

- **点菜单（`createNotifyWebDomain`）**：四行：投递看板、写公告、自己的信箱、柜台公章。每一行写死 `componentKey`，load 时用 `h(page, { runtime })` 把托盘塞进页面。页面自己**不** import 厅堂单例。
- **托盘（`NotifyWebRuntime`）**：必带 `service`（厨房）、`directory`（选人）、`hasPermission`、`navigate`、`dicts`。可选两口：`subscribeInbox`（推送到了喊一声）、`inboxChanged`（已读了请同步顶栏）。
- **厨房（`runtime.service`）**：服务员只喊「来一份 list / save / deliveries」。地址印在 `createNotificationService`：`/notify/notice`、`/notify/inbox`、`/notify/config`、`/notify/monitor`。
- **门卫室（`runtime.directory`）**：指定用户时不要把整本系统用户册搬进通知包。厅堂把门卫室塞进托盘；空关键字在厅堂短路，不打 `/notify/recipients/search`。

小孩子版只记十二句：

1. **先找工厂，再找四键。** `export function createNotifyWebDomain(runtime: NotifyWebRuntime)`。封面 id 永远是 `web-domain-notify`。
2. **四键不是随便排。** 测试锁死：monitor → notice → inbox → config。
3. **厨房从托盘进门，不从 `@/application/services` 进门。** 四页只认 `props.runtime`。
4. **公告抽屉叫 `notices`（复数）。** 菜单键却是 `notify/notice/index`（单数）。口试先数拼写。
5. **监控两枪在厨房根上。** `service.deliveries`，不是 `service.monitor.deliveries`。
6. **保存草稿 ≠ 发布。** 对话框按钮写「保存草稿」；列表再点「发布」。
7. **已见 ≠ 已读。** 厨房两枪都在；收件箱页只扣 `read`。顶栏才会 `readAll`。
8. **短信不能自由写信。** 配置测试把短信场景块锁成「供应商模板码 + 参数映射」，没有 textarea。
9. **选人不是厨房方法。** `RecipientUserPicker` 吃 `directory`。
10. **工厂今天不检查托盘齐不齐。** 缺方法会在点击时炸，不会在 `createNotifyWebDomain` 当场 throw。
11. **消息盒子不是第五键。** 它直接喊同一座厨房，靠 window 事件跟收件箱页打招呼。
12. **应用提交 API 不在这本点菜单上。** 种子里有 F 型「通知提交」，墙上没有那道菜。

**类比失效边界：** 点菜单类比**不**覆盖「短信字节怎么进阿里云」。试发仍走厨房 `testAccount` / `testTemplate`，后端再进统一通知（L-047 / L-051）。类比也**不**等于「墙上没挂 snapshot 厨房就把刀收了」——`service.snapshot` 还在。类比还不等于「`NotificationPage.vue` 是应用通知柜台」——那张 Vue 只刷投递流水。

### 精确定义与 English term

| 中文口头 | English term | 磁盘落点 |
| --- | --- | --- |
| 管理端通知菜单工厂 | `createNotifyWebDomain` | `web-domains/notify/src/index.ts`；manifest id `web-domain-notify` |
| 菜谱板 | `WebDomainManifest` | `id` / `domainId` / `permissions` / `registrations` / 空的 `messages` |
| 组件键 | `componentKey` | 与 `sys_menu.component` 对表：`notify/notice/index` 等 |
| 运行时托盘 | `NotifyWebRuntime` | `src/runtime.ts`；`service` 类型是 `ReturnType<typeof createNotificationService>` |
| 通知厨房 | `notificationService` | 厅堂导出名；工厂 `createNotificationService` 在 `domains/notify/src/transport.ts` |
| 选人目录 | `NotifyUserDirectory` | runtime 口；厅堂实现 `notificationDirectory` |
| 资源标签 | resource metadata | `notifyNoticeResource.controller = 'NotifyNoticeController'` 等；**不是** Vue 文件名 |
| 权限目录 | permissions catalog | 工厂四组 id：`notify-monitor` / `notify-notice` / `notify-inbox` / `notify-config` |
| 已见 | seen | `POST /notify/inbox/{id}/seen`；本课四页**不喊** |
| 已读 | read | `POST /notify/inbox/{id}/read`；收件箱页 + 顶栏 |
| 投递流水 | deliveries | `GET /notify/monitor/deliveries`；监控页唯一一枪 |
| 通知快照 | snapshot | `GET /notify/monitor/snapshot`；厨房有，监控页**无按钮** |
| 失败关闭 | fail-closed | 档案楼 `requireProfileWebRuntime` 那套；**本工厂 2026-09-17 没有** |
| 宿主端口 | host port | `subscribeInbox` / `inboxChanged` / `navigate` / `dicts` / `hasPermission` |

厨房对象形状（L-053 的封面，本课用来对扳机）：

| 厨房字段 | HTTP 门牌 | 本课四页用不用 |
| --- | --- | --- |
| `notices.list/get/save/publish/retract/remove` | `/notify/notice/*` | **用。** `NoticePage` |
| `inbox.list/read` | `GET /notify/inbox`；`POST .../read` | **用。** `InboxPage`（顶栏也用 list/read） |
| `inbox.seen` | `POST .../seen` | **无 Vue 调用** |
| `inbox.readAll` | `POST /notify/inbox/read-all` | **顶栏用。** 收件箱页无「全部已读」 |
| `config.accounts/addAccount/editAccount/changeStatus/removeAccount/scenes/saveScene/testAccount/testTemplate` | `/notify/config/*` | **用。** `ConfigPage` |
| `config.account(id)` | `GET /notify/config/account/{id}` | **无 Vue 调用**；编辑直接抄表格行 |
| `deliveries` | `GET /notify/monitor/deliveries` | **用。** `NotificationPage` |
| `snapshot` | `GET /notify/monitor/snapshot` | **无 Vue 调用** |
| 应用 API submit/query/retry/cancel | `/notify/notification` | **厨房对象上没有这些方法** |
| 供应商 callback | `/notify/callback/{channel}` | **厨房无方法；无页面** |
| 收件人 search / by-ids | `/notify/recipients/*` | **directory 口**，不在 `service` 上 |

`notifyDomainModule.capabilities` 只有 `'notification-control-plane'` / `'notification-monitor'`。这是名片，**不是** componentKey，也不决定 App 挂哪几页。

### 机制/因果链

**1. 工厂把四键冻进同一本菜单，但不检查托盘。**

`createNotifyWebDomain(runtime)` 立刻 `Object.freeze` 一份 manifest。`registrations[].load` 闭包住这份 `runtime`，再 `defineComponent({ setup: () => () => h(page, { runtime }) })`。测试锁死四键顺序与 config 六串权限。把 `service: {} as never` 传进去**也能**得到四键——缺方法要等页面 `onMounted` 才爆。对照 L-044：档案楼会 `requireProfileWebRuntime`，缺十一口 archive 当场 throw。口试不要把两座工厂的关门策略背成一句。

权限目录四组（字符串以工厂为准）：

| id | 冻进去的串 | 四页按钮实际核对 |
| --- | --- | --- |
| `notify-monitor` | `notify:monitor:list`、`notify:monitor:query` | 监控页**没有** `hasPermission`。进门靠菜单 `list`。`query` 种子备注是「查询通知快照」，页上没有 snapshot 按钮 |
| `notify-notice` | list / query / add / edit / publish / retract / remove | 新增、编辑、发布、撤回、删除各核一串 |
| `notify-inbox` | list / seen / read | 页上**不核**权限。没有 `readAll` 这一串 |
| `notify-config` | list / query / add / edit / remove / test | 新增、编辑、启停（edit）、删除、试发 |

`composeAppRuntime` 只有 `selectedManifestIds` 含 `web-domain-notify` 时才把键交给导航。admin 厅堂这份 id 在 `adminManifestRegistry.ts` 末尾；home **不**选它。registry 测试另锁：`monitor/notify/index` + `domainId: 'system'` 是 `undefined`；`notify/monitor/index` + `'notify'` 才是 `NotificationMonitor`。

**2. 厅堂托盘把厨房、选人、推送从页面 import 里拆出去。**

`adminManifestRegistry.ts` 第 291 行：

| runtime 口 | 厅堂接到哪 | 四页怎么用 |
| --- | --- | --- |
| `service` | `notificationService` | 所有 HTTP 方法 |
| `directory` | `notificationDirectory` | 公告选人；`userTypes` 其实转 `systemService.userTypes.options()` |
| `subscribeInbox` | `window.addEventListener('notify:inbox-updated', handler)` | 收件箱 `onMounted` 订阅，卸载解开 |
| `inboxChanged` | 动态 import `initMessageBox` | 收件箱标记已读后刷新顶栏 |
| `hasPermission` | `createAdminAccessEvaluator()` | 公告 / 配置按钮；收件箱和监控页不用 |
| `navigate` | `router.push` | 收件箱详情「查看业务」 |
| `dicts` | `createLiveSystemDictRefs` → `useDict` | 生命周期、渠道、投递状态、消息分类、`sys_notice_type` |

口试不要说「页面直接 `import notificationDirectory`」。选人组件只收 `directory` prop。

**3. 公告页走 `notices` 六枪，发布与保存拆开，选人走 directory。**

`NoticePage`：

- 列表 `notices.list({ ...query })`。查询条件是标题、类型、分页。
- 只有 `lifecycle === 'DRAFT' | 'RETRACTED'` 才 `editable`。已发布只能撤回，不能编辑。
- 编辑先 `notices.get(noticeId)`，再打开对话框。保存一律 `notices.save`，成功文案是「草稿已保存，可在列表中发布」。
- 发布 / 撤回 / 删除走 `runAction`。删除 `notices.remove([id])`（数组）；发布/撤回点名单枪。确认框会把渠道和发送对象念一遍，并写明停用/删除用户会被过滤。
- 渠道 UI 只放字典里属于 `IN_APP | SMS | MAIL` 的项。默认草稿渠道是 `['IN_APP']`。
- `recipientType`：`ALL` 提示发给全部正常用户；`USER` 挂 `RecipientUserPicker`；`USER_TYPE` 调 `directory.userTypes()`，再 `status === '0'` 过滤。保存时把另外一种 id 数组清空。
- 字典：`notify_notice_lifecycle`、`sys_notice_type`、`notify_channel`。
- 按钮核 `notify:notice:add|edit|publish|retract|remove`。列表查询不核 `query`——进门已经靠菜单 `notify:notice:list`。

`useRecipientSelection`：空白不请求；输入 300ms 防抖；翻页/换关键词**保留**已选；表头复选只作用于当前页；`usersByIds` 用来恢复草稿名单。这是 web-domain 里的选人剧本，HTTP 仍打 directory，不打 `service.notices`。

**4. 收件箱页只 list + read，已见枪和全部已读留给别人。**

`InboxPage`：`inbox.list()` 一次拿全部，**没有**分页组件。状态列看 `readTime`，不看 `seenTime`。点行或「查看详情」若未读就 `inbox.read`，再 `runtime.inboxChanged?.()`。有 `path` 才出现「查看业务」，走 `runtime.navigate`。`onMounted` 订阅 `subscribeInbox`；`onActivated` 在 keep-alive 下再刷一次。

2026-09-17 全仓 Vue：**没有** `inbox.seen(`。种子却有 F 型 `notify:inbox:seen`。厨房枪在，扳机不在本页。

`readAll` 在顶栏 `layout/components/notice/index.vue`：直接 `notificationService.inbox.readAll()`，再 `refreshMessageInbox()`。收件箱页没有这个按钮。工厂权限目录也**没有** `notify:inbox:readAll` 这一串。

推送：`utils/push.ts` 的 `initMessageBox` 同样 `notificationService.inbox.list()`，把结果投影进 Pinia。SSE/WebSocket 到了只 `dispatchEvent('notify:inbox-updated')` 再刷盒子——实时通道**不**当已读事实，已读仍以后端为准。

**5. 配置页两张 TAB，邮件能写信，短信只能填模板码。**

`ConfigPage`：`channel` 是 `'MAIL' | 'SMS'`，没有 IN_APP 配置页——站内信不需要 SMTP / 厂商钥匙。账号表 `config.accounts(channel, pageNum, pageSize)`；场景表 `config.scenes(channel)`。新增/编辑走 `addAccount` / `editAccount`；启停 `changeStatus(id, 'Y'|'N')`；删除先 confirm 再 `removeAccount`。编辑打开时把 `mailPass` / `accessKeySecret` 清空，列表只显示 `mailPassSet` / `accessKeySecretSet` 布尔。**不**先打 `config.account(id)`。

场景绑定 `saveScene`：邮件写 `mailSubject` / `mailBody`，可点变量插入 `${name}`，并计算「已占用变量」；短信只 `smsTemplateCode` + 每个变量映射到供应商参数名。源码契约测试锁死短信块：有「禁止自由正文」，**没有** `type="textarea"`，**没有** `mailBody`。未绑定账号时下拉提示「未绑定则该渠道失败关闭」——这是 UI 文案，失败关闭发生在后端（L-047）。

试发：账号试发 `testAccount({ accountId, target })`（页不传 `sceneCode`）；模板试发 `testTemplate({ sceneCode, channel, target })`。成功提示用厨房返回的 `result.data`。

**6. 监控页只刷 deliveries，文件名像应用 API。**

`NotificationPage.vue` 的 `componentName` 是 `NotificationMonitor`，键是 `notify/monitor/index`。查询条件 `userId` / `channel` / `status`，一枪 `runtime.service.deliveries(query)`。没有 snapshot 输入框，没有 `notificationId`。`load()` **没有** `catch`——失败时表格可能停在旧数据，loading 仍会关掉。字典：`notify_channel`、`notify_delivery_status`；`DISPATCH_ERROR` 在页内有一份 fallback「投递异常」。

种子 F 型「通知详情」perms 是 `notify:monitor:query`，备注写「查询通知快照」。页上没有对应按钮。不要把菜单备注说成已经做了的 UI。

**7. 厨房有枪、本工厂页面没扣扳机；更外面还有根本没写进厨房的窗。**

四页 + 顶栏 2026-09-17 **不喊**：`snapshot`、`inbox.seen`、`config.account`。顶栏喊 `readAll`，四页不喊。

厨房对象上**根本没有**：`/notify/notification` 四枪、`/notify/callback/{channel}`。`domains/notify` 的 `./notification` / `./callback` 子路径只冻了 `controller` + `basePath` 标签，并 re-export **同一份** `createNotificationService`。不要把「有子路径」说成「有 submit 方法」。

种子里的 `notify:notification:submit|retry|cancel|query` 是 F 型权限行，挂在通知中心目录下，**没有** component。导航不会为它们 `addRoute`。

**8. 两颗监控菜单种子，同一把钥匙。**

`50-cde-base-dml.sql`：

- 补偿块 `NAMEWTA-OSS-NOTIFY-DSL-001`：父菜单「日志管理」`1761400000000000108`，path `notify-monitor`，component 仍是 `notify/monitor/index`，perms `notify:monitor:list`。子按钮却是 **`system:notify:query` / `system:notify:remove`**——这两串不在本工厂权限目录里，四页也不核它们。
- 通知中心块：父菜单 `2100600000000000001`，path `monitor`，同一 component，perms 仍是 `notify:monitor:list`，子按钮才是 `notify:monitor:query`。

两颗种子都能让 `getRouters` 吐出同一 `componentKey`，registry 对上同一份 `NotificationMonitor`。不要把「日志管理里也有通知监控」说成 system 监控四页，也不要发明第二张 Vue。

### 图、表或文本图

**图 1：宏观四键怎样吃到厨房**

```text
 sys_menu.component（管理端 Client）
   notify/monitor/index     通知中心 / 也可从「日志管理」那颗旧种子进来
   notify/notice/index
   notify/inbox/index
   notify/config/index
   （F 型 notify:notification:* 没有 component，不上图）
        │
        v
 GET /system/menu/getRouters        已按人 + Client 裁剪（L-020）
        │
        v
 App composeAppRuntime
   selectedManifestIds 含 web-domain-notify
        │
        ├─ 键在已选 manifest → registration.load
        │     defineComponent({ setup: () => () => h(Page, { runtime }) })
        └─ 键不在已选清单 → 解析失败关闭
           monitor/notify/index + domain system → undefined（测试锁死）
                │
                v
         Page props.runtime
                │
                ├─ runtime.service = createNotificationService(...)   厨房
                ├─ runtime.directory = notificationDirectory          选人
                ├─ subscribeInbox / inboxChanged                      顶栏同步
                └─ 页面只喊方法名，不拼 URL
```

**图题 / caption：** 宏观同一厨房、管理端四键、托盘把厨房和选人一起塞进页面。alt：菜单键来自 sys_menu；admin 选 `web-domain-notify`；页面只收到 runtime；HTTP 字符串在 domain 工厂。

**文字等价物：** 人先碰到动态菜单里的 component 字符串。App 用已选 manifest 把字符串换成带 runtime 的 Vue 页。四张通知页面向 `runtime.service` 喊方法；公告选人另走 `runtime.directory`。厨房把方法换成 GET/POST 和 `/notify/...`。home 即使以后有人把厨房单例造出来，墙上没有这四键，导航也不会挂页。`monitor/notify/index` 这种把 system 监控目录和 notify 拼在一起的键，registry 明确拒绝。

**图 2：厨房有枪 / 本课页面扣扳机**

| 厨房方法 | HTTP（动词以 transport 为准） | 2026-09-17 谁扣扳机 |
| --- | --- | --- |
| `notices.list/get/save/publish/retract/remove` | GET list 与 `/{id}`；POST save / `/{id}/publish` / `/{id}/retract` / remove | **NoticePage** |
| `inbox.list` | GET `/notify/inbox` | **InboxPage** + `push.ts` 消息盒子 |
| `inbox.read` | POST `/{id}/read` | **InboxPage** + 顶栏消息盒子 |
| `inbox.readAll` | POST `/read-all` | **仅顶栏** |
| `inbox.seen` | POST `/{id}/seen` | **无 Vue** |
| `config.accounts/add/edit/changeStatus/remove/scenes/save/test*` | `/notify/config/account*` `/scene*` `/test*` | **ConfigPage** |
| `config.account(id)` | GET `/account/{id}` | **无 Vue** |
| `deliveries` | GET `/notify/monitor/deliveries` | **NotificationPage** |
| `snapshot` | GET `/notify/monitor/snapshot` | **无 Vue** |
| 应用 submit/query/retry/cancel | `/notify/notification` | **厨房无方法，无 Vue** |
| `directory.searchUsers/usersByIds` | GET `/notify/recipients/search`、`/by-ids` | **RecipientUserPicker**（经 runtime.directory） |
| `directory.userTypes` | 厅堂转 `systemService.userTypes.options()` | **NoticePage** 用户类型下拉 |

**图题 / caption：** 工厂注册了四键 ≠ 厨房每支枪都有按钮。alt：公告六枪、收件箱 list/read、配置九枪、deliveries 有页面；snapshot、seen、按 id 取账号没有；应用 API 连厨房字段都没有。

**文字等价物：** 管理员四页消耗公告六枪、收件箱两枪、配置账号/场景/试发、监控流水。选人两枪打在 directory 上。顶栏额外消耗 list/read/readAll，并靠 `notify:inbox-updated` 跟收件箱页互刷。snapshot、seen、按 id 取账号目前只有厨房（和后端）。应用提交四枪连厨房封面都没印。不要把「工厂注册了四键」说成「所有 `/notify/*` HTTP 都有按钮」。

**图的边界：** 不画 Java 五层（L-045…L-052）。不画 SSE 帧格式。不保证以后产品会补 snapshot 页或应用提交页。不把 `capabilities` 名片画成菜单。不把 `sys_notify_log` 旧表当本课页面的列表源——收件箱读的是通知模块自己的站内信（L-046）。

## 正例、反例与边界

**正例 1 — 管理员翻公告名册。** 有 `notify:notice:list` 的人打开 `notify/notice/index`。页 `notices.list({ noticeTitle, noticeType, pageNum, pageSize })`。厨房 `GET /notify/notice/list`。

**正例 2 — 保存草稿再发布。** 持 `notify:notice:add` 点「新增」。填标题、类型、至少一条渠道、正文。对象选「指定用户」时，选人框 `directory.searchUsers('admin', 1, 20)` → `GET /notify/recipients/search`。保存 `POST /notify/notice/save`。列表再点「发布」才 `POST /notify/notice/{id}/publish`。对话框里没有「保存并发布」合一按钮。

**正例 3 — 已发布不能编辑。** 行 `lifecycle === 'PUBLISHED'` 时编辑按钮不出现。若详情回来已经不是 DRAFT/RETRACTED，页 warning「此通知已经发布，不能编辑」并刷新列表。撤回后重新变成可编辑。

**正例 4 — 收件箱点开即已读。** 打开 `notify/inbox/index`，`GET /notify/inbox`。点一行 → `POST /notify/inbox/{messageId}/read` → `inboxChanged` → 厅堂 `initMessageBox` 再 `inbox.list` 一次。若此时 SSE 也派了 `notify:inbox-updated`，收件箱 `subscribeInbox` 会再 `load()`。已读事实以服务端 `readTime` 为准。

**正例 5 — 顶栏全部已读。** 消息盒子点「全部已读」→ `notificationService.inbox.readAll()` → `refreshMessageInbox()`（先派 window 事件，再刷盒子）。收件箱页若已挂着，会跟着刷新。这**不是** `createNotifyWebDomain` 的第五键。

**正例 6 — 邮件场景绑定。** 配置页 TAB「邮件」，点场景「绑定」，选启用中的账号，插入 `${code}` 到主题或正文，`saveScene` POST `/notify/config/scene/save`。密钥列表列只显示「已设置 / 未设置」。

**正例 7 — 短信场景禁止自由正文。** TAB「短信」的绑定对话框要供应商模板码和参数映射。测试读源码：短信块没有 textarea、没有「邮件正文」。试发 `testTemplate({ sceneCode, channel: 'SMS', target: 手机号 })`。

**正例 8 — 监控流水筛选。** `deliveries({ channel: 'SMS', status: 'FAILED' })` → `GET /notify/monitor/deliveries?channel=SMS&status=FAILED`。transport 测试锁死这条 params。页还可以填 `userId`。

**正例 9 — 旧日志菜单仍打开同一张监控页。** `component` 同为 `notify/monitor/index`。registry 不在乎父菜单叫「日志管理」还是「通知中心」。

**反例 1 — 「四张 Vue `import { notificationService }`。」** 全包搜不到 `@/application/services`。直接 import 的是顶栏和 `push.ts`。

**反例 2 — 「`createNotifyWebDomain` 等于九个 web-domain 工厂都 covered。」** 矩阵 (a) 把九个名字写在同一行。GP-L-020 / L-044 已经挖过：不要整行盖章。本课只给通知这一颗。

**反例 3 — 「`NotificationPage.vue` 会 `submit` 统一通知。」** 那张页只 `deliveries`。submit 是 L-051 的 Java 窗；厨房 facade 2026-09-17 没收这四枪。

**反例 4 — 「系统监控里的通知页是 `monitor/notify/index`。」** 测试：该键 + `system` 解析为 `undefined`。真键是 `notify/monitor/index` + `notify`。

**反例 5 — 「监控页会打 snapshot。」** 厨房有 `snapshot(notificationId)`。Vue 搜不到 `service.snapshot` / `runtime.service.snapshot`。

**反例 6 — 「收件箱点开走 `seen`。」** 页走 `read`。`seen` 只有 transport 和 OpenAPI 快照。

**反例 7 — 「`notificationService.recipients.search`。」** 没有这个字段。选人是 `runtime.directory.searchUsers`。L-007 已经把交叉线钉在厅堂。

**反例 8 — 「`createNotifyWebDomain(undefined)` 会给出空菜单。」** 今天不会在工厂里 throw。对照档案楼不要背错。缺 `service.notices.list` 的失败发生在 `onMounted`。

**反例 9 — 「短信场景能像邮件一样写正文。」** 测试禁止。后端模板闸是 L-047；本课 UI 已经先把 textarea 拿掉。

**反例 10 — 「home 的厨房没有 inbox。」** home **没有**这座厨房，也没有这份菜单工厂。不要用 L-039「同一厨房两份菜单」那句话套到 notify——档案楼那句成立，是因为 home 真的调了 `createProfileService`。

**反例 11 — 「包 README/AGENTS 写三页，所以没有配置页。」** `AGENTS.md` 漏了 `notify/config/index`。工厂、测试第二例、菜单种子、`ConfigPage.vue` 都在。工作树优先。

**反例 12 — 「`capabilities` 有 notification-monitor 所以一定有 snapshot 按钮。」** 名片有能力，页面可以不挂。今天就是这样。

**边界 1 — 工厂不校验 runtime ≠ 厨房方法不存在。** 缺 `deliveries` 仍能 `createNotifyWebDomain`。页面一喊才会在运行时失败。

**边界 2 — 顶栏与收件箱页共享厨房，不共享 Vue。** 已读同步靠 `inboxChanged` 和 `notify:inbox-updated`，不是把 InboxPage 嵌进顶栏。

**边界 3 — `hasPermission` 藏按钮；HTTP 仍可能 403。** 收件箱 / 监控页连这道 UI 闸都没有，进门只靠菜单。厨房把错误原样抛出（公告/配置用 `error.message`）。

**边界 4 — 公告 `remove` 的 body 是 id 数组。** 配置 `removeAccount` 的 body 是单个 id。不要背成同一种 payload。

**边界 5 — 公告编号插进 URL 模板，没有 `encodeURIComponent`。** 对照档案楼材料出门条会编码斜杠。本课厨房 ` /notify/notice/${noticeId}` 按磁盘直插。口试不要发明「通知也 `%2F`」。

**边界 6 — IN_APP 是公告渠道，不是配置 TAB。** 配置只 MAIL/SMS。不要问「站内信的 SMTP 填哪」。

**边界 7 — 字典与硬编码并存。** 渠道/生命周期/投递状态走 `runtime.dicts`；配置 TAB 名、短信「禁止自由正文」、公告 `supportedChannels` 写在 Vue 里。

**边界 8 — 匿名回调无页面。** 不要为 `ProviderCallbackController` 找 `src/callback/` 下的 Vue。domain 的 `./callback` 只有资源标签。

**边界 9 — 旧按钮串 `system:notify:*` 不是本工厂目录。** 那是日志管理补偿种子的子权限。四页不核它们，也不调用「通知删除」那种 system 风格窗。

**边界 10 — `readAll` 没有菜单 F 型行。** 顶栏仍然调用。授权以后端 `NotifyInboxController.readAll` 为准（L-046），不要用「菜单没有就不能喊」反推厨房。

## 变式与迁移

1. **和 L-053 对照。** 厨房认 `createNotificationService` + `notificationDirectory`。本课认菜单工厂怎样把 `service` 放进 runtime，以及四页扣了哪些扳机。不要把 URL 全表在本课再抄一遍；口试若被问「list 的 path」，指回 transport，不要指 Vue。
2. **和 L-007 对照。** 厅堂才把 `notificationDirectory.userTypes` 接到 `systemService`。web-domain 包的 `package.json` **没有** `@namewta/domain-system`。以后换厅堂，只要再实现三口 directory，不必让通知包 import system。
3. **和 L-020 对照。** 导航 host 仍是 `getRouters` → `resolveAdminWebRegistration` → `addRoute`。notify 不另写一套路由恢复。键找不到就是诊断，不是 glob 扫 `views/notify/`。
4. **和 L-024 对照。** 系统监控四页吃 `createMonitorService`，manifest id `web-domain-system-monitor`，`domainId` 仍是 `'system'`。通知监控吃 `notificationService.deliveries`，id `web-domain-notify`，`domainId` `'notify'`。iframe 运维（snailjob 等）是第三块板。三套「监控」不要画成一个抽屉。
5. **和 L-044 对照。** 都是「页面经 runtime 消费厨房」。差别：档案楼 fail-closed 校验十一口；通知楼工厂不校验。档案楼企业贡献是同一工厂里的后三键；通知楼自己就是第四个（矩阵行里的）工厂，四键一次冻住。
6. **和子课 L-001 / 父课 L-045 对照。** 后端发布才会把草稿变成统一通知。前端「保存草稿」不会发信。不要把 `notices.save` 说成已经进 Outbox。
7. **和子课 L-002 / 父课 L-046 对照。** 后端分 seen / read / readAll。前端收件箱页只 read；顶栏加 readAll；seen 尚未接线。
8. **和子课 L-005 / 父课 L-049 对照。** 后端 snapshot 与 deliveries 是两条查询链。前端目前只挂 deliveries 这一条看板。
9. **以后若要补快照页。** 应喊 `runtime.service.snapshot(notificationId)`，不要在 Vue 里手写 `/notify/monitor/snapshot`。菜单 F 型 `notify:monitor:query` 已经在种子里，缺的是按钮和输入框，不是权限名。
10. **以后若要应用提交柜台。** 先让 L-053 的厨房长出 submit/query/retry/cancel，再在 web-domain 加第五键。不要让页面直接打 `/notify/notification`，也不要把 `NotificationPage.vue` 改名当那张柜台——文件名已经够容易撞车。
11. **以后若要「只要公告」的瘦菜单。** 今天的工厂**会**把四键一起注册。不能靠「只用 NoticePage」让工厂少返回三键。要瘦，得改工厂或让 App 不选那些菜单种子。
12. **换 App。** 第三份 App 若只要收件箱，仍须 `createNotificationService` + 一份含 inbox 键的 manifest + 能订阅推送的 runtime。把 admin 的 `notificationService` 单例跨 App 偷走，口试先数 L-007「组合可以复制，单例不能跨 App 偷」。
13. **迁移口诀：** 先数工厂四键顺序 → 再数 `runtime.service` 真正喊出的方法（注意 `notices` 复数、监控在根上）→ 再数 directory 三口 → 再数顶栏旁路 list/read/readAll → 最后数厨房里还没扣扳机的 snapshot/seen/account(id)，以及根本没写进厨房的应用 API。跳步会出现「把 NotificationPage 说成 submit」「把 seen 说成收件箱默认动作」「把九厂一行盖章」。

## 常见误区

1. **「OBJ-54 包含 `createNotificationService`。」** 那是 OBJ-53。本课认菜单工厂与四页如何消费它。
2. **「OBJ-54 包含 `NotifyNoticeController`。」** 那是 OBJ-45 / 子课 L-001。
3. **「把九个 web-domain 工厂一行标 covered。」** 本课只给 `A:createNotifyWebDomain`。
4. **「`service.notices` 写成 `service.notice`。」** 厨房字段是复数；菜单键是单数。
5. **「`service.monitor.deliveries`。」** deliveries 在根上。
6. **「页面自己拼 `/notify/notice/list`。」** 厨房才拼。
7. **「选人走 `createSystemService.users`。」** 走 notify 的 recipients HTTP，经 directory；用户类型才借 system。
8. **「配置页也能管站内信账号。」** TAB 只有邮件和短信。
9. **「InboxPage 有全部已读。」** 在顶栏。
10. **「`subscribeInbox` 是厨房方法。」** 是厅堂把 window 事件塞进 runtime 的可选口。
11. **「工厂缺 runtime 会 throw。」** 2026-09-17 不会。
12. **「`AGENTS.md` 是四页的权威名单。」** 它漏了 config。测试才是权威。
13. **「日志管理下的通知监控是 `createMonitorWebDomain`。」** component 已经是 `notify/monitor/index`。
14. **「前端授权。」** 藏按钮 ≠ 授权。后端仍是最终授权者。
15. **「capabilities 决定挂哪几页。」** 决定挂页的是 manifest + 菜单种子 + `selectedManifestIds`。
16. **「home 也能打开通知中心。」** 工作树没有这份工厂接线。

## 非评分暂停

打开磁盘，不要凭记忆默写路径。不要改文件。没有标准答案栏，没有分数。

1. 打开 `frontend/packages/web-domains/notify/src/index.ts`。圈 id `web-domain-notify`、四键顺序、四组 permissions、`h(page, { runtime })`。打开 `index.test.ts`，核对 `service: {} as never` 仍能冻出四键，以及 ConfigPage 源码契约（短信无 textarea）。
2. 打开 `src/runtime.ts`。圈 `service: ReturnType<typeof createNotificationService>`、`directory` 三口、可选的 `subscribeInbox` / `inboxChanged`。确认**没有** `requireNotifyWebRuntime`。
3. 打开 `NoticePage.vue`、`notice/RecipientUserPicker.vue`、`InboxPage.vue`、`ConfigPage.vue`、`NotificationPage.vue`。圈各自真正喊出的 `runtime.service.*`。确认没有 `snapshot`、没有 `inbox.seen`、没有 `config.account(`。
4. 打开 `apps/admin-web/src/router/adminManifestRegistry.ts` 第 291 行附近。圈 `service: notificationService`、`directory: notificationDirectory`、`notify:inbox-updated`、`selectedManifestIds` 里的 `'web-domain-notify'`。打开 `adminManifestRegistry.test.ts`，核对 `monitor/notify/index` 为 `undefined`、`notify/monitor/index` 的 `componentName`。
5. 打开 `apps/admin-web/src/layout/components/notice/index.vue` 与 `utils/push.ts`。圈直接 import 的 `notificationService.inbox.list/read/readAll`，以及 `refreshMessageInbox` 派发的事件名。这不是第五键。
6. 打开 `domains/notify/src/transport.ts`。圈根上的 `snapshot` / `deliveries`、`notices` 复数、inbox 四枪、config 十枪。在 web-domains 里搜 `snapshot` 与 `inbox.seen`，确认不是页面调用。再看 `src/notification/index.ts`：只有资源标签，没有 submit 方法。

## 总结、词汇表与下一步

- **宏观四键通知菜单：** `createNotifyWebDomain` 把四张页订进 `web-domain-notify`。墙上是监控流水 / 公告 / 收件箱 / 配置。厨房方法全集 ≠ 墙上的菜 ≠ 页面已扣的扳机。
- **(a) `createNotifyWebDomain`：** 工厂冻住四键与四组 `notify:*` 权限串，用 `h(page, { runtime })` 把厅堂的 `notificationService` 喂进页面。本课**不**把矩阵那一行九个工厂一起闭合。
- **页面实际调用：** 公告走 `notices` 六枪 + directory 选人；收件箱走 list/read + 宿主推送口；配置走账号/场景/试发（短信无自由正文）；监控只 `deliveries`。`snapshot`、`seen`、按 id 取账号目前没有 Vue。应用 API 连厨房字段都没有。
- **顶栏是旁路，不是第五键。** 同一座厨房的 list/read/readAll，靠 `notify:inbox-updated` 跟收件箱页打招呼。不是 system 监控，不是 home 菜单。

词汇表：`createNotifyWebDomain` / `NotifyWebRuntime` / `notificationService` / `NotifyUserDirectory` / `componentKey` / `web-domain-notify` / `notices`（复数）/ `deliveries` / `snapshot` / seen vs read / `subscribeInbox` / `inboxChanged` / resource metadata / permissions catalog / host port。

下一步：厨房 URL 与 directory 三口是 OBJ-53。公告/收件箱/配置/收件人/监控/回调/应用 API/Outbox 的 Java 五层是 OBJ-45…OBJ-52（子课 L-001…L-008）。系统监控四页是 OBJ-24。厅堂插头是 OBJ-07。导航 host 是 OBJ-20。本课结束仍不是掌握证明；要练习请之后主动激活 Homework。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-005 | `frontend/apps/admin-web` | 厅堂 `notificationService` / `notificationDirectory`；registry 注入 runtime；home 无此工厂 | `application/services.ts` 第 70–88 行；`router/adminManifestRegistry.ts` 第 291–376 行 | 2026-09-17 |
| S-006 | `frontend/packages/{domains,web-domains}/notify` | 厨房 facade、web-domain 四键、runtime 类型、子路径 export | 各包 `package.json` `exports` 与 `src` | 2026-09-17 |
| S-010 | `release-artifacts/docker/infrastructure/mysql/init/50-cde-base-dml.sql` | 通知中心四键 + F 型按钮；日志管理补偿种子同一 `notify/monitor/index`；字典 `notify_*` | `210060…` 菜单块；`1761400000000000125`；dict 1671 行附近 | 2026-09-17 |
| S-013 | child `2026-09-14-wta-notify` course / L-001…L-005 | 后端八切片与 snapshot/seen/发布链；本课只对照不复述五层 | `children/2026-09-14-wta-notify/` | 2026-09-17 |
| S-L054-01 | `web-domains/notify/src/index.ts`；`index.test.ts` | 四键顺序；permissions 四组；`h(page, { runtime })`；config 源码契约；`service: {} as never` | `createNotifyWebDomain`；两个 `it` | 2026-09-17 |
| S-L054-02 | `web-domains/notify/src/runtime.ts` | `NotifyWebRuntime` / `NotifyUserDirectory`；`service` 类型来自 `createNotificationService` | 接口字段 | 2026-09-17 |
| S-L054-03 | `NoticePage.vue`；`notice/RecipientUserPicker.vue`；`useRecipientSelection.ts` 与 `.test.ts` | notices 六枪；directory 选人；DRAFT/RETRACTED 可编辑；空白不搜索 | 模板按钮与 script | 2026-09-17 |
| S-L054-04 | `InboxPage.vue` | list/read；`subscribeInbox`；`inboxChanged`；`navigate`；不用 seen/readAll/hasPermission | script `load` / `markRead` / 生命周期 | 2026-09-17 |
| S-L054-05 | `ConfigPage.vue` | MAIL/SMS TAB；账号/场景/试发；短信无自由正文；不调用 `config.account` | 模板与 script；测试第二例 | 2026-09-17 |
| S-L054-06 | `NotificationPage.vue`；`monitor/index.ts` | 只 `deliveries`；`componentName` NotificationMonitor；无 snapshot | `load()`；re-export 名 | 2026-09-17 |
| S-L054-07 | `apps/admin-web/src/layout/components/notice/index.vue`；`utils/push.ts`；`adminManifestRegistry.test.ts` | 顶栏直接消费 inbox；事件名 `notify:inbox-updated`；错误键 `monitor/notify/index` | `read` / `readAll` / `initMessageBox`；registry 第 116–118 行 | 2026-09-17 |
| S-L054-08 | `domains/notify/src/transport.ts`；`notification/index.ts`；`callback/index.ts`；`web-domains/notify/AGENTS.md` | 厨房根上 snapshot/deliveries；inbox 四枪；应用 API 只有资源标签；AGENTS 漏 config | `createNotificationService` 返回对象；资源 `controller` 字段；AGENTS 第 3 行 | 2026-09-17 |
