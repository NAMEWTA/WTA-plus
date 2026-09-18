---
lesson_id: L-053
objective_ids: [OBJ-53]
claimed_cells:
  - A:createNotificationService
  - A:notificationDirectory.searchUsers
  - A:usersByIds
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: kitchen-http
    minutes: 10
  - segment: directory-port
    minutes: 8
  - segment: visuals-and-worked-examples
    minutes: 7
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 6
expression_level: eli5
coverage_depth: deep
source_ids: [S-005, S-006, S-010, S-L007-01, S-L053-01, S-L053-02, S-L053-03, S-L053-04, S-L053-05, S-L053-06, S-L053-07, S-L053-08]
---

# Lesson 053：宏观两张纸条——`createNotificationService` 与厅堂 `notificationDirectory` 怎么打到 `/notify/*`

## 学完你能做什么

打开厨房 `frontend/packages/domains/notify/src/transport.ts` 的 `createNotificationService`，再打开厅堂 `frontend/apps/admin-web/src/application/services.ts` 里的 `notificationDirectory`，你能**口述浏览器怎么把通知纸条打到 `/notify/*`**：发公告、收件箱、渠道配置、投递监控走冻住的厨房；搜人、按 ID 回填走厅堂自己写的目录对象。不是 `notificationService.searchUsers`，不是 `createNotificationDirectory`，也不是把 `POST /notify/notification` 画进这座厨房。

口试名单就是矩阵 **(a)** 这一行三格，符号以**磁盘**为准：

1. **`A:createNotificationService`**（包 `@namewta/domain-notify`，实现在 `src/transport.ts`）：工厂只认 `HttpClient`，返回 `Object.freeze` 的对象。上面只有五块：顶层 `snapshot` / `deliveries`，再加嵌套 `notices` / `inbox` / `config`。每一枪都是 `http.request({ url, method, params?, data? })`。**不**自己画 Vue，**不**读 `Admin-Token`，**没有** `recipients`，**没有** `submit` / `retry` / `cancel`。
2. **`A:notificationDirectory.searchUsers`**（厅堂对象字面量，不是工厂返回值）：`keyword.trim()` 有字才 `GET /notify/recipients/search`；空白关键字**本地** `{ rows: [], total: 0 }`，不发卡车。
3. **`A:usersByIds`**（同一个厅堂对象上的第二枪）：`GET /notify/recipients/by-ids`，`params.userIds` 是 `ids.join(',')`。**没有**空数组短路。空数组仍会出门，查询串是 `userIds=`。

OBJ-53 还要你能把「谁在喊」和「哪条 URL」对上，但**不**把四张 Vue 页再讲成 L-054：

- 厅堂单例 `notificationService = createNotificationService(domainHttp)`。
- 厅堂对象 `notificationDirectory` 三方法：`searchUsers` / `usersByIds` / `userTypes`。第三枪**不是** `/notify/*`，它转 `systemService.userTypes.options()` → `GET /system/userType/options`。
- `adminManifestRegistry.ts` 把 `{ service: notificationService, directory: notificationDirectory }` 交给 `createNotifyWebDomain`。公告页选人找 directory，发草稿找 service。
- 导航铃铛 `layout/components/notice/index.vue` 和 `utils/push.ts` **直接 import** 厅堂 `notificationService.inbox.*`。web-domain 页只收 runtime，厅堂铬（chrome）可以碰单例。

2026-09-17 工作树先钉死**包边界**（口试先数包，再数函数）：

| 你可能以为的名字 | 磁盘事实 |
| --- | --- |
| `createNotificationDirectory` / `notificationService.searchUsers` | **没有。** 目录是 App 对象字面量。厨房 freeze 对象没有 `recipients` |
| `@namewta/domain-notify/recipients` | **没有这个子路径。** `package.json` `exports` 是 `.` / `./monitor` / `./notification` / `./callback` / `./notice` / `./inbox` / `./config` |
| `import { createNotificationService } from '@namewta/domain-notify/monitor'` 会得到更小的监控厨房 | **同一只手。** `monitor/index.ts` 和 `notification/index.ts` 都 `export { createNotificationService } from '../transport'`，五块全在 |
| 厨房有 `POST /notify/notification` | **没有这四枪。** `./notification` 只贴资源标签 `NotificationController` + `basePath: '/notify/notification'`。submit/query/retry/cancel 是 L-051 的后端窗，管理端厨房不打 |
| 页面自己拼 `/notify/notice/list` | **厨房才拼。** Vue 喊 `runtime.service.notices.list(...)` |
| `home-web` 对称一份 | **没有。** home / sso 搜不到 `createNotificationService` / `notificationDirectory` |
| `@namewta/domain-notify` 依赖 `@namewta/domain-system` 才能选用户类型 | **包依赖表没有 system。** 交叉线画在厅堂：`userTypes` 转 `systemService` |
| `NotifyUserDirectory` 是 domain-notify 的类型 | **口形状在 web-domain** `runtime.ts`。domain 只借出 `NotifyUserCandidatePage` / `NotifyUserTypeOption` |
| 旧菜单键 `monitor/notify/index` | **厅堂测试锁死 `toBeUndefined()`。** 现键是 `notify/monitor/index`，厨房仍是本课工厂，不是 L-024 的 `createMonitorService` |

本课**不宣称**你会拆 `NotifyNoticeController` 六扇（L-045）、收件箱已见/已读（L-046）、配置密钥与试发五层（L-047）、收件人端口与 `pageSize` clamp（L-048）、监控 snapshot 写死 `includeContent=false`（L-049）、供应商回调验签（L-050）、统一通知 submit/query/retry/cancel（L-051）、Outbox 领取（L-052），或把 `createNotifyWebDomain` 四键页面剧本再讲一遍（L-054）。今天只认：**浏览器这一头的通知厨房 URL 表、厅堂目录两枪如何打到 `/notify/recipients/*`，以及厨房标签上写着、方法表里没有的那几扇门。**

## 先把宏观地图放在桌上

