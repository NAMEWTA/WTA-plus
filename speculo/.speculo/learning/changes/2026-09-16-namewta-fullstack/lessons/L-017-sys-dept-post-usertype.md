---
lesson_id: L-017
objective_ids: [OBJ-17]
claimed_cells: [A:SysDeptController.list,excludeChild,getInfo,remove,optionselect, A:SysPostController.list,export,getInfo,remove,optionselect,deptTree, A:SysUserTypeController.list,export,getInfo,changeStatus,remove,options,listByUser]
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: method-table-on-disk
    minutes: 12
  - segment: deep-explanation
    minutes: 9
  - segment: visuals-and-worked-examples
    minutes: 6
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 4
expression_level: eli5
coverage_depth: deep
source_ids: [S-004, S-008, S-010, S-L017-01, S-L017-02, S-L017-03, S-L017-04, S-L017-05, S-L017-06, S-L017-07, S-L017-08]
---

# Lesson 017：三间组织房间——部门、岗位、登录域

## 学完你能做什么

你能指着 `wta-system` 里**三扇 classic 门**，口述每扇公开窗的 HTTP、Java 名、权限串、委托的服务方法和失败时动不动库。不要把三扇门揉成「组织架构 CRUD」，也不要把 `SysUserType` 说成人事里的「员工类型」。

三扇门的磁盘位置都是：

`backend/wta-modules/wta-system/src/main/java/org/namewta/system/controller/system/`

登记表写 `wta-system = classic`。调用链是 **Controller → `ISys*Service` → `Sys*ServiceImpl`（自己抱 Mapper）→ Mapper Java default / `BaseMapperPlus`**。这三间**没有 UseCase、没有 DAO 层**。同目录的 `*Mapper.xml` 存在，但是**空壳**；本课 SQL 不在 XML 里。

**两套名单，先对齐再背：**

- **OBJ-17 口述三扇门：** `SysDeptController` / `SysPostController` / `SysUserTypeController` 的公开方法。
- **矩阵 (a) 三行点名的窗（本课认格子）：**
  - 部门：`list` / `excludeChild` / `getInfo` / `remove` / `optionselect`
  - 岗位：`list` / `export` / `getInfo` / `remove` / `optionselect` / `deptTree`
  - 登录域：`list` / `export` / `getInfo` / `changeStatus` / `remove` / `options` / `listByUser`
- **同文件 add/edit 矩阵行没点名，课内函数表仍要出现。** Goal 合同：同一资源的 list/get/add/edit/remove 必须能指。漏 add/edit 不算把三扇门讲完。

类上没有 `@SaIgnore`。每扇窗自己贴 `@SaCheckPermission`。GET 只读、无 `@Log`；会改库或导出的窗带 `@Log`。add/edit 另贴 `@RepeatSubmit`（默认 5000ms）。导出是 **POST + 写响应流**，返回类型 `void`，不是 `R<>`。

本课不宣称你会拆 `createSystemService.departments/posts/userTypes` 全表（L-019）、用户如何被分配岗位/登录域（L-015）、角色数据范围怎么配（L-016）。本课要把**三扇门自己的合同**讲完。

## 先把宏观地图放在桌上

L-015 走进用户房间。用户身上挂了三样东西：坐在哪间教室（部门）、胸前什么工牌（岗位）、能进哪座校园门（登录域）。本课走进挂这三样东西的**柜台**，不走进用户本人那一扇窗。

```text
浏览器 / admin-web
        │  GET/POST/PUT/DELETE  /system/{dept|post|userType}
        v
SysDeptController        @RequestMapping("/system/dept")
SysPostController        @RequestMapping("/system/post")
SysUserTypeController    @RequestMapping("/system/userType")
        │  classic：没有 UseCase
        v
ISysDeptService / ISysPostService / ISysUserTypeService
ISysUserTypeRelService     ← 只有 listByUser 走关系桌
        │  ServiceImpl 直接持有 Mapper
        v
SysDeptMapper / SysPostMapper / SysUserTypeMapper / SysUserTypeRelMapper
        │  Java default + MPJ；XML 空壳
        v
MySQL  sys_dept / sys_post / sys_user_post / sys_user_type / sys_user_type_rel
Redis  部门缓存 SYS_DEPT、SYS_DEPT_AND_CHILD；登录域缓存 SYS_USER_TYPE
       changeStatus 还可能踢 Sa-Token 会话
```

| 门牌 | 资源 | 列表形状 | 删几条 | 数据权限贴在哪 |
| --- | --- | --- | --- | --- |
| `/system/dept` | 嵌套教室（树） | **整表 List**，不分页 | **一条** `/{deptId}` | `selectDeptList` / `countDeptById` 的 `@DataPermission(deptName→dept_id)` |
| `/system/post` | 挂在某间教室上的工牌 | **PageResult** | **批量** `/{postIds}` | **分页列表**走 `selectPagePostList`；导出/按部门 optionselect **不走**带注解的那个方法 |
| `/system/userType` | 校园门（登录域） | **PageResult** | **批量**（路径还要求数字） | **没有** `@DataPermission`；这是全局目录 |

**类比：** 学校有三样东西。教室可以套教室（部门树）。工牌必须挂在某一间教室上（岗位有 `deptId`）。第三样不是「实习生/正式工」这种工牌分类，而是**哪一座校园的大门钥匙**：编码叫 `userTypeCode`，登录会话里的 `LoginUser.userType` 存的是这串编码。停用一扇大门，会把还拿着这把钥匙的人从 Redis 会话柜里请出去。

**类比失效处：**

