# T45 两类真实 MinIO JUnit 结果只读核验

固定结果：`/tmp/wta-t45/oss-two-runs/45cde3cd5fe92d7c/result.json`，源码前后同一 clean HEAD `09be6db6c3bace8594f7db0fe49a221e3cc5f140` / tree `2d5459e7dc77ec4120516d97a63de17eee390be4`。Maven exit0，runner `acceptance=true`。

两份本次 fresh XML 的实际类/方法分别为 `OssStorageReadinessMinioIntegrationTest#verifiesPublicAndPrivateBoundariesWithoutMutatingPolicy` 与 `OssAccessUrlMinioIntegrationTest#resolvesStablePublicAndExpiringPrivateUrlsAgainstRealMinio`，各 `tests=1/failures=0/errors=0/skipped=0`。逐份独立重算保留的脱敏 XML SHA 与 result 的 `sanitized_sha256` 相同；XML suite/method/count、本次 `t45.owned.run` marker 一致，MinIO secret property 已脱敏。原始哈希在结果中保留，未读取或输出任何生成凭据。

三个 owned 容器均记录 64 字符完整 ID；MySQL 起始为空专库、MinIO 身份为 owned bootstrap root。Maven PGID 与 TMPDIR 子进程无残留，两个匿名卷逐名不存在、三个 loopback 端口关闭、`cleanup.errors=[]`；私有 Surefire properties 文件已清理。

结论限定为两类真实兼容回归通过。Readiness 用例中的匿名 PUT=403 是 isolated owned bucket 负控；它不证明生产诊断只读。T45 的受限 app identity、每次诊断精确五个只读请求、HTTP/权限/OpenAPI/Chrome 与完整候选最终验收须看新 UI v5 真实结果；此前两类失败记录仍不追认为通过。
