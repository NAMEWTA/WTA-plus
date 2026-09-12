# 发布资产与 MySQL 基座收敛实施计划

- **Change：** `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/</Path>`
- **行为权威：** `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/spec.md</Path>`
- **规划深度：** Deep；原因是跨父仓库和后端子仓库改变数据库基座所有权、测试接缝、发布兼容和已有库升级门禁。
- **执行授权：** 未授权。本计划只形成执行路线，不授权提交、推送、部署或数据库写入。

## 1. 最终目录和所有权

```text
release-artifacts/
├── docker/infrastructure/mysql/init/
│   ├── 10-ruoyi-base.sql
│   ├── 15-nacos-init.sh
│   ├── 20-ry-job.sql
│   ├── 30-ry-workflow.sql
│   ├── 40-ry-ai.sql
│   ├── 50-namewta-ddl.sql
│   ├── 60-namewta-dml.sql
│   └── nacos/
└── workflow/leave/
    ├── leave1.json
    ├── leave2.json
    ├── leave3.json
    ├── leave4.json
    ├── leave5.json
    └── leave6.json

ruoyi-vue-plus-namewta/
└── script/ 不存在
```

父仓库拥有发布 SQL、流程 JSON、发布脚本、测试、文档和项目 Skill。后端子仓库拥有应用源码及需要 Java 运行时的测试，但不拥有任何发布 SQL 副本。

## 2. Gate 与执行阶段

### Gate 0：冻结基线和保护用户工作

1. 读取父仓库、前端子仓库和后端子仓库状态，保存未提交改动与当前提交。
2. 不覆盖当前另一个活跃 change 的状态或工件。
3. 对六组旧/新 SQL 计算 SHA-256，要求逐组一致：
   - `ry_vue.sql` 对 `10-ruoyi-base.sql`
   - `ry_job.sql` 对 `20-ry-job.sql`
   - `ry_workflow.sql` 对 `30-ry-workflow.sql`
   - `ry_ai.sql` 对 `40-ry-ai.sql`
   - `namewta/DDL.sql` 对 `50-namewta-ddl.sql`
   - `namewta/DML.sql` 对 `60-namewta-dml.sql`
4. 任一不一致立即停止，建立 spec 级偏差并由用户决定权威内容。

### 阶段 1：让六份 SQL 成为父仓库源文件

1. 删除 `<Path>release-artifacts/docker/infrastructure/mysql/init/.gitignore</Path>` 的全目录排除效果；该文件没有其他治理价值时直接删除。
2. 从 `<Path>release-artifacts/.gitignore</Path>` 移除 `docker/infrastructure/mysql/init/*.sql`。
3. 将六份 SQL 纳入父仓库跟踪。
4. 保持以下忽略合同：
   - `<Path>release-artifacts/.env</Path>` 与其他真实环境密钥；
   - `<Path>release-artifacts/builds/</Path>`、`<Path>release-artifacts/bundles/</Path>` 生成物；
   - 后端镜像中的 `app.jar`；
   - 前端构建 HTML、TLS 现场证书/私钥、日志、`runtime/`、缓存和 Python bytecode。
5. 新增正反向测试：六份 SQL 不被忽略；上述敏感或生成路径继续被忽略。

### 阶段 2：改造发布脚本为只验证

1. 修改 `<Path>release-artifacts/scripts/release-manage.sh</Path>`：
   - 移除 `BACKEND_ROOT/script/sql` 来源；
   - 移除六组复制映射；
   - 移除清理目标 SQL 的 `find ... -delete`；
   - `stage_mysql_init` 改为验证六份 SQL、`15-nacos-init.sh` 和 `nacos/` 资产存在、可读、名称和顺序正确；
   - `stage-mysql` CLI 保留并更新帮助文本。
2. `stage --env` 继续调用验证逻辑，但运行前后 SQL SHA-256 与 Git 状态必须不变。
3. 更新 `<Path>release-artifacts/tests/release-config.test.mjs</Path>`：删除“从后端复制”的临时目录场景，替换为“发布源直接验证且不改写”的场景。
4. 更新 `<Path>release-artifacts/README.md</Path>`，明确直接维护六份文件。

### 阶段 3：迁移 Workflow JSON

