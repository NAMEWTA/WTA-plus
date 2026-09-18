---
lesson_id: L-040
objective_ids: [OBJ-40]
claimed_cells:
  - A:EnterpriseAdminController.*
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: twelve-windows-on-disk
    minutes: 9
  - segment: layered-lifecycle-and-fences
    minutes: 10
  - segment: visuals-and-worked-examples
    minutes: 8
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 4
expression_level: eli5
coverage_depth: deep
source_ids: [S-004, S-008, S-010, S-015, S-L003-01, S-L034-01, S-L040-01, S-L040-02, S-L040-03, S-L040-04, S-L040-05, S-L040-06, S-L040-07, S-L040-08]
---

# Lesson 040：宏观企业柜台——EnterpriseAdminController 档案 / 审核 / 绑定

## 学完你能做什么

打开厨房 `backend/wta-modules/wta-profile/wta-profile-enterprise/src/main/java/org/namewta/profile/enterprise/controller/admin/EnterpriseAdminController.java`。这份类**正好十二个**公开 HTTP 方法，门牌只有一块：`/profile/enterprise/archive`。你能**口述管理端怎么管一本企业档案**：怎么翻名册、怎么看袋里的材料出门条、怎么盖通过/驳回章、怎么直建、怎么改执照页、怎么把负责人钉上去或撕下来、怎么把整本作废。口试名单就是矩阵 (a) 这一行 **`A:EnterpriseAdminController.*`**，磁盘方法是：

1. **`page`**：`GET /profile/enterprise/archive`。名册。权限 `profile:enterprise:query`。
2. **`eligibleUsers`**：`GET /profile/enterprise/archive/eligible-users`。还能当负责人的账号候选人。权限 **`profile:enterprise:override`**，不是 query。
3. **`detail`**：`GET /profile/enterprise/archive/{profileId}`。一本档案的摘要 + 版本 + 绑定史 + 来源 + 审计 + 当前材料名牌。权限 query。
4. **`reviewContext`**：`GET /profile/enterprise/archive/application/{applicationId}/review-context`。待审申请的快照。权限 `profile:enterprise:review`。Java 方法叫 `reviewContext`，UseCase/Service 叫 **`review`**。
5. **`reviewMaterial`**：`GET .../application/{applicationId}/material/{materialRefId}/access-url`。申请袋里一张材料的出门条。权限 review。
6. **`material`**：`GET .../{profileId}/material/{materialRefId}/access-url`。**已发布版本**袋里一张材料的出门条。权限 `profile:enterprise:material`。
7. **`decide`**：`POST .../application/{applicationId}/decision`。管理员决定。权限 override。`@Log` 双 false。
8. **`create`**：`POST .../admin-create`。管理员直建档案。权限 override。
9. **`revise`**：`POST .../{profileId}/revision`。管理员覆盖执照页。权限 override。
10. **`assign`**：`POST .../{profileId}/assign`。把一本空着的档案钉到一个合格账号。权限 override。
11. **`manageBinding`**：`POST .../{profileId}/binding`。暂停 / 恢复 / 解绑。权限 **`profile:enterprise:manage`**，十二扇里唯一挂 manage 的写窗。
12. **`revoke`**：`POST .../{profileId}/revoke`。注销整本档案。权限 override。不是 HTTP DELETE，也不是只解绑，更不是转移挑战。

OBJ-40 要你当场说完的那句是：**这是 layered 的管理端企业档案柜台，不是 system 资料页，不是本人自助申请窗，也不是转移挑战窗。查询 GET、变更 POST；写窗把操作者从登录票取出，不信请求体里的人。档案、审核、绑定是三件不同的事：注销作废整本；解绑只拔负责人钉子；审核只处理 WAITING 申请，并且先掐掉工作流再发布。企业负责人还必须先有一本个人档案。**

`wta-profile` 在登记表是 **layered**。能跑五层检查的房间是隔壁 `wta-profile-person`（L-034）和本课 `wta-profile-enterprise`。主链路是 `Controller -> UseCase -> Service -> DAO -> Mapper -> XML`。不要口述成 classic 的 `Controller → I*Service → ServiceImpl → Mapper`。

本课**不宣称**你会拆 `EnterpriseApplicationController` 的 current/save/submit/probe 与核身匿名回调（L-041）、`EnterpriseTransferController` 的 send/confirm/unbind（L-042）、企业材料 admin/self（L-043），或把 web-domain 怎么消费 `profileService` 再讲一遍（L-044）。今天只认：**管理端这一扇门牌上的十二个方法，以及它们怎样穿过五层去动企业档案。**

## 先把宏观地图放在桌上

L-003 已经把 `wta-profile` 钉在 layered 列：编排在 UseCase，规则在 Service，SQL 条件在 DAO/XML。L-034 是同一栋楼的**个人**柜台，形状像，表名、权限前缀、候选人资格都不是本课对象。L-022 的 `SysProfileController` 是 **system 账号资料页**。本课走进档案楼的**企业管理员侧门**。

2026-09-16 工作树先钉死**包边界**（口试先数包，再数方法）：

| 你可能以为的名字 | 磁盘事实 |
| --- | --- |
| `SysProfileController` / `SysUserController` | **另一栋楼。** classic `wta-system`。本课 Java 在 `org.namewta.profile.enterprise.controller.admin` |
| `PersonAdminController` | **隔壁个人柜台。** `/profile/person/archive`。L-034。表、实体、Mapper 互不 import |
| `EnterpriseApplicationController` | **自助门。** `@RequestMapping("/profile/enterprise/application")`，权限 `profile:enterprise:apply`。L-041 |
| `EnterpriseMaterialAdminController` | **材料总库门。** `/profile/enterprise/materials/{ownerType}/{ownerId}`。L-043 |
| `EnterpriseTransferController` | **转移门。** `/profile/enterprise/transfer`。L-042。本课 `revoke` **不是**撤销转移挑战 |
| `IEnterpriseAdminService` + `EnterpriseAdminServiceImpl` | **生产主链不用这对。** 接口在 `service/` 且全仓只有自己这一处引用；`EnterpriseAdminServiceImpl` 只在 `src/test`。HTTP 注入的是 `EnterpriseAdminUseCase` |
| Controller 字段名叫 `service` | 类型是 **`EnterpriseAdminUseCase`**。不要被字段名骗去找 `EnterpriseAdminService` |
| 绑定状态 `EFFECTIVE` | **没有这个枚举值。** SQL 用 `status in ('ACTIVE','SUSPENDED')` 表示「还钉着」 |
| HTTP DELETE 注销 | **没有。** 合同测试断言方法名不含 `export`、`delete`。注销是 `POST .../revoke` |
| `IEnterpriseAdminService.revoke` 注释「撤销转移挑战或档案绑定」 | **注释撒谎。** 磁盘是把档案打成 `REVOKED`，有钉子再顺手 `UNBOUND` |
| `EnterpriseAdminUseCaseImpl.revoke` 注释「撤销企业档案账号绑定」 | **也撒谎。** 那是解绑的活，走 `manageBinding` 的 `UNBIND` |

