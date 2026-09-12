---
schema_version: 3
plan_contract_version: 1
skill_scan: "scanned project Skill roots under <Path>.agents/skills/*/SKILL.md</Path> (engineering-standards, namewta-fullstack-development, ruoyi-common-modules-guide, ruoyi-module-guide, deploy-namewta-environment, java-api-compatibility, project-customization-delivery, upstream-fork-sync); excluded last three as non-axis for rename/W0b/publication; bound for T-14: engineering-standards, namewta-fullstack-development"
skill_bindings:
  - {"id":"engineering-standards","path":"<Path>.agents/skills/engineering-standards/SKILL.md</Path>","sha256":"acac7d1d4341d3361c298ebc5a61841d794cafbe9f235171b56fddd7ecb0037d","phase":"implement","operation":"orphan-push-wta-plus","inputs":["Ticket T-14","Tickets Map","Spec/ADR projections"],"outputs":["Evidence notes for T-14","next-step inputs"],"required":true,"on_failure":"block-ticket","references":[]}
  - {"id":"namewta-fullstack-development","path":"<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>","sha256":"e17e31173638b06f93bb2c26cf3315f50a1a075b3e298434c98fc47d175d7792","phase":"implement","operation":"orphan-push-wta-plus","inputs":["Ticket T-14","Tickets Map","Spec/ADR projections"],"outputs":["Evidence notes for T-14","next-step inputs"],"required":true,"on_failure":"block-ticket","references":[]}
resource_claims:
  - "orphan-public-wta-plus"
artifact: ticket
change: 2026-09-12-rename-ruoyi-dromara-to-wta
id: T-14
title: Orphan Repository Publication → NAMEWTA/WTA-plus
status: blocked
planning_depth: deep
planning_depth_reason: orphan 历史公开推送；不可逆发布面。
ready: false
risk: critical
blocked_by: [T-13]
contract_ids: [AC-008, AC-009, AC-011]
owner: unassigned
expected_changes:
  - "<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/evidence/publication/</Path>"
writable_paths:
  - "<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/evidence/publication/**</Path>"
  - "<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/LOG.md</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/**</Path>"
  - "<Path>plus-ui-namewta/**</Path>"
  - "<Path>docs/**</Path>"
  - "<Path>speculo/**</Path>"
  - "<Path>release-artifacts/**</Path>"
  - "<Path>scripts/**</Path>"
shared_paths: []
shared_path_owners: []
---

