# T-41 D 固定候选实现轴增量审查（验收待补）

固定 C `eff81f642f08a1590f85a1f1f813a592a8232451` → D `d93eb1d8d6095464a9606619f4161b3dd4ef35c7`，D tree `668f14e67b3e21ac91f76e7c9b64e224ae29e3f5`。只读 `git diff C...D` 只有 `frontend/apps/admin-web/src/router/adminManifestRegistry.test.ts` 一个产品测试路径（45增/1减），**无生产代码、生成合同、浏览器运行器或配置变化**。本报告先给静态增量结论，D 正式 attempt3 的完整测试/构建、真实 501 和浏览器尚不能代称通过。

**C 报告中的宿主端口覆盖缺口已由 D 的源码关闭。** 新测试 `adminManifestRegistry.test.ts:1-24,70-98` 使用 `vi.mock('@namewta/web-domain-notify', importOriginal)` 包裹真实 `createNotifyWebDomain` factory，仅捕获实际 Admin 组合入口传入的 `runtime.inboxSession`，随后调用原 factory；并未以假 snapshot 替换产品端口。`useUserStore` mock 从固定对象改为可变响应式对象；真实 `adminManifestRegistry.ts:277-300` snapshot 被逐次调用。断言 A 已加载时 active，登出 generation 先变而 token/userId/loaded 尚旧时 epoch 增且 active=false，重复读取不再递增；新 B token/userId 载入前仍 inactive，identityLoaded=true 后 epoch 再增且 active=true。快照只含 epoch/active，不携带 token。原四项 registry 断言未删除/放宽；Notice 五项及其他 B/C 断言未变。这个测试正对应 Ticket41 revision176 的“真实宿主 session 端口时序”，与此前 `InboxPage.test.ts` 的合成 port 层形成互补。

Lead 的定点结果 `/tmp/wta-t41/d-host-port.json` 和 `.log` 显示 Vitest selector `src/router/adminManifestRegistry.test.ts` exit 0，**1 file / 5 tests pass**。这是真实 factory 适配测试通过，不是整个 D 前端或真实浏览器通过。C 的前端整轮 `/tmp/wta-t41/c-frontend-counts.json` 记录 642 Vitest + 108 Node = 750 执行、零失败/skip，`c-frontend-build.json` 生产 build exit0、`c-frontend-artifacts.json` 记录三 App 328 文件。因 C→D 仅测试文件不同，生产输入静态等价；若 Lead 逐件复核 artifact SHA 并确认 build 所依赖输入未变，可以**明确复用 C 的生产产物证据**，但不能称 D 又跑过生产 build。D 的 architecture/lint/typecheck/test 四项门禁、当前 clean/source-after、后端与浏览器结果以正式 attempt3 实际记录为准，仍 pending。

历史 B 测试装配失败与 C 完整前端通过应各自保留；此 D 静态审查不覆盖或改写旧证据。最终 T-41 实现轴是否 pass，待 D 所有必需真实验证及清理证据齐全后补判。
