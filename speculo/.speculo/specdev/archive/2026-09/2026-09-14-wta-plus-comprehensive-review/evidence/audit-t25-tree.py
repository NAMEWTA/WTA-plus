#!/usr/bin/env python3
"""Read an exported department snapshot and emit review-only, parameterized repair proposals."""
import argparse
import json
from pathlib import Path


def audit(rows):
    active = {int(row['dept_id']): row for row in rows if str(row['del_flag']) == '0'}
    if len(active) != sum(str(row['del_flag']) == '0' for row in rows):
        raise ValueError('duplicate active department IDs')
    findings = []
    for dept_id, row in sorted(active.items()):
        seen, ancestors, cursor, problem = {dept_id}, [], row.get('parent_id'), None
        while cursor is not None and int(cursor) > 0:
            parent = int(cursor)
            if parent in seen:
                problem = 'cycle'; break
            if parent not in active:
                problem = 'missing-or-deleted-parent'; break
            seen.add(parent); ancestors.append(parent); cursor = active[parent].get('parent_id')
        if problem is None and (cursor is None or int(cursor) != 0):
            problem = 'invalid-root-sentinel'
        if problem:
            findings.append(dict(dept_id=str(dept_id), problem=problem, action='manual-parent-decision-required'))
            continue
        expected = '0' + ''.join(',' + str(value) for value in reversed(ancestors))
        if expected != row.get('ancestors'):
            findings.append(dict(dept_id=str(dept_id), problem='ancestors-mismatch', expected=expected,
                action='review-only-parameterized-proposal', proposal=dict(
                    sql="update sys_dept set ancestors=? where dept_id=? and parent_id=? and ancestors <=> ? and del_flag='0'",
                    parameters=[expected, str(dept_id), str(row['parent_id']), row.get('ancestors')])) )
    return dict(mode='read-only-dry-run', active_count=len(active), finding_count=len(findings), findings=findings,
                executes_repairs=False, source='supplied snapshot only; no production connection')


def self_test():
    rows = [dict(dept_id='1', parent_id='0', ancestors='0', del_flag='0'),
            dict(dept_id='2', parent_id='1', ancestors="bad'path", del_flag='0'),
            dict(dept_id='3', parent_id='999', ancestors='0', del_flag='0'),
            dict(dept_id='4', parent_id='5', ancestors='0', del_flag='0'),
            dict(dept_id='5', parent_id='4', ancestors='0', del_flag='0')]
    before = json.dumps(rows); result = audit(rows)
    assert result['active_count'] == 5 and result['finding_count'] == 4
    assert result['findings'][0]['proposal']['parameters'] == ['0,1', '2', '1', "bad'path"]
    assert 'bad' not in result['findings'][0]['proposal']['sql']
    assert [row['problem'] for row in result['findings']] == ['ancestors-mismatch', 'missing-or-deleted-parent', 'cycle', 'cycle']
    assert json.dumps(rows) == before
    print('T-25 dry-run self-test: passed; input unchanged; no SQL executed')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--input'); parser.add_argument('--output'); parser.add_argument('--self-test', action='store_true')
    args = parser.parse_args()
    if args.self_test:
        self_test()
    else:
        if not args.input or not args.output: parser.error('--input and --output are required')
        if Path(args.input).resolve() == Path(args.output).resolve(): parser.error('output must not overwrite the input snapshot')
        report = audit(json.loads(Path(args.input).read_text()))
        Path(args.output).write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
        print(json.dumps({key: report[key] for key in ['mode', 'active_count', 'finding_count', 'executes_repairs']}))
