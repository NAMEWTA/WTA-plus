---
schema_version: 3
artifact: spec
change: 2026-08-24-upstream-fork-upgrade-remediation
status: ready
ready_for_tickets: true
sources:
  - USER-DECISION:修复两份上游 Fork 代码审查中的问题
  - REVIEW:CR-001
  - REVIEW:CR-002
---

# Spec: 上游 Fork 升级整改

## 1. 问题与目标

当前实现的业务架构可用，但聚合快照、默认测试、CI、前端类型检查、真实外部依赖验证、上游镜像、部署组合、数据库支持口径和占位业务入口尚未形成可信交付闭环。本 change 的目标是将所有可本地闭环的审查发现转为自动化合同，并将需要提交或远程动作的部分明确停在可审查候选状态。

非目标：实现数据采集、数超大赛、Token 中继和管理台业务；增加非 MySQL 方言；推送、部署或修改远程分支。

## 2. 解决方案与外部行为

- 开发者可通过稳定脚本执行 frontend lint/typecheck/test/build 与 backend test/package。
- CI 在固定子模块 SHA 上执行同一组命令，并用一次性 Redis、MySQL、MinIO 验证外部数据面。
- `bundle-full` 与 `bundle-core` 都是可选择、可构建且有文档的部署合同。
- NAMEWTA 明确只支持 MySQL；未完成业务菜单在 fresh/upgrade 执行后均不可见。
- 本地上游镜像和产品实现吸收已确认的非重叠 SQL 日志优化，热点清单和模块知识与代码一致。

## 3. 用户故事

- **US-001**：作为维护者，我希望父仓快照和 CI 可复现，以便每次 fork 同步都有可信基线。
- **US-002**：作为前端开发者，我希望 typecheck 和测试是稳定门禁，以便新增回归不会被存量路径大小写问题掩盖。
- **US-003**：作为部署维护者，我希望支持矩阵和 bundle 明确，以便不会把未验证组合当成产品能力。
- **US-004**：作为最终用户，我不应进入没有业务能力的空白工作台。

## 4. 验收合同

| ID | 动作或事件 | 可观察结果 | 验证接缝 |
|---|---|---|---|
| AC-001 | 运行前端质量命令 | lint、typecheck、Vitest、生产构建全部退出 0，路径大小写唯一 | pnpm scripts |
| AC-002 | 运行后端默认测试/构建 | Wrapper 可直接执行，默认 test 不被根属性跳过，full/core 均可打包 | Maven Wrapper |
| AC-003 | CI 拉取父仓 | 子模块 SHA 无漂移，前后端门禁与真实 Redis/MySQL/MinIO 测试自动执行 | GitHub Actions |
| AC-004 | 执行 NAMEWTA SQL 并构建前端 | 产品只声明 MySQL；四个占位组件被删除，其中三个既有菜单及默认角色关联被下线 | SQL review + MySQL test + frontend inventory |
| AC-005 | 比较缓存上游 | 产品包含 `2933bad` 的非重叠优化，本地 6.X 指向该 SHA | Git refs + source diff |
| AC-006 | 查阅维护知识 | common 数量、reactor、通知模块、热点和同步命令与当前代码一致 | deterministic inventory |

## 5. 范围

IN：父仓 CI/脚本/工程知识；前端工具链、路径大小写、浏览器验证和占位页面；后端 Maven、Wrapper、上游补丁、真实集成测试、SQL/部署文档。

REUSE：现有 129 个后端测试、4 个 Vitest、Redis/MySQL integration tests、OSS client、Git submodule 模型和 append-only NAMEWTA SQL。

OUT：远程 push/PR、生产迁移、其他数据库方言、占位业务功能、全仓类型严格化和无关格式化。

## 6. 已锁定实现约束

- **DEC-001**：MySQL-only 是产品不变量；避免维护无验证的多方言表象。
- **DEC-002**：占位业务采用下线而非假实现；SQL 通过新 append-only 块前向修复。
- **DEC-003**：CI 使用与本地相同的项目脚本，不用排除或 skip 制造绿色。
- **DEC-004**：当前 workspace 严格串行；现有用户脏改动保持不动。

## 7. 数据、接口与兼容

- 公共 HTTP/Java 接口不变。
- SQL 只追加禁用菜单和移除默认角色菜单关联的幂等块。
- 由于这是无历史负担基座，不保留占位路由兼容；既有环境通过同一追加块收敛。
- 发布前需要在各子仓形成逻辑提交，再由父仓单独记录指针；本轮未获提交授权。

## 8. 非功能要求

- 安全：Client 隔离与 OSS/通知授权合同不变，真实依赖测试不得提交 secret。
- 可靠性：测试拥有并清理临时 key/table/object；CI 服务只用于一次性验证。
- 可观测性：每个门禁记录命令、退出码、跳过数和残余风险。

## 9. 验证策略

| 接缝 | 层级 | 覆盖合同 | 命令 |
|---|---|---|---|
| 前端 scripts | static/unit/build | AC-001 | `pnpm lint && pnpm typecheck && pnpm test && pnpm build:prod` |
| Maven profiles | unit/package | AC-002 | `./mvnw test`、`./mvnw package -Pbundle-core` |
| 外部服务 | integration | AC-003 | Maven properties + disposable Redis/MySQL/MinIO |
| SQL/menu | migration | AC-004 | isolated MySQL + deterministic SQL assertions |
| Git/knowledge | repository | AC-005, AC-006 | branch/SHA/count scripts |

## 10. 风险、假设与未决问题

风险：父仓只有在后续提交子模块指针后才完全可复现；GitHub 网络不可用时只能以缓存 upstream 固定点实施；完整生产浏览器登录仍需部署环境。

已采用假设：GitHub Actions 是目标 CI；缓存的 `2933bad` 是本轮后端同步固定点；本地实现完成但未经授权不创建 commit。

未决问题：无。
