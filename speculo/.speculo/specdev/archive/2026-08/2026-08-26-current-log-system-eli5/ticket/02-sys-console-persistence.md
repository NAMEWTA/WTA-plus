---
schema_version: 3
artifact: ticket
change: 2026-08-26-current-log-system-eli5
id: T-02
title: sys-console 单文件持久化
status: done
planning_depth: standard
planning_depth_reason: 改动集中在 ruoyi-admin 的 Logback appender、滚动策略和真实文件输出测试，但需要同时保证专用 HTTP JSON 行与普通文本日志共存。
ready: true
risk: medium
blocked_by: [T-01]
contract_ids: [AC-013, AC-014, AC-015, AC-016]
owner: codex:/root
expected_changes:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/logback-plus.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/logging/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/resources/logging/**</Path>"
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/logback-plus.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/logging/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/resources/logging/**</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-web/src/main/java/org/dromara/common/web/logging/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/pom.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/application.yml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/pom.xml</Path>"
shared_paths: []
shared_path_owners: []
---

# Ticket T-02: sys-console 单文件持久化

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/ticket/02-sys-console-persistence.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/evidence/T-02.md</Path>`

## 1. 战略与来源

- **目标：** 让 ruoyi-admin 只维护一个受控的同步运行日志文件，同时让 T-01 的 HTTP JSON 事件以整行合法 JSON 写入，同文件中的普通系统日志继续保持现有文本格式。
- **可观察产出：** 当前文件路径仍为 `sys-console.log`；每天生成 gzip 归档，最长 60 天且归档总量不超过 40GB；不再创建或写入新的 sys-info/sys-error 文件；已有旧文件不被主动删除。
- **来源：** `US-006`、`AC-013` 至 `AC-016`、`DEC-003`、`DEC-005`、`DEC-006`、`USER-DECISION`、`CODE`。
- **当前事实：** `<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/logback-plus.xml</Path>` 当前同时配置 console、file_console、file_info、file_error 和 async wrapper；sys-console 只保留 1 天且未 gzip，prod profile 的 `org.dromara=WARN` 会抑制普通包下的 INFO HTTP 日志。
- **Planning Depth 原因：** 这是单部署应用内的多文件配置切片，无 schema 或业务 API 迁移，但混合文本/无前缀 JSON 的同文件格式与真实滚动策略需要集成级证据。

## 2. 决策状态

### 已锁定决策

- 只保留终端 appender 和一个直接挂到 root 的同步 RollingFileAppender；不增加 AsyncAppender、队列或丢弃策略。
- 当前文件路径保持 `${log.path}/sys-console.log`；归档文件名按天且以 `.log.gz` 结束，`maxHistory=60`、`totalSizeCap=40GB`。
- file_info、file_error、async_info、async_error 及其 root 引用全部移除；不创建新的 info/error 变体。
- 精确 logger `org.dromara.system.http` 在 local/dev/prod 均显式 INFO，并只沿 root/appender 拓扑写一次；不得因 `org.dromara=WARN` 丢失，也不得因单独重复挂载 appender 产生双写。
- sys-console 的输出接缝按 logger 区分：T-01 专用 logger 的 formatted message 原样成为一条物理 JSON 行；其他 logger 继续使用当前日期、级别、进程、线程、logger、消息和异常文本格式。
- Logback 配置不得主动扫描、删除或迁移部署前已有的 `sys-info*`、`sys-error*` 文件。
- 不在 application YAML 增加日志持久化默认值；路径、压缩、历史和容量由 Logback XML 拥有。

### 已采用的低影响假设

- 归档清理由 Logback TimeBasedRollingPolicy 在正常滚动时执行，不新增启动清理器或后台任务。
- AC-015 使用临时日志目录中的预置旧文件证明初始化和关闭不删除历史；真实服务器的部署前后文件清单仍作为运维 Evidence 记录，不在仓库测试中操作实际日志目录。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| ruoyi-admin Logback appender/logger/rolling policy、混合格式路由、静态 XML 合同和隔离真实文件测试 | T-01 专用 logger/JSON 消息/格式接缝、现有 log.path 属性、Logback RollingFileAppender 和 TimeBasedRollingPolicy | common-web Filter、应用 YAML 默认项、其他部署应用 Logback、旧服务器文件删除、异步文件写入、自建轮转线程 |

## 4. 要构建什么

ruoyi-admin 启动日志系统后，root 的 INFO 及以上普通事件继续输出到终端和同步 sys-console 文件。来自 `org.dromara.system.http` 的 INFO 事件无论 Maven profile 把通用 `org.dromara` 设为 INFO 还是 WARN，都传播到同一个文件 appender 且只写一次；文件格式接缝把其完整 JSON message 原样写成一条物理行，普通事件仍按原文本 pattern 输出。

日切后只为 sys-console 创建 `.log.gz` 历史，Logback 按 60 天和 40GB 双上限删除更旧归档。初始化、写入和关闭过程不触碰预先存在的 info/error 历史文件。

## 5. 实现契约

- **入口或接缝：** ruoyi-admin 的 `logback-plus.xml`；T-01 logger `org.dromara.system.http`；隔离 Logback LoggerContext 和临时 `log.path` 文件目录。
- **输入与输出：** 输入为普通 SLF4J 事件、专用 HTTP JSON message 和 log.path；输出为终端文本、当前 sys-console 文件和按日 gzip 归档。
- **公共接口变化：** 不改变业务 API；改变服务器日志 appender 拓扑、文件集合、HTTP 物理行格式和归档保留策略。
- **不变量：** 同一事件只写一次；sys-console 同步写入；HTTP 文件行无任何前后缀且可单独 JSON parse；普通日志保持现有文本格式；当前文件路径兼容；旧历史文件不删除。
- **状态或数据流：** logger level 判定 -> root 传播一次 -> console 使用现有文本 pattern；file_console 使用混合格式接缝 -> 当前文件 -> TimeBasedRollingPolicy 日切 gzip -> 60 天/40GB 清理。
- **错误与失败行为：** XML 解析或 Logback 初始化错误必须由测试暴露；磁盘/appender 故障沿用 Logback StatusManager/终端报告，不增加业务重试或吞错逻辑。
- **兼容要求：** `${log.path}/sys-console.log` 和非 HTTP 文本格式保持；停止新写 info/error，但保留部署前历史；其他部署应用不受本 Ticket 修改。
- **安全与隐私要求：** 文件会承载 T-01 的凭证原值；本 Ticket 不增删或改写事件内容，并保持 60 天/40GB 上限，不把安全例外扩展到普通日志。

## 6. 执行路线

1. 在 admin 日志测试目录建立 XML 结构断言和隔离 LoggerContext/临时日志目录接缝，先证明当前重复 appender、保留策略和 HTTP 行前缀问题。
2. 收缩 Logback 拓扑为 console + 同步 file_console，配置 sys-console 每日 `.log.gz`、`maxHistory=60` 和 `totalSizeCap=40GB`，删除 info/error/async appender 及引用。
3. 显式配置专用 HTTP logger 为 INFO 且避免重复 appender 绑定，接入 T-01 的文件格式接缝，使 HTTP message 原样成行、普通日志保持文本 pattern。
4. 用包含换行、引号和控制字符的 HTTP body 事件验证真实物理行 JSON 可解析；预置旧 info/error 文件验证初始化/关闭不删除。
5. 运行 admin 定向测试、后端完整测试和 full/core 双 bundle 构建，形成非空 implementation commit 与 Lead Evidence。

## 7. 路径访问契约

- **预计修改点：** frontmatter `expected_changes` 所列 admin Logback XML、日志测试 Java 和可选测试资源。
- **可写范围：** 仅 frontmatter `writable_paths`；若测试需要修改 admin POM、application YAML、common-web 或根 POM，必须停止并走 deviation control。
- **只读上下文：** T-01 logging 合同、admin POM/application YAML 和根 Maven profile。
- **共享路径：** 无；T-01 不写 admin，T-02 是 ruoyi-admin Logback XML 的唯一 owner。
- **保留或不动：** 实际服务器 logs 目录、其他部署应用 Logback、common-web 产品源码和历史 info/error 文件。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| profile 与单次写入 | Logback 配置合同 + 隔离 LoggerContext | cwd `<Path>ruoyi-vue-plus-namewta</Path>`：`./mvnw -pl ruoyi-admin -am test -Dtest='*Logging*Test' -Dsurefire.failIfNoSpecifiedTests=false` | local/dev/prod 等效 level 下专用 logger 为 INFO，每个 HTTP 事件在 sys-console 只写一次 | `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/evidence/T-02.md</Path>` |
| appender 与滚动策略 | XML 解析 + Logback policy 初始化 | 同一定向测试命令，检查 appender 引用和 policy | 仅 console 与同步 file_console；`.log.gz`、60、40GB；无 info/error/async 定义或引用 | `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/evidence/T-02.md</Path>` |
| 物理 JSON 行和普通文本兼容 | 临时目录真实文件 appender | 写入含换行/引号/控制字符的 HTTP 事件和普通事件，逐物理行读取 | HTTP 行无前后缀且 JSON parser 可还原 body；普通行保留既有文本字段 | `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/evidence/T-02.md</Path>` |
| 旧历史非破坏性 | 临时目录预置文件 + 人工部署清单 | 初始化/关闭 LoggerContext 前后比对 `sys-info*`、`sys-error*`；部署时记录文件清单 | 旧文件内容和存在性不变，应用不再创建或追加这些文件 | `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/evidence/T-02.md</Path>` |
| 后端回归与组装 | Maven reactor | cwd `<Path>ruoyi-vue-plus-namewta</Path>`：依次运行 `./mvnw test`、`./mvnw clean package -DskipTests`、`./mvnw clean package -Pbundle-core -Dmaven.test.skip=true` | 测试与 full/core 组装均成功 | `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/evidence/T-02.md</Path>` |

- **Workspace checks：** 默认 Goal Plan 策略为 current；T-01 完成并通过 direct-parent 后，implementation owner 在 current workspace 串行运行定向、reactor 和 bundle 非 E2E 检查，形成 clean、非空 implementation commit。
- **E2E disposition：** not-required：隔离真实 Logback context、真实 RollingFileAppender 和临时文件系统已经覆盖配置到物理日志行及非破坏性文件行为，没有浏览器、远程服务或多进程部署边界。
- **E2E owner/environment：** Lead / current-workspace；本 Ticket 无 required E2E 场景。
- **Integration evidence：** Evidence 记录 T-01 result/base SHA、T-02 implementation commit、direct-parent 验证、父分支 result SHA、实际文件清单、命令和退出状态；Goal Plan 若改为 required，则使用 parent-candidate 证据。

## 9. 发布、迁移与恢复

- **迁移顺序：** T-01 logger 合同先进入父分支，再以本 Ticket 原子替换 ruoyi-admin appender 拓扑和滚动策略。
- **兼容窗口：** 当前 sys-console 路径持续兼容；部署后立即停止 info/error 新写入，无双写窗口；已有历史文件继续保留。
- **监控信号：** Logback StatusManager/终端错误、sys-console 当前文件增长、gzip 归档、HTTP requestId 配对和归档总量。
- **回滚或前向恢复：** 回滚 XML commit 可恢复旧 appender；不会恢复或删除任何历史数据。磁盘压力可先通过 T-01 外部开关停止 HTTP 事件，再评估部署配置。
- **不可逆操作与批准点：** 无；禁止由本 Ticket 删除部署机器历史文件。实际实施与 commit 仍需 I-implement/Goal Plan 的执行授权。
- **收缩条件：** 新配置中 info/error/async appender 定义和 root 引用为零，并由静态 XML 测试证明。

## 10. 验收标准

- [ ] `AC-013`：专用 HTTP logger 在 local/dev/prod 有效配置中均以 INFO 输出且每个事件只写一次。
- [ ] `AC-014`：仅终端和同步 sys-console 文件路径，日归档 gzip、maxHistory 60、totalSizeCap 40GB，无 info/error/async appender。
- [ ] `AC-015`：部署前已有 info/error 历史文件未删除，应用不再创建或追加这些文件，当前 sys-console 路径兼容。
- [ ] `AC-016`：真实文件中每条 HTTP 物理行均可独立 JSON parse，body 可还原且无 Logback 前后缀；普通日志仍为文本格式。
- [ ] 验证矩阵全部执行并记录到 `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/evidence/T-02.md</Path>`。
- [ ] 实际项目修改未超出 `writable_paths`，并形成非空 implementation commit、direct-parent 或适用 candidate 验证及父分支 result SHA。
- [ ] 未发生未批准的历史文件删除、异步写入、YAML 默认项或其他部署应用范围扩张。