1. 部门树 ≠ 岗位筛选用的部门树。后者是 `GET /system/post/deptTree`，权限串是 `system:post:list`，**不是** `SysDeptController` 的窗。
2. `optionselect` 三扇门长得不像。部门：可选 id 列表，空则所有**正常**部门。岗位：`deptId` 优先，否则 `postIds`，**两样都空就返回空列表**。登录域下拉不叫 `optionselect`，叫 **`/options`**，而且只给启用中的。
3. 登录域不是 HR 的员工类型。类注释写的是「登录域管理」。表注释是「登录域定义表」。Excel 页名是「登录域」。
4. classic 的 ServiceImpl 抱 Mapper，**不等于**新模块可以再抄一间。新房间默认 layered（L-003）。
5. 删部门/岗位/登录域都是 `@TableLogic` 逻辑删（`del_flag`）。「不允许删除」发生在进 `deleteById` **之前**。逻辑删掉的登录域编码仍占着库上的 `uk_sys_user_type_code`。

## 核心概念与机制

### 直觉讲解

小孩子能记住的三句话：

1. **教室可以套教室。** 改一间教室的上级，底下所有子孙的「祖宗名单」`ancestors` 都要改写。启用一间教室，会把祖先也启用。删教室之前先看：是不是默认教室、有没有下级、有没有人坐、有没有工牌挂着。这四道闸走的是 **`R.warn`（业务码 601）**，不是 `R.fail`。
2. **工牌挂在一间教室上。** 同名工牌只要教室不同就可以并存；工牌**编码全校唯一**（应用层校验，DDL 没有唯一索引）。有人戴着就不能删、不能停用。删是批量的。
3. **大门钥匙是登录域。** 编码创建后只读。停用大门会踢该域全部在线会话，并作废相关 OpenAPI 机器会话。还有人引用或还有 Client 指向它，就删不掉。查「某用户有哪些大门」那一枪的权限是 **`system:user:query`**，不是 `system:userType:query`。

### 精确定义与 English term

| 中文 | English term | 本课操作定义 |
| --- | --- | --- |
| 部门 | department / `SysDept` | `sys_dept` 一行。树靠 `parent_id` + `ancestors`（逗号分隔祖级 id）。默认部门 `SystemConstants.DEFAULT_DEPT_ID = 1761000000000000100L` 不允许删。 |
| 祖级列表 | ancestors | 字符串，分隔符 `StringUtils.SEPARATOR`（`,`）。根的常量是 `ROOT_DEPT_ANCESTORS = "0"`。 |
| 岗位 | post / `SysPost` | `sys_post` 一行，必有 `dept_id`。用户佩戴关系在 `sys_user_post`。 |
| 登录域 | user type / login domain / `SysUserType` | `sys_user_type` 一行。编码 `user_type_code` 唯一。Java/URL 仍叫 `userType`。 |
| 登录域关系 | user-type relation | `sys_user_type_rel`。`listByUser` 读这张表，不读定义表单独一行。 |
| 数据权限 | data permission / `@DataPermission` | 按当前登录人的部门范围改 SQL。超级管理员在 `checkDeptDataScope` 里直接放行。 |
| 逻辑删除 | logical delete / `@TableLogic` | `deleteById` / `deleteByIds` 改 `del_flag`，不是物理 DROP 行。 |
| 警告信封 | `R.warn` | `HttpStatus.WARN = 601`。部门删除四道业务闸用它；岗位/登录域拒绝删除抛 `ServiceException`。 |
| 选择框 | option select | 给下拉用的瘦列表。三扇门的空参行为**不一样**。 |
| 排除子孙 | exclude child | 列表里拿掉自己和 `ancestors` 包含自己的节点。用来挑新上级，避免把子孙当成爸爸。 |
| 防重复提交 | `@RepeatSubmit` | add/edit 默认 5 秒。`changeStatus`、remove、export **没有**。 |
| 控制器合同 | controller contract | 本课 (b) 口径：失败是否进 Service 写库、踢不踢会话、缓存刷不刷。不把 `wta-api` 的 `DeptService`/`PostService` 算进本课 (a) 格子。 |

状态值：`SystemConstants.NORMAL = "0"`，`DISABLE = "1"`。

### 机制/因果链

文件三份，2026-09-16 对过工作树。先对表，再背口诀。

#### A. `SysDeptController`：`/system/dept`

构造注入：`ISysDeptService deptService`、`ISysPostService postService`（删除时数岗位）。

**1. `list` — `GET /system/dept/list`**（格子 `list`）

权限 `system:dept:list`。入参 `SysDeptBo dept`（query）。**不分页。** `deptService.selectDeptList(dept)` → `deptMapper.selectDeptList`（`@DataPermission(deptName→dept_id)`）。条件：id / parentId / 名称模糊 / 类别 / 状态 / 时间区间；`belongDeptId` 有值时先 `selectDeptAndChildById` 再 `IN`。排序 `ancestors, parentId, orderNum, deptId`。

服务里另有 `selectPageDeptList`，**本窗不用**。前端 `createSystemService.departments.list` 的返回类型是 `DeptVO[]`，不是 `PageResult`。

**2. `excludeChild` — `GET /system/dept/list/exclude/{deptId}`**（格子 `excludeChild`）

权限同样 `system:dept:list`。先 `selectDeptList(new SysDeptBo())` 拉当前数据范围内的全量，再内存 `removeIf`：

- `d.getDeptId().equals(deptId)`（自己）
- 或 `StringUtils.splitList(d.getAncestors()).contains(Convert.toStr(deptId))`（子孙）

