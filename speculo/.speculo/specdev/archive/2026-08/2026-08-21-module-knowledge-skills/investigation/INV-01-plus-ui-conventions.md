---
artifact: wayfinder-ticket
id: INV-01
name: plus-ui 前端约定与动态权限路由
parent_map: <Path>{roots.state}/specdev/changes/2026-08-21-module-knowledge-skills/wayfinder-map.md</Path>
label: wayfinder:research
status: closed
blocked_by: []
resolution: answered
---

# plus-ui 前端约定与动态权限路由

## 问题

plus-ui-namewta 前端编码、注释与动态权限路由的现行事实与 Skill 切片边界是什么？

## Research: plus-ui-namewta 前端编码约定、注释约定与动态权限路由

- Decision / target: 为后续在 `<Path>.agents/skills/namewta-fullstack-development/</Path>` 编写 Skill 提供一手证据地图；本文件为唯一 owning artifact。
- Scope / version: `<Path>plus-ui-namewta/</Path>` 当前工作树；对照后端菜单/权限接口 `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/</Path>`。前端栈：Vue 3.5.40、TypeScript ^6.0.3、Vite ^8.1.5、Pinia 4.0.2、oxlint ^1.75.0、oxfmt ^0.60.0、pnpm@10.34.5、Node `>=20.19.0`（见 `<Path>plus-ui-namewta/package.json</Path>`）。
- Stop condition: 路由端到端路径完整；每条约定有仓库路径；未知项单列。未创建 Skill、未改 status。

### R-001

- Claim: 实际生效的静态检查是 Oxlint，不是 ESLint。`package.json` scripts：`lint` = `oxlint src`，`lint:fix` = `oxlint --fix src`。无 `eslint`、`prettier`、`stylelint`、`typecheck`、`test`、`format`/`fmt:check` script。
- Type: code fact
- Source: `<Path>plus-ui-namewta/package.json</Path>`（scripts / oxlint / oxfmt / vue-tsc / vitest）
- Confidence: high
- Limits: 未实际运行 `pnpm lint`；只读配置。
- Artifact impact: Skill 的质量门禁应写 `pnpm lint`，不得写成 ESLint。

### R-002

- Claim: `.oxlintrc.json` 启用 plugins `eslint`（Oxlint 的 ESLint 兼容规则集，不是独立 ESLint 配置文件）、`typescript`、`unicorn`、`oxc`、`import`、`vue`。categories：`correctness` 与 `suspicious` 均为 `error`。多条 TypeScript/unicorn/import 规则显式 `off`，包括 `typescript/no-explicit-any`、`typescript/no-unused-vars`。ignorePatterns：`.ai_state`、`.claude`、`.codex`、`doc`、`dist/**`、`dist-ssr/**`、`coverage/**`。
- Type: code fact
- Source: `<Path>plus-ui-namewta/.oxlintrc.json</Path>`
- Confidence: high
- Limits: 未枚举 Oxlint 默认规则全集；`$schema` 指向 oxc 官方 schema。仓库内无名为 `doc/` 的目录（见 R-010）。
- Artifact impact: Skill 必须把“any 被允许”记为当前 ratchet 事实，不能宣称禁止 any。

### R-003

- Claim: 实际生效的格式化是 Oxfmt。`fmt` = `oxfmt .`。`.oxfmtrc.json`：`printWidth` 120、`singleQuote` true、`trailingComma` none、`arrowParens` avoid、`htmlWhitespaceSensitivity` ignore；配置 `sortImports` 分组（type-import → builtin/external → internal → parent/sibling/index）；忽略 `src/types/components.d.ts`、`src/types/auto-imports.d.ts`、`.ai_state`、`.claude`、`.codex`、`doc`。无非写入式 format-check。
- Type: code fact
- Source: `<Path>plus-ui-namewta/.oxfmtrc.json</Path>`；`<Path>plus-ui-namewta/package.json</Path>` scripts.fmt
- Confidence: high
- Limits: 未运行 `pnpm fmt`。写入式格式化 ≠ CI check（与工程规范 PENDING-FE-001 一致，属交叉引用而非本仓库前端源码事实）。
- Artifact impact: Skill 应要求新代码按 oxfmt 选项书写；不要引入 Prettier 配置。

### R-004

- Claim: EditorConfig 为根文件：`[*]` UTF-8、LF、2 空格缩进、trim trailing whitespace、insert final newline；`[*.md]` 关闭末尾空行与尾空白修剪。
- Type: code fact
- Source: `<Path>plus-ui-namewta/.editorconfig</Path>`
- Confidence: high
- Limits: 编辑器是否强制执行取决于本机插件，仓库无 `.vscode/`。
- Artifact impact: Skill 编码约定的缩进/换行以 EditorConfig + oxfmt 为准。

