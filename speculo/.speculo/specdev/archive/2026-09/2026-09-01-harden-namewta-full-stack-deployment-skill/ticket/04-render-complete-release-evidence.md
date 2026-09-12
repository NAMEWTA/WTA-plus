---
schema_version: 3
artifact: ticket
change: 2026-09-01-harden-namewta-full-stack-deployment-skill
id: T-04
title: 渲染完整发布配置与交接证据
status: done
planning_depth: standard
planning_depth_reason: 私密配置和报告跨 profile、secret、state 与文件权限，需兼容和防泄漏回归
ready: true
risk: medium
blocked_by: [T-03]
contract_ids: [AC-008, AC-010]
owner: codex:/root
expected_changes: ["<Path>.agents/skills/deploy-namewta-environment/scripts/render-local-config.mjs</Path>", "<Path>.agents/skills/deploy-namewta-environment/scripts/generate-deployment-report.mjs</Path>", "<Path>.agents/skills/deploy-namewta-environment/assets/templates/**</Path>"]
writable_paths: ["<Path>.agents/skills/deploy-namewta-environment/scripts/render-local-config.mjs</Path>", "<Path>.agents/skills/deploy-namewta-environment/scripts/generate-deployment-report.mjs</Path>", "<Path>.agents/skills/deploy-namewta-environment/scripts/report-rendering.test.mjs</Path>", "<Path>.agents/skills/deploy-namewta-environment/assets/templates/admin-web.env.production.local.template</Path>", "<Path>.agents/skills/deploy-namewta-environment/assets/templates/deployment-report.md.template</Path>", "<Path>.agents/skills/deploy-namewta-environment/assets/templates/deployment-secrets.json.template</Path>", "<Path>.agents/skills/deploy-namewta-environment/assets/templates/release.env.template</Path>", "<Path>.agents/skills/deploy-namewta-environment/assets/templates/upgrade-report.md.template</Path>"]
read_only_paths: ["<Path>.agents/skills/deploy-namewta-environment/scripts/lib.mjs</Path>", "<Path>.agents/skills/deploy-namewta-environment/scripts/verify-deployment-state.mjs</Path>", "<Path>.agents/skills/deploy-namewta-environment/assets/templates/deployment-profile.json.template</Path>", "<Path>.agents/skills/deploy-namewta-environment/assets/templates/deployment-state.json.template</Path>"]
shared_paths: []
shared_path_owners: []
---

# Ticket T-04: 渲染完整发布配置与交接证据

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-09-01-harden-namewta-full-stack-deployment-skill/ticket/04-render-complete-release-evidence.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-09-01-harden-namewta-full-stack-deployment-skill/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-09-01-harden-namewta-full-stack-deployment-skill/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-09-01-harden-namewta-full-stack-deployment-skill/evidence/T-04.md</Path>`

## 1. 战略与来源

- **目标：** 使生成配置和私密报告完整表达 v2 发布合同与恢复证据。
- **可观察产出：** 渲染 production frontend/OpenAPI env，报告记录 Compose、构件、尝试、数据保护和保留资产，且权限/输出不泄密。
- **来源：** `US-004`、`AC-008`、`AC-010`。
- **当前事实：** 当前报告只有服务状态、入口、凭据、通用升级/回滚，无法重建 rollout。
- **Planning Depth 原因：** 多输入私密输出需要完整集成回归。

## 2. 决策状态

### 已锁定决策

- OpenAPI KEK 只进入 `0600` release env，不进入 profile/state/stdout。
- production frontend env 来自 profile 的 build contract。

### 已采用的低影响假设

- 报告继续允许显示用户选择写入私密文件的凭据，但任何 stdout 和 Speculo Evidence 不显示。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 渲染器、私密模板、报告、整套回归 | T-01/T-02 合同与 `writePrivateFile` | 自动 SSH、部署和 secret manager 集成 |

## 4. 要构建什么

执行者从 v2 profile、私密 secrets 和 v2 state 生成本地配置与交接报告；缺少 OpenAPI secret 或候选门禁失败时停止，成功文件均为 `0600`。

## 5. 实现契约

- **入口或接缝：** 既有 render/report CLI。
- **输入与输出：** profile/secrets/state 到私密文件与非敏感 stdout。
- **公共接口变化：** 新增 production frontend 输出，报告章节扩展。
- **不变量：** 权限 0600，候选失败报告退出 2，stdout 不含 secret。
- **状态或数据流：** validate -> render -> chmod -> report verify。
- **错误与失败行为：** 缺 secret/profile/state 直接失败，不输出部分敏感正文。
- **兼容要求：** v1 本地渲染仍工作；严格报告要求 v2 完整状态。
- **安全与隐私要求：** secret 不进入 Git、state 或 stdout。

## 6. 执行路线

1. 更新集成 fixture 并确认新输出/章节红灯。
2. 扩展 secret 映射、release env 与 production frontend env。
3. 扩展报告结构和服务行。
4. 运行完整 Node 测试、CLI、权限和泄漏检查。
5. 运行 SpecDev 与 diff 最终门禁。

## 7. 路径访问契约

- **预计修改点/可写范围：** 与 frontmatter 对齐。
- **只读上下文：** T-01/T-02 合同。
- **共享路径：** 无。
- **保留或不动：** development frontend 模板和 application local 行为除兼容所需外不变。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 完整渲染 | CLI integration | `node --test .../deployment-tools.test.mjs` | 新旧输出正确 | `<Path>{roots.state}/specdev/changes/2026-09-01-harden-namewta-full-stack-deployment-skill/evidence/T-04.md</Path>` |
| 权限/泄漏 | 临时文件与 stdout | 同上 | 文件 0600，stdout 无 secret | 同上 |
| 全回归 | Node/SpecDev/diff | 全部工具测试与 validator | 全绿 | 同上 |

- **Workspace checks：** current workspace 全 Node suite 与静态门禁。
- **E2E disposition：** not-required：本地 CLI 集成覆盖进程和文件系统，外部部署明确 OUT。
- **E2E owner/environment：** Lead / current-workspace。
- **Integration evidence：** implementation commit 与 direct-parent result。

## 9. 发布、迁移与恢复

- **迁移顺序：** v2 工具完成后更新报告；v1 本地渲染保留。
- **兼容窗口：** 旧 report 输入应获得明确升级错误，而非不完整“通过”。
- **监控信号：** CLI exit code、权限、内容和 stdout 测试。
- **回滚或前向恢复：** 恢复本 Ticket commit。
- **不可逆操作与批准点：** 无。
- **收缩条件：** 不适用。

## 10. 验收标准

- [x] AC-008、AC-010 全部通过。
- [x] 全套 Node、SpecDev 和 diff 门禁有 Evidence。
- [x] 路径和 direct-parent 合同满足。
