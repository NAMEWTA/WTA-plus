---
schema_version: 3
artifact: ticket
change: 2026-08-31-optional-nacos-dynamic-config
id: T-05
title: 通过 Nginx 同源代理加载 Nacos 控制台
status: done
planning_depth: standard
planning_depth_reason: 修改 HTTP/TLS 统一入口并验证 iframe、静态资源、登录和安全响应头，范围集中但跨浏览器与容器边界。
ready: true
risk: medium
blocked_by: [T-03, T-04]
contract_ids: [AC-016, AC-017, AC-022]
owner: unassigned
expected_changes:
  - "<Path>release-artifacts/docker/frontend/nginx/lb/nginx-lb-http.conf.template</Path>"
  - "<Path>release-artifacts/docker/frontend/nginx/lb/nginx-lb-tls.conf.template</Path>"
  - "<Path>plus-ui-namewta/e2e/nacos-console.spec.ts</Path>"
writable_paths:
  - "<Path>release-artifacts/docker/frontend/nginx/lb/nginx-lb-http.conf.template</Path>"
  - "<Path>release-artifacts/docker/frontend/nginx/lb/nginx-lb-tls.conf.template</Path>"
  - "<Path>release-artifacts/tests/nacos-proxy-config.test.mjs</Path>"
  - "<Path>plus-ui-namewta/e2e/nacos-console.spec.ts</Path>"
read_only_paths:
  - "<Path>release-artifacts/docker/docker-compose-infrastructure.yml</Path>"
  - "<Path>release-artifacts/docker/docker-compose-frontend.yml</Path>"
  - "<Path>plus-ui-namewta/apps/admin-web/src/views/monitor/external/index.vue</Path>"
  - "<Path>plus-ui-namewta/packages/domains/system/src/monitor/**</Path>"
shared_paths: []
shared_path_owners: []
---