### R-005

- Claim: `tsconfig.json`：`module` ESNext、`moduleResolution` Bundler、`lib` ESNext+DOM、`strict` true、`noEmit` true、`paths` `@/*` → `./src/*`。同时关闭 `noImplicitAny`、`strictFunctionTypes`、`strictNullChecks`；开启 `allowJs`、`removeComments`、`experimentalDecorators`、`forceConsistentCasingInFileNames`。include 含 `src/**/*.ts|vue|d.ts`、`vite.config.ts`、`vitest.config.ts`；exclude `node_modules`、`dist`、`src/**/__tests__/*`。
- Type: code fact
- Source: `<Path>plus-ui-namewta/tsconfig.json</Path>`
- Confidence: high
- Limits: include 列出 `vitest.config.ts`，工作树中未找到该文件（见 Unknowns）。`removeComments` 只影响 emit；因 `noEmit` true，对运行产物无实际注释剥离。
- Artifact impact: Skill 不得把 `strict: true` 解释为严格空值/隐式 any 已开启。

### R-006

- Claim: 工作树中不存在 `.eslintrc*`、`eslint.config.*`、`.prettierrc*`、`prettier.config.*`、`.eslintignore`、`.prettierignore`、`.stylelintrc*`、`.husky/`、lint-staged 配置、`.vscode/`。`package.json` 无 eslint/prettier 依赖。Oxlint 的 `"plugins": ["eslint"]` 不是遗留 ESLint 工程。
- Type: code fact
- Source: 对 `<Path>plus-ui-namewta/</Path>` 的 glob；`<Path>plus-ui-namewta/package.json</Path>`；`<Path>plus-ui-namewta/.oxlintrc.json</Path>`
- Confidence: high
- Limits: `pnpm-lock.yaml` 中 `@transloadit/prettier-bytes` 是传递依赖名，不是 Prettier 工具链。
- Artifact impact: Skill 应写“无 ESLint/Prettier 配置文件”；遗留只剩注释指令（R-007）。

### R-007

- Claim: Prettier/ESLint 遗留仅出现在：(1) 手写 `// prettier-ignore`：`<Path>plus-ui-namewta/src/utils/request.ts</Path>`、`<Path>plus-ui-namewta/src/plugins/tab.ts</Path>`；(2) 生成声明头：`<Path>plus-ui-namewta/src/types/auto-imports.d.ts</Path>` 含 `/* eslint-disable */`、`/* prettier-ignore */`；`<Path>plus-ui-namewta/src/types/components.d.ts</Path>` 含 `/* eslint-disable */`、`/* prettier-ignore */`、`// oxlint-disable`。二者均被 oxfmt ignorePatterns 排除。
- Type: code fact
- Source: 上述三个文件；`<Path>plus-ui-namewta/.oxfmtrc.json</Path>` ignorePatterns
- Confidence: high
- Limits: `prettier-ignore` 对 oxfmt 是否生效未验证。
- Artifact impact: 新代码不要为了“兼容 Prettier”新增 ignore；生成文件不要手改。

### R-008

- Claim: Vite 插件链为 vue / unocss / auto-import / components / compression / svg-icons / setup-extend / check-transition，不含 eslint 或 oxlint 插件。`vue-tsc` 与 `vitest` 在 devDependencies，但无对应 script、无 `vitest.config.ts`、无测试文件信号（tsconfig 仍 exclude `__tests__`）。
- Type: code fact
- Source: `<Path>plus-ui-namewta/vite.config.ts</Path>`；`<Path>plus-ui-namewta/vite/plugins/index.ts</Path>`；`<Path>plus-ui-namewta/package.json</Path>`
- Confidence: high
- Limits: 未扫描全部 `src/**/__tests__` 是否为空目录；`vue-tsc` 可作为补充诊断手动运行。
- Artifact impact: Skill 不要把 typecheck/test 写成已启用门禁。

### R-009

- Claim: 路径别名由 tsconfig `paths` 与 Vite `resolve.tsconfigPaths: true` 共同生效；`unplugin-auto-import` 自动导入 vue / vue-router / @vueuse/core / pinia 及 Element Plus resolver，dts 写入 `src/types/auto-imports.d.ts`。因此 `useStorage` 可在 `<Path>plus-ui-namewta/src/utils/auth.ts</Path>` 无显式 import（与 `<Path>plus-ui-namewta/src/store/modules/app.ts</Path>` 显式 import 并存）。
- Type: code fact
- Source: `<Path>plus-ui-namewta/tsconfig.json</Path>`；`<Path>plus-ui-namewta/vite.config.ts</Path>`；`<Path>plus-ui-namewta/vite/plugins/auto-import.ts</Path>`；`<Path>plus-ui-namewta/src/types/auto-imports.d.ts</Path>`
- Confidence: high
- Limits: 哪些符号被 auto-import 以生成 dts 为准；业务代码两种写法都存在。
- Artifact impact: Skill 编码约定应允许 auto-import，不强制每个文件显式导入 Vue API。

