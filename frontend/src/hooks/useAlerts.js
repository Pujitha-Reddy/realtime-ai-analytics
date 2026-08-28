import { useEffect, useState } from 'react'
import axios from 'axios'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { API_URL } from '../config'

export function useAlerts() {
  const [alerts, setAlerts] = useState([])

  useEffect(() => {
    axios.get(`${API_URL}/api/alerts`).then(res => setAlerts(res.data)).catch(() => {})

    const client = new Client({
      webSocketFactory: () => new SockJS(`${API_URL}/ws`),
      reconnectDelay: 3000,
      onConnect: () => client.subscribe('/topic/alerts', (msg) => setAlerts(JSON.parse(msg.body))),
    })
    client.activate()
    return () => client.deactivate()
  }, [])

  return alerts
}
