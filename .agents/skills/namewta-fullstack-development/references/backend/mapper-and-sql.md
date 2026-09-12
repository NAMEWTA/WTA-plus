# Mapper、查询、XML 与 MySQL

## 目录

- [查询实现阶梯](#查询实现阶梯)
- [Mapper interface](#mapper-interface)
- [Wrapper 与 QueryBuilder](#wrapper-与-querybuilder)
- [注解 SQL 的窄例外](#注解-sql-的窄例外)
- [Mapper XML](#mapper-xml)
- [数据权限](#数据权限)
- [锁、批量与返回值](#锁批量与返回值)
- [运行时 SQL 与初始化基座](#运行时-sql-与初始化基座)
- [MySQL DDL 检查](#mysql-ddl-检查)

## 查询实现阶梯

每个查询选择能清晰表达需求的最低层，不以“SQL 能跑”为完成标准。`classic` 允许 Service 构建简单 Wrapper；`layered` 必须把 Wrapper、QueryBuilder、分页和条件更新封装在 DAO：

1. `BaseMapperPlus` 内建主键、VO、分页、batch 和 lambda 能力。
2. fresh lambda wrapper 或 `QueryBuilder`，使用类型化字段和 `eqIfPresent`、`eqIfText`、`likeIfText`、`betweenParams` 等条件。
3. `MPJBaseMapper` + `QueryBuilder.lambdaJoin`，处理需要类型化 join、选择 VO 和 alias 的查询。
4. Mapper XML，处理 wrapper/MPJ 无法清晰维护的动态 SQL、复杂子查询、聚合、批量语句或数据库特性。

不要为炫技跳级。简单 CRUD 不需要 XML；复杂 SQL 也不能为了减少一个 XML 文件而塞进 Java 注解。

## Mapper interface

标准声明从 `docs/fm/java/mapper.java.ftl` 开始：

```java
public interface XxxMapper extends BaseMapperPlus<Xxx, XxxVo> {
}
```

- 只有确有 join 查询时再附加 `MPJBaseMapper<Xxx>`。
- 可复用且紧贴查询的 wrapper builder 在 classic 可放 Mapper default method；layered 的业务查询条件统一放 DAO，Mapper default method 只能使用本 Mapper 的数据访问能力，不调用其他 Mapper 或业务层。
- 方法名表达查询或写入语义，参数使用 `@Param` 显式匹配 XML；批量方法明确空集合和返回值语义。
- 不注入 Service，不发外部请求，不维护缓存，不捕获并吞掉数据库异常。
- 分页、VO 映射和 batch 优先复用 `BaseMapperPlus`，不复制公共实现。

## Wrapper 与 QueryBuilder

- 每个请求/操作创建 fresh wrapper；wrapper 有可变状态，不放 Bean 字段、不跨线程/请求复用。
- 空字符串和 null 使用项目 `*IfPresent/*IfText` 能力，避免手工 if 铺满 service。
- 查询必须有确定排序；分页排序白名单化，不把前端字段直接拼接为 SQL。
- update wrapper 只设置允许字段，并在状态迁移、乐观锁或所有权更新中带旧状态/版本/owner 条件。
- `QueryBuilder.lambdaJoin("u", SysUser.class)` 一类 alias 必须与数据权限 `@DataColumn`、select 和条件完全一致。

当前 system 证据：

- `SysOperLogServiceImpl`、`SysNoticeServiceImpl`：service 中 `QueryBuilder.lambda` 动态条件。
- `SysDeptMapper`、`SysPostMapper`：MPJ 类型化 join。
- `SysUserMapper`、`SysRoleMapper`、`SysMenuMapper`：多表 VO、alias 和 join wrapper。

只取同形态做法，不复制与当前业务无关的字段或权限。

## 注解 SQL 的窄例外

`SysOssConfigMapper` 存在短小、静态、无需动态拼接的 `@Select/@Update`，例如计数或单行 `for update`。这说明注解 SQL 不是绝对禁用，但必须同时满足：

- SQL 短小且一次可读完；
- 无动态条件、复杂 join、子查询树或长字段清单；
- 参数类型明确并有测试；
- 同模块已有一致证据，且 XML 不会明显提高可维护性。

长 SQL、字符串拼接、动态分支、复杂 join/聚合、批量 SQL 和多行更新必须使用 wrapper/MPJ/XML 中合适层级。禁止把 SQL 拆成多个 Java 字符串常量来规避该规则。

## Mapper XML

从 `docs/fm/xml/mapper.xml.ftl` 建立骨架，文件必须位于：

```text
src/main/resources/mapper/<module>/<Business>Mapper.xml
```

规则：

- `namespace` 精确等于 Mapper interface 全限定名。
- statement `id` 精确等于方法名；`parameterType/resultType/resultMap` 与 Java 合同一致。
- XML 参数使用 `@Param` 名称，不依赖编译器偶然保留的参数名。
- wrapper 片段使用 `${ew.customSqlSegment}` 时，只接受 MyBatis-Plus 生成的受控 wrapper；不把外部字符串放入 `${}`。
- join 字段带稳定 alias；数据权限列与 alias 一致。
- 提取 `<sql>` 片段仅用于真实重复列/条件，不建立难以追踪的层层 include。
- 动态条件使用 `<if>/<foreach>/<choose>` 等结构化标签，不拼接外部 SQL。

当前 system 中可阅读的自定义 XML 证据包括：

- `SysNotifyLogMapper.xml`、`SysNotifyDeliveryLogMapper.xml`：通知日志查询。
- `SysOssMapper.xml`、`SysOssRefMapper.xml`：OSS 查询和引用关系。
- `SysOpenApiAuthorizationMapper.xml`：OpenAPI 授权复杂查询。

许多 system XML 只有 namespace 骨架，表示对应查询已迁到 wrapper/MPJ；空骨架不是“所有 Mapper 必须有 XML”的依据。

## 数据权限

- `@DataPermission` 覆盖实际执行 SQL 的 mapper 方法，不只标在没有执行查询的上层接口。
- `@DataColumn.key/value` 与 SQL 表 alias/列完全一致；join 后必须使用带 alias 字段消除歧义。
- list、page、export、options、count、update、delete 都按真实数据泄露/越权风险评估，不能只保护页面列表。
- 管理员跳过、角色范围、自定义部门、本人数据和 Client 边界分别做正向与负向测试。
- 前端隐藏、Controller 权限和数据权限是不同层次，三者不能互相替代。

## 锁、批量与返回值

- 行锁查询必须在有效数据库事务内调用；没有事务的 `for update` 不构成可靠保护。
- 乐观锁使用 `version` 或条件更新，并对更新 0 行作冲突处理；不要静默报告成功。
- batch insert/update/delete 复用公共 batch 能力，明确批大小、事务范围、空集合和部分失败策略。
- 数据库唯一索引是并发唯一性的最终保障，service 预检查用于友好错误，不能替代约束。
- 删除采用项目逻辑删除合同；物理删除只在明确表语义和审计要求允许时使用。

## 运行时 SQL 与初始化基座

必须区分两类 SQL：

| 类型 | Owner | 形态 |
|---|---|---|
| 运行时查询/写入 | Mapper wrapper、MPJ、Mapper XML，窄例外为短静态注解 | 随应用执行 |
| 环境初始化 | 父仓库 `release-artifacts/docker/infrastructure/mysql/init/` | 六份 MySQL 8.4 完整可重建基座 |

NAMEWTA 只支持 MySQL：

- 表、索引、约束等 DDL 合并到 `50-namewta-ddl.sql`。
- 初始化数据、菜单、权限、字典和数据回填合并到 `60-namewta-dml.sql`。
- `docs/fm/sql/mysql.sql.ftl` 只是菜单 DML 片段，必须渲染后合并到 `60-namewta-dml.sql`，不能输出 `${businessName}Menu.sql` 一类独立部署文件。
- 不恢复 Oracle、PostgreSQL、SQL Server 模板，不在后端建立 `script/`，不创建模块私有 SQL 副本。
- 50/60 是当前完整基座，可为保持全新库可重建而整理相关段落；它们不是按日期无限追加的 migration 日志。

全新库顺序为 `10 -> 20 -> 30 -> 40 -> 50 -> 60`。已有库不得直接重放基座：指定源/目标 Git Tag，备份，生成差异 SQL，在隔离副本演练升级和回滚，并记录验证结果。

## MySQL DDL 检查

- 新建项目自有表包含 `version/create_dept/create_time/create_by/update_time/update_by/del_flag`，并与 Entity 映射一致。
- 主键、唯一键、外键策略、索引前缀和排序规则与查询/并发需求一致。
- 时间、金额、状态、JSON、文本长度与 Java 类型/nullability 一致。
- Client/tenant/owner 范围字段有支持权限查询的索引，不能只依赖应用过滤。
- DML 的权限字符串、菜单路径、组件、`client_id` 与 Controller/前端一致；`clientPk` 是 Long 数据库主键，不是 OAuth clientId。
- 菜单 DML 的 `icon` 保持字符串合同：新 NAMEWTA 展示菜单优先使用 `tabler:name`，功能节点使用 `#`，本地 SVG 使用 `local-name`；不得把 Element Plus 组件名写入 `sys_menu.icon`，也不得新增图标表或字段。已有无前缀值由前端兼容解析，未知值必须由前端 fallback 并可诊断。
