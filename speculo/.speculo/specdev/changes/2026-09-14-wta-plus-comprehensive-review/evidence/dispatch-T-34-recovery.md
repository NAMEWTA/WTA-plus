# Dispatch Packet T-34-20260923-02

operation=dispatch；task_kind=implementation；delivery_channel=native（Lead自行执行，无新子代理）；Lead/owner=single-agent；repo=/srv/WTA-plus；current/main/direct-parent；固定checkpoint=24909df0e49208535097f03d174c0592ea6836d3，原base6fcbfeb保持。唯一产品writer已由cors_audit交回Lead，其他代理仅只读。

输入Map revision142→现有三Skill与Python脚本安全/进程资源规则→T34。已完成强制复盘evidence/T-34-recovery.md，旧周期3轮不删除，新周期attempts=0。实质变化：测试资源独占，不与类型检查/构建并行；将实跑隔离驱动纳入frontend/e2e/run-inbox-real.py及inbox-real.md，仓库相对根、精确SHA/JAR/SQL、最小env、owned服务、Playwright进程组清理。E2E timeout与行为断言不变。

写集只在Ticket已声明frontend/e2e/，产品消息逻辑冻结。禁止改部署/secret/工具链/产品合同；不创建新worktree或远程写入。非E2E：Python语法/help及runner逐行安全审查；旧24909df前端输入与新HEAD无差异则关联714/5/构建/类型/lint真实结果。E2E owner仍Lead，单独运行真实用例并捕获clean前后；失败有新证据再分析，不盲重试。返回实际commit、命令/exit/计数/cleanup和残余限制。
