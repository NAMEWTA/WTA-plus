---
lesson_id: L-076
objective_ids: [OBJ-76]
claimed_cells:
  - A:TestRichTextController.list,create,update,remove,assets,get
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: six-doors-on-disk
    minutes: 8
  - segment: normalize-validate-reconcile-assets
    minutes: 12
  - segment: visuals-and-worked-examples
    minutes: 7
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 4
expression_level: eli5
coverage_depth: deep
source_ids: [S-004, S-006, S-008, S-010, S-015, S-L076-01, S-L076-02, S-L076-03, S-L076-04, S-L076-05, S-L076-06, S-L076-07, S-L076-08, S-L076-09]
---

# Lesson 076：宏观六扇窗——TestRichTextController 与 OSS 资产解析

## 学完你能做什么

打开 `backend/wta-modules/wta-demo/src/main/java/org/namewta/demo/controller/TestRichTextController.java`，你能**口述富文本演示柜台的六扇公开窗**，再单独把「库里只存 `oss://<正整数>`、短时网址另开一扇授权窗」说完。口试名单就是矩阵这一格，符号以**磁盘**为准：

**`A:TestRichTextController.list,create,update,remove,assets,get`**：同一门牌 `/demo/rich-text` 上，**正好 6 个** Java 公开方法。chain 把六个名字写全了，不是「富文本 CRUD」四个字母，也不是「再加一扇上传」。口试按磁盘，不要漏 `assets`，不要把 Java 的 `get` 说成单表那种 `getInfo`，也不要把票房五扇或厨房工厂背成第七扇。

OBJ-76 原文只要你能口述 `TestRichTextController` 与 **OSS 资产解析**。本课还要把 **规范化 → 校验归属/临时态/类型 → 落库 → `reconcileReferences` → `GET /assets` 发短时地址**讲完，否则「能口述」会退化成背路径：

- 登记表：`wta-demo` 是 **classic**。调用链是 **Controller → `ITestRichTextService` → `TestRichTextServiceImpl`（自己抱 `TestRichTextMapper`，跨模块只打 `wta-api` 的 `OssService`）**。没有 UseCase，没有 DAO，**没有** Mapper XML。
- 登记表旁注仍在：「示例和集成演示；保持现有示例可运行；不得作为新模块 layered 反例。」本课是存量样板，**不是**抄作业模板。
- 六扇都贴 `@SaCheckPermission`。没有 `@SaIgnore`，没有 `@ConditionalOnEnable`，没有 `@RepeatSubmit`，没有 `@DataPermission`。关模块靠 **bundle**，所有权靠 **`createBy` + `clientPk`**，不是部门数据权限注解。
- 写三扇（`create` / `update` / `remove`）都是 **POST**，都罩 `@Log(..., isSaveRequestData = false, isSaveResponseData = false)`：HTML 又大又可能含附件名，操作日志不存请求体/响应体。
- 失败信封主要是 `ServiceException`（文案见机制段）。本课**没有**分类课那种 `R.warn` 601 黄灯，也**没有**单表那种 `toAjax` 行数信封——写成功直接 `R.ok(Vo)` 或 `R.ok()`。

本课**不宣称**你会拆 `TestDemoController` 八扇（L-074）、`TestTreeController`（L-075）、或把 `createDemoService` / `createRichTextService` / `createDemoWebDomain` 当本格 covered（L-077）。厨房 URL 只当**对照**：证明六扇窗里哪些被浏览器扣了扳机。上传票房 `SysOssUploadController` 是 L-027；浏览器 `createOssUploadClient.upload` 是 L-029；对象卡片柜 `SysOssController` 是 L-026。本课只认：**业务柜台怎么引用已经入库的临时对象，以及谁有权把 `oss://id` 换成短时 URL。**

2026-09-17 工作树先钉死**包边界**（口试先数窗，再数链）：

| 你可能以为的名字 | 磁盘事实 |
| --- | --- |
| 第七扇 HTTP 上传 | **没有。** 字节走 `/resource/oss/uploads/**`（L-027）+ 浏览器直传（L-029）。策略名 `richtext-image` / `richtext-audio` / `richtext-video` / `richtext-file` |
| `POST /`、`PUT /`、`DELETE /{id}` | **没有。** 写是 `POST /create`、`POST /{id}/update`、`POST /{id}/remove` |
| Java `getInfo` | **没有。** 本课详情方法就叫 **`get`**，路径 `GET /{id}` |
| 列表带回 HTML | **没有。** `list` 的 VO 是 `TestRichTextSummaryVo`（id/标题/version/updateTime），HTML 只在 `get` / `create` / `update` 的 `TestRichTextVo` |
| `get` 直接给 MinIO 签名 URL | **没有。** 详情 HTML 里是 `oss://12` 这种引用；短时地址走 **`GET /assets`** |
| 库里存 Base64 / 编辑器 live URL | **禁止。** `RichTextProcessor.normalize` 只接受 `oss://<正整数>`，和 `data-oss-id` 对得上 |
| 编辑器删掉一张图就调 OSS `remove` | **禁止。** 文档写明：只改 HTML；撤销/重做/并发保存靠保存时对账，不立刻撕仓库 |
| `@DataPermission` 滤部门 | **没有。** 列表/详情/改/删都是 `createBy == 当前用户` **且** `clientPk == 当前 Client` |
| Mapper XML | **没有。** `TestRichTextMapper` 只有 Java default `selectOwned`（**列表主路径不用它**） |
| `OssService.selectUrlByIds` | **过时兼容口。** 新代码用 `resolveAccessUrl`；本课 `assets` 逐个调它 |
| 厨房自己实现上传 | **没有。** `createRichTextService` 的 `assets` 是注入的端口；厅堂 `services.ts` 才把 `upload` 接到 `ossUploadClient`，把 `resolve` 接到本课 `/assets` |
| 给 demo 房间加 UseCase/DAO | **本课不发动。** 登记表 classic；同一模块一种模式 |
| 新模块抄这一份当 layered 反例 | **禁止。** 未登记新模块默认五层 |

## 先把宏观地图放在桌上

L-003 已经把 `wta-demo` 钉在 classic 列。L-074 走进同一栋实验室的**单表八扇**。L-029 已经把浏览器直传讲成：票房开票 → 浏览器 PUT 到 MinIO → `complete` 登记**临时对象**（`isTemp='Y'`，默认大约 24h 内没人认领就会过期）。本课走进**富文本柜台**：一篇自己的业务表 `test_rich_text`，六扇自己的 HTTP，仓库一律打电话给 `OssService`。

把这篇文档想成**一本带插图的练习作文**。作文纸上不贴真实照片，只写「仓库第 12 号箱子」。要看图，门卫另开一扇「领出门条」的窗，查过「这张图真的印在你这本作文本上 / 或者还是你刚上传、还没装订的临时箱」才给一张很快作废的地址。写进数据库的永远是 `oss://12`，不是签名 URL，也不是 Base64。

四条河都叫「富文本 / OSS」，货不一样：

