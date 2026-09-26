# T-26 HTTP合同盘点

70项旧方法均为第一方变更，全部迁移；不改参数、权限标识和业务错误，新增POST冲突时仅更新操作增加/update。Log安全配置与真实314条编译后MVC映射另见测试和JSON。

| 旧method/path | 新method/path | Owner / 操作 | 权限 / 调用者 |
|---|---|---|---|
| `DELETE /auth/unlock/{socialId}` | `POST /auth/unlock/{socialId}` | AuthController.unlockSocial | StpUtil.checkLogin + 社交绑定owner；frontend/packages/domains/admin/src/index.ts |
| `PUT /es/update` | `POST /es/update` | EsCrudController.update | 沿用既有登录检查；未新增方法级权限，不推断额外owner保护；无仓内domain调用者；后端条件/演示HTTP入口，运行时映射测试覆盖 |
| `DELETE /es/delete/{id}` | `POST /es/delete/{id}` | EsCrudController.delete | 沿用既有登录检查；未新增方法级权限，不推断额外owner保护；无仓内domain调用者；后端条件/演示HTTP入口，运行时映射测试覆盖 |
| `DELETE /demo/batch` | `POST /demo/batch` | TestBatchController.remove | 沿用既有登录检查；未新增方法级权限，不推断额外owner保护；无仓内domain调用者；后端条件/演示HTTP入口，运行时映射测试覆盖 |
| `PUT /demo/demo` | `POST /demo/demo/update` | TestDemoController.edit | "demo:demo:edit"；frontend/packages/domains/demo/src/index.ts, frontend/packages/domains/demo/src/test-demo/index.ts |
| `DELETE /demo/demo/{ids}` | `POST /demo/demo/{ids}` | TestDemoController.remove | "demo:demo:remove"；frontend/packages/domains/demo/src/index.ts, frontend/packages/domains/demo/src/test-demo/index.ts |
| `PUT /demo/tree` | `POST /demo/tree/update` | TestTreeController.edit | "demo:tree:edit"；frontend/packages/domains/demo/src/index.ts, frontend/packages/domains/demo/src/test-tree/index.ts |
| `DELETE /demo/tree/{ids}` | `POST /demo/tree/{ids}` | TestTreeController.remove | "demo:tree:remove"；frontend/packages/domains/demo/src/index.ts, frontend/packages/domains/demo/src/test-tree/index.ts |
| `PUT /workflow/leave` | `POST /workflow/leave/update` | TestLeaveController.edit | "workflow:leave:edit"；frontend/packages/domains/workflow/src/index.ts, frontend/packages/domains/workflow/src/leave/index.ts |
| `DELETE /workflow/leave/{ids}` | `POST /workflow/leave/{ids}` | TestLeaveController.remove | "workflow:leave:remove"；frontend/packages/domains/workflow/src/index.ts, frontend/packages/domains/workflow/src/leave/index.ts |
| `DELETE /workflow/instance/deleteByBusinessIds/{businessIds}` | `POST /workflow/instance/deleteByBusinessIds/{businessIds}` | FlwInstanceController.deleteByBusinessIds | "workflow:instance:remove"；frontend/packages/domains/workflow/src/index.ts, frontend/packages/domains/workflow/src/instance/index.ts |
| `DELETE /workflow/instance/deleteByInstanceIds/{instanceIds}` | `POST /workflow/instance/deleteByInstanceIds/{instanceIds}` | FlwInstanceController.deleteByInstanceIds | "workflow:instance:remove"；frontend/packages/domains/workflow/src/index.ts, frontend/packages/domains/workflow/src/instance/index.ts |
| `DELETE /workflow/instance/deleteHisByInstanceIds/{instanceIds}` | `POST /workflow/instance/deleteHisByInstanceIds/{instanceIds}` | FlwInstanceController.deleteHisByInstanceIds | "workflow:instance:remove"；frontend/packages/domains/workflow/src/index.ts, frontend/packages/domains/workflow/src/instance/index.ts |
| `PUT /workflow/instance/cancelProcessApply` | `POST /workflow/instance/cancelProcessApply` | FlwInstanceController.cancelProcessApply | "workflow:instance:cancel"；frontend/packages/domains/workflow/src/index.ts, frontend/packages/domains/workflow/src/instance/index.ts |
| `PUT /workflow/instance/active/{id}` | `POST /workflow/instance/active/{id}` | FlwInstanceController.active | "workflow:instance:active"；frontend/packages/domains/workflow/src/index.ts, frontend/packages/domains/workflow/src/instance/index.ts |
| `PUT /workflow/instance/updateVariable` | `POST /workflow/instance/updateVariable` | FlwInstanceController.updateVariable | "workflow:instance:variable"；frontend/packages/domains/workflow/src/index.ts, frontend/packages/domains/workflow/src/instance/index.ts |
| `PUT /workflow/task/updateAssignee/{userId}` | `POST /workflow/task/updateAssignee/{userId}` | FlwTaskController.updateAssignee | "workflow:task:edit"；frontend/packages/domains/workflow/src/index.ts, frontend/packages/domains/workflow/src/task/index.ts |
| `PUT /workflow/definition` | `POST /workflow/definition/update` | FlwDefinitionController.edit | "workflow:definition:edit"；frontend/packages/domains/workflow/src/index.ts, frontend/packages/domains/workflow/src/definition/index.ts |
| `PUT /workflow/definition/publish/{id}` | `POST /workflow/definition/publish/{id}` | FlwDefinitionController.publish | "workflow:definition:publish"；frontend/packages/domains/workflow/src/index.ts, frontend/packages/domains/workflow/src/definition/index.ts |
| `PUT /workflow/definition/unPublish/{id}` | `POST /workflow/definition/unPublish/{id}` | FlwDefinitionController.unPublish | "workflow:definition:publish"；frontend/packages/domains/workflow/src/index.ts, frontend/packages/domains/workflow/src/definition/index.ts |
| `DELETE /workflow/definition/{ids}` | `POST /workflow/definition/{ids}` | FlwDefinitionController.remove | "workflow:definition:remove"；frontend/packages/domains/workflow/src/index.ts, frontend/packages/domains/workflow/src/definition/index.ts |
| `PUT /workflow/definition/active/{id}` | `POST /workflow/definition/active/{id}` | FlwDefinitionController.active | "workflow:definition:active"；frontend/packages/domains/workflow/src/index.ts, frontend/packages/domains/workflow/src/definition/index.ts |
| `PUT /workflow/spel` | `POST /workflow/spel/update` | FlwSpelController.edit | "workflow:spel:edit"；frontend/packages/domains/workflow/src/index.ts, frontend/packages/domains/workflow/src/spel/index.ts |
| `DELETE /workflow/spel/{ids}` | `POST /workflow/spel/{ids}` | FlwSpelController.remove | "workflow:spel:remove"；frontend/packages/domains/workflow/src/index.ts, frontend/packages/domains/workflow/src/spel/index.ts |
| `PUT /workflow/category` | `POST /workflow/category/update` | FlwCategoryController.edit | "workflow:category:edit"；frontend/packages/domains/workflow/src/index.ts, frontend/packages/domains/workflow/src/category/index.ts |
| `DELETE /workflow/category/{categoryId}` | `POST /workflow/category/{categoryId}` | FlwCategoryController.remove | "workflow:category:remove"；frontend/packages/domains/workflow/src/index.ts, frontend/packages/domains/workflow/src/category/index.ts |
| `DELETE /monitor/operlog/{operIds}` | `POST /monitor/operlog/{operIds}` | SysOperlogController.remove | "monitor:operlog:remove"；frontend/packages/domains/system/src/monitor/index.ts, frontend/packages/domains/system/src/monitor/operlog/index.ts |
| `DELETE /monitor/operlog/clean` | `POST /monitor/operlog/clean` | SysOperlogController.clean | "monitor:operlog:remove"；frontend/packages/domains/system/src/monitor/index.ts, frontend/packages/domains/system/src/monitor/operlog/index.ts |
| `DELETE /monitor/loginInfo/{infoIds}` | `POST /monitor/loginInfo/{infoIds}` | SysLoginInfoController.remove | "monitor:logininfo:remove"；frontend/packages/domains/system/src/monitor/index.ts, frontend/packages/domains/system/src/monitor/login-info/index.ts |
| `DELETE /monitor/loginInfo/clean` | `POST /monitor/loginInfo/clean` | SysLoginInfoController.clean | "monitor:logininfo:remove"；frontend/packages/domains/system/src/monitor/index.ts, frontend/packages/domains/system/src/monitor/login-info/index.ts |
| `DELETE /monitor/online/{tokenId}` | `POST /monitor/online/{tokenId}` | SysUserOnlineController.forceLogout | "monitor:online:forceLogout"；frontend/packages/domains/system/src/monitor/index.ts, frontend/packages/domains/system/src/monitor/online/index.ts |
| `DELETE /monitor/online/myself/{tokenId}` | `POST /monitor/online/myself/{tokenId}` | SysUserOnlineController.remove | 沿用既有登录检查；未新增方法级权限，不推断额外owner保护；frontend/packages/domains/system/src/monitor/index.ts, frontend/packages/domains/system/src/monitor/online/index.ts |
| `PUT /system/dict/data` | `POST /system/dict/data/update` | SysDictDataController.edit | "system:dict:edit"；frontend/packages/domains/system/src/resource-service.ts, frontend/packages/domains/system/src/dict-data/index.ts, frontend/packages/domains/system/src/dict-type/index.ts, frontend/packages/domains/system/src/dict-type/public.ts |
| `DELETE /system/dict/data/{dictCodes}` | `POST /system/dict/data/{dictCodes}` | SysDictDataController.remove | "system:dict:remove"；frontend/packages/domains/system/src/resource-service.ts, frontend/packages/domains/system/src/dict-data/index.ts, frontend/packages/domains/system/src/dict-type/index.ts, frontend/packages/domains/system/src/dict-type/public.ts |
| `PUT /system/role` | `POST /system/role/update` | SysRoleController.edit | "system:role:edit"；frontend/packages/domains/system/src/service.ts, frontend/packages/domains/system/src/role/index.ts |
| `PUT /system/role/permission` | `POST /system/role/permission` | SysRoleController.editPermission | "system:role:edit"；frontend/packages/domains/system/src/service.ts, frontend/packages/domains/system/src/role/index.ts |
| `PUT /system/role/changeStatus` | `POST /system/role/changeStatus` | SysRoleController.changeStatus | "system:role:edit"；frontend/packages/domains/system/src/service.ts, frontend/packages/domains/system/src/role/index.ts |
| `DELETE /system/role/{roleIds}` | `POST /system/role/{roleIds}` | SysRoleController.remove | "system:role:remove"；frontend/packages/domains/system/src/service.ts, frontend/packages/domains/system/src/role/index.ts |
| `PUT /system/role/authUser/cancel` | `POST /system/role/authUser/cancel` | SysRoleController.cancelAuthUser | "system:role:edit"；frontend/packages/domains/system/src/service.ts, frontend/packages/domains/system/src/role/index.ts |
| `PUT /system/role/authUser/cancelAll` | `POST /system/role/authUser/cancelAll` | SysRoleController.cancelAuthUserAll | "system:role:edit"；frontend/packages/domains/system/src/service.ts, frontend/packages/domains/system/src/role/index.ts |
| `PUT /system/role/authUser/selectAll` | `POST /system/role/authUser/selectAll` | SysRoleController.selectAuthUserAll | "system:role:edit"；frontend/packages/domains/system/src/service.ts, frontend/packages/domains/system/src/role/index.ts |
| `PUT /system/user/profile` | `POST /system/user/profile` | SysProfileController.updateProfile | 沿用既有登录检查；未新增方法级权限，不推断额外owner保护；frontend/packages/domains/system/src/service.ts, frontend/packages/domains/system/src/user/index.ts, frontend/packages/domains/system/src/user/public.ts, frontend/packages/domains/system/src/user-type/index.ts, frontend/packages/domains/system/src/profile/index.ts |
| `PUT /system/user/profile/updatePwd` | `POST /system/user/profile/updatePwd` | SysProfileController.updatePwd | 沿用既有登录检查；未新增方法级权限，不推断额外owner保护；frontend/packages/domains/system/src/service.ts, frontend/packages/domains/system/src/user/index.ts, frontend/packages/domains/system/src/user/public.ts, frontend/packages/domains/system/src/user-type/index.ts, frontend/packages/domains/system/src/profile/index.ts |
| `PUT /system/menu` | `POST /system/menu/update` | SysMenuController.edit | SystemConstants.SUPER_ADMIN_ROLE_KEY, "system:menu:edit"；frontend/packages/domains/system/src/service.ts, frontend/packages/domains/system/src/menu/index.ts, frontend/packages/domains/system/src/menu/public.ts |
| `DELETE /system/menu/{menuId}` | `POST /system/menu/{menuId}` | SysMenuController.remove | SystemConstants.SUPER_ADMIN_ROLE_KEY, "system:menu:remove"；frontend/packages/domains/system/src/service.ts, frontend/packages/domains/system/src/menu/index.ts, frontend/packages/domains/system/src/menu/public.ts |
| `DELETE /system/menu/cascade/{menuIds}` | `POST /system/menu/cascade/{menuIds}` | SysMenuController.remove | SystemConstants.SUPER_ADMIN_ROLE_KEY, "system:menu:remove"；frontend/packages/domains/system/src/service.ts, frontend/packages/domains/system/src/menu/index.ts, frontend/packages/domains/system/src/menu/public.ts |
| `PUT /system/dict/type` | `POST /system/dict/type/update` | SysDictTypeController.edit | "system:dict:edit"；frontend/packages/domains/system/src/resource-service.ts, frontend/packages/domains/system/src/dict-data/index.ts, frontend/packages/domains/system/src/dict-type/index.ts, frontend/packages/domains/system/src/dict-type/public.ts |
| `DELETE /system/dict/type/{dictIds}` | `POST /system/dict/type/{dictIds}` | SysDictTypeController.remove | "system:dict:remove"；frontend/packages/domains/system/src/resource-service.ts, frontend/packages/domains/system/src/dict-data/index.ts, frontend/packages/domains/system/src/dict-type/index.ts, frontend/packages/domains/system/src/dict-type/public.ts |
| `DELETE /system/dict/type/refreshCache` | `POST /system/dict/type/refreshCache` | SysDictTypeController.refreshCache | "system:dict:remove"；frontend/packages/domains/system/src/resource-service.ts, frontend/packages/domains/system/src/dict-data/index.ts, frontend/packages/domains/system/src/dict-type/index.ts, frontend/packages/domains/system/src/dict-type/public.ts |
| `DELETE /resource/oss/{ossIds}` | `POST /resource/oss/{ossIds}` | SysOssController.remove | "system:oss:remove"；frontend/packages/domains/system/src/resource-service.ts, frontend/packages/domains/system/src/oss/index.ts, frontend/packages/domains/system/src/oss-config/index.ts, frontend/packages/domains/system/src/oss-upload/index.ts |
| `PUT /system/client` | `POST /system/client/update` | SysClientController.edit | "system:client:edit"；frontend/packages/domains/system/src/service.ts, frontend/packages/domains/system/src/client/index.ts |
| `PUT /system/client/changeStatus` | `POST /system/client/changeStatus` | SysClientController.changeStatus | "system:client:edit"；frontend/packages/domains/system/src/service.ts, frontend/packages/domains/system/src/client/index.ts |
| `DELETE /system/client/{ids}` | `POST /system/client/{ids}` | SysClientController.remove | "system:client:remove"；frontend/packages/domains/system/src/service.ts, frontend/packages/domains/system/src/client/index.ts |
| `DELETE /resource/oss/uploads/{uploadToken}` | `POST /resource/oss/uploads/{uploadToken}` | SysOssUploadController.abort | "system:oss:upload"；frontend/packages/domains/system/src/resource-service.ts, frontend/packages/domains/system/src/oss/index.ts, frontend/packages/domains/system/src/oss-config/index.ts, frontend/packages/domains/system/src/oss-upload/index.ts |
| `PUT /system/userType` | `POST /system/userType/update` | SysUserTypeController.edit | "system:userType:edit"；frontend/packages/domains/system/src/service.ts, frontend/packages/domains/system/src/user-type/index.ts |
| `PUT /system/userType/changeStatus` | `POST /system/userType/changeStatus` | SysUserTypeController.changeStatus | "system:userType:edit"；frontend/packages/domains/system/src/service.ts, frontend/packages/domains/system/src/user-type/index.ts |
| `DELETE /system/userType/{userTypeIds:\\d+(?:,\\d+)*}` | `POST /system/userType/{userTypeIds:\\d+(?:,\\d+)*}` | SysUserTypeController.remove | "system:userType:remove"；frontend/packages/domains/system/src/service.ts, frontend/packages/domains/system/src/user-type/index.ts |
| `PUT /system/dept` | `POST /system/dept/update` | SysDeptController.edit | "system:dept:edit"；frontend/packages/domains/system/src/service.ts, frontend/packages/domains/system/src/dept/index.ts |
| `DELETE /system/dept/{deptId}` | `POST /system/dept/{deptId}` | SysDeptController.remove | "system:dept:remove"；frontend/packages/domains/system/src/service.ts, frontend/packages/domains/system/src/dept/index.ts |
| `PUT /system/post` | `POST /system/post/update` | SysPostController.edit | "system:post:edit"；frontend/packages/domains/system/src/service.ts, frontend/packages/domains/system/src/post/index.ts |
| `DELETE /system/post/{postIds}` | `POST /system/post/{postIds}` | SysPostController.remove | "system:post:remove"；frontend/packages/domains/system/src/service.ts, frontend/packages/domains/system/src/post/index.ts |
| `PUT /system/config` | `POST /system/config/update` | SysConfigController.edit | "system:config:edit"；frontend/packages/domains/system/src/resource-service.ts, frontend/packages/domains/system/src/config/index.ts |
| `PUT /system/config/updateByKey` | `POST /system/config/updateByKey` | SysConfigController.updateByKey | "system:config:edit"；frontend/packages/domains/system/src/resource-service.ts, frontend/packages/domains/system/src/config/index.ts |
| `DELETE /system/config/{configIds}` | `POST /system/config/{configIds}` | SysConfigController.remove | "system:config:remove"；frontend/packages/domains/system/src/resource-service.ts, frontend/packages/domains/system/src/config/index.ts |
| `DELETE /system/config/refreshCache` | `POST /system/config/refreshCache` | SysConfigController.refreshCache | "system:config:remove"；frontend/packages/domains/system/src/resource-service.ts, frontend/packages/domains/system/src/config/index.ts |
| `PUT /system/user` | `POST /system/user/update` | SysUserController.edit | "system:user:edit"；frontend/packages/domains/system/src/service.ts, frontend/packages/domains/system/src/user/index.ts, frontend/packages/domains/system/src/user/public.ts, frontend/packages/domains/system/src/user-type/index.ts, frontend/packages/domains/system/src/profile/index.ts |
| `DELETE /system/user/{userIds}` | `POST /system/user/{userIds}` | SysUserController.remove | "system:user:remove"；frontend/packages/domains/system/src/service.ts, frontend/packages/domains/system/src/user/index.ts, frontend/packages/domains/system/src/user/public.ts, frontend/packages/domains/system/src/user-type/index.ts, frontend/packages/domains/system/src/profile/index.ts |
| `PUT /system/user/resetPwd` | `POST /system/user/resetPwd` | SysUserController.resetPwd | "system:user:resetPwd"；frontend/packages/domains/system/src/service.ts, frontend/packages/domains/system/src/user/index.ts, frontend/packages/domains/system/src/user/public.ts, frontend/packages/domains/system/src/user-type/index.ts, frontend/packages/domains/system/src/profile/index.ts |
| `PUT /system/user/changeStatus` | `POST /system/user/changeStatus` | SysUserController.changeStatus | "system:user:edit"；frontend/packages/domains/system/src/service.ts, frontend/packages/domains/system/src/user/index.ts, frontend/packages/domains/system/src/user/public.ts, frontend/packages/domains/system/src/user-type/index.ts, frontend/packages/domains/system/src/profile/index.ts |
| `PUT /system/user/authRole` | `POST /system/user/authRole` | SysUserController.insertAuthRole | "system:user:edit"；frontend/packages/domains/system/src/service.ts, frontend/packages/domains/system/src/user/index.ts, frontend/packages/domains/system/src/user/public.ts, frontend/packages/domains/system/src/user-type/index.ts, frontend/packages/domains/system/src/profile/index.ts |

