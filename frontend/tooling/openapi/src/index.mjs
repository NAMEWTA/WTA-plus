import { createHash, randomUUID } from 'node:crypto';
import { mkdir, readFile, rename, rm, unlink, writeFile } from 'node:fs/promises';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import openapiTS, { astToString } from 'openapi-typescript';

const packageDirectory = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const contractsDirectory = resolve(packageDirectory, '../../packages/api-contracts');
const generator = 'openapi-typescript@7.13.0';
const revisionPattern = /^[\da-f]{64}$/;

export const defaultPaths = Object.freeze({
  output: resolve(contractsDirectory, 'generated/openapi.ts'),
  pointer: resolve(contractsDirectory, 'openapi/current.json'),
  store: resolve(contractsDirectory, 'openapi')
});

export class OpenApiContractError extends Error {
  constructor(message, cause) {
    super(message, cause ? { cause } : undefined);
    this.name = 'OpenApiContractError';
  }
}

export function validateOpenApi(value) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw new OpenApiContractError('OpenAPI source must be a JSON object');
  }
  if (typeof value.openapi !== 'string' || !/^3\.(?:0|1)\./.test(value.openapi)) {
    throw new OpenApiContractError('OpenAPI source must declare version 3.0.x or 3.1.x');
  }
  if (!value.paths || typeof value.paths !== 'object' || Array.isArray(value.paths)) {
    throw new OpenApiContractError('OpenAPI source must contain a paths object');
  }
  const schemas = value.components?.schemas;
  if (!schemas || typeof schemas !== 'object' || Array.isArray(schemas)) {
    throw new OpenApiContractError('OpenAPI source must contain components.schemas');
  }
  return value;
}

export function sha256(value) {
  return createHash('sha256').update(value).digest('hex');
}

function classifySource(source) {
  let url;
  try {
    url = new URL(source);
  } catch {
    if (/^https?:/i.test(source.trimStart())) {
      throw new OpenApiContractError('OpenAPI HTTP source URL is invalid');
    }
    return { kind: 'file', path: source };
  }

  const protocol = url.protocol.toLowerCase();
  if (protocol === 'http:' || protocol === 'https:') {
    const label = `${protocol}//${url.host}${url.pathname}`;
    if (url.username || url.password) {
      throw new OpenApiContractError(`OpenAPI HTTP source URL must not include credentials: ${label}`);
    }
    return { kind: 'http', label, url };
  }
  return { kind: 'file', path: source };
}

async function readSource(source) {
  const classified = classifySource(source);
  if (classified.kind === 'http') {
    let response;
    try {
      response = await fetch(classified.url, {
        headers: { accept: 'application/json' },
        signal: AbortSignal.timeout(30_000)
      });
    } catch {
      throw new OpenApiContractError(`Unable to fetch OpenAPI source: ${classified.label}`);
    }
    if (!response.ok) {
      throw new OpenApiContractError(`OpenAPI source returned HTTP ${response.status}: ${classified.label}`);
    }
    return Buffer.from(await response.arrayBuffer());
  }
  try {
    return await readFile(resolve(classified.path));
  } catch (error) {
    throw new OpenApiContractError(`Unable to read OpenAPI source: ${classified.path}`, error);
  }
}

function parseSource(bytes) {
  try {
    return validateOpenApi(JSON.parse(bytes.toString('utf8')));
  } catch (error) {
    if (error instanceof OpenApiContractError) throw error;
    throw new OpenApiContractError('OpenAPI source is not valid JSON', error);
  }
}

function summarize(bytes, spec) {
  return Object.freeze({
    openapi: spec.openapi,
    paths: Object.keys(spec.paths).length,
    schemas: Object.keys(spec.components.schemas).length,
    tags: Array.isArray(spec.tags) ? spec.tags.length : 0,
    sha256: sha256(bytes)
  });
}

function createProvenance(bytes, spec, { backendCommit, backendRepository, runtimeEndpoint }) {
  const summary = summarize(bytes, spec);
  if (!/^[\da-f]{40}$/i.test(backendCommit ?? '')) {
    throw new OpenApiContractError('fetch requires --backend-commit <40-character-sha>');
  }
  if (!backendRepository?.trim()) throw new OpenApiContractError('backend repository must not be empty');
  if (!runtimeEndpoint?.trim()) throw new OpenApiContractError('runtime endpoint must not be empty');
  return {
    backendCommit,
    backendRepository,
    generator,
    openapiVersion: summary.openapi,
    rawSha256: summary.sha256,
    runtimeEndpoint,
    totals: { paths: summary.paths, schemas: summary.schemas, tags: summary.tags }
  };
}

const jsonText = value => `${JSON.stringify(value, null, 2)}\n`;
const createRevision = (bytes, provenance) => sha256(Buffer.concat([bytes, Buffer.from(jsonText(provenance))]));

async function atomicWrite(path, content) {
  const temporary = `${path}.tmp-${process.pid}-${randomUUID()}`;
  try {
    await writeFile(temporary, content);
    await rename(temporary, path);
  } finally {
    await unlink(temporary).catch(() => undefined);
  }
}

function revisionPaths(store, revision) {
  if (!revisionPattern.test(revision)) throw new OpenApiContractError('OpenAPI revision pointer is invalid');
  const directory = join(store, 'revisions', revision);
  return {
    directory,
    provenance: join(directory, 'provenance.json'),
    snapshot: join(directory, 'source.json')
  };
}

