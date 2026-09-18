---
lesson_id: L-001
objective_ids: [OBJ-01]
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 6
  - segment: deep-explanation
    minutes: 14
  - segment: visuals-and-worked-examples
    minutes: 10
  - segment: pause
    minutes: 4
  - segment: recap-and-transfer
    minutes: 4
expression_level: eli5
coverage_depth: deep
source_ids: [S-001, S-002, S-003, S-006, S-007, S-008, S-010, S-011, S-012, S-013, S-014, S-015, S-016, S-017, S-018]
---

# Lesson 001：这是谁的仓库

## 学完你能做什么

打开仓库根目录时，你能指着五个主房间说清它们各自拥有什么，并且能解释：WTA-plus 的默认交付是**单一仓里的 `frontend/` 和 `backend/`**，不是 git submodule。

你还能画出一张 C4 **系统 Context**：谁是人、谁是机器、谁是外部渠道、NAMEWTA 把数据存在哪。本课覆盖格子是 `C:context` 与 `C:repo-ownership`。本课不讲 Maven 楼层、五层写法、HTTP 字段；那些是 L-002 起的事。

## 先把宏观地图放在桌上

想象你走进一栋标着「NAMEWTA / WTA-plus」的大楼。门牌上写着产品名，但楼里房间不是按“前端程序员 / 后端程序员”随便堆的，而是按**所有权**分的：谁可以改、谁只能引用、谁根本不进运行中的程序。

本课不是凭记忆画这栋楼。2026-09-16 在 `/srv/WTA-plus` 根目录核对过工作树。顶层实际有这些东西（按名字，不含 `.git` 里的历史）：

| 名字 | 类型 | 本课角色 |
| --- | --- | --- |
| `frontend/` | 目录 | 浏览器 App 源码房间 |
| `backend/` | 目录 | Java 服务源码房间 |
| `docs/` | 目录 | 给人看的说明书，不是运行时 |
| `release-artifacts/` | 目录 | 发布时带走的家具（含 MySQL 基座） |
| `.agents/skills/` | 目录 | 施工规范（不进 fat jar） |
| `scripts/` | 目录 | 聚合检查/脚本 |
| `speculo/` | 目录 | 规格与学习状态（不进 admin jar） |
| `README.md` / `AGENTS.md` / `CLAUDE.md` | 文件 | 门牌与施工入口 |
| `.gitignore` | 文件 | Git 忽略规则 |
| `.gitmodules` | **不存在** | 没有子模块指针文件 |

```text
WTA-plus/                         ← 这一栋楼（一个 Git 仓；公开仓名 WTA-plus）
├── frontend/                     ← 浏览器终端的房间
├── backend/                      ← 服务器程序的房间
├── docs/                         ← 给人看的说明书
├── release-artifacts/            ← 发布时带走的家具（含 MySQL 基座）
├── .agents/skills/               ← 施工规范（不是正在跑的服务）
├── scripts/                      ← 聚合检查/脚本
└── speculo/                      ← 规格与学习状态（也不进 admin jar）
```

`frontend/` 和 `backend/` 下面**没有**各自的 `.git`。根上也**没有** `.gitmodules`。这就是“合入 / in-tree”的磁盘证据：源码就在这一仓里，不是父仓只记两个指针。

**类比失效处：** 大楼比喻只帮你记“房间主人”。它不告诉你进程怎么部署、哪台机器跑 MySQL、也不等于你已经会改通知或登录。地图不是能力证明。看见五个房间，不等于已经画出 C4 里的人和外部渠道——那是下一张图。

## 核心概念与机制

### 直觉讲解

小孩子分玩具箱：小汽车进汽车箱，画笔进画笔箱。有人图省事把画笔塞进汽车箱，玩具还能玩，但第二天别人找画笔会发疯。

WTA-plus 也是这样。`frontend/` 不拥有 Java 服务；`backend/` 不拥有 Vue 页面；`docs/` 不拥有可运行代码；`.agents/skills/` 告诉你“该怎么施工”，但 Maven 打包时不会把它塞进 fat jar。