## GET副作用迁移

| 路径 | owner | 新方法与理由 |
|---|---|---|
| `/demo/websocket/send` | WebSocketController | POST；真实写入/发送/删除/消费动作，保留原输入和授权 |
| `/demo/mail/sendSimpleMessage` | MailSendController | POST；真实写入/发送/删除/消费动作，保留原输入和授权 |
| `/demo/mail/sendMessageWithAttachment` | MailSendController | POST；真实写入/发送/删除/消费动作，保留原输入和授权 |
| `/demo/mail/sendMessageWithAttachments` | MailSendController | POST；真实写入/发送/删除/消费动作，保留原输入和授权 |
| `/demo/mcp/receive` | McpDemoController | POST；真实写入/发送/删除/消费动作，保留原输入和授权 |
| `/demo/mqtt/send` | MqttController | POST；真实写入/发送/删除/消费动作，保留原输入和授权 |
| `/demo/cache/test2` | RedisCacheController | POST；真实写入/发送/删除/消费动作，保留原输入和授权 |
| `/demo/cache/test3` | RedisCacheController | POST；真实写入/发送/删除/消费动作，保留原输入和授权 |
| `/demo/cache/test6` | RedisCacheController | POST；真实写入/发送/删除/消费动作，保留原输入和授权 |
| `/demo/redis/pubsub/pub` | RedisPubSubController | POST；真实写入/发送/删除/消费动作，保留原输入和授权 |
| `/demo/redis/pubsub/sub` | RedisPubSubController | POST；真实写入/发送/删除/消费动作，保留原输入和授权 |
| `/demo/sms/sendAliyun` | SmsController | POST；真实写入/发送/删除/消费动作，保留原输入和授权 |
| `/demo/sms/sendTencent` | SmsController | POST；真实写入/发送/删除/消费动作，保留原输入和授权 |
| `/demo/sms/addBlacklist` | SmsController | POST；真实写入/发送/删除/消费动作，保留原输入和授权 |
| `/demo/sms/removeBlacklist` | SmsController | POST；真实写入/发送/删除/消费动作，保留原输入和授权 |
| `/demo/queue/priority/add` | PriorityQueueController | POST；真实写入/发送/删除/消费动作，保留原输入和授权 |
| `/demo/queue/priority/remove` | PriorityQueueController | POST；真实写入/发送/删除/消费动作，保留原输入和授权 |
| `/demo/queue/priority/get` | PriorityQueueController | POST；真实写入/发送/删除/消费动作，保留原输入和授权 |
| `/monitor/loginInfo/unlock/{userName}` | SysLoginInfoController | POST；真实写入/发送/删除/消费动作，保留原输入和授权 |
| `/system/user/unlock/{userId}` | SysUserController | POST；真实写入/发送/删除/消费动作，保留原输入和授权 |

