---
lesson_id: L-028
objective_ids: [OBJ-28]
claimed_cells:
  - A:SysOssMigrationController.batch, items, dryRun, start, retry, rollback, cleanup
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: seven-windows-on-disk
    minutes: 10
  - segment: copy-cas-cleanup-chain
    minutes: 11
  - segment: visuals-and-worked-examples
    minutes: 7
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 3
expression_level: eli5
coverage_depth: deep
source_ids: [S-004, S-008, S-010, S-L028-01, S-L028-02, S-L028-03, S-L028-04, S-L028-05, S-L028-06, S-L028-07, S-L028-08]
---

# Lesson 028：搬家批次——OSS 迁移的宏观五步与两扇观察窗

## 学完你能做什么

打开 `backend/wta-modules/wta-system/src/main/java/org/namewta/system/controller/system/SysOssMigrationController.java`。这份类**正好七个**公开 HTTP 方法，没有第八个，没有 `list` 全表、没有 PUT、没有 DELETE。你能**口述这七扇窗**：哪五步会改存储或目录指针，哪两扇只看批次；每一步碰不碰 MySQL、碰不碰 Bucket、要哪颗权限。口试名单就是矩阵 (a) 这一行，不是 `SysOssController`、不是直传 Ticket、不是前端 `createOssUploadClient`：

1. **`dryRun`**：`POST /resource/oss/migrations/dry-run`。拿着对象清单在门口数箱子，**不建批次、不复制、不改 `sys_oss.service`、不删源**。
2. **`start`**：`POST /resource/oss/migrations/start`。先再跑一遍预检；全绿才插批次，并在**当前 HTTP 线程**里逐条 `process`。返回 `Long` 批次号。
3. **`retry`**：`POST /resource/oss/migrations/{batchId}/retry`。只重做 `FAILED` 且 `lastErrorStage != COMPLETED` 的明细。清理失败不走这扇。
4. **`rollback`**：`POST /resource/oss/migrations/{batchId}/rollback`。把还没终态的明细，目录指针拨回来源；**不删目标副本、不删源对象**。
5. **`cleanup`**：`POST /resource/oss/migrations/{batchId}/cleanup`。身体里必须 `approved=true`，还要过安全窗口，才删**来源** Bucket 里的对象。
6. **`batch`**：`GET /resource/oss/migrations/{batchId}`。看这一票搬家单的汇总。没有「列出所有批次」。
7. **`items`**：`GET /resource/oss/migrations/{batchId}/items`。看这一票里每一件的阶段、错误码、可清理时间。

`wta-system` 在登记表是 **classic**。本课不是 `ISysOssService` 那条老 CRUD 链，也**没有** UseCase。磁盘是 `SysOssMigrationController → OssStorageMigrationService → OssMigrationStore / OssMigrationObjectStore / OssMigrationAccessVerifier`。不要口述成 layered。

本课不宣称你会拆 `SysOssConfigController`（OBJ-25）、`SysOssController` 列表/下载/删除（OBJ-26）、直传 `init/signParts/parts/complete/abort`（OBJ-27）、或浏览器 `createOssUploadClient`（OBJ-29）。今天只认：**怎么把已经落在 PRIVATE 配置上的 ACTIVE 对象，搬到 PUBLIC_READ 配置上，以及搬失败时指针怎么回来、源对象何时才准扔。**

## 先把宏观地图放在桌上

NAMEWTA 把对象存在 MinIO/OSS 上，目录写在 MySQL 表 `sys_oss`。列 `service` 是**唯一路由键**：它告诉系统「这个 `ossId` 现在住在哪一份 OSS 配置里」。业务表只记 `ossId`，不记 URL。访问策略库存里只有两种：`PRIVATE(0)` 与 `PUBLIC_READ(2)`。迁移不是「任意桶互搬」，磁盘硬编码成：**来源必须 PRIVATE 且 SERVING，目标必须 PUBLIC_READ 且 SERVING。**

2026-09-16 工作树里，搬家柜台单独一块门牌，不要把它和对象清单柜台合成一个 `/resource/oss` 超级 Controller：

```text
管理端 / 运维脚本                         （本课只认 HTTP；前端工厂是 L-029）
        │
        ├─ POST /resource/oss/migrations/dry-run     dryRun
        │        预检报告；零写入
        │
        ├─ POST /resource/oss/migrations/start       start
        │        再预检 → 建批次 → 同步 process 每件
        │
        ├─ GET  /resource/oss/migrations/{batchId}           batch
        ├─ GET  /resource/oss/migrations/{batchId}/items     items
        │
        ├─ POST /resource/oss/migrations/{batchId}/retry     retry
        ├─ POST /resource/oss/migrations/{batchId}/rollback  rollback
        └─ POST /resource/oss/migrations/{batchId}/cleanup   cleanup
                 body: { "approved": true } 才允许删源
```

| 类 / 合同 | 磁盘路径 | 公开 HTTP | 继承 `BaseController`？ | 持久化 |
| --- | --- | --- | --- | --- |
| `SysOssMigrationController` | `.../controller/system/SysOssMigrationController.java` | **7**：batch / items / dryRun / start / retry / rollback / cleanup | **否** | 自己不碰 Mapper；只调 `OssStorageMigrationService` |
| `OssStorageMigrationService` | `.../oss/migration/OssStorageMigrationService.java` | 无 HTTP | — | 编排预检、复制、CAS、回滚、清理 |
| `sys_oss_migration_batch` | 基座 DDL | 无直接 HTTP 列表 | — | 批次汇总。`dry_run` 列存在，但 HTTP `dry-run` **不插这张表** |
| `sys_oss_migration_item` | 基座 DDL | 经 `items` 读出 | — | 每件阶段 / 状态 / 错误码 / 窗口时间 |
| `sys_oss.service` | 表 `sys_oss` | 本课不提供改 service 的裸 API | — | CAS 切换来源→目标；失败或回滚再 CAS 回来 |

