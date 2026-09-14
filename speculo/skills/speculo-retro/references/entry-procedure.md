# Entry procedure

# Speculo Retro

## 输入

排他白名单，缺一不补扫全仓：

- 当前对话与本次使用的 commands/workflows。
- `<Path>{roots.state}/commands/**</Path>` 报告、active/archive change 状态，以及各 workflow `INDEX.md` 已经声明的知识 store。
- 这些 change 目录里的产物正文。
- 可选已有 issues，用于语义去重。

禁止作为输入：

- `<Path>{roots.skills}/**</Path>`（本 skill 与 `github-npm-ops` 的入口/SOP 除外）
- `<Path>{roots.skills}/**/examples/**</Path>` 与任何 skill fixture
- 项目源码树和构建缓存（`.gradle`、`target`、`node_modules`、`build`、`dist`）

禁止运行 `gradle`、`mvn`、`npm`、`pnpm`、`cargo`、`go test` 等项目构建。

## 流程

1. 读取 `references/friction-taxonomy.md`，按白名单穷尽扫描可用证据并列出原始摩擦；白名单外不走。完成标准：每项都有来源路径或对话节点，且来源落在白名单内。
2. 按 bug、friction、missing-capability、doc-gap、ergonomics 归类，合并同一根因。完成标准：每项只属于一个根因簇，合并关系可追溯。
3. 评估影响与频率，过滤一次性噪声；低信号项标为丢弃或仅记教训。完成标准：每项都有优先级和处置理由。
4. 读取 `references/issue-drafting-sop.md`，生成 issue-ready 提案并与已有 issue 去重。完成标准：标题、证据、问题、建议、验收、影响资产和去重结论齐全。

## 输出

- 按优先级排序的 `file-issue | lesson-only | discard | duplicate` 提案。
- 合并/丢弃说明和调用方执行 `gh` 所需字段。

本 skill 不写文件、不调用外部 API；command 负责报告、确认和 issue 创建。
