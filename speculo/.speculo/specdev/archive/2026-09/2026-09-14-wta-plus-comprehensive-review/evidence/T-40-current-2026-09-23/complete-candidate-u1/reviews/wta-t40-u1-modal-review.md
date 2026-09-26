# T40 U1 模态修复只读审查

固定候选 `26f04b94db68701ade80038a763eca0ffde83918`，相对其父提交仅 `frontend/e2e/notice-retraction-real.e2e.ts`、`run-notice-retraction-real.py`、`test_run_notice_retraction_real.py` 三文件变化；工作树检查为空。仅静态审查，未运行离线测试、类型检查、浏览器或服务。

**结论：静态范围内无阻断；实际浏览器通过仍待验收。**

- spec `:231-245` 等待精确旧 A Request 的真实 `route.fetch()` 和已授权正文后，确认 loading 弹窗可见，点击 footer“关闭”，等待隐藏，再以正常可操作方式点击头像退出。`InboxPage.vue:223-227,300-307` 表明关闭只递增 `detailGeneration` 和清空本地详情状态，没有取消详情 HTTP；route handler 仍停在 `await pending`（spec `:212-227`）。新增 `oldResponses===0`、`oldFailure===undefined`、`oldRequest.failure()===null`、`sessionSettled===false` 断言会在退出前证明该 Request 仍悬挂，未以关闭弹窗预先释放。测试未用强制点击或脚本越过模态遮罩。
- 原同页晚到响应（`:123-170`）、A 注销/B 登录、B 正控制、释放旧 A route 后按 Request 身份观察 `net::ERR_ABORTED`、B 详情与 A 内容隔离（`:245-266`）均保留。两次 `route.fetch()` 新增 `code=200` 及真实 messageId/content 正向校验，收紧而非替换原 HTTP 状态检查。关闭弹窗会使 Vue 的 generation guard 提前失效，但旧 A 网络请求跨注销仍存在并由精确取消断言覆盖；这符合用户可操作流程。
- runner `:37-40,1015-1039` 将阶段诊断限制为六个固定无敏感字面值。仅在精确 T40 文件、已知 case 标题、chromium、恰一结果、恰一个字典注解且键严格为 `type/description` 时采纳；优先结果级 annotations，仅在字段缺失时回退 case 级；无效、额外键或可变正文均输出 `null`。既有数字行列和失败状态过滤不变，原始 Playwright reporter/log 在 finally 删除（`:1451-1459`）。新增离线用例覆盖结果优先、缺失回退、含私有 canary/错误类型/重复注解拒绝、外来项目拒绝。

局限：静态无法证明 Playwright 实际 JSON reporter 会把动态 `test.info().annotations` 放在 result 还是 case；若二者都缺失，`last_started_phase` 安全地为 `null`，不影响浏览器业务门禁。真实验收须以固定 U1 的前后源码/JAR、离线与严格前端门禁、两 Chrome case 零失败零跳过及完整资源清理为准。
