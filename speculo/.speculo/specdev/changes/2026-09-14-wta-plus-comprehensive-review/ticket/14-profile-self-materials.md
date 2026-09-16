---
schema_version: 3
plan_contract_version: 1
skill_scan: "已扫描项目 .agents/skills/**/SKILL.md；仅绑定本票命中的入口，执行前按入口继续展开适用 references"
skill_bindings: [{"id":"engineering-standards","path":"<Path>.agents/skills/engineering-standards/SKILL.md</Path>","sha256":"ef0588daecab40e12e49e99d29a12bf107e887ab432e10a790bbc893fc42b0cf","phase":"plan","operation":"read-scope-contract-and-bind-acceptance","inputs":["当前审查候选、Ticket路径和上游ADR"],"outputs":["实现前边界、验证与失败停止条件"],"required":true,"on_failure":"block-ticket"},{"id":"namewta-fullstack-development","path":"<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>","sha256":"dd3e42720caa894bcb31ba1503b8211f669b3bbc2bb9ea3e40590a3a9edea395","phase":"plan","operation":"read-scope-contract-and-bind-acceptance","inputs":["当前审查候选、Ticket路径和上游ADR"],"outputs":["实现前边界、验证与失败停止条件"],"required":true,"on_failure":"block-ticket"}]
resource_claims: ["finding:F-02", "contract:AC-014"]
artifact: ticket
change: 2026-09-14-wta-plus-comprehensive-review
id: T-14
title: 补齐个人与企业自助认证材料闭环
status: draft
planning_depth: deep
planning_depth_reason: "本票涉及 F-02；需先完成证据/高影响取舍，不能直接实现。"
ready: false
risk: high
blocked_by: ["T-18"]
contract_ids: [AC-014]
owner: user-review
expected_changes: ["<Path>frontend/packages/web-domains/profile/src/self/</Path>", "<Path>frontend/packages/web-domains/profile/src/self/runtime.ts</Path>", "<Path>frontend/packages/domains/profile/src/person/application/service.ts</Path>", "<Path>frontend/packages/domains/profile/src/enterprise/application/service.ts</Path>", "<Path>frontend/apps/home-web/src/router/homeManifestRegistry.ts</Path>", "<Path>backend/wta-modules/wta-profile/</Path>", "<Path>frontend/e2e/profile-management.spec.ts</Path>", "<Path>frontend/apps/home-web/src/application/services.ts</Path>", "<Path>frontend/apps/home-web/package.json</Path>"]
writable_paths: ["<Path>frontend/packages/web-domains/profile/src/self/</Path>", "<Path>frontend/packages/web-domains/profile/src/self/runtime.ts</Path>", "<Path>frontend/packages/domains/profile/src/person/application/service.ts</Path>", "<Path>frontend/packages/domains/profile/src/enterprise/application/service.ts</Path>", "<Path>frontend/apps/home-web/src/router/homeManifestRegistry.ts</Path>", "<Path>backend/wta-modules/wta-profile/</Path>", "<Path>frontend/e2e/profile-management.spec.ts</Path>", "<Path>frontend/apps/home-web/src/application/services.ts</Path>", "<Path>frontend/apps/home-web/package.json</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/</Path>", "<Path>{roots.state}/specdev/adr/</Path>"]
shared_paths: []
shared_path_owners: []
---

# T-14：补齐个人与企业自助认证材料闭环

> 本 Ticket 是 <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/</Path> 内的待审实施计划。它不授权修改产品代码；只有用户明确接受该 Ticket 后，Lead 才能按 project Skill 建立实施 Evidence。

## 1. 战略与来源

- **Finding：** F-02。详细源码证据见专项审查报告。
- **目标：** 删除真实结构复杂性并恢复可验证的行为/安全/交付合同；不以移动代码、放宽门禁、删除测试或无限兼容层制造绿色。
- **可观察产出：** 新个人CN_RESIDENT_ID上传正反面后完成提交