同一模块还有 `controller/self`（已登录本人）和 `controller/anonymous`（`@SaIgnore` 供应商回调）。架构测试锁死：admin/self **不得**写 `@SaIgnore`；anonymous 文件名必须带 `Anonymous`。本课十二扇全在 admin。

```text
已登录管理员（浏览器，Admin-Token）
        │
        v
 /profile/enterprise/archive/*          ← 本课唯一门牌
        │
        ├─ GET  /                       page          query
        ├─ GET  /eligible-users         候选人        override
        ├─ GET  /{profileId}            detail        query
        ├─ GET  /{id}/material/{ref}/access-url       material
        ├─ GET  /application/{id}/review-context      review
        ├─ GET  /application/{id}/material/{ref}/access-url
        ├─ POST /admin-create           直建          override + @Log
        ├─ POST /{id}/revision          覆盖执照页    override + @Log
        ├─ POST /{id}/assign            钉负责人      override + @Log
        ├─ POST /{id}/binding           暂停/恢复/解绑 manage + @Log
        ├─ POST /{id}/revoke            整本注销      override + @Log
        └─ POST /application/{id}/decision  通过/驳回 override + @Log
                        │
                        v
              EnterpriseAdminUseCaseImpl     @DSTransactional（含 GET）
                        │
                        v
              EnterpriseAdminService         规则、状态机、调端口
                        │
          ┌─────────────┼──────────────────┬─────────────────┬────────────┐
          v             v                  v                 v            v
 EnterpriseAdminDao  PublicationPort   MaterialPort   WorkflowGateway  ProfileService
 Mapper XML          （发布通过件）     （材料袋）      terminate        查个人档案
          │
          v
   profile_enterprise / _version / _binding / _source / _application
   profile_decision_record / profile_operation_audit / profile_material_ref
   （后三张是档案楼共用本子，用 profile_type='ENTERPRISE' 区分）
```

| 符号 | 磁盘 | 拥有什么 | 不拥有什么 |
| --- | --- | --- | --- |
| `EnterpriseAdminController` | `controller/admin/EnterpriseAdminController.java` | 十二扇映射、权限串、`R` 包装、写窗从 `LoginHelper.getUserId()` 取操作者 | Mapper、事务、状态机 |
| `EnterpriseAdminExceptionHandler` | `controller/advice/` | 只捕 `EnterpriseAdminException` → `R.fail(category)` | HTTP 状态码改写、i18n |
| `EnterpriseAdminUseCase` / `Impl` | `usecase/` | 十二个场景 + 每方法 `@DSTransactional`；无操作者的 default 直接 `UnsupportedOperationException` | DAO、Mapper、`service.impl` |
| `EnterpriseAdminService` | `service/EnterpriseAdminService.java` | 决策/直建/覆盖/绑定/吊销规则；`@Service` 具体类 | Mapper import、`IService` |
| `EnterpriseAdminDao` | `dao/EnterpriseAdminDao.java` | 锁语义和 Mapper 调用 | 业务 if/状态机 |
| `EnterpriseAdminMapper.xml` | `resources/mapper/enterprise/` | `FOR UPDATE`、生成列不手写、条件 SQL | VO 类型 |

**图题 / caption：** 管理端企业档案宏观柜台。alt：十二扇窗共用门牌 `/profile/enterprise/archive`；Controller 只包装；UseCase 包事务；Service 改档案/申请/绑定并调用发布、材料、工作流三个端口，外加 `UserService` 与 `ProfileService`。

**文字等价物：** 工商科门口挂一块牌子「企业档案」。左边六扇是看：名册、候选人、执照袋、两张材料出门条、待审桌。右边六扇是改：直建、改页、钉负责人、暂停/恢复/解绑、整本作废、给待审件盖章。柜台后面不是办事员直接翻柜子，而是先填一张「用例单」（UseCase），单子进保险柜事务，再由科员（Service）按规矩改本子。本子在 DAO/XML。隔壁还有本人窗口、材料库窗口、转移窗口，门牌都不是 archive。候选人窗口还要先问隔壁个人柜台：这人有没有身份证。

**类比：** 一本营业执照（`profile_enterprise`）可以有很多历史页（version：CURRENT / SUPERSEDED），可以钉在一把负责人钥匙上（binding：ACTIVE / SUSPENDED / UNBOUND），也可以整本盖「作废」（REVOKED）。待审件是另一叠表格（application），不是执照本身。管理员盖章前必须先把流水线上的审批机器停掉，否则两个人同时盖章。这把负责人钥匙还规定：先有个人身份证，才能当企业负责人。

**类比失效处：**

1. 暂停绑定不是作废执照。`SUSPEND` 之后档案仍是 `ACTIVE`，只是这把钥匙暂时不能当「有效钉子」用；唯一键生成列在 ACTIVE 和 SUSPENDED 时都还占着位子。
2. 解绑不是注销。`UNBIND` 把钉子拔掉，本子还活着，还可以 `assign` 钉给别人。
3. `revoke` 才是整本 `REVOKED`。默认名册看不见它；要显式 `status=REVOKED` 才出现。作废后 `assign` / `revise` / `binding` 一律 `ENTERPRISE_PROFILE_REVOKED_READ_ONLY`。
4. 审核通过不是管理员自己 insert 档案那么简单：先 `OVERRIDE_PENDING`，再 `terminate` 工作流，再把申请拨回 `WAITING`，然后走发布端口 `publishApproved`（那会写档案/版本/绑定并把申请打成 `FINISH`）。驳回不发布，申请落 `INVALID`。
5. 「生效绑定」不是状态字 `EFFECTIVE`。口试说「还钉着」时，指 SQL 的 `ACTIVE` 或 `SUSPENDED`。
6. 转移挑战不是这扇决定窗，也不是 `revoke`。L-042 另有 `/profile/enterprise/transfer`。企业 `publishApproved` **没有**个人那边那种 `rebindIntent='Y'` 拒绝；转移根本不走 WAITING 申请桌。
7. 有个人档案 ≠ 有企业档案。候选人过滤器要的是 `ProfileSummary.person() != null`，不是已经钉着一本企业执照。

## 核心概念与机制

### 直觉讲解

先记住四件家具，再背路径：

- **执照本 `profile_enterprise`。** 状态只有 `ACTIVE` / `REVOKED`。活着的身份键是生成列 `active_credit_code`：未注销时等于 `upper(trim(unified_credit_code))`，注销后变 NULL，所以同一信用代码可以在作废后再直建另一本。
- **当前页 `profile_enterprise_version`。** 一页 `CURRENT`，旧页 `SUPERSEDED`。生成列 `current_profile_id` 卡住「一本只有一页当前」。
- **负责人钉子 `profile_enterprise_binding`。** 一本活档案最多一根有效钉子，一个账号最多钉一本活企业档案。生成列 `effective_user_id` / `effective_profile_id` 在 `ACTIVE` **和** `SUSPENDED` 时都有值。
- **待审桌 `profile_enterprise_application`。** 管理员 `decide` 只锁 `status='WAITING'`。自助草稿、核身、转移另有门。

再记住企业这边多出来的一把尺：**统一社会信用代码是执照身份，法定代表人证件号是另一栏。** Service 验号段时拿的是 `legalDocumentTypeCode` 的规则（E2E 用 `CN_RESIDENT_ID`），不是拿信用代码去配身份证正则。身份键 `identityKey` 在领域对象里等于规范化后的信用代码。