| 名字听起来像 | 实际是什么 | 本课认不认 |
| --- | --- | --- |
| `TestRichTextController` 六扇 | `/demo/rich-text` 的 REST | **本课 (a)** |
| `GET /assets` 资产解析 | 业务授权后再 `resolveAccessUrl` | **本课 (a) 的核心窗** |
| `RichTextProcessor.normalize` | 纯 Java，不碰 Spring/OSS | 本课机制；工具在 `wta-common-richtext` |
| `OssService.reconcileReferences` | 把临时箱装订成引用 / 拆掉引用变回临时 | 本课厨师必喊；实现在 system 管家，格子仍是本课怎么喊 |
| `SysOssUploadController` 五扇 | 直传控制面 | L-027；本课只认策略名和双钥匙 |
| `createOssUploadClient.upload` | 浏览器 PUT | L-029；本课认它产出临时 `ossId` |
| `SysOssController.downloadUrl` | 管理卡片柜出门条 | L-026；权限字是 `system:oss:download`，**不是**本课 `assets` |
| `createRichTextService` / `RichTextPage` | 厨房 + 菜单页 | L-077 对照；本课不盖章那两格 |
| 厅堂 `components/Editor` | 另一份 wangEditor + OSS，**不**走 web-kit-rich-text | L-010 对照；不是本课消费者 |

2026-09-17 工作树：权威 Controller 就是上面那一份。权威接口 `ITestRichTextService` **正好六法**，和窗 1:1。权威实现 `TestRichTextServiceImpl`。权威表 `test_rich_text`，DDL 在 `release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql`。权威规范化 `RichTextProcessor`。权威跨模块合同 `backend/wta-api/.../OssService.java`。菜单种子 `50-cde-base-dml.sql` 的 `NAMEWTA-RICHTEXT-DML-001`。

```text
已登录的作文作者 / 管理员
        │  Admin-Token；无类上 @SaIgnore
        v
 /demo/rich-text/*          TestRichTextController     ← 本课六扇
        │
        v  classic
 ITestRichTextService
        │
        v
 TestRichTextServiceImpl
        ├─ TestRichTextMapper  → test_rich_text（逻辑删 + version）
        └─ OssService          → objectMetadata / snapshot / reconcileReferences / resolveAccessUrl
                │
                v
        OssLifecycleManager    sys_oss + sys_oss_ref（L-026/L-027 的管家，本课只打电话）
```

上传不经过本课门牌：

```text
编辑器选文件
        │
        v  厅堂 richTextAssets.upload（L-077 接线；本课对照）
 ossUploadClient.upload(policy=richtext-*)     L-029
        │
        v
 /resource/oss/uploads/**                      L-027
        │  complete 之后
        v
 sys_oss 一行临时对象（isTemp=Y，记下 uploader userId + clientPk）
        │  编辑器 HTML 只写 oss://id
        v
 POST /demo/rich-text/create|.../update        ← 本课装订
        │  reconcileReferences("test_rich_text", id, previous, current)
        v
 sys_oss_ref 把箱子钉在这篇作文上，isTemp=N
```

**类比：** 实验室里有一间「插图作文」教室。门口六扇窗：翻自己的作文本目录、交新本、改旧本、撕旧本、看某一本正文、凭编号领插图出门条。仓库在隔壁，本间教室没有「把照片塞进仓库」的窗。

**类比失效处：**

1. 「作文本」**不是**新模块样板。登记表写明 demo 不得当 layered 的反面教材。
2. 目录窗**不发正文**。想看插图必须另打 `assets`，不能指望 `GET /{id}` 已经把 `src` 换成 https。
3. 领出门条**不是** OSS 管理柜的 `download-url`。权限字、门牌、是否检查「这张图印在你的作文上」都不一样。
4. 刚上传、还没保存的图，出门条规则更严：只能领**自己的临时箱**。已经装订的图，必须带作文本号。
5. `bundle-core` 整栋实验室不进包，不是某一扇 404 开关。
6. 别的模块不要 Maven 依赖 `wta-demo` 去「复用富文本」。新业务按 layered 自己建房间，规范化工具用 `wta-common-richtext`，仓库用 `OssService`。

## 核心概念与机制

### 直觉讲解

小孩子版只记十二句：

1. **一块牌子，六扇窗。** `@RequestMapping("/demo/rich-text")`。矩阵六个名字 = 磁盘 6 个方法。没有上传窗，没有 Excel 窗。
2. **classic，不是五层。** 门卫 → 接口 → `ServiceImpl` 抱 Mapper + `OssService`。不要发明 `TestRichTextUseCase`。不要在 demo 里抱 `SysOssMapper`。
3. **纸上只写箱号。** 保存前 `RichTextProcessor.normalize`：图片/音频/视频/带 `data-oss-id` 的附件，`src`/`href` 必须是 `oss://<正整数>`，还要和 `data-oss-id` 相同。Base64、https 图、`poster`/`srcset` 一律拒绝。
4. **先验货，再落库，再装订。** `validateAssets` 查：箱子在不在、是不是你和当前 Client 传的、新引用必须仍是临时箱（或这篇旧正文里已经有过）、种类和 `contentType` 对得上。然后 insert/update，最后 `reconcileReferences`。
5. **删节点 ≠ 删箱子。** 编辑器把 `<img>` 拿掉，只改 HTML。真正 `unbind` 发生在**下一次保存**的对账。立刻调 OSS 删除会把别人的撤销/并发保存搞坏。
6. **出门条是第六扇，不是详情附赠。** `GET /assets?ossIds=1,2&richTextId=3`。有作文本号：必须是你的本，且编号都印在**已落库**的 HTML 里，才给已装订的箱子发 URL。没有作文本号：只给**你的临时箱**。
7. **解析失败多数变 `unavailable`，不是 500。** `resolve` 里 `objectMetadata` / 签名抛错 → 这条资源 `status=unavailable`，不把别人的箱子存不存在说出来。但「作文本不是你的」或「这个 id 根本没印在已保存正文里」会**整单抛** `ServiceException`。
8. **所有权两把钥匙，不是部门注解。** 列表、详情、改、删、带 `richTextId` 的资产窗，都是当前 `userId` + `clientPk`。换一个 Client 登录，同一用户也看不见这本作文本。
9. **写三扇都要乐观锁（除了新建）。** `update` / `remove` 带 `version`；对不上就「富文本已被其他请求修改，请刷新后重试」。新建不收 version。
10. **规范化工具不碰仓库。** `wta-common-richtext` 零 Spring。归属、临时态、对账、签名全在业务厨师 + `OssService`。
11. **上传是隔壁两把钥匙。** 策略 `richtext-*` 的 `required-permission` 是 `common:richtext:upload`；票房方法上还有 `system:oss:upload`。本课六扇只要 `demo:richtext:*`。菜单 F 型有 `common:richtext:upload`，**没有**把 `system:oss:upload` 写进富文本权限组。
12. **装配电闸在 pom。** 默认 `bundle-full` 才把 `wta-demo` 塞进 admin jar；`bundle-core` 整栋实验室不进包。

**类比失效边界：** 「插图作文」**不**覆盖管理端 `Editor/index.vue` 那份厅堂私货。类比也**不**等于「前端六个按钮都在厨房对象上」——上传在端口上，不在 `TestRichTextController`。类比还不等于「带了 `richTextId` 就能预览刚插入、尚未保存的新图」——会员资格看的是**库里那份 HTML**，不是编辑器内存。类比更不等于「抄这一份就能开新模块」。

### 精确定义与 English term

