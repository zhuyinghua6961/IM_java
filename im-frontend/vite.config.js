import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 5173,
    proxy: {
      // 用户服务和好友服务接口
      '/api/user': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/api/friend': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/api/whitelist': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/api/group': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      // 消息服务接口
      '/api/conversation': {
        target: 'http://localhost:8082',
        changeOrigin: true
      },
      '/api/message': {
        target: 'http://localhost:8082',
        changeOrigin: true
      },
      '/api/notification': {
        target: 'http://localhost:8082',
        changeOrigin: true
      },
      // WebSocket连接
      '/ws': {
        target: 'ws://localhost:8082',
        ws: true,
        changeOrigin: true
      }
    }
  }
})
