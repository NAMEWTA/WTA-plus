# 总体背景与基础知识：NAMEWTA 前后端全模块

## 为什么这个主题重要

WTA-plus 不是「一个 backend 文件夹 + 一个 frontend 文件夹」。它是一座已经分好房间的大楼：

- 改错房间：功能也许能跑，但权限、bundle、动态路由或发布会在别的地方裂开。
- 只学后端或只学前端：一条真实请求会在对面断掉。管理端列表页必须能从 Vue 页面讲到 Mapper XML，失败时能讲到校验、权限或存储拒绝。
- 已有四门课覆盖了地图和三条业务域的后端切片；它们还没把前端 domain / web-domain / App 组合算进同一张覆盖网。

本课的宏观目标：先认整栋楼（Context / Container），再按 **总览 → 平台 → 切片 → 模块 → 业务域** 把公开函数走完，最后能指出主路径和失败路径的存储点与所有权。

## 宏观地图

```text
                         人 / 机器
          ┌───────────────┼────────────────┐
          v               v                v
     管理操作者         门户用户        SSO 客户端 / OpenAPI 机器
          |               |                |
          v               v                v
     admin-web        home-web          sso-web
     (组装全部        (身份+资料         (认人厅：
      业务 web-domain)  子集)             authorize/login)
          \               |                /
           \        App 只组装终端          /
            \       Client / 会话 / 路由    /
             v              v              v
        web-domain (Vue 页面 + manifest)
                      |
                      v
             domain (无 DOM 模型/服务)
                      |
           platform 端口 + adapters
                      |
                 HTTP/JSON
                      v
               backend/wta-admin
          AuthController / CaptchaController
                      |
         +------------+-------------+
         v            v             v
      wta-api     wta-modules    wta-common-*
     跨模块合同     业务房间        可复用工具
         |            |
         v            v
      MySQL 8.4    Redis 8     MinIO/OSS
      10-cde-base-ddl.sql
      50-cde-base-dml.sql
```

登记的业务模式：

- `layered`：`wta-profile`、`wta-notify`、`wta-sso`
- `classic`：`wta-system`、`wta-workflow`、`wta-job`、`wta-demo`、`wta-ai`

前端 domain 与后端模块对齐：`admin`/`system`/`profile`/`notify`/`third`/`workflow`/`demo`/`ai`。

## 核心概念与关系

| 中文 | English | 一句话 |
| --- | --- | --- |
| 单一仓 | monorepo | 前后端合入本仓，不以 submodule 为默认交付 |
| 组装应用 | composition | `wta-admin` 与前端 App 接线，不拥有可复用领域规则 |
| 公开合同 | public contract | HTTP 路径与字段、SQL 基座、OpenAPI transport、`wta-api` |
| 五层模块 | layered | Controller/Listener/API Adapter → UseCase → Service → DAO → Mapper → XML |
| 存量模块 | classic | Controller → Service → ServiceImpl → Mapper → XML |
| 领域包 | domain | 前端无界面模型与服务，对齐后端模块 |
| 表现包 | web-domain | Vue 页面与动态路由 manifest |
| 终端应用 | App | Client、布局、会话命名空间、选择哪些 domain |
| 逻辑子 Change | sub-change | 总览 / 业务域 / 模块 / 切片；四门原料课已在 `children/` |
| 覆盖格子 | coverage cell | `covered` / `deferred(reason)` / `covered-by-parent` / `uncovered` |

关系：

- 前端不 import Java 类型；只消费 HTTP/JSON 和生成的 transport。
- 业务模块跨房间走 `wta-api`，不深用对方 Mapper。
- `wta-common-*` 是工具间。本 Goal 只覆盖主路径上的枢纽（通知分发、MyBatis 基类、Sa-Token 登录、Redis 会话），不为每个 common 叶子开课。
- 现有 Lesson 是原料。standard 地图课和「前端出范围」的域课，在本 Goal 里都要升级后才能把格子标 `covered`。

## 先决知识与缺口

建议先具备：Git 目录、HTTP 请求/响应、任意 MVC 或 Vue 组件项目。

不要求：已经会改 UseCase、已经部署过 Nacos、已经做过 SSO 联调。

缺口（已闭合，2026-09-16T07:53:45.456Z）：四门课已嵌入 `children/`。`/goal` 可升级那些 Lesson；不得未 mine 就标 covered。

新增缺口：旧 Goal-Plan 的 18 波 / 一课一挖已作废。执行按 mine unit（U1–U9，每个 ≤15），teach-then-mine。

## 术语表

| 术语 | 含义 |
| --- | --- |
| bundle-full / bundle-core | `wta-admin` 组装哪些业务模块进 fat jar |
| PKCE S256 | SSO 授权码挑战，防止拦截 code |
| Outbox | 通知投递的领取/唤醒路径，不是「有个消息队列」一句话 |
| transport | `packages/api-contracts` 生成的 HTTP 形状；domain 再映射自有模型 |
| manifest | web-domain 交给 App 的动态页面清单 |
| mine 探针 | `/goal` 审问已写成的 Lesson 与源码，不是学习者问答 |
| mine unit | 一次写齐再挖掘的课包，硬顶 15 节，不是覆盖波 |

## 来源与不确定性

权威来源见 `sources.md`。文档与工作树冲突时以工作树为准，并记下冲突，不默默选一边。
