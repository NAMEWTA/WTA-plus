# T-50 Source A2 固定差异工程复审

基线 A1 `9bb2e2888b5d2b0563beb7164c8c0a2165547e2c` → A2 `1dfdcbca831ad7391d725d7f001acb2b9ba1c88f`；整票初始 base 仍为 `d544f02e1627d76883e9eeaf73a8f2b05002d079`。仅 `backend/wta-admin/src/test/java/org/namewta/test/notify/NotifySupportedModeIntegrationTest.java` 一处路径改变，生产 Java、API、通知规范、其他真实测试与 A1 字节相同；A1 固定源码审查的其他静态结论可继承。`git diff --check A1...A2` exit 0。本报告不运行 Maven/Docker/HTTP、不编辑仓库。

**结论：A2 对 A1 唯一真实失败的修正合理，八类隔离真实验收已通过；完整 T-50 交付仍待默认/full 构建、live OpenAPI 与前端生成门禁。**

A1 将省略 strategy/mode/**priority** 的 JSON 请求预期为 200，却在真实 HTTP 得到 400。A2 保留“省略 strategy/mode”正例，但显式发送 `priority:0`（第 308 行）；新增“priority 缺失”负例，断言业务码 400 及五张 owned 表计数逐项不变（第 330-333 行）。显式合法与非法策略、SYNC、非零/负优先级、无权/未登录断言均保留，生产绑定没有被放宽。现用 Jackson 3 的 primitive `int` 缺值绑定实际拒绝；Java record 构造器对 strategy/mode 的 null 归一化不能推导出 primitive priority 省略也可用。公共合同对 HTTP 调用方应表述为 `priority` **须传 0**；不要把“默认优先级 0”写成“JSON 可省略 priority”，除非后续另行设计并测试可选字段行为。

Lead 的 A2 owned 八类运行 `/tmp/wta-t50/runs/e29ae3ce94c6942c/result.json`：8 份 fresh XML，135 tests / 0 failure / 0 error / 0 skip；source 前后 clean `1dfdcbca831ad7391d725d7f001acb2b9ba1c88f`，tree 均 `6021ef927e0823f7059ec8a44ea71f71aa7faa25`；Maven exit 0，进程组、自有容器、精确匿名卷及两个 loopback 端口清理均无 errors。独立 retained XML/hash/mtime/count 重核 `/tmp/wta-t50/a2-retained-verification.json` 也为 135/0/0/0、8 类；旧 A1 135/1 证据未覆盖。隔离供应商仍为替身，这不是生产供应商网络验收。

下一步只需继续已计划的默认 Maven、完整 JAR 来源证明、owned live `/v3/api-docs` 原字节捕获、生成/检查合同和前端门禁。A2 无新增生产行为，若这些门禁发现实际问题，应固定下一候选并仅复审增量；不能把当前八类通过扩称完整票通过。
