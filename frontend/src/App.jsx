import { BrowserRouter, Routes, Route } from 'react-router-dom'
import Sidebar from './components/Sidebar'
import ChatDock from './components/ChatDock'
import Overview from './pages/Overview'
import Analytics from './pages/Analytics'
import LiveEvents from './pages/LiveEvents'
import Settings from './pages/Settings'

export default function App() {
  return (
    <BrowserRouter>
      <Sidebar />
      <main style={{
        marginLeft: 'var(--sidebar-width)',
        marginRight: 'var(--chat-width)',
        padding: '28px 28px 40px',
        minHeight: '100vh',
      }}>
        <h1 style={{ fontSize: 22, fontWeight: 700, marginBottom: 24 }}>Real-Time Analytics</h1>
        <Routes>
          <Route path="/" element={<Overview />} />
          <Route path="/analytics" element={<Analytics />} />
          <Route path="/live" element={<LiveEvents />} />
          <Route path="/settings" element={<Settings />} />
        </Routes>
      </main>
      <ChatDock />
    </BrowserRouter>
  )
}
