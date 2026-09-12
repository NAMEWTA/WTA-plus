# 档案领域

`@namewta/domain-profile` 对应后端 `wta-profile`。每个已消费的 Controller base path 都有独立资源子路径，`index.ts` 只公开资源 metadata、类型与 service；HTTP 实现留在资源 `service.ts`，边界投影留在需要它的 `transport.ts`。

| 公开子路径 | Controller base path |
|---|---|
| `./material-tags` | `/profile/material-tags` |
| `./person/application` | `/profile/person/application` |
| `./person/rebind` | `/profile/person/rebind` |
| `./person/materials` | `/profile/person/materials` |
| `./person/archive` | `/profile/person/archive` |
| `./enterprise/application` | `/profile/enterprise/application` |
| `./enterprise/transfer` | `/profile/enterprise/transfer` |
| `./enterprise/materials` | `/profile/enterprise/materials` |
| `./enterprise/archive` | `/profile/enterprise/archive` |

包根保留 `createProfileService`、`ProfileService`、`profilePermissions`、`profileDomainModule` 和既有类型名作为兼容 facade。新资源内代码优先从明确子路径导入；App 仍可通过包根创建聚合服务。`person.application` 与 `enterprise.application` 继续提供旧的换绑/负责人转移方法，同时分别公开 `person.rebind` 与 `enterprise.transfer`。

当前提交的 OpenAPI 快照尚无 `/profile/**` 路径，资源 service 的 URL、HTTP 方法和字段测试是暂态合同；待后端快照纳入后由 `tooling/openapi` 生成传输类型，再在 domain 边界映射，页面不直接引用 generated 类型。匿名验证回调没有前端资源。

本包不依赖 Vue、DOM、浏览器存储或具体请求实现。普通身份探测在 transport 边界严格投影为纯状态，只有个人完整身份匹配结果可以携带旧 system 手机号掩码；管理明文字段与材料访问仅由对应管理服务返回，后端仍是最终授权者。

验证命令：`pnpm --filter @namewta/domain-profile test`、`pnpm --filter @namewta/domain-profile typecheck`、`pnpm --filter @namewta/domain-profile lint`。
