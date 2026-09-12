# 前端领域 CRUD 与页面实现规范

适用 `packages/domains/**`、`packages/web-domains/**`、`packages/api-contracts/**` 及目标 App 的显式组合。当前模板基线位于父仓库 `docs/fm/vue/**`，按 domain/web-domain 目录生成资源切片，但不替代成熟实现与 App 显式组合。

### FE-CRUD-001 采用领域纵切片

Scope: `module:plus-ui-namewta`

Level: MUST

Source: `repository-fact`（demo/system 等领域实现）

Rule: 新能力按“生成 transport（需要时） -> domain-owned model/mapper/service -> web-domain 页面/manifest -> App 显式选择”落地。页面不得直接把 OpenAPI 生成类型当领域状态，也不得直接创建全局请求实现。

Verification: 对照后端 controller/BO/VO/OpenAPI；review mapper、公开 exports 与 App composition；运行受影响 domain/web-domain/App 测试和 `pnpm architecture:check`。

### FE-CRUD-002 HTTP 合同与方法

Scope: `path:plus-ui-namewta/packages/domains/**`, `path:plus-ui-namewta/packages/api-contracts/**`

Level: MUST

Source: `API-005` + 后端合同

Rule: 请求/响应字段、分页、ID、boolean、nullable 与错误语义必须匹配后端。只读查询使用 GET，产生业务状态变化的操作使用 POST；domain 只依赖平台 HTTP 端口，不泄漏 Axios/Taro 类型。

Verification: 跨端合同 review；domain transport/mapper 测试；搜索受影响请求 method；`pnpm typecheck`; `pnpm test`。

### FE-CRUD-003 查询与变更状态

Scope: `path:plus-ui-namewta/packages/web-domains/**`

Level: MUST

Source: `repository-fact` + `builder-baseline`

Rule: 列表查询、分页、选择、表单草稿、提交和删除具有明确 owner；loading 在成功、业务失败、网络失败和取消后都进入终态。快速查询和卸载处理过期响应，变更成功后按领域语义刷新或更新状态。

Verification: 页面/组合式函数测试覆盖成功、失败、空结果、重复提交和过期响应；`pnpm lint`; `pnpm typecheck`; 相关 App build。

### FE-CRUD-004 树与层级数据

Scope: `path:plus-ui-namewta/packages/domains/**`, `path:plus-ui-namewta/packages/web-domains/**`

Level: MUST

Source: demo 树能力 + 后端合同

Rule: domain 明确父子 ID、根节点、循环/孤儿和排序语义；web-domain 保持展开、选择、父节点过滤和非分页行为，不在模板中重复构树。

Verification: domain 树转换测试；页面展开/选择/过滤测试；真实数据人工或 E2E 验收。

### FE-CRUD-005 权限与宿主副作用

Scope: `path:plus-ui-namewta/packages/web-domains/**`, `path:plus-ui-namewta/apps/**`

Level: MUST

Source: platform permission + App runtime contracts

Rule: manifest 组件键和权限标识与后端精确一致；未选择、缺少权限或畸形输入失败关闭。弹窗、字典、下载、上传、导航、iframe 等通过类型化宿主端口执行；前端权限不替代后端鉴权。

Verification: manifest inventory、权限负向和零副作用测试；相关浏览器流程；`pnpm architecture:check`; `pnpm build:prod`。

### FE-CRUD-006 复用必须有真实消费者

Scope: `module:plus-ui-namewta`

Level: SHOULD

Source: architecture decision

Rule: 页面局部逻辑先留在所属 web-domain，App 私有机制留在 App；只有多个真实消费者形成稳定合同后才提取到 `web-kit` 或 `platform`。禁止建立通用业务 `common/utils` 包。

Verification: review 消费者与稳定合同证据；架构检查；代表性 App 构建。
