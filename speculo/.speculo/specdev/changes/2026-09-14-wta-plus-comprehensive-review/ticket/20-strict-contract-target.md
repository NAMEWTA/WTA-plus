---
schema_version: 3
plan_contract_version: 1
skill_scan: "已扫描项目 .agents/skills/**/SKILL.md；仅绑定本票命中的入口，执行前按入口继续展开适用 references"
skill_bindings: [{"id":"engineering-standards","path":"<Path>.agents/skills/engineering-standards/SKILL.md</Path>","sha256":"ef0588daecab40e12e49e99d29a12bf107e887ab432e10a790bbc893fc42b0cf","phase":"plan","operation":"read-scope-contract-and-bind-acceptance","inputs":["当前审查候选、Ticket路径和上游ADR"],"outputs":["实现前边界、验证与失败停止条件"],"required":true,"on_failure":"block-ticket"}]
resource_claims: ["finding:F-12", "contract:AC-020"]
artifact: ticket
change: 2026-09-14-wta-plus-comprehensive-review
id: T-20
title: 按边界落实完整TypeScript严格目标
status: draft
planning_depth: deep
planning_depth_reason: "本票涉及 F-12；需先完成证据/高影响取舍，不能直接实现。"
ready: false
risk: high
blocked_by: ["T-12", "T-14", "T-19"]
contract_ids: [AC-020]
owner: user-review
expected_changes: ["<Path>frontend/tsconfig.json</Path>", "<Path>frontend/packages/platform/http/</Path>", "<Path>frontend/packages/adapters/axios-browser/</Path>", "<Path>frontend/packages/adapters/crypto-browser/</Path>", "<Path>frontend/packages/domains/profile/src/person/application/service.ts</Path>", "<Path>frontend/packages/domains/profile/src/enterprise/application/service.ts</Path>", "<Path>frontend/apps/home-web/src/store/</Path>", "<Path>frontend/apps/admin-web/vite/</Path>", "<Path>frontend/.oxlintrc.json</Path>", "<Path>frontend/packages/domains/admin/src/transport.ts</Path>", "<Path>frontend/packages/domains/system/src/transport.ts</Path>", "<Path>frontend/packages/domains/third/src/index.ts</Path>", "<Path>frontend/packages/adapters/oss-upload-browser/src/transport.ts</Path>"]
writable_paths: ["<Path>frontend/tsconfig.json</Path>", "<Path>frontend/packages/platform/http/</Path>", "<Path>frontend/packages/adapters/axios-browser/</Path>", "<Path>frontend/packages/adapters/crypto-browser/</Path>", "<Path>frontend/packages/domains/profile/src/person/application/service.ts</Path>", "<Path>frontend/packages/domains/profile/src/enterprise/application/service.ts</Path>", "<Path>frontend/apps/home-web/src/store/</Path>", "<Path>frontend/apps/admin-web/vite/</Path>", "<Path>frontend/.oxlintrc.json</Path>", "<Path>frontend/packages/domains/admin/src/transport.ts</Path>", "<Path>frontend/packages/domains/system/src/transport.ts</Path>", "<Path>frontend/packages/domains/third/src/index.ts</Path>", "<Path>frontend/packages/adapters/oss-upload-browser/src/transport.ts</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/</Path>", "<Path>{roots.state}/specdev/adr/</Path>"]
shared_paths: []
shared_path_owners: []
---

# T-20：按边界落实完整TypeScript严格目标

> 本 Ticket 是 <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/</Path> 内的待审实施计划。它不授权修改产品代码；只有用户明确接受该 Ticket 后，Lead 才能按 project Skill 建立实施 Evidence。

## 1. 战略与来源

- **Finding：** F-12。详细源码证据见专项审查报告。
- **目标：** 删除真实结构复杂性并恢复可验证的行为/安全/交付合同；不以移动代码、放宽门禁、删除测试或无限兼容层制造绿色。
- **可观察产出：** 目标三个开关为true且全量typecheck通过