大楼还有门铃。门铃回答的是另一件事：**谁会来按铃**。这就是 C4 的系统 Context。

- 管理操作者按 `admin-web` 这扇门。
- 门户用户按 `home-web` 这扇门。
- 要先被认人的业务应用，被领到 `sso-web`，再走后端 `/sso` 与 `/sso/oauth2`。
- 不会点鼠标的机器，出示 HMAC 签名走 OpenAPI。
- 短信/邮件供应商不会登录后台，它们往 `/notify/callback/{channel}` 回传状态。

楼里放东西的柜子也要认出来：业务行进 MySQL 8.4，会话进 Redis 8，文件对象进 MinIO/OSS。柜子不是“又一个玩具箱主人”，它们是 NAMEWTA 存东西的地方。

还有一个容易晕的词：**monorepo（单一仓）**。意思是前后端源码就在这个 Git 仓库里，克隆一次就能看见两边。有一份旧施工说明书（`ARCH-001`）还在说“前后端是两个 submodule，父仓只记指针”。那是过期门牌。当前工作树和 `DEC-004`、根 README、工程规范入口都说：合入本仓，不以 submodule 为默认交付。

遇到门牌打架时，先看房间里到底有没有墙（工作树），再把冲突记下来，不要默默选听起来更熟的那张。权威顺序是：磁盘上的目录 → 根 `README.md` → `DEC-004` / 工程规范入口 → 过期规则文本只能当冲突记录。

**门铃类比失效处：** “按哪扇门”只帮你记住人和渠道。它不解释 OAuth PKCE、HMAC 规范化字符串、短信幂等，也不等于生产环境已经部署了几个前端 App。Context 图画的是边界，不是函数实现。

### 精确定义与 English term

| 中文 | English | 精确定义（本课） |
| --- | --- | --- |
| 单一仓 | monorepo | 一个 Git 仓库同时保存 `frontend/` 与 `backend/` 源码；默认交付不是 git submodule |
| 所有权 | ownership | 某个目录对一类文件有写入责任；别人只能经公开合同使用，不能把实现复制走 |
| 合入 | in-tree | 源码就在本仓路径下，而不是用 `.gitmodules` 指到另一个仓库 |
| 施工规范 | engineering standards | `.agents/skills/` 里的规则与模块事实；约束人怎么改代码，不是运行时模块 |
| 发布资产 | release artifacts | `release-artifacts/`：Docker/MySQL 初始化等带走就能重建环境的材料 |
| 启动类 | application class | 启动类是 `NamewtaApplication`，包名是 `org.namewta` |
| 系统 Context | C4 Context | 人、机器、外部渠道如何碰到 NAMEWTA 这一整个软件系统；不画模块内部楼层 |
| 容器 | C4 Container | 可单独部署或独立运行的块：三个浏览器 App、`wta-admin`、MySQL、Redis、对象存储。本课只点名，不拆内部 |
| 管理操作者 | admin operator | 用 `frontend/apps/admin-web`（包名 `@namewta/admin-web`）操作后台的人 |
| 门户用户 | portal user | 用 `frontend/apps/home-web`（包名 `@namewta/home-web`）走用户门户的人 |
| SSO 客户端 | SSO client | 认人页是 `frontend/apps/sso-web`；协议入口是后端 `SsoSessionController` 的 `/sso` 与 `SsoOAuthController` 的 `/sso/oauth2` |
| 机器调用 | machine call | 外部程序用 OpenAPI；算法名 `NAMEWTA-HMAC-SHA256`，由 `OpenApiGatewayFilter` 验签，不是浏览器 Cookie 登录 |
| 渠道回调 | provider callback | 短信/邮件供应商 `POST /notify/callback/{channel}`，入口是 `ProviderCallbackController` |
| 自有表基座 | owned-table baseline | NAMEWTA 自己的表结构只进 `10-cde-base-ddl.sql`，初始化数据只进 `50-cde-base-dml.sql`；二者是完整可重建基座，不是无限追加的迁移日志 |

