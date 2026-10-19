'use client'

import { apiUrl } from "../src/lib/api";

import React, { useState, useEffect } from 'react'
import { ArrowLeft, ChevronRight } from 'lucide-react'
import { getTranslation, LanguageCode } from '../lib/translations'

interface SettingPageProps {
  onBack?: () => void
  onLogout?: () => void
  onAboutPress?: () => void
  onBlocklistPress?: () => void
}

const OFFICIAL_IDS = ['500001', '500002', '500003', '500004', '500005']
const ADMIN_IDS = ['700001', '700002', '700003']

export default function SettingPage({
  onBack,
  onLogout,
  onAboutPress,
  onBlocklistPress,
}: SettingPageProps) {
  const [isNotificationsEnabled, setIsNotificationsEnabled] = useState(true)
  const [appLang, setAppLang] = useState<LanguageCode>('en')
  
  // ✅ State to toggle About Us page view
  const [showAboutView, setShowAboutView] = useState(false)
  
  // ✅ State to toggle Blocklist page view
  const [showBlocklistView, setShowBlocklistView] = useState(false)

  useEffect(() => {
    const savedNotifications = localStorage.getItem('appNotifications');
    if (savedNotifications !== null) {
      setIsNotificationsEnabled(savedNotifications === 'true');
    }
    const savedLang = localStorage.getItem('appLanguage') as LanguageCode
    if (savedLang) {
      setAppLang(savedLang)
    }

    const handleLangChange = (e: CustomEvent) => {
      if (e.detail && e.detail.lang) {
        setAppLang(e.detail.lang)
      }
    }

    window.addEventListener('languageChange', handleLangChange as EventListener)
    return () => window.removeEventListener('languageChange', handleLangChange as EventListener)
  }, [])

  const t = getTranslation(appLang)

  const toggleSwitch = () => {
    setIsNotificationsEnabled((prev) => {
      const newVal = !prev;
      localStorage.setItem('appNotifications', String(newVal));
      return newVal;
    });
  }

  const handleLogout = async () => {
    const uid = localStorage.getItem("userUID")
    
    const isOfficialOrAdmin = OFFICIAL_IDS.includes(uid || '') || ADMIN_IDS.includes(uid || '')
    
    if (isOfficialOrAdmin && uid) {
      try {
          const response = await fetch(apiUrl("/api/users"), {
            method: "PUT",
            headers: {
              "Content-Type": "application/json",
            },
            body: JSON.stringify({
              uid,
              isLoggedIn: false,
              forceLogoutTimestamp: Date.now(),
            }),
          });

          if (!response.ok) {
            throw new Error(`MongoDB logout update failed: ${response.status}`);
          }
      } catch (error) {
        console.error("Error updating logout status:", error);
      }

      const loggedInSessions = JSON.parse(localStorage.getItem('loggedInSessions') || '{}')
      delete loggedInSessions[uid]
      localStorage.setItem('loggedInSessions', JSON.stringify(loggedInSessions))
      
      localStorage.removeItem(`session_${uid}`)
      localStorage.removeItem(`user_data_${uid}`)
      
      localStorage.setItem(`forceLogout_${uid}`, Date.now().toString())
    }
    
    localStorage.removeItem("userName")
    localStorage.removeItem("userUID")
    localStorage.removeItem("userPhone")
    localStorage.removeItem("userPhoto")
    
    if (onLogout) {
      onLogout()
    }
    
    window.location.reload()
  }

  // ✅ BLOCKLIST PAGE RENDER
  if (showBlocklistView) {
    return (
      <div className="w-full min-h-screen bg-white">
        {/* Top Header with Safe Area for Android/iOS */}
        <div
          className="flex items-center px-4 py-3 bg-white relative"
          style={{
            paddingTop: 'calc(env(safe-area-inset-top, 0px) + 16px)'
          }}
        >
          <button 
            onClick={() => setShowBlocklistView(false)} 
            className="p-1 hover:bg-slate-100 rounded-full transition-colors z-10"
          >
            <ArrowLeft size={24} className="text-slate-900" />
          </button>
          <h1 className="text-lg font-semibold text-slate-900 absolute left-0 right-0 text-center pointer-events-none">
            Blocklist
          </h1>
        </div>

        {/* Blocklist Content (No Data) */}
        <div className="flex flex-col items-center justify-center px-6" style={{ minHeight: '60vh' }}>
          <p className="text-[#9CA3AF] text-[17px] font-medium tracking-wide">No data</p>
        </div>
      </div>
    )
  }

  // ✅ ABOUT US PAGE RENDER
  if (showAboutView) {
    return (
      <div className="w-full min-h-screen bg-white">
        {/* Top Header with Safe Area for Android/iOS */}
        <div
          className="flex items-center px-4 py-3 bg-white relative"
          style={{
            paddingTop: 'calc(env(safe-area-inset-top, 0px) + 16px)'
          }}
        >
          <button 
            onClick={() => setShowAboutView(false)} 
            className="p-1 hover:bg-slate-100 rounded-full transition-colors z-10"
          >
            <ArrowLeft size={24} className="text-slate-900" />
          </button>
          <h1 className="text-lg font-semibold text-slate-900 absolute left-0 right-0 text-center pointer-events-none">
            About Us
          </h1>
        </div>

        {/* About Us Content */}
        <div className="px-6 py-8 pb-20">
          <h2 className="text-3xl font-semibold text-center text-black mb-8 tracking-tight">About Us</h2>

          <div className="text-[#333333] text-[15px] leading-relaxed space-y-6 tracking-wide">
            <p>
              Hurry is a real-time group voice chat application for multiple players. There are South Asian users from all over the world. You can find new friends who share the same interests with you, chat with them and enjoy the fun of the party!
            </p>
            
            <p>We have the following functions:</p>

            <div className="space-y-1">
              <p>-Free</p>
              <p>You can have free, high-quality and stable voice chat through the Internet.</p>
            </div>

            <div className="space-y-1">
              <p>-Online party</p>
              <p>Here we have various online parties, rooms of topics in birthday party, wedding party, war drum game and so on.</p>
            </div>

            <div className="space-y-1">
              <p>-Gorgeous gifts</p>
              <p>While enjoying chatting with your friends, you can send them gifts. Gorgeous effects are coming with gift sent. You can have so much fun~</p>
            </div>
          </div>
        </div>
      </div>
    )
  }

  // ✅ MAIN SETTINGS PAGE RENDER
  return (
    <div className="w-full min-h-screen bg-white">
      <div
        className="flex items-center justify-between px-3 py-3 bg-white"
        style={{
          paddingTop: 'calc(env(safe-area-inset-top, 0px) + 16px)'
        }}
      >
        <button onClick={onBack} className="p-2 hover:bg-slate-100 rounded-full transition-colors">
          <ArrowLeft size={24} className="text-slate-900" />
        </button>
        <h1 className="text-lg font-semibold text-slate-900">{t.settings}</h1>
        <div className="w-6" />
      </div>

      <div className="bg-white mt-4 border-b border-slate-200">
        <div className="flex items-center justify-between px-5 py-4 border-b border-slate-100">
          <span className="text-base text-slate-800">{t.messageNotifications}</span>
          <button
            type="button"
            onClick={toggleSwitch}
            className={`w-12 h-7 flex items-center rounded-full p-1 transition-colors duration-300 ${
              isNotificationsEnabled ? 'bg-cyan-400' : 'bg-slate-300'
            }`}
          >
            <div
              className={`bg-white w-5 h-5 rounded-full shadow-md transform transition-transform duration-300 ${
                isNotificationsEnabled ? 'translate-x-5' : 'translate-x-0'
              }`}
            />
          </button>
        </div>

        {/* ✅ Blocklist Button - Ab yeh view open karega */}
        <div
          onClick={() => {
            if (onBlocklistPress) onBlocklistPress()
            setShowBlocklistView(true) // 👈 Yeh line view open karegi
          }}
          className="flex items-center justify-between px-5 py-4 border-b border-slate-100 cursor-pointer hover:bg-slate-50 transition-colors"
        >
          <span className="text-base text-slate-800">{t.blocklist || "Blocklist"}</span>
          <ChevronRight size={20} className="text-slate-400" />
        </div>

        <div
          onClick={() => {
            if (onAboutPress) onAboutPress()
            setShowAboutView(true)
          }}
          className="flex items-center justify-between px-5 py-4 cursor-pointer hover:bg-slate-50 transition-colors"
        >
          <span className="text-base text-slate-800">{t.about}</span>
          <ChevronRight size={20} className="text-slate-400" />
        </div>
      </div>

      <div className="px-6 mt-10">
        <button
          onClick={handleLogout}
          className="w-full border border-slate-300 rounded-full py-3.5 text-center bg-white hover:bg-slate-50 transition-colors shadow-sm"
        >
          <span className="text-base font-medium text-slate-700">{t.logout}</span>
        </button>
      </div>
    </div>
  )
}
