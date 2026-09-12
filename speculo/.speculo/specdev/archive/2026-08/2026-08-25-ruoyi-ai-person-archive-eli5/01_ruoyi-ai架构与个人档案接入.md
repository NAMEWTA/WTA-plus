# ruoyi-ai 架构、SnailAI Server 与个人档案接入

先说结论：这个工程里的 AI 不是“在 `ruoyi-ai` 目录里直接调用大模型”这么简单，而是一套 **SnailAI 控制端 + 若依业务执行端 + 模型服务** 的协作结构。

读完要能复述三个答案：

1. `ruoyi-ai` 是若依与 SnailAI 之间很薄的业务接入层，目前主要负责把若依登录用户映射为 SnailAI 用户。
2. `ruoyi-snailai-server` 是独立运行的 AI 控制与编排服务，管理模型、智能体、应用、会话、记忆、知识库、技能和客户端节点；它不是大模型本身。
3. 个人档案分析应先在若依业务侧完成身份校验、同名消歧、数据权限和上下文裁剪，再把受控上下文交给专用智能体；不能让模型拿一个姓名直接自由读取所有档案。

## 先看全图

```text
                              配置和管理
                         +------------------+
                         | SnailAI 管理页面 |
                         +--------+---------+
                                  |
                                  v
+--------+   HTTP   +---------------------------+
| 浏览器 | -------> | 若依主应用                 |
| AI 页面|          | ruoyi-admin + ruoyi-ai    |
+---+----+          |                           |
    |               | 1. 若依登录与权限         |
    |               | 2. SnailAI 用户注册桥     |
    |               | 3. AI 客户端与模型执行器  |
    |               +-------------+-------------+
    |                             ^ |
    |                  gRPC 分发  | | gRPC 流式结果
    |                             | v
    |               +-------------+-------------+
    +---- HTTP/SSE ->| ruoyi-snailai-server      |
                    |                           |
                    | 模型、智能体、会话、记忆  |
                    | RAG、MCP、Skill、OpenAPI  |
                    +-------------+-------------+
                                  |
                                  | 选择模型配置
                                  v
                    +---------------------------+
                    | 实际大模型服务             |
                    | OpenAI 兼容接口等          |
                    +---------------------------+
```

最容易理解错的地方是箭头方向：

```text
[SnailAI Server 决定这次怎么聊]
                |
                | gRPC 派发模型、提示词、历史和工具配置
                v
[若依主应用里的 SnailAI Client 真正调用模型]
                |
                | 流式结果
                v
[SnailAI Server 保存并转发] -> [浏览器逐字显示]
```

所以这是“服务端编排，业务客户端执行”，不是“所有 AI 计算都在 SnailAI Server 内完成”。

### 当前目录分层

```text
[前端]
plus-ui-namewta
  |
  +-- src/views/ai/chat
  |      嵌入 SnailAI 聊天页面
  |
  +-- src/views/monitor/snailai
         嵌入 SnailAI 管理页面

[若依主应用内的业务接入]
ruoyi-modules/ruoyi-ai
  |
  +-- pom.xml
  |      引入 ruoyi-api、登录态、Web 和 AI 公共适配
  |
  +-- SnailAiController
         把当前若依用户注册为 SnailAI OpenAPI 用户

[公共启动适配]
ruoyi-common/ruoyi-common-ai
  |
  +-- SnailAiConfig
         根据开关启用第三方 Agent 和 OpenAPI starter

[独立部署服务]
ruoyi-extend/ruoyi-snailai-server
  |
  +-- SnailAiServerApplication
  +-- application.yml
  +-- Actuator 认证过滤器
  +-- Dockerfile
         真正的 Server 功能来自 com.aizuda:snail-ai-starter
```

源码证据：

| 层 | 当前文件 | 已验证职责 |
| --- | --- | --- |
| 若依 AI 业务层 | <Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-ai/src/main/java/org/dromara/ai/controller/SnailAiController.java</Path> | 当前只有“注册当前用户”接口，没有个人档案查询、业务上下文装配或自研对话 Service。 |
| AI 公共层 | <Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-ai/src/main/java/org/dromara/common/ai/config/SnailAiConfig.java</Path> | 一个条件自动配置，打开第三方 Agent 与 OpenAPI 能力。 |
| SnailAI 独立服务 | <Path>ruoyi-vue-plus-namewta/ruoyi-extend/ruoyi-snailai-server/src/main/java/org/dromara/snailai/SnailAiServerApplication.java</Path> | 启动 `com.aizuda` 提供的 Server 主程序。 |
| SnailAI 数据 | <Path>ruoyi-vue-plus-namewta/script/sql/ry_ai.sql</Path> | 创建 `sai_*` 用户、模型、智能体、会话、RAG、MCP、Skill、应用和节点表。 |
| AI 聊天页面 | <Path>plus-ui-namewta/src/views/ai/chat/index.vue</Path> | 先注册若依用户，再以 iframe 打开 `/snail-chat/`。 |

