---
lesson_id: L-075
objective_ids: [OBJ-75]
claimed_cells:
  - A:TestTreeController.list,export,getInfo,add,edit,remove
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: six-doors-on-disk
    minutes: 10
  - segment: classic-chain-and-gates
    minutes: 11
  - segment: visuals-and-worked-examples
    minutes: 6
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 4
expression_level: eli5
coverage_depth: deep
source_ids: [S-004, S-006, S-007, S-008, S-010, S-L075-01, S-L075-02, S-L075-03, S-L075-04, S-L075-05, S-L075-06, S-L075-07, S-L075-08, S-L075-09]
---

# Lesson 075：宏观六扇树表窗——TestTreeController 的扁列表与父子

## 学完你能做什么

打开 `backend/wta-modules/wta-demo/src/main/java/org/namewta/demo/controller/TestTreeController.java`，你能**口述六扇公开窗**：谁回扁列表、谁用 GET 印 Excel、谁按主键看一行、谁增、谁改、谁按逗号删。口试名单就是矩阵 **(a)** 这一格，符号以**磁盘**为准：

**`A:TestTreeController.list,export,getInfo,add,edit,remove`**：同一门牌 `/demo/tree` 上，**正好 6 个** Java 公开方法。chain 把六个名字写全了，不是「树 CRUD 四个字母」，也不是「单表八扇再加 parentId」。口试按磁盘，不要发明 `/page`、`importData`、`treeSelect`、`excludeChild`、`categoryTree`，也不要把厨房五枪或 `buildTree` 背成第七扇 REST。

OBJ-75 原文只要你能口述 `TestTreeController` 树表。本课还要把 **classic 抽屉 + 扁列表拼树**讲完，否则「能口述」会退化成背路径：

- 登记表：`wta-demo` 是 **classic**。调用链是 **Controller → `ITestTreeService` → `TestTreeServiceImpl`（自己抱 `TestTreeMapper`）→ `BaseMapperPlus` 默认方法**。`TestTreeMapper.xml` 是**空壳**。没有 UseCase，没有 DAO。
- 登记表旁注：**「示例和集成演示；保持现有示例可运行；不得作为新模块 layered 反例。」** 本课是存量样板的**第二扇成熟切片**，仍然**不是**抄作业模板。
- 六扇都贴 `@SaCheckPermission`。没有 `@SaIgnore`，没有 `@ConditionalOnEnable`。关模块靠 **bundle**，不是运行时电闸。
- 失败信封主要是 `R.fail`（`toAjax` 为 false）。本课**没有**分类课那种 `R.warn` 601，也**没有**单表删除那种「您没有删除权限!」短路。
- HTTP **不**返回树。`TestTreeVo` **没有** `children`。页面上的折叠表是浏览器 `buildTree(id, parentId)` 拼的。

本课**不宣称**你会拆 `TestDemoController` 八扇（L-074 已讲）、`TestRichTextController` 与 OSS 资产（L-076）、或把 `createDemoService` / `createDemoWebDomain` 当本格 covered（L-077）。厨房 URL 只当**对照**：证明六扇窗里哪些被浏览器扣了扳机，哪些种子有、厨房和页面没收。Redis/MQTT/MCP/Excel 模板/ES/WebSocket/限流/加密/敏感/SaToken 文档/邮件短信/队列那些 Controller 仍是矩阵 **deferred(capability-demo-not-product-room)**：模式跟本课同一间 classic 房间，格子不标它们 covered。

2026-09-17 工作树先钉死**包边界**（口试先数窗，再数链）：

| 你可能以为的名字 | 磁盘事实 |
| --- | --- |
| 第七扇 HTTP `page` / `importData` | **没有。** 那是单表 L-074 的两扇。树表列表不分页，没有导入窗 |
| `POST /export` | **没有。** 树表导出是 **`GET /export`**。单表才是 POST |
| 后端返回带 `children` 的树 | **没有。** `list` 回 `R<List<TestTreeVo>>`，Vo 只有扁字段 |
| `GET /treeSelect` 或 `excludeChild` | **没有。** 部门房间才有同类窗（L-017）；分类才有 `categoryTree`（L-067） |
| wrapper 按 `parentId` 过滤 | **没有。** `buildQueryWrapper` 只用 `deptId` / `userId` / `treeName`，再 `orderByAsc(id)` |
| 厨房 `exportTree` | **没有。** `createDemoService` 只有 list/get/add/update/delete 五枪树方法 |
| web-domain 权限组含 `query` / `export` | **没有。** 组里四串：list/add/edit/remove |
| `TestTreeMapper.xml` 有树 SQL | **没有。** 空 `<mapper>`，连 `SELECT *` 都没有 |
| `deleteWithValidByIds` 会点名比大小 | **没有。** `isValid` 分支是空 TODO；真正的过滤若发生，靠类上数据权限改写 DELETE |
| `TestTreeBo` / `TestTreeVo` 有 `version` | **没有。** 实体和 DDL 有乐观锁列；HTTP 进出都不带版本 |
| 给树房间加 UseCase/DAO | **本课不发动。** 登记表 classic；同一模块一种模式 |
| 新模块抄 `TestTreeServiceImpl` | **禁止当 layered 反例。** 未登记新模块默认五层 |

## 先把宏观地图放在桌上

L-074 已经把同一间实验室的**单表八扇**讲完，并预告：树表 `list` 回 `List`、导出是 GET、有父节点。本课走进**隔壁那块牌子**，不是富文本，不是厨房工厂。

把树表想成实验室里的**家谱练习卡**：每张卡有自己的号 `id`、爸爸号 `parentId`、部门号、用户号、一个名字 `treeName`。卡自己不跑流程，不发通知，不进 OSS。后端只把卡**平铺**给你；谁是谁的孩子，是浏览器拿胶水粘的。粘完看起来像树，REST 仍然是扁列表。

2026-09-17 工作树：`wta-demo/controller/` 里有二十来份 Java。本课只认 **`TestTreeController`**。邻居门牌对照用，不要背进格子：

