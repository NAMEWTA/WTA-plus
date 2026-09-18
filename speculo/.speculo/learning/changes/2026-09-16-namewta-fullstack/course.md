# 课程设计：NAMEWTA 前后端全模块深度学习

## 目标与期望效果

学完本 Goal 的标准路径后，合格读者能对着当前工作树（不是记忆中的文档）做到：

1. 画出 C4 Context 与 Container，并指出一条管理端主路径和一条失败路径的存储点与所有权。
2. 按模块说出公开 HTTP / UseCase / 前端 domain 工厂各自做什么，以及枢纽方法的副作用与失败路径。
3. 指出已有四门课哪些格子仍要升级（architecture 从 standard 到 deep；sso/notify/third 补前端）。
4. 遇到文档打架时以工作树为准，并把冲突记成来源问题。

本课是父 Change。逻辑子 Change 按 **总览 / 业务域 / 模块 / 切片 / 平台** 分组。四门原料课已于 `2026-09-16T07:53:45.456Z` 物理嵌入 `children/<child-id>/`。

## 学习者与表达/深度配置

| 字段 | 值 |
| --- | --- |
| 受众 | 正在本仓库工作的开发者 |
| 交互语言 | `zh-CN` |
| expression_level | `eli5` |
| coverage_depth | `deep` |
| Lesson 时长 | `35` 分钟（允许 30–40） |
| 默认 Homework 题数 | `5`（本计划不生成作业） |
| project_path | `/srv/WTA-plus` |

## 逻辑子 Change 分类

| 类型 | 稳定 ID 或标签 | 覆盖 | 物理位置（当前 → 目标） |
| --- | --- | --- | --- |
| 总览 | `2026-09-14-namewta-architecture` | C4、组装、layered/classic、前端所有权、合同、请求走查 | `children/2026-09-14-namewta-architecture/` |
| 业务域 | `2026-09-14-wta-sso` | SSO 授权码/令牌/会话 | `children/2026-09-14-wta-sso/` |
| 业务域 | `2026-09-14-wta-notify` | 公告/收件箱/配置/收件人/监控/回调/应用/Outbox | `children/2026-09-14-wta-notify/` |
| 业务域 | `2026-09-14-wta-third` | Provider/Endpoint/Credential/Observability/Gateway | `children/2026-09-14-wta-third/` |
| 平台 | `platform-frontend`（父根新建课） | App 组合、platform 端口、adapters、web-kit、api-contracts | 本 Change `lessons/` |
| 切片 | `slice-auth` | `AuthController` / Captcha / 登录策略 / IdentityAccessService | 本 Change `lessons/` |
| 模块 | `module-system` | 用户/角色/菜单/部门/岗位/Client/字典/配置/监控 | 本 Change `lessons/` |
| 切片 | `slice-oss` | OSS 配置/对象/直传/迁移 + 浏览器 adapter | 本 Change `lessons/` |
| 切片 | `slice-openapi` | OpenAPI 目录/凭据 + common-openapi | 本 Change `lessons/` |
| 模块 | `module-profile` | person / enterprise 五层 + 前端 profile | 本 Change `lessons/` |
| 模块 | `module-workflow` | WarmFlow 分类/定义/实例/任务/请假 + 前端 | 本 Change `lessons/` |
| 模块 | `module-demo` | TestDemo / TestTree / TestRichText 成熟切片 | 本 Change `lessons/` |
| 模块 | `module-job` | SnailJob 执行器入口与 bundle 接线 | 本 Change `lessons/` |
| 模块 | `module-ai` | SnailAi 注册 + 前端 chat | 本 Change `lessons/` |
| 平台 | `platform-common` | NotifyDispatcher、MyBatis 基类、Sa-Token、MySQL 基座 | 本 Change `lessons/` |

后续若要把父根新建课再拆成独立子 Change，必须新开 C 或新 Change，不得在 `/goal` 里偷偷搬文件。

## 目标合同

