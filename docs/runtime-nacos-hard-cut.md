# Nacos hard-cut (shipped)

See change evidence `speculo/.speculo/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/evidence/runtime/nacos-hard-cut-mapping.md` for the full table.

Shipped runtime:

- `nacos.config.data-id` / `NacosConfigConstants.DEFAULT_DATA_ID` = `wta-namewta.yml`
- No dual-read of `ruoyi-namewta.yml`
- Docker product services remain `namewta-*`
- Container workdirs `/wta/...`

Existing-DB login migrate: `release-artifacts/docker/infrastructure/mysql/migrate/`.
