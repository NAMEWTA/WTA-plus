---
lesson_id: L-074
objective_ids: [OBJ-74]
claimed_cells:
  - A:TestDemoController.list,page,importData,export,getInfo,add,edit,remove
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: eight-doors-on-disk
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
source_ids: [S-004, S-006, S-007, S-008, S-010, S-L074-01, S-L074-02, S-L074-03, S-L074-04, S-L074-05, S-L074-06, S-L074-07, S-L074-08, S-L074-09]
---

# Lesson 074：宏观八扇样板窗——TestDemoController 的 classic CRUD

## 学完你能做什么

打开 `backend/wta-modules/wta-demo/src/main/java/org/namewta/demo/controller/TestDemoController.java`，你能**口述八扇公开窗**：谁分页、谁走 XML、谁读 Excel、谁写 Excel 流、谁按主键看一行、谁增、谁改、谁按逗号删。口试名单就是矩阵 **(a)** 这一格，符号以**磁盘**为准：

**`A:TestDemoController.list,page,importData,export,getInfo,add,edit,remove`**：同一门牌 `/demo/demo` 上，**正好 8 个** Java 公开方法。chain 把八个名字写全了，不是「CRUD 四个字母」。口试按磁盘，不要漏 `page` 和 `importData`，也不要把厨房五枪或 `TestBatchController` 背成第九扇。

OBJ-74 原文只要你能口述 `TestDemoController` classic CRUD（含导入导出）当作**成熟样例**。本课还要把 **classic 抽屉**讲完，否则「能口述」会退化成背路径：

- 登记表：`wta-demo` 是 **classic**。调用链是 **Controller → `ITestDemoService` → `TestDemoServiceImpl`（自己抱 `TestDemoMapper`）→ Mapper Java default / `BaseMapperPlus` + 一份 XML**。XML **不是空壳**：只有 `customPageList` 这一条 `SELECT *`。没有 UseCase，没有 DAO。
- 登记表旁注：**「示例和集成演示；保持现有示例可运行；不得作为新模块 layered 反例。」** 本课是存量样板，**不是**抄作业模板。
- 八扇都贴 `@SaCheckPermission`。没有 `@SaIgnore`，没有 `@ConditionalOnEnable`。关模块靠 **bundle**，不是运行时电闸。
- 失败信封主要是 `R.fail`（`toAjax` 行数为 0）和 `ServiceException`（删前「您没有删除权限!」）。本课**没有**分类课那种 `R.warn` 601 黄灯。

本课**不宣称**你会拆 `TestTreeController`（L-075）、`TestRichTextController` 与 OSS 资产（L-076）、或把 `createDemoService` / `createRichTextService` / `createDemoWebDomain` 当本格 covered（L-077）。厨房 URL 只当**对照**：证明八扇窗里哪些被浏览器扣了扳机，哪些种子有、厨房没收。Redis/MQTT/MCP/Excel 模板/ES/WebSocket/限流/加密/敏感/SaToken 文档/邮件短信/队列那些 Controller 是矩阵 **deferred(capability-demo-not-product-room)**：模式由本课 covered-by-parent，格子不标它们 covered。

2026-09-17 工作树先钉死**包边界**（口试先数窗，再数链）：

| 你可能以为的名字 | 磁盘事实 |
| --- | --- |
| 第九扇 HTTP `saveBatch` | **没有。** 它是 `ITestDemoService.saveBatch`，只给 `importData` 打电话 |
| `GET /export` | **没有。** 单表导出是 **`POST /export`**。树表才是 GET（L-075） |
| 厨房 `pageDemo` / `importDemo` | **没有。** `createDemoService` 只有 list/get/add/update/delete；导出走 `runtime.download`，不进厨房对象 |
| 菜单 F 型 `demo:demo:import` | **没有。** 种子只有 list/query/add/edit/remove/export。后端窗仍要 `demo:demo:import` |
| web-domain 权限组含 `query` / `import` | **没有。** 组里五串：list/add/edit/remove/export |
| `TestDemoMapper.xml` 空壳 | **不是。** 只有 `customPageList`；`/list` 那条主走廊**不执行**这段 XML |
| `getInfo` 也有数据权限 | **没有。** `@DataPermission` 贴在 `selectVoPage` / `customPageList` / `selectVoList` / `selectByIds` / `updateById`。`queryById` 走 `selectVoById` → `selectById`，**没贴** |
| `TestBatchController` 是本课样板 | **不是。** 门牌 `/demo/batch`，Controller **直持** `TestDemoMapper`。L-003 反例 5：棘轮拧松，不是模板 |
| 给 demo 房间加 UseCase/DAO | **本课不发动。** 登记表 classic；同一模块一种模式 |
| 新模块抄 `TestDemoServiceImpl` | **禁止当 layered 反例。** 未登记新模块默认五层 |

## 先把宏观地图放在桌上

L-003 已经把 `wta-demo` 钉在 classic 列，并用 `GET /demo/demo/list` 当「ServiceImpl 抱 Mapper」的正例。本课走进**同一份 Controller 的八扇窗**，不是树表、不是富文本、不是厨房工厂。

把 demo 楼想成教学实验室。单表是**一张没有父子的练习卡**：部门号、用户号、排序号、一把钥匙 `testKey`、一个值 `value`。卡片自己不跑流程，不发通知。树表（L-075）才有爸爸；富文本（L-076）才去 OSS 仓库取图片。能力展示窗（Redis 锁、MQTT、Excel 模板……）是走廊上的玩具柜，矩阵标 deferred，模式跟本课同一间 classic 房间，但**不是**这八扇。

2026-09-17 工作树：`wta-demo/controller/` 里有二十来份 Java。本课只认 **`TestDemoController`**。邻居门牌对照用，不要背进格子：

