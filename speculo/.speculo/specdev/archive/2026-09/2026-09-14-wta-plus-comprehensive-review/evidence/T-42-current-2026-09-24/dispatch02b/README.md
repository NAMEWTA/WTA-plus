# T42 02B 私有保留证据（固定封存）

本目录仅保存已运行记录的**白名单脱敏副本**。`manifest.json` 对每个源文件与副本保存字节数、SHA256；`secret-scan.json` 只保存有界启发式命中数量，不保存任何匹配内容。原始 `*.raw.log`、env、properties、容器配置、`logs/sys-console.log`、Surefire `.txt` 均未复制。本目录权限 0700，文件 0600。

| 子目录 | 固定来源与结论 |
|---|---|
| `mail-6590bf05a59d56f8` | A1 源码 `6f5ba38…`，fresh Mail 27/0失败/0错误/0跳过，exit0、源码前后 clean、owned cleanup 空。**BEFORE 注入的物理驱动调用证明尚有证据缺口**，只保留为 A1 历史结果，绝不追认为后续候选完整通过。 |
| `mail-a36d6d66aa5e06bd` | A2 源码 `43e5c2b…`，增补 BEFORE 断连与真实 driver commit 被调用的标志后，fresh Mail 27/0/0/0，exit0、源码前后 clean、owned cleanup 空。 |
| `regression-fe74fb5e20ee9b0f` | A2 `43e5c2b…` 旧通知八类共 135 项，**1 failure/0 error/0 skip，exit1，acceptance false**；唯一旧绑定数量期望 10 对当前实际 11 的 fixture 错误后来由 `cd78c395` 一行修正，原失败不改写。 |
| `misconfigured-head-63404705414ab19c` | Lead 的 expected-head 误设，在 source preflight 阶段即拒绝；无服务、无测试、新鲜 XML 为零。该记录不是新的候选运行，result 内后续缺 XML/source-after 错误也不应解读为产品失败。 |
| `analysis` | A1/A2 Mail 与回归 exact method inventories、02B JDBC 和身份竞态只读审查；静态审查不代替上述真实结果。 |

当前 A3 `cd78c395` 回归已由 Lead 单独运行并确认通过；**A3 结果尚未收入本冻结目录**，由 Lead 在独立 checkpoint 阶段附加并重生总 manifest，不覆盖这里四个来源时点。此目录不把 A1/A2 邮件与 A3 回归当成同一候选的组合绿灯。

扫描范围为本目录保留的 25 个源副本，检查授权头、Bearer、密码/密钥赋值、私钥头、S3 签名、JVM 密码实参六类显式泄露模式；各模式命中均为 0。它是必要的脱敏核对，不是对任意未知 secret 的数学证明。副本仍保持私有，不直接当可提交公开日志。