| 中文口头 | English term | 精确定义（本课，以 2026-09-17 工作树为准） |
| --- | --- | --- |
| 富文本演示控制器 | `TestRichTextController` | classic `@RestController`，`/demo/rich-text`，继承 `BaseController`。六个公开映射。类上 `@Validated` `@RequiredArgsConstructor`，**无** `@ConditionalOnEnable`、**无** `@SaIgnore`、**无** `@RepeatSubmit` |
| 经典三层 | classic | 登记表：`wta-modules/wta-demo`。Controller → Service / ServiceImpl → Mapper。ServiceImpl 允许持 Mapper；跨模块只经 `OssService`；不得当新模块 layered 反例 |
| 规范化 | `RichTextProcessor.normalize` | 无状态纯 Java。上限 1 MiB、1 万节点、100 个资源。媒体协议只允许 `oss`；同一 `ossId` 不能既当图又当音频。外链 `<a href=http(s)>` 补 `rel=noopener noreferrer`。失败抛 `IllegalArgumentException`，厨师改写成 `ServiceException` |
| 规范内容 | `RichTextContent` | record：`html`（可再 normalize 的片段）、`text`、`assets`。`ossIds()` 从 assets 去重抽出 |
| 资源引用 | `RichTextAsset` / `RichTextAssetKind` | `ossId>0` + 种类 `IMAGE` / `AUDIO` / `VIDEO` / `ATTACHMENT`。附件来自带 `data-oss-id` 且 `href=oss://id` 的 `<a>` |
| 保存请求 | `TestRichTextBo` | `title` `@NotBlank`；`html` **`@NotNull` 不是 `@NotBlank`**（空串能过校验，再交给 normalize）；`version` 无注解，新建可空，更新由服务再查 |
| 删除版本 | `TestRichTextVersionBo` | 只有 `version` `@NotNull("版本不能为空")` |
| 列表摘要 | `TestRichTextSummaryVo` | `richTextId` / `title` / `version` / `updateTime`。**没有 html**。Mapper 的 `BaseMapperPlus<Entity, SummaryVo>` 泛型就是它 |
| 详情视图 | `TestRichTextVo` | `html` 用 `@AutoMapping` 对 `contentHtml`。厨师 `view()` **手抄**字段，详情主路径不靠 MapStruct |
| 资产出门条 | `TestRichTextAssetVo` | `ossId`（**String**）、`status`=`available`/`unavailable`、`url`、`expiresAt`、`fileName`、`contentType`。unavailable 只填 id+status |
| 实体 | `TestRichText` | `@TableName("test_rich_text")`，`@TableId("rich_text_id")`，`clientPk`，`title`，`contentHtml`，`@Version`，`@TableLogic delFlag`（Java `Long`；DDL `char(1)`） |
| 引用类型 | `REF_TYPE` | 实现类常量 `"test_rich_text"`，必须是真实物理表名，交给 `reconcileReferences` |
| 对账 | `OssService.reconcileReferences` | 同一动态数据源事务里：current−previous → `bind`（临时变正式）；previous−current → `unbind`（引用为 0 则变回临时并写过期时间）。调用前必须已做业务授权 |
| 临时对象 | temporary object | `snapshot(ossId).temporary()==true`，通常 `sys_oss.isTemp='Y'`。直传 complete 之后、尚未 bind 之前 |
| 对象元数据 | `OssObjectMetadata` | `objectMetadata`：**不**返回访问 URL。含 `contentType`、`uploaderUserId`（行上 `createBy`）、`uploaderClientPk`（`ext1`） |
| 访问地址 | `OssAccessUrl` | `resolveAccessUrl`：PUBLIC 无到期；PRIVATE 短时签名。本课 `assets` 在授权后再调 |
| 生命周期快照 | `OssLifecycleSnapshot` | `temporary` + `expireTime` + 当前引用列表。本课用它判断「还是不是临时箱」 |
| 直传策略 | `richtext-*` | yaml：`richtext-image/audio/video/file`，`expected-access-policy: PRIVATE`，`required-permission: common:richtext:upload`。附件 kind 在厅堂映射成 `richtext-file` |
| 成熟切片 | mature demo slice | course 范围内的 TestDemo / TestTree / TestRichText。其余 demo Controller deferred |

### 机制/因果链

#### 1. 类上合同：门牌、电话、没有电闸注解

文件：`.../controller/TestRichTextController.java`。

类注解：`@Validated` `@RequiredArgsConstructor` `@RestController` `@RequestMapping("/demo/rich-text")`。

构造注入**只有** `ITestRichTextService service`。全文搜不到 Mapper、`OssService`、`LoginHelper`。门卫不写 SQL，也不直接领出门条。

类上没有 `@ConditionalOnEnable`。实验室整栋进不进 admin，看 `wta-admin/pom.xml`：`bundle-full`（默认激活）依赖 `wta-demo`；`bundle-core` **没有**这一行。

类上没有 `@SaIgnore`。六扇都要会话 + 各自权限字。

`wta-demo/pom.xml` 依赖 `wta-common-richtext` 和 `wta-api`（从而有 `OssService`），**不**依赖 `wta-system`。

磁盘顺序：`GET /assets` 写在 `GET /{id}` **上面**。即便颠倒，路径变量是 `Long`，「assets」也绑不成 id；仍按「具体路径在前」记。

#### 2. `list`：`GET /demo/rich-text/list`

权限 `demo:richtext:list`。无 `@Log`。入参只有 `PageQuery`（`pageNum` / `pageSize` / 排序列），**没有**标题搜索 Bo。

`PageQuery.build()`：页码默认 1；**页大小默认 `Integer.MAX_VALUE`（注释写「默认查全部」）**。演示页会传 `pageNum=1, pageSize=50`。你用 HTTP 客户端不带分页参数，等于一次捞出该用户在该 Client 下所有未删文档。

厨师：`requireUser()` → `createBy=userId` 且 `clientPk=当前 Client` → `mapper.selectVoPage` → `PageResult<TestRichTextSummaryVo>` → `R.ok`。`orderByDesc(updateTime)`。

这条链 **不执行** XML（根本没有 XML）。也**不调用** Mapper 上的 default `selectOwned`（那是同一套 wrapper 的死代码旁路）。

`selectVoPage` 把实体转成 SummaryVo：口试要能说「列表没有 `contentHtml`」。模块测试 `TestRichTextListAutoMapperTest` 钉的就是这道 MapStruct 缝。

谁在扣扳机：`RichTextPage.vue` `runtime.service.richText.list({ pageNum:1, pageSize:50 })` → 厨房 `GET /demo/rich-text/list`。对照用，本课不认前端格子。

#### 3. `get`：`GET /demo/rich-text/{id}`

权限 `demo:richtext:query`。`@NotNull` 在路径变量上（类上 `@Validated` 让它生效）。

`owned(id)`：`requireClient()` + `selectOne(richTextId + createBy + clientPk)`。找不到 → `ServiceException("富文本不存在或无权访问")`。**不是**单表那种 `R.ok(null)`。

`view(entity)` 手抄：`richTextId`、`title`、`html=contentHtml`、`version`、`updateTime`。此时 HTML 仍是规范片段，`<img src="oss://12">`，**没有**签名。

和单表对照（L-074 已讲、本课只点差）：单表 `getInfo` 走 `selectById`、**不**滤部门、无行也 200。本课详情**必须**是作者 + Client，否则直接抛。

管理页「编辑」先打这一枪。web-domain 权限组 **有** `demo:richtext:query`（和单表组没收 `demo:demo:query` 相反）。菜单种子也有 F 型 query，备注写「富文本详情与资源访问」——同一把 `query` 钥匙还开 `assets`。

#### 4. `create`：`POST /demo/rich-text/create`

