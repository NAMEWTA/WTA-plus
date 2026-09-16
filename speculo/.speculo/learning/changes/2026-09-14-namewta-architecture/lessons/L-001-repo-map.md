---
lesson_id: L-001
objective_ids: [OBJ-01]
estimated_minutes: 35
time_budget:
  - segment: orientation-and-map
    minutes: 5
  - segment: deep-explanation
    minutes: 12
  - segment: visuals-and-worked-examples
    minutes: 8
  - segment: pause
    minutes: 5
  - segment: recap-and-transfer
    minutes: 5
expression_level: eli5
coverage_depth: standard
source_ids: [S-001, S-002, S-003, S-006, S-007, S-008, S-010, S-011]
---

# Lesson 001：这是谁的仓库

## 学完你能做什么

打开仓库根目录时，你能指着五个主房间说清它们各自拥有什么，并且能解释：WTA-plus 的默认交付是**单一仓里的 `frontend/` 和 `backend/`**，不是 git submodule。

## 先把宏观地图放在桌上

想象你走进一栋标着「NAMEWTA / WTA-plus」的大楼。门牌上写着产品名，但楼里房间不是按“前端程序员 / 后端程序员”随便堆的，而是按**所有权**分的：谁可以改、谁只能引用、谁根本不进运行中的程序。

```text
WTA-plus/                         ← 这一栋楼（一个 Git 仓）
├── frontend/                     ← 浏览器终端的房间
├── backend/                      ← 服务器程序的房间
├── docs/                         ← 给人看的说明书
├── release-artifacts/            ← 发布时带走的家具（含 MySQL 基座）
├── .agents/skills/               ← 施工规范（不是正在跑的服务）
├── scripts/                      ← 聚合检查/脚本
└── speculo/                      ← 规格与学习状态（也不进 admin jar）
```

**类比失效处：** 大楼比喻只帮你记“房间主人”。它不告诉你进程怎么部署、哪台机器跑 MySQL、也不等于你已经会改通知或登录。地图不是能力证明。

## 核心概念与机制

### 直觉讲解

小孩子分玩具箱：小汽车进汽车箱，画笔进画笔箱。有人图省事把画笔塞进汽车箱，玩具还能玩，但第二天别人找画笔会发疯。

WTA-plus 也是这样。`frontend/` 不拥有 Java 服务；`backend/` 不拥有 Vue 页面；`docs/` 不拥有可运行代码；`.agents/skills/` 告诉你“该怎么施工”，但 Maven 打包时不会把它塞进 fat jar。

还有一个容易晕的词：**monorepo（单一仓）**。意思是前后端源码就在这个 Git 仓库里，克隆一次就能看见两边。有一份旧施工说明书（`ARCH-001`）还在说“前后端是两个 submodule，父仓只记指针”。那是过期门牌。当前工作树和 `DEC-004`、根 README 都说：合入本仓，不以 submodule 为默认交付。

遇到门牌打架时，先看房间里到底有没有墙（工作树），再把冲突记下来，不要默默选听起来更熟的那张。

### 精确定义与 English term

| 中文 | English | 精确定义（本课） |
| --- | --- | --- |
| 单一仓 | monorepo | 一个 Git 仓库同时保存 `frontend/` 与 `backend/` 源码；默认交付不是 git submodule |
| 所有权 | ownership | 某个目录对一类文件有写入责任；别人只能经公开合同使用，不能把实现复制走 |
| 合入 | in-tree | 源码就在本仓路径下，而不是用 `.gitmodules` 指到另一个仓库 |
| 施工规范 | engineering standards | `.agents/skills/` 里的规则与模块事实；约束人怎么改代码，不是运行时模块 |
| 发布资产 | release artifacts | `release-artifacts/`：Docker/MySQL 初始化等带走就能重建环境的材料 |
| 启动类 | application class | 启动类是 `NamewtaApplication`，包名是 `org.namewta` |

**Monorepo** 的反面在本项目里不是“禁止多包”，而是“禁止把前后端默认拆成两个要单独 clone 的 submodule 来交付”。前端内部仍然是 pnpm workspace，后端仍然是多 Maven 模块——那是楼层，不是另外两栋楼。

### 机制/因果链

1. 你克隆 WTA-plus。
2. 根 README 告诉你：这是 NAMEWTA 增强版后台与多 App 前端的**单一 monorepo**。
3. 真正能跑起来的程序在 `frontend/`（浏览器 App）和 `backend/`（Spring Boot 组装进 `wta-admin`）。
4. 数据库怎么从零长出来，权威在 `release-artifacts/docker/infrastructure/mysql/init/` 的六份 MySQL 8.4 脚本，而不是随便一份后端 `script/` 副本。
5. 你改代码时先问：这件事的主人是哪个目录？改错主人，门禁或下次发布会在别处裂开。
6. 文档若和目录打架（例如有人写 submodule、有人写只有一个前端 App），以工作树为准，冲突记进来源，不偷偷改历史。

因果：所有权清楚 → 依赖方向才站得住 → 公共合同才有人维护。L-002 起才拆后端楼层；本课先认楼。

### 图、表或文本图

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