```text
浏览器 / admin-web
        │  Admin-Token；无类上 @SaIgnore
        v
/demo/demo/*          TestDemoController      ← 本课八扇
/demo/tree/*          TestTreeController      L-075
/demo/rich-text/*     TestRichTextController  L-076
/demo/batch/*         TestBatchController     越层反例，不标 covered
/demo/excel/*         TestExcelController     deferred 玩具柜
/demo/encrypt         TestEncryptController   deferred；实体也映射 test_demo 表
        │
        v  classic
ITestDemoService
        │
        v
TestDemoServiceImpl     自己抱 TestDemoMapper
                        validEntityBeforeSave 是空 TODO
        │
        v
test_demo（逻辑删 del_flag；乐观锁 version）
XML 只服务 GET /page 的 customPageList
```

**类比：** 实验室门口挂一块牌子「测试单表」。你可以翻分页本（两本：一本机器写 SQL，一本手写 XML）、把本子印成 Excel、把 Excel 塞回去、看某一张卡、贴新卡、改卡、撕卡。厨房（浏览器 domain）认识其中五枪；印 Excel 那一枪页面用 `runtime.download` 另走一条下载管，不经过 `createDemoService`。自定义分页和导入两枪，管理页没扳机。

**类比失效处：**

1. 「练习卡」**不是**新模块样板。登记表写明 demo 不得当 layered 的反面教材。
2. 两本分页本看起来一样（都回 `PageResult<TestDemoVo>`），SQL 不是同一条。`/list` 走 `selectVoPage`；`/page` 才进 XML。
3. 导入窗在后端，种子**没有** import 按钮字，页面**没有**导入按钮。
4. `TestDemoEncrypt` 也写 `@TableName("test_demo")`，那是加密玩具，不是本课 CRUD 实体。
5. `bundle-core` 不含 `wta-demo`。关的是装配，不是某一扇 404 开关。
6. 别的模块不要 Maven 依赖 `wta-demo` 去「复用单表」。新业务按 layered 自己建房间。

## 核心概念与机制

### 直觉讲解

小孩子版只记十二句：

1. **一块牌子，八扇窗。** `@RequestMapping("/demo/demo")`。矩阵八个名字 = 磁盘 8 个方法，不是「增删改查」。
2. **classic，不是五层。** 门卫 → 接口 → `ServiceImpl` 抱 Mapper。不要发明 `TestDemoUseCase`。
3. **两本分页本。** `GET /list` = wrapper `selectVoPage`（主走廊，XML 不执行）。`GET /page` = XML `SELECT * FROM test_demo ${ew.customSqlSegment}`。权限字都是 `demo:demo:list`。
4. **导入导出是额外两扇。** 导入 `POST /importData` multipart；导出 `POST /export` 写 Excel 流，返回类型 `void`。
5. **新增用校验工具，修改用注解。** `add` 故意 `ValidatorUtils.validate(bo, AddGroup.class)`，注释写「对标 `@Validated(AddGroup.class)`，给非 Controller 的地方看」。`edit` 才是 `@Validated(EditGroup.class)`。
6. **防重两把秒表。** `add`：`@RepeatSubmit(interval = 2, timeUnit = SECONDS)` → **2 秒**。`edit`：`@RepeatSubmit` 默认 **5000ms**。导入、删除、导出不罩。
7. **数据权限贴在 Mapper 方法上，不是类上。** 列表/自定义分页/导出/按 id 集合查/按 id 更新会滤 `dept_id` + `user_id`。详情 `getInfo` **不滤**。
8. **删除先点名再撕。** `isValid=true` 时 `selectByIds`（带权限）条数对不上 → 抛「您没有删除权限!」，**一行都不删**。真正 `deleteByIds` **没贴**数据权限。
9. **保存前校验是空抽屉。** `validEntityBeforeSave` 只有 `//TODO`。`testKey` **不**唯一。
10. **导入回执不管写库成败。** `saveBatch` 的 boolean **丢掉**；永远 `R.ok(excelResult.getAnalysis())`。校验失败且 `failFast=true`（默认）会抛，进不了 save。
11. **厨房不是八枪。** 页只打 `/list`、`GET /{id}`、`POST /`、`PUT /`、`DELETE /{ids}`；导出走 `runtime.download('demo/demo/export', …)`。
12. **装配电闸在 pom。** 默认 `bundle-full` 才把 `wta-demo` 塞进 admin jar；`bundle-core` 整栋实验室不进包。

**类比失效边界：** 「练习卡」**不**覆盖 `TestExcelController` 的模板导出（另一门牌 `/demo/excel`）。类比也**不**等于「前端八个按钮」——自定义分页和导入在后端，管理页没扳机。类比还不等于「详情也按部门过滤」——`selectById` 没注解。类比更不等于「抄这一份就能开新模块」。

### 精确定义与 English term

