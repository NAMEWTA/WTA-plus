---
schema_version: 3
artifact: ticket
change: 2026-08-28-user-password-policy-temporary-credentials
id: T-05
title: 实现一分钟一次性临时密码认证
status: done
planning_depth: deep
planning_depth_reason: 该 Ticket 新增独立授权的敏感凭据、Redis TTL/CAS 并发和 password grant 后备路径，属于认证高风险核心。
ready: true
risk: critical
blocked_by: [T-03]
contract_ids: [AC-005, AC-006, AC-007, AC-008, AC-009, AC-010, AC-011, AC-012, AC-013, AC-014, AC-023, AC-024]
owner: codex:lead
expected_changes: ["<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/temporarypassword/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysTemporaryPasswordController.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/java/org/dromara/web/service/impl/PasswordAuthStrategy.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/java/org/dromara/web/service/SysLoginService.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/password/temporary/**</Path>"]
writable_paths: ["<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/temporarypassword/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysTemporaryPasswordController.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/domain/bo/password/TemporaryPasswordIssueBo.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/domain/vo/password/TemporaryPasswordVo.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/java/org/dromara/web/service/impl/PasswordAuthStrategy.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/java/org/dromara/web/service/SysLoginService.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/password/temporary/**</Path>"]
read_only_paths: ["<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/password/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/ClientUserTypeAccessService.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-redis/src/main/java/org/dromara/common/redis/utils/RedisUtils.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-satoken/src/main/java/org/dromara/common/satoken/utils/LoginHelper.java</Path>"]
shared_paths: []
shared_path_owners: []
---

