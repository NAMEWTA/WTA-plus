---
schema_version: 3
artifact: spec
change: 2026-08-28-plus-ui-shared-navigation-permission-runtime
status: ready
ready_for_tickets: true
sources:
  - USER-DECISION:2026-08-28-final-consensus
  - ADR-001
  - ADR-002
  - ADR-003
  - CODE:plus-ui-current-permission-navigation-runtime
---

# Spec: Plus UI 共享导航与权限运行时收口

- **Spec：** `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/spec.md</Path>`
- **当前 ADR：** `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/ADR.md</Path>`
- **当前领域上下文：** `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/CONTEXT.md</Path>`
- **规划深度：** Deep。原因是本变更触及认证后导航恢复、权限可见性和共享核心路径。

## 1. 问题与目标

### 问题陈述

当前多个 Web Domain 页面已经直接使用全局 `v-hasPermi` 与 `v-hasRoles`，但指令实现仍位于 Admin App 私有目录并直接读取 Admin Store。未来新增 Vue Web App 时，即使它复用同一组 Web Domain，也必须复制或依赖 Admin 私有实现，真实宿主合同与源码所有权不一致。

Admin 当前名为 permission 的 Store 同时承担服务端菜单树转换、动态组件解析、Router 注册和侧栏/顶栏/默认路由投影。它把可复用的纯菜单流程与 Admin 自有导航状态混在一起，并保留空 `dynamicRoutes` 和 App 本地 `views` 动态扫描等已无消费者的旧入口。这既降低职责可读性，也会绕过已接受的 Web Domain manifest 选择边界。

本变更需要收口这条认证后纵向链路，同时保持后端按 Client 裁剪菜单与权限的权威、现有 Admin 用户行为和失败关闭安全规则不变。

### 目标用户与场景

- **前端 monorepo 维护者：** 能从稳定的公开包边界找到跨终端权限算法、Vue Web 权限宿主和服务端菜单纯流程，不需要从 Admin 私有目录复制实现。
- **新 Vue Web App 实现者：** 能注入自己的会话权限求值器、选择 Web Domain manifest，并把共享菜单结果投影到自己的 Router 与状态容器。
- **移动端或小程序实现者：** 能继续复用无 Vue 依赖的权限算法，而不被迫依赖 DOM、Vue Router 或 Pinia。
- **Admin 用户：** 登录后仍按同一 Client、角色、权限和服务端菜单进入相同页面，看到相同导航与按钮可见性结果。
- **排障与评审人员：** 对未知组件键、未选择领域、重复路由名称和畸形输入获得稳定、可测试、失败关闭的结果。

### 成功标准

- Web Domain 对 `v-hasPermi`、`v-hasRoles` 的依赖由独立 Web Kit 权限宿主合同满足，宿主不读取任何具体 App Store。
- 跨终端权限语义继续只有一个权威实现；Vue 指令与命令式权限检查得到一致结果。
- 服务端菜单的确定性转换和诊断可脱离 Pinia、Router 单例、Admin Layout 与 UI 消息组件测试和复用。
- Admin 中的状态容器以导航职责命名，并继续只保存 Admin 的路由、侧栏、顶栏和默认导航投影。
- 认证后的调用顺序严格保持 `getInfo -> getRouters -> addRoute -> replace`，不增加重复身份或菜单请求。
- 动态领域页面只经特殊宿主组件或当前 App 已选择的 `WebDomainManifest` 解析；旧动态路由与本地 `views` 兜底完全移除。
- 缺失身份、畸形权限或菜单、未知或未选择组件键和权限不匹配都不会默认放行。
- 五个迁移阶段各自验证绿色后才进入下一阶段，最终通过前端架构、静态、单元、双模式构建和 Admin 端到端门禁。

### 非目标

