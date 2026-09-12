# 安全、配置与数据

### SEC-001 Client 隔离不变量

Scope: authentication, authorization, Client, role, menu, registration and session paths

Level: MUST

Source: `repository-fact` (`docs/upstream/customization-map.md`)

Rule: 权限、路由、角色和会话始终限定当前 Client；缺少 Token `clientPk` 时拒绝而非回退全局数据；超管也不能跨 Client；前端筛选或隐藏不是授权边界。

Verification: customization map review；跨 Client 负向测试；检查 mapper/service 查询同时包含用户与 Client 上下文。

### SEC-002 Client 标识语义

Scope: backend and frontend Client contracts

Level: MUST

Source: `repository-fact` (`docs/upstream/customization-map.md`)

Rule: 登录/注册 body 与 `clientid` header 的 `clientId` 是 OAuth 字符串；Token extra 的 `clientPk` 以及角色/菜单/RBAC JSON 中的 `clientId` 是 `sys_client.id` Long PK。命名相同不能互换、隐式解析或无证据强转。

Verification: 对照 BO/VO/TS types、controller、LoginUser 与 mapper 参数；契约测试覆盖错误标识被拒绝。

### SEC-003 Secret 与公开配置

Scope: `repository`, browser bundle, Spring configuration

Level: MUST

Source: `repository-fact` + `builder-baseline`

Rule: secret 仅由批准的环境/secret provider 注入，不提交、不进浏览器 bundle、不写日志。`/auth/client/context` 只返回 client/register enabled 状态，不泄露 client secret、IP whitelist、accessPath 或超时策略；所有 `VITE_*` 值视为公开。

Verification: secret scan/review；检查 controller VO 与前端 env 使用；构建产物抽查；日志/错误响应审查。

### SEC-004 SQL 与数据迁移

Scope: `path:release-artifacts/docker/infrastructure/mysql/init/**`, persistence contracts

Level: MUST

Source: `repository-fact` (`docs/upstream/customization-map.md`)

Rule: 项目只支持 MySQL 8.4；六份完整基座只由父仓库 `release-artifacts/docker/infrastructure/mysql/init/` 拥有并直接修改。`50-namewta-ddl.sql` 只含结构语句，`60-namewta-dml.sql` 只含数据语句；后端仓库不得保存 SQL 副本或其他方言。全新环境按数字前缀执行六份文件；已有环境不得重放基座，必须指定源/目标 Git Tag、完成备份、差异评审和隔离演练。新建项目自有表还必须遵循[数据源事务与建表](../java/persistence-transactions-and-ddl.md)中的命名、主键、基础字段和中文注释合同；破坏性变更提供 expand/migrate/contract 或明确回滚。

Verification: 检查六文件精确清单、Git 跟踪、DDL/DML 分类和后端无副本；在隔离数据库验证全新初始化，并按源/目标 Tag 差异验证存量升级和回滚/补偿路径。

### SEC-005 外部内容与输出

Scope: HTTP, upload, rich text, URL, redirect, SQL/query and template boundaries

Level: MUST

Source: `repository-fact` + `builder-baseline`

Rule: SQL/查询使用参数化 API；路径限定受控根；HTML/富文本/URL 按上下文消毒或转义；上传验证类型、大小和内容；日志与错误不泄露内部实现或敏感数据。

Verification: 负向输入测试；review mapper/query builder、`v-html`/sanitize、upload 和 redirect 使用；安全敏感 diff 精确检查。
