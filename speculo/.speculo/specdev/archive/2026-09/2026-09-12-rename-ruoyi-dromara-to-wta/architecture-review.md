---
artifact: architecture-review
change: 2026-09-12-rename-ruoyi-dromara-to-wta
status: draft
reviewed_at: 2026-09-12T11:01:00+08:00
reviewer: RVP·架构审（本地草稿）
verdict_suggestion: 有条件通过
---

# 架构审查：rename ruoyi/dromara → wta（合仓 + 坐标迁移门禁）

- **决策记录：** `<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/architecture-review.md</Path>`
- **审查准则：** `<Path>{roots.workflows}/specdev/R-review-architecture/review-rubric.md</Path>`

## 1. 审查压力与范围

- **触发目标：** Lead 要求对 change `2026-09-12-rename-ruoyi-dromara-to-wta` 做 R-review-architecture 本地审阅草稿；焦点为 ADR-001…011 / KEEP / W0b 依赖 / Nacos 退出门禁可验收性 / 换前缀 wta-X / 流程门禁 / AMBIGUOUS 是否阻塞 R。
- **审查入口：** 用户指定 SpecDev change 工件与证据（非 Git 热点产品代码扫描）。
- **相关行为或 Ticket：** 尚无正式 `ticket/*.md`；依赖 `goal-plan.md` 波次 W0/W0b/W1–W5、`tickets-map.md` outline、AC-001…007 草案。
- **不审查范围：** 不改产品代码；不提前设计新 interface；不改写已拍板 ADR 正文结论；不建/不 push 新仓；不访谈用户选择候选（本轮仅报告阶段）；不做实现级 rename 脚本设计。
- **成功标准：** 高置信结构性风险与门禁缺口可追踪；明确有无 code-judo 候选；给出裁决建议与是否可开 S-spec；残留 AMBIGUOUS 处置清晰。
- **热点依据：** 用户指定（Lead 审阅焦点）
- **结构性压力：** seam 泄漏（KEEP vs OWNED）、sequential orchestration（W0b→命名波次）、门禁可验收性缺口、文档 locality 漂移（CONTEXT/封面/outline 残留旧措辞）、thin wrapper 风险（无限期双读）

## 2. 当前结构地图

### 模块与接口

本 change 的「module」主要是**治理模块**而非业务代码模块：

| Module | Interface（调用者须知） | Depth 观察 |
|---|---|---|
| KEEP 规则引擎（ADR-003 + THIRD-PARTY-KEEP） | 哪些 `org.dromara.*` 字节级保留 | 深：小规则表覆盖大量误伤面 |
| PackageCoord 迁移（ADR-001/002/008） | 自有 `org.dromara`/`ruoyi-*` → `org.namewta`/`wta-*`；换前缀非裸名 | 深：目录+artifact+package+扫描一体 |
| W0b 合仓（ADR-010/011） | 旧三仓冻结；orphan 新 public monorepo | 深：一次合仓删除三仓协调复杂性 |
| Nacos 兼容期（ADR-006） | 双读 + **可验收退出** | 目前偏浅：退出度量未量化 |
| 实施门禁（ADR-004/010 + goal-plan） | ChatGPT→本地迭代→问 CTO→书面授权 | 深：串行门禁降低误实施半径 |

### 数据、控制与错误流及 seam

```text
SURVEY/KEEP 证据
  → ADR-001…011（已拍板）
    → goal-plan 波次/Gates
      → [G-R 本审查]
        → S-spec（量化 AMBIGUOUS）
          → T-tickets
            → [G-Doc ChatGPT] → [G-Auth] → W0b → W1…W5
```

关键 **seam**：

1. **OWNED vs KEEP seam** — `org.dromara`（无二级产品段）+ `ruoyi-*` vs `org.dromara.{sms4j,warm,easy-es,mica-mqtt}`；泄漏后果=编译/运行损坏。
2. **仓库形态 seam** — 旧 remote 只读备份 vs 新 monorepo 写面；泄漏后果=旧仓 force-push / 历史污染新仓。
3. **配置兼容 seam** — 旧 Nacos data-id vs 新 id；泄漏后果=无限期双读或硬切停机。

### 变化热点、locality 与测试表面

