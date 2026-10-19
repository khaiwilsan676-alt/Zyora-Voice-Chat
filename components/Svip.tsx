'use client'

import { useEffect, useRef } from 'react'

interface SvipProps {
  onBack?: () => void
}

export default function Svip({ onBack }: SvipProps) {
  const videoRef = useRef<HTMLVideoElement>(null)
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const rafRef = useRef<number | null>(null)

  // Realtime green screen removal from video via canvas
  useEffect(() => {
    const video = videoRef.current
    const canvas = canvasRef.current
    if (!video || !canvas) return

    const ctx = canvas.getContext('2d', { willReadFrequently: true })
    if (!ctx) return

    let stopped = false

    const render = () => {
      if (stopped) return

      if (video.readyState >= 2 && video.videoWidth) {
        const w = video.videoWidth
        const h = video.videoHeight

        if (canvas.width !== w || canvas.height !== h) {
          canvas.width = w
          canvas.height = h
        }

        ctx.clearRect(0, 0, w, h)
        ctx.drawImage(video, 0, 0, w, h)

        const frame = ctx.getImageData(0, 0, w, h)
        const d = frame.data

        for (let i = 0; i < d.length; i += 4) {
          const r = d[i]
          const g = d[i + 1]
          const b = d[i + 2]

          // green screen remove
          if (g > 60 && g > r * 1.15 && g > b * 1.15) {
            d[i + 3] = 0
          }
        }

        ctx.putImageData(frame, 0, 0)
      }

      rafRef.current = requestAnimationFrame(render)
    }

    video.play().catch(() => {})
    rafRef.current = requestAnimationFrame(render)

    return () => {
      stopped = true
      if (rafRef.current) cancelAnimationFrame(rafRef.current)
    }
  }, [])

  return (
    <div className="fixed inset-0 bg-black overflow-y-auto overflow-x-hidden select-none">
      {/* ===== Relative wrapper so we can overlay middle image on top of both sections ===== */}
      <div className="relative w-full">
        {/* ============ TOP SECTION (40vh) ============ */}
        {/* z-30 => top image bottom background ke upar rahega */}
        <div className="relative w-full z-30" style={{ height: '40vh' }}>
          {/* BG image */}
          <img
            src="/file_00000000bd448211892548a9f0469619.png"
            alt=""
            className="absolute inset-0 w-full h-full object-cover z-0"
            draggable="false"
          />

          {/* Header (safe area) */}
          <header
            className="relative z-30 flex items-center justify-between w-full px-3 py-2"
            style={{
              paddingTop: 'calc(env(safe-area-inset-top, 0px) + 8px)',
            }}
          >
            {/* Left: Back arrow */}
            <button
              onClick={onBack}
              aria-label="Back"
              className="flex items-center justify-center p-1 active:opacity-70 transition-opacity"
            >
              <svg
                width="26"
                height="26"
                viewBox="0 0 24 24"
                fill="none"
                stroke="#FFFFFF"
                strokeWidth="2.5"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <line x1="19" y1="12" x2="5" y2="12" />
                <polyline points="12 19 5 12 12 5" />
              </svg>
            </button>

            {/* Center: SVIP */}
            <span
              className="absolute left-1/2 -translate-x-1/2 font-black text-white text-[20px] tracking-wider"
              style={{
                top:
                  'calc(max(env(safe-area-inset-top, 0px), var(--status-bar-height, 0px)) + 12px)',
              }}
            >
              SVIP
            </span>

            {/* Right: images corner to corner */}
            <div className="flex items-center gap-2">
              <img
                src="/IMG_20260918_170725.png"
                alt=""
                className="w-7 h-7 object-contain"
                draggable="false"
              />
              <img
                src="/IMG_20260918_170736.png"
                alt="Crown"
                className="w-7 h-7 object-contain"
                draggable="false"
              />
            </div>
          </header>

          {/* ============ VIDEO (Bigger + slightly up, Green removed) ============ */}
          <div className="absolute inset-0 flex items-center justify-center z-20 pointer-events-none">
            <video
              controls={false}
              disablePictureInPicture
              disableRemotePlayback
              ref={videoRef}
              src="/1000197130-background (1).mp4"
              muted
              loop
              playsInline
              autoPlay
              className="hidden"
            />
            <canvas
              ref={canvasRef}
              className="object-contain"
              style={{
                width: '125vw',
                height: '125vw',
                transform: 'translateY(0px)',
              }}
            />
          </div>
        </div>

        {/* ============ BOTTOM SECTION — Darker Brown + Shining Glow ============ */}
        {/* z-10 => top section ke niche rahega, taaki top image bottom bg ke upar dikhe */}
        <div
          className="relative w-full z-10 min-h-[60vh] flex items-center justify-center leading-normal overflow-hidden"
          style={{ backgroundColor: '#150C07' }}
        >
          {/* Shining overlay — golden glow */}
          <div
            className="absolute inset-0 pointer-events-none svip-shine"
            aria-hidden="true"
          />

          <span className="relative z-10 text-white font-black text-2xl tracking-widest whitespace-nowrap">
            SVIP COMING SOON
          </span>
        </div>

        {/* ============ MIDDLE IMAGE — Exactly at connection line (40vh) ============ */}
        <img
          src="/file_00000000e5d881faa59ce174257e208e.png"
          alt=""
          className="absolute left-0 w-full block z-40 leading-none align-top pointer-events-none"
          style={{ top: '40vh', transform: 'translateY(-50%)' }}
          draggable="false"
        />
      </div>

      {/* Shining keyframes */}
      <style jsx>{`
        .svip-shine {
          background:
            radial-gradient(
              120% 60% at 50% 0%,
              rgba(255, 215, 120, 0.18) 0%,
              rgba(255, 215, 120, 0.06) 35%,
              transparent 70%
            ),
            linear-gradient(
              180deg,
              rgba(255, 200, 90, 0.08) 0%,
              transparent 40%
            );
          animation: svipShine 4s ease-in-out infinite;
        }

        @keyframes svipShine {
          0%,
          100% {
            opacity: 0.7;
          }
          50% {
            opacity: 1;
          }
        }
      `}</style>
    </div>
  )
}
