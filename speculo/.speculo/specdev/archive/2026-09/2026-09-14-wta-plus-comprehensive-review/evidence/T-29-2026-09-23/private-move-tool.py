#!/usr/bin/env python3
"""Prepare and perform the T-29 private, same-parent, no-replace directory move.

CLI is fixed to /srv/WTA-plus. Never prints private names, hashes, contents, or errors.
Run `prepare`, inspect its private manifest, then run `move <backup_root>`.
"""

import ctypes
import hashlib
import json
import os
import pathlib
import re
import secrets
import shutil
import stat
import subprocess
import sys


REPO = pathlib.Path('/srv/WTA-plus')
SHARED_UID = 1000
SHARED_GID = 1000
SHARED_MODE = 0o755
OLD = 'relase'
NEW = 'release'
BACKUP_PARENT = '.t29-private-backup'
REPORT = 'namewta-deployment.md'
DIR_FLAGS = os.O_RDONLY | os.O_DIRECTORY | os.O_NOFOLLOW | os.O_CLOEXEC
FILE_FLAGS = os.O_RDONLY | os.O_NOFOLLOW | os.O_CLOEXEC
RENAME_NOREPLACE = 1


class Stop(Exception):
    """A failed private gate; details must not reach public output."""

    def __init__(self, backup_root=None, count=0):
        super().__init__()
        self.backup_root = backup_root
        self.count = count


def require(condition):
    if not condition:
        raise Stop()


def lstat_or_none(path):
    try:
        return os.lstat(path)
    except FileNotFoundError:
        return None


def private_dir(path, uid, *, exact_mode=None):
    value = os.lstat(path)
    require(stat.S_ISDIR(value.st_mode) and value.st_uid == uid)
    mode = stat.S_IMODE(value.st_mode)
    require(mode & 0o077 == 0)
    if exact_mode is not None:
        require(mode == exact_mode)
    return value


def shared_dir(path):
    """The fixed, non-private repository/temp parent is owned by the site user."""
    value = os.lstat(path)
    mode = stat.S_IMODE(value.st_mode)
    require(stat.S_ISDIR(value.st_mode) and value.st_uid == SHARED_UID and
            value.st_gid == SHARED_GID and mode == SHARED_MODE and
            mode & 0o022 == 0)
    return value


def shared_parents_unchanged(root, temp, repo_before, temp_before):
    """Recheck fixed site ownership and the same directory inodes."""
    repo = shared_dir(root)
    parent = shared_dir(temp)
    require((repo.st_dev, repo.st_ino) == (repo_before.st_dev, repo_before.st_ino) and
            (parent.st_dev, parent.st_ino) == (temp_before.st_dev, temp_before.st_ino) and
            parent.st_dev == repo.st_dev)


def git_guard(root):
    """Capture Git results; never relay tracked private paths or Git stderr."""
    for name in ('temp/relase/deployment.md', 'temp/release/deployment.md',
                 'temp/.t29-private-backup/probe/manifest.json'):
        result = subprocess.run(
            ['git', '-C', str(root), 'check-ignore', '--no-index', '-q', '--', name],
            stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=False)
        require(result.returncode == 0)
    result = subprocess.run(
        ['git', '-C', str(root), 'ls-files', '-z', '--',
         'temp/relase', 'temp/release', 'temp/.t29-private-backup'],
        stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=False)
    require(result.returncode == 0 and result.stdout == b'')


def base_guard(root, git_check):
    uid = os.geteuid()
    require(root.is_absolute())
    ancestor = pathlib.Path(root.anchor)
    for part in root.parts[1:]:
        ancestor = ancestor / part
        require(stat.S_ISDIR(os.lstat(ancestor).st_mode))
    repo = shared_dir(root)
    temp = root / 'temp'
    parent = shared_dir(temp)
    require(parent.st_dev == repo.st_dev)
    source = temp / OLD
    original = private_dir(source, uid)
    require(original.st_dev == parent.st_dev)
    require(lstat_or_none(temp / NEW) is None)
    git_check(root)
    return temp, source, original, repo, parent


def _stable(before, after):
    return (before.st_dev, before.st_ino, before.st_mode, before.st_uid,
            before.st_gid, before.st_size, before.st_mtime_ns,
            before.st_ctime_ns) == (
            after.st_dev, after.st_ino, after.st_mode, after.st_uid,
            after.st_gid, after.st_size, after.st_mtime_ns,
            after.st_ctime_ns)


