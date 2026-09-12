---
schema_version: 3
artifact: ticket
change: 2026-08-28-user-password-policy-temporary-credentials
id: T-08
title: 发布密码策略数据迁移与长期认证合同
status: done
planning_depth: deep
planning_depth_reason: append-only DML 会改变生产配置和权限数据，且需协调滚动发布、回滚、OpenAPI/前后端完成状态与长期定制边界。
ready: true
risk: critical
blocked_by: [T-02, T-06, T-07]
contract_ids: [AC-005, AC-006, AC-019, AC-023]
owner: codex:lead
expected_changes: ["<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DML.sql</Path>", "<Path>ruoyi-vue-plus-namewta/script/sql/namewta/README.md</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/migration/password/**</Path>", "<Path>docs/upstream/customization-map.md</Path>"]
writable_paths: ["<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DML.sql</Path>", "<Path>ruoyi-vue-plus-namewta/script/sql/namewta/README.md</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/migration/password/**</Path>", "<Path>docs/upstream/customization-map.md</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/spec.md</Path>", "<Path>ruoyi-vue-plus-namewta/script/sql/ry_vue.sql</Path>", "<Path>plus-ui-namewta/packages/web-domains/system/src/index.ts</Path>", "<Path>plus-ui-namewta/packages/api-contracts/**</Path>"]
shared_paths: ["<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DML.sql</Path>", "<Path>ruoyi-vue-plus-namewta/script/sql/namewta/README.md</Path>", "<Path>docs/upstream/customization-map.md</Path>"]
shared_path_owners: ["<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DML.sql</Path> => T-08", "<Path>ruoyi-vue-plus-namewta/script/sql/namewta/README.md</Path> => T-08", "<Path>docs/upstream/customization-map.md</Path> => T-08"]
---

