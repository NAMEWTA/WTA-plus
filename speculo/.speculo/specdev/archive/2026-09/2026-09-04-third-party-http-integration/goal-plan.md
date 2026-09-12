---
schema_version: 6
artifact: goal-plan
change: 2026-09-04-third-party-http-integration
status: completed
modes: [high-assurance, release-coordination]
orchestration: lead-directed
lead: codex:/root
implementation_agent_limit: 3
integration_attempt_limit: null
ticket_workspace_policy: current
integration_gate: direct-parent
ready_for_execution: false
---

# Goal Plan: 统一第三方 HTTP 供应商与 Endpoint 全栈交付

- **Goal Plan：** <Path>{roots.state}/specdev/changes/{change}/goal-plan.md</Path>
- **Spec：** <Path>{roots.state}/specdev/changes/{change}/spec.md</Path>
- **Tickets Map：** <Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>
- **Ticket 目录：** <Path>{roots.state}/specdev/changes/{change}/ticket/</Path>
- **Evidence 目录：** <Path>{roots.state}/specdev/changes/{change}/evidence/</Path>

## 1. Outcome and Authority

### Outcome

在现有 RuoYi NAMEWTA 全栈中交付一个可持续扩展的 ruoyi-third：Provider 是可信 base URL 与共享策略聚合根，Endpoint 是受白名单约束的相对 URI 元数据；所有出站调用经过统一同步 Gateway/Pipeline，标准动态 Endpoint 使用 RestClient，固定类型化契约使用 @HttpExchange，特殊签名/加密/业务错误通过显式 Java SPI。管理端在“系统管理 → 三方接口管理”中维护 Provider、Endpoint、凭据/策略、脱敏调用记录和双维度统计。

完成后，其他业务模块只依赖 ruoyi-api 的 ThirdPartyGateway/ThirdPartyRequest/ThirdPartyResponse，不接触数据库 Entity、凭据或底层 HTTP client；状态、缓存、超时、有限幂等重试、Redisson 限制、统计和出站日志均由 ruoyi-third 统一控制。

### Success and False Completion

成功必须同时满足：

- AC-001 至 AC-018 在共同 parent 状态有 Lead 可复查的通过 Evidence；
- 后端 T-01 至 T-09、前端 T-10 至 T-12 均有非空 implementation commit、current-workspace 定向检查和 Lead-owned direct-parent 验证；
- MySQL 8.4 fresh-init、Redis 多实例失效、确定性本地 HTTP、系统日志 sink、权限/浏览器和 full/core bundle Gate 通过；
- 动态 URI/headers 无 SSRF 或任意执行路径，凭据/日志/响应/统计/DOM 无明文敏感泄漏；
- project/module/frontend Skills 只同步已证实源码事实。

以下不算完成：只有单元测试通过、只有 mock 自报成功、只有页面静态渲染、只写入数据库但未验证真实发送边界、只完成 Provider CRUD、或绕过 Gateway 的 typed/SPI 客户端。

### Non-goals

- 不预置真实企查查 URL、API key、secret、签名私钥或私有业务码；
- 不修改外部 cde-standard/cde-third；
- 不做异步投递、批量任务、outbox、multipart、流式上传、复杂模板、动态脚本/SpEL、任意类反射、默认熔断/自动降级或 quota 自动阻断；
- 不执行生产 DDL/DML、角色赋权、主密钥/凭据写入、真实供应商调用、远程 push/PR/merge 或 worktree 清理。

### Mandatory Skill Gate

每个 Ticket 开始时，Lead 必须确认 implementation owner 已完整读取该 Ticket 的“必须加载的 Skill 与工程基线”小节；该小节是执行前置条件，不是建议。全局最低集合为：

- backend T-01 至 T-09：<Path>.agents/skills/engineering-standards/SKILL.md</Path>、<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>、<Path>.agents/skills/ruoyi-module-guide/SKILL.md</Path>、<Path>.agents/skills/ruoyi-common-modules-guide/SKILL.md</Path>；T-01/T-07 另加 <Path>.agents/skills/java-api-compatibility/SKILL.md</Path>。
- frontend T-10/T-11：<Path>.agents/skills/engineering-standards/SKILL.md</Path>、<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>；T-12 另加 backend/module guide 以校验 SQL/menu/permission。
- T-13：以上全部 Skill，用于只读事实同步和全栈 Gate。

