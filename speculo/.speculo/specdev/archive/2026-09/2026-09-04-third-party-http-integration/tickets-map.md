---
schema_version: 3
artifact: tickets-map
change: 2026-09-04-third-party-http-integration
status: completed
---

# Tickets Map: 统一第三方 HTTP 供应商与 Endpoint 管理

- **Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **Ticket 目录：** `<Path>{roots.state}/specdev/changes/{change}/ticket/</Path>`
- **Evidence 目录：** `<Path>{roots.state}/specdev/changes/{change}/evidence/</Path>`
- **推荐 Goal Plan：** `<Path>{roots.state}/specdev/changes/{change}/goal-plan.md</Path>`

## 1. 目标与拆分策略

13 个 Ticket 共同交付 `US-001` 至 `US-009` 和 `AC-001` 至 `AC-018`：先并行建立模块/公共 API 与完整数据基座，再依次关闭 Provider、Endpoint、凭据和有效快照的安全决策，随后交付统一 Gateway、韧性与可观测性，最后建立独立 third 前端域、四个管理页面、Admin 菜单组合和全栈发布 Gate。

切片遵守以下边界：

- T-01/T-02 是唯一初始并行波次；其后依赖安全合同逐层收敛，避免未冻结配置或密文边界时提前发送 HTTP。
- 根 POM/API/yml、50 DDL、两个前端包、Admin composition/lock/61-third-dml.sql 和父级 Skills 分别只有一个 shared owner。
- Facade、Registry/Strategy、Factory、Pipeline、Decorator/Interceptor 按 ADR owner 分配到 T-07/T-08/T-09，不允许供应商 adapter 复制或替换基础设施。
- 不创建真实企查查 URL/secret/私有 adapter；通用执行以本地确定性 HTTP provider 验证。
- T-13 是只读产品树的集成与工程事实 Gate，不承载临时产品修补；失败退回原 owner Ticket。
- Ticket 数量达到 13，且含公共 API/schema、安全密钥、多实例 Redis、共享路径与多个 required E2E，实施前必须完成 Goal Plan。

## 1.1 Skill 加载与新模块最低工程基线

每个 Ticket 开始前，implementation owner 必须完整读取其 Ticket 的“必须加载的 Skill 与工程基线”小节；不能以本 Map 的摘要代替 Skill 原文。所有 backend Ticket 还必须先读取 <Path>.agents/skills/engineering-standards/SKILL.md</Path> 的项目画像、模块地图、决策/例外、backend module modes 和命中的 Java/Spring/persistence/CRUD/security/testing references。所有 frontend Ticket 还必须读取 <Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path> 及 TypeScript/Vue/Browser/CRUD/API references。

新 ruoyi-third 的不可放宽基线：

- 后端默认且必须登记为 layered，目录主轴为 controller、usecase/impl、service、dao、mapper、resources/mapper/<module>；调用链必须是 Controller/Listener/API Adapter → UseCase → Service → DAO → Mapper → XML，禁止 IService/ServiceImpl、入口直连 Mapper/DAO、跨模块 Entity/VO/Mapper deep import。
- 后端 CRUD 查询使用 GET，业务变更使用 POST；每个 POST 使用安全 @Log；业务事务使用 @DSTransactional；Entity、domain/model/read Row、BO、VO 分离，Controller 不返回 Entity。
- POM 只按需依赖 ruoyi-common-* 子 artifact，不依赖 packaging=pom 的父 ruoyi-common；公共 Gateway/DTO/SPI 位于 ruoyi-api，新增 public API 必须读取并遵守 java-api-compatibility。
- MySQL 只支持 8.4；项目自有 DDL 只能进入 <Path>release-artifacts/docker/infrastructure/mysql/init/50-namewta-ddl.sql</Path>，三方菜单/初始化 DML 独立进入 <Path>release-artifacts/docker/infrastructure/mysql/init/61-third-dml.sql</Path>；禁止 script/、模块私有 SQL 和其他方言。
- 前端必须保持 App → web-domain → domain → platform；domain 无 Vue/DOM/具体请求库，web-domain 承载页面和局部状态，App 只在 package.json、application/services.ts、adminManifestRegistry.ts 显式组合。每个 package.json 包必须有同目录 AGENTS.md、README、tsconfig、scripts 和 exports；资源使用 kebab-case、薄 index.ts 和相邻测试，禁止 catch-all export/deep import。
- 验证必须包含目标行为的正常、失败和回归接缝；安全/数据/跨运行时边界按 Ticket 的 E2E disposition 执行，不能用单元测试、自报截图或跳过命令伪造通过。

