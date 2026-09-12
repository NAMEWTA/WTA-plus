# ADR-0022: 统一密码策略使用 sys_config 类型化配置

- **Status:** Accepted
- **Date:** 2026-08-29
- **Source:** `<Path>{roots.state}/specdev/archive/2026-08/2026-08-28-user-password-policy-temporary-credentials/ADR.md</Path>` ADR-001

## Context

NAMEWTA 的默认密码配置原本由 `sys_config` 承载。统一密码策略还需要原子表达随机生成规则、固定默认密码兼容、密码强度和跨字段约束；把这些内容拆成 `sys_dict` 条目会混淆运行配置与枚举翻译职责，也无法提供可靠的结构约束。

## Decision

统一密码策略继续由 `sys_config` 中的类型化配置对象承载，支持 `random` 和 `fixed` 模式。服务端在解析、保存、生成和所有密码写入边界执行同一组类型与安全不变量校验；固定密码也必须满足强度基线。未认证端点只发布表单校验需要的非敏感策略投影，不返回固定密码或随机生成内部配置。

## Consequences

注册、管理员新增、Excel 导入、管理员重置、个人改密、随机生成和临时凭据共享同一服务端策略权威。配置迁移必须兼容 fresh 与 upgrade 数据库并验证畸形、缺字段和弱固定值；存量弱密码仍可登录，但下一次写入必须收敛到当前策略。
