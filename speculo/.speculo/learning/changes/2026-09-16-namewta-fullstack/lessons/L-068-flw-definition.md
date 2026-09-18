---
lesson_id: L-068
objective_ids: [OBJ-68]
claimed_cells:
  - A:FlwDefinitionController.*
  - B:IFlwDefinitionService.publish,importJson,removeDef
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: method-table-on-disk
    minutes: 10
  - segment: publish-import-remove
    minutes: 10
  - segment: visuals-and-worked-examples
    minutes: 7
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 4
expression_level: eli5
coverage_depth: deep
source_ids: [S-004, S-008, S-010, S-015, S-L068-01, S-L068-02, S-L068-03, S-L068-04, S-L068-05, S-L068-06, S-L068-07, S-L068-08]
---

# Lesson 068：宏观图纸柜台——`FlwDefinitionController` 怎样发布、导入、撕掉一张流程图纸

## 学完你能做什么

打开 `backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/controller/FlwDefinitionController.java`，再打开旁边的 `IFlwDefinitionService` / `FlwDefinitionServiceImpl`，你能**口述这块宏观图纸柜台**：墙上挂的是「流程定义」菜谱，不是正在炒的那盘菜（实例），也不是厨师手里的那张工单（任务）。口试名单就是矩阵这两格，符号以**磁盘**为准：

1. **`A:FlwDefinitionController.*`**：同一门牌 `/workflow/definition` 上，**正好 13** 个 Java 公开方法。矩阵那一行把 13 个名字都写了，口试按磁盘，不要漏 `xmlString` / `active`，也不要发明 `definitionXml`。
2. **`B:IFlwDefinitionService.publish,importJson,removeDef`**：包装层真正加闸的三枪。发布先查中间节点办理人；导入把 JSON 读成 `DefJson` 再盖分类；删除先翻历史任务本。接口上还有 `queryList` / `unPublishList` / `exportDef`，那些是列表和导出，**不是** (b) 这一格。

你还能把三句话分清，不揉成「改流程」：

- **发布 ≠ 保存图纸。** `POST /` 是 `defService.checkAndSave`；`PUT /publish/{id}` 才走 `flwDefinitionService.publish`。没配办理人的中间节点，章盖不下去。
- **导入 ≠ XML。** 窗名叫 `importDef`，服务名叫 `importJson`，文件是 JSON。`GET /xmlString/{id}` 的方法体是 `defService.exportJson`。
- **删除 ≠ 取消发布。** 取消发布走引擎 `unPublish`，定义还在「未发布」抽屉（含失效）。删除走 `removeDef`：历史任务本上有字就撕不掉。

`wta-workflow` 在登记表是 **classic**：`Controller → ServiceImpl → Mapper`。本课房间多了一把**引擎钥匙** `DefService`（Warm-Flow 1.8.9），不是五层 UseCase。不要把这 13 扇窗口述成 layered，也不要把模块内的 `IFlwDefinitionService` 说成 `wta-api` 上的 `WorkflowService`（那是 L-069 的跨模块合同，管启动/办结/删实例）。

本课**不宣称**你会拆分类树（L-067）、实例与 `WorkflowService`（L-069）、任务枢纽（L-070）、SpEL（L-071）、请假示例流（L-072）、或浏览器 `createWorkflowDefinitionService` / `createWorkflowWebDomain`（L-073）。设计器 iframe 的 `WarmFlowController` 只作为邻居出现：它不是这 13 扇窗。

## 先把宏观地图放在桌上

L-003 已经把 `wta-workflow` 钉在 classic 列。L-002 说过它只进 **bundle-full**；`bundle-core` 的注释写明「不装配工作流引擎」。L-067 是左边那棵分类树：本课列表筛分类时会把父节点展开成「自己 + 子孙」再 `IN`。本课站在**已经登录、且 `warm-flow.enabled=true` 的管理端**这一头。

2026-09-17 工作树：类上 `@ConditionalOnEnable`（`warm-flow.enabled=true` 才进容器）+ `@Validated` + `@RequiredArgsConstructor` + `@RestController` + `@RequestMapping("/workflow/definition")`。**两把钥匙一起注入**：

```text
已登录的管理员（Admin-Token）
        │
        v
/workflow/definition/*          ← 一块门牌，一份 Java
        │
        ├─ IFlwDefinitionService / FlwDefinitionServiceImpl
        │     列表两抽屉、发布闸、导入 JSON、导出字节、删除前查历史任务
        │
        └─ org.dromara.warm.flow.core.service.DefService
              详情、新增校验保存、修改、取消发布、复制、导出 JSON 字符串、激活/挂起
```

| 你可能以为的名字 | 磁盘事实 |
| --- | --- |
| 13 扇窗都进 `IFlwDefinitionService` | **没有。** 7 扇直通 `DefService` |
| `IFlwDefinitionService` = `wta-api` 的 `WorkflowService` | **不是。** 前者在模块内；后者是跨模块启动/办结/删实例 |
| `xmlString` 吐 XML | **没有。** 方法体 `exportJson` |
| `unPublishList` 只有未发布 | **还有失效。** `isPublish in (0, 9)` |
| `definitionXml` 这一窗 | **Controller 没有。** 厨房测试仍打 `GET /workflow/definition/definitionXml/{id}` |
| 设计器保存走本 Controller | **没有。** iframe 打 Warm-Flow UI；XSS 排除 `/warm-flow/save-json` |
| `wta-workflow` 有 `src/test` | **2026-09-17 没有。** 本课证据是生产源码 + 前端契约/E2E，不是模块单测 |
| classic 所以没有引擎类型 | **有。** `FlowDefinition` / `FlowNode` / `FlowHisTask` 来自 `org.dromara.warm.flow.orm` |