各 Ticket 的 Skill owner 矩阵：

| Ticket | 必须加载的 Skill | 重点参考/门禁 |
|---|---|---|
| T-01 | engineering-standards、namewta-fullstack-development、ruoyi-module-guide、ruoyi-common-modules-guide、java-api-compatibility | layered 新模块、POM/common 按需依赖、ruoyi-api 公共兼容 |
| T-02 | engineering-standards、namewta-fullstack-development、ruoyi-module-guide、ruoyi-common-modules-guide | persistence/DDL、Mapper/XML、50 DDL、MySQL 8.4 |
| T-03 | engineering-standards、namewta-fullstack-development、ruoyi-module-guide、ruoyi-common-modules-guide | layered CRUD、GET/POST/@Log/@DSTransactional、权限/BO/VO |
| T-04 | engineering-standards、namewta-fullstack-development、ruoyi-module-guide、ruoyi-common-modules-guide | security/data、metadata 白名单、运行时二次校验 |
| T-05 | engineering-standards、namewta-fullstack-development、ruoyi-module-guide、ruoyi-common-modules-guide | 认证加密、密文边界、canary/日志负向验证 |
| T-06 | engineering-standards、namewta-fullstack-development、ruoyi-module-guide、ruoyi-common-modules-guide | RedisUtils/Redisson、提交后失效、fail-closed |
| T-07 | engineering-standards、namewta-fullstack-development、ruoyi-module-guide、ruoyi-common-modules-guide、java-api-compatibility | RestClient/@HttpExchange、SPI/Registry/Factory/Pipeline owner、URI 安全 |
| T-08 | engineering-standards、namewta-fullstack-development、ruoyi-module-guide、ruoyi-common-modules-guide | timeout、幂等 retry、发送前 Redisson gate、permit 释放 |
| T-09 | engineering-standards、namewta-fullstack-development、ruoyi-module-guide、ruoyi-common-modules-guide | SysLog writer/sink、脱敏/截断、attempt/逻辑统计 |
| T-10 | engineering-standards、namewta-fullstack-development | domain 资源纵切片、exports/AGENTS、transport mapper 和权限 |
| T-11 | engineering-standards、namewta-fullstack-development | web-domain 页面、局部状态、component key、敏感 DOM 和组件测试 |
| T-12 | engineering-standards、namewta-fullstack-development、namewta-fullstack-development、ruoyi-module-guide | App 三点组合、manifest/SQL/permission 一致、61-third-dml.sql 与浏览器 E2E |
| T-13 | engineering-standards、namewta-fullstack-development、ruoyi-module-guide、ruoyi-common-modules-guide、java-api-compatibility、namewta-fullstack-development | 事实同步、full/core、MySQL/Redis/HTTP/SysLog/frontend 全 Gate |

任何 Ticket 若未完成规定 Skill 读取、目录模式验证或命中停止条件，必须暂停该 Ticket，不得以“沿用现有写法”替代新模块基线。

## 2. 执行清单

