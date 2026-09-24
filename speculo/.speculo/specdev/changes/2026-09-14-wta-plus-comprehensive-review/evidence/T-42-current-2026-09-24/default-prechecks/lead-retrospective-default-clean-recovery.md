# T42 Lead 四项复盘：构建输出恢复

## 已观察失败及共同模式
02B A1 Mail27 XML通过但BEFORE注入证明存在缺口；A2补强Mail27通过后135回归134通过/1绑定数量旧断言；A3一行夹具修复后135通过，但3efc5862默认测试在common-openapi的JUnit discovery因ClassFormatError退出。先前全部记录保留，不重置为成功。

## 最可能原因与不确定性
保存的target class经javap确认同签名create/find重复，LoginUser以未解析的默认包类型出现；当前Java源码只有单一声明且T42基线以来该模块无diff。class在本次默认Maven启动前已存在。观察到Java语言服务进程，但未证明其一定为该class写入者，不把推断写作事实。实际失败发生于不可信旧构建输出。31项fresh XML含2环境skip只属未完成前缀；full打包尚未执行。

## 下一实质改变
保留class摘要/私有副本、javap和fresh报告后，在同一clean SHA先使用Maven clean移除各模块target，再执行完整默认mvnw test及full clean package；不改源码、断言、编译器、profile或排除测试。保持进程堆上限1536MiB，环境skip单列。若同一class仍损坏，再定位并隔离并发builder，不盲目继续重试。真实Mail27与135源坐标保持，不重复无关服务。

## Owner与恢复入口
Lead独占构建/服务，所有agent只读；新构建输出恢复批第1次，原02B候选3次和前批8次失败永久保留。执行私有prepare-source-backend-clean-v1.py于固定3efc5862，先clean有记录且source前后恒定，再使用原全默认/full准则。此为不同恢复操作，不是新产品修复或免验。
