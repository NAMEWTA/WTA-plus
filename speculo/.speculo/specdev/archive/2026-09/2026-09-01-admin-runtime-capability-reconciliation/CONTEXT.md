# Change Context: Admin 运行能力收敛

## 术语

- **OpenAPI 管理面：** `/system/openApi/**` 的凭据与接口目录 HTTP API，以及 Admin 的个人 OpenAPI Tab 和系统管理菜单页面。
- **OpenAPI 调用面：** 使用 AppKey/AppSecret 签名访问方法级 `@OpenApi` 接口的机器调用网关。
- **运行能力最终态：** OpenAPI 运行配置、数据库菜单/schema、前端 manifest 和实际双实例共同表达同一可用能力。
- **生成器退役最终态：** 活动源码、HTTP、前端页面、菜单权限、`gen_table` 与 `gen_table_column` 均不存在。
- **开发目标环境：** 部署档案中由 NAMEWTA 管理的 `172.16.105.9` 环境；同机 CDE 不在范围内。

## 已确认事实

- OpenAPI 代码和前端页面已经存在，且合法启用态的组装测试通过。
- OpenAPI 代码级默认值必须继续为关闭；只有受管环境可通过外部配置显式启用。
- 当前开发数据库处于混合迁移状态：OpenAPI 表存在但菜单缺失，Nacos 菜单已存在，生成器菜单和表仍存在。
- Nacos 仍是独立鉴权的官方控制台 iframe，不属于 RuoYi 配置 CRUD。
- 生成器两张残留表当前均为 0 行；用户后续明确豁免本次目标开发库备份，物理删除前仍需重新验证 0 行、对象身份和 SQL preflight。

## 外部权威

- 最新用户决定：执行已确认修复计划，并允许在该计划范围内修复开发数据库。
- 根因与修复不变量：`<Path>{roots.state}/specdev/changes/2026-09-01-admin-runtime-capability-reconciliation/diagnosis.md</Path>`。
- 被修正的历史 change：`<Path>{roots.state}/specdev/changes/2026-08-30-openapi-common-module/spec.md</Path>`、`<Path>{roots.state}/specdev/changes/2026-08-31-optional-nacos-dynamic-config/spec.md</Path>`。
