# 架构、目录与依赖边界

### ARCH-001 Monorepo 源码所有权

Scope: `repository`, `module:frontend`, `module:backend`

Level: MUST

Source: `repository-fact` (`README.md`, `backend/pom.xml`, `frontend/package.json`, Git tracked tree)

Rule: 前后端源码由同一 monorepo 的 `backend/` 与 `frontend/` 拥有，不以 Git submodule 指针交付。变更在所属目录实现和验证，形成聚焦逻辑主题的提交；跨端合同按后端合同与前端消费者顺序同步。不得复制一套平行源码或跨目录建立隐式构建依赖。

Verification: 从 monorepo 根检查 `git status` 与实际 diff；核对前后端源码由普通 Git 文件跟踪而非 gitlink，并在对应目录运行门禁。

### ARCH-002 后端依赖方向

Scope: `module:backend`

Level: MUST

Source: `repository-fact` (root and child `pom.xml`)

Rule: 可部署应用负责组装，业务模块依赖 `wta-api` 与最小 `wta-common-*` 能力，`wta-api` 只依赖稳定基础合同，common 不反向依赖业务模块。跨模块使用公开类型/服务合同，不深用对方 implementation、mapper 或内部实体。

Verification: `./mvnw clean package`; review 变更模块的 `pom.xml` 与 import；新增依赖说明方向、必要性和替代方案。

### ARCH-002A 新业务模块五层调用链

Scope: `path:backend/wta-modules/**`

Level: MUST

Source: `user-decision` + `repository-fact` (`wta-profile` layered implementation)

Rule: 新增后端业务模块必须使用 `Controller/Listener/API Adapter -> UseCase -> Service -> DAO -> Mapper -> Mapper XML` 调用链。入口只能依赖 UseCase 合同；UseCase 只能编排 Service；Service 只能调用本模块自己的 DAO、明确声明的外部 Port 及无状态 domain policy/codec/converter；DAO 只能调用本 owner 的 Mapper；Mapper 只负责 SQL。UseCase 不得直连 DAO、Mapper、Gateway、Store、Provider 或外部 API；Service 不得调用另一个 Service，不得导入 `PageQuery`、MyBatis/Mapper 或继承/实现 `IService`、`ServiceImpl`；DAO 不得调用 DAO、UseCase、Service、Gateway、Store、Provider、跨模块 API，也不得继承/实现 `IService`、`ServiceImpl`；API Adapter/Listener 同样不得直连 DAO/Mapper。事务命令和锁查询由 UseCase 经 Service 在事务内调用。Listener 和 API Adapter 是入口适配器，不能绕过 UseCase。support、domain policy、codec、converter 只能辅助主链路；support 不得持有 Spring Bean、数据库或远程调用。

数据库用例必须能验证完整 Entry -> UseCase -> Service -> DAO -> Mapper -> XML 链路。`domain/vo` 只承载 HTTP 输出；Mapper 读结果使用 `domain/model/read` 下的 `<Capability>Row` 或 `<Capability>Projection`，Row/Projection 不是第六层，也不得作为 Controller 返回值。

存量模块按[后端模块模式登记表](../project/03-backend-module-modes.md)维持已登记的 classic 形态；不得借新规则发动 `wta-system`、`wta-workflow`、`wta-job`、`wta-demo` 或 `wta-ai` 的无关迁移。新文件和实质修改文件遵循 Target，存量偏差按 Ratchet 处理。

Verification: 对 layered 模块运行 `validate-module-mode.mjs`，检查入口、UseCase、Service、DAO、Mapper 的 import 方向和五层存在性，并检查 API Adapter/Listener、port/adapter/support/domain 边界、事务注解位置、Service 同层互调、MyBatis 类型和 Row 命名；对每个数据库用例追踪至 XML statement，并运行受影响 Maven 测试/构建。

### ARCH-003 前端边界

Scope: `module:frontend`

Level: MUST

Source: `repository-fact` (`apps/**`, `packages/domains/**`, `packages/web-domains/**`, `packages/platform/**`, `packages/adapters/**`, `tooling/architecture/**`)

Rule: App 拥有终端 Client、布局、路由装配和部署；domain 拥有无界面模型/服务并只依赖抽象端口；web-domain 拥有 Vue 表现但不拥有 App 全局单例；adapter 实现平台端口。工作区内部只经公开 exports 导入，禁止 App 互相导入、包深层导入、跨包相对导入和通过 alias 绕过依赖方向。

Verification: `pnpm architecture:check`; `pnpm architecture:test`; `pnpm lint`; `pnpm typecheck`; `pnpm build:prod`; review App composition、import graph、runtime port 与状态所有权。

### ARCH-004 Public contract 与跨端顺序

Scope: `public-api:wta-api`, HTTP/JSON, SQL/schema, `path:frontend/packages/api-contracts/**`, `path:frontend/packages/domains/**`

Level: MUST

Source: `repository-fact` (`AGENTS.md`, `namewta-fullstack-development`)

Rule: JSON 字段、HTTP 路径、认证 header、数据库 schema 和初始化 SQL 都是兼容合同。跨端变更先形成向后兼容或同步可交付的后端合同，再更新前端消费者；破坏性变更必须给出迁移和回滚。

Verification: 对照 BO/VO/controller 与前端 types/API；运行两端构建；执行相关契约/人工验收；review SQL 顺序和兼容窗口。

### ARCH-005 目录与命名保持局部一致

Scope: `repository`

Level: SHOULD

Source: `repository-fact` + `builder-baseline`

Rule: 保持模块地图中的稳定主轴。命名表达业务和边界；只有形成独立生命周期、依赖方向或明显导航价值时新增目录/模块。大文件是职责 review 触发器，不是机械拆分阈值。

Verification: review 新路径能映射到模块职责；检查是否新增含义模糊的 `utils/helpers/common`；触及大文件时说明保留或提取职责的理由。

### ARCH-006 生成器与生成物分离

Scope: `path:frontend/**`, `path:backend/**`

Level: MUST

Source: `repository-fact` + manual discovery correction

Rule: 运行时代码生成器、后端 `wta-gen` 模块和前端 `gen` domain/web-domain 已物理删除；`tooling/generators` 当前只是 README 占位。标准 CRUD 静态模板统一位于父仓库 `docs/fm/**`，供 AI 与开发者参考实现。`target/**`、`.flattened-pom.xml`、App 自动导入声明和 `packages/api-contracts` 生成结果是生成物，应通过源配置或 OpenAPI 工具修复，不直接手改。

Verification: review 文件头、插件配置和生成来源；`git diff` 不含意外 build output；修改生成结果时同时指出源模板和生成命令。
