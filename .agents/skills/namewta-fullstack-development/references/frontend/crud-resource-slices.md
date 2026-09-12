# CRUD 与 Controller 资源纵切片

新增、迁移或审查 `packages/domains/**`、`packages/web-domains/**` 的 Controller 资源时读取本文件。目标是让后端合同、domain、Vue 页面、package exports 和 App 组合保持可定位，不把所有能力继续堆在包根。

## 证据顺序

1. 同 owner、同交互形态且已经过测试的成熟 domain/web-domain/App 实现。
2. 当前 `package.json#exports`、platform/runtime port、后端 Controller/BO/VO 或 OpenAPI 合同。
3. 父仓库 `docs/fm/README.md`、`docs/fm/catalog.json`、`docs/fm/context-contract.md` 与相关 `docs/fm/vue/*.ftl`。
4. 框架通用做法。

每次 CRUD/树表资源变更都必须实际读取第 3 项；它是当前静态骨架和审查清单，不是可跳过的历史样例。若模板与前两项冲突，以当前实现合同为准并同步修正模板。不要只在业务实现中打补丁，让模板继续生成同类偏差。

## 目录目标

```text
packages/domains/<module>/src/<resource>/
  index.ts              # 显式公开入口 + Controller 元数据
  types.ts              # 本资源拥有的领域 model/query/form/command
  transport.ts          # 可选：外部 transport 校验与领域投影
  service.ts            # 可选：port、service/factory 与 HTTP 操作
  *.test.ts             # mapper、规则、HTTP 合同

packages/web-domains/<module>/src/<resource>/
  <BusinessName>Page.vue
  composables.ts        # 可选：本资源有状态逻辑
  runtime.ts            # 可选：本资源宿主端口
  registration.ts       # 组件键、权限贡献、runtime fail-closed
  index.ts              # 显式公开资源合同
  *.test.ts
```

这些文件不是配额。没有 transport 映射、局部 composable 或局部 runtime 时不要创建空文件；多个同包资源确实共享稳定合同时，才把该合同上提到包级命名文件。没有页面的 headless、自服务、回调或服务端专用资源不创建 Web 空目录。

`profile` 的资源可定位基线是 `material-tags`、`person/application`、`person/rebind`、`person/materials`、`person/archive`、`enterprise/application`、`enterprise/transfer`、`enterprise/materials`、`enterprise/archive`。管理页面可由较粗的 Web owner 聚合多个 headless Controller；该选择必须在 README/manifest 可追溯，并保持后端菜单已有 componentKey。不要为 verification callback 创建空前端资源。

Profile 当前 OpenAPI 快照 `packages/api-contracts/openapi/current.json` 尚无 `/profile/**`。过渡期可在 domain 资源 `service.ts` 中维护已核实的 typed HTTP 合同，并用 URL/方法测试固定；快照纳入后必须使用 `tooling/openapi` 生成 transport，再由 domain 映射。页面只能使用 domain-owned 类型。`material-tags` 是 domain/backend 名称，`material-tag` 是 Web 页面 owner 别名，组件键 `profile/materialTag/index` 保持不变。

## 文件职责

- `types.ts` 只声明资源拥有的领域语义。Identifier、响应壳等确被多个资源共用的合同可放包级 `types.ts` 或既有 platform 合同；不要在每个资源复制一套后再由根 `export *` 合并。
- `transport.ts` 只处理外部 transport 到领域模型的投影。对缺失、nullable、非法字面量和畸形树结构明确 reject/default/normalize，并用测试固定；不把 Axios 或浏览器类型带入 domain。
- `service.ts` 拥有该 Controller base path 的请求接口与 factory。只读调用使用 GET，业务变更使用 POST；URL、字段、分页、ID 和 nullable 语义逐项对照后端。
- 资源 `index.ts` 只显式导出上述公共合同并声明 Controller/basePath 元数据。包根 `index.ts` 只保留 domain module、稳定 facade 或 manifest 组合。薄入口按职责判断，不按行数判断。
- `composables.ts` 管理页面查询、选择、表单与提交状态；快速切换使用 sequence/abort 等策略阻止过期响应，卸载后不写入，所有 Promise 有 owner，重叠 loading 有确定语义。
- `runtime.ts` 描述 service、反馈、字典、上传下载、导航和宿主组件等依赖，并提供同步的 `requireXxxRuntime` 失败关闭。浏览器单例、Router、Store 和 adapter 实例仍由 App 拥有。
- `registration.ts` 只贡献页面 registration 和权限清单；包级 manifest 显式汇总这些贡献，不靠 import 副作用注册。

树的 parent/root、循环、孤儿和领域排序由 domain 处理并测试；web-domain 只处理展开、选择、父节点过滤和可视交互。禁止在页面模板再实现第二套构树算法。

## 公开入口与兼容 facade

每个对外资源同步增加：

```json
{
  "exports": {
    ".": "./src/index.ts",
    "./resource-name": "./src/resource-name/index.ts"
  }
}
```

嵌套资源按完整子路径公开，例如 `./monitor/login-info` 或 `./person/archive`。消费者从 `@namewta/domain-<module>/<resource>` 或对应 web-domain 子路径导入，禁止导入 `src/**`。

迁移存量包时可保留根级 factory/type 名称作为兼容 facade，但其实现应显式组合资源 factory；先盘点调用方和测试，再逐步迁移。禁止通过 `export *` 将所有资源重新摊平成根级大桶，也不要借结构重构改变正确 URL、权限、组件键或业务状态机。

## 模板生成后的人工集成

`docs/fm/catalog.json` 的 Vue targets 只覆盖资源局部文件。模板不会也不应覆盖共享入口。每次参考或渲染模板后，人工完成：

1. 将资源加入 domain/web-domain `package.json#exports`。
2. 由包根 domain module、兼容 service facade 或 web-domain manifest 显式组合。
3. 更新包 README 的后端模块、Controller、页面 owner 和公开入口。
4. 将资源加入 `tooling/architecture/test/domain-layout.test.mjs`，同时断言 `index.ts`、`types.ts` 和 exports；有页面时再加入 web resource 断言。
5. 在目标 App 的 `package.json`、`application/services.ts` 和 manifest registry 三处同步选择。
6. 增加 domain mapper/HTTP、web registration/状态和 App 失败关闭测试。

## 验证

先运行受影响包的 `lint`、`typecheck`、`test`，再从 `plus-ui-namewta` 根运行：

```bash
pnpm architecture:check
pnpm architecture:test
pnpm lint
pnpm typecheck
pnpm test
pnpm build:prod
```

涉及动态菜单、权限、快速交互或跨端身份时按风险追加 `pnpm test:e2e`。修改 `docs/fm/**` 时另运行 `node docs/fm/scripts/validate.mjs`，并用代表性普通表和树表上下文验证渲染结果；静态模板校验不能替代目标工程编译与行为测试。
