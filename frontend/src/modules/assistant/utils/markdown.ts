import { marked } from 'marked'
import DOMPurify from 'dompurify'

/**
 * AI 回复的 Markdown 渲染。
 *
 * - gfm：支持表格（余量/可约清单）、删除线
 * - breaks：单个换行即 <br>，贴合模型流式输出的换行习惯
 * - 流式过程中对半截 Markdown 也能渐进渲染（表格未闭合时先渲染已收到部分）
 *
 * 安全：marked v14 已内置移除原始 HTML，但 LLM 输出并非完全可信——
 * 回复会引用/回显知识库文档内容（管理员可上传任意文档）与用户提问，
 * 历史上 marked 版本与渲染链路也可能变化。渲染结果统一经 DOMPurify 消毒
 * （去 script/事件属性/javascript: 链接等），再交给 v-html，纵深防御 stored XSS。
 */
marked.setOptions({ gfm: true, breaks: true })

export function renderMarkdown(source: string): string {
  if (!source) {
    return ''
  }
  const rawHtml = marked.parse(source, { async: false }) as string
  return DOMPurify.sanitize(rawHtml, { ADD_ATTR: ['target'] })
}
