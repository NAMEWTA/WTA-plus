# wta-notify

- 入口：`controller/admin`、`controller/anonymous`；业务调用按 `UseCase -> Service -> DAO -> Mapper` 分层。
- 所有通知、公告、收件箱、Outbox 和监控数据由本模块拥有；跨模块仅依赖 `wta-api` 与 common SPI。
- DDL/DML：父仓库 `release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql` 与 `50-cde-base-dml.sql`。
- 测试（cwd=`backend/`）：`./mvnw -pl wta-modules/wta-notify -am test`。
