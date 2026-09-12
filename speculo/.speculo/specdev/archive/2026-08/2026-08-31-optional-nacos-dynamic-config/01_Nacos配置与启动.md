# Nacos 配置与启动：从空环境到动态修改配置

可以把 Nacos 想成一块放在服务器上的“共享配置白板”。两台 NAMEWTA 后端都看着同一块白板；运维人员修改白板后，它们各自读取、检查，再决定立即采用还是等下次重启采用。

先记住三个结论：

1. Nacos 是可选项。不开启、服务掉线或里面没有配置时，应用仍使用自己完整的本地 YAML。
2. Nacos 中只放需要覆盖的少量配置，不要复制整份本地 YAML。
3. 不是所有配置都能立即生效。验证码、通知幂等窗口、OSS 下载时效可以立即生效；其他合法配置需要重启应用。

> 此前在用户指定服务器上运行的是隔离验收环境，验证完成后测试容器、网络和目录已经清理。测试证明这套方案可运行，但不代表服务器上已经常驻部署了本项目的 Nacos。正式使用仍需执行下面的部署步骤。

## 先看全图

```text
                    同一个可信 Docker 网络

  [运维人员]
       |
       | 登录官方控制台，发布少量 YAML
       v
  [Nginx 的 /nacos/] ----------------------+
       |                                   |
       v                                   |
  [官方 Nacos 2.5.4]                       |
       |                                   |
       | 保存配置和修改历史                 |
       v                                   |
  [MySQL 中独立的 nacos 数据库]             |
                                           |
       +----------------+------------------+
       |                |
       v                v
  [后端实例 1]      [后端实例 2]
       |                |
       | 检查完整候选配置 |
       +--------+-------+
                |
        +-------+-------------------+
        |                           |
        v                           v
  [允许立即更新的配置]        [需要重启的配置]
  验证码 / 通知窗口 /         其余合法 YAML
  OSS 下载时效
```

本地 YAML 始终是地基，Nacos 只是上面的一层稀疏覆盖：

```text
低优先级                                                高优先级

[本地 application*.yml] -> [Nacos 稀疏 YAML] -> [环境变量 / JVM 参数 / 命令行]

示例：
本地 captcha.enable=true
Nacos captcha.enable=false
环境变量 CAPTCHA_ENABLE=true
最终值 = true
```

应用选择哪一份远程配置，由三个坐标共同决定：

```text
[当前 profile] -> [对应 namespace] + [DEFAULT_GROUP] + [ruoyi-namewta.yml]

local -> local namespace --+
dev   -> dev namespace   ---+--> DEFAULT_GROUP / ruoyi-namewta.yml
prod  -> prod namespace  ---+
```

## 一步一步看

### 1. 先选择正式部署方式

发布事实源位于 `<Path>release-artifacts/</Path>`。标准方案会启动以下组件：

```text
[MySQL 8.4.9]
   +-- ry-namewta 数据库：业务数据
   +-- nacos 数据库：Nacos 配置与历史

[Nacos 2.5.4]
   +-- 只通过独立 nacos 数据库账号访问 nacos 数据库

[Redis 8.6.3] + [MinIO]

[ruoyi-server1] + [ruoyi-server2]
   +-- 两台都直接连接 Nacos

[Nginx LB]
   +-- 管理系统
   +-- /nacos/ 官方控制台代理
```

有两种 MySQL 情况：

| 情况 | 应该怎样做 |
| --- | --- |
| 第一次启动，MySQL 数据目录为空 | 启用 `nacos` profile 后，MySQL entrypoint 自动创建独立 Nacos 数据库、10 张表和最小权限数据库账号。 |
| MySQL 已经在运行，数据目录非空 | entrypoint 不会重跑，必须先执行 `<Path>release-artifacts/scripts/init-nacos-mysql-container.sh</Path>`。该脚本可重复执行，不会修改其他数据库。 |

