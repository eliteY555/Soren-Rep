import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'

// Global marked configuration — called ONCE at app startup
marked.setOptions({ breaks: true, gfm: true })

// Code block extension with syntax highlighting and copy button
const codeExtension = {
  name: 'codeBlock',
  level: 'block',
  renderer(token) {
    const code = token.text || ''
    const lang = token.lang || ''
    const langLabel = lang ? `<span class="code-lang">${lang}</span>` : ''
    const escaped = code.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')
    let highlighted
    try {
      highlighted = lang && hljs.getLanguage(lang)
        ? hljs.highlight(code, { language: lang }).value
        : hljs.highlightAuto(code).value
    } catch (_) {
      highlighted = escaped
    }
    return `<div class="code-block">${langLabel}<button class="code-copy" data-code="${escaped}">
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
    </button><pre><code class="hljs">${highlighted}</code></pre></div>`
  }
}

marked.use({ extensions: [codeExtension] })

/**
 * Safe parse — wraps marked.parse() in try-catch so incomplete
 * streaming markdown never crashes the app.
 */
export function safeMarkdown(content) {
  if (!content) return ''
  try {
    return marked.parse(content)
  } catch (e) {
    console.warn('Markdown parse error:', e.message)
    // Fallback: escape HTML and wrap in paragraphs
    const escaped = content.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    return escaped.split('\n\n').map(p => `<p>${p}</p>`).join('')
  }
}

/**
 * Streaming-safe markdown render.
 *
 * During streaming, the last paragraph is likely incomplete (unclosed bold,
 * half-written code blocks, orphan `---`). We split on \n\n boundaries:
 *   - Complete paragraphs → full markdown rendering
 *   - In-progress final paragraph → escaped plain text
 *
 * Once a \n\n appears, the paragraph graduates to markdown on the next token.
 */
export function safeStreamingMarkdown(content) {
  if (!content) return ''

  // Find the last paragraph boundary
  const lastBreak = content.lastIndexOf('\n\n')
  if (lastBreak === -1) {
    // Single paragraph — could be mid-sentence, just escape to avoid artifacts
    const escaped = content.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    return `<p>${escaped}</p>`
  }

  const complete = content.slice(0, lastBreak)
  const inProgress = content.slice(lastBreak + 2)

  try {
    const rendered = marked.parse(complete)
    if (!inProgress.trim()) return rendered // trailing newlines, no new content yet

    const escaped = inProgress.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    return rendered + '\n<p>' + escaped + '</p>'
  } catch (e) {
    console.warn('Streaming markdown error:', e.message)
    const escaped = content.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    return escaped.split('\n\n').map(p => `<p>${p}</p>`).join('')
  }
}

export default marked