| ID | Ticket | 可观察产出 | Blocked By | Depth | Risk | Ready | Owner | Contract IDs | Wave/Gate | Status |
|---|---|---|---|---|---|---|---|---|---|---|
| T-01 | `<Path>{roots.state}/specdev/changes/{change}/ticket/01-module-and-public-contracts.md</Path>` | ruoyi-third 双 bundle 装配与 Gateway 公共合同 | — | deep | high | yes | unassigned | AC-012,018 | W1 / foundation | ready |
| T-02 | `<Path>{roots.state}/specdev/changes/{change}/ticket/02-third-schema.md</Path>` | 五类表、唯一约束、Mapper 与 MySQL 基座 | — | deep | high | yes | unassigned | AC-001,002,005,013,017 | W1 / data | ready |
| T-03 | `<Path>{roots.state}/specdev/changes/{change}/ticket/03-provider-management.md</Path>` | Provider 管理、trusted origin、启停与删除保护 | T-01,T-02 | deep | high | yes | unassigned | AC-001,016,017 | W2 | ready |
| T-04 | `<Path>{roots.state}/specdev/changes/{change}/ticket/04-endpoint-management.md</Path>` | Endpoint 元数据、继承、白名单和安全路径 | T-03 | deep | high | yes | unassigned | AC-002,003,006,007,016,017 | W3 | ready |
| T-05 | `<Path>{roots.state}/specdev/changes/{change}/ticket/05-credential-security.md</Path>` | 分 scope AES-GCM 密文、覆盖与无明文管理 | T-04 | deep | high | yes | unassigned | AC-005,016 | W4 | ready |
| T-06 | `<Path>{roots.state}/specdev/changes/{change}/ticket/06-config-cache-invalidation.md</Path>` | DB 事实快照、Redis 二级缓存和集群失效 | T-03,T-04,T-05 | deep | high | yes | unassigned | AC-003,004,005,010 | W5 | ready |
| T-07 | `<Path>{roots.state}/specdev/changes/{change}/ticket/07-http-gateway-pipeline.md</Path>` | RestClient/@HttpExchange、SPI registry 与安全 Pipeline | T-01,T-06 | deep | high | yes | unassigned | AC-003,006-008,011,012 | W6 | ready |
| T-08 | `<Path>{roots.state}/specdev/changes/{change}/ticket/08-resilience-and-limits.md</Path>` | 超时、幂等有限重试和双层 Redisson 门禁 | T-07 | deep | high | yes | unassigned | AC-009,010 | W7 | ready |
| T-09 | `<Path>{roots.state}/specdev/changes/{change}/ticket/09-observability-and-statistics.md</Path>` | attempt/逻辑统计、脱敏明细和强制出站日志 | T-02,T-07,T-08 | deep | high | yes | unassigned | AC-011,013,014 | W8 | ready |
| T-10 | `<Path>{roots.state}/specdev/changes/{change}/ticket/10-frontend-domain-contracts.md</Path>` | 独立 domain-third transport/model/permission 合同 | T-03,T-04,T-05,T-09 | standard | medium | yes | unassigned | AC-015,016 | W9 | ready |
| T-11 | `<Path>{roots.state}/specdev/changes/{change}/ticket/11-third-admin-pages.md</Path>` | Provider/Endpoint/明细/统计四个管理页面 | T-10 | standard | medium | yes | unassigned | AC-015,016 | W10 | ready |
| T-12 | `<Path>{roots.state}/specdev/changes/{change}/ticket/12-admin-composition-and-menu.md</Path>` | Admin 显式组合及系统管理下菜单/权限 | T-11 | deep | high | yes | unassigned | AC-015,016 | W11 / app Gate | ready |
| T-13 | `<Path>{roots.state}/specdev/changes/{change}/ticket/13-engineering-sync-and-release-gate.md</Path>` | Skills 事实同步与全栈/full-core 发布 Gate | T-01-T-12 | deep | high | yes | unassigned | AC-001-018 | W12 / release Gate | ready |

Ticket frontmatter 是状态、依赖、深度和路径访问契约的权威；本表是同步投影，不得独立修改出另一套真相。

