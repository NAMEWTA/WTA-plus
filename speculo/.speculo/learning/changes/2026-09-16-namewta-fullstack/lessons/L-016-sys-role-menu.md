---
lesson_id: L-016
objective_ids: [OBJ-16]
claimed_cells:
  - A:SysRoleController.*
  - A:SysMenuController.*
  - B:ISysPermissionService.getRolePermission,getMenuPermission,getDataScopeRoleMap
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: windows-on-disk
    minutes: 12
  - segment: snapshot-and-causal-chain
    minutes: 8
  - segment: visuals-and-worked-examples
    minutes: 7
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 4
expression_level: eli5
coverage_depth: deep
source_ids: [S-004, S-008, S-L016-01, S-L016-02, S-L016-03, S-L016-04, S-L016-05, S-L016-06, S-L016-07, S-L016-08]
---

# Lesson 016：徽章、走廊图、上锁的复印件

## 学完你能做什么

你能站在 `wta-system` 的 **classic** 房间里，口述 **RBAC 怎么拼起来**，而不是说一句「角色管菜单」。三块必须分开指：

1. **`SysRoleController`**（`/system/role`）：改徽章。谁戴哪枚、这枚能开哪串钥匙、这枚看得到哪几排座位。
2. **`SysMenuController`**（`/system/menu`）：改走廊。一扇窗只给**已经进门的人**当天的路线图（`getRouters`）；其余是超级管理员的车间，外加两扇给普通授权员用的勾选树。
3. **`ISysPermissionService`**：登录时把徽章和钥匙**复印进通行证夹**。以后 `@SaCheckPermission` / `@SaCheckRole` 看的是这张复印件，不是再跑一趟车间。

矩阵 (a) 两行按 Controller 聚合；chain 写 `A:SysRoleController.*` / `A:SysMenuController.*`。口试要把**磁盘上每一扇公开窗**指到 HTTP。矩阵 (a) 行名把角色/菜单的 `add`/`edit` 写窄了——源码有这两扇，本课 `*` **认全表**；漏了则 CRUD 链不满。(b) 只认三个读快照方法：`getRolePermission` / `getMenuPermission` / `getDataScopeRoleMap`。

两套名字先对齐：

| 格子 / 口试名 | Java 方法 | HTTP |
| --- | --- | --- |
| `editPermission` | `editPermission` | `PUT /system/role/permission` |
| `roleDeptTreeselect` | `roleDeptTreeselect` | `GET /system/role/deptTree/{roleId}` |
| `cancelAuthUserAll` | `cancelAuthUserAll` | `PUT /system/role/authUser/cancelAll` |
| `selectAuthUserAll` | `selectAuthUserAll` | `PUT /system/role/authUser/selectAll` |
| `getRouters` | `getRouters` | `GET /system/menu/getRouters` |
| `cascadeRemove` | 第二个 `remove`（重载） | `DELETE /system/menu/cascade/{menuIds}` |
| `getRolePermission` 等 | 同名，**不是** Controller | 无 HTTP；登录里屋调用 |

本课不宣称你会拆 `createSystemService.roles/menus`（L-019）、导航 host（L-020）、OpenAPI 机器快照（L-032）或数据权限 SQL 模板的每一个 SpEL。本课要把 **写表的窗、读树的窗、复印快照的枢纽** 讲完。

## 先把宏观地图放在桌上

L-006 / L-014 已经走过：`POST /auth/login` → Redis 通行证 → `GET /system/user/getInfo` → **`GET /system/menu/getRouters`**。L-015 是用户室。本课走进隔壁两间：**徽章室**和**走廊图纸室**，再打开登录时那台**复印机**。

`wta-system` 在模块登记表上是 **classic**：`Controller → I*Service → *ServiceImpl → Mapper`。没有 UseCase。两个 Controller 都在 `backend/wta-modules/wta-system/src/main/java/org/namewta/system/controller/system/`。认证门卫仍在 `wta-admin`（L-011），不在这里。

```text
[进门那天]
  SysLoginService.buildLoginUser
        │  三台复印机并行（virtual threads）
        ├─ getMenuPermission(userId, clientPk)   → LoginUser.menuPermission
        ├─ getRolePermission(userId, clientPk)   → LoginUser.rolePermission
        └─ selectRoles + getDataScopeRoleMap     → LoginUser.roles / dataScopeRoleMap
        │
        v
  token-session 里的 LoginUser   ← 复印件，锁进 Redis
        │
        ├─ SaPermissionImpl.getPermissionList / getRoleList
        │     只读复印件；对不上 loginId → 空集合（失败关闭）
        ├─ @SaCheckPermission / @SaCheckRole
        └─ PlusDataPermissionHandler 用 dataScopeRoleMap
              按「当前这扇窗的 perm」挑选哪些角色参与行过滤

[进门之后]
  GET /system/menu/getRouters     ← 不读复印件，当场查库画走廊
  /system/role/*  /system/menu/*（除 getRouters）← 改库
        改完：有的踢整栋 Client，有的只送一个人回家
```

**类比：** 学校有三样东西。徽章（角色）决定你是哪一班；走廊图纸（菜单）决定你能进哪间教室、墙上贴哪把钥匙码；进校时门卫把徽章和钥匙码**复印进学生证夹**（token-session）。以后门禁刷的是夹子里的复印件。走廊导视牌（`getRouters`）却是办公室现查黑板，不是复印件。改了班级课表，学校会拉火警（`kickoutClient`）把整栋楼的人请出去再进一次，复印件才能换新的。

**类比失效处：**

