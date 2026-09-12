---
schema_version: 3
artifact: ticket
change: 2026-08-21-oss-direct-unified-notification
id: T-18
title: OSS 管理面与 Bucket 实际诊断
status: done
planning_depth: standard
planning_depth_reason: 运维 UI 与只读外部配置检查
ready: true
risk: medium
blocked_by: [T-17]
contract_ids: [AC-008, AC-009, AC-032]
owner: codex
expected_changes: ["<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-oss/**</Path>", "<Path>plus-ui-namewta/src/views/system/oss/**</Path>"]
writable_paths: ["<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-oss/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/**</Path>", "<Path>plus-ui-namewta/src/**</Path>"]
read_only_paths: []
shared_paths: []
shared_path_owners: []
---

# Ticket T-18: OSS 管理面与 Bucket 实际诊断

## 1. 战略与来源
来源 CR-001 与 AC-032。把生命周期和部署前置从静态说明变成可观察事实。

## 2. 决策状态
common-oss 提供只读 Bucket 配置快照；启动读取 CORS/Lifecycle，不修改 policy；管理页显示状态和引用。

## 3. 范围边界
IN：默认 Bucket 诊断、OSS 列表。REUSE：S3AsyncClient、SysOssVo。OUT：自动修复 Bucket。

## 4. 要构建什么
启动日志明确指出 Origin/PUT/ETag/Abort 缺口或读取错误；管理员可扫描 TEMP/PENDING/引用数/过期时间。

## 5. 实现契约
- **安全：** 不输出 AccessKey/Secret/签名 URL。

## 6. 执行路线
1. 公共诊断模型。2. 实际只读调用。3. UI 字段。4. 编译/lint/build。

## 7. 路径访问契约
不自动变更任何外部 Bucket 状态。

## 8. 验证矩阵
 - **E2E disposition：** not-required；用户已确认不建设 E2E，执行环境为 current-workspace。
| 风险 | 接缝 | 预期 | Evidence |
|---|---|---|---|
| 假阳性运维提示 | common compile + review | 实际读取并逐项报告 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-18.md</Path>` |

## 9. 发布、迁移与恢复
读取权限不足仅记录诊断失败，不阻止应用启动。

## 10. 验收标准
- [x] common 编译、前端门禁及 Secret 静态审查通过。