标准 Compose 假定 MySQL、Nacos、双后端和 Nginx 都加入同一个 `ruoyi-namewta-network`，并使用网络内服务名 `mysql`、`nacos` 互相访问。若要复用一个任意名称、任意网络的旧 MySQL，不能直接照搬标准命令；必须先给它加入同一网络并提供 `mysql` 网络别名，或准备受审查的 Compose override。

### 2. 创建运行配置文件

变量模板位于 `<Path>release-artifacts/.env.example</Path>`，真实值写入被 Git 忽略的 `<Path>release-artifacts/.env</Path>`。在项目根目录执行：

```bash
RELEASE_ROOT=release-artifacts
cp "${RELEASE_ROOT}/.env.example" "${RELEASE_ROOT}/.env"
chmod 600 "${RELEASE_ROOT}/.env"
```

至少替换以下 Nacos 变量，不能保留 `replace-*` 占位值：

| 变量 | 它是什么 | 推荐填写方式 |
| --- | --- | --- |
| `NAMEWTA_DATA_ROOT` | 中间件数据根目录 | 服务器使用 `/data/namewta-data`，其下会创建 MySQL、Nacos、Redis 等子目录。 |
| `NAMEWTA_BIND_HOST` | 宿主机监听地址 | 保持 `127.0.0.1`；需要跨机访问时优先通过可信内网或 TLS 代理。 |
| `SPRING_PROFILES_ACTIVE` | 当前环境 | 生产使用 `prod`。 |
| `NACOS_DB_NAME` | Nacos 独立数据库 | 固定 `nacos`。 |
| `NACOS_DB_USER` | Nacos 数据库账号 | 固定或使用受控账号名 `nacos`。 |
| `NACOS_DB_PASSWORD` | Nacos 数据库密码 | 使用至少 24 字节随机值；当前脚本只接受字母、数字、点、下划线、波浪号和连字符。 |
| `NACOS_AUTH_TOKEN` | Nacos 签发登录令牌的服务端密钥 | 使用 Base64 编码、原始长度至少 32 字节的随机值。 |
| `NACOS_AUTH_IDENTITY_KEY` | Nacos 节点身份键 | 使用随机且不公开的字符串。 |
| `NACOS_AUTH_IDENTITY_VALUE` | Nacos 节点身份值 | 使用另一份独立随机值。 |
| `NACOS_CONFIG_USERNAME` | 后端读取配置的 Nacos 用户 | 首次联调可用 `nacos`；生产建议改为只读应用账号。 |
| `NACOS_CONFIG_PASSWORD` | 上述 Nacos 用户的密码 | 在管理员初始化或应用账号创建后填写。 |
| `NACOS_CONFIG_NAMESPACE_PROD` | prod 配置所在 namespace ID | 可以使用 `prod`，也可以使用控制台生成的 ID，但两边必须完全一致。 |

可以用 OpenSSL 生成材料；每一行都应单独执行并立即写入受保护的 `.env`：

```bash
# 适合 NACOS_DB_PASSWORD、identity key/value
openssl rand -hex 24

# 适合 NACOS_AUTH_TOKEN
openssl rand -base64 48 | tr -d '\n'

# 适合 Nacos 管理员或只读账号密码，字符集合与真实 E2E 一致
printf 'A%s_a1\n' "$(openssl rand -hex 16)"
```

`NACOS_AUTH_TOKEN`、identity、数据库密码和控制台密码必须彼此不同。不要把真实值写进 Git、聊天记录、构建日志或命令文档。

### 3. 准备数据库

先把六份业务 SQL 和固定的 Nacos 2.5.4 schema 放到发布 staging 目录：

```bash
RELEASE_ROOT=release-artifacts
bash "${RELEASE_ROOT}/scripts/release-manage.sh" stage-mysql
```

如果 MySQL 是全新的，下一步启动 infrastructure 时会自动导入 Nacos schema。

如果标准 Compose 的 MySQL 已经运行过，先执行：

```bash
RELEASE_ROOT=release-artifacts
bash "${RELEASE_ROOT}/scripts/init-nacos-mysql-container.sh" \
  --env-file "${RELEASE_ROOT}/.env" \
  --container ruoyi-namewta-mysql
```

