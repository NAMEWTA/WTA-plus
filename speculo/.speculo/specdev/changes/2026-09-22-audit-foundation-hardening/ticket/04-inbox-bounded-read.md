---
schema_version: 3
plan_contract_version: 1
skill_scan: "已读取根 AGENTS 与四个命中项目 Skill 入口；限定现有分层、事务、前端注入、SQL基座和交付门禁。"
skill_bindings: [{"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "implement", "operation": "apply-project-contracts-and-verify-scope", "inputs": ["当前 Ticket、固定源码和定向 diff"], "outputs": ["实际源码、回归测试与带退出码的 Evidence"], "required": true, "on_failure": "block-ticket"}, {"id": "namewta-fullstack-development", "path": "<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>", "sha256": "675c053c11d8b22cd394c875f48688242d8e8328dd14e69657f5d5b12f2af68b", "phase": "implement", "operation": "apply-project-contracts-and-verify-scope", "inputs": ["当前 Ticket、固定源码和定向 diff"], "outputs": ["实际源码、回归测试与带退出码的 Evidence"], "required": true, "on_failure": "block-ticket"}, {"id": "wta-module-guide", "path": "<Path>.agents/skills/wta-module-guide/SKILL.md</Path>", "sha256": "bb57a781314abe316f06ba9538f62043f1bd053904c0832969e575c09731a7a3", "phase": "implement", "operation": "apply-project-contracts-and-verify-scope", "inputs": ["当前 Ticket、固定源码和定向 diff"], "outputs": ["实际源码、回归测试与带退出码的 Evidence"], "required": true, "on_failure": "block-ticket"}, {"id": "wta-common-modules-guide", "path": "<Path>.agents/skills/wta-common-modules-guide/SKILL.md</Path>", "sha256": "d7b7e105c37499e0e0f8ad9b1e2dbf379100df5b6a47e0d6affb04483f8b162c", "phase": "implement", "operation": "apply-project-contracts-and-verify-scope", "inputs": ["当前 Ticket、固定源码和定向 diff"], "outputs": ["实际源码、回归测试与带退出码的 Evidence"], "required": true, "on_failure": "block-ticket"}]
resource_claims: ["audit-foundation-single-writer"]
artifact: "ticket"
change: "2026-09-22-audit-foundation-hardening"
id: "T-04"
title: "有界收件箱读取与批量投递成本"
status: "ready"
kind: "refactor"
planning_depth: "deep"
planning_depth_reason: "涉及现有公共合同、跨层行为、安全或故障恢复，必须交叉验证真实调用链。"
ready: true
risk: "high"
blocked_by: ["T-03"]
contract_ids: ["N-08", "N-10"]
owner: "audit-lead"
expected_changes: ["<Path>backend/wta-api/**</Path>", "<Path>backend/wta-modules/wta-notify/**</Path>", "<Path>backend/wta-admin/src/test/**</Path>", "<Path>frontend/packages/domains/notify/**</Path>", "<Path>frontend/packages/web-domains/notify/**</Path>", "<Path>frontend/apps/admin-web/**</Path>", "<Path>frontend/e2e/**</Path>", "<Path>release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql</Path>", "<Path>scripts/ci/**</Path>", "<Path>docs/**</Path>"]
writable_paths: ["<Path>backend/wta-api/**</Path>", "<Path>backend/wta-modules/wta-notify/**</Path>", "<Path>backend/wta-admin/src/test/**</Path>", "<Path>frontend/packages/domains/notify/**</Path>", "<Path>frontend/packages/web-domains/notify/**</Path>", "<Path>frontend/apps/admin-web/**</Path>", "<Path>frontend/e2e/**</Path>", "<Path>release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql</Path>", "<Path>scripts/ci/**</Path>", "<Path>docs/**</Path>"]
read_only_paths: ["<Path>backend/wta-modules/wta-sso/**</Path>", "<Path>frontend/apps/sso-web/**</Path>"]
shared_paths: []
shared_path_owners: []
---

# Ticket T-04：有界收件箱读取与批量投递成本

总体 Map：<Path>{roots.state}/specdev/changes/2026-09-22-audit-foundation-hardening/tickets-map.md</Path>。上游 Spec：<Path>{roots.state}/specdev/changes/2026-09-22-audit-foundation-hardening/spec.md</Path>。完成 Evidence：<Path>{roots.state}/specdev/changes/2026-09-22-audit-foundation-hardening/evidence/T-04.md</Path>。先读 Map、适用项目 Skill，再读本票。

## 1. 战略与来源

来源 USER-DECISION:2026-09-22 与 <Path>docs/reviews/WTA-plus-review-64b4ea7.md</Path> 的 N-08, N-10。目标：超过500条历史仍可访问；未读总数来自服务器，UI区分失败与空；规模优化以计数或实际测量为证据，不凭估计引入中间件。

## 2. 决策状态

已锁定：按审计最小正确方案修复，无兼容负担；保持授权/唯一约束/外部结果未知的安全边界。低影响假设：无额外新产品能力。未决问题：无；现场密钥轮换不以源码操作冒充。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 本票审计项、必要仓内消费者与测试 | 既有模块、API、Mapper、事务、Provider和前端runtime | SSO实现、真实服务操作、全站重写 |

## 4. 要构建什么

超过500条历史仍可访问；未读总数来自服务器，UI区分失败与空；规模优化以计数或实际测量为证据，不凭估计引入中间件。 错误必须可恢复且不显示伪成功，不放大无关配置故障。

## 5. 实现契约

输入/输出以受影响既有 API 为主；移除未实现参数时同步消费者，不接受静默忽略。不变量是用户归属、数据事务、供应商未知结果不重发、可选基础设施不阻塞核心。变更 POST 追踪脱敏，查询 GET；SQL 仅六文件基座。状态与错误遵循 Spec 和审计逐项方案，若源码否定原推断先记证据。

## 6. 执行路线

1. 读取真实上下游并建立失败用例，记录当前基线。
2. 修复本票路径，保持共享资源单写，不修改其他 active change。
3. 同步测试、接口消费者和文档，执行定向检查。
4. 运行适用整体门禁，记录真实提交、结果和残余项。

## 7. 路径访问契约

可写与预计修改点由 frontmatter 明确；只读 SSO 范围优先于较宽前端/后端通配。Lead 独占本 change 状态，串行交接共享源码。越界先修订计划。

## 8. 验证矩阵

| 行为或风险 | 接缝/步骤 | 预期 | Evidence |
|---|---|---|---|
| 正常/失败/边界 | 501条按用户稳定分页、未读计数、旧响应及失败状态；SQL 调用计数与有界批量基准，完整前端类型/构建/E2E。 | 真实断言通过；无供应商真实发送 | <Path>{roots.state}/specdev/changes/2026-09-22-audit-foundation-hardening/evidence/T-04.md</Path> |
| 回归 | 原质量门禁中的受影响模块检查 | 不关闭规则、不丢弃负向测试 | <Path>{roots.state}/specdev/changes/2026-09-22-audit-foundation-hardening/evidence/T-04.md</Path> |

Workspace checks：current 单一工作区；依赖构建在固定提交的 GitHub Actions 隔离checkout执行。E2E disposition：required。E2E owner/environment：audit-lead / current-workspace 的隔离固定提交候选；使用 fake provider 与临时数据库，不连接真实环境。Integration evidence：实际 implementation/source commit、parent before/result SHA 与验证对应头，未通过不得标 done。

## 9. 发布、迁移与恢复

前后端同版切换，无旧格式兼容窗口；新库六文件初始化。变更在独立分支，最终验证后一个PR合并；失败保留分支，不强推main、不部署、不重放业务基座。删除临时构建入口前保留测试证据。安全密钥轮换由真实环境授权入口处置。

## 10. 验收标准

- [ ] N-08, N-10 的可观察结果和负向断言通过。
- [ ] Skill执行、命令、退出码、固定源版本和风险记录于 Evidence。
- [ ] 所有变更在授权写集，原审计未丢失。
- [ ] 非空实现提交与整体集成/E2E通过，Ticket/Map/Evidence一致。

## 11. SKILL 调用计划

engineering-standards 审定分层、API、事务、源码命名和完整门禁；fullstack 验证跨前后端行为；module-guide 核查通知/系统公开边界；common-guide 避免新增重复基础能力。按 frontmatter 摘要读取实际入口及适用引用，并将产出和验证命令写入 Skill Execution Records。

## 12. 停止、检查点与交付

交付原报告一份、完成报告一份、最终PR一个。测试/能力缺失时记录明确阻塞，继续独立可验证修复但不标本票完成。检查点保持原始验证与未闭合动作；全票及整体完成后才进入A归档。