```text
浏览器 / admin-web
        │  Admin-Token；无类上 @SaIgnore
        v
/demo/demo/*          TestDemoController      L-074（八扇，分页，POST 导出）
/demo/tree/*          TestTreeController      ← 本课六扇
/demo/rich-text/*     TestRichTextController  L-076
/demo/batch/*         TestBatchController     越层反例，不标 covered
/demo/excel/*         TestExcelController     deferred 玩具柜
/demo/encrypt         TestEncryptController   deferred
        │
        v  classic
ITestTreeService      五法：queryById / queryList / insertByBo / updateByBo / deleteWithValidByIds
        │
        v
TestTreeServiceImpl     自己抱 TestTreeMapper
                        validEntityBeforeSave 是空 TODO
                        删前校验是空 TODO
                        wrapper 不读 parentId
        │
        v
test_tree（逻辑删 del_flag；乐观锁 version；parent_id 默认 0）
XML 空壳；数据权限贴在 Mapper 接口类上
浏览器 buildTree 才把扁列表折成 children
```

**类比：** 实验室门口挂一块牌子「测试树表」。你可以要一整叠家谱卡（不分页）、把这叠卡印成 Excel、看某一张、贴新卡（可以指定爸爸）、改卡、撕卡。厨房认识其中五枪；印 Excel 那一枪种子上有按钮字，**管理页没画按钮**，厨房也没这支枪。厅堂下载管还只会 **POST**，就算你把单表那行 `runtime.download` 抄过来，也打不中这扇 **GET** 窗。

**类比失效处：**

1. 「家谱卡」**不是**新模块样板。登记表写明 demo 不得当 layered 的反面教材。
2. 页面上是树，HTTP **不是**树。`children` 是浏览器写进对象的，OpenAPI 的 `TestTreeVo` 没有这一列。
3. 查询参数里的 `parentId` **不是**「只看这一支」。厨师造 wrapper 时把这列丢掉了。
4. 导出动词和单表不一样。不要把 L-074 的 POST 抄到本课。
5. 删除**不会**先点名再一票否决。单表那套「集合里有一条越权，整票作废」不要搬过来。
6. `bundle-core` 不含 `wta-demo`。关的是装配，不是某一扇 404 开关。
7. 别的模块不要 Maven 依赖 `wta-demo` 去「复用树表」。新业务按 layered 自己建房间。
8. 分类课的「有孩子不能删 / 不能给自己当爸爸 / 后端拼 tree」**都不是**本课行为。本课这些闸都不存在。

## 核心概念与机制

### 直觉讲解

小孩子版只记十二句：

1. **一块牌子，六扇窗。** `@RequestMapping("/demo/tree")`。矩阵六个名字 = 磁盘 6 个方法。
2. **classic，不是五层。** 门卫 → 接口 → `ServiceImpl` 抱 Mapper。不要发明 `TestTreeUseCase`。
3. **列表是扁的，树是胶水。** `GET /list` 回 `List<TestTreeVo>`。页面 `buildTree(res.data, 'id', 'parentId')` 才长出 `children`。
4. **没有分页窗，没有导入窗。** 不要把单表的 `/page`、`/importData` 说成树表也有。
5. **导出是 GET。** `GET /export`，`void`，Excel 流。单表是 POST。厅堂 `download()` 固定 POST，对不上这扇窗。
6. **新增用注解校验，不是校验工具。** `add` 是 `@Validated(AddGroup.class)`。单表 `add` 才演示 `ValidatorUtils`。
7. **防重两扇一样长。** `add` / `edit` 都是 `@RepeatSubmit` 默认 **5000ms**。没有单表那种 2 秒特例。
8. **数据权限贴在 Mapper 类上。** `TestTreeMapper` 接口本体有 `@DataPermission(dept_id + user_id)`。切点命中该类代理；拦截器改写 SELECT / UPDATE / DELETE。INSERT 不改写。
9. **删除前校验是空抽屉。** `isValid=true` 只进一个 TODO 注释，然后 `deleteByIds`。没有「点名条数对不上就整票作废」。
10. **爸爸号是列，不是查询条件。** 实体/Bo/Vo/DDL 都有 `parentId`；`buildQueryWrapper` **不** `eq(parentId)`。厨房测试传 `{ parentId: 0 }` 也捞整片可见林。
11. **厨房不是六枪。** 页只打 `/list`、`GET /{id}`、`POST /`、`PUT /`、`DELETE /{ids}`。导出、自定义树接口、按父过滤，厨房都没有。
12. **装配电闸在 pom。** 默认 `bundle-full` 才把 `wta-demo` 塞进 admin jar；`bundle-core` 整栋实验室不进包。

**类比失效边界：** 「家谱卡」**不**覆盖部门 `excludeChild`、分类 `categoryTree`、单表两本分页本。类比也**不**等于「前端六个按钮」——导出在后端，管理页没扳机。类比还不等于「删爸爸会把孩子一起撕」——孩子会变成找不到爸爸的新根。类比更不等于「抄这一份就能开新模块」。

### 精确定义与 English term

