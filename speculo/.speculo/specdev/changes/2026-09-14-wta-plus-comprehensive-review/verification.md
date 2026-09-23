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

## revision181 — T41完成

Revision181: T41 accepted at c21de75f, recovery attempt3; G real501 Chrome1/0/0/0 and SQL A501unread0/B2 unchanged, original timestamps preserved, exactclean source/JAR and cleanup[]. Prior3 plus recoveryE/F failures immutable. Explicit D/C/E/F same-input test/build/contract reuse; both reviews pass.13done/2cancelled/35ready; nextT40; Goal active, no archive.

## revision182 — T40实施开始

revision182：T40开始，base601b9273；13done/2cancelled/1in_progress/34ready。T41已完成，新增为前置。cors_audit唯一产品writer；Lead治理/提交/隔离验收。先现API真实撤回红灯，再版本栅栏、三处链接一致、本人深链与竞争/回滚/浏览器验收。

## revision183 — 后端分段反馈

revision183：T40后端分段A2在a638ef48通过16定向单测、6真实MySQL/Redis，0fail/error/skip，源码前后clean、cleanup[]。A769e5d17元数据类型编译失败保留；公共Map<String,String>不变，内部noticeVersion用严格JSON字符串。完整候选attempts0；剩余竞争/重试/旧来源矩阵、前端和浏览器待实施，13done/2cancelled/T40in_progress/34ready。

真实run `ee99f3fbd678653d`，源码 `a638ef485c3819bfc8d67553877fd7c106ef9579`、tree `ceb4a5354641b7327bc65728bfaeb0f6678c52d8`；六SQL103表，2容器/1匿名卷/2端口及Maven进程组已回收。通过原红灯、两静态seed/普通missingIntent拒绝、late Snapshot SQL trigger完整回滚、V1撤回/V2送达、真实MAIL planner外部-only与混合三处path，未使用真实供应商。定向5类16测试均通过。原始失败与当前fresh XML/命令/哈希见 `T-40-current-2026-09-23/backend-a2/manifest.json`。这些是预先声明的后端反馈，未冒充整票验收。

Dispatch01C：cors_audit在原22写集内继续补充真正双连接gate先后/已获发送权真实ACCEPTED与UNKNOWN落库、旧owner、duplicate/retry零唤醒、legacy UNVERIFIED、本次Notice行数/关联冲突回滚等遗漏；保留现有6项绿灯。由Lead固定下一后端候选并串行运行，再移交前端/浏览器。若路径不足先登记；产品writer不自行构建、启动服务或提交。

## revision184 — 后端竞争矩阵与前端派单

revision184：T40后端A4在5fc319ed通过17真实MySQL/Redis测试（含双JDBC连接行锁）；A3共享8类135项通过，均0fail/error/skip、源码前后clean、cleanup[]。A2单测16沿用原始坐标且生产输入等价。13done/2cancelled/T40in_progress/34ready；完整候选attempts0，前端与真实浏览器尚待验收。

原始结果分别为A3 `270bd94e3f59a784929448e2cfa84ae913b686b7` / run `5102c97369fbecac`（16），共享 run `b1d860dd4901770e`（135），A4 `5fc319ed58d9586a92166459b3296618e7dc47dc` / run `526ecc242d191177`（17）。A4 tree `445d0a24596b32360a3b54de94c272599ad866bd`。17项包含真实双连接锁阻塞与提交后栅栏、真实provider第二gate后的三类结果保留、旧lease owner、duplicate/retry、legacy来源、版本隔离及两类晚期SQL回滚。外部provider为测试替身，不声称真实供应商发送。三个隔离run容器/卷/端口/进程组均回收。原始XML、日志、哈希及明确复用说明见 `T-40-current-2026-09-23/backend-a4/manifest.json`；有界A2安全审查不替代最终整票审查。