| 中文口头 | English term | 精确定义（本课，以 2026-09-17 工作树为准） |
| --- | --- | --- |
| 测试单表控制器 | `TestDemoController` | classic `@RestController`，`/demo/demo`，继承 `BaseController`。八个公开映射。类上 `@Validated` `@RequiredArgsConstructor`，**无** `@ConditionalOnEnable` |
| 经典三层 | classic | 登记表：`wta-modules/wta-demo`。Controller → Service / ServiceImpl → Mapper。ServiceImpl 允许持 Mapper；不得借机扩大越层；不得当新模块 layered 反例 |
| 业务对象 | `TestDemoBo` | 入参。`@AutoMapper(target=TestDemo, reverseConvertGenerate=false)`。`AddGroup`/`EditGroup`：`deptId`/`userId`/`orderNum` `@NotNull`，`testKey`/`value` `@NotBlank`；`id` 只在 Edit 组 `@NotNull`。**没有** QueryGroup 约束 |
| 视图对象 | `TestDemoVo` | 出参。`@ExcelIgnoreUnannotated`。`createByName`/`updateByName` 靠 `@Translation(USER_ID_TO_NAME)`。`userId` 的 `@ExcelProperty` 带 `index = 5`。`version` 无 Excel 注解 |
| 导入视图 | `TestDemoImportVo` | 导入行。无 id、无 version。`orderNum` 是 **`Long`**（实体是 `Integer`）。校验在默认组，不是 AddGroup |
| 实体 | `TestDemo` | `@TableName("test_demo")`，`@TableId id`，`@Version version`，`@TableLogic delFlag`（Java `Long`；DDL `int`）。`orderNum` 上 `@OrderBy(asc=false, sort=1)` |
| 单表服务 | `ITestDemoService` / `TestDemoServiceImpl` | 模块内门面。Impl 唯一字段 `TestDemoMapper demoMapper` |
| 主走廊分页 | `queryPageList` / `selectVoPage` | `/list` 走它。Mapper 上 override 并贴 `@DataPermission` |
| 自定义分页 | `customPageList` | `/page` 走它。Java 接口 + XML `SELECT * FROM test_demo ${ew.customSqlSegment}`，同样贴数据权限 |
| 数据权限 | `@DataPermission` + `@DataColumn` | 占位符 `deptName`→`dept_id`，`userName`→`user_id`。`selectByIds` 额外 `joinStr = "AND"` |
| 防重复提交 | `@RepeatSubmit` | Redis `setObjectIfAbsent`。间隔小于 1 秒会抛。键含 URL + token + 参数 MD5 |
| 校验工具 | `ValidatorUtils.validate` | 手动跑 Bean Validation。`add` 用来对标注解；Excel 监听默认也对每一行调用它 |
| 成功/失败信封 | `toAjax(boolean)` | `true` → `R.ok()`；`false` → `R.fail()`。导入**不**走 toAjax |
| 导入回执 | `ExcelResult.getAnalysis()` | 无成功行：「读取失败，未解析到数据」；全成功：「恭喜您，全部读取成功！共N条」；有错：统计句 |
| 资源标签 | `demoTestDemoResource` | 前端 `controller: 'TestDemoController'`，`basePath: '/demo/demo'`。对照用，本课不认前端格子 |
| 成熟切片 | mature demo slice | course 范围内的 TestDemo / TestTree / TestRichText。其余 demo Controller deferred |

### 机制/因果链

#### 1. 类上合同：门牌、电话、没有电闸注解

文件：`.../controller/TestDemoController.java`。

类注解：`@Validated` `@RequiredArgsConstructor` `@RestController` `@RequestMapping("/demo/demo")`。

构造注入**只有** `ITestDemoService testDemoService`。全文搜不到 Mapper、`LoginHelper`、Excel 以外的第三方引擎。

类上没有 `@ConditionalOnEnable`。实验室整栋进不进 admin，看 `wta-admin/pom.xml`：`bundle-full`（默认激活）依赖 `wta-demo`；`bundle-core` **没有**这一行。

类上没有 `@SaIgnore`。八扇都要会话 + 各自权限字。

#### 2. `list`：`GET /demo/demo/list`

权限 `demo:demo:list`。无 `@Log`。入参 `TestDemoBo bo` 标了 `@Validated(QueryGroup.class)`，但 Bo **没有任何** QueryGroup 约束，所以查询字段都可以空。另绑 `PageQuery`（`pageNum` / `pageSize` / 排序列）。

`PageQuery.build()`：页码默认 1；**页大小默认 `Integer.MAX_VALUE`（注释写「默认查全部」）**。管理页会传 `pageNum=1, pageSize=10`。你用 HTTP 客户端不带分页参数，等于一次捞整表可见行。

委托 `queryPageList` → `buildQueryWrapper`：

- `eq`：`deptId`、`userId`（非 null）
- `like`：`testKey`（非空白）
- `eq`：`value`（非空白；**不是** like）
- 再 `orderByAsc(id)`

然后 `demoMapper.selectVoPage(pageQuery.build(), lqw)` → `PageResult.build(records, total)` → `R.ok`。

这条链 **不执行** `TestDemoMapper.xml`。L-006 探针已经点过：主走廊是 wrapper，XML 只给 `/page`。口试不要说「单表列表走 XML」。

谁在扣扳机：`DemoPage.vue` `runtime.service.listDemo(queryParams)` → 厨房 `GET /demo/demo/list`。厨房把 `TestDemoVo` 投影成 `DemoVO`（缺字段填 `''` / `0`），**丢掉** `createTime` / `version` 等审计列。

#### 3. `page`：`GET /demo/demo/page`

权限同样 `demo:demo:list`。入参形状与 `list` 相同。委托 `customPageList` → 同一份 `buildQueryWrapper` → `demoMapper.customPageList`。

XML：

```xml
<select id="customPageList" resultType="org.namewta.demo.domain.vo.TestDemoVo">
    SELECT * FROM test_demo ${ew.customSqlSegment}
</select>
```

`${ew.customSqlSegment}` 是 wrapper 生成的 `WHERE … ORDER BY …`。逻辑删、数据权限、查询条件都靠这段拼进去。它**不是**另一张表，只是「手写 SELECT *」教学窗。

厨房 **没有** `pageDemo`。管理页 **不打** `/page`。口试说「有自定义分页窗」指 **Java + OpenAPI**，不指「表格切页走它」。表格切页走 `/list`。

#### 4. `importData`：`POST /demo/demo/importData`

权限 `demo:demo:import`。`@Log(title="测试单表", businessType=IMPORT)`。`consumes = MULTIPART_FORM_DATA`，参数 `@RequestPart("file") MultipartFile file`。**没有** `@RepeatSubmit`。

门卫顺序：

1. `ExcelBuilder.read(stream, TestDemoImportVo.class).validate(true).doRead()`。`validate` 默认已是 true，这里再写一次。监听默认 `failFast=true`：某一行校验/转换失败 → `ExcelAnalysisException`，**整次导入中止**，还没到 `saveBatch`。
2. `MapstructUtils.convert(excelResult.getList(), TestDemo.class)`。导入 Vo 没有主键；`orderNum` Long→实体 Integer。
3. `testDemoService.saveBatch(list)` → `demoMapper.insertBatch` → MP `Db.saveBatch`。返回值**丢掉**。
4. `return R.ok(excelResult.getAnalysis())`。分析句只统计**读 Excel** 的成功/失败行，不统计 insert 是否 >0。

