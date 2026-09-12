---
schema_version: 3
artifact: spec
change: 2026-09-01-admin-runtime-capability-reconciliation
status: ready
ready_for_tickets: true
sources:
  - USER-DECISION:2026-09-01-menu-corrections
  - USER-DECISION:2026-09-01-execute-confirmed-plan
  - DIAG-001
  - CHANGE:2026-08-30-openapi-common-module
  - CHANGE:2026-08-31-optional-nacos-dynamic-config
  - CHANGE:2026-08-28-retire-runtime-code-generator
---

# Spec: 收敛 Admin 运行能力与开发环境状态

- **Spec：** `<Path>{roots.state}/specdev/changes/2026-09-01-admin-runtime-capability-reconciliation/spec.md</Path>`
- **当前 ADR：** `<Path>{roots.state}/specdev/changes/2026-09-01-admin-runtime-capability-reconciliation/ADR.md</Path>`
- **当前领域上下文：** `<Path>{roots.state}/specdev/changes/2026-09-01-admin-runtime-capability-reconciliation/CONTEXT.md</Path>`

## 1. 问题与目标

### 问题陈述

OpenAPI、Nacos 和已退役代码生成器的源码、发布配置、数据库 schema/menu 与实际运行状态没有形成一致最终态：OpenAPI 页面存在但后端条件 Controller 未装配且菜单数据缺失；Nacos 入口名称和位置不符合最新产品决定；生成器运行代码已经删除但数据库仍向前端投影陈旧菜单和表。

### 目标用户与场景

- Admin 用户需要在个人设置中正常使用本人 OpenAPI 凭据和接口目录。
- 超级管理员需要从“系统管理 > OpenAPI管理”进入目标用户管理面。
- 运维管理员需要从“系统监控 > Nacos配置中心”进入官方 Nacos 控制台。
- 所有 Admin 用户都不应再看到“系统工具”或“代码生成”。
- 部署维护者需要用同一组可重复门禁证明 fresh、已有数据库 upgrade、双后端实例和菜单投影全部收敛。

### 成功标准

- 目标开发环境两个后端实例都装配 OpenAPI；本人目录请求不再返回业务 404。
- 数据库拥有且仅拥有一组固定 OpenAPI 菜单权限和一个固定 Nacos 菜单，显示名称与父菜单正确。
- 数据库不再包含生成器菜单、角色关系或两张生成器表。
- OpenAPI 在没有显式环境配置的其他安装中继续默认关闭，KEK 永不进入版本库、浏览器或日志。
- fresh install、当前混合状态 upgrade、重复执行、构建和真实运行验收都有 Evidence。

### 非目标

- 不改变 OpenAPI 签名协议、凭据生命周期、权限并集算法或公开 HTTP 路径。
- 不为 Nacos 建立 RuoYi CRUD、SSO、自动登录或凭据注入。
- 不恢复或替代运行时代码生成器。
- 不修改冻结上游 SQL、不操作同机 CDE、不执行远端 push 或生产发布。

## 2. 解决方案与外部行为

### 解决方案摘要

保留 OpenAPI default-off 安全模型，为受管发布补齐显式配置透传；用新的 NAMEWTA append-only DDL/DML 尾部块将 fresh 与已有混合数据库收敛到相同菜单/schema 最终态；按用户明确的无备份决定在开发环境执行新块、逐实例滚动启用 OpenAPI，并以真实 HTTP、数据库查询和 Admin 菜单验收关闭问题。

### 主要流程

1. 发布维护者为目标环境生成或取得合法 32-byte Base64 KEK 和非敏感版本标识，将三项 OpenAPI 配置写入权限受限且被忽略的部署配置。
2. 升级前保存当前发布定位，记录用户明确的无备份决定，并确认生成器表为 0 行且对象身份匹配。
3. 执行本 change 新增的 DDL/DML 块：移除生成器数据库面、补齐 OpenAPI 菜单、移动并重命名 Nacos 菜单。
4. 逐个滚动两个后端实例；每个实例启动和 OpenAPI 路由验证通过后才继续下一个。
5. 刷新登录/菜单状态，验证个人 OpenAPI、系统管理菜单、系统监控菜单以及生成器入口消失。

