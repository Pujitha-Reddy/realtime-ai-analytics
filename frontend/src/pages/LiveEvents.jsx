import { useLiveMetrics } from '../hooks/useLiveMetrics'
import { Card } from './Overview'

const STATUS_STYLE = {
  SUCCESS: { color: '#22c55e', bg: 'rgba(34,197,94,0.12)' },
  PENDING: { color: '#ffb020', bg: 'rgba(255,176,32,0.12)' },
  FAILED:  { color: '#f43f5e', bg: 'rgba(244,63,94,0.12)' },
}

function parseEvent(summary) {
  const m = summary.match(/^\[(.+?)\]\s+(\w+) order in (.+?) worth \$([\d.]+) \((\w+)\)$/)
  if (!m) return null
  const [, timestamp, category, region, amount, status] = m
  return { timestamp, category, region, amount, status }
}

export default function LiveEvents() {
  const metrics = useLiveMetrics()
  if (!metrics) return <div style={{ padding: 40, color: 'var(--text-dim)' }}>Waiting for live data…</div>

  return (
    <Card title={`Live Event Stream (${metrics.recentEvents.length} most recent)`}>
      <div style={{ display: 'grid', gap: 4, maxHeight: 640, overflowY: 'auto' }}>
        <div style={{
          display: 'grid', gridTemplateColumns: '1.6fr 1fr 1fr 0.8fr 0.8fr', gap: 12,
          padding: '8px 10px', fontSize: 11, color: 'var(--text-dim)', textTransform: 'uppercase',
          borderBottom: '1px solid var(--border)',
        }}>
          <span>Timestamp</span><span>Category</span><span>Region</span><span>Amount</span><span>Status</span>
        </div>
        {metrics.recentEvents.map((raw, i) => {
          const e = parseEvent(raw)
          if (!e) return null
          const style = STATUS_STYLE[e.status] || {}
          return (
            <div key={i} style={{
              display: 'grid', gridTemplateColumns: '1.6fr 1fr 1fr 0.8fr 0.8fr', gap: 12,
              padding: '9px 10px', fontSize: 12.5, borderRadius: 6,
              background: i % 2 === 0 ? 'rgba(255,255,255,0.02)' : 'transparent',
              alignItems: 'center',
            }}>
              <span style={{ fontFamily: 'monospace', color: 'var(--text-dim)' }}>{e.timestamp}</span>
              <span>{e.category}</span>
              <span>{e.region}</span>
              <span>${e.amount}</span>
              <span style={{
                color: style.color, background: style.bg, padding: '3px 8px',
                borderRadius: 6, fontSize: 11, fontWeight: 600, width: 'fit-content',
              }}>{e.status}</span>
            </div>
          )
        })}
      </div>
    </Card>
  )
}
