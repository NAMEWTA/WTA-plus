# T/P Plan Quality Review

2026-09-18，single-agent串行审查；源基线见<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/planning-baseline.json</Path>。调用<Path>{roots.workflows}/specdev/common/skills/plan-quality-review/SKILL.md</Path>，输入Spec、ADR、全部31票、Map、Goal、用户授权与项目Skill真实元数据，按checklist逐轴审查并修正文档。未启动子代理，未执行产品代码审查/实现测试。

| 检查轴 | 结论 | 本次实际操作与证据 |
|---|---|---|
| 背景与边界 | pass | 31票逐项保留源码finding、目标与IN/REUSE/OUT；修正过宽备选，复用现有模块；见各票1—5节 |
| Skill调用 | pass | 扫描.agents/skills入口；绑定4个命中Skill的真实name/sha256/implement与verify/input/output/on_failure；控制器无绑定错误；未把计划阅读伪造成实现调用 |
| 执行计划 | partial/block | 每票有路线/异常/恢复/验证；T-03预算与T-23供应商身份仍需证据，明确blocked；其余29票局部DoR通过 |
| 控制图 | pass | 31节点、全部真实依赖、拓扑串行顺序、共享owner及workspace-exclusive资源；117条保守并发warning已有串行裁决，未放宽检查器 |
| 验收与数量 | pass | AC-001—031全部有同号票与未来Evidence；保留31完整票，T/P各一份主工件，无隐式deferred |
| 权限 | pass | 计划与相关索引已授权；implementation/commit/integration/deploy全部未授权，Goal关闭；schema正数上限不覆盖用户禁止派遣 |
| 恢复 | pass | 保存HEAD/branch/dirty及change外哈希；原review检查点交接T，再P；永久知识及其他change只读，未占用外部事务 |

## 逐票结果

Ready仅局部计划门禁；全局执行仍受Spec/Goal/授权与依赖约束。下面不是产品测试通过记录。

| Ticket | 计划结论 | 专项核对/裁决 |
|---|---|---|
| T-01 | pass | ADR-CR-006：恢复仓内GitHub Actions候选配置与本地门禁合同；启用远程Actions/required checks不属于本票自动授权。 |
| T-02 | pass | ADR-0025已禁止泄漏；新的默认日志保留策略见ADR-CR-002当前计划合同。 |
| T-03 | block | 建议普通JSON/机器正文2MiB为评估起点，最终以真实最大请求决定；新增413属于当前计划合同外部合同。 |
| T-04 | pass | ADR-CR-003：算法以显式可信CIDR为输入，默认空集合只信socket peer；隔离拓扑提供测试CIDR，真实部署值在G-release关闭前实测。 |
| T-05 | pass | 不改变防重窗口的业务含义；若引入嵌套支持需单独定界。 |
| T-06 | pass | ADR-CR-001：推荐强制旧SSO session失效并重新登录；保留业务Sa-Token target Client协议，不顺手改认证体系。 |
| T-07 | pass | ADR-CR-001/002；后端已精确校验redirect，本票不是虚构开放重定向修复。 |
| T-08 | pass | 兑现ADR-0073/0074的独立SSO Origin；本地隔离三Origin验证，真实域名/TLS/端口只作为发布Gate输入。 |
| T-09 | pass | ADR-CR-005：显式产品构建名单；保持当前Compose镜像版本，不将镜像升级纳入本票。 |
| T-10 | pass | ADR-CR-005：取消partial deployable promotion，只允许同一干净源码完整产物切换指针。 |
| T-11 | pass | 按用户要求以HTTPS完成浏览器传输硬切，删除共享响应私钥和ECB；不新增AEAD备选协议、Cookie会话迁移或双协议兼容。 |
| T-12 | pass | ADR-CR-007保留各App路由owner，共享稳定机制；本地退出不伪称远端token已撤销。 Home已有finally清token必须保留，修复其异常后导航和route回收；不再报告Home token残留。 |
| T-13 | pass | 不改变密码策略和匿名注册权限，仅兑现已存在Client context合同。 |
| T-14 | pass | 沿用现有Profile材料归属与数据库规则，不删除后端门禁换取页面成功。 |
| T-15 | pass | 基座直接替换公开合同，同步仓内调用、测试和生成物；不保留deprecated/default桥，不要求外部消费者清零、版本等待或兼容窗口。 |
| T-16 | pass | 复用既有checkTaskReadAccess及WarmFlow锁；先用实际引擎权限测试确定调用接入点，B-10仍标likely直到运行证实。 |
| T-17 | pass | 当前缺origin/source检查已确认；使用现有close协议即可修复，不依赖供应商新增nonce。 |
| T-18 | pass | F11仅object URL资源泄漏confirmed；detach/delete为T14新增使用前的设计约束，必须通过调用者证据决定接口，不默认新增泛化API。 |
| T-19 | pass | ADR-CR-007保留domain/web-domain/App主轴；实际竞态需代表页面定向证实。 |
| T-20 | pass | 全局三项strict关闭是工程债，尚无诊断规模证明全仓硬切必要；本change优先收紧实际修改的合同边界，全仓严格化仅在诊断证明范围可控时另行纳入。 |
| T-21 | pass | 沿用现有视觉基线，只补已证实的Home token和SSO状态语义，不引入新的视觉选择。 |
| T-22 | pass | ADR-CR-009保留Outbox/Redis wake，不退回同步发送；外部投递exactly-once不作无法证明承诺。 |
| T-23 | block | ADR-CR-009推荐MySQL receipt唯一键；唯一身份与保留期需按实际供应商合同审核。 |
| T-24 | pass | 采用可过期permit，TTL由现有连接/读超时与有限重试总预算推导；稳定key按配置保存更新，降低额度不强杀在途调用。 |
| T-25 | pass | 同一部门树的结构变更在事务内串行，跨根移动按稳定根ID顺序锁两树；所有会改变父子关系的入口使用同一锁域，不仅updateDept。 |
| T-26 | pass | 基座HTTP合同直接切换；同步仓内Controller、domain、OpenAPI及测试并删除旧CRUD路由，不等待外部消费者兼容期。 |
| T-27 | pass | 保留Demo/default bundle，实现合法parent/无环/有子节点拒删；不增加名称唯一性。 |
| T-28 | pass | 删除本地重复wake，保留Redis有界发布与慢poll；本票不新增executor。 |
| T-29 | pass | ADR-CR-006：重复事实收敛；本change只提出未来永久知识修订，不越权覆盖现有ADR/context。 |
| T-30 | pass | 本票是未来实施后的最终验收，不是当前只写change审查任务已执行的验证。 |
| T-31 | pass | 修正假同步合同，复用Notify公开query按需核验投递状态；不预设新增Profile事件总线、投影Outbox或持久验证码副本。 |

