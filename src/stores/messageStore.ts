import { defineStore } from 'pinia';
import { useMessage } from 'naive-ui';

/**
 * 消息状态接口
 */
interface MessageState {
  messageApi: any | null;
}

/**
 * 消息状态管理
 */
export const useMessageStore = defineStore('message', {
  state: (): MessageState => ({
    messageApi: null
  }),
  
  actions: {
    /**
     * 初始化消息API
     */
    initMessageApi() {
      this.messageApi = useMessage();
    },
    
    /**
     * 显示成功消息
     * @param content 消息内容
     */
    success(content: string) {
      if (!this.messageApi) {
        this.initMessageApi();
      }
      this.messageApi.success(content);
    },
    
    /**
     * 显示错误消息
     * @param content 消息内容
     */
    error(content: string) {
      if (!this.messageApi) {
        this.initMessageApi();
      }
      this.messageApi.error(content);
    },
    
    /**
     * 显示警告消息
     * @param content 消息内容
     */
    warning(content: string) {
      if (!this.messageApi) {
        this.initMessageApi();
      }
      this.messageApi.warning(content);
    },
    
    /**
     * 显示信息消息
     * @param content 消息内容
     */
    info(content: string) {
      if (!this.messageApi) {
        this.initMessageApi();
      }
      this.messageApi.info(content);
    }
  }
});
