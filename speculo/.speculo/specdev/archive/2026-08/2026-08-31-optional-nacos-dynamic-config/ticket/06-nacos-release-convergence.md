---
schema_version: 3
artifact: ticket
change: 2026-08-31-optional-nacos-dynamic-config
id: T-06
title: 固化 Nacos 双实例发布与故障验收
status: done
planning_depth: deep
planning_depth_reason: 汇合后端、前端、数据库、Nginx 与官方服务，覆盖双实例收敛、网络故障、持久化和 secret 边界，并形成可重复发布门禁。
ready: true
risk: high
blocked_by: [T-02, T-03, T-05]
contract_ids: [AC-001, AC-002, AC-003, AC-004, AC-005, AC-006, AC-007, AC-008, AC-009, AC-010, AC-011, AC-012, AC-013, AC-014, AC-015, AC-016, AC-017, AC-018, AC-019, AC-020, AC-021, AC-022, AC-023, AC-024]
owner: unassigned
expected_changes:
  - "<Path>release-artifacts/scripts/verify-nacos.sh</Path>"
  - "<Path>release-artifacts/tests/nacos-runtime.e2e.mjs</Path>"
  - "<Path>release-artifacts/README.md</Path>"
writable_paths:
  - "<Path>release-artifacts/scripts/verify-nacos.sh</Path>"
  - "<Path>release-artifacts/tests/nacos-runtime.e2e.mjs</Path>"
  - "<Path>release-artifacts/README.md</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-nacos/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-web/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-notify/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/**</Path>"
  - "<Path>plus-ui-namewta/apps/admin-web/**</Path>"
  - "<Path>plus-ui-namewta/packages/domains/system/**</Path>"
  - "<Path>release-artifacts/docker/**</Path>"
shared_paths: []
shared_path_owners: []
---

# Ticket T-06: 固化 Nacos 双实例发布与故障验收

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/06-nacos-release-convergence.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-06.md</Path>`

## 1. 战略与来源

- **目标：** 把分散的单元/组件证据汇合为一条可重复的真实发布验收，证明可选基线、双实例收敛、故障语义、控制台和安全边界共同成立。
- **可观察产出：** 一条受保护的本地验证脚本在隔离环境中启动全链路、发布测试配置、注入故障、检查双实例与浏览器、重启持久化并清理非持久测试资源。
- **来源：** 全部 `US-001` 至 `US-007`、`AC-001` 至 `AC-024`、所有 accepted ADR。
- **当前事实：** T-01/T-02 提供应用行为，T-03 提供官方服务，T-04/T-05 提供入口与代理；现有 release 有 `verify-release.sh` 与 Node 静态测试，但没有 Nacos 真实运行门禁。
- **Planning Depth 原因：** 多仓库、多进程与数据持久化汇合，任何假阳性都会掩盖单实例未更新、离线快照或 secret 泄露。

## 2. 决策状态

### 已锁定决策

- 验收使用隔离 Compose project、临时 data root、随机假 secret 和专用 namespace；拒绝复用用户 `<Path>release-artifacts/.env</Path>` 或现有数据卷。
- 测试先证明 base backend 默认关闭，再用 Nacos-enabled override 启动两实例；两者必须独立上报相同 digest。
- 发布矩阵至少包含：稀疏覆盖、部署层优先、三组即时/重启分类、删除、非法 YAML、保护键、类型错误、单实例可定位、运行断连、离线重启不读 snapshot、恢复收敛和容器重启持久化。
- 浏览器检查复用 T-05 Playwright；脚本不得记录配置正文、假 secret 或登录 token。
- 清理只删除本脚本解析出的隔离 project/container/network/临时目录；保留失败现场的选择必须显式，不运行广泛 `down -v`。

### 已采用的低影响假设

- 对无法通过公开业务入口稳定观测的 notify/OSS 细节，以 T-02 行为测试加实例分类状态作为组合 Evidence；不得虚构生产调用。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 隔离 E2E harness、全合同矩阵、操作文档、full/core/release gate | T-01..T-05 的稳定接缝与现有 verify-release | 新产品行为、生产部署、性能压测、HA 演练 |

## 4. 要构建什么

Lead 使用一次性测试环境先启动无 Nacos 的应用并证明默认关闭，再启动完整 Nacos-enabled 组合。脚本通过官方 API 创建测试 namespace/dataId，逐次发布合法与非法候选，并轮询两个实例的脱敏状态和可公开观察的验证码行为。随后停止 Nacos、重启单实例、恢复服务、清空配置并重启 Nacos 容器，分别验证上一有效内存、本地离线启动、最终恢复收敛和 MySQL 持久化。最后运行系统菜单浏览器路径、secret 扫描、full/core 构建与基础发布回归。

## 5. 实现契约

- **入口或接缝：** `verify-nacos.sh`、Nacos official API、两实例 actuator info、公开验证码、Playwright、Docker health/log、Maven bundle。
- **输入与输出：** 隔离测试 env -> 可判定步骤报告与 Evidence；任何步骤失败 -> 非零退出并指出阶段/实例/digest，不输出值。
- **公共接口变化：** 新增开发/CI 验收命令和运维说明，无产品 API。
- **不变量：** 每个实例独立判断；digest 可比较不可逆；测试资源唯一命名；用户环境/数据不被修改。
- **状态或数据流：** base smoke -> enabled bootstrap -> publish/observe -> invalid/fault -> restart/recover -> browser/persistence -> cleanup/report。
- **错误与失败行为：** timeout、实例摘要不一致、frame 失败、匿名成功、secret 命中或清理目标不唯一均使 gate 失败。
- **兼容要求：** `verify-release.sh`、四类 Compose、旧 external targets 与 non-Nacos startup 继续通过。
- **安全与隐私要求：** shell 禁止 xtrace secret；敏感值经 stdin/env/临时 0600 文件传递，报告只记 hash/类别。

## 6. 执行路线

1. 建立可重复的隔离 project/env/data-root 与只删除已解析目标的 cleanup trap。
2. 编排 base/full/core 与 Nacos-enabled 启动，记录 image digest、commit 和健康证据。
3. 实现合法、混合、删除、非法、保护键、类型错误的发布/双实例轮询矩阵。
4. 实现断连、单实例重启、全恢复和 Nacos/MySQL 持久化矩阵。
5. 串联 Playwright、权限、Nginx、secret/日志扫描与现有 release 回归。
6. 将操作、首次密码、明文风险、启用/回退与失败保留方式写入 release README。
7. 在 parent current workspace 运行完整 gate 并固化 Evidence。

## 7. 路径访问契约

- **预计修改点/可写范围：** 只新增验收脚本/test 并更新 release README。
- **只读上下文：** T-01..T-05 全部产品和发布实现；发现缺陷时停止并回到 owner Ticket，不在本 Ticket 越界修补。
- **共享路径：** 无共同写路径。
- **保留或不动：** 产品实现、用户 `.env`、现有数据根、生产资源和 Spec 合同。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常 | full real stack | `bash release-artifacts/scripts/verify-nacos.sh` | 双实例同 digest、即时/重启分类、控制台与持久化通过 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-06.md</Path>` |
| 失败 | publish/network/restart matrix | 非法/保护/断连/离线重启/单实例故障 | 原子拒绝、上一有效或本地回退且实例可定位 | 同上 |
| 回归 | full/core/frontend/release | Maven bundles、frontend test/build、`verify-release.sh` | non-Nacos 与旧发布能力保持绿色 | 同上 |

