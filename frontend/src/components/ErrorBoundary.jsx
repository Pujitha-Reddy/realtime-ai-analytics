import { Component } from 'react'

export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props)
    this.state = { hasError: false }
  }

  static getDerivedStateFromError() {
    return { hasError: true }
  }

  componentDidCatch(error, info) {
    console.error('Caught by ErrorBoundary:', error, info)
  }

  render() {
    if (this.state.hasError) {
      return (
        <div style={{
          minHeight: '100vh', display: 'flex', flexDirection: 'column',
          alignItems: 'center', justifyContent: 'center', gap: 12,
          background: 'var(--bg)', color: 'var(--text)', fontFamily: 'Inter, sans-serif',
        }}>
          <h2>Something went wrong.</h2>
          <p style={{ color: 'var(--text-dim)' }}>Try refreshing the page.</p>
          <button onClick={() => window.location.reload()} style={{
            background: 'var(--accent)', color: 'white', border: 'none',
            padding: '10px 20px', borderRadius: 8, cursor: 'pointer',
          }}>Refresh</button>
        </div>
      )
    }
    return this.props.children
  }
}
