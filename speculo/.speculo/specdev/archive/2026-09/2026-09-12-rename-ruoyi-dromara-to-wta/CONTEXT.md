# ruoyi / dromara → wta 重命名 — 领域上下文

> **Topology authority:** ADR-010 / ADR-011 only.  
> 本地工作区路径仍可能是 `/workspace/vp-dev/ruoyi-vue-plus-docs` + submodules；这是**当前 checkout 形态**，不是目标交付拓扑。目标 = 新 public monorepo（orphan；slug **`NAMEWTA/WTA-plus`**）+ 旧三仓 freeze。

## Glossary（短）

| Term | Meaning |
|---|---|
| **first-party / OWNED** | 本项目拥有并发布的模块、包、artifact、配置键、品牌文案、种子字面量；可按规则迁到 `wta`（ruoyi 名型）/ `org.namewta`（自有 dromara 包/group）/ `wta-`（模块前缀） |
| **third-party / KEEP** | 上游开源产品坐标与包（sms4j / warm-flow / easy-es / mica-mqtt 等）、license/NOTICE、upstream URL 归因；**不得**机械改为 `org.namewta` |
| **freeze** | 旧三仓不再作为开发主线：默认禁止新主线提交、force-push、history rewrite；只读备份；mutation 仅在 `legacy_repo_mutation_authorized` |
| **orphan** | 新仓以无旧三仓 Git 祖先的干净历史发布（ADR-010 ORPH-*） |
| **publication gate** | 公开 push 前硬门禁：baseline、secret scan、forbidden files、KEEP verify、license、residual classification、显式 `public_repo_publication_authorized` |
| **wta- prefix** | 自有模块目录 / artifactId / 约定产品前缀：`ruoyi-X` → `wta-X`（非裸名 `X`） |
| **namespace ownership inventory** | 实施自动迁移前，对每个 `org.dromara` / `ruoyi` 命中判定 first-party / third-party / attribution / review |
| **authorization gate** | `implementation_authorized` / `public_repo_publication_authorized` / `legacy_repo_mutation_authorized` 默认 false；与 review 结论独立 |

## 概念卡

**OwnedBrand**：命名分裂——**ruoyi→wta**（模块 `wta-*`、用户/品牌等）；**自有 dromara→namewta family**（Java/Maven **`org.namewta`**，与 Docker `namewta-*` / `@namewta/*` 对齐）。「RuoYi / ruoyi」仅允许出现在上游溯源叙述或已分类允许残留中，不再作为运行时坐标或模块名。  
_Avoid_: 抹掉历史说明中的上游专名；强行 `@namewta`→`@wta`；另起 `com.wta`；把自有 Java group 写成已废止的 `org.wta`

**ThirdPartyDep**：SMS4J、Warm-Flow、Easy-Es、mica-mqtt（见 `evidence/THIRD-PARTY-KEEP.md`）。`org.dromara.<product>` KEEP。  
_Avoid_: 全局 `s/org.dromara/org.namewta/`；改 sms4j import

**PackageCoord**：自有 `org.dromara:ruoyi-*` + `org.dromara.{common,system,...}` → `org.namewta:wta-*` + `org.namewta.*`。第三方字节级保留。  
_Avoid_: 只改目录不改 package / AutoConfiguration / mapper namespace

**MigrationWave**：分波次；硬门禁见 goal-plan（documentation → inventory → auth → prepare → rename → residual → verify → publication readiness → publication_authorized push → legacy freeze）。  
_Avoid_: 无闸门 big-bang；未授权改产品代码

**CurrentCheckout（非目标拓扑）**：此刻 box 上常见形态为聚合工作区 `ruoyi-vue-plus-docs` + submodule `ruoyi-vue-plus-namewta` + `plus-ui-namewta`。仅描述现状，**不**表示交付继续 submodule。  
_Avoid_: 把 checkout 路径 KEEP 写成 ADR；默默 `mv` 根目录导致其他 agent 失联

**TargetTopology（ADR-011 + Spec DEC-SLUG/LAYOUT；LOG-017）**：单一 public monorepo；slug **`NAMEWTA/WTA-plus`**；布局镜像现 `ruoyi-vue-plus-docs`；去 submodule；前后端+文档/release/speculo 同源。  
_Avoid_: 先 public 再清理；把旧 `.git` 历史打进新仓；把本地 checkout 路径 `ruoyi-vue-plus-docs` 重新解释成目标拓扑 KEEP

**AmbiguousHit**：无法仅靠字符串规则判定的命中 → `MUST REVIEW` / CTO。  
_Avoid_: 「看起来像自有就改」

**Nacos 命名（LOG-018 / ADR-006）**：**一次性硬切（无双读）**；发版窗口人工迁旧 data-id→新 `wta-` data-id 后切应用。  
_Avoid_: 重新引入双读兼容期；未授权硬切生产

**默认用户 wta**：新装/种子默认登录名；旧库用户名一律 → wta/WTA（LOG-017 / ADR-013）。  
_Avoid_: 文档仍写 ruoyi 管理员

**换前缀 wta-**：`ruoyi-*`→`wta-*`，不是裸名。  
_Avoid_: 目录 `system` 与 artifact `wta-system` 分裂

**实施门禁**：ChatGPT review → 本地文档迭代 → **CTO 书面授权** 后才可实施。  
_Avoid_: `CHANGES REQUIRED` 被当成 authorized；文档未收敛就建仓/改代码

> **LOG-018：** Nacos 为**一次性硬切（无双读）**；发版窗口人工迁配置。原「兼容期双读/退出门禁」表述以 ADR-006 为准废止。