`TestDemoImportVo` 校验在**默认组**：部门/用户/排序非空，key/值非空白。不像 `add` 走 AddGroup。没有 version、没有 id——新行靠雪花主键，version 吃列默认 0。

前端：厨房没有 import 方法；`DemoPage` 没有导入按钮；web-domain `demo-table` 权限组没有 `demo:demo:import`；`50-cde-base-dml.sql` 测试单表菜单 **没有** import 那颗 F。后端窗 + OpenAPI 仍在。口试不要说「页面能导入」。

对照：用户课 `SysUserController.importData` 有专用 Listener 写密码。本课导入是默认监听 + 整批 insert，**不**走 `insertByBo`，因此也**不**跑 `validEntityBeforeSave`（反正它是空的）。

#### 5. `export`：`POST /demo/demo/export`

权限 `demo:demo:export`。`@Log EXPORT`。返回类型 **`void`**，不是 `R<>`。入参 `@Validated TestDemoBo`——默认组，Bo 上约束都在 Add/Edit 组，所以导出过滤字段同样可空。

同一份 `queryList` → `selectVoList`（带数据权限）→ `ExcelBuilder.of(list, TestDemoVo.class).sheetName("测试单表").toResponse(response)`。

源码里有一段**注释掉的**雪花 id 测试（把 Vo 的 id 改成超长数字）。不要把它说成还在跑。

Excel 列以 `@ExcelProperty` 为准。`version` 无注解，进不了表。`userId` 写了 `index = 5`，和未指定 index 的列排在一起时，列顺序以 EasyExcel/Fesod 的 index 规则为准——口试只要知道「导出形状看 Vo 注解，不是看表字段顺序」。

前端：`handleExport` 调 `runtime.download('demo/demo/export', { ...queryParams }, 'demo_${timestamp}.xlsx')`。这是厅堂下载管，**不是** `createDemoService` 上的方法。权限组**有** `demo:demo:export`；菜单种子**有** F 型导出。和分类课相反：分类页没按钮，单表页**有**按钮。

树表对照（L-075 才认格子）：`TestTreeController.export` 是 **`GET /export`**。不要把两扇导出动词背成一样。

#### 6. `getInfo`：`GET /demo/demo/{id}`

权限 `demo:demo:query`。`@NotNull` 在路径变量上（类上 `@Validated` 让它生效）。`R.ok(queryById)` → `selectVoById` → `selectById` + MapStruct。查不到是 `R.ok(null)`，**不是** fail。

**没有** `@DataPermission`。知道主键的人，只要有 `query` 字，就能读到不在自己部门/用户范围内的那一行。列表里看不见，详情仍可能 200。这是磁盘事实，不是「应该如此」的产品承诺。

管理页编辑先打这一枪：`runtime.service.getDemo(demoId)`，再 `Object.assign` 进表单。web-domain 权限组**没收** `demo:demo:query` 串；按钮靠 `edit` 字显示。直接打 GET 仍由 `@SaCheckPermission("demo:demo:query")` 决定。菜单种子**有** F 型 query。

#### 7. `add`：`POST /demo/demo`

权限 `demo:demo:add`。`@Log INSERT` + `@RepeatSubmit(interval = 2, timeUnit = TimeUnit.SECONDS, message = "{repeat.submit.message}")`。

门卫顺序：

1. 切面：同一 URL + token + 参数 2 秒内再来 → `ServiceException`（国际化 `repeat.submit.message`）。
2. `ValidatorUtils.validate(bo, AddGroup.class)`。缺部门/用户/排序/key/值 → 约束异常。**不要求** id。
3. `toAjax(insertByBo)`。

`insertByBo`：Bo→实体，空 TODO，`insert`。成功则把生成的 id **写回 Bo**（HTTP 响应仍是 `R<Void>`，浏览器通常看不到这个 id）。`insert` **没贴**数据权限——你可以写入任意 `deptId`/`userId`，表单就是两个普通输入框。

厨房 `addDemo`：`POST /demo/demo`。页面提交成功后**永远** `runtime.success('修改成功')`，连新增也说「修改」——那是 L-077 的对照，不要当成后端文案。

#### 8. `edit`：`PUT /demo/demo`

权限 `demo:demo:edit`。`@Log UPDATE` + `@RepeatSubmit`（默认 5 秒，单位毫秒）。`@Validated(EditGroup.class) @RequestBody TestDemoBo`：还要 id。

`updateByBo`：convert → 空 TODO → `updateById`。`updateById` **贴了**数据权限。列表看不见的行，改它可能 0 行 → `toAjax` 走 `R.fail()`。

`@Version`：实体带着旧 version 更新；并发改同一行，后到的那次 rows=0，同样 fail。导入新行不带 version，吃库默认 0。Bo 有 `version` 字段但**没有**校验——页面投影丢掉 version 的话，乐观锁可能拿不到旧值。厨房 `updateDemo` 原样 POST/PUT `DemoForm`（表单初始没有 version）。口试点到「有乐观锁注解」即可；页面是否带 version 留给 L-077。

#### 9. `remove`：`DELETE /demo/demo/{ids}`

权限 `demo:demo:remove`。`@Log DELETE`。**没有** `@RepeatSubmit`。路径是 `Long[] ids`，`@NotEmpty`。厨房 `deleteDemo` 把数组 `join(',')`，测试锁 `/demo/demo/demo%2F7,demo%208,demo%2C9`。管理页可多选。

`deleteWithValidByIds(Arrays.asList(ids), true)`：

1. `isValid==true`（门卫写死 true）：`selectByIds`（带数据权限，`joinStr=AND`）。`list.size() != ids.size()` → `ServiceException("您没有删除权限!")`。**短路，零删除。** 这是「集合里有一条越权，整票作废」，不是跳过坏行。
2. 过了才 `deleteByIds`。Mapper **没有**给 `deleteByIds` 贴数据权限；闸在上一步。`@TableLogic` → 逻辑删 `del_flag`。

