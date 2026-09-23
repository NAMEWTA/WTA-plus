# T-40 R1 发布失败的有界安全观测方案（只读设计）

固定失败输入：`/tmp/wta-t40/browser-runs/0c8f89704398d48a/result.json`，source `c860480914b5fc563dd119e760e37d5eb99064e3`。本次三次无 UA 登录和 Chrome 正控制的在线、异步审计均通过；`notice_publish` 收到 HTTP 200、业务码 500，尚未启动 Chrome；source/JAR 前后相同、cleanup errors 空。当前 run 只保留 `init.log` 和安全 `result.json`，原始 backend log 已删除，无法从本次记录恢复服务端异常类或栈帧。

## 先用静态根因，确有歧义才增加一次观测

`GlobalExceptionHandler.java:157-173` 对一般 RuntimeException/Exception 只调用 `LogSanitizer.failure(e)` 记录异常类型；该方法在 `LogSanitizer.java:113-116` 返回类名，不记录 throwable 或 stack。`NotifyNoticeUseCase.java:25-29` 的发布事务跨 `noticeService.publish` 与 `publisher.publish`，后者可能在多个校验/写入点抛错。因而现有 raw backend log 至多能稳定提供该请求附近的**异常类型**，不能承诺有项目 Java 栈帧。若 cors 对固定源码和受控输入已定位根因，应直接修正并用原严格矩阵验证；不要为了期望不存在的栈帧重复启动完整环境。

## 如仍需观测：只在现有 runner 写集内增加诊断分支

1. 在 `real_notice_control()` 调用 `notice_publish` HTTP 前，记录私有 `backend.raw.log` 当前字节偏移；请求返回或抛错时记录结束偏移。偏移仅留内存。读取只在 `OwnedControlFailure(stage='notice_publish', kind='action_rejected', http_status=200, business_code=500)` 且源 phase 为 `notice_control` 时执行，并且一定在 `main()` 的 `finally` 删除 raw log 之前。读取失败不得覆盖原业务失败；只产生固定 `unavailable` 诊断状态。
2. 用 `O_NOFOLLOW` 打开本次 `0700` run 目录中的固定 `backend.raw.log`，确认普通文件；只 `pread` 上述区间末尾最多 **256 KiB**。若区间更大，标 `truncated=true`；每行最多检查 **1024 bytes**，超限行跳过。只在内存对 ASCII bytes 做严格匹配，不打印、保存或 hash 原始片段，也不扫描其他进程日志、Docker inspect Env 或任意路径。HTTP 业务响应的 `msg`、body 和 headers 仍完全不参与诊断。
3. 保留至多 **3 组**观察，每组仅异常 FQCN（固定 Java 标识符语法、最长 160 字符、必须以 `Exception`/`Error` 结尾）和至多 **4 个**项目帧：仅接受独立栈行 `at org.namewta.<类>.<方法>(<Java文件>.java:<十进制行号>)`，类/方法/文件均限 Java 标识符、行号 `1..100000`，并优先验证对应项目 `.java` 源文件存在。全局 handler 的 `类型=<FQCN>` 只能按固定 `/notify/notice/<数字>/publish` 日志模式在内存识别；输出只保留类名，不保留路径、错误编号、时间戳、线程、消息、SQL、请求体、令牌、异常 `toString()`、cause 文本或原栈行。多组可以并存，**不得宣称任何一组必然属于失败请求**；若只有类型无帧就只记类型。
4. `result.json` 只新增类似 `backend_exception_observation={stage:'notice_publish',status:'observed|none|unavailable', scanned_bytes:<0..262144>,truncated:<bool>,groups:[{type:<允许类名>,project_frames:[{class,method,file,line}]}]}` 的受控结构。原 `safe_failure`、`OwnedControlFailure`、两 Chrome case、source/JAR、双标签容器/匿名卷/进程组/端口、raw log 与 reporter 删除，以及 `cleanup.errors` 失败门禁一律不变。解析器失效时可以没有观察，不得变成通过或延长/重试业务请求。

离线负例应包括：带 canary 的异常 message/body、混合多段无关异常、伪造超长类名/行、`Caused by` 文本、超 256 KiB 日志、缺日志/文件异常，以及仅有全局 handler 的类名无栈；断言结果中无 canary、原文和完整 token，`notice_publish` 原失败与 cleanup 仍为非接受。此观测只是诊断辅助，不是 T-40 真实验收或唯一请求归因证据。
