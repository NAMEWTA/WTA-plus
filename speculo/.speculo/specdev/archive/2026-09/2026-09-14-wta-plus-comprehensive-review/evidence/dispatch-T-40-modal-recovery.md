# T-40 Dispatch04 — 可操作的模态关闭与安全阶段定位

## 1. 失败与根因边界

此前三个批次各3次失败均保留。Dispatch03的S1/S2在lint失败；隔离诊断和命名AggregateError语义证明已保留，规则未放宽。S3 ddc0b51d已通过strict E2E类型、全前端760、三App329产物、fresh full/bundle；run434baf70aa25766e仅A timedOut、B passed，0skip/retry、source/JAR同值cleanup[]。安全位置22是聚合清理处，无法还原最初操作，不能宣称唯一根因。

新的源码证据：InboxPage.openDetail在await详情前立即打开默认modal且展示loading；测试hold真实响应后直接点弹窗外头像，违反正常用户可操作路径。默认locator动作未设独立超时，可一直耗到120秒总限。关闭按钮在loading期间仍可操作；关闭只隐藏/使详情代次失效，不abort实际请求。故可以先正常关闭弹窗并证明旧Request仍pending，再验证注销取消。

## 2. 独立审查

cors_audit与ops_audit分别确认上述UI合同和最小改法，报告与哈希见 `T-40-current-2026-09-23/recovery-modal/manifest.json`。这是有源码证据的候选原因，仍需真实Chrome验证；不将之前所有超时都追认为该唯一原因。固定阶段诊断用于避免后续清理覆盖首因，不能证明未完成操作成功。

## 3. 实质变更和写集

cors_audit唯一writer，范围仅现有 `frontend/e2e/notice-retraction-real.e2e.ts`、`run-notice-retraction-real.py`、`test_run_notice_retraction_real.py`，必要时现有README。业务、依赖、配置、权限、SQL不变。禁止force click、直接调用store替代注销UI、延长120/90秒总限、吞未知错误或削减断言。writer不跑测试/构建/服务/提交。

- 设置Page默认动作等待15秒，避免locator无界消耗总时限；保留原断言及总timeout。
- 捕获精确A Request并确认真实fetch完成后，先确认loading详情弹窗可见，用真实关闭按钮关闭并等hidden。保持held payload未释放，确认oldFetched=1、该旧Request无response/failure、handler未完成；然后正常头像/确认注销、同context B登录及本人消息正控制。
- 保留B登录/本人详情后释放A真实payload的顺序；要求有界handler终态、精确旧Request ERR_ABORTED、无旧Response、B正文保持及A正文缺失。原同页迟到成功、legacy路径、B独立foreign/absent/malformed、后端/seed/post-browser/cleanup不变。
- 用固定annotation标记last_started_phase，值为小型静态枚举，不含ID/URL/正文/凭据。当前Playwright JSON reporter在case和单次result中序列化annotations；runner优先从唯一result读取并白名单提取，缺失/畸形/未知记null，不复制任意对象。离线覆盖成功、缺失、未知type/value、数组/对象/超长/credential-canary注入，不放宽parse_counts与浏览器identity门禁。

## 4. 恢复与验证

本复盘提交后新批从0起，之前3×3失败永久保留，最多3完整候选且无新证据重复失败提前停。Lead固定clean HEAD/tree后先离线及独立E2E tsc，再新全前端architecture/OpenAPI/lint/typecheck/tests/三App构建、exact-source full JAR/bundle和真实Chrome2零skip/retry；source/JAR一致cleanup[]方可整票验收。backend/SQL/依赖相同可据diff明确复用R2 default889pass216skip、real18/shared135/core/static；不得冒称重跑。两类安全诊断仅附加观测，不替代真实断言。Goal继续active；不push/deploy/生产数据修复/归档。