# Ticket T-05: 通过 Nginx 同源代理加载 Nacos 控制台

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/05-nacos-same-origin-proxy.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-05.md</Path>`

## 1. 战略与来源

- **目标：** 让生产 admin-web 在同一浏览器 origin 下完整加载官方 Nacos 控制台，同时保持独立登录和现有入口安全策略。
- **可观察产出：** HTTP/TLS LB 的 `/nacos/` 可返回登录页及其静态资源；系统菜单 iframe 非空、无资源路径错误，并要求 Nacos 登录。
- **来源：** `US-004`、`US-005`、`AC-016`、`AC-017`、`AC-022`、`ADR-007`、`ADR-011`。
- **当前事实：** 两份 LB 模板已同源代理 `/snail-job/`、`/snail-ai/`，所有容器共享 external network；生产前端 URL 已由 T-04 固定为 `/nacos/`。
- **Planning Depth 原因：** 需要同时验证 Nginx 路径保留、官方前端资源、iframe 响应头和 HTTP/TLS 对称性。

## 2. 决策状态

### 已锁定决策

- 两份 LB 均增加 `nacos_server` upstream，容器内目标为 `nacos:8848`，`/nacos/` 原样传递给上游 `/nacos/`。
- 复用标准 forward headers 和合理超时；不向请求注入 Nacos 凭据或伪造登录 cookie。
- 不通过删除所有 frame/CSP 安全头允许跨域嵌入；仅保证同源 iframe，任何必要的响应头调整必须精确限制在 `/nacos/` 并由浏览器测试证明。
- HTTP 与 TLS 模板保持行为一致；根路径、actuator 拒绝和其他 external route 不变。

### 已采用的低影响假设

- Nacos 2.5.4 的 `/nacos/` context path 保持官方默认，不改 Server context path。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| LB upstream/location、静态配置测试、真实 iframe 登录页 E2E | Snail external proxy headers、shared network、T-04 iframe | SSO、跨域嵌入、Nacos 配置发布逻辑、直接 app-nginx 端口代理 |

## 4. 要构建什么

浏览器从统一 admin-web origin 打开配置中心时，iframe 请求 `/nacos/`，LB 在共享 Docker network 中转发到官方 Nacos context path。HTML、脚本、样式和 API 保持同源并加载完成；页面显示官方登录，未授权 API 仍被 Nacos 拒绝。HTTP 与 TLS 入口表现一致，Nacos 不可达时由现有上游错误机制呈现，不泄露内部地址或凭据。

## 5. 实现契约

- **入口或接缝：** HTTP/TLS LB `/nacos/`、admin external iframe、Playwright network/DOM。
- **输入与输出：** same-origin GET/API/WebSocket-like requests -> Nacos response；无登录 -> 官方登录/未授权。
- **公共接口变化：** 发布入口增加根相对 `/nacos/`。
- **不变量：** path 不被错误剥离；无凭据注入；HTTP/TLS 对称；其他 location 优先级不变。
- **状态或数据流：** browser -> LB -> `nacos:8848/nacos/` -> browser same origin。
- **错误与失败行为：** upstream 不健康产生现有维护/错误响应；资源 404、frame blocked、混合内容或跨域请求均使测试失败。
- **兼容要求：** admin、snail-job、snail-ai、API 和 actuator location 回归通过。
- **安全与隐私要求：** 不把 `Access-Control-Allow-Origin: *`、账号或 token 注入 response/request；保留同源防护。

## 6. 执行路线

1. 先增加 HTTP/TLS 对称、path-preserving 和无凭据注入的 Nginx 静态测试。
2. 在两份 LB 模板增加相同 upstream/location 与 headers。
3. 启动 T-03 Nacos 与 frontend LB，验证直接 `/nacos/` 登录页和全部关键资源。
4. 以有/无 RuoYi 菜单权限执行 Playwright，验证 iframe、官方登录、无 console/network 错误。
5. 回归现有 external route、actuator 拒绝、HTTP 和 TLS config。

## 7. 路径访问契约

- **预计修改点/可写范围：** 两份 LB 模板、专用 proxy static test 和 Nacos Playwright spec。
- **只读上下文：** T-03 service name/network 与 T-04 target/URL；不修改其合同。
- **共享路径：** 无共同写路径。
- **保留或不动：** app nginx、frontend Compose、Nacos auth 设置、其他 route。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常 | real LB + Playwright | 有权用户打开菜单，等待 iframe 和登录 UI | 同源加载完整且显示官方登录 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-05.md</Path>` |
| 失败 | auth/upstream/browser | 无权用户、匿名 Nacos API、停止 Nacos | 不建 iframe或明确拒绝/错误，无 secret/内网地址泄露 | 同上 |
| 回归 | Nginx static + existing routes | node test、HTTP/TLS config、Snail/actuator smoke | 两模板对称，旧入口不回归 | 同上 |

- **Workspace checks：** current workspace 先运行静态 test、frontend lint/typecheck/build 和 `nginx -t` 等价容器检查。
- **E2E disposition：** required：iframe frame policy、同源资源和官方登录必须由真实 LB/Nacos/浏览器证明。
- **E2E owner/environment：** Lead / current-workspace；使用隔离 Docker 与测试账号。
- **Integration evidence：** parent before、backend/frontend/release implementation commits、direct-parent result SHA、Playwright screenshot/network/console 结果。

## 9. 发布、迁移与恢复

- **迁移顺序：** Nacos healthy -> 部署 LB route -> 部署前端 URL/menu -> 授予权限。
- **兼容窗口：** route 可先于菜单存在；未授权用户无法从 RuoYi 导航进入，Nacos 仍要求登录。
- **监控信号：** `/nacos/` upstream status、资源 4xx/5xx、frame/console 错误和 Nacos 登录审计。
- **回滚或前向恢复：** 先撤销菜单权限，再删除 LB location；Nacos 服务和应用客户端可独立保留。
- **不可逆操作与批准点：** 无生产部署授权；实现只改模板和测试。
- **收缩条件：** 不适用：新增同源路径，无旧路径替换。

## 10. 验收标准

- [ ] AC-016/017/022 的同源 iframe、独立登录、资源完整与不泄密通过真实浏览器。
- [ ] HTTP/TLS 模板对称，旧 external/API/actuator 路由回归通过。
- [ ] required E2E、截图/网络证据、implementation commit 和 direct-parent result 完整。