再记住五颗权限，不要并成「企业管理员」一词：

| 权限串 | 本课窗子 | 种子菜单文案（50 DML） |
| --- | --- | --- |
| `profile:enterprise:query` | page、detail | 企业档案 |
| `profile:enterprise:review` | reviewContext、reviewMaterial | 企业档案审核 |
| `profile:enterprise:material` | material（已发布袋出门条） | 企业材料办理 |
| `profile:enterprise:manage` | **只有** manageBinding | 企业档案处置 |
| `profile:enterprise:override` | eligibleUsers、decide、create、revise、assign、revoke | 企业档案覆盖 |

query 能在 detail 里看见当前材料**名牌**（Service 调 `materials.list`），但拿不到私有 URL；出门条是另一扇窗、另一颗权限。候选人名单挂 override：那是给直建/指定负责人用的，不是给随便翻名册的人用的。

写窗一律 POST，一律 `@Log(..., isSaveRequestData=false, isSaveResponseData=false)`：袋里是信用代码、法人证件、材料、原因，日志不当成明文仓库。操作者编号只来自 `LoginHelper.getUserId()`。UseCase 上那些「不传 operatorId」的 `@Deprecated default` 会抛 `请传入 operatorId`，生产 Controller 不会走它们。

企业候选人比个人多一道篱笆：账号要启用（`status=="0"`）、企业侧还没有有效钉子，**并且** `ProfileService` 能看见一本个人档案。单测名字就叫 `eligibleUsersRequireAnActivePersonProfileAndNoEnterpriseBinding`。空关键字得到 `[]`，不打用户服务。

### 精确定义与 English term

| 中文口诀 | English term | 磁盘上的东西 |
| --- | --- | --- |
| 企业档案主体 | enterprise profile | 实体 `ProfileEnterprise`，表 `profile_enterprise`；主键 `enterprise_profile_id` |
| 档案状态 | profile status | `ACTIVE` / `REVOKED`；默认列表 `status != 'REVOKED'` |
| 活信用代码唯一键 | active credit code | 生成列 `active_credit_code`；`FieldStrategy.NEVER` |
| 档案页 | profile version | 表 `profile_enterprise_version`；页状态 `CURRENT` / `SUPERSEDED` |
| 来源快照 | source snapshot | 表 `profile_enterprise_source`；直建 `ADMIN_CREATE`，覆盖 `ADMIN_OVERRIDE` |
| 负责人绑定 / 钉子 | binding | 表 `profile_enterprise_binding`；`ACTIVE` / `SUSPENDED` / `UNBOUND` |
| 还钉着 | effective binding | SQL `status in ('ACTIVE','SUSPENDED')`，**不是**枚举值 EFFECTIVE |
| 绑定竞态版 | binding version | 列 `binding_version`；和实体 `@Version`、档案 `version` 不是同一把尺 |
| 申请 | application | 表 `profile_enterprise_application`；decide 只锁 `WAITING` |
| 提交件 | submission | 表 `profile_enterprise_submission`；材料袋 owner 类型 `SUBMISSION` |
| 管理员决定 | admin decision | `EnterpriseAdminDecisionBo.decision` 只认 `APPROVED` / `REJECTED` |
| 覆盖中 | override pending | 申请状态 `OVERRIDE_PENDING`；决定记录先 `PENDING` 再 `FINAL` |
| 直建 | admin create | `POST /admin-create`；来源类型 `ADMIN_CREATE` |
| 覆盖修订 | admin revise | `POST /{id}/revision`；来源 `ADMIN_OVERRIDE`；要 `expectedVersion` |
| 指定负责人 | assign | `POST /{id}/assign`；档案上还不能有有效钉子，人也不能已钉别人，人还要有个人档案 |
| 绑定处置 | manage binding | `SUSPEND` / `RESUME` / `UNBIND`；要 `expectedBindingVersion` |
| 整本注销 | revoke | `POST /{id}/revoke`；档案 `REVOKED`，若有钉子顺便 `UNBOUND` |
| 合格负责人 | qualified owner | `UserService` 启用账号 + 无企业有效钉子 + `ProfileSummary.person() != null` |
| 材料出门条 | access URL | `EnterpriseProfileAccessUrl`；从 OSS 合同投影，不把 system 类型漏出 HTTP |
| 管理结果盒 | admin result | `EnterpriseAdminResultVo(status, profileId, versionId, bindingId, version)`；最后一个 `version` **随窗子换尺子** |
| 业务失败码 | failure category | `EnterpriseAdminException` 的 message，如 `ENTERPRISE_PROFILE_NOT_FOUND`；Handler 原样放进 `R.fail` |
| 五层试点 | layered | Controller → UseCase → Service → DAO → Mapper XML；登记表 `wta-modules/wta-profile` |
| 动态数据源事务 | `@DSTransactional` | UseCaseImpl **每个**方法都有，包括 page/detail |
| 安全写日志 | safe `@Log` | 六扇 POST 全是请求体/响应体都不存 |
| 工作流网关 | workflow gateway | `EnterpriseWorkflowGateway.terminate`；生产适配 `SpringEnterpriseWorkflowGateway` |
| 发布端口 | publication port | `EnterpriseApplicationPublicationPort.publishApproved`；Service 不 import 申请 Mapper |
| 个人档案查找 | profile lookup | `ProfileService.findByUserId` / `findByUserIds`；跨子域走 `wta-api`，不 import person 实现 |

HTTP 合同（API-005）：查询 GET，变更 POST。本 Controller **零**个 `@PutMapping` / `@DeleteMapping`。`EnterpriseAdminHttpContractTest` 按方法名钉死十二扇权限和写窗日志，并额外钉 `assign` 的 `@Log` title 为「管理员指定企业档案负责人」。

`EnterpriseAdminResultVo.version` 口试必须问「这是哪把尺」：

- `decide`：申请的 `decisionVersion`
- `create`：固定 `1`（新档案乐观锁从 0 加到 1 之后的对外值）
- `revise` / `revoke`：档案实体 `version`（`expectedVersion ± 1`）
- `manageBinding` / `assign`：绑定 `binding_version`（assign 新钉子固定 `1`）

不要把盒子里的 `version` 说成版本表的 `version_no`。版本表主键在 `versionId`。

### 机制/因果链

#### 1. 十二扇窗在磁盘上的真实映射

文件：

- `.../controller/admin/EnterpriseAdminController.java`
- `.../usecase/EnterpriseAdminUseCase.java` + `usecase/impl/EnterpriseAdminUseCaseImpl.java`
- `.../service/EnterpriseAdminService.java`
- `.../dao/EnterpriseAdminDao.java`
- `.../mapper/EnterpriseAdminMapper.java` + `resources/mapper/enterprise/EnterpriseAdminMapper.xml`

类注解：`@Validated` `@RestController` `@RequiredArgsConstructor` `@RequestMapping("/profile/enterprise/archive")`。没有 `@ConditionalOnProperty`，没有 `BaseController`。Spring 注入的是 UseCase；Controller 源码 **禁止** import `service` 包（架构测试按文件内容扫描）。路径变量是 `long` 的都标 `@Positive`；请求体标 `@Valid`。`page` 的查询 BO 没有 `@Valid`——分页边界在 Service 里收。