- **OOS-001：** 不修改后端 Controller、HTTP/JSON 合同、Client 裁剪、角色权限算法、数据库或 SQL；本变更是前端运行时所有权收口。
- **OOS-002：** 不激活或实现 client-web、mobile-web、miniapp-taro；它们继续保持 README-only 占位。
- **OOS-003：** 不建立公共 Pinia Store、公共 Vue Router 实例、公共 Admin Layout 或统一所有 App 的导航外观。
- **OOS-004：** 不重构与认证后权限/导航纵向切片无关的 App Store、插件、工具、布局、样式或领域页面。
- **OOS-005：** 不引入微前端、运行时插件市场、远程模块加载或发布到外部 npm registry。
- **OOS-006：** 不以按钮隐藏或前端路由过滤替代后端接口鉴权。
- **OOS-007：** 不保留旧 Store 名称、空动态路由、旧函数入口、本地动态页面兜底的别名、转发门面、功能开关或双轨实现。

## 2. 解决方案与外部行为

### 解决方案摘要

能力按运行时依赖与状态所有权分为三层：

1. 跨终端权限算法继续由 Platform Permission 提供，输入是当前身份的角色与权限集合，输出是确定性的授权判断。
2. 独立 Web Kit 权限宿主把 App 注入的权限求值器注册为 Vue 全局 `v-hasPermi` 与 `v-hasRoles` 指令；它不感知 Admin Store。
3. Platform App Runtime 提供服务端菜单的纯转换与稳定诊断；具体 App 继续拥有 Router、状态容器、布局、白名单、特殊宿主组件、manifest 选择和诊断呈现。

Admin 使用这些公开能力完成显式组合，并把原 permission Store 收口为 App 自有 navigation Store。动态领域页面遵循 manifest-only 解析；Admin 自有登录、错误、首页、个人中心等页面继续使用显式静态路由或明确的 App-owned manifest。

### 主要流程

#### 登录后恢复受保护导航

```text
进入受保护地址
  -> 获取当前身份 getInfo
  -> 获取后端按 Client 裁剪的菜单 getRouters
  -> 纯函数规范化菜单树并解析组件键
  -> navigation Store 保存 Admin 导航投影
  -> Router 逐项 addRoute
  -> replace 回原目标地址
```

该流程只消费服务端已裁剪结果，不在前端再次执行跨 Client 菜单授权补偿，也不从本地 `views` 扫描补齐服务端未注册页面。

#### Web 页面按钮与操作可见性

```text
App 从自己的会话状态创建 AccessEvaluator
  -> App 安装 Web Kit 权限宿主
  -> Web Domain 使用 v-hasPermi / v-hasRoles
  -> 指令调用注入的 evaluator
  -> 允许时保留元素；拒绝或身份不可用时移除元素
```

命令式权限检查继续直接复用同一权限求值语义，因此同一输入不得出现指令允许而命令式检查拒绝，或相反的分叉。

### 边界、失败与稳定错误行为

- 权限或角色指令绑定必须是有效、非空的字符串集合；非法绑定保持显式失败，不可按“无要求”放行。
- 缺少会话身份、权限集合或角色集合时，权限判断失败关闭。
- 普通用户只在任一所需权限或任一所需角色匹配时通过；超级管理员与通配权限保持现有语义。
- 服务端菜单为空时产生空的动态导航投影，不扫描本地目录猜测页面。
- 畸形菜单节点不得生成可访问的未知页面；转换行为必须确定、可测试且不静默扩大访问范围。
- 特殊宿主组件仅从 App 明确提供的映射解析；领域组件仅从当前 App 明确选择的 manifest 注册表解析。
- 未知组件键或属于未选择 Web Domain 的组件键必须失败关闭，并产生包含稳定组件键上下文的诊断，不能退回任意本地文件加载。
- 重复 route name 必须被确定性识别并通过 App 的诊断呈现边界报告；共享纯函数不得直接依赖 UI 消息组件。
- 身份获取、菜单获取、组件解析或路由注册失败时，不得继续 `replace` 进入一个未完成注册的目标页面。

