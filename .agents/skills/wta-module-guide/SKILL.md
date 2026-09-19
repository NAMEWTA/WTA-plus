---
name: wta-module-guide
description: 新增模块、跨模块接入，或查询 Profile/System/Workflow/Notify/Third 模块事实时使用。
---

# WTA 模块导航

本 Skill 是模块事实和接入面路由器，不是第二套架构规范。处理新增模块、跨模块 API、Profile 业务、System 能力或 Workflow 审批时，先读取本入口，再按目标模块加载一个最小 reference。若模块没有专用 reference，则从[模块地图](../engineering-standards/references/project/01-module-map.md)定位源码，并读取该路径适用的 `AGENTS.md`（没有模块手册时继承最近父目录），再回到通用全栈 Skill。

## 使用顺序

1. 确认任务目标模块和运行面（后端、前端或跨模块）。
2. 读取对应 `references/modules/<module>/index.md`。
3. 只读取 index 按当前问题指向的 capability、integration 或 domain reference；条目不清楚时直接按路径回读源码。
4. 架构、目录、命名、注释、框架和质量门禁以 `engineering-standards` 或 `namewta-fullstack-development` 为准；本 Skill 不复制这些通用规则。

## 模块路由

| 模块 | 当前定位 | 首读入口 |
|---|---|---|
| `wta-profile` | 新模块五层架构试点；person/enterprise 资料、认证、材料、绑定和转移 | [`profile/index.md`](references/modules/profile/index.md) |
| `wta-system` | 存量基础模块；本轮保持 classic 目录和实现不变，只描述稳定 API 与内部边界 | [`system/index.md`](references/modules/system/index.md) |
| `wta-workflow` | 存量 Warm-Flow 模块；本轮保持内部实现不变，只通过 `wta-api` 提供审批接入 | [`workflow/index.md`](references/modules/workflow/index.md) |
| `wta-third` | 新增分层的第三方 HTTP Provider/Endpoint 聚合、凭据、Gateway、SPI、限流与出站观测模块 | [`third/index.md`](references/modules/third/index.md) |
| `wta-notify` | 统一通知意图、收件箱、公告、Outbox、渠道投递和供应商回调 | [`notify/index.md`](references/modules/notify/index.md) |

新模块不能因为没有专用地图而复制 `wta-system`/`wta-workflow` 的内部实现。先通过适用的 `AGENTS.md`、模块地图和实际 POM/package 确认职责、组成、入口和本地验证命令；跨模块能力只能使用已公开的 API 或 common SPI。

## 新模块与存量模块

- 新增模块或新能力默认采用 `Controller -> UseCase -> Service -> DAO -> Mapper -> XML`；辅助的 listener、event、gateway、provider、store、domain policy 不得绕过主链路。完整的层级和 `IService` 使用边界见通用后端规则。
- `wta-profile` 是当前参考实现；继续保持 person 与 enterprise 的实现、Mapper、Entity、domain 和表隔离，跨子域查询使用公开身份合同。
- `wta-system`、`wta-workflow`、`wta-job`、`wta-demo`、`wta-ai` 等存量模块本轮不做架构迁移。修改存量实现时先遵守其本地事实和兼容合同，不把 classic 代码复制成新模块模板。
- 新模块需要 System 能力时注入 `wta-api`/common SPI；需要 Workflow 时只使用 `org.namewta.workflow.api.WorkflowService` 和事件合同，并优先在 `adapter/gateway` 封装为本模块 Port，Service 只注入该 Port；禁止依赖实现模块 POM。

## 公开边界

模块 reference 只记录当前工作树已核实的合同、路径、行为和例外。它不创造远程客户端、租户模型、表单引擎或未在源码中确认的 API。涉及旧模块的内部 `I*Service`、Mapper、Entity、Controller 或 Warm-Flow 类型时，除 `wta-admin` 组装例外外，均视为非稳定实现面。

## AGENTS.md 协同

模块沿用适用的父目录 `AGENTS.md`；只有独有职责、入口、依赖边界或验证要求需要就地说明时，才保留或新增模块手册。不因存在 Maven `pom.xml` 或前端 `package.json` 复制通用手册。聚合导航与模块地图负责定位源码，细节仍由源码和本 Skill references 提供。嵌套文件遵循最近目录优先；收敛手册时必须先承接独有硬约束并校验引用，不在多个 Skill 中重复维护同一事实。

## 按需索引

- Profile：person/enterprise 能力、公开合同和五层试点例外 → [`profile/index.md`](references/modules/profile/index.md)
- System：`wta-api`、common SPI、HTTP 管理面和存量内部服务 → [`system/index.md`](references/modules/system/index.md)
- Workflow：`WorkflowService`、事件、businessId、待办 REST 和请假样例 → [`workflow/index.md`](references/modules/workflow/index.md)
- Notify：`NotificationApplicationService`、公告 UseCase、Inbox、Outbox、回调验签和推送票据 → [`notify/index.md`](references/modules/notify/index.md)
