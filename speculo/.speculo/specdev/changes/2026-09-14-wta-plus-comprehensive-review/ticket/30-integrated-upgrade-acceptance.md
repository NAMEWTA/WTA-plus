---
schema_version: 3
plan_contract_version: 1
skill_scan: "已扫描项目 .agents/skills/**/SKILL.md；仅绑定本票命中的入口，执行前按入口继续展开适用 references"
skill_bindings: [{"id":"engineering-standards","path":"<Path>.agents/skills/engineering-standards/SKILL.md</Path>","sha256":"ef0588daecab40e12e49e99d29a12bf107e887ab432e10a790bbc893fc42b0cf","phase":"plan","operation":"read-scope-contract-and-bind-acceptance","inputs":["当前审查候选、Ticket路径和上游ADR"],"outputs":["实现前边界、验证与失败停止条件"],"required":true,"on_failure":"block-ticket"},{"id":"namewta-fullstack-development","path":"<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>","sha256":"dd3e42720caa894bcb31ba1503b8211f669b3bbc2bb9ea3e40590a3a9edea395","phase":"plan","operation":"read-scope-contract-and-bind-acceptance","inputs":["当前审查候选、Ticket路径和上游ADR"],"outputs":["实现前边界、验证与失败停止条件"],"required":true,"on_failure":"block-ticket"},{"id":"wta-module-guide","path":"<Path>.agents/skills/wta-module-guide/SKILL.md</Path>","sha256":"f2e7e020552795c02285df65a941609fcd93692298951c012adbc555c20ac87f","phase":"plan","operation":"read-scope-contract-and-bind-acceptance","inputs":["当前审查候选、Ticket路径和上游ADR"],"outputs":["实现前边界、验证与失败停止条件"],"required":true,"on_failure":"block-ticket"}]
resource_claims: ["contract:AC-030"]
artifact: ticket
change: 2026-09-14-wta-plus-comprehensive-review
id: T-30
title: 完成升级整体验收与可审查交付
status: draft
planning_depth: deep
planning_depth_reason: "本票涉及 整体验收；需先完成证据/高影响取舍，不能直接实现。"
ready: false
risk: high
blocked_by: ["T-01", "T-02", "T-03", "T-04", "T-05", "T-06", "T-07", "T-08", "T-09", "T-10", "T-11", "T-12", "T-13", "T-14", "T-15", "T-16", "T-17", "T-18", "T-19", "T-20", "T-21", "T-22", "T-23", "T-24", "T-25", "T-26", "T-27", "T-28", "T-29", "T-31"]
contract_ids: [AC-030]
owner: user-review
expected_changes: ["<Path>frontend/e2e/</Path>", "<Path>backend/wta-admin/src/test/</Path>", "<Path>scripts/ci/</Path>", "<Path>release-artifacts/scripts/tests/</Path>"]
writable_paths: ["<Path>frontend/e2e/</Path>", "<Path>backend/wta-admin/src/test/</Path>", "<Path>scripts/ci/</Path>", "<Path>release-artifacts/scripts/tests/</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/</Path>", "<Path>{roots.state}/specdev/adr/</Path>"]
shared_paths: []
shared_path_owners: []
---

# T-30：完成升级整体验收与可审查交付

> 本 Ticket 是 <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/</Path> 内的待审实施计划。它不授权修改产品代码；只有用户明确接受该 Ticket 后，Lead 才能按 project Skill 建立实施 Evidence。

## 1. 战略与来源

- **Finding：** 整体验收与交付。详细源码证据见专项审查报告。
- **目标：** 删除真实结构复杂性并恢复可验证的行为/安全/交付合同；不以移动代码、放宽门禁、删除测试或无限兼容层制造绿色。
- **可观察产出：** 全部已接受AC均有实际命令/退出码/环境/源码checkpoint

## 2. 决策状态

- **当前状态：** draft，ready=false；高影响决策：本票是未来实施后的最终验收，不是当前只写change审查任务已执行的验证。
- **已锁定：** 本轮只写 change；实现、提交、发布、远程写入和永久知识修改均未授权。
- **未决：** 用户是否接受本票、外部合同/数据迁移/运行环境参数及是否进入下一 Work。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 回读所有已接受Ticket与ADR，未接受项明确移出实施范围并保留理由，不默认为完成；在同一候选版本运行前端architecture/lint/typecheck/unit/dev-prod与后端测试/full-core构建；隔离MySQL/Redis/MinIO/双后端验证SSO、Profile材料、workflow动作/权限、通知fence/callback、Third crash/限额、部门树 | 现有模块边界、权限、日志/错误合同中未被本票改变者 | 其他 finding 的实现、无证据的全仓重写、提交/部署、历史数据清理 |

## 4. 行为与实现契约

- **入口或 seam：** <Path>frontend/e2e/</Path>；由该 module 的公开 interface 接受输入。
- **输入/输出：** 输入、错误和状态必须保持对应现有合同；任何外部行为变化先更新 Spec/ADR并阻塞本票。
- **不变量：** 失败关闭、权限/Client上下文、资源所有权、事务/幂等和可观测性按适用工程规则成立。
- **错误：** required E2E与失败注入全部完成，not-run不能被标通过

## 5. 执行路线

