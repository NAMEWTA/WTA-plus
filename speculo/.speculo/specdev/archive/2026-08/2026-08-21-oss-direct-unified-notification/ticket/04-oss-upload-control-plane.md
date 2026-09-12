---
schema_version: 3
artifact: ticket
change: 2026-08-21-oss-direct-unified-notification
id: T-04
title: OSS 上传控制面
status: done
planning_depth: deep
planning_depth_reason: 新增安全敏感 HTTP 协议、Redis 状态机、策略配置、Multipart 完成与补偿清理。
ready: true
risk: high
blocked_by: [T-03]
contract_ids: [AC-001, AC-002, AC-004, AC-005, AC-006, AC-007, AC-032]
owner: cursor-agent
expected_changes: ["<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysOssUploadController.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/upload/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/application.yml</Path>"]
writable_paths: ["<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysOssUploadController.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/upload/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/application.yml</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/upload/**</Path>"]
read_only_paths: ["<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-oss/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/**</Path>"]
shared_paths: []
shared_path_owners: []
---

# Ticket T-04: OSS 上传控制面

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/ticket/04-oss-upload-control-plane.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-04.md</Path>`

## 1. 战略与来源

- **目标：** 交付后端只处理 JSON 授权与元数据的 SINGLE/MULTIPART 上传控制面。
- **可观察产出：** init/sign/list/complete/abort 可用，Complete 只返回字符串 ossId，续传和重复 Complete 可判定。
- **来源：** `ADR-002/003/004/005`、`AC-001/002/004/005/006/007/032`。
- **当前事实：** 当前 Controller 接收 MultipartFile；缺少 UploadTicket、命名策略、恢复和完成确认。
- **Planning Depth 原因：** 该协议直接控制任意对象写入，涉及身份冻结、Redis、Provider、DB 和清理竞态。

## 2. 决策状态

### 已锁定决策

- 固定 REST 合同为 `/resource/oss/uploads`、parts sign/list、complete、abort。
- 对象 Key、bucket、provider/uploadId 和策略快照只由服务端/Ticket 决定。
- Ticket 默认 24 小时；Part URL 分钟级、按窗口申请；Complete 重试返回同一 ossId。
- 类型化命名 uploadPolicy 在启动校验，可含 requiredPermission 与可选 Client 入口准入；Client 不是对象所有权。

### 已采用的低影响假设

- 默认 Part 签名 5 分钟；具体可配置值受安全上下限约束。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| policy、Ticket、REST、resume、complete、abort、cleanup/diagnostics | T-01 OssClient、T-03 lifecycle、RedisUtils、权限上下文 | 前端 transport、旧协议删除、数据库策略后台、跨设备恢复、杀毒 |

## 4. 要构建什么

登录用户以 policy 和文件元数据初始化；后端验证策略/权限/可选 Client 准入并冻结上下文。SINGLE 返回结构化 PUT；MULTIPART 返回 partSize/count，Part URL 按批次申请。重新选择同文件后以指纹和 ListParts 恢复。Complete 校验所有权、Part、HEAD、size/type/magic/checksum capability，成功仅创建一次 TEMP sys_oss 并返回 ossId；失败不生成可用元数据并清理或登记补偿。Abort 与过期清理幂等。

## 5. 实现契约

- **入口或接缝：** 固定 REST、ConfigurationProperties、Redis UploadTicket、T-01/T-03 Service。
- **输入与输出：** Spec 第 7 节固定请求/响应；complete `R<String>`。
- **公共接口变化：** 新 HTTP 控制面和配置键；不再新增 MultipartFile 入口。
- **不变量：** 浏览器不权威提供 key/provider/uploadId；Ticket 绑定 user 并记录来源 client_pk 快照，但 client_pk 不授予所有权。
- **状态或数据流：** INITIALIZED -> UPLOADING -> COMPLETING -> COMPLETED/ABORTED/EXPIRED。
- **错误与失败行为：** 未知策略、无权限、指纹不符、过期/非本人、校验失败与清理失败可区分。
- **兼容要求：** T-05 同批迁移前保留旧 Controller，不在此 Ticket 收缩。
- **安全与隐私要求：** 不记录 URL/Secret；CORS/Lifecycle 只诊断不修改；防止任意 key 签名。

## 6. 执行路线

1. 固化 REST DTO、状态机、策略校验和身份/指纹失败测试。
2. 实现类型化 policy 与启动时校验、运维诊断。
3. 实现 UploadTicket、过期索引、init/sign/list/abort。
4. 实现 SINGLE/MULTIPART complete、HEAD/策略验证、幂等结果与失败补偿。
5. 实现应用主动 Abort cleanup，并验证 Provider Lifecycle 仅为兜底配置。
6. 执行 HTTP+Redis+OSS+DB 集成矩阵。

## 7. 路径访问契约

- **预计修改点：** 新 Controller、`system/oss/upload/**`、应用配置示例。
- **可写范围：** frontmatter 精确路径；若需改 T-03 文件必须停止并交回 owner/调整 Ticket。
- **只读上下文：** common-oss 和 lifecycle API。
- **共享路径：** 无。
- **保留或不动：** 旧 SysOssController、前端、SQL、Bucket policy。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | HTTP+Redis+S3+DB | SINGLE、Multipart、resume、重复 complete | 仅 OSS 承载字节，返回唯一 ossId | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-04.md</Path>` |
| 失败路径 | 安全/清理注入 | 伪造 key、换用户、过期、bad magic/size、部分 Part | 拒绝且无可用孤儿元数据 | 同上 |
| 回归 | Maven tests/context | `./mvnw -pl ruoyi-modules/ruoyi-system -am -Dmaven.test.skip=false test` | 配置和 system 回归通过 | 同上 |

- **Workspace checks：** current 为 `current-workspace`；required 为 `source-worktree` 非 E2E 检查，Lead 在候选父分支运行集成。
- **E2E disposition：** not-required：用户明确不执行 E2E；跨依赖风险由 ruoyi-admin 集成测试、Provider 测试替身和人工 API 验收覆盖。
- **E2E owner/environment：** 不适用；Lead 在 current-workspace 核对单/多段、续传、重复完成、取消、过期和坏配置证据。
- **Integration evidence：** implementation/source commit、parent before、candidate/result SHA、网络/对象/DB Evidence。

## 9. 发布、迁移与恢复

- **迁移顺序：** T-01/T-02/T-03 -> 配置/CORS/Lifecycle 预检 -> 控制面 -> T-05 前端切换。
- **兼容窗口：** 本 Ticket 与旧入口短时并存，仅供集成；生产由 T-05 一次切换。
- **监控信号：** 活跃/过期 Ticket、sign/complete/abort 失败、遗留 multipart、TEMP 数和校验失败原因。
- **回滚或前向恢复：** 禁用新策略/入口，主动 Abort 会话，保留已完成 sys_oss；修复后重开。
- **不可逆操作与批准点：** 启用过期主动 Abort 和生产 CORS 前 Lead 批准运维检查。
- **收缩条件：** T-05 证明已知浏览器调用全部迁移后删除旧协议。

## 10. 验收标准

- [x] `AC-001/002/004/005/006/007/032` 的控制面接缝已验证；浏览器网络合同由 T-05 验收。
- [x] complete 只返回字符串 ossId 且重复调用幂等。
- [x] Client 仅作策略入口准入/审计快照，不形成对象隔离。
- [x] ruoyi-admin 集中测试、真实 Redis、提交 SHA 与 Evidence 完整；E2E 按用户决定 not-required。
