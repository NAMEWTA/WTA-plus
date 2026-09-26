# T-42 Dispatch05 前置复盘（待具体写集登记）

固定输入73c28e751f6276e45b0488ab5d8eacbf9f0c77fb。T42仍in_progress，全部AC仍未勾选。本记录不宣布任何新代码已实现。

1. 共同模式：真实业务行为和默认Maven可通过，但尚不能覆盖分层静态约束。Dispatch04完成三项测试合同同步后，默认第一次又受构建输出异常影响；隔离编辑器后台编译后940执行通过/244环境skip，full/core成功。HTTP v4隔离验证码前置失败，v5同源码同JAR修夹具后成功。原始失败全部保留，不能把后来的绿色追写到此前输入。
2. 根因和证据：static04-notify-layered明确报告8项违反规则：两个generic adapter直接DAO/Service依赖，ActorPort错放runtime，Transactions类错放Service且事务不在UseCase。这是当前实现的真实结构缺陷，不是校验器误报。Maven与功能用例未执行此独立分层门禁；Lead在完整候选后期才跑该门禁，发现顺序过晚。
3. 新实质变化：按现有port→UseCase→Service→DAO形态迁移附件预约/确认/未知/释放边界；Spring代理继续在每次独立短事务包住原锁和CAS语义，复制/物化I/O保持事务外。端口及模型使用合法owner，禁止全限定名、allowlist、放宽validator或隐藏字段依赖绕过规则。先跑该静态门禁，再跑受影响单元、真实Mail27与通知135、默认/full/core及真实HTTP。前端和生成合同若实际无变更，以显式输入等价证据复用，不无依据重生成。
4. Owner与尝试：Lead负责治理/源码固定/全部构建服务/提交；仅在精确新路径登记提交后授权cors_audit为唯一产品writer，legacy_audit独立审查。新Dispatch05批attempts0；前批失败、JDT恢复及HTTPv4永久保留，三次限制不被抹除或作为盲重跑理由。新结构会改变后端输入，旧Mail27/135及HTTP仅作原候选历史，必须按影响重验。无生产部署、推送或归档。

已完成且待安全留存：backend默认/full/core c980；真实Mail27 43e5、通知135 cd78；HTTPv5 c980 run2a1672a910adffea；正式生成73c28；frontend663Vitest+108Node（额外101架构命令不重复计数）、三App329产物；facts2/FM/handbooks/release128通过，Notify layered失败8项。唯一full-suite结论仍pending。
