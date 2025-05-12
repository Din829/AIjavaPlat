/**
 * 消息服务
 * 提供全局消息提示功能，基于Naive UI的message API
 * 
 * 使用方法：
 * 1. 在组件中：
 *    import { useMessage } from 'naive-ui'
 *    const message = useMessage()
 *    message.success('操作成功')
 * 
 * 2. 在非组件中（如API拦截器）：
 *    import { message } from '@/services/messageService'
 *    message.error('请求失败')
 */

import { createDiscreteApi } from 'naive-ui'

// 创建离散API，这样可以在非组件环境中使用
const { message } = createDiscreteApi(['message'], {
  // 可以根据需要配置主题
  // configProviderProps: { theme: lightTheme }
})

// 导出message实例，可以在任何地方使用
export { message }

/**
 * 显示成功消息
 * @param content 消息内容
 */
export const showSuccess = (content: string) => {
  message.success(content)
}

/**
 * 显示错误消息
 * @param content 消息内容
 */
export const showError = (content: string) => {
  message.error(content)
}

/**
 * 显示警告消息
 * @param content 消息内容
 */
export const showWarning = (content: string) => {
  message.warning(content)
}

/**
 * 显示信息消息
 * @param content 消息内容
 */
export const showInfo = (content: string) => {
  message.info(content)
}

/**
 * 处理API错误并显示适当的消息
 * @param error API错误对象
 * @param defaultMessage 默认错误消息
 */
export const handleApiError = (error: any, defaultMessage: string = '操作失败，请稍后重试') => {
  if (error.response) {
    const { status, data } = error.response
    
    // 根据状态码显示不同的错误消息
    switch (status) {
      case 400:
        // 处理验证错误
        if (data.errors && Array.isArray(data.errors)) {
          // 如果有详细的验证错误信息，显示第一个
          showError(data.errors[0]?.defaultMessage || data.message || '请求参数无效')
        } else {
          // 一般的Bad Request错误
          showError(data.message || '请求参数无效')
        }
        break
      case 401:
        showError(data.message || '认证失败，请重新登录')
        break
      case 403:
        showError(data.message || '您没有权限执行此操作')
        break
      case 404:
        showError(data.message || '请求的资源不存在')
        break
      case 500:
      case 502:
      case 503:
      case 504:
        showError(data.message || '服务器错误，请稍后重试')
        break
      default:
        showError(data.message || defaultMessage)
    }
  } else if (error.request) {
    // 请求已发出但没有收到响应
    showError('网络错误，请检查您的连接')
  } else {
    // 请求设置时出错
    showError(error.message || defaultMessage)
  }
}

export default {
  message,
  showSuccess,
  showError,
  showWarning,
  showInfo,
  handleApiError
}
