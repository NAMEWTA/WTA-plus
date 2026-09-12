# Nacos 完整改动全景：它怎样接入、更新、嵌入与兜底

这次改动不是简单地“加一个 Nacos 容器”，而是把配置发布、后端读取、安全热更新、状态观察、前端控制台入口和发布验证连成了一条完整链路。

先记住三个结论：

1. 它是什么：Nacos 是一层可选的远程稀疏配置覆盖，也是一个独立鉴权的官方管理控制台。
2. 为什么需要：运维人员可以在不重做镜像的情况下集中修改配置；Nacos 不可用时，应用仍能依靠完整本地 YAML 启动。
3. 怎样流动：运维人员在官方控制台发布 YAML，两个后端实例分别拉取、校验并原子采用；只有明确接入热更新协议的三类配置会立即生效。

最容易产生的误解也先说清楚：

```text
“Nacos 收到了整份 YAML”
              不等于
“所有 Spring Bean 都会自动重新创建”
```

启动时，合法的远程配置可以参与 Bean 的首次绑定。系统运行后，只有验证码、通知幂等窗口和 OSS 下载签名时长使用了专门的运行期快照，因此能安全地立即更新。其他合法键会被记录为“需要重启”，等应用重启后再参与首次绑定。

## 先看全图

```text
                         管理平面

 [NAMEWTA 用户]
       |
       | 1. 拥有 system:nacos:console 才看得到入口
       v
 [系统管理 -> 配置中心]
       |
       | 2. 前端 iframe 打开同源 /nacos/
       v
 [Nginx 反向代理] --------------------------+
       |                                     |
       v                                     |
 [Nacos 官方控制台]                          |
       |                                     |
       | 3. 再进行一次独立的 Nacos 登录       |
       | 4. 发布 ruoyi-namewta.yml            |
       v                                     |
 [Nacos 2.5.4]                               |
       |                                     |
       | 配置、用户、权限、历史                |
       v                                     |
 [MySQL 的 nacos 独立数据库]                  |
                                             |
                         配置数据平面          |
                                             |
 [Nacos 2.5.4] ------------------------------+
       |
       | SDK 长轮询通知同一份配置
       +----------------------+----------------------+
       |                                             |
       v                                             v
 [ruoyi-server1]                               [ruoyi-server2]
       |                                             |
       | 解析 -> 校验 -> 准备全部参与者 -> 原子切换   |
       +----------------------+----------------------+
                              |
                  +-----------+-----------+
                  |                       |
                  v                       v
            [立即生效快照]            [重启后生效]
            captcha.*                其余合法配置
            notify.idempotency.*
            oss.lifecycle.download-ttl
```

这张图中有两条不同的链：

- 管理平面回答“人怎样进入 Nacos 控制台并发布配置”。
- 配置数据平面回答“后端怎样读取并采用配置”。

前端没有连接 Nacos SDK，也没有自己实现 Nacos 的增删改查接口。它只是安全地嵌入官方控制台。真正读取配置的是每个后端实例中的 `ruoyi-common-nacos`。

整个改动可以按责任分成六块：

```text
[1. 公共模块]  ruoyi-common-nacos
       |
       +-- 启动拉取、监听、解析、校验、原子状态、Actuator 摘要

[2. 业务接入]  captcha / notify / OSS
       |
       +-- 三个运行期参与者，每次业务调用读取当前不可变快照

[3. 应用装配]  ruoyi-admin + application.yml
       |
       +-- 加依赖、开关、地址、账号、namespace、dataId、group

[4. 管理入口]  SQL 菜单 + Vue 动态路由 + iframe
       |
       +-- RuoYi 权限管入口，Nacos 账号管控制台内部权限

[5. 发布设施]  Docker Compose + MySQL 初始化 + Nginx
       |
       +-- 官方 Nacos 镜像、持久化、鉴权、同源代理、显式启用

[6. 验证证据]  单测 + 构建 + 浏览器 + 远程真实 E2E
       |
       +-- 覆盖正常更新、错误配置、断线、重启、持久化与回退
```

## 一步一步看

### 1. 需求被拆成了哪些可验证目标

