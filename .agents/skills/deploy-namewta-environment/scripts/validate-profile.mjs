#!/usr/bin/env node
import { parseArgs, readJson, requireArg, validateProfile } from './lib.mjs';

try {
  const args = parseArgs(process.argv.slice(2));
  const profileFile = requireArg(args, 'profile');
  const errors = validateProfile(readJson(profileFile));
  if (errors.length) {
    for (const error of errors) console.error(`[错误] ${error}`);
    process.exitCode = 1;
  } else {
    console.log(`[通过] 部署配置档案有效：${profileFile}`);
  }
} catch (error) {
  console.error(`[错误] ${error.message}`);
  process.exitCode = 1;
}
