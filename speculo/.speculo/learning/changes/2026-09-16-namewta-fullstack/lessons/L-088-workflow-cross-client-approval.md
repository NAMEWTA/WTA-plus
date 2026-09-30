---
lesson_id: L-088
objective_ids: [OBJ-18, OBJ-68, OBJ-70, OBJ-73]
estimated_minutes: 36
time_budget:
  - segment: orientation-and-map
    minutes: 5
  - segment: three-doors
    minutes: 8
  - segment: design-versus-runtime
    minutes: 9
  - segment: worked-example-and-boundaries
    minutes: 7
  - segment: pause
    minutes: 2
  - segment: recap-and-transfer
    minutes: 5
expression_level: eli5
coverage_depth: deep
source_ids: [S-010, S-016, S-L088-01, S-L088-02, S-L088-03, S-L088-04, S-L088-05, S-L088-06, S-L088-07, S-L088-08, S-L088-09, S-L088-10, S-L088-11, S-L088-12]
---

# Lesson 088：跨 Client 审核不是节点上的一列

## 学完你能做什么

有人问：能不能配一条流程，让 **A Client 的用户提交，B Client 的某个用户初审，C Client 的某个用户终审，管理端 Client 最终审核**。

学完这一课，你能把这句话拆成引擎真正保存的东西，并拒绝两种听起来很像、其实都不成立的说法：

1. 「设计器里有一个 Client 下拉框，每一级选一个端。」当前 `wta-workflow` 没有这种字段。
2. 「选了某个 Client 的角色，运行时就只允许这个人用那个 Client 的登录会话来点通过。」运行时待办认的是**用户 ID**，不认这次登录的 Client。

你还能指出，要让这件事「看起来像跨端」，必须同时打开三扇互不替代的门：登录域、客户端访问路径、办理人用户 ID。少一扇，流程定义画得再完整也走不到那一端。

本课是 L-018（Client）、L-068（流程定义）、L-070（任务办理）、L-073（管理端流程页面）的补充。它不替换那四课的方法口试，也不进入原来的 85 课目标链。证据是 2026-09-28 的工作树和 Warm-Flow 1.8.9 源码包，不是一次点过设计器的浏览器验收。

## 先把宏观地图放在桌上

NAMEWTA 里的 Client 是 `sys_client` 的一行：一套登录入口、一个登录域、一批属于这一端的角色和菜单、以及这张登录票允许打哪些接口。人是 `sys_user` 的一行。同一个人可以拥有多个登录域，于是可以分别登录管理端和用户端。流程不认识「端」，只认识「这一步由哪些人来办」。

```text
人 sys_user（没有 client_id）
  |  拥有登录域 sys_user_type_rel
  v
登录某个 sys_client
  |  会话里记下 clientPk / clientKey / access_path
  |  角色和菜单只加载这一端的
  v
业务提交（这一端的 access_path 必须放行那个业务接口）
  |
  v
一条 flow_instance（create_by = 用户 ID，没有 client 列）
  |
  +-- 节点初审  flow_node.permission_flag = 用户/角色/部门/岗位/SpEL
  +-- 节点终审  同上
  +-- 节点终审  同上
  |
  v
生成 flow_task 时，角色/部门/岗位被展开成用户 ID
  |
  v
flow_user.processed_by = 用户 ID
  |
  v
办理时比较：当前登录用户 ID 是否在这张待办的 processed_by 里
         不比较：这次 token 属于哪个 Client
```

**图题 / caption：** 人、登录端、流程待办是三层。alt：用户表没有客户端列；登录会话才带客户端；流程待办最后只保存用户 ID。

**文字等价物：** 上面从上到下是一条因果链，不是三套可以互相代替的配置。第一层，用户表本身不归属某个 Client。第二层，一次登录把人和某一个 Client 绑在这张 token 上，并限制这张 token 能访问的路径。第三层，流程定义只保存办理人存储标识；任务真正生成时，这些标识被收成用户 ID 写进 `flow_user`。办理校验拿当前登录用户 ID 去对这列，不去对 Client。所以「B Client 的用户」如果被理解成「持有 B 端登录域、并且被选进节点的那个人」，可以近似配出来；如果被理解成「必须坐在 B 端的会话里才能按通过」，引擎没有这一列。

