---
schema_version: 3
artifact: ticket
change: 2026-08-31-optional-nacos-dynamic-config
id: T-04
title: 在系统管理中提供 Nacos 官方控制台入口
status: done
planning_depth: deep
planning_depth_reason: 跨动态菜单持久数据、前端领域权限、manifest 和环境 URL 建立双权限边界，错误可能泄露管理入口或绕过前端授权。
ready: true
risk: high
blocked_by: []
contract_ids: [AC-015, AC-016, AC-022, AC-024]
owner: unassigned
expected_changes:
  - "<Path>plus-ui-namewta/packages/domains/system/src/monitor/**</Path>"
  - "<Path>plus-ui-namewta/apps/admin-web/src/views/monitor/external/index.vue</Path>"
  - "<Path>plus-ui-namewta/apps/admin-web/src/router/adminManifestRegistry.ts</Path>"
  - "<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DML.sql</Path>"
writable_paths:
  - "<Path>plus-ui-namewta/packages/domains/system/src/monitor/**</Path>"
  - "<Path>plus-ui-namewta/apps/admin-web/src/views/monitor/external/index.vue</Path>"
  - "<Path>plus-ui-namewta/apps/admin-web/src/router/adminManifestRegistry.ts</Path>"
  - "<Path>plus-ui-namewta/apps/admin-web/src/router/adminManifestRegistry.test.ts</Path>"
  - "<Path>plus-ui-namewta/apps/admin-web/src/types/env.d.ts</Path>"
  - "<Path>plus-ui-namewta/apps/admin-web/.env.development</Path>"
  - "<Path>plus-ui-namewta/apps/admin-web/.env.production</Path>"
  - "<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DML.sql</Path>"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/60-namewta-dml.sql</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/nacos/menu/**</Path>"
read_only_paths:
  - "<Path>plus-ui-namewta/apps/admin-web/src/views/monitor/external/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/script/sql/ry_vue.sql</Path>"
  - "<Path>release-artifacts/docker/frontend/nginx/**</Path>"
shared_paths: []
shared_path_owners: []
---

# Ticket T-04: 在系统管理中提供 Nacos 官方控制台入口

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/04-nacos-console-menu.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-04.md</Path>`

## 1. 战略与来源

- **目标：** 复用现有 external iframe 和动态菜单，为获授权管理员提供 Nacos 官方控制台入口，同时保持 Nacos 独立登录。
- **可观察产出：** 系统管理出现“配置中心”菜单；只有拥有 `system:nacos:console` 的用户能解析并进入 route；开发指向本机 Nacos，生产指向 `/nacos/`。
- **来源：** `US-004`、`US-005`、`AC-015`、`AC-016`、`AC-022`、`AC-024`、`ADR-007`、`ADR-008`。
- **当前事实：** 通用 external view 已承载 Monitor Admin、SnailJob、SnailAI；domain 中集中做 URL 安全和 target 权限映射，但 view 仍重复推导权限；动态 route 由 component key 与 manifest registration 对接。
- **Planning Depth 原因：** 菜单数据、route manifest 和权限映射必须一致，且入口授权不能被误当作 Nacos 配置授权。

## 2. 决策状态

### 已锁定决策

- target 固定为 `nacos`，component key 固定为 `monitor/nacos/index`，domain 权限固定为 `system:nacos:console`。
- 开发 URL 为 `http://localhost:8848/nacos/`，生产 URL 为根相对 `/nacos/`，环境键为 `VITE_APP_NACOS_ADMIN`。
- 菜单放在系统管理父菜单 `1761400000000000001` 下，固定 menu ID `2094360621561675790`、path `nacos`、类型 `C`；DML 使用不存在时插入/等价幂等方式。
- 不自动授予普通业务角色；超级管理员按现有平台规则可见，其他角色由管理员显式授权。
- 前端只决定入口可见与 URL 是否安全；iframe 中仍显示 Nacos 官方登录，不注入账号、密码、token、identity 或 access token。
- external view 从 domain 的单一 target-permission 映射取得权限，不再为 Nacos 增加第三份字符串推导规则。

### 已采用的低影响假设

- 菜单展示名使用“配置中心”，图标从现有 icon 集中选择，不新增自绘图标。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| Nacos target/permission/URL、manifest、通用 iframe、菜单 DML 与发布镜像、测试 | external monitor domain、dynamic route、系统管理父菜单 | Nacos CRUD、按钮权限、SSO、密码自动填写、Nginx 代理实现 |

## 4. 要构建什么

数据库增量加入系统管理下的配置中心菜单。后端返回动态菜单后，admin-web 用 manifest 把 `monitor/nacos/index` 映射到通用 external 页面；页面从 domain 取得 `system:nacos:console` 并校验当前用户和 URL。无权限或 URL 不安全时显示稳定失败状态，不创建 iframe；有权限时加载官方控制台 URL，并由 Nacos 自身完成登录与配置授权。

## 5. 实现契约

- **入口或接缝：** `ExternalMonitorTarget`/monitor service、admin manifest、external Vue view、sys_menu DML。
- **输入与输出：** target + env URL + RuoYi permission -> safe navigation intent/iframe 或稳定拒绝。
- **公共接口变化：** target union 增加 `nacos`，环境类型增加 `VITE_APP_NACOS_ADMIN`，持久权限增加 `system:nacos:console`。
- **不变量：** target、permission、component key 与 menu 唯一对应；不把 Nacos 登录状态当 RuoYi 登录状态。
- **状态或数据流：** sys_menu -> backend dynamic routes -> manifest component -> external intent -> iframe URL。
- **错误与失败行为：** 无权限、空 URL、危险协议/路径时不渲染 iframe并显示现有风格错误；不退回任意 URL。
- **兼容要求：** Monitor Admin、SnailJob、SnailAI targets、权限和 URL 行为不变；DML 重放不重复菜单。
- **安全与隐私要求：** 构建产物/DOM/URL 不含 Nacos secret；普通角色不自动获权。

## 6. 执行路线

1. 扩展 domain target/permission 与 URL 安全测试，先证明无权和危险 URL 拒绝。
2. 注册 admin manifest 和环境类型/默认 URL，使通用 external view 使用单一权限映射。
3. 追加固定、幂等 sys_menu DML，并同步 release 的 60-namewta-dml 镜像。
4. 增加 menu/component/permission/DML 镜像一致性合同测试。
5. 运行 domain Vitest、manifest 测试、typecheck/lint/build 和 SQL 合同回归。

## 7. 路径访问契约

- **预计修改点/可写范围：** frontmatter 所列 monitor domain、admin registration/env、NAMEWTA DML/镜像及测试。
- **只读上下文：** 现有 external 视图模式、上游 SQL 和 T-05 Nginx 接缝。
- **共享路径：** 无共同写路径；T-05 只读本 Ticket 的 `/nacos/` URL 合同。
- **保留或不动：** `ry_vue.sql`、Nacos Server、RuoYi 后端 Controller/API、普通角色授权。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常 | domain/manifest/menu contracts | 有权限并使用 dev/prod 合法 URL | target 映射正确且 iframe intent 成立 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-04.md</Path>` |
| 失败 | permission/URL tests | 无权限、空值、javascript/越界 URL | 不创建 intent/iframe，稳定失败 | 同上 |
| 回归 | frontend build + DML replay | Vitest/typecheck/build 与隔离 MySQL 重放 DML | 旧 targets 不变，菜单唯一且镜像一致 | 同上 |

- **Workspace checks：** current workspace 串行运行 frontend domain/manifest 测试、lint/typecheck/build 和后端 SQL 合同测试。
- **E2E disposition：** not-required：真实 iframe/同源代理与独立登录由 T-05 统一浏览器验证；本 Ticket 由权限和 manifest 合同闭环。
- **E2E owner/environment：** Lead / current-workspace。
- **Integration evidence：** backend/frontend 子模块 implementation commit、父仓 gitlink/direct-parent before/result SHA 和测试报告。

## 9. 发布、迁移与恢复

- **迁移顺序：** 前端可识别 component 后追加菜单；发布环境在 T-05 代理可用后再授予非超级管理员。
- **兼容窗口：** 菜单存在但 Nacos 未部署时显示入口不可用/连接失败，不影响其他系统管理页面。
- **监控信号：** 前端安全拒绝、Nginx upstream 状态和 Nacos 登录审计；不采集凭据。
- **回滚或前向恢复：** 先撤销角色授权/隐藏菜单，再回退前端注册；固定 menu ID 便于显式删除，不能改上游 SQL。
- **不可逆操作与批准点：** 生产 DML 和角色授权未授权；实现仅更新脚本。
- **收缩条件：** 不适用：新增菜单，无旧入口迁移。

## 10. 验收标准

- [x] AC-015/016/022/024 的权限、URL、独立登录边界和幂等菜单合同通过。
- [x] `system:nacos:console` 是唯一 RuoYi 入口权限，未新增 `/system/nacos` API 或配置编辑器。
- [x] 旧 external targets 与构建不回归，DML 与发布镜像一致。
- [x] Evidence 与 backend/frontend implementation/direct-parent 结果完整。
