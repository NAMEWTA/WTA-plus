---
schema_version: 3
plan_contract_version: 1
plan_revision: 5
requested_deliverables: [{"name": "Java AI Maven占位模块", "count": 2}, {"name": "MySQL初始化基座文件", "count": 6}]
deliverable_policy: "用户最终共识明确保留两个Java占位和六份SQL；两项业务目标由Spec覆盖；无指定Ticket数，不从票数推断业务产物数。"
artifact: "tickets-map"
change: "2026-09-19-remote-issues-phone-ai"
status: "in_progress"
---

# Tickets Map: 手机号必填与 Snail AI 退出

- **Map：** <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/tickets-map.md</Path>
- **Spec：** <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/spec.md</Path>
- **Ticket目录：** <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/ticket/</Path>
- **Evidence目录：** <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/evidence/</Path>
- **Goal Plan：** <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/goal-plan.md</Path>（本次P的正式编排入口）

## 1. 目标与拆分策略

US-001–003由T-01贯穿两端表单、domain、HTTP/导入写入；US-004由T-02交付用户可见退出；US-005与整体验收由T-03在前置稳定后收缩服务、发布和共享初始化/生成合同。三个切片都有可观察结果和各自回归；没有为技术层单独造票或为纯美化引入prefactor。

### 总体实施背景

本change只处理手机号必填与SnailAI退出，Go/Python已归独立暂缓change。旧空手机号仍可登录，资料写入形成有效号码；旧sai_*数据不迁移、不删除。wta-ai/common-ai保留Maven占位；六份SQL固定槽位不变；40槽位不得含vendor表/数据。SnailJob、SpringAI BOM/common-mcp与NAMEWTA OpenAPI不是退出对象。

T-01/T-02完成各自消费者后，T-03统一更新当前OpenAPI、50基座和项目事实，避免生成物/SQL多writer。前两票的不可变旧快照仍只代表原捕获来源，不能冒充最终后端合同；完整change的当前合同与release门直到T-03才能关闭。每票必须自身构建和行为回归可验证，不用后续修复容忍红灯；若中间点无法保持本票绿色，停止并由Lead重新拆分，不悄悄放宽检查。

### 项目 Skill 读取矩阵

Lead/implementation owner始终先读完整Map，再读匹配Skill入口和scope references，最后读本Ticket。矩阵是最低路由，不是allowlist；frontmatter skill_bindings是实际调用合同。

| Applies To | Project Skill | Trigger / Scope | Read Timing | Purpose |
|---|---|---|---|---|
| ALL | <Path>.agents/skills/engineering-standards/SKILL.md</Path> | 边界、命名、后端classic、API/安全/SQL、质量和交付裁决 | Map后Ticket前；随后按绑定implement/verify实际执行 | 形成调用/实现/验证证据，失败block-ticket |
| ALL | <Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path> | 账号或退役菜单/配置/数据库的跨层合同与验证 | Map后Ticket前；随后按绑定implement/verify实际执行 | 形成调用/实现/验证证据，失败block-ticket |
| T-01 | <Path>.agents/skills/java-api-compatibility/SKILL.md</Path> | RegisterBody公共DTO的验证语义演进，保留签名与省略/null兼容 | Map后Ticket前；随后按绑定implement/verify实际执行 | 形成调用/实现/验证证据，失败block-ticket |
| ALL | <Path>.agents/skills/wta-module-guide/SKILL.md</Path> | System用户/菜单、AI/Extend模块事实与公开调用边界 | Map后Ticket前；随后按绑定implement/verify实际执行 | 形成调用/实现/验证证据，失败block-ticket |
| ALL | <Path>.agents/skills/wta-common-modules-guide/SKILL.md</Path> | 校验/导入公共入口、AI vendor与MCP/SnailJob保留边界 | Map后Ticket前；随后按绑定implement/verify实际执行 | 形成调用/实现/验证证据，失败block-ticket |

## 2. 执行清单

| ID | Ticket | 可观察产出 | Blocked By | Depth | Risk | Ready | Owner | Contract IDs | Wave/Gate | Status |
|---|---|---|---|---|---|---|---|---|---|---|
| T-01 | <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/ticket/01-required-phone-write-paths.md</Path> | 注册、管理新增/编辑、个人资料与新增/覆盖导入形成一致手机号写入合同，旧空号用户仍可登录。 | — | deep | high | yes | codex-root | AC-001, AC-002, AC-003, AC-004, AC-005 | W1/G-phone | in_progress |
| T-02 | <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/ticket/02-retire-snail-ai-business-surface.md</Path> | Admin 不再呈现聊天/控制台，也不访问旧注册桥；Java 两个 AI artifact 仅保留可构建占位。 | — | deep | high | yes | codex-root | AC-006, AC-007, AC-008 | W1/G-ai | ready |
| T-03 | <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/ticket/03-remove-snail-ai-release-and-baseline.md</Path> | 最终源码与本地发布候选不产出或启动SnailAI；新库无vendor表，旧数据原样保留，当前API/基座/文档与两个前置切片一致。 | T-01, T-02 | deep | high | yes | codex-root | AC-009, AC-010, AC-011, AC-012 | W2/G-final | ready |

Ticket frontmatter是状态、依赖、路径与绑定权威；本表仅投影。

## 3. 依赖 DAG

```text
T-01 ──┐
       ├──> T-03 ──> G-final
T-02 ──┘
```

T-03必须等待手机号实际合同与AI消费者退出稳定后，才能统一更新当前API、基座、发布库存并验收最终候选。T-01/T-02无业务依赖，因用户选择current仍按T-01→T-02串行，不把调度偏好伪造成blocked_by。