每个 OBJ 对应 chain 中一节 35 分钟课。关键性均为「是」，除非标明。Homework 列为本计划不生成。

| ID | 可观察目标 | 前置 OBJ | 证据类型 | Lesson | 子 Change |
| --- | --- | --- | --- | --- | --- |
| OBJ-01 | 面对仓库树能指出 `frontend/`、`backend/`、`docs/`、`release-artifacts/`、`.agents/skills/` 各自拥有什么，并说明交付不是 git submodule | none | 解释+指路径 | L-001 | overview |
| OBJ-02 | 能画出后端依赖方向：`wta-admin` 组装，业务模块走 `wta-api` 和最小 `wta-common-*`，common 不反向依赖业务 | OBJ-01 | 解释+画图 | L-002 | overview |
| OBJ-03 | 能用登记表区分 layered 与 classic，说出新模块默认五层，并拒绝把两种模式混在同一模块 | OBJ-02 | 解释+对照登记表 | L-003 | overview |
| OBJ-04 | 能说明 App → web-domain → domain → platform，并指出工作树里三个 App 包与文档发布面冲突 | OBJ-01 | 解释+指包名 | L-004 | overview |
| OBJ-05 | 能识别 HTTP/JSON、SQL 基座、OpenAPI transport、`wta-api` 是公共合同；跨端变更先后端 | OBJ-02, OBJ-04 | 解释+分类 | L-005 | overview |
| OBJ-06 | 能按层口述一条管理端只读查询从浏览器走到 Mapper XML，并指出至少一种失败抄近路 | OBJ-02 … OBJ-05 | 迁移/应用 | L-006 | overview |
| OBJ-07 | 能指着 `admin-web/src/application/services.ts` 说出每个 `create*Service` 与 OSS/通知目录的组合点 | OBJ-04 | 解释+走查 | L-007 | platform-frontend |
| OBJ-08 | 能对比 `home-web` 与 `sso-web` 的组装：home 只接身份+资料，sso-web 是认人厅不是管理端 | OBJ-07 | 解释+对照 | L-008 | platform-frontend |
| OBJ-09 | 能说明 `platform-{http,auth,permission,app-runtime,contracts}` 端口与 App 适配器的方向 | OBJ-07 | 解释+指导出 | L-009 | platform-frontend |
| OBJ-10 | 能说明 `adapters/*`、`web-kit/*`、`api-contracts` 各自所有权；transport 不可手改生成结果 | OBJ-09 | 解释+边界 | L-010 | platform-frontend |
| OBJ-11 | 能口述 `AuthController`：`login` / `logout` / `register` / `client/context` / `social/callback` | OBJ-06 | 解释+走查 | L-011 | slice-auth |
| OBJ-12 | 能口述 `CaptchaController` 三个 code 入口与密码策略如何挡住非法注册/登录 | OBJ-11 | 解释+走查 | L-012 | slice-auth |
| OBJ-13 | 能说明 `IAuthStrategy` / `SysLoginService` / `SysRegisterService` 的副作用、会话写入和失败路径 | OBJ-11 | 方法性状 | L-013 | slice-auth |
| OBJ-14 | 能口述 `IdentityAccessService.login/prepareLogin/logout/register` 如何打到 `/auth/*`，以及 session 命名空间 | OBJ-11, OBJ-07 | 解释+走查 | L-014 | slice-auth |
| OBJ-15 | 能口述 `SysUserController` 与临时密码/重置候选的公开方法链 | OBJ-03 | 解释+走查 | L-015 | module-system |
| OBJ-16 | 能口述 `SysRoleController` + `SysMenuController` + `ISysPermissionService` 如何构成 RBAC | OBJ-15 | 解释+走查 | L-016 | module-system |
| OBJ-17 | 能口述 `SysDeptController` / `SysPostController` / `SysUserTypeController` 的公开方法 | OBJ-15 | 解释+走查 | L-017 | module-system |
| OBJ-18 | 能口述 `SysClientController`（含 SSO bind/rotate）与 `SysSsoAppController` | OBJ-15 | 解释+走查 | L-018 | module-system |
| OBJ-19 | 能指着 `createSystemService` 的 `users/roles/menus/departments/posts/clients/userTypes` 说明前端如何映射 HTTP | OBJ-15 … OBJ-18 | 解释+对照 | L-019 | module-system |
| OBJ-20 | 能说明 `createSystemWebDomain` 与 admin-web 导航/权限 host 如何消费服务端菜单 | OBJ-19, OBJ-09 | 解释+走查 | L-020 | module-system |
| OBJ-21 | 能口述 `SysConfigController` 与字典 Type/Data 的缓存刷新失败路径 | OBJ-03 | 解释+走查 | L-021 | module-system |
| OBJ-22 | 能口述 `SysSocialController` 与 `SysProfileController`（账号资料页，不是 profile 模块） | OBJ-15 | 解释+走查 | L-022 | module-system |
| OBJ-23 | 能口述 monitor：`CacheController` / `SysLoginInfoController` / `SysOperlogController` / `SysUserOnlineController` | OBJ-13 | 解释+走查 | L-023 | module-system |
| OBJ-24 | 能说明 `createMonitorService` 与 web-domain monitor 页面 | OBJ-23 | 解释+对照 | L-024 | module-system |
| OBJ-25 | 能口述 `SysOssConfigController` 配置切换副作用 | OBJ-05 | 解释+走查 | L-025 | slice-oss |
| OBJ-26 | 能口述 `SysOssController` 列表/下载 URL/删除 | OBJ-25 | 解释+走查 | L-026 | slice-oss |
| OBJ-27 | 能口述直传：`SysOssUploadController.init/signParts/parts/complete/abort` 与失败回滚 | OBJ-26 | 方法性状 | L-027 | slice-oss |
| OBJ-28 | 能口述 `SysOssMigrationController` dry-run/start/retry/rollback/cleanup | OBJ-26 | 解释+走查 | L-028 | slice-oss |
| OBJ-29 | 能口述 `createOssUploadClient` + `useDirectOssUpload` + FileUpload 如何拿 ticket 直传 MinIO | OBJ-27, OBJ-07 | 解释+走查 | L-029 | slice-oss |
| OBJ-30 | 能口述 `SysOpenApiCatalogController` 自有/他人接口目录 | OBJ-05 | 解释+走查 | L-030 | slice-openapi |
| OBJ-31 | 能口述 `SysOpenApiCredentialController` 创建/重置/启停/删除（明文不回显） | OBJ-30 | 方法性状 | L-031 | slice-openapi |
| OBJ-32 | 能说明 `wta-common-openapi` 网关、HMAC、Sa-Token 机器会话的失败关闭 | OBJ-31 | 解释+走查 | L-032 | slice-openapi |
| OBJ-33 | 能说明 `createOpenApiService` 与 admin-web `openApi.vue` | OBJ-31 | 解释+对照 | L-033 | slice-openapi |
| OBJ-34 | 能口述 `PersonAdminController` 档案/审核/绑定/吊销 | OBJ-03 | 解释+走查 | L-034 | module-profile |
| OBJ-35 | 能口述 `PersonApplicationController` current/submit | OBJ-34 | 解释+走查 | L-035 | module-profile |
| OBJ-36 | 能口述 `PersonRebindController` probe/match/confirm/submit/unbind | OBJ-35 | 方法性状 | L-036 | module-profile |
| OBJ-37 | 能口述个人材料 attach/detach 与 `MaterialTagController` | OBJ-34 | 解释+走查 | L-037 | module-profile |
| OBJ-38 | 能口述 `PersonVerificationAnonymousController` 供应商回调 | OBJ-35 | 解释+走查 | L-038 | module-profile |
| OBJ-39 | 能说明 `createProfileService` 与 web-domain person/self/material-tag | OBJ-34 … OBJ-37 | 解释+对照 | L-039 | module-profile |
| OBJ-40 | 能口述 `EnterpriseAdminController` 档案/审核/绑定 | OBJ-34 | 解释+走查 | L-040 | module-profile |
| OBJ-41 | 能口述 `EnterpriseApplicationController.current/save/submit/probe` 与 `EnterpriseVerificationAnonymousController.callback` | OBJ-40 | 解释+走查 | L-041 | module-profile |
| OBJ-42 | 能口述 `EnterpriseTransferController` send/confirm/unbind | OBJ-41 | 方法性状 | L-042 | module-profile |
| OBJ-43 | 能口述企业材料 admin/self 切片 | OBJ-40 | 解释+走查 | L-043 | module-profile |
| OBJ-44 | 能说明 web-domain enterprise 页面如何消费 profileService | OBJ-40 … OBJ-43 | 解释+对照 | L-044 | module-profile |
| OBJ-45 | 能口述 `/notify/notice` 列表/详情/保存/发布/撤回/删除的五层链 | OBJ-03 | 解释+走查 | L-045 | domain-notify |
| OBJ-46 | 能口述 `/notify/inbox` 列表与已读 | OBJ-45 | 解释+走查 | L-046 | domain-notify |
| OBJ-47 | 能口述 `/notify/config` 账号/场景/测试发送 | OBJ-45 | 解释+走查 | L-047 | domain-notify |
| OBJ-48 | 能口述 `/notify/recipients` 搜索 | OBJ-45 | 解释+走查 | L-048 | domain-notify |
| OBJ-49 | 能口述 `/notify/monitor` snapshot/deliveries | OBJ-45 | 解释+走查 | L-049 | domain-notify |
| OBJ-50 | 能口述 `POST /notify/callback/{channel}` | OBJ-47 | 方法性状 | L-050 | domain-notify |
| OBJ-51 | 能口述 `/notify/notification` submit/query/retry/cancel | OBJ-45 | 解释+走查 | L-051 | domain-notify |
| OBJ-52 | 能口述 Outbox 领取与 worker/wake | OBJ-51 | 方法性状 | L-052 | domain-notify |
| OBJ-53 | 能说明 `createNotificationService` 与 notificationDirectory | OBJ-45 … OBJ-51 | 解释+对照 | L-053 | domain-notify |
| OBJ-54 | 能说明 `createNotifyWebDomain` 公告/收件箱/配置/监控页 | OBJ-53 | 解释+走查 | L-054 | domain-notify |
| OBJ-55 | 能口述 `GET /sso/oauth2/authorize` 的 PKCE/Client/授权码链 | OBJ-18 | 解释+走查 | L-055 | domain-sso |
| OBJ-56 | 能口述 `POST /sso/oauth2/token` 与 `/revoke` | OBJ-55 | 方法性状 | L-056 | domain-sso |
| OBJ-57 | 能口述 `POST /sso/login`、`GET /sso/session`、`POST /sso/logout` | OBJ-55 | 解释+走查 | L-057 | domain-sso |
| OBJ-58 | 能口述 sso-web `parseAuthorizeQuery` / `loginWithPassword` / `requestAuthorize` | OBJ-55 … OBJ-57 | 解释+走查 | L-058 | domain-sso |
| OBJ-59 | 能说明 `SsoClientCatalog` / `SsoIdentityService` 只经 `wta-api` 读用户与 Client | OBJ-55, OBJ-18 | 解释+边界 | L-059 | domain-sso |
| OBJ-60 | 能口述 Provider 管理五层与路径安全 | OBJ-03 | 解释+走查 | L-060 | domain-third |
| OBJ-61 | 能口述 Endpoint 切片以及网关如何选中它 | OBJ-60 | 解释+走查 | L-061 | domain-third |
| OBJ-62 | 能口述 Credential 加密、列表不回显、删除 | OBJ-60 | 方法性状 | L-062 | domain-third |
| OBJ-63 | 能口述 invocation/statistics 查询 | OBJ-61 | 解释+走查 | L-063 | domain-third |
| OBJ-64 | 能口述 `ThirdPartyGateway.execute` → Adapter → SPI → 记录 | OBJ-61, OBJ-62 | 方法性状 | L-064 | domain-third |
| OBJ-65 | 能说明 `createThirdService` | OBJ-60 … OBJ-63 | 解释+对照 | L-065 | domain-third |
| OBJ-66 | 能说明 `createThirdWebDomain` | OBJ-65 | 解释+走查 | L-066 | domain-third |
| OBJ-67 | 能口述 `FlwCategoryController` | OBJ-03 | 解释+走查 | L-067 | module-workflow |
| OBJ-68 | 能口述 `FlwDefinitionController` 发布/导入 | OBJ-67 | 解释+走查 | L-068 | module-workflow |
| OBJ-69 | 能口述 `FlwInstanceController` 与 `WorkflowService` 跨模块合同 | OBJ-68 | 解释+走查 | L-069 | module-workflow |
| OBJ-70 | 能口述 `FlwTaskController` / `IFlwTaskService` 启动、完成、驳回、催办、终止 | OBJ-69 | 方法性状 | L-070 | module-workflow |
| OBJ-71 | 能口述 `FlwSpelController` | OBJ-68 | 解释+走查 | L-071 | module-workflow |
| OBJ-72 | 能口述 `TestLeaveController` 含 `submitAndFlowStart` | OBJ-70 | 解释+走查 | L-072 | module-workflow |
| OBJ-73 | 能说明 `createWorkflowDefinitionService` 与 `createWorkflowWebDomain` | OBJ-67 … OBJ-72 | 解释+对照 | L-073 | module-workflow |
| OBJ-74 | 能口述 `TestDemoController` classic CRUD（含导入导出）作为成熟样例 | OBJ-03 | 解释+走查 | L-074 | module-demo |
| OBJ-75 | 能口述 `TestTreeController` 树表 | OBJ-74 | 解释+走查 | L-075 | module-demo |
| OBJ-76 | 能口述 `TestRichTextController` 与 OSS 资产解析 | OBJ-74, OBJ-29 | 解释+走查 | L-076 | module-demo |
| OBJ-77 | 能说明 `createDemoService` / `createRichTextService` 与 demo web-domain | OBJ-74 … OBJ-76 | 解释+对照 | L-077 | module-demo |
| OBJ-78 | 能指出 `wta-job` 的 SnailJob 执行器类是什么入口、不是 HTTP Controller | OBJ-02 | 解释+指类 | L-078 | module-job |
| OBJ-79 | 能说明 job 只在 `bundle-full` 进入 admin jar，`bundle-core` 排除 | OBJ-78 | 解释+对照 pom | L-079 | module-job |
| OBJ-80 | 能口述 `SnailAiController.registerCurrentUser` | OBJ-02 | 解释+走查 | L-080 | module-ai |
| OBJ-81 | 能说明 `createAiService` 与 `createAiWebDomain` | OBJ-80 | 解释+对照 | L-081 | module-ai |
| OBJ-82 | 能口述 `NotifyClient.send` / `NotifyDispatcher` 与业务模块的同步入口 | OBJ-51 | 方法性状 | L-082 | platform-common |
| OBJ-83 | 能说明 `BaseEntity` / `BaseMapperPlus` / QueryBuilder 的所有权：DAO 或 classic ServiceImpl 才持 Mapper | OBJ-03 | 解释+边界 | L-083 | platform-common |
| OBJ-84 | 能说明 Sa-Token 会话写入与 Redis 的失败路径（登录成功但会话丢失算哪边） | OBJ-13 | 方法性状 | L-084 | platform-common |
| OBJ-85 | 能指出 MySQL `10-cde-base-ddl.sql` / `50-cde-base-dml.sql` 是自有表唯一基座，以及动态数据源 `@DS` 的所有权 | OBJ-05 | 解释+指文件 | L-085 | platform-common |

