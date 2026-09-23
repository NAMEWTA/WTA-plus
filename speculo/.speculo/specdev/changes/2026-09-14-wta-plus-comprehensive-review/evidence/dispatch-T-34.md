# Dispatch Packet T-34-20260923-01

operation=dispatch；task_kind=implementation；delivery_channel=native；provider=gpt-6-sol/xhigh；owner=/root/cors_audit；Lead=/root (single-agent)。

Fixed base `6fcbfeb50747b67bcaf4bf9af1d964cc7d760c04`；repo=/srv/WTA-plus；branch=main；workspace_ref=current；strategy=current/direct-parent；唯一产品writer串行锁=T-34。无依赖；T32/33出口已关闭。

先完整读本change tickets-map.md（revision142）→.agents/skills/engineering-standards/SKILL.md、namewta-fullstack-development/SKILL.md、wta-module-guide/SKILL.md及对应前端/Notify引用和AGENTS→ticket/34-inbox-without-realtime.md→Goal/AC-034。此前只读调查不替代派单权威合同。

IN：实时关闭仍本人REST可读；加载/失败重试/空状态；单份摘要；身份切换的成功/异常/详情隔离与卸载；已有读/读全刷新保持。OUT：完整分页T41、后端业务/通知协议、OIDC、secret/env/部署、其他产品。

Writable仅Ticket T34五组路径：push.ts、push.test.ts、layout/components/notice/、store/modules/notice.ts、frontend/e2e/。Lead保留所有SpecDev工件；其他产品、配置/依赖/生成物只读，需扩范围先报告。不能创建worktree，不提交/推送/部署，不运行真实E2E Gate。可开发Playwright用例，Lead使用其选择器实际验收。

实现保持最小：flag只控制连接；token/generation守护成功与异常；复用当前通知domain service，视图不直连API。去重/dirty刷新仅覆盖已证明竞争，不引入全局状态机。合成UI用例与真实后端E2E明确分开；不能用route.fulfill登录/inbox替身证明真实Admin合同。

允许产品与测试可逆修改及非E2E单元/组件/lint/typecheck/architecture/build。先红灯再最小绿灯；日志到/tmp/wta-t34，保留commands/cwd/exit/count/skip。Lead正在准备真实服务环境；不启动自己的Docker/Maven/生产连接。

最低非E2E：admin-web test＋受影响lint/typecheck/architecture及构建；不放宽规则/测试/类型。E2E owner Lead=current-workspace，真实Admin登录→开盒子→本人公告，前后端实时false，零票据/stream；加合成组件状态/身份乱序/旧异常/单份内容场景，给出可执行选集。

停止条件：新Skill/路径/合同边界、其他writer、产品base漂移、必要依赖未满足。Lead治理变更是预期，不因其dirty误判产品冲突。返回Ticket/workspace/base/currentHEAD/commit=null pendingLead、真实paths、红绿命令/结果、Skill执行轨迹、E2E源码与选集/未验证限制，不写Evidence或Done。
