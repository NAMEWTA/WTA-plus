---
schema_version: 3
artifact: ticket
change: 2026-09-08-richtext-third-oss
id: T-02
title: 三方调用明细和统计支持空 providerCode
status: done
planning_depth: deep
planning_depth_reason: 这是对公开 GET 接口查询语义的修订，controller/service/mapper 必须一致，且要兼容现有页面直接打开的行为。
ready: true
risk: medium
blocked_by: []
contract_ids: [AC-002]
owner: grok-build
expected_changes:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/controller/admin/ThirdObservabilityController.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/service/ThirdObservabilityService.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/mapper/ThirdInvocationMapper.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/mapper/ThirdStatisticMapper.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/dao/ThirdInvocationDao.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/dao/ThirdStatisticDao.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/usecase/impl/ThirdObservabilityUseCaseImpl.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/test/java/org/dromara/third/controller/admin/ThirdObservabilityControllerContractTest.java</Path>"
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/controller/admin/ThirdObservabilityController.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/service/ThirdObservabilityService.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/mapper/ThirdInvocationMapper.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/mapper/ThirdStatisticMapper.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/dao/ThirdInvocationDao.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/dao/ThirdStatisticDao.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/usecase/impl/ThirdObservabilityUseCaseImpl.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/test/java/org/dromara/third/controller/admin/ThirdObservabilityControllerContractTest.java</Path>"
read_only_paths:
  - "<Path>plus-ui-namewta/packages/web-domains/third/src/ThirdPage.vue</Path>"
  - "<Path>plus-ui-namewta/apps/admin-web/src/utils/ruoyi.ts</Path>"
  - "<Path>plus-ui-namewta/e2e/third-party-management.spec.ts</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-web/src/main/java/org/dromara/common/web/handler/GlobalExceptionHandler.java</Path>"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/50-namewta-ddl.sql</Path>"
shared_paths: []
shared_path_owners: []
---