## 课程地图

结构：**总览地图 → 前端平台 → 认证切片 → system 模块 → OSS/OpenAPI 切片 → profile 模块 → 已有业务域升级并补前端 → workflow/demo/job/ai → 公共枢纽与数据基座**。

```text
L-001 … L-006  总览（升级现有 architecture）
        |
        v
L-007 … L-010  前端平台容器
        |
        v
L-011 … L-014  认证切片
        |
        +-- L-015 … L-024  system 模块（RBAC / 配置监控）
        |
        +-- L-025 … L-029  OSS 切片
        |
        +-- L-030 … L-033  OpenAPI 切片
        |
        +-- L-034 … L-044  profile person / enterprise
        |
        +-- L-045 … L-054  notify 域（升级 + 前端）
        |
        +-- L-055 … L-059  sso 域（升级 + sso-web）
        |
        +-- L-060 … L-066  third 域（升级 + 前端）
        |
        +-- L-067 … L-073  workflow
        |
        +-- L-074 … L-077  demo 成熟切片
        |
        +-- L-078 … L-081  job / ai
        |
        v
L-082 … L-085  common 枢纽 + MySQL 基座
```

`/goal` 按 `goal/chain.md` 的 mine unit 执行：先写完当前单元全部 L（阶段 T），再按课 mine（阶段 M），再 Lead 处置（阶段 D）。上图的并列只表示依赖，不是并发授权，也不是挖掘门。覆盖波只有两波（架构/数据流、公开 API）；85 节切成 U1…U9，每单元 ≤15。

