import { useEffect, useState } from 'react'
import axios from 'axios'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { API_URL } from '../config'

export function useLiveMetrics() {
  const [metrics, setMetrics] = useState(null)

  useEffect(() => {
    axios.get(`${API_URL}/api/metrics`).then(res => setMetrics(res.data)).catch(() => {})

    const client = new Client({
      webSocketFactory: () => new SockJS(`${API_URL}/ws`),
      reconnectDelay: 3000,
      onConnect: () => client.subscribe('/topic/metrics', (msg) => setMetrics(JSON.parse(msg.body))),
    })
    client.activate()
    return () => client.deactivate()
  }, [])

  return metrics
}