矩阵 (a) 这一行的方法名单**与磁盘一致**。多出来的是菜单种子：`50-cde-base-dml.sql` 有 `system:oss:*` / `system:ossConfig:*`，**没有** `system:ossMigration:list|execute|rollback|cleanup`。口试按磁盘注解，不要把「文件管理菜单」背成已经挂了迁移按钮。

**类比：** 学校要把锁着的库房箱子（PRIVATE Bucket）搬到临街橱窗（PUBLIC_READ Bucket）。目录柜上每张卡片写着「这只箱子现在在哪间房」（`sys_oss.service`）。搬家公司先拿清单在门口点名（dry-run）；点名通过才发一张工单号（start 的 batchId），当场把箱子**复制**到橱窗、改卡片、再隔着玻璃试拉一下门（匿名 GET/HEAD）。拉不开就把卡片改回库房。过了观察期、校长再签一次字，才准把库房那只原箱扔掉（cleanup）。工单看不清就用 batch/items 两扇窗，但你必须已经知道工单号。

**类比失效处：**

1. 不是任意两间房互搬。来源不是 PRIVATE、目标不是 PUBLIC_READ，预检直接红灯。
2. 不是「晚上后台慢慢搬」。`start` / `retry` 的 `process` 跑在**当前请求线程**里，没有 SnailJob 队列。
3. 橱窗里那只副本，回滚时**不会**搬走。rollback 只改卡片，integration 断言目标对象还在。
4. 门口点名（HTTP dry-run）**不**给工单。表上虽有 `dry_run` 列（DDL 默认 `'Y'`，实体字段默认 `"N"`），`dryRun()` 方法体零 `createBatch`。
5. 扔原箱不是 start 的最后一步。默认还要等 `oss.migration.cleanup-delay`（代码默认 24 小时），并且 cleanup 身体里的 `approved` 必须是 true。
6. 前端 OpenAPI 生成了这七条路径，**没有** `createOssMigrationService` 或迁移页。不要把 L-029 的直传客户端说成本课 UI。

## 核心概念与机制

### 直觉讲解

小孩子版只记十二句：

1. **一份 Controller，七扇窗，一块牌子。** 类上 `@RequestMapping("/resource/oss/migrations")`。数方法先打开这一份，不要在 `SysOssController`（`/resource/oss`）里找 dry-run。
2. **五步会动手，两步只看。** dry-run / start / retry / rollback / cleanup 都是 POST，都有 `@Log` 且 `isSaveRequestData = false`，都有 `@RepeatSubmit`（默认 5 秒）。batch / items 是 GET，无 `@Log`。
3. **四颗权限不要并成一词。** `list` 管看；`execute` 管预检、启动、重试；`rollback` 单独一颗；`cleanup` 单独一颗。删源不是 execute。
4. **预检全绿才准开工。** `start` 内部先调 `dryRun`；`ready()==false` 抛 `INVALID_REQUEST`「迁移预检未通过」，不建批次。
5. **复制先于改名。** `process` 先 `transferAndVerify`（Bucket 间 copy + 校验），再 CAS `sys_oss.service`。卡片还没改时，业务仍去库房。
6. **改名用比较并交换，不是 `updateById`。** SQL 要求当前 `service` 等于期望值，且 `delete_state = 'ACTIVE'`。别人先改了就 `SERVICE_DRIFT`。
7. **橱窗要真的能从街上看见。** CAS 之后 `verifyPublic`：解析出的访问必须是 `PUBLIC` 且 `expiresAt == null`，再匿名 Range GET，状态码只要 200 或 206。失败会尝试把卡片拨回库房。
8. **到站不等于扔原箱。** 成功停在 `CLEANUP_ELIGIBLE`，`cleanupEligibleTime = now + cleanupDelay`。批次这时也是 `CLEANUP_ELIGIBLE`，**不是** `COMPLETED`。
9. **重试不重做已经终态的件。** `retry` 跳过非 FAILED，也跳过 `lastErrorStage == COMPLETED`（那是清理失败，要再走 cleanup）。
10. **回滚保留两份字节。** 源还在才准标 `ROLLED_BACK`；目标副本留下。源没了就这件变 FAILED，阶段记 `ROLLED_BACK`，错误 `OBJECT_NOT_FOUND`。
11. **清理要第二张字条。** `CleanupRequest.approved == false`（缺字段也是 false）→ `CLEANUP_NOT_APPROVED`。窗口没到 → `CLEANUP_WINDOW_OPEN`。批次里混进别的状态 → `INVALID_STATE`。
12. **错误码进明细，不一定进 HTTP 正文。** 明细 `errorMessage` 存的是枚举名（如 `CONTENT_MISMATCH`）。`OssMigrationException` 没有专用 `@ExceptionHandler`，会掉进全局 `RuntimeException` 处理，客户端看到的是「未知异常」加错误编号，不是枚举名。

### 精确定义与 English term

