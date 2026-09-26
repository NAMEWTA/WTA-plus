from pathlib import Path
import datetime,hashlib,json,re,shutil,subprocess
root=Path('/srv/WTA-plus');c=root/'speculo/.speculo/specdev/changes/2026-09-14-wta-plus-comprehensive-review';src=Path('/tmp/wta-t50')
base='d544f02e1627d76883e9eeaf73a8f2b05002d079';head='84ce0a9162dfc507fb4e8339575247477e57684f';tree='c118348385489be74b55233508ae0b5f40a93ee1';run_id='e0ba930d8ef45c5d'
git=lambda *x:subprocess.check_output(['git',*x],cwd=root,text=True).strip()
assert git('rev-parse','HEAD')==head and not git('status','--porcelain=v1','--untracked-files=all')
ident=dict(head=head,tree=tree,clean=True)
assert json.loads((src/'c1-static-source-after.json').read_text())==ident
checks=[*['a3-'+x for x in ['backend-default','full-package','full-bundle']],*['b1-'+x for x in ['frontend-architecture','frontend-lint','frontend-typecheck','frontend-test','frontend-build','core-package','core-bundle']],*['c1-'+x for x in ['openapi-check','skill-facts','fullstack-facts','notify-layered','fm','handbooks','diff-check']]]
for name in checks:assert json.loads((src/(name+'.json')).read_text())['exit_code']==0,name
r=json.loads((src/'runs'/run_id/'result.json').read_text());assert r['acceptance'] and r['source_before']==r['source_after']==ident and not r['cleanup']['errors']
assert {k:sum(v[k] for v in r['counts'].values()) for k in ['tests','failures','errors','skipped']}==dict(tests=135,failures=0,errors=0,skipped=0)
assert json.loads((src/'a3-backend-default-counts.json').read_text())['counts']==dict(tests=1067,failures=0,errors=0,skipped=197)
assert json.loads((src/'lead-frontend-evidence-check.json').read_text())['total_executed']==736
for name in ['source-c-security-review.md','source-c-engineering-review.md']:assert (src/name).is_file()
ticket=c/'ticket/50-reject-unimplemented-notify-modes.md';t=ticket.read_text();assert 'status: "in_progress"' in t
bindings=json.loads(re.search(r'^skill_bindings: (.+)$',t,re.M)[1]);audit=json.loads((src/'path-audit.json').read_text());assert audit['source']==head and audit['all_in_declared_scope'] and len(audit['product_paths'])==37
for b in bindings:assert hashlib.sha256((root/b['path'].removeprefix('<Path>').removesuffix('</Path>')).read_bytes()).hexdigest()==b['sha256']
e=c/'evidence/T-50-current-2026-09-23';e.mkdir()
for p in src.iterdir():
 if p.is_file() and p.suffix in ['.json','.log','.md','.py','.txt']:shutil.copy2(p,e/p.name)
for folder in ['red-reports','a2-default-reports','a3-default-reports']:shutil.copytree(src/folder,e/folder)
for run in ['35e14468040352c5','e29ae3ce94c6942c','e5e3229eb2500be7',run_id]:
 d=e/'runs'/run;d.mkdir(parents=True)
 for name in ['result.json','maven.log','xml-manifest.json']:
  p=src/'runs'/run/name;assert p.exists();shutil.copy2(p,d/name)
 shutil.copytree(src/'runs'/run/'xml',d/'xml')
for run in ['63c7980e7727c426','00926d62b78a715a']:
 d=e/'openapi-live'/run;d.mkdir(parents=True)
 for name in ['result.json','backend.log','init.log','source.json']:
  p=src/'openapi-live'/run/name;assert p.exists();shutil.copy2(p,d/name)
for bundle in ['a2-full','a3-full']:
 d=e/'artifacts'/bundle;d.mkdir(parents=True);shutil.copy2(src/'artifacts'/bundle/'manifest.json',d/'manifest.json')