Dispatch01E：cors_audit作为唯一产品writer，在原22写集内完成宿主懒加载query订阅、本人分页外detail、query/session代际隔离、历史管理链接按本人messageId转换、撤回提示和两条Skill事实reference；补领域/SFC/宿主测试及独立T40真实浏览器驱动。浏览器验证真实HTTP发布→Worker送达→撤回后本人快照，A/B权限隔离与同页query；不得SQL伪造发布证据。Lead独占治理、提交、构建及服务，writer不运行构建/服务或提交。新增路径超范围须先登记。候选交回后冻结源码，再串行运行定向/全量门禁及真实Chrome；本checkpoint不是整票通过。

## revision185 — 前端定向通过，浏览器实施派单

revision185：T40前端分段F4在461b0a43通过112项定向测试及3包typecheck；F1 eb8cefe0真实后端18项全部通过、源码前后clean/cleanup[]，后端自F1未变。F1/F2/F3失败及诊断已保留。13done/2cancelled/T40in_progress/34ready，完整候选attempts0；真实Chrome、完整门禁及两条Skill事实尚待完成。

F1 `eb8cefe0f67895e75832b2a15d8f955b7b01e447` / tree `33ef52d0f1250c6221cb4ad3f586563d84332c67` 的真实run `c7fac20c13a862b2` 为18/0/0/0，新增首次READY MAIL未授权前撤回零外呼和provider ID保留断言。前端首次工具PATH错误127未启动测试；修正环境后111pass/1fail，3包typecheck通过。F2 snapshot正向通过而subscribe失败；F3临时诊断揭示Vitest并发懒导入加载真实App HTTP导致ClientContext缺失。F4仅在测试中等待已取消导入结束，再验证有效订阅，保留全部正负断言；生产未放宽，临时console未提交并已删除。当前 domain7 + webdomain25 + Admin80 =112全部通过，3包typecheck通过。结果/原始失败见 frontend-f1 与 frontend-f4 manifest。SFC缓存页检查调用实际组件注册的生命周期hook，不能冒称真实浏览器KeepAlive交互。

Dispatch01F：cors_audit唯一产品writer，在原22写集内继续独立T40真实Chrome两案例、owned runner及离线安全测试，更新两条已声明Skill reference。依据冻结设计与 `/tmp/wta-t40-browser-design.md`，真实HTTP发布→实际Worker送达→撤回后普通A快照可读，B foreign/absent拒绝、历史管理链接按本人消息导向、同页query与会话迟到隔离。控制HTTP允许，禁止伪造发布/详情响应；SQL fillers明确标识。Lead独占治理/提交/构建/服务；writer不得运行构建、测试服务或提交，超范围先登记。交回完整源码后冻结并串行执行剩余必需门禁。

## revision186 — C1门禁反馈与定向补修写集

revision186：T40完整候选C1 2c8047a2首次验收失败；前端652 Vitest＋108工具测试、全量检查与三App构建通过，浏览器离线15通过；后端默认测试中两项旧notice-published邮件夹具未触发send，待核查修复。完整candidate attempts1；full/core打包及真实Chrome尚未执行，13done/2cancelled/T40in_progress/34ready。

C1前端每步前后均同一clean HEAD/tree，补齐此前F4 clean:false来源限制。两份固定C1静态审查未发现生产合同阻断，但不能代替失败的默认测试与未运行浏览器。全部已执行记录、fresh XML及审查见complete-candidate-c1/manifest.json；token仅按精确JWT模式脱敏并保留原始哈希/替换数，其他字节保留。默认失败为DispatchNotificationServiceTest的recipientMinuteCapIsIsolatedByScene与noticePublishedMailRendersWrapperNotCallerSnapshot，均send未调用。Lead登记该测试文件作为第23写集，先定位并按新noticeVersion合同补齐夹具，保留两项原目的断言，必要时增加缺事实失败关闭负例；不得放宽生产栅栏。cors_audit唯一产品writer，Lead治理与全部服务/构建。

## revision187 — C2默认/构建通过，浏览器前置失败