| 中文口头 | English term | 精确定义（本课，以 2026-09-17 工作树为准） |
| --- | --- | --- |
| 测试树表控制器 | `TestTreeController` | classic `@RestController`，`/demo/tree`，继承 `BaseController`。六个公开映射。类上 `@Validated` `@RequiredArgsConstructor`，**无** `@ConditionalOnEnable` |
| 经典三层 | classic | 登记表：`wta-modules/wta-demo`。Controller → Service / ServiceImpl → Mapper。ServiceImpl 允许持 Mapper；不得借机扩大越层；不得当新模块 layered 反例 |
| 业务对象 | `TestTreeBo` | 入参。`@AutoMapper(target=TestTree, reverseConvertGenerate=false)`。`AddGroup`/`EditGroup`：`deptId`/`userId` `@NotNull`，`treeName` `@NotBlank`；`id` 只在 Edit 组 `@NotNull`。`parentId` **无**校验。**没有** QueryGroup 约束，**没有** `version` |
| 视图对象 | `TestTreeVo` | 出参。`@ExcelIgnoreUnannotated`。Excel 列：父id / 部门id / 用户id / 树节点名 / 创建时间。**没有**主键列，**没有** `children`，**没有** `@Translation`，**没有** `version` |
| 实体 | `TestTree` | `@TableName("test_tree")`，`@TableId id`，`parentId`，`@Version version`，`@TableLogic delFlag`（Java `Long`；DDL `int`）。**没有** `@OrderBy` |
| 树表服务 | `ITestTreeService` / `TestTreeServiceImpl` | 模块内门面。接口五法。Impl 唯一字段 `TestTreeMapper treeMapper`。注释里的 `// @DS("slave")` **未启用** |
| 扁列表 | `queryList` / `selectVoList` | `/list` 与 `/export` 共用。`selectList` + Mapstruct 转 Vo。不分页 |
| 前端拼树 | `buildTree` | `web-domains/demo/src/composables.ts`。先把每行 `children=[]`，再按 `parentId` 挂到父节点；父不在本批结果里的行升为根 |
| 类上数据权限 | `@DataPermission` on type | `TestTreeMapper` 接口级。占位符 `deptName`→`dept_id`，`userName`→`user_id`。`joinStr` 默认空：SELECT 用 OR，UPDATE/DELETE 用 AND |
| 防重复提交 | `@RepeatSubmit` | Redis `setObjectIfAbsent`。本课 add/edit 都是默认 **5000ms** |
| 成功/失败信封 | `toAjax(boolean)` | `true` → `R.ok()`；`false` → `R.fail()`。详情无行仍 `R.ok(null)` |
| 资源标签 | `demoTestTreeResource` | 前端 `controller: 'TestTreeController'`，`basePath: '/demo/tree'`。对照用，本课不认前端格子 |
| 成熟切片 | mature demo slice | course 范围内的 TestDemo / TestTree / TestRichText。其余 demo Controller deferred |
| 顶级父号 | `parentId = 0` | DDL 默认 0。表里**没有** `id=0` 的行。下拉里的「顶级节点」是浏览器造的假根 |

### 机制/因果链

#### 1. 类上合同：门牌、电话、没有电闸注解

文件：`backend/wta-modules/wta-demo/src/main/java/org/namewta/demo/controller/TestTreeController.java`。

类注解：`@Validated` `@RequiredArgsConstructor` `@RestController` `@RequestMapping("/demo/tree")`。

构造注入**只有** `ITestTreeService testTreeService`。全文搜不到 Mapper、`LoginHelper`、Excel 以外的第三方引擎。

类上没有 `@ConditionalOnEnable`。实验室整栋进不进 admin，看 `wta-admin/pom.xml`：`bundle-full`（默认激活）依赖 `wta-demo`；`bundle-core` **没有**这一行。

类上没有 `@SaIgnore`。六扇都要会话 + 各自权限字。

静态路径 `/list`、`/export` 和变量路径 `/{id}`、`/{ids}` 分开映射。`id` / `ids` 都是 `Long`，不会把 `"list"` 当成主键。

#### 2. `list`：`GET /demo/tree/list`

权限 `demo:tree:list`。无 `@Log`。入参 `TestTreeBo bo` 标了 `@Validated(QueryGroup.class)`，但 Bo **没有任何** QueryGroup 约束，所以查询字段都可以空。**没有** `PageQuery`。返回 `R<List<TestTreeVo>>`，不是 `PageResult`。

委托 `queryList` → `buildQueryWrapper`：

- `eq`：`deptId`、`userId`（非 null）
- `like`：`treeName`（非空白）
- **不读** `parentId`
- 再 `orderByAsc(id)`

然后 `treeMapper.selectVoList(lqw)` → `selectList` + 转 Vo → `R.ok(list)`。

这条链 **不执行** `TestTreeMapper.xml`（文件是空 mapper）。口试不要说「树表列表走 XML」，也不要说「后端已经按父子排好」。

谁在扣扳机：`TreePage.vue` `getList` → `runtime.service.listTree(queryParams)` → 厨房 `GET /demo/tree/list`。页面搜索表单**只绑** `treeName`；`queryParams` 里虽有 `parentId` / `deptId` / `userId`，界面没输入框。厨房测试会传 `{ parentId: 0 }`，那是契约形状，**不是**「只返回根」。

拿到扁数组之后，页面立刻 `buildTree<TreeVO>(res.data ?? [], 'id', 'parentId')`。后端 Vo 没有 `children`；胶水函数给每行补 `children: []` 再挂接。父行若被数据权限或 `treeName like` 滤掉，孩子会**升格成根**——表格看起来像断林，不是后端又返回了一棵新树。

下拉树 `getTreeselect` 再打一次**不带查询**的 `listTree()`，然后浏览器造 `{ id: 0, treeName: '顶级节点', children: ... }`。假根不在表里。

#### 3. `export`：`GET /demo/tree/export`

权限 `demo:tree:export`。`@Log(title="测试树表", businessType=EXPORT)`。返回类型 **`void`**，不是 `R<>`。入参 `@Validated TestTreeBo`——默认组，Bo 上约束都在 Add/Edit 组，所以导出过滤字段同样可空。绑定的是 **query string**，不是 JSON body。

同一份 `queryList`（仍不按 `parentId` 过滤）→ `ExcelBuilder.of(list, TestTreeVo.class).sheetName("测试树表").toResponse(response)`。

Excel 列以 `@ExcelProperty` 为准：父id、部门id、用户id、树节点名、创建时间。`id` **没有** Excel 注解，进不了表。`version` / 审计人 / `delFlag` 同样不进。树表 Vo **没有**单表那种 `index = 5` 和 `@Translation` 姓名列。导出仍是扁行，**不会**在单元格里画出缩进树。

前端对照（不标 covered）：

- 菜单种子 **有** F 型 `demo:tree:export`（`1761400000000001511`）。
- `createDemoWebDomain` 的 `demo-tree` 权限组 **没收** export 串。
- `TreePage.vue` **没有**导出按钮，也没有 `runtime.download`。
- 厅堂 `downloadWithAxios` **固定 `client.post`**。单表页那行 `runtime.download('demo/demo/export', …)` 若原样抄到树表，会 POST 一扇只认 GET 的窗。

口试说「有导出窗」指 **Java + OpenAPI `GET /demo/tree/export`**，不指「页面能点下载」。

#### 4. `getInfo`：`GET /demo/tree/{id}`

权限 `demo:tree:query`。无 `@Log`。路径 `@NotNull Long id`。`queryById` → `treeMapper.selectVoById` → `selectById` 再转 Vo。无行 → `R.ok(null)`，**不是** `R.fail`。

