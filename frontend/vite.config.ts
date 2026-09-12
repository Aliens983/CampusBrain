import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'node:path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
      '@common': path.resolve(__dirname, './src/common'),
    },
  },
  server: {
    port: 3000,
    open: true,
    // 前导点 = 通配子域：覆盖 cpolar free 版每次重启随机分配的域名（r6.cpolar.cn / r34.cpolar.top 两种后缀）
    allowedHosts: ['.cpolar.cn', '.cpolar.top'],
    proxy: {
      // KB 路径已带 /api/v1/kb 前缀，直接透传给网关，不再加 /v1
      '/api/v1/kb': {
        target: 'http://localhost:8888',
        changeOrigin: true,
      },
      // CAS 路径（/api/captcha、/api/auth/** 等）补上 /v1，网关只认 /api/v1/**
      '/api': {
        target: 'http://localhost:8888',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '/api/v1'),
      },
    },
  },
  build: {
    outDir: 'dist',
    // 生产不生成 sourcemap，避免暴露源码与增大产物体积（开发 vite dev 不受影响）
    sourcemap: false,
    chunkSizeWarningLimit: 900,
    rollupOptions: {
      output: {
        manualChunks: {
          vue: ['vue', 'vue-router', 'pinia'],
          element: ['element-plus', '@element-plus/icons-vue'],
          vendor: ['axios', 'dayjs', 'echarts'],
        },
      },
    },
  },
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: `@use "@/assets/styles/variables.scss" as *;`,
      },
    },
  },
})
