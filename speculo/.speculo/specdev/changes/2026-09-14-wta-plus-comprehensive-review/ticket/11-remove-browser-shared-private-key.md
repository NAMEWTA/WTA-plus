---
schema_version: 3
plan_contract_version: 1
skill_scan: "已扫描项目 .agents/skills/**/SKILL.md；仅绑定本票命中的入口，执行前按入口继续展开适用 references"
skill_bindings: [{"id":"engineering-standards","path":"<Path>.agents/skills/engineering-standards/SKILL.md</Path>","sha256":"ef0588daecab40e12e49e99d29a12bf107e887ab432e10a790bbc893fc42b0cf","phase":"plan","operation":"read-scope-contract-and-bind-acceptance","inputs":["当前审查候选、Ticket路径和上游ADR"],"outputs":["实现前边界、验证与失败停止条件"],"required":true,"on_failure":"block-ticket"},{"id":"namewta-fullstack-development","path":"<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>","sha256":"dd3e42720caa894bcb31ba1503b8211f669b3bbc2bb9ea3e40590a3a9edea395","phase":"plan","operation":"read-scope-contract-and-bind-acceptance","inputs":["当前审查候选、Ticket路径和上游ADR"],"outputs":["实现前边界、验证与失败停止条件"],"required":true,"on_failure":"block-ticket"},{"id":"wta-common-modules-guide","path":"<Path>.agents/skills/wta-common-modules-guide/SKILL.md</Path>","sha256":"e6cc92b64069528df5c16ebc3c16b6623561539c5f19db5df07147649270d8bd","phase":"plan","operation":"read-scope-contract-and-bind-acceptance","inputs":["当前审查候选、Ticket路径和上游ADR"],"outputs":["实现前边界、验证与失败停止条件"],"required":true,"on_failure":"block-ticket"}]
resource_claims: ["finding:F-01", "contract:AC-011"]
artifact: ticket
change: 2026-09-14-wta-plus-comprehensive-review
id: T-11
title: 删除浏览器共享私钥与ECB传输包装
status: draft
planning_depth: deep
planning_depth_reason: "本票涉及 F-01；需先完成证据/高影响取舍，不能直接实现。"
ready: false
risk: high
blocked_by: []
contract_ids: [AC-011]
owner: user-review
expected_changes: ["<Path>frontend/apps/admin-web/src/application/http.ts</Path>", "<Path>frontend/apps/home-web/src/application/http.ts</Path>", "<Path>frontend/apps/admin-web/src/types/env.d.ts</Path>", "<Path>frontend/packages/adapters/crypto-browser/</Path>", "<Path>frontend/packages/adapters/axios-browser/</Path>", "<Path>frontend/packages/platform/http/</Path>", "<Path>backend/wta-common/wta-common-encrypt/</Path>", "<Path>backend/wta-admin/src/main/java/org/namewta/web/controller/AuthController.java</Path>", "<Path>frontend/apps/admin-web/src/application/services.ts</Path>", "<Path>frontend/apps/home-web/src/application/services.ts</Path>", "<Path>frontend/packages/platform/contracts/src/index.ts</Path>", "<Path>frontend/packages/platform/contracts/src/index.test.ts</Path>", "<Path>frontend/apps/admin-web/package.json</Path>", "<Path>frontend/apps/home-web/package.json</Path>", "<Path>frontend/package.json</Path>", "<Path>frontend/pnpm-lock.yaml</Path>", "<Path>backend/wta-admin/src/main/resources/application.yml</Path>"]
writable_paths: ["<Path>frontend/apps/admin-web/src/application/http.ts</Path>", "<Path>frontend/apps/home-web/src/application/http.ts</Path>", "<Path>frontend/apps/admin-web/src/types/env.d.ts</Path>", "<Path>frontend/packages/adapters/crypto-browser/</Path>", "<Path>frontend/packages/adapters/axios-browser/</Path>", "<Path>frontend/packages/platform/http/</Path>", "<Path>backend/wta-common/wta-common-encrypt/</Path>", "<Path>backend/wta-admin/src/main/java/org/namewta/web/controller/AuthController.java</Path>", "<Path>frontend/apps/admin-web/src/application/services.ts</Path>", "<Path>frontend/apps/home-web/src/application/services.ts</Path>", "<Path>frontend/packages/platform/contracts/src/index.ts</Path>", "<Path>frontend/packages/platform/contracts/src/index.test.ts</Path>", "<Path>frontend/apps/admin-web/package.json</Path>", "<Path>frontend/apps/home-web/package.json</Path>", "<Path>frontend/package.json</Path>", "<Path>frontend/pnpm-lock.yaml</Path>", "<Path>backend/wta-admin/src/main/resources/application.yml</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/</Path>", "<Path>{roots.state}/specdev/adr/</Path>"]
shared_paths: []
shared_path_owners: []
---

