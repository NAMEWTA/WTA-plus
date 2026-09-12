# KEEP inventory (T-03)

**Baseline SHAs:** docs `cf9842660df2557439e13a3fca27cc2cac155210` / backend `60c9d31f9419e7d7560706a2b6a41ff44b41d551` / frontend `d77b55651e5754a54508e51a79f536afbd2392b8`  
**Authority:** ADR-003 + `evidence/THIRD-PARTY-KEEP.md`  
**Rule:** byte-level preserve Maven coordinates and Java imports. Zero-match of `org.dromara` is **not** AC.

## Locked KEEP products

| Product | Maven groupId | Runtime import prefix | Must remain after every wave |
|---|---|---|---|
| SMS4J | `org.dromara.sms4j` | `org.dromara.sms4j.` | yes |
| Warm-Flow | `org.dromara.warm` | `org.dromara.warm.` | yes |
| Easy-Es | `org.dromara.easy-es` | `org.dromara.easyes.` | yes |
| mica-mqtt | `org.dromara.mica-mqtt` | `org.dromara.mica.mqtt.` / `org.dromara.mica.` | yes |

## Detection (implementation)

KEEP iff:

1. Maven `groupId` is `org.dromara.<segment>` with segment in `{sms4j, warm, easy-es, mica-mqtt}`.
2. Java/XML text matches `org.dromara.(sms4j|warm|easyes|easy-es|mica)`.

Owned wrapper packages **do** rename (example `org.dromara.common.sms` → `org.namewta.common.sms`) while their **imports of** sms4j stay KEEP.

## Confusion pairs (must not invert)

| String | Class |
|---|---|
| `org.dromara.common.sms` | RENAME → `org.namewta.common.sms` |
| `org.dromara.sms4j.api.SmsBlend` | KEEP |
| `org.dromara.workflow` | RENAME |
| `org.dromara.warm.flow` | KEEP |

## Pre-rename occurrence proof (legacy checkout, excludes target/node_modules/.git)

These strings **must still exist** on the WTA-plus tree after rename:

- `org.dromara.sms4j` in `wta-vue-plus-namewta/pom.xml` and sms wrapper sources
- `org.dromara.warm` in root POM and `wta-workflow` handlers
- `org.dromara.easy-es` / `org.dromara.easyes` in root POM and Easy-Es config
- `org.dromara.mica-mqtt` / `org.dromara.mica.mqtt` in root POM and mqtt config

Regression command (prep tree):

```bash
rg -n 'org\.dromara\.(sms4j|warm|easyes|easy-es|mica)' --glob '!**/target/**' --glob '!**/node_modules/**'
```

Expected: hits remain. Failure: KEEP rewritten to `org.namewta.sms4j` (etc.) → roll back that wave.