### R-010

- Claim: oxlint/oxfmt 都 ignore `doc`，但 `<Path>plus-ui-namewta/docs/</Path>` 与 `<Path>plus-ui-namewta/doc/</Path>` 均不存在。
- Type: code fact
- Source: glob `plus-ui-namewta/docs/**`、`plus-ui-namewta/doc`；ignorePatterns 见 R-002/R-003
- Confidence: high
- Limits: 可能是上游遗留忽略项。
- Artifact impact: 不要把 `plus-ui-namewta/docs` 写成约定来源。

### R-011

- Claim: `src/` 下不存在 `TODO`/`FIXME`/`HACK`/`XXX` 标记。`待办` 仅出现在 workflow 业务文案（待办任务），不是代码债注释。
- Type: code fact
- Source: 对 `<Path>plus-ui-namewta/src/</Path>` 的 TODO/FIXME 与「待办」检索
- Confidence: high
- Limits: 未覆盖 `.codex/`、`gen/` 以外的全部非 src 文本。
- Artifact impact: Skill 不要发明 TODO/FIXME 规范；仓库当前几乎不使用这类标记。

### R-012

- Claim: 标准 API 文件使用块级 JSDoc：一行中文摘要 + `@param` + 偶发 `@returns {*}`。该模式同时出现在手写 demo API 与生成模板，是可复现约定，不是全仓强制。
- Type: code fact
- Source: `<Path>plus-ui-namewta/src/api/demo/demo/index.ts</Path>`；`<Path>plus-ui-namewta/gen/api.ts.ftl</Path>`
- Confidence: high
- Limits: 部分 API（如 `<Path>plus-ui-namewta/src/api/menu.ts</Path>`、`<Path>plus-ui-namewta/src/api/login.ts</Path>` 的 `getInfo`）只有单行 `//` 或无完整 JSDoc。
- Artifact impact: 新标准 CRUD API 可跟随 gen/demo；不要要求每个函数都有完整 JSDoc。

### R-013

- Claim: Vue SFC `<script setup>` 中，页面操作函数普遍使用单行 `/** 查询xx列表 */`、`/** 新增按钮操作 */` 等中文 JSDoc，无 `@param`。demo、post、operlog 等页面一致。
- Type: code fact
- Source: `<Path>plus-ui-namewta/src/views/demo/demo/index.vue</Path>`；`<Path>plus-ui-namewta/src/views/system/post/index.vue</Path>`；`<Path>plus-ui-namewta/src/views/monitor/operlog/index.vue</Path>`；`<Path>plus-ui-namewta/gen/index.vue.ftl</Path>`
- Confidence: high
- Limits: 这是主导实践，不是 linter 强制。复杂页会夹杂 `//` 与少量块注释。
- Artifact impact: Skill 注释约定应描述“页面 handler 单行中文 JSDoc”，不要写成 Google/TSDoc 完整风格指南。

### R-014

- Claim: 模板 HTML 注释用于分区：`<!-- 部门树 -->`、`<!-- 添加或修改…对话框 -->`。用户页有一条功能性注释：el-dropdown-item 延迟加载导致权限指令不生效，需 `v-if="checkPermi([...])"`。
- Type: code fact
- Source: `<Path>plus-ui-namewta/src/views/system/user/index.vue</Path>`（约部门树/对话框/dropdown 注意）；`<Path>plus-ui-namewta/src/views/demo/demo/index.vue</Path>`；`<Path>plus-ui-namewta/src/views/system/post/index.vue</Path>`
- Confidence: high
- Limits: 未统计全部 Vue 文件的注释密度。
- Artifact impact: Skill 应保留“dropdown 不用指令、改用 checkPermi”这一真实例外。

### R-015

- Claim: 绝大多数 TS/Vue 文件没有版权头/`@author` 文件头。例外：`<Path>plus-ui-namewta/src/directive/common/copyText.ts</Path>` 有 `v-copyText` + `Copyright (c) 2022 ruoyi`。登录/注册页 Copyright 是 UI 文案，不是源码头。
- Type: code fact
- Source: 对 Copyright/@author 的检索；`<Path>plus-ui-namewta/src/directive/common/copyText.ts</Path>`；`<Path>plus-ui-namewta/src/views/login.vue</Path>`
- Confidence: high
- Limits: 未逐文件审计全部 src。
- Artifact impact: Skill 不要要求新文件加版权头。