## 4. 合同覆盖矩阵

| Contract ID | 覆盖 Ticket | 验证接缝 | 状态 | 说明 |
|---|---|---|---|---|
| AC-001 | T-01 | S1/S2 | covered | 计划覆盖，不代表执行通过 |
| AC-002 | T-01 | S1/S2 | covered | 计划覆盖，不代表执行通过 |
| AC-003 | T-01 | S1/S2 | covered | 计划覆盖，不代表执行通过 |
| AC-004 | T-01 | S1/S2 | covered | 计划覆盖，不代表执行通过 |
| AC-005 | T-01 | S1/S2 | covered | 计划覆盖，不代表执行通过 |
| AC-006 | T-02 | S1/S2/S3/S4 | covered | 计划覆盖，不代表执行通过 |
| AC-007 | T-02 | S1/S2/S3/S4 | covered | 计划覆盖，不代表执行通过 |
| AC-008 | T-02 | S1/S2/S3/S4 | covered | 计划覆盖，不代表执行通过 |
| AC-009 | T-03 | S1–S5 | covered | 计划覆盖，不代表执行通过 |
| AC-010 | T-03 | S1–S5 | covered | 计划覆盖，不代表执行通过 |
| AC-011 | T-03 | S1–S5 | covered | 计划覆盖，不代表执行通过 |
| AC-012 | T-03 | S1–S5 | covered | 计划覆盖，不代表执行通过 |

## 5. 并行与路径所有权

- 用户已选择current/direct-parent，严格串行；Lead codex-root为SpecDev与父分支唯一owner，每次仅一个implementation writer。
- T-01独占手机号DTO/业务写入与表单映射；T-02独占AI业务模块、Admin组合、AI前端依赖锁；T-03独占root POM、release、40/50基座、当前OpenAPI及父Skill事实。
- T-01/T-02仅提交共享文件变更需求给T-03，不写其文件。T-03不得借release广域授权修改用户运行数据、历史产物或无关服务。
- 实现agent上限由Goal取config与当前策略的更低值；review/research/test-observation只读，无SpecDev固定数值上限。

| Ticket A | Ticket B | Writable交集 | 真实依赖 | 处理 |
|---|---|---|---|---|
| T-01 | T-02 | 无 | 否 | current单writer串行 |
| T-01 | T-03 | 无；API/SQL由T-03统一owner | 是 | T-01验收后T-03消费合同 |
| T-02 | T-03 | 无；业务配置与release资产不同owner | 是 | T-02验收后收缩独立服务 |

## 6. Gate、Wave 与集成点

候选W1含T-01/T-02但逐票串行；W2为T-03。G-plan核对真实授权/基线，G-phone与G-ai关闭行为，G-final关闭六基座/数据保留/当前API/同源构建。正式Gate条件由<Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/goal-plan.md</Path>定义，E2E均由Lead在current-workspace验收。

## 7. 横切契约与风险

部分更新不能用全局NotBlank替代合并；系统未知菜单诊断不得整体隐藏；移除SnailAI不等于清历史数据。只有经过正式fetch/generate的当前API可成为最终合同。保护原始快照与旧release。无生产Tag/部署/迁移权限，不能把计划写成已部署。

环境事实：Node24.21.0、Java21.0.12.1、Docker29.7.2；frontend目录corepack pnpm实测10.34.5，root默认pnpm9.15.9不可代替。frontend命令统一以该目录为cwd使用corepack pnpm。

## 8. 同步规则

状态从Ticket投影；依赖/路径/Skill摘要变化先回读真实源、更新本Map、递增plan_revision并重跑校验。T-03会修改common Skill事实，应记录入口摘要变化原因和执行前后证据，由Lead刷新受影响绑定；历史Evidence不重写。当前Goal不拥有新行为决定。

## 9. 总控与恢复

唯一入口是本Map。先运行：

```bash
node <Path>{roots.workflows}/specdev/common/tools/ticket-control.mjs</Path> --map <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/tickets-map.md</Path> --repo <project-root>
```

随后按<Path>{roots.workflows}/specdev/P-goal-plan/P-goal-plan.md</Path>选择plan/run/resume/replan/verify。控制器只读，不执行也不授予权限。本次只plan；实现、commit和父分支推进待真实授权，缺失时不自动进入I。

恢复需重读Spec/Map/当前票/Goal/状态/最新Evidence、Git与Skill摘要；不得接管旧comprehensive-review或Go/Python暂缓change。每票完成仍须非空commit、current-workspace direct-parent验证和result；全部done仍须G-final及两个明确数量验收。无修改票应cancelled，不伪造empty commit。

实施修订 revision 3：T-01 纳入直接调用注册持久化的现有 OSS 测试单文件，只补合法手机号夹具、保留原断言；不改其他 OSS 行为。用户已授权本地实现，提交前确认待答复。

实施修订 revision 4：原默认Playwright配置明确忽略registration/transport专项；T-01修正为真实专项入口，增加phone双App配置并从默认配置排除其用例，两个配置路径归T-01。不得把默认零发现当E2E通过。

实施修订 revision 5：T-01 纳入既有 PasswordAuthStrategyTemporaryUnitTest 单文件，把原无手机号成功认证用例显式参数化为 null/空串/空白；保持真实 authenticate 边界与原密码和授权断言。pnpm 根脚本会调用裸 pnpm，验证时通过任务专属 Corepack shim PATH 保证子进程同为 10.34.5，不修改系统安装。
