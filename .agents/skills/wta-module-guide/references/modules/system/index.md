# wta-system 模块索引

`wta-system` 是当前存量基础模块。本轮保持它的 classic `controller -> service/impl -> mapper` 实现、内部接口和数据库结构不变；本目录只记录其他模块可以使用的稳定能力和必须避免的内部实现面。

## 何时读取

| 任务 | 继续读取 |
|---|---|
| 选择 `wta-api`、HTTP 前缀、事件和模块结构 | [capability-map.md](capability-map.md) |
| 给新业务模块接用户、部门、OSS、消息或办理人 | [how-other-modules-call.md](how-other-modules-call.md) |
| 处理用户、角色、菜单、字典、配置、OSS、消息或登录域 | [domains.md](domains.md) |
| 修改字典类型、字典值或前端回显 | [dictionary-management.md](dictionary-management.md) |

## 允许的调用面

- 其他模块的 POM 只依赖 `wta-api`；需要字典翻译时注入 `DictService` common SPI。
- 跨模块 Java 调用在同一 JVM 中通过 `org.namewta.system.api.*` 注入，禁止 Feign/Dubbo、HTTP 调用 `/system` 或注入 `ISys*`。
- 前端管理页面走 `/system`、`/resource`、`/monitor` Controller；这些路径不是跨模块 Java 合同。
- `wta-admin` 是登录和启动组装例外，可以穿透 system 内部服务、Mapper 和 domain；新业务模块不得仿照。

## 存量边界

本模块无 tenant；登录隔离使用 Client + UserType。`wta-system` 的 `ISys*`、Mapper、Entity/BO/VO、Controller、`ClientSessionService`、`ClientUserTypeAccessService` 和资源 XML 均为内部实现面。新增模块需要相同能力时，先查本目录的 API/SPI 映射，不猜接口。