权限 `demo:richtext:add`。`@Log INSERT`，**不存**请求/响应体。`@Valid @RequestBody TestRichTextBo`：标题非空白，html 非 null。**没有** `@RepeatSubmit`——连点两次可能两篇。演示页用 `editorState.valid` 挡住「上传未完成」，不挡双击保存。

门卫之后厨师 `@DSTransactional`：

1. `requireClient()`。没有 Client →「当前会话缺少 Client」。
2. `normalize(html)`。超 1 MiB / 节点过多 / Base64 / 外链图 / 样式 `url(` / 同一 id 两用途 → `ServiceException(原句)`。
3. `validateAssets(assets, null)`。新建没有「旧正文」，**每一张图都必须仍是临时对象**，且上传者 = 当前用户 + 当前 Client，且 kind 对得上 `contentType`。
4. `new TestRichText()`：`clientPk`、`title.strip()`、`contentHtml=content.html()`。不设 version（吃列默认 / MP 填充）。`insert<=0` →「富文本保存失败」。
5. `reconcile(null, entity, content)`：previous 空，current = 规范 HTML 里的 ossIds。管家 `bind`：临时箱 `isTemp` 改为 `N`，写下 `sys_oss_ref(ref_type=test_rich_text, ref_id=主键)`。
6. `return view(entity)` → `R.ok(Vo)`，带规范 HTML 和主键。

对账失败会把 insert 一起回滚（`@DSTransactional`）。不要口述成「先对账再 insert」——磁盘是 **insert 成功才 reconcile**，靠事务兜。

#### 5. `update`：`POST /demo/rich-text/{id}/update`

权限 `demo:richtext:edit`。`@Log UPDATE`，同样不存体。路径 `id` + body `TestRichTextBo`。

厨师：

1. `bo.version == null` →「版本不能为空」。注意：Bo **没有**给 version 贴 `@NotNull`，这句是服务自己喊的；删除窗才用 `TestRichTextVersionBo` 的注解。
2. `owned(id)` 取出旧行（含旧 HTML、旧 `clientPk`）。
3. normalize 新 HTML；`validateAssets(assets, old)`。旧正文里已经出现过的 ossId，即使现在已经不是临时箱，也允许继续引用。**新出现**的 id 必须仍是临时箱。别人的箱子、种类不对、元数据读不到，一律抛。
4. 新实体只带 `richTextId`、strip 过的标题、新 HTML、`version=bo.version`、`clientPk=old.clientPk`（**不能**靠请求改 Client）。
5. `updateById<=0` →「富文本已被其他请求修改，请刷新后重试」。乐观锁或行已消失都走这句。此时**不会**对账。
6. 返回前 `entity.setVersion(bo.getVersion() + 1)`——手里的对象不会自动带上 MP 加一后的值，厨师手写给前端。
7. `reconcile(old, entity, content)`：新图 bind，拿掉的图 unbind。引用归零的箱子变回临时并写过期时间，**不是**立刻物理删。

#### 6. `remove`：`POST /demo/rich-text/{id}/remove`

权限 `demo:richtext:remove`。`@Log DELETE`，不存体。**不是** `DELETE` 动词。Body 只有 `{ version }`。

厨师 `@DSTransactional`：

1. `owned(id)`。越权/已逻辑删 →「不存在或无权访问」，还没碰到 version。
2. 只带主键 + version 的实体 `deleteById`。`@TableLogic` → 改 `del_flag`。`@Version` 进 WHERE。0 行 → 同一句「已被其他请求修改」。
3. 用**旧 HTML** normalize 出 ossIds，`reconcileReferences(REF_TYPE, id, ossIds, List.of())`：全部 unbind。owner 登记表 `deleteStrategy`：logical delete 时把引用对成空集。`restoreStrategy: not supported`——本课没有恢复窗，不要发明「取消删除会自动 bind 回来」。

#### 7. `assets`：`GET /demo/rich-text/assets`

权限 **同样** `demo:richtext:query`。查询参数：`ossIds` **必填**字符串，`richTextId` 可选 Long。

`parseIds`：

- null / 空白 → 空集合，后面 `R.ok([])`。
- 按逗号切、trim、`Long.valueOf`、**去重**。
- 超过 **100** 个，或任一 `id <= 0` →「OSS ID 参数无效」。
- 非数字 → 同一句「OSS ID 参数无效」（`NumberFormatException` 被包掉）。

若带了 `richTextId`：

1. `owned(richTextId)`——不是你的本，整单抛「不存在或无权访问」。
2. 把**已保存** HTML normalize，看 `content.ossIds().containsAll(请求的 ids)`。失败 →「资源不属于当前富文本」。注意：这是**库里的作文**，不是编辑器里还没保存的草稿。
3. 之后 `resolve(id, referencedDocument=true)`：**跳过**临时态检查。已装订的箱子可以领出门条。

若不带 `richTextId`：直接 `resolve(id, false)`。此时若 `snapshot.temporary()` 不是 true → **这条**返回 `unavailable`（不抛）。用来给「刚直传完、作文还没交」的预览。

`resolve` 内部：

1. `objectMetadata(id)`。
2. 上传者 userId、clientPk 必须等于当前会话。对不上 → `unavailable`（不抛「不属于你」，避免用 500/403 探测别人的 id）。
3. 若不是「已印在指定作文上」的模式，且不是临时箱 → `unavailable`。
4. `resolveAccessUrl(id)` → `available` + url + expiresAt + fileName + contentType。
5. 任何 `RuntimeException`（对象不存在、元数据不齐、存储未就绪、PENDING 删除）→ `unavailable`。

和保存路径的差别：`validateAssets` 把 `objectMetadata` 的异常**改写成**「富文本资源不存在」，种类不对是另一句；`assets` 预览则降级为 unavailable 列表。口试不要说「两扇对坏 id 行为一样」。

谁在扣扳机（对照）：厅堂 `richTextAssets.resolve` 打本窗，`ossIds` join 逗号，带上 `richTextId`。编辑器上传成功后也会 `resolve([新id], { richTextId: editingId })`。若正在改旧本、新 id 还没进库，本窗会抛「资源不属于当前富文本」；编辑器 **catch 掉**，占位仍写 `oss://id`，等保存后再由 Viewer 解析。不要把「编辑中立刻能预览新图」说成后端保证。

#### 8. 规范化与类型闸（厨师每次写库都跑）

`RichTextProcessor` 要点（测试 `RichTextProcessorTest` 已钉）：

- `<h1 onclick>`：clean 掉事件，保留标题。
- `<img data-oss-id='12' src='oss://12'>` 进 assets。
- `data:image/png;base64,...` 拒绝。
- 同一 id 又是 img 又是 audio 拒绝。
- `style='background:url(javascript:...)'` 拒绝。
- `data-oss-id` 超出 Long（`9223372036854775808`）拒绝。
- 再 normalize 一次，HTML 不变（幂等）。
- `<audio ... src='oss://2'>` 与 `<a data-oss-id='3' href='oss://3'>` 都会进 `ossIds()`。
- 普通 https 外链保留，并补 `noopener`。

`kindMatches`：`image/*` / `audio/*` / `video/*`；附件是**三者都不是**（pdf/zip/octet-stream/text 才能当 ATTACHMENT）。`contentType==null` 直接类型不匹配。yaml 里 `richtext-image` 允许 jpeg/png/gif/webp，**不含** svg；`editor-image` 那条是厅堂私货编辑器的策略，不是本课 `richtext-*`。

