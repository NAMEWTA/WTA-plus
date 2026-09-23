# T-44 Dispatch01 — 诊断与业务解耦

## 权威、基线与所有权

Ticket `ticket/44-optional-oss-diagnostics.md`；Goal `goal-plan.md`；已接受ADR D-008及AC-044；此前T40完成结果26f04b94，治理基线8db922e1971b4781b2b53f8db837c06f7b60c4e7。main/current，provider=git，direct-parent，无worktree。用户Goal允许实施、本地commit及委派；cors_audit唯一产品writer，Lead独占所有治理、提交、测试/构建/隔离服务，ops_audit与legacy_audit只读。writer不推送/部署/生产操作。

## 写集与实际Skill

Ticket frontmatter 19条是权威；本轮01A仅admin OSS测试根可写。生产与生成物、其他票、change治理只读。01B须Lead接收红灯后再发。engineering/fullstack/module/common按实际入口及命中references检查System classic、Service复用OssFactory/RedisUtils、HTTP POST和安全日志、默认缓存与事务失效；不得扩wta-api/common或修改六SQL。额外写路径先回Lead登记。生成物只由Lead用真实full JAR捕获与正式工具生成。

## 01A 红灯与01B预期合同

先使用现有生命周期、上传、迁移服务的真实方法编写诊断缺失/过期不阻断合法配置的测试；保留权限/非ACTIVE/错误policy/缺service负例，确保失败来自旧requireServing，而非错误夹具或编译。不要先改生产使测试变绿。返回路径、测试选择器、具体红灯预期和未验证声明；Lead串行执行并保留结果。

01B合同见Ticket revision199和activation-audit/writer-plan/http-path-registration存档。业务不依赖registry；启动/config提交后无远端refresh；管理员单配置有界诊断、去敏VO，唯一PRIVATE默认管理不放松；无配置/坏非默认核心可启动，无效默认不复活Redis指针；无效可选Duration不abort核心。健康组隔离，应用全局调度保留；真实最小权限MinIO+full app/Notify scheduled fallback必验。

## 返回、验证与停止

writer返回Ticket ID、workspace、base/HEAD、dirty和精确修改列表、未运行测试、冲突/风险。Lead先审核并提交固定候选，定向red/green、全后端测试/full与core包、Skill静态、真实MySQL8.4/Redis/最小权限MinIO、full app核心和诊断HTTP、授权/审计去敏及Notify兜底；OpenAPI正式生成/check/typecheck后再完整合同门禁。测试任务局部JVM heap 128m/1536m，不改全局环境。源码冻结期间writer停写，真实测试零skip及cleanup[]、来源/产物一致才算通过。完整候选最多3次，重复失败无新证据提前复盘；无权限/新公共决策/写集不足/资源冲突立刻回Lead。尚无implementation/result，不能勾AC或宣称完成。