`isValid=false` 会跳过点名。本课 HTTP **从不**传 false。不要发明「管理端可关校验」。

#### 10. 同一厨师的侧门（不是八扇）

`TestBatchController` 门牌 `/demo/batch`：`add` / `addOrUpdate` / `remove`，字段 `TestDemoMapper testDemoMapper`，注释「为了便于测试 直接引入mapper」。往 `test_demo` 插 `orderNum=-1` 的一千行，再按 `orderNum=-1` 删。**不是**本课 `.*`，也不是成熟切片。L-003：存量允许的是 ServiceImpl→Mapper；入口再抱 Mapper 是棘轮拧松。

`TestEncryptController` `/demo/encrypt` 用 `TestDemoEncryptMapper` 写**同一张** `test_demo`，给 `testKey`/`value` 加 `@EncryptField`。deferred 玩具。不要把加密列说成本课 Vo 的行为。

`ITestDemoService.saveBatch` 只给导入打电话，没有自己的 HTTP。

## 图、表或文本图

**图 1：宏观八扇窗与 classic 抽屉**

```text
 bundle-full 才把 wta-demo 装进 admin
        │
        v
 TestDemoController     /demo/demo
        │
        ├─ GET  /list                 list         perm list     → selectVoPage（无 XML）
        ├─ GET  /page                 page         perm list     → XML customPageList
        ├─ POST /importData           importData   perm import   → Excel 读 → insertBatch
        ├─ POST /export               export       perm export   → selectVoList → Excel 流
        ├─ GET  /{id}                 getInfo      perm query    → selectById（无数据权限）
        ├─ POST /                     add          perm add      → ValidatorUtils + insert
        ├─ PUT  /                     edit         perm edit     → EditGroup + updateById（有数据权限）
        └─ DELETE /{ids}              remove       perm remove   → selectByIds 点名 → 逻辑删
                │
                v
        ITestDemoService
                │
                v
        TestDemoServiceImpl
                └─ TestDemoMapper  → test_demo（逻辑删 + version）
```

**图题 / caption：** 宏观同一门牌八扇窗，classic 三跳，装配电闸在 bundle。alt：八个 Java 方法映射到 `/demo/demo`；ServiceImpl 抱 Mapper；XML 只服务 `/page`；`getInfo` 不滤部门。

**文字等价物：** 已登录的管理员打到 `/demo/demo`。八扇都要对应权限字。门卫不写 SQL。厨师 `TestDemoServiceImpl` 自己抱 Mapper。只有自定义分页才执行 XML 的 `SELECT *`。详情不走数据权限；列表、导出、更新、删前点名才走。`bundle-core` 时整份模块不进 jar，不是某一扇还在。

**图 2：八扇合同对照表**

| HTTP | 动词 | Java | 权限 | 写库？ | 成功形状 |
| --- | --- | --- | --- | --- | --- |
| `/list` | GET | `list` | `demo:demo:list` | 否 | `R.ok(PageResult)`；SQL 是 wrapper |
| `/page` | GET | `page` | `demo:demo:list` | 否 | 同样 `PageResult`；SQL 是 XML |
| `/importData` | POST | `importData` | `demo:demo:import` | 是（过 Excel 校验） | `R.ok(分析句)`；**不看** insert 成败 |
| `/export` | POST | `export` | `demo:demo:export` | 否 | Excel 流，`void` |
| `/{id}` | GET | `getInfo` | `demo:demo:query` | 否 | `R.ok(Vo)`，无行也 ok |
| `/` | POST | `add` | `demo:demo:add` | 是 | `toAjax`；2 秒防重 |
| `/` | PUT | `edit` | `demo:demo:edit` | 是（过数据权限+版本） | `toAjax`；5 秒防重 |
| `/{ids}` | DELETE | `remove` | `demo:demo:remove` | 过点名才删 | 越权抛「您没有删除权限!」 |

**图题 / caption：** 矩阵八行与磁盘 1:1。alt：两扇 list 权限相同 SQL 不同；导入无页面扳机；导出是 POST 流。

**文字等价物：** 口试按这张表念：路径、动词、Java 名、权限串、动不动库、成功/失败信封。漏 `page`/`importData`、把导出说成 GET、把 `/list` 说成走 XML，格子不满。厨房覆盖 list/get/add/update/delete；导出另走 download；page 与 import 在表上仍在、页上没枪。

**图 3：数据权限贴在哪几枪**

```text
 带 @DataPermission(dept_id + user_id)
        ├─ selectVoPage          ← GET /list
        ├─ customPageList        ← GET /page
        ├─ selectVoList          ← POST /export
        ├─ updateById            ← PUT /
        └─ selectByIds           ← DELETE 点名（joinStr=AND）

 没贴
        ├─ selectById            ← GET /{id}
        ├─ insert / insertBatch  ← POST / 与导入
        └─ deleteByIds           ← 点名通过之后的逻辑删
```

**图题 / caption：** 权限注解在 Mapper 方法，不是 Controller。alt：详情可越权读；删除靠点名一票否决。

**文字等价物：** 数据权限不是类上总开关。看得见的列表、导出、改行、删前点名会滤部门/用户。拿着 id 看详情不过滤。新增和导入不过滤你填的部门号。真正删除语句本身不过滤，因为上一枪已经要求「集合里每一条你都看得见」。

**图的边界：** 不画树表 `parentId`（L-075）。不画富文本 OSS（L-076）。不保证以后会给 `getInfo` 补数据权限。不把 `TestExcelController` 模板文件画进本课导入。不把厨房投影丢字段说成后端 Vo 没有那些列。不把 `bundle-core` 说成运行时 `@ConditionalOnEnable`。

## 正例、反例与边界

**正例 1 — 打开练习卡本。** 持 `demo:demo:list` 的人打开 `demo/demo/index`。页 `listDemo({ pageNum, pageSize, testKey, value })` → `GET /demo/demo/list`。后端 `PageResult`，前端填表。

