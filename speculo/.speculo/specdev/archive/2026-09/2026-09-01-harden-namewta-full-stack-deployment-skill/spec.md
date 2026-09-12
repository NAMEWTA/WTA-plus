---
schema_version: 3
artifact: spec
change: 2026-09-01-harden-namewta-full-stack-deployment-skill
status: ready
ready_for_tickets: true
sources:
  - USER-DECISION:2026-09-01-execute-confirmed-plan
  - EVIDENCE:2026-09-01-admin-runtime-capability-reconciliation/T-01
  - EVIDENCE:2026-09-01-admin-runtime-capability-reconciliation/T-02
---

# Spec: 强化 NAMEWTA 全栈部署 Skill

- **Spec：** `<Path>{roots.state}/specdev/changes/2026-09-01-harden-namewta-full-stack-deployment-skill/spec.md</Path>`
- **当前 ADR：** 不适用，本 change 不改变产品架构。
- **当前领域上下文：** 复用已完成部署 Evidence 中的术语和最终态。

## 1. 问题与目标

### 问题陈述

当前 Skill 有安全的高层流程，但配置档案、状态验证和报告无法判定完整发布身份与逐实例最终态。执行者仍可能选错 Compose project、遗漏 override、等待重启循环、只看 HTTP 200、用错前端 context path，或因一次瞬时 502 错误回滚。

### 目标用户与场景

NAMEWTA 部署执行者在接管、升级、生产发布和回滚时，需要从一个非敏感 profile 得到精确发布合同，并用机器可读状态证明后端、前端、数据库和恢复边界均可接受。

### 成功标准

- profile v2 固定 Compose 身份、镜像、前端构建参数、能力开关和滚动策略。
- v1 仍可用于旧的审计/本地配置流程，v2 发布候选必须通过强化门禁。
- 状态工具能拒绝重启、镜像漂移、业务码错误、错误资源前缀和未达到稳定窗口的候选。
- 报告能重建发布尝试、备份或明确 waiver、回滚资产及最终构件身份。
- Skill 主文件保持精简，完整运行步骤进入一级 reference，重复检查进入带测试的脚本。

### 非目标

- 不重新部署、不连接服务器、不修改数据库或运行 secret。
- 不改变 `release-artifacts` 的产品发布合同。
- 不把本次开发库无备份决定扩展成默认策略。

## 2. 解决方案与外部行为

### 解决方案摘要

将 profile/state 扩展为 schema v2，并提供纯本地的候选状态与前端 HTML 校验。Skill 引导执行者先锁定现场 Compose 身份，再按 server1、server2、frontend 顺序发布；每一步必须通过快速失败和稳定窗口。报告保留完整但脱敏的发布证据。

### 主要流程

1. 盘点并固定 project、Compose 文件顺序、env 文件、服务名、镜像和探针地址。
2. 校验 profile，构建不可变前后端产物并检查前端资源前缀。
3. 完成备份，或仅在明确开发环境 waiver 下通过增强 preflight。
4. 逐个滚动后端，持续观察状态、重启次数、日志和业务语义。
5. 原子发布前端，在有上限窗口内等待连续成功。
6. 校验机器状态并生成含恢复资产的私密报告。

### 边界、失败与稳定错误行为

- v2 缺少精确 Compose 身份、镜像、前端合同或 rollout 参数时 profile 校验失败。
- 任一后端 restart count 超限、image ID/tag 不一致或能力摘要漂移时候选失败。
- 业务码不在每个探针显式允许集合时失败；不能把 HTTP 200 等同于业务正确。
- 前端资源不在配置的 asset prefix 下，或连续成功数不足时失败。
- 数据库发生变更且无已验证备份时默认失败；仅 `dev` 可使用显式授权、完整 preflight 和 forward-only 恢复的 waiver。

### 状态转换与不变量

`discovered -> validated -> backend1-stable -> backend2-stable -> frontend-stable -> accepted`。后一步不得在前一步失败时开始。secret 只验证存在性或摘要，不写入状态、报告 Evidence 或标准输出。

## 3. 用户故事

- **US-001**：作为部署执行者，我希望发布身份被机器锁定，以免操作错误 Compose 项目或遗漏覆盖文件。
- **US-002**：作为值班人员，我希望候选验证能快速区分重启循环、业务错误与短暂入口抖动。
- **US-003**：作为前端发布者，我希望构建产物在传输前证明 context、API 和资源路径正确。
- **US-004**：作为交接人员，我希望报告可以重建发布、失败、回滚和数据保护决策。

## 4. 验收合同