## 一步一步看

### 第一步：先区分四个不同角色

```text
[若依登录用户]
      是正在使用业务系统的人

[SnailAI OpenAPI 用户]
      是 SnailAI 为外部应用用户建立的映射身份

[智能体]
      是“模型 + 固定指令 + 记忆 + 知识库 + 工具”的组合

[大模型]
      是最终生成文字的模型服务
```

当前用户映射方式是：

```text
[若依 userId]
      |
      | 作为 externalId 注册，重复注册会幂等返回
      v
[SnailAI openId]
      |
      | 用于 SnailAI 用户、订阅和会话隔离
      v
[SnailAI 内部 userId]
```

这三个 ID 不能混用。`openId` 不是若依的 `userId`，SnailAI 内部用户 ID 也不是业务档案人员 ID。

### 第二步：系统启动时发生什么

默认配置中 `snail-ai.enabled` 是 `false`，所以 AI 客户端默认不启动。完整启用顺序是：

```text
[导入 ry_ai.sql]
       |
       v
[启动 MySQL]
       |
       v
[启动 ruoyi-snailai-server]
       |
       +-- HTTP 8900：管理页面、OpenAPI、会话入口
       +-- gRPC 18888：客户端注册、心跳、对话分发
       |
       v
[在 SnailAI 创建或核对应用]
       |
       +-- app-id
       +-- token
       |
       v
[在 SnailAI 配置模型和智能体]
       |
       v
[若依配置相同 app-id/token，设置 enabled=true]
       |
       v
[启动 ruoyi-admin]
       |
       +-- 启动自己的 AI Client gRPC 端口
       +-- 向 Server 注册并发送心跳
       +-- 等待 Server 派发对话
```

当前开发配置已经给出一套示例 `app-id=1` 和 token，`ry_ai.sql` 也插入了对应应用、默认模型与示例智能体。它们适合帮助理解，不应直接当生产密钥。

如果使用仓库 Docker Compose，SnailAI 容器端口是 HTTP `8900` 与 gRPC `18888`，宿主机映射为 `48900` 与 `48888`。若依应用实例还有自己的 AI Client gRPC 端口，例如主服务 `8080` 对应 `38080`。

### 第三步：用户打开 AI 页面时发生什么

```text
[用户已经登录若依]
        |
        v
[前端 POST /snail-ai/user/register]
        |
        v
[SnailAiController 读取当前 LoginUser]
        |
        +-- externalId = 若依 userId
        +-- nickname   = 若依 nickname
        v
[OpenApiUserClient 调 SnailAI HTTP 8900]
        |
        v
[SnailAI 返回 openId]
        |
        v
[前端打开 /snail-chat/?openId=...]
        |
        v
[嵌入页面换取 SnailAI 会话 Token]
        |
        v
[展示智能体、会话和聊天输入框]
```

前端管理页 `<Path>plus-ui-namewta/src/views/monitor/snailai/index.vue</Path>` 则直接嵌入 SnailAI Server 的管理界面，开发环境地址来自 `VITE_APP_SNAILAI_ADMIN`。

### 第四步：发送一句话后怎样流动

```text
[浏览器输入问题]
        |
        | HTTP + SSE
        v
[若依应用内嵌聊天网关 /api/snail/chat]
        |
        | OpenAPI 请求
        v
[SnailAI Server]
        |
        +-- 10 读取用户和智能体
        +-- 15 检查是否订阅智能体
        +-- 20 建立或读取会话
        +-- 30 选择模型并解密模型配置
        +-- 35 解析附件
        +-- 40 加载 MCP
        +-- 50 初始化系统提示词
        +-- 60 处理 RAG
        +-- 65 处理网页搜索
        +-- 70 加载 Skill
        +-- 75 加载短期历史并选择可用 Client
        +-- 80 组装 ChatDispatchRequest
        |
        | gRPC
        v
[若依应用内 SnailAI Agent Client]
        |
        +-- 解析 Shell、HTTP、RAG、Skill、MCP、自定义工具
        +-- 组装 system + 历史 + 本轮 user 消息
        +-- 用 Spring AI 调真实模型
        |
        | gRPC 流式回传
        v
[SnailAI Server 保存会话记录]
        |
        | SSE
        v
[浏览器逐字显示答案]
```