**正例 2 — 搜钥匙。** 查询 `testKey` 走 **like**；`value` 走 **eq**。只输入值 `测试`，对不上「测试数据」这种更长的串。不要把两列都说成模糊搜。

**正例 3 — 贴新卡。** 点新增，POST `/demo/demo`，AddGroup 校验五字段。2 秒内连点第二次：防重异常，不 insert。成功 `toAjax(true)`。页面 toast 却写「修改成功」。

**正例 4 — 改一张看得见的卡。** 行上「修改」先 `GET /{id}` 填表，再 PUT。EditGroup 要 id。`updateById` 带数据权限；自己范围内 rows>0。

**正例 5 — 撕多张。** 勾 2 行，确认后 `DELETE /demo/demo/id1,id2`。点名两条都看得见 → 逻辑删。页上也可单行删。

**正例 6 — 越权撕。** 三个 id 里有一条 `selectByIds` 看不见 → 抛「您没有删除权限!」，三条都还在（逻辑删标志不变）。

**正例 7 — 导出。** 点工具栏导出：`runtime.download` POST 风格打 `/demo/demo/export`，带当前筛选。收到 `.xlsx`。权限字与菜单 F 都在。

**正例 8 — 用 HTTP 客户端打自定义分页。** `GET /demo/demo/page` 与 `/list` 同一权限字、同一 Bo。响应形状相同，SQL 走 XML。页面不这样打。

**正例 9 — 用 HTTP 客户端导入。** 持 `demo:demo:import` 上传 multipart 字段名 `file`。行都合法 → 分析句「全部读取成功」+ `insertBatch`。缺「值」且 failFast → 抛分析异常，零插入。

**正例 10 — 关装配。** `-Pbundle-core` 打 admin 包，本类不在 classpath。不要指望还能 `GET /list` 拿空数组。

**反例 1 — 「八扇都走 XML。」** 只有 `page` 走 XML。`list` 是 `selectVoPage`。

**反例 2 — 「`/list` 和 `/page` 权限不同。」** 都是 `demo:demo:list`。

**反例 3 — 「导出是 GET，和树表一样。」** 单表是 POST。树表才是 GET（L-075）。

**反例 4 — 「页面能导入、能打 `/page`。」** 2026-09-17 都不能。

**反例 5 — 「厨房八枪。」** 五枪 + 下载管。page/import 不在 `DemoService`。

**反例 6 — 「这是 layered，Service 不许碰 Mapper。」** 登记 classic；Impl 必须能指到 `demoMapper`。

**反例 7 — 「新模块就该抄这一份。」** 登记表禁止把 demo 当 layered 反例。未登记默认五层。

**反例 8 — 「`TestBatchController` 也是成熟 CRUD。」** 越层入口；deferred 玩具柜旁边的压力测试窗。

**反例 9 — 「`getInfo` 也滤部门。」** `selectById` 没注解。

**反例 10 — 「`deleteWithValidByIds` 会在 SQL 里再滤一次。」** 删语句没贴；Valid 是点名比大小。

**反例 11 — 「导入失败会 `R.fail`。」** 读失败抛；读成功后 insert 成败被丢掉，仍 `R.ok(分析句)`。

**反例 12 — 「`validEntityBeforeSave` 会查唯一 key。」** 空 TODO。

**反例 13 — 「QueryGroup 会挡住空查询。」** Bo 上没有 QueryGroup 约束。

**反例 14 — 「OBJ-74 包含 `createDemoService`。」** 那是 OBJ-77。

**反例 15 — 「加密实体是另一张表。」** `TestDemoEncrypt` 也是 `test_demo`。玩具柜，不标 covered。

**边界 1 — getInfo 无行仍 200。** 编辑页要把 `res.data` 空值当失败，后端不会帮你 fail。

**边界 2 — 不带 PageQuery 等于整表。** `DEFAULT_PAGE_SIZE = Integer.MAX_VALUE`。

**边界 3 — like vs eq。** 只有 `testKey` 模糊；`value` 精确。

**边界 4 — 逻辑删。** `exists` / 列表受 `@TableLogic` 影响；已删行不再进点名集合，再删同一 id 可能点名失败（看不见）。

**边界 5 — 乐观锁。** version 对不上 → update 0 行 → fail。不是 601。

**边界 6 — RepeatSubmit 间隔单位。** add 写 `interval=2` **秒**；edit 默认 5000 **毫秒**。不要都说成 2 秒，也不要都说成 5 秒。

**边界 7 — 权限藏按钮 ≠ 授权。** `v-hasPermi` 藏新增；直接 POST 仍由 `@SaCheckPermission` 决定。`query`/`import` 连权限组都没收，REST 仍要那两串字。

**边界 8 — 导入不走 insertByBo。** 没有把 id 写回 Bo 的那一步，也没有单行 toAjax。

**边界 9 — 无本课 Java 测试类。** 2026-09-17 工作树 `wta-demo` 测试目录几乎只见富文本。本课证据是 Controller/Service/Mapper/DDL/前端对照，不是 `TestDemoControllerTest`。

**边界 10 — bundle vs 运行时电闸。** demo 没有 `warm-flow.enabled` 那种类注解。关模块是编译装配。workflow 课的电闸不要搬过来。

**边界 11 — 种子行。** `50-cde-base-dml.sql` 插入 13 行练习数据，`test_key` 从「测试数据权限」到「子节点99」，用来看数据权限，不是树（树在 `test_tree`）。

**边界 12 — `ExcelProperty index=5`。** 导出列顺序可能和表单字段顺序不一致。以注解为准。

## 变式与迁移

