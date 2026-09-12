# Alloy、Loki、Grafana 接入配置详解

这份图解只聚焦一件事：一条后端日志究竟怎样从 Java 应用走到 Grafana 页面。

读完后应能回答三个问题：

1. 三个服务怎样通过目录、网络和 HTTP 接起来？
2. 每一份配置文件分别控制什么？
3. 日志不出现时，应该沿哪条链逐段排查？

先记住：当前接入不是“后端调用 Loki API”，而是“后端写文件，Alloy 在旁边读文件”。

```text
[后端业务代码]
      |
      | 普通 Java 日志
      v
[Logback 写文件]
      |
      | Docker 共享宿主机目录
      v
[Alloy 追踪新增行]
      |
      | HTTP push
      v
[Loki 保存和检索]
      |
      | HTTP query
      v
[Grafana 查询并展示]
      |
      v
[运维人员的浏览器]
```

本图解释的是当前仓库配置定义的行为。本次没有实际启动容器，因此不把配置存在等同于运行环境已经健康。

## 先看全图

### 一条日志的完整旅程

```text
后端容器 ruoyi-server1

[/ruoyi/server/logs/sys-console.log]
                |
                | bind mount：两个路径指向同一文件
                v
宿主机

[NAMEWTA_DATA_ROOT/logs/server1/sys-console.log]
                |
                | Alloy 把整个 logs 根目录只读挂载进来
                v
Alloy 容器

[/var/log/ruoyi/server1/sys-console.log]
                |
                | 匹配 *.log，附上三张标签
                | job=ruoyi
                | service=ruoyi-admin
                | instance=server1
                v
[loki.source.file]
                |
                | forward_to
                v
[loki.write.local.receiver]
                |
                | POST 到内部地址
                v
Loki 容器

[http://loki:3100/loki/api/v1/push]
                |
                | 写入 TSDB 索引和日志数据块
                v
[宿主机 NAMEWTA_DATA_ROOT/observability/loki]
                |
                | Grafana 服务端查询
                v
Grafana 容器

[http://loki:3100]
                |
                | 数据源 uid=loki
                | 查询 {job="ruoyi"}
                v
[NAMEWTA Logs 仪表盘]
                |
                | 宿主机端口 43000
                v
[浏览器 http://127.0.0.1:43000]
```

同一份文件在图中出现了三个路径名称，但不是复制三次：

```text
应用看到：/ruoyi/server/logs/sys-console.log

宿主机看到：NAMEWTA_DATA_ROOT/logs/server1/sys-console.log

Alloy 看到：/var/log/ruoyi/server1/sys-console.log

三个名称 --通过目录映射--> 同一批宿主机数据
```

### 配置文件地图

```text
[后端如何写文件]
  <Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/logback-plus.xml</Path>
  <Path>release-artifacts/docker/docker-compose-backend.yml</Path>

[Alloy 如何找到并发送文件]
  <Path>release-artifacts/docker/docker-compose-observability.yml</Path>
  <Path>release-artifacts/docker/observability/alloy/config.alloy</Path>

[Loki 如何保存和清理]
  <Path>release-artifacts/docker/observability/loki/loki.yml</Path>

[Grafana 如何自动接入 Loki]
  <Path>release-artifacts/docker/observability/grafana/provisioning/datasources/loki.yml</Path>
  <Path>release-artifacts/docker/observability/grafana/provisioning/dashboards/namewta.yml</Path>
  <Path>release-artifacts/docker/observability/grafana/dashboards/namewta-logs.json</Path>

[运行变量和启动入口]
  <Path>release-artifacts/.env.example</Path>
  <Path>release-artifacts/scripts/docker-manage.sh</Path>
```

## 一步一步看

### 第一步：用同一个数据根把不同 Compose 接起来

`.env` 中最关键的三个变量是：

```dotenv
NAMEWTA_BIND_HOST=127.0.0.1
NAMEWTA_DATA_ROOT=./runtime
NAMEWTA_NETWORK=ruoyi-namewta-network
```

它们分别表示：

