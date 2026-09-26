# T42 三次前置失败后的 Lead 复盘 / Dispatch02A

本文件保持 A1/A2/A3 全部失败事实。A1源码0595e2cc，编译前置失败、0测试；A2源码53505153，OSS3/3通过后Notify缺少实体import导致编译失败；A3源码0adb76af881968b0f6641bdf6e7d364f8af4d965/tree见原始JSON，16套件105测试，104通过、0failure、1error、0skip。真实E2E/full/core/OpenAPI均未执行，不勾AC。三个候选都是clean source前后相同。

## 共同失败模式

首次较大实施尚未通过全编译与现有夹具一致性检查，连续在进入真实E2E之前发现Java签名/import遗漏及原正向夹具的状态缺省。A1受检close异常、A2缺NotifyIntentAttachment导入、A3旧metadata正例deleteState=null，是不同具体失败，但均说明静态回交不能代替实际门禁。前三次不删除、不合并成一次成功；实际测试计数只取fresh XML。

## 最可能原因

生产ACTIVE-only合同已在revision226登记，旧单测构造的对象却没有设置持久默认ACTIVE；A3异常是该保守合同正确拒绝不完整夹具，不能据此恢复对null/NOT_READY的放行。跨模块大批实现时先前人工签名检查也漏掉了checked exception与import。独立审查另发现P1/P2/P3，53505153已实现send_reserved、UNDELIVERABLE回收、@Version/@TableLogic及20项full-context测试，但这些运行验收尚未发生。

## 下一轮具体改变

Dispatch02A由Lead亲自只修OssObjectMetadataServiceUnitTest：正向与缺失metadata夹具明确ACTIVE，新增完整metadata但PENDING/NOT_READY/null状态仍拒绝的反例；不得弱化生产状态条件。固定新候选后先重跑同16类定向门禁并核对每套件实际正数/零skip，再进入后续补验。此顺序把编译与既有夹具统一作为后续真实服务的硬前提，禁止未绿时继续堆入JDBC/HTTP新测试。

补充AC采用下一独立Dispatch02B：先复核private JDBC hook只定位真正send_reserved=true的提交（单看SQL列名会错命中复制更新），再串行补真实ACK丢失、强制唯一碰撞及身份负例。ops只在/tmp准备full-JAR HTTP/OpenAPI v3，不启动服务。所有owned SQL/HTTP/MinIO与default/full/core/frontend/静态、双轴审查仍是整票关闭前提。

## 下一 owner / 路由

三次阈值已停止向原writer重复派发并回到Goal Plan Lead；本复盘后Lead承担Dispatch02A唯一产品写锁，cors只读待命。累计失败固定为3，原日志/XML/hash/source保留。根据lead-orchestration复盘后新Dispatch规则，恢复批attempts从0计，累计历史不清零；这不是单纯修复后自动重置。Dispatch02B只能在02A实际门禁通过、另发明确写锁后开始。Ticket保持in_progress，Goal active，结果/result SHA与AC仍空。
