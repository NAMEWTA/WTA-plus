# T-40 Dispatch03 — 浏览器跨会话请求生命周期恢复

## 1. 失败事实与根因边界

前批 C1/C2/C3 与恢复批 R1/R2/R3 共六次完整失败原样保留，不追认通过。R3 378f8481 / run f3ff781212c46752 已过真实发布→Worker V1→撤回、权限与seed；Chrome B passed、A timedOut，各1attempt/0skip，source/JAR一致且cleanup[]。安全位置175是finally unroute，不是已证明的首次失败位置。

源码证明两个测试合同缺陷：产品注销同步cancelPending会abort旧Axios scope，旧A浏览器XHR可能永无Response；route.fetch获得的独立后端响应不等于浏览器收到。Navbar保留V1 redirect，B登录会产生同URL的新请求，URL-only等待还可误认B响应。具体最后挂点未被旧安全记录保存，不能冒称已还原。保留实际取消/后端取得/handler终态以证伪下一候选。

## 2. 独立审查及影响

cors_audit与ops_audit独立复核上述矛盾和Playwright当前本地实现；legacy_audit确认产品静态AC仍无新阻断但A真实旅程未验收。报告原文/哈希见 `T-40-current-2026-09-23/recovery-browser/manifest.json`。现有InboxPage会话epoch单测与same-page实际迟到response防线均保留。业务代码、HTTP/API/权限/SQL/依赖不变；这是测试精确识别取消行为，不删除取消保护或容忍任意网络失败。

## 3. 实质修正与唯一写者

cors_audit只写现有 `frontend/e2e/notice-retraction-real.e2e.ts`，必要时同步README及现有两Python用于安全阶段摘要。都在既有Ticket授权写集内；禁止修改产品/配置/timeout上限/跳过规则。先读命中TS/Browser/测试规范；不跑测试/构建/服务，不提交，由Lead固定候选后串行验证。

- 保留同页V1真实fetch/迟到成功response与legacy正文保持；按精确Request识别响应，避免同URL误认。
- 跨会话捕获held A的route.request()，注销前绑定该对象的requestfailed/response；真实fetch完成后注销，必须观察精确旧请求被Chromium ERR_ABORTED取消且没有浏览器Response。未知失败不能作为取消通过，不使用catch吞错。
- 同context真实B登录，区分redirect引发的新请求；B本人详情正控制必须成功且A内容不可见。优先保留B登录后才释放A真实已取得payload的时序；不强求已取消XHR收到Response。handler必须有有界、可检查终态；仅在精确旧request已经证实取消时解释相应取消终态，其余错误失败。
- 每步等待有界10–15秒，总用例120/90秒不提高。finally先释放latch，清理有界；主失败及清理失败均不能被静默抹掉。若加报告，仅固定阶段/计数/布尔，严禁URL/ID/token/正文/任意异常文本进入证据。既有两Chrome、zero skip/retry、owner/seed/post-browser/cleanup全部保留。

## 4. 恢复入口、门禁及停止

本复盘提交后才恢复派单。新恢复批从0计，之前两个批次各3次失败永久保留；恢复最多3完整候选，重复无新证据则提前停。Lead独占提交与owned服务。新TS候选必须重新跑前端architecture/OpenAPI/lint/typecheck/test/三App生产build，并保留产物；Python若改则新离线。backend/SQL/依赖输入不变可明确复用R2 default889pass216skip/real18/shared135/core/static，但必须新exact-source full JAR、两真实Chrome零skip/retry、source/JAR同值cleanup[]。不部署、不写生产、不推送、不归档；Goal继续active。