菜单种子在 `30-cde-workflow.sql`（不是 `50-cde-base-dml.sql` 那份系统菜单）：C 型页 `workflow/processDefinition/index`，perms `workflow:definition:list`。F 型按钮九串：`query` / `add` / `edit` / `remove` / `export` / `import` / `publish` / `copy` / `active`。**没有**单独的 `unPublish` 权限——取消发布和发布共用 `workflow:definition:publish`。隐藏设计页 `workflow/processDefinition/design` 的 perms 却写成 `workflow:leave:edit`，那是邻居陷阱，本课不认它为本柜台授权。

表在 `30-cde-workflow.sql`：`flow_definition` / `flow_node` / `flow_skip` / `flow_instance` / `flow_task` / `flow_his_task`。`50-cde-base-dml.sql` 另有两套已发布种子：`profile_person_verification`、`profile_enterprise_verification`（`is_publish=1`）。它们证明「申请人」在库里是 **START（type 0）**，第一个中间节点才是复核。

**类比：** 这是厨房墙上的**菜谱抽屉**。已发布抽屉只放正在用的那一版（`isPublish=1`）。未发布抽屉放草稿和被换下来的旧版（0 和 9）。发布是盖「可以按这张菜谱炒」的章，但中间工位没写厨师名就盖不了。导入是把别人复印的 JSON 菜谱塞进来，并盖上你指定的分类章。删除是把菜谱撕掉——只要历史上有人按它做过一道菜（`flow_his_task`），就不许撕。

**类比失效处：**

1. 「菜谱」不是「正在炒的那盘」。实例、任务是 L-069 / L-070。本柜台不启动流程。
2. 「未发布抽屉」不是「只有草稿」。失效（9）也在这里。前端 TAB 文案写「未发布」，行上仍可能打红标「失效」。
3. 「申请人节点」不是 START。包装层问的是 `getFirstBetweenNode`：第一个 **BETWEEN（中间节点，type 1）**。
4. 「导入 XML」对不上磁盘。上传框 `accept` 写 `application/json,application/text`，文案写「仅支持 json」。
5. 「一张 Controller 等于一把 Service」。13 扇窗里 7 扇根本不进 `IFlwDefinitionService`。
6. `bundle-core` 没有这间房。开关关掉（`warm-flow.enabled` 不是 `true`）时，`@ConditionalOnEnable` 让 Controller 和 `FlwDefinitionServiceImpl` 都不会进容器。

## 核心概念与机制

### 直觉讲解

小孩子版只记十句：

1. **一块牌子，两把钥匙。** 找映射先数 13 个方法，再问每一枪是包装服务还是 `DefService`。
2. **`.*` 按磁盘数。** 13 个 Java 方法。矩阵名单齐；不要把前端厨房的 `legacyDefinitionXml` 算进 Controller。
3. **两只列表抽屉。** `GET /list` 只 `isPublish=1`。`GET /unPublishList` 是 `0` 和 `9`。权限都是 `workflow:definition:list`。
4. **详情不是 VO。** `GET /{id}` 返回 Warm-Flow 的 `Definition`，不是带 `categoryName` 的 `FlowDefinitionVo`。
5. **新增验格式，修改直接更新。** `add` → `checkAndSave`；`edit` → `updateById`。两枪都 `@Transactional` + `@RepeatSubmit`（默认 5000ms）。
6. **发布先点名。** 包装层把该定义的 `flow_node` 拉出来：中间节点（BETWEEN）除了「第一个中间节点」以外，`permissionFlag` 不能空白。过了才 `defService.publish(id)`。
7. **导入盖分类。** 文件字节 → `DefJson` → `setCategory(请求参数)` → `defService.importDef`。空文件会得到 `null` 再 NPE，不是那句「文件读取失败」。
8. **删除看历史本。** 有 `flow_his_task` 且还能查到定义，就 `ServiceException("流程定义【{}】已被使用不可被删除！")`。不查在途 `flow_task` / `flow_instance`。
9. **xml 是假名。** `xmlString` 和 `exportDef` 都向引擎要 JSON 字符串。导出响应 `Content-Type: application/text`，`Content-Disposition: attachment;`（没有文件名）。
10. **取消发布不走包装闸。** `PUT /unPublish/{id}` 直通 `defService.unPublish`。权限却仍是 `workflow:definition:publish`。`@Log` 的 `businessType` 在 publish / unPublish / copy 上都写成了 **INSERT**。

### 精确定义与 English term