for helper in ['/tmp/wta-check.py','/tmp/wta-t02-c1/record-reactor-counts.py','/tmp/wta-t38/run-notify-manual-retry-integration.py','/tmp/wta-t50-recover.py','/tmp/wta-t50-current-audit.md','/tmp/wta-t50-implementation-outline.md','/tmp/wta-t50-security-design.md']:
 p=Path(helper);assert p.exists();shutil.copy2(p,e/p.name)
ev='<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-50.md</Path>'
records=[{k:x[k] for k in ['id','phase','operation','sha256']}|dict(status='passed',evidence=[ev+' §§3–9; source equivalence, current real XML, exact capture and path audit']) for x in bindings]
gates='''- A3默认后端：257类1067项，870实际执行通过、197环境skip，0failure/error；257份fresh XML在clean前保存，环境skip不算通过。C后端tree与A3一致，按记录的输入等价复用。
- C恢复批真实八类135项，0failure/error/skip：SupportedMode19、Deadline15、Enterprise8、Wake1、Atomic66、ManualRetry13、SMS10、RedisIdempotency3。
- B前端architecture/lint/typecheck/test/build:prod均exit0；29个Vitest包99文件628项＋2个Node测试包108项＝736实际测试，0failure/skip；api-contracts的tsc-only不计测试。三App均production，330个产物逐项hash已由Lead复核。C可执行前端与B相同，仅README变化，按明确输入等价复用。
'''
for label,filename in [('A3 full','artifacts/a3-full/manifest.json'),('B core','b1-core-artifact.json')]:
 a=json.loads((src/filename).read_text());gates+='- '+label+' clean package及bundle内容校验exit0，JAR SHA256 '+a['jar']['sha256']+'，'+str(len(a['wta_libraries']))+'个WTA lib条目；不将skipTests打包当测试。C后端输入相同，manifest/实际源码/命令保留。\n'
