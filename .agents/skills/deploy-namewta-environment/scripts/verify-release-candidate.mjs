#!/usr/bin/env node
import { parseArgs, readJson, requireArg, validateProfile } from './lib.mjs';
import { verifyState } from './verify-deployment-state.mjs';

try {
  const args = parseArgs(process.argv.slice(2));
  const profileFile = requireArg(args, 'profile');
  const stateFile = requireArg(args, 'state');
  const profile = readJson(profileFile);
  const state = readJson(stateFile);
  const errors = [...validateProfile(profile), ...verifyState(state, { profile })];
  if (errors.length) {
    for (const error of errors) console.error(`[失败] ${error}`);
    process.exitCode = 1;
  } else {
    console.log(`[通过] 发布候选满足 profile 与运行状态门禁：${stateFile}`);
  }
} catch (error) {
  console.error(`[错误] ${error.message}`);
  process.exitCode = 1;
}
