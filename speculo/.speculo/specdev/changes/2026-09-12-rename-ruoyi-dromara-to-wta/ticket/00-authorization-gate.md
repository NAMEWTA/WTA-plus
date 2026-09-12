---
schema_version: 3
plan_contract_version: 1
skill_scan: "scanned project Skill roots under <Path>.agents/skills/*/SKILL.md</Path> (engineering-standards, namewta-fullstack-development, ruoyi-common-modules-guide, ruoyi-module-guide, deploy-namewta-environment, java-api-compatibility, project-customization-delivery, upstream-fork-sync); excluded last three as non-axis for rename/W0b/publication; bound for T-00: engineering-standards, namewta-fullstack-development"
skill_bindings:
  - {"id":"engineering-standards","path":"<Path>.agents/skills/engineering-standards/SKILL.md</Path>","sha256":"acac7d1d4341d3361c298ebc5a61841d794cafbe9f235171b56fddd7ecb0037d","phase":"plan","operation":"record-auth-gates","inputs":["Ticket T-00","Tickets Map","Spec/ADR projections"],"outputs":["Evidence notes for T-00","next-step inputs"],"required":true,"on_failure":"block-ticket","references":[]}
  - {"id":"namewta-fullstack-development","path":"<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>","sha256":"e17e31173638b06f93bb2c26cf3315f50a1a075b3e298434c98fc47d175d7792","phase":"plan","operation":"record-auth-gates","inputs":["Ticket T-00","Tickets Map","Spec/ADR projections"],"outputs":["Evidence notes for T-00","next-step inputs"],"required":true,"on_failure":"block-ticket","references":[]}
resource_claims:
  - "auth-gate-projection"
artifact: ticket
change: 2026-09-12-rename-ruoyi-dromara-to-wta
id: T-00
title: Authorization Gate / 三门授权记录（不实施）
status: ready
planning_depth: standard
planning_depth_reason: 跨实施/公开/旧仓三门门禁投影与 AC-Gate/NAC-04；仅文档闸门，无产品树写入。
ready: true
risk: high
blocked_by: []
contract_ids: [AC-012, NAC-04]
owner: unassigned
expected_changes:
  - "<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/.status.json</Path>"
  - "<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/LOG.md</Path>"
  - "<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/MANIFEST-CHANGE.md</Path>"
writable_paths:
  - "<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/.status.json</Path>"
  - "<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/LOG.md</Path>"
  - "<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/MANIFEST-CHANGE.md</Path>"
  - "<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/tickets-map.md</Path>"
read_only_paths:
  - "<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/spec.md</Path>"
  - "<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/ADR.md</Path>"
shared_paths: []
shared_path_owners: []
---

