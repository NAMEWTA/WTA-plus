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

当前工作树的 `frontend/packages/api-contracts/openapi/current.json` 不包含任何 `/profile/**` 路径，`generated/openapi.ts` 也没有 Profile 传输类型。这是已确认的暂态事实：Profile domain 的类型化 HTTP service 可以暂时在资源边界维护 URL/方法和领域映射，并用合同测试固定；不得伪造 OpenAPI 类型，也不得让页面直接依赖 generated 文件。

后端 OpenAPI 快照纳入 Profile 后，按 `tooling/openapi` 的 `openapi:fetch`、`openapi:generate`、`openapi:check` 流程更新快照和生成结果，禁止手工编辑 `generated/openapi.ts`。domain 仍需把生成 transport 映射成自有模型，Web 只依赖 domain 公开合同；迁移完成后删除上述暂态例外。

## 变更护栏

新增能力按垂直切片完成 DAO 查询、Service 规则、UseCase 编排、入口切换和行为回归。涉及数据库的测试必须能证明完整 `Entry -> UseCase -> Service -> DAO -> Mapper -> XML` 路径；不要把 Profile 的 pilot 约束复制回本轮冻结的 system/workflow。
