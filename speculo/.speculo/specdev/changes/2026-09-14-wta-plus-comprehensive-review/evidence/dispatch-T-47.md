# Dispatch Packet T-47-20260923-01

operation=dispatch；task_kind=implementation；delivery_channel=native；provider=gpt-6-sol/xhigh；owner=cors_audit；Lead=single-agent。base_sha `97e1ee9e1ad40de75deffd025379a6a5488c4882`，current / main / direct-parent；单产品writer，禁止新worktree。

先完整Map→工程/全栈/模块Skill及前端scope references、最近AGENTS→ticket47/AC047。T34不可变验收已关闭，预研/tmp/wta-t47-audit.md仅导航，源码为准。

写集仅frontend/packages/web-domains/system/src/oss/，包括真实SFC组件测试。Lead独占SpecDev工件/commit；其余产品只读。IN：启动时深快照query；config/list/preview结果本地聚合，仅最新代且未卸载可一次提交；loading/error同属当前代；当前失败可见可重试，旧失败无污染；独立mutation loading。保持授权URL按需申请及精确deleted语义，不用列表中不可信URL。不要改变公共service、依赖、生成物、后台/API/SQL；T34两处泛型提示已落地。

允许可逆实现与非E2E组件/包测试、lint/typecheck，日志到/tmp/wta-t47。先受控真实SFC红灯再修正；A/B config/list/preview逆序、旧异常不清Bloading、当前异常重试、空结果、嵌套query快照、卸载、mutation/query并行均需行为验证。避免复制实现的假测试。没有服务/Docker/部署/浏览器E2E权限；本票E2E not-required，T30组合验收。

不提交。返回路径、固定base/currentHEAD、commit=null、dirty、Skill执行要点、真实命令/cwd/exit/count/skip/日志与未验证项，交回锁。遇写集/合同/依赖冲突先停报告，不自行扩范围或改状态。Lead随后提交、精确clean集成门禁和双轴审查。