## 2. 决策状态

- **当前状态：** draft，ready=false；高影响决策：沿用现有Profile材料归属与数据库规则，不删除后端门禁换取页面成功。
- **已锁定：** 本轮只写 change；实现、提交、发布、远程写入和永久知识修改均未授权。
- **未决：** 用户是否接受本票、外部合同/数据迁移/运行环境参数及是否进入下一 Work。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 从材料目录与基座规则读取必填tag，确认person/enterprise状态及owner；复用现有材料domain/OSS上传能力，向self runtime显式注入，不复制管理页面；实现材料列表、上传进度/完成、预览、替换、删除引用及tag级错误 | 现有模块边界、权限、日志/错误合同中未被本票改变者 | 其他 finding 的实现、无证据的全仓重写、提交/部署、历史数据清理 |

## 4. 行为与实现契约

- **入口或 seam：** <Path>frontend/packages/web-domains/profile/src/self/</Path>；由该 module 的公开 interface 接受输入。
- **输入/输出：** 输入、错误和状态必须保持对应现有合同；任何外部行为变化先更新 Spec/ADR并阻塞本票。
- **不变量：** 失败关闭、权限/Client上下文、资源所有权、事务/幂等和可观测性按适用工程规则成立。
- **错误：** 企业必填及条件材料齐备时完成提交，缺项定位准确

## 5. 执行路线

1. 从材料目录与基座规则读取必填tag，确认person/enterprise状态及owner。
2. 在self/runtime.ts与Home homeManifestRegistry.ts显式注入fileUpload、material tree及业务owner能力；web-domain-profile通过runtime消费上传端口，不新增假OSS依赖或复制管理端页面。
3. 实现材料列表、上传进度/完成、预览、替换、删除引用及tag级错误。
4. 上传完成并经业务owner登记后才可submit；草稿保存/刷新可恢复。
5. 按后端材料目录保留完整必填门禁：个人CN_RESIDENT_ID人像/国徽两面；企业营业执照、法人身份证明；非法人经办人条件触发授权委托书。后端MISSING_REQUIRED_MATERIAL映射到具体tag，不关闭校验。
6. 提交期间只锁必要动作，失败保留草稿；审核通过后的只读/修订语义沿用后端。

## 6. 路径访问与所有权

- **可写候选：** <Path>frontend/packages/web-domains/profile/src/self/</Path>, <Path>frontend/packages/web-domains/profile/src/self/runtime.ts</Path>, <Path>frontend/packages/domains/profile/src/person/application/service.ts</Path>, <Path>frontend/packages/domains/profile/src/enterprise/application/service.ts</Path>, <Path>frontend/apps/home-web/src/router/homeManifestRegistry.ts</Path>, <Path>backend/wta-modules/wta-profile/</Path>, <Path>frontend/e2e/profile-management.spec.ts</Path>, <Path>frontend/apps/home-web/src/application/services.ts</Path>, <Path>frontend/apps/home-web/package.json</Path>（仅用户批准后；越界必须停止）。
- **共享路径：** 无；若实现时发现与其他票交集，先由 Lead 串行化并修订 Map。
- **不动：** 供应商/历史archive、永久 namespace、用户未提交改动和生成物，除非本票修订后有明确 owner。

## 7. 验证矩阵

| 行为/风险 | 接缝与方法 | 预期 | Evidence |
|---|---|---|---|
| AC 场景 1 | <Path>frontend/packages/web-domains/profile/src/self/</Path> 定向测试/静态或隔离运行 | 新个人CN_RESIDENT_ID上传正反面后完成提交 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-14.md</Path> |
| AC 场景 2 | <Path>frontend/packages/web-domains/profile/src/self/</Path> 定向测试/静态或隔离运行 | 企业必填及条件材料齐备时完成提交，缺项定位准确 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-14.md</Path> |
| AC 场景 3 | <Path>frontend/packages/web-domains/profile/src/self/</Path> 定向测试/静态或隔离运行 | 取消/失败/过期OSS/刷新不会伪造完成或越owner访问 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-14.md</Path> |
| AC 场景 4 | <Path>frontend/packages/web-domains/profile/src/self/</Path> 定向测试/静态或隔离运行 | 后端必填校验不被关闭，真实浏览器+MySQL+OSS验收通过 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-14.md</Path> |