## 只读POST与非CRUD边界

- FlwTaskController.getNextNodeList：实际Service只预览节点，迁移为GET；taskId与可选JSON对象variables放query，不接收GET body；调用者为workflow domain与ProcessActionDialog。
- SysOssMigrationController.dryRun：OSS维护计划诊断，不是列表/详情CRUD；保留POST与已有安全Log。
- SysUserController.importTemplate及各export/import：文件流协议保留原GET/POST；补齐缺失POST安全Log。
- SysResetPasswordCandidateController.candidate：密码生成命令；保留POST和禁止敏感正文审计。
- AuthController登录/注册/社交回调/退出：既有认证协议保留POST，补安全Log，不改OAuth协议或认证检查。
- SysOssUploadController.signParts：服务端签名协议保留POST，禁止签名URL/令牌正文审计；浏览器直传对象存储的PUT不属于第一方CRUD，保持供应商签名方法。
- RedisCacheController.test1缓存查询、限流和短期锁演示：保留GET；它们的基础设施缓存/计数/锁不是业务变更。显式缓存写入/删除/TTL修改已在上表改POST。
- CaptchaController与其他未触及模块的认证挑战/验证码、Profile/Notify/Third及外部回调：原owner保留自身协议。本票不宣称整个后端每个GET都没有基础设施副作用。
- SnailAiChatGatewayController（锁定依赖1.1.1）：/api/snail/chat/conversations PUT/DELETE及/api/snail/chat/agent/subscribe DELETE归供应商协议；jar SHA与javap见T-26-third-party-protocols.json。
- EsCrudController：条件easy-es.enable=true才装配；两项旧方法已编译验证为POST，原激活OpenAPI未启用该条件，不虚构不存在的schema。

候选快照由既有schema和实际编译后MVC映射派生，经正式fetch/generate/check生成；不是完整live采集。只读POST、GET副作用的权威实现和失败语义仍以所属Controller/Service为准。

表中调用者列为按资源定位的仓内实现/公开入口导航，包含共享模块，不能当成每个函数的精确调用图。实际请求方法/路径由domain单测、浏览器请求断言与314条编译映射共同核对。
