---
lesson_id: L-015
objective_ids: [OBJ-15]
claimed_cells: [A:SysUserController.*, A:SysUserCredentialController.candidate, A:SysTemporaryPasswordController.issue, B:SysTemporaryPasswordController.issue]
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: method-table-on-disk
    minutes: 11
  - segment: candidate-and-one-shot-redis
    minutes: 8
  - segment: visuals-and-worked-examples
    minutes: 7
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 5
expression_level: eli5
coverage_depth: deep
source_ids: [S-004, S-008, S-L015-01, S-L015-02, S-L015-03, S-L015-04, S-L015-05, S-L015-06, S-L015-07, S-L015-08]
---

# Lesson 015：人事柜台——SysUser 与两把钥匙

## 学完你能做什么

打开 `backend/wta-modules/wta-system/src/main/java/org/namewta/system/controller/system/` 里**三份** Java，你能**口述公开方法链**：谁改用户表、谁只打印候选、谁往 Redis 塞 60 秒一次性纸条。口试名单就是这三格，不是资料页、不是登录策略全文：

1. **`A:SysUserController.*`**：同一门牌 `/system/user` 上，**磁盘真实**的 17 个 Java 公开方法（含两个叫 `getInfo` 的重载）。矩阵 (a) 那一行目前写成 14 个名字，**漏了 `add` / `edit`**。口试按磁盘，不要把漏行背成「没有新增/修改」。
2. **`A:SysUserCredentialController.candidate`**：`POST /system/user/resetPwd/candidate`。打印可编辑的**永久密码候选**，**不改** `sys_user.password`。
3. **`A:SysTemporaryPasswordController.issue`** + **`B:SysTemporaryPasswordController.issue`**：`POST /system/user/temporaryPassword`。签发 60 秒、单次消费的临时密码；Redis 只存 BCrypt 哈希；明文只在这一次 HTTP 响应里露面。

你还能把三句话分清，不揉成「改密码」：

- **候选 ≠ 已改锁。** `candidate` 和「无 userId 的 `GET /system/user/`」只 `generateDefaultPassword()`，零 `resetUserPwd`。
- **永久重置 ≠ 临时纸条。** `PUT /resetPwd` 验明文、BCrypt、写用户表；临时签发**不碰**用户表密码列。
- **临时 Redis ≠ 错次 Redis。** 临时键是 `auth:temporary-password:user:{userId}`；解锁擦的是 `pwd_err_cnt:{userName}`。两只邮箱不是同一只。

本课不宣称你会拆 `SysRoleController` / `SysMenuController`（L-016）、部门岗位登录域（L-017）、`createSystemService` 整张表（L-019）、`SysProfileController.updatePwd`（L-022），或把 `PasswordAuthStrategy.authenticate` 再讲一遍（L-013 已认消费顺序）。登录消费只作为 (b)「一次性」的闭合点：issue 写入的那一格，要等人拿纸条进门才 CAS 删掉。

`wta-system` 在登记表是 **classic**：`Controller → ISysUserService / ServiceImpl → Mapper`。临时密码是 classic 房间里多出来的 **Redis 小柜子**（`TemporaryPasswordService` + `RedisTemporaryPasswordStore`），不是五层 UseCase。不要把这三份 Controller 口述成 layered。

## 先把宏观地图放在桌上

L-003 已经把 `wta-system` 钉在 classic 列。L-012 点名过 `generateDefaultPassword` / `generateTemporaryPassword` 的 HTTP 调用方，格子留给本课。L-014 的 `getInfo` 打的是 **`GET /system/user/getInfo`**——本课第一扇「我是谁」窗，不是管理端那扇「查某个人」窗。

2026-09-16 工作树里，三份类**共用**类上 `@RequestMapping("/system/user")`，分文件不是分门牌：

```text
浏览器 / admin-web 用户页
        │
        v
/system/user/*          ← 一块门牌，三份 Java
        │
        ├─ SysUserController            classic 人事柜台（17 个公开方法）
        │     GET  /list /getInfo /{userId} /optionselect /authRole/{id}
        │          /deptTree /list/dept/{deptId} /unlock/{userId}
        │     POST /export /importData /importTemplate  以及 POST /
        │     PUT  /  /resetPwd /changeStatus /authRole
        │     DELETE /{userIds}
        │
        ├─ SysUserCredentialController  只一扇：POST /resetPwd/candidate
        │     打印永久候选，不改用户
        │
        └─ SysTemporaryPasswordController  只一扇：POST /temporaryPassword
              60s Redis 哈希；明文只回这一枪
```

| 类 | 磁盘路径 | 公开方法数 | 写 `sys_user.password`？ | 写临时 Redis？ |
| --- | --- | --- | --- | --- |
| `SysUserController` | `.../controller/system/SysUserController.java` | 17（两个 `getInfo`） | **仅** `add` / `resetPwd`；导入监听在 `importData` 里另走 generate | 否 |
| `SysUserCredentialController` | 同目录 sibling | 1：`candidate` | **否** | 否 |
| `SysTemporaryPasswordController` | 同目录 sibling | 1：`issue` | **否** | **是**（委托 `TemporaryPasswordService.issue`） |

**类比：** 人事科门口挂一块牌子「用户」。左边一长排窗办档案：列表、新增、改资料、开锁计数、授权角色。角落有一台**打印机**（候选）：纸上印一串建议密码，锁没换。另一台是**60 秒传真**（临时密码）：纸会自己烧掉，烧完进不了门；柜子里只留一截烤过的哈希，不把明文钉在档案袋上。

**类比失效处：**

