# 三类代码归属与 ruoyi-ai 的真实身份

## 先给直接答案

不是“把所有东西都写出来，然后 SnailAI 自动生成对应业务接口”。

```text
SnailAI 已经提供：
[Agent 管理] [模型管理] [Skill 上传和读取] [会话] [聊天] [任务调度]

SnailAI 没有提供：
[个人档案查询] [个人档案新增] [个人档案删除] [你的业务权限规则]
```

个人档案的表、查询、新增、删除、权限和审计都要由本项目自己实现。SnailAI 只负责让模型决定何时调用你写好的 Tool，并把调用分发到运行 `ruoyi-ai` 的 RuoYi 主应用。

## 先看全图

这套系统实际是两个独立运行的 Java 程序，不是一个程序里的两个文件夹。

```text
程序一：ruoyi-snailai-server，端口 8900 / gRPC 18888
┌─────────────────────────────────────────────┐
│ 管 Agent、模型、提示词、Skill、会话和聊天   │
│ 接收用户对话，决定交给哪个 Agent Client      │
└─────────────────────┬───────────────────────┘
                      │ gRPC 分发对话
                      v
程序二：ruoyi-admin，包含 ruoyi-ai
┌─────────────────────────────────────────────┐
│ SnailAI Agent Client 和模型执行器            │
│                                             │
│ [ruoyi-ai 的 @Tool]                          │
│       |                                     │
│       v                                     │
│ [个人档案业务 Service] -> [个人档案数据库]   │
└─────────────────────────────────────────────┘
```

`ruoyi-ai` 的身份是：**被打包进 `ruoyi-admin` 的 AI 业务适配模块**。

它不是独立服务，不负责管理 SnailAI 平台，也不是个人档案数据库模块。它应该成为“SnailAI 与本项目业务 Service 之间的转换层”。

## 哪些必须自己写

| 要做的东西 | 谁提供 | 写在哪里 | 运行在哪里 |
| --- | --- | --- | --- |
| 个人档案表、实体、Mapper | 本项目自己写 | `ruoyi-person-archive` | `ruoyi-admin` |
| 查询、新增、删除 Service | 本项目自己写 | `ruoyi-person-archive` | `ruoyi-admin` |
| 普通网页 CRUD 接口 | 本项目自己写 | `ruoyi-person-archive/controller` | `ruoyi-admin` |
| 跨模块查询/命令合同 | 本项目自己写 | `ruoyi-api` | `ruoyi-admin` |
| `person_archive_search` 等 `@Tool` | 本项目自己写 | `ruoyi-ai/tool/archive` | `ruoyi-admin` |
| 个人档案 Skill 内容 | 本项目自己写 | 仓库保存源文件，再上传 SnailAI | SnailAI 保存，模型按需读取 |
| 个人档案系统提示词 | 本项目自己配置 | SnailAI 的 Agent 配置页面 | SnailAI 保存并下发 |
| Agent、Skill、会话、聊天管理接口 | SnailAI 已提供 | 来自 `snail-ai-starter` | `ruoyi-snailai-server` |
| Tool 自动发现和模型调用机制 | SnailAI 已提供 | 来自 Agent Executor starter | `ruoyi-admin` |

因此，“SnailAI 有对应接口”要拆成两种情况：

```text
平台能力：
上传 Skill、创建 Agent、配置提示词、发起聊天
       -> SnailAI 已有接口

业务能力：
查张三档案、新增档案、删除档案
       -> SnailAI 没有，必须自己写
```

## 一步一步看

### 1. 当前 ruoyi-ai 已经做了什么

当前目录里只有一个真正的 Java 源文件：

```text
ruoyi-modules/ruoyi-ai/
|
+-- pom.xml
+-- src/main/java/org/dromara/ai/controller/
    +-- SnailAiController.java
```

这个 Controller 只提供：

```text
POST /snail-ai/user/register
        |
        +-- 从 LoginHelper 取得当前 RuoYi 用户
        +-- 把 RuoYi userId 当作 externalId
        +-- 调 SnailAI OpenAPI 注册用户
        v
返回 SnailAI OpenAPI 用户信息
```

