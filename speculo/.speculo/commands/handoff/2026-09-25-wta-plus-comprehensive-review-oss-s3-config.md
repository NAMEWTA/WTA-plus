# 交接：OSS 实现与 S3 配置要求

日期 2026-09-25。下一会话用于阅读当前 OSS/文件存储实现和 S3 配置约束，不改产品代码，不归档，不推送。

## 范围

变更 `2026-09-14-wta-plus-comprehensive-review` 仍 active。`main` 为 `5259e3b5`，比 `origin/main` 超前 255，工作区干净。50 票为 30 done / 20 cancelled / 0 ready。Windows 真机是 `user-waived/not-run`。

只引用这些工件，不抄正文：

- <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/.status.json</Path>
- <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/source.md</Path>
- <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/spec.md</Path>
- <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/worklog.md</Path>
- <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/handoff.md</Path>

该变更目录没有 `triage.md` 或 `publish.md`。持久说明已写入上面的 change `handoff.md` 开头。

## 已确认的实现

没有第二套文件存储。「文件存储服务类型无法找到」是 `OssFactory` 在 Redis 默认配置缺失时的错误，指 S3 兼容后端。业务保存 `ossId`。

- 配置在 `sys_oss_config`，对象在 `sys_oss`，引用在 `sys_oss_ref`。`sys_oss.service` 是对象所属配置键，切换默认桶不搬迁旧对象。
- 客户端在 <Path>backend/wta-common/wta-common-oss/</Path>，使用 AWS S3 SDK。默认指针是 Redis `sys_oss:default_config`。
- 浏览器直传是 `/resource/oss/uploads`，策略在 <Path>backend/wta-admin/src/main/resources/application.yml</Path> 的 `oss.direct-upload.policies`。服务端文件上传是 `SysOssServiceImpl.upload(File)`。
- 访问用 `resolveAccessUrl`：公共读无签名，私有短时签名。`presignDownload` 只用于私有对象。

配置入口是 <Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/service/impl/SysOssConfigServiceImpl.java</Path> 和 <Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/domain/bo/SysOssConfigBo.java</Path>。页面编码在 <Path>frontend/packages/domains/system/src/resource-service.ts</Path>。

## S3 配置要求

- 配置 key 2 到 20 个字符、全库唯一。启动只接受字母或数字开头，其余为字母、数字、点、下划线、连字符。
- accessKey、桶名、访问站点均为 2 到 100 个字符。secretKey 新建时同样要求，编辑留空沿用旧值。站点不写协议，由 `isHttps` 的 `Y/N` 决定。
- 访问类型只有 `0` PRIVATE 和 `2` PUBLIC_READ。恰好一个 `status=Y`，且必须私有。不能在编辑里取消当前默认，也不能删除默认配置。
- 空区域按 `us-east-1`。站点含 `aliyun`、`qcloud`、`qiniu`、`obs` 用虚拟主机风格，MinIO 及其他用路径风格。
- 公共读的可公开 `domainUrl` 是页面要求，服务端保存不强制。
- 已被对象引用后不能改配置 key、桶名、访问类型、站点、HTTPS 和区域。密钥可轮换。前缀和域名可改，已写入对象键不改写。有引用或四个内置主键不能删除。

## 建议 skills

- `wta-module-guide`
- `wta-common-modules-guide`
- `engineering-standards`（仅当下一会话要改代码或交付时）

## 不要做

不重新打开已关闭票据，不实现相邻 OIDC 变更，不归档，不推送，不把 Windows 豁免记成测试通过。
