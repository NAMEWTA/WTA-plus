"""Run a serial Maven check using only a newly created disposable local Redis."""
import argparse
import datetime
import json
import pathlib
import subprocess
import time
import uuid
import xml.etree.ElementTree as ET
import sys

parser = argparse.ArgumentParser()
parser.add_argument("--evidence-name", required=True)
parser.add_argument("--test")
parser.add_argument("--modules", default="wta-admin")
args = parser.parse_args()
evidence = pathlib.Path(__file__).resolve().parent
root = evidence.parents[5]
name = "namewta-t24-redis-" + uuid.uuid4().hex[:12]
image = "redis:7.4-alpine@sha256:ff02b58f971e7d7d156a1267e283fcbbeee91773b6aa36c49dac28ecfe28eadf"
record = {"captured_at": datetime.datetime.now(datetime.timezone.utc).isoformat(), "container": name, "image": image}
def inventory():
    return {key: sorted(subprocess.check_output(command, text=True).splitlines()) for key, command in {
        'containers': ['docker', 'ps', '-aq', '--no-trunc'], 'networks': ['docker', 'network', 'ls', '-q', '--no-trunc'],
        'volumes': ['docker', 'volume', 'ls', '-q']}.items()}
record['before'] = inventory()
started = time.time()
try:
    subprocess.run(["docker", "run", "-d", "--name", name, "--label", "namewta.test.owner=T-24",
                    "-p", "127.0.0.1::6379", image, "redis-server", "--save", "", "--appendonly", "no"],
                   check=True, capture_output=True, text=True)
    port = subprocess.check_output(["docker", "port", name, "6379/tcp"], text=True).strip().rsplit(":", 1)[1]
    for attempt in range(50):
        ping = subprocess.run(["docker", "exec", name, "redis-cli", "ping"], capture_output=True, text=True)
        if ping.returncode == 0 and ping.stdout.strip() == "PONG":
            break
        time.sleep(0.1)
    else:
        raise RuntimeError("Owned Redis did not become ready")
    command = ["./mvnw", "-B", "-ntp", "-pl", args.modules, "-am", "test",
               "-Dthird.redis.integration.port=" + port, "-Dthird.redis.integration.container=" + name, "-Dprofiles.active=dev,local"]
    if args.test:
        command.extend(["-Dtest=" + args.test, "-Dsurefire.failIfNoSpecifiedTests=false"])
    record.update(command=command, cwd=str(root / "backend"), port=int(port), log=args.evidence_name + ".log")
    with (evidence / record["log"]).open("w") as output:
        result = subprocess.run(command, cwd=root / "backend", stdout=output, stderr=subprocess.STDOUT)
    record["exit_code"] = result.returncode
    counts = dict(tests=0, failures=0, errors=0, skipped=0)
    for report in (root / 'backend').rglob('target/surefire-reports/TEST-*.xml'):
        if report.stat().st_mtime < started: continue
        suite = ET.parse(report).getroot()
        for key in counts: counts[key] += int(suite.get(key, 0))
    record['counts'] = counts
finally:
    cleanup = subprocess.run(["docker", "rm", "-fv", name], capture_output=True, text=True)
    record["cleanup_exit_code"] = cleanup.returncode
    record["after"] = inventory()
    record["resources_restored"] = record["before"] == record["after"]
    (evidence / (args.evidence_name + ".json")).write_text(json.dumps(record, indent=2) + "\n")
print(json.dumps({key: record.get(key) for key in ["exit_code", "counts", "resources_restored"]}))
sys.exit(record.get("exit_code", 1))