如果实际容器名不同，只替换 `--container` 的值。脚本成功时应报告 `database=nacos, tables=10`。它会把 Nacos 数据库账号限制为该数据库上的 `SELECT`、`INSERT`、`UPDATE`、`DELETE`，不授予建表、删库或访问业务库的权限。

### 4. 只启动基础设施和 Nacos

先校验 Compose，再启动带 `nacos` profile 的基础设施：

```bash
RELEASE_ROOT=release-artifacts
bash "${RELEASE_ROOT}/scripts/docker-manage.sh" config infrastructure --profile nacos
bash "${RELEASE_ROOT}/scripts/docker-manage.sh" up infrastructure --profile nacos
```

启动关系是：

```text
[MySQL healthy]
      |
      v
[Nacos 启动并连接 nacos 数据库]
      |
      v
[readiness HTTP 200]
```

检查状态：

```bash
docker inspect --format '{{.State.Health.Status}}' ruoyi-namewta-nacos
curl --fail http://127.0.0.1:8848/nacos/v1/console/health/readiness
```

预期第一条输出 `healthy`，第二条返回 HTTP 200。8848 和 9848 默认只绑定到 `127.0.0.1`，这是有意的安全边界。

### 5. 第一次设置管理员密码

Nacos 2.5.4 第一次启动后，用一次性初始化接口设置管理员密码。不要把密码直接写进脚本：

```bash
read -rsp 'Nacos admin password: ' NACOS_ADMIN_PASSWORD
printf '\n'
curl --fail --request POST \
  --data-urlencode "password=${NACOS_ADMIN_PASSWORD}" \
  http://127.0.0.1:8848/nacos/v1/auth/users/admin
unset NACOS_ADMIN_PASSWORD
```

该接口只用于首次初始化。完成后登录地址为：

```text
服务器本机直接访问： http://127.0.0.1:8848/nacos/
统一入口已经启动： http(s)://<统一入口>/nacos/
管理系统菜单入口： 系统管理 -> 配置中心
```

如果端口仍只绑定回环地址，可在自己的电脑建立 SSH 端口转发，再打开 `http://127.0.0.1:8848/nacos/`。不要为了方便把 8848 暴露到公网。

### 6. 创建 namespace 和读取账号

用管理员登录官方 Nacos 控制台后：

1. 进入“命名空间”，创建 `prod`。
2. 记下真正的 namespace ID。
3. 把这个 ID 写入 `.env` 的 `NACOS_CONFIG_NAMESPACE_PROD`。
4. 在“权限控制”中创建后端专用账号，授予该 namespace 下目标配置的读取权限。
5. 把专用账号和密码写入 `NACOS_CONFIG_USERNAME`、`NACOS_CONFIG_PASSWORD`。

最容易联调的方式是让后端暂时使用管理员 `nacos`，模板也是这样写的；正式环境不建议长期这样做。控制台管理员负责发布和删除配置，后端应用账号只负责读取，两种权限应分开。

### 7. 在 Nacos 中创建第一份配置

在官方控制台进入“配置管理 -> 配置列表”，切到 prod namespace，然后新建：

| 字段 | 固定值 |
| --- | --- |
| Data ID | `ruoyi-namewta.yml` |
| Group | `DEFAULT_GROUP` |
| 配置格式 | `YAML` |

第一次只发布一个很小、很容易验证的覆盖：

```yaml
captcha:
  enable: false
```

不要把本地 `application.yml`、`application-prod.yml` 整份复制进去。Nacos 中缺失的键会继续使用本地值。

下面是一份覆盖三类即时配置的完整示例：

```yaml
captcha:
  enable: true
  type: char
  charLength: 5

notify:
  idempotency:
    default-window: 3m
    min-window: 1m
    max-window: 5m

oss:
  lifecycle:
    download-ttl: 3m
```

这些值有边界：验证码长度必须为正整数；通知窗口必须满足 `min <= default <= max`；OSS `download-ttl` 必须落在本地设置的最小值与最大值之间。任何一个值非法，整份候选配置都会被拒绝，应用继续使用上一份有效配置。