1. 创建 `<Path>release-artifacts/workflow/leave/</Path>`。
2. 将后端旧目录中的 `leave1.json` 至 `leave6.json` 等价迁移到新位置。
3. 对迁移前后文件计算 SHA-256，并逐个解析 JSON。
4. 更新 `<Path>.agents/skills/ruoyi-workflow-module-guide/</Path>` 和当前 Workflow 使用文档中的活动引用。
5. 增加发布合同测试，要求 6 个文件均存在、可解析且被 Git 跟踪。

### 阶段 4：迁移 SQL 测试接缝

1. 盘点后端所有读取 `script/sql` 的测试并分类：
   - 纯 SQL 内容、标记、表/字段、菜单、权限、DDL/DML 分类；
   - 需要 Java Service、Mapper 或真实 MySQL 的集成行为；
   - 同时扫描 SQL owner 的架构测试。
2. 将纯 SQL 合同迁移到父仓库 `<Path>release-artifacts/tests/</Path>`，以六份唯一 SQL 为输入。
3. 对需要 Java/真实 MySQL 的测试建立统一 SQL 根解析合同：
   - 显式 JVM system property；
   - 显式环境变量；
   - 聚合工作区父级默认路径仅作为本地便利入口。
4. 父仓库 CI 显式传入 SQL 根。缺少 required SQL 输入时，工作区集成门禁失败，不得静默跳过。
5. 后端普通 compile、package 和不依赖 SQL 的单元测试保持独立可运行；不得复制 SQL 到 `src/test/resources` 制造第三份源。
6. 更新配置注释，将 SnailJob 表引用改为 `<Path>release-artifacts/docker/infrastructure/mysql/init/20-ry-job.sql</Path>`。

### 阶段 5：删除后端旧 script

按以下分类删除 `<Path>ruoyi-vue-plus-namewta/script/</Path>`：

| 旧内容 | 处理 |
|---|---|
| `script/sql/ry_vue.sql`、`ry_job.sql`、`ry_workflow.sql`、`ry_ai.sql` | 六份新 SQL 已校验并跟踪后删除 |
| `script/sql/namewta/DDL.sql`、`DML.sql` | `50/60` 已校验并跟踪后删除 |
| `script/sql/oracle/` | 直接删除，不迁移 |
| `script/sql/postgres/` | 直接删除，不迁移 |
| `script/sql/sqlserver/` | 直接删除，不迁移 |
| `generate-change-timestamp.js`、`generate-snowflake-id.js` | 直接删除；新合同不再依赖追加块工具 |
| `script/sql/namewta/README.md` | 有效当前规则先并入父仓库发布文档，再删除 |
| `script/docker/` | 父仓库 Compose 已替代，直接删除 |
| `script/bin/` | 父仓库发布管理脚本已替代，直接删除 |
| `script/leave/` | JSON 等价迁移验证后删除 |

删除完成后要求 `<Path>ruoyi-vue-plus-namewta/script/</Path>` 不存在。

### 阶段 6：同步规范、文档和活动计划

1. 更新父仓库当前权威：
   - `<Path>README.md</Path>`；
   - `<Path>docs/README.md</Path>`；
   - `<Path>docs/namewta-enhancements.md</Path>`；
   - `<Path>docs/oss-public-private-operations.md</Path>`；
   - `<Path>docs/upstream/customization-map.md</Path>`；
   - `<Path>release-artifacts/README.md</Path>`。
2. 更新后端当前权威：
   - `<Path>ruoyi-vue-plus-namewta/README.md</Path>`；
   - `<Path>ruoyi-vue-plus-namewta/docs/upstream/README.md</Path>`；
   - `application-dev.yml`、`application-prod.yml` 的 SQL 导航注释。
3. 更新项目 Skill 和规范：
   - `<Path>.agents/skills/engineering-standards/</Path>`；
   - `<Path>.agents/skills/namewta-fullstack-development/</Path>`；
   - `<Path>.agents/skills/deploy-namewta-environment/</Path>`；
   - `<Path>.agents/skills/ruoyi-workflow-module-guide/</Path>`。
4. 规范变化必须明确：MySQL-only、六文件直接编辑、`50/60` 分类、fresh/upgrade 分流、非 MySQL 方言退出、release-artifacts 唯一 owner。
5. 更新仍在实施的 `<Path>{roots.state}/specdev/changes/2026-08-31-account-profile-verification/</Path>` 中 SQL writable/shared/read-only 路径和 owner 投影，避免后续 Ticket 恢复旧目录。
6. 已 completed 或 archived 的 Spec、Ticket、Evidence 和 handoff 保持历史原文，不做批量路径替换。

