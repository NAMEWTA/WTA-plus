# 状态 Schema

状态 Schema 版本 2 将紧凑的当前指针与每次评估的证据分离。Git 历史仍是祖先关系的权威来源。

## 顶层字段

```json
{
  "schema_version": 2,
  "updated_at": "RFC-3339 timestamp",
  "current_change": "current",
  "repositories": {
    "backend": {
      "integrated_upstream_sha": "full SHA",
      "main_merge_sha": "full SHA or null",
      "observed_upstream_sha": "full SHA"
    },
    "frontend": {}
  }
}
```

全局文件只能作为当前同步索引：

- `integrated_upstream_sha`：已在产品 `main` 中得到证明的上游点。
- `main_merge_sha`：集成该上游点的产品合并提交；如果是从提交图推导的 merge-base，则为 `null`。
- `observed_upstream_sha`：本次评估冻结的上游目标。
- `current_change`：拥有详细状态和报告的目录。

其中不得包含运行历史、ref 配置、脏路径、文件清单、冲突数据、验证日志或集成事件。

## 单次 change 状态

`docs/upstream/current/state.json` 只包含：

```json
{
  "created_at": "RFC-3339 timestamp",
  "repositories": {
    "backend": {
      "upstream_sha": "full SHA selected for this merge",
      "main_merge_sha": "full SHA or null"
    }
  }
}
```

只列出存在上游增量的仓库。在精确的上游目标合并到产品 `main` 并完成记录前，`main_merge_sha` 保持为 `null`。详细事实保留在两份 Markdown 报告中。

## 更新规则

- `assess` 可以创建初始的图推导检查点、替换当前报告目录并替换全局当前索引。
- 获取上游、推进镜像、生成报告或观测新的上游目标，都不得修改 `integrated_upstream_sha`。
- `record-integration` 要求合并提交可从 `refs/heads/main` 到达，存在一个与 `--upstream-sha` 精确相等的非首个父提交，并至少提供一条验证证据字符串。
- 在填充 `main_merge_sha` 并推进全局检查点前，它还必须匹配选定 change 状态中冻结的 `upstream_sha`。
- 仅接受 Schema v1 用于在内存中迁移到紧凑的 v2 结构；未知版本和格式错误的状态会被拒绝。
- Schema v2 拒绝额外的顶层字段或仓库字段，防止详细运行证据泄漏回全局索引。
- 替换全局 JSON 前，先写入两份报告和单次 change 状态。失败的运行不得推进全局指针。
