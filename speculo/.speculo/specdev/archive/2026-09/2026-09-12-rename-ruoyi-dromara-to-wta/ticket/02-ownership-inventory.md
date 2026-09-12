---
schema_version: 3
plan_contract_version: 1
skill_scan: "scanned project Skill roots under <Path>.agents/skills/*/SKILL.md</Path> (engineering-standards, namewta-fullstack-development, ruoyi-common-modules-guide, ruoyi-module-guide, deploy-namewta-environment, java-api-compatibility, project-customization-delivery, upstream-fork-sync); excluded last three as non-axis for rename/W0b/publication; bound for T-02: engineering-standards, namewta-fullstack-development"
skill_bindings:
  - {"id":"engineering-standards","path":"<Path>.agents/skills/engineering-standards/SKILL.md</Path>","sha256":"acac7d1d4341d3361c298ebc5a61841d794cafbe9f235171b56fddd7ecb0037d","phase":"implement","operation":"build-ownership-inventory","inputs":["Ticket T-02","Tickets Map","Spec/ADR projections"],"outputs":["Evidence notes for T-02","next-step inputs"],"required":true,"on_failure":"block-ticket","references":[]}
  - {"id":"namewta-fullstack-development","path":"<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>","sha256":"e17e31173638b06f93bb2c26cf3315f50a1a075b3e298434c98fc47d175d7792","phase":"implement","operation":"build-ownership-inventory","inputs":["Ticket T-02","Tickets Map","Spec/ADR projections"],"outputs":["Evidence notes for T-02","next-step inputs"],"required":true,"on_failure":"block-ticket","references":[]}
resource_claims:
  - "ownership-inventory"
artifact: ticket
change: 2026-09-12-rename-ruoyi-dromara-to-wta
id: T-02
title: Build Rename/Ownership Inventory（只写 evidence/inventory）
status: done
planning_depth: standard
planning_depth_reason: 分类 KEEP/RENAME/REVIEW 驱动全部自动迁移；执行扫产品树属实施面，HOLD 下 blocked。
ready: true
risk: high
blocked_by: [T-01]
contract_ids: [AC-001, AC-002, AC-007, NAC-01]
owner: unassigned
expected_changes:
  - "<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/evidence/inventory/</Path>"
writable_paths:
  - "<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/evidence/inventory/**</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/**</Path>"
  - "<Path>plus-ui-namewta/**</Path>"
  - "<Path>docs/**</Path>"
  - "<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/evidence/SOURCE-BASELINE.md</Path>"
  - "<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/evidence/THIRD-PARTY-KEEP.md</Path>"
shared_paths: []
shared_path_owners: []
---

