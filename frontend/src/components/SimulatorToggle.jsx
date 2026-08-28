import { useEffect, useState } from 'react'
import axios from 'axios'
import { Play, Pause } from 'lucide-react'
import { API_URL } from '../config'

export default function SimulatorToggle() {
  const [paused, setPaused] = useState(false)

  useEffect(() => {
    axios.get(`${API_URL}/api/simulator/status`).then(res => setPaused(res.data.paused)).catch(() => {})
  }, [])

  const toggle = async () => {
    const endpoint = paused ? '/api/simulator/resume' : '/api/simulator/pause'
    const res = await axios.post(`${API_URL}${endpoint}`)
    setPaused(res.data.paused)
  }

  return (
    <button onClick={toggle} style={{
      display: 'flex', alignItems: 'center', gap: 8,
      background: 'var(--card)', border: '1px solid var(--border)', borderRadius: 8,
      padding: '8px 14px', color: 'var(--text)', fontSize: 13,
    }}>
      {paused ? <Play size={14} /> : <Pause size={14} />}
      {paused ? 'Resume simulator' : 'Pause simulator'}
    </button>
  )
}