| HTTP | 动词 | Java | 权限 | 入参 | 返回 | 写库？ |
| --- | --- | --- | --- | --- | --- | --- |
| `/profile/enterprise/archive` | GET | `page` | query | `EnterpriseAdminQueryBo` | `PageResult<EnterpriseProfileSummaryVo>` | 否 |
| `/eligible-users` | GET | `eligibleUsers` | override | `keyword` | `List<EnterpriseAccountCandidateVo>` | 否 |
| `/{profileId}` | GET | `detail` | query | 路径 id | `EnterpriseProfileDetailVo` | 否 |
| `/application/{id}/review-context` | GET | `reviewContext` | review | 申请 id | `EnterpriseReviewContextVo` | 否 |
| `/application/{id}/material/{ref}/access-url` | GET | `reviewMaterial` | review | 申请 + 材料 | `EnterpriseProfileAccessUrl` | 否 |
| `/{profileId}/material/{ref}/access-url` | GET | `material` | material | 档案 + 材料 | `EnterpriseProfileAccessUrl` | 否 |
| `/application/{id}/decision` | POST | `decide` | override | `EnterpriseAdminDecisionBo` | `EnterpriseAdminResultVo` | 是 |
| `/admin-create` | POST | `create` | override | `EnterpriseAdminCreateBo` | `EnterpriseAdminResultVo` | 是 |
| `/{profileId}/revision` | POST | `revise` | override | `EnterpriseAdminReviseBo` | `EnterpriseAdminResultVo` | 是 |
| `/{profileId}/assign` | POST | `assign` | override | `EnterpriseAdminAssignBo` | `EnterpriseAdminResultVo` | 是 |
| `/{profileId}/binding` | POST | `manageBinding` | manage | `EnterpriseAdminBindingBo` | `EnterpriseAdminResultVo` | 是 |
| `/{profileId}/revoke` | POST | `revoke` | override | `EnterpriseAdminRevokeBo` | `EnterpriseAdminResultVo` | 是 |

路径别撞车：字面量 `/eligible-users`、`/admin-create`、`/application/{id}/...` 比 `/{profileId}` 更具体。Spring 不会把 `eligible-users` 当成档案号。

#### 2. 五层怎么走，谁许碰 Mapper

口试按登记表背，再按架构测试对磁盘：

1. Controller 只 `import ...usecase.EnterpriseAdminUseCase`，把 HTTP 变成调用。写窗补 `LoginHelper.getUserId()`。
2. `EnterpriseAdminUseCaseImpl` 每个方法 `@DSTransactional` 后调用 `EnterpriseAdminService` 同名方法。它 **不** import dao/mapper，也没有 `service.impl`。
3. `EnterpriseAdminService` 是具体 `@Service`，**不** `implements IEnterpriseAdminService`。它持有 `EnterpriseAdminDao` + 三个端口：`EnterpriseApplicationPublicationPort`、`ProfileMaterialPort`、`EnterpriseWorkflowGateway`，外加 `UserService`、`ProfileService`（都是 `wta-api`）和 `Clock`。
4. DAO 标 `@Repository`，只包 Mapper；不 import service/usecase。
5. Mapper 继承 `BaseMapperPlus<ProfileEnterprise, ProfileEnterprise>`，方法与 XML id 一一对应，**禁止** `@Select` 等注解 SQL。读模型在 `domain/model/read/EnterpriseAdminRows$*Row`，Controller/UseCase **不得** import 这个包。

生产主构造走网关端口。EX-002 允许 Service 上留着「`WorkflowService` + 无用 jsonMapper」的旧构造给测试桥；新代码不许再从主构造塞实现模块。测试目录里的 `EnterpriseAdminServiceImpl` 继承 Service 并 `implements EnterpriseAdminUseCase`，用 `new EnterpriseAdminDao(mapper)` 抄近路——那是夹具，**不是**五层目标形状。夹具还把无操作者重载改成调用 `LoginHelper`；生产接口的 default 是直接抛错。

子域隔离：person 与 enterprise 不得互相依赖实现类、Mapper、Entity 或表。企业要问「这人有没有个人档案」时，走 `ProfileService`，不碰 `profile_person`。架构测试至少把转移 Mapper XML 锁死不得出现 `profile_person` / `sys_user`；管理端 XML 自己也只写 `profile_enterprise*` 和三张共用表。

#### 3. 名册、详情、两张出门条

`page`：`pageNum` 小于 1 当 1；`pageSize<=0` 当 20，上限 200。`status` 空或空白 → SQL `p.status != 'REVOKED'`。显式只接受 `ACTIVE` / `REVOKED`，否则 `ENTERPRISE_QUERY_STATUS_INVALID`。企业名称模糊 `like`，信用代码精确大写。列表左连「还钉着」的绑定，取出 `bindingUserId` / `bindingStatus`。

`eligibleUsers`：关键字空 → 空列表，不打用户服务。否则 `users.searchActiveUsers(strip, 50)`，留下 `status=="0"`、**已有个人档案**、还没有企业有效钉子的人，最多 20 个，投影成 `(userId, userName, nickName)`。未核身（`ProfileSummary.unverified`）进不了名单。

`detail`：没有行 → `ENTERPRISE_PROFILE_NOT_FOUND`（注销了仍能按 id 打开，名册默认只是不列）。材料袋取 **CURRENT** 版本；没有 CURRENT 字样时退回版本列表第一页。`currentMaterials` 是 `ProfileMaterialPort.list(VERSION, versionId)`，不是 access-url。VO 里材料类型仍是端口的 `MaterialReferenceView`。

`review`：申请必须已有 `submission_seq > 0`，并且 join 到对应 submission。材料袋 owner 是 `SUBMISSION`。找不到 → `ENTERPRISE_REVIEW_CONTEXT_NOT_FOUND`。`reviewMaterial` 先走同一条 review，再用 submissionId 换 URL。`material` 走档案 CURRENT 页；没有当前页 → `ENTERPRISE_PROFILE_VERSION_NOT_FOUND`。出门条把 OSS 合同收成 `EnterpriseProfileAccessUrl(accessType, url, expiresAt, fileName)`。

#### 4. 直建与覆盖：先写来源袋，再冻成当前页

`create`：

1. 身份字段经 `EnterpriseIdentityFields.normalize`（信用代码/法人证件号大写，邮箱小写），再验必填、成立日不得晚于今天、营业期限起止顺序；失败 `ENTERPRISE_IDENTITY_INVALID` / `ENTERPRISE_IDENTITY_REQUIRED`。法人证件类型找不到规则 → `ENTERPRISE_DOCUMENT_TYPE_UNAVAILABLE`；号段不配正则 → 还是 `ENTERPRISE_IDENTITY_INVALID`。
2. `beginCreate` insert 档案 `ACTIVE` + 来源 `ADMIN_CREATE`。撞活信用代码唯一键 → `ENTERPRISE_ADMIN_IDENTITY_CONFLICT`。
3. 材料 attach 到 owner `SOURCE`。Bean 校验 `@Size(max = 10)`；合同测试用 11 条材料打失败。Service 循环本身不再另设上限。
4. `validateRequired(..., ALWAYS)`，尺子是法人证件类型码。
5. 若带了 `bindUserId`，先 `requireEligibleUser`（人要存在、启用、没有企业有效钉子、**有个人档案**）。
6. `completeCreate`：第一页 `CURRENT`、来源类型 `ADMIN_CREATE`，更新档案当前页（期望档案 `version=0`），可选 insert 绑定 `ACTIVE`（绑定行 `source_type='ADMIN_CREATE'`），审计 `ADMIN_CREATE`。
7. `snapshotImmutable(SOURCE → VERSION)`，材料从可改袋冻到版本袋。

