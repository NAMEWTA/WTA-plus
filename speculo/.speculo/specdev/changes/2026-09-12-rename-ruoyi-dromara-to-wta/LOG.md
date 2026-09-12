# 设计日志

## LOG-001 — 2026-09-12T10:29:50+08:00 — CTO 发起 rename 纲领并限定 SpecDev 落盘
- **设计树节点：** 不适用
- **轮次与依赖：** round 0 / 无
- **状态：** confirmed
- **问题：** 是否现在就做全仓代码重命名，还是先落 SpecDev 并调研分类
- **事实与来源：** 用户意图：自有 `ruoyi`→`wta`；自有 `dromara` 包/group→`wta`；第三方（sms4j 等）KEEP；仅在 `/workspace/vp-dev/ruoyi-vue-plus-docs` 写工件；禁止 Cloud Agent、禁止 CTO 机器、禁止本轮实现
- **选项：** A 立即改代码 / B 先 SpecDev+调研 / C 只改文档品牌
- **推荐：** B
- **结论：** 新建 change `2026-09-12-rename-ruoyi-dromara-to-wta`，phase=draft/speccing；`implementation_commit=not-authorized`
- **原因：** 触达面含 ~1648 Java 文件与多类第三方 `org.dromara.*`，无分类门禁必误伤
- **影响工件：** .status / source / evidence
- **约束或不变量：** 本轮零产品代码 rename
- **后续：** 仓库调研写入 SURVEY
- **替代/被替代：** 无

## LOG-002 — 2026-09-12T10:35:00+08:00 — 调研范围与排除项
- **设计树节点：** 不适用
- **轮次与依赖：** round 0 / LOG-001
- **状态：** confirmed
- **问题：** 扫描哪些树、排除哪些噪声
- **事实与来源：** 仓库含 `ruoyi-vue-plus-namewta`、`plus-ui-namewta`、`docs`、`release-artifacts`、`speculo`、`.agents`
- **结论：** 排除 `node_modules`、`.git`、`target`、`dist`、`pnpm-lock.yaml`、`.flattened-pom.xml`；扫描 Maven/Java/前端/docker/docs/skills
- **影响工件：** evidence/SURVEY.md、source.md
- **后续：** 分类 OWNED / KEEP / AMBIGUOUS

## LOG-003 — 2026-09-12T10:40:00+08:00 — 自有坐标与包目标定为 org.wta
> **SUPERSEDED by LOG-020 / ADR-002 (rev):** owned target is now **`org.namewta`**, not `org.wta`. Historical decision text retained below for audit.
- **设计树节点：** D-group
- **轮次与依赖：** round 1 / LOG-002
- **状态：** superseded（见 LOG-020）
- **问题：** `org.dromara` 自有部分改为 `org.wta` 还是 `com.wta`
- **事实与来源：** root pom `groupId=org.dromara`；全仓自有包 `org.dromara.*`；无 `com.wta` 先例；前端已是 `@namewta`
- **选项：** A `org.wta` / B `com.wta` / C 保持 org.dromara 只改 artifact
- **推荐：** A
- **结论：** ADR-002 采纳 `org.wta` + artifact `wta-*`
- **原因：** 与现有 `org.*` 扫描配置同构；短 group；避免引入第二套公司域名假设
- **影响工件：** ADR-002、spec、goal-plan
- **约束或不变量：** 禁止盲替换所有 `org.dromara` 字符串

## LOG-004 — 2026-09-12T10:45:00+08:00 — 第三方 KEEP 清单（sms4j 等）
- **设计树节点：** D-keep
- **轮次与依赖：** round 1 / LOG-002
- **状态：** confirmed
- **问题：** 哪些 `org.dromara.*` 是引入产品而非本仓模块
- **事实与来源：**
  - `org.dromara.sms4j:sms4j-spring-boot-starter`（9 文件触达；真实 import 于 common-sms / notify / demo）
  - `org.dromara.warm:warm-flow-*`（~53 文件）
  - `org.dromara.easy-es:easy-es-boot-starter`（运行时 `org.dromara.easyes`，~7）
  - `org.dromara.mica-mqtt:mica-mqtt-*`（运行时 `org.dromara.mica.mqtt`，~7）
  - 规则：`groupId=org.dromara.<product>` → KEEP；`groupId=org.dromara` + `ruoyi-*` → OWNED
