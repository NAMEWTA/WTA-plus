# 上游边界

`main` 是 NAMEWTA 产品分支，`6.X` 是只允许 fast-forward 的上游镜像分支。业务提交不得进入 `6.X`，基线标签 `namewta-base-upstream-6x` 不得移动。

上游更新用于发现能力、缺陷修复和安全变化，不要求本地目录与上游同构。评估时应先确认行为和风险，再映射到当前 Maven 模块、公开 API 与测试。

NAMEWTA 当前已在上游基础上形成 Client 登录域/RBAC 隔离、OSS 直传与生命周期、统一通知、完整 HTTP 系统日志和父仓库统一 MySQL 基座等产品边界。涉及这些区域的上游提交必须按行为适配，不能通过整目录覆盖回退本地安全语义。

常见映射原则：

- 登录和 Token 变化同时复核所有认证策略、ClientContext、LoginUser、动态菜单和会话清理。
- system 的角色、菜单、用户或 Client 变化同时复核 `userId + clientPk` 查询和无上下文失败关闭。
- OSS、邮件、短信或通知变化优先接入现有公开服务和 channel adapter，不恢复业务模块直连工具类。
- Web Filter、异常处理或日志配置变化必须保留 requestId、异步完成、正文策略和日志失败不影响业务的合同。
- 上游 SQL 变化先按行为合入父仓库 `release-artifacts/docker/infrastructure/mysql/init/` 对应完整基座；后端仓库不得恢复 `script/`、SQL 副本或非 MySQL 方言。

在父工作区中，认证、权限、Client、菜单、OSS、通知和 SQL 的长期合并约束以 `docs/upstream/customization-map.md` 为准。
