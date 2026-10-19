'use client'

import React, { useEffect, useRef, useState } from 'react'
import { ArrowLeft, HelpCircle } from 'lucide-react'

// ==========================================
// UPGRADE SHEET COMPONENT (WHITE BG + BLUE BUTTON)
// ==========================================
function UpgradeSheet({ isOpen, onClose }: { isOpen: boolean; onClose: () => void }) {
  if (!isOpen) return null;

  return (
    <div 
      className="fixed inset-0 z-[200] flex items-center justify-center bg-transparent transition-opacity"
      onClick={onClose}
    >
      <div 
        className="w-full max-w-[340px] bg-white rounded-[24px] border border-gray-200 shadow-2xl overflow-hidden animate-in zoom-in-95 duration-300"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-center pt-5 pb-2 relative">
          <div className="flex items-center gap-2">
            <span className="text-[#f59e0b] text-lg">◆</span>
            <h2 className="text-gray-900 font-bold text-[18px] tracking-wide">How to upgrade</h2>
            <span className="text-[#f59e0b] text-lg">◆</span>
          </div>
        </div>

        {/* List Items */}
        <div className="px-5 py-2 flex flex-col gap-1">
          
          {/* Send Gifts */}
          <div className="flex items-center gap-4 py-3 border-b border-gray-100">
            <div className="w-12 h-12 flex items-center justify-center shrink-0">
              <svg className="w-10 h-10 text-[#3b82f6]" viewBox="0 0 24 24" fill="currentColor">
                <path d="M20 12v10H4V12h16zm-2-6a2 2 0 0 0-2-2h-1.5a3.5 3.5 0 0 0-2.5 1.05A3.5 3.5 0 0 0 9.5 4H8a2 2 0 0 0-2 2v1h2v2H4v3h16v-3h-4V7h2V6zM8 6h1.5a1.5 1.5 0 0 1 0 3H8V6zm6.5 3a1.5 1.5 0 0 1 0-3H16v3h-1.5z"/>
              </svg>
            </div>
            <div className="flex flex-col">
              <span className="text-gray-900 font-semibold text-[15px]">Send Gifts</span>
              <span className="text-gray-500 text-[13px]">1 coins=1 EXP</span>
            </div>
          </div>

          {/* Join rooms */}
          <div className="flex items-center gap-4 py-3 border-b border-gray-100">
            <div className="w-12 h-12 flex items-center justify-center shrink-0">
              <svg className="w-10 h-10 text-[#3b82f6]" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 3L2 12h3v8h6v-6h2v6h6v-8h3L12 3z"/>
              </svg>
            </div>
            <div className="flex flex-col">
              <span className="text-gray-900 font-semibold text-[15px]">Join rooms</span>
              <span className="text-gray-500 text-[13px]">2000 EXP/Day</span>
            </div>
          </div>

          {/* Share Rooms */}
          <div className="flex items-center gap-4 py-3 border-b border-gray-100">
            <div className="w-12 h-12 flex items-center justify-center shrink-0">
              <svg className="w-10 h-10 text-[#3b82f6]" viewBox="0 0 24 24" fill="currentColor">
                <path d="M10 9V5l-7 7 7 7v-4.1c5 0 8.5 1.6 11 5.1-1-5-4-10-11-11z"/>
              </svg>
            </div>
            <div className="flex flex-col">
              <span className="text-gray-900 font-semibold text-[15px]">Share Rooms</span>
              <span className="text-gray-500 text-[13px]">2000 EXP/Day</span>
            </div>
          </div>

          {/* Take the mic and unmute in your ov */}
          <div className="flex items-center gap-4 py-3 border-b border-gray-100">
            <div className="w-12 h-12 flex items-center justify-center shrink-0">
              <svg className="w-10 h-10 text-[#3b82f6]" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/>
                <path d="M19 10v2a7 7 0 0 1-14 0v-2"/>
                <line x1="12" y1="19" x2="12" y2="23"/>
                <line x1="8" y1="23" x2="16" y2="23"/>
              </svg>
            </div>
            <div className="flex flex-col">
              <span className="text-gray-900 font-semibold text-[15px] leading-tight">Take the mic and unmute in your ov</span>
              <span className="text-gray-500 text-[13px]">1 min = 500 exp, 10000 exp/day</span>
            </div>
          </div>

          {/* Take the mic and unmute in others' */}
          <div className="flex items-center gap-4 py-3">
            <div className="w-12 h-12 flex items-center justify-center shrink-0">
              <svg className="w-10 h-10 text-[#3b82f6]" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/>
                <path d="M19 10v2a7 7 0 0 1-14 0v-2"/>
                <line x1="12" y1="19" x2="12" y2="23"/>
                <line x1="8" y1="23" x2="16" y2="23"/>
              </svg>
            </div>
            <div className="flex flex-col">
              <span className="text-gray-900 font-semibold text-[15px] leading-tight">Take the mic and unmute in others'</span>
              <span className="text-gray-500 text-[13px]">1 min = 500 exp, 20000 exp/day</span>
            </div>
          </div>
        </div>

        {/* Confirm Button (BLUE) */}
        <div className="p-4 pt-2">
          <button
            onClick={onClose}
            className="w-full py-3.5 rounded-full bg-gradient-to-r from-[#3b82f6] to-[#2563eb] text-white font-bold text-[17px] tracking-wide shadow-[0_4px_15px_rgba(59,130,246,0.5)] active:scale-[0.98] transition-transform"
          >
            Confirm
          </button>
        </div>
      </div>
    </div>
  );
}
// ==========================================
// END UPGRADE SHEET COMPONENT
// ==========================================