`validateAssets` 还有一句口语要能复述：「新增内容只能引用临时上传资源」。意思是：不能把别人已经装订、或你自己另一篇已经 bind 的正式对象，偷偷写进新 HTML。除非它出现在**这篇**更新前的旧 HTML 里（换段落、保留原图）。

#### 9. 同一厨师的侧门（不是六扇）

`TestRichTextMapper.selectOwned`：Java default，和生产 `list` 条件几乎一样，但返回实体列表不是 SummaryVo。2026-09-17 **没有** Controller/Service 调用。口试不要说「列表走 selectOwned」。

`TestRichTextOssOwnerUnitTest`：合同目录 `business-oss-owners.json` 把本课登记为 owner（carrier=`content_html`，encoding=`canonical-html-oss-id-references`）。测试方法本身只断言一段字符串 `contains("oss://")`，**不是**对账集成测试。真闸在 ServiceImpl + `RichTextProcessorTest`。

`ITestRichTextService` 没有第七法。没有 import/export，没有 tree，没有 batch。

## 图、表或文本图

**图 1：宏观六扇窗与 classic 抽屉**

```text
 bundle-full 才把 wta-demo 装进 admin
        │
        v
 TestRichTextController     /demo/rich-text
        │
        ├─ GET  /list                 list     perm list    → selectVoPage SummaryVo（无 HTML、无 XML）
        ├─ POST /create               create   perm add     → normalize → validate(临时) → insert → reconcile
        ├─ POST /{id}/update          update   perm edit    → owned + version → validate → updateById → reconcile
        ├─ POST /{id}/remove          remove   perm remove  → owned + version → 逻辑删 → reconcile 到空集
        ├─ GET  /assets               assets   perm query   → 解析短时 URL（本课核心窗）
        └─ GET  /{id}                 get      perm query   → owned → 规范 HTML（仍是 oss://）
                │
                v
        ITestRichTextService
                │
                v
        TestRichTextServiceImpl
                ├─ TestRichTextMapper  → test_rich_text
                └─ OssService          → 元数据 / 临时态 / 对账 / 签名
```

**图题 / caption：** 宏观同一门牌六扇窗，classic 三跳，仓库只经 `OssService`。alt：六个 Java 方法；列表无 HTML；写三扇都是 POST；上传不在这张图里。

**文字等价物：** 已登录的人打到 `/demo/rich-text`。六扇都要对应权限字。门卫不写 SQL、不签名。厨师抱 Mapper，跨模块只喊 `OssService`。列表只给目录卡片。正文里的图号要另开 `assets` 才变成短时 https。新建/修改/删除在同一事务里对账引用。`bundle-core` 时整份模块不进 jar，不是某一扇还在。

**图 2：六扇合同对照表**

| HTTP | 动词 | Java | 权限 | 写库？ | 成功形状 |
| --- | --- | --- | --- | --- | --- |
| `/list` | GET | `list` | `demo:richtext:list` | 否 | `R.ok(PageResult<SummaryVo>)`，无 html |
| `/create` | POST | `create` | `demo:richtext:add` | 是 | `R.ok(Vo)`；日志不存体 |
| `/{id}/update` | POST | `update` | `demo:richtext:edit` | 是（过所有权+版本） | `R.ok(Vo)`，version 手加一 |
| `/{id}/remove` | POST | `remove` | `demo:richtext:remove` | 逻辑删 + 对空账 | `R.ok()`；不是 DELETE 动词 |
| `/assets` | GET | `assets` | `demo:richtext:query` | 否 | `R.ok(List<AssetVo>)`；单条可 unavailable |
| `/{id}` | GET | `get` | `demo:richtext:query` | 否 | `R.ok(Vo)`；无权抛，不是 ok(null) |

**图题 / caption：** 矩阵六行与磁盘 1:1。alt：写窗全是 POST；query 同时开详情和资产；没有上传行。

**文字等价物：** 口试按这张表念：路径、动词、Java 名、权限串、动不动库、成功/失败信封。漏 `assets`、把详情说成 `getInfo`、把写窗说成 PUT/DELETE、把上传算进六扇，格子不满。厨房六枪 URL 与本表对齐；真正传文件的枪在厅堂端口，不在本表。

**图 3：资产解析两条走廊**

```text
 GET /assets?ossIds=1,2[,3]
        │
        ├─ 带了 richTextId
        │     owned(文档) 失败 ──────────────► 整单抛「不存在或无权访问」
        │     已保存 HTML 的 ossIds 不含请求 id ► 整单抛「资源不属于当前富文本」
        │     通过后 resolve(id, referenced=true)
        │           跳过「必须临时」检查（已装订也可领条）
        │
        └─ 没带 richTextId
              resolve(id, referenced=false)
                    不是你的上传者/Client ──► 该条 unavailable
                    不是临时箱 ─────────────► 该条 unavailable
                    元数据/签名抛错 ────────► 该条 unavailable
                    否则 available + 短时 url
```

**图题 / caption：** 有作文号看已装订的图；没作文号只看自己的临时箱。alt：会员资格对照的是库里 HTML，不是编辑器草稿。

**文字等价物：** 出门条窗先切「有没有作文本号」。有号：先证明这本是你的，再证明这些箱号已经印在保存稿上，然后连正式箱也签名。没号：只给还没装订、并且是你传的临时箱。单条坏箱在预览里变成 unavailable；整本不是你的，或拿草稿里的新 id 去冒充已保存引用，会整单失败。编辑器若在改旧本时预览新图，可能撞上第二句，前端自己吞掉，后端不保证 live 预览。

**图 4：保存时验货与装订**

```text
 浏览器 HTML（可能还带着签名 URL / 音频 span 占位）
        │  厨房 canonicalize → 只剩 oss:// + data-oss-id
        v
 RichTextProcessor.normalize
        │  纯 Java 闸：协议、大小、节点、同 id 两用途
        v
 validateAssets
        │  objectMetadata：存在、是你、Client 对
        │  新 id 必须 snapshot.temporary
        │  kind vs contentType
        v
 insert / updateById（带 version）
        v
 reconcileReferences("test_rich_text", 主键, 旧 ossIds, 新 ossIds)
        │  新的 bind → isTemp=N
        │  拿掉的 unbind → 引用 0 则回到临时+过期
        v
 库里 content_html 仍是 oss:// ，没有 https
```

**图题 / caption：** 先规范，再验货，再落库，再装订。alt：对账在写库之后、同一事务；删除节点不在这一步调 OSS 删除接口。

**文字等价物：** 交本作文本时，门卫先把非法地址撕掉（规范失败就整篇打回）。再去仓库核对每只箱子：是你的、种类对、新箱子还挂着临时标签。纸写进柜子以后，才把箱子从「待认领」钉到这篇作文上。你在编辑器里撕掉一张图，此时仓库那只箱子还钉着；要等你点保存，对账发现旧集合里有、新集合里没有，才解开钉子，让它重新变成会过期的临时箱。

**图的边界：** 不画票房 init/sign/complete（L-027）。不画浏览器 PUT 进度条（L-029）。不画 `SysOssController` 四扇。不把 `createRichTextService` 画成本格 covered。不保证以后会给 `assets` 改成「对照编辑器草稿」。不把 `selectOwned` 画进主走廊。不把厅堂 `Editor/index.vue` 画成第二消费者。

## 正例、反例与边界