## 2. 决策状态

- **当前状态：** draft，ready=false；高影响决策：ADR-CR-008建议本次升级以全严格为终态；迁移中间态Ratchet是施工顺序，不是永久兼容层。
- **已锁定：** 本轮只写 change；实现、提交、发布、远程写入和永久知识修改均未授权。
- **未决：** 用户是否接受本票、外部合同/数据迁移/运行环境参数及是否进入下一 Work。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 记录strictNullChecks/noImplicitAny/strictFunctionTypes真实诊断基线与公开输入类型；优先为user/menu/SSO/OSS/third响应建立unknown narrowing与精确transport映射；逐包消除真实any/cast根因，禁止as unknown as或新ignore抵消门禁 | 现有模块边界、权限、日志/错误合同中未被本票改变者 | 其他 finding 的实现、无证据的全仓重写、提交/部署、历史数据清理 |

## 4. 行为与实现契约

- **入口或 seam：** <Path>frontend/tsconfig.json</Path>；由该 module 的公开 interface 接受输入。
- **输入/输出：** 输入、错误和状态必须保持对应现有合同；任何外部行为变化先更新 Spec/ADR并阻塞本票。
- **不变量：** 失败关闭、权限/Client上下文、资源所有权、事务/幂等和可观测性按适用工程规则成立。
- **错误：** 新边界无无理由any与双cast

## 5. 执行路线

1. 记录strictNullChecks/noImplicitAny/strictFunctionTypes真实诊断基线与公开输入类型。全strict是ADR-CR-008待接受目标，先按真实诊断建立分期ratchet与精确写集；当前候选路径不是全仓类型重写授权。
2. 按user/menu/SSO/OSS/third真实边界parser和transport逐项建立unknown narrowing，先复用已有运行时校验。追加文件需按诊断登记；不一次性重写generated、第三方声明和Vite AST类型。
3. 逐包消除真实any/cast根因，禁止as unknown as或新ignore抵消门禁。
4. 消除存量豁免后启用三个严格开关，若不能一波完成则列明确剩余集合及到期门。
5. 删除已过期axios类型例外仅当新版适配已通过；不盲目升级依赖。

## 6. 路径访问与所有权

- **可写候选：** <Path>frontend/tsconfig.json</Path>, <Path>frontend/packages/platform/http/</Path>, <Path>frontend/packages/adapters/axios-browser/</Path>, <Path>frontend/packages/adapters/crypto-browser/</Path>, <Path>frontend/packages/domains/profile/src/person/application/service.ts</Path>, <Path>frontend/packages/domains/profile/src/enterprise/application/service.ts</Path>, <Path>frontend/apps/home-web/src/store/</Path>, <Path>frontend/apps/admin-web/vite/</Path>, <Path>frontend/.oxlintrc.json</Path>, <Path>frontend/packages/domains/admin/src/transport.ts</Path>, <Path>frontend/packages/domains/system/src/transport.ts</Path>, <Path>frontend/packages/domains/third/src/index.ts</Path>, <Path>frontend/packages/adapters/oss-upload-browser/src/transport.ts</Path>（仅用户批准后；越界必须停止）。
- **共享路径：** 无；若实现时发现与其他票交集，先由 Lead 串行化并修订 Map。
- **不动：** 供应商/历史archive、永久 namespace、用户未提交改动和生成物，除非本票修订后有明确 owner。

- **上游删除裁决：** crypto-browser仅在T11之后仍有获准消费者时纳入类型改造；若T11决定删除则从本票写集移除，禁止为完成类型目标重建已删除适配器。共享http、Home store和transport文件由Lead与T11/T12/T14/T19串行化后更新Map。

## 7. 验证矩阵

