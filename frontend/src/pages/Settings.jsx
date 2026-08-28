import { useState } from 'react'
import { Card } from './Overview'
import { API_URL } from '../config'
import CsvUpload from '../components/CsvUpload'

const ACCENTS = [
  { name: 'Violet', value: '#7c5cff' },
  { name: 'Teal', value: '#06d6d6' },
  { name: 'Pink', value: '#ff5c8a' },
  { name: 'Amber', value: '#ffb020' },
]

export default function Settings() {
  const [accent, setAccent] = useState('#7c5cff')

  const applyAccent = (color) => {
    document.documentElement.style.setProperty('--accent', color)
    setAccent(color)
  }

  return (
    <div style={{ display: 'grid', gap: 20, maxWidth: 560 }}>
      <Card title="Appearance">
        <div style={{ fontSize: 13, color: 'var(--text-dim)', marginBottom: 12 }}>
          Accent color (applies for this session only)
        </div>
        <div style={{ display: 'flex', gap: 10 }}>
          {ACCENTS.map(a => (
            <button key={a.value} onClick={() => applyAccent(a.value)} style={{
              width: 36, height: 36, borderRadius: 10, background: a.value, border: 'none',
              outline: accent === a.value ? '2px solid white' : 'none', outlineOffset: 2,
            }} title={a.name} />
          ))}
        </div>
      </Card>

      <CsvUpload />

      <Card title="Connection">
        <div style={{ display: 'grid', gap: 10, fontSize: 13 }}>
          <Row label="API URL" value={API_URL || 'http://localhost:8080 (via proxy)'} />
          <Row label="WebSocket" value="/ws (STOMP over SockJS)" />
          <Row label="Data pipeline" value="Kafka → Redis → WebSocket" />
          <Row label="AI model" value="Gemini (embeddings + chat)" />
        </div>
      </Card>
    </div>
  )
}

function Row({ label, value }) {
  return (
    <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: 8, borderBottom: '1px solid var(--border)' }}>
      <span style={{ color: 'var(--text-dim)' }}>{label}</span>
      <span style={{ fontFamily: 'monospace' }}>{value}</span>
    </div>
  )
}
