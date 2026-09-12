#!/usr/bin/env node
import crypto from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { parseArgs, readJson, requireArg, validateProfile } from './lib.mjs';

function relevantAsset(value) {
  return /\.(?:css|js)(?:[?#].*)?$/iu.test(value);
}

function assetPaths(html) {
  const results = [];
  for (const match of html.matchAll(/<(?:script|link)\b[^>]*>/giu)) {
    const attribute = match[0].match(/\b(?:src|href)\s*=\s*(["'])(.*?)\1/iu);
    if (attribute && relevantAsset(attribute[2])) results.push(attribute[2]);
  }
  return results;
}

export function verifyFrontendHtml(html, profile) {
  const errors = [];
  const paths = assetPaths(html);
  const prefix = profile.release?.frontend?.assetPrefix;
  if (paths.length === 0) errors.push('index.html 未发现 JavaScript 或 CSS 资源');
  for (const value of paths) {
    const pathname = value.split(/[?#]/u, 1)[0];
    if (!prefix || !pathname.startsWith(prefix)) errors.push(`前端资源不符合 assetPrefix ${prefix ?? '未配置'}：${value}`);
  }
  return {
    errors,
    assetPaths: paths,
    indexSha256: crypto.createHash('sha256').update(html).digest('hex'),
  };
}

if (process.argv[1] && fileURLToPath(import.meta.url) === path.resolve(process.argv[1])) {
  try {
    const args = parseArgs(process.argv.slice(2));
    const profile = readJson(requireArg(args, 'profile'));
    const indexFile = requireArg(args, 'index');
    const html = fs.readFileSync(indexFile, 'utf8');
    const result = verifyFrontendHtml(html, profile);
    const errors = [...validateProfile(profile), ...result.errors];
    if (errors.length) {
      for (const error of errors) console.error(`[失败] ${error}`);
      process.exitCode = 1;
    } else {
      console.log(`[通过] 前端资源前缀有效；index SHA-256=${result.indexSha256}`);
    }
  } catch (error) {
    console.error(`[错误] ${error.message}`);
    process.exitCode = 1;
  }
}
