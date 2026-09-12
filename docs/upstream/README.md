# 上游能力治理

NAMEWTA 以本地产品架构为权威。后端 `6.X` 与前端 `6.X-Vue` 仅作为上游能力、缺陷修复和安全变化的发现来源，不要求本地目录与上游路径同构。

NAMEWTA 相较上游的当前产品增强见 [增强说明](../namewta-enhancements.md)。这里不维护功能历史，只维护上游变化如何进入当前增强架构的规则。

## 当前入口

- [定制边界](./customization-map.md)：长期有效的本地 owner、必须保持的不变量和验证热点。
- [同步状态](./upstream-sync-state.json)：已确认集成点与最近观测点；`current_change` 为 `null` 表示当前没有评估报告。
- `current/`：执行上游评估后生成的当前 `state.json`、差异报告和冲突报告。后续评估直接替换，历史通过 Git 查看。

## 维护规则

1. 先从 release、commit、PR 或安全公告识别能力，再映射到本地 owner。
2. 上游实现不符合本地边界时采用本地化适配，不恢复已经退役的目录或门面。
3. 同步评估只产生证据，不自动授权 merge、push、tag 或子模块推进。
4. 实际集成必须经过独立子仓库验证，最后再推进父仓库 gitlink。

具体命令与状态合同由 `.agents/skills/upstream-fork-sync/` 维护。
