# Evidence — S-spec 只读代码锚点（2026-09-13）

未改产品代码。确认下列现状供 Spec Path 引用：

| 锚点 | 路径 | 观察 |
|---|---|---|
| clientid 隔离 | `backend/wta-common/wta-common-security/src/main/java/org/namewta/common/security/config/SecurityConfig.java` | header/param `clientid` 须与 Token extra 一致，否则 NotLoginException |
| CLIENT_KEY | `backend/wta-common/wta-common-satoken/src/main/java/org/namewta/common/satoken/utils/LoginHelper.java` | `CLIENT_KEY = "clientid"`；另有 `clientPk` |
| 授权策略 | `backend/wta-admin/src/main/java/org/namewta/web/service/IAuthStrategy.java` | grantType → `{grantType}AuthStrategy`；已有 password/sms/email/social/xcx |
| 应用目录 | `backend/wta-modules/wta-system/src/main/java/org/namewta/system/domain/SysClient.java` | 表 `sys_client`；含 `clientId`/`clientSecret`/`grantType` 等 |
| client context | `backend/wta-admin/src/main/java/org/namewta/web/controller/AuthController.java` | `GET /auth/client/context` 现返回 enabled/register/passwordPolicy；Spec 要求扩展 sso 字段 |
