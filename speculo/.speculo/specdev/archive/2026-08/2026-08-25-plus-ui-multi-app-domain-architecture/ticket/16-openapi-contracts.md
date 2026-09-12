---
schema_version: 3
artifact: ticket
change: 2026-08-25-plus-ui-multi-app-domain-architecture
id: T-16
title: 引入 OpenAPI transport 合同生成与漂移检查
status: done
planning_depth: deep
planning_depth_reason: 生成物会影响全部 domain transport types，需要锁定来源、可重复命令、映射边界、漂移 Gate 和批量兼容迁移
ready: true
risk: high
blocked_by: [T-15]
contract_ids: [AC-027, AC-028]
owner: codex:/root
expected_changes: ["<Path>plus-ui-namewta/packages/api-contracts/**</Path>", "<Path>plus-ui-namewta/tooling/openapi/**</Path>", "<Path>plus-ui-namewta/tooling/architecture/**</Path>", "<Path>plus-ui-namewta/packages/domains/**</Path>", "<Path>plus-ui-namewta/.oxfmtrc.json</Path>", "<Path>plus-ui-namewta/pnpm-lock.yaml</Path>"]
writable_paths: ["<Path>plus-ui-namewta/packages/api-contracts/**</Path>", "<Path>plus-ui-namewta/tooling/openapi/**</Path>", "<Path>plus-ui-namewta/tooling/architecture/**</Path>", "<Path>plus-ui-namewta/packages/domains/**</Path>", "<Path>plus-ui-namewta/.oxfmtrc.json</Path>", "<Path>plus-ui-namewta/pnpm-lock.yaml</Path>"]
read_only_paths: ["<Path>ruoyi-admin/**</Path>", "<Path>ruoyi-modules/**</Path>", "<Path>plus-ui-namewta/apps/**</Path>", "<Path>plus-ui-namewta/package.json</Path>"]
shared_paths: ["<Path>plus-ui-namewta/packages/api-contracts/**</Path>"]
shared_path_owners: ["<Path>plus-ui-namewta/packages/api-contracts/**</Path> => T-16"]
---

# Ticket T-16: 引入 OpenAPI transport 合同生成与漂移检查

- **工件：** Ticket `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/ticket/16-openapi-contracts.md</Path>`；Map `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/tickets-map.md</Path>`；Spec `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/spec.md</Path>`；Evidence `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-16.md</Path>`。

## 1. 战略与来源

- **目标/产出：** 从明确后端 OpenAPI 来源可重复生成 private `api-contracts`，检查漂移，并让 domain 通过映射层消费 transport schema 而不替代业务模型。
- **来源：** `US-004`、`US-011`、`AC-027`、`AC-028`、`ADR-001`、`ADR-002`、`ADR-006`。
- **当前事实：** 前序迁移刻意继续手工 transport types，避免等待生成；现在所有 domain 边界已稳定。
- **Planning Depth 原因：** 生成合同横跨七领域且生成物/业务模型混淆会放大后端 schema 变化。

## 2. 决策状态

### 已锁定决策

- `api-contracts` 只承载生成 transport schema/client primitives；domain 保留语义模型、use cases 和 DTO 映射。
- 生成来源、版本/命令、格式化和 drift check 必须固定；手工编辑生成物被检查拒绝。
- 按 domain 批次迁移且每批绿色，不一次性替换业务类型。
- `DEV-T16-001`：T16 可激活 `api-contracts` 与 `tooling/openapi` package manifests，在七个 domain manifests 中只增加实际消费的 `@namewta/api-contracts: workspace:*`，固定唯一外部生成器 `openapi-typescript@7.13.0`，并机械更新对应 lock importers/nodes。根 package/scripts、workspace/catalog、apps、web-domains、其他依赖与既有 resolutions 保持只读；生成/检查入口使用 package-local scripts。
- `DEV-T16-002`：初轮双轴审查证明 provenance 未进入 fetch/generate/check 合同、根格式化会改写 raw/generated 文件、mapper 尚未进入真实 HTTP 响应链路，且生成器在不支持的 TypeScript 6 peer 下运行。T16 可为 raw snapshot/generated output 增加两个精确 Oxfmt ignore，在 tooling package 本地固定 `typescript@5.9.3`，并整改 provenance 原子更新与校验、source URL 脱敏、临时目录清理、七领域生产 mapper 链路及对应 README 依赖声明。App、根 scripts/package、workspace/catalog、web-domains、其他根配置和公共业务模型保持不变。
- `DEV-T16-003`：完整 architecture Gate 证明 `DEV-T16-002` 的 generator-local TypeScript 5.9.3 与共享 catalog TypeScript 6.0.3 ratchet 冲突。Architecture tooling 只可增加精确匹配 `@namewta/tooling-openapi` / `devDependencies` / `typescript` / `5.9.3` 的例外及正反单测；其他包、字段、版本和 catalog 规则继续严格失败。
- `DEV-T16-004`：第二轮标准轴发现 workflow/operations mapper 仍以双断言展开 generated DTO、大小写 HTTP scheme 可绕过脱敏，且双文件顺序 rename 不具备真正原子提交语义。T16 必须改为字段级、可验证的 domain projection；统一用 URL parser 按规范化 protocol 分流；source/provenance 存入 immutable digest revision，并以单一原子 `current.json` pointer 激活，禁止继续宣称 best-effort rollback 为原子事务。
- `DEV-T16-005`：第三轮 fixed-point 审查发现分页 `data/rows` 缺失仍可绕过 projection、raw-only revision 无法精确绑定 provenance，且可由 URL parser 规范化的 `HTTPS:/` 输入会落入文件路径错误。T16 必须移除 workflow/operations 剩余双断言并显式归一化缺失页数据；revision 身份改为 raw bytes 与 canonical provenance 的 bundle digest；HTTP(S) scheme 分流覆盖单斜杠和大小写形式，并以合法 provenance 字段篡改、同 raw 不同 backend commit、缺失 page data 与 URL 凭据/query 反向测试锁定。
- `DEV-T16-006`：第四轮规格轴发现 WHATWG 会把带前导空白的 `HTTPS:/` 规范化为网络 URL，而 scheme-first 正则仍会把它当本地路径并回显 secret。T16 必须先用 URL parser 分类可解析输入；仅解析失败时才以去除前导空白后的 scheme 判断“无效 HTTP URL”并返回固定错误。反向测试覆盖标准、单斜杠、前导空格和前导 Tab；实现不得以违反 Oxlint `no-control-regex` 的控制字符正则绕过门禁。
- `DEV-T16-007`：第五轮规格轴发现脱敏顶层 message 仍通过 `Error.cause` 暴露底层 URL/fetch 的 credential/query。T16 必须在 HTTP URL parse/fetch 安全错误上丢弃不可信原始 cause，含 userinfo 的 HTTP(S) URL 必须在分类阶段以脱敏 host/path fail-fast；测试必须检查完整常规错误展开而非只检查 `message`。