1. 三份 Java 不是三间房。URL 都在 `/system/user` 底下。前端 `createSystemService().users` 也是同一只对象打这三份类。
2. 「打印机」不是 `PUT /resetPwd`。管理端对话框先打 candidate 填表，点确定才走 resetPwd。只打开对话框 ≠ 锁已换。
3. 「传真」不是用户表那一列。`issue` 全文不调 `resetUserPwd`。永久哈希仍是旧的。
4. `GET /getInfo`（当前登录者）和 `GET /system/user/`、`GET /system/user/{userId}`（管理表单）都叫 `getInfo`，**不是同一扇窗**。
5. `users.profile` / `updatePassword` 打的是 **`/system/user/profile*`**，类是 `SysProfileController`（L-022），不在本三份文件里。
6. 临时消费发生在门厅 `PasswordAuthStrategy`（以及 SSO 的 `AdminSsoIdentityService.verifyPassword`），不在 `SysTemporaryPasswordController` 方法体里。Controller 只负责签发。

## 核心概念与机制

### 直觉讲解

小孩子版只记九句：

1. **一块牌子，三份源码。** 找映射先打开三份 Controller，不要只打开 `SysUserController.java`。
2. **`.*` 按磁盘数。** 17 个 Java 方法。矩阵漏了新增/修改，口试不能跟着漏。
3. **两扇 `getInfo`。** `/getInfo` 是「我是谁」；`/` 与 `/{userId}` 是「查/建这个人」。无 userId 时还往 Vo 里塞一串默认候选密码。
4. **新增验你带来的明文。** `add` 走 `validateOrThrow(password)` 再 `BCrypt.hashpw`，**不**调用 `generateDefaultPassword`。
5. **改资料不改锁。** `edit` 不验密、不哈希。实体 `password` 的 `updateStrategy = NOT_EMPTY`，空串不会把旧哈希覆盖掉。
6. **候选只打印。** `POST /resetPwd/candidate`：闸 `checkUserAllowed` + `checkUserDataScope`，然后 generate，返回 `Cache-Control: no-store`。
7. **临时只塞 Redis。** `issue` 用 `generateTemporaryPassword()`（**永远随机**，不理 FIXED 默认值），哈希进 `auth:temporary-password:user:{id}`，TTL **正好 60 秒**。再签发会覆盖并重置 TTL。
8. **一次性是 CAS 删除。** 登录先对永久哈希；不对再 `verify`；Client 准入之后才 `compareAndDelete`。并发两个赢家只许一个删掉。Redis 故障 → 校验失败关闭，不退回「当永久密码」。
9. **解锁擦的是错次。** `GET /unlock/{userId}` 删 `pwd_err_cnt:` + 用户名。它**不是**作废临时密码。

### 精确定义与 English term

| 中文 | English | 精确定义（本课，以工作树为准） |
| --- | --- | --- |
| 用户控制器 | `SysUserController` | classic `@RestController`，`@RequestMapping("/system/user")`，继承 `BaseController`。人事 CRUD + 导入导出 + 当前用户信息 + 永久重置 + 解锁错次 + 按 Client 授权角色 |
| 凭据候选控制器 | `SysUserCredentialController` | 同门牌另一份类。公开方法只有 `candidate` |
| 临时密码控制器 | `SysTemporaryPasswordController` | 同门牌第三份类。公开方法只有 `issue` |
| 当前用户信息 | `GET /system/user/getInfo` | 无 `@SaCheckPermission`。读 `LoginHelper.getLoginUser()`，`DataPermissionHelper.ignore` 查自己，返回 `UserInfoVo`（`user` + 会话里的 `permissions`/`roles`） |
| 管理端用户详情/初始化 | `GET /system/user/` 与 `GET /system/user/{userId}` | Java 名也是 `getInfo`。权限 `system:user:query`。返回类型在源码里是 **`ResetPasswordCandidateVo`**（继承 `SysUserInfoVo`，多一个 `password`） |
| 永久密码候选 | reset-password candidate | `generateDefaultPassword()` 的结果。FIXED 模式直接返回配置值；RANDOM 走生成器。HTTP：无 userId 的详情窗，或 `POST /resetPwd/candidate` |
| 永久重置 | `resetPwd` | `PUT /system/user/resetPwd`，`@ApiEncrypt`，权限 `system:user:resetPwd`。验明文 → BCrypt → `resetUserPwd` 写用户表 |
| 临时密码签发 | issue temporary password | `POST /system/user/temporaryPassword`，权限 **`system:user:temporaryPassword`**（独立于 resetPwd） |
| 临时密码存储 | `TemporaryPasswordStore` / `RedisTemporaryPasswordStore` | 键 `auth:temporary-password:user:{userId}`。`store` 覆盖哈希+TTL；`read` 读哈希；`compareAndDelete` 是 Lua：值相等才 `DEL` |
| 已校验凭据 | `VerifiedPassword` | 仅登录路径内存对象，`toString` 把哈希打成 `<redacted>`。不是 HTTP VO |
| 一次性 | one-shot / compare-and-delete | 成功登录（且走临时分支）后原子删键；过期、再签发、并发失败者都让下一次 `consume` 失败 |
| 用户允许闸 | `checkUserAllowed` | 目标 `userId` 等于超级管理员常量 `1761100000000000001L` → `ServiceException("不允许操作超级管理员用户")` |
| 数据权限闸 | `checkUserDataScope` | 当前操作者不是超管时，`countUserById==0` → 「没有权限访问用户数据！」 |
| 错次键 | `pwd_err_cnt:` | `CacheNames.PWD_ERR_CNT_KEY`。解锁和登录失败计数共用。**不是**临时密码键 |
| 默认密码生成 | `generateDefaultPassword` | 可 FIXED。给候选和导入新用户 |
| 临时密码生成 | `generateTemporaryPassword` | **始终** `generate(currentPolicy())`，不读 FIXED |

