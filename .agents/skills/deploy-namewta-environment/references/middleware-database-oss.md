# 中间件、数据库与 OSS

## 启动顺序

1. 外部 Docker 网络。
2. MySQL、Redis、MinIO。
3. Schema 与账号准备完成后的可选 Nacos。
4. 可观测服务。
5. 后端双实例及可选 SnailJob/SnailAI。
6. 前端应用 Nginx 与统一入口。

依赖服务通过健康检查后才能启动调用方。

## MySQL

新库通过 `release-artifacts/scripts/init-mysql-container.sh` 只读消费 `release-artifacts/docker/infrastructure/mysql/init/` 中由 Git 跟踪的六份完整基座，统一初始化业务库 `wta-plus`：`10-cde-base-ddl`、job、workflow、ai、`50-cde-base-dml`；`60-cde-nacos.sql` 初始化独立库 `nacos`。保护脚本必须拒绝已有数据库或账号；失败时只能清理本次创建的资源。`stage-mysql` 只校验这些资产，不复制、不生成、不删除文件。

接管或升级已有库时，必须记录源 Git Tag 和目标 Git Tag，先做一致性备份，再依据两 Tag 的六文件差异形成、评审并在隔离副本演练升级 SQL；不得对已填充数据库重放初始化基座。差异 SQL 与执行记录只写入被忽略的 `temp/relase/` 私密报告目录。

数据库写入前先验证执行器的事务能力。工具拒绝 `START TRANSACTION`、多语句或自动提交控制时，必须在第一条持久写入前停止，记录差异并改用可验证的固定 session 事务方式；失败路径通过回滚或关闭 session 终止。不得因工具语法差异改为逐语句自动提交。

备份是默认硬门。仅目标为开发库且用户对精确变更明确授权时，可登记一次性 waiver；必须同时证明目标数据状态、表/主键/固定对象身份、冲突为零和 `forward-only` 恢复。生产环境、未知环境或范围不清时不允许 waiver。

## Redis

使用已记录的独立数据库和强密码。验证带认证的 `PING` 以及就绪所需的少量应用键。不得清空共享 Redis。

## MinIO 与 OSS

默认 OSS 配置不变量：

```text
config_key=minio
status=Y
access_policy=0（PRIVATE）
启用的默认配置恰好一个
```

当前代码中 `access_policy=2` 表示 `PUBLIC_READ`，不得用于默认私有 MinIO。公共资源应使用独立且明确公开的配置/桶；涉及权限的内容使用私有存储和短时签名预览或下载链接。

配置 `MINIO_ENDPOINT`、`MINIO_BUCKET` 和 `MINIO_DIAGNOSTIC_OBJECT`。在 `.well-known/oss-readiness/private-canary.txt`（或配置路径）上传小型私有探针，并验证：认证读取成功、匿名读取拒绝、应用就绪通过、签名链接在 TTL 内成功、过期后失败。

桶 CORS 只允许必要的开发/生产来源和方法。生命周期清理属于独立评审动作，应从 dry-run 开始，通用初始化不得自动开启破坏性清理。

只有确认数据库为全新时才运行：

```bash
bash release-artifacts/scripts/release-manage.sh stage-mysql
bash release-artifacts/scripts/init-mysql-container.sh \
  --container namewta-data-mysql \
  --env-file release-artifacts/.env
```

已有现场应在备份后事务性修复具体 OSS 行，不得调用初始化流程。
