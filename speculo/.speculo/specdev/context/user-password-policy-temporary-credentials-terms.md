# 用户密码策略与临时凭据术语

- **Source:** `<Path>{roots.state}/specdev/archive/2026-08/2026-08-28-user-password-policy-temporary-credentials/CONTEXT.md</Path>`（2026-08-29 归档提升）

**密码强度基线（Password Strength Baseline）**：新写入的密码至少 8 位，并至少各包含一个大写字母、小写字母、数字和特殊字符；服务端是强制校验权威，前端提供一致的即时反馈。
_Avoid_: 仅校验长度、仅由前端校验、继续接受 123456 作为新默认密码

**一分钟临时密码（One-minute Temporary Password）**：管理员为指定用户签发、符合密码强度基线并在签发后 60 秒内可进入密码认证流程的一次性用户级凭据；新签发覆盖旧值，首次成功认证时原子消费，且不修改永久密码。它可用于该用户原本具有准入资格的任意 Client，每次登录仍执行目标 Client 的既有准入校验。
_Avoid_: 把临时密码等同于直接覆盖数据库永久密码、绑定单个管理端 Client、绕过目标 Client 准入、在有效期内重复使用同一临时密码

**临时密码普通会话（Ordinary Session after Temporary Authentication）**：临时密码只参与一次密码认证；认证成功后沿用普通登录的完整会话签发流程，不添加临时标记、时长限制、功能限制或权限变化。
_Avoid_: 把临时密码登录变成受限会话、强制改密会话或特殊权限会话

**可编辑重置候选密码（Editable Generated Reset Candidate）**：管理员发起永久密码重置时，服务端依据有效策略产生且不再使用 123456 的合规候选值；前端允许管理员编辑，只有最终确认提交才修改永久密码，且服务端必须重新校验最终值。
_Avoid_: 浏览器自行随机、生成即重置、弱固定默认值、服务端不校验管理员编辑后的最终值

**注册密码校验（Registration Password Validation）**：公开注册在前端提示密码强度，并在服务端持久化前使用同一强度基线强制拒绝弱密码，同时保留 Client 注册开关、登录域和验证码现有准入。
_Avoid_: 把前端表单规则当成注册安全边界

**密码策略参数（Password Policy Configuration）**：由 `sys_config` 承载的统一密码策略，供注册、管理员新增、Excel 导入、管理员重置、个人改密、随机生成和临时凭据共同使用。
_Avoid_: 使用 `sys_dict` 承载安全策略对象、不同入口维护互相漂移的强度规则

**密码规则明细错误（Detailed Password Policy Errors）**：服务端对密码或策略校验失败返回稳定、可区分的具体原因，例如长度不足、缺少大写字母、缺少小写字母、缺少数字或缺少特殊字符；错误不得包含 fixed 密码或随机生成内部参数。
_Avoid_: 只返回“密码不符合规则”的笼统提示、由前端自行猜测服务端失败原因

**公开密码策略投影（Public Password Policy Projection）**：公开认证上下文只暴露最小长度、必需字符类别和允许的特殊字符等表单校验所需的非敏感规则；fixed 密码和生成长度、字符池等内部参数不下发，服务端保持最终校验权威。
_Avoid_: 向未认证端点返回 fixed 密码或完整生成配置、把公开投影当成安全边界

**存量弱密码兼容（Legacy Weak Password Compatibility）**：启用新策略后，不在登录读取路径按新强度拒绝已有 BCrypt 密码；存量账号在下一次改密或管理员重置时收敛到新策略。
_Avoid_: 无恢复流程地立即锁死存量账号、继续允许新写入弱密码

**临时密码签发权限（Temporary Password Issuance Permission）**：`system:user:temporaryPassword` 独立控制管理员签发临时登录凭据的能力，不由永久密码重置权限隐式授予。
_Avoid_: 复用 `system:user:resetPwd` 自动扩大临时凭据签发权

**永久密码重置会话兼容（Password Reset Session Compatibility）**：管理员重置永久密码沿用当前系统行为，只更新密码而不主动注销既有会话；该行为不因随机候选密码和统一强度策略而改变。
_Avoid_: 借密码规则优化隐式改变存量会话生命周期

**权限变更强制重新登录（Forced Re-login after Authorization Change）**：角色菜单/数据权限变化后，该角色所属 Client 的全部既有会话失效；用户角色关系变化后，该用户在目标 Client 的全部既有会话失效。其他 Client 的权限快照没有变化并保留会话；目标范围内的用户重新登录后由服务端生成最新角色、菜单和数据权限快照。
_Avoid_: 跨无关 Client 扩大注销范围、原地修改部分 Token 快照、等待旧会话自然过期、只刷新前端路由而保留后端旧权限

**授权会话分层失效（Layered Authorization Session Invalidation）**：会话失效必须同时删除 Sa-Token Redis 中的 Token/Session 与权限快照、清理权限数据缓存，并使全部运行实例的 JVM 本地 Caffeine 副本立即失效。
_Avoid_: 直接猜测并删除零散 Redis key、只清执行节点的内存、依赖 5 秒或 30 秒本地缓存自然过期