L-007 已经把插头插进厅堂：`export const notificationService = createNotificationService(domainHttp)`，旁边另写 `notificationDirectory`。L-008 对照过 home **没有**这两样。L-024 把系统监控四键钉死，并明确通知投递监控是另一座厨房。L-045 … L-051 是后端 `/notify/*` 的窗。本课站在**已经登录的管理员浏览器**这一头：口袋里是 `Admin-Token`，司机是 `adminHttp`。

三条河都叫 notify，货不一样：

| 名字听起来像 | 实际是什么 | 本课认不认 |
| --- | --- | --- |
| `createNotificationService` | 浏览器通知厨房：公告 / 收件箱 / 配置 / 监控查询 | **本课格子** |
| `notificationDirectory.searchUsers` / `usersByIds` | 厅堂选人端口的两枪 HTTP | **本课格子** |
| `notificationDirectory.userTypes` | 同一对象上的第三枪，URL 在 system | **邻居**：用来证明目录不是「notify 厨房漏写的抽屉」 |
| `NotifyUserDirectory` | web-domain 要求宿主提供的口 | **对照**；格子不标 web-domain 工厂 |
| `createNotifyWebDomain` | 管理端四键菜单 | L-054 |
| `NotifyNoticeController` 等 | 后端 layered 窗 | L-045 … L-051 |
| `createMonitorService` / `monitor/operlog/index` | system 操作日志监控 | L-024；**不要**和 `notify/monitor/index` 混楼 |
| 布局「消息盒子」`layout/.../notice` | 厅堂铃铛，import `notificationService.inbox` | **本课认它是厨房的第二张脸**；不是公告管理页 |

2026-09-17 工作树：权威厨房是 `frontend/packages/domains/notify/`（根 facade + `transport.ts` + 六个资源子目录）。权威厅堂接线是 `apps/admin-web/src/application/services.ts` 第 70–88 行。权威口形状是 `packages/web-domains/notify/src/runtime.ts` 的 `NotifyUserDirectory`。消费者在 `adminManifestRegistry.ts`、`NoticePage.vue` / `useRecipientSelection.ts`、`InboxPage.vue`、`ConfigPage.vue`、`NotificationPage.vue`、`layout/components/notice/index.vue`、`utils/push.ts`。菜单种子在 `50-cde-base-dml.sql`：`notify/monitor/index`、`notify/notice/index`、`notify/inbox/index`、`notify/config/index`。

```text
已登录的管理员（浏览器，Admin-Token）
        │
        ├─ 四张菜单页（createNotifyWebDomain，L-054）
        │     runtime.service = notificationService
        │     runtime.directory = notificationDirectory
        │
        └─ 厅堂铬：铃铛 + SSE/WS 推送
              直接 import notificationService.inbox
                        │
                        v
              ┌─────────────────────────────────────────┐
              │  createNotificationService(domainHttp)  │  ← 本课厨房
              │    snapshot / deliveries                │
              │    notices.* / inbox.* / config.*       │
              └─────────────────────────────────────────┘
                        │
              ┌─────────────────────────────────────────┐
              │  notificationDirectory（App 对象）       │  ← 本课目录
              │    searchUsers → /notify/recipients/search
              │    usersByIds  → /notify/recipients/by-ids
              │    userTypes   → /system/userType/options
              └─────────────────────────────────────────┘
                        │
                        v
              厅堂 domainHttp → adminHttp.request
              （Bearer 登录票；不是供应商回调签名头）
                        │
        /notify/notice/*     /notify/inbox*
        /notify/config/*     /notify/monitor/*
        /notify/recipients/*
        （没有 /notify/notification、没有 /notify/callback）
```

| 符号 | 磁盘 | 拥有什么 | 不拥有什么 |
| --- | --- | --- | --- |
| `createNotificationService` | `domains/notify/src/transport.ts` | 22 枪 URL / 动词 / 嵌套抽屉；`Object.freeze` | Vue、权限串、`Admin-Token` 抽屉、选人、submit |
| 资源标签 | `notice` / `inbox` / `monitor` / `notification` / `callback` 的 `index.ts` | `controller` 字符串 + `basePath` | HTTP 发送。`config/` **没有**资源对象 |
| `NotifyUserCandidate*` | `domains/notify/src/types.ts` | 选人最小视图类型 | 搜人方法 |
| `notificationService` | `apps/admin-web/.../services.ts` | 厅堂单例，把 `domainHttp` 塞进工厂 | 页面生命周期 |
| `notificationDirectory` | 同文件对象字面量 | search / by-ids / 转调 userTypes | 冻住的厨房字段 |
| `NotifyUserDirectory` | `web-domains/notify/src/runtime.ts` | 三方法签名；注释写明宿主组合目录 | 自己打 HTTP |
| `createNotifyWebDomain` | `web-domains/notify/src/index.ts` | 四键 + 权限目录 | 拼 URL |

**图题 / caption：** 通知前端宏观两张纸条。alt：菜单页和铃铛共用一座厨房；搜人走厅堂目录对象；用户类型拐去 system；厨房不打统一通知应用 API，也不打供应商回调。

**文字等价物：** 厅堂只造一个 `notificationService`。公告、收件箱、配置、监控查询都从这座工厂出门，前缀 `/notify/notice`、`/notify/inbox`、`/notify/config`、`/notify/monitor`。要找人，厅堂另写一张纸条 `notificationDirectory`：有关键字才搜，按 ID 回填总是打 `/notify/recipients/by-ids`，用户类型去隔壁 system 楼。卡车司机还是 `adminHttp`，口袋里是登录票。外面的短信/邮件供应商走 `/notify/callback/{channel}`，本课厨房看不见那扇门。业务模块交统一通知单走 `/notify/notification`，本课厨房也没有那四枪。

**类比：** 把 `createNotificationService` 想成邮局大厅的**业务表格打印机**。窗口上四本表格：公告草稿、你的信箱、公章与信纸、投递看板。`notificationDirectory` 不是第五本表格，而是大厅门口**厅堂自己雇的门卫名单夹**：要按名字找人、按工号回填已勾的人，门卫去通知楼的「收件人窗口」问；要问「有哪些用户类型」，门卫去隔壁行政楼（system）问，不把行政楼的钥匙焊进邮局厨房。资料页铃铛是大厅墙上的信箱灯，它直接按打印机上的「信箱」键，不绕菜单页。

