# T45：受限身份 OSS 诊断的真实验收准备

准备时固定 HEAD：`4c93a2fb9c7d2c1d68a0c8194197e2af1908b4d2`。仅根据当前 Ticket 45 / AC-045、`/tmp/wta-t45-activation-audit.md` 及已验收的 T44 当前源码制定；T45 产品尚未实现。本文件不代表运行或通过，且不读取任何部署凭据。

## 可复用的隔离底座

- `/tmp/wta-t44/run-minio-real-v8.py`：已有 full JAR/package-proof、exact clean source、owned MySQL/Redis/MinIO、六 SQL 103 表、管理员/普通用户真实登录、私有 mc alias、限定 access key、三 owned 桶/对象、loopback 计数代理、健康/API/OpenAPI和双标签完整容器 ID/匿名卷/PGID/端口清理。其 MinIO 固定 digest 和 0600 私有文件做法可沿用；原 v8 仍明确允许 `UNVERIFIED`，不能拿原通过结果替代 AC-045。
- 当前应用路由已有受 `system:ossConfig:list` 保护的 `POST /resource/oss/config/diagnose/{ossConfigId}`；当前 VO 仅 `status/reason/checkedAt`。T45 的新事实字段与前端 `oss-config` 交互仍待固定实施合同。普通无权限用户 403、诊断不进入正常下载门禁可复用 T44 断言。
- `/tmp/wta-t44/run-oss-seven-integration.py` 的独立七类 MinIO/MySQL/Redis runner可复用 0600 `surefire.systemPropertiesFile` 与 fresh XML/零 skip/资源卫生，但这七类多数仍用 bootstrap 身份；只用于兼容回归，不是受限诊断的验收。

## T45 独立真实矩阵

1. **隔离 setup（非诊断计数）**：在本次 owned MinIO 创建 PUBLIC_READ 与 PRIVATE 两桶，分别写入固定字节的诊断对象。bootstrap 身份创建公开对象读取策略并在最后删除；有限身份只拥有这些对象的 HEAD/GET 和测试所需的基础 bucket 查询能力，不拥有 GetBucketPolicy/GetBucketAcl、写策略/ACL或跨桶管理权限。以有限身份正控对象 HEAD 成功、GetBucketPolicy/GetBucketAcl 各 403；bootstrap 在 setup/cleanup 使用的 PUT/DELETE 与诊断阶段计数分开。
2. **匿名真实观察**：公开对象无凭据 HEAD 2xx 与 Range GET 200/206，私有对象无凭据 HEAD/GET 403。记录状态类别、对象别名、是否含 Range；不记录对象键、URL、Authorization、策略正文或响应体。404、5xx、超时和跳转另在有界 mock/代理故障用例中证成 UNKNOWN，不能算 DENIED。
3. **同一有限身份的应用诊断**：fresh 六 SQL 库将两个配置指向 owned proxy/对象；真实 Admin 登录后分别显式调用受权限保护的诊断 API。PUBLIC_READ 应给该对象读取 ALLOWED、Policy/ACL UNKNOWN、匿名写 UNKNOWN，不能给确定 `POLICY_MISMATCH`；PRIVATE 的对象读取 DENIED 但 Policy/ACL、匿名写与全桶安全仍 UNKNOWN。每个事实须有 source/scope/checkedAt，整体验证不能超出事实范围；普通用户对相同 POST 必须 403。诊断后正常已授权下载仍按预期访问类型工作，诊断 UNKNOWN 不授予任何新访问。
4. **代理请求证明**：在每次诊断前后取安全计数快照，按 S3 operation 将 `GET ?policy`、`GET ?acl`、签名对象 HEAD、匿名对象 HEAD、匿名 Range GET 分开，并分别记录 403/2xx；只用布尔 `Authorization` 是否存在，不保存 header 值。诊断 delta 中 `PUT/POST/DELETE=0` 且无匿名 mutating 请求；仅测试 bootstrap setup/cleanup 可以写/删。策略/ACL 403 是真正供应商响应而不是 mock；Policy 404 是独立状态，不能与 403 混淆。
5. **解析器/失败回归**：同次源码的 `OssAccessDiagnosticUnitTest` 对 `Resource` 不相关、`Condition`、显式 Deny + Allow、未知 Principal/Action、坏 JSON 断言不越权推断；PUBLIC_READ 403+403+匿名可读和 PRIVATE 403+403+匿名拒绝均保留部分事实。不使用真实业务桶或匿名 PUT/DELETE 测写权限。现存七类 OSS 真实测试可按影响范围另跑，不把 default 环境 skip 算通过。
6. **真实 HTTP/UI**：等待 T45 固定 DTO/transport/UI 选择器后，以 Admin `dist` + owned full app + Playwright Chrome 打开 OSS 配置页面，点击真实诊断动作；页面必须同时表达对象读事实与策略/写未知，不显示“全桶安全”“匿名写已禁止”，切换配置后旧响应不能覆盖新结果。普通用户页面/API不因前端隐藏而越过后端权限。浏览器不存 storageState/trace/HAR/video、含凭据 screenshot；真实诊断 HTTP 不由 route mock 替代。若 DTO 改动进入 OpenAPI，则由完整同源 JAR 实际捕获并更新生成物，不拼旧快照。

## 私有 runner 的固定执行边界

最终 runner 必须显式 `--execute --expected-head <40hex> --expected-jar-sha256 <64hex> --package-proof <私有记录>` 才能启动，先验证 clean HEAD/tree、full package proof、三 App dist 清单；服务只用随机 `127.0.0.1` 端口、owned 双 label/完整 ID，三容器和匿名卷逐名回收。MySQL/Redis/MinIO 凭据只进 0600 私有 env/config；应用 overlay 覆盖全部外部地址并禁用外部供应商、Snailjob；Java/Node/Chrome 子进程按本次 TMPDIR/PGID 精确清理。私有结果仅保存精确 source/JAR、计数、状态枚举与脱敏 XML/安全浏览器 reporter；原始策略、AK/SK、登录 token、对象键、签名 URL、HTTP 正文与 raw logs 不进入可提交 Evidence。候选一次运行失败保留并诊断，不自动重试或弱化断言。

## 驱动待定输入与不能提前称真的内容

- T45 真实 MinIO 测试类名、opt-in 属性及是否复用现有 `OssStorageReadinessMinioIntegrationTest`，需在 writer 冻结源码后精确绑定；现有类用 bootstrap 身份，不能证明 403 受限身份。
- `OssAccessDiagnostic`/管理 VO 的字段名、三值枚举、source/scope 形状、最终 `status/reason` 兼容映射；当前仅有三字段管理 VO，任何预先硬编码的 JSON 断言都可能是假合同。
- 前端按钮/行选择器及页面负向文案、OpenAPI 新 schema；当前 `OssConfigPage.vue` 只有访问类型编辑，无诊断展示。
- 代理如何在不保留 URL/键的情况下分类 `?policy`/`?acl` 与对象 Range：需新私有安全聚合 hook；不能只使用 T44 v8 的粗 `method` 总数证明 T45。

因此本次先冻结隔离方案。具体可执行私有脚本在 T45 首个 test-only 红灯或 DTO/UI 草案落盘后，以 T44 v8 的现有卫生函数窄适配并作纯离线合成安全测试；Lead 独占运行真实服务。本稿不启动 Docker、Maven、pnpm 或浏览器。
