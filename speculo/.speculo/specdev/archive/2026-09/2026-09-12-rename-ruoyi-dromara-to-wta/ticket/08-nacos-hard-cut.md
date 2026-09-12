---
schema_version: 3
plan_contract_version: 1
skill_scan: "scanned project Skill roots under <Path>.agents/skills/*/SKILL.md</Path> (engineering-standards, namewta-fullstack-development, ruoyi-common-modules-guide, ruoyi-module-guide, deploy-namewta-environment, java-api-compatibility, project-customization-delivery, upstream-fork-sync); excluded last three as non-axis for rename/W0b/publication; bound for T-08: engineering-standards, namewta-fullstack-development, deploy-namewta-environment"
skill_bindings:
  - {"id":"engineering-standards","path":"<Path>.agents/skills/engineering-standards/SKILL.md</Path>","sha256":"acac7d1d4341d3361c298ebc5a61841d794cafbe9f235171b56fddd7ecb0037d","phase":"implement","operation":"nacos-hard-cut-window","inputs":["Ticket T-08","Tickets Map","Spec/ADR projections"],"outputs":["Evidence notes for T-08","next-step inputs"],"required":true,"on_failure":"block-ticket","references":[]}
  - {"id":"namewta-fullstack-development","path":"<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>","sha256":"e17e31173638b06f93bb2c26cf3315f50a1a075b3e298434c98fc47d175d7792","phase":"implement","operation":"nacos-hard-cut-window","inputs":["Ticket T-08","Tickets Map","Spec/ADR projections"],"outputs":["Evidence notes for T-08","next-step inputs"],"required":true,"on_failure":"block-ticket","references":[]}
  - {"id":"deploy-namewta-environment","path":"<Path>.agents/skills/deploy-namewta-environment/SKILL.md</Path>","sha256":"064bc3349a1e9d256f844f6e3578bc01f99c112051a531334adc30a91d5a8f02","phase":"implement","operation":"nacos-hard-cut-window","inputs":["Ticket T-08","Tickets Map","Spec/ADR projections"],"outputs":["Evidence notes for T-08","next-step inputs"],"required":true,"on_failure":"block-ticket","references":[]}
resource_claims:
  - "nacos-hard-cut"
  - "runtime-id-mapping"
