---
schema_version: 3
artifact: spec
change: 2026-09-14-wta-plus-comprehensive-review
status: draft
ready_for_tickets: false
sources:
  - USER-DECISION: 2026-09-18激活T/P，自主完善全部计划；禁止子代理、保持单并发；不实施产品代码
  - CODE: <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/</Path>
  - BASELINE: <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/worktree-baseline.json</Path>
---

# Spec：基座问题修复与结构收敛

## 1. 问题与目标

以2026-09-18当前源码为准，修正本change发现与实施方案。保留31张票据的可追溯编号；修复真实安全、状态、资源与发布问题，减少重复规则和失效文档。仅修改本change，不实施产品代码。

用户明确要求基座无需兼容旧接口、旧数据格式或旧会话。方案直接切换并同步仓内消费者，不设置兼容期、双协议或外部消费者批准门槛。实际部署与重要数据操作仍不在本轮范围。

## 2. 目标行为

- 日志不保存凭据；正文硬上限在认证前生效，413不能被日志兜底吞掉；来源IP由可信代理合同决定。
- SSO使用CSPRNG、安全Cookie及精确state往返；App回调符合base且可恢复失败。
- 页面状态仅属于当前任务/查询/会话；上传失败可重试，预览失败不伪造上传失败。
- 新个人/企业申请可完成必填材料闭环，服务端owner和提交校验保留。
- 通知结果短事务原子提交并受有效lease约束；callback幂等持久化；企业转移接受排队并按公开query核验投递。
- 部门与Demo树拒绝非法移动；基座CRUD直接切GET查询/POST变更，安全Log与调用方同步。
- 发布使用完整同源产物和不可变版本目录，明确产物切换与运行容器重建的不同边界。

## 3. 验收合同

下表是目标摘要，边界场景、写集和命令以同号Ticket为准。全部为未来实现验收，本次未报告其通过。T-03正文预算与T-23供应商事件身份/保留策略尚未决，Spec保持draft/ready_for_tickets=false；其余29票按已明确的用户范围、ADR及源码合同完成局部Definition of Ready，全局执行仍关闭。

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

## 4. 范围

IN：修订31票及报告、ADR、清理清单、依赖、命令和证据。

REUSE：现有App/domain/platform边界、classic/layered登记、wta-api/common、MySQL唯一基座、PKCE、HMAC、Outbox、OSS owner与工作流任务锁。

OUT：本轮产品实现、提交/推送/部署、运行数据修改、供应商schema重写、历史删除。未经测量的全仓strict、全站换皮、通用lease框架、消息投影系统及新加密协议不作为默认目标。

## 5. 约束与设计决定

1. 无兼容升级不影响权限、Client、事务、供应商协议及资源所有权；同步修改仓内消费者与生成流程后删除旧入口。
2. 只在现有owner不能承载真实需求时扩展接口，不以“统一”创建新模块、重复状态或兜底。
3. SQL结构只改10-cde-base-ddl.sql，数据只改50-cde-base-dml.sql；新表遵循基础字段、主键、注释和entity规则。新环境验收从六份完整基座初始化。
4. 当前Spring事务被实质修改时按工程规则统一DSTransactional，禁止叠加两套注解或靠self-invocation。
5. Release manifest已有digest；修复的是来源和完整性，不重造一套摘要。预期bundle名单独立于待测产物。

## 6. 验证策略

每项confirmed有静态调用链或命令证据；likely/needs-runtime需隔离环境验证，不直接升级为已发生漏洞。按风险运行定向测试；受影响前端门禁、full/core构建、SSO专用E2E及真实服务在实现阶段完成。所有命令记录cwd、退出码、测试数和跳过项。

本轮已执行结果见verification.md及reviews/re-review-command-results.json；原始历史证据保留日期，不冒充当前结果。

## 7. 未决运行参数

规划阻塞仅有T-03正文预算与T-23 callback事件身份/保留策略；关闭条件在对应票据。可信代理算法默认只信peer，显式CIDR作为输入；真实SSO Origin/TLS及CIDR属于发布Gate，不阻止隔离实现。Third permit TTL从既有有界HTTP/重试预算推导，禁止任意常量或无界重试。

T-01生成并验证仓内CI候选；实际启用远程CI/required checks属于外部动作，未授权且未完成。