### 边界、失败与稳定错误行为

- 未显式启用：OpenAPI Controller 和网关继续不装配，普通 Token 请求不受影响。
- 显式启用但 KEK、KEK version、Redis 或 SPI 无效：应用启动失败，错误只指出配置键或缺失类型，不回显 key material。
- SQL 固定 ID、父菜单或现有 component/permission 与允许状态冲突：迁移必须在破坏性语句前失败，不进行宽泛删除或覆盖。
- OpenAPI 无登录请求：启用后到达既有认证/授权边界，不得再表现为路由不存在。
- Nacos 不可用：菜单仍指向既有安全 URL，页面显示连接失败；不影响其他系统监控页面。
- 第一个实例滚动失败：停止第二个实例升级，恢复该实例 `OPENAPI_ENABLED=false` 或上一发布配置。

### 状态转换与不变量

```text
OpenAPI runtime: default-off -> configured -> instance-1 verified -> instance-2 verified
Database: mixed -> backed-up -> DDL reconciled -> DML reconciled -> final-state verified
Generator: source-retired/data-present -> backed-up -> data-retired
Nacos menu: 系统管理/配置中心 -> 系统监控/Nacos配置中心
```

- OpenAPI component key、permissions、HTTP 路径和个人/管理员 scope 不变。
- Nacos `menu_id`、path、component、permission 和独立鉴权不变。
- 生成器只允许从“残留”向“完全不存在”转换，不建立兼容回退面。
- 普通角色不因迁移自动获得 OpenAPI 或 Nacos 权限。

## 3. 用户故事

- **US-001**：作为普通 Admin 用户，我希望个人 OpenAPI 页面连接真实后端能力，以便创建凭据并查看本人可调用接口。
- **US-002**：作为超级管理员，我希望从系统管理看到“OpenAPI管理”，以便管理目标用户的凭据与接口目录。
- **US-003**：作为运维管理员，我希望从系统监控看到“Nacos配置中心”，以便进入官方控制台。
- **US-004**：作为 Admin 用户，我希望不再看到已删除的系统工具和代码生成，以免进入无效页面。
- **US-005**：作为部署维护者，我希望 fresh 和 upgrade 得到相同最终态，并在失败时保留可执行恢复路径。
- **US-006**：作为安全维护者，我希望 OpenAPI 仍默认关闭且 KEK 仅由私密部署配置提供，以免修复扩大默认暴露面。

## 4. 验收合同

