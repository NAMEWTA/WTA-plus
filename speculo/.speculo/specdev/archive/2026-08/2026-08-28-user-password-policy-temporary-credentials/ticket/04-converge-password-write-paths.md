---
schema_version: 3
artifact: ticket
change: 2026-08-28-user-password-policy-temporary-credentials
id: T-04
title: 收敛全部密码写入并提供可编辑重置候选
status: done
planning_depth: deep
planning_depth_reason: 注册、管理员新增/重置、Excel 导入和个人改密横跨 admin/system/api，涉及公共认证、敏感数据和既有会话兼容。
ready: true
risk: high
blocked_by: [T-03]
contract_ids: [AC-001, AC-002, AC-003, AC-004, AC-016, AC-017, AC-018]
owner: codex:lead
expected_changes: ["<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/system/api/model/RegisterBody.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/java/org/dromara/web/service/SysRegisterService.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysUserController.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysProfileController.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/listener/SysUserImportListener.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/password/write/**</Path>"]
writable_paths: ["<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/system/api/model/RegisterBody.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/java/org/dromara/web/service/SysRegisterService.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysUserController.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysProfileController.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysUserCredentialController.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/domain/bo/password/ResetPasswordCandidateBo.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/domain/vo/password/ResetPasswordCandidateVo.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/listener/SysUserImportListener.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/password/write/**</Path>"]
read_only_paths: ["<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/password/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysUserServiceImpl.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-log/src/main/java/org/dromara/common/log/annotation/Log.java</Path>"]
shared_paths: []
shared_path_owners: []
---