1. 回读所有已接受Ticket与ADR，未接受项明确移出实施范围并保留理由，不默认为完成。
2. 在同一候选版本运行前端architecture/lint/typecheck/unit/dev-prod与后端测试/full-core构建。
3. 隔离MySQL/Redis/MinIO/双后端验证SSO、Profile材料、workflow动作/权限、通知fence/callback、Third crash/限额、部门树。
4. 校验release artifact source/digest、stage失败不污染、三Origin Nginx/Compose与UI/a11y。
5. 复核所有旧入口/兼容桥删除映射及重要数据迁移dry-run、恢复演练。
6. 交付完整证据、残余风险和具体发布候选，提交/推送/部署均在用户明确批准后执行。

## 6. 路径访问与所有权

- **可写候选：** <Path>frontend/e2e/</Path>, <Path>backend/wta-admin/src/test/</Path>, <Path>scripts/ci/</Path>, <Path>release-artifacts/scripts/tests/</Path>（仅用户批准后；越界必须停止）。
- **共享路径：** 无；若实现时发现与其他票交集，先由 Lead 串行化并修订 Map。
- **不动：** 供应商/历史archive、永久 namespace、用户未提交改动和生成物，除非本票修订后有明确 owner。

## 7. 验证矩阵

| 行为/风险 | 接缝与方法 | 预期 | Evidence |
|---|---|---|---|
| AC 场景 1 | <Path>frontend/e2e/</Path> 定向测试/静态或隔离运行 | 全部已接受AC均有实际命令/退出码/环境/源码checkpoint | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-30.md</Path> |
| AC 场景 2 | <Path>frontend/e2e/</Path> 定向测试/静态或隔离运行 | required E2E与失败注入全部完成，not-run不能被标通过 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-30.md</Path> |
| AC 场景 3 | <Path>frontend/e2e/</Path> 定向测试/静态或隔离运行 | 未增加安全/类型豁免或删除测试制造绿色 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-30.md</Path> |
| AC 场景 4 | <Path>frontend/e2e/</Path> 定向测试/静态或隔离运行 | 用户能据artifact digest批准明确候选，未自动发布 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-30.md</Path> |

- **Workspace checks：** frontend: pnpm architecture:check; pnpm lint; pnpm typecheck; pnpm test; pnpm test:e2e; pnpm build:dev; pnpm build:prod；backend: ./mvnw test; ./mvnw clean package -DskipTests; ./mvnw clean package -Pbundle-core -Dmaven.test.skip=true；bash release-artifacts/scripts/verify-release.sh; bash scripts/ci/run-external-services.sh。本轮未运行实现验证。
- **E2E disposition：** required：跨模块/浏览器/外部服务行为必须由 Lead 在 current workspace 或 parent-candidate 验收。

## 8. 迁移、发布与恢复

- **迁移/兼容：** 本票是未来实施后的最终验收，不是当前只写change审查任务已执行的验证。
- **回滚：** 先保留基线 SHA、产物/数据库备份及可验证恢复点；任何不可逆删除、DDL、凭据轮换或发布需独立批准。
- **停止条件：** 证据缺失、路径越界、父分支漂移、权限/安全合同不明、测试未运行或出现未批准偏差时停票。

## 9. 验收标准

- [ ] `AC-030`：全部已接受AC均有实际命令/退出码/环境/源码checkpoint。
- [ ] `AC-030`：required E2E与失败注入全部完成，not-run不能被标通过。
- [ ] `AC-030`：未增加安全/类型豁免或删除测试制造绿色。
- [ ] `AC-030`：用户能据artifact digest批准明确候选，未自动发布。
- [ ] Ticket、Map、Spec、ADR 与 Evidence 状态一致；本票保持 draft/ready=false 直到用户接受并完成计划质量复核。
- [ ] 未通过删除测试、关闭类型/安全规则、吞错误、扩大权限或伪造运行证据获得通过。

## 10. Skill 调用计划

- 计划绑定：namewta-fullstack-development, wta-module-guide。每项入口摘要、references、phase和失败动作以 frontmatter 为最低合同；执行前必须读取当前 Ticket、Map 与实际命中 references。
- 失败动作：必需入口、验证工具或安全/数据证据不可用则 block-ticket；不得以其他工具静默替换。

## 11. 交付与恢复

- **依赖：** T-29；完成顺序由 Tickets Map 控制。
- **检查点：** 记录实现前 base SHA、工作树、命令/退出码、偏差、最终 result SHA 及未运行项。
- **完成出口：** 只在用户批准、所有适用 AC/E2E/Evidence 通过、父分支包含结果后进入 done；当前审查阶段不进入 done。

## 12. 最新证据校正与具体检查

本票是未来实施后的最终验收，不是当前只写change审查任务已执行的验证。

- 全部已接受AC均有实际命令/退出码/环境/源码checkpoint。
- required E2E与失败注入全部完成，not-run不能被标通过。
- 未增加安全/类型豁免或删除测试制造绿色。
- 用户能据artifact digest批准明确候选，未自动发布。

实际命令候选（实施前对照script重新解析，当前not-run）：

- `frontend: pnpm architecture:check; pnpm lint; pnpm typecheck; pnpm test; pnpm test:e2e; pnpm build:dev; pnpm build:prod`
- `backend: ./mvnw test; ./mvnw clean package -DskipTests; ./mvnw clean package -Pbundle-core -Dmaven.test.skip=true`
- `bash release-artifacts/scripts/verify-release.sh; bash scripts/ci/run-external-services.sh`