1. **超级管理员有两把尺子，不是一枚徽章。** `LoginHelper.isSuperAdmin(userId)` 只认石头上刻的用户 ID（`SUPER_ADMIN_USER_ID`）。它给菜单权限加 `*:*:*`、给角色权限加 `superadmin`、数据过滤直接不过滤、`getRouters` 拿**当前 Client 的全部目录/菜单**。车间窗上的 `@SaCheckRole("superadmin")` 认的是复印件里的 **roleKey 字符串**。刻了 ID 的人会被复印机**塞进**这个字符串；没刻 ID、只是被人塞进内置超级管理员角色行的人，能过车间角色闸，但**不会**自动拿到 `*:*:*` 和「不过滤行」。
2. **默认角色不是花名册上的一行。** Client 的 `defaultRoleId` 在运行时 **merge** 进权限，**不写** `sys_user_role`。所以「已分配不能删/不能停」数的是花名册；默认角色另有 `checkNotClientDefaultRole` 挡删挡停。不要把「没人在花名册上」说成「没人在用这枚徽章」。
3. **超级管理员的走廊仍按楼栋收口。** 注释写得很白：管理员拥有全部权限标识，**菜单仍按当前 Client 收敛**。`selectMenuTreeAll(clientId)`，不是全库所有 Client 的菜单并集。
4. **`getRouters` 不是菜单车间。** 动态路由没有 `system:menu:list`。`/list` 才是车间，还叠了超级管理员角色闸。

## 窗在磁盘上长什么样

2026-09-16 对过工作树。classic 房间，前缀以类上 `@RequestMapping` 为准。

### 徽章室：`SysRoleController`

文件：`.../controller/system/SysRoleController.java`。

类：`@Validated` `@RequiredArgsConstructor` `@RestController` `@RequestMapping("/system/role")`。

桌上四部电话：`ISysRoleService`、`ISysUserService`、`ISysDeptService`、`ClientSessionService`。**没有** `ISysPermissionService`——复印机不在这间屋。

| # | HTTP | 动词 | Java | 权限码 | 写库？ | 踢谁 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | `/list` | GET | `list` | `system:role:list` | 否 | 否 |
| 2 | `/export` | POST | `export` | `system:role:export` | 否（Excel 流） | 否 |
| 3 | `/{roleId}` | GET | `getInfo` | `system:role:query` | 否 | 否 |
| 4 | `/` | POST | `add` | `system:role:add` | 角色 + `sys_role_menu` | **否** |
| 5 | `/` | PUT | `edit` | `system:role:edit` | 角色基础列 | 成功则 **整栋 Client** |
| 6 | `/permission` | PUT | `editPermission` | `system:role:edit` | 角色列 + 重建菜单 + 重建部门 | 成功则 **整栋 Client** |
| 7 | `/changeStatus` | PUT | `changeStatus` | `system:role:edit` | 状态列 | 成功则 **整栋 Client** |
| 8 | `/{roleIds}` | DELETE | `remove` | `system:role:remove` | 角色及关联 | 不踢浏览器；OpenAPI 机会话按受影响用户作废 |
| 9 | `/optionselect` | GET | `optionselect` | `system:role:query` | 否 | 否 |
| 10 | `/authUser/allocatedList` | GET | `allocatedList` | `system:role:list` | 否 | 否 |
| 11 | `/authUser/unallocatedList` | GET | `unallocatedList` | `system:role:list` | 否 | 否 |
| 12 | `/authUser/cancel` | PUT | `cancelAuthUser` | `system:role:edit` | 删一行 `sys_user_role` | **那个人在该 Client** |
| 13 | `/authUser/cancelAll` | PUT | `cancelAuthUserAll` | `system:role:edit` | 批删 | 名单里每个人在该 Client |
| 14 | `/authUser/selectAll` | PUT | `selectAuthUserAll` | `system:role:edit` | 批插 | 名单里每个人在该 Client |
| 15 | `/deptTree/{roleId}` | GET | `roleDeptTreeselect` | `system:role:list` | 否 | 否 |

`DeptTreeSelectVo` 是文件末尾的 **record**，不是第 16 扇窗。`kickRoleClient` 是 **private**：读库里的 `clientId`，再 `clientSessionService.kickoutClient`。单元测试 `rolePermissionWriteInvalidatesTheRolesWholeClientBeforeSuccessReturns` 钉死顺序：先 `updateRolePermission` 成功，再 `selectRoleById`，再 `kickoutClient`。

#### 1–3. 看徽章：`list` / `export` / `getInfo`

`list`：`roleService.selectPageRoleList`。实现第一句 `requireActiveClient(role.getClientId())`——没选楼栋就抛「请选择客户端」；楼栋停用、没配登录域、登录域停用都不行。Mapper 的 `selectPageRoleList` 带 `@DataPermission`（`create_dept` / `create_by`）。超级管理员用户不过滤行；别人按复印件里的数据范围看。

`export`：`@Log EXPORT` + `POST`。查出列表后 `ExcelBuilder...toResponse`，**没有** `R` 信封。权限码是 `export` 不是 `list`。

`getInfo`：先 `checkRoleDataScope(roleId)` 再 `selectRoleById`。非超级管理员若数据权限数不着这枚徽章 →「没有权限访问部分角色数据！」。`roleId` 为 null 时检查直接 return。

#### 4. `add`：新做一枚徽章

`@Validated @RequestBody SysRoleBo`：`clientId` / `roleName` / `roleKey` / `roleSort` 必填。`@Log INSERT` + `@RepeatSubmit`。

闸：`checkRoleAllowed`（新建时禁止 `roleKey=superadmin`）→ 名称按 **Client 内** 唯一 → `roleKey` 按 **Client 内** 唯一 → `insertRole`。

`insertRole` 标 `@DSTransactional`：没 `clientId` 抛「客户端不能为空」；`insert` 角色后 **立刻** `insertRoleMenu`。`menuIds` 空或 null：菜单插入返回 1，角色照样成立。菜单必须全部属于这枚徽章的 Client，否则「菜单必须属于当前角色所在客户端」。

Controller **不** `kickRoleClient`。新徽章还没戴在任何人头上（默认角色也不能被显式分配），走廊复印件不用换。

#### 5–7. 改徽章：基础 / 权限 / 状态 三扇窗

三扇都是 `system:role:edit`，都 `@Log UPDATE` + `@RepeatSubmit`，成功都走 `kickRoleClient`。**不要说成同一个 PUT。**

