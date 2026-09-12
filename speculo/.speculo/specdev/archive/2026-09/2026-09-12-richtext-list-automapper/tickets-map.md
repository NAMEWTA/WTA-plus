---
schema_version: 3
plan_contract_version: 1
plan_revision: 3
requested_deliverables: []
deliverable_policy: "用户未指定额外交付物名称或正整数数量；Lead 授权跳过 S/R，以 diagnosis 修复契约为规划输入；本 change 仅要求可验收的计划型 Tickets 与 Map，票数量由垂直切片决定（1），不以票数充当 requested_deliverables。"
artifact: tickets-map
change: 2026-09-12-richtext-list-automapper
status: ready
---

# Tickets Map: 富文本 list SummaryVo AutoMapper 修复

- **Map：** `<Path>{roots.state}/specdev/changes/2026-09-12-richtext-list-automapper/tickets-map.md</Path>`
- **Spec：** `<Path>{roots.state}/specdev/changes/2026-09-12-richtext-list-automapper/spec.md</Path>`（诊断修复契约的工件化镜像；Lead 授权跳过正式 S 访谈）
- **上游诊断：** `<Path>{roots.state}/specdev/changes/2026-09-12-richtext-list-automapper/diagnosis.md</Path>`
- **Ticket 目录：** `<Path>{roots.state}/specdev/changes/2026-09-12-richtext-list-automapper/ticket/</Path>`
- **Evidence 目录：** `<Path>{roots.state}/specdev/changes/2026-09-12-richtext-list-automapper/evidence/</Path>`
- **Goal Plan：** `<Path>{roots.state}/specdev/changes/2026-09-12-richtext-list-automapper/goal-plan.md</Path>`（Lead 派单正式 Goal Plan；单票仍须编排）
- **关联只读归档：** `<Path>{roots.state}/specdev/archive/2026-09/2026-09-08-richtext-third-oss/</Path>`

## 1. 目标与拆分策略

单票交付诊断修复契约：`TestRichTextSummaryVo` 补 `@AutoMapper`，建议一并补齐 `TestRichTextVo` 的 `contentHtml→html` 映射，并建立 list 非空回归（修复前红 / 修复后绿）。

切片原则：

- 合并信号成立：注解修复与回归测共享同一不可分割验收（非空 list 转换），拆成两票会制造空依赖边与重复上下文。
- 无 Prefactor / expand-contract。
- 写集收紧到 demo 富文本 VO 与 demo 测试目录；对照只读同模块 Vo、ServiceImpl、归档。

### 总体实施背景

- 根因：`BaseMapperPlus<TestRichText, TestRichTextSummaryVo>.selectVoPage` 非空时 `MapstructUtils.convert` 找不到 Entity→SummaryVo converter。
- create/get 手工 `view()` 不经 MapStruct，故曾掩盖缺口；空列表早退不红。
- 对照：`TestDemoVo`/`TestTreeVo` 已有 `@AutoMapper(target=…)`。
- OUT：不改 BaseMapperPlus/MapstructUtils；不扩 OSS/三方；不 reopen 归档。
- 并行 active change `2026-09-10-notify-channel-config` 写集不相交。
- Ticket workspace 已由 Goal Plan 固定为 current（串行）+ direct-parent；Wave 仅依赖投影，同时仅一个 implementation writer。Wave/Gate 权威见 Goal Plan。

### 项目 Skill 读取矩阵

每个 Ticket 的 Lead 或 implementation subagent 都必须先完整读取本 Map，再读取候选项目 Skill 的 frontmatter 与入口；只有适用于 `ALL` 或当前 Ticket ID、且 scope/路径/技术域/验证条件命中的 Skill 才完整读取，最后进入当前 Ticket。下表是发布时已确认的**最低必读集合，不是 Skill allowlist**。

| Applies To | Project Skill | Trigger / Scope | Read Timing | Purpose |
|---|---|---|---|---|
| ALL | `<Path>.agents/skills/engineering-standards/SKILL.md</Path>` | `ruoyi-demo` Java VO/MapStruct 注解与定向回归测 | Map 后、Ticket 前 | 架构/Java CRUD VO 惯例与测试门禁 |

扫描证据：已枚举 `.agents/skills/*/SKILL.md` 与 AGENTS.md 路由。不适用 `ruoyi-module-guide`（非新增模块、非 Profile/System/Workflow/Notify/Third 接入）；不适用 `namewta-fullstack-development`（无前端/菜单切片）；不适用 `ruoyi-common-modules-guide` / `java-api-compatibility` / `deploy-namewta-environment` / `upstream-fork-sync` / `project-customization-delivery`。

## 2. 执行清单