## 2.1 Implementation status and Skill gate result

T-01 through T-13 are integrated and done; the release gate is closed. Every ticket has implementation owner `codex:/root`. Before implementation, the owner loaded the Skills declared by each ticket. The minimum required matrix is authoritative:

| Tickets | Required Skills |
|---|---|
| T-01..T-09 | `engineering-standards`, `namewta-fullstack-development`, `ruoyi-module-guide`, `ruoyi-common-modules-guide` |
| T-01, T-07 | `java-api-compatibility` |
| T-10..T-11 | `engineering-standards`, `namewta-fullstack-development` |
| T-12 | frontend Skills above plus `namewta-fullstack-development`, `ruoyi-module-guide` |
| T-13 | all backend and frontend Skills above plus `java-api-compatibility` |

The new backend module is registered as layered and must keep `controller/admin -> usecase/impl -> service -> dao -> mapper -> XML`; runtime infrastructure is isolated behind `port`, `adapter`, and pure `support` packages, and only stable contracts in `ruoyi-api` may be consumed by other business modules. The frontend remains `App -> web-domain -> domain -> platform`, with explicit package exports and App composition. No database script, SpEL, arbitrary reflection, full URL, arbitrary header, plaintext credential, or caller-side bypass is allowed.

Implementation commits and verification evidence are recorded in the per-ticket files under `evidence/`. The final evidence includes randomized and exact-schema MySQL 8.4.9 checks, canonical ordered fresh-init, isolated Redis/Redisson invalidation and limiter checks, Spring application-context HTTP/SPI checks, production SysLog persistence, direct unauthenticated management HTTP, browser permission E2E, and isolated full/core bundle composition. All release prerequisites are closed in the disposable verification environment.

The execution overlay is authoritative over the planning table below: T-01 through T-13 are integrated and the release gate is closed. The latest backend checkpoint is `751ecd439`; layered validation passes for 71 Java files after provider-owned adapter validation, typed `@HttpExchange`, bounded retry, ACK-based invalidation, physical-attempt logging, immutable provider identity, and credential lifecycle guards. The latest frontend checkpoint is `cc6a6f2`, covering the four-page web-domain manifest, nested credential workflow, endpoint override input, list-only permission hiding, and the `third -> ruoyi-third` architecture registration. Full workspace typecheck, lint, test, build, architecture, direct HTTP permission, browser E2E, and real Redis/MySQL/HTTP/SysLog checks pass as recorded in Evidence, including the canonical MySQL fresh-init.

## 3. 依赖 DAG

```text
W1  T-01 [module/API]        T-02 [schema]
       ╲                       ╱
W2      └────────→ T-03 [Provider]
                       │
W3                 T-04 [Endpoint]
                       │
W4                 T-05 [Credential]
                       │
W5                 T-06 [Snapshot/Redis]
                       │
W6                 T-07 [Gateway/Pipeline]
                       │
W7                 T-08 [Timeout/Retry/Limits]
                       │
W8                 T-09 [Logs/Statistics]
                       │
W9                 T-10 [Frontend Domain]
                       │
W10                T-11 [Admin Pages]
                       │
W11                T-12 [App/Menu Gate]
                       │
W12                T-13 [RELEASE GATE]
```

补充真实开始条件：T-06 同时核对三类管理提交事件；T-07 还直接等待 T-01 公共合同；T-09 直接等待 T-02 统计表；T-10 等待后端 transport 稳定；T-13 明确等待 T-01 至 T-12 的全部 result，而非只依赖最后一个文件合入。

## 4. 合同覆盖矩阵