| 行为/风险 | 接缝与方法 | 预期 | Evidence |
|---|---|---|---|
| AC 场景 1 | <Path>frontend/tsconfig.json</Path> 定向测试/静态或隔离运行 | 目标三个开关为true且全量typecheck通过 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-20.md</Path> |
| AC 场景 2 | <Path>frontend/tsconfig.json</Path> 定向测试/静态或隔离运行 | 新边界无无理由any与双cast | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-20.md</Path> |
| AC 场景 3 | <Path>frontend/tsconfig.json</Path> 定向测试/静态或隔离运行 | 畸形运行时数据在入口被拒，合法空值语义明确 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-20.md</Path> |
| AC 场景 4 | <Path>frontend/tsconfig.json</Path> 定向测试/静态或隔离运行 | 未通过的包不能被从检查中静默排除 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-20.md</Path> |

- **Workspace checks：** 在frontend目录运行 `pnpm typecheck`（包含architecture:check及 `pnpm -r --if-present typecheck` 的workspace-wide脚本）、`pnpm lint`、`pnpm test`、`pnpm build:prod`。不能将workspace命令表述为仅受影响包通过；若分期验证单包，另列真实filter与未覆盖集合。本轮未运行实现验证。
- **E2E disposition：** required：跨模块/浏览器/外部服务行为必须由 Lead 在 current workspace 或 parent-candidate 验收。

## 8. 迁移、发布与恢复

- **迁移/兼容：** ADR-CR-008建议本次升级以全严格为终态；迁移中间态Ratchet是施工顺序，不是永久兼容层。
- **回滚：** 先保留基线 SHA、产物/数据库备份及可验证恢复点；任何不可逆删除、DDL、凭据轮换或发布需独立批准。
- **停止条件：** 证据缺失、路径越界、父分支漂移、权限/安全合同不明、测试未运行或出现未批准偏差时停票。

## 9. 验收标准

- [ ] `AC-020`：目标三个开关为true且全量typecheck通过。
- [ ] `AC-020`：新边界无无理由any与双cast。
- [ ] `AC-020`：畸形运行时数据在入口被拒，合法空值语义明确。
- [ ] `AC-020`：未通过的包不能被从检查中静默排除。
- [ ] `AC-020`：分期ratchet逐项列边界parser、真实诊断与owner；全strict终态须经ADR接受，不将生成物/第三方类型/全部Vite AST重写作为默认方案。
- [ ] Ticket、Map、Spec、ADR 与 Evidence 状态一致；本票保持 draft/ready=false 直到用户接受并完成计划质量复核。
- [ ] 未通过删除测试、关闭类型/安全规则、吞错误、扩大权限或伪造运行证据获得通过。

## 10. Skill 调用计划

- 计划绑定：engineering-standards。每项入口摘要、references、phase和失败动作以 frontmatter 为最低合同；执行前必须读取当前 Ticket、Map 与实际命中 references。
- 失败动作：必需入口、验证工具或安全/数据证据不可用则 block-ticket；不得以其他工具静默替换。

## 11. 交付与恢复

- **依赖：** T-12, T-14, T-19；完成顺序由 Tickets Map 控制。
- **检查点：** 记录实现前 base SHA、工作树、命令/退出码、偏差、最终 result SHA 及未运行项。
- **完成出口：** 只在用户批准、所有适用 AC/E2E/Evidence 通过、父分支包含结果后进入 done；当前审查阶段不进入 done。

## 12. 最新证据校正与具体检查

ADR-CR-008建议本次升级以全严格为终态；迁移中间态Ratchet是施工顺序，不是永久兼容层。

- 目标三个开关为true且全量typecheck通过。
- 新边界无无理由any与双cast。
- 畸形运行时数据在入口被拒，合法空值语义明确。
- 未通过的包不能被从检查中静默排除。

实际命令候选（实施前对照script重新解析，当前not-run）：

- `frontend: pnpm typecheck; pnpm architecture:check; pnpm lint; pnpm test; pnpm build:prod`
