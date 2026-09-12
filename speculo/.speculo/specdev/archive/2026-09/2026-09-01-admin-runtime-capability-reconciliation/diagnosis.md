---
schema_version: 1
artifact: diagnosis
change: 2026-09-01-admin-runtime-capability-reconciliation
status: root-cause-confirmed
feedback_loop_ready: true
red_command: "bash <Path>{roots.state}/specdev/changes/2026-09-01-admin-runtime-capability-reconciliation/diagnostics/openapi-route-check.sh</Path> http://172.16.105.9:48080"
red_evidence: '目标后端返回 {"code":404,"data":null,"msg":"请求地址不存在"} 且命令退出 1；数据库四项最终态检查均为 0'
cleanup_status: clean
updated_at: 2026-09-01T15:22:59+08:00
---

# Diagnosis: Admin 运行能力与菜单状态未收敛

## 1. 现象与影响

- NAMEWTA 开发环境的两个后端实例请求 `GET /system/openApi/self/interfaces` 均返回业务 `404`，个人设置 OpenAPI 页面因此显示“当前未启用”。
- `ry-namewta.sys_menu` 没有 OpenAPI 管理菜单及五个权限子项；超级管理员也无法从系统管理进入管理页。
- Nacos 菜单实际名为“配置中心”，父菜单是“系统管理”；用户当前决定要求名称为“Nacos配置中心”并移动到“系统监控”。
- 后端和前端运行时代码生成器源码已经物理删除，但数据库仍有完整的九条系统工具/代码生成菜单以及 `gen_table`、`gen_table_column` 两张表，界面因此投影出无法解析的陈旧入口。
- 当前两张生成器表和 `sys_open_api_credential` 都是 0 行；本轮只读诊断未执行迁移、启用 OpenAPI 或修改角色授权。

## 2. 红灯反馈回路

- **命令：** `bash <Path>{roots.state}/specdev/changes/2026-09-01-admin-runtime-capability-reconciliation/diagnostics/openapi-route-check.sh</Path> http://172.16.105.9:48080`
- **至少一次真实输出：** `{"code":404,"data":null,"msg":"请求地址不存在"}`，随后输出 `RED: OpenAPI route is not assembled`，退出码 `1`。`48081` 返回相同结果。
- **精确症状断言：** 直接请求后端并断言响应不能是业务 `code=404`；这绕过前端、浏览器缓存与动态菜单。
- **耗时：** 小于 1 秒。
- **确定性/复现率：** 两个后端实例各 1/1，复现率 100%。
- **Agent 可运行性：** autonomous。
- **数据库伴随回路：** 用 DBX 在 `ry-namewta` 执行 `<Path>{roots.state}/specdev/changes/2026-09-01-admin-runtime-capability-reconciliation/diagnostics/runtime-capability-state.sql</Path>`；`openapi_menu_ok`、`nacos_menu_ok`、`gen_menu_removed_ok`、`gen_tables_removed_ok` 均为 `0`。

## 3. 最小复现

- **环境与输入：** NAMEWTA 开发服务器 `172.16.105.9`；直连任一后端实例；单个无凭据 GET。数据库检查只依赖 `ry-namewta` 的 `sys_menu` 与 `information_schema.tables`。
- **剩余步骤：** OpenAPI 只需请求一个条件装配 Controller 的路径；菜单只需执行一条聚合只读查询。
- **逐项删除证据：** 去掉前端和统一 Nginx 后，直连 `48080/48081` 仍为业务 404，排除前端路由、代理前缀和浏览器缓存；去掉角色过滤直接查 `sys_menu`，缺失/残留仍成立，排除当前用户菜单缓存。
- **最后红灯证据：** OpenAPI 探针退出 `1`；数据库四个布尔值为 `0/0/0/0`。
- **捕获物：** `<Path>{roots.state}/specdev/changes/2026-09-01-admin-runtime-capability-reconciliation/diagnostics/</Path>`。

## 4. 假设与证伪

| 排名 | 假设与预测 | 支持证据 | 单变量实验 | 结果 |
|---|---|---|---|---|
| 1 | OpenAPI 运行配置未启用；若 `openapi.enabled=true` 且 KEK/SPI/Redis 合法，组装会出现 | 应用默认 `${OPENAPI_ENABLED:false}`；Controller 与自动配置都受相同条件控制；发布 Compose 和 env 示例没有传入任何 `OPENAPI_*` | 执行 `OpenApiAssemblyContextTest` 的关闭/启用矩阵 | 确认：7/7 通过；默认无 Bean，合法启用时完整组装 |
| 2 | 既有环境没有执行相关 SQL 收敛块；若是，则会同时看到旧生成器数据、缺 OpenAPI 菜单和已单独应用的 Nacos 菜单 | 数据库恰为 9 个生成器菜单、2 张生成器表、0 个 OpenAPI 菜单、1 个 Nacos 菜单；源码 DDL/DML 已包含前三项历史块 | DBX 对固定 ID、表名和 Nacos ID 做最终态查询 | 确认：数据库是选择性迁移后的混合状态 |
| 3 | 前端没有 OpenAPI/Nacos component registration；若是，则数据库正确也无法解析页面 | 当前 manifest 注册 `system/openApi/index` 与 `monitor/nacos/index`，相关单测存在 | 读取当前 App 显式组合与 manifest 测试 | 排除：前端注册完整；错误在服务端启用和数据库投影之前 |
| 4 | 仅是当前用户角色或菜单缓存；若是，直连 Controller 应返回认证/授权失败，数据库目标行应存在 | 两个后端直连均为路由 404，数据库固定行缺失/残留可见 | 绕过前端并直接查询权威菜单表 | 排除：不是单一会话或缓存问题 |

