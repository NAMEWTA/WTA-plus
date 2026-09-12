# 文档与注释

### DOC-001 注释解释非直观原因

Scope: `repository`

Level: SHOULD

Source: `repository-fact` + `builder-baseline`

Rule: 注释用于解释代码无法自证的兼容限制、协议/安全权衡、资源生命周期、事务/并发不变量、生成约束、历史原因和临时例外。不得逐行翻译实现，也不得用注释掩盖模糊命名或职责混合。

Verification: review 新增/修改注释是否提供 WHY、约束或删除条件；删除实现已自证的重复旁白；语言和框架的具体注释形式读取对应 reference。

### DOC-002 Public contract 按生态记录

Scope: public HTTP/JSON contract, `public-api:wta-api`, extension points, shared frontend adapters

Level: SHOULD

Source: `repository-fact` + `builder-baseline`

Rule: 对调用方不可从类型或签名直接得知的输入、输出、失败、副作用、取消、生命周期、安全与兼容要求，使用该语言生态的 JSDoc/Javadoc 或项目合同文档记录。显而易见的 getter、字段和类型信息不重复成文档噪声。

Verification: public API review；对照 controller/BO/VO、TypeScript transport type、Javadoc/JSDoc 与调用方；相关契约测试或构建。

### DOC-003 文档随边界变化同步

Scope: `repository`

Level: MUST

Source: `repository-fact` (`AGENTS.md`, SpecDev ADR/context) + `builder-baseline`

Rule: 模块边界、public API/协议、数据模型/迁移、部署拓扑、质量门禁、支持版本或长期例外发生变化时，同步对应权威文档、ADR、规范或 customization map。实现注释不得成为跨模块合同的唯一权威。

Verification: diff 检查代码、测试和权威文档同步；运行文档/Skill 链接与 schema 校验；交付报告列出未更新项及原因。

### DOC-004 TODO 与文件头不凭偏好新增

Scope: `repository`

Level: SHOULD

Source: `repository-fact` + `builder-baseline`

Rule: 不新增无上下文 TODO/FIXME、统一版权头或 `@author` 文件头。确需 TODO 时关联 owner/issue，写明原因和完成/删除条件；既有第三方或生成文件头按所有权保留。

Verification: review 新增 `TODO|FIXME|HACK|XXX|@author|Copyright`；确认来源、owner 和删除条件；不为统一外观重写无关文件。

### DOC-005 Profile Java 类型与业务方法使用中文 Javadoc

Scope: `path:backend/wta-modules/wta-profile/**/src/main/java/**`

Level: MUST

Rule: `wta-profile` 生产代码中的每个自有类、接口、枚举、record 以及显式声明的方法，必须在声明前提供简明中文 Javadoc。注释至少说明职责、输入输出或业务边界；对锁、事务、幂等、脱敏、外部调用和兼容适配等非直观约束必须说明原因。Lombok 生成的 getter/setter、record 自动 accessor 和纯覆盖且语义已由接口完整说明的方法可以不重复展开，但新增或修改的 public contract 必须补充 `@param`、`@return`、`@throws` 等调用方所需信息。不得用统一的无意义句式替代真实职责说明。

Verification: 评审 `wta-profile` 新增/修改 Java 源码，确认类型和显式方法均有中文 Javadoc；对外 Controller、UseCase、Service、DAO、Mapper 方法核对参数、返回值、失败语义和副作用；运行 Profile 架构测试、Maven 测试和构建。

### DOC-006 新业务模块中文 Javadoc

Scope: `path:backend/wta-modules/**/src/main/java/**`，仅适用于模块模式登记表中的新模块和新增/实质修改代码

Level: MUST

Source: `user-decision` + `repository-fact` (`wta-profile` implementation)

Rule: 新业务模块生产代码中的自有类、接口、枚举、record 以及显式声明的方法必须使用简明中文 Javadoc，说明职责、输入输出和边界。对外 Controller、UseCase、Service、DAO、Mapper 或跨模块合同补充 `@param`、`@return`、`@throws` 以及事务、锁、幂等、脱敏、外部调用和兼容适配等副作用说明。纯 Lombok/record 自动生成方法和语义已由接口完整说明的纯覆盖方法可以免重复注释。不得使用“处理业务”“执行操作”等无信息模板句式；注释不得替代清晰命名和正确分层。

存量 classic 模块保持既有注释风格，只有新增或实质修改的代码按 Ratchet 补齐，不以本规则要求无关全量改写。

Verification: 对新模块扫描自有 Java 类型和显式方法的 Javadoc；评审公开合同的参数、返回值、失败和副作用说明；运行模块测试与编译。