和单表的关键差别：树表 Mapper **类上**就有 `@DataPermission`。详情这条 SELECT 也会进数据权限拦截器。单表 `getInfo` 走的 `selectById` **没贴**注解，所以 L-074 说「详情不滤」。不要把那句话复制到本课。

无权限或看不见的 id：SQL 滤空 → 仍 200 + `data=null`。页面 `handleUpdate` 会 `Object.assign(form, res.data)`；空 data 不会帮你弹失败。

厨房 `getTree` 把 id `encodeURIComponent` 再拼路径。测试用 `'tree/8'` 锁 `/demo/tree/tree%2F8`，那是编码契约，不是表里真有这个主键。

#### 5. `add`：`POST /demo/tree`

权限 `demo:tree:add`。`@Log INSERT`。`@RepeatSubmit` 默认 5 秒。`@Validated(AddGroup.class) @RequestBody TestTreeBo`。

AddGroup 要：`deptId`、`userId` 非空，`treeName` 非空白。**不要** `id`。**不要** `parentId`——Bo 上爸爸号没有 `@NotNull`。页面规则写了「父id不能为空」，那是浏览器闸；HTTP 客户端可以不传。JSON `parentId: null` 会写入 null，**不会**自动变成 DDL 默认 0（默认只在列缺省插入时生效）。页面工具栏新增会把 `parentId` 写成 `0`；行内「新增」会写成当前行的 `id`（在这张卡下面挂孩子）。

`insertByBo`：Mapstruct 转实体 → 空的 `validEntityBeforeSave` → `insert`。成功则 `bo.setId(add.getId())`（雪花主键回填），门卫 `toAjax(true)`。2 秒内连点在单表会撞更短的表；本课两扇写窗都是 5 秒。

没有唯一名闸。同父同名可以插两行。没有「父行必须存在」闸。`parentId` 指到不存在的 id、指到自己、指到自己的子孙，本课 Service **都不拦**。分类课那些闸不要搬过来。

INSERT **不**被数据权限拦截器改写。你可以给别人的 `deptId` 挂一张卡；列表/详情能不能看见，是后面 SELECT 的事。

#### 6. `edit`：`PUT /demo/tree`

权限 `demo:tree:edit`。`@Log UPDATE`。`@RepeatSubmit` 默认 5 秒。`@Validated(EditGroup.class)`：要 `id`，其余与 Add 相同。仍不要 `parentId` 非空。

`updateByBo`：转实体 → 同一空 TODO → `updateById`。类上数据权限会给 UPDATE 加 AND 过滤。看不见的行 → 0 行 → `toAjax(false)` → `R.fail`。不是 601，也不是「您没有删除权限!」。

`TestTreeBo` / `TestTreeVo` **都没有** `version`。实体虽有 `@Version`，HTTP 往返带不走版本号。不要把单表「version 对不上则更新 0 行」说成本课页面的常规路径。乐观锁列在表上，进出这六扇窗时是休眠的。

页面 `handleUpdate` 有一句先把 `form.parentId = row.id`（把爸爸写成自己），紧接着 `getTree` + `Object.assign` 用详情覆盖。详情成功时那句赋值是死代码；详情 `data` 为空时，表单可能顶着「自己当爸爸」去提交——后端照单全收。

#### 7. `remove`：`DELETE /demo/tree/{ids}`

权限 `demo:tree:remove`。`@Log DELETE`。**没有** `@RepeatSubmit`。路径 `Long[] ids`，`@NotEmpty`。厨房 `deleteTree` 把数组 `join(',')`，测试锁 `/demo/tree/tree%2F8,tree%209`。管理页**没有**多选；行内删除一次只打一个 id。

`deleteWithValidByIds(Arrays.asList(ids), true)`：

1. `isValid==true`（门卫写死 true）：注释 `//TODO 做一些业务上的校验`，**什么都不做**。不 `selectByIds`，不数孩子，不抛「您没有删除权限!」。
2. 立刻 `treeMapper.deleteByIds(ids)`。`@TableLogic` → 逻辑删 `del_flag`。类上数据权限会给 DELETE 加 AND。集合里看不见的 id **静默跳过**；看得见的仍删。`toAjax(deleteByIds > 0)`：删到至少一行就 ok，剩下的越权行还在。这是「能删的先撕」，不是单表那种整票作废。

`isValid=false` 会跳过那个空 TODO，结果与 true **相同**。不要发明「管理端可关校验」——HTTP 从不传 false，而且 true 分支本来就是空的。

没有「有孩子不许删」。爸爸被逻辑删后，孩子的 `parentId` 仍指向那条已删 id。下一次 `list` 捞不到爸爸，`buildTree` 把孩子升成根。页面不会级联撕。

#### 8. 厨师侧门（不是六扇）

`ITestTreeService` 只有五法。`list` 与 `export` 共用 `queryList`，所以服务方法比窗少一扇。不要把 `queryList` 背成第七扇 HTTP。

`validEntityBeforeSave` 与删除 TODO 都是空抽屉。`treeName` 不唯一，父节点不校验存在。

Impl 类上和 `queryList` 上有注释掉的 `// @DS("slave")`。2026-09-17 **没有**从库切换。数据源课是 L-085，本课只认：注释不是电闸。

`TestTreeMapper` 接口体是空的，只继承 `BaseMapperPlus<TestTree, TestTreeVo>`，再贴类级数据权限。不要在 XML 里找 `WITH RECURSIVE` 或 `findChildren`。

## 图、表或文本图

**图 1：宏观六扇窗与 classic 抽屉**

```text
 bundle-full 才把 wta-demo 装进 admin
        │
        v
 TestTreeController     /demo/tree
        │
        ├─ GET  /list                 list         perm list     → selectVoList（扁 List，无 XML）
        ├─ GET  /export               export       perm export   → 同一 queryList → Excel 流
        ├─ GET  /{id}                 getInfo      perm query    → selectById（类上数据权限）
        ├─ POST /                     add          perm add      → AddGroup 注解 + insert
        ├─ PUT  /                     edit         perm edit     → EditGroup + updateById（类上数据权限）
        └─ DELETE /{ids}              remove       perm remove   → 空 TODO → 逻辑删（类上数据权限）
                │
                v
        ITestTreeService（五法，list/export 共用 queryList）
                │
                v
        TestTreeServiceImpl
                └─ TestTreeMapper  → test_tree（逻辑删 + version + parent_id）
                   类上 @DataPermission；XML 空壳
                │
                v  （浏览器，不是本课 REST）
        buildTree(id, parentId)  → el-table tree-props.children
```

