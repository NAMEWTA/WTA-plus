# ADR-0036: 面向 Client 无关接口的 OpenAPI 全局身份

- **Status:** Accepted
- **Date:** 2026-09-02
- **Source:** `<Path>{roots.state}/specdev/archive/2026-08/2026-08-30-openapi-common-module/ADR.md</Path>` ADR-008、ADR-018、ADR-019

## Context

`sys_client` 约束浏览器登录入口、动态路由和请求策略，而机器调用不需要前端路由。同一用户为各 Client 维护多套凭据会破坏统一调用体验；让调用方选择或让服务端猜测 Client 又会在重复权限和 Client 专属能力上产生歧义。

## Decision

OpenAPI 是独立认证通道，每个 AppKey 只绑定 userId，不创建或伪装专用 `sys_client`，调用协议不接受 `clientid`。全局身份只聚合该用户可合法登录的有效 Client：Client、登录域、关系、默认角色、显式角色和菜单均按当前正常状态过滤，并形成标准 `LoginUser` 权限及数据范围输入。首期只有执行语义不依赖单一 `clientPk/clientKey` 的方法级接口可以标注 `@OpenApi`；运行时不选择、不推断也不填充默认 Client。

## Consequences

普通前端 Token 的单 Client 约束保持不变。OSS 上传、Client 级路径/IP 策略等依赖唯一 Client 的能力必须失败关闭，除非后续 change 定义 Client 无关合同。开放接口目录和真实调用必须使用同一全局身份规则。
