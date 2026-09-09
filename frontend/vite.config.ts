import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const proxy = env.API_PROXY_TARGET
    ? { '/api': { target: env.API_PROXY_TARGET } }
    : undefined;
  return { plugins: [react()], server: { proxy }, build: { target: 'es2020' } };
});
