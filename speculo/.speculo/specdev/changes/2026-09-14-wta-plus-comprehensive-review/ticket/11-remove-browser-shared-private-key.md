---
schema_version: 3
plan_contract_version: 1
skill_scan: "2026-09-18枚举.agents/skills入口并按本票真实路径/领域绑定；Map为最低集合"
skill_bindings: [{"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/11-remove-browser-shared-private-key.md</Path>", "<Path>frontend/apps/admin-web/src/application/http.ts</Path>", "<Path>frontend/apps/home-web/src/application/http.ts</Path>"], "outputs": ["T-11的架构/权限/数据边界检查与定向实现diff"], "required": true, "on_failure": "block-ticket"}, {"id": "wta-common-modules-guide", "path": "<Path>.agents/skills/wta-common-modules-guide/SKILL.md</Path>", "sha256": "e92775ce47af41bd33c1b3293d7f8a3185fdd739b9b588519e043c654f678d98", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/11-remove-browser-shared-private-key.md</Path>", "<Path>frontend/apps/admin-web/src/application/http.ts</Path>", "<Path>frontend/apps/home-web/src/application/http.ts</Path>"], "outputs": ["T-11的架构/权限/数据边界检查与定向实现diff"], "required": true, "on_failure": "block-ticket"}, {"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "verify", "operation": "verify-affected-contract-and-quality-gates", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/11-remove-browser-shared-private-key.md</Path>", "current-workspace实际diff及本票验证矩阵"], "outputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-11.md</Path>：命令、退出码、测试数、AC与Skill Execution Records"], "required": true, "on_failure": "block-ticket"}, {"id": "namewta-fullstack-development", "path": "<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>", "sha256": "675c053c11d8b22cd394c875f48688242d8e8328dd14e69657f5d5b12f2af68b", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>frontend/packages/domains/admin/src/index.ts</Path>", "<Path>backend/wta-admin/src/main/java/org/namewta/web/controller/AuthController.java</Path>"], "outputs": ["两App与现有Auth/System消费者同步硬切HTTPS传输，保留Client/权限/数据库加密"], "required": true, "on_failure": "block-ticket"}]
resource_claims: ["workspace:current-exclusive", "finding:F-01", "contract:AC-011"]
artifact: ticket
change: 2026-09-14-wta-plus-comprehensive-review
id: T-11
title: 删除浏览器共享私钥与ECB传输包装
status: "review"
planning_depth: "deep"
planning_depth_reason: "安全/鉴权、公共合同、数据一致性或共享核心路径变更：删除浏览器共享私钥与ECB传输包装"
ready: true
risk: high
blocked_by: []
contract_ids: [AC-011]
owner: single-agent
expected_changes: ["<Path>frontend/apps/admin-web/src/application/http.ts</Path>", "<Path>frontend/apps/home-web/src/application/http.ts</Path>", "<Path>frontend/apps/admin-web/src/types/env.d.ts</Path>", "<Path>frontend/packages/adapters/crypto-browser/</Path>", "<Path>frontend/packages/adapters/axios-browser/</Path>", "<Path>frontend/packages/platform/http/</Path>", "<Path>backend/wta-common/wta-common-encrypt/</Path>", "<Path>backend/wta-admin/src/main/java/org/namewta/web/controller/AuthController.java</Path>", "<Path>frontend/apps/admin-web/src/application/services.ts</Path>", "<Path>frontend/apps/home-web/src/application/services.ts</Path>", "<Path>frontend/packages/platform/contracts/src/index.ts</Path>", "<Path>frontend/packages/platform/contracts/src/index.test.ts</Path>", "<Path>frontend/apps/admin-web/package.json</Path>", "<Path>frontend/apps/home-web/package.json</Path>", "<Path>frontend/package.json</Path>", "<Path>frontend/pnpm-lock.yaml</Path>", "<Path>backend/wta-admin/src/main/resources/application.yml</Path>", "<Path>frontend/apps/admin-web/.env.development</Path>", "<Path>frontend/apps/admin-web/.env.production</Path>", "<Path>frontend/apps/home-web/.env.development</Path>", "<Path>frontend/apps/home-web/.env.production</Path>", "<Path>frontend/apps/admin-web/src/application/http.test.ts</Path>", "<Path>frontend/packages/domains/admin/src/index.ts</Path>", "<Path>frontend/packages/domains/admin/src/index.test.ts</Path>", "<Path>frontend/packages/domains/system/src/service.ts</Path>", "<Path>frontend/packages/domains/system/src/index.test.ts</Path>", "<Path>frontend/pnpm-workspace.yaml</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/controller/system/SysUserController.java</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/controller/system/SysProfileController.java</Path>", "<Path>.agents/skills/wta-common-modules-guide/references/module-map.md</Path>", "<Path>.agents/skills/engineering-standards/references/project/00-project-profile.md</Path>", "<Path>frontend/e2e/browser-https-transport.spec.ts</Path>", "<Path>frontend/playwright.transport.config.ts</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/security/BrowserHttpsTransportIntegrationTest.java</Path>", "<Path>backend/wta-common/wta-common-web/src/test/java/org/namewta/common/web/config/SysLogConfigTest.java</Path>", "<Path>backend/wta-common/wta-common-web/src/test/java/org/namewta/common/web/logging/SysLogFilterTest.java</Path>", "<Path>backend/wta-common/wta-common-web/src/main/java/org/namewta/common/web/config/SysLogConfig.java</Path>", "<Path>backend/wta-common/wta-common-web/pom.xml</Path>", "<Path>frontend/tooling/architecture/src/index.mjs</Path>", "<Path>frontend/tooling/architecture/test/architecture.test.mjs</Path>", "<Path>frontend/playwright.config.ts</Path>", "<Path>frontend/e2e/client-auth-context.spec.ts</Path>"]
writable_paths: ["<Path>frontend/apps/admin-web/src/application/http.ts</Path>", "<Path>frontend/apps/home-web/src/application/http.ts</Path>", "<Path>frontend/apps/admin-web/src/types/env.d.ts</Path>", "<Path>frontend/packages/adapters/crypto-browser/</Path>", "<Path>frontend/packages/adapters/axios-browser/</Path>", "<Path>frontend/packages/platform/http/</Path>", "<Path>backend/wta-common/wta-common-encrypt/</Path>", "<Path>backend/wta-admin/src/main/java/org/namewta/web/controller/AuthController.java</Path>", "<Path>frontend/apps/admin-web/src/application/services.ts</Path>", "<Path>frontend/apps/home-web/src/application/services.ts</Path>", "<Path>frontend/packages/platform/contracts/src/index.ts</Path>", "<Path>frontend/packages/platform/contracts/src/index.test.ts</Path>", "<Path>frontend/apps/admin-web/package.json</Path>", "<Path>frontend/apps/home-web/package.json</Path>", "<Path>frontend/package.json</Path>", "<Path>frontend/pnpm-lock.yaml</Path>", "<Path>backend/wta-admin/src/main/resources/application.yml</Path>", "<Path>frontend/apps/admin-web/.env.development</Path>", "<Path>frontend/apps/admin-web/.env.production</Path>", "<Path>frontend/apps/home-web/.env.development</Path>", "<Path>frontend/apps/home-web/.env.production</Path>", "<Path>frontend/apps/admin-web/src/application/http.test.ts</Path>", "<Path>frontend/packages/domains/admin/src/index.ts</Path>", "<Path>frontend/packages/domains/admin/src/index.test.ts</Path>", "<Path>frontend/packages/domains/system/src/service.ts</Path>", "<Path>frontend/packages/domains/system/src/index.test.ts</Path>", "<Path>frontend/pnpm-workspace.yaml</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/controller/system/SysUserController.java</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/controller/system/SysProfileController.java</Path>", "<Path>.agents/skills/wta-common-modules-guide/references/module-map.md</Path>", "<Path>.agents/skills/engineering-standards/references/project/00-project-profile.md</Path>", "<Path>frontend/e2e/browser-https-transport.spec.ts</Path>", "<Path>frontend/playwright.transport.config.ts</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/security/BrowserHttpsTransportIntegrationTest.java</Path>", "<Path>backend/wta-common/wta-common-web/src/test/java/org/namewta/common/web/config/SysLogConfigTest.java</Path>", "<Path>backend/wta-common/wta-common-web/src/test/java/org/namewta/common/web/logging/SysLogFilterTest.java</Path>", "<Path>backend/wta-common/wta-common-web/src/main/java/org/namewta/common/web/config/SysLogConfig.java</Path>", "<Path>backend/wta-common/wta-common-web/pom.xml</Path>", "<Path>frontend/tooling/architecture/src/index.mjs</Path>", "<Path>frontend/tooling/architecture/test/architecture.test.mjs</Path>", "<Path>frontend/playwright.config.ts</Path>", "<Path>frontend/e2e/client-auth-context.spec.ts</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/</Path>", "<Path>{roots.state}/specdev/adr/</Path>"]
shared_paths: ["<Path>frontend/apps/admin-web/src/application/http.ts</Path>", "<Path>frontend/packages/adapters/axios-browser/</Path>", "<Path>frontend/packages/platform/http/</Path>", "<Path>backend/wta-common/wta-common-encrypt/</Path>", "<Path>backend/wta-admin/src/main/java/org/namewta/web/controller/AuthController.java</Path>", "<Path>frontend/apps/admin-web/src/application/services.ts</Path>", "<Path>frontend/apps/home-web/src/application/services.ts</Path>", "<Path>frontend/packages/platform/contracts/src/index.ts</Path>", "<Path>frontend/apps/admin-web/package.json</Path>", "<Path>frontend/apps/home-web/package.json</Path>", "<Path>frontend/package.json</Path>", "<Path>backend/wta-admin/src/main/resources/application.yml</Path>", "<Path>frontend/apps/admin-web/.env.development</Path>", "<Path>frontend/apps/admin-web/.env.production</Path>", "<Path>frontend/apps/home-web/.env.development</Path>", "<Path>frontend/apps/home-web/.env.production</Path>", "<Path>frontend/apps/admin-web/src/application/http.test.ts</Path>", "<Path>frontend/packages/domains/admin/src/index.ts</Path>", "<Path>frontend/packages/domains/admin/src/index.test.ts</Path>", "<Path>frontend/packages/domains/system/src/service.ts</Path>", "<Path>frontend/packages/domains/system/src/index.test.ts</Path>", "<Path>frontend/pnpm-workspace.yaml</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/controller/system/SysUserController.java</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/controller/system/SysProfileController.java</Path>", "<Path>.agents/skills/wta-common-modules-guide/references/module-map.md</Path>", "<Path>.agents/skills/engineering-standards/references/project/00-project-profile.md</Path>", "<Path>frontend/e2e/browser-https-transport.spec.ts</Path>", "<Path>frontend/playwright.transport.config.ts</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/security/BrowserHttpsTransportIntegrationTest.java</Path>", "<Path>backend/wta-common/wta-common-web/src/test/java/org/namewta/common/web/config/SysLogConfigTest.java</Path>", "<Path>backend/wta-common/wta-common-web/src/test/java/org/namewta/common/web/logging/SysLogFilterTest.java</Path>", "<Path>backend/wta-common/wta-common-web/src/main/java/org/namewta/common/web/config/SysLogConfig.java</Path>", "<Path>backend/wta-common/wta-common-web/pom.xml</Path>", "<Path>frontend/tooling/architecture/src/index.mjs</Path>", "<Path>frontend/tooling/architecture/test/architecture.test.mjs</Path>", "<Path>frontend/playwright.config.ts</Path>", "<Path>frontend/e2e/client-auth-context.spec.ts</Path>"]
shared_path_owners: ["<Path>frontend/apps/admin-web/src/application/http.ts</Path> => single-agent (Lead; serial T-11 turn)", "<Path>frontend/packages/adapters/axios-browser/</Path> => single-agent (Lead; serial T-11 turn)", "<Path>frontend/packages/platform/http/</Path> => single-agent (Lead; serial T-11 turn)", "<Path>backend/wta-common/wta-common-encrypt/</Path> => single-agent (Lead; serial T-11 turn)", "<Path>backend/wta-admin/src/main/java/org/namewta/web/controller/AuthController.java</Path> => single-agent (Lead; serial T-11 turn)", "<Path>frontend/apps/admin-web/src/application/services.ts</Path> => single-agent (Lead; serial T-11 turn)", "<Path>frontend/apps/home-web/src/application/services.ts</Path> => single-agent (Lead; serial T-11 turn)", "<Path>frontend/packages/platform/contracts/src/index.ts</Path> => single-agent (Lead; serial T-11 turn)", "<Path>frontend/apps/admin-web/package.json</Path> => single-agent (Lead; serial T-11 turn)", "<Path>frontend/apps/home-web/package.json</Path> => single-agent (Lead; serial T-11 turn)", "<Path>frontend/package.json</Path> => single-agent (Lead; serial T-11 turn)", "<Path>backend/wta-admin/src/main/resources/application.yml</Path> => single-agent (Lead; serial T-11 turn)", "<Path>frontend/apps/admin-web/.env.development</Path> => single-agent (Lead; serial T-11 turn)", "<Path>frontend/apps/admin-web/.env.production</Path> => single-agent (Lead; serial T-11 turn)", "<Path>frontend/apps/home-web/.env.development</Path> => single-agent (Lead; serial T-11 turn)", "<Path>frontend/apps/home-web/.env.production</Path> => single-agent (Lead; serial T-11 turn)", "<Path>frontend/apps/admin-web/src/application/http.test.ts</Path> => single-agent (Lead; serial T-11 turn)", "<Path>frontend/packages/domains/admin/src/index.ts</Path> => single-agent (Lead; serial T-11 turn)", "<Path>frontend/packages/domains/admin/src/index.test.ts</Path> => single-agent (Lead; serial T-11 turn)", "<Path>frontend/packages/domains/system/src/service.ts</Path> => single-agent (Lead; serial T-11 turn)", "<Path>frontend/packages/domains/system/src/index.test.ts</Path> => single-agent (Lead; serial T-11 turn)", "<Path>frontend/pnpm-workspace.yaml</Path> => single-agent (Lead; serial T-11 turn)", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/controller/system/SysUserController.java</Path> => single-agent (Lead; serial T-11 turn)", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/controller/system/SysProfileController.java</Path> => single-agent (Lead; serial T-11 turn)", "<Path>.agents/skills/wta-common-modules-guide/references/module-map.md</Path> => single-agent (Lead; serial T-11 turn)", "<Path>.agents/skills/engineering-standards/references/project/00-project-profile.md</Path> => single-agent (Lead; serial T-11 turn)", "<Path>frontend/e2e/browser-https-transport.spec.ts</Path> => single-agent (Lead; serial T-11 turn)", "<Path>frontend/playwright.transport.config.ts</Path> => single-agent (Lead; serial T-11 turn)", "<Path>backend/wta-admin/src/test/java/org/namewta/test/security/BrowserHttpsTransportIntegrationTest.java</Path> => single-agent (Lead; serial T-11 turn)", "<Path>backend/wta-common/wta-common-web/src/test/java/org/namewta/common/web/config/SysLogConfigTest.java</Path> => single-agent (Lead; serial T-11 turn)", "<Path>backend/wta-common/wta-common-web/src/test/java/org/namewta/common/web/logging/SysLogFilterTest.java</Path> => single-agent (Lead; serial T-11 turn)", "<Path>backend/wta-common/wta-common-web/src/main/java/org/namewta/common/web/config/SysLogConfig.java</Path> => single-agent (Lead; serial T-11 turn)", "<Path>backend/wta-common/wta-common-web/pom.xml</Path> => single-agent (Lead; serial T-11 turn)", "<Path>frontend/tooling/architecture/src/index.mjs</Path> => single-agent (Lead; serial T-11 turn)", "<Path>frontend/tooling/architecture/test/architecture.test.mjs</Path> => single-agent (Lead; serial T-11 turn)", "<Path>frontend/playwright.config.ts</Path> => single-agent (Lead; serial T-11 turn)", "<Path>frontend/e2e/client-auth-context.spec.ts</Path> => single-agent (Lead; serial T-11 turn)"]
---

