---
lesson_id: L-073
objective_ids: [OBJ-73]
claimed_cells:
  - A:createWorkflowDefinitionService
  - A:createWorkflowWebDomain
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: kitchen-http
    minutes: 10
  - segment: factory-and-pages
    minutes: 10
  - segment: visuals-and-worked-examples
    minutes: 7
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 4
expression_level: eli5
coverage_depth: deep
source_ids: [S-005, S-006, S-008, S-010, S-015, S-L007-01, S-L073-01, S-L073-02, S-L073-03, S-L073-04, S-L073-05, S-L073-06, S-L073-07, S-L073-08]
---

# Lesson 073：宏观十二键流程厅——`createWorkflowDefinitionService` 怎样印 URL，`createWorkflowWebDomain` 怎样把菜贴到墙上

## 学完你能做什么

打开厨房 `frontend/packages/domains/workflow/src/index.ts` 的 `createWorkflowDefinitionService`，再打开菜单工厂 `frontend/packages/web-domains/workflow/src/index.ts` 的 `createWorkflowWebDomain`，你能**口述浏览器这一头怎样把流程纸条打到 `/workflow/*`，再贴到管理端墙上**：分类、图纸、表达式、待办、实例、请假示例。不是 Java 七扇分类窗（L-067），不是发布闸（L-068），不是跨模块 `WorkflowService`（L-069），不是任务枢纽十六扇（L-070），也不是「工厂名叫 Definition 所以只管图纸」。

口试名单就是矩阵 **(a)** 这两格，符号以**磁盘**为准：

1. **`A:createWorkflowDefinitionService`**（包 `@namewta/domain-workflow`，实现在根 `src/index.ts`，**不是** `transport.ts`）：工厂只认 `HttpClient`，返回 `Object.freeze` 的 `WorkflowDefinitionService`。封面是 **57 支扁平 HTTP 枪 + 一只嵌套 `users` 口**，分成六族：分类 6、图纸 13、SpEL 5、任务 16、实例 11、请假 6。每一枪都是内部 `request` → `http.request({ url, method, params?, data?, headers? })`。路径 id 走私有 `segment()`：先 `encodeURIComponent`，再逗号拼接。任务列表/详情会先投影再出门。**不**自己画 Vue，**不**读 `Admin-Token`，**没有** `exportDef` / `exportCategory` / `getInfo(businessId)`，**没有** `execute` 式的引擎入口。
2. **`A:createWorkflowWebDomain`**（包 `@namewta/web-domain-workflow`）：工厂先 `requireWorkflowWebRuntime`，缺托盘当场 throw；再冻住 `WebDomainManifest`。id 是 `web-domain-workflow`，`domainId` 是 `'workflow'`。磁盘上正好 **12** 条 `componentKey`（测试锁死顺序与名字）。它**不** `addRoute`，**不**读 `Admin-Token`，**不**自己拼 `/workflow/*` 字符串。设计页是 iframe，地址来自宿主 `designUrl`，不是厨房方法。

OBJ-73 还要你能把「墙上哪一行」和「厨房哪一枪」对上，并说出三处故意错位：

- 厅堂单例导出名是 `workflowService`，工厂名仍是 `createWorkflowDefinitionService`。不要发明 `createWorkflowService`。
- 导出图纸、导出请假、分类导出，页面走 `runtime.download(...)`，**不是**厨房方法。E2E 锁死定义导出是 `POST /workflow/definition/exportDef/{id}`。
- 厨房仍打 `GET /workflow/definition/definitionXml/{id}`（方法名 `legacyDefinitionXml`）。L-068 的 Controller **没有**这扇窗。Vue 也**不喊**它。测试锁的是遗留契约，不是活按钮。

2026-09-17 工作树先钉死**包边界**（口试先数包，再数函数）：

| 你可能以为的名字 | 磁盘事实 |
| --- | --- |
| `createWorkflowService` / `createWorkflowTaskService` | **没有。** 一座工厂管六族 |
| 工厂写在 `transport.ts`，像 notify | **没有。** 工厂、类型、名片全在 `src/index.ts`。`transport.ts` 只投影 `FlowTaskVo` |
| `@namewta/domain-workflow/definition` 会得到更小的图纸厨房 | **同一只手不在子路径。** 子路径只冻 `controller` + `basePath` 资源标签，并再 export 类型 |
| `workflowService.definitions.list` | **没有嵌套抽屉。** 方法名就是 `listDefinitions` |
| 页面 `import { workflowService } from '@/application/services'` | **十二张 web-domain 页都不。** 厅堂把单例塞进 `runtime.service` |
| `createWorkflowWebDomain(undefined)` 像 notify 那样仍能冻出键 | **会 throw。** `requireWorkflowWebRuntime`：`'WorkflowWebRuntime is required'`。对照 L-054 / L-066 |
| home / sso 对称一份 | **没有。** 两厅搜不到这两个工厂 |
| 厨房有 `POST /workflow/definition/exportDef/{id}` | **没有这支枪。** 页走 `runtime.download` |
| `WorkflowService.startCompleteTask` 是厨房方法 | **不是。** 那是 `wta-api` 给档案/请假房间的 Java 点菜单（L-069） |
| 设计器保存走本厨房 | **没有。** iframe 打 `/warm-flow-ui/` 与 `/warm-flow/save-json` |
| 请假列表挂在「工作流」目录下 | **C 型列表挂在「测试菜单」。** 隐藏编辑页才挂在工作流目录 |

本课**不宣称**你会拆 `FlwCategoryController` 七扇（L-067）、`FlwDefinitionController` 发布/导入（L-068）、大厅十三扇实例窗与 `WorkflowService.*`（L-069）、任务枢纽五支副作用（L-070）、`FlwSpelController`（L-071）、`TestLeaveController.submitAndFlowStart` 的 Java 链（L-072），或把九个 web-domain 工厂一行标 covered（GP-L-020）。今天只认：**浏览器这一头的流程厨房 URL 表，十二键菜单怎样把厨房喂进页面，页面实际扣了哪几枪，厨房标签上写着、方法表里没有、或方法表有、墙上没按钮的那几扇门。**

矩阵 (a) 那一行把九个工厂写在一起。本课只给 **`createWorkflowWebDomain` 这一颗**当菜单证据，**不要**把整行九厂标成 covered。厨房独行只给 **`createWorkflowDefinitionService`**。

## 先把宏观地图放在桌上

L-007 已经把插头插进厅堂：`export const workflowService = createWorkflowDefinitionService(domainHttp)`，只吃 http。L-008 对照过 home / sso **没有**这座厨房。L-020 讲导航 host 怎样用 `sys_menu.component` 对上 `registration.load`。L-067…L-072 是后端 `/workflow/*` 的六份 classic 窗。本课站在**已经登录的管理员浏览器**这一头：口袋里是 `Admin-Token`，司机是 `adminHttp`。

四条河都叫 workflow，货不一样：

| 名字听起来像 | 实际是什么 | 本课认不认 |
| --- | --- | --- |
| `createWorkflowDefinitionService` | 浏览器流程厨房：分类 / 图纸 / SpEL / 任务 / 实例 / 请假 + 借来的选人口 | **本课格子** |
| `workflowService` | 厅堂单例，把 `domainHttp` 塞进工厂 | **接线**；组合点是 L-007 |
| `createWorkflowWebDomain` | 管理端十二键菜单 | **本课格子** |
| `Flw*Controller` / `TestLeaveController` | 后端 classic 窗 | L-067…L-072 |
| `org.namewta.workflow.api.WorkflowService` | Java 跨模块点菜单 | L-069；**厨房没有这些方法名** |
| Warm-Flow UI / `DefService` | 设计器 iframe 与引擎钥匙 | **邻居**：本课只认谁拼 iframe URL |
| `completeWorkflowTask`（档案 runtime） | 档案页办复核时喊同一座厨房的 `completeTask` | **对照旁路**；不是十二键之一 |

