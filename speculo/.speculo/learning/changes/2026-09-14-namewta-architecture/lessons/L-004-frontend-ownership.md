---
lesson_id: L-004
objective_ids: [OBJ-04]
estimated_minutes: 35
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: deep-explanation
    minutes: 12
  - segment: visuals-and-worked-examples
    minutes: 8
  - segment: conflict-and-pause
    minutes: 6
  - segment: recap-and-transfer
    minutes: 5
expression_level: eli5
coverage_depth: standard
source_ids: [S-001, S-002, S-003, S-004, S-006, S-008, S-009]
---

# Lesson 004：前端不是再复制一份 src/api

## 学完你能做什么

你能说明前端依赖方向：App → web-domain → domain → platform，以及 adapter / web-kit 站在哪。你能指出现在工作树里真实存在的三个 App 包，并且能把“有几个可构建包”和“哪个算产品发布面”分成两句话，不把文档冲突吞掉。

## 先把宏观地图放在桌上

旧前端像一个大书包：页面、接口、类型全塞在同一个 App 的 `src/api`。要做第二个终端，就复印书包。NAMEWTA 把书包拆开：

```text
frontend/
├── apps/
│   ├── admin-web/     ← 管理端终端（工作树：有完整 src 与 build）
│   ├── home-web/      ← 用户门户终端（工作树：有 src 与 build）
│   └── sso-web/       ← 第一方 SSO 认人页（工作树：有 src 与 build）
└── packages/
    ├── web-domains/   ← Vue 页面、manifest（有界面）
    ├── domains/       ← 模型与服务（无 DOM）
    ├── platform/      ← 端口：HTTP、权限、运行时
    ├── adapters/      ← 端口的浏览器实现（axios、存储、加密……）
    ├── web-kit/       ← 多页面共用的壳、指令、token
    └── api-contracts/ ← OpenAPI 生成的 transport（L-005）
```

方向：**App 选择并接线；web-domain 画画；domain 说话；platform 定插头形状；adapter 把插头插进浏览器。**

**类比失效处：** “书包拆开”不是说 App 变空。App 仍拥有 Client、布局、会话名、环境变量、选哪些 domain。失效点：你不能把可复用列表页写回 `apps/admin-web/src/views/system/...` 当长期主人，除非那页明确是该终端私有壳。

## 核心概念与机制

### 直觉讲解

三家餐厅共用中央厨房。菜单照片（web-domain）可以各挂各的墙；菜怎么从后厨点出来（domain）只有一份；插座规格（platform）统一；插头（adapter）按浏览器来做。

新开一家餐厅（新 App）时，不要把中央厨房再盖一遍。选你要的菜（显式依赖哪些 `@namewta/domain-*` / `web-domain-*`），再配自己的厅堂和收银（Client、session、http）。

`admin-web` 的 `application/services.ts` 就是接线板：`createDemoService(domainHttp)`、`createSystemService`、`createNotificationService`……页面不自己 new 一套 URL。

### 精确定义与 English term

| 中文 | English | 精确定义（本课） |
| --- | --- | --- |
| 终端应用 | App | `apps/*`：Client、入口、布局、品牌、会话命名空间、领域选择、部署 |
| 表现域 | web-domain | Vue 页面、领域组件、hooks、语言包、动态路由 manifest；不拥有 App 全局单例 |
| 领域包 | domain | 无 Vue/DOM 的类型、查询/命令服务、transport 映射 |
| 平台端口 | platform port | 跨领域抽象：HTTP、权限、app-runtime 等 |
| 适配器 | adapter | 端口在某运行时的实现，如 axios-browser |
| 组合 | composition | App 显式 import 并创建服务、把 web-domain 注册进运行时 |
| 工作区包 | workspace package | pnpm 包；`apps/*` 与 `packages/*/*` 都是包，App 之间禁止互相 import |

**App → web-domain → domain → platform** 是允许的依赖方向。反向（domain import Vue 页面、App import 另一个 App）是门禁要抓的。

### 机制/因果链

1. 浏览器打开某一个 App 的入口（`admin-web/src/main.ts` 等）。
2. App 创建自己的 HTTP/session/Client。
3. App 用这些端口 `createXxxService`，得到 domain 服务。
4. App 把 web-domain 的 manifest 交给 `platform-app-runtime`，再按后端菜单把组件键解析成真正的页面。
5. 页面（web-domain）只调 domain 服务，例如演示列表页调 `demoService.listDemo`，而 `listDemo` 发 `GET /demo/demo/list`。
6. HTTP 真正出门走 adapter，不是走 Java 类型。

因果：可复用规则留在 domain → 第二个 App 才能选同一份规则、换自己的厅堂。复印 `src/api` 会让 Client 隔离和类型漂移立刻发生。

### 图、表或文本图

**图题 / caption：** 前端所有权与依赖方向；三个 App 包是工作树事实。

```text
 admin-web     home-web      sso-web
  （厅堂）      （厅堂）      （认人厅）
     |             |             |
     +------+------+------+------+
            |  选择并组合
            v
      web-domains/*     web-kit（共用壳/指令）
            |
            v
       domains/*   ----→  api-contracts（transport）
            |
            v
        platform/*  ← 插头形状
            |
            v
        adapters/*  ← 浏览器插头
```

