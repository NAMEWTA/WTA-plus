---
schema_version: 3
artifact: tickets-map
change: 2026-09-10-notify-channel-config
status: ready
---

# Tickets Map: 通知中心邮件/短信配置管理

- **Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **Ticket 目录：** `<Path>{roots.state}/specdev/changes/{change}/ticket/</Path>`
- **Evidence 目录：** `<Path>{roots.state}/specdev/changes/{change}/evidence/</Path>`
- **可选 Goal Plan：** `<Path>{roots.state}/specdev/changes/{change}/goal-plan.md</Path>`

## 1. 目标与拆分策略

六个 Ticket 共同交付 `US-001`–`US-010` 与 `AC-001`–`AC-018`：先让管理员能配置并启停渠道账号且 YAML 不再提供账号，再让邮件/短信按场景绑定真正发出热配内容，然后加上三层限额，最后迁移调用方并提供测试发送与 Skill 同步。

切片原则：

- 每个 Ticket 都是可观察的垂直切片（页面/API + 发送行为或调用方契约），不是按 DB/后端/前端水平拆分。
- Dispatch 与通知配置页由链路串行修改，避免并行写同一发送内核。
- T-01 删除 YAML 账号后 MAIL/SMS 在无绑定前失败关闭；T-02/T-03 必须紧随，不能把「长期无渠道」当成发布状态。
- 含 Deep Ticket、密钥、YAML 收缩与共享发送路径。Ticket 数量未到 10，但建议实施前运行 Goal Plan 以固定串行 workspace。

### 总体实施背景

- `ruoyi-notify` 是 layered 控制面；`common-mail`/`common-sms` 只按解析后的账号发送；`common-notify` 不建模板中心。
- `NotificationCommand` 不增加 `providerKey`；MAIL/SMS 正文权威在场景绑定。
- 数据库是账号、文案、配额、收件人拦截的唯一运行时权威。
- 查询 GET、变更 POST、POST `@Log`；权限前缀 `notify:config:`。
- DDL 只进 `<Path>release-artifacts/docker/infrastructure/mysql/init/50-namewta-ddl.sql</Path>`，菜单/种子进 `<Path>release-artifacts/docker/infrastructure/mysql/init/60-namewta-dml.sql</Path>`。
- 前端只扩展已组合的 `@namewta/domain-notify` 与 `@namewta/web-domain-notify`。
- IN_APP 与 SYNC 真同步不在本 change。
- 默认 Ticket workspace 为 current：严格串行。

### 项目 Skill 读取矩阵

| Applies To | Project Skill | Trigger / Scope | Read Timing | Purpose |
|---|---|---|---|---|
| ALL | `<Path>.agents/skills/engineering-standards/SKILL.md</Path>` | 架构、权限、API GET/POST、质量门禁 | Map 后、Ticket 前 | 硬约束与模块模式 |
| ALL | `<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>` | 跨层菜单/API/SQL/domain/web-domain | Map 后、Ticket 前 | 垂直切片与合同映射 |
| ALL | `<Path>.agents/skills/ruoyi-module-guide/SKILL.md</Path>` | ruoyi-notify 控制面事实 | Map 后、Ticket 前 | Notify 入口与分层 |
| T-01,T-03,T-04,T-06 | `<Path>.agents/skills/ruoyi-common-modules-guide/SKILL.md</Path>` | mail/sms SPI、Redis 限额、Skill 事实同步 | 进入这些 Ticket 前 | 选择 common 入口，禁止业务直连 SDK；T-06 回写 mail/sms 事实 |

## 2. 执行清单

| ID | Ticket | 可观察产出 | Blocked By | Depth | Risk | Ready | Owner | Contract IDs | Wave/Gate | Status |
|---|---|---|---|---|---|---|---|---|---|---|
| T-01 | `<Path>{roots.state}/specdev/changes/{change}/ticket/01-channel-account-admin.md</Path>` | 通知配置双 TAB 账号 CRUD/启停，YAML 账号退出 | — | deep | high | yes | unassigned | AC-001, AC-002, AC-003, AC-014, AC-017 | W1 | ready |
| T-02 | `<Path>{roots.state}/specdev/changes/{change}/ticket/02-mail-scene-binding.md</Path>` | 邮件场景绑定与热配发送 | T-01 | deep | high | yes | unassigned | AC-005, AC-006 | W2 | ready |
| T-03 | `<Path>{roots.state}/specdev/changes/{change}/ticket/03-sms-template-binding.md</Path>` | 短信模板码绑定与按账号发送 | T-02 | deep | high | yes | unassigned | AC-004, AC-007 | W3 | ready |
| T-04 | `<Path>{roots.state}/specdev/changes/{change}/ticket/04-send-quotas.md</Path>` | 三层限额与 YAML 拦截退出 | T-03 | deep | high | yes | unassigned | AC-008, AC-009, AC-010, AC-015 | W4 | ready |
| T-05 | `<Path>{roots.state}/specdev/changes/{change}/ticket/05-caller-variable-contract.md</Path>` | 调用方只传变量，验证码补 templateCode | T-04 | standard | medium | yes | unassigned | AC-011, AC-012, AC-013 | W5 | ready |
| T-06 | `<Path>{roots.state}/specdev/changes/{change}/ticket/06-test-send-and-skills.md</Path>` | 测试发送与 Skill/注释同步 | T-05 | standard | medium | yes | unassigned | AC-016, AC-018 | W6 | ready |