### 状态转换与不变量

```text
未恢复
  --getInfo 成功--> 身份已加载
  --getRouters 成功--> 菜单已加载
  --转换与 addRoute 成功--> 导航已注册
  --replace 成功--> 恢复完成

任一步失败 --> 失败关闭；不得跳过前置状态继续后续步骤
```

- 后端返回的 Client 裁剪菜单是前端动态导航输入权威。
- `getInfo` 必须先于 `getRouters`，全部内部动态路由完成 `addRoute` 后才能 `replace`。
- navigation Store 是 Admin 导航投影，不是后端权限权威，也不成为跨 App 公共状态。
- Vue Web 权限宿主只负责界面可见性；Platform Permission 保持无 Vue、无 DOM、无 Store。
- 共享菜单能力保持纯函数或显式依赖注入，不读取 App 单例。
- App 只能通过工作区包公开 `exports` 组合共享能力；不得深层导入或跨包相对导入。

## 3. 用户故事

- **US-001：** 作为前端 monorepo 维护者，我希望 Vue 权限指令拥有独立、无 Admin Store 依赖的宿主合同，以便 Web Domain 不再依赖某个 App 的私有实现。
- **US-002：** 作为新 Vue Web App 实现者，我希望注入自己的权限求值器并选择 manifest，即可复用权限可见性和菜单纯流程，以便无需复制 Admin Store 或 Router。
- **US-003：** 作为移动端或小程序实现者，我希望权限算法保持跨终端纯净，以便不引入 Vue、DOM、Pinia 或 Web Router 依赖。
- **US-004：** 作为 Admin 用户，我希望登录恢复、导航菜单、动态页面和按钮可见性在重构前后保持一致，以便目录与所有权调整不改变业务使用。
- **US-005：** 作为安全维护者，我希望缺失身份、畸形权限/菜单、未知组件和未选择领域均失败关闭，以便前端重构不扩大可见或可访问范围。
- **US-006：** 作为排障人员，我希望未知组件键和重复路由名称有稳定诊断，以便定位后端菜单与 App manifest 组合不一致。
- **US-007：** 作为代码评审者，我希望旧入口和兼容门面完全消失，且架构检查能阻止 App 私有依赖回流，以便新基座只有一个正式实现路径。

## 4. 验收合同

