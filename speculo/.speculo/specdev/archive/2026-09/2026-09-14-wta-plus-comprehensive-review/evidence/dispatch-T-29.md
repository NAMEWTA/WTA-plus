# Dispatch Packet T-29-20260923-01

operation=dispatch；task_kind=implementation；native gpt-6-sol/xhigh；owner=cors_audit；Lead=single-agent；current/main/direct-parent；fixed base `7a1810288d3292ceeb4987f488b13353af1a1286`，单产品writer，无新worktree。

先完整Map→绑定Skill/当前规则→T29/AC029；/tmp/wta-t29-audit.md、/tmp/wta-t29-replan-audit.md是已复核预研。T29→T01依赖已按当前验收重排，旧合同保留。

产品写集仅以下12路径（Ticket旧范围仅为历史归属，本次不额外改它们）：
- .agents/skills/deploy-namewta-environment/SKILL.md
- .agents/skills/deploy-namewta-environment/references/existing-site-takeover.md
- .agents/skills/deploy-namewta-environment/references/middleware-database-oss.md
- .agents/skills/deploy-namewta-environment/references/rolling-full-stack-release.md
- .agents/skills/deploy-namewta-environment/references/upgrade-and-rollback.md
- .agents/skills/project-customization-delivery/SKILL.md
- .agents/skills/project-customization-delivery/references/inputs.md
- .agents/skills/project-customization-delivery/references/verification.md
- .agents/skills/engineering-standards/references/project/00-project-profile.md
- .agents/skills/engineering-standards/scripts/validate-skill-facts.mjs
- .agents/skills/engineering-standards/scripts/validate-skill-facts.test.mjs
- .agents/skills/engineering-standards/references/project/01-module-map.md


统一旧默认路径到temp/release，保留0600/忽略/唯一最新报告/备份/MUST全部约束；项目定制Skill只修默认事实，不激活其发布流程；Profile修release-state.mjs，Module Map按当前SmsDeliveryQueryClientTest修正common-sms测试根，不保留none。facts checker全Skill Markdown旧拼写扫描，两个独立Skill family负向fixtures；保留其他门禁，不能用放宽检查/删目录通过。

禁止读取/写入temp/私有恢复文件/部署服务器，Lead单独执行私有备份迁移；禁止SpecDev/commit/推送/服务/数据库/E2E/build。允许node合成fixture与部署工具synthetic测试，日志/tmp/wta-t29。先红灯后修；当前仓库facts可能在Lead迁移前仍因旧目录失败，报告真实原因，不自行处理目录。Lead在产品返回后最终实际验收。所有源改动一次归此Ticket，不影响T47已完成产品。返回paths/commands/cwd/exit/count/skip/Skill轨迹/已知未验证项及写锁，不自行Done。