祖先、兄弟留下。这是改上级时的候选列表。**不**在 SQL 里 `NOT FIND_IN_SET`。空 `SysDeptBo` 不过滤状态，停用部门也会出现（随后再按 id/祖级剔除）。

**3. `getInfo` — `GET /system/dept/{deptId}`**（格子 `getInfo`）

权限 `system:dept:query`。先 `checkDeptDataScope(deptId)`，再 `selectDeptById`（`@Cacheable(SYS_DEPT, key=deptId)`，缓存名 `sys_dept#30d`）。命中后补 `parentName`。

`checkDeptDataScope`：`deptId==null` 直接 return；超管 return；否则 `countDeptById`（带数据权限）为 0 → `ServiceException("没有权限访问部门数据！")`。非超管时**缺行和没权限同一句话**。超管查无：过闸，`R.ok(null)`。

**4. `add` — `POST /system/dept`**（矩阵行没点名，课内必指）

权限 `system:dept:add`。`@Log INSERT`、`@RepeatSubmit`、`@Validated @RequestBody SysDeptBo`。名称不唯一 → `R.fail("新增部门'…'失败，部门名称已存在")`。唯一性是 **同父节点下同名**（`deptName + parentId`），不是全局。过闸 `insertDept`：父不存在抛「父部门不存在」；父状态不是 `"0"` 抛「部门停用，不允许新增」；`ancestors = 父.ancestors + "," + parentId`。刷 `SYS_DEPT_AND_CHILD` 全表。**本方法不调 `checkDeptDataScope`。** 知道父 id 就能试图往视野外的父下面挂（父存在且启用是硬闸）。DDL 对 `(parent_id, dept_name)` **没有**唯一索引，校验与插入之间有 TOCTOU 窗口。

**5. `edit` — `PUT /system/dept`**（矩阵行没点名，课内必指）

权限 `system:dept:edit`。`@Log UPDATE`、`@RepeatSubmit`。顺序：

1. `checkDeptDataScope(deptId)`
2. 名称不唯一 → `R.fail`
3. `parentId.equals(deptId)` → `R.fail`「上级部门不能是自己」
4. 若停用：有正常子孙 → `R.fail("该部门包含未停用的子部门!")`；有用户 → `R.fail("该部门下存在已分配用户，不能禁用!")`。**停用不查岗位。** 有岗位、没人、子孙都停用，停用能过。
5. `updateDept`（`@Transactional`）：换父则对新父再 `checkDeptDataScope`，改写自己和子孙 `ancestors`，并逐个 `CacheUtils.evict(SYS_DEPT, childId)`；启用且祖先不是根 `"0"` 时，把祖先 id 全部打成正常。刷 `SYS_DEPT` 当前键和 `SYS_DEPT_AND_CHILD` 全表。

**6. `remove` — `DELETE /system/dept/{deptId}`**（格子 `remove`）

权限 `system:dept:remove`。`@Log DELETE`。**单 id，不是数组。** 闸门顺序（注意数据权限在后）：

1. `DEFAULT_DEPT_ID` → `R.warn("默认部门,不允许删除")`
2. `hasChildByDeptId`（`parent_id = deptId` 的 lambda，**无** `@DataPermission`）→ `R.warn("存在下级部门,不允许删除")`
3. `checkDeptExistUser`（`sys_user.dept_id`，lambda）→ `R.warn("部门存在用户,不允许删除")`
4. `postService.countPostByDeptId` → `R.warn("部门存在岗位,不允许删除")`
5. **然后才** `checkDeptDataScope`
6. `deleteDeptById`（逻辑删 + 刷两块缓存）

超管也过不了第 1 闸。非超管若部门有下级，可能先拿到 601 警告，还没走到「没有权限」。空叶子且没权限，才抛那句数据权限异常。`toAjax(int)`：影响行 >0 才 `R.ok`，否则 `R.fail`。

**7. `optionselect` — `GET /system/dept/optionselect`**（格子 `optionselect`）

权限 `system:dept:query`。可选 `Long[] deptIds`。`selectDeptByIds`：只选 `deptId/deptName/leader`，**状态必须正常**；`deptIds` 空则不加 `IN`，等于数据范围内全部正常部门。走 `selectDeptList`，所以**带**数据权限。前端 `departments.options(ids)` 会拼 `?deptIds=`。

#### B. `SysPostController`：`/system/post`

构造注入：`ISysPostService postService`、`ISysDeptService deptService`（只要部门树）。

**1. `list` — `GET /system/post/list`**（格子 `list`）

权限 `system:post:list`。`SysPostBo` + `PageQuery` → `selectPagePostList` → `postMapper.selectPagePostList`（`@DataPermission`：`deptName→dept_id` **和** `userName→create_by`）。`deptId` 优先等值；否则 `belongDeptId` 走部门及以下。这是三扇门里**唯一带数据权限的岗位列表窗**。

**2. `export` — `POST /system/post/export`**（格子 `export`）

权限 `system:post:export`。`@Log EXPORT`。`selectPostList` → `ExcelBuilder.of(list, SysPostVo.class).sheetName("岗位数据").toResponse(response)`。返回 `void`。

深点：`SysPostServiceImpl.selectPostList` 调的是 `postMapper.selectVoList(...)`，**不是**带 `@DataPermission` 的 `selectPostList`。导出窗和分页列表的数据范围**可以不一致**。不要把「列表看不见」说成「导出一定看不见」。

**3. `getInfo` — `GET /system/post/{postId}`**（格子 `getInfo`）

