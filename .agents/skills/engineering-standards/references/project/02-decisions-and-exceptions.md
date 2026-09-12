# 决策、迁移与例外

## 决策

### DEC-000 实现证据优先级

- Scope: `path:frontend/apps/**`, `path:frontend/packages/**`, `path:backend/wta-modules/**`
- Decision: 前端实现按“同 owner、同形态的成熟 domain/web-domain/App 实现 -> 当前公开平台/宿主合同 -> 后端或 OpenAPI 合同 -> 框架通用做法”排序。后端标准 CRUD 按“同 owner 且已确认规范的成熟实现 -> `wta-system` 同类实现 -> `docs/fm` 中职责对应的精确模板 -> `wta-common` 公共合同 -> 框架通用做法”排序；已知待重构模块不能反向成为规范样例。`docs/fm` 提供当前标准骨架，不得覆盖复杂模块既有权限、事务、缓存、关系和交互合同。
- Source: `repository-fact`
- Rationale: 当前成熟模块包含模板没有覆盖的 Client 隔离、数据权限、关联写入、缓存失效、导入导出和条件装配。
- Migration: 新前端能力按 transport -> domain -> web-domain -> App composition 纵切片落地；修改现有复杂功能只做满足需求的增量变化，除非另有经验证的重构任务。
- Verification: diff 对照同 owner 最近实现、后端合同与包 README；评审中说明保留的领域行为、权限、事务与状态语义。

### DEC-001 Canonical 与作用域

- Scope: `repository`
- Decision: 项目开发 Skill 的唯一根目录是父工作区 `.agents/skills/`；其中 `engineering-standards` 是覆盖父仓库与两个子模块的唯一规范裁决层，其他 Skill 只承载实现导航与模块事实。
- Source: `user-decision` + `repository-fact`
- Rationale: 用户明确要求 Codex Skill 与 Claude Agent 收敛到同一父级目录；仓库以 Submodule 聚合两个独立技术栈，子仓库副本会产生漂移。
- Migration: 将当前有效内容合并到父级 Skill，删除两个子仓库中的 `.claude`、`.codex` 目录；不创建工具专属兼容入口。
- Verification: 对全部父级 Skill 运行 Skill Creator validator；检查活动项目文档不再引用子仓库 `.claude/.codex`，且 canonical 外不存在重复 Skill 正文。

### DEC-002 存量采用 Ratchet

- Scope: `repository`
- Decision: 新代码与实质修改代码立即遵循 Target；存量偏差不得扩大，触及高风险边界时同步收紧。
- Source: `builder-baseline` + `repository-fact`
- Rationale: 前端类型规则宽松、测试门禁稀疏且存在大文件；全量迁移会制造无关变更。
- Migration: 按模块/变更文件推进，不发动全仓格式化或目录重写。
- Verification: PR diff 不增加 `any`/深依赖/无测试高风险逻辑；实际门禁结果记录。

### DEC-003 保留项目目录主轴

- Scope: `module:frontend`, `module:backend`
- Decision: 前端保持 `apps`、`domains`、`web-domains`、`platform`、`adapters`、`web-kit`、`api-contracts`、`tooling` 的所有权主轴；后端由[模块模式登记表](03-backend-module-modes.md)裁决目录主轴：登记为 classic 的存量模块保持 `controller`、`domain/{bo,vo}`、`mapper`、`service/impl`，新建或登记为 layered 的模块使用 `controller`、`usecase`、`service`、`dao`、`mapper` 和 `domain/model/read`。所有 Mapper XML 位于 `src/main/resources/mapper/<module>/`。
- Source: `repository-fact`
- Rationale: 前端多 App 领域重构已完成且有架构工具持续检查；后端仍保持稳定模块边界。
- Migration: 新前端能力放入真实 owner 并由 App 显式选择；移动端和小程序在独立规格激活前保持 README-only。
- Verification: module map review；`pnpm architecture:check`; 工作区 build；Maven build。

### DEC-009 新后端业务模块默认五层

