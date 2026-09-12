# Change Architecture Decisions

> **Authority:** CTO written decisions > this ADR set > `spec.md` > `goal-plan.md` > `tickets-map.md`.  
> **Implementation / publication / legacy mutation:** all **false** until CTO sets the corresponding gate.  
> ChatGPT review `CHANGES REQUIRED` does **not** authorize implementation.

## ADR-001: 自有产品表面 ruoyi → wta

**Status:** accepted（调研基线；实现待授权）
**Source:** LOG-001 / LOG-002 / CTO intent 2026-09-12
**Supersedes:** none

### Context
本仓是基于上游 RuoYi-Vue-Plus 的 NAMEWTA 增强版。前端与 Docker 运行时已大量使用 `namewta` / `@namewta`，但后端模块目录、Maven `artifactId`、品牌文案、容器路径仍充斥 `ruoyi` / `RuoYi`，造成「对内已是 NAMEWTA、对外/对构建仍是 RuoYi」的双品牌。

### Decision
凡**项目自有**的 `ruoyi` / `RuoYi` / `RUOYI` 产品命名（模块目录、artifactId、自有配置键中的产品段、品牌标题、Skill 名中的产品前缀、容器内自有工作目录等）统一迁到 `wta` / `WTA`（前端 npm 范围继续 `@namewta`，与既有约定对齐，不强制改成 `@wta`）。

**Naming split（与 ADR-002 对照，必须分清）：**
1. **Name-type `ruoyi` → `wta`（本 ADR）：** 模块目录 / artifactId `ruoyi-X`→`wta-X`；默认用户名与 OWNED 种子字面量；OWNED Nacos 产品段 `ruoyi-`→`wta-`；容器路径 `/ruoyi`→`/wta`；品牌标题 RuoYi→WTA（OWNED）。
2. **Owned brand token `dromara` → `namewta` family（ADR-002 / LOG-020）：** Maven `groupId` / Java 基包 `org.dromara` → **`org.namewta`**（**SUPERSEDES** 先前 `org.wta`）。**不要**把自有 Java/Maven group 写成短 `org.wta`。
3. **Third-party `org.dromara.<product>` KEEP**（ADR-003）：sms4j / warm / easyes / mica.mqtt — 永不改成 `org.namewta.*`。

### Trade-off
全量一次性替换事故半径大；保留双品牌则文档与脚本持续分叉。选统一 `wta` 品牌，换取分波次改造成本。

### Consequences
实现必须按 MigrationWave 推进；任何脚本默认排除 THIRD_PARTY_KEEP。上游溯源叙述可保留「基于 RuoYi-Vue-Plus」，但不作为运行时坐标。**禁止**全库盲 `s/ruoyi/wta/`。

## ADR-002: 自有 org.dromara → org.namewta

**Status:** accepted（坐标选择；LOG-020 修订）
**Source:** LOG-003（历史） / **LOG-020 USER-DECISION [t93u]** / evidence SURVEY
**Supersedes:** 本 ADR 先前 Decision 中的 owned 目标 **`org.wta`**（LOG-003）；凡 SpecDev 正文仍写「自有 → org.wta」的 active 投影均以本修订为准废止

### Context
根 POM 与全部自有模块使用 `groupId=org.dromara`，Java 包为 `org.dromara.{common,system,...}`。早期曾在 `org.wta` 与 `com.wta` 间选择并短暂采纳 `org.wta`。CTO [t93u] 锁定：自有 **dromara 品牌 token** 对齐既有 `@namewta` / Docker `namewta-*`，采用 **namewta** family；短 `wta` 留给 ruoyi 名型（模块前缀等，见 ADR-001/008），**不**作为 Java/Maven group 段。

### Decision
自有 Maven `groupId` 与 Java 基包统一改为 **`org.namewta`**（例如 `org.namewta.common.core`、`org.namewta:wta-admin`）。不采用 `com.wta`；**不再采用** 先前拍板的 `org.wta` 作为 owned 目标。

**KEEP（critical，与 ADR-003 一致）：** `org.dromara.sms4j`、`org.dromara.warm`、`org.dromara.easyes` / easy-es、`org.dromara.mica.mqtt` / mica-mqtt — **MUST NOT** 变为 `org.namewta.*`。CTO「全部 dromara→namewta」解释为 **owned/first-party brand only**，禁止盲替换第三方坐标。

### Trade-off
`org.wta` 更短但与 `@namewta`/`namewta-*` 分裂；`org.namewta` 与既有商标对齐，group 更长。`org.namewta` 与第三方 `org.dromara.*` 仍共享 `org.` 前缀，**禁止**「替换所有 org.dromara」——必须按 ADR-003 + namespace ownership inventory。