gates+='''- A3真实full JAR OpenAPI：HTTP200、508752字节、SHA256 2c12f7d5eeea5afc906577250a928f6d2d0e5e7bbd45a6e345dd6320bf32230c；436paths/445schemas无丢失，priority required正确。正式fetch/generate/check及C再次check均exit0。
- C实际OpenAPI check、engineering/fullstack facts、Notify layered（101 Java）、FM（32模板）、handbooks及diff-check全部exit0，前后均固定clean C。生成TS900696字节、SHA256 734d43fc7b8704ad555df9a65a034f89f55c449eba12b5590107dfc731bcf0a5。
'''
body=Path('/tmp/wta-t50-evidence-body.md').read_text()
for k,v in {'HEAD':head,'TREE':tree,'PATHS':'37','RUN':run_id,'GATES':gates,'VALIDATOR':'最终SpecDev implement结构校验记录见本票原始证据目录的closure-validator文件；业务通过结论由上述真实运行及来源证据支撑。','SKILLS':'```json\n'+json.dumps(records,ensure_ascii=False,indent=2)+'\n```'}.items():body=body.replace('{{'+k+'}}',v)
assert '{{' not in body
(c/'evidence/T-50.md').write_text(body)
(c/'evidence/T-50-replan-2026-09-23.md').write_text('# T-50 当前验收入口\n\n'+ev+'。result `'+head+'`：恢复批真实135零skip且clean/cleanup通过；A3默认870执行/197skip、full/实际OpenAPI，B前端736/core经明确输入等价复用，C静态检查通过。前批三次全部保留；无生产队列处置。\n')
t=t.replace('status: "in_progress"','status: "done"',1).replace('- [ ]','- [x]');t+='\n## revision172 当前验收完成\n\nresult `'+head+'`，tree`'+tree+'`；前批3次＋经Lead复盘后的恢复批1次，全部保留。C真实135零skip与clean/cleanup、C静态通过，A3默认/full/live及B前端736/core按输入等价复用，双轴审查通过；37路径均在预登记17项写集。详见evidence/T-50.md。下一T41，Goal保持active。\n';ticket.write_text(t)
now=datetime.datetime.now(datetime.timezone.utc).isoformat();s=json.loads((c/'.status.json').read_text());w=next(x for x in s['worktrees'] if x['ticket_id']=='T-50');w.update(source_checkpoint=head,status='integrated',updated_at=now);w['integration'].update(status='passed',source_sha=head,result_sha=head,verification='passed',attempts=1,promotion_status='applied',evidence=ev)
for k in ['full_suite','e2e']:w['integration'][k].update(status='passed',evidence=ev,reason=None)
summary='Revision172: T50 accepted at84ce0a9/treec118348; prior batch3 retained and Lead-reviewed recovery batch1 real135 zeroSkip/source-clean/owned cleanup passed. A3 default870 executed/197skip/full/live and B frontend736/core reused only for identical inputs; C static+OpenAPI check passed.37 paths within17 scope entries;12done/2cancelled/36ready. NextT41; Goal active, no production queue action/deployment/archive.'
s['updated_at']=now;s['deviations'].append(summary);s['blockers']=[x.replace('remaining37','remaining36') for x in s['blockers']];(c/'.status.json').write_text(json.dumps(s,ensure_ascii=False,indent=2)+'\n')
a=json.loads((c/'plan-data.json').read_text());next(x for x in a if x['id']=='T-50').update(status='done',implementation_commit='9bb2e2888b5d2b0563beb7164c8c0a2165547e2c',result_sha=head,plan_revision=172);(c/'plan-data.json').write_text(json.dumps(a,ensure_ascii=False,indent=2)+'\n')
p=c/'tickets-map.md';m=p.read_text().replace('plan_revision: 171','plan_revision: 172',1);m='\n'.join(x.replace('| in_progress |','| done |') if x.startswith('| T-50 |') else x for x in m.split('\n'));m=re.sub(r'(?<=### 总体实施背景\n\n).*?(?=\n\n)','用户已激活Goal。当前12done、2cancelled（仅重复施工，AC保留）、36ready；T50固定C已完成。下一T41，current单writer、Lead验收；native gpt-6-sol/xhigh协作已授权。',m,count=1,flags=re.S);m=re.sub(r'(?<=## 2. 执行清单\n\n).*?(?=\n\n)','50票中12done、2cancelled、36ready；AC-001/003由T30最终复验，下一T41。Ready不代表完成。',m,count=1,flags=re.S);p.write_text(m+'\n## revision172 当前验收\n\n'+summary+'\n')
p=c/'goal-plan.md';g=p.read_text().replace('**run已激活，revision171','**run已激活，revision172',1);g=re.sub(r'(?<=### Current Status\n\n).*?(?=\n\n)','revision172；12done/2cancelled/36ready。T50最终C84ce0a9真实135零skip、clean/cleanup及静态通过，旧批3次与恢复批1次完整保留；A3/B门禁按输入等价复用。无在途产品writer，下一T41收件箱分页；Goal active。',g,count=1,flags=re.S);p.write_text(g+'\n## revision172 — T50完成\n\n'+summary+'\n')
for name in ['handoff.md','worklog.md']:
 p=c/name;p.write_text(p.read_text()+'\n## revision172 — T50完成\n\n'+summary+'\n')
p=c/'handoff.md';p.write_text(p.read_text().replace('# 恢复入口\n','# 恢复入口\n\n当前revision172：12done/2cancelled/36ready；T50最终84ce0a9已验收。无产品writer，下一T41；/tmp/wta-t41-implementation-design.md及real-environment-outline.md为只读准备。以下历史保持原时点。\n',1))
p=c/'README.md';v=p.read_text().split('\n\n');v[1]='用户已激活Goal，执行全部50票。当前revision172：12done、2cancelled（AC保留）、36ready。T50完成，下一T41；Goal active。';v[2]='最近T50 result `'+head+'`：恢复批真实135零skip、clean/cleanup与静态通过；A3默认870执行/197skip/full/live及B前端736/core按输入等价复用。旧批三次原记录保留，整个change仍未完成。';p.write_text('\n\n'.join(v))
manifest=[dict(path=str(p.relative_to(e)),sha256=hashlib.sha256(p.read_bytes()).hexdigest(),bytes=p.stat().st_size) for p in sorted(e.rglob('*')) if p.is_file()];(e/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n');print(json.dumps(dict(copied_files=len(manifest),product_paths=37,done=12)))
