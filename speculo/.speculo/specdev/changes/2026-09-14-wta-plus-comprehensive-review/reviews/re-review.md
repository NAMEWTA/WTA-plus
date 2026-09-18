# 当前工作树复核（2026-09-18）

基线HEAD：`e285d0800c530fcb6b90790a8aab5dcc3162d3bd`。单人串行，无子代理。核对31张票据、四份报告及源码/POM/SQL/前端配置/发布脚本；这不是全仓逐行审计，也不代表生产复现。

`confirmed`表示源码链或本次命令可证；`likely/needs-runtime`保留真实运行验证；工程债不按安全故障定级。详细源码路径继续由四份专项报告和票据持有。

| 票据 | Findings | 结论 | 当前证据与最小方案 |
|---|---|---|---|
| [T-01](../ticket/01-restore-trustworthy-gates.md) | D-01/D-07/D-11/B-09 | confirmed | 缺失CI、旧cwd、temp/release硬前置、检查器旧namespace/注释误报均仍存在；发布测试另有Speculo所有权误判。 |
| [T-02](../ticket/02-unify-log-redaction.md) | R-01 | confirmed | SysLogFilter写原query；LogAspect序列化完整响应，SsoOAuthController.token未关闭响应正文。复用common-json脱敏，不新建模块。 |
| [T-03](../ticket/03-bound-request-capture.md) | R-02 | confirmed | RepeatedlyRequestWrapper、ReplayableOpenApiRequest及解密/XSS读体无硬限；普通两filter已复用wrapper。新增413不能被日志catch吞掉。 |
| [T-04](../ticket/04-trusted-client-address.md) | R-03 | likely | ServletUtils信任请求头；Nginx保留外来XFF链。缺陷链存在，实际白名单绕过需代理环境验证。 |
| [T-05](../ticket/05-repeat-submit-lease.md) | R-04 | confirmed | RepeatSubmitAspect写空值且失败直接DEL；A过期后可删B的防重键。局部owner+原子比较删除即可。 |
| [T-06](../ticket/06-secure-sso-session.md) | B-01/B-03 | confirmed / runtime-qualified | 授权码ThreadLocalRandom、Redis session雪花ID；Cookie缺Secure已确认。生产TLS/Cookie效果未运行。 |
| [T-07](../ticket/07-sso-callback-journey.md) | B-02/F-09 | confirmed | appendQuery未编码，authorize还trim state；两App回调固定origin根且成功跳固定页。补精确state往返与App内returnTo。 |
| [T-08](../ticket/08-complete-sso-release.md) | D-02 | confirmed / needs-runtime | 三个App被release扫描构建，Compose仅两App且SSO Nginx模板缺失。发布资产补齐，真实Origin/TLS待验。 |
| [T-09](../ticket/09-coherent-build-matrix.md) | D-04/D-08/D-09 | confirmed / needs-runtime | Home/SSO的dev被production覆盖；JAR必需断言漏Notify/Profile；core仍用skipTests；MinIO镜像不同。 |
| [T-10](../ticket/10-atomic-release-provenance.md) | D-03/D-10 | confirmed | partial继承旧current，manifest已有digest但整体HEAD/bundle失真；stage原地分步写。原子范围是产物，不是运行容器。 |
| [T-11](../ticket/11-remove-browser-shared-private-key.md) | F-01 | confirmed static | crypto-browser存在共享响应privateKey和AES-ECB注入路径；未构建生产包，不断言实密钥已发布。HTTPS硬切即可。 |
| [T-12](../ticket/12-session-navigation-lifecycle.md) | F-04/F-05 | confirmed static / needs-runtime | Admin logout成功后才清token；Home已有finally但上层导航会被异常打断；roles空值哨兵及动态route缺回收仍存在。 |
| [T-13](../ticket/13-recoverable-registration.md) | F-06 | confirmed | RegisterPage只prepare一次，无验证码刷新；后端验证时消费uuid。registerEnabled已在domain存在，页面未呈现。 |
| [T-14](../ticket/14-profile-self-materials.md) | F-02 | confirmed | self/runtime无上传能力；两页仅save/submit；后端validateRequired及当前DML必填规则存在。范围是新建且未补材料的申请。 |
| [T-15](../ticket/15-contract-profile-legacy-bridges.md) | B-08 | confirmed | PersonRebindUseCase等旧default抛错/新签名转接；Service保留过渡构造。删除旧桥，保留Clock/port等真实测试入口。 |
| [T-16](../ticket/16-workflow-task-integrity.md) | F-03/B-10 | confirmed / likely | 弹窗未清旧task且确认闭包读可变task；两节点API缺显式读权限检查。后端已有Lock4j，不新增taskVersion协议。 |
| [T-17](../ticket/17-trusted-designer-messages.md) | F-10 | confirmed | DesignPage只传event.data，designer只判method=close。精确origin/source/shape足够，不扩展供应商nonce协议。 |
| [T-18](../ticket/18-upload-ownership-lifecycle.md) | F-07/F-11 | confirmed | 用户导入缺error复位；下载URL失败创建Blob URL但无revoke。未证实引用误删；优先删除隐式预览fallback。 |
| [T-19](../ticket/19-system-page-state-locality.md) | F-08 | confirmed static | User/Role/Menu直接应用任意完成顺序的列表响应。用筛选乱序复现，不能将未证明的热切Client写成跨租户泄漏。 |
| [T-20](../ticket/20-strict-contract-target.md) | F-12 | confirmed engineering debt | 三个strict开关关闭及边界cast存在；没有诊断规模或线上故障证据。收紧触及边界，避免全仓类型重写绑架修复。 |
| [T-21](../ticket/21-accessible-public-apps.md) | F-13 | confirmed static / needs-runtime | Home未声明共享LoginPage所需主题变量，SSO status无live region。既有label/alert保留，视觉合格性需浏览器。 |
| [T-22](../ticket/22-atomic-notify-result.md) | B-04 | confirmed | dispatch裸写Delivery/Attempt/Outbox/Intent；renew/finish只比owner/token未检查过期。短事务+fence，不新增同义幂等key。 |
| [T-23](../ticket/23-durable-provider-callback.md) | B-05 | confirmed | seenEvents在事务完成前入内存且eventId无provider空间。最小持久receipt与状态同事务；不默认新建callback inbox。 |
| [T-24](../ticket/24-recoverable-third-resilience.md) | B-06/B-07 | likely / needs-runtime | RSemaphore仅finally归还，trySetRate/trySetPermits未更新已存在key。优先现有expirable permit与稳定key更新，实测回收。 |
| [T-25](../ticket/25-acyclic-department-moves.md) | B-12 | confirmed | updateDept仅处理ancestors，无后代拒绝；已有Spring事务。改DSTransactional并同树串行校验，不另建树框架。 |
| [T-26](../ticket/26-cut-over-crud-contracts.md) | B-11 | confirmed contract debt | 重新枚举70个旧method注解/27文件；原59项漏11个无括号@PutMapping，非全部安全漏洞；另需审只读POST/有副作用GET，不按注解机械替换。 |
| [T-27](../ticket/27-honest-demo-tree-baseline.md) | B-14 | confirmed | TestTreeServiceImpl保存与删除校验仍TODO。补最小树规则，保留真实示例，不凭依赖数量拆Demo。 |
| [T-28](../ticket/28-bounded-notify-wake.md) | B-13 | likely | after-commit发本地事件，Worker监听直接drain/dispatch；默认线程效果待运行。删重复本地wake，保留Redis+poll。 |
| [T-29](../ticket/29-converge-current-documentation.md) | D-05/D-06/B-17 | confirmed | 50 POM/247后端Java测试源/3 App与画像不符；49 AGENTS中41通用索引；Third缺登记。旧清单SHA不能直接用于当前删除。 |
| [T-30](../ticket/30-integrated-upgrade-acceptance.md) | 整体验收 | planned | 本轮不进行产品实现或全仓构建；此票只验收未来同一候选版本的全部已实施合同。 |
| [T-31](../ticket/31-enterprise-transfer-queued-contract.md) | B-15 | confirmed | EnterpriseTransfer只接受ACCEPTED/DELIVERED，真实submit新通知为QUEUED，事务内即抛错。复用公开query确认投递，免新投影事件链。 |