interface LevelProps {
  onBack?: () => void
}

interface TierData {
  id: string
  range: string
  rightGraphic: string
  medalBadgeSrc: string
  isWhiteBg: boolean
  rewards: { level: number; coins: string }[]
}

const tiersList: TierData[] = [
  {
    id: 'tier-1',
    range: 'Lv.1 - Lv.10',
    rightGraphic: '/IMG_20260911_230430.png',
    medalBadgeSrc: '/IMG_20260917_220530.png',
    isWhiteBg: true,
    rewards: [
      { level: 2, coins: '16,000' },
      { level: 4, coins: '25,000' },
      { level: 6, coins: '35,000' },
      { level: 8, coins: '46,000' },
      { level: 10, coins: '58,000' },
    ],
  },
  {
    id: 'tier-2',
    range: 'Lv.11 - Lv.20',
    rightGraphic: '/IMG_20260911_230448.png',
    medalBadgeSrc: '/IMG_20260917_220613.png',
    isWhiteBg: false,
    rewards: [
      { level: 12, coins: '70,000' },
      { level: 14, coins: '84,000' },
      { level: 16, coins: '98,000' },
      { level: 18, coins: '114,000' },
      { level: 20, coins: '130,000' },
    ],
  },
  {
    id: 'tier-3',
    range: 'Lv.21 - Lv.30',
    rightGraphic: '/IMG_20260911_230538.png',
    medalBadgeSrc: '/IMG_20260917_220641.png',
    isWhiteBg: false,
    rewards: [
      { level: 22, coins: '146,000' },
      { level: 24, coins: '164,000' },
      { level: 26, coins: '280,000' },
      { level: 28, coins: '430,000' },
      { level: 30, coins: '676,000' },
    ],
  },
  {
    id: 'tier-4',
    range: 'Lv.31 - Lv.40',
    rightGraphic: '/IMG_20260911_230602.png',
    medalBadgeSrc: '/IMG_20260917_220710.png',
    isWhiteBg: false,
    rewards: [
      { level: 32, coins: '1,000,000' },
      { level: 34, coins: '1,600,000' },
      { level: 36, coins: '11,200,000' },
      { level: 38, coins: '11,400,000' },
      { level: 40, coins: '16,000,000' },
    ],
  },
  {
    id: 'tier-5',
    range: 'Lv.41 - Lv.50',
    rightGraphic: '/IMG_20260911_230631.png',
    medalBadgeSrc: '/IMG_20260917_220733.png',
    isWhiteBg: false,
    rewards: [
      { level: 42, coins: '22,000,000' },
      { level: 44, coins: '29,000,000' },
      { level: 46, coins: '37,000,000' },
      { level: 48, coins: '47,000,000' },
      { level: 50, coins: '58,000,000' },
    ],
  },
  {
    id: 'tier-6',
    range: 'Lv.51 - Lv.60',
    rightGraphic: '/IMG_20260911_230722.png',
    medalBadgeSrc: '/IMG_20260917_220753.png',
    isWhiteBg: false,
    rewards: [
      { level: 52, coins: '70,000,000' },
      { level: 54, coins: '84,000,000' },
      { level: 56, coins: '100,000,000' },
      { level: 58, coins: '120,000,000' },
      { level: 60, coins: '140,000,000' },
    ],
  },
  {
    id: 'tier-7',
    range: 'Lv.61 - Lv.70',
    rightGraphic: '/IMG_20260911_230739.png',
    medalBadgeSrc: '/IMG_20260917_220815.png',
    isWhiteBg: false,
    rewards: [
      { level: 62, coins: '160,000,000' },
      { level: 64, coins: '180,000,000' },
      { level: 66, coins: '200,000,000' },
      { level: 68, coins: '230,000,000' },
      { level: 70, coins: '260,000,000' },
    ],
  },
  {
    id: 'tier-8',
    range: 'Lv.71 - Lv.80',
    rightGraphic: '/IMG_20260911_230808.png',
    medalBadgeSrc: '/IMG_20260917_220839.png',
    isWhiteBg: false,
    rewards: [
      { level: 72, coins: '290,000,000' },
      { level: 74, coins: '320,000,000' },
      { level: 76, coins: '360,000,000' },
      { level: 78, coins: '400,000,000' },
      { level: 80, coins: '440,000,000' },
    ],
  },
  {
    id: 'tier-9',
    range: 'Lv.81 - Lv.90',
    rightGraphic: '/IMG_20260911_230826.png',
    medalBadgeSrc: '/IMG_20260917_220900.png',
    isWhiteBg: false,
    rewards: [
      { level: 82, coins: '480,000,000' },
      { level: 84, coins: '520,000,000' },
      { level: 86, coins: '570,000,000' },
      { level: 88, coins: '620,000,000' },
      { level: 90, coins: '680,000,000' },
    ],
  },
  {
    id: 'tier-10',
    range: 'Lv.91 - Lv.100',
    rightGraphic: '/file_00000000b06081fabde2d7eac02ce8c2.png',
    medalBadgeSrc: '/IMG_20260917_220922.png',
    isWhiteBg: false,
    rewards: [
      { level: 92, coins: '740,000,000' },
      { level: 94, coins: '800,000,000' },
      { level: 96, coins: '860,000,000' },
      { level: 98, coins: '930,000,000' },
      { level: 100, coins: '1,000,000,000' },
    ],
  },
]

