import { useState } from 'react'
import axios from 'axios'
import { API_URL } from '../config'

export default function ChatPanel() {
  const [question, setQuestion] = useState('')
  const [messages, setMessages] = useState([])
  const [loading, setLoading] = useState(false)

  const ask = async () => {
    if (!question.trim()) return
    const q = question
    setMessages(prev => [...prev, { role: 'user', text: q }])
    setQuestion('')
    setLoading(true)
    try {
      const res = await axios.post(`${API_URL}/api/chat`, { question: q })
      setMessages(prev => [...prev, { role: 'assistant', text: res.data.answer }])
    } catch (e) {
      setMessages(prev => [...prev, { role: 'assistant', text: 'Error contacting assistant.' }])
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ marginTop: '2rem', border: '1px solid #ddd', borderRadius: 8, padding: '1rem', maxWidth: 600 }}>
      <h3>Ask the Analytics Assistant</h3>
      <div style={{ maxHeight: 250, overflowY: 'auto', marginBottom: '1rem' }}>
        {messages.map((m, i) => (
          <p key={i}><b>{m.role === 'user' ? 'You' : 'Assistant'}:</b> {m.text}</p>
        ))}
        {loading && <p><i>Assistant is thinking...</i></p>}
      </div>
      <input
        value={question}
        onChange={e => setQuestion(e.target.value)}
        onKeyDown={e => e.key === 'Enter' && ask()}
        placeholder="e.g. Which region is driving the most revenue right now?"
        style={{ width: '80%', padding: '0.5rem' }}
      />
      <button onClick={ask} style={{ padding: '0.5rem 1rem', marginLeft: 8 }}>Ask</button>
    </div>
  )
}