权限 `system:post:query`。一行 `selectPostById` → `selectVoById`。**没有** `checkDeptDataScope`，Mapper 方法也**没有** `@DataPermission`。缺行 → `R.ok(null)`。和部门详情不是同一把锁。

**4. `add` — `POST /system/post`**（课内必指）

权限 `system:post:add`。`@Log INSERT`、`@RepeatSubmit`、`@Validated`。`deptId`/`postCode`/`postName`/`postSort` 是 Bean 硬约束。名称不唯一（**同部门同名**）或编码不唯一（**全局编码**）→ `R.fail`。然后 `insertPost`：转换 + `insert`。**不查部门是否存在。** DDL 无外键、岗位编码无唯一索引。

**5. `edit` — `PUT /system/post`**（课内必指）

权限 `system:post:edit`。同样两道唯一闸。再：`status==DISABLE` 且 `countUserPostById>0` → `R.fail("该岗位下存在已分配用户，不能禁用!")`。`updatePost` 无缓存。

**6. `remove` — `DELETE /system/post/{postIds}`**（格子 `remove`）

权限 `system:post:remove`。`@Log DELETE`。`Long[]`，逗号分隔。服务 `deletePostByIds`：先 `selectByIds`，任一已分配 → `ServiceException("{postName}已分配，不能删除!")`，**整批不删**。过闸 `deleteByIds` 逻辑删。这里的 `selectByIds` / `deleteByIds` **无** `@DataPermission`（带权限的是没用上的 `selectPostCount`）。拒绝形状是异常，不是部门那种 `R.warn`。

**7. `optionselect` — `GET /system/post/optionselect`**（格子 `optionselect`）

权限 `system:post:query`。两个可选 query：`postIds`、`deptId`。分支：

- `deptId != null` → `selectPostList`（只按部门，**不**强制 `status=0`；同样走无数据权限的 `selectVoList`）。`deptId` 和 `postIds` 同时传时 **部门赢**。
- 否则 `postIds != null` → `selectPostByIds`（只正常状态；lambda，无数据权限）
- 否则 **空 `ArrayList`**，不是全量岗位

前端 `posts.options(deptId, postIds)` 两个参数都会带上。厅堂若同时给了部门，后端不会用 `postIds` 收窄。

**8. `deptTree` — `GET /system/post/deptTree`**（格子 `deptTree`）

权限 `system:post:list`（不是 `system:dept:list`）。`deptService.selectDeptTreeList(dept)`：先带数据权限的部门列表，再 `TreeBuildUtils.buildMultiRoot`。停用节点 `extra.disabled=true`。这是岗位页左边的教室树，**不是**部门管理那扇门。

用户管理另有 `GET /system/user/deptTree`（L-015）。角色另有 `roleDeptTreeselect`（L-016）。三棵树三个权限串，不要并成「全系统一棵部门树」。

#### C. `SysUserTypeController`：`/system/userType`

类注释：**登录域管理**。构造注入：`ISysUserTypeService userTypeService`、`ISysUserTypeRelService userTypeRelService`。

编码校验（只在 add 的 `AddGroup`）：`^[a-z][a-z0-9_]*$`（`RegexConstants.DICTIONARY_TYPE`），最长 32。库上 `uk_sys_user_type_code`。

**1. `list` — `GET /system/userType/list`**（格子 `list`）

权限 `system:userType:list`。分页 `queryPageList`。条件：编码/名称模糊、状态。无数据权限。这是全局目录。

**2. `export` — `POST /system/userType/export`**（格子 `export`）

权限 `system:userType:export`。`@Log EXPORT`。`queryList` + Excel，页名 **「登录域」**。`void`。

**3. `getInfo` — `GET /system/userType/{userTypeId:\\d+}`**（格子 `getInfo`）

权限 `system:userType:query`。路径**只吃数字**。`queryById` → `selectVoById`。缓存注解在 **`queryByCode`**（`SYS_USER_TYPE`，`sys_user_type#30d`），不在这一枪。缺行 `R.ok(null)`。

**4. `add` — `POST /system/userType`**（课内必指）

权限 `system:userType:add`。`@Validated(AddGroup.class)`：编码、名称、排序必填。编码不唯一 → `R.fail("新增登录域'…'失败，登录域编码已存在")`。`insertByBo` 成功后把生成的 id 写回 BO。**不**踢会话，**不**主动 evict（新编码尚未进 `queryByCode` 缓存）。

**5. `edit` — `PUT /system/userType`**（课内必指）

权限 `system:userType:edit`。`@Validated(EditGroup.class)`：id、名称、排序。`updateByBo`：`userTypeCode` **被设成 null 再 update**，入参改编码无效。`@DSTransactional`。成功且从非停用改为停用：`clientSessionService.kickoutUserType(null, db.getUserTypeCode())` —— `userId=null` 表示**该登录域下全部 Token**。状态相对库值有变化时，再对持有该域正常关系的用户 `openApiSessionInvalidator.invalidateByUserId`。刷 `SYS_USER_TYPE` 全表。OpenAPI 作废器是 `@Autowired(required=false)`，缺 Bean 时是空操作。

**6. `changeStatus` — `PUT /system/userType/changeStatus`**（格子 `changeStatus`）

权限同样 `system:userType:edit`。`@Log UPDATE`。**无** `@Validated`、**无** `@RepeatSubmit`。body 当 `SysUserTypeBo` 用，只取 `userTypeId`/`status`。id 空 → `ServiceException("登录域ID不能为空")`。`updateStatus` 同样 `@DSTransactional` + 全表缓存 evict。`rows>0` 且目标是停用 → 踢该域全部会话。`rows>0` 且新旧状态不同（或库中无行）→ 作废 OpenAPI。已停用再提交停用：第一闸仍 kickout，第二闸因状态相等跳过 invalidate。`rows==0` → `toAjax` 变 `R.fail`，不踢人。

