---
schema_version: 3
artifact: ticket
change: 2026-08-26-current-log-system-eli5
id: T-03
title: 后端首页时间与启动运行摘要
status: done
planning_depth: standard
planning_depth_reason: 改动只位于 ruoyi-admin 的启动入口、根路径和测试，但必须同时验证固定时间语义、实际 WebServer 地址以及 Logback 文件落盘。
ready: true
risk: medium
blocked_by: [T-02]
contract_ids: [AC-017, AC-018, AC-019]
owner: codex:/root
expected_changes:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/java/org/dromara/DromaraApplication.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/java/org/dromara/web/controller/IndexController.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/DromaraApplicationUnitTest.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/controller/**</Path>"
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/java/org/dromara/DromaraApplication.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/java/org/dromara/web/controller/IndexController.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/DromaraApplicationUnitTest.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/controller/**</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/logback-plus.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/logging/**</Path>"
shared_paths: []
shared_path_owners: []
---

# Ticket T-03: 后端首页时间与启动运行摘要

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/ticket/03-admin-runtime-summary.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/evidence/T-03.md</Path>`

## 1. 战略与来源

- **目标：** 根路径返回固定启动时间与动态当前时间；应用启动完成后通过 INFO logger 在控制台和 sys-console 同时输出访问地址、启动时间、系统、耗时和内存。
- **可观察产出：** `/` 的中文响应可区分实例启动时刻和访问时刻；启动摘要在终端与服务器文件中均可检索，使用实际 WebServer 端口且不输出成功口号。
- **来源：** `US-007`、`US-008`、`AC-017` 至 `AC-019`、`LOG-019`、`LOG-020`。
- **当前执行模式：** 用户先授权非 SpecDev 完成模式直接修改和测试，随后授权把全部后端范围作为单一提交推送；该提交不是 T-03 独立 checkpoint，因此本 Ticket 保持 ready，不得标记 done。
- **Planning Depth 原因：** 单模块改动不涉及数据迁移，但跨越 Spring 启动生命周期、WebServer 实际绑定值、Logger 和物理文件，需要 standard 深度。

## 2. 决策状态

### 已锁定决策

- `IndexController` 构造时从 `ApplicationContext#getStartupDate()` 固化启动时间，每次 `/` 调用重新计算当前时间，格式均为 `yyyy-MM-dd HH:mm:ss`。
- `DromaraApplication` 在 `application.run` 返回后输出摘要，所有专用实现方法保留在该类内。
- 摘要使用 SLF4J INFO，不使用 `System.out`，使 root appender 同时输出到终端与 sys-console。
- 访问地址优先使用实际 WebServer 端口；SSL、监听地址、IPv6 和 context path 遵守 AC-019。
- 启动时间使用 Spring 上下文启动时间；启动耗时使用单调时钟；内存显示 JVM 已用与最大堆。
- 不输出额外“启动成功”提示语，不新增 YAML、配置类或服务类。

### 已采用的低影响假设

- 启动摘要是普通系统 INFO 日志，允许在 sys-console 中占多行；只有 HTTP 专用事件要求单行 JSON。
- 访问地址是本机直连提示，不推断反向代理或公网 URL。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变） | OUT（明确不做） |
|---|---|---|
| admin 启动摘要、根路径时间响应和对应测试 | Spring ApplicationContext 启动时间、WebServer 端口、SLF4J 与 T-02 root appender | 新配置类/YAML、其他应用启动类、代理 URL 推断、`@Log` 审计 |

## 4. 要构建什么

根路径 Controller 在实例创建时保存格式化启动时间，在每次请求中取得当前时间并返回中文响应。主启动类在 Spring 完成启动和 runners 后组装摘要，通过自身 logger 输出：分隔线、访问地址、启动时间、运行系统、启动耗时、已用/最大堆内存和结束分隔线。

地址生成读取 SSL、server.address 和 servlet context path，并在可用时以实际 WebServer 端口覆盖配置端口；通配地址显示为 localhost，裸 IPv6 地址增加方括号。

## 5. 实现契约

- **入口：** `DromaraApplication.main` 和 `IndexController#index`。
- **时间语义：** 启动时间来自 `ApplicationContext#getStartupDate()`，访问时间来自请求处理当刻，耗时来自 `System.nanoTime()` 差值。
- **日志语义：** 摘要以 `org.dromara.DromaraApplication` INFO 事件输出，传播到 T-02 的 console/file_console；不能同时保留 `System.out` 造成重复。
- **格式：** 时间为 `yyyy-MM-dd HH:mm:ss`，耗时保留两位秒，内存在 MB/GB 间选择并保留两位。
- **兼容：** 不改变业务 API schema、数据库和配置；根路径仍返回字符串，只扩充有用运行信息。
- **失败行为：** `application.run` 失败时不输出成功摘要；成功后使用上下文已经确定的实际端口。

## 6. 执行路线

1. 用日志捕获测试锁定摘要字段、启动时间、实际端口、协议、context path 和 INFO 级别。
2. 将启动输出从 `System.out` 改为 SLF4J，并在同类中增加启动时间和格式化方法。
3. 保持并验证根路径固定启动时间与动态访问时间。
4. 运行 admin 定向测试、受影响 reactor package 和临时端口/临时 LOG_PATH 真实启动。
5. 记录 clean fat jar、完整 test 和 Git checkpoint 的未完成状态。

## 7. 路径访问契约

- **可写范围：** 仅 frontmatter `writable_paths`。
- **只读接缝：** T-02 Logback XML 和 sys-console 测试；不得由本 Ticket 改写滚动策略。
- **共享路径：** 无；T-03 是 admin 启动类与根路径的唯一 owner。
- **偏差控制：** 若需要修改 common-web、application YAML、其他可部署应用或依赖，必须停止并更新 Ticket/Map。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| AC-017 固定/动态时间 | Controller 单元 | cwd admin：`../mvnw -Dtest=IndexControllerUnitTest test` | 启动时间跨请求固定，当前时间位于访问窗口 | `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/evidence/T-03.md</Path>` |
| AC-018 INFO 摘要 | Logger 捕获单元 | cwd admin：`../mvnw -Dtest=DromaraApplicationUnitTest test` | 单一 INFO 事件含全部字段、启动时间和分隔线 | `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/evidence/T-03.md</Path>` |
| AC-018/019 物理落盘 | 本机进程级 E2E | 临时 `LOG_PATH` 和未占用端口运行 `spring-boot:run`，检查终端与 sys-console 后关闭 | 两处内容一致，地址使用实际端口且进程正常关闭 | `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/evidence/T-03.md</Path>` |
| 后端组装 | Maven reactor | cwd backend root：定向 test、clean full/core package | 所有命令通过且 fat jar 可启动 | `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/evidence/T-03.md</Path>` |

- **E2E disposition：** required；只有真实启动才能同时证明 Spring 启动后实际端口、终端输出和物理 sys-console 文件。
- **E2E owner/environment：** Lead codex；本机 current workspace，临时 `LOG_PATH`、未占用端口和 dev/local 外部依赖。
- **Workspace checks：** 当前非完成模式可运行观察验证；Ticket done 仍要求 implementation commit、direct-parent result 和 clean 组装证据。

## 9. 发布、迁移与恢复

- 无数据库、配置或文件迁移；应用重启后新摘要生效。
- 回滚本 Ticket 可恢复旧根路径/启动输出，不影响现有日志文件。
- 当前活动 JVM 不热替换主类；用户正在运行的旧进程必须自行重启后才使用新实现。
- 不在本 Ticket 中清理构建目录或停止用户进程。

## 10. 验收标准

- [ ] AC-017：根路径启动时间固定、当前时间按访问刷新。
- [ ] AC-018：启动摘要以单一 INFO 事件同时进入终端和 sys-console，并包含启动时间且无成功口号。
- [ ] AC-019：实际端口、协议、host 与 context path 组合正确。
- [ ] 定向测试、真实启动、完整 test 和 clean full/core package 均通过。
- [ ] 存在非空 implementation commit、direct-parent result SHA 和完整 Lead Evidence。
- [ ] 实际路径未越界且无未批准偏差。

当前工作区已有观察通过结果，详见 T-03 Evidence；以上复选框保持未完成。