- Scope: `path:backend/wta-modules/**`
- Decision: 新增后端业务模块默认登记为 `layered`，强制 `Controller/Listener/API Adapter -> UseCase -> Service -> DAO -> Mapper -> Mapper XML`。DAO 不继承 MyBatis-Plus `IService`；Service 不继承 `IService`/`ServiceImpl`，只承载业务规则并通过 DAO 持久化。`wta-system`、`wta-workflow`、`wta-job`、`wta-demo`、`wta-ai` 在本阶段登记为 classic，保持现状。
- Source: `user-decision` + `repository-fact` (`wta-profile`)
- Rationale: 编排、业务规则和持久化条件分离，避免 ServiceImpl 变成 SQL、事务、关系和外部副作用的耦合中心，同时保留存量模块兼容窗口。
- Migration: 新模块直接使用 layered；存量只按 Ratchet 收紧。classic -> layered 必须提供旧路径、公开合同、调用方、SQL、测试和回滚映射，不以本决策授权无关重构。
- Verification: 模块注册表、目录/import 静态检查、五层链路测试、Maven 测试和构建；通过 `validate-module-mode.mjs` 检查 layered 依赖方向。

### DEC-004 上游与产品分支隔离

- Scope: `repository`, both submodules
- Decision: `main` 承载产品；后端 `6.X`、前端 `6.X-Vue` 是只允许 fast-forward 的上游镜像；基线标签不移动。
- Source: `repository-fact`
- Rationale: `README.md` 与 customization map 明确规定同步模型。
- Migration: 上游更新先检查重叠面，再将必要提交同步到产品分支；父仓库最后更新指针。
- Verification: branch/ref review；按 customization map 逐项核对。

### DEC-005 NAMEWTA 安全合同优先

- Scope: authentication, authorization, Client, user type, menu, session and registration paths
- Decision: `docs/upstream/customization-map.md` 中“合并后必须保持”的约束均作为 MUST 合同。
- Source: `repository-fact`
- Rationale: 这些规则防止跨 Client 权限泄漏、身份混淆和上游同步回归。
- Migration: 触及热点时执行前后端和 SQL 契约测试/人工矩阵；不以 UI 隐藏替代服务端授权。
- Verification: customization map 路径 review；相关回归测试；认证验收矩阵。

### DEC-006 CRUD 查询 GET、变更 POST 并追踪日志

- Scope: 前后端 HTTP CRUD 接口及其生成模板
- Decision: 列表/分页、详情、树和下拉等只读查询使用 HTTP `GET`；新增、修改、删除、批量删除、状态变更和排序更新等变更操作使用 HTTP `POST`，每个 POST 业务接口必须使用安全、准确的 `@Log` 记录调用追踪；CRUD 不使用 PUT/PATCH/DELETE，非 CRUD 协议按自身合同处理。
- Source: `user-decision`
- Rationale: 统一查询与变更的接口语义，并通过操作日志保留 POST 调用的操作者、方法、URL、结果、耗时与失败状态等追踪信息。
- Migration: 新增 CRUD 立即执行；实质修改存量 CRUD 合同时，同步迁移该合同内受影响的前端请求、controller mapping、`@Log`、测试和文档，禁止继续新增 PUT/PATCH/DELETE CRUD 路由或无 `@Log` 的 POST 业务接口。生成器迁移后才能作为新 CRUD 骨架。
- Verification: 执行 `API-005` 的前后端静态搜索与契约/集成测试；生成分页和树表代表样例，确认 Java/Vue 两端查询为 GET、变更为 POST，且每个 POST controller 方法均有准确、安全的 `@Log`。

### DEC-007 前端按文件角色命名