以下内容禁止放入远程 YAML：

```yaml
nacos:
  config:
    enabled: false

spring:
  profiles:
    active: dev
```

它们会改变“去哪里取配置”或“当前是什么环境”，因此被保护。包含这些键的整份配置都会收到 `PROTECTED_KEY` 拒绝结果。

### 8. 启动两台后端实例

先构建和投放应用镜像；具体构建入口位于 `<Path>release-artifacts/scripts/release-manage.sh</Path>`：

```bash
RELEASE_ROOT=release-artifacts
bash "${RELEASE_ROOT}/scripts/release-manage.sh" build \
  --target all --env prod --env-file "${RELEASE_ROOT}/.env"
bash "${RELEASE_ROOT}/scripts/release-manage.sh" stage --env prod
bash "${RELEASE_ROOT}/scripts/docker-manage.sh" build-images
```

启动后端时必须同时组合基础后端 Compose 和 Nacos enabled override：

```bash
RELEASE_ROOT=release-artifacts
docker compose \
  --env-file "${RELEASE_ROOT}/.env" \
  -p namewta-backend \
  -f "${RELEASE_ROOT}/docker/docker-compose-backend.yml" \
  -f "${RELEASE_ROOT}/docker/overrides/nacos-enabled.yml" \
  up -d ruoyi-server1 ruoyi-server2
```

override 会为两台后端注入：

```text
NACOS_CONFIG_ENABLED=true
NACOS_CONFIG_SERVER_ADDR=nacos:8848
NACOS_CONFIG_USERNAME=<读取账号>
NACOS_CONFIG_PASSWORD=<读取密码>
NACOS_CONFIG_NAMESPACE_PROD=<prod namespace ID>
```

并要求 Nacos healthy 后才启动应用。不要只执行普通的 `docker-manage.sh up backend` 后就认为 Nacos 已开启；基础后端 Compose 故意不包含这些启用变量。

### 9. 启动前端入口并授权菜单

启动前端 LB：

```bash
RELEASE_ROOT=release-artifacts
bash "${RELEASE_ROOT}/scripts/docker-manage.sh" up frontend
```

生产前端把 Nacos 地址固定为根相对 `/nacos/`。浏览器看到的是同一个站点：

```text
[管理系统页面]
      |
      | 点击 系统管理 -> 配置中心
      v
[同源 iframe: /nacos/]
      |
      v
[Nginx 转发到 nacos:8848/nacos/]
      |
      v
[Nacos 自己的登录页面]
```

NAMEWTA 登录不会自动登录 Nacos，也不会向 iframe 注入密码。要看见菜单，管理员还需在角色管理中把“配置中心”菜单授权给目标角色；其 RuoYi 权限字符是 `system:nacos:console`。配置发布权限仍由 Nacos 自己的账号和权限控制。

### 10. 修改配置后会发生什么

```text
[控制台点击发布]
        |
        v
[Nacos 通知实例 1 和实例 2]
        |
        v
[每台实例分别解析整份 YAML]
        |
        +-- 语法、保护键、类型或组合非法
        |       |
        |       v
        |   [整份拒绝，继续用上一有效版本]
        |
        +-- 全部合法
                |
                +-- 验证码 / 通知窗口 / OSS 下载时效 -> 下一次业务调用立即读取新快照
                |
                +-- 其他键 -> 标记为需要重启；重启后从 Nacos 读取
```

每次业务调用只读取一份不可变快照，不会出现“验证码开关是新版、长度却还是旧版”的半新半旧状态。

如果删除 `ruoyi-namewta.yml`，两台实例会回到本地 YAML。运行中 Nacos 断线时，实例保留最后一次已经验证的内存配置；如果应用也在 Nacos 离线期间重启，它不会读取磁盘快照，而是从本地 YAML 启动并等待 Nacos 恢复。

### 11. 怎样确认两台实例都成功了

Nacos 开启后，受监控账号保护的 `/actuator/info` 会增加 `nacosConfig`，但不会返回 YAML 正文或 secret：