- **Workspace checks：** current workspace 先执行所有非 E2E tests/build，再由 Lead 运行完整隔离 gate。
- **E2E disposition：** required：本 Ticket 本身就是跨 MySQL/Nacos/双实例/Nginx/浏览器的最终门禁。
- **E2E owner/environment：** Lead / current-workspace；严格串行且只使用隔离资源。
- **Integration evidence：** 所有前置 implementation commits、parent before/final result SHA、image digests、步骤结果和父分支包含关系。

## 9. 发布、迁移与恢复

- **迁移顺序：** 先通过 base regression -> 初始化 Nacos DB -> 启动/设强密码 -> 建 namespace/config -> 开启两实例 -> 部署 proxy/menu -> 授权。
- **兼容窗口：** 任一阶段可保持客户端 disabled；启用后回退时先关闭/移除远程覆盖，再重启应用确认本地基线。
- **监控信号：** 两实例 digest/last result、Nacos/MySQL health、拒绝类别、Nginx status、浏览器 console/network 和 secret scan。
- **回滚或前向恢复：** 关闭两实例客户端并重启回本地；隐藏菜单/撤销权限；停止 Nacos但保留 DB/数据；代码缺陷按 owner Ticket 前向修复后重跑全 gate。
- **不可逆操作与批准点：** 生产部署、生产 DB、真实角色授权和卷删除均未授权；本 Ticket 只操作隔离本地资源。
- **收缩条件：** 所有 24 个 AC 有执行 Evidence，所有前置 commit 已包含于最终 parent result，且无临时测试资源残留。

## 10. 验收标准

- [x] AC-001..AC-024 均在本 Ticket 或引用的前置 Evidence 中有可执行结果，无 `uncovered`。
- [x] 双实例、离线重启无 snapshot、原子拒绝、同源登录和持久化由真实环境证明。
- [x] full/core、默认关闭、旧 external/release 回归和 secret 扫描全部通过。
- [x] required E2E、final implementation commit、direct-parent result SHA 与清理结果完整记录。