`PUT /` `edit`：`@Validated`。`checkRoleAllowed` + `checkRoleDataScope` + 名称/`roleKey` 唯一。然后 **`updateRoleBaseInfo`**。实现把 BO 转实体后，**用库里的 `clientId` 覆盖**，不让你把徽章搬到另一栋楼。注释写「不更新菜单与数据权限」：它 **不** 删 `sys_role_menu` / `sys_role_dept`。停用时：是 Client 默认角色 → 不能禁用；花名册 `countUserRoleByRoleId > 0` →「角色已分配，不能禁用!」。`hasAuthorizationChange`（`roleKey` / 状态 / `dataScope` / 两个 strictly / `clientId`）为真才把受影响用户丢给 OpenAPI 作废。Controller 只要 `rows > 0` **仍会**整栋踢人——改个显示名也拉火警。

`PUT /permission` `editPermission`：**没有** `@Validated`，也不查名称唯一。`checkRoleAllowed` + `checkRoleDataScope` → **`updateRolePermission`**。`@CacheEvict(SYS_ROLE_CUSTOM)`。顺序：锁定 Client → 找出受影响用户 → `updateById` 权限相关列 → **删光旧菜单再按本次 `menuIds` 重建** → **删光旧部门再按本次 `deptIds` 重建** → OpenAPI 作废。`insertRoleDept` 对 `deptIds` 做 for-each：空数组 = 不插行；**`null` 会 NPE**。数据范围是不是「自定」都走这套重建；真正读行时只有 `DataScopeType.CUSTOM("2")` 才去用这些部门行。

`PUT /changeStatus`：body 带 `roleId`/`status`，无 `@Validated`。停用闸与 edit 相同。只 update 状态列。成功同样整栋踢。

`checkRoleAllowed` 还挡住：操作 `SUPER_ADMIN_ROLE_ID` 那一行；把已有内置 key 改掉；把别人的 key 改成 `superadmin`。

#### 8–9. `remove` / `optionselect`

`DELETE /{roleIds}`：`deleteRoleByIds`。先 `checkRoleDataScope` 整批；再对每行 `checkRoleAllowed`、默认角色不能删、花名册已分配不能删。然后删菜单关联、部门关联、角色行。`@CacheEvict` **allEntries**。Controller **没有** `kickRoleClient`。能删成功的通常本来就没人戴；OpenAPI 作废仍会跑 `findAffectedUserIdsByRoleIds`（花名册 ∪ 把这枚当默认角色的 Client 下的登录域用户）。

`GET /optionselect`：可选 `roleIds`。`selectRoleByIds` 只取 `status=NORMAL`；`inIfNotEmpty`——没传 ID 就不过滤 ID。走带 `@DataPermission` 的 `selectRoleList`。**不** `requireActiveClient`。跨楼栋能看见多少，取决于数据权限，不是 Client 闸。

#### 10–14. 给人戴徽章 / 摘徽章

`allocatedList` / `unallocatedList` 都是 `system:role:list`，转给 `ISysUserService`。Mapper 带 `@DataPermission`（部门列是 `d.dept_id`）。未分配列表先按 `roleId` 取出已戴这枚的用户 ID，再 `notIn`。

三扇授权窗都是 `system:role:edit` + `@Log GRANT` + `@RepeatSubmit`。服务层共同闸：**不允许改当前登录用户自己的角色**。

- `cancel`：body 是 `SysUserRole`（`userId`+`roleId`）。删那一行后 `kickoutUserClient(userId, role.clientId)` + 该用户 OpenAPI 作废。角色不存在或没 Client → 抛。
- `cancelAll`：`roleId` + `userIds`。批删后对每个人 `kickoutUserClient`，再 `invalidateUsers`。
- `selectAll`：Controller **多一枪** `checkRoleDataScope(roleId)`。服务层 `validateUsersHaveRoleClientType`：角色必须正常且有 Client；**禁止把 Client 默认角色显式分配**；每个人必须已经拥有该 Client 要求的登录域。插完同样按人踢。

给一个人戴/摘，只送**那个人离开这栋楼**。改徽章权限，拉**整栋**火警。别说成同一种踢法。

#### 15. `roleDeptTreeselect`

`GET /deptTree/{roleId}`。拼 record：`checkedKeys` = 该角色已勾部门；`depts` = 一棵部门树（空的 `SysDeptBo`）。**没有** `checkRoleDataScope`。有 `system:role:list` 就能问任意 `roleId` 的勾选结果（角色不存在时服务自己处理）。这是数据范围勾选树，不是菜单树。

### 走廊室：`SysMenuController`

文件：`.../controller/system/SysMenuController.java`。

类：`@Validated` `@RequiredArgsConstructor` `@RestController` `@RequestMapping("/system/menu")`。

桌上两部电话：`ISysMenuService`、`ISysRoleService`。同样 **没有** 复印机。

| # | HTTP | 动词 | Java / 格子 | 角色闸 | 权限码 | 写库？ | 踢谁 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `/getRouters` | GET | `getRouters` | **无** | **无** | 否（现查） | 否 |
| 2 | `/list` | GET | `list` | `@SaCheckRole superadmin`（OR，单值） | `system:menu:list` | 否 | 否 |
| 3 | `/{menuId}` | GET | `getInfo` | 同上 | `system:menu:query` | 否 | 否 |
| 4 | `/treeselect` | GET | `treeselect` | **无角色闸** | `system:menu:query` | 否 | 否 |
| 5 | `/roleMenuTreeselect/{roleId}` | GET | `roleMenuTreeselect` | **无角色闸** | `system:menu:query` | 否 | 否 |
| 6 | `/` | POST | `add` | `@SaCheckRole superadmin` | `system:menu:add` | 插菜单 | **否** |
| 7 | `/` | PUT | `edit` | 同上 | `system:menu:edit` | 改菜单 | 成功则整栋 Client |
| 8 | `/{menuId}` | DELETE | `remove` | 同上 | `system:menu:remove` | 删一行 | 成功则整栋 Client |
| 9 | `/cascade/{menuIds}` | DELETE | `cascadeRemove`（重载 `remove`） | 同上 | `system:menu:remove` | 批删 + 拆 `sys_role_menu` | 涉及的每栋 Client |

没有 `@SaCheckOr` 把角色闸和权限闸包成「满足一个即可」。两枚注解叠在同一方法上，是 **两道都要过**。车间（list/getInfo/add/edit/remove/cascade）= 复印件里有 `superadmin` **并且** 有对应 `system:menu:*`。勾选树两扇只有权限码，**普通授权员**只要有 `system:menu:query` 就能打开树，给角色勾走廊。