### Consequences
`mapperPackage` / `typeAliasesPackage` / springdoc `packages-to-scan` / `AutoConfiguration.imports` 随包名同步到 `org.namewta.*`。artifactId 仍：`ruoyi-X` → `wta-X`（ADR-001/008，不变）。

## ADR-003: 第三方 dromara 生态 KEEP 清单与检测规则

**Status:** accepted
**Source:** LOG-004 / evidence/THIRD-PARTY-KEEP.md
**Supersedes:** none

### Context
Dromara 生态下多个独立开源组件以 `org.dromara.<product>` 发布。本仓已引入 sms4j、warm-flow、easy-es、mica-mqtt。CTO 明确：**不得**改第三方坐标与包名。

### Decision
硬 KEEP：`org.dromara.sms4j`、`org.dromara.warm`、`org.dromara.easy-es`（运行时 `org.dromara.easyes`）、`org.dromara.mica-mqtt`（运行时 `org.dromara.mica.mqtt`）。检测规则与易混淆对照见 `evidence/THIRD-PARTY-KEEP.md`。自有包装模块（如 `org.dromara.common.sms`）仍 RENAME，但其 import 的 SDK 包 KEEP。

### Trade-off
机械全局替换更快但必破坏编译。规则引擎 + 审阅更慢，换取可合并的正确性。

### Consequences
Tickets 必须含「KEEP 回归」验收：改后仍能解析 sms4j/warm/easyes/mica import；pom 中第三方 groupId 字节级不变。**全局 `ruoyi`/`org.dromara` zero-match 不是验收标准。**

## ADR-004: 分波次 rollout，禁止无闸门大爆炸

**Status:** accepted
**Source:** LOG-005
**Supersedes:** none

### Decision
按波次：文档收敛与 inventory →（授权后）准备 monorepo 树 → rename 波次 → residual → verify → publication readiness →（publication_authorized）orphan public push → legacy freeze transition。每波有可验证闸门；默认不 big-bang。

Work 路由：`P-goal-plan` → `R-review-architecture` → `S-spec` → `T-tickets` →（`implementation_authorized`）`I-implement` → `C-code-review`。

### Consequences
本 change 文档轮只完成 SpecDev；`.status.json` 中 implementation / publication / legacy mutation 授权均为 false。

## ADR-005: 明确不改或延期项（部分已被 010/011 取代）

**Status:** partially-superseded（见 ADR-010/011；LOG-011；LOG-013）
**Source:** LOG-006
**Supersedes:** none
**Superseded-in-part-by:** ADR-010（历史重置+新仓）、ADR-011（三仓合并与整体仓名）

### Decision — 仍有效（NON-NEGOTIABLE）

1. **不对旧 remote 做 filter-repo / force-push / 作为新主线继续开发** —— 见 ADR-010 freeze 定义。
2. **公开上游 URL 溯源语义可保留** —— README 可保留「基于 RuoYi-Vue-Plus」叙述；产品标题改为 WTA/NAMEWTA。是否改 `repository.url` / pom `<url>` 仍待 CTO（AMBIGUOUS）。
3. **第三方依赖版本升级** —— 非本 change 目标。
4. **CTO 机器 / Cloud Agent** —— 本 change 执行禁区。

### Decision — 已作废 / 禁止再引用为 active topology

下列历史表述 **不再是有效决策**；任何 plan/ticket/CONTEXT 若复述之，以 ADR-010/011 为准：

- ~~「不 rewrite 历史」作为全局策略~~ → **仅旧仓**不 rewrite；**新 public 仓**采用 orphan 干净历史（ADR-010）。
- ~~「聚合仓目录名 `ruoyi-vue-plus-docs` 本期 KEEP」~~ → **废止**。交付形态为三仓合并后的新 monorepo + 新仓名（ADR-011）。本地工作区路径可暂时仍叫 `ruoyi-vue-plus-docs`，但这不是目标仓库拓扑。
- ~~「新远程仓名仅 AMBIGUOUS / 可不建」~~ → **必须**新建 public 仓（精确 slug 待 CTO 点名）。

### Consequences
实现文档不得自行重新解释 repository topology。冲突时 **Supersedes 指向 ADR-010/011**。

## ADR-006: Nacos data-id 重命名 — 一次性硬切（无双读）

**Status:** accepted（LOG-018 修正策略）  
**Source:** LOG-010 / LOG-017 / USER-DECISION widget t89s3  
**Supersedes:** open ambiguity #1；废止「兼容期双读」表述

### Context
现网/本地存在 `data-id: ruoyi-namewta.yml` 一类标识。只改代码不改 data-id 会留下品牌分裂。CTO 已理解「兼容期双读」含义后，明确选择**不要双读**。

### Decision
将自有 Nacos data-id / group 中的 `ruoyi` 收敛为 `wta`（对照表在 S-spec）。**无双读兼容期**：在约定**发版窗口**内人工把 Nacos 配置从旧 data-id **复制/迁移到新 data-id**，再切换应用只认新 id。窗口外不要求运行时同时读新旧 id。