2026-09-17 工作树：权威厨房是 `frontend/packages/domains/workflow/`（根 facade `src/index.ts` + 任务投影 `src/transport.ts` + 六个资源子目录）。权威菜单包是 `frontend/packages/web-domains/workflow/`。厅堂接线是 `apps/admin-web/src/application/services.ts` 第 52 行与 `apps/admin-web/src/router/adminManifestRegistry.ts` 第 87–139 行。`selectedManifestIds` 含 `'web-domain-workflow'`。home **不**选这份 id，也**没有** `workflow` domain。

菜单种子在 `30-cde-workflow.sql`（**不是** `50-cde-base-dml.sql` 那份系统菜单）：目录「工作流」+「我的任务」+「流程监控」，外加挂在「测试菜单」下的请假列表。隐藏两页：`workflow/processDefinition/design`、`workflow/leave/leaveEdit`。设计页 perms 写成 `workflow:leave:edit`——邻居陷阱，本课认它是种子笔误，不认它为设计器授权。

```text
已登录的管理员（浏览器，Admin-Token）
        │
        ├─ 十二张菜单页（createWorkflowWebDomain）
        │     runtime.service = workflowService
        │     runtime.designUrl / chartUrl / download / fileUpload / treePanel / dicts / confirm…
        │
        ├─ 档案旁路（不是本工厂的键）
        │     completeWorkflowTask → workflowService.completeTask
        │
        x  没有 home / sso 插座
                        │
                        v
              ┌──────────────────────────────────────────────┐
              │  createWorkflowDefinitionService(domainHttp) │  ← 本课厨房
              │    分类 6 / 图纸 13 / SpEL 5                 │
              │    任务 16（含投影）/ 实例 11 / 请假 6         │
              │    users = createUserQueryPort(http)         │  ← 借人事室
              └──────────────────────────────────────────────┘
                        │
                        v
              厅堂 domainHttp → adminHttp.request
              （Bearer 登录票；不是 Warm-Flow 设计器内部票）
                        │
        /workflow/category/*     /workflow/definition/*
        /workflow/spel/*         /workflow/task/*
        /workflow/instance/*     /workflow/leave/*
        /system/user/list|optionselect|deptTree   ← users 口
        （厨房不打 /workflow/definition/exportDef、
          厨房不打 /workflow/category/export、
          厨房不打 /workflow/leave/export、
          厨房不打 /workflow/instance/getInfo/{businessId}、
          厨房仍测试 GET .../definitionXml/{id}，Controller 没有）
```

往下走不要跳层：

```text
Vue 流程页（只收 runtime；任务页另收 mode）
    └─ web-domain 外包一层 h(page, { runtime, ...props })
          └─ domain 工厂拼 URL / method / 任务投影
                └─ App 的 domainHttp（axios + Admin-Token）
                      └─ 后端 Flw* / TestLeave Controller（L-067…L-072）
```

设计器**不**走这条河的保存枪：

```text
DefinitionPage 点「流程设计」
    └─ router.push /workflow/design/index?definitionId=&disabled=
          └─ DesignPage iframe ← runtime.designUrl(...)
                └─ /warm-flow-ui/index.html?...   （厅堂拼）
                      └─ iframe 自己打 /warm-flow/save-json
```

**类比：** 把厨房想成**一本印好地址的出餐单**，把 `createWorkflowWebDomain` 想成**大厅墙上的十二行点菜单**。服务员（Vue）只对托盘喊「来一份 listDefinitions / pageTaskWaiting」。地址印在出餐单上。选人时厨房向人事室借电话本（`users`），不是自己印 `/system/user`。墙上请画图纸的那一行，其实是请**外面的画师**进 iframe，不是后厨再炒一道菜。

**类比失效边界：** 「宏观十二键」**不**等于「厨房里只有十二道菜」。厨房还有 `legacyDefinitionXml`、`getDefinitionXmlString`，墙上没按钮。类比也**不**等于「home 选了 `web-domain-workflow` 就会少几道菜」——home 根本不选这份 id，连厨房单例都没造。类比还不等于「工厂名叫 Definition 所以任务枪是另一座工厂」。类比更**不**等于「点保存就是引擎 `DefService.checkAndSave` 的内部实现」——浏览器只保证打到哪扇窗。类比也不等于「`WorkflowService` 是厨房的 Java 名」——那扇窗给邻居模块，浏览器不走它。

## 核心概念与机制

### 直觉讲解

小孩子版先记十四句：

1. **先找工厂，再找十二键。** 厨房 `createWorkflowDefinitionService(http)`。菜单 `createWorkflowWebDomain(runtime)`。封面 id 永远是 `web-domain-workflow`。
2. **工厂名有 Definition，货却有六族。** 分类、图纸、表达式、任务、实例、请假，全在同一份 freeze 对象上。
3. **`transport.ts` 不是工厂。** 它只把 OpenAPI 的 `FlowTaskVo` 投影成 `WorkflowTask`：日期、`instanceId` 变字符串、`varList` 变 `Map`，丢掉 `delFlag` / `permissionList` / button 的 `value`。
4. **十二键不是随便排。** 测试锁死：category → processDefinition → design → spel → taskWaiting → taskFinish → taskCopyList → myDocument → allTaskWaiting → processInstance → leave → leaveEdit。
5. **厨房从托盘进门。** 页面只认 `props.runtime.service.*`。
6. **选人不是 `/workflow/user`。** `UserSelect` 喊 `service.users.list` / `departmentTree` / `options`，URL 在 `/system/user/*`。
7. **保存图纸 ≠ 发布 ≠ 设计。** 对话框保存走 `addDefinition` / `updateDefinition`；列表再点发布；画线在 iframe。
8. **导出不走厨房方法。** 定义和请假点导出，喊 `runtime.download`。
9. **请假三条出门路。** 保存草稿 = add/update；提交审批 = 先存再 `startWorkflow`；「后端发起」= `submitLeave`（`/submitAndFlowStart`）。
10. **任务页一张 Vue、四个 mode。** `TaskListPage` 靠工厂 `mode` 换枪：waiting / finished / copy / all-waiting。
11. **缺托盘会关门。** 本工厂有 `requireWorkflowWebRuntime`。notify / third 2026-09-17 没有。
12. **应用办理对话框不是第十三键。** `ProcessActionDialog` 是组件，挂在请假编辑页和「流程干预」上。
13. **档案办复核会借用同一座厨房。** 那是 profile runtime 的旁路，不是流程菜单。
14. **名片七项 capabilities 不是 componentKey。** 挂页的是 manifest + 菜单种子 + `selectedManifestIds`。

**类比失效边界：** 点菜单类比**不**覆盖「中间节点没办理人时发布会失败」——那是 L-068 的闸。类比也**不**等于「墙上没挂 xmlString 厨房就把刀收了」——枪还在。类比还不等于「iframe 里的保存会走 `updateDefinition`」——设计器走 Warm-Flow 自己的 `/warm-flow/save-json`。

### 精确定义与 English term