**标准路径**：L-001 → L-085 全上，按 U1→U9。

**较短路径**（不能声称「全模块 deep」）：完成 U1（L-001…L-006）再加当前正在改的那一个模块所在 unit。其余格子保持 `uncovered` 或 `deferred`。

## 成功证据与范围外

成功（仍是 `mastery.overall=unverified`，要等 Homework/Review）：

- 覆盖矩阵范围内格子全部 `covered` | `deferred(reason)` | `covered-by-parent`。
- 每节新课或改写课至多两份 `goal/probes/GP-*-b0N.md`（每课 ≤10 问）。
- `goal/verify.md` 对照 stop-rules 勾选。

范围外（矩阵标 `deferred`，不为它们开课）：

- `wta-extend` 独立进程（monitor-admin / snailai-server / snailjob-server）内部实现。
- WarmFlow / SnailJob / SnailAI 厂商引擎内部。
- Nacos 服务器产品本身（`wta-common-nacos` 客户端叠加标 deferred(optional-overlay)）。
- 生成物：`packages/api-contracts` 快照字节、Vite auto-import d.ts。
- 测试夹具、`node_modules`、`target`、`dist`。
- `speculo/`、`.agents/skills` 作为运行时（它们是来源，不是业务房间）。
- demo 能力展示 Controller（Redis/MQTT/MCP/Excel/ES/WebSocket/限流/加密/敏感/SaToken 文档/邮件短信演示/队列）：`deferred(capability-demo-not-product-room)`，模式由 TestDemo 课 `covered-by-parent`。
- 琐碎 helper、converter、BO/VO getter。

## Revision 记录

| 时间 | 变化 | 原因 | 是否重新生成 Lesson |
| --- | --- | --- | --- |
| 2026-09-16T06:56:05.000Z | 初版全模块 deep 课程地图；引用现有四门课为可升级子 Change | 激活 G-goal；用户要求前后端各模块全面深度学习并归档为同一 Change | 本会话不写 Lesson 正文 |
| 2026-09-16T08:47:16.000Z | 核对：children/ 已嵌入；去掉一课一挖与 18 波挖掘门；OBJ 未改 | G-goal replan，对齐最新 mine unit / teach-then-mine | 仍不写 Lesson 正文 |