所以当前 `ruoyi-ai` 更准确的状态是：**一个很薄的 SnailAI 接入桥梁，还没有形成个人档案 AI 业务能力。**

`ruoyi-ai` 的 POM 又依赖了 `ruoyi-common-ai`。后者开启三个第三方 starter：

```text
[聊天界面 starter]
[Agent 执行器 starter]
[OpenAPI 客户端 starter]
          |
          v
      [ruoyi-admin]
```

这就是为什么将来可以在 `ruoyi-ai` 中写 `@Component + @Tool`：Agent 执行器会扫描 `ruoyi-admin` Spring 容器里的 Tool Bean。

### 2. 写一个 Tool 后，SnailAI 怎样调用

Tool 不是 SnailAI Server 自动生成的 HTTP 接口。它是 `ruoyi-admin` 进程里的一个 Java 方法。

```text
[用户发消息]
      |
      v
[SnailAI Server 接收聊天]
      |
      | gRPC 分发
      v
[ruoyi-admin 的 Agent 执行器]
      |
      | 把全部发现的 @Tool 告诉模型
      v
[模型选择 person_archive_search]
      |
      | 在本进程直接调用 Java 方法
      v
[PersonArchiveQueryTool.search]
      |
      v
[PersonArchiveQueryService]
      |
      v
[数据库结果返回模型]
```

代码放置示意：

```text
ruoyi-modules/ruoyi-ai/src/main/java/org/dromara/ai/
|
+-- controller/
|   +-- SnailAiController.java
|
+-- tool/archive/
|   +-- PersonArchiveQueryTool.java
|   +-- PersonArchiveOperationTool.java
|
+-- service/
    +-- PersonArchiveAiContextService.java
```

Tool 只负责把模型参数转换成业务调用。例如：

```java
@Component
@RequiredArgsConstructor
public class PersonArchiveQueryTool {

    private final PersonArchiveQueryService personArchiveQueryService;

    @Tool(
        name = "person_archive_search",
        description = "按姓名搜索有权查看的个人档案候选"
    )
    public PersonCandidateResult search(String name) {
        return personArchiveQueryService.searchByName(name);
    }
}
```

这只是形状示例。真实实现还必须传入可信用户和客户端权限，不能只凭模型给出的姓名直接查全库。

### 3. Skill 写在哪里，又怎样生效

Skill 也不会生成 Tool。它只是告诉模型 Tool 的正确使用顺序。

```text
[自己编写 SKILL.md]
      |
      | 压缩成 zip
      v
[SnailAI Skill 管理页面上传]
      |
      v
[绑定到个人档案 Agent]
      |
      v
[模型对话时调用 read_skill]
      |
      v
[模型知道先 search，再 get_context]
```

推荐把源文件留在项目中：

```text
ruoyi-modules/ruoyi-ai/skills/person-archive/
|
+-- SKILL.md
+-- references/
    +-- tool-contracts.md
    +-- examples.md
```

这里的目录只是为了版本管理。当前代码没有自动把该目录发布到 SnailAI 的机制，所以仍需要手动上传，或者以后自己做一条受控的发布脚本。

### 4. 系统提示词写在哪里

个人档案 Agent 的系统提示词通常直接配置在 SnailAI 管理端的 Agent 配置中：

```text
[SnailAI 管理端]
      |
      +-- Agent 名称：个人档案助手
      +-- 系统提示词：长期角色和禁止事项
      +-- 绑定 Skill：person-archive-operations
      +-- 绑定模型：指定大模型
      v
[保存后，SnailAI 在每次对话中下发]
```

不建议把整段系统提示词硬编码在 `PersonArchiveQueryTool.java` 中，因为 Tool 只负责一次具体动作。

### 5. 是否要修改 ruoyi-snailai-server

普通个人档案功能不需要把业务代码写进 `ruoyi-snailai-server`：

```text
个人档案 CRUD          -> 不写进 server
个人档案 @Tool         -> 不写进 server
个人档案 Service       -> 不写进 server
个人档案 Skill 正文    -> 上传 server 管理，但源文件不必写进 server 源码
个人档案系统提示词      -> 配置在 server 管理的 Agent 中
```