| 变量 | 日常解释 | 当前作用 |
| --- | --- | --- |
| `NAMEWTA_BIND_HOST` | 哪张网卡允许人访问 | Grafana 默认只发布到本机 |
| `NAMEWTA_DATA_ROOT` | 所有持久数据放在哪里 | 后端日志、Alloy 状态、Loki 数据和 Grafana 数据共享同一个根 |
| `NAMEWTA_NETWORK` | 容器共同加入哪个私有网络 | Alloy 用 `loki` 服务名连接 Loki，Grafana也用同一服务名查询 |

四份 Compose 是分别启动的项目。它们能够共享日志，不是因为 Compose 自动互相发现文件，而是因为它们读取同一份 `.env`，并使用相同的 `NAMEWTA_DATA_ROOT`。

```text
[backend Compose]
  写 NAMEWTA_DATA_ROOT/logs/server1
  写 NAMEWTA_DATA_ROOT/logs/server2
  写 NAMEWTA_DATA_ROOT/logs/snailjob
  写 NAMEWTA_DATA_ROOT/logs/snailai

[observability Compose]
  读 NAMEWTA_DATA_ROOT/logs

相同的根目录
      |
      v
跨 Compose 共享文件成功
```

如果两个 Compose 使用了不同的 `.env` 或不同的 `NAMEWTA_DATA_ROOT`，后端仍会正常写日志，但 Alloy 会看到另一个空目录，Grafana最终没有日志。

### 第二步：后端 Logback 创建可采集文件

主 Admin 的 `<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/logback-plus.xml</Path>` 核心配置是：

```xml
<property name="log.path" value="${LOG_PATH:-./logs}"/>

<appender name="file_console"
          class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>${log.path}/sys-console.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
        <fileNamePattern>${log.path}/sys-console.%d{yyyy-MM-dd}.log.gz</fileNamePattern>
        <maxHistory>60</maxHistory>
        <totalSizeCap>40GB</totalSizeCap>
    </rollingPolicy>
</appender>
```

逐项翻译：

```text
LOG_PATH
  -> 外部可以决定日志目录
  -> 未提供时使用 ./logs

sys-console.log
  -> 当前正在追加的新日志文件

sys-console.日期.log.gz
  -> 每天把旧文件压缩归档

maxHistory=60
  -> 最多保留 60 个历史日期

totalSizeCap=40GB
  -> 历史归档总量最多 40GB
```

`<Path>release-artifacts/docker/docker-compose-backend.yml</Path>` 再把容器目录连接到宿主机：

```yaml
ruoyi-server1:
  environment:
    LOG_PATH: /ruoyi/server/logs
  volumes:
    - "${NAMEWTA_DATA_ROOT:-./runtime}/logs/server1:/ruoyi/server/logs"

ruoyi-server2:
  environment:
    LOG_PATH: /ruoyi/server/logs
  volumes:
    - "${NAMEWTA_DATA_ROOT:-./runtime}/logs/server2:/ruoyi/server/logs"
```

两个容器内部路径相同，但宿主机目录不同：

```text
server1 /ruoyi/server/logs
  -> 宿主机 logs/server1

server2 /ruoyi/server/logs
  -> 宿主机 logs/server2
```

这就是 Alloy 后面能够给两个实例贴不同 `instance` 标签的物理基础。

其他 Java 服务使用同一模式：

| 服务 | 宿主机日志目录 | Alloy 中的目录 |
| --- | --- | --- |
| Admin 一号 | `logs/server1` | `/var/log/ruoyi/server1` |
| Admin 二号 | `logs/server2` | `/var/log/ruoyi/server2` |
| Monitor Admin | `logs/monitor` | `/var/log/ruoyi/monitor` |
| SnailJob | `logs/snailjob` | `/var/log/ruoyi/snailjob` |
| SnailAI | `logs/snailai` | `/var/log/ruoyi/snailai` |

注意：这里还没有发生网络传输。到这一步为止，只是应用把文本写进了宿主机文件。

### 第三步：Compose 给 Alloy 三样东西

Alloy 服务的当前配置是：