**类比：** 学校有几扇门（Client），学生证是同一个人（用户 ID）。审批单上写的是学号，不写「必须从东门进来」。门卫可以另有规定：西门的通行证进不了教务处。那是门卫的规则，不是审批单上的一栏。类比失效处：门卫规则在本系统里是 token 的 `access_path` 和菜单，不是校园地理；同一个人可以同时持有几扇门的通行证。

## 核心概念与机制

### 直觉讲解

把流程想成一张接力表。每一格可以写人名、部门、岗位，或者「我现在登录的这一端里的某个角色」。表上没有「这一格归哪个 App」的格子。

接力跑起来之后，裁判不再看你当初写的「角色」，而是看已经抄到待办条上的学号。你从哪扇门进来，裁判不看。门卫看。门卫和裁判不是同一个人。

### 精确定义与 English term

| 中文 | English term | 在本仓库里是什么 | 不是什么 |
| --- | --- | --- | --- |
| 客户端 | Client | `sys_client` 一行。种子里管理端 `client_key` 是 `pc`，主键 `1762000000000000001`；用户端更新后是 `home`；另有 `sso`。OAuth 的 `client_id` 是另一列字符串 | 不是流程节点属性，也不是 `sys_user` 的外键 |
| 登录域 | user type | `sys_user_type`。Client 通过 `user_type_id` 要求一种登录域；用户靠 `sys_user_type_rel` 持有它 | 不是办理人类型 |
| 办理人存储标识 | permission flag / storage id | `flow_node.permission_flag`。多个用 `@@` 隔开。用户是纯数字 ID，角色是 `role:{id}`，部门 `dept:{id}`，岗位 `post:{id}`，SpEL 以 `$` 或 `#` 开头 | 不是 `client:{id}`。本模块枚举里没有 Client |
| 待办权限人 | processed by | `flow_user.processed_by`。任务生成后，这里应是展开后的用户 ID | 不是「当前 Client」 |
| 当前办理身份 | handler permissions | `WorkflowPermissionHandler.permissions()` 与 `getHandler()` 都返回 `LoginHelper.getUserIdStr()` | 不返回角色 ID，不返回 `clientPk` |
| 访问路径 | access path | token 扩展里的路径白名单。空白名单表示不限制；非空则请求路径必须命中，否则抛出「当前客户端未授权访问该接口路径」 | 不是流程条件表达式 |

`TaskAssigneeEnum` 只有五档：用户、角色、部门、岗位、SpEL 表达式。`wta-workflow` 的 Java 源码里搜不到 `clientId`、`client_id` 或 `Client`。流程表 `flow_definition`、`flow_node`、`flow_task`、`flow_user`、`flow_instance` 有 Warm-Flow 自带的 `tenant_id`，没有 `client_id`。本模块没有任何 Java 去读写 `tenant_id`，`WarmFlowConfig` 也是空配置类。不能把租户列当成 Client 开关。

### 机制/因果链

分三段看：设计时谁进候选名单，运行时名单怎么变成待办，登录端怎么决定你碰不碰得到接口。

**设计时。** 设计器的办理人页签来自 `FlwTaskAssigneeServiceImpl.getHandlerType()`，也就是上面那五档。

- 角色页签走 `SysTaskAssigneeServiceImpl.selectRolesByTaskAssigneeList`。它把 `LoginHelper.getLoginUser().getClientPk()` 写进查询条件。角色查询再要求这个 Client 存在。所以你在管理端画流程时，角色名单只有管理端的角色。换端登录，名单就换成那一端的角色。它不会同时列出 A、B、C 和管理端。
- 用户页签不按 Client 过滤。`sys_user` 没有 `client_id`。名单仍受 `SysUserMapper.selectPageUserList` 上的部门数据权限约束，所以你看到的是「当前数据权限里的人」，不是「B Client 的人」。
- 部门、岗位同样没有 `client_id`。把部门名叫成「B 端」只是命名习惯，引擎不会因此检查登录端。
- 给用户授某个 Client 的角色时，`validateUsersHaveRoleClientType` 会先要求这个人已经拥有该 Client 的登录域。这是角色分配的门，不是流程节点的门。

