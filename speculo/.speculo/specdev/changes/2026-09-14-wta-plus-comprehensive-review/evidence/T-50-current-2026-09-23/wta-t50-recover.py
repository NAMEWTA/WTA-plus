from pathlib import Path
import datetime,json,re,subprocess,shutil
root=Path('/srv/WTA-plus');c=root/'speculo/.speculo/specdev/changes/2026-09-14-wta-plus-comprehensive-review';out=Path('/tmp/wta-t50')
git=lambda *x:subprocess.check_output(['git',*x],cwd=root,text=True).strip()
head='50757f9b872b0b9b0131e4b2a4538bff7473f74d';assert git('rev-parse','HEAD')==head and not git('status','--porcelain=v1','--untracked-files=all')
r=json.loads((out/'runs/e5e3229eb2500be7/result.json').read_text());assert r['maven_exit_code']==0 and not r['acceptance'] and r['cleanup']['errors']==['source_after_not_clean_exact_head']
for name in ['frontend-architecture','frontend-lint','frontend-typecheck','frontend-test','frontend-build','core-package','core-bundle','skill-facts','fullstack-facts','notify-layered','fm','handbooks']:assert json.loads((out/('b1-'+name+'.json')).read_text())['exit_code']==0,name
s=json.loads((c/'.status.json').read_text());w=next(x for x in s['worktrees'] if x['ticket_id']=='T-50');now=datetime.datetime.now(datetime.timezone.utc).isoformat();w.update(status='blocked',source_checkpoint=head,updated_at=now);w['integration'].update(status='failed',source_sha=head,verification='failed',attempts=3);s['updated_at']=now
(c/'.status.json').write_text(json.dumps(s,ensure_ascii=False,indent=2)+'\n')
p=c/'ticket/50-reject-unimplemented-notify-modes.md';t=p.read_text().replace('status: "in_progress"','status: "blocked"',1);p.write_text(t)
(c/'evidence/T-50-attempt-limit-state.json').write_text(json.dumps({'recorded_at':now,'ticket_status':'blocked','workspace':w,'prior_runs':['35e14468040352c5','e29ae3ce94c6942c','e5e3229eb2500be7'],'meaning':'Actual Lead transition before recovery; preserve prior batch attempts=3'},ensure_ascii=False,indent=2)+'\n')
body='''# T-50 — 三次尝试复盘与恢复

## Failure History And Lead Recovery

| 轮次 | 固定源码/运行 | 事实 | 下一轮改变 |
|---|---|---|---|
| 1 | A1 9bb2e28 / 35e14468040352c5 | 135项1failure/0error/skip；省略primitive priority的新增测试错误期待200 | 保留enum默认正例显式传0；新增priority缺失400及五表零增 |
| 2 | A2 1dfdcbc / e29ae3ce94c6942c | 135项零fail/error/skip、clean/cleanup通过；实际OpenAPI仍未标priority必填 | A3 0b33ff3用既有JsonProperty声明并补schema断言；重新default/full和真实capture，正式生成B |
| 3 | B 50757f9 / e5e3229eb2500be7 | Maven0、135项零fail/error/skip，但source-after未clean，acceptance=false | 停止并行前端build与exact-clean验收，所有构建先结束，再独占串行真实运行 |

- **共同失败模式：** 各次原因不同：新增测试假设、真实合同声明、验收调度环境；不是忽略同一业务断言重复重试。三次尚未完成完整票验收，按有效Goal上限进入Lead复盘。
- **最可能原因：** 第3次检查15:09:43.665 UTC被前端build15:08:27.990—15:09:52.777覆盖。Vite插件会非原子重写跟踪的auto-imports.d.ts/components.d.ts，文件mtime15:09:42.731/15:09:46.739夹住检查，最终hash与HEAD相同。高置信为瞬时写入，但驱动未保留dirty路径，不能声称唯一原因；不回填source_after、不覆写原result。
- **下一轮具体改变：** 完成全部前后端构建后取得唯一验收锁；先修正API README过期来源说明（硬写集目录内的文档），与本次恢复记录提交为新clean checkpoint。新checkpoint与B的backend、生成物和前端可执行输入逐项等价，仅README/本change治理变化，旧门禁仅在该精确输入边界复用；新checkpoint仍以相同冻结v2驱动串行执行全部八类，source before/after必须同HEAD/tree且全仓clean，135项零skip及owned清理全部满足。验证期间不启动任何构建、生成器或writer。不放宽门禁，不改变业务断言、测试选择器或资源清理。
- **下一 owner/路由：** 返回当前Goal Lead，自行接管README/治理并独占所有真实验收；原implementation代理停止产品写入。两只读轴核恢复和最终证据。无新worktree/部署/生产数据动作。

## 状态与尝试计数

Lead先将Ticket/workspace记blocked并保存T-50-attempt-limit-state.json（上一批attempts=3），本复盘落盘后才生成dispatch-T-50-20260923-02.md并将新批次attempts重置0。后续Evidence必须同时报告前批3次和恢复批实际次数，绝不把旧失败删除或将计数称为总次数。此为I-implement implementation-procedure重复失败门及P-goal-plan/lead-orchestration的Lead恢复，不请求用户重新授权已授权本地工作。

原始result、sanitized Maven日志、8份XML/manifest及frontend/core/static命令记录保存在/tmp/wta-t50，将在最终证据目录按原字节归档。第3次retain脚本因缺source_after正确拒绝；Lead在下一次clean到达admin前手动保留八份fresh XML，仅证明断言执行，不提升acceptance。
'''
(c/'evidence/T-50-recovery-2026-09-23.md').write_text(body)
packet='''# T-50 Lead Dispatch 02 — 串行验收恢复

依据 evidence/T-50-recovery-2026-09-23.md 的四项Lead复盘及已保存attempt-limit状态。相较第一派单，本轮不退回原writer重复修业务：Lead仅同步 frontend/packages/api-contracts/README.md 的过期来源（已被Ticket整目录写集覆盖），提交后拥有唯一验收锁；所有前端构建/生成器和后端打包先结束，真实八类严格串行运行。只读代理不写产品或执行构建/服务。

新clean checkpoint实际SHA由提交后记录。与A3/B的backend及frontend可执行输入逐项比较，旧默认/full/capture/frontend/core只按相同输入复用，不冒称新HEAD实跑；新HEAD的真实八类、静态合同、OpenAPI check、before/after-clean与资源清理由Lead实际执行。冻结v2驱动及所有断言不变，failed/skip/source漂移均保持失败；若再次失败，先保留证据并按具体新原因定位，禁止自动重复。恢复批attempts从0开始，前批3次全部保留。无远程发布、真实数据操作、归档授权。
'''
(c/'evidence/dispatch-T-50-20260923-02.md').write_text(packet)
# Reset only after the recovery decision and materially changed dispatch have been saved.
w.update(status='active',updated_at=now);w['integration'].update(status='pending',verification='pending',attempts=0)
summary='Revision171: T50 prior batch3 attempts retained; B real135 assertions passed but exact-clean gate failed during overlapping Vite build. Lead review and new Dispatch02 saved before reset0; serial-only recovery, no relaxed checks. API README provenance correction is within existing directory scope. 11done/2cancelled/T50in_progress/36ready; Goal active.'
s['deviations'].append(summary);(c/'.status.json').write_text(json.dumps(s,ensure_ascii=False,indent=2)+'\n')
t=t.replace('status: "blocked"','status: "in_progress"',1);p.write_text(t+'\n## revision171 Lead恢复派单\n\n'+summary+'\n详见evidence/T-50-recovery-2026-09-23.md与dispatch-T-50-20260923-02.md。\n')
a=json.loads((c/'plan-data.json').read_text());next(x for x in a if x['id']=='T-50').update(plan_revision=171);(c/'plan-data.json').write_text(json.dumps(a,ensure_ascii=False,indent=2)+'\n')
p=c/'tickets-map.md';p.write_text(p.read_text().replace('plan_revision: 170','plan_revision: 171',1)+'\n## revision171 T50恢复\n\n'+summary+'\n')
p=c/'goal-plan.md';g=p.read_text().replace('**run已激活，revision170','**run已激活，revision171',1);g=re.sub(r'(?<=### Current Status\n\n).*?(?=\n\n)','revision171；11done/2cancelled/T50 in_progress/36ready。前批三次尝试保留，Lead复盘后Dispatch02要求构建结束、独占串行真实验收，新批attempts0。待README来源修正与恢复记录提交成新clean checkpoint。Goal active。',g,count=1,flags=re.S);p.write_text(g+'\n## revision171 — T50恢复\n\n'+summary+'\n')
for name in ['worklog.md','handoff.md']:
 p=c/name;p.write_text(p.read_text()+'\n## revision171 — T50恢复\n\n'+summary+'\n')
p=c/'README.md';v=p.read_text().split('\n\n');v[1]='用户已激活Goal。当前revision171：11done、2cancelled、T50 in_progress、36ready。T50三次尝试后已完成Lead复盘并转独占串行验收，尚未关闭。';p.write_text('\n\n'.join(v))
p=root/'frontend/packages/api-contracts/README.md';v=p.read_text();start=v.index('当前快照来自已提交后端');end=v.index('\n\n',start);v=v[:start]+'当前版本由 `openapi/current.json` 指向；对应 `openapi/revisions/<revision>/provenance.json` 保存后端提交、运行端点与源文档摘要，`source.json` 保留该次真实 full JAR 的完整 `/v3/api-docs` 原文。以这些文件为唯一来源，不在本说明中重复固定提交号或路径/schema数量。历史 revisions 保持不可变。\n\n通知提交当前仅支持 ALL、ASYNC 和 priority=0；HTTP JSON 必须显式传入 `priority`，生成类型也将其标为必填。历史策略枚举可读取，但不代表新提交支持对应编排；`RetryReceipt.queuedCount` 是整数。'+v[end:];p.write_text(v)
print('revision171 recovery and actual README correction prepared; no acceptance claimed')
