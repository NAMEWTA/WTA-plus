---
schema_version: 3
artifact: spec
change: 2026-09-14-wta-plus-comprehensive-review
status: draft
ready_for_tickets: true
sources:
  - USER-DECISION: 2026-09-18最新Goal要求完成全部31票实现与验证；禁止子代理和新worktree、保持单并发
  - CODE: <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/</Path>
  - BASELINE: <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/worktree-baseline.json</Path>
---

# Spec：基座问题修复与结构收敛

## 1. 问题与目标

以2026-09-18当前源码为准，修正本change发现与实施方案。保留31张票据的可追溯编号；修复真实安全、状态、资源与发布问题，减少重复规则和失效文档。最新Goal已授权实现与本地验证，按原31票完成条件推进。

用户明确要求基座无需兼容旧接口、旧数据格式或旧会话。方案直接切换并同步仓内消费者，不设置兼容期、双协议或外部消费者批准门槛。实际部署与重要数据操作仍不在本轮范围。

## 2. 解决方案与外部行为

- 日志不保存凭据；正文硬上限在认证前生效，413不能被日志兜底吞掉；来源IP由可信代理合同决定。
- SSO使用CSPRNG、安全Cookie及精确state往返；App回调符合base且可恢复失败。
- 页面状态仅属于当前任务/查询/会话；上传失败可重试，预览失败不伪造上传失败。
- 新个人/企业申请可完成必填材料闭环，服务端owner和提交校验保留。
- 通知结果短事务原子提交并受有效lease约束；callback幂等持久化；企业转移接受排队并按公开query核验投递。
- 部门与Demo树拒绝非法移动；基座CRUD直接切GET查询/POST变更，安全Log与调用方同步。
- 发布使用完整同源产物和不可变版本目录，明确产物切换与运行容器重建的不同边界。

## 3. 约束与设计决定

1. 无兼容升级不影响权限、Client、事务、供应商协议及资源所有权；同步修改仓内消费者与生成流程后删除旧入口。
2. 只在现有owner不能承载真实需求时扩展接口，不以“统一”创建新模块、重复状态或兜底。
3. SQL结构只改10-cde-base-ddl.sql，数据只改50-cde-base-dml.sql；新表遵循基础字段、主键、注释和entity规则。新环境验收从六份完整基座初始化。
4. 当前Spring事务被实质修改时按工程规则统一DSTransactional，禁止叠加两套注解或靠self-invocation。
5. Release manifest已有digest；修复的是来源和完整性，不重造一套摘要。预期bundle名单独立于待测产物。

## 4. 验收合同

下表是目标摘要，边界场景、写集和命令以同号Ticket为准。T-03正文预算已由用户授权采用JSON/机器各2MiB可配置初值；T-23采用账号内持久身份、永久去重及官方鉴权查询，合同已明确。31票计划合同就绪；30票完成本地review，T-30正在执行正式本地整体验收。所有提交仍由用户暂缓，局部验证不代表全change已交付。