`revise` **不是**「改一份退回的自助申请」。`IEnterpriseAdminService` 和 Service 注释还写着「已退回」，磁盘是：锁 **ACTIVE** 档案，核对 `expectedVersion`，锁 CURRENT 页，新开 `ADMIN_OVERRIDE` 来源，把当前页 ATTACHED 材料 **clone** 进 SOURCE，校验必传件，把旧页标 `SUPERSEDED`，插入新 CURRENT，更新档案身份列。注销本 `requireWritable` 直接 `ENTERPRISE_PROFILE_REVOKED_READ_ONLY`。版本对不上 → `ENTERPRISE_PROFILE_VERSION_CONFLICT`。改信用代码若撞另一本活档案 → `ENTERPRISE_ADMIN_IDENTITY_CONFLICT`。

#### 5. 审核决定：先掐机器，再改本子

`decide` 只认大写后的 `APPROVED` / `REJECTED`。原因必填且 ≤500。网关是 null → `ENTERPRISE_ADMIN_WORKFLOW_UNAVAILABLE`，**一行都不写**（`beginDecision` 都不会进）。

然后：

1. `lockWaitingApplication`：不是 WAITING 就当没有，后续 `ENTERPRISE_REVIEW_CONTEXT_NOT_FOUND` / 更新 0 行变冲突。
2. 申请打成 `OVERRIDE_PENDING`，`decision_source='ADMIN_OVERRIDE'`，插入 `profile_decision_record` 状态 `PENDING`（`profile_type='ENTERPRISE'`），审计 `ADMIN_DECISION_PENDING`。
3. `workflow.terminate(Long.toString(applicationId), reason)`。抛错 → `ENTERPRISE_ADMIN_WORKFLOW_TERMINATION_FAILED`。E2E 证明：会话回滚后申请仍是 `WAITING:0`，决定记录 0 行。
4. **REJECTED：** `markRejected` 把申请打成 `INVALID`，决定记录 `FINAL`，返回 `EnterpriseAdminResultVo("REJECTED", 0, null, null, decisionVersion)`。不发布档案。
5. **APPROVED：** `resumeWaiting` 把申请拨回 `WAITING`（发布端口只认 WAITING），`requireSubmission`，`publishApproved` 写/更新档案页和绑定并把申请 `FINISH`，材料 `snapshotImmutable(SUBMISSION → VERSION)`，`markApproved`（SQL 要求当前已是 FINISH），决定记录 `FINAL`。返回盒带上 `profileId` / `versionId` / `bindingId`。

发布端口看见账号已有企业有效钉子 → `ENTERPRISE_ACCOUNT_ALREADY_RESPONSIBLE`；执照上已有负责人 → `ENTERPRISE_RESPONSIBLE_ALREADY_BOUND`。那是发布端口自己的篱笆，不是本 Controller 的新方法。转移挑战不走这扇决定窗（L-042）。

单测 `approvalTerminatesBeforePublishingAndFinalizesAdminDecision` 把顺序钉死：beginDecision → terminate → resume → publishApproved → snapshotImmutable → finalizeApproved。terminate 失败则 never publish。

网关接口的 `terminate` **default** 会抛 `UnsupportedOperationException`。生产适配 `SpringEnterpriseWorkflowGateway` 覆盖了它，缺 `WorkflowService` 时抛的是申请侧码 `ENTERPRISE_WORKFLOW_UNAVAILABLE`。管理端 Service 认的失败码是自己的 `ENTERPRISE_ADMIN_WORKFLOW_*`。口试不要把两套码并成一句。

#### 6. 钉子三动作、指定、整本吊销

`manageBinding` 只认 `SUSPEND` / `RESUME` / `UNBIND`。先锁可写档案，再锁「还钉着」的那一行，并核 `expectedBindingVersion`。状态机：

| action | 现在必须是 | 变成 |
| --- | --- | --- |
| SUSPEND | ACTIVE | SUSPENDED |
| RESUME | SUSPENDED | ACTIVE |
| UNBIND | 当前这根钉子的现状（ACTIVE 或 SUSPENDED） | UNBOUND（写 `unbound_time`） |

错状态 → `ENTERPRISE_BINDING_STATE_CONFLICT`。成功写 `profile_enterprise_binding_event`（事件来源在 XML 里写死 `ADMIN_OVERRIDE`，即使钉子行自己是 `ADMIN_CREATE`），审计 `BINDING_*`，能力串 `profile:enterprise:manage`。

`assign`：先 `requireEligibleUser`，再锁档案。档案可写、档案上没有有效钉子、目标人也没有有效钉子，否则 `ENTERPRISE_BINDING_TARGET_INELIGIBLE`。新行 `ACTIVE`、`binding_version=1`，来源 `ADMIN_OVERRIDE`。这是新钉子，不是把 UNBOUND 行改回去。E2E 在把个人档案改成 unverified 之后，同一人 `assign` 会炸这颗码。

`revoke`：核档案 `expectedVersion`，把主体打成 `REVOKED` 并记下原因/时间；若还有有效钉子，按该行当前状态拨到 `UNBOUND` 并记事件。默认 `page` 之后看不见这本；显式 `status=REVOKED` 才见。detail 仍能打开。再 `assign` / `revise` 会 `ENTERPRISE_PROFILE_REVOKED_READ_ONLY`。`del_flag` 仍是 `'0'`——注销不是逻辑删除。

E2E `persistsAdminLifecycleQualifiedOwnerDecisionFenceAndRollback` 把这条生命线跑完：create（带合格负责人）→ revise（两页，CURRENT 来源 OVERRIDE）→ SUSPEND → RESUME → UNBIND → assign（事件累计 5）→ 去掉个人档案后再 assign 失败并回滚 → GET detail 企业名已是修订名、材料名牌在 → GET material 出门条 → revoke → 默认 page 0 行 / REVOKED page 1 行 → 驳回 WAITING 申请成 `INVALID:1:ADMIN_OVERRIDE` → 工作流失败回滚。

## 图、表或文本图

**图 1. 十二扇窗、四件家具、五层楼梯**

```text
浏览器  GET 六扇 / POST 六扇
   │     门牌 /profile/enterprise/archive
   v
EnterpriseAdminController
   │  校验 + R.ok + LoginHelper（只写窗）
   v
EnterpriseAdminUseCaseImpl   @DSTransactional
   v
EnterpriseAdminService
   │
   ├─ page/detail/review ──► DAO 读 Row ──► VO
   ├─ eligibleUsers ───────► UserService + ProfileService（要个人档案）
   ├─ create/revise ──────► 档案 + 来源 + 版本 + 可选绑定
   ├─ decide ─────────────► 申请 WAITING → 掐工作流 → 发布或 INVALID
   ├─ assign/binding ─────► 钉子 + 事件
   └─ revoke ─────────────► 档案 REVOKED + 钉子 UNBOUND
```