| 中文口头 | English term | 磁盘落点 |
| --- | --- | --- |
| 流程厨房工厂 | `createWorkflowDefinitionService` | `domains/workflow/src/index.ts`；返回 `Object.freeze` |
| 流程厨房封面 | `WorkflowDefinitionService` | 同文件 `export interface`；57 方法 + `users` |
| 任务投影 | `projectWorkflowTaskTransport` | `src/transport.ts`；输入 `OpenApiSchema<'FlowTaskVo'>` |
| 路径段编码 | `segment` | 私有箭头：数组/标量 → `encodeURIComponent` → 逗号拼接 |
| 选人口 | `UserQueryPort` / `users` | 从 `@namewta/domain-system/user` 的 `createUserQueryPort` 嵌进来 |
| 模块名片 | `workflowDomainModule` | `id: 'workflow'`，`backendModules: ['wta-workflow']`，七项 capabilities |
| 资源标签 | resource metadata | 如 `workflowDefinitionResource.controller = 'FlwDefinitionController'`；**不是** HTTP 发送 |
| 管理端流程菜单工厂 | `createWorkflowWebDomain` | `web-domains/workflow/src/index.ts`；manifest id `web-domain-workflow` |
| 菜谱板 | `WebDomainManifest` | `id` / `domainId` / `messages` / `permissions` / `registrations` |
| 组件键 | `componentKey` | 与 `sys_menu.component` 对表 |
| 运行时托盘 | `WorkflowWebRuntime` | `src/runtime.ts`；`service` 类型是 `WorkflowDefinitionService` |
| 失败关闭（本厂） | fail-closed runtime | `requireWorkflowWebRuntime`；缺 runtime 当场 throw |
| 设计器控制器 | `createDesignerController` | `src/designer.ts`；只认 `designUrl` + `close` 消息 |
| 办理按钮可见集 | `enabledProcessButtons` | 参与者看 `buttonList.show`；干预模式另算一套 |
| 宿主端口 | host port | `designUrl` / `chartUrl` / `download` / `fileUpload` / `treePanel` / `dicts` / `confirm` / `success` / `error` / `closeCurrentPage` / `closeDesigner` / `resolveAttachments` |
| 跨模块 Java 点菜单 | `WorkflowService` | `wta-api`；**不是**本课工厂 |

厨房六族与 HTTP 门牌（动词以 `index.ts` 为准）：

| 族 | 方法数 | 门牌前缀 | 本课十二页用不用 |
| --- | --- | --- | --- |
| 分类 | 6 | `/workflow/category` | **用。** 管理页 list/get/add/update/delete；侧栏 `categoryTree`。**无** export |
| 图纸 | 13 | `/workflow/definition` | **用** list / unpublished / CRUD / publish / unpublish / active / import / copy。**无 Vue** 喊 `legacyDefinitionXml` / `getDefinitionXmlString`。导出走 download |
| SpEL | 5 | `/workflow/spel` | **用。** `SpelPage` 五枪。种子有 export，厨房和页都没有 |
| 任务 | 16 | `/workflow/task` | **用。** 四 mode 列表 + 办理对话框 + 催办改办理人。十六扇后端窗厨房全接 |
| 实例 | 11 | `/workflow/instance` | **用。** 运行中/已结束/我发起的、历史、变量、作废、激活、两条删除、撤销。厨房**没有** `getInfo/{businessId}`、`deleteByBusinessIds` |
| 请假 | 6 | `/workflow/leave` | **用。** 列表 + 编辑三路出门。导出走 download |
| `users` | 3 URL | `/system/user/*` | **用。** `UserSelect` / `prepareUserSelection` |

`workflowDomainModule.capabilities`：`category-admin` / `definition-admin` / `definition-design` / `spel-admin` / `task-runtime` / `instance-runtime` / `leave-runtime`。这是名片，**不是** componentKey，也不决定 App 挂哪几页。

### 机制/因果链

**1. 一座扁平厨房，路径编码，任务才投影。**

`createWorkflowDefinitionService(http)` 立刻 freeze。内部 `request` 只是 `http.request<ApiResponse<T>>(config)`，不剥字段、不加 `no-store`。写动词按后端存量：**GET 读，POST 新增/导入/办理/作废，PUT 改/发布/激活/撤销/改办理人/改变量，DELETE 删**。不要把 third 的「变更一律 POST」套过来。

`segment('task/1')` → `task%2F1`。`deleteCategory(['first/id', 'second id', 'comma,value'])` → `/workflow/category/first%2Fid,second%20id,comma%2Cvalue`。测试锁死这条。分类后端 `@PathVariable Long categoryId` **绑不上**逗号串——厨房测试保的是浏览器拼法，不是后端一定吃得下。定义删除的路径变量是 `List<Long>`，逗号分隔才对得上。

任务五份分页 + `getTask` 走 `projectTaskPage` / `projectTask`。缺 `rows` 时变成 `{ rows: [], total }`；缺 data 的详情原样返回 metadata（测试：`{ code: 204 }`）。`currentTaskUsers` 先 `projectUserSummary`，再拆掉 `phoneNumber` 和测试里的邮箱——减签名单只要身份。

`importDefinition` 是厨房里**唯一**带 `headers: { repeatSubmit: false }` 的枪。页把 `FormData`（`file` + `category`）塞进去。

嵌套 `users` **不是**厅堂另写的 directory 对象（对照 notify）。它在工厂闭包里 `createUserQueryPort(http)`：`GET /system/user/list`、`GET /system/user/optionselect?userIds=`、`GET /system/user/deptTree`。包依赖表**有** `@namewta/domain-system`。口试不要说「厅堂把 systemService 传给了 workflow」。

**2. 菜单工厂先关门再冻十二键。**

`createWorkflowWebDomain(runtimeInput)`：`requireWorkflowWebRuntime` 看到 `undefined` 就 throw。测试传入的是 stub runtime，**不是** `undefined`，所以仍能冻出十二键。把 `service: {} as never` 传进去也能得到键——缺方法要等页面点击才爆。对照档案楼：缺十一口 archive 当场 throw。对照 notify/third：工厂根本不检查。

`registrations[].load` 闭包住这份 runtime，再 `runtimeView` → `defineComponent({ setup: () => () => h(page, { runtime, ...props }) })`。任务四键把同一张 `TaskListPage.vue` 包四次，`mode` 分别是 `'waiting' | 'finished' | 'copy' | 'all-waiting'`。设计键 `componentName` 是 `WarmFlow`。

权限目录六组（字符串以工厂为准）：

| id | 冻进去的串 | 页上实际核对 |
| --- | --- | --- |
| `workflow-category` | list / query / add / edit / remove | 新增、改、删。**没有** export 串 |
| `workflow-definition` | list / add / edit / remove / import / **export** / active / copy / query / publish | 工具栏与行按钮。取消发布与发布共用 `publish` |
| `workflow-spel` | list / query / add / edit / remove | 增删改。种子有 `workflow:spel:export`，本组**没收** |
| `workflow-task-runtime` | `workflow:task:list`、`workflow:task:edit` | 待办任务页的催办 / 改办理人 / 干预 |
| `workflow-instance-runtime` | list / currentList / cancel / remove / invalid / active / query / variable / variableQuery | 实例页与「我发起的」 |
| `workflow-leave-runtime` | list / query / add / edit / remove / export | 列表导出走 download；撤销核的是 **instance:cancel** |

`messages` 只冻三句标题：分类 / 定义 / 表达式。其余九页没有 i18n 块。不要把 messages 数成十二。

