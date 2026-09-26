# Evidence: T-02 — HTTP/操作日志凭据副本修复

- Workspace：`/srv/WTA-plus` / main；单人串行，零子代理、零新worktree。
- Parent before：`76dbbe84a34624234e379661a57b232529e34ed3`。
- Implementation commit / result SHA：null / null。用户明确本 change 所有实现暂不提交，继续可逆工作。
- 当前状态：review。33项定向测试及required真实HTTP/MySQL验证通过；完整测试选集仍有旧Notify DDL的OSS载体门禁错误，归T-22，不能标Done。

## 实现与合同

common-json的`LogSanitizer`由HTTP和操作日志复用，沿用JsonUtils和已有依赖。处理嵌套对象/数组、大小写/下划线别名、自定义排除和嵌套表单名，转换树副本，不修改Map/DTO/JsonNode。普通业务code保留；OAuth code/verifier/redirectUri/state按认证路径处理。重复query/form参数保留值列表，删除原始queryString副本；URL跳转头只记录存在。

凭据签发/授权响应仅保留审计元数据；SSO换票显式`isSaveResponseData=false`。非法/截断JSON、非结构化正文及自由文本错误只输出安全摘要。操作日志先脱敏再限制长度；HTTP不完整前缀不输出原文。业务异常继续原样抛出，错误日志只记录类型与原有定位元数据。数据库入口对副本再次保护，不处置历史数据。

真实HTTP发现匿名请求直接读取空Token会话抛SaTokenException，原日志catch导致整条审计事件丢失。按当前Sa-Token 1.45.0字节码取证后，LogAspect先用既有LoginHelper.isLogin判断；匿名仍发布元数据，登录用户再读取操作者。没有修改权限或会话实现。

## 验证

| 命令/证据 | exit | 实际结果 |
|---|---|---|
| T-02-baseline.json完整选集、精确测试选择器 | 143 | 首次依赖下载到17/44，主动结束并缩小红灯范围；不计测试失败/通过 |
| T-02-web-red.json/log | 1 | 13项中新增OAuth query/redirect用例失败，其余12通过 |
| T-02-operation-red.json/log | 1 | 嵌套access_token进入OperLogEvent，新增1项失败 |
| T-02-focused-green-1.json/log | 0 | common-json/web/log及依赖42项通过，零跳过 |
| T-02-reactor-1.json/log：Ticket第8节完整模块选集、默认test | 1 | 134项中133通过、1失败、0 skipped；NotifyOutboxWakeScopeGateTest错误要求后端独立Git根 |
| T-02-mysql-9968041ca4d8.json/log | 1 | 新fixture registerBean构造器引用重载歧义，admin testCompile失败；容器已清理 |
| T-02-mysql-f0e146304fbc.json/log | 1 | HTTP/响应正确但操作事件0/预期3；实证空Token会话导致匿名审计丢失。另2项admin用例通过；容器已清理 |
| T-02-mysql-890abb54964e.json/log | 0 | 33/33定向测试，0 failed/errors/skipped；44模块编译完成；真实MySQL用例实际执行 |
| git diff --check | 0 | 无空白错误 |

精确可复现入口：`python3 speculo/.speculo/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/run-t02-mysql.py`。该入口串行启动自有MySQL、运行明确测试选择器、保存命令/原始输出并清理容器。固定镜像mysql:8.4.9，digest `sha256:c36050afdca850f23cef85703f84c7531a5ae155a11b5ee1c60acb09937c4084`；仅127.0.0.1随机端口与随机`namewta_log_test_*`数据库。密码是脚本内公开的临时测试夹具值，非运行环境秘密。

真实E2E使用Jetty Servlet、生产SysLogFilter、LogAspect、OperLogEvent、SysOperLogServiceImpl、Mapper和基座原始sys_oper_log DDL。SSO签发UseCase替换为确定性canary，明确不声称完整PKCE/Redis会话旅程（T-06负责）。依次调用换票、普通嵌套正文、含凭据异常三条HTTP请求：响应仍返回合成令牌/原业务字段；6条HTTP事件、3条操作事件及3行MySQL均无canary。断言非零事件/行数，避免“未记录所以安全”的伪通过。失败状态、title、耗时保留，另2项admin单测验证操作人/user/dept及异常对象不变。日志链不使用Redis/OSS，不为本票启动无关服务。

