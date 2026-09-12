# CRUD、HTTP 与业务层实现

## 模板不是泛指目录

实现标准 CRUD 时逐项打开下列精确文件，并将模板输出放入 [module-layout.md](module-layout.md) 的目录。不要只读取 `docs/fm/java` 目录名后凭习惯重写：

| 层 | 精确模板 | 模板负责 | 实现时必须补充 |
|---|---|---|---|
| Entity | `docs/fm/java/domain.java.ftl` | 表映射、主键、审计/版本/逻辑删除字段分支 | 当前 50 DDL、nullable、枚举与敏感字段 |
| BO | `docs/fm/java/bo.java.ftl` | 输入字段、AutoMapper、Add/Edit groups | 跨字段校验、访问面输入白名单 |
| VO | `docs/fm/java/vo.java.ftl` | 响应字段、AutoMapper、Excel/Translation 分支 | 对外可见性、脱敏、导出合同 |
| Read Model | `docs/fm/java/read.java.ftl` | Mapper 查询读模型骨架 | 查询字段、SQL alias、DAO/Service 可见边界 |
| Mapper | `docs/fm/java/mapper.java.ftl`；layered 使用 `docs/fm/java/layered/mapper.java.ftl` | classic 为 `BaseMapperPlus<Entity, Vo>`；layered 为 `BaseMapperPlus<Entity, Row>` | 数据权限、MPJ/XML 和锁语义；layered 的 Row 放在 `domain/model/read` |
| Service 接口 | `docs/fm/java/service.java.ftl` | CRUD 用例合同 | 业务命名、批量/失败语义、公开边界 |
| Service 实现 | `docs/fm/java/serviceImpl.java.ftl` | wrapper、映射、唯一性、树、CRUD | 事务、关系、缓存、并发与副作用 |
| Controller | `docs/fm/java/controller.java.ftl` | GET/POST、权限、校验、`@Log`、Excel | admin/anonymous 目录、Client/签名/重放合同 |
| Mapper XML | `docs/fm/xml/mapper.xml.ftl` | namespace 与 XML 骨架 | 参数、result、alias、数据权限和 SQL 测试 |

模板出现的条件分支必须结合 `docs/fm/context-contract.md` 解释。模板与当前 owner 的成熟行为冲突时，保留权限、事务、缓存、关系、树和兼容合同，并将可重复的模板缺陷修回 `docs/fm`。

## Entity、BO 与 VO

- 新建 NAMEWTA 自有表按工程持久化规范包含 `version/create_dept/create_time/create_by/update_time/update_by/del_flag`；Entity 与 DDL 一致。
- Entity 继承 `BaseEntity` 获得审计字段；不得为方便 HTTP 直接在 Entity 堆请求校验和展示注解。
- BO 使用 `@AutoMapper(target = ..., reverseConvertGenerate = false)`、Serializable、Add/Edit validation groups。Query、Form 若业务差异显著可拆分，但不能建立多个内容相同的 DTO。
- 对外 VO 只公开调用方需要的字段；敏感、凭据、内部状态和逻辑删除字段默认不出现在响应。复杂 Mapper XML 所需的类型化查询投影放在 `domain/model/read`，不得把内部读模型放入 `domain/vo` 或直接由 Controller 返回。
- 跨层转换沿用 `MapstructUtils`/项目 AutoMapper 合同，不手写散落字段复制，也不把 Entity 直接用作 Controller 返回或跨模块 DTO。
- OAuth 字符串 `clientId` 与数据库 Long `clientId/clientPk` 不混用；必要时在 BO/VO 使用更明确名称。

## Controller 与 HTTP

- list/page/detail/tree/options 等只读操作使用 GET，不接收 request body。
- add/edit/remove、批量删除、状态和排序变更使用 POST；CRUD 不使用 PUT/PATCH/DELETE。
- 每个 POST 业务方法都有准确 `@Log`：新增 `INSERT`，编辑/状态/排序 `UPDATE`，删除 `DELETE`。凭据、Token、证件和不受控正文通过 `excludeParamNames`、`isSaveRequestData`、`isSaveResponseData` 控制。
- 写操作按风险使用 `@RepeatSubmit`；权限字符串与菜单 DML、前端按钮和测试完全一致。
- 使用 `@Validated(AddGroup.class)`/`EditGroup.class`、path/query/body 的准确绑定以及 `R`/`PageResult` 现有返回合同。
- URL 必须无 mapping 冲突；前端 transport 与 Controller 同步变更。

访问面必须在目录和注解上双重明确：

- `controller/admin`：登录、`@SaCheckPermission`、数据范围与 Client 隔离均在后端落地。
- `controller/anonymous`：显式 `@SaIgnore`；继续执行签名、时间戳窗口、nonce/重放、幂等、限流、审计和日志脱敏。
- 其他客户端：只有真实 App/协议存在并有测试时建立；不能用“未来可能有”作为目录依据。
- 已登录自服务：先由规格确认其认证主体和访问面，不默认归入 admin 或 anonymous。

## Service 实现

