# 用户密码策略与临时凭据架构决策

## ADR-001: 使用 sys_config 承载统一密码策略

**Status:** accepted
**Source:** LOG-007 / USER-DECISION:2026-08-28
**Supersedes:** none

### Context
现有默认密码来自 `sys_config` 的 `sys.user.initPassword`，而不是 `sys_dict`。新能力需要同时表达随机生成规则、固定默认密码兼容和所有密码写入口的统一校验；把结构化安全策略拆成字典项会混淆枚举翻译与运行配置职责。

### Decision
继续使用 `sys_config` 作为统一密码策略配置载体。下游 Spec 必须定义类型化、可校验的配置对象，支持 `random` 生成模式以及必须通过同一强度基线的 `fixed` 模式；不得把策略迁移到 `sys_dict`。

### Trade-off
沿用单值参数减少了新表和缓存链路，但 JSON 参数缺少数据库结构约束，因此必须在服务端解析、保存和使用边界进行类型与不变量校验。真正的 `sys_dict` 更易在通用字典页面逐项编辑，但不能自然表达原子策略对象和跨字段约束。

### Consequences
注册、管理员新增、Excel 导入、管理员重置、个人改密、随机生成和临时密码都以同一服务端策略为权威。前端只能取得非敏感策略投影；固定密码值和生成内部配置不得通过公开认证端点泄露。

### Verification / Migration
在 `script/sql/namewta/DML.sql` 末尾追加 fresh/upgrade 兼容块；验证 random/fixed 合法配置、畸形 JSON、缺字段、弱 fixed 值和缓存刷新。

## ADR-002: 临时密码作为 Redis 一次性旁路凭据

**Status:** accepted
**Source:** LOG-008, LOG-015 / USER-DECISION:2026-08-28
**Supersedes:** none

### Context
把一分钟临时密码直接写入 `sys_user.password` 会破坏永久密码，而允许有效期内重复使用会扩大凭据泄露后的重放窗口。临时凭据还需要集群一致 TTL、覆盖旧值和并发登录下只成功一次。

### Decision
临时密码不修改永久密码，只保存不可逆校验值到 Redis，并设置 60 秒 TTL。凭据以用户为作用域，新签发覆盖该用户旧值；首次成功认证必须通过原子校验与删除完成消费。它可用于该用户原本具有准入资格的任意 Client，但每次仍继续执行用户状态、验证码、目标 Client 和登录域准入。

### Trade-off
Redis 方案不需要数据库 schema 和定时清理，但临时密码能力依赖 Redis 可用性；原子校验删除比普通 get/delete 更复杂。数据库持久化便于审计，但会引入敏感凭据生命周期、清理任务和并发更新负担。

### Consequences
Redis 不可用时临时密码签发和验证失败关闭，永久密码认证保持原合同。永久密码与临时密码必须共享登录失败计数语义，且任何日志和错误响应不得泄露 Redis 校验值。

### Verification / Migration
验证 TTL、覆盖旧值、错误密码不消费、首次成功消费、双并发仅一次成功、过期拒绝和永久密码不受影响。

## ADR-003: 临时密码认证后签发无标记普通会话

**Status:** accepted
**Source:** LOG-012 / USER-DECISION:2026-08-28
**Supersedes:** none

### Context
临时密码用于管理员临时登录指定账号排查问题，目标是避免重置用户永久密码。它只替代密码认证阶段的凭据，并不代表账号、权限或登录后的业务能力发生变化。

### Decision
临时密码认证成功后，完全复用普通密码登录的会话签发流程。会话不增加临时凭据标记，不缩短会话时长，不限制功能或权限，也不强制用户修改永久密码。

### Trade-off
普通会话最符合排障场景，且避免在全部鉴权链路传播新的会话类型；代价是临时密码只有 60 秒和单次使用约束，认证成功后所签会话仍按 Client 的正常生命周期有效。

### Consequences
临时密码的特殊逻辑止于密码校验与原子消费边界。认证后的 Token、权限、角色、数据范围和退出行为均沿用该账号在目标 Client 的普通合同，不得出现临时登录专用分支。

### Verification / Migration
验证永久密码与临时密码两种成功认证产生等价会话属性；临时认证不写标记、不改变权限，并仍受用户状态、验证码、Client 和登录域准入约束。

## ADR-004: 权限变化按 Client 边界强制重建登录快照

**Status:** accepted
**Source:** LOG-018, LOG-019, LOG-020 / USER-DECISION:2026-08-28
**Supersedes:** none

### Context
角色、菜单与数据权限在登录时被组装进 `LoginUser`，后续鉴权直接读取 Token 会话中的快照。数据库权限变化后若保留旧会话，权限回收无法立即生效。系统同时使用 Redis 和 JVM 本地 Caffeine；只删除一个层级或只清执行节点会留下短暂旧快照。

### Decision
角色菜单或数据权限变化后，注销该角色所属 Client 的全部会话；用户角色关系变化后，注销该用户在目标 Client 的全部会话。其他 Client 的会话保持不变。注销统一通过会话与缓存抽象完成，并必须同步删除目标范围的 Redis Token/Session、权限数据缓存及全部运行实例的本地副本，使用户重新登录后生成新快照。

### Trade-off
角色权限变化按 Client 注销会使该 Client 中未直接绑定该角色的用户也重新登录，但能完整覆盖客户端默认角色和其他无法仅靠显式关系表枚举的授权来源。按 Client 隔离又避免把中断扩散到权限未变化的其他 Client。跨实例立即失效比等待短 TTL 需要额外一致性机制和集群测试。

### Consequences
权限变更成功响应不能先于目标会话与缓存失效完成，也不能通过修改旧 Token 中的部分集合绕过重新登录。业务服务不得散落硬编码 Sa-Token Redis key；永久密码重置继续沿用不注销既有会话的独立兼容合同。

### Verification / Migration
覆盖角色菜单与数据权限修改、单个与批量用户角色授予/撤销、客户端默认角色、同用户多 Token、同用户多 Client、双 JVM 本地缓存和 Redis 清理；断言目标 Client 立即未登录，其他 Client Token 仍有效，重新登录获得最新快照。
