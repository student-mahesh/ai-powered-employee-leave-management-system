import {defineConfig} from 'vite';

export default defineConfig(({mode}) => ({
  esbuild: {
    jsx: 'automatic'
  },
  server: {
    port: 5175,
    strictPort: true
  }
}));
