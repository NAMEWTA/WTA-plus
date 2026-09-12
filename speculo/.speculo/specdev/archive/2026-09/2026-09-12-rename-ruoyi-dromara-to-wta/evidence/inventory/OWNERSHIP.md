# Ownership inventory (T-02)

**Captured:** 2026-09-12 implementation start  
**Baseline SHAs (SOURCE-BASELINE, reconfirmed T-01):**

```text
docs:     cf9842660df2557439e13a3fca27cc2cac155210
backend:  60c9d31f9419e7d7560706a2b6a41ff44b41d551
frontend: d77b55651e5754a54508e51a79f536afbd2392b8
```

**Scan excludes:** `node_modules`, `target`, `.git`, `dist`, lockfile noise, `.flattened-pom.xml`.  
**Mutation target:** local WTA-plus prep tree only (not legacy remotes).

Classification: **KEEP** / **RENAME** / **REVIEW**. Only **RENAME** rows are auto-migrated.

## Path-pattern table

| Path pattern / class | Example | Class | Owner | Disposition |
|---|---|---|---|---|
| Maven `groupId` exactly `org.dromara` + artifact `ruoyi-*` | `ruoyi-vue-plus-namewta/pom.xml` `<groupId>org.dromara</groupId>` `<artifactId>ruoyi-vue-plus</artifactId>` | RENAME | first-party | `org.namewta` + `wta-vue-plus` / `wta-*` |
| Java/Kotlin sources under `**/java/org/dromara/{common,system,admin,web,notify,workflow,profile,demo,job,ai,third,test,monitor,snailjob,snailai}/**` | `org.dromara.system.mapper` | RENAME | first-party | `org.namewta.*`; move `org/dromara` → `org/namewta` |
| MyBatis `namespace` / `typeAliasesPackage` / `mapperPackage` `org.dromara.**` | `application.yml` `mapperPackage: org.dromara.**.mapper` | RENAME | first-party | `org.namewta.**` |
| `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` FQCN `org.dromara.*` | `ruoyi-common-core/.../AutoConfiguration.imports` | RENAME | first-party | `org.namewta.*` |
| Module dirs / artifactIds `ruoyi-X` | `ruoyi-admin`, `ruoyi-common-core`, `ruoyi-system` | RENAME | first-party | prefix SWAP `wta-X` (not bare `X`) |
| Backend subtree dir `ruoyi-vue-plus-namewta` | aggregate layout | RENAME | first-party | `wta-vue-plus-namewta` |
| Nacos owned data-id | `data-id: ruoyi-namewta.yml`; `NacosConfigConstants.DEFAULT_DATA_ID` | RENAME | first-party | `wta-namewta.yml`; **hard-cut, no dual-read** |
| `spring.application.name` owned | `RuoYi-Vue-Plus`; `ruoyi-monitor-admin` | RENAME | first-party | `WTA-Plus` / `wta-monitor-admin` |
| Docker **product** compose service names | `namewta-server1`, `namewta-nginx-lb` | KEEP | first-party brand already namewta | do **not** rename to `wta-*` |
| Docker / container **paths** `/ruoyi/...` | `WORKDIR /ruoyi/server`; alloy `/var/log/ruoyi/...` | RENAME | first-party | `/wta/...` |
| Docker build context dirs `images/ruoyi-*` | `release-artifacts/docker/backend/images/ruoyi-admin` | RENAME | first-party | `images/wta-*` |
| Frontend npm scope `@namewta/*` | `@namewta/admin-web` | KEEP | first-party | not `@wta` |
| Frontend workspace `name` | `plus-ui-namewta/package.json` `"ruoyi-vue-plus"` | RENAME | first-party | `wta-vue-plus` |
| Admin titles `RuoYi-Vue-Plus` | `.env*` `VITE_APP_TITLE` | RENAME | first-party | `WTA-Plus` / WTA |
| `repository.url` / pom `<url>` pointing at gitee/upstream | `gitee.com/dromara/RuoYi-Vue-Plus`; `gitee.com/JavaLionLi/plus-ui.git` | RENAME | first-party | `https://github.com/NAMEWTA/WTA-plus` (ADR-012, no upstream dep) |
| Default/demo **login** user in seed | `sys_user` currently `admin`/`test` (no `ruoyi` login row in 10-ruoyi-base.sql) | REVIEW→RENAME (migrate) | first-party | new-install stays `admin`; **existing-DB** `user_name='ruoyi'` → `wta` (T-17) |
| Monitor / MQTT example username `ruoyi` | `pom.xml` `monitor.username`; `mqtt.client.username` | RENAME | first-party | `wta` |
| OSS seed bucket/access-key `ruoyi` | `sys_oss_config` minio `'ruoyi'` | RENAME | first-party | `wta` / `wta-1240000000` |
| SQL file `10-ruoyi-base.sql` | release-artifacts mysql init | RENAME | first-party | `10-wta-base.sql` |
| Skill dirs `ruoyi-module-guide`, `ruoyi-common-modules-guide` | `.agents/skills/` | RENAME | first-party | `wta-module-guide` / `wta-common-modules-guide` |
| Architecture map `admin: 'ruoyi-admin'` | `plus-ui-namewta/tooling/architecture/test/domain-layout.test.mjs` | RENAME | first-party | `wta-admin` etc. |
| Third-party Maven `org.dromara.<product>` | sms4j / warm / easy-es / mica-mqtt | KEEP | third-party | see KEEP inventory; never `org.namewta.*` |
| Java import `org.dromara.sms4j.**` / `warm.**` / `easyes.**` / `mica.mqtt.**` | workflow handlers, sms resolver | KEEP | third-party | byte-level |
| `com.aizuda.snailjob.**` / `com.aizuda.snail.ai.**` | extend servers | KEEP | third-party | out of dromara rename |
| License / NOTICE / “based on RuoYi-Vue-Plus” attribution | README lineage sentence | KEEP (optional one-liner) | attribution | allowed residual; not a runtime coord |
| OpenAPI `packages/api-contracts/openapi/revisions/**` | content-addressed historical snapshots | REVIEW | first-party historical | **do not rewrite** (hash identity); classify residual allowed |
| SpecDev change `2026-09-12-rename-ruoyi-dromara-to-wta` | speculo change id | KEEP | process | do not rewrite change identity |
| Legacy remote URLs in freeze evidence | `NAMEWTA/ruoyi-vue-plus-namewta.git` | KEEP | freeze docs | T-16 intent only |

## Auto-migrate allow-list (RENAME only)

- Owned `org.dromara` (not KEEP suffixes) → `org.namewta`
- Owned `ruoyi-` module/artifact/dir/file prefix → `wta-`
- Owned `/ruoyi` workdirs → `/wta`
- Owned Nacos data-id `ruoyi-namewta.yml` → `wta-namewta.yml`
- Owned brand `RuoYi`/`RuoYi-Vue-Plus` product titles → `WTA`/`WTA-Plus`
- Upstream product URLs in pom/package.json → `https://github.com/NAMEWTA/WTA-plus`

## Not auto-migrated

KEEP rows, REVIEW snapshots, speculo change identity, compose `namewta-*` service names, `@namewta` npm scope.
