# T-41 三次完整候选检查点与Lead复盘（未恢复派单）

Revision177: T41 current batch reached3 complete-candidate attempts: B frontend fixture failure; C frontend750/buildpassed but missing declared actual-host coverage; D frontend751/default870+201skip/full/realHTTP4/shared135 passed but owned browser seed SQL exec failed before JVM/Chrome. D sourceclean/cleanup[] preserved; ticket/workspace blocked, result null. Stop automatic resend. Lead diagnosis and materially changed dispatch required before resetting a recovery batch.12done/2cancelled/T41blocked/35ready; Goal remains active.

## 1. 失败模式

B05d0f34：架构/lint/typecheck通过，但人工SFC缺SSR context及既有registry test未mock新userStore导致五例setup失败和一suite import失败。Ceff81f6只修两测试，750测试/三Appbuild通过，但缺revision176已声明的实际宿主snapshot时序验证，独立审查未接受。Dd93eb1d补该一测试，751前端测试/默认后端1071（870执行、201envskip）/full bundle/真实HTTP4/共享Notify135均通过；真实浏览器runf2ed71eeedb7b8e2在初始化SQL时失败，尚未启动JVM、Vite或Chrome，因此不是UI失败也不是通过。

## 2. 已知原因与未决诊断

B/C已通过确定的最小测试修改关闭，旧断言均保留。D六SQLinitializer exit0且103表/无outbox/无外部投递/无启用供应商/单private OSS基线已成功；随后one_user、Admin Client查询、两次owned密码更新之间的一次mysql exec失败。原driver只留通用Docker错误，未保留SQL错误号/SQLSTATE/阶段，现有证据不能定位具体语句，也不能宣称已经证明collation根因。需要以同一六SQL/镜像的新owned单库诊断复现该区间，并只保留允许的数字错误号、SQLSTATE和受控操作名，不保存SQL正文/密码。

## 3. 下一步实质变化（尚未启动新完整尝试）

先独立只读核schema/query与连接collation，再准备一次可审查的owned诊断，明确执行到哪一段；诊断证实后只修runner连接/fixture边界与针对性离线/真实检查，保持权限、数据断言、exact-source/JAR与资源清理门禁。新增安全阶段诊断以防泛化错误掩盖后续根因。新的完整候选必须有非空工具修复commit与新的Lead Dispatch；本记录不重置attempts，不能直接原样重跑。

## 4. 后续责任人

Lead负责block快照、诊断工具审查/唯一服务运行/恢复裁决与新Dispatch；ops_audit负责固定D失败点只读审查；cors_audit当前仅未来T40只读准备，未授权修改T41产品。确认根因与具体修复后再转交唯一产品writer。legacy_audit/ops_audit按新输入复核安全与实现轴。

D source before/after同一clean d93eb1d/tree668f14e，JAR SHA97f1e73b保持；临时三容器、两卷、端口已全部清理，无服务器/供应商副作用。所有本阶段原始记录和258份默认fresh XML、四项/135真实XML保存于three-attempt-checkpoint/manifest.json。C三App生产build按D唯一test diff逐件hash复用，不冒称D再build。先前红灯、后端A1/A2/A3和B证据保持原manifest不变。

## 证据脱敏

默认reactor日志及其中少量XML包含测试生成的JWT形状值。收集器首次检测到即在写入前拒绝，随后以`[REDACTED_JWT]`生成脱敏副本；manifest为每项保存原始source_sha256、脱敏后sha256和替换数量。共2个文件、2处替换；未输出或提交原始token，未改变用例计数/状态/时间或其他日志内容。第一次中断的部分安全副本逐字核对后恢复收集，不删除既有证据。
