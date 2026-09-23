# T-40 Notice 撤回浏览器候选：独立安全静态审查

结论：固定提交 `2c8047a2bfca47389557e4261ff5a55036e1c72e` 的五个浏览器文件未见静态阻断，可由 Lead 继续离线门禁与独占真实运行；这不是浏览器验收通过结论。审查仅阅读文件，未启动 Docker、JVM、Vite、Chrome 或构建。

审查源为 `/tmp/wta-t40-browser-final-snapshot/`，捕获清单见该目录 `manifest.json`。逐个 `git show <fixed-sha>:frontend/e2e/<name>` 与快照字节相等，提交时工作树 clean。SHA-256：

| 文件 | SHA-256 |
|---|---|
| `run-notice-retraction-real.py` | `6e609557296398a0546850c5547f60a26d9535a338a9ca8846143763caed05b8` |
| `notice-retraction-real.e2e.ts` | `41a0c019e18bf9c0071d048a62ce5c9f953f19d85c0d2bd1fc98907fd805d12b` |
| `playwright.notice-retraction-real.config.ts` | `0824670ffef0a993b000b1b9024825503460e9bb1bb1ff22bbb3d7d7c05afd4b` |
| `test_run_notice_retraction_real.py` | `c03c6c3c9f4650718805829a71c87169a7a6521c42571077ee87f8af144ad01b` |
| `README-notice-retraction-real.md` | `5774ce34b503391ed9417df0dd78ce521954c9bde3e0f5ba3c6a496e14aa2cea` |

前次草稿的安全问题已闭合：`control_token` 和 A/B 两个 recipient token 在签发后立即追加 `redactions`（runner 1078–1085）；`control_request` 将网络/JSON异常转成静态阶段错误且不保留 cause 正文（631–654），顶层任意异常只持久化允许类型名与固定文案（123–132、1154–1160）。`OwnedSqlError` 仅保存白名单 stage、退出码、MySQL 错误数字和 SQLSTATE。Playwright 原始 reporter/stderr、运行目录和浏览器附件进入私有临时目录，持久诊断只保留固定用例的数值位置与状态；配置禁 trace、video、screenshot，源码预检禁 storageState/HAR。失败时原始 reporter、原始日志和附件仍走 finally 删除；清理失败会使 `acceptance=false`。离线测试包含敏感正文反例，但本审查没有执行测试。

权限负例现在要求两个普通身份的真实 Notice 管理 GET 同时满足 HTTP 200 和业务 `R.code=403`（1086–1093），500/其他错误不能冒充拒绝。正向由 control 身份经真实 `/auth/login`、`/notify/notice/save`、`/{id}/publish`、Worker 送达、`/{id}/retract` 完成；`notice_version_fact` 核对 V1 的 notice/snapshot/intent 身份及撤回标记（576–594）。浏览器两例只走本人 `/notify/inbox/{messageId}`，明确断言无管理 GET；A 的真实 V1 被合成 20 行挤出第一页仍可深链访问，B 的他人/缺失 ID 返回同形响应，非法 ID 不发详情 HTTP。

迟到详情屏障已改为等待被截留的真实 `route.fetch()` 响应成功转发、Playwright `Response.finished()` 与两个动画帧，然后复验当前 detail 内容和跨身份无旧正文（TS 55–59、88–116、143–175）；没有依赖固定 100ms sleep。这里是浏览器可观察的完成屏障，真实运行仍须以两个 Chrome 用例的结果裁决。

资源边界：运行前精确 clean HEAD + full JAR SHA/包证明；Docker 仅 `T-40-BROWSER` owner/run 双标签、完整 ID，三个镜像无自动拉取，端口只随机绑定 `127.0.0.1`，六 SQL 新库 103 表且 SMS/MAIL 无启用账号；所有登录凭据是 owned 随机值，24 字符登录密码符合 HTTP DTO。JVM/Vite/Chrome 使用独立进程组；finally 按 owner/run 检查删除容器、逐名核匿名卷不存在、端口关闭、进程组无成员、源/JAR前后同值（runner 217–243、930–1030、1161–1245）。要求精确两例、各唯一尝试、零 skip/flaky、所有清理成功才可 `acceptance=true`。

保留的运行限制：本静态审查未证明 full JAR 的实际 Worker 行为、Playwright 真实调度或资源清理结果；Lead 的离线门禁及随后 owned 浏览器运行必须分别留真实记录。若浏览器失败，应按仅含固定 case/行列的脱敏诊断和私有 run/result 判断，不能仅用进程退出码声称行为红灯或通过。
