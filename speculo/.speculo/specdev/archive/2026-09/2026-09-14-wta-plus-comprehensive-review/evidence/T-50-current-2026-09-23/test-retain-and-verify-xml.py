#!/usr/bin/env python3
"""Offline safety checks only; never starts Maven, Docker, or HTTP."""

import datetime as dt
import importlib.util
import json
import os
from pathlib import Path
import tempfile
import unittest

SCRIPT = Path(__file__).with_name("retain-and-verify-xml.py")
spec = importlib.util.spec_from_file_location("retained_t50", SCRIPT)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)
HEAD = "1" * 40
TREE = "2" * 40


class RetainedXmlTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory(prefix="t50-retained-offline-")
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        self.run = self.root / "run-one"
        self.run.mkdir()
        self.repo = self.root / "repo"
        source = self.repo / "backend/wta-admin/target/surefire-reports"
        source.mkdir(parents=True)
        now = dt.datetime.now(dt.timezone.utc)
        self.report = {"run_id": self.run.name,
                       "source_before": {"head": HEAD, "tree": TREE, "clean": True},
                       "source_after": {"head": HEAD, "tree": TREE, "clean": True},
                       "started_utc": (now - dt.timedelta(minutes=1)).isoformat(),
                       "finished_utc": (now + dt.timedelta(minutes=1)).isoformat(),
                       "maven_exit_code": 1, "acceptance": False,
                       "counts": {name: {"tests": 1, "failures": int(index == 0),
                                         "errors": 0, "skipped": 0}
                                  for index, name in enumerate(module.CLASSES)}}
        self.save_report()
        for index, name in enumerate(module.CLASSES):
            failure = '<failure message="synthetic"/>' if index == 0 else ""
            (source / ("TEST-" + name + ".xml")).write_text(
                f'<testsuite name="{name}" tests="1" failures="{int(index==0)}" '
                f'errors="0" skipped="0"><testcase name="synthetic">{failure}'
                '</testcase></testsuite>')

    def save_report(self):
        (self.run / "result.json").write_text(json.dumps(self.report))

    def test_retains_failure_run_and_rechecks_without_overwriting(self):
        first = module.verify(self.run, HEAD, self.repo)
        self.assertEqual(first["totals"], {"tests": 8, "failures": 1, "errors": 0, "skipped": 0})
        self.assertFalse(first["acceptance"])
        target = self.run / "xml" / ("TEST-" + module.CLASSES[0] + ".xml")
        before = (target.stat().st_mtime_ns, target.read_bytes())
        second = module.verify(self.run, HEAD, self.repo)
        self.assertEqual(second["totals"], first["totals"])
        self.assertEqual((target.stat().st_mtime_ns, target.read_bytes()), before)

    def test_digest_tamper_rejected_without_repairing_evidence(self):
        module.verify(self.run, HEAD, self.repo)
        target = self.run / "xml" / ("TEST-" + module.CLASSES[0] + ".xml")
        target.write_text(target.read_text().replace('synthetic', 'tampered'))
        with self.assertRaisesRegex(ValueError, "digest"):
            module.verify(self.run, HEAD, self.repo)

    def test_stale_source_rejected_before_copy(self):
        source = self.repo / "backend/wta-admin/target/surefire-reports" / ("TEST-" + module.CLASSES[0] + ".xml")
        os.utime(source, (1, 1))
        with self.assertRaisesRegex(ValueError, "stale"):
            module.verify(self.run, HEAD, self.repo)
        self.assertFalse((self.run / "xml").exists())

    def test_source_change_rejected(self):
        self.report["source_after"]["tree"] = "3" * 40
        self.save_report()
        with self.assertRaisesRegex(ValueError, "source identity"):
            module.verify(self.run, HEAD, self.repo)

    def test_existing_xml_without_manifest_is_not_overwritten(self):
        folder = self.run / "xml"
        folder.mkdir()
        target = folder / ("TEST-" + module.CLASSES[0] + ".xml")
        target.write_text("old")
        with self.assertRaisesRegex(ValueError, "partial retained"):
            module.verify(self.run, HEAD, self.repo)
        self.assertEqual(target.read_text(), "old")


if __name__ == "__main__":
    unittest.main()
