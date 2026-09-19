# Archive and Consolidate Dry-Run Report

生成：2026-09-19T14:27:41.971042+00:00；Workflow specdev；archive-single；generic；状态 dry-run，尚未移动或写永久知识，等待本计划确认。

## 已完成的授权动作

用户本轮明确要求关闭已完成Issue。NAMEWTA/WTA-plus #1/#2经准确目标/评论全文/reason展示、幂等comment-close与远程重读，均CLOSED，各1条完成marker；#3仍OPEN且正文、评论、更新时间未变化。回执位于本change evidence/reconcile.json。无公开commit/PR，未push、部署、创建票级Issue。

本地完成首先在干净104a6d3上重验，complete exit0/0errors/0warnings。历史3项accepted/resolved deviations已保留到LOG-016并清空活跃偏差数组。产品result保持723e851，三票done，Goal/Map completed。以上为独立已授权reconcile，本报告dry-run阶段没有执行归档/永久知识写入。

## Path Context

| Key | Project-relative path |
|---|---|
| project_root | 本仓库根 |
| workflow_root | speculo/workflows/specdev |
| state_root | speculo/.speculo/specdev |
| changes_root | speculo/.speculo/specdev/changes |
| archive_root | speculo/.speculo/specdev/archive |
| commands_root | speculo/.speculo/commands |
| commands_def_root（非报告根） | speculo/commands |
| knowledge stores | speculo/.speculo/specdev/adr、context、research |

roots来自workspace.json，按嵌套安装的权威解析；未采用旧报告或入口示例的扁平安装路径。所有目标真实路径在声明根内，无符号链接逃逸。

## 预检与顺序

源存在；目标不存在；全局active只有本change唯一条目，archived尚无；另两个active保持原样。无未完成父Implementation Map/Plan归属；current/direct-parent三票已集成，无待清理worktree。知识74ADR、12context、research占位均扫描，无本scope未闭合锁/事务/promotion/recovery；旧4份归档报告已executed/verified。

聚合source为conversation，external_action=not-applicable保持；逐条#1/#2关闭门以issue-index与reconcile回执核对；#3已转移，不挡本change归档。publish_action=not-requested。

执行顺序：复验本报告指纹与远程状态 → 设置本change的A工作状态 → 原子rename移动 → 更新归档.status/global索引 → 新建知识文件 → 回读文件与索引、运行归档complete及包级self-check → 按既有本地提交授权提交治理变动 → 含--repo的complete与clean最终复核 → 报告补遗。期间失败保存检查点，不覆盖既有目标，不回滚已观察的远程完成事实。

## 归档移动与状态计划

| 来源 | 目标 | 动作/理由/风险 |
|---|---|---|
| speculo/.speculo/specdev/changes/2026-09-19-remote-issues-phone-ai | speculo/.speculo/specdev/archive/2026-09/2026-09-19-remote-issues-phone-ai | 1个completed change整体原子rename；归档历史完整保留；目录移动需确认 |
| speculo/.speculo/specdev/status.json | 同文件 | 仅active移除本change、archived去重追加其名称；其余索引原样 |
| 移动后的.status.json | 同位置 | change_status=archived、archived=true、archive_path为匹配目标的rooted Path标签；current_work=null；works_run追加specdev/archive-and-consolidate；保留completed_at及三票result |

移动前49个文件，其中.status之外全部按相对路径及字节hash核对，归档后只对.status作终态更新，其他历史文件不改写。历史内部active路径是原执行时定位，产品及脚本复现以记录的Git检查点为准，不把归档历史当作新的运行工作区。

## 知识毕业计划（1文件、2术语）

来源：当前change CONTEXT.md、对应POM、T-02/T-03验收。符合接手者必知，无现有同名术语。新建 speculo/.speculo/specdev/context/ai-placeholder-terms.md，内容如下：

```markdown
# AI Maven 占位模块术语

- Status: Current
- Graduated: 2026-09-19
- Source change: 2026-09-19-remote-issues-phone-ai
- Source: 归档后同change的CONTEXT.md与evidence/T-03.md（使用rooted Path标签）
- Verified product result: 723e8514cbaeba13094415ba5f1071de31b2241c

**AI 业务占位模块**：wta-ai保留Maven构建身份与业务边界，仅依赖wta-common-ai，当前不提供聊天、模型、知识库、Snail AI用户注册或其他AI服务接入。
_Avoid_: cde-ai（来源旧称，不是重命名要求）

**AI 公共占位模块**：wta-common-ai保留Maven构建身份，无生产依赖、源码、Snail AI starter、自动配置或替代模型SDK。
_Avoid_: cde-common-ai（来源旧称）
```

与前端“占位目录”不同：这里是可构建Maven artifact，不改原术语。知识写入后重读确认证据和owner。

## 跳过与清理结论

- 新ADR：0。手机号写入兼容是已验收行为合同，未满足ADR三要素，留LOG/Spec/Evidence；不伪造新的架构决定。
- 新research：0；调试、临时fixture和中间构建细节随归档保存。
- 六SQL已有ADR-0042及mysql-release-baseline-terms，不复制。
- 删除/合并/改写既有知识：0；87个既有知识文件全部保持字节不变。
- ADR-0014中的历史gen/ai前端枚举与当前结构不同，列为独立待裁决审计项，当前保留；本计划不请求或执行其改写。active综合审查仍引用它，不扩大到其他change。
- ADR-0017 superseded但不足30天且被active引用，keep；research/.gitkeep保持。

## 归档校验器预检修复

实际发现A要求归档后--stage complete，但原校验器只接受completed并拒绝archived。已由独立有界实施者仅修改validate-specdev.mjs及其原测试文件，以真实临时Git仓库red/green修复普通单change归档：合法路径/精确metadata通过，假终态、未完成票、伪SHA、缺集成和dirty仍失败，历史治理提交不能因移动冒充实现。没有修改业务源码或父实现归档成员发现逻辑。原35场景保留，新增13场景；最终48pass/0skip，exit0；self-check 0errors/0warnings，exit0。初轮red为39pass/8fail；另加实际月份与名称不匹配负向先red1，再全部green。Lead回读diff并补月份边界，未删断言。当前A恢复键已登记，等待本计划确认。

## 计划指纹

- source（排除必须变更的.status）SHA256：6add4d4c231ae9300e978160cc7fb50d6ac2391b041e15fa522fe0670a96b0d1
- source .status SHA256：b14ab9f0b7869bd9dcebffa0443c15ef26568157b93598e4e19e0715a63db7e9
- global status SHA256：5eeb46c1a7c2d1da7215f5293c179f2802b8b2b39ef7c69d068ea6894da22efc
- 87个知识文件的有序清单SHA256：938263be59361dd1cf543a5db49505eced466d922e18efccf81c1c197495f98a

确认后重验；合法计划内工作状态/终态更新单独记录，其余漂移停止。远程若出现新内容，先重读并说明，marker防止重复评论。

## 确认边界

归档移动1个change、新建1文件/2术语、既有知识清理0。本报告只准备具体计划，不执行目录移动或永久知识写入。按archive-and-consolidate入口“预执行完整计划 → 用户显式确认 → 执行”，需用户确认这份计划后才进入confirmed。确认不包含push/部署、关闭#3或改写其他change。