```yaml
alloy:
  image: grafana/alloy:v1.11.0
  command:
    - run
    - --storage.path=/var/lib/alloy/data
    - /etc/alloy/config.alloy
  volumes:
    - ./observability/alloy/config.alloy:/etc/alloy/config.alloy:ro
    - "${NAMEWTA_DATA_ROOT:-./runtime}/logs:/var/log/ruoyi:ro"
    - "${NAMEWTA_DATA_ROOT:-./runtime}/observability/alloy:/var/lib/alloy/data"
  depends_on: [loki]
  networks: [namewta]
```

三次挂载分别提供：

```text
第一份：配置说明书
  宿主机 config.alloy
    -> 容器 /etc/alloy/config.alloy
    -> ro，只读，容器不能改项目配置

第二份：待采集日志
  宿主机 NAMEWTA_DATA_ROOT/logs
    -> 容器 /var/log/ruoyi
    -> ro，Alloy 只能读，不能改后端日志

第三份：Alloy 自己的运行状态
  宿主机 NAMEWTA_DATA_ROOT/observability/alloy
    -> 容器 /var/lib/alloy/data
    -> 重建容器后仍保留组件状态
```

`--storage.path` 指向第三个挂载。它让 Alloy 的组件状态有持久位置；对文件采集而言，读取进度相关状态不必只活在一次容器生命周期中。但它不应被当作完整日志备份，真正可补采的数据仍来自原始日志文件。

`depends_on: [loki]` 只表示 Docker 先发起 Loki 容器，再发起 Alloy 容器。当前 Loki 没有 Compose healthcheck，所以它不保证 Alloy 启动时 Loki 已经真正可以接收请求。

### 第四步：Alloy 先制作“文件地址簿”

`<Path>release-artifacts/docker/observability/alloy/config.alloy</Path>` 的第一段是：

```hcl
local.file_match "ruoyi_logs" {
  path_targets = [
    {
      "__path__" = "/var/log/ruoyi/server1/*.log",
      "service"  = "ruoyi-admin",
      "instance" = "server1",
      "job"      = "ruoyi"
    },
    {
      "__path__" = "/var/log/ruoyi/server2/*.log",
      "service"  = "ruoyi-admin",
      "instance" = "server2",
      "job"      = "ruoyi"
    }
  ]
}
```

实际文件还有 monitor、snailjob 和 snailai 三个 target。每个 target 包含两类信息：

```text
__path__
  -> 去哪里找文件
  -> 是采集器内部使用的特殊字段

job / service / instance
  -> 日志发送到 Loki 后保留的检索标签
```

当前五组标签是：

| 文件来源 | `job` | `service` | `instance` |
| --- | --- | --- | --- |
| `server1/*.log` | `ruoyi` | `ruoyi-admin` | `server1` |
| `server2/*.log` | `ruoyi` | `ruoyi-admin` | `server2` |
| `monitor/**/*.log` | `ruoyi` | `ruoyi-monitor-admin` | `monitor` |
| `snailjob/**/*.log` | `ruoyi` | `ruoyi-snailjob-server` | `snailjob` |
| `snailai/**/*.log` | `ruoyi` | `ruoyi-snailai-server` | `snailai` |

`*.log` 表示当前目录下以 `.log` 结尾的文件；`**/*.log` 表示还会进入下级目录寻找。

这带来两个当前事实：

1. 主 Admin 的 `.log.gz` 压缩历史不会被重新当作新文件采集；日志应当在写入活动 `.log` 时被实时读取。
2. SnailJob 和 SnailAI 的 `console.log`、`info.log`、`error.log` 都可能被匹配，上一份图解所说的重复采集风险来自这里。

### 第五步：Alloy 把地址簿交给文件读取器

第二段只有四行：

```hcl
loki.source.file "ruoyi_logs" {
  targets    = local.file_match.ruoyi_logs.targets
  forward_to = [loki.write.local.receiver]
}
```

箭头关系是：

```text
[local.file_match.ruoyi_logs.targets]
        文件路径和静态标签
                  |
                  v
[loki.source.file.ruoyi_logs]
        持续读取匹配文件的新日志行
                  |
                  | forward_to
                  v
[loki.write.local.receiver]
        等待接收日志的内部入口
```

`forward_to` 不是外部 URL，而是 Alloy 配置内部的一根连接线。它把读取结果交给下面名为 `local` 的 `loki.write` 组件。

当前 Alloy 配置没有处理流水线，因此它没有做这些事情：