| 中文 | English | 精确定义（本课，以工作树为准） |
| --- | --- | --- |
| OSS 迁移控制器 | `SysOssMigrationController` | `@RestController`，`@RequestMapping("/resource/oss/migrations")`，**不**继承 `BaseController`。公开方法正好七个，与矩阵同行同名 |
| 迁移服务 | `OssStorageMigrationService` | `@Service`。Controller 唯一注入。公开方法与七扇窗同名（`cleanup` 在服务里是 `(batchId, approved)`） |
| 迁移请求 | `MigrationRequest` | record：`@NotEmpty List<@NotNull Long> ossIds` + `@NotBlank String targetConfigKey`。**没有**来源配置字段；来源读 `sys_oss.service` |
| 清理请求 | `CleanupRequest` | record：单个 `boolean approved`。没有默认 true |
| 预检报告 | `DryRunReport` | `targetConfigKey` + `ready`（全部 `PreflightItem.ready`）+ 不可变 `items` |
| 预检行 | `PreflightItem` | `ossId / sourceConfigKey / targetConfigKey / objectKey / ready / reason`。失败 `reason` 是 `OssMigrationError.name()` |
| 批次视图 | `BatchView` | `batchId / targetConfigKey / status / totalCount / successCount / failedCount / startedTime / completedTime`。**不含** `dryRun`、`errorMessage` |
| 明细视图 | `ItemView` | 含 `status / stage / retryCount / lastErrorStage / errorMessage / cleanupEligibleTime` |
| 批次表 | `sys_oss_migration_batch` | 主键 `oss_migration_batch_id`。实体 `SysOssMigrationBatch` 继承 `BaseEntity`，`@TableLogic` |
| 明细表 | `sys_oss_migration_item` | 主键 `oss_migration_item_id`。唯一键 `(oss_migration_batch_id, oss_id)` |
| 迁移阶段 | `OssMigrationStage` | `PREFLIGHT, COPIED, CONTENT_VERIFIED, SERVICE_SWITCHED, ACCESS_VERIFIED, CLEANUP_ELIGIBLE, COMPLETED, ROLLED_BACK` |
| 迁移状态 | `OssMigrationStatus` | `PENDING, RUNNING, FAILED, CLEANUP_ELIGIBLE, COMPLETED, ROLLED_BACK`。`terminal()` 只有 COMPLETED 与 ROLLED_BACK |
| 迁移错误 | `OssMigrationError` | 15 个名字。明细里存 name；HTTP 抛错走全局处理 |
| 对象键 | `objectKey` | 取自 `SysOss.fileName`，不是 `originalName`，不是 URL |
| 路由键 | `sys_oss.service` | 对象当前 OSS 配置标识。CAS SQL 在 `SysOssMapper.xml` 的 `compareAndSetService` |
| 删除状态 | `deleteState` | 仅 `"ACTIVE"` 可迁。`PENDING` 预检记 `INVALID_STATE` |
| 就绪快照 | `OssStorageReadinessEntry` | `SERVING` / `NOT_SERVING`。`requireRoute` 要求快照存在、SERVING、且策略等于期望 |
| 访问策略 | `AccessPolicy` | 仅 `PRIVATE`、`PUBLIC_READ`。发布收缩测试钉死没有第三种 |
| 认领 | `claim` | 明细乐观锁：`status` 改为 `RUNNING` 且 `version+1`，条件是 version 匹配且当前不是 RUNNING |
| 清理延迟 | `oss.migration.cleanupDelay` | 默认 24h；启动校验 1 分钟到 30 天 |
| 校验上限 | `oss.migration.maxVerifyBytes` | 默认 64MiB；启动校验 1MiB 到 5GiB |
| 批大小 | `oss.migration.maxBatchSize` | 默认 100；启动校验 1 到 1000 |
| 活动配置占用 | `OssMigrationRequiredConfigContributor` | 未 COMPLETED/ROLLED_BACK 的明细，其 source+target 对 readiness 声明 `OSS_MIGRATION` |

### 机制/因果链

#### 1. 七扇窗对照表（先数门，再讲搬家）

文件：`SysOssMigrationController.java`。只注入 `OssStorageMigrationService`。`OssMigrationHttpContractUnitTest` 与 `OssReleaseContractionUnitTest` 把这张表钉死：根路径唯一、无 PUT/DELETE、每扇都有 `@SaCheckPermission`、五条命令都是 POST + `@Log` + 不存请求体。

| Java | HTTP | 权限 | `@Log` | 入参 | 返回 |
| --- | --- | --- | --- | --- | --- |
| `batch` | `GET /{batchId}` | `system:ossMigration:list` | 无 | 路径 `Long batchId` | `R<BatchView>` |
| `items` | `GET /{batchId}/items` | `system:ossMigration:list` | 无 | 路径 `Long batchId` | `R<List<ItemView>>` |
| `dryRun` | `POST /dry-run` | `system:ossMigration:execute` | 标题「OSS迁移预检」，`OTHER` | `MigrationRequest` | `R<DryRunReport>` |
| `start` | `POST /start` | `system:ossMigration:execute` | 「OSS迁移启动」，`OTHER` | `MigrationRequest` | `R<Long>` 批次号 |
| `retry` | `POST /{batchId}/retry` | `system:ossMigration:execute` | 「OSS迁移重试」，`OTHER` | 路径 batchId | `R<Void>` |
| `rollback` | `POST /{batchId}/rollback` | `system:ossMigration:rollback` | 「OSS迁移回滚」，`UPDATE` | 路径 batchId | `R<Void>` |
| `cleanup` | `POST /{batchId}/cleanup` | `system:ossMigration:cleanup` | 「OSS迁移源清理」，`CLEAN` | 路径 batchId + `CleanupRequest` | `R<Void>` |

五条 POST 都有 `@RepeatSubmit`。GET 两扇没有。批次不存在时，`batch` / `items` / `retry` / `rollback` / `cleanup` 都走 `requireBatch` → `BATCH_NOT_FOUND`。

#### 2. `dryRun`：点名，不动手

`validateRequest` 先挡：请求空、目标空、id 空、id≤0、重复 id（去重后长度对不上原列表）、超过 `maxBatchSize` → 直接抛 `INVALID_REQUEST`，**没有**逐条报告。

然后 `requireRoute(target, PUBLIC_READ)`：目标不在就绪快照、不是 SERVING、或策略不是公有只读 → `STORAGE_NOT_SERVING` 或 `ACCESS_POLICY_MISMATCH`。这也是整单抛错，不是逐条。

