# T-45 Dispatch01 — 有界诊断事实

## revision205 — T-45启动与范围登记

revision205：T44已完成（结果fc50c1e，治理4c93a2f）；T45激活并先复现403/未知事实红灯。15done/2cancelled/1in_progress/32ready；Goal active，未完成或归档。

基线 `4c93a2fb9c7d2c1d68a0c8194197e2af1908b4d2`，main/current/direct-parent。T44 已提供单配置管理员 POST 与安全三字段 VO；本票将该 VO 扩展为有来源/范围/时间的安全事实投影，正式重新生成 OpenAPI 与前端映射。方法、权限、正ID和审计禁正文保持。策略/ACL 403不能伪装空策略；单对象匿名 HEAD/GET 仅陈述该对象，404/超时/网络异常/重定向不得推断匿名拒绝。PRIVATE未知不能宣称全桶安全或匿名写已禁止；PUBLIC_READ对象可读而策略不可读不能确定POLICY_MISMATCH。

仅解释明确、无条件且匹配目标资源的策略子集，尊重 Deny，Condition/Not*/不明Principal/Action/Resource及坏JSON保留UNKNOWN；可识别危险写以有界风险警告表达，不称实际PUT成功。保留每个独立读取的部分事实；不匿名PUT/DELETE、不提权、不影响T44业务/核心就绪解耦。每网络步骤100ms–3s/最多5步的已接受预算保持，无未回收后台任务。

公开 Java 结果按已确认仓内同步切换决定迁移全部实际调用者，java-api-compatibility用于调用清单/语义/编译核查，不新增已被用户排除的兼容桥。写集从6扩为17条：模型/Javadoc、管理VO与对应HTTP测试、domain transport/测试/出口、正式OpenAPI生成、运行文档及system模块事实。目录授权仅限本票行为，原快照不可覆盖。新增其他文件先回Lead登记。

Dispatch01A仅可改 `backend/wta-admin/src/test/java/org/namewta/test/oss/readiness/OssAccessDiagnosticUnitTest.java`，用旧公开合同可编译断言复现PUBLIC_READ+policy/ACL403误报和PRIVATE未知误称writeDenied；生产不改。Lead固定红测试提交并实际运行后再给01B写锁。legacy_audit独立只读合同审查；ops_audit仅准备隔离驱动。Lead独占治理/提交/Maven/pnpm/服务与生成。最终需受限MinIO真实零skip、HTTP权限/事实/无写调用、真实管理页面与OpenAPI同源、默认测试/full-core/前端适用门禁和cleanup。当前尚无T45实施/验收结果。

## revision206 — 红灯与Dispatch01B

revision206：T45红灯在73edcbff复现（8例/2预期failure/0error/0skip，源码前后clean）；登记三态事实投影和Dispatch01B。15done/2cancelled/1in_progress/32ready，完整候选attempts0，Goal active。

两条失败是PUBLIC_READ+策略/ACL403被误判MISMATCH，以及PRIVATE+403被误判VERIFIED；其余6项旧合同测试通过。保留red01/manifest.json，不计完整候选失败，不勾AC。

实现合同：公开OssAccessDiagnostic同批改为verification/reason/expectedAccessPolicy/checkedAt及不可变facts；每条事实包含subject、observation(ALLOWED/DENIED/UNKNOWN)、source、scope(OBJECT/BUCKET)、observedAt和固定basis。basis区分文档缺失、不可读、复杂/坏格式、超时、HTTP观察类别；不返回原始policy、bucket/key、URI、凭据、错误文本。subjects表达POLICY_READ/POLICY_WRITE限定文档声明、ACL_LIST桶列表授权、ACL_WRITE_RISK桶写入/ACL修改危险声明、OBJECT_HEAD/OBJECT_GET单对象实测。文档里的Allow不等于实际操作允许；NoSuchBucketPolicy只证明文档缺失，普通404不是同义结论。

bucket ACL READ只表示列对象而非GetObject，WRITE/WRITE_ACP/FULL_CONTROL作为危险声明警告；不因缺Allow或读文档403推断DENIED。明确支持资源/Principal/Action且无Condition/Not*的策略子集才可作限定声明，考虑Deny优先及重叠未知，不能以忽略条件的Allow宣称有效授权。PRIVATE仍有未知时不得声称全桶安全或匿名写已禁止。

匿名HEAD/GET的401/403只证明该对象该次DENIED；404/3xx/5xx/timeout/网络异常UNKNOWN，禁止自动重定向。两步独立保留部分事实，中断保留线程状态并停止后续网络操作；请求超时释放自身资源，不起遗留后台探测。总网络步骤至多5及每步100ms–3s预算保持。

管理POST/权限/正ID/审计禁正文不变；VO在status/reason/checkedAt外增加安全facts，Service私有Evaluation(entry+facts)，registry保留既有summary合同与revision fence。前端通过既有ossConfigs transport解析unknown为域模型，页面按需执行、明确事实来源范围/时间，处理失败、切换和卸载，不用旧响应覆盖当前上下文。正式OpenAPI由Lead捕获生成。

Dispatch01B：cors_audit唯一产品writer，使用Ticket17条写集，迁移全体Java构造/调用方、HTTP合同测试、domain transport与页面、文档；不写治理、不跑构建/服务/提交、不手改生成物。新增写集先回Lead。Lead固定源码后定向green/default/full、真实受限MinIO+HTTP/UI/安全与cleanup、正式OpenAPI、前端/core/静态验收；legacy_audit只读，ops_audit仅私有驱动。

协议依据：[AWS S3 ACL权限表](https://docs.aws.amazon.com/AmazonS3/latest/userguide/acl-overview.html)、[AWS策略显式Deny评估](https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_evaluation-logic_policy-eval-denyallow.html)。只用其约束解释范围，不宣称实现完整云端IAM计算。