def _record(common, kind, size, digest=None):
    result = {
        'type': kind, 'size': size, 'mode': stat.S_IMODE(common.st_mode),
        'mtime_ns': common.st_mtime_ns, 'uid': common.st_uid,
        'gid': common.st_gid, 'dev': common.st_dev, 'ino': common.st_ino,
    }
    if digest is not None:
        result['sha256'] = digest
    return result


def inventory(root, uid):
    """Read only regular files, anchored by no-follow directory descriptors."""
    records = {}

    def visit(directory_fd, parts, expected_dev):
        before = os.fstat(directory_fd)
        require(stat.S_ISDIR(before.st_mode) and before.st_uid == uid and
                before.st_dev == expected_dev)
        require(stat.S_IMODE(before.st_mode) & 0o077 == 0)
        entries = sorted(entry.name for entry in os.scandir(directory_fd))
        records['/'.join(parts)] = _record(before, 'directory', len(entries))
        for name in entries:
            child_parts = (*parts, name)
            key = '/'.join(child_parts)
            child = os.stat(name, dir_fd=directory_fd, follow_symlinks=False)
            require(child.st_uid == uid and child.st_dev == expected_dev and
                    stat.S_IMODE(child.st_mode) & 0o077 == 0)
            if stat.S_ISDIR(child.st_mode):
                fd = os.open(name, DIR_FLAGS, dir_fd=directory_fd)
                try:
                    require((child.st_dev, child.st_ino) == (os.fstat(fd).st_dev, os.fstat(fd).st_ino))
                    visit(fd, child_parts, expected_dev)
                finally:
                    os.close(fd)
            elif stat.S_ISREG(child.st_mode):
                require(child.st_nlink == 1)
                fd = os.open(name, FILE_FLAGS, dir_fd=directory_fd)
                try:
                    opened = os.fstat(fd)
                    require(_stable(child, opened))
                    digest = hashlib.sha256()
                    while chunk := os.read(fd, 1024 * 1024):
                        digest.update(chunk)
                    require(_stable(opened, os.fstat(fd)))
                    records[key] = _record(opened, 'file', opened.st_size, digest.hexdigest())
                finally:
                    os.close(fd)
            else:
                raise Stop()  # Symlink, FIFO, socket, device, or unknown type.
        require(_stable(before, os.fstat(directory_fd)))

    fd = os.open(root, DIR_FLAGS)
    try:
        visit(fd, (), os.fstat(fd).st_dev)
    finally:
        os.close(fd)
    return records


def _copy_dir(source_fd, target_fd, uid, source_dev, target_dev):
    original = os.fstat(source_fd)
    require(stat.S_ISDIR(original.st_mode) and original.st_uid == uid and
            original.st_dev == source_dev and os.fstat(target_fd).st_dev == target_dev)
    for name in sorted(entry.name for entry in os.scandir(source_fd)):
        source_stat = os.stat(name, dir_fd=source_fd, follow_symlinks=False)
        require(source_stat.st_uid == uid and source_stat.st_dev == source_dev and
                stat.S_IMODE(source_stat.st_mode) & 0o077 == 0)
        if stat.S_ISDIR(source_stat.st_mode):
            os.mkdir(name, 0o700, dir_fd=target_fd)
            source_child = os.open(name, DIR_FLAGS, dir_fd=source_fd)
            target_child = os.open(name, DIR_FLAGS, dir_fd=target_fd)
            try:
                require((source_stat.st_dev, source_stat.st_ino) ==
                        (os.fstat(source_child).st_dev, os.fstat(source_child).st_ino))
                _copy_dir(source_child, target_child, uid, source_dev, target_dev)
            finally:
                os.close(target_child)
                os.close(source_child)
        elif stat.S_ISREG(source_stat.st_mode):
            require(source_stat.st_nlink == 1)
            source_file = os.open(name, FILE_FLAGS, dir_fd=source_fd)
            try:
                require(_stable(source_stat, os.fstat(source_file)))
                target_file = os.open(name, os.O_WRONLY | os.O_CREAT | os.O_EXCL |
                                      os.O_NOFOLLOW | os.O_CLOEXEC, 0o600, dir_fd=target_fd)
                try:
                    while chunk := os.read(source_file, 1024 * 1024):
                        view = memoryview(chunk)
                        while view:
                            written = os.write(target_file, view)
                            require(written > 0)
                            view = view[written:]
                    require(_stable(source_stat, os.fstat(source_file)))
                    os.fchown(target_file, source_stat.st_uid, source_stat.st_gid)
                    os.fchmod(target_file, stat.S_IMODE(source_stat.st_mode))
                    os.utime(target_file, ns=(source_stat.st_atime_ns, source_stat.st_mtime_ns))
                    os.fsync(target_file)
                finally:
                    os.close(target_file)
            finally:
                os.close(source_file)
        else:
            raise Stop()
    require(_stable(original, os.fstat(source_fd)))
    os.fchown(target_fd, original.st_uid, original.st_gid)
    os.fchmod(target_fd, stat.S_IMODE(original.st_mode))
    os.utime(target_fd, ns=(original.st_atime_ns, original.st_mtime_ns))
    os.fsync(target_fd)


