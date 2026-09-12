---
schema_version: 3
artifact: tickets-map
change: 2026-08-31-optional-nacos-dynamic-config
status: completed
---

# Tickets Map: 可选 Nacos 动态配置

- **Map：** `<Path>{roots.state}/specdev/changes/2026-08-31-optional-nacos-dynamic-config/tickets-map.md</Path>`
- **Spec：** `<Path>{roots.state}/specdev/changes/2026-08-31-optional-nacos-dynamic-config/spec.md</Path>`
- **Ticket 目录：** `<Path>{roots.state}/specdev/changes/2026-08-31-optional-nacos-dynamic-config/ticket/</Path>`
- **Evidence 目录：** `<Path>{roots.state}/specdev/changes/2026-08-31-optional-nacos-dynamic-config/evidence/</Path>`
- **Goal Plan：** `<Path>{roots.state}/specdev/changes/2026-08-31-optional-nacos-dynamic-config/goal-plan.md</Path>`

## 1. 目标与拆分策略

六个 Ticket 共同交付 `US-001` 至 `US-007`：先由单一 owner 建立共享 Nacos runtime，再接入三组即时生效消费者；基础设施、系统菜单与同源代理各自形成可观察垂直切片；最后以真实 MySQL、Nacos、双 ruoyi-admin、Nginx 和浏览器收敛验证全部 24 个合同。

没有为了目录整洁创建 prefactor。T-01 是必要的 shared-contract preparation，解除 T-02/T-03 对配置键、SDK 生命周期和刷新协议的阻塞；T-06 是集成 Gate，不在其中越界修复前置实现。不存在旧协议替换，因此不使用 expand-contract。

用户已明确要求 Spec 后连续生成 Tickets 与 Goal Plan，并确认采用已收敛方案；本次按该授权自主发布拆分，不再为无未决问题的粒度重复提问。

## 2. 执行清单

| ID | Ticket | 可观察产出 | Blocked By | Depth | Risk | Ready | Owner | Contract IDs | Wave/Gate | Status |
|---|---|---|---|---|---|---|---|---|---|---|
| T-01 | `<Path>{roots.state}/specdev/changes/{change}/ticket/01-nacos-config-runtime.md</Path>` | 默认关闭且可降级的稀疏覆盖、原子状态与实例观测 | — | deep | high | yes | Lead | AC-001..009, AC-011..014, AC-022..023 | W1 / G1 / 1 | done |
| T-04 | `<Path>{roots.state}/specdev/changes/{change}/ticket/04-nacos-console-menu.md</Path>` | 系统管理菜单、权限和安全 external target | — | deep | high | yes | Lead / dynamic dispatch | AC-015, AC-016, AC-022, AC-024 | W1 / G1 / 2 | done |
| T-02 | `<Path>{roots.state}/specdev/changes/{change}/ticket/02-safe-live-refresh.md</Path>` | Captcha、Notify、OSS 三组配置即时生效 | T-01 | deep | high | yes | Lead / dynamic dispatch | AC-009..013, AC-022 | W2 / G2 / 3 | done |
| T-03 | `<Path>{roots.state}/specdev/changes/{change}/ticket/03-nacos-docker-infrastructure.md</Path>` | 固定镜像、鉴权、独立 DB、持久化和 optional override | T-01 | deep | high | yes | Lead / dynamic dispatch | AC-006, AC-018..021, AC-024 | W2 / G2 / 4 | done |
| T-05 | `<Path>{roots.state}/specdev/changes/{change}/ticket/05-nacos-same-origin-proxy.md</Path>` | `/nacos/` 同源 iframe 与官方独立登录 | T-03, T-04 | standard | medium | yes | Lead / dynamic dispatch | AC-016, AC-017, AC-022 | W3 / G3 / 5 | done |
| T-06 | `<Path>{roots.state}/specdev/changes/{change}/ticket/06-nacos-release-convergence.md</Path>` | 全链路双实例、故障与发布验收门禁 | T-02, T-03, T-05 | deep | high | yes | Lead / dynamic dispatch | AC-001..024 | W4 / G4 / 6 | done |

Ticket frontmatter 是状态、依赖、深度和路径访问契约的权威；本表是同步投影。

## 3. 依赖 DAG

```text
T-01 [runtime contract]
  ├─→ T-02 [three live-refresh behaviors] ──────────────┐
  └─→ T-03 [official Nacos infrastructure] ──┐          │
                                             ├─→ T-05 ─┼─→ T-06 [final convergence gate]
T-04 [menu + external target] ───────────────┘          │
                                                        ┘
```

- 根 Ticket：T-01、T-04。
- 分叉：T-01 完成后 T-02 与 T-03 没有可写路径交集。
- 第一次汇合：T-05 需要 T-03 的服务名/network 与 T-04 的 `/nacos/` URL/iframe 合同。
- 最终汇合：T-06 只读所有产品实现，按失败归属退回原 owner Ticket。
- DAG 无环，所有边都是开始所需的公共合同或真实运行依赖。

## 4. 合同覆盖矩阵

