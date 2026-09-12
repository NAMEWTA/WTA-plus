---
schema_version: 1
artifact: source
change: 2026-09-12-rename-ruoyi-dromara-to-wta
source_type: conversation
canonical_locator: null
captured_at: 2026-09-12T10:29:50+08:00
content_sha256: null
remote_state: not-applicable
close_capability: not-applicable
---

# Source: ruoyi / dromara → wta 重命名纲领

## Capture Metadata

- **Capture method:** conversation（CTO / 用户意图转 SpecDev）
- **Author:** CTO ask via secretary/RVP Lead
- **Created:** 2026-09-12T10:29:50+08:00
- **Labels:** 激活 SpecDev change 创建；仅持久化工件；禁止 Cloud Agent；禁止触碰 CTO 机器；禁止本轮 I-implement
- **Attachments:** 要求镜像既有 change `2026-09-10-notify-channel-config` 结构；调研写入 LOG/evidence

## Original Content（意图摘要）

优化/升级 RVP 项目命名：

1. 所有**本项目自有**与本 ruoyi-vue-plus 工程相关的 `ruoyi` 命名 → `wta`
2. 属于本项目的 `dromara` 包路径 / Maven group 坐标 → **`namewta` family**（实现上定为 **`org.namewta`**，见 ADR-002 / LOG-020；**SUPERSEDES** 曾用 `org.wta`）
3. **不得**重命名第三方/引入包（例如 **sms4j** 及同类）：保持其 Maven 坐标、包名、import 与文档中的上游专名

工作范围：仅在 box 上 `/workspace/vp-dev/ruoyi-vue-plus-docs` 写 SpecDev 持久化工件。

## Source Comments

- 同仓已有活跃/历史 change `2026-09-10-notify-channel-config`；主题无关，本请求新建独立 change。
- 前端大量包已是 `@namewta/*`，Docker 服务名已是 `namewta-*`；后端 Maven/Java 仍大量 `org.dromara` + `ruoyi-*`。
- 本期创建 change = draft/speccing；**I-implement 授权前不得改产品代码**。

## Survey commands used

在 `/workspace/vp-dev/ruoyi-vue-plus-docs` 下（已排除 node_modules/.git/target/dist）：

```bash
# 根 POM 坐标与第三方 dromara 生态
rg -n 'groupId|artifactId|org\.dromara' ruoyi-vue-plus-namewta/pom.xml

# 自有模块目录
find ruoyi-vue-plus-namewta -type d -name 'ruoyi-*'

# 自有 Java 包树
find ruoyi-vue-plus-namewta -type d -path '*/java/org/dromara/*'
find ruoyi-vue-plus-namewta -path '*/java/org/dromara/*' -name '*.java' | wc -l

# 第三方 import 抽样
rg -n 'import org\.dromara\.sms4j' -g '*.java' ruoyi-vue-plus-namewta
rg -n 'import org\.dromara\.warm' -g '*.java' ruoyi-vue-plus-namewta
rg -n 'import org\.dromara\.(easyes|mica)' -g '*.java' ruoyi-vue-plus-namewta

# 文件触达约数
rg -l -i 'ruoyi' ruoyi-vue-plus-namewta plus-ui-namewta docs release-artifacts ... | wc -l
rg -l 'org\.dromara' ... | wc -l
rg -l 'sms4j|org\.dromara\.sms4j' ... | wc -l

# 前端 / Docker / 文档
rg -n '"name"|@namewta|RuoYi' plus-ui-namewta/package.json plus-ui-namewta/apps/*/.env*
rg -n 'namewta|/ruoyi|ruoyi-' release-artifacts/docker/docker-compose*.yml
cat .gitmodules
```

详细表见 `<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/evidence/SURVEY.md</Path>`。
