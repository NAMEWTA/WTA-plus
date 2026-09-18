---
schema_version: 1
artifact: learning-goal-plan
change: 2026-09-16-namewta-fullstack
topic: fullstack
project_path: /srv/WTA-plus
status: draft
modes: [replan]
orchestration: external-goal
run_orchestration: lead-directed
coverage_domain: programming
expression_level: eli5
coverage_depth: deep
mine_unit: project
mine_unit_cap: 15
learning_agent_limit: 4
max_waves: 2
ready_for_execution: false
---

# Goal-Plan: NAMEWTA 前后端全模块

本文件由 G-goal 在 `replan` 会话编译。计划正文里的「允许」不构成执行授权。`ready_for_execution: false`，直到用户把本计划交给外部 AI CLI `/goal`。

旧计划只读归档：`<Path>{roots.state}/learning/changes/2026-09-16-namewta-fullstack/goal/revisions/REV-001.md</Path>`（`wave_cap` / 18 波 / 一课一挖）。本次对齐最新 G-goal：覆盖波最多 2，挖掘按 mine unit（每个 ≤15），运行形状是 teach-then-mine。

四门原料课已于 `2026-09-16T07:53:45.456Z` 嵌入 `children/`。`/goal` 可以升级那些 Lesson 文件；不得把它们未 mine 就标 `covered`。

## 路径

- 本计划：`<Path>{roots.state}/learning/changes/2026-09-16-namewta-fullstack/goal/goal-plan.md</Path>`
- Chain：`<Path>{roots.state}/learning/changes/2026-09-16-namewta-fullstack/goal/chain.md</Path>`
- 覆盖矩阵：`<Path>{roots.state}/learning/changes/2026-09-16-namewta-fullstack/goal/coverage-matrix.md</Path>`
- 进度：`<Path>{roots.state}/learning/changes/2026-09-16-namewta-fullstack/goal/progress.md</Path>`
- 挖掘探针：`<Path>{roots.state}/learning/changes/2026-09-16-namewta-fullstack/goal/probes/</Path>`
- 验收：`<Path>{roots.state}/learning/changes/2026-09-16-namewta-fullstack/goal/verify.md</Path>`
- 旧计划：`<Path>{roots.state}/learning/changes/2026-09-16-namewta-fullstack/goal/revisions/</Path>`
- 课程地图（A 所有）：`<Path>{roots.state}/learning/changes/2026-09-16-namewta-fullstack/course.md</Path>`
- 讲义（L 所有）：`<Path>{roots.state}/learning/changes/2026-09-16-namewta-fullstack/lessons/</Path>` 以及 `children/<child-id>/lessons/`
- 问答课（Q 所有，`/goal` 不得写入）：`<Path>{roots.state}/learning/changes/2026-09-16-namewta-fullstack/inquiry/</Path>`

---

## §0 `/goal` 粘贴块

把下面整块复制到 AI CLI 后发送 `/goal`。不要改写 Outcome 或 HARD NO。

