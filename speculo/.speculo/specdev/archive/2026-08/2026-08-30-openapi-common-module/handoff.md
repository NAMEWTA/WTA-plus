# Handoff：OpenAPI Common Module

- 生成时间：`2026-08-31T22:46:31+08:00`
- Change：`2026-08-30-openapi-common-module`
- 当前状态：`completed`
- 当前 Work：`null`
- Lead：`codex:/root`
- 用户指定持久化位置：<Path>{roots.state}/specdev/changes/2026-08-30-openapi-common-module/handoff.md</Path>

## 接手结论

该 change 已完成本地设计、12 个 Ticket 实现、候选集成、全量验证和 ELI5 解释，不存在实现 blocker。OpenAPI 保持默认关闭，未执行生产 DDL/DML、未下发 KEK、未启用功能、未部署，也没有远程 push/PR/close 动作。

不要重新实现已经完成的能力。下一会话应根据用户新目标选择：阅读讲解、继续开放具体业务接口、做真实基础设施验证、准备生产发布，或进入归档；这些是彼此不同的授权边界。

## 权威工件

按以下顺序恢复上下文：

1. Change 状态与授权边界：<Path>{roots.state}/specdev/changes/2026-08-30-openapi-common-module/.status.json</Path>
2. 完成后的编排权威：<Path>{roots.state}/specdev/changes/2026-08-30-openapi-common-module/goal-plan.md</Path>
3. 外部行为与验收合同：<Path>{roots.state}/specdev/changes/2026-08-30-openapi-common-module/spec.md</Path>
4. 最终发布门禁证据：<Path>{roots.state}/specdev/changes/2026-08-30-openapi-common-module/evidence/T-12.md</Path>
5. Ticket 状态投影：<Path>{roots.state}/specdev/changes/2026-08-30-openapi-common-module/tickets-map.md</Path>
6. 设计决定与术语：<Path>{roots.state}/specdev/changes/2026-08-30-openapi-common-module/ADR.md</Path>、<Path>{roots.state}/specdev/changes/2026-08-30-openapi-common-module/CONTEXT.md</Path>
7. 全面代码与数据流讲解：<Path>{roots.state}/specdev/changes/2026-08-30-openapi-common-module/01_OpenAPI开放平台整体逻辑.md</Path>
8. ELI5 索引：<Path>{roots.state}/specdev/changes/2026-08-30-openapi-common-module/eli_index.md</Path>

该 change 没有 `source.md` 或 `triage.md`；不要虚构这两个工件。初始需求与决策轨迹由 <Path>{roots.state}/specdev/changes/2026-08-30-openapi-common-module/LOG.md</Path>、<Path>{roots.state}/specdev/changes/2026-08-30-openapi-common-module/CONTEXT.md</Path> 和 Spec 承担。

## 已交付范围

- 后端新增默认关闭的 `ruoyi-common-openapi`：方法级 `@OpenApi` 注册、NAMEWTA v1 HMAC-SHA256、Redis nonce/两级限流、Sa-Token 机器会话桥和失败关闭装配。
- `ruoyi-system` 提供唯一凭据、AES-256-GCM 加密、current-user/super-admin 管理 API、全局授权快照、实时目录和授权变化失效。
- 原 `SecurityConfig` 仅对已验证机器身份跳过浏览器 Client 约束，普通 Token/Client 链不变。
- HTTP 日志对 AppKey、AppSecret、签名、Token、Cookie 和敏感 JSON 字段失败关闭脱敏。
- 前端交付同一 domain/web-domain 的双入口：系统管理 target-user scope 与个人中心 current-user scope；AppSecret 只存在于创建/重置后的组件本地一次性状态。
- DDL/DML、菜单权限、full/core bundle、发布开关和恢复说明已纳入交付。

详细实现不要在本文件展开，以 ELI5、Spec、各 Ticket 和 Evidence 为准。

## 固定验证点

| 范围 | OpenAPI 验证结果 | 当前仓库位置 |
| --- | --- | --- |
| Backend | commit `412c2bf1e394042aa841f719a0348b645a26680d`；tree `46ee107ba80046b98c5b698a596a917f4c761a7e` | 当前 `main@c13a375f649cba176ba004d2f45ab2907c0f3574`；验证 commit 是其祖先，工作树 clean |
| Frontend | commit `ea32aa1b1c9911e430f406631199e30589ba007b`；tree `5aebefaebdc1418a05a9677b9298157d8aee6e78` | 当前 `main@4b204f65a822bf080d71d9c90ed430e9467bcf16`；验证 commit 是其祖先，工作树 clean |
| Parent | OpenAPI 最终交付 commit `41d2a30cb4b9ea24a00d870d319fd4ad59f2ebf3` | 当前 HEAD 仍为该 commit；工作树包含本次 ELI5/状态及其他用户改动 |

Backend 和 frontend 当前 HEAD 只在已验证固定点之后增加了忽略 SpecDev worktree 的提交。若后续修改 OpenAPI，不得把旧 T-12 证据自动套用到新 HEAD；应按实际 diff 重新选择门禁。

