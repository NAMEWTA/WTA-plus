---
schema_version: 3
artifact: ticket
change: 2026-09-01-harden-namewta-full-stack-deployment-skill
id: T-03
title: 固化全栈滚动发布运行手册
status: done
planning_depth: standard
planning_depth_reason: 文档改变高风险部署顺序与停止规则，必须与新工具和实际 Evidence 一致
ready: true
risk: medium
blocked_by: [T-02]
contract_ids: [AC-009]
owner: codex:/root
expected_changes: ["<Path>.agents/skills/deploy-namewta-environment/SKILL.md</Path>", "<Path>.agents/skills/deploy-namewta-environment/references/rolling-full-stack-release.md</Path>"]
writable_paths: ["<Path>.agents/skills/deploy-namewta-environment/SKILL.md</Path>", "<Path>.agents/skills/deploy-namewta-environment/references/**</Path>"]
read_only_paths: ["<Path>.agents/skills/deploy-namewta-environment/scripts/**</Path>", "<Path>release-artifacts/**</Path>"]
shared_paths: []
shared_path_owners: []
---

# Ticket T-03: 固化全栈滚动发布运行手册

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-09-01-harden-namewta-full-stack-deployment-skill/ticket/03-document-rolling-release-runbook.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-09-01-harden-namewta-full-stack-deployment-skill/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-09-01-harden-namewta-full-stack-deployment-skill/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-09-01-harden-namewta-full-stack-deployment-skill/evidence/T-03.md</Path>`

## 1. 战略与来源

- **目标：** 让后续 Agent 从 Skill 可发现并严格执行经过验证的全栈滚动流程。
- **可观察产出：** 主 Skill 路由清楚，一级 runbook 含精确阶段、门禁、停止与恢复合同。
- **来源：** `US-001..004`、`AC-009`、T-01/T-02 工具合同。
- **当前事实：** 现有 references 分散描述流程，没有单一完整 control flow。
- **Planning Depth 原因：** 文档直接指导高事故半径操作，错误顺序会影响现场。

## 2. 决策状态

### 已锁定决策

- 主 Skill 不复制详细步骤；runbook 为一级 reference。
- 默认备份不弱化；waiver 只作为受限例外。

### 已采用的低影响假设

- 既有专题 references 继续存在，runbook 负责顺序并路由细节。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| Skill、runbook、专题 reference 同步 | 现有安全/中间件/Nacos 文档 | 用户教程、历史复盘全文、远端命令执行 |

## 4. 要构建什么

Agent 选择 takeover/upgrade/release 后，必须按 runbook 从身份冻结推进到候选、数据库、双实例、前端、语义验收和交接；任何 Gate 失败都停止后续步骤。

## 5. 实现契约

- **入口或接缝：** `SKILL.md` 的构建部署与验证路由。
- **输入与输出：** 部署模式和 v2 工件到有序执行合同。
- **公共接口变化：** Skill 操作合同收紧。
- **不变量：** 精确身份、前一步稳定、secret 不回显、失败资产保留。
- **状态或数据流：** discover -> candidate -> backend1 -> backend2 -> frontend -> semantic accept。
- **错误与失败行为：** Gate 失败停止，不清理证据，不推进下一实例。
- **兼容要求：** 既有 audit/fresh-dev/Nacos/OSS 路由保留。
- **安全与隐私要求：** 文档不含真实 endpoint 凭据或 secret。

## 6. 执行路线

1. 新增完整 runbook，映射实际故障到门禁。
2. 同步 takeover/build/verification/database/rollback 专题 reference。
3. 精简并更新主 Skill 路由和命令。
4. 运行链接、重复内容和安全词扫描。

## 7. 路径访问契约

- **预计修改点/可写范围：** 与 frontmatter 对齐。
- **只读上下文：** 工具、release assets 和 Evidence。
- **共享路径：** 无。
- **保留或不动：** agents/openai.yaml、产品代码。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 路由可发现 | 静态 review | `rg` Skill 链接 | 目标文件存在 | `<Path>{roots.state}/specdev/changes/2026-09-01-harden-namewta-full-stack-deployment-skill/evidence/T-03.md</Path>` |
| 安全边界 | 文档审查 | backup/waiver/secret/cleanup 扫描 | 默认安全合同完整 | 同上 |
| 回归 | Skill line/link review | `git diff --check` | 无坏链接/格式错误 | 同上 |

- **Workspace checks：** current workspace 静态检查。
- **E2E disposition：** not-required：文档路由，不执行外部部署。
- **E2E owner/environment：** Lead / current-workspace。
- **Integration evidence：** implementation commit 与 direct-parent result。

## 9. 发布、迁移与恢复

- **迁移顺序：** 工具合同稳定后文档切换。
- **兼容窗口：** 既有专题文档保留。
- **监控信号：** 链接和关键词扫描。
- **回滚或前向恢复：** 恢复文档 commit。
- **不可逆操作与批准点：** 无。
- **收缩条件：** 不适用。

## 10. 验收标准

- [x] AC-009 通过。
- [x] 主 Skill 小于 500 行并直接路由一级 references。
- [x] 路径和 direct-parent 合同满足。
