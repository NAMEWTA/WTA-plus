# FreeMarker 上下文合同

未来模板运行器必须一次性构造下列上下文。字段缺失时应在渲染前失败，不允许以空字符串静默生成不完整代码。

## 表级字段

| 变量 | 含义 |
|---|---|
| `table` | 完整生成表对象，提供 `crud`、`tree`、`menuIds` 等属性 |
| `columns` | 按数据库字段顺序排列的列集合 |
| `pkColumn` | 主键列 |
| `tableName`、`functionName` | 表名与界面功能名 |
| `ClassName`、`className` | Java 类名及首字母小写形式 |
| `BusinessName`、`businessName` | 资源公开名及目录/变量名 |
| `moduleName` | 后端模块与前端一级 domain 标识 |
| `packageName`、`basePackage` | Java 包与其父包 |
| `packagePath` | `packageName` 将 `.` 替换为 `/` 后的 Java 输出目录片段 |
| `author`、`datetime` | 生成署名与日期 |
| `permissionPrefix` | 权限前缀，通常为 `<module>:<business>` |
| `parentMenuId` | 菜单父节点 ID |
| `clientPk` | `sys_client.id` 的数据库主键；不要与 OAuth `clientId` 字符串混用 |
| `dicts`、`dictsNoSymbol` | Vue/React 字典参数表达式及无引号名称列表 |
| `moduleLifecycle` | 模块生命周期：`new` 或 `legacy`，必填 |
| `architectureMode` | 后端模板模式：新模块只能为 `layered`，legacy 模块只能为 `classic`，必填 |
| `controllerSurface` | layered Controller 访问面：`admin`、`self` 或 `anonymous` |
| `transactionalCommands` | layered UseCase 中需要 `@DSTransactional` 的命令方法集合；事务边界不下沉 Service/DAO |
| `generateService` | layered 是否生成语义明确的 Service，强制为 `true` |
| `agentsRole` | AGENTS 导读角色：`aggregator`、`runnable-app`、`contract-module`、`capability-module` 或 `tooling` |
| `modulePath` | AGENTS 输出目录，相对对应子仓库根目录 |
| `modulePurpose` | AGENTS 的一句话职责 |
| `moduleComponents` | AGENTS 要点组件列表 |
| `entryPoints` | AGENTS 具体入口文件/类列表 |
| `dependencies` | AGENTS 直接依赖与主要消费者 |
| `verificationCommands` | AGENTS 最小验证命令 |
| `readNext` | AGENTS 后续阅读路径列表 |

读模型模板使用同一组 `packageName`、`packagePath`、`ClassName`、`functionName`、`tableName`、`columns` 上下文，输出到 `domain/model/read/${ClassName}Row.java`。列别名必须与 `javaField` 对应，读模型只作为 Mapper/DAO/Service 查询边界，不属于 HTTP 输出合同。DAO 可以把 MP `Page<Row>` 收敛成 `PageResult<Row>`，Service 负责唯一的 Row/Entity 到业务结果或 VO 转换。

## 列字段

每个列对象至少提供 `columnName`、`columnComment`、`columnLabel`、`javaField`、`capJavaField`、`javaType`、`tsType`、`htmlType`、`queryType`、`dictType`，以及 `pk`、`list`、`query`、`insert`、`edit`、`required`、`dictColumn` 等布尔属性。

## 功能开关

| 变量 | 关联字段 |
|---|---|
| `enableExport` | 是否生成导出入口 |
| `enableStatus` | `statusColumn`、`statusField` |
| `enableSort` | `sortColumn`、`sortField` |
| `enableUnique` | `uniqueColumns` |
| `hasBetween`、`needAddDateRange`、`needDateRange` | 日期范围查询 |
| `needDict` | 字典 runtime |
| `needImagePreview`、`needImageUpload` | 图片宿主组件 |
| `needFileUpload`、`needEditor` | 文件与编辑器宿主组件 |
| `needCheckbox`、`needSelect`、`needTextArea` | 表单控件分支 |
| `needDigit`、`needDateField`、`needSwitchField` | 数字、日期与开关分支 |
| `needParseTime` | 时间格式化 runtime |

## 树表字段

树表额外要求 `treeCode`、`treeParentCode`、`treeName`、`treeParentColumn`、`treeRootValue`、`treeRootValueTsLiteral`、`treeRootValueJavaLiteral`、`treeAncestorsField`、`treeOrderField` 与 `expandColumn`。运行器必须在渲染前验证父字段、根值和主键类型一致。

## 输出集成

模板运行器依据 [catalog.json](./catalog.json) 选择模板和目标路径。Vue resource 输出后仍需由所属 domain/web-domain 的公开 exports、service 组合、manifest 与目标 App 显式接入；模板不得通过副作用修改这些共享文件。

`sql/mysql.sql.ftl` 是待合并片段，不生成独立部署脚本。表结构变更合并到 `release-artifacts/docker/infrastructure/mysql/init/50-namewta-ddl.sql`；初始化数据、菜单和回填合并到 `release-artifacts/docker/infrastructure/mysql/init/60-namewta-dml.sql`。

渲染前必须验证：`moduleLifecycle=new` 只能配 `architectureMode=layered`，`moduleLifecycle=legacy` 只能配 `architectureMode=classic`，且两个字段不得缺失。当 `architectureMode=layered` 时，生成结果必须满足：Controller/Listener/API Adapter 只注入 UseCase；UseCase 只编排 Service；UseCase/Service 不导入 `PageQuery` 或 MyBatis `Page`；Service 只依赖自身 DAO 和明确外部 Port；Service 不调用另一个 Service；DAO 只依赖本 owner Mapper；DAO/API Adapter 不跨层调用；Mapper 方法与 XML statement 一一对应。事务命令由 UseCase 持有 `@DSTransactional`。不得生成 `IService`、`ServiceImpl` 继承、`DataSupport` 或 DAO/Repository 双套结构。

新模块模板输出的自有 Java 类型和显式方法必须带有简明中文 Javadoc；对外方法应根据实际合同补充参数、返回值、异常、事务、锁、幂等和外部副作用说明，不能用“处理业务”等空泛句式代替职责描述。业务代码中的 JSON 和 ID 生成只能调用项目统一的 `JsonUtils`、`IdGeneratorUtil`，模板不得生成 `new JsonMapper`、`ObjectMapper` 或 `IdWorker` 直连代码；这两个统一入口已兼容无 Spring 测试上下文的 fallback。