**`UserInfoVo` ≠ `SysUserInfoVo` ≠ `ResetPasswordCandidateVo`。** 当前用户窗用 `UserInfoVo`（权限字面量集合）。管理详情窗用继承链上的 `ResetPasswordCandidateVo`（角色/岗位列表 + 可选明文候选）。不要口述成同一个 VO。

**`candidate` ≠ `resetPwd`。** 一个打印，一个换锁。权限字符串碰巧都是 `system:user:resetPwd`，方法不是同一个。

**`issue` ≠ `resetPwd`。** 权限都不同。临时不改永久哈希。

### 机制/因果链

#### A. `SysUserController.*`：磁盘上的 17 个公开方法

文件：`SysUserController.java`。类注解：`@Validated` `@RequiredArgsConstructor` `@RestController` `@RequestMapping("/system/user")`。注入：`ISysUserService`、`ISysRoleService`、`ISysPostService`、`ISysDeptService`、`PasswordPolicyService`。

下面按源码出现顺序。**不要发明**未出现的 `@GetMapping("/resetPwd")` 或 `POST /unlock`。

| # | Java 方法 | HTTP | 权限 | 写什么 | 口试要点 |
| --- | --- | --- | --- | --- | --- |
| 1 | `list` | `GET /list` | `system:user:list` | 无 | `selectPageUserList` |
| 2 | `export` | `POST /export` | `system:user:export` + `@Log EXPORT` | 无（Excel 响应） | 查询 `selectUserExportList` 后 `ExcelBuilder.toResponse` |
| 3 | `importData` | `POST /importData`（multipart） | `system:user:import` + `@Log IMPORT` | 新用户：generate + `validateOrThrow` + BCrypt + `insertUser`；更新不改密 | 监听器 `SysUserImportListener` |
| 4 | `importTemplate` | `POST /importTemplate` | **方法上无** `@SaCheckPermission`、无 `@Log` | 无 | 空列表导出模板 |
| 5 | `getInfo()` | `GET /getInfo` | **无权限注解**（要登录） | 无 | 当前用户；用户缺失 → `R.fail("没有权限访问用户数据!")` |
| 6 | `getInfo(userId, clientId)` | `GET /` 与 `GET /{userId}` | `system:user:query` | 无；无 userId 时 **generate 填 Vo.password** | 有 userId 才 `checkUserDataScope`；`Cache-Control: no-store` **只在无 userId 分支** |
| 7 | `add` | `POST /` | `system:user:add` + `@Log INSERT`（排除 `password`）+ `@RepeatSubmit` | **写永久哈希** | 验部门范围、用户名/手机/邮箱唯一，**验请求体明文**，不 generate |
| 8 | `edit` | `PUT /` | `system:user:edit` + `@Log UPDATE`（排除 `password`）+ `@RepeatSubmit` | 资料/角色/岗位，**不走验密哈希** | `checkUserAllowed` + 双范围 + 唯一性 |
| 9 | `remove` | `DELETE /{userIds}` | `system:user:remove` + `@Log DELETE` | 删用户及关联 | 数组含当前登录者 → `R.fail("当前用户不能删除")`；服务里再挡超管与数据范围 |
| 10 | `optionselect` | `GET /optionselect` | `system:user:query` | 无 | 可选 `userIds` / `deptId` |
| 11 | `resetPwd` | `PUT /resetPwd` | `system:user:resetPwd` + `@ApiEncrypt` + `@Log UPDATE`（排除 `password`）+ `@RepeatSubmit` | **写永久哈希** | 闸允许/范围 → `validateOrThrow` → `BCrypt.hashpw` → `resetUserPwd` |
| 12 | `changeStatus` | `PUT /changeStatus` | `system:user:edit` + `@Log UPDATE` + `@RepeatSubmit` | 状态；停用会踢登录域 | 闸允许/范围 |
| 13 | `unlock` | **`GET`** `/unlock/{userId}` | `system:user:edit` + `@Log OTHER` + `@RepeatSubmit` | 删错次 Redis | 用户不存在 → `R.fail`；有键才 `deleteObject` |
| 14 | `authRole` | `GET /authRole/{userId}` | `system:user:query` | 无 | **必填** query `clientId`；超管目标才看见超管角色 |
| 15 | `insertAuthRole` | `PUT /authRole` | `system:user:edit` + `@Log GRANT` + `@RepeatSubmit` | 替换该 Client 显式角色 | `roleIds` 空 = 撤销该 Client 显式角色；`clientId` 空由服务抛「请选择客户端」 |
| 16 | `deptTree` | `GET /deptTree` | `system:user:list` | 无 | 用户页筛部门树，不是 `SysDeptController` 那棵管理树的全部职责 |
| 17 | `listByDept` | `GET /list/dept/{deptId}` | `system:user:list` | 无 | `@NotNull deptId` |

**两扇 `getInfo` 不要并成一扇：**

- L-014 / 身份口打的是 **第 5 行** `/getInfo`。它把会话里的菜单权限、角色权限抄进 `UserInfoVo`。查库时 **忽略数据权限**，避免「看不见自己」。
- 用户表单打的是 **第 6 行**。源码局部变量叫 `userInfoVo`，类型却是 `new ResetPasswordCandidateVo()`。有 `userId` 时填 `user` / 可选 `roleIds` / 岗位；无 `userId` 时 `setPassword(generateDefaultPassword())` 并 `no-store`。`clientId` 缺席则角色列表保持空（方法开头 `roleIds = List.of()`）。超管角色过滤：`LoginHelper.isSuperAdmin(userId)` 为真才把超管角色留给表单，否则 `StreamUtils.filter(..., r -> !r.isSuperAdmin())`。

