import { BarChart, Bar, PieChart, Pie, Cell, XAxis, YAxis, Tooltip, Legend, CartesianGrid, ResponsiveContainer } from 'recharts'
import { TrendingUp, DollarSign, Percent, Activity } from 'lucide-react'
import { useLiveMetrics } from '../hooks/useLiveMetrics'
import AlertBanner from '../components/AlertBanner'
import TrendChart from '../components/TrendChart'
import SimulatorToggle from '../components/SimulatorToggle'

const PIE_COLORS = ['#7c5cff', '#06d6d6', '#ff5c8a', '#ffb020']

export default function Overview() {
  const metrics = useLiveMetrics()
  if (!metrics) return <div style={{ padding: 40, color: 'var(--text-dim)' }}>Waiting for live data…</div>

  const categoryData = Object.entries(metrics.countByCategory)
    .map(([name, value]) => ({ name, value: Number(value) }))
    .filter(d => Number.isFinite(d.value))

  const regionData = Object.entries(metrics.revenueByRegion)
    .map(([name, value]) => ({ name, value: Math.round(Number(value)) }))
    .filter(d => Number.isFinite(d.value) && d.value > 0)

  return (
    <div style={{ display: 'grid', gap: 20 }}>
      <AlertBanner />

      <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
        <SimulatorToggle />
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16 }}>
        <StatCard icon={Activity} color="var(--accent)" label="Total Events" value={metrics.totalEvents.toLocaleString()} />
        <StatCard icon={DollarSign} color="var(--accent-2)" label="Total Revenue" value={`$${metrics.totalRevenue.toLocaleString(undefined, {maximumFractionDigits:0})}`} />
        <StatCard icon={TrendingUp} color="var(--accent-4)" label="Avg Order Value" value={`$${metrics.avgOrderValue.toFixed(2)}`} />
        <StatCard icon={Percent} color="var(--success)" label="Success Rate" value={`${metrics.successRate.toFixed(1)}%`} />
      </div>

      <TrendChart hours={24} />

      <div style={{ display: 'grid', gridTemplateColumns: '1.3fr 1fr', gap: 16 }}>
        <Card title="Events by Category">
          <ResponsiveContainer width="100%" height={280}>
            <BarChart data={categoryData}>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.06)" vertical={false} />
              <XAxis dataKey="name" stroke="var(--text-dim)" fontSize={12} tickLine={false} axisLine={false} />
              <YAxis stroke="var(--text-dim)" fontSize={12} tickLine={false} axisLine={false} />
              <Tooltip contentStyle={{ background: 'var(--card)', border: '1px solid var(--border)', borderRadius: 8, color: 'var(--text)' }} />
              <Bar dataKey="value" fill="var(--accent)" radius={[6, 6, 0, 0]} isAnimationActive={false} minPointSize={0} />
            </BarChart>
          </ResponsiveContainer>
        </Card>

        <Card title="Revenue by Region">
          {regionData.length > 0 && (
            <ResponsiveContainer width="100%" height={280}>
              <PieChart>
                <Pie
                  data={regionData} dataKey="value" nameKey="name"
                  cx="50%" cy="45%" innerRadius={55} outerRadius={85}
                  startAngle={0} endAngle={360} paddingAngle={2} minAngle={0}
                  labelLine={false} isAnimationActive={false}
                >
                  {regionData.map((_, i) => <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]} stroke="var(--card)" strokeWidth={2} />)}
                </Pie>
                <Tooltip contentStyle={{ background: 'var(--card)', border: '1px solid var(--border)', borderRadius: 8, color: 'var(--text)' }} />
                <Legend wrapperStyle={{ fontSize: 12, color: 'var(--text-dim)' }} />
              </PieChart>
            </ResponsiveContainer>
          )}
        </Card>
      </div>

      <Card title="Recent Events">
        <div style={{ maxHeight: 220, overflowY: 'auto', display: 'grid', gap: 6 }}>
          {metrics.recentEvents.map((e, i) => (
            <div key={i} style={{
              fontFamily: 'monospace', fontSize: 12, color: 'var(--text-dim)',
              padding: '6px 10px', borderRadius: 6, background: i % 2 === 0 ? 'rgba(255,255,255,0.02)' : 'transparent',
            }}>{e}</div>
          ))}
        </div>
      </Card>
    </div>
  )
}

export function StatCard({ icon: Icon, color, label, value }) {
  return (
    <div style={{
      background: 'var(--card)', border: '1px solid var(--border)', borderRadius: 14,
      padding: '18px 20px', display: 'flex', flexDirection: 'column', gap: 10,
    }}>
      <div style={{
        width: 34, height: 34, borderRadius: 9, background: `${color}22`,
        display: 'flex', alignItems: 'center', justifyContent: 'center', color,
      }}>
        <Icon size={17} />
      </div>
      <div style={{ fontSize: 12, color: 'var(--text-dim)' }}>{label}</div>
      <div style={{ fontSize: 22, fontWeight: 700 }}>{value}</div>
    </div>
  )
}

export function Card({ title, children }) {
  return (
    <div style={{ background: 'var(--card)', border: '1px solid var(--border)', borderRadius: 14, padding: 20 }}>
      <div style={{ fontSize: 14, fontWeight: 600, marginBottom: 14 }}>{title}</div>
      {children}
    </div>
  )
}