```text
没有解析 JSON 字段
没有从正文提取 level、requestId、URI 等标签
没有多行异常堆栈合并规则
没有过滤 DEBUG 或敏感日志
没有改写时间戳
```

所以当前基本单位是“文件中的日志行”。主 Admin 的 HTTP 系统日志本来就是单行 JSON，适合进入 Loki；普通 Java 异常堆栈包含多行时，在 Loki 中可能表现为多条相邻日志，而不是一个合并事件。

安全过滤也不在 Alloy 中完成。密码、Token 等内容必须在应用写文件之前脱敏，否则 Alloy 会原样送入 Loki。

### 第六步：Alloy 用 HTTP 把日志推给 Loki

最后一段是：

```hcl
loki.write "local" {
  endpoint {
    url = "http://loki:3100/loki/api/v1/push"
  }
}
```

这个 URL 可以拆成：

```text
http://
  -> 当前 Docker 私有网络内使用明文 HTTP

loki
  -> Compose 服务名
  -> Docker 内部 DNS 把它解析到 Loki 容器

3100
  -> Loki 配置的 HTTP 监听端口

/loki/api/v1/push
  -> Loki 接收日志的写入接口
```

因为 Alloy 和 Loki 都加入 external network `ruoyi-namewta-network`，Alloy 不需要知道 Loki 容器 IP。容器重建后 IP 可以变化，服务名仍保持不变。

Loki 没有发布 `3100:3100` 到宿主机，因此：

```text
同一 Docker 网络中的 Alloy -> 可以访问 loki:3100

宿主机浏览器 -> 默认不能直接访问 127.0.0.1:3100
```

### 第七步：Loki 在单机文件系统中保存日志

Loki 容器配置分成“启动参数”和“运行配置”两层。

Compose 层：

```yaml
loki:
  image: grafana/loki:3.6.0
  user: "0:0"
  command: ["-config.file=/etc/loki/loki.yml"]
  volumes:
    - ./observability/loki/loki.yml:/etc/loki/loki.yml:ro
    - "${NAMEWTA_DATA_ROOT:-./runtime}/observability/loki:/loki"
  networks: [namewta]
```

这里表示：

```text
使用固定 Loki 3.6.0 镜像
  |
以 0:0 用户运行，确保能写挂载目录
  |
从 /etc/loki/loki.yml 读取配置
  |
把 /loki 下的数据保存回宿主机
```

`user: "0:0"` 简化了目录权限问题，但权限较宽。若以后收紧为非 root 用户，需要先让宿主机 Loki 数据目录拥有匹配的 UID/GID，否则 Loki 会因无法写入而启动失败。

Loki 的 `<Path>release-artifacts/docker/observability/loki/loki.yml</Path>` 首先定义入口：

```yaml
auth_enabled: false

server:
  http_listen_port: 3100
```

含义是：Loki 自己不校验租户账号，所有能连到 3100 的网络成员都可尝试调用接口。当前主要保护是“3100 不发布到宿主机”，而不是 Loki 应用层认证。

然后定义存储：

```yaml
common:
  path_prefix: /loki
  storage:
    filesystem:
      chunks_directory: /loki/chunks
      rules_directory: /loki/rules
  replication_factor: 1
  ring:
    kvstore:
      store: inmemory
```

可以画成：

```text
[/loki]
   |
   +--> chunks：日志数据块
   +--> rules：规则目录位置
   +--> compactor：整理和删除工作的目录

replication_factor=1
   -> 每份数据只有一份

ring=inmemory
   -> 单进程内存协调
   -> 容器重启后重新建立
```

这是单机形态，不是多副本高可用集群。宿主机 `observability/loki` 目录损坏或丢失时，没有第二份 Loki 副本接管。

接着定义索引格式：

```yaml
schema_config:
  configs:
    - from: 2024-01-01
      store: tsdb
      object_store: filesystem
      schema: v13
      index:
        prefix: index_
        period: 24h
```

日常解释：

```text
从 2024-01-01 开始使用这一套规则
        |
        v
使用 TSDB v13 组织时间和标签索引
        |
        v
日志数据仍放在本地 filesystem
        |
        v
索引按 24 小时周期组织
```

