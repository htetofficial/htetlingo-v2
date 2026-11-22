import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  base: '/htetlingo-v2/',
  plugins: [react()],
  build: {
    target: 'esnext',
    outDir: 'dist',
  },
});
