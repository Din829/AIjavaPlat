/// <reference types="vite/client" />

/**
 * 1. 添加 Vue 组件的类型声明
 * - 解决 TypeScript 无法识别 Vue 模板中 HTML 元素的问题
 * - 这是 Vue 3 + TypeScript 项目的标准类型声明
 * - 帮助 Volar 扩展正确识别和检查模板语法
 */
declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

/**
 * 2. 添加静态资源模块的类型声明
 * - 使 TypeScript 能够正确识别导入的静态资源
 * - 解决图片引用路径的类型检查问题
 */
declare module '*.svg' {
  const content: string
  export default content
}
