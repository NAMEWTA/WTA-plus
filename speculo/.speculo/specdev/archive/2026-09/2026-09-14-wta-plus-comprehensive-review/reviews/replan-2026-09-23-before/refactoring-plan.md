# 实施路线

唯一串行执行顺序见tickets-map：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>。先恢复门禁与安全输入边界，再处理SSO/发布、前端状态与材料、通知/转移及树/CRUD，最后收敛文档并运行整体验收。

无需兼容窗口或多代理调度。每票先复现其真实问题，完成最小修改和定向验收后继续；无证据的额外框架、协议和通用兜底不纳入。

正式总控由<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/goal-plan.md</Path>拥有；本文件仅为路线摘要。当前先关闭T-03/T-23决策缺口，再核对实施授权与workspace门禁。
