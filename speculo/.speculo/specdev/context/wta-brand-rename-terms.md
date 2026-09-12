# WTA 品牌重命名术语

- **Status:** Current
- **Date:** 2026-09-12
- **Source:** `2026-09-12-rename-ruoyi-dromara-to-wta` CONTEXT.md

**first-party / OWNED**：本项目拥有并发布的模块、包、artifact、配置键、品牌文案、种子字面量；可按规则迁到 `wta`（ruoyi 名型）/ `org.namewta`（自有 dromara 包/group）/ `wta-`（模块前缀）。
_Avoid_: 把第三方坐标当 OWNED 盲改

**third-party / KEEP**：上游开源产品坐标与包（sms4j / warm-flow / easy-es / mica-mqtt 等）、license/NOTICE、upstream URL 归因；**不得**机械改为 `org.namewta`。
_Avoid_: 全局 `s/org.dromara/org.namewta/`

**freeze**：旧三仓不再作为开发主线；默认禁止新主线提交、force-push、history rewrite；只读备份；mutation 仅在 `legacy_repo_mutation_authorized`。
_Avoid_: 在旧 remote 上继续 rename 主线

**orphan**：新仓以无旧三仓 Git 祖先的干净历史发布。
_Avoid_: 把旧 `.git` 对象打进新仓

**publication gate**：公开 push 前硬门禁（baseline、secret scan、forbidden files、KEEP verify、license、residual、显式 `public_repo_publication_authorized`）。
_Avoid_: gate 前 push

**wta- prefix**：自有模块目录 / artifactId：`ruoyi-X` → `wta-X`（非裸名 `X`）。
_Avoid_: 目录与 artifact 前缀分裂

**namespace ownership inventory**：自动迁移前对每个 `org.dromara` / `ruoyi` 命中判定 first-party / third-party / attribution / review。
_Avoid_: 「看起来像自有就改」

**authorization gate**：`implementation_authorized` / `public_repo_publication_authorized` / `legacy_repo_mutation_authorized` 默认 false；与 review 结论独立。
_Avoid_: 把 review CHANGES REQUIRED 当成 authorized

**TargetTopology**：单一 public monorepo slug `NAMEWTA/WTA-plus`；去 submodule；前后端+文档/release/speculo 同源。
_Avoid_: 把本地 checkout 路径 `ruoyi-vue-plus-docs` 写成目标拓扑 KEEP

**Nacos 硬切**：自有 data-id `ruoyi`→`wta` 一次性硬切，无双读；发版窗口人工迁配置。
_Avoid_: 重新引入双读兼容期