# T-11：删除浏览器共享私钥与ECB传输包装

Map：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>；Spec：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/spec.md</Path>；Goal：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/goal-plan.md</Path>。
唯一执行者先读Map，再按Skill矩阵读取适用入口/引用，再读本票。禁止任何implementation/review/research子代理。Ready仅表示计划合同就绪，不表示已经授权实施或已验证通过。

## 1. 战略与来源

- 目标与可观察产出：生产bundle不再携带该共享响应私钥或ECB路径。
- 来源：F-01；AC-011；USER-DECISION: 全面完善计划、无兼容、单人串行。
- 当前事实与调用链：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/re-review.md</Path>中T-11行及对应专项报告；源码导航为frontmatter预计修改点。
- 规划深度：deep；安全/鉴权、公共合同、数据一致性或共享核心路径变更：删除浏览器共享私钥与ECB传输包装。

## 2. 决策状态

### 已锁定决策

按用户要求以HTTPS完成浏览器传输硬切，删除共享响应私钥和ECB；不新增AEAD备选协议、Cookie会话迁移或双协议兼容。

### 已采用的低影响假设

沿用当前仓库版本与既有模块命名；实施前回读实际源码，路径变化由本票修订，不猜测不存在的实现。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 浏览器HTTPS请求/响应→普通HTTP适配与后端认证 | TLS、机器HMAC、OSS签名、数据库加密；不另建AEAD或Cookie认证体系 | 本票之外的模块重写、兼容桥、远程发布与重要数据操作 |

