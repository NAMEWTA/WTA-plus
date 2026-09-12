#!/usr/bin/env node
import { checkContracts, defaultPaths, fetchSnapshot, generateContracts, OpenApiContractError } from './index.mjs';

function option(name) {
  const index = process.argv.indexOf(`--${name}`);
  return index === -1 ? undefined : process.argv[index + 1];
}

const command = process.argv[2];

try {
  if (command === 'fetch') {
    const result = await fetchSnapshot({
      backendCommit: option('backend-commit'),
      backendRepository: option('backend-repository') ?? 'wta-vue-plus-namewta',
      pointer: option('pointer') ?? defaultPaths.pointer,
      runtimeEndpoint: option('runtime-endpoint') ?? '/v3/api-docs',
      source: option('source'),
      store: option('store') ?? defaultPaths.store
    });
    console.log(`OpenAPI snapshot updated: ${result.paths} paths, ${result.schemas} schemas, sha256=${result.sha256}`);
  } else if (command === 'generate') {
    const result = await generateContracts({
      output: option('output') ?? defaultPaths.output,
      pointer: option('pointer') ?? defaultPaths.pointer,
      store: option('store') ?? defaultPaths.store
    });
    console.log(`OpenAPI contract generated: ${result.bytes} bytes, sha256=${result.sha256}`);
  } else if (command === 'check') {
    const result = await checkContracts({
      output: option('output') ?? defaultPaths.output,
      pointer: option('pointer') ?? defaultPaths.pointer,
      store: option('store') ?? defaultPaths.store
    });
    console.log(`OpenAPI contract is current: ${result.bytes} bytes, sha256=${result.sha256}`);
  } else {
    throw new OpenApiContractError(
      'Usage: namewta-openapi <fetch|generate|check> [--source <url-or-file>] [--backend-commit <sha>]'
    );
  }
} catch (error) {
  console.error(error instanceof Error ? error.message : String(error));
  process.exitCode = 1;
}