`composeAppRuntime` 只有 `selectedManifestIds` 含 `web-domain-workflow` 时才把键交给导航。admin 厅堂这份 id 在 registry 第 369 行。home 不选。重复 `componentKey` 会 `AppRuntimeError`——测试第二例锁死。

**3. 厅堂托盘把设计器、下载、选人附件从页面 import 里拆出去。**

`adminWorkflowWebRuntime`（registry 第 87 行）：

| runtime 口 | 厅堂接到哪 | 页怎么用 |
| --- | --- | --- |
| `service` | `workflowService` | 所有厨房方法 |
| `designUrl` | `VITE_APP_BASE_API + /warm-flow-ui/index.html?id=&onlyDesignShow=&clientid=` | `DesignPage` iframe |
| `chartUrl` | 同 UI，`type=FlowChart` + 时间戳 | `FlowChart.vue` |
| `closeDesigner` | `tab.closeOpenPage({ path: '/workflow/processDefinition', query: { activeName } })` | iframe `postMessage` method=`close` |
| `download` | `application/http.download` | 定义导出、请假导出 |
| `fileUpload` | 厅堂 `FileUpload` 异步组件 | 办理附件 |
| `treePanel` | 厅堂 `TreePanel` | 分类树侧栏 |
| `dicts` | `createLiveWorkflowDictRefs` → `useDict` | 如 `wf_task_status` |
| `resolveAttachments` / `downloadAttachment` | `systemService.resources.oss.listByIds` / OSS 下载 | 审批记录附件 |
| `confirm` / `success` / `error` | host/feedback | 确认框与提示 |
| `closeCurrentPage` | 关标签 | 请假保存后离开 |

口试不要说「页面直接 `import workflowService`」。选人组件只收 `service` prop，再喊 `service.users`。

**4. 分类页用 list + 浏览器拼树；侧栏才打 categoryTree。**

`CategoryPage`：`listCategories` 拿扁平表，`handleTree` 在浏览器里按 `categoryId` / `parentId` 长成树。下拉同样 `listCategories()`，**不**打 `categoryTree`。删一行 `deleteCategory(row.categoryId)`，是单 id。

`DefinitionPage` / `InstancePage` / `MyDocumentPage` 侧栏才 `categoryTree()`。`categoryTree` 的查询类型在封面上写成 `CategoryForm`，实现照样当 params 发出去。

分类导出：后端有 `POST /workflow/category/export`，种子有 F 型 `workflow:category:export`，厨房没有枪，权限组没收，页没有按钮。

**5. 图纸页两抽屉，发布和保存拆开，设计走隐藏路由。**

`DefinitionPage`：TAB `activeName === '0'` → `listDefinitions`（已发布）；否则 `listUnpublishedDefinitions`。保存 `addDefinition` / `updateDefinition`，`ext` 是 `{ autoPass }` 的 JSON 字符串。动态表单「是」被 disabled，只许 `formCustom = 'N'`。

发布 / 取消发布 / 激活 / 复制 / 删除走厨房对应枪。导入先选分类（禁止 `'ALL'`），再 `importDefinition(FormData)`，成功跳未发布 TAB。导出 `runtime.download(\`/workflow/definition/exportDef/${ids[0]}\`, {}, \`${flowCode}.json\`)`。E2E 认 POST。

设计：未发布行 `design()` → `/workflow/design/index?definitionId=&disabled=false&activeName=`；已发布 `designView()` 把 `disabled=true`。`DesignPage` 不喊厨房，只 `createDesignerController`。测试：`disabled: 'true'` 会当成布尔 true 传给 `designUrl`。

厨房两支 XML 枪：**无 Vue 调用**。`legacyDefinitionXml` 打的 `definitionXml` 路径，Controller 2026-09-17 没有映射。`getDefinitionXmlString` 打的是真窗 `xmlString`（方法体却是 `exportJson`，L-068）。

**6. SpEL 五枪齐，导出种子是空子弹。**

`SpelPage`：`listSpel` / `getSpel` / `addSpel` / `updateSpel` / `deleteSpel`。种子 F 型 `workflow:spel:export` 存在，`FlwSpelController` **没有** `/export`，厨房也没有。不要发明导出按钮。

**7. 一张任务页四个 mode；办理对话框按 buttonList 扣扳机。**

| mode（工厂传入） | 列表枪 | 菜单键 |
| --- | --- | --- |
| `waiting` | `pageTaskWaiting` | `workflow/task/taskWaiting` |
| `finished` | `pageTaskFinished` | `workflow/task/taskFinish` |
| `copy` | `pageTaskCopies` | `workflow/task/taskCopyList` |
| `all-waiting` + TAB waiting | `pageAllTaskWaiting` | `workflow/task/allTaskWaiting` |
| `all-waiting` + TAB finished | `pageAllTaskFinished` | 同一键 |

抄送 mode **没有**选申请人。待办任务 TAB 才有改办理人 / 催办 / 干预。催办 `urgeTask(createUrgePayload(ids, message, types))`，默认消息类型 `['1']`（站内信，复选框 disabled）。改办理人 `updateAssignee(taskIds, userId)` → `PUT /workflow/task/updateAssignee/{userId}`，body 是任务 id 数组。

点「办理」**不**在本页调 `completeTask`。它 `router.push({ path: task.formPath, query: { id: businessId, taskId, type: 'approval'|'view' } })`。请假示例的 `formPath` 会进隐藏的 `leaveEdit`。

`ProcessActionDialog`：打开时并行 `getTask` + `getNextNodes`。参与者模式按钮看 `buttonList.show`；干预模式（待办任务页）在 `flowStatus === 'waiting'` 时强制给 transfer / termination，会签再加 addSign / subSign。提交 `completeTask(createCompletePayload(...))`（意见 trim，抄送变成 `flowCopyList`）。委托/转办/加签/减签 `operateTask(payload, 'delegateTask'|'transferTask'|'addSignature'|'reductionSignature')`。减签先 `currentTaskUsers`。退回先 `getBackTaskNodes` 再 `backProcess`。终止 `terminateTask({ taskId, comment })`。

**8. 实例两 TAB + 「我发起的」；按业务 id 的两扇窗厨房没收。**

`InstancePage`：运行中 `pageRunningInstances`，已结束 `pageFinishedInstances`。运行中可激活、作废。删除：运行中 `deleteInstances`，已结束 `deleteHistoricInstances`。变量 `instanceVariables` / `updateInstanceVariables`。历史 `flowHistory(businessId)`。

`MyDocumentPage` 键却是 `workflow/task/myDocument`，组件名 `myDocument`，权限 `workflow:instance:currentList`。列表枪是 `pageCurrentInstances`。可编辑行才能删实例；`waiting` 才能撤销，payload `createCancelProcessPayload(businessId)`，留言写死「申请人撤销流程！」。

后端还有 `GET /workflow/instance/getInfo/{businessId}` 与 `DELETE .../deleteByBusinessIds/{businessIds}`。厨房方法表**没有**。Java 跨模块 `WorkflowService` 会用到按业务 id 的删/查（L-069），浏览器十二页不走。

**9. 请假列表挂测试菜单；编辑页三条出门路。**

`LeaveListPage` 键 `workflow/leave/index`，父菜单是 `50-cde-base-dml.sql` 的「测试菜单」`1761400000000000005`。列表 `listLeaves`。导出 `runtime.download('/workflow/leave/export', query, ...)`。可编辑状态 `draft|cancel|back`；撤销只在 `waiting`，走 `cancelProcess`，权限串却是 `workflow:instance:cancel`。