**Monorepo** 的反面在本项目里不是“禁止多包”，而是“禁止把前后端默认拆成两个要单独 clone 的 submodule 来交付”。前端内部仍然是 pnpm workspace，后端仍然是多 Maven 模块——那是楼层，不是另外两栋楼。

**C4 Context** 的反面也不是“禁止画 Container”。Context 先回答“谁在系统外面、怎么碰到系统”；Container 才回答“系统里面有几块可运行的东西”。本课两张图都要会指，但 OBJ-01 的可观察行为仍是：指五个房间 + 说明不是 submodule。Context 是 deep 升级补上的同一系统边界。

### 机制/因果链

1. 你克隆 WTA-plus。一次 `git clone` 就应看到 `frontend/` 与 `backend/`，不需要再 `git submodule update`。
2. 根 README 告诉你：这是 NAMEWTA 增强版后台与多 App 前端的**单一 monorepo**；交付不以 git submodule 或上游 URL 为依赖。
3. 真正能跑起来的程序在 `frontend/`（浏览器 App）和 `backend/`（Spring Boot 组装进 `wta-admin`）。启动类是 `org.namewta.NamewtaApplication`。
4. 人怎么进来：管理操作者 → `admin-web`；门户用户 → `home-web`；认人走 `sso-web`，后端路径是 `/sso` 与 `/sso/oauth2`（安全排除名单里也写了这两条，避免把认人厅当成普通登录 Cookie）。
5. 机器怎么进来：OpenAPI 网关读 HMAC（算法 `NAMEWTA-HMAC-SHA256`），给机器会话，而不是复用浏览器 Token。
6. 外部渠道怎么回来：短信/邮件供应商 `POST /notify/callback/{channel}`，带 `X-Notify-Signature`，只更新投递状态。
7. NAMEWTA 把东西放哪：业务表在 MySQL 8.4（Compose 镜像 `mysql:8.4.9`）；会话/锁/限流在 Redis 8（Compose 镜像 `redis:8.6.3`）；对象在 MinIO/OSS（Compose 服务 `minio`）。
8. 数据库怎么从零长出来，权威在 `release-artifacts/docker/infrastructure/mysql/init/` 的六份 MySQL 8.4 脚本，而不是随便一份后端 `script/` 副本。NAMEWTA **自有表**只合并进 `10-cde-base-ddl.sql` 与 `50-cde-base-dml.sql`。
9. 你改代码时先问：这件事的主人是哪个目录？改错主人，门禁或下次发布会在别处裂开。
10. 文档若和目录打架（例如有人写 submodule、有人写只有一个前端 App），以工作树为准，冲突记进来源，不偷偷改历史。

因果：所有权清楚 → 依赖方向才站得住 → 公共合同才有人维护。L-002 起才拆后端楼层；本课先认楼，并认清楼门外是谁。

五个房间的所有权合同（deep 版，按目录写死）：

| 目录 | 拥有什么 | 明确不拥有 |
| --- | --- | --- |
| `frontend/` | 浏览器 App：工作树存在 `apps/admin-web`、`apps/home-web`、`apps/sso-web`，包名 `@namewta/admin-web` / `@namewta/home-web` / `@namewta/sso-web`；另有 `packages/` 里的 domain / web-domain / platform | Java 服务、MySQL 基座、Skill 正文 |
| `backend/` | Java 21 / Spring Boot 模块化后端：`wta-admin` 组装启动，`wta-api` 跨模块合同，`wta-common-*` 工具，`wta-modules` 业务房间 | Vue 页面、给人读的架构说明书、发布用的六份 SQL 权威副本 |
| `docs/` | 给人看的架构、产品说明、截图、静态 CRUD 模板 `docs/fm/**` | 运行中的代码生成器（已删除）、fat jar 里的业务实现 |
| `release-artifacts/` | Docker Compose、Nginx、发布脚本；MySQL 8.4 初始化基座的**唯一事实源** | 业务模块里再藏一份方言 SQL；`10`/`50` 以外的上游 schema 整治权 |
| `.agents/skills/` | 项目开发 Skill 唯一根；`engineering-standards` 是规范裁决层 | 不进 `wta-admin` fat jar；不是浏览器 HTTP 模块 |
| `speculo/`（相邻房间） | 规格驱动研发与学习状态 | 不参与 admin jar 组装，不能当业务实现 |