这里的“系统提示词”最初来自智能体的 `instruction`。强制 RAG 会把检索结果追加进去；智能 RAG 则把知识库说明交给模型，让模型决定是否调用检索工具。

### 第五步：SnailAI Server 到底保存和管理什么

```text
[SnailAI Server]
  |
  +-- 用户与外部用户映射
  +-- 模型提供商、模型地址、密钥和参数
  +-- 智能体及其固定 instruction
  +-- 应用 app-id、token、路由策略
  +-- 在线 Client 节点和负载
  +-- 会话、消息、Token 用量和统计
  +-- 短期记忆
  +-- RAG 知识库、文档、分块和向量检索
  +-- MCP 服务配置
  +-- Skill 内容和关联关系
  +-- 上传资源
```

所以 `ruoyi-snailai-server` 更像“AI 调度中心和资料管理员”。它决定用哪个智能体、哪个模型、哪些历史和工具，然后把任务发给有真实业务代码的若依应用。

### 第六步：RAG、Skill、MCP、自定义 Tool 有什么区别

| 能力 | 日常比喻 | 适合什么 | 不适合什么 |
| --- | --- | --- | --- |
| 固定 instruction | 岗位说明书 | 固定角色、分析方法、输出格式 | 每次变化的个人档案 |
| RAG | 去资料库找相关段落 | 规章、报告、附件、长文档 | 强一致的实时结构化业务字段 |
| Skill | 操作手册 | 告诉智能体按什么步骤做事 | 替代业务权限校验 |
| MCP | 标准化外部工具插座 | 接独立工具服务 | 无鉴权地暴露内部数据库 |
| 自定义 `@Tool` | 若依应用里的业务按钮 | 实时查询订单、档案等业务能力 | 当前身份没有传播时直接查敏感数据 |

SnailAI `1.1.1` 的客户端会自动发现 Spring 容器中带 `@Tool` 方法的 Bean。也就是说，未来可以在若依应用里写“查询个人档案”工具。但当前实现会把发现的自定义工具加入每次对话，没有按智能体做细粒度白名单，因此不能只靠“某个智能体应该不会调用它”保障安全。

## 个人档案怎样深入业务

### 先选正确方案

用户设想的是：输入姓名，自动查人、查相关信息、注入上下文，再按特定提示词分析。

这类需求有三种接法：

```text
方案 A：业务后端先查，再交给 AI

[输入姓名] -> [后端鉴权和查档案] -> [裁剪上下文] -> [专用智能体分析]


方案 B：模型运行时调用业务 Tool

[输入姓名] -> [模型判断要查人] -> [调用档案 Tool] -> [拿结果继续分析]


方案 C：把档案都放进 RAG

[输入姓名] -> [向量检索类似片段] -> [把片段交给模型]
```

对个人档案，推荐先做 **方案 A**。它最确定，也最容易落实权限、审计和字段裁剪。

方案 B 可以作为后续增强，但必须先把“当前若依用户、Client 主键、授权范围”可靠传播到模型执行端。方案 C 只适合个人相关的非结构化材料，而且检索前必须有逐人、逐文档访问控制；不应把所有人的结构化档案放进一个共享知识库后只靠姓名检索。

### 推荐业务全图

```text
[用户输入“张三”]
        |
        v
[若依：按姓名搜索候选人]
        |
        +-- 0 人 --> 明确告诉用户未找到
        |
        +-- 1 人 --> 得到 personId
        |
        +-- 多人 --> 展示候选，让用户选择
        |
        v
[档案查询用例]
        |
        +-- 校验当前登录用户
        +-- 固定当前 Client 主键
        +-- 校验数据权限和档案可见范围
        +-- 按 personId 查询基础档案
        +-- 查询获准的关系、事件、标签、材料摘要
        v
[AI 上下文装配器]
        |
        +-- 只保留分析所需字段
        +-- 手机、证件、住址等默认不发送
        +-- 标记数据来源和更新时间
        +-- 限制字符数和记录数
        v
[专用分析请求]
        |
        +-- 锁定 agentId
        +-- 使用服务端管理的提示词模板
        +-- 记录谁、何时、因何分析了谁
        v
[SnailAI 编排与模型分析]
        |
        v
[流式返回结论、依据、未知和风险]
```

### 为什么不能只把姓名直接交给模型