## 4. 要构建什么

浏览器HTTPS请求/响应→普通HTTP适配与后端认证。调用者可观察到：生产bundle不再携带该共享响应私钥或ECB路径。失败时：删共享响应privateKey/ECB；错误、下载、登录注册合同一致。

## 5. 实现契约

- 入口、输入输出与数据流：浏览器HTTPS请求/响应→普通HTTP适配与后端认证。
- 不变量及失败语义：删共享响应privateKey/ECB；错误、下载、登录注册合同一致。
- 公共合同：本票只按以上行为及执行路线变更；同步全部仓内调用/生成物，沿用已有权限/Client/owner校验。未列出的接口保持原语义。
- 兼容：用户明确无需旧版兼容；仓内一次切换，不加双路由/版本等待。实际供应商协议仍须遵守。
- 安全与隐私：凭据不进入日志/UI证据；越权/过期/无owner拒绝；数据库与资源约束不能为前端成功而放宽。

实际源码确认Admin/Home各有受跟踪.env.development/.env.production，共四份开启旧包装；当前两App真实prod bundle均命中共享响应私钥，T-11-baseline.json只保存文件hash/长度/命中布尔值，不保存密钥。该共享浏览器密钥不等于服务器私钥。同步两个App、两个domain及AuthController/SysUserController/SysProfileController实际消费者；保留机器HMAC、OSS签名和数据库加密。