# T-11：删除浏览器共享私钥与ECB传输包装

> 本 Ticket 是 <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/</Path> 内的待审实施计划。它不授权修改产品代码；只有用户明确接受该 Ticket 后，Lead 才能按 project Skill 建立实施 Evidence。

## 1. 战略与来源

- **Finding：** F-01。详细源码证据见专项审查报告。
- **目标：** 删除真实结构复杂性并恢复可验证的行为/安全/交付合同；不以移动代码、放宽门禁、删除测试或无限兼容层制造绿色。
- **可观察产出：** 生产bundle不再携带该共享响应私钥或ECB路径

## 2. 决策状态

- **当前状态：** draft，ready=false；高影响决策：ADR-CR-004必须先裁决TLS-only或另有明确AEAD需求；HttpOnly Cookie改造不在本票默认范围。
- **已锁定：** 本轮只写 change；实现、提交、发布、远程写入和永久知识修改均未授权。
- **未决：** 用户是否接受本票、外部合同/数据迁移/运行环境参数及是否进入下一 Work。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 盘点哪些登录/业务请求实际开启VITE_APP_ENCRYPT、服务端@ApiEncrypt及外部消费者；推荐TLS作为浏览器传输合同，删除共享响应私钥、ECB包装及对应两端env；不替换机器OpenAPI HMAC；若确有TLS之外威胁模型，另做版本化AEAD协议并说明密钥管理，不把浏览器静态privateKey当保密材料 | 现有模块边界、权限、日志/错误合同中未被本票改变者 | 其他 finding 的实现、无证据的全仓重写、提交/部署、历史数据清理 |

## 4. 行为与实现契约

- **入口或 seam：** <Path>frontend/packages/adapters/crypto-browser/</Path>；由该 module 的公开 interface 接受输入。
- **输入/输出：** 输入、错误和状态必须保持对应现有合同；任何外部行为变化先更新 Spec/ADR并阻塞本票。
- **不变量：** 失败关闭、权限/Client上下文、资源所有权、事务/幂等和可观测性按适用工程规则成立。
- **错误：** 正常登录、注册、错误响应及下载可运行

## 5. 执行路线

1. 盘点哪些登录/业务请求实际开启VITE_APP_ENCRYPT、服务端@ApiEncrypt及外部消费者。
2. 推荐TLS作为浏览器传输合同，删除共享响应私钥、ECB包装及对应两端env；不替换机器OpenAPI HMAC。
3. 若确有TLS之外威胁模型，另做版本化AEAD协议并说明密钥管理，不把浏览器静态privateKey当保密材料。
4. 跨端同步切换请求/响应解析、错误映射和登录，不留双协议无限兼容。
5. 同步移除无消费者CryptoJS/jsencrypt依赖，锁文件由正常依赖工具生成。
6. 构建扫描不能误删用于测试的公钥/占位说明，针对生产bundle与环境策略验收。浏览器共享响应privateKey暴露不等同于服务器私钥泄露，本票不声称TLS下可任意解密抓包。
7. 按实际@ApiEncrypt消费者裁决移除范围；机器OpenAPI HMAC、OSS签名和HttpOnly Cookie迁移不在默认写集。
8. 当前App根目录未发现.env文件，环境注入由实际构建/部署owner补证并登记真实路径后清理，不保留虚构.env通配写集。package.json与lock仅按无消费者依赖清退修改，使用正常依赖工具生成lock；release-artifacts为构建验收产物，不作为源码写目录。

## 6. 路径访问与所有权

- **可写候选：** <Path>frontend/apps/admin-web/src/application/http.ts</Path>, <Path>frontend/apps/home-web/src/application/http.ts</Path>, <Path>frontend/apps/admin-web/src/types/env.d.ts</Path>, <Path>frontend/packages/adapters/crypto-browser/</Path>, <Path>frontend/packages/adapters/axios-browser/</Path>, <Path>frontend/packages/platform/http/</Path>, <Path>backend/wta-common/wta-common-encrypt/</Path>, <Path>backend/wta-admin/src/main/java/org/namewta/web/controller/AuthController.java</Path>, <Path>frontend/apps/admin-web/src/application/services.ts</Path>, <Path>frontend/apps/home-web/src/application/services.ts</Path>, <Path>frontend/packages/platform/contracts/src/index.ts</Path>, <Path>frontend/packages/platform/contracts/src/index.test.ts</Path>, <Path>frontend/apps/admin-web/package.json</Path>, <Path>frontend/apps/home-web/package.json</Path>, <Path>frontend/package.json</Path>, <Path>frontend/pnpm-lock.yaml</Path>, <Path>backend/wta-admin/src/main/resources/application.yml</Path>（仅用户批准后；越界必须停止）。
- **共享路径：** 无；若实现时发现与其他票交集，先由 Lead 串行化并修订 Map。
- **不动：** 供应商/历史archive、永久 namespace、用户未提交改动和生成物，除非本票修订后有明确 owner。

