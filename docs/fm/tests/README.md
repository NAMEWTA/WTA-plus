# classic 模板输出的运行验收

`RenderClassicFixtures.java` 使用 FreeMarker 2.3.34 的 dollar 插值模式，从当前模板渲染 Service 接口、Service 实现、Mapper 和 XML；分别使用 TestDemo 普通表、TestTree 无 ancestors 树和 SysDept 含 ancestors 树的真实 Entity/BO/VO 合同。测试只借用类型与 schema，不替换或修改 System 产品实现。

`ClassicTemplateMySqlChecks.java` 必须与这些输出一起编译，并在独立 JVM 中将生成 class 目录放在工程 classpath 之前；启动时检查 Service 确实来自生成目录。使用真实 MyBatis、MapStruct、动态事务和隔离 MySQL，执行 21 个场景：普通 CRUD、非法父节点、成环、带子节点拒删、合法叶删除、跨根/转根、同名节点、批删故障回滚、ancestors 子树更新/回滚/坏路径及两种树各三类并发竞争。基础设施错误、SQL 死锁和超时均不会被当作预期业务拒绝。

本 change 的可复现串行入口（仓根）：

```bash
python3 speculo/.speculo/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/run-t27-template.py T-27-template-recheck
```

前置条件：JDK 21、可用 Docker、当前工作树完成 `CrudMySqlHttpIntegrationTest` 后保留的 Surefire classpath，以及 `temp/team/lead/t27-template-engine/freemarker-2.3.34.jar`。引擎仅为测试依赖；下载来源、SHA-512 与 SHA-256 见同一 evidence 目录的 `T-27-template-engine-download.json`。启动器核对引擎 SHA-256，去除 classpath 空项，串行渲染/编译后初始化六文件基座。数据库使用随机名称、仅绑定 loopback；退出时清理自己的容器和编译临时目录，对比资源库存。生成的 9 份 Java 和 3 份 XML 及其哈希保留在该次 evidence 中。

边界：这些场景验证 classic 后端的结构与事务合同，不代表 layered 或 Vue/React 完整输出通过构建。模板 Mapper 仍需按资源配置 DataPermission；这里不把未配置的通用模板声称为权限隔离实现。实际 Demo 权限由 `DemoTreeMySqlIntegrationTest` 的真实 DataPermission/MySQL 场景和 `CrudMySqlHttpIntegrationTest` 的真实 HTTP 场景覆盖。
