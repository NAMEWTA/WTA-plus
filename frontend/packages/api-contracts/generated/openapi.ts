export interface paths {
    "/workflow/task/updateAssignee/{userId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 批量修改任务办理人。
         * @description 批量修改任务办理人。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `workflow:task:edit`<br><br>
         */
        put: operations["updateAssignee"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/spel": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 修改流程 SpEL 表达式定义。
         * @description 修改流程 SpEL 表达式定义。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `workflow:spel:edit`<br><br>
         */
        put: operations["edit"];
        /**
         * 新增流程 SpEL 表达式定义。
         * @description 新增流程 SpEL 表达式定义。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `workflow:spel:add`<br><br>
         */
        post: operations["add"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/leave": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 修改请假单。
         * @description 修改请假单。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `workflow:leave:edit`<br><br>
         */
        put: operations["edit_1"];
        /**
         * 新增请假单。
         * @description 新增请假单。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `workflow:leave:add`<br><br>
         */
        post: operations["add_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/instance/updateVariable": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 修改流程变量。
         * @description 修改流程变量。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `workflow:instance:variable`<br><br>
         */
        put: operations["updateVariable"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/instance/cancelProcessApply": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 撤销当前申请人发起的流程。
         * @description 撤销当前申请人发起的流程。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `workflow:instance:cancel`<br><br>
         */
        put: operations["cancelProcessApply"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/instance/active/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 激活或挂起流程实例。
         * @description 激活或挂起流程实例。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `workflow:instance:active`<br><br>
         */
        put: operations["active"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/definition": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 修改流程定义。
         * @description 修改流程定义。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `workflow:definition:edit`<br><br>
         */
        put: operations["edit_2"];
        /**
         * 新增流程定义并执行格式校验。
         * @description 新增流程定义并执行格式校验。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `workflow:definition:add`<br><br>
         */
        post: operations["add_2"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/definition/unPublish/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 取消发布流程定义。
         * @description 取消发布流程定义。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `workflow:definition:publish`<br><br>
         */
        put: operations["unPublish"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/definition/publish/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 发布流程定义，使其进入可用状态。
         * @description 发布流程定义，使其进入可用状态。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `workflow:definition:publish`<br><br>
         */
        put: operations["publish"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/definition/active/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 激活或挂起流程定义。
         * @description 激活或挂起流程定义。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `workflow:definition:active`<br><br>
         */
        put: operations["active_1"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/category": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 修改流程分类，并校验名称唯一及父子关系合法性。
         * @description 修改流程分类，并校验名称唯一及父子关系合法性。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `workflow:category:edit`<br><br>
         */
        put: operations["edit_3"];
        /**
         * 新增流程分类，并校验分类名称唯一性。
         * @description 新增流程分类，并校验分类名称唯一性。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `workflow:category:add`<br><br>
         */
        post: operations["add_3"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/user": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 修改用户。
         * @description 修改用户。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:user:edit`<br><br>
         */
        put: operations["edit_4"];
        /**
         * 新增用户。
         * @description 新增用户。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:user:add`<br><br>
         */
        post: operations["add_4"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/userType": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 修改登录域名称、状态、排序与备注。编码不可改。
         * @description 修改登录域名称、状态、排序与备注。编码不可改。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:userType:edit`<br><br>
         */
        put: operations["edit_5"];
        /**
         * 新增登录域。编码创建后只读。
         * @description 新增登录域。编码创建后只读。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:userType:add`<br><br>
         */
        post: operations["add_5"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/userType/changeStatus": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 修改登录域启停状态。
         * @description 修改登录域启停状态。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:userType:edit`<br><br>
         */
        put: operations["changeStatus"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/user/resetPwd": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 重置指定用户密码。
         * @description 重置指定用户密码。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:user:resetPwd`<br><br>
         */
        put: operations["resetPwd"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/user/profile": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 获取当前登录用户的个人中心信息。
         * @description 获取当前登录用户的个人中心信息。
         */
        get: operations["profile"];
        /**
         * 修改当前登录用户的个人资料。
         * @description 修改当前登录用户的个人资料。
         */
        put: operations["updateProfile"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/user/profile/updatePwd": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 重置密码
         * @description 重置密码
         */
        put: operations["updatePwd"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/user/changeStatus": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 修改用户状态。
         * @description 修改用户状态。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:user:edit`<br><br>
         */
        put: operations["changeStatus_1"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/user/authRole": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 用户授权角色
         * @description 用户授权角色<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:user:edit`<br><br>
         */
        put: operations["insertAuthRole"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/role": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 修改角色基础信息（不包含菜单权限、数据权限）。
         * @description 修改角色基础信息（不包含菜单权限、数据权限）。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:role:edit`<br><br>
         */
        put: operations["edit_6"];
        /**
         * 新增角色。
         * @description 新增角色。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:role:add`<br><br>
         */
        post: operations["add_6"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/role/permission": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 修改角色权限信息（菜单权限 + 数据权限）。
         * @description 修改角色权限信息（菜单权限 + 数据权限）。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:role:edit`<br><br>
         */
        put: operations["editPermission"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/role/changeStatus": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 修改角色状态。
         * @description 修改角色状态。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:role:edit`<br><br>
         */
        put: operations["changeStatus_2"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/role/authUser/selectAll": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 批量选择用户授权
         * @description 批量选择用户授权<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:role:edit`<br><br>
         */
        put: operations["selectAuthUserAll"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/role/authUser/cancel": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 取消授权用户。
         * @description 取消授权用户。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:role:edit`<br><br>
         */
        put: operations["cancelAuthUser"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/role/authUser/cancelAll": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 批量取消授权用户
         * @description 批量取消授权用户<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:role:edit`<br><br>
         */
        put: operations["cancelAuthUserAll"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/post": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 修改岗位。
         * @description 修改岗位。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:post:edit`<br><br>
         */
        put: operations["edit_7"];
        /**
         * 新增岗位。
         * @description 新增岗位。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:post:add`<br><br>
         */
        post: operations["add_7"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/menu": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 修改菜单。
         * @description 修改菜单。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:menu:edit`<br><br>**角色校验：**<br><br>- `superadmin`<br>
         */
        put: operations["edit_8"];
        /**
         * 新增菜单。
         * @description 新增菜单。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:menu:add`<br><br>**角色校验：**<br><br>- `superadmin`<br>
         */
        post: operations["add_8"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/dict/type": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 修改字典类型。
         * @description 修改字典类型。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:dict:edit`<br><br>
         */
        put: operations["edit_9"];
        /**
         * 新增字典类型。
         * @description 新增字典类型。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:dict:add`<br><br>
         */
        post: operations["add_9"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/dict/data": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 修改字典数据。
         * @description 修改字典数据。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:dict:edit`<br><br>
         */
        put: operations["edit_10"];
        /**
         * 新增字典数据。
         * @description 新增字典数据。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:dict:add`<br><br>
         */
        post: operations["add_10"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/dept": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 修改部门。
         * @description 修改部门。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:dept:edit`<br><br>
         */
        put: operations["edit_11"];
        /**
         * 新增部门。
         * @description 新增部门。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:dept:add`<br><br>
         */
        post: operations["add_11"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/config": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 修改参数配置。
         * @description 修改参数配置。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:config:edit`<br><br>
         */
        put: operations["edit_12"];
        /**
         * 新增参数配置。
         * @description 新增参数配置。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:config:add`<br><br>
         */
        post: operations["add_12"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/config/updateByKey": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 根据参数键名修改参数配置。
         * @description 根据参数键名修改参数配置。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:config:edit`<br><br>
         */
        put: operations["updateByKey"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/client": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 修改客户端配置，避免重复占用同一个客户端 key。
         * @description 修改客户端配置，避免重复占用同一个客户端 key。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:client:edit`<br><br>
         */
        put: operations["edit_13"];
        /**
         * 新增客户端配置，入库前先校验客户端 key 是否唯一。
         * @description 新增客户端配置，入库前先校验客户端 key 是否唯一。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:client:add`<br><br>
         */
        post: operations["add_13"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/client/changeStatus": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 修改客户端启停状态。
         * @description 修改客户端启停状态。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:client:edit`<br><br>
         */
        put: operations["changeStatus_3"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/tree": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 修改测试树表
         * @description 修改测试树表<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `demo:tree:edit`<br><br>
         */
        put: operations["edit_14"];
        /**
         * 新增测试树表
         * @description 新增测试树表<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `demo:tree:add`<br><br>
         */
        post: operations["add_14"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/demo": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 修改测试单表
         * @description 修改测试单表<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `demo:demo:edit`<br><br>
         */
        put: operations["edit_15"];
        /**
         * 新增测试单表
         * @description 新增测试单表<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `demo:demo:add`<br><br>
         */
        post: operations["add_15"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/snail/chat/conversations": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["listConversations"];
        put: operations["updateConversationTitle"];
        post: operations["createConversation"];
        delete: operations["deleteConversation"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/task/urgeTask": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 催办任务。
         * @description 催办任务。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `workflow:task:edit`<br><br>
         */
        post: operations["urgeTask"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/task/terminationTask": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 终止流程任务。
         * @description 终止流程任务。
         */
        post: operations["terminationTask"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/task/taskOperation/{taskOperation}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 执行任务操作，例如委派、转办、加签或减签。
         * @description 执行任务操作，例如委派、转办、加签或减签。
         */
        post: operations["taskOperation"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/task/startWorkFlow": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 启动流程任务。
         * @description 启动流程任务。
         */
        post: operations["startWorkFlow"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/task/getNextNodeList": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 获取流程下一节点信息。
         * @description 获取流程下一节点信息。
         */
        post: operations["getNextNodeList"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/task/completeTask": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 办理当前任务节点。
         * @description 办理当前任务节点。
         */
        post: operations["completeTask"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/task/backProcess": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 驳回审批到前置节点。
         * @description 驳回审批到前置节点。
         */
        post: operations["backProcess"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/leave/submitAndFlowStart": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 提交请假单并同步发起流程。
         * @description 提交请假单并同步发起流程。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `workflow:leave:add`<br><br>
         */
        post: operations["submitAndFlowStart"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/leave/export": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 导出请假列表。
         * @description 导出请假列表。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `workflow:leave:export`<br><br>
         */
        post: operations["export"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/instance/invalid": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 作废流程实例。
         * @description 作废流程实例。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `workflow:instance:invalid`<br><br>
         */
        post: operations["invalid"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/definition/importDef": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 通过文件导入流程定义。
         * @description 通过文件导入流程定义。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `workflow:definition:import`<br><br>
         */
        post: operations["importDef"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/definition/exportDef/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 导出流程定义文件。
         * @description 导出流程定义文件。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `workflow:definition:export`<br><br>
         */
        post: operations["exportDef"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/definition/copy/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 复制一份流程定义，便于快速创建相似流程。
         * @description 复制一份流程定义，便于快速创建相似流程。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `workflow:definition:copy`<br><br>
         */
        post: operations["copy"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/category/export": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 导出流程分类列表。
         * @description 导出流程分类列表。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `workflow:category:export`<br><br>
         */
        post: operations["export_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/warm-flow/save-json": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["saveJson"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/warm-flow/form-content": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["saveFormContent"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/warm-flow/execute/handle": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["handle"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/third/provider": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** @description <br><h3>访问权限</h3><br>**权限校验：**<br><br>- `third:provider:add`<br><br> */
        post: operations["add_16"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/third/provider/{providerId}/status": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** @description <br><h3>访问权限</h3><br>**权限校验：**<br><br>- `third:provider:edit`<br><br> */
        post: operations["status"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/third/provider/{providerId}/remove": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** @description <br><h3>访问权限</h3><br>**权限校验：**<br><br>- `third:provider:remove`<br><br> */
        post: operations["remove"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/third/provider/save": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** @description <br><h3>访问权限</h3><br>**权限校验：**<br><br>- `third:provider:edit`<br><br> */
        post: operations["save"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/third/endpoint": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** @description <br><h3>访问权限</h3><br>**权限校验：**<br><br>- `third:endpoint:add`<br><br> */
        post: operations["add_17"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/third/endpoint/{endpointId}/status": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** @description <br><h3>访问权限</h3><br>**权限校验：**<br><br>- `third:endpoint:edit`<br><br> */
        post: operations["status_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/third/endpoint/{endpointId}/remove": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** @description <br><h3>访问权限</h3><br>**权限校验：**<br><br>- `third:endpoint:remove`<br><br> */
        post: operations["remove_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/third/endpoint/save": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** @description <br><h3>访问权限</h3><br>**权限校验：**<br><br>- `third:endpoint:edit`<br><br> */
        post: operations["save_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/third/credential": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** @description <br><h3>访问权限</h3><br>**权限校验：**<br><br>- `third:credential:add`<br><br> */
        post: operations["save_2"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/third/credential/{credentialId}/remove": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** @description <br><h3>访问权限</h3><br>**权限校验：**<br><br>- `third:credential:remove`<br><br> */
        post: operations["remove_2"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/userType/export": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 导出登录域列表。
         * @description 导出登录域列表。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:userType:export`<br><br>
         */
        post: operations["export_2"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/user/temporaryPassword": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 签发 60 秒有效、单次消费的临时密码，不修改永久密码。
         * @description 签发 60 秒有效、单次消费的临时密码，不修改永久密码。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:user:temporaryPassword`<br><br>
         */
        post: operations["issue"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/user/resetPwd/candidate": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 生成可编辑的永久密码重置候选，不修改用户。
         * @description 生成可编辑的永久密码重置候选，不修改用户。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:user:resetPwd`<br><br>
         */
        post: operations["candidate"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/user/importTemplate": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 导出用户导入模板。
         * @description 导出用户导入模板。
         */
        post: operations["importTemplate"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/user/importData": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 导入数据
         * @description 导入数据<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:user:import`<br><br>
         */
        post: operations["importData"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/user/export": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 导出符合条件的用户列表。
         * @description 导出符合条件的用户列表。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:user:export`<br><br>
         */
        post: operations["export_3"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/role/export": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 导出角色信息列表。
         * @description 导出角色信息列表。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:role:export`<br><br>
         */
        post: operations["export_4"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/post/export": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 导出岗位列表。
         * @description 导出岗位列表。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:post:export`<br><br>
         */
        post: operations["export_5"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/openApi/users/{userId}/credential/reset": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** @description <br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:openApi:edit`<br><br> */
        post: operations["resetUser"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/openApi/users/{userId}/credential/enable": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** @description <br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:openApi:edit`<br><br> */
        post: operations["enableUser"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/openApi/users/{userId}/credential/disable": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** @description <br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:openApi:edit`<br><br> */
        post: operations["disableUser"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/openApi/users/{userId}/credential/delete": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** @description <br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:openApi:remove`<br><br> */
        post: operations["deleteUser"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/openApi/users/{userId}/credential/create": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** @description <br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:openApi:add`<br><br> */
        post: operations["createUser"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/openApi/self/credential/reset": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** @description <br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:openApi:self`<br><br> */
        post: operations["resetSelf"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/openApi/self/credential/enable": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** @description <br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:openApi:self`<br><br> */
        post: operations["enableSelf"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/openApi/self/credential/disable": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** @description <br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:openApi:self`<br><br> */
        post: operations["disableSelf"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/openApi/self/credential/delete": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** @description <br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:openApi:self`<br><br> */
        post: operations["deleteSelf"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/openApi/self/credential/create": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** @description <br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:openApi:self`<br><br> */
        post: operations["createSelf"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/dict/type/export": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 导出字典类型列表。
         * @description 导出字典类型列表。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:dict:export`<br><br>
         */
        post: operations["export_6"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/dict/data/export": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 导出字典数据列表。
         * @description 导出字典数据列表。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:dict:export`<br><br>
         */
        post: operations["export_7"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/config/export": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 导出参数配置列表。
         * @description 导出参数配置列表。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:config:export`<br><br>
         */
        post: operations["export_8"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/client/export": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 导出客户端管理列表，便于离线审计与配置核查。
         * @description 导出客户端管理列表，便于离线审计与配置核查。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:client:export`<br><br>
         */
        post: operations["export_9"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/swagger/demo/upload": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 上传请求
         *      必须使用 @RequestPart 注解标注为文件
         * @description 上传请求
         *      必须使用 @RequestPart 注解标注为文件
         */
        post: operations["upload"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/snail-ai/user/register": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 注册当前登录用户并返回 OpenAPI 用户信息。
         * @description 注册当前登录用户并返回 OpenAPI 用户信息。
         */
        post: operations["registerCurrentUser"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/resource/oss/uploads": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** @description <br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:oss:upload`<br><br> */
        post: operations["init"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/resource/oss/uploads/{uploadToken}/parts/sign": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** @description <br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:oss:upload`<br><br> */
        post: operations["signParts"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/resource/oss/uploads/{uploadToken}/complete": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** @description <br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:oss:upload`<br><br> */
        post: operations["complete"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/resource/oss/migrations/{batchId}/rollback": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** @description <br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:ossMigration:rollback`<br><br> */
        post: operations["rollback"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/resource/oss/migrations/{batchId}/retry": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** @description <br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:ossMigration:execute`<br><br> */
        post: operations["retry"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/resource/oss/migrations/{batchId}/cleanup": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** @description <br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:ossMigration:cleanup`<br><br> */
        post: operations["cleanup"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/resource/oss/migrations/start": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** @description <br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:ossMigration:execute`<br><br> */
        post: operations["start"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/resource/oss/migrations/dry-run": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** @description <br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:ossMigration:execute`<br><br> */
        post: operations["dryRun"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/resource/oss/config": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 新增对象存储配置。
         * @description 新增对象存储配置。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:ossConfig:add`<br><br>
         */
        post: operations["add_18"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/resource/oss/config/remove/{ossConfigIds}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 批量删除对象存储配置。
         * @description 批量删除对象存储配置。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:ossConfig:remove`<br><br>
         */
        post: operations["remove_3"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/resource/oss/config/edit": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 修改对象存储配置。
         * @description 修改对象存储配置。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:ossConfig:edit`<br><br>
         */
        post: operations["edit_16"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/resource/oss/config/changeStatus": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 切换对象存储配置启用状态，并同步更新当前生效配置。
         * @description 切换对象存储配置启用状态，并同步更新当前生效配置。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:ossConfig:edit`<br><br>
         */
        post: operations["changeStatus_4"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/profile/person/verification/providers/{providerCode}/callback": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 处理 callback HTTP 请求。
         * @description 处理 callback HTTP 请求。<br><h3>访问权限</h3><br>> **权限策略**：忽略权限检查<br>
         */
        post: operations["callback"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/profile/person/rebind/unbind": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 处理 unbind HTTP 请求。
         * @description 处理 unbind HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:person:apply`<br><br>
         */
        post: operations["unbind"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/profile/person/rebind/submit": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 处理 submit HTTP 请求。
         * @description 处理 submit HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:person:apply`<br><br>
         */
        post: operations["submit"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/profile/person/rebind/probe": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 处理 probe HTTP 请求。
         * @description 处理 probe HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:person:apply`<br><br>
         */
        post: operations["probe"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/profile/person/rebind/match": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 处理 match HTTP 请求。
         * @description 处理 match HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:person:apply`<br><br>
         */
        post: operations["match"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/profile/person/rebind/confirm": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 处理 confirm HTTP 请求。
         * @description 处理 confirm HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:person:apply`<br><br>
         */
        post: operations["confirm"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/profile/person/materials/{ownerType}/{ownerId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 处理 list HTTP 请求。
         * @description 处理 list HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:person:material` | `profile:person:query` | `profile:person:review` | `profile:person:manage` | `profile:person:override`<br><br>
         */
        get: operations["list"];
        put?: never;
        /**
         * 处理 attach HTTP 请求。
         * @description 处理 attach HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:person:material`<br><br>
         */
        post: operations["attach"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/profile/person/materials/{ownerType}/{ownerId}/{materialRefId}/detach": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 处理 detach HTTP 请求。
         * @description 处理 detach HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:person:material`<br><br>
         */
        post: operations["detach"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/profile/person/archive/{profileId}/revoke": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 处理 revoke HTTP 请求。
         * @description 处理 revoke HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:person:override`<br><br>
         */
        post: operations["revoke"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/profile/person/archive/{profileId}/revision": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 处理 revise HTTP 请求。
         * @description 处理 revise HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:person:override`<br><br>
         */
        post: operations["revise"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/profile/person/archive/{profileId}/binding": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 处理 manageBinding HTTP 请求。
         * @description 处理 manageBinding HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:person:manage`<br><br>
         */
        post: operations["manageBinding"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/profile/person/archive/{profileId}/assign": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 处理 assign HTTP 请求。
         * @description 处理 assign HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:person:override`<br><br>
         */
        post: operations["assign"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/profile/person/archive/application/{applicationId}/decision": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 处理 decide HTTP 请求。
         * @description 处理 decide HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:person:override`<br><br>
         */
        post: operations["decide"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/profile/person/archive/admin-create": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 处理 create HTTP 请求。
         * @description 处理 create HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:person:override`<br><br>
         */
        post: operations["create"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/profile/person/application": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 处理 current HTTP 请求。
         * @description 处理 current HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:person:apply`<br><br>
         */
        get: operations["current"];
        put?: never;
        /**
         * 处理 save HTTP 请求。
         * @description 处理 save HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:person:apply`<br><br>
         */
        post: operations["save_3"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/profile/person/application/submit": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 处理 submit HTTP 请求。
         * @description 处理 submit HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:person:apply`<br><br>
         */
        post: operations["submit_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/profile/material-tags": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 处理 create HTTP 请求。
         * @description 处理 create HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:material-tag:manage`<br><br>
         */
        post: operations["create_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/profile/material-tags/{materialNodeId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 处理 update HTTP 请求。
         * @description 处理 update HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:material-tag:manage`<br><br>
         */
        post: operations["update"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/profile/material-tags/{materialNodeId}/status": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 处理 status HTTP 请求。
         * @description 处理 status HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:material-tag:manage`<br><br>
         */
        post: operations["status_2"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/profile/material-tags/{materialNodeId}/archive": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 处理 archive HTTP 请求。
         * @description 处理 archive HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:material-tag:manage`<br><br>
         */
        post: operations["archive"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/profile/enterprise/verification/providers/{providerCode}/callback": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 处理 callback HTTP 请求。
         * @description 处理 callback HTTP 请求。<br><h3>访问权限</h3><br>> **权限策略**：忽略权限检查<br>
         */
        post: operations["callback_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/profile/enterprise/transfer/unbind": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 处理 unbind HTTP 请求。
         * @description 处理 unbind HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:enterprise:apply`<br><br>
         */
        post: operations["unbind_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/profile/enterprise/transfer/send": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 处理 send HTTP 请求。
         * @description 处理 send HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:enterprise:apply`<br><br>
         */
        post: operations["send"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/profile/enterprise/transfer/confirm": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 处理 confirm HTTP 请求。
         * @description 处理 confirm HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:enterprise:apply`<br><br>
         */
        post: operations["confirm_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/profile/enterprise/materials/{ownerType}/{ownerId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 处理 list HTTP 请求。
         * @description 处理 list HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:enterprise:material` | `profile:enterprise:query` | `profile:enterprise:review` | `profile:enterprise:manage` | `profile:enterprise:override`<br><br>
         */
        get: operations["list_1"];
        put?: never;
        /**
         * 处理 attach HTTP 请求。
         * @description 处理 attach HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:enterprise:material`<br><br>
         */
        post: operations["attach_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/profile/enterprise/materials/{ownerType}/{ownerId}/{materialRefId}/detach": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 处理 detach HTTP 请求。
         * @description 处理 detach HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:enterprise:material`<br><br>
         */
        post: operations["detach_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/profile/enterprise/archive/{profileId}/revoke": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 处理 revoke HTTP 请求。
         * @description 处理 revoke HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:enterprise:override`<br><br>
         */
        post: operations["revoke_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/profile/enterprise/archive/{profileId}/revision": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 处理 revise HTTP 请求。
         * @description 处理 revise HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:enterprise:override`<br><br>
         */
        post: operations["revise_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/profile/enterprise/archive/{profileId}/binding": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 处理 manageBinding HTTP 请求。
         * @description 处理 manageBinding HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:enterprise:manage`<br><br>
         */
        post: operations["manageBinding_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/profile/enterprise/archive/{profileId}/assign": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 处理 assign HTTP 请求。
         * @description 处理 assign HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:enterprise:override`<br><br>
         */
        post: operations["assign_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/profile/enterprise/archive/application/{applicationId}/decision": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 处理 decide HTTP 请求。
         * @description 处理 decide HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:enterprise:override`<br><br>
         */
        post: operations["decide_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/profile/enterprise/archive/admin-create": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 处理 create HTTP 请求。
         * @description 处理 create HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:enterprise:override`<br><br>
         */
        post: operations["create_2"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/profile/enterprise/application": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 处理 current HTTP 请求。
         * @description 处理 current HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:enterprise:apply`<br><br>
         */
        get: operations["current_1"];
        put?: never;
        /**
         * 处理 save HTTP 请求。
         * @description 处理 save HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:enterprise:apply`<br><br>
         */
        post: operations["save_4"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/profile/enterprise/application/submit": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 处理 submit HTTP 请求。
         * @description 处理 submit HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:enterprise:apply`<br><br>
         */
        post: operations["submit_2"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/profile/enterprise/application/probe": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 处理 probe HTTP 请求。
         * @description 处理 probe HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:enterprise:apply`<br><br>
         */
        post: operations["probe_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/notify/notification": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 提交通知。
         * @description 提交通知。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `notify:notification:submit`<br><br>
         */
        post: operations["submit_3"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/notify/notification/{notificationId}/retry": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 重试通知。
         * @description 重试通知。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `notify:notification:retry`<br><br>
         */
        post: operations["retry_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/notify/notification/{notificationId}/cancel": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 取消通知。
         * @description 取消通知。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `notify:notification:cancel`<br><br>
         */
        post: operations["cancel"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/notify/notice/{noticeId}/retract": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 撤回公告。
         * @description 撤回公告。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `notify:notice:retract`<br><br>
         */
        post: operations["retract"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/notify/notice/{noticeId}/publish": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 发布公告并提交统一通知。
         * @description 发布公告并提交统一通知。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `notify:notice:publish`<br><br>
         */
        post: operations["publish_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/notify/notice/save": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 新增或修改公告草稿。
         * @description 新增或修改公告草稿。
         */
        post: operations["save_5"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/notify/notice/remove": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 删除公告。
         * @description 删除公告。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `notify:notice:remove`<br><br>
         */
        post: operations["remove_4"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/notify/inbox/{messageId}/seen": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 标记已见。
         * @description 标记已见。
         */
        post: operations["seen"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/notify/inbox/{messageId}/read": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 标记已读。
         * @description 标记已读。
         */
        post: operations["read"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/notify/inbox/read-all": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 将当前用户全部收件消息标记为已读。
         * @description 将当前用户全部收件消息标记为已读。
         */
        post: operations["readAll"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/notify/callback/{channel}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 接收供应商状态回调。
         * @description 接收供应商状态回调。<br><h3>访问权限</h3><br>> **权限策略**：忽略权限检查<br>
         */
        post: operations["callback_2"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/monitor/operlog/export": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 导出操作日志记录列表。
         * @description 导出操作日志记录列表。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `monitor:operlog:export`<br><br>
         */
        post: operations["export_10"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/monitor/loginInfo/export": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 导出系统访问记录列表。
         * @description 导出系统访问记录列表。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `monitor:logininfo:export`<br><br>
         */
        post: operations["export_11"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/excel/importWithOptions": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 导入表格
         * @description 导入表格
         */
        post: operations["importWithOptions"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/demo/importData": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 导入数据
         * @description 导入数据<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `demo:demo:import`<br><br>
         */
        post: operations["importData_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/demo/export": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 导出测试单表列表
         * @description 导出测试单表列表<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `demo:demo:export`<br><br>
         */
        post: operations["export_12"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/batch/add": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 新增批量方法 可完美替代 saveBatch 秒级插入上万数据 (对mysql负荷较大)
         * @description 新增批量方法 可完美替代 saveBatch 秒级插入上万数据 (对mysql负荷较大)
         *      <p>
         *      3.5.0 版本 增加 rewriteBatchedStatements=true 批处理参数 使 MP 原生批处理可以达到同样的速度
         */
        post: operations["add_19"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/batch/addOrUpdate": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 新增或更新 可完美替代 saveOrUpdateBatch 高性能
         * @description 新增或更新 可完美替代 saveOrUpdateBatch 高性能
         *      <p>
         *      3.5.0 版本 增加 rewriteBatchedStatements=true 批处理参数 使 MP 原生批处理可以达到同样的速度
         */
        post: operations["addOrUpdate"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/auth/social/callback": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 处理前端回调后的社交账号绑定。
         * @description 处理前端回调后的社交账号绑定。<br><h3>访问权限</h3><br>> **权限策略**：忽略权限检查<br>
         */
        post: operations["socialCallback"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/auth/register": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 用户注册。
         * @description 用户注册。<br><h3>访问权限</h3><br>> **权限策略**：忽略权限检查<br>
         */
        post: operations["register"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/auth/logout": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 退出登录
         * @description 退出登录<br><h3>访问权限</h3><br>> **权限策略**：忽略权限检查<br>
         */
        post: operations["logout"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/auth/login": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 登录方法
         * @description 登录方法<br><h3>访问权限</h3><br>> **权限策略**：忽略权限检查<br>
         */
        post: operations["login"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/snail/chat/session": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["session"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/snail/chat/resource/upload": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["uploadResource"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/snail/chat/completions": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["completions"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/snail/chat/agent/subscribe": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["subscribeAgent"];
        delete: operations["unsubscribeAgent"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/task/pageByTaskWait": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 查询当前用户的待办任务。
         * @description 查询当前用户的待办任务。
         */
        get: operations["pageByTaskWait"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/task/pageByTaskFinish": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 查询当前用户的已办任务。
         * @description 查询当前用户的已办任务。
         */
        get: operations["pageByTaskFinish"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/task/pageByTaskCopy": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 查询当前用户收到的抄送任务。
         * @description 查询当前用户收到的抄送任务。
         */
        get: operations["pageByTaskCopy"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/task/pageByAllTaskWait": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 查询全部待办任务。
         * @description 查询全部待办任务。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `workflow:task:list`<br><br>
         */
        get: operations["pageByAllTaskWait"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/task/pageByAllTaskFinish": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 查询全部已办任务。
         * @description 查询全部已办任务。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `workflow:task:list`<br><br>
         */
        get: operations["pageByAllTaskFinish"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/task/getTask/{taskId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 根据任务 id 查询任务详情。
         * @description 根据任务 id 查询任务详情。
         */
        get: operations["getTask"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/task/getBackTaskNode/{taskId}/{nowNodeCode}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 获取当前任务可驳回的前置节点。
         * @description 获取当前任务可驳回的前置节点。
         */
        get: operations["getBackTaskNode"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/task/currentTaskAllUser/{taskId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 获取当前任务的所有办理人。
         * @description 获取当前任务的所有办理人。
         */
        get: operations["currentTaskAllUser"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/spel/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 获取流程 SpEL 表达式定义详情。
         * @description 获取流程 SpEL 表达式定义详情。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `workflow:spel:query`<br><br>
         */
        get: operations["getInfo"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/spel/list": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 分页查询流程 SpEL 表达式定义列表。
         * @description 分页查询流程 SpEL 表达式定义列表。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `workflow:spel:list`<br><br>
         */
        get: operations["list_2"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/leave/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 获取请假单详情。
         * @description 获取请假单详情。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `workflow:leave:query`<br><br>
         */
        get: operations["getInfo_1"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/leave/list": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 分页查询请假列表。
         * @description 分页查询请假列表。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `workflow:leave:list`<br><br>
         */
        get: operations["list_3"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/instance/pageByRunning": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 分页查询正在运行的流程实例。
         * @description 分页查询正在运行的流程实例。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `workflow:instance:list`<br><br>
         */
        get: operations["selectRunningInstanceList"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/instance/pageByFinish": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 分页查询已结束的流程实例。
         * @description 分页查询已结束的流程实例。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `workflow:instance:list`<br><br>
         */
        get: operations["selectFinishInstanceList"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/instance/pageByCurrent": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 获取当前登录人发起的流程实例列表。
         * @description 获取当前登录人发起的流程实例列表。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `workflow:instance:currentList`<br><br>
         */
        get: operations["selectCurrentInstanceList"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/instance/instanceVariable/{instanceId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 获取流程变量。
         * @description 获取流程变量。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `workflow:instance:variableQuery`<br><br>
         */
        get: operations["instanceVariable"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/instance/getInfo/{businessId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 根据业务 id 查询流程实例详细信息。
         * @description 根据业务 id 查询流程实例详细信息。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `workflow:instance:query`<br><br>
         */
        get: operations["getInfo_2"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/instance/flowHisTaskList/{businessId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 获取流程图和流程记录，用于展示实例流转轨迹。
         * @description 获取流程图和流程记录，用于展示实例流转轨迹。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `workflow:instance:query`<br><br>
         */
        get: operations["flowHisTaskList"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/definition/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 获取流程定义详细信息。
         * @description 获取流程定义详细信息。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `workflow:definition:query`<br><br>
         */
        get: operations["getInfo_3"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/definition/xmlString/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 获取流程定义 JSON 字符串。
         * @description 获取流程定义 JSON 字符串。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `workflow:definition:query`<br><br>
         */
        get: operations["xmlString"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/definition/unPublishList": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 分页查询未发布的流程定义列表。
         * @description 分页查询未发布的流程定义列表。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `workflow:definition:list`<br><br>
         */
        get: operations["unPublishList"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/definition/list": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 分页查询流程定义列表。
         * @description 分页查询流程定义列表。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `workflow:definition:list`<br><br>
         */
        get: operations["list_4"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/category/{categoryId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 获取单个流程分类详情。
         * @description 获取单个流程分类详情。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `workflow:category:query`<br><br>
         */
        get: operations["getInfo_4"];
        put?: never;
        post?: never;
        /**
         * 删除流程分类，删除前校验默认分类、子节点和绑定流程定义。
         * @description 删除流程分类，删除前校验默认分类、子节点和绑定流程定义。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `workflow:category:remove`<br><br>
         */
        delete: operations["remove_5"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/category/list": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 查询流程分类列表。
         * @description 查询流程分类列表。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `workflow:category:list`<br><br>
         */
        get: operations["list_5"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/category/categoryTree": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 获取流程分类树列表，用于流程定义选择分类节点。
         * @description 获取流程分类树列表，用于流程定义选择分类节点。
         */
        get: operations["categoryTree"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/warm-flow/query-flow-chart/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["queryFlowChart"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/warm-flow/query-def/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["queryDef"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/warm-flow/query-def": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["queryDef_1"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/warm-flow/published-form": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["publishedForm"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/warm-flow/node-ext": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["nodeExt"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/warm-flow/listener-list": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["listenerList"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/warm-flow/handler-type": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["handlerType"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/warm-flow/handler-result": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["handlerResult"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/warm-flow/handler-feedback": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["handlerFeedback"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/warm-flow/handler-dict": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["handlerDict"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/warm-flow/form-content/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["getFormContent"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/warm-flow/execute/load/{taskId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["load"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/warm-flow/execute/hisLoad/{taskId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["hisLoad"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/warm-flow-ui/config": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["config"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/third/statistics/list": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description <br><h3>访问权限</h3><br>**权限校验：**<br><br>- `third:statistics:list`<br><br> */
        get: operations["statistics"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/third/provider/{providerId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description <br><h3>访问权限</h3><br>**权限校验：**<br><br>- `third:provider:query`<br><br> */
        get: operations["get"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/third/provider/list": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description <br><h3>访问权限</h3><br>**权限校验：**<br><br>- `third:provider:list`<br><br> */
        get: operations["list_6"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/third/invocation/list": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description <br><h3>访问权限</h3><br>**权限校验：**<br><br>- `third:invocation:list`<br><br> */
        get: operations["invocations"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/third/endpoint/{endpointId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description <br><h3>访问权限</h3><br>**权限校验：**<br><br>- `third:endpoint:query`<br><br> */
        get: operations["get_1"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/third/endpoint/list": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description <br><h3>访问权限</h3><br>**权限校验：**<br><br>- `third:endpoint:list`<br><br> */
        get: operations["list_7"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/third/credential/list": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description <br><h3>访问权限</h3><br>**权限校验：**<br><br>- `third:credential:list`<br><br> */
        get: operations["list_8"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/userType/{userTypeId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 获取登录域详细信息。
         * @description 获取登录域详细信息。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:userType:query`<br><br>
         */
        get: operations["getInfo_5"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/userType/user/{userId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 查询指定用户拥有的登录域。
         * @description 查询指定用户拥有的登录域。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:user:query`<br><br>
         */
        get: operations["listByUser"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/userType/options": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 获取启用中的登录域下拉列表。
         * @description 获取启用中的登录域下拉列表。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:userType:query`<br><br>
         */
        get: operations["options"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/userType/list": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 分页查询登录域列表。
         * @description 分页查询登录域列表。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:userType:list`<br><br>
         */
        get: operations["list_9"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/user/unlock/{userId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 解锁用户
         * @description 解锁用户<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:user:edit`<br><br>
         */
        get: operations["unlock"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/user/optionselect": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 根据用户ID串批量获取用户基础信息
         * @description 根据用户ID串批量获取用户基础信息<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:user:query`<br><br>
         */
        get: operations["optionselect"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/user/list": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 分页查询用户列表。
         * @description 分页查询用户列表。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:user:list`<br><br>
         */
        get: operations["list_10"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/user/list/dept/{deptId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 获取指定部门下的全部用户信息。
         * @description 获取指定部门下的全部用户信息。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:user:list`<br><br>
         */
        get: operations["listByDept"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/user/getInfo": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 获取用户信息
         * @description 获取用户信息
         */
        get: operations["getInfo_6"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/user/deptTree": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 获取用户筛选用的部门树。
         * @description 获取用户筛选用的部门树。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:user:list`<br><br>
         */
        get: operations["deptTree"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/user/authRole/{userId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 根据用户编号获取授权角色
         * @description 根据用户编号获取授权角色<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:user:query`<br><br>
         */
        get: operations["authRole"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/user/{userId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 根据用户编号获取详细信息
         * @description 根据用户编号获取详细信息<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:user:query`<br><br>
         */
        get: operations["getInfo_7"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/user/": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 根据用户编号获取详细信息
         * @description 根据用户编号获取详细信息<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:user:query`<br><br>
         */
        get: operations["getInfo_8"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/social/list": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 查询当前登录用户的社会化账号绑定列表。
         * @description 查询当前登录用户的社会化账号绑定列表。
         */
        get: operations["list_11"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/role/{roleId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 根据角色编号获取详细信息
         * @description 根据角色编号获取详细信息<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:role:query`<br><br>
         */
        get: operations["getInfo_9"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/role/optionselect": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 获取角色选择框列表
         * @description 获取角色选择框列表<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:role:query`<br><br>
         */
        get: operations["optionselect_1"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/role/list": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 分页查询角色列表。
         * @description 分页查询角色列表。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:role:list`<br><br>
         */
        get: operations["list_12"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/role/deptTree/{roleId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 获取对应角色部门树列表
         * @description 获取对应角色部门树列表<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:role:list`<br><br>
         */
        get: operations["roleDeptTreeselect"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/role/authUser/unallocatedList": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 查询未分配用户角色列表。
         * @description 查询未分配用户角色列表。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:role:list`<br><br>
         */
        get: operations["unallocatedList"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/role/authUser/allocatedList": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 查询已分配用户角色列表。
         * @description 查询已分配用户角色列表。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:role:list`<br><br>
         */
        get: operations["allocatedList"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/post/{postId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 根据岗位编号获取详细信息
         * @description 根据岗位编号获取详细信息<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:post:query`<br><br>
         */
        get: operations["getInfo_10"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/post/optionselect": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 获取岗位选择框列表
         * @description 获取岗位选择框列表<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:post:query`<br><br>
         */
        get: operations["optionselect_2"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/post/list": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 分页查询岗位列表。
         * @description 分页查询岗位列表。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:post:list`<br><br>
         */
        get: operations["list_13"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/post/deptTree": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 获取岗位筛选用的部门树。
         * @description 获取岗位筛选用的部门树。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:post:list`<br><br>
         */
        get: operations["deptTree_1"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/openApi/users": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description <br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:openApi:list`<br><br> */
        get: operations["users"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/openApi/users/{userId}/interfaces": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description <br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:openApi:list`<br><br> */
        get: operations["userInterfaces"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/openApi/users/{userId}/interfaces/{interfaceId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description <br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:openApi:query`<br><br> */
        get: operations["userInterface"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/openApi/users/{userId}/credential": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description <br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:openApi:query`<br><br> */
        get: operations["getUserCredential"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/openApi/self/interfaces": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description <br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:openApi:self`<br><br> */
        get: operations["selfInterfaces"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/openApi/self/interfaces/{interfaceId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description <br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:openApi:self`<br><br> */
        get: operations["selfInterface"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/openApi/self/credential": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description <br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:openApi:self`<br><br> */
        get: operations["getSelfCredential"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/menu/{menuId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 根据菜单编号获取详细信息
         * @description 根据菜单编号获取详细信息<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:menu:query`<br><br>**角色校验：**<br><br>- `superadmin`<br>
         */
        get: operations["getInfo_11"];
        put?: never;
        post?: never;
        /**
         * 删除菜单
         * @description 删除菜单<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:menu:remove`<br><br>**角色校验：**<br><br>- `superadmin`<br>
         */
        delete: operations["remove_6"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/menu/treeselect": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 获取菜单下拉树列表。
         * @description 获取菜单下拉树列表。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:menu:query`<br><br>
         */
        get: operations["treeselect"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/menu/roleMenuTreeselect/{roleId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 加载对应角色菜单列表树
         * @description 加载对应角色菜单列表树<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:menu:query`<br><br>
         */
        get: operations["roleMenuTreeselect"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/menu/list": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 查询菜单列表。
         * @description 查询菜单列表。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:menu:list`<br><br>**角色校验：**<br><br>- `superadmin`<br>
         */
        get: operations["list_14"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/menu/getRouters": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 获取路由信息
         * @description 获取路由信息
         */
        get: operations["getRouters"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/dict/type/{dictId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 查询字典类型详细
         * @description 查询字典类型详细<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:dict:query`<br><br>
         */
        get: operations["getInfo_12"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/dict/type/optionselect": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 获取字典类型下拉选择列表。
         * @description 获取字典类型下拉选择列表。
         */
        get: operations["optionselect_3"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/dict/type/list": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 分页查询字典类型列表。
         * @description 分页查询字典类型列表。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:dict:list`<br><br>
         */
        get: operations["list_15"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/dict/data/{dictCode}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 查询字典数据详细
         * @description 查询字典数据详细<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:dict:query`<br><br>
         */
        get: operations["getInfo_13"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/dict/data/type/{dictType}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 根据字典类型查询字典数据信息
         * @description 根据字典类型查询字典数据信息
         */
        get: operations["dictType"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/dict/data/list": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 分页查询字典数据列表。
         * @description 分页查询字典数据列表。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:dict:list`<br><br>
         */
        get: operations["list_16"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/dept/{deptId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 根据部门编号获取详细信息
         * @description 根据部门编号获取详细信息<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:dept:query`<br><br>
         */
        get: operations["getInfo_14"];
        put?: never;
        post?: never;
        /**
         * 删除部门
         * @description 删除部门<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:dept:remove`<br><br>
         */
        delete: operations["remove_7"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/dept/optionselect": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 获取部门选择框列表
         * @description 获取部门选择框列表<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:dept:query`<br><br>
         */
        get: operations["optionselect_4"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/dept/list": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 查询部门列表。
         * @description 查询部门列表。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:dept:list`<br><br>
         */
        get: operations["list_17"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/dept/list/exclude/{deptId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 查询部门列表（排除节点）
         * @description 查询部门列表（排除节点）<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:dept:list`<br><br>
         */
        get: operations["excludeChild"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/config/{configId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 根据参数编号获取详细信息
         * @description 根据参数编号获取详细信息<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:config:query`<br><br>
         */
        get: operations["getInfo_15"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/config/list": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 分页查询参数配置列表。
         * @description 分页查询参数配置列表。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:config:list`<br><br>
         */
        get: operations["list_18"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/config/configKey/{configKey}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 根据参数键名查询参数值
         * @description 根据参数键名查询参数值
         */
        get: operations["getConfigKey"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/client/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 获取单个客户端的详细配置信息。
         * @description 获取单个客户端的详细配置信息。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:client:query`<br><br>
         */
        get: operations["getInfo_16"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/client/list": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 分页查询客户端管理列表。
         * @description 分页查询客户端管理列表。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:client:list`<br><br>
         */
        get: operations["list_19"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/resource/sms/code": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 发送短信验证码。
         * @description 发送短信验证码。<br><h3>访问权限</h3><br>> **权限策略**：忽略权限检查<br>
         */
        get: operations["smsCode"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/resource/oss/{ossId}/download-url": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 生成管理面短时下载授权。普通业务应先校验自身业务权限，再调用内部 OssService。
         * @description 生成管理面短时下载授权。普通业务应先校验自身业务权限，再调用内部 OssService。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:oss:download`<br><br>
         */
        get: operations["downloadUrl"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/resource/oss/uploads/{uploadToken}/parts": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description <br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:oss:upload`<br><br> */
        get: operations["parts"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/resource/oss/migrations/{batchId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description <br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:ossMigration:list`<br><br> */
        get: operations["batch"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/resource/oss/migrations/{batchId}/items": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description <br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:ossMigration:list`<br><br> */
        get: operations["items"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/resource/oss/list": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 分页查询 OSS 对象存储列表。
         * @description 分页查询 OSS 对象存储列表。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:oss:list`<br><br>
         */
        get: operations["list_20"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/resource/oss/listByIds/{ossIds}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 查询OSS对象基于id串
         * @description 查询OSS对象基于id串<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:oss:query`<br><br>
         */
        get: operations["listByIds"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/resource/oss/config/{ossConfigId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 获取单个对象存储配置详情。
         * @description 获取单个对象存储配置详情。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:ossConfig:list`<br><br>
         */
        get: operations["getInfo_17"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/resource/oss/config/list": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 分页查询对象存储配置列表。
         * @description 分页查询对象存储配置列表。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:ossConfig:list`<br><br>
         */
        get: operations["list_21"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/resource/message": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 建立当前登录用户的 SSE 连接。
         * @description 建立当前登录用户的 SSE 连接。<br><h3>访问权限</h3><br>> **权限策略**：忽略权限检查<br>
         */
        get: operations["connect"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/resource/message/ticket": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["issue_1"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/resource/message/close": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 关闭当前登录用户的 SSE 连接。
         * @description 关闭当前登录用户的 SSE 连接。<br><h3>访问权限</h3><br>> **权限策略**：忽略权限检查<br>
         */
        get: operations["close"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/resource/email/code": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 发送邮箱验证码
         * @description 发送邮箱验证码<br><h3>访问权限</h3><br>> **权限策略**：忽略权限检查<br>
         */
        get: operations["emailCode"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/profile/person/materials/{ownerType}/{ownerId}/{materialRefId}/access-url": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 处理 accessUrl HTTP 请求。
         * @description 处理 accessUrl HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:person:material` | `profile:person:query` | `profile:person:review` | `profile:person:manage` | `profile:person:override`<br><br>
         */
        get: operations["accessUrl"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/profile/person/archive": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 处理 page HTTP 请求。
         * @description 处理 page HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:person:query`<br><br>
         */
        get: operations["page"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/profile/person/archive/{profileId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 处理 detail HTTP 请求。
         * @description 处理 detail HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:person:query`<br><br>
         */
        get: operations["detail"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/profile/person/archive/{profileId}/material/{materialRefId}/access-url": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 处理 material HTTP 请求。
         * @description 处理 material HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:person:material`<br><br>
         */
        get: operations["material"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/profile/person/archive/eligible-users": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 处理 eligibleUsers HTTP 请求。
         * @description 处理 eligibleUsers HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:person:override`<br><br>
         */
        get: operations["eligibleUsers"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/profile/person/archive/application/{applicationId}/review-context": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 处理 reviewContext HTTP 请求。
         * @description 处理 reviewContext HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:person:review`<br><br>
         */
        get: operations["reviewContext"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/profile/person/archive/application/{applicationId}/material/{materialRefId}/access-url": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 处理 reviewMaterial HTTP 请求。
         * @description 处理 reviewMaterial HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:person:review`<br><br>
         */
        get: operations["reviewMaterial"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/profile/material-tags/tree": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 处理 tree HTTP 请求。
         * @description 处理 tree HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:material-tag:query` | `profile:person:material` | `profile:enterprise:material` | `profile:person:override` | `profile:enterprise:override`<br><br>
         */
        get: operations["tree"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/profile/enterprise/materials/{ownerType}/{ownerId}/{materialRefId}/access-url": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 处理 accessUrl HTTP 请求。
         * @description 处理 accessUrl HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:enterprise:material` | `profile:enterprise:query` | `profile:enterprise:review` | `profile:enterprise:manage` | `profile:enterprise:override`<br><br>
         */
        get: operations["accessUrl_1"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/profile/enterprise/archive": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 处理 page HTTP 请求。
         * @description 处理 page HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:enterprise:query`<br><br>
         */
        get: operations["page_1"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/profile/enterprise/archive/{profileId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 处理 detail HTTP 请求。
         * @description 处理 detail HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:enterprise:query`<br><br>
         */
        get: operations["detail_1"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/profile/enterprise/archive/{profileId}/material/{materialRefId}/access-url": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 处理 material HTTP 请求。
         * @description 处理 material HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:enterprise:material`<br><br>
         */
        get: operations["material_1"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/profile/enterprise/archive/eligible-users": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 处理 eligibleUsers HTTP 请求。
         * @description 处理 eligibleUsers HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:enterprise:override`<br><br>
         */
        get: operations["eligibleUsers_1"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/profile/enterprise/archive/application/{applicationId}/review-context": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 处理 reviewContext HTTP 请求。
         * @description 处理 reviewContext HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:enterprise:review`<br><br>
         */
        get: operations["reviewContext_1"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/profile/enterprise/archive/application/{applicationId}/material/{materialRefId}/access-url": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 处理 reviewMaterial HTTP 请求。
         * @description 处理 reviewMaterial HTTP 请求。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `profile:enterprise:review`<br><br>
         */
        get: operations["reviewMaterial_1"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/notify/recipients/search": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description <br><h3>访问权限</h3><br>**权限校验：**<br><br>- `notify:notice:add` | `notify:notice:edit`<br><br> */
        get: operations["search"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/notify/recipients/by-ids": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description <br><h3>访问权限</h3><br>**权限校验：**<br><br>- `notify:notice:add` | `notify:notice:edit`<br><br> */
        get: operations["byIds"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/notify/notification/{notificationId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 查询通知。
         * @description 查询通知。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `notify:notification:query`<br><br>
         */
        get: operations["query"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/notify/notice/{noticeId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 公告详情。
         * @description 公告详情。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `notify:notice:query`<br><br>
         */
        get: operations["get_2"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/notify/notice/list": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 公告分页。
         * @description 公告分页。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `notify:notice:list`<br><br>
         */
        get: operations["list_22"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/notify/monitor/snapshot": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 查询通知详情。
         * @description 查询通知详情。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `notify:monitor:query`<br><br>
         */
        get: operations["snapshot"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/notify/monitor/deliveries": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 按接收人和渠道查询投递日志。
         * @description 按接收人和渠道查询投递日志。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `notify:monitor:list`<br><br>
         */
        get: operations["deliveries"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/notify/inbox": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 查询当前用户收件箱。
         * @description 查询当前用户收件箱。
         */
        get: operations["list_23"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/monitor/operlog/list": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 分页查询操作日志记录。
         * @description 分页查询操作日志记录。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `monitor:operlog:list`<br><br>
         */
        get: operations["list_24"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/monitor/online": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 获取当前登录用户的在线设备列表，仅返回当前账号仍有效的 token 会话。
         * @description 获取当前登录用户的在线设备列表，仅返回当前账号仍有效的 token 会话。
         */
        get: operations["getInfo_18"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/monitor/online/list": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 获取在线用户监控列表，并按 IP 或用户名条件过滤当前有效会话。
         * @description 获取在线用户监控列表，并按 IP 或用户名条件过滤当前有效会话。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `monitor:online:list`<br><br>
         */
        get: operations["list_25"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/monitor/loginInfo/unlock/{userName}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 清除指定用户的登录失败锁定状态。
         * @description 清除指定用户的登录失败锁定状态。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `monitor:logininfo:unlock`<br><br>
         */
        get: operations["unlock_1"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/monitor/loginInfo/list": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 分页查询系统访问记录。
         * @description 分页查询系统访问记录。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `monitor:logininfo:list`<br><br>
         */
        get: operations["list_26"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/monitor/cache": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 获取 Redis 缓存监控信息。
         * @description 获取 Redis 缓存监控信息。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `monitor:cache:list`<br><br>
         */
        get: operations["getInfo_19"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/websocket/send": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 发布消息
         * @description 发布消息
         */
        get: operations["send_1"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/tree/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 获取测试树表详细信息
         * @description 获取测试树表详细信息<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `demo:tree:query`<br><br>
         */
        get: operations["getInfo_20"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/tree/list": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 查询测试树表列表
         * @description 查询测试树表列表<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `demo:tree:list`<br><br>
         */
        get: operations["list_27"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/tree/export": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 导出测试树表列表
         * @description 导出测试树表列表<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `demo:tree:export`<br><br>
         */
        get: operations["export_13"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/sms/sendTencent": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 发送短信Tencent
         * @description 发送短信Tencent
         */
        get: operations["sendTencent"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/sms/sendAliyun": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 发送短信Aliyun
         * @description 发送短信Aliyun
         */
        get: operations["sendAliyun"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/sms/removeBlacklist": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 移除黑名单
         * @description 移除黑名单
         */
        get: operations["removeBlacklist"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/sms/addBlacklist": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 添加黑名单
         * @description 添加黑名单
         */
        get: operations["addBlacklist"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/sensitive/test": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 测试数据脱敏
         * @description 测试数据脱敏
         */
        get: operations["test"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/saTokenDoc/special/tempPermission": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 场景15：临时权限校验（SaCheckPermission逻辑：临时权限>永久权限）
         *      注：临时权限需通过SaToken API手动设置，如 SaHolder.getStpLogic().setTempPermission("system:temp:test")
         * @description 场景15：临时权限校验（SaCheckPermission逻辑：临时权限>永久权限）
         *      注：临时权限需通过SaToken API手动设置，如 SaHolder.getStpLogic().setTempPermission("system:temp:test")<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:temp:test`<br><br>
         */
        get: operations["tempPermission"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/saTokenDoc/special/loginTypeSpecify": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 场景16：登录类型指定（多端登录场景，如PC/APP/小程序）
         *      注：需配合SaToken多账号体系配置
         * @description 场景16：登录类型指定（多端登录场景，如PC/APP/小程序）
         *      注：需配合SaToken多账号体系配置
         */
        get: operations["loginTypeSpecify"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/saTokenDoc/special/ignoreOverride": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 场景14：SaIgnore局部覆盖（方法注解覆盖类注解，若有）
         *      假设类上有@SaCheckLogin，方法上@SaIgnore会覆盖
         * @description 场景14：SaIgnore局部覆盖（方法注解覆盖类注解，若有）
         *      假设类上有@SaCheckLogin，方法上@SaIgnore会覆盖<br><h3>访问权限</h3><br>> **权限策略**：忽略权限检查<br>
         */
        get: operations["ignoreOverride"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/saTokenDoc/basic/singleRole": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 场景2：单一角色校验（AND模式，默认）
         * @description 场景2：单一角色校验（AND模式，默认）<br><h3>访问权限</h3><br>**角色校验：**<br><br>- `admin`<br>
         */
        get: operations["singleRole"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/saTokenDoc/basic/singlePermission": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 场景3：单一权限校验（AND模式，默认）
         * @description 场景3：单一权限校验（AND模式，默认）<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:user:view`<br><br>
         */
        get: operations["singlePermission"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/saTokenDoc/basic/loginOnly": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 场景1：仅登录校验（无角色/权限限制，只需登录态）
         * @description 场景1：仅登录校验（无角色/权限限制，只需登录态）
         */
        get: operations["loginOnly"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/saTokenDoc/basic/ignoreAll": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 场景4：忽略所有权限校验（SaIgnore优先级最高）
         * @description 场景4：忽略所有权限校验（SaIgnore优先级最高）<br><h3>访问权限</h3><br>> **权限策略**：忽略权限检查<br>
         */
        get: operations["ignoreAll"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/saTokenDoc/advanced/roleWildcardPrefix": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 场景10：角色通配符匹配（前缀匹配）
         *      拥有admin_* 即可匹配所有admin开头的角色
         * @description 场景10：角色通配符匹配（前缀匹配）
         *      拥有admin_* 即可匹配所有admin开头的角色<br><h3>访问权限</h3><br>**角色校验：**<br><br>- `admin_*`<br>
         */
        get: operations["roleWildcardPrefix"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/saTokenDoc/advanced/permWithOrRole": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 场景13：orRole参数（权限校验失败时，兜底角色校验）
         *      核心逻辑：无system:user:export权限时，检查是否有admin/operator角色
         * @description 场景13：orRole参数（权限校验失败时，兜底角色校验）
         *      核心逻辑：无system:user:export权限时，检查是否有admin/operator角色<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:user:export`<br>  - 或角色：`admin` & `operator`<br><br>
         */
        get: operations["permWithOrRole"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/saTokenDoc/advanced/permWildcardPrefix": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 场景9：权限通配符匹配（前缀匹配）
         *      拥有system:user:* 即可匹配所有用户模块权限
         * @description 场景9：权限通配符匹配（前缀匹配）
         *      拥有system:user:* 即可匹配所有用户模块权限<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:user:*`<br><br>
         */
        get: operations["permWildcardPrefix"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/saTokenDoc/advanced/mixRolePermOr": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 场景12：权限+角色混合OR模式（任一条件满足即可）
         *      满足任一：拥有super_admin角色 | 拥有system:manage权限
         * @description 场景12：权限+角色混合OR模式（任一条件满足即可）
         *      满足任一：拥有super_admin角色 | 拥有system:manage权限<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:manage`<br><br>**角色校验：**<br><br>- `super_admin`<br>
         */
        get: operations["mixRolePermOr"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/saTokenDoc/advanced/mixRolePermAnd": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 场景11：权限+角色混合AND模式（所有条件必须满足）
         *      需同时满足：拥有admin角色 + 拥有system:user:all权限
         * @description 场景11：权限+角色混合AND模式（所有条件必须满足）
         *      需同时满足：拥有admin角色 + 拥有system:user:all权限<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:user:all`<br><br>**角色校验：**<br><br>- `admin`<br>
         */
        get: operations["mixRolePermAnd"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/saTokenDoc/advance/multiRoleOr": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 场景6：多角色OR模式（拥有任一角色即可）
         * @description 场景6：多角色OR模式（拥有任一角色即可）<br><h3>访问权限</h3><br>**角色校验：**<br><br>- `admin` | `test`<br>
         */
        get: operations["multiRoleOr"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/saTokenDoc/advance/multiRoleAnd": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 场景5：多角色AND模式（必须同时拥有所有角色）
         * @description 场景5：多角色AND模式（必须同时拥有所有角色）<br><h3>访问权限</h3><br>**角色校验：**<br><br>- `admin` & `operator`<br>
         */
        get: operations["multiRoleAnd"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/saTokenDoc/advance/multiPermOr": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 场景8：多权限OR模式（拥有任一权限即可）
         * @description 场景8：多权限OR模式（拥有任一权限即可）<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:user:add` | `system:user:delete`<br><br>
         */
        get: operations["multiPermOr"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/saTokenDoc/advance/multiPermAnd": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 场景7：多权限AND模式（必须同时拥有所有权限）
         * @description 场景7：多权限AND模式（必须同时拥有所有权限）<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:user:edit` & `system:log:view`<br><br>
         */
        get: operations["multiPermAnd"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/redisLock/testLock4j": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 测试lock4j 注解
         * @description 测试lock4j 注解
         */
        get: operations["testLock4j"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/redisLock/testLock4jLockTemplate": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 测试lock4j 工具
         * @description 测试lock4j 工具
         */
        get: operations["testLock4jLockTemplate"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/redis/pubsub/sub": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 订阅消息
         * @description 订阅消息
         */
        get: operations["sub"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/redis/pubsub/pub": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 发布消息
         * @description 发布消息
         */
        get: operations["pub"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/rateLimiter/testip": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 测试请求IP限流
         *      同一IP请求受影响
         * @description 测试请求IP限流
         *      同一IP请求受影响
         */
        get: operations["testip"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/rateLimiter/testcluster": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 测试集群实例限流
         *      启动两个后端服务互不影响
         * @description 测试集群实例限流
         *      启动两个后端服务互不影响
         */
        get: operations["testcluster"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/rateLimiter/test": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 测试全局限流
         *      全局影响
         * @description 测试全局限流
         *      全局影响
         */
        get: operations["test_1"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/rateLimiter/testObj": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 测试请求IP限流(key基于参数获取)
         *      同一IP请求受影响
         * @description 测试请求IP限流(key基于参数获取)
         *      同一IP请求受影响
         *      <p>
         *      简单变量获取 #变量 复杂表达式 #{#变量 != 1 ? 1 : 0}
         */
        get: operations["testObj"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/queue/priority/remove": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 删除队列数据
         * @description 删除队列数据
         */
        get: operations["remove_8"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/queue/priority/get": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 获取队列数据
         * @description 获取队列数据
         */
        get: operations["get_3"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/queue/priority/add": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 添加队列数据
         * @description 添加队列数据
         */
        get: operations["add_20"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/openapi/echo": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 回显调用方传入的消息，用于验证 OpenAPI 接口目录和签名调用。
         * @description 回显调用方传入的消息，用于验证 OpenAPI 接口目录和签名调用。
         */
        get: operations["echo"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/mqtt/send": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 发布一条 MQTT 测试消息。
         * @description 发布一条 MQTT 测试消息。
         */
        get: operations["send_2"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/mcp/tools": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 查询外部 MCP Server 工具列表。
         * @description 查询外部 MCP Server 工具列表。
         */
        get: operations["tools"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/mcp/resource": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 读取外部 MCP 资源。
         * @description 读取外部 MCP 资源。
         */
        get: operations["resource"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/mcp/receive": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 调用外部 MCP 工具并模拟业务处理。
         * @description 调用外部 MCP 工具并模拟业务处理。
         */
        get: operations["receive"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/mail/sendSimpleMessage": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 发送邮件
         * @description 发送邮件
         */
        get: operations["sendSimpleMessage"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/mail/sendMessageWithAttachments": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 发送邮件（多附件）
         * @description 发送邮件（多附件）<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:oss:download`<br><br>
         */
        get: operations["sendMessageWithAttachments"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/mail/sendMessageWithAttachment": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 发送邮件（带附件）
         * @description 发送邮件（带附件）<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:oss:download`<br><br>
         */
        get: operations["sendMessageWithAttachment"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/i18n": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 通过code获取国际化内容
         *      code为 messages
         * @description 通过code获取国际化内容
         *      code为 messages.properties 中的 key
         *      <p>
         *      测试使用 user.register.success
         */
        get: operations["get_4"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/i18n/test2": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Bean 校验国际化
         *      不传值 分别查看异常返回
         * @description Bean 校验国际化
         *      不传值 分别查看异常返回
         *      <p>
         *      测试使用 not.null
         */
        get: operations["test2"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/i18n/test1": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Validator 校验国际化
         *      不传值 分别查看异常返回
         * @description Validator 校验国际化
         *      不传值 分别查看异常返回
         *      <p>
         *      测试使用 not.null
         */
        get: operations["test1"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/excel/exportWithOptions": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 导出下拉框
         * @description 导出下拉框
         */
        get: operations["exportWithOptions"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/excel/exportTemplateOne": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 单列表多数据
         * @description 单列表多数据
         */
        get: operations["exportTemplateOne"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/excel/exportTemplateMultiSheet": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 多个sheet导出
         * @description 多个sheet导出
         */
        get: operations["exportTemplateMultiSheet"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/excel/exportTemplateMuliti": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 多列表多数据
         * @description 多列表多数据
         */
        get: operations["exportTemplateMuliti"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/excel/customExport": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 自定义导出
         * @description 自定义导出
         */
        get: operations["customExport"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/encrypt": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 测试数据库加解密
         * @description 测试数据库加解密
         */
        get: operations["test_2"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/demo/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 获取测试单表详细信息
         * @description 获取测试单表详细信息<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `demo:demo:query`<br><br>
         */
        get: operations["getInfo_21"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/demo/page": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 自定义分页查询
         * @description 自定义分页查询<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `demo:demo:list`<br><br>
         */
        get: operations["page_2"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/demo/list": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 查询测试单表列表
         * @description 查询测试单表列表<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `demo:demo:list`<br><br>
         */
        get: operations["list_28"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/cache/test6": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 测试设置过期时间
         *      手动设置过期时间10秒
         *      11秒后获取 判断是否相等
         * @description 测试设置过期时间
         *      手动设置过期时间10秒
         *      11秒后获取 判断是否相等
         */
        get: operations["test6"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/cache/test3": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 测试 @CacheEvict
         * @description 测试 @CacheEvict
         *      <p>
         *      使用了CacheEvict注解的方法,会清空指定缓存
         *      「一般用在删除的方法上」
         *      <p>
         *      cacheNames 命名规则 查看 {@link CacheNames CacheNames} 注释 支持多参数
         */
        get: operations["test3"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/cache/test2": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 测试 @CachePut
         * @description 测试 @CachePut
         *      <p>
         *      加了@CachePut注解的方法,会把方法的返回值put到缓存里面缓存起来,供其它地方使用
         *      它「通常用在新增或者实时更新方法上」
         *      <p>
         *      cacheNames 命名规则 查看 {@link CacheNames CacheNames} 注释 支持多参数
         */
        get: operations["test2_1"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/cache/test1": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 测试 @Cacheable
         * @description 测试 @Cacheable
         *      <p>
         *      表示这个方法有了缓存的功能,方法的返回值会被缓存下来
         *      下一次调用该方法前,会去检查是否缓存中已经有值
         *      如果有就直接返回,不调用方法
         *      如果没有,就调用方法,然后把结果缓存起来
         *      这个注解「一般用在查询方法上」
         *      <p>
         *      重点说明: 缓存注解严谨与其他筛选数据功能一起使用
         *      例如: 数据权限注解 会造成 缓存击穿 与 数据不一致问题
         *      <p>
         *      cacheNames 命名规则 查看 {@link CacheNames CacheNames} 注释 支持多参数
         */
        get: operations["test1_1"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/auth/code": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 获取图片验证码。
         * @description 获取图片验证码。<br><h3>访问权限</h3><br>> **权限策略**：忽略权限检查<br>
         */
        get: operations["getCode"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/auth/client/context": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 查询客户端公开认证上下文。
         * @description 查询客户端公开认证上下文。
         *      <p>
         *      同时接受查询参数 <code>clientId</code> 与请求头 <code>clientid</code>（OAuth 客户端标识）。<br><h3>访问权限</h3><br>> **权限策略**：忽略权限检查<br>
         */
        get: operations["clientContext"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/auth/binding/{source}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 获取第三方绑定跳转地址。
         * @description 获取第三方绑定跳转地址。<br><h3>访问权限</h3><br>> **权限策略**：忽略权限检查<br>
         */
        get: operations["authBinding"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/snail/chat/my-agents": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["myAgents"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/snail/chat/conversations/messages": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["getMessages"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/snail/chat/messages": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["getMessages_1"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/snail/chat/config": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["config_1"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/snail/chat/agents": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["listAgents"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/snail/chat/agent": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["getAgent"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 访问首页时返回后端启动时间和当前访问时间。
         * @description 访问首页时返回后端启动时间和当前访问时间。<br><h3>访问权限</h3><br>> **权限策略**：忽略权限检查<br>
         */
        get: operations["index"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/spel/{ids}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        /**
         * 批量删除流程 SpEL 表达式定义。
         * @description 批量删除流程 SpEL 表达式定义。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `workflow:spel:remove`<br><br>
         */
        delete: operations["remove_9"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/leave/{ids}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        /**
         * 批量删除请假单。
         * @description 批量删除请假单。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `workflow:leave:remove`<br><br>
         */
        delete: operations["remove_10"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/instance/deleteHisByInstanceIds/{instanceIds}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        /**
         * 按实例 id 批量删除已完成的流程实例。
         * @description 按实例 id 批量删除已完成的流程实例。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `workflow:instance:remove`<br><br>
         */
        delete: operations["deleteHisByInstanceIds"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/instance/deleteByInstanceIds/{instanceIds}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        /**
         * 按实例 id 批量删除流程实例。
         * @description 按实例 id 批量删除流程实例。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `workflow:instance:remove`<br><br>
         */
        delete: operations["deleteByInstanceIds"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/instance/deleteByBusinessIds/{businessIds}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        /**
         * 按业务 id 批量删除流程实例。
         * @description 按业务 id 批量删除流程实例。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `workflow:instance:remove`<br><br>
         */
        delete: operations["deleteByBusinessIds"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workflow/definition/{ids}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        /**
         * 批量删除流程定义。
         * @description 批量删除流程定义。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `workflow:definition:remove`<br><br>
         */
        delete: operations["remove_11"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/userType/{userTypeIds}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        /**
         * 批量删除登录域。仍被引用时拒绝删除。
         * @description 批量删除登录域。仍被引用时拒绝删除。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:userType:remove`<br><br>
         */
        delete: operations["remove_12"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/user/{userIds}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        /**
         * 删除用户
         * @description 删除用户<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:user:remove`<br><br>
         */
        delete: operations["remove_13"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/role/{roleIds}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        /**
         * 删除角色
         * @description 删除角色<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:role:remove`<br><br>
         */
        delete: operations["remove_14"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/post/{postIds}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        /**
         * 删除岗位
         * @description 删除岗位<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:post:remove`<br><br>
         */
        delete: operations["remove_15"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/menu/cascade/{menuIds}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        /**
         * 批量级联删除菜单
         * @description 批量级联删除菜单<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:menu:remove`<br><br>**角色校验：**<br><br>- `superadmin`<br>
         */
        delete: operations["remove_16"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/dict/type/{dictIds}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        /**
         * 删除字典类型
         * @description 删除字典类型<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:dict:remove`<br><br>
         */
        delete: operations["remove_17"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/dict/type/refreshCache": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        /**
         * 刷新字典缓存。
         * @description 刷新字典缓存。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:dict:remove`<br><br>
         */
        delete: operations["refreshCache"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/dict/data/{dictCodes}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        /**
         * 删除字典数据
         * @description 删除字典数据<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:dict:remove`<br><br>
         */
        delete: operations["remove_18"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/config/{configIds}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        /**
         * 删除参数配置
         * @description 删除参数配置<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:config:remove`<br><br>
         */
        delete: operations["remove_19"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/config/refreshCache": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        /**
         * 刷新参数缓存。
         * @description 刷新参数缓存。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:config:remove`<br><br>
         */
        delete: operations["refreshCache_1"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/system/client/{ids}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        /**
         * 批量删除客户端配置。
         * @description 批量删除客户端配置。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:client:remove`<br><br>
         */
        delete: operations["remove_20"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/resource/oss/{ossIds}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        /**
         * 删除OSS对象存储
         * @description 删除OSS对象存储<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:oss:remove`<br><br>
         */
        delete: operations["remove_21"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/resource/oss/uploads/{uploadToken}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        /** @description <br><h3>访问权限</h3><br>**权限校验：**<br><br>- `system:oss:upload`<br><br> */
        delete: operations["abort"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/monitor/operlog/{operIds}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        /**
         * 批量删除操作日志记录
         * @description 批量删除操作日志记录<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `monitor:operlog:remove`<br><br>
         */
        delete: operations["remove_22"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/monitor/operlog/clean": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        /**
         * 清空操作日志记录。
         * @description 清空操作日志记录。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `monitor:operlog:remove`<br><br>
         */
        delete: operations["clean"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/monitor/online/{tokenId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        /**
         * 按 token 强制用户下线，适用于管理员踢除异常会话。
         * @description 按 token 强制用户下线，适用于管理员踢除异常会话。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `monitor:online:forceLogout`<br><br>
         */
        delete: operations["forceLogout"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/monitor/online/myself/{tokenId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        /**
         * 强退当前账号下指定在线设备，避免误踢其他账号的会话。
         * @description 强退当前账号下指定在线设备，避免误踢其他账号的会话。
         */
        delete: operations["remove_23"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/monitor/loginInfo/{infoIds}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        /**
         * 批量删除登录日志
         * @description 批量删除登录日志<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `monitor:logininfo:remove`<br><br>
         */
        delete: operations["remove_24"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/monitor/loginInfo/clean": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        /**
         * 清空系统访问记录。
         * @description 清空系统访问记录。<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `monitor:logininfo:remove`<br><br>
         */
        delete: operations["clean_1"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/tree/{ids}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        /**
         * 删除测试树表
         * @description 删除测试树表<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `demo:tree:remove`<br><br>
         */
        delete: operations["remove_25"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/demo/{ids}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        /**
         * 删除测试单表
         * @description 删除测试单表<br><h3>访问权限</h3><br>**权限校验：**<br><br>- `demo:demo:remove`<br><br>
         */
        delete: operations["remove_26"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/demo/batch": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        /**
         * 删除批量方法
         * @description 删除批量方法
         */
        delete: operations["remove_27"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/auth/unlock/{socialId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        /**
         * 取消当前用户的社交账号授权。
         * @description 取消当前用户的社交账号授权。<br><h3>访问权限</h3><br>> **权限策略**：忽略权限检查<br>
         */
        delete: operations["unlockSocial"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
}
export type webhooks = Record<string, never>;
export interface components {
    schemas: {
        ErrorInfo: {
            code?: string;
            args?: {
                [key: string]: unknown;
            };
            field?: string;
            violations?: components["schemas"]["ErrorInfo"][];
        };
        /** @description 响应信息主体 */
        RVoid: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: unknown;
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 流程spel表达式定义业务对象 flow_spel */
        FlowSpelBo: {
            /**
             * Format: int64
             * @description 主键id
             */
            id?: number;
            /** @description 组件名称 */
            componentName?: string;
            /** @description 方法名 */
            methodName?: string;
            /** @description 参数 */
            methodParams?: string;
            /** @description 预览spel值 */
            viewSpel: string;
            /** @description 状态（0正常 1停用） */
            status: string;
            /** @description 备注 */
            remark?: string;
            /** @description 请求参数 */
            params?: {
                [key: string]: unknown;
            };
        };
        /** @description 请假业务对象 test_leave */
        TestLeaveBo: {
            /**
             * Format: int64
             * @description 主键
             */
            id: number;
            /** @description 流程code */
            flowCode?: string;
            /** @description 申请编号 */
            applyCode?: string;
            /** @description 请假类型 */
            leaveType: string;
            /**
             * Format: date-time
             * @description 开始时间
             */
            startDate: string;
            /**
             * Format: date-time
             * @description 结束时间
             */
            endDate: string;
            /**
             * Format: int32
             * @description 请假天数
             */
            leaveDays?: number;
            /**
             * Format: int32
             * @description 开始时间
             */
            startLeaveDays?: number;
            /**
             * Format: int32
             * @description 结束时间
             */
            endLeaveDays?: number;
            /** @description 请假原因 */
            remark?: string;
            /** @description 状态 */
            status?: string;
            /** @description 请求参数 */
            params?: {
                [key: string]: unknown;
            };
        };
        /** @description 响应信息主体 */
        RTestLeaveVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["TestLeaveVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 请假视图对象 test_leave */
        TestLeaveVo: {
            /**
             * Format: int64
             * @description 主键
             */
            id?: number;
            /** @description 申请编号 */
            applyCode?: string;
            /** @description 请假类型 */
            leaveType?: string;
            /**
             * Format: date-time
             * @description 开始时间
             */
            startDate?: string;
            /**
             * Format: date-time
             * @description 结束时间
             */
            endDate?: string;
            /**
             * Format: int32
             * @description 请假天数
             */
            leaveDays?: number;
            /** @description 备注 */
            remark?: string;
            /** @description 状态 */
            status?: string;
        };
        /** @description 流程变量参数 */
        FlowVariableBo: {
            /**
             * Format: int64
             * @description 流程实例 ID
             */
            instanceId?: number;
            /** @description 变量键 */
            key?: string;
            /** @description 变量值 */
            value?: string;
        };
        /** @description 撤销流程请求对象。 */
        FlowCancelBo: {
            /** @description 业务 ID */
            businessId?: string;
            /** @description 撤销说明 */
            message?: string;
        };
        /** @description 响应信息主体 */
        RBoolean: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: boolean;
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        FlowDefinition: {
            /** Format: int64 */
            id?: number;
            /** Format: date-time */
            createTime?: string;
            /** Format: date-time */
            updateTime?: string;
            createBy?: string;
            updateBy?: string;
            tenantId?: string;
            delFlag?: string;
            flowCode?: string;
            flowName?: string;
            modelValue?: string;
            category?: string;
            version?: string;
            /** Format: int32 */
            isPublish?: number;
            formCustom?: string;
            formPath?: string;
            /** Format: int32 */
            activityStatus?: number;
            listenerType?: string;
            listenerPath?: string;
            ext?: string;
            nodeList?: components["schemas"]["Node"][];
            userList?: components["schemas"]["User"][];
        };
        Node: {
            /** Format: int64 */
            id?: number;
            version?: string;
            /** Format: int32 */
            nodeType?: number;
            ext?: string;
            delFlag?: string;
            updateBy?: string;
            /** Format: date-time */
            createTime?: string;
            createBy?: string;
            /** Format: date-time */
            updateTime?: string;
            nodeCode?: string;
            nodeName?: string;
            nodeRatio?: string;
            /** Format: int64 */
            definitionId?: number;
            formCustom?: string;
            formPath?: string;
            permissionFlag?: string;
            anyNodeSkip?: string;
            tenantId?: string;
            listenerPath?: string;
            skipList?: components["schemas"]["Skip"][];
            coordinate?: string;
            listenerType?: string;
        };
        Skip: {
            /** Format: int64 */
            id?: number;
            /** Format: int64 */
            nodeId?: number;
            delFlag?: string;
            updateBy?: string;
            /** Format: date-time */
            createTime?: string;
            createBy?: string;
            /** Format: date-time */
            updateTime?: string;
            /** Format: int64 */
            definitionId?: number;
            nextNodeCode?: string;
            nowNodeCode?: string;
            tenantId?: string;
            skipName?: string;
            skipType?: string;
            skipCondition?: string;
            coordinate?: string;
            /** Format: int32 */
            nowNodeType?: number;
            /** Format: int32 */
            nextNodeType?: number;
        };
        User: {
            /** Format: int64 */
            id?: number;
            delFlag?: string;
            updateBy?: string;
            /** Format: date-time */
            createTime?: string;
            createBy?: string;
            /** Format: date-time */
            updateTime?: string;
            /** Format: int64 */
            associated?: number;
            processedBy?: string;
            tenantId?: string;
            type?: string;
        };
        /** @description 流程分类业务对象 wf_category */
        FlowCategoryBo: {
            /**
             * Format: int64
             * @description 流程分类ID
             */
            categoryId: number;
            /**
             * Format: int64
             * @description 父流程分类id
             */
            parentId: number;
            /** @description 流程分类名称 */
            categoryName: string;
            /**
             * Format: int64
             * @description 显示顺序
             */
            orderNum?: number;
        };
        /** @description 用户信息业务对象 sys_user */
        SysUserBo: {
            /**
             * Format: int64
             * @description 用户ID
             */
            userId?: number;
            /**
             * Format: int64
             * @description 部门ID
             */
            deptId?: number;
            /** @description 用户账号 */
            userName: string;
            /** @description 账号、昵称或手机号的统一模糊检索词，仅用于查询。 */
            keyword?: string;
            /** @description 用户昵称 */
            nickName: string;
            /** @description 用户邮箱 */
            email?: string;
            /** @description 手机号码 */
            phoneNumber?: string;
            /** @description 用户性别（0男 1女 2未知） */
            gender?: string;
            /**
             * Format: int64
             * @description 头像 OSS ID
             */
            avatar?: number;
            /** @description 密码 */
            password?: string;
            /** @description 账号状态（0正常 1停用） */
            status?: string;
            /** @description 备注 */
            remark?: string;
            /** @description 角色组 */
            roleIds?: number[];
            /** @description 岗位组 */
            postIds?: number[];
            /** @description 登录域ID列表 */
            userTypeIds?: number[];
            /**
             * Format: int64
             * @description 数据权限 当前角色ID
             */
            roleId?: number;
            /** @description 用户ID */
            userIds?: string;
            /** @description 排除不查询的用户(工作流用) */
            excludeUserIds?: string;
            /**
             * Format: int64
             * @description 创建者
             */
            createBy?: number;
            /**
             * Format: int64
             * @description 更新者
             */
            updateBy?: number;
            /** @description 请求参数 */
            params?: {
                [key: string]: unknown;
            };
            /** @description 判断当前用户是否为超级管理员。 */
            superAdmin?: boolean;
        };
        /** @description 登录域业务对象 sys_user_type */
        SysUserTypeBo: {
            /**
             * Format: int64
             * @description 登录域ID
             */
            userTypeId: number;
            /** @description 登录域编码 */
            userTypeCode?: string;
            /** @description 登录域名称 */
            userTypeName: string;
            /**
             * Format: int32
             * @description 显示顺序
             */
            orderNum: number;
            /** @description 状态（0正常 1停用） */
            status?: string;
            /** @description 备注 */
            remark?: string;
            /** @description 请求参数 */
            params?: {
                [key: string]: unknown;
            };
        };
        /** @description 个人信息业务处理 */
        SysUserProfileBo: {
            /** @description 用户昵称 */
            nickName?: string;
            /** @description 用户邮箱 */
            email?: string;
            /** @description 手机号码 */
            phoneNumber?: string;
            /** @description 用户性别（0男 1女 2未知） */
            gender?: string;
            /**
             * Format: int64
             * @description 头像 OSS ID
             */
            avatar?: number;
        };
        /** @description 用户密码修改 */
        SysUserPasswordBo: {
            /** @description 旧密码 */
            oldPassword: string;
            /** @description 新密码 */
            newPassword: string;
        };
        /** @description 角色信息业务对象 sys_role */
        SysRoleBo: {
            /**
             * Format: int64
             * @description 角色ID
             */
            roleId?: number;
            /**
             * Format: int64
             * @description 归属客户端主键
             */
            clientId: number;
            /** @description 角色名称 */
            roleName: string;
            /** @description 角色权限字符串 */
            roleKey: string;
            /**
             * Format: int32
             * @description 显示顺序
             */
            roleSort: number;
            /** @description 数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限 5：仅本人数据权限 6：部门及以下或本人数据权限） */
            dataScope?: string;
            /** @description 菜单树选择项是否关联显示 */
            menuCheckStrictly?: boolean;
            /** @description 部门树选择项是否关联显示 */
            deptCheckStrictly?: boolean;
            /** @description 角色状态（0正常 1停用） */
            status?: string;
            /** @description 备注 */
            remark?: string;
            /** @description 菜单组 */
            menuIds?: number[];
            /** @description 部门组（数据权限） */
            deptIds?: number[];
            /** @description 请求参数 */
            params?: {
                [key: string]: unknown;
            };
            /** @description 判断当前角色是否为超级管理员角色。 */
            superAdmin?: boolean;
        };
        /** @description 用户和角色关联 sys_user_role */
        SysUserRole: {
            /**
             * Format: int64
             * @description 用户ID
             */
            userId?: number;
            /**
             * Format: int64
             * @description 角色ID
             */
            roleId?: number;
        };
        /** @description 岗位信息业务对象 sys_post */
        SysPostBo: {
            /**
             * Format: int64
             * @description 岗位ID
             */
            postId?: number;
            /**
             * Format: int64
             * @description 部门id（单部门）
             */
            deptId: number;
            /**
             * Format: int64
             * @description 归属部门id（部门树）
             */
            belongDeptId?: number;
            /** @description 岗位编码 */
            postCode: string;
            /** @description 岗位名称 */
            postName: string;
            /** @description 岗位类别编码 */
            postCategory?: string;
            /**
             * Format: int32
             * @description 显示顺序
             */
            postSort: number;
            /** @description 状态（0正常 1停用） */
            status?: string;
            /** @description 备注 */
            remark?: string;
            /** @description 请求参数 */
            params?: {
                [key: string]: unknown;
            };
        };
        /** @description 菜单权限业务对象 sys_menu */
        SysMenuBo: {
            /**
             * Format: int64
             * @description 菜单ID
             */
            menuId?: number;
            /**
             * Format: int64
             * @description 归属客户端主键
             */
            clientId: number;
            /**
             * Format: int64
             * @description 父菜单ID
             */
            parentId?: number;
            /** @description 菜单名称 */
            menuName: string;
            /**
             * Format: int32
             * @description 显示顺序
             */
            orderNum: number;
            /** @description 路由地址 */
            path?: string;
            /** @description 组件路径 */
            component?: string;
            /** @description 路由参数 */
            queryParam?: string;
            /** @description 是否为外链（Y是 N否） */
            isFrame?: string;
            /** @description 是否缓存（Y缓存 N不缓存） */
            isCache?: string;
            /** @description 菜单类型（M目录 C菜单 F按钮） */
            menuType: string;
            /** @description 显示状态（0显示 1隐藏） */
            visible?: string;
            /** @description 菜单状态（0正常 1停用） */
            status?: string;
            /** @description 权限标识 */
            perms?: string;
            /** @description 菜单图标 */
            icon?: string;
            /** @description 激活菜单路径 */
            activeMenu?: string;
            /** @description 扩展字段 */
            ext?: string;
            /** @description 备注 */
            remark?: string;
        };
        /** @description 字典类型业务对象 sys_dict_type */
        SysDictTypeBo: {
            /**
             * Format: int64
             * @description 字典主键
             */
            dictId?: number;
            /** @description 字典名称 */
            dictName: string;
            /** @description 字典类型 */
            dictType: string;
            /** @description 备注 */
            remark?: string;
        };
        /** @description 字典数据业务对象 sys_dict_data */
        SysDictDataBo: {
            /**
             * Format: int64
             * @description 字典编码
             */
            dictCode?: number;
            /**
             * Format: int32
             * @description 字典排序
             */
            dictSort?: number;
            /** @description 字典标签 */
            dictLabel: string;
            /** @description 字典键值 */
            dictValue: string;
            /** @description 字典类型 */
            dictType: string;
            /** @description 样式属性（其他样式扩展） */
            cssClass?: string;
            /** @description 表格回显样式 */
            listClass?: string;
            /** @description 是否默认（Y是 N否） */
            isDefault?: string;
            /**
             * Format: int64
             * @description 创建部门
             */
            createDept?: number;
            /** @description 备注 */
            remark?: string;
        };
        /** @description 部门业务对象 sys_dept */
        SysDeptBo: {
            /**
             * Format: int64
             * @description 部门id
             */
            deptId?: number;
            /**
             * Format: int64
             * @description 父部门ID
             */
            parentId?: number;
            /** @description 部门名称 */
            deptName: string;
            /** @description 部门类别编码 */
            deptCategory?: string;
            /**
             * Format: int32
             * @description 显示顺序
             */
            orderNum: number;
            /**
             * Format: int64
             * @description 负责人
             */
            leader?: number;
            /** @description 联系电话 */
            phone?: string;
            /** @description 邮箱 */
            email?: string;
            /** @description 部门状态（0正常 1停用） */
            status?: string;
            /**
             * Format: int64
             * @description 归属部门id（部门树）
             */
            belongDeptId?: number;
            /** @description 请求参数 */
            params?: {
                [key: string]: unknown;
            };
        };
        /** @description 参数配置业务对象 sys_config */
        SysConfigBo: {
            /**
             * Format: int64
             * @description 参数主键
             */
            configId?: number;
            /** @description 参数名称 */
            configName: string;
            /** @description 参数键名 */
            configKey: string;
            /** @description 参数键值 */
            configValue: string;
            /** @description 系统内置（Y是 N否） */
            configType?: string;
            /** @description 备注 */
            remark?: string;
            /** @description 请求参数 */
            params?: {
                [key: string]: unknown;
            };
        };
        /** @description 授权管理业务对象 sys_client */
        SysClientBo: {
            /**
             * Format: int64
             * @description id
             */
            id: number;
            /** @description 客户端id */
            clientId?: string;
            /** @description 客户端key */
            clientKey: string;
            /** @description 客户端秘钥 */
            clientSecret: string;
            /** @description 授权类型 */
            grantTypeList: string[];
            /** @description 授权类型 */
            grantType?: string;
            /** @description 设备类型 */
            deviceType?: string;
            /** @description 允许访问路径 */
            accessPath?: string;
            /** @description 允许访问路径列表 */
            accessPathList?: string[];
            /** @description IP白名单 */
            ipWhitelist?: string;
            /** @description IP白名单列表 */
            ipWhitelistList?: string[];
            /**
             * Format: int64
             * @description token活跃超时时间
             */
            activeTimeout?: number;
            /**
             * Format: int64
             * @description token固定超时时间
             */
            timeout?: number;
            /**
             * Format: int64
             * @description 登录域ID
             */
            userTypeId: number;
            /** @description 是否开放公开注册 */
            registerEnabled?: boolean;
            /**
             * Format: int64
             * @description 默认角色ID
             */
            defaultRoleId?: number;
            /** @description 状态（0正常 1停用） */
            status?: string;
        };
        /** @description 测试树表业务对象 test_tree */
        TestTreeBo: {
            /**
             * Format: int64
             * @description 主键
             */
            id: number;
            /**
             * Format: int64
             * @description 父ID
             */
            parentId?: number;
            /**
             * Format: int64
             * @description 部门id
             */
            deptId: number;
            /**
             * Format: int64
             * @description 用户id
             */
            userId: number;
            /** @description 树节点名 */
            treeName: string;
        };
        /** @description 测试单表业务对象 test_demo */
        TestDemoBo: {
            /**
             * Format: int64
             * @description 主键
             */
            id: number;
            /**
             * Format: int64
             * @description 部门id
             */
            deptId: number;
            /**
             * Format: int64
             * @description 用户id
             */
            userId: number;
            /**
             * Format: int32
             * @description 排序号
             */
            orderNum: number;
            /** @description key键 */
            testKey: string;
            /** @description 值 */
            value: string;
            /**
             * Format: int64
             * @description 版本
             */
            version?: number;
        };
        OpenApiConversationUpdateTitleRequest: {
            /** Format: int64 */
            agentId: number;
            conversationId: string;
            openId: string;
            title: string;
        };
        ResultVoid: {
            /** Format: int32 */
            status?: number;
            message?: string;
            data?: unknown;
        };
        /** @description 流程变量参数 */
        FlowUrgeTaskBo: {
            /** @description 任务id */
            taskIdList?: number[];
            /** @description 消息类型 */
            messageType?: string[];
            /** @description 催办内容 */
            message?: string;
        };
        /** @description 终止任务请求对象 */
        FlowTerminationBo: {
            /**
             * Format: int64
             * @description 任务 ID
             */
            taskId?: number;
            /** @description 终止意见 */
            comment?: string;
        };
        /**
         * @description 任务操作业务对象，用于描述任务委派、转办、加签等操作的必要参数
         *      包含了用户ID、任务ID、任务相关的消息、以及加签/减签的用户ID
         */
        TaskOperationBo: {
            /** @description 委派/转办人的用户ID（必填，准对委派/转办人操作） */
            userId?: string;
            /** @description 加签/减签人的用户ID列表（必填，针对加签/减签操作） */
            userIds?: string[];
            /**
             * Format: int64
             * @description 任务ID（必填）
             */
            taskId: number;
            /** @description 消息类型 */
            messageType?: string[];
            /** @description 意见或备注信息（可选） */
            message?: string;
        };
        /** @description 流程实例业务扩展对象 flow_instance_biz_ext */
        FlowInstanceBizExt: {
            /**
             * Format: int64
             * @description 创建部门
             */
            createDept?: number;
            /**
             * Format: int64
             * @description 创建者
             */
            createBy?: number;
            /**
             * Format: date-time
             * @description 创建时间
             */
            createTime?: string;
            /**
             * Format: int64
             * @description 更新者
             */
            updateBy?: number;
            /**
             * Format: date-time
             * @description 更新时间
             */
            updateTime?: string;
            /**
             * Format: int64
             * @description 主键
             */
            id?: number;
            /**
             * Format: int64
             * @description 流程实例ID
             */
            instanceId?: number;
            /** @description 业务ID */
            businessId?: string;
            /** @description 业务编码 */
            businessCode?: string;
            /** @description 业务标题 */
            businessTitle?: string;
            /** @description 删除标志（0代表存在 1代表删除） */
            delFlag?: string;
        };
        /** @description 启动流程对象 */
        StartProcessBo: {
            /** @description 业务唯一值id */
            businessId: string;
            /** @description 流程定义编码 */
            flowCode: string;
            /** @description 办理人(可不填 用于覆盖当前节点办理人) */
            handler?: string;
            /** @description 流程变量，前端会提交一个元素{'entity': {业务详情数据对象}} */
            variables?: {
                [key: string]: unknown;
            };
            /** @description 流程业务扩展信息 */
            bizExt?: components["schemas"]["FlowInstanceBizExt"];
        };
        /** @description 响应信息主体 */
        RStartProcessReturnDTO: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["StartProcessReturnDTO"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 启动流程后的返回结果对象。 */
        StartProcessReturnDTO: {
            /**
             * Format: int64
             * @description 流程实例 ID
             */
            processInstanceId?: number;
            /**
             * Format: int64
             * @description 首个任务 ID
             */
            taskId?: number;
        };
        /** @description 查询下一节点信息的请求对象。 */
        FlowNextNodeBo: {
            /**
             * Format: int64
             * @description 任务id
             */
            taskId?: number;
            /** @description 流程变量 */
            variables?: {
                [key: string]: unknown;
            };
        };
        FlowNode: {
            skipList?: components["schemas"]["Skip"][];
            /** Format: int64 */
            id?: number;
            /** Format: date-time */
            createTime?: string;
            /** Format: date-time */
            updateTime?: string;
            createBy?: string;
            updateBy?: string;
            tenantId?: string;
            delFlag?: string;
            /** Format: int32 */
            nodeType?: number;
            /** Format: int64 */
            definitionId?: number;
            nodeCode?: string;
            nodeName?: string;
            permissionFlag?: string;
            nodeRatio?: string;
            coordinate?: string;
            version?: string;
            anyNodeSkip?: string;
            listenerType?: string;
            listenerPath?: string;
            formCustom?: string;
            formPath?: string;
            ext?: string;
        };
        /** @description 响应信息主体 */
        RListFlowNode: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["FlowNode"][];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 办理任务请求对象 */
        CompleteTaskBo: {
            /**
             * Format: int64
             * @description 任务id
             */
            taskId: number;
            /** @description 附件id */
            fileId?: string;
            /** @description 抄送人员 */
            flowCopyList?: components["schemas"]["FlowCopyBo"][];
            /** @description 消息类型 */
            messageType?: string[];
            /** @description 办理意见 */
            message?: string;
            /** @description 消息通知 */
            notice?: string;
            /** @description 办理人(可不填 用于覆盖当前节点办理人) */
            handler?: string;
            /** @description 流程变量 */
            variables?: {
                [key: string]: unknown;
            };
            /** @description 弹窗选择的办理人 */
            assigneeMap?: {
                [key: string]: unknown;
            };
            /** @description 扩展变量(此处为逗号分隔的ossId) */
            ext?: string;
        };
        /** @description 流程抄送请求对象。 */
        FlowCopyBo: {
            /**
             * Format: int64
             * @description 用户id
             */
            userId?: number;
            /** @description 用户昵称 */
            nickName?: string;
        };
        /** @description 驳回参数请求 */
        BackProcessBo: {
            /**
             * Format: int64
             * @description 任务ID
             */
            taskId: number;
            /** @description 附件id */
            fileId?: string;
            /** @description 消息类型 */
            messageType?: string[];
            /** @description 驳回的节点id(目前未使用，直接驳回到申请人) */
            nodeCode?: string;
            /** @description 办理意见 */
            message?: string;
            /** @description 通知 */
            notice?: string;
            /** @description 流程变量 */
            variables?: {
                [key: string]: unknown;
            };
        };
        /** @description 作废流程请求对象。 */
        FlowInvalidBo: {
            /**
             * Format: int64
             * @description 流程实例 ID
             */
            id?: number;
            /** @description 作废意见 */
            comment?: string;
        };
        DefJson: {
            /** Format: int64 */
            id?: number;
            flowCode?: string;
            flowName?: string;
            modelValue?: string;
            category?: string;
            version?: string;
            /** Format: int32 */
            isPublish?: number;
            formCustom?: string;
            formPath?: string;
            listenerType?: string;
            listenerPath?: string;
            instance?: components["schemas"]["Instance"];
            ext?: string;
            extMap?: {
                [key: string]: unknown;
            };
            nodeList?: components["schemas"]["NodeJson"][];
            chartStatusColor?: string[];
            topText?: string;
            topTextShow?: boolean;
            createBy?: string;
            updateBy?: string;
            categoryList?: components["schemas"]["Tree"][];
            formPathList?: components["schemas"]["Tree"][];
        };
        InfoItem: {
            prefix?: string;
            prefixStyle?: {
                [key: string]: unknown;
            };
            content?: string;
            contentStyle?: {
                [key: string]: unknown;
            };
            rowStyle?: {
                [key: string]: unknown;
            };
        };
        Instance: {
            /** Format: int64 */
            id?: number;
            /** Format: int32 */
            nodeType?: number;
            variable?: string;
            ext?: string;
            delFlag?: string;
            updateBy?: string;
            /** Format: date-time */
            createTime?: string;
            createBy?: string;
            /** Format: date-time */
            updateTime?: string;
            flowStatus?: string;
            nodeCode?: string;
            businessId?: string;
            nodeName?: string;
            flowName?: string;
            variableMap?: {
                [key: string]: unknown;
            };
            /** Format: int64 */
            definitionId?: number;
            formCustom?: string;
            /** Format: int32 */
            activityStatus?: number;
            formPath?: string;
            tenantId?: string;
            defJson?: string;
        };
        NodeJson: {
            /** Format: int32 */
            nodeType?: number;
            nodeCode?: string;
            nodeName?: string;
            version?: string;
            permissionFlag?: string;
            nodeRatio?: string;
            coordinate?: string;
            anyNodeSkip?: string;
            listenerType?: string;
            listenerPath?: string;
            formCustom?: string;
            formPath?: string;
            ext?: string;
            /** Format: int32 */
            status?: number;
            extMap?: {
                [key: string]: unknown;
            };
            promptContent?: components["schemas"]["PromptContent"];
            skipList?: components["schemas"]["SkipJson"][];
            createBy?: string;
            updateBy?: string;
        };
        PromptContent: {
            dialogStyle?: {
                [key: string]: unknown;
            };
            info?: components["schemas"]["InfoItem"][];
        };
        SkipJson: {
            nowNodeCode?: string;
            nextNodeCode?: string;
            skipName?: string;
            skipType?: string;
            skipCondition?: string;
            coordinate?: string;
            /** Format: int32 */
            status?: number;
            extMap?: {
                [key: string]: unknown;
            };
            promptContent?: string[];
            createBy?: string;
            updateBy?: string;
        };
        Tree: {
            id?: string;
            name?: string;
            parentId?: string;
            children?: components["schemas"]["Tree"][];
        };
        ApiResultVoid: {
            /** Format: int32 */
            code?: number;
            msg?: string;
            data?: unknown;
        };
        FlowDto: {
            /** Format: int64 */
            id?: number;
            formContent?: string;
            form?: components["schemas"]["Form"];
            data?: unknown;
        };
        Form: {
            /** Format: int64 */
            id?: number;
            version?: string;
            formContent?: string;
            ext?: string;
            delFlag?: string;
            updateBy?: string;
            /** Format: date-time */
            createTime?: string;
            createBy?: string;
            /** Format: date-time */
            updateTime?: string;
            /** Format: int32 */
            isPublish?: number;
            formPath?: string;
            tenantId?: string;
            /** Format: int32 */
            formType?: number;
            formCode?: string;
            formName?: string;
        };
        ApiResultInstance: {
            /** Format: int32 */
            code?: number;
            msg?: string;
            data?: components["schemas"]["Instance"];
        };
        ThirdProviderBo: {
            /** Format: int64 */
            providerId?: number;
            providerCode: string;
            providerName: string;
            baseUrl: string;
            status?: string;
            /** Format: int32 */
            timeoutConnectMs?: number;
            /** Format: int32 */
            timeoutReadMs?: number;
            /** Format: int32 */
            rateLimit?: number;
            /** Format: int32 */
            concurrencyLimit?: number;
            sharedHeadersJson?: string;
            remark?: string;
        };
        ThirdEndpointBo: {
            /** Format: int64 */
            endpointId?: number;
            /** Format: int64 */
            providerId: number;
            providerCode: string;
            endpointCode: string;
            endpointName: string;
            httpMethod: string;
            relativePath: string;
            requestMode: string;
            responseMode: string;
            pathSchemaJson?: string;
            querySchemaJson?: string;
            headerSchemaJson?: string;
            bodySchemaJson?: string;
            responseSchemaJson?: string;
            overrideJson?: string;
            status?: string;
            idempotent?: boolean;
            /** Format: int32 */
            rateLimit?: number;
            /** Format: int32 */
            concurrencyLimit?: number;
            /** Format: int32 */
            retryCount?: number;
            sensitiveFieldsJson?: string;
            adapterCode?: string;
        };
        ThirdCredentialBo: {
            /** Format: int64 */
            credentialId?: number;
            providerCode: string;
            endpointCode?: string;
            credentialType: string;
            secretJson: string;
            enabled: boolean;
            /** Format: date-time */
            expiresAt?: string;
        };
        /** @description 临时密码签发请求。 */
        TemporaryPasswordIssueBo: {
            /**
             * Format: int64
             * @description 目标用户 ID
             */
            userId: number;
        };
        /** @description 响应信息主体 */
        RTemporaryPasswordVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["TemporaryPasswordVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 一次性临时密码签发响应。 */
        TemporaryPasswordVo: {
            /** @description 仅本次响应展示的明文 */
            password?: string;
            /**
             * Format: int64
             * @description 有效秒数
             */
            expiresInSeconds?: number;
        };
        /** @description 永久密码重置候选请求。 */
        ResetPasswordCandidateBo: {
            /**
             * Format: int64
             * @description 目标用户 ID
             */
            userId: number;
        };
        /** @description 响应信息主体 */
        RResetPasswordCandidateVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["ResetPasswordCandidateVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 用户初始化或永久重置使用的可编辑密码候选。 */
        ResetPasswordCandidateVo: {
            /** @description 用户信息 */
            user?: components["schemas"]["SysUserVo"];
            /** @description 角色ID列表 */
            roleIds?: number[];
            /** @description 角色列表 */
            roles?: components["schemas"]["SysRoleVo"][];
            /** @description 岗位ID列表 */
            postIds?: number[];
            /** @description 岗位列表 */
            posts?: components["schemas"]["SysPostVo"][];
            password?: string;
        };
        /** @description 岗位信息视图对象 sys_post */
        SysPostVo: {
            /**
             * Format: int64
             * @description 岗位ID
             */
            postId?: number;
            /**
             * Format: int64
             * @description 部门id
             */
            deptId?: number;
            /** @description 岗位编码 */
            postCode?: string;
            /** @description 岗位名称 */
            postName?: string;
            /** @description 岗位类别编码 */
            postCategory?: string;
            /**
             * Format: int32
             * @description 显示顺序
             */
            postSort?: number;
            /** @description 状态（0正常 1停用） */
            status?: string;
            /** @description 备注 */
            remark?: string;
            /**
             * Format: date-time
             * @description 创建时间
             */
            createTime?: string;
            /** @description 部门名 */
            deptName?: string;
        };
        /** @description 角色信息视图对象 sys_role */
        SysRoleVo: {
            /**
             * Format: int64
             * @description 角色ID
             */
            roleId?: number;
            /**
             * Format: int64
             * @description 归属客户端主键
             */
            clientId?: number;
            /** @description 是否为当前客户端默认角色 */
            clientDefault?: boolean;
            /** @description 角色名称 */
            roleName?: string;
            /** @description 角色权限字符串 */
            roleKey?: string;
            /**
             * Format: int32
             * @description 显示顺序
             */
            roleSort?: number;
            /** @description 数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限 5：仅本人数据权限 6：部门及以下或本人数据权限） */
            dataScope?: string;
            /** @description 菜单树选择项是否关联显示 */
            menuCheckStrictly?: boolean;
            /** @description 部门树选择项是否关联显示 */
            deptCheckStrictly?: boolean;
            /** @description 角色状态（0正常 1停用） */
            status?: string;
            /** @description 备注 */
            remark?: string;
            /**
             * Format: date-time
             * @description 创建时间
             */
            createTime?: string;
            /** @description 用户是否存在此角色标识 默认不存在 */
            flag?: boolean;
            /** @description 判断当前角色是否为超级管理员角色。 */
            superAdmin?: boolean;
        };
        /** @description 用户信息视图对象 sys_user */
        SysUserVo: {
            /**
             * Format: int64
             * @description 用户ID
             */
            userId?: number;
            /**
             * Format: int64
             * @description 部门ID
             */
            deptId?: number;
            /** @description 用户账号 */
            userName?: string;
            /** @description 用户昵称 */
            nickName?: string;
            /** @description 用户邮箱 */
            email?: string;
            /** @description 手机号码 */
            phoneNumber?: string;
            /** @description 用户性别（0男 1女 2未知） */
            gender?: string;
            /**
             * Format: int64
             * @description 头像 OSS ID
             */
            avatar?: number;
            /** @description 头像地址 */
            avatarUrl?: string;
            /** @description 账号状态（0正常 1停用） */
            status?: string;
            /** @description 最后登录IP */
            loginIp?: string;
            /**
             * Format: date-time
             * @description 最后登录时间
             */
            loginDate?: string;
            /** @description 备注 */
            remark?: string;
            /**
             * Format: date-time
             * @description 创建时间
             */
            createTime?: string;
            /**
             * Format: date-time
             * @description 更新时间
             */
            updateTime?: string;
            /** @description 部门名 */
            deptName?: string;
            /** @description 角色对象 */
            roles?: components["schemas"]["SysRoleVo"][];
            /** @description 角色组 */
            roleIds?: number[];
            /** @description 岗位组 */
            postIds?: number[];
            /** @description 登录域ID列表 */
            userTypeIds?: number[];
            /** @description 登录域编码列表 */
            userTypeCodes?: string[];
            /** @description 登录域名称列表 */
            userTypeNames?: string[];
            /**
             * Format: int64
             * @description 数据权限 当前角色ID
             */
            roleId?: number;
        };
        /** @description One-time create/reset result. AppSecret is never part of a summary model. */
        OpenApiCredentialIssued: {
            /** Format: int64 */
            credentialId?: number;
            /** Format: int64 */
            ownerUserId?: number;
            appKey?: string;
            appSecret?: string;
            appName?: string;
            status?: string;
            /** Format: date-time */
            expiresAt?: string;
            remark?: string;
            /** Format: date-time */
            createTime?: string;
            /** Format: date-time */
            updateTime?: string;
        };
        /** @description 响应信息主体 */
        ROpenApiCredentialIssued: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["OpenApiCredentialIssued"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description Persisted credential fields safe for repeated transport. */
        OpenApiCredentialSummary: {
            /** Format: int64 */
            credentialId?: number;
            /** Format: int64 */
            ownerUserId?: number;
            appKey?: string;
            appName?: string;
            status?: string;
            /** Format: date-time */
            expiresAt?: string;
            remark?: string;
            /** Format: date-time */
            createTime?: string;
            /** Format: date-time */
            updateTime?: string;
        };
        /** @description 响应信息主体 */
        ROpenApiCredentialSummary: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["OpenApiCredentialSummary"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description Credential creation input. The owner is deliberately absent. */
        OpenApiCredentialCreateRequest: {
            appName: string;
            /** Format: date-time */
            expiresAt?: string;
            remark?: string;
        };
        /** @description 响应信息主体 */
        RString: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: string;
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        OpenApiUserVO: {
            openId?: string;
            externalId?: string;
            nickname?: string;
            avatarUrl?: string;
            created?: boolean;
        };
        /** @description 响应信息主体 */
        ROpenApiUserVO: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["OpenApiUserVO"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        InitRequest: {
            policy: string;
            fileName: string;
            /** Format: int64 */
            fileSize?: number;
            contentType: string;
            fingerprint: string;
        };
        InitResponse: {
            uploadToken?: string;
            /** @enum {string} */
            mode?: "AUTO" | "SINGLE" | "MULTIPART";
            /** Format: date-time */
            expiresAt?: string;
            presignedRequest?: components["schemas"]["OssPresignedRequest"];
            /** Format: int64 */
            partSize?: number;
            /** Format: int32 */
            partCount?: number;
        };
        /** @description 浏览器可直接执行的 OSS 预签名请求。 */
        OssPresignedRequest: {
            /** @description HTTP 方法 */
            method?: string;
            /** @description 短时签名 URL */
            url?: string;
            /** @description 调用方必须原样发送的请求头 */
            requiredHeaders?: {
                [key: string]: string;
            };
            /**
             * Format: date-time
             * @description 绝对过期时间
             */
            expiresAt?: string;
        };
        /** @description 响应信息主体 */
        RInitResponse: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["InitResponse"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        SignPartsRequest: {
            partNumbers: number[];
        };
        /** @description 响应信息主体 */
        RSignPartsResponse: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["SignPartsResponse"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        SignPartsResponse: {
            parts?: components["schemas"]["SignedPart"][];
        };
        SignedPart: {
            /** Format: int32 */
            partNumber?: number;
            method?: string;
            url?: string;
            requiredHeaders?: {
                [key: string]: string;
            };
            /** Format: date-time */
            expiresAt?: string;
        };
        CompleteRequest: {
            parts?: components["schemas"]["CompletedPart"][];
        };
        CompletedPart: {
            /** Format: int32 */
            partNumber?: number;
            eTag: string;
        };
        CleanupRequest: {
            approved?: boolean;
        };
        MigrationRequest: {
            ossIds: number[];
            targetConfigKey: string;
        };
        /** @description 响应信息主体 */
        RLong: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /**
             * Format: int64
             * @description 响应业务数据
             */
            data?: number;
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        DryRunReport: {
            targetConfigKey?: string;
            ready?: boolean;
            items?: components["schemas"]["PreflightItem"][];
        };
        PreflightItem: {
            /** Format: int64 */
            ossId?: number;
            sourceConfigKey?: string;
            targetConfigKey?: string;
            objectKey?: string;
            ready?: boolean;
            reason?: string;
        };
        /** @description 响应信息主体 */
        RDryRunReport: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["DryRunReport"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 对象存储配置业务对象 sys_oss_config */
        SysOssConfigBo: {
            /**
             * Format: int64
             * @description 主键
             */
            ossConfigId?: number;
            /** @description 配置key */
            configKey: string;
            /** @description accessKey */
            accessKey: string;
            /** @description 秘钥 */
            secretKey: string;
            /** @description 桶名称 */
            bucketName: string;
            /** @description 前缀 */
            prefix?: string;
            /** @description 访问站点 */
            endpoint: string;
            /** @description 自定义域名 */
            domainUrl?: string;
            /** @description 是否https（Y=是,N=否） */
            isHttps?: string;
            /** @description 是否默认（Y=是,N=否） */
            status?: string;
            /** @description 域 */
            region?: string;
            /** @description 扩展字段 */
            ext1?: string;
            /** @description 备注 */
            remark?: string;
            /** @description 桶权限类型（0=PRIVATE，2=PUBLIC_READ） */
            accessPolicy: string;
        };
        /** @description CallbackRequest HTTP 接口，负责参数校验和响应包装。 */
        CallbackRequest: {
            providerRequestId: string;
            /** Format: int64 */
            timestampEpochSecond?: number;
            payload: string;
            signature: string;
        };
        /** @description 响应信息主体 */
        RPersonVerificationCallbackOutcome: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /**
             * @description 响应业务数据
             * @enum {string}
             */
            data?: "ACCEPTED" | "IDEMPOTENT" | "LATE_IGNORED";
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description PersonRebindUnbindVo 对外返回模型。 */
        PersonRebindUnbindVo: {
            status?: string;
        };
        /** @description 响应信息主体 */
        RPersonRebindUnbindVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["PersonRebindUnbindVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description PersonRebindSubmitBo 请求参数模型。 */
        PersonRebindSubmitBo: {
            /** Format: int32 */
            expectedVersion?: number;
        };
        /** @description PersonRebindSubmissionVo 对外返回模型。 */
        PersonRebindSubmissionVo: {
            status?: string;
            /** Format: int32 */
            snapshotVersion?: number;
            /** Format: int32 */
            version?: number;
        };
        /** @description 响应信息主体 */
        RPersonRebindSubmissionVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["PersonRebindSubmissionVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description PersonRebindProbeBo 请求参数模型。 */
        PersonRebindProbeBo: {
            documentTypeCode: string;
            documentNumber: string;
        };
        /** @description PersonRebindProbeVo 对外返回模型。 */
        PersonRebindProbeVo: {
            status?: string;
        };
        /** @description 响应信息主体 */
        RPersonRebindProbeVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["PersonRebindProbeVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description PersonRebindIdentityBo 请求参数模型。 */
        PersonRebindIdentityBo: {
            fullName: string;
            documentTypeCode: string;
            documentNumber: string;
            gender: string;
            /** Format: date */
            birthDate: string;
            /** Format: date */
            validFrom?: string;
            /** Format: date */
            validUntil?: string;
        };
        /** @description PersonRebindMatchBo 请求参数模型。 */
        PersonRebindMatchBo: {
            identity: components["schemas"]["PersonRebindIdentityBo"];
        };
        /** @description PersonRebindMatchVo 对外返回模型。 */
        PersonRebindMatchVo: {
            status?: string;
            maskedPhone?: string;
        };
        /** @description 响应信息主体 */
        RPersonRebindMatchVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["PersonRebindMatchVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description PersonRebindConfirmBo 请求参数模型。 */
        PersonRebindConfirmBo: {
            identity: components["schemas"]["PersonRebindIdentityBo"];
            /** Format: int32 */
            expectedVersion?: number;
        };
        /** @description PersonRebindConfirmationVo 对外返回模型。 */
        PersonRebindConfirmationVo: {
            status?: string;
            maskedPhone?: string;
            /** Format: int32 */
            version?: number;
        };
        /** @description 响应信息主体 */
        RPersonRebindConfirmationVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["PersonRebindConfirmationVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description AttachRequest HTTP 接口，负责参数校验和响应包装。 */
        AttachRequest: {
            /** Format: int64 */
            ossId?: number;
            /** Format: int64 */
            materialNodeId?: number;
        };
        MaterialOwnerKey: {
            /** @enum {string} */
            profileType?: "PERSON" | "ENTERPRISE";
            /** @enum {string} */
            ownerType?: "WORKING" | "SUBMISSION" | "SOURCE" | "VERSION";
            /** Format: int64 */
            ownerId?: number;
        };
        MaterialReferenceView: {
            /** Format: int64 */
            materialRefId?: number;
            owner?: components["schemas"]["MaterialOwnerKey"];
            /** Format: int64 */
            ossId?: number;
            /** Format: int64 */
            materialNodeId?: number;
            materialTagCode?: string;
            materialTagName?: string;
            fileName?: string;
            /** Format: int64 */
            fileSize?: number;
            fileExtension?: string;
            mimeType?: string;
            attached?: boolean;
            immutableEvidence?: boolean;
            /** Format: date-time */
            attachedTime?: string;
            /** Format: date-time */
            detachedTime?: string;
            /** Format: int32 */
            version?: number;
        };
        /** @description 响应信息主体 */
        RMaterialReferenceView: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["MaterialReferenceView"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description PersonAdminRevokeBo 请求参数模型。 */
        PersonAdminRevokeBo: {
            reason: string;
            /** Format: int32 */
            expectedVersion?: number;
        };
        /** @description PersonAdminResultVo 对外返回模型。 */
        PersonAdminResultVo: {
            status?: string;
            /** Format: int64 */
            profileId?: number;
            /** Format: int64 */
            versionId?: number;
            /** Format: int64 */
            bindingId?: number;
            /** Format: int32 */
            version?: number;
        };
        /** @description 响应信息主体 */
        RPersonAdminResultVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["PersonAdminResultVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description PersonAdminIdentityBo 请求参数模型。 */
        PersonAdminIdentityBo: {
            fullName: string;
            documentTypeCode: string;
            documentNumber: string;
            gender: string;
            /** Format: date */
            birthDate: string;
            /** Format: date */
            validFrom?: string;
            /** Format: date */
            validUntil?: string;
        };
        /** @description PersonAdminReviseBo 请求参数模型。 */
        PersonAdminReviseBo: {
            identity: components["schemas"]["PersonAdminIdentityBo"];
            reason: string;
            /** Format: int32 */
            expectedVersion?: number;
        };
        /** @description PersonAdminBindingBo 请求参数模型。 */
        PersonAdminBindingBo: {
            action: string;
            reason: string;
            /** Format: int32 */
            expectedBindingVersion?: number;
        };
        /** @description PersonAdminAssignBo 请求参数模型。 */
        PersonAdminAssignBo: {
            /** Format: int64 */
            userId?: number;
            reason: string;
        };
        /** @description PersonAdminDecisionBo 请求参数模型。 */
        PersonAdminDecisionBo: {
            decision: string;
            reason: string;
        };
        /** @description PersonAdminCreateBo 请求参数模型。 */
        PersonAdminCreateBo: {
            identity: components["schemas"]["PersonAdminIdentityBo"];
            /** Format: int64 */
            bindUserId?: number;
            reason: string;
            materials?: components["schemas"]["PersonAdminMaterialBo"][];
        };
        /** @description PersonAdminMaterialBo 请求参数模型。 */
        PersonAdminMaterialBo: {
            /** Format: int64 */
            ossId?: number;
            /** Format: int64 */
            materialNodeId?: number;
        };
        /** @description PersonApplicationSaveBo 请求参数模型。 */
        PersonApplicationSaveBo: {
            fullName?: string;
            documentTypeCode?: string;
            documentNumber?: string;
            gender?: string;
            /** Format: date */
            birthDate?: string;
            /** Format: date */
            validFrom?: string;
            /** Format: date */
            validUntil?: string;
            /** Format: int32 */
            expectedVersion?: number;
        };
        /** @description PersonApplicationVo 对外返回模型。 */
        PersonApplicationVo: {
            /** Format: int64 */
            personApplicationId?: number;
            status?: string;
            fullName?: string;
            documentTypeCode?: string;
            documentNumber?: string;
            gender?: string;
            /** Format: date */
            birthDate?: string;
            /** Format: date */
            validFrom?: string;
            /** Format: date */
            validUntil?: string;
            providerCode?: string;
            /** Format: int32 */
            snapshotVersion?: number;
            /** Format: int32 */
            version?: number;
            /** Format: date-time */
            submittedTime?: string;
            /** Format: date-time */
            finishedTime?: string;
        };
        /** @description 响应信息主体 */
        RPersonApplicationVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["PersonApplicationVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description PersonApplicationSubmitBo 请求参数模型。 */
        PersonApplicationSubmitBo: {
            /** Format: int32 */
            expectedVersion?: number;
        };
        MaterialNodeCommand: {
            /** Format: int64 */
            parentId?: number;
            /** @enum {string} */
            nodeType?: "CATEGORY" | "TAG";
            /** @enum {string} */
            scope?: "PERSON" | "ENTERPRISE" | "COMMON";
            materialTagCode?: string;
            nodeName?: string;
            systemRequired?: boolean;
            /** Format: int32 */
            orderNum?: number;
            /** Format: int32 */
            expectedVersion?: number;
        };
        MaterialNodeView: {
            /** Format: int64 */
            materialNodeId?: number;
            /** Format: int64 */
            parentId?: number;
            /** @enum {string} */
            nodeType?: "CATEGORY" | "TAG";
            /** Format: int32 */
            nodeDepth?: number;
            /** @enum {string} */
            scope?: "PERSON" | "ENTERPRISE" | "COMMON";
            materialTagCode?: string;
            nodeName?: string;
            systemRequired?: boolean;
            enabled?: boolean;
            /** Format: int32 */
            orderNum?: number;
            /** Format: int32 */
            version?: number;
            children?: components["schemas"]["MaterialNodeView"][];
        };
        /** @description 响应信息主体 */
        RMaterialNodeView: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["MaterialNodeView"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 材料节点状态变更请求。 */
        StatusCommand: {
            enabled?: boolean;
            /** Format: int32 */
            expectedVersion?: number;
        };
        /** @description 材料节点版本变更请求。 */
        VersionCommand: {
            /** Format: int32 */
            expectedVersion?: number;
        };
        /** @description 响应信息主体 */
        REnterpriseVerificationCallbackOutcome: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /**
             * @description 响应业务数据
             * @enum {string}
             */
            data?: "ACCEPTED" | "IDEMPOTENT" | "LATE_IGNORED";
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description EnterpriseTransferVo 对外返回模型。 */
        EnterpriseTransferVo: {
            status?: string;
            challengeId?: string;
            /** Format: int64 */
            expiresInSeconds?: number;
        };
        /** @description 响应信息主体 */
        REnterpriseTransferVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["EnterpriseTransferVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description EnterpriseTransferSendBo 请求参数模型。 */
        EnterpriseTransferSendBo: {
            fullName: string;
            documentLastFour: string;
            phone: string;
        };
        /** @description EnterpriseTransferConfirmBo 请求参数模型。 */
        EnterpriseTransferConfirmBo: {
            challengeId: string;
            code: string;
        };
        /** @description EnterpriseAdminRevokeBo 请求参数模型。 */
        EnterpriseAdminRevokeBo: {
            reason: string;
            /** Format: int32 */
            expectedVersion?: number;
        };
        /** @description EnterpriseAdminResultVo 对外返回模型。 */
        EnterpriseAdminResultVo: {
            status?: string;
            /** Format: int64 */
            profileId?: number;
            /** Format: int64 */
            versionId?: number;
            /** Format: int64 */
            bindingId?: number;
            /** Format: int32 */
            version?: number;
        };
        /** @description 响应信息主体 */
        REnterpriseAdminResultVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["EnterpriseAdminResultVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description EnterpriseAdminIdentityBo 请求参数模型。 */
        EnterpriseAdminIdentityBo: {
            enterpriseName: string;
            unifiedCreditCode: string;
            enterpriseType: string;
            legalRepresentativeName: string;
            legalDocumentTypeCode: string;
            legalDocumentNumber: string;
            /** Format: date */
            establishedDate: string;
            /** Format: date */
            businessTermFrom?: string;
            /** Format: date */
            businessTermUntil?: string;
            registeredAddress: string;
            businessScope: string;
            contactName?: string;
            contactPhone?: string;
            email?: string;
            registeredCapital?: number;
            industryCode?: string;
            website?: string;
        };
        /** @description EnterpriseAdminReviseBo 请求参数模型。 */
        EnterpriseAdminReviseBo: {
            identity: components["schemas"]["EnterpriseAdminIdentityBo"];
            reason: string;
            /** Format: int32 */
            expectedVersion?: number;
        };
        /** @description EnterpriseAdminBindingBo 请求参数模型。 */
        EnterpriseAdminBindingBo: {
            action: string;
            reason: string;
            /** Format: int32 */
            expectedBindingVersion?: number;
        };
        /** @description EnterpriseAdminAssignBo 请求参数模型。 */
        EnterpriseAdminAssignBo: {
            /** Format: int64 */
            userId?: number;
            reason: string;
        };
        /** @description EnterpriseAdminDecisionBo 请求参数模型。 */
        EnterpriseAdminDecisionBo: {
            decision: string;
            reason: string;
        };
        /** @description EnterpriseAdminCreateBo 请求参数模型。 */
        EnterpriseAdminCreateBo: {
            identity: components["schemas"]["EnterpriseAdminIdentityBo"];
            /** Format: int64 */
            bindUserId?: number;
            reason: string;
            materials?: components["schemas"]["EnterpriseAdminMaterialBo"][];
        };
        /** @description EnterpriseAdminMaterialBo 请求参数模型。 */
        EnterpriseAdminMaterialBo: {
            /** Format: int64 */
            ossId?: number;
            /** Format: int64 */
            materialNodeId?: number;
        };
        /** @description EnterpriseApplicationSaveBo 请求参数模型。 */
        EnterpriseApplicationSaveBo: {
            enterpriseName?: string;
            unifiedCreditCode?: string;
            enterpriseType?: string;
            legalRepresentativeName?: string;
            legalDocumentTypeCode?: string;
            legalDocumentNumber?: string;
            handlerIsLegalRepresentative?: boolean;
            /** Format: date */
            establishedDate?: string;
            /** Format: date */
            businessTermFrom?: string;
            /** Format: date */
            businessTermUntil?: string;
            registeredAddress?: string;
            businessScope?: string;
            contactName?: string;
            contactPhone?: string;
            email?: string;
            registeredCapital?: number;
            industryCode?: string;
            website?: string;
            /** Format: int32 */
            expectedVersion?: number;
        };
        /** @description EnterpriseApplicationVo 对外返回模型。 */
        EnterpriseApplicationVo: {
            /** Format: int64 */
            enterpriseApplicationId?: number;
            status?: string;
            enterpriseName?: string;
            unifiedCreditCode?: string;
            enterpriseType?: string;
            legalRepresentativeName?: string;
            legalDocumentTypeCode?: string;
            legalDocumentNumber?: string;
            handlerIsLegalRepresentative?: boolean;
            /** Format: date */
            establishedDate?: string;
            /** Format: date */
            businessTermFrom?: string;
            /** Format: date */
            businessTermUntil?: string;
            registeredAddress?: string;
            businessScope?: string;
            contactName?: string;
            contactPhone?: string;
            email?: string;
            registeredCapital?: number;
            industryCode?: string;
            website?: string;
            providerCode?: string;
            /** Format: int32 */
            snapshotVersion?: number;
            /** Format: int32 */
            version?: number;
            /** Format: date-time */
            submittedTime?: string;
            /** Format: date-time */
            finishedTime?: string;
        };
        /** @description 响应信息主体 */
        REnterpriseApplicationVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["EnterpriseApplicationVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description EnterpriseApplicationSubmitBo 请求参数模型。 */
        EnterpriseApplicationSubmitBo: {
            /** Format: int32 */
            expectedVersion?: number;
        };
        /** @description EnterpriseApplicationProbeBo 请求参数模型。 */
        EnterpriseApplicationProbeBo: {
            unifiedCreditCode: string;
        };
        /** @description EnterpriseApplicationProbeVo 对外返回模型。 */
        EnterpriseApplicationProbeVo: {
            status?: string;
        };
        /** @description 响应信息主体 */
        REnterpriseApplicationProbeVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["EnterpriseApplicationProbeVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 统一通知提交命令。 */
        NotificationCommand: {
            /** @description 通知应用标识 */
            appId?: string;
            /** @description 业务场景编码 */
            sceneCode?: string;
            /** @description 业务类型 */
            bizType?: string;
            /** @description 业务主键 */
            bizId?: string;
            /** @description 接收者类型 */
            recipientType?: string;
            /** @description 接收者标识 */
            recipientIds?: string[];
            /** @description 逻辑模板编码 */
            templateCode?: string;
            /** @description 模板参数 */
            templateParams?: {
                [key: string]: unknown;
            };
            /** @description 请求渠道 */
            channels?: ("IN_APP" | "SMS" | "MAIL")[];
            /**
             * @description 编排策略
             * @enum {string}
             */
            strategy?: "ALL" | "ORDERED_FALLBACK" | "ESCALATION";
            /**
             * @description 执行模式
             * @enum {string}
             */
            mode?: "SYNC" | "ASYNC";
            /**
             * Format: int32
             * @description 优先级
             */
            priority?: number;
            /**
             * Format: date-time
             * @description 计划时间
             */
            scheduledAt?: string;
            /**
             * Format: date-time
             * @description 截止时间
             */
            expiresAt?: string;
            /** @description 幂等键 */
            idempotencyKey?: string;
            /** @description 脱敏元数据 */
            metadata?: {
                [key: string]: string;
            };
        };
        /** @description 单渠道投递结果。 */
        DeliveryReceipt: {
            /** @description 接收者标识 */
            recipientId?: string;
            /**
             * @description 渠道
             * @enum {string}
             */
            channel?: "IN_APP" | "SMS" | "MAIL";
            /**
             * @description 投递状态
             * @enum {string}
             */
            status?: "QUEUED" | "PROCESSING" | "ACCEPTED" | "PARTIAL_FAILURE" | "DELIVERED" | "UNDELIVERABLE" | "UNKNOWN" | "FAILED" | "CANCELLED" | "EXPIRED";
            /** @description 供应商消息标识 */
            providerMessageId?: string;
        };
        /** @description 通知提交结果。 */
        NotificationReceipt: {
            /** @description 通知主键 */
            notificationId?: string;
            /**
             * @description 聚合状态
             * @enum {string}
             */
            status?: "QUEUED" | "PROCESSING" | "ACCEPTED" | "PARTIAL_FAILURE" | "DELIVERED" | "UNDELIVERABLE" | "UNKNOWN" | "FAILED" | "CANCELLED" | "EXPIRED";
            /** @description 是否进入 Outbox */
            outboxQueued?: boolean;
            /** @description 是否需要后续回执 */
            followUpRequired?: boolean;
            /** @description 渠道投递结果 */
            deliveries?: components["schemas"]["DeliveryReceipt"][];
        };
        /** @description 响应信息主体 */
        RNotificationReceipt: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["NotificationReceipt"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 通知重试命令。 */
        NotificationRetryCommand: {
            /** @description 通知主键 */
            notificationId?: string;
            /** @description 可选投递主键 */
            deliveryId?: string;
            /** @description 重试原因 */
            reason?: string;
            /** @description 重试幂等键 */
            idempotencyKey?: string;
        };
        /** @description 响应信息主体 */
        RRetryReceipt: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["RetryReceipt"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 通知重试结果。 */
        RetryReceipt: {
            /** @description 通知主键 */
            notificationId?: string;
            /**
             * @description 状态
             * @enum {string}
             */
            status?: "QUEUED" | "PROCESSING" | "ACCEPTED" | "PARTIAL_FAILURE" | "DELIVERED" | "UNDELIVERABLE" | "UNKNOWN" | "FAILED" | "CANCELLED" | "EXPIRED";
        };
        /** @description 取消尚未完成通知命令。 */
        NotificationCancelCommand: {
            /** @description 通知主键 */
            notificationId?: string;
            /** @description 取消原因 */
            reason?: string;
        };
        /** @description 通知取消结果。 */
        CancelReceipt: {
            /** @description 通知主键 */
            notificationId?: string;
            /**
             * @description 状态
             * @enum {string}
             */
            status?: "QUEUED" | "PROCESSING" | "ACCEPTED" | "PARTIAL_FAILURE" | "DELIVERED" | "UNDELIVERABLE" | "UNKNOWN" | "FAILED" | "CANCELLED" | "EXPIRED";
        };
        /** @description 响应信息主体 */
        RCancelReceipt: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["CancelReceipt"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 通知中心公告写入参数。 */
        NotifyNoticeBo: {
            /** Format: int64 */
            noticeId?: number;
            noticeTitle: string;
            noticeType: string;
            noticeContent: string;
            /** @description ALL 全部正常用户；USER 指定用户；USER_TYPE 匹配任一启用类型。 */
            recipientType?: string;
            /** @description 指定用户 ID 快照，只在 USER 模式非空。 */
            recipientIds?: number[];
            /** @description 用户类型 ID 快照，只在 USER_TYPE 模式非空。 */
            userTypeIds?: number[];
            /** @description 本次发布选择的渠道，缺省为站内信。 */
            channels?: string[];
            status?: string;
            remark?: string;
        };
        /** @description 操作日志记录业务对象 sys_oper_log */
        SysOperLogBo: {
            /**
             * Format: int64
             * @description 日志主键
             */
            operId?: number;
            /** @description 模块标题 */
            title?: string;
            /**
             * Format: int32
             * @description 业务类型（0其它 1新增 2修改 3删除）
             */
            businessType?: number;
            /** @description 业务类型数组 */
            businessTypes?: number[];
            /** @description 方法名称 */
            method?: string;
            /** @description 请求方式 */
            requestMethod?: string;
            /**
             * Format: int32
             * @description 操作类别（0其它 1后台用户 2手机端用户）
             */
            operatorType?: number;
            /** @description 操作人员 */
            operName?: string;
            /**
             * Format: int64
             * @description 操作用户ID
             */
            userId?: number;
            /**
             * Format: int64
             * @description 操作部门ID
             */
            deptId?: number;
            /** @description 部门名称 */
            deptName?: string;
            /** @description 客户端 */
            clientKey?: string;
            /** @description 设备类型 */
            deviceType?: string;
            /** @description 浏览器类型 */
            browser?: string;
            /** @description 操作系统 */
            os?: string;
            /** @description 请求URL */
            operUrl?: string;
            /** @description 主机地址 */
            operIp?: string;
            /** @description 操作地点 */
            operLocation?: string;
            /** @description 请求参数 */
            operParam?: string;
            /** @description 返回参数 */
            jsonResult?: string;
            /**
             * Format: int32
             * @description 操作状态（0正常 1异常）
             */
            status?: number;
            /** @description 错误消息 */
            errorMsg?: string;
            /**
             * Format: date-time
             * @description 操作时间
             */
            operTime?: string;
            /**
             * Format: int64
             * @description 消耗时间
             */
            costTime?: number;
            /** @description 请求参数 */
            params?: {
                [key: string]: unknown;
            };
        };
        /** @description 系统访问记录业务对象 sys_login_info */
        SysLoginInfoBo: {
            /**
             * Format: int64
             * @description 访问ID
             */
            infoId?: number;
            /** @description 用户账号 */
            userName?: string;
            /** @description 客户端 */
            clientKey?: string;
            /** @description 设备类型 */
            deviceType?: string;
            /** @description 登录IP地址 */
            ipaddr?: string;
            /** @description 登录地点 */
            loginLocation?: string;
            /** @description 浏览器类型 */
            browser?: string;
            /** @description 操作系统 */
            os?: string;
            /** @description 登录状态（0成功 1失败） */
            status?: string;
            /** @description 提示消息 */
            msg?: string;
            /**
             * Format: date-time
             * @description 访问时间
             */
            loginTime?: string;
            /** @description 请求参数 */
            params?: {
                [key: string]: unknown;
            };
        };
        /** @description 带有下拉选的Excel导出 */
        ExportDemoVo: {
            /** @description 用户昵称 */
            nickName?: string;
            /**
             * @description 用户类型
             *      </p>
             *      使用ExcelEnumFormat注解需要进行下拉选的部分
             */
            userStatus?: string;
            /**
             * @description 性别
             *      <p>
             *      使用ExcelDictFormat注解需要进行下拉选的部分
             */
            gender?: string;
            /** @description 手机号 */
            phoneNumber?: string;
            /** @description Email */
            email?: string;
            /**
             * @description 省
             *      <p>
             *      级联下拉，仅判断是否选了
             */
            province?: string;
            /**
             * Format: int32
             * @description 数据库中的省ID
             *      </p>
             *      处理完毕后再判断是否市正确的值
             */
            provinceId?: number;
            /**
             * @description 市
             *      <p>
             *      级联下拉
             */
            city?: string;
            /**
             * Format: int32
             * @description 数据库中的市ID
             */
            cityId?: number;
            /**
             * @description 县
             *      <p>
             *      级联下拉
             */
            area?: string;
            /**
             * Format: int32
             * @description 数据库中的县ID
             */
            areaId?: number;
        };
        /** @description 第三方平台登录绑定请求对象。 */
        SocialLoginBody: {
            /** @description 客户端id */
            clientId: string;
            /** @description 授权类型 */
            grantType: string;
            /** @description 验证码 */
            code?: string;
            /** @description 验证码唯一标识。 */
            uuid?: string;
            /** @description 第三方登录平台 */
            source: string;
            /** @description 第三方登录code */
            socialCode: string;
            /** @description 第三方登录socialState */
            socialState: string;
        };
        /** @description 用户注册对象 */
        RegisterBody: {
            /** @description 客户端id */
            clientId: string;
            /** @description 授权类型 */
            grantType: string;
            /** @description 验证码 */
            code?: string;
            /** @description 验证码唯一标识。 */
            uuid?: string;
            /** @description 用户名 */
            username: string;
            /** @description 用户密码 */
            password?: string;
            /** @description 可选邮箱。 */
            email?: string;
            /** @description 可选手机号码。 */
            phoneNumber?: string;
        };
        /** @description 登录成功后的令牌信息返回对象。 */
        LoginVo: {
            /** @description 令牌权限 */
            scope?: string;
            /** @description 用户 openid */
            openid?: string;
            /** @description 授权令牌 */
            access_token?: string;
            /** @description 刷新令牌 */
            refresh_token?: string;
            /**
             * Format: int64
             * @description 授权令牌 access_token 的有效期
             */
            expire_in?: number;
            /**
             * Format: int64
             * @description 刷新令牌 refresh_token 的有效期
             */
            refresh_expire_in?: number;
            /** @description 应用id */
            client_id?: string;
        };
        /** @description 响应信息主体 */
        RLoginVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["LoginVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        OpenApiEmbedTokenRequest: {
            openId: string;
            trustedCredential?: string;
            /** Format: int32 */
            ttlSeconds?: number;
        };
        OpenApiEmbedTokenResponse: {
            token?: string;
            tokenType?: string;
            authHeader?: string;
            /** Format: date-time */
            expiresAt?: string;
            /** Format: int32 */
            ttlSeconds?: number;
            openId?: string;
            nickname?: string;
            avatarUrl?: string;
        };
        ResultOpenApiEmbedTokenResponse: {
            /** Format: int32 */
            status?: number;
            message?: string;
            data?: components["schemas"]["OpenApiEmbedTokenResponse"];
        };
        OpenApiResourceResponse: {
            /** Format: int64 */
            id?: number;
            storageKey?: string;
            originalName?: string;
            /** Format: int64 */
            fileSize?: number;
            mimeType?: string;
            storageType?: string;
            accessUrl?: string;
            bizType?: string;
            /** Format: int64 */
            bizId?: number;
            /** Format: int64 */
            creatorId?: number;
            /** Format: date-time */
            createDt?: string;
        };
        ResultOpenApiResourceResponse: {
            /** Format: int32 */
            status?: number;
            message?: string;
            data?: components["schemas"]["OpenApiResourceResponse"];
        };
        OpenApiCreateConversationRequest: {
            /** Format: int64 */
            agentId: number;
            openId: string;
            title: string;
        };
        OpenApiConversationVO: {
            conversationId?: string;
            /** Format: int64 */
            agentId?: number;
            title?: string;
            createDt?: string;
            updateDt?: string;
        };
        ResultOpenApiConversationVO: {
            /** Format: int32 */
            status?: number;
            message?: string;
            data?: components["schemas"]["OpenApiConversationVO"];
        };
        OpenApiChatAttachmentRequest: {
            /** Format: int64 */
            resourceId: number;
            type: string;
        };
        OpenApiChatRequest: {
            /** Format: int64 */
            agentId: number;
            openId: string;
            conversationId: string;
            content: string;
            attachments?: components["schemas"]["OpenApiChatAttachmentRequest"][];
            disabledMcpServerIds?: number[];
            disabledSkillIds?: number[];
            deepPlanEnabled?: boolean;
            webSearchEnabled?: boolean;
            sid?: string;
            /** Format: int64 */
            timeout?: number;
        };
        ServerSentEventString: unknown;
        /** @description 任务请求对象 */
        FlowTaskBo: {
            /** @description 任务名称 */
            nodeName?: string;
            /** @description 流程定义名称 */
            flowName?: string;
            /** @description 流程定义编码 */
            flowCode?: string;
            /** @description 流程分类id */
            category?: string;
            /**
             * Format: int64
             * @description 流程实例id
             */
            instanceId?: number;
            /** @description 流程状态 */
            flowStatus?: string;
            /** @description 权限列表 */
            permissionList?: string[];
            /** @description 申请人Ids */
            createByIds?: number[];
            /** @description 请求参数 */
            params?: {
                [key: string]: unknown;
            };
        };
        /** @description 分页查询实体类 */
        PageQuery: {
            /**
             * Format: int32
             * @description 分页大小
             */
            pageSize?: number;
            /**
             * Format: int32
             * @description 当前页数
             */
            pageNum?: number;
            /** @description 排序列 */
            orderByColumn?: string;
            /** @description 排序的方向desc或者asc */
            isAsc?: string;
        };
        /** @description 按钮权限视图对象。 */
        ButtonPermissionVo: {
            /** @description 唯一编码 */
            code?: string;
            /** @description 选项值 */
            value?: string;
            /** @description 是否显示 */
            show?: boolean;
        };
        /** @description 流程抄送视图对象。 */
        FlowCopyVo: {
            /**
             * Format: int64
             * @description 用户id
             */
            userId?: number;
            /** @description 用户昵称 */
            nickName?: string;
        };
        /** @description 流程任务视图对象。 */
        FlowTaskVo: {
            /**
             * Format: int64
             * @description 任务ID。
             */
            id?: number;
            /**
             * Format: date-time
             * @description 创建时间
             */
            createTime?: string;
            /**
             * Format: date-time
             * @description 更新时间
             */
            updateTime?: string;
            /** @description 删除标记 */
            delFlag?: string;
            /**
             * Format: int64
             * @description 对应flow_definition表的id
             */
            definitionId?: number;
            /**
             * Format: int64
             * @description 流程实例表id
             */
            instanceId?: number;
            /** @description 流程定义名称 */
            flowName?: string;
            /** @description 业务id */
            businessId?: string;
            /** @description 节点编码 */
            nodeCode?: string;
            /** @description 节点名称 */
            nodeName?: string;
            /**
             * Format: int32
             * @description 节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）
             */
            nodeType?: number;
            /** @description 权限标识 permissionFlag的list形式 */
            permissionList?: string[];
            /** @description 流程用户列表 */
            userList?: components["schemas"]["User"][];
            /** @description 审批表单是否自定义（Y是 N否） */
            formCustom?: string;
            /** @description 审批表单 */
            formPath?: string;
            /** @description 流程定义编码 */
            flowCode?: string;
            /** @description 流程版本号 */
            version?: string;
            /** @description 流程状态 */
            flowStatus?: string;
            /** @description 流程分类id */
            category?: string;
            /** @description 流程分类名称 */
            categoryName?: string;
            /** @description 流程状态 */
            flowStatusName?: string;
            /** @description 办理人类型 */
            type?: string;
            /** @description 办理人ids */
            assigneeIds?: string;
            /** @description 办理人名称 */
            assigneeNames?: string;
            /** @description 抄送人id */
            processedBy?: string;
            /** @description 抄送人名称 */
            processedByName?: string;
            /** @description 流程签署比例值 大于0为票签，会签 */
            nodeRatio?: string;
            /** @description 申请人id */
            createBy?: string;
            /** @description 申请人名称 */
            createByName?: string;
            /** @description 是否为申请人节点 */
            applyNode?: boolean;
            /** @description 按钮权限 */
            buttonList?: components["schemas"]["ButtonPermissionVo"][];
            /**
             * @description 抄送对象 ID 集合
             *      <p>
             *      根据扩展属性中 CopySettingEnum 类型的数据生成，存储需要抄送的对象 ID
             */
            copyList?: components["schemas"]["FlowCopyVo"][];
            /**
             * @description 自定义参数 Map
             *      <p>
             *      根据扩展属性中 VariablesEnum 类型的数据生成，存储 key=value 格式的自定义参数
             */
            varList?: {
                [key: string]: string;
            };
            /** @description 业务编码 */
            businessCode?: string;
            /** @description 业务标题 */
            businessTitle?: string;
        };
        /** @description 表格分页数据对象 */
        PageResultFlowTaskVo: {
            /**
             * Format: int64
             * @description 总记录数
             */
            total?: number;
            /** @description 列表数据 */
            rows?: components["schemas"]["FlowTaskVo"][];
        };
        /** @description 响应信息主体 */
        RPageResultFlowTaskVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["PageResultFlowTaskVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 历史任务视图 */
        FlowHisTaskVo: {
            /**
             * Format: int64
             * @description 历史任务主键
             */
            id?: number;
            /**
             * Format: date-time
             * @description 创建时间
             */
            createTime?: string;
            /**
             * Format: date-time
             * @description 更新时间
             */
            updateTime?: string;
            /** @description 删除标记 */
            delFlag?: string;
            /**
             * Format: int64
             * @description 对应flow_definition表的id
             */
            definitionId?: number;
            /** @description 流程定义名称 */
            flowName?: string;
            /**
             * Format: int64
             * @description 流程实例表id
             */
            instanceId?: number;
            /**
             * Format: int64
             * @description 任务表id
             */
            taskId?: number;
            /**
             * Format: int32
             * @description 协作方式(1审批 2转办 3委派 4会签 5票签 6加签 7减签)
             */
            cooperateType?: number;
            /** @description 协作方式(1审批 2转办 3委派 4会签 5票签 6加签 7减签) */
            cooperateTypeName?: string;
            /** @description 业务id */
            businessId?: string;
            /** @description 开始节点编码 */
            nodeCode?: string;
            /** @description 开始节点名称 */
            nodeName?: string;
            /**
             * Format: int32
             * @description 开始节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）
             */
            nodeType?: number;
            /** @description 目标节点编码 */
            targetNodeCode?: string;
            /** @description 结束节点名称 */
            targetNodeName?: string;
            /** @description 审批者 */
            approver?: string;
            /** @description 审批者 */
            approverName?: string;
            /** @description 协作人(只有转办、会签、票签、委派) */
            collaborator?: string;
            /** @description 权限标识 permissionFlag的list形式 */
            permissionList?: string[];
            /** @description 跳转类型（PASS通过 REJECT退回 NONE无动作） */
            skipType?: string;
            /** @description 流程状态 */
            flowStatus?: string;
            /** @description 任务状态 */
            flowTaskStatus?: string;
            /** @description 流程状态 */
            flowStatusName?: string;
            /** @description 审批意见 */
            message?: string;
            /** @description 业务详情 存业务类的json */
            ext?: string;
            /** @description 创建者 */
            createBy?: string;
            /** @description 申请人 */
            createByName?: string;
            /** @description 流程分类id */
            category?: string;
            /** @description 流程分类名称 */
            categoryName?: string;
            /** @description 审批表单是否自定义（Y是 N否） */
            formCustom?: string;
            /** @description 审批表单路径 */
            formPath?: string;
            /** @description 流程定义编码 */
            flowCode?: string;
            /** @description 流程版本号 */
            version?: string;
            /** @description 运行时长 */
            runDuration?: string;
            /** @description 业务编码 */
            businessCode?: string;
            /** @description 业务标题 */
            businessTitle?: string;
        };
        /** @description 表格分页数据对象 */
        PageResultFlowHisTaskVo: {
            /**
             * Format: int64
             * @description 总记录数
             */
            total?: number;
            /** @description 列表数据 */
            rows?: components["schemas"]["FlowHisTaskVo"][];
        };
        /** @description 响应信息主体 */
        RPageResultFlowHisTaskVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["PageResultFlowHisTaskVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 响应信息主体 */
        RFlowTaskVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["FlowTaskVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 响应信息主体 */
        RListNode: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["Node"][];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 响应信息主体 */
        RListUserDTO: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["UserDTO"][];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 用户 */
        UserDTO: {
            /**
             * Format: int64
             * @description 用户ID
             */
            userId?: number;
            /**
             * Format: int64
             * @description 部门ID
             */
            deptId?: number;
            /** @description 用户账号 */
            userName?: string;
            /** @description 用户昵称 */
            nickName?: string;
            /** @description 用户邮箱 */
            email?: string;
            /** @description 手机号码 */
            phoneNumber?: string;
            /** @description 用户性别（0男 1女 2未知） */
            gender?: string;
            /** @description 账号状态（0正常 1停用） */
            status?: string;
            /**
             * Format: date-time
             * @description 创建时间
             */
            createTime?: string;
        };
        /** @description 流程spel表达式定义视图对象 flow_spel */
        FlowSpelVo: {
            /**
             * Format: int64
             * @description 主键id
             */
            id?: number;
            /** @description 组件名称 */
            componentName?: string;
            /** @description 方法名 */
            methodName?: string;
            /** @description 参数 */
            methodParams?: string;
            /** @description 预览spel值 */
            viewSpel?: string;
            /** @description 状态（0正常 1停用） */
            status?: string;
            /** @description 备注 */
            remark?: string;
            /**
             * Format: date-time
             * @description 创建时间
             */
            createTime?: string;
        };
        /** @description 响应信息主体 */
        RFlowSpelVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["FlowSpelVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 表格分页数据对象 */
        PageResultFlowSpelVo: {
            /**
             * Format: int64
             * @description 总记录数
             */
            total?: number;
            /** @description 列表数据 */
            rows?: components["schemas"]["FlowSpelVo"][];
        };
        /** @description 响应信息主体 */
        RPageResultFlowSpelVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["PageResultFlowSpelVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 表格分页数据对象 */
        PageResultTestLeaveVo: {
            /**
             * Format: int64
             * @description 总记录数
             */
            total?: number;
            /** @description 列表数据 */
            rows?: components["schemas"]["TestLeaveVo"][];
        };
        /** @description 响应信息主体 */
        RPageResultTestLeaveVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["PageResultTestLeaveVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 流程实例查询条件对象。 */
        FlowInstanceBo: {
            /** @description 流程定义名称 */
            flowName?: string;
            /** @description 流程定义编码 */
            flowCode?: string;
            /** @description 任务发起人 */
            startUserId?: string;
            /** @description 业务id */
            businessId?: string;
            /** @description 流程分类id */
            category?: string;
            /** @description 任务名称 */
            nodeName?: string;
            /** @description 申请人Ids */
            createByIds?: string[];
        };
        /** @description 流程实例视图对象。 */
        FlowInstanceVo: {
            /**
             * Format: int64
             * @description 流程实例ID。
             */
            id?: number;
            /**
             * Format: date-time
             * @description 创建时间
             */
            createTime?: string;
            /**
             * Format: date-time
             * @description 更新时间
             */
            updateTime?: string;
            /** @description 删除标记 */
            delFlag?: string;
            /**
             * Format: int64
             * @description 对应flow_definition表的id
             */
            definitionId?: number;
            /** @description 流程定义名称 */
            flowName?: string;
            /** @description 流程定义编码 */
            flowCode?: string;
            /** @description 业务id */
            businessId?: string;
            /**
             * Format: int32
             * @description 节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）
             */
            nodeType?: number;
            /** @description 流程节点编码   每个流程的nodeCode是唯一的,即definitionId+nodeCode唯一,在数据库层面做了控制 */
            nodeCode?: string;
            /** @description 流程节点名称 */
            nodeName?: string;
            /** @description 流程变量 */
            variable?: string;
            /** @description 流程状态 */
            flowStatus?: string;
            /** @description 流程状态 */
            flowStatusName?: string;
            /**
             * Format: int32
             * @description 流程激活状态（0挂起 1激活）
             */
            activityStatus?: number;
            /** @description 审批表单是否自定义（Y是 N否） */
            formCustom?: string;
            /** @description 审批表单路径 */
            formPath?: string;
            /** @description 扩展字段，预留给业务系统使用 */
            ext?: string;
            /** @description 流程定义版本 */
            version?: string;
            /** @description 创建者 */
            createBy?: string;
            /** @description 申请人 */
            createByName?: string;
            /** @description 流程分类id */
            category?: string;
            /** @description 流程分类名称 */
            categoryName?: string;
            /** @description 业务编码 */
            businessCode?: string;
            /** @description 业务标题 */
            businessTitle?: string;
        };
        /** @description 表格分页数据对象 */
        PageResultFlowInstanceVo: {
            /**
             * Format: int64
             * @description 总记录数
             */
            total?: number;
            /** @description 列表数据 */
            rows?: components["schemas"]["FlowInstanceVo"][];
        };
        /** @description 响应信息主体 */
        RPageResultFlowInstanceVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["PageResultFlowInstanceVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 响应信息主体 */
        RMapStringObject: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: {
                [key: string]: unknown;
            };
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 响应信息主体 */
        RFlowInstanceVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["FlowInstanceVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        Definition: {
            /** Format: int64 */
            id?: number;
            version?: string;
            ext?: string;
            delFlag?: string;
            updateBy?: string;
            /** Format: date-time */
            createTime?: string;
            createBy?: string;
            /** Format: date-time */
            updateTime?: string;
            category?: string;
            flowCode?: string;
            flowName?: string;
            nodeList?: components["schemas"]["Node"][];
            formCustom?: string;
            /** Format: int32 */
            activityStatus?: number;
            /** Format: int32 */
            isPublish?: number;
            formPath?: string;
            tenantId?: string;
            modelValue?: string;
            userList?: components["schemas"]["User"][];
            listenerPath?: string;
            listenerType?: string;
        };
        /** @description 响应信息主体 */
        RDefinition: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["Definition"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 流程定义视图 */
        FlowDefinitionVo: {
            /**
             * Format: int64
             * @description 流程定义主键
             */
            id?: number;
            /**
             * Format: date-time
             * @description 创建时间
             */
            createTime?: string;
            /**
             * Format: date-time
             * @description 更新时间
             */
            updateTime?: string;
            /** @description 删除标记 */
            delFlag?: string;
            /** @description 流程定义编码 */
            flowCode?: string;
            /** @description 流程定义名称 */
            flowName?: string;
            /** @description 流程分类id */
            category?: string;
            /** @description 流程分类名称 */
            categoryName?: string;
            /** @description 流程版本 */
            version?: string;
            /**
             * Format: int32
             * @description 是否发布（0未发布 1已发布 9失效）
             */
            isPublish?: number;
            /** @description 审批表单是否自定义（Y是 N否） */
            formCustom?: string;
            /** @description 审批表单路径 */
            formPath?: string;
            /**
             * Format: int32
             * @description 流程激活状态（0挂起 1激活）
             */
            activityStatus?: number;
            /** @description 监听器类型 */
            listenerType?: string;
            /** @description 监听器路径 */
            listenerPath?: string;
            /** @description 扩展字段，预留给业务系统使用 */
            ext?: string;
        };
        /** @description 表格分页数据对象 */
        PageResultFlowDefinitionVo: {
            /**
             * Format: int64
             * @description 总记录数
             */
            total?: number;
            /** @description 列表数据 */
            rows?: components["schemas"]["FlowDefinitionVo"][];
        };
        /** @description 响应信息主体 */
        RPageResultFlowDefinitionVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["PageResultFlowDefinitionVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 流程分类视图对象 wf_category */
        FlowCategoryVo: {
            /**
             * Format: int64
             * @description 流程分类ID
             */
            categoryId?: number;
            /**
             * Format: int64
             * @description 父级分类id
             */
            parentId?: number;
            /** @description 父级分类名称 */
            parentName?: string;
            /** @description 祖级列表 */
            ancestors?: string;
            /** @description 流程分类名称 */
            categoryName?: string;
            /**
             * Format: int64
             * @description 显示顺序
             */
            orderNum?: number;
            /**
             * Format: date-time
             * @description 创建时间
             */
            createTime?: string;
        };
        /** @description 响应信息主体 */
        RFlowCategoryVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["FlowCategoryVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 响应信息主体 */
        RListFlowCategoryVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["FlowCategoryVo"][];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 响应信息主体 */
        RListTreeString: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["TreeString"][];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        TreeNodeConfig: {
            idKey?: string;
            parentIdKey?: string;
            weightKey?: string;
            nameKey?: string;
            childrenKey?: string;
            /** Format: int32 */
            deep?: number;
        };
        TreeString: {
            id?: string;
            parentId?: string;
            config?: components["schemas"]["TreeNodeConfig"];
            weight?: unknown;
            name?: {
                empty?: boolean;
            };
            empty?: boolean;
        } & {
            [key: string]: unknown;
        };
        ApiResultDefJson: {
            /** Format: int32 */
            code?: number;
            msg?: string;
            data?: components["schemas"]["DefJson"];
        };
        ApiResultListForm: {
            /** Format: int32 */
            code?: number;
            msg?: string;
            data?: components["schemas"]["Form"][];
        };
        ApiResultListNodeExt: {
            /** Format: int32 */
            code?: number;
            msg?: string;
            data?: components["schemas"]["NodeExt"][];
        };
        ChildNode: {
            code?: string;
            desc?: string;
            label?: string;
            /** Format: int32 */
            type?: number;
            must?: boolean;
            multiple?: boolean;
            /** Format: int32 */
            precision?: number;
            step?: string;
            min?: string;
            dateType?: string;
            dateFormat?: string;
            dict?: components["schemas"]["DictItem"][];
        };
        DictItem: {
            label?: string;
            value?: string;
            selected?: boolean;
        };
        NodeExt: {
            code?: string;
            name?: string;
            desc?: string;
            /** Format: int32 */
            type?: number;
            childs?: components["schemas"]["ChildNode"][];
        };
        ApiResultListListenerVo: {
            /** Format: int32 */
            code?: number;
            msg?: string;
            data?: components["schemas"]["ListenerVo"][];
        };
        ListenerVo: {
            type?: string;
            path?: string;
            description?: string;
        };
        ApiResultListString: {
            /** Format: int32 */
            code?: number;
            msg?: string;
            data?: string[];
        };
        HandlerQuery: {
            handlerCode?: string;
            handlerName?: string;
            handlerType?: string;
            groupId?: string;
            /** Format: int32 */
            pageNum?: number;
            /** Format: int32 */
            pageSize?: number;
            beginTime?: string;
            endTime?: string;
        };
        ApiResultHandlerSelectVo: {
            /** Format: int32 */
            code?: number;
            msg?: string;
            data?: components["schemas"]["HandlerSelectVo"];
        };
        FlowPageHandlerAuth: {
            /** Format: int64 */
            total?: number;
            rows?: components["schemas"]["HandlerAuth"][];
            /** Format: int32 */
            code?: number;
            msg?: string;
        };
        HandlerAuth: {
            storageId?: string;
            handlerCode?: string;
            handlerName?: string;
            groupName?: string;
            createTime?: string;
        };
        HandlerSelectVo: {
            handlerAuths?: components["schemas"]["FlowPageHandlerAuth"];
            treeSelections?: components["schemas"]["Tree"][];
        };
        HandlerFeedBackDto: {
            storageIds?: string[];
        };
        ApiResultListHandlerFeedBackVo: {
            /** Format: int32 */
            code?: number;
            msg?: string;
            data?: components["schemas"]["HandlerFeedBackVo"][];
        };
        HandlerFeedBackVo: {
            storageId?: string;
            handlerName?: string;
        };
        ApiResultListDict: {
            /** Format: int32 */
            code?: number;
            msg?: string;
            data?: components["schemas"]["Dict"][];
        };
        Dict: {
            label?: string;
            value?: string;
            childList?: components["schemas"]["Dict"][];
        };
        ApiResultString: {
            /** Format: int32 */
            code?: number;
            msg?: string;
            data?: string;
        };
        ApiResultFlowDto: {
            /** Format: int32 */
            code?: number;
            msg?: string;
            data?: components["schemas"]["FlowDto"];
        };
        ApiResultWarmFlowVo: {
            /** Format: int32 */
            code?: number;
            msg?: string;
            data?: components["schemas"]["WarmFlowVo"];
        };
        WarmFlowVo: {
            tokenNameList?: string[];
            framework?: string;
        };
        /** @description 响应信息主体 */
        RObject: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: unknown;
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 响应信息主体 */
        RThirdProviderVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["ThirdProviderVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        ThirdProviderVo: {
            /** Format: int64 */
            providerId?: number;
            providerCode?: string;
            providerName?: string;
            baseUrl?: string;
            status?: string;
            /** Format: int32 */
            timeoutConnectMs?: number;
            /** Format: int32 */
            timeoutReadMs?: number;
            /** Format: int32 */
            rateLimit?: number;
            /** Format: int32 */
            concurrencyLimit?: number;
            sharedHeadersJson?: string;
            remark?: string;
            /** Format: int32 */
            version?: number;
            /** Format: date-time */
            createTime?: string;
            /** Format: date-time */
            updateTime?: string;
        };
        /** @description 响应信息主体 */
        RListThirdProviderVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["ThirdProviderVo"][];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 响应信息主体 */
        RThirdEndpointVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["ThirdEndpointVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        ThirdEndpointVo: {
            /** Format: int64 */
            endpointId?: number;
            /** Format: int64 */
            providerId?: number;
            providerCode?: string;
            endpointCode?: string;
            endpointName?: string;
            httpMethod?: string;
            relativePath?: string;
            requestMode?: string;
            responseMode?: string;
            pathSchemaJson?: string;
            querySchemaJson?: string;
            headerSchemaJson?: string;
            bodySchemaJson?: string;
            responseSchemaJson?: string;
            overrideJson?: string;
            status?: string;
            idempotent?: boolean;
            /** Format: int32 */
            rateLimit?: number;
            /** Format: int32 */
            concurrencyLimit?: number;
            /** Format: int32 */
            retryCount?: number;
            sensitiveFieldsJson?: string;
            adapterCode?: string;
            /** Format: int32 */
            version?: number;
            /** Format: date-time */
            createTime?: string;
            /** Format: date-time */
            updateTime?: string;
        };
        /** @description 响应信息主体 */
        RListThirdEndpointVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["ThirdEndpointVo"][];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 响应信息主体 */
        RSysUserTypeVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["SysUserTypeVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 登录域视图对象 sys_user_type */
        SysUserTypeVo: {
            /**
             * Format: int64
             * @description 登录域ID
             */
            userTypeId?: number;
            /** @description 登录域编码 */
            userTypeCode?: string;
            /** @description 登录域名称 */
            userTypeName?: string;
            /**
             * Format: int32
             * @description 显示顺序
             */
            orderNum?: number;
            /** @description 状态（0正常 1停用） */
            status?: string;
            /** @description 备注 */
            remark?: string;
            /**
             * Format: date-time
             * @description 创建时间
             */
            createTime?: string;
        };
        /** @description 响应信息主体 */
        RListSysUserTypeRelVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["SysUserTypeRelVo"][];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 用户登录域关系视图对象 sys_user_type_rel */
        SysUserTypeRelVo: {
            /**
             * Format: int64
             * @description 关系ID
             */
            relId?: number;
            /**
             * Format: int64
             * @description 用户ID
             */
            userId?: number;
            /**
             * Format: int64
             * @description 登录域ID
             */
            userTypeId?: number;
            /** @description 登录域编码 */
            userTypeCode?: string;
            /** @description 登录域名称 */
            userTypeName?: string;
            /** @description 授权来源 */
            grantSource?: string;
            /** @description 状态（0正常 1停用） */
            status?: string;
            /** @description 登录域状态（0正常 1停用） */
            userTypeStatus?: string;
            /**
             * Format: date-time
             * @description 创建时间
             */
            createTime?: string;
        };
        /** @description 响应信息主体 */
        RListSysUserTypeVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["SysUserTypeVo"][];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 表格分页数据对象 */
        PageResultSysUserTypeVo: {
            /**
             * Format: int64
             * @description 总记录数
             */
            total?: number;
            /** @description 列表数据 */
            rows?: components["schemas"]["SysUserTypeVo"][];
        };
        /** @description 响应信息主体 */
        RPageResultSysUserTypeVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["PageResultSysUserTypeVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 用户信息视图对象 sys_user */
        ProfileUserVo: {
            /**
             * Format: int64
             * @description 用户ID
             */
            userId?: number;
            /**
             * Format: int64
             * @description 部门ID
             */
            deptId?: number;
            /** @description 用户账号 */
            userName?: string;
            /** @description 用户昵称 */
            nickName?: string;
            /** @description 用户邮箱 */
            email?: string;
            /** @description 手机号码 */
            phoneNumber?: string;
            /** @description 用户性别（0男 1女 2未知） */
            gender?: string;
            /**
             * Format: int64
             * @description 头像 OSS ID
             */
            avatar?: number;
            /** @description 头像地址 */
            avatarUrl?: string;
            /** @description 最后登录IP */
            loginIp?: string;
            /**
             * Format: date-time
             * @description 最后登录时间
             */
            loginDate?: string;
            /** @description 部门名 */
            deptName?: string;
        };
        /** @description 用户个人信息 */
        ProfileVo: {
            /** @description 用户信息 */
            user?: components["schemas"]["ProfileUserVo"];
            /** @description 用户所属角色组 */
            roleGroup?: string;
            /** @description 用户所属岗位组 */
            postGroup?: string;
        };
        /** @description 响应信息主体 */
        RProfileVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["ProfileVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 响应信息主体 */
        RListSysUserVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["SysUserVo"][];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 表格分页数据对象 */
        PageResultSysUserVo: {
            /**
             * Format: int64
             * @description 总记录数
             */
            total?: number;
            /** @description 列表数据 */
            rows?: components["schemas"]["SysUserVo"][];
        };
        /** @description 响应信息主体 */
        RPageResultSysUserVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["PageResultSysUserVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 响应信息主体 */
        RUserInfoVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["UserInfoVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 登录用户信息 */
        UserInfoVo: {
            /** @description 用户基本信息 */
            user?: components["schemas"]["SysUserVo"];
            /** @description 菜单权限 */
            permissions?: string[];
            /** @description 角色权限 */
            roles?: string[];
        };
        /** @description 响应信息主体 */
        RSysUserInfoVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["SysUserInfoVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 用户信息 */
        SysUserInfoVo: {
            /** @description 用户信息 */
            user?: components["schemas"]["SysUserVo"];
            /** @description 角色ID列表 */
            roleIds?: number[];
            /** @description 角色列表 */
            roles?: components["schemas"]["SysRoleVo"][];
            /** @description 岗位ID列表 */
            postIds?: number[];
            /** @description 岗位列表 */
            posts?: components["schemas"]["SysPostVo"][];
        };
        /** @description 响应信息主体 */
        RListSysSocialVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["SysSocialVo"][];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 社会化关系视图对象 sys_social */
        SysSocialVo: {
            /**
             * Format: int64
             * @description 主键
             */
            id?: number;
            /**
             * Format: int64
             * @description 用户ID
             */
            userId?: number;
            /** @description 的唯一ID */
            authId?: string;
            /** @description 用户来源 */
            source?: string;
            /** @description 用户的授权令牌 */
            accessToken?: string;
            /**
             * Format: int32
             * @description 用户的授权令牌的有效期，部分平台可能没有
             */
            expireIn?: number;
            /** @description 刷新令牌，部分平台可能没有 */
            refreshToken?: string;
            /** @description 用户的 open id */
            openId?: string;
            /** @description 授权的第三方账号 */
            userName?: string;
            /** @description 授权的第三方昵称 */
            nickName?: string;
            /** @description 授权的第三方邮箱 */
            email?: string;
            /** @description 授权的第三方头像地址 */
            avatar?: string;
            /** @description 平台的授权信息，部分平台可能没有 */
            accessCode?: string;
            /** @description 用户的 unionid */
            unionId?: string;
            /** @description 授予的权限，部分平台可能没有 */
            scope?: string;
            /** @description 个别平台的授权信息，部分平台可能没有 */
            tokenType?: string;
            /** @description id token，部分平台可能没有 */
            idToken?: string;
            /** @description 小米平台用户的附带属性，部分平台可能没有 */
            macAlgorithm?: string;
            /** @description 小米平台用户的附带属性，部分平台可能没有 */
            macKey?: string;
            /** @description 用户的授权code，部分平台可能没有 */
            code?: string;
            /** @description Twitter平台用户的附带属性，部分平台可能没有 */
            oauthToken?: string;
            /** @description Twitter平台用户的附带属性，部分平台可能没有 */
            oauthTokenSecret?: string;
            /**
             * Format: date-time
             * @description 创建时间
             */
            createTime?: string;
        };
        /** @description 响应信息主体 */
        RSysRoleVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["SysRoleVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 响应信息主体 */
        RListSysRoleVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["SysRoleVo"][];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 表格分页数据对象 */
        PageResultSysRoleVo: {
            /**
             * Format: int64
             * @description 总记录数
             */
            total?: number;
            /** @description 列表数据 */
            rows?: components["schemas"]["SysRoleVo"][];
        };
        /** @description 响应信息主体 */
        RPageResultSysRoleVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["PageResultSysRoleVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 角色部门列表树信息 */
        DeptTreeSelectVo: {
            /** @description 选中部门列表 */
            checkedKeys?: number[];
            /** @description 下拉树结构列表 */
            depts?: components["schemas"]["TreeString"][];
        };
        /** @description 响应信息主体 */
        RDeptTreeSelectVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["DeptTreeSelectVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 响应信息主体 */
        RSysPostVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["SysPostVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 响应信息主体 */
        RListSysPostVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["SysPostVo"][];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 表格分页数据对象 */
        PageResultSysPostVo: {
            /**
             * Format: int64
             * @description 总记录数
             */
            total?: number;
            /** @description 列表数据 */
            rows?: components["schemas"]["SysPostVo"][];
        };
        /** @description 响应信息主体 */
        RPageResultSysPostVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["PageResultSysPostVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description Super-admin search row with an optional safe credential summary. */
        OpenApiCredentialUserSummary: {
            /** Format: int64 */
            userId?: number;
            userName?: string;
            nickName?: string;
            credential?: components["schemas"]["OpenApiCredentialSummary"];
        };
        /** @description 响应信息主体 */
        RListOpenApiCredentialUserSummary: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["OpenApiCredentialUserSummary"][];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description Raw Sa-Token constraints attached to one mapped handler. */
        OpenApiAccessRule: {
            permissions?: components["schemas"]["PermissionRule"][];
            roles?: components["schemas"]["RoleRule"][];
        };
        /** @description Credential-independent catalog view of one callable operation. */
        OpenApiCatalogItem: {
            interfaceId?: string;
            summary?: string;
            method?: string;
            path?: string;
            accessRule?: components["schemas"]["OpenApiAccessRule"];
            parameters?: components["schemas"]["OpenApiParameterDefinition"][];
            requestSchema?: string;
            responseSchema?: string;
            curlExample?: string;
            javaExample?: string;
        };
        /** @description Spring MVC parameter with a Swagger-resolved schema. */
        OpenApiParameterDefinition: {
            name?: string;
            location?: string;
            required?: boolean;
            schema?: string;
        };
        PermissionRule: {
            values?: string[];
            /** @enum {string} */
            mode?: "AND" | "OR";
            orRoles?: string[];
        };
        /** @description 响应信息主体 */
        RListOpenApiCatalogItem: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["OpenApiCatalogItem"][];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        RoleRule: {
            values?: string[];
            /** @enum {string} */
            mode?: "AND" | "OR";
        };
        /** @description 响应信息主体 */
        ROpenApiCatalogItem: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["OpenApiCatalogItem"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 响应信息主体 */
        RSysMenuVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["SysMenuVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 菜单权限视图对象 sys_menu */
        SysMenuVo: {
            /**
             * Format: int64
             * @description 菜单ID
             */
            menuId?: number;
            /**
             * Format: int64
             * @description 归属客户端主键
             */
            clientId?: number;
            /** @description 菜单名称 */
            menuName?: string;
            /**
             * Format: int64
             * @description 父菜单ID
             */
            parentId?: number;
            /**
             * Format: int32
             * @description 显示顺序
             */
            orderNum?: number;
            /** @description 路由地址 */
            path?: string;
            /** @description 组件路径 */
            component?: string;
            /** @description 路由参数 */
            queryParam?: string;
            /** @description 是否为外链（Y是 N否） */
            isFrame?: string;
            /** @description 是否缓存（Y缓存 N不缓存） */
            isCache?: string;
            /** @description 菜单类型（M目录 C菜单 F按钮） */
            menuType?: string;
            /** @description 显示状态（0显示 1隐藏） */
            visible?: string;
            /** @description 菜单状态（0正常 1停用） */
            status?: string;
            /** @description 权限标识 */
            perms?: string;
            /** @description 菜单图标 */
            icon?: string;
            /** @description 激活菜单路径 */
            activeMenu?: string;
            /** @description 扩展字段 */
            ext?: string;
            /**
             * Format: int64
             * @description 创建部门
             */
            createDept?: number;
            /** @description 备注 */
            remark?: string;
            /**
             * Format: date-time
             * @description 创建时间
             */
            createTime?: string;
            /** @description 子菜单 */
            children?: components["schemas"]["SysMenuVo"][];
        };
        /** @description 角色菜单列表树信息 */
        MenuTreeSelectVo: {
            /** @description 选中菜单列表 */
            checkedKeys?: number[];
            /** @description 菜单下拉树结构列表 */
            menus?: components["schemas"]["TreeString"][];
        };
        /** @description 响应信息主体 */
        RMenuTreeSelectVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["MenuTreeSelectVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 响应信息主体 */
        RListSysMenuVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["SysMenuVo"][];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 路由显示信息 */
        MetaVo: {
            /** @description 设置该路由在侧边栏和面包屑中展示的名字 */
            title?: string;
            /**
             * @description 设置该路由的图标。支持本地 SVG 名称（对应前端 src/assets/icons/svg）、
             *      离线 Tabler 名称（tabler:users）以及显式的外部 Iconify 名称。
             */
            icon?: string;
            /** @description 设置为true，则不会被 <keep-alive>缓存 */
            noCache?: boolean;
            /** @description 内链地址（http(s)://开头） */
            link?: string;
            /** @description 激活菜单 */
            activeMenu?: string;
        };
        /** @description 响应信息主体 */
        RListRouterVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["RouterVo"][];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 路由配置信息 */
        RouterVo: {
            /** @description 路由名字 */
            name?: string;
            /** @description 路由地址 */
            path?: string;
            /** @description 是否隐藏路由，当设置 true 的时候该路由不会再侧边栏出现 */
            hidden?: boolean;
            /** @description 重定向地址，当设置 noRedirect 的时候该路由在面包屑导航中不可被点击 */
            redirect?: string;
            /** @description 组件地址 */
            component?: string;
            /** @description 路由参数：如 {"id": 1, "name": "ry"} */
            query?: string;
            /** @description 扩展字段 */
            ext?: string;
            /** @description 当你一个路由下面的 children 声明的路由大于1个时，自动会变成嵌套的模式--如组件页面 */
            alwaysShow?: boolean;
            /** @description 其他元素 */
            meta?: components["schemas"]["MetaVo"];
            /** @description 子路由 */
            children?: components["schemas"]["RouterVo"][];
        };
        /** @description 响应信息主体 */
        RSysDictTypeVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["SysDictTypeVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 字典类型视图对象 sys_dict_type */
        SysDictTypeVo: {
            /**
             * Format: int64
             * @description 字典主键
             */
            dictId?: number;
            /** @description 字典名称 */
            dictName?: string;
            /** @description 字典类型 */
            dictType?: string;
            /** @description 备注 */
            remark?: string;
            /**
             * Format: date-time
             * @description 创建时间
             */
            createTime?: string;
        };
        /** @description 响应信息主体 */
        RListSysDictTypeVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["SysDictTypeVo"][];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 表格分页数据对象 */
        PageResultSysDictTypeVo: {
            /**
             * Format: int64
             * @description 总记录数
             */
            total?: number;
            /** @description 列表数据 */
            rows?: components["schemas"]["SysDictTypeVo"][];
        };
        /** @description 响应信息主体 */
        RPageResultSysDictTypeVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["PageResultSysDictTypeVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 响应信息主体 */
        RSysDictDataVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["SysDictDataVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 字典数据视图对象 sys_dict_data */
        SysDictDataVo: {
            /**
             * Format: int64
             * @description 字典编码
             */
            dictCode?: number;
            /**
             * Format: int32
             * @description 字典排序
             */
            dictSort?: number;
            /** @description 字典标签 */
            dictLabel?: string;
            /** @description 字典键值 */
            dictValue?: string;
            /** @description 字典类型 */
            dictType?: string;
            /** @description 样式属性（其他样式扩展） */
            cssClass?: string;
            /** @description 表格回显样式 */
            listClass?: string;
            /** @description 是否默认（Y是 N否） */
            isDefault?: string;
            /** @description 备注 */
            remark?: string;
            /**
             * Format: date-time
             * @description 创建时间
             */
            createTime?: string;
        };
        /** @description 响应信息主体 */
        RListSysDictDataVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["SysDictDataVo"][];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 表格分页数据对象 */
        PageResultSysDictDataVo: {
            /**
             * Format: int64
             * @description 总记录数
             */
            total?: number;
            /** @description 列表数据 */
            rows?: components["schemas"]["SysDictDataVo"][];
        };
        /** @description 响应信息主体 */
        RPageResultSysDictDataVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["PageResultSysDictDataVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 响应信息主体 */
        RSysDeptVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["SysDeptVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 部门视图对象 sys_dept */
        SysDeptVo: {
            /**
             * Format: int64
             * @description 部门id
             */
            deptId?: number;
            /**
             * Format: int64
             * @description 父部门id
             */
            parentId?: number;
            /** @description 父部门名称 */
            parentName?: string;
            /** @description 祖级列表 */
            ancestors?: string;
            /** @description 部门名称 */
            deptName?: string;
            /** @description 部门类别编码 */
            deptCategory?: string;
            /**
             * Format: int32
             * @description 显示顺序
             */
            orderNum?: number;
            /**
             * Format: int64
             * @description 负责人ID
             */
            leader?: number;
            /** @description 负责人 */
            leaderName?: string;
            /** @description 联系电话 */
            phone?: string;
            /** @description 邮箱 */
            email?: string;
            /** @description 部门状态（0正常 1停用） */
            status?: string;
            /**
             * Format: date-time
             * @description 创建时间
             */
            createTime?: string;
            /** @description 子部门 */
            children?: components["schemas"]["SysDeptVo"][];
        };
        /** @description 响应信息主体 */
        RListSysDeptVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["SysDeptVo"][];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 响应信息主体 */
        RSysConfigVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["SysConfigVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 参数配置视图对象 sys_config */
        SysConfigVo: {
            /**
             * Format: int64
             * @description 参数主键
             */
            configId?: number;
            /** @description 参数名称 */
            configName?: string;
            /** @description 参数键名 */
            configKey?: string;
            /** @description 参数键值 */
            configValue?: string;
            /** @description 系统内置（Y是 N否） */
            configType?: string;
            /** @description 备注 */
            remark?: string;
            /**
             * Format: date-time
             * @description 创建时间
             */
            createTime?: string;
        };
        /** @description 表格分页数据对象 */
        PageResultSysConfigVo: {
            /**
             * Format: int64
             * @description 总记录数
             */
            total?: number;
            /** @description 列表数据 */
            rows?: components["schemas"]["SysConfigVo"][];
        };
        /** @description 响应信息主体 */
        RPageResultSysConfigVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["PageResultSysConfigVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 响应信息主体 */
        RSysClientVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["SysClientVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 授权管理视图对象 sys_client */
        SysClientVo: {
            /**
             * Format: int64
             * @description id
             */
            id?: number;
            /** @description 客户端id */
            clientId?: string;
            /** @description 客户端key */
            clientKey?: string;
            /** @description 客户端秘钥 */
            clientSecret?: string;
            /** @description 授权类型 */
            grantTypeList?: string[];
            /** @description 授权类型 */
            grantType?: string;
            /** @description 设备类型 */
            deviceType?: string;
            /** @description 允许访问路径 */
            accessPath?: string;
            /** @description 允许访问路径列表 */
            accessPathList?: string[];
            /** @description IP白名单 */
            ipWhitelist?: string;
            /** @description IP白名单列表 */
            ipWhitelistList?: string[];
            /**
             * Format: int64
             * @description token活跃超时时间
             */
            activeTimeout?: number;
            /**
             * Format: int64
             * @description token固定超时时间
             */
            timeout?: number;
            /**
             * Format: int64
             * @description 登录域ID
             */
            userTypeId?: number;
            /** @description 登录域编码 */
            userTypeCode?: string;
            /** @description 登录域名称 */
            userTypeName?: string;
            /** @description 是否开放公开注册 */
            registerEnabled?: boolean;
            /**
             * Format: int64
             * @description 默认角色ID
             */
            defaultRoleId?: number;
            /** @description 默认角色名称 */
            defaultRoleName?: string;
            /** @description 状态（0正常 1停用） */
            status?: string;
        };
        /** @description 表格分页数据对象 */
        PageResultSysClientVo: {
            /**
             * Format: int64
             * @description 总记录数
             */
            total?: number;
            /** @description 列表数据 */
            rows?: components["schemas"]["SysClientVo"][];
        };
        /** @description 响应信息主体 */
        RPageResultSysClientVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["PageResultSysClientVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        OssAccessUrl: {
            accessType?: string;
            url?: string;
            /** Format: date-time */
            expiresAt?: string;
            fileName?: string;
        };
        /** @description 响应信息主体 */
        ROssAccessUrl: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["OssAccessUrl"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 响应信息主体 */
        RResumeResponse: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["ResumeResponse"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        ResumeResponse: {
            uploadToken?: string;
            /** @enum {string} */
            mode?: "AUTO" | "SINGLE" | "MULTIPART";
            /** @enum {string} */
            state?: "INITIALIZED" | "UPLOADING" | "COMPLETING" | "COMPLETED" | "ABORTED" | "EXPIRED";
            completedOssId?: string;
            fileName?: string;
            /** Format: int64 */
            fileSize?: number;
            contentType?: string;
            /** Format: int64 */
            partSize?: number;
            /** Format: int32 */
            partCount?: number;
            /** Format: date-time */
            expiresAt?: string;
            presignedRequest?: components["schemas"]["OssPresignedRequest"];
            uploadedParts?: components["schemas"]["UploadedPart"][];
        };
        UploadedPart: {
            /** Format: int32 */
            partNumber?: number;
            eTag?: string;
            /** Format: int64 */
            size?: number;
        };
        BatchView: {
            /** Format: int64 */
            batchId?: number;
            targetConfigKey?: string;
            /** @enum {string} */
            status?: "PENDING" | "RUNNING" | "FAILED" | "CLEANUP_ELIGIBLE" | "COMPLETED" | "ROLLED_BACK";
            /** Format: int32 */
            totalCount?: number;
            /** Format: int32 */
            successCount?: number;
            /** Format: int32 */
            failedCount?: number;
            /** Format: date-time */
            startedTime?: string;
            /** Format: date-time */
            completedTime?: string;
        };
        /** @description 响应信息主体 */
        RBatchView: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["BatchView"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        ItemView: {
            /** Format: int64 */
            itemId?: number;
            /** Format: int64 */
            ossId?: number;
            sourceConfigKey?: string;
            targetConfigKey?: string;
            objectKey?: string;
            /** @enum {string} */
            status?: "PENDING" | "RUNNING" | "FAILED" | "CLEANUP_ELIGIBLE" | "COMPLETED" | "ROLLED_BACK";
            /** @enum {string} */
            stage?: "PREFLIGHT" | "COPIED" | "CONTENT_VERIFIED" | "SERVICE_SWITCHED" | "ACCESS_VERIFIED" | "CLEANUP_ELIGIBLE" | "COMPLETED" | "ROLLED_BACK";
            /** Format: int32 */
            retryCount?: number;
            /** @enum {string} */
            lastErrorStage?: "PREFLIGHT" | "COPIED" | "CONTENT_VERIFIED" | "SERVICE_SWITCHED" | "ACCESS_VERIFIED" | "CLEANUP_ELIGIBLE" | "COMPLETED" | "ROLLED_BACK";
            errorMessage?: string;
            /** Format: date-time */
            cleanupEligibleTime?: string;
        };
        /** @description 响应信息主体 */
        RListItemView: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["ItemView"][];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description OSS对象存储分页查询对象 sys_oss */
        SysOssBo: {
            /**
             * Format: int64
             * @description ossId
             */
            ossId?: number;
            /** @description 文件名 */
            fileName?: string;
            /** @description 原名 */
            originalName?: string;
            /** @description 文件后缀名 */
            fileSuffix?: string;
            /** @description URL地址 */
            url?: string;
            /** @description 扩展字段 */
            ext1?: string;
            /** @description 服务商 */
            service?: string;
            /** @description 是否为临时对象。 */
            isTemp?: string;
            /**
             * Format: int64
             * @description 创建者
             */
            createBy?: number;
            /** @description 请求参数 */
            params?: {
                [key: string]: unknown;
            };
        };
        OssReference: {
            refType?: string;
            refId?: string;
        };
        /** @description 表格分页数据对象 */
        PageResultSysOssVo: {
            /**
             * Format: int64
             * @description 总记录数
             */
            total?: number;
            /** @description 列表数据 */
            rows?: components["schemas"]["SysOssVo"][];
        };
        /** @description 响应信息主体 */
        RPageResultSysOssVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["PageResultSysOssVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description OSS对象存储视图对象 sys_oss */
        SysOssVo: {
            /**
             * Format: int64
             * @description 对象存储主键
             */
            ossId?: number;
            /** @description 文件名 */
            fileName?: string;
            /** @description 原名 */
            originalName?: string;
            /** @description 文件后缀名 */
            fileSuffix?: string;
            /** @description URL地址 */
            url?: string;
            /** @description 扩展字段 */
            ext1?: string;
            /**
             * Format: date-time
             * @description 创建时间
             */
            createTime?: string;
            /**
             * Format: int64
             * @description 上传人
             */
            createBy?: number;
            /** @description 上传人名称 */
            createByName?: string;
            /** @description 服务商 */
            service?: string;
            /** @description 是否为临时对象。 */
            isTemp?: string;
            /**
             * Format: date-time
             * @description 临时对象到期时间。
             */
            expireTime?: string;
            /** @description 对象删除状态（ACTIVE 正常，PENDING 等待供应商删除）。 */
            deleteState?: string;
            /**
             * Format: int64
             * @description 当前有效业务引用数。
             */
            referenceCount?: number;
            /** @description 用于管理面反向定位的引用摘要。 */
            references?: components["schemas"]["OssReference"][];
        };
        /** @description 响应信息主体 */
        RListSysOssVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["SysOssVo"][];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 响应信息主体 */
        RSysOssConfigVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["SysOssConfigVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 对象存储配置视图对象 sys_oss_config */
        SysOssConfigVo: {
            /**
             * Format: int64
             * @description 主键
             */
            ossConfigId?: number;
            /** @description 配置key */
            configKey?: string;
            /** @description accessKey */
            accessKey?: string;
            /** @description 桶名称 */
            bucketName?: string;
            /** @description 前缀 */
            prefix?: string;
            /** @description 访问站点 */
            endpoint?: string;
            /** @description 自定义域名 */
            domainUrl?: string;
            /** @description 是否https（Y=是,N=否） */
            isHttps?: string;
            /** @description 域 */
            region?: string;
            /** @description 是否默认（Y=是,N=否） */
            status?: string;
            /** @description 扩展字段 */
            ext1?: string;
            /** @description 备注 */
            remark?: string;
            /** @description 桶权限类型（0=PRIVATE，2=PUBLIC_READ） */
            accessPolicy?: string;
        };
        /** @description 表格分页数据对象 */
        PageResultSysOssConfigVo: {
            /**
             * Format: int64
             * @description 总记录数
             */
            total?: number;
            /** @description 列表数据 */
            rows?: components["schemas"]["SysOssConfigVo"][];
        };
        /** @description 响应信息主体 */
        RPageResultSysOssConfigVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["PageResultSysOssConfigVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        SseEmitter: {
            /** Format: int64 */
            timeout?: number;
        };
        /** @description 响应信息主体 */
        RListMaterialReferenceView: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["MaterialReferenceView"][];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /**
         * @description 个人档案材料访问地址响应。
         *
         *      <p>该响应只描述 Profile 对外的 HTTP 数据，不暴露 system 模块的内部类型。</p>
         */
        PersonProfileAccessUrl: {
            /** @description 访问类型 */
            accessType?: string;
            /** @description 访问地址 */
            url?: string;
            /**
             * Format: date-time
             * @description 私有地址过期时间
             */
            expiresAt?: string;
            /** @description 文件名 */
            fileName?: string;
        };
        /** @description 响应信息主体 */
        RPersonProfileAccessUrl: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["PersonProfileAccessUrl"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description PersonAdminQueryBo 请求参数模型。 */
        PersonAdminQueryBo: {
            fullName?: string;
            documentNumber?: string;
            status?: string;
            /** Format: int32 */
            pageNum?: number;
            /** Format: int32 */
            pageSize?: number;
        };
        /** @description 表格分页数据对象 */
        PageResultPersonProfileSummaryVo: {
            /**
             * Format: int64
             * @description 总记录数
             */
            total?: number;
            /** @description 列表数据 */
            rows?: components["schemas"]["PersonProfileSummaryVo"][];
        };
        /** @description PersonProfileSummaryVo 对外返回模型。 */
        PersonProfileSummaryVo: {
            /** Format: int64 */
            profileId?: number;
            /** Format: int64 */
            previousProfileId?: number;
            fullName?: string;
            documentTypeCode?: string;
            documentNumber?: string;
            gender?: string;
            /** Format: date */
            birthDate?: string;
            status?: string;
            /** Format: int64 */
            bindingUserId?: number;
            bindingStatus?: string;
            /** Format: date-time */
            createTime?: string;
        };
        /** @description 响应信息主体 */
        RPageResultPersonProfileSummaryVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["PageResultPersonProfileSummaryVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description PersonProfileAuditVo 对外返回模型。 */
        PersonProfileAuditVo: {
            /** Format: int64 */
            auditId?: number;
            operationType?: string;
            /** Format: int64 */
            operatorUserId?: number;
            capability?: string;
            reason?: string;
            beforeStatus?: string;
            afterStatus?: string;
            result?: string;
            failureCategory?: string;
            /** Format: date-time */
            occurredTime?: string;
        };
        /** @description PersonProfileBindingVo 对外返回模型。 */
        PersonProfileBindingVo: {
            /** Format: int64 */
            bindingId?: number;
            /** Format: int64 */
            userId?: number;
            status?: string;
            /** Format: int32 */
            bindingVersion?: number;
            sourceType?: string;
            /** Format: int64 */
            sourceId?: number;
            /** Format: date-time */
            boundTime?: string;
            /** Format: date-time */
            unboundTime?: string;
        };
        /** @description PersonProfileDetailVo 对外返回模型。 */
        PersonProfileDetailVo: {
            profile?: components["schemas"]["PersonProfileSummaryVo"];
            versions?: components["schemas"]["PersonProfileVersionVo"][];
            bindings?: components["schemas"]["PersonProfileBindingVo"][];
            sources?: components["schemas"]["PersonProfileSourceVo"][];
            audits?: components["schemas"]["PersonProfileAuditVo"][];
            currentMaterials?: components["schemas"]["MaterialReferenceView"][];
        };
        /** @description PersonProfileSourceVo 对外返回模型。 */
        PersonProfileSourceVo: {
            /** Format: int64 */
            sourceId?: number;
            sourceType?: string;
            /** Format: int64 */
            operatorUserId?: number;
            reason?: string;
            fieldSnapshotJson?: string;
            /** Format: date-time */
            occurredTime?: string;
        };
        /** @description PersonProfileVersionVo 对外返回模型。 */
        PersonProfileVersionVo: {
            /** Format: int64 */
            versionId?: number;
            /** Format: int32 */
            versionNo?: number;
            sourceType?: string;
            /** Format: int64 */
            sourceId?: number;
            fullName?: string;
            documentTypeCode?: string;
            documentNumber?: string;
            gender?: string;
            /** Format: date */
            birthDate?: string;
            /** Format: date */
            validFrom?: string;
            /** Format: date */
            validUntil?: string;
            status?: string;
            /** Format: date-time */
            publishedTime?: string;
        };
        /** @description 响应信息主体 */
        RPersonProfileDetailVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["PersonProfileDetailVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description PersonAccountCandidateVo 对外返回模型。 */
        PersonAccountCandidateVo: {
            /** Format: int64 */
            userId?: number;
            userName?: string;
            nickName?: string;
        };
        /** @description 响应信息主体 */
        RListPersonAccountCandidateVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["PersonAccountCandidateVo"][];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description PersonReviewContextVo 对外返回模型。 */
        PersonReviewContextVo: {
            /** Format: int64 */
            applicationId?: number;
            /** Format: int64 */
            applicantUserId?: number;
            status?: string;
            /** Format: int32 */
            submissionSeq?: number;
            /** Format: int32 */
            decisionVersion?: number;
            /** Format: int32 */
            version?: number;
            /** Format: int64 */
            submissionId?: number;
            fieldSnapshotJson?: string;
            /** Format: date-time */
            submittedTime?: string;
            materials?: components["schemas"]["MaterialReferenceView"][];
        };
        /** @description 响应信息主体 */
        RPersonReviewContextVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["PersonReviewContextVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 响应信息主体 */
        RListMaterialNodeView: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["MaterialNodeView"][];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 企业档案材料访问地址响应。 */
        EnterpriseProfileAccessUrl: {
            /** @description 访问类型 */
            accessType?: string;
            /** @description 访问地址 */
            url?: string;
            /**
             * Format: date-time
             * @description 私有地址过期时间
             */
            expiresAt?: string;
            /** @description 文件名 */
            fileName?: string;
        };
        /** @description 响应信息主体 */
        REnterpriseProfileAccessUrl: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["EnterpriseProfileAccessUrl"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description EnterpriseAdminQueryBo 请求参数模型。 */
        EnterpriseAdminQueryBo: {
            enterpriseName?: string;
            unifiedCreditCode?: string;
            status?: string;
            /** Format: int32 */
            pageNum?: number;
            /** Format: int32 */
            pageSize?: number;
        };
        /** @description EnterpriseProfileSummaryVo 对外返回模型。 */
        EnterpriseProfileSummaryVo: {
            /** Format: int64 */
            profileId?: number;
            /** Format: int64 */
            previousProfileId?: number;
            enterpriseName?: string;
            unifiedCreditCode?: string;
            enterpriseType?: string;
            legalRepresentativeName?: string;
            status?: string;
            /** Format: int64 */
            bindingUserId?: number;
            bindingStatus?: string;
            /** Format: date-time */
            createTime?: string;
        };
        /** @description 表格分页数据对象 */
        PageResultEnterpriseProfileSummaryVo: {
            /**
             * Format: int64
             * @description 总记录数
             */
            total?: number;
            /** @description 列表数据 */
            rows?: components["schemas"]["EnterpriseProfileSummaryVo"][];
        };
        /** @description 响应信息主体 */
        RPageResultEnterpriseProfileSummaryVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["PageResultEnterpriseProfileSummaryVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description EnterpriseProfileAuditVo 对外返回模型。 */
        EnterpriseProfileAuditVo: {
            /** Format: int64 */
            auditId?: number;
            operationType?: string;
            /** Format: int64 */
            operatorUserId?: number;
            capability?: string;
            reason?: string;
            beforeStatus?: string;
            afterStatus?: string;
            result?: string;
            failureCategory?: string;
            /** Format: date-time */
            occurredTime?: string;
        };
        /** @description EnterpriseProfileBindingVo 对外返回模型。 */
        EnterpriseProfileBindingVo: {
            /** Format: int64 */
            bindingId?: number;
            /** Format: int64 */
            userId?: number;
            status?: string;
            /** Format: int32 */
            bindingVersion?: number;
            sourceType?: string;
            /** Format: int64 */
            sourceId?: number;
            /** Format: date-time */
            boundTime?: string;
            /** Format: date-time */
            unboundTime?: string;
        };
        /** @description EnterpriseProfileDetailVo 对外返回模型。 */
        EnterpriseProfileDetailVo: {
            profile?: components["schemas"]["EnterpriseProfileSummaryVo"];
            versions?: components["schemas"]["EnterpriseProfileVersionVo"][];
            bindings?: components["schemas"]["EnterpriseProfileBindingVo"][];
            sources?: components["schemas"]["EnterpriseProfileSourceVo"][];
            audits?: components["schemas"]["EnterpriseProfileAuditVo"][];
            currentMaterials?: components["schemas"]["MaterialReferenceView"][];
        };
        /** @description EnterpriseProfileSourceVo 对外返回模型。 */
        EnterpriseProfileSourceVo: {
            /** Format: int64 */
            sourceId?: number;
            sourceType?: string;
            /** Format: int64 */
            operatorUserId?: number;
            reason?: string;
            fieldSnapshotJson?: string;
            /** Format: date-time */
            occurredTime?: string;
        };
        /** @description EnterpriseProfileVersionVo 对外返回模型。 */
        EnterpriseProfileVersionVo: {
            /** Format: int64 */
            versionId?: number;
            /** Format: int32 */
            versionNo?: number;
            sourceType?: string;
            /** Format: int64 */
            sourceId?: number;
            enterpriseName?: string;
            unifiedCreditCode?: string;
            enterpriseType?: string;
            legalRepresentativeName?: string;
            legalDocumentTypeCode?: string;
            legalDocumentNumber?: string;
            /** Format: date */
            establishedDate?: string;
            /** Format: date */
            businessTermFrom?: string;
            /** Format: date */
            businessTermUntil?: string;
            registeredAddress?: string;
            businessScope?: string;
            contactName?: string;
            contactPhone?: string;
            email?: string;
            registeredCapital?: number;
            industryCode?: string;
            website?: string;
            status?: string;
            /** Format: date-time */
            publishedTime?: string;
        };
        /** @description 响应信息主体 */
        REnterpriseProfileDetailVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["EnterpriseProfileDetailVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description EnterpriseAccountCandidateVo 对外返回模型。 */
        EnterpriseAccountCandidateVo: {
            /** Format: int64 */
            userId?: number;
            userName?: string;
            nickName?: string;
        };
        /** @description 响应信息主体 */
        RListEnterpriseAccountCandidateVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["EnterpriseAccountCandidateVo"][];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description EnterpriseReviewContextVo 对外返回模型。 */
        EnterpriseReviewContextVo: {
            /** Format: int64 */
            applicationId?: number;
            /** Format: int64 */
            applicantUserId?: number;
            status?: string;
            /** Format: int32 */
            submissionSeq?: number;
            /** Format: int32 */
            decisionVersion?: number;
            /** Format: int32 */
            version?: number;
            /** Format: int64 */
            submissionId?: number;
            fieldSnapshotJson?: string;
            /** Format: date-time */
            submittedTime?: string;
            materials?: components["schemas"]["MaterialReferenceView"][];
        };
        /** @description 响应信息主体 */
        REnterpriseReviewContextVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["EnterpriseReviewContextVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 通知目标选择器使用的最小用户视图。 */
        NotifyRecipientUserVo: {
            /** Format: int64 */
            userId?: number;
            userName?: string;
            nickName?: string;
            phoneNumber?: string;
            status?: string;
        };
        /** @description 表格分页数据对象 */
        PageResultNotifyRecipientUserVo: {
            /**
             * Format: int64
             * @description 总记录数
             */
            total?: number;
            /** @description 列表数据 */
            rows?: components["schemas"]["NotifyRecipientUserVo"][];
        };
        /** @description 响应信息主体 */
        RPageResultNotifyRecipientUserVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["PageResultNotifyRecipientUserVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 响应信息主体 */
        RListNotifyRecipientUserVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["NotifyRecipientUserVo"][];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 通知状态快照。 */
        NotificationSnapshot: {
            /** @description 通知主键 */
            notificationId?: string;
            /**
             * @description 聚合状态
             * @enum {string}
             */
            status?: "QUEUED" | "PROCESSING" | "ACCEPTED" | "PARTIAL_FAILURE" | "DELIVERED" | "UNDELIVERABLE" | "UNKNOWN" | "FAILED" | "CANCELLED" | "EXPIRED";
            /**
             * Format: date-time
             * @description 创建时间
             */
            createdAt?: string;
            /** @description 投递明细 */
            deliveries?: components["schemas"]["DeliveryReceipt"][];
        };
        /** @description 响应信息主体 */
        RNotificationSnapshot: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["NotificationSnapshot"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 通知中心公告展示对象。 */
        NotifyNoticeVo: {
            /** Format: int64 */
            noticeId?: number;
            noticeTitle?: string;
            noticeType?: string;
            noticeContent?: string;
            recipientType?: string;
            recipientIds?: number[];
            userTypeIds?: number[];
            channels?: string[];
            status?: string;
            lifecycle?: string;
            /** Format: date-time */
            publishedAt?: string;
            /** Format: date-time */
            retractedAt?: string;
            remark?: string;
            /** Format: int64 */
            createBy?: number;
            /** Format: date-time */
            createTime?: string;
        };
        /** @description 响应信息主体 */
        RNotifyNoticeVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["NotifyNoticeVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 表格分页数据对象 */
        PageResultNotifyNoticeVo: {
            /**
             * Format: int64
             * @description 总记录数
             */
            total?: number;
            /** @description 列表数据 */
            rows?: components["schemas"]["NotifyNoticeVo"][];
        };
        /** @description 响应信息主体 */
        RPageResultNotifyNoticeVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["PageResultNotifyNoticeVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 通知监控安全投影，不暴露内部实体和敏感目标。 */
        NotificationDeliveryView: {
            /** Format: int64 */
            deliveryId?: number;
            /** Format: int64 */
            intentId?: number;
            /** Format: int64 */
            userId?: number;
            channel?: string;
            status?: string;
            /** Format: int32 */
            attemptCount?: number;
            providerMessageId?: string;
            errorCode?: string;
            /** Format: date-time */
            acceptedAt?: string;
            /** Format: date-time */
            deliveredAt?: string;
            /** Format: date-time */
            readAt?: string;
            /** Format: date-time */
            createTime?: string;
        };
        /** @description 响应信息主体 */
        RListNotificationDeliveryView: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["NotificationDeliveryView"][];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 当前用户收件箱消息及其阅读状态。 */
        NotifyInboxMessageVo: {
            /** Format: int64 */
            messageId?: number;
            category?: string;
            noticeType?: string;
            channels?: string[];
            type?: string;
            source?: string;
            title?: string;
            message?: string;
            content?: string;
            path?: string;
            /** Format: date-time */
            createTime?: string;
            /** Format: date-time */
            seenTime?: string;
            /** Format: date-time */
            readTime?: string;
        };
        /** @description 响应信息主体 */
        RListNotifyInboxMessageVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["NotifyInboxMessageVo"][];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 表格分页数据对象 */
        PageResultSysOperLogVo: {
            /**
             * Format: int64
             * @description 总记录数
             */
            total?: number;
            /** @description 列表数据 */
            rows?: components["schemas"]["SysOperLogVo"][];
        };
        /** @description 响应信息主体 */
        RPageResultSysOperLogVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["PageResultSysOperLogVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 操作日志记录视图对象 sys_oper_log */
        SysOperLogVo: {
            /**
             * Format: int64
             * @description 日志主键
             */
            operId?: number;
            /** @description 模块标题 */
            title?: string;
            /**
             * Format: int32
             * @description 业务类型（0其它 1新增 2修改 3删除）
             */
            businessType?: number;
            /** @description 业务类型数组 */
            businessTypes?: number[];
            /** @description 方法名称 */
            method?: string;
            /** @description 请求方式 */
            requestMethod?: string;
            /**
             * Format: int32
             * @description 操作类别（0其它 1后台用户 2手机端用户）
             */
            operatorType?: number;
            /** @description 操作人员 */
            operName?: string;
            /**
             * Format: int64
             * @description 操作用户ID
             */
            userId?: number;
            /**
             * Format: int64
             * @description 操作部门ID
             */
            deptId?: number;
            /** @description 部门名称 */
            deptName?: string;
            /** @description 客户端 */
            clientKey?: string;
            /** @description 设备类型 */
            deviceType?: string;
            /** @description 浏览器类型 */
            browser?: string;
            /** @description 操作系统 */
            os?: string;
            /** @description 请求URL */
            operUrl?: string;
            /** @description 主机地址 */
            operIp?: string;
            /** @description 操作地点 */
            operLocation?: string;
            /** @description 请求参数 */
            operParam?: string;
            /** @description 返回参数 */
            jsonResult?: string;
            /**
             * Format: int32
             * @description 操作状态（0正常 1异常）
             */
            status?: number;
            /** @description 错误消息 */
            errorMsg?: string;
            /**
             * Format: date-time
             * @description 操作时间
             */
            operTime?: string;
            /**
             * Format: int64
             * @description 消耗时间
             */
            costTime?: number;
        };
        /** @description 表格分页数据对象 */
        PageResultSysUserOnline: {
            /**
             * Format: int64
             * @description 总记录数
             */
            total?: number;
            /** @description 列表数据 */
            rows?: components["schemas"]["SysUserOnline"][];
        };
        /** @description 响应信息主体 */
        RPageResultSysUserOnline: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["PageResultSysUserOnline"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 当前在线会话 */
        SysUserOnline: {
            /** @description 会话编号 */
            tokenId?: string;
            /** @description 部门名称 */
            deptName?: string;
            /** @description 用户账号 */
            userName?: string;
            /** @description 客户端 */
            clientKey?: string;
            /** @description 设备类型 */
            deviceType?: string;
            /** @description 登录IP地址 */
            ipaddr?: string;
            /** @description 登录地址 */
            loginLocation?: string;
            /** @description 浏览器类型 */
            browser?: string;
            /** @description 操作系统 */
            os?: string;
            /**
             * Format: int64
             * @description 登录时间
             */
            loginTime?: number;
        };
        /** @description 表格分页数据对象 */
        PageResultSysLoginInfoVo: {
            /**
             * Format: int64
             * @description 总记录数
             */
            total?: number;
            /** @description 列表数据 */
            rows?: components["schemas"]["SysLoginInfoVo"][];
        };
        /** @description 响应信息主体 */
        RPageResultSysLoginInfoVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["PageResultSysLoginInfoVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 系统访问记录视图对象 sys_login_info */
        SysLoginInfoVo: {
            /**
             * Format: int64
             * @description 访问ID
             */
            infoId?: number;
            /** @description 用户账号 */
            userName?: string;
            /** @description 客户端 */
            clientKey?: string;
            /** @description 设备类型 */
            deviceType?: string;
            /** @description 登录状态（0成功 1失败） */
            status?: string;
            /** @description 登录IP地址 */
            ipaddr?: string;
            /** @description 登录地点 */
            loginLocation?: string;
            /** @description 浏览器类型 */
            browser?: string;
            /** @description 操作系统 */
            os?: string;
            /** @description 提示消息 */
            msg?: string;
            /**
             * Format: date-time
             * @description 访问时间
             */
            loginTime?: string;
        };
        /** @description 缓存监控列表信息 */
        CacheListInfoVo: {
            /** @description 信息 */
            info?: {
                [key: string]: string;
            };
            /**
             * Format: int64
             * @description 数据库
             */
            dbSize?: number;
            /** @description 命令统计 */
            commandStats?: {
                [key: string]: string;
            }[];
        };
        /** @description 响应信息主体 */
        RCacheListInfoVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["CacheListInfoVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 响应信息主体 */
        RTestTreeVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["TestTreeVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 测试树表视图对象 test_tree */
        TestTreeVo: {
            /**
             * Format: int64
             * @description 主键
             */
            id?: number;
            /**
             * Format: int64
             * @description 父id
             */
            parentId?: number;
            /**
             * Format: int64
             * @description 部门id
             */
            deptId?: number;
            /**
             * Format: int64
             * @description 用户id
             */
            userId?: number;
            /** @description 树节点名 */
            treeName?: string;
            /**
             * Format: date-time
             * @description 创建时间
             */
            createTime?: string;
        };
        /** @description 响应信息主体 */
        RListTestTreeVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["TestTreeVo"][];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 响应信息主体 */
        RTestSensitive: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["TestSensitive"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 脱敏测试对象。 */
        TestSensitive: {
            /** @description 身份证 */
            idCard?: string;
            /** @description 电话 */
            phone?: string;
            /** @description 地址 */
            address?: string;
            /** @description 邮箱 */
            email?: string;
            /** @description 银行卡 */
            bankCard?: string;
        };
        /** @description 响应信息主体 */
        RMapStringListString: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: {
                [key: string]: string[];
            };
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        BlobResourceContents: components["schemas"]["ResourceContents"] & {
            uri?: string;
            mimeType?: string;
            blob?: string;
            _meta?: {
                [key: string]: unknown;
            };
        };
        /** @description MCP 资源读取结果。 */
        McpResourceReadResult: {
            serverName?: string;
            contents?: (components["schemas"]["BlobResourceContents"] | components["schemas"]["TextResourceContents"])[];
        };
        /** @description 响应信息主体 */
        RMapStringMcpResourceReadResult: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: {
                [key: string]: components["schemas"]["McpResourceReadResult"];
            };
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        ResourceContents: unknown;
        TextResourceContents: components["schemas"]["ResourceContents"] & {
            uri?: string;
            mimeType?: string;
            text?: string;
            _meta?: {
                [key: string]: unknown;
            };
        };
        Annotations: {
            audience?: ("user" | "assistant")[];
            /** Format: double */
            priority?: number;
            lastModified?: string;
        };
        AudioContent: {
            type: "AudioContent";
        } & (Omit<components["schemas"]["Content"], "type"> & {
            annotations?: components["schemas"]["Annotations"];
            data?: string;
            mimeType?: string;
            _meta?: {
                [key: string]: unknown;
            };
        });
        Content: {
            type: string;
        };
        EmbeddedResource: {
            type: "EmbeddedResource";
        } & (Omit<components["schemas"]["Content"], "type"> & {
            annotations?: components["schemas"]["Annotations"];
            resource?: components["schemas"]["BlobResourceContents"] | components["schemas"]["TextResourceContents"];
            _meta?: {
                [key: string]: unknown;
            };
        });
        ImageContent: {
            type: "ImageContent";
        } & (Omit<components["schemas"]["Content"], "type"> & {
            annotations?: components["schemas"]["Annotations"];
            data?: string;
            mimeType?: string;
            _meta?: {
                [key: string]: unknown;
            };
        });
        /** @description MCP 数据处理结果。 */
        McpDemoHandleResult: {
            /** @description 数据来源类型 */
            sourceType?: string;
            /** @description 是否已处理 */
            handled?: boolean;
            /** @description MCP 原始返回数据 */
            data?: {
                [key: string]: components["schemas"]["McpToolCallResult"];
            };
        };
        /** @description MCP 工具调用结果。 */
        McpToolCallResult: {
            serverName?: string;
            error?: boolean;
            content?: (components["schemas"]["AudioContent"] | components["schemas"]["EmbeddedResource"] | components["schemas"]["ImageContent"] | components["schemas"]["ResourceLink"] | components["schemas"]["TextContent"])[];
            structuredContent?: unknown;
        };
        /** @description 响应信息主体 */
        RMcpDemoHandleResult: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["McpDemoHandleResult"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        ResourceLink: {
            type: "ResourceLink";
        } & (Omit<components["schemas"]["Content"], "type"> & {
            name?: string;
            title?: string;
            uri?: string;
            description?: string;
            mimeType?: string;
            /** Format: int64 */
            size?: number;
            annotations?: components["schemas"]["Annotations"];
            _meta?: {
                [key: string]: unknown;
            };
        });
        TextContent: {
            type: "TextContent";
        } & (Omit<components["schemas"]["Content"], "type"> & {
            annotations?: components["schemas"]["Annotations"];
            text?: string;
            _meta?: {
                [key: string]: unknown;
            };
        });
        /** @description 国际化 Bean 校验测试对象。 */
        TestI18nBo: {
            /** @description 名称。 */
            name: string;
            /**
             * Format: int32
             * @description 年龄。
             */
            age: number;
        };
        /** @description 响应信息主体 */
        RTestI18nBo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["TestI18nBo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 响应信息主体 */
        RMapStringTestDemoEncrypt: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: {
                [key: string]: components["schemas"]["TestDemoEncrypt"];
            };
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 测试加密字段实体。 */
        TestDemoEncrypt: {
            /**
             * Format: int64
             * @description 创建部门
             */
            createDept?: number;
            /**
             * Format: int64
             * @description 创建者
             */
            createBy?: number;
            /**
             * Format: date-time
             * @description 创建时间
             */
            createTime?: string;
            /**
             * Format: int64
             * @description 更新者
             */
            updateBy?: number;
            /**
             * Format: date-time
             * @description 更新时间
             */
            updateTime?: string;
            /**
             * Format: int64
             * @description 主键
             */
            id?: number;
            /**
             * Format: int64
             * @description 部门id
             */
            deptId?: number;
            /**
             * Format: int64
             * @description 用户id
             */
            userId?: number;
            /**
             * Format: int32
             * @description 排序号
             */
            orderNum?: number;
            /** @description key键 */
            testKey?: string;
            /** @description 值 */
            value?: string;
            /**
             * Format: int64
             * @description 版本
             */
            version?: number;
            /**
             * Format: int64
             * @description 删除标志
             */
            delFlag?: number;
        };
        /** @description 响应信息主体 */
        RTestDemoVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["TestDemoVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 测试单表视图对象 test_demo */
        TestDemoVo: {
            /**
             * Format: int64
             * @description 主键
             */
            id?: number;
            /**
             * Format: int64
             * @description 部门id
             */
            deptId?: number;
            /**
             * Format: int64
             * @description 用户id
             */
            userId?: number;
            /**
             * Format: int32
             * @description 排序号
             */
            orderNum?: number;
            /** @description key键 */
            testKey?: string;
            /** @description 值 */
            value?: string;
            /**
             * Format: date-time
             * @description 创建时间
             */
            createTime?: string;
            /**
             * Format: int64
             * @description 创建人
             */
            createBy?: number;
            /** @description 创建人账号 */
            createByName?: string;
            /**
             * Format: date-time
             * @description 更新时间
             */
            updateTime?: string;
            /**
             * Format: int64
             * @description 更新人
             */
            updateBy?: number;
            /** @description 更新人账号 */
            updateByName?: string;
            /**
             * Format: int64
             * @description 版本
             */
            version?: number;
        };
        /** @description 表格分页数据对象 */
        PageResultTestDemoVo: {
            /**
             * Format: int64
             * @description 总记录数
             */
            total?: number;
            /** @description 列表数据 */
            rows?: components["schemas"]["TestDemoVo"][];
        };
        /** @description 响应信息主体 */
        RPageResultTestDemoVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["PageResultTestDemoVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 图片验证码响应对象。 */
        CaptchaVo: {
            /** @description 是否启用验证码 */
            captchaEnabled?: boolean;
            /** @description 验证码标识 */
            uuid?: string;
            /** @description Base64 图片数据 */
            img?: string;
        };
        /** @description 响应信息主体 */
        RCaptchaVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["CaptchaVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        /** @description 客户端公开认证上下文，仅返回前端展示注册入口所需字段。 */
        AuthClientContextVo: {
            /** @description 客户端是否可用 */
            clientEnabled?: boolean;
            /** @description 是否开放公开注册 */
            registerEnabled?: boolean;
            /** @description 当前可用客户端的非敏感密码规则。 */
            passwordPolicy?: components["schemas"]["PasswordPolicyProjection"];
        };
        /** @description 可向未认证客户端公开的密码规则。 */
        PasswordPolicyProjection: {
            /** Format: int32 */
            minimumLength?: number;
            /** Format: int32 */
            maximumLength?: number;
            requiredCharacterClasses?: ("UPPERCASE" | "LOWERCASE" | "DIGIT" | "SPECIAL")[];
            allowedSpecialCharacters?: string;
        };
        /** @description 响应信息主体 */
        RAuthClientContextVo: {
            /**
             * Format: int32
             * @description 响应状态码
             */
            code?: number;
            /** @description 响应提示信息 */
            msg?: string;
            /** @description 响应业务数据 */
            data?: components["schemas"]["AuthClientContextVo"];
            /** @description 机器可读的错误合同，旧客户端可继续使用 code/msg/data。 */
            error?: components["schemas"]["ErrorInfo"];
        };
        OpenApiAgentToolVO: {
            /** Format: int64 */
            id?: number;
            name?: string;
            description?: string;
        };
        OpenApiAgentVO: {
            /** Format: int64 */
            id?: number;
            name?: string;
            description?: string;
            avatar?: string;
            greeting?: string;
            presetQuestions?: string[];
            capabilities?: string[];
            mcpEnabled?: boolean;
            mcpServers?: components["schemas"]["OpenApiAgentToolVO"][];
            skillEnabled?: boolean;
            skills?: components["schemas"]["OpenApiAgentToolVO"][];
            webSearchEnabled?: boolean;
            /** Format: int32 */
            viewCount?: number;
            isFeatured?: boolean;
            /** Format: int32 */
            status?: number;
            /** Format: date-time */
            createDt?: string;
            /** Format: date-time */
            updateDt?: string;
            subscribed?: boolean;
        };
        ResultListOpenApiAgentVO: {
            /** Format: int32 */
            status?: number;
            message?: string;
            data?: components["schemas"]["OpenApiAgentVO"][];
        };
        PageResultListOpenApiConversationVO: {
            /** Format: int32 */
            status?: number;
            message?: string;
            data?: components["schemas"]["OpenApiConversationVO"][];
            /** Format: int64 */
            page?: number;
            /** Format: int64 */
            size?: number;
            /** Format: int64 */
            total?: number;
        };
        OpenApiMessageVO: {
            role?: string;
            content?: string;
            thinking?: string;
            /** Format: int32 */
            status?: number;
            createDt?: string;
        };
        ResultListOpenApiMessageVO: {
            /** Format: int32 */
            status?: number;
            message?: string;
            data?: components["schemas"]["OpenApiMessageVO"][];
        };
        ResultMapStringObject: {
            /** Format: int32 */
            status?: number;
            message?: string;
            data?: {
                [key: string]: unknown;
            };
        };
        OpenApiAgentQueryRequest: {
            /** Format: int32 */
            page?: number;
            /** Format: int32 */
            size?: number;
            appId?: string;
        };
        PageResultListOpenApiAgentVO: {
            /** Format: int32 */
            status?: number;
            message?: string;
            data?: components["schemas"]["OpenApiAgentVO"][];
            /** Format: int64 */
            page?: number;
            /** Format: int64 */
            size?: number;
            /** Format: int64 */
            total?: number;
        };
        OpenApiAgentIdentityRequest: {
            /** Format: int64 */
            agentId: number;
        };
        ResultOpenApiAgentVO: {
            /** Format: int32 */
            status?: number;
            message?: string;
            data?: components["schemas"]["OpenApiAgentVO"];
        };
    };
    responses: never;
    parameters: never;
    requestBodies: never;
    headers: never;
    pathItems: never;
}
export type $defs = Record<string, never>;
export interface operations {
    updateAssignee: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 办理人id */
                userId: string;
            };
            cookie?: never;
        };
        /** @description 任务id */
        requestBody: {
            content: {
                "application/json": number[];
            };
        };
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    edit: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 表达式信息 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["FlowSpelBo"];
            };
        };
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    add: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 表达式信息 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["FlowSpelBo"];
            };
        };
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    edit_1: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 请假信息 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["TestLeaveBo"];
            };
        };
        responses: {
            /** @description 修改后的请假单 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RTestLeaveVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    add_1: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 请假信息 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["TestLeaveBo"];
            };
        };
        responses: {
            /** @description 新增后的请假单 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RTestLeaveVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    updateVariable: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 参数 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["FlowVariableBo"];
            };
        };
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    cancelProcessApply: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 参数 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["FlowCancelBo"];
            };
        };
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    active: {
        parameters: {
            query: {
                /** @description 激活/挂起 */
                active: boolean;
            };
            header?: never;
            path: {
                /** @description 流程实例id */
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 处理结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RBoolean"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    edit_2: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 流程定义信息 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["FlowDefinition"];
            };
        };
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RBoolean"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    add_2: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 流程定义信息 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["FlowDefinition"];
            };
        };
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RBoolean"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    unPublish: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 流程定义id */
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RBoolean"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    publish: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 流程定义id */
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 发布结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RBoolean"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    active_1: {
        parameters: {
            query: {
                /** @description 激活/挂起 */
                active: boolean;
            };
            header?: never;
            path: {
                /** @description 流程定义id */
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 处理结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RBoolean"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    edit_3: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 分类信息 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["FlowCategoryBo"];
            };
        };
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    add_3: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 分类信息 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["FlowCategoryBo"];
            };
        };
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    edit_4: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 用户编辑参数 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["SysUserBo"];
            };
        };
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    add_4: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 用户新增参数 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["SysUserBo"];
            };
        };
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    edit_5: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 登录域信息 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["SysUserTypeBo"];
            };
        };
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    add_5: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 登录域信息 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["SysUserTypeBo"];
            };
        };
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    changeStatus: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 登录域状态信息 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["SysUserTypeBo"];
            };
        };
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    resetPwd: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 用户参数 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["SysUserBo"];
            };
        };
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    profile: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 用户信息、角色组和岗位组 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RProfileVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    updateProfile: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 个人资料参数 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["SysUserProfileBo"];
            };
        };
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    updatePwd: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 新旧密码 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["SysUserPasswordBo"];
            };
        };
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    changeStatus_1: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 用户参数 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["SysUserBo"];
            };
        };
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    insertAuthRole: {
        parameters: {
            query: {
                /** @description 用户Id */
                userId: number;
                /** @description 角色ID串，空表示撤销当前客户端显式角色 */
                roleIds: number[];
                /** @description 客户端主键 */
                clientId: number;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    edit_6: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 角色参数 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["SysRoleBo"];
            };
        };
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    add_6: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 角色参数 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["SysRoleBo"];
            };
        };
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    editPermission: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 角色参数 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["SysRoleBo"];
            };
        };
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    changeStatus_2: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 角色参数 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["SysRoleBo"];
            };
        };
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    selectAuthUserAll: {
        parameters: {
            query: {
                /** @description 角色ID */
                roleId: number;
                /** @description 用户ID串 */
                userIds: number[];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    cancelAuthUser: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 用户角色关系 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["SysUserRole"];
            };
        };
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    cancelAuthUserAll: {
        parameters: {
            query: {
                /** @description 角色ID */
                roleId: number;
                /** @description 用户ID串 */
                userIds: number[];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    edit_7: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 岗位参数 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["SysPostBo"];
            };
        };
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    add_7: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 岗位参数 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["SysPostBo"];
            };
        };
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    edit_8: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 菜单参数 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["SysMenuBo"];
            };
        };
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    add_8: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 菜单参数 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["SysMenuBo"];
            };
        };
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    edit_9: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 字典类型参数 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["SysDictTypeBo"];
            };
        };
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    add_9: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 字典类型参数 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["SysDictTypeBo"];
            };
        };
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    edit_10: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 字典数据参数 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["SysDictDataBo"];
            };
        };
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    add_10: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 字典数据参数 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["SysDictDataBo"];
            };
        };
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    edit_11: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 部门参数 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["SysDeptBo"];
            };
        };
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    add_11: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 部门参数 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["SysDeptBo"];
            };
        };
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    edit_12: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 参数配置 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["SysConfigBo"];
            };
        };
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    add_12: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 参数配置 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["SysConfigBo"];
            };
        };
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    updateByKey: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 参数配置 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["SysConfigBo"];
            };
        };
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    edit_13: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 客户端信息 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["SysClientBo"];
            };
        };
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    add_13: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 客户端信息 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["SysClientBo"];
            };
        };
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    changeStatus_3: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 客户端状态信息 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["SysClientBo"];
            };
        };
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    edit_14: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["TestTreeBo"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    add_14: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["TestTreeBo"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    edit_15: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["TestDemoBo"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    add_15: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["TestDemoBo"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    listConversations: {
        parameters: {
            query: {
                agentId: number;
                page?: number;
                size?: number;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["PageResultListOpenApiConversationVO"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    updateConversationTitle: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["OpenApiConversationUpdateTitleRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ResultVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    createConversation: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["OpenApiCreateConversationRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ResultOpenApiConversationVO"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    deleteConversation: {
        parameters: {
            query: {
                agentId: number;
                conversationId: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ResultVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    urgeTask: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 参数 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["FlowUrgeTaskBo"];
            };
        };
        responses: {
            /** @description 结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    terminationTask: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 参数 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["FlowTerminationBo"];
            };
        };
        responses: {
            /** @description 处理结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RBoolean"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    taskOperation: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 操作类型，委派 delegateTask、转办 transferTask、加签 addSignature、减签 reductionSignature */
                taskOperation: string;
            };
            cookie?: never;
        };
        /** @description 参数 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["TaskOperationBo"];
            };
        };
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    startWorkFlow: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 启动流程参数 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["StartProcessBo"];
            };
        };
        responses: {
            /** @description 启动结果及后续流程信息 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RStartProcessReturnDTO"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    getNextNodeList: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 参数 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["FlowNextNodeBo"];
            };
        };
        responses: {
            /** @description 下一节点列表 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RListFlowNode"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    completeTask: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 办理任务参数 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["CompleteTaskBo"];
            };
        };
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    backProcess: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 参数 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["BackProcessBo"];
            };
        };
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    submitAndFlowStart: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 请假信息 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["TestLeaveBo"];
            };
        };
        responses: {
            /** @description 提交后的请假单 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RTestLeaveVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    export: {
        parameters: {
            query: {
                /** @description 查询条件 */
                bo: components["schemas"]["TestLeaveBo"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    invalid: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 参数 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["FlowInvalidBo"];
            };
        };
        responses: {
            /** @description 处理结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RBoolean"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    importDef: {
        parameters: {
            query: {
                /** @description 文件 */
                file: string;
                /** @description 分类 */
                category: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 导入结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RBoolean"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    exportDef: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 流程定义id */
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
            /** @description Internal Server Error */
            500: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    copy: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 流程定义id */
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 复制结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RBoolean"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    export_1: {
        parameters: {
            query: {
                /** @description 查询条件 */
                bo: components["schemas"]["FlowCategoryBo"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    saveJson: {
        parameters: {
            query?: never;
            header: {
                onlyNodeSkip: boolean;
            };
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["DefJson"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResultVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
            /** @description Internal Server Error */
            500: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    saveFormContent: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["FlowDto"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResultVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    handle: {
        parameters: {
            query: {
                taskId: number;
                skipType: string;
                message: string;
                nodeCode?: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": {
                    [key: string]: unknown;
                };
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResultInstance"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    add_16: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ThirdProviderBo"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    status: {
        parameters: {
            query: {
                status: string;
            };
            header?: never;
            path: {
                providerId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    remove: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                providerId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    save: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ThirdProviderBo"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    add_17: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ThirdEndpointBo"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    status_1: {
        parameters: {
            query: {
                status: string;
            };
            header?: never;
            path: {
                endpointId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    remove_1: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                endpointId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    save_1: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ThirdEndpointBo"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    save_2: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ThirdCredentialBo"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    remove_2: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                credentialId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    export_2: {
        parameters: {
            query: {
                /** @description 查询条件 */
                bo: components["schemas"]["SysUserTypeBo"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    issue: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["TemporaryPasswordIssueBo"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RTemporaryPasswordVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    candidate: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 目标用户 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["ResetPasswordCandidateBo"];
            };
        };
        responses: {
            /** @description 合规密码候选 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RResetPasswordCandidateVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    importTemplate: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    importData: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: {
            content: {
                "multipart/form-data": {
                    /**
                     * Format: binary
                     * @description 导入文件
                     */
                    file: string;
                    /** @description 是否更新已存在数据 */
                    updateSupport?: boolean;
                };
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
            /** @description Internal Server Error */
            500: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    export_3: {
        parameters: {
            query: {
                /** @description 用户查询条件 */
                user: components["schemas"]["SysUserBo"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    export_4: {
        parameters: {
            query: {
                /** @description 查询条件 */
                role: components["schemas"]["SysRoleBo"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    export_5: {
        parameters: {
            query: {
                /** @description 查询条件 */
                post: components["schemas"]["SysPostBo"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    resetUser: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                userId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ROpenApiCredentialIssued"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    enableUser: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                userId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ROpenApiCredentialSummary"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    disableUser: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                userId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ROpenApiCredentialSummary"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    deleteUser: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                userId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    createUser: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                userId: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["OpenApiCredentialCreateRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ROpenApiCredentialIssued"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    resetSelf: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ROpenApiCredentialIssued"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    enableSelf: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ROpenApiCredentialSummary"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    disableSelf: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ROpenApiCredentialSummary"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    deleteSelf: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    createSelf: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["OpenApiCredentialCreateRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ROpenApiCredentialIssued"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    export_6: {
        parameters: {
            query: {
                /** @description 查询条件 */
                dictType: components["schemas"]["SysDictTypeBo"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    export_7: {
        parameters: {
            query: {
                /** @description 查询条件 */
                dictData: components["schemas"]["SysDictDataBo"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    export_8: {
        parameters: {
            query: {
                /** @description 查询条件 */
                config: components["schemas"]["SysConfigBo"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    export_9: {
        parameters: {
            query: {
                /** @description 查询条件 */
                bo: components["schemas"]["SysClientBo"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    upload: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: {
            content: {
                "multipart/form-data": {
                    /**
                     * Format: binary
                     * @description 导入文件
                     */
                    file: string;
                };
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RString"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    registerCurrentUser: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ROpenApiUserVO"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    init: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["InitRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RInitResponse"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    signParts: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                uploadToken: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["SignPartsRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RSignPartsResponse"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    complete: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                uploadToken: string;
            };
            cookie?: never;
        };
        requestBody?: {
            content: {
                "application/json": components["schemas"]["CompleteRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RString"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    rollback: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                batchId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    retry: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                batchId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    cleanup: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                batchId: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CleanupRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    start: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["MigrationRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RLong"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    dryRun: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["MigrationRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RDryRunReport"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    add_18: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 配置信息 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["SysOssConfigBo"];
            };
        };
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    remove_3: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description OSS配置ID串 */
                ossConfigIds: number[];
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    edit_16: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 配置信息 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["SysOssConfigBo"];
            };
        };
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    changeStatus_4: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 状态变更信息 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["SysOssConfigBo"];
            };
        };
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    callback: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                providerCode: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CallbackRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPersonVerificationCallbackOutcome"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    unbind: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPersonRebindUnbindVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    submit: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["PersonRebindSubmitBo"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPersonRebindSubmissionVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    probe: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["PersonRebindProbeBo"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPersonRebindProbeVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    match: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["PersonRebindMatchBo"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPersonRebindMatchVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    confirm: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["PersonRebindConfirmBo"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPersonRebindConfirmationVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    list: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                ownerType: "WORKING" | "SUBMISSION" | "SOURCE" | "VERSION";
                ownerId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RListMaterialReferenceView"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    attach: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                ownerType: "WORKING" | "SUBMISSION" | "SOURCE" | "VERSION";
                ownerId: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["AttachRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RMaterialReferenceView"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    detach: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                ownerType: "WORKING" | "SUBMISSION" | "SOURCE" | "VERSION";
                ownerId: number;
                materialRefId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    revoke: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                profileId: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["PersonAdminRevokeBo"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPersonAdminResultVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    revise: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                profileId: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["PersonAdminReviseBo"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPersonAdminResultVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    manageBinding: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                profileId: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["PersonAdminBindingBo"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPersonAdminResultVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    assign: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                profileId: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["PersonAdminAssignBo"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPersonAdminResultVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    decide: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                applicationId: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["PersonAdminDecisionBo"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPersonAdminResultVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    create: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["PersonAdminCreateBo"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPersonAdminResultVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    current: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPersonApplicationVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    save_3: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["PersonApplicationSaveBo"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPersonApplicationVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    submit_1: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["PersonApplicationSubmitBo"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPersonApplicationVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    create_1: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["MaterialNodeCommand"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RMaterialNodeView"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    update: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                materialNodeId: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["MaterialNodeCommand"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RMaterialNodeView"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    status_2: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                materialNodeId: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["StatusCommand"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    archive: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                materialNodeId: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["VersionCommand"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    callback_1: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                providerCode: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CallbackRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["REnterpriseVerificationCallbackOutcome"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    unbind_1: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["REnterpriseTransferVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    send: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["EnterpriseTransferSendBo"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["REnterpriseTransferVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    confirm_1: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["EnterpriseTransferConfirmBo"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["REnterpriseTransferVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    list_1: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                ownerType: "WORKING" | "SUBMISSION" | "SOURCE" | "VERSION";
                ownerId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RListMaterialReferenceView"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    attach_1: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                ownerType: "WORKING" | "SUBMISSION" | "SOURCE" | "VERSION";
                ownerId: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["AttachRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RMaterialReferenceView"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    detach_1: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                ownerType: "WORKING" | "SUBMISSION" | "SOURCE" | "VERSION";
                ownerId: number;
                materialRefId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    revoke_1: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                profileId: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["EnterpriseAdminRevokeBo"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["REnterpriseAdminResultVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    revise_1: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                profileId: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["EnterpriseAdminReviseBo"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["REnterpriseAdminResultVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    manageBinding_1: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                profileId: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["EnterpriseAdminBindingBo"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["REnterpriseAdminResultVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    assign_1: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                profileId: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["EnterpriseAdminAssignBo"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["REnterpriseAdminResultVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    decide_1: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                applicationId: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["EnterpriseAdminDecisionBo"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["REnterpriseAdminResultVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    create_2: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["EnterpriseAdminCreateBo"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["REnterpriseAdminResultVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    current_1: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["REnterpriseApplicationVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    save_4: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["EnterpriseApplicationSaveBo"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["REnterpriseApplicationVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    submit_2: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["EnterpriseApplicationSubmitBo"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["REnterpriseApplicationVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    probe_1: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["EnterpriseApplicationProbeBo"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["REnterpriseApplicationProbeVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    submit_3: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["NotificationCommand"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RNotificationReceipt"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    retry_1: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                notificationId: string;
            };
            cookie?: never;
        };
        requestBody?: {
            content: {
                "application/json": components["schemas"]["NotificationRetryCommand"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RRetryReceipt"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    cancel: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                notificationId: string;
            };
            cookie?: never;
        };
        requestBody?: {
            content: {
                "application/json": components["schemas"]["NotificationCancelCommand"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RCancelReceipt"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    retract: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                noticeId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    publish_1: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                noticeId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    save_5: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["NotifyNoticeBo"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    remove_4: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": number[];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    seen: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                messageId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    read: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                messageId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    readAll: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    callback_2: {
        parameters: {
            query?: never;
            header: {
                /** @description HMAC-SHA256 签名 */
                "X-Notify-Signature": string;
            };
            path: {
                /** @description 渠道 */
                channel: string;
            };
            cookie?: never;
        };
        /** @description 原始 JSON 回调数据，必须包含 providerMessageId 和 status */
        requestBody: {
            content: {
                "application/json": string;
            };
        };
        responses: {
            /** @description 处理结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    export_10: {
        parameters: {
            query: {
                /** @description 查询条件 */
                operLog: components["schemas"]["SysOperLogBo"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    export_11: {
        parameters: {
            query: {
                /** @description 查询条件 */
                loginInfo: components["schemas"]["SysLoginInfoBo"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    importWithOptions: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: {
            content: {
                "multipart/form-data": {
                    /**
                     * Format: binary
                     * @description 导入文件
                     */
                    file: string;
                };
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ExportDemoVo"][];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
            /** @description Internal Server Error */
            500: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    importData_1: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: {
            content: {
                "multipart/form-data": {
                    /**
                     * Format: binary
                     * @description 导入文件
                     */
                    file: string;
                };
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
            /** @description Internal Server Error */
            500: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    export_12: {
        parameters: {
            query: {
                bo: components["schemas"]["TestDemoBo"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    add_19: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    addOrUpdate: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    socialCallback: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 请求体 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["SocialLoginBody"];
            };
        };
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    register: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 注册信息 */
        requestBody: {
            content: {
                "application/json": components["schemas"]["RegisterBody"];
            };
        };
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    logout: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    login: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** @description 登录信息 */
        requestBody: {
            content: {
                "application/json": string;
            };
        };
        responses: {
            /** @description 结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RLoginVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    session: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["OpenApiEmbedTokenRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ResultOpenApiEmbedTokenResponse"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    uploadResource: {
        parameters: {
            query?: {
                bizType?: string;
                bizId?: number;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: {
            content: {
                "multipart/form-data": {
                    /** Format: binary */
                    file: string;
                };
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ResultOpenApiResourceResponse"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    completions: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["OpenApiChatRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "text/event-stream": components["schemas"]["ServerSentEventString"][];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    subscribeAgent: {
        parameters: {
            query: {
                agentId: number;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ResultVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    unsubscribeAgent: {
        parameters: {
            query: {
                agentId: number;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ResultVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    pageByTaskWait: {
        parameters: {
            query: {
                /** @description 参数 */
                flowTaskBo: components["schemas"]["FlowTaskBo"];
                /** @description 分页 */
                pageQuery: components["schemas"]["PageQuery"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 待办任务分页数据 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPageResultFlowTaskVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    pageByTaskFinish: {
        parameters: {
            query: {
                /** @description 参数 */
                flowTaskBo: components["schemas"]["FlowTaskBo"];
                /** @description 分页 */
                pageQuery: components["schemas"]["PageQuery"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 已办任务分页数据 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPageResultFlowHisTaskVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    pageByTaskCopy: {
        parameters: {
            query: {
                /** @description 参数 */
                flowTaskBo: components["schemas"]["FlowTaskBo"];
                /** @description 分页 */
                pageQuery: components["schemas"]["PageQuery"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 抄送任务分页数据 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPageResultFlowTaskVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    pageByAllTaskWait: {
        parameters: {
            query: {
                /** @description 参数 */
                flowTaskBo: components["schemas"]["FlowTaskBo"];
                /** @description 分页 */
                pageQuery: components["schemas"]["PageQuery"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 待办任务分页数据 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPageResultFlowTaskVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    pageByAllTaskFinish: {
        parameters: {
            query: {
                /** @description 参数 */
                flowTaskBo: components["schemas"]["FlowTaskBo"];
                /** @description 分页 */
                pageQuery: components["schemas"]["PageQuery"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 已办任务分页数据 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPageResultFlowHisTaskVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    getTask: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 任务id */
                taskId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 任务详情 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RFlowTaskVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    getBackTaskNode: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 任务id */
                taskId: number;
                /** @description 当前节点 */
                nowNodeCode: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 可驳回节点列表 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RListNode"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    currentTaskAllUser: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 任务id */
                taskId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 办理人列表 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RListUserDTO"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    getInfo: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 主键 */
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 表达式详情 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RFlowSpelVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    list_2: {
        parameters: {
            query: {
                /** @description 查询条件 */
                bo: components["schemas"]["FlowSpelBo"];
                /** @description 分页参数 */
                pageQuery: components["schemas"]["PageQuery"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 表达式分页数据 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPageResultFlowSpelVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    getInfo_1: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 主键 */
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 请假详情 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RTestLeaveVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    list_3: {
        parameters: {
            query: {
                /** @description 查询条件 */
                bo: components["schemas"]["TestLeaveBo"];
                /** @description 分页参数 */
                pageQuery: components["schemas"]["PageQuery"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 请假分页数据 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPageResultTestLeaveVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    selectRunningInstanceList: {
        parameters: {
            query: {
                /** @description 流程实例 */
                flowInstanceBo: components["schemas"]["FlowInstanceBo"];
                /** @description 分页 */
                pageQuery: components["schemas"]["PageQuery"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 正在运行的流程实例分页数据 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPageResultFlowInstanceVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    selectFinishInstanceList: {
        parameters: {
            query: {
                /** @description 流程实例 */
                flowInstanceBo: components["schemas"]["FlowInstanceBo"];
                /** @description 分页 */
                pageQuery: components["schemas"]["PageQuery"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 已结束的流程实例分页数据 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPageResultFlowInstanceVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    selectCurrentInstanceList: {
        parameters: {
            query: {
                /** @description 参数 */
                flowInstanceBo: components["schemas"]["FlowInstanceBo"];
                /** @description 分页 */
                pageQuery: components["schemas"]["PageQuery"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 当前用户发起的流程实例分页数据 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPageResultFlowInstanceVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    instanceVariable: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 流程实例id */
                instanceId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 流程变量 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RMapStringObject"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    getInfo_2: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 业务id */
                businessId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 流程实例详情 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RFlowInstanceVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    flowHisTaskList: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 业务id */
                businessId: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 流程图与历史节点信息 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RMapStringObject"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    getInfo_3: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 流程定义id */
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 流程定义详情 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RDefinition"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    xmlString: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 流程定义id */
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 流程定义 JSON */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RString"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    unPublishList: {
        parameters: {
            query: {
                /** @description 查询条件 */
                flowDefinition: components["schemas"]["FlowDefinition"];
                /** @description 分页参数 */
                pageQuery: components["schemas"]["PageQuery"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 未发布流程定义分页数据 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPageResultFlowDefinitionVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    list_4: {
        parameters: {
            query: {
                /** @description 查询条件 */
                flowDefinition: components["schemas"]["FlowDefinition"];
                /** @description 分页参数 */
                pageQuery: components["schemas"]["PageQuery"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 流程定义分页数据 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPageResultFlowDefinitionVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    getInfo_4: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 主键 */
                categoryId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 流程分类详情 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RFlowCategoryVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    remove_5: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 主键 */
                categoryId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    list_5: {
        parameters: {
            query: {
                /** @description 查询条件 */
                bo: components["schemas"]["FlowCategoryBo"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 流程分类列表 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RListFlowCategoryVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    categoryTree: {
        parameters: {
            query: {
                /** @description 流程分类 */
                categoryBo: components["schemas"]["FlowCategoryBo"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 流程分类树 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RListTreeString"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    queryFlowChart: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResultDefJson"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    queryDef: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResultDefJson"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    queryDef_1: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResultDefJson"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    publishedForm: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResultListForm"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    nodeExt: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResultListNodeExt"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    listenerList: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResultListListenerVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    handlerType: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResultListString"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    handlerResult: {
        parameters: {
            query: {
                arg0: components["schemas"]["HandlerQuery"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResultHandlerSelectVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    handlerFeedback: {
        parameters: {
            query: {
                arg0: components["schemas"]["HandlerFeedBackDto"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResultListHandlerFeedBackVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    handlerDict: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResultListDict"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    getFormContent: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResultString"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    load: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                taskId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResultFlowDto"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    hisLoad: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                taskId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResultFlowDto"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    config: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ApiResultWarmFlowVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    statistics: {
        parameters: {
            query: {
                providerCode: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RObject"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    get: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                providerId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RThirdProviderVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    list_6: {
        parameters: {
            query?: {
                keyword?: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RListThirdProviderVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    invocations: {
        parameters: {
            query: {
                providerCode: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RObject"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    get_1: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                endpointId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RThirdEndpointVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    list_7: {
        parameters: {
            query?: {
                providerId?: number;
                keyword?: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RListThirdEndpointVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    list_8: {
        parameters: {
            query: {
                providerCode: string;
                endpointCode?: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RObject"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    getInfo_5: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 主键 */
                userTypeId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 登录域详情 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RSysUserTypeVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    listByUser: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 用户ID */
                userId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 登录域关系列表 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RListSysUserTypeRelVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    options: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 登录域列表 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RListSysUserTypeVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    list_9: {
        parameters: {
            query: {
                /** @description 查询条件 */
                bo: components["schemas"]["SysUserTypeBo"];
                /** @description 分页参数 */
                pageQuery: components["schemas"]["PageQuery"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 登录域分页数据 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPageResultSysUserTypeVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    unlock: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 用户ID */
                userId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    optionselect: {
        parameters: {
            query?: {
                /** @description 用户ID串 */
                userIds?: number[];
                /** @description 部门ID */
                deptId?: number;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 用户基础信息列表 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RListSysUserVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    list_10: {
        parameters: {
            query: {
                /** @description 用户查询条件 */
                user: components["schemas"]["SysUserBo"];
                /** @description 分页参数 */
                pageQuery: components["schemas"]["PageQuery"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 用户分页列表 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPageResultSysUserVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    listByDept: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 部门ID */
                deptId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 用户列表 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RListSysUserVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    getInfo_6: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 当前登录用户信息、角色与权限集合 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RUserInfoVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    deptTree: {
        parameters: {
            query: {
                /** @description 部门查询条件 */
                dept: components["schemas"]["SysDeptBo"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 部门树列表 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RListTreeString"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    authRole: {
        parameters: {
            query: {
                clientId: number;
            };
            header?: never;
            path: {
                /** @description 用户ID */
                userId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 用户及其可授权角色信息 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RSysUserInfoVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    getInfo_7: {
        parameters: {
            query?: {
                clientId?: number;
            };
            header?: never;
            path: {
                /** @description 用户ID */
                userId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 用户详情、角色与岗位信息 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RSysUserInfoVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    getInfo_8: {
        parameters: {
            query?: {
                clientId?: number;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 用户详情、角色与岗位信息 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RSysUserInfoVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    list_11: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 绑定关系列表 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RListSysSocialVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    getInfo_9: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 角色ID */
                roleId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 角色详情 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RSysRoleVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    optionselect_1: {
        parameters: {
            query?: {
                /** @description 角色ID串 */
                roleIds?: number[];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 角色列表 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RListSysRoleVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    list_12: {
        parameters: {
            query: {
                /** @description 查询条件 */
                role: components["schemas"]["SysRoleBo"];
                /** @description 分页参数 */
                pageQuery: components["schemas"]["PageQuery"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 角色分页结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPageResultSysRoleVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    roleDeptTreeselect: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 角色ID */
                roleId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 角色部门树信息 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RDeptTreeSelectVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    unallocatedList: {
        parameters: {
            query: {
                /** @description 查询条件 */
                user: components["schemas"]["SysUserBo"];
                /** @description 分页参数 */
                pageQuery: components["schemas"]["PageQuery"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 用户分页结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPageResultSysUserVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    allocatedList: {
        parameters: {
            query: {
                /** @description 查询条件 */
                user: components["schemas"]["SysUserBo"];
                /** @description 分页参数 */
                pageQuery: components["schemas"]["PageQuery"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 用户分页结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPageResultSysUserVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    getInfo_10: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 岗位ID */
                postId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 岗位详情 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RSysPostVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    optionselect_2: {
        parameters: {
            query?: {
                /** @description 岗位ID串 */
                postIds?: number[];
                /** @description 部门id */
                deptId?: number;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 岗位列表 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RListSysPostVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    list_13: {
        parameters: {
            query: {
                /** @description 查询条件 */
                post: components["schemas"]["SysPostBo"];
                /** @description 分页参数 */
                pageQuery: components["schemas"]["PageQuery"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 岗位分页结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPageResultSysPostVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    deptTree_1: {
        parameters: {
            query: {
                /** @description 部门查询条件 */
                dept: components["schemas"]["SysDeptBo"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 部门树列表 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RListTreeString"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    users: {
        parameters: {
            query?: {
                keyword?: string;
                limit?: number;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RListOpenApiCredentialUserSummary"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    userInterfaces: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                userId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RListOpenApiCatalogItem"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    userInterface: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                userId: number;
                interfaceId: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ROpenApiCatalogItem"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    getUserCredential: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                userId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ROpenApiCredentialSummary"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    selfInterfaces: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RListOpenApiCatalogItem"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    selfInterface: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                interfaceId: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ROpenApiCatalogItem"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    getSelfCredential: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ROpenApiCredentialSummary"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    getInfo_11: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 菜单ID */
                menuId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 菜单详情 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RSysMenuVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    remove_6: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 菜单ID */
                menuId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    treeselect: {
        parameters: {
            query: {
                /** @description 查询条件 */
                menu: components["schemas"]["SysMenuBo"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 菜单树 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RListTreeString"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    roleMenuTreeselect: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 角色ID */
                roleId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 角色菜单树及选中节点 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RMenuTreeSelectVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    list_14: {
        parameters: {
            query: {
                /** @description 查询条件 */
                menu: components["schemas"]["SysMenuBo"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 菜单列表 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RListSysMenuVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    getRouters: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 当前用户可访问的路由信息 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RListRouterVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    getInfo_12: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 字典ID */
                dictId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 字典类型详情 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RSysDictTypeVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    optionselect_3: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 字典类型列表 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RListSysDictTypeVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    list_15: {
        parameters: {
            query: {
                /** @description 查询条件 */
                dictType: components["schemas"]["SysDictTypeBo"];
                /** @description 分页参数 */
                pageQuery: components["schemas"]["PageQuery"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 字典类型分页结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPageResultSysDictTypeVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    getInfo_13: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 字典code */
                dictCode: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 字典数据详情 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RSysDictDataVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    dictType: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 字典类型 */
                dictType: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 字典数据列表 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RListSysDictDataVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    list_16: {
        parameters: {
            query: {
                /** @description 查询条件 */
                dictData: components["schemas"]["SysDictDataBo"];
                /** @description 分页参数 */
                pageQuery: components["schemas"]["PageQuery"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 字典数据分页结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPageResultSysDictDataVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    getInfo_14: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 部门ID */
                deptId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 部门详情 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RSysDeptVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    remove_7: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 部门ID */
                deptId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    optionselect_4: {
        parameters: {
            query?: {
                /** @description 部门ID串 */
                deptIds?: number[];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 部门列表 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RListSysDeptVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    list_17: {
        parameters: {
            query: {
                /** @description 查询条件 */
                dept: components["schemas"]["SysDeptBo"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 部门列表 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RListSysDeptVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    excludeChild: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 部门ID */
                deptId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 过滤后的部门列表 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RListSysDeptVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    getInfo_15: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 参数ID */
                configId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 参数配置详情 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RSysConfigVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    list_18: {
        parameters: {
            query: {
                /** @description 查询条件 */
                config: components["schemas"]["SysConfigBo"];
                /** @description 分页参数 */
                pageQuery: components["schemas"]["PageQuery"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 参数配置分页结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPageResultSysConfigVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    getConfigKey: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 参数Key */
                configKey: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 参数值 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RString"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    getInfo_16: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 主键 */
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 客户端详情 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RSysClientVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    list_19: {
        parameters: {
            query: {
                /** @description 查询条件 */
                bo: components["schemas"]["SysClientBo"];
                /** @description 分页参数 */
                pageQuery: components["schemas"]["PageQuery"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 客户端分页数据 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPageResultSysClientVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    smsCode: {
        parameters: {
            query: {
                /** @description 用户手机号 */
                phoneNumber: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    downloadUrl: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                ossId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ROssAccessUrl"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    parts: {
        parameters: {
            query: {
                fingerprint: string;
            };
            header?: never;
            path: {
                uploadToken: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RResumeResponse"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    batch: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                batchId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RBatchView"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    items: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                batchId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RListItemView"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    list_20: {
        parameters: {
            query: {
                /** @description 查询条件 */
                bo: components["schemas"]["SysOssBo"];
                /** @description 分页参数 */
                pageQuery: components["schemas"]["PageQuery"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OSS 分页结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPageResultSysOssVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    listByIds: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description OSS对象ID串 */
                ossIds: number[];
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OSS 对象列表 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RListSysOssVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    getInfo_17: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description OSS配置ID */
                ossConfigId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 配置详情 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RSysOssConfigVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    list_21: {
        parameters: {
            query: {
                /** @description 查询条件 */
                bo: components["schemas"]["SysOssConfigBo"];
                /** @description 分页参数 */
                pageQuery: components["schemas"]["PageQuery"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 配置分页数据 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPageResultSysOssConfigVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    connect: {
        parameters: {
            query: {
                ticket: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description SSE 发射器 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "text/event-stream": components["schemas"]["SseEmitter"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    issue_1: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RString"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    close: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    emailCode: {
        parameters: {
            query: {
                /** @description 邮箱 */
                email: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    accessUrl: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                ownerType: "WORKING" | "SUBMISSION" | "SOURCE" | "VERSION";
                ownerId: number;
                materialRefId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPersonProfileAccessUrl"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    page: {
        parameters: {
            query: {
                query: components["schemas"]["PersonAdminQueryBo"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPageResultPersonProfileSummaryVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    detail: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                profileId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPersonProfileDetailVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    material: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                profileId: number;
                materialRefId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPersonProfileAccessUrl"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    eligibleUsers: {
        parameters: {
            query: {
                keyword: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RListPersonAccountCandidateVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    reviewContext: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                applicationId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPersonReviewContextVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    reviewMaterial: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                applicationId: number;
                materialRefId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPersonProfileAccessUrl"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    tree: {
        parameters: {
            query: {
                scope: "PERSON" | "ENTERPRISE" | "COMMON";
                includeDisabled?: boolean;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RListMaterialNodeView"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    accessUrl_1: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                ownerType: "WORKING" | "SUBMISSION" | "SOURCE" | "VERSION";
                ownerId: number;
                materialRefId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["REnterpriseProfileAccessUrl"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    page_1: {
        parameters: {
            query: {
                query: components["schemas"]["EnterpriseAdminQueryBo"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPageResultEnterpriseProfileSummaryVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    detail_1: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                profileId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["REnterpriseProfileDetailVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    material_1: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                profileId: number;
                materialRefId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["REnterpriseProfileAccessUrl"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    eligibleUsers_1: {
        parameters: {
            query: {
                keyword: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RListEnterpriseAccountCandidateVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    reviewContext_1: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                applicationId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["REnterpriseReviewContextVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    reviewMaterial_1: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                applicationId: number;
                materialRefId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["REnterpriseProfileAccessUrl"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    search: {
        parameters: {
            query: {
                keyword: string;
                pageSize?: number;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPageResultNotifyRecipientUserVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    byIds: {
        parameters: {
            query: {
                userIds: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RListNotifyRecipientUserVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    query: {
        parameters: {
            query?: {
                includeContent?: boolean;
            };
            header?: never;
            path: {
                notificationId: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RNotificationSnapshot"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    get_2: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                noticeId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RNotifyNoticeVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    list_22: {
        parameters: {
            query: {
                query: components["schemas"]["NotifyNoticeBo"];
                pageQuery: components["schemas"]["PageQuery"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPageResultNotifyNoticeVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    snapshot: {
        parameters: {
            query: {
                /** @description 通知编号 */
                notificationId: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 通知快照 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RNotificationSnapshot"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    deliveries: {
        parameters: {
            query?: {
                /** @description 用户编号，可选 */
                userId?: number;
                /** @description 渠道，可选 */
                channel?: string;
                /** @description 状态，可选 */
                status?: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 投递日志 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RListNotificationDeliveryView"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    list_23: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RListNotifyInboxMessageVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    list_24: {
        parameters: {
            query: {
                /** @description 查询条件 */
                operLog: components["schemas"]["SysOperLogBo"];
                /** @description 分页参数 */
                pageQuery: components["schemas"]["PageQuery"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 操作日志分页结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPageResultSysOperLogVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    getInfo_18: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 当前用户在线设备列表 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPageResultSysUserOnline"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    list_25: {
        parameters: {
            query: {
                /** @description IP地址 */
                ipaddr: string;
                /** @description 用户名 */
                userName: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 在线用户分页数据 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPageResultSysUserOnline"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    unlock_1: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 用户名 */
                userName: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    list_26: {
        parameters: {
            query: {
                /** @description 查询条件 */
                loginInfo: components["schemas"]["SysLoginInfoBo"];
                /** @description 分页参数 */
                pageQuery: components["schemas"]["PageQuery"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 登录日志分页结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPageResultSysLoginInfoVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    getInfo_19: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description Redis 信息、库大小与命令统计 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RCacheListInfoVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
            /** @description Internal Server Error */
            500: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    send_1: {
        parameters: {
            query: {
                /** @description 目标用户 */
                userId: number;
                /** @description 发送内容 */
                message: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    getInfo_20: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 测试树ID */
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RTestTreeVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    list_27: {
        parameters: {
            query: {
                bo: components["schemas"]["TestTreeBo"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RListTestTreeVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    export_13: {
        parameters: {
            query: {
                bo: components["schemas"]["TestTreeBo"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    sendTencent: {
        parameters: {
            query: {
                /** @description 电话号 */
                phones: string;
                /** @description 模板ID */
                templateId: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RObject"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    sendAliyun: {
        parameters: {
            query: {
                /** @description 电话号 */
                phones: string;
                /** @description 模板ID */
                templateId: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RObject"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    removeBlacklist: {
        parameters: {
            query: {
                /** @description 手机号 */
                phone: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RObject"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    addBlacklist: {
        parameters: {
            query: {
                /** @description 手机号 */
                phone: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RObject"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    test: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RTestSensitive"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    tempPermission: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    loginTypeSpecify: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    ignoreOverride: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    singleRole: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    singlePermission: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    loginOnly: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    ignoreAll: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    roleWildcardPrefix: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    permWithOrRole: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    permWildcardPrefix: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    mixRolePermOr: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    mixRolePermAnd: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    multiRoleOr: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    multiRoleAnd: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    multiPermOr: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    multiPermAnd: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    testLock4j: {
        parameters: {
            query: {
                key: string;
                value: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RString"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    testLock4jLockTemplate: {
        parameters: {
            query: {
                key: string;
                value: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RString"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    sub: {
        parameters: {
            query: {
                /** @description 通道Key */
                key: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    pub: {
        parameters: {
            query: {
                /** @description 通道Key */
                key: string;
                /** @description 发送内容 */
                value: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    testip: {
        parameters: {
            query: {
                value: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RString"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    testcluster: {
        parameters: {
            query: {
                value: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RString"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    test_1: {
        parameters: {
            query: {
                value: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RString"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    testObj: {
        parameters: {
            query: {
                value: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RString"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    remove_8: {
        parameters: {
            query: {
                /** @description 队列名 */
                queueName: string;
                /** @description 对象名 */
                name: string;
                /** @description 排序号 */
                orderNum: number;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    get_3: {
        parameters: {
            query: {
                /** @description 队列名 */
                queueName: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    add_20: {
        parameters: {
            query: {
                /** @description 队列名 */
                queueName: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    echo: {
        parameters: {
            query?: {
                /** @description 待回显的消息，未传入时使用默认示例文本 */
                message?: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 包含回显消息的统一响应结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RString"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    send_2: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 是否发送成功 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": boolean;
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    tools: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RMapStringListString"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    resource: {
        parameters: {
            query?: {
                uri?: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RMapStringMcpResourceReadResult"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    receive: {
        parameters: {
            query?: {
                toolName?: string;
                id?: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RMcpDemoHandleResult"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    sendSimpleMessage: {
        parameters: {
            query: {
                /** @description 接收人 */
                to: string;
                /** @description 标题 */
                subject: string;
                /** @description 内容 */
                text: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    sendMessageWithAttachments: {
        parameters: {
            query: {
                /** @description 接收人 */
                to: string;
                /** @description 标题 */
                subject: string;
                /** @description 内容 */
                text: string;
                ossIds: number[];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    sendMessageWithAttachment: {
        parameters: {
            query: {
                /** @description 接收人 */
                to: string;
                /** @description 标题 */
                subject: string;
                /** @description 内容 */
                text: string;
                ossId: number;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    get_4: {
        parameters: {
            query: {
                /** @description 国际化code */
                code: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    test2: {
        parameters: {
            query: {
                bo: components["schemas"]["TestI18nBo"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RTestI18nBo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    test1: {
        parameters: {
            query: {
                str: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    exportWithOptions: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    exportTemplateOne: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    exportTemplateMultiSheet: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    exportTemplateMuliti: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    customExport: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    test_2: {
        parameters: {
            query: {
                /** @description 测试key */
                key: string;
                /** @description 测试value */
                value: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RMapStringTestDemoEncrypt"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    getInfo_21: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 测试ID */
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RTestDemoVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    page_2: {
        parameters: {
            query: {
                bo: components["schemas"]["TestDemoBo"];
                pageQuery: components["schemas"]["PageQuery"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPageResultTestDemoVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    list_28: {
        parameters: {
            query: {
                bo: components["schemas"]["TestDemoBo"];
                pageQuery: components["schemas"]["PageQuery"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RPageResultTestDemoVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    test6: {
        parameters: {
            query: {
                key: string;
                value: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RBoolean"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    test3: {
        parameters: {
            query: {
                key: string;
                value: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RString"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    test2_1: {
        parameters: {
            query: {
                key: string;
                value: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RString"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    test1_1: {
        parameters: {
            query: {
                key: string;
                value: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RString"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    getCode: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 验证码信息 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RCaptchaVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    clientContext: {
        parameters: {
            query?: {
                /** @description 客户端标识（查询参数） */
                clientId?: string;
            };
            header?: {
                /** @description 客户端标识（请求头） */
                clientid?: string;
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 客户端是否可用及是否开放注册 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RAuthClientContextVo"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    authBinding: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 登录来源 */
                source: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 跳转地址 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RString"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    myAgents: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ResultListOpenApiAgentVO"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    getMessages: {
        parameters: {
            query: {
                agentId: number;
                conversationId: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ResultListOpenApiMessageVO"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    getMessages_1: {
        parameters: {
            query: {
                agentId: number;
                conversationId: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ResultListOpenApiMessageVO"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    config_1: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ResultMapStringObject"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    listAgents: {
        parameters: {
            query: {
                arg0: components["schemas"]["OpenApiAgentQueryRequest"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["PageResultListOpenApiAgentVO"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    getAgent: {
        parameters: {
            query: {
                arg0: components["schemas"]["OpenApiAgentIdentityRequest"];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ResultOpenApiAgentVO"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    index: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 后端运行时间信息 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    remove_9: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 主键串 */
                ids: number[];
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    remove_10: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 主键串 */
                ids: number[];
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    deleteHisByInstanceIds: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 实例id */
                instanceIds: number[];
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    deleteByInstanceIds: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 实例id */
                instanceIds: number[];
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    deleteByBusinessIds: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 业务id */
                businessIds: number[];
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    remove_11: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 流程定义ID集合 */
                ids: number[];
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    remove_12: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 主键串 */
                userTypeIds: number[];
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    remove_13: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 用户ID数组 */
                userIds: number[];
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    remove_14: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 角色ID串 */
                roleIds: number[];
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    remove_15: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 岗位ID串 */
                postIds: number[];
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    remove_16: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 菜单ID串 */
                menuIds: number[];
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    remove_17: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 字典ID串 */
                dictIds: number[];
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    refreshCache: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    remove_18: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 字典code串 */
                dictCodes: number[];
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    remove_19: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 参数ID串 */
                configIds: number[];
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    refreshCache_1: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    remove_20: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 主键串 */
                ids: number[];
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    remove_21: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description OSS对象ID串 */
                ossIds: number[];
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    abort: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                uploadToken: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    remove_22: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 日志ids */
                operIds: number[];
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    clean: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    forceLogout: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description token值 */
                tokenId: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    remove_23: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description token值 */
                tokenId: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    remove_24: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 日志ids */
                infoIds: number[];
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    clean_1: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    remove_25: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 测试树ID串 */
                ids: number[];
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    remove_26: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 测试ID串 */
                ids: number[];
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    remove_27: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
    unlockSocial: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description socialId */
                socialId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 操作结果 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RVoid"];
                };
            };
            /** @description Unauthorized */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
        };
    };
}