隐藏 `LeaveEditPage`：`query.type` 为 add/update/view/approval。天数 `calculateLeaveDays`：两个时间戳，向下取整天再 +1；区间非法返回 `undefined`，页拒绝提交。

`persist` 三路：

| 按钮 | action | 厨房枪 |
| --- | --- | --- |
| 保存 | `'draft'` | 有 id → `updateLeave`，否则 `addLeave` |
| 提交审批 | `'start'` | 先 add/update，再 `startWorkflow({ businessId, flowCode, variables, bizExt })`，变量里 `userList: ['1','3','4']` 写死 |
| 后端发起 | `'direct'` | `submitLeave` → `POST /workflow/leave/submitAndFlowStart` |

提交审批成功若带回 `taskId`，就打开办理对话框。`flowCode` 默认 `leave1`，下拉 leave1…leave6 是页内硬编码，不是厨房方法。

**10. 厨房有枪没扳机；更外面还有根本没写进厨房的窗；设计器在第三条河。**

2026-09-17 **无 Vue** 喊：`legacyDefinitionXml`、`getDefinitionXmlString`。

厨房对象上**根本没有**：`exportDef`、分类/请假/SpEL 的 export、`/workflow/instance/getInfo/{businessId}`、`deleteByBusinessIds`、任何 `/warm-flow/*`、任何 `WorkflowService.*` 方法名。

种子里的 `workflow:category:export`、`workflow:spel:export` 是 F 型权限行。分类/SpEL 页没有对应按钮。定义和请假的导出按钮走 download 口，不走厨房封面。

### 图、表或文本图

**图 1：宏观十二键怎样吃到厨房**

```text
 sys_menu.component（管理端；种子在 30-cde-workflow.sql）
   workflow/category/index
   workflow/processDefinition/index
   workflow/processDefinition/design     ← 隐藏 C；perms 写成 leave:edit
   workflow/spel/index
   workflow/task/taskWaiting
   workflow/task/taskFinish
   workflow/task/taskCopyList
   workflow/task/myDocument              ← 键在 task，枪在 instance
   workflow/task/allTaskWaiting
   workflow/processInstance/index
   workflow/leave/index                  ← 父菜单是「测试菜单」
   workflow/leave/leaveEdit              ← 隐藏 C
        │
        v
 GET /system/menu/getRouters
        │
        v
 App composeAppRuntime
   selectedManifestIds 含 web-domain-workflow
        │
        ├─ 键在已选 manifest → registration.load
        │     h(Page, { runtime, mode? })
        └─ 键不在已选清单 → 解析失败关闭
                │
                v
         Page props.runtime
                │
                ├─ runtime.service = createWorkflowDefinitionService(...)
                ├─ runtime.designUrl / chartUrl / download / users（在 service 上）
                └─ 页面只喊方法名，不拼 URL（导出除外：download 吃字符串）
```

**图题 / caption：** 宏观同一厨房、管理端十二键、托盘把厨房和设计器地址一起塞进页面。alt：菜单键来自 30-cde-workflow.sql；admin 选 web-domain-workflow；页面只收到 runtime；HTTP 字符串在 domain 工厂，导出字符串在 download。

**文字等价物：** 人先碰到动态菜单里的 component 字符串。App 用已选 manifest 把字符串换成带 runtime 的 Vue 页。十二张流程页面向 `runtime.service` 喊方法；选人走嵌套 `users`；导出和设计器 iframe 走宿主口。厨房把方法换成 GET/POST/PUT/DELETE 和 `/workflow/...`。home 即使以后有人把厨房单例造出来，墙上没有这十二键，导航也不会挂页。`workflow/task/myDocument` 这种把任务目录和实例枪拼在一起的键，registry 仍能对上 `MyDocumentPage`，不要按目录名猜 HTTP。

**图 2：厨房有枪 / 本课页面扣扳机**

| 厨房方法 | HTTP（动词以 index.ts 为准） | 2026-09-17 谁扣扳机 |
| --- | --- | --- |
| `listCategories` / CRUD | GET list；GET/DELETE `/{id}`；POST/PUT `/workflow/category` | **CategoryPage** |
| `categoryTree` | GET `/categoryTree` | **Definition / Instance / MyDocument 侧栏**；分类管理页不用 |
| `listDefinitions` / `listUnpublishedDefinitions` | GET `/list`、`/unPublishList` | **DefinitionPage** 两 TAB |
| `add/update/get/delete/copy/publish/unpublish/active/import` Definition | POST/PUT `/`；GET/DELETE `/{id}`；POST `/copy` `/importDef`；PUT `/publish` `/unPublish` `/active` | **DefinitionPage** |
| `legacyDefinitionXml` | GET `/definitionXml/{id}` | **无 Vue**；Controller **无此窗** |
| `getDefinitionXmlString` | GET `/xmlString/{id}` | **无 Vue**；后端窗在 |
| 定义导出 | `POST /exportDef/{id}` | **download 口**，不是厨房方法 |
| SpEL 五枪 | `/workflow/spel` | **SpelPage** |
| 任务五份分页 | `pageByTaskWait` 等 | **TaskListPage** 四 mode |
| `startWorkflow` / `completeTask` / `backProcess` / `operateTask` / `terminateTask` / `getTask` / `getNextNodes` / `getBackTaskNodes` / `currentTaskUsers` | `/workflow/task/*` | **LeaveEdit + ProcessActionDialog**；档案旁路也 completeTask |
| `urgeTask` / `updateAssignee` | POST urge；PUT updateAssignee | **all-waiting 待办 TAB** |
| 实例三份分页 / 历史 / 变量 / 作废 / 激活 / 两删除 / 撤销 | `/workflow/instance/*` | **InstancePage + MyDocumentPage + LeaveList 撤销** |
| `getInfo/{businessId}` / `deleteByBusinessIds` | 后端有 | **厨房无方法，无 Vue** |
| 请假六枪 | `/workflow/leave` | **LeaveList + LeaveEdit** |
| 请假导出 | `POST /leave/export` | **download 口** |
| `users.list/options/departmentTree` | `/system/user/*` | **UserSelect** |

**图题 / caption：** 工厂注册了十二键 ≠ 厨房每支枪都有按钮 ≠ 后端每扇窗都进了厨房。alt：分类侧栏才用 tree；两支 XML 枪无页面；导出走 download；实例按业务 id 两窗没进厨房；设计器不在这张表。

**文字等价物：** 管理员十二页消耗分类 CRUD、图纸发布导入复制、SpEL 五枪、任务十六扇里的列表与办理、实例运行/结束/我的、请假三路出门。选人三枪打在 `/system/user`。顶栏没有流程铃铛。xmlString / definitionXml 目前只有厨房（后者连后端窗都没有）。导出三扇（分类/定义/请假）里，定义和请假走宿主 download，分类谁都不接。不要把「工厂注册了十二键」说成「所有 `/workflow/*` HTTP 都有按钮」。

**图的边界：** 不画 Java classic 链和 LiteFlow XML（L-067…L-072）。不画 Warm-Flow 引擎内部 skip。不保证以后产品会补 xmlString 预览或按业务 id 删除页。不把 capabilities 名片画成菜单。不把 `WorkflowService` 画进浏览器河。不把请假 `userList: ['1','3','4']` 教成通用选人 API。

## 正例、反例与边界

