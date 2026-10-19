'use client'

import React, { useState, useEffect, useRef } from 'react'
import { 
  ArrowLeft, 
  Link2, 
  MoreHorizontal, 
  X,
  ChevronRight,
  HelpCircle
} from 'lucide-react'

// WebShader / Chroma Key Component jo sirf GREEN background remove karta hai
const ChromaKeyImage = ({ 
  src, 
  alt, 
  className = "", 
  style = {} 
}: { 
  src: string; 
  alt: string; 
  className?: string; 
  style?: React.CSSProperties 
}) => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null)

  useEffect(() => {
    const img = new Image()
    img.crossOrigin = 'anonymous'
    img.src = src

    img.onload = () => {
      const canvas = canvasRef.current
      if (!canvas) return
      const ctx = canvas.getContext('2d')
      if (!ctx) return

      canvas.width = img.naturalWidth || img.width
      canvas.height = img.naturalHeight || img.height

      ctx.drawImage(img, 0, 0)
      const imgData = ctx.getImageData(0, 0, canvas.width, canvas.height)
      const data = imgData.data

      for (let i = 0; i < data.length; i += 4) {
        const r = data[i]
        const g = data[i + 1]
        const b = data[i + 2]

        const maxRB = Math.max(r, b)
        const greenDifference = g - maxRB

        if (greenDifference > 30 && g > 60) {
          data[i + 3] = 0
        } else if (greenDifference > 10 && g > 50) {
          const factor = (greenDifference - 10) / 20
          data[i + 3] = Math.round(data[i + 3] * (1 - factor))
          data[i + 1] = maxRB
        }
      }
      ctx.putImageData(imgData, 0, 0)
    }
  }, [src])

  return (
    <canvas ref={canvasRef} className={className} style={style} aria-label={alt} />
  )
}

// Special WebShader / Component jo sirf WHITE background remove karta hai
const WhiteKeyImage = ({ 
  src, 
  alt, 
  className = "", 
  style = {} 
}: { 
  src: string; 
  alt: string; 
  className?: string; 
  style?: React.CSSProperties 
}) => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null)

  useEffect(() => {
    const img = new Image()
    img.crossOrigin = 'anonymous'
    img.src = src

    img.onload = () => {
      const canvas = canvasRef.current
      if (!canvas) return
      const ctx = canvas.getContext('2d')
      if (!ctx) return

      canvas.width = img.naturalWidth || img.width
      canvas.height = img.naturalHeight || img.height

      ctx.drawImage(img, 0, 0)
      const imgData = ctx.getImageData(0, 0, canvas.width, canvas.height)
      const data = imgData.data

      for (let i = 0; i < data.length; i += 4) {
        const r = data[i]
        const g = data[i + 1]
        const b = data[i + 2]

        if (r > 210 && g > 210 && b > 210) {
          const avg = (r + g + b) / 3
          if (avg > 240) {
            data[i + 3] = 0
          } else {
            const factor = (avg - 210) / 30
            data[i + 3] = Math.round(data[i + 3] * (1 - factor))
          }
        }
      }
      ctx.putImageData(imgData, 0, 0)
    }
  }, [src])

  return (
    <canvas ref={canvasRef} className={className} style={style} aria-label={alt} />
  )
}

