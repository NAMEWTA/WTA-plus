# T-03 当前合同工程复核（只读）

固定 HEAD：`617a3693ddb2897851ab46d4131690898df6a690`；历史非空实现提交：`214de538267a60fa527b1213f7dd9139622f4a2e`，是当前 HEAD 的祖先（`git merge-base --is-ancestor` exit 0）。本报告只读 Ticket 03、Map/Goal、历史 `evidence/T-03.md`、生产代码/测试与 Lead 已保存的当前运行记录；未运行构建、服务或修改仓库。结论截至当前证据：**没有发现需要重复实施 T-03 核心产品代码的退化**；能否关闭本轮重复施工仍须 Lead 完成当前候选验收与合法状态处置，不能以旧 review 或取消状态冒充 Done。

## 当前工程合同逐项核对

| AC-003 条件 | 当前源码与观察 | 当前验收边界 |
|---|---|---|
| 定长/chunked 在 2 MiB 前一字节、等于、超一字节可判定；未知或低报长度不绕过 | `CapturedRequestBody.capture` 先检查声明长度，未知/低报长度最多读预算加一字节，完整成功后才设置请求属性；`RepeatableFilter` 在普通 JSON/+json 进入业务前返回 413，`OpenApiGatewayFilter` 在认证/业务前捕获机器正文并单独处理 413。`RequestBodyHttpIntegrationTest` 真实 Jetty/HttpClient 的 ordinary/signed fixed/chunked 用例逐项断言 HTTP 状态与 controller 调用数。 | 当前 `acceptance-consumers.log` 中该类 11/11、0 skip；不是用 mock 代替 HTTP。 |
| 原字节验签、不同入口预算、XSS 不改原文 | `ReplayableOpenApiRequest` 从同一 `CapturedRequestBody` 取防御性副本；`OpenApiRequest` 构造和 `body()` 均防御复制；签名哈希消费原字节。`OpenApiGatewayFilter` 为最高优先级 +1，普通 `RepeatableFilter` +2，日志 +3，XSS +4；`XssHttpServletRequestWrapper` 另建有界 UTF-8 视图。普通与机器预算由 `RequestBodyProperties` / `OpenApiProperties` 独立校验，`application.yml` 默认均 2MB。真实 HTTP 测试含伪签名大正文 413、较小伪签名 401、原始空白/UTF-8/HTML 的逐字节 HMAC 与独立视图。 | 机器凭据/nonce/会话存储在该测试中是确定性内存夹具；这里只证明 Filter/签名/正文边界，不称真实外部状态服务验收。 |
| 同一正文不随观察者线性复制；上传/SSE 可取消 | owner 存在请求属性；`RepeatedlyRequestWrapper` 复用 owner；`openStream` 建游标不复制底层数组，`prefix` 仅取日志所需字节，`copy` 用于不可变签名合同。XSS 视图是一次有界副本。multipart/二进制/事件流不进入普通 JSON 捕获；`SysLogResponseWrapper` 直写响应，只复制有界日志前缀。`RequestBodyLimitTest` 验证 20 次观察者复用、断流无部分缓存；真实 HTTP 覆盖上传大于普通预算仍流式、JSON/上传取消后下一请求恢复、SSE 首事件 flush 和客户端取消。 | 实测 `/tmp/wta-t02-c1/body-probe.json` 为单 JVM 512 MiB、4 并发×2 MiB、两轮 8 请求、日志前缀 32 B，峰值约 154 MiB；这只证明该夹具的有界性，不外推生产峰值或 SLO。缓存流的 ReadListener 非阻塞模式不在既定同步 MVC JSON 合同内。 |
| 非法配置与说明 | `RequestBodyProperties`、`OpenApiProperties` 验证正数与数组范围；`backend/README.md` 记普通/机器独立 2MB 与日志 1MB 前缀。 | 当前 `RequestBodyLimitTest` 9/9、`OpenApiAssemblyContextTest` 9/9、`SysLogConfigTest` 4/4，零 skip。 |

## 历史差异与消费者