# Ticket T-08: 发布密码策略数据迁移与长期认证合同

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/ticket/08-publish-password-migration-and-contracts.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/evidence/T-08.md</Path>`

## 1. 战略与来源

- **目标：** 以可演练、可回滚的 NAMEWTA 增量数据块启用新策略和独立权限，并把经实现验证的认证边界写入长期定制文档。
- **可观察产出：** fresh/upgrade 数据库都有有效 random v1 策略，旧 initPassword 不再是 123456，菜单可独立授予 temporaryPassword；发布顺序不会让旧节点继续弱写。
- **来源：** `AC-005`、`AC-006`、`AC-019`、`AC-023`、`DEC-002`、`DEC-010`、`DEC-015`、`RISK-001`。
- **当前事实：** upstream `ry_vue.sql` 固定 initPassword=123456；NAMEWTA DML append-only；当前无新配置/菜单权限，customization map 未记录临时凭据和策略不变量。
- **Planning Depth 原因：** 生产数据配置、权限、滚动部署和跨三仓库发布具有高事故半径和不可静默回退要求。

## 2. 决策状态

### 已锁定决策

- 只在 DML.sql 尾部追加，timestamp/snowflake ID 由仓库脚本生成，不改历史或 ry_vue.sql。
- 新 key 默认 RANDOM v1；旧 key 用数据库随机源一次生成合规兼容值并标记退役，不提交跨环境固定明文。
- 新 sys_menu 按独立 permission 创建；除超管通配外不自动等同 resetPwd，由角色菜单配置授予。
- 迁移块写明 fresh/upgrade、重复执行、回滚/补偿；配置 JSON 必须小于 500 字符。
- 长期文档只记录当前不变量、发布/验证边界，不复制 Ticket 历史。

### 已采用的低影响假设

- 无。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| DML 配置/权限、迁移 tests、发布顺序/回滚、customization map | timestamp/Snowflake scripts、现有 sys_config/sys_menu、T-03/T-05 contracts | 不改 schema/ry_vue.sql，不部署生产，不默认授予普通角色 |

## 4. 要构建什么

在隔离 MySQL 上，fresh 顺序执行 upstream + NAMEWTA 后得到合法策略与独立菜单；已有库只执行新块，保留用户数据并消除 123456 默认。滚动发布先替换旧 key 为每环境随机合规值，再切全部 backend，再发布 frontend/授予权限。回滚步骤能恢复上一个 backend 可理解的配置并撤销菜单数据，不触碰用户密码。

## 5. 实现契约

- **入口或接缝：** DML append block、MySQL migration integration test、customization map。
- **输入与输出：** baseline DB -> new config row/legacy compatibility value/menu row；无 schema 变化。
- **公共接口变化：** 无新增 wire；发布已实现接口的数据启用条件。
- **不变量：** JSON <500；legacy !=123456 且满足基线；fixed secret 不提交；权限独立；ID 无冲突。
- **状态或数据流：** block preflight -> legacy safe value -> insert policy/menu -> backend cutover -> frontend -> role grant。
- **错误与失败行为：** 前置不符停止；重复执行按注释合同处理；中途失败执行补偿，不继续发布。
- **兼容要求：** 旧 backend 滚动窗口读取 legacy 合规值；新 backend 只读新 key；回滚恢复明确。
- **安全与隐私要求：** SQL/测试/文档不包含 fixedValue 或实际生成密码；DML random expression 不输出日志秘密。

## 6. 执行路线

1. 生成规范 timestamp 和 menu Snowflake ID，扫描 DML 历史、现有 PK/key/perms 冲突。
2. 先写 migration tests，覆盖 fresh、upgrade、JSON 长度/解析、legacy 合规随机和权限独立。
3. 仅在 DML 尾部追加完整配置/菜单/补偿块，不修改历史前缀。
4. 在隔离 MySQL 演练发布、重复执行适用语义和回滚/前向补偿。
5. 更新 customization map 和适用 SQL 说明，记录密码策略、临时凭据和 Client 失效长期边界。
6. 运行 SQL 静态扫描、backend migration tests、前后端最终全量 Gate。

## 7. 路径访问契约

- **预计修改点：** NAMEWTA DML/说明、password migration tests、父 customization map。
- **可写范围：** frontmatter 所列 DML、现有 SQL README、migration tests 与父文档。
- **只读上下文：** Spec、upstream SQL、frontend permission manifest/OpenAPI。
- **共享路径：** DML/SQL README/customization map 仅 T-08 写。
- **保留或不动：** DDL.sql、ry_vue.sql、产品源码、生产数据库。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | isolated MySQL | fresh + upgrade 执行 | policy 可解析、legacy 强随机、menu permission 精确 | `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/evidence/T-08.md</Path>` |
| 失败路径 | migration/rollback | key/ID 冲突、重复执行、半失败补偿 | 按合同停止或补偿，无历史 SQL/用户密码改写 | 同上 |
| 回归 | full gates | backend `./mvnw test`/full/core package；frontend `pnpm --filter @namewta/tooling-openapi openapi:check`、architecture/lint/typecheck/test/build/E2E | 三仓库组合、生成合同与长期文档一致 | 同上 |

- **Workspace checks：** Goal Plan current/source-worktree 运行 SQL 静态和 migration 非 E2E；不得连接生产。
- **E2E disposition：** required：真实 MySQL fresh/upgrade/rollback 与最终跨端认证流程是发布 Gate。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate；隔离 MySQL + Redis + Admin 浏览器，不在 source-worktree 声明最终 E2E。
- **Integration evidence：** backend SQL commit、frontend已集成 SHA、parent docs/result SHA、两个 gitlink 包含关系。

## 9. 发布、迁移与恢复

- **迁移顺序：** 暂停旧写入口 -> 应用 legacy 安全值和新 DML -> 部署全部 backend -> 健康/配置验证 -> 部署 frontend -> 独立授予权限 -> 观察。
- **兼容窗口：** 只在受控维护/滚动窗口存在；旧节点只读 legacy 合规值，新节点读新 key，禁止旧弱写。
- **监控信号：** policy parse、config cache refresh、temporary issue/auth、permission denial、session invalidation、login failure rate。
- **回滚或前向恢复：** 前端先回滚；撤销普通角色新权限；backend 回滚前恢复 legacy 合规配置；菜单/config 用补偿 SQL，绝不改用户 password。
- **不可逆操作与批准点：** 实际执行 DML、部署和角色授权均是外部写入，必须另行批准；本 Ticket 只编写并在隔离环境验证。
- **收缩条件：** 新代码与前端对 `sys.user.initPassword`、旧弱静态规则零匹配；所有节点读取新 policy，OpenAPI/权限 manifest/DML 一致。

## 10. 验收标准

- [x] `AC-005`、`AC-006` 的独立权限数据存在且不隐式授予普通角色。
- [x] `AC-019` 的 random/fixed 配置迁移、缓存刷新前置与回滚可执行。
- [x] `AC-023` 的审计/权限文档无秘密，长期 customization map 已同步。
- [x] DML 历史前缀逐字不变，fresh/upgrade/rollback Evidence 完整。
- [x] backend/frontend/parent commits 与 Lead 最终 Gate、result SHA、gitlink 包含关系完整。
