#!/usr/bin/env python3
"""Prepare a baseline-derived source, checking each changed operation against compiled Spring MVC."""
from pathlib import Path
import hashlib
import json
import re

evidence = Path(__file__).resolve().parent
root = evidence.parents[5]
revision = '0f17b6d637b1ba8015aa7adf688bdd1327467ff946341fb36be374424dd98e64'
source = root/'frontend/packages/api-contracts/openapi/revisions'/revision/'source.json'
spec = json.loads(source.read_text())
runtime = json.loads((evidence/'T-26-runtime-mappings.json').read_text())
planned = json.loads((evidence/'T-26-migration-map.json').read_text()) + json.loads((evidence/'T-26-additional-migration-map.json').read_text())
excluded = []

def normalize(path):
    return re.sub(r'\{([^}:]+):[^}]*\}', r'{\1}', path)

for row in planned:
    old, new = normalize(row['path']), normalize(row['new_path'])
    verb, newverb = row['method'].lower(), row['new_method'].lower()
    matching = [r for r in runtime if r['java'] == row['java']
                and r['controller'].split('.')[-1] == Path(row['file']).stem
                and newverb.upper() in r['methods'] and new in [normalize(x) for x in r['paths']]]
    assert len(matching) == 1, (old, new, 'not proven runtime route')
    if verb not in spec['paths'].get(old, {}):
        # The historical served bundle disabled Easy-ES. Do not fabricate its absent schemas.
        assert Path(row['file']).stem == 'EsCrudController' and old.startswith('/es/'), (old, verb)
        assert '@ConditionalOnProperty(value = "easy-es.enable", havingValue = "true")' in (root/row['file']).read_text()
        excluded.append(dict(path=old, method=verb, reason='Easy-ES conditional controller absent from source snapshot; compiled mapping migrated and tested separately'))
        continue
    operation = spec['paths'][old].pop(verb)
    if not spec['paths'][old]: del spec['paths'][old]
    assert newverb not in spec['paths'].get(new, {}), (new, newverb, 'conflict')
    spec['paths'].setdefault(new, {})[newverb] = operation
    for parameter in operation.get('parameters', []):
        if parameter.get('in') == 'path': assert '{' + parameter['name'] + '}' in new

path = '/workflow/task/getNextNodeList'
assert any(path in row['paths'] and row['methods'] == ['GET']
           and 'java.lang.Long,java.lang.String' in row['signature'] for row in runtime)
operation = spec['paths'][path].pop('post')
operation.pop('requestBody')
operation['parameters'] = [
    dict(name='taskId', **{'in': 'query'}, required=True, schema=dict(type='integer', format='int64'), description='当前任务ID；服务层校验读取权限'),
    dict(name='variables', **{'in': 'query'}, required=False, schema=dict(type='string'), description='可选JSON对象；保留嵌套值和数值/布尔类型，不接收GET正文')]
spec['paths'][path]['get'] = operation
spec['x-namewta-working-tree-source'] = dict(
    baseHead='76dbbe84a34624234e379661a57b232529e34ed3', committed=False, baselineRevision=revision,
    baselineSha256=hashlib.sha256(source.read_bytes()).hexdigest(), mappingEvidence='T-26-runtime-mappings.json',
    mappingSha256=hashlib.sha256((evidence/'T-26-runtime-mappings.json').read_bytes()).hexdigest(),
    excludedConditionalOperations=excluded,
    scope='88 method/path changes and next-node query parameters checked against compiled Spring MVC; unchanged schemas from baseline; not a complete live /v3/api-docs capture')
remaining = {(path, verb) for path, operations in spec['paths'].items()
             for verb in ['put', 'delete', 'patch'] if verb in operations}
# These are the pinned SnailAI 1.1.1 controller's protocol, not first-party CRUD aliases.
assert remaining == {('/api/snail/chat/conversations', 'put'), ('/api/snail/chat/conversations', 'delete'),
                     ('/api/snail/chat/agent/subscribe', 'delete')}, remaining
spec['x-namewta-working-tree-source']['retainedThirdPartyMethods'] = [
    dict(path=path, method=verb, owner='SnailAiChatGatewayController 1.1.1; T-26-third-party-protocols.json')
    for path, verb in sorted(remaining)]
(evidence/'T-26-openapi-input.json').write_text(json.dumps(spec, ensure_ascii=False, indent=2) + '\n')
print(json.dumps(dict(runtime_mappings=len(runtime), changed_operations=len(planned) + 1 - len(excluded),
                     conditional_operations=excluded, candidate_paths=len(spec['paths'])), ensure_ascii=False))
