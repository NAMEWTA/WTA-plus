---
schema_version: 3
artifact: ticket
change: 2026-08-31-account-profile-verification
id: T-04
title: 交付材料目录、必传校验与 OSS 引用生命周期
status: done
planning_depth: deep
planning_depth_reason: 材料树、不可删除证据、OSS ACL/生命周期和敏感下载跨越数据与安全边界。
ready: true
risk: high
blocked_by: [T-01, T-02]
contract_ids: [AC-032, AC-033, AC-034, AC-035, AC-042]
owner: codex:/root
expected_changes:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/profile/api/material/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/system/api/OssService.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysOssServiceImpl.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-person/src/main/java/org/dromara/profile/shared/material/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-enterprise/src/main/java/org/dromara/profile/enterprise/material/**</Path>"
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/profile/api/material/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/system/api/OssService.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysOssServiceImpl.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/access/OssObjectMetadataServiceUnitTest.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-person/src/main/java/org/dromara/profile/shared/material/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-enterprise/src/main/java/org/dromara/profile/enterprise/material/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-person/src/test/java/org/dromara/profile/shared/material/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-enterprise/src/test/java/org/dromara/profile/enterprise/material/**</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysMenuServiceImpl.java</Path>"
shared_paths: []
shared_path_owners: []
---

# Ticket T-04: 交付材料目录、必传校验与 OSS 引用生命周期

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/04-material-catalog-and-lifecycle.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-04.md</Path>`

## 1. 战略与来源

- **目标：** 提供个人/企业共同使用、可运营且不会破坏认证证据的材料能力。
- **可观察产出：** 管理 API 可维护合法三层目录；申请可选择一个启用标签挂接合规文件；无权下载拒绝，任何状态变化不删除已挂接 OSS。
- **来源：** `US-016`、`US-017`、`AC-032` 至 `AC-035`、`AC-042`、`ADR-020`、`ADR-023`、`ADR-025`。
- **当前事实：** OssService 已提供元数据、短时下载和 reconcileReferences，但不存在 profile 材料 owner。
- **Planning Depth 原因：** 文件 ACL、永久保留和必传规则失误会泄露或丢失高敏感证据。

## 2. 决策状态

### 已锁定决策

- person 叶子拥有共享目录实现，enterprise 只经 T-01 公开窄端口消费。
- 一级固定个人/企业/通用，可直接标签或二级分类后标签；每材料恰好一个标签。
- systemRequired code 不可改/停/删；普通已引用标签只能停用并保存名称快照。
- 当前集合最多 10 个，每个 10 MiB，仅 JPEG/PNG/PDF；同时校验扩展名、MIME 与 OSS 元数据。
- 工作副本 detach 释放名额但不删 OSS；不可变来源永不 detach。

### 已采用的低影响假设

- 文件扩展名按原始文件名最后后缀规范化，MIME 使用 OSS 权威元数据。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 标签树 CRUD、选择查询、材料挂接/detach、必传/文件校验、预览下载 | OssService、树构建范式 | 文件字节代理、孤立 OSS 清理、前端页面 |

## 4. 要构建什么

运营可维护受限标签树，申请者凭 apply 读取适用叶子并挂接材料；服务端在挂接与提交边界复验。材料下载先校验 profile 权限和关系，再返回短时 URL。

## 5. 实现契约

- **入口或接缝：** material-tag query/manage、work-copy material attach/detach、authorized download。
- **输入与输出：** 树节点/ossId/tagCode -> 合法树、材料关系或短时下载意图。
- **公共接口变化：** 新增 profile 材料 HTTP/API；查询 GET、变更 POST+安全 `@Log`。
- **不变量：** 层级/父类型/单标签、10×10MiB、必传 code、不可变材料永不删除。
- **状态或数据流：** ACL -> OSS metadata -> 领域关系 -> 同事务 reconcileReferences。
- **错误与失败行为：** 非叶子/停用标签、非法类型/大小/数量、无权或 OSS 不一致失败关闭。
- **兼容要求：** 不改变 system OSS 生命周期和 URL 合同。
- **安全与隐私要求：** 引用不是 ACL；未授权不能得到元数据或下载 URL。

## 6. 执行路线

1. 先写树不变量、材料矩阵与无权下载失败测试。
2. 实现共享目录、生命周期保护和申请可选树。
3. 实现 attach/detach、快照锁定、OSS 元数据与引用协调。
4. 实现授权预览下载与敏感日志排除。
5. 运行 MySQL/OSS spy 集成、模块回归和无删除扫描。

## 7. 路径访问契约

- **预计修改点/可写范围：** shared material、enterprise material adapter、profile 材料窄端口，以及补齐 OSS 文件大小/MIME 只读能力的最小 API/实现/测试路径。
- **只读上下文：** system 树实现及 OSS 生命周期内部实现；OssService 只新增兼容只读合同。
- **共享路径：** 无；SQL 只读，公开 API 的局部兼容扩展已纳入本 Ticket 可写合同。
- **保留或不动：** system OSS 表、全局孤立对象清理和生产对象。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常 | service/MVC | 合法树、挂接、detach、下载 | 关系/名额/短时 URL 正确 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-04.md</Path>` |
| 失败 | invariant/ACL matrix | 非法层级、标签、文件、无权下载 | 全部拒绝且无 OSS 删除 | 同上 |
| 回归 | OssService spy + DB | 各状态变化和标签停用 | 历史引用/名称快照保留 | 同上 |

- **Workspace checks：** 定向模块测试、日志/DELETE 扫描与 Maven 编译。
- **E2E disposition：** required：MySQL 关系、OssService 引用和短时下载授权必须联合验证。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate，使用隔离 OSS/test double。
- **Integration evidence：** commit、parent/candidate/result SHA、DB/OSS 结果。

## 9. 发布、迁移与恢复

- **迁移顺序：** T-02 schema -> 目录/材料代码 -> DML 标签校验 -> 消费者。
- **兼容窗口：** 标签 code 发布后稳定；显示名可变并在历史快照保存。
- **监控信号：** 校验失败、无权下载、OSS reconcile 失败、detached 数量。
- **回滚或前向恢复：** 禁用写入口并保留引用；只前向修复已挂接证据。
- **不可逆操作与批准点：** 无 OSS 删除授权；生产标签变更另行运营控制。
- **收缩条件：** 不适用：无旧材料模型。

## 10. 验收标准

- [x] AC-032 至 AC-035、AC-042 通过且无物理删除。
- [x] required E2E 与敏感下载正反矩阵记录完整。
- [x] 实际路径、提交和父分支结果符合合同。