**7. `remove` — `DELETE /system/userType/{userTypeIds:\\d+(?:,\\d+)*}`**（格子 `remove`）

权限 `system:userType:remove`。路径正则强制数字串。`deleteWithValidByIds`：任一仍被 `sys_user_type_rel` 引用（**不看关系状态**）→ `ServiceException("{name}已被用户引用，不能删除!")`；任一被 `sys_client.user_type_id` 引用 → `…已被客户端引用，不能删除!`。过闸逻辑删 + 刷缓存。按设计删之前没有引用，方法**不**调 kickout。逻辑删后编码仍占唯一键，同编码不能立刻 insert。

**8. `options` — `GET /system/userType/options`**（格子 `options`）

权限 `system:userType:query`。无 query。只返回 `status=0`，按 `orderNum`。不是 `optionselect`，也不是岗位那种可空参空列表。

**9. `listByUser` — `GET /system/userType/user/{userId}`**（格子 `listByUser`）

权限 **`system:user:query`**。`userTypeRelService.selectByUserId`。`userId==null`（正常路径进不来）返回空列表。MPJ：关系表 join 定义表，带上 `userTypeCode` / `userTypeName` / `userTypeStatus`，按登录域 `orderNum`。**不过滤**关系或定义的停用状态——停用的大门仍会出现在「这人有过哪些域」里。前端 `createSystemService.userTypes` **没有**这一枪，也没有 `changeStatus` / `export`。L-019 认工厂缺口；本课认 HTTP 合同。

#### D. 对照口诀（三扇门不要背成一张 CRUD 表）

| 问题 | 部门 | 岗位 | 登录域 |
| --- | --- | --- | --- |
| 列表分页？ | 否，树 List | 是 | 是 |
| 删除批量？ | 否，单 id | 是 | 是（数字正则） |
| 删除拒绝形状 | `R.warn` 601 | `ServiceException` | `ServiceException` |
| 详情是否数据范围闸 | `checkDeptDataScope` | 无 | 无 |
| 写库是否踢会话 | 否 | 否 | **停用时是**（edit/changeStatus） |
| 下拉空参 | 全部正常部门 | **空列表** | `/options` 全部启用域 |
| 前端工厂缺的窗 | 无 export（后端也无） | export | export、changeStatus、listByUser |

### 图、表或文本图

**图题 / caption：** 三扇 classic 门与真实映射。alt：部门树不分页；岗位列表分页且 deptTree 在岗位门上；登录域停用会踢会话。

```text
                wta-system  classic
                Controller → ServiceImpl → Mapper（XML 空）

 /system/dept                         /system/post
 ┌─────────────────────────┐          ┌──────────────────────────────┐
 │ GET  /list              │          │ GET  /list          分页+数据权限
 │ GET  /list/exclude/{id} │          │ POST /export        void+Excel
 │ GET  /{deptId}          │          │ GET  /{postId}      无数据范围闸
 │ POST /            add   │          │ POST /              名/码唯一
 │ PUT  /            edit  │          │ PUT  /              有人不能停用
 │ DELETE /{deptId}  warn闸│          │ DELETE /{postIds}   有人则整批抛
 │ GET  /optionselect      │          │ GET  /optionselect  deptId 优先
 └──────────┬──────────────┘          │ GET  /deptTree      岗位权限串
            │                         └──────────────┬───────────────┘
            v                                        v
     sys_dept  + 缓存 SYS_DEPT                sys_post
     默认 id 不能删                           sys_user_post 占用计数

 /system/userType
 ┌──────────────────────────────────────────┐
 │ GET  /list                     分页      │
 │ POST /export                   页名登录域 │
 │ GET  /{id:\\d+}                          │
 │ POST /  编码只在新增、库唯一键            │
 │ PUT  /  编码强制忽略；停用则踢会话        │
 │ PUT  /changeStatus  无 RepeatSubmit      │
 │ DELETE /{ids:\\d+(,\\d+)*}  引用则拒     │
 │ GET  /options                  仅启用    │
 │ GET  /user/{userId}  权限 system:user:query
 └──────────────────┬───────────────────────┘
                    v
         sys_user_type + sys_user_type_rel
         缓存 SYS_USER_TYPE（按 code）
         kickoutUserType(null, code) → Redis 会话柜
```

**文字等价物：** 三扇门都在 `wta-system` 的 `controller/system` 包，classic，没有管家 UseCase。左边部门是树：列表一次拿完，删除只收一个 id，业务拒绝用 601 警告。中间岗位列表才分页；岗位页自己提供部门树，权限算岗位的。右边登录域是全局目录：编码创建后只读；停用会按编码扫 Token 并把匹配会话注销；「某人有哪些域」挂在用户查询权限上。部门/岗位/登录域的 Mapper XML 都是空文件。

**图的边界：** 不画用户如何被 `coverUserTypes` / 授岗（L-015）。不画角色 `data_scope` 枚举怎么生成 `@DataPermission` SQL（L-016 / L-083）。不把 `wta-api` 的 `DeptService`/`PostService` 画成 HTTP 窗。前端工厂缺枪留给 L-019。

**图题 / caption：** 删除与停用时哪些柜子动。alt：部门 601 不删行；岗位抛异常不删；登录域停用才踢 Redis。

