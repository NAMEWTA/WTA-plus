# 设计日志 — 2026-09-12-wta-sso

## LOG-001 — 2026-09-12T17:26:04+08:00 — 创建 change 并冻结 CTO intake

- **设计树节点：** 不适用
- **轮次与依赖：** round 0 / 无
- **状态：** confirmed
- **问题：** 如何承接 CTO SSO 技术方案而不进入实现
- **事实与来源：**
  - Intake：`temp/team/lead/intake/2026-09-12-wta-sso-proposal.md`
  - 副本：`temp/team/goal-plan/INTAKE-20260912-wta-sso.md`（sha256 相同）
  - 派单：`temp/team/lead/dispatch-20260912-wta-sso-goal-plan.md` → RVP·规划 / P-goal-plan
  - content_sha256：`839aa05666d49789ba013bcf1f53f5dbef04b598e172e006b734def497f281c7`
  - Baseline：CTO `b06d161`；HEAD `d1ce372`（祖先含 b06d161）
- **选项：** A 直接改代码 / B 先 SpecDev P-goal-plan draft / C 仅口头方案
- **推荐：** B
- **结论：** 新建 active change `2026-09-12-wta-sso`；冻结 `source.md`；写 draft `goal-plan.md`（`ready_for_execution=false`）；execution_authorization 全 not-authorized；leadership=`rvp-lead:1c44f1c3-a61e-4d39-a7a7-f2d21ce5fb00`
- **原因：** 高事故半径（认证/会话/Client 隔离）；CTO 仍有 4 个开放问题；派单明确本期只出方案工件
- **影响工件：** `.status.json`、`source.md`、`goal-plan.md`、`CONTEXT.md`、`ADR.md`、`tickets-map.md`、`evidence/`、`external-brain/notes.md`、全局 `status.json`
- **约束或不变量：** 零产品代码；零 push/PR；不触碰 `2026-09-10-notify-channel-config`；不声称外脑已通过；不造假票
- **后续：** 下一 Work=`specdev/G-grill-with-docs`；父进程补外脑

## LOG-002 — 2026-09-12T17:26:04+08:00 — P-goal-plan draft 收口要点

- **设计树节点：** D-protocol / D-module / D-phase
- **轮次与依赖：** round 0 / LOG-001
- **状态：** proposed（待 Grill 升级）
- **结论摘要：**
  1. 协议：Authorization Code + PKCE S256；无外置 IdP；禁 Implicit/password/SAML；OIDC→P1
  2. 模块：`wta-sso` + `sso-web`；扩展 `sys_client`；access_token=Sa-Token 且 extras=目标业务 Client
  3. P0：authorize/token/revoke + local/sso/both
  4. 硬验收：同浏览器 SSO→admin→home，两 Token clientid 不同且互拒
  5. blockers 写入 CTO-Q1…Q4
- **影响工件：** goal-plan / ADR-001…006（proposed）/ CONTEXT
- **后续：** G-grill 访谈；勿将 proposed 标为 accepted

## LOG-EB-001 — 2026-09-12T17:40:00+08:00 — ChatGPT 外脑 CHANGES REQUIRED 已晋升
- **状态：** confirmed（外脑已跑）；门禁未通过
- **事实：** zip `20260912T092830Z-P-goal-plan.zip` 上传 ChatGPT（Latest/Wing Pro）；结论 CHANGES REQUIRED
- **影响工件：** `external-brain/reply.md`、`external-brain/notes.md`、ingest `temp/chatgpt-ingest/20260912T093900Z-P-goal-plan-wta-sso/`、goal-plan Progress 语义收紧为 precursor
- **后续：** G-grill-with-docs；正式 Ready P 待 G→S→T 后二次收敛；禁止 I-implement