# Ticket T-00: Authorization Gate / 三门授权记录（不实施）

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/ticket/00-authorization-gate.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/evidence/T-00.md</Path>`

实现本 Ticket 时，Lead 与 implementation subagent 必须按顺序完整读取总体 Map，读取项目 Skill 的 frontmatter 与入口并只展开适用于 `ALL`/`T-00` 的匹配项，再读取本 Ticket 与相关上游工件。Map 中的 Skill 是最低必读集合；新的匹配项先由 Lead 同步到 Map 并重新校验。

**冻结投影（覆盖旧 S-spec 默认）：** 公开仓 **WTA-plus**（`NAMEWTA/WTA-plus`）；布局=**docs 聚合镜像**（非强制 backend/frontend/docs/speculo 四顶层）；Nacos=**一次性硬切无双读**；上游不需要；旧库用户一律 `wta`（T-17）。

## 1. 战略与来源

- **目标：** 记录并投影三道授权闸门状态；证明文档 ready ≠ 可实施。
- **可观察产出：** `.status.json` / Map / LOG 明示三门 false；任何实施企图可被 AC-Gate 否决。
- **来源：** AC-012、NAC-04、HC-08/09/10、ADR-010、USER-DECISION:t84s2 CTO HOLD。
- **当前事实：** implementation_authorized / public_repo_publication_authorized / legacy_repo_mutation_authorized 均为 false；execution_authorization.* 均为 not-authorized。
- **Planning Depth 原因：** 跨实施/公开/旧仓三门门禁投影与 AC-Gate/NAC-04；仅文档闸门，无产品树写入。

## 2. 决策状态

### 已锁定决策

- 三门独立翻转，仅 CTO 书面。
- Review/S-spec ready_for_tickets ≠ implementation_authorized。
- 禁止代理自授。

### 已采用的低影响假设

- 本票只改 SpecDev 状态投影，不碰产品树。

### blocked-by-auth

本票**不实施**产品变更；仅记录闸门。当前三门 false。禁止借本票翻转授权。

### blocked-by-CTO / AMBIGUOUS

三门书面授权仍待 CTO；不阻塞本票文档完成。

### 未决问题

无。


## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| 状态/LOG/MANIFEST/Map 授权投影；Gate 验收叙述。 | ADR-010 sequencing；spec AC-Gate。 | 翻转任何 *_authorized；I-implement；建仓/push。 |

## 4. 要构建什么

审阅者打开 change 状态即可看到三门仍 false，并理解只有 CTO 书面授权后才可进入 W0b/rename/publication/legacy。本票完成不改变任何授权位。

## 5. 实现契约

- **入口：** `.status.json` 授权字段与 execution_authorization。
- **输入/输出：** CTO 书面 → 字段翻转记录（本票不执行翻转）。
- **不变量：** 未授权时产品树/旧 remote/public push 不变。
- **失败：** 把 ready_for_tickets 当授权 → NAC-04 缺陷。
- **安全：** 不泄露凭据；不伪造授权。
- **兼容要求：** 见发布/迁移节；未授权不实施。
- **安全与隐私要求：** 不提交密钥；公开前走 T-12。

## 6. 执行路线

1. 核对三门与 execution_authorization 均为未授权。
2. 在 Map/LOG 投影 Gate 语义（AC-012）。
3. 验收：文档关闭不触发实施。
4. 记录 Evidence 指针（完成时，不新建伪 Evidence）。

## 7. 路径访问契约

- **预计修改点：** 与 `expected_changes` 对齐。
- **可写范围：** 与 `writable_paths` 对齐；越界前必须停止。
- **只读上下文：** 与 `read_only_paths` 对齐。
- **共享路径：** 无。
- **保留或不动：** 第三方 KEEP 坐标/import；旧仓 Git 历史对象。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| Gate 语义 | status 字段 | 读 `.status.json` 三门 | 全 false | evidence/T-00.md（完成时） |
| 误把 Spec ready 当授权 | NAC-04 | 对照 LOG/HOLD | 无实施写面 | evidence/T-00.md |
| 回归 | AC-Gate | Map 投影一致 | 三门仍 false | evidence/T-00.md |

- **KEEP 回归（强制）：** 任意波次后 `rg`/compile 证明 `org.dromara.sms4j` / `org.dromara.warm` / `org.dromara.easyes` / `org.dromara.mica.mqtt` 坐标与 import **字节级保留**（AC-KEEP / ADR-003）。
- **禁止 zero-match 当验收：** 全局 `ruoyi`/`org.dromara` 清零 **不是** 通过条件（NAC-05 / HC-12）。

- **Workspace checks：** current-workspace 非 E2E（lint/build/`rg`/checklist）按 Goal Plan；未授权前仅文档/inventory 写面。
- **E2E disposition：** not-required：本票为命名/仓库形态/门禁；跨边界 UI 冒烟留给 T-11 可选。
- **E2E owner/environment：** Lead；若日后 required，则 parent-candidate / current-workspace（按 Goal Plan）；禁止在 Ticket source worktree 宣称 E2E 通过。
- **Integration evidence：** implementation/source commit、direct-parent 或 candidate、父分支 result SHA（授权实施后由 Lead 记入 Evidence）。

## 9. 发布、迁移与恢复

- **迁移顺序：** 不适用：纯门禁记录。
- **回滚：** 恢复状态字段原文（若误改）。
- **不可逆操作：** 无（本票禁止翻转授权）。

## 10. 验收标准

- [ ] `AC-012`：文档/Spec ready **不等于** 三门授权。
- [ ] `NAC-04`：未将 Review/CHANGES REQUIRED 当作可实施。
- [ ] 未翻转任何 `*_authorized` / `execution_authorization.*`。
- [ ] 已读 Map ALL Skills。
- [ ] E2E disposition=not-required 已声明。
- [ ] 实现开始前已完整读取 Tickets Map，已读取项目 Skill 入口并完整展开适用于 `ALL`/`T-00` 的匹配项。
- [ ] 验证矩阵全部执行并记录到 `<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/evidence/T-00.md</Path>`（完成时；**本轮不新建伪 Evidence**）。
- [ ] 实际项目修改未超出 `writable_paths`。
- [ ] Ticket 按 Goal Plan 形成非空 implementation/source commit（**仅授权实施后**）；direct-parent 或 candidate 验证由 Lead 记录。
- [ ] E2E disposition 已声明/执行；环境为 current-workspace 或 parent-candidate/direct-parent 规则。
- [ ] 未发生未批准的范围、契约或发布偏差；未翻转未授权门禁。
- [ ] Ticket、Tickets Map 和 Evidence 状态一致。


## 11. SKILL 调用计划

依据 `<Path>{roots.workflows}/specdev/common/rules/skill-invocation.md</Path>`：本票 `skill_bindings` 绑定 Map 中适用于 ALL/`T-00` 的每一 Path。

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
