'use client'

import React, { useState, useEffect } from 'react'

type LeaderboardTab = 'honour' | 'charm' | 'room'

interface LeaderboardProps {
  onBack: () => void
  initialTab?: LeaderboardTab
}
type LeaderboardSubTab = 'daily' | 'weekly' | 'monthly'

// Global in-memory cache to prevent re-processing same image multiple times (Anti-Freeze Cache added)
const processedImageCache: Record<string, string> = {}
const processingPromises: Record<string, Promise<string>> = {}

// Ultra-fast Chroma Key green screen remover without WebGL crashes
export function ChromaImage({
  src,
  alt,
  className = '',
}: {
  src: string
  alt: string
  className?: string
}) {
  const [dataUrl, setDataUrl] = useState<string>(processedImageCache[src] || '')

  useEffect(() => {
    let isMounted = true
    
    if (processedImageCache[src]) {
      setDataUrl(processedImageCache[src])
      return
    }

    if (!processingPromises[src]) {
      processingPromises[src] = new Promise((resolve) => {
        const img = new Image()
        img.crossOrigin = 'anonymous'
        img.src = src

        img.onload = () => {
          const canvas = document.createElement('canvas')
          canvas.width = img.naturalWidth || 300
          canvas.height = img.naturalHeight || 300
          const ctx = canvas.getContext('2d', { willReadFrequently: true })

          if (ctx) {
            ctx.drawImage(img, 0, 0)
            const imgData = ctx.getImageData(0, 0, canvas.width, canvas.height)
            const data = imgData.data

            for (let i = 0; i < data.length; i += 4) {
              const r = data[i], g = data[i + 1], b = data[i + 2]
              if (g > 50 && g > r * 1.15 && g > b * 1.15) {
                data[i + 3] = 0
              }
            }
            ctx.putImageData(imgData, 0, 0)
            const finalUrl = canvas.toDataURL('image/png')
            processedImageCache[src] = finalUrl
            resolve(finalUrl)
          }
        }
      })
    }

    processingPromises[src].then((url) => {
      if (isMounted) setDataUrl(url)
    })

    return () => { isMounted = false }
  }, [src])

  if (!dataUrl) {
    return <div className={`opacity-0 ${className}`} style={{ minHeight: '60px' }} />
  }

  return (
    <img
      src={dataUrl}
      alt={alt}
      className={className}
      draggable="false"
    />
  )
}