### R-016

- Claim: 仓库没有独立“注释规范”文档。可观察约定仅来自真实代码与 gen 模板（R-012–R-015）。`<Path>plus-ui-namewta/src/router/index.ts</Path>` 顶部有一段路由配置项说明块注释（hidden/alwaysShow/roles/permissions/meta），属于框架自述，不是全仓注释风格。
- Type: code fact
- Source: `<Path>plus-ui-namewta/src/router/index.ts</Path>` 文件头；无 `CONTRIBUTING*`/`STYLE*`；无 `plus-ui-namewta/docs/`
- Confidence: high
- Limits: `.codex/skills` 描述的是 CRUD 编码，几乎不谈注释（见 R-040）。
- Artifact impact: 后续 Skill 必须标明“观察性约定”，禁止发明未落地的注释规范。

### R-017

- Claim: 应用启动在 `<Path>plus-ui-namewta/src/main.ts</Path>` 中 `import './permission'`（副作用注册全局前置守卫），并 `app.use(router)`、`directive(app)`、`app.use(plugins)`（挂 `$auth`）。
- Type: code fact
- Source: `<Path>plus-ui-namewta/src/main.ts</Path>`；`<Path>plus-ui-namewta/src/directive/index.ts</Path>`；`<Path>plus-ui-namewta/src/plugins/index.ts</Path>`
- Confidence: high
- Limits: 无。
- Artifact impact: Skill 路由章节从 main.ts 副作用入口写起。

### R-018

- Claim: 全局守卫逻辑：有 Token 且访问 `/login` → `{ path: '/' }`；有 Token 且白名单 → 放行；有 Token 且 `userStore.roles.length === 0` → `getInfo()` 成功后 `generateRoutes()` 并对非 http path `router.addRoute`，再 replace 回当前 to；`getInfo` 失败则 `logout` 并 `{ path: '/' }`。无 Token：白名单放行，否则 `/login?redirect=${encodeURIComponent(to.fullPath || '/')}`。白名单：`/login`、`/register`、`/social-callback`、`/register*`、`/register/*`。匹配器为 `isPathMatch`。
- Type: code fact
- Source: `<Path>plus-ui-namewta/src/permission.ts</Path>`；`<Path>plus-ui-namewta/src/utils/validate.ts</Path>` `isPathMatch`/`isHttp`
- Confidence: high
- Limits: `getInfo` 失败返回 `/` 而非带 redirect 的登录页，依赖 logout 清 Token 后下一次导航再进登录（见 Unknowns）。
- Artifact impact: Skill 必须原样描述该分支，不要简化成“无 Token 一律登录”。

### R-019

- Claim: Token 键为 `Admin-Token`，经 VueUse `useStorage` 持久化。登录成功写 `access_token`；`getInfo` 填充 `roles`/`permissions`，若 roles 为空数组则 roles 设为 `['ROLE_DEFAULT']`（permissions 不在该分支赋值）。logout 调后端后清空 token/roles/permissions。
- Type: code fact
- Source: `<Path>plus-ui-namewta/src/utils/auth.ts</Path>`；`<Path>plus-ui-namewta/src/store/modules/user.ts</Path>`
- Confidence: high
- Limits: `useStorage` 默认 localStorage（VueUse 默认）；未再读 VueUse 实现确认 storage 类型。
- Artifact impact: Skill 应说明“roles 长度为 0 是是否已拉取用户信息的哨兵”。

### R-020

- Claim: 用户信息接口 `GET /system/user/getInfo`，前端类型 `UserInfo { user, roles, permissions }`。后端 `SysUserController.getInfo` 返回 `UserInfoVo`：`user` + `permissions=loginUser.menuPermission` + `roles=loginUser.rolePermission`。超管菜单权限含 `*:*:*`，角色含 `superadmin`（`SystemConstants.SUPER_ADMIN_ROLE_KEY`）。
- Type: code fact
- Source: `<Path>plus-ui-namewta/src/api/login.ts</Path>`；`<Path>plus-ui-namewta/src/api/system/user/types.ts</Path>`；`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysUserController.java</Path>`；`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/domain/vo/UserInfoVo.java</Path>`；`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysPermissionServiceImpl.java</Path>`；`<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-core/src/main/java/org/dromara/common/core/constant/SystemConstants.java</Path>`
- Confidence: high
- Limits: 未在运行时抓包核对 JSON 字段名（Jackson 默认与 Java 字段一致）。
- Artifact impact: 按钮权限通配符是 `*:*:*`；超管角色键是 `superadmin` 不是 `admin`（与 R-035 冲突相关）。

