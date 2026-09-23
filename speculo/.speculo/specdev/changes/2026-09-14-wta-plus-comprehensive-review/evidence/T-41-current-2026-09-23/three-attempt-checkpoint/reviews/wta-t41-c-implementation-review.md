# T-41 C 固定候选增量实现审查（待完整验收）

固定 B `05d0f3443dc3364917a8149ce9290f7dc6785670` → C `eff81f642f08a1590f85a1f1f813a592a8232451`，C tree `d8143ce10fca585d001af46dd94bb9a272074a42`。只读 `git diff B...C`：22 路径，其中产品仅 `frontend/apps/admin-web/src/layout/components/notice/index.test.ts` 与 `frontend/apps/admin-web/src/router/adminManifestRegistry.test.ts` 两个测试文件；其余为 revision176 治理/失败证据。Ticket 前置精确写集由 23 增至 24 条，新增 registry test 路径在修改前登记。C 相对 B **没有生产 Java/TS、OpenAPI 生成物、浏览器 runner/spec/config 或业务断言的改动**；B 实现轴见 `/tmp/wta-t41-b-implementation-review.md`。

`notice/index.test.ts:1,61` 仅引入 Vue `ssrContextKey` 并给人工 renderer 提供 `{modules: Set}`，恰好消除 B 中 SFC setup 因 SSR Context 缺失崩溃的夹具问题。五例测试的标题、调用、断言和执行顺序均未改，旧身份详情/已读回调与只读权限断言保留。`adminManifestRegistry.test.ts:4-6` 仅 mock 既有 user store 模块，提供 token/sessionGeneration/userId/identityLoaded 固定空值，阻断 Node 单测加载真实 Router 的 `window is not defined`；原四例关于路由选择、端口、Profile/Workflow 的断言均未改。这两个改动没有降低断言，也没有产品逻辑变化。

已由 Lead 执行的精确两文件门禁 `/tmp/wta-t41/c-targeted-two.json`：`pnpm exec vitest run src/layout/components/notice/index.test.ts src/router/adminManifestRegistry.test.ts` exit 0；`c-targeted-two.log` 2 files、9 tests 通过。此结果仅证明两个夹具可执行，**完整 frontend attempt2、真实 501/MySQL/HTTP/Browser、default/full/135 回归和 clean source/JAR 尚待实际完成**，不可据此称 C 验收通过。

**仍有明确的合同覆盖缺口，建议在最终 C 验收前补齐。** Ticket41 revision176 要求“mock 宿主 user Store 以隔离 node 环境中的浏览器 Router，**并覆盖真实宿主 session 端口时序**”。当前 registry test 新 mock 是静态对象，未调用 `adminManifestRegistry.ts:277-300` 中传给 `createNotifyWebDomain` 的真实 `inboxSession.snapshot()`，没有断言 sessionGeneration 先变、旧 token 未清时 active 立即 false/epoch 增，及新身份出现后的恢复。`InboxPage.test.ts` 测的是自造 epoch port；双 Context 真实浏览器测 A/B 隔离，也不是同一宿主端口时序。故 9/9 不能代称这项明确新增负向覆盖。最小补法是在 registry test 用响应式可变 user store，并通过该组合入口捕获/调用真实 runtime 的 snapshot，保留原九例与断言，新增上述时序用例；不要修改生产 adapter 或放宽门禁。若 Lead 裁决已有等价、真实执行的同端口测试，则应给出具体源路径和断言后关闭此项。

本报告为固定 C 的静态增量判定；C 当前完整验收 pending，后续固定提交若补测试需另出精确 diff 复审，不覆盖本历史结论。