| ID | 前置条件 | 动作或事件 | 可观察结果 | 验证接缝 |
|---|---|---|---|---|
| AC-001 | Vue App 提供一个符合 Platform Permission 语义的求值器 | 安装 Web Kit 权限宿主 | 全局 `v-hasPermi` 与 `v-hasRoles` 可供 Web Domain 使用；共享宿主不读取任何 App Store | Web Kit 指令单元/组件测试；架构依赖检查 |
| AC-002 | 指令分别收到匹配、不匹配、超级管理员、通配权限和非法绑定 | Vue 挂载受控元素 | 匹配时保留，不匹配或身份缺失时移除；超级管理员和通配权限保持现有结果；非法绑定显式失败且不放行 | 权限指令特征测试与 Platform Permission 单元测试 |
| AC-003 | 同一角色与权限输入同时用于指令和命令式检查 | 执行两种检查 | 两种入口返回一致的授权结果，不存在独立权限算法 | Platform Permission 与 Web Kit 组合测试 |
| AC-004 | 非 Web 终端只依赖 Platform Permission | 执行该包构建与依赖检查 | 无 Vue、DOM、Pinia、Vue Router 或 Admin App 依赖 | 包级 typecheck/build；架构检查 |
| AC-005 | 已登录用户进入需要恢复的 Admin 地址 | 路由守卫恢复导航 | 事件顺序严格为 `getInfo -> getRouters -> addRoute -> replace`，且每类请求只执行当前流程所需次数 | App Runtime 导航恢复单元测试；Admin 守卫测试；Playwright |
| AC-006 | 后端返回已按 Client 裁剪的菜单 | Admin 生成动态导航 | 前端只转换该菜单并注册当前 App 可解析的路由，不用本地权限再次补偿或扩展菜单 | 菜单纯函数测试；navigation Store 测试；Client/菜单 E2E |
| AC-007 | 菜单含 Layout、ParentView、InnerLink、空 children 或多级节点 | 执行共享菜单转换 | 特殊宿主、ParentView 展平、空 children 清理和外链行为与冻结基线一致，输入得到确定输出 | Platform App Runtime 表驱动单元测试 |
| AC-008 | 菜单组件键存在于当前 App 选择的 Web Domain manifest 或 App-owned manifest | 恢复动态导航 | 对应页面可解析、注册并导航，Admin 静态页面仍由显式静态路由工作 | manifest registry 测试；Admin Store 测试；Playwright |
| AC-009 | 菜单组件键未知或属于当前 App 未选择的 Web Domain | 恢复动态导航 | 不从 App 本地 `views` 扫描兜底，不注册未知页面，并产生带组件键上下文的稳定诊断 | manifest 失败测试；源码清理扫描；Playwright 失败场景 |
| AC-010 | 菜单转换得到重复 route name | 完成转换并交给 App 呈现诊断 | 重复项被确定性识别；共享层不直接调用 Element Plus 或其他 App UI 单例 | App Runtime 诊断单元测试；Admin 呈现接缝测试 |
| AC-011 | Admin 导航数据加载完成 | 布局、侧栏、顶栏、标签页或守卫读取导航状态 | 所有消费者使用 App 自有 navigation Store，现有路由、侧栏、顶栏和默认导航投影行为不变 | Admin Store/布局测试；typecheck；Playwright |
| AC-012 | 身份、权限、菜单、组件解析或路由注册任一步失败 | 尝试进入受保护页面 | 流程失败关闭，不继续 replace 到未完成注册的目标，不默认展示受限元素或页面 | 单元失败路径；Admin 守卫测试；Playwright |
| AC-013 | 完成 Admin 迁移 | 扫描工作区正式源码与公开出口 | 旧 permission Store 标识、App 私有权限指令实现、空 `dynamicRoutes`、旧动态过滤入口和本地动态页面 glob 不再存在；无别名、转发、功能开关或双路径 | 精确源码扫描；architecture:check；architecture:test |
| AC-014 | 每个迁移阶段准备结束 | 运行该阶段定向门禁 | 当前阶段全部绿色后才可开始下一阶段；失败不得通过删测试、放宽规则或兼容旁路绕过 | 分阶段 Evidence 与 Git checkpoint |
| AC-015 | 全部迁移完成 | 运行前端全量门禁 | 架构检查/测试、lint、typecheck、工作区测试、开发与生产构建、Admin Playwright 均通过 | 根级质量门禁与 E2E Evidence |

## 5. 范围

### IN

- Vue 全局 `v-hasPermi` 与 `v-hasRoles` 的独立 Web Kit 宿主合同、公开入口、类型和测试。
- Platform Permission 作为唯一跨终端权限算法的复用与回归保护。
- Platform App Runtime 中无 App 单例依赖的菜单规范化、特殊组件/manifest 解析协作和稳定诊断能力。
- Admin 路由守卫、权限求值器装配、动态菜单装配、导航 Store 及其布局/导航消费者的正式入口迁移。
- Admin 动态页面 manifest 注册与现有 App-owned 静态/外部页面边界复核。
- 旧 permission Store、App 私有权限指令、空动态路由、本地动态页面 glob 和对应旧测试假设的零兼容清理。
- 与新边界直接相关的架构规则、包 README、源码地图和测试说明更新。

### REUSE

