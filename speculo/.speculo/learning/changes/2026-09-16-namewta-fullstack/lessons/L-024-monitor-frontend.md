---
lesson_id: L-024
objective_ids: [OBJ-24]
claimed_cells: [A:createMonitorService, A:system web-domain monitor pages]
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: kitchen-http
    minutes: 10
  - segment: pages-and-manifest
    minutes: 9
  - segment: visuals-and-worked-examples
    minutes: 8
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 4
expression_level: eli5
coverage_depth: deep
source_ids: [S-005, S-006, S-010, S-L024-01, S-L024-02, S-L024-03, S-L024-04, S-L024-05, S-L024-06, S-L024-07]
---

# Lesson 024：监控室的抽屉和四块屏——`createMonitorService` 与 web-domain 监控页

## 学完你能做什么

打开厨房 `frontend/packages/domains/system/src/monitor/index.ts` 的 `createMonitorService`，再打开菜谱板 `frontend/packages/web-domains/system/src/monitor/index.ts` 的 `createMonitorWebDomain`，你能**口述监控室怎么发 HTTP、四块屏怎么挂上 runtime**，而不是把「监控」说成 `systemService` 的一个字段、或把 iframe 运维入口说成这四页。

本课认两格：

1. **`A:createMonitorService`**：子路径工厂 `@namewta/domain-system/monitor`。冻住四个资源口（`cache` / `loginInfo` / `online` / `operationLogs`）外加两把**不发 HTTP** 的安全尺（`externalIntent` / `attachmentIntent`）。它**不** `addRoute`，**不**读 `Admin-Token`，**不**自己画 ECharts。
2. **`A:system web-domain monitor pages`**：同一 npm 包 `@namewta/web-domain-system` 里的**第二块板** `createMonitorWebDomain`。磁盘上正好 **4** 条 `componentKey`，对应四份 Vue。manifest id 是 `web-domain-system-monitor`，`domainId` 仍是 `'system'`。

2026-09-16 工作树先钉死**包边界**（口试先数包，再数函数）：

| 你可能以为的包名 | 磁盘事实 |
| --- | --- |
| `@namewta/domain-monitor` | **没有这个包。** 厨房在 `@namewta/domain-system`，入口是 `package.json` 的 `"./monitor"` → `src/monitor/index.ts` |
| `@namewta/web-domain-monitor` | **没有这个包。** 四页在 `@namewta/web-domain-system` 的 `src/monitor/`。根 `src/index.ts` 再导出工厂 |
| `createSystemService.monitor` | **没有这个字段。** 厅堂 `services.ts` 并列调用 `createMonitorService(domainHttp)` |
| 根 `createSystemService` 再导出监控 | **没有。** `domains/system/src/index.ts` 不 re-export `createMonitorService`；必须走子路径 |

本课**不讲**四份 Java 窗怎么查 Redis / 踢 token（L-023）、通知监控 `/notify/monitor`（L-049 / L-005-notify-monitor）、`wta-extend` 里 monitor-admin / snailjob / snailai 进程内部、OpenAPI 工作区（L-033）。今天只认：**监控厨房的纸条、四块屏怎么吃 runtime、以及隔壁那扇 iframe 门为什么不是这四页。**

## 先把宏观地图放在桌上

L-007 已经把插头插进厅堂：`import { createMonitorService } from '@namewta/domain-system/monitor'`，导出 `monitorService`。L-019 把 `createOpenApiService` / `createMonitorService` 标成**另一座工厂**。L-020 把监控四键从 system 的 15 键里踢出去，留给本课。L-023 才是后端四扇窗。本课站在**前端这一头**，把厨房和四块屏对上。

```text
sys_menu.component          种子四键：monitor/{online,cache,operlog,logininfo}/index
        │
        v
GET /system/menu/getRouters  已按人+Client 裁剪（L-020）
        │
        v
createMonitorWebDomain       菜谱板 id=web-domain-system-monitor
  runtimeView 外包四页，注入 MonitorWebRuntime
        │  页面只认 runtime.service.*
        v
createMonitorService(http)   厨房，子路径工厂
        │  http.request({ url, method, params? })
        v
厅堂 domainHttp → adminHttp
        │
        ├─ GET    /monitor/cache
        ├─        /monitor/loginInfo/*
        ├─        /monitor/online/*
        └─        /monitor/operlog/*
```

旁边还有**第二张客单**，不要画进本课四页：

```text
sys_menu.component          monitor/{admin,snailjob,snailai,nacos}/index
        │
        v
admin-external-monitor      App 自有 manifest（不是 createMonitorWebDomain）
        │  views/monitor/external/index.vue
        v
monitorService.externalIntent(...)   只校验权限+URL，不发 /monitor/* HTTP
        │
        v
iframe 看别人家的楼（VITE_APP_*_ADMIN）
```

资料页那两枪也要先标位置：`apps/admin-web/src/views/system/user/profile/` 会直接 import 厅堂单例 `monitorService.online.current` / `removeCurrent`。那是**厅堂自己的镜子**，不是 web-domain 四页。口试能指文件，不要把「在线设备」说成 `OnlinePage.vue`。