这次实现最终落成六张任务票：

| 任务 | 解决的问题 |
| --- | --- |
| T-01 | 建立可选的 Nacos 配置客户端、启动拉取、监听和状态摘要。 |
| T-02 | 建立三类安全热更新参与者，并保证候选配置整体成功或整体失败。 |
| T-03 | 把固定官方 Nacos 镜像、MySQL schema、最小数据库权限和持久化放进发布基础设施。 |
| T-04 | 在系统管理下增加“配置中心”菜单，并接入现有动态菜单路由。 |
| T-05 | 通过 Nginx 同源代理和 iframe 嵌入官方 Nacos 页面。 |
| T-06 | 用两台真实后端实例完成正常、异常、断线、重启和恢复的收敛验证。 |

规格事实源在 `<Path>{roots.state}/specdev/changes/2026-08-31-optional-nacos-dynamic-config/spec.md</Path>`，设计取舍在 `<Path>{roots.state}/specdev/changes/2026-08-31-optional-nacos-dynamic-config/ADR.md</Path>`，每张任务的实际验证证据在 `<Path>{roots.state}/specdev/changes/2026-08-31-optional-nacos-dynamic-config/evidence/</Path>`。

### 2. 为什么新建 `ruoyi-common-nacos`

核心能力位于：

```text
<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-nacos/</Path>
```

它是一个薄公共模块，直接依赖官方 `nacos-client`，但不把业务规则塞进 Nacos SDK 适配层。

```text
官方 Nacos SDK
      |
      v
[ruoyi-common-nacos]
      |
      +-- 负责通信和通用安全协议
      |
      +-- 提供 NacosConfigParticipant<T>
                      |
          +-----------+-----------+
          |                       |
          v                       v
    common-web / notify      ruoyi-system OSS
    各自保有业务校验         保有下载 TTL 边界
```

这样做的原因是：公共模块知道“怎样拉取和原子切换”，具体业务模块知道“什么值才合法”。如果以后要让另一个配置支持热更新，它只需实现同一参与者接口，不需要改写 SDK 监听主流程。

模块已加入 common 聚合、BOM 和需要使用它的 Maven 模块。`ruoyi-admin` 的完整装配会带上它，但是否真的创建 Nacos 客户端仍由运行开关决定。

### 3. 为什么默认关闭

应用本地配置新增了这一组入口：

```yaml
nacos:
  config:
    enabled: ${NACOS_CONFIG_ENABLED:false}
    server-addr: ${NACOS_CONFIG_SERVER_ADDR:127.0.0.1:8848}
    username: ${NACOS_CONFIG_USERNAME:}
    password: ${NACOS_CONFIG_PASSWORD:}
    group: DEFAULT_GROUP
    data-id: ruoyi-namewta.yml
    timeout-ms: ${NACOS_CONFIG_TIMEOUT_MS:3000}
    namespaces:
      local: ${NACOS_CONFIG_NAMESPACE_LOCAL:local}
      dev: ${NACOS_CONFIG_NAMESPACE_DEV:dev}
      prod: ${NACOS_CONFIG_NAMESPACE_PROD:prod}
```

开关默认是 `false`，所以原有部署不增加任何外部依赖：

```text
NACOS_CONFIG_ENABLED=false
             |
             v
启动处理器立即返回
             |
             +-- 不创建 SDK 客户端
             +-- 不访问 8848
             +-- 不注册监听器
             +-- 完整使用本地 YAML
```

只有显式组合 `<Path>release-artifacts/docker/overrides/nacos-enabled.yml</Path>` 时，两台后端才会同时收到 `NACOS_CONFIG_ENABLED=true` 和连接凭据，并等待 Nacos 健康后启动。

因此“没有 Nacos 也没关系”不是口头约定，而是默认配置和启动分支共同保证的行为。

### 4. 应用启动时发生了什么

`NacosConfigEnvironmentPostProcessor` 在 Spring 完成普通配置数据加载后、创建业务 Bean 前运行。

