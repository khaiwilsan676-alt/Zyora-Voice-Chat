'use client'

import { useEffect } from 'react'
import { StatusBar, Style } from '@capacitor/status-bar'

export default function StatusBarController() {
  useEffect(() => {
    const setup = async () => {
      try {
        // Keep the entire app content below the Android status bar.
        // This applies to every page so no page renders underneath the status-bar area.
        await StatusBar.setOverlaysWebView({ overlay: false })

        // Keep the status-bar background transparent while content begins below it
        await StatusBar.setBackgroundColor({
          color: '#00000000',
        })

        // Dark Android status-bar icons
        await StatusBar.setStyle({
          style: Style.Dark,
        })
      } catch (error) {
        console.warn('StatusBar setup failed:', error)
      }
    }

    setup()
  }, [])

  return null
}
