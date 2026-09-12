# ruoyi-workflow：代码接入、流程定义与跨 Client 审核

先给结论。这套工作流不是让业务模块搬进 `ruoyi-workflow`，而是让业务模块保管自己的单据，再把“这张单据现在走到哪里”交给工作流模块。

你最需要带走三个答案：

1. **它是什么**：Warm-Flow 保存流程图、实例、任务和办理人；`ruoyi-workflow` 把它接入本项目；业务模块通过 `ruoyi-api` 中的 `WorkflowService` 使用它。
2. **怎样先用代码做**：业务接入写 Java；流程图先写版本化 JSON，再导入、发布。仓库已经有可直接参考的 `<Path>ruoyi-vue-plus-namewta/script/leave/leave1.json</Path>`，不必先从空白画布拖拽。
3. **跨 Client 能否直接用**：如果 Client 只是不同前端入口，并且审批人是全局唯一用户，当前代码可以让任务按用户依次流转；如果要求“任务只能在指定 Client 出现和办理”，当前实现还不完整，因为流程任务没有 `clientId` 隔离。

## 先看全图

```text
                 同一个后端应用 ruoyi-admin

[Client1 发起页]                                      [Client2/3/管理端待办页]
       |                                                       |
       | 提交业务单据                                          | 查询和办理任务
       v                                                       v
+--------------------+                              +-----------------------+
| 你的业务模块        |                              | ruoyi-workflow        |
| 业务表 + 业务状态   |                              | 待办、已办、驳回、抄送 |
+--------------------+                              +-----------------------+
       |                                                       |
       | 只依赖公开桥梁                                        |
       +--------------------> [ruoyi-api] <--------------------+
                              WorkflowService
                              ProcessEvent
                                    |
                                    v
                         +-----------------------+
                         | Warm-Flow 流程引擎     |
                         | 定义、实例、任务、办理人 |
                         +-----------------------+
                                    |
                   +----------------+----------------+
                   |                                 |
                   v                                 v
          [flow_* 工作流表]                 [事件回写业务表 status]
```

每一层只做自己的事情：

```text
[业务模块]
  +-- 保存采购单、合同、报销单等真实业务数据
  +-- 保存 draft / waiting / finish 等业务状态
  +-- 发起流程并监听结果

[ruoyi-api]
  +-- 提供跨模块可用的 WorkflowService 和事件对象
  +-- 防止业务模块直接依赖工作流内部实现

[ruoyi-workflow]
  +-- 把公开调用转换成 Warm-Flow 操作
  +-- 提供定义管理、待办、已办、驳回、抄送等接口
  +-- 把办理人类型展开成具体用户

[Warm-Flow]
  +-- 保存流程定义和运行中的流程实例
  +-- 根据节点和连线生成下一项任务

[plus-ui]
  +-- 提供设计器、待办中心和业务表单页面
```

这里有两个很容易混在一起的东西：

```text
业务接入代码                         流程定义
--------------------------------    --------------------------------
保存单据                             有哪些节点
调用 WorkflowService                节点先后顺序
监听 ProcessEvent                   谁能办理
更新业务状态                         节点打开哪个 formPath

写在你的 Java 业务模块               写在 Warm-Flow 定义 JSON / 设计器中
```

所以“先用代码，后来再拖拽”在当前仓库中的实际含义是：

```text
[手写或复制一份流程 JSON]
             |
             v
[导入为未发布定义] -> [检查并发布] -> [业务代码用 flowCode 启动]
                                               |
                                               v
                                    [以后用设计器调整]
                                               |
                                               v
                                    [重新导出 JSON 入库]
```

它不是“先写一组 Java if/else，之后自动变成设计器图”。当前仓库没有把 Java 流程 DSL 自动转换成设计器定义的公开能力。

## 一步一步看

### 第一步：先认识当前模块架构

`ruoyi-workflow` 是一个被 `ruoyi-admin` 装配的业务模块，不是单独运行的服务。

```text
[ruoyi-admin]
      |
      +-- 运行时装配 [ruoyi-workflow]
      |
      +-- 运行时装配 [你的业务模块]

[你的业务模块]
      |
      +-- Maven 依赖 [ruoyi-api]
      |
      +-- 注入 [WorkflowService]
      |
      +-- 不依赖 [ruoyi-workflow]
      +-- 不 import [IFlw* 或 Warm-Flow 内部服务]
```

公开桥梁位于 `<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/workflow/api/WorkflowService.java</Path>`。它提供的主要动作是：

