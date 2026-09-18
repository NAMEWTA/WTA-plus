# 课程设计：wta-sso 第一方 SSO 切片

## 目标与期望效果

学完后能对着 `backend/wta-modules/wta-sso/src/main/java` 口述每条公开 HTTP 切片的真实调用链：哪个 Controller 方法进、哪个 UseCase 方法接、Service / Port / Adapter / DAO / Mapper 各做什么。不把地图课 L-001～L-006 的“房间”知识当成已经会改授权码。

## 学习者与表达/深度配置

| 字段 | 值 |
| --- | --- |
| 交互语言 | `zh-CN` |
| expression_level | `eli5` |
| coverage_depth | `deep` |
| Lesson 时长 | `35` 分钟（30–40） |

前置：架构 Change `2026-09-14-namewta-architecture` 的地图课。本课不重讲 monorepo。

## 目标合同

公开切片 = 每个 `controller/*` 类型 + 每个 `usecase` 类型（实现与接口算同一切片）。工作树入口：

| 类型 | 路径 |
| --- | --- |
| `SsoOAuthController` | `controller/anonymous/SsoOAuthController.java` |
| `SsoSessionController` | `controller/anonymous/SsoSessionController.java` |
| `SsoOAuthUseCase` / `SsoOAuthUseCaseImpl` | `usecase/` |
| `SsoSessionUseCase` / `SsoSessionUseCaseImpl` | `usecase/` |

| ID | 可观察目标 | 关键性 | 前置 | 证据 | Lesson |
| --- | --- | --- | --- | --- | --- |
| OBJ-01 | 能口述 `GET /sso/oauth2/authorize`：`SsoOAuthController.authorize` → `SsoOAuthUseCase.authorize` → `SsoOAuthUseCaseImpl` → `SsoAuthorizationService` 及该路径上的 PKCE、Client 目录、会话、授权码 DAO | 是 | none | 解释+走查 | L-001 |
| OBJ-02 | 能口述 `POST /sso/oauth2/token` 与 `/revoke`：`exchange`/`revoke` 如何验 code、发业务令牌、作废令牌 | 是 | OBJ-01 | 解释+走查 | L-002 |
| OBJ-03 | 能口述 `POST /sso/login`、`GET /sso/session`、`POST /sso/logout`：`SsoSessionController` → `SsoSessionUseCase`/`Impl` → `SsoSessionService` → Redis 会话存储 | 是 | none | 解释+走查 | L-003 |

## 课程地图

```text
L-001 authorize（授权码+PKCE）
L-002 token / revoke
L-003 SSO 域会话 login/session/logout
```

每课 35 分钟。OAuth 切片因方法多拆成两课，但 `SsoOAuthController` 与 `SsoOAuthUseCaseImpl` 必须在 L-001 和 L-002 都出现。

## 成功证据与范围外

成功：能指到具体 Java 方法，不靠“大概是 OAuth”。范围外：OSS、Notify、Nacos、前端 sso-web 页面实现（可点名它是认人厅，不走组件树）。

## Revision 记录

| 时间 | 变化 | 原因 | 是否重新生成 Lesson |
| --- | --- | --- | --- |
| 2026-09-14T07:42:38.672Z | 初版三切片课程 | 从架构课拆出 SSO 深课 | 待生成 |
