# 上游增量 Diff 报告

- 运行 ID：`2026-09-06T183605+0800-current`
- 生成时间：`2026-09-06T18:36:05+08:00`
- 主题：`baseline-unification`
- 口径：已确认集成上游点到本次观测上游点；不以镜像分支位置替代集成点。

## backend

| 固定项 | 值 |
|---|---|
| 产品 SHA | `28ce64ecd9b5a479089106aa961a77bdd41c5209` |
| 已集成上游 SHA | `bffc39a89fd6ed196031e71cbceefd9986eecce8` |
| 集成识别方式 | `recorded_merge` |
| 产品 merge commit | `9b66010d29a37b577eb29e48ba7790ad04906119` |
| 观测上游 SHA | `bffc39a89fd6ed196031e71cbceefd9986eecce8` |
| merge-base | `bffc39a89fd6ed196031e71cbceefd9986eecce8` |
| mirror SHA | `bffc39a89fd6ed196031e71cbceefd9986eecce8` |
| freshness | `fresh` |

### 上游新增提交（0）

- 无新增上游提交。

### 文件 Diff

统计：0 files changed

无文件变化。

### 产品重叠面

- 上游增量与产品自集成点后的文件变化无路径重叠。

### 高风险上游路径

- 未命中内置热点分类；仍需按 customization map 复核长期不变量。

### 复现命令

```bash
git -C wta-vue-plus-namewta log --oneline bffc39a89fd6ed196031e71cbceefd9986eecce8..bffc39a89fd6ed196031e71cbceefd9986eecce8
git -C wta-vue-plus-namewta diff --name-status bffc39a89fd6ed196031e71cbceefd9986eecce8..bffc39a89fd6ed196031e71cbceefd9986eecce8
git -C wta-vue-plus-namewta diff bffc39a89fd6ed196031e71cbceefd9986eecce8..bffc39a89fd6ed196031e71cbceefd9986eecce8 -- <path>
```

## frontend

| 固定项 | 值 |
|---|---|
| 产品 SHA | `10e662d825aa24d4ae7adb9d73ea9a4be2683ce4` |
| 已集成上游 SHA | `a85fa0aee44f6f12dc35198126914ce722ee8622` |
| 集成识别方式 | `recorded_merge` |
| 产品 merge commit | `d1748f3506259fa75eced87d6983dd14292b9a68` |
| 观测上游 SHA | `a85fa0aee44f6f12dc35198126914ce722ee8622` |
| merge-base | `a85fa0aee44f6f12dc35198126914ce722ee8622` |
| mirror SHA | `a85fa0aee44f6f12dc35198126914ce722ee8622` |
| freshness | `fresh` |

### 上游新增提交（0）

- 无新增上游提交。

### 文件 Diff

统计：0 files changed

无文件变化。

### 产品重叠面

- 上游增量与产品自集成点后的文件变化无路径重叠。

### 高风险上游路径

- 未命中内置热点分类；仍需按 customization map 复核长期不变量。

### 复现命令

```bash
git -C plus-ui-namewta log --oneline a85fa0aee44f6f12dc35198126914ce722ee8622..a85fa0aee44f6f12dc35198126914ce722ee8622
git -C plus-ui-namewta diff --name-status a85fa0aee44f6f12dc35198126914ce722ee8622..a85fa0aee44f6f12dc35198126914ce722ee8622
git -C plus-ui-namewta diff a85fa0aee44f6f12dc35198126914ce722ee8622..a85fa0aee44f6f12dc35198126914ce722ee8622 -- <path>
```

## 现状 Merge 清单

| 仓库 | 上游增量 | Git 冲突 | 定制风险路径 | 当前处置 |
|---|---:|---:|---:|---|
| backend | 0 commits / 0 files | 0 | 0 | 无上游增量，无需合并 |
| frontend | 0 commits / 0 files | 0 | 0 | 无上游增量，无需合并 |

## 结论边界

本报告提供完整文件清单与可复现固定点，不内嵌无限制完整 patch。代码级结论必须使用上述固定 SHA 的路径级 diff 补证。
