---
schema_version: 3
artifact: ticket
change: 2026-08-28-user-password-policy-temporary-credentials
id: T-01
title: 建立跨 JVM 可确认的缓存失效协议
status: done
planning_depth: deep
planning_depth_reason: 该 prefactor 修改 Redis、Spring Cache 与 Sa-Token 共享核心，决定多实例安全一致性和后续密码策略、授权会话的正确性。
ready: true
risk: critical
blocked_by: []
contract_ids: [AC-022]
owner: codex:lead
expected_changes: ["<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-redis/src/main/java/org/dromara/common/redis/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-satoken/src/main/java/org/dromara/common/satoken/core/dao/PlusSaTokenDao.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/cache/invalidation/**</Path>"]
writable_paths: ["<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-redis/src/main/java/org/dromara/common/redis/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-satoken/src/main/java/org/dromara/common/satoken/core/dao/PlusSaTokenDao.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/cache/invalidation/**</Path>"]
read_only_paths: ["<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/ClientSessionService.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-satoken/src/main/java/org/dromara/common/satoken/utils/LoginHelper.java</Path>"]
shared_paths: ["<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-redis/src/main/java/org/dromara/common/redis/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-satoken/src/main/java/org/dromara/common/satoken/core/dao/PlusSaTokenDao.java</Path>"]
shared_path_owners: ["<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-redis/src/main/java/org/dromara/common/redis/**</Path> => T-01", "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-satoken/src/main/java/org/dromara/common/satoken/core/dao/PlusSaTokenDao.java</Path> => T-01"]
---