`MenuTreeSelectVo` 是 record，不是窗。

#### 1. `getRouters`：导视牌，不是车间

**无** `@SaCheckPermission`。全局登录拦截仍在（类上没有 `@SaIgnore`）。

```text
LoginUser loginUser = LoginHelper.getLoginUser();
if (loginUser == null || loginUser.getClientPk() == null)
    return R.fail("当前登录缺少客户端上下文");
menus = menuService.selectMenuTreeByUserId(loginUser.getUserId(), loginUser.getClientPk());
return R.ok(menuService.buildMenus(menus));
```

`clientPk` 就是登录时 `client.getId()` 写进夹子的主键。缺它是 **`R.fail` 句子**，不是空数组装成功信封。

`selectMenuTreeByUserId`：`clientId` 空 → 空列表。超级管理员用户 → `selectMenuTreeAll(clientId)`（该楼栋 **正常** 的目录 `M` + 菜单 `C`，不含按钮 `F`）。别人 → 自己花名册角色的树 **merge** 默认角色的树，按菜单 ID 去重。再 `TreeBuildUtils` 挂 `children`。`buildMenus` 把树收成 `RouterVo`：路由 name = `routeName + menuId`；目录 `alwaysShow`；外链/内链另包一层 children。按钮类型进不了这棵给前端的路由树。

这是 **现查黑板**。改了菜单表、还没踢人时，下一次 `getRouters` 可能已经是新走廊；门禁 `@SaCheckPermission` 仍看旧复印件。两边对不齐，就是要拉火警的原因。

#### 2–5. 车间清单 vs 勾选树

`list` / `getInfo`：超级管理员角色闸 + 权限码。`selectMenuList(menu, LoginHelper.getUserId())`：先解析 Client；超级管理员用户看该 Client **全部** 菜单行（含按钮）；别人看自己的 + 默认角色的，merge。没 Client → 空列表，不是 fail。

`getInfo`：`selectMenuById`，无数据权限注解在菜单 Mapper 上（和角色 `getInfo` 不同）。

`treeselect`：同一份 `selectMenuList`，再 `buildMenuTreeSelect`。给下拉树，不是 `RouterVo`。

`roleMenuTreeselect`：先 `roleService.selectRoleById(roleId)`；角色有 `clientId` 就塞进查询，保证树是**那枚徽章所在楼栋**的走廊。`checkedKeys` = `selectMenuListByRoleId`（尊重 `menuCheckStrictly`：严格时父节点 ID 会从勾选里拿掉，避免父被勾导致子全亮）。`menus` = 当前操作者能看见的树。角色不存在时仍返回树，只是不按角色收 Client。

#### 6–7. `add` / `edit`

都要超级管理员角色闸。`@Log` + `@RepeatSubmit` + `@Validated`。

共同闸：同父、同 Client 下菜单名唯一；外链（`isFrame=YES`）地址必须 `http(s)://`；非按钮的路由名/地址唯一。edit 额外：不能把上级设成自己。

`insertMenu`：`validateMenuClient`——必须有 Client；非根节点时父菜单必须存在且 **同一 Client**。只 `insert`，**不踢人**。新图纸还没钉到任何徽章上；超级管理员的 `getRouters` 是「该楼全部 M/C」，他们下一次拉导视牌会看见新走廊，门禁复印件里他们已有 `*:*:*`。

`updateMenu`：`clientId` 用库里的覆盖；不能把菜单挪到另一栋。`status` / `perms` / `clientId` 变了才收集受影响用户。成功：`kickoutClient` + OpenAPI 作废。

#### 8–9. 单删 vs 级联删

两扇都是 `system:menu:remove` + 超级管理员角色闸 + `@Log DELETE`。Java **两个方法都叫 `remove`**，靠路径区分。格子名 `cascadeRemove` 指带 `/cascade/` 的那扇。

单删 `DELETE /{menuId}`：

1. `hasChildByMenuId(menuId)` 为真 → **`R.warn("存在子菜单,不允许删除")`**（不是 `R.fail`）。
2. `checkMenuExistRole`（`sys_role_menu` 有行）→ **`R.warn("菜单已分配,不允许删除")`**。
3. 否则 `deleteMenuById`：删菜单行，踢该菜单的 Client，作废受影响用户。**不**在成功路径里拆角色菜单关联——因为第二闸已经禁止「还挂在徽章上」的删除。

级联 `DELETE /cascade/{menuIds}`：

1. `hasChildByMenuId(menuIdList)`：**父在名单里、孩子不在名单里** 才算还有孩子。父+子一起提交，这闸能过。
2. **没有** `checkMenuExistRole`。
3. `deleteMenuById(Collection)`：删菜单行，**`roleMenuMapper.deleteByMenuIds`** 把挂在这些菜单上的徽章钉子拔掉，再对涉及的每个 `clientId` `kickoutClient`，再 OpenAPI 作废。

口诀：单删怕孤儿孩子、怕拆别人的徽章钉子，用 `warn` 停住；级联允许你把整枝一起带走，并且**会拆钉子**。不要把两扇说成「批量只是循环单删」。

### 复印机：`ISysPermissionService`

接口：`.../service/ISysPermissionService.java`。实现：`SysPermissionServiceImpl`，同时实现 common-core 的 **`PermissionService`**（只有前两个方法）。`getDataScopeRoleMap` **不在** SPI 里，只在 system 这份接口。

调用方在登录里屋：`SysLoginService.buildLoginUser` 用 `client.getId()` 当 `clientId` 参数（就是后来的 `clientPk`）。三枪和岗位查询走 `ThreadUtils.virtualInvokeAll`。

#### `getRolePermission(userId, clientId)` → `Set<String>`

空集合起步。若 `LoginHelper.isSuperAdmin(userId)`，先放入 **`superadmin`**。再 `roleService.selectRolePermissionByUserId`：`selectRolesByUserId`（花名册 **merge 默认角色**，且 `requireActiveClient`）后，把每枚 `roleKey` **按逗号切开** 丢进 Set。所以复印件里的角色权限是 **字符串徽章**，不是角色主键。`@SaCheckRole` 刷的是这些字符串。

