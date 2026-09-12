---
schema_version: 3
artifact: ticket
change: 2026-08-31-account-profile-verification
id: T-03
title: 交付个人与企业认证供应商策略及回调证据框架
status: done
planning_depth: deep
planning_depth_reason: 供应商签名、时间窗、回调幂等和敏感证据属于外部安全协议与公共扩展点。
ready: true
risk: high
blocked_by: [T-01, T-02]
contract_ids: [AC-036, AC-037]
owner: codex:/root
expected_changes:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-person/pom.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-enterprise/pom.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-person/src/main/java/org/dromara/profile/person/verification/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-enterprise/src/main/java/org/dromara/profile/enterprise/verification/**</Path>"
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-person/pom.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-enterprise/pom.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-person/src/main/java/org/dromara/profile/person/verification/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-enterprise/src/main/java/org/dromara/profile/enterprise/verification/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-person/src/test/java/org/dromara/profile/person/verification/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-enterprise/src/test/java/org/dromara/profile/enterprise/verification/**</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/profile/api/**</Path>"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/50-namewta-ddl.sql</Path>"
shared_paths: []
shared_path_owners: []
---

# Ticket T-03: 交付个人与企业认证供应商策略及回调证据框架

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/03-verification-provider-framework.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-03.md</Path>`

## 1. 战略与来源

- **目标：** 为 manual 及未来支付宝、微信、微警建立不污染档案核心的稳定适配边界。
- **可观察产出：** 服务端按 profileType/providerCode 选择唯一启用策略，重试新增 attempt；合法回调幂等记录证据，伪造/冲突/迟到回调不改业务状态。
- **来源：** `US-019`、`AC-036`、`AC-037`、`ADR-004`、`ADR-013`。
- **当前事实：** 仓库没有 profile provider SPI 或回调持久化。
- **Planning Depth 原因：** 外部鉴权、幂等键和敏感原文边界影响安全与后续兼容。

## 2. 决策状态

### 已锁定决策

- person/enterprise SPI 分离，manual 默认；一次申请固定一个启用 provider。
- 失败只允许显式重试并追加 attempt，不自动降级或组合。
- 适配器负责签名/时间窗，领域按 `(providerCode, providerRequestId)` 幂等。
- 同内容重复成功；冲突或终态后迟到只写安全审计，任何回调不发布档案。

### 已采用的低影响假设

- 测试 provider 使用确定性签名协议，仅用于验证 SPI，不宣称任何真实供应商协议。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| SPI、注册表、manual/test provider、attempt、回调鉴权/幂等 | Spring Bean 注册、现有异常映射与审计 | 真实支付宝/微信/微警 SDK、自动通过 |

## 4. 要构建什么

调用者选择服务端启用 provider 后得到规范化证据；回调入口只把经过适配器验证的结果交给领域 attempt，重复网络投递不产生第二条决定，终态申请不被重新打开。

## 5. 实现契约

- **入口或接缝：** 两类 VerificationProvider registry、retry command、provider callback adapter。
- **输入与输出：** providerCode+申请/回调 -> attempt 与规范化证据/稳定失败类别。
- **公共接口变化：** 新增供应商回调 HTTP 适配入口；供应商专属 payload 不进入档案 API。
- **不变量：** provider 固定、attempt 只追加、回调不发布、幂等键唯一。
- **状态或数据流：** adapter 鉴权 -> 规范化 -> attempt 行锁/唯一键 -> evidence/audit。
- **错误与失败行为：** 未知/禁用、伪造、过期、冲突、迟到均失败关闭或仅审计。
- **兼容要求：** 新 provider 只实现 SPI，不改个人/企业状态机。
- **安全与隐私要求：** 原始签名、凭据和敏感 payload 不进持久日志。

## 6. 执行路线

1. 以 manual/test provider 建立选择、重试和回调失败矩阵。
2. 实现两类 SPI/注册表和启用配置校验。
3. 实现 attempt 追加、规范化证据和领域幂等。
4. 增加回调 adapter 签名/时间窗接缝与安全日志。
5. 运行模块、MockMvc/DB 幂等及回归测试。

## 7. 路径访问契约

- **预计修改点/可写范围：** 两叶子 verification 子树、对应测试与仅用于 test-scope 依赖的叶子 POM。
- **只读上下文：** T-01 公共端口与 T-02 schema。
- **共享路径：** 无。
- **保留或不动：** 申请发布、workflow 和档案绑定。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常 | provider contract | manual/test provider 启动、重试、合法回调 | 固定 provider，attempt/证据只追加 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-03.md</Path>` |
| 失败 | callback matrix | 伪造、过期、重复冲突、迟到、未知 provider | 不发布档案且无敏感日志 | 同上 |
| 回归 | 两叶子模块测试 | Maven 定向 test | person/enterprise 策略互不串用 | 同上 |

- **Workspace checks：** 所选 workspace 执行两叶子测试与静态日志扫描。
- **E2E disposition：** required：HTTP callback、数据库幂等和适配器鉴权需在集成环境联合验证。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate，使用 test provider。
- **Integration evidence：** commit、parent/candidate/result SHA 和 callback 结果。

## 9. 发布、迁移与恢复

- **迁移顺序：** schema 已存在后部署 manual/test 框架；真实 provider 独立发布。
- **兼容窗口：** providerCode 含义发布后不可改写。
- **监控信号：** 未知、签名失败、冲突、迟到 attempt 计数。
- **回滚或前向恢复：** 禁用 provider 并保留 attempts；前向修复 adapter。
- **不可逆操作与批准点：** 真实供应商启用/密钥下发未授权。
- **收缩条件：** 不适用：无旧 provider。

## 10. 验收标准

- [x] AC-036/037 全矩阵通过且回调不能发布档案。
- [x] required E2E、敏感日志扫描与 Evidence 完整。
- [x] 实际路径、提交和父分支结果符合合同。
