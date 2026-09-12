# OSS 公共桶与私有桶发布运维手册

本文用于发布和运维 `PUBLIC_READ` 与 `PRIVATE` 物理双 Bucket。它不授权生产 SQL、Provider Policy、DNS/CDN、正式迁移、流量启用、回滚、源对象清理或部署；每项生产动作都需要环境负责人单独批准。

## 1. 安全不变量

- `PUBLIC_READ` 仅允许匿名 `GET/HEAD`，匿名 `PUT/POST/DELETE` 必须拒绝。
- `PRIVATE` 原始对象 URL 的匿名 `GET/HEAD` 必须拒绝，只能在业务授权后返回短时签名 URL。
- `sys_oss.service` 是对象存储位置和访问类型的唯一来源；业务表只保存 `ossId`，不保存 URL。
- 客户端只能提交 upload policy、文件元数据和指纹，不能提交 configKey、Bucket、访问类型或 TTL。
- 历史对象升级后保持 `PRIVATE`。公共访问只能来自新公共策略或审核后的显式迁移。
- 应用只诊断 Provider Policy，不创建 Bucket、不修改 Policy、不创建公共探测对象。
- 公共配置不能成为默认配置；生产公共配置必须使用可公开访问的 `domainUrl`。
- 管理列表不得批量生成签名 URL；下载必须逐对象通过有权限的 access-url 接口解析。

任一不变量失败时结论为 `NO RELEASE`，先将受影响 configKey 置为不可服务并停止新上传和迁移。

## 2. 发布前人工批准点

| 动作 | 必需批准人 | 本次本地 Gate 是否授权 |
|---|---|---|
| 创建 Bucket、配置公共只读 Policy | Provider/安全负责人 | 否 |
| 配置 DNS、CDN、TLS 与公共 domainUrl | 网络/平台负责人 | 否 |
| 备份并执行生产 DDL/DML | DBA | 否 |
| 部署后端与前端 | 发布负责人 | 否 |
| 启用公共 upload policy 或流量 | 产品 + 安全负责人 | 否 |
| 启动正式迁移批次 | 数据 owner + 发布负责人 | 否 |
| 执行迁移 rollback | 数据 owner + 值班负责人 | 否 |
| 删除源对象 | 数据 owner + 安全负责人 | 否 |

批准记录必须包含环境、configKey、Bucket、domainUrl、变更窗口、回滚 owner 和观察时长。不得在工单、日志或 Evidence 中记录 SecretKey 或完整签名 URL。

## 3. Provider 与域名预检

1. 由 Provider 管理员创建独立 PRIVATE 与 PUBLIC_READ Bucket，开启服务端加密、版本控制或组织要求的等效保护。
2. PRIVATE Bucket 不配置匿名读取。PUBLIC_READ Policy 只授予对象 `GetObject`，不得授予 ListBucket 或任何写/删除操作。
3. 使用不带凭据的 HTTP 客户端验证：公共对象 `GET/HEAD=200/206`，私有对象 `GET/HEAD=403`，两个 Bucket 的匿名写均为 `403`。
4. 保存 Policy 的脱敏摘要或版本号。应用 readiness 前后摘要必须一致，证明应用没有修改 Policy。
5. 验证公共 domainUrl 的 DNS、TLS、CDN 回源、对象路径编码、缓存键和 Range 请求；domainUrl 不得指向仅内网可达地址。
6. Provider 不支持安全匿名能力诊断时，该 configKey 保持 `UNVERIFIED/NOT_SERVING`，不得用人工假设绕过。

## 4. 数据库与配置顺序

1. DBA 对 `sys_oss_config`、`sys_oss`、`sys_oss_ref` 和迁移表做可恢复备份，记录行数与校验摘要。
2. 全新库在隔离环境按顺序执行 `release-artifacts/docker/infrastructure/mysql/init/` 六份基座；已有库指定源/目标 Git Tag，备份后生成并评审差异 SQL，再申请生产执行。
3. 检查访问类型物理编码仅为 `0=PRIVATE`、`2=PUBLIC_READ`。旧 `0/1/2` 和未知值在升级时都必须先按 PRIVATE 解释。
4. 先配置 PRIVATE configKey 并保持默认；再配置非默认 PUBLIC_READ configKey。存在对象引用的配置不得原地修改 Bucket 或访问类型。
5. 后端以公共策略默认关闭的配置部署。readiness 必须同时确认所有 upload policy、存量 service 和活动迁移 configKey。
6. 确认 `oss.readiness.refresh-interval` 严格小于 `max-snapshot-age`；默认分别为 1 分钟和 5 分钟。至少跨两个刷新周期观察 `checkedAt` 持续推进，禁止依赖重启或配置变更续期。
7. 后端健康且 OpenAPI 固定点无 drift 后再发布配对前端；旧前端不得恢复 custom/public-write 或旧 PUT/DELETE 配置命令。

## 5. 发布候选验证

后端候选使用隔离 MySQL、Redis 和双 Bucket MinIO 执行：