## 5. 已确认根因

- **触发条件：** 使用当前开发发布配置启动后端，并在没有完整执行 NAMEWTA 待应用 SQL 块的既有数据库上登录 Admin。
- **失败机制：** OpenAPI 的代码合同本来就是默认关闭，且发布 Compose/环境模板没有把 `OPENAPI_ENABLED`、`OPENAPI_KEK_VERSION`、`OPENAPI_KEK` 注入两个后端，所以条件 Controller 不注册并返回路由 404。与此同时，既有数据库升级没有迁移版本账本或可执行 pending-block 门禁：OpenAPI 菜单和生成器退役块未应用，Nacos 菜单块被单独应用，形成混合状态。Nacos 的旧名称和位置则是前一 change 明确选择的合同，源码与数据库一致，但已被用户最新决定替代。
- **根因位置：** `<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/application.yml</Path>`、`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/openapi/</Path>`、`<Path>release-artifacts/docker/docker-compose-backend.yml</Path>`、`<Path>release-artifacts/.env.example</Path>`、`<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DDL.sql</Path>`、`<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DML.sql</Path>`。
- **漏检原因：** OpenAPI 原 change 的最终 Evidence 明确没有执行真实 MySQL/Redis、生产 DDL/DML、KEK 下发或 `OPENAPI_ENABLED=true`；Nacos 原 change 只验收了其新增入口，没有重新断言此前已完成 change 的数据库最终态；部署验收验证了服务健康和 Nacos 收敛，但没有把 OpenAPI 路由、OpenAPI 菜单、生成器退役最终态加入升级后门禁。
- **为何排除其他候选：** 当前后端 reactor 明确装配 `ruoyi-common-openapi`，启用态组装测试通过；前端 domain、web-domain、Admin service 与 manifest 均存在；直连后端和权威数据库查询消除了代理、浏览器与角色缓存变量。
- **确认实验：** `./mvnw -pl ruoyi-admin -am test -Dtest='*OpenApiAssembly*' -Dsurefire.failIfNoSpecifiedTests=false` 成功，7 tests、0 failure/error；真实后端关闭态探针稳定为红；DBX 最终态查询稳定为 `0/0/0/0`。

## 6. 修复契约

- **必须改变：** 保持应用代码默认关闭，但为受管发布补齐 OpenAPI 三个环境变量和私密 KEK 注入合同，在目标开发环境显式启用两个实例；按 PERSIST-006 只在 `DDL.sql`/`DML.sql` 末尾追加 upgrade/fresh 均可收敛的新块，不修改历史块；新 DDL 幂等删除两张生成器表；新 DML 以固定 ID 预检后删除九个生成器菜单/角色关系、补齐六个 OpenAPI 菜单权限并将主菜单命名为“OpenAPI管理”、把 Nacos 固定菜单改名为“Nacos配置中心”并移动到“系统监控”。
- **必须保持：** OpenAPI 在未显式配置时仍默认关闭且启用配置无效时启动失败；KEK 不进入 Git、Speculo、浏览器、日志或命令输出；OpenAPI permission/component/API 合同不变；Nacos 仍使用 `monitor/nacos/index`、`system:nacos:console` 和独立登录；不向普通角色自动授权；不恢复任何生成器源码、接口、页面或兼容别名；冻结的 `ry_vue.sql` 不变。
- **正确测试 seam：** `<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/openapi/assembly/OpenApiAssemblyContextTest.java</Path>`、`<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/openapi/credential/OpenApiCredentialSqlContractTest.java</Path>`、`<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/nacos/menu/NacosMenuContractUnitTest.java</Path>`、`<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/migration/BusinessMenuRetirementMySqlIntegrationTest.java</Path>`、`<Path>release-artifacts/tests/release-config.test.mjs</Path>` 以及本 change 的两个诊断探针。
- **回归测试：** 修复前真实 OpenAPI 路由探针退出 1、数据库四项为 0；修复并滚动启用后，无 Token 请求应到达认证/授权边界而不是业务 404，登录态个人目录返回成功；数据库四项必须全部为 1；动态菜单只显示“OpenAPI管理”和位于系统监控下的“Nacos配置中心”，系统工具/代码生成完全不存在。
- **OUT：** 不重构 OpenAPI 协议/凭据/授权实现，不改变 Nacos 配置内容与 SSO 边界，不重新设计菜单系统，不恢复生成器数据，不在本诊断 Work 执行数据库写入、部署或滚动重启。
- **风险与回滚：** 生成器表删除属于破坏性 schema 变更，即使当前为 0 行也必须先备份并验证；先应用向前 SQL 并验证，再以安全 secret 同时配置两个实例并逐个滚动。OpenAPI 启用失败时先恢复 `OPENAPI_ENABLED=false` 并回滚应用配置；菜单 DML 用固定 ID 前向补偿。部署前必须登记当前活动发布和可执行回滚命令。
- **推荐下游：** `S-spec` 后进入 `T-tickets`；本 change 涉及用户可见行为、破坏性 schema、secret 注入与双实例滚动，Planning Depth 使用 Deep，不直接进入 I。

## 7. 清理

- **原始回路重跑：** 已重跑；`48080` 与 `48081` 仍返回相同业务 404，数据库四项仍为 0，诊断解释全部原始症状。
- **`[DEBUG-...]` 搜索：** 未添加任何 debug 插桩。
- **一次性脚本/原型：** 两个只读诊断探针保留为后续修复前红/修复后绿验收资产。
- **未清理项 owner 与删除条件：** 无。