# Ticket T-02: Build Rename/Ownership Inventory（只写 evidence/inventory）

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/ticket/02-ownership-inventory.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/evidence/T-02.md</Path>`

实现本 Ticket 时，Lead 与 implementation subagent 必须按顺序完整读取总体 Map，读取项目 Skill 的 frontmatter 与入口并只展开适用于 `ALL`/`T-02` 的匹配项，再读取本 Ticket 与相关上游工件。Map 中的 Skill 是最低必读集合；新的匹配项先由 Lead 同步到 Map 并重新校验。

**冻结投影（覆盖旧 S-spec 默认）：** 公开仓 **WTA-plus**（`NAMEWTA/WTA-plus`）；布局=**docs 聚合镜像**（非强制 backend/frontend/docs/speculo 四顶层）；Nacos=**一次性硬切无双读**；上游不需要；旧库用户一律 `wta`（T-17）。

## 1. 战略与来源

- **目标：** 在授权后产出 ownership inventory，作为自动迁移唯一输入。
- **可观察产出：** `evidence/inventory/*` 列出 path pattern / 例 / KEEP|RENAME|REVIEW / owner / disposition，并引用 SOURCE-BASELINE SHA。
- **来源：** §2.2、P0-06、HC-01/02、FR-007。
- **当前事实：** 产品树仍含大量 org.dromara + ruoyi-*；第三方 KEEP 清单已存在。
- **Planning Depth 原因：** 分类 KEEP/RENAME/REVIEW 驱动全部自动迁移；执行扫产品树属实施面，HOLD 下 blocked。

## 2. 决策状态

### 已锁定决策

- 仅 first-party 可自动迁到 org.namewta / wta-。
- 写面仅 evidence/inventory。
- 禁止盲 sed。

### 已采用的低影响假设

- 排除 node_modules/target/.git/lockfile 噪声。

### blocked-by-auth

**CTO HOLD / 三门未授权。** 本票在 `implementation_authorized=true`（及本票适用的 `public_repo_publication_authorized` / `legacy_repo_mutation_authorized`）书面翻转前 **禁止执行** 任何产品树改写、建仓、push、旧 remote mutation。当前三门均为 `false`。本文仅为 SpecDev 规划合同。

### blocked-by-CTO / AMBIGUOUS

无剩余高影响 AMBIGUOUS（LOG-017/018 / ADR-006硬切 / ADR-012 / ADR-013 已收口）。仅余三门书面授权等待 CTO；不阻塞票结构。

### 未决问题

无。


## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| inventory 证据文件。 | THIRD-PARTY-KEEP、SOURCE-BASELINE。 | 改产品代码；自动 migrate；zero-match 目标。 |

## 4. 要构建什么

授权后扫描命中 → 分类 → 写入 inventory；无 inventory 不得开 rename 波次。

## 5. 实现契约

- **入口：** scoped rg 扫描。
- **输出：** evidence/inventory 分类表。
- **不变量：** 产品字节不变。
- **失败：** 未分类命中进入自动迁移 → 停。
- **兼容要求：** 见发布/迁移节；未授权不实施。
- **安全与隐私要求：** 不提交密钥；公开前走 T-12。

## 6. 执行路线

1. 确认 implementation_authorized。
2. 按基线 SHA 扫描。
3. 分类 KEEP/RENAME/REVIEW。
4. 写入 evidence/inventory 并交叉 KEEP。
5. 验证无产品树 diff。

## 7. 路径访问契约

- **预计修改点：** 与 `expected_changes` 对齐。
- **可写范围：** 与 `writable_paths` 对齐；越界前必须停止。
- **只读上下文：** 与 `read_only_paths` 对齐。
- **共享路径：** 无。
- **保留或不动：** 第三方 KEEP 坐标/import；旧仓 Git 历史对象。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| inventory 覆盖 | 文档+抽样 | 对照 SURVEY 类命中 | 每类有 disposition | evidence/T-02.md |
| 未改产品 | git diff 产品路径 | 空 | 空 | evidence/T-02.md |
| KEEP 回归意识 | 对照 THIRD-PARTY-KEEP | 第三方标 KEEP | 是 | evidence/T-02.md |

- **KEEP 回归（强制）：** 任意波次后 `rg`/compile 证明 `org.dromara.sms4j` / `org.dromara.warm` / `org.dromara.easyes` / `org.dromara.mica.mqtt` 坐标与 import **字节级保留**（AC-KEEP / ADR-003）。
- **禁止 zero-match 当验收：** 全局 `ruoyi`/`org.dromara` 清零 **不是** 通过条件（NAC-05 / HC-12）。

- **Workspace checks：** current-workspace 非 E2E（lint/build/`rg`/checklist）按 Goal Plan；未授权前仅文档/inventory 写面。
- **E2E disposition：** not-required：本票为命名/仓库形态/门禁；跨边界 UI 冒烟留给 T-11 可选。
- **E2E owner/environment：** Lead；若日后 required，则 parent-candidate / current-workspace（按 Goal Plan）；禁止在 Ticket source worktree 宣称 E2E 通过。
- **Integration evidence：** implementation/source commit、direct-parent 或 candidate、父分支 result SHA（授权实施后由 Lead 记入 Evidence）。

## 9. 发布、迁移与恢复

- **迁移顺序：** inventory → 后续 rename。
- **回滚：** 删除/还原 inventory 文件。

## 10. 验收标准

- [ ] inventory 可作为 T-05+ 输入。
- [ ] 产品树无修改。
- [ ] 未使用 zero-match 作为完成标准。
- [ ] blocked-by-auth 已解除后才执行扫描写 inventory。
- [ ] 实现开始前已完整读取 Tickets Map，已读取项目 Skill 入口并完整展开适用于 `ALL`/`T-02` 的匹配项。
- [ ] 验证矩阵全部执行并记录到 `<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/evidence/T-02.md</Path>`（完成时；**本轮不新建伪 Evidence**）。
- [ ] 实际项目修改未超出 `writable_paths`。
- [ ] Ticket 按 Goal Plan 形成非空 implementation/source commit（**仅授权实施后**）；direct-parent 或 candidate 验证由 Lead 记录。
- [ ] E2E disposition 已声明/执行；环境为 current-workspace 或 parent-candidate/direct-parent 规则。
- [ ] 未发生未批准的范围、契约或发布偏差；未翻转未授权门禁。
- [ ] Ticket、Tickets Map 和 Evidence 状态一致。
- [ ] **blocked-by-auth：** 执行前三门/适用门已书面 true。

## 11. SKILL 调用计划

依据 `<Path>{roots.workflows}/specdev/common/rules/skill-invocation.md</Path>`：本票 `skill_bindings` 绑定 Map 中适用于 ALL/`T-02` 的每一 Path。

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
