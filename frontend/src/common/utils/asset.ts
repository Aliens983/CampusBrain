/** 静态资源统一使用与文件服务一致的同源 /uploads 路径。 */
export function assetUrl(p?: string): string {
  if (!p) return ''
  if (/^https?:/.test(p)) return p
  return p
}
