import { defineConfig, loadEnv } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd());
  return {
    base: env.VITE_APP_CONTEXT_PATH || '/',
    plugins: [vue()],
    resolve: { tsconfigPaths: true, extensions: ['.mjs', '.js', '.ts', '.jsx', '.tsx', '.json', '.vue'] },
    server: {
      host: '0.0.0.0',
      port: Number(env.VITE_APP_PORT || 4176),
      open: false,
      proxy: {
        '/sso': {
          target: env.VITE_SSO_API_PROXY || 'http://127.0.0.1:18080',
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