### R-021

- Claim: 公共路由 `constantRoutes` 含：`/redirect`、`/social-callback`、`/login`、`/register`、catch-all `/:pathMatch(.*)*` → 404、`/401`、`''` 重定向 `/index`（首页）、隐藏的 `/user/profile`。全部 `hidden: true` 的登录/错误页不进侧栏。history 基路径 `import.meta.env.VITE_APP_CONTEXT_PATH`。
- Type: code fact
- Source: `<Path>plus-ui-namewta/src/router/index.ts</Path>`
- Confidence: high
- Limits: catch-all 写在 constantRoutes 靠前位置；Vue Router 4 按评分匹配，动态 `addRoute` 后未匹配路径仍应落到 404。未做运行时验证。
- Artifact impact: Skill 应把 404/401 列为 constant 隐藏路由，而不是动态生成。

### R-022

- Claim: 前端本地 `dynamicRoutes` 当前是空数组 `[]`。`generateRoutes` 仍会 `filterDynamicRoutes(dynamicRoutes)` 并对结果 `router.addRoute`，但空数组导致该路径为空操作。
- Type: code fact
- Source: `<Path>plus-ui-namewta/src/router/index.ts</Path>` `export const dynamicRoutes`；`<Path>plus-ui-namewta/src/store/modules/permission.ts</Path>` `generateRoutes`
- Confidence: high
- Limits: 上游 RuoYi 历史上常用 dynamicRoutes 挂隐藏页（如授权角色）。本仓库已清空；是否刻意为之未在前端注释说明。
- Artifact impact: Skill 应写“本地 permissions/roles 动态路由机制仍在，但当前无条目；侧栏路由来自后端 getRouters”。

### R-023

- Claim: 后端菜单路由：`getRouters()` → `GET /system/menu/getRouters`。后端要求 `LoginUser.clientPk` 非空，否则失败「当前登录缺少客户端上下文」；然后 `selectMenuTreeByUserId(userId, clientPk)` + `buildMenus`。超管按当前 Client 查全部菜单，非超管合并用户菜单与默认角色菜单。前端不再做跨 Client 过滤。
- Type: code fact
- Source: `<Path>plus-ui-namewta/src/api/menu.ts</Path>`；`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysMenuController.java</Path>` `getRouters`；`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysMenuServiceImpl.java</Path>` `selectMenuTreeByUserId`；`<Path>docs/upstream/customization-map.md</Path>` 前端「动态路由」行
- Confidence: high
- Limits: SQL/mapper 细节未展开（非本问题核心）。
- Artifact impact: Skill 必须强调 NAMEWTA：菜单已在服务端按 Client 过滤，前端只 `addRoute`。

### R-024

- Claim: `RouterVo` JSON 字段：name、path、hidden、redirect、component、query、ext、alwaysShow、meta、children。`MetaVo`：title、icon、noCache、link、activeMenu。`hidden` 来自菜单 `visible == "1"`。目录类型设 `alwaysShow=true` 且 `redirect=noRedirect`。路由 name 规则注释为 path 首字母大写 + menuId（实现为 `menu.getRouteName() + menu.getMenuId()`）。**RouterVo 不含 permissions/roles 字段**——后端已按用户裁剪菜单树。
- Type: code fact
- Source: `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/domain/vo/RouterVo.java</Path>`；`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/domain/vo/MetaVo.java</Path>`；`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysMenuServiceImpl.java</Path>` `buildMenus`
- Confidence: high
- Limits: `visible=="1"` 表示隐藏，与字段名直觉相反，以代码为准。
- Artifact impact: Skill 须区分：后端路由对象无 permissions；前端 `_RouteRecordBase.permissions/roles` 只服务本地 dynamicRoutes。

### R-025

- Claim: `filterAsyncRouter` 把 component 字符串映射为：`Layout` / `ParentView` / `InnerLink` 组件，其余走 `loadView(view, name)`。`loadView` 用 `import.meta.glob('../../views/**/*.vue')` 预建 Map，再 `createCustomNameComponent` 包装以固定组件 name（keep-alive）。rewrite 模式会 `filterChildren` 展平 ParentView 子路径。http(s) 外链在守卫里不 `addRoute`。
- Type: code fact
- Source: `<Path>plus-ui-namewta/src/store/modules/permission.ts</Path>`；`<Path>plus-ui-namewta/src/utils/createCustomNameComponent.tsx</Path>`；`<Path>plus-ui-namewta/src/permission.ts</Path>` `isHttp`
- Confidence: high
- Limits: 若后端 component 路径在 views 中找不到，`loadView` 返回 `undefined`（无用户提示，仅 createCustomNameComponent 在 load 失败时 console.error）。
- Artifact impact: 新页面必须落在 `src/views/**/*.vue` 且菜单 component 与相对路径一致（不含 `views/` 前缀与 `.vue`）。

