# wta-profile 模块索引

`wta-profile` 是新增模块的五层架构试点，聚合 `wta-profile-person`、`wta-profile-enterprise` 和 BOM。本文只索引 Profile 的业务事实；五层依赖、目录命名、中文 Javadoc 和质量门禁由通用 Skill 统一裁决。

## 何时读取

| 任务 | 继续读取 |
|---|---|
| person/enterprise 能力、表和公共合同 | [capability-map.md](capability-map.md) |
| Profile 特有的子域隔离、外部端口和读模型注意事项 | [layered-boundaries.md](layered-boundaries.md) |
| System 用户、OSS、消息或配置接入 | [../system/how-other-modules-call.md](../system/how-other-modules-call.md)、[../system/domains.md](../system/domains.md) |
| Workflow 启动、状态回写或事件订阅 | [../workflow/integration-guide.md](../workflow/integration-guide.md) |

## 模块事实

- person 负责个人申请、认证、材料、档案投影、重新绑定和通知。
- enterprise 负责企业申请、认证、材料、档案投影和转移。
- enterprise 转移只能通过 `wta-api` 的 `PersonIdentityLookupService` 查个人精确匹配，不读取 person 实现或数据库。
- 对外合同位于 `wta-api`，实现细节可以重构，但方法、批量语义、锁内复核、脱敏和错误语义必须保持兼容。
- Profile POM 只依赖 `wta-api` 和必要 common 模块；不依赖 `wta-system` 或 `wta-workflow` 实现模块。

## 入口与适配

管理端、自服务和匿名回调分别放在 `controller/admin`、`controller/self`、`controller/anonymous`。Listener 和公共 API Adapter 也是入口，必须把事件/合同转换后委托 UseCase；不要在适配器中直接调用 Service、DAO 或 Mapper。

Profile 中的 `Gateway`、`Provider`、`Store` 是外部端口类型：合同放在能力的 `port/`，Spring/Redis/远程实现放在 `adapter/`，只有所属 Service 可以调用这些端口。工作流使用 `org.namewta.workflow.api` 合同，System 使用 `org.namewta.system.api` 和 common SPI。

## 前端合同与 OpenAPI 状态

Profile 后端 Controller 与前端资源的当前映射如下。`web-domain` 是页面 owner，不要求与每一个无页面的 Controller 一一对应：

| 后端 base path | domain 公开资源 | Web owner |
|---|---|---|
| `/profile/material-tags` | `@namewta/domain-profile/material-tags` | `@namewta/web-domain-profile/material-tag` |
| `/profile/person/application` | `person/application` | `person` |
| `/profile/person/rebind` | `person/rebind` | `person` |
| `/profile/person/materials` | `person/materials` | `person` |
| `/profile/person/archive` | `person/archive` | `person` |
| `/profile/enterprise/application` | `enterprise/application` | `enterprise` |
| `/profile/enterprise/transfer` | `enterprise/transfer` | `enterprise` |
| `/profile/enterprise/materials` | `enterprise/materials` | `enterprise` |
| `/profile/enterprise/archive` | `enterprise/archive` | `enterprise` |

`material-tags`（领域合同、后端路径）与 `material-tag`（页面 owner、组件键 `profile/materialTag/index`）是有意的复数/单数别名，不能随意改名。匿名验证回调是后端入口，没有前端页面时不创建空资源目录。

自助材料页通过 `GET /profile/material-tags/requirements` 读取数据库中按 profileType、documentTypeCode 和 handlerIsLegalRepresentative 选择的必填规则。该查询只用于提示；提交时仍由服务端按已保存申请独立校验。上传先保存草稿取得 WORKING owner，再登记 OSS 引用；预览与移除都经过 owner 权限校验，移除引用不物理删除 OSS 对象。`current` 查询只返回 DRAFT/BACK/CANCEL/WAITING，已完成申请不作为可编辑 current 返回。

当前 `frontend/packages/api-contracts/openapi/current.json` 是不可变快照版本的指针；必须读取对应 `openapi/revisions/<revision>/source.json`，不能把指针本身误当路径清单。当前版本已包含 50 条 `/profile/**` 路径，`generated/openapi.ts` 也有 Profile 传输类型。

企业转移资源通过 generated `EnterpriseTransferSendBo`、`EnterpriseTransferConfirmBo`、`EnterpriseTransferVo` 映射为 domain 自有状态合同；其他存量 Profile 资源的映射以源码为准，不再以“快照不存在”解释独立类型。变更命中的资源应同步校核生成 transport 与 domain 模型；Web 只依赖 domain 公开合同。

更新快照与生成结果使用 `tooling/openapi` 的 `openapi:fetch`、`openapi:generate`、`openapi:check`，禁止手工编辑 `generated/openapi.ts`。来源若是已有全量快照加实际 Java schema 导出，provenance 必须明确各自来源及未提交工作树，不能声称是当前完整 HTTP 文档采集。

## 变更护栏

新增能力按垂直切片完成 DAO 查询、Service 规则、UseCase 编排、入口切换和行为回归。涉及数据库的测试必须能证明完整 `Entry -> UseCase -> Service -> DAO -> Mapper -> XML` 路径；不要把 Profile 的 pilot 约束复制回本轮冻结的 system/workflow。
