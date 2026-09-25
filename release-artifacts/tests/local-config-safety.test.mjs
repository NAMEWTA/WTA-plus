import assert from 'node:assert/strict';
import { execFileSync, spawnSync } from 'node:child_process';
import { copyFileSync, existsSync, mkdtempSync, mkdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { createServer } from 'node:net';
import { tmpdir } from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import test from 'node:test';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const localConfig = 'backend/wta-admin/src/main/resources/application-local.yml';

test('machine-local credentials are ignored and absent from the Git index', () => {
  const tracked = execFileSync('git', ['ls-files', '--', localConfig], { cwd: root, encoding: 'utf8' });
  assert.equal(tracked.trim(), '', 'the private local configuration must not be tracked');
  execFileSync('git', ['check-ignore', '--no-index', localConfig], { cwd: root, stdio: 'pipe' });
});

test('the developer launcher loads the private file externally and preserves an explicit override', async () => {
  const directory = mkdtempSync(path.join(tmpdir(), 'wta config launcher '));
  const server = createServer();
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  const port = server.address().port;
  await new Promise(resolve => server.close(resolve));
  try {
    const put = (name, text, options) => {
      const file = path.join(directory, name);
      mkdirSync(path.dirname(file), { recursive: true });
      writeFileSync(file, text, options);
    };
    for (const file of ['scripts/start-dev.sh', 'scripts/lib/backend-build-guard.sh', 'scripts/lib/dev-runtime.sh']) {
      mkdirSync(path.dirname(path.join(directory, file)), { recursive: true });
      copyFileSync(path.join(root, file), path.join(directory, file));
    }
    put(localConfig, `server:\n  port: ${port}\npassword: synthetic-private-canary\n`);
    put('backend/pom.xml', '<project/>');
    put('backend/wta-admin/pom.xml', '<project/>');
    const classes = 'backend/wta-modules/wta-system/target/classes';
    for (const suffix of ['domain/vo/SysClientVo', 'mapper/SysUserMapper', 'password/PasswordPolicyService',
      'service/ISysClientService', 'temporarypassword/TemporaryPasswordService']) {
      put(`${classes}/org/namewta/system/${suffix}.class`, 'synthetic class entry');
    }
    const jar = path.join(directory, 'backend/wta-modules/wta-system/target/wta-system-fixture.jar');
    execFileSync('jar', ['cf', jar, '-C', path.join(directory, classes), '.']);
    const mvnw = `#!/usr/bin/env bash
set -eu
printf '%s\\n' "$*" >> "$FIXTURE_MVNW_LOG"
for argument in "$@"; do
  case "$argument" in
    -Dmdep.outputFile=*) printf '%s\\n' "$FIXTURE_SYSTEM_JAR" > "\${argument#*=}"; exit 0 ;;
    spring-boot:run) printf 'CONFIG=%s\\n' "\${SPRING_CONFIG_ADDITIONAL_LOCATION-}"; exit 0 ;;
    clean) exit 0 ;;
  esac
done
if [[ " $* " == *" install "* && " $* " != *" clean "* ]]; then exit 0; fi
exit 1
`;
    put('backend/mvnw', mvnw, { mode: 0o755 });
    for (const override of [undefined, 'optional:file:/explicit/operator-config/']) {
      const env = { ...process.env, FIXTURE_SYSTEM_JAR: jar, FIXTURE_MVNW_LOG: path.join(directory, 'backend/mvnw-log') };
      delete env.SPRING_CONFIG_ADDITIONAL_LOCATION;
      delete env.SERVER_PORT;
      if (override !== undefined) env.SPRING_CONFIG_ADDITIONAL_LOCATION = override;
      writeFileSync(path.join(directory, 'backend/mvnw-log'), '');
      const result = spawnSync('bash', [path.join(directory, 'scripts/start-dev.sh'), 'start', 'backend'], {
        cwd: directory, env, encoding: 'utf8', timeout: 30000
      });
      assert.equal(result.status, 0, result.stderr + result.stdout);
      const selected = result.stdout.split('\n').find(line => line.startsWith('CONFIG='));
      assert.equal(selected, `CONFIG=${override ?? `optional:file:${path.join(directory, localConfig)}`}`);
      const recorded = readFileSync(path.join(directory, 'backend/mvnw-log'), 'utf8');
      assert.equal(recorded.includes('--server.port'), false);
      assert.equal(recorded.includes(' clean '), false);
      assert.equal(recorded.includes('install'), false);
      assert.equal((result.stdout + result.stderr).includes('synthetic-private-canary'), false);
    }
    const repairEnv = { ...process.env, FIXTURE_SYSTEM_JAR: jar, FIXTURE_MVNW_LOG: path.join(directory, 'backend/mvnw-log') };
    delete repairEnv.SERVER_PORT;
    writeFileSync(path.join(directory, 'backend/mvnw-log'), '');
    const repair = spawnSync('bash', [path.join(directory, 'scripts/start-dev.sh'), 'repair', 'backend'], {
      cwd: directory, env: repairEnv, encoding: 'utf8', timeout: 30000
    });
    assert.equal(repair.status, 0, repair.stderr + repair.stdout);
    const repairLog = readFileSync(path.join(directory, 'backend/mvnw-log'), 'utf8');
    assert.equal(repairLog.includes(' clean '), true, repairLog);
    const preserved = { ...process.env, FIXTURE_SYSTEM_JAR: jar, FIXTURE_MVNW_LOG: path.join(directory, 'backend/mvnw-log'), SERVER_PORT: String(port) };
    delete preserved.SPRING_CONFIG_ADDITIONAL_LOCATION;
    writeFileSync(path.join(directory, 'backend/mvnw-log'), '');
    const kept = spawnSync('bash', [path.join(directory, 'scripts/start-dev.sh'), 'start', 'backend'], {
      cwd: directory, env: preserved, encoding: 'utf8', timeout: 30000
    });
    assert.equal(kept.status, 0, kept.stderr + kept.stdout);
    assert.equal(readFileSync(path.join(directory, 'backend/mvnw-log'), 'utf8').includes('--server.port'), false);
    assert.equal((kept.stdout + kept.stderr).includes(`SERVER_PORT=${port}`), false);
    writeFileSync(path.join(directory, 'backend/mvnw-log'), '');
    const menu = spawnSync('bash', [path.join(directory, 'scripts/start-dev.sh')], {
      cwd: directory, env: preserved, input: '2\n', encoding: 'utf8', timeout: 30000
    });
    assert.equal(menu.status, 0, menu.stderr + menu.stdout);
    assert.equal(readFileSync(path.join(directory, 'backend/mvnw-log'), 'utf8').includes('spring-boot:run'), true);
  } finally {
    rmSync(directory, { recursive: true, force: true });
  }
});

test('Maven excludes private configuration and examples, including stale target files, from JARs', () => {
  const directory = mkdtempSync(path.join(tmpdir(), 'wta-local-config-'));
  try {
    const resources = path.join(directory, 'src/main/resources');
    mkdirSync(resources, { recursive: true });
    writeFileSync(path.join(resources, 'application.yml'), 'spring:\n  application:\n    name: synthetic-fixture\n');
    const excluded = ['application-local.yml', 'application-local.yaml', 'application-local.example.yml'];
    for (const file of excluded) writeFileSync(path.join(resources, file), 'password: synthetic-secret-canary\n');
    const parent = readFileSync(path.join(root, 'backend/pom.xml'), 'utf8');
    const resourcePolicy = parent.match(/<resources>[\s\S]*?<\/resources>/)?.[0];
    const jarPolicy = [...parent.matchAll(/<plugin>[\s\S]*?<\/plugin>/g)]
      .find(([plugin]) => plugin.includes('<artifactId>maven-jar-plugin</artifactId>'))?.[0] ?? '';
    const jarVersion = parent.match(/<maven-jar-plugin.version>([^<]+)</)?.[1];
    assert.ok(resourcePolicy);
    // Execute the repository's actual packaging rules on synthetic files, without
    // resolving its CI-friendly parent from a potentially stale installed POM.
    writeFileSync(path.join(directory, 'pom.xml'), `<project xmlns="http://maven.apache.org/POM/4.0.0">
      <modelVersion>4.0.0</modelVersion><groupId>org.namewta</groupId>
      <artifactId>local-config-fixture</artifactId><version>1</version>
      <properties><maven-jar-plugin.version>${jarVersion}</maven-jar-plugin.version></properties>
      <build>${resourcePolicy}<plugins>${jarPolicy}</plugins></build></project>`);
    const maven = (...goals) => execFileSync(path.join(root, 'backend/mvnw'), ['-q', '-f', path.join(directory, 'pom.xml'), ...goals], {
      cwd: path.join(root, 'backend'), encoding: 'utf8', stdio: 'pipe', timeout: 120000
    });
    maven('resources:resources');
    assert.ok(existsSync(path.join(directory, 'target/classes/application.yml')));
    for (const file of excluded) {
      assert.equal(existsSync(path.join(directory, 'target/classes', file)), false, `${file} must not enter the resource output`);
      // An incremental build can retain files copied before the exclusion existed.
      writeFileSync(path.join(directory, 'target/classes', file), 'password: synthetic-stale-canary\n');
    }
    maven('jar:jar');
    const entries = execFileSync('jar', ['tf', path.join(directory, 'target/local-config-fixture-1.jar')], { encoding: 'utf8' }).split('\n');
    assert.ok(entries.includes('application.yml'));
    for (const file of excluded) assert.equal(entries.includes(file), false, `${file} must not enter the JAR`);
  } finally {
    rmSync(directory, { recursive: true, force: true });
  }
});