**类比失效处：**

1. 门卫名单夹**不是**厨房漏装的抽屉。`@namewta/domain-notify` 的 `package.json` 依赖没有 `domain-system`，`transport.ts` 也没有 `recipients`。把搜人写进工厂，就会逼厨房 import 行政楼。
2. 空白关键字的空页是门卫**根本没出门**，不是邮局已经查过零个人。后端 L-048 对空白关键字也会空页，但那是另一扇窗的合同；本课短路发生在 App。
3. `usersByIds` **不**继承「空就不打」的规矩。`[].join(',')` 是空串，卡车仍开。真正跳过空名单的是选人 composable 的 `restore()`，不是目录对象。
4. 打印机打的是登录票卡车，不是供应商回调的 `X-Notify-Signature`。厨房源码里搜不到那颗头。
5. `./notification` 门牌写着 `NotificationController`，**不等于**打印机有 submit 键。那是给卡片柜看的标签，方法表以 `transport.ts` 为准。
6. 从 `./monitor` 进口不会得到「只含 snapshot/deliveries 的小厨房」。你会拿到同一台打印机。
7. 投递看板页今天只按 `deliveries`。厨房仍有 `snapshot`。墙上没挂的按钮 ≠ 刀被收了。
8. `recipientType=ALL` 时页面**不会**把全站用户拉进前端。规范就是：全员不走 search。

## 核心概念与机制

### 直觉讲解

小孩子版只记十二句：

1. **先找工厂，再找目录。** `export function createNotificationService(http: HttpClient)`。参数只有 `http`。返回值 `Object.freeze`。没有导出的 `NotificationService` 接口名；web-domain 写成 `ReturnType<typeof createNotificationService>`。
2. **五块，不是一个 `notify()`。** 监控两枪在顶层：`snapshot`、`deliveries`。其余三块嵌套：`notices`、`inbox`、`config`。
3. **读 GET，写 POST。** 删除公告、删账号也是 POST。厨房里没有 `method: 'delete'`。
4. **没有投影闸。** 内部 `request` 只是 `http.request` 再断言成 `{ data, code?, msg?, error? }`。不像 OpenAPI 那样验钞灯扔掉畸形 JSON。
5. **只有监控两枪用 `keyof paths`。** `snapshotPath` / `deliveriesPath` 从 `@namewta/api-contracts` 的 `paths` 取值。公告/收件箱/配置的 URL 是字符串字面量。不要口述成「每一枪都是 typed path」。
6. **选人不在厨房。** `searchUsers` / `usersByIds` 是厅堂对象。类型从 notify 借，口形状从 web-domain 借，HTTP 由 App 直打。
7. **空关键字短路，空 ID 不短路。** 两句话必须并排。
8. **`userTypes` 不是 `/notify/*`。** 它是 `systemService.userTypes.options()`。
9. **子路径 re-export 同一工厂。** `./monitor` 与 `./notification` 都能 import 工厂，得到的对象一样大。
10. **资源标签可以没有方法。** `notifyNotificationResource.basePath === '/notify/notification'`，工厂零枪打这条前缀。`notifyCallbackResource` 同理。`config/index.ts` 连资源对象都没有，只有类型。
11. **两张脸共用厨房。** 菜单页走 runtime；铃铛和推送刷新走厅堂单例。
12. **home 没有这台打印机。** 产品差在 App 插座板，不在工厂里 `delete` 方法。

**类比补一句：** 把 `transport.ts` 想成一台只会填表头的打印机，不会验钞。填错格子（比如把 `noticeId` 写成带斜杠的字符串）它仍会把路径打出去，因为本课厨房**没有** OpenAPI 那种 `userSegment` / `encodeURIComponent` 卡子。测试用的是雪花数字 ID。

### 精确定义与 English term

| 中文口头 | English term | 磁盘落点 |
| --- | --- | --- |
| 通知领域工厂 | `createNotificationService` | `frontend/packages/domains/notify/src/transport.ts`。入参 `HttpClient`，返回冻住对象 |
| 通知领域模块名片 | `notifyDomainModule` | `src/index.ts`：`id: 'notify'`，`backendModules: ['wta-notify']`，`capabilities: ['notification-control-plane', 'notification-monitor']` |
| 厅堂通知单例 | `notificationService` | `apps/admin-web/src/application/services.ts` 第 70 行 |
| 厅堂选人目录 | `notificationDirectory` | 同文件第 72–88 行。对象字面量，无工厂名 |
| 选人端口 | `NotifyUserDirectory` | `web-domains/notify/src/runtime.ts`：`searchUsers` / `usersByIds` / `userTypes` |
| 候选用户 | `NotifyUserCandidate` | `userId` / `userName` / `nickName` / `phoneNumber` / `status` |
| 候选页 | `NotifyUserCandidatePage` | `{ rows, total }` |
| 用户类型选项 | `NotifyUserTypeOption` | `userTypeId` / `userTypeName` / `status?`。HTTP 实际来自 system `UserTypeVO` |
| 通知渠道 | `NotificationChannel` | `'IN_APP' \| 'SMS' \| 'MAIL'`。公告和投递用 |
| 配置渠道 | `NotifyConfigChannel` | `'MAIL' \| 'SMS'`。**没有**站内信账号 |
| 发送对象类型 | `NotifyRecipientType` | `'ALL' \| 'USER' \| 'USER_TYPE'` |
| 资源标签 | resource metadata | 如 `notifyNoticeResource.controller = 'NotifyNoticeController'` |
| 兼容子路径 | package `exports` | 根 barrel 与 `./monitor`、`./notification` 都能拿到工厂 |
| 失败关闭运行时 | fail-closed runtime | web-domain 要 `service` + `directory`；本课工厂本身不校验缺方法 |
| 空关键字短路 | empty-keyword short-circuit | `searchUsers` 在 App 层 `Promise.resolve` 空页 |
| 统一通知应用 API | notification application API | `/notify/notification`；**本课厨房不打** |
| 供应商回调 | provider callback | `POST /notify/callback/{channel}`；**本课厨房不打** |

