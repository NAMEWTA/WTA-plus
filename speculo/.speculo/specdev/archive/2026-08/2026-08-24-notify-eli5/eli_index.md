# ELI5 图解索引

| 编号 | 文件 | 主题 | 简介 |
| --- | --- | --- | --- |
| 01 | 01_notify架构与依赖方向.md | notify 代码构造与依赖方向 | 图解 common-notify、Mail/SMS Adapter、system 监控与 admin 组装的职责和设计原因。 |
| 02 | 02_notify架构模式与模块合并.md | notify 架构模式与模块合并 | 解释端口适配器等架构模式，并比较单目录、多 artifact 和单 JAR 三种组织方案。 |
| 03 | 03_通知内核端口适配器插件实现.md | 通知内核端口适配器插件实现 | 沿真实启动和发送代码，图解端口、Spring 插件发现、Registry 路由以及新增渠道的实现方式。 |
| 04 | 04_系统消息公告与notify.md | 系统消息、通知公告与 notify | 图解 sys_message 的轻量站内消息盒子实现、sys_notice 的公告发布链路，以及它与外部通知投递 notify 的边界。 |
