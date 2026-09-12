---
schema_version: 3
artifact: ticket
change: 2026-08-28-retire-runtime-code-generator
id: T-04
title: 重建无生成器的当前 OpenAPI 契约
status: done
planning_depth: deep
planning_depth_reason: 删除公开 wire contract 并原子更新不可变 revision、current pointer 与共享生成 TypeScript
ready: true
risk: high
blocked_by: [T-01]
contract_ids: [AC-002, AC-008]
owner: codex:/root
expected_changes: ["<Path>plus-ui-namewta/packages/api-contracts/openapi/revisions/{new-content-hash}/source.json</Path>", "<Path>plus-ui-namewta/packages/api-contracts/openapi/revisions/{new-content-hash}/provenance.json</Path>", "<Path>plus-ui-namewta/packages/api-contracts/openapi/current.json</Path>", "<Path>plus-ui-namewta/packages/api-contracts/generated/openapi.ts</Path>"]
writable_paths: ["<Path>plus-ui-namewta/packages/api-contracts/openapi/revisions/{new-content-hash}/**</Path>", "<Path>plus-ui-namewta/packages/api-contracts/openapi/current.json</Path>", "<Path>plus-ui-namewta/packages/api-contracts/generated/openapi.ts</Path>"]
read_only_paths: ["<Path>plus-ui-namewta/packages/api-contracts/openapi/revisions/612ad27447c60cf330820b30ffa40d2d9eb8c60f4816340e536c6581b313a11d/**</Path>", "<Path>plus-ui-namewta/packages/api-contracts/openapi/revisions/a1f65734eec247e2dc33c42bb017309b48f5ffe0b86a0bab1d176a41c247b50f/**</Path>", "<Path>plus-ui-namewta/tooling/openapi/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/**</Path>"]
shared_paths: ["<Path>plus-ui-namewta/packages/api-contracts/openapi/current.json</Path>", "<Path>plus-ui-namewta/packages/api-contracts/generated/openapi.ts</Path>"]
shared_path_owners: ["<Path>plus-ui-namewta/packages/api-contracts/openapi/current.json</Path> => T-04", "<Path>plus-ui-namewta/packages/api-contracts/generated/openapi.ts</Path> => T-04"]
---

