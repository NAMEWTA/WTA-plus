# Vue 资源纵切片模板

本目录只生成单个业务资源的 domain 与 web-domain 切片。实现前先读取 `namewta-fullstack-development` Skill、工程规范的前端 CRUD 规则，并对照同 owner 的成熟源码及当前后端 Controller/BO/VO/OpenAPI 合同；模板不是脱离项目现状的独立规范。

## 目标结构

```text
packages/domains/<module>/src/
  index.ts                    # 包级显式组合，手工维护
  service.ts                  # 仅在多个资源需要聚合兼容服务时手工维护
  types.ts                    # Identifier、ApiResponse、PageResult 等共享合同
  <resource>/
    index.ts                  # 薄公开入口与资源元数据
    service.ts                # HTTP 用例与 transport -> domain 组合
    transport.ts              # OpenAPI 边界投影；树结构校验也在 domain
    types.ts                  # 该资源独有 Form/Query/VO

packages/web-domains/<module>/src/
  index.ts                    # 包级 manifest 汇总，手工维护
  runtime.ts                  # 仅在多个资源真实共享时手工维护
  composables.ts              # 仅在多个资源真实共享时手工维护
  <resource>/
    <BusinessName>Page.vue
    composables.ts
    index.ts
    registration.ts
    runtime.ts
```

`index.ts` 的“薄”按职责判断，不按行数判断：只允许显式 export、资源元数据或 manifest/module 组合，不放 HTTP 实现、整套模型、页面状态或顶层注册副作用。禁止根入口 `export *` 汇总所有资源；消费方从 `@namewta/domain-<module>/<resource>` 与 `@namewta/web-domain-<module>/<resource>` 导入。

## 使用顺序

1. 后端 Maven 模块决定一级 `<module>`，Controller base path 决定 kebab-case `<resource>`；同一业务体验可组合多个 Controller，但必须记录映射。
2. 在 domain 包的 `types.ts` 手工确认真正共享的 `Identifier`、`IdentifierList`、`ApiResponse`、`PageResult`、`BaseEntity`、`PageQuery`。只有两个以上资源真实复用时才上移，不复制多份后再由根入口通配导出。
3. 选择普通表或树表页面模板。树表由 domain 的 `build<BusinessName>Tree` 定义根、重复 ID、孤儿、循环和排序语义；web-domain 不构树。
4. 将资源子路径写入 domain/web-domain `package.json#exports`，再由所属包根入口显式组合兼容服务或完整 manifest。`registration.ts.ftl` 只贡献一个 registration 与权限清单，不自动注册。
5. 目标 App 必须显式加入 domain service、web-domain manifest/registration 与 package dependency。动态菜单 `componentKey` 和权限字符串必须与后端完全一致，缺 runtime、未知组件或重复注册失败关闭。
6. 为 transport 投影、树异常、service method/path、runtime 缺失、manifest inventory、查询成功/失败/空结果/重叠响应/卸载和重复提交补测试，再运行受影响包及根级架构、lint、typecheck、test、build 门禁。

## 所有权约束

- `service.ts.ftl` 只依赖平台 `HttpClient`，查询使用 GET，CRUD 变更使用 POST；路径必须先与当前后端核对。
- `transport.ts.ftl` 将 OpenAPI transport 投影为 domain model，缺失必需字段时拒绝，不用页面断言掩盖畸形响应。
- `runtime.ts.ftl` 是 web-domain 与 App 的类型化宿主端口，缺失时在 manifest 工厂阶段失败关闭。
- `composables.ts.ftl` 默认属于当前资源。只有同包多个真实消费者时才提升到包级；只有跨包多个消费者形成稳定合同后才进入 `web-kit`。
- 查询采用 latest-wins，并在 scope dispose 后禁止提交结果；loading 使用进行中任务计数，不能由一个较早请求提前清除另一个请求的状态。
- 页面事件返回或 await Promise；ID 用 `??` / `!= null` 判断，不能把 `0` 或空字符串等合法边界误判为缺失。仅图标按钮必须提供可访问名称。

模板不会覆盖 package 根 `index.ts`、聚合 `service.ts`/`runtime.ts`、`package.json` 或 App 组合文件；这些共享文件必须在资源生成后按当前源码手工接入并评审。