`capabilities` 是模块名片，**不是** componentKey，也不决定 App 挂哪几页。菜单键在 web-domain manifest 与 `50-cde-base-dml.sql`。

厨房子路径与 Controller 门牌：

| 子路径 | 资源标签 | 工厂有没有对应方法 | 本课页面/铬用不用 |
| --- | --- | --- | --- |
| `./notice` | `NotifyNoticeController` `/notify/notice` | **有。** `notices.*` 六枪 | 公告页用 list/get/save/publish/retract/remove |
| `./inbox` | `NotifyInboxController` `/notify/inbox` | **有。** `inbox.*` 四枪 | 收件箱页用 list/read；铃铛用 list/read/readAll。**未见 `seen` 的 Vue 调用** |
| `./config` | **无资源对象**，只有类型 | **有。** `config.*` 十枪 | 配置页用九枪；**未见 `config.account(id)` 的 Vue 调用** |
| `./monitor` | `NotificationMonitorController` `/notify/monitor` | **有。** 顶层 snapshot/deliveries | 监控页只用 `deliveries`。`snapshot` 在厨房里 |
| `./notification` | `NotificationController` `/notify/notification` | **无方法。** 只 re-export 同一工厂 | 无 Vue |
| `./callback` | `ProviderCallbackController` `/notify/callback` | **无方法。** 不 re-export 工厂 | 无 Vue；供应商服务器打 |

### 机制/因果链

#### 1. 厅堂怎么把钥匙塞进这座厨房

`frontend/apps/admin-web/src/application/services.ts`：先做懒电线 `domainHttp = { request: config => adminHttp.request(config) }`（L-007 的初始化环），再：

```ts
export const notificationService = createNotificationService(domainHttp);

export const notificationDirectory = {
  searchUsers: (keyword: string, page = 1, pageSize = 20) =>
    keyword.trim()
      ? domainHttp.request<{ data: NotifyUserCandidatePage }>({
          url: '/notify/recipients/search',
          method: 'get',
          params: { pageNum: page, pageSize, keyword: keyword.trim() }
        })
      : Promise.resolve({ data: { rows: [], total: 0 } }),
  usersByIds: (ids: readonly (string | number)[]) =>
    domainHttp.request<{ data: Array<{ userId: string | number; userName?: string; nickName?: string; phoneNumber?: string; status?: string }> }>({
      url: '/notify/recipients/by-ids',
      method: 'get',
      params: { userIds: ids.join(',') }
    }),
  userTypes: () => systemService.userTypes.options()
};
```

`adminHttp` 来自 `application/http.ts` 的 axios adapter，带 `getToken`（`Admin-Token`）和 `clientId`。厨房函数签名里没有 session。换 App 只换司机；home-web / sso-web **根本不雇**这座厨房，也没有目录对象。

`adminManifestRegistry.ts`：

```ts
const notifyManifest = createNotifyWebDomain({
  service: notificationService,
  directory: notificationDirectory,
  subscribeInbox: handler => { /* window 'notify:inbox-updated' */ },
  inboxChanged: () => { /* initMessageBox */ },
  hasPermission, navigate, dicts
});
```

`selectedManifestIds` 含 `'web-domain-notify'`。四键的页面剧本留给 L-054；本课只要能指着：**service 与 directory 是两根线，缺一根选人页会没有口。**

#### 2. 厨房 22 枪的 HTTP 形状

内部 `request` **不**做 fail-closed 投影。运输错误原样拒绝。路径里的 ID 用模板字符串拼接，**没有** `encodeURIComponent`。

**顶层监控（两枪）**

| 方法 | HTTP | 查询 |
| --- | --- | --- |
| `snapshot(notificationId)` | GET `/notify/monitor/snapshot` | `params.notificationId` |
| `deliveries(params = {})` | GET `/notify/monitor/deliveries` | `userId?` / `channel?` / `status?` |

`snapshotPath` / `deliveriesPath` 的类型是 `keyof paths`。监控页 `NotificationPage.vue`（组件名 `NotificationMonitor`，键 `notify/monitor/index`）目前只喊 `runtime.service.deliveries(query)`。

**`notices`（六枪）**

| 方法 | HTTP | body / params |
| --- | --- | --- |
| `list(params = {})` | GET `/notify/notice/list` | `NotifyNoticeQuery`：`pageNum?` `pageSize?` `noticeTitle?` `noticeType?` `status?` |
| `get(noticeId)` | GET `/notify/notice/${noticeId}` | 路径 ID |
| `save(data)` | POST `/notify/notice/save` | `Partial<NotifyNotice>` |
| `publish(noticeId)` | POST `/notify/notice/${noticeId}/publish` | 无 body |
| `retract(noticeId)` | POST `/notify/notice/${noticeId}/retract` | 无 body |
| `remove(noticeIds)` | POST `/notify/notice/remove` | `data` 是 **数组**，不是 `{ ids }` |

`transport.test.ts` 锁死：三类目标 `ALL` / `USER` / `USER_TYPE` 都走同一枪 `save`；`publish` 是**独立** POST。改草稿 ≠ 发信。发信发生在后端 publish 链（L-045），前端只负责按键。

**`inbox`（四枪）**

| 方法 | HTTP |
| --- | --- |
| `list()` | GET `/notify/inbox`（无 params；当前用户由后端登录态决定） |
| `seen(messageId)` | POST `/notify/inbox/${messageId}/seen` |
| `read(messageId)` | POST `/notify/inbox/${messageId}/read` |
| `readAll()` | POST `/notify/inbox/read-all` |

2026-09-17 的 Vue：**没有** `inbox.seen` 调用。收件箱页与铃铛走 `read` / `readAll` / `list`。权限种子里仍有 `notify:inbox:seen`，web-domain 权限目录也列了这串。厨房有枪，扳机在菜单按钮上还没人扣。

**`config`（十枪）**

