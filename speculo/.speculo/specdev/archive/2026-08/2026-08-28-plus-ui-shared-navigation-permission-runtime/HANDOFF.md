# Handoff: Plus UI 共享导航与权限运行时完成态

- **更新时间：** 2026-08-28T23:19:58+08:00
- **Change：** `2026-08-28-plus-ui-shared-navigation-permission-runtime`
- **当前状态：** `completed`，未归档
- **前端最终结果：** `plus-ui-namewta/main@07962c7cad9ca4db168b3c423b9e3675f312a874`
- **复审：** `CR-002=approved`，标准轴与规范轴均为 `pass`
- **下一独立 Work：** `<Path>{roots.workflows}/specdev/A-archive-and-consolidate/A-archive-and-consolidate.md</Path>`

## 恢复入口

按以下顺序读取，不要从旧对话或旧 handoff 重新推导状态：

1. `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/.status.json</Path>`
2. `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/triage.md</Path>`
3. `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/goal-plan.md</Path>`
4. `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/tickets-map.md</Path>`
5. `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/reviews/CR-001.md</Path>`
6. `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/reviews/CR-002.md</Path>`
7. `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/T-06.md</Path>`
8. `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/T-07.md</Path>`
9. `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/T-08.md</Path>`

原始行为合同仍由 Spec、ADR 与 CONTEXT 拥有。CR-001 整改没有改变产品目标、后端 Client 权威或 App/Platform 所有权。

## 完成事实

- T-01 至 T-05 的历史实现与 Evidence 保持不变。
- T-06 source `7c77bec3f29bbebede554feb8be808fe9ba885297`：权限指令异常路径先移除 DOM，再原样重抛；Web Kit 16/16。
- T-07 source `00f7c759b51c5c9a7e900a18a9e614b117a83b0c`，result `720bcdafc487846b1260584d9ec2fd324f8db728`：递归菜单 Domain parser、精确 Platform 投影和显式 Admin Router adapter。
- T-08 source `9d67923e9c59904fac292ce186eb5f776f90c954`，result `07962c7cad9ca4db168b3c423b9e3675f312a874`：两个 overlay locator 绑定业务 owner 并等待 transition。
- CR-001 的 DOM fail-close、菜单宽泛类型/双重断言和并发 locator 三项 finding 全部由 CR-002 关闭。
- AC-001 至 AC-015 全部 passed；change blockers 为空。

## 最终验证

所有 required 浏览器检查只在 clean parent-candidate 执行：

- 两个目标场景：2/2。
- 完整 Playwright `--workers=5 --retries=0`：连续三轮各 48/48。
- 标准 `pnpm test:e2e`：48/48。
- Architecture：26 个 workspace、0 violation；架构测试 99/99。
- 根 `lint`、`typecheck`、workspace `test`：通过。
- `build:dev`、`build:prod`：通过；仅保留既有 Vite ineffective dynamic import warning。
- SpecDev validator：普通模式和 clean fixed-result repo 模式均为 0 errors、0 warnings。

## Git 与 Worktree 现场

- `plus-ui-namewta/main` 已包含 T-06、T-07、T-08 source commits，HEAD 为 `07962c7cad9ca4db168b3c423b9e3675f312a874`。
- 用户已明确授权全面清理已完成 worktree；本 change 的 T-06/T-07/T-08 source/candidate worktree 与对应本地分支均已清理。
- `plus-ui-namewta` 主检出仍有用户既有的 Skill 删除与 README 修改。本 change 未暂存、回退或提交这些路径；因此以该 dirty 主检出运行 `validate-specdev --repo` 会对所有 Ticket 报 repository dirty，这不是 change finding。
- 密码策略 change 的 8 个 source/candidate worktree 与对应本地分支也已按同一授权清理。
- 父聚合仓库有大量其他并行改动；本轮没有创建父仓库 commit，也没有改写其他 change。

## 剩余授权边界

- **Source cleanup：** 已授权并完成；Git worktree 注册仅剩前后端各自的 `main`。
- **Archive：** 尚未执行；需要进入 archive-and-consolidate dry-run 并取得该流程要求的明确确认。
- **Push / PR / remote merge / deploy / release / production：** 全部不在当前授权范围。

## 下一会话

若用户要求归档，先运行 archive-and-consolidate dry-run，核对知识提升候选与父仓库 dirty 隔离，再请求该流程的独立执行确认。source cleanup 已完成，不要重复创建或删除 worktree，也不要触碰主检出的用户改动。

不要重复执行 T-06 至 T-08，也不要把 dirty 主检出的 validator 结果解释为固定候选失败。
