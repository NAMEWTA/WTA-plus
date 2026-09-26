# T44 `owned_minio` 首轮失败：静态定位（未运行服务）

输入：原冻结 `/tmp/wta-t44/run-minio-real.py` 与 Lead 派生的 v2；首轮清洗后 `/tmp/wta-t44/runs/92641a8250fba5db/result.json`。本审查仅读文件，没有读取已删除的私有 env/alias/raw 日志，没有重跑 Docker、mc、MySQL、Maven 或 HTTP。

## 确定的夹具缺陷

原版与 v2 的 `config_a` UPDATE 用 `h('')` 写空 `domain_url`（v2:561）；`config_b` INSERT 同样用 `h('')`（v2:574）。`h` 是 tracked T40 helper `hexsql(value) -> 'CONVERT(0x' + value.encode().hex() + ' USING utf8mb4)'`。空串得到 **`CONVERT(0x USING utf8mb4)`**，不是有效的十六进制字面量；MySQL 会在 `config_a` 的第一条 UPDATE 拒绝。首轮 result 已含 `minimal_privilege` 三项 true，失败 phase 仍 `owned_minio`、type RuntimeError、无 app 进程，且 v2 70 个固定异常均未命中；这与 `count_sql` 的动态 `owned SQL failed at config_a` 完全吻合。仍以 Lead 的 v3 安全 stage/数字错误码取证为实际确认，不追认尚未返回的结果。

最小修复仅在私有驱动：两个**源码固定空字符串**用 SQL 字面量 `''`，其余来自生成凭据/桶名的值继续 `h(nonempty)`。不需要改生产代码，不需放宽检查或把 SQL/secret 原文写进证据。修复后 `config_a` 及 `config_b` 两处都要审；只改第一处会在稍后相同原因失败。

## 本轮已证明和仍未证明

首轮 result 显示 103 表、full JAR proof 和 `minimal_privilege.bucket_policy_write_denied/object_acl_write_denied/object_head_allowed` 三值 true，cleanup errors[]，没有启动 app。故这次失败**不是** MinIO accesskey 创建、app alias 导入、app HEAD、ACL no-op 写拒绝或 `mc anonymous set private` 拒绝探针的直接失败。它们通过也不证明后续 app 的真实上传/下载/诊断已通过。

拒绝探针边界：signed `PUT ?acl` 要求 HTTP 403 且响应 `AccessDenied`，排除了 `SignatureDoesNotMatch` 误报；目标是 owned PRIVATE 桶上原本 private 的 canary。`mc anonymous set private app/<owned-bucket>` 在 app alias 已成功 HEAD 后返回 AccessDenied，说明该客户端命令被拒；但 mc 可能先读桶策略再写，故这一条**单独不能精确证明** `PutBucketPolicy` 写动作被拒。附加 accesskey 的静态 policy 没有 `s3:PutBucketPolicy`/`s3:PutObjectAcl`，行为上无权；若需直接区分读拒与写拒，后续可对 owned 桶使用同一 app key 签名的 no-op `PUT ?policy`，要求 `AccessDenied`，不能触及非 owned 桶。此增强不应阻挡当前 SQL 夹具修复，也不能以本次 passed 三值代替 full-app 验收。

`v2` 的 allowlist 只记录固定 `RuntimeError`，`count_sql/one/base` 的动态错误仍只有 type/phase；Lead 预告 v3 增加固定 SQL stage 和数字错误码，是正确的下一证据入口，不应输出 SQL 文本或 mysql 原始 stderr。