**图题 / caption：** 宏观同一门牌六扇窗，classic 三跳，装配电闸在 bundle；树只存在于浏览器。alt：六个 Java 方法映射到 `/demo/tree`；ServiceImpl 抱 Mapper；列表扁；导出 GET；详情也走类上数据权限。

**文字等价物：** 已登录的管理员打到 `/demo/tree`。六扇都要对应权限字。门卫不写 SQL。厨师 `TestTreeServiceImpl` 自己抱 Mapper。XML 是空壳，列表不走手写 SQL。`parentId` 是行上的爸爸号，不是列表过滤器。页面把扁数组折成 `children`。`bundle-core` 时整份模块不进 jar，不是某一扇还在。

**图 2：六扇合同对照表**

| HTTP | 动词 | Java | 权限 | 写库？ | 成功形状 |
| --- | --- | --- | --- | --- | --- |
| `/list` | GET | `list` | `demo:tree:list` | 否 | `R.ok(List)`；扁；wrapper 不读 parentId |
| `/export` | GET | `export` | `demo:tree:export` | 否 | Excel 流，`void`；仍是扁行 |
| `/{id}` | GET | `getInfo` | `demo:tree:query` | 否 | `R.ok(Vo)`，无行也 ok；有数据权限 |
| `/` | POST | `add` | `demo:tree:add` | 是 | `toAjax`；5 秒防重；AddGroup 注解 |
| `/` | PUT | `edit` | `demo:tree:edit` | 是（过数据权限） | `toAjax`；5 秒防重 |
| `/{ids}` | DELETE | `remove` | `demo:tree:remove` | 是（权限 SQL 能删到才动） | 空 TODO；部分删除仍可能 ok |

**图题 / caption：** 矩阵六行与磁盘 1:1。alt：无 page/import；导出 GET；删前不点名。

**文字等价物：** 口试按这张表念：路径、动词、Java 名、权限串、动不动库、成功/失败信封。漏 export、把导出说成 POST、把 list 说成分页或后端树、把删除说成单表点名，格子不满。厨房覆盖 list/get/add/update/delete；导出既不进厨房，也不进页面。

**图 3：扁列表如何在浏览器长出树，以及权限如何把林剪断**

```text
 库里（种子两片林，parent_id=0 为根）
   测试数据权限(dept=102)
     └─ 子节点1
          └─ 子节点2
   测试树1(dept=108)
     ├─ 子节点11 → 子节点44
     ├─ 子节点22 → 子节点55
     └─ 子节点33 → 子节点66/77 → 88/99

 GET /list  （扁，按 id 升序）
   [根102, 子1, 子2, 根108, 11, 22, 33, 44, 55, 66, 77, 88, 99]

 浏览器 buildTree
   父在本批 → 挂 children
   父不在本批（like 只命中孩子 / 数据权限滤掉父 / 父已逻辑删）
        → 该行自己成为根

 类上 @DataPermission 改写
   SELECT list/export/getInfo
   UPDATE edit
   DELETE remove
   INSERT add     ← 不改写
```

**图题 / caption：** 树是胶水；权限和模糊搜都会让孩子变成假根。alt：两片种子林分属不同 dept_id；wrapper 不按父过滤。

**文字等价物：** 后端永远给你一叠卡。浏览器按爸爸号叠起来。叠的时候找不到爸爸，就把这张卡放到最外层。所以「页面上多了一截根」常常不是数据坏了，是这一批结果里爸爸没来。种子故意用两套 `dept_id` 练数据权限：102 那一枝和 108 那一枝不是同一间教室。

**图的边界：** 不画单表八扇（L-074）。不画富文本 OSS（L-076）。不保证以后会给 wrapper 补 `parentId`、给删除补点名、给导出改 POST。不把厨房 `TreeVO.children` 类型说成后端字段。不把 `bundle-core` 说成运行时 `@ConditionalOnEnable`。不把分类 `categoryTree` 画进 `/demo/tree`。

## 正例、反例与边界

**正例 1 — 打开家谱本。** 持 `demo:tree:list` 的人打开 `demo/tree/index`。页 `listTree(queryParams)` → `GET /demo/tree/list`。后端扁数组，前端 `buildTree` 后 `el-table` 用 `row-key=id`、`tree-props.children` 画出折叠表。默认 `isExpandAll=true`。

**正例 2 — 搜名字。** 查询只对 `treeName` 做 **like**。输入「子节点2」会命中「子节点2」「子节点22」这类更长的串。父行若名字不匹配，孩子在表格最外层冒出来。不要说成「搜子树」。

**正例 3 — 在根下贴新卡。** 工具栏「新增」：`parentId=0`，填部门/用户/值，POST `/demo/tree`。AddGroup 过了 → insert，主键回填 Bo。5 秒内连点第二次：防重异常，不 insert。页面 toast 写「操作成功」（不是单表那句「修改成功」）。

**正例 4 — 在某张卡下面挂孩子。** 行内 Plus：`handleAdd(row)` 把 `parentId` 写成 `row.id`，再 POST。后端不检查爸爸是否存在、是否已删、是否就是自己。

**正例 5 — 改一张看得见的卡。** 行上「修改」先 `GET /{id}` 填表，再 PUT。EditGroup 要 id。`updateById` 带类上数据权限；自己范围内 rows>0。

**正例 6 — 撕一张。** 行内删除确认后 `DELETE /demo/tree/{id}`。空 TODO 之后逻辑删。孩子还在，只是下次列表里找不到爸爸，升格为根。

**正例 7 — 越权改/撕（部分）。** 数据权限把 UPDATE/DELETE 的 WHERE 收窄。改看不见的行 → 0 行 → `R.fail`。删一个可见 id 加一个不可见 id → 可见的被逻辑删，不可见的还在，`toAjax(true)`。不要用单表「整票作废」来描述。