### Trade-off
硬切要求发版纪律与人工核对，换取实现更简单、无长期双读债务。

### Consequences
tickets 须含：发版窗口 checklist、旧→新 data-id 对照、迁移操作步骤、回滚（切回旧 id+旧构建）、失败即停窗口。禁止默默上线只改代码不迁 Nacos 内容。
## ADR-007: 默认用户名 ruoyi → wta

**Status:** accepted  
**Source:** LOG-010 / USER-DECISION:2026-09-12  
**Supersedes:** open ambiguity #2

### Decision
默认用户名改为 `wta`；迁移说明须覆盖已有库中旧用户名（自动 rename **或** 仅新装生效 —— 待 CTO/S-spec 二选一写死）。

### Consequences
登录文档、初始化 SQL/脚本、E2E 夹具同步；密码策略不在本 ADR 范围。

## ADR-008: 子模块目录换前缀 ruoyi- → wta-

**Status:** accepted（LOG-011 修正：换前缀，非去前缀）  
**Source:** LOG-010 / LOG-011 / USER-DECISION:2026-09-12 [t81u]  
**Supersedes:** open ambiguity #3；修正「去掉前缀」表述

### Decision
自有子模块**物理目录**与 artifactId：`ruoyi-X` → **`wta-X`**（小写）。例：`ruoyi-system`→`wta-system`。不是删成无前缀的 `system`。整体仓库形态见 ADR-011（三仓合并 + 新仓名）。

### Consequences
对照表驱动搬迁；CI/`include`/Docker context 全量更新；在**新 monorepo 准备树**内完成（旧仓不动）。

## ADR-009: 种子与示例字面量中的自有 ruoyi 一并改

**Status:** accepted  
**Source:** LOG-010 / USER-DECISION:2026-09-12  
**Supersedes:** open ambiguity #4

### Decision
凡属**自有品牌/租户/bucket/示例账号**的 `ruoyi` 字面量一并改为 `wta`（或 `wta-` 前缀规则在 spec 统一）。上游产品名（SMS4J 等）与第三方坐标 KEEP。

### Consequences
SQL seed、YAML 示例、文档截图文案纳入 tickets；改前应用 ownership inventory，禁止盲替换。

## ADR-010: 原仓冻结 + 新 public 仓 orphan 重置历史

**Status:** accepted — **FINAL for repository history / freeze / sequencing**  
**Source:** LOG-011 / USER-DECISION:2026-09-12 [t81u] / ChatGPT P0-01/P0-04  
**Supersedes:** ADR-005「不 rewrite 历史（全局）」「可不建新远程」中与本策略冲突的全部部分；任何 goal-plan/CONTEXT 旧「聚合仓 KEEP 且不 orphan」叙述

### Context
旧三仓带上游与双品牌历史；若在原 remote 上 filter-repo，会破坏协作者 clone 与溯源备份。CTO 要求：原仓先不动；内容摘出后以全新 orphan 历史进入 public monorepo。

### Decision

#### 1) Freeze definition（旧三仓）

对下列 remote 进入 **freeze**（默认主线语义）：

- `https://github.com/NAMEWTA/ruoyi-vue-plus-namewta.git`
- `https://github.com/NAMEWTA/plus-ui-namewta.git`
- `https://github.com/NAMEWTA/ruoyi-vue-plus-docs.git`

**Freeze means (intent):**

| 动作 | Freeze 下默认 |
|---|---|
| 新功能 / rename 提交作为 default branch 主线 | **禁止** |
| force-push / history rewrite / filter-repo | **禁止** |
| 删除 remote / 无人值守改保护规则 | **禁止**（除非 `legacy_repo_mutation_authorized=true`） |
| 只读 clone、备份、审计 | 允许 |
| 日后 README 指向新仓 / archive / branch protection 落地 | 仅在 `legacy_repo_mutation_authorized=true` 后按 T15 |

Branch protection intent：default branch 视为只读备份；变更须显式授权票，不得被「实施 rename」顺带执行。

#### 2) Orphan / clean history（新仓）

- 在干净工作区摘取前后端与所需文档/发布物（排除 `node_modules`、`target`、`.git` 对象、运行缓存等）。
- 以 **orphan / 单一初始提交（或等价无祖先历史）** 建立新历史。
- **不得**把旧三仓 Git 对象/祖先带进新仓。

#### 3) Publication sequencing + auth boundary

```text
baseline + inventory + (implementation_authorized) staged rename
  → residual + verify
  → publication readiness gate
  → (public_repo_publication_authorized) orphan public push
  → (legacy_repo_mutation_authorized) legacy freeze transition
```

