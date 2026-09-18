# 公共模块、安全与资源生命周期审查

> 2026-09-18 已按当前工作树重新核对并修订建议。原报告中的2026-09-14命令属于历史记录；当前命令结果见 [re-review-command-results.json](re-review-command-results.json)，逐票结论见 [re-review.md](re-review.md)。本次单人串行，仅改change；无旧版兼容要求。


审查人：Lead/root。基线：`19620c745bb341b0e19fd5872ce02f01f50f9471`，当前 main 工作树，2026-09-14。全部为审查意见，未实施。

本报告直接深读 Web 日志、操作日志、机器调用入口、Client 来源 IP、Redis 防重和 OSS 客户端等 module。P1 表示高优先级安全/关键功能问题；P2 表示可靠性、协议或维护性问题。`confirmed` 说明源码链成立，不表示已在生产复现。真实 HTTP、Redis、代理环境均未在本轮启动。

## R-01 — 两套日志策略留下可重放凭据副本

- **严重度 / 置信度：** P1 / confirmed（静态完整调用链）。
- **文件与符号：**
  - <Path>backend/wta-common/wta-common-web/src/main/java/org/namewta/common/web/logging/SysLogFilter.java</Path>：`requestEvent` 约 300 行直接写 `request.getQueryString()`；`requestParameters` 虽脱敏，原查询串仍保留。
  - <Path>backend/wta-common/wta-common-web/src/main/java/org/namewta/common/web/logging/SysLogBodySanitizer.java</Path>：`SENSITIVE_FIELD_NAMES` 没有 `code_verifier` 归一化后的 `codeverifier`，也不解析嵌在 `redirectUri` 内的 code。
  - <Path>backend/wta-common/wta-common-log/src/main/java/org/namewta/common/log/aspect/LogAspect.java</Path>：`getControllerMethodDescription` 约 159–160 行序列化完整响应；`excludeParamNames` 只作用于请求。
  - <Path>backend/wta-common/wta-common-log/src/main/java/org/namewta/common/log/annotation/Log.java</Path>：`isSaveResponseData` 默认 true。
  - <Path>backend/wta-modules/wta-sso/src/main/java/org/namewta/sso/controller/anonymous/SsoOAuthController.java</Path>：`token` 约 79 行只排除请求 code/verifier，返回 `SsoTokenVo.access_token`。
  - <Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/service/impl/SysOperLogServiceImpl.java</Path>：`recordOper` 约 44–50 行接收事件，`insertOperlog` 约 106–109 行落表，没有追加响应脱敏。
- **触发与后果：** 正常 SSO 换票成功就会发布包含业务 access_token 的操作日志；`/sso/oauth2/token` 的 JSON code/verifier 还可能被 HTTP 日志采集。任意带敏感查询参数的请求，其参数视图虽隐藏，原 queryString 仍是原值。日志及备份因此成为凭据副本。
- **反证：** HttpOnly/Set-Cookie 已被 HTTP logger 隐藏；普通 JSON access_token 归一化后可被 HTTP sanitizer 隐藏；SSO code 有五分钟 TTL、单次消费和 PKCE。这些保护不覆盖操作日志响应中的已签发业务 token，不能作为本发现的反证。没有声称 code 消费后仍可重放。
- **结构问题：** 同一 secret 规则分别存在于 HTTP sanitizer、四字段的 `SystemConstants.EXCLUDE_PROPERTIES` 和各 Controller 排除数组；interface 要求每个调用者知道所有日志 implementation。
- **代码 judo / 删除复杂性：** 建立一个按数据上下文决定日志可见性的内部 module，让 HTTP 与操作日志两个 adapter 使用同一份凭据规则；删除原 queryString、未经审计的响应序列化和散落的默认响应保留。正常关联、状态、耗时、操作者、非敏感结果保留。
- **逐项改造：**
  1. 先令换票及凭据签发响应不记录正文，补足所有相同签发调用点清单。
  2. query 只保留已解析、归一化、脱敏的视图；无法可靠解析时隐藏整体，不同时输出原串。
  3. OAuth 路由按上下文隐藏 code/verifier/含 code 的 redirectUri；普通业务字段名 `code` 不能被全局盲目删除。
  4. JSON、form、查询、请求/响应头分别定义策略；测试 `Token`、Location 中凭据等旁路，并区分目前已确认路径与待证场景。
  5. 日志策略只操作副本；不改签名原始字节、业务响应或认证结果。
  6. 发布后对历史日志制定保留/销毁方案并轮换受影响凭据；历史数据处理必须由用户另行批准，本次不执行。