最终验证命令、测试数量、候选固定点、偏差和双轴审查均记录在 <Path>{roots.state}/specdev/changes/2026-08-30-openapi-common-module/evidence/T-12.md</Path>，不要从本交接文档推断更多验证结论。

## 本会话新增但未提交

- <Path>{roots.state}/specdev/changes/2026-08-30-openapi-common-module/01_OpenAPI开放平台整体逻辑.md</Path>
- <Path>{roots.state}/specdev/changes/2026-08-30-openapi-common-module/eli_index.md</Path>
- <Path>{roots.state}/specdev/changes/2026-08-30-openapi-common-module/handoff.md</Path>
- <Path>{roots.state}/specdev/changes/2026-08-30-openapi-common-module/.status.json</Path> 已将 `specdev/eli5` 追加到 `works_run` 并清空 `current_work`。
- <Path>{roots.state}/specdev/status.json</Path> 已同步该 change 的 ELI5 完成状态。

ELI5 校验已经通过：`validate-specdev.mjs --stage eli5` 返回 `0 error(s), 0 warning(s)`。本会话未修改产品代码，也未重新运行产品测试。

父工作树还存在与本 change 无关的 archive、ADR、context、其他 change 和 skill 改动。它们属于用户或其他工作流；接手时不得还原、暂存或吸收到 OpenAPI 提交中。

## 关键边界与残余风险

1. 当前生产源码没有业务方法标注 `@OpenApi`；注解只存在于测试中。因此启用平台后生产目录仍为空，直到业务明确选择 Client 无关的方法开放。
2. 真实 MySQL 唯一竞争、真实 Redis 原子行为、多节点 Session 失效、KEK 运行环境和完整 Playwright 没有被本 change 的 test-double 门禁证明。
3. 生产 DDL/DML、数据库备份、secret provider、KEK/version 下发、`OPENAPI_ENABLED=true`、部署和回滚演练均未执行。
4. HMAC 提供完整性和来源认证，不提供传输机密性；生产调用应使用 HTTPS。
5. nonce 防重放不等于业务幂等；写接口仍需业务唯一约束或 Idempotency-Key。

完整残余风险以 <Path>{roots.state}/specdev/changes/2026-08-30-openapi-common-module/evidence/T-12.md</Path> 第 9 节和 Spec 风险章节为准。

## 授权状态

- 已授权并完成：Ticket implementation commits、Lead 本地 candidate integration 和父分支更新。
- 未授权：source branch/worktree cleanup、push、PR、远程 merge、部署、生产 DDL/DML、KEK 下发和启用 OpenAPI。
- `.status.json` 中没有 `external_action=pending-close` 或 `close-failed`，也没有远程 locator；不要假设存在待关闭远程 Issue。

任何生产或远程副作用都必须取得新的明确授权。历史“明确授权两项”不能扩展到这些动作。

## 建议 skills

- `engineering-standards`：任何代码、依赖、SQL、测试或交付修改前先加载。
- `ruoyi-common-modules-guide`：继续修改 `ruoyi-common-openapi`、网关、SPI、Redis 或 Sa-Token 桥时加载。
- `namewta-fullstack-development`：修改 admin/system Controller、Service、Mapper、事务、SQL 或后端门禁时加载。
- `ruoyi-system-module-guide`：修改凭据、授权快照、system API 边界或跨模块调用时加载。
- `namewta-fullstack-development`：修改双入口 UI、system domain/web-domain、manifest、动态菜单或权限组合时加载。

不要加载 workflow、AI、upstream 等与新目标无关的 skills。

## 建议恢复动作

新 agent 应先读取工作区 `AGENTS.md`、Speculo 初始化文件和 SpecDev INDEX，再读取本文件列出的状态、Goal Plan、T-12 Evidence 与 ELI5。

根据新目标继续：

- 仅理解系统：从 ELI5 开始，再按其中“源码导航”定位实现。
- 开放首个业务接口：先确认方法不依赖唯一 Client，再形成新的 Spec/Ticket 或明确小范围实现授权；不要直接批量给 Controller 加注解。
- 做真实环境验证：新建独立验证范围，明确 MySQL、Redis、多节点、浏览器和测试数据边界，不改写 T-12 的历史结论。
- 准备生产启用：先获得数据库迁移、secret provider、部署和 enable 的逐项授权，并遵守 `false -> 迁移/配置 -> 单独 enable` 顺序。
- 归档 change：进入 SpecDev `A-archive-and-consolidate`，先执行其完成、远程 reconcile、知识提升和用户确认门；本交接本身不授权移动归档。

## 完成定义

本次 handoff 的恢复点是：OpenAPI change 本地完成，代码固定点已验证，ELI5 已生成并通过校验，状态已清空，没有正在运行的 Work。下一位 agent 不需要恢复一个中断中的实现任务，只需要围绕用户的新目标选择新的工作流和授权边界。
