---
schema_version: 3
artifact: ticket
change: 2026-08-31-optional-nacos-dynamic-config
id: T-03
title: 交付鉴权持久化的 Nacos Docker 基础设施
status: done
planning_depth: deep
planning_depth_reason: 固定第三方服务版本、创建独立数据库最小权限、处理必填 secret 与新旧数据卷初始化，并协调双实例健康启动门。
ready: true
risk: high
blocked_by: [T-01]
contract_ids: [AC-006, AC-018, AC-019, AC-020, AC-021, AC-024]
owner: unassigned
expected_changes:
  - "<Path>release-artifacts/docker/docker-compose-infrastructure.yml</Path>"
  - "<Path>release-artifacts/docker/overrides/nacos-enabled.yml</Path>"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/nacos/**</Path>"
  - "<Path>release-artifacts/scripts/init-nacos-mysql-container.sh</Path>"
writable_paths:
  - "<Path>release-artifacts/.env.example</Path>"
  - "<Path>release-artifacts/docker/docker-compose-infrastructure.yml</Path>"
  - "<Path>release-artifacts/docker/overrides/nacos-enabled.yml</Path>"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/15-nacos-init.sh</Path>"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/nacos/**</Path>"
  - "<Path>release-artifacts/scripts/init-nacos-mysql-container.sh</Path>"
  - "<Path>release-artifacts/scripts/release-manage.sh</Path>"
  - "<Path>release-artifacts/tests/release-config.test.mjs</Path>"
read_only_paths:
  - "<Path>release-artifacts/docker/docker-compose-backend.yml</Path>"
  - "<Path>release-artifacts/scripts/init-mysql-container.sh</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-nacos/**</Path>"
shared_paths: []
shared_path_owners: []
---

# Ticket T-03: 交付鉴权持久化的 Nacos Docker 基础设施

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/03-nacos-docker-infrastructure.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-03.md</Path>`

## 1. 战略与来源

- **目标：** 在现有 release infrastructure 中提供固定、鉴权、MySQL 持久化且可选启用的官方 Nacos 服务，并为双 ruoyi-admin 增加安全的组合启动方式。
- **可观察产出：** 基础 Compose 可解析；选择 Nacos override 后，数据库先初始化、Nacos 健康后两实例启动并显式启用客户端；未选择时后端保持原状。
- **来源：** `US-002`、`US-006`、`AC-006`、`AC-018` 至 `AC-021`、`AC-024`、`ADR-005`、`ADR-008`、`ADR-011`、`ADR-012`、Nacos 2.5 官方鉴权与 Docker 文档。
- **当前事实：** infrastructure 已有 MySQL 8.4.9、Redis、MinIO 和可选 Elasticsearch，端口统一绑定 `NAMEWTA_BIND_HOST`；backend 是独立 Compose，直接写跨文件 `depends_on` 会使其单独解析失败；release 测试固定四类 Compose。
- **Planning Depth 原因：** secret、数据库权限、持久卷和启动门错误会导致服务暴露、数据丢失或整个发布不可启动。

## 2. 决策状态

### 已锁定决策

- 使用 `nacos/nacos-server:v2.5.4`、standalone、MySQL，不使用 `latest` 或自建 Server 镜像。
- 8848/9848 映射默认绑定 `${NAMEWTA_BIND_HOST:-127.0.0.1}`；容器网络内由服务名访问。
- 开启 `NACOS_AUTH_ENABLE`，token 与 identity key/value、独立 DB 密码均为 Compose 必填变量，不提供可工作的仓库默认值。
- Nacos 使用独立 `nacos` 数据库与最小权限账号；固定版本 schema 来源可追踪，fresh data volume 与 existing volume 都有幂等初始化入口。
- 继续保留四类顶级 Compose；Nacos 服务属于 infrastructure。跨 infrastructure/backend 的健康依赖与两实例 `NACOS_CONFIG_ENABLED=true` 由 `<Path>release-artifacts/docker/overrides/nacos-enabled.yml</Path>` 组合，基础 backend compose 不被破坏。
- Nacos 简单鉴权只用于可信网络；首次启动必须按官方流程设置强管理员密码，仓库不保存该密码。

### 已采用的低影响假设

- Nacos 数据与日志放入现有 `NAMEWTA_DATA_ROOT` 结构；具体子目录名遵守其他基础设施风格。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 官方镜像、数据库/schema、鉴权变量、持久目录、健康检查、optional override、初始化/静态测试 | 现有 MySQL/network/bind-host/env 与 release 脚本模式 | HA 集群、公网暴露、生产 secret、Nginx 路由、生产执行 |

## 4. 要构建什么

发布人员填充所有必填 Nacos 变量后，可在 fresh 或 existing MySQL 数据卷中幂等创建独立数据库、账号和 2.5.4 schema。基础设施启动固定 Nacos 容器并通过真实 API 健康检查。只有使用 Nacos-enabled override 组合四类 Compose 时，两台 ruoyi-admin 才获得一致连接参数和 profile namespace，并在 Nacos healthy 后启动；普通 backend compose 始终可独立解析且默认关闭客户端。

## 5. 实现契约

- **入口或接缝：** Compose infrastructure + optional override、MySQL init hook、existing-volume 初始化脚本、release config tests。
- **输入与输出：** 必填 env + MySQL -> healthy authenticated Nacos 与双实例连接 env；缺 secret -> Compose/config 明确失败。
- **公共接口变化：** 新增 Nacos 部署变量、服务名/端口和 override 调用合同。
- **不变量：** 固定镜像；独立 DB/user；无默认真实 secret；基础 backend 独立有效；默认宿主机本地绑定。
- **状态或数据流：** MySQL healthy/schema ready -> Nacos healthy -> override 中 ruoyi-admin 启动；数据保存在宿主目录/MySQL。
- **错误与失败行为：** 缺变量、schema 导入失败、未授权请求和 Nacos unhealthy 均明确失败且不伪造健康；初始化不得删除其他数据库。
- **兼容要求：** 顶级 Compose 仍为四类，现有 `up all` 不因未选择 Nacos 强制启用客户端。
- **安全与隐私要求：** secret 只从忽略的 env 注入，不在命令行回显、日志、测试 fixture 或生成 SQL 中落盘。

## 6. 执行路线

1. 为镜像 pin、必填变量、端口绑定、四类 Compose 和 override 合并建立失败/成功静态测试。
2. 固定并校验 Nacos 2.5.4 MySQL schema，增加 fresh init hook 与 existing-volume 幂等初始化脚本。
3. 在 infrastructure 增加 Nacos 服务、持久目录、鉴权、MySQL 连接和真实健康检查。
4. 增加 optional override，为两 admin 注入相同配置单元合同并增加 Nacos healthy gate。
5. 使用假 secret 启动真实 MySQL/Nacos，验证未授权、授权、持久化、重启和最小权限。
6. 运行 release 静态回归，确认基础四类 Compose 和非 Nacos 路径不变。

## 7. 路径访问契约

- **预计修改点/可写范围：** 仅 release env 示例、infrastructure/override、Nacos schema/init、release 管理与静态测试。
- **只读上下文：** backend Compose 和 T-01 属性名；不得为方便直接改 backend Compose。
- **共享路径：** 无共同写路径；Nginx 归 T-05，最终 E2E 脚本归 T-06。
- **保留或不动：** 用户的 `<Path>release-artifacts/.env</Path>`、业务 MySQL 初始化、生产数据卷与 secret。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常 | Compose + real containers | 用隔离 data root 组合 infrastructure/backend/override | MySQL/Nacos healthy，两 admin 获得启用变量和健康门 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-03.md</Path>` |
| 失败 | config/auth/init | 缺每类 secret、匿名请求、低权限越界和重复 init | 明确失败，其他 DB 不变，无 secret 回显 | 同上 |
| 回归 | release tests/base Compose | `node --test release-artifacts/tests/release-config.test.mjs` 与基础 `docker compose config` | 四分类和原基础部署继续通过 | 同上 |

- **Workspace checks：** current workspace 运行 shell/static tests 与所有 Compose config；真实容器使用隔离目录和测试凭据。
- **E2E disposition：** required：官方镜像、MySQL 持久化、鉴权和健康门无法由纯静态测试充分证明。
- **E2E owner/environment：** Lead / current-workspace；仅本地隔离 Docker，不执行生产变更。
- **Integration evidence：** implementation commit、direct-parent before/result SHA、容器 image digest/health、授权与持久化证据。

## 9. 发布、迁移与恢复

- **迁移顺序：** 备份 MySQL -> 幂等创建 Nacos DB/user/schema -> 启动 Nacos -> 首次强密码 -> 创建 namespace/config -> 使用 override 启动双实例。
- **兼容窗口：** base deployment 可一直保持 Nacos disabled；existing volume 先显式运行初始化脚本，不能依赖 Docker entrypoint 重跑。
- **监控信号：** MySQL/Nacos health、鉴权失败、schema/version、容器重启和磁盘增长。
- **回滚或前向恢复：** 先移除 override 让应用回到本地配置，再停止 Nacos；保留数据库与目录以便恢复，不自动删卷。
- **不可逆操作与批准点：** 生产 DB/user/schema 创建、首次密码和任何卷删除均未授权；必须单独批准。禁止 `down -v`。
- **收缩条件：** 不适用：新增可选基础设施，无旧服务替换。

## 10. 验收标准

- [ ] AC-018..AC-021/024 的固定镜像、鉴权、独立 DB、持久化、健康门和幂等初始化成立。
- [ ] 缺 secret 失败、匿名拒绝、默认本机绑定与 secret 不回显有 Evidence。
- [ ] 基础 backend/infrastructure 仍可单独 config，只有 override 显式启用两实例。
- [ ] required E2E、implementation commit 与 direct-parent 结果完整记录。