## LOG-003 — 2026-09-12T17:42:00+08:00 — 激活 G-grill-with-docs
- **设计树节点：** 不适用
- **轮次与依赖：** round 0 / LOG-EB-001
- **状态：** confirmed
- **问题：** 是否进入 Grill 锁定开放决策
- **事实与来源：** Lead PRIORITY 派单 G-grill；P precursor CHANGES REQUIRED；新 zip `temp/chatgpt-packs/20260912T094254Z-G-grill.zip` sha256 `9f12f4cf5e610e56f1bd217e91dee9315e16c719224ce78ab817a495f7323115`
- **选项：** 进入 G / 直接 S / 实现
- **推荐：** 进入 G
- **结论：** `current_work=specdev/G-grill-with-docs`；创建 design-tree round1（CTO-Q1..Q4 + F-02..F-08）；外脑须新 reply，不沿用 P 包结论当本轮通过
- **原因：** 高影响认证边界未拍板
- **影响工件：** `.status.json` / `design-tree.json` / external-brain（待晋升）
- **约束或不变量：** 未拍板不 locked；不碰 notify-channel-config；不实现；不假装外脑通过
- **后续：** ChatGPT 上传；抛 Round1 frontier 给 Lead→CTO
- **替代/被替代：** 无

## LOG-004 — 2026-09-12T17:44:00+08:00 — CTO-Q1 / D-001=A
- **设计树节点：** D-001
- **轮次与依赖：** round 1 / 无
- **状态：** confirmed
- **问题：** P0 是否按整包推进
- **事实与来源：** Lead【CTO 部分拍板】Q1=是；Lead 指令锁定 D-001=A
- **选项：** A 整包 / B 缩小 / C 扩大
- **推荐：** A
- **结论：** A。授权码+PKCE+sso-web+扩展 sys_client+本地登录并存。
- **原因：** CTO via Lead
- **影响工件：** CONTEXT / ADR-001..004（仍 proposed→可标方向已确认部分） / 下游 F 节点 frontier
- **约束或不变量：** 未答 Q2/Q4 前不进 S；不实现
- **后续：** 继续 Q2/Q4；解锁依赖 D-001 的 F 节点
- **替代/被替代：** 无

## LOG-005 — 2026-09-12T17:44:00+08:00 — CTO-Q3 / D-003 默认 both + 可选直连
- **设计树节点：** D-003
- **轮次与依赖：** round 1 / D-001
- **状态：** confirmed
- **问题：** admin/home 默认 authMode
- **事实与来源：** Lead【CTO 部分拍板】默认 both；并允许部分入口直接 sso
- **选项：** 原 A/B/C/D；CTO 收敛为「默认 both + 可选直连入口」
- **推荐：** 原 A；现按 CTO 措辞写入
- **结论：** 默认 `both`；允许部分入口配置为直接 `sso`；勿当作互斥冲突。
- **原因：** CTO via Lead
- **影响工件：** CONTEXT / ADR-004
- **约束或不变量：** 客户端管理须能表达入口级覆盖（细节进 Spec）
- **后续：** 无阻塞；Q2/Q4 仍开放
- **替代/被替代：** 无

