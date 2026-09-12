# 既有环境接管

## 只读盘点

记录时间并收集：

1. 主机身份、系统、架构、CPU、内存、磁盘、时区、Docker 与 Compose 版本。
2. 服务器根目录三层以内的文件元数据，不读取敏感文件内容。
3. Compose 项目、容器名称/状态/镜像/标签、网络、挂载与发布端口。
4. TCP 监听端口及其所有者。
5. MySQL 库、表数量、迁移版本和 OSS 配置不变量。
6. Redis 连接与选用的数据库，不遍历无关业务键。
7. MinIO 健康、桶、匿名策略和私有探针。
8. Nacos 命名空间、分组、Data ID、启用用户与配置摘要，不输出配置正文。
9. 前端、API、监控、SnailJob、SnailAI、Nacos 的入口状态。
10. 当前发布清单、镜像摘要、备份与回滚命令。

Compose 身份必须形成一个不可拆分快照：project、按命令实际顺序排列的全部 `-f` 文件、`--env-file`、service 名、容器 Compose labels、当前镜像标签/image ID 和 bind host。目录名、容器名前缀或仓库样例都不能单独证明 project 身份。

优先保存机器可读盘点结果。敏感结果只能放在被忽略的 `temp/relase/`，权限为 `0600`。

## 差异分类

- `expected`：与配置档案及当前发布契约一致。
- `safe-reconcile`：元数据或幂等配置缺失，不替换持久化数据。
- `requires-backup`：涉及 Compose、配置、镜像或 Schema 变更。
- `ownership-conflict`：发现无关服务、目录、网络、端口或数据库，立即停止。
- `unknown`：证据不足，必须停止而不是推测。

## 接管约束

- 不得用仓库通用模板覆盖现场根 Compose。
- 新版本验证通过前，不得重命名历史发布目录或修改 `release-current`。
- 不得停止或重建无关容器，不得删除卷。
- 写操作前备份 `.env`、发布 env、Compose、清单、数据库和服务元数据。
- 敏感文件保持 `0600`。使用非执行式解析器读取单个键，不能 `source` 整份 env；`JAVA_OPTS` 等值可能含空格。
- 只重建 Nacos 时使用 `docker compose ... up -d --no-deps nacos`。
- 写入前用精确快照运行 `docker compose config --quiet`，并确认渲染后的服务、镜像和环境存在性与 profile 一致。不得从 shell 中同名临时变量推断最终 env 优先级。

上传到 `<server-root>/releases/<release-id>.staging`，校验清单后重命名为不可变发布目录，再原子更新活动指针。报告中保留旧指针和精确回滚命令。现场使用其他历史目录布局时，应保留该布局并记录映射。
