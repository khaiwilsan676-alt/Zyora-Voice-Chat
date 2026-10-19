'use client'

import { apiUrl } from "../src/lib/api";
import { generateStableId } from "../lib/hash";

import React, { useEffect, useState, useRef } from 'react'
import { 
  Copy, 
  AlertTriangle,
  AtSign,
  Heart,
  MessageCircle,
  Mic
} from 'lucide-react'
import WhiteColorRemovalShader from './WhiteColorRemovalShader'
import UserReportScreen from './userreport'

// ============ Green Color Removal Shader Component ============
const GreenColorRemovalShader = ({ 
  imageSrc, 
  className = "",
  style = {}
}: { 
  imageSrc: string
  className?: string
  style?: React.CSSProperties
}) => {
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const [isLoaded, setIsLoaded] = useState(false)

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return

    const gl = canvas.getContext('webgl', { premultipliedAlpha: true })
    if (!gl) {
      console.warn('WebGL not supported')
      return
    }

    const vertexShaderSource = `
      attribute vec2 a_position;
      attribute vec2 a_texCoord;
      varying vec2 v_texCoord;

      void main() {
        gl_Position = vec4(a_position, 0.0, 1.0);
        v_texCoord = a_texCoord;
      }
    `

    const fragmentShaderSource = `
      precision mediump float;

      varying vec2 v_texCoord;
      uniform sampler2D u_texture;

      void main() {
        vec4 color = texture2D(u_texture, v_texCoord);

        // Detect green background (high green, low red and blue)
        if (color.g > 0.25 && color.g > color.r * 1.3 && color.g > color.b * 1.3) {
          gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0); // Make transparent
        } else {
          gl_FragColor = color;
        }
      }
    `

    const compileShader = (type: number, source: string) => {
      const shader = gl.createShader(type)
      if (!shader) return null
      gl.shaderSource(shader, source)
      gl.compileShader(shader)

      if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {
        console.error('Shader compile error:', gl.getShaderInfoLog(shader))
        gl.deleteShader(shader)
        return null
      }
      return shader
    }

    const vertexShader = compileShader(gl.VERTEX_SHADER, vertexShaderSource)
    const fragmentShader = compileShader(gl.FRAGMENT_SHADER, fragmentShaderSource)

    if (!vertexShader || !fragmentShader) return

    const program = gl.createProgram()
    if (!program) return

    gl.attachShader(program, vertexShader)
    gl.attachShader(program, fragmentShader)
    gl.linkProgram(program)

    if (!gl.getProgramParameter(program, gl.LINK_STATUS)) {
      console.error('Program link error:', gl.getProgramInfoLog(program))
      return
    }

    gl.useProgram(program)

    const positionBuffer = gl.createBuffer()
    gl.bindBuffer(gl.ARRAY_BUFFER, positionBuffer)
    const positions = new Float32Array([
      -1.0, -1.0,
       1.0, -1.0,
      -1.0,  1.0,
      -1.0,  1.0,
       1.0, -1.0,
       1.0,  1.0,
    ])
    gl.bufferData(gl.ARRAY_BUFFER, positions, gl.STATIC_DRAW)

    const positionLocation = gl.getAttribLocation(program, 'a_position')
    gl.enableVertexAttribArray(positionLocation)
    gl.vertexAttribPointer(positionLocation, 2, gl.FLOAT, false, 0, 0)

    const texCoordBuffer = gl.createBuffer()
    gl.bindBuffer(gl.ARRAY_BUFFER, texCoordBuffer)
    const texCoords = new Float32Array([
      0.0, 1.0,
      1.0, 1.0,
      0.0, 0.0,
      0.0, 0.0,
      1.0, 1.0,
      1.0, 0.0,
    ])
    gl.bufferData(gl.ARRAY_BUFFER, texCoords, gl.STATIC_DRAW)

    const texCoordLocation = gl.getAttribLocation(program, 'a_texCoord')
    gl.enableVertexAttribArray(texCoordLocation)
    gl.vertexAttribPointer(texCoordLocation, 2, gl.FLOAT, false, 0, 0)

    const texture = gl.createTexture()
    gl.bindTexture(gl.TEXTURE_2D, texture)

    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE)
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE)
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR)
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR)

    gl.enable(gl.BLEND)
    gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA)

    const image = new Image()
    image.crossOrigin = 'anonymous'
    image.onload = () => {
      gl.bindTexture(gl.TEXTURE_2D, texture)
      gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, image)

      canvas.width = image.width
      canvas.height = image.height
      gl.viewport(0, 0, canvas.width, canvas.height)

      gl.clearColor(0.0, 0.0, 0.0, 0.0)
      gl.clear(gl.COLOR_BUFFER_BIT)
      gl.drawArrays(gl.TRIANGLES, 0, 6)

      setIsLoaded(true)
    }
    image.onerror = () => {
      console.error('Failed to load image for WebGL processing')
    }
    image.src = imageSrc

    return () => {
      gl.deleteProgram(program)
      gl.deleteShader(vertexShader)
      gl.deleteShader(fragmentShader)
      gl.deleteBuffer(positionBuffer)
      gl.deleteBuffer(texCoordBuffer)
      gl.deleteTexture(texture)
    }
  }, [imageSrc])

  return (
    <canvas
      ref={canvasRef}
      className={className}
      style={{
        ...style,
        opacity: isLoaded ? 1 : 0,
        transition: 'opacity 0.3s ease-in-out'
      }}
    />
  )
}

