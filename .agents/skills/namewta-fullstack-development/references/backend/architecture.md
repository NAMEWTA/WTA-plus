# Maven 模块与依赖边界

## 模块职责

| 模块 | 当前职责 | 禁止承载 |
|---|---|---|
| `wta-admin` | Spring Boot 部署入口、运行配置和 bundle 组装 | 可复用领域实现、其他模块内部类型的适配层 |
| `wta-api` | 跨业务模块公开 Service/DTO 合同 | 业务模块私有 entity、mapper、实现细节 |
| `wta-common/wta-common-*` | 按能力拆分的基础设施、工具和稳定 SPI | 单一业务 owner 的领域规则 |
| `wta-modules/wta-system` | 用户、Client、角色、菜单、权限、组织、资源和监控 | 其他业务模块的私有用例 |
| `wta-modules/wta-workflow` | 流程定义、任务、实例和业务审批 | 业务模块状态的最终所有权 |
| `wta-modules/wta-profile` | profile 聚合 POM 与 BOM；person/enterprise 是可组装业务 jar | 部署入口和跨模块内部调用捷径 |
| `wta-modules/{wta-demo,wta-ai,wta-job}` | 示例、AI 和任务业务能力 | 通用基础设施容器 |
| `wta-extend` | Monitor、SnailJob、SnailAI 等独立部署应用 | admin 内嵌业务模块 |

根 Maven reactor 当前包含 46 个项目。`bundle-full` 组装 job/ai/demo/workflow/profile；`bundle-core` 保留平台基础与 profile person/enterprise，排除 job/ai/demo/workflow。调整模块时同时核对根 POM、聚合 POM、BOM、`wta-admin/pom.xml` 和双 bundle 验证脚本。

## 依赖方向

```text
deployable app (wta-admin / wta-extend app)
  -> wta-modules
  -> wta-api
  -> selected wta-common-* modules

business module A -> wta-api/common SPI <- business module B
```

- common 不反向依赖业务模块。
- 业务模块不引用其他业务模块的 mapper、entity、BO、VO、controller 或 service implementation。
- 跨模块调用在同一 JVM 内通过 Spring 注入 `wta-api` Service 或明确 common SPI；当前仓库没有通用 Feign/Dubbo 远程层。
- `wta-admin` 的组装可见性不能成为业务依赖理由。
- classic 存量模块可沿用 `Controller -> Service -> ServiceImpl -> Mapper -> Mapper XML`；layered 新模块必须使用 `Controller/Listener/API Adapter -> UseCase -> Service -> DAO -> Mapper -> Mapper XML`。layered DAO 是唯一业务持久化入口，禁止以 `*DataSupport`、同义 repository、manager 或空壳转发类替代真实 DAO。Gateway/Provider/Store 仅在存在稳定外部端口或真实 adapter 时建立，并由 Service 使用。

## 新建子模块清单

1. 明确单一业务 owner、artifactId、Java package 和是否需要独立 jar。
2. 在最近的聚合 POM 注册 `<module>`；需要版本对齐时进入对应 BOM/dependencyManagement。
3. 只声明真实使用的 `wta-api` 和 `wta-common-*` 依赖，不复制其他模块整份依赖。
4. 使用 [module-layout.md](module-layout.md) 的目录主轴和访问面分区。
5. 需要被 admin 运行时加载时，显式决定进入 `bundle-full`、`bundle-core` 或两者，并更新 bundle 断言。
6. 对外跨模块能力先设计 `wta-api` 合同和兼容策略；内部 CRUD 不因“以后可能复用”提前公开。
7. 同步测试、配置、MySQL 50/60 基座、前端调用方和部署资产；没有变化的层必须显式确认。

## 条件能力与工作流

- 可选 workflow 的 controller、service、listener、rule/config 等入口保持一致的启用条件，禁用时不得留下缺依赖 Bean。
- 业务模块拥有自身业务状态；工作流通过公开门面、businessId 和事件协作，不直接写业务模块私表。
- 触及工作流或 system 服务时加载统一的 `wta-module-guide` 对应 module reference；不再维护专用 Skill 入口或兼容别名。

## 安全边界

认证、权限、Client 和菜单是跨端合同。OAuth 字符串 `clientId` 与数据库 Long 主键 `clientId/clientPk` 必须在命名和类型上区分。前端可见性不是授权边界，最终认证、权限、数据范围和 Client 隔离由后端完成；触及这些路径时逐项核对 `docs/upstream/customization-map.md`。