1. **和 L-003 对照。** classic 列允许 ServiceImpl 抱 Mapper。本课把那条 list 链扩成八扇。不要用本课去改登记表，也不要给 demo 加 UseCase。L-003 反例 5（`TestBatchController`）仍然成立：本课八扇门卫**没有**抱 Mapper，隔壁那份抱了。
2. **和 L-015 对照。** 用户柜台也是 classic + 导入导出。差别：用户导入有专用 Listener 写密码；单表导入是默认监听 + `saveBatch`。用户有数据权限闸在 Service；单表把 `@DataPermission` 直接贴 Mapper。
3. **和 L-067 对照。** 分类也是 classic 八扇里少一扇树、多一扇无权限字。分类 `list` 整表 List 不分页；单表两扇都分页。分类删除三道 601；单表删除越权是抛异常。分类有运行时电闸；单表靠 bundle。
4. **和即将到来的 L-075 对照。** 树表 `list` 回 `List` 不分页，**没有** import、**没有** `/page`。树表 `add` 用 `@Validated(AddGroup)`，不演示 `ValidatorUtils`。树表导出是 **GET**。先把单表八扇背熟，再去数树表六扇，不要提前把树表方法表背进本格。
5. **和 L-076 对照。** 富文本才碰 OSS 资产口。本课 Vo 没有附件。
6. **和 L-077 对照。** 厨房五枪、download 导出、投影丢字段、权限组漏 query/import、页面永远「修改成功」，都是前端课的格子。本课只借用它们当「谁扣扳机」的证据，**不**把 `A:createDemoService` / `A:createDemoWebDomain` 标 covered。
7. **和 layered 公告对照。** `NotifyNoticeController` 只认识 UseCase；`PageQuery` 在门口拆成两个整数。本课 `PageQuery` 一直传到 ServiceImpl。不要在 demo 里学公告再加 DAO。
8. **以后若要给 getInfo 补数据权限。** 应 override `selectById` 或改走 `selectVoByIds` 单元素，并决定「无权限」是空 200 还是异常。本课不改代码。
9. **以后若要页面导入。** 先给厨房加枪，再给 Vue 按钮，菜单补 F 型 `demo:demo:import`，权限组补同一串。后端窗已经在。
10. **以后若要导入看写库成败。** 应 `toAjax(saveBatch)` 或把 insert 计数写进分析句。现在分析句只反映读 Excel。
11. **新模块。** 不要复制本课包结构当默认。先登记 layered，再按五层建。要把某模块留在 classic，必须走登记表例外，不能口称「跟 demo 一样」。
12. **迁移口诀：** 先数八扇 HTTP → 再数 classic 三跳（门卫不抱 Mapper）→ 再数 `/list` 无 XML vs `/page` 有 XML → 再数数据权限贴在哪几个 Mapper 方法 → 再数厨房五枪 + download ≠ 八扇 → 最后数「样板 ≠ 模板」。跳步会出现「把 XML 安在 list 上」「把 Batch 当样板」「把 demo 抄进新模块」。

## 常见误区

1. **「OBJ-74 是整个 wta-demo 模块。」** 只认单表 Controller 八法。树、富文本、玩具柜都不是这一格。
2. **「CRUD 四个字。」** 磁盘八扇。漏 page/import/export 不满格。
3. **「列表走 Mapper XML。」** `/list` 不走。`/page` 才走。
4. **「两扇分页权限不同。」** 都是 list。
5. **「导出 GET。」** POST。
6. **「页面有导入。」** 没有。种子也没有 import 字。
7. **「厨房有 page/import。」** 没有。
8. **「详情也数据权限。」** 没有。
9. **「删除 SQL 带数据权限。」** 点名有，deleteByIds 没有。
10. **「`validEntityBeforeSave` 有唯一闸。」** 空 TODO。
11. **「add 和 edit 防重一样长。」** 2 秒 vs 5 秒。
12. **「add 用了 `@Validated(AddGroup)`。」** 用的是 `ValidatorUtils`。树表才是注解（L-075）。
13. **「导入失败 code 500 且分析句。」** failFast 抛；成功读后不管 insert。
14. **「这是新模块样板。」** 登记表禁止。
15. **「Controller 直持 Mapper 也算 classic 存量允许。」** 允许的是 ServiceImpl。`TestBatchController` 是拧松。
16. **「`TestDemoEncrypt` 是另一张练习表。」** 同一张 `test_demo`。
17. **「前端授权。」** `v-hasPermi` 藏按钮；REST 仍是 `@SaCheckPermission`。
18. **「给 demo 加五层才算现代化。」** 同一模块一种模式；登记表保持 classic。
19. **「`bundle-core` 还留着 demo 窗。」** 不留。
20. **「OBJ-74 包含 DemoPage。」** 页面是对照；格子是 Java 八法。

## 非评分暂停

打开磁盘，不要凭记忆默写路径。不要改文件。没有标准答案栏，没有分数。

1. 打开 `TestDemoController.java`。用手指点 8 个映射。圈 `list` 与 `page` 权限相同。圈 `importData` 的 multipart 与丢掉的 `saveBatch` 返回值。圈 `export` 是 POST + `void`。圈 `add` 的 `ValidatorUtils` 与 2 秒防重。圈 `edit` 的 `EditGroup` 与默认防重。圈 `remove` 的 `Long[]` 与 `true`。
2. 打开 `TestDemoServiceImpl.java`。圈唯一字段 `demoMapper`。圈 `queryPageList` vs `customPageList`。圈空的 `validEntityBeforeSave`。圈 `deleteWithValidByIds` 的 `list.size() != ids.size()`。
3. 打开 `TestDemoMapper.java` 与 `mapper/demo/TestDemoMapper.xml`。把五个 `@DataPermission` 勾出来。确认 XML **只有** `customPageList`。确认没有 `selectById` 的数据权限 override。
4. 打开 `TestDemo.java` / `TestDemoBo.java` / `TestDemoVo.java` / `TestDemoImportVo.java`。圈 `@TableName("test_demo")`、`@Version`、`@TableLogic`。圈 Bo 没有 QueryGroup。圈 ImportVo 的 `Long orderNum`。圈 Vo 的 `index = 5`。
5. 打开 `10-cde-base-ddl.sql` 的 `CREATE TABLE test_demo` 与 `50-cde-base-dml.sql` 菜单 1761400000000001500–1505、INSERT 十三行。确认 **没有** import 菜单字。
6. 打开 `frontend/packages/domains/demo/src/index.ts` 与 `web-domains/demo/src/index.ts`、`DemoPage.vue`。勾 list/get/add/update/delete；叉 page/import；圈 `runtime.download` 与权限组五串。打开 `TestBatchController.java` 圈直持 Mapper。不要改这些文件。