- 证据热点：~49 `ruoyi-*` 目录、~1648 自有 Java、sms4j~9 / warm~53 / easy-es~7 / mica~7（见 SURVEY）。
- 测试表面（实现后）：`rg` KEEP 回归、`./mvnw … package`、`pnpm architecture:check`、compose config；E2E 默认 not-required。
- locality：KEEP 规则应集中在 `evidence/THIRD-PARTY-KEEP.md` + Ticket 验收；不得散落到各脚本 ad-hoc。

### 拆分压力

- **接近或超过 1k lines 的文件：** 本轮未扫产品源做 1k 拆分；实现期 W2 若单 PR 吞全包树会产生 decomposition pressure——goal-plan 已要求 W2 子波（对照表→git mv→common→modules→admin）。
- **需要先删除还是先拆分的复杂性：** 优先删除「盲全局替换」与「无限期双读」；合仓复杂性集中到 W0b，不与命名波次缠绕。

## 3. 候选提案

> **总声明：** 本 change 主轴是重命名/合仓治理，**无高置信 code-judo 候选**（无可删除的产品代码浅层 module；删除测试不适用于尚未授权的实现面）。下列候选均为**结构性风险 / 门禁缺口**，用于 G-R 裁决与交给 S-spec，不提前设计 interface。

### AR-001: KEEP seam 与盲替换结构性阻塞

- **文件：** `<Path>{roots.state}/specdev/changes/{change}/evidence/THIRD-PARTY-KEEP.md</Path>`、`<Path>{roots.state}/specdev/changes/{change}/ADR.md</Path>`（ADR-003）、SURVEY §2.2/§3.2；实现触点（只读证据）`ruoyi-common-sms`、`ruoyi-notify`、`ruoyi-workflow`、`ruoyi-common-elasticsearch`、`ruoyi-common-mqtt`
- **结构类别：** structural blocker / wrong-layer logic（若把第三方坐标当自有品牌层处理）
- **问题：** 自有包装 module（如 `org.dromara.common.sms`）与第三方 SDK（`org.dromara.sms4j`）共享字符串前缀；浅层「全局 s/org.dromara/org.namewta/」会让 KEEP seam 泄漏到调用方，破坏编译与运行时。
- **代码 judo：** 无产品代码可删；治理上**删掉盲替换策略**，保留「规则引擎 + KEEP 回归」这一深层 interface。
- **删除复杂性：** 删除「一次性全仓字符串替换」分支；复杂性集中到 THIRD-PARTY-KEEP 检测规则与 AC-003。
- **删除测试：** 删掉 KEEP 规则后，复杂性在 N 个 Ticket/脚本中重现 → 规则 module 有价值；删掉盲替换后复杂性消失 → 盲替换是浅层透传灾难。
- **收益：** locality（误伤检测一处）、leverage（一规则护四产品）、depth（小清单驱动大迁移面）。
- **建议强度：** Strong
- **依赖类别：** in-process（规则与静态 `rg`）；第三方本身为 mock/ports 外的真实外部依赖但本期 KEEP 不换 adapter
- **ADR 冲突：** 无（与 ADR-003 一致）

#### 前后对比

- Before：shallow「改所有 org.dromara」interface，seam 泄漏到 sms4j/warm/easyes/mica。
- After：deep KEEP module；OWNED 与 KEEP 分类稳定；AC-003 为测试表面。

- **证据：** `CODE`/`EVIDENCE` THIRD-PARTY-KEEP；SURVEY 第三方计数；ADR-003
- **推荐：** 接受为 R→S 硬约束；S-spec 必须把 AC-003 与检测规则写成可执行验收
- **访谈状态：** unselected
- **用户结论：**
- **ADR 影响：** 无（强化 ADR-003，不重审）

### AR-002: W0b 合仓/orphan 必须先于命名波次（依赖倒置风险）

- **文件：** `goal-plan.md` §2 DAG、ADR-010、ADR-011、tickets-map LOG-011 增补主题
- **结构类别：** structural blocker / sequential orchestration
- **问题：** 若在旧三仓或未 orphan 的工作区先做 `ruoyi-*`→`wta-*` 目录/包迁移，再合仓，会把双品牌历史与路径债带进新仓，并增加旧 remote 误操作面；W0b 与 W1–W5 的依赖若倒置，locality 破裂。
- **代码 judo：** 无；编排上删除「旧仓内 rename」路径，只保留「授权后 W0b → 新 monorepo 内命名」。
- **删除复杂性：** 删除旧仓 filter-repo / 边迁边合 / submodule 指针同步等多路径 special case。
- **删除测试：** 删除 W0b 门禁后，合仓与命名复杂性在三仓并行蔓延 → W0b 有价值。
- **收益：** locality（写面仅新仓）、leverage（一次 orphan 清历史）、depth（冻结+摘取+orphan 小 interface）。
- **建议强度：** Strong
- **依赖类别：** ports & adapters（GitHub remote 为外部）；本地导出为 local-substitutable
- **ADR 冲突：** 无；与 ADR-010/011 一致。注意 ADR-005「聚合仓路径 KEEP」已部分 supersede——goal-plan 已收口，**不建议重开 ADR-005 全文**。

