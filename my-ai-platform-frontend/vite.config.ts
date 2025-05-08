/**
 * Vite 配置文件。
 * 用于配置Vite构建工具的行为，例如插件、开发服务器选项、构建输出等。
 * https://vitejs.dev/config/
 */
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
})
