---
schema_version: 3
artifact: ticket
change: 2026-08-21-oss-direct-unified-notification
id: T-01
title: OSS S3 兼容直传基础能力
status: done
planning_depth: deep
planning_depth_reason: 扩展公共 OssClient 契约并处理多家 S3-compatible Provider、Multipart、自定义域名签名和对象复制兼容性。
ready: true
risk: high
blocked_by: []
contract_ids: [AC-002, AC-005, AC-006, AC-007, AC-023, AC-024, AC-032]
owner: cursor-agent
expected_changes: ["<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-oss/src/main/java/org/dromara/common/oss/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/client/**</Path>"]
writable_paths: ["<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-oss/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/client/**</Path>"]
read_only_paths: ["<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/**</Path>"]
shared_paths: []
shared_path_owners: []
---

# Ticket T-01: OSS S3 兼容直传基础能力

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/ticket/01-oss-s3-direct-transfer-foundation.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-01.md</Path>`

## 1. 战略与来源

- **目标：** 让 common-oss 提供浏览器直传控制面所需的稳定、Provider-neutral 基础合同。
- **可观察产出：** 上层可完成 HEAD、结构化预签名、Multipart create/sign/list/complete/abort、CopyObject 与 capability 查询，不接触 AWS SDK model。
- **来源：** `ADR-002`、`ADR-003`、`ADR-009`、`AC-002/005/006/007/023/024/032`。
- **当前事实：** `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-oss</Path>` 已有普通 PUT/GET presign 与 S3 客户端，但缺少完整 Multipart、HEAD、复制和能力合同。
- **Planning Depth 原因：** 公共 API 和多 Provider 兼容行为改变，错误会影响全部 OSS 消费者。

## 2. 决策状态

### 已锁定决策

- 新合同使用项目自有 DTO，不向 system 暴露 AWS SDK 类型。
- 预签名结果包含 method、URL、requiredHeaders、expiresAt；自定义域名签名覆盖 canonical query。
- 完整性基线是 HTTPS、Part ETag、Complete 与 HEAD size；标准 checksum 仅作为 capability 增强。
- CopyObject 用于通知附件快照；Abort、Complete 和删除相关操作必须支持幂等上层编排。

### 已采用的低影响假设

- 保留既有普通 presign 方法并委托新实现，直到 T-05 完成调用迁移。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| HEAD、Multipart、Copy、structured presign、capability 与测试 | 现有 S3AsyncClient、TransferManager、Presigner、OssFactory | UploadTicket、HTTP API、sys_oss、Bucket policy 自动修改 |

## 4. 要构建什么

调用者以 common-oss 自有请求模型创建上传、批量签 Part、查询已上传 Part、完成或中止会话，并能 HEAD 或复制对象。Provider 不支持增强 checksum 时仍按 S3 兼容基线工作；不支持必需能力时返回可识别错误而非静默降级。签名结果只要求浏览器发送明确列出的 Header。

## 5. 实现契约

- **入口或接缝：** `OssClient` 及其 S3 实现、common-oss 定向测试。
- **输入与输出：** bucket/key/uploadId/partNumber/options 转换为项目 DTO；HEAD 返回 size/contentType/etag/metadata。
- **公共接口变化：** additive 扩展 `OssClient`，增加 Multipart、Copy、HEAD、capability 和 structured presign。
- **不变量：** 不把 Multipart ETag 当整文件 MD5；Secret、签名 URL 不写日志；requiredHeaders 与签名完全一致。
- **状态或数据流：** create -> sign/upload -> list -> complete 或 abort；copy -> head 验证。
- **错误与失败行为：** Provider 拒绝、能力缺失、签名失败和对象不存在以稳定异常类别上抛。
- **兼容要求：** 现有 upload/download/delete 与普通 presign 调用保持编译和行为兼容。
- **安全与隐私要求：** 日志清洗 credential、token、signed query 和敏感 metadata。

## 6. 执行路线

1. 为公共模型、签名 query、Multipart ETag 和 capability 建立失败测试。
2. 增加 Provider-neutral DTO 与 additive `OssClient` 契约。
3. 实现标准 endpoint 与自定义域名下的结构化预签名、Multipart 和 HEAD。
4. 实现 CopyObject、能力探测及异常清洗。
5. 运行 common-oss 定向测试和受影响 Maven reactor 回归。

## 7. 路径访问契约

- **预计修改点：** common-oss 主代码与 `<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/client/**</Path>` 集中测试。
- **可写范围：** 仅 frontmatter `writable_paths`；越界前停止。
- **只读上下文：** system 当前 OssClient 使用方式。
- **共享路径：** 无。
- **保留或不动：** system、前端、SQL 和 Bucket policy。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | common-oss 单元/Provider 集成 | `./mvnw -pl ruoyi-common/ruoyi-common-oss -am -Dmaven.test.skip=false test` | Multipart/HEAD/Copy 合同通过 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-01.md</Path>` |
| 失败路径 | capability 与签名测试 | 注入不支持能力、过期签名和 Provider 错误 | 错误可区分且不泄露 Secret | 同上 |
| 回归 | Maven reactor | 编译既有 common-oss 消费者 | 旧 API 仍可编译使用 | 同上 |

- **Workspace checks：** Goal Plan 选择 current 时在 `current-workspace` 执行；required 时在 `source-worktree` 完成非 E2E 检查。
- **E2E disposition：** not-required：本 Ticket 是公共库接缝，Provider 集成测试足以验证，不单独交付终端用户流程。
- **E2E owner/environment：** 不适用；T-04/T-05 以 ruoyi-admin 集成测试和浏览器人工验收覆盖跨边界行为。
- **Integration evidence：** 记录 implementation/source commit、direct-parent 或 parent-candidate 的 parent before/result SHA，以及 Lead Evidence。

## 9. 发布、迁移与恢复

- **迁移顺序：** 先 additive 公共合同，后续 Ticket 才消费。
- **兼容窗口：** 旧方法保留至调用点收缩完成。
- **监控信号：** Provider capability、签名/Multipart/Copy 失败类别和耗时。
- **回滚或前向恢复：** 未有消费者前可回滚；已有消费者后以前向修复保持二进制/源码兼容。
- **不可逆操作与批准点：** 无。
- **收缩条件：** T-05 证明旧调用点为零后才允许另行删除兼容方法。

## 10. 验收标准

- [x] `AC-002/005/006/007/023/024/032` 所需 common-oss 能力有可执行验证。
- [x] AWS SDK model 未跨出 common-oss 公共边界。
- [x] 验证矩阵与集成 SHA 已记录到 Evidence。
- [x] 修改未超出 writable_paths，E2E disposition 已落实。