Client 停用时，这里会抛「客户端不存在或已停用」——发生在拼 `LoginUser` 时，进不了成功会话。

#### `getMenuPermission(userId, clientId)` → `Set<String>`

超级管理员用户先放入 **`*:*:*`**。再 `menuService.selectMenuPermsByUserId`：用户花名册角色（角色状态必须 `NORMAL`，菜单/角色都要落在该 Client）上的 `perms`，**再加上默认角色**的 `perms`。空/空白 `perms` 丢掉。这里 **不按逗号切** 菜单 `perms` 字段——库里一格就是一个权限码。

`*:*:*` 让 Sa-Token 门禁对超级管理员用户放行。后面仍塞进该楼真实 perm，是附加，不是「只查自己的、不给通配」。

#### `getDataScopeRoleMap(roles)` → `Map<String, List<Long>>`

入参是已经选好的 `List<RoleDTO>`（登录那一枪里，来自 **同一 Client** 的花名册 + 默认角色）。空/null → 不可变 `Map.of()`。

否则取出 `roleId` 列表 → `menuService.selectMenuPermsByRoleIds`（停用角色进不了；得到 `roleId → Set<perms>`）→ **倒过来**：每个 perm 收集拥有它的 `roleId` 列表。`LinkedHashMap`，同一 perm 下角色按遍历顺序追加。

`PlusDataPermissionHandler` 在非超级管理员请求上：看当前方法的 `@SaCheckPermission` 值，到这张图里找出「哪些角色主键有这把钥匙」，再用这些角色的 `dataScope`（1 全部 / 2 自定部门 / 3 本部门 / 4 部门及以下 / 5 仅本人 / 6 部门及以下或本人）拼 SQL。**不是**你身上所有角色一起过滤每一张表；是「这扇窗的钥匙」选出角色子集。超级管理员用户在 handler 入口直接跳过过滤。

OpenAPI 机器会话另有 `SystemOpenApiAuthorizationResolver` 自己拼一份不可变快照，**不是**这个接口。本课 (b) 不认那份工厂。

`SaPermissionImpl`：每次鉴权从 **当前 token** 取 `LoginUser`；`loginId` 对不上或没有人 → **空列表**，不是回库重查。改了徽章却没踢掉旧夹子，门禁仍按旧复印件；导视牌却可能已经换了。这就是 RBAC 的两口钟。

## 核心概念与机制

### 直觉讲解

想象你进学校。门卫不每天去教室点名，而是早上把「你是哪一班、你有哪串钥匙」印在学生证夹里。教室门锁认夹子。走廊牌子认黑板。改课表的人如果只改黑板、不换夹子，你会走进一间锁着的教室：牌子说可以进，门锁说不行。所以改课表的人会拉火警，请整栋人出去再进，夹子重印。

车间（改走廊图纸）只有校长胸牌能进。给班级发钥匙的老师，可以看勾选树，但不能改建走廊。导视牌谁进了这栋楼都能拉——拉到的是**自己被允许的路**，不是车间清单。

### 精确定义与 English term

| 中文 | English term | 本课的磁盘含义 |
| --- | --- | --- |
| 基于角色的访问控制 | RBAC (role-based access control) | 用户—角色—菜单三张关系表 + 登录快照 + 现查路由 |
| 角色 | role | `sys_role`；`roleKey` 进 `rolePermission`；主键进数据范围图 |
| 菜单 / 动态路由 | menu / dynamic route | `sys_menu`；`perms` 进 `menuPermission`；`M`/`C` 进 `RouterVo` |
| 权限码 | permission string | 如 `system:role:edit`；Sa-Token 精确匹配 |
| 登录快照 | login snapshot | `LoginUser` 里三份集合/映射，锁在 token-session |
| 数据范围 | data scope | 角色字段 `dataScope` `1`–`6`；按当前接口 perm 选角色再拼行过滤 |
| 客户端 / 楼栋 | Client | 角色、菜单、默认角色、会话踢出都以 Client 主键收口 |
| 默认角色 | client default role | `SysClient.defaultRoleId`；merge 进运行时，不写花名册 |
| 会话失效 | session invalidation | `kickoutClient` 整栋；`kickoutUserClient` 一人一栋；`invalidateByUserId` 机器会话 |
| classic 模块 | classic module | Controller 持 Service，ServiceImpl 持 Mapper；无 UseCase |

`ISysPermissionService` 的英文职责是 **permission snapshot builder at login**，不是运行时授权器。运行时授权器是 Sa-Token + `SaPermissionImpl` + `PlusDataPermissionHandler`。

### 机制/因果链

1. **写。** 徽章室改 `sys_role` / `sys_role_menu` / `sys_role_dept` / `sys_user_role`。走廊室改 `sys_menu`（级联还改 `sys_role_menu`）。classic 事务注在 ServiceImpl 的 `@DSTransactional`，不在 Controller。
2. **踢。** 改徽章基础/权限/状态、改菜单、删菜单：Controller 或 Service 拉整栋 `kickoutClient`（匹配 `LoginUser.clientPk`）。给人戴/摘徽章：只 `kickoutUserClient`。机器调用另走 `OpenApiMachineSessionInvalidator`。add 新徽章、add 新菜单（未挂角色）：不踢。
3. **再进门。** 策略调用 `buildLoginUser` → 三台复印机按 **当前 Client 主键** 印 `rolePermission` / `menuPermission` / `dataScopeRoleMap`（默认角色 merge 进来；超级管理员用户另贴通配）。
4. **刷门禁。** `@SaCheckPermission` 读 `menuPermission` 复印件。`@SaCheckRole` 读 `rolePermission` 复印件。对不上当前 token → 空集合 → 拒绝。
5. **看行。** 带 `@DataPermission` 的 Mapper（角色列表、已分配用户…）用 `dataScopeRoleMap[当前窗 perm]` 选出角色，再按各角色 `dataScope` 拼 SQL。超级管理员用户跳过。
6. **看路。** `getRouters` 无视复印件里的菜单集合，按 userId+clientPk **现查** 树。超级管理员用户现查该楼全部 M/C。

