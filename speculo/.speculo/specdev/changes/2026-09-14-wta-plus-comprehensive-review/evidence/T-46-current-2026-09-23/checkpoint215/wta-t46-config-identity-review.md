# T46 active migration config identity — read-only design review

Fixed repository HEAD at read: `32473bf819ae2ce7ea30449f7dac502a50220794`; T46 product writer had an uncommitted migration diff, so this report bases configuration facts on unchanged `SysOssConfigServiceImpl`, `SysOssConfigMapper`, `SysOssConfig`, `OssClientConfig`, `DefaultOssClientImpl`, and the fixed migration item schema. No repository edit, build, test or service was run. This is a narrow closure of source/target physical identity while an item is nonterminal, not a redesign of all OSS configuration governance.

## Confirmed gap

`SysOssConfigMapper.countOssReferences` is only `select count(*) from sys_oss where service = #{configKey}` (`:20–21`). `SysOssConfigServiceImpl.updateByBo` locks the config row then calls `validateReferencedBoundary` (`:171–191,353–361`); its guard only pins configKey, bucketName and accessPolicy when a **current** `sys_oss` row points to that config. `deleteWithValidByIds` likewise checks only that count (`:229–250`). Once migration switches the object's current pointer from source to target, the source has no `sys_oss.service` reference even though a `CLEANUP_ELIGIBLE` or UNKNOWN item still names it. The source config can then be retargeted or deleted. `OssMigrationRequiredConfigContributor` includes nonterminal item keys for readiness diagnostics, but it is not a config-write veto.

This is directly relevant to T46's single DELETE and HEAD-only reconciliation: the item stores `sourceConfigKey`/`targetConfigKey` and objectKey, **not** immutable endpoint/bucket identity. Resolving the same source key to a different physical bucket can make HEAD 404 falsely look like completed cleanup, or send DELETE to a new bucket's same object key. A target config with no current `sys_oss` reference can similarly be changed/deleted between dry-run and creation of its first migration item.

## Which fields matter for this narrow guard

`OssClientConfig.formPropertiesBuilder` maps endpoint, bucketName, region, prefix, isHttps, domainUrl, credentials and accessPolicy (`:165–176`). `DefaultOssClientImpl` builds the **actual S3AsyncClient** from `endpointUrl` (endpoint + isHttps), region, path style inferred from endpoint, credentials and bucket supplied on requests (`:42–74`); migration HEAD/DELETE use the client's default bucket and the **persisted full objectKey**.

| Config field | T46 effect while item active | Minimal edit rule |
|---|---|---|
| `configKey`, `bucketName`, `endpoint`, `isHttps` | Changes identity or physical S3 address; endpoint also determines path-style inference. | Reject change while a nonterminal migration item names the key. |
| `region`, `accessPolicy` | Changes request signing/routing assumptions or the source-private/target-public migration contract. | Reject change while active item exists. |
| `accessKey`, `secretKey` | Credential rotation/authentication, not the stored object key or configured endpoint/bucket. Failed auth must remain UNKNOWN, not ABSENT. | Do not freeze ordinary credential rotation merely for T46; never log values. If a provider binds the same endpoint/bucket name to credentials' tenant, that deployment requires a stronger identity rule before automatic reconciliation. |
| `prefix` | Used to generate **new** keys; item already stores full objectKey, and migration HEAD/DELETE pass it directly. | No new T46 freeze needed. |
| `domainUrl` | Presigner/public URL host; `S3AsyncClient` HEAD/DELETE use endpointUrl instead. | No physical-delete freeze needed here; public URL effects remain existing config contract. |
| `status`, `remark`, `ext1` | Default selection/description/current unused extension, not this provider object's physical key. | No new T46 freeze needed; status-changing methods still matter for lock ordering if they touch several config rows. |

The narrower, nonterminal-item-specific freeze avoids silently changing the preexisting rule that permits some fields to change for an ordinary current `sys_oss` reference. Deletion of either active source or target config must be rejected even if no current object points to it.

## SQL and status lifetime

Add a small query to `SysOssConfigMapper` (or delegate to existing migration mapper while keeping the config-service call narrow):

```sql
select count(*)
from sys_oss_migration_item
where del_flag = '0'
  and status not in ('COMPLETED', 'ROLLED_BACK')
  and (source_config_key = #{configKey} or target_config_key = #{configKey})
```