## LOG-EB-002 — 2026-09-12T17:56:00+08:00 — G-grill 外脑 reply 已晋升
- **设计树节点：** 不适用
- **轮次与依赖：** round 1 / LOG-003
- **状态：** confirmed（外脑已跑）；非 Grill consensus
- **问题：** 本轮 ChatGPT 分析是否可用
- **事实与来源：** zip `20260912T094254Z-G-grill.zip`；model=Latest（无 GPT-6 Pro）；ingest `20260912T094500Z-G-grill-wta-sso/`；session `6aa52008-4594-83e9-b6f7-4bfd5da41877`
- **选项：** 晋升 / 丢弃
- **推荐：** 晋升为候选；CTO 拍板仍走 Lead
- **结论：** `external-brain/reply.md` + `notes.md` 已更新；强化 Q4a/Q4b 拆分与 F-02…F-08 拍板选项；保持 ready_for_execution=false
- **原因：** Lead 要求新 zip 新 reply；禁止假装通过
- **影响工件：** external-brain/* / 开放决策表
- **约束或不变量：** 不覆盖已答 D-001/D-003；不进 S/I
- **后续：** CTO Q2/Q4；可选并行拍 F 节点
- **替代/被替代：** 本文件 reply 替换 P 轮外脑正文（P ingest 仍保留）

## LOG-006 — 2026-09-12T18:31:00+08:00 — CTO-Q2 / D-002=A
- **设计树节点：** D-002
- **轮次与依赖：** round 1 / 无
- **状态：** confirmed
- **问题：** SSO 域名与各环境 callback
- **事实与来源：** Lead【CTO 拍板】D-002=A
- **选项：** A 环境矩阵可占位 / B 必须真实生产域名才开 Spec / C 只定本地
- **推荐：** A
- **结论：** A。环境级 Origin/callback 矩阵；可先占位；精确匹配禁 `*`。
- **原因：** CTO via Lead
- **影响工件：** CONTEXT / ADR-005
- **约束或不变量：** Spec 须含矩阵表结构；真实 DNS 可后填但不得省略行
- **后续：** 继续 F 节点
- **替代/被替代：** 无

## LOG-007 — 2026-09-12T18:31:00+08:00 — CTO-Q4 / D-004=A1+B2
- **设计树节点：** D-004
- **轮次与依赖：** round 1 / 无
- **状态：** confirmed
- **问题：** 生产进程拓扑与 Web Origin
- **事实与来源：** Lead【CTO 拍板】D-004=A1+B2；外脑 G-grill 同向
- **选项：** Q4a 同进程/独立进程；Q4b 同站/独立 Origin
- **推荐：** A1+B2
- **结论：** 同进程组装 + sso-web 独立 Web Origin。
- **原因：** CTO via Lead；两维不互斥
- **影响工件：** CONTEXT / ADR-005；解锁 D-013
- **约束或不变量：** 独立 Origin 下 Cookie Domain/host-only 须进 Spec；独立后端进程非 P0
- **后续：** D-013 进入 frontier
- **替代/被替代：** 无

## LOG-008 — 2026-09-12T18:33:00+08:00 — F-02 / D-010=A
- **设计树节点：** D-010
- **轮次与依赖：** round 2 / D-001
- **状态：** confirmed
- **事实与来源：** Lead【CTO Round2 全荐 A】D-010=A
- **结论：** PKCE/code 负向合同全进 P0 AC。
- **影响工件：** ADR-001 / Spec AC
- **后续：** 无

## LOG-009 — 2026-09-12T18:33:00+08:00 — F-03 / D-011=A
- **设计树节点：** D-011
- **轮次与依赖：** round 2 / D-001
- **状态：** confirmed
- **事实与来源：** Lead【CTO Round2】D-011=A
- **结论：** P0-SSO-REUSE + P0-CLIENT-ISOLATION 两门硬验收。
- **影响工件：** Spec 硬验收
- **后续：** 无

## LOG-010 — 2026-09-12T18:33:00+08:00 — F-04 / D-012=A
- **设计树节点：** D-012
- **轮次与依赖：** round 2 / D-001
- **状态：** confirmed
- **事实与来源：** Lead【CTO Round2】D-012=A
- **结论：** wta-sso 仅 API/Port；禁直依赖 system 实现。
- **影响工件：** ADR-003
- **后续：** 无

## LOG-011 — 2026-09-12T18:33:00+08:00 — F-05 / D-013=A
- **设计树节点：** D-013
- **轮次与依赖：** round 2 / D-004
- **状态：** confirmed
- **事实与来源：** Lead【CTO Round2】D-013=A
- **结论：** 后端 Set-Cookie 建 SSO HttpOnly 会话；authorize 读之。
- **影响工件：** ADR-005 / Spec Cookie 合同
- **后续：** 无

## LOG-012 — 2026-09-12T18:33:00+08:00 — F-06 / D-014=A
- **设计树节点：** D-014
- **轮次与依赖：** round 2 / D-001
- **状态：** confirmed
- **事实与来源：** Lead【CTO Round2】D-014=A
- **结论：** platform 纯合同；adapters 持浏览器实现。
- **影响工件：** ADR-003
- **后续：** 无

## LOG-013 — 2026-09-12T18:33:00+08:00 — F-07 / D-015=A
- **设计树节点：** D-015
- **轮次与依赖：** round 2 / D-001
- **状态：** confirmed
- **事实与来源：** Lead【CTO Round2】D-015=A
- **结论：** 第一方 SPA=public；禁 browser secret。
- **影响工件：** ADR-001 / Client 管理语义
- **后续：** 无

## LOG-014 — 2026-09-12T18:33:00+08:00 — F-08 / D-016=A
- **设计树节点：** D-016
- **轮次与依赖：** round 2 / D-001
- **状态：** confirmed
- **事实与来源：** Lead【CTO Round2】D-016=A
- **结论：** revoke≠SLO；SLO→P1。
- **影响工件：** ADR / Spec revoke 合同
- **后续：** 无

## LOG-015 — 2026-09-12T18:33:00+08:00 — Grill 共识
- **设计树节点：** 不适用
- **轮次与依赖：** round 2 / 全部节点
- **状态：** confirmed
- **问题：** frontier 是否为空并可交 S-spec
- **事实与来源：** design-tree 11/11 answered；CTO via Lead 完成 Q1–Q4 与 F-02…F-08
- **选项：** consensus 并建议交 S / 继续 grill
- **推荐：** consensus；可交 S-spec
- **结论：** design-tree=`consensus`。**可进 S-spec**；由 Lead 派 RVP·规格。本岗不自启 S、不实现。
- **影响工件：** design-tree.status / .status.json
- **约束或不变量：** ready_for_execution 仍 false 直至后续授权；不碰 notify-channel-config
- **后续：** Lead 派 S-spec
- **替代/被替代：** 无

## LOG-016 — 2026-09-12T18:34:00+08:00 — CTO 否决此时进入 S-spec
- **设计树节点：** 不适用
- **轮次与依赖：** round 2 / LOG-015
- **状态：** confirmed
- **问题：** Grill consensus 后是否立即交 S-spec
- **事实与来源：** Lead【Lead·CTO】**不可以进 S-spec**；Round2 已拍 A 仍先停在 Grill/方案层；不要标「可交 S」、不要催派 RVP·规格
- **选项：** 立即交 S / 停在 Grill / 开 Round3
- **推荐：** 遵从 CTO：停在 Grill/方案层
- **结论：** **否决此时进入 S-spec**。design-tree 决策共识可保留，但下游 Work 门禁关闭。LOG-015 中「可进 S-spec / 请 Lead 派规格」**作废**。
- **原因：** CTO via Lead（书面原因待 Lead 补充；本条先记录否决事实）
- **影响工件：** 开放决策表 / `.status.json` blockers / LOG-015 后续指针
- **约束或不变量：** 不自启 S；不实现；不催 RVP·规格
- **后续：** 等 Lead 补充否决原因；若需 Round3 或其它方案层工作再报
- **替代/被替代：** 替代 LOG-015 关于「可交 S」的结论（共识记录本身不撤销）

## LOG-017 — 2026-09-12T18:34:30+08:00 — CTO t181u 否决覆盖：停催 S
- **设计树节点：** 不适用
- **轮次与依赖：** LOG-016 / Lead 否决覆盖
- **状态：** confirmed
- **问题：** consensus 是否等于授权进入 S-spec
- **事实与来源：** Lead【Lead·CTO 否决覆盖】引用 CTO t181u：**不可以进 S-spec**；不要等待或催派 RVP·规格；`current_work` 保持 null；访谈可停在此收口
- **选项：** 催 S / 停 S 并收口 Grill
- **推荐：** 停 S 并收口
- **结论：** Grill consensus **可保留**为决策收口；**不等于**授权 S。`current_work=null`。访谈岗本 change 收口待命。
- **原因：** CTO t181u via Lead
- **影响工件：** `.status.json` / 开放表
- **约束或不变量：** 不自启 S；不催规格；不实现
- **后续：** 若需否决原因细节再问 Lead/CTO；无 Round3 除非新派单
- **替代/被替代：** 强化 LOG-016