```text
[读取 application.yml / application-prod.yml]
                   |
                   v
[EnvironmentPostProcessor 检查 enabled]
                   |
          +--------+--------+
          |                 |
       false              true
          |                 |
          v                 v
    [直接继续]       [创建 manager 和 SDK client]
                            |
                            v
                     [首次 fetch 配置]
                            |
                 +----------+----------+
                 |                     |
               成功                  失败
                 |                     |
                 v                     v
          [解析并放入覆盖层]      [保留本地基线]
                 |                     |
                 +----------+----------+
                            |
                            v
                       [注册监听]
                            |
                            v
                     [Spring 创建 Bean]
```

首次拉取必须早于 Bean 创建，是因为普通 `@ConfigurationProperties` 只会在创建时绑定一次。这样，启动时 Nacos 中的合法配置能够参与首次绑定。

首次拉取失败不会阻止应用启动。系统仍注册监听器，使用本地配置继续运行；Nacos 恢复并推送有效配置后，应用可以重新收敛。

### 5. 一份远程配置怎样被准确定位

Nacos 不是只靠文件名找配置，而是使用三个坐标：

```text
namespace + group + dataId

prod      + DEFAULT_GROUP + ruoyi-namewta.yml
```

应用先从 Spring profile 推导 namespace：

```text
local profile -> NACOS_CONFIG_NAMESPACE_LOCAL -> 默认 local
dev profile   -> NACOS_CONFIG_NAMESPACE_DEV   -> 默认 dev
prod profile  -> NACOS_CONFIG_NAMESPACE_PROD  -> 默认 prod
```

这能防止开发配置被生产实例误读。namespace 在控制台创建后，部署变量中的值必须与真实 namespace ID 完全一致。

### 6. “稀疏覆盖”到底是什么意思

Nacos 中只写需要改变的键。例如：

```yaml
captcha:
  enable: false
```

没有写出的数据库、Redis、OSS、日志等配置仍来自本地 YAML。系统使用 Spring Boot 自带的 `YamlPropertySourceLoader` 解析远程文档，再把叶子属性摊平成 Spring 能查询的键。

```text
captcha:
  enable: false
  charLength: 5

          解析后
             |
             v
captcha.enable      = false
captcha.charLength  = 5
```

这里没有手写字符串切割器，因此 YAML 的列表、嵌套和 Spring 属性命名规则仍走标准解析能力。非法 YAML 会以 `INVALID_YAML` 拒绝。

最终优先级是：

```text
高优先级

命令行参数 / JVM system properties / 系统环境变量
                         |
                         v
                  Nacos 稀疏覆盖
                         |
                         v
            本地 application*.yml

低优先级
```

这意味着紧急部署变量仍能压过 Nacos。Nacos 不会夺走容器平台和启动命令的最终控制权。

### 7. 运行中收到新 YAML 后怎样避免“改一半”

核心对象 `NacosConfigManager` 用一个 `AtomicReference<Snapshot>` 保存当前完整状态。可以把它想成一个只能整盒替换的透明盒子：

```text
当前盒子 A
+--------------------------------------+
| overlay: 当前远程键值                 |
| prepared: 三个参与者的已校验快照      |
| state: 摘要、计数、时间、错误码        |
+--------------------------------------+

新配置到达
   |
   v
先在旁边准备盒子 B
   |
   +-- YAML 能解析吗？
   +-- 保护键和已知类型合法吗？
   +-- 三个参与者能全部准备成功吗？
   +-- 哪些键立即生效，哪些要重启？
   |
   +-- 全部成功 --> 一次指针切换：A -> B
   |
   +-- 任一失败 --> 丢弃 B，继续使用 A
```

“原子”在这里不是说服务器永远不出错，而是说一次配置发布不会让验证码采用新值、通知模块却还停在旧值。全部参与者先准备成功，系统才一次性切换。

候选配置的处理顺序是：

```text
Nacos 推送文本
      |
      v
标准 YAML 解析
      |
      v
通用保护键、类型和跨字段校验
      |
      v
为所有 NacosConfigParticipant 执行 prepare
      |
      v
统计 immediateKeyCount / restartKeyCount
      |
      v
原子发布 overlay + prepared snapshots + state
```