```bash
curl --fail --user '<monitor-user>:<monitor-password>' \
  http://127.0.0.1:48080/actuator/info

curl --fail --user '<monitor-user>:<monitor-password>' \
  http://127.0.0.1:48081/actuator/info
```

重点比较这些字段：

| 字段 | 怎样判断 |
| --- | --- |
| `enabled` | 两台都应为 `true`。 |
| `connected` | 正常时两台都应为 `true`。 |
| `digest` | 两台应相同，代表收到同一份正文。 |
| `result` | 正常应为 `APPLIED`；非法候选会显示 `REJECTED`。 |
| `immediateKeyCount` | 本次配置中可立即生效的叶子键数量。 |
| `restartKeyCount` | 本次配置中需要重启的叶子键数量。 |
| `errorCode` | 正常为空；失败可能是 `INVALID_YAML`、`PROTECTED_KEY`、`KNOWN_TYPE_INVALID`、`PARTICIPANT_REJECTED` 或 `FETCH_FAILED`。 |

只有“两台 digest 相同”才能说明双实例已经收敛。控制台显示发布成功，只代表 Nacos 保存成功，不代表每一台应用都已经接受。

### 12. 怎样安全回退

回退顺序不能反过来：

```text
[先删除远程配置]
        |
        v
[确认两台应用回到本地基线]
        |
        v
[不用 Nacos override 重建两台后端]
        |
        v
[确认业务正常]
        |
        v
[最后才停止 Nacos]
```

删除配置并确认本地基线后，用基础后端 Compose 重建两台应用，使 `NACOS_CONFIG_ENABLED` 恢复默认 `false`：

```bash
RELEASE_ROOT=release-artifacts
docker compose \
  --env-file "${RELEASE_ROOT}/.env" \
  -p namewta-backend \
  -f "${RELEASE_ROOT}/docker/docker-compose-backend.yml" \
  up -d --force-recreate ruoyi-server1 ruoyi-server2
```

最后如需停止 Nacos，只停止这个服务，不要停掉整组基础设施：

```bash
RELEASE_ROOT=release-artifacts
docker compose \
  --env-file "${RELEASE_ROOT}/.env" \
  -p ruoyi-namewta-infrastructure \
  -f "${RELEASE_ROOT}/docker/docker-compose-infrastructure.yml" \
  --profile nacos stop nacos
```

不要执行 `docker compose down -v`。Nacos 数据库和 `NAMEWTA_DATA_ROOT/nacos` 下的实际 Nacos 数据目录应先备份并保留，以便审计或重新启用。

### 13. 先做隔离验收，再正式启用

仓库提供了完整的隔离验收脚本 `<Path>release-artifacts/scripts/verify-nacos.sh</Path>`。它会创建自己的 MySQL、Redis、Nacos 和两台应用，验证后按精确名称清理，不复用正式数据：

```bash
./mvnw -f ruoyi-vue-plus-namewta/pom.xml \
  -Pbundle-full -DskipTests package

NACOS_E2E_CONFIRM=1 \
  bash release-artifacts/scripts/verify-nacos.sh
```

这条命令会验证默认关闭、双实例收敛、配置优先级、即时与重启分类、非法配置拒绝、单实例故障、Nacos 断连、离线重启、本地回退、恢复和 MySQL 持久化。它是验收工具，不是正式部署命令。

## 常见情况图解

### 情况一：Nacos 没有启动

```text
NACOS_CONFIG_ENABLED=false
        |
        v
不创建客户端、不监听 Nacos
        |
        v
应用只用本地 YAML，正常启动
```

### 情况二：开关打开，但 Nacos 暂时不可达

```text
应用启动 -> 连接失败 -> 本地 YAML 启动 -> 后台等待恢复

应用运行中 -> Nacos 断线 -> 保留上一份有效内存配置
```

### 情况三：发布了一份错误 YAML

```text
错误候选 -> 完整解析与校验 -> REJECTED
                                |
                                v
                         上一有效配置不变
```

### 情况四：改了普通 Spring 配置却没有立刻变化