循环才开始「温和」：缺对象、`deleteState != ACTIVE`、来源不是 PRIVATE/SERVING、来源等于目标、来源对象 head 不到、目标已有但内容冲突，都收成 `PreflightItem(ready=false, reason=枚举名)`。其它运行时收成 `COPY_FAILED`。`DryRunReport.ready` 是 `allMatch(ready)`。

`OssStorageMigrationServiceUnitTest.dryRunHasNoDatabaseOrProviderMutation` 钉死：零批次、零明细、零 copy、零 delete。运维手册同一句：dry-run 不得创建批次、复制、切换 service、删除源。

`inspect` 会 head 源（PRIVATE 客户端）和目标（PUBLIC_READ 客户端）。两边都在且 `matches` 失败才叫 `conflict`。目标已有**相同**内容不算冲突，后面 `start` 可以跳过物理 copy。

#### 3. `start`：再点名，发工单，当场搬

```text
start(request)
  report = dryRun(request)
  report.ready? 否 → INVALID_REQUEST「迁移预检未通过」
  createBatch(target, n)           状态 RUNNING，记下 startedTime
  每条 PreflightItem:
      createItem(...)              PENDING + PREFLIGHT；objectKey = fileName
      process(item)                同步
  refreshBatch(batch)
  return batchId
```

`createBatch` **不**把 `dryRun` 列写成 `'Y'`。HTTP 开工路径写入的是实体默认 `"N"`。不要把 DDL 默认 `'Y'` 说成「start 先插一条预检批次」。

没有异步执行器。一百个对象就一百次 `process` 排在这个 POST 里。超时是运维风险，不是另有「后台开始」接口。

#### 4. `process`：复制 → 校验 → CAS → 街上试门 → 贴可清理时间

先 `claim(itemId, version)`。SQL 把明细标 `RUNNING` 并 `version+1`，条件是 version 对上且当前不是 RUNNING。抢不到锁就 **return**，不抛错。内存里 `item.version` 再 +1，后面 `saveItem` 才把阶段写回去。

成功认领后的台阶：

1. 阶段写成 `COPIED` 并保存（**先记账，再搬**，所以失败时 `lastErrorStage` 往往已经是 COPIED 或更后）。
2. `objectStore.transferAndVerify(source, target, objectKey, maxVerifyBytes)`：
   - 源不在 → `OBJECT_NOT_FOUND`
   - 目标已在且内容不匹配 → `TARGET_CONFLICT`（不覆盖）
   - 目标不在才 `bucketCopyObject`
   - 复制后还要 `matches`；失败 → `CONTENT_MISMATCH`
   - `matches`：先比 size；再比两侧 checksum 任一算法相同；再比「可信 ETag」（非空且不含 `-`，躲开分片 ETag）；再不行且 size≤上限则两侧流式 SHA-256；超过上限且没有可信校验 → 直接 `CONTENT_MISMATCH`
3. 记下 size/etag，阶段 `CONTENT_VERIFIED`。
4. 读当前 `SysOss`。没有对象 → 失败阶段 `SERVICE_SWITCHED` / `OBJECT_NOT_FOUND`。`service` 仍是来源 → CAS 来源→目标；CAS 失败 → `SERVICE_DRIFT`。已经是目标 → 当作切换成功（幂等）。是第三份配置 → `SERVICE_DRIFT`，**不**把第三份改掉。
5. 阶段 `SERVICE_SWITCHED`，记下 `serviceSwitchedTime`。
6. `accessVerifier.verifyPublic(ossId)`。失败则 CAS 目标→来源；若卡片没回到来源 → `SERVICE_DRIFT`；否则 `ACCESS_VERIFICATION_FAILED`。失败阶段记 `ACCESS_VERIFIED`。
7. 阶段 `ACCESS_VERIFIED`，再写成 `CLEANUP_ELIGIBLE`，`cleanupEligibleTime = clock.instant() + cleanupDelay`，清掉 `lastErrorStage` / `errorMessage`。

`fail()` 一律：状态 `FAILED`，`errorMessage = error.name()`，保存。单元测试要求文案不含 `secret`、`http`。

`CONTENT_MISMATCH` 被单独钉到 `CONTENT_VERIFIED` 阶段，即使当时 item.stage 还停在 COPIED。

#### 5. `retry` / `rollback` / `cleanup`：三把不同的扳手

**retry：** 遍历明细。只有 `status==FAILED` 且 `lastErrorStage != COMPLETED` 才 `retryCount+1` 再 `process`。清理失败（`FAILED` + `lastErrorStage==COMPLETED`）走这扇**不会**再 copy——单元测试 `cleanupFailureRequiresAnotherApprovedCleanupWithoutRecopying`：retry 后 `copyCalls` 仍是 1，真正修好要再 `cleanup(..., true)`。

访问验收失败后的 retry：来源卡片已拨回，目标副本还在。第二次 `transferAndVerify` 发现目标已存在且内容匹配，**跳过** `bucketCopyObject`。单元测试：`physicalCopies` 仍为 1，`copyCalls` 为 2。

**rollback：** `terminal()`（COMPLETED / ROLLED_BACK）的件 `continue`。源对象 `exists` 为假 → 这件 FAILED。若当前 `sys_oss.service` 已是目标，CAS 回来源；CAS 失败 → `SERVICE_DRIFT`。否则只改明细为 `ROLLED_BACK`（卡片本来就在来源时，不强制 CAS）。全程 `deleteCalls` 保持 0。integration：回滚后私有桶对象仍在、公共桶副本仍在、私有匿名 GET 仍是 403。

**cleanup：**

