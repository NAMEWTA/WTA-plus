# 后端模块模式登记表

本表是后端业务模块采用 `layered` 或 `classic` 的唯一登记入口。模块路径、POM 和源码证据发生变化时，先更新本表，再更新实现导航；不得在业务文档中另写一份模式名单。

## 模式定义

| 模式 | 适用范围 | 强制调用链 | 允许的持久化形态 |
|---|---|---|---|
| `layered` | 新增业务模块，以及已明确迁移到五层的模块 | `Controller/Listener/API Adapter -> UseCase -> Service -> DAO -> Mapper -> Mapper XML` | DAO 是业务持久化唯一入口；Service/UseCase 不得导入 MyBatis、Mapper、`IService` 或 `ServiceImpl` |
| `classic` | 登记的存量兼容模块 | `Controller -> Service -> ServiceImpl -> Mapper -> Mapper XML` | ServiceImpl 可以直接持有 Mapper；不得因新增功能继续扩大 classic 的越层依赖 |

未登记的后端业务模块默认使用 `layered`。`wta-common-*` 是基础设施能力模块，不按业务五层强行套目录；新增 common 能力必须遵守 common 的 SPI、依赖和工具入口规则。

## 当前登记

| 模式 | 模块路径 | 说明 | 迁移策略 |
|---|---|---|---|
| `layered` | `wta-modules/wta-profile` | 新增 Profile 业务试点，person/enterprise 使用五层 | 作为新模块参考实现；新增能力必须保持五层和中文 Javadoc |
| `layered` | `wta-modules/wta-notify` | 统一通知控制面，含公告、收件箱、Outbox 和通知配置 | 新增能力必须保持五层；邮件/短信账号运行时以数据库为准 |
| `layered` | `wta-modules/wta-sso` | 第一方 SSO Authorization Code + PKCE | 新增能力必须保持五层；仅经 wta-api 读取用户与 Client |
| `layered` | `wta-modules/wta-third` | 第三方 Provider/Endpoint 控制面及 Gateway；源码为 controller -> usecase -> service -> dao -> mapper | 保持当前五层，SPI/HTTP适配器归模块owner |
| `classic` | `wta-modules/wta-system` | 既有用户、组织、权限、资源和监控能力 | 保持现状；只在触及文件按 Ratchet 收紧，不发动无关重构 |
| `classic` | `wta-modules/wta-workflow` | 既有 Warm-Flow 流程能力 | 保持现状；通过公开 Workflow API 接入，不改内部层次 |
| `classic` | `wta-modules/wta-job` | 既有任务业务能力 | 保持现状；新增独立业务能力需另行登记为 layered |
| `classic` | `wta-modules/wta-demo` | 示例和集成演示 | 保持现有示例可运行；不得作为新模块 layered 反例 |
| `classic` | `wta-modules/wta-ai` | 既有 AI 业务能力 | 保持现状；第三方 starter 适配遵守模块边界 |

`wta-admin`、`wta-api`、`wta-extend`、聚合 POM 和 `wta-common-*` 不属于业务五层登记范围，按各自模块职责和依赖规则执行。若将其中某个模块改造成业务实现，必须先补充本表条目和迁移说明。

## 新模块登记条件

新增 `wta-modules/<module>` 或新的业务子模块时，变更说明必须同时给出：

1. 唯一业务 owner、artifactId、Java package 和数据库 owner；
2. `layered` 目录主轴及入口访问面；
3. UseCase、Service、DAO、Mapper 的依赖方向和完整 SQL 验证路径；
4. 使用的 `wta-api`、`wta-common-*` 和框架公共入口；
5. 测试、bundle、配置、50/60 MySQL 基座和前端合同影响。

没有上述证据时不得以 `classic` 作为临时默认。若确有存量兼容原因需要保留 classic，必须登记模块、owner、例外理由、补偿验证和删除条件。

## 例外与迁移

- `classic` 只保护存量兼容，不授权新建 `service/impl`、直接暴露 Mapper 或增加新的越层依赖。
- `layered` 允许局部 `support`、`policy`、`codec`、`converter`、listener 和 event 作为辅助，但它们不能绕过 UseCase 或持有数据库/远程基础设施。
- 从 classic 迁移 layered 时，先建立旧路径到新 owner、路由、调用方、Mapper/XML、测试和 SQL 的映射；“仅移动”和“行为变化”分开验证。
- 模式变化属于架构合同变化，必须同步更新模块 `AGENTS.md`、本表、工程模块地图、对应 Skill 和验证脚本。

## 验证

- layered 模块：`node .agents/skills/namewta-fullstack-development/scripts/validate-module-mode.mjs <module-path> --mode layered`，再运行受影响 Maven 测试和构建。
- classic 模块：确认没有新增 layered 强制目录；按已有 ServiceImpl、Mapper/XML、权限和事务合同验证。
- 任何模块：交付报告列出实际命令、工作目录、退出码、跳过原因和未验证风险。