所有 backend owner 必须先读取 <Path>.agents/skills/engineering-standards/references/project/00-project-profile.md</Path>、<Path>.agents/skills/engineering-standards/references/project/01-module-map.md</Path>、<Path>.agents/skills/engineering-standards/references/project/02-decisions-and-exceptions.md</Path>、<Path>.agents/skills/engineering-standards/references/project/03-backend-module-modes.md</Path>；所有 frontend owner 必须读取 <Path>.agents/skills/namewta-fullstack-development/references/frontend/architecture.md</Path>、<Path>.agents/skills/namewta-fullstack-development/references/frontend/naming-and-layout.md</Path>、<Path>.agents/skills/namewta-fullstack-development/references/frontend/implementation.md</Path>。命中的 persistence、CRUD/API、security、Vue、Browser、permission 和 testing references 由 Ticket 小节逐项指定。

执行不可放宽的最低要求：

- ruoyi-third 必须按 layered 的 Controller/Listener/API Adapter → UseCase → Service → DAO → Mapper → XML 组织；POM 只依赖实际 common 子 artifact；公共 DTO/API 不暴露 Entity、凭据或 HTTP client。
- 后端查询 GET、变更 POST、POST 使用 @Log、事务使用 @DSTransactional；Entity/Row/BO/VO 分离，MySQL 8.4 DDL/DML 只能由 T-02/T-12 写入指定 50-namewta-ddl.sql/61-third-dml.sql 文件。
- frontend domain/web-domain/App 三层职责、package exports、同目录 AGENTS/README/tsconfig/scripts、kebab-case 资源、薄入口、相邻测试和显式三点组合必须完整。
- 每个行为都要有正常/失败/回归证据；未读取 Skill、越界写入、绕过分层或以跳过/放宽断言获得绿色时停止并返回对应 owner。

### Authoritative Inputs

| 优先级 | 来源 | 负责内容 | 冲突处理 |
|---|---|---|---|
| 1 | 用户最新明确决定 | 不开启 worktree；产品范围、供应商/Endpoint、日志、前后端菜单和安全取舍 | 更新真正拥有该决定的工件 |
| 2 | <Path>{roots.state}/specdev/changes/{change}/ADR.md</Path> 与 <Path>{roots.state}/specdev/changes/{change}/CONTEXT.md</Path> | 当前 change 架构决定与领域语义 | 返回 <Path>{roots.workflows}/specdev/G-grill-with-docs/G-grill-with-docs.md</Path> 更新 ADR/CONTEXT |
| 3 | <Path>{roots.state}/specdev/adr/</Path> 与 <Path>{roots.state}/specdev/context/</Path> | 永久架构和领域知识 | 在当前 change 明示替代关系 |
| 4 | <Path>{roots.state}/specdev/changes/{change}/spec.md</Path> | 外部行为、范围、AC/NFR 和发布约束 | 下游不得自行改写 |
| 5 | <Path>{roots.state}/specdev/changes/{change}/ticket/</Path> | 单 Ticket 实现合同、路径、局部验证 | Goal Plan 只编排，不扩大 |
| 6 | 当前 backend/frontend/SQL/Redis 代码与基线 SHA | 可执行命令、目录、依赖和运行事实 | 发现冲突暂停并按 deviation-control 返回 owner |

## 2. Execution Graph

### DAG and Critical Path

~~~text
W1  T-01 module/API ─┐
                     ├→ W2 T-03 Provider → W3 T-04 Endpoint → W4 T-05 Credential
W1  T-02 schema ─────┘                                      │
                                                            └→ W5 T-06 Snapshot/Redis
                                                               │
                                                               → W6 T-07 Gateway/Pipeline
                                                               │
                                                               → W7 T-08 Timeout/Retry/Limits
                                                               │
                                                               → W8 T-09 Logs/Statistics
                                                               │
                                                               → W9 T-10 domain-third
                                                               │
                                                               → W10 T-11 web-domain-third
                                                               │
                                                               → W11 T-12 Admin/Menu
                                                               │
                                                               → W12 T-13 full-stack release Gate