**写永久密码的三条链（都在 classic 房间，不进 Redis 临时柜）：**

1. **`add`：** 请求体 `SysUserBo.password` **没有** `@NotBlank`。空串能过 Bean 校验，被 `validateOrThrow` 挡住。过了才哈希，`insertUser`（`@DSTransactional`：用户行 + 登录域 + 岗位 + 角色）。
2. **`resetPwd`：** **没有**方法级 `@Validated`。闸完直接取 `user.getPassword()` 再 `validateOrThrow`。写库是 `userMapper.lambda().set(SysUser::getPassword, hashed).eq(...).updateCount()`。
3. **导入新行：** 监听器自己 `generateDefaultPassword()`，再 `validateOrThrow`（调用方二次检查，FIXED 值若已不合规会在这里炸），再哈希插入。已存在且 `updateSupport`：改资料，**不**重新 generate。

`SysUser.password` 标注 `insertStrategy/updateStrategy = NOT_EMPTY`。`edit` 即使 Mapstruct 带了空密码字段，也不会把列更新成空。这不是「edit 会重置密码」的口子；永久换锁仍只有 resetPwd / add / 导入新用户。

**`unlock` 是 GET 变异。** 存量 classic 如此。它只处理 `pwd_err_cnt:` + 用户名。没有临时键、不调 `TemporaryPasswordStore`。登录失败计数的写入在 `SysLoginService`（L-013），本课认「这扇窗删那把计数键」。

**`importTemplate` 无权限注解**是磁盘事实，不是推荐 Target。API-005 要求变更用 POST + `@Log`；本方法是 POST 但无 `@Log`。口试报事实，不把缺口说成规范。

#### B. `SysUserCredentialController.candidate`：打印，不改人

文件：`SysUserCredentialController.java`。构造注入 `ISysUserService`、`PasswordPolicyService`。**不**继承 `BaseController`。

```text
POST /system/user/resetPwd/candidate
  @SaCheckPermission("system:user:resetPwd")
  @Log(title="用户密码重置候选", OTHER, isSaveRequestData=false, isSaveResponseData=false)
  body: ResetPasswordCandidateBo { @NotNull userId }
```

顺序（单测 `candidateChecksResetScopeReturnsNoStoreAndDoesNotMutateUser` 用 `inOrder` 钉死）：

1. `response.setHeader(CACHE_CONTROL, "no-store")`
2. `userService.checkUserAllowed(body.userId())`
3. `userService.checkUserDataScope(body.userId())`
4. `passwordPolicyService.generateDefaultPassword()`
5. `return R.ok(new ResetPasswordCandidateVo(password))`

`verifyNoMoreInteractions(userService)`：没有 `resetUserPwd`、没有 `selectUserById`、没有 `updateUser`。JSON 在只给全参构造 `new ResetPasswordCandidateVo("Candidate1!")` 时是 **`{"password":"Candidate1!"}`**（父类字段 null + `@JsonInclude(NON_NULL)`）。

前端 `users.passwordResetCandidate`：同一 URL，头带 `Cache-Control: no-store` 与 `repeatSubmit: false`，投影只保留 `password`。用户页 `UserCredentialDialogs.openReset` 打开对话框就打这一枪，把返回值填进两个输入框；点确定才 `users.resetPassword` → `PUT /resetPwd`。

#### C. `issue` + 一次性 Redis（矩阵 (b)）

文件：`SysTemporaryPasswordController.java`。注入 `ISysUserService`、`TemporaryPasswordService`、`TemporaryPasswordAuditEnricher`。

```text
POST /system/user/temporaryPassword
  @SaCheckPermission("system:user:temporaryPassword")   ← 独立权限，不是 resetPwd
  @Log(title="用户临时密码", OTHER, isSaveRequestData=false, isSaveResponseData=false)
  body: TemporaryPasswordIssueBo { @NotNull userId }
```

Controller 顺序（`issueChecksUserScopeAndReturnsOneMinuteNoStoreResponse`）：

1. `Cache-Control: no-store`
2. `auditEnricher.attachTarget(userId)`（请求属性记下目标，**不**把 body 明文留给审计）
3. `checkUserAllowed` / `checkUserDataScope`
4. `temporaryPasswordService.issue(userId)`
5. `R.ok(new TemporaryPasswordVo(password, expiresInSeconds))`

VO / `IssuedPassword` 的 `toString` 都是 `password=<redacted>`。审计 enrich 只写 `targetUserId` + `expiresInSeconds=60`，断言字符串不含 `password` / `hash`。

**`TemporaryPasswordService.issue`（(b) 写入合同）：**

1. `generateTemporaryPassword()`——**永远随机**。策略不可用抛 `ServiceException`；单测：抛错时**不覆盖**店里已有哈希，旧纸条仍能 `verify`。
2. `BCrypt.hashpw(password)` 后 `temporaryPasswordStore.store(userId, hash, Duration.ofSeconds(60))`。
3. 返回 `IssuedPassword(明文, 60)`。店里留下的字符串 **≠** 明文（`assertNotEquals`）。

**Redis 形状：**

| 项 | 磁盘值 |
| --- | --- |
| 实现 | `RedisTemporaryPasswordStore`，Redisson `StringCodec` |
| 键 | `auth:temporary-password:user:` + `userId`（静态 `key()` 公开给运维/测试） |
| 值 | **BCrypt 哈希**，不是明文 |
| TTL | `Duration.ofSeconds(60)`；集成测试：覆盖写入换新哈希并刷新 TTL |
| 消费 | Lua：`if get(key)==ARGV[1] then del else 0`。并发两呼只要一个返回 1 |

