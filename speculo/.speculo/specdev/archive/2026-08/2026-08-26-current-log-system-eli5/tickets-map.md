---
schema_version: 3
artifact: tickets-map
change: 2026-08-26-current-log-system-eli5
status: completed
---

# Tickets Map: 单文件完整 HTTP 系统运行日志

- **Map：** `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/tickets-map.md</Path>`
- **Spec：** `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/spec.md</Path>`
- **Ticket 目录：** `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/ticket/</Path>`
- **Evidence 目录：** `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/evidence/</Path>`
- **可选 Goal Plan：** `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/goal-plan.md</Path>`

## 1. 目标与拆分策略

三个 Ticket 共同交付 `US-001` 至 `US-008`：T-01 以 common-web 的 Servlet Filter 为稳定入口，纵向完成配置、HTTP 请求/响应、加密边界、异常、异步和模块验证；T-02 消费其专用 logger/JSON 消息合同，纵向完成 ruoyi-admin 单文件持久化、混合格式、滚动留存和真实文件验证；T-03 收录后续追加的根路径时间响应与启动运行摘要。

不创建独立 prefactor：现有 Filter 自动配置、common-json 和 Logback 滚动能力已经是可复用接缝。也不把同步、异步、媒体分类或测试拆成水平 Ticket，因为这些行为共享同一个 Filter 状态机和不可分割验证；T-01 虽为 Deep，范围仍限制在一个 common-web 模块并适合单一新上下文。

## 2. 执行清单

| ID | Ticket | 可观察产出 | Blocked By | Depth | Risk | Ready | Owner | Contract IDs | Wave/Gate | Status |
|---|---|---|---|---|---|---|---|---|---|---|
| T-01 | `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/ticket/01-complete-http-system-log.md</Path>` | 完整、可关联、原值、可关闭且异步闭合的 HTTP JSON 日志链路 | — | deep | high | yes | codex:/root | AC-001..AC-012 | Wave 1 / G1 | done |
| T-02 | `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/ticket/02-sys-console-persistence.md</Path>` | ruoyi-admin 唯一同步 sys-console 文件、无前缀 HTTP JSON 行和 60 天/40GB gzip 策略 | T-01 | standard | medium | yes | codex:/root | AC-013..AC-016 | Wave 2 / G2 | done |
| T-03 | `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/ticket/03-admin-runtime-summary.md</Path>` | 根路径固定启动时间/动态访问时间，以及同时进入控制台和 sys-console 的启动摘要 | T-02 | standard | medium | yes | codex:/root | AC-017..AC-019 | Wave 3 / G3 | done |

Ticket frontmatter 是状态、依赖、深度和路径访问契约的权威；本表只做同步投影。

## 3. 依赖 DAG

```text
T-01 [READY, DEEP, HTTP LOGGER CONTRACT OWNER]
  └─→ G0: execution authorization [BLOCKED: no commits]
        └─→ G1: common-web contract + reactor test
        └─→ T-02 [READY, ADMIN LOGBACK OWNER]
              └─→ G2: file integration + full/core package
                    └─→ T-03 [READY, ADMIN RUNTIME SUMMARY OWNER]
                          └─→ G3: controller + startup runtime verification
```

依赖边是真实开始条件：T-02 必须在 T-01 的专用 logger 名、JSON message 和文件格式接缝形成已验证 result 后，才能断言物理文件行为；T-03 必须在 T-02 的 sys-console appender 合同稳定后，才能证明启动摘要同步落盘。

## 4. 合同覆盖矩阵

| Contract ID | 覆盖 Ticket | 验证接缝 | 状态 | 说明 |
|---|---|---|---|---|
| AC-001 | T-01 | Servlet Filter + logger 捕获 | covered | 默认同步请求一对完整 JSON 事件和响应 header |
| AC-002 | T-01 | Servlet Filter 合同 | covered | 客户端 ID 只作为 upstreamRequestId |
| AC-003 | T-01 | CryptoFilter 组合测试 | covered | 解密后请求、加密前响应、客户端密文不变 |
| AC-004 | T-01 | UTF-8 截断单元 + Filter 合同 | covered | 1MiB 日志副本上限和完整 bodyLength |
| AC-005 | T-01 | 安全例外精确合同 | covered | headers/body 凭证原值不改写 |
| AC-006 | T-01 | 媒体分类 + Servlet 合同 | covered | multipart、文件、二进制和 SSE 只记元数据 |
| AC-007 | T-01 | Web 异常集成 | covered | 记录 GlobalExceptionHandler 最终响应 |
| AC-008 | T-01 | Filter 失败路径 | covered | completed=false、异常原样传播、MDC 清理 |
| AC-009 | T-01 | AsyncContext/AsyncListener 合同 | covered | 完成、错误、超时和重复 dispatch 最多一次 |
| AC-010 | T-01 | 故障注入 | covered | 捕获/JSON 失败保持业务语义并报告故障 |
| AC-011 | T-01 | ApplicationContext 条件装配 | covered | 关闭后无 Filter/header/事件/旧日志 |
| AC-012 | T-01 | ApplicationContextRunner 配置绑定 | covered | 非法 DataSize 启动失败 |
| AC-013 | T-02 | profile/logger 配置合同 | covered | 专用 INFO 不受 prod WARN 抑制且无双写 |
| AC-014 | T-02 | XML 解析 + Logback policy | covered | 单同步文件、gzip、60 天、40GB、无旧 appender |
| AC-015 | T-02 | 临时目录 + 部署文件清单 | covered | 停写但不删除已有 info/error 历史 |
| AC-016 | T-02 | 真实文件 appender + JSON parser | covered | HTTP 物理行无前后缀且 body 可还原 |
| AC-017 | T-03 | Controller 单元测试 | covered | 启动时间固定、当前时间按访问刷新 |
| AC-018 | T-03 | 启动类日志捕获 + 真实启动文件检查 | covered | 摘要同时进入控制台和 sys-console，包含启动时间且无成功口号 |
| AC-019 | T-03 | WebServer mock + 真实 18081 启动 | covered | 实际端口、协议、host 和 context path 组成访问地址 |