```text
删部门 DELETE /system/dept/{id}
  默认部门 / 有下级 / 有用户 / 有岗位
        \____ R.warn 601，deleteDeptById 零次
        \____ 有下级的 hasChild 查询无数据权限
  过闸后
        \____ 逻辑删 sys_dept.del_flag
        \____ evict SYS_DEPT[id] + SYS_DEPT_AND_CHILD 全表
        \____ 不踢会话

删岗位 DELETE /system/post/{ids}
  任一条 sys_user_post 占用
        \____ ServiceException，整批不删
  过闸后逻辑删 sys_post；无岗位缓存

删登录域 DELETE /system/userType/{ids}
  任一 rel 或 client.user_type_id
        \____ ServiceException
  过闸后逻辑删；evict SYS_USER_TYPE 全表；不 kickout
  编码仍占 uk_sys_user_type_code

停用登录域 PUT /changeStatus 或 PUT /
  目标 "1" 且 update 行数 >0
        \____ kickoutUserType(null, code) 扫 Token，匹配 LoginUser.userType
        \____ Redis 会话柜被撕
        \____ 状态变化时 OpenAPI 机器会话按 userId 作废
  行数 0 → R.fail，不动会话柜
```

**文字等价物：** 部门删除的四道闸在进删除服务前就 `R.warn` 返回，行还在。岗位和登录域把「还被引用」做成异常，事务/方法在抛出处结束。只有登录域**停用**（不是删除）会去扫 Sa-Token。踢人用的是编码等于会话里的 `userType` 字段，userId 传 null 表示整域。部门改树会动祖先状态和子孙 ancestors，那是 update 事务，不是删除。

**图的边界：** `ClientSessionService` 集群重试与 pendingTokens 细节点到「失败抛 授权会话失效失败，请重试」即可，不把 Token 扫描实现当本课口试。OpenAPI 作废器缺 Bean 时是空 lambda，不能把「一定作废机器会话」说成硬条件。

### 正例、反例与边界

**正例 1：** 口述部门五扇点名窗。打开 `SysDeptController`：`GET /list` 不分页；`GET /list/exclude/{deptId}` 内存剔除自己和子孙；`GET /{deptId}` 先数据范围再缓存详情；`DELETE /{deptId}` 四道 `R.warn` 后才删；`GET /optionselect` 只正常状态。再补 `POST /` 与 `PUT /`。

**正例 2：** 默认部门删不掉。`SystemConstants.DEFAULT_DEPT_ID` 与 `remove` 第一闸。超管同样 601。不要指望 `checkDeptDataScope` 的超管短路帮你删它。

**正例 3：** 改上级不能选自己的子孙。`excludeChild` 用 `splitList(ancestors).contains(str(deptId))`，按逗号切段，不是字符串包含。id `10` 不会误伤祖先 `100`。

**正例 4：** 岗位列表和导出不是同一把数据权限锁。`list` → `selectPagePostList` 有 `@DataPermission`；`export` → `selectVoList` 没有。口试要能指到 `SysPostServiceImpl.selectPostList` 那一行调用。

**正例 5：** 岗位 optionselect 空参是空列表。两个 query 都缺，返回 `new ArrayList<>()`。不要把部门 optionselect「空=全部正常」套过来。

**正例 6：** 岗位 `deptTree` 的权限是 `system:post:list`。只有岗位列表权、没有部门列表权的人，仍可能拿到这棵树。

**正例 7：** 登录域编码只读。`updateByBo` 第一件业务事是 `update.setUserTypeCode(null)`。add 才校验编码格式和唯一。

**正例 8：** 停用登录域踢的是整域会话。`kickoutUserType(null, code)` 匹配 `loginUser.getUserType()`。`changeStatus` 与 `edit` 两条 HTTP 都能走到这里。

**正例 9：** `listByUser` 权限串是 `system:user:query`。用户详情页可以读关系，不必有 `system:userType:query`。返回 `SysUserTypeRelVo`，不是 `SysUserTypeVo`。

**正例 10：** 三张表逻辑删。`SysDept` / `SysPost` / `SysUserType` 都有 `@TableLogic delFlag`。`toAjax` 看的是影响行，不是「行从磁盘消失」。

**正例 11：** classic 证据。`SysDeptServiceImpl` 字段是 `SysDeptMapper` / `SysRoleMapper` / `SysUserMapper`；`SysPostServiceImpl` 持有 `SysPostMapper` / `SysDeptMapper` / `SysUserPostMapper`；`SysUserTypeServiceImpl` 持有定义、关系、Client Mapper 和 `ClientSessionService`。没有 `*UseCase`、没有 `*Dao`。

**反例 1：** 「`SysUserType` 就是员工类型 / 用户分类字典。」类注释、表注释、Excel 页名、会话字段都是登录域。人事工种更接近岗位。

**反例 2：** 「三扇门的 list 都分页。」部门不。前端类型已经写成 `DeptVO[]`。

**反例 3：** 「部门删除失败是 `R.fail`，和岗位一样。」部门是 `R.warn` 601；岗位/登录域是 `ServiceException`。

**反例 4：** 「`GET /system/post/deptTree` 在 `SysDeptController`。」它在岗位控制器。部门控制器没有 `deptTree`。

**反例 5：** 「optionselect 空参=全量。」只对部门（再加状态正常+数据权限）成立。岗位空参是 `[]`。登录域下拉是另一条 `/options`。

**反例 6：** 「岗位详情也走部门数据权限。」`getInfo` 没有 `checkDeptDataScope`，也没有带 `@DataPermission` 的 Mapper 方法。

**反例 7：** 「登录域 getInfo 走 `SYS_USER_TYPE` 缓存。」缓存挂在 `queryByCode`。HTTP 详情走 `queryById`。

