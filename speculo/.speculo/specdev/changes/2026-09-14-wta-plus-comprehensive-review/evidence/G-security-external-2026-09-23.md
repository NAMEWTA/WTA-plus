# G-security-external — 现场凭据处置

用户2026-09-23指明 `/srv/ops/minio-main`、`/srv/ops/mysql-main`、`/srv/ops/wta-plus`，明确要求依据部署替换调整；Lead按该授权实施，子代理ops_audit只读盘点。没有把源代码计划授权推定为环境授权。

## 已完成范围

历史Git的两枚本地DB/Redis密码与现场一致且能认证。Lead备份两库一致性SQL、应用用户权限、Redis RDB及相关配置并验证；轮换namewta_app、WTA专属Redis和通过已泄漏DB可读取的WTA MinIO应用身份。MySQL双密码迁移后移除旧密码；Redis AOF保持；MinIO权限语义保持，只更新双库4个精确OSS主键的secret。私有env、当前ignored local和睡眠的init消费者已同步；只重建WTA所属服务，未改变共享MySQL/MinIO root或重放初始化。

已验证：新账号连接/认证成功、三类旧值拒绝；MySQL无secondary旧密码；Redis DB1读取/AOF；私有MinIO真实读取/匿名403/短签成功后过期403；相关容器healthy、restartCount0；local/redis配置及env均0600。当前6396个tracked文件未包含所核对的6类新旧现场密钥。原T-32 Git/JAR整改证据不改写；轮换为其后独立环境处置。

脱敏结果在 `security-external-2026-09-23/rotation-result.json` 与 `tracked-credential-scan.json`。实际备份在 `/srv/ops/wta-plus/backups/owned/review-credentials-20260923T063047Z`，manifest哈希可回读。凭据与恢复检查点仅位于0600的 `temp/relase/` 和服务器授权目录；不得从这里复制到公开Evidence。

## 追加凭据的负责人处置确认

历史Git没有重写，旧值已失效；旧备份受限保留，不恢复泄漏密码。追加盘点发现：退役AI的sai_app.token、sai_model_config JSON存在非公开种子的凭据式值；wta-plus禁用qcloud配置也与公开种子不同。当前代码没有AI旧表消费者，停用也不能证明供应商已撤销。用户2026-09-23回复：“已经撤销停用，没有其他进行系统进行使用”。据此记录为负责人确认已撤销、无其他消费者；Lead未连接供应商复核，不将此确认表述为独立接口验证，也未删除历史业务记录。其余Client/SnailJob默认值与公开基座相同，禁用qiniu/aliyun为种子示例，不因此宣称外部有效或替换它们。

G-security-external已关闭：原DB/Redis披露及关联MinIO由实际轮换和旧值失效验证闭合；追加AI/qcloud项由上述用户明确处置确认闭合。此结论限定于已盘点范围，不代表未发现的任意外部账户均已审计。本次不是整套产品发布，未宣称双后端/前端业务候选验收完成。

## Skill实际执行

`deploy-namewta-environment`：takeover限定范围；真实Compose labels/路径/服务/挂载/端口/账号读取、备份校验、定向切换、连接与旧值失效、私有对象及签名验证、0600最新报告。使用其v1审计兼容profile记录多项目维护，validate-profile exit0；没有伪造v2双后端完整发布。MySQL双密码按[MySQL 8.4官方语法](https://dev.mysql.com/doc/refman/8.4/en/alter-user.html)；MinIO当前mc帮助及[官方实现](https://github.com/minio/mc/blob/master/cmd/admin-user-add.go)确认从stdin传入用户/新密钥，未把secret放入命令参数。README中通用轮换清单由本环境记录落实。

唯一最新部署报告：`temp/relase/namewta-deployment.md` 与 `/srv/ops/wta-plus/deployment/namewta-deployment.md`，内容含凭据、0600、未跟踪、不回显。失败过程及前向恢复边界在脱敏JSON/私有报告中保留。