任何一步失败，上一份有效配置和上一组业务快照都不变，只把状态标记为 `REJECTED` 并记录错误类别。

### 8. 哪些错误会被挡住

当前有四类清晰的拒绝结果：

| 错误码 | 代表什么 | 例子 |
| --- | --- | --- |
| `INVALID_YAML` | YAML 语法无法解析。 | 缩进破坏、列表结构错误。 |
| `PROTECTED_KEY` | 试图从远程改变配置系统自身或 profile。 | `nacos.config.enabled`、`spring.profiles.active`。 |
| `KNOWN_TYPE_INVALID` | 已知键无法转换成要求的类型或不是正数。 | `captcha.enable: maybe`、负数时长。 |
| `PARTICIPANT_REJECTED` | 单项能解析，但组合违反业务约束。 | 通知默认窗口小于最小窗口、OSS TTL 超过本地上限。 |

保护 `nacos.config.*` 是为了防止远程配置修改自己的连接地址、开关或凭据，造成自我切断或越权。保护 `spring.profiles.*` 是为了防止运行中的生产实例被远程伪装成其他环境。

### 9. 三个即时参与者怎样工作

#### 验证码

`CaptchaProperties` 认领 `captcha.*`。它把以下值准备成一份不可变快照：

```text
enable + type + numberLength + charLength
```

要求开关非空、类型非空、两个长度为正数。验证码控制器、登录和注册流程在每次业务调用时读取 `currentSnapshot()`，所以发布后无需重启。

#### 通知幂等窗口

`NotifyIdempotencyProperties` 认领 `notify.idempotency.*`：

```text
minWindow <= defaultWindow <= maxWindow
并且三个时长都必须大于 0
```

通知协调器每次操作读取当前不可变快照。三项作为一个整体切换，不会短暂出现彼此矛盾的窗口。

#### OSS 下载签名时长

`OssLifecycleProperties` 只认领精确键：

```text
oss.lifecycle.download-ttl
```

本地 YAML 中的 `download-ttl-min` 和 `download-ttl-max` 是安全边界，不允许被 Nacos 当作即时配置一起改变。远程 TTL 必须落在本地边界内，之后新的签名请求会读取当前值。

三者共同遵守：

```text
一次业务请求开始
       |
       v
读取当时的完整不可变快照
       |
       v
本次请求始终使用同一组值
```

因此不会在一次请求执行到一半时读到前后不一致的字段。

### 10. 为什么不是所有 YAML 都热更新

Spring 中很多配置会在 Bean 创建时被复制到对象、连接池或客户端里。仅仅让 `Environment` 出现新值，并不会自动重建这些对象。

```text
远程值变了
   |
   +-- 参与者配置 --> 业务代码主动读取当前快照 --> 立即生效
   |
   +-- 普通配置 ----> 旧 Bean 仍持有启动时的值 ----> 重启后生效
```

如果盲目宣称“所有 YAML 都热更新”，数据库连接地址、线程池大小、端口、鉴权密钥等状态很可能只更新一部分，风险比重启更大。

当前方案采用显式参与者模式：谁能够证明自己的校验、切换和并发行为是安全的，谁才进入即时集合。其他合法键仍可由 Nacos集中管理，但状态中的 `restartKeyCount` 会告诉运维人员它们需要重启应用。

这就是此前所问“白名单模式”的实际含义：不是限制哪些键能写，而是明确列出哪些键获得“运行中立即切换”的资格。

### 11. 删除配置、断线和重启时发生什么

状态变化可以简化成：

```text
                      +------------------+
                      | Nacos 未启用      |
                      | LOCAL_BASELINE   |
                      +------------------+

已启用，首次拉取失败 ----------------> 本地基线 + 继续监听
        |
        | 后来收到有效 YAML
        v
   APPLIED，使用远程覆盖
        |
        +-- 收到非法 YAML --> REJECTED，保留上一份有效覆盖
        |
        +-- Nacos 断线 ----> 保留内存中的上一份有效覆盖，connected=false
        |
        +-- 删除/清空配置 -> 清空覆盖，立即回到本地基线
```

