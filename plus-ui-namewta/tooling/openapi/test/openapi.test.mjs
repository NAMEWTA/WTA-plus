import assert from 'node:assert/strict';
import { mkdir, mkdtemp, readFile, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import test from 'node:test';
import { inspect } from 'node:util';
import { checkContracts, fetchSnapshot, generateContracts, OpenApiContractError, sha256 } from '../src/index.mjs';

const backendCommit = 'a98d6edcc591550221dd983e293d43e3aac36d23';
const validSpec = paths =>
  JSON.stringify({
    openapi: '3.1.0',
    info: { title: 'fixture', version: '1' },
    paths,
    components: { schemas: { Message: { type: 'object', properties: { value: { type: 'string' } } } } }
  });

async function fixture(t) {
  const directory = await mkdtemp(join(tmpdir(), 'namewta-openapi-'));
  t.after(() => rm(directory, { recursive: true, force: true }));
  const store = join(directory, 'openapi');
  return { directory, output: join(directory, 'openapi.ts'), pointer: join(store, 'current.json'), store };
}

const fetchFixture = (paths, source) => fetchSnapshot({ ...paths, backendCommit, source });

async function activeRevision({ pointer, store }) {
  const { revision } = JSON.parse(await readFile(pointer, 'utf8'));
  const directory = join(store, 'revisions', revision);
  return {
    directory,
    pointer: await readFile(pointer, 'utf8'),
    provenance: await readFile(join(directory, 'provenance.json'), 'utf8'),
    revision,
    snapshot: await readFile(join(directory, 'source.json'), 'utf8')
  };
}

test('invalid and missing sources preserve the last-known-good active revision', async t => {
  const paths = await fixture(t);
  const baseline = join(paths.directory, 'baseline.json');
  const invalid = join(paths.directory, 'invalid.json');
  await writeFile(baseline, validSpec({}));
  await writeFile(invalid, '{"openapi":"2.0"}');
  await fetchFixture(paths, baseline);
  const before = await activeRevision(paths);

  await assert.rejects(fetchFixture(paths, invalid), OpenApiContractError);
  await assert.rejects(fetchFixture(paths, join(paths.directory, 'missing.json')), OpenApiContractError);
  assert.deepEqual(await activeRevision(paths), before);
});

test('valid fetch persists an immutable revision before atomically activating its pointer', async t => {
  const paths = await fixture(t);
  const source = join(paths.directory, 'next.json');
  await writeFile(source, validSpec({ '/ready': { get: { responses: { 200: { description: 'ok' } } } } }));

  const result = await fetchFixture(paths, source);
  const active = await activeRevision(paths);
  const provenance = JSON.parse(active.provenance);
  assert.equal(result.paths, 1);
  assert.equal(active.snapshot, await readFile(source, 'utf8'));
  assert.match(active.revision, /^[\da-f]{64}$/);
  assert.notEqual(active.revision, result.sha256);
  assert.equal(provenance.backendCommit, backendCommit);
  assert.equal(provenance.generator, 'openapi-typescript@7.13.0');
  assert.deepEqual(provenance.totals, { paths: 1, schemas: 1, tags: 0 });
  assert.equal(provenance.rawSha256, result.sha256);
});

test('identical snapshots with different provenance activate distinct immutable revisions', async t => {
  const paths = await fixture(t);
  const source = join(paths.directory, 'source.json');
  await writeFile(source, validSpec({}));
  await fetchFixture(paths, source);
  const first = await activeRevision(paths);

  await fetchSnapshot({ ...paths, backendCommit: 'b'.repeat(40), source });
  const second = await activeRevision(paths);

  assert.notEqual(second.revision, first.revision);
  assert.equal(second.snapshot, first.snapshot);
  assert.equal(JSON.parse(second.provenance).backendCommit, 'b'.repeat(40));
  assert.equal(await readFile(join(first.directory, 'provenance.json'), 'utf8'), first.provenance);
});

test('an unactivated revision cannot change the last-known-good contract', async t => {
  const paths = await fixture(t);
  const source = join(paths.directory, 'source.json');
  await writeFile(source, validSpec({}));
  await fetchFixture(paths, source);
  await generateContracts(paths);
  const pointerBefore = await readFile(paths.pointer, 'utf8');

  const orphanBytes = Buffer.from(validSpec({ '/orphan': { get: { responses: { 200: { description: 'ok' } } } } }));
  const orphan = sha256(orphanBytes);
  const orphanDirectory = join(paths.store, 'revisions', orphan);
  await mkdir(orphanDirectory, { recursive: true });
  await writeFile(join(orphanDirectory, 'source.json'), orphanBytes);
  await writeFile(join(orphanDirectory, 'provenance.json'), '{}\n');

  await checkContracts(paths);
  assert.equal(await readFile(paths.pointer, 'utf8'), pointerBefore);
});

test('generation rejects stale or invalid provenance in the active immutable revision', async t => {
  const paths = await fixture(t);
  const source = join(paths.directory, 'source.json');
  const baseline = validSpec({});
  await writeFile(source, baseline);
  await fetchFixture(paths, source);
  await generateContracts(paths);
  const active = await activeRevision(paths);
  await writeFile(
    join(active.directory, 'source.json'),
    validSpec({ '/drift': { get: { responses: { 200: { description: 'ok' } } } } })
  );

  await assert.rejects(checkContracts(paths), /provenance drift detected/);
  await assert.rejects(generateContracts(paths), /provenance drift detected/);

  await writeFile(join(active.directory, 'source.json'), baseline);
  const provenance = JSON.parse(active.provenance);
  for (const [field, value] of [
    ['backendCommit', 'c'.repeat(40)],
    ['backendRepository', 'different-backend'],
    ['runtimeEndpoint', '/different-api-docs']
  ]) {
    await writeFile(join(active.directory, 'provenance.json'), `${JSON.stringify({ ...provenance, [field]: value }, null, 2)}\n`);
    await assert.rejects(checkContracts(paths), /provenance drift detected/);
  }

  await writeFile(join(active.directory, 'provenance.json'), 'null\n');
  await assert.rejects(checkContracts(paths), /provenance is invalid/);
});

test('check detects source drift and manual generated-file edits without writing', async t => {
  const paths = await fixture(t);
  const source = join(paths.directory, 'source.json');
  await writeFile(source, validSpec({}));
  await fetchFixture(paths, source);
  await generateContracts(paths);
  const baseline = await readFile(paths.output, 'utf8');
  await checkContracts(paths);

  await writeFile(source, validSpec({ '/drift': { get: { responses: { 200: { description: 'ok' } } } } }));
  await fetchFixture(paths, source);
  await assert.rejects(checkContracts(paths), /contract drift detected/);
  assert.equal(await readFile(paths.output, 'utf8'), baseline);

  await generateContracts(paths);
  await checkContracts(paths);
  await writeFile(paths.output, `${await readFile(paths.output, 'utf8')}\n// manual edit\n`);
  await assert.rejects(checkContracts(paths), /contract drift detected/);
});

test('HTTP source errors redact credentials and query parameters for normalized schemes', async () => {
  for (const source of [
    'HTTPS://user:secret@127.0.0.1:1/contracts?token=sensitive#fragment',
    'HTTPS:/user:secret@127.0.0.1:1/contracts?token=sensitive#fragment',
    ' HTTPS:/user:secret@127.0.0.1:1/contracts?token=sensitive#fragment',
    '\tHTTPS:/user:secret@127.0.0.1:1/contracts?token=sensitive#fragment'
  ]) {
    await assert.rejects(
      fetchSnapshot({ backendCommit, source }),
      error => {
        const diagnostic = inspect(error, { depth: 5 });
        return (
          error instanceof OpenApiContractError &&
          error.message.includes('https://127.0.0.1:1/contracts') &&
          !diagnostic.includes('secret') &&
          !diagnostic.includes('token') &&
          !diagnostic.includes('sensitive')
        );
      }
    );
  }
});