# Ticket T-02: 三方调用明细和统计支持空 providerCode

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-09-08-richtext-third-oss/ticket/02-third-observability-optional-provider-code.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-09-08-richtext-third-oss/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-09-08-richtext-third-oss/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-09-08-richtext-third-oss/evidence/T-02.md</Path>`

实现本 Ticket 时，Lead 与 implementation subagent 必须按顺序完整读取总体 Map、其中适用于 `ALL`/`T-02` 的项目 Skill，再读取本 Ticket 与其他上游工件。

## 1. 战略与来源

- **目标：** 让调用明细和调用统计页在不输入 `providerCode` 时也能打开并显示最近数据。
- **可观察产出：** `GET /third/invocation/list` 和 `GET /third/statistics/list` 在缺省筛选时返回 200，不再由空参数触发 500。
- **来源：** `AC-002`、`DIAG-2026-09-08-richtext-third-oss`、`CODE:<Path>plus-ui-namewta/packages/web-domains/third/src/ThirdPage.vue</Path>`、`CODE:<Path>plus-ui-namewta/apps/admin-web/src/utils/ruoyi.ts</Path>`、`CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/controller/admin/ThirdObservabilityController.java</Path>`
- **当前事实：** 前端会把空字符串从 query 参数里剥掉，而 controller 当前把 `providerCode` 当作必填参数；实际调用链还经过 `ThirdObservabilityUseCaseImpl`，不能只改 controller。
- **Planning Depth 原因：** 这是对公开 GET 接口查询语义的修订，controller/service/mapper 必须同步，并保持现有页面直开行为。

## 2. 决策状态

### 已锁定决策

- 空 `providerCode` 解释为“不过滤 provider，返回最近数据”。
- 有值时仍保留 provider 过滤。
- 现有页面的输入框仍可作为可选过滤器使用。

### 已采用的低影响假设

- 调用明细/统计表中最近数据的排序和上限沿用既有实现。
- 现有 Playwright 管理页仍然是主回归入口。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| controller 参数语义、service 空值归一、mapper 动态条件、契约测试 | 现有调用明细/统计页面、现有权限模型、现有统计表 | 改第三方供应商模型、放开写口、重做前端筛选布局 |

## 4. 要构建什么

用户直接打开调用明细页或调用统计页时，不需要先手工输入 `providerCode`。系统应该把空筛选解释成“看最近数据”，把有值筛选解释成“只看这个 provider”。页面还是同一个页面，查询框仍然可用，只是空值不再导致请求失败。

## 5. 实现契约

- **入口或接缝：** `ThirdObservabilityController`、`ThirdObservabilityUseCaseImpl`、`ThirdObservabilityService`、两个 DAO 和两个 Mapper 的查询入口/SQL 条件。
- **输入与输出：** 输入是可选 `providerCode`；输出是调用明细或统计列表。
- **公共接口变化：** `providerCode` 变成可选查询语义，空值表示不过滤。
- **不变量：** 有值时仍需按 provider 过滤；调用明细/统计仍然只读。
- **状态或数据流：** controller 先接收可选参数；use case/service 统一 `null`、空串、全空白并 `strip()`；DAO 透传归一值；Mapper 使用 XML/Provider 动态 SQL，仅在非空时拼接 provider 条件。
- **错误与失败行为：** 空值不得再触发 `MissingServletRequestParameterException` / 泛化 500。
- **兼容要求：** 已有带 providerCode 的请求继续可用。
- **安全与隐私要求：** 不新增写口，不暴露更多敏感调用明细。

## 6. 执行路线

1. 把 controller 的请求参数语义改成可选，并在 service 层统一归一空字符串。
2. 让 mapper 在 providerCode 为空时跳过 provider 条件，保持同一排序和上限。
3. 增加一个 controller / service 级回归测试，验证空筛选能直出 200。
4. 重跑现有 Playwright 管理页回归，确认页面直接打开不再炸。

## 7. 路径访问契约

- **预计修改点：** 与 `expected_changes` 对齐，仅作导航。
- **可写范围：** 与 `writable_paths` 对齐；越界前必须停止。
- **只读上下文：** 与 `read_only_paths` 对齐。
- **共享路径：** 无。
- **保留或不动：** 三方供应商、Endpoint、Credential、权限与页面布局。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | controller + service | `mvn -pl ruoyi-modules/ruoyi-third -am -Dtest=ThirdObservabilityControllerContractTest test` | 传入 providerCode 时返回原有过滤结果 | `<Path>{roots.state}/specdev/changes/2026-09-08-richtext-third-oss/evidence/T-02.md</Path>` |
| 失败路径 | controller | 不传 providerCode 调用 `/third/invocation/list` 和 `/third/statistics/list` | 返回 200，不再进入 500 | `<Path>{roots.state}/specdev/changes/2026-09-08-richtext-third-oss/evidence/T-02.md</Path>` |
| 回归 | 浏览器页 | `pnpm exec playwright test e2e/third-party-management.spec.ts` | 调用明细/统计页直接可打开并展示最近数据 | `<Path>{roots.state}/specdev/changes/2026-09-08-richtext-third-oss/evidence/T-02.md</Path>` |

- **Workspace checks：** current-workspace，先跑后端单测，再跑现有 Playwright 回归。
- **E2E disposition：** required: 这是对现有管理页的可见行为修复，必须回归页面直开。
- **E2E owner/environment：** Lead / current-workspace；场景是直接访问调用明细和调用统计页。
- **Integration evidence：** implementation commit、direct-parent 验证结果和父分支 result SHA 在完成时写入 Evidence；当前 draft 阶段仅保留计划，不伪造 SHA。

## 9. 发布、迁移与恢复

- **迁移顺序：** 不适用：无数据迁移。
- **兼容窗口：** 现有带 providerCode 的请求持续兼容。
- **监控信号：** 这两个接口的 500 和 missing-parameter 错误应消失。
- **回滚或前向恢复：** 回滚时恢复必填参数语义和原有 SQL 条件。
- **不可逆操作与批准点：** 无。
- **收缩条件：** 不适用：没有旧协议收缩。

## 10. 验收标准

- [ ] `AC-002`：调用明细/统计页在空 providerCode 下返回 200 并展示最近数据。
- [ ] `null`、空串、全空白和带首尾空格的参数均有契约测试；有值过滤结果保持不变。
- [ ] 实现开始前已完整读取 Tickets Map 及其中适用于 `ALL`/`T-02` 的项目 Skill。
- [ ] 验证矩阵全部执行并记录到 `<Path>{roots.state}/specdev/changes/2026-09-08-richtext-third-oss/evidence/T-02.md</Path>`。
- [ ] 实际项目修改未超出 `writable_paths`。
- [ ] Ticket、Tickets Map 和 Evidence 状态一致。