- **删除测试：** 删除第二套规则后，调用者不再逐个修补字段；规则与负向样本集中，复杂性确实减少。不能用完全关闭审计代替修复。
- **Before → After：** `Controller 排除数组 + HTTP 黑名单 + 原始响应` → `一个日志策略 module → 两个日志 adapter → 已脱敏持久化`。
- **收益：** locality 集中敏感规则；leverage 覆盖所有日志消费者；depth 体现在调用者无需了解字段遍历和媒体差异。
- **dependency class / strength：** in-process / Strong。
- **ADR：** <Path>{roots.state}/specdev/adr/0025-http-runtime-log-credential-redaction.md</Path> 已禁止原样凭据；当前是实现违背已接受合同，无需重新批准泄漏行为。扩展到操作日志的默认保留策略在本 change 提出明确方案。
- **验收：** 用唯一 canary 值完成换票、失败换票、查询 token、form 请求、非法/截断 JSON；捕获 HTTP sink、OperLogEvent 和最终持久化，任一路径不得包含原 canary；真实业务响应仍含应返回值。验证响应增强、流式响应、签名请求字节不变。
- **访谈状态 / 用户结论：** unselected / 待用户审核。

## R-02 — 认证前无界读体，日志上限并非内存上限

- **严重度 / 置信度：** P1 / confirmed（无界读取和顺序）；容量影响 needs-runtime。
- **文件与符号：**
  - <Path>backend/wta-common/wta-common-web/src/main/java/org/namewta/common/web/filter/RepeatedlyRequestWrapper.java</Path>：构造约 47 行 `IoUtil.readBytes` 读完整请求。
  - <Path>backend/wta-common/wta-common-web/src/main/java/org/namewta/common/web/filter/RepeatableFilter.java</Path>：JSON 请求无大小限制地包装。
  - <Path>backend/wta-common/wta-common-web/src/main/java/org/namewta/common/web/logging/SysLogFilter.java</Path>：`prepareRequest` 也主动读完整可记录正文。
  - <Path>backend/wta-common/wta-common-web/src/main/java/org/namewta/common/web/config/SysLogConfig.java</Path>：日志过滤器为最高优先级 +2，早于 MVC 登录校验。
  - <Path>backend/wta-common/wta-common-openapi/src/main/java/org/namewta/common/openapi/gateway/ReplayableOpenApiRequest.java</Path>：约 25 行 `readAllBytes`，`body()` 再复制完整 byte[]。
  - <Path>backend/wta-common/wta-common-openapi/src/main/java/org/namewta/common/openapi/gateway/OpenApiGatewayFilter.java</Path>：`doFilterInternal` 先创建包装器，后做 operation、签名认证及 nonce/rate 限制。
  - <Path>backend/wta-common/wta-common-web/src/main/java/org/namewta/common/web/config/SysLogProperties.java</Path>：1 MiB 仅限制输出前缀；<Path>release-artifacts/docker/frontend/nginx/apps/nginx-admin-web.conf.template</Path> 等入口允许 100m 正文。
- **触发与后果：** 大 JSON、chunked 请求或只携带五个伪签名头的请求，可在身份验证前占用完整正文内存；经过日志和签名路径时还可能出现多份缓存。并发请求会放大堆、GC 与线程占用。未声称已经测出 OOM 阈值。
- **反证：** multipart 的 20MB 限制不是普通 JSON 的界限；日志 `maxBodySize` 只控制前缀；Nginx 100m 是边缘单请求约束，不能替代 JVM 或直连入口的预算。
- **结构问题：** 普通过滤器已复用RepeatedlyRequestWrapper；OpenAPI另有replay缓存与OpenApiRequest防御性复制，解密/XSS还拥有变换视图，调用方承担一致性、容量和失败语义。
- **代码 judo / 删除复杂性：** 复用现有common中的有界读取和原始正文缓存能力 拥有原始字节及超限行为，日志只观察受限前缀、签名 adapter 复用已采集字节；删掉无界读体与不必要整数组复制。
- **逐项改造：**
  1. 明确普通 JSON、机器调用、上传各自预算（建议起点 JSON/机器正文 2 MiB，须结合最大真实业务请求裁决），日志前缀预算独立。
  2. Content-Length 提前拒绝，同时对未知长度/chunked 累计计数，不能只相信 header。
  3. 在读取前完成廉价的协议头格式、路由资格检查；读到超过预算立即以稳定413失败，不能静默截断或被SysLogFilter.prepareRequest的catch吞掉后进入业务。
  4. 保持验签使用收到的精确字节；JSON 解析/XSS 处理与日志副本不得改变签名输入。
  5. 明确同步/异步 dispatcher 的 owner、超时、取消和内存释放；现有 SSE/上传不能为了复用而被整流缓冲。