# Ticket T-04: 重建无生成器的当前 OpenAPI 契约

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/ticket/04-refresh-current-openapi-contract.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/evidence/T-04.md</Path>`

## 1. 战略与来源

- **目标：** 从已经完成 T-01 的后端获取新快照，激活不可变 revision 并确定性生成不含生成器的当前 TypeScript 合同。
- **可观察产出：** `current.json`、当前 `source.json` 和 `generated/openapi.ts` 中没有 `/tool/gen*`、生成器 operation 或 `GenTable*`，`openapi:check` 无漂移。
- **来源：** `US-003`、`AC-002`、`AC-008`、`DEC-006`、T-01 的后端接口删除。
- **当前事实：** 当前生成 TypeScript 仍包含多条 `/tool/gen` 路径；工具要求 fetch 保存不可变快照与 40 字符 backend commit，再更新 current 并离线 generate。
- **Planning Depth 原因：** 本 Ticket 收缩公开 wire contract 并更新全前端共享生成文件，历史 revision 还必须保持不可变。

## 2. 决策状态

### 已锁定决策

- T-01 是真实前置：只有后端 endpoint 已删除并形成可引用 commit 后才能获取目标合同。
- 必须运行 tooling fetch/generate/check；不得直接手改 `<Path>plus-ui-namewta/packages/api-contracts/generated/openapi.ts</Path>`。
- 只新增一个内容哈希 revision并原子更新 current；两个既有历史 revision 只读。

### 已采用的低影响假设

- 现有 OpenAPI 工具和后端文档端点可在验收环境运行；若端点不可达，允许从同一已验证 backend commit 的本地快照文件 fetch，但 provenance 必须记录真实来源。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| 新 immutable revision、current pointer、生成 TypeScript 和合同扫描 | `@namewta/tooling-openapi` fetch/generate/check、T-01 后端 commit | 重写历史 revision、直接编辑生成文件、删除前端包或后端接口 |

## 4. 要构建什么

合同消费者运行生成或读取当前 API 包时，只看到目标后端仍支持的接口。fetch 将来自 T-01 后端 commit 的 OpenAPI 保存为新的内容哈希 revision并切换 current，generate 从该已提交来源离线生成 TypeScript。无效/不可达来源必须保留最后可用 current，不能留下半切换状态。

## 5. 实现契约

- **入口或接缝：** `@namewta/tooling-openapi` 的 fetch、generate、check 和 API contracts current pointer。
- **输入与输出：** 输入为 T-01 非空后端 commit 对应的 OpenAPI 3.0/3.1；输出为新 immutable revision、provenance、current pointer 和确定性 TypeScript。
- **公共接口变化：** 当前合同删除全部 `/tool/gen*` operations 和专属 schema；无 deprecated 兼容声明。
- **不变量：** 历史 revisions 字节不变；provenance backend commit 为真实 40 字符 SHA；生成文件仅由工具产出。
- **状态或数据流：** backend snapshot -> validate/hash/provenance -> atomic current -> offline generate -> drift check。
- **错误与失败行为：** fetch 失败或内容无效时 current 不变；check 漂移、生成器残留或 provenance 不一致均阻塞完成。
- **兼容要求：** 不适用：当前合同硬删除旧接口，历史 revision 仅作审计来源。
- **安全与隐私要求：** provenance 不记录凭据或含秘密 URL；工具既有错误脱敏保持。

## 6. 执行路线

1. 确认 T-01 已完成且选择的 backend commit 可复现无生成器 OpenAPI。
2. 运行 `openapi:fetch -- --source <url-or-file> --backend-commit <40-character-sha>`，只新增内容哈希 revision并更新 current。
3. 运行 `openapi:generate` 产生 TypeScript，禁止手工修补生成结果。
4. 扫描 current snapshot/generated 中的生成器路径、operation 和模型，并运行工具测试与 `openapi:check`。
5. 证明既有历史 revision 未改，记录 implementation/source commit 与父分支验证证据。

## 7. 路径访问契约

- **预计修改点：** 一个新内容哈希 revision、current.json 和 generated/openapi.ts。
- **可写范围：** 仅新 revision 占位路径和两个当前合同文件；实际 hash 确定后在 Evidence 记录解析值。
- **只读上下文：** 两个既有历史 revisions、OpenAPI 工具源码与 T-01 后端源码/commit。
- **共享路径：** current pointer 与生成 TypeScript 由 T-04 唯一修改。
- **保留或不动：** 所有既有不可变 revisions 和工具实现；工具缺陷属于偏差而非本 Ticket 默默扩写。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | 确定性合同流 | 在 `<Path>plus-ui-namewta/**</Path>` 运行 filter 后的 `openapi:fetch`、`openapi:generate`、`openapi:check` | 新 revision 激活、生成成功、check 无漂移 | `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/evidence/T-04.md</Path>` |
| 失败路径 | 原子性与残留 | 运行工具现有失败测试；扫描 current/generated 的 `/tool/gen`、`GenTable` 和 generator operation | 无效来源不切换；生成器专属命中为零 | 同上 |
| 回归 | 历史不可变与包门禁 | 比较既有 revision hash/diff；运行 OpenAPI 工具 test/typecheck/build | 历史零 diff，工具与 API contracts 消费通过 | 同上 |

- **Workspace checks：** 按 Goal Plan 在 current workspace 或 source worktree 执行 fetch/generate/check、工具测试和静态扫描。
- **E2E disposition：** not-required：真实后端来源的快照、provenance 和确定性 check 直接覆盖 wire contract；无 UI/数据库边界。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate；无独立 E2E。
- **Integration evidence：** T-01 backend commit、T-04 非空 implementation/source commit、parent before、适用 candidate/result SHA、revision hash 和父分支包含关系。

## 9. 发布、迁移与恢复

- **迁移顺序：** 后端先删除并可取合同；再 fetch 激活、generate、check；前端消费者以同一父分支结果验收。
- **兼容窗口：** 不适用：无旧 contract consumer 兼容。
- **监控信号：** 不适用：无生产发布；drift check 与残留扫描是信号。
- **回滚或前向恢复：** fetch 失败保持旧 current；集成后如发现遗漏，修正后端并新增另一个 immutable revision，不改写已创建 revision。
- **不可逆操作与批准点：** 历史不可变意味着已写 revision 不重写；implementation commit/integration 需 I/Goal Plan 授权。
- **收缩条件：** current snapshot 和 generated contract 的生成器路径、operation、模型为零，历史 revision 保持。

## 10. 验收标准

- [x] `AC-002`：当前 OpenAPI 不含 `/tool/gen*`、生成器 operation 或 `GenTable*` schema。
- [x] `AC-008`：新 revision provenance 有效，current/generated 同步且 `openapi:check` 无漂移。
- [x] 验证矩阵全部记录到 `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/evidence/T-04.md</Path>`。
- [x] 修改未超出 `writable_paths`，shared path 由 T-04 修改，既有 revisions 零 diff。
- [x] 已形成非空 implementation/source commit，父分支验证/result 和包含关系已记录。
- [x] E2E disposition 已执行；未发生未批准偏差；Ticket、Map 和 Evidence 状态一致。
