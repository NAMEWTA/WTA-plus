# 前后端合同映射

实现或审查一个跨层资源时，使用下表建立一一对应关系。当前源码、OpenAPI、POM、SQL 和测试优先于模板摘要。

| 后端事实 | 前端对应 | 必须核对 |
|---|---|---|
| Controller base path / method | domain `service.ts` 或 transport | URL、GET/POST、path/query/body、返回壳 |
| BO/VO/Row/DTO | domain-owned model、form、query、mapper | nullable、ID 类型、日期、分页、敏感字段 |
| `@SaCheckPermission`、Client/UserType、数据范围 | manifest permissions、`v-hasPermi`、App evaluator | 未登录、无权限、跨 Client 失败关闭 |
| 菜单 componentKey | web-domain registration、manifest、router projection | 组件键、页面 owner、未选择能力 |
| 菜单 icon 字符串 | SvgIcon 协议解析、本地 SVG、离线 Tabler、外部 Iconify | `local-name`、`tabler:name`、`prefix:name`、fallback、旧无前缀兼容 |
| dictType/dictValue | `runtime.dicts()`、`DictTag`、option/label | label/value、样式、异步、缓存、未匹配回退 |
| 事务、缓存、事件、外部调用 | loading、取消、重试、反馈和刷新 | 幂等、失败补偿、卸载后不写入 |
| DDL/DML、排序、唯一约束 | 表单校验、查询条件、排序和错误提示 | 50/60 SQL owner、MySQL 8.4、边界值 |
| 导入/导出/文件/OSS | download/upload adapter、页面状态 | VO 字段、权限、脱敏、资源生命周期 |

CRUD 目标合同是查询 GET、变更 POST 和变更 `@Log`。旧模块若仍使用 PUT/DELETE，必须标记为 legacy，不得作为新模板或新 API 范例。

动态菜单与图标必须按以下链路核对：

```text
服务端 Client 菜单
  -> navigation Store
  -> App manifest componentKey 投影
  -> Vue Router addRoute
  -> SvgIcon icon 协议解析
  -> 本地 SVG / 离线 Tabler / 外部 Iconify / fallback
```

`icon` 只改变前端显示，不参与权限判断；`sys_menu.icon` 仍为字符串字段。新建菜单优先写 `tabler:name`，功能节点写 `#`，外部 Iconify 名称必须明确在线或额外注册前提。