最后定义保留与清理：

```yaml
limits_config:
  retention_period: 60d
  allow_structured_metadata: true

compactor:
  working_directory: /loki/compactor
  retention_enabled: true
  delete_request_store: filesystem
```

这里需要两个配置配合：

```text
retention_period=60d
  -> 声明日志保留 60 天

retention_enabled=true
  -> 允许 compactor 真正执行过期清理
```

`allow_structured_metadata: true` 只是允许接收结构化元数据；当前 Alloy 配置没有显式添加这类元数据，也没有把 JSON 字段转成标签。

配置中的 `rules_directory` 只是准备了目录。当前没有提供告警规则文件和 ruler 配置，所以这套 Loki 目前负责日志存储与查询，不负责现成告警。

### 第八步：Grafana 启动时自动登记 Loki

Grafana Compose 配置是：

```yaml
grafana:
  image: grafana/grafana:12.2.8
  environment:
    GF_SECURITY_ADMIN_USER: "${GRAFANA_ADMIN_USER:?required}"
    GF_SECURITY_ADMIN_PASSWORD: "${GRAFANA_ADMIN_PASSWORD:?required}"
    GF_USERS_ALLOW_SIGN_UP: "false"
  ports:
    - "${NAMEWTA_BIND_HOST:-127.0.0.1}:43000:3000"
  volumes:
    - ./observability/grafana/provisioning:/etc/grafana/provisioning:ro
    - ./observability/grafana/dashboards:/var/lib/grafana/dashboards:ro
    - "${NAMEWTA_DATA_ROOT:-./runtime}/observability/grafana:/var/lib/grafana"
  depends_on: [loki]
  networks: [namewta]
```

三份目录的职责不同：

```text
/etc/grafana/provisioning
  -> 声明“启动时应该自动创建哪些数据源和 dashboard provider”
  -> 来自项目只读文件

/var/lib/grafana/dashboards
  -> 项目提供的仪表盘 JSON
  -> 只读，不让页面永久改写受管文件

/var/lib/grafana
  -> Grafana 自己的运行数据
  -> 写入宿主机，容器重建后保留
```

管理员账号必须从未提交的真实 `.env` 提供。用户自助注册被关闭，匿名访问也没有在当前配置中开启。

端口映射的方向是：

```text
宿主机 127.0.0.1:43000
          |
          v
Grafana 容器 3000
```

所以默认只能从部署主机本身访问 `http://127.0.0.1:43000`。如果把 `NAMEWTA_BIND_HOST` 改成 `0.0.0.0`，当前 Compose 并不会自动增加 TLS，必须另行通过防火墙或反向代理保护。

### 第九步：数据源 provisioning 告诉 Grafana 去哪里查

`<Path>release-artifacts/docker/observability/grafana/provisioning/datasources/loki.yml</Path>` 内容是：

```yaml
apiVersion: 1

datasources:
  - name: Loki
    uid: loki
    type: loki
    access: proxy
    url: http://loki:3100
    isDefault: true
    editable: false
    jsonData:
      maxLines: 1000
```

逐项解释：

| 字段 | 作用 |
| --- | --- |
| `name: Loki` | 页面显示名称 |
| `uid: loki` | 仪表盘稳定引用的唯一标识 |
| `type: loki` | 告诉 Grafana 使用 Loki 查询协议 |
| `access: proxy` | 浏览器先访问 Grafana，由 Grafana 服务端访问 Loki |
| `url: http://loki:3100` | Docker 私有网络里的 Loki 地址 |
| `isDefault: true` | 新查询默认选 Loki |
| `editable: false` | 页面不能永久修改这份受管数据源 |
| `maxLines: 1000` | 单次日志查询展示行数上限配置 |

`access: proxy` 形成的实际调用是：

```text
[浏览器]
  只知道 127.0.0.1:43000
        |
        v
[Grafana]
  知道 Docker 内部地址 loki:3100
        |
        v
[Loki]
```

浏览器不需要解析 `loki` 服务名，Loki 也不需要发布宿主机端口。

### 第十步：dashboard provisioning 自动装入日志页面

`<Path>release-artifacts/docker/observability/grafana/provisioning/dashboards/namewta.yml</Path>` 声明：