还有一个刻意锁定的边界：Nacos Java 客户端的本地磁盘快照被显式关闭。

```text
应用正在运行时 Nacos 断线
    -> 可继续使用本进程内上一份有效配置

应用在 Nacos 离线时重新启动
    -> 不读取旧磁盘快照
    -> 使用完整本地 YAML 基线
```

这样可以避免进程重启后偷偷采用一份已经过期、运维人员却看不到来源的远程缓存。

### 12. 两台后端怎样收敛

两台实例各自创建 SDK 客户端和监听器，不通过另一台实例转发：

```text
             [同一次 Nacos 发布]
                   /       \
                  /         \
                 v           v
        [server1 独立校验] [server2 独立校验]
                 |           |
                 v           v
           [自己的快照]   [自己的快照]
```

短时间内到达先后可以不同，但相同有效候选应让两台实例最终得到相同摘要和计数。某一台暂时断线时，另一台仍可更新；断线实例恢复后会重新收到当前配置并收敛。

### 13. 怎样观察状态，又怎样避免泄密

启用 Nacos 后，Actuator `info` 中增加 `nacosConfig` 摘要。它只包含：

```text
enabled
connected
digest
result
lastSuccessAt
immediateKeyCount
restartKeyCount
errorCode
```

它不返回远程 YAML 正文、属性值、用户名、密码或 token。摘要回答的是“当前配置是否连通、是否采用、有没有待重启键”，而不是“秘密配置具体是什么”。

当 Nacos 功能关闭时，这个 contributor 不创建，原有 Actuator 行为保持不变。

### 14. Nacos 基础设施增加了什么

发布目录新增的 Nacos 服务使用固定官方镜像：

```text
nacos/nacos-server:v2.5.4
```

主要运行边界如下：

| 项目 | 实现 |
| --- | --- |
| 模式 | `standalone`，适合当前单节点基础设施，不是高可用集群。 |
| 存储 | 使用 MySQL 独立 `nacos` 数据库，不依赖容器临时文件。 |
| 端口 | 8848、9848 默认绑定 `${NAMEWTA_BIND_HOST:-127.0.0.1}`。 |
| 数据目录 | 日志和数据落到 `${NAMEWTA_DATA_ROOT}/nacos/`。 |
| 鉴权 | 开启 Nacos auth，要求 token、identity key 和 identity value。 |
| 白名单 | 关闭 user-agent 白名单，不允许客户端绕过鉴权。 |
| 健康检查 | 调用官方 readiness 地址，MySQL 健康后才启动 Nacos。 |

“固定官方推荐”在这里落实为版本固定和 schema 固定，而不是使用浮动的 `latest`。Nacos 2.5.4 官方 MySQL schema 随发布产物保存，并记录来源与校验值。

### 15. 为什么 MySQL 初始化分成两条路

Docker 的 `/docker-entrypoint-initdb.d` 只在全新数据目录时运行，所以必须区分：

```text
全新 MySQL 数据目录
    |
    v
15-nacos-init.sh 自动创建数据库、10 张表和账号

已经存在的 MySQL 数据目录
    |
    v
init-nacos-mysql-container.sh 幂等补建并校验
```

两条路最终都要得到同一结果：

- 独立 `nacos` 数据库。
- 固定的 10 张 Nacos 表。
- 独立数据库用户。
- 该用户只有 `SELECT`、`INSERT`、`UPDATE`、`DELETE`。
- 该用户不能读取业务数据库，也不能建表或删库。

这避免了为了运行一个中间件，把 MySQL root 权限长期交给 Nacos。

### 16. 前端究竟怎样“连接”Nacos

前端链路从数据库菜单开始：

```text
[sys_menu 中的“配置中心”]
  path      = nacos
  component = monitor/nacos/index
  perms     = system:nacos:console
               |
               v
[后端登录接口返回动态菜单]
               |
               v
[adminManifestRegistry 匹配 component key]
               |
               v
[AdminExternalMonitorPage，target=nacos]
               |
               v
[读取 VITE_APP_NACOS_ADMIN]
               |
               v
[权限检查 + URL 安全检查]
               |
               v
[通用 IFrame 组件加载 /nacos/]
```