#### 前后对比

- Before：三仓 + submodule + 可能在旧仓 rename → shallow 多写面。
- After：旧仓冻结；单一 monorepo 写面；命名波次 locality 集中。

- **证据：** ADR-010/011；goal-plan W0b→W1…W5；`.gitmodules` 现状（SURVEY/CONTEXT）
- **推荐：** R 通过条件之一：S-spec/T 必须显式纳入 T-freeze-old-remotes / T-monorepo-layout / T-orphan-public（现仅 outline 增补，未进主表 T-01…T-08）
- **访谈状态：** unselected
- **用户结论：**
- **ADR 影响：** 无

### AR-003: ADR-006 Nacos 双读退出门禁可验收性不足

- **文件：** ADR-006；goal-plan §5.1 / Gate G-W5；spec 风险表（仍有旧「默认延期」措辞残留风险）；SURVEY §8 原 AMBIGUOUS#2
- **结构类别：** boundary drift / missed simplification（无限期双读 = 未删除的兼容 special case）
- **问题：** 双读方向已拍板，但**兼容期时长与退出度量未量化**；当前 interface 对运维/验收者偏浅——调用者仍须猜测「何时可下线旧 data-id」。无退出门禁会把 temporary adapter 变成永久泄漏 seam。
- **代码 judo：** 无代码；规格上删除「无限期双读」选项，强制可勾选退出清单。
- **删除复杂性：** 删除「双读永久保留」mode；退出后只认新 id。
- **删除测试：** 删除退出门禁 → 兼容复杂性永不消失；保留量化退出 → 复杂性有界。
- **收益：** depth（小退出清单驱动迁移完成）、locality（下线证据集中在 G-W5）、可测试性（对照表 + 告警零命中可断言）。
- **建议强度：** Strong（对 S-spec）；不阻塞 R 方向接受
- **依赖类别：** ports & adapters（Nacos 配置中心）
- **ADR 冲突：** 无（ADR-006 已要求规格量化；属下游未完成，非 ADR 矛盾）

#### 前后对比

- Before：shallow「双读一段时间」——时长与退出模糊。
- After：deep 兼容 module——对照表 + 时长 + 退出检查清单 + 禁止无限期双读。

- **证据：** ADR-006 Consequences；goal-plan ADR-006 收口与 AMBIGUOUS#5
- **推荐：** S-spec 必须冻结：对照表、时长、退出度量（例：新装只写新 id、旧 id 只读窗口结束、监控零命中旧 id）、失败恢复。**R 不重审双读决策。**
- **访谈状态：** unselected
- **用户结论：**
- **ADR 影响：** 无 / 交 S-spec 落实 ADR-006

### AR-004: ADR-008 换前缀 wta-X vs 残留「去前缀/裸名」文档漂移

- **文件：** ADR-008；CONTEXT「模块目录无 ruoyi- 前缀」；封面首次 CTO 补记「去掉 ruoyi- 前缀」；LOG-010 旧表述 vs LOG-011 修正；tickets-map T-03（正确写 →`wta-*`）
- **结构类别：** spaghetti growth / boundary drift（文档层）
- **问题：** 权威 ADR-008 已锁定 **换前缀 `wta-X`（非裸名）**，但 CONTEXT/封面旧段仍可能被读成「去前缀成 `system`」。文档 locality 破裂会在 T/I 阶段制造错误对照表。
- **代码 judo：** 无；删除过时「去前缀/裸名」叙述分支（由 S-spec/后续文档收敛，**本 R 不改 ADR/CONTEXT 正文**——风险记账）。
- **删除复杂性：** 删除「去前缀 vs 换前缀」双语义 special case。
- **删除测试：** 删掉歧义措辞后，目录命名规则只剩一条 → 复杂性消失。
- **收益：** locality（单一前缀规则）、防止错误目录 interface。
- **建议强度：** Worth exploring（文档收敛）；决策本身 Strong 已锁定
- **依赖类别：** in-process
- **ADR 冲突：** **无真实 ADR 冲突**；ADR-008 为准。CONTEXT/封面为过时投影，不值得重审 ADR-008。