```text
/goal READ <Path>{roots.state}/learning/changes/2026-09-16-namewta-fullstack/goal/goal-plan.md</Path>

Outcome:
  范围内覆盖矩阵格子全部变为 covered | deferred(reason) | covered-by-parent；
  每个 chain 节点有一份 L 合同 Lesson；
  每节新课或改写课至多两份 goal/probes/GP-*-b0N.md（每课 ≤10 问）；
  写好 goal/verify.md。

Verification surface:
  goal/coverage-matrix.md 状态列；
  lessons/L-*.md 或 children/*/lessons/L-*.md 存在且满足 L-lesson 合同；
  goal/probes/GP-*-b0N.md 关闭态且至少改变过一个矩阵格子，或显式 defer / split-proposal；
  goal/verify.md 对照 stop-rules 逐条勾选。

Constraints:
  roots 只来自已打开的 workspace.json；
  只写当前 Change 树（本根 lessons/ 与已嵌入的 children/*/lessons/）以及本根 goal/；
  派单跟随 L-lesson 与 socratic-questioning audience=mine；
  不发明库存里没有的函数；
  expression_level=eli5；coverage_depth=deep；
  mine unit ≤15；每课最多 10 问。

Boundaries:
  不写 inquiry/、homework/、review/、synthesis/、archive/；
  不写 mastered；
  不复活 Q-quiz；
  不把 mine 写成学习者问答课；
  不自动激活 H/R/C/A-archive；
  不把 wta-extend 独立进程、厂商引擎内部、demo 能力展示 Controller 扩进 in-scope；
  不调用 C-consolidate 或 relocate-learning.mjs。

Iteration policy:
  先按 goal/chain.md 的当前 mine unit 写完该单元全部 L（阶段 T），再按课扇出 mine（阶段 M）；
  禁止 L → mine → L → mine 交错；
  并行只发生在写集不相交的子代理之间；矩阵、chain、progress、verify、lessons/INDEX 只有 Lead 可写；
  处置：re-dispatch-L | split-lesson | defer(reason)；
  mine-more 只允许在该课尚未满 10 问、且 batch-01 已证明存在真实矩阵缺口时，用来打开 b02；
  满 10 问后 mine-more 非法；
  当前 mine unit 课数不得超过 15。超过则停止，回到 G-goal replan 重新切单元。

Blocked-stop:
  本单元矩阵无变化；出现第 11 问；交错 L/mine；miner 写入 lessons/ 或 matrix；
  同一 unit 规划超过 15 节；用户暂停；
  缺源码 / 缺 baseline / 目标无法核对；所有权冲突或路径越界。
```

最小读取顺序见 §11。

---

## §1 Outcome and Authority

### Outcome

合格读者读完本 Goal 后，能用范围内源码与已写成的 Lesson 说明：系统 Context 与 Container、一条主路径与一条失败路径、范围内公开/领域函数的目的，以及公开 API / 枢纽方法的副作用与失败路径。完成证据是矩阵闭合 + Lesson + probe + `goal/verify.md`，不是「课写完」。

### Success and False Completion

算完成：stop-rules 的「完成即停」全部满足。

不算完成：

- 课写完
- 文件巡览结束
- 模型表示「已经理解」
- 主观百分比
- 只生成了 Goal-Plan 本身
- 只把现有四门课原样标成 covered
- 阶段 T 结束但尚未 mine

### Non-goals

- 不自动激活 `H-homework`、`R-review`、`C-consolidate`、`A-archive`
- 不写 mastered
- 不复活 `Q-quiz`
- 不使用 `Q-question` 的 `inquiry/` Response 协议
- 不为琐碎 helper、生成代码、测试夹具单独开课
- 不在本计划里预写探针题目或深化讲义
- 不在本会话写 `lessons/` 或 `goal/probes/`

### Authoritative Inputs

| 优先级 | 输入 | 用途 |
| --- | --- | --- |
| 1 | 用户当场决定 | 覆盖范围、暂停、defer |
| 2 | `course.md` OBJ | 权威学习目标；本文件不改写 OBJ |
| 3 | `goal/coverage-matrix.md` | 格子状态是覆盖权威 |
| 4 | `goal/chain.md` | 课程序列投影与 mine unit 切分 |
| 5 | 本计划派单菜谱 | `/goal` 只按 §5 / §7 行动 |
| 6 | 源码事实 | 库存与证据；不得发明符号 |

---

## §2 Scope

### In-scope

- 项目路径：`/srv/WTA-plus`
- 必须覆盖的容器：`admin-web`、`home-web`、`sso-web`、`wta-admin`、`wta-modules` 已登记业务模块、`wta-api`、主路径上的 `wta-common-notify` / `wta-common-mybatis` / `wta-common-satoken` / `wta-common-openapi`、MySQL 8.4 基座、Redis 会话、MinIO/OSS
- 模块 / 公开 API：见 `goal/coverage-matrix.md` (a)(b)。后端以 Controller / UseCase（layered）或 Controller + 枢纽 Service（classic）为准；前端以 `create*Service` / `create*WebDomain` / App `application/services.ts` 为准

### Out-of-scope

