---
schema_version: 3
artifact: spec
change: 2026-08-29-login-password-policy-runtime-and-http-log-redaction
status: ready
ready_for_tickets: true
sources:
  - USER-REPORT:2026-08-29-login-runtime-failure
  - DIAG-001:missing-live-password-policy-config
  - SEC-001:http-log-credential-exposure
---

# Spec: 恢复登录上下文并阻止 HTTP 日志泄露凭据

## 1. 问题与目标

当前本地数据库缺少已交付的密码策略配置，使登录前置 context 返回 `PASSWORD_POLICY_UNAVAILABLE`；同时公共 Servlet 日志原样记录认证头和 JSON 凭据字段。目标是恢复当前本地环境的登录前置 context，并在统一日志接缝隐藏认证、会话、密钥、密码、token 和验证码，不改变认证业务、用户密码或前端代码。

## 2. 解决方案与外部行为

应用 `<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DML.sql</Path>` 已有的密码迁移块并使配置缓存失效。日志过滤器保留普通请求/响应元数据，但把敏感头值替换为 `[REDACTED]`；对 JSON 日志副本递归脱敏，解析失败或截断时整段隐藏。业务请求、业务响应和认证结果保持不变。

## 3. 用户故事

- 作为本地开发者，我能加载登录页客户端上下文并继续使用既有账号登录。
- 作为运维和开发者，我能保留 HTTP 诊断元数据，同时避免凭据进入日志。

## 4. 验收合同

| ID | 合同 | 验证接缝 |
|---|---|---|
| AC-001 | `/auth/client/context` 返回业务码 200、`clientEnabled=true` 和合法密码策略 projection | live HTTP probe |
| AC-002 | 旧 token 继续返回登录状态异常，不通过放宽认证修复 context | 既有认证行为与代码审阅 |
| AC-003 | 认证、会话和密钥类头值及敏感请求参数记录为 `[REDACTED]` | `SysLogFilterTest` |
| AC-004 | JSON 中密码/token/secret/session/captcha 类字段递归脱敏；非法或截断 JSON 整段隐藏；业务正文不变 | `SysLogFilterTest` |
| AC-005 | common-web 全量测试、admin reactor package、前端与后端 live probe 通过 | Maven 与 HTTP |

## 5. 范围

- **IN：** live MySQL/Redis 本地状态、`ruoyi-common-web` filter/media policy/sanitizer/test、定向与 reactor 验证。
- **OUT：** JWT 签名/会话存储、密码验证规则、用户数据、前端产品源码、生产部署、commit/push。
- **REUSE：** 既有 DML、`SysLogBody` 元数据、Jackson `JsonMapper`、Maven Wrapper 和当前运行服务。

## 6. 已锁定实现约束

- 复用 `<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DML.sql</Path>` 的 append-only 密码迁移块，不另造漂移 SQL。
- 敏感头按名称大小写无关判定；敏感参数复用 JSON 字段名称策略；普通头、参数和非 JSON 文本继续保留现有可观测性。
- JSON 字段名匹配忽略大小写、连字符和下划线，并递归覆盖对象与数组。
- JSON 解析失败必须 fail closed，不能回退输出可能含凭据的原文。
- 只改日志副本；Servlet request/response 和加解密 wrapper 的真实内容保持原样。

## 7. 数据、接口与兼容

- **公共接口：** 无字段或路由变化；context 从运行时错误恢复到既有成功合同。
- **数据：** 本地环境补齐既有策略与权限配置，不修改 `sys_user.password`。
- **兼容：** 旧 token 继续无效；普通日志字段与正文采集策略保持兼容。

## 8. 非功能要求

- **安全：** 凭据不进入 HTTP 日志；脱敏失败时关闭正文可见性。
- **可靠性：** 日志处理失败不得改变业务 request/response。
- **性能：** 只对已采集且判定为 JSON 的受限正文副本解析一次。

## 9. 验证策略

| 接缝 | 层级 | 覆盖合同 | Evidence |
|---|---|---|---|
| `/auth/client/context` | live HTTP | AC-001、AC-002 | HTTP 状态与业务 envelope |
| `SysLogFilterTest` | 单元/Servlet 集成 | AC-003、AC-004 | TDD red/green 与 16-test suite |
| Maven reactor | 跨模块构建 | AC-005 | 35-module package summary |
| 前端入口与后端 context | 当前运行态 | AC-005 | 两个 HTTP 200 probe |

## 10. 风险、迁移与回滚

- 数据迁移按既有脚本随机化已废弃的初始化密码配置并补充临时密码权限；不输出生成值，不修改任何 `sys_user.password`。
- Redis 只删除精确的 `sys_config` cache map，使数据库权威值即时可见。
- 脱敏会降低敏感字段的日志可见性，这是目标安全行为；普通字段仍可用于排障。
- 代码回滚可撤销 common-web 四个受管文件；数据不应回滚到缺失策略的不可登录状态。