**正例 8 — 用 HTTP 客户端导出。** 持 `demo:tree:export`，**GET** `/demo/tree/export?treeName=测试`。收到 `.xlsx`，行仍是扁的，没有主键列。页面没有这颗按钮。

**正例 9 — 详情无行仍 200。** `GET /demo/tree/999` 或越权滤空：`R.ok(null)`。编辑页不会在后端这一枪看到 fail。

**正例 10 — 关装配。** `-Pbundle-core` 打 admin 包，本类不在 classpath。不要指望还能 `GET /list` 拿空数组。

**反例 1 — 「六扇都走 XML / 递归 SQL。」** XML 是空壳。没有 `findChildren`。

**反例 2 — 「`list` 回的是树。」** 回 `List`。`children` 是浏览器写的。

**反例 3 — 「导出是 POST，和单表一样。」** 树表是 GET。单表才是 POST（L-074）。

**反例 4 — 「页面能导出、能打 `/page`、能导入。」** 2026-09-17 都不能。

**反例 5 — 「厨房六枪。」** 五枪。export 不在 `DemoService`。

**反例 6 — 「传 `parentId=0` 只返回根。」** wrapper 丢掉 parentId。厨房测试那一行请求仍是整表可见林。

**反例 7 — 「这是 layered，Service 不许碰 Mapper。」** 登记 classic；Impl 必须能指到 `treeMapper`。

**反例 8 — 「新模块就该抄这一份。」** 登记表禁止把 demo 当 layered 反例。未登记默认五层。

**反例 9 — 「`getInfo` 像单表一样不滤部门。」** 树表 Mapper 类上有注解；详情 SELECT 会滤。

**反例 10 — 「`deleteWithValidByIds` 会点名，越权抛『您没有删除权限!』。」** 那是单表。本课 TODO 为空。

**反例 11 — 「有孩子会 601。」** 本课没有 `R.warn`。分类课才有（L-067）。

**反例 12 — 「`validEntityBeforeSave` 会查唯一名 / 父必须存在。」** 空 TODO。

**反例 13 — 「QueryGroup 会挡住空查询。」** Bo 上没有 QueryGroup 约束。

**反例 14 — 「OBJ-75 包含 `createDemoService` / `TreePage`。」** 那是 OBJ-77。本课只借用它们当扳机证据。

**反例 15 — 「`add` 用了 `ValidatorUtils`。」** 树表是 `@Validated(AddGroup.class)`。单表 add 才演示工具类。

**反例 16 — 「厅堂 download 抄过来就能导出。」** `downloadWithAxios` 固定 POST，打不中 GET 窗。

**反例 17 — 「后端 Vo 有 children，OpenAPI 也有。」** `TestTreeVo` 与 generated schema 都没有 `children`。前端 `TreeVO` 类型是页面自己的形状。

**边界 1 — 顶级是 0，不是表里的一行。** 下拉「顶级节点」id=0 是假根。不要 `GET /demo/tree/0` 当详情。

**边界 2 — like 断林。** 只搜孩子名时，UI 根集合 ≠ 库根集合。

**边界 3 — 逻辑删断林。** 已删父不再进 `@TableLogic` 列表；孩子升根。再删同一父 id，点不着行。

**边界 4 — 数据权限断林。** 两片种子林 `dept_id` 不同（`1761000000000000102` vs `…108`）。只看见其中一片时，另一片整枝消失，不是「还在但折叠着」。

**边界 5 — RepeatSubmit 间隔。** add 与 edit 都是 5000 **毫秒**。不要说成单表那种 2 秒 / 5 秒配对。

**边界 6 — 权限藏按钮 ≠ 授权。** `v-hasPermi` 藏新增/改/删；直接 HTTP 仍由 `@SaCheckPermission` 决定。`query`/`export` 连权限组都没收，REST 仍要那两串字。菜单 F 有 export，页面没按钮。

**边界 7 — 爸爸号校验不对称。** 页面 `rules.parentId` required；Bo 无 `@NotNull`。HTTP 可以不传爸爸。

**边界 8 — 无环检测。** 可以把 `parentId` 改成自己或子孙。分类课 Controller 那道「不能给自己当爸爸」本课没有。

**边界 9 — 无本课 Java 测试类。** 2026-09-17 `wta-demo` 测试目录几乎只见富文本。本课证据是 Controller/Service/Mapper/DDL/前端对照，不是 `TestTreeControllerTest`。厨房测试锁的是 URL 编码，不是树算法。

**边界 10 — bundle vs 运行时电闸。** demo 没有 `warm-flow.enabled` 那种类注解。关模块是编译装配。

**边界 11 — 种子十三行。** `50-cde-base-dml.sql` `test_tree` 从 `1762200000000000001` 到 `…013`。两片林。菜单 C/F：`1761400000000001506`–`1511`（list/query/add/edit/remove/export）。**没有** import 字，也**没有**「树选择」菜单。

**边界 12 — Excel 没有主键。** 导出再人工改 Excel **不能**当导入源——本课根本没有导入窗。

**边界 13 — 乐观锁休眠。** 表有 `version`，HTTP Bo/Vo 没有。不要用本课页面演示抢改冲突。

**边界 14 — `handleUpdate` 先写自己再覆盖。** 依赖 `getTree` 成功。失败或空 data 时，表单可能带着错误的 `parentId`。

## 变式与迁移