- `backend/wta-extend/**` 独立可部署进程内部
- WarmFlow / SnailJob / SnailAI 厂商引擎内部
- Nacos 服务器产品；`wta-common-nacos` 标 `deferred(optional-overlay)`
- 生成物与依赖缓存
- speculo / skills 运行时
- demo 能力展示 Controller（见 course 范围外）

### covered-by-parent / deferred

- 琐碎 helper、生成代码、测试夹具：`covered-by-parent` 或 `deferred(reason)`
- 「每一个函数」= 范围内公开/领域函数，不是仓库里每一个私有 one-liner
- 同一资源的 list/get/add/edit/remove 必须在对应课的函数表里出现；矩阵按 Controller/工厂聚合一行，课内仍要逐方法解释
- 不改变任何覆盖格子的 Lesson 或 probe 不要写

### 现有课升级合同

| 源 Change | 现状（已在 `children/`） | `/goal` 必须补的缺口 |
| --- | --- | --- |
| `2026-09-14-namewta-architecture` | 6 课，`standard` | 升 deep；L-004 写清三 App；L-006 补失败路径存储点 |
| `2026-09-14-wta-sso` | 3 课，后端 deep，前端出范围 | 保留后端走查；另写 L-058/L-059 覆盖 sso-web 与 `wta-api` |
| `2026-09-14-wta-notify` | 8 课，后端 deep，前端出范围 | 保留后端走查；另写 L-053/L-054 覆盖前端 |
| `2026-09-14-wta-third` | 5 课，后端 deep，前端出范围 | 保留后端走查；另写 L-065/L-066 覆盖前端 |

C-consolidate 已 apply（`synthesis/relocation-20260916075345.json`）。`/goal` 不得再搬迁，不得调用 `relocate-learning.mjs`。`goal/verify.md` 仍是 C 完成前的过程即停记录，停止规则再次触发时由 `/goal` Lead 重写。

### C4 库存快照

2026-09-16T08:47:16.000Z 抽查：`backend/wta-modules` 仍含 ai/demo/job/notify/profile/sso/system/third/workflow；`frontend/apps` 仍含 admin-web/home-web/sso-web；`frontend/packages/domains` 与 `web-domains` 仍含 admin/ai/demo/notify/profile/system/third/workflow；MySQL `10-cde-base-ddl.sql` / `50-cde-base-dml.sql` 仍在。矩阵 (a)(b) 符号名单保持 REV-001 库存，不另造第二份。

```text
[Context]
  管理操作者 --uses--> admin-web
  门户用户 --uses--> home-web
  SSO 客户端 --uses--> sso-web + /sso/oauth2
  机器调用方 --uses--> OpenAPI HMAC
  短信/邮件供应商 --callback--> /notify/callback/{channel}
  业务模块 --uses--> ThirdPartyGateway --> 外部 HTTP
  NAMEWTA --stores--> MySQL 8.4
  NAMEWTA --sessions/locks--> Redis 8
  NAMEWTA --objects--> MinIO/OSS

[Container]
  admin-web (Vue App, 组装全部业务 web-domain)
  home-web (Vue App, 身份+资料)
  sso-web (Vue App, 认人厅)
  wta-admin (Spring, AuthController + 模块组装)
  wta-modules/* (layered|classic 业务房间)
  wta-api (跨模块 Java 合同)
  wta-common-* (工具间；本 Goal 只覆盖主路径枢纽)
  MySQL 8.4 (10-cde-base-ddl / 50-cde-base-dml)
  Redis 8
  MinIO/OSS

[Happy path]
  浏览器准备登录 -> GET /auth/client/context + /auth/code
    -> POST /auth/login -> Sa-Token+Redis 会话
    -> GET /system/menu 动态路由
    -> domain list GET -> Controller -> (UseCase|) Service -> Mapper XML -> MySQL
    -> web-domain 表格渲染

[Fail path]
  缺 Client 上下文 / 验证码失败 / 密码策略拒绝
    -> AuthController 返回错误，不写会话
  或 OSS complete 失败 -> abort 票据，不留下业务引用
  或 SSO PKCE 不匹配 -> 不发业务令牌
  或 Outbox claim 失败 -> 投递保持待领取，不假装已发送
```