全部 19 个 Spec 验收合同已覆盖，无 deferred 或 uncovered。

## 5. 并行与路径所有权

- Goal Plan 已固定 workspace 策略为 `current`，三个实现 Ticket 严格串行且禁止创建 worktree；当前用户另行授权非 SpecDev 完成模式直接修改和测试，但 commit 完成门仍停在 G0。
- T-01 唯一拥有 common-web POM、配置、Filter、logging、旧 interceptor、自动配置和模块测试路径。
- T-02 唯一拥有 ruoyi-admin Logback XML 和 admin logging 测试路径。
- T-03 唯一拥有 admin 启动类、根路径 Controller 及对应测试路径。
- 三者没有 writable shared path；专用 logger/JSON 消息属于 T-01 的跨 Ticket 只读合同，ruoyi-admin Logback XML 属于 T-02，T-03 只读消费该 appender 合同。
- Lead 是 SpecDev 状态、Evidence 和父分支 integration owner；implementation owner 在派单前保持 unassigned。

| Ticket A | Ticket B | Writable 交集 | 真实依赖 | 处理 |
|---|---|---|---|---|
| T-01 | T-02 | 无 | 是，T-02 消费 T-01 logger/格式接缝 | Wave 1 完成 G1 后串行进入 Wave 2 |
| T-02 | T-03 | 无 | 是，T-03 消费 T-02 sys-console appender | Wave 2 完成 G2 后串行进入 Wave 3 |

## 6. Gate、Wave 与集成点

- **G0：** 当前仍 blocked。用户后来授权并已完成单一后端 commit/push，但该合并提交不能提供每 Ticket 独立 implementation commit 和 direct-parent result SHA，因此三个 Wave 的正式完成序列无法追溯闭合。
- **Wave 1 / T-01：** G0 关闭后，在 current workspace 实现 common-web HTTP 日志链路并形成 implementation commit。
- **G1：** Lead 核对 T-01 writable paths、运行 common-web 定向测试和 `./mvnw test`，记录 direct-parent result SHA；T-01 未通过时 T-02 不开始。
- **Wave 2 / T-02：** 基于 G1 result 实现 admin Logback 持久化并形成 implementation commit。
- **G2：** Lead 运行真实文件 appender测试、完整 `./mvnw test`、full package 和 core package，记录最终父分支 result SHA。
- **Wave 3 / T-03：** 基于 G2 result 实现首页时间和启动摘要，并形成 implementation commit。
- **G3：** Lead 运行 Controller/启动类测试、真实启动落盘检查和 clean full/core package，记录最终父分支 result SHA。
- T-01/T-02 的 E2E disposition 为 not-required；T-03 使用本机进程级启动冒烟接缝。若风险扩大，必须先走 deviation control 更新 disposition。
- Goal Plan 已写入 `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/goal-plan.md</Path>`：Lead 为 codex、implementation agent 上限 1、integration attempt 上限 4、`current + direct-parent`。其状态为 blocked；只有 G0 的 commit/direct-parent 授权解除后才能进入 I-implement。

## 7. 横切契约与风险

- `ADR-001` 的凭证原值记录只适用于 T-01 HTTP logger；T-02 不得改写事件，也不得扩展到普通系统日志或 `@Log`。
- 专用 logger 名 `org.dromara.system.http`、INFO level 和完整 JSON formatted message 是 T-01 交给 T-02 的稳定合同。
- 请求/响应捕获 fail-open，非法配置 fail-fast；Logback 磁盘错误使用其内部状态报告，不重试业务请求。
- sys-console 同步写入可能增加接口尾延迟，每方向日志副本最多 1MiB；运行期容量/性能止损通过外部 `sys.log.enabled=false`。
- 60 天和 40GB 是归档双上限，任一先达到即可清理；不得承诺最低保留 60 天。
- 不执行历史文件删除；回滚两个 implementation commit 均不需要数据库或文件数据回滚。

## 8. 同步规则

- Ticket 状态变化后同步本 Map；Ticket frontmatter 是依赖、路径和状态权威。
- Goal Plan 存在后，Wave、Gate、owner、workspace 和 integration 顺序以 Goal Plan 为编排权威并投影回本 Map。
- 当前 Goal Plan `ready_for_execution=false`；Ticket 本身仍为 ready，但不得把未提交 diff 或测试结果标记为 done。
- 依赖、合同覆盖、E2E disposition 或路径所有权变化后重新运行 tickets 阶段校验。
- Ticket 完成必须有 Lead Evidence、非空 implementation/source commit、direct-parent 或 candidate 验证及父分支 result SHA；未授权实施、未批准偏差或 Evidence 不完整时不得标记 done。
- 内部 SpecDev 引用继续使用完整根变量 Path 标签，不使用相对 Markdown 链接。