相关前端实现位于：

- `<Path>plus-ui-namewta/apps/admin-web/src/router/adminManifestRegistry.ts</Path>`：把后端组件键映射为 Vue 页面。
- `<Path>plus-ui-namewta/apps/admin-web/src/views/monitor/external/index.vue</Path>`：统一外部运维页面容器。
- `<Path>plus-ui-namewta/packages/domains/system/src/monitor/index.ts</Path>`：权限映射与 URL 安全策略。

开发环境可把 `VITE_APP_NACOS_ADMIN` 配为 `http://localhost:8848/nacos/`，标准生产环境配为同源相对路径 `/nacos/`。

前端只负责“打开哪一页”，不持有后端 Nacos 客户端账号，也不会把 Nacos 密码自动塞入 iframe。

### 17. 两层权限为什么缺一不可

这里存在两道门：

```text
第一道门：RuoYi 菜单权限
system:nacos:console
    |
    +-- 决定用户能否看到并打开“配置中心”入口

第二道门：Nacos 官方登录和权限
    |
    +-- 决定用户能查看哪个 namespace、能否发布或删除配置
```

SQL 只创建菜单，不自动授权给普通角色。管理员必须显式授予入口权限。即使用户通过第一道门，iframe 中仍显示 Nacos 官方登录页；当前没有单点登录、免密跳转或 token 注入。

这个分离很重要：RuoYi 用户身份不自动等于 Nacos 运维身份。

### 18. Nginx 为什么要代理 `/nacos/`

生产前端把 iframe 指向 `/nacos/`，Nginx 再转给 Docker 网络中的 `nacos:8848`：

```text
浏览器访问 https://namewta.example/nacos/
                   |
                   v
            [同一个 Nginx]
                   |
                   v
        http://nacos:8848/nacos/
```

同源代理减少了跨域配置和浏览器安全策略冲突。HTTP 与 TLS 模板都保留原始 `/nacos/` 路径，转发 Host、客户端地址、协议以及 WebSocket upgrade，并关闭代理缓冲、延长读取超时以适配控制台连接。

它没有增加通配 CORS，也没有注入 Nacos 身份。Nacos 不可用时，页面会在连接超时后表现为维护故障；服务恢复后重新加载即可。

### 19. 安全边界在哪里

这套实现解决了鉴权、最小权限和秘密不入库代码的问题，但 Nacos 配置本身不是秘密保险箱。

```text
Nacos 配置正文
    +-- 经 Nacos HTTP/SDK 传输
    +-- 以明文业务配置保存在 MySQL 中
```

因此必须：

- 把 8848 和 9848 留在回环地址或可信内网，不直接暴露公网。
- 对外访问走受控的 TLS Nginx 入口。
- 把数据库密码、Nacos token、identity 和客户端密码放在部署环境，不写入 Git。
- 生产上为后端创建只读配置账号，管理账号只给真正的运维人员。
- 不把长期密钥、私钥或高敏感凭据当普通 YAML 发布。

当前 Nacos 是单节点 standalone，能满足可选配置中心需求，但不能承诺集群级高可用。若未来要求 Nacos 自身无单点，需要另行设计集群、数据库容灾和负载入口。

### 20. 真实联调发现并修正了什么

完整 E2E 不只是“证明成功”，还暴露了五个实际问题并留下修正：

| 发现 | 风险 | 修正 |
| --- | --- | --- |
| Nacos SDK 默认会使用本地快照。 | 离线重启可能读到过期远程状态。 | 构造客户端前显式关闭 SDK snapshot。 |
| 全新 MySQL 导入部分中文 SQL 时字符集未先声明。 | 数据库初始化会在应用启动前失败。 | 六份初始化源统一建立 `utf8mb4` 会话合同。 |
| 启动早期参与者尚未注册时，跨字段非法值可能先进入环境。 | 初始 Bean 可能绑定到本应拒绝的组合。 | 在 Bean 创建前预校验已知通知窗口和 OSS TTL 组合。 |
| E2E 最初看聚合 Actuator health。 | 可选 OSS 不可用会误判应用进程失败。 | 探针与 Compose 一致，改用应用 TCP readiness。 |
| Docker 临时端口在 stop/start 后变化。 | 测试会访问旧端口，无法判断真实收敛。 | 测试生命周期内预留并固定回环端口。 |

