# Vue 3 / Pinia 工程规范

适用 `framework:vue`, `module:plus-ui-namewta`。当前使用 Vue 3.5.40、`@vue/compiler-sfc` 3.5.40、Composition API、`<script setup lang="ts">`、Pinia 4、Element Plus 与 Vite，无 SSR 信号。

### VUE-001 SFC 与组件合同

Scope: `framework:vue`

Level: MUST

Source: `repository-fact` + `official-guidance`

Rule: 新组件沿用 `<script setup lang="ts">` 与 Composition API；props、emits、slots 和 model 是公开合同，payload 精确类型化，组件不修改 prop 或不拥有的对象。稳定存量组件不为语法统一而重写。

Verification: `pnpm lint`; `pnpm build:prod`; review `defineProps`/`defineEmits`/model 类型、父子所有权和模板调用。

### VUE-002 响应式状态与副作用

Scope: `framework:vue`

Level: MUST

Source: `official-guidance` + `builder-baseline`

Rule: 派生值使用纯 `computed`；watch/watchEffect 只承担明确副作用并处理 cleanup/过期异步结果。timer、listener、observer、subscription 和 request owner 在 unmount/scope dispose 释放，不靠任意 timeout 等 DOM 更新。

Verification: component/composable 行为测试或人工 mount/unmount；review watcher、cleanup、`nextTick`/flush timing；构建。

### VUE-003 Composable 与 Pinia 所有权

Scope: `framework:vue`, `framework:pinia`

Level: SHOULD

Source: `repository-fact` + `official-guidance`

Rule: 可复用有状态逻辑使用有明确合同的 `useXxx` composable；纯函数不滥用 `use`。Pinia 只承载跨组件/页面生命周期状态，getter 保持派生，action 表达业务操作；请求临时状态、表单草稿不因可能复用提前全局化。

Verification: review state owner、store 写入入口和 reset 行为；相关 store/composable 测试；`pnpm build:prod`。

### VUE-004 Template、安全与可访问性

Scope: `framework:vue`

Level: MUST

Source: `official-guidance` + `builder-baseline`

Rule: `v-for` 使用稳定业务 key；避免同元素混用 `v-if`/`v-for`；`v-html` 仅接收可信或已消毒内容。交互控件具有 label/可访问名称、键盘和焦点行为；权限指令只控制呈现，不能替代后端鉴权。

Verification: DOM/交互测试或人工键盘/焦点检查；review `v-html`、URL、style 和 `v-hasPermi`; 安全负向测试。

### VUE-005 大页面按职责 Ratchet

Scope: `path:plus-ui-namewta/packages/web-domains/**`, `path:plus-ui-namewta/apps/**`

Level: SHOULD

Source: `repository-fact` (multiple 500-1200 line SFCs) + `builder-baseline`

Rule: 新功能不把查询、表格、表单、权限、映射和宿主副作用无界堆入单个 SFC。领域模型/映射下沉 domain，页面复用逻辑留在所属 web-domain composable，下载/导航/反馈走宿主端口；只有形成真实多消费者合同才提取到 web-kit。不为行数创建无语义组件。

Verification: review SFC 职责和依赖数；提取逻辑有测试/调用证据；UI 截图与交互回归。
