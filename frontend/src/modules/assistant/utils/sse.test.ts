import { describe, expect, it } from 'vitest'
import { parseSseFrames } from './sse'

describe('parseSseFrames', () => {
  it('parses default message frames', () => {
    expect(parseSseFrames('data: 你好\n\n')).toEqual([
      { event: 'message', data: '你好' },
    ])
  })

  it('keeps named events such as citations separate from message', () => {
    const raw = [
      'event: message',
      'data: 答案',
      '',
      'event: citations',
      'data: [{"documentTitle":"校园指南"}]',
      '',
      'data: [DONE]',
      '',
    ].join('\n')

    expect(parseSseFrames(raw)).toEqual([
      { event: 'message', data: '答案' },
      { event: 'citations', data: '[{"documentTitle":"校园指南"}]' },
      { event: 'message', data: '[DONE]' },
    ])
  })

  it('joins multi-line data and ignores comments', () => {
    const raw = ': heartbeat\nevent: slots\ndata: {"campus":"cq"}\ndata: {"date":"2026-10-31"}\n\n'
    expect(parseSseFrames(raw)).toEqual([
      { event: 'slots', data: '{"campus":"cq"}\n{"date":"2026-10-31"}' },
    ])
  })

  it('supports CRLF framing', () => {
    expect(parseSseFrames('event: message\r\ndata: ok\r\n\r\n')).toEqual([
      { event: 'message', data: 'ok' },
    ])
  })

  it('ignores frames without data', () => {
    expect(parseSseFrames('event: ping\n\n')).toEqual([])
  })
})