revision187：T40完整C2 851a6d6e仍未验收；两旧邮件夹具修复＋缺marker零发送负例通过。默认首次OOM137失败保留，同源限定1536MiB重跑877执行通过/216环境skip；full/core打包及全部静态门禁0。真实browser前置探针或登录阶段失败，Chrome未启动，source/JAR一致且cleanup[]。attempts2；下一仅补安全阶段诊断，13done/2cancelled/T40in_progress/34ready。

C2树a6d6fe1e3c5c372372ceb34bae8a191d44c46d47。默认初次未设堆上限，kernel OOM杀死Java PID1142623，fresh237类1009项0fail/error/198skip只属未完成证据；随后同源码JAVA_TOOL_OPTIONS=-Xms128m -Xmx1536m重跑默认260类1093项0fail/error、216skip，877实际执行通过；不修改规则/排除测试，全部fresh XML均在clean前保存。C1前端与浏览器离线按输入完全相同且329产物hash一致复用；F1真实18/A3共享135保留原SHA和明确差异清单。C2 full JAR SHA a6cece4cdbe349f3529870ee39d2397c85477513da1d603c2d40d1eda2c369a1；run19a061800c7e3c0f真实隔离六SQL103表、3容器/2卷/5端口和backend进程全部回收，但固定RuntimeError尚不能区分auth/code探针或WTA/A/B登录失败，不猜根因、不计浏览器通过。core随后完成，target现为core，下一browser必须重新full打包。

Dispatch01G：cors_audit只改已授权frontend/e2e/run-notice-retraction-real.py及test_run_notice_retraction_real.py，补固定阶段白名单、HTTP/R.code受限整数、backend退出码及本脚本帧行号，不持久化任意异常串/HTTP正文/凭据。保留严格真实控制链路、两Chrome案例/0skip/0retry、owned清理与固定source/JAR。加入401/R401/缺token/探针退出/canary/伪阶段与数字边界离线检查。Lead独占所有服务/构建与提交；未定位事实前不改产品行为或放宽验收。

## revision188 — 三次尝试停止与复盘入口

revision188：T40完整候选C1/C2/C3三次均未验收，停止本批集成并先复盘。C3 d5b4f5f8 full打包/17离线通过，真实run630336ccabc8052b确认backend探针200、控制账号login HTTP200/R500、未开始Chrome，source/JAR一致、cleanup[]。已定位缺User-Agent解析为null的登录调用链，待同版依赖复现与独立审查后登记窄修恢复包。13done/2cancelled/T40in_progress/34ready；Goal active。

C1为旧notice fixture失配；C2生产及默认门禁已通过但browser前置失败，另保留OOM和有界重跑；C3只加安全诊断并复现login_control业务500。不能把探针200或离线通过当公告/Chrome通过。三次记录不删除、不静默清零；完整C3记录见complete-candidate-c3/manifest.json。Lead暂停进一步集成尝试，先记录根因/影响边界、独立审查、下一次实质改变、恢复入口与验证策略。当前静态事实：精确full JAR hutool-http5.8.47对空UA返回null；LoginHelper、同步UserLoginSuccessListener、异步SysLoginInfoServiceImpl均需核查。拟保留无UA真实HTTP为回归，不靠添加UA掩盖生产空指针。未批准新路径前不修改这三个文件。

## revision189 — 四项复盘与Dispatch02

revision189：T40前批3次失败保留；同版UA探针及独立审查完成，Dispatch02四项复盘已落盘，事前扩3生产＋1精确单测路径并绑定common Skill。恢复批attempts0，先真实消费者单测红灯再三处Unknown窄修/真实无UA登录与审计回归；不补请求头绕过。13done/2cancelled/T40in_progress/34ready，Goal active。

权威恢复包：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/dispatch-T-40-recovery.md</Path>；精确依赖探针源/实际命令输出、分析与独立审查见recovery-diagnosis/manifest.json。恢复包先于产品修改落盘；不得删除旧三次记录或将旧完整候选称为passed。

