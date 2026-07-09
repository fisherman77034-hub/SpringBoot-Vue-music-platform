import axios, { type AxiosInstance } from 'axios'
import { useAuthStore } from '@/stores/auth'
import { getBackendOrigin } from '@/utils/media'

/** 响应拦截器已解包为业务 data，与 axios 默认泛型不一致 */
export type HttpClient = Pick<AxiosInstance, 'defaults' | 'interceptors'> & {
  get<T = unknown>(url: string, config?: object): Promise<T>
  post<T = unknown>(url: string, data?: unknown, config?: object): Promise<T>
  put<T = unknown>(url: string, data?: unknown, config?: object): Promise<T>
  delete<T = unknown>(url: string, config?: object): Promise<T>
}

const raw = axios.create({
  timeout: 15000,
  baseURL: typeof window !== 'undefined' ? getBackendOrigin() || undefined : undefined
})

raw.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers = config.headers ?? {}
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

raw.interceptors.response.use(
  (resp) => {
    const data = resp.data
    if (data && typeof data === 'object' && 'code' in data) {
      if (data.code !== 0) {
        return Promise.reject(new Error(data.message || '请求失败'))
      }
      return data.data
    }
    return data
  },
  (err) => {
    const data = err?.response?.data
    if (data && typeof data === 'object' && 'message' in data && data.message) {
      return Promise.reject(new Error(String(data.message)))
    }
    return Promise.reject(err)
  }
)

export const http = raw as HttpClient