- **结论：** ADR-003 + `evidence/THIRD-PARTY-KEEP.md`
- **影响工件：** ADR、tickets 验收、实现禁区
- **后续：** 注释类 AMBIGUOUS 单列

## LOG-005 — 2026-09-12T10:50:00+08:00 — 调研计数快照
- **设计树节点：** 不适用
- **轮次与依赖：** round 0 / LOG-002
- **状态：** confirmed
- **事实与来源：** 见 SURVEY §1
  - `ruoyi-*` 目录 ~49；含 `ruoyi-` artifact 的 pom ~49
  - 自有 Java under `org/dromara` ~1648
  - 内容命中 `org.dromara` ~1788；`ruoyi` ~277
  - 前端 `@namewta` 已铺开；admin 标题仍 RuoYi-Vue-Plus；根 package name 仍 `ruoyi-vue-plus`
  - Docker 服务已 `namewta-*`；容器路径仍 `/ruoyi/...`；build context 仍 `images/ruoyi-admin`
- **结论：** 后端坐标/包是主成本；前端与 compose 服务名部分已完成
- **影响工件：** SURVEY、spec 风险、tickets-map 波次

## LOG-006 — 2026-09-12T10:55:00+08:00 — 聚合仓路径 KEEP；分波次；AMBIGUOUS 上报 CTO
- **设计树节点：** D-path / D-rollout
- **轮次与依赖：** round 1 / LOG-005
- **状态：** confirmed（路径策略）/ open（AMBIGUOUS 项）
- **问题：** 是否重命名 `ruoyi-vue-plus-docs` 目录；如何 rollout
- **结论：**
  - ADR-005：聚合仓路径 **建议 KEEP**；不 rewrite git 历史；不升级第三方版本
  - ADR-004：W0 本变更落盘 → … → I-implement 后置
  - Open for CTO：Nacos `ruoyi-namewta.yml`、用户名 `ruoyi`、子模块目录名、种子 bucket `ruoyi-*`、新 remote 名、前端 repository.url
- **影响工件：** ADR-004/005、tickets-map、cover note
- **后续：** R-review-architecture / S-spec 精化；等待 CTO 对 AMBIGUOUS 答复后再开 T 细票与 I-implement

## LOG-007 — 2026-09-12T11:00:00+08:00 — SpecDev 工件集齐声明
- **状态：** confirmed
- **结论：** 已写入 `.status.json`、LOG、ADR、CONTEXT、spec、goal-plan、source、tickets-map、evidence/SURVEY、evidence/THIRD-PARTY-KEEP；封面 `/workspace/delivery/rvp-change-2026-09-12-rename-wta.md`。**明确：I-implement 为后续 Work，本轮仅 SpecDev persistence。**
- **影响工件：** 全 change 目录

## LOG-010 — 2026-09-12T10:36:09+08:00 — CTO 当面拍板四项歧义
- **设计树节点：** D-nacos / D-username / D-moduledir / D-seed
- **轮次与依赖：** round 2 / LOG-003–009
- **状态：** confirmed
- **问题：** Nacos data-id、默认用户名、子模块目录前缀、种子字面量是否纳入 OWNED_RENAME
- **事实与来源：** USER-DECISION:2026-09-12 widget t78s6 多选确认
- **结论：**
  1. **Nacos data-id**：要改（如 `ruoyi-namewta.yml`→`wta-*.yml` 等），**必须给兼容期**（双读/别名迁移窗口，时长在 S-spec 定）
  2. **默认用户名**：`ruoyi`→`wta`
  3. **子模块目录**：去掉 `ruoyi-` 前缀（与 artifact `wta-*` 对齐）
  4. **种子字面量**：一并改（含 OSS bucket 等示例中的 `ruoyi` 字面量，按 OWNED 规则；第三方产品名仍 KEEP）