- **alt：** 管理端企业档案十二扇 HTTP 窗全部进入同一 UseCase 与同一 Service；读模型停在 DAO，VO 才出门；候选人还要问个人档案端口。
- **caption：** 图 1——OBJ-40 的空间关系。矩阵 A 是 Controller 十二法。
- **文字等价物：** 柜台只负责核对工牌和把纸条装信封。真正改本子的是科员。科员不许自己写 SQL，要喊档案室（DAO）。申请通过时科员还要喊发布端口和材料端口，并先让流水线停机。指定负责人时还要打电话问个人柜台：这人有没有身份证。
- **图的边界：** 图上没有 `/profile/enterprise/application`、没有 `/profile/enterprise/materials`、没有 `/profile/enterprise/transfer`、没有个人柜台、没有前端工厂。不要画 PUT/DELETE。不要把 `IEnterpriseAdminService` 画进生产箭头。

**图 2. 审核盖章停在哪一层；绑定和吊销不是同一支笔**

```text
decide(APPROVED/REJECTED):
  workflow==null ──► WORKFLOW_UNAVAILABLE（零写入）
  锁 WAITING ──► OVERRIDE_PENDING + 决定记录 PENDING
  terminate 失败 ──► TERMINATION_FAILED；事务回滚，仍 WAITING
  REJECTED ──► 申请 INVALID，不发布
  APPROVED ──► 拨回 WAITING → publishApproved → 冻材料 → FINISH
                 账号已有企业钉子 / 执照已有负责人 ──► 发布端口拒绝

binding:
  SUSPEND/RESUME/UNBIND 只动钉子，档案仍 ACTIVE
  assign 必须两端都没有「还钉着」，且目标人有个人档案

revoke:
  档案 ACTIVE → REVOKED（生成列放出信用代码）
  若有钉子 → UNBOUND
  之后只读
  不是转移挑战，不是 HTTP DELETE
```

- **alt：** 管理员决定先停工作流；失败不留下 OVERRIDE_PENDING。解绑与注销分叉。指定负责人要个人档案。
- **caption：** 图 2——审核栅栏与绑定/吊销分叉。
- **文字等价物：** 盖章前先按停传送带。传送带卡死，桌子上的待审件必须回到原位，不能停在「覆盖中」。暂停是贴封条，解绑是拔钉子，注销是盖作废章。作废章会把钉子一起拔掉，反过来拔钉子不会盖作废章。转移验证码是另一扇门，不要把作废章说成「取消转手」。
- **图的边界：** 不画自助 `submit` 怎么把申请推进 WAITING（L-041）。不画材料 attach 树（L-043）。不画 Redis 转移挑战（L-042）。不把个人 `rebindIntent` 画进企业决定链。

## 正例、反例与边界

### 正例

1. **翻名册。** `GET /profile/enterprise/archive?enterpriseName=测试&pageNum=1&pageSize=20`，权限 query。不传 status 时 SQL 排除 REVOKED。返回摘要里可能带 `bindingStatus=SUSPENDED`，那仍算「还钉着」。
2. **打开一本已注销的档案。** `GET /archive/{id}` 仍然包装成功数据；`page` 无 status 时 total=0。E2E 用同一信用代码把这两种 page 对照过。
3. **直建并钉合格负责人。** `POST /admin-create`，body 含 identity、`bindUserId`、reason。成功 `status=ACTIVE`，版本来源 `ADMIN_CREATE`，绑定 `ACTIVE`。材料先落 SOURCE 再冻进 VERSION。目标人当时必须有个人档案。
4. **覆盖执照页。** `POST /{id}/revision`，带上 detail 里看到的档案 `version` 当 `expectedVersion`。旧页 SUPERSEDED，新页 CURRENT / `ADMIN_OVERRIDE`。返回盒的 `version` 是档案乐观锁 +1，不是 `version_no`。
5. **暂停再恢复再解绑再指定。** 每次 `manageBinding` 要把上一枪返回的 `version` 当作下一枪 `expectedBindingVersion`。UNBIND 后档案仍 ACTIVE，才能 `assign` 同一人。E2E 事件表 5 行：create 时 1 次 ACTIVE + suspend + resume + unbind + assign。
6. **驳回。** 申请 WAITING，`POST .../decision` `{decision:"REJECTED", reason:"..."}`。工作流 terminate 成功后申请 `INVALID:1:ADMIN_OVERRIDE`，决定记录 `FINAL`，结果盒 `profileId=0`。
7. **出门条分权。** 审核员用 review 权限拿申请袋 URL；档案材料员用 material 权限拿 CURRENT 页 URL。query 只能在 detail 里看见文件名。
8. **候选人。** override 权限 + 非空 keyword。已有 ACTIVE/SUSPENDED 企业钉子的账号不会出现；没有个人档案的也不会出现。空关键字得到 `[]`，不是全库扫描。

### 反例

1. **把 `POST /revoke` 说成解绑，或把 `UNBIND` 说成注销。** 前者改档案状态；后者只改钉子。
2. **把 `POST /revoke` 说成撤销转移挑战。** 那是 `IEnterpriseAdminService` 的假注释；转移在 L-042。
3. **给 manageBinding 传 `ASSIGN`。** 集合只有三动作 → `ENTERPRISE_BINDING_ACTION_INVALID`。指定走 `/assign`。
4. **SUSPEND 一根已经 SUSPENDED 的钉子。** `ENTERPRISE_BINDING_STATE_CONFLICT`。RESUME 同理，不能从 ACTIVE 恢复。
5. **decide 传 `RETURN` / `BACK`。** 只有 APPROVED/REJECTED。
6. **工作流挂了还指望驳回已经落库。** terminate 抛错则整单回滚。单测/E2E 都断言不 publish、不 finalize。
7. **给一个没有个人档案的启用账号直建/指定。** `ENTERPRISE_BINDING_TARGET_INELIGIBLE`。候选人名单里也不会出现这个人。
8. **默认名册里找刚注销的企业，以为丢了。** 加 `status=REVOKED`。
9. **拿 query 权限打 `/eligible-users` 或 `/revoke`。** 合同测试把这两扇钉在 override。
10. **从请求体读 operatorId。** Controller 没有这个字段；Service 正数校验的是登录票。
11. **把测试夹具 `EnterpriseAdminServiceImpl` 说成生产分层。** 生产 Controller 接 UseCaseImpl；夹具为了单测直接持 Mapper，还实现了无操作者重载去叫 `LoginHelper`。
12. **把 `SysProfileController.updateProfile` 或 `PersonAdminController` 说成本课。** 前者是账号资料页，后者是个人柜台。
13. **口述 classic：Controller 注入 `IEnterpriseAdminService`。** 磁盘上这份接口没有任何生产实现，也没有任何 import。
14. **把法人证件正则套到统一社会信用代码上。** 信用代码是身份键；正则打在 `legalDocumentNumber`。

## 变式与迁移

### 变式