| AC | Ticket | 可观察结果 |
|---|---|---|
| AC-001 | T-01 | 干净clone不创建temp/release也通过事实检查 |
| AC-002 | T-02 | canary不出现在HTTP sink、OperLogEvent、数据库或错误日志 |
| AC-003 | T-03 | 大小边界前/等于/超限一字节结果可判定 |
| AC-004 | T-04 | 任意外来XFF不改变直连或正常入口的授权结果 |
| AC-005 | T-05 | A失败不得删除B的键 |
| AC-006 | T-06 | 生产实现不含ThreadLocalRandom/雪花ID作为bearer |
| AC-007 | T-07 | 复杂state往返相等且无重复code/state参数 |
| AC-008 | T-08 | 每个shipped App均有且仅有完整配套，缺项在promotion前失败 |
| AC-009 | T-09 | build:dev最终三个App均development，build:prod均production |
| AC-010 | T-10 | manifest每个artifact的digest和source可追溯 |
| AC-011 | T-11 | 生产bundle不再携带该共享响应私钥或ECB路径 |
| AC-012 | T-12 | logout超时/401/离线时本地token和动态路由仍清空 |
| AC-013 | T-13 | 服务端关闭注册时入口与页面均准确，后端仍拒绝直接调用 |
| AC-014 | T-14 | 新个人CN_RESIDENT_ID上传正反面后完成提交 |
| AC-015 | T-15 | 旧入口引用为零且替代能力覆盖完整 |
| AC-016 | T-16 | B失败绝不发出A的审批请求 |
| AC-017 | T-17 | 错误origin、错误source、未知payload不能关闭标签 |
| AC-018 | T-18 | 下载URL失败不生成无人回收的Blob URL，也不把已完成上传误报失败 |
| AC-019 | T-19 | 快速筛选时旧响应不能覆盖新列表，失败/loading可恢复 |
| AC-020 | T-20 | 受影响边界类型检查通过，nullable与非法transport样本有明确处理 |
| AC-021 | T-21 | 只用键盘可完成公开流程，焦点可见且错误能被读屏发现 |
| AC-022 | T-22 | 任意一条SQL失败不会留下Delivery/Attempt/Outbox不一致 |
| AC-023 | T-23 | 回滚后相同事件可重试成功 |
| AC-024 | T-24 | 崩溃后permit在规定上限内恢复，不依赖人工删key |
| AC-025 | T-25 | 自父/后代父/并发互移均不能形成环 |
| AC-026 | T-26 | 每个候选有迁移或保留理由，不遗漏调用者 |
| AC-027 | T-27 | 保存/删除路径不再有虚假校验TODO |
| AC-028 | T-28 | 慢provider不阻塞业务提交线程 |
| AC-029 | T-29 | 每个删除文件有owner、内容迁移落点和无丢失硬约束证据 |
| AC-030 | T-30 | 全部已接受AC均有实际命令/退出码/环境/源码checkpoint |
| AC-031 | T-31 | 真实Notify返回QUEUED时send不抛DELIVERY_FAILED，transfer与通知同事务提交 |

## 5. 范围

IN：完成31票实现及本地验证；同步报告、ADR、清理清单、依赖、命令和真实证据。

REUSE：现有App/domain/platform边界、classic/layered登记、wta-api/common、MySQL唯一基座、PKCE、HMAC、Outbox、OSS owner与工作流任务锁。

OUT：未获具体结果批准的提交/推送/部署、真实运行数据修改、供应商schema重写、历史删除。未经测量的全仓strict、全站换皮、通用lease框架、消息投影系统及新加密协议不作为默认目标。

## 6. 数据与公共合同

各票拥有精确字段、事务、HTTP、SDK及测试合同；仓内消费者同批切换。T-03普通JSON与机器入口分别采用默认2MiB可配置预算。T-23按渠道/账号/事件持久去重且不自动过期，原生短信状态通过官方只读签名查询取得；SMTP受理不冒充送达。DDL/DML只修改既有两份产品基座。

## 7. 发布与恢复

本轮只完成可逆工作和本地验收，用户暂缓全部提交。实现、结果和候选SHA保持空，不能用工作树checkpoint冒充commit/direct-parent出口。生产发布、真实运行数据和远程CI启用不在自动授权内；升级不得重放初始化基座，按源/目标Tag差异及备份演练执行。

## 8. 已决参数与部署边界

T-03正文预算和T-23事件身份/保留策略已按用户授权、实际代码及官方协议闭合，不再等待业务样本或供应商文件。真实生产容量、供应商账号试发、实际SSO Origin/TLS/CIDR仍按部署环境验证，不冒充已完成。可信代理默认只信peer，显式CIDR作为输入；Third permit TTL来自既有有界HTTP/重试预算。

## 9. 验证策略


每项confirmed有静态调用链或命令证据；likely/needs-runtime需隔离环境验证，不直接升级为已发生漏洞。按风险运行定向测试；受影响前端门禁、full/core构建、SSO专用E2E及真实服务在实现阶段完成。所有命令记录cwd、退出码、测试数和跳过项。