范围内函数按模块归组，清单写在 `goal/coverage-matrix.md` 的 (a)(b) 表，不在本节约维护第二份过期名单。

---

## §3 Coverage Contract

v1 只实现编程四轴。默认档与证据规则以 `<Path>{roots.workflows}/learning/G-goal/references/coverage-bar.md</Path>` 为准。本 Goal 用户选择 `coverage_depth=deep`：每个 in-scope 容器/模块有对应课，主路径与失败路径按模块列出。深度靠课粒度，不靠把覆盖波扩成 18 个挖掘门。

| 轴 | 对象 | 默认 SOLO | 证据 |
| --- | --- | --- | --- |
| (a) 函数目的 | 范围内公开/领域函数 | Unistructural + explained | Lesson 函数表或父课 covered-by-parent |
| (b) 方法性状 | 公开 API 与枢纽内部方法 | Unistructural + explained | 副作用、失败路径、幂等、并发、纯/不纯 |
| (c) 业务架构 | 系统与容器 | Relational | C4 Context + Container + 关键 Component 的 ASCII |
| (d) 数据流 | 主路径与失败路径 | Relational | 源 → 变换 → 汇 + 存储点 + 所有权 |

Extended Abstract 不是完成条件。

---

## §4 Chain, Waves, and Mine Units

权威序列与 mine unit 切分在 `goal/chain.md`。本计划只记录覆盖波与挖掘单元门。Wave 是内容分组，不是挖掘门，也不是并发授权。

| Wave | 焦点 | Lessons | 分组尺 | Gate |
| --- | --- | --- | --- | --- |
| 1 | C4 Context + Container + 主/失败数据流 | L-001 … L-006 | 架构与数据流 | G1/G2/G3 |
| 2 | 范围内公开 API 与方法性状 | L-007 … L-085 | 公开 API | G1/G2/G3 |

| Mine unit | Lessons | 上限 | 状态 |
| --- | --- | --- | --- |
| U1 | L-001 … L-006（总览升级） | 6 / ≤15 | planned |
| U2 | L-007 … L-014（前端平台 + 认证） | 8 / ≤15 | planned |
| U3 | L-015 … L-024（system） | 10 / ≤15 | planned |
| U4 | L-025 … L-033（OSS + OpenAPI） | 9 / ≤15 | planned |
| U5 | L-034 … L-044（profile） | 11 / ≤15 | planned |
| U6 | L-045 … L-054（notify 升级 + 前端） | 10 / ≤15 | planned |
| U7 | L-055 … L-066（sso + third） | 12 / ≤15 | planned |
| U8 | L-067 … L-081（workflow / demo / job / ai） | 15 / ≤15 | planned |
| U9 | L-082 … L-085（common + MySQL） | 4 / ≤15 | planned |

`mine_unit: project`，`mine_unit_cap: 15`（上界，不是配额）。85 节 > 15，故切成 9 个连续单元。后一单元的阶段 T 等前一单元 D 结束（或至少等前置课 `mined` / `revised`）。切分规则读 `<Path>{roots.workflows}/learning/G-goal/references/mine-unit.md</Path>`。

跨单元前置：U2←U1；U3←U1；U4←U1；U5←U1；U6←U1；U7 的 SSO 课←U3 的 L-018；U8 的 L-076←U4 的 L-029；U9 的 L-082←U6 的 L-051，L-084←U2 的 L-013。

---

## §5 Dispatch Recipe

`/goal` 的 `run` / `resume` 只读本节与 §7 / §8。计划会话不得执行本节。编排读 `<Path>{roots.workflows}/learning/G-goal/references/lead-orchestration.md</Path>`。拆课读 `<Path>{roots.workflows}/learning/G-goal/references/split-rules.md</Path>`。

运行形状是 **teach-then-mine**，不是一课一挖交错：

