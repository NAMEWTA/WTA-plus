# Nacos / runtime mapping (T-08) — hard-cut, no dual-read

**Strategy:** one-shot cut in a release window. Shipped runtime reads **only** the new data-id. Rollback = old build + old data-id.

| Kind | Old | New | Compat | Status |
|---|---|---|---|---|
| Nacos data-id | `ruoyi-namewta.yml` | `wta-namewta.yml` | hard-cut window | LANDED in `NacosConfigConstants.DEFAULT_DATA_ID` and `application.yml` |
| Nacos group (owned product) | `DEFAULT_GROUP` | `DEFAULT_GROUP` | — | KEEP |
| spring.application.name (local yaml) | `RuoYi-Vue-Plus` | `WTA-Plus` | — | LANDED |
| spring.application.name (compose product) | `namewta-admin` | `namewta-admin` | — | KEEP Docker product id |
| Registration / discovery | `${spring.application.name}` | same, compose `namewta-*` | — | KEEP compose services |
| Gateway `lb://` | none in tree | none | — | n/a |
| Shared config ref | `ruoyi-namewta.yml` | `wta-namewta.yml` | hard-cut | LANDED; no dual-read loader |
| Profile config filename | `application-*.yml` | owned tokens renamed | — | LANDED |
| Container workdir | `/ruoyi/server` etc. | `/wta/server` etc. | — | LANDED |
| Compose service name | `namewta-*` | `namewta-*` | — | KEEP |
| Default login (seed) | (upstream `ruoyi`; this fork seeds `admin`) | seed stays `admin`; existing-DB `ruoyi`→`wta` | migrate T-17 | LANDED scripts |
| Monitor username | `ruoyi` | `wta` | — | LANDED pom profiles |
| SnailJob group | `ruoyi_group` | `wta_group` | — | LANDED |

## Runtime dual-read

**Forbidden.** `NacosConfigSettings` has a single `dataId`. Client `getConfigAndSignListener(settings.dataId(), …)` only. No `oldDataId` / fallback list.

## Release-window checklist (cut traffic only when all true)

| # | Check | Status |
|---|---|---|
| a | Mapping table frozen; copy Nacos content from `ruoyi-namewta.yml` → `wta-namewta.yml` and verify | **ops at window** — table landed |
| b | Application pulls **only** `wta-namewta.yml` | landed in shipped config |
| c | docs / compose / seeds reference new id | compose still `namewta-*` services; data-id new |
| d | Rollback drill: old build + old data-id | procedure below |
| e | No runtime dual-read on trunk | landed (single dataId) |

## Cutover steps

1. In Nacos console, copy `ruoyi-namewta.yml` (DEFAULT_GROUP) to `wta-namewta.yml` with the same content; verify YAML.
2. Deploy the WTA-plus build (DEFAULT_DATA_ID = `wta-namewta.yml`).
3. Confirm instances load overlay from the new id only.
4. Do not leave a dual-read adapter in the app.

## Rollback

1. Redeploy the **pre-rename** artifact (legacy `ruoyi-namewta.yml`).
2. Point Nacos consumers at `ruoyi-namewta.yml`.
3. Do **not** add dual-read to “fix” a failed window.

## Failure stop

If new data-id is empty/wrong, stop the window and roll back. Do not ship a compatibility reader.