# Ticket T-01: 建立跨 JVM 可确认的缓存失效协议

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/ticket/01-build-cluster-cache-invalidation.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/evidence/T-01.md</Path>`

## 1. 战略与来源

- **目标：** 建立一个由 common 基础设施拥有、业务调用方无需猜测 Redis/Sa-Token key 的跨 JVM 失效协议，为密码策略缓存刷新和授权会话清理提供可确认的共同接缝。
- **可观察产出：** 两个独立 JVM 本地缓存实例共享 Redis 时，任一节点删除 Spring Cache 或 Sa-Token 值后，另一节点立即不再读取旧副本；失败能够被调用方观察和重试。
- **来源：** `AC-022`、`ADR-004`、`NFR-003`、`NFR-004`、`RISK-006`。
- **当前事实：** `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-satoken/src/main/java/org/dromara/common/satoken/core/dao/PlusSaTokenDao.java</Path>` 使用进程静态 5 秒 Caffeine，`PlusSpringCacheManager` 使用本地 Caffeine 前置层；当前 delete/evict 只清执行 JVM。
- **Planning Depth 原因：** 共享认证缓存与跨节点消息属于高事故半径核心路径，错误会让已注销 Token 或旧权限在其他节点继续有效。

## 2. 决策状态

### 已锁定决策

- common 只提供通用、类型化的失效发布/订阅与本地 evict 能力，不承载用户、角色或 Client 业务规则。
- Redis 是节点协调权威；消息载荷只包含缓存命名空间、不可逆 key 标识/必要 key 和关联标识，不含 Token 值、LoginUser、密码或业务数据。
- 本地节点先完成持久层删除，再广播；发起方必须能区分发布失败，业务层不能把“等待 TTL”当成功。
- Sa-Token 与 Spring Cache 的本地失效通过各自公开抽象完成，不允许 system 直接反射或访问静态 Caffeine。

### 已采用的低影响假设

- 优先复用现有 `RedisUtils` 发布/订阅能力；具体 channel 名按 common 命名规范确定。验证：双实例集成测试与 channel 冲突扫描。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| 通用失效事件、Redis 发布/订阅、本地 Spring Cache/Sa-Token evict、幂等和双实例测试 | RedisUtils、现有 CacheManager、PlusSaTokenDao 的 Redis 数据模型 | 不决定哪些用户/Client 要注销，不清业务数据库，不修改 controller |

## 4. 要构建什么

当一个节点通过统一缓存抽象删除值时，持久 Redis 状态先被删除，本节点本地副本立即失效，并发布一条不含秘密的失效消息。所有其他实例接收后幂等清理对应本地副本。发布或订阅初始化失败必须暴露为可诊断失败；重复、乱序或本节点回环消息不得恢复旧值，也不得导致业务异常。

## 5. 实现契约

- **入口或接缝：** Spring Cache evict/clear 与 Sa-Token DAO delete/invalidate 接缝；真实 Redis pub/sub 集成测试。
- **输入与输出：** 输入为缓存 namespace、key 或 clear 范围与 requestId；输出为本地失效、Redis 删除结果、发布结果和节点处理记录。
- **公共接口变化：** common 内新增最小失效协议 API，供 T-02/T-03 只读消费；不新增 HTTP 接口。
- **不变量：** 删除先于广播；消息不含秘密；重复处理幂等；任何节点不得因本地缓存命中绕过 Redis 已删除事实。
- **状态或数据流：** caller -> Redis delete -> local evict -> publish -> remote listener -> remote local evict。
- **错误与失败行为：** Redis/publish 失败保留 cause 并返回失败；订阅失败有日志/健康信号；不得吞错或无限重试。
- **兼容要求：** 现有 CacheUtils、RedisUtils、SaTokenDao 调用形状保持；单节点行为不退化。
- **安全与隐私要求：** channel、日志、指标和测试 fixture 不记录 Token、密码、LoginUser 或 fixed 配置值。

## 6. 执行路线

1. 建立双本地实例加真实 Redis 的红色测试，证明当前远端 Caffeine 会残留。
2. 在 common-redis 定义通用失效事件、发布/订阅生命周期和 Spring Cache 本地 evict 接缝。
3. 让 PlusSaTokenDao 的写删操作接入同一广播语义，同时保持既有 Redis key 合同。
4. 覆盖回环、重复、发布失败、订阅关闭和节点重启后的幂等行为。
5. 运行 common/admin 定向测试和后端 reactor 回归，形成 prefactor 安全落点。

## 7. 路径访问契约

- **预计修改点：** common-redis manager/config/event/listener、PlusSaTokenDao、admin 聚合测试。
- **可写范围：** frontmatter 所列 common 源码与专用测试目录。
- **只读上下文：** ClientSessionService 与 LoginHelper 的现有消费语义。
- **共享路径：** common-redis 与 PlusSaTokenDao 仅由 T-01 修改；下游 Ticket 只调用公开接缝。
- **保留或不动：** system 业务规则、HTTP、SQL、前端和永久 raw HTTP 日志决定。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | 双实例真实 Redis 集成 | 节点 A 写/读后删除，节点 B 再读 | B 立即 miss，不等待 5/30 秒 TTL | `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/evidence/T-01.md</Path>` |
| 失败路径 | Redis/pub-sub 故障注入 | 中断发布或订阅并执行失效 | 调用方得到失败，cause 与节点状态可检索，无假成功 | 同上 |
| 回归 | Maven reactor | `./mvnw -pl ruoyi-common/ruoyi-common-redis,ruoyi-common/ruoyi-common-satoken,ruoyi-admin -am test` | 既有单节点缓存与 Sa-Token 行为保持 | 同上 |

- **Workspace checks：** Goal Plan 选 current 时在 current-workspace；选 required 时在 source-worktree 跑非 E2E 测试。
- **E2E disposition：** required：跨 JVM + 真实 Redis 是本 Ticket 的外部边界，mock 不能替代。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate；启动两个独立 application/cache context，确认远端本地值即时失效。
- **Integration evidence：** 记录 backend implementation/source commit、parent before、适用 candidate/result SHA 与父仓库 gitlink 包含关系。

## 9. 发布、迁移与恢复

- **迁移顺序：** 先集成本协议，再允许 T-02/T-03 消费；不先改业务调用方。
- **兼容窗口：** 旧调用继续工作，新失效广播向后兼容；集群部署时所有节点必须完成升级后才依赖确认语义。
- **监控信号：** publish/receive/evict 次数、延迟、失败、未知 namespace、节点 listener 健康。
- **回滚或前向恢复：** 协议失败时回退本 Ticket，业务保持旧 TTL 行为但不得发布依赖它的新授权合同；上线后优先前向修复 listener。
- **不可逆操作与批准点：** 无数据迁移；implementation commit、集成和部署仍需单独授权。
- **收缩条件：** 不适用；该协议是正式 shared contract，不是临时兼容层。

## 10. 验收标准

- [x] `AC-022` 的跨节点失败可观察和幂等重试基础成立。
- [x] 双实例不再读取已删除的 Spring Cache 或 Sa-Token 本地副本。
- [x] 消息、日志和指标无凭据/Token/会话正文。
- [x] 验证矩阵记录到 `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/evidence/T-01.md</Path>`。
- [x] 修改未超出路径合同，形成 backend 非空 commit，并由 Lead 完成 direct-parent 或 candidate 验证及父状态记录。