`git diff --name-only 214de538..617a369` 对 `CapturedRequestBody`、`RepeatableFilter`、`RepeatedlyRequestWrapper`、`wta-common-openapi/src/main` 和 `RequestBodyHttpIntegrationTest` 无输出；核心 owner 与真实 HTTP 测试 blob 在两点一致。`XssFilter`/`XssHttpServletRequestWrapper` 的可见增量仅移除作者注释。T-02 后来的 `SysLogFilter`、`GlobalExceptionHandler` 变化处理日志路径/异常输出副本，未改变正文采集、Filter 排序或签名输入；其当前消费者测试与真实 HTTP/MySQL 已在固定 HEAD 执行。`wta-common-encrypt` 相比历史点存在 `CryptoFilter`/请求解密 wrapper 删除及字段加密实现改动，这是后续 T-11 的退役范围；当前该模块不再是请求正文多观察者，不能为 T-03 复活旧解密链或用旧类缺失误判本票回归。

## 已有当前执行证据与尚不能声称的结果

- `/tmp/wta-t02-c1/acceptance-consumers.json`：cwd `backend`、`./mvnw -Pdev -pl wta-admin -am` 的精确 11 类选集、`JAVA_TOOL_OPTIONS=-Xmx512m`、实际命令/时间/exit 0。相应 `acceptance-consumers.log` 中 **10 个实际执行类合计 70/70，0 fail/error/skip**，其中 T-03 相关为 `RequestBodyHttpIntegrationTest` 11、`RequestBodyLimitTest` 9、`OpenApiAssemblyContextTest` 9、`SysLogConfigTest` 4；第 11 类 `BrowserHttpsTransportIntegrationTest` 当轮因开关未启用而 **1 skip**。因此 70 项事实不能写成 11 类全零 skip。
- `/tmp/wta-t02-c1/body-probe.json` 为上述测试的新写入探针（512 MiB/4×2 MiB/8 请求）；旧 `evidence/T-03.md` 的 8 并发/16 请求、83/83 与当时 full/core 记录只作历史实现证据，不能代替本 HEAD 的验收。
- `/tmp/wta-t02-c1/real-http-mysql-counts.json`：同 HEAD 的 T-02 日志链真实 HTTP/MySQL 7 类 39 项 0 skip，能够支持 T-02 上游状态与日志消费者安全，但不替代 AC-003 的 HTTP 边界/签名测试。
- Lead 正准备/执行 HTTPS 启用补跑及全后端 test/full-core；本报告未把它们标为通过，也未重新运行 Ticket §8 的 common-web/openapi/encrypt 全模块测试。若 Lead 尚无同 HEAD、零必需 skip 的可复用结果，需按 Ticket 所列模块门禁运行并记录 XML 数量/skip；HTTPS 类需启用其正式开关后 1/1。实际命令、clean 源码/树、报告 hash 与资源清理由 Lead 对应固定候选证据登记。重跑相同 2 MiB 负载仅为重复日志没有工程收益，除非验收环境或源码改变。

## 无新施工时的 T-30 责任与依赖处置

T-03 现为 ready、`blocked_by: [T-02]`；`plan-data.json` 当前只有 T-30 把 T-03 作为下游 blocked_by。先让 T-02 的当前候选与正式状态闭合，再用当前门禁和上述源码对照裁决是否 **仅取消本轮重复施工**。历史 `214de538` 是真实非空实现；旧整批 result `6c8764c...` 不能补写成当时逐票 clean exact-HEAD 结果，亦不得新造空产品提交。若当前全部 AC-003 行为/门禁成立且确实无需新改动，按 Goal §4 历史分支记录有证据的 cancelled；若新测试发现真实退化，则定界失败并回 T-03 写集作非空修复。

取消不自动满足依赖：Lead 须在 T-30 第 2/6 节明确引用 `214de538` 与当前 `617a369`（或最终同源候选）的 AC-003 源码、70/70、真实 HTTP/探针、HTTPS/全模块门禁以及各自适用性，再从 T-30 `blocked_by` 移除仅表示“待实施 T-03”的边，**保留 AC-003 的最终集成验收责任**。同步 Ticket、Map、plan-data、Goal revision 并重新运行控制器；不能因为 cancelled 就把 AC-003 当已完成或把 T-30 的完整跨票同候选验收省略。