| 动作 | 用途 |
| --- | --- |
| `startWorkFlow` | 只启动实例，不顺手办完申请人首节点 |
| `startCompleteTask` | 启动实例，并办完申请人首节点 |
| `completeTask` | 后台代码办理某个任务 |
| `deleteInstance` | 按业务 ID 删除对应实例 |
| `getBusinessStatus` | 按业务 ID 查询流程状态 |
| `setVariable` / `instanceVariable` | 写入或读取流程变量 |

模块内部又用 LiteFlow 组织“启动、办理、删除”的步骤。这里的 LiteFlow 是工作流模块自己的内部流水线，不是每个业务都要复制一套的扩展点。

```text
[WorkflowService.startWorkFlow]
              |
              v
[检查 businessId 和变量]
              |
              v
[查找已发布的 flowCode]
              |
              v
[Warm-Flow 创建实例和首任务]
              |
              v
[保存业务扩展信息]
```

### 第二步：用 JSON 写第一版流程，而不是先拖拽

仓库并非只有设计器。它已经保存了六份请假流程 JSON：

```text
<Path>ruoyi-vue-plus-namewta/script/leave/</Path>
  +-- leave1.json  普通串行流程
  +-- leave2.json  排他网关
  +-- leave3.json  并行网关
  +-- leave4.json  会签
  +-- leave5.json  并行会签网关
  +-- leave6.json  组合示例
```

最适合作为第一版模板的是 `leave1.json`。它的结构可以先读成：

```text
[流程基本信息]
  flowCode / flowName / version / formPath
                 |
                 v
[nodeList 节点列表]
  开始 -> 申请人 -> 组长 -> 部门主管 -> 结束
                 |
                 v
[每个节点自己的 skipList]
  说明当前节点通过后去哪个节点
```

节点中最重要的字段是：

| 字段 | 日常含义 |
| --- | --- |
| `nodeCode` | 节点的稳定身份证，业务监听节点时会用到 |
| `nodeName` | 页面上显示的节点名称 |
| `permissionFlag` | 谁可以办理这个节点 |
| `formPath` | 这个节点打开哪个业务页面；为空时使用流程默认页面 |
| `skipList` | 通过或退回后走向哪个节点 |
| `ext` | 按钮权限等节点扩展配置 |

当前办理人写法包括：

```text
123                         -> 指定用户 ID 123
role:456                    -> 角色 ID 456 下的用户
dept:789                    -> 部门 ID 789 下的用户
post:101                    -> 岗位 ID 101 下的用户
${initiator}                -> 发起人
#{@某个Bean.某个方法(...)}   -> 由代码规则动态算出办理人
```

多个办理人标记在定义中使用 `@@` 分隔。不要直接手写 `flow_definition`、`flow_node`、`flow_skip` 表；正确入口会校验 JSON，并负责建立这些记录。

定义从代码文件进入运行库的流程是：

```text
[版本库中的 xxx.json]
          |
          | POST /workflow/definition/importDef
          v
[未发布定义]
          |
          | PUT /workflow/definition/publish/{id}
          v
[已发布定义]
          |
          | 业务代码传同一个 flowCode
          v
[新的流程实例]
```

导入和导出入口分别位于 `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/controller/FlwDefinitionController.java</Path>`。导入时会把 JSON 解析成 Warm-Flow 的 `DefJson`；发布前还会检查除申请节点外的中间节点是否配置了办理人。

以后通过设计器调整时，应把调整后的定义重新导出成 JSON 并进入版本库。这样运行库不是唯一副本，环境重建和代码评审仍有依据。

### 第三步：把其他业务模块接上流程

假设新业务叫“采购申请”，推荐边界是：

```text
[采购模块]
  +-- purchase_apply 表
  +-- PurchaseApplyService
  +-- PurchaseApplyController
  +-- ProcessEvent 监听器
  +-- 只依赖 ruoyi-api

[工作流定义]
  +-- flowCode = purchase-approval
  +-- formPath = /purchase/apply/edit

[ruoyi-workflow]
  +-- 保持通用，不放采购业务代码
```

提交时必须先得到业务主键，再启动流程：

```text
[Client1 提交采购申请]
          |
          v
[校验并保存 purchase_apply]
          |
          | 得到业务主键 9001
          v
[StartProcessDTO]
  businessId = "9001"
  flowCode   = "purchase-approval"
  variables  = 金额、部门等流程判断数据
          |
          v
[workflowService.startCompleteTask]
          |
          v
[流程实例与业务单 9001 绑定]
```

