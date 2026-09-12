# App 组合运行时

`@namewta/platform-app-runtime` 提供 App、domain 与 web-domain 的类型化组合合同。它按 App 显式选择的领域构建不可变运行时，聚合路由和组件注册，并对重复键、未知领域、缺失依赖与未选择能力失败关闭。

服务端菜单通过 `projectServerRoutes` 生成新的确定性投影。调用方显式决定是否展平 `ParentView`，并注入特殊宿主组件、manifest resolver 与缺失组件诊断；空或畸形 children 不会保留隐式 redirect。`findDuplicateRouteNames` 返回稳定、去重的结构化诊断，如何呈现由 App 决定。

本包不选择具体 App 能力，不拥有页面、布局、路由器单例、请求实现或业务领域规则。可依赖最小平台合同，但不得依赖 App、domain 实现、web-domain 或浏览器适配器。

验证应覆盖选择顺序、不可变结果、重复注册、缺失领域、未选择注册和稳定诊断。