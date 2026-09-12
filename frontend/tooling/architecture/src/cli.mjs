#!/usr/bin/env node

import { resolve } from 'node:path';
import { createBaseline, runArchitectureCheck } from './index.mjs';

function parseArguments(arguments_) {
  const command = arguments_[0] ?? 'check';
  let root = process.cwd();
  for (let index = 1; index < arguments_.length; index += 1) {
    if (arguments_[index] === '--root' && arguments_[index + 1]) {
      root = resolve(arguments_[index + 1]);
      index += 1;
    } else {
      throw new Error(`Unknown argument: ${arguments_[index]}`);
    }
  }
  return { command, root };
}

try {
  const { command, root } = parseArguments(process.argv.slice(2));
  if (command === 'check') {
    const result = await runArchitectureCheck({ root });
    const stream = result.exitCode === 0 ? process.stdout : process.stderr;
    stream.write(`${result.output}\n`);
    process.exitCode = result.exitCode;
  } else if (command === 'baseline') {
    process.stdout.write(`${JSON.stringify(await createBaseline({ root }), null, 2)}\n`);
  } else {
    throw new Error(`Unknown command: ${command}`);
  }
} catch (error) {
  process.stderr.write(`Architecture CLI error: ${error instanceof Error ? error.message : String(error)}\n`);
  process.exitCode = 2;
}
