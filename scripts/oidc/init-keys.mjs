import { randomBytes } from 'node:crypto';
import { mkdir, writeFile, stat, rm } from 'node:fs/promises';
import { resolve } from 'node:path';
import { pathToFileURL } from 'node:url';

/** 仅初始化空目录中的根密钥；签名和状态密钥由认证服务管理页加密写入数据库。 */
export async function initializeOidcKeys(directory) {
  const destination = resolve(directory);
  try { await stat(destination); throw new Error('目标目录已存在，请使用新的空目录；轮换须保留旧公钥。'); }
  catch (error) { if (error?.code !== 'ENOENT') throw error; }
  await mkdir(destination, { mode: 0o700, recursive: false });
  try {
    const env = `AUTH_CONFIG_ROOT_KEY=${randomBytes(32).toString('base64')}\n`;
    await writeFile(resolve(destination, 'auth.env'), env, { mode: 0o600, flag: 'wx' });
    return { directory: destination };
  } catch (error) {
    // 只清理本次创建的尚未完成目录，不操作任何既有密钥。
    await rm(destination, { recursive: true, force: true });
    throw error;
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(resolve(process.argv[1])).href) {
  const [, , option, directory, ...extra] = process.argv;
  if (option !== '--directory' || !directory || extra.length) {
    process.stderr.write('用法：node scripts/oidc/init-keys.mjs --directory <尚不存在的运行目录>\n');
    process.exitCode = 1;
  } else {
    try {
      const result = await initializeOidcKeys(directory);
      process.stdout.write(`认证根密钥已生成：${result.directory}/auth.env。注入 AUTH_CONFIG_ROOT_KEY 后，在管理页生成签名/状态密钥，勿提交到源码。\n`);
    } catch (error) {
      process.stderr.write(error instanceof Error ? `${error.message}\n` : 'OIDC 密钥初始化失败\n');
      process.exitCode = 1;
    }
  }
}
