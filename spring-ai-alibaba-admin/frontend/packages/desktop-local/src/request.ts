import { notification } from 'antd';
import axios, { AxiosRequestConfig } from 'axios';

export const baseURL = process.env.WEB_SERVER || '';

export const request = axios.create({
  baseURL,
  timeout: 60 * 1000,
});

request.interceptors.response.use(
  (response) => response,
  (error) => {
    notification.error({
      message: error.response?.data?.code || error.message,
      description: error.response?.data?.message,
    });
    throw error;
  },
);

export function fetch<T = any>(config: AxiosRequestConfig) {
  return request.request<T>(config);
}
