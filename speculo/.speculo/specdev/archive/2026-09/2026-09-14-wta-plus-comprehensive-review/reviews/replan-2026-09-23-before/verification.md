# 审查交付验证

2026-09-18单人串行执行，仅修改本change。产品实现未开始。

## 已执行的当前基线

工作目录均为 `/srv/WTA-plus`。完整命令与输出见 re-review-command-results.json：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/re-review-command-results.json</Path>。

| 命令 | 退出码 | 结果 |
|---|---|---|
| `node .agents/skills/engineering-standards/scripts/validate-skill-facts.mjs` | 1 | 错误要求temp/release存在；纳入T-01 |
| `node .agents/skills/namewta-fullstack-development/scripts/validate-skill.mjs` | 0 | 19文件、16模板检查通过 |
| `node docs/fm/scripts/validate.mjs` | 0 | 32模板静态检查通过 |
| `node .agents/skills/namewta-fullstack-development/scripts/validate-module-mode.mjs backend/wta-modules/wta-profile/wta-profile-person --mode layered` | 0 | 168 Java文件 |
| 同上检查器，目标 `backend/wta-modules/wta-profile/wta-profile-enterprise` | 0 | 130 Java文件 |
| 同上检查器，目标 `backend/wta-modules/wta-notify` | 1 | Javadoc注解误报；wake publisher真实结构命中，分别纳入T-01/T-28 |
| 同上检查器，目标 `backend/wta-modules/wta-sso` | 1 | support依赖ResponseCookie，纳入T-06 |
| 同上检查器，目标 `backend/wta-modules/wta-third` | 0 | 71 Java文件 |
| `bash release-artifacts/scripts/verify-release.sh` | 1 | 43项，41通过、2失败：缺CI文件、误将Speculo供应商Skill列为必须不存在 |

上述是尚未修复的基线，不能冒充产品验收完成。发布测试的临时夹具由脚本创建/清理，无部署或数据库操作。

## 文档与范围校验

- `node speculo/workflows/specdev/common/tools/validate-specdev.mjs --repo /srv/WTA-plus speculo/.speculo/specdev/changes/2026-09-14-wta-plus-comprehensive-review`：退出0，0错误、0警告；见记录：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/re-review-document-check.json</Path>。
- `git diff --check -- speculo/.speculo/specdev/changes/2026-09-14-wta-plus-comprehensive-review`：退出0。
- JSON、31票frontmatter与plan-data路径/依赖、31个AC、串行拓扑顺序、相对链接和乱码检查通过；见一致性记录：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/re-review-validation.json</Path>。
- 源码清单重算为50 POM、247后端Java测试源、3 App；旧method重新枚举为70项/27文件，原59项漏掉11个无括号@PutMapping。见当前清单：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/re-review-inventory.json</Path>。
- 对比复核开始时change外已跟踪文件SHA-256，无新增修改；其他工作区既有改动保留。

尚不存在的validator测试文件、可选恢复CI文件及Redis测试目录明确为计划路径；没有将它们当作已存在门禁。

## 未执行

Maven编译/单测/打包、pnpm门禁、浏览器/E2E、真实MySQL/Redis/MinIO/Provider、生产Nginx/TLS及远程CI未运行。视觉、真实锁/租约、跨存储失败恢复与生产发布效果仍需实现阶段验收；本轮不把静态结论升级为运行通过。

## T/P规划校验（2026-09-18）

本次仅运行计划校验和控制器，完整记录见<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/planning-validation.json</Path>，控制器见<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/planning-control.json</Path>。T阶段结构无error；P阶段因校验器误要求父级工件退出1；共享写集warning见计划质量审查的串行裁决。产品测试未运行，原9条基线结果仍是历史事实。control frontier为空是Spec/Goal执行门禁关闭的预期结果，不是产品已经完成。

### P阶段校验限制（本轮实测）

P阶段校验器存在单change路由缺陷：validateParentImplementation以stage==goal-plan无条件要求implementation-map.md/implementation-plan.md，而single-change-plan只要求goal-plan.md。未创建虚假父级工件，也未越范围修改workflow工具。

`--stage tickets`、通用单change校验和Goal v6 JSON Schema可分别验证现有文档；它们不冒充P阶段已通过。P保持可恢复阻塞，解除工具路由问题及上游缺口后重跑原命令。