```text
合法普通键 -> Nacos 已保存 -> restartKeyCount 增加
                                  |
                                  v
                          重启应用后才使用
```

这不是故障，而是刻意限制。任意 Bean、连接池或框架对象都自动重建会产生不可预测的中间状态，所以只有三类经过专门适配和测试的配置允许即时更新。

## 术语小词典

| 日常说法 | 专业名字 | 本项目中的意思 |
| --- | --- | --- |
| 共享配置白板 | Nacos Config | 集中保存少量远程 YAML，并通知应用配置发生变化。 |
| 本地地基 | Local baseline | 应用 jar 自带的 `application.yml` 和 profile YAML；Nacos 不可用时仍能启动。 |
| 少量盖章修改 | Sparse overlay | Nacos 只写需要覆盖的叶子键，缺失键继续使用本地值。 |
| 环境隔间 | Namespace | 把 local、dev、prod 配置彼此隔开。应用实际使用 namespace ID。 |
| 文件名 | Data ID | 固定为 `ruoyi-namewta.yml`。 |
| 文件夹 | Group | 固定为 `DEFAULT_GROUP`。 |
| 立即换新便签 | Live refresh | 下一次业务调用直接读取新的不可变配置快照。 |
| 下次开机再采用 | Restart required | Nacos 已保存，但已创建的 Spring 对象不会自动重建，应用重启后才生效。 |
| 整份检查后一次换入 | Atomic apply | 所有键都合法才替换旧配置；任一键失败就整份拒绝。 |
| 内容指纹 | Digest | YAML 正文的 SHA-256 摘要；两台实例摘要相同才说明配置一致。 |
| 存活检查 | Health/readiness | Docker 判断 MySQL、Nacos、应用是否已经可以接收请求的探针。 |
| 只允许必要动作 | Least privilege | Nacos 数据库账号只有本库 DML 权限；应用读取账号不使用控制台管理员权限。 |

## 可以直接照着核对的清单

- [ ] `<Path>release-artifacts/.env</Path>` 权限是 `0600`，所有 `replace-*` 已替换。
- [ ] `NAMEWTA_BIND_HOST=127.0.0.1`，没有把 8848/9848 直接暴露到公网。
- [ ] Nacos 使用固定官方 `nacos/nacos-server:v2.5.4`，没有改成 `latest`。
- [ ] Nacos 独立数据库有精确 10 张表，数据库账号无业务库和 DDL 权限。
- [ ] 管理员首次密码已经设置；后端尽量使用独立只读 Nacos 账号。
- [ ] prod namespace ID 与 `NACOS_CONFIG_NAMESPACE_PROD` 完全一致。
- [ ] Data ID 是 `ruoyi-namewta.yml`，Group 是 `DEFAULT_GROUP`，格式是 YAML。
- [ ] 远程 YAML 只包含需要覆盖的少量键，不含 `nacos.config.*` 或 `spring.profiles.*`。
- [ ] 启动后端时实际组合了 `<Path>release-artifacts/docker/overrides/nacos-enabled.yml</Path>`。
- [ ] 两台 `/actuator/info` 都显示 `enabled=true`、`connected=true`，并且 digest 相同。
- [ ] 目标 RuoYi 角色获得 `system:nacos:console` 菜单权限；Nacos 仍单独登录。
- [ ] 已先运行隔离 E2E，再发布正式配置。
- [ ] 已写好“删除远程配置 -> 关闭客户端 -> 最后停止 Nacos”的回退步骤，且不使用 `down -v`。

## 你现在能复述什么

1. **它是什么：** Nacos 是本地 YAML 上方的一层可选稀疏覆盖，不是应用启动必须依赖的配置文件仓库。
2. **为什么需要它：** 它让两台后端从同一个配置单元读取运维调整，同时保留鉴权、环境隔离、完整校验、逐实例观测和本地回退。
3. **它怎样流动：** 运维人员在 prod namespace 的 `DEFAULT_GROUP / ruoyi-namewta.yml` 发布少量 YAML；两台实例各自读取并原子校验，三类适配项立即换入不可变快照，其余合法项等待重启，非法项整份拒绝。
