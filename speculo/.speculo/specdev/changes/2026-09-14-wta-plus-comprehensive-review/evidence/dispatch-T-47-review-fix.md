# Dispatch Packet T-47-20260923-02

operation=dispatch；native gpt-6-sol/xhigh；owner=cors_audit；Lead=single-agent；fixed source2d60e1af730036837576685a56657bf630a391cb；original base97e1ee9e1ad40de75deffd025379a6a5488c4882；current/main，唯一产品writer。

仍仅写frontend/packages/web-domains/system/src/oss/。修复规范轴P2：list成功即在current+alive检查后提交rows/total/config与清空的预览标识、结束列表loading；可选预览后续同代+alive回填，不让挂起预览阻断列表/后续操作刷新。所有旧响应/异常/卸载保护及授权URL/deleted语义不变。补一个预览永久pending时新列表可见/loading结束/getList刷新完成的受控回归；旧预览晚到仍不污染B，正常预览渐进可见。原wait-all断言确认为回退，先在2d60e1a得到新红灯再修正。

禁止提交/SpecDev/新worktree/部署/服务/E2E；日志/tmp/wta-t47新名字，保留上一轮原始结果。先Map/Ticket及已绑定Skill。允许包测试/typecheck/lint，不跑全仓构建。返回paths/commands/cwd/exit/count/skip与锁。Lead唯一提交与clean验收owner。