1. **和 L-074 对照。** 同一间 classic 房间。单表八扇：两本分页本、POST 导出、导入、`ValidatorUtils`、删前点名、方法级数据权限、详情不滤。树表六扇：整表 List、GET 导出、无导入、注解 AddGroup、空 TODO 删除、类上数据权限、详情也滤。先数窗数和动词，再搬「demo 都一样」。
2. **和 L-003 对照。** classic 列允许 ServiceImpl 抱 Mapper。本课把门卫六扇走完。不要用本课去改登记表，也不要给 demo 加 UseCase。L-003 反例 5（`TestBatchController`）仍然成立：本课门卫**没有**抱 Mapper。
3. **和 L-017 对照。** 部门才是「真组织树」：`excludeChild`、祖级链、缓存、删一条、列表仍是扁的再在别处拼。本课没有 exclude、没有 ancestors、没有 Redis 部门缓存。不要把 `SysDeptController` 的窗名背进 `/demo/tree`。
4. **和 L-067 对照。** 分类也是 classic + 扁列表 + 前端拼树 + 导出窗页面没扳机。差别：分类导出是 **POST**；有 `categoryTree` 后端树；有孩子/默认/已绑定义三道 601；不能给自己当爸爸；新增父必须存在；有运行时电闸。树表示例把这些闸全拆掉，只留家谱卡。
5. **和 L-076 对照。** 富文本才碰 OSS 资产口。本课 Vo 没有附件。
6. **和 L-077 对照。** 厨房五枪、权限组漏 query/export、页面无导出、`buildTree`、假根 id=0、toast「操作成功」，都是前端课的格子。本课只借用它们当「谁扣扳机」的证据，**不**把 `A:createDemoService` / `A:createDemoWebDomain` 标 covered。
7. **和 layered 公告对照。** `NotifyNoticeController` 只认识 UseCase。本课 `TestTreeBo` 一直传到 ServiceImpl。不要在 demo 里学公告再加 DAO。
8. **以后若要按父节点过滤。** 应在 `buildQueryWrapper` 加 `eq(parentId)`（并决定 0 是「只根」还是「不限」），同时改厨房测试——今天那条 `{ parentId: 0 }` 会从「整林」变成「只根」，属于行为变化。
9. **以后若要页面导出。** 不能照抄单表 `runtime.download`（POST）。要么把树表改成 POST（与单表/分类对齐，并改 OpenAPI），要么给下载管加 GET。还要给厨房加枪、给 Vue 按钮、给权限组补 `demo:tree:export`。菜单 F 已经在。
10. **以后若要删前点名 / 禁删有孩子。** 应把 TODO 写成真校验，并决定失败是 `ServiceException` 还是 601。现在部分删除会 ok。
11. **以后若要环检测。** 应在 Service 拦「父是自己 / 父是子孙」。现在 HTTP 放行。
12. **新模块。** 不要复制本课包结构当默认。先登记 layered，再按五层建。要把某模块留在 classic，必须走登记表例外，不能口称「跟 demo 树一样」。真业务树通常还要 ancestors、孩子计数、不能挂到自己下面——那些是部门/分类，不是本课。
13. **迁移口诀：** 先数六扇 HTTP → 再数 classic 三跳 → 再数「扁列表 + 浏览器胶水」→ 再数导出 GET ≠ 单表 POST → 再数类上数据权限（详情也滤，删除不点名）→ 再数 wrapper 丢掉 parentId → 最后数「样板 ≠ 模板」。跳步会出现「把树说成后端返回」「把导出说成 POST」「把单表点名搬过来」「把 demo 抄进新模块」。

## 常见误区

1. **「OBJ-75 是整个 wta-demo 模块。」** 只认树表 Controller 六法。单表、富文本、玩具柜都不是这一格。
2. **「CRUD 四个字。」** 磁盘六扇。漏 export 不满格。
3. **「列表走 Mapper XML / WITH RECURSIVE。」** 空 XML；Java wrapper。
4. **「`list` 的 JSON 带 children。」** 不带。胶水在浏览器。
5. **「导出 POST。」** GET。
6. **「页面有导出。」** 没有。种子有 F 字，权限组没收，download 还是 POST。
7. **「厨房有 exportTree / pageTree。」** 没有。
8. **「详情不数据权限。」** 那是单表。树表类上有注解。
9. **「删除 SQL 前会点名一票否决。」** 不会。TODO 是空的。
10. **「`validEntityBeforeSave` 有唯一闸 / 父存在闸。」** 空 TODO。
11. **「add 和 edit 防重不一样长。」** 都是 5 秒。2 秒是单表 add。
12. **「add 用了 `ValidatorUtils`。」** 用的是 `@Validated(AddGroup.class)`。
13. **「传 parentId 就是查子树。」** wrapper 不读它。
14. **「这是新模块样板。」** 登记表禁止。
15. **「有孩子会黄灯 601。」** 没有。孩子变孤儿根。
16. **「不能把自己设成爸爸。」** 能。分类课才拦。
17. **「前端授权。」** `v-hasPermi` 藏按钮；REST 仍是 `@SaCheckPermission`。
18. **「给 demo 加五层才算现代化。」** 同一模块一种模式；登记表保持 classic。
19. **「`bundle-core` 还留着树窗。」** 不留。
20. **「OBJ-75 包含 TreePage / buildTree。」** 页面是对照；格子是 Java 六法。
21. **「TestTreeVo 有 version，抢改会失败。」** Vo/Bo 都没有 version。
22. **「OpenAPI `POST /demo/tree/export`。」** generated 路径是 GET。
23. **「假根 id=0 在 test_tree 里。」** 不在。DDL 默认只是父号。

## 非评分暂停

打开磁盘，不要凭记忆默写路径。不要改文件。没有标准答案栏，没有分数。

1. 打开 `TestTreeController.java`。用手指点 6 个映射。圈 `list` 回 `List` 不是 `PageResult`。圈 `export` 是 **GET** + `void`。圈 `add` 的 `@Validated(AddGroup.class)` 与默认 `@RepeatSubmit`。圈 `edit` 同样 5 秒。圈 `remove` 的 `Long[]` 与 `true`。确认没有 `/page`、没有 `importData`。
2. 打开 `ITestTreeService.java` 与 `TestTreeServiceImpl.java`。圈五法。圈唯一字段 `treeMapper`。圈 `buildQueryWrapper` **没有** `parentId`。圈两个空 TODO。圈注释掉的 `@DS("slave")`。
3. 打开 `TestTreeMapper.java` 与 `mapper/demo/TestTreeMapper.xml`。圈**类上** `@DataPermission` 两列。确认 XML 是空 mapper。对比 `TestDemoMapper.java`：单表是五个方法上的注解，没有类上这一层。
4. 打开 `TestTree.java` / `TestTreeBo.java` / `TestTreeVo.java`。圈 `@TableName("test_tree")`、`parentId`、`@Version`、`@TableLogic`。圈 Bo 没有 QueryGroup、没有 version、`parentId` 无校验。圈 Vo 没有 children、没有主键 Excel 列、没有 Translation。
5. 打开 `10-cde-base-ddl.sql` 的 `CREATE TABLE test_tree` 与 `50-cde-base-dml.sql` 菜单 1761400000000001506–1511、INSERT 十三行。圈两片林的不同 `dept_id`。确认有 export F、没有 import。
6. 打开 `frontend/packages/domains/demo/src/{index.ts,index.test.ts,test-tree/index.ts}` 与 `web-domains/demo/src/{index.ts,composables.ts,test-tree/TreePage.vue}`。勾 list/get/add/update/delete；叉 export；圈 `buildTree`、假根「顶级节点」、权限组四串、无导出按钮。打开 `adapters/axios-browser` 的 `downloadWithAxios`，圈 `client.post`。不要改这些文件。

