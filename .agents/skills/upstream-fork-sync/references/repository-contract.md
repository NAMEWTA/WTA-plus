# 仓库契约

## 仓库映射

| ID | 路径 | 产品分支 | Origin 跟踪 ref | Upstream 跟踪 ref | 本地镜像 | 不可变基线标签 |
|---|---|---|---|---|---|---|
| `backend` | `backend` | `main` | `origin/main` | `upstream/6.X` | `6.X` | `namewta-base-upstream-6x` |
| `frontend` | `frontend` | `main` | `origin/main` | `upstream/6.X-Vue` | `6.X-Vue` | `namewta-base-upstream-6x-vue` |

父仓库只有 `origin/main`；它拥有文档和子模块 gitlink，不拥有子仓库的源码历史。

`docs/upstream/customization-map.md` 只维护稳定的 Fork 不变量和审查热点。当前 SHA、合并状态、冲突、脏路径和评估结论属于 `docs/upstream/current/`；旧快照由 Git 历史保存。

## Ref 不变量

- `main` 包含产品工作。绝不能将产品提交放到镜像上。
- 只有在镜像当前末端提交是已获取上游末端提交的祖先时，才能推进镜像。
- 基线标签绝不移动。
- 不得推送到 `upstream`。将镜像或产品 ref 推送到 `origin` 需要单独的明确授权。
- 不得自动 stash、reset、clean、rebase、强制更新、删除分支/工作树、提交、合并或更新子模块指针。
- 脏工作树不会阻止基于提交的评估，但会阻止推进已检出的产品分支，且必须在冲突报告中单独表示。

## 刷新语义

`--fetch` 获取父仓库的 `origin`、每个子仓库的 `origin` 以及每个子仓库的 `upstream`。上游新鲜度按仓库分别计算：

- `fresh`：本次运行成功获取该仓库的上游。
- `cached`：未请求网络刷新。
- `stale`：已请求刷新但失败；报告使用本地保留的远程跟踪 ref。

`--advance-mirrors` 和 `--advance-products` 会在更新任何请求的本地 ref 前执行完整预检。已检出的分支使用 `git merge --ff-only`；未检出的分支使用比较并交换式的 `git update-ref`。出现分叉会停止整个推进阶段。

## 集成检查点发现

不存在有效的已保存检查点时：

1. 检查不可变基线标签之后产品的第一父提交合并记录。
2. 选择最新的合并提交，要求其非首个父提交是观测到的上游末端提交的祖先。
3. 将该合并提交的上游父提交记录为 `integrated_upstream_sha`，将合并提交本身记录为 `integration_commit_sha`。
4. 如果没有符合条件的产品合并提交，则使用唯一的 `merge-base(product, upstream)`，并将来源标记为 `derived_merge_base`。

绝不能仅根据镜像末端提交推断已集成检查点。Cherry-pick 的补丁可以单独报告，但不得推进图检查点，因为补丁等价不等于祖先关系。