## 核心概念与机制

### 直觉讲解

**类比：** 把系统管理想成餐厅。L-019 的 `createSystemService` 是后厨八个抽屉，做菜给人吃。监控室是隔壁一间**保安亭**：墙上四台显示器（在线谁坐着、Redis 喘不喘、谁登过、谁点过哪些按钮），抽屉里放着对讲机。服务员（页面）不自己爬进机房；她按显示器上的按钮，对讲机里已经印好要喊哪条线路。

小孩子版只记十二句：

1. **先找子路径，再找工厂。** `export function createMonitorService(http: HttpClient)`。参数只有 `http`。根包 `index.ts` 找不到这个名字。
2. **四个抽屉，不是一个 `monitor()`。** `cache` / `loginInfo` / `online` / `operationLogs`。最后一个英文名是 `operationLogs`，URL 却是 `/monitor/operlog`。
3. **一张纸条三个格子。** 监控枪几乎都是 `request({ url, method, params? })`。没有 `data` 体。`method` 小写。
4. **改东西的枪今天仍是 DELETE / 偶发 GET。** 清空日志 DELETE；解锁账号 GET。不要用 API-005「变更一律 POST」把厨房先改掉。
5. **路径里的 token / 用户名要盖印章。** 本地 `segment`：`encodeURIComponent` 再用逗号拼。测试用 `'token/value'`，路上变成 `token%2Fvalue`。
6. **操作日志会先换衣服再出门。** `operationLogs.list` 不是原样丢 OpenAPI 行；它跑 `projectOperationLogTransport`，缺字段补 0 / `''`，多余字段丢掉。
7. **两把尺不发卡车。** `externalIntent` / `attachmentIntent` 只检查「有没有权限」和「URL 安不安全」，返回一张 `NavigationIntent`。
8. **四块屏在另一份工厂。** `createMonitorWebDomain(runtime)` 登记四键，用 `runtimeView` 把 runtime 塞进 props。页面写 `runtime.service.online.list`，不 import `monitorService`。
9. **菜单键全小写 logininfo；HTTP 路径驼峰 loginInfo。** 权限串也是 `monitor:logininfo:*`。三套拼写不要混背。
10. **导出不走厨房方法。** 登录日志 / 操作日志点导出，走 `runtime.download('monitor/loginInfo/export' | 'monitor/operlog/export')`，厅堂 axios **POST** 下 blob。工厂里**没有** `export()`。
11. **在线用户那张表是「先整袋拿回来，再在浏览器切片」。** `OnlinePage` 的分页组件没有 `@pagination="getList"`。不要把它说成和登录日志一样的服务端分页。
12. **iframe 四键是厅堂自有菜谱。** `admin-external-monitor` 住在 `adminManifestRegistry.ts`，页在 `apps/admin-web/src/views/monitor/external/`。四块屏工厂里没有 `monitor/admin/index`。

**类比失效处：**

1. 保安亭**会动手**：强退、清日志、解锁，不是只看摄像头。
2. 显示器不是厨房抽屉的镜子。`systemDomainModule.capabilities` 里有 `monitor-cache` 等四个名字，那是领域模块能力清单，不是 `createSystemService` 的字段，也不是 4 条 `componentKey` 的同义词。
3. 对讲机（`HttpClient`）是厅堂雇的。换 App 只换司机；home-web **根本不雇**这间保安亭。
4. 走廊上四扇玻璃窗（iframe）看出去是别人的楼（`wta-extend` / Nacos）。本课四块屏看的是本餐厅 Redis 和日志表。

### 精确定义与 English term

| 中文 | English | 精确定义（本课，以工作树为准） |
| --- | --- | --- |
| 监控领域工厂 | `createMonitorService` | `frontend/packages/domains/system/src/monitor/index.ts` 导出函数。入参 `HttpClient`，返回 `Object.freeze` 的 `MonitorService` |
| 监控领域服务 | `MonitorService` | 同一文件接口。四个资源口 + `externalIntent` + `attachmentIntent` |
| 监控 Web 工厂 | `createMonitorWebDomain` | `frontend/packages/web-domains/system/src/monitor/index.ts`。入参 `MonitorWebRuntime`，返回冻住的 `WebDomainManifest` |
| 监控页运行时 | `MonitorWebRuntime` | `src/monitor/runtime.ts`：`service` / 反馈口 / `download` / `openDownload` / `dicts` / `hasPermission`。**没有**密码策略、tab 托盘、OSS |
| 组件键 | `componentKey` | 与 `sys_menu.component` 对齐的字符串。本板四条：`monitor/online/index`、`monitor/cache/index`、`monitor/operlog/index`、`monitor/logininfo/index` |
| 清单 id | `web-domain-system-monitor` | compose 时必须出现在 `selectedManifestIds`。漏选则四键变诊断牌 |
| 领域 id | `domainId: 'system'` | 监控板挂在 system 领域下。`inferDomainId('monitor/online/index')` 得到 `'monitor'`，查找仍只认键（L-020） |
| 运行时外包 | `runtimeView` | 懒加载页面后 `h(page, { runtime })`。页面 `defineProps<{ runtime: MonitorWebRuntime }>()` |
| 路径段编码 | `segment` | 单值或数组 → `encodeURIComponent(String(item))` → 逗号连接 |
| 传输投影 | `projectOperationLogTransport` | `monitor/transport.ts`：OpenAPI `SysOperLogVo` → 领域 `OperLogVO`。只留领域字段 |
| 导航意图 | `NavigationIntent` | `{ target, url, mode: 'embed' \| 'download', downloadName? }`。embed 给 iframe，download 给附件 |
| 外部监控目标 | `ExternalMonitorTarget` | `'monitor-admin' \| 'snail-job' \| 'snail-ai' \| 'nacos'` |
| 监控安全错误 | `MonitorSecurityError` | `code`: `permission-denied` / `unsafe-url` / `missing-url`。缺权限先抛，不把 URL 递出去 |
| 权限目录 | `manifest.permissions` | 四组 `monitor-${slice}`，动作用 `monitor:${slice}:${action}` 拼出。这是**目录**，不是 `v-hasPermi` 的数据源 |
| 外部权限表 | `monitorPermissions` | 只映射四个 iframe 目标。Nacos 是 `system:nacos:console`，其余 `monitor:admin:list` 等 |
| 资源锚点 | `systemMonitor*Resource` | 子目录 `cache/login-info/online/operlog/index.ts` 冻住 `{ controller, basePath }`，给合同对齐用，**不是** HTTP 客户端 |