**一次性怎么闭合（消费不在本 Controller，但 (b) 必须能说）：**

`PasswordAuthStrategy.authenticate`（L-013 已认顺序，本课只接签发柜）：

1. 永久 `BCrypt.checkpw` 成功 → **从不** `verify` / `consume`。
2. 永久失败 → `verify`；没有匹配 → `loginFailed`（错次 +1，那是 `pwd_err_cnt:`）。
3. 先 `requireLoginAccess`（登录域）。失败 → **不** `consume`（单测钉死：准入失败不撕临时柜）。
4. 准入后再 `consume`（CAS）。输家算一次 `loginFailed`，没有 `loginSucceeded`。
5. 成功才清错次、写 Sa-Token 会话。

SSO 认人厅 `AdminSsoIdentityService.verifyPassword` 用同一对 `verify`/`consume`，没有 Client 准入夹在中间（SSO 另走 `assertClientAccess`）。不要把两条登录走廊说成同一段 Java，但 Redis 是同一只柜。

`verify` / `consume` 遇到 Redis `RuntimeException`：**失败关闭**（空 Optional / `false`），打 warn，不把临时纸条当成永久密码。

再签发：`store` 覆盖。集成测试：留下 `hash-two` 后，用 `hash-one` 做 `compareAndDelete` 失败，键仍在。过期后 `read` 为 null。

## 图、表或文本图

**图题 / caption：** 一块牌子三份源码。alt：`/system/user` 下 SysUserController 十七法、candidate、issue 分文件。

```text
                 @RequestMapping("/system/user")
        ┌────────────────────┼────────────────────┐
        v                    v                    v
 SysUserController   CredentialController   TemporaryPasswordController
 17 public methods    candidate()            issue()
        │                    │                    │
        │                    │                    │
        ├─ GET /getInfo      │                    │
        │    当前登录者       │                    │
        ├─ GET / 与 /{id}    │                    │
        │    表单；无 id 才   │                    │
        │    generateDefault  │                    │
        ├─ POST /  add       │                    │
        │    验明文→哈希→库   │                    │
        ├─ PUT /resetPwd     │                    │
        │    验明文→哈希→库   │                    │
        ├─ GET /unlock/{id}  │                    │
        │    删 pwd_err_cnt:  │                    │
        └─ 其余 CRUD/授权     │                    │
                             v                    v
                      generateDefault         generateTemporary
                      不改用户表              Redis 哈希 TTL 60s
                                              明文只回这一枪
```

**文字等价物：** 图顶是共享前缀 `/system/user`。左列是人事柜台全部公开窗，其中只有新增和永久重置把哈希写入用户表；无 userId 的详情 GET 只把默认密码打印进 Vo；解锁只擦登录错次键。中列是重置候选 POST，生成器与新增初始化相同，但不调用任何 update。右列是临时签发，生成器强制随机，结果进 Redis 而不是用户表。三列都可能先跑 `checkUserAllowed` / `checkUserDataScope`（当前用户 `GET /getInfo`、列表、模板除外）。

**图的边界：** 不画 `SysProfileController`。不画角色菜单树（L-016）。不把 `createSystemService.users.profile` 画进左列。不保证每个 App 都调用了全部 17 个方法；前端用户页用 download/upload 打 export/import，不一定经过 `users.*` 工厂方法。

**图题 / caption：** 临时密码一次烧完。alt：issue 写 BCrypt 到 Redis 60 秒；登录 CAS 删除；永久密码走另一条。

```text
管理员 POST /temporaryPassword
        │  明文 ──HTTP──► 浏览器（no-store，只展示一次）
        v
Redis  auth:temporary-password:user:42
        值 = BCrypt(明文)    TTL = 60s
        再签发 → 覆盖哈希并重置 TTL

登录 POST /auth/login  (PasswordAuthStrategy)
        │
        ├─ 永久哈希对上 ──────────────────────────────► 写会话，Redis 临时柜不动
        │
        └─ 永久不对
              verify(读哈希, checkpw)
                失败 → loginFailed（pwd_err_cnt: +1）
                成功 → 先 requireLoginAccess
                         失败 → 不 consume
                         成功 → Lua compareAndDelete
                                  赢家：loginSucceeded + 写会话，键消失
                                  输家：loginFailed，键可能已被别人删
```

**文字等价物：** 管理员签发时，浏览器拿到一分钟有效的明文；Redis 只记住烤过的哈希。用户用这张纸条登录：若其实永久密码也对，系统走永久分支，纸条原封不动留到过期。若只有纸条对，必须先通过该 Client 的登录域，再原子删键。删键失败（过期、被覆盖、别人抢先）算一次密码错误，不会发通行证。错次键是另一把名字，解锁窗擦的是它。

**图的边界：** 不展开图形验证码删键（L-012）。不把 SSO `verifyPassword` 画成第二条箭头也能自动带 Client 准入——SSO 文件是另一条走廊，柜相同。不把 TTL 说成可配置：源码常量 `EXPIRES_IN_SECONDS = 60`。

## 正例、反例与边界

**正例 1：** 数 17 个映射。打开 `SysUserController.java`，从 `list` 数到 `listByDept`。确认有 `add`（`@PostMapping` 无 path）和 `edit`（`@PutMapping` 无 path）。对照矩阵 (a) 那行少了这两个名字。