~~~

关键路径为 T-02 → T-03 → T-04 → T-05 → T-06 → T-07 → T-08 → T-09 → T-10 → T-11 → T-12 → T-13；T-01 与 T-02 是唯一可以在依赖上同时 Ready 的根节点。current 模式不并行执行，即使 W1 有两个候选也必须由 Lead 取得单一 writer 锁并按集成序号串行。

### Waves and Ownership

| Wave | Ticket | 前置条件 | 项目写路径 | Shared owner | Gate/集成序号 |
|---|---|---|---|---|---|
| W1 | T-01 | 无；后端/API 基线 | backend root POM/Admin yml、ruoyi-api、ruoyi-third POM/properties | T-01 | G1 / 01 |
| W1 | T-02 | 无；MySQL 基线 | NAMEWTA 50 DDL、third entity/Mapper/XML | T-02 | G1 / 02 |
| W2 | T-03 | T-01,T-02 result | Provider controller/usecase/service/BO/VO | 无 | G2 / 03 |
| W3 | T-04 | T-03 result | Endpoint controller/usecase/service/BO/VO | 无 | G2 / 04 |
| W4 | T-05 | T-04 result | credential security/usecase/controller/BO/VO | 无 | G2 / 05 |
| W5 | T-06 | T-03,T-04,T-05 result | runtime configuration/cache/invalidation | 无 | G2 / 06 |
| W6 | T-07 | T-01,T-06 result | adapter/gateway、http、spi、port、support、URI pipeline | 无 | G3 / 07 |
| W7 | T-08 | T-07 result | timeout/retry/limit policy | 无 | G3 / 08 |
| W8 | T-09 | T-02,T-07,T-08 result | observability/invocation/statistics Admin API | 无 | G4 / 09 |
| W9 | T-10 | T-03,T-04,T-05,T-09 result | packages/domains/third | T-10 | G5 / 10 |
| W10 | T-11 | T-10 result | packages/web-domains/third | T-11 | G5 / 11 |
| W11 | T-12 | T-11 result | Admin composition/manifest/lock/61-third-dml.sql | T-12 | G6 / 12 |
| W12 | T-13 | T-01..T-12 result | project/module/frontend Skills only | T-13 | G7 / 13 |

### Ticket Quick Reference

