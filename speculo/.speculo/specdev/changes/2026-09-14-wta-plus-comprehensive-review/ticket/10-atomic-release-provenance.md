---
schema_version: 3
plan_contract_version: 1
skill_scan: "已扫描项目 .agents/skills/**/SKILL.md；仅绑定本票命中的入口，执行前按入口继续展开适用 references"
skill_bindings: [{"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "ef0588daecab40e12e49e99d29a12bf107e887ab432e10a790bbc893fc42b0cf", "phase": "plan", "operation": "read-scope-contract-and-bind-acceptance", "inputs": ["当前审查候选、Ticket路径和上游ADR"], "outputs": ["实现前边界、验证与失败停止条件"], "required": true, "on_failure": "block-ticket"}]
resource_claims: ["finding:D-03", "finding:D-10", "contract:AC-010"]
artifact: ticket
change: 2026-09-14-wta-plus-comprehensive-review
id: T-10
title: 删除混源局部发布并实现原子stage
status: draft
planning_depth: deep
planning_depth_reason: "本票涉及 D-03, D-10；需先完成证据/高影响取舍，不能直接实现。"
ready: false
risk: high
blocked_by: ["T-09"]
contract_ids: [AC-010]
owner: user-review
expected_changes: ["<Path>release-artifacts/scripts/release-manage.sh</Path>", "<Path>release-artifacts/scripts/verify-release.sh</Path>", "<Path>release-artifacts/tests/</Path>", "<Path>release-artifacts/README.md</Path>"]
writable_paths: ["<Path>release-artifacts/scripts/release-manage.sh</Path>", "<Path>release-artifacts/scripts/verify-release.sh</Path>", "<Path>release-artifacts/tests/</Path>", "<Path>release-artifacts/README.md</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/</Path>", "<Path>{roots.state}/specdev/adr/</Path>"]
shared_paths: ["<Path>release-artifacts/scripts/release-manage.sh</Path>", "<Path>release-artifacts/tests/</Path>", "<Path>release-artifacts/README.md</Path>"]
shared_path_owners: ["<Path>release-artifacts/scripts/release-manage.sh</Path> => Lead", "<Path>release-artifacts/tests/</Path> => Lead", "<Path>release-artifacts/README.md</Path> => Lead"]
---

# T-10：删除混源局部发布并实现原子stage

> 本 Ticket 是 <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/</Path> 内的待审实施计划。它不授权修改产品代码；只有用户明确接受该 Ticket 后，Lead 才能按 project Skill 建立实施 Evidence。

## 1. 战略与来源

- **Finding：** D-03, D-10。详细源码证据见专项审查报告。
- **目标：** 删除真实结构复杂性并恢复可验证的行为/安全/交付合同；不以移动代码、放宽门禁、删除测试或无限兼容层制造绿色。
- **可观察产出：** manifest每个artifact的digest和source可追溯

## 2. 决策状态

- **当前状态：** draft，ready=false；高影响决策：ADR-CR-005推荐取消partial deployable promotion；若保留混源组合需额外复杂合同，当前不推荐。
- **已锁定：** 本轮只写 change；实现、提交、发布、远程写入和永久知识修改均未授权。
- **未决：** 用户是否接受本票、外部合同/数据迁移/运行环境参数及是否进入下一 Work。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 固定旧core后端+新frontend被标当前full/HEAD的红灯；把frontend-only/backend-only产物降为不可发布cache；完整release显式选择所有artifact及source SHA/digest；删current复制后局部覆盖仍可promotion的路径 | 现有模块边界、权限、日志/错误合同中未被本票改变者 | 其他 finding 的实现、无证据的全仓重写、提交/部署、历史数据清理 |

## 4. 行为与实现契约

- **入口或 seam：** <Path>release-artifacts/scripts/release-manage.sh</Path>；由该 module 的公开 interface 接受输入。
- **输入/输出：** 输入、错误和状态必须保持对应现有合同；任何外部行为变化先更新 Spec/ADR并阻塞本票。
- **不变量：** 失败关闭、权限/Client上下文、资源所有权、事务/幂等和可观测性按适用工程规则成立。
- **错误：** 缺模板/坏SQL/坏JAR/中断时current/context SHA256完全不变

## 5. 执行路线

1. 固定旧core后端+新frontend被标当前full/HEAD的红灯。
2. 把frontend-only/backend-only产物降为不可发布cache；完整release显式选择所有artifact及source SHA/digest。
3. 删current复制后局部覆盖仍可promotion的路径。
4. 所有App、JAR、SQL、Nginx、manifest在同一临时stage校验后再单次promotion；多个独立目录逐次rename不等于单次原子提交，须证明Linux/Windows及Docker bind mount读者只见完整版本。
5. 异常只清理本次受控stage，已发布current/context保持字节不变。
6. 增加失败注入、混源拒绝和恢复点；原子目录切换能力在Windows/Linux分别核验。

## 6. 路径访问与所有权

- **可写候选：** frontmatter的writable_paths为精确实施边界；当前仍只写change。T-29同时受cleanup-plan逐文件分类约束，KEEP文件不能因出现在写集便直接删除。
- **共享路径：** frontmatter列明的交集由Lead唯一整合。T-01 → T-09 → T-10 → T-08按依赖串行，T-29收敛文档；发现额外交集时先更新Map，不能各自覆盖同一脚本。
- **不动：** 供应商/历史archive、永久 namespace、用户未提交改动和生成物，除非本票修订后有明确 owner。

## 7. 验证矩阵

| 行为/风险 | 接缝与方法 | 预期 | Evidence |
|---|---|---|---|
| AC 场景 1 | <Path>release-artifacts/scripts/release-manage.sh</Path> 定向测试/静态或隔离运行 | manifest每个artifact的digest和source可追溯 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-10.md</Path> |
| AC 场景 2 | <Path>release-artifacts/scripts/release-manage.sh</Path> 定向测试/静态或隔离运行 | 缺模板/坏SQL/坏JAR/中断时current/context SHA256完全不变 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-10.md</Path> |
| AC 场景 3 | <Path>release-artifacts/scripts/release-manage.sh</Path> 定向测试/静态或隔离运行 | 单目标构建不能stage为完整release | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-10.md</Path> |
| AC 场景 4 | <Path>release-artifacts/scripts/release-manage.sh</Path> 定向测试/静态或隔离运行 | 恢复只操作本次stage，不触及他人文件或历史发布 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-10.md</Path> |

- **Workspace checks（当前 not-run）：** cwd `<Path>.</Path>`：`bash -n release-artifacts/scripts/release-manage.sh`、`bash release-artifacts/scripts/verify-release.sh`。实际测试根为<Path>release-artifacts/tests/</Path>；新增provenance/atomic-stage测试后才可使用精确node --test命令。失败注入须比较stable current/context前后SHA；真实build/stage/bundle只在批准隔离candidate运行。
- **E2E disposition：** required：跨模块/浏览器/外部服务行为必须由 Lead 在 current workspace 或 parent-candidate 验收。

## 8. 迁移、发布与恢复

- **迁移/兼容：** ADR-CR-005推荐取消partial deployable promotion；若保留混源组合需额外复杂合同，当前不推荐。
- **回滚：** 先保留基线 SHA、产物/数据库备份及可验证恢复点；任何不可逆删除、DDL、凭据轮换或发布需独立批准。
- **停止条件：** 证据缺失、路径越界、父分支漂移、权限/安全合同不明、测试未运行或出现未批准偏差时停票。

## 9. 验收标准

- [ ] `AC-010`：manifest每个artifact的digest和source可追溯。
- [ ] `AC-010`：缺模板/坏SQL/坏JAR/中断时current/context SHA256完全不变。
- [ ] `AC-010`：单目标构建不能stage为完整release。
- [ ] `AC-010`：恢复只操作本次stage，不触及他人文件或历史发布。
- [ ] Ticket、Map、Spec、ADR 与 Evidence 状态一致；本票保持 draft/ready=false 直到用户接受并完成计划质量复核。
- [ ] 未通过删除测试、关闭类型/安全规则、吞错误、扩大权限或伪造运行证据获得通过。

## 10. Skill 调用计划

- 计划绑定：以frontmatter的skill_bindings为完整集合，进入实现前重新读取真实Skill入口；T-01的全栈Skill用于模块模式validator，T-09用于App与构建合同。新路径或Skill先由Lead同步Map。
- 失败动作：必需入口、验证工具或安全/数据证据不可用则 block-ticket；不得以其他工具静默替换。

## 11. 交付与恢复

- **依赖：** T-09；完成顺序由 Tickets Map 控制。
- **检查点：** 记录实现前 base SHA、工作树、命令/退出码、偏差、最终 result SHA 及未运行项。
- **完成出口：** 只在用户批准、所有适用 AC/E2E/Evidence 通过、父分支包含结果后进入 done；当前审查阶段不进入 done。

## 12. 验证与范围校正

真实测试目录为<Path>release-artifacts/tests/</Path>，不存在scripts/tests。新provenance/atomic-stage夹具创建后才有对应node --test命令，不能用自然语言充当门禁。默认release输入为干净source snapshot；若接受dirty候选必须记录完整内容digest，不能只写HEAD。单目标cache绝不可进入deployable current。完整stage同卷promotion要验证读者原子可见性，逐目录rename不成立；异常/SIGTERM/SIGKILL恢复只处理本次owner目录。已有SQL只读校验，不创建第二套基座。

本票仍为draft / ready=false；实施验证not-run。恢复时按Map → 适用Project Skill → Ticket → 源码证据读取。
