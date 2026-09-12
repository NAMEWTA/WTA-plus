---
schema_version: 3
artifact: ticket
change: 2026-08-21-oss-direct-unified-notification
id: T-03
title: OSS TEMP、引用与下载授权
status: done
planning_depth: deep
planning_depth_reason: 引入跨业务引用和对象生命周期公共 API，并改变删除与下载安全边界。
ready: true
risk: high
blocked_by: [T-01, T-02]
contract_ids: [AC-007, AC-008, AC-009, AC-010, AC-011, AC-012, AC-013, AC-026, AC-029, AC-030]
owner: cursor-agent
expected_changes: ["<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/system/api/OssService.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysOssServiceImpl.java</Path>"]
writable_paths: ["<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/system/api/OssService.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/system/api/domain/OssDTO.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysOssController.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/domain/SysOss.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/domain/SysOssExt.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/domain/bo/SysOssBo.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/domain/vo/SysOssVo.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/mapper/SysOssMapper.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/ISysOssService.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysOssServiceImpl.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/resources/mapper/system/SysOssMapper.xml</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/lifecycle/**</Path>"]
read_only_paths: ["<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-oss/**</Path>", "<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DDL.sql</Path>"]
shared_paths: []
shared_path_owners: []
---

# Ticket T-03: OSS TEMP、引用与下载授权

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/ticket/03-oss-lifecycle-reference-download.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-03.md</Path>`

## 1. 战略与来源

- **目标：** 建立上传完成后的 TEMP 生命周期、轻量反向引用、安全删除和短时下载授权。
- **可观察产出：** 业务可 bind/unbind ossId；管理面看见真实来源表/主键；有引用对象拒绝删除；有权请求只得到短时 URL。
- **来源：** `ADR-001/002/005/009`、`AC-007..013/026/029/030`。
- **当前事实：** 当前 system OSS 只保存对象元数据并由后端代理下载，没有可索引 TEMP 或多业务引用。
- **Planning Depth 原因：** 该 Ticket 定义跨模块公共 API、并发生命周期和下载授权边界。

## 2. 决策状态

### 已锁定决策

- Complete 后对象默认 TEMP 24 小时；首个引用退出 TEMP，最后解绑重新开始 24 小时。
- 引用只做反向定位和生命周期保护：`ref_type=真实物理表名`、`ref_id=真实主键`，不动态查表、不授予 ACL。
- 普通业务必须先做自身权限/数据权限，再调用内部 presign；通用管理下载要求 `system:oss:download`。
- `sys_oss` 和 ref 不按 Client 隔离；Client 只通过当前权限求值影响接口访问。

### 已采用的低影响假设

- bind/unbind 接口返回最终引用/TEMP 状态，便于调用者验证幂等结果。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| lifecycle/ref Service、ruoyi-api 边界、管理下载 URL、引用保护删除、TEMP 清理 | T-01 OssClient、T-02 schema、Sa-Token/DataPermission | 浏览器通用 bind、动态查业务表、业务 ACL 引擎、上传控制面 |

## 4. 要构建什么

业务 Service 在自身事务授权后用实际表名和主键绑定或解除 ossId。生命周期 Service 串行化关键状态，确保清理与绑定并发时有效引用不被误删。管理员下载经权限返回短时 URL；普通业务只能通过内部 OssService 在完成业务对象授权后取 URL。管理删除发现有效引用立即拒绝并保留所有数据。

## 5. 实现契约

- **入口或接缝：** `ruoyi-api OssService`、system lifecycle/ref Service、SysOssController 管理下载/删除。
- **输入与输出：** ossId/refType/refId；下载返回 url/expiresAt/fileName；列表 VO 返回 TEMP 与引用定位摘要。
- **公共接口变化：** additive bind/unbind/snapshot/presign 接口与 DTO；不开放 HTTP bind。
- **不变量：** 引用唯一；有引用不 TEMP；最后解绑才 TEMP；引用不是权限；Provider 字节不经过后端。
- **状态或数据流：** TEMP -> REFERENCED -> TEMP -> DELETED，数据库与 Provider 删除可补偿且幂等。
- **错误与失败行为：** 无权、对象不存在、引用冲突、仍被引用和 Provider 删除失败均可区分。
- **兼容要求：** 历史非 TEMP 对象保持；T-05 才删除旧字节代理协议。
- **安全与隐私要求：** URL 短时、不得持久化；仅知道 ossId 不能下载；Client 不做行隔离。

## 6. 执行路线

1. 建立 bind/unbind/clean/delete/presign 的并发和权限失败测试。
2. 映射 T-02 schema，增加 ref Mapper、VO 和生命周期 Service。
3. 扩展 ruoyi-api OssService，供业务授权后调用。
4. 接入管理下载 URL、管理列表引用展示和引用保护删除。
5. 实现索引驱动、幂等可重试的 TEMP 清理并验证绑定竞争。

## 7. 路径访问契约

- **预计修改点：** ruoyi-api OssService/DTO 与 system OSS 精确路径、新 `system/oss/**`。
- **可写范围：** 仅 frontmatter 所列；SQL 由 T-02 独占。
- **只读上下文：** T-01 common-oss 与 T-02 schema。
- **共享路径：** 无；与 T-05 的重叠由阻塞链顺序移交。
- **保留或不动：** 前端、通知模块、业务表和业务授权实现。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | DB/OSS/Service 集成 | complete fixture 后 bind/unbind/download/clean | 状态和 URL 合同正确 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-03.md</Path>` |
| 失败路径 | 并发与权限矩阵 | 清理并发 bind、猜 ossId、删除有引用对象 | 不误删、不越权 | 同上 |
| 回归 | Maven tests | `./mvnw -pl ruoyi-modules/ruoyi-system -am -Dmaven.test.skip=false test` | system/API 回归通过 | 同上 |

- **Workspace checks：** current 使用 `current-workspace`；required 使用 `source-worktree` 做非 E2E，再由 Lead 合并验证。
- **E2E disposition：** not-required：用户明确不执行 E2E；用 ruoyi-admin 中 MySQL/Redis/OSS/权限集成测试及人工 API 验收覆盖。
- **E2E owner/environment：** 不适用；Lead 在 current-workspace 核对集成测试与人工 API transcript。
- **Integration evidence：** 记录 implementation/source commit、parent before、candidate/result SHA 和 Lead Evidence。

## 9. 发布、迁移与恢复

- **迁移顺序：** T-01/T-02 先落地；先启用 schema/API，再由 T-04 创建新 TEMP 对象。
- **兼容窗口：** 历史对象非 TEMP；旧下载协议保持到 T-05 同批收缩。
- **监控信号：** 到期 TEMP、孤立 ref、清理成功/失败/重试、拒绝删除和 presign 失败。
- **回滚或前向恢复：** 停用清理任务并保留 schema；Provider 删除失败进入补偿，禁止通过删 DB 掩盖对象残留。
- **不可逆操作与批准点：** 首次启用 TEMP 删除任务前，Lead 审核 dry-run 影响集合。
- **收缩条件：** T-05 完成旧下载入口移除，集成测试与人工网络验收证明新授权入口可用。

## 10. 验收标准

- [x] `AC-007..013/026/029/030` 的生命周期、授权和删除行为通过。
- [x] ref_type 只接受真实物理表名语义，且未用于 ACL/动态查表。
- [x] Client 未形成 sys_oss/ref 行隔离。
- [x] 集成测试、人工验收、提交 SHA 与 Evidence 完整。