**文字等价物：** 三个终端 App 画在顶上，它们只负责厅堂和选择。向下箭头先到带界面的 web-domain，以及给多个页面用的 web-kit。再往下是没有界面的 domain，它可以映射 OpenAPI transport。domain 依赖 platform 里的抽象端口，端口由 adapters 在浏览器里实现。App 可以依赖 adapter 和 web-kit 来接线，但 App 之间没有箭头。sso-web 画在顶上，是因为工作树里这个包存在；它是不是“产品发布面”要另说一句，见下面冲突。

**图的边界：** 不保证三个 App 都已在生产环境部署。本机未验证部署。图也不表示 sso-web 组合了和 admin-web 一样多的 domain——它的 `package.json` 依赖明显更瘦，主要是认人页。

### 正例、反例与边界

**正例 1：** `admin-web` 依赖 `@namewta/domain-demo` 和 `@namewta/web-domain-demo`，在 `services.ts` 里 `createDemoService`，在 `adminManifestRegistry.ts` 里 `createDemoWebDomain`。

**正例 2：** 演示列表页面在 `packages/web-domains/demo/src/test-demo/DemoPage.vue`，不在 admin-web 的 `views/` 里当可复用资源主人。

**正例 3：** home-web 自己的 `application/services.ts` 再组合一次它需要的 domain，而不是 import admin-web 的文件。

**反例 1：** 在 `admin-web/src/api/demo.ts` 再写一套 axios 封装。这是把中央厨房搬回厅堂。

**反例 2：** `home-web` 用相对路径 `../../admin-web/src/...` 偷组件。App 互引，Client 会话会串。

**反例 3：** domain 里 `import { ref } from 'vue'` 并操作 DOM。domain 不再能给未来非 Vue 终端用。

**边界：** App 私有的壳（登录页品牌、布局、某个终端独有的 404）可以留在 App。判断标准：有没有第二个终端要复用。会复用的，下沉。

## 变式与迁移

- **变式 A：文档说“只有 admin-web 可构建”。** 项目画像（S-003）仍这么写。工作树（S-008）：三个 App 都有 `package.json` 和 `build` 脚本；根 `build:prod` 是 `pnpm -r --if-present build`。本课按工作树：三个包可被 workspace 递归构建。这是冲突 C-002。
- **变式 B：发布面。** 根 README（S-001）把三终端标成已激活。`docs/namewta-enhancements.md` 与 `frontend/apps/README.md`（S-002、S-009）常把发布面写成 admin-web + home-web。`sso-web` 源码与 build **存在**。它等不等于“产品发布面”，本课保持 unresolved（C-003），不要用一句“都激活了”把张力抹平。
- **迁移：** 新增页面：先找对应后端模块名 → `packages/domains/<同名>` → `packages/web-domains/<同名>` → 在目标 App 的组合文件里接线。不要先在 App `views/` 长出可复用 CRUD。

## 常见误区

1. **“工作区里能 build 的 App = 已经对外发布。”** 构建脚本存在只证明包是真的，不证明生产部署。
2. **“sso-web 依赖少，所以不是 App。”** 它仍是 `apps/` 下的终端，只是组合更瘦。
3. **“web-domain 可以保存登录 token 全局单例。”** 会话属于 App。web-domain 不拥有 App 全局单例。
4. **“domain 可以对齐后端模块，所以可以 import Java 名字。”** 对齐的是模块名与 HTTP 资源，传输的是 JSON。

## 非评分暂停

数一数你磁盘上 `frontend/apps/` 下有几个带 `package.json` 的目录。再打开根 README 和 `frontend/apps/README.md`，看它们数出来的是不是同一个数字。如果不是，写下“工作树数字 / 文档数字 / 未决的是哪一句”，不要改成同一个数字假装和平。

## 总结、词汇表与下一步

- 方向：App 组装，web-domain 表现，domain 无界面规则，platform 端口，adapter 实现。
- 工作树：`admin-web`、`home-web`、`sso-web` 三个包都在，且带 build。
- 发布面争议未关闭：sso-web 存在 ≠ 文档已统一称它为发布面。
- 不要复印 `src/api`。

下一步：L-005 看跨过前后端那条河时，什么东西算合同（字段、路径、SQL、OpenAPI），谁先改。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-001 | `README.md` | 三终端表；apps + packages 分层 | 「终端」「架构一览」 | 2026-09-14 |
| S-002 | `docs/namewta-enhancements.md` | 分层职责；发布面写 admin-web + home-web | 「多 App 领域化」 | 2026-09-14 |
| S-003 | `00-project-profile.md` | 写“唯一可构建 App 是 admin-web”（C-002） | 事实来源列表 | 2026-09-14 |
| S-004 | `01-module-map.md` | App → web-domain → domain → platform | 依赖方向 | 2026-09-14 |
| S-006 | `architecture-and-boundaries.md` | ARCH-003 前端边界 | ARCH-003 | 2026-09-14 |
| S-008 | 三个 App 的 `package.json`、根 `build:prod` | 三个可构建包 | 工作树 | 2026-09-14 |
| S-009 | `frontend/apps/README.md` | 只点名 admin-web 与 home-web 可构建部署 | 「当前状态」 | 2026-09-14 |
