# 对抗性审查与修复（2026-09-26）

## Goal

用户要求审查当前 review change，直接修复实现问题；仅满足完成门后进入归档。保留用户有意设置的 OSS 和 CORS 通配符。历史 Orca 会话停于 T-43 基线测量，当前工作树起点为 `907443f7`，起始 Git clean；历史 transcript 只读。当前 50 票为 30 done / 20 cancelled，但完成状态尚未闭合。

## Scope and ownership

Lead 拥有本记录与状态，产品实现按唯一 writer 串行交接。只读 review 覆盖 Notify、OSS/Web、前端/脚本三个独立范围。此为风险驱动审查，不宣称证明全仓无缺陷。没有提交、推送、部署、归档或永久知识提升。

## Findings and current status

| ID | Severity | Finding | Status |
|---|---|---|---|
| AR-01 | P1 | 两 App SSO callback 离开页面/会话变化后仍接纳迟到 token；Home 换账号仍可复用旧身份菜单 | 已修复；定向与前端完整门禁通过 |
| AR-02 | P1 | Outbox 一次领取 50 条并串行发送，后排尚未发送即租约过期，重领转 UNKNOWN | 已修复；即时领取单条，Worker 继续循环；慢调用回归通过 |
| AR-03 | P1 | 共享附件 COPYING 被另一收件人当永久失败；等待若先占配额还会耗尽限额 | 已修复；COPYING 等待不占配额，60 秒后转 COPY_UNKNOWN；全量单测及 28 项真实服务测试通过 |
| AR-04 | P1 | 普通 OSS 删除已生效但回执/提交失败后，PENDING 仍可恢复/绑定缺失对象 | 已修复；独立事务预约 DELETING，真实 MySQL 提交/并发/故障测试通过 |
| AR-05 | P2 | 单对象公开迁移记录 FAILED 后仍返回成功，页面提示已复制 | 已修复；核对最终迁移结果，后端回归通过 |
| AR-06 | P2 | dev 脚本忽略环境 Vite 端口，显式外部 Spring 配置仍要求仓内私有配置并检查错误端口 | 已修复，定向启动夹具与 guard 通过 |
| AR-07 | P2 | 发布门禁旧菜单测试用非 TTY 管道输入，当前脚本拒绝此输入 | 已修复；显式参数及拒绝无参数均覆盖，完整 release 验证 exit 0 |
| AR-08 | P1 delivery | Done/Ready/Evidence/集成记录/Goal 状态不一致，complete 门失败 | 初始 122 errors / 69 warnings；不能宣称可归档 |

## Verification

- 修改前 `cd backend && ./mvnw test`：exit 0；1193 tests，944 passed，249 环境门控 skipped。skip 不算真实服务通过。日志 `/tmp/wta-review-backend-baseline.log`。
- 修改前 `bash release-artifacts/scripts/verify-release.sh`：exit 1，非 TTY 菜单夹具失败；日志 `/tmp/wta-review-release.log`。
- `node .agents/skills/engineering-standards/scripts/validate-skill-facts.mjs`：exit 0。
- SSO 定向 Vitest：Home 12/12，Admin 8/8，exit 0。
- `node --test --test-name-pattern='developer launcher' release-artifacts/tests/local-config-safety.test.mjs`：exit 0。
- `bash scripts/ci/verify-dev-build-guard.sh`（含环境端口覆盖回归）：exit 0。
- OSS 缺陷红灯：44 tests，2 failures，0 errors，0 skipped，exit 1；日志 `/tmp/oss-review-red.log`。
- complete 校验原始日志 `/tmp/wta-review-complete.log`；后续保留与更新当前结果，不能补造旧的逐票 clean HEAD 验收事实。

- 修改后 `cd backend && ./mvnw -B -ntp test`：exit 0；1210 tests，959 passed，251 环境门控 skipped；日志 `/tmp/wta-review-backend-final.log`。
- `cd frontend && pnpm lint && pnpm typecheck && pnpm test && pnpm build:prod`：exit 0；存在构建体积/静态和动态 import 重叠警告，无错误。
- `bash release-artifacts/scripts/verify-release.sh`：exit 0；日志 `/tmp/wta-review-release-fixed.log`。
- `node .agents/skills/engineering-standards/scripts/validate-skill-facts.mjs` 与 `node .agents/skills/namewta-fullstack-development/scripts/validate-module-mode.mjs backend/wta-modules/wta-notify --mode layered`：exit 0。
- 隔离 MySQL/MinIO：`OssCleanupMySqlIntegrationTest,OssStorageMigrationIntegrationTest,MinioOssClientIntegrationTest` 4 tests / 0 failures / 0 errors / 0 skipped，exit 0；`/tmp/wta-review-services-3db1658e/summary.json`。新增生命周期测试使用真实 Mapper、事务代理及两个 MySQL 连接；远端删除使用确定性故障替身，另外两个 suite 使用真实 MinIO。仅删除本轮创建的容器，清理 exit 0。首轮夹具混用数据库/JVM 时钟造成失败，统一为数据库时间后通过，未放宽断言。
- 当前完成门：`node speculo/workflows/specdev/common/tools/validate-specdev.mjs --stage complete --repo /srv/WTA-plus speculo/.speculo/specdev/changes/2026-09-14-wta-plus-comprehensive-review`：exit 1，104 errors / 0 warnings，日志 `/tmp/wta-review-complete-final.log`。修正了 18 个 cancelled 票的 ready=true；缺历史证据的 Done 记录仍如实标为阻断。

