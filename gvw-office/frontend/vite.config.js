import { defineConfig } from 'vite'
import { svelte } from '@sveltejs/vite-plugin-svelte'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [
    svelte(),
    tailwindcss(),
  ],
  server: {
    port: 5173,
    strictPort: true,
  },
  define: {
    __API_URL__: JSON.stringify(process.env.VITE_API_URL ?? "http://localhost:3500")
  }
})