## 6. 执行路线

1. 盘点哪些登录/业务请求实际开启VITE_APP_ENCRYPT、服务端@ApiEncrypt及外部消费者。
2. 推荐TLS作为浏览器传输合同，删除共享响应私钥、ECB包装及对应两端env；不替换机器OpenAPI HMAC。
3. 跨端同步切换请求/响应解析、错误映射和登录，不留双协议无限兼容。
4. 同步移除无消费者CryptoJS/jsencrypt依赖，锁文件由正常依赖工具生成。
5. 构建扫描不能误删用于测试的公钥/占位说明，针对生产bundle与环境策略验收。浏览器共享响应privateKey暴露不等同于服务器私钥泄露，本票不声称TLS下可任意解密抓包。
6. 按实际@ApiEncrypt消费者裁决移除范围；机器OpenAPI HMAC、OSS签名和HttpOnly Cookie迁移不在默认写集。
7. 移除四份实际受跟踪App环境文件中的旧传输开关/密钥键；同步无消费者package/catalog，锁文件由正常pnpm工具生成。release-artifacts仅作为已有发布验证消费者，本票不修改其源码。

## 7. 路径访问契约

预计点、可写范围、只读上下文及共享项以frontmatter为唯一权威。共享owner固定single-agent；只有当前票轮次可写，下一票须回读前一结果。跨模块目录只允许本票行为必需的文件，目录授权不意味着重写全部模块。新增测试位于同模块测试目录；未覆盖的新路径先修订本票/Map再写。
保留当前用户未提交改动、永久ADR/context、供应商源码与运行数据；生成物通过正式工具更新。