**运行时。** 节点上的 `permission_flag` 按 `@@` 拆开。Warm-Flow `ExpressionUtil.evalVariable` 先替换 SpEL 和变量，再调用 `PermissionHandler.convertPermissions`。NAMEWTA 的实现把 `role:`、`dept:`、`post:` 和裸用户 ID 收成 `UserDTO`，只返回用户 ID 字符串。角色展开用的是 `UserService.selectUsersByRoleIds`：按 `sys_user_role` 找人，**不再看**这个角色的 `client_id`，也**不看**办理人此刻登录的是哪一端。

展开发生在**生成下一待办**的时候，不是每次打开待办时重算。随后 `flow_user.processed_by` 记下这些用户 ID。之后有人被加入或移出该角色，已经生成的待办不会自动跟着变。

申请节点有一个额外覆盖：`WorkflowGlobalListener.assignment` 发现下一节点是申请节点时，把办理人改成 `instance.createBy`，也就是发起人的用户 ID。

办理时 `CompleteExecuteComponent` 没有自己填 permission flag。Warm-Flow 发现它是空的，就调用 `permissions()`，得到当前登录用户 ID。`TaskServiceImpl.checkAuth` 看这个 ID 是否出现在该任务的 `processed_by` 里。没有交集就拒绝。比较式里没有 Client。

待办列表 `pageByTaskWait` 同样只把 `LoginHelper.getUserIdStr()` 传给 `FlwTaskMapper.getListRunTask`，条件是 `flow_user.processed_by = 用户 ID`，外加待办类型 `1/2/3`。SQL 里没有 `client_id`。

**登录端这扇门。** 就算用户 ID 对得上，请求还要先过 `SecurityConfig.validateClientAccessRules`。token 里的 `access_path` 非空，而当前路径不在名单里，请求在进 Controller 之前就失败。种子里：

- 管理端 `pc` 的 `access_path` 保持空，空白名单等于不限制路径。
- `home` 被更新成 `/home/**,/system/user/getInfo,/system/menu/getRouters,/auth/logout,/profile/**`。`ClientAccessPaths` 只给 `home` 补身份和档案接口，**不补** `/workflow/**`。
- `sso` 那一行的 `access_path` 也是空，但它是 SSO 中心 Client，不是业务审核端。

工作流菜单在 `30-cde-workflow.sql` 里以 `client_id = NULL` 插入，随后 `50-cde-base-dml.sql` 把仍为空的菜单和角色回填到管理端主键 `1762000000000000001`。前端只有 `admin-web` 注册了 `createWorkflowWebDomain`。`home-web` 源码里没有 workflow 组装。

因此：库里的待办可以属于某个用户 ID；这个人若只拿着 home 的 token，连 `/workflow/task/pageByTaskWait` 和 `/workflow/task/completeTask` 都打不进去。`pageByTaskWait` 和 `completeTask` 本身没有 `@SaCheckPermission`，挡它们的是登录和 `access_path`，不是菜单权限字符串。菜单决定管理端页面出不出现。

`startWorkFlow` / `completeTask` 也没有菜单权限注解。另一条路是服务端：业务模块调用 `WorkflowService`，在一次 home 能访问的业务请求里由服务器启动流程。发起人仍然是用户 ID。这能让「home 用户提交」成立，但不能让「home 会话自己打开待办页并办理」成立。

### 图、表或文本图

四段审核如果硬画成引擎里的样子，是下面这张表，不是四列 Client。

| 你想说的话 | 节点上实际能存的 | 运行时待办变成 | 这一端的会话还要另过 |
| --- | --- | --- | --- |
| A Client 用户提交 | 申请节点会被改成发起人用户 ID | `processed_by = 发起人` | A 的登录域；提交接口在 A 的 `access_path` 里，或由服务端在一次合法业务请求中启动 |
| B Client 某个用户初审 | 一个用户 ID，或管理端设计时看得到的角色/部门/岗位 | 展开后的用户 ID | 办理所用的那张 token 必须放行 `/workflow/task/**` |
| C Client 某个用户终审 | 同上，另一格节点 | 另一批用户 ID | 同上，换的是人，不是换端校验 |
| 管理端最终审核 | 再一格节点，指定管理端用户或管理端角色 | 用户 ID | 种子里的 `pc` 路径不限制，且页面在 `admin-web` |

**图题 / caption：** 四级话术各自落到哪一列。alt：四行业务话术，右边三列分别是节点存储、待办用户 ID、会话路径，没有 Client 匹配列。