1. `approved` 假 → `CLEANUP_NOT_APPROVED`。
2. 存在既不是 `CLEANUP_ELIGIBLE`、也不是 `COMPLETED`、也不是「FAILED 且 lastErrorStage==COMPLETED」的件 → `INVALID_STATE`「批次尚不可清理」。
3. 非 COMPLETED 的件，`cleanupEligibleTime` 为空或还在未来 → `CLEANUP_WINDOW_OPEN`。
4. 已 COMPLETED 的件跳过。其余：`objectStore.delete(source, objectKey)`，写 `cleanedTime`，阶段/状态都变 `COMPLETED`。删除抛错：这件 `fail(..., COMPLETED, CLEANUP_FAILED)`，再把异常抛出，**后面的件这轮不再删**。

成功删源后 `refreshBatch`：没有 FAILED 且全部 terminal → 批次 `COMPLETED`，补 `completedTime`。

#### 6. `refreshBatch`：汇总怎么从明细滚上来

```text
success = 状态是 CLEANUP_ELIGIBLE 或 COMPLETED 的件数
failed  = 状态是 FAILED 的件数
若 failed > 0           → 批次 FAILED
否则若全部 ROLLED_BACK  → 批次 ROLLED_BACK
否则若全部 terminal()   → 批次 COMPLETED     （含「有的完成、有的回滚」）
否则                    → 批次 CLEANUP_ELIGIBLE
全部 terminal 才写 completedTime
```

混合批次里只要有一件 FAILED，整单就是 FAILED，哪怕别的已经 CLEANUP_ELIGIBLE。口试不要说「有成功件数就等于批次成功」。

`successCount` 把「可清理」和「已完成」算在一起。还没扔原箱也算 success。这是计数口径，不是「源已经删了」。

活动配置：`selectActiveConfigKeys` 把未 COMPLETED/ROLLED_BACK 的 source 与 target 并起来。start 成功后 readiness 仍要求两边都在；rollback 或 cleanup 全部终态后，contributor 不再占用。

#### 7. 失败怎么冒到浏览器

`OssMigrationException` 只是 `RuntimeException`。仓库里**没有**按 `OssMigrationError` 映射 HTTP 业务码的 Handler。`GlobalExceptionHandler.handleRuntimeException` 记日志，返回 `R.fail("发生未知异常，请联系管理员 [错误编号: …]")`。

所以：dry-run **逐条**失败走 200 + `ready:false` + 每行 reason；dry-run/start **整单**校验失败、start 预检未过、cleanup 未批准/窗口未到，走这只全局口袋。口试要分清「报告里的红灯」和「请求直接 500 口袋」。不要发明一个本课没有的 `R.fail(error.name())` 合同。

### 图、表或文本图

**图题 / caption：** 宏观五步与两扇观察窗，以及三只柜子。alt：dry-run 不写库；start 复制后 CAS sys_oss.service；rollback 只拨指针；cleanup 删来源对象；batch/items 按 batchId 读两张迁移表。

```text
 MigrationRequest { ossIds, targetConfigKey }
        │
        ├─ POST /dry-run ──► DryRunReport          ┌── 零 INSERT / 零 copy / 零 delete
        │
        └─ POST /start ──► 再 dryRun
                 │ ready=false → 抛 INVALID_REQUEST
                 │ ready=true
                 v
        sys_oss_migration_batch (RUNNING)
        sys_oss_migration_item  (PENDING/PREFLIGHT)
                 │
                 v  process() 每件，当前 HTTP 线程
        PRIVATE bucket ──copy+verify──► PUBLIC_READ bucket
                 │
                 v
        CAS sys_oss.service  source → target     （delete_state 必须 ACTIVE）
                 │
                 v
        匿名 Range GET 公共 URL （200/206）
           失败 ──CAS 回 source──► item FAILED (ACCESS_VERIFIED)
           成功 ──► CLEANUP_ELIGIBLE  （+ cleanupDelay）
                 │
        GET /{id} 与 GET /{id}/items 只读上面两张表
                 │
        POST /retry     仅 FAILED 且 lastErrorStage≠COMPLETED
        POST /rollback  拨 service 回 source；两桶对象都留
        POST /cleanup   approved + 窗口到 → delete 源对象 → COMPLETED
```

**文字等价物：** 上半是预检与开工：同一份 `MigrationRequest`，dry-run 只出报告，start 在报告全绿后才写批次并同步处理。中段是字节与指针分离：先在目标桶得到校验过的副本，再 CAS 改 `sys_oss.service`，再以无过期公共 URL 做匿名探测；探测失败必须把指针拨回来源。下半是两扇只读窗和三把扳手：retry 重走 process，rollback 只动指针，cleanup 才删源且要第二张批准加时间窗。

**图的边界：** 不画 `SysOssController.remove`（那是对象删除，OBJ-26）。不画直传 Ticket。不把 `dry_run` 列画成 HTTP dry-run 的写入点。不保证 `start` 返回后批次是 COMPLETED——正常成功是 CLEANUP_ELIGIBLE。不把全局异常口袋画成「返回了 OssMigrationError」。

**图题 / caption：** 一件对象的阶段梯子与失败落点。alt：PREFLIGHT 到 CLEANUP_ELIGIBLE 的正向台阶；失败落在 FAILED 并记下 lastErrorStage。

```text
PREFLIGHT ──claim──► COPIED ──transfer──► CONTENT_VERIFIED
                                              │
                                              ▼
                                      SERVICE_SWITCHED
                                              │
                                              ▼
                                      ACCESS_VERIFIED
                                              │
                                              ▼
                                      CLEANUP_ELIGIBLE ──(批准+窗口)──► COMPLETED
                                              │
                         rollback（非终态）──► ROLLED_BACK

失败：status=FAILED，lastErrorStage=当时那一格
  CONTENT_MISMATCH          → CONTENT_VERIFIED
  SERVICE_DRIFT（CAS 前/中） → SERVICE_SWITCHED
  ACCESS_VERIFICATION_FAILED → ACCESS_VERIFIED（并尝试拨回 service）
  CLEANUP_FAILED             → COMPLETED（只能再 cleanup，不能 retry 重搬）
```

