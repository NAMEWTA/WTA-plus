#!/usr/bin/env python3
"""Classification-first owned rename on the WTA-plus prep tree.

KEEP org.dromara.{sms4j,warm,easyes,easy-es,mica} byte-level.
Owned org.dromara → org.namewta; ruoyi- → wta-; /ruoyi → /wta.
Does not touch legacy remotes. Skips speculo change identity and OpenAPI revision hashes.
"""
from __future__ import annotations

import os
import re
import sys
from pathlib import Path

KEEP_RE = re.compile(
    r"org\.dromara\.(sms4j|warm|easyes|easy-es|mica(?:\.mqtt|-mqtt)?)"
)
KEEP_TOKEN = "___KEEP_DROMARA_{}___"

SKIP_DIR_NAMES = {
    ".git",
    "node_modules",
    "target",
    "dist",
    ".idea",
    "openapi/revisions",
}

# Relative path prefixes skipped entirely (process/history or hash identity).
SKIP_PREFIXES = (
    "speculo/",
    "plus-ui-namewta/packages/api-contracts/openapi/revisions/",
)

TEXT_EXTS = {
    ".java",
    ".kt",
    ".xml",
    ".yml",
    ".yaml",
    ".properties",
    ".md",
    ".json",
    ".ftl",
    ".sql",
    ".ts",
    ".tsx",
    ".vue",
    ".js",
    ".mjs",
    ".cjs",
    ".txt",
    ".imports",
    ".factories",
    ".conf",
    ".alloy",
    ".sh",
    ".html",
    ".css",
    ".scss",
    ".sass",
    ".env",
    ".gradle",
    ".kts",
    ".toml",
    ".ini",
    ".cfg",
    ".template",
    ".svg",
}

TEXT_NAMES = {
    "Dockerfile",
    "Dockerfile.backend",
    "AGENTS.md",
    "CLAUDE.md",
    "LICENSE",
    "NOTICE",
    "Jenkinsfile",
    ".gitignore",
    ".env",
    ".env.example",
    ".env.development",
    ".env.production",
    ".env.staging",
    "AutoConfiguration.imports",
}

URL_REPLACEMENTS = [
    ("https://gitee.com/dromara/RuoYi-Vue-Plus", "https://github.com/NAMEWTA/WTA-plus"),
    ("https://gitee.com/JavaLionLi/plus-ui.git", "https://github.com/NAMEWTA/WTA-plus.git"),
    ("https://gitee.com/JavaLionLi/plus-ui", "https://github.com/NAMEWTA/WTA-plus"),
    ("http://gitee.com/dromara/RuoYi-Vue-Plus", "https://github.com/NAMEWTA/WTA-plus"),
]


def skip_path(rel: str) -> bool:
    rel = rel.replace("\\", "/")
    for prefix in SKIP_PREFIXES:
        if rel == prefix.rstrip("/") or rel.startswith(prefix):
            return True
    return False


def is_text_file(path: Path) -> bool:
    if path.name in TEXT_NAMES or path.name.startswith(".env"):
        return True
    if path.suffix.lower() in TEXT_EXTS:
        return True
    if path.name.endswith(".imports"):
        return True
    return False


def protect_keep(text: str) -> tuple[str, list[str]]:
    found: list[str] = []

    def repl(match: re.Match[str]) -> str:
        found.append(match.group(0))
        return KEEP_TOKEN.format(len(found) - 1)

    return KEEP_RE.sub(repl, text), found


def restore_keep(text: str, found: list[str]) -> str:
    for i, original in enumerate(found):
        text = text.replace(KEEP_TOKEN.format(i), original)
    return text