| 方法 | HTTP | 备注 |
| --- | --- | --- |
| `accounts(channel, pageNum = 1, pageSize = 10)` | GET `/notify/config/account/list` | `params: { channel, pageNum, pageSize }`。`channel` 只能是 `MAIL` / `SMS` |
| `account(accountId)` | GET `/notify/config/account/${accountId}` | **厨房有，配置页不用。** 编辑对话框用列表行拷一份，并把密码/密钥字段写成 `''` |
| `addAccount(data)` | POST `/notify/config/account` | |
| `editAccount(data)` | POST `/notify/config/account/edit` | |
| `changeStatus(accountId, enabled)` | POST `/notify/config/account/changeStatus` | `data: { accountId, enabled }` |
| `removeAccount(accountId)` | POST `/notify/config/account/remove` | `data` 是 **标量 ID**，不是对象 |
| `scenes(channel)` | GET `/notify/config/scene/list` | `params: { channel }` |
| `saveScene(data)` | POST `/notify/config/scene/save` | |
| `testAccount({ accountId, sceneCode?, target })` | POST `/notify/config/test/account` | |
| `testTemplate({ sceneCode, channel, target })` | POST `/notify/config/test/template` | |

测试发送仍走后端 `NotificationApplicationService.submit`（L-047），前端只打 config 的 test 窗，**不会**自己再打 `/notify/notification`。

#### 3. 目录两枪如何打到 `/notify/recipients/*`

后端窗是 `NotifyRecipientController`（L-048）：权限不是 `notify:recipients:*`，而是公告起草权 `notify:notice:add` **或** `notify:notice:edit`。没有起草权就不能搜人。前端目录对象**不**自己查权限串；闸在页面 `hasPermission` 与后端注解。

**`searchUsers(keyword, page = 1, pageSize = 20)`**

1. `keyword.trim()` 为空 → `Promise.resolve({ data: { rows: [], total: 0 } })`。网络零次。
2. 否则 `GET /notify/recipients/search`，`params = { pageNum: page, pageSize, keyword: trimmed }`。

对照 OpenAPI 与 Java：`search` 的查询合同是 `keyword` + 可选 `pageSize`（默认 20）。Controller 方法签名**没有** `pageNum`。UseCase 把 `pageSize` clamp 到 1..50，当 **limit** 用，`total = rows.size()`。前端仍传 `pageNum`，Spring 会忽略多余 query。选人 UI 在 `total > pageSize` 时才渲染分页；按后端当前实现，`total` 不会大于这次返回的行数，分页条通常不会出现。口试要能分开三层：

| 层 | 对 page 做什么 |
| --- | --- |
| 选人 composable | 调用 `searchUsers(value, nextPage, 20)`，防抖 300ms，`generation` 丢弃过期响应，结果再滤 `status === '0'` |
| 厅堂 `searchUsers` | 把 `page` 放进 `params.pageNum` 发出去 |
| 后端 search | 不读 pageNum；按关键字取最多 `pageSize` 个活跃用户 |

不要把「前端有 page 参数」说成「收件人搜索是真分页」。真分页合同在 L-048 的后端课；本课只保证你能指着厅堂发出的那条 URL。

**`usersByIds(ids)`**

- 永远 `GET /notify/recipients/by-ids?userIds=` + `ids.join(',')`。
- 空数组 → `userIds=` 空串。后端空白 `userIds` 返回空列表，不是 400。
- 非法编号（非 Long）由后端变成 `R.fail("用户编号格式错误")`。前端目录**不**先校验。
- 选人 `restore()`：`if (!ids.value.length) return;` 之后才喊 `directory.usersByIds`。所以「空名单不打 by-ids」是 **UI 层**，不是 `A:usersByIds` 自己的短路。

**`userTypes()`（邻居，不是本课格子）**

- `systemService.userTypes.options()` → `GET /system/userType/options`。
- 公告页在 `recipientType === 'USER_TYPE'` 且对话框打开时才拉；再滤 `status === '0'`。
- 这是 L-007 已经画过的虚线：notify 包不依赖 system 包。

#### 4. 谁在喊：runtime 脸和铬脸

```text
NoticePage
  runtime.service.notices.list/get/save/publish/retract/remove
  runtime.directory.userTypes          （USER_TYPE）
  RecipientUserPicker → useRecipientSelection
        directory.searchUsers / usersByIds

InboxPage
  runtime.service.inbox.list / read
  runtime.subscribeInbox / inboxChanged / navigate

ConfigPage
  runtime.service.config.accounts/add/edit/changeStatus/remove
                      scenes/saveScene
                      testAccount/testTemplate

NotificationPage（投递监控，不是 /notify/notification）
  runtime.service.deliveries

layout/notice + utils/push.ts
  notificationService.inbox.list / read / readAll
  push 刷新派发 'notify:inbox-updated'，收件箱页 subscribeInbox 再 list
```

web-domain 页 **不** import `@/application/services`。铃铛 **会**。口试若把「页面永不碰单例」说成全仓定律，会被铃铛文件打脸。准确说法：菜单页走 runtime 注入；厅堂铬允许碰单例。

#### 5. 子路径 re-export 与「假抽屉」

厅堂实际 import：

```ts
import { createNotificationService } from '@namewta/domain-notify';
import type { NotifyUserCandidatePage } from '@namewta/domain-notify';
```

走根 barrel。你也可以从 `@namewta/domain-notify/monitor` 或 `.../notification` 拿到**同一个** `createNotificationService`。这和 `createMonitorService` 必须走 `@namewta/domain-system/monitor`、根 barrel 找不到——姿势相反。

`./notification` 同时导出 `notifyNotificationResource`。看见 `basePath: '/notify/notification'` 不要补脑出 `service.submit`。生成合同 `openapi.ts` 里确实有 submit/query/retry/cancel 四条 path；**浏览器厨房没有对应方法**。调用方是其它后端模块（Java `NotificationApplicationService`），不是 admin-web 这台打印机。

### 图、表或文本图

```text
NoticePage / InboxPage / ConfigPage / NotificationPage
        runtime.service                    runtime.directory
                 \                            /   searchUsers
                  \                          /    usersByIds
                   v                        v     userTypes ──► systemService
            createNotificationService            notificationDirectory
                   │                                  │
                   │  无 recipients                    │  无 notices.save
                   v                                  v
            /notify/notice|inbox|config|monitor     /notify/recipients/*
                   │
                   x  不打 /notify/notification
                   x  不打 /notify/callback/{channel}

layout/notice ──► notificationService.inbox.* ──► 同一厨房
utils/push.ts ──► notificationService.inbox.list
```

