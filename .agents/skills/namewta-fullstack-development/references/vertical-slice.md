# 垂直切片合同

一个切片必须能从用户行为追溯到持久化和交付证据。实现者先锁定边界，再同步各层，不把前端页面或后端接口当成独立完成标准。

## 设计输入

- 用户触发、成功结果、失败结果、空值和权限失败行为。
- 唯一业务 owner、后端模块/Controller、表和 SQL 基座 owner。
- `wta-api`/common SPI、Client/UserType、数据范围、事务、缓存和外部副作用。
- 前端领域资源、页面 owner、App 选择、菜单 componentKey、按钮权限和字典。

## 实现顺序

1. 以当前源码、POM/package、OpenAPI、SQL 和成熟同 owner 实现冻结事实。
2. 写出合同映射，再确定 backend mode、目录、DTO/VO、transport/domain model 和页面状态。
3. 后端先完成访问面、校验、业务用例、持久化、事务、缓存和事件；前端随后完成 transport 映射、资源导出、页面、runtime、manifest 和 App 组合。
4. 每层增加对应测试，并用真实请求/响应和权限菜单做集成验收。
5. 记录实际修改、命令、退出码、偏差和残余风险。

## 完成门槛

只有下列内容全部可追溯时切片才算完成：

- Controller、HTTP method、参数、响应与前端 service 一致。
- 表、Entity/BO/VO、Mapper/XML、DDL/DML 和数据权限一致。
- domain 与 web-domain 的公开入口、页面状态和 App manifest 一致。
- 菜单 componentKey、按钮权限、后端授权和测试一致。
- 字典、翻译、缓存、错误、幂等、事务和副作用行为已验证。
- 受影响的前端、后端、SQL、契约和发布门禁均有证据。
