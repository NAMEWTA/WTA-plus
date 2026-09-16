---
schema_version: 3
plan_contract_version: 1
skill_scan: "已扫描项目 .agents/skills/**/SKILL.md；仅绑定本票命中的入口，执行前按入口继续展开适用 references"
skill_bindings: [{"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "ef0588daecab40e12e49e99d29a12bf107e887ab432e10a790bbc893fc42b0cf", "phase": "plan", "operation": "read-scope-contract-and-bind-acceptance", "inputs": ["当前审查候选、Ticket路径和上游ADR"], "outputs": ["实现前边界、验证与失败停止条件"], "required": true, "on_failure": "block-ticket"}, {"id": "namewta-fullstack-development", "path": "<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>", "sha256": "dd3e42720caa894bcb31ba1503b8211f669b3bbc2bb9ea3e40590a3a9edea395", "phase": "plan", "operation": "read-scope-contract-and-bind-acceptance", "inputs": ["当前审查候选、Ticket路径和上游ADR"], "outputs": ["实现前边界、验证与失败停止条件"], "required": true, "on_failure": "block-ticket"}]
resource_claims: ["finding:D-02", "contract:AC-008"]
artifact: ticket
change: 2026-09-14-wta-plus-comprehensive-review
id: T-08
title: 补齐SSO独立Origin发布合同
status: draft
planning_depth: deep
planning_depth_reason: "本票涉及 D-02；需先完成证据/高影响取舍，不能直接实现。"
ready: false
risk: high
blocked_by: ["T-07", "T-09", "T-10"]
contract_ids: [AC-008]
owner: user-review
expected_changes: ["<Path>release-artifacts/docker/docker-compose-frontend.yml</Path>", "<Path>release-artifacts/docker/frontend/nginx/</Path>", "<Path>release-artifacts/.env.example</Path>", "<Path>release-artifacts/scripts/release-manage.sh</Path>", "<Path>release-artifacts/tests/</Path>", "<Path>release-artifacts/skills/wta-namewta-nginx-config/</Path>", "<Path>release-artifacts/README.md</Path>", "<Path>frontend/apps/sso-web/</Path>", "<Path>frontend/e2e/</Path>", "<Path>frontend/playwright.sso.config.ts</Path>", "<Path>scripts/sso-hard-e2e.sh</Path>"]
writable_paths: ["<Path>release-artifacts/docker/docker-compose-frontend.yml</Path>", "<Path>release-artifacts/docker/frontend/nginx/</Path>", "<Path>release-artifacts/.env.example</Path>", "<Path>release-artifacts/scripts/release-manage.sh</Path>", "<Path>release-artifacts/tests/</Path>", "<Path>release-artifacts/skills/wta-namewta-nginx-config/</Path>", "<Path>release-artifacts/README.md</Path>", "<Path>frontend/apps/sso-web/</Path>", "<Path>frontend/e2e/</Path>", "<Path>frontend/playwright.sso.config.ts</Path>", "<Path>scripts/sso-hard-e2e.sh</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/</Path>", "<Path>{roots.state}/specdev/adr/</Path>"]
shared_paths: ["<Path>release-artifacts/scripts/release-manage.sh</Path>", "<Path>release-artifacts/tests/</Path>", "<Path>release-artifacts/README.md</Path>", "<Path>frontend/apps/sso-web/</Path>", "<Path>frontend/e2e/</Path>"]
shared_path_owners: ["<Path>release-artifacts/scripts/release-manage.sh</Path> => Lead", "<Path>release-artifacts/tests/</Path> => Lead", "<Path>release-artifacts/README.md</Path> => Lead", "<Path>frontend/apps/sso-web/</Path> => Lead", "<Path>frontend/e2e/</Path> => Lead"]
---

# T-08：补齐SSO独立Origin发布合同

> 本 Ticket 是 <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/</Path> 内的待审实施计划。它不授权修改产品代码；只有用户明确接受该 Ticket 后，Lead 才能按 project Skill 建立实施 Evidence。

## 1. 战略与来源

- **Finding：** D-02。详细源码证据见专项审查报告。
- **目标：** 删除真实结构复杂性并恢复可验证的行为/安全/交付合同；不以移动代码、放宽门禁、删除测试或无限兼容层制造绿色。
- **可观察产出：** 每个shipped App均有且仅有完整配套，缺项在promotion前失败

## 2. 决策状态

- **当前状态：** draft，ready=false；高影响决策：推荐兑现ADR-0073/0074独立SSO Origin；生产域名与端口仍需用户填定。
- **已锁定：** 本轮只写 change；实现、提交、发布、远程写入和永久知识修改均未授权。
- **未决：** 用户是否接受本票、外部合同/数据迁移/运行环境参数及是否进入下一 Work。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 将三App的package、prefix、origin、port、API路由、callback、template、shipped状态写入显式发布清单；移除目录扫描隐式决定发布面的行为；未登记App不得悄悄进入产物；补SSO Compose服务、Nginx模板、/sso反代、health/env与端口台账 | 现有模块边界、权限、日志/错误合同中未被本票改变者 | 其他 finding 的实现、无证据的全仓重写、提交/部署、历史数据清理 |

## 4. 行为与实现契约

- **入口或 seam：** <Path>release-artifacts/docker/</Path>；由该 module 的公开 interface 接受输入。
- **输入/输出：** 输入、错误和状态必须保持对应现有合同；任何外部行为变化先更新 Spec/ADR并阻塞本票。
- **不变量：** 失败关闭、权限/Client上下文、资源所有权、事务/幂等和可观测性按适用工程规则成立。
- **错误：** SSO三端跳转、刷新、过期、跨OriginCookie可运行

## 5. 执行路线

1. 将三App的package、prefix、origin、port、API路由、callback、template、shipped状态写入显式发布清单；复用T-09构建矩阵和T-10 release manifest，独立Origin必须改变scheme/host/port之一，不能以同Origin路径冒充ADR-0073。
2. 移除目录扫描隐式决定发布面的行为；未登记App不得悄悄进入产物。
3. 补SSO Compose服务、Nginx模板、/sso反代、health/env与端口台账；<Path>frontend/apps/sso-web/src/ssoApi.ts</Path> 使用VITE_SSO_API和/sso/*，通用VITE_APP_BASE_API不会自动配置SSO API。
4. 验证静态部署子路径、HTTPS Cookie和三App独立存储/会话恢复。
5. 在本地隔离环境完成模板/Compose和浏览器验收，真实DNS/TLS/部署留到具体产物批准。

## 6. 路径访问与所有权

- **可写候选：** frontmatter的writable_paths为精确实施边界；当前仍只写change。T-29同时受cleanup-plan逐文件分类约束，KEEP文件不能因出现在写集便直接删除。
- **共享路径：** frontmatter列明的交集由Lead唯一整合。T-01 → T-09 → T-10 → T-08按依赖串行，T-29收敛文档；发现额外交集时先更新Map，不能各自覆盖同一脚本。
- **不动：** 供应商/历史archive、永久 namespace、用户未提交改动和生成物，除非本票修订后有明确 owner。

## 7. 验证矩阵

| 行为/风险 | 接缝与方法 | 预期 | Evidence |
|---|---|---|---|
| AC 场景 1 | <Path>release-artifacts/docker/</Path> 定向测试/静态或隔离运行 | 每个shipped App均有且仅有完整配套，缺项在promotion前失败 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-08.md</Path> |
| AC 场景 2 | <Path>release-artifacts/docker/</Path> 定向测试/静态或隔离运行 | SSO三端跳转、刷新、过期、跨OriginCookie可运行 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-08.md</Path> |
| AC 场景 3 | <Path>release-artifacts/docker/</Path> 定向测试/静态或隔离运行 | 已发布current在配置失败时不改变 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-08.md</Path> |
| AC 场景 4 | <Path>release-artifacts/docker/</Path> 定向测试/静态或隔离运行 | 发布清单与Compose、Nginx、文档一致 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-08.md</Path> |

- **Workspace checks（当前 not-run）：** cwd `<Path>.</Path>`：`bash release-artifacts/scripts/verify-release.sh`；cwd `<Path>.</Path>`：`docker compose --env-file release-artifacts/.env.example -f release-artifacts/docker/docker-compose-frontend.yml config --quiet`；cwd `<Path>frontend</Path>`：`pnpm exec playwright test --config playwright.sso.config.ts`。该配置无webServer，需隔离后端18080、Admin4174、Home4175、SSO4176、MySQL/Redis；默认pnpm test:e2e忽略SSO spec。nginx -t须针对候选容器；当前未运行实现验证或外部服务。
- **E2E disposition：** required：跨模块/浏览器/外部服务行为必须由 Lead 在 current workspace 或 parent-candidate 验收。

## 8. 迁移、发布与恢复

- **迁移/兼容：** 推荐兑现ADR-0073/0074独立SSO Origin；生产域名与端口仍需用户填定。
- **回滚：** 先保留基线 SHA、产物/数据库备份及可验证恢复点；任何不可逆删除、DDL、凭据轮换或发布需独立批准。
- **停止条件：** 证据缺失、路径越界、父分支漂移、权限/安全合同不明、测试未运行或出现未批准偏差时停票。

## 9. 验收标准

- [ ] `AC-008`：每个shipped App均有且仅有完整配套，缺项在promotion前失败。
- [ ] `AC-008`：SSO三端跳转、刷新、过期、跨OriginCookie可运行。
- [ ] `AC-008`：已发布current在配置失败时不改变。
- [ ] `AC-008`：发布清单与Compose、Nginx、文档一致。
- [ ] Ticket、Map、Spec、ADR 与 Evidence 状态一致；本票保持 draft/ready=false 直到用户接受并完成计划质量复核。
- [ ] 未通过删除测试、关闭类型/安全规则、吞错误、扩大权限或伪造运行证据获得通过。

## 10. Skill 调用计划

- 计划绑定：以frontmatter的skill_bindings为完整集合，进入实现前重新读取真实Skill入口；T-01的全栈Skill用于模块模式validator，T-09用于App与构建合同。新路径或Skill先由Lead同步Map。
- 失败动作：必需入口、验证工具或安全/数据证据不可用则 block-ticket；不得以其他工具静默替换。

## 11. 交付与恢复

- **依赖：** T-07, T-09, T-10；完成顺序由 Tickets Map 控制。
- **检查点：** 记录实现前 base SHA、工作树、命令/退出码、偏差、最终 result SHA 及未运行项。
- **完成出口：** 只在用户批准、所有适用 AC/E2E/Evidence 通过、父分支包含结果后进入 done；当前审查阶段不进入 done。

## 12. 验证与范围校正

正确SSO浏览器命令为cwd <Path>frontend</Path>：`pnpm exec playwright test --config playwright.sso.config.ts`。默认配置忽略SSO测试，不能使用默认pnpm test:e2e冒充。现有SSO config无webServer；Lead先提供隔离服务与真实origin/callback矩阵。生产域名与TLS未确定则保持not-ready。SSO独立Origin不是同站点一个新prefix。只修改.env.example占位；真实.env/证书/DNS不在写集。

本票仍为draft / ready=false；实施验证not-run。恢复时按Map → 适用Project Skill → Ticket → 源码证据读取。
