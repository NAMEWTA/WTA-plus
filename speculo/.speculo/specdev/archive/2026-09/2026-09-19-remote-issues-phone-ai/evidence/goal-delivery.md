# Goal Delivery

三票本地完成；G-phone/G-ai/G-contract/G-final通过。最终产品 result 723e8514cbaeba13094415ba5f1071de31b2241c；各票 immutable acceptance、双轴审查、同源发布与实际运行详见 T-01.md、T-02.md、T-03.md。

用户指定实物数量按 tickets-map revision8核对：两个 Maven占位仍可构建，full包包含两JAR且core遵守原排除选择；六份SQL都进入最终发布，40只有合法SET NAMES语句，新库不建vendor表。以下每个路径已实际回读，最终发布实物/hash见 T-03-release.json。

## Delivery Records

```json
[
  {
    "name": "Java AI Maven占位模块",
    "outputs": [
      "backend/wta-modules/wta-ai/pom.xml",
      "backend/wta-common/wta-common-ai/pom.xml"
    ]
  },
  {
    "name": "MySQL初始化基座文件",
    "outputs": [
      "release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql",
      "release-artifacts/docker/infrastructure/mysql/init/20-cde-job.sql",
      "release-artifacts/docker/infrastructure/mysql/init/30-cde-workflow.sql",
      "release-artifacts/docker/infrastructure/mysql/init/40-cde-ai.sql",
      "release-artifacts/docker/infrastructure/mysql/init/50-cde-base-dml.sql",
      "release-artifacts/docker/infrastructure/mysql/init/60-cde-nacos.sql"
    ]
  }
]
```

手机号写入必填且旧空号登录保留；SnailAI当前源码/消费者/发布入口退出，旧数据不迁移、不删除。隔离23表结构/内容摘要验证通过。Go/Python保持独立暂缓；未部署、push、关闭远程Issue或归档。默认后端属性skip与一个外部Nacos浏览器条件skip均单列记录，未作为执行通过；本change必需场景已实际运行。
