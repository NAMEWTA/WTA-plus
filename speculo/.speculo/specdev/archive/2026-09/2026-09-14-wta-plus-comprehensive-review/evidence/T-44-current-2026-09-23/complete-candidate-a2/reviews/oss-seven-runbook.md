# T44 七类现有 OSS 真集成隔离验收

固定产品源码：`fc50c1e1227d42a46f8e25b3e19949baeccb89e0`。仅由 Lead 在无并行产品 writer、Maven、frontend build 的独占窗口执行一次：

```bash
python3 /tmp/wta-t44/run-oss-seven-integration.py --execute --expected-head fc50c1e1227d42a46f8e25b3e19949baeccb89e0
```

脚本默认不启动。执行前后要求同一 clean HEAD，并逐文件核三 App 现有 dist 清单；不构建产品。运行时创建随机双 label/full-ID owned MySQL 8.4.9、Redis 8.6.3 与固定 digest MinIO；MySQL 仅新建一空迁移库并授予该库应用账户，绝不导六 SQL 或连接现存 WTA 库。Redis 无密码，仅随机 127.0.0.1 映射，用于原有 port-only 夹具。MinIO 临时 root 仅用于这七个旧测试自行建桶及写公开策略，不能代替已单独验证的 T44 受限应用身份。

一次 `./mvnw -B -ntp -o -Pdev -pl wta-admin -am ... test` 精确选七类，预期七份 fresh Surefire XML、各 1 项且 0 failure/error/skip；其中 BrowserUploadLifecycleIntegrationTest 的子进程另要求 Playwright `10 passed`、逐项 `[1/10]` 至 `[10/10]`、无 skipped/failed/flaky。它是模拟 HTTP 控制面的真实 MinIO/Chrome 字节测试，不能当作全应用 HTTP 验收。

生成的凭据只进 0600 Docker env-file 或 0600 `surefire.properties`，Maven argv 仅含后者路径。`JAVA_TOOL_OPTIONS` 仅有公开内存限制与本次 owned `java.io.tmpdir` 路径，不含凭据；fork 与 Chrome 继承 owned TMPDIR。`systemPropertiesFile` 入口已由本机 Surefire 3.5.5 插件描述符和独立 `/tmp` offline Maven probe 证实：私有文件哨兵 1/1 通过，而与仓库相同的硬编码 POM argLine 下 CLI `-DargLine=@file` 哨兵 1/2 失败，因此此 runner 不使用 argLine。正式每份 XML 必须出现本次私有 marker。

私有结果路径由 runner 末尾 stdout 返回，格式 `/tmp/wta-t44/oss-seven-runs/<随机ID>/result.json`；只记录安全计数、方法名、源码/JAR无关的 dist 清单 hash、XML/日志原始 SHA 与脱敏 SHA、资源回收事实。fresh XML 与 `.txt` 仅保留脱敏副本，已知合成凭据及 SigV4 query 值去敏；临时属性文件和 Chrome 原日志删除。对已有 Playwright `.last-run.json` 做有界原字节恢复。Maven PGID 之外，按本次唯一 TMPDIR 环境/命令路径核查 Node/Chrome/JVM，必要时只终止该 owner 的进程；最后逐名确认匿名卷、三端口、容器全部不存在。任何门禁或清理失败均返回非零，不自动重试。

风险边界：正式七类尚未运行；Playwright `line` reporter 的精确 XML 嵌套形式与 `[1/10]` 行号已做纯合成解析，若真实格式不同脚本会安全失败，需保留原始失败证据后另起版本修证据解析，不能直接宣布通过。当前 README 不包含任何生产凭据。