这些修正说明系统行为是在真实容器、真实 MySQL、真实 Nacos 和真实双实例重启中验证出来的，不只是通过 mock 推测。

### 21. 验证覆盖到了什么程度

最终证据包括：

| 层级 | 已验证内容 |
| --- | --- |
| common-nacos 单元测试 | 16 项，覆盖开关、解析、优先级、监听、校验、原子拒绝、删除与状态。 |
| 后端行为测试 | 7 项，覆盖三个参与者、菜单 DML 和敏感信息边界。 |
| Maven 组合构建 | full/core 共 42 模块门禁通过。 |
| 前端 | 构建、lint、typecheck，以及 8 个测试文件共 41 项测试。 |
| 发布与代理静态合同 | 18 项，覆盖 Compose、schema、变量、Nginx 和秘密规则。 |
| 真实浏览器 iframe | Chromium 3/3，通过菜单、权限和官方登录页嵌入验证。 |
| 真实远程全栈 E2E | 1/1，约 117 秒，覆盖两实例、MySQL、Nacos、故障和恢复矩阵。 |

远程矩阵实际走过：全新初始化、默认关闭、鉴权边界、稀疏优先级、即时与重启键混合、非法 YAML、保护键、错误类型、参与者拒绝、删除配置、单实例故障、Nacos 断线、离线重启、本地基线、恢复、Nacos 重启、MySQL 持久化、最终清理和秘密扫描。

用户指定服务器上的运行是隔离验收，不是正式常驻部署。验收结束后测试容器、网络和目录已清理，既有中间件保持不变。

### 22. 正常回退顺序是什么

回退不需要删数据库：

```text
1. 在 Nacos 删除或清空 ruoyi-namewta.yml
                  |
                  v
2. 等两台实例回到本地基线
                  |
                  v
3. 重启应用时不再组合 nacos-enabled.yml
                  |
                  v
4. 确认 NACOS_CONFIG_ENABLED=false
                  |
                  v
5. 再停止 Nacos，保留 MySQL 和数据目录
```

不要使用 `down -v` 作为普通回退动作，因为它会删除持久卷，扩大影响范围。先让应用脱离远程配置，再停中间件，故障面最清楚。

### 23. 以后怎样增加第四个热更新配置

扩展路径是显式的：

```text
选择一个业务配置
      |
      v
定义不可变 Snapshot
      |
      v
实现 NacosConfigParticipant<T>
      |
      +-- 唯一 id
      +-- prefixes 或 exactKeys
      +-- prepare(Binder) 完成绑定和全部业务校验
      |
      v
业务入口每次读取 accessor 中的当前快照
      |
      v
补充成功、拒绝、并发一致性和回退测试
```

只有完成这条链，新的配置才应该从 `restartKeyCount` 移到 `immediateKeyCount`。单独加一个 Nacos 键或 `@RefreshScope` 标签并不足以证明业务切换安全。

### 24. 关键源码地图

| 关注点 | 位置 |
| --- | --- |
| 启动前处理和 SDK 生命周期 | `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-nacos/src/main/java/org/dromara/common/nacos/</Path>` |
| 应用 Nacos 连接默认值 | `<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/application.yml</Path>` |
| 验证码热更新参与者 | `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-web/src/main/java/org/dromara/common/web/config/properties/CaptchaProperties.java</Path>` |
| 通知幂等热更新参与者 | `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-notify/src/main/java/org/dromara/common/notify/idempotency/NotifyIdempotencyProperties.java</Path>` |
| OSS TTL 热更新参与者 | `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/config/OssLifecycleProperties.java</Path>` |
| 配置中心菜单 DML | `<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DML.sql</Path>` |
| 前端动态组件注册 | `<Path>plus-ui-namewta/apps/admin-web/src/router/adminManifestRegistry.ts</Path>` |
| iframe 页面 | `<Path>plus-ui-namewta/apps/admin-web/src/views/monitor/external/index.vue</Path>` |
| 前端权限与 URL 校验 | `<Path>plus-ui-namewta/packages/domains/system/src/monitor/index.ts</Path>` |
| Nacos Docker 服务 | `<Path>release-artifacts/docker/docker-compose-infrastructure.yml</Path>` |
| 显式启用覆盖 | `<Path>release-artifacts/docker/overrides/nacos-enabled.yml</Path>` |
| MySQL 初始化与 E2E | `<Path>release-artifacts/scripts/</Path>` |
| Nginx 同源代理 | `<Path>release-artifacts/docker/frontend/nginx/lb/</Path>` |