function ShaderImageBadge({
  src,
  isWhiteBg,
  className = 'w-16 h-8 object-contain',
}: {
  src: string
  isWhiteBg: boolean
  className?: string
}) {
  const canvasRef = useRef<HTMLCanvasElement | null>(null)

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return
    const ctx = canvas.getContext('2d', { willReadFrequently: true })
    if (!ctx) return

    const img = new Image()
    img.src = src

    img.onload = () => {
      const w = img.naturalWidth || 120
      const h = img.naturalHeight || 60
      canvas.width = w
      canvas.height = h

      ctx.clearRect(0, 0, w, h)
      ctx.drawImage(img, 0, 0, w, h)

      try {
        const imgData = ctx.getImageData(0, 0, w, h)
        const d = imgData.data

        for (let i = 0; i < d.length; i += 4) {
          const r = d[i]
          const g = d[i + 1]
          const b = d[i + 2]

          if (isWhiteBg) {
            const minVal = Math.min(r, g, b)
            const maxVal = Math.max(r, g, b)
            const isNeutral = maxVal - minVal < 30

            if (r > 200 && g > 200 && b > 200 && isNeutral) {
              if (r > 235 && g > 235 && b > 235) {
                d[i + 3] = 0
              } else {
                const factor = (255 - Math.max(r, g, b)) / 55
                d[i + 3] = Math.floor(d[i + 3] * Math.max(0, Math.min(1, factor)))
              }
            }
          } else {
            const maxRB = Math.max(r, b)
            const greenDiff = g - maxRB

            if (g > 70 && greenDiff > 25) {
              d[i + 3] = 0
            } else if (g > 60 && greenDiff > 10) {
              const alphaRatio = 1 - (greenDiff - 10) / 15
              d[i + 3] = Math.floor(d[i + 3] * Math.max(0, Math.min(1, alphaRatio)))
              d[i + 1] = Math.min(g, maxRB + 5)
            }
          }
        }

        ctx.putImageData(imgData, 0, 0)
      } catch (e) {
        ctx.drawImage(img, 0, 0, w, h)
      }
    }
  }, [src, isWhiteBg])

  return (
    <canvas
      ref={canvasRef}
      className={`${className} drop-shadow-[0_6px_14px_rgba(0,0,0,0.6)] filter transition-transform duration-200`}
    />
  )
}