**文字等价物：** 阶段是梯子，状态是灯。灯在 CLEANUP_ELIGIBLE 时梯子已经走过访问验收，只差扔原箱。灯在 FAILED 时看 `lastErrorStage` 才知道卡在哪一格。COMPLETED 既是「源已删」的成功终态，也是「删源失败」时记下的失败格——所以 retry 故意跳过这一格。

**图的边界：** 不要把 PENDING/RUNNING 说成业务终态；RUNNING 是 claim 的占位。不要把批次 FAILED 理解成每一件都 FAILED。

### 正例、反例与边界

**正例 1：** 数方法。`SysOssMigrationController` 声明方法七个：`batch, items, dryRun, start, retry, rollback, cleanup`。HTTP 测试 `containsExactlyInAnyOrderEntriesOf` 这七对。无 PUT/DELETE。

**正例 2：** 根路径。`@RequestMapping("/resource/oss/migrations")`。对象清单是 `/resource/oss`。两块牌子。

**正例 3：** dry-run 零突变。单元测试在 ready=true 时仍 `batches`/`items` 空，`copyCalls=0`，`deleteCalls=0`。

**正例 4：** start 成功后 `sys_oss.service` 变目标，明细 `CLEANUP_ELIGIBLE`，`cleanupEligibleTime = NOW + delay`，`accessVerifier.verifiedOssIds` 含该 ossId，批次 `successCount=1`。

**正例 5：** 访问失败拨回。`service` 回到 `"private"`，`lastErrorStage=ACCESS_VERIFIED`，`errorMessage` 不含 secret/http。retry 后 service 再变 public，`retryCount=1`，物理 copy 仍一次。

**正例 6：** 复制过程中 service 被改成 `"other"`。批次 `failedCount=1`，`lastErrorStage=SERVICE_SWITCHED`，卡片停在 `"other"`，本课不擅自改第三份配置。

**正例 7：** `deleteState=PENDING` 的对象 dry-run `ready=false`，reason `INVALID_STATE`，零 copy。

**正例 8：** `CONTENT_MISMATCH` 记在 `CONTENT_VERIFIED`，`sys_oss.service` 仍是来源。

**正例 9：** cleanup 未批准 / 窗口未到都抛错且 `deleteCalls=0`。随后 rollback：service 回来源，明细 `ROLLED_BACK`，仍零 delete。

**正例 10：** 基座 DDL：`sys_oss_migration_batch` 约 547 行，`sys_oss_migration_item` 约 569 行。明细唯一键 `uk_sys_oss_migration_item_batch_oss`。

**正例 11：** CAS SQL：`update sys_oss set service=#{targetService} where oss_id=? and service=#{expectedService} and delete_state='ACTIVE'`。

**正例 12：** integration（有隔离 MySQL+双 Bucket 才跑）：cleanup 后源 head 失败、目标仍在；rollback 后两桶对象都在，私有匿名 403，公共匿名 200 发生在迁移成功未清理时。

**反例 1：** 「还有 `GET /resource/oss/migrations` 列表。」没有。必须带 `{batchId}`。

**反例 2：** 「dry-run 会插一条 `dry_run='Y'` 的批次。」`dryRun()` 不调 `createBatch`。

**反例 3：** 「start 把状态直接打成 COMPLETED。」成功件是 `CLEANUP_ELIGIBLE`。COMPLETED 是源删掉之后。

**反例 4：** 「rollback 会把橱窗里的副本删掉。」integration 与单元测试都要求目标仍在、`deleteCalls=0`。

**反例 5：** 「cleanup 是 execute 权限。」注解是 `system:ossMigration:cleanup`。rollback 也不是 execute。

**反例 6：** 「来源配置由请求体传入。」`MigrationRequest` 只有 `ossIds` 与 `targetConfigKey`。

**反例 7：** 「对象键是 `originalName` 或 URL。」createItem 用 `oss.getFileName()`。

**反例 8：** 「可以 PRIVATE→PRIVATE 或 PUBLIC→PRIVATE。」`requireRoute` 钉死目标 PUBLIC_READ、来源 PRIVATE。

**反例 9：** 「菜单有文件管理，所以超管页面已有迁移按钮。」DML 没有 `system:ossMigration:*` 种子。权限串只活在注解和 OpenAPI 描述里。

**反例 10：** 「这是 layered，UseCase 在 `usecase/oss`。」`wta-system` 无这条目录。迁移包在 `oss/migration`。

**反例 11：** 「`retry` 能修好 cleanup 失败。」`lastErrorStage==COMPLETED` 被跳过。要再 POST cleanup。

**反例 12：** 「失败时 HTTP 返回 `{ error: CONTENT_MISMATCH }`。」没有这种 Handler。整单抛错进未知异常口袋。逐条预检失败才把枚举名放进 `PreflightItem.reason`。

**反例 13：** 「目标已存在就覆盖。」`TARGET_CONFLICT` 直接失败。相同内容才视为可跳过 copy。

**反例 14：** 「`SysOssController.remove` 等于本课 cleanup。」remove 是对象管理删除（OBJ-26）；cleanup 只删**来源配置**上、迁移明细记下的那个 key，且要批准+窗口。

**反例 15：** 「`batch` 视图能看见 `dryRun` 字段。」`BatchView` 没有这个分量。

**边界：**