失败怎么走：缺 Client 上下文的 `getRouters` → `R.fail` 句子，库不动。角色名撞车 → `R.fail` 句子，不 insert。停用已分配角色 → `ServiceException`，事务回滚。单删仍有孩子或仍挂角色 → `R.warn`，不删。级联仍有名单外的孩子 → `R.warn`。`editPermission` 菜单跨 Client → 事务回滚，**还没走到** `kickRoleClient`（Controller 只在 `> 0` 之后踢）。

### 图、表或文本图

```text
        sys_user ──sys_user_role── sys_role ──sys_role_menu── sys_menu.perms
                         │              │                         │
                         │              ├──sys_role_dept── sys_dept
                         │              │
                         │              └── SysClient.defaultRoleId  (虚线 merge，不写花名册)
                         │
              登录复印机 ISysPermissionService
                         │
              LoginUser { rolePermission, menuPermission, dataScopeRoleMap, clientPk }
                         │
         ┌───────────────┼────────────────┐
         v               v                v
   @SaCheckRole   @SaCheckPermission   行过滤（按当前 perm 选角色）
   (roleKey)      (perms / *:*:*)      DataScopeType 1..6
                         │
                         x  不经过
                         v
                  GET /getRouters  (现查 sys_menu 树 → RouterVo)
```

**文字等价物：** 用户通过花名册连到角色，角色通过钉子连到菜单权限码，角色还可以钉到部门（自定数据范围）。Client 默认角色像虚线：运行时并进权限，不在花名册落行。登录把角色字符串、菜单权限码、以及「权限码 → 角色 ID 列表」三样锁进 `LoginUser`。门禁两道注解和行过滤都读这把锁。动态路由不走这把锁，直接按人+楼查菜单树。虚线默认角色和实线花名册在复印时已经合并；导视牌对非超级管理员也会 merge 默认角色的走廊。

**图的边界：** 不画岗位、登录域、OpenAPI HMAC、前端 `createSystemService`。不画 `cleanOnlineUserByRole`（接口上有，本课 Controller **零调用**）。菜单按钮 `F` 进 `perms` 复印件，不进 `getRouters` 树。图上的 `clientPk` 与服务参数名 `clientId` 是同一主键。

## 正例、反例与边界

**正例 1：** 口述 `getRouters`。打开 `SysMenuController.getRouters`：没有权限注解；`clientPk == null` → `R.fail("当前登录缺少客户端上下文")`；然后 `selectMenuTreeByUserId` + `buildMenus`。对照 L-014：前端 `getMenus` 打的就是这扇，不是 `/list`。

**正例 2：** 改一枚徽章的菜单。`PUT /system/role/permission`。Controller 不查重名。服务重建 `sys_role_menu` / `sys_role_dept`。返回前 `kickoutClient(dbRole.clientId)`。测试方法名已经把「整栋 Client、成功返回之前」写死。

**正例 3：** 登录复印。打开 `SysLoginService.buildLoginUser`：三枪 `getMenuPermission` / `getRolePermission` / `getDataScopeRoleMap(roleDtos)`，`clientId` 来自 `client.getId()`。打开 `SysPermissionServiceImpl`：超级管理员用户 **先 add 通配/superadmin，再 addAll 该楼真实集合**。

**正例 4：** 默认角色。`selectRolesByUserId` 调 `mergeDefaultRole`：已在列表里只打标 `clientDefault`；不在则 add 进去。`insertAuthUsers` 若目标是默认角色 →「客户端默认角色由系统自动授予，不能显式分配」。停用/删除默认角色 →「角色已被客户端设为默认角色，不能…」。

**正例 5：** 单删 vs 级联。单删两道 `R.warn`。级联 `hasChildByMenuId(Collection)` 带 `.notIn(SysMenu::getMenuId, menuIds)`，并且 `deleteByMenuIds`。两个 Java 方法都叫 `remove`。

**正例 6：** 车间 vs 勾选树。`list` 叠 `@SaCheckRole(superadmin)` + `system:menu:list`。`roleMenuTreeselect` 只有 `system:menu:query`。给角色发钥匙的人可以打开树，不能改建走廊。

**正例 7：** 数据范围图。`getDataScopeRoleMap` 把「角色 → perms」倒成「perm → 角色 ID」。`PlusDataPermissionHandler.scopeRoles` 用**当前窗**的 perm 去这张图里取角色。角色列表查询自己的 `@DataPermission` 就吃这套。

**正例 8：** 给人戴徽章。`selectAuthUserAll` 先 `checkRoleDataScope`，再校验登录域，再插入，再按人 `kickoutUserClient`。不是整栋火警。

**反例 1：** 「`GET /system/menu` 就是动态路由。」动态路由是 **`/getRouters`**。`/list` 是车间。

**反例 2：** 「`ISysPermissionService` 每个请求都会再查一遍权限。」登录时查一次，锁进夹子。之后 `SaPermissionImpl` 只读夹子。

**反例 3：** 「超级管理员用户的 `getRouters` 返回全库菜单。」`selectMenuTreeAll(clientId)`，按楼栋。

**反例 4：** 「`isSuperAdmin` 就是 `roleKey=superadmin`。」前者是用户 ID 常量；后者是复印件字符串。车间角色闸认后者；通配 perm 和跳过行过滤认前者。

**反例 5：** 「`edit` 和 `editPermission` 一个意思，都是改角色。」一个不重建钉子；一个拆光菜单钉和部门钉再钉回去。HTTP 都是 PUT，路径不同。

**反例 6：** 「级联删除就是循环调用单删。」级联不检查「菜单已分配」，会拆 `sys_role_menu`；单删遇到分配会 `warn` 停住。

**反例 7：** 「改角色权限只踢戴了这枚徽章的人。」Controller 调 `kickoutClient`，这栋楼所有 token 都走。戴/摘才按人踢。

**反例 8：** 「`add` 角色成功会踢会话，因为权限变了。」`add` 没有 `kickRoleClient`。

**反例 9：** 「`getRolePermission` 返回角色 ID。」返回的是切开的 `roleKey` 字符串。