**反例 8：** 「`@RepeatSubmit` 保护 changeStatus。」没有。连点停用会多次 kickout。add/edit 才有 5 秒窗。

**反例 9：** 「这三间有 UseCase，因为 system 很大。」登记表 classic。磁盘无 UseCase 包。

**反例 10：** 「矩阵没写 add/edit 就可以不讲。」Goal 合同要求函数表出现 list/get/add/edit/remove。本课 (a) 格子按矩阵三行认；add/edit 是同文件必指，不另开格子。

**反例 11：** 「部门停用和删除检查同一套占用。」停用查正常子孙和用户，**不**查岗位。删除查下级、用户、岗位。有岗无人的部门：停用可能成功，删除 601。

**反例 12：** 「前端 `userTypes` 有 changeStatus / listByUser。」2026-09-16 的 `createSystemService` 没有这两枪，也没有 export。HTTP 仍在。

**反例 13：** 「grantType / 用户类型 / 登录域是同一个词。」`grantType` 是 Client 允许的登录策略种类（L-011）。`userType` 是登录域编码。不要把 `password` 说成一种 `SysUserType`。

**边界：**

- `wta-api` 的 `DeptService` / `PostService` 由同一份 ServiceImpl 实现，给别的模块用。矩阵 (a) 另有 `wta-api/system` 行，不是本课认的三行。
- `selectPageDeptList` 存在但部门控制器不用。
- 部门 add 不校验数据范围；岗位 add 不校验部门存在。
- 登录域 Mapper / 关系 Mapper 无 `@DataPermission`。
- XML 空壳仍占用 `src/main/resources/mapper/system/`；改 SQL 去 Java default 方法。
- 逻辑删 + 登录域唯一键：删掉再新建同一编码，可能撞 `uk_sys_user_type_code`。
- home-web / sso-web 不组装这三扇管理窗。App 组合是 L-007 / L-008。

## 变式与迁移

- **变式 A：给部门列表加分页。** 服务已经有 `selectPageDeptList`。改控制器会让前端 `DeptVO[]` 合同裂开。树 UI 要的是整枝，不是第 1 页 10 行。不要只改后端。
- **变式 B：删除部门改成批量。** 当前路径是单 id，前端 `departments.delete` 也是单 id。岗位/登录域才是数组。改批量必须重做四道 601 闸（默认部门、下级、用户、岗位）的逐条/整批语义。
- **变式 C：岗位 optionselect 在空参时返回全量。** 今天会变成「全校工牌泄漏给任何有 `system:post:query` 的人」，而且这条路径还没有列表那种 `@DataPermission`。要改先补权限，再改空参。
- **变式 D：让导出与列表权限范围一致。** 把 `selectPostList` 改调 Mapper 上已有的 `@DataPermission selectPostList(...)`，不要再走 `selectVoList`。不要只在 Excel 里「自己再滤一遍」。
- **变式 E：停用登录域但不踢会话。** 现合同是踢。若产品要「停用只挡新登录、旧票还能用」，得改 `updateStatus` / `updateByBo`，不能只藏按钮。反过来，若希望删除未引用域时也清残留 Token，得在 `deleteWithValidByIds` 补 kickout——今天没有。
- **变式 F：给登录域改编码。** HTTP 入参会被服务端丢掉。要改编码等于新开资源（删+增），且会撞唯一键与已发会话里的 `LoginUser.userType`。
- **变式 G：把 `listByUser` 的权限改成 `system:userType:query`。** 用户页会打不开这枪，除非用户管理员同时有登录域查询权。现合同刻意挂在 `system:user:query`。
- **变式 H：新增一种「组织」资源。** 先看登记表：`wta-system` 仍是 classic，但**不得为新功能把 ServiceImpl 再开宽越层依赖**；全新业务房间默认 layered。不要在这三个 Controller 里塞第四种树。

## 常见误区

1. **「UserType = 用户类型字典。」** 登录域。会话、Client 默认域、踢人编码都认它。
2. **「三扇门都在 layered 五层里。」** `03-backend-module-modes.md` 写 classic。
3. **「部门 list 返回 PageResult。」** `R<List<SysDeptVo>>`。
4. **「岗位 deptTree 是部门模块的公开方法。」** Java 名在 `SysPostController`。
5. **「optionselect 语义统一。」** 空参三套答案。
6. **「删除失败都是 fail JSON。」** 部门 601 warn。
7. **「getInfo 都先 checkDeptDataScope。」** 只有部门详情。
8. **「导出一定遵守列表数据权限。」** 岗位导出当前没走带注解方法。
9. **「changeStatus 只改一列，纯函数。」** 停用会踢 Redis、可能作废 OpenAPI。
10. **「前端没有的枪后端就没有。」** `listByUser` / `changeStatus` / 两处 export 仍在 Controller。
11. **「XML 里有树查询。」** 四个相关 XML 都是空 mapper。
12. **「逻辑删等于编码可以复用。」** 登录域唯一键不跟 `del_flag` 组合。
13. **「矩阵 (a) 没写 add 就是没有 add。」** 三扇门都有 POST `/` 与 PUT `/`。
14. **「`R.warn` 等于 HTTP 200 业务成功。」** 码是 601，行没删。前端若只认 HTTP 状态会当成成功。

## 非评分暂停

打开磁盘，不要凭记忆默写路径。不要改文件。没有标准答案栏。