- **Workspace checks：** 在frontend目录运行 `pnpm --filter @namewta/web-domain-profile test`、`pnpm typecheck`、`pnpm exec playwright test e2e/profile-management.spec.ts`；在backend目录运行 `./mvnw -pl wta-modules/wta-profile -am test`。现有profile-management是管理端测试，Home self材料旅程需新增明确入口用例并使用真实MySQL/OSS验收。本轮未运行实现验证。
- **E2E disposition：** required：跨模块/浏览器/外部服务行为必须由 Lead 在 current workspace 或 parent-candidate 验收。

## 8. 迁移、发布与恢复

- **迁移/兼容：** 沿用现有Profile材料归属与数据库规则，不删除后端门禁换取页面成功。
- **回滚：** 先保留基线 SHA、产物/数据库备份及可验证恢复点；任何不可逆删除、DDL、凭据轮换或发布需独立批准。
- **停止条件：** 证据缺失、路径越界、父分支漂移、权限/安全合同不明、测试未运行或出现未批准偏差时停票。

## 9. 验收标准

- [ ] `AC-014`：新个人CN_RESIDENT_ID上传正反面后完成提交。
- [ ] `AC-014`：企业必填及条件材料齐备时完成提交，缺项定位准确。
- [ ] `AC-014`：取消/失败/过期OSS/刷新不会伪造完成或越owner访问。
- [ ] `AC-014`：后端必填校验不被关闭，真实浏览器+MySQL+OSS验收通过。
- [ ] `AC-014`：self runtime的fileUpload、material tree与业务owner由Home显式组合；材料tag覆盖个人证件两面、企业营业执照和法人证件、非法人经办授权书条件。
- [ ] Ticket、Map、Spec、ADR 与 Evidence 状态一致；本票保持 draft/ready=false 直到用户接受并完成计划质量复核。
- [ ] 未通过删除测试、关闭类型/安全规则、吞错误、扩大权限或伪造运行证据获得通过。

## 10. Skill 调用计划

- 计划绑定：namewta-fullstack-development。每项入口摘要、references、phase和失败动作以 frontmatter 为最低合同；执行前必须读取当前 Ticket、Map 与实际命中 references。
- 失败动作：必需入口、验证工具或安全/数据证据不可用则 block-ticket；不得以其他工具静默替换。

## 11. 交付与恢复

- **依赖：** T-18；完成顺序由 Tickets Map 控制。
- **检查点：** 记录实现前 base SHA、工作树、命令/退出码、偏差、最终 result SHA 及未运行项。
- **完成出口：** 只在用户批准、所有适用 AC/E2E/Evidence 通过、父分支包含结果后进入 done；当前审查阶段不进入 done。

## 12. 最新证据校正与具体检查

沿用现有Profile材料归属与数据库规则，不删除后端门禁换取页面成功。

- 新个人CN_RESIDENT_ID上传正反面后完成提交。
- 企业必填及条件材料齐备时完成提交，缺项定位准确。
- 取消/失败/过期OSS/刷新不会伪造完成或越owner访问。
- 后端必填校验不被关闭，真实浏览器+MySQL+OSS验收通过。

实际命令候选（实施前对照script重新解析，当前not-run）：

- `frontend: pnpm --filter @namewta/web-domain-profile test; pnpm typecheck; pnpm exec playwright test e2e/profile-management.spec.ts`
- `backend: ./mvnw -pl wta-modules/wta-profile -am test`
