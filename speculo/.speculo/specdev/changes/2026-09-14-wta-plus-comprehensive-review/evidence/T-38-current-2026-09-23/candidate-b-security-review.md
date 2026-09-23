# T-38 Candidate B 增量安全/合同审查

固定 Source A `7a6f75ac9238399daf7936797d07da141f0f5a03`（tree `3c519c0cad48d11ec1331a92884332a0ca230f4b`），最终 B `a3b289efbaf5862e55a40a6c58a6ad66c79b40b2`（tree `e2997f977fbbb2d3b48590233b38865b689e5a87`），B 直接父为 A。审查 `git diff A B`/固定 `git show`；未依赖工作树、未修改仓库、未执行 build、Maven、服务或测试。

**增量静态结论：PASS，无 B 新增阻断。** A→B 仅改 `frontend/packages/api-contracts/README.md`、`generated/openapi.ts`、`openapi/current.json`，新增本次 revision 的 `source.json`/`provenance.json`，共 5 个文件；`backend/`、`release-artifacts/`、`.agents/` 无差异，`git diff --check A B` 无错误。因此 A 已审的重试权限、事务、锁与保守外部 UNKNOWN 生产路径原字节继承；详见 `/tmp/wta-t38/source-a-security-review.md`。本结论不替代 Lead 正在执行的 B 真实 14 项、full frontend/core 门禁。

## 来源与合同核对

- `openapi/current.json` 指向新增 revision `5f2afc58f2a31e4c172fbf12076d43f1aba8fbfbba6e71079701bc335c976302`。其 `source.json` 在 B commit 的原字节 SHA-256 是 `8f57fc94ab0936c48d57b5947879ea0e7280a6b559dc4895852a9670ae97b3c7`，与本 revision `provenance.json:6` 的 `rawSha256`、Lead 的 `/tmp/wta-t38/openapi-live/26d29e5a680cf899/source.json` **逐字节相同**。`provenance.json:2-12` 指向 A、OpenAPI 3.1.0、436 paths/445 schemas/77 tags；实际解析 source 也为 436/445/77。`README.md:13` 对这些数字和 A 的描述一致。
- 已清洗的 `/tmp/wta-t38/openapi-live/26d29e5a680cf899/result.json` 显示真实 HTTP `/v3/api-docs` 200、JSON、508562 bytes、exit 0、acceptance true，source 前后均 A/tree `3c519c0c`/clean，full JAR hash `01678e64ede453a387d352b7961cb97ee382bd945ed2a2935851f46ee52c1ef4`；backend PGID 无成员、owned 容器/匿名卷/四 loopback 端口均已回收且 cleanup errors 空。该数据是 **A/JAR 的 live capture**，不是 B 编译或 B 浏览器复验。历史 v1 失败不被 v2 成功覆盖，仍保留独立证据。
- 旧 revision 433/444→新 revision 436/445：新增且仅新增三个 path：`POST /resource/oss/{ossId}/publish`、`POST /resource/oss/{ossId}/unpublish`、`POST /resource/oss/{ossIds}/restore`；新增 schema `PublishRequest`。A 的 `SysOssController.java:33,87-103,124-129` 路由、HTTP 方法、`system:oss:publish/remove` 权限与 source/TS 一致。旧 `POST /resource/oss/{ossIds}` 和 `POST /notify/notice/{noticeId}/publish` 保留，因新增 OSS `publish` 操作，生成器把 OSS 操作命名为 `publish_1`、公告改为 `publish_2`；path→operation 绑定仍对应。旧路径没有删除或方法改变。
- `POST /notify/notification/{notificationId}/retry` 保留为生成 `paths` entry，绑定 `operations["retry_1"]`；source 请求 schema 是 `NotificationRetryCommand`，200 响应包装 `RRetryReceipt`。`RetryReceipt.queuedCount` 在 raw schema 为 `integer/int32`，生成 `openapi.ts:11155-11159` 为 `queuedCount?: number`，描述“本次实际重新排队的投递数；零表示持久状态未改变”；与 A 的 `RetryReceipt.java:8-10` 原始 `int` 一致。`NotificationRetryCommand.idempotencyKey` 描述的当前仅接受空值及 `DeliveryReceipt.providerMessageId` 脱敏说明也从 A 的公开 Javadoc 带入，B 没有新状态承诺。
- 新 `SysOssVo.accessPolicy/restorable` 字段对应 A 的 `SysOssVo.java:77,82`；`RLong` 在 TS 的位置变化和已有 workflow `Skip` 字段顺序变化均不改变字段类型。B 的其他可见差异为新增 OSS 结构和 A 已存在的公开 Javadoc。未见生成合同把旧安全限制放宽、把 `queuedCount` 误生成为字符串、或新增未实现的通知路由。

## 验证边界

Lead 的 A 当前真实 13 个 `NotifyManualRetryIntegrationTest` + 1 个 Wake 共 14 个测试、0 fail/error/skip 已由 `/tmp/wta-t38/runs/83150f82cac60fb5/result.json` 证明；默认后端 251 类、854 执行/163 环境 skip 与 full JAR 均是 **A 产品输入**，由于 B 不改 backend/release，可作为相同输入的已有证据引用，不能写成 B 重新运行。B 的专属真实 14 项、full frontend/core 运行在本报告时仍待 Lead 结果，`openapi:check` 和 typecheck 亦须以其精确出口补证。静态 PASS 不等于 T-38 整票完成。
