---
name: namewta-fullstack-development
description: 实现跨前后端业务垂直切片时使用，覆盖数据库、API、权限、domain/web-domain、App 组合和交付验证。
---

# NAMEWTA 全栈垂直切片开发

本 Skill 是业务实现的唯一全栈入口。它把一个用户可见能力作为完整切片推进，不按前端或后端拆分任务入口。强制规范、Ratchet、质量门禁与冲突裁决由 [engineering-standards](../engineering-standards/SKILL.md) 负责；模块事实由 [wta-module-guide](../wta-module-guide/SKILL.md) 和 [wta-common-modules-guide](../wta-common-modules-guide/SKILL.md) 负责。

本 Skill 中的跨层合同、分层结构、依赖方向、数据库落位、权限失败关闭和 API 传输约束是硬约束，不能为缩短流程而省略。下面的读取顺序和检查清单是路由建议；只加载命中当前任务的专题，仍须完成命中的硬约束验证。

## 使用顺序

1. 读取最近的 `AGENTS.md`，再读取工程画像、模块地图、决策与例外和后端模块模式登记表。
2. 确定业务 owner、后端 Maven 模块、Controller base path、数据库表/SQL owner、前端 domain/web-domain 资源和目标 App。
3. 建立垂直切片映射：

   ```text
   需求/菜单
     -> 数据库与初始化数据
     -> Controller/API/权限
     -> domain transport/model/service
     -> web-domain 页面/runtime/manifest
     -> App services/路由/权限投影
     -> 测试、构建和交付证据
   ```

4. 先冻结跨层合同，再实现。逐项确认 URL、HTTP method、请求/响应、ID/分页/日期、权限、菜单 componentKey、字典、错误、缓存和副作用。
5. 后端按登记表选择 `layered` 或 `classic`；前端按 `App -> web-domain -> domain -> platform` 实现。新模块与新代码遵循目标规则，存量偏差记录为 legacy 兼容项。
6. 同步更新 Java、SQL、`wta-api`/common SPI、OpenAPI、domain、web-domain、App 组合、菜单 DML、测试和 README/AGENTS 索引；没有消费者的回调或 headless 能力不创建空页面目录。
7. 按 [verification-matrix.md](references/verification-matrix.md) 执行静态检查、单元/契约测试、模块测试、前端门禁、双 bundle 和真实服务验收，并记录命令、退出码、跳过原因和残余风险。

涉及动态菜单、路由或菜单图标时，必须同时读取前端 [permission-routing.md](references/frontend/permission-routing.md)、[contract-mapping.md](references/contract-mapping.md) 与后端 [mapper-and-sql.md](references/backend/mapper-and-sql.md)，把路由投影和图标字符串作为同一条跨层合同验收。

## 按任务读取的专题

- 跨层需求或需要拆分交接时读取[垂直切片设计与交接](references/vertical-slice.md)。
- 修改请求/响应、URL、字段、权限或 OpenAPI 时读取[前后端合同映射](references/contract-mapping.md)。
- 需要决定检查范围或交付证据时读取[验证与交付矩阵](references/verification-matrix.md)。
- 修改前端时按路径读取 `references/frontend/` 中命中的架构、资源、Vue、权限或命名专题；不要读取整个目录。
- 修改后端时按路径读取 `references/backend/` 中命中的模块、CRUD、事务、Mapper、SQL 或公共入口专题；不要读取整个目录。
- 命中 system、workflow、profile、notify 等模块事实时读取 [wta-module-guide](../wta-module-guide/SKILL.md) 对应 index；命中 common SPI、Redis、登录、JSON、Excel、OSS 或通知入口时读取 [wta-common-modules-guide](../wta-common-modules-guide/SKILL.md) 对应 reference。

## 不可突破的跨层边界

- 后端是最终授权者；前端菜单、路由和按钮权限只负责当前 App 的投影与失败关闭。
- domain 不依赖 Vue、DOM、浏览器存储或具体请求库；web-domain 不拥有 App 布局、全局 Router、请求单例或后端授权。
- 跨业务模块只使用 `wta-api` 或明确 common SPI；不得引用其他模块的 Mapper、Entity、BO、VO、Controller 或实现类。
- 新模块使用 `Controller/Listener/API Adapter -> UseCase -> Service -> DAO -> Mapper -> XML`；`wta-system`、`wta-workflow` 等登记为 classic 的存量模块保持兼容，不把 classic 结构复制到新模块。
- CRUD 查询使用 GET，业务变更使用 POST，每个 POST 业务接口有准确、安全的 `@Log`；现有 legacy PUT/DELETE 只在兼容迁移记录中保留。
- 数据库只支持 MySQL 8.4；NAMEWTA DDL 进入 `50-namewta-ddl.sql`，初始化数据、菜单和回填进入 `60-namewta-dml.sql`，不得恢复模块私有 SQL。
- 字典是跨层合同：`dictType + dictValue -> dictLabel`，必须验证真实页面的下拉、单选、表格标签、详情、翻译、Excel 和缓存失效；不能把数字值硬编码成文案。
- 页面、API、权限、菜单、数据库和测试必须由同一个 owner 负责收口，不能只完成单侧代码后宣称切片完成。

## 动态路由与菜单图标合同

- 动态路由唯一恢复顺序为 `getInfo -> getRouters -> navigation Store -> manifest -> addRoute -> replace`；身份、菜单、投影或注册任一步失败时必须失败关闭，不能继续替换到未注册目标。
- 后端按 Client 和权限裁剪菜单，前端只投影当前 App 已显式组合的 domain/web-domain；domain/web-domain 不得直接操作全局 Router，也不得用 catch-all 或点击时注册掩盖缺失的 manifest registration。
- 每个菜单 `componentKey` 必须由目标 App manifest 精确解析；新增资源必须同步 domain resource、web-domain registration、App manifest、菜单 DML 和对应测试。
- 菜单 `icon` 始终是字符串，协议为 `local-name`（本地 SVG）、`tabler:name`（内置离线 Tabler）或 `prefix:name`（外部 Iconify，需在线或额外注册）。空值和 `#` 仅用于无图标功能节点。
- `SvgIcon` 解析优先级为本地 SVG、无前缀 Tabler、显式 `tabler:name`、外部 Iconify；未知或缺失图标必须使用离线 fallback 并在开发环境提供一次性诊断。Element Plus 图标只作为 Vue 组件 API，不写入菜单图标命名空间。
- 后端 `icon` 字段、`sys_menu.icon` 结构和旧无前缀数据保持兼容；NAMEWTA 新菜单优先使用 `tabler:*`，不得为图标新增数据库表或迁移字段。

## 变更前后检查

变更前建立受影响清单：POM/package.json、Java/TypeScript、resources、SQL、`wta-api`、OpenAPI 快照、权限菜单、App 三点组合、测试、部署资产和文档。变更后运行统一校验脚本：

```bash
node .agents/skills/namewta-fullstack-development/scripts/validate-skill.mjs
node .agents/skills/namewta-fullstack-development/scripts/validate-module-mode.mjs <module-path> --mode layered
node docs/fm/scripts/validate.mjs
```

只有当实际变化命中对应范围时，才追加 `pnpm architecture:check`、`pnpm lint`、`pnpm typecheck`、`pnpm test`、`pnpm build:prod`、Maven 测试/打包和外部服务验收。未经授权不得提交、推送、合并、部署、删除来源 worktree 或写入远程系统。