artifact: ticket
change: 2026-09-12-rename-ruoyi-dromara-to-wta
id: T-08
title: Runtime/Nacos 一次性硬切（无双读）
status: done
planning_depth: deep
planning_depth_reason: 运行时标识对照与发版窗口硬切；失败即停与回滚。
ready: true
risk: critical
blocked_by: [T-05, T-06, T-07]
contract_ids: [AC-005, AC-006]
owner: unassigned
expected_changes:
  - "<Path>ruoyi-vue-plus-namewta/**</Path>"
  - "<Path>release-artifacts/**</Path>"
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/**</Path>"
  - "<Path>release-artifacts/**</Path>"
  - "<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/evidence/**</Path>"
read_only_paths:
  - "<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/evidence/inventory/**</Path>"
shared_paths: []
shared_path_owners: []
---

# Ticket T-08: Runtime/Nacos 一次性硬切（无双读）

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/ticket/08-nacos-hard-cut.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/evidence/T-08.md</Path>`

实现本 Ticket 时，Lead 与 implementation subagent 必须按顺序完整读取总体 Map，读取项目 Skill 的 frontmatter 与入口并只展开适用于 `ALL`/`T-08` 的匹配项，再读取本 Ticket 与相关上游工件。Map 中的 Skill 是最低必读集合；新的匹配项先由 Lead 同步到 Map 并重新校验。

**冻结投影（覆盖旧 S-spec 默认）：** 公开仓 **WTA-plus**（`NAMEWTA/WTA-plus`）；布局=**docs 聚合镜像**（非强制 backend/frontend/docs/speculo 四顶层）；Nacos=**一次性硬切无双读**；上游不需要；旧库用户一律 `wta`（T-17）。

## 1. 战略与来源

- **目标：** 按 ADR-006/LOG-018 完成自有 Nacos/runtime 标识迁移：**一次性硬切、无双读**。发版窗口内人工迁完 Nacos 配置后再切应用。
- **可观察产出：** 对照表覆盖 data-id / application.name / discovery / gateway；窗口 checklist 完成或失败回滚；无永久/临时双读上线。
- **来源：** ADR-006（硬切）、AC-RUNTIME、AC-006 硬切门禁、LOG-018。
- **当前事实：** 已知示例 data-id：`ruoyi-namewta.yml`→`wta-*.yml`；compose 服务名默认 KEEP `namewta-*`。**废止**任何 14 自然日双读 / 退出勾选 a–e / DEC-NACOS 14d 表述。
- **Planning Depth 原因：** 运行时标识对照与发版窗口硬切；失败即停与回滚。

## 2. 决策状态

### 已锁定决策

- 无双读。
- 失败即停窗口。
- 回滚=切回旧 id + 旧构建。
- 禁止只改代码不迁 Nacos 内容。

### 已采用的低影响假设

- 对照表行由 inventory 补全。

### blocked-by-auth

**CTO HOLD / 三门未授权。** 本票在 `implementation_authorized=true`（及本票适用的 `public_repo_publication_authorized` / `legacy_repo_mutation_authorized`）书面翻转前 **禁止执行** 任何产品树改写、建仓、push、旧 remote mutation。当前三门均为 `false`。本文仅为 SpecDev 规划合同。

### blocked-by-CTO / AMBIGUOUS

无剩余高影响 AMBIGUOUS（LOG-017/018 / ADR-006硬切 / ADR-012 / ADR-013 已收口）。仅余三门书面授权等待 CTO；不阻塞票结构。

### 未决问题

无。


## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| runtime 对照表、发版窗口步骤、回滚。 | deploy skill、inventory。 | 双读兼容期；无限期双读；密码策略。 |

## 4. 要构建什么

补全旧→新对照 → 发版窗口人工复制/迁移 Nacos → 切换应用只认新 id → 验证；失败回滚旧 id。

## 5. 实现契约

- **入口：** Nacos 控制台/配置与应用配置。
- **输出：** 硬切完成证明或回滚证明。
- **不变量：** 窗口外不要求双读。
- **失败：** 未迁配置就切应用 → 停窗口。
- **兼容要求：** 见发布/迁移节；未授权不实施。
- **安全与隐私要求：** 不提交密钥；公开前走 T-12。

## 6. 执行路线

1. 授权确认。
2. 完成对照表（data-id/application.name/discovery/gateway/shared refs/profile files/workdir）。
3. 发版窗口：人工迁 Nacos 旧→新。
4. 切换应用只认新 id。
5. 验证一致性。
6. 失败：切回旧 id 并停。

## 7. 路径访问契约

- **预计修改点：** 与 `expected_changes` 对齐。
- **可写范围：** 与 `writable_paths` 对齐；越界前必须停止。
- **只读上下文：** 与 `read_only_paths` 对齐。
- **共享路径：** 无。
- **保留或不动：** 第三方 KEEP 坐标/import；旧仓 Git 历史对象。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 对照表 | 文档 | Kind/Old/New 齐全 | 是 | evidence/T-08.md |
| 硬切窗口 | checklist | 先迁配置再切应用 | 是 | evidence/T-08.md |
| 回滚演练 | 步骤 | 可切回旧 id | 是 | evidence/T-08.md |
| 无双读 | 配置/代码 | 无双读实现 | 是 | evidence/T-08.md |
| KEEP | rg | 第三方不变 | 是 | evidence/T-08.md |

- **KEEP 回归（强制）：** 任意波次后 `rg`/compile 证明 `org.dromara.sms4j` / `org.dromara.warm` / `org.dromara.easyes` / `org.dromara.mica.mqtt` 坐标与 import **字节级保留**（AC-KEEP / ADR-003）。
- **禁止 zero-match 当验收：** 全局 `ruoyi`/`org.dromara` 清零 **不是** 通过条件（NAC-05 / HC-12）。

- **Workspace checks：** current-workspace 非 E2E（lint/build/`rg`/checklist）按 Goal Plan；未授权前仅文档/inventory 写面。
- **E2E disposition：** not-required：本票为命名/仓库形态/门禁；跨边界 UI 冒烟留给 T-11 可选。
- **E2E owner/environment：** Lead；若日后 required，则 parent-candidate / current-workspace（按 Goal Plan）；禁止在 Ticket source worktree 宣称 E2E 通过。
- **Integration evidence：** implementation/source commit、direct-parent 或 candidate、父分支 result SHA（授权实施后由 Lead 记入 Evidence）。

## 9. 发布、迁移与恢复

- **迁移顺序：** 对照表→窗口迁配置→切应用。
- **兼容窗口：** **无**（硬切）。
- **监控：** 窗口内错误率/启动失败即停。
- **回滚：** 切回旧 data-id + 旧构建。
- **不可逆：** 窗口结束后旧 id 废弃需运维确认。
- **收缩：** 旧 data-id 引用清零（文档/compose/种子）。

## 10. 验收标准

- [ ] `AC-005`：runtime 标识一致。
- [ ] `AC-006`（硬切门禁）：发版窗口完成或已回滚；**无** dual-read exit a–e。
- [ ] 无 14d 双读合同残留。
- [ ] 失败即停已演练/写明。
- [ ] 实现开始前已完整读取 Tickets Map，已读取项目 Skill 入口并完整展开适用于 `ALL`/`T-08` 的匹配项。
- [ ] 验证矩阵全部执行并记录到 `<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/evidence/T-08.md</Path>`（完成时；**本轮不新建伪 Evidence**）。
- [ ] 实际项目修改未超出 `writable_paths`。
- [ ] Ticket 按 Goal Plan 形成非空 implementation/source commit（**仅授权实施后**）；direct-parent 或 candidate 验证由 Lead 记录。
- [ ] E2E disposition 已声明/执行；环境为 current-workspace 或 parent-candidate/direct-parent 规则。
- [ ] 未发生未批准的范围、契约或发布偏差；未翻转未授权门禁。
- [ ] Ticket、Tickets Map 和 Evidence 状态一致。
- [ ] **blocked-by-auth：** 执行前三门/适用门已书面 true。

## 11. SKILL 调用计划

依据 `<Path>{roots.workflows}/specdev/common/rules/skill-invocation.md</Path>`：本票 `skill_bindings` 绑定 Map 中适用于 ALL/`T-08` 的每一 Path。

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
