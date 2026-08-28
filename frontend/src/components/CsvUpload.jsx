import { useState } from 'react'
import axios from 'axios'
import { Upload } from 'lucide-react'
import { API_URL } from '../config'
import { Card } from '../pages/Overview'

export default function CsvUpload() {
  const [status, setStatus] = useState(null)
  const [uploading, setUploading] = useState(false)

  const handleFile = async (e) => {
    const file = e.target.files[0]
    if (!file) return
    setUploading(true)
    setStatus(null)
    const formData = new FormData()
    formData.append('file', file)
    try {
      const res = await axios.post(`${API_URL}/api/ingest/csv`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      setStatus(`Ingested ${res.data.accepted} rows (${res.data.rejected} rejected)`)
    } catch {
      setStatus('Upload failed — check the CSV format.')
    } finally {
      setUploading(false)
      e.target.value = ''
    }
  }

  return (
    <Card title="Import your own data">
      <div style={{ fontSize: 12.5, color: 'var(--text-dim)', marginBottom: 12 }}>
        CSV columns: <code style={{ color: 'var(--text)' }}>category,region,amount,status</code> (timestamp optional).
        Rows are fed through the same pipeline as live events.
      </div>
      <label style={{
        display: 'inline-flex', alignItems: 'center', gap: 8, cursor: 'pointer',
        background: 'var(--accent)', color: 'white', padding: '9px 16px',
        borderRadius: 8, fontSize: 13, fontWeight: 500,
      }}>
        <Upload size={14} />
        {uploading ? 'Uploading…' : 'Choose CSV file'}
        <input type="file" accept=".csv" onChange={handleFile} style={{ display: 'none' }} disabled={uploading} />
      </label>
      {status && <div style={{ marginTop: 10, fontSize: 12.5, color: 'var(--text-dim)' }}>{status}</div>}
    </Card>
  )
}