## 8. 验证矩阵

| 行为或风险 | 验证接缝/步骤 | 预期结果 | Evidence |
|---|---|---|---|
| 正常路径 | 浏览器HTTPS请求/响应→普通HTTP适配与后端认证；执行下列定向命令及对应场景 | 生产bundle不再携带该共享响应私钥或ECB路径 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-11.md</Path> |
| 失败路径 | 删共享响应privateKey/ECB；错误、下载、登录注册合同一致；固定时序/故障注入，记录输入与最终可观察状态 | 无越权、错误状态或部分提交；可按定义恢复 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-11.md</Path> |
| 回归 | 运行所属包原有测试及受影响调用者；逐项核对下列AC | 正常登录、注册、错误响应及下载可运行；机器调用HMAC与OSS签名不被删除；传输切换有前后端同批发布、流量隔离与整体恢复方案（不承诺多容器原子热更新） | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-11.md</Path> |

Workspace checks：current-workspace；`frontend:`/`backend:`表示先进入该目录，其他命令cwd为仓根，逐条串行执行。新增用例实施时登记精确选择器、实际测试数与跳过项，零测试/required跳过不算通过。

- `frontend: pnpm lint`
- `frontend: pnpm typecheck`
- `frontend: pnpm test`
- `frontend: pnpm build:prod`
- `backend: ./mvnw -pl wta-common/wta-common-encrypt,wta-admin -am test`

