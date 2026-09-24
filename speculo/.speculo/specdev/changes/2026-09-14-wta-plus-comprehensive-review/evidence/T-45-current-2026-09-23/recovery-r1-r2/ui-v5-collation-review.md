# T45 UI v5：仅修 owned 异常行删除比较

v4 真实 run `9d62ae67389c950b` 在 `browser_fixture` 阶段已实证：同页 total=rowcount=7、owned bad/public/private 三者存在、未知策略数=1；随后精确 DELETE 的 MySQL 错误为 1267/HY000，未到 Chrome，最终清理错误 0。既有 v4 结果保留且不是通过证据。

根因范围是删除夹具的 `config_key` 字符串等值比较。v4 使用 `base.hexsql(key_bad)`，该 `CONVERT(0x... USING utf8mb4)` 在 MySQL 8.4 的已记录诊断中使用 `utf8mb4_0900_ai_ci`；owned MySQL 显式以 `utf8mb4_general_ci` 建库表，`sys_oss_config.config_key` DDL 未覆写列排序规则。两种不同隐式 collation 比较产生 1267。基座已有 `base.compared_hexsql` 专门对字符串比较追加 `COLLATE utf8mb4_general_ci`。

v5 只将该 DELETE 的 key 比较改为 `config_key=base.compared_hexsql(key_bad)` 并在私有函数中拒绝缺少显式 `COLLATE utf8mb4_general_ci` 的表达式。`oss_config_id`、owned key、`access_policy='9'` 三重精确谓词、`ROW_COUNT()=1`、前后 GET 安全计数、先前 INVALID_ACCESS_POLICY HTTP 负例和两个原 Chrome 用例均不变。没有修改 v4、产品、表 schema、用户/生产数据或验收断言。

离线测试验证真实基座 `hexsql` 与 `compared_hexsql` 生成形状、DDL/owned server collation 前提、缺 collation 的负例、删除身份保护和保密摘要；这不是对真实 v5 服务结果的替代。仅 Lead 在新 clean 源/JAR/dist proof 具备后运行：

```text
python3 /tmp/wta-t45/run-minio-diagnostic-ui-v5.py --execute \
  --expected-head <new-clean-SHA> --expected-jar-sha256 <new-full-JAR-SHA> \
  --package-proof <new-private-proof.json> --dist-manifest <new-private-manifest.json>
```
