---
schema_version: 3
artifact: ticket
change: 2026-08-24-upstream-fork-upgrade-remediation
id: T-01
title: 前端质量门禁与占位页面退役
status: done
planning_depth: standard
planning_depth_reason: 涉及工具脚本、路径大小写、浏览器验证和动态路由可达性，但不改变后端公共接口
ready: true
risk: medium
blocked_by: []
contract_ids: [AC-001, AC-004]
owner: codex
expected_changes: ["<Path>plus-ui-namewta/package.json</Path>", "<Path>plus-ui-namewta/pnpm-lock.yaml</Path>", "<Path>plus-ui-namewta/.gitignore</Path>", "<Path>plus-ui-namewta/src/types/auto-imports.d.ts</Path>", "<Path>plus-ui-namewta/src/types/components.d.ts</Path>", "<Path>plus-ui-namewta/src/api/monitor/logininfo/**</Path>", "<Path>plus-ui-namewta/src/views/business/**</Path>", "<Path>plus-ui-namewta/e2e/**</Path>"]
writable_paths: ["<Path>plus-ui-namewta/package.json</Path>", "<Path>plus-ui-namewta/pnpm-lock.yaml</Path>", "<Path>plus-ui-namewta/.gitignore</Path>", "<Path>plus-ui-namewta/src/types/auto-imports.d.ts</Path>", "<Path>plus-ui-namewta/src/types/components.d.ts</Path>", "<Path>plus-ui-namewta/playwright.config.ts</Path>", "<Path>plus-ui-namewta/src/api/monitor/loginInfo/**</Path>", "<Path>plus-ui-namewta/src/api/monitor/logininfo/**</Path>", "<Path>plus-ui-namewta/src/views/monitor/logininfo/**</Path>", "<Path>plus-ui-namewta/src/views/business/**</Path>", "<Path>plus-ui-namewta/e2e/**</Path>"]
read_only_paths: ["<Path>plus-ui-namewta/tsconfig.json</Path>", "<Path>plus-ui-namewta/src/views/login.vue</Path>", "<Path>plus-ui-namewta/src/hooks/oss/**</Path>"]
shared_paths: []
shared_path_owners: []
---

# Ticket T-01: 前端质量门禁与占位页面退役

- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-24-upstream-fork-upgrade-remediation/evidence/T-01.md</Path>`

## 1. 战略与来源

- **目标：** 消除 TS1149，建立 test/typecheck/browser scripts，并移除没有业务合同的四个空页面。
- **可观察产出：** 开发者一条命令可稳定运行每类前端门禁，未完成业务页面不再作为可加载组件存在。
- **来源：** CR-002 standards P1、specification P1/P2；AC-001、AC-004。

## 2. 决策状态

- API 目录保留仓库既有 camelCase `loginInfo`，所有 import 使用精确大小写；视图目录保持既有路由命名 `logininfo`。
- Vitest 使用现有四项测试；Playwright 只覆盖可在浏览器确定验证的登录上下文失败关闭和未完成入口不可达合同。
- 占位组件直接删除，业务能力另立 change 后再恢复。
- 未决问题：无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| casing、scripts、Playwright、删除占位组件 | 现有 login/OSS hooks 和 Vitest | 编写四项业务功能、修改后端 API |

## 4. 要构建什么

监控登录日志页在任何大小写语义文件系统上都解析到唯一 API 目录；开发者可用正式 scripts 运行 unit/type/browser 门禁；用户不再能加载无功能的业务组件。

## 5. 实现契约

- `pnpm typecheck` 等价于 `vue-tsc --noEmit`，不得缩小 tsconfig scope；API import 必须精确匹配 `loginInfo`。
- `pnpm test` 运行 Vitest 一次并正确传递失败码；`pnpm test:e2e` 运行 Playwright。
- 缺失、失败或畸形 Client context 时登录提交保持不可用。
- 不保留大小写兼容目录；API 目录唯一为 camelCase `loginInfo`，视图目录唯一为既有小写 `logininfo`。

## 6. 执行路线

1. 固定现有 typecheck 失败和缺失 scripts 为 red 证据。
2. 迁移 API 路径并更新 imports，加入 test/typecheck scripts。
3. 增加最小浏览器合同测试并删除占位组件。
4. 执行 lint、typecheck、Vitest、Playwright 和生产构建。

## 7. 路径访问契约

- **可写范围：** 仅 frontmatter `writable_paths`。
- **只读上下文：** 现有 tsconfig、登录页与 OSS hooks。
- **共享路径：** 无。
- **保留或不动：** 后端接口、自动生成声明和全仓格式。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常 | package scripts | `pnpm lint && pnpm typecheck && pnpm test && pnpm build:prod` | 全部退出 0 | `<Path>{roots.state}/specdev/changes/2026-08-24-upstream-fork-upgrade-remediation/evidence/T-01.md</Path>` |
| 失败关闭 | browser | `pnpm test:e2e` | 畸形 Client context 不允许提交 | `<Path>{roots.state}/specdev/changes/2026-08-24-upstream-fork-upgrade-remediation/evidence/T-01.md</Path>` |
| 回归 | git index | `git ls-files | rg 'login[Ii]nfo'` | API 仅 `loginInfo`，视图仅 `logininfo`，import 与真实大小写完全一致 | `<Path>{roots.state}/specdev/changes/2026-08-24-upstream-fork-upgrade-remediation/evidence/T-01.md</Path>` |

- **E2E disposition：** required；Lead 在 current workspace 执行浏览器合同。
- **E2E owner/environment：** Lead / current-workspace。
- **集成出口：** 子仓结果已提交并推送，父仓以 `e623b9e2e9381f39721b15bcb779d260d03a84e4` 记录最终可复现快照。

## 9. 发布、迁移与恢复

- **迁移顺序：** casing 迁移与 import 同步完成后再启用 typecheck。
- **回滚或前向恢复：** 恢复原目录和组件即可；未产生持久化数据。
- **不可逆操作与批准点：** 无。

## 10. 验收标准

- [x] AC-001 与 AC-004 的前端部分通过。
- [x] 实际修改未超出 writable paths；fresh-checkout 声明文件修复已补入合同。
- [x] Evidence 记录 red/green、命令和未验证项。
