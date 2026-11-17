import tailwindcss from '@tailwindcss/vite';
import react from '@vitejs/plugin-react';
import { defineConfig } from 'vitest/config';

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    // Local development: the backend runs on 8080 (spring-boot:run).
    proxy: { '/api': 'http://localhost:8080' },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    // docs/testing.md, Determinism: fixed locale and time zone.
    env: { TZ: 'Europe/Istanbul', LANG: 'tr_TR.UTF-8' },
  },
});
