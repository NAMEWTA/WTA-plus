# Web 权限宿主

`@namewta/web-kit-permission` 为 Vue Web App 注册全局 `v-hasPermi` 与 `v-hasRoles` 指令。App 必须注入一个按调用返回当前 `AccessEvaluator` 的 provider；宿主不读取 Store、Router 或会话持久化，也不缓存角色与权限快照。

权限语义只由 `@namewta/platform-permission` 决定。非法绑定、缺少求值器或不匹配均失败关闭；异常路径会先从宿主 DOM 移除受限元素，再传播稳定错误。指令只控制界面呈现，后端仍负责最终鉴权。
