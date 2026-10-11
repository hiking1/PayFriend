import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// The portal currently runs on mock data (src/services/api.ts).
// The proxy is ready for when it is pointed at the real backend.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
