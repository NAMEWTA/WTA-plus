# T40 浏览器 C2 启动后失败：只读定位与安全诊断提案

固定 `851a6d6eb0edf9b53876bf43b56bb1747f1f7b4a`；`/tmp/wta-t40/browser-runs/19a061800c7e3c0f/result.json` 保留 source/JAR 与 owned 资源、清理全零。未读/恢复已删除 raw 日志，未启动服务或改仓库。

## 当前可证明的位置

`run-notice-retraction-real.py` 在 `report.owned.backend_pgid` 写入后执行 `wait_http('/auth/code')`，然后顺序直连 `/auth/login` 校验 WTA、test、test1，三次均成功才写 `control_and_recipient_logins_validated=3`。本次该字段缺失，而 `backend_pgid` 已有；约 34 秒的运行时间既可包含启动后第一登录失败，也可包含进程退出，**不能**从固定 `RuntimeError` 推断是哪一步。T41 已验收的 runner 使用相同六 SQL、BCrypt 24 位密码轮换，但在启动 Vite 后由真实页面登录 WTA/test1；T40 多了 test 用户/两份 owned 最小角色，并在 Vite 前立即通过 `http.client` 直接登录。现有 `/auth/login` JSON 字段 `username,password,clientId,grantType`、`access_token` 名称与前端一致，未发现静态字段错配；直接 HTTP 的业务 code/状态和 backend ready 事实必须下一次安全采样验证。

## 只改 runner+相邻离线测试的最窄补丁

1. 声明固定 `PHASES` / `HTTP_STAGES` 白名单：`backend_probe`、`login_control`、`login_a`、`login_b`、`notice_save/publish/retract` 等；在 `wait_http` 前、每次 login 前设 `report.phase`，成功后置下一阶段。只保留白名单枚举值，不持久化 URL、请求/响应、用户名、密码或 token。`wait_http` 返回后写 `backend_probe_http_status=200`，若进程先退出，只记录 `backend_exit_code`（`poll()` 为整数时）与阶段；超时/网络错保留固定类别，响应体丢弃。
2. `control_request` 内创建结构化 `OwnedControlFailure(stage,http_status,business_code)`；只接受白名单 stage、100..599 的数字 HTTP 状态、有限范围的整型 `R.code`（`bool`/字符串/异常值记 `null`）。请求超时、连接错误、JSON 非对象/超限分别用固定 failure kind；不要把 `response.msg`、`data`、原始异常或 URL 放进异常文本。`control_login` 接收 `login_control/a/b`，失败时带当前数值 status/code；HTTP 200/R200 但无合格 token 记录 `token_missing` 类别，不保存 token 长度或值。token 仍只用于进程内后续调用并加入私有 redactions。`require_control_success` 后续控制请求也复用同一数值诊断，不改变 200/code200 的严格验收。
3. 顶层 `safe_failure` 除固定 `error_type/error` 外，仅收结构化 `phase`、`http_status`、`business_code`、`backend_exit_code` 与 `runner_line`；`runner_line` 从 traceback 中定位本脚本 `__file__` 的最后一帧，只保留 `1..源码总行数` 的整数，其他帧/路径/stack/snippet 全丢弃。`OwnedSqlError` 现有 allowlisted stage/exit/MySQL number/SQLSTATE 原样保留。不要把任意 `str(exc)`、HTTP body、日志行或 Playwright raw reporter 纳入结果；cleanup/fail gate 不放宽。
4. 离线回归（mock HTTPConnection/进程，不运行服务）：200/R200+token 正常返回；401、200/R401、200/R200缺 token、超限/非 JSON、网络异常分别保存阶段和数字或 null；每个响应的 msg/data 与异常包含 credential/token canary，`result.json` 安全摘要均不含 canary；后端 process exit7 保留数字；伪造阶段、bool/巨整数 code、非本脚本 traceback 行号被拒/归 null；所有旧清理与严格双 case 门禁保持。

建议一次安全运行后按 `phase+status/code+backend_exit_code+runner_line` 判定：`backend_probe` 失败查启动/探针，`login_control` 失败查 WTA/API，`login_a/b` 失败查新普通角色与 Client/密码域。不能靠增加睡眠、接受任意 HTTP 失败、保存原响应或复用 T41 浏览器 main 掩盖原因。
