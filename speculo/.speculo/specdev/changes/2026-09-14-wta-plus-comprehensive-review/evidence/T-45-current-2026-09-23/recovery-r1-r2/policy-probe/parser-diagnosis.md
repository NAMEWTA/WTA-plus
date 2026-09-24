# T45 公开策略诊断 INVALID_POLICY 的零网络复核

固定 `be35584b0d8dcfbb560dc9f338c303dafc77fbfd`，真实单 MinIO 探针 `/tmp/wta-t45/policy-probe/runs/fb6caa8cfb7564b2/result.json`：`submitted_equal=true`，Statement 单项数组、Principal.AWS `['*']`、GetObject Action 和桶资源范围均匹配；Condition/Not* 均不存在。匿名 HEAD/GET 为 ALLOWED，唯一偏差是 POLICY_READ/WRITE 的 `basis=INVALID_POLICY`。owned cleanup=0，source 前后同 clean HEAD。

生产 `AbstractOssClientImpl.publicPrincipal` 先无条件调用 `principal.asText()`，然后才判断 ObjectNode 并读取 `AWS`。Jackson 3 的该操作对 ObjectNode 抛 `JsonNodeException`，被 `policyFacts` 的广义 `catch (Exception)` 映射成 `INVALID_POLICY`。这不是 MinIO 改写策略、Condition 或匿名访问问题。

零网络验证源码 `/tmp/wta-t45/policy-probe/T45JacksonNodeProbe.java`，从固定去敏 Surefire XML 提取同测试的 classpath，以 `/tmp` 中 `javac -cp ... -d node-classes` 编译并运行。实测：`javac_exit=0`、`java_exit=0`，`object_asText=JsonNodeException`、`array_asText=JsonNodeException`、`element_asText=OK`。小程序仅输出操作结果/异常类型，不含策略文本、凭据、桶、endpoint；没有启动服务或修改仓库。

最小产品修复建议：先用 `principal.isTextual()` 守护裸 `"*"` 分支；仅 ObjectNode 且唯一 AWS 属性时，先检查 AWS 是文本 `"*"`，或长度恰 1 的文本数组，再调用其文本元素 `asText()`。复杂 Principal 保持 UNKNOWN，不放宽资源、Action、Deny 解释。新增 ObjectNode 正例与裸星号/非法 AWS 负例，随后重新跑该真实 two-suite 与 T45 全门禁。此处仅诊断，未写产品。