- **原因：** CTO 明确全量品牌/标识收敛，同时要求配置中心平滑过渡
- **影响工件：** ADR-006+、CONTEXT、spec、goal-plan、tickets-map
- **约束或不变量：** sms4j/warm-flow/easy-es/mica-mqtt 仍 KEEP；兼容期结束条件须可验收
- **后续：** 规划/规格把兼容期与目录搬迁波次写进 goal-plan / tickets
- **替代/被替代：** 覆盖此前「可延后」的开放歧义清单


## LOG-011 — 2026-09-12T10:39:56+08:00 — CTO 当面拍板：换前缀 wta- + 三仓合并进全新 public 仓并重置历史
- **设计树节点：** D-prefix / D-monorepo / D-history / D-public
- **轮次与依赖：** round 3 / LOG-010 / ADR-005–009
- **状态：** confirmed（文档层）；I-implement 仍未授权
- **问题：** 子模块前缀是去还是换；原仓与历史如何处理；是否合并三仓
- **事实与来源：** USER-DECISION:2026-09-12 [t81u]
- **结论：**
  1. **换前缀**：`ruoyi-` → 小写 **`wta-`**（不是单纯去前缀）。例：`ruoyi-system`→`wta-system`。
  2. **原有仓库先不动**：`NAMEWTA/ruoyi-vue-plus-namewta`、`NAMEWTA/plus-ui-namewta`、`NAMEWTA/ruoyi-vue-plus-docs` 现网 remote **冻结保留**，不在本阶段改写或 force-push。
  3. **三仓合一**：将当前**前端**（`plus-ui-namewta`）、**后端**（`ruoyi-vue-plus-namewta`）、**副/聚合仓**（`ruoyi-vue-plus-docs` 内文档/SpecDev/release 等）合并为**同一 monorepo**，并更改整体仓库名。
  4. **摘取 + 清空旧内容 + 重置历史**：从三源摘出前后端与所需文档内容；**删除**合并工作区中的旧历史/旧布局残留；以**全新 orphan/单根提交**重置整个历史；推送到一个**全新的 public** GitHub 仓库。
  5. **流程门禁（本轮文档后）：** ① 整包 change + zip（无运行缓存）上传 ChatGPT 6 Pro 全面 review → ② 按 review 本地迭代到完善 → ③ **再问 CTO 是否实施**（implementation_commit 仍须书面授权）。
- **原因：** 品牌坐标与仓库形态一次对齐；旧仓可作只读备份；新仓干净公开、无上游历史包袱。
- **影响工件：** ADR-005（部分 supersede）、ADR-008（修正）、ADR-010/011、CONTEXT、spec、goal-plan、tickets-map、封面
- **约束或不变量：** KEEP 第三方；不触 CTO 机器；不默认 Cloud Agent；未授权不改产品代码、不建/不 push 新仓
- **开放细节（可在 S-spec 定，不阻塞文档迭代）：** 新 public 仓精确 slug（建议 `NAMEWTA/wta` 或 `NAMEWTA/namewta`，待 CTO 最终点名）；monorepo 内前后端目录树布局；旧仓 archive/README 指向策略
- **后续：** 更新归口 → pack → ChatGPT review → 本地迭代 → 问实施