## 设计收敛

- 无旧版兼容：同步修改仓内调用、测试和生成物后删除旧入口，不增加兼容adapter、双路由、版本窗口或外部消费者等待。运行数据安全和供应商协议仍须遵守。
- state必须原样往返；短事务必须在真实代理调用处；租约核验同时覆盖owner/token/有效期。文件哈希不能证明继承产物的构建来源。
- 复用现有Notify query处理企业转移投递状态，不预设事件总线或Redis投影Outbox；复用工作流现有锁与权限，不凭前端竞态新增版本协议。
- 不新增iframe nonce握手、上传通用dispose接口、发布多容器原子切换或全仓strict硬门槛。需要这些能力时必须有独立问题证据。
- 模式登记与SQL owner属于硬约束；当前SQL为10-cde-base-ddl.sql与50-cde-base-dml.sql，旧50/60-namewta路径不再进入写集。

## 当前检查

9条命令均串行执行，完整stdout/stderr/退出码见 [命令记录](re-review-command-results.json)。全栈Skill、模板和person/enterprise/third分层检查通过；facts、notify/sso分层、release检查非零。release为43项、41通过、2失败，不能沿用旧报告的42/43。

现存非零结果纳入T-01/T-06/T-28/T-29；本轮不修改产品使其变绿。Maven、pnpm、浏览器、真实MySQL/Redis/MinIO/Provider、生产Nginx及远程CI未执行。

## 保留与边界

保留PKCE/精确redirect/单次consume、Client隔离、Outbox与OSS引用保护、classic/layered边界、供应商schema及许可证。B-16仅凭Demo broad POM删除依赖的候选继续拒绝。

早期evidence与command JSON保留为2026-09-14历史快照，不覆盖原运行证据；当前事实与change外哈希使用re-review前缀文件；当前CRUD明细见../evidence/re-review-inventory.json。旧清单中的路径仅供溯源，实施写集以修订后的票据为准。
