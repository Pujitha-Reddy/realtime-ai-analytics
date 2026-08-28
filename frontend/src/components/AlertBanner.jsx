import { AlertTriangle, AlertOctagon } from 'lucide-react'
import { useAlerts } from '../hooks/useAlerts'

const STYLE = {
  WARNING: { color: '#ffb020', bg: 'rgba(255,176,32,0.1)', border: 'rgba(255,176,32,0.3)', icon: AlertTriangle },
  CRITICAL: { color: '#f43f5e', bg: 'rgba(244,63,94,0.1)', border: 'rgba(244,63,94,0.3)', icon: AlertOctagon },
}

export default function AlertBanner() {
  const alerts = useAlerts()
  if (alerts.length === 0) return null

  return (
    <div style={{ display: 'grid', gap: 8, marginBottom: 20 }}>
      {alerts.map(a => {
        const s = STYLE[a.severity] || STYLE.WARNING
        const Icon = s.icon
        return (
          <div key={a.id} style={{
            display: 'flex', alignItems: 'center', gap: 10,
            padding: '11px 16px', borderRadius: 10,
            background: s.bg, border: `1px solid ${s.border}`, color: s.color,
            fontSize: 13, fontWeight: 500,
          }}>
            <Icon size={16} />
            {a.message}
          </div>
        )
      })}
    </div>
  )
}