- Scope: `module:frontend`
- Decision: 不采用全仓单一 kebab/Pascal/camel 文件名规则；按 API/页面、通用组件、page-local SFC、composable、枚举、utility/store/plugin 和测试角色使用 `TS-ORG` 映射。
- Source: `repository-fact` + `builder-baseline`
- Rationale: 当前生成器和源码已形成稳定的角色化模式，同时保留少量历史组件、layout 和 workflow 例外；单一大小写会破坏生成路径、路由和局部一致性。
- Migration: 新文件立即遵循角色 Target；存量只在实际触及且调用方、路由、自动导入和大小写敏感验证完整时改名，不发动批量重命名。
- Verification: `TS-ORG-001`～`TS-ORG-005`；`pnpm lint`, `pnpm typecheck`, `pnpm test`, `pnpm build:prod`; `git diff --summary` 检查大小写重命名。

### DEC-008 后端 Controller 访问面与匿名边界

- Scope: `path:backend/wta-modules/**/controller/**`
- Decision: 需要登录与权限校验的管理端接口放入 `controller/admin`；使用 `@SaIgnore` 的匿名接口放入 `controller/anonymous`，不得与受保护接口混放。只有真实存在并有独立合同的其他客户端才建立 `controller/<actual-client>`；不得为“未来可能存在”的客户端预建目录。已登录但非管理端的自服务接口必须在业务规格中先裁决访问面，通用规范不猜测归属。
- Source: `user-decision` + `repository-fact` (`wta-system` controller/security practices)
- Rationale: 目录直接暴露认证边界，防止匿名与管理接口混杂，也避免围绕假想客户端制造空抽象。
- Migration: 新模块立即执行；存量模块结构化重构时先建立路由、权限、调用方和测试映射，再移动文件，不借机改变正确业务合同。
- Verification: 扫描 `@SaIgnore` 与 controller 路径；核对所有管理端权限；确认不存在无真实 owner 的客户端目录；运行认证、签名、重放、幂等与权限负向测试。

## 当前状态与目标状态

| ID | Current | Target | Migration / Verification |
|---|---|---|---|
| `MIG-TS-STRICT` | `strict: true` 但 `noImplicitAny`、`strictNullChecks`、`strictFunctionTypes` 关闭；lint 允许多类 any | 新/修改的 API、存储、环境变量和第三方响应边界使用精确类型、`unknown` + narrowing，不新增无理由 any | 变更文件 Ratchet；review、`pnpm lint` 与完整 `pnpm typecheck` 已是 active gate |
| `MIG-FE-TEST` | 根级 `pnpm test` 聚合当前激活 App/包的 Vitest，Playwright 覆盖 Admin 关键浏览器流程 | 新增纯逻辑、状态、权限或复杂交互时在 domain、web-domain、App 或 E2E 的正确层级扩展测试；第二个 App 激活时恢复跨 App 隔离场景 | 本地与 CI 运行 architecture/lint/typecheck/unit/build，按风险运行 E2E；覆盖率阈值仍待决策 |
| `MIG-BE-TEST` | 176 个已跟踪 Java 测试源码文件；root 默认执行测试，Redis/MySQL/MinIO 用例通过属性门控接入真实服务 job | 认证、权限、事务、SQL 和公共 API 变更有回归测试，并在合并门禁执行 | `./mvnw test` 为默认门禁；core 产物因排除 demo/workflow 等可选模块而在已测试后用 `-Dmaven.test.skip=true` 组装；真实服务由 CI 补证 |
| `MIG-BE-DS-TX` | 业务代码同时存在 Spring `@Transactional` 与 dynamic-datasource `@DSTransactional` | 新建或实质修改的业务事务统一使用 `@DSTransactional`，事务事件使用匹配的 `@DsTxEventListener` | 按变更文件 Ratchet；迁移时验证代理调用、回滚、数据源切换和提交阶段事件；不发动无需求证据的全仓替换 |
| `MIG-BE-DDL-BASE` | 历史、上游、第三方及部分 NAMEWTA 表未统一具备 `version/create_dept/create_time/create_by/update_time/update_by/del_flag` | 每个新建项目自有表均具备七个基础字段，并与 `BaseEntity`、`@Version`、`@TableLogic` 映射一致 | 新表立即执行；触及存量项目自有表时评估兼容迁移，未经迁移设计不直接补列；上游冻结和第三方 schema 不做无关整治 |
| `MIG-CI` | 已配置 `.github/workflows/quality-gates.yml`，含快照、前端、后端与真实服务四个 job | PR 稳定执行同源 lint/typecheck/test/build，并由分支保护设为 required checks | 本地完成静态验证；提交推送后观察首次 Actions 运行，再配置分支保护并记录远程证据 |
| `MIG-LARGE-FILES` | 前后端存在多个 700-1400 行文件 | 新功能避免继续混合职责，触及文件时提取可命名且可测试的边界 | 文件大小只作 review 触发器；不得为行数机械拆分 |
| `MIG-CRUD-METHOD-LOG` | 现有前端 API、后端 controller 和生成模板仍混用 GET/POST/PUT/DELETE，POST 日志约束尚未统一 | CRUD 只读查询使用 GET，变更使用 POST；CRUD 不使用 PUT/PATCH/DELETE；每个 POST 业务接口使用安全、准确的 `@Log` | 新增立即执行；存量按 CRUD 合同触及范围迁移，前后端、日志注解、测试、文档和生成模板同步收敛；按 `API-005` 验证 |
| `MIG-FE-NAMING` | 各 App/包内存在符合局部语义的 PascalCase/lowerCamelCase 混用，工作区目录和包名已稳定使用 kebab-case | 新文件按 `TS-ORG` 所有权与角色映射命名，import 与磁盘大小写精确一致 | 不批量改名；触及存量时核对 exports/manifest/route/import，运行 architecture/typecheck/build 并检查 Git 大小写 diff |

