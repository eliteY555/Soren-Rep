/**
 * 纯文本流式读取器
 *
 * 后端: OutputStream.write(token) → flush() → TCP 逐包推送
 * 前端: ReadableStream reader.read() → 每个 chunk = 一个或多个 token
 *
 * streaming-markdown-solution 核心设计:
 *   流式期间 — 纯文本渲染 (<br> + HTML 转义)，不调 marked.parse
 *   流结束后 — marked.parse 全量解析一次
 */

/**
 * 从 ReadableStream 读取纯文本 token 流
 */
export async function parseStream(stream, { onToken, onDone, onError }) {
  try {
    const reader = stream.getReader()
    const decoder = new TextDecoder('utf-8')

    while (true) {
      const { done, value } = await reader.read()
      if (done) {
        onDone && onDone()
        break
      }
      const chunk = decoder.decode(value, { stream: true })
      if (chunk) onToken && onToken(chunk)
    }
  } catch (err) {
    if (err.name !== 'AbortError') {
      console.error('Stream error:', err)
      onError && onError(err)
    }
    onDone && onDone()
  }
}

/** 转义 HTML 特殊字符 */
export function escapeHTML(s) {
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
}

export { parseStream as parseSSEStream }