| Contract ID | 覆盖 Ticket | 验证接缝 | 状态 | 说明 |
|---|---|---|---|---|
| AC-001 | T-01, T-06 | Spring 启动、base stack | covered | 默认关闭无客户端副作用 |
| AC-002 | T-01, T-06 | PropertySource、真实发布 | covered | 稀疏覆盖保留本地键 |
| AC-003 | T-01, T-06 | 优先级测试、真实 env | covered | 部署层优先 |
| AC-004 | T-01, T-06 | 校验器、真实拒绝 | covered | 保护键整份拒绝 |
| AC-005 | T-01, T-06 | profile/namespace 订阅 | covered | local/dev/prod 隔离 |
| AC-006 | T-01, T-03, T-06 | 无服务启动、容器故障 | covered | 空/不可达本地启动 |
| AC-007 | T-01, T-06 | 临时 snapshot、离线重启 | covered | 不读持久快照 |
| AC-008 | T-01, T-06 | 断连 fault injection | covered | 运行保留上一有效 |
| AC-009 | T-01, T-02, T-06 | 删除监听、业务调用 | covered | 删除回本地 |
| AC-010 | T-02, T-06 | Captcha/Notify/Oss 行为 | covered | 三组即时生效 |
| AC-011 | T-01, T-02, T-06 | 分类与启动行为 | covered | 清单外等待重启 |
| AC-012 | T-01, T-02, T-06 | 混合文档 | covered | 分类而非误报 |
| AC-013 | T-01, T-02, T-06 | 两阶段原子拒绝 | covered | 非法版本无部分更新 |
| AC-014 | T-01, T-06 | 双实例 digest | covered | 独立订阅与定位 |
| AC-015 | T-04, T-06 | menu/domain permission | covered | 无权不可进入 |
| AC-016 | T-04, T-05, T-06 | external intent、Playwright | covered | 官方登录，无 SSO |
| AC-017 | T-05, T-06 | Nginx/浏览器 | covered | `/nacos/` 同源资源完整 |
| AC-018 | T-03, T-06 | Compose/real container | covered | 固定镜像、DB、auth、health |
| AC-019 | T-03, T-06 | 缺变量、secret scan | covered | 无仓库默认 secret |
| AC-020 | T-01, T-03, T-06 | base/override compose | covered | 可选启用与健康门 |
| AC-021 | T-03, T-06 | 容器重启/MySQL 查询 | covered | 配置持久化 |
| AC-022 | T-01..T-06 | 日志、状态、DOM、产物扫描 | covered | 全链路不回显 |
| AC-023 | T-01, T-06 | Maven full/core | covered | 两 bundle 兼容 |
| AC-024 | T-03, T-04, T-06 | fresh/existing MySQL、DML replay | covered | schema 与菜单幂等 |

无 `uncovered` 或 `deferred` 合同。

## 5. 并行与路径所有权

- implementation agent 配置上限为 3，不含 Lead；Goal Plan 若采用 current workspace，实际 implementation writer 上限降为 1。
- review/research/test-observation agent 只读，不修改 Ticket writable paths。
- 每个可写路径只出现在一个 Ticket；T-01 是 common/POM owner，T-03 是 Compose/Nacos DB owner，T-04 是菜单/frontend registration owner，T-05 是 LB owner，T-06 是最终 harness/docs owner。
- Lead 是 SpecDev 状态、父分支 integration 与 required E2E owner。

| Ticket A | Ticket B | Writable 交集 | 真实依赖 | 处理 |
|---|---|---|---|---|
| T-01 | T-04 | 无 | 否 | required-worktree 模式可并行；current 模式串行 |
| T-02 | T-03 | 无 | 否，均只依赖 T-01 | required-worktree 模式可并行；current 模式串行 |
| T-03 | T-04 | 无 | 否 | 可并行但 T-05 等待两者 |
| T-03 | T-05 | 无 | 是 | T-05 只读 T-03 合同并在其后执行 |
| T-04 | T-05 | 无 | 是 | T-05 只读 T-04 合同并在其后执行 |
| T-01..T-05 | T-06 | 无 | 是 | T-06 只读并在最终父工作区执行 |

## 6. Gate、Wave 与集成点

- **Gate G0：** implementation commit、direct-parent 与 clean current baseline 均获授权/固定；未关闭前不得改产品。
- **Wave 1：** T-01、T-04，current 模式依序 1、2 串行；G1 关闭 backend runtime 与 console route 合同。
- **Wave 2：** T-02、T-03，current 模式依序 3、4 串行；G2 关闭三域原子行为与 real Docker auth/persistence/health。
- **Wave 3：** T-05；G3 在统一入口运行 Nginx + Playwright required E2E。
- **Wave 4：** T-06；G4 在最终 parent current workspace 串行运行所有构建、真实双实例故障矩阵和 secret scan。

Goal Plan 已选择 `current` + `direct-parent`，固定串行顺序 `T-01 -> T-04 -> T-02 -> T-03 -> T-05 -> T-06`；六个 Ticket 与最终 G4 均已关闭。

## 7. 横切契约与风险

- 远程配置始终是本地 YAML 上的可选稀疏覆盖，不能删减本地基线。
- 配置接受以整份候选为原子单位；只有三组清单配置即时生效，其他合法键等待重启。
- Nacos 数据库/备份/控制台按 secret 系统治理；明文静态存储风险必须在文档中直说。
- RuoYi 权限只控制菜单入口；Nacos 账号控制配置操作。无 SSO、无凭据注入、无自建 CRUD。
- 固定 `nacos/nacos-server:v2.5.4` standalone 不代表生产 HA；生产执行、角色授权和卷操作不在实现授权内。
- 当前 workspace 已有用户改动；任何实施必须在 Goal Plan execution gate 先解决干净提交边界，不能把无关改动带入 Ticket commit。

## 8. 同步规则

- Ticket 状态变化后同步执行清单；frontmatter 为权威。
- Goal Plan 建立后，Wave、Gate、owner 和 workspace 以 Goal Plan 为编排权威并回投本 Map。
- 依赖、合同覆盖或路径所有权变化后重新运行 tickets validator。
- required E2E 只能由 Lead 在 current workspace 或 parent-candidate 运行，不能在隔离 source worktree 声称完成。
- 内部工件只使用完整根变量 Path 标签。
