# 个人中心与跨客户端审核

Home 登录后进入个人中心，显示“企业认证”“实名认证”。认证草稿、审核中、退回原因和已认证资料通过本人摘要接口获取。管理端和 Home 共用登录后壳层及图标，菜单仍来自各自 Client。

## 配置流程

1. 在设计器中保留唯一申请人节点，办理客户端选择“发起客户端”。
2. 每个审核节点选择一个具体客户端，再选该端角色或有该端登录资格的用户。发布时检查选人和客户端一致。
3. 审核人所属 Client 必须开放任务与认证审核接口，并拥有对应菜单和 `profile:person:task-review` 或 `profile:enterprise:task-review` 权限。
4. 本人待办、已办按节点客户端显示。同一人拥有多个客户端身份，也须在该节点指定的客户端办理。

角色与指定用户使用同一规则；转办、委派和加签不改变节点客户端。任一级退回后，申请人修改再提交，从第一审核节点重新办理。已办页仍展示原提交的材料快照。审核页面将“退回补充”与管理员覆盖的“拒绝（终态）”分别展示。

Home 基座提供非默认 `home_reviewer` 角色，但不自动分配用户。普通注册角色不获得审核权限。A/B/C/D 是测试场景，不额外创建生产客户端。

## 接口

| 用途 | 接口 |
| --- | --- |
| 本人认证状态与有效资料 | `GET /profile/{person\|enterprise}/application/summary` |
| 当前或已办任务提交详情 | `GET /profile/{person\|enterprise}/review/tasks/{taskId}` |
| 任务材料地址 | `GET /profile/{person\|enterprise}/review/tasks/{taskId}/materials/{materialRefId}/access-url` |
| 审核决定 | `POST /profile/{person\|enterprise}/review/tasks/{taskId}/decision` |

决定正文为 `decision`（`APPROVE` 或 `RETURN`）、必填 `reason`、已查看的 `snapshotVersion`。任务和申请编号关联由服务端读取，不能自行拼接另一个申请或材料。管理员覆盖决定继续使用独立的原有接口和权限。

## 跨域与密码

`WEB_CORS_ALLOWED_ORIGINS` 可使用精确地址、`http://192.168.*:[*]`、`https://*.example.com`，也可直接使用 `*`。CORS 配置独立记录在发布 manifest，实际网站地址及 SSO 回调继续填写精确地址。

初始化密码读取 `sys.user.passwordPolicy`。默认每次随机生成 12 位，满足大小写、数字和特殊字符要求。`sys.user.initPassword` 已停用；参数说明调整不会修改已有账号密码。

## 存量升级

全新环境使用现有六份基座；现有环境不得重放基座。升级必须使用已确认源/目标版本差异，在备份副本中演练：

- 新增 `flow_instance_node_client` 表，并为已有实例逐节点填入明确 Client。角色能证明审核端归属，但裸用户 ID 和发起者 ID 无法证明历史登录端；无法确认的记录列为待处理项，不能自动设成管理端。
- 原流程版本和历史保持不变。新的定义配置申请节点与审核节点客户端，并通过复制新版本发布。认证旧种子缺少申请人 BETWEEN，应使用修正版本启动新申请。
- 核查旧认证流程是否曾自动完成；修复代码不等于撤销或更改已发布的历史认证结果。
- 按差异补齐 Home 任务菜单、审核权限和非默认角色；更新路径配置后重新登录以刷新 Token 上下文。
- 只修正密码参数名称和旧说明，保留既有策略、迁移标记和账号密码。

本次开发与隔离验证不执行生产部署、历史数据改写或基座重放。验证结果见同目录 `worklog.md`。