1. 打开三份 Controller。用手指点每个 `@GetMapping` / `@PostMapping` / `@PutMapping` / `@DeleteMapping`。把矩阵点名的窗和 add/edit 分成两堆。确认部门没有 export、没有 deptTree；岗位没有 excludeChild；登录域下拉叫 `/options`。
2. 在 `SysDeptController.remove` 圈出四次 `R.warn` 和后面的 `checkDeptDataScope`。对照 `DEFAULT_DEPT_ID`。
3. 在 `SysPostServiceImpl.selectPostList` 确认它调用的是 `selectVoList`。再看 Mapper 上那个带 `@DataPermission` 的 `selectPostList` 有没有被 Controller 用到。
4. 在 `SysPostController.optionselect` 看 `deptId` 分支是否在 `postIds` 前面。把「两样都空」的返回值记下来。
5. 在 `SysUserTypeServiceImpl.updateStatus` 圈出 `kickoutUserType(null, …)` 和 `invalidateUsersForUserType`。再看 Controller 上 `changeStatus` 有没有 `@RepeatSubmit`。
6. 打开 `frontend/packages/domains/system/src/service.ts` 的 `departments` / `posts` / `userTypes`。记下工厂缺的枪，留给 L-019，不要在本课假装前端已经打全。

## 总结、词汇表与下一步

- **三扇门在 `wta-system`，classic，XML 空壳。** 公开合同是 HTTP 路径 + 权限串 + `R`/`void`，不是 UseCase 名。
- **部门是树。** 列表不分页；排除子孙是内存过滤；详情先数据范围；删除四道 601；optionselect 只正常部门。add/edit 课内必指：同父同名、换父改 ancestors、停用不查岗位。
- **岗位挂部门。** 列表分页且带数据权限；导出/按部门下拉当前不带同一把锁；optionselect 空参是空列表；`deptTree` 用岗位权限借部门树；有人则不能停用、不能删。
- **UserType 是登录域。** 编码只读且库唯一；`/options` 只启用；`listByUser` 走关系表和 `system:user:query`；停用踢整域会话。
- **本课 (a) 认矩阵三行点名的窗。** add/edit 在函数表里出现，不另占格子。前端工厂缺口、RBAC、用户授岗/授域分别是 L-019 / L-016 / L-015。

词汇表：classic / `SysDeptController` / `SysPostController` / `SysUserTypeController` / ancestors / `excludeChild` / `optionselect` / `options` / `deptTree` / `@DataPermission` / `checkDeptDataScope` / `R.warn` / `@TableLogic` / `@RepeatSubmit` / `kickoutUserType` / login domain / `sys_user_type_rel` / `toAjax`。

下一步：L-018 走 Client 与 SsoApp。L-019 把 `createSystemService` 的 `departments/posts/userTypes` 对上这些 URL。L-016 才解释数据范围 SQL 怎么从角色配出来。本课结束仍不是掌握证明；要练习请之后主动激活 Homework。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-004 | `backend/wta-modules/**` Controller / Service | system 公开入口在模块内，不在 `wta-admin` | `wta-system/controller/system` | 2026-09-16 |
| S-008 | `.agents/skills/engineering-standards/references/project/03-backend-module-modes.md` | `wta-system` 登记 classic：Controller→ServiceImpl→Mapper | 登记表 classic 行 | 2026-09-16 |
| S-010 | `release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql` | `sys_dept` / `sys_post` / `sys_user_post` / `sys_user_type` / `sys_user_type_rel`；登录域编码唯一键 | 表定义与 comment | 2026-09-16 |
| S-L017-01 | `.../controller/system/SysDeptController.java` | 七个公开映射；list 不分页；remove 四道 `R.warn`；optionselect；add/edit 闸 | `/system/dept` | 2026-09-16 |
| S-L017-02 | `.../controller/system/SysPostController.java` | 八个公开映射；分页 list；export void；optionselect 分支；`deptTree` | `/system/post` | 2026-09-16 |
| S-L017-03 | `.../controller/system/SysUserTypeController.java` | 九个公开映射；`/options`；`changeStatus`；`listByUser` 权限 `system:user:query`；路径数字正则 | `/system/userType` | 2026-09-16 |
| S-L017-04 | `.../service/impl/SysDeptServiceImpl.java`；`SysDeptMapper.java` | 数据权限方法；缓存；ancestors 改写；`checkDeptDataScope`；insert 不查范围 | `selectDeptList` / `insertDept` / `updateDept` / `deleteDeptById` | 2026-09-16 |
| S-L017-05 | `.../service/impl/SysPostServiceImpl.java`；`SysPostMapper.java` | `selectPagePostList` 有权限；`selectPostList` 走 `selectVoList`；删除占用抛异常 | `selectPostList` / `deletePostByIds` | 2026-09-16 |
| S-L017-06 | `.../service/impl/SysUserTypeServiceImpl.java`；`ClientSessionService.java` | 编码只读；停用 kickout；OpenAPI invalidate；删除拒引用 | `updateByBo` / `updateStatus` / `deleteWithValidByIds` / `kickoutUserType` | 2026-09-16 |
| S-L017-07 | `SystemConstants.java`；`CacheNames.java`；`R.java`；`BaseController.java`；`RepeatSubmit.java` | 默认部门 id；`NORMAL/DISABLE`；缓存名 30d；`WARN=601`；`toAjax`；5s 防重 | 常量与基类 | 2026-09-16 |
| S-L017-08 | `frontend/packages/domains/system/src/service.ts`；对应 `*Mapper.xml` | 前端 departments/posts/userTypes 映射与缺口；XML 空壳 | `departments`/`posts`/`userTypes`；空 `<mapper>` | 2026-09-16 |
