# Nacos 当前配置切换合同

当前客户端默认 Data ID 为 `wta-namewta.yml`，以 `backend/wta-common/wta-common-nacos/src/main/java/org/namewta/common/nacos/NacosConfigConstants.java` 和运行配置为准；不双读旧 `ruoyi-namewta.yml`。Docker 产品服务保持 `namewta-*`，容器工作目录位于 `/wta/...`。

当前启用、稀疏覆盖、断连/重启与恢复规则统一维护在[发布说明的 Nacos 章节](../release-artifacts/README.md#可选-nacos-配置中心)。上述名称是源码与配置合同，不能据此声称生产已部署。

全新数据库从 `50-cde-base-dml.sql` 初始化登录用户 `WTA`。禁止增加 `mysql/migrate/` 文件；已有数据库按源/目标 Git Tag 差异形成经评审的升级方案，不重放六文件基座。历史改名记录从 Git/Speculo 归档查询，不把失效的 active change 路径作为当前权威。
