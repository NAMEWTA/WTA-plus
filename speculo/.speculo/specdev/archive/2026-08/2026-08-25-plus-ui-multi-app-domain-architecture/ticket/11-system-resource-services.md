---
schema_version: 3
artifact: ticket
change: 2026-08-25-plus-ui-multi-app-domain-architecture
id: T-11
title: 迁移 system-admin 资源与内容服务
status: done
planning_depth: deep
planning_depth_reason: 字典、配置、通知、OSS、消息和社交登录包含上传下载、安全校验与多个共享 UI 消费者
ready: true
risk: high
blocked_by: [T-10]
contract_ids: [AC-009, AC-010, AC-019, AC-021]
owner: codex:/root
expected_changes: ["<Path>plus-ui-namewta/packages/domains/system-admin/**</Path>", "<Path>plus-ui-namewta/packages/web-domains/system-admin/**</Path>", "<Path>plus-ui-namewta/src/api/system/**</Path>", "<Path>plus-ui-namewta/src/views/system/**</Path>"]
writable_paths: ["<Path>plus-ui-namewta/packages/domains/system-admin/**</Path>", "<Path>plus-ui-namewta/packages/web-domains/system-admin/**</Path>", "<Path>plus-ui-namewta/src/api/system/dict/**</Path>", "<Path>plus-ui-namewta/src/api/system/config/**</Path>", "<Path>plus-ui-namewta/src/api/system/notice/**</Path>", "<Path>plus-ui-namewta/src/api/system/oss/**</Path>", "<Path>plus-ui-namewta/src/api/system/ossConfig/**</Path>", "<Path>plus-ui-namewta/src/api/system/message/**</Path>", "<Path>plus-ui-namewta/src/api/system/social/**</Path>", "<Path>plus-ui-namewta/src/views/system/dict/**</Path>", "<Path>plus-ui-namewta/src/views/system/config/**</Path>", "<Path>plus-ui-namewta/src/views/system/notice/**</Path>", "<Path>plus-ui-namewta/src/views/system/oss/**</Path>", "<Path>plus-ui-namewta/src/views/system/ossConfig/**</Path>", "<Path>plus-ui-namewta/src/views/system/message/**</Path>", "<Path>plus-ui-namewta/src/views/system/social/**</Path>", "<Path>plus-ui-namewta/e2e/system-resources.spec.ts</Path>"]
read_only_paths: ["<Path>plus-ui-namewta/src/components/**</Path>", "<Path>plus-ui-namewta/packages/domains/identity-access/**</Path>", "<Path>plus-ui-namewta/package.json</Path>"]
shared_paths: []
shared_path_owners: []
---

# Ticket T-11: 迁移 system-admin 资源与内容服务

- **工件：** Ticket `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/ticket/11-system-resource-services.md</Path>`；Map `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/tickets-map.md</Path>`；Spec `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/spec.md</Path>`；Evidence `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-11.md</Path>`。

## 1. 战略与来源

- **目标/产出：** 字典、配置、公告、OSS/OSS 配置、消息和社交能力完成 domain/web-domain 迁移并可独立定制表现层。
- **来源：** `US-004`、`US-005`、`AC-009`、`AC-010`、`AC-019`、`AC-021`、`ADR-002`、`ADR-003`。
- **当前事实：** 能力位于根 system API/views；OSS 上传/下载和消息处理已有安全/单元测试接缝，devtools 消费字典/菜单。
- **Planning Depth 原因：** 跨上传下载、富内容和共享字典合同，失败可能泄露资源或破坏多消费者。

## 2. 决策状态

### 已锁定决策

