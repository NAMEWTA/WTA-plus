# Nacos 动态配置术语

- **Source:** `<Path>{roots.state}/specdev/archive/2026-08/2026-08-31-optional-nacos-dynamic-config/CONTEXT.md</Path>`
- **Source:** `<Path>{roots.state}/specdev/archive/2026-09/2026-09-01-admin-runtime-capability-reconciliation/ADR.md</Path>` ADR-003
- **Graduated:** 2026-09-02

**Nacos 配置覆盖层**：由 Nacos 保存的稀疏 YAML 配置，只覆盖实际出现且获准的键；其他配置继续由本地 `application*.yml` 提供。
_Avoid_: 完整远程配置、Nacos 主配置

**本地配置基线**：应用在关闭 Nacos、远程无内容或 Nacos 不可达时仍可独立启动和运行的完整 `application*.yml` 配置集合。
_Avoid_: 备用残片、占位配置

**ruoyi-common-nacos**：封装可选 Nacos Config 客户端、远程覆盖、校验和安全刷新能力的独立 common artifact；不包含 Nacos Server，也不承担服务注册发现。
_Avoid_: ruoyi-nacos-server、微服务注册中心模块

**Nacos 配置管理入口**：“系统监控 > Nacos配置中心”下通过通用 external iframe 和 `/nacos/` 同源反代打开官方控制台的动态菜单入口；RuoYi 控制入口可见性，Nacos 账号控制配置操作。
_Avoid_: 自建 Nacos CRUD、系统管理下的配置中心、Nacos 无感登录

**部署层强制值**：由命令行参数或环境变量提供、优先级高于 Nacos 覆盖层的部署配置；远程 YAML 不能绕过或替换这些值。
_Avoid_: Nacos 最高优先级

**环境配置单元**：由 active profile 对应的 Nacos namespace、`DEFAULT_GROUP` 和 `ruoyi-namewta.yml` 共同标识的远程覆盖文档；local、dev、prod 相互隔离。
_Avoid_: 跨环境共享配置

**Nacos 单机持久化基线**：固定 `nacos/nacos-server:v2.5.4`、standalone、独立 MySQL 最小权限存储、鉴权、本机端口和健康门的可复现基础设施合同；不代表生产高可用。
_Avoid_: Nacos 集群基线、无鉴权开发容器、生产 HA

**即时生效配置清单**：规格明确列出并经测试证明能在不重启应用时安全改变运行行为的远程属性前缀集合。
_Avoid_: 所有 Spring 属性都热更新、收到即生效

**重启生效配置**：通过远程校验但当前进程不会安全重建的配置；保存后必须明确标记等待应用重启。
_Avoid_: 发布失败、伪装成即时生效

**上一有效覆盖**：当前进程最后一次完整通过解析和安全校验的远程覆盖版本；运行中失联或收到非法版本时继续使用。
_Avoid_: 持久离线快照、局部接受非法文档

**Nacos 普通敏感配置**：允许进入普通 Nacos YAML、可能以明文存在于 Nacos MySQL 和授权控制台中的密码或密钥；必须依靠访问控制和禁止回显保护。
_Avoid_: 已加密 secret、可写入日志的配置值

**实例配置状态**：单个应用实例对远程版本摘要、连接、校验、应用结果、最后成功时间及即时/重启分类的安全报告，不包含配置正文。
_Avoid_: Nacos 发布成功等同全部实例生效、返回 secret 的健康详情

**Nacos 显式启用**：`nacos.config.enabled` 默认关闭，只由明确需要动态覆盖的部署环境开启；关闭时不创建客户端或监听器。
_Avoid_: 默认连接 Nacos、通过代码环境猜测启用