```yaml
providers:
  - name: NAMEWTA
    orgId: 1
    folder: NAMEWTA
    type: file
    disableDeletion: true
    editable: false
    options:
      path: /var/lib/grafana/dashboards
```

意思是：

```text
Grafana 启动
   |
   v
扫描 /var/lib/grafana/dashboards
   |
   v
把 JSON 放入页面中的 NAMEWTA 文件夹
   |
   v
用户不能在页面中删除或永久改写这份受管仪表盘
```

`<Path>release-artifacts/docker/observability/grafana/dashboards/namewta-logs.json</Path>` 当前只定义一个 logs 面板。核心查询是：

```text
{job="ruoyi"}
```

它使用数据源 `uid=loki`，默认：

```text
查看最近 1 小时
每 30 秒刷新
新日志在前，倒序显示
不执行界面去重 dedupStrategy=none
显示时间和日志详情
不默认展开标签
```

`uid=loki` 把数据源文件和仪表盘 JSON 连接起来：

```text
[datasource uid=loki]
          ^
          |
          | dashboard 引用同一个 uid
          |
[NAMEWTA Logs panel]
```

如果数据源 UID 被改名但仪表盘没有同步改，Grafana 能启动，但面板会提示找不到数据源。

## 日志怎样被查询

### 标签查询先缩小范围

Alloy 提供的三个标签可以直接组合：

```text
全部 RuoYi 日志：
{job="ruoyi"}

只看两个主 Admin：
{job="ruoyi", service="ruoyi-admin"}

只看一号 Admin：
{job="ruoyi", service="ruoyi-admin", instance="server1"}

只看 SnailJob：
{job="ruoyi", service="ruoyi-snailjob-server"}
```

然后再按正文筛选：

```text
只包含 ERROR 的日志：
{job="ruoyi"} |= "ERROR"

只找某个 requestId：
{job="ruoyi", service="ruoyi-admin"} |= "目标 requestId"

排除健康检查文字：
{job="ruoyi"} != "/actuator/health"
```

标签筛选与正文筛选的顺序可以这样理解：

```text
先按标签选“哪几个文件来源”
            |
            v
再在这些日志正文里找关键词
```

当前 dashboard 没有服务、实例或级别下拉框，只有固定 `{job="ruoyi"}`。更细的查询需要在 Grafana Explore 中手工输入，或者以后给 dashboard 增加变量。

### JSON 日志当前只是正文

主 Admin 的 HTTP 系统日志可能长这样：

```json
{"event":"HTTP_RESPONSE","requestId":"abc","status":200,"costMs":38}
```

它进入 Loki 后仍是一行日志正文。当前 Alloy 没有把 `event`、`requestId`、`status` 变成持久标签。

这是一项刻意需要控制的边界：如果把每个 requestId 都变成索引标签，会产生数量极多的标签组合。更适合的方式是在查询时解析 JSON，或只把取值范围稳定且较小的字段做标签。

## 启动时到底发生什么

### 启动前准备

```text
1. 从 .env.example 准备未提交的 .env
2. 设置 GRAFANA_ADMIN_USER 和 GRAFANA_ADMIN_PASSWORD
3. 确认 NAMEWTA_DATA_ROOT 对 backend 和 observability 相同
4. 管理脚本检查或创建 external network
5. 先校验 Compose，再启动
```

对应命令：

```bash
cp release-artifacts/.env.example release-artifacts/.env

bash release-artifacts/scripts/docker-manage.sh config observability
bash release-artifacts/scripts/docker-manage.sh up observability
```

真实 `.env` 必须替换所有密码占位值，并保持不提交。管理脚本通过 `--env-file` 把同一份变量交给 Compose。

### 三个服务的启动时间线

```text
[docker-manage.sh]
        |
        +--> 确保 ruoyi-namewta-network 存在
        |
        v
[Compose 创建 Loki]
  读取 loki.yml
  挂载 /loki 数据目录
        |
        +----------------------+
        |                      |
        v                      v
[Compose 创建 Alloy]      [Compose 创建 Grafana]
  读取 config.alloy         读取 provisioning
  挂载日志目录              自动建立 Loki 数据源
  开始跟踪文件              自动导入 NAMEWTA Logs
        |                      |
        +----------+-----------+
                   v
              [等待新日志]
```

