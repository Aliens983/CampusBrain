/**
 * SSE 分帧解析（与 Vue 解耦，便于单元测试）.
 * <p>
 * 换行可能是 \n\n 或 \r\n\r\n；同一事件可有多行 data（按 \n 拼接）；
 * 以冒号开头的行是注释，忽略.
 */
export interface SseFrame {
  event: string
  data: string
}

export function parseSseFrames(raw: string): SseFrame[] {
  const frames: SseFrame[] = []
  for (const block of raw.split(/\r?\n\r?\n/)) {
    if (!block.trim()) continue
    let event = 'message'
    const dataLines: string[] = []
    for (const line of block.split(/\r?\n/)) {
      if (line.startsWith(':')) continue
      if (line.startsWith('event:')) event = line.slice(6).trim()
      else if (line.startsWith('data:')) dataLines.push(line.slice(5).replace(/^ /, ''))
    }
    if (dataLines.length > 0) frames.push({ event, data: dataLines.join('\n') })
  }
  return frames
}