**文字等价物：** 仓库根部分成三类主人。第一类是说明书和规范：`docs/` 给人读架构，`.agents/skills/` 约束怎么改代码，`speculo/` 存规格与学习状态，它们都不应该被当成 `wta-admin` 的业务实现。第二类是能构建的程序：`frontend/` 是浏览器终端，`backend/` 是 Java 服务。第三类是发布时要带走的基座：`release-artifacts/` 里的 MySQL 初始化脚本是空环境怎么长出来的权威。箭头的意思是“克隆一次就看到这些房间”，不是“它们编译成同一个文件”。

**图的边界：** 这张图不画出 pnpm 包、Maven 模块、HTTP 路径。那些是后面几课的楼层平面图。它也不断言生产环境已经部署了几个前端 App。

### 正例、反例与边界

**正例 1：** 要改管理端登录页文案。主人在 `frontend/apps/admin-web/`（或它组合的 web-domain），不在 `backend/`。

**正例 2：** 要改“测试单表”列表 SQL。主人在 `backend/wta-modules/wta-demo/` 的 Mapper/XML，不在 `docs/`。

**正例 3：** 要新增一张 NAMEWTA 自有表。DDL 合并进 `release-artifacts/.../10-cde-base-ddl.sql`，初始化数据进 `50-cde-base-dml.sql`，而不是在后端模块里再藏一份方言脚本。

**反例 1：** 把 Vue 页面复制进 `backend/src/main/resources/static` 当“一起发布更简单”。这绕过前端 App 所有权，动态路由和 Client 隔离会丢。

**反例 2：** 把 Skill 里的 `ARCH-001`（submodule 交付）当成今天的施工合同，去建 `.gitmodules` 当默认流程。工作树已经是合入的 `frontend/`、`backend/`。

**反例 3：** 从旧组织名推断新产品边界或新建包路径。启动类是 `NamewtaApplication`，包名是 `org.namewta`。

**边界：** “合入”不等于“前后端可以互相 import 源码”。它们仍然独立构建，只通过 HTTP/JSON 说话。L-005 再讲合同。

## 变式与迁移

- **变式 A：** 有人问“父仓是不是只放文档？”——旧 submodule 心智是那样。现在父仓就是产品仓，文档和源码在一起。
- **变式 B：** 你只想改规范文字。走 `.agents/skills/`，不要为了“方便”把规则复制进某个 App 的 README 当第二份真相。
- **迁移到改代码：** 接到任务先在根目录指房间，再进 L-002 的后端楼层或 L-004 的前端楼层。指错房间，后面的层都会跟错。
- **文档打架怎么迁：** 先打开工作树（有没有这个目录、有没有 `package.json`），再打开 README / Skill。本课冲突 C-001：Skill 仍写 submodule，现行裁决是 monorepo。

## 常见误区

1. **“一个仓 = 一个可部署程序。”** 错。一个仓里有多个 Maven 模块、多个前端 App、还有根本不部署的规范目录。
2. **“docs 和 skills 也是业务模块。”** 错。它们约束人，不给浏览器提供 HTTP。
3. **“看见历史名字就跟着旧上游走。”** 错。以当前路径和包名为准。
4. **“门牌和房间冲突时，听更长的那份文档。”** 错。先看工作树，再记录冲突。

## 非评分暂停

想一想（不必写下来，本课不给标准答卷）：如果你要改“空数据库怎么创建 NAMEWTA 自己的表”，你会先打开哪个目录？为什么不是 `backend/wta-modules/` 里随手新建 `script.sql`？

再想：如果有人发给你一份还在说 submodule 的检查清单，你用哪三样东西反驳它？（提示：根 README、`DEC-004`、你磁盘上的 `frontend/` 与 `backend/` 目录。）

## 总结、词汇表与下一步

- 五个要能指出来的房间：`frontend/`、`backend/`、`docs/`、`release-artifacts/`、`.agents/skills/`。
- 默认交付是单一仓合入，不是 git submodule。
- 规范目录不进运行中的 admin 程序；发布用的 MySQL 基座不在业务模块里各藏一份。
- 文档冲突：工作树优先，冲突本身也是知识，不要假装没看见。

下一步：L-002 走进 `backend/`，看 admin / api / common / modules 谁组装、谁订合同、谁干活、谁当工具间。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-001 | `README.md` | 单一 monorepo；frontend/backend 合入；根目录树 | 开篇与「架构一览」 | 2026-09-14 |
| S-002 | `docs/namewta-enhancements.md` | 目录所有权以当前实现为准 | 开篇 | 2026-09-14 |
| S-003 | `00-project-profile.md` | 拓扑为单一 monorepo；排除区与 SQL 基座位置 | 「身份」「排除与冻结」 | 2026-09-14 |
| S-006 | `architecture-and-boundaries.md` | ARCH-001 仍写 submodule（过期文本，C-001） | ARCH-001 | 2026-09-14 |
| S-007 | `02-decisions-and-exceptions.md` | DEC-004 不以 submodule 为默认交付 | DEC-004 | 2026-09-14 |
| S-008 | `frontend/package.json` 与三个 App 包 | 前端程序真实存在于本仓 `frontend/` | 工作树 | 2026-09-14 |
| S-010 | `NamewtaApplication.java` | 启动类 `NamewtaApplication` 与 `org.namewta` 包名 | 类声明 | 2026-09-14 |
| S-011 | `AGENTS.md` | 项目 Skill 唯一根 `.agents/skills/` | PROJECT_SKILLS | 2026-09-14 |
