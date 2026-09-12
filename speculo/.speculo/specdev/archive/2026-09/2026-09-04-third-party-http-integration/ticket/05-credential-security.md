---
schema_version: 3
artifact: ticket
change: 2026-09-04-third-party-http-integration
id: T-05
title: 交付分 scope 凭据与 AES-GCM 密文管理
status: done
planning_depth: deep
planning_depth_reason: 凭据写入、覆盖、解密和日志边界涉及高敏感数据与主密钥外置，任何失败必须在发送前关闭。
ready: true
risk: high
blocked_by: [T-04]
contract_ids: [AC-005, AC-016]
owner: codex:/root
expected_changes:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/security/credential/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/usecase/credential/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/controller/admin/credential/**</Path>"
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/security/credential/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/usecase/credential/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/service/credential/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/controller/admin/credential/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/domain/bo/credential/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/domain/vo/credential/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/test/java/org/dromara/third/credential/**</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-encrypt/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/config/properties/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/mapper/**</Path>"
shared_paths: []
shared_path_owners: []
---

# Ticket T-05: 交付分 scope 凭据与 AES-GCM 密文管理

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/05-credential-security.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-05.md</Path>`

## 1. 战略与来源

- **目标：** 提供 Provider/Endpoint 分离、同类型覆盖且永不回显明文的凭据管理与运行时解密接缝。
- **可观察产出：** 管理员能替换/停用凭据并只看到掩码元数据；DB 仅有 AES-GCM 认证密文，主密钥缺失、篡改或版本未知时调用 fail-closed。
- **来源：** `US-004`、`US-008`、`AC-005`、`AC-016`、`ADR-003`、`ADR-008`。
- **当前事实：** T-02 有 envelope 持久化列，T-01 有外部主密钥配置入口，尚无安全 codec 和管理路径。
- **Planning Depth 原因：** 涉及 API key/secret、签名私钥与加密密钥，泄漏或静默解密错误事故半径高。

## 2. 决策状态

### 已锁定决策

- Provider 与 Endpoint 凭据分 scope；Endpoint 同类型覆盖 Provider，不做字段级拼接。
- 使用随机 nonce 的 AES-GCM 类认证加密 envelope；主密钥只从 yml/环境外部配置读取。
- 保存 API 不记录请求体且不回显明文；首期保留 keyVersion/algorithm 等轮换字段，不实现在线轮换。
- 解密仅发生在受控调用 pipeline 的最短生命周期内。

### 已采用的低影响假设

- 若现有 encrypt 模块无满足认证加密的公共 API，可在 third 内封装 JCA AES/GCM，但不得削弱算法或复制通用密码工具。

### 未决问题

无。

## 2.1 必须加载的 Skill 与工程基线

- **必须加载：** <Path>.agents/skills/engineering-standards/SKILL.md</Path>、<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>、<Path>.agents/skills/ruoyi-module-guide/SKILL.md</Path>、<Path>.agents/skills/ruoyi-common-modules-guide/SKILL.md</Path>。
- **必须先读的参考：** <Path>.agents/skills/engineering-standards/references/project/00-project-profile.md</Path>、<Path>.agents/skills/engineering-standards/references/project/01-module-map.md</Path>、<Path>.agents/skills/engineering-standards/references/project/03-backend-module-modes.md</Path>、<Path>.agents/skills/engineering-standards/references/java/security-and-data.md</Path>、<Path>.agents/skills/namewta-fullstack-development/references/backend/framework-usage.md</Path>、<Path>.agents/skills/namewta-fullstack-development/references/backend/implementation.md</Path>。
- **目录与代码最低要求：** credential codec/resolver 只能作为 layered Service 的明确端口或受控 support，Controller 只进 UseCase；密文 DTO、BO、VO、Entity 分离，任何 secret 不得进入 ruoyi-api、toString、异常或 @Log。公共加密入口按 common-modules-guide 的准确 FQN 复用，不复制同义 EncryptUtils。
- **密码学与持久化要求：** 使用认证加密 envelope、随机 nonce、版本/算法字段和主密钥外置；DB 只存密文。事务写入遵守 @DSTransactional，查询/变更遵守 GET/POST，测试必须包含篡改、错误 key、版本未知和 canary 扫描。
- **执行停止条件：** 主密钥写入代码/DB、固定 IV、明文回显、日志记录 secret、在 Controller/DAO 直接解密、或为方便修改 common-encrypt 公共契约时立即停止并升级 T-05/Lead。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| 密文 codec、scope 解析、替换/停用 Admin API、掩码 VO | JCA/可用 Encrypt 公共能力、T-02 Mapper、T-01 配置 | Secret Manager、在线轮换、明文查询/导出、凭据列表页面 |

## 4. 要构建什么

管理员从 Provider 或 Endpoint 编辑流程提交新的秘密值，系统在事务外不保留明文副本，使用外部主密钥生成认证密文后写库；后续解析时 Endpoint 同类型值完整覆盖 Provider。读取、列表、异常和日志都只显示类型、状态、版本、更新时间和固定掩码。

## 5. 实现契约

- **入口或接缝：** 嵌套 Provider/Endpoint credential Admin Controller、Credential UseCase、codec/resolver。
- **输入与输出：** scope、type、一次性 secret 输入，返回无 secret/ciphertext 的元数据 VO；运行时内部返回短生命周期值。
- **公共接口变化：** 无。
- **不变量：** 主密钥不落 DB；nonce 不复用；认证失败不返回部分明文；同 scope/type 唯一。
- **状态或数据流：** 输入 → AES-GCM envelope → DB；调用时 DB ciphertext → 验证/解密 → pipeline 使用 → 释放引用。
- **错误与失败行为：** 缺主密钥、坏 Base64、未知版本、tag 篡改、disabled credential 均返回 CONFIG_UNAVAILABLE 且不发送 HTTP。
- **兼容要求：** keyVersion 为后续读取兼容保留，首期只有一个明确支持版本。
- **安全与隐私要求：** `@Log` 排除全部 secret 输入；异常、toString、VO、测试快照和日志不得出现 canary。

## 6. 执行路线

1. 先建立密文非确定性、篡改、错误主密钥和 canary 泄漏测试。
2. 实现版本化 AES-GCM envelope codec 与配置校验。
3. 实现 scope/type resolver 和 Endpoint 覆盖规则。
4. 实现替换/停用 UseCase、嵌套路由、权限与安全日志。
5. 运行单元、MVC、MySQL 和日志 canary 扫描。

## 7. 路径访问契约

- **预计修改点/可写范围：** credential security/usecase/service/controller/BO/VO/test 子树。
- **只读上下文：** common-encrypt、T-01 properties、T-02 Mapper。
- **共享路径：** 无；application.yml、Provider/Endpoint Controller 与 DDL 不改。
- **保留或不动：** 真实 secret、外部 Secret Manager、独立凭据菜单。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | codec/MVC/MySQL | 写入 Provider 凭据并以 Endpoint 同类型覆盖 | DB 仅密文且解析结果遵守覆盖 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-05.md</Path>` |
| 失败路径 | codec/executor seam | 篡改 tag、错 key、缺 key、未知版本、越权 | fail-closed、零 HTTP、零明文输出 | 同上 |
| 回归 | canary scan | Maven 测试并搜索日志/序列化结果 | canary 不出现在响应和持久日志 | 同上 |

- **Workspace checks：** source/current workspace 运行 credential 定向测试与敏感字符串扫描。
- **E2E disposition：** required：需在真实配置绑定、MVC、MySQL 和日志 sink 边界验证秘密不泄漏。
- **E2E owner/environment：** Lead / parent-candidate 或 current-workspace；使用仅测试 canary，不使用生产密钥。
- **Integration evidence：** implementation/source commit、parent before、candidate/result SHA、测试密钥来源和父分支包含关系。

## 9. 发布、迁移与恢复

- **迁移顺序：** 配置主密钥 → 部署 codec/API → 管理员写入密文 → 启用 Endpoint。
- **兼容窗口：** 支持版本字段；未知版本关闭而非猜测解密。
- **监控信号：** 缺 key、认证 tag 失败、未知版本、重复 scope/type 和泄漏 canary。
- **回滚或前向恢复：** 禁用相关 Provider/Endpoint；保留 ciphertext 后前向修复 codec/config。
- **不可逆操作与批准点：** 生产主密钥和凭据写入不在本 Ticket 执行，需运维批准。
- **收缩条件：** 不适用：首期无旧明文凭据。

## 10. 验收标准

- [x] `AC-005`：scope 覆盖、认证密文和 fail-closed 全部成立。
- [x] `AC-016`：管理权限和后端明文防线可判定。
- [x] 测试 canary 未出现在 API、异常、日志或 DB 明文字段。
- [x] required E2E、提交和集成 Evidence 完整。
