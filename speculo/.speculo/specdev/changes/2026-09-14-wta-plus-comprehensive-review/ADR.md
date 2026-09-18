# ADR：基座修复方案

2026-09-18复核。用户已确定不考虑旧版兼容；按本次T/P自主完善请求锁定局部计划设计，产品实施与发布尚未发生。ADR-CR-002的正文预算和ADR-CR-009的callback事件身份/保留策略保持Pending；其余为本change的计划合同，不代表永久ADR已更新。

## ADR-CR-001：SSO随机源与浏览器合同

以JDK SecureRandom生成32字节opaque code/session，保留数据库主键、现有存储、TTL、PKCE与单次consume。state作为opaque值原样编码往返。Cookie生产Secure/HttpOnly，默认host-only、Path=/、Lax；独立SSO Origin通过同源/sso反代服务，不推导出共享父域或SameSite=None需求。

不新增随机数框架、哈希存储迁移或碰撞证明测试。T-06/T-07/T-08负责源码、协议与独立发布验收。

## ADR-CR-002：日志副本与正文边界

HTTP/操作日志在已有common-json中复用最小脱敏能力；签发接口关闭响应正文记录，保留审计元数据。日志副本不得改动业务响应或验签输入。

原始请求字节由有硬预算的采集路径拥有，JSON/机器正文和日志前缀分开计限。普通两filter已有wrapper复用；OpenAPI/解密/XSS边界分别核对，超限必须413，日志catch不能吞掉。复用既有common，不强制新Maven模块。T-02/T-03验收。

## ADR-CR-003：可信来源与防重所有权

公网入口清洗外来转发头；应用仅对显式可信peer解析代理链，直连使用peer。白名单/限流/审计复用同一结果，生产CIDR待实际拓扑确定。

防重键值保存本次随机owner，失败用原子compare-delete，成功保留原TTL语义；不扩展为业务exactly-once或通用租约服务。T-04/T-05验收。

## ADR-CR-004：删除浏览器共享私钥/ECB

浏览器传输使用HTTPS，同步删除Admin/Home共享响应私钥、ECB包装及无消费者配置/依赖。同步后端相关ApiEncrypt端点，保留机器HMAC、OSS签名和数据库加密。

不增加AEAD备选、Cookie认证迁移或双协议兼容；实际生产密钥是否进入bundle需构建验证。T-11验收。

## ADR-CR-005：完整同源发布产物

每个App每种模式只构建一次；App发布名单显式且同时校验Compose/Nginx资产。预期bundle组合由产品合同规定，不能由待测JAR自生成。

发布脚本现有manifest保留每文件SHA-256，补准确单一monorepo源码来源；完整构建形成不可变版本目录并校验全部资产后切换一个指针。局部构建不晋升为可部署current。消费者固定版本路径，运行容器在部署阶段显式重建；不承诺多容器原子热切换，不无故增加Windows适配。

镜像以现有Compose为来源，CI读取或校验一致性，不另建配置生成系统。T-08/T-09/T-10验收。

## ADR-CR-006：事实与门禁准确

修复旧namespace和注释误报，真实分层命中在所属模块修复。不存在CI不能写active；私密输出目录不作为clone前置。发布测试不因Speculo供应商Skill存在而删除供应商资产，应按所有权界定检查范围。

事实来自源码/POM/package/发布资产；短README负责导航，必要模块规则保留，不边删除文档边生成同义副本。T-01/T-29验收。

## ADR-CR-007：局部异步状态与资源

页面query/task由generation约束响应、错误和loading；提交捕获当前taskId与payload，等待确认后再次检查。会话退出finally清本地身份和动态route；空roles不代表未初始化。

复用已有工作流锁和权限检查；不预设taskVersion或全链AbortSignal。iframe使用精确origin/source/close shape，不新增nonce协议。上传成功与预览失败分开，优先删除无人持有的Blob URL fallback；self材料依赖真实owner完成引用，不复制管理页面。类型收紧聚焦触及边界，不作为全仓重写理由。T-12—T-21验收。

## ADR-CR-008：直接切换基座合同

用户已排除兼容要求。实现时同步仓内调用方、测试与生成物，删除旧Java重载、default桥和旧CRUD method，不引入兼容adapter、版本窗口、双路由或外部消费者审批。

删除范围仍以真实调用与职责为证：保留Clock/port等有用测试接缝、第三方协议与现有安全边界。不因无兼容要求重写全部classic模块或删除重要数据。新环境由唯一六文件SQL基座重建；真实运行环境的数据处理和发布仍需另行授权。T-15/T-26/T-27验收。

## ADR-CR-009：通知短事务与可恢复排队

Provider I/O在事务外，Delivery/Attempt/Outbox/Intent在短DSTransactional内提交；锁后核验owner/token/状态/有效期，失效结果零写入，finish失败回滚。callback receipt使用明确供应商命名空间+eventId唯一身份，与状态推进和聚合同事务；不默认新增callback inbox。

Third优先复用Redisson可过期permit，TTL匹配有界调用预算，稳定key更新限额；不按配置版本复制完整额度。Notify本地重复wake优先删除，Redis+poll恢复，不新建executor。

Profile转移接受QUEUED，将transfer和通知关联同事务保存；状态查询/confirm经Notify公开query核验ACCEPTED/DELIVERED后激活现有Redis挑战。未提交transfer不能确认，过期不续期；失败或跨存储部分失败允许重新发码恢复。不增加Profile投影Outbox、事件总线或持久验证码副本，不承诺外部exactly-once。T-22/T-23/T-24/T-28/T-31验收。

## 2026-09-18 T/P 规划补充

- T-01明确生成仓内CI候选；远程启用和required checks另行授权。
- T-03：2 MiB仍是测量起点。缺合法样本/内存预算时不能把它改写成默认生产限制。
- T-04：可信CIDR默认空集合，只信socket peer；算法在隔离单/双代理验收。真实入口CIDR和清洗配置在发布Gate核验。
- T-23：当前回调参数不足以证明供应商账号/eventId全局唯一；先固定adapter身份及重试窗口，再锁定receipt复合唯一键和保留策略。未匹配providerMessageId不能提前消费receipt。
- T-25：同树结构变更使用共同事务锁域；跨根按稳定根ID顺序锁两树，新增/删除/移动关系入口共用，锁后重读并检查环。保留classic结构，不新增树框架。
- T-27保留主动Demo，带子节点拒删，不新增名称唯一规则。T-28删除本地重复wake，不再保留无测量依据的executor备选。
- 用户排除兼容要求，替代当前change范围内EX-002桥接保留理由及默认expand-contract指导；永久ADR/Skill仅记录未来责任票，不在本轮修改。
