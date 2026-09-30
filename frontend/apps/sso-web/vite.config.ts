import vue from '@vitejs/plugin-vue';
import { readFile } from 'node:fs/promises';
import { defineConfig, loadEnv } from 'vite';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd());
  return {
    base: env.VITE_APP_CONTEXT_PATH || '/',
    plugins: [
      vue(),
      {
        name: 'sso-build-mode',
        apply: 'build',
        async generateBundle() {
          this.emitFile({
            type: 'asset',
            fileName: 'oidc-theme.css',
            source: await readFile(new URL(import.meta.resolve('@namewta/web-kit-ui-element/theme.css')), 'utf8')
          });
          this.emitFile({
            type: 'asset',
            fileName: 'build-mode.json',
            source: JSON.stringify({ app: 'sso-web', mode })
          });
        }
      }
    ],
    resolve: { tsconfigPaths: true, extensions: ['.mjs', '.js', '.ts', '.jsx', '.tsx', '.json', '.vue'] },
    server: {
      host: '0.0.0.0',
      port: Number(env.VITE_APP_PORT || 4176),
      open: false,
      proxy: {
        '/oidc': {
          target: env.VITE_SSO_API_PROXY || 'http://127.0.0.1:38888',
          changeOrigin: true
        },
        '/.well-known': {
          target: env.VITE_SSO_API_PROXY || 'http://127.0.0.1:38888',
          changeOrigin: true
        },
        '/sso': {
          target: env.VITE_SSO_API_PROXY || 'http://127.0.0.1:38888',
          changeOrigin: true
        }
      }
    },
    preview: {
      host: '127.0.0.1',
      port: Number(env.VITE_APP_PORT || 4176),
      strictPort: true
    }
  };
});
