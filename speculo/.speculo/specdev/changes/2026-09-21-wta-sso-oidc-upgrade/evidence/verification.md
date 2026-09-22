# G 工件验证

日期：2026-09-21。验证对象为当前 change 文档与状态；不包含产品实现或生产环境验收。

## 已执行

| 检查 | 结果 | 说明 |
|---|---|---|
| 初次 `validate-specdev --stage grill` | exit 1，10 errors / 0 warnings | 新来源文件缺 source schema frontmatter 和三个规定章节；不是产品故障 |
| 修复来源工件后再次 grill 校验 | exit 0，0 errors / 0 warnings | 补 conversation 类型、真实会话原文摘要及章节；没有把缺失 artifact 填成已读 |
| 文档所有 Path 目标存在性 | exit 0 | 根据 workspace roots 解析，无失效引用 |
| 来源正文摘要核对 | exit 0 | 会话 UTF-8 原文 SHA-256 与 source metadata 一致 |
| 保护已有改动与状态增量 | exit 0 | 7 个既有修改/删除路径逐字节或缺失状态不变；全局索引仅增加本 change；HEAD 不变 |
| 文档只读复核 | 完成 | 补 prompt=none/max_age/auth_time、offline_access 同意、最小 openid scope；未把推荐写成实现/共识 |
| `git diff --check` 及直接文本检查 | exit 0 | 17 个本 change 文件通过换行/尾空白/JSON 检查；97 处 Path 引用有效；四份 G 源工件已完整回读 |

命令形式：

```text
node <Path>{roots.workflows}/specdev/common/tools/validate-specdev.mjs</Path> --stage grill --repo <project-root> <Path>{roots.state}/specdev/changes/2026-09-21-wta-sso-oidc-upgrade/</Path>
```

实际执行时按已打开的 workspace roots 解析 Path。最终结果文件：<Path>{roots.state}/specdev/changes/2026-09-21-wta-sso-oidc-upgrade/evidence/grill-validation.txt</Path>。

源码摘要与保护校验记录：<Path>{roots.state}/specdev/changes/2026-09-21-wta-sso-oidc-upgrade/evidence/repository-snapshot.json</Path>。快照记录 42 个在文档中引用的项目源文件；它是当前静态审查定位，不代表构建输入全量快照。

## 未执行和剩余限制

- Claude artifact 全文未取得；HTTP/Chrome 返回访问验证页，不能宣布其内容已完整 review。
- G 高影响质询与用户最终共识尚未完成，设计树不为 consensus；结构校验通过不等于需求收口。
- 没有修改产品代码、数据库、依赖、发布配置或永久知识。
- 未运行 Maven、前端 lint/typecheck/unit/build、浏览器产品验收、真实服务、OIDC conformance、数据库迁移或部署；本轮仅修改文档，没有将这些门禁伪报通过。
- 后续必须用真实 System 账户和真实业务 Token 补齐现有 SSO fixture 的覆盖缺口。