当前局部实现结果以各票evidence/T-xx.md及源码checkpoint为准；reviews/re-review-command-results.json是历史复核，不能代替当前候选。T-30已在最终654累计owned路径上串行完成可逆门禁，实际启用环境测试及全部浏览器配置；117默认环境skip另有零skip执行记录，源码等价关系和边界见最终覆盖索引。正式提交/发布出口仍暂缓。


## 历史修订

Revision120：T-23开始，610上游hash已核对。用户要求的五种禁用账号预设、必要凭据/签名校验和SMS4J消息ID保留先行；已登记common-sms、DML、通知配置页面、精确父规范与供应商说明写集。官方原生回执均不同于现有HMAC；阿里重试文档互相矛盾、腾讯仅说明再试2次，保留期不猜测。回执安全接入/身份仍在内部研究，不等待用户、不标review。29review/1in_progress/1ready/0Done，全部提交暂缓。

Revision121：T-23三项真实MySQL红灯已复现（回滚重试、跨账号eventId、同事件事实冲突），3/3失败且隔离资源已回收。冻结自定义HMAC接入的渠道+账号configKey+eventId持久身份；业务事实摘要排除每次重签的传输timestamp，其他字段不可变；同一供应商消息多收件人必须明确target，歧义拒绝且不消费receipt。receipt无自动清理，与状态和聚合同事务。原生厂商接入仍单独研究；现有HMAC不宣称原生短信回调。新增common Skill绑定；29review/1in_progress/1ready/0Done，所有提交暂缓。

Revision122：T-23已完成30项真实数据库/HTTP/独立JVM回归及2项JDBC提交故障测试，均零skip；扩展测试首次夹具NotAMock错误已修复，失败记录保留。补齐账号命名空间生命周期：configKey和渠道创建后不可变，已选短信厂商不可替换；账号逻辑删除并永久保留唯一标识，禁止删除后重建同名账号混淆迟到回执。该变更在既有Notify/DDL写集内，随后验证真实Mapper与软删除。原生短信回执接入仍不宣称完成，29review/1in_progress/1ready/0Done，全部提交暂缓。

Revision123：全后端首次838项中1项静态基座断言失败（旧测试要求供应商消息号跨收件人唯一），110项环境测试默认跳过。实际33项MySQL/HTTP验收已证明共享消息号按target关联。先登记精确OssNotifyMigrationUnitTest写集并保存原文，再将过时断言更新为普通关联索引+持久receipt唯一约束；保留既有OSS与基础字段检查，不删测试放宽门禁。前端首次旧参数提示断言失败已按阿里名称/腾讯位置两种合同更新，9项测试/typecheck/lint通过。T-23仍in_progress，全部提交暂缓。

Revision124：带真实GlobalExceptionHandler的回调SQL失败已映射503并可重试，34/34隔离集成通过。复核新增target关联字段会经过HTTP日志，按通知规范的手机号/邮箱不得写日志硬约束，先登记common-json脱敏实现与测试两个精确写集；自定义回执正文/查询参数仅保留脱敏摘要，签名与业务原文不变。之前full/core和838默认测试通过为上一源码阶段；变更后重新运行受影响验证。全部提交暂缓。

Revision128：T-30进入同一工作树候选整体验收。30个前置票已完成本地review，648累计路径全部核对；计划T/P与ticket-control均0error，T-03/T-23原未知关闭。源码88a5a9a25f9c3d88def978ac6aac64d60522638cfa43150e0cfb3f4924a9fee0，重建正式门禁/环境/浏览器矩阵，旧准备日志保留不覆盖。30review/1in_progress/0Done；提交/推送/部署全部暂缓，正式交付出口仍未完成。

Revision129：T-30真实依赖门禁发现T-23新增receipt表未同步受保护初始化器：实际126表/预期125，测试尚未开始即exit1，owned资源已恢复。回到T-23补修，先登记初始化脚本及两份发布合同测试精确写集；T-30暂停为ready，29review/1in_progress/1ready/0Done。保留旧源码88a5a9a的通过与失败证据；修正后重新冻结输入并运行受影响发布/真实服务门禁。全部提交继续暂缓。

