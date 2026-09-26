# T-41 隔离验收驱动只读审查

审查对象：`/tmp/wta-t41/run-notify-inbox-paging-integration-v1.py` SHA-256 `81ecc9db1fc4ecbde4ae1332b90f753d499ca18e26ed3c361b3f9ac21eeef6b9`、`capture-live-openapi-v1.py` SHA-256 `e5ec0e5ada965c1a9df2fe007af3258627a32c1676c9643a3eac412aeff15b54`、`test_driver_safety_v1.py` SHA-256 `2252402d2b535e3549f01f31783b5067780fbe12f00474e29b6bb4c25c755bcb`，均与 `version-freeze-v1.json` 一致；被导入 T-38 helper SHA-256 `d068610f75a9c65cabf77d69d230884b33edd9c9f3d64c73c73296655066c31e` 亦一致。冻结清单是在 clean `d7d534cb03aa000d603a53c092c4bf1d48bb5cc3` 准备的，明确目标测试类当时不存在。当前实施工作树可能继续变化，本报告只评审冻结脚本，不把工作树当候选实现。本轮未运行 Docker、Maven、服务、HTTP 或离线测试，仅核对已有 `offline-synthetic-v1-final.json/.log`：命令 `python3 /tmp/wta-t41/test_driver_safety_v1.py -v`、exit 0、12 tests、0 failures/errors；这是脚本合成保护，不是 T-41 集成通过。

## 判定

**驱动可作为 Lead 固定候选后的隔离运行准备；真实鉴权覆盖仍待新测试类源码与真实运行证明。** 两脚本默认无 `--execute` 即参数错误退出 2；主驱动还要求精确 clean 40 位 HEAD，capture 同时要求精确 JAR SHA 与完整包构建证明。不能把已通过的 12 个合成检查、脚本存在、或自然红灯的 fresh XML 称为业务验收。

## 单类、fixture 与红灯

- 主驱动第 29–33、393–400 行只选 `NotifyInboxPagingIntegrationTest`，有 `notify.inbox.paging.integration=true`、独立 Surefire fork；第 156–204、414–434 行核精确 XML 类名、fresh mtime、正测试数、0 fail/error/skip 和 Maven exit 0。`surefire.failIfNoSpecifiedTests=false` 可容忍依赖模块无选中类，但目标 `wta-admin` 的精确 XML 必须存在。第 446–449 行在 fail/skip/Maven 失败时仍复制 fresh XML 至私有 run 目录并记 SHA；第 489–495 行仅全零且资源/源码清理成功才 `acceptance=true`。合成测试第 99–129 行覆盖 stale/fail/skip、Maven 失败和红 XML 保留。Lead 必须复核 red 的失败方法及断言文本是第 501 条旧行为，而非编译、启动、鉴权或 fixture 失败。
- 第 301–316 行的源预检是 **字符串启发式**：可证明类名、opt-in 注解与 `T41_MYSQL_PASSWORD` 直接接缝，或对 `NotifyAtomicResultIntegrationTest.open()` 的引用。当前 Atomic 第 88–148 行确实接入隔离 MySQL/Redis，但它手装 DAO/Redisson、mock `NotifyClient`，自身不执行 HTTP 登录或 Client 授权。预检不证明新类真的调用 fixture、更不证明真实 token→Controller→本人 JOIN。固定候选须人工核新测试的真实 HTTP 登录、A/B 身份与跨 Client/越权断言；实际 XML 方法名/计数和数据库观测也需逐项对应 AC-041。测试类当前在冻结点未存在，所以这不是已证缺陷，只是必须验证的接缝边界。

## 资源与秘密

- 主驱动使用随机 run ID、双 label `namewta.test.owner=T-41`/`namewta.test.run`、`docker ps -aq --no-trunc` 全 ID、移除前复核标签（第 75–94、257–298 行），MySQL/Redis 均 `--pull=never` 且随机 `127.0.0.1` 端口（第 142–149、379–390 行）；六份固定 SQL、103 表检验、随机 owned database/app user（第 24–28、355–377 行）。应用密码只经子进程环境传给 `T41_MYSQL_PASSWORD`/`T36_MYSQL_PASSWORD`，MySQL root 密码只在 0600 env-file 与容器内命令，不进 Maven argv。`/tmp/wta-t41` 及脚本现场权限 0700，记录/日志 0600；第 132–139、327–358、401–409 行给出边界。
- 第 219–254、450–488 行在 Maven leader 退出后仍检 PGID 成员、终止组；捕获匿名卷并核不存在、容器双 label 清理、端口关闭、源 HEAD/tree clean。容器标签漂移或发现失败会报错而不是按名称盲删；可能留下须人工检查的隔离资源，不能标通过。若 Docker 在返回完整 ID 前已创建容器，按 run label 仍能发现。生成密码会从 fresh XML/原始 Maven 日志替换，原始日志删除；若脱敏/清理失败，验收为 false。未来真实鉴权测试若生成额外 token/凭据，驱动不掌握其值，测试不得将其打印到 Surefire/日志；上线前审新测试输出与私有 evidence，不能把仅脱敏两个 DB 密码当作任意秘密的完整证明。
- capture 使用独立 `T-41-OPENAPI` owner/run label，第 112–142、393–404、440–487 行将 MySQL/Redis/MinIO 绑定随机 loopback 端口；MinIO 镜像按 digest 固定、MySQL/Redis 使用本地固定 tag 且 `--pull=never`。第 187–247 行的配置及 103 表/零待发 outbox/零外部 delivery/账号的启动前检查限制误呼供应商。凭据在 0600 env/config、Redis bind 配置或子进程环境，不在 Java argv；日志脱敏、私有配置与 file logs 删除。第 532–591 行在退出前检 PGID、全 ID 容器、匿名卷、端口、clean 源、JAR SHA 和 helper SHA；任何失败保持 `acceptance=false`。由 Docker inspect 临时可见的容器环境仅在 owned 生命周期内，不能把 Docker 管理权限等同秘密隔离。

## OpenAPI 证据与后续风险

capture 第 64–109、425–438 行绑定 clean full package 命令、源 HEAD/tree、JAR SHA/大小/mtime、成功 build log 与 full bundle；第 250–263 行从 127.0.0.1 的实际 full JAR 拉取有 16 MiB 上限的 HTTP 200 响应。第 337–348 行先以 0600 保存原字节 `source.json`/SHA，随后第 305–334 行才检查 OpenAPI 全文、旧 path/method/schema 无丢失及既有通知 retry/cancel；严格失败时仍保留原字节，但 exit 非零，不能把 raw capture 当合格 generate/check 来源。

T-41 将旧 `R<List<...>>` 换为分页与详情时，若旧专用 `RList` schema 因不再被使用而消失，现有“旧 schema 零丢失”门会如设计般拒绝。**这是未来固定候选的已知风险，当前不放松规则。** Lead 应在实际 direct-HTTP 原字节与新旧 schema 使用图明确后，单独审查有意迁移及 T-41 正向 `/notify/inbox` 分页、详情合同；不得提前删除 no-loss 断言或因脚本失败把旧快照拼成新来源。当前 capture 的正向断言仍是旧 retry/cancel 与 queuedCount，不足以单独证明新收件箱页/详情 API 已正确生成。

剩余未验证：新测试类/真实 HTTP 鉴权源码、自然红灯原因、MySQL/Redis 真实 one-class 数量与零 skip、full-JAR live HTTP、OpenAPI 迁移判断、浏览器 501 条/会话切换。上述均须由 Lead 在固定 clean 候选独占运行和记录；本报告不作通过声明。
