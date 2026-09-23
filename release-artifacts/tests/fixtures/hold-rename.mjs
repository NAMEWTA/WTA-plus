// Test-only preload. Holds the one release pointer rename until SIGTERM.
import fs from 'node:fs';

const marker = process.env.RELEASE_RENAME_HOLD;
if (marker) {
  const keepAlive = setInterval(() => {}, 1000);
  fs.rename = (source) => {
    fs.writeFileSync(marker, String(source));
    process.once('SIGTERM', () => clearInterval(keepAlive));
    process.once('SIGINT', () => clearInterval(keepAlive));
  };
}
