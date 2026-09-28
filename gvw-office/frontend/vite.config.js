import { defineConfig } from 'vite'
import { svelte } from '@sveltejs/vite-plugin-svelte'
import { alphaTab } from "@coderline/alphatab-vite";
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [
    svelte(),
    tailwindcss(),
    alphaTab({
      assetOutputDir: 'public/alphatab',
    }),
  ],
  optimizeDeps: {
    exclude: ['@coderline/alphatab']
  },
  server: {
    port: 5173,
    strictPort: true,
  },
  test: {
    environment: 'jsdom',
    include: ['src/**/*.{test,spec}.{js,ts}'],
    globals: true,
  },
})