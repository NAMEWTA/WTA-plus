# T42 fixed candidate partial acceptance — TICKET NOT COMPLETE

revision233：T42固定764820dd的113项单元与20项真实MAIL均零skip通过，104表fresh与owned清理通过；整票仍缺ACK/强制碰撞/身份/HTTP等验收。Lead复盘后Dispatch02B补验批attempts0，历史8失败保留；17done/2cancelled/1in_progress/30ready。
## revision233 — 真实附件20项通过与剩余故障矩阵派单

revision233：T42固定764820dd的113项单元与20项真实MAIL均零skip通过，104表fresh与owned清理通过；整票仍缺ACK/强制碰撞/身份/HTTP等验收。Lead复盘后Dispatch02B补验批attempts0，历史8失败保留；17done/2cancelled/1in_progress/30ready。

证据evidence/T-42-current-2026-09-24/dispatch03-a3-pass/manifest.json。store-order-red固定2534405f为3项2pass/1fail；afterName修复764820dd固定18套件113/0/0/0，真实MAIL run dbc2abb3de3a852b为20/0/0/0，源码前后clean同HEAD/tree。真实生产Redis Store已装配，实际请求审计userId/Client正确；仅底层物理邮件sender替换，完整Notify/System/OSS/Redis/事务链均为生产Bean。20项覆盖零附件零OSS、真实私有字节、多收件人共享与顺序、owner/client撤权、COPY_UNKNOWN/迟到PUT、deadline/lease、send_reserved在取消及二次领取后保护引用、N项失败保留引用、乐观锁/逻辑删除。三个容器、两匿名卷、五端口、Maven/proxy进程全部清理。此为局部完整门禁通过，不是T42 Done；全默认、full/core、八类回归和真实HTTP/生成合同仍待完成。

本批达到3次candidate尝试，前两失败与第三通过均保留；按Lead四项复盘进入有不同交付物的Dispatch02B，而非继续盲重跑。新owner cors_audit唯一产品writer，只可修改现有admin notify测试根下NotifyMailAttachmentIntegrationTest及两份OwnedAttachment JDBC helper，负责7项真实用例（提交BEFORE/AFTER、最终发送预约BEFORE/AFTER、强制唯一竞争、更换User、更换Client）；不得改生产、构建、服务、提交或治理。完整Packet和四项复盘见同目录lead-retrospective-dispatch02b.md。Lead负责回读、固定source、runner准确27方法清单、实际执行与Evidence。新补验批attempts0，前两批6加本批2共8失败保留，62写集不变，所有整票AC不勾，Goal active。