## 例外

### EX-001 前端 axios TypeScript 6 兼容断言

- Scope: `path:frontend/packages/adapters/axios-browser/src/index.ts`
- Rule: 边界不得使用无约束 `any`。
- Owner: `frontend` maintainers
- Reason: 当前源码记录 axios 1.x 默认导出与 TypeScript 6 的 `export=` 解析兼容问题。
- Risk: request/response/interceptor 类型错误可能被隐藏。
- Compensation: 将兼容断言限制在 axios browser adapter；domain 使用平台 HTTP 合同与明确模型，修改时审查所有断言。
- Created: existing repository state
- Expires/removal condition: axios/typescript 类型兼容方式允许删除 adapter 断言，或项目建立精确本地类型包装。
- Verification: `pnpm lint`, `pnpm typecheck`, `pnpm build:prod`，review axios adapter 中断言是否扩散。

### EX-002 Profile 测试兼容构造桥

- Scope: `wta-profile` 生产 Service 中标记为 `@Deprecated` 的旧构造方法。
- Rule: Spring 主构造只能注入本模块 `port` 和公开 API；新代码不得通过旧构造直接注入 `WorkflowService`、具体 Provider 或具体 Store。
- Reason: Profile 迁移期间保留既有单元/E2E 测试的构造签名，避免一次重构破坏测试夹具；正式 Bean 装配已统一使用 `WorkflowGateway`、Provider/Store 等端口。
- Compensation: 兼容构造不参与 Spring 自动注入，转换逻辑只建立本模块端口桥；新增测试直接使用端口合同。
- Expires/removal condition: 测试夹具全部迁移到 `port` 合同后删除旧构造、旧 `I*Service` 兼容接口及相关 import。
- Verification: Profile strict layered validator、Maven test-compile 和模块测试；确认生产主构造不依赖实现模块。

## 待确认

- `PENDING-CI-001`: 首次 GitHub Actions 远程运行通过后，将哪些 job 设为 PR/主分支 required checks，以及失败责任与重跑权限。
- `PENDING-FE-001`: 是否新增非写入式 format-check 与覆盖率阈值；typecheck、Vitest、Playwright 已成为 active gate。
- `PENDING-ARCH-001`: 前端多 App 依赖方向与占位纯度已有自动架构测试；Maven 模块方向是否追加自动架构测试仍待确认。