### 机制/因果链

#### 1. 厅堂怎么把钥匙塞进监控室

`frontend/apps/admin-web/src/application/services.ts`：先做懒电线 `domainHttp = { request: config => adminHttp.request(config) }`（L-007 的初始化环），再 `export const monitorService = createMonitorService(domainHttp)`。home-web 的 `services.ts` **没有**这一行。

厨房自己：`const request = <T = unknown>(config: HttpRequest) => http.request<ApiResponse<T>>(config)`。整座服务和四个抽屉都 `Object.freeze`。测试可以替换 `http.request`，不能运行时给 `service.cache` 再挂方法。

web-domain **不 import 工厂**。`adminManifestRegistry.ts` 组 `adminMonitorWebRuntime`：`service: monitorService`，确认/成功/失败/loading 懒加载 `@/application/host/feedback`，`download` 转 `application/http.download`，`openDownload` 自己建 `<a download>`，`dicts` 走 `createLiveMonitorDictRefs` + 厅堂 `useDict`，`hasPermission` 走同一把 `createAdminAccessEvaluator`。然后 `const monitorManifest = createMonitorWebDomain(adminMonitorWebRuntime)`。

`composeAppRuntime` 的 `selectedManifestIds` 同时含 `'web-domain-system'` 和 `'web-domain-system-monitor'`。少后一个，`resolveAdminWebRegistration('monitor/online/index', 'system')` 会变成缺键。registry 测试锁死：online 键能解析成 `Online`；`monitor/notify/index` 是 `undefined`（通知监控键是 `notify/monitor/index`，另一块板）。

#### 2. 四个抽屉的 HTTP 形状

口试按抽屉，不默写测试里每一行。权威快照在 `domains/system/src/monitor/index.test.ts` 的 `binds the complete monitor surface`。下面补上那则用例没点名、但工厂里有的枪。

| 抽屉 | 前缀 | 方法（磁盘） | 后端窗（L-023 认，本课只对 URL） | 页面怎么吃 |
| --- | --- | --- | --- | --- |
| `cache` | `/monitor/cache` | `get()` → **GET** 根路径，无 params | `CacheController.getInfo` | `CachePage`：`runtime.service.cache.get` |
| `loginInfo` | `/monitor/loginInfo` | `list` GET `/list` + params；`delete` DELETE `/{ids}`；`unlock` **GET** `/unlock/{names}`；`clean` DELETE `/clean` | `SysLoginInfoController` | `LoginInfoPage`。导出**不在**此抽屉 |
| `online` | `/monitor/online` | `list` GET `/list` + params；`forceLogout` DELETE `/{tokenId}`；`current` GET 根路径；`removeCurrent` DELETE `/myself/{tokenId}` | `SysUserOnlineController` | `OnlinePage` 只用 `list` + `forceLogout`。`current` / `removeCurrent` 给资料页 |
| `operationLogs` | `/monitor/operlog` | `list` GET `/list` + params，**先投影再返回**；`delete` DELETE `/{ids}`；`clean` DELETE `/clean` | `SysOperlogController` | `OperationLogPage` 解构名是 `list` / `delOperlog` / `cleanOperlog`。导出不在此抽屉 |

必须能指的例外：