**图题 / caption：** 两张纸条、两张脸、一条登录票卡车。alt：菜单页 runtime 同时拿 service 与 directory；铃铛只拿 inbox；recipients 不在工厂里；application API 与 callback 不在本课卡车路线上。

**文字等价物：** 人先碰到菜单或铃铛。菜单要发公告就喊厨房 `notices`，要勾人就喊目录 `searchUsers` / `usersByIds`。铃铛只喊 `inbox`。厨房决定除收件人以外的 `/notify/*` URL。目录决定收件人两枪，并把用户类型转到 system。统一通知柜台和供应商门铃不在这张浏览图表上。

## 正例、反例与边界

**正例 1 — 保存指定用户草稿再发布。** 有 `notify:notice:add` 的人打开 `notify/notice/index`，选 `recipientType=USER`，在选人框输入 `admin`。300ms 后 `searchUsers('admin', 1, 20)` 打 `GET /notify/recipients/search?pageNum=1&pageSize=20&keyword=admin`。勾人、填标题、渠道含 `IN_APP`。`notices.save` POST `/notify/notice/save`。列表里再 `publish`，独立 POST `/notify/notice/{id}/publish`。测试锁死：三类目标都只增加 save 次数，publish 另算一枪。

**正例 2 — 空白关键字。** 选人框清空或只打空格。`searchUsers` 不发 HTTP，本地空页。composable 里 `if (!value) return`，按钮 `:disabled="!keyword.trim()"`。不要说「后端返回了零行」。

**正例 3 — 编辑草稿回填已选用户。** 对话框打开时 `restore()` 看到 `recipientIds` 非空，喊 `usersByIds(['1761...', '1761...'])` → `GET /notify/recipients/by-ids?userIds=1761...,1761...`。缓存进 Map，翻页或换关键词不会丢已选。`join` 用逗号，和后端 `split(",")` 对齐。

**正例 4 — 铃铛与收件箱共用 list。** 推送 `initMessageBox` 喊 `notificationService.inbox.list()`，把 `readTime` 映射成铃铛的 `read`。点一条未读再 `inbox.read(messageId)`。收件箱页同一枪 `runtime.service.inbox.list`。两张脸，一座厨房，一条 `GET /notify/inbox`。

**正例 5 — 短信场景试发。** 配置页 `testTemplate({ sceneCode, channel: 'SMS', target })` POST `/notify/config/test/template`。前端**不会**再打 `/notify/notification`。是否 SYNC、是否进 Outbox，是 L-047 / L-051 / L-052。

**反例 1 — `notificationService.searchUsers`。** freeze 对象没有这个键。TypeScript 也会拦。选人必须走 `directory`。

**反例 2 — 发明 `createNotificationDirectory`。** 磁盘无此工厂。L-007 反例名单里已经写过。本课再钉一次：目录是 App 字面量。

**反例 3 — 从 `./monitor` 进口以求「监控专用小厨房」。** 你会连 `notices.save` 一起拿到。真要子集，得新写工厂，不能靠子路径。

**反例 4 — 页面手写 `/notify/notice/list`。** 违反 L-004 方向：`App → web-domain → domain → platform`。URL 只许出现在 `transport.ts` 或厅堂目录对象。

**反例 5 — 在 `@namewta/domain-notify` 里 import `createSystemService`。** 包依赖表没有这条边。用户类型交叉必须留在 `services.ts`。

**反例 6 — 把 submit 画进本课厨房。** `NotificationController` 的四扇窗存在于 Java 与 `openapi.ts`。管理端厨房 22 枪对不上其中任何一枪。业务模块交单走后端 API，不走 admin-web。

**反例 7 — 在 home-web 的 `services.ts` 加 `createNotificationService`，却不改 package.json 与 registry。** 三点组合门会裂开（L-008）。今天 home 没有通知铃产品差。

**反例 8 — 把 `userTypes` 说成 `GET /notify/recipients/types`。** 没有这条 path。真实 URL 是 `/system/userType/options`。

**边界 1 — `searchUsers` 的短路不推广到 `usersByIds`。** 口试要能同时说出两句。

**边界 2 — `pageNum` 会发出去，后端 search 当没看见。** 不要把 OpenAPI 没有的参数教成「后端分页合同」。也不要为了上课去改产品。

**边界 3 — 厨房有、UI 不用。** `snapshot`、`inbox.seen`、`config.account(id)`。口试要能指文件，并说目前谁没扣扳机。

**边界 4 — `ALL` 不搜人。** 公告页对 ALL 只显示提示文案。search 接口也没有「无关键字列出全部用户」。空白关键字被故意设计成空结果。

**边界 5 — 配置渠道没有 `IN_APP`。** 站内信不是 SMTP/短信账号。`NotifyConfigChannel` 与 `NotificationChannel` 差一个值。

**边界 6 — 旧键 `monitor/notify/index`。** registry 测试对 system 域解析为 `undefined`；新键在 notify 域。不要把通知监控说成 L-024 的第四块屏。

**边界 7 — 资源标签 `controller` 字符串不是 Java 类加载器。** 例如标签存在不等于工厂有方法。`config/` 连标签都没有，方法却在 `transport.ts`。

**边界 8 — 铃铛 import 单例 ≠ 允许 Vue 业务页跨过 web-domain 打 HTTP。** 铬是 App 自己的壳。新菜单页仍应只收 `NotifyWebRuntime`。

## 变式与迁移

