---
schema_version: 3
artifact: ticket
change: 2026-09-08-richtext-third-oss
id: T-03
title: OSS 开发环境默认不再依赖固定前端 IP 才能通过 CORS
status: done
planning_depth: deep
planning_depth_reason: 这是跨浏览器、MinIO 和发布工件的安全敏感环境配置修订，会影响联调用 CORS 语义和生产默认值说明。
ready: true
risk: high
blocked_by: []
contract_ids: [AC-003]
owner: grok-build
expected_changes:
  - "<Path>release-artifacts/docker/docker-compose-infrastructure.yml</Path>"
  - "<Path>release-artifacts/.env.example</Path>"
  - "<Path>docs/error/oss-login-and-direct-upload-troubleshooting.md</Path>"
writable_paths:
  - "<Path>release-artifacts/docker/docker-compose-infrastructure.yml</Path>"
  - "<Path>release-artifacts/.env.example</Path>"
  - "<Path>docs/error/oss-login-and-direct-upload-troubleshooting.md</Path>"
read_only_paths:
  - "<Path>docs/oss-public-private-operations.md</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-oss/src/main/java/org/dromara/common/oss/client/DefaultOssClientImpl.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-oss/src/main/java/org/dromara/common/oss/config/OssClientConfig.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/domain/SysOssConfig.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysOssServiceImpl.java</Path>"
shared_paths: []
shared_path_owners: []
---