## 总结、词汇表与下一步

- **宏观六扇树表窗：** 同一门牌 `/demo/tree`，磁盘 6 个方法。classic 抽屉：门卫只打电话给 `ITestTreeService`；厨师抱 Mapper；XML 空壳。装配电闸是 `bundle-full` / `bundle-core`。
- **(a) `TestTreeController.list,export,getInfo,add,edit,remove`：** `list` 扁 List 且 wrapper 不读 parentId、`export` GET 流、`getInfo` 可空且**走类上数据权限**、`add` 注解校验 + 5 秒防重、`edit` 注解校验 + 5 秒防重 + 更新带数据权限、`remove` 空 TODO 后逻辑删（可见行才动，不整票作废）。不要把 `/page`、导入、后端 children、厨房 export、单表点名、分类 601 算进这一格。
- **树在浏览器，不在 REST。** `buildTree` 是对照；本格 covered 的是六扇 Java 窗。爸爸号是列；过滤条件里它是摆设。
- **样板 ≠ 模板。** 成熟切片用来认 classic 形状和「树表示例到底省了哪些闸」；新模块默认 layered。真业务树去看部门/分类，不要抄本课空 TODO。

词汇表：`TestTreeController` / `ITestTreeService` / `TestTreeServiceImpl` / `TestTree` / `TestTreeBo` / `TestTreeVo` / `test_tree` / `parentId` / `selectVoList` / `@DataPermission`（类上） / `@RepeatSubmit` / `ExcelBuilder` / `toAjax` / `@Version` / `@TableLogic` / `buildTree` / classic / `bundle-full` / `bundle-core`。

下一步：L-076 才是富文本和 OSS 资产。L-077 才把厨房五枪、`buildTree`、三键菜单、download 对完。能力展示 Controller 保持 deferred。本课结束不发作业、不打分；要练习请之后主动激活 Homework。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-004 | `backend/wta-modules/**` Controller | 业务模块公开入口；本课类在 `wta-demo/controller` | `TestTreeController.java` | 2026-09-17 |
| S-006 | `frontend/packages/{domains,web-domains}` | 厨房五枪、资源标签、权限组、TreePage 扳机、`buildTree` | `domains/demo`；`web-domains/demo` | 2026-09-17 |
| S-007 | `.agents/skills/engineering-standards/references/project/01-module-map.md` 与 `wta-admin/pom.xml` | bundle-full 含 demo；bundle-core 不含 | `wta-admin` profile `bundle-full` / `bundle-core` | 2026-09-17 |
| S-008 | `.agents/skills/engineering-standards/references/project/03-backend-module-modes.md` | `wta-demo` = classic；不得当 layered 反例；不得扩大越层 | 登记表 demo 行 | 2026-09-17 |
| S-010 | `release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql`、`50-cde-base-dml.sql` | 表 `test_tree`；十三行种子两片林；菜单 C/F 含 export | `CREATE TABLE test_tree`；菜单 1761400000000001506–1511 | 2026-09-17 |
| S-L075-01 | `TestTreeController.java` | 六扇映射；权限字；GET 导出；AddGroup 注解；5s 防重；删 `true` | 类上 `/demo/tree` 及各方法 | 2026-09-17 |
| S-L075-02 | `ITestTreeService.java`；`TestTreeServiceImpl.java` | classic 厨师；五法；wrapper 无 parentId；两个空 TODO | queryList/buildQueryWrapper/deleteWithValidByIds | 2026-09-17 |
| S-L075-03 | `TestTree.java`；`TestTreeBo.java`；`TestTreeVo.java`；`TestTreeMapper.java`；`TestTreeMapper.xml` | 表名；校验组；Excel 注解；类上 DataPermission；XML 空壳 | `@TableName`；Add/Edit；空 mapper | 2026-09-17 |
| S-L075-04 | `BaseController.toAjax`；`RepeatSubmit`；`DataPermission` / `DataPermissionPointcut` / `PlusDataPermissionInterceptor`；`ExcelBuilder` | 行数信封；默认 5000ms；类上切点；SELECT/UPDATE/DELETE 改写、INSERT 不改；Excel 流 | 各类型字段/方法 | 2026-09-17 |
| S-L075-05 | `BaseMapperPlus.selectVoById` / `selectVoList` | 详情 `selectById`；列表 `selectList` 再转 Vo | default 方法 | 2026-09-17 |
| S-L075-06 | `TestDemoController.java`；`TestDemoMapper.java`；L-074 | 单表八扇、POST 导出、方法级权限、删前点名；对照不是本格 | `/demo/demo`；五个 `@DataPermission` override | 2026-09-17 |
| S-L075-07 | `frontend/packages/domains/demo/src/{index.ts,index.test.ts,test-tree/index.ts}` | 五枪 URL；无 export；测试 `{ parentId: 0 }`；资源标签 | `createDemoService`；`demoTestTreeResource` | 2026-09-17 |
| S-L075-08 | `web-domains/demo/src/{index.ts,composables.ts,test-tree/TreePage.vue}`；`adapters/axios-browser` `downloadWithAxios` | 权限组四串；`buildTree`；假根；无导出按钮；download 固定 POST | `demo-tree` permissions；`getList`；`client.post` | 2026-09-17 |
| S-L075-09 | 子课 L-003 / L-017 / L-067 / L-074；course.md OBJ-75；coverage-matrix TestTree 行；OpenAPI `/demo/tree/export` GET | classic；部门/分类对照；单表预告 GET 导出；矩阵六名 | L-003；`course.md` OBJ-75；`generated/openapi.ts` | 2026-09-17 |
