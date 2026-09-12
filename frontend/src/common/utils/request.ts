import axios, { type AxiosInstance, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/common/stores/user'

/**
 * 统一业务成功码。
 * CAS 的 CommonResult 与 KB 的 ApiResponse 已对齐为 200，
 * 前端所有成功判断都引用此常量，不要再写死字面量或双写兼容 0/200。
 */
export const API_SUCCESS_CODE = 200

const request: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
  // 注意：这里不全局设置 Content-Type，让 axios 按 data 类型自动选择：
  //   普通对象 → application/json
  //   FormData → multipart/form-data; boundary=...（文件上传必需）
  // 否则全局锁死 application/json 会导致文件上传报
  // "Current request is not a multipart request"
})

request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  },
)

request.interceptors.response.use(
  (response: AxiosResponse) => {
    const { data } = response

    if (typeof data === 'string') {
      return data
    }

    if (data.code !== undefined && data.code !== API_SUCCESS_CODE) {
      const msg = data.msg || data.message || '请求失败'
      // 不在此处弹错误，由组件自行处理，避免重复提示
      return Promise.reject(new Error(msg))
    }

    return data.data !== undefined ? data.data : data
  },
  (error) => {
    const status = error.response?.status
    const serverMsg = error.response?.data?.msg || error.response?.data?.message

    if (status === 401) {
      ElMessage.error('登录已过期，请重新登录')
      const userStore = useUserStore()
      userStore.logout()
      window.location.href = '/login'
    } else if (status === 400) {
      // 业务异常（参数错误 / 规则不满足）：CAS 现已按 HTTP 语义返回 400。
      // 错误文案交给调用组件展示，此处不弹，避免与组件提示重复。
      return Promise.reject(new Error(serverMsg || '请求失败'))
    } else if (status === 403) {
      ElMessage.error(serverMsg || '没有权限访问该资源')
    } else if (status === 404) {
      ElMessage.error(serverMsg || '请求的资源不存在')
    } else if (status === 500) {
      ElMessage.error(serverMsg || '服务器内部错误')
    } else if (status) {
      ElMessage.error(serverMsg || '网络请求失败')
    } else {
      // 无 status 的错误（网络中断等），返回通用消息
      return Promise.reject(error)
    }

    return Promise.reject(error)
  },
)

export default request