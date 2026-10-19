/**
 * 本地时区日期工具.
 *
 * 统一收敛于此：此前 `new Date().toISOString().slice(0,10)` 被误用于"今天"
 * 判定——toISOString 是 UTC，北京时间 00:00–08:00 期间得到的是昨天，
 * 工作台「今日预约」在该时段恒为空。
 */

/** 本地时区 yyyy-MM-dd（避免 toISOString 的 UTC 跨天问题） */
export function localDate(d = new Date()): string {
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}