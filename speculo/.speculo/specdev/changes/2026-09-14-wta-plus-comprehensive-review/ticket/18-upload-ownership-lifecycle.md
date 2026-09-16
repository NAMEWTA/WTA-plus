---
schema_version: 3
plan_contract_version: 1
skill_scan: "已扫描项目 .agents/skills/**/SKILL.md；仅绑定本票命中的入口，执行前按入口继续展开适用 references"
skill_bindings: [{"id":"engineering-standards","path":"<Path>.agents/skills/engineering-standards/SKILL.md</Path>","sha256":"ef0588daecab40e12e49e99d29a12bf107e887ab432e10a790bbc893fc42b0cf","phase":"plan","operation":"read-scope-contract-and-bind-acceptance","inputs":["当前审查候选、Ticket路径和上游ADR"],"outputs":["实现前边界、验证与失败停止条件"],"required":true,"on_failure":"block-ticket"},{"id":"namewta-fullstack-development","path":"<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>","sha256":"dd3e42720caa894bcb31ba1503b8211f669b3bbc2bb9ea3e40590a3a9edea395","phase":"plan","operation":"read-scope-contract-and-bind-acceptance","inputs":["当前审查候选、Ticket路径和上游ADR"],"outputs":["实现前边界、验证与失败停止条件"],"required":true,"on_failure":"block-ticket"}]
resource_claims: ["finding:F-07", "finding:F-11", "contract:AC-018"]
artifact: ticket
change: 2026-09-14-wta-plus-comprehensive-review
id: T-18
title: 明确上传完成、引用移除和导入失败生命周期
status: draft
planning_depth: deep
planning_depth_reason: "本票涉及 F-07, F-11；需先完成证据/高影响取舍，不能直接实现。"
ready: false
risk: high
blocked_by: []
contract_ids: [AC-018]
owner: user-review
expected_changes: ["<Path>frontend/packages/web-kit/file-upload/src/FileUpload.vue</Path>", "<Path>frontend/packages/web-kit/file-upload/src/ImageUpload.vue</Path>", "<Path>frontend/packages/adapters/oss-upload-browser/src/client.ts</Path>", "<Path>frontend/packages/web-domains/system/src/user/UserPage.vue</Path>", "<Path>frontend/packages/web-domains/system/src/runtime.ts</Path>", "<Path>frontend/packages/platform/contracts/src/index.ts</Path>", "<Path>frontend/e2e/</Path>", "<Path>frontend/packages/web-kit/file-upload/src/types.ts</Path>", "<Path>frontend/packages/web-kit/file-upload/src/upload-request.ts</Path>", "<Path>frontend/packages/adapters/oss-upload-browser/src/client.test.ts</Path>", "<Path>frontend/apps/admin-web/src/router/adminManifestRegistry.ts</Path>"]
writable_paths: ["<Path>frontend/packages/web-kit/file-upload/src/FileUpload.vue</Path>", "<Path>frontend/packages/web-kit/file-upload/src/ImageUpload.vue</Path>", "<Path>frontend/packages/adapters/oss-upload-browser/src/client.ts</Path>", "<Path>frontend/packages/web-domains/system/src/user/UserPage.vue</Path>", "<Path>frontend/packages/web-domains/system/src/runtime.ts</Path>", "<Path>frontend/packages/platform/contracts/src/index.ts</Path>", "<Path>frontend/e2e/</Path>", "<Path>frontend/packages/web-kit/file-upload/src/types.ts</Path>", "<Path>frontend/packages/web-kit/file-upload/src/upload-request.ts</Path>", "<Path>frontend/packages/adapters/oss-upload-browser/src/client.test.ts</Path>", "<Path>frontend/apps/admin-web/src/router/adminManifestRegistry.ts</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/</Path>", "<Path>{roots.state}/specdev/adr/</Path>"]
shared_paths: []
shared_path_owners: []
---

# T-18：明确上传完成、引用移除和导入失败生命周期

> 本 Ticket 是 <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/</Path> 内的待审实施计划。它不授权修改产品代码；只有用户明确接受该 Ticket 后，Lead 才能按 project Skill 建立实施 Evidence。

## 1. 战略与来源

