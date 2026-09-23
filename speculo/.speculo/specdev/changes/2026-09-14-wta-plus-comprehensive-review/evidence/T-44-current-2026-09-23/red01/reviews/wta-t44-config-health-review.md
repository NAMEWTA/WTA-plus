# T-44 配置初始化、诊断属性与 health 边界只读审查

固定产品输入 `8db922e1971b4781b2b53f8db837c06f7b60c4e7`，对照 Ticket44、`/tmp/wta-t44-writer-plan.md`、`/tmp/wta-t44-lead-plan-notes.md` 与此前 `/tmp/wta-t44-http-path-registration.md`。本报告是实施前契约/风险审查，不称当前源码已满足 AC-044；未改仓库，未构建、测试或启动服务。以下文件位置均按该固定提交读取。

## 当前可达阻断

1. **坏的可选属性仍会阻断核心启动。** `OssStorageReadinessProperties.java:23-27` 将 timeout、refreshInterval、maxSnapshotAge 直接绑定成 `Duration`；不可转换文本在 Spring 绑定时已失败，`:30-48` 的 `afterPropertiesSet` 无机会做降级。可转换但 0/负数/超界、坏 diagnosticObjects 目前也直接抛异常。另 `OssStorageReadinessService.java:39-40` 的 `@Scheduled` 直接解析 `oss.readiness.refresh-interval`，即使属性 bean 容错，坏文本仍可能使调度装配失败。T44 应移除默认全量巡检的这个注解；可选诊断设置以安全有界默认继续装配，单配置诊断返回固定无敏感原因，不让原始绑定异常/对象键进响应或日志。只修改 `afterPropertiesSet` 不足以关闭不可转换字符串反例。
2. **默认指针可因启动中途失败而保留或复活。** `SysOssConfigServiceImpl.init():53-68` 对 0/2 个默认立即抛 `ServiceException`；恰有一个默认时逐行校验，并在遇到默认行时立即写 Redis `DEFAULT_CONFIG_KEY`，之后坏非默认行仍会抛。若上次 Redis 已有旧默认而本次 DB 空/重复/坏默认，当前代码不会清它。`OssFactory.instance()` 与 `RedisOssDefaultStorageKey.current()` 都直接读此键，所以不能只忽略异常继续启动。最小契约是：DB 查询失败仍按核心依赖故障失败；DB 可读时先取消旧默认指针并核全部候选，只在**恰好一个有效 PRIVATE 默认**时写新指针；0/2/坏默认时指针保持空，默认上传本地失败。坏非默认仅隔离其已知缓存与客户端，不阻断核心；其他有效的历史 service 仍可被独立读取。管理新增/切换的唯一 PRIVATE 默认校验不能随启动容错一起放宽。
3. **启动和提交回调仍主动访问远端 OSS。** `SystemApplicationRunner.java:29-30` 在 `init()` 后执行全量 `readinessService.refresh()`；后者 `OssStorageReadinessService.java:43-56` 扫所有配置、历史对象和 contributor，并逐配置诊断。`OssConfigChangeListener.java:50-51` 在 after-commit 监听的 `finally` 再做同一远端全量刷新；因此一次配置保存可能在提交后被慢/失败的可选诊断拖住。`OssUploadDiagnostics.java:33,47,52` 的 ApplicationReadyEvent 还单独调 `bucketConfiguration()`；只去 runner 的 refresh 仍不能证明启动远端 I/O=0。最小契约：启动仅 DB/必要缓存初始化；管理显式诊断只碰一个配置；提交回调保留目标配置缓存/OssFactory 失效与默认指针更新，仅使相关诊断事实失效，不同步全扫/调用 Provider。调度不能随 OSS 巡检删除而关闭：当前无条件 `@EnableScheduling` 只在 `OssStorageReadinessSchedulingConfiguration`，而 `NotifyOutboxWorker.poll()` 用 `@Scheduled` 做 Redis wake 丢失后的 60s 默认兜底；应将无条件启用移到常驻应用配置，并以真实 full-app fallback 测试验证。
4. **现有 health 是全局聚合风险，分组必须测 HTTP。** `OssStorageReadinessHealthIndicator` 以 `ossStorageReadiness` 名称注册，registry 空或未发现时返回 DOWN；`application.yml` 只有 `management.endpoint.health.show-details=ALWAYS`，尚无显式 core/OSS 组。即使业务移除诊断门禁，默认 aggregate health 仍会受该 Indicator 影响。T44 应显式形成核心 readiness/liveness 与独立 OSS 诊断组，并在实际配置/探针文档中采用核心组路径；不能只单测 Indicator 或称原 `/actuator/health` 已隔离。当前后端 Compose healthcheck 仅 TCP 8080，不能把它算作 HTTP core readiness 实测。Actuator 路径由现有 Basic Auth 过滤器保护；HTTP 组测试仍须核实际状态与去敏详情。
5. **配置的 3s 不是单次诊断总时限。** `OssStorageReadinessService.java:96` 把一个 `diagnosticTimeout` 传给 common OSS；`AbstractOssClientImpl.diagnoseAccess():1018,1028,1039,1053,1109-1117` 顺序执行对象 HEAD、策略、ACL、匿名 HEAD 与 GET，分别使用同一 timeout。因而当前默认 3s 可累计多次，属性允许 30s 也不等于请求最多 30s。实施应冻结**整个管理员接口**的可观测上限并用连续慢响应验证，或明确把可接受总上限按多步调用计算并限制配置/并发；不能只断言 `getDiagnosticTimeout()<=30s` 就声称 HTTP <=30s。若合同要求严格单个总 deadline，现有 common OSS 方法不提供此保证，需先扩该精确路径与测试；不要在 readiness service 中无清理地起后台线程来制造表面超时。

