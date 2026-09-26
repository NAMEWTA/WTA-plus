#!/usr/bin/env python3
"""Retain/check the eight exact T-50 Surefire XMLs; never run tests or overwrite evidence."""

import argparse
import datetime as dt
import hashlib
import json
import os
from pathlib import Path
import shutil
import sys
import xml.etree.ElementTree as ET


CLASSES = (
    "org.namewta.test.notify.NotifyDeadlineIntegrationTest",
    "org.namewta.test.notify.EnterpriseQueuedNotificationIntegrationTest",
    "org.namewta.test.notify.NotifyWakeIntegrationTest",
    "org.namewta.test.notify.NotifySupportedModeIntegrationTest",
    "org.namewta.test.notify.NotifyAtomicResultIntegrationTest",
    "org.namewta.test.notify.NotifyManualRetryIntegrationTest",
    "org.namewta.test.notify.NotifySmsDispatchIntegrationTest",
    "org.namewta.test.notify.idempotency.RedisNotifyIdempotencyStoreIntegrationTest",
)
FIELDS = ("tests", "failures", "errors", "skipped")


def require(ok, message):
    if not ok:
        raise ValueError(message)


def timestamp(value):
    instant = dt.datetime.fromisoformat(value)
    require(instant.tzinfo is not None, "result timestamp lacks timezone")
    return instant.timestamp()


def digest(data):
    return hashlib.sha256(data).hexdigest()


def inspect_xml(file, classname, started, finished):
    require(file.is_file() and not file.is_symlink(), "missing/nonregular retained XML: " + file.name)
    stat = file.stat()
    require(started <= stat.st_mtime <= finished, "stale/out-of-window XML: " + file.name)
    data = file.read_bytes()
    root = ET.fromstring(data)
    require(root.tag == "testsuite" and root.attrib.get("name") == classname,
            "wrong Surefire class: " + file.name)
    counts = {key: int(root.attrib.get(key, "0")) for key in FIELDS}
    require(counts["tests"] > 0 and len(root.findall("testcase")) == counts["tests"],
            "zero/incomplete testcase list: " + file.name)
    return {"file": "xml/" + file.name, "sha256": digest(data), "counts": counts,
            "mtime_ns": stat.st_mtime_ns, "bytes": len(data)}


def verify(run, expected_head, repo):
    require(run.is_dir() and not run.is_symlink(), "run must be a real directory")
    report_file = run / "result.json"
    require(report_file.is_file() and not report_file.is_symlink(), "missing result.json")
    report = json.loads(report_file.read_text())
    before, after = report.get("source_before"), report.get("source_after")
    require(isinstance(before, dict) and before == after and before.get("clean") is True,
            "source identity was not clean/equal before and after")
    require(before.get("head") == expected_head and len(expected_head) == 40,
            "source HEAD differs from explicit expected HEAD")
    require(isinstance(before.get("tree"), str) and len(before["tree"]) == 40,
            "missing exact source tree")
    require(report.get("run_id") == run.name, "run ID/path mismatch")
    started, finished = timestamp(report["started_utc"]), timestamp(report["finished_utc"])
    require(started <= finished, "run time window is reversed")
    counts = report.get("counts")
    require(isinstance(counts, dict) and set(counts) == set(CLASSES), "result must have exact eight class counts")

    retained = run / "xml"
    manifest_path = run / "xml-manifest.json"
    manifest_exists = manifest_path.exists()
    require(not manifest_path.is_symlink(), "XML manifest cannot be symlink")
    existing = list(retained.glob("*")) if retained.exists() else []
    require(not retained.is_symlink(), "retained XML directory cannot be symlink")
    require(manifest_exists or not existing, "partial retained XML without manifest; manual review required")
    if manifest_exists:
        manifest = json.loads(manifest_path.read_text())
        require(isinstance(manifest, list) and len(manifest) == len(CLASSES), "manifest is not exact eight")
        by_file = {entry["file"]: entry for entry in manifest}
        require(len(by_file) == len(CLASSES), "duplicate manifest entry")
        require(set(by_file) == {"xml/TEST-" + name + ".xml" for name in CLASSES},
                "manifest class set differs")
        require(len(existing) == len(CLASSES), "retained XML count differs from manifest")
    else:
        # The frozen driver already redacted each fresh source XML. Its private secrets are
        # unavailable here, so this is only a retention/verifier, not independent redaction.
        source_root = repo / "backend/wta-admin/target/surefire-reports"
        source_rows = {}
        for name in CLASSES:
            file = source_root / ("TEST-" + name + ".xml")
            source_rows[name] = inspect_xml(file, name, started, finished)
            require(source_rows[name]["counts"] == counts[name], "source/result counts differ: " + name)
        retained.mkdir(mode=0o700, exist_ok=False)
        created = []
        try:
            manifest = []
            for name in CLASSES:
                source = source_root / ("TEST-" + name + ".xml")
                target = retained / source.name
                with source.open("rb") as inp, target.open("xb") as out:
                    os.fchmod(out.fileno(), 0o600)
                    shutil.copyfileobj(inp, out)
                os.utime(target, ns=(source.stat().st_atime_ns, source.stat().st_mtime_ns))
                created.append(target)
                row = inspect_xml(target, name, started, finished)
                require(row["sha256"] == source_rows[name]["sha256"], "copy digest differs: " + name)
                manifest.append({"file": row["file"], "sha256": row["sha256"],
                                 "counts": row["counts"], "mtime_ns": row["mtime_ns"]})
            with manifest_path.open("x") as out:
                os.fchmod(out.fileno(), 0o600)
                json.dump(manifest, out, indent=2)
                out.write("\n")
        except BaseException:
            for target in created:
                target.unlink(missing_ok=True)
            if not list(retained.iterdir()):
                retained.rmdir()
            raise
        by_file = {entry["file"]: entry for entry in manifest}

    rows = []
    for name in CLASSES:
        file = retained / ("TEST-" + name + ".xml")
        row = inspect_xml(file, name, started, finished)
        require(row["counts"] == counts[name], "retained/result counts differ: " + name)
        entry = by_file[row["file"]]
        require(row["sha256"] == entry["sha256"] and row["counts"] == entry["counts"],
                "retained/manifest digest or count differs: " + name)
        if "mtime_ns" in entry:
            require(row["mtime_ns"] == entry["mtime_ns"], "retained mtime differs: " + name)
        rows.append(row)
    total = {key: sum(row["counts"][key] for row in rows) for key in FIELDS}
    return {"run_id": report["run_id"], "source_head": expected_head,
            "source_tree": before["tree"], "classes": len(rows), "totals": total,
            "maven_exit_code": report.get("maven_exit_code"), "acceptance": report.get("acceptance"),
            "retained": rows}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("run", type=Path)
    parser.add_argument("--expected-head", required=True)
    parser.add_argument("--repo-root", type=Path, default=Path("/srv/WTA-plus"))
    args = parser.parse_args()
    try:
        print(json.dumps(verify(args.run.resolve(), args.expected_head, args.repo_root.resolve()), indent=2))
    except Exception as error:
        print(json.dumps({"verified": False, "error_type": type(error).__name__,
                          "reason": str(error)}), file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