| 中文 | English | 精确定义（本课，以工作树为准） |
| --- | --- | --- |
| 流程定义控制器 | `FlwDefinitionController` | classic `@RestController`，`@RequestMapping("/workflow/definition")`，继承 `BaseController`。13 个公开映射 |
| 流程定义包装服务 | `IFlwDefinitionService` | 模块内接口，6 个方法：`queryList` / `unPublishList` / `publish` / `exportDef` / `importJson` / `removeDef`。**不在** `wta-api` |
| 引擎定义服务 | `DefService` | Warm-Flow 核心服务。本 Controller 直通：`getById` / `checkAndSave` / `updateById` / `unPublish` / `copyDef` / `exportJson` / `active` / `unActive` |
| 跨模块工作流合同 | `WorkflowService` | `wta-api` 接口。启动、办结、删实例、变量。**本课不认** |
| 流程定义实体 | `FlowDefinition` | Warm-Flow ORM；表 `flow_definition` |
| 流程定义视图 | `FlowDefinitionVo` | 列表用。`categoryName` 靠 `@Translation(type = category_id_to_name)` |
| 发布状态 | `isPublish` | `0` 未发布、`1` 已发布、`9` 失效。VO 注释与 DDL 一致 |
| 激活状态 | `activityStatus` | `0` 挂起、`1` 激活。与发布是两列 |
| 中间节点 | BETWEEN / `NodeType.BETWEEN` | DDL：`node_type` `1` = 中间节点。`0` 开始、`2` 结束、`3` 互斥网关、`4` 并行网关 |
| 申请人节点编码 | `applyNodeCode` | `IFlwCommonService.applyNodeCode`：`FlowEngine.nodeService().getFirstBetweenNode`。空则 `ServiceException("流程定义缺少申请人节点，请检查流程定义配置")` |
| 办理人标识 | `permissionFlag` | `flow_node.permission_flag`。DDL：可多个、`@@` 分隔。发布闸只问是不是空白 |
| 发布 | `publish` | `PUT /publish/{id}` → 包装闸 → `defService.publish` |
| 取消发布 | `unPublish` | `PUT /unPublish/{id}` → `defService.unPublish`。无包装办理人闸 |
| 导入 | `importDef` / `importJson` | `POST /importDef` multipart：`file` + `category` |
| 导出文件 | `exportDef` | `POST /exportDef/{id}`，void，写响应流 |
| 导出 JSON 字符串 | `xmlString` | `GET /xmlString/{id}` → `R.data(defService.exportJson(id))` |
| 历史任务 | `FlowHisTask` | 表 `flow_his_task`。`removeDef` 的占用证据 |
| 条件装配 | `@ConditionalOnEnable` | `@ConditionalOnProperty("warm-flow.enabled"=true)`。Controller 与 `FlwDefinitionServiceImpl` 都打了 |
| 防重复提交 | `@RepeatSubmit` | 默认 5000ms。publish / unPublish / add / edit / copy / active 有；**list / remove / importDef / exportDef / xmlString / getInfo 没有** |
| 工作流失败 | `FlowException` | `FlowExceptionHandler` → `R.fail(e.getMessage())`。引擎抛的，不是包装层 `ServiceException` |

**`list` ≠ `unPublishList`。** 权限字符串碰巧都是 `workflow:definition:list`，过滤不是同一只。

**`publish` ≠ `unPublish`。** 权限碰巧都是 `workflow:definition:publish`。一个走包装闸，一个直通引擎。

**`importDef` ≠ `importJson` ≠ XML。** HTTP 方法名、服务方法名、文件格式是三张纸条。

**`xmlString` ≠ `exportDef`。** 一个塞进 `R.data` 的 JSON 文本（权限 `query`），一个是附件流（权限 `export`）。都向引擎要同一份 `exportJson`。

**`IFlwDefinitionService` ≠ `WorkflowService`。** 一个管图纸，一个管实例生命周期。

### 机制/因果链

#### A. `FlwDefinitionController.*`：磁盘上的 13 个公开方法

文件：`FlwDefinitionController.java`。注入：`DefService defService`、`IFlwDefinitionService flwDefinitionService`。

下面按源码出现顺序。**不要发明** `GET /definitionXml/{id}` 或 `POST /publish`。

| # | Java 方法 | HTTP | 权限 | 委托 | `@Log` / 事务 / 防重 | 口试要点 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | `list` | `GET /list` | `workflow:definition:list` | `flwDefinitionService.queryList` | 无 | 只已发布 |
| 2 | `unPublishList` | `GET /unPublishList` | `workflow:definition:list` | `flwDefinitionService.unPublishList` | 无 | `0` + `9` |
| 3 | `getInfo` | `GET /{id}` | `workflow:definition:query` | `defService.getById` | 无 | 返回 `Definition`，不是 Vo |
| 4 | `add` | `POST /` | `workflow:definition:add` | `defService.checkAndSave` | INSERT + RepeatSubmit + Transactional | 引擎格式校验 |
| 5 | `edit` | `PUT /` | `workflow:definition:edit` | `defService.updateById` | UPDATE + RepeatSubmit + Transactional | 无包装闸 |
| 6 | `publish` | `PUT /publish/{id}` | `workflow:definition:publish` | `flwDefinitionService.publish` | **INSERT** + RepeatSubmit；**Controller 无 Transactional**（服务有） | (b) |
| 7 | `unPublish` | `PUT /unPublish/{id}` | `workflow:definition:publish` | `defService.unPublish` | **INSERT** + RepeatSubmit + Transactional | 不进包装 |
| 8 | `remove` | `DELETE /{ids}` | `workflow:definition:remove` | `flwDefinitionService.removeDef` | DELETE；无 RepeatSubmit；Controller 无事务（服务有） | `toAjax` → `R<Void>` |
| 9 | `copy` | `POST /copy/{id}` | `workflow:definition:copy` | `defService.copyDef` | INSERT + RepeatSubmit + Transactional | 直通引擎 |
| 10 | `importDef` | `POST /importDef` | `workflow:definition:import` | `flwDefinitionService.importJson` | IMPORT；无 RepeatSubmit；Controller 无事务（服务有） | (b) |
| 11 | `exportDef` | `POST /exportDef/{id}` | `workflow:definition:export` | `flwDefinitionService.exportDef` | EXPORT；方法 `void` | 附件流 |
| 12 | `xmlString` | `GET /xmlString/{id}` | `workflow:definition:query` | `defService.exportJson` | 无 | `R.data`，JSON 文本 |
| 13 | `active` | `PUT /active/{id}?active=` | `workflow:definition:active` | `active? defService.active : defService.unActive` | UPDATE + RepeatSubmit + Transactional | 布尔查询参数 |

