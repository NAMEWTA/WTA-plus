# 总体背景与基础知识：NAMEWTA / WTA-plus 整体架构

## 为什么这个主题重要

把 WTA-plus 想成一座分好房间的大楼，而不是一间什么都往里堆的仓库。

- 改错房间：功能也许能跑，但下一次构建、权限或发布会在别的地方裂开。
- 抄近路：比如 App 直接写 API 类型、Service 直接拿 Mapper、前端先改字段后端后补，短期快，合同会漂。
- 文档会过期：同一件事可能 README、Skill、增强说明各写各的。能干活的人先会查工作树，再记下冲突。

本课的宏观目标：先认房间，再认门牌（依赖方向），再认钥匙（公共合同），最后走一遍真实走廊（一次请求）。

## 宏观地图

```text
                    浏览器终端
          ┌────────────┼────────────┐
          v            v            v
     admin-web     home-web      sso-web
          \            |            /
           \     App 只组装终端      /
            \    Client/布局/路由   /
             v          v          v
        web-domain (Vue 页面/manifest)
                    |
                    v
           domain (无 DOM 的模型/服务)
                    |
                    v
         platform 端口 + adapters 实现
                    |
                    |  HTTP/JSON 合同
                    v
              backend/wta-admin  组装启动
                    |
         +----------+----------+
         v          v          v
      wta-api   wta-modules  wta-common-*
     跨模块合同   业务房间      可复用工具
                    |
                    v
         Mapper XML  +  MySQL 8.4 基座
         release-artifacts/.../mysql/init
```

父仓另外还拥有：

- `.agents/skills/`：项目开发规范与模块事实（不是业务运行时）。
- `docs/`：给人看的架构与产品说明。
- `speculo/`：规格/学习/运维状态，不参与 admin jar 组装。

## 核心概念与关系

| 中文 | English | 一句话 |
| --- | --- | --- |
| 单一仓 | monorepo | 前后端源码合在本仓，不以 submodule 为默认交付 |
| 组装应用 | composition / assembly | `wta-admin` 和前端 App 负责接线，不拥有可复用领域规则 |
| 公开合同 | public contract | HTTP 路径与字段、SQL 基座、OpenAPI transport、跨模块 Java API |
| 五层模块 | layered | Controller/Listener/API Adapter → UseCase → Service → DAO → Mapper → XML |
| 存量模块 | classic | Controller → Service → ServiceImpl → Mapper → XML；只保护已登记模块 |
| 领域包 | domain | 前端无界面模型与服务，对齐后端模块 |
| 表现包 | web-domain | Vue 页面与动态路由 manifest，不拥有 App 全局单例 |
| 终端应用 | App | Client、布局、会话命名空间、选哪些 domain、如何部署 |
| 棘轮 | Ratchet | 新代码按 Target；存量不借机全仓重写 |

关系：

- 前端不 import Java 类型；只消费 HTTP/JSON 和生成的 transport。
- 业务模块不深用另一个模块的 Mapper/内部实体；跨模块走 `wta-api`。
- `wta-common-*` 是工具间，不是“什么业务都往里扔”的筐。
- 登记表是 layered/classic 的唯一名单；未登记新业务模块默认 layered。

## 先决知识与缺口

建议先具备：

- 知道 Git 仓库、目录、HTTP 请求/响应是什么。
- 见过任意一个后端 MVC 或前端组件项目。

本课不要求：

- 会写 Spring 事务或 Vue 动态路由实现。
- 已读完全部 ADR。

评估缺口见 `baseline.md`：没有自评，所以标准路径从地图开始，不从“你已经会”开始。

## 术语表

| 术语 | 工作树位置或裁决者 | 注意 |
| --- | --- | --- |
| NAMEWTA | 产品发行线；公开仓名 WTA-plus | 不是“再复制一份 RuoYi” |
| `org.namewta` | 后端 Java 包名 | 启动类是 `NamewtaApplication` |
| Client | 后端登录域/RBAC 隔离键；前端每个 App 一个 Client | 菜单和 Token 不能跨 App 串用 |
| `bundle-full` / `bundle-core` | `wta-admin` Maven profile | 组装进 fat jar 的业务模块集合不同 |
| `docs/fm/**` | 静态 CRUD 模板 | 运行时代码生成器已删除 |
| `10-cde-base-ddl.sql` / `50-cde-base-dml.sql` | NAMEWTA 自有表结构与初始化数据 | 完整基座，不是无限追加的迁移日志 |

## 来源与不确定性

权威顺序（本课）：

1. 当前工作树（`package.json`、目录、Java 启动类、登记表）。
2. 根 `README.md`、`docs/namewta-enhancements.md`。
3. `.agents/skills/engineering-standards` 的项目画像、模块地图、架构规则。

已核对的冲突（详情 `sources.md`）：

- Skill `ARCH-001` 仍写 submodule 交付；`README.md` / `DEC-004` / 工作树是单一 monorepo。本课按 monorepo 教。
- 项目画像写“只有 admin-web 可构建”；工作树存在 `admin-web`、`home-web`、`sso-web`，且根 `build:prod` 会递归构建带 `build` 脚本的包。
- `frontend/apps/README.md` 与增强说明把发布面写成 admin-web + home-web；根 README 把三个终端都标为已激活。`sso-web` 源码和构建脚本存在，是否算“产品发布面”仍标 unresolved。

类比失效处：大楼比喻不覆盖进程、部署拓扑和运行时配置；不能从“房间”推出“已经掌握通知或 SSO”。
