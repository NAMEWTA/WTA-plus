# T-44 单配置管理员诊断 HTTP 路径与事前登记核查

本次只读核查基于固定源码 `6c3dbdbd9900ae41fde5ac047aeaa0f5c482e66a`、`/tmp/wta-t44-writer-plan.md`、`/tmp/wta-t44-lead-plan-notes.md`。未修改仓库，未运行构建、测试或服务。T-40 工作树后续变化不作为本报告产品事实。

## 结论

新增单配置管理员诊断 POST 在现有 `SysOssConfigController` 下即可实现；现有 OSS 配置页注册与 `system:ossConfig:list` 资源权限已覆盖访问面，无需为纯 HTTP 管理诊断新建前端 route/菜单。**Ticket 当前写集尚未覆盖 Controller、推荐的 HTTP 响应 VO 和 OpenAPI 生成三处；必须先登记再写/生成。**

建议冻结为 `POST /resource/oss/config/diagnose/{ossConfigId}`，参数只用经验证的数字配置 ID。服务端根据 ID 取目标配置，再仅诊断该配置；不要让 configKey、endpoint、凭据或预签名 URL 进入 path/query/body。该静态二级路径与现有 `GET /resource/oss/config/{ossConfigId}` 及现有 POST 路径无映射冲突。项目 API-005 要求 POST 使用 `@Log`，推荐 `@SaCheckPermission("system:ossConfig:list")`、`@Log(title = "对象存储诊断", businessType = BusinessType.OTHER, isSaveRequestData = false, isSaveResponseData = false)`。若 Lead 选择更严格的既存 `system:ossConfig:edit`，应在票内固定并以同一权限做负向 HTTP 测试；不要创造未声明权限。诊断会更新诊断快照，使用 POST 合适。

`@Log` 两个保存开关默认均为 true（`Log.java`），必须显式关掉；`LogAspect` 当前把最佳匹配路由模板写入 `operUrl`，关闭开关后不序列化请求/响应，但异常仍走 `LogSanitizer.failure`。因此响应和异常须仅含固定 status/reason/checkedAt，不传原始 provider 错误、配置或 URL。现有 `SysOssConfigVo` 含 accessKey、endpoint、domainUrl 等管理配置字段，`OssStorageReadinessEntry` 含 configKey/requiredBy，不宜复用为诊断 HTTP 响应。

## 单 DTO 与资源映射

按 System classic 模块的 `domain/vo` 布局，建议只新增 `backend/wta-modules/wta-system/src/main/java/org/namewta/system/domain/vo/OssStorageDiagnosticVo.java`，字段仅 `status`、固定枚举 `reason`、`checkedAt`，由 Controller 从内部 readiness entry 显式投影，响应 `R<OssStorageDiagnosticVo>`。这是一个 HTTP 输出 DTO；不需要请求 BO，因为 path ID 足以定位单配置，也不需要修改 `ISysOssConfigService` 或 `wta-api`。Writer 原稿提出把返回 DTO 放已授权 `oss/readiness/**`；该目录适合内部诊断模型，但 HTTP VO 放 `domain/vo` 更符合现有 System classic 目录和 BE-CRUD-006，需额外精确登记此一文件。若 Lead 固定内部 readiness 包方案，应确保只输出单独的去敏 DTO，绝不直接返回 `OssStorageReadinessEntry`；届时该新增文件已被现有 `oss/readiness/**` 覆盖。

`frontend/packages/web-domains/system/src/index.ts` 已登记 `system-oss-config` 页面组件，并声明 `ossConfig: ['list','query','add','edit','remove']`；`frontend/packages/domains/system/src/resource-service.ts` 已有 OSS 配置 CRUD 传输，但没有诊断调用。当前方案只要求管理员 HTTP 入口，未要求页面按钮，故这两个前端源码文件均无需修改。若后续决定页面触发，再事前扩对应 domain service、web-domain 页面/测试写集，不能把 OpenAPI 类型生成视为已接入页面。

## 事前精确写集

Ticket 当前 `writable_paths` 已覆盖 `backend/.../oss/readiness/**`、admin OSS 测试根等。针对本接口与生成链路，需在实施前**新增**：

1. `backend/wta-modules/wta-system/src/main/java/org/namewta/system/controller/system/SysOssConfigController.java`（writer 四项之一）。
2. `backend/wta-modules/wta-system/src/main/java/org/namewta/system/domain/vo/OssStorageDiagnosticVo.java`（若采用上面的单 VO；不可把整 `domain/vo/**` 宽泛授权）。
3. `frontend/packages/api-contracts/openapi/current.json`。
4. `frontend/packages/api-contracts/openapi/revisions/`，仅新不可变 revision 的 `source.json`、`provenance.json`；实际 64 位 revision 在 fetch 后确定，保留旧版本。
5. `frontend/packages/api-contracts/generated/openapi.ts`。

其余 writer 已识别的 `SysOssConfigServiceImpl.java`、`OssConfigChangeListener.java`、`NamewtaApplication.java` 为 T-44 其他生产切片必要扩写集，但不是本次 HTTP/生成产物的增量判据。`frontend/packages/api-contracts/src/index.ts` 已从 `../generated/openapi` 通用导出 `paths/operations/components`，本次无需编辑；`README.md` 已要求通过 current 指针/对应 provenance 找来源，无固定 SHA 漂移，无需为此编辑。新接口及 VO 会改变完整 `/v3/api-docs`，当前快照只含六个既存 `/resource/oss/config...` 操作，尚无 diagnose 路径。OpenAPI 生成物不能手工修改。

## Lead 后续生成与检查命令（此处未运行）

先用最终固定 backend source 构建的 owned full JAR 启动隔离实例，并从该实例真实 `/v3/api-docs` 抓取完整原文；记录 JAR 摘要、构建 source SHA、端口和 cleanup。再在 `frontend/` 运行：

```bash
pnpm --filter @namewta/tooling-openapi openapi:fetch -- --source /tmp/<owned-live-capture>/source.json --backend-commit <exact-40-char-backend-source-sha>
pnpm --filter @namewta/tooling-openapi openapi:generate
pnpm --filter @namewta/tooling-openapi openapi:check
pnpm --filter @namewta/api-contracts typecheck
```

`openapi:fetch` 写新 revision 的 `source.json`/`provenance.json` 和 `current.json`；`openapi:generate` 写 `generated/openapi.ts`；`openapi:check` 只读校验。工具不会验证命令中给出的 backend commit 是否真的对应 JAR，Lead 需用构建记录与摘要证明来源。检查新 OpenAPI path 只有预期 POST、响应 schema 仅安全三字段，旧 path/schema 不丢失，并用真实有权/无权 HTTP 测试核权限、单配置定位、审计日志去敏及固定错误类别。

依据：`SysOssConfigController.java` 现有映射/权限，`Log.java` 与 `LogAspect.java`；`frontend/packages/web-domains/system/src/index.ts`；`frontend/packages/domains/system/src/resource-service.ts`；`frontend/tooling/openapi/src/index.mjs`、`README.md`、`frontend/packages/api-contracts/AGENTS.md`；工程规范 API-005、BE-CRUD-006、FILES-002、SEC-003。