列表共用 `buildQueryWrapper`：`flowCode` / `flowName` 模糊；`category` 非空时 `flwCategoryMapper.selectCategoryIdsByParentId`（子孙 + **自己**）再 `IN`，ID 转成字符串；`orderByDesc(createTime)`。然后：

- `queryList` 再 `eq(isPublish, PUBLISHED)`。
- `unPublishList` 再 `in(isPublish, UNPUBLISHED, EXPIRED)`。

`BeanUtil.copyToList(..., FlowDefinitionVo.class)`。分类名不在 SQL 里填，靠翻译器 `CategoryNameTranslationImpl`（`FlowConstant.CATEGORY_ID_TO_NAME`）。

#### B. `publish`：盖章前先点中间工位的厨师名

Controller：`PUT /publish/{id}`。**没有**类方法级 `@Transactional`。服务：

```text
flowNodeMapper.selectList(definitionId = id)
        │
        ├─ 节点列表空 → 跳过 NAMEWTA 闸，仍调用 defService.publish(id)
        │
        └─ 有节点
              applyNodeCode = flwCommonService.applyNodeCode(id)
                    └─ getFirstBetweenNode；空 → 「流程定义缺少申请人节点…」
              对每个 FlowNode：
                    permissionFlag 空白
                    且 nodeCode ≠ applyNodeCode
                    且 nodeType == BETWEEN
                    → 把 nodeName 放进 errorMsg
              errorMsg 非空 → ServiceException("节点【{}】未配置办理人!", 逗号拼接名字)
              否则 defService.publish(id)
```

短路顺序以源码为准：先问空白，再问是不是申请人节点，再问是不是 BETWEEN。START / END / 网关空白办理人**不会**进名单。申请人节点（第一个 BETWEEN）空白办理人也**不会**进名单。

种子对照（`50-cde-base-dml.sql` 个人实名）：

| node_code | node_type | 名字 | permission_flag | 发布闸会不会点名 |
| --- | --- | --- | --- | --- |
| `person_apply` | 0 START | 提交申请 | null | 否（不是 BETWEEN） |
| `person_review` | 1 BETWEEN | 人工复核 | `role:1761300000000000001` | 它就是 `applyNodeCode`；即使空白也放过。种子其实写了角色 |
| `person_finish` | 2 END | 复核完成 | null | 否 |

口试不要说「START 叫申请人所以 publish 检查它」。包装层问的是第一个 BETWEEN。

页面确认文案写「发布后会将已发布流程定义改为失效」。那句是前端 `DefinitionPage.handlePublish` 的提示；NAMEWTA 包装层**自己不改**别人的 `isPublish`。旧版怎样变成 9，是 `defService.publish` 的引擎活。本课认「章是引擎盖的；办理人闸是 NAMEWTA 加的」。

#### C. `importJson`：JSON 菜谱 + 分类章

Controller 参数是 `MultipartFile file, String category`（不是 `@RequestBody`）。前端 `FormData` 追加 `file` 和 `category`；顶级分类 `ALL` 在页面 `before-upload` 就拦下。

服务：

1. `JsonUtils.parseObject(file.getBytes(), DefJson.class)`。空字节返回 `null`（`JsonUtils` 如此），下一行 `defJson.setCategory` **NPE**。
2. 非空则 `setCategory(category)`——文件里自带的分类被请求参数盖掉。
3. `defService.importDef(defJson)`。
4. `catch (IOException)` 才变成 `IllegalStateException("文件读取失败，请检查文件内容")`。Jackson 解析失败走 `JsonUtils` 的运行时异常，**不是**这句中文。
5. 没抛错就 `return true`。Controller `R.ok(true)`。

前端厨房给这枪加了 `headers: { repeatSubmit: false }`；**后端方法上没有** `@RepeatSubmit`。不要把请求头说成服务端注解。

#### D. `removeDef`：历史本上有字就不许撕

Controller：`DELETE /{ids}`，`List<Long>`，`toAjax(boolean)` → 成功 `R.ok()` 无 data。

服务：

1. `flowHisTaskMapper.selectList(definitionId IN ids)`。
2. 有历史行 → 用这些行的 `definitionId` 再 `flowDefinitionMapper.selectByIds`。
3. 定义还在 → `ServiceException("流程定义【{}】已被使用不可被删除！", flowCode 拼接)`。日志同一句。
4. 历史行在、定义已经查不到 → **不抛**，继续删。这是磁盘分支，不是推荐 Target。
5. `defService.removeDef((List<Long>) ids)`。HTTP 进来的是 `List`，强转成立。若将来有人用非 List 的 `Collection` 调服务，会 `ClassCastException`。
6. 引擎异常被包成 `RuntimeException("Failed to remove flow definitions")`——英文，和上面那句中文占用提示不是同一条路。
7. `return true`。

占用证据**只有** `flow_his_task`。口试不要说「有在途任务 / 有实例就不能删」——那两张表本方法没查。实例怎么删是 L-069。

#### E. 导出、激活、设计器邻居（认边界，不扩格子）

`exportDef`：`exportJson` → UTF-8 字节 → `response.reset()` → `application/text` → `attachment;`（无 filename）→ `IoUtil.write(..., false, data)`。页面实际用 `runtime.download('/workflow/definition/exportDef/{id}', {}, '{flowCode}.json')` 自己补文件名。