const isValidName = (val?: string | null): boolean => {
  if (!val) return false;
  const clean = val.trim().toLowerCase();
  return clean !== '' && clean !== 'guest' && clean !== 'user' && clean !== 'null' && clean !== 'undefined';
}

export default function Level({ onBack }: LevelProps) {
  const [activeTierIdx, setActiveTierIdx] = useState(0)
  const scrollContainerRef = useRef<HTMLDivElement | null>(null)
  const tierSectionRefs = useRef<(HTMLDivElement | null)[]>([])

  const [userName, setUserName] = useState<string>('')
  const [userPhoto, setUserPhoto] = useState<string>('')
  const [userUid, setUserUid] = useState<string>('')

  const [showUpgradeSheet, setShowUpgradeSheet] = useState(false)

  const loadUserFromLocal = () => {
    if (typeof window === 'undefined') return
    const uid = localStorage.getItem('userUID') || localStorage.getItem('userPhone') || localStorage.getItem('userId') || ''
    const localName = localStorage.getItem('userName') || ''
    const photo = localStorage.getItem('userPhoto') || ''

    setUserUid(uid)
    setUserName(isValidName(localName) ? localName : '')
    setUserPhoto(photo || '')
  }

  useEffect(() => {
    loadUserFromLocal()

    const interval = setInterval(loadUserFromLocal, 500)

    const handleStorage = (e: StorageEvent) => {
      if (e.key === 'userName' || e.key === 'userPhoto' || e.key === 'userUID') {
        loadUserFromLocal()
      }
    }
    window.addEventListener('storage', handleStorage)

    return () => {
      clearInterval(interval)
      window.removeEventListener('storage', handleStorage)
    }
  }, [])

  const displayName = userName || (userUid ? userUid.substring(0, 8) : 'Guest')
  const avatarLetter = displayName ? displayName.charAt(0).toUpperCase() : '?'

  const currentTier = tiersList[activeTierIdx] || tiersList[0]

  const handleScroll = () => {
    const container = scrollContainerRef.current
    if (!container) return

    const containerTop = container.getBoundingClientRect().top
    const triggerPoint = containerTop + 140

    let active = 0
    tierSectionRefs.current.forEach((ref, index) => {
      if (ref) {
        const rect = ref.getBoundingClientRect()
        if (rect.top <= triggerPoint) {
          active = index
        }
      }
    })
    setActiveTierIdx(active)
  }

  return (
    <div className="relative w-full max-w-[440px] mx-auto h-[100dvh] bg-[#04060a] text-white flex flex-col font-sans select-none overflow-hidden">
      {/* 1. TOP BACKGROUND IMAGE */}
      <div className="absolute top-0 left-0 w-full h-[280px] pointer-events-none z-0 overflow-hidden">
        <div
          className="absolute inset-0 bg-top bg-cover bg-no-repeat"
          style={{ backgroundImage: "url('/file_00000000e02481f4bb2153e2714aca47.png')" }}
        />
        <div className="absolute -top-10 -left-10 w-[260px] h-[260px] bg-[#1d4ed8]/30 blur-[90px] rounded-full" />
        <div className="absolute inset-0 bg-gradient-to-b from-transparent via-[#04060a]/40 to-[#04060a]" />
      </div>

      {/* 2. FIXED / PINNED TOP CONTAINER */}
      <div className="relative z-30 flex flex-col shrink-0 px-4">
        {/* Top App Bar */}
        <div
          className="flex items-center justify-center w-full -mx-4 pb-2 pt-2 bg-transparent relative z-50"
          style={{ paddingTop: 'calc(env(safe-area-inset-top, 0px) + 24px)' }}
        >
          {/* Back Button */}
          <button
            onClick={onBack}
            className="absolute left-0 p-2 hover:bg-white/10 active:scale-95 rounded-full transition-all cursor-pointer z-50"
            style={{ top: 'calc(max(env(safe-area-inset-top, 0px), var(--status-bar-height, 0px)) + 13px)' }}
          >
            <ArrowLeft size={26} strokeWidth={2.5} className="text-white drop-shadow-md" />
          </button>
          
          <h1 className="flex-1 text-center translate-x-4 text-xl font-extrabold text-white tracking-wide drop-shadow-lg">
            Level
          </h1>

          {/* Help Button */}
          <button
            onClick={() => setShowUpgradeSheet(true)}
            className="absolute -right-6 p-2 hover:bg-white/10 active:scale-95 rounded-full transition-all cursor-pointer z-[100]"
            style={{ top: 'calc(max(env(safe-area-inset-top, 0px), var(--status-bar-height, 0px)) + 13px)' }}
          >
            <HelpCircle size={24} strokeWidth={2.5} className="text-white drop-shadow-md" />
          </button>
        </div>

        {/* Top Image Card Frame */}
        <div className="relative -mt-10 -mx-5">
          <img
            src="/file_000000007044820ea729df406d1dc320.png"
            alt="Top Card Frame"
            className="w-full h-auto block"
          />

          <div className="absolute inset-0 z-10 flex items-center px-8 gap-3.5">
            {/* Avatar */}
            <div className="relative shrink-0">
              {userPhoto ? (
                <img
                  src={userPhoto}
                  alt="User"
                  className="w-12 h-12 rounded-full object-cover"
                  onError={(e) => {
                    e.currentTarget.style.display = 'none';
                    e.currentTarget.nextElementSibling?.classList.remove('hidden');
                  }}
                />
              ) : null}
              <div
                className={`w-12 h-12 rounded-full bg-gray-600 flex items-center justify-center text-lg font-bold text-white ${
                  userPhoto ? 'hidden' : ''
                }`}
              >
                {avatarLetter}
              </div>
            </div>

            {/* Profile Info Details */}
            <div className="flex-1 flex flex-col justify-center min-w-0 pr-2">
              <div className="flex items-center gap-2 mt-1">
                <span className="text-white font-serif font-black text-[17px] tracking-wide truncate">
                  {displayName}
                </span>

                <img
                  src={tiersList[0].medalBadgeSrc}
                  alt="User Level"
                  className="h-7 w-auto object-contain drop-shadow-[0_2px_8px_rgba(0,0,0,0.8)] mt-1"
                />
              </div>

              <div className="relative w-full h-[6px] bg-white/25 rounded-full mt-2 overflow-visible">
                <div
                  className="h-full bg-gradient-to-r from-[#ffe072] to-[#f4b63f] rounded-full relative"
                  style={{ width: '38%' }}
                >
                  <span className="absolute -right-1.5 -top-[3px] w-3 h-3 bg-white rounded-full border-2 border-[#f4b63f] shadow-md" />
                </div>
              </div>

              <div className="flex items-center justify-between mt-1.5">
                <span className="text-[10.5px] text-white/90 font-medium tracking-wide drop-shadow-sm">
                  4.5k/20.2k remaining to reach Level 5 &gt;
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* Dynamic Sticky Header */}
        <div className="relative flex items-center justify-between w-full -mt-10 pb-1 min-h-[85px]">
          <div className="absolute -top-3.5 -left-4 pointer-events-none z-20">
            <img
              src="/file_000000006688821197edc482e295d3fd.png"
              alt="Corner Tag"
              className="h-7 object-contain drop-shadow-md"
            />
          </div>

          <div className="flex items-center gap-2 -mt-4 z-10">
            <div className="flex flex-col items-center justify-center">
              <svg
                className="w-4 h-4 text-white drop-shadow-[0_2px_6px_rgba(0,0,0,0.9)]"
                viewBox="0 0 24 24"
                fill="currentColor"
              >
                <path d="M12 3L4 11h5v9h6v-9h5L12 3z" />
              </svg>
              <div className="flex flex-col gap-0.5 mt-0.5">
                <div className="w-3.5 h-[1.5px] bg-white rounded-full" />
                <div className="w-2 h-[1.5px] bg-white rounded-full mx-auto" />
              </div>
            </div>

            <h2 className="text-white font-bold text-[14px] whitespace-nowrap tracking-wide drop-shadow-[0_2px_6px_rgba(0,0,0,0.9)]">
              Update level to {currentTier.range}
            </h2>
          </div>

          <div className="shrink-0 -mr-2 -mt-6 z-10 transition-all duration-300">
            <img
              key={currentTier.rightGraphic}
              src={currentTier.rightGraphic}
              alt="Tier Graphic"
              className="w-[100px] h-[100px] object-contain drop-shadow-[0_8px_24px_rgba(0,0,0,0.8)] animate-in fade-in zoom-in-95 duration-200"
            />
          </div>
        </div>
      </div>

      {/* 3. SCROLLABLE SET SECTION */}
      <div
        ref={scrollContainerRef}
        onScroll={handleScroll}
        className="flex-1 px-4 overflow-y-auto z-20 pb-24 pt-1"
      >
        <div className="flex flex-col gap-9">
          {tiersList.map((tier, tIdx) => (
            <div
              key={tier.id}
              ref={(el) => {
                tierSectionRefs.current[tIdx] = el
              }}
              className="w-full flex flex-col pt-1"
            >
              {/* Section Header */}
              <div className="flex items-center justify-between w-full min-h-[60px] mb-2 px-1">
                <div className="flex items-center gap-2">
                  <svg
                    className="w-4 h-4 text-white drop-shadow-md shrink-0"
                    viewBox="0 0 24 24"
                    fill="currentColor"
                  >
                    <path d="M12 4l-8 8h5v8h6v-8h5z" />
                  </svg>
                  <h3 className="text-white font-bold text-[14px] tracking-wide drop-shadow-[0_2px_8px_rgba(0,0,0,0.8)]">
                    Upgrade to level {tier.range}
                  </h3>
                </div>

                <div className="shrink-0 -mr-1">
                  <img
                    src={tier.rightGraphic}
                    alt={tier.range}
                    className="w-[60px] h-[60px] object-contain drop-shadow-[0_4px_10px_rgba(0,0,0,0.8)]"
                  />
                </div>
              </div>

              {/* 3 Square Cards Row */}
              <div className="grid grid-cols-3 gap-2 w-full mb-4 px-0.5">
                <div className="relative bg-gradient-to-br from-[#06080d] to-[#132c54]/60 rounded-lg p-2.5 flex flex-col items-center justify-center shadow-[0_4px_10px_rgba(0,0,0,0.5)]">
                  <span className="absolute top-1 left-1.5 text-[9px] font-bold text-white/70">
                    Lv.{tier.rewards[0].level}
                  </span>
                  
                  <ShaderImageBadge
                    src="/file_00000000b2d481fd8cd233482dbeb9ef.png"
                    isWhiteBg={true}
                    className="w-8 h-8 object-contain mb-1.5 drop-shadow-md"
                  />
                  <span className="text-[11px] font-semibold text-white/90">
                    {tier.rewards[0].coins}
                  </span>
                </div>

                <div className="bg-gradient-to-br from-[#06080d] to-[#132c54]/60 rounded-lg p-2.5 flex flex-col items-center justify-center shadow-[0_4px_10px_rgba(0,0,0,0.5)]">
                  <div className="px-2 py-0.5 rounded-md bg-gradient-to-r from-[#177488] to-[#1ea3b3] text-white font-bold text-[9px] mb-1.5 shadow-sm">
                    Entry Tag
                  </div>
                  <span className="text-[11px] font-semibold text-white/90">Entry</span>
                </div>

                <div className="bg-gradient-to-br from-[#06080d] to-[#132c54]/60 rounded-lg p-2.5 flex flex-col items-center justify-center shadow-[0_4px_10px_rgba(0,0,0,0.5)]">
                  <div className="w-8 h-8 mb-1.5"></div>
                  <span className="text-[11px] font-semibold text-white/90">Frame</span>
                </div>
              </div>

              {/* Lambe Lambe Level Reward Cards */}
              <div className="flex flex-col gap-2.5 w-full">
                
                <div className="w-full relative overflow-hidden rounded-md bg-gradient-to-r from-[#06080d] via-[#080d17] to-[#132c54]/45 px-3.5 py-3 flex items-center justify-between backdrop-blur-md transition-all duration-200 hover:to-[#173a70]/60 shadow-[0_4px_12px_rgba(0,0,0,0.7)]">
                  <div className="flex flex-col justify-center z-10">
                    <span className="text-[13.5px] font-semibold text-white tracking-wide">
                      Level {tier.range.replace(/Lv\./g, '')}
                    </span>
                    <span className="text-[11px] text-gray-400 mt-0.5 font-normal">
                      Level badge upgraded
                    </span>
                  </div>
                  <div className="flex items-center gap-2 shrink-0 z-10">
                    <img
                      src={tier.medalBadgeSrc}
                      alt="Level Badge"
                      className="h-10 w-auto object-contain drop-shadow-[0_4px_10px_rgba(0,0,0,0.6)]"
                    />
                  </div>
                </div>

                <div className="w-full relative overflow-hidden rounded-md bg-gradient-to-r from-[#06080d] via-[#080d17] to-[#132c54]/45 px-3.5 py-3 flex items-center justify-between backdrop-blur-md transition-all duration-200 hover:to-[#173a70]/60 shadow-[0_4px_12px_rgba(0,0,0,0.7)]">
                  <div className="flex flex-col justify-center z-10">
                    <span className="text-[13.5px] font-semibold text-white tracking-wide">
                      Room Send image
                    </span>
                    {tIdx === 0 && (
                      <span className="text-[11px] text-gray-400 mt-0.5 font-normal">
                        Lv.5
                      </span>
                    )}
                  </div>
                  <div className="flex items-center gap-2 shrink-0 z-10">
                    <img
                      src="/file_00000000ac708211bca5d461c021b0af.png"
                      alt="Room Send"
                      className="w-8 h-8 object-contain drop-shadow-md"
                    />
                  </div>
                </div>

                {tIdx >= 2 && (
                  <div className="w-full relative overflow-hidden rounded-md bg-gradient-to-r from-[#06080d] via-[#080d17] to-[#132c54]/45 px-3.5 py-3 flex items-center justify-between backdrop-blur-md transition-all duration-200 hover:to-[#173a70]/60 shadow-[0_4px_12px_rgba(0,0,0,0.7)]">
                    <div className="flex flex-col justify-center z-10">
                      <span className="text-[13.5px] font-semibold text-white tracking-wide">
                        Background Image
                      </span>
                    </div>
                    <div className="flex items-center gap-2 shrink-0 z-10">
                      <div className="w-8 h-8 rounded-md bg-[#131f33] flex items-center justify-center">
                        <svg
                          className="w-4 h-4 text-emerald-300"
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="currentColor"
                          strokeWidth="2"
                        >
                          <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
                          <circle cx="8.5" cy="8.5" r="1.5" />
                          <path d="M21 15l-5-5L5 21" />
                        </svg>
                      </div>
                    </div>
                  </div>
                )}

                {tIdx >= 7 && (
                  <div className="w-full relative overflow-hidden rounded-md bg-gradient-to-r from-[#06080d] via-[#080d17] to-[#132c54]/45 px-3.5 py-3 flex items-center justify-between backdrop-blur-md transition-all duration-200 hover:to-[#173a70]/60 shadow-[0_4px_12px_rgba(0,0,0,0.7)]">
                    <div className="flex flex-col justify-center z-10">
                      <span className="text-[13.5px] font-semibold text-white tracking-wide">
                        Room Theme
                      </span>
                    </div>
                    <div className="flex items-center gap-2 shrink-0 z-10">
                      <div className="w-8 h-8 rounded-md bg-[#131f33] flex items-center justify-center">
                        <svg 
                          className="w-4 h-4 text-fuchsia-300" 
                          viewBox="0 0 24 24" 
                          fill="none" 
                          stroke="currentColor" 
                          strokeWidth="2"
                        >
                          <path d="M12 22c5.523 0 10-4.477 10-10S17.523 2 12 2 2 6.477 2 12s4.477 10 10 10z" />
                          <path d="M2 12h20" />
                          <path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z" />
                        </svg>
                      </div>
                    </div>
                  </div>
                )}

                {tier.rewards.slice(1).map((reward, rIdx) => (
                  <div
                    key={rIdx}
                    className="w-full relative overflow-hidden rounded-md bg-gradient-to-r from-[#06080d] via-[#080d17] to-[#132c54]/45 px-3.5 py-3 flex items-center justify-between backdrop-blur-md transition-all duration-200 hover:to-[#173a70]/60 shadow-[0_4px_12px_rgba(0,0,0,0.7)]"
                  >
                    <div className="flex flex-col justify-center z-10">
                      <span className="text-[13.5px] font-semibold text-white tracking-wide">
                        Level {reward.level} Reward
                      </span>
                    </div>
                    <div className="flex items-center gap-1.5 shrink-0 z-10">
                      <ShaderImageBadge
                        src="/file_00000000b2d481fd8cd233482dbeb9ef.png"
                        isWhiteBg={true}
                        className="w-6 h-6 object-contain drop-shadow-[0_2px_6px_rgba(0,0,0,0.8)]"
                      />
                      <span className="text-[13px] font-extrabold text-[#fcd34d] tracking-wide drop-shadow-[0_1px_3px_rgba(0,0,0,0.9)]">
                        {reward.coins}
                      </span>
                    </div>
                  </div>
                ))}

              </div>
            </div>
          ))}
        </div>
      </div>

      {/* ========================================== */}
      {/* SHEET RENDER */}
      {/* ========================================== */}
      <UpgradeSheet 
        isOpen={showUpgradeSheet} 
        onClose={() => setShowUpgradeSheet(false)} 
      />
      
    </div>
  )
}