**正例 2：** 两扇 `getInfo`。`GET /getInfo` 返回 `R<UserInfoVo>`；`GET /` 与 `GET /{userId}` 返回 `R<SysUserInfoVo>` 形状，实现类型是 `ResetPasswordCandidateVo`。`PasswordCandidateContractUnitTest.addInitializationReturnsPolicyCandidateWithoutCaching`：`GET /system/user/` → `no-store` + `data.password`。

**正例 3：** `add` 不 generate。打开 `add`：`validateOrThrow(password)` 然后 `BCrypt.hashpw`。全文无 `generateDefaultPassword`。L-012 已点名，本课认格子。

**正例 4：** 候选不改用户。`PasswordCandidateContractUnitTest.candidateChecksResetScope...`：`inOrder` 只有 allowed → dataScope → generate；`verifyNoMoreInteractions(userService)`。

**正例 5：** 临时权限独立。`TemporaryPasswordControllerContractUnitTest.issueHasIndependentPermission...`：权限数组恰好 `system:user:temporaryPassword`；`@Log` 双 false。

**正例 6：** Redis 只存哈希、刚好一分钟。`TemporaryPasswordServiceUnitTest.issueStoresOnlyBcryptForExactlyOneMinute`：TTL `Duration.ofSeconds(60)`，`checkpw` 为真且明文 ≠ 存储值；`IssuedPassword.toString` 不含明文。

**正例 7：** 一次性 CAS。同文件 `wrongValueDoesNotConsumeWhileVerifiedValueUsesCompareAndDelete`：错值 `verify` 空、`compareAndDeleteCalls==0`；对值才 consume 并把存储清空。集成测试：两线程 `compareAndDelete` 成功数 = 1。

**正例 8：** 永久赢则不碰临时柜。`PasswordAuthStrategyTemporaryUnitTest.permanentPasswordWinsWithoutReadingOrConsumingTemporaryValue`。

**正例 9：** 准入失败不消费。同测试 `validTemporaryValueIsNotConsumedWhenClientAccessFails`。

**正例 10：** 前端对话框两枪分离。`UserCredentialDialogs.vue`：`openReset` → `passwordResetCandidate`；`submitReset` → `resetPassword`；`issueTemporary` → `issueTemporaryPassword`。提示文案写「仅展示一次…成功登录后立即作废」。

**正例 11：** 前端投影丢多余字段。`domains/system/src/index.test.ts`：「uses no-store POST…」candidate 响应里的 `ignored` 不会出现在 `data` 里。

**正例 12：** classic 房间。登记表 `03-backend-module-modes.md`：`wta-modules/wta-system` = classic。这三份类在 `controller/system`，服务在 `service/impl`，临时柜在包 `temporarypassword`，没有 UseCase 目录。

**反例 1：** 「`SysUserController` 没有 `add`/`edit`，因为矩阵没写。」磁盘有 `@PostMapping` / `@PutMapping` 两个无 path 方法。

**反例 2：** 「`GET /system/user/getInfo` 就是带 userId 的详情。」那是当前登录者。详情是 `GET /system/user/{userId}`。身份口只用前者。

**反例 3：** 「打开重置对话框就已经改密。」只打了 candidate。库仍是旧哈希。

**反例 4：** 「临时密码会写进 `sys_user.password`。」`issue` 不调 `resetUserPwd`。随后用旧永久密码仍能走永久分支（正例 8）。

**反例 5：** 「临时签发走 `generateDefaultPassword`，FIXED 配置会让临时密码变成固定值。」`generateTemporaryPassword` 永远 `generate(policy)`。合同测试标题就是 `shouldSupportCompliantFixedDefaultButKeepTemporaryPasswordsRandom`。

**反例 6：** 「解锁等于作废临时密码。」解锁删 `pwd_err_cnt:`。临时键还在，直到 TTL 或 consume 或再签发覆盖。

**反例 7：** 「`unlock` 是 POST，因为改了 Redis。」映射是 **`@GetMapping("/unlock/{userId}")`**。

**反例 8：** 「candidate 权限是 `system:user:query`。」是 **`system:user:resetPwd`**。临时才是独立的 `temporaryPassword`。

**反例 9：** 「三份 Controller 在 layered 的 UseCase 后面。」`wta-system` 是 classic。`issue` 直接打 `TemporaryPasswordService`。

**反例 10：** 「`users.profile` / `updatePassword` 属于本课 `SysUserController.*`。」URL 是 `/system/user/profile`、`/system/user/profile/updatePwd`，类是 `SysProfileController`（L-022）。

**反例 11：** 「导入更新也会重置密码。」监听器更新分支只 `updateUser`，不 generate。

**反例 12：** 「`GET /{userId}` 也会 `no-store` 并带候选密码。」`no-store` 与 `setPassword(generate…)` 在 `userId == null` 的 else 分支。有 id 时走查库，不 generate。

**反例 13：** 「临时消费在 Controller 里 `deleteObject`。」Controller 只 `issue`。删除在 `RedisTemporaryPasswordStore.compareAndDelete` 的 Lua，由登录策略调用。

**反例 14：** 「`ResetPasswordCandidateVo` 不能当详情 Vo。」它 **extends** `SysUserInfoVo`。详情方法就是 new 了这个子类。不要发明一个「源码里不存在的独立详情方法返回类型」。

**边界：**