`active`：查询参数 `boolean active`。真激活、假挂起。与 `isPublish` 独立：已发布仍可挂起。

设计器：`DesignPage.vue` 是 iframe，`runtime.designUrl`。`WarmFlowDefinitionResponseAdvice` 只切 `WarmFlowController.queryDef`，给历史节点补 `nodeRatio="0"`。**不是** `xmlString` 的 Advice。安全排除 `/warm-flow-ui/config`；XSS 排除 `/warm-flow/save-json`。本课 13 扇窗不保存节点坐标。

前端厨房另留 `legacyDefinitionXml` → `GET /workflow/definition/definitionXml/{id}`。2026-09-17 **没有**对应 Java 映射。Vue 定义页不喊它；契约测试仍锁这条 URL。口试：厨房遗留 ≠ Controller 有窗。

## 图、表或文本图

**图 1：宏观图纸柜台的两把钥匙**

```text
 浏览器 / DefinitionPage（L-073 才认页面）
        │
        v
 GET/POST/PUT/DELETE  /workflow/definition/*
        │
        v
 FlwDefinitionController     @ConditionalOnEnable
        │
        ├─ 包装 IFlwDefinitionService
        │     list / unPublishList / publish / importDef / exportDef / remove
        │           │
        │           ├─ FlowDefinitionMapper / FlowNodeMapper / FlowHisTaskMapper   （Warm-Flow ORM）
        │           ├─ FlwCategoryMapper.selectCategoryIdsByParentId               （NAMEWTA）
        │           ├─ IFlwCommonService.applyNodeCode
        │           └─ 仍可能再调 DefService.publish / importDef / removeDef / exportJson
        │
        └─ 引擎 DefService（直通）
              getInfo / add / edit / unPublish / copy / xmlString / active
```

**图题 / caption：** 宏观同一门牌、两把钥匙。alt：13 扇窗里 6 扇先包装、7 扇直通引擎。

**文字等价物：** 管理员只看见 `/workflow/definition`。列表、发布、导入、导出文件、删除先经过 NAMEWTA 包装：包装层负责抽屉过滤、分类展开、办理人闸、JSON 读入、历史占用。详情、新增校验、修改、取消发布、复制、JSON 字符串、激活直接叫 Warm-Flow。不要把「有 IFlwDefinitionService」说成「引擎从不露脸」，也不要把包装服务说成 api 合同。

**图 2：发布闸怎么点名**

```text
PUT /publish/{id}   权限 workflow:definition:publish
        │
        v
读 flow_node where definition_id = id
        │
        ├─ 0 行 ──────────────────────────────► defService.publish
        │
        └─ ≥1 行
              getFirstBetweenNode → applyNodeCode
                    空 → 缺少申请人节点（在有节点时也会炸）
              每个 BETWEEN：
                    空白 permissionFlag 且不是 applyNodeCode → 记 nodeName
              有名字 → 节点【A,B】未配置办理人
              无名字 → defService.publish
```

**图题 / caption：** NAMEWTA 闸只拦「非申请人的中间节点没办理人」。alt：START/END/网关不查；空节点列表跳过闸。

**文字等价物：** 发布像给菜谱盖「可以炒」的章。盖章前，包装层数中间工位：第一个中间工位允许暂时没写厨师名；后面的中间工位必须写。开始/结束/路口牌子不查。抽屉里一张空白纸（没有任何节点）会跳过点名，章还是交给引擎。缺第一个中间工位时，点名开始就会说「缺少申请人节点」。

**图 3：导入盖分类、删除翻历史本**

```text
POST /importDef  file + category     DELETE /{ids}
        │                                 │
        v                                 v
 bytes → DefJson                         flow_his_task
 setCategory(参数盖掉文件内分类)           │
 defService.importDef                    ├─ 有行且定义还在 → 不可删除（flowCode）
 return true                             ├─ 有行但定义查不到 → 继续
                                         └─ defService.removeDef
```

**图题 / caption：** 导入是 JSON+分类章；删除只认历史任务本。alt：空文件 NPE；占用提示中文；引擎失败英文 RuntimeException。

**文字等价物：** 导入像把复印件塞进抽屉，分类章用你在树上点的那一格，不信文件里写的。删除像撕菜谱：只要历史本上记过按它做过的菜，就不许撕。在途任务表、实例表这枪没翻。空复印件（0 字节）不是「读取失败」那句客气话，是空对象后面的空指针。

**图的边界：** 不画 `WorkflowService.startWorkFlow`（L-069）。不画 LiteFlow 办理链（L-070）。不保证引擎 `publish` 如何把旧版打成 9——那句以页面文案和引擎为准，包装层无此 SQL。不把 `createWorkflowDefinitionService` 标 covered。

## 正例、反例与边界

**正例 1 — 已发布抽屉。** 有 `workflow:definition:list` 的人打开定义页默认 TAB「已发布」。`GET /workflow/definition/list?flowName=&flowCode=&category=&pageNum=1&pageSize=10`。服务再加 `isPublish=1`。种子两套实名认证会出现在这里。

**正例 2 — 未发布抽屉含失效。** TAB「未发布」打 `GET /unPublishList`。行上 `isPublish==0` 红标「未发布」，其它非 1 红标「失效」。发布按钮条件是 `isPublish !== 1`，所以失效行也能再点发布。

**正例 3 — 分类树展开。** 点某个分类节点，`category` 变成该 ID。包装层 `selectCategoryIdsByParentId` 把子孙和自己一起 `IN`。点根「0」时页面把 `category` 清空，**不加**分类条件。

