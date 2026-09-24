# T45 单 MinIO 策略表示探针

目的：在独占 MinIO 上使用与 `OssStorageReadinessMinioIntegrationTest.publicReadPolicy` 完全相同的策略文本，观察 MinIO 回读后的**结构**和当前生产诊断事实。此前真实 two-suite 运行的公开桶匿名 HEAD/GET 成功，但 `POLICY_READ` 为 UNKNOWN；本探针不预设其原因。

已准备并离线检查，尚未编译 Java、启动容器或运行探针。冻结输入是同源、去敏的 Surefire XML `0627f8329450e8f7`；脚本校验其 SHA 后提取原测试 classpath，不运行 Maven，也不改 `target`。只用一个随机双标签的 loopback MinIO 容器，私有环境文件不会进入命令行；仅保留安全结构布尔值、statement 数量、诊断枚举及清理布尔/计数。原始策略、桶/对象名、endpoint、凭据、Java stdout/stderr均不持久化。Java 退出非零时只报告固定阶段和异常类型，不把 SDK 消息写入结果。

Lead 应待当前 UI 服务结束且源码 clean、没有并行门禁时，从仓根运行：

```bash
python3 /tmp/wta-t45/policy-probe/test_policy_probe_offline.py
python3 /tmp/wta-t45/policy-probe/run-policy-probe.py --execute --expected-head be35584b0d8dcfbb560dc9f338c303dafc77fbfd
```

第二条会打印唯一 `result.json` 路径。检查 `exit_code=0`、`source_same_clean=true`、`owned_cleanup=true`、`cleanup_error_count=0`；重点看 `probe.shape` 的 `principal_aws_* / action_get / resource_match / has_condition / has_not` 与 `probe.facts.POLICY_READ` 的 observation/basis。若源码 SHA 已变化，须先核实是否仍是与失败运行相同的编译产品及 classpath，再更新期望来源；不要跳过 clean/source 检查。

离线记录：最初误用 `python3 -m unittest <绝对路径>` 导致测试模块定位错误，exit1；正确命令 `python3 /tmp/wta-t45/policy-probe/test_policy_probe_offline.py` 实测 4/4，exit0。没有 Docker/Java/Maven 操作。