**正例 1 — 翻自己的目录。** 持 `demo:richtext:list` 打开 `demo/rich-text/index`。页 `list({pageNum:1,pageSize:50})` → `GET /list`。只看见当前用户 + 当前 Client 的摘要行，没有 HTML。

**正例 2 — 先直传再交新本。** 编辑器插入图片：厅堂用策略 `richtext-image` 走 L-027/L-029，得到临时 `ossId=12`。保存体经 canonicalize 变成 `<img data-oss-id="12" src="oss://12">`。`POST /create`。normalize 收集 12，校验临时且是你的，insert，bind。响应 Vo 的 html **仍然**是 `oss://12`。

**正例 3 — 看正文再领条。** `GET /{id}` 拿到规范 HTML。Viewer 抽出 ossIds，再 `GET /assets?ossIds=12&richTextId={id}`。因为 12 已印在保存稿上，`referencedDocument=true`，正式箱也能 `available`。页面把 `src` 换成短时 URL；库里那一行 HTML 不变。

**正例 4 — 没交本先预览新图。** 新建（没有 `editingId`）时 `resolve` 不带 `richTextId`。12 仍是临时箱且上传者是你 → `available`。同一 id 若已经被另一篇 bind、不再 temporary，这条会 `unavailable`。

**正例 5 — 改本换图。** 旧稿有 12，新稿有 12 和刚传的 13。`validateAssets`：12 在 previousIds 里，允许非临时；13 必须仍临时。`updateById` 带旧 version。reconcile bind 13、12 保留。返回 version+1。

**正例 6 — 改本拿掉图。** 新 HTML 不再提 12。校验只看还在的资源。对账 unbind 12。12 若没有别的引用，变回临时并开始倒计时。**没有**调用 `SysOssController.remove`。

**正例 7 — 乐观锁打回。** 两个人打开同一篇，version=3。先到的更新成功，库变成 4。后到的仍带 3 → `updateById` 0 行 →「请刷新后重试」，**不对账**，后到者插入的新临时箱也不会被这篇 bind。

**正例 8 — 撕本。** `POST /{id}/remove` body `{version:4}`。先 owned，再带 version 逻辑删，再把旧 ossIds 对成空集。列表再也看不到（`@TableLogic`）。再 `GET /{id}` →「不存在或无权访问」。

**正例 9 — 规范拒绝 Base64。** 直打 `POST /create`，html 里 `<img src="data:image/png;base64,aa" data-oss-id="1">`。normalize 抛，厨师变成 `ServiceException`，零 insert，零 bind。

**正例 10 — 类型不对。** 箱子 contentType=`application/pdf`，HTML 却用 `<img data-oss-id src=oss://...>`。过 normalize，过归属，死在 `kindMatches` →「富文本资源类型不匹配」。

**正例 11 — 出门条把坏 id 藏起来。** 不带 `richTextId`，`ossIds=999` 不存在。`resolve` catch → `{ossId:"999", status:"unavailable"}`，HTTP 仍 200。不要指望靠这扇窗枚举仓库。

**正例 12 — 关装配。** `-Pbundle-core` 打 admin 包，本类不在 classpath。不要指望还能 `GET /list` 拿空数组。

**反例 1 — 「六扇是标准 REST：POST /、PUT /、DELETE。」** 三扇写全是 POST，路径带 `create/update/remove`。

**反例 2 — 「详情方法叫 `getInfo`。」** 磁盘是 `get`。那是单表/请假的名字。

**反例 3 — 「列表也带正文，浏览器直接显示图。」** SummaryVo 无 html；有 html 的窗也不带签名 URL。

**反例 4 — 「`GET /{id}` 已经把 src 换成 https。」** 没有。短时地址只在 `assets`。

**反例 5 — 「上传也是本课一扇。」** 票房在 `/resource/oss/uploads`。本课零 `MultipartFile`。

**反例 6 — 「`richtext-image` 只需 `common:richtext:upload`。」** 还要跨过票房方法上的 `system:oss:upload`（L-027 反例 17）。本课六扇注解里甚至没有 upload 字。

**反例 7 — 「assets 就是 `SysOssController.listByIds` / `downloadUrl`。」** 门牌、权限、是否检查作文会员资格都不同。管理柜还会把列表 URL 抹掉。

**反例 8 — 「新代码继续用 `OssService.selectUrlByIds`。」** 合同标 deprecated；本课走 `resolveAccessUrl`。

**反例 9 — 「编辑器删图必须调 OSS 删除，否则会泄漏。」** 文档和实现都禁止立刻删。靠保存时 unbind；零引用后变回临时，再交给生命周期清理。

**反例 10 — 「带 `richTextId` 就能预览尚未保存的新 id。」** `containsAll` 对照的是**已保存** HTML。新 id 会整单「资源不属于当前富文本」。

**反例 11 — 「这是 layered，Service 不许碰 Mapper。」** 登记 classic；Impl 必须能指到 `mapper` 和 `ossService`。

**反例 12 — 「新模块就该抄这一份。」** 登记表禁止把 demo 当 layered 反例。

**反例 13 — 「所有权靠 `@DataPermission` 滤部门。」** 本课是 `createBy` + `clientPk`。没有部门注解。

**反例 14 — 「无权看详情会 `R.ok(null)`。」** 抛「不存在或无权访问」。

**反例 15 — 「OBJ-76 包含 `createRichTextService`。」** 那是 OBJ-77。

**反例 16 — 「owner 合同测试已经把对账跑通。」** `TestRichTextOssOwnerUnitTest` 只查字符串含 `oss://`。

**反例 17 — 「列表走 `selectOwned` / XML。」** 主走廊是 `selectVoPage`；没有 XML。

**反例 18 — 「html `@NotBlank`，空正文建不了。」** 注解是 `@NotNull`。空串能过 Bean Validation，normalize 成空片段，零资源也可以建一篇只有标题的作文。

**边界 1 — 不带 PageQuery 等于该作者在该 Client 下整表。** `DEFAULT_PAGE_SIZE = Integer.MAX_VALUE`。

**边界 2 — 标题长度。** DDL `varchar(120)`；演示输入框 `maxlength=120`；Bo **没有** `@Size`。超长直打 HTTP 可能死在数据库，不是校验句。

**边界 3 — 没有防重。** 与单表 `add` 的 2 秒秒表不同。双提交可能两篇。

**边界 4 — `query` 一钥两窗。** `get` 和 `assets` 都是 `demo:richtext:query`。菜单 F 型备注已经把「详情与资源访问」写在一起。

**边界 5 — 权限组含 query 和 upload。** `demo-rich-text` 六串：`list/query/add/edit/remove` + `common:richtext:upload`。仍**不含** `system:oss:upload`。藏按钮 ≠ 票房放行。

**边界 6 — 音频编辑占位。** 编辑器把 `<audio>` 收成 `span[data-richtext-asset-kind=audio]`。保存前 `canonicalizeRichText` 必须变回 `<audio src=oss://id>`。直打 HTTP 若只 POST span，后端**不会**把它收进 assets，也就**不会** bind。

**边界 7 — 逻辑删与 del_flag 类型。** Java `Long`，DDL `char(1)` 默认 `'0'`，与单表同一历史缝。口试点到「有逻辑删」即可，不要发明第二套删除语义。

**边界 8 — 对账参数是表名。** `refType` 必须像 `test_rich_text` 这种物理表；`refId` 是主键字符串。不要传类名。

**边界 9 — `objectMetadata` 默认方法会抛 Unsupported。** 生产由 `SysOssServiceImpl` 覆盖。第三方空实现不能当本课依赖。