```text
姓名“张三”
    |
    +-- 可能是员工 1001
    +-- 可能是客户 5028
    +-- 可能是已离职人员 777
    +-- 可能有同名但不同 Client 的人员
```

模型不应该猜。正确状态变化是：

```text
[输入姓名]
    |
    v
[候选待确认] -- 唯一匹配 --> [已确定 personId]
    |
    +-- 多个匹配 --> [用户选择] -> [已确定 personId]
    |
    +-- 没有匹配 --> [结束，不调用模型]
```

姓名只用于搜索，后续所有查询和审计都使用稳定的 `personId`。

### 推荐模块边界

```text
[ruoyi-ai]
    |
    | 只依赖公开业务合同
    v
[PersonArchiveQueryService]
    |
    v
[个人档案模块实现]
    |
    +-- 档案表
    +-- 关系表
    +-- 事件表
    +-- 材料与 OSS 引用
    +-- 数据权限

禁止的捷径：

[ruoyi-ai] -X-> [个人档案 Mapper]
[ruoyi-ai] -X-> [ruoyi-system 内部 ISysUserService / SysUserMapper]
[SnailAI Server] -X-> [直接读业务表]
```

当前 `<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/system/api/UserService.java</Path>` 只能按用户 ID、角色、部门或岗位查询，**没有按姓名查询用户的公开方法**。而且“系统登录用户”和“个人档案中的人员”未必是同一个概念。

因此未来应先明确：

```text
[sys_user]
    解决“谁能登录系统”

[person_archive]
    解决“被分析的人是谁、有哪些档案”

两者可以有关联 userId，但不能默认就是同一张表、同一个生命周期。
```

跨模块调用建议使用稳定接口和专用 DTO，例如概念上的 `PersonArchiveQueryService` 与 `PersonAnalysisContext`。接口可放在项目认可的跨模块 API 层，AI 模块只看到经过权限裁剪的 DTO，不接触档案实体、Mapper 或查询 Wrapper。

### 上下文应该长什么样

不要把数据库整行或整份档案原样拼进提示词。推荐把上下文装成一份小而明确的“分析材料包”：

```text
[分析材料包]
  |
  +-- personId：稳定标识
  +-- displayName：展示姓名
  +-- profile：允许分析的基础事实
  +-- relations：获准查看的关系摘要
  +-- events：限定时间范围的重要事件
  +-- documentSummaries：获准查看的材料摘要
  +-- sources：每条事实来自哪里、何时更新
  +-- missingFields：本次缺失但可能影响结论的数据
  +-- policy：允许做什么分析、禁止输出什么
```

提示词组合应保持三层分开：

```text
[固定系统指令]
    定义角色、分析方法、禁止事项和输出格式
             +
[受控业务上下文]
    只含本次已授权、已裁剪的事实
             +
[用户本轮问题]
    例如“分析近半年的职业稳定性信号”
             |
             v
[模型输出]
    结论 + 证据 + 不确定性 + 不应推断的部分
```

固定提示词可以配置在专用 SnailAI 智能体的 `instruction` 中。动态档案上下文必须由若依后端生成，不能由浏览器提交完整 JSON 后让后端原样相信。

### 推荐第一版落地顺序

```text
[1. 建个人档案领域模型和权限]
                 |
                 v
[2. 做按姓名搜索候选接口]
                 |
                 v
[3. 做按 personId 查询分析上下文的公开业务合同]
                 |
                 v
[4. 做上下文裁剪、脱敏、大小限制和审计]
                 |
                 v
[5. 建锁定的个人档案分析智能体]
                 |
                 v
[6. ruoyi-ai 增加专用分析入口并流式返回]
                 |
                 v
[7. 再评估动态 Tool 与个人材料 RAG]
```

第一版不必让模型“自由决定去哪查”。后端先确定人和数据，模型只负责分析，这样最容易验证结果对不对、权限有没有越界。

## 当前边界与风险

下面是当前源码事实，不是说这些风险已经修复。

### 1. 当前嵌入身份还没有闭环

```text
[若依 Token]
    |
    | 当前前端放入 iframe URL 的 trustedCredential 参数
    v
[SnailAI Chat 会话 Token]
    |
    | 会保存 trustedCredential 声明
    v
[Chat 网关]
```

当前仓库没有 `SnailAiChatCredentialValidator` 实现，所以这个若依 Token 被携带了，但没有在 SnailAI 会话签发和后续请求中完成若依侧二次校验。Token 放在 URL 查询参数中还可能进入浏览器历史、代理日志或错误采集。