def rewrite_text(text: str) -> str:
    text, kept = protect_keep(text)
    text = text.replace("org.dromara", "org.namewta")
    text = restore_keep(text, kept)

    for old, new in URL_REPLACEMENTS:
        text = text.replace(old, new)

    # Prefix SWAP (modules, artifacts, files, data-ids).
    text = text.replace("ruoyi-", "wta-")
    text = text.replace("RuoYi-Vue-Plus", "WTA-Plus")
    text = text.replace("RuoYi-Vue", "WTA")
    text = text.replace("RuoYi", "WTA")
    text = text.replace("RUOYI", "WTA")
    # Owned container / log paths that are not prefix-form.
    text = text.replace("/ruoyi", "/wta")
    text = text.replace("var/log/ruoyi", "var/log/wta")

    # Remaining standalone owned token (usernames, buckets, comments).
    # Do not touch already-rewritten wta- or KEEP org.dromara.* (restored).
    text = re.sub(r"\bruoyi\b", "wta", text)
    text = re.sub(r"\bRuoyi\b", "Wta", text)
    return text


def iter_files(root: Path):
    for dirpath, dirnames, filenames in os.walk(root):
        rel_dir = os.path.relpath(dirpath, root).replace("\\", "/")
        if rel_dir == ".":
            rel_dir = ""
        dirnames[:] = [
            d
            for d in dirnames
            if d not in SKIP_DIR_NAMES
            and not skip_path(f"{rel_dir}/{d}/".lstrip("./").replace("//", "/"))
        ]
        for name in filenames:
            path = Path(dirpath) / name
            rel = str(path.relative_to(root)).replace("\\", "/")
            if skip_path(rel):
                continue
            yield path, rel


def rewrite_contents(root: Path) -> tuple[int, int]:
    changed = 0
    scanned = 0
    for path, rel in iter_files(root):
        if not is_text_file(path):
            continue
        scanned += 1
        try:
            original = path.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            continue
        updated = rewrite_text(original)
        if updated != original:
            path.write_text(updated, encoding="utf-8")
            changed += 1
    return scanned, changed


def move_org_dromara_dirs(root: Path) -> int:
    moved = 0
    dromara_dirs = []
    for dirpath, dirnames, _ in os.walk(root, topdown=False):
        rel_dir = os.path.relpath(dirpath, root).replace("\\", "/")
        if skip_path(rel_dir + "/"):
            continue
        base = Path(dirpath)
        if base.name == "dromara" and base.parent.name == "org":
            dromara_dirs.append(base)
    # deepest first
    dromara_dirs.sort(key=lambda p: len(p.parts), reverse=True)
    for src in dromara_dirs:
        dest = src.parent / "namewta"
        if dest.exists():
            for child in src.iterdir():
                target = dest / child.name
                if target.exists():
                    raise SystemExit(f"collision moving {child} -> {target}")
                child.rename(target)
            src.rmdir()
        else:
            src.rename(dest)
        moved += 1
    return moved


def rename_ruoyi_paths(root: Path) -> int:
    renamed = 0
    entries = []
    for dirpath, dirnames, filenames in os.walk(root, topdown=False):
        rel_dir = os.path.relpath(dirpath, root).replace("\\", "/")
        if skip_path(rel_dir + "/"):
            continue
        base = Path(dirpath)
        for name in list(dirnames) + list(filenames):
            if name.startswith("ruoyi-") or name.startswith("RuoYi"):
                entries.append(base / name)
    entries.sort(key=lambda p: len(p.parts), reverse=True)
    for src in entries:
        name = src.name
        if name.startswith("ruoyi-"):
            new_name = "wta-" + name[len("ruoyi-") :]
        elif name.startswith("RuoYi"):
            new_name = "WTA" + name[len("RuoYi") :]
        else:
            continue
        dest = src.with_name(new_name)
        if dest.exists():
            raise SystemExit(f"collision renaming {src} -> {dest}")
        src.rename(dest)
        renamed += 1
    return renamed


def main() -> int:
    if len(sys.argv) != 2:
        print("usage: rename_owned.py <WTA-plus-root>", file=sys.stderr)
        return 2
    root = Path(sys.argv[1]).resolve()
    if not root.is_dir():
        print(f"not a directory: {root}", file=sys.stderr)
        return 2
    scanned, changed = rewrite_contents(root)
    moved = move_org_dromara_dirs(root)
    renamed = rename_ruoyi_paths(root)
    print(f"content_scanned={scanned}")
    print(f"content_changed={changed}")
    print(f"org_dromara_dirs_moved={moved}")
    print(f"ruoyi_paths_renamed={renamed}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
