# T-41 后端切片与正式合同检查点（尚未完成本票）

当前产品检查点 `42f07eb` 仅完成后端和正式生成合同，前端旧数组消费者尚待迁移。完整票据仍in_progress，result_sha=null；本阶段是预先约定的分段实现反馈，不是已提交的完整票据集成包。不得把后端阶段通过记为T41已验收或归档就绪。

| 点位 | 源码 | 真实结果 | 结论 |
|---|---|---|---|
| red | 8a90e404fcd899aa68482f3f52bb902521dc6aa0 | 1 test/1 failure/0 error/0 skip，run afc15f3575e3b1f6 | JWT owner和A/B HTTP成功后，page26旧data数组形状触发预期红灯 |
| A1 | b08105fb00ce2006bfd7a4083758033035d632b1 | 3/3fail/0error/0skip，run3149ce13e6dfd617 | 手装factory漏MPJInterceptor，导致Row动态映射ClassCast/业务500；不追认通过 |
| A2 | 662b351fca76a9e50ae8fba699e9e26aa65225f8 | 3/0/0/0，runff7664968f5f9976 | 夹具按生产MPJInterceptorConfig装配，并加真实DAO正控制，关闭装配根因 |
| A3 | 9d748aa9df9310cb64320e5e0db83d6dad8aeff7 | 4/0/0/0，runae58665d5b552a2b | 审查发现旧readAll覆写首次时间；新增single SQL参数化COALESCE条件更新及HTTP/MySQL原时间保留测试，既有三例保持 |

各owned运行均fresh六SQL/103表、独占app账户、实际DAO/MySQL/Redis、Jetty/MVC/JWT LoginHelper/Sa注解权限，source前后精确clean、匿名卷/完整ID/PGID/端口清理全部成功。该Jetty fixture不装完整生产Client策略，不声称完整登录链通过；后续full Admin真实浏览器负责补齐。A3明确覆盖：第501条、本人/他人/不存在详情、无登录/无写权限、列表无content、全局未读/跨页readAll、孤儿JOIN三值、固定顺序/极大页码/非法参数、首次seen/read时间与幂等/B不变。

A3执行 `./mvnw -B -ntp clean package -DskipTests` 与full bundle内容校验均exit0，实际JAR SHA256 `2da66e55cea75a4ba65ab4c4ab4cc69ebebf2c37b5c9dae5ede8d207eddab8f2`/214186495bytes。这是导出合同的中间full包；default全套测试留待前后端完整候选后统一跑，不能把skipTests包装成全套通过。

## 实际OpenAPI迁移

冻结v1捕获run7671ef3441f17602 HTTP200/510375bytes，但严格旧schema零丢失检查拒绝 `RListNotifyInboxMessageVo` 的移除；paths/methods/其他schema均无丢失，source/JAR/cleanup全部通过。保留失败result与原始字节，不称v1通过。原schema在旧文档仅被 `/notify/inbox GET 200` 引用一次，本票已授权同候选把其数组改为PageResult，新增本人详情；这是一项明确响应迁移。

Lead在私有capture v2仅修改validate_full_openapi并新增validate_inbox_migration，AST核对所有资源与JAR/源码/cleanup函数未改；只有该旧数组确属唯一引用且旧items类型正确、新响应RNotifyInboxPageVo/NotifyInboxPageVo.rows/total/unreadTotal与详情/整数query均验证后，才接受此一项移除。其他path/method/schema损失仍失败。11项正负离线测试exit0；v2 SHA256 `92ad110cf52d7ab6570e07bfc68d5631627b6a55c4c31712d23439dda999b08c`。

v2真实run743626759bcb709c，HTTP200/510375bytes、437paths/447schemas、SHA256 `36c4388a99a2f3dd7cb1d528846af5b07669ea1c8f810264a82028c25e98eab6`，actual source9d748aa与JAR一致且cleanup[]。正式fetch→generate→check各exit0，生成commit42f07eb，revision `4ab6abdaf712861468eb9bc86b8d52c607a82402a600261ddf02776862c54bf3`；TS903193bytes/SHA256 `8d3e64dd2f1b62f4a3c945462b20439415a4dca1505495f258367d470570cffd`。生成source与真实HTTP原字节一致，provenance绑定实际后端commit/loopback endpoint；其他common schema语义零变化，common path仅inbox改，新增detail路径。未手写类型/快照。

## 恢复与下一步

45个原始检查点文件在T-41-current-2026-09-23/backend-checkpoint-manifest.json逐一列hash。full JAR保留私有副本，不提交环境secret或JAR；日志/XML保留原字节，raw whitespace不重排。只读审查和失败记录均保留。

当前cors_audit继续单产品writer完成前端/domain/全部消费者与HTTPS stub、session隔离/global badge、真实browser case/config。ops_audit在/tmp独立准备的browser runner及18项离线检查经只读审查，无产品写入；cors审后才纳入已登记frontend/e2e。Lead继续独占构建/commit/真实资源，构建和exact-clean实测串行。完整默认/full/core门禁、实际501 UI与完整Client登录、原Notify受影响回归、完整双轴审查与最终证据尚未完成。
