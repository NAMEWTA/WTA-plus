# 来源：wta-sso

访问日期：2026-09-14。权威为工作树 Java，不是 Skill 摘要。

| Source ID | 定位 | 支持的 claim |
| --- | --- | --- |
| S-SSO-01 | `controller/anonymous/SsoOAuthController.java` | `/sso/oauth2` authorize/token/revoke；依赖 `SsoOAuthUseCase`、`SsoSessionUseCase`、`SsoProperties` |
| S-SSO-02 | `controller/anonymous/SsoSessionController.java` | `/sso` login/session/logout |
| S-SSO-03 | `usecase/SsoOAuthUseCase.java`、`usecase/impl/SsoOAuthUseCaseImpl.java` | authorize/exchange/revoke 委托 `SsoAuthorizationService` |
| S-SSO-04 | `usecase/SsoSessionUseCase.java`、`usecase/impl/SsoSessionUseCaseImpl.java` | login/current/logout 委托 `SsoSessionService` |
| S-SSO-05 | `service/SsoAuthorizationService.java`、`service/SsoSessionService.java` | 授权码与会话规则 |
| S-SSO-06 | `dao/SsoAuthorizationCodeDao*.java`、`mapper/SsoAuthorizationCodeMapper.java` | 授权码持久化 |
| S-SSO-07 | `port/*.java`、`adapter/**/*.java`、`support/*.java`、`config/*.java` | 端口、适配器、PKCE、Cookie、配置 |
| S-SSO-08 | `03-backend-module-modes.md` | wta-sso 登记为 layered |

未决：生产环境 SSO 是否已部署（与架构课 C-003 相同，本课不关闭）。