- **删除测试：** 替换两个无界缓存 owner 后，容量与复制问题消失在一个 seam 内；若只把 `readAllBytes` 挪到 helper，候选不算完成。
- **Before → After：** `重复整流缓存 → 登录/签名 → 前缀日志` → `受限入口采集 → 验签/业务 → 有界观察`。
- **收益：** locality 在一个读取/失败点；leverage 同时覆盖浏览器及机器请求；depth 隐藏计数、复用和释放逻辑。
- **dependency class / strength：** local-substitutable / Strong。
- **ADR：** <Path>{roots.state}/specdev/adr/0034-versioned-openapi-hmac-ingress-security.md</Path> 的签名语义必须保持。正文上限是新增外部合同，需审核，但不是撤销签名协议。
- **验收：** 阈值前/等于/超一字节、缺 Content-Length、伪造长度、慢请求、无效签名、正常二进制上传与 SSE；验证 413、业务未执行、重复缓存不会随正文再翻倍、签名测试仍通过。容量压测阈值在隔离环境测量。
- **访谈状态 / 用户结论：** unselected / 待用户审核。

## R-03 — 来源 IP 缺少可信代理约束，白名单采用可转发输入

- **严重度 / 置信度：** P1 / likely（静态信任链可见；需要代理环境复现最终授权效果）。
- **文件与符号：**
  - <Path>backend/wta-common/wta-common-core/src/main/java/org/namewta/common/core/utils/ServletUtils.java</Path>：`getClientIP` 约 291–298 行优先检查 X-Forwarded-For 等多个 header，并委托 Hutool，无远端代理白名单判断。
  - <Path>backend/wta-common/wta-common-security/src/main/java/org/namewta/common/security/config/SecurityConfig.java</Path>：`validateClientAccessRules` 使用上述 IP 匹配 Client whitelist。
  - <Path>release-artifacts/docker/frontend/nginx/lb/nginx-lb-http.conf.template</Path> 与 <Path>release-artifacts/docker/frontend/nginx/lb/nginx-lb-tls.conf.template</Path>：多处使用 `$proxy_add_x_forwarded_for` 保留来路提供的链。
  - <Path>release-artifacts/docker/frontend/nginx/apps/nginx-admin-web.conf.template</Path> 和 <Path>release-artifacts/docker/frontend/nginx/apps/nginx-home-web.conf.template</Path>：继续追加来源链。
- **触发与后果：** 客户端自带 X-Forwarded-For 经入口继续传递，应用的权限输入不是经过可信 hop 解析的唯一来源；可能影响 IP 白名单、限流和审计。缺少代理拓扑与 Hutool 实际版本行为验证，所以不宣称已成功越权。
- **反证：** Compose 默认把后端映射到 127.0.0.1，降低直连暴露；X-Real-IP 虽重写，但工具首先读取 X-Forwarded-For。外部网关若清洗该头可能缓解，当前模板未证明。
- **结构问题 / 代码 judo：** 多个调用点各自读取传输 header；让一个来源解析 module 拥有可信代理配置与链解释，边缘覆盖未可信输入，内部只处理已确认 hop。删除任意 header fallback。
- **具体修改：** 确定公网入口和内部代理 CIDR；只在 `remoteAddr` 为可信代理时解析链；从可信端逐 hop 剥离而非任取首项；直连只用 socket peer；日志同时保留受控的 peer/proxy/client 三种概念，避免误称同一个 IP。
- **删除测试：** 删掉 header 优先的通用工具逻辑后，白名单与限流共享唯一可信结果，复杂性不再散落。
- **Before → After：** `任意 header → 每个安全调用点` → `peer + 明确代理配置 → 唯一来源解析 → 安全调用点`。
- **dependency class / strength：** ports & adapters / Strong（信任合同整改）；网络利用细节待验证。
- **ADR：** 执行既有 Client 隔离与安全合同；代理拓扑和生产 CIDR 是部署决定，需用户审核后确定，不在本次接管环境。
- **验收：** 直连伪造 XFF、经单/双代理伪造首项、IPv4/IPv6、多个 header、空/非法值及真实白名单；同时验证正常多级代理不误封。部署验证须走原环境 Skill。
- **访谈状态 / 用户结论：** unselected / 待用户审核。

## R-04 — 防重复提交键没有请求所有权，过期后的旧请求可删新键

