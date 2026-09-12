# Final Implementation Audit

- **Change:** `2026-08-21-oss-direct-unified-notification`
- **Review:** `CR-001`
- **Backend checkpoint:** `704d87a6ba09cf106760ff6619c4d4e4437c4087`
- **Frontend checkpoint:** `f7d116f6e2b6b61239afc86cbcb860a07530abad`
- **Workspace:** current workspace / direct-parent
- **E2E:** not-required；遵循用户明确决定，未建设或运行 E2E

## CR-001 Findings Closed

1. 附件 HTTP 入口补齐显式权限和业务归属校验，内部快照 SPI 保持可复用。
2. credential-like 通知使用 `REDACT_SENSITIVE`，不落正文/参数并脱敏目标；重复 requestId 可保留独立审计记录。
3. 通知清理按固定批次解绑附件并物理删除父子日志。
4. OSS 删除使用 `ACTIVE/PENDING` 两阶段；Provider 失败可重试，重复 unbind 幂等，bind 可取消 PENDING。
5. SINGLE resume 重新签名，上传 owner 同时校验 userId/clientPk，本地恢复状态按 Client/Token/policy/fingerprint 隔离。
6. 头像和公告接入真实业务引用，公告/通知通过业务授权端点解析实际附件短链。
7. 上传组件使用命名策略，Editor 防止陈旧异步解析覆盖最新内容。
8. 管理页展示生命周期/引用状态，启动诊断实际读取 Bucket CORS/Lifecycle 且不修改外部配置。

## ADR-010 Owner Extension Closed

1. `OssService.reconcileReferences` 以规范化集合差分完成先绑定、后解绑；非法输入、幂等和失败传播合同已覆盖。
2. `sys_user.avatar`、`sys_notice.notice_content`、`sys_notify_log.attachment_oss_ids` 与 `flow_his_task.ext` 均由所属业务模块在同一 `@DSTransactional` 内维护引用，失败时业务写入回滚。
3. `business-oss-owners.json` 登记四个 Owner；架构测试双向扫描生产 Java、SQL、调用方和 carrier allowlist，防止漏登记与陈旧登记。
4. 公共 raw `bind/unbind` API 已移除，只保留集合协调入口；fresh baseline 验证有引用对象不清理、无引用 TEMP 仅进入 dry-run 候选。

## Verification

| Gate | Result |
|---|---|
| `sh mvnw -pl ruoyi-admin -am -Dmaven.test.skip=false test` | pass；102 tests，0 failure/error，3 个外部 Redis/MySQL 条件测试 skip |
| `sh mvnw -pl ruoyi-admin -am package -DskipTests` | pass；36-module reactor，生成 ruoyi-admin jar |
| `bash ./mvnw -Dmaven.test.skip=false test` | pass；ADR-010 最终 HEAD 41-module reactor，129 tests，0 failure/error，3 个外部环境条件测试 skip |
| `bash ./mvnw -DskipTests package` | pass；ADR-010 最终 HEAD 41-module reactor |
| `pnpm exec vitest run` | pass；2 files / 4 tests |
| `pnpm lint` | pass；0 error |
| `pnpm build:prod` | pass；3368 modules transformed |
| `pnpm exec vue-tsc --noEmit` | baseline exception；仅既有两个 TS1149：`loginInfo` / `logininfo` 大小写冲突 |
| `git diff --check` | pass；backend/frontend/本 change 工件 |

## Speculo Validator Limitation

本 change 的 Evidence 同时记录 backend/frontend 两个独立 Git 仓库 SHA。当前 `validate-specdev.mjs --repo` 只接受单一仓库并把全部 SHA 交给父仓库解析，因此对 22 个 execution records 报告 176 条 multi-repo Git Evidence/HEAD/dirty 派生错误；它不能表达组件 SHA 对。无 `--repo` 的完整结构/合同校验为 `0 error / 0 warning`。实际 commit 已按所属子仓分别使用 `git cat-file` 和祖先关系核对；未修改用户 dirty 的校验器来规避该限制。

## Review Axes

- **标准轴:** 外部副作用发生在已持久化 PENDING 之后；敏感审计最小化；HTTP 权限与内部 SPI 分层；上传成功不依赖管理下载权限；异步回显只接受最新 generation；业务写入与引用转换同事务 fail-closed。
- **规范轴:** common 不反向依赖 system；workflow 只经 ruoyi-api 调用 OSS；`ref_type` 保持真实物理表名；Client 不作为租户/所有权；manifest 只作为测试期 ratchet，不成为运行时注册中心。

## Residual Release Conditions

本地实现完成不等于生产发布已验收。生产 SQL、真实 Bucket CORS/Expose ETag/Multipart Lifecycle、Provider 权限和浏览器角色矩阵均未执行；TEMP 主动清理继续默认关闭并保持 dry-run，启用前必须单独审核。