// SVG Icons
const WhatsAppIcon = ({ size = 24, className = "" }: { size?: number; className?: string }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="currentColor" className={className}>
    <path d="M12.04 2c-5.46 0-9.91 4.45-9.91 9.91 0 1.75.46 3.45 1.32 4.95L2.05 22l5.25-1.38c1.45.79 3.08 1.21 4.74 1.21 5.46 0 9.91-4.45 9.91-9.91 0-2.65-1.03-5.14-2.9-7.01A9.816 9.816 0 0 0 12.04 2zm.01 1.67c2.2 0 4.26.86 5.82 2.42a8.225 8.225 0 0 1 2.41 5.83c0 4.54-3.7 8.24-8.24 8.24-1.48 0-2.93-.4-4.2-1.15l-.3-.18-3.12.82.83-3.04-.2-.31a8.196 8.196 0 0 1-1.26-4.38c0-4.54 3.7-8.24 8.24-8.24zm4.52 11.66c-.25-.13-1.47-.72-1.7-.81-.23-.08-.39-.13-.56.13-.17.25-.64.81-.79.97-.14.17-.29.19-.54.06-.25-.13-1.06-.39-2.02-1.25-.75-.67-1.26-1.5-1.4-1.75-.15-.25-.02-.39.11-.51.11-.11.25-.29.37-.44.13-.14.17-.25.25-.42.08-.17.04-.31-.02-.44-.06-.13-.56-1.35-.77-1.85-.2-.49-.41-.42-.56-.43h-.48c-.17 0-.44.06-.67.31-.23.25-.88.86-.88 2.1 0 1.24.9 2.44 1.03 2.61.13.17 1.77 2.7 4.29 3.79.6.26 1.07.41 1.44.53.6.19 1.15.16 1.58.1.48-.07 1.47-.6 1.68-1.18.21-.58.21-1.07.15-1.18-.07-.1-.23-.17-.48-.29z"/>
  </svg>
)

const FacebookIcon = ({ size = 24, className = "" }: { size?: number; className?: string }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="currentColor" className={className}>
    <path d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z"/>
  </svg>
)

interface InviteFriendsProps {
  onBack?: () => void
  onClose?: () => void
}