Ticket frontmatter 是状态、依赖、深度和路径访问契约的权威；本表是同步投影，不得独立修改出另一套真相。

## 3. 依赖 DAG

```text
T-01 [READY] 账号 + YAML 账号退出
  └─→ T-02 [READY] 邮件绑定/热配
        └─→ T-03 [READY] 短信模板绑定
              └─→ T-04 [READY] 限额
                    └─→ T-05 [READY] 调用方
                          └─→ T-06 [READY] 试发 + Skill
```

每条边都是真实开始条件：无账号表不能绑定；无 MAIL 场景表不能做 SMS 绑定列/Dispatch 扩展；无限额语义不能宣称 YAML 拦截已退出；无模板路径不能清调用方正文；无完整发送/限额不能提供同语义测试发送。

## 4. 合同覆盖矩阵

| Contract ID | 覆盖 Ticket | 验证接缝 | 状态 | 说明 |
|---|---|---|---|---|
| AC-001 | T-01 | 菜单 + manifest | covered | 通知配置入口 |
| AC-002 | T-01 | 配置 API | covered | 新增账号 |
| AC-003 | T-01 | 启停 API | covered | 停用 |
| AC-004 | T-03 | Dispatch | covered | 双账号选绑定 |
| AC-005 | T-02 | Dispatch | covered | 缺绑定失败 |
| AC-006 | T-02 | 配置 API | covered | 变量 token |
| AC-007 | T-03 | Dispatch | covered | NotifyTemplateContent |
| AC-008 | T-04 | Dispatch | covered | 账号吞吐 |
| AC-009 | T-04 | Dispatch | covered | 收件人拦截隔离 |
| AC-010 | T-04 | 配置 API | covered | 上限校验 |
| AC-011 | T-05 | caller 契约 | covered | 无硬编码正文 |
| AC-012 | T-05 | Captcha 测试 | covered | templateCode |
| AC-013 | T-05 | 公告/工作流 | covered | 包装变量 |
| AC-014 | T-01 | 配置 API | covered | secret 不回显 |
| AC-015 | T-04 | YAML + 发送 | covered | 库权威 |
| AC-016 | T-06 | 测试发送 API | covered | 试发 |
| AC-017 | T-01 | Controller | covered | GET/POST/@Log |
| AC-018 | T-06 | Skill 校验 | covered | 文档同步 |

## 5. 并行与路径所有权

- implementation subagent 上限来自 `<Path>{roots.state}/specdev/config.json</Path>`。
- 本 DAG 为全串行，current workspace 单 writer。
- Dispatch 与配置页按 T-01→T-06 顺序交接，不并行。

| Ticket A | Ticket B | Writable 交集 | 真实依赖 | 处理 |
|---|---|---|---|---|
| 任意 | 后续 | 有意串行重叠 | 是 | 禁止并行 |

## 6. Gate、Wave 与集成点

- W1：账号可管理且 YAML 账号退出。
- W2/W3：MAIL/SMS 可按绑定发出。
- W4：限额与 YAML 拦截退出。
- W5：调用方合同。
- W6：试发与 Skill，change 可验收。

Goal Plan 权威：`<Path>{roots.state}/specdev/changes/{change}/goal-plan.md</Path>`。Wave=G1–G5 串行；current + direct-parent。未读项目 Skill 不得开始 Ticket。

## 7. 横切契约与风险

- 密钥不回显、不进 `@Log` 请求体。
- 无隐式默认账号；超限与缺绑定失败关闭。
- T-01 之后到 T-02 之前，生产 MAIL/SMS 会失败关闭，不得单独发布 W1。
- Skill 以 T-06 为权威同步点，实现中途发现 Skill 过期时由 Lead 更新 Map。

## 8. 同步规则

- Ticket 状态变化后同步执行清单。
- 以 Ticket 文件为路径与依赖权威。
- 变更后运行 `<Path>{roots.workflows}/specdev/common/tools/validate-specdev.mjs</Path> --stage tickets --repo <Path>.</Path>`。
