---
schema_version: 3
artifact: ticket
change: 2026-09-01-release-artifacts-mysql-baseline-consolidation
id: T-01
title: 建立唯一且可跟踪的 MySQL 发布基座
status: done
planning_depth: deep
planning_depth_reason: 改变六份数据库基座的所有权、可变性合同和发布命令语义，影响全新初始化与后续升级依据
ready: true
risk: high
blocked_by: []
contract_ids: [AC-001, AC-002, AC-003, AC-007, AC-009]
owner: codex:/root
expected_changes:
  - "<Path>release-artifacts/.gitignore</Path>"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/.gitignore</Path>"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/*.sql</Path>"
  - "<Path>release-artifacts/scripts/release-manage.sh</Path>"
  - "<Path>release-artifacts/tests/release-config.test.mjs</Path>"
  - "<Path>release-artifacts/tests/mysql-baseline-contract.test.mjs</Path>"
  - "<Path>release-artifacts/README.md</Path>"
writable_paths:
  - "<Path>release-artifacts/.gitignore</Path>"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/.gitignore</Path>"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/*.sql</Path>"
  - "<Path>release-artifacts/scripts/release-manage.sh</Path>"
  - "<Path>release-artifacts/tests/release-config.test.mjs</Path>"
  - "<Path>release-artifacts/tests/mysql-baseline-contract.test.mjs</Path>"
  - "<Path>release-artifacts/README.md</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/script/sql/**</Path>"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/15-nacos-init.sh</Path>"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/nacos/**</Path>"