- API-005：五条变更窗是 POST + `@Log`，与规范同向。GET 两扇只读。`isSaveRequestData=false` 避免把 ossId 清单写进操作日志。
- `validateRequest` 拒绝重复 id；Bean 校验还要求列表非空、元素非 null、目标非空白。两层都要口述，不要只背一层。
- `start` 同步。`maxBatchSize` 默认 100 是安全阀，不是分页。
- `claim` 失败静默 return。并发双 retry 可能只有一个真的 process；内存里 +1 的 `retryCount` 若尚未 `saveItem` 会丢掉。
- 批次 FAILED 时仍可能有 successCount>0。cleanup 在这种混合态会 `INVALID_STATE`，要先处理失败件（retry 或让它们别挡着）。
- 全部 ROLLED_BACK 后 `activeConfigKeys()` 为空，readiness 不再因本批占用那两个 configKey。
- `OssFactory.instance(service)` 实际策略再校验一次；快照是 SERVING 但客户端配置已漂，仍可能 `ACCESS_POLICY_MISMATCH`。
- 公共验收走真实 HTTP（默认跟随重定向、5 秒超时、`Range: bytes=0-0`）。域名/CDN 不通会变成访问失败并拨回指针，即使字节已经在目标桶。
- 本课 Clock 默认 UTC。窗口比较用 `Instant`，不是业务时区墙钟。
- 教学正文不是生产授权。运维手册写明：启动批次、rollback、删源都要环境负责人另批。

## 变式与迁移

- **变式 A：只想知道这批 ossId 能不能搬。** 走 `POST /dry-run`。看 `ready` 和每行 `reason`。不要 start「试试看」，也不要去 `SysOssController.list` 用肉眼代替预检。

- **变式 B：预检全绿，真正搬家。** `POST /start`，收下 `Long` 批次号，立刻 `GET /{id}` 与 `GET /{id}/items`。成功灯是 `CLEANUP_ELIGIBLE`，不是 COMPLETED。

- **变式 C：访问验收红了。** 先看明细 `lastErrorStage=ACCESS_VERIFIED` 且 `sys_oss.service` 是否已回 PRIVATE。修公共域名/Policy 后再 `retry`。不要先 cleanup。

- **变式 D：复制后内容对不上。** `CONTENT_VERIFIED` + `CONTENT_MISMATCH`。卡片应仍在来源。查目标是否早有不同内容（冲突）或校验上限不够。

- **变式 E：搬家后反悔，观察期还没到。** `rollback`。卡片回来源。橱窗副本会留下，需要运维另开清理，本课没有「删目标」窗。

- **变式 F：观察期过了，校长签字删库房原箱。** `cleanup` body `{ "approved": true }`。`approved:false` 或缺字段都会 `CLEANUP_NOT_APPROVED`。删失败再 cleanup，不要 retry。

- **变式 G：产品要「任意两桶互迁」。** 今天硬编码 PRIVATE→PUBLIC_READ。改合同是新课，不是把 `requireRoute` 假装成通用。

- **变式 H：产品要迁移列表页。** 后端没有 list-all。前端 OpenAPI 也没有。先补 HTTP 合同，再谈 web-domain。

- **迁移口诀：** 先数七扇窗四颗权限 → 分清报告/工单/指针/源对象 → dry-run 零写入、start 同步搬、成功停在可清理 → retry 不修清理失败、rollback 不删字节、cleanup 要批准加窗口 → 看批次必须已有 batchId。跳步会出现「把 dry-run 当成已搬家」「把可清理说成已完成」「用 rollback 当删桶」。

## 常见误区

1. **「OBJ-28 只有五步，batch/items 不算。」** 矩阵与 chain 点名七个方法。五步是 OBJ 句子里的动手名单；两扇观察窗同课必须能口述。
2. **「`/resource/oss/migrations` 挂在 `SysOssController`。」** 另一份类、另一块牌子。
3. **「dry-run 会写 `sys_oss_migration_batch.dry_run`。」** HTTP 预检零 INSERT。
4. **「start 异步投递 Job。」** `process` 在请求线程。
5. **「CLEANUP_ELIGIBLE 等于源已删除。」** 源还在。COMPLETED 才是删源成功。
6. **「rollback = 反向 copy 回去再删目标。」** 只 CAS 指针；两份字节都可能还在。
7. **「cleanup 与 retry 都能救删源失败。」** 只有再 cleanup。retry 跳过 `lastErrorStage==COMPLETED`。
8. **「请求体带 sourceConfigKey。」** 没有这个字段。
9. **「目标已存在就覆盖。」** 冲突失败。相同内容才跳过 copy。
10. **「PENDING 删除中的对象也能搬。」** 预检 `INVALID_STATE`。
11. **「菜单种子已有 ossMigration 按钮。」** 2026-09-16 的 `50-cde-base-dml.sql` 没有这些 perms。
12. **「前端 `createOssUploadClient` 会调 start。」** 那是直传 Ticket（OBJ-29）。本课七条路径只出现在生成的 OpenAPI。
13. **「wta-system 迁移应按 layered 加 UseCase。」** 登记表 classic；本课不发动重构。
14. **「抛 `OssMigrationException` 时响应体是枚举名。」** 全局口袋返回未知异常编号。枚举名在明细/预检行里。
15. **「successCount>0 表示批次 COMPLETED。」** 有 FAILED 则批次 FAILED；全绿未清理则 CLEANUP_ELIGIBLE。
16. **「ETag 一律可信。」** 含 `-` 的分片 ETag 不走快捷比较，可能改走 SHA-256 或直接超限失败。

## 非评分暂停

打开磁盘，不要凭记忆默写路径。不要改文件。没有标准答案栏。

