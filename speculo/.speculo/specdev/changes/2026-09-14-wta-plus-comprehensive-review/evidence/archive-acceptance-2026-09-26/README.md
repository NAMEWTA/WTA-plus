# 归档补充验收（2026-09-26）

源码固定于 `c671b8cfbc7f2894b49580cc81f2e097ee066807`，执行前后 Git HEAD/tree 相同且 tracked/untracked clean；证明见 source-before.json/source-after.json。命令与退出码见 summary.json。本轮补验是真的当前执行，不是历史时点重建。

Linux 从含空格的路径调用真实脚本；先显式 build backend，随后后端及 Home 前端各启动两次，均 HTTP 200 后由 runner 终止自己启动的进程组。start 日志无 clean/install，doctor backend exit 0。外部配置、SERVER_PORT/VITE_APP_PORT 使用本轮临时值。隔离 MySQL/Redis 初始化六 SQL，启动账号和凭据仅测试可用；容器清理退出码均 0。第一次夹具 Redis 未配置密码但客户端执行 AUTH，启动失败；第二次统一隔离密码后通过，未修改产品或断言。Windows 继续 user-waived/not-run。

存储测试使用隔离 MySQL/MinIO，10 项零失败/错误/跳过，涵盖 T49 配置校验、真实默认存储切换、无附件零 OSS 调用和 OSS 清理/迁移。详情 storage-tests-result.json；容器清理退出码均 0。

前端、默认后端、release、Skill facts 与 SpecDev 包级自检全部 exit 0。默认后端 251 项环境跳过不算通过，亦未被这 10 项全部替代。上一轮审查的其他真实服务与浏览器结果按其真实执行范围保留于 reviews/adversarial-2026-09-26.md。

历史票据的实现提交可核实；其中部分没有保存原始 clean 时点。旧失败/未运行/旧原文保留，不把本轮结果反写为当时通过。是否采用当前补充验收替代缺失历史 clean 证明仍待用户裁决，因此本记录不宣称 change completed 或可归档。