生产接入前至少要改成后端换取短期、一次性、只含必要声明的 AI 会话凭证，并校验 `openId` 确实属于当前若依用户。

### 2. 调度请求没有携带完整业务身份

SnailAI `1.1.1` 的 `ChatDispatchRequest` 包含智能体、会话、用户消息、模型、工具、历史、记忆和系统提示词，但没有若依 `userId`、Client 主键、`openId` 或 `trustedCredential`。

客户端上下文类型虽然预留了 `userId`、用户名和部门字段，当前构建时只填充了智能体 ID、会话 ID 和模型标识。因此在自定义 `@Tool` 中直接调用 `LoginHelper` 或依赖当前 HTTP 登录态并不可靠；模型执行发生在独立 gRPC 请求线程中。

### 3. 自定义 Tool 目前是全局发现

```text
[Spring 容器全部 Bean]
        |
        | 扫描所有 @Tool 方法
        v
[缓存为自定义工具]
        |
        | 每次对话都加入工具集合
        v
[模型可见]
```

当前没有按 `agentId` 的自定义 Tool 白名单。个人档案工具在解决身份传播和工具授权前不能直接注册为全局工具。

### 4. 基础 Shell 和 HTTP Tool 默认加入每次对话

当前客户端 `ToolRuntime` 每次都会加入基础 Shell 与 HTTP 工具。用于可信开发场景很方便，但对面向普通用户、能接触个人档案的生产智能体风险很高。应替换或约束默认工具解析器，采用按智能体、按权限的最小工具集合。

### 5. 发送给模型的上下文还会留下数据足迹

```text
[个人档案上下文]
    |
    +-- SnailAI 会话消息表
    +-- 短期记忆
    +-- 应用日志或链路日志
    +-- 第三方模型服务
    +-- 可选 RAG / 向量库
```

必须先决定保存周期、删除方式、模型提供商的数据策略、日志脱敏、谁能查看会话、是否允许用于长期记忆。敏感字段不应因为“模型可能有用”就默认发送。

### 6. 当前配置是开发样例，不是生产基线

可见的开发默认包括数据库 `root/root`、示例 app token、默认加密密钥、MinIO 示例密钥、Actuator 全量暴露和 `show-details=ALWAYS`。生产必须通过环境或 Secret Provider 注入独立密钥，限制管理端口和 Actuator 暴露，并把 SnailAI HTTP/gRPC 放在受控网络内。

### 7. 已验证范围

本图解核对了仓库源码、配置、SQL、前端调用和本机 Maven 缓存中的 SnailAI `1.1.1` sources。没有实际启动 MySQL、SnailAI Server、若依主应用或外部模型，也没有发送真实个人数据进行端到端验证。

## 术语小词典

| 日常说法 | 专业名字 | 在这里是什么意思 |
| --- | --- | --- |
| 总调度室 | SnailAI Server | 保存 AI 配置、组织对话流程并把执行任务发给业务应用。 |
| 业务执行员 | Agent Client | 运行在若依主应用中，真正组装 Spring AI ChatClient、工具并调用模型。 |
| 岗位说明书 | system prompt / instruction | 告诉模型扮演什么角色、按什么规则回答。 |
| 对话便签 | short-term memory | 保存最近若干轮对话，下一轮不用从零开始。 |
| 资料库检索 | RAG | 先搜索相关文档片段，再把片段交给模型回答。 |
| 工具插座 | MCP | 用标准协议让模型连接外部工具服务。 |
| 业务按钮 | Tool | 让模型按参数调用一个明确的 Java 业务方法。 |
| 外部用户号码 | openId | SnailAI 给某个外部应用用户分配的标识。 |
| 稳定档案编号 | personId | 个人档案领域自己的唯一标识，不能用姓名代替。 |
| 分析材料包 | AI context | 本次经过权限校验、裁剪并发送给模型的业务事实。 |
| 控制面 | control plane | 配置、鉴权、路由和策略决定所在的一侧。 |
| 数据面 | data plane | 实际对话内容、工具结果和模型输出流动的一侧。 |

## 你现在能复述什么

```text
它是什么？
SnailAI Server 管配置和调度，若依主应用承载业务身份并执行模型调用。

为什么需要它？
它把模型、智能体、会话、记忆、RAG、MCP、Skill 和多应用 Client 统一管理。

个人档案怎样接入？
姓名只负责找候选，personId 才负责后续查询；若依后端先鉴权、查档案、裁剪上下文，
再交给锁定的专用智能体分析。动态 Tool 和 RAG 是后续能力，不是绕过业务权限的捷径。
```
