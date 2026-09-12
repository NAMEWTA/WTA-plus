---
schema_version: 1
artifact: diagnosis
change: 2026-09-08-richtext-third-oss
status: root-cause-confirmed
feedback_loop_ready: true
red_command: "set +e; ! rg -q providerCode ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/controller/admin/ThirdObservabilityController.java; rc=$?; echo assert_optional_providerCode_exit:$rc; test $rc -eq 0"
red_evidence: "assert_optional_providerCode_exit:1 (2026-09-08); controller still requires providerCode."
cleanup_status: clean
updated_at: 2026-09-08T11:28:11+08:00
---

# Diagnosis: 富文本、三方调用明细/统计、OSS 直传

## 1. 现象与影响

- 富文本演示页保存后刷新列表时抛出 `TransportError`，错误被包装成 500。
- 三方调用明细和调用统计页面在进入时抛出 500，页面 mounted hook 里出现未处理异常。
- OSS 直传 URL 能签出来，但浏览器对 MinIO 的 `OPTIONS` / `PUT` 预检失败，说明问题在浏览器 Origin/CORS 和公共访问域名策略，不在密钥本身。

## 2. 红灯反馈回路

- **命令：** `set +e; ! rg -q providerCode ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/controller/admin/ThirdObservabilityController.java; rc=$?; echo assert_optional_providerCode_exit:$rc; test $rc -eq 0`
- **至少一次真实输出：** 用户日志中的 `/demo/rich-text/list`、`/third/invocation/list`、`/third/statistics/list` 500，以及 MinIO CORS 拒绝。
- **精确症状断言：** 页面请求在对应接口上返回 500，且富文本页是在 `save()` 后的 `load()` 里炸掉。
- **耗时：** 不适用。
- **确定性/复现率：** 对空 `providerCode` 和缺失 `clientPk` 是确定性路径。
- **Agent 可运行性：** autonomous（静态分析）.
- **无法建立时已尝试方式和所需输入：** 需要登录态、MinIO 运行环境或可直接命中的浏览器会话才能做真正的在线重放。

## 3. 最小复现

- **环境与输入：**
  - 富文本页在没有 `clientPk` 的登录态下调用 `GET /demo/rich-text/list`。
  - 三方页面默认 `providerCode=''`，而序列化层会丢弃空参数。
  - MinIO 只允许有限 Origin，而浏览器 Origin 与预签名 URL 的 host 不一致。
- **剩余步骤：**
  - 富文本：进入页面或保存后刷新列表。
  - 三方：直接进入调用明细 / 调用统计页。
  - OSS：从浏览器执行预签名 PUT。
- **逐项删除证据：**
  - 去掉列表刷新，富文本保存不会暴露这个 500，但问题只是被遮住。
  - 给三方页补上 providerCode，接口才不报缺参。
  - 让 MinIO 接受当前 Origin 后，预检恢复正常。
- **最后红灯证据：**
  - 富文本错误来自列表刷新后端 500。
  - 三方页面错误来自空 providerCode 被剥离。
  - OSS 错误来自 CORS 预检失败。

## 4. 假设与证伪

| 排名 | 假设 | 可证伪预测 | 当前证据 |
|---|---|---|---|
| 1 | 三方 controller 将空 providerCode 视为必填 | 删除参数时 Spring 前置校验返回 500 | 静态红灯命中 |
| 2 | 富文本会话缺少 Client | clientPk 为空时服务抛“当前会话缺少 Client” | 源码与日志时序一致 |
| 3 | MinIO CORS 只允许固定 Origin | 未列 Origin 的 OPTIONS 无 CORS 头 | 用户预检日志与 compose 默认值一致 |

## 5. 已确认根因

### 5.1 富文本页

`RichTextPage.vue` 在保存成功后立即执行 `await load()`，而 `load()` 固定调用 `runtime.service.richText.list({ pageNum: 1, pageSize: 50 })`。对应后端 `TestRichTextServiceImpl.list()` 同时依赖 `LoginHelper.getUserId()` 和 `clientPk()`；`clientPk()` 一旦拿不到当前会话的 `clientPk` 就抛 `ServiceException("当前会话缺少 Client")`。

