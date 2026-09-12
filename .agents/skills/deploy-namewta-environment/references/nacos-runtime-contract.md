# Nacos 运行契约

Nacos 是可选能力。本地 Spring YAML 保持完整基线，Nacos 只提供经过校验的稀疏覆盖。当前契约使用分组 `DEFAULT_GROUP`、Data ID `wta-namewta.yml`，命名空间与活动 profile 对应。

部署环境变量优先于远程配置。验证码、通知幂等和 OSS 下载 TTL 等允许键可即时生效；其他允许键可能需要重启。`nacos.config.*`、`spring.profiles.*`、未知键和非法候选必须被原子拒绝。

运行中断连时保留最后一次有效内存配置。Nacos 不可用时重启应用，应从本地基线启动并等待恢复，不能依赖未定义的磁盘快照。

当前开发服务器不得发布宿主机 `8848/9848`，Nacos 保持在 NAMEWTA Docker 网络内，并通过 `http://<host>:40080/nacos/` 代理。不得修改同机 CDE Nacos。

启动前初始化 Nacos Schema 和账号。至少保留一个启用的管理员账号，并为应用配置独立的只读账号；管理员凭据不得注入应用。报告记录命名空间、分组、Data ID 与配置摘要；配置正文含敏感信息时不得写入报告。

只变更 Nacos 时使用 `docker compose ... up -d --no-deps nacos`，不得连带重建 MySQL。回退时先删除或清空稀疏覆盖，再关闭两个实例的 `NACOS_CONFIG_ENABLED` 并逐一重启。保留 Nacos 数据库与数据目录用于审计。
