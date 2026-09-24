# T42 基座表数文档同步（只读审计）

核查时工作树 HEAD `624432faaff2fa14f853642d3a3f782eac242d8a`；产品 writer 正在写测试文件，未将其混作已冻结实现。T42 已固定的自动配置修复源为 `764820dd`。本记录不更改仓库，也不复跑构建或服务。

## 当前事实与最小修订

`release-artifacts/scripts/init-mysql-container.sh:11` 已规定新建六 SQL 基座 `EXPECTED_TABLES=104`；`release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql:1425` 新增 `notify_intent_attachment`。因此下面三个当前说明的 **103** 已陈旧，只替换为 **104**，其余文字原样保留：

| 文件与行 | 现有片段 | 建议替换 |
|---|---|---|
| `.agents/skills/engineering-standards/references/project/00-project-profile.md:85` | `新业务库 103 张表` | `新业务库 104 张表` |
| `.agents/skills/namewta-fullstack-development/references/backend/mapper-and-sql.md:141` | `新业务库103张表` | `新业务库104张表` |
| `release-artifacts/README.md:226` | `六份 SQL 初始化新业务库103张表` | `六份 SQL 初始化新业务库104张表` |

这个计数只陈述**当前全新库六 SQL 基座**；不暗示对既有部署库重放 SQL。必须保留各句原有的 AI 模块退役、`40-cde-ai.sql` 仅字符集、旧 AI 表/数据保留不迁移、不重放已有库等硬边界。旧 ticket/Evidence 的 `103` 是当时来源快照，不批量重写历史记录。

## 写集事前登记

T42 票 `ticket/42-mail-attachment-contract.md` frontmatter 的 `expected_changes`、`writable_paths`、`shared_paths`、`shared_path_owners` 均尚未包含以上三个精确路径；`NotifyAutoConfiguration.java` 已在其中。Lead 应先对三个文档路径做精确事前登记及 owner 分配，并同步本轮 dispatch/路径审计，然后由持有对应写锁的文档 writer 仅作三个数字替换。当前票文 prose 的根数与解析出的唯一 `&lt;Path&gt;` 数并不一致，勿直接写成“62→65”；让现有 scope checker 重算。

## 自动配置顺序

`backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/config/NotifyAutoConfiguration.java:29` 的 `@AutoConfiguration(afterName = "org.redisson.spring.starter.RedissonAutoConfigurationV4")` 已在 T42 产品路径写集内。现有通知 Skill 的 Redis 幂等/失败关闭承诺和 common module-map 对 `AutoConfiguration.imports` / `RedisNotifyIdempotencyStore` 的事实仍准确，没有文档承诺“无论自动配置顺序如何均能注入 Store”。因此不为 `afterName` 实现细节额外扩 Skill 路径；在 T42 当前证据中记录红灯的缺 Store 与绿灯的真实装配即可。
