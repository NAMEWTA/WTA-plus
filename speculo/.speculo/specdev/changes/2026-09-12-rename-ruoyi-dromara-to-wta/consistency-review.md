# Documentation Consistency Re-review

**Change:** `2026-09-12-rename-ruoyi-dromara-to-wta`  
**Reviewer role:** RVP·架构审执行助手（Grok Bot / box `/workspace` only）  
**Review type:** 文档一致性复核（非第二次完整热点架构审）  
**Captured at:** 2026-09-12T11:16:41+08:00 (Asia/Shanghai)  
**Authority path:** `/workspace/vp-dev/ruoyi-vue-plus-docs/speculo/.speculo/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/`  
**Inputs:** `external-brain/reply.md` + `meta.md` + `notes.md`；`LOG-013`/`LOG-014`；`ADR.md` / `CONTEXT.md` / `spec.md` / `goal-plan.md` / `tickets-map.md` / `MANIFEST-CHANGE.md` / `.status.json`；`evidence/SOURCE-BASELINE.md` + `THIRD-PARTY-KEEP.md`；既有 `architecture-review.md`；封面 `/workspace/delivery/rvp-change-2026-09-12-rename-wta.md`

---

## 裁决

**`consistency-pass-with-nits`**

权威合同链（CTO > ADR-010/011 > spec > goal-plan > tickets）对 ChatGPT P0-01…P0-08 的本地关闭内容**一致且可复核**；未发现会把实施/公开/旧仓 mutation 误开的门禁语义冲突。残留为**证据快照 / 历史审阅稿**未加废止横幅类 NIT，**不构成 FAIL**。

**Hard limits observed this turn:** 未改产品代码；未翻转任何 `*_authorized` / `execution_authorization`；未建/未 push 新仓；未改写已拍板 ADR 结论；未改 `.status.json`（仅建议主会话补丁）。

---

## P0 八项表

| # | Checklist | Result | Evidence (path + excerpt) |
|---|---|---|---|
| 1 | ADR-010/011 Supersedes 已传播；无「聚合 KEEP / 去前缀」冲突投影 | **NIT** | **PASS on authority docs:** `ADR.md` ADR-005 作废清单 + ADR-010/011 `Supersedes`；`CONTEXT.md` 标明 Topology authority=ADR-010/011；`goal-plan`/`spec`/`tickets-map`/`封面` 均写换前缀 `wta-` 与 monorepo/orphan，封面明确「聚合仓 KEEP / 全局不 orphan **已废止**」。**NIT:** `evidence/SURVEY.md:133-134` 仍写「**建议本期 KEEP 路径**」与「目录是否**去掉** `ruoyi-` 前缀 → AMBIGUOUS」，无废止横幅——易被误读为 active topology（应加「superseded by ADR-010/011/008」）。`LOG-006` 为历史条目，可接受。既有 `architecture-review.md` 仍描述 P0 关闭前的 CONTEXT/封面漂移，属历史 R 稿。 |
| 2 | Authorization Gate 字段齐全且全 false；Review≠Implement | **PASS** | `.status.json`: `implementation_authorized` / `public_repo_publication_authorized` / `legacy_repo_mutation_authorized` 均为 `false`；`execution_authorization.*` 全 `not-authorized`。`goal-plan.md` §0 + Authorization Matrix；`MANIFEST-CHANGE.md` §2；`spec.md` HC-08/09/10 + NAC-04；`REVIEW-BRIEF.md`:「CHANGES REQUIRED ≠ authorized. Local P0 close ≠ authorized。」；「`implementation_authorized=true` \| Only CTO written flip \| Reviewer opinion」。封面 Gate 表全 false。 |
| 3 | documentation / review / implementation 状态语义不冲突 | **PASS** | 拆分存在且一致：`documentation_status=iterated-p0-closed`；`review_status=local-p0-addressed-awaiting-cto`；`implementation_status=not-started`；`implementation_gate=blocked`；`ready_for_execution=false`（goal-plan / MANIFEST）。无单一 undifferentiated `READY`。**微 NIT（不升格）:** `spec.md` §11 未单列 `review_status`（goal-plan/.status 已有）。 |
| 4 | publication 顺序：清理/扫描/KEEP/授权后才 public push | **PASS** | `ADR-010` sequencing +「**No public push before publication gate.**」；`spec.md` §5 Publication gate（baseline / secret / forbidden / KEEP / license / residual / explicit auth）+ Negative「no public first, clean later」；`goal-plan.md` hard sequence W-PubReady → G-Auth-Pub → T14；`tickets-map` T12→T13→T14。 |
| 5 | KEEP 三类 MUST KEEP/RENAME/REVIEW；禁止 zero-match AC | **PASS** | `spec.md` §2 三类齐全；HC-12 + AC-RESIDUAL + NAC-05：「Zero-match is not required / not AC」；`ADR-003` Consequences 同义；`tickets-map` T09 Forbidden「Zero-match as pass」；`THIRD-PARTY-KEEP.md` 规则保留。 |
| 6 | ownership inventory 合同存在 | **PASS** | `spec.md` §3 Namespace ownership inventory（before auto-migrate；cite SOURCE-BASELINE；T02）；`CONTEXT` glossary；`tickets-map` T02；`goal-plan` W-Inv。 |
| 7 | Nacos/runtime 合同 + 对照表/退出门禁占位可验收 | **PASS** | `spec.md` §4 Runtime Naming Contract（identifiers 表 + Dual-read「No indefinite dual-read」+ mapping **PLACEHOLDER** 表 + Exit gate「quantified duration + metrics TBD」）；AC-RUNTIME；ADR-006；T08。占位可验收；时长/度量仍属 CTO AMBIGUOUS（非合同缺失）。 |
| 8 | SOURCE-BASELINE SHA/remote/zip hash 可追溯 | **PASS** | `evidence/SOURCE-BASELINE.md` 三仓 remote+SHA+dirty + zip SHA-256。本轮实测复现：docs `cf9842660df2557439e13a3fca27cc2cac155210`；backend `60c9d31f9419e7d7560706a2b6a41ff44b41d551`；frontend `d77b55651e5754a54508e51a79f536afbd2392b8`；zip `2a8c4f5205d27a16a43ca4f7484a130fd5a3c4a77ba03ca12a82b07d86f305a6` 与文件/meta 一致。 |