async function readPointer(pointer) {
  let current;
  try {
    current = JSON.parse(await readFile(pointer, 'utf8'));
  } catch (error) {
    throw new OpenApiContractError(`OpenAPI revision pointer is missing or invalid: ${pointer}`, error);
  }
  if (!current || typeof current !== 'object' || Array.isArray(current) || !revisionPattern.test(current.revision)) {
    throw new OpenApiContractError(`OpenAPI revision pointer is missing or invalid: ${pointer}`);
  }
  return current.revision;
}

async function readRevision(store, revision) {
  const paths = revisionPaths(store, revision);
  try {
    const [bytes, provenance] = await Promise.all([readFile(paths.snapshot), readFile(paths.provenance, 'utf8')]);
    return { bytes, paths, provenance };
  } catch (error) {
    if (error?.code === 'ENOENT') return undefined;
    throw error;
  }
}

async function persistRevision(store, revision, bytes, provenance) {
  const paths = revisionPaths(store, revision);
  const expectedProvenance = jsonText(provenance);
  const existing = await readRevision(store, revision);
  if (existing) {
    if (!existing.bytes.equals(bytes) || existing.provenance !== expectedProvenance) {
      throw new OpenApiContractError(`Immutable OpenAPI revision collision detected: ${revision}`);
    }
    return;
  }

  const revisions = join(store, 'revisions');
  const staged = join(revisions, `.staged-${revision}-${process.pid}-${randomUUID()}`);
  await mkdir(staged, { recursive: true });
  try {
    await Promise.all([
      writeFile(join(staged, 'source.json'), bytes),
      writeFile(join(staged, 'provenance.json'), expectedProvenance)
    ]);
    try {
      await rename(staged, paths.directory);
    } catch (error) {
      if (!['EEXIST', 'ENOTEMPTY'].includes(error?.code)) throw error;
      const concurrent = await readRevision(store, revision);
      if (!concurrent || !concurrent.bytes.equals(bytes) || concurrent.provenance !== expectedProvenance) {
        throw new OpenApiContractError(`Immutable OpenAPI revision collision detected: ${revision}`, error);
      }
    }
  } finally {
    await rm(staged, { recursive: true, force: true });
  }
}

async function readAndValidateActiveRevision(store, pointer) {
  const revision = await readPointer(pointer);
  const active = await readRevision(store, revision);
  if (!active) throw new OpenApiContractError(`Active OpenAPI revision is missing: ${revision}`);
  const spec = parseSource(active.bytes);
  let current;
  try {
    current = JSON.parse(active.provenance);
  } catch (error) {
    throw new OpenApiContractError(`OpenAPI provenance is invalid in revision ${revision}`, error);
  }
  if (!current || typeof current !== 'object' || Array.isArray(current)) {
    throw new OpenApiContractError(`OpenAPI provenance is invalid in revision ${revision}`);
  }
  const expected = createProvenance(active.bytes, spec, {
    backendCommit: current.backendCommit,
    backendRepository: current.backendRepository,
    runtimeEndpoint: current.runtimeEndpoint
  });
  if (revision !== createRevision(active.bytes, expected) || active.provenance !== jsonText(expected)) {
    throw new OpenApiContractError('OpenAPI provenance drift detected; run openapi:fetch and review the diff');
  }
  return { spec };
}

export async function fetchSnapshot({
  backendCommit,
  backendRepository = 'backend',
  pointer = defaultPaths.pointer,
  runtimeEndpoint = '/v3/api-docs',
  source,
  store = defaultPaths.store
}) {
  if (!source) throw new OpenApiContractError('fetch requires --source <url-or-file>');
  const bytes = await readSource(source);
  const spec = parseSource(bytes);
  const metadata = createProvenance(bytes, spec, { backendCommit, backendRepository, runtimeEndpoint });
  const revision = createRevision(bytes, metadata);
  await persistRevision(store, revision, bytes, metadata);
  await mkdir(dirname(pointer), { recursive: true });
  await atomicWrite(pointer, jsonText({ revision }));
  return summarize(bytes, spec);
}

async function generateContractsFromSpec(spec) {
  try {
    return astToString(await openapiTS(spec));
  } catch (error) {
    throw new OpenApiContractError('Unable to generate TypeScript from the checked OpenAPI snapshot', error);
  }
}

export async function generateContractsText({ pointer = defaultPaths.pointer, store = defaultPaths.store } = {}) {
  const { spec } = await readAndValidateActiveRevision(store, pointer);
  return generateContractsFromSpec(spec);
}

export async function generateContracts({
  output = defaultPaths.output,
  pointer = defaultPaths.pointer,
  store = defaultPaths.store
} = {}) {
  const generated = await generateContractsText({ pointer, store });
  await atomicWrite(output, generated);
  return Object.freeze({ bytes: Buffer.byteLength(generated), sha256: sha256(generated) });
}

export async function checkContracts({
  output = defaultPaths.output,
  pointer = defaultPaths.pointer,
  store = defaultPaths.store
} = {}) {
  const expected = await generateContractsText({ pointer, store });
  let current;
  try {
    current = await readFile(output, 'utf8');
  } catch (error) {
    throw new OpenApiContractError(`Generated contract is missing: ${output}`, error);
  }
  if (current !== expected) {
    throw new OpenApiContractError(
      'Generated OpenAPI contract drift detected; run openapi:generate and review the diff'
    );
  }
  return Object.freeze({ bytes: Buffer.byteLength(current), sha256: sha256(current) });
}
