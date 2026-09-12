# 报告契约

## 输出位置

每次评估都会原子替换：

```text
docs/upstream/current/
|-- state.json
|-- diff_report.md
`-- conflict_report.md
```

`state.json` 只保存创建时间、冻结的上游目标和最终产品合并 SHA。报告使用中文，并使用完整 SHA 标识所有 ref。历史评估仍可通过 Git 历史查看；不要在工作树中保留带日期的报告目录。

所有动态清单都属于该目录。不得将脏路径、变更文件、冲突、获取详情或运行历史持久化到 `upstream-sync-state.json`；不得将这些内容写入 `customization-map.md`。

## 差异报告

后端和前端均须包含：

- 检查点来源、已集成上游 SHA、观测到的上游 SHA、产品 SHA、合并基点和新鲜度；
- `integrated_upstream_sha..observed_upstream_sha` 中按顺序排列的仅上游提交；
- 完整的 `name-status` 和 `numstat` 文件清单；
- 自检查点以来也被产品修改的上游路径；
- 匹配认证、授权、Client、菜单、SQL、OSS、通知、工作流、依赖或构建热点的路径；
- 使用冻结 SHA 的可复现 Git 命令；
- 一份 `现状 Merge 清单`，汇总每个仓库的提交/文件数量、Git 冲突、定制风险路径和当前处置。

脚本会创建客观的清单基线。随后，Agent 将受影响路径映射回稳定的 `customization-map.md` 不变量，并只添加有路径差异支持的语义结论。报告不嵌入无界的完整补丁。

## 冲突报告

使用 `git merge-tree --write-tree --messages <product-sha> <upstream-sha>`，确保评估不会修改索引或工作树。该命令可能向 Git 对象数据库写入不可达的树对象，但不会创建产品合并或移动 ref。

以下类别必须分开：

1. `Git 确认冲突`：merge-tree 返回的未合并路径/阶段。
2. `可自动合并的双方重叠`：产品和新上游都修改了该路径，但 merge-tree 未报告树/文本冲突。
3. `定制契约风险`：即使没有重叠，也需要进行语义审查的高风险上游路径。
4. `未提交工作树重叠`：未提交路径与上游增量相交；从基于提交的模拟中排除。

始终创建冲突报告。文本冲突为零时必须明确写出为零，不得省略或描述为安全合并。注意，merge-tree 无法检测编译、运行时、API、SQL 迁移、授权或行为回归。