---

## 残留缺口

1. **NIT-SURVEY：** `evidence/SURVEY.md` § 分类表仍投影「聚合仓 KEEP 路径」「去掉前缀 AMBIGUOUS」——建议主会话加一行 superseded 横幅指向 ADR-010/011/008（**勿**改拍板 ADR）。  
2. **NIT-ARCH-REVIEW-STALE：** `architecture-review.md` 为 P0 关闭前本地 R 稿；读者应优先看 LOG-014 + 本 consistency 报告。可选在文首加「superseded for conflict claims by LOG-014 / consistency-review」。  
3. **CTO AMBIGUOUS（仍 open，不阻塞「问」实施，但阻塞盲目细票冻结）:** public slug；monorepo layout；Nacos dual-read duration+exit metrics；旧库 username 策略；`repository.url`/upstream 政策；三门书面授权。  
4. **无正式 `ticket/*.md`** — tickets-map 仅为 skeleton（预期）。

封面 `/workspace/delivery/rvp-change-2026-09-12-rename-wta.md`：**未发现**过时「去前缀 / 聚合 KEEP 仍有效」措辞；已正确标注废止。

---

## 是否建议再 pack ChatGPT

**不强制再 pack。**  
理由：本轮为本地一致性复核；ChatGPT computerUse 上传已知易 Auto-review BLOCKED；P0 已在 SpecDev 吸收且权威链一致。  
**建议：** 仅当 CTO **明确要求**外脑二次确认 P0 关闭结果时，再由主会话决定 pack；pack 前宜先 scrub SURVEY NIT。本助手本轮**未**启动 ChatGPT 上传。

---

## 是否可向 CTO **问**实施

**可以问（ask），不可自授（authorize）。**

- 文档一致性已达 `consistency-pass-with-nits`，满足 goal-plan「consistency re-review → CTO implement ask」顺序。  
- **问 ≠ 授权：** 须 CTO 书面翻转 `implementation_authorized`（及后续 publication / legacy mutation）；当前全 false。  
- 问实施时可**并行**请 CTO 点名 AMBIGUOUS（slug/layout/Nacos 退出度量/旧库策略/URL 政策）。

---

## 建议 `.status.json` 补丁（主会话改；本助手未改）

```json
{
  "documentation_gate": "consistency-pass-with-nits",
  "review_status": "consistency-pass-with-nits-awaiting-cto-ask",
  "updated_at": "2026-09-12T11:16:41+08:00",
  "notes_append": "2026-09-12T11:16+08: RVP·架构审 consistency re-review = consistency-pass-with-nits. P0-02..08 PASS; P0-01 NIT (SURVEY stale KEEP/去前缀投影). All *_authorized remain false. May ASK CTO for implementation (ask≠auth). ChatGPT re-pack not required unless CTO requests. Reports: temp/team/architecture-review/2026-09-12-rename-consistency.md + changes/.../consistency-review.md."
}
```

建议同步：`blockers` 将「Consistency re-review dispatched…」替换为「Consistency = pass-with-nits；awaiting CTO ask + AMBIGUOUS answers」；**勿**把 `implementation_gate` 改为可执行。

---

## 本轮写入

1. `/workspace/vp-dev/ruoyi-vue-plus-docs/temp/team/architecture-review/2026-09-12-rename-consistency.md`  
2. `.../changes/2026-09-12-rename-ruoyi-dromara-to-wta/consistency-review.md`（权威副本）  
3. `external-brain/notes.md` — 追加 consistency 本地复核一行（不覆盖 locked 段）
