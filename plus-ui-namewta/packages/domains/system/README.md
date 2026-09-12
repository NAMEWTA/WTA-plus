# 系统管理领域

`@namewta/domain-system` 已激活，拥有用户、部门、岗位、角色、菜单、字典、参数、通知、OSS、个人资料、OpenAPI 和监控相关的领域模型、查询/命令服务与传输映射。

本领域不拥有 Vue 页面、表格交互、弹窗、编辑器、下载副作用、App 路由或后端授权，也不依赖具体请求适配器。宿主能力通过端口注入。

后端只映射 `wta-system` 的 `/system/**`、`/resource/**` 与 `/monitor/**` 接口。资源目录按 Controller base path 命名，例如 `client/`、`dict-data/`、`oss-upload/` 与 `monitor/online/`。Admin 选择完整 system Web 能力；Client 仅无界面组合身份与菜单端口。

包根 `src/index.ts` 只维护兼容导出与 `systemDomainModule` 元数据；共享响应、标识符和分配合同位于 `src/types.ts`，聚合 `SystemService` 及其工厂位于 `src/service.ts`。新增资源优先通过对应 package subpath 公开，不能继续向根入口加入 HTTP 实现。

OpenAPI 能力从 `@namewta/domain-system/open-api` 导出。`currentUser` 不接受 owner 参数，`targetUser` 的每项操作都要求显式 userId；所有生命周期命令使用 POST。传输响应在领域边界收窄，一次性 `appSecret` 只存在于 create/reset 的命令结果，不属于凭据摘要或用户列表模型。

验证覆盖请求参数、领域映射、资源安全边界、错误分类和公开入口。