def copy_tree(source, target, uid):
    os.mkdir(target, 0o700)
    source_fd = os.open(source, DIR_FLAGS)
    target_fd = os.open(target, DIR_FLAGS)
    try:
        source_dev = os.fstat(source_fd).st_dev
        target_dev = os.fstat(target_fd).st_dev
        require(source_dev == target_dev)
        _copy_dir(source_fd, target_fd, uid, source_dev, target_dev)
    finally:
        os.close(target_fd)
        os.close(source_fd)


def comparable(records):
    return {name: {key: value for key, value in item.items() if key not in ('dev', 'ino')}
            for name, item in records.items()}


def private_write(path, value):
    fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL | os.O_NOFOLLOW | os.O_CLOEXEC, 0o600)
    try:
        require(stat.S_IMODE(os.fstat(fd).st_mode) == 0o600)
        content = value.encode('utf-8') if isinstance(value, str) else value
        view = memoryview(content)
        while view:
            size = os.write(fd, view)
            require(size > 0)
            view = view[size:]
        os.fsync(fd)
    finally:
        os.close(fd)
    parent_fd = os.open(path.parent, DIR_FLAGS)
    try:
        os.fsync(parent_fd)
    finally:
        os.close(parent_fd)


def private_json(path, value):
    private_write(path, json.dumps(value, ensure_ascii=True, sort_keys=True,
                                   separators=(',', ':')) + '\n')


def read_manifest(run, uid):
    path = run / 'manifest.json'
    metadata = os.lstat(path)
    require(stat.S_ISREG(metadata.st_mode) and metadata.st_uid == uid and
            stat.S_IMODE(metadata.st_mode) == 0o600 and metadata.st_nlink == 1)
    fd = os.open(path, FILE_FLAGS)
    try:
        require((os.fstat(fd).st_dev, os.fstat(fd).st_ino) == (metadata.st_dev, metadata.st_ino))
        with os.fdopen(fd, 'r', encoding='utf-8') as reader:
            fd = -1
            result = json.load(reader)
    finally:
        if fd >= 0:
            os.close(fd)
    require(result.get('version') == 1 and result.get('state') == 'prepared')
    return result


def _backup_parent(temp, uid):
    parent = temp / BACKUP_PARENT
    if lstat_or_none(parent) is None:
        os.mkdir(parent, 0o700)
        fsync_dir(temp)
    value = private_dir(parent, uid, exact_mode=0o700)
    require(value.st_dev == os.lstat(temp).st_dev)
    return parent


def fsync_dir(path):
    fd = os.open(path, DIR_FLAGS)
    try:
        os.fsync(fd)
    finally:
        os.close(fd)


def prepare(root=REPO, git_check=git_guard):
    os.umask(0o077)
    uid = os.geteuid()
    temp, source, _, repo_before, temp_before = base_guard(root, git_check)
    source_before = inventory(source, uid)
    report = source_before.get(REPORT)
    if report is not None:
        require(report['type'] == 'file' and report['mode'] == 0o600)
    file_bytes = sum(item['size'] for item in source_before.values() if item['type'] == 'file')
    require(shutil.disk_usage(temp).free >= file_bytes * 2 + 16 * 1024 * 1024)
    parent = _backup_parent(temp, uid)
    run = parent / secrets.token_hex(16)
    os.mkdir(run, 0o700)
    fsync_dir(parent)
    try:
        private_dir(run, uid, exact_mode=0o700)
        backup = run / 'backup'
        rehearsal = run / 'restore-rehearsal'
        copy_tree(source, backup, uid)
        source_after = inventory(source, uid)
        backup_data = inventory(backup, uid)
        require(source_before == source_after and comparable(source_before) == comparable(backup_data))
        copy_tree(backup, rehearsal, uid)
        rehearsal_data = inventory(rehearsal, uid)
        require(comparable(source_before) == comparable(rehearsal_data))
        manifest = {'version': 1, 'state': 'prepared', 'source': source_before,
                    'backup': backup_data, 'rehearsal': rehearsal_data}
        private_json(run / 'manifest.json', manifest)
        private_write(run / 'recovery.txt',
            'Private T-29 recovery: keep backup and rehearsal intact. Before any recovery, '
            'quiesce all writers, inspect both directory trees and their private manifest, '
            'verify labels/ownership/modes/content and obtain a new explicit recovery decision. '
            'Never overwrite either path, merge trees, delete the backup, or blindly reverse the rename.\n')
        private_json(run / 'prepared.json', {'prepared': True, 'count': len(source_before)})
        fsync_dir(parent)
        shared_parents_unchanged(root, temp, repo_before, temp_before)
    except BaseException:
        raise Stop(run, len(source_before)) from None
    return len(source_before), run


