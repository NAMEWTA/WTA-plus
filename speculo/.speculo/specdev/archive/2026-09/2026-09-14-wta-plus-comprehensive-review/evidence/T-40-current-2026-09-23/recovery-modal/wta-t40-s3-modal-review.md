# T40 S3 弹窗交互只读核查

固定源码：`ddc0b51d6c3278f9e55e9c0aa21bde614958e6cb` 的 `frontend/e2e/notice-retraction-real.e2e.ts`；`InboxPage.vue` 当前相关实现按行号核对。本报告没有运行浏览器或服务，不能将此推断当作 S3 失败的最终归因。

- `InboxPage.vue:223-224` 在请求详情前立即设置 `detailVisible=true`、`detailLoading=true`；模板 `:56-57` 是默认模态的 `el-dialog`（本地 Element Plus `dialog.mjs:140` 默认 `modal:true`）。因此测试在 `oldFetched=1` 后直接点击底层 `.avatar-wrapper`（spec `:216-220`），通常会被遮罩拦截，Playwright 的普通 `click()` 会等待可操作而不能完成注销。这是确定的测试操作顺序缺口，是否正是本次超时原因仍需安全结果中的失败位置证明。
- 弹窗 footer 的“关闭”按钮始终存在（`InboxPage.vue:71-74`），仅写 `detailVisible=false`。`watch(detailVisible)`（`:300-307`）递增 `detailGeneration`、清理本地详情状态；`openDetail`（`:215-240`）未传 AbortController，也不在关闭时取消 `runtime.service.inbox.detail(id)`。因此关闭弹窗可保留已经 `route.fetch()`、仍被 `pending` 持住的旧 A 请求；响应回来后因 generation/visible 检查不会写回旧详情。
- 最小可操作修正：在 `oldFetched===1`、`oldRequest` 非空后，先断言弹窗 loading，再点击 `detail(page).getByRole('button',{name:'关闭',exact:true})`，等待弹窗隐藏，并断言 `oldResponses===0`、`oldFetched===1`；之后以普通头像点击进入现有注销流程。不要用 `force:true` 或页面脚本绕过遮罩。原 exact Request `requestfailed/ERR_ABORTED`、B 登录正控制、同页迟到响应断言和 finally 清理继续保留。关闭弹窗只使 A 的展示所有权失效，不等于释放 held route；仍由原 `unblock()` 时点释放。

若最终运行位置为 `:219` 的头像点击或其前后 actionability 超时，则与此机制一致；若落在别处，需按实际安全定位另查，不据本报告排他归因。