**文字等价物：** 每一级都可以是流程里的一个中间节点，这是多级审批，L-068 / L-070 已经覆盖。跨端要求额外落在第三列：办理那一下所用的登录票必须被允许访问工作流接口。引擎不会因为节点名叫「B 端初审」就去读 token 的 `clientKey`。管理端最终审核在种子环境里最容易走通，因为 `pc` 的访问路径为空，而且只有 `admin-web` 带了流程页面。B 和 C 若是新建 Client，要自己准备登录域、角色、访问路径和页面；设计器不会替你生成。

**图的边界：** 这张表描述的是当前源码和初始化脚本。已经运行过的库如果后来手工把工作流菜单复制到了别的 Client，页面可见性会不同，但 `flow_*` 表仍然没有 Client 列，办理比较仍然只看用户 ID。本课没有连上那台库去核对有没有手工数据。

### 正例、反例与边界

**正例：多级指定具体的人，审核人用管理端会话办。**

在管理端设计器的用户页签里，初审节点选用户甲，终审节点选用户乙，最后节点选用户丙。三个人都拥有管理端登录域。甲在自己能访问的业务接口里提交，服务端记下甲的用户 ID。乙登录 `pc`（管理端），待办查询按乙的用户 ID 命中，办理时 `permissions()` 也是乙的用户 ID，`checkAuth` 通过。丙同理。

这条链路是「三个不同的人依次办」，不是「三个 Client 依次办」。甲即使也有 home 登录域，他的 home token 仍然打不开工作流待办接口。

**正例的近似：用角色表达「这个端里的一批人」，但只能在该端的设计会话里选到这个角色。**

角色 ID 本身属于一个 Client。运行时 `selectUsersByRoleIds` 会把「拥有这个角色的用户」收成用户 ID。若节点里真的写下了 `role:{B端角色ID}`，待办会落到这些用户身上，之后仍不检查他们是不是用 B 的 token 来点的。

设计器不会在管理端会话里把 B 端角色列出来。本模块也没有一段代码在保存流程定义时校验 `permission_flag` 里的角色是否属于当前 Client。所以「管理端画布上看不到」不等于「数据库列拒绝外端角色 ID」。本课没有把手工改库或导入 JSON 当成支持的配置步骤；只说明候选名单和保存校验不是同一道门。

**反例：希望节点属性等于 Client。**

不存在 `client:1762...` 这种办理人类型。SpEL 组件 `SpelRuleComponent` 只有 `selectDeptLeaderById`，按发起人部门找负责人，入参不是 Client。节点扩展 `VariablesEnum` 和 `CopySettingEnum` 都是空枚举；未知扩展码只打日志，不产生 Client 条件。抄送展开同样走 `convertPermissions`，收成用户 ID。

**反例：同一个人用「错误的端」就被引擎拒绝。**

用户甲同时拥有管理端登录域和 home 登录域。种子还给超级管理员用户 `1761100000000000001` 补了一条用户端登录域。甲被指定为初审人。他用管理端 token 可以办（路径不限制，用户 ID 匹配）。他用 home token 会被 `access_path` 挡住，这是门卫，不是节点配置。引擎没有「初审必须来自 B」的失败分支。

超级管理员绕过的是 `checkTaskReadAccess` 这类读任务判断（还有 `workflow:task:list` / `workflow:task:edit`）。办理仍走 `checkAuth`，仍然比用户 ID。超管身份不等于「任意 Client 都能调工作流接口」。

**边界：办理人校验可以被请求变量关掉。**

`CompleteExecuteComponent` 从办理请求的变量里读取 `ignore`。为真时 Warm-Flow `checkAuth` 直接返回。`WorkflowService.completeTask(Long, String)` 自己也会放入 `ignore=true`，注释写明这是给「系统后台发起、没有用户信息」用的。所以「必须是节点上的那个人」不是无条件成立。这不是 Client 配置项。本课不给出构造这种请求的步骤。

**边界：历史里的办理人字符串可以和登录用户不一致。**

`CompleteTaskBo.handler` 的注释是「可不填，用于覆盖当前节点办理人」。请求体里若带了这个字段，`FlowParams` 就不再回落到 `getHandler()`。放行与否仍看 permission flag（默认是登录用户 ID），除非 `ignore`。历史记录里的办理人因此不一定是「哪个 Client」。

