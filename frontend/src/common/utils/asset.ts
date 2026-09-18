/** /uploads/xx → /api/uploads/xx（走 vite 代理到网关） */
export function assetUrl(p?: string): string {
  if (!p) return ''
  if (/^https?:/.test(p)) return p
  if (p.startsWith('/uploads')) return `/api${p}`
  return p
}