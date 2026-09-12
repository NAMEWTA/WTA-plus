---
schema_version: 1
artifact: diagnosis
change: 2026-08-29-login-password-policy-runtime-and-http-log-redaction
status: root-cause-confirmed
feedback_loop_ready: true
red_command: "curl -H 'clientid: <configured-client-id>' http://127.0.0.1:8080/auth/client/context"
red_evidence: "HTTP 200 envelope with code=500 and msg=PASSWORD_POLICY_UNAVAILABLE"
cleanup_status: clean
updated_at: 2026-08-29T14:00:00+08:00
---

# Diagnosis: 登录页客户端上下文无法加载

## 1. 现象与影响

- 旧 Bearer token 请求 `/system/user/getInfo` 返回业务码 401，前端按既有行为退出并回到登录页。
- 登录页随后请求 `/auth/client/context`，后端由 `PasswordPolicyConfigParser` 报告 `sys.user.passwordPolicy` 不可用并返回 `PASSWORD_POLICY_UNAVAILABLE`，登录前置上下文无法加载。
- 同一批 HTTP 日志原样记录了 `Authorization`、`Cookie`；代码检查进一步确认登录 JSON 的密码和 token 字段也会原样进入日志。

## 2. 红灯反馈回路

- **命令：** `curl -H 'clientid: <configured-client-id>' http://127.0.0.1:8080/auth/client/context`
- **至少一次真实输出：** HTTP 200，响应 envelope 为 `code=500`、`msg=PASSWORD_POLICY_UNAVAILABLE`。
- **精确症状断言：** 无认证的登录前置接口不能返回客户端启用状态和密码策略。
- **耗时：** 约 54 ms；后续复现约 7 ms。
- **确定性/复现率：** 2/2。
- **Agent 可运行性：** autonomous。
- **无法建立时已尝试方式和所需输入：** 不适用。

## 3. 最小复现

- **环境与输入：** 当前运行中的 8080 后端、配置的 client id、MySQL `sys_config` 与 Redis `sys_config` cache map。
- **剩余步骤：** 单次调用公开的 `/auth/client/context`；无需 Bearer token、用户密码或前端状态。
- **逐项删除证据：** 删除旧 token 后仍稳定失败，排除 session 401；数据库只读查询显示策略行数为 0，问题不依赖具体用户。
- **最后红灯证据：** `policy row_count=0`，接口稳定返回 `PASSWORD_POLICY_UNAVAILABLE`。
- **捕获物：** 用户提供的服务日志与本 change Evidence 中的结构化摘要。

## 4. 假设与证伪

| 排名 | 假设与预测 | 支持证据 | 单变量实验 | 结果 |
|---|---|---|---|---|
| 1 | 本地库未应用已交付的密码策略 DML；补齐策略行后接口转绿 | live row count 为 0，DML 已包含合法 v1 JSON | 只执行既有密码迁移块并清理对应 cache map | confirmed：接口返回 code 200 与 v1 projection |
| 2 | 策略行存在但 JSON 损坏；修正 JSON 后转绿 | parser 抛出 IllegalArgumentException | 只读查询 row count、JSON_VALID、长度与 version | rejected：策略行完全不存在，不是损坏 |
| 3 | Redis 缓存保留旧空值；只清缓存即可转绿 | 配置服务使用 `sys_config` cache map | 迁移前核对数据库权威行 | rejected as root cause：数据库同样缺行；迁移后仍需清缓存保证即时可见 |
| 4 | 旧 token 或 client 被禁用导致 context 失败 | 同时观察到 user/getInfo 401 | 不带 token 调 context，并核对 clientEnabled | rejected：context 不依赖 token，迁移后 clientEnabled=true |

## 5. 已确认根因

- **触发条件：** 当前本地数据库未执行 `script/sql/namewta/DML.sql` 中已经交付的密码策略与临时密码权限迁移块。
- **失败机制：** `PasswordPolicyService` 读取空配置，严格 parser 按设计 fail closed，导致登录页公开 context 返回 500 envelope。
- **根因位置：** live MySQL `sys_config` 数据状态；权威迁移位于 `<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DML.sql</Path>`。
- **漏检原因：** 编译与启动验收验证了代码/进程可用性，但没有验证已有本地数据库是否追上新 DML。
- **为何排除其他候选：** client 有效，context 无需 token；策略行在迁移前不存在，迁移与 cache invalidation 后同一请求立即转绿。
- **确认实验：** row count 0 -> 应用既有迁移块 -> 删除 `sys_config` cache map -> row count 1、JSON_VALID=1、version=1 -> context code 200。

## 6. 修复契约

- **必须改变：** 当前本地库应用既有密码迁移块，并删除配置 cache map；HTTP 日志不得记录认证/会话头或 JSON 凭据字段原值。
- **必须保持：** 旧 token 继续失效；现有用户密码 hash、认证决策、HTTP 业务响应、加解密正文和前端退出流程不变。
- **正确测试 seam：** live `/auth/client/context`；`<Path>ruoyi-common/ruoyi-common-web/src/test/java/org/dromara/common/web/logging/SysLogFilterTest.java</Path>`。
- **回归测试：** context 返回 code 200；敏感头、完整/嵌套 JSON、截断 JSON 在日志副本中脱敏，业务副本保持原值。
- **OUT：** 不复活旧 JWT，不修改用户密码，不放宽密码策略 parser，不修改前端产品代码。
- **风险与回滚：** 数据变更采用已交付 append-only DML；代码可按四个 common-web 文件回滚，数据不应为恢复旧故障而回滚。
- **推荐下游：** `I-implement` Direct Spec。

## 7. 清理

- **原始回路重跑：** 同一 context 命令返回 HTTP 200、业务码 200 和有效密码策略 projection。
- **`[DEBUG-...]` 搜索：** 未添加临时插桩。
- **一次性脚本/原型：** 无；迁移直接复用受版本控制的 DML 块。
- **未清理项 owner 与删除条件：** 无。