**边界：`permission_flag` 只有 `varchar(200)`。** 本仓库的用户主键是 19 位雪花 ID，多个之间还要加 `@@`。一格节点写得下的显式用户很少。角色 ID 更短，但仍是「一批人」，不是「一个端」。

**边界：本课没有启动应用，也没有在设计器里点选。** 上面的因果来自控制器、服务、Mapper、初始化 SQL 和 Warm-Flow 1.8.9 源码。若运行中的库改过菜单归属或 `access_path`，页面和路径白名单以那台库为准；用户 ID 比较这一条不以那台库为准，因为它在 Java 里。

## 变式与迁移

把「Client」换成别的分组时，先问分组键有没有进 `permission_flag` 或 `checkAuth`。

- 换成部门：可以。部门是办理人类型，展开成该部门用户。它仍然不限制登录端。数据权限只影响设计时你看不看得到这个部门或用户。
- 换成岗位：同上。
- 换成「必须是某个 SSO 应用的用户」：SSO Client 只决定能不能登录、token 路径和会话。`sso` 种子行的访问路径是空的，也不自带 `admin-web` 的流程页。不要把 SSO 回调理解成审核节点。
- 换成多租户：表上有 `tenant_id`，本模块没有使用它。没有租户开关就不会自动变成「一端一个流程」。
- 若以后真要「只许 B 端会话办理」，那是新的校验：在 `permissions()` 或 `checkAuth` 之前比较 token 的 `clientPk` 与节点上新存的 Client。当前代码没有这个比较。补它之前，只改设计器文案不会生效。那是另一项实现，不是本课能配出来的。

迁移到别的业务单（请假、资料审核）时：看启动是浏览器直接打 `/workflow/task/startWorkFlow`，还是业务服务在服务端调用 `WorkflowService`。前者要算进该 Client 的 `access_path`。后者只要业务接口本身被放行。两种启动都不会把 Client 写进 `flow_instance`。

## 常见误区

- 「角色属于 Client，所以选角色就等于指定这一端来审。」角色归属只影响设计时的候选名单，以及授角色时用户必须有该端登录域。待办生成后比较的是用户 ID。
- 「管理端能看见所有 Client 的角色。」角色页签把 `clientPk` 设为当前登录 Client。管理端会话看不到 B 端角色。
- 「用户页签列出的就是某一端的用户。」用户表没有 Client 列。你看到的是数据权限范围内的人。
- 「home 用户登录了，就该在 home 里看到待办。」home 的访问路径不含工作流接口，`home-web` 也没有流程页面。待办行可以属于他的用户 ID，这个会话进不去。
- 「`tenant_id` 或菜单上的 `client_id` 就是流程的端。」菜单 `client_id` 决定这个菜单挂在哪一端。流程表的 `tenant_id` 在本模块未被使用。两者都不会参与 `checkAuth`。
- 「超级管理员用任意端都能审。」超管用户 ID 仍要过 `access_path`。home 白名单不放行 `/workflow/**`。读任务的超管短路也不等于办理短路。
- 「`ignore` 是设计器上的跨端开关。」它关掉的是办理人校验，而且不是节点模型里的 Client 字段。

## 非评分暂停

先别看总结。用自己的话走一遍，不要求交给谁：

- 四段话（A 提交、B 初审、C 终审、管理端终审）里，哪一段能写进 `flow_node`，哪一段只能写在 token 和菜单上。
- 同一个人分别拿管理端 token 和 home token，待办查询的 SQL 条件有什么不同，路径检查又有什么不同。
- 角色成员在待办生成之后才变动，已经存在的 `flow_user` 会不会跟着变。

若你把「选了 B 端角色」说成「必须用 B 端登录才能通过」，回到「运行时」那一段，指出比较的是哪两个值。

## 总结、词汇表与下一步

当前 `wta-workflow` **不能**把「A 端提交、B 端初审、C 端终审、管理端终审」配置成节点上的 Client 约束。它**能**配置多级办理人：用户、当前设计会话所属 Client 的角色、部门、岗位，或现有的部门负责人 SpEL。这些标识在生成待办时收成用户 ID。办理和待办列表只拿用户 ID 比较。

Client 仍有三道独立的门。登录域决定这个人能不能登录那一端。`access_path` 决定这张 token 能不能打到工作流接口。菜单和 `admin-web` 决定有没有现成页面。种子里工作流菜单归管理端，`home` 的路径白名单排除了工作流接口，流程页面只装在 `admin-web`。所以跨端审核不是打开一个开关；缺的那一扇门不会由流程定义补上。

