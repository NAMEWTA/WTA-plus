# 实施路线（已确认计划）

当前唯一总控为<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/goal-plan.md</Path>，单票细节为<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/</Path>。本轮不执行。

1. G关闭完整frontier并明确共识，再S Ready、T DoR、P计划发布；用户自行激活目标后才实施。
2. 回查旧31票提交/真实验收，保留安全、权限、事务与前端合同；按G-legacy合法处理缺口，不重做旧功能。
3. T-32/33收口凭据与CORS；T-34/47恢复立即可见路径。
4. T-35—39/50修真实通知发送、IN_APP事务、重试与截止；T-41/40完成历史阅读与撤回。
5. T-44—46解耦诊断、报告事实、统一对象互斥；T-42补生产附件闭环。
6. T-43记录规模证据并最小批量优化，T-48/49收缩启动与重复配置。
7. T-30完成同候选全矩阵、release candidate与历史票关闭；G-complete后才申请A归档。

不制造水平数据库/前端空票；prefactor只在真实接缝缺失时纳入责任票。新旧接口仓内同批切换沿用已有用户决定，但实际数据升级必须Tag差异/备份/演练，不能重放基座。
