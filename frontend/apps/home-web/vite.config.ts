import vue from '@vitejs/plugin-vue';
import autoprefixer from 'autoprefixer';
import { defineConfig, loadEnv } from 'vite';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd());
  return {
    base: env.VITE_APP_CONTEXT_PATH,
    plugins: [
      vue(),
      {
        name: 'home-build-mode',
        apply: 'build',
        generateBundle() {
          this.emitFile({
            type: 'asset',
            fileName: 'build-mode.json',
            source: JSON.stringify({ app: 'home-web', mode })
          });
        }
      }
    ],
    resolve: { tsconfigPaths: true, extensions: ['.mjs', '.js', '.ts', '.jsx', '.tsx', '.json', '.vue'] },
    server: {
      host: '0.0.0.0',
      port: Number(env.VITE_APP_PORT),
      open: true,
      proxy: {
        [env.VITE_APP_BASE_API]: {
          target: env.VITE_APP_PROXY_TARGET || 'http://127.0.0.1:18080',
          changeOrigin: true,
          ws: true,
          rewrite: path => path.replace(new RegExp('^' + env.VITE_APP_BASE_API), '')
        }
      }
    },
    css: { postcss: { plugins: [autoprefixer()] } }
  };
});
