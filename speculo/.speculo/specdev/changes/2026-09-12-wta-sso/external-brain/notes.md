# External brain notes — 2026-09-12-wta-sso / G-grill

**Updated:** 2026-09-12T17:56+08:00

## 本轮外脑（G-grill）

- **Zip：** `temp/chatgpt-packs/20260912T094254Z-G-grill.zip`（sha256 `9f12f4cf5e610e56f1bd217e91dee9315e16c719224ce78ab817a495f7323115`）
- **Ingest：** `temp/chatgpt-ingest/20260912T094500Z-G-grill-wta-sso/`
- **Model：** UI 显示 **Latest**（GPT-6 Pro 未露出）；thinking effort High
- **Session：** `https://chatgpt.com/c/6aa52008-4594-83e9-b6f7-4bfd5da41877`
- **Reply：** `external-brain/reply.md`（已覆盖晋升；旧 P-goal-plan reply 已被本轮替换——P 结论仍见 ingest `20260912T093900Z-P-goal-plan-wta-sso/`）
- **结论口径：** 可继续 Grill；**不可**宣称 consensus；**保持** `ready_for_execution=false`；不进实现

## Lead / 访谈接受点

1. **Q4 拆分正确：** Q4a 同进程 vs Q4b 独立 Web Origin 不互斥；荐 **A1+B2** 与外脑一致，继续请 CTO 拍。
2. **Q2：** 外脑要求环境级矩阵（SSO Web Origin + Auth Origin + admin/home callback）；对齐 D-002 荐 A。
3. **F-02…F-08：** 外脑均给可拍板选项；与 design-tree D-010…D-016 对齐。主推荐：负向 PKCE AC、SSO-REUSE+隔离两门、API/Port 边界、后端 Set-Cookie SSO 会话、platform/adapters 分层、SPA=public、revoke≠SLO。
4. **F-07：** 外脑对 confidential 运行时有 A（压缩）/B（管理即运行）分叉——Grill 时让 CTO 选，默认倾向第一方 SPA=public。
5. **外脑说包内无 design-tree：** 过时——本地 owner 已建 `design-tree.json`（D-001/D-003 已答）；不据此否定本地树。

## 已由 CTO via Lead 确认（高于外脑 Q3 原文）

- D-001/Q1=A（整包）
- D-003：默认 `both` + **允许部分入口直 sso**（外脑原文偏「先 both 再切」；以 CTO 拍板为准）

## 仍开放

- D-002/Q2、D-004/Q4a+b；以及已解锁的 D-010…016（D-013 仍依赖 D-004）

## 忽略

- 「无 design-tree 则不能 Grill」——本地已有树
- 任何把 reply 当 accepted ADR / Ready Spec / 可执行 P 的暗示

## 下一动作

等 CTO 拍 Q2/Q4；外脑 F 节点可并行请拍；不进 S-spec / I-implement。