### R-026

- Claim: `filterDynamicRoutes`：路由有 `permissions` 则 `auth.hasPermiOr`；否则若有 `roles` 则 `auth.hasRoleOr`；两者都没有则丢弃。`permissions`/`roles` 定义在 `_RouteRecordBase`，不在 `RouteMeta`。
- Type: code fact
- Source: `<Path>plus-ui-namewta/src/store/modules/permission.ts</Path>` `filterDynamicRoutes`；`<Path>plus-ui-namewta/src/types/router.d.ts</Path>`；`<Path>plus-ui-namewta/src/plugins/auth.ts</Path>`
- Confidence: high
- Limits: 因 dynamicRoutes 为空，该过滤器当前无运行效果。
- Artifact impact: Skill 把这套机制标为“本地隐藏路由扩展点”，不要说后端菜单靠它过滤。

### R-027

- Claim: 指令注册名为 `hasPermi` 与 `hasRoles`（复数）。模板主流写法 `v-hasPermi="['module:res:op']"`；`<Path>plus-ui-namewta/src/views/system/user/index.vue</Path>` 混用 `v-has-permi`。Vue 将 camel/kebab 视为同一指令。无任何模板使用 `v-hasRole`/`v-hasRoles`（仅 error 文案举例）。指令在 `mounted` 时无权限则 `removeChild`；值必须是非空数组，否则 throw。`*:*:*` 视为全部权限。`hasRoles` 将 `superadmin` **或** `admin` 视为放行。
- Type: code fact
- Source: `<Path>plus-ui-namewta/src/directive/index.ts</Path>`；`<Path>plus-ui-namewta/src/directive/permission/index.ts</Path>`；`<Path>plus-ui-namewta/src/views/demo/demo/index.vue</Path>`；`<Path>plus-ui-namewta/src/views/system/user/index.vue</Path>`；全仓 `v-hasRoles`/`v-hasRole` 检索仅命中 directive 源文件
- Confidence: high
- Limits: 指令只控制呈现，不替代后端鉴权（工程规范 VUE-004 亦如此陈述）。
- Artifact impact: Skill 应写真实指令名 `v-hasPermi` / `v-hasRoles`；注明 user 页 kebab 混用；不要把用户问题里的 `v-hasRole` 当成仓库 API。

### R-028

- Claim: 编程式校验有两套：`$auth`/`plugins/auth.ts`（`hasPermi/Or/And`、`hasRole/Or/And`），角色超管键为 **`admin`**；`utils/permission.ts` 的 `checkPermi`/`checkRole`，角色超管键亦为 **`admin`**。`checkPermi` 实际用于 user 页 dropdown。全仓未检索到 `$auth.` 或 `checkRole(` 的业务调用。
- Type: code fact
- Source: `<Path>plus-ui-namewta/src/plugins/auth.ts</Path>`；`<Path>plus-ui-namewta/src/utils/permission.ts</Path>`；`<Path>plus-ui-namewta/src/views/system/user/index.vue</Path>` import checkPermi
- Confidence: high
- Limits: `$auth` 经 globalProperties 暴露，模板字符串用法可能漏检；未搜 `app.config.globalProperties.$auth` 之外的间接调用。
- Artifact impact: Skill 应并列三套校验（指令 / auth 插件 / checkPermi），并标出 admin vs superadmin 不一致。

### R-029

- Claim: 侧栏与顶栏用 `hidden` 过滤：`SidebarItem` 根节点 `v-if="!item.hidden"`，子节点 `item.hidden` 不计入 showingChildren；TopBar 对 `sidebarRouters` `filter(f => !f.hidden)`。`meta.noCache` 为 true 时 tagsView 不写入 `cachedViews`；`AppMain` 用 `<keep-alive :include="cachedViews">`。`meta.activeMenu` 用于侧栏高亮。
- Type: code fact
- Source: `<Path>plus-ui-namewta/src/layout/components/Sidebar/SidebarItem.vue</Path>`；`<Path>plus-ui-namewta/src/layout/components/TopBar/index.vue</Path>`；`<Path>plus-ui-namewta/src/store/modules/tagsView.ts</Path>`；`<Path>plus-ui-namewta/src/layout/components/AppMain.vue</Path>`
- Confidence: high
- Limits: `hidden` 类型声明为 `boolean | string | number`，真值判断依赖 JS truthiness。
- Artifact impact: Skill 的 route meta 表应覆盖 hidden/alwaysShow/noCache/title/icon/affix/activeMenu/link/permissions/roles。

