# CRUD 开发术语

- **Source:** `2026-08-28-retire-runtime-code-generator` CONTEXT
- **Graduated:** 2026-08-29

**CRUD 标准模板资产**：父仓库 `<Path>docs/fm/</Path>` 中供 AI 与开发者参考的 Java、Vue、React、XML 和 SQL 静态模板及开发约束；不进入产品 classpath，不提供在线生成接口。
_Avoid_: 运行时模板、在线低代码生成器

**运行时代码生成器（已退役）**：曾由后端 `ruoyi-gen`、前端 gen domain/web-domain、`/tool/gen` 接口与菜单权限、`gen_table*` 元数据共同组成的在线能力；只用于解释历史记录，不是当前模块或可复用入口。
_Avoid_: CRUD 标准模板资产、OpenAPI 合同生成工具