- `resetPwd` 方法参数没有 `@Validated`；空密码靠 `validateOrThrow`。`add` 有 `@Validated`，但 `password` 字段本身无 `@NotBlank`。
- `insertAuthRole` 的 `roleIds` 注释写明空表示撤销当前客户端显式角色；`clientId` 空抛「请选择客户端」。
- `remove` 先挡自杀，服务再挡超管。超管常量是 `1761100000000000001L`，不是 `1`。
- `importTemplate` 无权限注解：只要过登录拦截就能打。不要把「无注解」说成「匿名 `@SaIgnore`」——类上没有 `@SaIgnore`。
- 前端 `users` 工厂**没有** `export` / `importData` / `importTemplate` 方法。`UserPage.vue` 用 `requestDownload('system/user/export'|`importTemplate`)` 和上传 URL `.../importData`。HTTP 仍打到本课这三扇窗。
- `SysUserVo.password` 带 `@JsonIgnore` + `@JsonProperty`；详情里的用户哈希不靠这个字段出站。候选明文走的是子类自己的 `password` 字段。
- 再签发覆盖旧临时值：旧纸条立刻作废，不必等 60 秒。
- 策略生成失败：issue 抛错，旧哈希仍可 verify（单测 `policyFailureBlocksIssueButExistingHashStillVerifies`）。

## 变式与迁移

- **变式 A：只要「帮用户进一次门」，不要换永久锁。** 走 `issue` + 独立权限。不要复用 `resetPwd`。前端应展示 TTL，并假设响应不可缓存。

- **变式 B：管理员要可编辑的新永久密码。** 先 `candidate`（或新建表单的无 id `GET /`）拿到 generate 值，允许改，再 `resetPwd` / `add`。不要把 generate 的结果直接当已经落库。

- **变式 C：FIXED 默认密码。** 只影响 `generateDefaultPassword`（候选、导入新用户、无 id 详情）。临时路径仍随机。改配置后旧临时哈希不会自动作废，直到 TTL/消费/再签发。

- **变式 D：新窗口不要塞进 `SysUserController`。** 候选和临时已经分文件、同门牌。继续加凭据窗时优先新类 + 明确权限，而不是再往 17 方法文件堆重载。

- **变式 E：解锁与临时作废是两把扫帚。** 产品若要「解锁同时作废临时纸条」，今天**没有**这行代码。需要另调 store 删除。不要把 `unlock` 改口成已经删临时键。

- **变式 F：登录域挡住临时登录。** 纸条对、Client 无登录域 → 键还在，用户可以换有权限的 Client 再试（在 TTL 内）。这是故意顺序，不是 bug 口述。

- **变式 G：前端工厂漏了导入导出。** 补 `users.export` 之类是 L-019 的映射课。本课 HTTP 已经存在；不要为了「工厂没有」否认 Controller 映射。

- **迁移口诀：** 数三份类 → 分清两扇 `getInfo` → 写永久密只有 add/resetPwd/导入新行 → 候选打印、临时 Redis → 键名带 `userId` 且只存哈希 → 一次性是登录 CAS，不是签发时删除 → 解锁是错次键。跳步会出现「以为重置了其实只打印」「把临时写进用户表」「用 unlock 当临时作废」。

## 常见误区

1. **「`SysUserController.*` 就是矩阵那 14 个名字。」** 磁盘 17 个 Java 方法。补上 `add`/`edit`。
2. **「`getInfo` 只有一个。」** `/getInfo` 对当前登录者；`/` 与 `/{userId}` 对表单。返回类型都不同。
3. **「新增用户会调用 `generateDefaultPassword`。」** `add` 验请求体。generate 在无 id 详情、candidate、导入新行。
4. **「临时密码是 60 秒的永久重置。」** 用户表哈希不变。
5. **「FIXED 默认密码也会当临时密码。」** 临时永远随机。
6. **「candidate 会 `updateUser`。」** 零写库。
7. **「三份类要按 layered 加 UseCase。」** 登记表 classic；本课不发动重构。
8. **「`unlock` 与临时键、错次键是一把。」** 只有 `pwd_err_cnt:`。
9. **「资料页改密是 `resetPwd`。」** 那是 `SysProfileController.updatePwd`（L-022），还要旧密码。
10. **「消费在 Controller。」** 消费在 `PasswordAuthStrategy` / SSO identity。Controller 只 issue。
11. **「Redis 存明文方便对照。」** 存 BCrypt。Lua 比对的是**哈希字符串**相等，不是再算一次 BCrypt。
12. **「`importTemplate` 有 `system:user:import`。」** 方法上没有这条注解。
13. **「超管 userId 是 1。」** 常量是 `1761100000000000001L`。
14. **「前端 `users.get()` 就是身份口 `getInfo`。」** `users.get` 打 `/system/user/` 或 `/{id}`；身份口是 `identity.loadInfo` → `/system/user/getInfo`。

## 非评分暂停

打开磁盘，不要凭记忆默写路径。不要改文件。没有标准答案栏。

1. 打开 `SysUserController.java`。用手指点 17 个映射方法。把 `add`、`edit` 圈出来。把两个 `getInfo` 分成「当前用户」和「表单」。
2. 打开 `SysUserCredentialController.java` 与 `SysTemporaryPasswordController.java`。确认类上前缀仍是 `/system/user`，方法分别是 `POST /resetPwd/candidate`、`POST /temporaryPassword`。圈出两个权限字符串，确认它们不一样。
3. 在 `candidate` / `issue` / 无 userId 的 `getInfo` 三处圈 `Cache-Control: no-store`。在 `add` / `resetPwd` 圈 `validateOrThrow` 与 `BCrypt.hashpw`。确认 `issue` 没有这两行写库。
4. 打开 `RedisTemporaryPasswordStore.java`。抄下 `KEY_PREFIX` 和 Lua。打开 `TemporaryPasswordService.issue`，确认 TTL 是 `Duration.ofSeconds(60)`，存的是 `BCrypt.hashpw` 的结果。
5. 打开 `PasswordAuthStrategy.authenticate`（只要看临时四行：永久 checkpw → verify → 准入 → consume）。再打开 `SysUserController.unlock`，确认它拼的是 `PWD_ERR_CNT_KEY`，不是 `auth:temporary-password`。
6. 打开 `UserCredentialDialogs.vue` 的 `openReset` / `submitReset` / `issueTemporary`，对照 `createSystemService` 的 `passwordResetCandidate`、`resetPassword`、`issueTemporaryPassword` 三条 URL。