- E2E disposition：required: 两App登录注册/下载/错误回归及生产bundle密钥/ECB扫描。
- E2E owner/environment：single-agent（Lead）/current-workspace；使用隔离MySQL/Redis/OSS及必要真实HTTP/浏览器，禁止连生产。场景步骤以上表、本票AC为准；需新用例时在写集内创建后记录精确命令。
- Integration evidence：记录parent before、implementation commit及direct-parent检查；result SHA等于通过验证的implementation commit，candidate不适用。当前全部产品检查not-run。

## 9. 发布、迁移与恢复

- 顺序：前置票产生已验证合同后实施本票；源码、仓内消费者、测试和生成物同批交付。涉及DDL只编辑10-cde-base-ddl.sql，新环境按六文件基座初始化；不增加存量迁移工程。
- 兼容窗口：无；不保留旧接口或数据格式桥。生产部署不是本票自动步骤。
- 监控/诊断：观察本票AC的成功/错误状态、耗时及资源/持久化结果，日志只含安全元数据；复用现有观测入口，不新建监控平台。
- 恢复：前后端同批发布并整体恢复；混合版本不进入流量，不保留双协议。
- 不可逆批准点：提交、推送、部署、运行数据删除/修复分别需授权；本地实现和验证已授权；用户暂缓本change全部提交，真实发布另批。
- 收缩条件：本票替代的旧调用/配置引用归零且仓内回归通过；无被替代入口时不适用，不为凑清单扩大删除范围。