- **解锁是 GET。** 后端擦 Redis 错次键 `PWD_ERR_CNT_KEY + userName`。厨房没有把它改成 POST。
- **`current` 不要求 `monitor:online:list`。** Java 上 `GET /monitor/online` 无 `@SaCheckPermission`。这是「我自己的设备」，不是监控员名单。
- **`removeCurrent` 只踢当前登录者自己的 token。** 监控页强退走 `forceLogout`（权限 `monitor:online:forceLogout`）。两枪不要对调。
- **`operationLogs.list` 的空页合同：** `data` 缺 rows 时变成 `[]`；`total` 不是 number 就当 0；连 `data` 都没有（例如 `{ code: 204 }`）仍回 `{ rows: [], total: 0 }`，并保留其它元数据。其它三个 `list` **没有**这层衣服。
- **工厂没有 export / batchLogout。** 权限目录里有 `monitor:operlog:export`、`monitor:logininfo:export`、`monitor:online:batchLogout`。后一个在四页上**没有按钮**，后端也**没有**批量踢人窗。目录 ≠ 已接线的枪。

#### 3. 安全尺：`externalIntent` / `attachmentIntent`

两把尺共用 `requirePermission(allowed)` 和 `safeUrl(rawUrl)`。`allowed === false` 立刻 `MonitorSecurityError('permission-denied')`，测试断言中文 `/无权/`。缺 URL 是 `missing-url`（「未配置运维入口地址」）。

`safeUrl` 拒绝的形状（测试 `it.each` 锁死，不要背成「有 http 就行」）：

- 非 http(s) 协议：`javascript:`、`data:`
- 协议相对：`//evil.example/x`
- 带用户名：`https://user@evil.example/x`
- 空白、反斜杠、控制字符、残缺百分号（`%GG`、`%2`）
- 根相对却以 `//` 开头（会被当成协议相对）

接受并规范化：

- 根相对：用假 origin `https://monitor.invalid` 解析，**丢掉 `..`**。`/admin/../applications?view=all#health` → `/applications?view=all#health`
- 已编码空格保留：`/reports/report%20name` 仍是那串
- 绝对 http(s) + 合法 host（含 IPv6）→ `parsed.href`
- `attachmentIntent` 的 `target` 固定 `'notify-attachment'`，`mode: 'download'`。四块屏**不调用**它；当前工作树生产调用点在测试里。厅堂 runtime 仍准备了 `openDownload`

iframe 页 `views/monitor/external/index.vue` 才是 `externalIntent` 的产品入口：按 `target` 取 `VITE_APP_MONITOR_ADMIN` / `VITE_APP_SNAILJOB_ADMIN` / `VITE_APP_SNAILAI_ADMIN` / `VITE_APP_NACOS_ADMIN`，权限取 `monitorPermissions[target]`。Nacos 那串是 `system:nacos:console`，不是 `monitor:nacos:list`。

#### 4. 四块屏怎么挂、每页吃哪几口

`createMonitorWebDomain` 的 registrations 顺序（测试锁死，不要按菜单 orderNum 背）：

| 登记 id | `componentKey` | 组件名 | Vue 文件 |
| --- | --- | --- | --- |
| `system-monitor-online` | `monitor/online/index` | `Online` | `online/OnlinePage.vue` |
| `system-monitor-cache` | `monitor/cache/index` | `Cache` | `cache/CachePage.vue` |
| `system-monitor-operlog` | `monitor/operlog/index` | `Operlog` | `operlog/OperationLogPage.vue` |
| `system-monitor-logininfo` | `monitor/logininfo/index` | `LoginInfo` | `login-info/LoginInfoPage.vue` |

权限目录四组：

| slice | 动作（拼成 `monitor:${slice}:${action}`） | 四页实际用到的指令 |
| --- | --- | --- |
| `online` | list, query, **batchLogout**, forceLogout | 只有 `v-hasPermi="['monitor:online:forceLogout']"`。无批量钮 |
| `cache` | list | 页上**没有** `v-hasPermi`。进门靠菜单 `monitor:cache:list` |
| `operlog` | list, query, remove, export | 删/清 `remove`；导出 `export`；详情钮 `query` |
| `logininfo` | list, query, remove, unlock, export | 删/清 `remove`；解锁 `unlock`；导出 `export`。无详情对话框，`query` 只在目录里 |

页面局部状态：

- **CachePage：** 只读。`runtime.loading` / `closeLoading` 包住 `cache.get()`。用 echarts 画命令饼图和内存仪表。`onBeforeUnmount` 必须 `dispose`。不走表格 CRUD composable。
- **OnlinePage：** `list` 把 `rows` 整袋放进 `onlineList`，表格 `:data` 再 `slice` 出当前页。`pagination` 只绑 `pageNum` / `pageSize` / `total`，**没有** `@pagination="getList"`。换页不重新打 HTTP。强退先 `runtime.confirm`，再 `forceLogout(tokenId)`，成功文案磁盘写的是「删除成功」。
- **LoginInfoPage / OperationLogPage：** 共用 `monitor/composables.ts`：`useLoading`、`useSearchToggle`、`useDateRangeQuery`（把 `beginTime`/`endTime` 写进 `query.params`）、`useSearchReset`、`useTableSelection`、`useTableSortQuery`。列表走服务端分页：`@pagination="getList"`。
- **OperationInfoDialog：** 不发 HTTP。父页把当前行塞进去，JSON 用 `VueJsonPretty`。解析失败就原样显示字符串。