interface RoomProfileProps {
  user: {
    id?: string
    uid?: string
    accountId?: string
    name: string
    image: string
    gender?: string
    age?: number
    country?: string
    flag?: string
    followers?: number
    bio?: string
    isFollowing?: boolean
    isOnline?: boolean
    isInSeat?: boolean
    isMuted?: boolean
    isLocked?: boolean
  }
  isCurrentUser?: boolean
  isRoomOwner?: boolean
  onClose: () => void
  onCopyId?: () => void
  onLeaveSeat?: () => void
  onMention?: (username?: string) => void
  onFollow?: () => void
  onMessage?: () => void
  onThirdAction?: () => void
  onMute?: () => void
  onLock?: () => void
  onKickOut?: () => void
}

export default function RoomProfile({
  user,
  isCurrentUser = false,
  isRoomOwner = false,
  onClose,
  onCopyId,
  onLeaveSeat,
  onMention,
  onFollow,
  onMessage,
  onThirdAction,
  onMute,
  onLock,
  onKickOut
}: RoomProfileProps) {
  // Default values
  const displayName = user.name || 'User'
  const displayImage = user.image || '/default-avatar.png'
  const displayGender = user.gender || '♂'
  const displayAge = user.age || 24
  const displayCountry = user.country || 'India'
  const displayFlag = user.flag || '🇮🇳'
  const followers = user.followers || 0
  const isOnline = user.isOnline !== undefined ? user.isOnline : true
  const isInSeat = user.isInSeat !== undefined ? user.isInSeat : false
  const isFollowing = user.isFollowing || false
  const isMuted = user.isMuted || false
  const isLocked = user.isLocked || false

  // Tag states
  const [tags, setTags] = useState({
    adminTag: false,
    officialTag: false,
    vipTag: false,
    premiumTag: false,
  })

  // Reports overlay
  const [showReports, setShowReports] = useState(false)

  const hasAnyTag =
    tags.adminTag ||
    tags.officialTag ||
    tags.vipTag ||
    tags.premiumTag

  // Fetch tags from MongoDB
  useEffect(() => {
    const uid = user.uid || user.id || user.accountId

    if (!uid || uid === 'N/A' || uid === 'User') return

    let cancelled = false

    const fetchTags = async () => {
      try {
        const response = await fetch(
          apiUrl(`/api/users?uid=${encodeURIComponent(uid)}`)
        )

        if (!response.ok) {
          throw new Error(`MongoDB user fetch failed: ${response.status}`)
        }

        const result = await response.json()
        const data = result?.user

        if (!cancelled && data) {
          setTags({
            adminTag: data.adminTag || false,
            officialTag: data.officialTag || false,
            vipTag: data.vipTag || false,
            premiumTag: data.premiumTag || false,
          })
        }
      } catch (err) {
        console.warn('Error fetching tags from MongoDB:', err)

        if (!cancelled) {
          setTags({
            adminTag: false,
            officialTag: false,
            vipTag: false,
            premiumTag: false,
          })
        }
      }
    }

    fetchTags()

    return () => {
      cancelled = true
    }
  }, [user.uid, user.id, user.accountId])

  const getUserId = () => {
    const possibleIds = [
      user.accountId,
      user.id,
      user.uid,
    ]

    const id = possibleIds.find(val => val && val.trim() !== '')

    return id ? generateStableId(id) : 'User'
  }

  const accountId = getUserId()

  const handleCopyId = () => {
    if (accountId && accountId !== 'N/A' && accountId !== 'User') {
      navigator.clipboard.writeText(accountId)
      if (onCopyId) onCopyId()
    }
  }

  const handleLeaveSeat = () => {
    if (onLeaveSeat) onLeaveSeat()
  }

  const handleMention = () => {
    if (onMention) onMention(displayName)
  }

  // Determine gender color
  const getGenderColor = (gender: string) => {
    const lowerGender = gender.toLowerCase()
    if (lowerGender === 'male' || lowerGender === 'm' || lowerGender === '♂') {
      return 'bg-blue-500'
    } else if (lowerGender === 'female' || lowerGender === 'f' || lowerGender === '♀') {
      return 'bg-pink-500'
    }
    return 'bg-gray-500'
  }

  const genderColor = getGenderColor(displayGender)

  // Determine what to show
  const showLeaveSeat = isCurrentUser && isInSeat
  const showModerationRow = isRoomOwner && !isCurrentUser
  const showActions = !isCurrentUser // Show actions for all other users

  // Determine sheet height based on content
  const getSheetHeight = () => {
    let baseHeight = 27 // default

    if (showActions && showModerationRow) {
      baseHeight = 40
    }
    else if (showActions && !showModerationRow) {
      baseHeight = 32
    }
    else if (showLeaveSeat) {
      baseHeight = 34
    }

    const finalHeight = hasAnyTag ? baseHeight : baseHeight - 6

    return `${finalHeight}vh`
  }

  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center">
      {/* Transparent Backdrop */}
      <div
        className="absolute inset-0 bg-transparent"
        onClick={onClose}
      />

      {/* Bottom Sheet - Dynamic height based on content */}
      <div 
        className="relative bg-white w-full max-w-md rounded-t-3xl shadow-2xl animate-slide-up"
        style={{
          height: getSheetHeight(),
          minHeight: getSheetHeight(),
          maxHeight: getSheetHeight(),
          overflow: 'visible'
        }}
      >
        {/* Avatar - Positioned above the sheet */}
        <div className="absolute -top-12 left-1/2 -translate-x-1/2 z-30">
          <div className="relative w-22 h-22">
            {/* Base Avatar */}
            <img
              src={displayImage}
              alt={displayName}
              className="w-full h-full object-cover rounded-full shadow-xl bg-gray-100"
              style={{
                position: 'relative',
                zIndex: 1,
              }}
              onError={(e) => {
                (e.target as HTMLImageElement).src = '/default-avatar.png'
              }}
            />

            {/* WebGL Shader Overlay - Size increased */}
            <div
              className="absolute pointer-events-none"
              style={{
                top: '50%',
                left: '50%',
                transform: 'translate(-50%, -50%)',
                width: '150%',
                height: '150%',
                zIndex: 2,
              }}
            >
              <WhiteColorRemovalShader
                imageSrc="/1786867564769.png"
                threshold={0.85}
                className="w-full h-full"
                style={{
                  width: '100%',
                  height: '100%',
                  objectFit: 'contain',
                }}
              />
            </div>
          </div>
        </div>

        {/* Top Left - Warning Icon (only for OTHER users, not own profile) */}
        {!isCurrentUser && (
          <div className="absolute top-2 left-3 z-30">
            <button
              onClick={() => setShowReports(true)}
              className="px-3 py-1.5 rounded-full hover:bg-gray-100 transition-colors"
              aria-label="Warning"
            >
              <AlertTriangle size={22} className="text-gray-700" strokeWidth={2.5} />
            </button>
          </div>
        )}

        {/* Top Right - @ Mention Icon (only for OTHER users, not own profile) */}
        {!isCurrentUser && (
          <div className="absolute top-2 right-3 z-30">
            <button
              onClick={handleMention}
              className="px-3 py-1.5 rounded-full hover:bg-gray-100 transition-colors"
              aria-label="Mention user"
            >
              <AtSign size={22} className="text-gray-700" strokeWidth={2.5} />
            </button>
          </div>
        )}

        {/* Content - No scroll */}
        <div className="flex flex-col items-center px-4 pt-14 pb-3 h-full">
          {/* User Info */}
          <div className="text-center w-full shrink-0">
            {/* Row 1: Name + Gender Tag */}
            <div className="flex items-center justify-center gap-2">
              <h3 className="text-lg font-bold text-gray-900">
                {displayName}
              </h3>
              <span className={`px-2 py-0.5 ${genderColor} text-white text-xs font-medium rounded-full`}>
                {displayGender} {displayAge}
              </span>
            </div>

            {/* Row 2: Tags - ONLY show if assigned from Owner Panel */}
            {hasAnyTag && (
              <div className="flex items-center justify-center gap-0.5 mt-1.5 w-auto flex-wrap">
                {/* Admin Tag with Green Removal */}
                {tags.adminTag && (
                  <GreenColorRemovalShader
                    imageSrc="/1788021461820~2.jpg"
                    className="h-8 w-auto object-contain"
                  />
                )}

                {/* Official Tag with Green Removal */}
                {tags.officialTag && (
                  <GreenColorRemovalShader
                    imageSrc="/1788021468845~2.jpg"
                    className="h-8 w-auto object-contain"
                  />
                )}

                {/* VIP Tag */}
                {tags.vipTag && (
                  <img src="/1785469775751.png" alt="VIP" className="h-6 w-auto object-contain" />
                )}

                {/* Premium Tag */}
                {tags.premiumTag && (
                  <img src="/1785469365805.png" alt="Premium" className="h-6 w-auto object-contain" />
                )}
              </div>
            )}

            {/* Row 3: Level Badge + Additional Image */}
            <div className="flex items-center justify-center gap-0.5 mt-1.5">
              <div className="relative inline-flex items-center">
                <img
                  src="/IMG_20260917_220530.png"
                  alt="Level"
                  className="h-8 w-auto object-contain"
                />
                <span className="absolute inset-0 flex items-center justify-center text-[11px] font-bold text-white drop-shadow-sm pl-4.5 pt-1">
                  Lv.1
                </span>
              </div>
              <img src="/1785486414756.png" alt="" className="h-6 w-auto object-contain" />
            </div>

            {/* Row 4: ID | Fans | Flag */}
            <div className="flex items-center justify-center gap-2 mt-1.5 text-sm">
              {/* ID with Copy */}
              <div className="flex items-center gap-1">
                <span className="text-gray-500">ID:</span>
                <span className="text-gray-700 font-medium">{accountId}</span>
                {accountId !== 'N/A' && accountId !== 'User' && (
                  <button
                    onClick={handleCopyId}
                    className="text-gray-400 hover:text-gray-600 transition-colors p-0.5"
                    aria-label="Copy ID"
                  >
                    <Copy size={14} strokeWidth={2} />
                  </button>
                )}
              </div>

              <span className="text-gray-300">|</span>

              {/* Fans */}
              <div className="flex items-center gap-1">
                <span className="text-gray-700 font-medium">{followers}</span>
                <span className="text-gray-500">Fans</span>
              </div>

              <span className="text-gray-300">|</span>

              {/* Flag only */}
              <div className="flex items-center">
                {displayFlag && (
                  <span className="text-base">{displayFlag}</span>
                )}
              </div>
            </div>
          </div>

          {/* Blue Leave Button - Only for current user in seat */}
          {showLeaveSeat && (
            <div className="mt-3 w-full px-2 shrink-0">
              <button
                onClick={handleLeaveSeat}
                className="w-full h-12 rounded-full bg-blue-500 text-white font-semibold text-base shadow-md shadow-blue-200 hover:bg-blue-600 hover:shadow-lg transition-all active:scale-95 flex items-center justify-center gap-2"
              >
                <div className="relative flex items-center justify-center">
                  <Mic size={20} strokeWidth={2.5} className="text-white" />
                  <span className="absolute -bottom-1 -right-1 text-[11px] font-bold text-white">↓</span>
                </div>
                <span>Leave</span>
              </button>
            </div>
          )}

          {/* Action Buttons for OTHER users */}
          {showActions && (
            <div className="mt-2 w-full flex flex-col gap-2 shrink-0">
              {/* Row: Follow, Chat, Image */}
              <div className="flex items-center gap-9 w-full justify-center">
                <button
                  onClick={onFollow}
                  className="flex items-center gap-1.5 text-pink-500 font-medium text-base hover:text-pink-600 transition-colors active:scale-95"
                >
                  <Heart size={23} className="fill-pink-500" />
                  <span>{isFollowing ? 'Following' : 'Follow'}</span>
                </button>

                <button
                  onClick={onMessage}
                  className="flex items-center gap-1.5 text-gray-700 font-medium text-base hover:text-gray-900 transition-colors active:scale-95"
                >
                  <MessageCircle size={23} />
                  <span>Chat</span>
                </button>

                <button
                  onClick={onThirdAction}
                  className="flex items-center gap-1.5 group active:scale-95 transition-all"
                  aria-label="Additional action"
                >
                  <img
                    src="/file_000000008e508208b1353ae33e2abef9.png"
                    alt="Action"
                    className="w-10 h-10 object-contain rounded-full group-hover:scale-110 transition-transform"
                  />
                </button>
              </div>

              {/* Moderation row - Only for room owner viewing other users */}
              {showModerationRow && (
                <div className="flex items-center justify-around w-full py-1 text-gray-500 text-sm font-medium">
                  <button
                    onClick={onMute}
                    className="hover:text-gray-800 transition-colors py-0.5 px-2"
                  >
                    {isMuted ? 'Unmute' : 'Mute'}
                  </button>
                  <span className="text-gray-300">|</span>
                  <button
                    onClick={onLeaveSeat}
                    className="hover:text-gray-800 transition-colors py-0.5 px-2"
                  >
                    Leave
                  </button>
                  <span className="text-gray-300">|</span>
                  <button
                    onClick={onLock}
                    className="hover:text-gray-800 transition-colors py-0.5 px-2"
                  >
                    {isLocked ? 'Unlock' : 'Lock'}
                  </button>
                  <span className="text-gray-300">|</span>
                  <button
                    onClick={onKickOut}
                    className="hover:text-red-600 transition-colors py-0.5 px-2"
                  >
                    Kick out
                  </button>
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Reports Overlay */}
      {showReports && (
        <UserReportScreen currentUser={{ accountId: isCurrentUser ? accountId : undefined }} targetUser={user} onClose={() => setShowReports(false)} />
      )}

      <style jsx>{`
        @keyframes slideUp {
          from {
            transform: translateY(100%);
            opacity: 0;
          }
          to {
            transform: translateY(0);
            opacity: 1;
          }
        }
        .animate-slide-up {
          animation: slideUp 0.3s cubic-bezier(0.16, 1, 0.3, 1);
        }
      `}</style>
    </div>
  )
      }
