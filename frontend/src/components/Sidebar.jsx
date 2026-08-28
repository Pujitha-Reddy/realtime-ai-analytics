import { NavLink } from 'react-router-dom'
import { LayoutDashboard, Activity, Radio, Settings } from 'lucide-react'

const NAV_ITEMS = [
  { to: '/', icon: LayoutDashboard, label: 'Overview' },
  { to: '/analytics', icon: Activity, label: 'Analytics' },
  { to: '/live', icon: Radio, label: 'Live Events' },
  { to: '/settings', icon: Settings, label: 'Settings' },
]

export default function Sidebar() {
  return (
    <aside style={{
      width: 'var(--sidebar-width)',
      background: 'var(--bg-elevated)',
      borderRight: '1px solid var(--border)',
      height: '100vh',
      position: 'fixed',
      left: 0,
      top: 0,
      display: 'flex',
      flexDirection: 'column',
      padding: '20px 14px',
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '4px 8px 28px' }}>
        <div style={{
          width: 32, height: 32, borderRadius: 8,
          background: 'linear-gradient(135deg, var(--accent), var(--accent-3))',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontWeight: 800, fontSize: 15,
        }}>A</div>
        <span style={{ fontWeight: 700, fontSize: 15 }}>Analytics AI</span>
      </div>

      <nav style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
        {NAV_ITEMS.map(({ to, icon: Icon, label }) => (
          <NavLink key={to} to={to} end={to === '/'} style={({ isActive }) => ({
            display: 'flex', alignItems: 'center', gap: 10,
            padding: '9px 12px', borderRadius: 8,
            background: isActive ? 'var(--card)' : 'transparent',
            color: isActive ? 'var(--text)' : 'var(--text-dim)',
            fontSize: 13.5, fontWeight: 500,
            border: isActive ? '1px solid var(--border)' : '1px solid transparent',
          })}>
            <Icon size={16} />
            {label}
          </NavLink>
        ))}
      </nav>

      <div style={{ marginTop: 'auto', padding: '10px 12px', fontSize: 11, color: 'var(--text-dim)' }}>
        Kafka · Redis · Gemini RAG
      </div>
    </aside>
  )
}