1. **阶段 T**：按当前 mine unit 为每课派一个 L 子代理。跟随 `<Path>{roots.workflows}/learning/L-lesson/L-lesson.md</Path>` 与 `<Path>{roots.workflows}/learning/common/rules/teaching-policy.md</Path>`，写入 chain 表「将写入」列的路径（父根 `lessons/` 或已嵌入的 `children/*/lessons/`）。`lessons/INDEX.md` 只有 Lead 可写。本单元全部 planned 节点都有合格 L 合同讲义后，才进入阶段 M。
2. **阶段 M**：为每课派一个 miner。跟随 `<Path>{roots.workflows}/learning/common/skills/socratic-questioning/SKILL.md</Path>` 且 `audience=mine`，按 `<Path>{roots.workflows}/learning/G-goal/probe-template.md</Path>` 写入 `goal/probes/GP-<lesson>-b01.md`（Q1–Q5）；若仍有真实矩阵缺口且预算未满 10，再开 `b02.md`（Q6–Q10）。审问对象是已写成的 Lesson 与源码，不是学习者。miner 不得写 `lessons/` 或 matrix。
3. **阶段 D**：仅 Lead 根据各课缺口表与 `split-proposal` 处置：`re-dispatch-L` | `split-lesson` | `defer(reason)`。Lead 提交矩阵 / progress。满 10 问后禁止 `mine-more`。然后进入下一 unit。

升级现有课时：不删除仍覆盖有效格子的段落；补 deep 证据和前端缺口。子 Change 内的 `lesson_id`（如 notify 的 `L-001`）保持文件原 ID，chain 全局 ID 写在进度里对照。`re-dispatch-L` 不重置该课 `question_budget_used`。

`learning_agent_limit: 4`。并行只发生在写集不相交的子代理之间。宿主没有 team 时按相同派单包顺序执行，合同不降级。禁止因为不能并行就退回交错循环。

禁止：

- 跟随 `Q-question` 的 `inquiry/` Response 协议
- 自动激活 H / R / C / A-archive
- 把 mine 写成学习者问答课
- 在计划会话里写 `lessons/` 或 `goal/probes/`
- 在 Goal-Plan 里预写探针题目或深化讲义
- 阶段 T 完成前启动 miner
- `L → mine → L → mine` 交错
- 同一 `lesson_id` 第 11 问，或 `b03`
- 把现有课直接标 `covered` 而不 mine
- 调用 `C-consolidate` 或 `relocate-learning.mjs`

---

## §6 Gates and DoD

停止规则以 `<Path>{roots.workflows}/learning/G-goal/references/stop-rules.md</Path>` 为准。

| Gate | 含义 | 证据 |
| --- | --- | --- |
| G0 | 计划已编译 | `goal/goal-plan.md`、`chain.md`、`coverage-matrix.md`、`progress.md` 存在；`course.md` 有可观察 OBJ；每个 mine unit ≤15 节 |
| G1 | 本单元全部 planned 节点已写成合格 L 合同讲义 | `lessons/L-*.md` 或 `children/*/lessons/L-*.md`；progress 显示本 unit 先进入 `written` |
| G2 | 本单元至少改变一个矩阵格子 | 矩阵 diff + `progress.md` 一行事实 |
| G3 | 本单元新课或改写课有对应 probe，且每课 Q 数 ≤10 | `goal/probes/GP-*-b0N.md` |
| G-final | 范围内格子全部闭合且已验收 | 矩阵无 uncovered；`goal/verify.md` 已写（由 `/goal` 在停止规则触发后重写） |

Overall Definition of Done = G-final。课写完不是 DoD。阶段 T 结束之前禁止 miner。阶段 M 结束之前，Lead 不得把「课写完」写成 Goal 完成。

---

## §7 Authorization Matrix

