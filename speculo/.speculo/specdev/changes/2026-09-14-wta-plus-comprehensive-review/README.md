# WTA-plus comprehensive review：当前执行入口

revision183：T40后端分段A2在a638ef48通过16定向单测、6真实MySQL/Redis，0fail/error/skip，源码前后clean、cleanup[]。A769e5d17元数据类型编译失败保留；公共Map<String,String>不变，内部noticeVersion用严格JSON字符串。完整候选attempts0；剩余竞争/重试/旧来源矩阵、前端和浏览器待实施，13done/2cancelled/T40in_progress/34ready。

最近T50 result `84ce0a9162dfc507fb4e8339575247477e57684f`：恢复批真实135零skip、clean/cleanup与静态通过；A3默认870执行/197skip/full/live及B前端736/core按输入等价复用。旧批三次原记录保留，整个change仍未完成。

本轮重规划基线为 `1264980c74e594bc594e88561bb292fbe5d968a1`：保留原31票编号与历史实现，新增19票覆盖新报告18项，T-30承担全部AC的最终集成。G共识和设计选择已确认，无需再次确认；2026-09-23早期“仅规划、目标未激活”是已被后续授权替代的历史状态。

阅读顺序：

1. <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/current-review.md</Path>：当前源码逐项结论、补充缺口与证据。
2. <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/design-tree.json</Path>、<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ADR.md</Path>：已接受决定、真实用户答复及旧合同替代关系。
3. <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/spec.md</Path>：用户行为、50项AC与范围。
4. <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>：完整50票、依赖、Skill与写集；具体施工见各票。
5. <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/goal-plan.md</Path>：串行顺序、Gate、历史票处置、验收/归档路线。
6. <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/verification.md</Path>、<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/replan-validation.json</Path>：完整业务验证矩阵和历史重规划校验；实施证据位于evidence目录。

旧Evidence与原报告按字节保留；原47份活动工件快照位于<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/replan-2026-09-23-before/</Path>。快照中的“尚未实现/暂缓提交/31票全绿”等仅描述其原时点，不能覆盖当前权威。

计划质量审查：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/plan-quality-review-2026-09-23.md</Path>。结构校验通过；共享写路径告警由已锁定的single-agent/current串行策略处理，不授权并行。

G-security-external已由实际MySQL/Redis/WTA MinIO轮换验证及用户对退役AI/qcloud密钥的撤销确认关闭。不得重复轮换。全应用部署、远程推送、真实存量数据修复及归档没有由当前本地实施授权自动涵盖。

## revision174 — 读投影路径纠正

Revision174: before production edits, correct T41 Mapper read projection to domain/model/read/NotifyInboxRow.java per FILES-002/005; no dto exception. 23 declared entries, same red-only phase;12done/2cancelled/T41in_progress/35ready.

## revision175 — T41分段检查点

Revision175: T41 backend checkpoints A1 b08105f failed (fixture MPJ result mapping), A2 662b351 passed3, A3 9d748aa passed4 after atomic first-time preservation. Full JAR/live OpenAPI fetched and generated in42f07eb. Frontend sole writer cors_audit active;12done/2cancelled/T41in_progress/35ready. This is partial implementation feedback, not complete-ticket integration; default full suite/frontend/browser still pending.

## revision176 — T41测试夹具修复

Revision176: complete T41 candidate B 05d0f34 failed integration attempt1 at frontend tests: missing Notice SFC SSR context and existing manifest registry test reaches browser Router through new user Store import. Architecture/lint/typecheck passed; later stages not run. Preserve raw failed evidence; add exact registry test path before fixture repair.24 declared entries;12done/2cancelled/T41in_progress/35ready. No product assertion weakened; Goal active.

## revision177 — T41三次复盘

Revision177: T41 current batch reached3 complete-candidate attempts: B frontend fixture failure; C frontend750/buildpassed but missing declared actual-host coverage; D frontend751/default870+201skip/full/realHTTP4/shared135 passed but owned browser seed SQL exec failed before JVM/Chrome. D sourceclean/cleanup[] preserved; ticket/workspace blocked, result null. Stop automatic resend. Lead diagnosis and materially changed dispatch required before resetting a recovery batch.12done/2cancelled/T41blocked/35ready; Goal remains active.