1. **和 `createOpenApiService` 对照。** OpenAPI 有两扇柜台、`NO_STORE`、fail-closed 投影、路径段正则。通知厨房是扁平填表：无投影、无禁缓存头、ID 不编码。不要把「domain 工厂」说成同一种严密度。
2. **和 `createProfileService` 对照。** 档案厨房是九张资源纸条订成一本 facade，子路径各有 `createXxxService`。通知厨房是**一份** `transport.ts` 写完全部 URL。`./notice` 并不导出自己的 `createNoticeService`。
3. **和 `createMonitorService` 对照。** 系统监控必须走子路径，根 barrel 不导出。通知工厂**根与子路径都能 import**，且子路径不会裁方法。通知监控键是 `notify/monitor/index`，Java 是 `NotificationMonitorController`，不要复用 `operationLogs`。
4. **和 L-007 对照。** 组合点课只要求你能指着「工厂一行 + 目录对象三方法」。本课要求能把 22 枪和收件人两枪的 URL 背到 method。不要把 L-007 的接线板图当成已经 covered 的方法表。
5. **和 L-048 对照。** 后端空关键字空页、`pageSize` clamp 1..50、非法 ID 失败文案、端口隔离 `UserService`，本课不重讲。前端保证：空白不发 search、by-ids 用逗号、类型从 notify 借、口形状在 web-domain。
6. **和 L-051 对照。** 统一通知应用 API 是给模块和测试发送链用的。管理端发布公告走 `notices.publish`，由后端 publisher 去 submit。前端不直接 retry/cancel。
7. **换 App。** 今天只有 admin-web 接线。以后若门户要铃铛：复制的是「`createNotificationService(该厅堂 http)` + 如需选人再写 directory」，不是 import admin 的 `services.ts`（会串 `Admin-Token`）。
8. **要加一条新的 `/notify/*`。** 先改 `transport.ts` 与 `transport.test.ts`。若这条需要 system 用户合同，优先做成厅堂端口（像 directory），不要让 domain-notify 依赖 domain-system。若这条是供应商回调，不要做浏览器厨房方法。
9. **详情要不要 `config.account`。** 当前 UI 信任列表行，编辑时清空密钥输入。若以后列表改成摘要、详情才含 host/port，再把对话框改成 GET by id。现在改等于重复请求。
10. **监控看板要不要 `snapshot`。** 当前页是投递流水。若以后要「这一票件总状态」，厨房已经有 `snapshot(notificationId)`，不必手写 URL。

## 常见误区

1. **「`notificationDirectory` 是 `createNotificationService` 返回的。」** 返回值导出成 `notificationService`。目录是旁边的 App 对象。
2. **「domain-notify 有 recipients transport，厅堂只是转发。」** 没有 recipients 模块。厅堂直打 HTTP，只借类型。
3. **「空关键字还会打 search。」** `trim()` 后空则本地空页。
4. **「`usersByIds([])` 也会短路。」** 不会。短路在 picker 的 `restore`，不在目录方法。
5. **「`userTypes` 也是 `/notify/recipients`。」** 是 `/system/userType/options`。
6. **「`NotificationPage.vue` 就是 `/notify/notification`。」** 文件名是监控页。componentName `NotificationMonitor`，键 `notify/monitor/index`，方法 `deliveries`。
7. **「布局 `notice/index.vue` 就是公告管理。」** 那是消息盒子。公告管理是 `web-domains/notify/src/NoticePage.vue`。
8. **「从 `./monitor` 进口只含监控。」** 同一工厂，五块都在。
9. **「看见 `notifyNotificationResource` 就有 submit。」** 标签不是方法表。
10. **「前端会给回调签名。」** 不会。`X-Notify-Signature` 是供应商服务器的事（L-050）。
11. **「home-web 也有通知厨房。」** 本课核对：无工厂、无单例、无目录。
12. **「`capabilities` 有 `notification-control-plane` 所以厨房含 application API。」** 名片不是 URL 表。控制面 HTTP 在后端 `NotificationController`；浏览器厨房没接。
13. **「删除用 DELETE。」** POST `/notify/notice/remove`、POST `/notify/config/account/remove`。
14. **「`removeAccount` 的 body 是 `{ accountId }`。」** 磁盘是标量 `data: accountId`。`notices.remove` 才是数组。
15. **「本课覆盖四张页面的按钮剧本。」** 那是 L-054。本课认 URL 与谁注入谁。
16. **「搜人权限是 `notify:recipients:query`。」** 没有这串。后端是公告 add/edit 的 OR。
17. **「`createMonitorService.deliveries`。」** 系统监控工厂没有这枪。通知投递在 `createNotificationService.deliveries`。
18. **「`inbox.list` 要传当前 `userId`。」** 厨房无此参数。后端用 `LoginHelper.getUserId()`。

## 非评分暂停

打开磁盘，不要凭记忆默写路径。不要改文件。没有标准答案栏，没有分数。

1. 打开 `frontend/packages/domains/notify/src/transport.ts`。圈 `Object.freeze`、五块名字、`snapshotPath`/`deliveriesPath` 的 `keyof paths`。顺着 `notices` / `inbox` / `config` 把 URL 和 method 点完。圈 `remove` 的数组 body、`removeAccount` 的标量 body、`changeStatus` 的对象 body。确认文件里**没有** `recipients`、**没有** `/notify/notification`、**没有** `callback`。
2. 打开同包 `package.json` `exports`、`src/index.ts`、`src/monitor/index.ts`、`src/notification/index.ts`、`src/callback/index.ts`、`src/config/index.ts`。圈：谁 re-export 工厂，谁只有资源标签，谁连标签都没有。圈 `notifyDomainModule.capabilities`。
3. 打开 `src/transport.test.ts`。圈三类 `recipientType` 都打 `/notify/notice/save`、publish 独立、deliveries 保留筛选参数、config 的 MAIL/SMS 账号与 `testTemplate`。
4. 打开 `apps/admin-web/src/application/services.ts` 第 5、13、70–88 行。圈工厂从 `@namewta/domain-notify` 进，导出 `notificationService`；`notificationDirectory` 是对象字面量；`searchUsers` 的三元短路；`usersByIds` 的 `join(',')`；`userTypes` 转 `systemService`。确认 **没有** 从工厂返回值上取 search。
5. 打开 `web-domains/notify/src/runtime.ts` 与 `adminManifestRegistry.ts` 的 `createNotifyWebDomain({ service, directory })`。圈 `NotifyUserDirectory` 三方法签名。打开 `notice/useRecipientSelection.ts`，圈空关键字不请求、`searchUsers(..., 20)`、`restore` 对空 ids return、`status === '0'` 过滤发生在 composable 不是目录。
6. 打开 `layout/components/notice/index.vue` 与 `utils/push.ts`。圈它们 import 的是 `@/application/services` 的 `notificationService`，方法是 `inbox.list` / `read` / `readAll`。打开 `NotificationPage.vue` 确认只喊 `deliveries`。在 `frontend/apps/home-web` 搜 `createNotificationService`，确认无匹配。