- **直建不带 bindUserId。** 档案 ACTIVE，结果盒 `bindingId=null`。之后用 assign 补钉子，或等人走自助申请发布（L-041）——本课不负责自助那条。`bindUserId` 在 BO 上是 `@Positive Long`，可以缺省；给 `0` 会被 Bean 校验挡掉。
- **revise 时材料。** 不在 body 里重传文件；Service 把 CURRENT 页 ATTACHED 行 clone 成 SOURCE，再 `validateRequired`。缺必传件会在冻版前失败。
- **pageSize=0 或负数。** 当 20。超过 200 截成 200。status 乱写直接失败，不会当空。
- **信用代码大小写。** Service `upper` 后再精确匹配；企业名称才是 like。生成列也是 `upper(trim(...))`。
- **同一信用代码注销后再直建。** `active_credit_code` 在 REVOKED 时变 NULL，唯一键放开。活着的两本不能同键。发布端口新建档案时还可能把 `previous_profile_id` 指到最近一本已注销的；管理员 `insertProfile` 把这一列写成 null。
- **Handler 形态。** `EnterpriseAdminException` 变成 `R.fail(category)`，类别码当 msg。不是 404 数字码那套 OpenAPI 风格。校验失败走框架 `@Valid`，不进这个 Handler。
- **个人对照（只作篱笆）。** L-034 的 Person 柜台形状相近，但表、权限前缀、换绑意图、候选人资格都不是本课对象。enterprise 的 XML/Java 不得出现 `profile_person_` 作为持久化依赖。个人候选人不必先有企业档案；企业候选人必须先有个人档案。
- **材料 11 条。** Bean 校验先炸；不要指望 Service 再给你一颗业务码。

### 迁移

当前是 Target 形状，不是 classic 遗留房间。口试仍要能指出**还放在抽屉里、不能当主链讲**的几件旧家具：

| Current | Target / 口试说法 | 证据 |
| --- | --- | --- |
| `IEnterpriseAdminService` 仍在 `service/`，Javadoc 把 revoke 写成「撤销转移挑战或档案绑定」，把 revise 写成「修改已退回档案申请」 | 删除条件见 EX-002：测试夹具迁完再删。Controller/UseCase **不得**注入它 | 全仓只有自己这一处引用 |
| `EnterpriseAdminService` 兼容构造仍接 `WorkflowService` | 生产主构造接 `EnterpriseWorkflowGateway`；新测试走端口 | EX-002；架构测试禁 `service/impl` 生产目录 |
| `src/test/.../EnterpriseAdminServiceImpl` 同时当 UseCase 和 Mapper 持有者 | 夹具允许；E2E 甚至 `new EnterpriseAdminController(service)` 绕过 UseCaseImpl | 不要画进模块地图主箭头 |
| UseCase 无 operatorId 的 default | 全部 `throw new UnsupportedOperationException("请传入 operatorId")` | 新入口必须显式操作者 |
| 部分注释写「修改已退回申请」/「撤销账号绑定」 | 磁盘 `revise` 覆盖 **ACTIVE** 档案当前页；`revoke` 改档案 `REVOKED` | 以 XML `status='ACTIVE'` / `revokeProfile` 为准 |
| 读方法也 `@DSTransactional` | 这是当前磁盘，不是要求你改；新代码沿用 UseCase 事务边界，不在 Controller 开事务 | UseCaseImpl 十二个方法 |
| 绑定事件 XML 写死 `ADMIN_OVERRIDE` | 直建钉子行可以是 `ADMIN_CREATE`；事件表仍盖覆盖章 | `insertBindingEvent` |
| API-005 存量 PUT | 本切片已经全 GET/POST | 合同测试 |

Ratchet：新窗继续 layered；不要为了「看起来像 system 用户 CRUD」加 export/delete，也不要把 Mapper 送回 Service。不要为了「企业也能换绑」把个人 `rebindIntent` 抄进 `publishApproved`。

## 常见误区

1. **「这是资料页。」** 资料页在 system。本课是企业实名档案。
2. **「layered 所以 Controller 调 Service。」** Controller 调 UseCase。字段名叫 `service` 是陷阱。
3. **「UseCase 只包写操作。」** 磁盘上 page 也有 `@DSTransactional`。
4. **「生效=状态字 EFFECTIVE。」** 有效钉子是 ACTIVE 或 SUSPENDED 的生成列。
5. **「注销=逻辑删除。」** `revokeProfile` 改 `status='REVOKED'`，`del_flag` 仍是 `'0'`。生成列靠 status 放行信用代码。
6. **「detail 能看材料就等于能下文件。」** list 名牌 ≠ access-url。
7. **「审核材料和管理端材料库是同一扇窗。」** 本课两条 URL 写死 SUBMISSION / VERSION。`EnterpriseMaterialAdminController` 按 ownerType 通用列举，L-043。
8. **「通过就是 update 申请=FINISH。」** 先停工作流，再发布端口造页，再冻材料。
9. **「候选人挂 query，因为只是查询。」** 合同测试钉 override。
10. **「`EnterpriseAdminResultVo.version` 都是档案版本。」** 三把尺。
11. **「person 可以 reuse 这张 Mapper。」** 子域隔离：实现、表、实体互不 import。
12. **「前端工厂也是本课格子。」** web-domain enterprise 留给 L-044；本课只要求你能听懂门牌，不认前端符号。
13. **「IEnterpriseAdminService 的方法注释才是合同。」** 以 Controller + Service 实现 + XML 为准。
14. **「没有工作流 Bean 时 decide 会降级成直接改申请。」** `workflow==null` 直接失败关闭。
15. **「E2E 绕过 UseCase，所以生产也没有 UseCase。」** 生产 Bean 是 UseCaseImpl。E2E 为了同一条 SQL 会话才直接 new Controller。
16. **「企业负责人和个人档案无关。」** 磁盘强制有个人档案。E2E 测试名带 `QualifiedOwner`。
17. **「OBJ-40 没写吊销，所以没有 revoke。」** 矩阵格子是十二法，包含 revoke。口述重点是档案/审核/绑定三分，吊销是档案生命周期的关闭闸，不是另一扇门牌。

## 非评分暂停

打开磁盘，不要凭记忆，也不要交卷。下列动作用来把门钉住，没有标准答案栏。

1. 打开 `EnterpriseAdminController`。把十二对 mapping 抄成一张表：路径、动词、Java 名、权限串、返回类型、有没有 `@Log`。圈出：GET 六、POST 六；manage 只出现一次；`eligibleUsers` 不是 query；`reviewContext` 调的是 `service.review`。
2. 用手指划 `decide` → `LoginHelper.getUserId()` → `UseCaseImpl.decide`（`@DSTransactional`）→ `EnterpriseAdminService.decide` → `beginDecision` → `workflow.terminate` → 分叉 `finalizeRejected` / `resumeForApproval + publishApproved`。在 terminate 旁边写：失败时申请不该停在 OVERRIDE_PENDING。
3. 打开 `EnterpriseAdminMapper.xml` 的 `lockEffectiveBinding`、`countEffectiveBindingByUser`、`revokeProfile`、`updateBinding`。抄下 status 集合。对照实体生成列 `activeCreditCode` / `effectiveUserId`。再看 `insertBindingEvent` 里写死的 `'ADMIN_OVERRIDE'`。
4. 打开 `EnterpriseAdminHttpContractTest`。确认没有 `export`/`delete`，六扇 POST 的 `isSaveRequestData`/`isSaveResponseData` 都是 false，`assign` 的 title 是「管理员指定企业档案负责人」。
5. 打开 `EnterpriseModuleArchitectureTest` 的 `enforcesTheFiveLayerDependencyDirection`。对照 Controller 是否 import service、UseCase 是否 import dao、Service 是否 import mapper。再看测试目录 `EnterpriseAdminServiceImpl` 为什么允许破例。最后打开 `eligibleUsersRequireAnActivePersonProfileAndNoEnterpriseBinding`，把「必须先有个人档案」写在名册旁边。

