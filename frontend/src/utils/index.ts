/**
 * 通用工具函数
 */

/**
 * 格式化日期时间
 * @param date 日期对象或时间戳
 * @param format 格式化字符串
 * @returns 格式化后的日期字符串
 */
export function formatDate(
  date: Date | number | string,
  format: string = 'YYYY-MM-DD HH:mm:ss'
): string {
  if (!date) return ''
  
  const d = new Date(date)
  if (isNaN(d.getTime())) return ''
  
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  const seconds = String(d.getSeconds()).padStart(2, '0')
  
  return format
    .replace('YYYY', String(year))
    .replace('MM', month)
    .replace('DD', day)
    .replace('HH', hours)
    .replace('mm', minutes)
    .replace('ss', seconds)
}

/**
 * 格式化日期（仅日期部分）
 * @param date 日期对象或时间戳
 * @returns 格式化后的日期字符串
 */
export function formatDateOnly(date: Date | number | string): string {
  return formatDate(date, 'YYYY-MM-DD')
}

/**
 * 格式化时间（仅时间部分）
 * @param date 日期对象或时间戳
 * @returns 格式化后的时间字符串
 */
export function formatTimeOnly(date: Date | number | string): string {
  return formatDate(date, 'HH:mm:ss')
}

/**
 * 格式化金额
 * @param amount 金额
 * @param decimals 小数位数
 * @returns 格式化后的金额字符串
 */
export function formatAmount(amount: number, decimals: number = 2): string {
  if (isNaN(amount)) return '0.00'
  
  return amount.toFixed(decimals)
}

/**
 * 生成随机ID
 * @param length ID长度
 * @returns 随机ID字符串
 */
export function generateId(length: number = 16): string {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'
  let result = ''
  
  for (let i = 0; i < length; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  
  return result
}

/**
 * 防抖函数
 * @param func 函数
 * @param wait 延迟时间（毫秒）
 * @returns 防抖后的函数
 */
export function debounce<T extends (...args: any[]) => any>(
  func: T,
  wait: number = 300
): (...args: Parameters<T>) => void {
  let timeout: NodeJS.Timeout | null = null
  
  return function (...args: Parameters<T>) {
    if (timeout) {
      clearTimeout(timeout)
    }
    
    timeout = setTimeout(() => {
      func(...args)
    }, wait)
  }
}

/**
 * 节流函数
 * @param func 函数
 * @param wait 延迟时间（毫秒）
 * @returns 节流后的函数
 */
export function throttle<T extends (...args: any[]) => any>(
  func: T,
  wait: number = 300
): (...args: Parameters<T>) => void {
  let lastTime = 0
  let timeout: NodeJS.Timeout | null = null
  
  return function (...args: Parameters<T>) {
    const now = Date.now()
    
    if (now - lastTime >= wait) {
      if (timeout) {
        clearTimeout(timeout)
        timeout = null
      }
      
      func(...args)
      lastTime = now
    } else if (!timeout) {
      timeout = setTimeout(() => {
        func(...args)
        lastTime = Date.now()
        timeout = null
      }, wait - (now - lastTime))
    }
  }
}

/**
 * 深拷贝
 * @param data 数据
 * @returns 深拷贝后的数据
 */
export function deepClone<T>(data: T): T {
  return JSON.parse(JSON.stringify(data))
}

/**
 * 检查邮箱格式
 * @param email 邮箱
 * @returns 是否合法
 */
export function isValidEmail(email: string): boolean {
  const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  return regex.test(email)
}

/**
 * 检查手机号格式
 * @param phone 手机号
 * @returns 是否合法
 */
export function isValidPhone(phone: string): boolean {
  const regex = /^1[3-9]\d{9}$/
  return regex.test(phone)
}

/**
 * 检查URL格式
 * @param url URL
 * @returns 是否合法
 */
export function isValidUrl(url: string): boolean {
  try {
    new URL(url)
    return true
  } catch {
    return false
  }
}

/**
 * 下载文件
 * @param data 文件数据
 * @param filename 文件名
 * @param mimeType MIME类型
 */
export function downloadFile(
  data: BlobPart,
  filename: string,
  mimeType: string = 'application/octet-stream'
): void {
  const blob = new Blob([data], { type: mimeType })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  
  a.href = url
  a.download = filename
  a.style.display = 'none'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  window.URL.revokeObjectURL(url)
}

/**
 * 复制文本到剪贴板
 * @param text 文本
 * @returns 是否成功
 */
export async function copyToClipboard(text: string): Promise<boolean> {
  try {
    await navigator.clipboard.writeText(text)
    return true
  } catch {
    // 兼容旧浏览器
    const textarea = document.createElement('textarea')
    textarea.value = text
    textarea.style.position = 'fixed'
    textarea.style.opacity = '0'
    document.body.appendChild(textarea)
    textarea.focus()
    textarea.select()
    
    try {
      document.execCommand('copy')
      document.body.removeChild(textarea)
      return true
    } catch {
      document.body.removeChild(textarea)
      return false
    }
  }
}

/**
 * 获取URL参数
 * @param name 参数名
 * @returns 参数值
 */
export function getUrlParam(name: string): string | null {
  const params = new URLSearchParams(window.location.search)
  return params.get(name)
}

/**
 * 设置URL参数
 * @param name 参数名
 * @param value 参数值
 */
export function setUrlParam(name: string, value: string): void {
  const params = new URLSearchParams(window.location.search)
  params.set(name, value)
  window.history.replaceState({}, '', `${window.location.pathname}?${params.toString()}`)
}

/**
 * 删除URL参数
 * @param name 参数名
 */
export function removeUrlParam(name: string): void {
  const params = new URLSearchParams(window.location.search)
  params.delete(name)
  window.history.replaceState({}, '', `${window.location.pathname}?${params.toString()}`)
}