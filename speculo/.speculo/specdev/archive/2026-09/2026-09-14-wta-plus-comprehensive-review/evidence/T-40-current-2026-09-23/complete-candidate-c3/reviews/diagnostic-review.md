# T-40 C3 失败诊断增量：只读静态审查

结论：`8bed2548`（C2）→ 固定 `d5b4f5f83e43afdc30ad30b3577a1531b030fc11` 的增量仅有两个 Python 文件，未发现会泄露响应/令牌或放宽业务、清理门禁的静态阻断；可由 Lead 继续固定同源完整构建与独占真实浏览器验收。Lead 报告 17 个离线测试已通过，本审查没有重跑测试或启动服务。`git diff --check` 对这两路径为 exit 0。

| 固定文件 | SHA-256 |
|---|---|
| `frontend/e2e/run-notice-retraction-real.py` | `9f0cd4ff3247e50db57581cac53f1a45c0b93c1f794c3f9b2330b6cd25ee01a4` |
| `frontend/e2e/test_run_notice_retraction_real.py` | `257d683f4166c7b27daebc410428767e6872c3506b451923846196185f734a9a` |

新增 `OwnedControlFailure`（runner 78–100）仅可持久化预定义的 HTTP 阶段/失败种类、100–599 HTTP 状态和受限整数业务码；任意 `msg`、响应 `data`、Bearer、密码、URL、异常正文均不进入 `safe_details()`。`control_request` 对大于 1 MiB、无效 JSON/结构、传输错误给固定类别；`control_login` 与 `require_control_success` 仍严格要求 HTTP 200 与业务码 200，且令牌不存在/太短继续失败（697–742）。原始响应只在内存中供业务判断，没有新增日志或持久化入口。登录分为 control/A/B 三个固定阶段，真实 Notice save/publish/retract 与 recipient 403 检查未变。

`safe_failure`（153–188）继续不用异常正文，只保留预定义异常类名、固定文案、白名单 phase，以及 traceback 中文件路径精确等于**本 runner**且行号落在其源文件范围内的一个整数；不输出 traceback 文本、文件路径、第三方帧或局部变量。启动前失败可得 `null` 行号。`failed_backend_exit_code()` 在发送清理信号前读取 backend 原进程退出码，只保留 -255..255 的整数；`backend_probe_http_status=200` 只在 `wait_http()` 已确认精确 200 后赋值（412–429、1145–1150）。顶层捕获 `BaseException` 仍写脱敏摘要；SQL 故障继续仅保存原来的白名单 stage/错误数字/SQLSTATE，不回退到 stderr 正文（1241–1247）。新增离线反例覆盖恶意 `msg`/token、布尔或巨型业务码、坏 JSON/超限响应、退出码和非脚本位置。

`git diff` 没有更改真实发布→Worker 送达→撤回、本人/他人消息断言、Playwright 两例精确零 skip/单尝试校验、JAR/source 前后锁、owner/run 双标签与 full ID、匿名卷/端口/进程组清理；原始 Playwright reporter/日志/附件仍在 `finally` 删除，清理失败仍使 `acceptance=false`（runner 1254–1321）。本增量只提高失败定位能力，不能用离线 17 例替代真实 C3 Chrome 结果。

非阻断限制：`runner_source_line()` 在异常路径读取固定 runner 源文件；若外部进程同时移走该文件，摘要本身也可能失败，但 `finally` 清理仍执行，随后 source-after 不会为 clean，不能产生通过。Lead 实际运行应继续串行保持固定 HEAD 与 full JAR，不在构建/运行期间改源。