| ID | 可观察产出 | Dependencies | Workspace | Implementation owner | E2E disposition | Evidence |
|---|---|---|---|---|---|---|
| T-01 | 模块装配与公共 Gateway 合同 | — | backend current/main | Lead 或 execution-time dynamic writer | not-required：公共合同 prefactor | <Path>{roots.state}/specdev/changes/{change}/evidence/T-01.md</Path> |
| T-02 | 五类表、唯一约束和 Mapper | — | backend/root SQL current/main | Lead 或 execution-time dynamic writer | required：MySQL 8.4 schema | <Path>{roots.state}/specdev/changes/{change}/evidence/T-02.md</Path> |
| T-03 | Provider CRUD、origin、状态/删除保护 | T-01,T-02 | backend current/main | Lead 或 execution-time dynamic writer | required：MVC/MySQL/权限 | <Path>{roots.state}/specdev/changes/{change}/evidence/T-03.md</Path> |
| T-04 | Endpoint 元数据、继承和 URI 白名单 | T-03 | backend current/main | Lead 或 execution-time dynamic writer | required：MVC/MySQL/零请求安全 | <Path>{roots.state}/specdev/changes/{change}/evidence/T-04.md</Path> |
| T-05 | AES-GCM 密文和 scope 覆盖 | T-04 | backend current/main | Lead 或 execution-time dynamic writer | required：密文/MVC/log canary | <Path>{roots.state}/specdev/changes/{change}/evidence/T-05.md</Path> |
| T-06 | Redis 快照和集群失效 | T-03,T-04,T-05 | backend current/main | Lead 或 execution-time dynamic writer | required：双实例 Redis | <Path>{roots.state}/specdev/changes/{change}/evidence/T-06.md</Path> |
| T-07 | RestClient/@HttpExchange 安全 Pipeline | T-01,T-06 | backend current/main | Lead 或 execution-time dynamic writer | required：本地 HTTP/redirect/SSRF | <Path>{roots.state}/specdev/changes/{change}/evidence/T-07.md</Path> |
| T-08 | timeout、有限重试、双层 Redisson 门禁 | T-07 | backend current/main | Lead 或 execution-time dynamic writer | required：Redis/HTTP 计数 | <Path>{roots.state}/specdev/changes/{change}/evidence/T-08.md</Path> |
| T-09 | 出站日志、调用明细和聚合统计 | T-02,T-07,T-08 | backend current/main | Lead 或 execution-time dynamic writer | required：MySQL/SysLog/attempt | <Path>{roots.state}/specdev/changes/{change}/evidence/T-09.md</Path> |
| T-10 | domain-third 合同 | T-03,T-04,T-05,T-09 | frontend current/main | Lead 或 execution-time dynamic writer | not-required：headless package | <Path>{roots.state}/specdev/changes/{change}/evidence/T-10.md</Path> |
| T-11 | 四个 Admin 页面 | T-10 | frontend current/main | Lead 或 execution-time dynamic writer | not-required：组件测试；浏览器由 T-12 | <Path>{roots.state}/specdev/changes/{change}/evidence/T-11.md</Path> |
| T-12 | Admin 组合、菜单、权限 E2E | T-11 | frontend/root current/main | Lead 或 execution-time dynamic writer | required：浏览器+后端+MySQL | <Path>{roots.state}/specdev/changes/{change}/evidence/T-12.md</Path> |
| T-13 | Skills 同步与最终 release Gate | T-01..T-12 | root current/main；产品树只读 | Lead | required：全栈/full-core Gate | <Path>{roots.state}/specdev/changes/{change}/evidence/T-13.md</Path> |

## 3. Gates and Completion Evidence

### Overall Definition of Done

Lead 只有在所有非取消 Ticket 都形成 implementation commit、在对应 current workspace 完成定向检查、由 Lead 完成 direct-parent 验证并记录 parent result SHA，且 G1-G7 全部关闭后，才可将 change 标记 completed。任何 required E2E 未运行、失败、来源为 source-worktree、自报未复核、父 HEAD 漂移、shared path 越界、密文/日志 canary 泄漏或生产动作未批准，均不得完成。

### Gates

| Gate | 开启条件 | 关闭证据 | 阻塞范围 | Lead/批准人 | 失败恢复 |
|---|---|---|---|---|---|
| G0 执行授权 | 用户明确授权 implementation commit/direct-parent | 状态授权矩阵、当前分支和初始 SHA | 全部 Ticket | Lead/用户 | 保持 Plan blocked，等待授权 |
| G1 模块与数据 | T-01/T-02 定向检查通过 | API/Context、full/core 片段、MySQL fresh-init/约束 | T-03+ | Lead | 返回对应 Ticket，不推进父分支 |
| G2 管理与配置安全 | T-03..T-06 result 串行集成 | 权限/CRUD、URL 白名单、AES-GCM canary、Redis 失效/故障 | T-07+ | Lead | 保留 workspace，修复原 Ticket |
| G3 统一调用数据面 | T-07/T-08 result | local HTTP 零请求、registry、timeout/retry/Redisson 计数 | T-09+ | Lead | parent 不推进，重跑失败 Ticket |
| G4 观测闭环 | T-09 result | SysLog sink 脱敏、attempt/逻辑统计、MySQL 查询 | T-10+ | Lead | 保留历史，前向修复统计/日志 |
| G5 前端包与页面 | T-10/T-11 result | domain/web-domain test/type/lint/architecture | T-12 | Lead | 回退页面包，不开放菜单 |
| G6 App/菜单 | T-12 result | 61-third-dml.sql、manifest、权限、浏览器 E2E | T-13 | Lead/发布批准人 | 先撤菜单/权限，再修复组合 |
| G7 发布就绪 | T-13 全部验证 | AC-001..018、full/core、frontend、MySQL/Redis、Skills | change 完成 | Lead/发布批准人 | 保持 active，按偏差回到 owner |