### 阶段 7：建立防回归门禁

新增或修改门禁，至少证明：

1. 六份 SQL 存在、被 Git 跟踪且不被忽略。
2. 后端 `script/` 不存在。
3. 发布脚本不包含 `BACKEND_ROOT/script/sql`、SQL 复制和 SQL 删除逻辑。
4. `stage-mysql` 运行前后六份 SQL 内容不变。
5. `50` 仅 DDL，`60` 仅 DML。
6. Oracle、PostgreSQL、SQL Server 路径不存在，当前支持文档只声明 MySQL。
7. 6 个 Workflow JSON 存在、可解析、被跟踪。
8. 当前权威文档、Skill、活动代码和活动实施工件不存在旧路径；历史工件排除在此断言外。
9. `.env`、构建物、运行数据、证书、日志和缓存仍被忽略。

## 3. 验证矩阵

| Gate | 命令或方法 | 预期 |
|---|---|---|
| Git 跟踪 | `git ls-files release-artifacts/docker/infrastructure/mysql/init` | 六份 SQL 全部出现 |
| 忽略正向 | 对六份 SQL 执行 `git check-ignore` | 均未命中 |
| 忽略负向 | 对 `.env`、构建物、运行目录、私钥候选执行 `git check-ignore` | 均命中预期规则 |
| 目录删除 | `test ! -d ruoyi-vue-plus-namewta/script` | 退出状态 0 |
| 旧引用 | 对活动范围执行 `rg` | 无旧 owner 引用 |
| JSON | 逐个解析 `<Path>release-artifacts/workflow/leave/*.json</Path>` | 6 个全部有效 |
| 发布合同 | `node --test release-artifacts/tests/*.test.mjs` | 全部通过 |
| Shell | 对 `<Path>release-artifacts/scripts/*.sh</Path>` 执行 `bash -n` | 全部通过 |
| 发布总门禁 | `bash release-artifacts/scripts/verify-release.sh` | 除明确环境 skip 外通过 |
| 后端测试 | 在 `<Path>ruoyi-vue-plus-namewta/</Path>` 执行 `./mvnw test` | 非 SQL 普通门禁与显式 SQL 门禁符合分类 |
| 后端打包 | `./mvnw clean package -DskipTests` | 全量构建通过 |
| Fresh MySQL | 在隔离 MySQL 8.4 顺序导入六份 SQL | 初始化及运行不变量通过 |
| Skill | 对受影响 Skill 运行 Skill validator | 全部有效 |
| SpecDev | 运行 spec、自检及受影响活动 change 校验 | 全部通过 |
| 差异质量 | `git diff --check` | 无错误 |

## 4. 提交与集成顺序

实施获得授权后按以下顺序形成可审查提交：

1. **后端子仓库提交：** SQL 读取测试接缝、配置注释、README 更新和整个 `script/` 删除。
2. **父仓库提交：** 六份 SQL 纳入跟踪、Workflow JSON、忽略规则、发布脚本/测试、项目文档/Skill/SpecDev 更新，并更新后端 gitlink。
3. 前端子仓库无行为或路径变化时不制造空提交。
4. 两个提交均完成相应门禁后，才申请推送、Tag 或部署授权。

## 5. 回滚与恢复

- 实施尚未提交时，可按文件级 diff 恢复；不得使用破坏性工作树重置覆盖用户改动。
- 后端提交回滚会恢复 `script/`，父仓库提交回滚会恢复 SQL 忽略和旧发布脚本；两个仓库必须成对回滚，不能保留中间双源状态。
- 已有数据库未在本 change 中修改，因此代码回滚不包含数据回滚。
- 若 fresh MySQL 验证失败，保留隔离数据库日志和失败 SQL 位置，修复唯一六文件后重跑；不得从已删除旧目录复制回来绕过失败。
- 若发现仍需非 MySQL 方言，属于 Spec 级范围变化，必须重新取得用户决定，不能在实现中自行恢复目录。

## 6. 人工批准点

1. 执行删除 `<Path>ruoyi-vue-plus-namewta/script/</Path>` 前，需要用户批准实施。
2. 创建后端和父仓库提交前，需要 implementation commit 授权。
3. 推送、打 Tag、部署或修改真实数据库需要分别取得明确授权。
4. 已有数据库差异执行前，必须人工确认源/目标 Tag、备份、隔离演练和回滚/补偿。