export default function Leaderboard({ onBack, initialTab = 'honour' }: LeaderboardProps) {
  const [activeTab, setActiveTab] = useState<LeaderboardTab>(initialTab)
  const [activeSubTab, setActiveSubTab] = useState<LeaderboardSubTab>('daily')
  const [showRankingRules, setShowRankingRules] = useState(false)

  const tabs: { id: LeaderboardTab; label: string }[] = [
    { id: 'honour', label: 'Honour' },
    { id: 'charm', label: 'Charm' },
    { id: 'room', label: 'Room' },
  ]

  const subTabs: { id: LeaderboardSubTab; label: string }[] = [
    { id: 'daily', label: 'Daily' },
    { id: 'weekly', label: 'Weekly' },
    { id: 'monthly', label: 'Monthly' },
  ]

  const tabImages: Record<LeaderboardTab, { top: string }> = {
    honour: { top: '/file_00000000b83c81fa93d3e53e046c1b81.png' },
    charm: { top: '/file_0000000086f481fa8653fc12d2577596.png' },
    room: { top: '/file_00000000619c822f8a1577f69e039527.png' },
  }

  // Find index of active sub-tab for the moveable highlight
  const activeSubTabIndex = subTabs.findIndex(st => st.id === activeSubTab)

  // Rank 4 to 50
  const rankCards = Array.from({ length: 47 }, (_, i) => i + 4)

  return (
    <div
      className="min-h-screen bg-[#1A0204] text-white overflow-y-auto overflow-x-hidden flex flex-col select-none relative"
      style={{ touchAction: 'manipulation', WebkitUserSelect: 'none' }}
    >
      {/* BACKGROUND TOP IMAGE: 60vh blended into Ultra-Dark Red */}
      <div
        className="absolute top-0 left-0 w-full pointer-events-none z-0 overflow-hidden"
        style={{
          height: '60vh',
          maskImage: 'linear-gradient(to bottom, rgba(0,0,0,1) 45%, rgba(0,0,0,0) 100%)',
          WebkitMaskImage: 'linear-gradient(to bottom, rgba(0,0,0,1) 45%, rgba(0,0,0,0) 100%)',
        }}
      >
        <img
          key={`${activeTab}-top`}
          src={tabImages[activeTab].top}
          alt={`${activeTab} top`}
          className="w-full h-full object-cover"
          draggable="false"
        />
      </div>

      {/* FIXED TOP HEADER */}
      <header
        className="relative z-50 flex flex-col w-full"
        style={{ paddingTop: 'calc(env(safe-area-inset-top, 0px) + 20px)' }}
      >
        {/* Row 1: Back Icon (Corner) - Tabs Card (Center) - Info Icon (Corner) */}
        <div className="relative flex items-center justify-center w-full h-[40px] mb-1.5">
          
          {/* Back Button as Image - Ekdam Left Corner */}
          <button
            onClick={onBack}
            className="absolute left-2 flex items-center justify-center active:opacity-70 transition-opacity p-1"
            aria-label="Back"
          >
            <img 
              src="/file_0000000051d881f5af4f9cf84a56dcd3.png" 
              alt="Back" 
              className="w-9 h-9 object-contain"
              draggable="false"
            />
          </button>

          {/* Main Tabs Container - Center (Balanced slim + wide) */}
          <div className="flex items-center justify-between h-[30px] border-[1px] border-[#D4AF37] rounded-full bg-[#110A07]/85 w-[70%] max-w-[440px] overflow-hidden shadow-[0_0_15px_rgba(0,0,0,0.6)] p-[3px]">
            {tabs.map((tab) => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`relative flex-1 text-[14px] font-semibold h-[24px] rounded-full transition-all flex items-center justify-center tracking-wide ${
                  activeTab === tab.id ? 'text-white' : 'text-[#8A857D]'
                }`}
              >
                {activeTab === tab.id && (
                  <span className="absolute inset-0 bg-gradient-to-b from-[#F0BC60] via-[#C67F2A] to-[#7A3A0E] rounded-full shadow-[inset_0_2px_4px_rgba(255,255,255,0.45),0_0_8px_rgba(230,180,90,0.5)]" />
                )}
                <span className="relative z-10 drop-shadow-md">{tab.label}</span>
              </button>
            ))}
          </div>

          {/* Info Button - Image, Ekdam Right Corner */}
          <button
            onClick={() => setShowRankingRules(true)}
            className="absolute right-2 flex items-center justify-center active:opacity-70 transition-opacity p-1"
            aria-label="Ranking Rules"
          >
            <img 
              src="/file_0000000073ec820b832b6dafb168dabe.png" 
              alt="Info" 
              className="w-9 h-9 object-contain"
              draggable="false"
            />
          </button>
        </div>

        {/* 1. SUB-TABS SECTION (Daily, Weekly, Monthly) - CENTERED */}
        <div className="relative w-full flex justify-center mt-1">
          <div className="relative w-[220px] h-[40px] z-10 flex items-center justify-start gap-1 shrink-0">
            {subTabs.map((st) => (
              <button
                key={st.id}
                onClick={() => setActiveSubTab(st.id)}
                className="relative z-10 flex-1 flex items-center justify-center text-[15px] font-bold transition-colors duration-200"
                style={{
                  color: '#FFFFFF',
                  textShadow: '0 1px 3px rgba(0,0,0,0.9)',
                }}
              >
                {st.label}
              </button>
            ))}

            {/* EK HI Moveable Golden Highlight Shape */}
            <span
              className="absolute z-0 bottom-0 top-[2px] h-full w-[33.33%] bg-gradient-to-b from-[#D4AF37]/60 via-[#D4AF37]/25 to-transparent rounded-md transition-transform duration-300 ease-out"
              style={{
                transform: `translateX(calc(${activeSubTabIndex * 100}%))`,
                boxShadow: '0 -2px 8px rgba(212, 175, 55, 0.6)',
              }}
            >
              <span className="absolute left-[25%] right-[25%] top-[-1px] h-[3px] bg-[#FFE9A8] rounded-full scale-y-[1.2] blur-[0.5px] shadow-[0_0_8px_rgba(255,215,0,1)]" />
            </span>
          </div>
        </div>

        {/* 2. TOP IMAGES PODIUM (Top 1, 2, 3) */}
        <div className="relative z-10 w-full shrink-0 flex flex-col items-center">
          <div className="w-full flex flex-col items-center gap-0.5 mt-5">
            {/* Row 1: Top 1 (Center) */}
            <div className="flex justify-center w-full">
              <ChromaImage
                src="/1787994771034~2.jpg"
                alt="Top 1"
                className="w-[44vw] max-w-[180px] min-w-[132px] h-auto object-contain drop-shadow-2xl"
              />
            </div>

            {/* Row 2: Top 2 & Top 3 */}
            <div className="flex justify-between items-center w-full gap-1 px-1 mt-4">
              <ChromaImage
                src="/1787994751636~2.jpg"
                alt="Top 2"
                className="w-[40vw] max-w-[160px] min-w-0 h-auto object-contain drop-shadow-lg -ml-1"
              />
              <ChromaImage
                src="/1787994761762~2.jpg"
                alt="Top 3"
                className="w-[40vw] max-w-[160px] min-w-0 h-auto object-contain drop-shadow-lg -mr-1"
              />
            </div>
          </div>
        </div>

        {/* 3. SPACE (5vh) */}
        <div style={{ height: '5vh' }} className="w-full shrink-0 relative z-10" />
      </header>

      {/* 4. RANK CARDS 4 TO 50 */}
      <div className="relative z-10 flex-1">
        {rankCards.map((rank) => (
          <div
            key={rank}
            className="relative w-full flex items-center justify-start overflow-hidden shrink-0 h-[80px]"
          >
            <ChromaImage
              src="/1787992320047~2.jpg"
              alt={`Rank ${rank}`}
              className="absolute inset-0 w-full h-full object-fill"
            />
            <span className="relative z-10 left-10 text-white font-bold text-lg">{rank}</span>
          </div>
        ))}
      </div>

      {showRankingRules && (
        <div
          className="fixed inset-0 z-[100] flex items-end justify-center bg-black/60"
          onClick={() => setShowRankingRules(false)}
          role="presentation"
        >
          <section
            className="w-full max-h-[82vh] overflow-y-auto rounded-t-2xl border-t border-[#D4AF37]/70 bg-[#1A0204] px-5 pt-5 pb-8 text-white shadow-2xl relative"
            role="dialog"
            aria-modal="true"
            aria-labelledby="ranking-rules-title"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="mb-5 text-center">
              <h2 id="ranking-rules-title" className="text-xl font-bold text-[#E7B865]">Rules</h2>
            </div>

            <div className="space-y-4 text-sm leading-6 text-white/90">
              <p><strong>Contribution Leaderboard:</strong> Your ranking on this leaderboard is determined by the total number of Coins you have spent in the room. The higher the amount spent, the higher your position on the leaderboard.</p>
              <p><strong>Charm Leaderboard:</strong> Your ranking on this leaderboard is determined by the total number of Coins you Receive. The higher the amount spent, the higher your position on the leaderboard.</p>
              <p><strong>Room Leaderboard:</strong> Your ranking on this leaderboard is determined by the total number of Coins you have spent in the room. The higher the amount spent, the higher your position on the leaderboard.</p>
            </div>

            <div className="my-5 h-px bg-[#D4AF37]/30" />
            <h3 className="mb-2 font-semibold text-[#E7B865]">Data Periods</h3>
            <ol className="list-decimal space-y-2 pl-5 text-sm leading-6 text-white/90">
              <li>Daily leaderboard data is finalized every day at (5:30+GMT).</li>
              <li>Weekly leaderboard data is finalized every Monday at (5:30+GMT).</li>
              <li>Monthly leaderboard data is finalized on the first day of every month at (5:30+GMT).</li>
            </ol>

            <p className="mt-5 text-sm leading-6 text-white/90"><strong>Note:</strong> Users who violate platform rules are ineligible to participate in the rankings.</p>
            <p className="mt-3 text-sm leading-6 text-white/90">The platform reserves the right to make the final decision.</p>
            <p className="mt-5 text-right text-sm font-semibold text-[#E7B865]">Regard Zyora team</p>
          </section>
        </div>
      )}

      {/* 5. FIXED BOTTOM USER CARD */}
      <div className="fixed bottom-0 left-0 w-full h-[90px] px-0 py-0 z-50 pointer-events-auto shadow-[0_-5px_20px_rgba(0,0,0,0.8)] border-t-[1.5px] border-[#694B2E] bg-gradient-to-b from-[#3E2114] via-[#2A1309] to-[#120703]">
        <div className="relative w-full h-full flex items-center justify-start px-6 gap-5">
          {/* Yahan se 100+ aur circle hata diya gaya hai bss khali space hai ab */}
        </div>
      </div>
    </div>
  )
}