Use `countOssReferences > 0 OR countActiveMigrationReferences > 0` for config delete. For update, retain the old current-reference check and add the physical-field comparison above **when activeMigrationReferences > 0**. This matches the existing migration `selectActiveConfigKeys` definition: `PENDING`, `RUNNING`, `CLEANUP_ELIGIBLE`, and **all FAILED**, including `FAILED/COMPLETED/CLEANUP_OUTCOME_UNKNOWN`, retain both source and target config references. `COMPLETED` and `ROLLED_BACK` alone release the migration reference; the current `sys_oss.service` still pins whichever config it points to. An ordinary FAILED item can therefore retain configs until explicit rollback/retry/repair. That overretention is a deliberate conservative limit, not a reason to silently release UNKNOWN after elapsed time.

## Transaction order without Object→Config inversion

1. On creation of each migration item, perform a **short, proxied** transaction: identify source and target config IDs, acquire both `sys_oss_config` rows in ascending **`oss_config_id`** order, recheck their configKey and physical-route fields against preflight, then acquire `sys_oss` object row, then relevant item rows (Config→Object→Item), recheck current service/ACTIVE/latest marker, insert PENDING item with exact-one row count, and commit. Do not hold config or object locks over copy, verification or provider I/O. If a config was deleted or changed after dry-run, abort/re-preflight before an item or pointer is claimed. Both configs must exist; a target with zero current-object references is not optional.
2. The item must be **committed before** source→target pointer change. Before that commit, `sys_oss.service=source` continuously protects source through the existing current-object check; the Config locks protect the otherwise-unreferenced target. After commit, the nonterminal item protects both through the new count. There is no source/target reference gap. Already-created items' cleanup/restore/process methods keep their established **Object→Item** order and never acquire Config locks, so they cannot invert to Object→Config.
3. Config update/delete operations already acquire a config row lock. For multi-ID delete, normalize IDs and lock **all** rows in ascending config ID before the *first* ordinary `countDefaultConfigs`/reference query; only then read counts and delete. Current code locks one ID, performs plain reads, then locks the next. Under MySQL REPEATABLE READ, that first plain read may establish a snapshot which remains stale after waiting for a migration creator on a later ID; an active target item could be missed. The config-row lock forces creator and editor to serialize, and placing the first consistent read after all locks gives the editor the creator's committed item when it wins second. If config delete wins first, creator recheck finds a missing row and inserts no item.
4. Use one canonical config-row order across operations, preferably primary-key ascending. `updateOssConfigStatus:277–287` and `updateByBo` when setting `status=Y` lock the selected config and then `clearOtherDefaultStatuses` may update the **old default** row. If migration creation locks those two in the reverse order, Config↔Config deadlock remains even though Object→Config inversion is avoided. Before a default switch, lock the old default and selected row in the same ascending-ID order (or prove this path cannot overlap and supply bounded deadlock retry); then perform the status update. `insertByBo(status=Y)` should be checked for the same multi-row order when reusing this guard. Do not add Config locks to already-created-item cleanup just to solve this.
5. Keep `@DsTxEventListener` postcommit cache refresh (`OssConfigChangeListener:24–30`); a rejected/rolled-back config edit must not publish a new physical client. Tests must exercise the actual postcommit client/DB view, not only the SQL count.

## Minimal evidence and write set

Necessary additional production paths are `SysOssConfigMapper.java` and `SysOssConfigServiceImpl.java`; migration item's short create boundary stays in the already registered migration root. Register those and affected config/migration tests before implementation. No schema, global lock or new state enum is required. Test with two physical MySQL connections: creator wins (target had zero current references; editor/delete waits then rejects), editor wins (creator waits then aborts on missing/changed target), source pointer switches but active item still vetoes source edit/delete, multi-ID delete with target on the later ID sees fresh count under RR, and FAILED UNKNOWN keeps both references until safe finalization. Add a default-switch overlap test to show no reverse Config row-lock cycle. Ordinary credential rotation/default status/remark changes should remain allowed if they do not change pinned route fields; HEAD auth failure remains UNKNOWN, not source absence.

This is a design recommendation for Lead's write-set decision. It does not certify the currently changing T46 implementation or claim any test has run.