| ID | 前置条件 | 动作或事件 | 可观察结果 | 验证接缝 |
|---|---|---|---|---|
| AC-001 | 未提供 OpenAPI 环境配置 | 启动应用 | OpenAPI Bean、Controller、网关不装配；普通 MVC 不变 | Spring context/MockMvc |
| AC-002 | 提供合法 OpenAPI 配置与既有 Redis/SPI | 启动应用 | 恰好装配一套网关和管理 Controller | Spring context + package assembly |
| AC-003 | OpenAPI 已启用 | 无 Token 请求本人目录 | 返回既有认证/授权失败而非业务 404 | 真实 HTTP 探针 |
| AC-004 | KEK/version/依赖无效 | 启动应用 | fail closed，错误不包含 key material | 启动失败矩阵 |
| AC-005 | fresh 或缺菜单的已有库 | 执行新 DML | 固定六条 OpenAPI 菜单存在；主菜单为“OpenAPI管理”且位于系统管理 | SQL contract + MySQL |
| AC-006 | 固定 Nacos 菜单为历史或缺失状态 | 执行新 DML | 唯一菜单为“Nacos配置中心”，父菜单是系统监控，component/permission 不变 | SQL contract + MySQL |
| AC-007 | 生成器菜单完整历史状态或已删除 | 执行新 DML | 九个固定菜单和全部对应角色关系为 0 | MySQL upgrade/replay |
| AC-008 | `gen_table*` 存在、用户批准无备份且两表为 0 行 | 执行新 DDL | 两张生成器表不存在 | MySQL upgrade/replay |
| AC-009 | 当前数据库混合状态 | 执行本 change 两个新块 | OpenAPI、Nacos、生成器四项最终态全为真 | 诊断 SQL |
| AC-010 | 已达到目标最终态 | 重复执行两个新块 | 成功且计数、菜单和非目标数据不变 | MySQL replay |
| AC-011 | 现有数据与固定 ID/component/permission 冲突 | 执行 DML | 在任何目标删除或覆盖前失败 | 冲突哨兵测试 |
| AC-012 | 发布 Compose 与 env 示例 | 解析静态配置 | 三个 OpenAPI 变量被两个实例统一透传，默认仍为 false且无真实 secret | Node release config test |
| AC-013 | 普通角色没有显式授权 | 执行迁移 | 不新增 OpenAPI/Nacos `sys_role_menu` 行 | SQL contract + DB query |
| AC-014 | 当前前端构建和目标菜单数据 | 恢复动态导航 | OpenAPI/Nacos component 都可解析；旧 `tool/gen` 继续失败关闭且不显示 | Vitest + Admin browser smoke |
| AC-015 | 已登记当前发布且数据库最终态通过 | 滚动启用两个实例 | 每个实例依次通过启动、路由和日志脱敏检查；失败不推进下一实例 | takeover deployment evidence |
| AC-016 | 登录超级管理员与目标最终态 | 打开个人设置和侧栏 | 个人 OpenAPI 可用，系统管理出现 OpenAPI管理，系统监控出现 Nacos配置中心，无系统工具 | Playwright/人工浏览器验收 |

## 5. 范围

### IN

- NAMEWTA append-only DDL/DML 收敛块及其 Java/MySQL 合同测试。
- 发布 Compose、公开 env 示例和 release 静态测试中的 OpenAPI 配置透传。
- 目标开发环境按用户无备份决定执行数据库迁移、双实例显式启用和真实验收。
- 必要的发布/升级说明和 SpecDev Evidence。

### REUSE

- 既有 `ruoyi-common-openapi` 条件装配、startup validator、system Controller 和前端双入口。
- 既有 system web-domain manifest、Admin 动态路由恢复和 external monitor iframe。
- 固定菜单 ID、现有 permissions 和 release takeover 流程。
- `<Path>{roots.state}/specdev/changes/2026-09-01-admin-runtime-capability-reconciliation/diagnostics/</Path>` 中的红绿探针。

### OUT

- **OOS-001**：不修改 OpenAPI wire contract、数据库凭据模型或业务授权算法。
- **OOS-002**：不自动给普通角色授权；角色分配继续由管理员管理。
- **OOS-003**：不修改 Nacos Server、配置正文、账号或登录边界。
- **OOS-004**：不操作 CDE、生产环境、DNS、TLS、防火墙或远端 Git。
- **OOS-005**：不恢复生成器表、数据、API、页面或历史配置。

## 6. 已锁定实现约束

- **DEC-001**：OpenAPI 代码与样例默认关闭，目标环境显式启用。来源：`ADR-001`。
- **DEC-002**：只通过新的 append-only 尾部块收敛 fresh/upgrade，不修改历史 SQL。来源：`ADR-002`。
- **DEC-003**：菜单显示固定为“OpenAPI管理”和“系统监控 > Nacos配置中心”，稳定 component/permission 不变。来源：`ADR-003`。
- **DEC-004**：生成器数据库面在 0 行与身份校验通过后物理删除，不保留兼容层。来源：`ADR-004`、`ADR-005`。
- **DEC-005**：两个后端实例使用同一 KEK/version；KEK 只能进入被忽略且 `0600` 的部署 secret 配置，不写入 SpecDev/Evidence/终端输出。
- **DEC-006**：用户明确豁免本次目标开发库备份；外部写入前仍必须确认目标、0 行、对象身份与菜单冲突，失败只前向修复；不使用 `docker compose down -v`。来源：`ADR-005`。