# Ticket T-03: OSS 开发环境默认不再依赖固定前端 IP 才能通过 CORS

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-09-08-richtext-third-oss/ticket/03-oss-dev-cors-default.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-09-08-richtext-third-oss/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-09-08-richtext-third-oss/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-09-08-richtext-third-oss/evidence/T-03.md</Path>`

实现本 Ticket 时，Lead 与 implementation subagent 必须按顺序完整读取总体 Map、其中适用于 `ALL`/`T-03` 的项目 Skill，再读取本 Ticket 与其他上游工件。

## 1. 战略与来源

- **目标：** 把本地/开发环境的 MinIO CORS 默认值从固定前端 IP 白名单，调整成更适合频繁变更 Origin 的默认策略，并把生产侧继续收口的规则写清楚。
- **可观察产出：** 前端 Origin 改变后，浏览器不再因为固定 IP 白名单而触发预检失败；生产仍可通过显式白名单和稳定域名控制。
- **来源：** `AC-003`、`DIAG-2026-09-08-richtext-third-oss`、`CODE:<Path>release-artifacts/docker/docker-compose-infrastructure.yml</Path>`、`CODE:<Path>release-artifacts/.env.example</Path>`、`CODE:<Path>docs/error/oss-login-and-direct-upload-troubleshooting.md</Path>`、`CODE:<Path>docs/oss-public-private-operations.md</Path>`
- **当前事实：** release-artifacts 里目前把 CORS 默认值写成了少数本地 Origin；文档还在强调最小白名单。
- **Planning Depth 原因：** 这是跨浏览器、MinIO 和发布工件的安全敏感配置修订，会影响联调用 CORS 语义和生产默认值说明。

## 2. 决策状态

### 已锁定决策

- 开发环境可以采用更宽松的 CORS 默认值，以减少前后端地址变化带来的反复配置。
- 本地 release compose 默认使用 `*`（仅开发/联调）；共享或生产环境必须通过 `MINIO_API_CORS_ALLOW_ORIGIN` 显式覆盖为稳定 HTTPS 域名白名单。
- 生产环境仍保留稳定公共域名和显式白名单的收口策略。
- 本次不改 Bucket policy，也不改对象访问类型语义。

### 已采用的低影响假设

- `*` 只允许作为本地 release compose 默认值；共享/生产验收必须检查显式 HTTPS 白名单覆盖。

- release-artifacts 是开发/联调入口，不是生产唯一权威。
- 生产约束仍以 `docs/oss-public-private-operations.md` 和环境变量覆盖为准。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| compose 默认值、示例环境文件、排障文档 | 现有 `endpoint` / `domainUrl` 分离、现有 OSS 私有/公共语义、现有生产手册 | Bucket policy 迁移、对象清理、反向代理新基础设施、登录/权限模型改造 |

## 4. 要构建什么

本地开发者换了前端端口、域名或子域时，不应该还得去 MinIO 容器里手工改固定 IP 白名单。开发环境应该提供一个更宽松、更容易启动的默认配置；一旦进入生产或共享环境，仍然可以通过显式白名单和稳定域名把权限收紧回来。这样用户就不会因为前端地址改了就立刻掉进 CORS 预检失败里。

## 5. 实现契约

- **入口或接缝：** release-artifacts 里的 MinIO compose 默认值、示例 `.env`、以及 OSS 排障文档。
- **输入与输出：** 输入是开发环境或联调环境的前端 Origin 变化；输出是无需逐次修改 IP 白名单的可用 CORS 默认值和明确的生产说明。
- **公共接口变化：** 无代码接口变化；只改变默认环境契约和文档语义。
- **不变量：** 生产依旧需要稳定 `domainUrl` 和显式白名单；私有对象仍必须走签名访问。
- **状态或数据流：** 配置层先命中更宽松的默认值，文档明确生产覆盖方式。
- **错误与失败行为：** 若有人把开发默认值误用到生产，文档必须明确这是不允许的。
- **兼容要求：** 现有环境变量显式覆盖仍优先于默认值。
- **安全与隐私要求：** 不在文档或 evidence 里记录密钥或完整签名 URL；生产仍遵守最小权限。

## 6. 执行路线

1. 把 release-artifacts 中 MinIO 的默认 CORS 来源改成适合本地联调的更宽松默认值。
2. 同步更新 `.env.example` 和排障文档，说明开发环境与生产环境的不同默认策略。
3. 检查文档是否仍然清楚保留了生产侧稳定域名和显式白名单要求。
4. 用 compose config 和浏览器预检做一次回归验证。

## 7. 路径访问契约

- **预计修改点：** 与 `expected_changes` 对齐，仅作导航。
- **可写范围：** 与 `writable_paths` 对齐；越界前必须停止。
- **只读上下文：** 与 `read_only_paths` 对齐。
- **共享路径：** 无。
- **保留或不动：** OSS 访问类型、bucket 生命周期和私有签名语义。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | compose 默认值 | `docker compose --env-file release-artifacts/.env.example -f release-artifacts/docker/docker-compose-infrastructure.yml config --quiet` | CORS 默认值能正确展开，且文档/示例一致 | `<Path>{roots.state}/specdev/changes/2026-09-08-richtext-third-oss/evidence/T-03.md</Path>` |
| 失败路径 | 浏览器预检 | 使用不同前端 Origin 访问同一 MinIO 直传 URL | 不再因固定 IP 白名单触发预检失败 | `<Path>{roots.state}/specdev/changes/2026-09-08-richtext-third-oss/evidence/T-03.md</Path>` |
| 回归 | 生产说明 | 检查 `docs/oss-public-private-operations.md` / `docs/error/oss-login-and-direct-upload-troubleshooting.md` | 生产仍明确要求稳定域名和显式白名单 | `<Path>{roots.state}/specdev/changes/2026-09-08-richtext-third-oss/evidence/T-03.md</Path>` |

- **Workspace checks：** current-workspace，先跑 compose config，再做浏览器预检回归。
- **E2E disposition：** required: 这是浏览器到对象存储的跨边界行为，必须确认预检真的恢复。
- **E2E owner/environment：** Lead / current-workspace；场景是本地 release-artifacts + MinIO + 浏览器预检。
- **Integration evidence：** implementation commit、config diff、浏览器预检结果和父分支 result SHA 在完成时写入 Evidence；当前 draft 阶段仅保留计划，不伪造 SHA。

## 9. 发布、迁移与恢复

- **迁移顺序：** 不适用：无数据迁移。
- **兼容窗口：** 现有显式 CORS 覆盖仍可继续使用。
- **监控信号：** 浏览器预检失败率应下降；生产侧仍需观察显式白名单是否保持收口。
- **回滚或前向恢复：** 回滚时恢复原先固定 Origin 白名单。
- **不可逆操作与批准点：** 无生产启用动作；若要把更宽松默认值带入共享环境，需额外审批。
- **收缩条件：** 不适用：没有旧协议收缩。

## 10. 验收标准

- [ ] `AC-003`：前端 Origin 改变时，开发环境不再因固定 IP 白名单导致 CORS 预检失败。
- [ ] 实现开始前已完整读取 Tickets Map 及其中适用于 `ALL`/`T-03` 的项目 Skill。
- [ ] 验证矩阵全部执行并记录到 `<Path>{roots.state}/specdev/changes/2026-09-08-richtext-third-oss/evidence/T-03.md</Path>`。
- [ ] 实际项目修改未超出 `writable_paths`。
- [ ] Ticket、Tickets Map 和 Evidence 状态一致。
