# T42 Lead 复盘与 Dispatch02B：完成剩余真实故障矩阵

## 共同失败模式

Dispatch03三次候选：A1完整业务6pass/14fail；A2启动硬断言证明生产Redis幂等Store未装配、20方法体0；A3修复顺序后20/20真实通过。此前两批6失败全部保留，加本批2失败为历史8失败。单测手工Bean/空lambda曾掩盖生产装配差异，依次通过真实配置红绿及全应用硬断言定位，不再把夹具成功当生产链证明。

## 最可能原因及已证结论

后台审计直接读需请求的SaToken上下文、Notify早于Redisson导致OnBean跳过Store，是已由固定源码红绿证明的两个生产缺陷；逻辑删除引用的旧COUNT断言属于夹具错误。764820dd已以113单元、真实MySQL/Redis/MinIO20方法全部通过闭合这部分。20方法中的普通并发不能证明唯一冲突后的当前读；SQL触发器回滚不能证明真实JDBC commit ACK丢失，因此仍缺不同的合同证据。

## 下一轮具体改变

Dispatch02B只增补真实数据库与身份边界，用现有20全应用案例作为不可删除基线，预计新增7方法。引入已离线复核的OwnedAttachmentJdbcFaults与OwnedAttachmentSendReservationSqlProbe，保留其准确true/1 SET绑定与1row匹配，false复制更新不可触发最终发送预约故障。动态数据源只CAS替换master entry的物理池包装，不调用会close原池的addDataSource；原池保持所有权，全部线程join后恢复。

1. 提交BEFORE：在真实relation INSERT后、物理commit前由另一同池连接KILL当前owned连接，断言Intent/Relation/Outbox/Ref均未持久，原key可正常重试一次。
2. 提交AFTER：真实commit已返回后抛ACK异常，断言事实已提交；同key重试返回原receipt，单关系/引用/出箱且无额外对象。
3. 最终发送预约BEFORE：只在send_reserved=true UPDATE成功连接commit前断开，所有标记回滚，物理sender0；共享READY/refs完整，不能误记供应商UNKNOWN。
4. 最终发送预约AFTER：true提交后丢ACK，物理sender0且send_reserved已持久；取消及到期重领后不得释放任何ref。
5. 强制唯一竞争：A已INSERT未commit，B已通过普通幂等查找并进入INSERT，再释放A；必须证明B遇真实唯一约束并执行当前读，两个receipt一致且relation/ref一次。
6. 同key同附件改User：真实另一正常用户及有效授权来源，从真实Sa会话提交，不能返回首个actor的receipt或改其关系。
7. 同key同附件改Client：真实另一允许用户登录域的Client与会话，同样失败关闭。保存恢复原会话/客户端/fixture；不要靠公开command伪造actor。

测试故障helper不改变生产DAO/SPI/NotifyClient/事务代码；只捕获最底层物理sender。SQL/线程fault计划需one-shot、作用于当前测试owner，所有release/restore放finally，异常不得遗留线程或污染后续测试。不得删除/弱化当前20用例；实际方法计数由Lead重新冻结。若发现新生产缺陷，只回报事实/最小路径，先由Lead登记再改。

## 下一 owner / 路由

Lead已回读source764820dd、fresh XML方法名/零skip及owned cleanup，完成本复盘后重置新补验批attempts0。cors_audit唯一产品writer，基线为本治理commit之后current main，允许3文件：现有NotifyMailAttachmentIntegrationTest.java、同目录OwnedAttachmentJdbcFaults.java及OwnedAttachmentSendReservationSqlProbe.java；均属于已登记62根。不得构建、起服务、提交、改治理/生成合同。回交须逐项列证据计划、已改路径和未执行事项；Lead独占所有命令/服务/提交，随后再跑完整27项及余下整票门禁。其他agent仅私有准备/只读审查，无并行产品票。