`release-artifacts/.../mysql/init/` 六份脚本在磁盘上是：`10-cde-base-ddl.sql`、`20-cde-job.sql`、`30-cde-workflow.sql`、`40-cde-ai.sql`、`50-cde-base-dml.sql`、`60-cde-nacos.sql`。项目画像写死：NAMEWTA 自有表结构只合并到 `10`；初始化数据、菜单和回填只合并到 `50`。`20`/`30`/`40`/`60` 是任务、工作流、AI、Nacos 等上游或配套 schema，不是“再找个地方新建 NAMEWTA 表”的借口。

### 图、表或文本图

**图题 / caption：** C4 系统 Context——人、机器、外部渠道如何碰到 NAMEWTA（alt：NAMEWTA 系统边界外人与外部系统示意图）。

```text
  [管理操作者]                    [门户用户]                 [要被认人的业务应用]
        |                              |                              |
        v                              v                              v
  admin-web 浏览器                 home-web 浏览器              sso-web 认人页
        \                              |                         /sso + /sso/oauth2
         \                             |                        /
          \                            v                       /
           \------------------>  NAMEWTA 软件系统  <----------/
                                 (frontend Apps
                                  + backend/wta-admin)
                                        ^
                                        | NAMEWTA-HMAC-SHA256
                                 [机器调用方 / OpenAPI]
                                        ^
                                        | POST /notify/callback/{channel}
                                 [短信 / 邮件供应商]

          NAMEWTA 自己的柜子（系统边界内的存储，不是又一套业务源码）
             MySQL 8.4 业务表     Redis 8 会话/锁/限流     MinIO/OSS 对象
```

**文字等价物：** 把 NAMEWTA 看成一栋会应答的大楼。三种人走浏览器门：管理员进 `admin-web`，门户用户进 `home-web`，需要先被认人的应用被领到 `sso-web`，后端对应 `/sso` 和 `/sso/oauth2`。机器不点页面，它们带 HMAC 签名走 OpenAPI。短信和邮件供应商也不登录，它们只往 `/notify/callback/{channel}` 回报送达状态。大楼把业务行写进 MySQL 8.4，把会话放进 Redis 8，把文件放进 MinIO 或云 OSS。箭头表示“谁碰到系统”，不表示这些东西编译成同一个文件，也不表示生产环境已经部署了几个 App。

**图的边界：** 这是 C4 Context，不是 Container 平面图。它不拆 `wta-api` / `wta-modules` / domain 包，不画 HMAC 规范化行，不画 Outbox，也不断言 Nacos 一定开着（Nacos 在 Compose 里是可选 profile）。未知：本机未验证生产部署拓扑。

**图题 / caption：** 仓库根所有权地图（按“谁拥有什么”，不是按编程语言）。

```text
                 克隆 WTA-plus 这一仓
                           |
           +---------------+---------------+
           |               |               |
      给人看的纸         能跑的程序         带走就能重建
           |               |               |
        docs/         frontend/          release-artifacts/
     .agents/skills/  backend/           （含 MySQL 8.4 基座）
      speculo/        scripts/ci
           |               |
           |          浏览器 App 在 frontend/apps/*
           |          服务组装在 backend/wta-admin
           v               v
     不进 fat jar      进构建与部署
```

**文字等价物：** 仓库根部分成三类主人。第一类是说明书和规范：`docs/` 给人读架构，`.agents/skills/` 约束怎么改代码，`speculo/` 存规格与学习状态，它们都不应该被当成 `wta-admin` 的业务实现。第二类是能构建的程序：`frontend/` 是浏览器终端，`backend/` 是 Java 服务。第三类是发布时要带走的基座：`release-artifacts/` 里的 MySQL 初始化脚本是空环境怎么长出来的权威；其中 NAMEWTA 自有表只认 `10-cde-base-ddl.sql` 与 `50-cde-base-dml.sql`。箭头的意思是“克隆一次就看到这些房间”，不是“它们编译成同一个文件”。

