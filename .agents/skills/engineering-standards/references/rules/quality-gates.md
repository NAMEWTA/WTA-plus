# 工具链与质量门禁

### QUALITY-001 使用真实命令

Scope: `repository`

Level: MUST

Source: `repository-fact`

Rule: 只把项目画像列出的 active 命令报告为当前门禁；从相应工作目录运行并记录退出码。依赖存在但无 script/config 不构成 gate；写入式 `pnpm fmt` 不等于 format-check。

Verification: 对照 `package.json`、`pom.xml`、Wrapper 和项目画像；交付报告包含 command、cwd、exit code、result。

### QUALITY-002 变更最小充分验证

Scope: `repository`

Level: MUST

Source: `builder-baseline` + `repository-fact`

Rule: 前端代码至少运行 `pnpm lint` 与 `pnpm build:prod`；后端代码至少运行受影响 Maven reactor 的 compile/package，认证、事务、数据和公共 API 风险另行运行未跳过的测试；跨端合同变化验证两端。

Verification: 项目画像中的真实命令；若因环境/成本未运行，报告为 `not-run` 并说明未验证影响，不能写 passed。

### QUALITY-002A 前端类型补充诊断

Scope: `module:frontend`, TypeScript/Vue contract changes

Level: SHOULD

Source: `repository-fact` (`package.json`, `tsconfig.json`)

Rule: 修改 API 类型、通用 hooks、复杂表单或组件公开合同时，必须运行 `pnpm typecheck`。该 script 执行完整 `vue-tsc --noEmit`，不得用缩小 scope 或跳过诊断制造绿色。

Verification: 在 `frontend` 目录记录命令、退出码和诊断；同时仍执行 `pnpm lint` 与 `pnpm build:prod`，遵循 `PENDING-FE-001`。

### QUALITY-003 不绕过门禁

Scope: `repository`

Level: MUST

Source: `builder-baseline`

Rule: 不为绿色状态删除测试、全局关闭安全/类型/并发规则、放宽 tsconfig/compiler、排除关键模块、吞失败或让命令固定退出 0。局部兼容例外必须进入 decisions/exceptions 并有删除条件。

Verification: config diff review；比较失败前后命令与覆盖范围；严格检查新增 disable/skip/exclude/ignore。

### QUALITY-004 工具链与锁文件稳定

Scope: `module:frontend`, `module:backend`

Level: MUST

Source: `repository-fact`

Rule: 前端使用 pnpm 10 与现有 lockfile，后端使用 `./mvnw` 和 parent/BOM/pluginManagement。非依赖任务不得无意修改 lockfile、版本、Wrapper、BOM 或 Maven profile。

Verification: `git diff -- package.json pnpm-lock.yaml pom.xml .mvn mvnw*`; 构建命令；新增依赖说明许可证、安全、体积/启动和现有替代。

### QUALITY-005 新门禁采用 Ratchet

Scope: `repository`

Level: SHOULD

Source: `builder-baseline`

Rule: 新增 typecheck、format-check、测试、覆盖率或架构检查前先测量基线，再按新代码、变更文件或模块分阶段启用；不得把未确认计划写成 active gate。

Verification: 基线结果、采用 scope、owner 和升级/删除条件写入决策；CI 与本地命令一致。