已有的实际配置和启动手册位于 `<Path>{roots.state}/specdev/changes/2026-08-31-optional-nacos-dynamic-config/01_Nacos配置与启动.md</Path>`。本文解释设计和完整改动，手册负责给出部署命令，两者用途不同。

## 术语小词典

| 术语 | 用最简单的话说 |
| --- | --- |
| Nacos | 保存和发布远程配置的独立服务，同时带官方管理控制台。 |
| 本地基线 | 应用包内完整的 `application*.yml`；没有 Nacos 时仍足够启动。 |
| 稀疏覆盖 | Nacos 只写想改的少量键，没写的继续用本地值。 |
| namespace | 环境隔离格子，例如 local、dev、prod。 |
| group | 同一 namespace 内的进一步分组，本项目固定为 `DEFAULT_GROUP`。 |
| dataId | 一份配置的名字，本项目固定为 `ruoyi-namewta.yml`。 |
| SDK | 后端用来连接 Nacos、拉取和监听配置的官方 Java 客户端。 |
| 长轮询 | 客户端保持等待，服务端有变化时通知客户端，不需要人反复刷新。 |
| Environment | Spring 汇总本地 YAML、远程覆盖、环境变量等配置来源的容器。 |
| PropertySource | Environment 中的一层配置来源；越靠前通常优先级越高。 |
| Participant | 声明某组配置可以怎样绑定、校验和立即切换的业务参与者。 |
| Snapshot | 某一时刻完整、不可变的一组业务配置值。 |
| 原子切换 | 新配置全部准备成功后一次替换；失败时一项也不替换。 |
| digest | 配置文本的 SHA-256 摘要，用来比较版本，不暴露正文。 |
| iframe | 在管理系统页面内部显示另一个网页的浏览器容器。 |
| 同源代理 | 浏览器访问本站 `/nacos/`，由 Nginx 在服务器内部转给 Nacos。 |
| Actuator | Spring Boot 暴露运行状态的管理能力；这里只给出脱敏摘要。 |
| readiness | 服务是否已准备好接收请求的健康判断。 |
| standalone | 单节点运行模式，部署简单，但不是高可用集群。 |
| E2E | 从真实基础设施到浏览器或双后端的端到端验证。 |

## 你现在能复述什么

读完后，应该能用自己的话说明下面五件事：

1. Nacos 是可选的远程稀疏覆盖层，本地 YAML 始终是完整基线；默认关闭时应用完全不连接 Nacos。
2. 后端启动时会先拉取远程配置，运行中再监听变化；每份候选都要整体解析、校验、准备并原子切换，失败则保留上一份有效值。
3. 当前只有 `captcha.*`、`notify.idempotency.*` 和 `oss.lifecycle.download-ttl` 能立即生效，其余合法键需要重启，不存在“所有 YAML 自动热更新”。
4. 前端没有实现 Nacos API，也没有连接配置 SDK；它通过动态菜单、权限检查、同源 `/nacos/` 代理和 iframe 嵌入官方控制台，而且仍需独立登录 Nacos。
5. 发布侧用固定官方 Nacos 2.5.4、独立 MySQL 数据库和最小权限账号；断线、错误配置、删除、双实例重启与持久化都经过真实 E2E 验证，并且可以按“先清远程覆盖、再关闭客户端、最后停 Nacos”的顺序回退。