**边界 10 — PENDING 删除。** 元数据接口把 `deleteState=PENDING` 当成不可用；`assets` 里变 unavailable；保存校验里变「资源不存在」。

**边界 11 — 同一 ossId 两种标签。** 规范层直接拒绝，到不了 `kindMatches`。

**边界 12 — 种子。** 菜单 C/F 在 `2100800000000000001`–`0015`，并挂到超管角色。**没有**预置 `test_rich_text` 作文行——目录一开始可以是空的。

**边界 13 — 日志。** 写三扇 `isSaveRequestData=false`。不要在操作日志里找 HTML。

**边界 14 — 详情 Vo 的 AutoMapper 不是主路径。** `view()` 手抄。单元测试另证 MapStruct 也能把 `contentHtml` 映到 `html`，那是列表/转换缝，不是 `get` 的代码路径。

## 变式与迁移

1. **和 L-074 对照。** 同一栋 classic 实验室。单表八扇含导入导出、两本分页、`@DataPermission`、`toAjax`、`getInfo` 可空。本课六扇、无 Excel、无部门注解、写窗全 POST、无权详情抛异常、多一扇 `assets`。不要把单表的 `importData` 或 `GET /page` 搬进富文本。
2. **和即将到来的 L-075 对照。** 树表讲父子。本课讲插图箱号。两课都从 L-074 分叉，不要提前把树表方法表背进本格。
3. **和 L-029 / L-027 对照。** 直传产出临时 `ossId` 和进度/中止；票房还有双钥匙。本课从「已经有一个临时 id」开始：验货、装订、领条。不要在本课把 init/sign/complete 再走一遍当 covered。
4. **和 L-026 对照。** 管理卡片柜给管理员看对象行、抹掉 URL、用 `system:oss:download` 领条、删除走 PENDING。本课领条要先证明「这张图属于这篇作文或仍是你的临时箱」，权限字是 `demo:richtext:query`。
5. **和 L-077 对照。** 厨房 `createRichTextService` 六枪 URL 与本课 1:1；`assets` 端口由厅堂注入；页上 `RichTextEditor` 的 canonicalize、pending 占位、卸载 abort，都是前端课的格子。本课只借用它们当「谁扣扳机 / 草稿会员资格为何会 抛」的证据，**不**把 `A:createRichTextService` / `A:createDemoWebDomain` 标 covered。
6. **和 L-010 对照。** `web-kit-rich-text` 当前活消费者是 demo 页；厅堂 `components/Editor` 是平行私货。本课 HTTP 合同不依赖那份私货。
7. **和 layered 公告对照。** 通知模块走 UseCase。本课 PageQuery 一直传到 ServiceImpl。不要在 demo 里学公告再加 DAO。跨模块仓库必须走 `OssService`，这点和 layered 房间一致。
8. **以后若要草稿预览带 `richTextId`。** 应把会员资格改成「已保存 ossIds ∪ 当前用户的临时箱」，并决定失败是整单抛还是单条 unavailable。本课不改代码。
9. **以后若要防重。** 给 `create`/`update` 加 `@RepeatSubmit`。现在没有。
10. **以后若要标题搜索 / `@Size(120)`。** 后端今天没有；只有前端输入框和 DDL。
11. **新模块接富文本。** 不要复制 `wta-demo` 包结构。登记 layered，Controller 只喊 UseCase；规范化用 `RichTextProcessor`；对账用 `OssService.reconcileReferences(真实表名, 主键, previous, current)`；出门条做成自己的业务授权窗，**不要**让浏览器直接打 `SysOssController.downloadUrl` 拼 HTML。上传策略自备 `required-permission`，并记住票房方法上还有 `system:oss:upload`。
12. **迁移口诀：** 先数六扇 HTTP（写全是 POST，没有上传）→ 再数 classic 三跳（门卫不抱 Mapper，厨师抱 Mapper + `OssService`）→ 再数「库里只有 `oss://`」→ 再数验货三闸（归属 / 临时或旧稿 / 类型）→ 再数对账在写库之后同一事务 → 再数 `assets` 两条走廊（有稿号 vs 无稿号）→ 最后数「样板 ≠ 模板」。跳步会出现「把上传算进本课」「把签名 URL 写进库」「删节点就删 OSS」「带 richTextId 预览新图」「把 demo 抄进新模块」。

## 常见误区

1. **「OBJ-76 是整个 wta-demo 模块。」** 只认富文本 Controller 六法。单表、树表、玩具柜都不是这一格。
2. **「CRUD 四个字。」** 磁盘六扇。漏 `assets` 不满格。
3. **「上传在 `/demo/rich-text/upload`。」** 没有这扇。
4. **「详情已经是可显示的 https。」** 是 `oss://`。
5. **「列表带回 html。」** SummaryVo 没有。
6. **「Java 叫 `getInfo`。」** 叫 `get`。
7. **「写窗是 PUT/DELETE。」** POST。
8. **「`@DataPermission`。」** 没有。作者 + Client。
9. **「无权详情 200+null。」** 抛异常。
10. **「编辑器删图 = OSS remove。」** 只改 HTML。
11. **「`selectUrlByIds` 是新合同。」** deprecated。
12. **「assets = 管理柜 download-url。」** 不是。
13. **「带 richTextId 对照的是编辑器内存。」** 对照已保存 HTML。
14. **「预览坏 id 会 404。」** 常是 200 + unavailable；稿号/会员资格失败才整单抛。
15. **「html 不能为空串。」** `@NotNull` 允许空串。
16. **「有防重。」** 没有。
17. **「这是新模块样板。」** 登记表禁止。
18. **「ServiceImpl 抱 `SysOssMapper`。」** 抱的是 `OssService`。
19. **「列表走 XML / selectOwned。」** 走 `selectVoPage`。
20. **「owner 单测已经覆盖对账。」** 那是字符串 stub。
21. **「`common:richtext:upload` 单独就能直传。」** 票房还要 `system:oss:upload`。
22. **「OBJ-76 包含 RichTextPage。」** 页面是对照；格子是 Java 六法 + 资产解析语义。
23. **「前端授权。」** `v-hasPermi` 藏按钮；REST 仍是 `@SaCheckPermission`。
24. **「给 demo 加五层才算现代化。」** 同一模块一种模式；登记表保持 classic。
25. **「`bundle-core` 还留着富文本窗。」** 不留。

## 非评分暂停

打开磁盘，不要凭记忆默写路径。不要改文件。没有标准答案栏，没有分数。