对应的最小代码形状如下。它说明调用关系，不代替目标业务的校验、权限和错误处理：

```java
private final WorkflowService workflowService;

@Transactional(rollbackFor = Exception.class)
public PurchaseApplyVo submit(PurchaseApplyBo bo) {
    PurchaseApply entity = saveAndGetId(bo);

    Map<String, Object> variables = new HashMap<>();
    variables.put("ignore", true);
    variables.put("amount", entity.getAmount());

    StartProcessDTO process = new StartProcessDTO();
    process.setBusinessId(entity.getId().toString());
    process.setFlowCode("purchase-approval");
    process.setVariables(variables);

    workflowService.startCompleteTask(process);
    return toVo(entity);
}
```

`businessId` 不能为空，而且必须能唯一找到这张业务单。当前启动代码先按 `businessId` 查已有实例，所以不同业务表若可能产生相同数字主键，需要在正式设计前确认全局唯一策略；不要默认“不同表的 9001”一定不会冲突。

流程状态通过事件回写，不要让页面猜状态：

```text
[有人办理任务]
       |
       +-- ProcessTaskEvent -> 某个节点刚被创建，可做节点级副作用
       |
       +-- ProcessEvent ----> 总体状态变化，回写业务表 status
       |
       +-- ProcessDeleteEvent -> 从流程侧删除实例时清理或解绑业务单
```

业务监听器要按自己的 `flowCode` 精确过滤：

```java
@EventListener(condition = "#event.flowCode == 'purchase-approval'")
public void onProcess(ProcessEvent event) {
    updateBusinessStatus(event.getBusinessId(), event.getStatus(), event.getSubmit());
}
```

完整可运行样例位于 `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/service/impl/TestLeaveServiceImpl.java</Path>`。复制它的接入模式，但把实体、Controller、Service 和监听器移到自己的业务模块；不要继续把新业务塞进 `ruoyi-workflow`。

### 第四步：前端怎样打开业务单并完成审核

工作流本身不生成采购、合同或报销表单。`formPath` 只是告诉前端“应该打开哪一个 Vue 路由”。

```text
[待办列表返回]
  businessId = 9001
  taskId     = 7001
  formPath   = /purchase/apply/edit
          |
          v
[前端 routerJump]
          |
          v
/purchase/apply/edit?id=9001&type=...&taskId=7001
          |
          +-- 用 id 调采购模块详情接口
          |
          +-- 用 taskId 调 POST /workflow/task/completeTask
```

公共跳转代码位于 `<Path>plus-ui-namewta/src/api/workflow/workflowCommon/index.ts</Path>`。因此每个会打开这项待办的 Client 都必须满足两件事：

1. 它的路由表中存在该 `formPath`，或者当前节点用自己的 `formPath` 覆盖流程默认值。
2. 当前用户有权读取这张业务单。拥有流程任务并不会自动获得业务表的数据权限。

如果三个 Client 使用不同的前端工程或不同路由，可以为每个节点设置不同的 `formPath`：

```text
[Client1 申请节点]  formPath=/client1/purchase/edit
          |
          v
[Client2 审核 A]    formPath=/client2/purchase/review-a
          |
          v
[Client3 审核 B]    formPath=/client3/purchase/review-b
          |
          v
[管理端终审]        formPath=/admin/purchase/final-review
```

这只解决“打开哪个页面”，不解决“任务只能在哪个 Client 看见”。

### 第五步：跨 Client 审核在当前代码中到底怎样工作

先把 Client 和审批人分开：

```text
Client                         审批人
---------------------------    ---------------------------
登录入口和权限上下文           一个 sys_user 用户
决定菜单、角色和接口权限       由 userId 标识
Token 中携带 clientPk          任务最终写入 processed_by=userId
```

当前待办查询的核心条件是：

```text
[当前登录用户 userId]
          |
          v
[flow_user.processed_by = userId]
          |
          v
[返回这个用户的待办]
```

当前查询没有再加：

```text
flow_task.client_id = 当前 clientPk
```

因为 `flow_definition`、`flow_instance`、`flow_task`、`flow_user` 等当前表都没有 `client_id` 字段，只有租户字段 `tenant_id`。`WorkflowPermissionHandler` 也只把当前登录用户 ID 交给引擎。

因此，下面这种“共享用户模式”可以流转：

