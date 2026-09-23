# T-40 Dispatch02 — 缺请求头登录故障的窄修恢复包

## 1. 前批结论与根因

前批C1 2c8047a2、C2 851a6d6e、C3 d5b4f5f8三次完整候选均未验收；原始失败保留在complete-candidate-c1/c2/c3，不删除或追认。C1旧公告夹具已修；C2默认OOM不计通过，有界重跑877pass/216skip及full/core/静态通过；C2/C3 browser前置登录失败，尚未进入Chrome。C3 run630336ccabc8052b确认backend探针200、login_control HTTP200/R500、backend仍存活；source/JAR前后同值、cleanup[]。

固定full JAR使用Hutool5.8.47，源码和实际无服务Java探针均证实null/blank UA解析返回null；三个真实消费者无保护解引用，构成确定的代码缺陷。C3安全记录未保存服务端异常类型，因此不将唯一服务器根因冒称已由HTTP栈证明；下一轮回归将证伪/验证此链。T41 Chrome正常UA与本driver未发UA的差异已核实。

## 2. 独立审查与影响边界

legacy_audit独立核对LoginHelper（令牌前）、UserLoginSuccessListener（同步Sa-Token事件）、SysLoginInfoServiceImpl（异步审计）三条路径，支持窄修。common导航和System classic/应用组装例外已核，公共Java签名、HTTP schema、认证/Client/角色/权限/Token/事件顺序、六SQL不变；不新增同义公共工具，不迁移模块，不扩大OIDC change。UserAgent仅影响browser/os元数据，不参与授权。

## 3. 实质变更与受限派单

cors_audit唯一产品writer，Lead独占治理/提交/所有测试构建和owned服务。现有23写集加以下4精确路径（事前登记）：

- `backend/wta-common/wta-common-satoken/src/main/java/org/namewta/common/satoken/utils/LoginHelper.java`
- `backend/wta-admin/src/main/java/org/namewta/web/listener/UserLoginSuccessListener.java`
- `backend/wta-modules/wta-system/src/main/java/org/namewta/system/service/impl/SysLoginInfoServiceImpl.java`
- `backend/wta-admin/src/test/java/org/namewta/test/auth/LoginUserAgentUnitTest.java`

Dispatch02A先只交新LoginUserAgentUnitTest，走三个真实消费者（LoginHelper的public login，外部StpUtil/Redis/mapper可mock），覆盖无头、空白、正常UA、已有字段不覆盖及监听器的在线/审计写入；不反射private方法代替真实消费者。Lead固定test-only源，运行目标类产生可观察NPE红灯，不将此预声明诊断作为完整恢复候选。

红灯保留后Dispatch02B仅三处缺失browser/os回退Unknown，保留非空UA分类、现有字段和全部原认证/事件/存储行为。现有owned浏览器runner继续不发UA登录，补充当前token可用、本人在线DTO及精确本次异步登录audit Unknown取证，以及固定Chrome UA正控制；结构化结果只保留固定分类、计数和布尔值，不保存token/口令/响应正文。可复用GET /monitor/online的本人分页列表与owned MySQL，禁止新增第二套容器/完整Spring启动夹具或用补UA绕过。若需其他路径先登记。

## 4. 恢复入口、门禁与停止条件

本包和独立意见已落盘后，恢复批完整候选attempts从0计，前批3次永久保留；只在完整源码返回并固定clean HEAD/tree后计恢复候选。红→绿目标单测、默认全后端、full/core与静态；新common登录路径变化使先前后端实测不能冒称同输入，重新跑真实18和共享135（服务严格串行，fresh XML在clean前保留）。前端业务/TS/E2E spec若未改可按C1明确输入等价复用，修改Python需离线安全回归；重新构建exact-source full JAR后运行两真实Chrome及UA登录/在线/审计对照，zero-skip、source/JAR同值和cleanup[]才可验收。

Maven使用本轮局部JAVA_TOOL_OPTIONS=-Xms128m -Xmx1536m防止再次host OOM，不改全局环境、不停止其他服务、不排除测试。若UA修复后仍HTTP500，按新阶段与固定异常类型/源码位置调查另一根因，不忽略错误；若恢复批再到3次，先保留证据并重新复盘，不盲重试。Goal持续active；不推送、部署、生产数据修复或归档。