**图的边界：** 这张图不画出 pnpm 包、Maven 模块、HTTP 路径。那些是后面几课的楼层平面图。它也不断言生产环境已经部署了几个前端 App。工作树三个 App 包都存在且带 `build` 脚本；根 README 把三个终端标为已激活；`frontend/apps/README.md` 与增强说明常把发布面写成 admin-web + home-web。本课把“源码和构建脚本存在”当成事实，把“是否等于产品发布面”标为未关闭冲突（C-003）。

可选产品图（链接失效也不影响本课）：根 README 的架构一览图 `docs/image/architecture-overview.png`。caption：终端 App、前端复用层、后端合同与数据基座。source：仓库内 `docs/image/architecture-overview.png`，访问日期 2026-09-16。完整的文字替代见上面两张 ASCII 图。

### 正例、反例与边界

**正例 1：** 要改管理端登录页文案。主人在 `frontend/apps/admin-web/`（或它组合的 web-domain），不在 `backend/`。对应 Context：这是管理操作者那扇门上的牌子。

**正例 2：** 要改“测试单表”列表 SQL。主人在 `backend/wta-modules/wta-demo/` 的 Mapper/XML，不在 `docs/`。对应存储：结果行最终进 MySQL 8.4，DDL 权威仍在 `10-cde-base-ddl.sql`。

**正例 3：** 要新增一张 NAMEWTA 自有表。DDL 合并进 `release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql`，初始化数据进 `50-cde-base-dml.sql`，而不是在后端模块里再藏一份方言脚本。

**正例 4：** 机器要调一个已登记的 OpenAPI 接口。它走 HMAC（`NAMEWTA-HMAC-SHA256`），不把浏览器 `Authorization` Cookie 借给脚本。入口在后端 OpenAPI 网关，不在 `docs/`。

**正例 5：** 短信供应商回报“已送达”。请求打到 `POST /notify/callback/{channel}`，由 `ProviderCallbackController` 验 `X-Notify-Signature`。这是外部渠道，不是管理操作者。

**反例 1：** 把 Vue 页面复制进 `backend/src/main/resources/static` 当“一起发布更简单”。这绕过前端 App 所有权，动态路由和 Client 隔离会丢。

**反例 2：** 把 Skill 里的 `ARCH-001`（submodule 交付）当成今天的施工合同，去建 `.gitmodules` 当默认流程。工作树已经是合入的 `frontend/`、`backend/`；根上没有 `.gitmodules`，两边也不是嵌套 Git 仓。

**反例 3：** 从旧组织名推断新产品边界或新建包路径。启动类是 `NamewtaApplication`，包名是 `org.namewta`。

**反例 4：** 把 `.agents/skills/` 或 `speculo/` 打进 admin fat jar，以为“规范也要跟着服务走”。规范约束人；jar 里跑的是 Java。`scripts/ci/verify-admin-bundle.sh` 检查的是模块集合，不是 Skill 正文。

**反例 5：** 在 `20-cde-job.sql` 或某个模块 `script.sql` 里新建 NAMEWTA 业务表。自有表基座只有 `10` 与 `50`。

**边界：** “合入”不等于“前后端可以互相 import 源码”。它们仍然独立构建，只通过 HTTP/JSON 说话。L-005 再讲合同。Context 图画了 Redis / MinIO，也不等于本课已经教会话丢失或 OSS 直传失败路径（那些是后面的数据流课）。

## 变式与迁移