#### 前后对比

- Before：ADR 说换前缀，CONTEXT/封面残留去前缀 → 双语义 shallow。
- After：全工件统一 `ruoyi-X`→`wta-X`；禁止裸名。

- **证据：** ADR-008；LOG-011；CONTEXT 旧句；delivery 封面两段补记
- **推荐：** S-spec 用权威措辞覆盖；可选另开文档清理（不改 ADR 结论）
- **访谈状态：** unselected
- **用户结论：**
- **ADR 影响：** 无

### AR-005: 流程门禁与未授权建仓（含外脑 blocked）

- **文件：** goal-plan Gates G-Doc/G-Auth；ADR-010；`.status.json` blockers；`external-brain/notes.md` / `reply.md`
- **结构类别：** structural blocker（对 I/建仓）；对 R→S 为外部流程依赖
- **问题：** 门禁 ChatGPT 6 Pro review → 本地迭代 → 再问 CTO 实施 已锁定；当前外脑上传 Auto-review **blocked**，G-Doc 未关闭。若跳过门禁建仓/改代码，会破坏「文档收敛后再动写面」的 depth。
- **代码 judo：** 无；删除「文档未收敛即实施」捷径。
- **删除复杂性：** 删除未授权 push/建仓/I-implement 路径。
- **删除测试：** 删除门禁后误实施半径扩散到旧仓与新仓。
- **收益：** 风险局部化；authorization matrix 保持全 not-authorized。
- **建议强度：** Strong（实施前）；**不阻塞本 R 草稿与后续 S-spec 文档工作**
- **依赖类别：** ports & adapters（ChatGPT / CTO 书面授权）
- **ADR 冲突：** 无

#### 前后对比

- Before：可随时建仓/rename → shallow 高风险写面。
- After：串行门禁；外脑 blocked 时本地文档可继续，但不得问实施/建仓（除非 CTO 豁免）。

- **证据：** `.status.json` blockers；external-brain notes；goal-plan Authorization Matrix
- **推荐：** R 通过后可开 S-spec；G-Doc 仍为实施前门禁
- **访谈状态：** unselected
- **用户结论：**
- **ADR 影响：** 无

### AR-006: 残留 AMBIGUOUS 与工件投影不一致（交 S-spec）

- **文件：** goal-plan §6 AMBIGUOUS 清单；spec.md §5 风险（仍写 Nacos「默认延期」、聚合路径 KEEP——与 ADR-006/011 过时）；SURVEY §8；封面 Open ambiguities（部分已拍板仍列出）
- **结构类别：** spaghetti growth（文档） / boundary drift
- **问题：** 计划层 AMBIGUOUS 已收口为可 S-spec 量化项（slug、布局、兼容期时长、旧库用户策略、上游 URL、注释口径、旧仓 README）。但 spec/封面/SURVEY 仍混有「未拍板」投影，降低 domain/locality。
- **代码 judo：** 无；删除过时「开放项」列表中的已关闭决策。
- **删除复杂性：** 删除已拍板项的重复 AMBIGUOUS 叙述。
- **删除测试：** 收敛后开放集变小且可验收。
- **收益：** S-spec 输入清晰；避免重开已锁 ADR。
- **建议强度：** Worth exploring
- **依赖类别：** in-process
- **ADR 冲突：** 无（投影滞后，非决策冲突）

#### 前后对比

- Before：已锁项与未锁项混在 AMBIGUOUS 列表。
- After：仅剩 S-spec 可冻结的量化/点名项。

- **证据：** goal-plan §6；spec §5；delivery 封面；LOG-010/011
- **推荐：** **不阻塞 R 通过**；S-spec 必须清理投影并冻结剩余项（或显式 backlog 且不挡已授权波次，对齐 AC-006）
- **访谈状态：** unselected
- **用户结论：**
- **ADR 影响：** 无

## 4. 最佳推荐

**无高置信 code-judo 候选。**

治理面上首先应守住的结构性结论（非访谈排序，而是 G-R 接受序）：

1. **AR-001 KEEP seam** 与 **AR-002 W0b 先后** 为硬约束（已与 ADR 一致）。
2. **AR-003** 将退出门禁量化交给 S-spec（不重开双读决策）。
3. **AR-004/AR-006** 为文档 locality 收敛，不改 ADR 结论。
4. **AR-005** 外脑 blocked 不挡 R→S，挡 G-Doc/实施。