- 复用 `<Path>plus-ui-namewta/packages/platform/permission/</Path>` 的现有角色、权限、超级管理员、通配符和失败关闭语义。
- 复用 `<Path>plus-ui-namewta/packages/platform/app-runtime/</Path>` 的服务端路由装配与受保护导航恢复能力，不新增大而全 common 包。
- 复用 `<Path>plus-ui-namewta/apps/admin-web/src/router/adminManifestRegistry.ts</Path>` 所体现的 App 显式选择与 manifest registry 模式。
- 复用 `<Path>plus-ui-namewta/packages/web-domains/</Path>` 中现有全局指令使用方式，不要求领域页面改用 Admin 私有 API。
- 复用现有 Vitest、架构检查、TypeScript/Vue 类型检查、双模式构建和 Playwright 接缝。

### OUT

- **OOS-001** 至 **OOS-007** 全部适用。
- 本变更不迁移 Admin 其他全局 directive；只有已由多个 Web Domain 实际依赖的权限指令进入新宿主合同。
- 本变更不改变领域 API、数据模型、HTTP 端口、会话持久化、注册流程或后端返回的 RouterVo 结构。

## 6. 已锁定实现约束

- **DEC-001：** Vue 权限宿主进入独立 Web Kit 包，由 App 注入权限求值器；不得读取 Admin Store。来源：`ADR-001`。
- **DEC-002：** Platform Permission 保持跨终端且无 Vue、DOM、Pinia、Router 或 App 依赖，是权限语义的唯一权威。来源：`ADR-001`。
- **DEC-003：** 只把无单例依赖的服务端菜单确定性流程与诊断下沉到现有 Platform App Runtime；不得建立公共 Store 或公共 Router。来源：`ADR-002`。
- **DEC-004：** Admin 的 permission Store 改名并收口为 navigation Store，继续由 App 拥有侧栏、顶栏、默认路由和动态注册投影。来源：`ADR-002`。
- **DEC-005：** 动态领域页面严格 manifest-only；允许的解析来源只有 App 明确提供的特殊宿主组件、所选 Web Domain manifest 和 App-owned manifest。来源：`ADR-003`。
- **DEC-006：** 当前项目是新基座，迁移后直接删除旧入口，不提供别名、转发门面、功能开关、弃用窗口或双路径。来源：`ADR-003`、`USER-DECISION:基座无需兼容`。
- **DEC-007：** 后端 Client 裁剪结果保持权威，前端不得重新进行跨 Client 菜单授权；认证后顺序保持 `getInfo -> getRouters -> addRoute -> replace`。来源：`USER-DECISION:2026-08-28-final-consensus`。
- **DEC-008：** 缺失身份、畸形输入、未知/未选择组件和权限不匹配必须失败关闭；前端权限可见性不替代后端鉴权。来源：`ADR-001`、`ADR-003`。
- **DEC-009：** 迁移固定分为行为冻结、共享能力、Admin 迁移、零兼容清理、全量验证五阶段，每阶段绿色后才能进入下一阶段。来源：`USER-DECISION:2026-08-28-migration-strategy`。
- **DEC-010：** 工作区内部只从包公开 `exports` 导入；共享包不得导入 `apps/**`、App alias 或其他包的深层入口。来源：永久前端架构合同与 `ADR-001`、`ADR-002`。
- **DEC-011：** `v-hasPermi` 与 `v-hasRoles` 的名称和已冻结行为是 Web Domain 宿主合同；本变更不要求现有领域模板改名。来源：`CODE:<Path>plus-ui-namewta/packages/web-domains/</Path>`、`ADR-001`。
- **DEC-012：** 共享纯流程通过显式输入接收特殊组件解析、manifest resolver 和诊断信息；诊断如何展示由 App 决定。来源：`ADR-002`、`ADR-003`。

## 7. 数据、接口与兼容