- transport/model/use cases 在 system-admin domain；Vue/Element 上传、编辑和页面在 web-domain/web-kit。
- 对 devtools 暴露最小 dict/menu public contracts，不允许其 deep import system 实现。
- 保留现有 OSS 安全校验、下载语义和社交 auth ClientContext。
- `DEV-T11-001`：仅可在 `src/router/adminManifestRegistry.ts` 为现有 system runtime 追加由宿主既有实现提供的 dict cache、OSS download、HTML/content security 与 Editor/ImagePreview typed ports；对应 registry test 只把 T-10 的 OSS 未选择夹具更新为资源页 selected 和无关 key unselected。不得改变 manifest 选择、resolver、根安全/组件实现、全局 router/request/App、root manifest/lock、identity 或 devtools。
- `DEV-T11-003`：前三次 candidate 均未推进 parent；第 4 次只可在专用 E2E fixture append `GET /system/dict/data/list` 与上传后 `GET /resource/oss/listByIds/8` 的具体响应、Client request 记录及既有 strict-unknown 空终态。禁止修改生产文件、locator、权限、endpoint、断言强度或无关 fixture；新 source 必须双轴 PASS，第 4 candidate 必须从未漂移 parent 重建并依次通过完整非浏览器 Gate、T11 `4/4` 与 full dual-App Playwright。
- `DEV-T11-004`：用户明确授权把 Speculo config 与 Goal Plan 的集成上限从 `4` 同步提高到 `5`。第 5 次只可把专用 E2E 的文件 input 与确认前同名文件可见断言限定到“上传文件”dialog；确认后列表、上传 transfer/complete、`listByIds/8`、Client request 与 strict-unknown 强断言保持。新 source 必须双轴 PASS，第 5 candidate 从未漂移 parent 重建并通过完整非浏览器 Gate、T11 `4/4` 与 full dual-App Playwright后方可推进。
- `DEV-T11-005`：`cfc09f4` 标准轴发现确认后 page-wide `.first()` 可能命中关闭动画中的旧 dialog 链接而伪绿。允许在专用 E2E 先记录确认前 OSS list request 次数，确认后等待 dialog hidden、list request 精确增加一次，并把新文件断言限定到 OSS table row；这只增强确认后结果证据，不改生产、endpoint、权限或其余强断言。新 fixed point 必须重新双轴 PASS。
- `DEV-T11-006`：用户明确授权把 Speculo config 与 Goal Plan 的集成上限从 `5` 同步提高到 `6`。第 6 次只可把专用 E2E 的确认前文件名断言改为 upload dialog 内具名 link 的明确单元素 locator；确认后 dialog hidden、list request 精确 `+1`、OSS table row、transfer/complete、`listByIds/8`、Client request、permission 与 strict-unknown 强断言保持。新 source 必须双轴 PASS，第 6 candidate 从未漂移 parent 重建并通过完整非浏览器 Gate、T11 `4/4` 与 full dual-App Playwright 后方可推进。

### 已采用的低影响假设

- 被多个领域复用且无 system 语义的 UI 才进入 web-kit；其余留在 system web-domain。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 七组资源服务、页面/组件归属、dict/menu public 合同、facades | T-10 system identity、T-04 adapters、既有 OSS tests | devtools 页面、全局 auth、后端/存储迁移 |

## 4. 要构建什么

管理员在选中 system-admin 的 App 中管理字典、配置、内容、对象存储、消息和社交连接；上传下载、权限和错误行为保持。其他 App 可仅复用 domain API 或不组合页面，且不需要复制业务模型。

## 5. 实现契约

- **入口/输入输出：** resource services、DictQuery/MenuQuery public ports、system manifest；现有 DTO/file/message 输入输出。
- **公共接口变化：** system resource exports 与最小 public contracts；旧 API/views facade。
- **不变量/数据流：** web -> domain -> adapters；file download/upload 保持鉴权、文件名与错误分类；devtools -> public port。
- **失败行为：** 非法 OSS URL/文件响应、无权限、Client 错误稳定失败，不触发不安全下载。
- **兼容/安全：** 不记录凭据/token；OSS/security 现有测试必须继续通过；旧 paths 到 T-15。

## 6. 执行路线

1. 盘点七组 API/pages、shared components、OSS/security tests 和 devtools 消费。
2. 提取 resource domain 与 dict/menu public contracts。
3. 迁移 Web 页面/组件并扩展 system manifest。
4. 旧 paths 变 facade，devtools 仍保持旧消费者待 T-13。
5. 跑 OSS/消息/社交失败测试、graph/type/build 和 Lead E2E，记录 Gate G5。

## 7. 路径访问契约

- **可写：** system 新包资源范围、七组旧 API/views、专用 E2E；**只读：** root shared components、identity、root config。
- **共享路径：** 无；需要移动 shared component 时须在 Goal Plan 先明确转归路径 owner。
- **保留或不动：** devtools 调用点、T-07 auth/shared router、后端。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | service/page E2E | dict/config/OSS/message 核心路径 | API/页面/上传下载可用 | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-11.md</Path>` |
| 失败路径 | security/unit | 非法 URL/响应、无权限、Client 错 | 安全失败且无敏感泄漏 | 同上 |
| 回归 | graph/type/build | public contract + 双 build | 表现可分离，旧消费者兼容 | 同上 |

- **Workspace checks：** source-worktree/current-workspace 运行 unit/security、architecture、lint、typecheck、双 build。
- **E2E disposition：** required：上传下载、社交 Client 和管理页面跨浏览器/网络边界。
- **E2E owner/environment：** Lead / parent-candidate 或 current-workspace；覆盖 OSS、字典和消息关键路径。
- **Integration evidence：** source commit、parent before、candidate/result SHA、Gate G5。

## 9. 发布、迁移与恢复

- **迁移/兼容：** domain/public expand -> web migrate -> facades；旧入口至 T-15。
- **监控/回滚：** OSS/security、message、missing key；manifest 回切旧 views。
- **批准点/收缩：** Gate G5 后允许 T-13；旧 imports T-15 清零，public dict/menu 保留。

## 10. 验收标准

- [x] `AC-009/AC-010`：system 资源职责可追踪且选择性注册。
- [x] `AC-019`：App/web-domain 可定制表现而不复制 domain。
- [x] `AC-021`：安全、单元、build、E2E 真实通过，失败保留旧入口。
- [x] 公共合同、commit/candidate/result SHA 写入 `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-11.md</Path>`。