| 动作 | plan 会话 | `/goal` Lead | L 子代理 | Miner 子代理 |
| --- | --- | --- | --- | --- |
| 跟随 I 写空骨架 | 允许 | 不授权 | 不授权 | 不授权 |
| 跟随 A 写/核 course 地图 | 允许 | 不授权 | 不授权 | 不授权 |
| 写 goal-plan / chain 骨架 | 允许 | run 中可追加 split 子节点、切 unit | 不授权 | 不授权 |
| 更新 matrix / progress | 只写骨架 | 允许（独占） | 不授权 | 不授权 |
| 写 `lessons/L-*.md` 或 `children/*/lessons/L-*.md` | 不授权 | 不代替 writer | 仅自己的文件 | 不授权 |
| 写 `goal/probes/` | 不授权 | 不代替 miner | 不授权 | 仅自己的 `GP-b0N` |
| 写 `goal/verify.md` | 不授权 | 停止规则触发后允许 | 不授权 | 不授权 |
| `inquiry/` `homework/` `review/` `synthesis/` `archive/` mastered | 禁止 | 禁止 | 禁止 | 禁止 |
| 物理移动 Change / `relocate-learning.mjs` | 禁止 | 禁止 | 禁止 | 禁止 |

文档中的「允许」不构成 `/goal` 之外的额外授权。

---

## §8 Constraints / HARD NO

- 不复活 `Q-quiz`
- mine ≠ 学习者 Q/A；禁止 `inquiry/`、`Response:`、`Submission:`、`verdict`、mastered
- 不自动串联 H / R / C / A-archive
- 路径只用 `<Path>{roots.*}/...</Path>`；roots 必须来自已打开的 `workspace.json`
- 不发明库存里没有的函数、容器或数据流
- 不把 Wave 当成挖掘门或并发授权
- 不把 Extended Abstract 当成完成条件
- 不把计划会话里的「已批准 / 允许」当成执行授权
- 不预写探针题目，不在 Goal-Plan 里写深化讲义
- 不在阶段 T 完成前启动 miner
- 不交错 L 与 mine
- 同一 `lesson_id` 第 11 问非法；满 10 问禁止 mine-more
- miner 不得选择处置，只填表
- `split-lesson` 不得调用 `C-consolidate` 或 `relocate-learning.mjs`
- 同一 mine unit 不得超过 15 节；不得把 16+ 节一次扇出
- 宿主没有 team 时，不得借机退回旧交错循环
- 不把 standard 总览课或「前端出范围」的域课直接标 covered

---

## §9 Resume Protocol

恢复时只读：

1. 本文件 §0、§5、§7、§8
2. `goal/progress.md` 当前 `mine_unit` 与阶段
3. 最近一份 `goal/probes/GP-*.md`
4. `goal/chain.md` 的 Units 表与当前 unit 内状态为 `writing` / `written` / `mined` / 下一个 `planned` 的节点

从最后一个改变了矩阵格子的检查点继续。仍覆盖有效格子的 Lesson 不删除、不重写。源码或范围变了先回到 G-goal `replan`，不要在 `/goal` 里偷偷扩 scope。当前 unit 若尚未全部 `written`，先完成阶段 T，禁止提前 mine。

`goal/verify.md` 若仍描述「本树无 children/」或独立根 locator，视为过期过程即停记录，不要当当前 blocker。

---

## §10 Progress

只记录可核对事实，权威表在 `goal/progress.md`。不准写主观百分比。progress 必须能看出当前 unit 全部 L 先进入 `written`，再进入 `mined`。

---

## §11 Minimum read order for `/goal`

1. 已打开的 `<Path>{roots.state}/workspace.json</Path>`
2. 本计划 §0、§5、§7、§8
3. `goal/chain.md` 当前 mine unit
4. `<Path>{roots.workflows}/learning/G-goal/references/lead-orchestration.md</Path>` 与 mine-unit / split-rules
5. 阶段 T：`<Path>{roots.workflows}/learning/L-lesson/L-lesson.md</Path>` 与 teaching-policy
6. 本单元全部 L 写成后：`probe-template.md` + `<Path>{roots.workflows}/learning/common/skills/socratic-questioning/SKILL.md</Path>` 且 `audience=mine`
7. `goal/coverage-matrix.md` + `<Path>{roots.workflows}/learning/G-goal/references/stop-rules.md</Path>`
8. 停止规则触发后写 `goal/verify.md`

不要整读 archive、其他独立 Change，或 `inquiry/`。
