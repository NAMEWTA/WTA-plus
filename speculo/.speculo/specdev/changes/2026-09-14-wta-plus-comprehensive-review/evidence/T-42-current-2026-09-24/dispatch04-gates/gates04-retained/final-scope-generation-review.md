# T42 最终写集与生成合同只读自审

审查身份：原产品 writer 的自审，**非独立验收**。输入基线 `5417c257130216e2b283ca933d9496d76c10f46a`、最终提交 `73c28e751f6276e45b0488ab5d8eacbf9f0c77fb`（当前 HEAD，`git status --short` 为空）、Ticket42 frontmatter 的 `writable_paths` 66 根，以及真实 HTTP 捕获 `/tmp/wta-t42/openapi-http-live/2a1672a910adffea/{result.json,source.json}`。本次只读，没有仓库写入、构建或服务启动。

## 精确写集

`git diff --name-only` 从基线到最终提交共 630 路径；其中 546 位于本 change 的 `speculo/.speculo/specdev/changes/2026-09-14-wta-plus-comprehensive-review/` 治理/证据目录，84 个非治理路径。使用 Ticket42 frontmatter **66 条 `writable_paths`**（文件精确匹配、目录仅匹配其子路径）逐一核对后，84/84 均覆盖，越界 **0**；`expected_changes` 同为 66 条。变更分布为后端实现/测试、六 SQL 与交付合同、项目引用事实，以及正式 API 生成物。治理/证据由 Lead 单独持有，本报告只核当前最终 diff 的路径归属，不将 546 条治理文件冒充产品写集。

从后端固定源 `c980698b794fba9b02d0041207f57c7b5aa77bb7` 到最终 `73c28e...`，`git diff --name-only` **恰好四个**非治理路径，均为授权的正式生成合同：`frontend/packages/api-contracts/generated/openapi.ts`、`openapi/current.json`、新 revision `95e5ae4b17d69b749f17faad172a4b865d5527a9a6174e0469839205a111804e/{source.json,provenance.json}`；该区间没有后端产品或测试变动。

## 真实来源与语义差异

真实捕获 `2a1672a910adffea/result.json` 记录 clean 的源 `c980698b...`、full JAR SHA-256 `d67d1a5dfbfa95655ed3450fb51302ba3f9462853a9474c2c13aead78357b54a`、`/v3/api-docs` HTTP 200、512478 bytes、原文 SHA-256 `3b7f8a6a5fc47072eadcb719d044c949f6312fbf1240ac11a5e0ebdfe5a1dda0`、owned 资源清理无错。最终 revision 的 `source.json` 与捕获原文 JSON 相等，文件哈希也等于该 raw SHA；`provenance.json` 的 backendCommit、runtimeEndpoint、rawSha256、generator `openapi-typescript@7.13.0` 与此一致。`generated/openapi.ts` SHA-256 `5252f88227027d028658b378fab60ba3f26483fe8ac8d59c67c00a0654f4987a` 与 `/tmp/wta-t42/gen04-openapi-generation-proof.json` 相符。Lead 留存的 fetch/generate/check/types 四项命令记录均 exit 0；这里只核证据，不重新运行。

直接以基线 current revision `0c1c94bf47083a6b7a3620c9494ee23bd629321ac7d8c3d7963f8c840343a477` 与最终 revision 结构比较：paths **438→438**、schemas **450→450**，无删除或新增 path/schema，所有既有 path operation 语义未变化。共同 schema 中仅两处语义差异：`NotificationCommand` 新增可选 `attachmentOssIds: array<string>`，`required` 仍仅 `priority`；`SysOssVo.deleteState` 描述加入已实现的 `NOT_READY`。`servers[0].url` 只从一次 loopback 端口变为本次 owned loopback 端口。生成 TS 除上述两处，还出现 `Instance.defJson/tenantId` 与 `Definition.userList/...` 的属性声明顺序调整，类型和值不变；没有意外删除。真实 HTTP 正向证据还记录 >2^53 的带引号 OSS ID 精确保留、无附件 submit 零 S3；零/负/小数/溢出/前导零/空白及无权限请求均零写入。数字 JSON token 被精确绑定是观察项，不把它扩大成新公开 `number[]` 合同。

## core04/JDT 与剩余边界

`/tmp/wta-t42/core04-jdt-exclusion.json` 记录受控编辑器进程 `paused=true` 且 **`resumed=true`**，build_exit_code 0；`core04-core-package.json` 和 `core04-core-bundle.json` 均 exit 0；source before/after 均 clean、同 `c980698b...`/tree `b518062e41cdd42b9be37b53cb6ea110d61d1a40`。`core04-core-artifact.json` 记录 core JAR SHA-256 `dc87d2d4c5cb443c9628d13ade3499233790c5db893d94e6ad93b87b5ed77371`。这是已保存的 core04 事实，不替代正在运行的最终 frontend gate 或随后 Lead 的候选收口。

**写集与生成合同自审未发现越界或语义缺口，但此候选存在新的架构门禁阻断，不能批准关闭。** Root frontend gate `57148` 随后已由 Lead 确认通过；不能用前端通过替代 Notify 分层静态检查。本报告没有将源哈希、单测、生成检查与真实 SMTP 发送混为同一证明。

## 后续 static04 阻断更新（原产品 writer 自审）

同一固定源 `73c28e751f6276e45b0488ab5d8eacbf9f0c77fb` 的 `/tmp/wta-t42/static04-notify-layered.log` 显示 **8 个实际错误**：`NotifyAttachmentReleaseJob` 和 `NotifyAttachmentSnapshotAdapter` 各自直接 import DAO 与 runtime Service（4）；`SessionNotifyAttachmentActorAdapter` import 错层 runtime ActorPort（1）；ActorPort 与 SnapshotTransactions 放在只允许 `*Service` 的 service 目录（2）；SnapshotTransactions 在 UseCase 以外声明 `@DSTransactional`（1）。校验脚本也禁止 Adapter 直接依赖 UseCase，因此单纯改名、改全限定名或放宽规则均不合格。

符合现有校验器的最小改法是新建 `port/NotifyAttachmentActorPort.java`、`port/NotifyAttachmentSnapshotPort.java`、`usecase/NotifyAttachmentSnapshotUseCase.java`，把原 SnapshotTransactions 迁为 `service/runtime/NotifyAttachmentSnapshotService.java` 并将四个短事务注解移到 UseCase 的 Spring 代理入口。两个 Adapter 只依赖新 Port；Service 保留 DAO/System OSS 操作及原锁序，远端复制仍在 reserve 与 confirm 两个短事务之间。新 Port 的 `Prepared` 可携既有公开 OSS 预约值；`confirm` 接收复制长度与 SHA 等中性事实，由 Service 转回 System 结果类型，UseCase 不直接 import System API。旧类名在 `NotifyMailAttachmentIntegrationTest`、`NotifyAttachmentRuntimeRedTest`、`NotificationApplicationRuntimeService` 和 OSS owner manifest 的引用须同步。上述三个新增路径需 Lead 事前登记；其余已在原 Ticket42 写根。此段是**只读修复建议**，仓库尚无修复，static04 尚未重新通过。