Alloy 与 Grafana 都声明 `depends_on: [loki]`，但没有健康检查门。启动初期如果看到连接失败，应先确认 Loki 是否已完成初始化；不能仅凭容器处于 running 就断定 3100 已可用。

后端启动后写出 `sys-console.log`，Alloy 匹配到文件并读取新增行，Grafana下一次 30 秒刷新时才可能在默认面板显示它。因此“刚启动页面为空”可能只是还没有匹配日志或时间范围不对。

## 重启、故障与数据保留

### 三个服务互相故障时会怎样

```text
Grafana 停止：
  Alloy -> Loki 仍可继续
  只是人暂时看不到页面

Alloy 停止：
  后端仍继续写宿主机文件
  Alloy 恢复后依赖持久状态和仍存在的文件继续处理

Loki 停止：
  Alloy 无法成功把日志写入 Loki
  原始文件仍是主要恢复来源
  若文件在恢复前已轮转删除，缺失可能无法补回

宿主机 Loki 数据目录丢失：
  单副本集中历史丢失
  Grafana 自己的数据目录不能恢复 Loki 原始日志
```

### 三个持久目录不能混为一谈

```text
NAMEWTA_DATA_ROOT/logs
  -> 原始应用日志

NAMEWTA_DATA_ROOT/observability/alloy
  -> Alloy 组件运行状态

NAMEWTA_DATA_ROOT/observability/loki
  -> 集中日志索引和数据块

NAMEWTA_DATA_ROOT/observability/grafana
  -> Grafana 自己的运行数据
```

删除任意一个目录的影响不同。尤其不能把 Grafana 数据目录当成 Loki 日志备份；Grafana 是查询者，不保存 Loki 的原始日志库。

当前应用本地历史和 Loki 都保留 60 天。上一份图解建议未来把本地文件改为较短缓冲、Loki 保持长期集中保留，但本次没有修改这些配置。

## 当前配置没有做什么

为了准确理解当前成熟度，需要明确这些缺口：

```text
采集方面：
  没有 Nginx、浏览器、Docker json-file 或基础设施日志
  没有多行 Java 异常合并
  没有 JSON 字段提取和动态标签
  SnailJob/SnailAI 可能多文件重复采集

Loki 方面：
  没有多副本和高可用
  没有应用层认证
  没有预置告警规则
  没有对象存储远端备份

Grafana 方面：
  只有一个固定日志面板
  没有服务、实例、级别下拉筛选
  没有告警面板
  没有嵌入 Admin 前端或 Nginx /grafana/ 路由

编排方面：
  没有 Loki、Alloy、Grafana healthcheck
  depends_on 只控制启动顺序，不保证服务就绪
```

这些缺口不表示当前链路不能工作，而是说明它目前是一套单机、文件采集、基础集中查询方案。

## 日志为空时怎样排查

必须按数据流逐段检查，不要一开始就只刷新 Grafana：

```text
第一站：应用有没有写文件？
  NAMEWTA_DATA_ROOT/logs/server1/sys-console.log 是否存在并增长
            |
            v
第二站：Alloy 能否看到同一文件？
  /var/log/ruoyi/server1/sys-console.log 是否匹配 *.log
            |
            v
第三站：Alloy 是否能连 Loki？
  Alloy 日志中是否有 loki:3100 写入错误
            |
            v
第四站：Loki 是否正常接收和保存？
  Loki 日志中是否有权限、目录、schema 或写入错误
            |
            v
第五站：Grafana 数据源是否健康？
  Loki datasource URL 是否仍是 http://loki:3100
            |
            v
第六站：查询条件是否覆盖目标日志？
  时间范围是否包含日志
  是否存在 job=ruoyi 标签
```

查看三个容器自身日志可使用：

```bash
bash release-artifacts/scripts/docker-manage.sh logs observability alloy
bash release-artifacts/scripts/docker-manage.sh logs observability loki
bash release-artifacts/scripts/docker-manage.sh logs observability grafana
```

典型故障与含义：

