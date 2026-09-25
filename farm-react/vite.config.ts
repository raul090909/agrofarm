import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Запросы /api и /export проксируются на бэкенд Spring Boot (порт 8090)
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': { target: 'http://127.0.0.1:8090', changeOrigin: true },
      '/export': { target: 'http://127.0.0.1:8090', changeOrigin: true },
    },
  },
})
