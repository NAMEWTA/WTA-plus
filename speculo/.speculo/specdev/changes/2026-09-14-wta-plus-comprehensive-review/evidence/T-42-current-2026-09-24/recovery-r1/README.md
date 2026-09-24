# T42 恢复批 R1：定向通过，真实应用启动失败

固定7070e1d79b7a0934492d0f07ddb969edbb629983/tree ec9b759b5d74c0ba21d5678812b3415817fbd074，前后clean。定向16套件106测试，0fail/error/skip，exit0。真实MAIL v5 run8f6c6e89433059e7执行owned六SQL，104表成功，但WebEnvironment.NONE使生产AllUrlHandler缺requestMappingHandlerMapping。JUnit XML是类初始化1error/0failure/0skip、testcase.name为空，不是20个方法执行；严格runner拒绝该非方法XML是正确的，实际20方法体执行0。S3到达0也不能冒充无附件零OSS正例。

MAIL exit1/acceptance=false；source前后相同，MySQL/Redis/MinIO三个容器、两个卷、五端口和Maven进程均清理，cleanup.errors=[]。仅保存工具已脱敏的XML/日志，不保留认证配置。恢复批attempts1、前批失败3不变；full/core/正式OpenAPI和前端仍pending。

下一窄修改：测试使用WebEnvironment.MOCK装配真实MVC与生产安全Bean，不监听HTTP端口、不mock安全组件或生产SPI；实际HTTP仍由独立full-JAR v3验证。修改后固定新源码重跑Mail20。该选择修正测试上下文与实际应用依赖不符，不修改产品安全合同。所有剩余JDBC ACK、强制幂等碰撞、身份负例随后另派02B，不能把private离线测试当产品验收。
