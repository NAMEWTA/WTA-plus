# 全栈滚动发布运行手册

本手册用于 `takeover`、`upgrade` 和 `release-prod`。专题细节仍由同目录的接管、构建、中间件、验证和回滚文档拥有。

## 目录

1. 发布状态机
2. G0 授权与现场身份
3. G1 构件与配置候选
4. G2 数据保护
5. G3 后端实例 1
6. G4 后端实例 2
7. G5 前端与入口
8. G6 登录语义验收
9. 交接、失败与清理

## 发布状态机

```text
discovered
  -> candidate-validated
  -> data-protected
  -> backend1-stable
  -> backend2-stable
  -> frontend-stable
  -> semantic-accepted
```

每个箭头都是硬 Gate。当前 Gate 失败时停止，不启动下一实例，不提升活动指针，不删除失败候选。

## G0 授权与现场身份

1. 确认模式、目标、授权根目录、允许的外部写操作和明确排除的系统。
2. 从 `docker compose ls`、容器 Compose labels、现场脚本和版本化文件反查真实 project，不从目录名推断。
3. 固定 project、Compose 文件完整有序列表、env 文件、backend/frontend service 名和 bind host。
4. 记录当前镜像标签、image ID、restart count、环境摘要、活动指针、前端 index SHA-256 和回滚命令。
5. 将结果写入 profile v2，并在人工核对完成后把 `release.compose.identityConfirmed` 设为 `true`。

profile 中每个 Compose/env 路径必须是授权根目录下的绝对路径。Compose project 必须属于 `ownership.composeProjects`，任何 protected project 命中都停止。

不要执行或 `source` 现场 env。需要读取单个键时使用非执行式解析器；报告和终端不得显示 secret 正文。

## G1 构件与配置候选

1. 固定父仓库、后端和前端源码修订。
2. 执行 `release-artifacts/scripts/verify-release.sh`；本机没有 Docker 时如实记录 Compose skip。
3. 使用 `release-artifacts/scripts/release-manage.sh` 构建、暂存和打包，保留 release manifest。
4. 前端构建参数必须来自 profile：

   ```text
   VITE_APP_CONTEXT_PATH=<release.frontend.contextPath>
   VITE_APP_BASE_API=<release.frontend.baseApi>
   ```

5. 在传输前校验静态入口：

   ```bash
   node .agents/skills/deploy-namewta-environment/scripts/verify-frontend-artifact.mjs \
     --profile temp/relase/deployment-profile.json \
     --index plus-ui-namewta/apps/admin-web/dist/index.html
   ```

6. 使用 `.part` 或新 staging 目录传输，服务器端重新计算 SHA-256，再原子重命名为不可变目录。
7. 使用精确 project、全部 Compose files 和 env file 在目标机运行 `docker compose config --quiet`。本机 Compose skip 不能替代此 Gate。
8. 构建或加载候选镜像，记录标签和 image ID；不得用可变标签替代最终证据。

## G2 数据保护

默认要求配置、数据库、对象存储元数据和当前发布的可验证备份。记录命令、校验值、恢复方式和可读性验证。

只有目标为 `dev` 且用户对精确数据库、精确变更明确授权时，才允许无备份 waiver。waiver 必须同时记录：

- 授权来源和仅限目标开发库的 scope；
- 目标对象零行或可接受的数据状态；
- 表、主键和固定对象身份匹配；
- 命名、ID、权限和关联冲突为零；
- `forward-only` 恢复边界。

生产环境不接受 waiver。执行器不支持预期事务命令时，先记录能力差异；在持久写入前改用可验证的固定 session/事务方式，失败路径必须能回滚或关闭 session。

## G3 后端实例 1

1. 只更新第一个后端 service，沿用 G0 固定的 project/files/env；不使用宽泛 `up`。
2. 从容器创建开始持续采集 state、health、restart count 和脱敏启动日志。
3. 命中 `restarting`、`exited`、restart count 超限、配置装配异常或 secret 日志时立即失败，不等待总 timeout。
4. 健康后直连实例验证路由、认证边界和至少一个业务语义，不只检查 TCP 或 HTTP 200。
5. 达到 profile 规定的连续成功次数后，记录 image ID、环境摘要、业务码和日志扫描结果。

失败时恢复该实例的上一镜像/配置，验证恢复结果；第二实例保持不动。保留失败镜像、日志和 staging。

## G4 后端实例 2

只有 G3 关闭后才滚动第二实例。执行与 G3 相同的快速失败、直连语义和稳定窗口。

完成后比较两实例：

- 镜像标签与 image ID；
- restart count；
- OpenAPI 启用状态、KEK 版本和 KEK 存在性；
- OpenAPI/Nacos/运行环境摘要；
- 关键启动和 secret 日志扫描。

任何漂移都失败，不能以负载均衡入口偶尔成功代替实例一致性。

## G5 前端与入口

1. 保存当前 index hash 和活动静态目录。
2. 将校验过的前端产物解压到新目录，不在活动目录内逐文件覆盖。
3. 原子切换目录或挂载并重载入口。
4. 在有上限窗口内探测入口；记录瞬时 502/503，但以连续成功次数作为最终判定。
5. 达不到连续成功阈值、资源仍为旧 hash 或任一 JS/CSS 不在 asset prefix 下时自动恢复旧目录。
6. 记录最终 index SHA-256、实际资源路径、elapsed time 和历史瞬时失败。

单次即时 502 不是永久失败；总窗口超时或连续成功不足才是失败。探针 host 从 profile 的 bind/server 配置派生，不假设服务绑定 `127.0.0.1`。

## G6 登录语义验收

运行候选状态门禁：

```bash
node .agents/skills/deploy-namewta-environment/scripts/verify-release-candidate.mjs \
  --profile temp/relase/deployment-profile.json \
  --state temp/relase/deployment-state.json
```

随后使用全新登录会话验证关键用户路径。每个探针同时记录 HTTP 状态、业务码和可观察页面状态。允许值属于具体探针，例如“缺失凭据”可以是业务 404，而接口目录应为业务 200；不得全局把 404 当成能力关闭，也不得把 HTTP 200 当成业务成功。

菜单验收同时检查应存在项、父级/名称和已退役项。浏览器验证必须可重复，不依赖无法恢复的临时 REPL 会话。

## 交接、失败与清理

生成报告前确认 state v2 与最终运行状态一致。报告至少记录：

- 精确 Compose 身份和目标服务；
- 父/后端/前端修订、构件 SHA-256、镜像标签与 image ID；
- 每次 rollout 尝试、拒绝原因和恢复结果；
- HTTP/业务语义、稳定窗口、前端 context/API/asset；
- 备份证据，或仅限 dev 的明确 waiver；
- previous release、回滚命令、失败候选和保留资产。

失败候选、旧镜像和 rollback 目录默认保留。清理是独立授权动作，不从发布成功或用户允许部署中继承。禁止 `docker compose down -v`。
