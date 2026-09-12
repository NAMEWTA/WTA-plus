---
schema_version: 3
artifact: ticket
change: 2026-09-01-harden-namewta-full-stack-deployment-skill
id: T-01
title: 版本化完整发布配置合同
status: done
planning_depth: standard
planning_depth_reason: profile 公共 JSON 合同跨校验器与模板，必须保持 v1 读取兼容并为 v2 提供严格失败行为
ready: true
risk: medium
blocked_by: []
contract_ids: [AC-001, AC-002]
owner: codex:/root
expected_changes: ["<Path>.agents/skills/deploy-namewta-environment/scripts/lib.mjs</Path>", "<Path>.agents/skills/deploy-namewta-environment/assets/templates/deployment-profile.json.template</Path>"]
writable_paths: ["<Path>.agents/skills/deploy-namewta-environment/scripts/lib.mjs</Path>", "<Path>.agents/skills/deploy-namewta-environment/scripts/profile-contract.test.mjs</Path>", "<Path>.agents/skills/deploy-namewta-environment/scripts/deployment-tools.test.mjs</Path>", "<Path>.agents/skills/deploy-namewta-environment/assets/templates/deployment-profile.json.template</Path>"]
read_only_paths: ["<Path>release-artifacts/.env.example</Path>", "<Path>release-artifacts/docker/**</Path>"]
shared_paths: []
shared_path_owners: []
---

# Ticket T-01: 版本化完整发布配置合同

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-09-01-harden-namewta-full-stack-deployment-skill/ticket/01-version-deployment-profile-contract.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-09-01-harden-namewta-full-stack-deployment-skill/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-09-01-harden-namewta-full-stack-deployment-skill/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-09-01-harden-namewta-full-stack-deployment-skill/evidence/T-01.md</Path>`

## 1. 战略与来源

- **目标：** 让部署输入精确描述 Compose、镜像、前端、能力开关和稳定窗口。
- **可观察产出：** v2 profile 完整时通过，缺失或矛盾时给出精确错误；v1 仍能通过旧合同。
- **来源：** `US-001`、`AC-001`、`AC-002`。
- **当前事实：** `<Path>.agents/skills/deploy-namewta-environment/scripts/lib.mjs</Path>` 只接受 schema v1，未验证发布身份。
- **Planning Depth 原因：** 这是多调用方共享 JSON 合同，需要兼容与负向矩阵。

## 2. 决策状态

### 已锁定决策

- v2 新增 release 与 capability 合同；v1 继续接受但不获得 v2 rollout 语义。
- profile 只保存 secret 来源和存在性要求，不保存真实 secret。

### 已采用的低影响假设

- Compose 文件使用目标机绝对路径，便于验证授权根目录。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| v2 模板、校验和兼容测试 | 现有 root/port/Nacos 安全校验 | 状态、报告和远端执行 |

## 4. 要构建什么

执行者填写 v2 模板后，CLI 在连接服务器前确认精确发布身份和前后端构建合同；占位值、越界路径、项目不一致、错误前缀或无效阈值直接失败。

## 5. 实现契约

- **入口或接缝：** `validateProfile(profile)`。
- **输入与输出：** JSON 对象到中文错误数组。
- **公共接口变化：** 接受 schema v1/v2；v2 增加严格字段。
- **不变量：** 不读取 env，不输出 secret，Compose 文件位于授权 root。
- **状态或数据流：** 纯函数，无 I/O。
- **错误与失败行为：** 每个缺失或冲突字段形成可定位错误。
- **兼容要求：** 既有 v1 测试保持绿色。
- **安全与隐私要求：** profile 不包含 KEK 正文。

## 6. 执行路线

1. 新增 v2 正负向测试并确认旧实现红灯。
2. 扩展纯校验函数，同时保留 v1 分支。
3. 将模板升级到 v2 并复核无 secret。
4. 运行定向与既有工具测试。

## 7. 路径访问契约

- **预计修改点/可写范围：** 与 frontmatter 对齐。
- **只读上下文：** release env 与 Compose。
- **共享路径：** 无。
- **保留或不动：** 产品前后端、release assets。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| v2 正常路径 | Node unit | `node --test .../profile-contract.test.mjs` | 0 error | `<Path>{roots.state}/specdev/changes/2026-09-01-harden-namewta-full-stack-deployment-skill/evidence/T-01.md</Path>` |
| 缺失/冲突 | Node unit | 同上 | 精确错误 | 同上 |
| v1 回归 | 既有测试 | `node --test .../deployment-tools.test.mjs` | 全绿 | 同上 |

- **Workspace checks：** current workspace Node tests。
- **E2E disposition：** not-required：纯本地 JSON 合同，无跨进程或外部环境行为。
- **E2E owner/environment：** Lead / current-workspace。
- **Integration evidence：** implementation commit 与 direct-parent result。

## 9. 发布、迁移与恢复

- **迁移顺序：** 新部署使用 v2；v1 继续审计和本地渲染。
- **兼容窗口：** v1 读取兼容，无自动升级。
- **监控信号：** 校验错误。
- **回滚或前向恢复：** 恢复模板与校验 commit。
- **不可逆操作与批准点：** 无。
- **收缩条件：** 不适用，不在本 change 删除 v1。

## 10. 验收标准

- [ ] AC-001、AC-002 全部通过。
- [ ] 验证矩阵记录于 Evidence。
- [ ] 路径和 direct-parent 合同满足。
