import { marked } from 'marked'

/**
 * AI 回复的 Markdown 渲染。
 *
 * - gfm：支持表格（余量/可约清单）、删除线
 * - breaks：单个换行即 <br>，贴合模型流式输出的换行习惯
 * - 流式过程中对半截 Markdown 也能渐进渲染（表格未闭合时先渲染已收到部分）
 *
 * 内容来自自有后端的 LLM 输出，非第三方用户富文本，按可信内容处理。
 */
marked.setOptions({ gfm: true, breaks: true })

export function renderMarkdown(source: string): string {
  if (!source) {
    return ''
  }
  return marked.parse(source, { async: false }) as string
}