Revision130：T-23初始化补修完成review：受保护六文件初始化实际126表，发布117项及真实MySQL/Redis/MinIO八项均零skip通过，资源恢复；仅初始化脚本和两处旧计数断言变化。T-23-checkpoint-v2共50路径，累计649路径；源码1dff1a345e1979d809bb547f3060645d86b4508f3df3aad5fd227de53ab2c504。T-30继续整体验收，30review/1in_progress/0Done；未受影响的前端/后端源树逐文件相同，旧验证证据按明确输入等价关系关联，受影响release/external已重跑。全部提交暂缓。

Revision131：T-30补跑默认环境skip发现5个夹具错误：Admin菜单使用失效裸图标，两个OSS测试从旧标记截取至EOF导致重复建表，Profile以分号直接切SQL破坏坐标字面量且截取后续无关域。在本票既有admin测试写集内登记4测试及1共用SQL执行工具：复用真实基座DDL、限定片段、使用Spring SQL脚本解析，Profile在owned空数据库完整初始化五份业务基座并清理全部所建表；保留所有权限/数据/失败关闭断言，生产SQL不放宽。保留T-30-extra-services-v1的15项/5错误，修复后重跑受影响闭包；30review/1in_progress/0Done，全部提交暂缓。

Revision132：T-30补查全部环境门控/Tag发现9个Profile e2e类未被默认Maven选择；真实MySQL补跑15项，13通过/2失败/零skip。企业申请夹具credit(suffix)拼接任意末位，不满足当前统一社会信用代码校验码合同，save在业务入口即被拒绝。先追加该EnterpriseApplicationMySqlE2ETest.java精确写集，仅修正合成合法身份数据并增加响应code断言，保留发布/重新认证/唯一约束/工作流回滚断言及生产校验。浏览器163项及额外10工作流弹窗已实际通过；30review/1in_progress/0Done，全部提交暂缓。

Revision133：T-30本地整体验收完成review。最终源码bc561a9c45850bdb0a8783a7d9700ef299a2d0bd774fecbebb55fb3dc108b3ba，3577源码/654累计owned路径；6测试修复、1上游重叠/648非重叠不变。18核心门禁通过，前端722、浏览器163+工作流10=173；默认后端740通过/117环境skip均有专项零skip闭合，46环境类/264测试源逐类对应，额外非默认选集53通过/1既有教学Disabled占位。最终full/core构建/清单、117发布合同、真实依赖与发布恢复、五分层/facts检查器通过。全部31票review、0Done；仅本地产物可审查，干净提交/正式发布candidate/direct-parent出口因用户全change提交暂缓保持未完成。最终治理结果见T-30-final-governance.json。

Revision134：完成逐票出口复核与本地产物重新验hash，3577源码/654owned路径无漂移，两JAR及三App保留副本一致、HEAD不变且index为空。T-01/T-02/T-04/T-05早期验收勾选尚未承接最终实际证据，已按各项源码/测试报告补齐并注明旧失败由后续票关闭；T-02历史日志处置/凭据轮换属于OUT且无批准，继续明确未执行。31review/0Done；非空implementation/result、direct-parent和干净正式发布候选仍受用户全change不提交约束。没有新产品改动，不重跑已证明输入未变的业务测试。

Revision135：用户明确授权全部 commit push；当前批次按已验证工作树形成票据主题提交并推送 origin/main。用户人工检查环境继续运行，真实部署与生产配置验收不在本次操作内。

Revision135 提交结果：43个非空实现提交已形成，覆盖31票。完整实现 result SHA=`6c8764cca97bb6057fcb90ccdfe635c7efbf502a`；3577源码路径经Git归档逐文件核对，与T-30最终输入完全一致，提交父链与包含关系真实通过。票据保留review，正式发布候选/生产配置出口未冒充Done。详细证据为 evidence/commit-delivery.json；推送目标 origin/main。
