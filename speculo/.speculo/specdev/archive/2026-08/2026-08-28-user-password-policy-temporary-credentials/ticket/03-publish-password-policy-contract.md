---
schema_version: 3
artifact: ticket
change: 2026-08-28-user-password-policy-temporary-credentials
id: T-03
title: 发布统一密码策略与安全公开投影
status: done
planning_depth: deep
planning_depth_reason: 该 Ticket 建立 versioned sys_config、安全随机生成、结构化错误和公开认证 wire contract，属于安全与公共 API 共享核心。
ready: true
risk: high
blocked_by: [T-01]
contract_ids: [AC-001, AC-003, AC-015, AC-019]
owner: codex:lead
expected_changes: ["<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-core/src/main/java/org/dromara/common/core/exception/ServiceException.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-web/src/main/java/org/dromara/common/web/handler/GlobalExceptionHandler.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/password/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysConfigServiceImpl.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/java/org/dromara/web/controller/AuthController.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/java/org/dromara/web/domain/vo/AuthClientContextVo.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/password/policy/**</Path>"]
writable_paths: ["<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-core/src/main/java/org/dromara/common/core/exception/ServiceException.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-web/src/main/java/org/dromara/common/web/handler/GlobalExceptionHandler.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/password/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysConfigServiceImpl.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/java/org/dromara/web/controller/AuthController.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/java/org/dromara/web/domain/vo/AuthClientContextVo.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/password/policy/**</Path>"]
read_only_paths: ["<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/ISysConfigService.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-core/src/main/java/org/dromara/common/core/constant/RegexConstants.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-redis/src/main/java/org/dromara/common/redis/**</Path>"]
shared_paths: ["<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-core/src/main/java/org/dromara/common/core/exception/ServiceException.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-web/src/main/java/org/dromara/common/web/handler/GlobalExceptionHandler.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/password/**</Path>"]
shared_path_owners: ["<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-core/src/main/java/org/dromara/common/core/exception/ServiceException.java</Path> => T-03", "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-web/src/main/java/org/dromara/common/web/handler/GlobalExceptionHandler.java</Path> => T-03", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/password/**</Path> => T-03"]
---

# Ticket T-03: 发布统一密码策略与安全公开投影

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/ticket/03-publish-password-policy-contract.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/evidence/T-03.md</Path>`

## 1. 战略与来源

- **目标：** 建立所有密码入口共用的类型化策略、生成器、详细违规合同和公开非敏感投影。
- **可观察产出：** `/auth/client/context` 返回 8-30、四类必需字符和允许特殊字符；合法 random/fixed 配置可用，非法配置保存被拒且旧有效策略继续服务。
- **来源：** `US-004`、`US-006`、`AC-001`、`AC-003`、`AC-015`、`AC-019`、`ADR-001`。
- **当前事实：** 当前只有静态 `RegexConstants.PASSWORD`；`sys.user.initPassword` 是普通字符串，AuthClientContextVo 只含 Client/注册开关，ServiceException handler 无结构化 data。
- **Planning Depth 原因：** 同时改变公共 JSON、异常 envelope、配置缓存与密码生成安全边界。

## 2. 决策状态

### 已锁定决策

- 配置键 `sys.user.passwordPolicy`，v1 JSON 形状、8-30 ASCII、四类必需、默认 specials、generator 与 RANDOM/FIXED 依 Spec。
- random 使用密码学安全随机源并构造性包含四类；临时密码始终使用 generator。
- 配置在 parse、save 和 use 边界验证；新值完整合法后才替换旧缓存，跨 JVM 复用 T-01。
- 公开投影只含 minimumLength、maximumLength、requiredCharacterClasses、allowedSpecialCharacters。
- 违规 `reason` 集合和顺序按 Spec；common 只承载通用结构化业务错误 envelope，不依赖 system。

### 已采用的低影响假设

- 策略领域代码位于 system 的 `password` 明确命名包，避免继续把安全逻辑塞入 SysConfigService。验证：package/import review。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| 配置模型/验证/缓存、生成/校验、详细错误、公开投影 | JsonUtils、SecureRandom/JDK、R envelope、T-01 cache invalidation | 不改密码写入口，不签发临时凭据，不写 DML |

## 4. 要构建什么

调用方取得当前有效策略后可验证明文、生成默认候选或生成临时候选。违规返回全部稳定原因，策略错误不泄露 fixedValue/字符池。配置管理员更新该 key 时，非法 JSON 或不可能满足规则的组合被拒并保留旧策略。未认证 Client context 只返回表单所需投影，任何敏感内部字段都不出现在 JSON。

## 5. 实现契约

- **入口或接缝：** system 密码策略服务、SysConfigService 保存边界、GET `/auth/client/context`、GlobalExceptionHandler。
- **输入与输出：** 输入 v1 JSON/明文；输出 validated policy、候选密码、violations 或公开 projection。
- **公共接口变化：** AuthClientContextVo 新增 `passwordPolicy`；R 失败 data 支持 `{violations:[{reason,message}]}`，既有失败保持兼容。
- **不变量：** fixedValue/generator internals 永不进入公开 projection/异常/普通日志；生成结果必过同一 validator。
- **状态或数据流：** sys_config -> parse/validate -> immutable cached policy -> projection/generate/validate；save 时 validate-before-cache-put。
- **错误与失败行为：** 策略不可用为 `PASSWORD_POLICY_UNAVAILABLE`；密码违规按固定顺序；Client 不可用保持现有 client flags 语义且不猜测弱策略。
- **兼容要求：** 新响应字段对旧客户端为 additive；现有 R code/msg 保持，只有密码错误增加 data。
- **安全与隐私要求：** 不记录或序列化 fixedValue、提交密码、完整 generator；raw HTTP 例外不在本 Ticket 扩大。

## 6. 执行路线

1. 以表驱动测试冻结 v1 配置、每个 violation、random/fixed 和敏感投影边界。
2. 建立 system password policy 模型、解析器、验证器与 CSPRNG generator。
3. 建立通用结构化业务违规到 R data 的异常映射，保持非密码异常兼容。
4. 把策略 key 的保存与缓存替换接入 validate-before-swap 和 T-01 跨节点失效。
5. 扩展 AuthClientContextVo/Controller 并验证敏感字段零泄露。
6. 运行定向、admin/system/common reactor 与 API 合同测试。

## 7. 路径访问契约

- **预计修改点：** common error、system password package/config service、Auth context VO/controller、专用 tests。
- **可写范围：** frontmatter 精确路径。
- **只读上下文：** ISysConfigService、RegexConstants、T-01 common cache contract。
- **共享路径：** common error 与 system password 包由 T-03 独占，下游只读调用。
- **保留或不动：** user/profile/register controllers、PasswordAuthStrategy、SQL、前端生成物。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | policy/API test | random/fixed、context GET | 合规候选与安全 projection，字段精确 | `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/evidence/T-03.md</Path>` |
| 失败路径 | table-driven config/error | 弱 fixed、坏 JSON、空池、越界、组合违规 | 保存拒绝、旧策略保留、稳定 reasons、无秘密 | 同上 |
| 回归 | Maven reactor | `./mvnw -pl ruoyi-common/ruoyi-common-core,ruoyi-common/ruoyi-common-web,ruoyi-modules/ruoyi-system,ruoyi-admin -am test` | 既有 R/配置/Auth context 兼容 | 同上 |

- **Workspace checks：** Goal Plan current/source-worktree 非 E2E 检查。
- **E2E disposition：** required：公共未认证 API、跨 JVM 配置缓存和敏感字段边界需要真实 Spring/Redis 接缝。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate；验证两个节点配置刷新、公开 JSON 和无敏感字段。
- **Integration evidence：** backend commit、parent/candidate/result SHA 与父 gitlink。

## 9. 发布、迁移与恢复

- **迁移顺序：** T-01 先集成；T-08 才写生产 DML。本 Ticket 在配置缺失时失败关闭新能力但不影响永久登录。
- **兼容窗口：** 后端 additive 字段先行，旧前端忽略；新前端必须等 T-04/T-05 API 完整后发布。
- **监控信号：** policy parse/save failure、cache version/refresh、generation failure，不带 config value。
- **回滚或前向恢复：** 配置更新失败保留 last-known-good；代码回滚前恢复旧配置语义并禁止新前端。
- **不可逆操作与批准点：** 无生产配置写入；commit/integration/deploy 另行授权。
- **收缩条件：** T-08 部署后新代码/调用点对 `sys.user.initPassword` 零读取。

## 10. 验收标准

- [x] `AC-001`、`AC-003` 的生成与详细错误核心可复用。
- [x] `AC-015` 的公开投影字段精确且无内部配置。
- [x] `AC-019` 的非法配置拒绝、last-known-good 和跨节点刷新成立。
- [x] 验证矩阵完整；修改未越界；backend commit、Lead 集成/E2E/父状态已记录。