**反例 10：** 「`PermissionService`（common-core）也有 `getDataScopeRoleMap`。」没有。第三枪只在 `ISysPermissionService`。

**反例 11：** 「默认角色在 `sys_user_role` 里，所以 allocatedList 能看见。」默认角色不写花名册。allocatedList 走花名册 join。

**反例 12：** 「`optionselect` 和 `list` 一样必须选 Client。」`list` 的服务会 `requireActiveClient`；`optionselect` 不会。

**反例 13：** 「矩阵没写 `add`/`edit`，所以这两扇窗不存在。」磁盘上有。本课 `*` 认全表。

**反例 14：** 「`cascadeRemove` 是另一个类。」它是同一 Controller 里重载的 `remove`。

**反例 15：** 「`getRouters` 读 `loginUser.getMenuPermission()`。」它读库。复印件里的 perm 管门禁，不管画树。

**边界：**

- `SysRoleBo.clientId` 在 **body + `@Validated`** 的 add/edit 上必填；GET `list` 参数没有方法级 `@Valid`，真正闸在 `requireActiveClient`。
- `editPermission` / `changeStatus` 无 `@Validated`。
- `insertRoleDept`：`deptIds == null` 会 NPE；空数组安全。
- `roleKey` 可逗号分隔，进 `rolePermission` 时切开；菜单 `perms` 一格一码，不切。
- 角色名 / `roleKey` 唯一范围是 **同一 Client**，不是全局。
- 停用角色：`selectMenuPermsByRoleIds` / 用户 perm 查询都要求角色 `NORMAL`，停用的徽章不再给新复印件贡献钥匙；旧夹子仍要靠踢才能死。
- `checkRoleAllowed` 不挡 `selectAuthUserAll` 把人塞进内置超级管理员**角色行**。那会让对方复印件出现 `superadmin` 字符串（过车间角色闸），但不会让 `isSuperAdmin(userId)` 变真（没有 `*:*:*`、不过滤行也不会跳过），除非对方本来就是那颗石头 ID。
- `cleanOnlineUserByRole` 在 `ISysRoleService` 上，本课 Controller 不用；现役踢法是 `ClientSessionService`。
- 前端 URL 表、图标协议、路由 `name` 规则的验收合同在 fullstack skill；本课只钉 Java 侧 `buildMenus` 的 name 拼接。
- OpenAPI 授权快照不可变（测试里 `add`/`clear` 会抛）是另一条生产线。

## 变式与迁移

- **变式 A：新业务要加一把钥匙。** 先在走廊车间加按钮菜单（`menuType=F`，填 `perms`），再在徽章室 `editPermission` 把按钮钉到角色上，再拉火警。不要只在前端藏按钮。不要把 perm 写进 `AuthController`。
- **变式 B：同一用户进两栋楼。** 权限按 `clientPk` 收口。换 Client 登录会印另一份夹子。不要假设「超级管理员在 A 栋看见的菜单」会在 B 栋自动出现。
- **变式 C：只想让某人立刻丢掉一把钥匙。** 摘徽章走 `cancel`/`cancelAll`（按人踢）。改整枚徽章的菜单走 `editPermission`（整栋踢）。不要对「一个人」调用 `kickoutClient` 当精确手术。
- **变式 D：默认角色当「这栋楼人人都有的底包」。** 配在 Client 上，不要往花名册插。不要用 `selectAuthUserAll` 发默认角色。停用默认角色前先换 Client 配置。
- **变式 E：菜单要连枝删。** 走 `/cascade/{ids}`，把父和子都放进名单。走单删会 `warn`。接受级联会拆 `sys_role_menu`。
- **变式 F：授权员需要勾菜单，但不能改建走廊。** 给他们 `system:menu:query`（以及角色侧 `system:role:edit`），不要给他们 `superadmin` 角色字符串。车间 list/add/edit/remove 过不去角色闸。
- **变式 G：改了 `dataScope` 却行过滤没变。** 确认走的是 `editPermission`（或基础 edit 真的带了非空 `dataScope` 且 MP 更新策略写了列），确认踢过会话，确认当前接口 perm 在 `dataScopeRoleMap` 里能命中这枚角色。超级管理员用户本来就不会过滤。
- **迁移口诀：** 先分清三台机器（写徽章、写走廊、登录复印）→ 再分清两口钟（复印件门禁 vs 现查导视牌）→ 再分清两种踢（整栋 vs 一人）→ 再分清两种超级（石头 ID vs `superadmin` 字符串）→ 默认角色走虚线。跳步就会出现「前端路由有了、接口 403」或「改了一个角色、整栋人掉线却说课讲错」。

## 常见误区

1. **「RBAC 就是 `SysRoleController`。」** 缺菜单车间和登录复印机，门禁和导视牌对不上。
2. **「`getRouters` 要 `system:menu:list`。」** 不要。那是车间码。
3. **「权限每次请求现查库。」** 门禁读复印件；导视牌现查；行过滤读复印件里的图。
4. **「`clientId` 参数和 `clientPk` 是两样东西。」** 登录写入时是同一个主键；服务方法参数常叫 `clientId`。
5. **「classic 所以可以在 Controller 里持 Mapper。」** 登记表仍是 Controller → Service → ServiceImpl → Mapper。这两个 Controller 没有 Mapper 字段。
6. **「`R.warn` 等于成功删除。」** 单删两道 warn 是拒绝。信封形状不是 `fail`，库没删。
7. **「矩阵行名是口试清单的上限。」** 行名写窄了 `add`/`edit`；`*` 与 CRUD 链要求认全表。
8. **「踢会话 = 复印机关掉。」** 踢的是 token。复印机在下次 `buildLoginUser` 才会再跑。
9. **「`getMenuPermission` 的 `*:*:*` 让超级管理员看见所有 Client 的菜单。」** 那是门禁通配。树仍按楼栋。
10. **「已分配不能删，所以默认角色也能靠花名册保护。」** 默认角色不在花名册；另有默认角色闸。
11. **「`ISysPermissionService` 在角色 Controller 里被调用。」** 全文没有。它在 `SysLoginService`。
12. **「`selectAuthUserAll` 和 `editPermission` 一样整栋踢。」** 前者按人；后者整栋。
13. **「两个 `remove` 可以随便替换。」** 路径、孩子检查、是否拆 `sys_role_menu` 都不同。
14. **「数据范围是用户身上的一个字段。」** 在角色上。再经 perm→角色图，按**当前接口**选用。