## revision178 — T41恢复派单

Revision178: clean d58fc3d owned SQL diagnostic proves four1267/HY000 failures and collated controls pass;501/2 fixture and cleanup pass, acceptance=false. Separate login40>30 fixture defect confirmed. Four-part Lead review and materially changed Dispatch02 recorded before recovery attempts reset0; old B/C/D three attempts immutable.12done/2cancelled/T41in_progress/35ready; Goal active.

## revision179 — 恢复E检查点

Revision179: recovery E a0dcbac8 attempt1 failed real Chrome at spec90 close locator after SQL501/2, real login, top10/detail body passed; source/JAR clean and cleanup[]. Five API-controlled Chrome cases passed5/0/0/0 on exactE using independently verified C production328 artifacts. Retain E failed result; two exact-name close locator fixes authorized within existing e2e scope; no runtime or assertion relaxation.12done/2cancelled/T41in_progress/35ready; oldbatch3 retained.

## revision180 — 恢复F检查点

Revision180: recovery F cb8063b6 attempt2 failed Chrome at B shared-title locator112 after Apage26/oldest/foreign-negative and Blogin/unread2/two rows. Seed title=summary renders twice; authorize two unique table-row/title-cell assertions, retaining counts and all negative/readAll checks. Ffull/core/static/OpenAPI pass, source/JAR stable cleanup[]. Oldbatch3 and recoveryE/F retained; nextG attempt3.12done/2cancelled/T41in_progress/35ready.

## revision181 — T41完成

Revision181: T41 accepted at c21de75f, recovery attempt3; G real501 Chrome1/0/0/0 and SQL A501unread0/B2 unchanged, original timestamps preserved, exactclean source/JAR and cleanup[]. Prior3 plus recoveryE/F failures immutable. Explicit D/C/E/F same-input test/build/contract reuse; both reviews pass.13done/2cancelled/35ready; nextT40; Goal active, no archive.

## revision182 — T40实施开始

revision182：T40开始，base601b9273；13done/2cancelled/1in_progress/34ready。T41已完成，新增为前置。cors_audit唯一产品writer；Lead治理/提交/隔离验收。先现API真实撤回红灯，再版本栅栏、三处链接一致、本人深链与竞争/回滚/浏览器验收。

## revision183 — 后端分段反馈

revision183：T40后端分段A2在a638ef48通过16定向单测、6真实MySQL/Redis，0fail/error/skip，源码前后clean、cleanup[]。A769e5d17元数据类型编译失败保留；公共Map<String,String>不变，内部noticeVersion用严格JSON字符串。完整候选attempts0；剩余竞争/重试/旧来源矩阵、前端和浏览器待实施，13done/2cancelled/T40in_progress/34ready。

真实run `ee99f3fbd678653d`，源码 `a638ef485c3819bfc8d67553877fd7c106ef9579`、tree `ceb4a5354641b7327bc65728bfaeb0f6678c52d8`；六SQL103表，2容器/1匿名卷/2端口及Maven进程组已回收。通过原红灯、两静态seed/普通missingIntent拒绝、late Snapshot SQL trigger完整回滚、V1撤回/V2送达、真实MAIL planner外部-only与混合三处path，未使用真实供应商。定向5类16测试均通过。原始失败与当前fresh XML/命令/哈希见 `T-40-current-2026-09-23/backend-a2/manifest.json`。这些是预先声明的后端反馈，未冒充整票验收。

Dispatch01C：cors_audit在原22写集内继续补充真正双连接gate先后/已获发送权真实ACCEPTED与UNKNOWN落库、旧owner、duplicate/retry零唤醒、legacy UNVERIFIED、本次Notice行数/关联冲突回滚等遗漏；保留现有6项绿灯。由Lead固定下一后端候选并串行运行，再移交前端/浏览器。若路径不足先登记；产品writer不自行构建、启动服务或提交。
