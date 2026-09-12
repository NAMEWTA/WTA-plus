---
schema_version: 3
artifact: ticket
change: 2026-09-04-third-party-http-integration
id: T-01
title: 建立 ruoyi-third 模块装配与公共 Gateway 合同
status: done
planning_depth: deep
planning_depth_reason: 新增 Spring Boot 4.1 模块、跨模块 Java 公共 API、full/core 装配和安全配置入口，后续全部切片依赖其稳定签名。
ready: true
risk: high
blocked_by: []
contract_ids: [AC-012, AC-018]
owner: codex:/root
expected_changes:
  - "<Path>ruoyi-vue-plus-namewta/pom.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/third/api/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/**</Path>"
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/pom.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/pom.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/pom.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/application.yml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/third/api/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/pom.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/AGENTS.md</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/config/properties/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/test/java/org/dromara/third/contract/**</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/**</Path>"
shared_paths:
  - "<Path>ruoyi-vue-plus-namewta/pom.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/pom.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/pom.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/application.yml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/third/api/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/pom.xml</Path>"
shared_path_owners:
  - "<Path>ruoyi-vue-plus-namewta/pom.xml</Path> => T-01"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/pom.xml</Path> => T-01"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/pom.xml</Path> => T-01"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/application.yml</Path> => T-01"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/third/api/**</Path> => T-01"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/pom.xml</Path> => T-01"
---

# Ticket T-01: 建立 ruoyi-third 模块装配与公共 Gateway 合同

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/01-module-and-public-contracts.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-01.md</Path>`

## 1. 战略与来源

- **目标：** 建立单一 `ruoyi-third` jar、最小 common 依赖、配置入口和不暴露数据库/HTTP client 的公共调用合同。
- **可观察产出：** full/core 都能解析并启动模块；业务模块可编译调用 `ThirdPartyGateway`，动态与类型化结果具有稳定请求、响应和错误分类。
- **来源：** `US-001`、`US-006`、`US-009`、`AC-012`、`AC-018`、`ADR-001`、`ADR-005`。
- **当前事实：** reactor、Admin 装配和 `ruoyi-api` 尚无 third artifact 或公共合同。
- **Planning Depth 原因：** 公共签名与共享 POM 一旦错误会扩散到所有调用方，并影响两种 bundle。

## 2. 决策状态

### 已锁定决策

- 首期为单 jar，不预建聚合/BOM；公共 API 位于 `org.dromara.third.api`。
- Gateway 同步返回 `ThirdPartyResponse<T>`；动态值受限，类型化重载使用 `Class<T>` 或 `ParameterizedTypeReference<T>`。
- yml 仅声明主密钥外部占位符和静态安全上限，不提交真实密钥；缺失密钥不阻止无 Provider 数据的应用启动。

### 已采用的低影响假设

- DTO 采用邻近 `ruoyi-api` 的不可变风格；具体类名可依 Java 21 习惯调整但字段语义不变。

### 未决问题

无。

## 2.1 必须加载的 Skill 与工程基线

- **必须加载：** <Path>.agents/skills/engineering-standards/SKILL.md</Path>、<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>、<Path>.agents/skills/ruoyi-module-guide/SKILL.md</Path>、<Path>.agents/skills/ruoyi-common-modules-guide/SKILL.md</Path>、<Path>.agents/skills/java-api-compatibility/SKILL.md</Path>。
- **必须先读的参考：** <Path>.agents/skills/engineering-standards/references/project/00-project-profile.md</Path>、<Path>.agents/skills/engineering-standards/references/project/01-module-map.md</Path>、<Path>.agents/skills/engineering-standards/references/project/02-decisions-and-exceptions.md</Path>、<Path>.agents/skills/engineering-standards/references/project/03-backend-module-modes.md</Path>、<Path>.agents/skills/namewta-fullstack-development/references/backend/architecture.md</Path>、<Path>.agents/skills/namewta-fullstack-development/references/backend/module-layout.md</Path>、<Path>.agents/skills/namewta-fullstack-development/references/backend/implementation.md</Path>、<Path>.agents/skills/java-api-compatibility/SKILL.md</Path>。
- **目录与代码最低要求：** 新业务模块必须登记/验证为 layered；入口只能走 Controller/Listener/API Adapter → UseCase → Service → DAO → Mapper → XML，禁止 IService/ServiceImpl、Controller 直连 DAO/Mapper。ruoyi-api 只放不暴露 Entity/client 的稳定 DTO/接口，包与 POM 使用项目既有命名和 AGENTS 索引。
- **POM/API 要求：** 只声明实际使用的 ruoyi-common-* 子 artifact，不依赖父 ruoyi-common；full/core 依赖方向必须可复核。新增公共 Java API 必须执行兼容性检查、迁移 Javadoc 和调用方盘点。
- **执行停止条件：** 未完整读取以上 Skill/参考、试图套用 classic、扩大公共 API、修改未授权根 POM 或绕过 API compatibility 时立即停止并回到 Lead。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| Maven 装配、模块 AGENTS、配置属性、Gateway/Request/Response/错误枚举 | 现有 common 子模块和 Spring HTTP 类型 | DB、HTTP 执行、管理 CRUD、真实供应商 |

## 4. 要构建什么

任一后端业务模块只依赖 `ruoyi-api` 即可按 providerCode 和 endpointCode 构造受约束请求，并以统一结果读取 requestId、数据或稳定失败分类；应用在 full/core 中都可装配，且没有 Provider 配置时不要求生产凭据。

## 5. 实现契约

- **入口或接缝：** `ThirdPartyGateway` 动态与类型化同步方法。
- **输入与输出：** 编码、受限 query/header/body 值和目标类型，返回不含底层异常体的稳定响应。
- **公共接口变化：** additive 新增 `org.dromara.third.api`；后续演进遵守 Java API compatibility。
- **不变量：** 不公开 Entity、Mapper、RestClient、凭据或任意类名反射入口。
- **状态或数据流：** 此 Ticket 只冻结 API 与装配，Gateway 行为由 T-07 实现。
- **错误与失败行为：** 参数合同可表达拒绝、配置、传输、超时、Provider 业务错误；普通预期失败不泄露堆栈或原始 body。
- **兼容要求：** 现有 system/monitor/OpenAPI 行为不变；full/core 均包含 third。
- **安全与隐私要求：** 配置占位符默认空，禁止默认主密钥和样例生产 secret。

## 6. 执行路线

1. 以 API 字段白名单、模块图和双 bundle 装配测试建立失败接缝。
2. 注册模块、版本和 Admin 依赖，仅引入实际使用的 common 子 artifact。
3. 新增 Gateway、请求/响应、错误类别与错误码合同。
4. 增加类型安全配置属性和外部主密钥占位符，校验静态上下限。
5. 运行 API、模块模式、Spring Context 与 full/core 编译验证。

## 7. 路径访问契约

- **预计修改点/可写范围：** 仅 frontmatter 所列共享 POM、application.yml、API、模块 POM/AGENTS、properties 和合同测试。
- **只读上下文：** 邻近 API、Profile 模块装配惯例。
- **共享路径：** 根/模块/Admin POM、application.yml、third API 和模块 POM 由 T-01 唯一修改。
- **保留或不动：** 其他业务模块、私有 SQL、外部 `cde-third`。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | API/Context 合同 | `./mvnw -pl :ruoyi-third,:ruoyi-api -am test` | 公共合同可编译，模块无 Provider 数据可启动 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-01.md</Path>` |
| 失败路径 | 配置绑定测试 | 非法 timeout/cap 与空主密钥场景 | 非法静态值启动失败；空主密钥仅在凭据调用时关闭 | 同上 |
| 回归 | reactor/bundle | module-mode 校验并分别构建 full/core | 两种 bundle 解析 third，现有模块无回归 | 同上 |

- **Workspace checks：** 按 Goal Plan 在 current workspace 或 source worktree 运行 Maven 定向测试与模块模式校验。
- **E2E disposition：** not-required：本 Ticket 是 additive 公共合同与装配 prefactor，真实调用由 T-07/T-13 验证。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate。
- **Integration evidence：** implementation/source commit、parent before、适用 candidate/result SHA 和父分支包含关系。

## 9. 发布、迁移与恢复

- **迁移顺序：** 先 additive API/POM，再由后续 Ticket 实现消费者。
- **兼容窗口：** 新 API 无存量调用；已发布后只做兼容扩展。
- **监控信号：** Context 装配失败和非法静态配置。
- **回滚或前向恢复：** 消费者合入前可整体回滚；合入后保留 API 并前向修复。
- **不可逆操作与批准点：** 无生产动作；提交与集成仍需执行授权。
- **收缩条件：** 不适用：无旧合同待删除。

## 10. 验收标准

- [x] `AC-012`：动态与类型化公共合同稳定且不暴露底层实现。
- [x] `AC-018`：full/core 均装配 ruoyi-third，无 Provider 数据可启动。
- [x] 所有共享路径仅由 T-01 修改，验证与提交证据写入指定 Evidence。
- [x] E2E disposition 和 Goal Plan workspace 集成合同已执行，无未批准偏差。