### 已采用的低影响假设

- 具体生成器基于实际 OpenAPI 文档与现有 Node 工具链选择，但必须锁版本并产出确定性 diff。
- 权威 OpenAPI endpoint 或文件路径是执行时通过后端配置和生成文档发现的代码库事实；若来源不存在，执行者记录 deviation 并停止生成，不伪造 schema。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| generator/config/source provenance、api-contracts、domain transport mappings、drift tests | 稳定 domain models/services、后端 OpenAPI | 后端注解修复、业务模型生成、公开 npm 发布 |

## 4. 要构建什么

开发者运行固定 generate/check 命令得到确定生成物；后端合同变化产生可审查 diff，未重生成或手改会失败。Domain 将 transport schema 映射为既有业务模型，两个 App 行为和类型检查保持。

## 5. 实现契约

- **入口/输入输出：** `openapi:generate`/`openapi:check`、api-contracts exports、domain mapper；OpenAPI 输入，确定 TS schema 与 drift 状态输出。
- **公共接口变化：** 新 api-contracts private exports；domain public API 尽量不变。
- **不变量/数据流：** backend spec -> pinned generator -> generated transport -> domain mapper -> business model；生成层不依赖 domain。
- **失败行为：** 来源不可达、schema invalid、非确定输出、未提交 drift 均非零且不覆盖最后已知良好生成物。
- **兼容/安全：** 不抓取需 secret 的生产 endpoint；生成物不得包含 credentials；每领域可回退手工 transport mapping。

## 6. 执行路线

1. 发现并记录权威 OpenAPI 来源、版本和覆盖范围，先验证可重复获取。
2. 建立 pinned generator、格式化、provenance README 和 drift check。
3. 生成 api-contracts 并验证公开 exports/无反向依赖。
4. 按 demo/identity/workflow/system/AI/devtools/operations 批次加入 mapper，每批 type/API tests 绿色。
5. 验证 clean regenerate、intentional drift、manual edit failure 和双 App build。
6. 记录恢复策略和 Gate I 后形成 commit。

## 7. 路径访问契约

- **可写：** api-contracts、openapi tooling、domain transport/mappers，以及仅含 raw/generated 精确 ignore 的 `.oxfmtrc.json`；**只读：** 后端来源、apps、其余 T-03 root config。
- **共享路径：** api-contracts 唯一 owner `T-16`；所有 domain 批次在同一 Ticket 串行完成。
- **保留或不动：** domain 业务模型/public use cases、后端、App UI。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | generation/type | clean generate + check + typecheck | 确定生成、无 drift、映射通过 | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-16.md</Path>` |
| 失败路径 | drift fixtures | schema 改动、手改、来源失败 | 非零且保留 last-known-good | 同上 |
| 回归 | API tests/build | domain tests + 双 App build | 业务模型/API 行为不变 | 同上 |

- **Workspace checks：** source-worktree/current-workspace 运行 generate/check、architecture、typecheck、unit/API tests、双 build。
- **E2E disposition：** not-required：不改变已验证 UI/路由行为，以确定生成、类型、API tests 和双 build 覆盖 transport 风险。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate；不新增场景，必要时复用 T-15 smoke 作为集成观察。
- **Integration evidence：** source commit、parent before、candidate/result SHA、生成器版本、source digest 和 Gate I。

## 9. 发布、迁移与恢复

- **迁移顺序：** source/provenance -> generator -> contracts expand -> domain batches -> drift Gate。
- **兼容窗口：** 每个 domain mapper 可暂时并存手工 transport type；public domain model 不变。
- **监控信号：** generation diff、source digest、type/API failures、domain batch count。
- **回滚或前向恢复：** 回退某 domain mapper 到手工 type；保留 last-known-good generated commit。
- **不可逆操作与批准点：** 无；删除手工 transport type 前要求引用为零和 mapper tests。
- **收缩条件：** 对应 domain 旧 transport type 零引用，clean regenerate/check/type/API/build 全绿。

## 10. 验收标准

- [x] `AC-027`：前序手工 transport types 已证明不阻塞，本 Ticket 不追溯改写历史。
- [x] `AC-028`：来源/命令/drift 明确，生成物确定，domain 业务模型未被 schema 替代。
- [x] shared owner、generator/source、commit/candidate/result SHA 写入 `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-16.md</Path>`。