### Contract and Reference Coverage

| 合同或参考要求 | 覆盖 Ticket | 验证接缝 | Evidence | 状态 |
|---|---|---|---|---|
| AC-001..005 Provider/Endpoint/凭据/缓存 | T-02..T-07,T-13 | MySQL/MVC/codec/Redis/Gateway | T-02..T-07,T-13 Evidence | covered |
| AC-006..010 格式、安全、registry、重试、限流 | T-04,T-07,T-08,T-13 | metadata/local HTTP/Redis/Context | T-04,T-07,T-08,T-13 Evidence | covered |
| AC-011..014 响应、类型化、统计、日志 | T-07,T-09,T-13 | adapter/Gateway/MySQL/SysLog | T-07,T-09,T-13 Evidence | covered |
| AC-015..016 Admin 菜单、页面、权限 | T-10,T-11,T-12,T-13 | package/manifest/MVC/browser | T-10..T-13 Evidence | covered |
| AC-017..018 删除/历史/full-core | T-01,T-02,T-03,T-04,T-13 | UseCase/MySQL/Maven bundles | T-01..T-04,T-13 Evidence | covered |
| Spring REST Client 官方边界 | T-01,T-07 | RestClient/@HttpExchange Context/mock | T-01,T-07,T-13 Evidence | covered |
| RuoYi/plus-ui 工程规范 | T-01,T-03,T-09,T-10,T-11,T-12,T-13 | module mode、architecture、Skills | T-01,T-10..T-13 Evidence | covered |

## 4. Execution and Integration Protocol

### Lead Orchestration

| 项目 | 决定 | 事实依据 |
|---|---|---|
| Lead | codex:/root | 唯一 SpecDev 状态、Evidence、父分支集成和最终验收 owner |
| Implementation subagents | 3 上限，Lead 不计入；current 实际同时写入锁为 1 | <Path>{roots.state}/specdev/config.json</Path> 的 max_implementation_agents=3 与 current 串行规则 |
| Integration attempts | null，表示 unlimited；仍受停止条件/偏差控制 | config max_integration_attempts=null 快照 |
| Read-only agents | 无 SpecDev 数字上限 | review/research/test-observation 只读，不写工件/产品 |
| Dispatch | execution-time dynamic | provider、模型、Ticket 是否派单按执行时能力和授权决定，不在本计划预分配 |
| Delivery channel | native 或 Lead；执行时显式锁定 | 本计划不授权 external-web，若选择外部通道必须另行满足 ZIP/授权合同 |

### Ticket Workspace and Integration

| Ticket | Parent/base | Workspace/branch | Source checks | Implementation commit | Integration checks/E2E | Parent result |
|---|---|---|---|---|---|---|
| T-01 | backend main@a63c83ac... | backend current/main | API/module tests | 82cb1febf | direct-parent integrated；isolated full/core bundle gate recorded in T-13 | 82cb1febf |
| T-02 | root SQL + backend | current/main | SQL static/schema tests | ec8cd78a + `751ecd439` | direct-parent integrated；isolated MySQL 8.4.9 constraints and canonical fresh-init pass | `751ecd439` |
| T-03 | backend T-01/T-02 result | backend current/main | provider tests | ec8cd78a | direct-parent integrated；provider CRUD/lifecycle checks pass | `751ecd439` |
| T-04 | backend T-03 result | backend current/main | endpoint/security tests | ec8cd78a | direct-parent integrated；URI/header/path zero-request checks pass | `751ecd439` |
| T-05 | backend T-04 result | backend current/main | credential/canary tests | ec8cd78a | direct-parent integrated；AES-GCM/canary and credential lifecycle checks pass | `751ecd439` |
| T-06 | backend T-05 result | backend current/main | resolver/cache tests | `751ecd439` | direct-parent integrated；isolated two-client Redis invalidation and fail-closed test 2/2 pass | `751ecd439` |
| T-07 | backend T-06 result | backend current/main | Gateway/mock HTTP tests | `751ecd439` | direct-parent integrated；loopback and Spring context status/redirect/SSRF/retry, endpoint SPI ownership, and typed transport tests pass | `751ecd439` |
| T-08 | backend T-07 result | backend current/main | adapter/resilience、port/support、Redisson tests | `751ecd439` | direct-parent integrated；Redis limiter and bounded retry checks pass | `751ecd439` |
| T-09 | backend T-08 result | backend current/main | observability/MySQL tests | `751ecd439` | direct-parent integrated；MySQL aggregate and production SysLog persistence canaries pass | `751ecd439` |
| T-10 | frontend main@587a629a... | frontend current/main | domain test/type/lint | d45a480 | direct-parent integrated；E2E not required | d45a480 |
| T-11 | frontend T-10 result | frontend current/main | web-domain component/type/lint | d45a480 | direct-parent integrated；E2E not required | d45a480 |
| T-12 | frontend T-11 + root SQL | frontend/root current/main | Admin manifest/architecture/build | `cc6a6f2` | direct-parent integrated；backend unauthenticated HTTP, browser permission E2E, and canonical fresh-init pass | `cc6a6f2` |
| T-13 | root + all product results | root current/main；产品树只读 | Skills/static/full Gate checks | completed release gate | direct-parent integrated；full/core, Redis, MySQL fresh-init, HTTP, SysLog, permission and browser checks pass | `339739d` |

