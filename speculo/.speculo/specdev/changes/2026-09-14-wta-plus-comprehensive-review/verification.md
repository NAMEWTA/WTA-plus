# 验证计划与本轮实际结果

本轮代码基线 `1264980c74e594bc594e88561bb292fbe5d968a1`；只执行静态读取/Git/文档检查。下面业务命令均是未来执行计划，**本轮not-run**。旧成功/失败记录仍只对原输入有效。

## 本轮实际验证

- 启动Git工作树clean，当前仅本change文档差异。
- 报告60引用hash比对：50相同，10仅作者/联系元数据；<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/current-source-audit.json</Path>。
- 旧31implementation commit存在且是HEAD祖先；并记录后续写集漂移，未验证旧当时clean/逐票出口：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/legacy-ticket-audit.json</Path>。
- 初始tickets/control各exit1，11条common Skill摘要漂移；原配置宽共享路径产生219警告。已读取当前common入口并重绑活动票，旧Skill执行记录不改。
- 最终G/S/T/P、ticket-control、git diff --check均exit0。计划发布后有443组已声明owner的共享路径警告，current单人串行处理；frontier与in_flight均空（Goal执行未授权）。实际校验结果写<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/replan-validation.json</Path>；structural pass不表示G共识、Ready或业务通过。

## 当前候选业务矩阵（未来执行）

每条独立执行，记录cwd、源码HEAD/tree、命令、环境、exit、tests/skip及产物摘要。不能把test list算E2E；required环境用例必须实际执行零skip。

| 范围 | cwd | 命令 | 责任与断言 |
|---|---|---|---|
| 工程事实 | 仓根 | `node .agents/skills/engineering-standards/scripts/validate-skill-facts.mjs` | T-01/29/30；不靠放宽规则 |
| Notify分层 | 仓根 | `node .agents/skills/namewta-fullstack-development/scripts/validate-module-mode.mjs backend/wta-modules/wta-notify --mode layered` | T-35—43/50；DAO/UseCase/adapter真实边界 |
| 全后端 | backend | `./mvnw test` | T-30；单元通过与环境skip分开 |
| Notify定向 | backend | `./mvnw -pl wta-modules/wta-notify,wta-admin -am test` | 真实dispatcher用例不能mock NotifyClient |
| CORS定向 | backend | `./mvnw -pl wta-common/wta-common-web,wta-admin -am test` | profile＋HTTP正负向 |
| 前端全量 | 仓根 | `pnpm --dir frontend test` | T-30；各包实际计数 |
| 类型与lint | 仓根 | `pnpm --dir frontend typecheck`；`pnpm --dir frontend lint` | 新分页/附件/页面合同 |
| 架构 | 仓根 | `pnpm --dir frontend architecture:check`；`pnpm --dir frontend architecture:test` | 不跨包深导入 |
| OpenAPI | 仓根 | `pnpm --dir frontend --filter @namewta/tooling-openapi openapi:check` | 正式生成，禁止手改 |
| 发布合同 | 仓根 | `bash release-artifacts/scripts/verify-release.sh` | 全部发布/SQL/恢复测试 |
| 已有隔离服务 | 仓根 | `bash scripts/ci/run-external-services.sh` | 脚本会建/清自有Docker资源，仅执行获授权后用 |
| full | backend→仓根 | `./mvnw clean package -DskipTests`；`bash scripts/ci/verify-admin-bundle.sh full` | 先保存full摘要再core clean |
| core | backend→仓根 | `./mvnw clean package -Pbundle-core -Dmaven.test.skip=true`；`bash scripts/ci/verify-admin-bundle.sh core` | 不把skip打包当测试 |
| App产物 | 仓根 | `pnpm --dir frontend build:dev`；保存标记后`pnpm --dir frontend build:prod` | 三App各模式及完整manifest |
| 浏览器默认 | frontend | `pnpm exec playwright test --workers=1` | 默认选集不能覆盖专用配置 |
| SSO专用 | frontend | `pnpm exec playwright test --config playwright.sso.config.ts --workers=1` | 先准备隔离服务/Origin，非生产凭据 |
| Profile/transfer/upload/lifecycle/registration/transport/accessibility | frontend | 逐个`pnpm exec playwright test --config`实际对应配置文件`--workers=1` | 全部配置路径见下面源清单，逐类用例和截图 |
| 启动脚本 | 仓根 | `bash -n scripts/start-dev.sh`；`bash scripts/ci/verify-dev-build-guard.sh` | T-48还需实际首次/二次启动和repair负向 |

