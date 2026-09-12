# 上游合并冲突客观报告

- 运行 ID：`2026-09-06T183605+0800-current`
- 生成时间：`2026-09-06T18:36:05+08:00`
- 模拟方式：`git merge-tree --write-tree --messages <product-sha> <upstream-sha>`
- 工作树说明：模拟只使用提交固定点；未提交修改不进入 merge-tree。

## backend

| 固定项 | 值 |
|---|---|
| 产品 SHA | `28ce64ecd9b5a479089106aa961a77bdd41c5209` |
| 上游 SHA | `bffc39a89fd6ed196031e71cbceefd9986eecce8` |
| merge-base | `bffc39a89fd6ed196031e71cbceefd9986eecce8` |
| merge-tree 状态 | `clean` |
| merge-tree exit code | `0` |
| 结果 tree | `087d6c7cb92e6edb1b8110c946b97f6d8943f72c` |
| Git 确认冲突数 | `0` |

### Git 确认冲突

- Git 未报告文本或树冲突。

### 可自动合并的双方重叠

- 没有双方同时修改但可自动合并的路径。

### 定制合同风险

- 未命中内置热点分类；仍须核对 customization map。

### 未提交工作树重叠

- 未提交路径与本次上游增量无交集。

### 工作树状态

- 工作树 clean。

### 复现命令

```bash
git -C backend merge-tree --write-tree --messages 28ce64ecd9b5a479089106aa961a77bdd41c5209 bffc39a89fd6ed196031e71cbceefd9986eecce8
```

## frontend

| 固定项 | 值 |
|---|---|
| 产品 SHA | `10e662d825aa24d4ae7adb9d73ea9a4be2683ce4` |
| 上游 SHA | `a85fa0aee44f6f12dc35198126914ce722ee8622` |
| merge-base | `a85fa0aee44f6f12dc35198126914ce722ee8622` |
| merge-tree 状态 | `clean` |
| merge-tree exit code | `0` |
| 结果 tree | `4b0f4f9f7a53b0a4f2c550f50df486577de57b59` |
| Git 确认冲突数 | `0` |

### Git 确认冲突

- Git 未报告文本或树冲突。

### 可自动合并的双方重叠

- 没有双方同时修改但可自动合并的路径。

### 定制合同风险

- 未命中内置热点分类；仍须核对 customization map。

### 未提交工作树重叠

- 未提交路径与本次上游增量无交集。

### 工作树状态

- 工作树 clean。

### 复现命令

```bash
git -C frontend merge-tree --write-tree --messages 10e662d825aa24d4ae7adb9d73ea9a4be2683ce4 a85fa0aee44f6f12dc35198126914ce722ee8622
```

## 局限

`merge-tree` 只能描述冻结提交的 Git 文本/树合并结果。零文本冲突不代表编译、运行时、API、权限、SQL 迁移或业务语义安全；必须继续执行 customization map 复核与项目质量门禁。

## 2026-09-06 三仓库基线收口

- 父仓库收口前为 `f54fed471c4612d5a69270f40517568806404365`，前端产品为 `10e662d825aa24d4ae7adb9d73ea9a4be2683ce4`，后端产品为 `292b04c82fdd9c3ccca8e0da35d090fc0bcb0f84`；获取三个 origin 与两个 upstream 成功，无待获取的上游功能增量。
- 前端候选 `d1748f3506259fa75eced87d6983dd14292b9a68` 已是产品 main 的祖先，无需再次合并。
- 后端候选 `27cdd71bfea6998eedce181cd3ca6168abf98c56` 的逐条上游合并与两项修复已被产品 main 集成。两项修复的 `git cherry` 均为 `-`；候选与 main 的工作流通知 API 差异来自产品后续主动调整。
- 使用 `28ce64ecd9b5a479089106aa961a77bdd41c5209` 合并候选历史。两处冲突为 `FlwCommonServiceImpl.java` 和 `WorkflowNotifyCallerUnitTest.java`，保留产品当前 `NotificationApplicationService` 合同；合并前后 tree 均为 `087d6c7cb92e6edb1b8110c946b97f6d8943f72c`，未改动源码。
- 后端 `mvnw.cmd test` 的全部 reactor 模块报告 `BUILD SUCCESS`；Surefire 汇总 553 tests、0 failures、0 errors、26 skipped。PowerShell 5 对 Maven stderr 的包装使外层会话返回 1，日志含 `NativeCommandError`；此处仅以 Maven 与 Surefire 报告陈述测试结果，不将 shell exit 1 改写为 0。
- 前端 `pnpm architecture:check` 与 `pnpm typecheck` 均 exit 0。此次历史收口未改变前端或后端文件树，因此未重复打包及浏览器验收。
- 两个 `codex/upstream-*-20260905` 分支已删除，两个旧 worktree 已从 Git 注销；上游镜像分支及基线标签保留。Windows 长路径/非空目录导致旧目录部分残留，进一步文件清理被自动审批拦截，等待明确清理决定。
- 所有操作均为本地收口，未推送远程；已记录的上游实际集成检查点保持不变。