- **严重度 / 置信度：** P2 / confirmed（源码与并发时序）；真实 Redis 复现未运行。
- **文件与符号：** <Path>backend/wta-common/wta-common-redis/src/main/java/org/namewta/common/redis/aspectj/RepeatSubmitAspect.java</Path>：`doBefore` 约 76 行写固定空值，`KEY_CACHE` 只保存 key；`deleteRepeatKey` 约 133 行直接删除。
- **触发时序：** A 获得键且执行超过防重 TTL → 键过期 → B 获得同名键 → A 失败/返回失败结果 → A 删除 B 所拥有的键 → C 在 B 防重窗口内再次进入。
- **后果：** 注解承诺的防重窗口失效，调用方不得把它当 exactly-once；是否形成业务重复还取决于数据库唯一约束和用例幂等，不能自动推断所有操作都重复落库。
- **反证：** `finally KEY_CACHE.remove()` 已清理线程上下文，但 Redis key 仍没有所有权标识；成功保留键直到 TTL 并不能保护旧失败请求。
- **结构问题 / 代码 judo：** 请求只持有字符串 key，缺少 lease identity；将 token 与键生命周期合为内部 module，获取写随机 owner token，失败释放用 Redis 原子 compare-and-delete，删除无条件释放分支；不新增租约框架或自动续期。
- **具体修改：**
  1. 仅释放当前请求真正取得的 lease；没有取得 key 的请求不能删除任何 key。
  2. TTL 过期不拥有后继请求的 lease；现有注解消费者以HTTP入口为主；优先用around局部上下文持有本次owner，不新增没有消费者的嵌套/异步支持。
  3. 成功保留 TTL 的现有业务语义是否保留写入合同；防重和持久幂等明确分开，不引入全局重型事务框架。
  4. 复用 RedisUtils 所属模块内的 Redis 原子能力，不让业务层绕过公共入口。
- **删除测试：** 删除无 owner 的 delete 后，释放正确性集中到一个 seam；没有把防重知识推给每个 Controller。
- **Before → After：** `SETNX(key, 空值) → 任意旧调用 DEL(key)` → `SETNX(key, owner) → 原子比较 owner 再 DEL`。
- **收益：** locality 集中获取/释放；leverage 覆盖所有 @RepeatSubmit；depth 隐藏过期竞态。
- **dependency class / strength：** local-substitutable / Strong。
- **ADR：** 无已知冲突；不能承诺强业务幂等作为顺手升级。
- **验收：** 控制时钟与 Redis 场景复现 A/B/C 时序；A 失败不得删除 B；正常失败立即允许合法重试；正常成功在 TTL 内被拒；嵌套、异常和线程复用无泄漏。真实 Redis 原子性验证单独记录。
- **访谈状态 / 用户结论：** unselected / 待用户审核。

## 保留项与拆分压力

| 范围 | 本轮证据与裁决 |
|---|---|
| OpenAPI 签名/nonce/session | 已读取签名入口、nonce/rate fail-closed、会话 bridge 注册与操作 registry；保留独立机器身份、浏览器凭据混用拒绝、nonce/鉴权规则，不因体积增加而删除这些保护。未做全协议模糊测试。 |
| OSS | `AbstractOssClientImpl` 1932 行，`OssClient` 811 行，确有 decomposition pressure；其 presign/multipart/bucket access/stream 生命周期具有真实职责。不能只因行数而删掉能力。先用调用清单确认无消费者 overload，再评估内部提取诊断/签名实现；本轮不把机械拆类升级为强候选。 |
| QueryBuilder/ExcelBuilder | 1392/1110 行含大量公开重载与 Javadoc。保留 BaseMapperPlus、MyBatis 查询入口、ExcelBuilder 合同。未证明等价且更小的替代，不建议另引 ORM 或导出框架。 |
| 公共认证 | SaToken 机器身份的 server-only attribute 保护与 Client 身份不能合并成无 clientPk 的普通用户 fallback。 |
| CORS | 默认 credentials + `*` origin pattern 值得与 SSO Cookie 联合验证。SameSite=Lax 限制跨站 Cookie，是否可被同站子域利用依赖实际域名，不在缺运行证据时写成已复现跨站盗号。纳入 SSO 安全票验收。 |
| 第三方模块 | AI/job/ES/MQTT 等 common 包的少量配置类可能是合法 adapter；不能以 thin wrapper 一词代替是否有真实配置生命周期的判断。 |

完整文件规模、POM 与前端包清单位于 <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/repository-inventory.json</Path>、<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/maven-module-inventory.json</Path> 和 <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/frontend-package-inventory.json</Path>。这些是结构清单，不是逐行审查完成证明。
