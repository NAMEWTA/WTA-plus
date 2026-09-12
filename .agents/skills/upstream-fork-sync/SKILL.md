---
name: upstream-fork-sync
description: 评估或获授权集成 wta-vue-plus-docs 上游 Fork 时使用；不用于普通分支合并。
---

# 上游 Fork 同步

对每个产品仓库分别记录以下三个事实：最近获取的上游末端提交、本地镜像末端提交，以及实际集成到产品 `main` 的上游提交。获取上游或快进镜像都不会推进已集成检查点。

## 必需上下文

1. 在评估或集成前，读取仓库 `AGENTS.md`、`.agents/skills/engineering-standards/SKILL.md` 和 `docs/upstream/customization-map.md`。
2. 读取 [repository-contract.md](references/repository-contract.md)，了解仓库/ref 所有权和副作用门禁。
3. 在初始化、修复或记录状态前，读取 [state-schema.md](references/state-schema.md)。
4. 生成或审查报告时，读取 [report-contract.md](references/report-contract.md)。

## 评估

从父仓库根目录运行。主题使用 ASCII kebab-case 格式。

```bash
node .agents/skills/upstream-fork-sync/scripts/upstream-sync.mjs assess \
  --root . \
  --topic <topic>
```

默认离线运行：使用已有 ref，原子替换 `docs/upstream/current/` 下的 `state.json`、`diff_report.md` 和 `conflict_report.md`，然后原子替换紧凑的当前指针 `docs/upstream/upstream-sync-state.json`。历史评估由 Git 历史保存，工作树只保留当前报告。

- 用户要求刷新网络 ref 时，添加 `--fetch`。上游获取失败会产生 `freshness: stale`；不得将缓存 ref 描述为最新。
- 仅在获取/预检完成后需要快进本地 `6.X` 和 `6.X-Vue` 镜像 ref 时，添加 `--advance-mirrors`。
- 仅在用户要求将本地产品分支与 `origin/main` 同步时，添加 `--advance-products`。该操作只允许快进，并拒绝处理已检出且包含未提交修改的工作树。
- 使用 `--dry-run` 可将完整快照作为 JSON 查看，不写入报告或状态。

生成后，使用精确的 `git diff <base>..<target> -- <path>` 证据检查报告的每一项冲突和高风险重叠。以 `customization-map.md` 作为稳定的来源清单；然后仅根据冻结的 SHA 所支持的结论，补充生成的 `现状 Merge 清单`。将文本/树冲突、自动合并的重叠、定制契约风险和脏工作树重叠分为不同类别。不得将本次运行特有的 SHA、脏路径、冲突列表或结论复制到 `customization-map.md` 或全局 JSON。

## 集成

评估不授权产品合并、提交、推送、标签变更、子模块指针更新或清理操作。在执行这些操作前，必须针对冻结的产品/上游 SHA 获得明确授权。

对于已获授权的集成：

1. 在可恢复的候选分支/工作树中分别集成后端和前端；保留 `--no-ff` 合并提交。
2. 解决报告中的冲突，并重新检查 `docs/upstream/customization-map.md` 中所有受影响的不变量。
3. 运行工程规范要求的适用前端/后端质量门禁。不得将跳过或未运行的检查视为通过。
4. 只有在候选验证和授权完成后，才推进产品 `main`。最后再更新父仓库的子模块指针。
5. 仅当合并提交可从产品 `main` 到达，且所提供的上游 SHA 是该合并提交的非首个父提交之一时，记录新的检查点：

```bash
node .agents/skills/upstream-fork-sync/scripts/upstream-sync.mjs record-integration \
  --root . \
  --change current \
  --repository <backend|frontend> \
  --merge-commit <full-sha> \
  --upstream-sha <full-sha> \
  --verification '<command>: exit 0'
```

`--change` 默认使用全局 `current_change`；评估后该值为 `current`。记录操作会更新当前报告的 `main_merge_sha` 和紧凑的全局检查点；验证证据应保留为命令输出或写入当前报告，不得写入任一 JSON。只有需要新的上游比较时才重新运行 `assess`。

## 停止条件

出现以下任一情况时，停止操作且不得更改 ref 或状态：镜像不是已获取上游 ref 的祖先；保存的检查点不在产品历史中；保存的上游 SHA 不是观测到的上游末端提交的祖先；存在多个合并基点；或任一目标 ref 缺失。报告需要人工协调的确切仓库、ref 和 SHA。
