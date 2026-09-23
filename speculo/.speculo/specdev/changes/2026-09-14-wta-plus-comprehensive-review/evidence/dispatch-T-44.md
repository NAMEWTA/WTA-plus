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

## revision200 — 红灯与Dispatch01B

revision200：T44定向红灯已证实，固定11960110的53例为49pass/1failure/3error/0skip；三条诊断前置阻断及空service仍进入Provider的缺陷均可重现。Dispatch01B开始产品实现，完整候选attempts0；14done/2cancelled/T44 in_progress/33ready，Goal active。

`red01` Maven exit1、编译成功，源码前后clean同值。生命周期/直传/迁移各因旧readiness门禁抛错；空service负例在objectStore.accessPolicy被调用后失败，确认须补本地路由校验。其余49项通过；未把预期红灯算成验收或完整候选失败。XML/命令/源码/两份独立审查已保存red01/manifest.json。

01B在原19条写集内完成：业务诊断解耦而授权/ACTIVE/service/policy不放松；DB成功读取后清SYS_OSS_CONFIG专用缓存和默认指针，再填合法当前行，DB故障仍核心报错；单配置管理员诊断、配置变更只失效、无启动远端探测、常驻应用调度、core和ossdiagnostics健康组及规范同步。可选诊断timeout冻结为每网络步骤100ms–3s，最多5个顺序步骤，网络等待预算最多15s，不称整个请求3s；配置无效返回固定诊断配置错误，核心仍启动。不得起未回收后台任务制造表面超时。若需严格单个总deadline或common路径先回Lead登记，T45策略解释未提前改动。

01B source=11960110ff4f5a8c99a885dbf2866578fc8493af；main/current。cors_audit唯一产品writer，允许原Ticket19条内生产/测试/对应文档，OpenAPI生成三处仍由Lead正式工具写；不跑构建/服务/提交、不写治理。返回精确diff与选择器后停写，Lead定向绿灯及独立审查。ops_audit/legacy_audit仅准备/tmp/wta-t44私有隔离驱动，未启动或改仓库。