```bash
./mvnw -pl wta-admin -am -Dtest='*Oss*Test' -Dsurefire.failIfNoSpecifiedTests=false test
./mvnw -pl wta-admin -am -Dmaven.test.skip=false test
./mvnw -pl wta-admin -am -DskipTests package
```

为 required integration tests 提供一次性 `oss.minio.integration.*` 与 `oss.migration.mysql.integration.*` 系统属性。必须确认相关测试 `0 skipped`；没有外部参数而被跳过不能作为发布证据。

前端候选执行：

```bash
pnpm --filter @namewta/tooling-openapi openapi:check
pnpm architecture:check
pnpm lint
pnpm typecheck
pnpm test
pnpm build:prod
pnpm exec playwright test e2e/system-resources.spec.ts e2e/oss-config-access-policy.spec.ts
```

浏览器必须从本候选的独立 preview 端口加载，不得复用其他项目的同端口服务。验收下载时，PRIVATE 响应有到期时间，PUBLIC 响应 `expiresAt=null`；列表中的存量 URL 不得直接显示或使用。

## 6. 上传与访问启用

1. 先以内部 PRIVATE policy 做 SINGLE 和 MULTIPART 小文件验证，确认 Ticket 冻结 service/configKey/Bucket，完成后 `sys_oss.service` 正确。
2. 以受控 PUBLIC_READ policy 上传测试对象，确认稳定 domainUrl 可匿名 GET/HEAD、匿名写拒绝。
3. 对 PRIVATE 对象确认原始 URL 拒绝；签名 URL 在命名 TTL 内有效，过期后失效，重新授权产生新的有效 URL。
4. 删除测试对象前先确认无 `sys_oss_ref`；业务对象的删除继续遵守引用与 TEMP 生命周期。
5. 小比例启用公共上传后至少观察一个完整缓存 TTL 和签名 TTL，再申请扩大流量。

## 7. 存量迁移

1. 迁移前再次确认来源为 ACTIVE/PRIVATE、目标为 PUBLIC_READ，双方 readiness 为 SERVING，ossId 与引用基线已记录。
2. 先执行 dry-run。dry-run 不得创建批次、复制对象、切换 service 或删除源。
3. 从小批次开始：复制 -> size/checksum 或有界 SHA-256 校验 -> CAS 切换 `sys_oss.service` -> 公共访问验收。
4. 每批确认 ossId 和 `sys_oss_ref` 不变，目标冲突内容不被覆盖，失败项可按审计状态幂等重试。
5. 访问验收失败时停止后续批次并确认 service 恢复来源。复制但未切换的目标对象可保留供重试，不得提前删源。
6. 只有批次进入 cleanup eligible、观察窗口结束且再次获得独立批准后，才允许执行源清理。

## 8. 监控与告警

至少按 configKey、policy 和 Provider 监控：

- readiness 状态、原因、快照年龄、`checkedAt` 周期推进和所需 configKey 集合；
- upload init/sign/complete 失败率、Ticket 过期和落点不一致；
- access-url 解析失败、PUBLIC/PRIVATE 分类、签名到期失败和 Provider 4xx/5xx；
- 公共匿名 GET/HEAD 探测与匿名写拒绝探测；
- 迁移阶段、失败原因、重试次数、CAS 冲突、rollback 和 cleanup 状态；
- Provider 延迟、限流、容量、复制/校验耗时与 CDN 回源异常。

日志只能记录 ossId、configKey、阶段、稳定错误码和 traceId。禁止记录完整 URL 查询、Authorization、AccessKey、SecretKey、Policy 正文或对象内容。

## 9. 停止与恢复

- readiness 失败：立即停止该 configKey 的新上传和 URL 解析，保持默认 PRIVATE 配置，不自动修改 Provider。
- 公共域名或 Policy 异常：停止公共 policy 流量，修复外部状态后重新执行匿名矩阵与 readiness。
- 上传路由异常：停止对应 policy，保留 Ticket 和对象审计信息，禁止客户端改选 Bucket 规避。
- 迁移失败但未切换：保留源对象，修复后幂等重试。
- 迁移已切换但访问失败：停止批次，按审计台账恢复旧 service 并验证 PRIVATE 原始访问仍拒绝。
- 前端合同异常：后端保持可用但不发布前端，前向修复 OpenAPI/domain/UI，不恢复歧义策略。

回滚不会删除 additive schema，也不会自动删除已经复制的目标或源对象。任何生产 rollback 和对象清理都需要表中批准人重新确认。

## 10. 发布完成条件

- 后端、前端和聚合父仓候选 SHA/tree 与 Evidence 一致，工作树干净。
- 真实 MySQL、MinIO、HTTP 和 browser required tests 均实际执行且关键项没有 skipped。
- OpenAPI 无 drift，禁止能力扫描为零，公共/私有/上传/迁移/恢复矩阵全部通过。
- 无 active 或未处理 failed migration；源对象仍受独立 cleanup Gate 保护。
- 生产批准、监控、值班、回滚 owner 和观察窗口均已记录。

上述条件关闭的是发布候选 Gate，不等同于已经部署或已经启用生产公共流量。