本节的直接持有 Mapper 规则只适用于 `classic`。`layered` 模块必须遵循 `UseCase -> Service -> DAO -> Mapper`：UseCase 只编排 Service，Service 只持有自己的 DAO 和外部端口，DAO 持有 Mapper 并封装全部 Wrapper/QueryBuilder/分页/锁条件。Controller 可以接收 `PageQuery`，但应在入口解包为页码、页大小和排序基础值；UseCase/Service 禁止导入 `PageQuery`、MyBatis `Page`、Mapper 或 `IService`/`ServiceImpl`。DAO 可以在内部使用这些持久化类型，并将 `Page<Row>` 收敛为 `PageResult<Row>`。

classic ServiceImpl 拥有完整业务用例；layered Service 只拥有业务规则和外部端口，完整用例由 UseCase 编排：

1. classic ServiceImpl 构建 fresh 查询 wrapper；layered DAO 处理 null、空文本、日期区间、确定排序和持久化映射。
2. DAO 将 MP Page 收敛为 `PageResult<Row>`；Service 将 BO 映射为 Entity、将 Entity/Row 映射为业务模型，并负责业务模型到 VO 的转换。
3. Service 校验唯一性、引用存在性、Client/用户类型、状态迁移、删除约束和树不变量，不调用另一个 Service。
4. UseCase 在一个业务事务中编排主体、关系表和必要状态；事务边界、跨 Service 组合和锁查询不得下沉到 Service 或 DAO。
5. 成功后按 owner 合同失效所有相关缓存；外部通知、会话或事件说明提交前后与失败补偿。

关键业务失败使用现有 `ServiceException` 合同，不用 mapper 返回 0 静默掩盖。insert 按合同回填主键；update/status/sort 只写允许字段；唯一性编辑检查排除当前主键。不要让 Controller 或 Mapper 接管这些规则。

`docs/fm/java/serviceImpl.java.ftl` 的依赖形态只适用于登记的 classic 存量模块：ServiceImpl 直接注入 Mapper。layered 模式必须使用 `docs/fm/java/layered/` 模板并新增 DAO；不得把 classic ServiceImpl 规则套到新模块。layered 中不得使用 `*DataSupport`、同义 repository/manager 或 `IService`/`ServiceImpl`，跨 DAO 的事务编排属于 UseCase，跨 Mapper 的持久化组合属于 owner DAO。只有存在真实可替换基础设施实现时才抽取 Gateway、Store、Provider 或 Policy 端口。

classic 业务协作者统一注入 `service` 根接口；layered 入口只注入 UseCase，UseCase 只注入 Service。任何模式下 Controller、Listener 或其他实现都不得依赖具体 `*ServiceImpl`。需要跨用例调用的方法应进入对应模块内 Service 合同；仅供单个实现使用的步骤继续保持私有方法，不为满足形式制造接口。

## 事务与并发

- 新建或实质修改的业务事务使用 dynamic-datasource `@DSTransactional`，不要照搬 Spring `@Transactional`。
- `@DSTransactional` 默认对 `Exception` 回滚；只有明确失败语义和测试时调整 propagation/rollback/noRollback。
- 注意 Spring 代理边界和 self-invocation；事务入口必须是可代理的业务方法。
- 提交阶段事件使用匹配的 `@DsTxEventListener`；不要混用 Spring transaction event 造成监听不到提交。
- 涉及并发审批、状态迁移、唯一约束、库存/配额时，明确数据库唯一键、乐观锁 `version`、条件更新、`for update` 或分布式锁的选择，不依赖“先查后写”侥幸正确。
- 锁内禁止不可控远程 I/O；异常路径必须释放锁并保持数据库、缓存、会话一致。

## 关系、缓存与副作用

- 用户-角色、角色-菜单等主体与关系变更放入覆盖完整用例的事务。
- 替换关系时明确空集合是“清空”还是“无变化”，并测试重复、跨 Client 和无效引用。
- 先写数据库，再按 owner 合同失效/重建缓存；列出所有受影响 key，不只清当前对象。
- 会话踢出、消息、对象存储和第三方调用说明幂等键、重试、超时、补偿与日志脱敏。
- 翻译使用公共批量合同，避免逐项 N+1；Excel 导入先校验再持久化，明确原子/部分失败语义，导出只使用 VO 声明字段。

## 树结构

- 保存时统一处理 root value、parent existence、self-parent、ancestor path 和排序。
- 移动节点拒绝自身或后代作为父节点，并在同一一致性边界更新后代 ancestors。
- 删除前检查子节点或明确级联策略；前后端对 root、id、parentId、children 的表示一致。
- 覆盖 root/child insert、move、cycle rejection、descendant ancestors 和 delete-with-children 测试。

## 复杂模块取样

`wta-system` 是复杂实现证据，不是整目录复制源：

- `SysUserServiceImpl`：关系维护、业务校验、事务和查询组合。
- `SysUserMapper`、`SysRoleMapper`、`SysMenuMapper`：类型化 MPJ、alias 与数据权限思路。
- `SysOssConfigMapper`：短小静态注解 SQL 的窄例外，不是把业务 SQL 写进注解的依据。
- `notify`、`openapi`、`oss` 子树：只有稳定 owner/生命周期时才增加子领域目录。

具体 system 能力先加载 `wta-module-guide` 的 system reference；不要绕过其公开 API 直接依赖内部实现。