current 模式严格一次一个 implementation writer。Ticket 只有在实际授权后，基于最新父 SHA 运行非 E2E 检查、形成非空 implementation commit，Lead 在同一 workspace 执行 direct-parent 集成/适用 E2E、重读父 HEAD 并记录 result SHA 后，才允许开始下一个 Ticket。不得创建 source/candidate worktree；不得把 subagent 自报或 source-worktree E2E 作为通过。

### Authorization Matrix

| 动作 | 状态 | 目标与条件 |
|---|---|---|
| Current workspace Ticket changes | authorized | USER-DECISION:2026-09-04-execute-goal-plan；仅在当前 workspace 按 writable/shared path 串行写入 |
| Ticket worktree local changes | not-authorized | 本计划选择 current，禁止创建 worktree |
| Implementation commit | authorized | USER-DECISION:2026-09-04-execute-goal-plan；每 Ticket 创建非空本地 commit |
| Local direct-parent verification and parent update | authorized | Lead-only；当前 workspace clean、可复查且完成适用 E2E 后执行 |
| Local candidate integration and parent update | not-authorized | current 模式不适用，保持禁止 |
| Push / PR / remote merge | not-authorized | 不从本地计划继承授权 |
| Branch/worktree cleanup | not-authorized | 不自动清理用户工作区或分支 |
| Deploy / migration / production actions | not-authorized | 生产 DDL/DML、菜单赋权、主密钥、凭据和真实 provider 均需逐动作批准 |

### Evidence Return

implementation owner 只返回 Ticket ID、workspace、base/implementation commit、dirty 状态、实际路径、定向命令/结果、未验证项、冲突和恢复条件；review/research/test-observation 只返回只读事实。Lead 必须重读 Git、diff、命令和运行环境后，独立写入 <Path>{roots.state}/specdev/changes/{change}/evidence/{ticket-id}.md</Path> 及状态，任何 subagent 不得写 SpecDev 工件、Evidence、父分支或 E2E Gate。

## 5. Constraints, Risk and Recovery

### Non-negotiable Constraints

- Provider disabled 优先于 Endpoint；配置/Redis/凭据状态未知时 fail-closed，发送前返回 PROVIDER_DISABLED、ENDPOINT_DISABLED、RATE_LIMITED 或 CONFIG_UNAVAILABLE。
- DB 是事实源；Redis 仅使用版本化有效快照，变更成功后立即精确失效并确认集群通知。
- Endpoint 只能是经校验的相对 path 和白名单参数；拒绝绝对 URL、//host、穿越、脚本/表达式、任意 header、跨域 redirect 和任意类反射。
- Provider/Endpoint 凭据分 scope，Endpoint 同类型覆盖 Provider；DB 只存 AES-GCM 类认证密文，主密钥只来自 yml/外部配置。
- RestClient 负责统一连接/超时/状态/转换；固定源码合同使用 @HttpExchange，动态 Endpoint 使用通用 RestClient；WebClient 仅后续响应式能力。
- 只有明确幂等 Endpoint 才能有限重试；非幂等最多发送一次；Provider/Endpoint Redisson 限制和并发拒绝发生在发送前。
- 物理 attempt、逻辑结果和 rejected 分开统计；全量 third 出站自动进入 common SysLog writer/sink，服务端黑名单不可关闭，正文受大小上限。
- 管理端 third 包独立于 system 源码，菜单仅投影到 System Management；后端权限是最终权威。

