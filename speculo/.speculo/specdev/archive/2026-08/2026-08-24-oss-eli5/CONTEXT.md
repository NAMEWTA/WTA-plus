# OSS 公共与受控访问

**公共 OSS 对象**：经服务端明确归类、允许匿名读取的对象；调用方可以持久展示稳定 URL，任何获得该 URL 的访问者均可直接预览或下载。公共只表示读取公开，不表示匿名上传、覆盖或删除。
_Avoid_: 把全部 sys_oss 元数据理解为公共资源、public-read-write、公共上传

**受控 OSS 对象**：Provider 读取权限保持私有，业务接口完成自身权限与数据权限校验后，才签发有明确过期时间的读取 URL。
_Avoid_: 永久私有 URL、仅凭 ossId 授权、把 sys_oss_ref 当作 ACL

**稳定公共 URL**：不包含短时签名参数、供公开页面和缓存长期引用的对象读取地址；其可访问性由对象所属公共存储配置与 Provider public-read 策略共同保证。
_Avoid_: 把预签名 URL 入库为公共 URL、依赖过期签名做门户长期展示

**受控访问 URL**：在业务授权后为受控 OSS 对象生成的 bearer URL，包含服务端确定的有效期；有效期内持有者可访问，过期后 Provider 拒绝。
_Avoid_: 用户绑定 URL、客户端自报 TTL、把签名 URL 写入业务持久数据

**公共存储配置**：具有独立 configKey 和独立 Bucket、只允许匿名读取对象的 OSS 配置档案；它不与受控对象共享 Bucket，也不允许匿名写。
_Avoid_: public-read-write、同桶对象 ACL 混用、把默认配置等同公共配置

**受控存储配置**：具有独立 configKey 和独立 Bucket、Provider 无签名读取保持关闭的 OSS 配置档案；读取由业务授权后的短时签名完成。
_Avoid_: 同桶 public/private 混用、通过隐藏 URL 代替 Provider 私有策略

**公共 URL 发布**：门户等 Business OSS Owner 随已获准公开的业务数据返回稳定公共 URL；平台不提供按任意 ossId 匿名发现对象或元数据的通用接口。
_Avoid_: 匿名 OSS 元数据查询、公开 sys_oss 索引、仅凭 ossId 发现资源

**OSS 访问结果**：OssService 根据 ossId 对应存储配置解析出的结构化结果，包含访问类型、URL、可空过期时间和文件名；它不替代调用方的业务授权。
_Avoid_: 业务模块自行读取 OSS 配置、用 URL 是否含查询参数猜测访问类型

**存储访问类型**：由 OSS 存储配置声明并唯一决定的 PUBLIC 或 PRIVATE 读取语义；对象通过 sys_oss.service 绑定配置后获得该语义，不另存对象级副本。
_Avoid_: 同一 configKey 混用、对象级重复快照、根据 URL 猜测类型

**Provider Policy 诊断**：应用对存储配置声明和云厂商实际匿名读写能力的一致性检查；应用不修改 Bucket Policy，不一致的配置不能进入可服务状态。
_Avoid_: 启动时自动公开 Bucket、只告警后继续使用、用应用声明代替真实策略

**受管公共域名**：生产公共存储配置用于生成稳定 URL 的 domainUrl 或 CDN 域名；开发环境只有显式允许时才能回退到 Provider Bucket URL。
_Avoid_: 生产隐式回退、手工字符串拼接、用公共域名访问私有对象

**兼容 URL 解析**：旧 OssService URL 查询方法保留签名并委托统一访问解析，使公共对象返回稳定 URL、受控对象返回短时 URL。
_Avoid_: 各调用方重复判断配置、管理列表批量签名、把兼容方法当匿名接口

**命名访问策略**：由服务端配置并以稳定名称引用的私有下载规则，可在安全上下限内覆盖默认 TTL；客户端不能直接指定有效期。
_Avoid_: 客户端自报 TTL、无限期签名、将策略名误作业务授权

**上传存储路由**：命名 uploadPolicy 在服务端固定绑定 storageConfigKey 的规则；客户端只选择允许的策略名，不能直接选择公共或受控 Bucket。
_Avoid_: 客户端提交 configKey、完成阶段回退默认配置、策略与存储类型不匹配

**公共只读策略**：PUBLIC_READ 存储允许匿名 GET/HEAD，但拒绝匿名 PUT、覆盖和删除；所有写操作继续通过服务端授权的预签名请求完成。
_Avoid_: PUBLIC_READ_WRITE、custom、把公开读取扩展成匿名写入

**存储边界迁移**：将已有对象从一个 storage configKey/Bucket 显式迁移到另一个配置的受控流程，包含目标校验、对象复制、内容验证、sys_oss.service 切换、业务读取验证和可恢复清理。
_Avoid_: 原地修改有对象配置的 Bucket 或访问类型、先删源对象、无审计批量公开