**正例 4 — 发布中间节点。** 草稿有 START、BETWEEN「审批」、END。审批节点 `permissionFlag` 空白且它不是第一个 BETWEEN → `节点【审批】未配置办理人!`。配上 `role:…` 或用户存储 id 后再 `PUT /publish/{id}`，包装层放行，引擎盖章。页面成功后把 TAB 切回「已发布」再 `list`。

**正例 5 — 申请人中间节点允许空白。** 第一个 BETWEEN 叫「填写申请」、`permissionFlag` 空；后面「经理审」写了办理人。发布通过包装闸。不要要求填写申请也必须有 `permissionFlag`。

**正例 6 — 导入 JSON。** 左侧选中非顶级分类，上传本项目导出的 `.json`。`POST /importDef` multipart。服务把 `category` 盖进 `DefJson`。页面切到未发布 TAB。

**正例 7 — 导出附件。** 选中一行，`POST /exportDef/{id}`。响应体是 JSON 字节。页面文件名用 `flowCode.json`。

**正例 8 — 占用不可删。** 该定义跑过任务，`flow_his_task` 有行。`DELETE /{id}` → 中文占用异常。取消发布不能替代删除闸。

**正例 9 — 复制直通引擎。** `POST /copy/{id}` 不进包装。页面成功后切未发布 TAB。

