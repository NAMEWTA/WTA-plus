# 服务器目录与权限

建议在 `/data/namewta-data` 下使用以下独占布局：

```text
/data/namewta-data/
  compose/                     所有 Compose、override、env 与运行说明
    docker-compose-*.yml       0640
    overrides/*.yml            0640
    namewta-release.env        0600
  releases/                    不可变发布目录
  release-current -> releases/<release-id>
  data/mysql/                  持久化
  data/redis/                  持久化
  data/minio/                  持久化
  data/minio-config/           持久化
  data/nacos/                  持久化
  data/nacos-logs/              持久化
  data/{loki,grafana,prometheus}/ 启用时持久化
  backups/<timestamp>/         配置、发布与数据备份
  staging/                     未完成传输
  deployment/namewta-deployment.md  唯一最新部署报告（0600）
  log/<yyyy-mm-dd>/            检测日志、命令输出与截图
```

`compose/` 是唯一的编排文件目录。基础文件、环境差异 override 和对应的
`--env-file` 都必须放在这里；禁止把 Compose 文件散落在根目录、`data/` 或
`releases/`。所有中间件数据只放在 `data/` 下：

```text
/data/namewta-data/data/{mysql,redis,minio,nacos,loki,grafana,prometheus}/
```

`releases/` 仅保存不可变应用发布构件，`backups/` 仅保存备份，二者都不承担
运行时数据。部署完成后只保留 `deployment/namewta-deployment.md` 作为唯一最新报告，并在 `log/<yyyy-mm-dd>/` 保存检测证据；报告含敏感信息，权限为 `0600`。

目录由部署用户或已记录的容器 UID/GID 持有。敏感文件为 `0600`，普通配置为 `0640` 或更严格，目录为 `0750` 或更严格；容器确有要求时才记录例外。

每次写操作前解析并验证服务器根目录的规范路径。拒绝空值、`/`、`/root`、`/home`、用户主目录展开、未解析的 `$VARIABLE` 或逃逸出授权根目录的路径。禁止对根目录递归修改所有者，只处理明确需要的单个服务目录。

备份必须包含校验值和恢复命令。配置备份含有敏感信息，必须加密或限制访问。生产环境的保留周期和异机备份决策必须写入报告。