- **Finding：** F-07, F-11。详细源码证据见专项审查报告。
- **目标：** 删除真实结构复杂性并恢复可验证的行为/安全/交付合同；不以移动代码、放宽门禁、删除测试或无限兼容层制造绿色。
- **可观察产出：** object URL具备明确dispose owner，导入失败后可再次提交；引用删除合同按真实调用者盘点裁决

## 2. 决策状态

- **当前状态：** draft，ready=false；高影响决策：ADR-CR-007沿用业务owner拥有OSS引用，UploadClient目标形状需根据真实消费者裁决。
- **已锁定：** 本轮只写 change；实现、提交、发布、远程写入和永久知识修改均未授权。
- **未决：** 用户是否接受本票、外部合同/数据迁移/运行环境参数及是否进入下一 Work。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 区分通用表单附件、业务材料引用与OSS管理删除三类调用者；F11已确认object URL无dispose owner，引用删除尚未证明为现存BUG；T14新增材料引用由业务owner定义detach/reconcile合同，上传状态向表单暴露pending/success/error/cancel，不允许未完成就提交；失败保留可重试信息 | 现有模块边界、权限、日志/错误合同中未被本票改变者 | 其他 finding 的实现、无证据的全仓重写、提交/部署、历史数据清理 |

## 4. 行为与实现契约

- **入口或 seam：** <Path>frontend/packages/web-kit/file-upload/</Path>；由该 module 的公开 interface 接受输入。
- **输入/输出：** 输入、错误和状态必须保持对应现有合同；任何外部行为变化先更新 Spec/ADR并阻塞本票。
- **不变量：** 失败关闭、权限/Client上下文、资源所有权、事务/幂等和可观测性按适用工程规则成立。
- **错误：** 导入失败/401/取消后可重新提交

## 5. 执行路线

1. 确认F11已证实的是createObjectURL无dispose owner；当前未证明业务引用删除BUG，不将detach/delete风险当已确认缺陷。
2. 现有表单/OSS管理删除行为先按调用者盘点；不存在业务引用误删路径则记录deferred。T14 self材料接入时由业务owner定义detach/reconcile；组件验证聚焦object URL回收与上传状态，不盲目改变现有UploadClient公共API。
3. 上传状态向表单暴露pending/success/error/cancel，不允许未完成就提交；失败保留可重试信息。
4. 保留globalHeaders来自runtime.uploadHeaders的现有机制，修正创建时token快照、web-domain env URL、error/abort复位与统一HTTP失败语义。
5. 对createObjectURL建立创建/替换/unmount的revoke配对，明确返回URL所有权。
6. 不把业务domain注入web-kit，不建立无真实调用的泛化上传框架。

## 6. 路径访问与所有权

- **可写候选：** <Path>frontend/packages/web-kit/file-upload/src/FileUpload.vue</Path>, <Path>frontend/packages/web-kit/file-upload/src/ImageUpload.vue</Path>, <Path>frontend/packages/adapters/oss-upload-browser/src/client.ts</Path>, <Path>frontend/packages/web-domains/system/src/user/UserPage.vue</Path>, <Path>frontend/packages/web-domains/system/src/runtime.ts</Path>, <Path>frontend/packages/platform/contracts/src/index.ts</Path>, <Path>frontend/e2e/</Path>, <Path>frontend/packages/web-kit/file-upload/src/types.ts</Path>, <Path>frontend/packages/web-kit/file-upload/src/upload-request.ts</Path>, <Path>frontend/packages/adapters/oss-upload-browser/src/client.test.ts</Path>, <Path>frontend/apps/admin-web/src/router/adminManifestRegistry.ts</Path>（仅用户批准后；越界必须停止）。
- **共享路径：** 无；若实现时发现与其他票交集，先由 Lead 串行化并修订 Map。
- **不动：** 供应商/历史archive、永久 namespace、用户未提交改动和生成物，除非本票修订后有明确 owner。

## 7. 验证矩阵

