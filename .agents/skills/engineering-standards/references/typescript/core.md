# TypeScript 工程规范

适用 `module:plus-ui-namewta`。当前使用 TypeScript 6、ESM、pnpm 10 workspace 与 Vite 8；根配置由各 App/包继承或组合，各包拥有自己的源码范围和边界，不存在根级产品 `src` 单体别名合同。

### TS-001 类型严格度 Ratchet

Scope: `language:typescript`, `module:plus-ui-namewta`

Level: MUST

Source: `repository-fact` (`tsconfig.json`, `.oxlintrc.json`) + `builder-baseline`

Rule: 不为局部问题继续放宽全局 compiler/lint。新建或实质修改的 API、env、storage、DOM dataset、JSON 和第三方 response 边界从 `unknown` 验证/缩窄；`any` 只留在有说明的兼容 adapter，不能扩散到业务合同。

Verification: `pnpm lint`; `pnpm build:prod`; review 变更中的 `any`、双重断言、非空断言和宽泛索引签名；遵循 EX-001。

### TS-002 API 类型与 transport 分离

Scope: `path:plus-ui-namewta/packages/api-contracts/**`, `path:plus-ui-namewta/packages/domains/**`, `path:plus-ui-namewta/packages/platform/http/**`, `path:plus-ui-namewta/packages/adapters/**`

Level: MUST

Source: `repository-fact`

Rule: API request/response transport 与后端 BO/VO JSON 合同一致，分页、ID、boolean 和 nullable 语义明确；OpenAPI 生成类型在 domain 边界映射为 domain-owned model。domain 只依赖平台 HTTP 合同，不泄漏 Axios/Taro 类型；页面不重复定义 transport shape。

Verification: 对照后端 controller/BO/VO；`pnpm lint`; `pnpm build:prod`; 跨端契约 review。

### TS-003 模块与 import

Scope: `module:plus-ui-namewta`

Level: SHOULD

Source: `repository-fact` + `builder-baseline`

Rule: 使用 ESM，类型依赖使用 `import type`；工作区内部从 package exports 导入，包内别名不能绕过所有权。`index.ts` 只作为稳定公开入口，不机械创建 barrel；顶层副作用限定在 App 启动与明确插件注册入口，domain/web-domain 不自动注册自身。

Verification: `pnpm architecture:check`; `pnpm lint`; `pnpm typecheck`; `pnpm build:prod`; review 深层导入、循环依赖、运行时 type import 和隐式初始化顺序。

### TS-004 Promise、错误与取消

Scope: `language:typescript`

Level: MUST

Source: `repository-fact` + `builder-baseline`

Rule: Promise 必须 await、return、批量等待或由有监督 owner 接管；catch 值按 `unknown` 处理。网络/搜索/组件异步任务定义过期响应和卸载行为；并发批处理说明上限、顺序和部分失败。

Verification: review floating Promise、catch 和 lifecycle；失败/超时/重复响应测试或人工步骤；`pnpm lint`。

### TS-005 公共类型表达真实语义

Scope: `language:typescript`

Level: SHOULD

Source: `repository-fact` + `builder-baseline`

Rule: 沿用 scope 内 `interface`/`type` 主导实践，不把风格偏好升为硬规则。用字面量联合、可选字段和领域类型表达协议；不要用 magic number/string、`Record<string, any>` 或假枚举抹去已知字段。

Verification: public type review；后端 JSON 对照；构建与相关行为验证。
