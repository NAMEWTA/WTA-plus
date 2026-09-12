# Change Log

## 2026-09-01

- 用户报告 OpenAPI 路由 404、OpenAPI 菜单缺失、Nacos 菜单名称/位置错误，以及已退役生成器仍显示。
- D-diagnose-bugs 通过真实双实例 HTTP、DBX 权威查询、OpenAPI 组装测试和前端 manifest 测试确认根因。
- 用户明确要求按诊断计划执行。
- 接受 ADR-001 至 ADR-004。ADR-003 在当前 change 中覆盖旧 Nacos/OpenAPI 菜单显示合同；旧工件不改写。
- 本次授权包含本地实现 commit、当前父分支集成和计划内开发数据库修复；不包含远端 push、生产环境或同机 CDE。
- 发布两张 Ready/Deep Ticket 与 current/direct-parent Goal Plan；固定数据库先于双实例启用，开发环境写入受备份、0-row、恢复步骤和逐实例 Gate 约束。
- T-01 反向验证发现既有 OpenAPI SQL 前缀常量已落后于 backend HEAD 的后续 OSS SQL；将该 contract test 纳入 T-01，以实施前完整 HEAD 重建哈希门，历史 SQL 本身保持不变。
- T-01 全量 reactor 又暴露密码迁移测试的 15,370 字节 DML 前缀常量已在实施前 backend HEAD 失真；HEAD 与工作树前缀均为 `698675a3a16598df7313b90a5b267bb3cbfe9fe1a8e489737752189b3f58a81f`，只同步期望哈希，不改密码 SQL、固定长度或算法。
- T-01 标准轴审查发现 DDL 仅凭同名列即可删除生成器表、DML 对生成器固定菜单身份验证偏宽；新增“同名列非主键表”和“固定 ID 非历史菜单”两个隔离 MySQL 负向用例，确认修复前均红灯。
- T-01 将 DDL 前置收紧为 `BASE TABLE` 且冻结 ID 列必须为 `PRI`，并用历史名称、路径、client、component/permission 补齐九个生成器菜单身份验证；隔离 MySQL 静态加数据库矩阵 `9/9` 通过，其中数据库场景 `7/7` 通过。
- T-01 完成标准轴与 Spec 轴固定点审查：未发现剩余阻塞 finding；最终 `./mvnw -pl ruoyi-admin -am test` 为 `328` 项、`0` 失败、`23` 项环境型跳过，`git diff --check` 通过。
- T-01 形成 backend implementation commit `9f5d382ada11e4bbc01bb7b49ca8ed4c6770f6ef`，父仓库 direct-parent checkpoint `1bae24677e601b6313820a3198fc0e8690ab8248` 的 gitlink 精确包含该提交；开发库备份/迁移 Gate 尚未执行，Ticket 保持 `in_progress`。
- 用户在 G1 后明确决定“无需备份”；新增并接受 `ADR-005`，本次目标开发库迁移改为无备份、仅前向修复。0 行、对象身份、固定菜单冲突、禁止 `--force`、目标最终态和逐实例 Gate 保持不变。
- T-01 以 backend follow-up commit `c7e926c8ac663b1fc83e1df2abf39ffcebbecef3` 同步 DDL/DML 运维注释；SQL 行为未变化，聚焦测试仍为 `14` 项、`0` 失败（无 JDBC 时 `7` 项环境型跳过）。
- T-01 通过 DBX 对 `ry-namewta` 完成无备份写前 Gate：生成器表 `0/0`、对象身份正确、固定菜单与 parent/duplicate preflight 全部通过；DDL/DML 自身 preflight 均为 `1`。
- DBX 在任何持久化 DML 前拒绝显式 `START TRANSACTION`；同一 pinned session 改用 `autocommit=0` 执行已提交 DML，并通过恢复 `autocommit=1` 原子提交。最终四项诊断全真，重放前后指纹同为 `2d76cc06b7238381aead6178ed5bf0ba`，目标角色授权和生成器表均保持 `0`。
- T-01 Evidence 完成并标记 `done/integrated`；T-02 依赖 Gate 关闭并进入 `in_progress`。
- T-02 先以 Node 合同测试确认共享锚点缺少 OpenAPI 变量的红灯，再在三个 writable release path 中补齐 default-off 双实例同源透传和公开占位符；aggregate source checkpoint 为 `97d67cf3aac1ce0dfdd38a5c8e3b1b235c7f3e8d`。
- T-02 本地门通过 Node `18/18`、Spring assembly `7/7`、Admin `41` tests、system web-domain `23` tests、两包 typecheck 和 Admin production build；本机无 Docker CLI，Compose parsing 明确留给目标只读 Gate，未虚报 G3 完成。
- T-02 首轮真实启用暴露 Actuator 第二个 `RequestMappingHandlerMapping` 注入歧义；server1 恢复且未推进 server2。backend `b694dd4b84dd442f7f2e3247c57a184aa2698e50` 与 aggregate `22941077a547bdf3afc86c93fedf704e55cd2b42` 以 qualifier 和 `8/8` assembly 回归完成纠偏。
- 第二轮登录浏览器发现缺失凭据的 `R.ok(null)` 被误判为能力不可用；backend `1300301098d0b27805f4224ef83e5b49262e4ef5` 以业务 404 和 `4/4` Controller contract 修复，最终 Admin reactor 为 `330` 项、0 失败、23 项环境型跳过。
- aggregate final result `ae3689ec419fb97e0a216e4ab44b85a3517d3bd9` 构建镜像并按 server1 -> server2 滚动；两实例同镜像、healthy/0 重启、配置一致、未认证路由业务 401、关键日志扫描 0，目标四文件 Compose parse 通过。
- 正确 `/namewta/` 前端构建经连接等待窗口发布，最终 index 哈希与资源前缀通过；错误 root-base 构建、瞬时 502 rollback、旧镜像和失败候选均保留为恢复资产，未执行未授权 cleanup。
- 全新登录浏览器确认本人凭据业务 404 与目录业务 200 被正确组合为“尚未创建凭据”空态；OpenAPI管理位于系统管理、Nacos配置中心位于系统监控，系统工具/代码生成均不存在。T-02、G3/G4 与 change 全部完成。