**反例 1 — 「13 扇窗都 `flwDefinitionService.xxx」。」** `getInfo` / `add` / `edit` / `unPublish` / `copy` / `xmlString` / `active` 七扇直通 `defService`。

**反例 2 — 「`unPublishList` 等于 `isPublish=0`。」** 还有 `9`。

**反例 3 — 「`xmlString` 返回 BPMN XML。」** `exportJson`。

**反例 4 — 「导入走 `@RequestBody DefJson`。」** multipart 文件。

**反例 5 — 「发布权限和取消发布权限不同。」** 都是 `workflow:definition:publish`。菜单 F 型一行就叫「流程定义发布/取消发布」。

**反例 6 — 「删除会查 `flow_instance` / `flow_task`。」** 只查 `flow_his_task`。

**反例 7 — 「`applyNodeCode` 是 START 节点编码。」** 第一个 BETWEEN。种子里 START 叫「提交申请」，第一个 BETWEEN 叫「人工复核」。

**反例 8 — 「`IFlwDefinitionService` 在 `wta-api`。」** 在 `org.namewta.workflow.service`。跨模块请用 `WorkflowService`（L-069）。

**反例 9 — 「Controller 有 `GET /definitionXml/{id}`。」** 没有。那是前端厨房遗留。

**反例 10 — 「设计器保存打 `PUT /workflow/definition`。」** `edit` 改的是定义头（编码/名称/分类/ext…）。节点图画在 Warm-Flow UI。

**反例 11 — 「空 JSON 文件会提示文件读取失败。」** 0 字节 → `parseObject` 返回 null → NPE。`IOException` 那句只包 `file.getBytes()`。

**反例 12 — 「本模块有单测钉死办理人闸。」** 2026-09-17 `wta-workflow` **没有** `src/test`。闸在 `FlwDefinitionServiceImpl.publish` 源码里。

**反例 13 — 「`publish` 的 `@Log` 是 UPDATE。」** 源码 `BusinessType.INSERT`。unPublish、copy 同样写成 INSERT。口试报事实。

**反例 14 — 「把 `createWorkflowDefinitionService` 标 covered。」** 那是 OBJ-73。本课只给 Java 柜台和包装三闸。

**边界 1 — 开关与装配。** `application.yml` 默认 `warm-flow.enabled: true`。LiteFlow `enable` 跟随同一把开关。关掉后本 Controller 不进容器，13 扇窗全部 404/不存在，不是 403。

**边界 2 — bundle-core 没有这间房。** 档案模块仍在 core；流程引擎不在。不要口述「core 也能发流程定义」。

**边界 3 — 空节点仍可把 id 交给引擎 publish。** NAMEWTA 闸跳过 ≠ 引擎一定成功。引擎失败变 `FlowException` → `R.fail(message)`。

**边界 4 — 有节点但没有 BETWEEN。** `applyNodeCode` 先炸「缺少申请人节点」，轮不到办理人名单。

**边界 5 — `permissionFlag` 格式。** 闸只认空白。`@@` 分隔、`role:` 前缀是表注释和种子写法；本方法不解析。

**边界 6 — 导入分类。** 服务不校验 category 是否存在。页面拦顶级；直接打 HTTP 仍可能把奇怪字符串写进 `DefJson`。

**边界 7 — `remove` 的 ids。** 路径变量 `List<Long>`。前端厨房 `deleteDefinition` 会 `encodeURIComponent` 后再用逗号拼接。

**边界 8 — 前端 TAB 索引。** `handleClick` 把 `tab.index` 赋给 `activeName`（`'0'` 已发布 / `'1'` 未发布）。发布成功强制 `'0'`；导入/新增/复制/取消发布走 `'1'`。

**边界 9 — `getInfo` 与列表 VO。** 改弹窗 `getDefinition` 打的是 `GET /{id}`，没有 `categoryName` 翻译字段也正常；分类靠表单自己的树。

**边界 10 — 动态表单单选在页上把 Y 禁用了。** 那是 Vue；本 Controller 的 `add`/`edit` 仍把 body 交给引擎。不要把页面禁用说成后端拒 Y。

## 变式与迁移

1. **和 L-067 对照。** 分类树是另一份 Controller。本课只消费 `selectCategoryIdsByParentId` 做列表过滤，以及导入时的 category 字符串。不要在本课把分类 CRUD 再讲一遍。
2. **和 L-069 对照。** `WorkflowService` 按 **业务 id** 启停实例。本课按 **定义 id** 管图纸。删定义看历史任务；删实例是另一扇窗。
3. **和 L-070 对照。** 办理人标识真正在任务里怎么变成用户，走 `WorkflowPermissionHandler.convertPermissions`。本课发布闸只问「中间节点写没写」。没发布成功，后面的任务柜台没有这张菜谱。
4. **和 L-071 对照。** SpEL 是表达式目录，不是定义节点。不要把 `permissionFlag` 说成 SpEL。
5. **和 L-072 对照。** 请假 `submitAndFlowStart` 消费的是**已经发布**的定义。本课是那张菜谱怎么上台。
6. **和 L-073 对照。** 厨房 `createWorkflowDefinitionService` 把 13 扇窗（外加分类/SpEL/任务）收成方法名。页面 `DefinitionPage` 才扣扳机。本课不把厨房标 covered。记住厨房多了一条没有后端窗的 `legacyDefinitionXml`。
7. **和 L-003 / 登记表对照。** classic 允许 `ServiceImpl` 持 Mapper。本课还持了引擎 `DefService` 和三张 Warm-Flow Mapper。不要借机改 layered。
8. **和 L-034 / L-040 对照。** 档案审核种子已经是 `is_publish=1` 的定义。它们走 profile 的 `workflow.start` / `terminate`，不打本课的 publish HTTP。
9. **以后若要「未发布 TAB 不含失效」。** 今天的 `unPublishList` **会**把 9 放进来。要瘦，改包装查询，不要改前端文案假装。
10. **以后若要删定义同时拦在途实例。** 今天必须加查询；现闸只有历史任务。
11. **换 App。** home-web 没有这份菜单工厂。第三份 App 若只要只读已发布列表，仍须打 `GET /list`，权限仍是 `workflow:definition:list`。
12. **迁移口诀：** 先数 13 扇窗和两把钥匙 → 再数已发布/未发布+失效两抽屉 → 再走 publish 点名（第一个 BETWEEN ≠ START）→ 再走 importJson 盖分类 → 再走 removeDef 只翻历史本 → 最后把 xml 假名、设计器邻居、厨房遗留 URL 放在门外。跳步会出现「把 WorkflowService 当定义服务」「把失效当成已发布」「把 START 当申请人」。

## 常见误区

1. **「OBJ-68 包含 `createWorkflowDefinitionService`。」** 那是 OBJ-73。
2. **「OBJ-68 包含 `FlwCategoryController`。」** 那是 OBJ-67。
3. **「OBJ-68 包含 `WorkflowService.startWorkFlow`。」** 那是 OBJ-69。
4. **「13 扇全进包装服务。」** 只有 6 扇 HTTP 调它；(b) 口试再收成 publish / importJson / removeDef 三枪。
5. **「`importDef` 服务方法也叫 importDef。」** 服务叫 `importJson`。
6. **「`xmlString` 是 XML。」** JSON。
7. **「未发布列表没有失效。」** 有。
8. **「发布和取消发布权限不同。」** 同一串。
9. **「申请人 = START。」** 第一个 BETWEEN。
10. **「删除看实例表。」** 看 `flow_his_task`。
11. **「本课要把九个 web-domain 工厂一行盖章。」** 本课不碰那一行。
12. **「classic 所以没有 Warm-Flow 类型。」** 实体和 `DefService` 就是引擎的。
13. **「`bundle-core` 也能发定义。」** 模块不在 core。
14. **「前端授权。」** 藏按钮 ≠ 授权。后端仍是最终授权者。
15. **「`@Log INSERT` 表示 publish 插入一行定义。」** 那是审计业务类型枚举写错了常规直觉；定义早已存在，publish 改发布状态（引擎内部还可能让旧版失效）。
16. **「设计页 perms `workflow:leave:edit` 是定义设计权限。」** 种子如此；本柜台设计按钮实际核 `workflow:definition:query`。

## 非评分暂停

打开磁盘，不要凭记忆默写路径。不要改文件。没有标准答案栏，没有分数。

1. 打开 `FlwDefinitionController.java`。用手指点 13 个映射。把 6 扇包装和 7 扇 `defService` 分成两列。圈 publish 的 `@Log INSERT`、Controller 上没有 `@Transactional`。圈 `xmlString` 的 `exportJson`。确认没有 `definitionXml`。
2. 打开 `IFlwDefinitionService.java`。确认接口只有 6 个方法，(b) 是其中 `publish` / `importJson` / `removeDef`。不要把 `queryList` 说成 (b)。
3. 打开 `FlwDefinitionServiceImpl.publish`。圈 BETWEEN、空白 `permissionFlag`、`applyNodeCode`。打开 `FlwCommonServiceImpl.applyNodeCode`，圈 `getFirstBetweenNode`。打开 `50-cde-base-dml.sql` 个人实名三节点，核对 type 0/1/2。
4. 打开 `importJson` / `removeDef`。圈 `setCategory`、`IOException` 文案、空字节 NPE 路径、`flow_his_task`、中文占用、英文 `RuntimeException`、`(List<Long>) ids`。
5. 打开 `30-cde-workflow.sql` 的 `flow_definition.is_publish` 注释和菜单 F 型九串。打开 `DefinitionPage.vue` 的两个 TAB、发布确认文案、导入 `FormData`、导出 `runtime.download`。打开厨房 `legacyDefinitionXml`，确认 Vue 不喊、Controller 无窗。
6. 打开 `03-backend-module-modes.md` 的 workflow 行、`wta-admin/pom.xml` 的 bundle-full、`ConditionalOnEnable`、`application.yml` 的 `warm-flow.enabled`。确认不要口述 layered。

## 总结、词汇表与下一步

- **宏观图纸柜台：** 一块 `/workflow/definition` 牌子，13 扇 classic 窗，两把钥匙（NAMEWTA 包装 + Warm-Flow `DefService`）。墙上是菜谱，不是炒到一半的菜。
- **(a) `FlwDefinitionController.*`：** 磁盘 13 法。两只列表抽屉（1 vs 0+9）、详情走引擎实体、新增 `checkAndSave`、修改 `updateById`、发布走包装、取消发布直通、删除 `toAjax`、复制直通、导入 multipart、导出附件流、`xmlString` 其实是 JSON、激活靠布尔查询参数。
- **(b) 三闸：** `publish` 点非申请人中间节点的办理人；`importJson` 读 JSON 并盖请求分类；`removeDef` 只认历史任务占用。
- **假名与邻居：** xml 不是 XML；厨房 `definitionXml` 没有后端窗；设计器 iframe 不是这 13 扇；`WorkflowService` 不是这张接口。

词汇表：`FlwDefinitionController` / `IFlwDefinitionService` / `DefService` / `FlowDefinitionVo` / `isPublish` 0·1·9 / `activityStatus` / BETWEEN / `applyNodeCode` / `permissionFlag` / `importJson` / `removeDef` / `flow_his_task` / `@ConditionalOnEnable` / classic / `WorkflowService`（非本课）。

下一步：L-069 才把实例和跨模块 `WorkflowService` 放上桌。L-070 才是启动/完成/驳回。L-071 是 SpEL 目录。L-072 是请假怎样提交并拉起一张已发布定义。L-073 才把厨房方法名对着这些 HTTP 念完。本课结束仍不是掌握证明；要练习请之后主动激活 Homework。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-004 | `backend/wta-modules/**` Controller | 业务模块公开入口；本课类在 `wta-workflow` | `controller/FlwDefinitionController.java` | 2026-09-17 |
| S-008 | `.agents/skills/engineering-standards/references/project/03-backend-module-modes.md` | `wta-workflow` = classic；经公开 Workflow API 接入，不改内部层次 | 登记表 classic 行 | 2026-09-17 |
| S-010 | `release-artifacts/docker/infrastructure/mysql/init/50-cde-base-dml.sql` | 两套已发布实名定义种子；START vs 第一个 BETWEEN | `flow_definition` / `flow_node` 210060… 块 | 2026-09-17 |
| S-015 | `backend/wta-api/.../workflow/api/WorkflowService.java` | 跨模块合同不是本包装接口 | 启动/办结/删实例方法名单 | 2026-09-17 |
| S-L068-01 | `FlwDefinitionController.java` | 13 个公开映射；两把钥匙；publish 无 Controller 事务；xmlString→exportJson；Log INSERT | 类上 `/workflow/definition` 及各方法注解 | 2026-09-17 |
| S-L068-02 | `IFlwDefinitionService.java`；`FlwDefinitionServiceImpl.java` | 6 方法接口；(b) 三闸；列表过滤 1 vs 0+9；分类展开；导出 `application/text` | `queryList` / `publish` / `importJson` / `removeDef` / `exportDef` | 2026-09-17 |
| S-L068-03 | `FlwCommonServiceImpl.applyNodeCode`；`FlowDefinitionVo`；`ConditionalOnEnable` | 第一个 BETWEEN；isPublish 注释；`warm-flow.enabled=true` 才装配 | `getFirstBetweenNode`；VO `isPublish`；注解 havingValue | 2026-09-17 |
| S-L068-04 | `30-cde-workflow.sql` | 表结构 node_type 0–4；is_publish 0/1/9；定义菜单与 F 型九串；设计页 perms 写成 leave | `flow_definition`/`flow_node`；menu `1761400000000011620` 及 1644–1652、1700 | 2026-09-17 |
| S-L068-05 | `wta-admin/pom.xml` bundle-full；`application.yml` `warm-flow`；`FlowExceptionHandler`；`WarmFlowDefinitionResponseAdvice` | 模块只进 full；开关；引擎异常；Advice 只切设计器 queryDef | bundle-full 依赖；yml 408 行附近；handler 包 | 2026-09-17 |
| S-L068-06 | `JsonUtils.parseObject(byte[])`；`BaseController.toAjax(boolean)`；`RepeatSubmit` 默认 5000ms | 空字节返回 null；remove 成功无 data；哪些窗有防重 | common-json / common-web / common-redis | 2026-09-17 |
| S-L068-07 | `frontend/packages/domains/workflow/src/index.ts` 与 `index.test.ts` | 厨房 URL 与 13 窗对齐；另锁 `definitionXml` 遗留；import `repeatSubmit: false` | `createWorkflowDefinitionService` 定义段 | 2026-09-17 |
| S-L068-08 | `web-domains/workflow/src/definition/DefinitionPage.vue`；`DesignPage.vue`；`e2e/workflow-definition.spec.ts` | 两 TAB；发布/导入/导出扣扳机；iframe 设计器；E2E 打 `/publish/{id}` 与 `/importDef` | `handlePublish` / `handlerImportDefinition` / `runtime.download` | 2026-09-17 |
