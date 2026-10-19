'use client'

import { useEffect, useState } from 'react'

export default function DebugPage() {
  const [loaded, setLoaded] = useState(false)
  const [email, setEmail] = useState('')

  useEffect(() => {
    const keys = ['userEmail', 'email', 'emailPhone', 'user_email', 'emailOrPhone']
    let found = ''
    for (const k of keys) {
      const v = localStorage.getItem(k)
      if (v && v.trim()) { found = v.trim().toLowerCase(); break }
    }
    setEmail(found)

    const s = document.createElement('script')
    s.src = 'https://cdn.jsdelivr.net/npm/eruda'
    s.async = true
    s.onload = () => {
      if ((window as any).eruda) {
        (window as any).eruda.init()
        setLoaded(true)
      }
    }
    document.head.appendChild(s)
  }, [])

  return (
    <div style={{
      minHeight: '100vh',
      background: '#020617',
      color: '#fff',
      padding: 20,
      fontFamily: 'monospace',
    }}>
      <h1 style={{ fontSize: 24, marginBottom: 12 }}>🐛 Debug Console</h1>
      <p style={{ fontSize: 14, marginBottom: 8 }}>
        <strong>Status:</strong> {loaded ? '✅ Eruda loaded' : '⏳ Loading...'}
      </p>
      <p style={{ fontSize: 14, marginBottom: 8 }}>
        <strong>Email:</strong> {email || '(none)'}
      </p>
      <p style={{ fontSize: 12, color: '#888', marginTop: 20 }}>
        Bottom-right me Eruda ka button aayega. Uspe tap karke console kholo.
      </p>
    </div>
  )
}