**Hard rule:** **No public push before publication gate.**  
Publication gate 最低覆盖：source baseline、secret scan、forbidden-file check、third-party KEEP verify、license attribution、rename residual classification、explicit authorization 记录。

Gate fields（默认全 false）：

- `implementation_authorized`
- `public_repo_publication_authorized`
- `legacy_repo_mutation_authorized`

#### 4) Orphan acceptance criteria

| ID | Criterion |
|---|---|
| ORPH-01 | 新仓 `git rev-list --max-parents=0 HEAD` 仅反映 orphan 根，无旧三仓 commit SHA 作为祖先 |
| ORPH-02 | `git log` 不含旧仓业务历史；blame 不依赖旧三仓 |
| ORPH-03 | 无 `.git` 子目录从旧仓被拷入工作树；无 `node_modules`/`target`/运行缓存入仓 |
| ORPH-04 | 首次 public push 仅在 publication gate + `public_repo_publication_authorized=true` 之后 |
| ORPH-05 | 旧三仓 HEAD 历史在 freeze 语义下未被 rewrite/force-push |

### Trade-off
新仓无连续 blame；换取干净公开面。旧仓保留作只读备份。

### Consequences
未授权不得创建/push 新仓；不得在旧仓执行 freeze transition 以外的 mutation。Tickets：导出清单、排除规则、orphan 步骤、publication gate、旧仓冻结确认。

## ADR-011: 前端 + 后端 + 副仓合并为单一 monorepo 并改整体仓名

**Status:** accepted — **FINAL for target topology**  
**Source:** LOG-011 / USER-DECISION:2026-09-12 [t81u] / ChatGPT P0-01  
**Supersedes:** ADR-005「聚合仓目录名 KEEP」及一切「继续以三仓+submodule 为交付主线」的旧叙述；关闭「是否新远程」方向性开放项

### Context
当前形态：聚合仓 `ruoyi-vue-plus-docs` + submodule 后端 `ruoyi-vue-plus-namewta` + submodule 前端 `frontend`。三仓分裂增加坐标迁移与发布成本。

### Decision
将**前端、后端、副/聚合仓**合并为**同一个仓库**（monorepo），并更改整体仓库名（与 `wta` / NAMEWTA 品牌对齐；精确 GitHub slug **待 CTO 最终点名**，候选 `NAMEWTA/wta` 或 `NAMEWTA/namewta`）。合并后不再以 git submodule 作为默认交付形态；目录布局（如 `backend/`、`frontend/`、`docs/`、`speculo/`）在 S-spec / inventory 定稿前不得假装已冻结。

### Trade-off
单仓更大、CI 需路径过滤；换取一次 rename + 发布 + 文档同源。

### Consequences
`.gitmodules` 策略作废于新仓；goal-plan 合仓/准备树在 rename 波次之前（授权后）；禁止 implementation 文档把「本地仍叫 ruoyi-vue-plus-docs」重新解释成「目标拓扑 KEEP」。

## ADR-012: 公开仓名 WTA-plus + 布局镜像 docs + 清理旧平台残留

**Status:** accepted  
**Source:** LOG-017 / USER [t89u]  
**Supersedes:** open slug/layout items

### Decision
1. 新 public monorepo 名为 **`WTA-plus`**（远程建议 `NAMEWTA/WTA-plus`）。
2. **目录布局**对齐当前 `ruoyi-vue-plus-docs`（聚合仓内前后端子树 + `docs` / `speculo` / `release-artifacts` / `scripts` 等），在新仓内完成 `ruoyi-`→`wta-` 换前缀与去 submodule 默认交付（内容合入，不再依赖旧 remote）。
3. 新仓 **必须清理** `.gitee` 及同类旧托管/CI 残留；禁止把旧平台钩子带进 public。
4. **上游不需要**：交付不以 upstream URL/remote 为依赖；`repository.url` / pom `<url>` 等改为自有或移除上游指向（历史“基于 RuoYi”一句话可选极简，非必须）。

### Consequences
Publication checklist 含 `.gitee` 扫描；tickets 含布局对照表（旧 docs 树 → `WTA-plus` 树）。

## ADR-013: 旧库默认用户名一律迁移为 wta

**Status:** accepted  
**Source:** LOG-017 / USER [t89u]  
**Supersedes:** ADR-007「旧库策略未定」部分

### Decision
不仅新装种子：既有数据库中的默认/演示用户名 **`ruoyi` → `wta`**（品牌书面可写 WTA）。须有迁移 SQL/脚本与回滚说明；密码策略仍不在本 ADR。

### Consequences
T-tickets 含 DB migrate 票；E2E/文档登录账号同步。

## ADR-006 补记（LOG-017/018）：策略已选 — 硬切

**Status:** closed by LOG-018 / t89s3  
CTO 选择 **B. 一次性硬切**（无双读）。见上方 ADR-006 正文。
