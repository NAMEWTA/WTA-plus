"""Run a serial Maven check using only a newly created disposable local Redis."""
import argparse
import datetime
import json
import pathlib
import subprocess
import time
import uuid

parser = argparse.ArgumentParser()
parser.add_argument("--evidence-name", required=True)
parser.add_argument("--test")
parser.add_argument("--modules", default="wta-common/wta-common-redis")
args = parser.parse_args()
evidence = pathlib.Path(__file__).resolve().parent
root = evidence.parents[5]
name = "namewta-t30-repeat-" + uuid.uuid4().hex[:12]
image = "redis:7.4-alpine@sha256:ff02b58f971e7d7d156a1267e283fcbbeee91773b6aa36c49dac28ecfe28eadf"
record = {"captured_at": datetime.datetime.now(datetime.timezone.utc).isoformat(), "container": name, "image": image}
try:
    subprocess.run(["docker", "run", "-d", "--name", name, "--label", "namewta.test.owner=T-30",
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
               "-Drepeat.redis.integration.port=" + port]
    if args.test:
        command.extend(["-Dtest=" + args.test, "-Dsurefire.failIfNoSpecifiedTests=false"])
    record.update(command=command, cwd=str(root / "backend"), port=int(port), log=args.evidence_name + ".log")
    with (evidence / record["log"]).open("w") as output:
        result = subprocess.run(command, cwd=root / "backend", stdout=output, stderr=subprocess.STDOUT)
    record["exit_code"] = result.returncode
finally:
    cleanup = subprocess.run(["docker", "rm", "-f", "-v", name], capture_output=True, text=True)
    record["cleanup_exit_code"] = cleanup.returncode
    (evidence / (args.evidence_name + ".json")).write_text(json.dumps(record, indent=2) + "\n")
print(json.dumps(record))
