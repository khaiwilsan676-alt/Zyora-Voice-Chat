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

  // ✅ State to toggle Privacy Policy page view
  const [showPrivacyView, setShowPrivacyView] = useState(false)

  // ✅ State to toggle Account Deletion page view
  const [showAccountDeleteView, setShowAccountDeleteView] = useState(false)

  // ✅ State to handle Confirmation Dialog for Account Deletion
  const [showConfirmDialog, setShowConfirmDialog] = useState(false)

  // ✅ State to handle Confirmation Dialog for Logout
  const [showLogoutDialog, setShowLogoutDialog] = useState(false)

  // ✅ State for dynamic App Version
  const [appVersion, setAppVersion] = useState('1.0.0 (763)')

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

  // ✅ Logic to fetch actual App Version dynamically
  useEffect(() => {
    // 1. Check if Next.js Environment Variable is set
    if (process.env.NEXT_PUBLIC_APP_VERSION) {
      setAppVersion(process.env.NEXT_PUBLIC_APP_VERSION);
    } 
    // 2. Check if Capacitor/Cordova is used (for Mobile Apps)
    else if (typeof window !== 'undefined' && (window as any).Capacitor?.Plugins?.App) {
      (window as any).Capacitor.Plugins.App.getInfo()
        .then((info: any) => {
          if (info && info.version) {
            setAppVersion(`${info.version} (${info.build || '1'})`);
          }
        })
        .catch((err: any) => console.log("Error fetching app version:", err));
    }
    // 3. Fallback to package.json version if available via window object (custom logic)
    else {
      // You can add custom logic here if you are loading version from an API
    }
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

        <div className="flex flex-col items-center justify-center px-6" style={{ minHeight: '60vh' }}>
          <p className="text-[#9CA3AF] text-[17px] font-medium tracking-wide">No data</p>
        </div>
      </div>
    )
  }

  // ✅ ABOUT US PAGE RENDER (Updated with Dynamic Version)
  if (showAboutView) {
    return (
      <div className="w-full min-h-screen bg-white">
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

        <div className="px-6 py-8 pb-20">
          {/* ✅ Top Icon and Version Info */}
          <div className="flex flex-col items-center justify-center mb-10">
            {/* Make sure to place your image in public folder as 'zyora-icon.png' or update the src below */}
            <img 
              src="/zyora-icon.png" 
              alt="Zyora App Icon" 
              className="w-32 h-32 rounded-[2.5rem] shadow-sm object-cover mb-5"
            />
            <h2 className="text-[42px] font-bold text-[#333333] tracking-tight leading-none mb-3">
              Zyora
            </h2>
            <p className="text-[17px] font-medium text-[#7A7A7A] tracking-wide">
              Current version V {appVersion}
            </p>
          </div>

          {/* Existing About Us Text */}
          <div className="text-[#333333] text-[15px] leading-relaxed space-y-6 tracking-wide mt-8">
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

  // ✅ PRIVACY POLICY PAGE RENDER
  if (showPrivacyView) {
    return (
      <div className="w-full min-h-screen bg-white">
        <div
          className="flex items-center px-4 py-3 bg-white relative"
          style={{
            paddingTop: 'calc(env(safe-area-inset-top, 0px) + 16px)'
          }}
        >
          <button 
            onClick={() => setShowPrivacyView(false)} 
            className="p-1 hover:bg-slate-100 rounded-full transition-colors z-10"
          >
            <ArrowLeft size={24} className="text-slate-900" />
          </button>
          <h1 className="text-lg font-semibold text-slate-900 absolute left-0 right-0 text-center pointer-events-none">
            Privacy Policy
          </h1>
        </div>

        <div className="px-6 py-6 pb-20">
          <h2 className="text-2xl font-bold text-slate-900 mb-1">Privacy Policy</h2>
          <p className="text-slate-500 text-[14px] mb-6">Last updated: October 14, 2026</p>

          <div className="text-slate-700 text-[15px] leading-relaxed space-y-5">
            <div>
              <h3 className="text-[16px] font-bold text-slate-900 mb-1">1. About Zyora</h3>
              <p>Zyora is a social voice-chat application where people can create profiles, join voice rooms, meet others, chat, and enjoy social features together.</p>
            </div>

            <div>
              <h3 className="text-[16px] font-bold text-slate-900 mb-1">2. Information Used</h3>
              <p>To provide the app, Zyora may process information you provide, such as your email address, profile name, avatar, account ID, country, messages, and activity within the app. Device information may also be used to help protect accounts and enforce safety restrictions. When you join a live voice room, audio is transmitted to enable voice chat. This policy does not claim that voice conversations are recorded.</p>
            </div>

            <div>
              <h3 className="text-[16px] font-bold text-slate-900 mb-1">3. How Information Is Used</h3>
              <p>Information is used to sign in, maintain your profile, connect you with rooms and other users, enable app features, protect the community, and troubleshoot problems. Zyora does not sell your personal information.</p>
            </div>

            <div>
              <h3 className="text-[16px] font-bold text-slate-900 mb-1">4. Your Data and Choices</h3>
              <p>You can clear data stored locally on your phone at any time by clearing Zyora's app storage in your Android Settings. This may sign you out and remove locally saved app information. Information already associated with your online account may not be removed by clearing phone storage; for that, contact Zyora support through the app.</p>
            </div>

            <div>
              <h3 className="text-[16px] font-bold text-slate-900 mb-1">5. Permissions</h3>
              <p>Zyora needs an internet connection to provide online features and microphone access when you use live voice chat. You can manage these permissions at any time in Android Settings.</p>
            </div>

            <div>
              <h3 className="text-[16px] font-bold text-slate-900 mb-1">6. Safety and Security</h3>
              <p>We take reasonable steps to protect information and maintain a safer community. Please keep your account details private and avoid sharing sensitive personal information in public rooms or with people you do not know. No online service can guarantee complete security.</p>
            </div>

            <div>
              <h3 className="text-[16px] font-bold text-slate-900 mb-1">7. Sharing and Legal Requests</h3>
              <p>We do not sell personal information. Information may be processed as needed to run Zyora, keep the service secure, or comply with applicable law.</p>
            </div>

            <div>
              <h3 className="text-[16px] font-bold text-slate-900 mb-1">8. Updates and Contact</h3>
              <p>This policy may be updated when Zyora's features change. If you have questions or a privacy request, contact the Zyora team through the support option available in the app.</p>
            </div>
          </div>
        </div>
      </div>
    )
  }

  // ✅ ACCOUNT DELETION PAGE RENDER
  if (showAccountDeleteView) {
    return (
      <div className="w-full min-h-screen bg-[#F8F9FA] relative">
        {/* Header */}
        <div
          className="flex items-center px-4 py-3 bg-[#F8F9FA] relative"
          style={{
            paddingTop: 'calc(env(safe-area-inset-top, 0px) + 16px)'
          }}
        >
          <button 
            onClick={() => setShowAccountDeleteView(false)} 
            className="p-1 hover:bg-slate-200 rounded-full transition-colors z-10"
          >
            <ArrowLeft size={24} className="text-slate-900" />
          </button>
          <h1 className="text-lg font-semibold text-slate-900 absolute left-0 right-0 text-center pointer-events-none">
            Account Deletion
          </h1>
        </div>

        {/* Content Area */}
        <div className="px-5 mt-6">
          
          {/* Reminder Section */}
          <h2 className="text-[18px] font-bold text-slate-900 mb-2">Reminder</h2>
          <p className="text-[16px] text-slate-800 leading-relaxed mb-1">
            1. Cancellation erases all personal data, including profiles.
          </p>
          <p className="text-[16px] text-slate-800 leading-relaxed mb-6">
            2. No login post-cancellation.
          </p>

          {/* Conditions Section */}
          <h2 className="text-[18px] font-bold text-slate-900 mb-2">Conditions</h2>
          <p className="text-[16px] text-slate-800 leading-relaxed mb-1">
            1. Account must be clear of bans or freezes.
          </p>
          <p className="text-[16px] text-slate-800 leading-relaxed mb-8">
            2. Wallet must be empty of Gold Coins and diamonds.
          </p>

          {/* Apply for Account Deletion Button */}
          <button
            onClick={() => setShowConfirmDialog(true)}
            className="w-full border-[1.5px] border-[#FF4D79] rounded-full py-3 text-center bg-transparent hover:bg-[#FF4D79]/5 transition-colors"
          >
            <span className="text-[17px] font-medium text-[#FF4D79] tracking-wide">
              Apply for Account Deletion
            </span>
          </button>
        </div>

        {/* ✅ Confirmation Dialog (Popup) */}
        {showConfirmDialog && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4">
            <div className="bg-white rounded-2xl w-full max-w-sm p-6 shadow-xl">
              <h3 className="text-[18px] font-bold text-slate-900 mb-2 text-center">
                Confirm Deletion
              </h3>
              <p className="text-slate-600 text-[14px] text-center mb-6 leading-relaxed">
                Are you sure you want to apply for account deletion? This action cannot be undone.
              </p>
              
              <div className="flex gap-3">
                {/* Cancel Button */}
                <button
                  onClick={() => setShowConfirmDialog(false)}
                  className="flex-1 py-2.5 rounded-full border border-slate-300 text-slate-700 font-medium hover:bg-slate-50 transition-colors"
                >
                  Cancel
                </button>
                
                {/* Confirm Button */}
                <button
                  onClick={() => {
                    // Add your actual account deletion logic here
                    console.log("Account deletion confirmed");
                    setShowConfirmDialog(false);
                  }}
                  className="flex-1 py-2.5 rounded-full bg-[#FF4D79] text-white font-medium hover:bg-[#e6456d] transition-colors"
                >
                  Confirm
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    )
  }

  // ✅ MAIN SETTINGS PAGE RENDER
  return (
    <div className="w-full min-h-screen bg-white relative">
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

        {/* ✅ Blocklist Button */}
        <div
          onClick={() => {
            if (onBlocklistPress) onBlocklistPress()
            setShowBlocklistView(true)
          }}
          className="flex items-center justify-between px-5 py-4 border-b border-slate-100 cursor-pointer hover:bg-slate-50 transition-colors"
        >
          <span className="text-base text-slate-800">{t.blocklist || "Blocklist"}</span>
          <ChevronRight size={20} className="text-slate-400" />
        </div>

        {/* ✅ About Us Button */}
        <div
          onClick={() => {
            if (onAboutPress) onAboutPress()
            setShowAboutView(true)
          }}
          className="flex items-center justify-between px-5 py-4 border-b border-slate-100 cursor-pointer hover:bg-slate-50 transition-colors"
        >
          <span className="text-base text-slate-800">{t.about}</span>
          <ChevronRight size={20} className="text-slate-400" />
        </div>

        {/* ✅ Privacy Policy Button */}
        <div
          onClick={() => setShowPrivacyView(true)}
          className="flex items-center justify-between px-5 py-4 border-b border-slate-100 cursor-pointer hover:bg-slate-50 transition-colors"
        >
          <span className="text-base text-slate-800">{t.privacyPolicy || "Privacy Policy"}</span>
          <ChevronRight size={20} className="text-slate-400" />
        </div>

        {/* ✅ Account Deletion Button */}
        <div
          onClick={() => setShowAccountDeleteView(true)}
          className="flex items-center justify-between px-5 py-4 cursor-pointer hover:bg-slate-50 transition-colors"
        >
          <span className="text-base text-slate-800">{t.accountDeletion || "Account Deletion"}</span>
          <ChevronRight size={20} className="text-slate-400" />
        </div>
      </div>

      <div className="px-6 mt-10">
        <button
          onClick={() => setShowLogoutDialog(true)}
          className="w-full border border-slate-300 rounded-full py-3.5 text-center bg-white hover:bg-slate-50 transition-colors shadow-sm"
        >
          <span className="text-base font-medium text-slate-700">{t.logout}</span>
        </button>
      </div>

      {/* ✅ Logout Confirmation Dialog (Popup) */}
      {showLogoutDialog && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4">
          <div className="bg-white rounded-2xl w-full max-w-sm p-6 shadow-xl">
            <h3 className="text-[18px] font-bold text-slate-900 mb-2 text-center">
              Confirm Logout
            </h3>
            <p className="text-slate-600 text-[14px] text-center mb-6 leading-relaxed">
              Are you sure you want to log out?
            </p>
            
            <div className="flex gap-3">
              {/* Cancel Button */}
              <button
                onClick={() => setShowLogoutDialog(false)}
                className="flex-1 py-2.5 rounded-full border border-slate-300 text-slate-700 font-medium hover:bg-slate-50 transition-colors"
              >
                Cancel
              </button>
              
              {/* Logout Button */}
              <button
                onClick={() => {
                  setShowLogoutDialog(false);
                  handleLogout();
                }}
                className="flex-1 py-2.5 rounded-full bg-[#FF4D79] text-white font-medium hover:bg-[#e6456d] transition-colors"
              >
                Logout
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
