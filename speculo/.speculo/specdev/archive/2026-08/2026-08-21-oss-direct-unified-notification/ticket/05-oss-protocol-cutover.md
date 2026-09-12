---
schema_version: 3
artifact: ticket
change: 2026-08-21-oss-direct-unified-notification
id: T-05
title: 前后端 OSS 协议切换
status: done
planning_depth: deep
planning_depth_reason: 一次性移除旧字节代理协议并迁移多个共享前端组件，属于破坏性跨端 contract 变更。
ready: true
risk: high
blocked_by: [T-04]
contract_ids: [AC-001, AC-003, AC-004, AC-012, AC-013, AC-014]
owner: cursor-agent
expected_changes: ["<Path>plus-ui-namewta/src/api/system/oss/**</Path>", "<Path>plus-ui-namewta/src/components/FileUpload/**</Path>", "<Path>plus-ui-namewta/src/components/ImageUpload/**</Path>", "<Path>plus-ui-namewta/src/components/Editor/**</Path>", "<Path>plus-ui-namewta/src/plugins/download.ts</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysOssController.java</Path>"]
writable_paths: ["<Path>plus-ui-namewta/src/api/system/oss/**</Path>", "<Path>plus-ui-namewta/src/components/FileUpload/**</Path>", "<Path>plus-ui-namewta/src/components/ImageUpload/**</Path>", "<Path>plus-ui-namewta/src/components/Editor/**</Path>", "<Path>plus-ui-namewta/src/plugins/download.ts</Path>", "<Path>plus-ui-namewta/src/views/system/user/profile/userAvatar.vue</Path>", "<Path>plus-ui-namewta/src/utils/oss/**</Path>", "<Path>plus-ui-namewta/src/hooks/oss/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysOssController.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/ISysOssService.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysOssServiceImpl.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/cutover/**</Path>"]
read_only_paths: ["<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/upload/**</Path>"]
shared_paths: []
shared_path_owners: []
---