**正例 1 — 管理员翻已发布图纸。** 有 `workflow:definition:list` 的人打开 `workflow/processDefinition/index`。默认 TAB 已发布，`listDefinitions({ category, flowName, flowCode, pageNum, pageSize })`。厨房 `GET /workflow/definition/list`。

**正例 2 — 保存后再发布。** 点新增，选分类、编码、名称、经典模式。保存 `POST /workflow/definition`，页跳未发布 TAB。再点「发布流程」`PUT /workflow/definition/publish/{id}`。对话框里没有「保存并发布」合一按钮。中间节点没办理人时失败文案从 L-068 的后窗回，厨房不预检。

**正例 3 — 部署 JSON。** 选非 ALL 分类，拖一个本项目导出的 JSON。`importDefinition(FormData)` → `POST /workflow/definition/importDef`，header `repeatSubmit: false`。成功进未发布抽屉。

**正例 4 — 导出不喊厨房方法。** 勾一行，点导出。`runtime.download('/workflow/definition/exportDef/d0', {}, 'xxx.json')`。E2E 看到 `POST` 该路径。freeze 对象上搜不到 `exportDefinition`。

**正例 5 — 未发布才能设计。** `isPublish === 0` 显示「流程设计」，`disabled=false`。已发布显示「查看流程」，`disabled=true`。iframe 源是厅堂拼的 `/warm-flow-ui/index.html?id=...&onlyDesignShow=true|false&clientid=`。iframe 说 `method: 'close'`，厅堂把定义 TAB 的 `activeName` 带回去。

**正例 6 — 我的待办办理请假。** `pageTaskWaiting` → 行上 `formPath` 推到 `/workflow/leaveEdit/index?id=&taskId=&type=approval`。对话框 `getTask` + `getNextNodes`，点提交 `completeTask`。意见两端空格会被 `createCompletePayload` trim 掉。

**正例 7 — 待办任务改办理人。** mode `all-waiting` 且 TAB 待办，核 `workflow:task:edit`。`UserSelect` 单选，`updateAssignee(['task/1'], 'user/1')` → `PUT /workflow/task/updateAssignee/user%2F1`，body `['task/1']`。

**正例 8 — 请假「后端发起」。** 填类型和时间。`submitLeave(form)` → `POST /workflow/leave/submitAndFlowStart`。这**不是**页再调一次 `startWorkflow`。对照「提交审批」那条：先 `addLeave` 再 `startWorkflow`。

**正例 9 — 减签不带手机号。** 办理对话框点减签。`currentTaskUsers` 回来的对象没有 `phoneNumber`，测试锁死 JSON 里不能出现 `must-not-cross`。

**正例 10 — 缺 runtime 当场关门。** `createWorkflowWebDomain(undefined)` throw `'WorkflowWebRuntime is required'`。notify 工厂 2026-09-17 不会。

**反例 1 — 「十二张 Vue `import { workflowService }`。」** 全包搜不到 `@/application/services`。直接消费单例的是厅堂 registry 和档案 `completeWorkflowTask`。

**反例 2 — 「`createWorkflowWebDomain` 等于九个 web-domain 工厂都 covered。」** 矩阵 (a) 把九个名字写在同一行。GP-L-020 已经挖过：不要整行盖章。本课只给流程这一颗。

**反例 3 — 「工厂在 `transport.ts`。」** 打开 `src/index.ts` 第 334 行。`transport.ts` 只有投影。

**反例 4 — 「`createWorkflowService`。」** L-007 反例已经禁这个名字。导出名 `workflowService` ≠ 工厂名。

**反例 5 — 「`service.definitions.list` / `service.tasks.waiting`。」** 扁平：`listDefinitions`、`pageTaskWaiting`。

**反例 6 — 「从 `@namewta/domain-workflow/task` 进口小厨房。」** 子路径只有 `workflowTaskResource = { controller: 'FlwTaskController', basePath: '/workflow/task' }`。

**反例 7 — 「页面手写 `/workflow/task/completeTask`。」** 违反 L-004 方向。办理走 `runtime.service.completeTask`。导出是**唯一**页里出现 `/workflow/...` 字符串的地方，而且走 download，不走 request 工厂。

**反例 8 — 「`legacyDefinitionXml` 能预览图。」** 无 Vue。路径在 Controller 上 404。真 JSON 窗是 `xmlString`，同样无按钮。

**反例 9 — 「分类页打 `categoryTree`。」** 管理页 `listCategories` + `handleTree`。tree 是定义/实例侧栏。

**反例 10 — 「删除分类可以一次传 `1,2`。」** 厨房测试会拼，后端 `Long categoryId` 绑不上。页实际只传当前行 id。

**反例 11 — 「设计页权限是 `workflow:definition:edit`。」** 种子 perms 是 `workflow:leave:edit`。行按钮核的是 `workflow:definition:query`。三套字不要背成一句。

**反例 12 — 「请假列表在工作流目录下。」** C 型 `workflow/leave/index` 的父菜单是测试菜单。工作流目录下的是隐藏 `leaveEdit`。

**反例 13 — 「`myDocument` 走任务分页。」** 枪是 `pageCurrentInstances`。

**反例 14 — 「home 也能打开我的待办。」** 工作树没有这份工厂接线。

**反例 15 — 「`WorkflowService.completeTask` 就是厨房 `completeTask`。」** 一个是 Java 接口给档案模块；一个是浏览器 `POST /workflow/task/completeTask`。档案页办复核时，厅堂把前者的命令**翻译**成后者。不要说厨房实现了 `wta-api`。

**反例 16 — 「iframe 保存走 `updateDefinition`。」** 保存 JSON 的窗是 `/warm-flow/save-json`，XSS 排除名单里有它（L-068 邻居）。厨房封面没有。

**边界 1 — 工厂校验 runtime ≠ 校验 service 方法齐。** 缺 `listDefinitions` 仍能通过 `requireWorkflowWebRuntime`，只要对象不是 `undefined`。页面一喊才会在运行时失败。

**边界 2 — `hasPermission` 藏按钮；HTTP 仍可能 403。** 我的待办 / 已办 / 抄送菜单种子甚至没写 `perms` 字段。厨房把错误原样抛出。

**边界 3 — 路径编码只管路径段。** query 里的 `flowName` 不走 `segment()`。`users.options` 把 id 编码后拼进 **URL 查询串** `?userIds=`，不是 params 对象。

**边界 4 — 任务投影只发生在任务族。** 实例、请假、图纸 VO 原样过。不要说「整个厨房都 fail-closed 投影」。

**边界 5 — 请假提交审批的 `userList` 写死。** 这是示例流，不是选人组件。换真实业务表单应改 Vue，不要假装厨房会填办理人。

**边界 6 — 干预模式可以不管 `buttonList.show`。** 测试：后端把 transfer 标 `show: false`，干预仍给 transfer / termination / 会签加减签。参与者模式严格跟 `show`。

**边界 7 — 字典与硬编码并存。** 任务状态走 `runtime.dicts('wf_task_status')`；请假类型 1–4、leave1–leave6、催办渠道 1/2/3 写在 Vue 里。

**边界 8 — 激活开关的「下一状态」看 v-model。** 实例页 `next = row.activityStatus === 1` 发生在 switch 已经改过之后。失败要把行上的值拨回去。

**边界 9 — `repeatSubmit: false` 只贴在导入。** 发布/保存仍可能被厅堂 axios 的防重复提交拦住——那是司机的缓存，不是厨房第二支 header。