| Contract ID | 覆盖 Ticket | 验证接缝 | 状态 | 说明 |
|---|---|---|---|---|
| AC-001 | T-02,T-03,T-13 | MySQL/MVC/full stack | covered | Provider CRUD、唯一、启停 |
| AC-002 | T-02,T-04,T-13 | MySQL/MVC/full stack | covered | Endpoint CRUD 与组合唯一 |
| AC-003 | T-04,T-06,T-07,T-13 | resolver/Gateway | covered | Provider 优先与继承覆盖 |
| AC-004 | T-06,T-13 | 双实例 Redis | covered | 保存后立即失效与确认 |
| AC-005 | T-02,T-05,T-06,T-13 | codec/MySQL/Gateway | covered | 分 scope 密文和 fail-closed |
| AC-006 | T-04,T-07,T-13 | metadata/local HTTP | covered | JSON/query/form 与 JSON/text/bytes |
| AC-007 | T-04,T-07,T-13 | zero-request security | covered | URL/path/header/script/redirect 拒绝 |
| AC-008 | T-07,T-13 | Spring Context/registry | covered | adapter 唯一与缺失校验 |
| AC-009 | T-08,T-13 | retry HTTP counter | covered | 幂等有限重试 |
| AC-010 | T-06,T-08,T-13 | Redis/zero-request | covered | 双层门禁与未知状态关闭 |
| AC-011 | T-07,T-09,T-13 | adapter/response/log | covered | 2xx 与业务成功分离 |
| AC-012 | T-01,T-07,T-13 | public API/typed call | covered | JsonNode 与显式类型响应 |
| AC-013 | T-02,T-09,T-13 | MySQL statistics | covered | attempt 与逻辑结果计数 |
| AC-014 | T-09,T-13 | SysLog sink/canary | covered | 强制日志、脱敏和截断 |
| AC-015 | T-10,T-11,T-12,T-13 | manifest/browser | covered | 系统管理下目录与四页面 |
| AC-016 | T-03,T-04,T-05,T-10,T-11,T-12,T-13 | MVC/permission/browser | covered | 页面、按钮与后端授权 |
| AC-017 | T-02,T-03,T-04,T-13 | UseCase/MySQL | covered | disabled 才可删与历史保留 |
| AC-018 | T-01,T-13 | full/core/context | covered | 两 bundle 装配与空数据启动 |

不存在 `uncovered` 或 `deferred` 合同。T-13 的重复覆盖是集成 Gate，不改变前序 Ticket 的实现所有权。

## 5. 并行与路径所有权

- implementation subagent 上限来自 `<Path>{roots.state}/specdev/config.json</Path>`，Goal Plan 已固定为 current/direct-parent；本 Map 仍不授权 subagent、实施或提交。
- current 模式下即使 W1 有 T-01/T-02 两个候选，也必须由 Lead 严格串行持有唯一 writer 锁；其余链路因安全合同和公共输出存在真实依赖。
- review/research/test-observation 保持只读；Lead 持有 SpecDev 状态、父分支集成和全部 E2E。
- Ticket frontmatter 是路径权威；消费者不得为方便修改 shared owner 文件。

| Ticket A | Ticket B | Writable 交集 | 真实依赖 | 处理 |
|---|---|---|---|---|
| T-01 | T-02 | 无 | 否 | W1 可并行；分别拥有 Maven/API/config 与 schema/Mapper |
| T-03 | T-04 | 无 | 是 | Provider 合同先冻结，再建立 Endpoint |
| T-05 | T-06 | 无 | 是 | 密文覆盖先冻结，再缓存有效快照 |
| T-07 | T-08 | 无 | 是 | Pipeline 扩展点先冻结，再挂载 resilience stage |
| T-08 | T-09 | 无 | 是 | attempt/rejection 语义先冻结，再统计 |
| T-10 | T-11 | 无 | 是 | domain exports 先冻结，再实现页面 |
| T-11 | T-12 | 无 | 是 | page keys 先冻结，再写 App/SQL |
| T-12 | T-13 | 无 | 是 | 产品共同状态完成后才允许文档同步与 release Gate |

共享文件唯一所有者：

