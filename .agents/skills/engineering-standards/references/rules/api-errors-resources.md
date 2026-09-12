# API、错误、资源与并发

### API-001 边界验证与可信模型

Scope: HTTP/JSON, browser storage, environment configuration, database and third-party integration boundaries

Level: MUST

Source: `repository-fact` + `builder-baseline`

Rule: 外部输入先验证、标准化并转换为 BO/DTO/VO 或精确 TypeScript 类型，再进入业务逻辑。类型断言、Bean 映射和前端 interface 不能替代运行时验证；非法状态不得靠 magic string 或宽泛 map 隐藏。

Verification: review controller validation groups、service invariants、前端 response narrowing 和错误分支；相关边界测试；两端构建。

### API-002 错误合同

Scope: `repository`

Level: MUST

Source: `repository-fact`（`GlobalExceptionHandler`、前端 platform HTTP 合同、axios browser adapter）+ `builder-baseline`

Rule: 只在能恢复、映射协议或增加边界上下文时捕获错误；保留原始 cause。用户消息、机器错误码和运维日志分离，不吞异常、不把日志当成功返回，也不在多层重复记录同一失败。

Verification: 失败路径测试；review catch/Promise rejection/exception mapping；检查日志不含 secret、SQL、Token 或过量个人数据。

### API-003 超时、取消与重复响应

Scope: HTTP clients, async tasks, schedulers, listeners and batch operations

Level: MUST

Source: `repository-fact` + `builder-baseline`

Rule: 异步 I/O 明确超时、取消、幂等、重试和部分失败语义；前端避免卸载后写入和过期响应覆盖；后端不吞中断，不无界重试，不在事务中等待无界远程 I/O。

Verification: 超时/取消/重复提交测试或精确人工步骤；review Abort/cleanup、retry 上限、事务范围与 interrupt 处理。

### API-004 资源所有权

Scope: database transactions, streams, files, executors, schedulers, listeners, observers, timers, object URLs and subscriptions

Level: MUST

Source: `builder-baseline`

Rule: 每项资源都有创建者、所有者、关闭/取消路径和部分初始化失败清理；Spring singleton 不保存请求可变状态，Vue 生命周期资源在 unmount/scope dispose 时释放。

Verification: review try-with-resources/Bean lifecycle/Vue cleanup；资源失败和重复关闭测试；检查线程池、timer、listener 未在请求或重复 mount 中泄漏。

### API-005 CRUD 查询 GET、变更 POST 与日志追踪

Scope: 前后端 HTTP CRUD 接口及其生成模板；包括列表/分页、详情、树/下拉等只读查询，以及新增、修改、删除、批量删除、状态变更、排序更新等变更操作

Level: MUST

Source: `user-decision`

Rule: 按业务语义统一 CRUD method：不产生业务状态变化的列表/分页、详情、树和下拉等查询只使用 HTTP `GET`，前端显式使用 `method: 'get'`，后端使用 `@GetMapping`，参数只放 path 或 query，不使用 GET request body。新增、修改、删除、批量删除、状态变更和排序更新等产生业务状态变化的操作只使用 HTTP `POST`，前端显式使用 `method: 'post'`，后端使用 `@PostMapping`；禁止以 `PUT`、`PATCH`、`DELETE` 或对应的 `@RequestMapping(method = ...)` 定义 CRUD 操作。每个 POST 业务接口必须使用项目的 `@Log` 注解记录调用追踪，设置可识别的 `title` 和准确的 `BusinessType`：新增用 `INSERT`，修改/状态/排序用 `UPDATE`，删除用 `DELETE`，其他操作使用与语义匹配的现有枚举。请求或响应含凭据、Token、个人敏感数据或大对象时，使用 `excludeParamNames`、`isSaveRequestData = false` 或 `isSaveResponseData = false` 控制落库内容，不得为了追踪泄露敏感值。每个操作的 URL 必须无映射冲突，并保持前端、controller、测试和文档完全一致；HTTP method、`@Log` 不能替代 Bean Validation、权限、幂等或数据权限。导入导出、登录/OAuth 回调、文件流、WebSocket、SSE、健康检查及第三方协议适配等非 CRUD 接口按自身合同选择 method，但使用 POST 时同样必须配置安全的 `@Log` 追踪。

Verification: 对受影响合同逐项核对 list/detail/tree/options/add/update/delete 及派生操作；前端确认只读查询为 `get`、变更操作为 `post`，后端确认对应使用 `@GetMapping`/`@PostMapping`，CRUD 中不得出现 `@PutMapping`、`@PatchMapping`、`@DeleteMapping` 或对应 `RequestMethod`。逐个检查 `@PostMapping` 业务方法紧邻有效的 `@Log`，`title`、`BusinessType` 与敏感字段排除配置准确；执行前后端契约/集成测试。修改生成能力时同时检查 Java controller 与 Vue API 模板，生成代表性分页和树表 CRUD 后重复上述检查。