## revision190 — 隔离红灯及生产补修派单

revision190：T40隔离UA真实消费者红灯已确认，clean828ad32f：10测试/6NPE失败/4正向通过/0error/skip。两次编译和一次隔离初始化失败单独保留，不当业务红灯；默认静态单例污染已通过子JVM隔离消除，真实Spring转换工厂及生成mapper参与断言。Dispatch02B三处Unknown窄修与真实登录/在线/审计取证已派；恢复完整候选attempts0，前批3失败保留。13done/2cancelled/T40in_progress/34ready，Goal active。

证据见`T-40-current-2026-09-23/recovery-ua-isolated/manifest.json`。原6eb5783c非隔离red仍保留；ee8e6d83编译失败、b7a12bc1十项HARNESS初始化失败、de891cc4单例诊断均不冒称业务NPE。828ad32f才为有效隔离red。每个子JVM限45秒、384MB，finally回收leader/后代并删除临时文件；父默认Surefire不初始化RedisUtils/MapstructUtils。生产签名、HTTP schema、认证/权限不变。

后续真实18/共享135原driver安全环境会过滤外部JAVA_TOOL_OPTIONS，故各复制一个新版本，仅加入固定本轮-Xms128m/-Xmx1536m；原版本保留，精确diff/hash/新driver随证据留存。没有扩大测试范围或降低断言。新候选须重新执行默认、真实18/135、fresh full/core、静态、两Chrome；前端未变输入按既有等价证据复用。

## revision191 — R1完整应用发布失败与JSON合同核查

revision191：T40恢复R1 c8604809未验收，UA10/离线21/默认887pass216skip/full/core/静态、真实18及共享135均通过；run0c8f89704398d48a已实证三次无UA与Chrome登录/在线/异步审计全通过，但notice_publish HTTP200/R500，Chrome尚未开始，source/JAR一致cleanup[]。发现生产大整数JSON字符串与版本栅栏只收数字不兼容，下一仅先真实Jackson回归红灯。恢复attempts1、前批3失败保留；13done/2cancelled/T40in_progress/34ready，Goal active。

完整证据：`T-40-current-2026-09-23/complete-candidate-r1/manifest.json`。默认261类1103项中887实际通过、216环境skip；真实18 run da0dee6e6a6ce32f及共享135 run46fb407df36ab094均0fail/error/skip并清理完成。full JAR SHA59dda7ff6e9b8e94f903310bc71bbbab439b6ff50f087d825b2e23d4f2d56978，target随后core覆盖，下一browser必须重新full打包。前端760测试/三App329产物按与C1输入等价复用；Python21单独通过。

Dispatch02C限定既有NotifyNoticeVersionFenceTest路径先复现：使用真实JacksonConfig module和超过JS安全范围的ID，经initial/requirePublishedIdentity/state/retractedMetadata往返；用scoped JsonUtils.getJsonMapper替身绑定真实mapper而不修改其全局缓存/Spring状态，保留旧3例与负向校验。Lead记录test-only诊断红灯后才派既有NotifyNoticeVersionFence路径窄修：接受生产规范ASCII正整数字符串及旧数字形式，严格正值/Long范围，继续拒绝空白、符号、前导零、小数指数、溢出、boolean及身份不一致；不换全局序列化器、不取消版本栅栏、不新增API/SQL。必要字段表示说明仅在既有Skill refs中同步。

服务器异常处理仅记录异常类而非完整栈，本次不冒称已采集唯一服务器异常根因。源码序列化差异须由上述红灯及修复后的真实publish/retract/Chrome来确认；安全观测提案保留为备用，不因已有静态线索而重复无修改完整运行。下一完整R2必须新default/real18/shared135/freshfull/browser2/core/static，不能将R1业务失败追认为passed。

## revision192 — 生产Jackson红灯与Dispatch02D