首先探索（若用户进入访谈）：**AR-003**（退出门禁可验收形状）——因其直接决定 W5 是否可关闭；其次 **AR-002** 在 tickets 主表吸收 W0b。

## 5. Lead 焦点核对

| # | 焦点 | 结论 |
|---|---|---|
| 1 | ADR-001…011 与 KEEP | **一致可接受**；KEEP 四件套规则完整；ADR-005 部分 supersede 已在 ADR/goal-plan 标明，无需重审结论 |
| 2 | W0b vs 命名波次 | **依赖正确**（授权→W0b→W1…W5）；风险是 tickets 主表尚未吸收 W0b 票题 |
| 3 | ADR-006 退出门禁 | **方向通过；可验收性待 S-spec 量化** |
| 4 | ADR-008 换前缀 | **ADR 正确锁定 wta-X**；CONTEXT/封面残留「去前缀」为文档漂移 |
| 5 | 流程门禁 | **锁定有效**；未授权不建仓；ChatGPT blocked ≠ 可跳过 |
| 6 | AMBIGUOUS 是否阻塞 R | **不阻塞 R**；交 S-spec |

## 6. 裁决建议

### 裁决：`有条件通过`

**条件（须在 S-spec / 文档收敛中关闭，不要求本轮改 ADR 正文）：**

1. S-spec 冻结 Nacos 对照表、兼容期时长、**可勾选退出门禁**与失败恢复（落实 ADR-006）。
2. S-spec 冻结新仓 slug 候选点名与 monorepo 目录布局（落实 ADR-010/011 开放细节）。
3. S-spec（或随后文档票）收敛 CONTEXT/封面/spec 风险表中与 ADR-008/011/006 冲突的**过时投影**（不改 ADR 已拍板结论）。
4. T 之前：tickets-map 主表吸收 W0b 相关票题（freeze / layout / orphan / review-gate）。
5. 实施前仍须：ChatGPT 6 Pro review 闭环（或 CTO 书面豁免）→ 本地迭代 → 书面 `implementation_commit`；**本 R 不授权建仓或改产品代码。**

### 是否可开 S-spec

**可以。** 建议立即开 S-spec；R 本轮无强制用户访谈阻塞项。

### 关键风险清单

1. 盲替换破坏 sms4j/warm/easy-es/mica（KEEP seam）
2. 在旧仓或未 orphan 工作区做命名迁移（W0b 倒置）
3. Nacos 无限期双读或硬切无退出证据
4. 目录做成裸名而非 `wta-X`
5. 跳过 ChatGPT/CTO 门禁建仓或改代码
6. 文档过时投影导致 T/I 误读「去前缀 / 聚合路径 KEEP」
7. 外脑长期 blocked 却被误当成门禁豁免

## 7. 下一步

- 报告生成后：若需访谈，仅选一个候选（建议 AR-003）；本草稿默认不批量访谈。
- 主会话验收本报告后：可追加 `.status.json` `works_run`（本助手**未**改 status）。
- 下一 Work：**S-spec**（冻结 AC / AMBIGUOUS 量化 / monorepo 布局 / 退出门禁）。
- 达成共识的接受项进入 T-tickets；纯文档漂移不升格为产品代码 Ticket。

## 8. 字段完整性自检

| 候选 | files | 结构问题 | code-judo | deleted complexity | dependency class | strength | ADR conflict | interview state | user conclusion |
|---|---|---|---|---|---|---|---|---|---|
| AR-001 | ✓ | ✓ | ✓（治理删除盲替换） | ✓ | ✓ | Strong | 无 | unselected | （空，待用户） |
| AR-002 | ✓ | ✓ | ✓ | ✓ | ✓ | Strong | 无 | unselected | （空） |
| AR-003 | ✓ | ✓ | ✓ | ✓ | ✓ | Strong | 无 | unselected | （空） |
| AR-004 | ✓ | ✓ | ✓ | ✓ | ✓ | Worth exploring | 无 | unselected | （空） |
| AR-005 | ✓ | ✓ | ✓ | ✓ | ✓ | Strong | 无 | unselected | （空） |
| AR-006 | ✓ | ✓ | ✓ | ✓ | ✓ | Worth exploring | 无 | unselected | （空） |

**code-judo 总判：** 无高置信产品代码 code-judo 候选（已在 §3 总声明与 §4 写明）。
