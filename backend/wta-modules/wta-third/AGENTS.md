# wta-third

统一管理第三方 HTTP Provider 和 Endpoint。数据库是配置事实源，业务调用只能依赖 `wta-api` 的 `ThirdPartyGateway` 合同。

## Structure

Production code follows `controller/admin -> usecase/impl -> service -> dao -> mapper -> XML`; HTTP/SPI adapters and runtime policies remain module-owned ports. Persistence entities, read models, BOs, and VOs are separate; only the stable gateway contract crosses the module boundary. Credential, URI allowlist, cache snapshots, execution pipeline, and observability are module-owned.

## 约束

- Provider 是唯一可信 base URL 与共享策略来源，Endpoint 只能使用经校验的相对 path 和白名单参数。
- 不使用数据库脚本、SpEL 或任意类名反射；特殊签名、加密、分页和错误映射使用显式 Java SPI。
- 业务表不保存完整请求/响应和明文凭据；出站日志经过服务端不可关闭的脱敏与大小限制。

## 验证

在后端仓库根目录运行 `mvn -pl wta-modules/wta-third -am -DskipTests compile`，再运行本模块定向测试。