只有要修改 SnailAI 的“平台级行为”时，才可能改或扩展 `ruoyi-snailai-server`，例如：

- 给 gRPC 对话分发合同增加可信的 RuoYi 用户身份。
- 增加每个 Agent 的自定义 Tool 白名单。
- 改造 SnailAI 自身的 Skill、Agent、会话或 OpenAPI 管理能力。

即使需要做这些改造，个人档案 Mapper 和业务 Service 仍不能搬到 `ruoyi-snailai-server`。

### 6. 个人档案场景现在有两条实现路线

#### 路线 A：先做受控上下文注入

```text
[RuoYi 登录用户请求分析]
      |
      v
[ruoyi-admin 校验权限并查询档案]
      |
      v
[只把允许分析的字段提交给 SnailAI]
      |
      v
[模型分析并返回]
```

这条路线不要求立即修改 SnailAI Server，适合先做第一版“输入姓名后分析”。

#### 路线 B：让模型自主调用业务 Tool

```text
[用户在 SnailAI 中聊天]
      |
      v
[SnailAI Server 分发可信用户上下文]
      |
      v
[ruoyi-ai 的 Tool 取得 userId + clientPk + 权限]
      |
      v
[业务 Service 安全查询或生成变更预览]
```

这条路线体验更灵活，但当前 SnailAI 1.1.1 的分发请求没有完整携带 RuoYi 的可信登录上下文。要做真实个人档案，必须先扩充分发合同，或者建立服务端短期会话映射。这个改动涉及 SnailAI 平台集成层，但不是把个人档案 CRUD 写进 Server。

## 最终目录图

```text
ruoyi-vue-plus-namewta/
|
+-- ruoyi-extend/ruoyi-snailai-server/
|   +-- 启动和配置 SnailAI 平台
|   +-- 一般不写个人档案业务
|
+-- ruoyi-common/ruoyi-common-ai/
|   +-- 引入并开启 SnailAI starters
|   +-- 放通用 AI 基础配置，不放个人档案业务
|
+-- ruoyi-modules/ruoyi-ai/
|   +-- RuoYi 与 SnailAI 的业务适配层
|   +-- 写 @Tool、AI 上下文组装、SnailAI OpenAPI 接入
|   +-- 不直接写个人档案 Mapper
|
+-- ruoyi-modules/ruoyi-person-archive/
|   +-- 写个人档案 Controller、Service、Mapper、实体
|   +-- 掌握权限、事务、审计和真实数据
|
+-- ruoyi-api/
    +-- 放 ruoyi-ai 调用个人档案模块所需的公开合同和 DTO
```

## 术语小词典

- 独立程序（可部署应用）：可以自己启动、监听端口的 Java 程序。`ruoyi-admin` 和 `ruoyi-snailai-server` 都属于这一类。
- 业务适配层：把一种系统的请求翻译成另一种系统能理解的业务调用。这里就是 `ruoyi-ai`。
- 自动发现：程序启动时扫描 Spring Bean，找到带 `@Tool` 的方法并注册给模型。
- Starter：第三方提供的一组自动配置和依赖，项目引入后就能开启一批基础能力。
- OpenAPI：SnailAI 对外提供的用户、Agent、会话和聊天等 HTTP 接口，不等于你的业务 CRUD。
- gRPC 分发：SnailAI Server 把一次聊天任务发送给 RuoYi 里的 Agent Client。
- 业务 Service：真正判断权限、执行规则、读写数据库的代码层。

## 你现在能复述什么

1. SnailAI 已经提供 Agent、Skill、会话和聊天平台，但不会自动提供个人档案查询、新增和删除接口。
2. 个人档案业务写在自己的业务模块；`@Tool` 写在 `ruoyi-ai`，运行时由 `ruoyi-admin` 里的 SnailAI Agent 执行器自动发现。
3. `ruoyi-ai` 是 RuoYi 主应用中的 AI 业务适配模块，不是独立 Server，也不是个人档案模块。
4. 通常不需要把个人档案代码写进 `ruoyi-snailai-server`；只有身份分发、Tool 白名单等平台级能力才需要扩展 Server 或 SnailAI 集成层。