已有HTTP回归同时覆盖SSE、异步完成/超时、二进制、加解密以及业务原字节不变；新增用例覆盖sink/handler异常消息及cause、表单重复值/嵌套秘密、context-path、非法JSON、UTF-8边界。完整选集中的OpenAPI协议14项通过；不把它扩写为T-03预算或真实机器网关E2E已完成。

## 写集与审查

当前14份产品文件精确SHA-256见`T-02-checkpoint.json`，均在T-02写集。没有新增模块/POM/依赖版本，没有反向common-web依赖，没有改变业务HTTP响应或处理历史日志。单writer复核安全副本、事件非空、数据库防线与异常传播；普通文本日志保留减少是ADR-CR-002当前合同，有UTF-8原字节回归，不是删除旧测试。

## 未完成与恢复

完整选集失败责任转T-01：Notify旧门禁同时绑定后端独立Git根、硬编码历史commit和旧change写集，原测试文件本轮未修改；失败保留，没有设置skip或删除测试。下一步先核对原合同再修门禁，重跑完整选集。SSO/Notify原分层问题仍属T-06/T-28。

T-02只达到本地功能检查点；commit/result、完整候选Gate仍未闭合。恢复只针对T-02-checkpoint列出文件，不能重新开启签发正文泄漏。全部测试容器已清理；没有提交、推送、部署或生产数据变更。

## Skill Execution Records

```json
[
  {"id":"engineering-standards","phase":"implement","operation":"apply-scope-contract","sha256":"dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9","status":"passed","evidence":["T-02-checkpoint.json：安全日志副本、common依赖方向和classic/layered边界","T-02-mysql-890abb54964e.json：原字节、事件与真实SQL链"]},
  {"id":"wta-common-modules-guide","phase":"implement","operation":"apply-scope-contract","sha256":"e92775ce47af41bd33c1b3293d7f8a3185fdd739b9b588519e043c654f678d98","status":"passed","evidence":["LogSanitizer复用JsonUtils，common-log/web原有common-json依赖；不新增同义模块","匿名审计使用既有LoginHelper入口，不改变鉴权实现"]},
  {"id":"engineering-standards","phase":"verify","operation":"verify-affected-contract-and-quality-gates","sha256":"dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9","status":"passed","evidence":["两项实际红灯与33项定向绿灯、真实HTTP/MySQL和资源清理","完整选集134项中的Notify门禁失败如实保留，未报告全量通过"]}
]
```

## 最新完整回归（Notify 门禁修复后）

`T-02-reactor-2.json/.log`：44模块选集跑完，exit 1；总计651项，625通过、1错误、25跳过。Notify门禁及模块57项已通过，当前错误为BusinessOssOwnerArchitectureUnitTest发现基座中的旧`sys_notify_log.attachment_oss_ids`无owner。生产Java/XML无此表/列调用，已登记T-22的Notify schema收缩核对，未增加豁免。T-02产品hash不变，33项定向/真实HTTP与MySQL成功证据仍有效；默认选集的外部环境跳过不替代独立真实E2E。整体回归未通过、无提交、无Done。

## 后续验证交接

T-09已移除无生产/DML消费者的两张旧Notify基座表，默认50模块Maven退出0、673项中645通过28跳过，旧OSS载体失败已关闭；本票原失败记录保留。

## Revision134 当前结果（历史结果保留）

HTTP sink、OperLogEvent和真实MySQL canary及普通字段/失败元数据已复验；全量旧Notify错误已关闭。历史日志处置/凭据轮换在本票OUT且未获授权，未执行，也不存在可伪造的批准记录；该外部批准项继续未勾选，不是待执行的本地实现。 当前验收以T-30-extra-services-v3.json, T-30-http-v1.json, T-30-v3-backend-tests.json及T-30-completion-audit-revision134.json为准；本票仍为review，commit/result均null。