- **公共接口变化：** 新增一个仅工作区内部使用的 Web Kit 权限宿主公开入口；Platform App Runtime 的公开入口扩展菜单纯流程与诊断合同；Admin 内部 Store 正式入口从 permission 语义改为 navigation 语义。具体包名和函数名由 Ticket 按现有命名规范确定，但不得改变上述依赖方向与注入合同。
- **数据模型与持久化：** 无变化。不新增数据库、浏览器持久化或后端字段；继续消费现有身份、角色、权限和 RouterVo 数据。
- **Wire/API 合同：** 无变化。不修改 HTTP 路径、请求方法、响应字段、认证 header 或 Client 上下文。
- **兼容要求：** 无。当前为基座且没有需要保护的外部消费者；旧内部入口不设兼容窗口。
- **迁移要求：** 同一交付中完成全部正式消费者迁移后删除旧入口。不得先删除再留下不可构建中间状态；按五阶段形成可验证 checkpoint。
- **回滚要求：** 不建立运行时双轨回滚。若某阶段未通过，停止在该阶段并通过 Git checkpoint 回退该阶段变更；不得继续推进或启用旧兼容入口。
- **发布或运维影响：** 只影响前端工作区构建产物；不要求后端部署、数据库迁移、环境变量或运行时配置变更。
- **人工批准点：** Spec 和 Ticket 不构成实现、提交、集成或清理授权；进入 Implement 仍须遵循 change 状态中的授权边界。

## 8. 非功能要求

- **NFR-001 安全与隐私：** 所有身份、角色、权限、菜单和组件解析异常均失败关闭；后端继续承担最终鉴权。不得新增敏感信息持久化或在诊断中输出 Token、完整会话内容等秘密。
- **NFR-002 性能与容量：** 不新增网络往返，不重复调用 `getInfo` 或 `getRouters`；权限指令保持同步本地判断；菜单转换相对于菜单节点数量保持线性遍历级别，不引入运行时全目录扫描。
- **NFR-003 可用性与可靠性：** 相同输入必须生成确定的权限结果、路由树和诊断；恢复流程不得在路由尚未完整注册时 replace；所有测试需清理 Vue/Pinia/Router 全局状态，能够重复运行。
- **NFR-004 可观测性与运营：** 未知组件键和重复 route name 必须保留可检索的稳定上下文；共享层只产生结构化或可注入诊断，不直接绑定 Admin UI。无需新增遥测平台或后端日志。
- **NFR-005 可维护性：** App、Web Kit、Platform、Web Domain 依赖方向由自动架构检查保护；新能力必须有包级 README、公开 exports 和邻近测试，禁止通过 `utils/common` 杂物包承载。
- **NFR-006 可访问性与视觉：** 不适用。本变更不改变页面布局、交互控件或视觉样式；按钮是否存在仍由既有权限合同决定。

## 9. 验证策略

