# Verify: 2026-09-16-namewta-fullstack

对照 `<Path>{roots.workflows}/learning/G-goal/references/stop-rules.md</Path>` 与本 Change `goal/goal-plan.md` §0 / §6 / §8。课写完不是完成。本文件由 `/goal` 在停止规则触发后写入。

检查时间：`2026-09-17T14:40:00.000Z`。roots 来自已打开的 `<Path>{roots.state}/workspace.json</Path>`：`state=speculo/.speculo`，`workflows=speculo/workflows`。

本文件覆盖并取代 `2026-09-16T07:13:10.000Z` 那份过程即停草稿（当时 `children/` 尚未嵌入、矩阵 covered=0）。`children/` 已于 `2026-09-16T07:53:45.456Z` 嵌入；U1…U9 已按 teach-then-mine 写课并挖掘。

## 触发的停止

**完成即停**。范围内格子已全部闭合，85 个 chain 节点均有 L 合同 Lesson，每课一份 `GP-L-NNN-b01.md`（无 b02/b03），`goal/verify.md` 已按当前 stop-rules 重写。

## 完成即停（逐条）

| 条款 | 结果 | 证据 |
| --- | --- | --- |
| 范围内格子全部为 `covered`、`deferred(reason)` 或 `covered-by-parent` | pass | 闭合统计 in-scope=175，covered=163，deferred=7，covered-by-parent=5，uncovered=0。U9 D 将最后 9 格盖上：`MySQL 8.4 基座`、`Redis 8`、`dao-mapper-ownership`、`mysql-base`、`失败路径 会话丢失`、`存储点 MySQL`、`NotifyClient.send / NotifyDispatcher.send`、`LoginHelper / StpUtil`、`BaseMapperPlus / QueryBuilder`。证据列均指向对应 Lesson + `GP-L-08N-b01`。未发明库存外符号。 |
| 每个 chain 节点有一份 L 合同 Lesson（本树内） | pass | `goal/chain.md` L-001…L-085 状态均为 `mined` 或 `revised`（`revised` 来自同课 `re-dispatch-L`，不重置问数）。`将写入` 路径均在本 Change 树：architecture/sso/notify/third 在 `children/<id>/lessons/`，其余在本根 `lessons/`。frontmatter `expression_level=eli5`、`coverage_depth=deep`、`estimated_minutes` 30–40；教学覆盖含 宏观 / 机制 / 精确定义 / English / 正例 / 反例 / 迁移 / 误区 / 总结 / 来源 / 文字等价物。 |
| 每节新课或改写课有对应关闭态 probe，且至少改变过一个矩阵格子或显式 defer | pass | `goal/probes/` 恰有 85 份 `GP-L-001-b01`…`GP-L-085-b01`，零 `b02`、零 `b03`。每课 5 问、`audience=mine`、`question_budget_used: 5`。85 份 frontmatter `status: closed`，关闭段同步为 `closed`；Change 树探针无未关闭态。U9 四份 Q5 均为 `defer`，缺口表显式 defer 邻格并建议 Lead 盖本课格子；Lead 已按磁盘+探针改矩阵。probe 正文无问答目录路径、无行首应答/提交字段、无掌握标记。 |
| `goal/verify.md` 已写 | pass | 本文件。不再声称缺失 `children/` 或独立根 locator。 |

完成即停：**满足**。

## 过程即停（逐条）

| 条款 | 结果 | 证据 |
| --- | --- | --- |
| 本 mine unit 没有矩阵格子发生变化 | not fired | U9 D 将 uncovered 9 → 0 |
| 出现第 11 问，或 `GP-*-b03.md` | not fired | 每课 5 问；仓库无 b03 |
| 交错 L/mine（阶段 T 完成前启动 miner，或 `L → mine → L`） | not fired | U9 先把 L-082…L-085 标 `written` 再写探针；chain 无 `planned`/`writing` 课与探针并存 |
| miner 写入 `lessons/` 或 matrix | not fired | miner 只写 `goal/probes/GP-L-08N-b01.md`；矩阵由 Lead D 改 |
| 同一 mine unit 规划或扇出超过 15 节 | not fired | U9 = 4；U8 = 15；其余均 <15 |
| 用户暂停 | not fired | 无暂停指令 |
| 缺源码、缺 baseline，或目标主要是无法核对的生成物 | not fired | `/srv/WTA-plus` 源码与 Change `baseline.md` 存在 |
| 所有权冲突或路径越界 | not fired | 写入限于本 Change `lessons/`（含 `children/*/lessons/`）与 `goal/` |
| split depth=2 后仍不清 | not fired | 全部 probe `split_depth: 0`；split-proposal none |

## Locator 核对

| change_id | locations.json locator | 是否 `…/children/…` |
| --- | --- | --- |
| 2026-09-14-namewta-architecture | `changes/2026-09-16-namewta-fullstack/children/2026-09-14-namewta-architecture` | 是 |
| 2026-09-14-wta-sso | `changes/2026-09-16-namewta-fullstack/children/2026-09-14-wta-sso` | 是 |
| 2026-09-14-wta-notify | `changes/2026-09-16-namewta-fullstack/children/2026-09-14-wta-notify` | 是 |
| 2026-09-14-wta-third | `changes/2026-09-16-namewta-fullstack/children/2026-09-14-wta-third` | 是 |
| 2026-09-16-namewta-fullstack | `changes/2026-09-16-namewta-fullstack` | 本根 |

## 不算完成（对照）

| 条款 | 本 run |
| --- | --- |
| 课写完 | 不是完成；已继续 mine + D + 本文件 |
| 文件巡览结束 | 不是完成 |
| 模型表示已经理解 | 不是完成 |
| 主观百分比 | 未写 |
| 阶段 T 结束但尚未 mine | 不是完成；U9 已 mine |

## 闭合统计（本 run 结束时）

- in-scope 格子总数：175
- covered：163
- deferred：7
- covered-by-parent：5
- uncovered：0

deferred 七行保持计划会话原由：`deferred(test-fixture)`、`deferred(generated)`、`deferred(capability-demo-not-product-room)`、`deferred(separate-process)`、`deferred(vendor-engine)`、`deferred(optional-overlay)`、`deferred(not-on-happy-path-hub)`。

covered-by-parent 五行：前端聚合 Service、档案 Composite 厨房、NotifyDispatcher 之外的 DTO、BO/VO getter 与 converter、与 Controller 同资源的 Mapper XML。

## HARD NO 核对

- 未写问答目录 / homework / review / synthesis / archive
- 未写掌握标记，未复活 Q-quiz
- 未调用 C-consolidate 或 `relocate-learning.mjs`
- 未把 wta-extend、厂商引擎内部、demo 能力展示 Controller 扩进 in-scope
- 未发明库存外函数行
- teach-then-mine：无 L→mine 交错；每 unit ≤15；每课 ≤10 问