**边界 10 — 包依赖有 `api-contracts`，URL 却是字符串字面量。** 只有任务投影吃 `OpenApiSchema<'FlowTaskVo'>`。不要说 57 条 URL 都是 `keyof paths`。

## 变式与迁移

1. **和 L-007 对照。** 组合点课只要求你能指着「`createWorkflowDefinitionService` 一行，注入 `domainHttp`，导出名 `workflowService`」。本课要求能把六族 URL 背到 method，并指出 XML 鬼窗、三扇导出、两扇按业务 id 的实例窗。不要把 L-007 的接线板图当成已经 covered 的方法表。
2. **和 L-053 / L-065 对照。** 通知厨房嵌套抽屉 + 厅堂 directory + 备用枪。三方厨房扁平十五枪、变更 POST、缺口在后端 `/save`。流程厨房扁平六族、动词混用 GET/PUT/DELETE、选人口嵌在工厂里、缺口是导出/鬼窗/按业务 id。不要把「domain 工厂」说成同一种抽屉形状。
3. **和 L-054 / L-066 对照。** 都是「页面经 runtime 消费厨房」。差别：流程厂 **会** `requireWorkflowWebRuntime`；notify/third 2026-09-17 不会。流程是十二键多张 Vue；third 是四键一张页四个 kind。流程有 iframe 第三条河。
4. **和 L-067…L-072 对照。** 后端空壳 Mapper、发布闸、LiteFlow、`submitAndFlowStart` 事务，本课不重讲。前端保证：哪一枪打哪条 URL、哪一页扣扳机、哪扇窗厨房没收。
5. **和 L-020 对照。** 导航 host 仍是 `getRouters` → 解析 componentKey → `addRoute`。workflow 不另写一套路由恢复。设计页 path 是种子里的 `design/index`，Vue 却 `push('/workflow/design/index')`——对表时两套都要能指认，不要只背其中一句。
6. **和 L-039 / 档案旁路对照。** 档案 runtime 的 `completeWorkflowTask` 把评论翻成 `workflowService.completeTask({ taskId, message, variables })`。同一座厨房，不是第十二键之外的第十三键菜单。
7. **以后若要 xmlString 预览。** 应喊 `runtime.service.getDefinitionXmlString(id)`，不要在 Vue 里手写路径，更不要启用 `legacyDefinitionXml`——那条 URL 后端没有。
8. **以后若要分类导出。** 先让厨房长出打 `POST /workflow/category/export` 的方法（或统一走 download 口），再把 `workflow:category:export` 补进权限组。只改种子解决不了——今天连枪都没有。
9. **以后若要按业务 id 删实例。** 先在 `WorkflowDefinitionService` 加方法对准 `deleteByBusinessIds`，再让页面喊它。不要让 Vue 直接拼那条 DELETE，也不要把 `deleteInstances` 的实例 id 数组说成业务 id。
10. **以后若只要「我的待办」瘦菜单。** 今天的工厂**会**把十二键一起注册。不能靠「只用 TaskListPage」让工厂少返回十一键。要瘦，得改工厂或让 App 不选那些菜单种子。
11. **换 App。** 第三份 App 若只要待办，仍须 `createWorkflowDefinitionService(该厅堂 http)` + 一份含 taskWaiting 键的 manifest + 能给 `designUrl`/`download` 的 runtime（即便暂时用不到）。把 admin 的 `workflowService` 单例跨 App 偷走，口试先数 L-007「组合可以复制，单例不能跨 App 偷」。
12. **要加一条新的 `/workflow/*`。** 先改 `src/index.ts` 的封面与工厂，再补 `index.test.ts`（最好像现有那样锁 URL，不要只锁编码）。若这条是 Warm-Flow UI 内部窗，不要做浏览器厨房方法。若这条需要嵌套抽屉，那是新形状。
13. **迁移口诀：** 先数工厂名 ≠ 导出名 → 再数六族 57 枪 + users 三 URL → 再数十二键顺序（myDocument 在 task 目录）→ 再数 `requireWorkflowWebRuntime` → 再数导出走 download、XML 鬼窗、请假三路出门、设计器 iframe → 最后数 `WorkflowService` 不在浏览器河。跳步会出现「把 Definition 说成只管图纸」「把 xmlString 说成页面预览」「把九厂一行盖章」「把 leave:edit 说成设计器真权限」。

## 常见误区

1. **「OBJ-73 只包含 `createWorkflowDefinitionService`。」** 原文两颗：厨房 + 菜单。
2. **「OBJ-73 包含 `FlwTaskController`。」** 那是 OBJ-70。
3. **「把九个 web-domain 工厂一行标 covered。」** 本课只给 `A:createWorkflowWebDomain`。
4. **「`createWorkflowService`。」** 磁盘没有。
5. **「工厂在 `transport.ts`。」** 在 `index.ts`。
6. **「页面自己拼 `/workflow/definition/list`。」** 厨房才拼。导出字符串是例外，走 download。
7. **「选人走 `notificationDirectory` 或厅堂 `systemService` 传入。」** 走工厂内 `users`。
8. **「`service.tasks.pageWaiting`。」** 方法名是 `pageTaskWaiting`。
9. **「分类页 = categoryTree。」** 分类页 = list + handleTree。
10. **「`definitionXml` 是 L-068 的第十四扇窗。」** Controller 没有。厨房测试仍打。
11. **「xmlString 吐 XML。」** 方法体 `exportJson`。
12. **「取消发布要单独的 unPublish 权限。」** 与发布共用 `workflow:definition:publish`。
13. **「设计器保存走 addDefinition。」** 走 iframe `/warm-flow/save-json`。
14. **「请假提交审批 = submitLeave。」** 提交审批是 add/update + `startWorkflow`；`submitLeave` 是「后端发起」按钮。
15. **「我发起的是任务分页。」** 是 `pageCurrentInstances`。
16. **「home 也能打开流程。」** 工作树没有。
17. **「capabilities 决定挂哪几页。」** 决定挂页的是 manifest + 菜单种子 + `selectedManifestIds`。
18. **「前端授权。」** 藏按钮 ≠ 授权。后端仍是最终授权者。
19. **「`requireWorkflowWebRuntime` 会检查 57 支枪。」** 只检查 runtime 对象在不在。
20. **「包 README 写『设计』所以厨房有 design 方法。」** 设计 URL 是宿主口。

## 非评分暂停

打开磁盘，不要凭记忆默写路径。不要改文件。没有标准答案栏，没有分数。