### R-030

- Claim: 登录成功后 `router.push(redirectUrl)`，`redirect` 来自 query.redirect 的 `decodeURIComponent`。HTTP 业务码 401 时弹窗「重新登录」，logout 后 `router.replace({ path: '/login', query: { redirect: encodeURIComponent(current fullPath) } })`。请求头带 `Authorization: Bearer` 与 `clientid: VITE_APP_CLIENT_ID`。
- Type: code fact
- Source: `<Path>plus-ui-namewta/src/views/login.vue</Path>`；`<Path>plus-ui-namewta/src/utils/request.ts</Path>` 拦截器与 `globalHeaders`
- Confidence: high
- Limits: 守卫已 `encodeURIComponent`，login 再 decode；401 路径把已编码字符串放进 query 对象，Vue Router 可能二次编码（见 Unknowns）。
- Artifact impact: Skill 描述“无 Token / 401 → 登录页 + redirect”，细节实现以源码为准，不要承诺双重编解码一定正确。

### R-031

- Claim: 社交回调 `/social-callback` 在白名单且 constantRoutes。无 Token 走 `loginByCode`，有 Token 走 `callbackByCode`；成功 `setToken` 后 `location.href = VITE_APP_CONTEXT_PATH + 'index'`（整页跳转，不是 router.push）。
- Type: code fact
- Source: `<Path>plus-ui-namewta/src/layout/components/SocialCallback/index.vue</Path>`；`<Path>plus-ui-namewta/src/permission.ts</Path>` whiteList
- Confidence: medium
- Limits: 未跑 OAuth 流程。
- Artifact impact: Skill 白名单需包含 social-callback；不要把它当成普通 Vue 内导航。

### R-032

- Claim: `plus-ui-namewta/docs` 不存在。父仓库确认约定的文档是 `<Path>docs/upstream/customization-map.md</Path>`：动态路由「后端已按 Client 过滤；前端继续 addRoute，不再做跨 Client 菜单过滤」；多 APP 用 `VITE_APP_CLIENT_ID`。
- Type: code fact
- Source: glob `plus-ui-namewta/docs` 空；`<Path>docs/upstream/customization-map.md</Path>` 前端表
- Confidence: high
- Limits: 父仓库 `docs/` 目前仅此一篇。
- Artifact impact: Skill 权限章节应链接 customization-map，而不是复制长文。

### R-033

- Claim: `.agents/skills/engineering-standards` 已记录 oxlint/oxfmt/tsconfig ratchet、`pnpm lint`/`pnpm fmt`/`pnpm build:prod` 门禁、`v-hasPermi` 只控制呈现、CRUD 页用 `v-hasPermi`。这是规范裁决层，不是 plus-ui 源码。
- Type: code fact
- Source: `<Path>.agents/skills/engineering-standards/references/project/00-project-profile.md</Path>`；`<Path>.agents/skills/engineering-standards/references/typescript/core.md</Path>` TS-001；`<Path>.agents/skills/engineering-standards/references/typescript/frameworks/vue.md</Path>` VUE-004；`<Path>.agents/skills/engineering-standards/references/typescript/crud-api-and-pages.md</Path>` FE-CRUD-007；`<Path>.agents/skills/engineering-standards/references/project/02-decisions-and-exceptions.md</Path>` PENDING-FE-001
- Confidence: high
- Limits: 规范层可能滞后于代码；本调研以代码为准并已交叉核对，结论一致。
- Artifact impact: 新 Skill 不要复制工程规范正文；只做模块知识并链接。

### R-034

- Claim: 子模块内另有 CRUD 编码 Skill：`<Path>plus-ui-namewta/.codex/skills/frontend-crud-coding/SKILL.md</Path>` 与 `<Path>plus-ui-namewta/.codex/skills/frontend-crud-coding/references/frontend.md</Path>`。它确认无 `.prettierrc`、用 `pnpm fmt`/`pnpm lint`、EditorConfig 2 空格、新代码默认 `v-hasPermi`、已混用则保持文件现状、dropdown 用 `checkPermi`。几乎不覆盖动态路由与注释。
- Type: code fact
- Source: 上述两个文件
- Confidence: high
- Limits: 该 Skill 面向 CRUD 生成，不是本 change 要创建的 namewta-fullstack-development。
- Artifact impact: 新 Skill 可引用其 CRUD 细节，但权限路由必须以本调研的源码路径为准。