1. 打开 `TestRichTextController.java`。用手指点 6 个映射。圈写三扇都是 POST、都 `isSaveRequestData = false`。圈 `assets` 与 `get` 权限相同。圈没有上传方法、没有 `@RepeatSubmit`。
2. 打开 `ITestRichTextService.java` 与 `TestRichTextServiceImpl.java`。确认六法 1:1。圈 `REF_TYPE = "test_rich_text"`。圈 `@DSTransactional` 只在 create/update/remove。圈 `owned` 的 `createBy` + `clientPk`。圈 `validateAssets` 三闸。圈 `assets` 的 `containsAll` 与 `resolve(..., referencedDocument)`。圈 `view()` 手抄。圈 `selectOwned` **没有**被调用。
3. 打开 `RichTextProcessor.java` 与 `RichTextProcessorTest.java`。圈 1 MiB / 100 assets / 只允许 `oss` 协议 / 同 id 两用途。圈 Base64 与 javascript 样式用例。
4. 打开 `TestRichText.java` / Bo / VersionBo / 三个 Vo / Mapper。圈表名、`contentHtml`、`@Version`、`@TableLogic`。圈 Bo 的 `@NotNull html`。圈 SummaryVo 没有 html。圈 AssetVo 的 String `ossId`。确认没有 Mapper XML。
5. 打开 `OssService.java` 的 `reconcileReferences` / `snapshot` / `objectMetadata` / `resolveAccessUrl`，以及 `OssLifecycleManager.bind/unbind`。圈临时变正式、零引用变回临时。打开 `business-oss-owners.json` 的 `demo-rich-text-content` 行，圈 encoding 与 `restoreStrategy: not supported`。
6. 打开 `10-cde-base-ddl.sql` 的 `test_rich_text` 与 `50-cde-base-dml.sql` 菜单 2100800000000000001–0015。圈 F 型 `common:richtext:upload`。打开 `application.yml` 的 `richtext-image/audio/video/file`。打开厅堂 `services.ts` 的 `richTextAssets` 与厨房 `createRichTextService`——只勾对照，不改文件。打开 `RichTextPage.vue` 圈 list/get/create/update/remove；上传不打本课门牌。

## 总结、词汇表与下一步

- **宏观六扇窗：** 同一门牌 `/demo/rich-text`，磁盘 6 个方法。classic 抽屉：门卫只打电话给 `ITestRichTextService`；厨师抱 Mapper 和 `OssService`；没有 XML。装配电闸是 `bundle-full` / `bundle-core`。
- **(a) `TestRichTextController.list,create,update,remove,assets,get`：** `list` 只给当前用户+Client 的摘要分页；`create` 规范+校验临时资源+insert+对账；`update` 所有权+乐观锁+旧稿可保留正式箱+对账；`remove` POST 带 version 逻辑删并对空账；`get` 无权即抛、HTML 仍是 `oss://`；`assets` 才是 OSS 资产解析窗（有稿号看已装订，无稿号只看自己的临时箱）。不要把上传票房、管理柜 download-url、厨房工厂、`selectUrlByIds`、单表八扇算进这一格。
- **纸上只写箱号。** 规范化在 `wta-common-richtext`；归属/临时态/类型在厨师；钉子在 `reconcileReferences`；短时 https 在业务授权之后的 `resolveAccessUrl`。
- **样板 ≠ 模板。** 成熟切片用来认「业务模块如何接 OSS 引用」；新模块默认 layered，工具可以复用，包结构不要抄。

词汇表：`TestRichTextController` / `ITestRichTextService` / `TestRichTextServiceImpl` / `TestRichText` / `TestRichTextBo` / `TestRichTextVersionBo` / `TestRichTextVo` / `TestRichTextSummaryVo` / `TestRichTextAssetVo` / `test_rich_text` / `RichTextProcessor` / `RichTextContent` / `RichTextAsset` / `OssService.reconcileReferences` / `objectMetadata` / `snapshot` / `resolveAccessUrl` / temporary object / `richtext-image` / `common:richtext:upload` / `demo:richtext:query` / classic / `bundle-full` / `bundle-core`。

下一步：L-075 若尚未读，把同一房间的树表六扇讲完。L-077 才把 `createDemoService` / `createRichTextService` / `createDemoWebDomain`、canonicalize、权限组六串、厅堂资产端口对完。票房与浏览器直传保持 L-027/L-029。能力展示 Controller 保持 deferred。本课结束不发作业、不打分；要练习请之后主动激活 Homework。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-004 | `backend/wta-modules/**` Controller | 业务模块公开入口；本课类在 `wta-demo/controller` | `TestRichTextController.java` | 2026-09-17 |
| S-006 | `frontend/packages/{domains,web-domains}` 与 admin-web `services.ts` | 厨房六枪 URL、资产端口、权限组、RichTextPage 扳机（对照，不盖章前端格） | `domains/demo/src/rich-text.ts`；`web-domains/demo`；`application/services.ts` | 2026-09-17 |
| S-008 | `.agents/skills/engineering-standards/references/project/03-backend-module-modes.md` | `wta-demo` = classic；不得当 layered 反例 | 登记表 demo 行 | 2026-09-17 |
| S-010 | `release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql`、`50-cde-base-dml.sql` | 表 `test_rich_text`；菜单 C/F 含 `common:richtext:upload`；无作文种子行 | `CREATE TABLE test_rich_text`；菜单 2100800000000000001–0015 | 2026-09-17 |
| S-015 | `backend/wta-api/.../OssService.java` | `reconcileReferences` / `snapshot` / `objectMetadata` / `resolveAccessUrl`；`selectUrlByIds` deprecated | 接口 javadoc 与 record | 2026-09-17 |
| S-L076-01 | `TestRichTextController.java` | 六扇映射；权限字；写三扇 POST + 日志不存体；`assets` 在 `get` 前 | 类上 `/demo/rich-text` 及各方法 | 2026-09-17 |
| S-L076-02 | `ITestRichTextService.java`；`TestRichTextServiceImpl.java` | classic 厨师；owned；normalize/validate/reconcile；assets 两条走廊；view 手抄 | `REF_TYPE`；`validateAssets`；`resolve` | 2026-09-17 |
| S-L076-03 | `TestRichText.java`；Bo/VersionBo/Vo/AssetVo/SummaryVo；`TestRichTextMapper.java` | 表映射；校验；列表无 html；无 XML；`selectOwned` 未被调用 | `@TableName`；Mapper 泛型 SummaryVo | 2026-09-17 |
| S-L076-04 | `RichTextProcessor.java`；`RichTextContent.java`；`RichTextProcessorTest.java` | 1 MiB/节点/100 资源；只允许 oss://；幂等；拒绝 Base64 与同 id 两用途 | `normalize`；测试四例 | 2026-09-17 |
| S-L076-05 | `OssLifecycleManager.bind/unbind/reconcileReferences/snapshot/resolveAccessUrl`；`SysOssServiceImpl.objectMetadata` | 临时变正式；零引用回临时；PENDING 元数据不可用 | `isTemp`；`sys_oss_ref` | 2026-09-17 |
| S-L076-06 | `wta-modules/wta-demo/docs/rich-text.md`；`business-oss-owners.json`；`TestRichTextOssOwnerUnitTest.java` | 接入说明与 owner 登记；restore 不支持；单测是 stub | `demo-rich-text-content` | 2026-09-17 |
| S-L076-07 | `application.yml` `oss.direct-upload` 的 `richtext-*`；菜单 F 型 `common:richtext:upload` | 四条策略、PRIVATE、双钥匙对照 | yaml 139–178 行附近；DML 0015 | 2026-09-17 |
| S-L076-08 | `frontend/packages/web-kit/rich-text/src/{content.ts,RichTextEditor.vue,RichTextViewer.vue}`；`RichTextPage.vue` | canonicalize；上传后 resolve 带 resourceId；会员资格失败被 catch；页打 list/get/create/update/remove | `canonicalizeRichText`；`queueUpload` | 2026-09-17 |
| S-L076-09 | 子课 L-003 / L-026 / L-027 / L-029 / L-074；course.md OBJ-76；coverage-matrix TestRichText 行；`wta-demo/pom.xml` | classic；卡片柜/票房/直传边界；单表对照；本格 uncovered→本课声称；依赖 richtext+api | OBJ-76；pom `wta-common-richtext` | 2026-09-17 |
