"""Serial T-08 regression verification using owned localhost MySQL/Redis and real Chrome."""
import argparse
import datetime
import json
import pathlib
import re
import subprocess
import sys
import time
import uuid

parser = argparse.ArgumentParser()
parser.add_argument("--evidence-name", required=True)
parser.add_argument("--test", default="CorsPolicyTest,Sso*Test,PkceS256Test")
parser.add_argument("--journey", action="store_true")
args = parser.parse_args()
evidence = pathlib.Path(__file__).resolve().parent
root = evidence.parents[5]
suffix = uuid.uuid4().hex[:12]
mysql = "namewta-sso-mysql-" + suffix
redis = "namewta-sso-redis-" + suffix
database = "namewta_sso_test_" + suffix
password = "owned-sso-test-only"
images = {
    "mysql": "mysql:8.4.9@sha256:c36050afdca850f23cef85703f84c7531a5ae155a11b5ee1c60acb09937c4084",
    "redis": "redis:7.4-alpine@sha256:ff02b58f971e7d7d156a1267e283fcbbeee91773b6aa36c49dac28ecfe28eadf",
}
record = {"captured_at": datetime.datetime.now(datetime.timezone.utc).isoformat(), "images": images, "containers": [], "cleanup": [],
          "log_redaction": "Owned OAuth query credentials omitted"}
code = 1


def run(command):
    return subprocess.check_output(command, text=True, stderr=subprocess.STDOUT).strip()


def inventory():
    return {kind: sorted(run(command).splitlines()) for kind, command in {
        "containers": ["docker", "ps", "-aq", "--no-trunc"],
        "networks": ["docker", "network", "ls", "-q", "--no-trunc"],
        "volumes": ["docker", "volume", "ls", "-q"],
    }.items()}

record["before"] = inventory()
try:
    mysql_id = run(["docker", "run", "-d", "--name", mysql, "--label", "namewta.test.owner=T-08-regression", "-p", "127.0.0.1::3306",
         "-e", "MYSQL_ROOT_PASSWORD=" + password, "-e", "MYSQL_DATABASE=" + database, images["mysql"],
         "--character-set-server=utf8mb4", "--collation-server=utf8mb4_general_ci"])
    record["containers"].append(mysql_id)
    print("Created owned MySQL fixture", mysql, flush=True)
    for attempt in range(90):
        ready = subprocess.run(["docker", "exec", mysql, "mysqladmin", "ping", "-h", "127.0.0.1", "-uroot", "-p" + password, "--silent"], capture_output=True)
        if ready.returncode == 0:
            break
        time.sleep(1)
    else:
        raise RuntimeError("Owned MySQL was not ready within 90 seconds")
    mysql_port = run(["docker", "port", mysql, "3306/tcp"]).rsplit(":", 1)[1]
    redis_id = run(["docker", "run", "-d", "--name", redis, "--label", "namewta.test.owner=T-08-regression", "-p", "127.0.0.1::6379",
         images["redis"], "redis-server", "--save", "", "--appendonly", "no"])
    record["containers"].append(redis_id)
    for attempt in range(50):
        ready = subprocess.run(["docker", "exec", redis, "redis-cli", "ping"], text=True, capture_output=True)
        if ready.returncode == 0 and ready.stdout.strip() == "PONG":
            break
        time.sleep(0.1)
    else:
        raise RuntimeError("Owned Redis was not ready")
    redis_port = run(["docker", "port", redis, "6379/tcp"]).rsplit(":", 1)[1]
    command = ["./mvnw", "-B", "-ntp", "-pl", "wta-admin", "-am", "test", "-Dtest=" + args.test,
               "-Dsurefire.failIfNoSpecifiedTests=false", "-Dsso.redis.integration.port=" + redis_port,
               "-Dsso.mysql.integration.url=jdbc:mysql://127.0.0.1:" + mysql_port + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai",
               "-Dnamewta.repo.root=" + str(root), "-Dnamewta.sql.root=" + str(root / "release-artifacts/docker/infrastructure/mysql/init")]
    if args.journey:
        command.append("-Dsso.journey.integration=true")
    record.update(command=command, cwd=str(root / "backend"), log=args.evidence_name + ".log")
    with (evidence / record["log"]).open("w") as log:
        process = subprocess.Popen(command, cwd=root / "backend", text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        for line in process.stdout:
            line = re.sub(r'''([?&](?:code|state|code_challenge|code_verifier)=)[^&\s"'<>]+''', r"\1[REDACTED]", line)
            log.write(line)
            log.flush()
            if any(marker in line for marker in ["Building ", "[ERROR]", "Tests run:", "BUILD SUCCESS", "BUILD FAILURE", "real Chrome", " passed ("]):
                print(line, end="", flush=True)
        code = process.wait()
finally:
    for container in reversed(record["containers"]):
        cleanup = subprocess.run(["docker", "rm", "-fv", container], capture_output=True, text=True)
        record["cleanup"].append({"container": container, "exit_code": cleanup.returncode})
        if cleanup.returncode:
            code = 1
    record["after"] = inventory()
    record["resources_restored"] = record["before"] == record["after"]
    if not record["resources_restored"]: code = 1
    record["exit_code"] = code
    (evidence / (args.evidence_name + ".json")).write_text(json.dumps(record, indent=2) + "\n")
print("T-08 regression exit:", code, flush=True)
sys.exit(code)