### Verification Integrity

判定接缝必须是实际边界：MySQL/Redis 使用真实属性门控基础设施，本地 HTTP 使用可计数 server 验证零请求/attempt，日志使用可捕获 sink 和 canary，浏览器使用动态菜单/权限真实后端。禁止删除测试、放宽断言、吞错、把完整 raw body 当稳定业务结果、以 source-worktree E2E 代替 parent 验证，或让 mock/截图单独宣称通过。

current/direct-parent 每个 Ticket 的基线、implementation commit、父 HEAD、命令退出码、E2E 运行环境、未运行项、失败分类、偏差和残余风险必须进入对应 Evidence。父 HEAD 漂移或 direct-parent 失败时不推进父分支。

### Migration or Release Sequence

1. 先在后端 ruoyi-api/module POM 和 NAMEWTA 50 DDL 建立 additive 地基。
2. 依次合入 Provider、Endpoint、凭据、快照、Gateway、韧性、观测后端能力。
3. 建立独立 domain-third/web-domain-third，完成 Admin composition、lock 和 61-third-dml.sql。
4. fresh-init 顺序为基础 10 → third 50 → third baseline DML 60 → third menu/permission 61；生产先部署代码，再经批准开放菜单/角色和 Endpoint。
5. 以本地 mock/test canary 执行全栈 Gate；不输入真实 provider secret。
6. 全部 Gate 通过后才同步 Skills 事实并形成 release-ready 结论。

### Risks, Monitoring and Recovery

- **SSRF/redirect/header 绕过：** 监控 zero-request security tests、拒绝分类和目标 origin；失败时禁用 Endpoint，返回 T-04/T-07。
- **密文/日志泄漏：** 监控 canary、redaction/truncation、异常序列化和 SysLog sink；发现即停 G2/G4，不写入或保留明文。
- **缓存陈旧/失效超时：** 监控 cache version、ack timeout、CONFIG_UNAVAILABLE；不使用 stale，清理 third namespace 后前向恢复。
- **重试/计数错误：** 监控 attempt 守恒、requestId、permit leak 和 provider 业务错误；回退 T-08/T-09，不修改历史伪造统计。
- **菜单/权限漂移：** 监控 unknown component、403、SQL/manifest diff；先撤 61-third-dml.sql/角色，再修 T-12。
- **父分支漂移/集成失败：** 保留当前 commit 与 workspace，标记 direct-parent failed/stale，从最新父 SHA 重跑。

### Deviation Control

遵循 <Path>{roots.workflows}/specdev/common/rules/deviation-control.md</Path>：local 偏差写 Evidence 后可继续；改变 Ticket 路径/契约须停止并修订 Ticket；改变 Spec/ADR/发布或生产权限必须返回对应上游 owner 并获得批准。任何越界写入、真实 provider、生产密钥或未授权 migration 都是停止条件。

## 6. Progress and Decisions

### Current Status

#### Execution update (2026-09-04, superseded by 2026-09-05)

The user authorized implementation commits and direct-parent integration with decision `USER-DECISION:2026-09-04-execute-goal-plan`. T-01 through T-12 are integrated; T-13 is the active release gate, all owned by `codex:/root`. This historical checkpoint used backend `2dacefcfb`, frontend `d45a480`, root menu `bed64ec`, and DDL `c49f562`; its external-gate pending statements are superseded by the 2026-09-05 verification below.

Completed local gates in this historical checkpoint included backend module compile, module-mode, loopback Gateway, frontend package checks, architecture, and Admin build. The remaining external gates at that time were subsequently executed where the current workspace permitted; see the 2026-09-05 section.

The historical release bundle result is superseded by the current `751ecd439` export and package copies recorded below.