# Ticket T-05: 前后端 OSS 协议切换

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/ticket/05-oss-protocol-cutover.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-05.md</Path>`

## 1. 战略与来源

- **目标：** 将所有已知浏览器 OSS 调用切到直传/直下，并收缩旧后端字节协议。
- **可观察产出：** FileUpload、ImageUpload、Editor、头像和下载插件只走控制面；网络面板中文件字节只往返 OSS。
- **来源：** `ADR-002/003`、`AC-001/003/004/012/013/014`。
- **当前事实：** 组件仍使用 `/resource/oss/upload`，下载插件使用后端 blob；共享 Axios 会附加业务 Header。
- **Planning Depth 原因：** 同时改变公共前端组件和后端 HTTP contract，且无兼容窗口。

## 2. 决策状态

### 已锁定决策

- 使用无业务 baseURL/interceptor/Auth/clientid 的独立 OSS transport，只发送 requiredHeaders。
- 组件保留进度、取消、回显、失败 Part 重试和重新选择同文件续传。
- 删除 `POST /resource/oss/upload` 与字节代理 `GET /resource/oss/download/{id}`，不保留 302 兼容。
- 管理下载调用新 download-url；普通业务仍必须由业务 API 授权。

### 已采用的低影响假设

- 本地续传索引存于 IndexedDB 或现有前端可维护存储，值只含 token/fingerprint/非敏感元数据。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| API、裸 transport、共享组件、头像、下载插件、旧后端协议删除、调用点扫描 | T-04 控制面、现有组件 props/events/UI | 新业务附件 ACL、跨设备恢复、静态路由 |

## 4. 要构建什么

用户选择文件后组件初始化上传，根据模式直接 PUT 或按窗口上传 Part，展示稳定进度并支持取消。刷新/断网后重新选择同一文件可恢复缺失 Part。成功只把 ossId 回传业务表单。预览/下载先通过适用授权 API 获取短时 URL再访问 OSS。所有旧浏览器字节入口和已知调用点在同一候选版本中移除。

## 5. 实现契约

- **入口或接缝：** system OSS API、`customUpload/http-request`、独立 transport、共享组件事件、旧路由扫描。
- **输入与输出：** File -> upload session -> ossId；ossId -> authorized short URL -> browser navigation/blob-free download。
- **公共接口变化：** 前端 OSS API 全面替换；旧后端 upload/download 字节协议删除。
- **不变量：** 不向 OSS 发送 Authorization/clientid；业务模型不保存临时 URL；取消不改变组件布局。
- **状态或数据流：** init -> PUT/parts -> complete -> emit ossId；resume 仅上传缺失 Part。
- **错误与失败行为：** CORS、签名过期、Part 失败、指纹不符和取消以组件可恢复状态呈现。
- **兼容要求：** 前后端必须原子发布；仓库内旧 URL/Builder 调用扫描为零。
- **安全与隐私要求：** 本地不保存 signed URL/Secret；HTML/文件预览遵循既有安全策略。

## 6. 执行路线

1. 建立前端 API 类型、裸 transport 与上传状态机测试接缝。
2. 迁移 FileUpload/ImageUpload/Editor/头像并保留 props/events。
3. 迁移管理下载与插件，业务下载只接受业务授权 URL。
4. 删除后端旧 upload/download 字节方法及前端旧调用。
5. 执行 lint/build/typecheck、静态扫描和真实浏览器人工网络验收；不建设 E2E 测试套件。

## 7. 路径访问契约

- **预计修改点：** frontmatter 所列前端路径和三个旧 system OSS 文件。
- **可写范围：** 仅所列；upload control 只读。
- **只读上下文：** T-04 固定 HTTP contract。
- **共享路径：** 无；T-03/T-04 完成后顺序移交重叠文件。
- **保留或不动：** SQL、业务模块的具体附件权限和全局 request 实例。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | 浏览器网络/UI | 四类入口上传、预览、下载 | OSS 承载字节，业务仅得 ossId | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-05.md</Path>` |
| 失败路径 | 浏览器故障矩阵 | 断网、刷新、过期签名、Part 失败、取消 | 可重试/续传且无布局错乱 | 同上 |
| 回归 | frontend/backend gate | `pnpm lint`; `pnpm exec vue-tsc --noEmit`; `pnpm build:prod`; Maven test；`rg` 旧 URL | 构建通过且旧调用为零 | 同上 |

- **Workspace checks：** current 在 `current-workspace`；required 时 source-worktree 跑非 E2E，Lead 在 parent-candidate 跑浏览器矩阵。
- **E2E disposition：** not-required：用户明确不执行 E2E；以 ruoyi-admin 集成测试、前端门禁和 Lead 浏览器人工网络验收替代自动化 E2E Gate。
- **E2E owner/environment：** 不适用；Lead 在 current-workspace 保留人工 network/screenshot/cancel/resume Evidence。
- **Integration evidence：** 前后端各自 implementation commit、各 parent before/result SHA、候选组合 SHA 和 Lead Evidence。

## 9. 发布、迁移与恢复

- **迁移顺序：** CORS/Lifecycle -> 后端新控制面 -> 前端切换与旧协议收缩，作为同一发布单元。
- **兼容窗口：** 无生产兼容窗口；禁止只发布一侧。
- **监控信号：** init/complete 错误率、CORS、签名过期、旧路由 404、前端取消/失败。
- **回滚或前向恢复：** 以配对前后端版本整体回滚；不能单独恢复旧路由而保留新前端。
- **不可逆操作与批准点：** 删除旧 HTTP contract 前以仓库扫描和外部调用方发布确认作为批准点。
- **收缩条件：** 所有已知前端旧 URL 为零，浏览器人工网络验收通过。

## 10. 验收标准

- [x] `AC-001/003/004/012/013/014` 的本地合同、失败恢复与调用面验证完成；真实 Bucket/CORS 浏览器验收保留为发布前置条件。
- [x] 旧 upload/download 字节入口及已知调用点为零。
- [x] OSS transport 不携带业务 Header，成功值仅 ossId。
- [x] 前后端提交、direct-parent result SHA 与本地 Evidence 完整；未把未执行的真实环境人工验收记为通过。
