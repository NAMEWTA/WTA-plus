---
schema_version: 3
artifact: ticket
change: 2026-08-20-namewta-client-rbac-review
id: T-01
title: 同步后端 upstream 6.X
status: done
planning_depth: standard
planning_depth_reason: 需要保护镜像分支与独立 merge commit，但当前只有一个无重叠上游提交。
ready: true
risk: medium
blocked_by: []
contract_ids: [AC-006]
owner: codex-root
expected_changes: ["<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-redis/**</Path>"]
writable_paths: ["<Path>ruoyi-vue-plus-namewta/**</Path>"]
read_only_paths: ["<Path>docs/upstream/customization-map.md</Path>"]
shared_paths: []
shared_path_owners: []
---

# Ticket T-01: 同步后端 upstream 6.X

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-08-20-namewta-client-rbac-review/ticket/01-sync-backend-upstream.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-08-20-namewta-client-rbac-review/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-08-20-namewta-client-rbac-review/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-20-namewta-client-rbac-review/evidence/T-01.md</Path>`

## 1. 战略与来源

- **目标：** 将纯镜像 `6.X` fast-forward 到当前 upstream，并以独立 merge commit 更新产品 main。
- **可观察产出：** 后端产品包含 `387c4f0a2` 的 Redis cache 修复且 NAMEWTA 热点不变。
- **来源：** `AC-006`、`CR-001`、`DEC-004`。
- **当前事实：** upstream 仅修改 CacheConfig，与产品 diff 无重叠，merge-tree 无冲突。
- **Planning Depth 原因：** 涉及共享产品分支但改动局部且已验证无重叠。

## 2. 决策状态

### 已锁定决策

- 镜像只 fast-forward；产品只 merge 不 rebase；不移动基线标签；不 push。

### 已采用的低影响假设

- 本地 merge commit 是后续整改的父提交。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| local 6.X fast-forward、main merge、构建 | 现有 upstream remote 与 customization map | push、业务修复 |

## 4. 要构建什么

在不混入业务整改的前提下更新后端上游基础，并证明当前认证、权限与 Client 定制仍能完整编译。

## 5. 实现契约

- **入口或接缝：** Git refs 和 Maven reactor。
- **输入与输出：** `upstream/6.X@387c4f0a2` -> local `6.X` 和 main merge commit。
- **公共接口变化：** 无。
- **不变量：** mirror fast-forward；base tag 不动；merge commit 不夹带业务编辑。
- **状态或数据流：** fetch 后更新镜像，再从 main 合并镜像。
- **错误与失败行为：** 冲突或构建失败立即停止，main 不进入后续 Ticket。
- **兼容要求：** 保留 customization map 全部热点。
- **安全与隐私要求：** 不适用：纯上游代码同步。

## 6. 执行路线

1. 重查 refs、merge-base、重叠文件和标签。
2. fast-forward local `6.X`，切回 main 创建独立 merge commit。
3. 运行后端完整 package 并审计 merge diff。

## 7. 路径访问契约

- **预计修改点：** `expected_changes` 所列 Redis 模块。
- **可写范围：** 后端子模块，仅来自 merge。
- **只读上下文：** customization map。
- **共享路径：** 无。
- **保留或不动：** 前端、父仓库文档、基线标签。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 镜像纯净 | Git refs | merge-base、diff、tag 检查 | 仅 upstream commit | `<Path>{roots.state}/specdev/changes/2026-08-20-namewta-client-rbac-review/evidence/T-01.md</Path>` |
| 回归 | Maven | `./mvnw clean package` | exit 0 | 同上 |

- **Workspace checks：** current workspace，Git diff 与 Maven package。
- **E2E disposition：** not-required：无业务合同变化。
- **E2E owner/environment：** Lead/current-workspace。
- **Integration evidence：** merge commit、parent before/result SHA。

## 9. 发布、迁移与恢复

- **迁移顺序：** 先镜像后产品 merge。
- **兼容窗口：** 无。
- **监控信号：** 构建与热点 diff。
- **回滚或前向恢复：** revert 本地 merge commit。
- **不可逆操作与批准点：** 无；不 push。
- **收缩条件：** 不适用。

## 10. 验收标准

- [x] `AC-006`：产品包含当前 upstream 且完整构建。
- [x] 验证矩阵写入 Evidence。
- [x] 只修改授权路径。
- [x] 形成非空本地 implementation merge commit并通过 direct-parent 验证。
- [x] E2E disposition 已记录。
- [x] 无未批准偏差。
- [x] Ticket、Map、Evidence 一致。