- **变式 A：** 有人问“父仓是不是只放文档？”——旧 submodule 心智是那样。现在父仓就是产品仓，文档和源码在一起。反驳材料三件套：根 README 写“单一 monorepo / 不是 git submodule”；`DEC-004` 写不以 submodule 为默认交付；你磁盘上的 `frontend/`、`backend/`，以及**没有** `.gitmodules`。
- **变式 B：** 你只想改规范文字。走 `.agents/skills/`，不要为了“方便”把规则复制进某个 App 的 README 当第二份真相。项目 Skill 唯一根是 `.agents/skills/`，本仓不维护 `.claude` / `.codex` 副本。
- **变式 C：** 有人问“这系统有几类客人？”——先画 Context，不要先翻 Mapper。人：管理操作者 / 门户用户 / SSO 认人。机器：OpenAPI HMAC。外部渠道：短信邮件回调。柜子：MySQL 8.4、Redis 8、MinIO/OSS。
- **变式 D：** `scripts/ci/verify-submodules.sh` 还在树上。`scripts/README.md` 写明它是聚合仓时期的遗留校验，**不是**克隆/启动步骤。看见旧脚本不等于现行交付仍是 submodule。
- **迁移到改代码：** 接到任务先在根目录指房间，再进 L-002 的后端楼层或 L-004 的前端楼层。指错房间，后面的层都会跟错。若任务是“空库怎么长出 NAMEWTA 自己的表”，先打开 `release-artifacts/.../10-cde-base-ddl.sql` 与 `50-cde-base-dml.sql`。
- **文档打架怎么迁：** 先打开工作树（有没有这个目录、有没有 `package.json`、有没有 `.gitmodules`），再打开 README / Skill。本课冲突 C-001：Skill 的 `ARCH-001` 仍写 submodule，现行裁决是 monorepo。C-002：项目画像曾写“唯一可构建 App 是 admin-web”；工作树三个 App 都有 build。C-003：发布面文档常写 admin-web + home-web，根 README 把 sso-web 标为已激活——源码存在是事实，发布面未在本课关闭。

## 常见误区

1. **“一个仓 = 一个可部署程序。”** 错。一个仓里有多个 Maven 模块、多个前端 App、还有根本不部署的规范目录。
2. **“docs 和 skills 也是业务模块。”** 错。它们约束人，不给浏览器提供 HTTP。
3. **“看见历史名字就跟着旧上游走。”** 错。以当前路径和包名为准。
4. **“门牌和房间冲突时，听更长的那份文档。”** 错。先看工作树，再记录冲突。
5. **“C4 Context 就是再画一张仓库树。”** 错。仓库树回答所有权；Context 回答人/机器/外部渠道如何碰到系统。两张图都要，不能互相替代。
6. **“合入了就可以 frontend import Java。”** 错。合入只改变 Git 拓扑，不改变构建边界。
7. **“Redis / MinIO 也是源码房间。”** 错。它们是存储容器。源码房间仍是 `frontend/` 与 `backend/`；Compose 定义在 `release-artifacts/docker/`。
8. **“六份 SQL 都可以往里面加 NAMEWTA 新表。”** 错。自有表只进 `10` 和 `50`。

## 非评分暂停

想一想（不必写下来，本课不给标准答卷）：如果你要改“空数据库怎么创建 NAMEWTA 自己的表”，你会先打开哪个目录？为什么不是 `backend/wta-modules/` 里随手新建 `script.sql`？

再想：如果有人发给你一份还在说 submodule 的检查清单，你用哪三样东西反驳它？（提示：根 README、`DEC-004`、你磁盘上的 `frontend/` 与 `backend/` 目录，外加“根上没有 `.gitmodules`”。）

再想：管理操作者、门户用户、机器调用方、短信供应商，他们分别敲哪扇门？哪一扇门**不是** `admin-web`？

## 总结、词汇表与下一步

- 五个要能指出来的房间：`frontend/`、`backend/`、`docs/`、`release-artifacts/`、`.agents/skills/`。2026-09-16 工作树确认它们都在 `/srv/WTA-plus` 根下；另有 `scripts/` 与 `speculo/`。
- 默认交付是单一仓合入，不是 git submodule。权威是工作树（无 `.gitmodules`、前后端非嵌套 Git）+ 根 README + `DEC-004`。`ARCH-001` 是过期文本。
- `frontend/` = 浏览器 App；`backend/` = Java；`docs/` = 给人看的文档，不是运行时；`release-artifacts/` 里 NAMEWTA 自有表基座是 `10-cde-base-ddl.sql` 与 `50-cde-base-dml.sql`；`.agents/skills/` = 工程规范，不进 fat jar；`speculo/` = 规格/学习状态，不进 admin jar。
- C4 Context：管理操作者 → `admin-web`；门户用户 → `home-web`；SSO 客户端 → `sso-web` + `/sso` + `/sso/oauth2`；机器 → OpenAPI HMAC；短信/邮件供应商 → `POST /notify/callback/{channel}`。NAMEWTA 存业务于 MySQL 8.4，存会话于 Redis 8，存对象于 MinIO/OSS。
- 规范目录不进运行中的 admin 程序；发布用的 MySQL 基座不在业务模块里各藏一份。
- 文档冲突：工作树优先，冲突本身也是知识，不要假装没看见。