```text
[Client1 用户 U1 发起]
          |
          v
[任务分给全局用户 U2]
          |
          v
[U2 从 Client2 登录并办理]
          |
          v
[任务分给全局用户 U3]
          |
          v
[U3 从 Client3 登录并办理]
          |
          v
[管理端用户 U4 终审]
```

前提是 U2、U3、U4 在对应 Client 能登录，拥有工作流页面和业务接口权限，并且对应路由存在。

但当前代码不能严格保证下面这条规则：

```text
“给 U2 的审核 A 只能在 Client2 看见和办理，
  U2 即使也能登录 Client1 或管理端，也不能在那里看见它。”
```

只要同一个用户在另一个 Client 也能访问待办接口，待办查询仍可能按相同 `userId` 返回这项任务。这是当前架构边界，不是前端隐藏一个菜单就能补上的安全隔离。

### 第六步：你看到的 Client 改动实际改了什么

提交 `eeab21d91` 的标题是 `fix(workflow): scope assignee roles to client`。它改的是 workflow 使用的 system 服务：`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysTaskAssigneeServiceImpl.java</Path>`。

```text
[设计器在某个 Client 登录上下文中打开]
                    |
                    v
[读取 Token 中的 clientPk]
                    |
                    v
[角色选择器只查询这个 Client 的角色]
```

前端设计器 iframe 也把 `clientid` 传给后端，路径是 `<Path>plus-ui-namewta/src/views/workflow/processDefinition/design.vue</Path>`。

这个改动修复的是“设计流程时不能看到并选择别的 Client 的角色”。它没有做以下事情：

```text
没有给流程定义增加 clientId
没有给流程实例增加 clientId
没有给流程任务增加 clientId
没有让待办查询按 clientPk 过滤
没有让 ProcessEvent 携带 clientId
```

因此它是“设计时角色候选隔离”，不是“运行时流程任务的 Client 隔离”。而且当前只明确过滤了角色候选；用户、部门和岗位不是按同一 `clientId` 字段隔离的角色模型。

### 第七步：你的四端流程现在有两种理解

#### 理解 A：Client 只是入口，任务属于人

```text
[一条连续流程实例]
        |
        +-> 审核 A 属于用户 U2
        +-> 审核 B 属于用户 U3
        +-> 终审属于用户 U4

Client1 / Client2 / Client3 / 管理端
        |
        +-> 只是这些用户进入同一后端的不同门
```

这种模式最接近当前实现。你需要准备好各端路由、菜单、接口权限和业务数据授权，然后用用户、动态规则或办理时指定下一办理人完成路由。

#### 理解 B：每个节点必须属于一个指定 Client

```text
[流程实例]
    |
    +-> 节点 A：targetClient=Client2，assignee=U2
    +-> 节点 B：targetClient=Client3，assignee=U3
    +-> 终审：targetClient=管理端，assignee=U4
                         |
                         v
              [查询和办理都校验 targetClient]
```

这种模式当前尚未形成完整合同。至少要决定并实现：

| 要决定的地方 | 为什么需要 |
| --- | --- |
| Client 归属放在定义、节点、实例还是任务 | 决定一条流程能否跨多个 Client |
| 启动时怎样记录来源 Client | 便于审计和回溯 |
| 下一节点怎样确定目标 Client | 角色和用户选择需要正确作用域 |
| 待办、已办、抄送怎样按 Client 过滤 | 防止跨 Client 看见任务 |
| 办理接口怎样校验 Client | 防止绕过页面直接调用接口 |
| 事件是否携带来源和目标 Client | 业务状态和通知可能需要知道端别 |
| 各 Client 如何安全读取同一业务单 | 流程权限不能替代业务数据权限 |

这已经不是简单配置，应另开 Spec/设计 change 决定安全合同后再实现。

### 第八步：一条最稳妥的落地顺序

```text
[1. 先选一个真实但低风险业务]
              |
              v
[2. 复制 leave1.json，代码方式定义串行节点]
              |
              v
[3. 导入、检查、发布固定 flowCode]
              |
              v
[4. 业务模块按 TestLeave 模式接入]
              |
              v
[5. 先在单 Client 跑通发起、待办、办理、状态回写]
              |
              v
[6. 再用全局用户模式跑通 Client1 -> Client2 -> Client3 -> 管理端]
              |
              v
[7. 做跨 Client 可见性测试]
              |
              +-- 若允许“任务属于人” -> 固化权限和路由合同
              |
              +-- 若必须“任务属于端” -> 进入新的 Client 隔离设计
```

