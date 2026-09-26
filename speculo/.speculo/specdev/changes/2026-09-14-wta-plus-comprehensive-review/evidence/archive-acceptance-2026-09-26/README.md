# 归档补充验收（2026-09-26）

源码固定于 `c671b8cfbc7f2894b49580cc81f2e097ee066807`，执行前后 Git HEAD/tree 相同且 tracked/untracked clean；证明见 source-before.json/source-after.json。命令与退出码见 summary.json。本轮补验是真的当前执行，不是历史时点重建。

Linux 从含空格的路径调用真实脚本；先显式 build backend，随后后端及 Home 前端各启动两次，均 HTTP 200 后由 runner 终止自己启动的进程组。start 日志无 clean/install，doctor backend exit 0。外部配置、SERVER_PORT/VITE_APP_PORT 使用本轮临时值。隔离 MySQL/Redis 初始化六 SQL，启动账号和凭据仅测试可用；容器清理退出码均 0。第一次夹具 Redis 未配置密码但客户端执行 AUTH，启动失败；第二次统一隔离密码后通过，未修改产品或断言。Windows 继续 user-waived/not-run。

存储测试使用隔离 MySQL/MinIO，10 项零失败/错误/跳过，涵盖 T49 配置校验、真实默认存储切换、无附件零 OSS 调用和 OSS 清理/迁移。详情 storage-tests-result.json；容器清理退出码均 0。

前端、默认后端、release、Skill facts 与 SpecDev 包级自检全部 exit 0。默认后端 251 项环境跳过不算通过，亦未被这 10 项全部替代。上一轮审查的其他真实服务与浏览器结果按其真实执行范围保留于 reviews/adversarial-2026-09-26.md。

历史票据的实现提交可核实；其中部分没有保存原始 clean 时点。旧失败/未运行/旧原文保留，不把本轮结果反写为当时通过。是否采用当前补充验收替代缺失历史 clean 证明仍待用户裁决，因此本记录不宣称 change completed 或可归档。

## 用户裁决与补验结论

2026-09-26 用户对“采用当前已提交、干净工作树补充验收替代缺失历史 clean 时点”的明确回复为：“同意采用本轮补充验收”。该批准仅改变此 change 的历史证据接纳方式，不声称恢复原始时点、不豁免产品测试、不修改通用验证器。上文 pending 是形成该记录时的事实，现由本节取代。

T43 聚合探针在 clean `d0759fdf` 上真实 MySQL/Redis smoke 1 项通过、零跳过；100 收件人中逐条领取并执行 10 次，1030 行聚合锁读、10 条消息收件关系、0 OSS 调用，清理完整。具体源前后完整 SHA/tree 在 fanout-smoke-result.json。新计量含 claim，不能与旧预领50条基线直接比较；顶层 runner 的 matrix acceptance=false 是 smoke 的预期标记，单次真实用例 acceptance=true，不宣称重跑完整性能矩阵。

本次补充验收与历史 replan 的实际行为记录共同作为完成依据。逐票源/result 使用真实非空实现提交，测试发生时间和当前 source 则分别记录，绝不把记录提交或旧共同 result 当作逐票实现。窗口后新增的 d0759fdf 只同步该探针和治理证据；c671 质量门的产品输入保持不变。默认环境 skip、实际供应商投递/生产部署未执行边界均保留。