## LOG-012 — 2026-09-12T10:46:30+08:00 — P-goal-plan 本地定稿；外脑上传 Auto-review 阻断
- **设计树节点：** D-plan
- **轮次与依赖：** round 4 / LOG-011 / ADR-001…011
- **状态：** confirmed（计划文档层）；I-implement 仍未授权
- **问题：** 秘书草案 goal-plan 硬伤与 ADR-006…011 / W0b 是否收口；外脑是否可用
- **事实与来源：** RVP·规划执行面审定；ChatGPT 上传被 Auto-review blocked；validate --stage goal-plan 10 errors（无票等，未造假票）
- **结论：**
  1. 修订 `<Path>{roots.state}/specdev/changes/{change}/goal-plan.md</Path>`：lead=`rvp-lead:1c44f1c3-a61e-4d39-a7a7-f2d21ce5fb00`；draft + ready_for_execution=false；Skill Gate 预告；Authorization Matrix 全 not-authorized；False completion；AMBIGUOUS；Resume/Progress；W0–W5 + **W0b 合仓**；ADR-006…011 计划层收口；不再写聚合仓路径本期 KEEP。
  2. 外脑：`external-brain/notes.md` + `reply.md` 记 blocked；本地定稿先行；门禁仍须 ChatGPT review→本地迭代→再问实施。
  3. `.status.json`：`works_run` 加入 `specdev/P-goal-plan`；`current_work` 清空；leadership 规范为 rvp-lead；**未改** execution_authorization。
  4. **下一 Work：R-review-architecture 可开。**