revision192：T40生产Jackson大ID回归已实证：5项/1真实ServiceException失败/4通过/0error/skip；新发布版本元数据在真实序列化配置下无法自验证。Dispatch02D只修既有Fence私有数字解析与Python消费者相同表示、负向测试及必要既有Skill说明；不改全局mapper/身份/权限/SQL。恢复完整attempts仍1，R1原失败保留；13done/2cancelled/T40in_progress/34ready，Goal active。

诊断源码`9faeab8740f211ecfbc3644a4ffcad51076df17e` clean，raw XML/命令/独立意见见`T-40-current-2026-09-23/recovery-json-red/manifest.json`。当前成功序列化断言先证明noticeId/snapshotId确为字符串，失败落在requirePublishedIdentity，排除了仅夹具启动失败。仍待修复后的真实publish/worker/retract/Chrome闭环，不冒称R1服务器栈已观测。

02D唯一writer cors_audit，仅既有已授权路径：NotifyNoticeVersionFence.java、NotifyNoticeVersionFenceTest.java、run-notice-retraction-real.py、test_run_notice_retraction_real.py；必要事实说明可在既有两个Notify Skill refs同步。Java String须ASCII `[1-9][0-9]*`、≤19字符且longValueExact，旧数值表示保留。Python notice_version_fact也须同等严格接收int（禁止bool）或规范正int64字符串，对notice/snapshot/version继续精确比较；不能让字符串兼容放过浮点、符号、空白、前导零、Unicode数字、溢出或身份错配。保留所有旧负例、真实UA证明和两Chrome断言。Lead独占新候选所有测试/服务/提交。

## revision193 — R2真实发布闭环通过，seed排序夹具待修

revision193：T40恢复R2 8a387192仍未整票验收；Fence5/离线22/default889pass216skip/full/core/静态/真实18/共享135均通过。真实run318e1b1d8873fa80已完成UA3+Chrome、公告发布→Worker送达→撤回、2普通用户管理GET403；仅seed分页位置合并断言失败，Chrome尚未开始，cleanup[]。下一仅修runner合成数据时间锚点，恢复attempts2、前批3失败保留；13done/2cancelled/T40in_progress/34ready，Goal active。

完整证据见`T-40-current-2026-09-23/complete-candidate-r2/manifest.json`；新default261类1105项、真实18 run9a2dc8c991096685和共享135 run3e74344a4b3fd209已在clean前保留fresh XML。R2 full JAR SHA c2b023231d0713b1c17ca9940b94a6937af53b627205d574151ad589126d79a9。随后core覆盖target，下一次须fresh full构建。R2发布成功实证大ID表示修复已穿过真实生产调用链；seed失败未留具体计数/时间，不能把时区推断写成已观测唯一原因。

Dispatch02E限定现有两个Python文件（run-notice-retraction-real.py/test_run_notice_retraction_real.py）；必要README可同步。合成22行时间须取本次V1/A精确且唯一、create_time非null的持久关系时间+1秒，禁止用独立NOW/修改V1/改业务时区掩盖；写入前条件不成立则失败。保留22/1/22/1/0数量、20条首屏、V1离页、legacy前10及全部Chrome/owner断言；失败观测仅计数/布尔和固定阶段，不存ID/正文/token。离线覆盖缺失/歧义/空时间、时钟错位和数量/排序失败。

若产品/TS/依赖/SQL/Skill完全不变，R3可依据精确diff复用R2默认/真实18/135/core/静态和C1前端证据；仍必须新Python离线、新exact-source full JAR及两真实Chrome零skip/零retry，source/JAR一致cleanup[]。第三完整恢复候选若再失败，保留三次并执行既定四项复盘，不盲重试。Lead独占服务/提交，writer不并行构建。

## revision194 — 恢复批第三次失败已停止

revision194：T40恢复R3 378f8481未通过；离线25/fresh full及bundle通过，真实发布送达撤回/seed通过，Chrome 1pass1timedOut、0skip/0retry，cleanup[]。B越权矩阵通过；A超时仅保留finally unroute:175位置，不据此断言根因。恢复批3次已停止，保留前批3次与R1/R2/R3失败，进入只读四项复盘；13done/2cancelled/T40in_progress/34ready，Goal active。