字典口：在线 / 登录日志要 `sys_device_type`（登录日志还要 `sys_common_status`）；操作日志再加 `sys_oper_type`。`createLiveMonitorDictRefs` 第一次是空数组，`useDict` 回来再填。不要把 `manifest.permissions` 说成字典来源。

子路径导出：web-domain 的 `package.json` 有 `./monitor/cache` 等四个页入口；**没有** `./monitor` 指向工厂。工厂从包根 `createMonitorWebDomain` 走。根 `src/pages.ts` **不含**监控页；监控页清单在 `src/monitor/pages.ts`。

### 图、表或文本图

**图 1：监控室两块板（厨房 HTTP + 四页菜谱）**

```text
                    ┌─ home-web ─────────────────────────────────┐
                    │  无 createMonitorService                   │
                    │  无 web-domain-system-monitor              │
                    └────────────────────────────────────────────┘

apps/admin-web/src/application/services.ts
        domainHttp (lazy)
              │
              v
   createMonitorService ──────────── MonitorService (freeze)
              │
    ┌─────────┼──────────┬────────────┬──────────────┐
    v         v          v            v              v
  cache    loginInfo   online   operationLogs   intents
  GET /    /loginInfo  /online   /operlog       (no HTTP)
  cache    list/del/   list/     list(+proj)/   externalIntent
           unlock/     force/    delete/clean   attachmentIntent
           clean       current/
                       myself

apps/admin-web/src/router/adminManifestRegistry.ts
   adminMonitorWebRuntime.service = monitorService
              │
              v
   createMonitorWebDomain ── id: web-domain-system-monitor
              │                 domainId: system
              │  4 componentKey  (测试锁死顺序)
              v
   composeAppRuntime.selectedManifestIds
     含 web-domain-system 与 web-domain-system-monitor
     另含 admin-external-monitor  ← 不是本板

getRouters.component == 四键之一
              │
              v
   runtimeView → Vue 页 props.runtime
              │
              ├─ CachePage      cache.get + echarts
              ├─ OnlinePage     online.list + forceLogout（本地切片分页）
              ├─ OperationLogPage operationLogs.* + runtime.download
              └─ LoginInfoPage  loginInfo.* + runtime.download
```

**alt：** 管理端把懒 HTTP 交给 `createMonitorService`，再把同一单例放进 `createMonitorWebDomain` 的 runtime；四条菜单键对上四份 Vue。home-web 整条链缺席。iframe 运维是并列的第三块板。

**caption：** 图 1——监控厨房与四页菜谱板的接线。司机在厅堂，菜单键在种子 SQL，页面不直接 import 工厂。

**文字等价物：** 图的上缘声明门户 App 不参与。中间左列是厨房：四个资源口打 `/monitor/*`，两把意图尺不打 HTTP。中间右列是厅堂 registry：把单例塞进 `MonitorWebRuntime`，工厂产出 id 为 `web-domain-system-monitor` 的 manifest，必须被 `selectedManifestIds` 选中。底列是客单键命中后，`runtimeView` 把 runtime 注入四页。导出走 `runtime.download` 的旁路，不经过厨房方法表。iframe 板画在选择集里，但用虚线标成「不是本课四页」。

**图的边界：** 不画四份 Java 的 Redis / Sa-Token 实现（L-023）。不画 `wta-extend` 进程怎么启动。不把 `/notify/monitor` 接到 `operationLogs`。不把 `capabilities` 四个 `monitor-*` 画成独立 npm 包。

**图 2：三套拼写（登录日志这一条最容易摔）**

| 层 | 登录日志 | 操作日志 | 在线 | 缓存 |
| --- | --- | --- | --- | --- |
| 菜单 `component` / 工厂 `componentKey` | `monitor/logininfo/index` | `monitor/operlog/index` | `monitor/online/index` | `monitor/cache/index` |
| HTTP 前缀 | `/monitor/loginInfo` | `/monitor/operlog` | `/monitor/online` | `/monitor/cache` |
| 权限串 | `monitor:logininfo:*` | `monitor:operlog:*` | `monitor:online:*` | `monitor:cache:list` |
| 厨房字段 | `loginInfo` | `operationLogs` | `online` | `cache` |
| Vue 目录 | `login-info/` | `operlog/` | `online/` | `cache/` |

**alt：** 同一资源在菜单键、HTTP、权限、工厂字段、目录名上的五种写法对照。

**caption：** 图 2——登录日志必须同时记住小写键、驼峰 URL、厨房字段 `loginInfo`。

**文字等价物：** 四条资源里，登录日志的菜单键和权限是全小写 `logininfo`，HTTP 与厨房字段是驼峰 `loginInfo`，文件夹是 kebab `login-info`。操作日志菜单/URL/权限都是 `operlog`，但厨房字段叫 `operationLogs`。缓存和在线三套比较齐。口试若把 `runtime.service.operlog` 说成磁盘符号，即为失败。

### 正例、反例与边界

**正例 1：强退一条在线会话。** 菜单键 `monitor/online/index` → `OnlinePage` → `runtime.confirm` → `runtime.service.online.forceLogout(tokenId)` → `DELETE /monitor/online/{encodeURIComponent(tokenId)}`。厅堂 axios 带 `Authorization` 与 `clientid`。权限指令看 `monitor:online:forceLogout`。不要走 `removeCurrent`。

