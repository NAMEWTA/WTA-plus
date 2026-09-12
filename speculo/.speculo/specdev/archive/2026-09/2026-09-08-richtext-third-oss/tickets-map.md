---
schema_version: 3
artifact: tickets-map
change: 2026-09-08-richtext-third-oss
status: completed
---

# Tickets Map: 富文本、三方调用明细/统计、OSS 直传稳定性修复

- **Map：** `<Path>{roots.state}/specdev/changes/2026-09-08-richtext-third-oss/tickets-map.md</Path>`
- **Spec：** `<Path>{roots.state}/specdev/changes/2026-09-08-richtext-third-oss/spec.md</Path>`
- **Ticket 目录：** `<Path>{roots.state}/specdev/changes/2026-09-08-richtext-third-oss/ticket/</Path>`
- **Evidence 目录：** `<Path>{roots.state}/specdev/changes/2026-09-08-richtext-third-oss/evidence/</Path>`
- **可选 Goal Plan：** `<Path>{roots.state}/specdev/changes/2026-09-08-richtext-third-oss/goal-plan.md</Path>`

## 1. 目标与拆分策略

本 change 解决三个彼此独立的症状：富文本演示页的上下文缺失、三方调用明细/统计的空筛选失败、以及 OSS 开发环境对前端 Origin/IP 的过度绑定。拆分原则是每张 Ticket 只交付一个可观察行为，并把验证接缝尽量压在各自 owner 模块内。

### 总体实施背景

- 富文本 Ticket 需要同时看懂前端页、demo 服务和登录会话中的 Client 语义。
- 三方 Ticket 需要理解 controller -> service -> mapper 的查询链路，以及 `tansParams()` 会吞掉空字符串这一前端行为。
- OSS Ticket 需要理解 MinIO CORS、`domainUrl` / `endpoint` 分离，以及 release-artifacts 里对本地开发环境的默认约定。

### 项目 Skill 读取矩阵

每个 Ticket 的 Lead 或 implementation subagent 都必须先完整读取本 Map，再读取表中适用于 `ALL` 或当前 Ticket ID 的项目 Skill。

| Applies To | Project Skill | Trigger / Scope | Read Timing | Purpose |
|---|---|---|---|---|
| ALL | `<Path>.agents/skills/engineering-standards/SKILL.md</Path>` | 代码、配置、测试和交付门禁 | Map 后、Ticket 前 | 全局架构、模块边界与质量门禁 |
| T-01 | `<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>` | 富文本页面 + demo 服务的前后端联动 | Map 后、Ticket 前 | 对齐页面、runtime、服务和验证顺序 |
| T-01, T-02 | `<Path>.agents/skills/ruoyi-module-guide/SKILL.md</Path>` | `ruoyi-demo` / `ruoyi-third` 模块边界与现有接口事实 | Map 后、Ticket 前 | 保持各自模块内部合同和验证入口 |
| T-03 | `<Path>.agents/skills/ruoyi-common-modules-guide/SKILL.md</Path>` | OSS 共用语义、`domainUrl` / `endpoint` 说明与部署配置 | Map 后、Ticket 前 | 把 Common OSS 合同和 release-artifacts 对齐 |

## 2. 执行清单

| ID | Ticket | 可观察产出 | Blocked By | Depth | Risk | Ready | Owner | Contract IDs | Wave/Gate | Status |
|---|---|---|---|---|---|---|---|---|---|---|
| T-01 | `<Path>{roots.state}/specdev/changes/2026-09-08-richtext-third-oss/ticket/01-richtext-client-context-stability.md</Path>` | 富文本页不再因缺少 Client 抛未处理异常 | — | standard | medium | yes | grok-build | AC-001 | — | done |
| T-02 | `<Path>{roots.state}/specdev/changes/2026-09-08-richtext-third-oss/ticket/02-third-observability-optional-provider-code.md</Path>` | 调用明细/统计空筛选可直接返回最近数据 | — | deep | medium | yes | grok-build | AC-002 | — | done |
| T-03 | `<Path>{roots.state}/specdev/changes/2026-09-08-richtext-third-oss/ticket/03-oss-dev-cors-default.md</Path>` | 开发环境不再依赖固定前端 IP 才能通过 OSS CORS | — | deep | high | yes | grok-build | AC-003 | — | done |

## 3. 依赖 DAG

```text
T-01 [DONE]
T-02 [DONE]
T-03 [DONE]
```

三张 Ticket 彼此独立，没有真实阻塞边。若后续实现阶段发现共享写路径，先把该路径提到专门 owner Ticket，再回写 Map。

## 4. 合同覆盖矩阵

| Contract ID | 覆盖 Ticket | 验证接缝 | 状态 | 说明 |
|---|---|---|---|---|
| AC-001 | T-01 | 富文本页面 + demo service | covered | 上下文缺失时返回可诊断错误，并由页面捕获 |
| AC-002 | T-02 | third controller/service + 现有 Playwright | covered | blank providerCode 视为不筛选 |
| AC-003 | T-03 | release-artifacts + 浏览器预检 | covered | 开发环境默认不再绑定固定前端 IP |

## 5. 并行与路径所有权

| Ticket A | Ticket B | Writable 交集 | 真实依赖 | 处理 |
|---|---|---|---|---|
| T-01 | T-02 | 无 | 否 | 可并行 |
| T-01 | T-03 | 无 | 否 | 可并行 |
| T-02 | T-03 | 无 | 否 | 可并行 |

shared path 不需要专门 owner；每张 Ticket 只写自己的模块或文档路径。

## 6. Gate、Wave 与集成点

当前 change 不需要 Goal Plan。三个 Ticket 都是可独立验证的直切片，没有跨 Ticket 写入协调、迁移批次或 shared owner 冲突。

## 7. 横切契约与风险

- 富文本 Ticket 必须保留当前用户 + Client 的隔离语义。
- 三方 Ticket 必须兼容已有查询参数，并把空值解释为无筛选。
- OSS Ticket 必须把宽松 CORS 限定在开发/联调约定里，生产仍通过显式白名单和稳定域名收口。

## 8. 同步规则

- Ticket 状态变化后同步执行清单。
- 如果实现中发现新的共享路径或需要串行写入，再回写 Map 和 ticket frontmatter。
- 依赖、合同覆盖或路径所有权变化后运行 `<Path>{roots.workflows}/specdev/common/tools/validate-specdev.mjs</Path>`。
- 内部工件不得使用相对 Markdown 链接。