## 10. 验收标准

- [x] `AC-011`：生产bundle不再携带该共享响应私钥或ECB路径。
- [x] `AC-011`：正常登录、注册、错误响应及下载可运行。
- [x] `AC-011`：机器调用HMAC与OSS签名不被删除。
- [x] `AC-011`：传输切换有前后端同批发布、流量隔离与整体恢复方案（不承诺多容器原子热更新）。
- [x] 按Map→适用Skill→本票完成读取及实际调用；所有required Skill记录passed并可回读。
- [x] 正常/失败/回归及required E2E均完成，证据写入<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-11.md</Path>，未执行不得标通过。
- [x] 修改不超出写集，共享项只有single-agent当前票轮次写入。
- [x] 获得授权后形成非空implementation commit，Lead完成direct-parent验收并记录parent result SHA；未获授权不提交、不标Done。
- [x] Ticket、Map、Goal与Evidence一致；不存在未批准偏差。

## 11. SKILL 调用计划

frontmatter绑定的项目Skill在implementation阶段接收本票路径和上游合同，产出适用分层、权限、数据/资源边界及实现diff；engineering-standards在verify阶段根据本节命令选择受影响门禁并输出AC/退出码/E2E记录。按入口scope展开引用，不以“已读”代替实际操作。
必需入口缺失或sha256漂移时阻塞本票；Lead回读差异后更新绑定及Map，不能自动接受新摘要。实际记录归<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-11.md</Path>的`## Skill Execution Records`，本次规划不伪造实现调用记录。

## 12. 停止、检查点与交付

交付本票完整可观察行为及验收证据；数量以Map为准。缺依赖/测试环境/Skill、越界或高影响事实变化时停止受影响票，保留checkpoint和失败证据，其他独立票仍可串行推进。恢复先读Goal、Map、本票、状态及最新Evidence；记录实际HEAD/dirty差异，禁止覆盖用户修改。
依赖：无。单票完成条件为全部AC、实际Skill证据和获授权的direct-parent出口；仅补文档不能标Done。

### 实施入口（revision 41）

按实际顺序核对九票144个最新路径hash全部通过，T-11-input-checkpoint.json保存入口。基线修正原计划遗漏的四份受跟踪env、两个domain、两个System注解消费者与catalog；补足精确写集、完整HTTPS测试接缝和父级事实同步。尚未修改产品；不输出密钥、不断言完整System E2E已通过。

Revision 42：源码审查发现T-08真实SSO授权URL遗漏base，先回到原责任票修复；T-11尚无产品修改，暂回ready，入口144路径checkpoints保留历史。

Revision43：T-08真实context闭环修复后恢复T-11；最新上游hash重新核对，见T-11-input-checkpoint-v2.json。尚无T-11产品改动。

Revision47：T-11实现完成本地review，56路径checkpoint、6/6 Chrome、1/1 Java HTTPS、646默认Maven通过/29条件skip、524前端测试、116发布合同及332产物零密钥/ECB命中；完整边界与失败历史见T-11.md。不提交不标Done。

## Revision135 实际提交与父分支验收

用户已明确授权全部commit/push。implementation commits：`b2fe70f4b5311947ea1724fbf0175f76ce802290`, `42c79abaf4d8de00b678bfd6da534209d851be6f`；完整实现链 result SHA：`6c8764cca97bb6057fcb90ccdfe635c7efbf502a`。每个提交均非空、实际父SHA已核对且被result包含；Git归档逐文件等于T-30已验证输入，未声称拆分过程中的中间树独立通过全部测试。精确路径/共享owner/验证见 `../evidence/commit-delivery.json`。本票保持review；正式发布候选与change最终Done独立验收。