**正例 2：操作日志列表投影。** 后端可能多给 `serverOnly` 这类生成字段。`projectOperationLogTransport` 丢掉它，并给缺的 `businessType` / `requestMethod` / `status` / `costTime` 补 0 或 `''`，`tenantId` 恒为 `''`。测试 `projects generated operation-log rows` 锁死。页面再拿投影后的 `OperLogVO` 填表。

**正例 3：导出走厅堂下载口。** `LoginInfoPage.handleExport` 调 `runtime.download('monitor/loginInfo/export', { ...queryParams }, fileName)`。`adminMonitorWebRuntime.download` 转 `downloadWithAxios`：对 url **POST**、`application/x-www-form-urlencoded`、`responseType: 'blob'`。后端窗是 `SysLoginInfoController.export`（`@PostMapping("/export")`）。厨房 `MonitorService` 上没有对应方法。

**正例 4：iframe 失败关闭。** `external/index.vue` 把 `hasPermission(monitorPermissions[target])` 的布尔值传给 `externalIntent`。没权限：抛错，页上 `el-alert`，**iframe 不出现**。没配 `VITE_APP_*`：`missing-url`。配了 `javascript:`：`unsafe-url`。

**正例 5：registry 把三块「监控」拆开。** `monitor/online/index` → `Online`（本课板）。`monitor/admin/index` → `MonitorAdmin`（`admin-external-monitor`）。`notify/monitor/index` → `NotificationMonitor`（notify web-domain）。`monitor/notify/index` 与 `monitor/report/index` 是 `undefined`。

**反例 1：** 「`createMonitorService` 从 `@namewta/domain-system` 根入口来。」根 `index.ts` 只导出 `createSystemService` / `createOpenApiService` 等。监控必须 `from '@namewta/domain-system/monitor'`。

**反例 2：** 「监控页在 `createSystemWebDomain` 的 15 键里。」L-020 已否。本课工厂才贴四键。

**反例 3：** 「`runtime.service.operlog.list`。」字段名是 `operationLogs`。

**反例 4：** 「在线用户换页会再打 `/monitor/online/list`。」`OnlinePage` 本地 `slice`。登录日志 / 操作日志才会每次分页重打。

**反例 5：** 「`monitor:online:batchLogout` 能批量踢人。」目录有这串，四页无按钮，工厂无方法，Java 无批量窗。

**反例 6：** 「`LoginInfoVO` 写了 `clientKey` / `deviceType`，所以类型文件就是表头。」`LoginInfoPage` 表格确实有客户端、设备类型列，但 `monitor/types.ts` 的 `LoginInfoVO` **没这两字段**。后端 `SysLoginInfoVo` 有。这是类型缺口，不要把 VO 接口背成「页上没有这些列」，也不要假装类型已经对齐。

**反例 7：** 「`monitor/types.ts` 里的 `NotifyQuery` 说明监控厨房打通知。」那是同文件里的残留通知形状，`createMonitorService` **不引用**它们。通知监控是 `@namewta/domain-notify` 的 `/notify/monitor`。

**反例 8：** 「资料页在线设备就是 `OnlinePage`。」资料页是 `views/system/user/profile/onlineDevice.vue`，直接 `import { monitorService } from '@/application/services'`，走 `current` / `removeCurrent`。web-domain 四页禁止这样 import 厅堂单例。

**反例 9：** 「capabilities 有 `monitor-online`，所以 `createSystemService` 能 list 在线用户。」`systemDomainModule.capabilities` 只是领域模块声明。HTTP 在子路径工厂。

**反例 10：** 「home-web 也能开缓存监控，只要菜单种子带这个键。」门户不选 `web-domain-system-monitor`，也不创建 `monitorService`。键会变成缺页诊断，不会偷偷 glob 到 Vue。

**边界：**

- 本格覆盖厨房方法表 + 四页 + 它们如何经 runtime 接线。
- `admin-external-monitor` 只作为**对照边界**出现，不把 iframe 页标成 claimed cell。
- 后端四份 Controller 的权限注解、锁、踢人语义以 L-023 为准；本课只要求 URL / 动词 / 哪一页调用能对上。
- 导出 POST、解锁 GET、强退 DELETE：与 API-005 目标不一致，属存量。本课不改合同。

## 变式与迁移

- **变式 A：给监控室加第五块屏。** 顺序硬：① 种子 `sys_menu.component` 写成合同键（不要写成 `../CachePage.vue`）；② 在 `createMonitorWebDomain` 的 registrations **和** 权限目录加一行；③ 若有新 HTTP，先在 `createMonitorService` 加抽屉或方法，并补 `index.test.ts` 的 surface 名单；④ 页面只吃 `MonitorWebRuntime`；⑤ admin 已选 `web-domain-system-monitor` 则不必改选择集。不要把新页写进 `createSystemWebDomain`，也不要丢进 `apps/admin-web/src/views/` 当可复用 CRUD。

