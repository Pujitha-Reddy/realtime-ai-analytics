import { PieChart, Pie, Cell, Tooltip, Legend, ResponsiveContainer, BarChart, Bar, XAxis, YAxis, CartesianGrid } from 'recharts'
import { useLiveMetrics } from '../hooks/useLiveMetrics'
import { Card } from './Overview'

const STATUS_COLORS = { SUCCESS: '#22c55e', PENDING: '#ffb020', FAILED: '#f43f5e' }

function parseStatus(summary) {
  const match = summary.match(/\((SUCCESS|PENDING|FAILED)\)/)
  return match ? match[1] : null
}

export default function Analytics() {
  const metrics = useLiveMetrics()
  if (!metrics) return <div style={{ padding: 40, color: 'var(--text-dim)' }}>Waiting for live data…</div>

  const statusCounts = { SUCCESS: 0, PENDING: 0, FAILED: 0 }
  metrics.recentEvents.forEach(e => {
    const s = parseStatus(e)
    if (s) statusCounts[s]++
  })
  const statusData = Object.entries(statusCounts)
    .map(([name, value]) => ({ name, value }))
    .filter(d => d.value > 0)

  const categoryData = Object.entries(metrics.countByCategory)
    .map(([name, value]) => ({ name, value: Number(value) }))
    .filter(d => Number.isFinite(d.value))
    .sort((a, b) => b.value - a.value)

  return (
    <div style={{ display: 'grid', gap: 20 }}>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
        <Card title="Order Status (recent sample)">
          <ResponsiveContainer width="100%" height={280}>
            <PieChart>
              <Pie data={statusData} dataKey="value" nameKey="name" cx="50%" cy="45%" outerRadius={90}
                   isAnimationActive={false} labelLine={false}>
                {statusData.map((d, i) => <Cell key={i} fill={STATUS_COLORS[d.name]} stroke="var(--card)" strokeWidth={2} />)}
              </Pie>
              <Tooltip contentStyle={{ background: 'var(--card)', border: '1px solid var(--border)', borderRadius: 8, color: 'var(--text)' }} />
              <Legend wrapperStyle={{ fontSize: 12, color: 'var(--text-dim)' }} />
            </PieChart>
          </ResponsiveContainer>
          <div style={{ fontSize: 11, color: 'var(--text-dim)', marginTop: 8 }}>
            Based on the most recent {metrics.recentEvents.length} events, not the full history.
          </div>
        </Card>

        <Card title="Category Ranking">
          <ResponsiveContainer width="100%" height={280}>
            <BarChart data={categoryData} layout="vertical" margin={{ left: 20 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.06)" horizontal={false} />
              <XAxis type="number" stroke="var(--text-dim)" fontSize={12} tickLine={false} axisLine={false} />
              <YAxis type="category" dataKey="name" stroke="var(--text-dim)" fontSize={12} tickLine={false} axisLine={false} width={80} />
              <Bar dataKey="value" fill="var(--accent-2)" radius={[0, 6, 6, 0]} isAnimationActive={false} minPointSize={0} />
            </BarChart>
          </ResponsiveContainer>
        </Card>
      </div>

      <Card title="Snapshot">
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16, fontSize: 13 }}>
          <Stat label="Total Events" value={metrics.totalEvents.toLocaleString()} />
          <Stat label="Total Revenue" value={`$${metrics.totalRevenue.toFixed(2)}`} />
          <Stat label="Avg Order Value" value={`$${metrics.avgOrderValue.toFixed(2)}`} />
          <Stat label="Success Rate" value={`${metrics.successRate.toFixed(1)}%`} />
        </div>
      </Card>
    </div>
  )
}

function Stat({ label, value }) {
  return (
    <div>
      <div style={{ color: 'var(--text-dim)', fontSize: 12, marginBottom: 4 }}>{label}</div>
      <div style={{ fontWeight: 700, fontSize: 16 }}>{value}</div>
    </div>
  )
}