| 行为/风险 | 接缝与方法 | 预期 | Evidence |
|---|---|---|---|
| AC 场景 1 | <Path>frontend/packages/web-kit/file-upload/</Path> 定向测试/静态或隔离运行 | object URL具备明确dispose owner，导入失败后可再次提交；引用删除合同按真实调用者盘点裁决 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-18.md</Path> |
| AC 场景 2 | <Path>frontend/packages/web-kit/file-upload/</Path> 定向测试/静态或隔离运行 | 导入失败/401/取消后可重新提交 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-18.md</Path> |
| AC 场景 3 | <Path>frontend/packages/web-kit/file-upload/</Path> 定向测试/静态或隔离运行 | 未完成上传阻止业务提交且给出原因 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-18.md</Path> |
| AC 场景 4 | <Path>frontend/packages/web-kit/file-upload/</Path> 定向测试/静态或隔离运行 | 本地object URL替换/卸载后全部释放 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-18.md</Path> |
| AC 场景 5 | <Path>frontend/packages/web-kit/file-upload/</Path> 定向测试/静态或隔离运行 | 管理删除仍受后端权限及引用保护 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-18.md</Path> |

- **Workspace checks：** 在frontend目录运行 `pnpm test`、`pnpm typecheck`、`pnpm test:e2e`；OSS引用生命周期由backend owner在实施前补真实测试类与命令，不能把描述当成可执行命令。本轮未运行实现验证。
- **E2E disposition：** required：跨模块/浏览器/外部服务行为必须由 Lead 在 current workspace 或 parent-candidate 验收。

## 8. 迁移、发布与恢复

- **迁移/兼容：** ADR-CR-007沿用业务owner拥有OSS引用，UploadClient目标形状需根据真实消费者裁决。
- **回滚：** 先保留基线 SHA、产物/数据库备份及可验证恢复点；任何不可逆删除、DDL、凭据轮换或发布需独立批准。
- **停止条件：** 证据缺失、路径越界、父分支漂移、权限/安全合同不明、测试未运行或出现未批准偏差时停票。

## 9. 验收标准

- [ ] `AC-018`：object URL具备明确dispose owner，导入失败后可再次提交；引用删除合同按真实调用者盘点裁决。
- [ ] `AC-018`：导入失败/401/取消后可重新提交。
- [ ] `AC-018`：未完成上传阻止业务提交且给出原因。
- [ ] `AC-018`：本地object URL替换/卸载后全部释放。
- [ ] `AC-018`：管理删除仍受后端权限及引用保护。
- [ ] `AC-018`：先盘点引用删除调用者；没有已证实路径则记录deferred，T14接入时由业务owner定义detach/reconcile；组件确认object URL回收和pending/error/cancel状态。
- [ ] Ticket、Map、Spec、ADR 与 Evidence 状态一致；本票保持 draft/ready=false 直到用户接受并完成计划质量复核。
- [ ] 未通过删除测试、关闭类型/安全规则、吞错误、扩大权限或伪造运行证据获得通过。

## 10. Skill 调用计划

- 计划绑定：namewta-fullstack-development。每项入口摘要、references、phase和失败动作以 frontmatter 为最低合同；执行前必须读取当前 Ticket、Map 与实际命中 references。
- 失败动作：必需入口、验证工具或安全/数据证据不可用则 block-ticket；不得以其他工具静默替换。

## 11. 交付与恢复

- **依赖：** 无；完成顺序由 Tickets Map 控制。
- **检查点：** 记录实现前 base SHA、工作树、命令/退出码、偏差、最终 result SHA 及未运行项。
- **完成出口：** 只在用户批准、所有适用 AC/E2E/Evidence 通过、父分支包含结果后进入 done；当前审查阶段不进入 done。

## 12. 最新证据校正与具体检查

F11仅object URL资源泄漏confirmed；detach/delete为T14新增使用前的设计约束，必须通过调用者证据决定接口，不默认新增泛化API。

- object URL替换、移除、卸载均有明确dispose owner，原有管理删除与后端引用保护仍成立。
- 导入失败/401/取消后可重新提交。
- 未完成上传阻止业务提交且给出原因。
- 本地object URL替换/卸载后全部释放。
- 管理删除仍受后端权限及引用保护。

实际命令候选（实施前对照script重新解析，当前not-run）：

- `frontend: pnpm test; pnpm typecheck; pnpm test:e2e`
- 后端引用生命周期：由backend owner在实施前登记真实测试类和命令；当前为needs-evidence，未运行。