shared_paths:
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/*.sql</Path>"
  - "<Path>release-artifacts/scripts/release-manage.sh</Path>"
shared_path_owners:
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/*.sql</Path> => T-01"
  - "<Path>release-artifacts/scripts/release-manage.sh</Path> => T-01"
---

# Ticket T-01：建立唯一且可跟踪的 MySQL 发布基座

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/ticket/01-establish-canonical-mysql-baseline.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/evidence/T-01.md</Path>`

## 1. 战略与来源

- **目标：** 让 `<Path>release-artifacts/docker/infrastructure/mysql/init/</Path>` 成为唯一 MySQL 基座，并让 `stage-mysql` 从复制器收敛为只读校验器。
- **可观察产出：** 六份 SQL 被 Git 跟踪且可直接编辑；执行 `stage-mysql` 前后内容与 Git 状态不变；缺文件、错顺序或资产不可读时命令明确失败。
- **来源：** `US-001`、`US-002`、`US-003`、`US-007`、`AC-001`、`AC-002`、`AC-003`、`AC-007`、`AC-009`、`USER-DECISION:仅支持 MySQL 且直接维护六份基座`。
- **当前事实：** 六组旧/新 SQL 当前摘要一致，但发布目录两层忽略规则排除 SQL，`stage_mysql_init` 仍从后端旧目录复制并删除目标 SQL。
- **Planning Depth 原因：** 基座所有权和发布消费方式同时变化；错误实现可能丢失 SQL、提交敏感文件或破坏全新初始化。

## 2. 决策状态

### 已锁定决策

- 六份文件名及 `10 -> 20 -> 30 -> 40 -> 50 -> 60` 顺序固定。
- 六份 SQL 是可直接迭代的完整基座，不采用 append-only 规则。
- 保留 `stage-mysql` 命令名，只把语义改为存在性、可读性、名称和顺序校验。
- 忽略规则只精准解除六份 SQL；`.env`、证书、日志、运行数据、缓存和构建物继续忽略。

### 已采用的低影响假设

- 新增独立的 MySQL 基座合同测试，减少与既有发布配置测试的职责混杂。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| Git 跟踪、忽略正反矩阵、六文件摘要预检、只读 `stage-mysql`、发布文档和合同测试 | 现有 Nacos 初始化脚本、固定 Schema、Shell 错误输出风格 | 删除后端 `script/`、迁移 Java 测试、修改真实数据库、生成已有库升级 SQL |

## 4. 要构建什么

维护者直接修改六份发布 SQL 后，可以运行 `stage-mysql` 检查发布目录是否完整。命令不得寻找后端 SQL 来源，不得删除未知 SQL，也不得覆盖任何文件。缺少任一必需资产时退出非零并指出文件；正常执行时六份 SQL 的摘要和仓库状态保持不变。

## 5. 实现契约

- **入口或接缝：** `<Path>release-artifacts/scripts/release-manage.sh</Path>` 的 `stage-mysql`；Node 合同测试；Git 忽略与跟踪查询。
- **输入与输出：** 输入为固定发布目录；输出为中文校验结果和退出状态，不产生 SQL 文件写入。
- **公共接口变化：** CLI 名称和调用方式不变，内部语义从复制变为验证。
- **不变量：** 六份 SQL 内容不因任何 stage 命令改变；Nacos 固定资产不变；敏感与生成文件仍被忽略。
- **状态或数据流：** Git 跟踪文件 -> 只读发布校验 -> fresh 初始化消费者；不再经过后端副本。
- **错误与失败行为：** 缺失、不可读、命名异常或顺序异常时失败并指出目标，不自动修复。
- **兼容要求：** 既有自动化仍可调用 `stage-mysql`；依赖“自动复制后端 SQL”的行为明确退出支持。
- **安全与隐私要求：** 反向忽略测试必须覆盖 `.env`、私钥、证书、日志、运行数据和构建输出。

## 6. 执行路线

1. 再次计算六组旧/新 SQL 摘要；任一不一致停止并升级为 Spec 偏差。
2. 先写失败合同，证明 SQL 当前未跟踪、`stage-mysql` 会写入且缺失资产未按新合同失败。
3. 精准调整两层忽略规则并确认六份 SQL进入父仓库跟踪候选。
4. 移除后端 SQL 来源、复制和删除逻辑，把 `stage_mysql_init` 改为只读验证。
5. 更新帮助文本与发布 README，明确六份文件直接维护和 fresh/upgrade 分流。
6. 运行正向、反向、幂等和 Shell 回归，记录前后摘要与 Git 状态。

## 7. 路径访问契约

- **预计修改点：** 与 frontmatter 的 `expected_changes` 一致。
- **可写范围：** 仅 frontmatter 的 `writable_paths`；六份 SQL 本 Ticket 只纳入跟踪，不改变业务内容。
- **只读上下文：** 后端旧 SQL 仅用于摘要比对；Nacos 资产只验证。
- **共享路径：** 六份 SQL 与发布脚本由 T-01 唯一拥有；其他 Ticket 只读。
- **保留或不动：** `<Path>release-artifacts/.env</Path>` 及所有现场密钥、运行数据和生成物。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | Node/Shell/Git | 运行发布测试、`stage-mysql`、`git ls-files` 和前后摘要 | 六份 SQL 被跟踪且零写入 | `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/evidence/T-01.md</Path>` |
| 失败路径 | 隔离副本 | 删除一个必需文件后运行校验 | 非零退出并指明缺失文件，不生成替代文件 | 同上 |
| 回归 | 忽略矩阵与 Shell 语法 | `git check-ignore` 正反断言、`bash -n`、完整 stage 测试 | secret/生成物仍忽略，既有发布命令不回归 | 同上 |

- **Workspace checks：** 按 Goal Plan 在所选 workspace 运行 Node 测试、Shell 语法和 Git 静态检查。
- **E2E disposition：** `not-required`：本 Ticket 只改变仓库资产所有权和只读发布校验，真实 MySQL 导入由 T-06 统一执行。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate；由 Goal Plan 的 workspace 策略确定。
- **Integration evidence：** 记录实现提交、父分支前后提交、六文件摘要和父分支包含关系。

## 9. 发布、迁移与恢复

- **迁移顺序：** 摘要相等 -> 解除忽略并跟踪 -> 改只读校验 -> 验证零写入。
- **兼容窗口：** 在 T-04 完成前保留后端旧目录但禁止发布脚本消费，形成短暂只读副本窗口。
- **监控信号：** Git 跟踪清单、忽略命中、stage 前后摘要、错误文件名。
- **回滚或前向恢复：** 若校验失败，修正唯一六文件或脚本；不得重新启用复制逻辑。未提交时按文件级 diff 恢复。
- **不可逆操作与批准点：** 本 Ticket 不删除旧目录、不写数据库；实现提交仍需单独授权。
- **收缩条件：** `stage-mysql` 中后端 SQL 来源、复制和删除调用为零且合同测试证明零写入。

## 10. 验收标准

- [ ] `AC-001`、`AC-002`、`AC-003`、`AC-007`、`AC-009` 全部形成可重复证据。
- [ ] 六组旧/新 SQL 在切换前摘要一致，六份发布 SQL 被 Git 跟踪且不被忽略。
- [ ] `stage-mysql` 正常和失败路径均不改写 SQL。
- [ ] 敏感与生成路径继续被忽略。
- [ ] 验证矩阵记录到 `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/evidence/T-01.md</Path>`。
- [ ] 修改未超出授权路径，并形成非空实现提交和父分支验证结果。
