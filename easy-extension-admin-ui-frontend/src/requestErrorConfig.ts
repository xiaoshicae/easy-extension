import type { RequestConfig } from '@umijs/max';
import { message } from 'antd';

// 与后端约定的响应数据格式
interface ResponseStructure {
  data: any;
  message?: string;
  code?: string;
}

const SUCCESS_CODE: string = 'A00000';
// const ERROR_CODE: string = 'B00000';

/**
 * 错误处理
 * @doc https://umijs.org/docs/max/request#配置
 */
export const errorConfig: RequestConfig = {
  errorConfig: {
    // 错误抛出
    errorThrower: (res) => {
      const { code, data, message } = res as unknown as ResponseStructure;
      if (code !== SUCCESS_CODE) {
        const error: any = new Error(message);
        error.name = 'BizError';
        error.info = { code, data, message };
        throw error;
      }
    },
    // 错误接收及处理
    errorHandler: (error: any, opts: any) => {
      if (opts?.skipErrorHandler) throw error;
      // 我们的 errorThrower 抛出的错误。
      if (error.name === 'BizError') {
        const errorInfo: ResponseStructure | undefined = error.info;
        if (errorInfo) {
          const { code, message: errorMessage } = errorInfo;
          console.log(
            'Response BizError occurred,  code=' + code + ', errorMessage=' + errorMessage,
          );
        }
      } else if (error.response) {
        // Axios 的错误
        // 请求成功发出且服务器也响应了状态码，但状态代码超出了 2xx 的范围
        message.error(`Response status:${error.response.status}`);
      } else if (error.request) {
        // 请求已经成功发起，但没有收到响应
        // \`error.request\` 在浏览器中是 XMLHttpRequest 的实例，
        // 而在node.js中是 http.ClientRequest 的实例
        message.error('None response! Please retry.');
      } else {
        // 发送请求时出了点问题
        message.error('Request error, please retry.');
      }
    },
  },

  // 请求拦截器
  requestInterceptors: [
    // (config: RequestOptions) => {
    //   // 拦截请求配置，进行个性化处理。
    //   const url = config?.url?.concat('?token = 123');
    //   return { ...config, url };
    // },
  ],

  // 响应拦截器
  responseInterceptors: [
    (response) => {
      // 拦截响应数据，进行个性化处理
      const { data } = response as unknown as ResponseStructure;
      if (data?.code !== SUCCESS_CODE) {
        message.error('请求失败 ₍ᐢ..ᐢ₎');
      }
      return response;
    },
  ],
};
