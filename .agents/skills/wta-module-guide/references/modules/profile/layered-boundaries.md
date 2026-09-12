# Profile 试点边界

五层调用规则、DAO/Mapper 职责、`domain` 分类、事务和中文 Javadoc 由 `engineering-standards` 与 `namewta-fullstack-development` 统一维护。本页只记录 Profile 的业务隔离和已确认的实现形状，避免把通用规范复制成另一份。

## 子域隔离

- `person` 与 `enterprise` 不得互相依赖实现类、Mapper、Entity、domain 类型或数据库表。
- enterprise 转移通过 `wta-api` 的 `PersonIdentityLookupService` 查询个人精确身份；不得直接访问 person 实现或表。
- 两个子域分别拥有自己的 Controller、UseCase、Service、DAO、Mapper、Entity 和读模型；跨子域结果通过公开合同或不可变的输入/输出模型传递。
- Profile 聚合 POM 只负责模块组合和版本管理，不承载业务实现。

## Profile 目标目录形状

```text
<profile-submodule>/src/main/java/org/dromara/profile/<capability>/
├── controller/{admin,self,anonymous}/
├── listener/                 # 事件入口，转换后委托 UseCase
├── usecase/{<Capability>UseCase.java,impl/<Capability>UseCaseImpl.java}
├── service/<Capability>Service.java
├── dao/<Capability>Dao.java  # 只封装 Mapper 查询/写入
├── mapper/<Capability>Mapper.java
├── domain/{<Entity>.java,bo/,vo/,model/read/,application/,material/,transfer/,verification/,exception/}
├── port/{gateway/,provider/,store/}  # Service 可消费的外部合同
├── adapter/{api/,codec/,gateway/,provider/,security/,store/,time/} # 合同实现和入口适配
└── support/       # 纯函数或无状态辅助（如有）
```

上述目录是 Profile 新能力的目标落点，不要求每个能力创建所有目录。当前 person 与 enterprise 的生产实现已统一放入 `adapter/api|codec|gateway|provider|security|store|time` 等职责目录；测试兼容桥只允许留在 `src/test`，不得回流到生产 `service/impl`。新增实现统一放入 `adapter`，迁移时只移动实现和注入点，不改变公开合同、URL、权限或状态语义。不要创建 `buz`、泛化 `manager/helper/util` 或 DAO/Repository 双套命名。

入口的唯一主链路是 `Controller|Listener|adapter/api -> UseCase -> Service -> DAO -> Mapper -> XML`。`UseCase` 只负责用例编排并调用 Service；Service 只调用本能力 DAO 和明确的 `port`，不能调用另一个 Service；DAO 只能调用本能力 Mapper；Mapper 只对应 SQL/XML。`adapter/gateway|provider|store` 不属于主链路层，作为端口实现由 Service 注入。API adapter 即使提供公开 SPI，也必须把合同转换后交给 UseCase，不能下沉到 DAO。

Profile 生产目录不创建 `service/impl`；Service 直接放在 `service` 下，`service/impl` 仅可作为测试兼容代码目录，且不代表 MyBatis-Plus 的 `ServiceImpl`。Profile Service 不得继承或导入 `IService`/`ServiceImpl`，也不得直接导入 Mapper；所有持久化访问都经 DAO 收敛。

迁移期生产目录中若仍存在 `I*Service`，只能作为测试或旧调用方的 `@Deprecated` 兼容合同，不得由 Controller、UseCase 或 Spring 主构造注入；测试夹具迁移完毕后应删除。

`domain` 按数据边界分类：根目录实体用于持久化和领域状态，`bo` 只承载入口输入，`vo` 只承载 HTTP 输出，`model/read` 只承载 Mapper 读模型；`application`、`material`、`transfer`、`verification` 放跨层传递的不可变命令/结果，`exception` 放业务失败合同。它们都是辅助类型，不得绕过五层主链路。

## 外部端口

- `Gateway`、`Provider`、`Store` 均为外部依赖的适配边界：合同放在 `port/`，实现放在 `adapter/`，只能由所属 Service 使用；其实现不得反向调用另一个 DAO 或 UseCase。
- `EnterpriseTransferChallengeStore` 是 Redis challenge Store，不是 DAO；不要把 Redis key/value 操作移入关系数据库 DAO。
- Verification Provider 只负责外部认证和证据规范化，不直接发布档案、发送通知或修改身份绑定。
- System 访问使用 `wta-api`/common SPI；Workflow 访问使用 `org.namewta.workflow.api` 合同。Profile POM 不依赖 system/workflow 实现模块。

## 读模型约束

Mapper 查询结果放在 `domain/model/read`，新类型使用能力语义的 `<Capability>Row` 或 `<Capability>Projection` 名称；Row 只经过 DAO/Service 转换为业务结果或 `domain/vo`，不得直接暴露到 Controller。当前仅 `person/domain/model/read/PersonAdminRows.java` 与 `enterprise/domain/model/read/EnterpriseAdminRows.java` 是兼容期的聚合读结果包装，不是新的命名模板；新增查询应按能力拆成具体 Row/Projection，存量包装在迁移时逐步拆分。读模型不是额外层，不能替代 Service/DAO。

## 迁移护栏

Profile 是新增模块的参考实现；新能力必须从入口到 XML 验证完整链路。存量 `wta-system` 与 `wta-workflow` 不因本页而迁移，任何兼容性变化需单独立项并更新其事实地图。
