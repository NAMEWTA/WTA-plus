# T-29 本地文档审查

Revision116；本地可逆文档收敛及父级补修完成，状态 review。所有实现仍未暂存、提交、推送或部署；implementation_commit/result_sha/candidate 均为空，不标 Done。

## 逐文件裁决与规则承接

41 份 generic AGENTS 的当前全文已复核：仅替换标题模块名、Scope 中模块路径及旧编译命令后，正文 SHA-256 全部相同（a70d288109e032df6196c35d98f5113f9d9613445ce6a6ec6e636592aa5bd1d2）。不是按 31 行或文件名推断可删除。37 个叶手册在删除前扫描当前文档/脚本/配置直接路径及相对 Markdown 引用，结果为零；先重写 backend/common/modules/extend 四个父导航，再移除叶文件。

唯一共同 MUST「跨模块能力必须使用项目公开 API 或 SPI」保留在 backend/AGENTS.md，由各子模块继承；原 POM/源码/资源/测试导航改由真实父入口和模块地图承接。四个聚合入口不再声称自己有 src/main；BOM 不冒充业务实现或测试。未为被删除文件另建 37 个同义 README。

8 份 special 手册全部保留：System/SSO 字节不变，其余仅修正可证路径和命令 cwd。原有 28 条独立硬约束逐行仍存在；Profile 子域隔离、System/OSS 权限与生命周期、SSO Cookie/Client/PKCE、Third 凭据/URI/日志边界均未削弱。每个路径、原 SHA、历史 SHA 对照、处置、owner 与承接位置见 T-29-agents-dispositions.json/md；完整原文与可回退补丁见 T-29-originals.json、T-29-owned-diff.patch。

## 事实与当前文档

项目画像提供基于真实工作树的盘点命令；模块地图与 49 个子 POM 逐项一致，并反映实际测试根，补入 wta-common-richtext。wta-third layered 模式已在上游修正，核对现有源码/登记后保持，无重复改写。实际库存为 50 个 POM、3 个 App package、279 个 src/test/java Java 文件，其中 242 个当前跟踪文件、37 个未跟踪文件；排除 target，数量不是 JUnit 用例数，也不是固定门禁阈值。

15 份当前文档校正 monorepo 工作区、三 App 构建/预览/发布清单/部署的区别，指向真实 apps.json 与发布 manifest。移除不存在 plan/update 的 current 权威引用，取消必须存在 .vscode/settings.json 的旧描述；CI 明确为仓内候选，未称远程合并门禁已启用。默认 Admin 浏览器与 SSO 专用矩阵明确区分，不再等待“第二 App 激活”。去除已删除浏览器共享响应加密的能力描述；机器 HMAC、存储签名、数据库加密仍保持各自合同。

根 README 修正从 frontend 切换 backend 的 cwd 和产品数据 50/Nacos 60 的归属；保留所有图片并明确历史预览性质。Nacos 现行 Data ID/单读规则汇入发布说明，旧 active change 链接移除但历史文件不改；六文件基座、已有库不得重放、生产批准及 OSS 双桶安全/迁移/回滚规则保留。原许可证、根 AGENTS/CLAUDE、历史 archive、永久 ADR/context 和图片合计 855 个保护文件 SHA 全部不变。

## 实际验证

所有最终命令 exit 0，完整 argv/cwd/日志见 JSON。

| 证据 | 实际结果 |
|---|---|
| T-29-audit-final | 134 个本地 Markdown 链接/锚点通过；49 个 Maven 模块行与真实 POM、测试根一致；删除手册现行引用 0；28 条特殊硬约束保留；855 保护文件不变；三个库存 rg 命令均 exit 0 |
| T-29-verification | 八条串行命令全通过：工程事实（5 Skill/76 引用）、fullstack（19 文件/16 模板）、docs/fm（32 模板）、开发构建锁/JAR保护、十个真实 -pl 选择器的 Maven validate、frontend architecture（35 workspace/0违例）、正式 OpenAPI check、git diff --check |
| T-29-release-final | 仓根 bash release-artifacts/scripts/verify-release.sh；116/116 Node 发布合同，零 skip，Shell/Python 语法、三 App 配套、Nginx台账及四类 Compose 解析均通过 |

Maven validate 只证明选择器/reactor 可解析，不算业务测试或重新编译。文档中的完整构建、真实外部服务、部署、初始化、晋升、迁移和恢复示例按实际脚本/参数/cwd解析；本票没有执行这些有环境前置或外部副作用的运维动作，也未把历史构建结果冒充本票新测试。没有产品 Java/TS/SQL 改动，故未重复业务全量测试或浏览器运行。E2E 按票 not-required；前票行为证据保持各自边界，T-30 负责最终组合验收。

首轮通用链接审计发现我对三处 Profile 相对 Skill 链接多增加了一层 ../；原链接本来正确，已恢复，最终 134 链接零缺失。该失败诊断保留于 T-29-links-first.json，不掩盖为上游缺陷。普通 code span 的模块相对路径、构建输出、运维输入、否定示例和协议/字段记号单列人工分类；没有把 /sso 等 HTTP 路径伪装成文件引用检查。

## Skill Execution Records