### R-035

- Claim: 角色超管标识前后端/前端内部不一致：后端与 `hasRoles` 指令认 `superadmin`；`plugins/auth.ts` 与 `checkRole` 认 `admin`。前端 `hasRoles` 额外把 `admin` 也当超管。NAMEWTA 后端 `SUPER_ADMIN_ROLE_KEY = "superadmin"`。
- Type: code fact（不一致本身是事实）；inference（`checkRole`/`hasRole` 对超管可能失效，因 getInfo 不会返回 `admin` 键，除非某角色编码恰好是 admin）
- Source: `<Path>plus-ui-namewta/src/directive/permission/index.ts</Path>`；`<Path>plus-ui-namewta/src/plugins/auth.ts</Path>`；`<Path>plus-ui-namewta/src/utils/permission.ts</Path>`；`<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-core/src/main/java/org/dromara/common/core/constant/SystemConstants.java</Path>`
- Confidence: high（不一致）；medium（运行时影响，因 `v-hasRoles`/`checkRole` 业务调用极少）
- Limits: 未登录超管账号验证 roles 数组实际内容。
- Artifact impact: Skill 必须把该冲突列为已知陷阱，不要静默“统一成 admin”。

### Conflicts and Unknowns

**Conflicts（源码并列，不裁决）**

1. 超管角色键：`superadmin`（后端 + hasRoles 指令）vs `admin`（auth.ts / checkRole）。见 R-035。
2. 指令示例文案使用 kebab `v-has-permi` / `v-has-roles`，页面主流 camel `v-hasPermi`；Vue 两者等价。
3. `RouterVo` 无 permissions/roles，但前端 router 注释与 `_RouteRecordBase` 仍描述这两字段（仅本地 dynamicRoutes 使用，且当前数组为空）。

**Unknowns（需以后重读代码或运行时验证）**

1. `getInfo` 失败后 `return { path: '/' }` 在 logout 抛错时的导航结果。路径：`<Path>plus-ui-namewta/src/permission.ts</Path>`。
2. `login?redirect=` 与 401 `query.redirect` 是否双重编码。路径：`<Path>plus-ui-namewta/src/permission.ts</Path>`、`<Path>plus-ui-namewta/src/views/login.vue</Path>`、`<Path>plus-ui-namewta/src/utils/request.ts</Path>`。
3. catch-all 404 注册顺序与动态 addRoute 的运行时匹配，未做浏览器验证。路径：`<Path>plus-ui-namewta/src/router/index.ts</Path>`。
4. `dynamicRoutes = []` 是 NAMEWTA 有意清空还是上游已改，前端无注释。若需历史意图，读子模块 git log（本次未执行）。
5. `tsconfig.json` include `vitest.config.ts` 但文件不存在；Vitest 依赖的启用计划未知。
6. oxfmt 是否尊重 `// prettier-ignore`。
7. `loadView` 找不到组件时用户看到什么（空白/404）。路径：`<Path>plus-ui-namewta/src/store/modules/permission.ts</Path>` `loadView`。
8. `$auth` 是否在 Options API 模板中使用（本次只搜脚本）。
9. Token storage 引擎（localStorage vs sessionStorage）未打开 VueUse `useStorage` 默认实现再确认。

### Recommendation

后续 Skill `<Path>.agents/skills/namewta-fullstack-development/</Path>` 建议三份 references，且全部声明“描述不清则按文中路径读源码，禁止发明规范”：

1. **coding-style**：EditorConfig + oxlint/oxfmt/tsconfig/package.json scripts；明确无 ESLint/Prettier 工程；门禁 `pnpm lint`、写入格式化 `pnpm fmt`、构建 `pnpm build:prod`；typecheck/test 不是 active gate。链接 engineering-standards，不复制 MUST 条文。
2. **comments**：只记录观察性实践（API 块 JSDoc、页面单行 `/** 操作名 */`、模板分区 HTML 注释、无版权头、无 TODO 文化、user 页 dropdown 那条注意）。不要写虚构 style guide。
3. **permission-routing**：按本文件 R-017–R-031 画端到端图：main.ts → permission 守卫 → getInfo → getRouters(Client) → filterAsyncRouter/loadView/addRoute → sidebar hidden → 指令/checkPermi；标注 dynamicRoutes 为空、admin/superadmin 冲突、指令名 `hasRoles` 而非 `hasRole`。

本 Skill（research）未写入任何共享 namespace，仅本 investigation 文件。
