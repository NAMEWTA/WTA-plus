#!/usr/bin/env python3
"""Synthetic-only T29 helper test: no real repository Git or private data access."""

import ast
import os
import pathlib
import stat
import tempfile


SCRIPT = pathlib.Path('/tmp/wta-t29-private-move.py')
source_code = SCRIPT.read_text()
ast.parse(source_code)
scope = {'__name__': 't29_synthetic'}
exec(compile(source_code, str(SCRIPT), 'exec'), scope)
assert os.geteuid() == 0
Stop = scope['Stop']
fake_git = lambda root: None


def fixture():
    holder = tempfile.TemporaryDirectory(prefix='t29-synthetic-')
    root = pathlib.Path(holder.name)
    os.chown(root, 1000, 1000)
    os.chmod(root, 0o755)
    temp = root / 'temp'
    temp.mkdir()
    os.chown(temp, 1000, 1000)
    os.chmod(temp, 0o755)
    private = temp / 'relase'
    private.mkdir()
    (private / 'deployment.md').write_text('synthetic fixture only')
    os.chmod(private / 'deployment.md', 0o600)
    os.chmod(private, 0o2700)
    return holder, root, temp, private


def rejects(action):
    try:
        action()
    except Stop:
        return
    raise AssertionError('expected private gate rejection')


def identity(path):
    value = os.lstat(path)
    return (value.st_dev, value.st_ino, value.st_uid,
            value.st_gid, stat.S_IMODE(value.st_mode))


passed = []
holder, root, temp, private = fixture()
with holder:
    parent_before = [identity(root), identity(temp)]
    inode = os.lstat(private).st_ino
    count, run = scope['prepare'](root, fake_git)
    moved_count, moved_run = scope['move'](run, root, fake_git)
    assert count >= 2 and moved_count == count and moved_run == run
    assert not private.exists() and os.lstat(temp / 'release').st_ino == inode
    assert parent_before == [identity(root), identity(temp)]
    passed.append('shared-positive')

for name in ('repo', 'temp'):
    holder, root, temp, private = fixture()
    with holder:
        os.chmod(root if name == 'repo' else temp, 0o775)
        rejects(lambda: scope['prepare'](root, fake_git))
        assert not (temp / '.t29-private-backup').exists()
        passed.append(name + '-writable-negative')

for name in ('repo', 'temp'):
    holder, root, temp, private = fixture()
    with holder:
        os.chown(root if name == 'repo' else temp, 0, 0)
        rejects(lambda: scope['prepare'](root, fake_git))
        assert not (temp / '.t29-private-backup').exists()
        passed.append(name + '-owner-negative')

holder, root, temp, private = fixture()
with holder:
    _, run = scope['prepare'](root, fake_git)
    os.chown(temp, 0, 0)
    rejects(lambda: scope['move'](run, root, fake_git))
    assert private.exists() and not (temp / 'release').exists()
    passed.append('move-owner-drift-negative')

holder, root, temp, private = fixture()
with holder:
    (private / 'unsafe').symlink_to('deployment.md')
    rejects(lambda: scope['prepare'](root, fake_git))
    passed.append('source-symlink-negative')

holder, root, temp, private = fixture()
with holder:
    _, run = scope['prepare'](root, fake_git)
    (private / 'deployment.md').write_text('changed synthetic fixture')
    rejects(lambda: scope['move'](run, root, fake_git))
    assert private.exists() and not (temp / 'release').exists()
    passed.append('source-mutation-negative')

holder, root, temp, private = fixture()
with holder:
    (temp / 'release').mkdir()
    rejects(lambda: scope['prepare'](root, fake_git))
    passed.append('target-exists-negative')

print('AST: PASS')
print(f'synthetic checks: {len(passed)}/{len(passed)} passed')
for name in passed:
    print('PASS', name)