- **变式 B：只要强退、不要 Redis 图。** 当前工厂**没有**子集参数，一次贴四键。真要子集：新写更窄的工厂，或接受四键都可解析、靠 `getRouters` 不发其它键。不要 compose 之后去 `delete` registrations。

- **变式 C：资料页「我的设备」。** 继续走厅堂单例 `monitorService.online.current` / `removeCurrent`。不要为了「也是在线」把 `OnlinePage` 嵌进资料页，也不要给 `removeCurrent` 加 `v-hasPermi="['monitor:online:forceLogout']"`——那是管理员踢别人的串。

- **变式 D：再开一扇看别人家的窗。** 那是 `admin-external-monitor` 登记表 + `VITE_APP_*` + `monitorPermissions` 新键。URL 必须能通过 `safeUrl`。不要把 iframe 登记进 `createMonitorWebDomain`。

- **变式 E：通知投递监控。** 键是 `notify/monitor/index`，厨房是 `createNotificationService`，Java 是 `NotificationMonitorController`。不要复用本课 `operationLogs` 或 `monitor/operlog/index`。

- **变式 F：换厅堂。** 复制的是「造 `MonitorWebRuntime` + 调 `createMonitorWebDomain` + 把 `web-domain-system-monitor` 放进**该厅堂** compose」，不是 import admin 的 `adminManifestRegistry`。home-web 今天没有这条变式的产品需求。

- **迁移口诀：** 子路径工厂 → 四抽屉 URL → 意图尺不发 HTTP → 第二块 web-domain 板四键 → App 选 manifest → 页面只认 runtime → 导出走 download 旁路 → iframe / 资料页 / 通知监控各走各的门。跳步会出现「菜单有监控点进去是告示牌」「页面 import 了 `monitorService`」「把 Nacos 写进 CachePage」。

## 常见误区

1. **「监控是 `systemService` 的字段。」** 并列工厂，子路径导出。
2. **「有独立的 `web-domain-monitor` 包。」** 没有。四页住在 `web-domain-system/src/monitor/`。
3. **「`createSystemWebDomain` 含监控键。」** 不含。第二工厂。
4. **「四个 capabilities = 四条 componentKey。」** capabilities 是 `monitor-cache` 这种能力名；键是 `monitor/cache/index`。
5. **「厨房字段叫 `operlog` / `logininfo`。」** 叫 `operationLogs` / `loginInfo`。
6. **「导出也是 `service.loginInfo.export`。」** 走 `runtime.download`，POST blob。
7. **「在线列表和登录日志一样服务端分页。」** 在线是整袋 + 本地切片。
8. **「`batchLogout` 一定有按钮。」** 目录有，接线无。
9. **「iframe 四页也是 web-domain-system-monitor。」** 它们是 `admin-external-monitor`。
10. **「`notify/monitor` 和 `monitor/operlog` 都是监控，可以共用厨房。」** 不同包、不同前缀、不同 Java。
11. **「页面可以 `import { monitorService } from '@/application/services'`。」** web-domain 四页只认 `runtime.service`。资料页那两份 Vue 是厅堂私有例外，不要抄回四页。
12. **「`externalIntent` 会帮你 fetch 运维控制台。」** 它只返回安全 URL。iframe 自己加载。
13. **「Nacos 权限是 `monitor:nacos:list`。」** 磁盘是 `system:nacos:console`。
14. **「`unlock` 既是 GET 又在工厂里改成了 POST。」** 仍是 GET。
15. **「类型文件里的 Notify* 说明本工厂管站内信。」** 残留形状，无调用点。
16. **「`closeLoading(handle)` 一定用得上 handle。」** 厅堂实现忽略参数，直接 `modal.closeLoading()`。页面仍应在 `finally` 里调用，合同在 runtime 口，不在回传值。

## 非评分暂停

打开磁盘，不要凭记忆默写 URL。不要改文件。没有标准答案栏、没有分数、没有掌握结论。

1. 打开 `frontend/packages/domains/system/package.json`。用手指点 `"./monitor"` 和四个 `./monitor/*` 子路径。再打开根 `src/index.ts`，确认搜不到 `createMonitorService`。
2. 打开 `frontend/packages/domains/system/src/monitor/index.ts`。点四个 freeze 抽屉和两把 Intent。圈出 `operationLogs.list` 里的 `projectOperationLogTransport`。圈出 `unlock` 的 `'get'`。
3. 打开同目录 `index.test.ts`。把 `complete monitor surface` 那 10 行 `METHOD url` 读一遍。再看 encoding 那则：`a/b` 与 `token/value` 变成了什么。
4. 打开 `frontend/packages/web-domains/system/src/monitor/index.ts`。点 `id: 'web-domain-system-monitor'`、四条 `componentKey`、`batchLogout` 是否出现在权限数组。确认没有 `monitor/admin/index`。
5. 打开四份 Vue：Cache 找 `runtime.service.cache.get`；Online 找 `slice` 与缺失的 `@pagination`；LoginInfo / Operlog 找 `runtime.download` 的路径大小写。
6. 打开 `frontend/apps/admin-web/src/application/services.ts` 第 8 行与 `monitorService =` 那一行。再打开 `adminManifestRegistry.ts` 的 `adminMonitorWebRuntime`、`selectedManifestIds`、`adminExternalMonitorManifest`。用三色（厨房 / 四页 / iframe）在脑子里涂一遍。
7. 打开 `views/system/user/profile/index.vue` 的 `getOnlines`，以及 `onlineDevice.vue` 的 `removeCurrent`。说一句：这两枪为什么不在 `OnlinePage`。

