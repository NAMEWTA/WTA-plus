---
schema_version: 3
artifact: ticket
change: 2026-09-01-harden-namewta-full-stack-deployment-skill
id: T-02
title: 校验双实例候选与前端产物
status: done
planning_depth: standard
planning_depth_reason: 候选验收跨 Compose 身份、双实例、业务语义、前端静态产物和稳定窗口
ready: true
risk: medium
blocked_by: [T-01]
contract_ids: [AC-003, AC-004, AC-005, AC-006, AC-007]
owner: codex:/root
expected_changes: ["<Path>.agents/skills/deploy-namewta-environment/scripts/verify-deployment-state.mjs</Path>", "<Path>.agents/skills/deploy-namewta-environment/scripts/verify-release-candidate.mjs</Path>", "<Path>.agents/skills/deploy-namewta-environment/scripts/verify-frontend-artifact.mjs</Path>"]
writable_paths: ["<Path>.agents/skills/deploy-namewta-environment/scripts/verify-deployment-state.mjs</Path>", "<Path>.agents/skills/deploy-namewta-environment/scripts/verify-release-candidate.mjs</Path>", "<Path>.agents/skills/deploy-namewta-environment/scripts/verify-frontend-artifact.mjs</Path>", "<Path>.agents/skills/deploy-namewta-environment/scripts/release-candidate.test.mjs</Path>", "<Path>.agents/skills/deploy-namewta-environment/assets/templates/deployment-state.json.template</Path>"]
read_only_paths: ["<Path>.agents/skills/deploy-namewta-environment/scripts/lib.mjs</Path>", "<Path>.agents/skills/deploy-namewta-environment/assets/templates/deployment-profile.json.template</Path>"]
shared_paths: []
shared_path_owners: []
---

# Ticket T-02: 校验双实例候选与前端产物

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-09-01-harden-namewta-full-stack-deployment-skill/ticket/02-verify-release-candidate.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-09-01-harden-namewta-full-stack-deployment-skill/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-09-01-harden-namewta-full-stack-deployment-skill/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-09-01-harden-namewta-full-stack-deployment-skill/evidence/T-02.md</Path>`

## 1. 战略与来源

- **目标：** 将本次实际发布故障转换为候选验收红灯。
- **可观察产出：** 正确候选通过；错误 project、重启、镜像漂移、业务码、asset prefix、稳定窗口和数据保护分别失败。
- **来源：** `US-002`、`US-003`、`AC-003..007`。
- **当前事实：** 现有状态验证只看服务状态、端口、OSS、Nacos 和 endpoint.ok。
- **Planning Depth 原因：** 多类状态必须在一个候选接口中保持一致语义。

## 2. 决策状态

### 已锁定决策

- v2 状态必须能与 v2 profile 对照；传输失败可记录，但最终必须达到连续成功阈值。
- 业务码由每个探针显式允许集合决定，不全局禁止 404。
- 数据 waiver 只允许 dev，且必须具备显式授权、完整 preflight 和 forward-only。

### 已采用的低影响假设

- 使用 Node 标准库解析 Vite `script/link` 资源 URL。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| state v2、候选 CLI、HTML CLI、测试 | T-01 profile、现有 OSS/Nacos 检查 | 网络探测器、浏览器自动化、远端命令 |

## 4. 要构建什么

执行者采集脱敏状态并运行候选 CLI。工具比较配置与实况、验证 server1/server2/frontend 顺序和最终稳定性；前端 HTML 可在传输前单独检查。

## 5. 实现契约

- **入口或接缝：** `verifyState(state,{profile})`、`verifyFrontendHtml(html,profile)` 和两个 CLI。
- **输入与输出：** JSON/HTML 到错误数组和非敏感摘要。
- **公共接口变化：** state schema v2；旧 state 仅保留旧级别校验。
- **不变量：** 两后端同 image ID/tag/config digest，restart 不超限，语义探针覆盖 backend1/backend2/ingress 和认证路径。
- **状态或数据流：** profile + state -> 纯校验 -> CLI 退出码。
- **错误与失败行为：** 一个风险至少一个可定位错误，不吞并。
- **兼容要求：** 无 profile 的旧 state 仍执行原有门禁。
- **安全与隐私要求：** OpenAPI KEK 只记录 present，不记录值。

## 6. 执行路线

1. 为每类已知失败建立红灯夹具。
2. 扩展 state 模板和验证纯函数。
3. 增加候选组合 CLI。
4. 增加前端 HTML 校验和 CLI。
5. 运行新旧测试与受控反向验证。

## 7. 路径访问契约

- **预计修改点/可写范围：** 与 frontmatter 对齐。
- **只读上下文：** T-01 profile 合同。
- **共享路径：** 无。
- **保留或不动：** server、数据库、产品前后端。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 完整候选 | Node unit/CLI | `node --test .../release-candidate.test.mjs` | 通过 | `<Path>{roots.state}/specdev/changes/2026-09-01-harden-namewta-full-stack-deployment-skill/evidence/T-02.md</Path>` |
| 已知失败矩阵 | Node unit | 同上 | 每项稳定失败 | 同上 |
| 旧门禁回归 | Node unit | `node --test .../deployment-tools.test.mjs` | 通过 | 同上 |

- **Workspace checks：** current workspace Node tests。
- **E2E disposition：** not-required：不访问真实服务器；CLI 临时 fixture 覆盖进程边界。
- **E2E owner/environment：** Lead / current-workspace。
- **Integration evidence：** implementation commit 与 direct-parent result。

## 9. 发布、迁移与恢复

- **迁移顺序：** profile v2 后启用 state v2 严格候选验证。
- **兼容窗口：** 旧 state 无 profile 时维持原门禁，但报告不得声称 v2 rollout。
- **监控信号：** CLI 错误、restart、probe、stable window。
- **回滚或前向恢复：** 恢复脚本 commit；不影响现场。
- **不可逆操作与批准点：** 无。
- **收缩条件：** 不适用。

## 10. 验收标准

- [ ] AC-003..007 全部通过。
- [ ] 反向夹具证明每个门禁会红。
- [ ] 路径和 direct-parent 合同满足。
