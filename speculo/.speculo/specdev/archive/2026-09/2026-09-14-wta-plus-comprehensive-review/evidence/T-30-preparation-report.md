# T-30 可逆验收准备

当前只完成准备；正式整体验收 not-run，AC-030 未勾选。T-03 请求预算与 T-23 供应商回调合同仍阻塞，所有提交按用户指令暂缓。没有 implementation_commit、result_sha 或发布候选；未推送、部署或操作生产数据。

## 已实际完成

- 核对 T-29-checkpoint-v3 与其上游合并的589条路径，全部一致；HEAD仍为76dbbe84a34624234e379661a57b232529e34ed3，暂存区为空。
- 捕获3549条产品/工具源码路径及删除记录，形成 T-30-source-checkpoint.json 工作树指纹。它包含未提交源码，不能当作已验证发布候选。后续产品变化后必须重新捕获并重跑受影响门禁。
- 实际执行10份现有Playwright配置的 `--list` 枚举。初次有8份exit 0，Profile/transfer因缺隔离Origin在导入时exit 1；失败JSON原样保留。给这两份配置补入仅供枚举的 `http://127.0.0.1:9` 参数后，2份均exit 0。没有启动浏览器、产品服务或执行用例。
- 最终枚举163个用例：默认54、accessibility29、lifecycle20、Profile8、registration10、SSO19、transfer5、transport6、upload10、SSO security2。枚举数不是通过数。workflow-task-integrity.spec.ts另外由原T-16 driver生成临时配置，未被这10份持久配置覆盖。
- 现有真实服务CI脚本选择7个测试类；只读扫描找到47个读取系统属性的测试源码文件，其中包含环境门控夹具及辅助测试。这个数字不是用例数，不能声称所有47个都在CI执行。
- 回读T-03/T-23当前17个源文件并保存哈希，纠正旧快照中已删除解密包装器及回调事务/锁协作描述。未决合同仍保留原责任票，见 planning-blocker-source-review-current.md。

准备命令、cwd、退出码、配置和原始结果见 T-30-browser-discovery.json、T-30-browser-discovery-fixture.json。prepare-t30-inventory.py实际exit 0，输出 T-30-preflight-inventory.json；其内所有产品验收命令状态仍为not-run。

## 正式验收执行边界

1. T-03/T-23合同及实现闭合后，重新核对全部30个前置票、来源源码与哈希。当前28张review不是Done，也不自动满足direct-parent提交出口。
2. 按inventory的16条核心命令串行运行前端architecture/architecture tests/OpenAPI/lint/typecheck/unit/dev/prod/default E2E、后端默认测试、full打包和内容验证、core打包和内容验证、发布合同、真实服务脚本。full必须在core clean前验证并保留产物digest。默认后端环境skip单列，不能抵扣真实服务要求。
3. 追加下表中的真实服务及浏览器专用场景；每个required类须有本轮新鲜、非零、零失败/错误/skip的报告。默认浏览器的Nacos条件skip也不代表真实Nacos门禁通过。
4. 每组前后检查源码指纹，保留命令、配置来源、实际测试数、失败日志、产物SHA和资源恢复结果。历史driver先适配独立T-30输出路径及清理，再执行；不得覆盖T-xx已有证据。
5. 只创建并清理本轮拥有的本地容器、端口、文件及测试数据库；不连接或重启现有服务。生产批准、正式发布与真实运行数据不属于本轮准备。源提交暂缓期间不产生可批准发布候选。

## 需要明确追加的场景

| 验收面 | 现有入口 / 责任票 | 尚需在最终源码重新执行的范围 |
|---|---|---|
| 入口限制与审计 | T-02/T-03/T-04/T-05 | 实际HTTP边界/签名/413/上传/SSE/取消；脱敏canary、可信代理链、Redis租约竞态；T-03尚缺实现 |
| SSO与三Origin | T-06/T-07/T-08/T-11/T-12/T-13/T-21；playwright.sso及security/transport/lifecycle/registration | 真实HTTPS Cookie/PKCE/单次兑换、会话失效、跳转、跨Client和清理；SSO配置没有webServer，需原隔离服务fixture |
| Profile与对象存储 | T-14/T-15/T-20；playwright.profile/upload | 真实MySQL/Redis/MinIO与HTTP、材料归属/私有访问/取消恢复；普通profile-management mock页面不能替代self链路 |
| Workflow | T-16；WorkflowTaskEngineIntegrationTest及run-t16-dialog-browser.py | 实际WarmFlow动作/权限、失败与弹窗状态；临时Playwright配置需保留新证据 |
| UI与注册 | T-17/T-18/T-19/T-21/T-31；accessibility/registration/transfer | 权限呈现、异步乱序、桌面/移动键盘与a11y、QUEUED状态；有mock场景须明确边界 |
| Notify | T-22/T-23/T-28/T-31 | 真实MySQL租约fence/过期接管、回滚、Outbox唤醒及排队消费；供应商双实例/重启回调幂等依赖T-23，不能用dispatch测试代替 |
| Third | T-24；ThirdResilienceRecoveryIntegrationTest、ThirdLimitConfigurationIntegrationTest | 独立JVM崩溃/重启、Redis丢失恢复、真实配置与限额；现有CI的ThirdRedisIntegrationTest不等于全部恢复场景 |
| 部门/Demo树与HTTP | T-25/T-26/T-27；DepartmentTreeMySqlIntegrationTest、DemoTreeMySqlIntegrationTest、CrudMySqlHttpIntegrationTest | 隔离MySQL真实事务/权限/并发、旧方法拒绝、代表CRUD、生成合同；模板实际渲染/编译/MySQL另行执行 |
| 发布与恢复 | T-08/T-09/T-10/T-29；release-artifacts/tests与各本地隔离driver | 六文件新库基座、三App/Origin、source/digest、stage失败不污染、恢复演练及Nacos真实门禁；没有新增存量迁移工程或线上操作 |

## Skill Execution Records

| Skill / phase | 本轮实际范围 | 状态 |
|---|---|---|
| engineering-standards / implement | 按测试隔离/质量/交付规则核对源码哈希、枚举命令、真实服务选择器和full/core顺序 | preparation-only |
| namewta-fullstack-development / implement | 按验证矩阵区分API/数据库/权限/浏览器/发布证据，识别默认命令未覆盖场景 | preparation-only |
| wta-module-guide / implement | 回读现有模块入口与责任票，定位实际测试owner，不改业务合同 | preparation-only |
| engineering-standards / verify | 整体产品测试、失败恢复、正式候选与全部AC尚未运行/完成 | not-run |

没有将Skill阅读、配置枚举、历史票验证或准备脚本exit 0冒充T-30产品验收。下一步需要关闭T-03/T-23事实并完成责任票；提交仍继续暂缓。治理结果见T-30-preparation-governance.json。

Revision116更新：以上589/3549快照为准备时历史值，T-29检查器补修后已失效。下一次T-30准备/运行须从T-29-checkpoint-v4及其上游593路径重新捕获源码；核心命令追加手册检查器及8项回归，不重用旧指纹为最终验收。用户已授权T-03先用通用初值、T-23常见供应商预置，原等待答复不再适用，责任票合同/实现仍须完成。

Revision119：T-03本地review后累计产品检查点610路径。旧T-30源快照与准备清单仍不是当前验收候选；T-23完成后从T-03检查点叠加重建。T-03预算不再是用户等待，正式T-30验收仍未运行。
