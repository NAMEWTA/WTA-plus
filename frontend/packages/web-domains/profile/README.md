# 档案管理 Web 领域

`@namewta/web-domain-profile` 对应 profile 领域的材料标签、个人档案和企业档案页面。管理端页面 owner 分别通过 `./material-tag`、`./person`、`./enterprise` 子路径公开；用户端通过 `./self` 暴露档案中心和个人/企业认证表单。包根只校验 runtime 并组合完整 manifest。

页面目录按用户工作流聚合，不为每个无页面 Controller 建空目录。动态菜单组件键保持 `profile/materialTag/index`、`profile/person/*`、`profile/enterprise/*`；用户端额外注册 `profile/center/index`、`profile/person/application` 和 `profile/enterprise/application`，由目标 App 显式选择对应 manifest 后注册。缺失 runtime 会在 manifest 创建时失败关闭。

`material-tag` 是页面 owner 名称，领域合同仍使用 `material-tags`，对应后端 `/profile/material-tags`；这是稳定别名，不改变组件键或 URL。匿名验证回调仅由后端处理，不建立空页面目录。

本包通过 `ProfileWebRuntime` 获取领域服务、实时权限判断、确认/反馈、已授权材料下载、用户检索和 workflow 办理能力，不拥有 App 路由、请求单例、当前会话或后端授权。

验证命令：`pnpm --filter @namewta/web-domain-profile test`、`pnpm --filter @namewta/web-domain-profile typecheck`、`pnpm --filter @namewta/web-domain-profile lint`。