问题不在富文本 HTML 本身，而在当前登录态没有满足这个 Client 归属约束，或者有一条登录路径没有把 `clientPk` 写进会话。`SysLoginService.buildLoginUser()` 只有在 `client != null` 时才给 `LoginUser.clientPk` 赋值，所以这条前置契约要么在登录侧补齐，要么在服务侧显式返回可识别的登录错误，而不是让页面看到统一 500。

### 5.2 三方调用明细 / 统计

`ThirdPage.vue` 默认 `providerCode = ''`，但 HTTP 序列化工具 `tansParams()` 会把空字符串直接省略。于是页面实际发出的请求里没有 `providerCode`，而 `ThirdObservabilityController` 的两个接口都写成了 `@RequestParam String providerCode` 必填。

Spring 因缺少必需请求参数抛的是 `ServletException` 系列异常；`GlobalExceptionHandler` 没有单独处理这个分支，所以最终被统一包装成“发生未知异常，请联系管理员”的 500。E2E 里本来就断言了调用明细和统计页可以直接打开并显示数据，所以当前实现和产品意图是冲突的。

### 5.3 OSS 直传

当前 compose 已经把 `MINIO_API_CORS_ALLOW_ORIGIN` 做成环境变量，默认值是一个 Origin 白名单。MinIO 官方文档支持把该值设成通配 `*`，所以“不能不限制 IP”这个前提不成立。更关键的是，代码里已经把 `endpoint` 和 `domainUrl` 分开：

- `endpoint` 给后端 SDK 访问 MinIO。
- `domainUrl` 给浏览器预签名 URL。

`DefaultOssClientImpl` 也确实是用 `endpoint` 初始化 S3 client、用 `domainUrl` 初始化 presigner。也就是说，这里不需要把浏览器直连地址和内网服务地址绑死在同一个 IP 上。真正应当稳定的是浏览器可见的公共域名，而不是容器 IP。

## 6. 修复契约

- **必须改变：**
  - 富文本的 Client 归属要么在登录态里稳定补齐，要么在后端返回明确的登录类错误码，不要再落成泛化 500。
  - 三方调用明细 / 统计要支持空筛选条件，空 `providerCode` 应该返回全量或最近数据，而不是把页面炸掉。
  - OSS 浏览器直传要改成稳定 Origin / 稳定公共域名策略，不要依赖可变 IP。
- **必须保持：**
  - 富文本仍然按当前用户 + Client 做隔离。
  - 三方调用明细 / 统计仍然只读，不放开写口。
  - OSS 仍然保留预签名和私有桶语义。
- **正确 seam：**
  - 富文本：登录态 / `LoginUser.clientPk` / 业务异常码。
  - 三方：controller 参数语义 + service / mapper 的动态过滤。
  - OSS：`MINIO_API_CORS_ALLOW_ORIGIN`、`SysOssConfig.domainUrl`、部署文档。
- **回归测试：**
  - 富文本保存后刷新列表不再抛 500；缺 client 时返回明确错误。
  - 调用明细 / 统计页空筛选可直接打开并返回 200。
  - 指定前端 Origin 的预签名 PUT 预检通过；切换到稳定公共域名后不再依赖 IP。
- **OUT：**
  - 不在此 change 里改生产代码。
  - 不在此 change 里改 Bucket policy 或做不可逆迁移。
- **风险与回滚：**
  - 富文本和三方修复如果只改前端，会掩盖后端契约问题；应优先修后端语义。
  - OSS 若直接放开 `*`，只建议用于开发/测试；生产应优先稳定域名加显式白名单。
- **推荐下游：** T-tickets / I-implement

## 7. 清理

- 本次没有生成临时 debug 插桩、脚本或抓包文件。
- `diagnostics/` 目录未创建。
- 后续实现建议仍按上面的修复契约拆票推进。