- **影响工件：** goal-plan、.status、external-brain、temp 便签、本 LOG
- **约束或不变量：** 零产品代码；无 ticket/*.md；不自启 I/Cloud Agent
- **后续：** R-review → S-spec →（补外脑或 CTO 豁免）→ T-tickets → 问实施授权

## LOG-013 — 2026-09-12T11:12:00+08:00 — ChatGPT 6 Pro 全面 review 入库（BLOCKED / P0）
- **设计树节点：** D-review-gate
- **轮次与依赖：** round 5 / LOG-012 / R-review / S-spec in flight
- **状态：** accepted（review 结论入库）；implementation **仍未授权**
- **类型：** decision-ingest（外脑） / proposal→accepted for local iteration scope
- **问题：** 实施前文档合同是否足以 deterministic implementation
- **事实与来源：**
  - Ingest: `temp/chatgpt-ingest/20260912-rename-wta-review/reply.md` + `meta.md`
  - Promoted: `external-brain/reply.md` + `external-brain/meta.md`
  - Zip: `temp/chatgpt-packs/20260912T024033Z-P-goal-plan-review.zip`
  - SHA-256: `2a8c4f5205d27a16a43ca4f7484a130fd5a3c4a77ba03ca12a82b07d86f305a6`
  - Model: GPT-6 Pro；chat_url 见 meta
- **结论：**
  1. Review result = **CHANGES REQUIRED / BLOCKED**；documentation_gate + implementation_gate **BLOCKED**
  2. 方向性硬约束（owned rename、KEEP、freeze、orphan public monorepo）**继续 LOCKED**，不重开
  3. P0-01…P0-08 全部接受为**本地文档迭代必做**；P1 高价值项一并纳入
  4. **CHANGES REQUIRED ≠ authorized**；不得进入 I-implement / 建仓 / 旧仓 mutation
- **影响工件：** external-brain/*、.status、本 LOG；触发 LOG-014 文档关闭迭代
- **约束或不变量：** 零产品代码；不 push；不改 CTO 机器
- **后续：** 本地关闭 P0 → 文档一致性复核 → 再问 CTO 授权
- **替代/被替代：** 取代「外脑仍完全未通」的叙述（本 ingest 已有完整 reply）；早先 Auto-review 阻断上传的历史仍保留在旧 notes

## LOG-014 — 2026-09-12T11:20:00+08:00 — 本地关闭 ChatGPT P0-01…P0-08（及关键 P1）文档迭代
- **设计树节点：** D-spec-contract
- **轮次与依赖：** round 6 / LOG-013
- **状态：** confirmed（文档层 iterated-p0-closed）；三门授权闸仍 false
- **类型：** accepted（local doc iteration）
- **问题：** 如何在不实施的前提下把 review 的实施合同缺口补到可复核
- **事实与来源：** ChatGPT P0/P1 清单；CTO locked decisions；SOURCE-BASELINE 实测 SHA
- **结论（P0 逐条）：**
  1. **P0-01** ADR-010/011 Supersedes 强化；ADR-005 冲突拓扑作废清单；CONTEXT 去掉 KEEP 聚合路径旧拓扑
  2. **P0-02** goal-plan / MANIFEST-CHANGE / .status 明确 implementation / publication / legacy mutation 授权闸
  3. **P0-03** 拆分 documentation_status / review_status / implementation_*（禁止单一 READY）
  4. **P0-04** 安全 publication 顺序 +「No public push before publication gate」
  5. **P0-05** Rename Classification MUST KEEP/RENAME/REVIEW；zero-match 非 AC
  6. **P0-06** namespace ownership inventory 实施前必做（T02）
  7. **P0-07** Nacos/runtime naming contract + 对照表占位
  8. **P0-08** `evidence/SOURCE-BASELINE.md`（三仓 remote/SHA/dirty + zip sha256）
- **亦完成 P1：** freeze 定义、orphan AC、权威优先级、T00–T15、REVIEW-BRIEF 确定性审题、glossary、LOG 状态区分
- **未做：** 产品代码 rename、建仓 push、旧 remote mutation、翻转任何 `*_authorized=true`
- **影响工件：** ADR/CONTEXT/spec/goal-plan/tickets-map/MANIFEST-CHANGE/REVIEW-BRIEF/.status/SOURCE-BASELINE/external-brain/notes/封面
- **后续：** 文档一致性复核 → CTO 点名 slug 等 AMBIGUOUS → 书面授权后再 T04+

## LOG-015 — 2026-09-12T11:18:35+08:00 — SURVEY NIT scrub（Lead 一致性复核）
- **状态：** done
- **事实：** `evidence/SURVEY.md` §6 L133-134 与 §8 部分 AMBIGUOUS 加 SUPERSEDED 横幅，指向 ADR-008/010/011；未改拍板 ADR 正文。
- **后续：** 回 Lead 路径供复核收口

## LOG-016 — 2026-09-12T11:22:00+08:00 — S-spec 本地定稿（消化 R 条件；不豁免实施）
- **设计树节点：** D-spec-contract
- **轮次与依赖：** round 8 / LOG-014 + architecture-review R 条件 + consistency-pass-with-nits + LOG-015 SURVEY NIT
- **状态：** confirmed（spec `ready_for_tickets=true` / `status=ready`）；三门授权闸仍 false
- **类型：** accepted（local S-spec finalize）
- **结论：**
  1. 量化 ADR-006：对照表形状 + 默认 14 自然日（UTC+8）+ 退出勾选 a–e；AC-006/AC-Nacos-Exit
  2. 冻结 DEC-SLUG=`NAMEWTA/wta`（候选 namewta）；DEC-LAYOUT 顶层四目录；去 submodule
  3. AMBIGUOUS 处置表：锁定 / 待 CTO / OOS；`### 未决问题` = 无。
  4. AC-001…AC-012 可勾选表 + 家族别名；补强 AC-W0b / Nacos-Exit / Gate；P0-01…08 覆盖表
  5. tickets-map 吸收 W0b（T00/T04/T04b/T07/T14）；铁律授权→W0b→rename
  6. CONTEXT/封面收敛「去前缀 / 聚合 KEEP / Nacos 延期」投影
  7. ChatGPT S 轮 Auto-review blocked — 本地定稿不豁免门禁
- **未做：** 产品代码、ticket/*.md、建仓 push、改 ADR 决策句、翻转 *_authorized
- **影响工件：** spec.md、tickets-map.md、CONTEXT.md、.status.json、delivery 封面、temp/team/spec 便签、本 LOG
- **后续：** T-tickets → CTO 点名剩余 AMBIGUOUS + written auth → 仅此后 T04+/I-implement


## LOG-016 — 2026-09-12T11:34:31+08:00 — CTO 当面：先不实施
- **状态：** confirmed
- **来源：** USER-DECISION widget t84s2
- **结论：** **不授权** `implementation_commit`；等一致性复核与剩余拍板后再问。
- **三道门保持 false：** implementation_authorized / public_repo_publication_authorized / legacy_repo_mutation_authorized
- **约束：** 不建/不 push 新仓；不改旧仓；不改产品代码
- **剩余拍板（待 CTO）：** 公开仓 slug；monorepo 布局；Nacos 兼容期时长/退出指标；旧库用户名迁移策略；上游 repository.url 政策

## LOG-017 — 2026-09-12T11:37:09+08:00 — CTO 当面拍板剩余项 + 询 Nacos 兼容期含义
- **状态：** partial-confirmed（Nacos 时长仍待选）
- **来源：** USER [t89u]
- **结论：**
  1. **公开仓名：** `WTA-plus`（组织默认 `NAMEWTA/WTA-plus`；若 GitHub 大小写敏感按 org 惯例）
  2. **目录布局：** 参考当前 `ruoyi-vue-plus-docs` 聚合布局（前端/后端子树 + docs/speculo/release-artifacts 等同源结构）
  3. **清理：** 新仓必须清掉 `.gitee` 及同类旧平台残留（历史 CI/钩子/镜像配置等按清单扫）
  4. **上游：** **不需要**——自有仓不再保留上游 remote/URL 作为交付依赖（溯源叙述可极简一句或不写；pom/`repository.url` 改自有或删除上游指向）
  5. **旧库用户名：** 一律改为 **WTA/wta**（登录标识技术上用 `wta`，与 ADR-007 对齐；展示名可用 WTA）——含已有库迁移，不只新装
  6. **Nacos 兼容期：** CTO 询问含义；时长/是否硬切 **尚未拍板**
- **影响：** ADR-011 补仓名；ADR-005/010 上游策略；ADR-007 旧库迁移=改；新 ADR 或补记清理清单；goal-plan 布局=docs 镜像
- **后续：** 向 CTO 解释兼容期并用选项收口


## LOG-018 — 2026-09-12T11:37:58+08:00 — CTO 拍板 Nacos：一次性硬切
- **状态：** confirmed
- **来源：** USER-DECISION widget t89s3
- **结论：** **无双读**；发版窗口人工迁移 Nacos 配置旧 data-id→新 data-id 后切应用。ADR-006 已改写。
- **约束：** 实施仍 HOLD（t84s2）；未授权不建仓/不改代码
- **后续：** 拆票含发版窗口 checklist；剩余开放项已基本收口


## LOG-019 — 2026-09-12T11:39:54+08:00 — Spec 正文对齐 LOG-017/018
- **状态：** done
- **结论：** `spec.md` 将 DEC-NACOS 改为硬切、DEC-SLUG→`NAMEWTA/WTA-plus`、DEC-LAYOUT→镜像 docs、上游/旧库用户按 ADR-012/013 锁定；废止正文残留「14 天双读 / NAMEWTA/wta」投影。
- **约束：** 实施仍 HOLD

## LOG-020 — 2026-09-12T11:42:00+08:00 — USER-DECISION [t93u]：自有 org.dromara → org.namewta（废止 org.wta）
- **设计树节点：** D-group / D-brand-split
- **轮次与依赖：** round 9 / LOG-003 + LOG-017/018/019
- **状态：** confirmed
- **类型：** USER-DECISION（CTO widget t93u）
- **问题：** 自有 Maven groupId / Java 基包目标是 `org.wta` 还是对齐既有 `namewta` 商标族？
- **事实与来源：**
  - 前端已 `@namewta/*`；Docker 已 `namewta-*`
  - 先前 ADR-002 / LOG-003 曾采纳短 group `org.wta`
  - CTO 锁定命名分裂：`ruoyi`→`wta`（模块/artifact/用户等）；**owned `dromara`→`namewta` family**
  - 第三方 `org.dromara.sms4j` / `warm` / `easyes` / `mica.mqtt` **KEEP**（「全部 dromara→namewta」= owned/first-party only）
- **选项：** A 维持 `org.wta` / B **`org.namewta`** / C `com.wta`
- **推荐 / 结论：** **B** — 自有 `org.dromara` → **`org.namewta`**；**SUPERSEDES** LOG-003 与 ADR-002 旧 Decision（`org.wta`）
- **原因：** 与 `@namewta` / Docker `namewta-*` 对齐；`wta` 保留给 ruoyi 名型（模块前缀 `wta-X` 等），不拿来当 Java group 段
- **影响工件：** ADR-002（重写）、ADR-001（分裂说明）、spec HC-02/FR-002/AC-PACKAGE/DEC-002、CONTEXT/goal-plan/tickets-map/source/REVIEW-BRIEF/MANIFEST/.status/SURVEY/THIRD-PARTY-KEEP/notes/封面；ticket T05/T02 目标句；architecture-review 投影
- **约束或不变量：**
  - 禁止盲替换所有 `org.dromara` → `org.namewta`
  - 第三方 KEEP 仍强制（ADR-003）
  - 模块前缀规则仍 `ruoyi-X`→`wta-X`（不变）
  - 实施 / 建仓 / push / 三门授权仍 **HOLD / false**
- **后续：** 文档一致性以本 LOG 为准；不发明新 AMBIGUOUS；不实施
- **替代/被替代：** **取代** LOG-003 的 owned 目标 `org.wta`；历史 LOG-003 正文保留并标 SUPERSEDED

## LOG-021 — 2026-09-12T11:43:25+08:00 — T-tickets 正式拆票（CTO HOLD）
- **状态：** completed-local（SpecDev tickets）；实施仍 HOLD
- **写面：** `ticket/00-…`…`17-db-user-migrate.md`；重写 `tickets-map.md`（plan_contract_version:1, plan_revision:1）
- **票数：** 18（T-00…T-17）；T04b 正式 id=**T-16**
- **覆盖：** ADR-012 WTA-plus + docs 镜像布局；ADR-006/LOG-018 Nacos 硬切；ADR-013→T-17；废止 NAMEWTA/wta、四顶层、14d/a–e
- **Ready：** 仅 T-00/T-01；T-02+ blocked-by-auth
- **三门：** 仍全部 false（未翻转）
- **外脑：** zip=`temp/chatgpt-packs/20260912T032524Z-T-tickets.zip`；ChatGPT 上传 Auto-review **blocked**；本地完成；**不豁免**实施门禁
- **未做：** I-implement / 建仓 / push / 产品代码 / 伪 Evidence

## LOG-022 — 2026-09-12T12:06:16+08:00 — CTO 授权实施并派执行
- **状态：** authorized
- **来源：** USER-DECISION [t98u]
- **结论：**
  - `implementation_authorized=true`（implementation_commit authorized）
  - `public_repo_publication_authorized=true`（仍须过 publication checklist 才可 push）
  - `legacy_repo_mutation_authorized=false`（旧仓继续冻结、不 rewrite）
  - 派 **RVP·实现**：Herdr→Grok→`/goal`，change 绝对路径本目录；必读 goal-plan/tickets-map/spec/ADR/CONTEXT；以落盘代码验收至 change 完成
- **约束：** 唯一执行面 Grok Bot 电脑；不默认 Cloud Agent；KEEP 第三方；硬切 Nacos；公开仓 WTA-plus
- **后续：** Lead 顺序派 I-implement；秘书跟踪回报