| 接缝 | 层级 | 覆盖合同 | 现有先例或命令 | Evidence 类型 |
|---|---|---|---|---|
| Platform Permission | 纯单元 | AC-002、AC-003、AC-004、AC-012 | `<Path>plus-ui-namewta/packages/platform/permission/src/index.test.ts</Path>`；包级 `pnpm test` / `pnpm typecheck` | Vitest 输出与退出码 |
| Web Kit 权限宿主 | Vue 指令单元/组件 | AC-001、AC-002、AC-003、AC-012 | 复用 Admin 当前指令行为作为先验，建立包级 Vitest 接缝 | 定向测试输出、依赖图审查 |
| Platform App Runtime 菜单纯流程 | 纯单元、表驱动 | AC-006、AC-007、AC-009、AC-010、AC-012 | `<Path>plus-ui-namewta/packages/platform/app-runtime/src/routeAssembler.test.ts</Path>`、`<Path>plus-ui-namewta/packages/platform/app-runtime/src/navigationRecovery.test.ts</Path>` | Vitest 输出与失败路径断言 |
| Admin manifest registry | App 集成单元 | AC-008、AC-009、AC-010 | `<Path>plus-ui-namewta/apps/admin-web/src/router/adminManifestRegistry.test.ts</Path>`、`<Path>plus-ui-namewta/apps/admin-web/src/router/manifestDiagnostic.test.ts</Path>` | Vitest 输出与诊断断言 |
| Admin navigation Store 与守卫 | App 状态/路由集成 | AC-005、AC-006、AC-008、AC-011、AC-012 | 迁移 `<Path>plus-ui-namewta/apps/admin-web/src/store/modules/permission.test.ts</Path>` 的有效行为；复用 App Runtime 恢复顺序测试 | Vitest、typecheck 与调用顺序断言 |
| 架构与遗留扫描 | 静态架构 | AC-001、AC-004、AC-013、AC-015 | `pnpm architecture:check`；`pnpm architecture:test`；针对旧标识、App 私有指令和动态 glob 的 `rg` 扫描 | 命令、退出码、零匹配或批准清单 |
| 前端工作区门禁 | 静态、单元、构建 | AC-014、AC-015 | `pnpm lint`；`pnpm typecheck`；`pnpm test`；`pnpm build:dev`；`pnpm build:prod` | 各命令 cwd、退出码和摘要 |
| Admin 浏览器流程 | 端到端 | AC-005、AC-006、AC-008、AC-009、AC-011、AC-012、AC-015 | `pnpm test:e2e`，覆盖登录、Client 菜单、动态页面、权限可见性与失败路径 | Playwright 报告、场景结果 |
| 分阶段 Gate | 迁移治理 | AC-014 | 每阶段定向测试与 Git checkpoint；下一阶段开始前复核上一阶段 Evidence | Ticket Evidence 与 checkpoint SHA |

低层单元测试不能替代认证、Client 菜单和动态路由的浏览器级证据。实现阶段必须记录实际命令、工作目录、退出码、未运行项和残余风险，不得仅凭脚本存在报告通过。

## 10. 风险、假设与未决问题

### 风险

- **RISK-001 Manifest 覆盖缺口：** 删除本地 `views` 兜底会暴露未注册的服务端组件键。缓解方式是在清理前盘点当前 Admin 可收到的组件键，验证其属于特殊宿主、所选 Web Domain manifest 或 App-owned manifest，并保留未知键失败测试。
- **RISK-002 指令行为漂移：** 从 Admin 私有实现迁出时可能改变非法绑定、元素移除时机、超级管理员或通配权限语义。缓解方式是先建立特征测试，再迁移实现。
- **RISK-003 Store 改名遗漏：** 布局、守卫、标签页或测试可能仍引用旧 Store。缓解方式是完整调用点扫描、TypeScript 检查、App 测试和零匹配验收。
- **RISK-004 共享类型过度绑定 Vue Router：** 菜单纯流程若直接暴露 App Router 或 Admin Layout 类型，会形成新的伪共享层。缓解方式是依赖注入、公开 exports 审查和架构测试。
- **RISK-005 安全回归：** 在前端二次过滤或异常兜底中可能扩大 Client 菜单或按钮可见范围。缓解方式是锁定服务端权威、失败关闭矩阵与 Admin E2E。
- **RISK-006 零兼容回滚成本：** 删除旧入口后不能运行时切回。该成本由用户明确接受；通过五阶段 checkpoint、每阶段绿色和 Git 回退控制，不引入双轨代码。

### 已采用的低影响假设

- **ASSUMPTION-001：** 独立 Web Kit 包的最终包名和局部函数名按现有 kebab-case 与公开 exports 规范在 Ticket 中确定。验证：package manifest、公开入口、架构测试和全工作区 typecheck。
- **ASSUMPTION-002：** Admin navigation Store 的局部 action/getter 命名可在不改变导航投影行为的前提下遵循现有 Pinia 风格。验证：调用点扫描、Store 测试、typecheck 和 Playwright。
- **ASSUMPTION-003：** 重复 route name 的具体诊断载体由 Admin 保持现有交互风格；共享层只提供稳定诊断数据。验证：App Runtime 单元测试与 Admin 呈现接缝测试。

### 未决问题

无。
