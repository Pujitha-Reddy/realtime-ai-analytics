import { useState } from 'react'
import { MessageCircle, X, Send, Sparkles } from 'lucide-react'
import { API_URL } from '../config'

export default function ChatDock() {
  const [open, setOpen] = useState(true)
  const [question, setQuestion] = useState('')
  const [messages, setMessages] = useState([])
  const [streaming, setStreaming] = useState(false)

  const ask = () => {
    if (!question.trim() || streaming) return
    const q = question
    setQuestion('')
    setMessages(prev => [...prev, { role: 'user', text: q }, { role: 'assistant', text: '' }])
    setStreaming(true)

    const es = new EventSource(`${API_URL}/api/chat/stream?question=${encodeURIComponent(q)}`)

    es.onmessage = (event) => {
      setMessages(prev => {
        const updated = [...prev]
        const last = updated[updated.length - 1]
        updated[updated.length - 1] = { ...last, text: last.text + event.data }
        return updated
      })
    }

    es.onerror = () => {
      es.close()
      setStreaming(false)
    }

    // SSE has no built-in "done" event by default; close once the connection
    // naturally ends (server completes the Flux, browser fires onerror/close).
    es.addEventListener('close', () => {
      es.close()
      setStreaming(false)
    })
  }

  if (!open) {
    return (
      <button onClick={() => setOpen(true)} style={{
        position: 'fixed', bottom: 24, right: 24, width: 54, height: 54, borderRadius: '50%',
        background: 'linear-gradient(135deg, var(--accent), var(--accent-3))', border: 'none',
        display: 'flex', alignItems: 'center', justifyContent: 'center', boxShadow: '0 8px 24px rgba(124,92,255,0.4)',
      }}>
        <MessageCircle size={22} color="white" />
      </button>
    )
  }

  return (
    <div style={{
      width: 'var(--chat-width)', height: '100vh', position: 'fixed', right: 0, top: 0,
      background: 'var(--bg-elevated)', borderLeft: '1px solid var(--border)',
      display: 'flex', flexDirection: 'column',
    }}>
      <div style={{
        padding: '18px 18px 14px', borderBottom: '1px solid var(--border)',
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontWeight: 600, fontSize: 14 }}>
          <Sparkles size={16} color="var(--accent)" />
          Analytics Assistant
        </div>
        <button onClick={() => setOpen(false)} style={{ background: 'none', border: 'none', color: 'var(--text-dim)' }}>
          <X size={18} />
        </button>
      </div>

      <div style={{ flex: 1, overflowY: 'auto', padding: 18, display: 'flex', flexDirection: 'column', gap: 12 }}>
        {messages.length === 0 && (
          <div style={{ color: 'var(--text-dim)', fontSize: 13 }}>
            Ask about live revenue, categories, regions, or trends.
          </div>
        )}
        {messages.map((m, i) => (
          <div key={i} style={{
            alignSelf: m.role === 'user' ? 'flex-end' : 'flex-start',
            maxWidth: '90%', padding: '10px 13px', borderRadius: 12, fontSize: 13, lineHeight: 1.5,
            background: m.role === 'user' ? 'var(--accent)' : 'var(--card)',
            color: m.role === 'user' ? 'white' : 'var(--text)',
            border: m.role === 'user' ? 'none' : '1px solid var(--border)',
            whiteSpace: 'pre-wrap',
          }}>
            {m.text || (streaming && i === messages.length - 1 ? '…' : '')}
          </div>
        ))}
      </div>

      <div style={{ padding: 14, borderTop: '1px solid var(--border)', display: 'flex', gap: 8 }}>
        <input
          value={question}
          onChange={e => setQuestion(e.target.value)}
          onKeyDown={e => e.key === 'Enter' && ask()}
          placeholder="Ask a question…"
          disabled={streaming}
          style={{
            flex: 1, background: 'var(--card)', border: '1px solid var(--border)', borderRadius: 8,
            padding: '9px 12px', color: 'var(--text)', fontSize: 13, outline: 'none',
          }}
        />
        <button onClick={ask} disabled={streaming} style={{
          width: 36, height: 36, borderRadius: 8, background: 'var(--accent)', border: 'none',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          opacity: streaming ? 0.5 : 1,
        }}>
          <Send size={15} color="white" />
        </button>
      </div>
    </div>
  )
}
