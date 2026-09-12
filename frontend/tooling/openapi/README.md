# OpenAPI 工具

本工具为私有 `@namewta/api-contracts` 提供确定性、包内运行的合同工作流。

- `openapi:fetch -- --source <url-or-file> --backend-commit <40-character-sha>`：校验 OpenAPI 3.0/3.1，保存带来源的不可变快照版本，再原子更新 `current.json`。
- `openapi:generate`：校验当前版本来源，并从已提交快照离线生成 TypeScript。
- `openapi:check`：在内存中重新生成并比较已提交结果，全程不写文件。

生成器使用自身锁定且受支持的 TypeScript 5.9.3；生成合同由工作区 TypeScript 6 消费和检查。

测试覆盖无效或不可达来源保持最后可用版本、未激活版本不影响生成、来源/快照漂移、手工修改生成文件、错误 URL 敏感信息脱敏和重新生成闭环。