## 7. 数据、接口与兼容

- **公共接口变化：** 无。`/system/openApi/**` 和签名调用路径保持原合同；变化是目标环境从未装配转为显式装配。
- **数据模型与持久化：** 删除已退役且当前为空的 `gen_table_column`、`gen_table`；收敛固定 `sys_menu`/`sys_role_menu` 行；不改变 OpenAPI 凭据表结构。
- **兼容要求：** 未显式配置的安装保持 default-off；前端 component key 与 permission 不变；Nacos URL/独立登录不变。
- **迁移要求：** fresh 固定执行完整 DDL 后 DML；已有环境只执行本 change 新块。新块可重放，并在冲突状态下先失败。
- **发布或运维影响：** 目标开发环境需要私密 KEK、无备份风险确认、逐实例重建和菜单/会话刷新；上一活动发布与实例回滚命令必须在执行前登记。

## 8. 非功能要求

- **NFR-001 安全与隐私：** KEK、AppSecret、Token、数据库凭据不进入 Git、Speculo、浏览器、日志或命令输出；迁移不扩大普通角色授权。
- **NFR-002 性能与容量：** 不新增运行查询或轮询；菜单操作固定小集合，迁移不得扫描或删除 pattern 匹配的非目标数据。
- **NFR-003 可用性与可靠性：** 双实例逐个滚动；第一个实例未验证不得推进第二个；新 SQL 重放保持最终态。
- **NFR-004 可观测性与运营：** 验收记录实例启动、路由状态、数据库最终态和脱敏日志；配置失败保留现场并报告配置键，不报告值。

## 9. 验证策略

| 接缝 | 层级 | 覆盖合同 | 现有先例或命令 | Evidence 类型 |
|---|---|---|---|---|
| OpenAPI assembly | Spring context/MockMvc | AC-001..004 | `*OpenApiAssembly*` | Maven test report |
| SQL 静态合同 | Java unit | AC-005..013 | OpenAPI/Nacos/menu retirement contract tests | Maven test report |
| fresh/upgrade/replay/conflict | MySQL integration | AC-005..011、AC-013 | external service MySQL tests | SQL query transcript |
| release env/Compose | Node static integration | AC-012、AC-015 | `<Path>release-artifacts/tests/release-config.test.mjs</Path>` | Node test report |
| Admin manifest/profile | Vitest | AC-014 | Admin manifest/profile tests | Vitest report |
| 实际双实例 | HTTP + takeover | AC-003、AC-015 | 本 change OpenAPI route probe | status and sanitized logs |
| 实际菜单工作流 | Browser | AC-014、AC-016 | Admin Playwright/manual smoke | screenshot/assertion summary |
| 数据库最终态 | DBX/MySQL | AC-005..010、AC-013、AC-016 | 本 change runtime state SQL | result table |

## 10. 风险、假设与未决问题

### 风险

- 生成器表删除不可通过应用回滚恢复，且本次没有迁移前备份；失败只允许停止 rollout 并前向修复。
- 当前发布报告没有登记上一版本和精确回滚命令；这是部署前硬门。
- 两实例若使用不同 KEK，会造成凭据加解密不一致；配置必须同源并逐实例验证。
- 菜单缓存和已有登录会话可能暂时保留旧投影；迁移后需要按现有机制刷新或重新登录。

### 已采用的低影响假设

- 系统监控下 Nacos 使用 `order_num=8`，位于当前 AI 控制台之后；通过最终菜单排序查询验证。
- 现有生成器表在执行时仍为空；若变为非空，停止删除并重新取得数据处置决定。

### 未决问题

无。