| ID | Ticket | 可观察产出 | Blocked By | Depth | Risk | Ready | Owner | Contract IDs | Wave/Gate | Status |
|---|---|---|---|---|---|---|---|---|---|---|
| T-01 | `<Path>{roots.state}/specdev/changes/2026-09-12-richtext-list-automapper/ticket/01-richtext-list-automapper-regression.md</Path>` | 非空富文本 list 不再 ConvertException；SummaryVo 有 AutoMapper；回归前红后绿 | — | standard | low | yes | rvp-implement:4c753e13-b41b-408e-8cc8-275810f995e4 | AC-001, AC-002, AC-003, AC-004 | W1/G1 | done |

Ticket frontmatter 是状态、依赖、深度和路径访问契约的权威；本表是同步投影，不得独立修改出另一套真相。

## 3. 依赖 DAG

```text
T-01 [READY]
```

无真实阻塞边。单票含注解 + 可选 Vo + 回归；不按技术层拆空票。

## 4. 合同覆盖矩阵

| Contract ID | 覆盖 Ticket | 验证接缝 | 状态 | 说明 |
|---|---|---|---|---|
| AC-001 | T-01 | SummaryVo 源码 + 编译/转换 | covered | `@AutoMapper(target=TestRichText.class)` |
| AC-002 | T-01 | list / selectVoPage 非空回归 | covered | 修复前红 / 修复后绿 |
| AC-003 | T-01 | TestRichTextVo 注解审查（可选） | covered | 若补 AutoMapper 必须 AutoMapping contentHtml→html；未做须 Evidence 声明 |
| AC-004 | T-01 | diff/范围守门 | covered | 不碰归档、BaseMapperPlus、MapstructUtils、OSS/三方 |

`uncovered` 必须修复；本矩阵无 deferred。

## 5. 并行与路径所有权

- 单实现票，无并行写冲突。
- shared owner 不需要；Lead 仍是 SpecDev 状态与父分支 integration owner。
- 项目路径契约以 Ticket frontmatter 为准。

| Ticket A | Ticket B | Writable 交集 | 真实依赖 | 处理 |
|---|---|---|---|---|
| — | — | — | — | 仅 T-01 |

## 6. Gate、Wave 与集成点

Goal Plan 权威：`<Path>{roots.state}/specdev/changes/2026-09-12-richtext-list-automapper/goal-plan.md</Path>`。

- W1/G1：T-01 SummaryVo `@AutoMapper` + list 非空回归（前红后绿）+ Lead **required** E2E（save→list 点击/页面验收，与诊断症状对齐）。

workspace：`current` + `direct-parent`；Lead=`rvp-lead:1c44f1c3-a61e-4d39-a7a7-f2d21ce5fb00`；implementation_agent_limit=3（Lead 不计；current 单 writer）。Goal Plan 已 `ready` / `ready_for_execution: true`（授权已落）；I 由 Lead 调度。本 Map 不自启实现。

## 7. 横切契约与风险

- 保留富文本 Client+用户隔离与手工 `view()`。
- 注解依赖编译期 mapstruct-plus；验证须覆盖生成/转换而非仅改源码文本。
- 归档 `2026-09-08-richtext-third-oss` 只读。

## 8. 同步规则

- Ticket 状态变化后同步执行清单；
- Ticket ID、路径、依赖或 frontmatter 不一致时，以 Ticket 文件为权威并修复本 Map；
- 项目 Skill 新增适用项时先同步读取矩阵并重新校验；
- Goal Plan 存在时，Wave/Gate/owner 以 Goal Plan 为编排权威；
- 依赖、合同覆盖或路径所有权变化后运行 `<Path>{roots.workflows}/specdev/common/tools/validate-specdev.mjs</Path>`；
- 内部工件不得使用相对 Markdown 链接。

## 9. 总控与恢复

从本 Map 进入 `<Path>{roots.workflows}/specdev/P-goal-plan/P-goal-plan.md</Path>` 的 plan/run/resume/replan/verify；获 Goal Plan 授权与 Lead 派单后再按 `<Path>{roots.workflows}/specdev/I-implement/I-implement.md</Path>` 执行。先运行 `<Path>{roots.workflows}/specdev/common/tools/ticket-control.mjs</Path>` 的 `--map` 只读检查，再按授权调用 I 与真实 Skill、验收、更新状态直到完成或明确阻塞；此工具本身不执行代码。

- frontmatter 中 `requested_deliverables` 为空：用户无额外数量合同。
- `deliverable_policy` 记录跳过 S/R 与单票切片依据。
- 变更范围或验收后递增 `plan_revision` 并重算受影响闭包。