## 非评分暂停

打开磁盘，不要凭记忆默写路径。不要改文件。没有标准答案栏。

1. 打开 `SysRoleController.java` 和 `SysMenuController.java` 的类注解。数清每一对 HTTP 映射。把 Java 名写左边、格子名写右边，圈出 `cascadeRemove` 对应的那个重载、以及矩阵行名漏掉的 `add`/`edit`。
2. 用手指划 `getRouters`：有没有 `@SaCheckPermission`；`clientPk` 空时返回什么句子；下一行查库还是读 `loginUser.getMenuPermission()`。再划 `list`：两枚注解是什么。
3. 打开 `editPermission` 与 private `kickRoleClient`。对照 `AuthorizationInvalidationCallSiteUnitTest` 的 verify 顺序。再打开 `selectAuthUserAll`，看它踢的是 `kickoutClient` 还是服务里的 `kickoutUserClient`。
4. 打开 `SysPermissionServiceImpl` 三个方法。对超级管理员用户各说一句「先放什么，再 addAll 什么」。打开 `SysLoginService.buildLoginUser`，确认第三枪的 `roles` 从哪来、`clientId` 从哪来。
5. 打开单删和级联删。圈出 `R.warn` 两句、`notIn` 那行、`deleteByMenuIds`。想一句：哪扇会拆徽章上的钉子。

## 总结、词汇表与下一步

- **徽章室** `/system/role`：15 扇公开窗。`list`/`export` 要活 Client；`edit` / `editPermission` / `changeStatus` 成功拉整栋火警；戴/摘按人踢；`add` 不踢；删已分配/默认角色会停住。`add`/`edit` 磁盘上在，本课 `*` 认。
- **走廊室** `/system/menu`：`getRouters` 无权限注解、要 `clientPk`、现查树。车间叠 `superadmin` 角色闸。勾选树只有 `system:menu:query`。单删 `warn` 护孩子和钉子；级联可带整枝并拆钉子。`add`/`edit` 同样认。
- **复印机** `ISysPermissionService`：登录时印三样。`getRolePermission` = `roleKey` 切片（超级管理员用户另贴 `superadmin`）。`getMenuPermission` = 菜单 `perms`（另贴 `*:*:*`）。`getDataScopeRoleMap` = perm → 角色 ID。SPI `PermissionService` 没有第三枪。之后门禁只读复印件；导视牌不读复印件。
- **两把尺子、两种踢、一条虚线默认角色、按楼栋收口。** classic，不经过 UseCase。

词汇表：RBAC / role / menu / dynamic route / `getRouters` / permission string / login snapshot / data scope / `dataScopeRoleMap` / Client / `clientPk` / default role / `kickoutClient` / `kickoutUserClient` / `@SaCheckPermission` / `@SaCheckRole` / `*:*:*` / `superadmin` / classic / `sys_role_menu` / cascade delete。

下一步：L-017 口述部门/岗位/登录域。L-018 拆 Client 与 SSO 应用（默认角色挂在 Client 上）。L-019 从前端 `createSystemService.roles/menus` 打回本课这些 HTTP。L-020 看导航 host 怎么吃 `RouterVo`。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-004 | `backend/wta-modules/**` Controller / Service | system 公开入口在模块内，不在 `wta-admin` 门卫 | 模块树 | 2026-09-16 |
| S-008 | `.agents/skills/engineering-standards/references/project/03-backend-module-modes.md` | `wta-system` = classic；`Controller → Service → ServiceImpl → Mapper` | 当前登记表 | 2026-09-16 |
| S-L016-01 | `SysRoleController.java` | 15 扇映射；`kickRoleClient`；`edit` vs `editPermission` vs `changeStatus`；授权三窗；`DeptTreeSelectVo` | 类与各方法 | 2026-09-16 |
| S-L016-02 | `SysMenuController.java` | `getRouters` 无权限注解与 `clientPk` fail；车间角色闸；勾选树无角色闸；两个 `remove`；`add`/`edit` 外链与唯一 | 类与各方法 | 2026-09-16 |
| S-L016-03 | `ISysPermissionService.java`、`SysPermissionServiceImpl.java`、`PermissionService.java` | 三枪签名；超级管理员先贴再 addAll；SPI 无第三枪 | 接口与实现 | 2026-09-16 |
| S-L016-04 | `SysRoleServiceImpl.java`、`SysRoleMapper.java`、`SysRoleBo.java` | Client 闸、默认角色 merge、唯一按 Client、`updateRolePermission` 重建钉子、戴摘踢人、不能显式分配默认角色、`@DataPermission` | 对应方法 | 2026-09-16 |
| S-L016-05 | `SysMenuServiceImpl.java`、`SysMenuMapper.java` | 现查树、默认角色 merge、`hasChild` 的 notIn、级联拆 `role_menu`、insert 不踢、update/delete 踢 Client | 对应方法 | 2026-09-16 |
| S-L016-06 | `SysLoginService.buildLoginUser`、`SaPermissionImpl.java`、`LoginHelper.isSuperAdmin`、`SystemConstants` | 登录三枪并行；鉴权只读复印件；石头 ID vs `superadmin` / `*:*:*` | 对应方法与常量 | 2026-09-16 |
| S-L016-07 | `ClientSessionService.java`、`PlusDataPermissionHandler.java`、`DataScopeType.java`、`LoginUser.java`、`RoleDTO.java` | 整栋/按人踢；perm→角色再按 1–6 拼 SQL；快照字段 | 对应类型 | 2026-09-16 |
| S-L016-08 | `AuthorizationInvalidationCallSiteUnitTest.java` | `editPermission` 成功后、返回前 `kickoutClient` | `rolePermissionWriteInvalidatesTheRolesWholeClientBeforeSuccessReturns` | 2026-09-16 |