1. 打开 `SysOssMigrationController.java`。圈类上 `/resource/oss/migrations`。五指点 POST 五步，两指点 GET 两扇。确认没有第八个映射。
2. 圈四颗 `@SaCheckPermission`：list / execute / rollback / cleanup。圈五处 `@Log` 的 `isSaveRequestData = false` 与 `@RepeatSubmit`。
3. 打开 `OssStorageMigrationService.dryRun`。确认没有 `createBatch` / `transferAndVerify` / `delete`。再打开 `start`，确认它先 `dryRun` 再 `createBatch`。
4. 在 `process` 里按顺序圈：`claim` → `COPIED` → `transferAndVerify` → `compareAndSetService` → `verifyPublic` → `CLEANUP_ELIGIBLE`。圈访问失败那次拨回 CAS。
5. 在 `retry` 圈 `lastErrorStage != COMPLETED`。在 `rollback` 圈 `terminal()` 与「没有 delete」。在 `cleanup` 圈 `approved`、窗口、`objectStore.delete(source, …)`。
6. 打开 `SysOssMapper.xml` 的 `compareAndSetService`，圈 `delete_state = 'ACTIVE'`。打开 `10-cde-base-ddl.sql` 两张迁移表。在 `50-cde-base-dml.sql` 搜 `ossMigration`，看搜不到。

## 总结、词汇表与下一步

- **七扇窗：** dry-run / start / retry / rollback / cleanup 动手；batch / items 只看。矩阵 (a) 该行与磁盘同名。
- **四颗权限：** list、execute、rollback、cleanup。删源不是 execute。
- **三只柜子：** 目录指针 `sys_oss.service`；搬家账本两张表；两个 Bucket 里的字节。rollback 动第一只；cleanup 动来源那只桶；dry-run 三只都不动。
- **成功停在可清理：** 要第二张批准加时间窗才 COMPLETED。
- **失败看格子：** `lastErrorStage` + 枚举名。HTTP 整单抛错进未知异常口袋。
- **菜单缺口：** 种子无 `system:ossMigration:*`。口试按注解，不按文件管理菜单。

词汇表：`SysOssMigrationController` / `OssStorageMigrationService` / `MigrationRequest` / `CleanupRequest` / `DryRunReport` / `BatchView` / `ItemView` / `OssMigrationStage` / `OssMigrationStatus` / `OssMigrationError` / `claim` / `compareAndSetService` / `CLEANUP_ELIGIBLE` / `verifyPublic` / classic。

下一步：对象清单与删除是 OBJ-26 的 `SysOssController`；直传 Ticket 是 OBJ-27；浏览器拿 Ticket 是 OBJ-29。配置切换副作用是 OBJ-25。本课结束仍不是掌握证明；要练习请之后主动激活 Homework。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-004 | `backend/wta-modules/**` Controller | 业务模块公开入口；本课类在 `wta-system` 的 `controller/system` | `SysOssMigrationController.java` | 2026-09-16 |
| S-008 | `.agents/skills/engineering-standards/references/project/03-backend-module-modes.md` | `wta-system` = classic；不按 layered 口述 | 登记表 classic 行 | 2026-09-16 |
| S-010 | `release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql`、`50-cde-base-dml.sql` | 表 `sys_oss_migration_batch` / `sys_oss_migration_item`；菜单有 oss/ossConfig 无 ossMigration | DDL 约 547–601 行；DML 文件管理段 | 2026-09-16 |
| S-L028-01 | `SysOssMigrationController.java` | 七扇窗；根路径 `/resource/oss/migrations`；四颗权限；POST+`@Log`+`@RepeatSubmit`；GET 两扇无 `@Log`；不继承 `BaseController` | 类与七个方法 | 2026-09-16 |
| S-L028-02 | `OssStorageMigrationService.java`；`OssMigrationContracts.java` | dry-run 零写入；start 先预检再同步 process；retry/rollback/cleanup 分支；`refreshBatch` 口径；`validateRequest` / `requireRoute` | 各公开方法与 private `process` | 2026-09-16 |
| S-L028-03 | `OssMigrationStage.java`；`OssMigrationStatus.java`；`OssMigrationError.java`；`OssMigrationException.java` | 阶段 / 状态 / 15 个错误名；`terminal()`；异常只是 RuntimeException | 同包枚举与异常 | 2026-09-16 |
| S-L028-04 | `MybatisOssMigrationStore.java`；`SysOssMigrationItemMapper.java`；`SysOssMapper.xml`；`SysOss.java` | createBatch/Item；claim SQL；CAS `delete_state='ACTIVE'`；objectKey=`fileName`；活动 configKey | store / mapper / XML `compareAndSetService` | 2026-09-16 |
| S-L028-05 | `DefaultOssMigrationObjectStore.java`；`DefaultOssMigrationAccessVerifier.java`；`OssStorageMigrationProperties.java` | copy+matches；不覆盖冲突；可信 ETag；SHA-256 上限；公共 Range GET 200/206；delay/batch/verify 安全范围 | `transferAndVerify` / `verifyPublic` / `afterPropertiesSet` | 2026-09-16 |
| S-L028-06 | `OssMigrationHttpContractUnitTest.java`；`OssReleaseContractionUnitTest.java`；`OssStorageMigrationServiceUnitTest.java`；`OssStorageMigrationIntegrationTest.java` | 七路由；POST-only 命令；dry-run 零突变；拨回+幂等 retry；窗口与批准；双桶闭环 | `backend/wta-admin/src/test/java/org/namewta/test/oss/` | 2026-09-16 |
| S-L028-07 | `GlobalExceptionHandler.java`；`RepeatSubmit.java`；`AccessPolicy.java` | 未知运行时口袋；默认 5s 防重；仅 PRIVATE/PUBLIC_READ | `handleRuntimeException`；注解 default；枚举两个值 | 2026-09-16 |
| S-L028-08 | `docs/oss-public-private-operations.md`；`frontend/packages/api-contracts/generated/openapi.ts`（仅边界） | 手册：dry-run 零写入、cleanup 另批；OpenAPI 有七条路径与权限描述，无前端工厂 | 手册 §7；openapi.ts `/resource/oss/migrations*` | 2026-09-16 |