## 总结、词汇表与下一步

- **宏观两张纸条：** 厨房 `createNotificationService` 打公告/收件箱/配置/监控。厅堂 `notificationDirectory` 打收件人搜索与按 ID 回填。用户类型拐去 system。不是同一只返回值上的三个字段。
- **(a) `createNotificationService`：** 只认 `HttpClient`，冻住 22 枪，前缀 `/notify/notice|inbox|config|monitor`。无 Vue、无选人、无 submit、无回调、无投影闸。`./monitor` 与 `./notification` 再 export 仍是这一只。
- **(a) `notificationDirectory.searchUsers`：** 有字才 `GET /notify/recipients/search`。空白本地空页。默认 `page=1, pageSize=20`。前端会带 `pageNum`；后端 search 当前当 limit 用。
- **(a) `usersByIds`：** `GET /notify/recipients/by-ids?userIds=` + 逗号串。空数组不短路。非法 ID 由后端失败，不由目录预检。
- **两张脸：** 菜单页经 runtime；铃铛/推送经厅堂单例 `inbox`。
- **备用枪不是 UI 枪：** `snapshot`、`inbox.seen`、`config.account`。标签不是方法：`/notify/notification`、`/notify/callback`。
- **不是系统监控，不是 home 插座板。**

词汇表：`createNotificationService` / `notificationService` / `notificationDirectory` / `NotifyUserDirectory` / `NotifyUserCandidatePage` / `NotifyUserTypeOption` / `NotificationChannel` / `NotifyConfigChannel` / `NotifyRecipientType` / resource metadata / empty-keyword short-circuit / `notifyDomainModule` / `web-domain-notify` / `Admin-Token`。

下一步：公告六扇与发布链是 OBJ-45（child L-001）。收件箱已见/已读是 OBJ-46。配置账号/场景/试发是 OBJ-47。收件人端口与 clamp 是 OBJ-48。监控 snapshot/deliveries 五层是 OBJ-49。回调验签是 OBJ-50。统一通知 submit/query/retry/cancel 是 OBJ-51。Outbox 是 OBJ-52。四张菜单页怎么消费 runtime 是 OBJ-54。厅堂插头是 OBJ-07。本课结束仍不是掌握证明；要练习请之后主动激活 Homework。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-005 | `frontend/apps/{admin-web,home-web,sso-web}` | 工厂单例与目录只在 admin-web `services.ts`；home/sso 无匹配 | `application/services.ts`；home/sso 无 `createNotificationService` | 2026-09-17 |
| S-006 | `frontend/packages/{domains,web-domains}/notify` | 厨房、子路径 exports、web-domain 口形状与四键 | 各包 `package.json` 与 `src` | 2026-09-17 |
| S-010 | `release-artifacts/docker/infrastructure/mysql/init/50-cde-base-dml.sql` | 菜单键 `notify/{monitor,notice,inbox,config}/index`；`notify:inbox:seen` 种子；旧键 `monitor/notify/index` 被排除 | 通知菜单行 | 2026-09-17 |
| S-L007-01 | `apps/admin-web/src/application/services.ts` | 工厂调用、目录字面量、空关键字短路、`userTypes` 转 system | 第 5、70–88 行 | 2026-09-17 |
| S-L053-01 | `packages/domains/notify/src/transport.ts` | 22 枪 URL/动词/body 形状；freeze；无 recipients；监控 path 用 `keyof paths` | `createNotificationService` 全文 | 2026-09-17 |
| S-L053-02 | `src/transport.test.ts`；`src/index.ts`；`package.json` `exports`；`src/{monitor,notification,notice,inbox,config,callback}/index.ts` | 三类目标 save+独立 publish；子路径谁 export 工厂；资源标签；capabilities | 三个 `it`；各 `index.ts` | 2026-09-17 |
| S-L053-03 | `src/types.ts` | Candidate / Channel / ConfigChannel / RecipientType / Inbox / Scene 类型 | 类型声明 | 2026-09-17 |
| S-L053-04 | `web-domains/notify/src/runtime.ts`；`src/index.ts`；`src/index.test.ts` | `NotifyUserDirectory`；四键顺序；权限目录含 seen | 接口与 manifest 测试 | 2026-09-17 |
| S-L053-05 | `notice/useRecipientSelection.ts` 与 `.test.ts`；`RecipientUserPicker.vue`；`NoticePage.vue` | 防抖、空关键字、restore、status 过滤、`directory.userTypes`、`notices.*` | composable 五个 `it`；页面 script | 2026-09-17 |
| S-L053-06 | `InboxPage.vue`；`ConfigPage.vue`；`NotificationPage.vue`；`layout/components/notice/index.vue`；`utils/push.ts` | 实际扣的扳机：list/read/readAll、config 九枪、仅 deliveries、铃铛 import 单例 | 各文件的 service 调用 | 2026-09-17 |
| S-L053-07 | `apps/admin-web/src/router/adminManifestRegistry.ts` 与 `.test.ts` | `{ service, directory }` 注入；`notify/monitor/index` 可解析；`monitor/notify/index` undefined | `createNotifyWebDomain`；resolve 两个 `it` | 2026-09-17 |
| S-L053-08 | `NotifyRecipientController.java`；`NotifyRecipientUseCase.java`；`generated/openapi.ts` 的 `search`/`byIds`；`domains/system/src/service.ts` `userTypes.options` | 后端 search 无 pageNum、空白空页、by-ids 逗号拆 Long；OpenAPI 无 pageNum；userTypes URL | Controller 两方法；openapi `operations.search`；`/system/userType/options` | 2026-09-17 |