第一条业务流不要一开始就加入会签、并行网关、自由跳转和复杂动态办理人。先用五个节点跑通：

```text
[开始] -> [申请人] -> [审核 A] -> [审核 B] -> [管理端终审] -> [结束]
```

验收时至少检查：

```text
[重复提交同一 businessId] -> 不会生成两条流程
[非办理人直接调用 completeTask] -> 被拒绝
[Client2 的审核人] -> 能打开业务页并读到业务单
[Client3 的非审核人] -> 看不到或不能办理该任务
[每次办理] -> 业务 status 与流程状态一致
[终审完成] -> 业务状态进入 finish
[流程退回/撤销/删除] -> 业务状态或数据按约定变化
[同一用户登录多个 Client] -> 结果符合你选择的隔离模式
```

### 哪些做法不要采用

```text
[不要] 让每个业务模块直接依赖 ruoyi-workflow
[不要] 让业务模块调用 IFlw*、DefService、TaskService
[不要] 直接写 flow_* 表来部署定义
[不要] 在业务模块复制 LiteFlow chain
[不要] 自己查询 flow_task 拼待办中心
[不要] 认为 formPath 自动授予了业务数据权限
[不要] 认为设计器角色已按 Client 过滤，就等于运行时任务已隔离
```

## 术语小词典

| 日常说法 | 专业名字 | 在这里是什么意思 |
| --- | --- | --- |
| 流程图纸 | 流程定义（Definition） | 节点、连线、条件、办理人和表单路径的模板 |
| 一张正在走的单 | 流程实例（Instance） | 某个 `businessId` 按某个定义运行的一次记录 |
| 轮到某人处理的一步 | 任务（Task） | 当前节点产生的待办事项 |
| 真实业务单号码 | `businessId` | 把工作流实例和采购单、合同等业务数据绑在一起 |
| 流程模板代号 | `flowCode` | 业务代码启动哪一种已发布流程 |
| 办理人标签 | `permissionFlag` | 用户、角色、部门、岗位或动态规则 |
| 打开业务页的地址 | `formPath` | 待办点击后跳转的 Vue 路由 |
| 不同登录入口 | Client | 当前项目中决定登录域、菜单、角色和会话权限的上下文 |
| 当前 Client 的数据库主键 | `clientPk` | Token 中保存的 `sys_client.id`，类型为 Long |
| 业务结果通知 | `ProcessEvent` | 流程总体状态改变后发布给业务模块的 Spring 事件 |
| 节点通知 | `ProcessTaskEvent` | 新任务节点创建时发布给业务模块的 Spring 事件 |
| 代码里的流程文件 | 定义 JSON | 可以导入、发布、导出，也能被设计器继续修改的流程定义 |

## 你现在能复述什么

读完后，你应该能复述下面四句话：

1. 我的采购、合同或其他业务数据仍由自己的业务模块管理；它只通过 `ruoyi-api` 的 `WorkflowService` 接入审批。
2. 第一版流程可以复制仓库现有 `script/leave/*.json` 用代码编辑，再通过定义导入和发布接口部署，以后再进设计器调整并导出回版本库。
3. 当前跨 Client 流转的真正钥匙是全局 `userId`；`formPath` 决定打开哪个页面，但不会隔离任务，也不会自动授权业务数据。
4. 现有 Client 改动只把设计器的角色候选限制在当前 Client；如果要求每个节点只能在指定 Client 看见和办理，需要另做运行时 Client 隔离设计。

最小参考路径：

| 要看什么 | 项目路径 |
| --- | --- |
| 公开 Java 门面 | `<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/workflow/api/WorkflowService.java</Path>` |
| 业务接入完整样例 | `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/service/impl/TestLeaveServiceImpl.java</Path>` |
| 最简单流程 JSON | `<Path>ruoyi-vue-plus-namewta/script/leave/leave1.json</Path>` |
| 定义导入、发布、导出 | `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/controller/FlwDefinitionController.java</Path>` |
| 当前用户待办查询 | `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/mapper/FlwTaskMapper.java</Path>` |
| 当前用户身份交给引擎 | `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/handler/WorkflowPermissionHandler.java</Path>` |
| 设计器办理人解析 | `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/service/impl/FlwTaskAssigneeServiceImpl.java</Path>` |
| Client 角色候选过滤 | `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysTaskAssigneeServiceImpl.java</Path>` |
| 前端待办跳转 | `<Path>plus-ui-namewta/src/api/workflow/workflowCommon/index.ts</Path>` |
