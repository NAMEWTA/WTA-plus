# Review brief — CTO asks for full SpecDev change review (pre-implement)

## Hard decisions already locked (do not reopen unless contradiction found)
1. Owned `ruoyi` → `wta`; owned `org.dromara` → **`org.namewta`** (NOT `org.wta`; LOG-020); KEEP sms4j/warm-flow/easy-es/mica-mqtt (never → org.namewta.*).
2. Prefix **swap** `ruoyi-` → `wta-` (not bare names).
3. Nacos data-id rename = **hard-cut no dual-read** (LOG-018); default user `wta`; seed literals rename; public slug `NAMEWTA/WTA-plus`.
4. **Freeze** old remotes: `ruoyi-vue-plus-namewta`, `frontend`, `ruoyi-vue-plus-docs` (see ADR-010 freeze definition).
5. Merge FE + BE + aggregate/docs into **one monorepo**, rename overall repo, extract content, wipe old layout, **orphan-reset history**, push to a **brand-new public** GitHub repo — **only after publication gate + auth**.
6. Gate: ChatGPT review → local doc iteration → **ask CTO before any implement / create-repo / legacy mutation**.

## Deterministic review question（必答）

> **Given only the SpecDev artifacts + evidence (including SOURCE-BASELINE and KEEP/inventory rules), can an implementer execute this change deterministically — without inventing classification rules, repository sequencing, or authorization — such that first-party rename, third-party KEEP, orphan public monorepo sequencing, and runtime/Nacos contracts are unambiguous and testable?**

Answer must be one of: **YES (ready for CTO auth ask)** / **CHANGES REQUIRED** / **BLOCKED**, with findings mapped to P0/P1.

Mandatory check topics: third-party KEEP verifiability, public publication sequencing (no public-before-clean), orphan acceptance, namespace ownership inventory, Nacos/runtime mapping, gate field semantics, baseline provenance.

## Status semantics（critical）

| Phrase | Means | Does NOT mean |
|---|---|---|
| `CHANGES REQUIRED` | Docs must improve before auth ask | Authorized to implement |
| `BLOCKED` | Implementation gate closed | Project cancelled |
| `iterated-p0-closed` | Local P0 doc work done | CTO authorized |
| `implementation_authorized=true` | Only CTO written flip | Reviewer opinion |

**CHANGES REQUIRED ≠ authorized. Local P0 close ≠ authorized.**

## Review focus
Critique ADR/LOG/CONTEXT/spec/goal-plan/tickets-map/MANIFEST-CHANGE/evidence for:
- gaps, contradictions (ADR-005 vs 010/011 must stay scrubbed)
- AC families + negative ACs
- monorepo layout still AMBIGUOUS (ok if explicit)
- KEEP blind-replace risks
- exact public slug still needing CTO
- SpecDev completeness before tickets

## Out of scope
Do not write production code; do not claim git/E2E/publication done; do not invent survey numbers not in evidence/; do not flip authorization gates.
