# 可选动态配置

**Nacos 配置覆盖层**：由 Nacos 保存的稀疏 YAML 配置，只覆盖其中实际出现且获准的键；其他配置继续由本地 `application*.yml` 提供。
_Avoid_: 完整远程配置、Nacos 主配置

**本地配置基线**：应用在关闭 Nacos、远程无内容或 Nacos 不可达时仍可独立启动和运行的完整 `application*.yml` 配置集合。
_Avoid_: 备用残片、占位配置

**ruoyi-common-nacos**：封装可选 Nacos Config 客户端、远程覆盖和安全刷新能力的独立 common 子 artifact；它不包含 Nacos Server，也不承担服务注册发现。
_Avoid_: ruoyi-nacos-server、微服务注册中心模块

**Nacos 配置管理入口**：系统管理下通过通用 external iframe 和 `/nacos/` 同源反代打开 Nacos 官方控制台的动态菜单入口；RuoYi 权限控制入口可见性，Nacos 自身账号控制配置操作。
_Avoid_: 自建 Nacos CRUD、Nacos 无感登录、RuoYi 代管 Nacos 密码

**部署层强制值**：由命令行参数或环境变量提供、优先级高于 Nacos 覆盖层的部署配置；远程 YAML 不能绕过或替换这些值。
_Avoid_: Nacos 最高优先级

**环境配置单元**：由 active profile 对应的 Nacos namespace、`DEFAULT_GROUP` 和 `ruoyi-namewta.yml` 共同标识的远程覆盖文档；local、dev、prod 相互隔离。
_Avoid_: 跨环境共享配置

**Nacos 单机持久化基线**：固定 `nacos/nacos-server:v2.5.4`、standalone、MySQL 持久化并开启鉴权的单机发布形态，不代表生产高可用集群。
_Avoid_: Nacos 生产集群、latest 镜像

**即时生效配置清单**：规格明确列出且通过运行期重绑定测试的配置前缀；Nacos 推送成功后，这些配置可以在不重启进程的情况下生效。
_Avoid_: 热更新白名单、所有 YAML 热更新

**重启生效配置**：可以保存在 Nacos 覆盖层、但当前 Spring 进程不能可靠重建关联运行时对象的配置；它们在应用下次启动加载远程文档时生效。
_Avoid_: 不支持的配置、无效配置

**上一有效覆盖**：当前实例最后一次完整通过 YAML、保护键和类型校验的 Nacos 覆盖版本；监听到非法版本或运行中失联时继续保持该版本。
_Avoid_: Nacos 最新版本、持久离线快照

**Nacos 普通敏感配置**：允许存入普通 `ruoyi-namewta.yml`、不使用配置加密插件保护的敏感值；其保护依赖 Nacos 鉴权、可信网络和最小账号权限，并禁止出现在应用输出中。
_Avoid_: 已加密 secret、安全明文

**实例配置状态**：单个 ruoyi-admin 实例记录的环境配置单元版本摘要、最后应用结果、最后成功时间及即时生效或等待重启分类，不包含远程正文或敏感值。
_Avoid_: Nacos 发布状态、集群统一成功状态

**Nacos 显式启用**：`nacos.config.enabled` 默认关闭；普通本地环境按需开启，包含 Nacos 基础设施的 Docker 发布通过环境变量为每个 ruoyi-admin 实例开启。
_Avoid_: 强制 Nacos 启动依赖、默认尝试连接