def _run_guard(temp, supplied, uid):
    parent = temp / BACKUP_PARENT
    private_dir(parent, uid, exact_mode=0o700)
    run = pathlib.Path(supplied)
    require(run.is_absolute() and run.parent == parent and
            re.fullmatch(r'[0-9a-f]{32}', run.name) is not None)
    private_dir(run, uid, exact_mode=0o700)
    for name in ('backup', 'restore-rehearsal'):
        private_dir(run / name, uid)
    marker = os.lstat(run / 'prepared.json')
    require(stat.S_ISREG(marker.st_mode) and marker.st_uid == uid and
            stat.S_IMODE(marker.st_mode) == 0o600)
    require(lstat_or_none(run / 'moving.json') is None and
            lstat_or_none(run / 'moved.json') is None)
    return run


def atomic_noreplace(temp, expected_parent):
    """Linux renameat2, both names anchored to one open parent, no fallback."""
    libc = ctypes.CDLL(None, use_errno=True)
    rename = getattr(libc, 'renameat2', None)
    require(rename is not None)
    rename.argtypes = (ctypes.c_int, ctypes.c_char_p, ctypes.c_int,
                       ctypes.c_char_p, ctypes.c_uint)
    rename.restype = ctypes.c_int
    parent_fd = os.open(temp, DIR_FLAGS)
    try:
        opened = os.fstat(parent_fd)
        require((opened.st_dev, opened.st_ino) ==
                (expected_parent.st_dev, expected_parent.st_ino))
        if rename(parent_fd, OLD.encode(), parent_fd, NEW.encode(), RENAME_NOREPLACE) != 0:
            raise OSError(ctypes.get_errno(), 'renameat2 rejected')
        os.fsync(parent_fd)
    finally:
        os.close(parent_fd)


def move(supplied, root=REPO, git_check=git_guard):
    os.umask(0o077)
    uid = os.geteuid()
    temp, source, original, repo_before, temp_before = base_guard(root, git_check)
    run = _run_guard(temp, supplied, uid)
    manifest = read_manifest(run, uid)
    source_records = inventory(source, uid)
    backup_records = inventory(run / 'backup', uid)
    rehearsal_records = inventory(run / 'restore-rehearsal', uid)
    require(source_records == manifest['source'] and
            backup_records == manifest['backup'] and
            rehearsal_records == manifest['rehearsal'] and
            comparable(source_records) == comparable(backup_records) == comparable(rehearsal_records))
    require((source_records['']['dev'], source_records['']['ino']) ==
            (original.st_dev, original.st_ino))
    shared_parents_unchanged(root, temp, repo_before, temp_before)
    git_check(root)
    require(lstat_or_none(temp / NEW) is None)
    private_json(run / 'moving.json', {'move_attempted': True, 'count': len(source_records)})
    atomic_noreplace(temp, temp_before)
    require(lstat_or_none(source) is None)
    target = temp / NEW
    target_records = inventory(target, uid)
    require(target_records == source_records)
    require(comparable(target_records) == comparable(backup_records))
    shared_parents_unchanged(root, temp, repo_before, temp_before)
    git_check(root)
    private_json(run / 'moved.json', {'moved': True, 'count': len(target_records)})
    return len(target_records), run


def main(argv):
    count = 0
    run = None
    try:
        if argv == ['prepare']:
            count, run = prepare()
        elif len(argv) == 2 and argv[0] == 'move':
            run = pathlib.Path(argv[1])
            count, run = move(run)
        else:
            raise Stop()
        success = True
        code = 0
    except Stop as failure:
        if failure.backup_root is not None:
            run = failure.backup_root
            count = failure.count
        success = False
        code = 1
    except BaseException:
        success = False
        code = 1
    backup_root = (str(run) if run is not None and run.is_absolute() and
                   run.parent == REPO / 'temp' / BACKUP_PARENT and
                   re.fullmatch(r'[0-9a-f]{32}', run.name) else None)
    print(json.dumps({'success': success, 'count': count,
                      'backup_root': backup_root, 'exit_code': code}, separators=(',', ':')))
    return code


if __name__ == '__main__':
    raise SystemExit(main(sys.argv[1:]))