## 总结、词汇表与下一步

- **`A:createMonitorService`** 是 `@namewta/domain-system/monitor` 子路径工厂：四抽屉打 `/monitor/{cache,loginInfo,online,operlog}`，操作日志 list 要投影，解锁仍是 GET，两把 Intent 尺失败关闭且不发 HTTP。它不是 `createSystemService` 的字段。
- **`A:system web-domain monitor pages`** 是同仓库 `@namewta/web-domain-system` 的 `createMonitorWebDomain`：四键四页，runtime 注入，导出走厅堂 `download`，在线页本地切片。没有独立的 web-domain-monitor 包。iframe 运维与资料页设备列表是隔壁门。
- 拼写合同：菜单键 `logininfo` ≠ HTTP `loginInfo` ≠ 字段 `loginInfo`；`operlog` URL ≠ 字段 `operationLogs`。
- 权限目录可以比按钮和 HTTP 更宽（`batchLogout`、`logininfo:query`）。口试以接线为准，不以目录幻想为准。

词汇表：`createMonitorService` / `MonitorService` / `createMonitorWebDomain` / `MonitorWebRuntime` / `runtimeView` / `componentKey` / `web-domain-system-monitor` / `segment` / `projectOperationLogTransport` / `NavigationIntent` / `MonitorSecurityError` / `externalIntent` / `attachmentIntent` / `monitorPermissions` / `admin-external-monitor`。

下一步：L-023 把四扇 Java 窗（Redis info、登录日志擦锁、操作日志清表、Sa-Token 踢人）摊开；L-049 才是通知投递监控。本课结束仍不是掌握证明；要练习请之后主动激活 Homework。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-005 | `frontend/apps/admin-web` | 厅堂创建 `monitorService`；runtime 袋；compose 选择集含 system-monitor 与 external-monitor；iframe 页；资料页 `current`/`removeCurrent` | `application/services.ts`；`router/adminManifestRegistry.ts`；`views/monitor/external/index.vue`；`views/system/user/profile/{index,onlineDevice}.vue` | 2026-09-16 |
| S-006 | `frontend/packages/{domains/system,web-domains/system}` | 子路径工厂与四页工厂同住 system 包；无独立 monitor 包 | 两包 `package.json`、`src/monitor/` | 2026-09-16 |
| S-010 | `release-artifacts/docker/infrastructure/mysql/init/50-cde-base-dml.sql` | 四键与 iframe 键的菜单种子 | `monitor/online/index` 等 insert；nacos 后补段 | 2026-09-16 |
| S-L024-01 | `frontend/packages/domains/system/src/monitor/index.ts` 与 `index.test.ts` | 工厂签名、四抽屉 URL/动词、segment 编码、Intent 失败关闭、Nacos 权限串、operlog 投影与空页 | `createMonitorService`；`binds the complete monitor surface` | 2026-09-16 |
| S-L024-02 | `frontend/packages/domains/system/src/monitor/transport.ts` 与 `transport.test.ts` | OpenAPI 行投影；丢掉 `serverOnly`；缺字段默认值 | `projectOperationLogTransport` | 2026-09-16 |
| S-L024-03 | `frontend/packages/domains/system/package.json` 与 `src/index.ts` | `./monitor` 子路径；根入口不导出监控工厂；`capabilities` 含 monitor-* | `exports["./monitor"]`；`systemDomainModule` | 2026-09-16 |
| S-L024-04 | `frontend/packages/web-domains/system/src/monitor/{index.ts,index.test.ts,runtime.ts,pages.ts}` 与四份 Vue | 四键顺序、权限目录含 batchLogout、runtime 口、页面实际调用与本地切片、download 旁路 | `createMonitorWebDomain`；`OnlinePage` `slice`；`runtime.download` | 2026-09-16 |
| S-L024-05 | `frontend/packages/web-domains/system/package.json` 与根 `src/index.ts` / `src/pages.ts` | 工厂从包根导出；根 pages 不含监控；子路径只到四页 | `export { createMonitorWebDomain }`；`./monitor/cache` | 2026-09-16 |
| S-L024-06 | `frontend/apps/admin-web/src/router/adminManifestRegistry.ts` 与 `adminManifestRegistry.test.ts` | runtime 接线；`selectedManifestIds`；三块监控键拆解 | `web-domain-system-monitor`；`monitor/online/index` vs `monitor/admin/index` vs `notify/monitor/index` | 2026-09-16 |
| S-L024-07 | 四份 Java 窗与 `adapters/axios-browser` `downloadWithAxios` | URL 对窗；export 是 POST blob；本课不覆盖窗内实现 | `CacheController`；`SysLoginInfoController`；`SysOperlogController`；`SysUserOnlineController`；`downloadWithAxios` 的 `client.post` | 2026-09-16 |