1. 打开 `frontend/packages/domains/workflow/src/index.ts`。圈 `createWorkflowDefinitionService`、`segment`、`users: createUserQueryPort(http)`、`importDefinition` 的 `repeatSubmit`、`legacyDefinitionXml` 与 `getDefinitionXmlString` 两行 URL。打开 `transport.ts`，确认没有工厂。
2. 打开 `src/index.test.ts`。圈 24 枪分类/图纸/SpEL 快照、路径 `%2F`、任务投影把 `instanceId` 变成字符串、`currentTaskUsers` 去掉手机号。在 freeze 对象上搜 `exportDef`，确认没有。
3. 打开 `frontend/packages/web-domains/workflow/src/index.ts` 与 `index.test.ts`。圈 id `web-domain-workflow`、十二键顺序、六组 permissions、`requireWorkflowWebRuntime`。核对 `componentKey` 不含 credential 那种第五键故事——流程的「隐藏键」是 design 与 leaveEdit。
4. 打开 `DefinitionPage.vue`、`DesignPage.vue`、`TaskListPage.vue`、`LeaveEditPage.vue`、`ProcessActionDialog.vue`、`UserSelect.vue`。圈各自真正喊出的 `runtime.service.*` 与 `runtime.download` / `designUrl`。确认没有 `legacyDefinitionXml`。
5. 打开 `apps/admin-web/src/application/services.ts` 第 52 行与 `router/adminManifestRegistry.ts` 第 87–139、366–369 行。圈 `workflowService`、`adminWorkflowWebRuntime`、`selectedManifestIds` 里的 `'web-domain-workflow'`。打开 home-web，确认搜不到这两个工厂。
6. 打开 `30-cde-workflow.sql` 菜单块。圈设计页 perms `workflow:leave:edit`、请假列表父 id `1761400000000000005`、`myDocument` 的 component `workflow/task/myDocument`。再打开 `FlwDefinitionController.java`，确认没有 `definitionXml` 映射、有 `exportDef` 与 `xmlString`。

## 总结、词汇表与下一步

- **宏观十二键流程厅：** `createWorkflowDefinitionService` 印六族 57 枪加一只人事室电话本；`createWorkflowWebDomain` 把十二张页订进 `web-domain-workflow`。墙上的菜 ≠ 厨房方法全集 ≠ 后端窗全集。
- **(a) `createWorkflowDefinitionService`：** 根 `index.ts` freeze；路径 `segment()` 编码；任务投影；`users` 嵌在工厂里。本课**不**把 Java 六份 Controller 再标一遍 covered。
- **(a) `createWorkflowWebDomain`：** 先 fail-closed 要 runtime，再冻十二键与六组 `workflow:*` 权限串，用 `h(page, { runtime })` 把厅堂的 `workflowService` 喂进页面。本课**不**把矩阵那一行九个工厂一起闭合。
- **页面实际调用：** 分类 list+树；图纸两抽屉+发布导入复制；SpEL 五枪；任务四 mode + 办理对话框；实例三份分页+变量作废；请假三路出门。导出走 download。设计器走 iframe。xmlString / definitionXml 没有按钮。按业务 id 的实例两窗没进厨房。
- **档案旁路不是第十三键。** 同一座厨房的 `completeTask`，从 profile runtime 进来。

词汇表：`createWorkflowDefinitionService` / `WorkflowDefinitionService` / `segment` / `projectWorkflowTaskTransport` / `UserQueryPort` / `workflowDomainModule` / `createWorkflowWebDomain` / `WorkflowWebRuntime` / `requireWorkflowWebRuntime` / `componentKey` / `web-domain-workflow` / `createDesignerController` / `enabledProcessButtons` / `submitLeave` vs `startWorkflow` / resource metadata / host port / `WorkflowService`（Java，非本厂）。

下一步：分类七扇是 OBJ-67。图纸发布/导入是 OBJ-68。实例大厅与跨模块合同是 OBJ-69。任务枢纽是 OBJ-70。SpEL 是 OBJ-71。请假 Java `submitAndFlowStart` 是 OBJ-72。厅堂插头是 OBJ-07。导航 host 是 OBJ-20。本课结束仍不是掌握证明；要练习请之后主动激活 Homework。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-005 | `frontend/apps/admin-web` | 厅堂 `workflowService`；registry 注入 runtime 与 designUrl；`selectedManifestIds` 含 `web-domain-workflow`；home 无此工厂 | `application/services.ts` 第 52 行；`router/adminManifestRegistry.ts` 第 87–139、366–369 行 | 2026-09-17 |
| S-006 | `frontend/packages/{domains,web-domains}/workflow` | 厨房 facade、投影、web-domain 十二键、runtime、子路径 export | 各包 `package.json` `exports` 与 `src` | 2026-09-17 |
| S-008 | 登记表 classic | `wta-workflow` 保持 classic；本课只对照不改层次 | engineering-standards `03-backend-module-modes.md` | 2026-09-17 |
| S-010 | `50-cde-base-dml.sql` | 「测试菜单」父 id `1761400000000000005`，请假列表挂在它下面 | 第 41 行附近 | 2026-09-17 |
| S-015 | `wta-api` `WorkflowService` | 跨模块点菜单不是厨房方法 | `org.namewta.workflow.api` | 2026-09-17 |
| S-L007-01 | L-007 厅堂组合 | 工厂名 ≠ 导出名；只吃 `domainHttp`；禁止 `createWorkflowService` | `lessons/L-007-admin-web-composition.md` | 2026-09-17 |
| S-L073-01 | `domains/workflow/src/index.ts`；`index.test.ts` | 57 枪 URL/动词；`segment` 编码；导入 header；users 嵌套；无 exportDef | `createWorkflowDefinitionService`；两个合同 `it` | 2026-09-17 |
| S-L073-02 | `domains/workflow/src/transport.ts`；`transport.test.ts` | 任务投影去 generated-only 字段；非法日期变 undefined | `projectWorkflowTaskTransport` | 2026-09-17 |
| S-L073-03 | `web-domains/workflow/src/index.ts`；`index.test.ts`；`runtime.ts`；`designer.ts` | 十二键顺序；六组权限；`requireWorkflowWebRuntime`；设计器 close 语义 | `createWorkflowWebDomain`；三个 `describe` | 2026-09-17 |
| S-L073-04 | `DefinitionPage.vue`；`DesignPage.vue`；`CategoryPage.vue`；`SpelPage.vue` | 两抽屉、导入 FormData、download 导出、iframe、list+handleTree | 模板按钮与 script | 2026-09-17 |
| S-L073-05 | `TaskListPage.vue`；`ProcessActionDialog.vue`；`process-actions.ts` 与测试 | 四 mode 换枪；办理 payload trim；干预按钮集 | `loaders`；`enabledProcessButtons` | 2026-09-17 |
| S-L073-06 | `InstancePage.vue`；`MyDocumentPage.vue`；`LeaveListPage.vue`；`LeaveEditPage.vue`；`runtime-actions.ts` | 实例两删除；我发起的；请假三路；天数 +1 | `persist`；`calculateLeaveDays` | 2026-09-17 |
| S-L073-07 | `UserSelect.vue`；`user-selection.ts` 与测试 | `users.list` / `departmentTree` / `options`；翻页保留已选 | `prepareUserSelection` | 2026-09-17 |
| S-L073-08 | `30-cde-workflow.sql`；`FlwDefinitionController.java`；`FlwInstanceController.java`；`FlwCategoryController.java` | 菜单十二 component；设计页 leave:edit；exportDef/xmlString 有、definitionXml 无；分类删除是单个 Long；实例 getInfo / deleteByBusinessIds 厨房没收 | 菜单 insert 第 260–314 行；Controller 映射 | 2026-09-17 |

## 文字等价物

本课所有 ASCII 图与对照表都可以用这段话代替：已经登录的管理员在 admin-web 打开流程菜单。导航用 `sys_menu.component` 去对 `createWorkflowWebDomain` 冻住的十二个 `componentKey`。页面只拿到 `WorkflowWebRuntime`。真正的 `/workflow/*` 字符串写在 `createWorkflowDefinitionService` 里，共六族 57 枪，外加嵌套的 `/system/user` 选人口。任务响应会投影。导出和设计器 iframe 不走这 57 枪。home 与 sso 没有这座厅。Java `WorkflowService` 是给邻居模块的点菜单，不是浏览器工厂的另一个名字。