下一步若要口试单点，回到 L-018 看 Client 与登录域，L-068 看定义如何保存办理人，L-070 看办理链，L-073 看页面只挂在管理端。本课不布置作业，也不表示这些目标已经掌握。

| 词 | 指什么 |
| --- | --- |
| Client / `sys_client` | 登录端。管理端种子 key 是 `pc`，不是字面量 `admin` |
| `clientPk` | 会话里的 `sys_client.id`。设计器用它过滤角色 |
| `access_path` | 这张 token 的接口白名单。空表示不限制 |
| permission flag | 节点上的办理人存储串，`@@` 分隔 |
| `processed_by` | 待办上展开后的用户 ID |
| `permissions()` | 当前登录用户 ID 的单元素列表 |

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-010 | 初始化 SQL | 管理端/home 的 `access_path`；菜单 `client_id` 回填；工作流菜单原先为空 | `release-artifacts/docker/infrastructure/mysql/init/50-cde-base-dml.sql`、`30-cde-workflow.sql` | 2026-09-28 |
| S-016 | 学习者偏好 | 中文、eli5；本课深度用 deep | `{roots.state}/learning/learner-profile.md` | 2026-09-16 |
| S-L088-01 | 工作树 | 办理身份只有用户 ID | `WorkflowPermissionHandler.permissions` / `getHandler` | 2026-09-28 |
| S-L088-02 | 工作树 | 五档办理人；展开角色时不看 Client | `TaskAssigneeEnum`、`FlwTaskAssigneeServiceImpl`、`SysUserServiceImpl.selectUsersByRoleIds` | 2026-09-28 |
| S-L088-03 | 工作树 | 设计器角色名单限定当前 `clientPk`；用户/部门/岗位不按 Client | `SysTaskAssigneeServiceImpl` | 2026-09-28 |
| S-L088-04 | 工作树 | 待办查询只等于 `processed_by` | `FlwTaskMapper.getListRunTask`、`FlwTaskServiceImpl.pageByTaskWait` | 2026-09-28 |
| S-L088-05 | 工作树 | 非空 `access_path` 在进接口前拦截；home 不追加工作流路径 | `SecurityConfig.validateClientAccessRules`、`ClientAccessPaths` | 2026-09-28 |
| S-L088-06 | 工作树 | 登录必须持有该 Client 的登录域；会话角色按 Client 加载 | `ClientUserTypeAccessService`、`SysLoginService.buildLoginUser` | 2026-09-28 |
| S-L088-07 | 工作树 | 角色属于 Client；授角色前检查登录域 | `SysRole.clientId`、`SysRoleServiceImpl.validateUsersHaveRoleClientType` | 2026-09-28 |
| S-L088-08 | 工作树 | 流程表无 `client_id`；`permission_flag` 长度 200；本模块不使用 `tenant_id` | `30-cde-workflow.sql`、`WarmFlowConfig` | 2026-09-28 |
| S-L088-09 | Warm-Flow 1.8.9 源码包 | 生成待办时 `convertPermissions`；`checkAuth` 比较用户标识；`ignore` 跳过 | `ExpressionUtil`、`TaskServiceImpl.checkAuth`、`FlowParams.getPermissionFlag` | 2026-09-28 |
| S-L088-10 | 工作树 | 申请节点改成发起人；办理请求可带 `ignore` 和 `handler` | `WorkflowGlobalListener.assignment`、`CompleteExecuteComponent`、`CompleteTaskBo` | 2026-09-28 |
| S-L088-11 | 工作树 | 流程页面只组装进 `admin-web` | `frontend/apps/admin-web/src/router/adminManifestRegistry.ts`；`home-web` 无 workflow 引用 | 2026-09-28 |
| S-L088-12 | 工作树 | SpEL 只有部门负责人；节点扩展没有 Client 变量 | `SpelRuleComponent`、`VariablesEnum`、`FlwNodeExtServiceImpl` | 2026-09-28 |

未验证：没有启动设计器，没有对运行中的数据库查询是否存在手工复制的工作流菜单或其他 Client 的 `access_path`。代码路径上的「节点无 Client、办理比用户 ID」不依赖那台库。