| 症状 | 优先怀疑 |
| --- | --- |
| 宿主机没有 `sys-console.log` | 后端 `LOG_PATH`、Logback 或后端目录挂载 |
| 宿主机有文件，Alloy无采集 | `NAMEWTA_DATA_ROOT` 不一致、只读挂载错误或 glob 不匹配 |
| Alloy报告连接拒绝 | Loki 尚未就绪、未在同一网络或服务名错误 |
| Loki报告 permission denied | `/loki` 宿主机目录权限不匹配 |
| Grafana数据源失败 | Grafana与Loki网络、服务名或 Loki 状态 |
| 数据源正常但面板为空 | 时间范围、`job=ruoyi` 标签、文件尚无新内容 |
| 同一 Snail 日志出现两次 | Alloy同时匹配 console 与 info/error 文件 |

## 安全边界

当前链路的保护关系是：

```text
[浏览器]
  -> Grafana 有管理员账号密码
  -> 默认只绑定 127.0.0.1

[Grafana / Alloy]
  -> Loki 没有账号密码
  -> 依赖 Docker 私有网络隔离

[Alloy]
  -> 对应用日志目录只有只读权限

[Loki]
  -> 以 0:0 写宿主机数据目录
```

需要注意：所有加入同一 external network 的容器理论上都能尝试访问无认证 Loki。若网络中未来加入不受信任的工作负载，应增加网络分区、Loki 网关认证或其他访问控制。

Alloy 不做脱敏，Grafana 也不是敏感数据清理器。日志安全必须从应用产生日志的地方开始控制。

## 术语小词典

- 日志写入器（Logback appender）：把 Java 日志事件写到控制台或文件的组件。
- 宿主机目录映射（bind mount）：让容器路径与宿主机路径指向同一批文件。
- 文件目标地址簿（`local.file_match`）：列出 Alloy 应该寻找哪些文件，并给它们附什么标签。
- 文件跟读器（`loki.source.file`）：持续读取目标文件新增内容的 Alloy 组件。
- 内部接收口（receiver）：Alloy 组件之间传递日志的连接点，不是宿主机端口。
- 日志写出器（`loki.write`）：把 Alloy 收到的日志发送到 Loki HTTP 接口。
- 日志标签（label）：用于先缩小检索范围的稳定字段，如 `service` 和 `instance`。
- 日志正文：文件中的实际文字；当前 JSON 字段也仍属于正文。
- 日志数据块（chunk）：Loki 保存日志内容的文件单元。
- 时间序列索引（TSDB index）：根据时间和标签定位日志数据块的索引结构。
- 整理清理器（compactor）：整理索引并执行过期日志删除的 Loki 组件。
- 自动装配（provisioning）：Grafana 启动时按照文件自动创建数据源和仪表盘。
- 服务端代理查询（`access: proxy`）：浏览器只访问 Grafana，由 Grafana 再访问 Loki。
- LogQL：Loki 的查询语言，`{job="ruoyi"}` 是一个标签选择器。
- 读取进度：采集器记录文件读到哪里，用于重启后继续；它不是原始日志备份。

## 你现在能复述什么

1. 后端和 Loki 没有代码级依赖；它们通过相同 `NAMEWTA_DATA_ROOT` 下的宿主机日志文件间接连接。
2. Alloy 把宿主机日志根只读挂载为 `/var/log/ruoyi`，用 `local.file_match` 找文件并附上 `job/service/instance`，再由 `loki.source.file` 转给 `loki.write`。
3. `loki.write` 通过共享 Docker 网络访问 `http://loki:3100/loki/api/v1/push`；Loki 的 3100 没有公开到宿主机。
4. Loki 使用本地 filesystem、TSDB v13、单副本和 60 天保留，把数据持久化到 `NAMEWTA_DATA_ROOT/observability/loki`。
5. Grafana 通过 provisioning 自动创建 `uid=loki` 数据源和 `NAMEWTA Logs` 仪表盘，服务端查询内部 Loki，浏览器只访问 `127.0.0.1:43000`。
6. 当前方案没有 JSON 预解析、多行堆栈合并、告警或高可用；出现空日志时应按“文件、Alloy、Loki、Grafana、查询条件”的顺序排查。