# Ticket T-14: Orphan Repository Publication → NAMEWTA/WTA-plus

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/ticket/14-orphan-wta-plus.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/evidence/T-14.md</Path>`

实现本 Ticket 时，Lead 与 implementation subagent 必须按顺序完整读取总体 Map，读取项目 Skill 的 frontmatter 与入口并只展开适用于 `ALL`/`T-14` 的匹配项，再读取本 Ticket 与相关上游工件。Map 中的 Skill 是最低必读集合；新的匹配项先由 Lead 同步到 Map 并重新校验。

**冻结投影（覆盖旧 S-spec 默认）：** 公开仓 **WTA-plus**（`NAMEWTA/WTA-plus`）；布局=**docs 聚合镜像**（非强制 backend/frontend/docs/speculo 四顶层）；Nacos=**一次性硬切无双读**；上游不需要；旧库用户一律 `wta`（T-17）。

## 1. 战略与来源

- **目标：** 在 publication auth 后，将准备树以 **orphan** 历史推送到 **`NAMEWTA/WTA-plus`**（仓名 WTA-plus）。**禁止**再写 NAMEWTA/wta。
- **可观察产出：** 远程 HEAD 证明；ORPH-01…05；无旧三仓祖先。
- **来源：** ADR-010/011/012、AC-REPOSITORY、AC-PUBLICATION、AC-W0b。
- **当前事实：** 目标 remote 建议 `NAMEWTA/WTA-plus`；布局为聚合镜像。
- **Planning Depth 原因：** orphan 历史公开推送；不可逆发布面。

## 2. 决策状态

### 已锁定决策

- orphan only。
- 无 gate 不 push。
- slug=WTA-plus。

### 已采用的低影响假设

- GitHub 大小写按 org 惯例。

### blocked-by-auth

**CTO HOLD / 三门未授权。** 本票在 `implementation_authorized=true`（及本票适用的 `public_repo_publication_authorized` / `legacy_repo_mutation_authorized`）书面翻转前 **禁止执行** 任何产品树改写、建仓、push、旧 remote mutation。当前三门均为 `false`。本文仅为 SpecDev 规划合同。

### blocked-by-CTO / AMBIGUOUS

无剩余高影响 AMBIGUOUS（LOG-017/018 / ADR-006硬切 / ADR-012 / ADR-013 已收口）。仅余三门书面授权等待 CTO；不阻塞票结构。

### 未决问题

无。


## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| orphan 建仓+push 证据。 | T-04 准备树、T-12/T-13。 | 旧仓 mutation（T-15）；NAMEWTA/wta。 |

## 4. 要构建什么

orphan 初始提交 → push NAMEWTA/WTA-plus → 记录证明。

## 5. 实现契约

- **输出：** remote HEAD + ORPH 证明。
- **失败：** 带祖先 / 未授权 push。
- **兼容要求：** 见发布/迁移节；未授权不实施。
- **安全与隐私要求：** 不提交密钥；公开前走 T-12。

## 6. 执行路线

1. 确认 pub auth。
2. orphan 历史。
3. push NAMEWTA/WTA-plus。
4. ORPH 检查。
5. 记录 evidence。

## 7. 路径访问契约

- **预计修改点：** 与 `expected_changes` 对齐。
- **可写范围：** 与 `writable_paths` 对齐；越界前必须停止。
- **只读上下文：** 与 `read_only_paths` 对齐。
- **共享路径：** 无。
- **保留或不动：** 第三方 KEEP 坐标/import；旧仓 Git 历史对象。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| slug | remote | NAMEWTA/WTA-plus | 是 | evidence/publication/ |
| orphan | git rev-list | 无旧祖先 | 是 | evidence/publication/ |
| 无 .gitee | 树扫描 | 无 | 是 | evidence/publication/ |
| KEEP | rg | 通过 | 是 | evidence/publication/ |

- **KEEP 回归（强制）：** 任意波次后 `rg`/compile 证明 `org.dromara.sms4j` / `org.dromara.warm` / `org.dromara.easyes` / `org.dromara.mica.mqtt` 坐标与 import **字节级保留**（AC-KEEP / ADR-003）。
- **禁止 zero-match 当验收：** 全局 `ruoyi`/`org.dromara` 清零 **不是** 通过条件（NAC-05 / HC-12）。

- **Workspace checks：** current-workspace 非 E2E（lint/build/`rg`/checklist）按 Goal Plan；未授权前仅文档/inventory 写面。
- **E2E disposition：** not-required：本票为命名/仓库形态/门禁；跨边界 UI 冒烟留给 T-11 可选。
- **E2E owner/environment：** Lead；若日后 required，则 parent-candidate / current-workspace（按 Goal Plan）；禁止在 Ticket source worktree 宣称 E2E 通过。
- **Integration evidence：** implementation/source commit、direct-parent 或 candidate、父分支 result SHA（授权实施后由 Lead 记入 Evidence）。

## 9. 发布、迁移与恢复

- **迁移顺序：** auth→orphan push。
- **兼容窗口：** 不适用。
- **监控：** remote 可见性。
- **回滚：** 公开后需 CTO 决策（不可轻易 force）。
- **不可逆：** public 暴露。
- **收缩：** 旧聚合不再作为 public 源。

## 10. 验收标准

- [ ] `AC-008/009/011`。
- [ ] 目标为 WTA-plus，非 NAMEWTA/wta。
- [ ] ORPH 通过。
- [ ] 实现开始前已完整读取 Tickets Map，已读取项目 Skill 入口并完整展开适用于 `ALL`/`T-14` 的匹配项。
- [ ] 验证矩阵全部执行并记录到 `<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/evidence/T-14.md</Path>`（完成时；**本轮不新建伪 Evidence**）。
- [ ] 实际项目修改未超出 `writable_paths`。
- [ ] Ticket 按 Goal Plan 形成非空 implementation/source commit（**仅授权实施后**）；direct-parent 或 candidate 验证由 Lead 记录。
- [ ] E2E disposition 已声明/执行；环境为 current-workspace 或 parent-candidate/direct-parent 规则。
- [ ] 未发生未批准的范围、契约或发布偏差；未翻转未授权门禁。
- [ ] Ticket、Tickets Map 和 Evidence 状态一致。
- [ ] **blocked-by-auth：** 执行前三门/适用门已书面 true。

## 11. SKILL 调用计划

依据 `<Path>{roots.workflows}/specdev/common/rules/skill-invocation.md</Path>`：本票 `skill_bindings` 绑定 Map 中适用于 ALL/`T-14` 的每一 Path。

- **engineering-standards / namewta-fullstack-development：** 规划与（授权后）实现质量门禁、品牌合同。
- **按票附加：** common-modules（T-05/T-07）、module-guide（T-05/T-06/T-07）、deploy（T-08）。
- 调用时机：plan/implement/verify 与 frontmatter phase 一致；失败 `block-ticket`。
- 输入：本票+Map+inventory/KEEP；输出：下一步证据与门禁判断。

## 12. 停止、检查点与交付

- **用户交付要求与数量：** Map `requested_deliverables=[]`；不为压缩减少票合同。
- **必需 Skill 不可用：** 阻塞本票。
- **归属与资源冲突：** 暂停本票与下游，不接管他人状态。
- **检查点：** 记录源版本、绑定摘要、步骤、Evidence；恢复前回读。
- **完成出口：** 适用验收通过且授权条件满足后交回 Goal；HOLD 期间保持 blocked。
- **铁律：** 授权→W0b→rename；禁止旧仓内先 rename。