## 校验解释与边界

T/P阶段validator、通用单change校验及ticket-control退出码、warnings和完整输出保存于<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/planning-validation.json</Path>与<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/planning-control.json</Path>。检查器现有并发警告逻辑只根据依赖关系判断票对，不读取current策略；无未归属写集error。当前明确同一Lead串行slot，禁止并行，所以不追加虚假blocked_by消除warning。

未做Maven/pnpm/浏览器/真实服务验证；这些属于未来票据证据。T-03/T-23本轮没有批准替代证据，不伪造Ready；Goal blocked是执行状态，T/P本次plan文档交付到此可结束。

## Skill来源

- <Path>.agents/skills/engineering-standards/SKILL.md</Path>：SHA-256 `dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9`；规划用途为裁决架构/依赖/验收，不是已执行implement/verify。
- <Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>：SHA-256 `675c053c11d8b22cd394c875f48688242d8e8328dd14e69657f5d5b12f2af68b`；规划用途为裁决架构/依赖/验收，不是已执行implement/verify。
- <Path>.agents/skills/wta-common-modules-guide/SKILL.md</Path>：SHA-256 `e92775ce47af41bd33c1b3293d7f8a3185fdd739b9b588519e043c654f678d98`；规划用途为裁决架构/依赖/验收，不是已执行implement/verify。
- <Path>.agents/skills/wta-module-guide/SKILL.md</Path>：SHA-256 `441de2ccc513e09820ed3d7d2faf559eeaa7466202e4fbb0c8dd3eabf09510c9`；规划用途为裁决架构/依赖/验收，不是已执行implement/verify。
- <Path>{roots.workflows}/specdev/common/skills/plan-quality-review/SKILL.md</Path>：SHA-256 `0b8bc84ab83f32c243c1a347f52e614ab86cb4dfc74035d6ab8c0b70ea333c3a`；本报告为检查输出。
- <Path>{roots.workflows}/specdev/common/skills/subagent-delivery/SKILL.md</Path>：SHA-256 `e5d8a3628a34edb8a03b9a49aa2a2d35dfc211d0cf8604f0fa191e16b8c7b513`；仅operation=plan，task_kind为空、派遣禁止；所有权输出在Goal第4节。

### P阶段校验限制（本轮实测）

P阶段校验器存在单change路由缺陷：validateParentImplementation以stage==goal-plan无条件要求implementation-map.md/implementation-plan.md，而single-change-plan只要求goal-plan.md。未创建虚假父级工件，也未越范围修改workflow工具。

`--stage tickets`、通用单change校验和Goal v6 JSON Schema可分别验证现有文档；它们不冒充P阶段已通过。P保持可恢复阻塞，解除工具路由问题及上游缺口后重跑原命令。