export default function InviteFriends({ onBack, onClose }: InviteFriendsProps) {
  const handleBack = onBack || onClose || (() => {})
  const [copied, setCopied] = useState(false)
  const [isSheetOpen, setIsSheetOpen] = useState(false)
  
  // Invite Friends → SIRF EK LINE wala rules
  const [isRulesPopupOpen, setIsRulesPopupOpen] = useState(false) 
  const [isGiftRulesPopupOpen, setIsGiftRulesPopupOpen] = useState(false)
  const [isRechargeRulesPopupOpen, setIsRechargeRulesPopupOpen] = useState(false)
  // ? icon → NAYE rules
  const [isHelpRulesPopupOpen, setIsHelpRulesPopupOpen] = useState(false)
  
  const userId = typeof window !== 'undefined' ? (localStorage.getItem('userUID') || 'N/A') : 'N/A'
  const inviteCode = userId !== 'N/A' ? userId : 'WELCOME123'
  const inviteLink = `https://yourapp.com/invite/${inviteCode}`

  const handleShare = (platform: string) => {
    const shareText = `Join me on this amazing app! Use my invite code: ${inviteCode} or click here: ${inviteLink}`
    switch(platform) {
      case 'whatsapp':
        window.open(`https://wa.me/?text=${encodeURIComponent(shareText)}`, '_blank')
        break
      case 'facebook':
        window.open(`https://www.facebook.com/sharer/sharer.php?u=${encodeURIComponent(inviteLink)}`, '_blank')
        break
      case 'copy':
        navigator.clipboard.writeText(inviteLink).then(() => { setCopied(true); setTimeout(() => setCopied(false), 2000) })
        break
      case 'more':
        if (navigator.share) {
          navigator.share({ title: 'Invite Code', text: shareText, url: inviteLink }).catch(() => {})
        }
        break
      default: break
    }
  }

  return (
    <div className="min-h-screen relative w-full h-full bg-[#4d0515] overflow-y-auto overflow-x-hidden select-none pb-[25vh]">
      
      {/* Top Background Image */}
      <div className="absolute top-0 left-0 right-0 h-[50vh] z-0 pointer-events-none overflow-hidden">
        <ChromaKeyImage src="/IMG-20260830-WA0076.jpg" alt="Top Background" className="w-full h-full object-cover object-top" />
        <div className="absolute inset-0 bg-gradient-to-b from-transparent via-transparent to-[#4d0515]" />
      </div>

      {/* Main Images Container */}
      <div className="absolute top-[45vh] left-1/2 -translate-x-1/2 w-full max-w-md z-0 flex flex-col justify-center items-center gap-3">
        
        {/* 1. TOP ORNAMENT IMAGE BOX */}
        <div className="relative w-full">
          <ChromaKeyImage src="/1788074201753~2.jpg" alt="Middle Ornament" className="w-full object-contain pointer-events-none" />
          
          <div className="absolute inset-0 z-10 pointer-events-none">
            <div 
              className="absolute text-yellow-400 font-bold text-sm drop-shadow-md"
              style={{ bottom: '15px', left: '30px' }}
            >
              Friend's Invite
            </div>
            
            <div 
              className="absolute text-yellow-400 font-bold text-sm drop-shadow-md"
              style={{ bottom: '15px', right: '30px' }}
            >
              0
            </div>
          </div>
        </div>


        {/* 2. MY GAIN IMAGE BOX */}
        <div className="relative w-full">
          <ChromaKeyImage src="/1788100666322~2.jpg" alt="My gain" className="w-full object-contain pointer-events-none" />

          <div className="absolute inset-0 z-10">
            {/* ROW 1: Invite Friends */}
            <div className="absolute text-yellow-400 font-bold text-sm" style={{ top: '100px', left: '40px' }}>
              Invite Friends
            </div>
            <div 
              className="absolute flex items-center gap-1 cursor-pointer active:scale-95 transition-transform" 
              style={{ top: '100px', right: '35px' }}
              onClick={() => setIsRulesPopupOpen(true)}
            >
              <span className="text-yellow-400 text-xs font-semibold">Get</span>
              <WhiteKeyImage src="/file_00000000e56882119c217d508b6733dc.png" alt="coin" className="object-contain" style={{ width: '20px', height: '20px' }} />
              <span className="text-yellow-400 text-sm font-bold">250000</span>
              <div className="w-4 h-4 rounded-full bg-yellow-400 flex items-center justify-center ml-0.5">
                <ChevronRight size={12} strokeWidth={3} className="text-[#4d0515] -mr-0.5" />
              </div>
            </div>

            {/* ROW 2: Friends Recharge */}
            <div className="absolute flex flex-col" style={{ top: '175px', left: '40px' }}>
              <span className="text-yellow-400 font-bold text-sm leading-tight">Friends Recharge</span>
              <span className="text-yellow-300 text-[10px] font-medium tracking-wider">&gt;500000 Coins</span>
            </div>
            <div 
              className="absolute flex items-center gap-1 cursor-pointer active:scale-95 transition-transform" 
              style={{ top: '175px', right: '35px' }}
              onClick={() => setIsRechargeRulesPopupOpen(true)}
            >
              <span className="text-yellow-400 text-xs font-semibold">Get</span>
              <WhiteKeyImage src="/file_00000000e56882119c217d508b6733dc.png" alt="coin" className="object-contain" style={{ width: '20px', height: '20px' }} />
              <span className="text-yellow-400 text-sm font-bold">250000</span>
              <div className="w-4 h-4 rounded-full bg-yellow-400 flex items-center justify-center ml-0.5">
                <ChevronRight size={12} strokeWidth={3} className="text-[#4d0515] -mr-0.5" />
              </div>
            </div>

            {/* ROW 3: Friends Send Gift */}
            <div className="absolute flex flex-col" style={{ top: '255px', left: '40px' }}>
              <span className="text-yellow-400 font-bold text-sm leading-tight">Friends Send</span>
              <span className="text-yellow-400 font-bold text-sm leading-tight">Gift</span>
            </div>
            <div 
              className="absolute flex items-center gap-1 cursor-pointer active:scale-95 transition-transform" 
              style={{ top: '255px', right: '35px' }}
              onClick={() => setIsGiftRulesPopupOpen(true)}
            >
              <span className="text-yellow-400 text-xs font-semibold">Get</span>
              <WhiteKeyImage src="/file_00000000e56882119c217d508b6733dc.png" alt="coin" className="object-contain" style={{ width: '20px', height: '20px' }} />
              <span className="text-yellow-400 text-sm font-bold pr-1">6%</span>
              <div className="w-4 h-4 rounded-full bg-yellow-400 flex items-center justify-center ml-0.5">
                <ChevronRight size={12} strokeWidth={3} className="text-[#4d0515] -mr-0.5" />
              </div>
            </div>
          </div>
        </div>


        {/* 3. COINS IMAGE BOX */}
        <div className="relative w-full">
          <ChromaKeyImage src="/1788102034828~2.jpg" alt="Coins" className="w-full object-contain pointer-events-none" />
          
          <div className="absolute inset-0 z-10">
            
            {/* Top Center Items (Coin, 0, Claim) */}
            <div className="absolute flex flex-col items-center w-full" style={{ bottom: '31vh' }}>
              <WhiteKeyImage src="/file_00000000e56882119c217d508b6733dc.png" alt="coin" className="object-contain" style={{ width: '50px', height: '50px' }} />
              <div className="text-yellow-400 font-bold text-3xl drop-shadow-md leading-none mt-1">0</div>
              
              <div className="relative mt-3 flex items-center justify-center">
                <button className="relative z-10 bg-gradient-to-b from-[#e62e2e] via-[#b80d0d] to-[#7a0000] text-yellow-300 font-extrabold text-sm rounded-full px-10 py-1.5 shadow-[inset_0_2px_4px_rgba(255,255,255,0.3),inset_0_-2px_4px_rgba(0,0,0,0.6),0_4px_0_#4a0000] border-[1.5px] border-yellow-400 active:shadow-[inset_0_2px_4px_rgba(255,255,255,0.3),inset_0_-2px_4px_rgba(0,0,0,0.6),0_0px_0_#4a0000] active:translate-y-1 transition-all cursor-pointer drop-shadow-md">
                  Claim
                </button>
              </div>
            </div>

            {/* Bottom Left Items */}
            <div className="absolute flex flex-col items-center" style={{ bottom: '9vh', left: '85px' }}>
              <div className="text-yellow-400 font-bold text-2xl drop-shadow-md">0</div>
              <div className="text-yellow-400 text-[10px] font-semibold text-center leading-tight mt-1">Number<br/>Invitation</div>
            </div>

            {/* Bottom Right Items */}
            <div className="absolute flex flex-col items-center" style={{ bottom: '10vh', right: '80px' }}>
              <WhiteKeyImage src="/file_00000000e56882119c217d508b6733dc.png" alt="coin" className="object-contain" style={{ width: '40px', height: '40px' }} />
              <div className="text-yellow-400 font-bold text-2xl drop-shadow-md leading-none mt-1">0</div>
              <div className="text-yellow-400 text-[10px] font-semibold text-center leading-tight mt-1">Total Coins<br/>Rewards</div>
            </div>

          </div>
        </div>

        <div className="w-full h-[8vh] shrink-0 pointer-events-none"></div>
      </div>

      {/* Bottom Floating Decor Image */}
      <div 
        onClick={() => setIsSheetOpen(true)}
        className="fixed bottom-0 left-0 right-0 z-20 flex justify-end pr-2 cursor-pointer active:scale-[0.98] transition-transform"
      >
        <ChromaKeyImage src="/1788074191602~2.jpg" alt="Bottom Decor" className="w-[88%] max-w-[360px] md:max-w-md object-contain object-bottom pointer-events-auto" />
      </div>

      {/* Back Icon */}
      <div className="fixed top-0 left-0 z-[60] flex items-center pl-0 pr-4 py-4 pt-[calc(env(safe-area-inset-top,0px)+24px)] pointer-events-auto">
        <button
          onClick={handleBack}
          className="p-1 text-white hover:text-white/80 transition-colors cursor-pointer active:scale-95"
          aria-label="Back"
        >
          <ArrowLeft size={28} />
        </button>
      </div>

      {/* Question Mark Icon */}
      <div className="fixed top-0 right-0 z-[60] flex items-center pl-4 pr-4 py-4 pt-[calc(env(safe-area-inset-top,0px)+24px)] pointer-events-auto">
        <button
          onClick={() => setIsHelpRulesPopupOpen(true)}
          className="p-1 text-white hover:text-white/80 transition-colors cursor-pointer active:scale-95"
          aria-label="Details"
        >
          <HelpCircle size={28} />
        </button>
      </div>

      {/* --- INVITE FRIENDS RULES POPUP (SIRF EK LINE) --- */}
      {isRulesPopupOpen && (
        <div 
          className="fixed inset-0 z-[100] flex items-center justify-center bg-black/60 px-4"
          onClick={() => setIsRulesPopupOpen(false)}
        >
          <div 
            className="relative w-full max-w-[300px]"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="relative bg-[#750a75] rounded-2xl border-[3px] border-[#facc15] p-4 pt-5 pb-6 shadow-[0_0_20px_rgba(250,204,21,0.3)]">
              <div className="text-center mb-3">
                <span className="text-yellow-400 font-bold text-lg drop-shadow-md tracking-wider">
                  Rules
                </span>
              </div>
              <div className="text-white text-left text-[12px] leading-relaxed font-medium">
                If invite friend download &amp; enter the Invite code in the input Given. Then a Inverter will Receive a 250,000Coins rewards hand to hand in there coins wallet
              </div>
            </div>
          </div>
        </div>
      )}

      {/* --- FRIENDS SEND GIFT RULES POPUP --- */}
      {isGiftRulesPopupOpen && (
        <div 
          className="fixed inset-0 z-[100] flex items-center justify-center bg-black/60 px-4"
          onClick={() => setIsGiftRulesPopupOpen(false)}
        >
          <div 
            className="relative w-full max-w-[300px]"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="relative bg-[#750a75] rounded-2xl border-[3px] border-[#facc15] p-4 pt-5 pb-6 shadow-[0_0_20px_rgba(250,204,21,0.3)]">
              <div className="text-center mb-3">
                <span className="text-yellow-400 font-bold text-lg drop-shadow-md tracking-wider">
                  Rules
                </span>
              </div>
              <div className="text-white text-center text-[12px] leading-relaxed font-medium">
                If Invited friend Will send Gift to any one So a Inviter user will receive 6% of the Sended gift Value.
              </div>
            </div>
          </div>
        </div>
      )}

      {/* --- FRIENDS RECHARGE RULES POPUP --- */}
      {isRechargeRulesPopupOpen && (
        <div 
          className="fixed inset-0 z-[100] flex items-center justify-center bg-black/60 px-4"
          onClick={() => setIsRechargeRulesPopupOpen(false)}
        >
          <div 
            className="relative w-full max-w-[300px]"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="relative bg-[#750a75] rounded-2xl border-[3px] border-[#facc15] p-4 pt-5 pb-6 shadow-[0_0_20px_rgba(250,204,21,0.3)]">
              <div className="text-center mb-3">
                <span className="text-yellow-400 font-bold text-lg drop-shadow-md tracking-wider">
                  Rules
                </span>
              </div>
              <div className="text-white text-center text-[12px] leading-relaxed font-medium">
                If the Invited friend Recharge more then &gt;500000 so a inviter User will receive 250000 coins In there Coins wallet
              </div>
            </div>
          </div>
        </div>
      )}

      {/* --- ? ICON KA NAYA RULES POPUP --- */}
      {isHelpRulesPopupOpen && (
        <div 
          className="fixed inset-0 z-[100] flex items-center justify-center bg-black/60 px-4"
          onClick={() => setIsHelpRulesPopupOpen(false)}
        >
          <div 
            className="relative w-full max-w-[300px]"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="relative bg-[#750a75] rounded-2xl border-[3px] border-[#facc15] p-4 pt-5 pb-6 shadow-[0_0_20px_rgba(250,204,21,0.3)]">
              <div className="text-center mb-3">
                <span className="text-yellow-400 font-bold text-lg drop-shadow-md tracking-wider">
                  Rules
                </span>
              </div>
              <div className="text-white text-left text-[11px] leading-relaxed font-medium space-y-2">
                <p>
                  1. Invite a friend to register and receive instant coins. You will get 100,000 coins when they log in. If your friend logs into the Zyora app the next day or within 3 days, they will receive an additional 150,000 coins.
                </p>
                <p>
                  2. If your invited friend recharges 500,000 coins within 90 days, you will receive 6% of the coins from each recharge. Additionally, if your friend sends gift coins, you will receive 6% of the coins from the gifts they send.
                </p>
                <p>
                  3. The Zyora app must not already be downloaded or registered on your friend's phone, and they must not be part of another account's network. The invitation will be considered successful—and rewards granted—only when all these conditions are met.
                </p>
                <p>
                  4. Invite your friends; you will earn free coins when they recharge or send gifts.
                </p>
                <p className="pt-1 text-right text-[10px] italic text-yellow-200">
                  Best regards, The Zyora Team
                </p>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Bottom Sheet (WhatsApp / Facebook / Copy / More) - No blur, no dark overlay */}
      <div className={`fixed bottom-0 left-0 right-0 h-[20vh] bg-white rounded-t-3xl z-50 transition-transform duration-300 ease-out shadow-2xl flex flex-col px-4 py-2 border-0 outline-none ${ isSheetOpen ? 'translate-y-0' : 'translate-y-full' }`} style={{ border: 'none' }}>
        <div className="flex justify-end pt-1 pr-1">
          <button onClick={() => setIsSheetOpen(false)} className="text-gray-400 hover:text-gray-700 p-1 cursor-pointer transition-colors">
            <X size={18} />
          </button>
        </div>

        <div className="flex-1 grid grid-cols-4 gap-2 items-center justify-center text-center pb-2">
          <button onClick={() => handleShare('whatsapp')} className="flex flex-col items-center justify-center gap-1.5 group cursor-pointer">
            <div className="w-12 h-12 rounded-full bg-[#25D366] text-white flex items-center justify-center shadow-md group-hover:scale-105 transition-transform"><WhatsAppIcon size={24} /></div>
            <span className="text-[11px] font-medium text-gray-800">WhatsApp</span>
          </button>
          <button onClick={() => handleShare('facebook')} className="flex flex-col items-center justify-center gap-1.5 group cursor-pointer">
            <div className="w-12 h-12 rounded-full bg-[#1877F2] text-white flex items-center justify-center shadow-md group-hover:scale-105 transition-transform"><FacebookIcon size={22} /></div>
            <span className="text-[11px] font-medium text-gray-800">Facebook</span>
          </button>
          <button onClick={() => handleShare('copy')} className="flex flex-col items-center justify-center gap-1.5 group cursor-pointer">
            <div className="w-12 h-12 rounded-full bg-[#0088cc] text-white flex items-center justify-center shadow-md group-hover:scale-105 transition-transform"><Link2 size={22} /></div>
            <span className="text-[11px] font-medium text-gray-800">Copy Link</span>
          </button>
          <button onClick={() => handleShare('more')} className="flex flex-col items-center justify-center gap-1.5 group cursor-pointer">
            <div className="w-12 h-12 rounded-full bg-orange-500 text-white flex items-center justify-center shadow-md group-hover:scale-105 transition-transform"><MoreHorizontal size={22} /></div>
            <span className="text-[11px] font-medium text-gray-800">More</span>
          </button>
        </div>
        {copied && <div className="text-center text-[11px] text-blue-600 font-medium pb-1 animate-pulse">✓ Link copied!</div>}
      </div>
    </div>
  )
      }