| Shared owner | 共享路径 |
|---|---|
| T-01 | backend 根/模块/Admin POM、application.yml、third public API、module POM |
| T-02 | NAMEWTA 50 DDL |
| T-10 | `packages/domains/third` 公共包 |
| T-11 | `packages/web-domains/third` 页面包 |
| T-12 | Admin package/services/manifest、pnpm lock、NAMEWTA 61-third-dml.sql |
| T-13 | project/module/frontend Skills 事实文档 |

## 6. Gate、Wave 与集成点

| Wave | 候选 Ticket | 行为里程碑 | Gate 退出条件 |
|---|---|---|---|
| W1 | T-01,T-02 | 模块/API 与数据基座 | 双 bundle/API contract、MySQL schema 通过 |
| W2-W5 | T-03-T-06 | 安全配置控制面 | CRUD/权限/密文/多实例失效通过 |
| W6-W8 | T-07-T-09 | 统一调用数据面 | SSRF 零请求、响应映射、韧性、日志和计数通过 |
| W9-W10 | T-10,T-11 | 独立前端域与页面 | package contract、组件权限和敏感 DOM 通过 |
| W11 | T-12 | App/菜单开放 | SQL、manifest、后端权限和浏览器 E2E 通过 |
| W12 | T-13 | release readiness | 全 AC、full/core、MySQL/Redis/frontend 回归和 Skills 同步通过 |

Goal Plan 已确定 workspace strategy=current、integration_gate=direct-parent、Lead=codex:/root、implementation_agent_limit=3、integration_attempt_limit=null。用户已通过 `USER-DECISION:2026-09-04-execute-goal-plan` 授权本地实现、验证、Evidence 与 direct-parent 更新；T-01 至 T-12 已有实现提交，T-13 仍等待外部 release Gate。本 Map 不构成远程提交、生产迁移、菜单赋权或真实供应商调用授权。

## 7. 横切契约与风险

- **安全：** Provider 可信 origin 加运行时相对 path 双重校验；无绝对 URL、`//host`、穿越、任意 header、脚本/表达式、任意类反射或跨域 redirect。
- **凭据：** Provider/Endpoint 分 scope，Endpoint 同类型覆盖；DB 仅 AES-GCM 认证密文，主密钥只来自外部配置；所有未知状态发送前关闭。
- **模式 owner：** Facade 只编排，Registry/Strategy 只选 adapter，Factory 只建 client，Pipeline 只管理生命周期，Decorator/Interceptor 只做横切。
- **一致性：** DB 是事实源；Redis 为版本化二级快照；管理提交后精确失效并确认集群通知，不使用陈旧未知快照。
- **韧性：** timeout 有界，非幂等只发一次，幂等有限重试；Provider/Endpoint 两层 Redisson 限制均在发送前，quota 仅展示。
- **可观测性：** attempt 是物理发送，最终结果是逻辑调用；业务表无完整 body；全部 third 出站自动进入 common SysLog sink 且强制脱敏/截断。
- **前端：** third 源码独立于 system，菜单仅投影在 System Management；SQL/Controller/domain/manifest 权限字符串完全一致。
- **发布：** 无真实 Provider 预置；fresh-init 顺序 10→50→60→61；生产 DDL/DML、密钥、角色赋权和 Endpoint 启用均需外部批准。

## 8. 同步规则

- Ticket 状态变化后同步执行清单；
- Ticket ID、路径、依赖或 frontmatter 不一致时，以 Ticket 文件为权威并修复本 Map；
- Goal Plan 存在时，Wave、Gate、workspace 和 owner 以 `<Path>{roots.state}/specdev/changes/{change}/goal-plan.md</Path>` 为编排权威；
- 依赖、合同覆盖或路径所有权变化后运行 `<Path>{roots.workflows}/specdev/common/tools/validate-specdev.mjs</Path>`；
- 内部工件不得使用相对 Markdown 链接；
- 本 Map 和 Ticket 均不构成实现、提交、合并、生产迁移、凭据写入或权限赋予授权。