## 总结、词汇表与下一步

- **一块 `/system/user` 牌子，三份 classic 源码。** 口试：`SysUserController` 17 法（含矩阵漏掉的 `add`/`edit` 和两扇 `getInfo`）+ `candidate` + `issue`。
- **写永久锁：** `add` / `resetPwd` / 导入新用户。都先 `validateOrThrow` 再 BCrypt。候选和临时**不**走这条写库链。
- **候选：** `POST /resetPwd/candidate`（以及无 id 的 `GET /`）只 `generateDefaultPassword`，`no-store`，不 mutate。
- **临时 (b)：** `issue` → 随机明文回响应、哈希进 Redis 60 秒、键按 userId、再签发覆盖、登录 CAS 删除、失败关闭。权限 `system:user:temporaryPassword`。
- **解锁不是临时作废。** `pwd_err_cnt:` 对用户名。

词汇表：`SysUserController` / `SysUserCredentialController` / `SysTemporaryPasswordController` / `UserInfoVo` / `SysUserInfoVo` / `ResetPasswordCandidateVo` / `TemporaryPasswordVo` / `generateDefaultPassword` / `generateTemporaryPassword` / `checkUserAllowed` / `checkUserDataScope` / `TemporaryPasswordStore` / `compareAndDelete` / `pwd_err_cnt:` / `auth:temporary-password:user:` / classic。

下一步：L-016 把角色菜单权限拼成 RBAC。L-017 部门岗位登录域。L-018 Client / SsoApp。L-019 才把 `createSystemService.users.*` 整表对着 HTTP 念完。L-022 才是资料页改自己的密。本课结束仍不是掌握证明；要练习请之后主动激活 Homework。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-004 | `backend/wta-modules/**` Controller | 业务模块公开入口；本课三份类在 `wta-system` | `controller/system/SysUser*.java` | 2026-09-16 |
| S-008 | `.agents/skills/engineering-standards/references/project/03-backend-module-modes.md` | `wta-system` = classic；不按 layered 口述 | 登记表 classic 行 | 2026-09-16 |
| S-L015-01 | `SysUserController.java` | 17 个公开映射；两扇 `getInfo`；`add`/`resetPwd` 写哈希；`edit` 不验密；`unlock` 为 GET 且删 `pwd_err_cnt:`；`importTemplate` 无权限注解 | 类上 `/system/user` 及各方法注解 | 2026-09-16 |
| S-L015-02 | `SysUserCredentialController.java`；`ResetPasswordCandidateBo`/`Vo` | 唯一方法 `candidate`；权限 `resetPwd`；不 mutate；`no-store` | `POST /resetPwd/candidate` | 2026-09-16 |
| S-L015-03 | `SysTemporaryPasswordController.java`；`TemporaryPasswordIssueBo`/`TemporaryPasswordVo`；`TemporaryPasswordAuditEnricher` | 唯一方法 `issue`；独立权限；审计不存明文 | `POST /temporaryPassword`；`AUDIT_TITLE` | 2026-09-16 |
| S-L015-04 | `TemporaryPasswordService.java`；`RedisTemporaryPasswordStore.java`；`TemporaryPasswordStore.java` | 60s TTL；只存 BCrypt；Lua CAS；再签发覆盖 | `issue`/`verify`/`consume`；`KEY_PREFIX` | 2026-09-16 |
| S-L015-05 | `PasswordCandidateContractUnitTest`；`TemporaryPasswordControllerContractUnitTest`；`TemporaryPasswordServiceUnitTest`；`TemporaryPasswordRedisIntegrationTest`；`TemporaryPasswordAuditUnitTest`；`PasswordAuthStrategyTemporaryUnitTest` | 候选不改人；issue 顺序与 no-store；哈希+60s；CAS 单赢家；审计无 password；永久赢不读临时；准入失败不消费 | 各 `@Test` 方法名 | 2026-09-16 |
| S-L015-06 | `PasswordPolicyService.generateDefaultPassword` / `generateTemporaryPassword`；`SysUserImportListener`；`SysUser.java` 密码 `FieldStrategy.NOT_EMPTY`；`SysUserServiceImpl.checkUserAllowed`/`resetUserPwd`/`insertUserAuth` | generate 分流；导入新行才 generate；edit 空密码不覆盖；超管常量闸；空 roleIds 撤销该 Client 角色 | 对应方法 | 2026-09-16 |
| S-L015-07 | `PasswordAuthStrategy.authenticate`；`AdminSsoIdentityService.verifyPassword`；`CacheNames.PWD_ERR_CNT_KEY` | 一次性闭合在登录走廊；SSO 共用柜；错次键另一把 | 永久 checkpw 之后的临时分支 | 2026-09-16 |
| S-L015-08 | `frontend/packages/domains/system/src/service.ts`、`transport.ts`、`index.test.ts`；`web-domains/system/src/user/UserCredentialDialogs.vue`、`UserPage.vue` | 前端 URL 与 no-store；对话框 candidate≠reset；import/export 走页面 download 不是 `users.*` | `passwordResetCandidate` / `issueTemporaryPassword` / `resetPassword` | 2026-09-16 |
