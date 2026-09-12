# 测试策略

### TEST-001 风险驱动测试

Scope: `repository`

Level: MUST

Source: `builder-baseline` + `repository-fact`

Rule: 缺陷修复提供能复现原症状的回归测试；纯逻辑用单元测试，Spring Web/Data/Security 或 Vue component 用适合的 slice/component 层，数据库/缓存/协议用集成测试，关键跨端身份与权限路径使用端到端或明确人工矩阵。

Verification: review 测试为何选择该层级；运行适用测试命令并记录退出码；无现成 harness 时记录未验证影响和可复现人工步骤。

### TEST-002 认证与权限矩阵

Scope: authentication, authorization, Client, role, menu, registration, user type and session changes

Level: MUST

Source: `repository-fact` (`docs/upstream/customization-map.md`, `plan/update.md`)

Rule: 覆盖多 Client、多登录域、默认/显式角色、超管、缺失 `clientPk`、跨 Client 越权、注册开关、Token 失效和前端伪造字段。只测正常路径不充分。

Verification: 自动测试或 `plan/update.md` 人工验收矩阵；核对后端拒绝路径和前端呈现；记录数据库与 Token 的可观察结果。

### TEST-003 测试隔离与清理

Scope: `repository`

Level: MUST

Source: `builder-baseline`

Rule: 测试拥有并清理其文件、端口、数据库数据、容器、timer、mock 与全局 Pinia/Spring context 状态；时间、随机、网络和并发行为可控且可重复。

Verification: 重复/并行运行相关测试；review teardown、fixture 和事务回滚；检查测试不依赖开发者本地隐式状态。

### TEST-004 不夸大当前门禁

Scope: `module:frontend`, `module:backend`

Level: MUST

Source: `repository-fact`

Rule: 不得凭配置或测试文件存在就报告通过。前端分别实际运行 lint、typecheck、Vitest、Playwright 与生产构建；后端分别运行默认 `./mvnw test`、`bundle-full` 与 `bundle-core` 打包。属性门控的 Redis/MySQL/MinIO 测试只有在真实服务参数存在且用例未 skipped 时，才能报告真实集成通过。

Verification: 交付报告列出 command、working directory、exit code、执行/跳过数量；对照 `package.json` scripts、root `pom.xml`、CI workflow 与外部服务脚本。远程 CI 未运行或分支保护未启用时必须明确说明。
