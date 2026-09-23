# T40 控制登录缺失 User-Agent：窄修只读审查

审查源码点：`d5b4f5f83e43afdc30ad30b3577a1531b030fc11`（通过 `git show` 固定读取；当前工作树可能继续变化）。结论：**三处均需防空**。这是合法 HTTP 客户端未发送 `User-Agent` 时的登录附属设备描述故障，不是认证、Client 或权限规则问题。本文仅为三次失败后的修复输入；未修改仓库、未运行构建或服务，也不把修复视为已验证。

## 确认的根因与调用链

- `backend/pom.xml:49` 固定 Hutool `5.8.47`。本地该版本 `hutool-http-5.8.47-sources.jar` 的 `UserAgentUtil.java:17-19` 直接调用 `UserAgentParser.parse`，后者 `:19-22` 在 `StrUtil.isBlank(userAgentString)` 时返回 `null`。已知浏览器 Chrome 自动带 UA，而本轮合法 `http.client` 控制登录不带 UA；空白 UA 同样命中。非空但无法识别的 UA 则由解析器返回 `Browser.Unknown`/`OS.Unknown`，不属于同一故障。
- `backend/wta-common/wta-common-satoken/src/main/java/org/namewta/common/satoken/utils/LoginHelper.java:53-65,73-95`：`fillRequestContext` 先于 `StpUtil.login`；第 85 行解析请求头，第 87/90 行在登录用户 browser/os 尚空时直接解引用。缺失 UA 会在令牌创建前中断。已有 browser/os 非空时应保持原值；IP、位置与 `model.deviceType` 原逻辑应保留。
- `backend/wta-admin/src/main/java/org/namewta/web/listener/UserActionListener.java:23-25` 在 Sa-Token `doLogin` 中发布 `UserLoginSuccessEvent`；`backend/wta-admin/src/main/java/org/namewta/web/listener/UserLoginSuccessListener.java:39-66` 是无 `@Async` 的 `@EventListener`，第 42 行解析 UA，第 50/51 行直接解引用。修第一处后仍可能在此同步回调中抛错，导致客户端看到失败响应或不完整的在线态。第 53-66 行的 token/Client/device/Redis、登录日志、最近登录更新与现有事件时序均应原样保留。
- `backend/wta-admin/src/main/java/org/namewta/web/service/SysLoginService.java:133-145` 与 `SysRegisterService.java:134-145` 把原请求 UA（可为 null）装进 `LoginInfoEvent`；`backend/wta-modules/wta-system/src/main/java/org/namewta/system/service/impl/SysLoginInfoServiceImpl.java:50-93` 的异步监听器第 53 行解析、第 71/73 行直接解引用。该支路可丢失登录/失败/退出/注册审计行；修复不应改变第 56-69 行 Client 查询及已有日志内容、第 75-93 行状态与入库。

## 最小修复界限

在上述三个现有生产文件各自计算安全的 browser/os 名称：UA 为 `null` 或空白时两者均用 Hutool 当前约定的 `Unknown`（本地 `UserAgentInfo.NameUnknown` 为此字符串）；非空 UA 沿用当前 `UserAgentUtil.parse(...).getBrowser()/getOs()` 结果。可以在每个调用点局部判断 `userAgent == null`；若顺手防御解析器对象字段为 null，也仅回退两个描述字段。不要通过给测试客户端添加 UA、给 parser 传伪造字符串、捕获后跳过整个监听器、绕开 Sa-Token 事件或更改 LoginInfoEvent/DTO/认证合同来掩盖故障。

`LoginHelper` 保留“只有原 browser/os 为空才补写”的条件；同步在线 DTO 和异步审计行照旧写入，只是 UA 缺失时 browser/os 为 `Unknown`。三处已有 `StringUtils`/Hutool 依赖，局部修复无需新增公共 SPI、配置或跨模块依赖。正常 UA 的解析值必须不变。`ServletUtils.getRequest()` 为 null 属于另一入口条件：`LoginHelper` 已直接返回；本次已证实的是有 HTTP request 但缺 UA，不应扩大为重新设计事件异步上下文。

## 必需回归与写集

现有 T40 ticket 的 `writable_paths` 仅列通知业务及其测试/前端路径，**没有**上述三个生产文件；写入前由 Lead 将其和选择的精确测试路径预登记到当前票/Packet。修复不需要改 UA 事件生产者、认证 Controller、Client 校验或浏览器 runner。

最小验证为真实有效 Client/凭据的控制登录：无 `User-Agent`、仅空白 `User-Agent` 各一例，断言业务成功并按原规则获得令牌/身份/Client，在线记录 browser/os 为 `Unknown`，异步 `sys_login_info`（`SysLoginInfo` 的 `@TableName`）最终写入且 browser/os 为 `Unknown`；再用已知 Chrome UA 作正控制，确认正常解析和日志写入未变。若用单元/集成测试补齐，三处各需覆盖 null 与 blank；至少有一条真实 HTTP 链路同时观察同步响应、在线态和异步落库。保留现有控制登录与 T40 公告 E2E 断言，不以补头绕过。只对合成隔离服务验证，不记录令牌或密码；记录命令、退出码、测试/skip 数、固定源码与清理结果。

## 边界

本审查没有执行修复后代码、Maven、HTTP 或真实 DB；不能宣布 C3 或整票 T40 通过。当前 C3 的 HTTP200/R500 与源码空解引用一致，具体响应包装及三次尝试证据仍以 Lead 保留的原始结果为准。建议新候选固定后复核三处 diff 和一次不带 UA 的当前精确服务验收，再沿原 T40 全量门禁完成。
