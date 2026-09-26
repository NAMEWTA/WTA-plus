# T46 最终候选实现与验收证据复核

**结论：通过本票已登记的实现与验收门禁。** 固定候选 `4ecd45d7207429e78d767b281851eafb1ba0f955`，tree `38f4411a53af1096388dbe4afae610f72eda8365`，审查时 clean。依据固定源 `/tmp/wta-t46/real02-fixture-static-review.md`、真实隔离运行 `/tmp/wta-t46/real02-runtime-review-4ecd45d7.md`，以及下述最终候选门禁记录。只读核验，未重新运行构建/服务或修改仓库。

| 门禁 | 固定输入与结果 | 证据边界 |
|---|---|---|
| 精确真实 MySQL+MinIO | `oss-migration-runs/e9768dda90c986f5/result.json`：real02 Maven0、fresh 两方法 2/0 failure/0 error/0 skip、source 前后同 clean4ecd、两个 owned 容器及匿名卷/回环端口/进程全回收，cleanup[]。 | 使用真实动态事务代理、MyBatis/双连接/真实 MinIO；代理是测试手工 Spring `ProxyFactory` 装配，**不是 full-app HTTP Bean 启动证明**。 |
| 默认后端测试 | `a2-backend-default.json` exit0；`a2-backend-default-counts.json` 逐类求和 262 classes/1145 tests，其中 **928 实际执行通过、217 环境 opt-in skip**，0 failure/error。最终 SHA 的 `OssClientProviderUnitTest` 6/0、`OssStorageMigrationServiceUnitTest` 17/0；该默认运行中的迁移集成 2 项均 skip，不重复计作 real02，通过数仍以真实 run 为准。 | `green03` 的 175 个选择性测试属于旧 c303 候选；只因 c303→4ecd 后端变更仅为 `OssStorageMigrationIntegrationTest`，可作生产未变的背景证据，**不冒称 green03 在 4ecd 重跑**。 |
| Full package/bundle | `a2-full-package.json` 与 `a2-full-bundle.json` exit0；`a2-full-package-proof.json` 绑定 clean4ecd/tree 和 full JAR `e27b3af7b290125da3b7d14b7817c5d738a44ed94550df658a4e020ec17be70a`。私有 retained `artifacts/a2-full/wta-admin.jar` 逐字节 SHA 与 proof/manifest 一致，39 个 WTA 库；source before/after 等价且 clean。 | 后续 core clean 构建覆盖了 `backend/wta-admin/target/wta-admin.jar`，故 full 的权威物理文件是 retained 副本；不能把当前 target SHA 称作 full。 |
| Core package/bundle | `a2core-core-package.json` 与 `a2core-core-bundle.json` exit0；`a2core-core-artifact.json` 绑定同 clean4ecd/tree、core JAR `23975c269806f0f690b834a6e07e75ece15110e0363da47f00881cd79e9cec38`，当前 target 实际 SHA 相同，29 个 WTA 库；source before/after 等价。 | full/core 两种制品 SHA 不同且已明确分开；均是构建/打包证明，不是 full-app HTTP 运行。 |
| 静态/写集 | `a2static-{fullstack-facts,skill-facts,fm,handbooks,notify-layered}.json` 各 exit0，`a2static-source.json` clean 前后；`a2-write-scope-and-ancestry.json` 记录 19 实际产品路径/14 已登记根、outside_scope=[]、frontend_unchanged=true。 | 本票前端源未改，**没有声称在最终 SHA 跑过新的前端测试**；按精确写集不重复无关前端门禁。 |

real01 `60ecc38cb9c20055` 属旧 c303 源，2 项中首方法第191行 UNKNOWN=null，确实失败；保留该历史。4ecd 仅将测试 ACK-loss 注入绑定成功写入 UNKNOWN 的 XID；real02 首方法完整通过已证明目标事务 arm/hit 各 1、异常链为受控 SQLRecoverableException、UNKNOWN 持久、DELETE 未提前触发。该真实绿灯**不反向证明** real01 的具体早期 commit 栈位置；诊断仅给出高可信机制解释。第二方法真实通过覆盖对象/配置 NOWAIT 锁、旧恢复锁内复核、未创建桶的 HEAD404 保守未知、晚到物理 DELETE 期间第二对象推进与一次删除后核对。

余下证据界限：隔离测试未启 full-app HTTP，也未接生产存储/凭据；依照本票边界，这不影响上述门禁判定。部署与归档由主代理按照 goal-plan/归档流程决定，本报告不替代该操作。