## 总结、词汇表与下一步

- **宏观八扇样板窗：** 同一门牌 `/demo/demo`，磁盘 8 个方法。classic 抽屉：门卫只打电话给 `ITestDemoService`；厨师抱 Mapper；XML 只给自定义分页。装配电闸是 `bundle-full` / `bundle-core`。
- **(a) `TestDemoController.list,page,importData,export,getInfo,add,edit,remove`：** `list` wrapper 分页、`page` XML 分页、`importData` Excel→insertBatch 且不管写库成败、`export` POST 流、`getInfo` 可空且无数据权限、`add` 工具校验 + 2 秒防重、`edit` 注解校验 + 5 秒防重 + 更新带数据权限、`remove` 点名一票否决后逻辑删。不要把 `saveBatch` HTTP、厨房 page/import、`TestBatchController`、加密实体算进这一格。
- **样板 ≠ 模板。** 成熟切片用来认 classic 形状；新模块默认 layered。入口抱 Mapper 不是存量允许。
- **两本分页本、两套权限缺口。** `/list` 与 `/page` 权限相同、SQL 不同。后端有 import/query 字；菜单缺 import；权限组缺 query 与 import。

词汇表：`TestDemoController` / `ITestDemoService` / `TestDemoServiceImpl` / `TestDemo` / `TestDemoBo` / `TestDemoVo` / `TestDemoImportVo` / `test_demo` / `customPageList` / `selectVoPage` / `@DataPermission` / `@RepeatSubmit` / `ValidatorUtils` / `ExcelBuilder` / `toAjax` / `@Version` / `@TableLogic` / classic / `bundle-full` / `bundle-core`。

下一步：L-075 把同一房间的树表六扇讲完（整表 List、GET 导出、有父节点）。L-076 才是富文本和 OSS 资产。L-077 才把厨房五枪、download、三键菜单对完。能力展示 Controller 保持 deferred。本课结束不发作业、不打分；要练习请之后主动激活 Homework。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-004 | `backend/wta-modules/**` Controller | 业务模块公开入口；本课类在 `wta-demo/controller` | `TestDemoController.java` | 2026-09-17 |
| S-006 | `frontend/packages/{domains,web-domains}` | 厨房五枪、资源标签、权限组、DemoPage 扳机 | `domains/demo`；`web-domains/demo` | 2026-09-17 |
| S-007 | `.agents/skills/engineering-standards/references/project/01-module-map.md` 与 `wta-admin/pom.xml` | bundle-full 含 demo；bundle-core 不含 | `wta-admin` profile `bundle-full` / `bundle-core` | 2026-09-17 |
| S-008 | `.agents/skills/engineering-standards/references/project/03-backend-module-modes.md` | `wta-demo` = classic；不得当 layered 反例；不得扩大越层 | 登记表 demo 行 | 2026-09-17 |
| S-010 | `release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql`、`50-cde-base-dml.sql` | 表 `test_demo`；十三行种子；菜单 C/F 无 import | `CREATE TABLE test_demo`；菜单 1761400000000001500–1505 | 2026-09-17 |
| S-L074-01 | `TestDemoController.java` | 八扇映射；权限字；ValidatorUtils vs Validated；2s/5s 防重；导入丢 saveBatch | 类上 `/demo/demo` 及各方法 | 2026-09-17 |
| S-L074-02 | `ITestDemoService.java`；`TestDemoServiceImpl.java` | classic 厨师；两分页；空 TODO；删前点名；insertBatch | queryPageList/customPageList/deleteWithValidByIds/saveBatch | 2026-09-17 |
| S-L074-03 | `TestDemo.java`；`TestDemoBo.java`；`TestDemoVo.java`；`TestDemoImportVo.java`；`TestDemoMapper.java`；`TestDemoMapper.xml` | 表名；校验组；Excel 注解；五个 DataPermission；XML 仅 customPageList | `@TableName`；Add/Edit；XML select | 2026-09-17 |
| S-L074-04 | `BaseController.toAjax`；`RepeatSubmit` / `RepeatSubmitAspect`；`ValidatorUtils`；`ExcelBuilder.ReadBuilder`；`DefaultExcelListener`；`DefaultExcelResult.getAnalysis` | 行数信封；防重 Redis；手动校验；failFast；分析句 | 各类型字段/方法 | 2026-09-17 |
| S-L074-05 | `PageQuery.java`；`BaseMapperPlus.selectVoById` / `insertBatch` | 默认整表页大小；详情走 selectById；导入走 Db.saveBatch | `DEFAULT_PAGE_SIZE`；selectVoById；insertBatch | 2026-09-17 |
| S-L074-06 | `TestBatchController.java`；`TestEncryptController.java`；`TestDemoEncrypt.java`；`TestExcelController.java` | 越层反例；同表加密玩具；Excel 模板另一门牌 | `/demo/batch` `/demo/encrypt` `/demo/excel` | 2026-09-17 |
| S-L074-07 | `frontend/packages/domains/demo/src/{index.ts,index.test.ts,test-demo/index.ts,transport.ts}` | 五枪 URL；无 page/import；投影 DemoVO；资源标签 | `createDemoService`；测试 `/demo/demo/list` | 2026-09-17 |
| S-L074-08 | `web-domains/demo/src/{index.ts,runtime.ts,test-demo/DemoPage.vue}` | 权限组五串；download 导出；无导入按钮；toast「修改成功」 | `demo-table` permissions；`handleExport` | 2026-09-17 |
| S-L074-09 | 子课 L-003 正例 3 / 反例 3 / 反例 5；course.md OBJ-74；coverage-matrix demo 行 | classic 列表链；禁止抄新模块；Batch 越层；能力展示 deferred | L-003；`course.md` OBJ-74；矩阵 TestDemo 行 | 2026-09-17 |
