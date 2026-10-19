'use client'

import { useState, useEffect } from 'react'
import HomePage from '@/components/HomePage'
import LoginPage from '@/components/LoginPage'
import { auth } from '@/src/lib/firebase'
import { signOut, onAuthStateChanged } from 'firebase/auth'

const BAN_CHECK_TIMEOUT_MS = 5000

export default function Page() {
  const [isLoggedIn, setIsLoggedIn] = useState(false)
  const [loading, setLoading] = useState(true)

  const handleLoginSuccess = () => {
    setIsLoggedIn(true)
    setLoading(false)
  }

  async function handleLogout() {
    const uid = localStorage.getItem('userUID')

    try {
      await signOut(auth)
    } catch (error) {
      console.log('Firebase logout error:', error)
    }

    localStorage.removeItem('userEmail')
    localStorage.removeItem('userPhone')
    localStorage.removeItem('userName')
    localStorage.removeItem('userUID')
    localStorage.removeItem('userPhoto')
    localStorage.removeItem('accountNumber')

    if (uid) {
      localStorage.removeItem(`user_data_${uid}`)
      localStorage.removeItem(`session_${uid}`)
      localStorage.removeItem(`forceLogout_${uid}`)

      try {
        const loggedInSessions = JSON.parse(localStorage.getItem('loggedInSessions') || '{}')
        delete loggedInSessions[uid]
        localStorage.setItem('loggedInSessions', JSON.stringify(loggedInSessions))
      } catch {
        localStorage.removeItem('loggedInSessions')
      }
    }

    setIsLoggedIn(false)
    setLoading(false)
  }

  useEffect(() => {
    let active = true

    const checkUserStatus = async (user: any, fallbackUid?: string) => {
      const uid = user?.uid || fallbackUid
      const accountId = localStorage.getItem('accountNumber')

      if (uid || accountId) {
        try {
          // Do not leave the home screen on its splash state if a native plugin
          // or the ban endpoint is unavailable/offline.
          const banCheck = (async () => {
            const { Device } = await import('@capacitor/device')
            const deviceIdInfo = await Device.getId()
            const { apiUrl } = await import('@/src/lib/api')
            return fetch(apiUrl('/api/check-ban'), {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({
                accountId: accountId || 'N/A',
                deviceId: deviceIdInfo.identifier,
              }),
            })
          })()

          const timeout = new Promise<Response>((_, reject) =>
            setTimeout(() => reject(new Error('Ban check timed out')), BAN_CHECK_TIMEOUT_MS)
          )
          const response = await Promise.race([banCheck, timeout])

          if (response.ok) {
            const data = await response.json()
            if (data.banned) {
              localStorage.setItem('recentBanMessage', JSON.stringify(data.banData || {}))
              await handleLogout()
              return
            }
          }
        } catch (error) {
          // A failed ban check should not crash or indefinitely block the app.
          console.warn('Ban check skipped:', error)
        }
      }

      if (active) {
        setIsLoggedIn(true)
        setLoading(false)
      }
    }

    const unsubscribe = onAuthStateChanged(auth, (user) => {
      if (user) {
        void checkUserStatus(user)
        return
      }

      const userUID = localStorage.getItem('userUID')
      const userEmail = localStorage.getItem('userEmail')
      const userPhone = localStorage.getItem('userPhone')

      if (userUID || userEmail || userPhone) {
        void checkUserStatus(null, userUID || undefined)
      } else if (active) {
        setIsLoggedIn(false)
        setLoading(false)
      }
    })

    return () => {
      active = false
      unsubscribe()
    }
  }, [])

  useEffect(() => {
    const intervalId = setInterval(() => {
      const uid = localStorage.getItem('userUID')
      if (uid && localStorage.getItem(`forceLogout_${uid}`)) {
        void handleLogout()
      }
    }, 1000)

    return () => clearInterval(intervalId)
  }, [])

  const themeStyle = {
    background: 'linear-gradient(to bottom, #3b82f6 0%, #3b82f6 30%, #ffffff 50%, #ffffff 100%)',
    minHeight: '100svh',
    minHeight: '100dvh',
    width: '100%',
  }

  if (loading) {
    return (
      <div className="flex flex-col items-center" style={{ ...themeStyle, paddingTop: '18vh' }}>
        <img src="/logo.png" alt="Zyora Logo" className="w-24 h-24 rounded-2xl object-cover shadow-lg" />
        <div style={{ gap: '0.5rem', display: 'flex', flexDirection: 'column', alignItems: 'center', marginTop: '0.5rem' }}>
          <h1 className="text-3xl font-bold text-white tracking-wide">Zyora</h1>
        </div>
      </div>
    )
  }

  if (!isLoggedIn) {
    return (
      <div style={themeStyle}>
        <LoginPage onLoginSuccess={handleLoginSuccess} />
      </div>
    )
  }

  return (
    <div style={themeStyle}>
      <HomePage onLogout={handleLogout} />
    </div>
  )
}
