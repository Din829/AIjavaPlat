import { defineStore } from 'pinia';
import { message } from '../services/messageService';

/**
 * 消息状态接口
 */
interface MessageState {
  // 无需存储状态，直接使用messageService
}

/**
 * 消息状态管理
 * 这是一个简单的包装器，将messageService包装成store的形式
 * 实际上直接使用messageService也可以
 */
export const useMessageStore = defineStore('message', {
  state: (): MessageState => ({}),

  actions: {
    /**
     * 显示成功消息
     * @param content 消息内容
     */
    success(content: string) {
      message.success(content);
    },

    /**
     * 显示错误消息
     * @param content 消息内容
     */
    error(content: string) {
      message.error(content);
    },

    /**
     * 显示警告消息
     * @param content 消息内容
     */
    warning(content: string) {
      message.warning(content);
    },

    /**
     * 显示信息消息
     * @param content 消息内容
     */
    info(content: string) {
      message.info(content);
    }
  }
});