| Skill / phase / operation | 实际操作及结果 |
|---|---|
| engineering-standards / implement / apply-scope-contract | 逐文件原文/SHA 比对与父 owner 收敛，保留 MUST/权限/事务/许可证/供应商/永久知识，按真实 POM、package、App清单和源码修正文档；passed |
| namewta-fullstack-development / implement / synchronize-existing-package-navigation | 回读包索引与当前35个package，修正两包无手册的事实，保留父级边界与本地索引同步要求；passed |
| wta-module-guide / implement / synchronize-module-navigation-facts | 回读实际聚合/特殊手册，补修入口继承导航，跨模块 API/SPI 与架构硬约束保留；13票 Skill 绑定显式更新，144链接检查通过；passed |
| engineering-standards / verify / verify-affected-contract-and-quality-gates | 链接/锚点、命令选择器、模块/测试根、保护哈希、事实/模板/构建保护、OpenAPI/架构与116发布合同全部实际验证，明确未运行运维动作；passed |

T-29-checkpoint.json 登记 62 条产品文档路径（25 修改、37 删除）；5 条上游重叠记录前后 SHA，523 条上游非重叠不变，累计 585 条最新路径。HEAD 保持 76dbbe84a34624234e379661a57b232529e34ed3，index 为空。治理结果另见 T-29-governance.json。T-03/T-23 未知及提交暂缓仍有效，下一步仅能推进 T-30 的可逆准备和已具备条件的验收。

## 父级补修（Revision109–110）

初次文档审计遗漏父Skill要求每个POM/package都建AGENTS的旧指导，以及review-and-delivery的plan/update来源。先扩展两条写集，再回读并修正文档；DELIVERY-001 Rule及其他既有硬约束原文不变。新增“收敛手册必须先承接独有硬约束并校验引用”，与本次逐文件处理一致。13票绑定经显式差异审查更新，历史验证摘要保持不动。

T-29-supplement-originals.json / owned-diff.patch记录两文件原文和补丁；supplement-audit.json验证144链接、49模块、28特殊规则、855保护文件及零删除引用，exit 0；supplement-verification.json两条facts/fullstack命令exit 0。产品代码与发布脚本未变，不重复构建和发布测试。最新检查点为T-29-checkpoint-v2.json：64产品路径（27修改、37删除），5重叠/523非重叠不变，累计587；原revision108证据保留。治理见T-29-supplement-governance.json。

## 后续回读补修（Revision111–112）

T-30准备按需回读时，继续发现testing/architecture两份规则引用缺失plan/update，以及frontend naming声称每包都有AGENTS；实际35个包中validation和rich-text继承父导航。先登记三条写集，再修正来源、人工矩阵落点和已有索引事实；Rule行及其他MUST原文保持。全Skill搜索不再引用plan/update。

四条followup-verification命令全部exit 0，审计146链接、49模块、28特殊规则、855保护哈希及零删除引用；全文原文/补丁、日志与最新T-29-checkpoint-v3.json齐备。67路径（30修改/37删除），6重叠/522非重叠，累计589；v1/v2是历史快照，恢复/下游读取v3。治理见T-29-followup-governance.json。

## 手册检查器补修（Revision114–116）

上一轮只复核手册与父Skill，遗漏原verify-agent-handbooks脚本。一manifest一手册/七标题模板规则已与批准的收敛方案冲突，实际运行exit 1，保留T-29-handbooks-red证据。先扩展精确写集，再改为每个manifest必须找到产品目录内最近有效手册，保留中文导读与禁止嵌套CLAUDE约束，新增本地inline链接落点检查；不声称脚本能验证完整Markdown/锚点或独有业务硬约束。后者仍由逐文件原文与28条规则对照承担。

8个正负回归覆盖继承/特殊owner、根导航缺失、损坏/越界/非法编码链接、空白路径、中文标题、CLAUDE副本、零输入与构建目录隔离、CLI退出码。实际扫描50后端/35前端manifest全部分配12/33手册；发现Third两份既有英文手册缺少中文标题，先扩写集再补标题，原边界正文未变。两条命令接入CI候选并同步scripts导航、项目画像与前端父导航。apply_patch最初因同一patch删除/新增同路径被工具拒绝，未写入文件；改为单次替换后执行验证。

T-29-handbooks-first保留首轮真实目录失败；final五条命令全部exit 0，其中8新增+42既有检查器回归50/50零skip。T-29-checker-release重新运行116/116发布合同零skip及语法/台账/Compose校验；checker-audit核对146链接、49模块、28规则与855保护文件。最新T-29-checkpoint-v4为72路径，521非重叠不变/7重叠，累计593。前述v1/v2/v3均保留为历史，不再作为下游最新输入。治理见T-29-checker-governance.json。所有提交继续暂缓。

## 实施历史

# T-29 实施记录

Revision107：T-29开始，528条最新上游哈希全部核对。逐份复核41 generic与8 special手册的当前全文/SHA；仅在硬约束与导航承接、引用迁移后清理，保留许可证/历史/永久知识。27review/1in_progress/1ready/2blocked/0Done；全部提交暂缓。

完成标准：逐文件删除/保留裁决与SHA、硬约束去向明确；当前源码事实和引用/cwd命令核对；原有保护文件未变；事实/模板/治理门禁通过。本地完成后review，未提交不标Done。
