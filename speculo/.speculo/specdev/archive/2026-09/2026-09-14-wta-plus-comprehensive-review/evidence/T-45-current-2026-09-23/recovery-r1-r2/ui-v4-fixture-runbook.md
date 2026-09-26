# T45 UI v4：异常策略 HTTP 负例与正常 Admin 页面隔离

v3 真实恢复 run `c8ce84f90b58bf05` 保留 `1 passed / 1 failed`、安全首位置私有 spec 第 46 行 `toHaveCount(1)`、`cleanup_errors=[]`。该结果没有保留实际 row locator 的匹配数；异常策略夹具污染页面列表是源码可推断的风险，不能称为已观测的唯一根因。前置 INVALID_ACCESS_POLICY HTTP 负例在 v3 结果中通过。

v4 是 `/tmp/wta-t45/run-minio-diagnostic-ui-v4.py` 的独立私有新版本，完整沿用冻结的 v3 spec/config/server，不改任何产品代码或旧 runner。它保留真实 invalid-policy POST/无远端请求断言，然后在 Chrome 启动前用管理员 token GET `/resource/oss/config/list?pageNum=1&pageSize=10`，与当前页面初始列表页一致。仅记录 HTTP status、业务 code、total、rowcount、异常行/public/private 行成员布尔及未知策略计数。要求此页完整（total=rows<=10）、唯一 owned 异常行的 ID/key/物理策略 `9` 相符，公私行均在页，除此之外策略只为 `0/2`。若任一前提不成立，则失败并保留安全计数；不猜测原 v3 失败原因。

只有上述前提满足时，脚本在其**本次随机 owned 空库**执行精确 `DELETE ... WHERE oss_config_id=<id_bad> AND config_key=<key_bad> AND access_policy='9'`，要求 `ROW_COUNT()=1`。随后再次同页 GET，要求异常行消失、公私行仍在、所有策略均为 `0/2` 且 total/rowcount 恰减 1。私有结果不写回行内容、配置 key、凭据、URL 或 HTTP 原文。这是验收夹具的场景边界，不是放宽生产 `projectAccessPolicy`，也不删除原 HTTP 负例。

之后照旧运行两个精确 Chrome 用例，零 skip/retry，同一 timeout、身份、六事实、公私对象状态和警示断言不变。v3 的 source/JAR/proof/dist 绑定、双标签资源归属及 finally 清理均保留。Lead 新一轮 clean HEAD + full JAR/proof + admin dist manifest 准备好后，才可执行：

```text
python3 /tmp/wta-t45/run-minio-diagnostic-ui-v4.py --execute \
  --expected-head <new-clean-40-hex-SHA> --expected-jar-sha256 <new-full-JAR-SHA> \
  --package-proof <new-private-package-proof.json> \
  --dist-manifest <new-private-admin-dist-manifest.json>
```

本版本只做私有离线测试和静态预检；未启动服务、Maven、Chrome，也未触碰共享或生产配置。