## 7. 验证矩阵

| 行为/风险 | 接缝与方法 | 预期 | Evidence |
|---|---|---|---|
| AC 场景 1 | <Path>frontend/packages/adapters/crypto-browser/</Path> 定向测试/静态或隔离运行 | 生产bundle不再携带该共享响应私钥或ECB路径 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-11.md</Path> |
| AC 场景 2 | <Path>frontend/packages/adapters/crypto-browser/</Path> 定向测试/静态或隔离运行 | 正常登录、注册、错误响应及下载可运行 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-11.md</Path> |
| AC 场景 3 | <Path>frontend/packages/adapters/crypto-browser/</Path> 定向测试/静态或隔离运行 | 机器调用HMAC与OSS签名不被删除 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-11.md</Path> |
| AC 场景 4 | <Path>frontend/packages/adapters/crypto-browser/</Path> 定向测试/静态或隔离运行 | 传输切换有前后端原子发布与恢复方案 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-11.md</Path> |

- **Workspace checks：** frontend: pnpm lint; pnpm typecheck; pnpm test; pnpm build:prod；backend: ./mvnw -pl wta-common/wta-common-encrypt,wta-admin -am test。本轮未运行实现验证。
- **E2E disposition：** required：跨模块/浏览器/外部服务行为必须由 Lead 在 current workspace 或 parent-candidate 验收。

## 8. 迁移、发布与恢复

- **迁移/兼容：** ADR-CR-004必须先裁决TLS-only或另有明确AEAD需求；HttpOnly Cookie改造不在本票默认范围。
- **回滚：** 先保留基线 SHA、产物/数据库备份及可验证恢复点；任何不可逆删除、DDL、凭据轮换或发布需独立批准。
- **停止条件：** 证据缺失、路径越界、父分支漂移、权限/安全合同不明、测试未运行或出现未批准偏差时停票。

## 9. 验收标准

- [ ] `AC-011`：生产bundle不再携带该共享响应私钥或ECB路径。
- [ ] `AC-011`：正常登录、注册、错误响应及下载可运行。
- [ ] `AC-011`：机器调用HMAC与OSS签名不被删除。
- [ ] `AC-011`：传输切换有前后端原子发布与恢复方案。
- [ ] Ticket、Map、Spec、ADR 与 Evidence 状态一致；本票保持 draft/ready=false 直到用户接受并完成计划质量复核。
- [ ] 未通过删除测试、关闭类型/安全规则、吞错误、扩大权限或伪造运行证据获得通过。

## 10. Skill 调用计划

- 计划绑定：namewta-fullstack-development, wta-common-modules-guide。每项入口摘要、references、phase和失败动作以 frontmatter 为最低合同；执行前必须读取当前 Ticket、Map 与实际命中 references。
- 失败动作：必需入口、验证工具或安全/数据证据不可用则 block-ticket；不得以其他工具静默替换。

## 11. 交付与恢复

- **依赖：** 无；完成顺序由 Tickets Map 控制。
- **检查点：** 记录实现前 base SHA、工作树、命令/退出码、偏差、最终 result SHA 及未运行项。
- **完成出口：** 只在用户批准、所有适用 AC/E2E/Evidence 通过、父分支包含结果后进入 done；当前审查阶段不进入 done。

## 12. 最新证据校正与具体检查

ADR-CR-004必须先裁决TLS-only或另有明确AEAD需求；HttpOnly Cookie改造不在本票默认范围。

- 生产bundle不再携带该共享响应私钥或ECB路径。
- 正常登录、注册、错误响应及下载可运行。
- 机器调用HMAC与OSS签名不被删除。
- 传输切换有前后端原子发布与恢复方案。

实际命令候选（实施前对照script重新解析，当前not-run）：

- `frontend: pnpm lint; pnpm typecheck; pnpm test; pnpm build:prod`
- `backend: ./mvnw -pl wta-common/wta-common-encrypt,wta-admin -am test`