# Ticket T-05: 实现一分钟一次性临时密码认证

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/ticket/05-implement-temporary-password-authentication.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/evidence/T-05.md</Path>`

## 1. 战略与来源

- **目标：** 为管理员提供不修改永久密码的一分钟单次凭据，并让它在现有 password grant 中安全认证为普通会话。
- **可观察产出：** 独立权限签发合规密码；60 秒 TTL、覆盖旧值、错误不消费、正确并发只一次；可准入任意 Client，成功后会话无标记且与永久登录等价。
- **来源：** `US-002`、`US-003`、`US-009`、`AC-005` 至 `AC-014`、`AC-023`、`AC-024`、`ADR-002`、`ADR-003`。
- **当前事实：** PasswordAuthStrategy 只执行 BCrypt 永久校验并立即记录失败，再检查 ClientUserTypeAccess；系统没有临时凭据存储或独立权限。
- **Planning Depth 原因：** 新认证凭据涉及权限、审计、Redis 原子并发、失败计数和 Client 隔离。

## 2. 决策状态

### 已锁定决策

- POST `/system/user/temporaryPassword`，权限 `system:user:temporaryPassword`，响应 password + 60，no-store，`@Log` 请求/响应均不落库。
- Redis 只存不可逆校验值，key 以 userId 为作用域；reissue 覆盖，TTL 60 秒。
- 永久 BCrypt 优先；只有永久失败才读取临时 key。错误不消费，准入失败不消费，准入后 compare-and-delete 唯一赢家发 Token。
- 永久/临时共同形成一次 grant 判定：两者都错只记一次失败；临时成功不先记永久失败。
- 成功后完全复用普通 LoginUser/token，不写临时标记、不缩时长、不改权限。

### 已采用的低影响假设

- CAS 使用“读取存储 hash -> BCrypt verify -> Redis 脚本比较同一 hash 后删除”，无需在 Lua 中实现 BCrypt。验证：双并发唯一成功测试。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| 签发 API、Redis store/CAS、password grant 组合、失败计数、审计 | T-03 generator、RedisUtils、ClientUserTypeAccess、普通 token flow | 不改永久密码，不建临时会话类型，不新增登录 request 字段 |

## 4. 要构建什么

有独立权限且能操作目标用户的管理员签发一次密码，只显示本次明文。用户可在原本获准的任意 Client 输入它；错误/过期/覆盖/已消费统一返回普通凭据错误。目标 Client 未准入时不消费；首个准入并 CAS 成功的请求获得普通 Token，后续请求失败。正确永久密码不碰临时值。

## 5. 实现契约

- **入口或接缝：** temporaryPassword POST、现有 `/auth/login` password strategy、Redis CAS。
- **输入与输出：** `{userId}` -> `{password,expiresInSeconds:60}`；login body 不变。
- **公共接口变化：** 新签发 API；登录 wire 不变。
- **不变量：** DB password 不写；user scope；TTL 60；一次成功；Client/userType/status/captcha 均执行；普通 session 等价。
- **状态或数据流：** issue -> CSPRNG/hash/SET TTL -> login permanent check -> temp verify -> access check -> CAS delete -> normal login。
- **错误与失败行为：** Redis 不可用临时分支 fail-close，永久成功仍工作；策略不可用禁止新签发但已有 hash 可验证；token 签发异常不恢复已消费值。
- **兼容要求：** 登录 body、Client 时长和永久认证保持；通用错误不泄露 temp 状态。
- **安全与隐私要求：** 除 ADR-0017 专用 raw sink 外，日志/审计/Redis/异常不含明文或 hash；签发 no-store。

## 6. 执行路线

1. 建立 TTL、覆盖、错误不消费、永久不消费、并发唯一成功的真实 Redis 红测。
2. 建立 system 临时凭据 store/service 和独立 controller 权限/data scope/审计边界。
3. 重构 PasswordAuthStrategy/SysLoginService 为一次组合判定，避免中间永久失败计数。
4. 将 Client/userType 准入放在临时 CAS 前，并保持永久登录既有结果。
5. 覆盖 Redis/策略故障、未获准 Client、用户停用、验证码、token 签发异常和普通会话等价。
6. 运行 admin/system 定向和认证全矩阵。

## 7. 路径访问契约

- **预计修改点：** system temporarypassword package/controller/BO/VO、PasswordAuthStrategy、SysLoginService、tests。
- **可写范围：** frontmatter 精确路径；若 T-04 已创建 BO/VO 目录，仅写指定临时类型。
- **只读上下文：** T-03 policy、Client access、RedisUtils、LoginHelper。
- **共享路径：** 无；不得修改 T-01/T-03 shared implementation。
- **保留或不动：** SysUser.password、session model、SQL、前端。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | real Redis + login API | 签发并在获准 Client 登录 | 一次普通 Token、永久密码不变、无标记 | `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/evidence/T-05.md</Path>` |
| 失败路径 | concurrency/fault matrix | 错误、过期、覆盖、双并发、未准入、Redis down | 稳定通用错误、准确消费/不消费、失败计数一次 | 同上 |
| 回归 | Maven auth suite | `./mvnw -pl ruoyi-modules/ruoyi-system,ruoyi-admin -am test` | 永久 password grant/验证码/Client 行为保持 | 同上 |

- **Workspace checks：** Goal Plan current/source-worktree 跑非 E2E；真实 Redis test 必须确认未 skipped。
- **E2E disposition：** required：凭据从签发到真实 password grant、并发 CAS 和普通 Session 是关键跨边界。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate；多 Client、多请求、真实 Redis。
- **Integration evidence：** backend commit、parent/candidate/result SHA、父 gitlink。

## 9. 发布、迁移与恢复

- **迁移顺序：** T-03 后实现；T-08 权限 DML 后才对非超管开放；前端 T-07 后发。
- **兼容窗口：** 新 endpoint additive；旧 login client 无需变化。混合集群不得启用签发，避免节点能力不一致。
- **监控信号：** issue/overwrite/consume/expired/CAS lost/Redis failure，按 user/client/requestId 聚合但无凭据。
- **回滚或前向恢复：** 禁用新权限/前端入口即可止签；残留 key 最多 60 秒自清；不需改永久密码。
- **不可逆操作与批准点：** CAS 消费不可恢复且为合同；生产启用权限、部署需授权。
- **收缩条件：** 不适用；无临时兼容入口。

## 10. 验收标准

- [x] `AC-005` 至 `AC-014` 的权限、TTL、覆盖、消费、Client 和普通会话成立。
- [x] `AC-023` 审计脱敏，`AC-024` 失败计数准确。
- [x] 真实 Redis 并发 Evidence 不 skipped，永久密码/会话不受影响。
- [x] backend commit、Lead 集成/E2E/父状态完整，修改未越界。