下一步：L-002 走进 `backend/`，看 admin / api / common / modules 谁组装、谁订合同、谁干活、谁当工具间。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-001 | `README.md` | 单一 monorepo；frontend/backend 合入；根目录树；三终端已激活；MySQL 8.4 基座路径 | 开篇、「架构一览」、「获取与构建」 | 2026-09-16 |
| S-002 | `docs/namewta-enhancements.md` | 目录所有权以当前实现为准；发布面常写 admin-web + home-web（C-003） | 开篇、「多 App 领域化 monorepo」 | 2026-09-16 |
| S-003 | `.agents/skills/engineering-standards/references/project/00-project-profile.md` | 拓扑为单一 monorepo；`10`/`50` 自有表基座；排除区；SQL 唯一事实源 | 「身份」「排除与冻结」 | 2026-09-16 |
| S-006 | `.agents/skills/engineering-standards/references/rules/architecture-and-boundaries.md` | ARCH-001 仍写 submodule（过期文本，C-001） | ARCH-001 | 2026-09-16 |
| S-007 | `.agents/skills/engineering-standards/references/project/02-decisions-and-exceptions.md` | DEC-004 不以 submodule 为默认交付 | DEC-004 | 2026-09-16 |
| S-008 | `frontend/package.json` 与 `frontend/apps/{admin-web,home-web,sso-web}/package.json` | 三个 App 包真实存在于本仓 `frontend/`，均带 build 脚本 | 工作树 | 2026-09-16 |
| S-010 | `backend/wta-admin/src/main/java/org/namewta/NamewtaApplication.java` | 启动类 `NamewtaApplication` 与 `org.namewta` 包名 | 类声明 | 2026-09-16 |
| S-011 | `AGENTS.md` | 项目 Skill 唯一根 `.agents/skills/` | PROJECT_SKILLS | 2026-09-16 |
| S-012 | `release-artifacts/docker/docker-compose-infrastructure.yml` | MySQL 8.4.9、Redis 8.6.3、MinIO 服务定义 | `services.mysql` / `redis` / `minio` | 2026-09-16 |
| S-013 | `release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql` 与 `50-cde-base-dml.sql` | 六份脚本存在；自有表基座文件名 | 工作树该目录 listing | 2026-09-16 |
| S-014 | `SsoSessionController.java`、`SsoOAuthController.java`；`application.yml` `security.excludes` | SSO 客户端入口 `/sso` 与 `/sso/oauth2` | `@RequestMapping`；excludes 列表 | 2026-09-16 |
| S-015 | `backend/wta-modules/wta-notify/.../ProviderCallbackController.java` | 供应商回调 `POST /notify/callback/{channel}` | 类注释与 `@PostMapping("/{channel}")` | 2026-09-16 |
| S-016 | `OpenApiCanonicalizer.java`、`OpenApiGatewayFilter.java` | 机器调用算法 `NAMEWTA-HMAC-SHA256` | `ALGORITHM` 常量；网关 Filter | 2026-09-16 |
| S-017 | `/srv/WTA-plus` 工作树 | 顶层目录名单；无 `.gitmodules`；`frontend/`、`backend/` 非嵌套 Git | `ls` / 目录探测 | 2026-09-16 |
| S-018 | `.agents/skills/engineering-standards/SKILL.md`；`scripts/README.md` | 合入 `backend/` 与 `frontend/`，不以 submodule 交付；`verify-submodules.sh` 为遗留脚本 | 「约束」；`ci/verify-submodules.sh` | 2026-09-16 |