证据 `T-40-current-2026-09-23/complete-candidate-r3/manifest.json`；run f3ff781212c46752，HEAD 378f8481deae56bf7b6971ef1b1b2e66c47ca8fe，tree 37a8e633d9f78d6285bf0dba51c51b053e3b981b，full JAR SHA f8e7bdb0723e7f5543f674f59c11b92121e87ff46b401b49341c8fe275ccf50e。source/JAR前后同值，3进程组/3容器/2卷/5端口回收。准确diff仅2 Python＋README，R2后端default889pass216skip、真实18/共享135/core/静态与C1前端760及3App构建按输入等价复用；未伪称本次重跑。真实seed 22/1/22、V1离页、legacy前10通过；Chrome A timedOut/B passed，各1attempt。post-browser验收未完成。

下一步骤：保持产品冻结与服务停止，Lead保留失败；cors_audit与ops_audit独立只读审查跨会话挂起请求、注销/登录、response事件和finally清理。先记录根因证据/不确定性、独立审查、实质变更和恢复入口四项，再派遣窄修。不得加timeout、吞取消、删断言或将cleanup位置冒称原失败位置；当前不启动第4次完整候选。

## revision195 — 跨会话浏览器四项复盘与受限恢复

revision195：T40两批各3次失败保留；跨会话测试取消/同URL误认的四项复盘与独立审查已落盘，Dispatch03只修现有E2E及必要安全观测，不改产品、不放宽断言。新恢复批attempts0，需新全前端/full JAR/Chrome2；13done/2cancelled/T40in_progress/34ready，Goal active。

权威派单 `evidence/dispatch-T-40-browser-recovery.md`，原始报告 `evidence/T-40-current-2026-09-23/recovery-browser/manifest.json`。原超时具体挂点仍未观测；下一版按精确Request验证真实注销取消、保留同页迟到成功与同context B本人正控制。Lead唯一治理/提交/测试owner，cors_audit唯一E2E writer；源码交回前不运行服务。

## revision196 — Dispatch03三次失败停止，交互可操作性复盘

revision196：T40 Dispatch03三候选均失败并停止；S1/S2 lint失败及隔离诊断保留，S3 ddc0b51d已过strictE2Etypes/全前端760/3App329产物/fresh full，但Chrome A timedOut、B passed、0skip/retry，source/JAR同值cleanup[]。正在只读核模态遮罩挡住logout操作与安全阶段诊断；13done/2cancelled/T40in_progress/34ready，Goal active。

证据 `T-40-current-2026-09-23/complete-candidates-s1-s3/manifest.json`；S1 70bb5996、S2 c4e0d597均在lint失败，后续门禁未执行。隔离3case证明直接throw AggregateError的cause第三参识别问题；命名构造保留primary/cleanup/cause，未改lint规则。S3 ddc0b51d6c3278f9e55e9c0aa21bde614958e6cb tree 67eeb3947790d010933d43e566d40ec6491a950a；Chrome run434baf70aa25766e，full JAR SHA 5a7db069f3f9fd27d32f457b0c0c773d78df3303260646415491b3b8e6f4379a。前端652Vitest+108Node、额外101架构、全lint/typecheck/OpenAPI/三Appbuild实际通过，329产物哈希保留。后端R2与PythonR3按精确输入等价复用，不冒称重跑。

A安全位置22是Aggregate清理现场，原始report已按安全合同删除，不能声称已证最初卡点。Lead静态发现openDetail在请求完成前打开el-dialog，而测试挂起响应后直接点击被模态覆盖的头像菜单；独立审查确认后才派下一窄修。候选批已停，保留前两批各3次及本批3次，不直接启动第4次；下一须先四项复盘、真实用户可操作路径和安全阶段诊断，禁止force click、加总timeout、忽略取消/错误或删除断言。