## 最小可测试合同与隐蔽负例

| 接缝 | 应固定的正/负断言 | 最小证据 |
|---|---|---|
| 属性真实绑定 | 用 Spring 实际 `@ConfigurationProperties` 装配测试不可转换 Duration（如非时长文本）、0、负值、超 30s、maxAge 矛盾、null/坏 diagnosticObjects；核心 context 仍启动，诊断参数回安全默认或该 key 固定无敏感失败原因；测试日志/响应不出现输入 canary。既有 `OssStorageReadinessPropertiesUnitTest` 目前期待异常，须改为新合同并保留有界负例，不能删除。 | admin OSS 定向 context 测试，另 full app 带坏可选属性启动/核心 health HTTP；诊断不触发远端。 |
| DB/Redis 默认 | fresh DB 空表、2 个 status=Y、唯一 PUBLIC_READ 默认、唯一坏 PRIVATE 默认、唯一有效 PRIVATE 默认+坏非默认（两种行排序）；每种先在 owned Redis 放旧 `DEFAULT_CONFIG_KEY` 与已知坏行缓存。前四类启动成功且默认键为空；最后一类只指向有效行，坏行缓存/客户端失效，合法历史非默认可用。不能用旧 Redis key 补造默认；DB select 真失败仍启动失败。 | 扩 `OssConfigGovernanceUnitTest`、真实 MySQL/Redis full app；验证默认读取及一次真实默认上传入口，而不只 mock `init()`。 |
| 启动/变更远端零调用 | full app 的空/坏可选配置、MinIO 离线/慢、缺 canary 场景：启动期 `diagnoseAccess`、`bucketConfiguration`、其他 OSS 远端计数为 0；登录/核心 health/IN_APP 可用。配置保存/切换提交后缓存与 `OssFactory` 失效保持，诊断快照仅相应 key 失效，Provider 故障不把已提交管理操作伪报失败；回滚事件不执行失效。 | 定向 listener/runner 用例 + owned full app。特别覆盖 `defaultConfig` 事件旧代码 `return` 分支，确保新诊断失效逻辑不被跳过。 |
| health/调度 | MinIO DOWN、无 canary、诊断过期时，带现有 Basic Auth 的实际 `/actuator/health/<core-group>` 为可用，而独立 OSS 组反映不可用/未验证；无权访问按既有 filter 拒绝。关 Redis wake 后提交 IN_APP Outbox，靠常驻 `@Scheduled` poll 落地；只看 `@EnableScheduling` 反射不足。 | full app HTTP + Notify 真实 MySQL/Redis fallback；记录具体组名/路径与 Compose TCP 探针差异。 |
| 单配置诊断/超时 | 管理权限正反例、仅目标 ID/键一次诊断、他键快照不变；缺配置/坏 policy/缺 canary/真实超时返回固定 status/reason/checkedAt，审计 request/response body 关闭，日志无 key、secret、endpoint、签名、Provider 原文。对连续 5 个慢步骤测**总**响应上限而非单 SDK 调用 timeout；失败不污染核心 health。 | Controller HTTP + 慢 OSS stub/最小权限 MinIO；已登记的单 VO/OpenAPI 生成链路见前一报告。 |

现有 `OssStorageReadinessHealthUnitTest` 只直调 Indicator 并检查固定详情，不能证明 Spring health group 装配；`OssConfigGovernanceUnitTest.initializationFailsClosedWhenDefaultInvariantIsBroken` 与 T44 新启动容错目标相反，需用“默认键失败关闭、核心可启动”替换目的，保留管理端严格校验用例。当前 `OssStorageReadinessRegistry.requireServing` 仍被下载、直传、迁移引用；本报告只审 config/health，但最终 T44 必须按已接受业务合同移除这些业务门禁，不能用 health 分组代替调用链修改。

## 需要 Lead/Writer 事先冻结的窄点

- 诊断上限是 **per-step** 还是 **whole-request**：源码只证明前者；若按 whole-request 承诺，需要 common OSS 精确写集及真实慢调用测试，先登记再实施。无需为了本票引入新诊断平台。
- 旧默认缓存清除应在本次 DB 事实判定前后可证为单调：不能让旧键在“空/重复/坏默认”之后继续可读，也不能在 DB 查询失败时谎称可选存储问题。对已知坏行同时移除缓存与客户端。
- 分组名称/探针使用由 full app HTTP 验证并写进部署说明；默认聚合仍可能 DOWN，不能把 Compose TCP 绿灯或 Indicator 单测称为核心 HTTP readiness。可选诊断配置不应决定核心组创建是否成功。

未验证项：此轮未执行任何 Maven、Docker、HTTP 或服务测试；上述是固定 8db922e1 源码事实及可运行的下一步断言，不是 T44 通过证据。