| ID | 前置条件 | 动作或事件 | 可观察结果 | 验证接缝 |
|---|---|---|---|---|
| AC-001 | profile schema v2 | 校验候选配置 | 精确 Compose、镜像、前端和 rollout 合同缺失或矛盾时失败 | `validateProfile` 单元测试 |
| AC-002 | 旧 schema v1 profile | 执行原有校验/渲染 | 保持可读取，不静默获得 v2 rollout 通过 | 兼容测试 |
| AC-003 | 双实例候选状态 | 验证 state v2 | restart、image、配置或 OpenAPI 漂移稳定失败 | `verifyState` 单元测试 |
| AC-004 | HTTP 与业务响应 | 验证语义探针 | target 覆盖、认证探针和允许业务码均可判定 | 状态夹具测试 |
| AC-005 | Vite `index.html` | 校验前端产物 | 根路径或错误前缀资源失败，正确 `/namewta/assets/` 通过 | 前端产物 CLI/单元测试 |
| AC-006 | LB 切换存在短暂失败 | 验证稳定窗口 | 单次成功不足以通过，达到连续成功阈值后允许历史瞬时 502 | 状态夹具测试 |
| AC-007 | 发生数据库迁移 | 验证保护合同 | 默认要求 verified backup；dev waiver 仅在显式授权、完整 preflight 和 forward-only 时通过 | 正负向测试 |
| AC-008 | 完整 v2 状态 | 生成私密报告 | 报告含 Compose、镜像、构件、尝试、前端、数据保护和保留恢复资产 | 报告测试 |
| AC-009 | Agent 使用部署 Skill | 读取主 Skill 与 reference | 高风险步骤有单一运行手册，主 Skill 保持短小且不复制细节 | 文档/链接审查 |
| AC-010 | 所有工具修改完成 | 运行回归 | 既有安全、OSS、Nacos、权限和无 secret stdout 合同不回归 | Node 全测试与 diff 审查 |

## 5. 范围

### IN

- `<Path>.agents/skills/deploy-namewta-environment/**</Path>` 中的 Skill、一级 references、Node 脚本、模板和测试。
- 本 change 的 Speculo 工件与 Evidence。

### REUSE

- `<Path>release-artifacts/scripts/release-manage.sh</Path>` 的前端 build 参数和发布清单。
- `<Path>release-artifacts/docker/**</Path>` 的 Compose/OpenAPI/Nacos 默认合同。
- 上一 change 的 T-01/T-02 部署 Evidence。

### OUT

- **OOS-001**：服务器、数据库、中间件和 DNS/TLS 写操作。
- **OOS-002**：前后端产品源码或 release Compose 修改。
- **OOS-003**：清理旧镜像、失败候选、rollback 目录或其他用户工作树改动。

## 6. 已锁定实现约束

- **DEC-001**：profile v2 是强化 rollout 合同；v1 保留读取兼容但不能宣称具有 v2 证据。来源：已批准计划。
- **DEC-002**：主 `SKILL.md` 保持核心路由，详细步骤下沉一级 reference，确定性验证进入 Node 脚本。来源：Skill Creator。
- **DEC-003**：稳定窗口以连续成功数判定，允许窗口内记录瞬时传输失败。来源：实际瞬时 502 Evidence。
- **DEC-004**：备份仍是默认硬门；waiver 只允许明确开发目标、明确授权和强化 preflight，不适用于生产。来源：T-01 与安全边界。

## 7. 数据、接口与兼容

- **公共接口变化：** 部署工具 profile/state JSON 合同新增 schema v2；现有 CLI 参数保持，新增候选与前端校验 CLI。
- **数据模型与持久化：** 只修改模板和本地状态格式，不触碰业务数据。
- **兼容要求：** `validateProfile` 和本地渲染继续接受 v1；严格 rollout/report 使用 v2。
- **迁移要求：** 新部署从 v2 模板创建；既有 v1 profile 可继续审计，升级到 v2 后才能使用候选验收。
- **发布或运维影响：** Skill 的未来执行流程更严格，本 change 自身无外部发布。

## 8. 非功能要求

- **NFR-001 安全与隐私：** secret 只出现在权限 `0600` 的本地私密文件，状态和 stdout 不回显。
- **NFR-002 性能与容量：** 本地校验为线性文件/JSON 处理，不引入网络和第三方依赖。
- **NFR-003 可用性与可靠性：** 每个失败指出精确合同；历史瞬时故障不覆盖最终稳定窗口。
- **NFR-004 可观测性与运营：** 报告记录构件身份、逐步尝试、失败原因、保留资产和恢复边界。

## 9. 验证策略

| 接缝 | 层级 | 覆盖合同 | 现有先例或命令 | Evidence 类型 |
|---|---|---|---|---|
| profile/state 纯函数 | Node unit | AC-001..004、AC-006、AC-007 | `node --test .agents/skills/deploy-namewta-environment/scripts/deployment-tools.test.mjs` | TAP 红绿记录 |
| 前端 HTML CLI | Node unit/CLI | AC-005 | 同一测试文件与临时 fixture | TAP 与退出码 |
| 私密配置/报告 | Node integration | AC-008、AC-010 | 临时目录生成测试 | 内容断言、权限与 stdout |
| Skill/reference | 静态 review | AC-009、AC-010 | `rg`、链接检查、`git diff --check` | 路径与 diff 结果 |
| SpecDev | schema | 全部 | `validate-specdev.mjs --stage implement` | validator 输出 |

## 10. 风险、假设与未决问题

### 风险

- v2 约束过紧可能阻止合法现场；通过显式 profile 字段而非硬编码目录名降低风险。
- 仅解析 `index.html` 无法证明浏览器业务完整；Skill 仍要求登录语义验收，工具只负责构件前缀。
- waiver 被误用会扩大数据风险；生产环境始终拒绝 waiver。

### 已采用的低影响假设

- Node 标准库足以解析静态 Vite `script/link` 资源，不新增依赖。
- 当前工作区采用 `current/direct-parent`，严格串行且不连接外部环境。

### 未决问题

无。