# Ticket T-04: 收敛全部密码写入并提供可编辑重置候选

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/ticket/04-converge-password-write-paths.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/evidence/T-04.md</Path>`

## 1. 战略与来源

- **目标：** 让五类新密码写入在 BCrypt 前调用 T-03 同一策略，并让管理员重置先获取服务端候选、编辑确认后才真正更新。
- **可观察产出：** 公开注册、管理员新增、Excel 新用户导入、管理员重置和个人改密均拒绝弱值并返回具体原因；重置候选获取不改密码，确认后仍不踢既有会话。
- **来源：** `US-001`、`US-004`、`US-005`、`US-007`、`AC-001` 至 `AC-004`、`AC-016` 至 `AC-018`。
- **当前事实：** 各入口当前有 5/6 位不同规则或无服务端规则；import 在构造器读取同一个 initPassword；reset 直接 BCrypt 后更新，且不注销会话。
- **Planning Depth 原因：** 跨模块认证/用户写入、Excel 批处理、敏感响应和存量会话兼容均为安全合同。

## 2. 决策状态

### 已锁定决策

- 五类写入必须在明文阶段调用 T-03 validator，禁止各自复制 regex。
- `POST /system/user/resetPwd/candidate` 只生成并返回候选，执行 resetPwd 相同 allowed/data scope，不修改用户，`Cache-Control: no-store`。
- 最终 reset 继续 `PUT /system/user/resetPwd` 兼容现有调用；最终值重新验证，成功不注销会话。
- admin add 的既有初始化响应返回当前策略候选；Excel RANDOM 每个新用户独立生成，更新已有用户不改密码，摘要不回传明文。
- POST candidate 使用安全 `@Log`，请求/响应敏感数据不落数据库审计。

### 已采用的低影响假设

- 新 candidate controller 与现有 SysUserController 使用同一 `/system/user` 根但独立类，降低大文件职责与后续临时接口路径冲突。验证：Spring mapping 启动测试。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| 五入口校验、candidate API、逐用户 import 生成、详细错误、会话兼容测试 | T-03 policy/error、BCrypt、allowed/data scope、现有 R | 不实现临时密码，不改旧登录强度，不写前端/SQL |

## 4. 要构建什么

管理员打开重置时先请求候选，取消不会产生任何写入；编辑后提交，服务端按提交时策略验证并更新。注册、管理员新增、导入与个人改密即使绕过前端也无法写入弱值。存量弱 BCrypt 仍可登录；只有下一次写入收敛。所有失败保持数据库原值与事务完整性。

## 5. 实现契约

- **入口或接缝：** candidate POST、现有 reset PUT、user add POST、auth register POST、profile updatePwd PUT、Excel listener。
- **输入与输出：** candidate `{userId}` -> `{password}`；违规 -> R data.violations；其他请求形状不变。
- **公共接口变化：** 新增 candidate；既有接口只收紧弱值并增加详细错误。
- **不变量：** validate-before-BCrypt；candidate 无持久状态；最终提交权威；reset/profile 不新增 session kick。
- **状态或数据流：** request plaintext -> policy validate/generate -> BCrypt -> existing service/transaction -> R。
- **错误与失败行为：** 多违规全部返回；candidate/策略失败不写用户；import 单行失败进入现有逐行摘要且不泄露生成值。
- **兼容要求：** 存量弱密码登录兼容；HTTP path/method 除新 POST 外保持；reset 会话行为不变。
- **安全与隐私要求：** candidate no-store；`@Log` 不保存密码；普通业务日志不输出明文，ADR-0017 例外保持原边界。

## 6. 执行路线

1. 为五入口建立弱值红色测试和 reset 会话兼容基线。
2. 新增 candidate transport/controller，完成权限、data scope、no-store 与审计脱敏。
3. 将 add/reset/profile/register 接入 T-03 policy，移除分叉静态强度判断。
4. 重构 Excel 新用户按行生成，保持 update 与结果摘要语义。
5. 覆盖策略变化、取消候选、事务失败、存量弱登录/下次改密收敛。
6. 运行 admin/system/api reactor 和真实数据库集成。

## 7. 路径访问契约

- **预计修改点：** RegisterBody/SysRegisterService、user/profile/candidate controllers、import listener、专用 tests。
- **可写范围：** frontmatter 列出的精确源码和新 password BO/VO 目录。
- **只读上下文：** T-03 policy、SysUserServiceImpl、Log annotation。
- **共享路径：** 无；不得修改 T-03 owned password/error 实现。
- **保留或不动：** PasswordAuthStrategy、ClientSessionService、DML、前端。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | API/service integration | 五入口提交合规值、candidate 编辑确认 | BCrypt 写入正确，candidate 不先写，import 每用户独立 | `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/evidence/T-04.md</Path>` |
| 失败路径 | direct API/batch | 每类违规、策略变化、单行失败 | 原值不变、reasons 稳定、无明文摘要 | 同上 |
| 回归 | Maven + session | `./mvnw -pl ruoyi-api,ruoyi-modules/ruoyi-system,ruoyi-admin -am test` | 存量弱登录可用，reset 既有会话保持 | 同上 |

- **Workspace checks：** Goal Plan current/source-worktree 运行定向 Maven 非 E2E。
- **E2E disposition：** required：公开注册、数据库写入、Excel 批处理和会话兼容需真实 Spring/MySQL 接缝。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate；直接 API 绕过前端验证，并核对 DB/Token。
- **Integration evidence：** backend commit、candidate/direct-parent/result SHA、父 gitlink。

## 9. 发布、迁移与恢复

- **迁移顺序：** T-03 先集成；T-08 配置 DML 与后端部署同一发布窗口；前端后发。
- **兼容窗口：** 弱密码写调用会有意失败；旧读/登录兼容。滚动期间先阻止旧节点写弱默认。
- **监控信号：** violations reason、candidate 失败、入口/导入失败率，不记录密码。
- **回滚或前向恢复：** 保持 last-known-good 策略；回滚需同时回滚依赖新 candidate 的前端并恢复安全默认配置。
- **不可逆操作与批准点：** 永久密码确认写入不可自动恢复，沿用管理员显式确认；迁移/部署需授权。
- **收缩条件：** `sys.user.initPassword` 新代码读取为零，由 T-08 扫描证明。

## 10. 验收标准

- [x] `AC-001` 至 `AC-004` 的候选、确认、错误和会话兼容成立。
- [x] `AC-016` 至 `AC-018` 的五入口与存量收敛成立。
- [x] 业务审计/导入摘要无密码，直接 API 无旁路。
- [x] 验证、backend commit、Lead 集成/E2E/父状态完整。