Latest execution checkpoint for this historical section was backend `2dacefcfb`; current evidence is maintained in the next section.

#### Follow-up verification (2026-09-05)

Backend `751ecd439` was revalidated with `./mvnw.cmd -P local -q test` (exit 0; 32 `ruoyi-third` tests, 0 failures/errors), layered module validation (71 Java files), real MySQL 8.4.9 isolated constraint tests (1/1), real Redis/Redisson tests (2/2), Spring application-context Gateway/SPI tests (3/3), direct unauthenticated admin HTTP tests (1/1), and the production SysLog file-pipeline canary (1/1). The context tests cover no-data startup, duplicate/missing adapter validation, typed `@HttpExchange`, provider business-error mapping, 503/no-retry, 302 refusal, SSRF zero-request rejection, non-idempotent single attempt, and bounded idempotent retry. Frontend `cc6a6f2` passes full workspace `pnpm typecheck`, `pnpm lint`, `pnpm test`, and `pnpm build:prod`; Admin has 40 passing tests, architecture is 101/101, and the dedicated browser permission suite is 2/2. The release-artifacts static suite reports 31 passing tests. SpecDev `tickets` and `implement` validators remain at 0 errors / 0 warnings.

当前执行状态以本节和 `.status.json` 为准：用户已通过 `USER-DECISION:2026-09-04-execute-goal-plan` 授权本地实现、验证、Evidence 与 direct-parent 更新；远程、部署、生产迁移、凭据写入和真实供应商调用仍未授权。所有实现与 release Gate 已完成。

- Goal Plan：`completed`，`ready_for_execution=true`；T-01 至 T-13 均已形成实现提交并完成定向、真实服务和 release 检查。
- 集成状态：T-01 至 T-13 均为 `integrated/done`；所有实现工作树使用 current/direct-parent，没有创建 worktree。
- 最新检查点：backend `751ecd439`，frontend `cc6a6f2`；DDL、CI 与 bundle 校验脚本已更新在当前 root 工作树；tickets/implement 校验均为 0 error/0 warning，release-artifacts 静态套件 31 项通过。
- 外部 Gate：随机隔离 MySQL 约束、Redis 双客户端失效/限流、确定性 HTTP 应用上下文、SysLog 持久化、后端未授权 HTTP 和浏览器权限 E2E 均已通过。full/core 完整构建已在 `D:\Document\code\namewta-release-verify-751ecd439-zip` 导出目录通过，且 `BOOT-INF/lib` 组合已核对。canonical MySQL 8.4 fresh-init 已在隔离远端 Docker 环境成功完成，116 张表、5 张 `third_*` 表、三方管理菜单和 15 项权限已复核，临时资源已清理。

### Pending Decisions and Blockers

- **当前阻塞：** 无。canonical MySQL 8.4 fresh-init、exact-schema MySQL 1/1 和双客户端 Redis 2/2 均已通过，且隔离环境已清理。
- **仍保持禁止：** worktree、remote push/PR/merge、生产 DDL/DML/赋权、凭据/主密钥写入、真实供应商调用。
- 外部服务验证已完成；后续仅按已批准的生产迁移、角色赋权、凭据和真实供应商接入流程推进，不改变本 change 的完成态。

### Resume Protocol

恢复时读取本 Goal Plan、Tickets Map、当前 Ticket、change .status.json、三个仓库的最新 branch/HEAD/status 和最新 Evidence；从最后通过的 implementation/direct-parent result 继续。先确认父 HEAD 未漂移、workspace 仍干净/边界明确，再执行下一个 Ticket。若发生共享合同、路径、Spec/ADR、安全、验证接缝或发布决策变化，暂停受影响 Wave，按 deviation-control 返回真正 owner。

## Assumptions

- 低影响假设：当前 backend/frontend/root 默认分支均为 main，基线 SHA 已在创建计划时读取；执行前仍需重读。
- 低影响假设：项目配置声明的 Maven/pnpm 命令在实施环境可用；实际退出码和未运行原因必须写 Evidence。
- 没有把真实供应商协议、secret 或业务码作为假设；未提供部分保持 OOS-001。