专用配置真实存在：<Path>frontend/playwright.accessibility.config.ts</Path>, <Path>frontend/playwright.lifecycle.config.ts</Path>, <Path>frontend/playwright.phone.config.ts</Path>, <Path>frontend/playwright.profile.config.ts</Path>, <Path>frontend/playwright.registration.config.ts</Path>, <Path>frontend/playwright.sso.config.ts</Path>, <Path>frontend/playwright.transfer.config.ts</Path>, <Path>frontend/playwright.transport.config.ts</Path>, <Path>frontend/playwright.upload.config.ts</Path>。T-34/40/41新通知浏览器场景需登记在选集中，不自动假设旧配置覆盖。

## 真实服务启用及验收边界

当前<Path>scripts/ci/run-external-services.sh</Path>的固定选集不含全部NotifyAtomic/OssMigration用例，不能跑它一次就宣布新风险关闭。责任票必须复用其隔离Docker准备方式，增加明确测试选择器，并检查每个Surefire XML执行数和skip：

- NotifyAtomicResultIntegrationTest：`-Dnotify.atomic.integration=true`、`-Dnotify.mysql.integration.url`与`.username/.password`、`-Dnotify.redis.integration.port`；全部指向本票隔离实例。
- OssStorageMigrationIntegrationTest：`-Doss.migration.mysql.integration.url`与`.username/.password`、`-Doss.minio.integration.endpoint/.access-key/.secret-key`。真实双连接＋屏障不以mock行锁替代。
- OSS readiness/权限：同MinIO属性，使用只具目标对象能力的测试身份；Policy/ACL403和匿名读取事实独立断言。
- 现有测试使用`@Tag("dev")`与系统属性/assumption门控；定向命令需`-Pdev -Dtest=实际类名 -Dsurefire.failIfNoSpecifiedTests=false`，该参数仅允许无此测试的reactor模块，不允许目标测试零执行。新增方法选择器在实现后登记精确名称。
- 本轮不使用仓库披露的local连接参数，也不创建外部资源。若缺Docker/浏览器/Windows或专用测试条件，明确阻塞对应required验收，不宣称通过。

## 报告新增场景的必需证据

T-34关闭实时仍REST；T-35真实dispatcher短信；T-36双收件人同意图并发与commit故障；T-37拒绝→接受两次实际Provider调用/UNKNOWN不重发；T-38path/delivery/零任务；T-39截止零发送；T-40撤回时序与本人快照；T-41第501条/他人隔离；T-42生产附件装配＋真实引用/零附件零OSS；T-43SQL成本基线；T-44可选坏存储核心启动；T-45未知三态；T-46清理恢复两时序及删除确认丢失；T-47A慢B快；T-48正常二启与repair；T-49默认/历史service；T-50真实调用方同步与非法输入零持久化。

## 完成与归档

T-30汇总AC/样本/命令/源码/产物和旧票处置，按<Path>{roots.workflows}/specdev/common/rules/change-completion.md</Path>真实clean/direct-parent证据完成；无权限部署不冒充已部署。所有仍适用的外部凭据风险必须处置或用户明确决定，不能以“计划写完”归档。A单独授权，永久ADR只在毕业网关提升。

完整性检查：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/replan-integrity.json</Path>证明47份before快照与HEAD原文件逐字节一致、旧31票验收条目全部保留、原报告完整复制，产品/相邻change/永久知识与旧Evidence均未变。本轮修正Ready模板中的未决问题段格式；失败曾为Spec 1条与Ticket 50条格式判定，修正后重跑通过，未修改校验器或放宽规则。
