import { useEffect, useState } from 'react'
import axios from 'axios'
import { LineChart, Line, XAxis, YAxis, Tooltip, CartesianGrid, ResponsiveContainer } from 'recharts'
import { API_URL } from '../config'
import { Card } from '../pages/Overview'

export default function TrendChart({ hours = 24 }) {
  const [trend, setTrend] = useState([])

  useEffect(() => {
    const load = () => axios.get(`${API_URL}/api/trends?hours=${hours}`).then(res => setTrend(res.data)).catch(() => {})
    load()
    const interval = setInterval(load, 30000) // refresh every 30s, no need for websocket precision here
    return () => clearInterval(interval)
  }, [hours])

  const data = trend.map(t => ({
    ...t,
    label: new Date(t.hour).toLocaleTimeString([], { hour: '2-digit' }),
  }))

  return (
    <Card title={`Revenue Trend (last ${hours}h)`}>
      <ResponsiveContainer width="100%" height={240}>
        <LineChart data={data}>
          <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.06)" vertical={false} />
          <XAxis dataKey="label" stroke="var(--text-dim)" fontSize={11} tickLine={false} axisLine={false} />
          <YAxis stroke="var(--text-dim)" fontSize={11} tickLine={false} axisLine={false} />
          <Tooltip contentStyle={{ background: 'var(--card)', border: '1px solid var(--border)', borderRadius: 8, color: 'var(--text)' }} />
          <Line type="monotone" dataKey="revenue" stroke="var(--accent-2)" strokeWidth={2} dot={false} isAnimationActive={false} />
        </LineChart>
      </ResponsiveContainer>
    </Card>
  )
}
