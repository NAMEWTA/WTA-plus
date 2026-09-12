---
schema_version: 3
artifact: spec
change: 2026-09-01-release-artifacts-mysql-baseline-consolidation
status: ready
ready_for_tickets: true
sources:
  - USER-DECISION:2026-09-01-release-artifacts-is-the-only-release-and-mysql-baseline-source
  - USER-DECISION:2026-09-01-mysql-only-and-remove-other-dialects-without-migration
  - CODE:release-artifacts/docker/infrastructure/mysql/init/**
  - CODE:ruoyi-vue-plus-namewta/script/**
---

# Spec：发布资产与 MySQL 基座唯一事实源收敛

- **Spec：** `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/spec.md</Path>`
- **实施计划：** `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/plan.md</Path>`

## 1. 问题与目标

### 问题陈述

当前工作区同时保留后端子仓库 `<Path>ruoyi-vue-plus-namewta/script/</Path>` 与父仓库 `<Path>release-artifacts/</Path>` 两套发布和数据库资产。六份 MySQL SQL 虽已等价复制到 `<Path>release-artifacts/docker/infrastructure/mysql/init/</Path>`，但仍被发布目录的忽略规则排除，发布脚本还会从后端旧目录复制并覆盖它们。后端测试、配置注释、长期文档、项目 Skill 和仍在执行的 SpecDev 工件继续引用旧路径，使维护者无法确定真正应修改哪一份 SQL。

这会产生四类风险：数据库基座双源漂移、应交付 SQL 未进入 Git、发布阶段覆盖人工修改、删除后端旧目录后测试和文档失效。后端旧目录还继续携带当前项目不支持的 Oracle、PostgreSQL 和 SQL Server 脚本，错误扩大了支持边界。

### 目标用户与场景

- 后端和数据库开发者需要在唯一位置直接维护当前 MySQL 完整基座。
- 发布维护者需要从 Git 中取得可复现的六份初始化 SQL，发布过程不得再从其他目录生成或覆盖它们。
- CI 和评审者需要自动判定 SQL 是否存在、被跟踪、未被忽略、分类正确且没有旧路径回归。
- Workflow 开发者需要在删除后端 `script/` 后继续访问已有的流程 JSON 示例。
- 运维人员需要区分“全新环境完整初始化”和“既有数据库按 Git 标签差异升级”两条路径。

### 成功标准

1. `<Path>release-artifacts/docker/infrastructure/mysql/init/</Path>` 是数据库初始化的唯一事实源，六份 SQL 均被父仓库 Git 跟踪且不被忽略。
2. `<Path>ruoyi-vue-plus-namewta/script/</Path>` 整体不存在，活动代码、测试、配置、文档和规划不再依赖该路径。
3. 项目唯一支持的数据库方言是 MySQL；Oracle、PostgreSQL 和 SQL Server 脚本直接删除，不迁移到任何新位置。
4. 六份 SQL 表达当前最新完整基座，开发者可以直接修改现有内容，不受 append-only、时间戳变更块或历史前缀冻结约束。
5. 发布暂存命令只验证六份 SQL，不复制、不删除、不重排、不覆盖它们。
6. SQL 合同测试以父仓库的六份文件为输入，后端普通构建不再隐式要求子仓库拥有发布 SQL。
7. 仍有价值的 6 个 Workflow JSON 在父仓库发布资产中继续被跟踪和引用。
8. 全新 MySQL 8.4 环境可以按 `10 -> 20 -> 30 -> 40 -> 50 -> 60` 顺序完成初始化；既有数据库升级不得无条件重放完整基座。

### 非目标

- 本 change 不修改六份 SQL 所表达的业务 Schema 或初始化数据，只迁移所有权和维护合同。
- 本 change 不对开发、测试或生产数据库执行 DDL/DML。
- 本 change 不恢复或新增 Oracle、PostgreSQL、SQL Server 及其他数据库方言支持。
- 本 change 不建立仓库内版本化迁移脚本目录或重新引入 append-only 迁移账本。
- 本 change 不批量重写已归档 Evidence、历史 handoff 或描述当时真实路径的历史工件。
- 本 change 不部署服务器、不重建容器、不清理持久化卷。

## 2. 解决方案与外部行为

### 解决方案摘要

父仓库 `<Path>release-artifacts/</Path>` 同时成为发布拓扑、发布脚本和 MySQL 数据库基座的唯一 owner。数据库基座固定由以下六份可直接编辑的文件组成：

| 顺序 | 唯一文件 | 职责 |
|---|---|---|
| 10 | `<Path>release-artifacts/docker/infrastructure/mysql/init/10-ruoyi-base.sql</Path>` | RuoYi 基础 Schema 与基础数据 |
| 20 | `<Path>release-artifacts/docker/infrastructure/mysql/init/20-ry-job.sql</Path>` | Job Schema 与初始化数据 |
| 30 | `<Path>release-artifacts/docker/infrastructure/mysql/init/30-ry-workflow.sql</Path>` | Workflow Schema 与初始化数据 |
| 40 | `<Path>release-artifacts/docker/infrastructure/mysql/init/40-ry-ai.sql</Path>` | AI Schema 与初始化数据 |
| 50 | `<Path>release-artifacts/docker/infrastructure/mysql/init/50-namewta-ddl.sql</Path>` | NAMEWTA 表、字段、索引和约束 |
| 60 | `<Path>release-artifacts/docker/infrastructure/mysql/init/60-namewta-dml.sql</Path>` | NAMEWTA 初始化、菜单、字典、配置与回填数据 |

开发者根据实际 owner 直接修改对应文件。`50` 只承载 DDL，`60` 只承载 DML；`10/20/30/40` 可以在各自组件基座内同时包含完成全新初始化所需的结构与种子数据。已有新表仍必须符合模块前缀、非裸 `id` 主键、七个基础字段和中文表/字段注释等项目 Schema 质量要求。

### 主要流程

#### 全新环境

1. 从父仓库检出被 Git 跟踪的发布资产。
2. 发布校验确认六份 SQL、Nacos 初始化资产和执行顺序完整。
3. MySQL 8.4 按 `10 -> 20 -> 30 -> 40 -> 50 -> 60` 顺序导入同一目标库。
4. 初始化脚本验证表数量、应用账号和默认私有 OSS 等运行不变量。

#### 基座迭代

1. 开发者识别变更所属组件。
2. 直接编辑对应的唯一 SQL 文件；允许修改、删除、替换或重排尚未形成目标基座所需的现有内容。
3. SQL 合同、静态门禁和隔离 MySQL fresh install 验证新的完整基座。
4. 代码评审同时检查 Schema/Java 映射、初始化数据/权限合同和发布文档。

#### 已有数据库升级

1. 以部署现场当前 Git Tag 的六份 SQL 为旧基线，以目标 Git Tag 的六份 SQL 为新基线。
2. 生成并评审实际 DDL/DML 差异，在隔离 MySQL 演练。
3. 经发布授权后，只执行目标环境所需差异，不重放六份完整文件。
4. 将执行命令、备份、结果和回滚/补偿记录到本机忽略目录 `<Path>temp/relase/</Path>`，不把现场迁移脚本回写为第二套仓库事实源。

### 边界、失败与稳定错误行为

- 任一六份 SQL 缺失、未被 Git 跟踪或被 `.gitignore` 命中时，发布验证必须失败并指出精确文件。
- 任一活动脚本、测试、配置、Skill 或当前实施工件重新引用 `<Path>ruoyi-vue-plus-namewta/script/</Path>` 时，路径回归门禁必须失败。
- `stage-mysql` 或完整 `stage` 尝试复制、删除或覆盖六份 SQL 时，发布契约测试必须失败。
- `50-namewta-ddl.sql` 混入业务数据写入，或 `60-namewta-dml.sql` 混入建表/改表/删表语句时，分类门禁必须失败。
- 目标数据库不是 MySQL 8.4，或请求恢复其他方言支持时，当前发布流程明确拒绝，不生成兼容脚本。
- 已有数据库缺少可确认的当前 Tag、备份或差异演练证据时，升级必须停止，不能用完整基座试错。
- 删除旧目录前若六份 SQL 校验值与新位置不一致，实施必须停止并先裁决差异，不能静默选择一侧。

### 状态转换与不变量

```text
双源且 SQL 被忽略
        ↓ 六份内容逐文件一致性确认
父仓库六份 SQL 纳入跟踪
        ↓ 发布脚本改为只验证
测试、文档、Skill 与活动计划切换到新 owner
        ↓ Workflow JSON 迁移完成
后端 script/ 整体删除
        ↓ 全部门禁通过
父仓库发布资产成为唯一事实源
```

长期不变量：

- 只支持 MySQL，非 MySQL 方言不保留兼容占位。
- 六份 SQL 均为 Git 跟踪的源文件，不是构建产物。
- 六份 SQL 是当前完整基座，可以直接修改，不是 append-only 迁移日志。
- 发布脚本消费并验证 SQL，不生产 SQL。
- `<Path>release-artifacts/.env</Path>`、构建物、运行数据、证书私钥、日志和缓存继续被忽略，不能因追踪 SQL 而扩大敏感文件范围。
- 已有数据库升级依赖 Tag 差异、备份和现场 Evidence，不重放完整基座。

## 3. 用户故事

- **US-001**：作为数据库开发者，我希望只修改父仓库六份 MySQL SQL，以便不会在后端与发布目录之间维护重复副本。
- **US-002**：作为基座开发者，我希望可以直接调整完整 SQL 基座，而无需追加历史迁移块，以便基座始终描述当前最新初始状态。
- **US-003**：作为发布维护者，我希望 SQL 被 Git 跟踪且发布命令只验证它们，以便构建、暂存和部署不会覆盖已评审内容。
- **US-004**：作为后端维护者，我希望删除整个旧 `script/` 后测试和普通构建仍有确定行为，以便后端仓库不再承担发布资产所有权。
- **US-005**：作为 Workflow 开发者，我希望流程 JSON 移到父仓库并保持可发现，以便删除旧目录不会丢失可复用流程定义。
- **US-006**：作为运维人员，我希望全新初始化与已有库升级有不同门禁，以便不会把完整基座误当成可重复迁移脚本。
- **US-007**：作为项目维护者，我希望明确只支持 MySQL 并删除其他方言，以便文档、测试和发布承诺与真实支持范围一致。

## 4. 验收合同

| ID | 前置条件 | 动作或事件 | 可观察结果 | 验证接缝 |
|---|---|---|---|---|
| AC-001 | 当前六组旧/新 SQL 已确认逐字一致 | 完成所有权迁移 | 父仓库六份 SQL 内容无损且均被 Git 跟踪 | SHA-256 对照、`git ls-files` |
| AC-002 | 父仓库存在发布忽略规则 | 检查六份 SQL | 六份 SQL 均不被忽略；`.env`、构建物、运行数据、证书、日志和缓存仍被忽略 | `git check-ignore` 正反向矩阵 |
| AC-003 | 发布管理命令可执行 | 运行 `stage-mysql` | 命令只验证六份 SQL/Nacos 资产，运行前后六份 SQL 内容和 Git 状态不变 | Shell 契约测试、前后摘要 |
| AC-004 | 后端旧 `script/` 存在 | 完成收敛 | `<Path>ruoyi-vue-plus-namewta/script/</Path>` 不存在，活动源码和测试不再引用它 | 路径存在性与 `rg` 门禁 |
| AC-005 | 旧目录含 Oracle、PostgreSQL、SQL Server SQL | 完成删除 | 三类方言文件不存在，父仓库没有对应迁移副本，支持文档只声明 MySQL | 文件扫描、文档合同测试 |
| AC-006 | 六份 SQL 是可编辑完整基座 | 开发者修改对应文件 | 规范和测试不再要求 append-only、时间戳块、历史前缀冻结或 Snowflake 辅助脚本 | Skill/文档扫描、SQL 合同测试 |
| AC-007 | NAMEWTA SQL 分为结构与数据 | 验证 `50` 与 `60` | `50` 只含 DDL；`60` 只含 DML；分类违规可被门禁捕获 | SQL 结构化或语句级分类测试 |
| AC-008 | 项目仍保留新表质量规范 | 修改或新增项目自有表 | 模块前缀、主键、基础字段和中文注释合同继续生效 | SQL 合同与隔离 MySQL 元数据查询 |
| AC-009 | Workflow JSON 尚在旧目录 | 删除后端 `script/` | 6 个 JSON 在 `<Path>release-artifacts/workflow/leave/</Path>` 被跟踪，Workflow 文档和 Skill 指向新位置 | 文件清单、JSON 解析、引用扫描 |
| AC-010 | 后端存在 SQL 文本和集成测试 | 切换 SQL owner | 纯 SQL 合同由父仓库测试；需要 Java/真实 MySQL 的测试通过显式工作区 SQL 根读取唯一文件；普通后端构建不伪造 SQL 副本 | Node 测试、Maven 单元/集成门禁 |
| AC-011 | 存在当前权威文档和活动计划 | 完成路径迁移 | README、项目 Skill、工程规范、配置注释、customization map 和正在实施的 SpecDev 工件均指向新 owner | 活动引用扫描、SpecDev 校验 |
| AC-012 | 空白 MySQL 8.4 环境可用 | 顺序执行六份 SQL | 全新初始化完成，应用表、组件表、种子数据和关键运行不变量符合预期 | 隔离 MySQL fresh install |
| AC-013 | 目标是已有数据库 | 尝试发布升级 | 未确认 Tag、备份、差异和演练时流程停止；具备证据时只执行评审后的差异 | 部署 Skill 门禁、升级报告审查 |
| AC-014 | 历史 SpecDev Evidence 含旧路径 | 进行文档同步 | 历史归档不被批量改写；当前权威和活动实施工件不再使用旧 owner | Git diff 范围与活动/历史分类扫描 |

## 5. 范围

### IN

- 六份 MySQL SQL 的 Git 跟踪、唯一所有权、职责和直接修改合同。
- 发布目录忽略规则的精确收敛。
- 发布暂存脚本从“复制生成”改为“存在性与合同验证”。
- 后端 `<Path>ruoyi-vue-plus-namewta/script/</Path>` 全目录删除。
- Oracle、PostgreSQL、SQL Server SQL 无迁移删除。
- 旧启动、Docker 和 SQL 辅助脚本退役。
- Workflow JSON 迁移到父仓库发布资产。
- SQL 相关 Java/Node 测试接缝迁移。
- 当前权威 README、Skill、工程规范、配置注释和活动 SpecDev 路径同步。
- MySQL fresh install 与已有数据库升级门禁。

### REUSE

- 复用 `<Path>release-artifacts/scripts/init-mysql-container.sh</Path>` 的受保护全新库初始化和关键运行不变量验证。
- 复用 `<Path>release-artifacts/tests/release-config.test.mjs</Path>` 的发布资产合同测试框架。
- 复用 `<Path>release-artifacts/scripts/verify-release.sh</Path>` 的统一发布校验入口。
- 复用 `<Path>.agents/skills/deploy-namewta-environment/</Path>` 的接管、升级、备份和报告安全边界。
- 复用当前后端 Maven 测试中的真实 MySQL 属性门控，不把外部服务测试伪装成普通单元测试。

### OUT

- **OOS-001**：Oracle、PostgreSQL、SQL Server SQL 的迁移、归档或兼容维护；它们直接删除。
- **OOS-002**：对任一真实数据库执行 Schema 或数据变化；本 change 只改变仓库所有权和工具链。
- **OOS-003**：为每次基座修改在仓库新增版本化 migration 文件；已有环境差异属于部署 Evidence。
- **OOS-004**：保留后端旧启动脚本或旧 Compose 的兼容入口；当前发布入口已经由父仓库替代。
- **OOS-005**：批量改写归档 change、历史 Evidence 和 handoff 中描述当时真实状态的路径。
- **OOS-006**：改变前端应用行为、HTTP 公共接口或 Java 业务 API。
- **OOS-007**：在本 change 内提交、推送、打 Tag 或部署；实施和 Git 副作用需要后续明确授权。

## 6. 已锁定实现约束

- **DEC-001**：父仓库 `<Path>release-artifacts/</Path>` 是发布资产与 MySQL 基座唯一 owner；后端子仓库不保留 `script/`。来源：`USER-DECISION:2026-09-01-release-artifacts-is-the-only-release-and-mysql-baseline-source`。
- **DEC-002**：只支持 MySQL；Oracle、PostgreSQL 和 SQL Server 目录直接删除，不迁移、不归档、不保留占位。来源：`USER-DECISION:2026-09-01-mysql-only-and-remove-other-dialects-without-migration`。
- **DEC-003**：六份 SQL 是可直接修改的完整基座，不是 append-only 迁移日志；时间戳与雪花 ID 辅助脚本不再构成 SQL 变更前置。来源：`USER-DECISION:2026-09-01-baseline-is-always-iterated-directly`。
- **DEC-004**：发布脚本只消费和验证六份 SQL，不得从后端路径复制、生成或覆盖。来源：`AC-003`。
- **DEC-005**：`50` 与 `60` 分别维持 DDL/DML 职责，新表质量规则继续适用。来源：`AC-007`、`AC-008`。
- **DEC-006**：现存 Workflow JSON 迁移到父仓库发布资产后保留；其他已被新发布能力替代的旧脚本删除。来源：用户确认“以上计划”并追加 MySQL-only 裁决。
- **DEC-007**：已有数据库通过 Git Tag 基座差异形成现场执行计划，完整六文件只用于全新初始化；现场差异记录位于被忽略的部署报告，不成为第二套仓库源。来源：`AC-013`。
- **DEC-008**：只解除 SQL 的误忽略；真实 secret、构建物、运行数据、证书私钥、日志和缓存继续忽略。来源：安全配置合同与 `AC-002`。

## 7. 数据、接口与兼容

- **公共接口变化：** 无 HTTP、Java 公共 API 或前端运行接口变化。发布 CLI 中 `stage-mysql` 名称保留以兼容现有调用，但语义从复制 SQL 收敛为验证 SQL。
- **数据模型与持久化：** 本 change 不改变目标 Schema 和数据内容。它改变 SQL 文件所有权、可变性合同和发布消费方式。
- **兼容要求：** 父聚合工作区成为数据库发布与 SQL 合同测试的必需上下文；后端普通编译和非 SQL 单元测试保持可独立运行。需要唯一 SQL 的 Java 集成测试必须接受显式 SQL 根并由父级门禁提供。
- **迁移要求：** 六组旧/新文件在删除前必须逐文件内容一致。Workflow JSON 先迁移验证，再删除旧目录。非 MySQL 方言无迁移要求，直接删除。
- **发布或运维影响：** 全新环境继续按六文件顺序初始化；已有环境必须从 Tag 差异、备份和隔离演练生成现场变更，不得重放完整基座。发布报告需要记录源/目标 Tag 和差异执行证据。

## 8. 非功能要求

- **NFR-001 安全与隐私：** 解除忽略不得使 `.env`、密码、令牌、私钥、证书现场文件、运行数据或日志进入 Git。SQL 和流程 JSON 不得包含运行 secret。
- **NFR-002 性能与容量：** 不设置运行时性能阈值；六份 SQL 的验证不得在普通发布校验中访问生产数据库。
- **NFR-003 可用性与可靠性：** 发布脚本不得破坏或重写 Git 跟踪 SQL；已有数据库升级必须有备份、差异演练和停止条件。全新初始化失败只能清理本次创建的隔离资源。
- **NFR-004 可观测性与运营：** 校验失败必须指出具体文件、忽略规则、旧引用或 SQL 分类；升级 Evidence 记录源/目标 Tag、命令、结果、备份和回滚/补偿。
- **NFR-005 可维护性：** 活动规范不得再表达双源、append-only 或多方言支持；新增 SQL owner 必须可由单次仓库搜索发现。

## 9. 验证策略

| 接缝 | 层级 | 覆盖合同 | 现有先例或命令 | Evidence 类型 |
|---|---|---|---|---|
| Git 跟踪与忽略矩阵 | 仓库静态 | AC-001、AC-002 | `git ls-files`、`git check-ignore` | 文件清单与退出状态 |
| 发布资产与 `stage-mysql` 合同 | Node/Shell | AC-003、AC-005、AC-007、AC-009 | `<Path>release-artifacts/tests/release-config.test.mjs</Path>`、`bash -n` | TAP、前后 SHA-256、Git 状态 |
| 旧路径与支持范围扫描 | 仓库静态 | AC-004、AC-005、AC-006、AC-011、AC-014 | `rg`、目录存在性、活动/历史分类扫描 | 扫描报告 |
| SQL 内容合同 | Node 与 Java 定向测试 | AC-007、AC-008、AC-010 | 父仓库 SQL 合同测试、后端 Maven 定向测试 | 测试报告 |
| 后端普通回归 | Maven reactor | AC-004、AC-010 | `./mvnw test`、`./mvnw clean package -DskipTests` | Surefire 与构建日志 |
| MySQL 8.4 全新初始化 | 隔离真实服务 | AC-008、AC-012 | `<Path>release-artifacts/scripts/init-mysql-container.sh</Path>` | 表/数据/元数据查询与退出状态 |
| 已有库升级门禁 | 部署流程审查/dry-run | AC-013 | 部署 Skill、`<Path>temp/relase/</Path>` 报告模板 | Tag 差异、备份和演练记录 |
| SpecDev 与 Skill 一致性 | 文档/Schema | AC-011、AC-014 | SpecDev validator、Skill validator、链接扫描 | 校验输出 |

## 10. 风险、假设与未决问题

### 风险

- 直接修改完整基座会失去仓库内逐次迁移历史；已有数据库必须依赖 Git Tag 差异和部署 Evidence，不能把 fresh SQL 当 upgrade SQL。
- 后端 SQL 契约测试跨到父仓库后，错误处理可能使独立后端 CI 静默跳过关键测试；必须把纯 SQL 合同上移，并让需要 SQL 的集成测试显式声明输入。
- 删除整个 `script/` 可能遗漏旧文档或仍在实现的 Ticket 路径；活动路径门禁必须在删除前后运行。
- 忽略规则放宽过度可能提交 secret 或构建产物；必须使用正反向忽略矩阵，而不是删除整个发布 `.gitignore`。
- `10/20/30/40` 直接迭代会增加未来上游同步冲突；上游同步必须把六份文件视为 NAMEWTA 发布基座并显式评审差异。

### 已采用的低影响假设

- `stage-mysql` 名称继续保留，避免无必要破坏现有自动化；其新语义由帮助文本和合同测试明确。
- Workflow JSON 使用 `<Path>release-artifacts/workflow/leave/</Path>` 作为稳定新位置；由文件清单、JSON 解析和 Workflow Skill 引用验证。
- 历史归档不回写，只扫描当前权威文档、活动代码和仍在执行的 change；由路径分类清单验证。

### 未决问题

无。