## 总结、词汇表与下一步

- **十二扇窗。** 前缀 `/profile/enterprise/archive`。GET 六扇只读（名册 / 候选人 / 详情 / 审核上下文 / 两张出门条）；POST 六扇变更（直建 / 覆盖 / 指定 / 处置钉子 / 整本注销 / 决定）。layered：Controller → UseCase → Service → DAO → XML。
- **四件家具。** 档案 ACTIVE/REVOKED；页 CURRENT/SUPERSEDED；钉子 ACTIVE/SUSPENDED/UNBOUND；申请 WAITING 才能盖章。身份键是活着的统一社会信用代码。
- **五颗权限。** query / review / material / manage / override。不要并成一个「管理员」。
- **审核栅栏。** 无网关不写；先 OVERRIDE_PENDING；terminate 失败回滚；驳回 INVALID 不发布；通过必须拨回 WAITING 再走发布端口。
- **绑定 ≠ 吊销 ≠ 转移。** 处置钉子走 manage；整本作废走 override 的 revoke，并顺手拔钉子；转手走另一扇门。
- **合格负责人。** 启用账号 + 无企业有效钉子 + 已有个人档案。
- **格子按磁盘全表：** 不要把自助 `/application`、材料总库、转移、个人柜台、前端工厂补进来。不要把 `IEnterpriseAdminService` 说成 HTTP 依赖。不要把 EFFECTIVE 说成状态字。

词汇表：EnterpriseAdminController / layered / EnterpriseAdminUseCaseImpl / `@DSTransactional` / EnterpriseAdminDao / `profile_enterprise` / `active_credit_code` / CURRENT / SUPERSEDED / effective binding / `binding_version` / OVERRIDE_PENDING / `EnterpriseAdminResultVo` / `EnterpriseProfileAccessUrl` / `profile:enterprise:override` / `LoginHelper.getUserId()` / `EnterpriseWorkflowGateway.terminate` / `publishApproved` / REVOKED / qualified owner / `ProfileService`。

下一步：L-041 走进自助门 `EnterpriseApplicationController` 的 current/save/submit/probe，以及核身匿名回调——那才是 WAITING 申请怎么被本人推进待审桌。L-042 转移挑战、L-043 材料总库、L-044 才把 web-domain enterprise 接到这十二扇窗。对照时带着 L-034 的宏观口吻，换表名、换权限前缀、换「必须先有个人档案」这道篱笆，不要复制个人柜台的 `rebindIntent`。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-004 | `backend/wta-modules/**` Controller / Service | profile-enterprise 房间内公开入口；本课 Controller 同模块 | 模块树 | 2026-09-16 |
| S-008 | `.agents/skills/engineering-standards/references/project/03-backend-module-modes.md` | `wta-profile` 登记为 layered；禁止与 classic 混用 | 当前登记表 | 2026-09-16 |
| S-010 | `release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql`、`50-cde-base-dml.sql` | 表 `profile_enterprise*` 生成列与唯一键；菜单种子 `profile:enterprise:query/review/material/manage/override` | DDL 企业档案段 / DML 210050…0020–0025 | 2026-09-16 |
| S-015 | `backend/wta-api/.../{system,profile,workflow}/api` | `UserService` / `ProfileService` / `WorkflowService` 跨模块合同；候选人要问个人档案 | API 面 | 2026-09-16 |
| S-L003-01 | `children/2026-09-14-namewta-architecture/lessons/L-003-layered-vs-classic.md` | 五层楼梯与登记表；profile 是试点房间 | OBJ-03 前驱 | 2026-09-16 |
| S-L034-01 | `lessons/L-034-person-admin.md` | 个人柜台同构对照；本课换表名、换合格负责人篱笆 | OBJ-34 前驱 | 2026-09-16 |
| S-L040-01 | `EnterpriseAdminController.java`、`EnterpriseAdminExceptionHandler.java` | 十二扇映射、权限、POST 写窗、`LoginHelper`、`R.fail(category)`、无 PUT/DELETE | 类与各方法 | 2026-09-16 |
| S-L040-02 | `EnterpriseAdminUseCase.java`、`EnterpriseAdminUseCaseImpl.java` | 有操作者的正式签名；无操作者 default 抛错；十二方法均 `@DSTransactional`；只调 `EnterpriseAdminService` | usecase 包 | 2026-09-16 |
| S-L040-03 | `EnterpriseAdminService.java` | 决策顺序、直建/覆盖、绑定状态机、吊销、候选人过滤、身份校验、端口协作、`requireEligibleUser` | `decide` / `create` / `revise` / `manageBindingData` / `revokeData` / `eligibleUsers` | 2026-09-16 |
| S-L040-04 | `EnterpriseAdminDao.java`、`EnterpriseAdminMapper.java`、`EnterpriseAdminMapper.xml` | `FOR UPDATE`；WAITING 锁；有效钉子集合；revoke 改 status 不 del；clone 材料；事件来源写死 | XML 各 id | 2026-09-16 |
| S-L040-05 | `ProfileEnterprise.java`、`ProfileEnterpriseBinding.java` 与 domain bo/vo | 生成列 NEVER；`EnterpriseAdminResultVo` 五字段；出门条 record；材料 `@Size(max=10)` | 实体与 record | 2026-09-16 |
| S-L040-06 | `EnterpriseAdminHttpContractTest.java`、`EnterpriseModuleArchitectureTest.java`、`EnterpriseBeanValidationContractTest.java` | 十二扇权限与安全 `@Log`；无 export/delete；五层 import 方向；admin 无 `@SaIgnore`；命令 Bean 校验 | 测试方法名 | 2026-09-16 |
| S-L040-07 | `EnterpriseAdminServiceTest.java`、`EnterpriseAdminMySqlE2ETest.java`、`EnterpriseAdminServiceImpl.java`（test） | terminate 在 publish 前；失败关闭；合格负责人；E2E 生命线；夹具不是生产主链 | 测试目录 `service/impl` | 2026-09-16 |
| S-L040-08 | `EnterpriseApplicationPublicationPort.java`、`EnterpriseApplicationService.publishApproved`、`SpringEnterpriseWorkflowGateway.java`、`.agents/skills/wta-module-guide/references/modules/profile/layered-boundaries.md`、`02-decisions-and-exceptions.md` EX-002 | 发布只认 WAITING；网关适配；子域隔离；兼容构造例外 | 端口 / 决策例外 | 2026-09-16 |