- 通知附件真实 Spring/MySQL/Redis/MinIO：28 tests / 0 failures / 0 errors / 0 skipped，exit 0；`/tmp/wta-review-mail-runs/ef05420a0cfff5bd/result.json`，服务、端口、匿名卷及进程清理全部通过。SMTP 使用测试替身，不发送真实邮件。临时 runner `/tmp/wta-review-mail.py` 复用旧隔离夹具，当前源码由前后文件 SHA-256 保护；这是本轮未提交工作树验证，不冒充历史 clean HEAD 验收。前两次 preflight 因解释器缺少 bcrypt 退出 1 且未创建服务；使用已有 `/usr/bin/python3` 后通过。
- OSS `OssObjectStore` 是 System 模块内部接口；仓内只有 `DefaultOssObjectStore` 一个实现，已同步新有界删除/查询方法，旧方法和旧构造器签名保留；旧构造器未注入短事务组件时主动拒绝 cleanup，Spring 正式路径使用注入完整依赖的构造器。

- `bash scripts/sso-hard-e2e.sh --release-origin --evidence /tmp/wta-review-sso.json`：exit 0；3 个生产 App 构建及生产 Nginx 模板、真实 Redis/MySQL SSO、5 个 Chrome 场景通过；1 个集成测试和 3 个 client-context 测试均无失败/跳过。System 身份/菜单及业务 token minting 使用既有测试替身；所有本轮容器/network 清理 exit 0。
- SSO 分层检查：`node .agents/skills/namewta-fullstack-development/scripts/validate-module-mode.mjs backend/wta-modules/wta-sso --mode layered`：exit 0。

- 后端打包：`cd backend && ./mvnw clean package -DskipTests`、`bash scripts/ci/verify-admin-bundle.sh full`、`cd backend && ./mvnw clean package -Pbundle-core -Dmaven.test.skip=true`、`bash scripts/ci/verify-admin-bundle.sh core` 均 exit 0。对应 `/tmp/wta-review-package-{full,core}.log` 和 `/tmp/wta-review-bundle-{full,core}.log`；打包阶段跳过测试，测试结果单独列于上方。最后本地 jar 为 core 产物。
- `git diff --check`：exit 0；没有改动相邻 OIDC change，没有修改 CORS/OSS 通配符实现，没有提交或归档。

## Decisions

- OSS/CORS 通配符为用户明确选择；不以策略宽泛本身报缺陷，不改通配符行为。
- Windows 实机保持历史用户豁免 `user-waived/not-run`，不写成实测通过。
- 不修改历史结果、不用 schema 通过代替行为验证，不放宽门禁。源代码修复后运行受影响回归、真实事务测试、前端质量门禁及后端打包。

## Remaining work

本轮 AR-01—07 实现修复与回归已完成；仍需补齐历史票级 Evidence/集成事实，才能关闭 change。`T-43`、`T-48`、`T-49` 的规范 Evidence 文件缺失；另有历史 Evidence 结构、Skill 执行记录和 Done/集成记录不一致。不能将本轮未提交工作树测试回写为历史 clean HEAD 通过记录。Windows 仍按用户豁免处理。

<Path>{roots.workflows}/specdev/A-archive-and-consolidate/A-archive-and-consolidate.md</Path>要求“确认 `change_status: completed`、完成 owner 已写入时间和证据、无 blocker/deviation”。当前 `change_status=active` 且 complete exit 1，因此没有进入机械移动或永久知识提升。<Path>{roots.skills}/archive-and-consolidate/SKILL.md</Path>要求“Stop before side effects when the required input, owner, reference, confirmation, schema, or recovery evidence is missing”。此处阻断来自实际缺失的完成证据；本轮不请求归档确认，因为还没有满足前置门的可执行归档计划。

`DELETING` 或 `COPY_UNKNOWN` 的外部结果无法证明时继续保留对象/引用，需要人工核对；这是有意禁止自动恢复或重发的边界。此审查不宣称全仓不存在其他缺陷。

## 归档激活后续

前述104项是首轮审查时点结果；本轮补充验收、用户明确批准的历史证据接纳方式，以及真实提交坐标规范化已另行记录于 <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/archive-acceptance-2026-09-26/README.md</Path>。T43领取探针失配已修正并真实smoke通过。最终完成与归档状态以提交后complete校验及归档报告为准。
