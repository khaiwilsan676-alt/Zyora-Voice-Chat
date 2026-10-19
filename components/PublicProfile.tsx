'use client'

import { apiUrl } from "../src/lib/api";
import { generateStableId } from "../lib/hash";
import { socket } from "../src/lib/socket";

import React, { useEffect, useState, useRef } from 'react'
import {
  ArrowLeft,
  Edit3,
  MapPin,
  Copy,
  Camera,
  ChevronRight,
  X,
  Heart,
  MessageCircle,
  AlertTriangle,
  Check,
  Pin,
} from 'lucide-react'

import ChatScreen from './ChatScreen'
import UserReport from './userreport'

// ============ IndexedDB Functions ============
const PROFILE_DB_NAME = 'ProfileDataDB';
const PROFILE_STORE = 'profileData';

const openProfileDB = (): Promise<IDBDatabase> => {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(PROFILE_DB_NAME, 1);
    request.onerror = () => reject(request.error);
    request.onsuccess = () => resolve(request.result);
    request.onupgradeneeded = () => {
      const db = request.result;
      if (!db.objectStoreNames.contains(PROFILE_STORE)) {
        db.createObjectStore(PROFILE_STORE, { keyPath: 'uid' });
      }
    };
  });
};

const saveProfileToDB = async (profileData: any) => {
  try {
    const db = await openProfileDB();
    const transaction = db.transaction([PROFILE_STORE], 'readwrite');
    const store = transaction.objectStore(PROFILE_STORE);
    const completeData = {
      ...profileData,
      cachedAt: Date.now(),
      lastUpdated: new Date().toISOString(),
    };
    await new Promise<void>((resolve, reject) => {
      const request = store.put(completeData);
      request.onsuccess = () => resolve();
      request.onerror = () => reject(request.error);
    });
    db.close();
  } catch (error) {
    console.error('❌ Profile save error:', error);
  }
};

const loadProfileFromDB = async (uid: string): Promise<any> => {
  try {
    const db = await openProfileDB();
    const transaction = db.transaction([PROFILE_STORE], 'readonly');
    const store = transaction.objectStore(PROFILE_STORE);
    const profileData = await new Promise<any>((resolve, reject) => {
      const request = store.get(uid);
      request.onsuccess = () => resolve(request.result);
      request.onerror = () => reject(request.error);
    });
    db.close();
    return profileData || null;
  } catch (error) {
    console.error('❌ Profile load error:', error);
    return null;
  }
};

export interface TargetUser {
  id?: string
  uid?: string
  name?: string
  displayAccountNumber?: string
  accountId?: string
  photo?: string
  image?: string
  coverPhoto?: string
  gender?: string
  age?: number | string
  followers?: number
  bio?: string
  location?: string
  country?: string
  countryCode?: string
  flag?: string
  officialTag?: boolean
  adminTag?: boolean
  vipTag?: boolean
  premiumTag?: boolean
}

interface PublicProfileProps {
  onBack?: () => void
  onJoinRoom?: (roomId: string) => void
  isOtherUser?: boolean
  targetUser?: TargetUser | null
}

const COUNTRIES = [
  { code: 'IN', name: 'India', flag: '🇮🇳' },
  { code: 'US', name: 'United States', flag: '🇺🇸' },
  { code: 'GB', name: 'United Kingdom', flag: '🇬🇧' },
  { code: 'CA', name: 'Canada', flag: '🇨🇦' },
  { code: 'AU', name: 'Australia', flag: '🇦🇺' },
  { code: 'AE', name: 'UAE', flag: '🇦🇪' },
  { code: 'SA', name: 'Saudi Arabia', flag: '🇸🇦' },
  { code: 'PK', name: 'Pakistan', flag: '🇵🇰' },
  { code: 'BD', name: 'Bangladesh', flag: '🇧🇩' },
  { code: 'NP', name: 'Nepal', flag: '🇳🇵' },
  { code: 'LK', name: 'Sri Lanka', flag: '🇱🇰' },
  { code: 'SG', name: 'Singapore', flag: '🇸🇬' },
  { code: 'MY', name: 'Malaysia', flag: '🇲🇾' },
  { code: 'ID', name: 'Indonesia', flag: '🇮🇩' },
  { code: 'PH', name: 'Philippines', flag: '🇵🇭' },
  { code: 'JP', name: 'Japan', flag: '🇯🇵' },
  { code: 'KR', name: 'South Korea', flag: '🇰🇷' },
  { code: 'CN', name: 'China', flag: '🇨🇳' },
  { code: 'DE', name: 'Germany', flag: '🇩🇪' },
  { code: 'FR', name: 'France', flag: '🇫🇷' },
  { code: 'IT', name: 'Italy', flag: '🇮🇹' },
  { code: 'ES', name: 'Spain', flag: '🇪🇸' },
  { code: 'BR', name: 'Brazil', flag: '🇧🇷' },
  { code: 'MX', name: 'Mexico', flag: '🇲🇽' },
  { code: 'ZA', name: 'South Africa', flag: '🇿🇦' },
  { code: 'NG', name: 'Nigeria', flag: '🇳🇬' },
  { code: 'EG', name: 'Egypt', flag: '🇪🇬' },
  { code: 'RU', name: 'Russia', flag: '🇷🇺' },
  { code: 'TR', name: 'Turkey', flag: '🇹🇷' },
  { code: 'NL', name: 'Netherlands', flag: '🇳🇱' },
]

const SPECIAL_ACCOUNTS: { [key: string]: string } = {
  HUSxSvQnabgU029dWYt1TUV04hd2: '100002',
  ADqW31RGBMaosOzy0HiqexKSD7h1: '100003',
}

const OFFICIAL_IDS = ['500001', '500002', '500003', '500004', '500005']
const ADMIN_IDS = ['700001', '700002', '700003']

const isValidName = (val?: string | null): boolean => {
  if (!val) return false;
  const clean = val.trim().toLowerCase();
  return clean !== '' && clean !== 'guest' && clean !== 'user' && clean !== 'null' && clean !== 'undefined';
}

const getOrCreateAccountNumber = (uid: string) => {
  if (!uid || uid === 'N/A') return '100379620'
  if (OFFICIAL_IDS.includes(uid) || ADMIN_IDS.includes(uid)) return uid
  if (SPECIAL_ACCOUNTS[uid]) return SPECIAL_ACCOUNTS[uid]
  const storageKey = `user_account_number_${uid}`
  let savedAccountNumber = typeof window !== 'undefined' ? localStorage.getItem(storageKey) : null
  if (!savedAccountNumber) {
    let hash = 0
    for (let i = 0; i < uid.length; i++) {
      hash = (hash << 5) - hash + uid.charCodeAt(i)
      hash |= 0
    }
    const positiveHash = Math.abs(hash)
    savedAccountNumber = String(10000000 + (positiveHash % 90000000))
    if (typeof window !== 'undefined') {
      localStorage.setItem(storageKey, savedAccountNumber)
    }
  }
  return savedAccountNumber
}

const compressImage = (file: File, maxWidth: number, maxHeight: number, quality: number): Promise<string> => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.readAsDataURL(file)
    reader.onload = (event) => {
      const img = new Image()
      img.src = event.target?.result as string
      img.onload = () => {
        const canvas = document.createElement('canvas')
        let width = img.width
        let height = img.height
        if (width > height) {
          if (width > maxWidth) {
            height = Math.round((height * maxWidth) / width)
            width = maxWidth
          }
        } else {
          if (height > maxHeight) {
            width = Math.round((width * maxHeight) / height)
            height = maxHeight
          }
        }
        canvas.width = width
        canvas.height = height
        const ctx = canvas.getContext('2d')
        ctx?.drawImage(img, 0, 0, width, height)
        resolve(canvas.toDataURL('image/jpeg', quality))
      }
      img.onerror = (error) => reject(error)
    }
    reader.onerror = (error) => reject(error)
  })
}

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
    if (!gl) return

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
        if (color.g > 0.25 && color.g > color.r * 1.3 && color.g > color.b * 1.3) {
          gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0);
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
    if (!gl.getProgramParameter(program, gl.LINK_STATUS)) return
    gl.useProgram(program)
    const positionBuffer = gl.createBuffer()
    gl.bindBuffer(gl.ARRAY_BUFFER, positionBuffer)
    gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1, -1, 1, -1, -1, 1, -1, 1, 1, -1, 1, 1]), gl.STATIC_DRAW)
    const positionLocation = gl.getAttribLocation(program, 'a_position')
    gl.enableVertexAttribArray(positionLocation)
    gl.vertexAttribPointer(positionLocation, 2, gl.FLOAT, false, 0, 0)
    const texCoordBuffer = gl.createBuffer()
    gl.bindBuffer(gl.ARRAY_BUFFER, texCoordBuffer)
    gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([0, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 0]), gl.STATIC_DRAW)
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
      style={{ ...style, opacity: isLoaded ? 1 : 0, transition: 'opacity 0.3s ease-in-out' }}
    />
  )
}

const WhiteColorRemovalShader = ({
  imageSrc,
  threshold = 0.9,
  className = "",
  style = {}
}: {
  imageSrc: string
  threshold?: number
  className?: string
  style?: React.CSSProperties
}) => {
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const [isLoaded, setIsLoaded] = useState(false)

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return
    const gl = canvas.getContext('webgl', { premultipliedAlpha: true })
    if (!gl) return

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
      uniform float u_threshold;
      void main() {
        vec4 color = texture2D(u_texture, v_texCoord);
        float maxColor = max(color.r, max(color.g, color.b));
        float minColor = min(color.r, min(color.g, color.b));
        float lightness = (maxColor + minColor) / 2.0;
        float saturation = maxColor - minColor;
        if (lightness > u_threshold && saturation < 0.3) {
          gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0);
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
    if (!gl.getProgramParameter(program, gl.LINK_STATUS)) return
    gl.useProgram(program)
    const positionBuffer = gl.createBuffer()
    gl.bindBuffer(gl.ARRAY_BUFFER, positionBuffer)
    gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1, -1, 1, -1, -1, 1, -1, 1, 1, -1, 1, 1]), gl.STATIC_DRAW)
    const positionLocation = gl.getAttribLocation(program, 'a_position')
    gl.enableVertexAttribArray(positionLocation)
    gl.vertexAttribPointer(positionLocation, 2, gl.FLOAT, false, 0, 0)
    const texCoordBuffer = gl.createBuffer()
    gl.bindBuffer(gl.ARRAY_BUFFER, texCoordBuffer)
    gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([0, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 0]), gl.STATIC_DRAW)
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
      const thresholdLocation = gl.getUniformLocation(program, 'u_threshold')
      gl.uniform1f(thresholdLocation, threshold)
      canvas.width = image.width
      canvas.height = image.height
      gl.viewport(0, 0, canvas.width, canvas.height)
      gl.clearColor(0.0, 0.0, 0.0, 0.0)
      gl.clear(gl.COLOR_BUFFER_BIT)
      gl.drawArrays(gl.TRIANGLES, 0, 6)
      setIsLoaded(true)
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
  }, [imageSrc, threshold])

  return (
    <canvas
      ref={canvasRef}
      className={className}
      style={{ ...style, opacity: isLoaded ? 1 : 0, transition: 'opacity 0.3s ease-in-out' }}
    />
  )
}

const GreenColorRemovalVideo = ({ src, className = "" }: { src: string; className?: string }) => {
  const videoRef = useRef<HTMLVideoElement>(null)
  const canvasRef = useRef<HTMLCanvasElement>(null)

  useEffect(() => {
    const video = videoRef.current
    const canvas = canvasRef.current
    if (!video || !canvas) return
    const ctx = canvas.getContext('2d')
    if (!ctx) return

    let frameId = 0
    const draw = () => {
      if (video.readyState >= 2 && video.videoWidth && video.videoHeight) {
        if (canvas.width !== video.videoWidth || canvas.height !== video.videoHeight) {
          canvas.width = video.videoWidth
          canvas.height = video.videoHeight
        }
        ctx.clearRect(0, 0, canvas.width, canvas.height)
        ctx.drawImage(video, 0, 0, canvas.width, canvas.height)
        const image = ctx.getImageData(0, 0, canvas.width, canvas.height)
        const data = image.data
        for (let i = 0; i < data.length; i += 4) {
          const r = data[i], g = data[i + 1], b = data[i + 2]
          if (g > 80 && g > r * 1.25 && g > b * 1.25) data[i + 3] = 0
        }
        ctx.putImageData(image, 0, 0)
      }
      frameId = requestAnimationFrame(draw)
    }
    const drawFirstFrame = () => {
      if (video.readyState >= 2 && video.videoWidth && video.videoHeight) {
        if (canvas.width !== video.videoWidth || canvas.height !== video.videoHeight) {
          canvas.width = video.videoWidth
          canvas.height = video.videoHeight
        }
        ctx.clearRect(0, 0, canvas.width, canvas.height)
        ctx.drawImage(video, 0, 0, canvas.width, canvas.height)
        const image = ctx.getImageData(0, 0, canvas.width, canvas.height)
        const data = image.data
        for (let i = 0; i < data.length; i += 4) {
          const r = data[i], g = data[i + 1], b = data[i + 2]
          if (g > 80 && g > r * 1.25 && g > b * 1.25) data[i + 3] = 0
        }
        ctx.putImageData(image, 0, 0)
      }
    }
    const handleLoadedData = () => {
      video.currentTime = 0
      requestAnimationFrame(() => {
        drawFirstFrame()
        video.pause()
      })
    }
    video.addEventListener('loadeddata', handleLoadedData)
    video.addEventListener('canplay', handleLoadedData)
    video.load()
    if (video.readyState >= 2) handleLoadedData()
    return () => {
      video.removeEventListener('loadeddata', handleLoadedData)
      video.removeEventListener('canplay', handleLoadedData)
    }
  }, [src])

  return (
    <>
      <video ref={videoRef} src={src} muted loop playsInline preload="auto" className="hidden" />
      <canvas ref={canvasRef} className={className} />
    </>
  )
}

// ============ Crop Screen Component (FULL IMAGE + SAME CROP LINES) ============
const CropScreen = ({
  imageSrc,
  onBack,
  onSave,
}: {
  imageSrc: string;
  onBack: () => void;
  onSave: (croppedImage: string) => void;
}) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const imageRef = useRef<HTMLImageElement>(null);
  const [imageSize, setImageSize] = useState({ width: 0, height: 0 });
  const [displaySize, setDisplaySize] = useState({ width: 0, height: 0 });
  const [cropBox, setCropBox] = useState({ x: 50, y: 50, width: 200, height: 200 });

  const [interaction, setInteraction] = useState<{
    type: 'move' | 'resize-tl' | 'resize-tr' | 'resize-bl' | 'resize-br' | 'zoom' | null;
    startX: number;
    startY: number;
    startCrop: { x: number; y: number; width: number; height: number };
    startDist?: number;
    startScale: number;
  } | null>(null);

  const [scale, setScale] = useState(1);

  const handleImageLoad = (e: React.SyntheticEvent<HTMLImageElement>) => {
    const img = e.currentTarget;
    const { naturalWidth, naturalHeight } = img;
    setImageSize({ width: naturalWidth, height: naturalHeight });
    
    const container = containerRef.current;
    if (container) {
      const rect = container.getBoundingClientRect();
      const containerW = rect.width;
      const containerH = rect.height;
      
      const imgAspect = naturalWidth / naturalHeight;
      const containerAspect = containerW / containerH;

      // ✅ FULL IMAGE FIT — poori image dikhegi (contain)
      let displayW, displayH;
      if (imgAspect > containerAspect) {
        displayW = containerW;
        displayH = containerW / imgAspect;
      } else {
        displayH = containerH;
        displayW = containerH * imgAspect;
      }
      
      setDisplaySize({ width: displayW, height: displayH });
      
      // ✅ Crop box default = 70% of image (SAME as pehle)
      const initialSize = Math.min(displayW, displayH) * 0.7;
      setCropBox({
        x: (displayW - initialSize) / 2,
        y: (displayH - initialSize) / 2,
        width: initialSize,
        height: initialSize,
      });
    }
  };

  const handleTouchStart = (e: React.TouchEvent) => {
    if (e.touches.length === 2) {
      e.preventDefault();
      const dist = Math.hypot(
        e.touches[0].clientX - e.touches[1].clientX,
        e.touches[0].clientY - e.touches[1].clientY
      );
      setInteraction({
        type: 'zoom',
        startX: 0, startY: 0,
        startCrop: cropBox,
        startDist: dist,
        startScale: scale,
      });
    } else if (e.touches.length === 1) {
      const touch = e.touches[0];
      const { clientX, clientY } = touch;
      
      const rect = containerRef.current?.getBoundingClientRect();
      if (!rect) return;
      
      const x = clientX - rect.left;
      const y = clientY - rect.top;
      
      const handleSize = 30;
      const { x: cx, y: cy, width: cw, height: ch } = cropBox;
      
      const isTL = Math.abs(x - cx) < handleSize && Math.abs(y - cy) < handleSize;
      const isTR = Math.abs(x - (cx + cw)) < handleSize && Math.abs(y - cy) < handleSize;
      const isBL = Math.abs(x - cx) < handleSize && Math.abs(y - (cy + ch)) < handleSize;
      const isBR = Math.abs(x - (cx + cw)) < handleSize && Math.abs(y - (cy + ch)) < handleSize;
      const isInside = x >= cx && x <= cx + cw && y >= cy && y <= cy + ch;

      if (isTL) setInteraction({ type: 'resize-tl', startX: clientX, startY: clientY, startCrop: cropBox, startScale: scale });
      else if (isTR) setInteraction({ type: 'resize-tr', startX: clientX, startY: clientY, startCrop: cropBox, startScale: scale });
      else if (isBL) setInteraction({ type: 'resize-bl', startX: clientX, startY: clientY, startCrop: cropBox, startScale: scale });
      else if (isBR) setInteraction({ type: 'resize-br', startX: clientX, startY: clientY, startCrop: cropBox, startScale: scale });
      else if (isInside) setInteraction({ type: 'move', startX: clientX, startY: clientY, startCrop: cropBox, startScale: scale });
    }
  };

  const handleTouchMove = (e: React.TouchEvent) => {
    if (!interaction) return;
    e.preventDefault();

    if (interaction.type === 'zoom' && e.touches.length === 2) {
      const dist = Math.hypot(
        e.touches[0].clientX - e.touches[1].clientX,
        e.touches[0].clientY - e.touches[1].clientY
      );
      const newScale = Math.min(Math.max(interaction.startScale * (dist / interaction.startDist!), 1), 4);
      setScale(newScale);
      return;
    }

    if (e.touches.length === 1) {
      const { clientX, clientY } = e.touches[0];
      const deltaX = clientX - interaction.startX;
      const deltaY = clientY - interaction.startY;

      if (interaction.type === 'move') {
        let newX = interaction.startCrop.x + deltaX;
        let newY = interaction.startCrop.y + deltaY;
        
        newX = Math.max(0, Math.min(newX, displaySize.width - cropBox.width));
        newY = Math.max(0, Math.min(newY, displaySize.height - cropBox.height));
        
        setCropBox(prev => ({ ...prev, x: newX, y: newY }));
      } 
      else if (interaction.type === 'resize-tl') {
        let newW = interaction.startCrop.width - deltaX;
        let newH = interaction.startCrop.height - deltaY;
        let newX = interaction.startCrop.x + deltaX;
        let newY = interaction.startCrop.y + deltaY;
        
        if (newX < 0) { newW += newX; newX = 0; }
        if (newY < 0) { newH += newY; newY = 0; }
        
        newW = Math.max(50, newW);
        newH = Math.max(50, newH);
        
        setCropBox({ x: newX, y: newY, width: newW, height: newH });
      }
      else if (interaction.type === 'resize-tr') {
        let newW = interaction.startCrop.width + deltaX;
        let newH = interaction.startCrop.height - deltaY;
        let newY = interaction.startCrop.y + deltaY;
        
        if (newY < 0) { newH += newY; newY = 0; }
        
        newW = Math.max(50, newW);
        newH = Math.max(50, newH);
        
        setCropBox({ x: interaction.startCrop.x, y: newY, width: newW, height: newH });
      }
      else if (interaction.type === 'resize-bl') {
        let newW = interaction.startCrop.width - deltaX;
        let newH = interaction.startCrop.height + deltaY;
        let newX = interaction.startCrop.x + deltaX;
        
        if (newX < 0) { newW += newX; newX = 0; }
        
        newW = Math.max(50, newW);
        newH = Math.max(50, newH);
        
        setCropBox({ x: newX, y: interaction.startCrop.y, width: newW, height: newH });
      }
      else if (interaction.type === 'resize-br') {
        let newW = interaction.startCrop.width + deltaX;
        let newH = interaction.startCrop.height + deltaY;
        
        newW = Math.max(50, newW);
        newH = Math.max(50, newH);
        
        setCropBox({ x: interaction.startCrop.x, y: interaction.startCrop.y, width: newW, height: newH });
      }
    }
  };

  const handleTouchEnd = () => {
    setInteraction(null);
  };

  const handleSave = () => {
    if (!imageRef.current || !containerRef.current) return;
    
    const img = new Image();
    img.src = imageSrc;
    img.onload = () => {
      const canvas = document.createElement('canvas');
      const ctx = canvas.getContext('2d');
      if (!ctx) return;

      const naturalW = img.naturalWidth;
      const naturalH = img.naturalHeight;
      
      const baseW = displaySize.width;
      const baseH = displaySize.height;
      
      const imgLeftBase = (containerRef.current!.clientWidth - baseW) / 2;
      const imgTopBase = (containerRef.current!.clientHeight - baseH) / 2;
      
      const imgCenterScreenX = imgLeftBase + baseW / 2;
      const imgCenterScreenY = imgTopBase + baseH / 2;
      
      const scaledW = baseW * scale;
      const scaledH = baseH * scale;
      
      const imgLeftScaled = imgCenterScreenX - scaledW / 2;
      const imgTopScaled = imgCenterScreenY - scaledH / 2;
      
      const cropScreenX = cropBox.x;
      const cropScreenY = cropBox.y;
      const cropW = cropBox.width;
      const cropH = cropBox.height;
      
      const pxToNaturalX = naturalW / scaledW;
      const pxToNaturalY = naturalH / scaledH;
      
      const sourceX = (cropScreenX - imgLeftScaled) * pxToNaturalX;
      const sourceY = (cropScreenY - imgTopScaled) * pxToNaturalY;
      const sourceW = cropW * pxToNaturalX;
      const sourceH = cropH * pxToNaturalY;
      
      const finalX = Math.max(0, Math.min(sourceX, naturalW - sourceW));
      const finalY = Math.max(0, Math.min(sourceY, naturalH - sourceH));
      const finalW = Math.min(sourceW, naturalW);
      const finalH = Math.min(sourceH, naturalH);

      canvas.width = finalW;
      canvas.height = finalH;
      
      ctx.drawImage(
        img,
        finalX, finalY, finalW, finalH,
        0, 0, finalW, finalH
      );

      const croppedDataUrl = canvas.toDataURL('image/jpeg', 0.9);
      onSave(croppedDataUrl);
    };
  };

  return (
    <div className="fixed inset-0 z-[100] bg-black flex flex-col select-none">
      {/* Top Header */}
      <div className="flex items-center justify-between px-4 py-4 pt-[calc(env(safe-area-inset-top,0px)+16px)] bg-black z-20">
        <button onClick={onBack} className="text-white p-1">
          <ArrowLeft size={28} />
        </button>
        <h2 className="text-white text-lg font-semibold">Crop Image</h2>
        <button onClick={handleSave} className="text-green-400 p-1">
          <Check size={28} />
        </button>
      </div>

      {/* Main Image Area — FULL IMAGE VISIBLE + SAME CROP LINES */}
      <div 
        ref={containerRef}
        className="flex-1 relative overflow-hidden flex items-center justify-center bg-black"
        onTouchStart={handleTouchStart}
        onTouchMove={handleTouchMove}
        onTouchEnd={handleTouchEnd}
      >
        {/* ✅ Image: full fit — poori image dikhti hai */}
        <div 
          className="relative flex items-center justify-center"
          style={{
            transform: `scale(${scale})`,
            transition: interaction?.type === 'zoom' ? 'none' : 'transform 0.2s ease-out',
            transformOrigin: 'center center',
          }}
        >
          <img
            ref={imageRef}
            src={imageSrc}
            alt="Crop"
            onLoad={handleImageLoad}
            className="max-w-full max-h-full object-contain select-none pointer-events-none"
            style={{ display: 'block' }}
          />
        </div>

        {/* Dark Overlay with Mask — same as before */}
        <div className="absolute inset-0 pointer-events-none" style={{
          background: 'rgba(0,0,0,0.6)',
          clipPath: `polygon(
            0% 0%, 
            100% 0%, 
            100% 100%, 
            0% 100%, 
            0% 0%,
            ${cropBox.x}px ${cropBox.y}px,
            ${cropBox.x}px ${cropBox.y + cropBox.height}px,
            ${cropBox.x + cropBox.width}px ${cropBox.y + cropBox.height}px,
            ${cropBox.x + cropBox.width}px ${cropBox.y}px,
            ${cropBox.x}px ${cropBox.y}px
          )`
        }} />

        {/* Crop Box UI — SAME lines (border + grid + corner handles) */}
        <div 
          className="absolute border-2 border-white pointer-events-none"
          style={{
            left: cropBox.x,
            top: cropBox.y,
            width: cropBox.width,
            height: cropBox.height,
          }}
        >
          {/* Grid Lines */}
          <div className="absolute inset-0 pointer-events-none">
            <div className="absolute top-1/3 left-0 right-0 h-[1px] bg-white/40"></div>
            <div className="absolute top-2/3 left-0 right-0 h-[1px] bg-white/40"></div>
            <div className="absolute left-1/3 top-0 bottom-0 w-[1px] bg-white/40"></div>
            <div className="absolute left-2/3 top-0 bottom-0 w-[1px] bg-white/40"></div>
          </div>

          {/* Corner Handles */}
          <div className="absolute -top-2.5 -left-2.5 w-5 h-5 border-t-2 border-l-2 border-white pointer-events-none"></div>
          <div className="absolute -top-2.5 -right-2.5 w-5 h-5 border-t-2 border-r-2 border-white pointer-events-none"></div>
          <div className="absolute -bottom-2.5 -left-2.5 w-5 h-5 border-b-2 border-l-2 border-white pointer-events-none"></div>
          <div className="absolute -bottom-2.5 -right-2.5 w-5 h-5 border-b-2 border-r-2 border-white pointer-events-none"></div>
        </div>
      </div>
    </div>
  );
};


export default function PublicProfile({
  onBack,
  onJoinRoom,
  isOtherUser = false,
  targetUser = null,
}: PublicProfileProps) {
  const avatarInputRef = useRef<HTMLInputElement>(null)
  const albumInputRef = useRef<HTMLInputElement>(null)
  const coverInputRef = useRef<HTMLInputElement>(null)
  
  const longPressTimer = useRef<NodeJS.Timeout | null>(null)

  const [user, setUser] = useState(() => {
    if (isOtherUser && targetUser) {
      const targetUid = targetUser.uid || targetUser.id || 'N/A'
      const searchKey = targetUser.accountId || targetUser.displayAccountNumber || targetUid
      const displayAccNum = searchKey !== 'N/A' ? generateStableId(searchKey) : ''
      const initialName = isValidName(targetUser.name) ? targetUser.name! : (displayAccNum || 'User')
      const photo = targetUser.photo || targetUser.image || '/default-avatar.png'
      return {
        name: initialName,
        uid: targetUid,
        displayAccountNumber: displayAccNum,
        photo: photo,
        coverPhoto: targetUser.coverPhoto || '',
        gender: (targetUser.gender === 'female' || targetUser.gender === '♀' ? '♀' : '♂') as '♂' | '♀',
        age: targetUser.age ? (typeof targetUser.age === 'number' ? targetUser.age : parseInt(String(targetUser.age))) : 22,
        followers: targetUser.followers || 0,
        bio: targetUser.bio || '',
        location: targetUser.location || targetUser.country || 'India',
        flag: targetUser.flag || '🇮🇳',
        countryCode: targetUser.countryCode || 'IN',
        officialTag: Boolean(targetUser.officialTag),
        adminTag: Boolean(targetUser.adminTag),
        vipTag: Boolean(targetUser.vipTag),
        premiumTag: Boolean(targetUser.premiumTag),
      }
    }

    if (typeof window === 'undefined') {
      return {
        name: '', uid: '', displayAccountNumber: '100379620',
        photo: '', coverPhoto: '', gender: '♂', age: 24,
        followers: 0, bio: '', location: 'India', flag: '🇮🇳',
        countryCode: 'IN', officialTag: false, adminTag: false,
        vipTag: false, premiumTag: false,
      }
    }

    const uid = localStorage.getItem('userUID') || localStorage.getItem('userPhone') || localStorage.getItem('userId') || ''
    const localName = localStorage.getItem('userName')
    const validName = isValidName(localName) ? localName! : ''
    const photo = localStorage.getItem('userPhoto') || ''
    const coverPhoto = localStorage.getItem('userCoverPhoto') || ''
    const bio = localStorage.getItem('userBio') || ''
    const country = localStorage.getItem('userCountry') || 'India'
    const countryCode = localStorage.getItem('userCountryCode') || 'IN'
    const age = localStorage.getItem('userAge') ? parseInt(localStorage.getItem('userAge')!) : 24
    const gender = localStorage.getItem('userGender') || '♂'
    const displayAccNum = localStorage.getItem('accountNumber') || (uid ? getOrCreateAccountNumber(uid) : '100379620')

    return {
      name: validName, uid: uid, displayAccountNumber: displayAccNum,
      photo, coverPhoto, gender: gender === 'female' || gender === '♀' ? '♀' : '♂',
      age, followers: 0, bio, location: country, flag: '🇮🇳',
      countryCode, officialTag: false, adminTag: false, vipTag: false, premiumTag: false,
    }
  })

  const [albumImages, setAlbumImages] = useState<string[]>(() => {
    if (typeof window === 'undefined') return []
    const storedAlbum = localStorage.getItem('userAlbumImages')
    return storedAlbum ? JSON.parse(storedAlbum) : []
  })

  const [cropImageSrc, setCropImageSrc] = useState<string | null>(null)

  const [ownedFrameItems, setOwnedFrameItems] = useState<Array<{ id: string; image: string; name: string }>>([])
  const [receivedGifts, setReceivedGifts] = useState<Array<{ id: string; name: string; image: string }>>([])

  useEffect(() => {
    if (typeof window === 'undefined') return

    const FRAME_ITEMS: Record<string, { id: string; image: string; name: string }> = {
      a1: { id: 'a1', image: '/VID_20260905_024534_955_bsl.mp4', name: 'Crown Wings' },
      a2: { id: 'a2', image: '/VID_20260905_024726_660_bsl.mp4', name: 'Host Wings' },
      a3: { id: 'a3', image: '/VID_20260905_083446_619_bsl.mp4', name: 'Mystic Wings' },
    }

    const loadOwnedFrames = async () => {
      try {
        const db = await new Promise<IDBDatabase>((resolve, reject) => {
          const request = indexedDB.open('FruitPartyDB', 3)
          request.onsuccess = () => resolve(request.result)
          request.onerror = () => reject(request.error)
        })
        const tx = db.transaction('GameState', 'readonly')
        const req = tx.objectStore('GameState').get('user_data')
        req.onsuccess = () => {
          const owned = Array.isArray(req.result?.ownedItems) ? req.result.ownedItems.map(String) : []
          setOwnedFrameItems(owned.map((id: string) => FRAME_ITEMS[id]).filter(Boolean))
          db.close()
        }
        req.onerror = () => db.close()
      } catch {
        setOwnedFrameItems([])
      }
    }

    const loadReceivedGifts = () => {
      const uid = (isOtherUser ? (targetUser?.accountId || targetUser?.uid || targetUser?.id || '') : '') ||
        localStorage.getItem('userUID') || localStorage.getItem('userPhone') || localStorage.getItem('userId') || localStorage.getItem('accountNumber') || ''
      if (!uid) return
      try {
        const stored = localStorage.getItem('receivedGifts_' + uid)
        const parsed = stored ? JSON.parse(stored) : []
        if (Array.isArray(parsed)) setReceivedGifts(parsed.slice(0, 50))
      } catch {}
    }

    loadOwnedFrames()
    loadReceivedGifts()

    const refresh = () => {
      loadOwnedFrames()
      loadReceivedGifts()
    }
    window.addEventListener('hurry:received-gifts-updated', refresh)
    window.addEventListener('hurry-store-sync', refresh)
    return () => {
      window.removeEventListener('hurry:received-gifts-updated', refresh)
      window.removeEventListener('hurry-store-sync', refresh)
    }
  }, [])

  const [coverPhotos, setCoverPhotos] = useState<string[]>(() => {
    if (typeof window === 'undefined') return []
    const stored = localStorage.getItem('userCoverPhotos')
    if (stored) {
      try {
        const parsed = JSON.parse(stored)
        if (Array.isArray(parsed)) return parsed
      } catch {}
    }
    const single = localStorage.getItem('userCoverPhoto')
    return single ? [single] : []
  })

  const [currentCoverIndex, setCurrentCoverIndex] = useState(0)

  const [showEditSheet, setShowEditSheet] = useState(false)
  const [showNameScreen, setShowNameScreen] = useState(false)
  const [showAgeScreen, setShowAgeScreen] = useState(false)
  const [showBioScreen, setShowBioScreen] = useState(false)

  const [editName, setEditName] = useState(user.name)
  const [editAge, setEditAge] = useState(user.age.toString())
  const [editBio, setEditBio] = useState(user.bio)

  const [editGender, setEditGender] = useState('')
  const [genderLocked, setGenderLocked] = useState(false)

  const [editCountry, setEditCountry] = useState(user.location)
  const [editCountryCode, setEditCountryCode] = useState(user.countryCode)
  const [countryLocked, setCountryLocked] = useState(false)

  const [activeTab, setActiveTab] = useState('profile')
  
  const [fullImageIndex, setFullImageIndex] = useState<number | null>(null)

  const touchStartXRef = useRef<number>(0)
  const touchEndXRef = useRef<number>(0)
  const imageContainerRef = useRef<HTMLDivElement>(null)

  const [scale, setScale] = useState(1)
  const [position, setPosition] = useState({ x: 0, y: 0 })
  const lastTouchDistRef = useRef<number>(0)
  const lastTapRef = useRef<number>(0)
  const isPinchingRef = useRef<boolean>(false)
  const dragStartRef = useRef({ x: 0, y: 0 })
  const isDraggingRef = useRef<boolean>(false)

  const [isFollowing, setIsFollowing] = useState(false)

  const [showActionSheet, setShowActionSheet] = useState(false)
  const [showReportToast, setShowReportToast] = useState(false)

  const [showChat, setShowChat] = useState(false)
  const [showUserReport, setShowUserReport] = useState(false)

  const isSpecialAccount = SPECIAL_ACCOUNTS.hasOwnProperty(user.uid || '')

  const [birthYear, setBirthYear] = useState(2003)
  const [birthMonth, setBirthMonth] = useState(4)
  const [birthDay, setBirthDay] = useState(8)
  const years = Array.from({ length: 2026 - 1950 + 1 }, (_, i) => 1950 + i)
  const months = Array.from({ length: 12 }, (_, i) => i + 1)
  const days = Array.from({ length: 31 }, (_, i) => i + 1)

  const yearRef = useRef<HTMLDivElement>(null)
  const monthRef = useRef<HTMLDivElement>(null)
  const dayRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (showAgeScreen) {
      setTimeout(() => {
        if (dayRef.current) {
          dayRef.current.scrollTo({ top: days.indexOf(birthDay) * 50, behavior: 'smooth' });
        }
        if (monthRef.current) {
          monthRef.current.scrollTo({ top: months.indexOf(birthMonth) * 50, behavior: 'smooth' });
        }
        if (yearRef.current) {
          yearRef.current.scrollTo({ top: years.indexOf(birthYear) * 50, behavior: 'smooth' });
        }
      }, 200);
    }
  }, [showAgeScreen]);

  useEffect(() => {
    if (coverPhotos.length <= 1) {
      setCurrentCoverIndex(0)
      return
    }
    const interval = setInterval(() => {
      setCurrentCoverIndex((prev) => (prev + 1) % coverPhotos.length)
    }, 5000)
    return () => clearInterval(interval)
  }, [coverPhotos.length])

  useEffect(() => {
    if (user.coverPhoto && coverPhotos.length === 0) {
      setCoverPhotos([user.coverPhoto])
    }
  }, [user.coverPhoto])

  useEffect(() => {
    setScale(1)
    setPosition({ x: 0, y: 0 })
  }, [fullImageIndex])

  const saveToMongoDB = async (updateData: Record<string, any>) => {
    const currentUid = user.uid || localStorage.getItem('userUID') || localStorage.getItem('userPhone')
    if (!currentUid || currentUid === 'N/A') return

    try {
      const response = await fetch(apiUrl('/api/users'), {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ uid: currentUid, ...updateData }),
      })

      if (!response.ok) throw new Error(`MongoDB user update failed: ${response.status}`)

      const roomUpdateData = { ...updateData }
      delete roomUpdateData.name
      delete roomUpdateData.displayName
      delete roomUpdateData.userName
      delete roomUpdateData.image
      delete roomUpdateData.photo
      delete roomUpdateData.photoURL
      delete roomUpdateData.coverPhoto
      delete roomUpdateData.coverImage

      if (Object.keys(roomUpdateData).length > 0) {
        const roomResponse = await fetch(apiUrl('/api/rooms'), {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ roomId: currentUid, ...roomUpdateData }),
        })
        if (!roomResponse.ok) throw new Error(`MongoDB room update failed: ${roomResponse.status}`)
      }
    } catch (err) {
      console.error('Error saving data to MongoDB:', err)
    }
  }

  const saveCurrentUserToDB = async () => {
    if (isOtherUser) return;
    const uid = user.uid || localStorage.getItem('userUID') || localStorage.getItem('userPhone');
    if (uid && uid !== 'N/A') {
      const profileData = {
        uid: uid,
        name: user.name,
        displayAccountNumber: user.displayAccountNumber,
        photo: user.photo,
        coverPhoto: user.coverPhoto,
        gender: user.gender,
        age: user.age,
        followers: user.followers,
        bio: user.bio,
        location: user.location,
        flag: user.flag,
        countryCode: user.countryCode,
        albumImages: albumImages,
        coverPhotos: coverPhotos,
        officialTag: user.officialTag,
        adminTag: user.adminTag,
        vipTag: user.vipTag,
        premiumTag: user.premiumTag,
      };
      await saveProfileToDB(profileData);
    }
  };

  useEffect(() => {
    let unsubscribe: (() => void) | undefined

    const loadProfileData = async () => {
      if (isOtherUser && targetUser) {
        const targetUid = targetUser.uid || targetUser.id || 'N/A'
        const searchKey = targetUser.accountId || targetUser.displayAccountNumber || targetUid
        let displayAccNum = searchKey !== 'N/A' ? generateStableId(searchKey) : ''
        let initialName = isValidName(targetUser.name) ? targetUser.name! : (displayAccNum || 'User')
        let photo = targetUser.photo || targetUser.image || '/default-avatar.png'
        let coverPhoto = targetUser.coverPhoto || ''
        let bio = targetUser.bio || ''
        let country = targetUser.country || targetUser.location || 'India'
        let countryCode = targetUser.countryCode || 'IN'
        let gender = targetUser.gender || '♀'
        let age = targetUser.age ? (typeof targetUser.age === 'number' ? targetUser.age : parseInt(String(targetUser.age))) : 22
        let followers = targetUser.followers || 0
        let album: string[] = []
        let officialTag = Boolean(targetUser.officialTag)
        let adminTag = Boolean(targetUser.adminTag)
        let vipTag = Boolean(targetUser.vipTag)
        let premiumTag = Boolean(targetUser.premiumTag)

        setUser({
          uid: targetUid, name: initialName, displayAccountNumber: displayAccNum,
          photo, coverPhoto,
          gender: (gender === 'female' || gender === '♀' ? '♀' : '♂') as '♂' | '♀',
          age, followers, bio, location: country, flag: '🇮🇳', countryCode,
          officialTag, adminTag, vipTag, premiumTag,
        })

        if (targetUid && targetUid !== 'N/A') {
          const cachedProfile = await loadProfileFromDB(targetUid);
          if (cachedProfile && isValidName(cachedProfile.name)) {
            setUser({
              name: cachedProfile.name, uid: targetUid,
              displayAccountNumber: cachedProfile.displayAccountNumber || displayAccNum,
              photo: cachedProfile.photo || photo,
              coverPhoto: cachedProfile.coverPhoto || coverPhoto,
              gender: cachedProfile.gender || gender,
              age: cachedProfile.age || age,
              followers: cachedProfile.followers || followers,
              bio: cachedProfile.bio || bio,
              location: cachedProfile.location || country,
              flag: cachedProfile.flag || '🇮🇳',
              countryCode: cachedProfile.countryCode || countryCode,
              officialTag: cachedProfile.officialTag || officialTag,
              adminTag: cachedProfile.adminTag || adminTag,
              vipTag: cachedProfile.vipTag || vipTag,
              premiumTag: cachedProfile.premiumTag || premiumTag,
            });
            setAlbumImages(cachedProfile.albumImages || []);
            if (Array.isArray(cachedProfile.coverPhotos) && cachedProfile.coverPhotos.length > 0) {
              setCoverPhotos(cachedProfile.coverPhotos);
            } else if (cachedProfile.coverPhoto) {
              setCoverPhotos([cachedProfile.coverPhoto]);
            }
            return;
          }

          try {
            const mongoResponse = await fetch(
              apiUrl(`/api/users?search=${encodeURIComponent(searchKey)}&accountId=${encodeURIComponent(searchKey)}&uid=${encodeURIComponent(targetUid)}`)
            );
            if (mongoResponse.ok) {
              const res = await mongoResponse.json();
              const data = res && (res.user || (Array.isArray(res.users) ? res.users[0] : null) || res.data);
              if (data && (data.id || data.uid || data.accountId || data.name)) {
                displayAccNum = data.accountId || data.accountNumber || data['Account Number'] || displayAccNum;
                const docName = data.name || data.Name || data.displayName || data.userName || data.fullName;
                const finalName = isValidName(docName) ? docName : initialName;
                photo = data.photo || data.photoURL || data.image || data.avatar || photo;
                coverPhoto = data.coverPhoto || data.coverImage || data.backCover || coverPhoto;
                bio = data.bio || data.Bio || data.about || bio;
                country = data.country || data.Country || data.location || country;
                countryCode = data.countryCode || countryCode;
                gender = data.gender || data.Gender || gender;
                age = data.age || data.Age ? parseInt(String(data.age || data.Age)) : age;
                followers = data.followers !== undefined ? data.followers : followers;
                officialTag = Boolean(data.officialTag ?? officialTag);
                adminTag = Boolean(data.adminTag ?? adminTag);
                vipTag = Boolean(data.vipTag ?? vipTag);
                premiumTag = Boolean(data.premiumTag ?? premiumTag);

                let coverArr: string[] = [];
                if (Array.isArray(data.coverPhotos) && data.coverPhotos.length > 0) {
                  coverArr = data.coverPhotos;
                } else if (coverPhoto) {
                  coverArr = [coverPhoto];
                }
                if (data.albumImages && Array.isArray(data.albumImages)) album = data.albumImages;
                else if (data.album && Array.isArray(data.album)) album = data.album;

                const matchedCountry = COUNTRIES.find(
                  (c) => c.code === countryCode || c.name === country || c.flag === country
                ) || { name: 'India', flag: '🇮🇳', code: 'IN' };

                const profileData = {
                  uid: targetUid, name: finalName,
                  displayAccountNumber: String(displayAccNum),
                  photo, coverPhoto,
                  gender: (gender === 'female' || gender === '♀' ? '♀' : '♂') as '♂' | '♀',
                  age, followers, bio,
                  location: matchedCountry.name,
                  flag: matchedCountry.flag,
                  countryCode: matchedCountry.code,
                  albumImages: album, coverPhotos: coverArr,
                  officialTag, adminTag, vipTag, premiumTag,
                };
                setUser(profileData);
                setAlbumImages(album);
                if (coverArr.length > 0) setCoverPhotos(coverArr);
                await saveProfileToDB(profileData);
              }
            }
          } catch (err) {
            console.warn('MongoDB fetch error for Target User:', err);
          }
        }
        return
      }

      const uid = localStorage.getItem('userUID') || localStorage.getItem('userPhone') || localStorage.getItem('userId') || 'N/A'

      if (uid !== 'N/A') {
        const cachedProfile = await loadProfileFromDB(uid);
        if (cachedProfile && isValidName(cachedProfile.name)) {
          setUser({
            name: cachedProfile.name, uid: uid,
            displayAccountNumber: cachedProfile.displayAccountNumber || '',
            photo: cachedProfile.photo || '',
            coverPhoto: cachedProfile.coverPhoto || '',
            gender: cachedProfile.gender || '♂',
            age: cachedProfile.age || 24,
            followers: cachedProfile.followers || 0,
            bio: cachedProfile.bio || '',
            location: cachedProfile.location || 'India',
            flag: cachedProfile.flag || '🇮🇳',
            countryCode: cachedProfile.countryCode || 'IN',
            officialTag: cachedProfile.officialTag || false,
            adminTag: cachedProfile.adminTag || false,
            vipTag: cachedProfile.vipTag || false,
            premiumTag: cachedProfile.premiumTag || false,
          });
          setAlbumImages(cachedProfile.albumImages || []);
          if (Array.isArray(cachedProfile.coverPhotos) && cachedProfile.coverPhotos.length > 0) {
            setCoverPhotos(cachedProfile.coverPhotos);
          } else if (cachedProfile.coverPhoto) {
            setCoverPhotos([cachedProfile.coverPhoto]);
          }
          setEditName(cachedProfile.name);
          setEditAge(String(cachedProfile.age || '24'));
          setEditBio(cachedProfile.bio || '');
          setEditCountry(cachedProfile.location || 'India');
          setEditCountryCode(cachedProfile.countryCode || 'IN');
          return;
        }
      }

      let storedName = localStorage.getItem('userName') || ''
      if (!isValidName(storedName)) storedName = ''
      let photo = localStorage.getItem('userPhoto') || ''
      let coverPhoto = localStorage.getItem('userCoverPhoto') || ''
      let storedBio = localStorage.getItem('userBio') || ''
      let storedCountry = localStorage.getItem('userCountry') || 'India'
      let storedCountryCode = localStorage.getItem('userCountryCode') || 'IN'
      let storedAge = localStorage.getItem('userAge') || '24'
      let storedGender = localStorage.getItem('userGender') || localStorage.getItem('userGenderLocked') || ''
      let isCountryLockedInStorage = localStorage.getItem('userCountryLocked') === 'true' || localStorage.getItem('setupComplete') === 'true'

      const storedAlbum = localStorage.getItem('userAlbumImages')
      if (storedAlbum) setAlbumImages(JSON.parse(storedAlbum))

      let displayAccNum = localStorage.getItem('accountNumber') || ''

      if (uid && uid !== 'N/A') {
        try {
          const mongoResponse = await fetch(apiUrl(`/api/users?uid=${encodeURIComponent(uid)}`))
          if (!mongoResponse.ok) throw new Error(`MongoDB user fetch failed: ${mongoResponse.status}`)
          const result = await mongoResponse.json()
          const data = result?.user

          if (data) {
            if (data.accountId) {
              displayAccNum = String(data.accountId)
              localStorage.setItem('accountNumber', displayAccNum)
            }
            const docName = data.name || data.displayName || data.userName
            if (isValidName(docName)) {
              storedName = docName
              localStorage.setItem('userName', storedName)
            }
            if (data.photo || data.photoURL || data.image) {
              photo = data.photo || data.photoURL || data.image || photo
              localStorage.setItem('userPhoto', photo)
            }
            if (data.coverPhoto || data.coverImage) {
              coverPhoto = data.coverPhoto || data.coverImage || coverPhoto
              localStorage.setItem('userCoverPhoto', coverPhoto)
            }
            if (Array.isArray(data.coverPhotos) && data.coverPhotos.length > 0) {
              setCoverPhotos(data.coverPhotos)
              localStorage.setItem('userCoverPhotos', JSON.stringify(data.coverPhotos))
            } else if (coverPhoto) {
              setCoverPhotos([coverPhoto])
              localStorage.setItem('userCoverPhotos', JSON.stringify([coverPhoto]))
            }
            if (data.bio) {
              storedBio = data.bio
              localStorage.setItem('userBio', storedBio)
            }
            if (data.country || data.location) {
              storedCountry = data.country || data.location
              localStorage.setItem('userCountry', storedCountry)
            }
            if (data.countryCode) {
              storedCountryCode = data.countryCode
              localStorage.setItem('userCountryCode', storedCountryCode)
            }
            if (data.countryLocked !== undefined) {
              isCountryLockedInStorage = data.countryLocked
              if (data.countryLocked) localStorage.setItem('userCountryLocked', 'true')
            }
            if (data.setupComplete) {
              isCountryLockedInStorage = true
              localStorage.setItem('userCountryLocked', 'true')
            }
            if (data.gender) {
              storedGender = data.gender
              localStorage.setItem('userGender', storedGender)
            }
            if (data.age) {
              storedAge = String(data.age)
              localStorage.setItem('userAge', storedAge)
            }
            if (data.albumImages && Array.isArray(data.albumImages)) {
              setAlbumImages(data.albumImages)
              localStorage.setItem('userAlbumImages', JSON.stringify(data.albumImages))
            }
            if (!displayAccNum) displayAccNum = getOrCreateAccountNumber(uid)
            if (!isValidName(storedName)) storedName = displayAccNum

            const matchedCountry = COUNTRIES.find(
              (c) => c.code === storedCountryCode || c.flag === storedCountry || c.name === storedCountry
            ) || { name: 'India', flag: '🇮🇳', code: 'IN' }

            const profileData = {
              uid: uid, name: storedName, displayAccountNumber: displayAccNum,
              photo, coverPhoto, bio: storedBio,
              location: matchedCountry.name, flag: matchedCountry.flag, countryCode: matchedCountry.code,
              gender: storedGender === 'female' || storedGender === '♀' ? '♀' : '♂',
              age: storedAge ? parseInt(storedAge) : 24,
              followers: data.followers || 0,
              albumImages: data.albumImages || [],
              coverPhotos: Array.isArray(data.coverPhotos) ? data.coverPhotos : (coverPhoto ? [coverPhoto] : []),
              officialTag: data.officialTag || false,
              adminTag: data.adminTag || false,
              vipTag: data.vipTag || false,
              premiumTag: data.premiumTag || false,
            }
            setUser(profileData)
            await saveProfileToDB(profileData)
            setEditName(storedName)
            setEditAge(storedAge || '24')
            setEditBio(storedBio || '')
            setEditCountry(matchedCountry.name)
            setEditCountryCode(matchedCountry.code)
            setCountryLocked(isCountryLockedInStorage)
            if (storedGender) {
              setEditGender(storedGender === 'female' || storedGender === '♀' ? 'female' : 'male')
              setGenderLocked(true)
            }
          }
        } catch (err) {
          console.warn('MongoDB fetch error in PublicProfile:', err)
        }
      }
    }

    loadProfileData()
    return () => { if (unsubscribe) unsubscribe() }
  }, [isOtherUser, targetUser])

  useEffect(() => {
    if (user.uid && user.uid !== 'N/A') saveCurrentUserToDB();
  }, [user, albumImages, coverPhotos]);

  const handleCopyID = () => {
    if (user.displayAccountNumber && user.displayAccountNumber !== 'N/A') {
      navigator.clipboard.writeText(user.displayAccountNumber)
      alert('ID Copied!')
    }
  }

  const handleOpenEditSheet = () => {
    if (isOtherUser) return
    setEditName(user.name)
    setEditAge(user.age.toString())
    setEditBio(user.bio)
    setShowEditSheet(true)
  }

  const handleCloseEditSheet = () => {
    setShowEditSheet(false)
  }

  const handleAvatarUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (file) {
      try {
        const compressedBase64 = await compressImage(file, 800, 800, 0.85)
        setCropImageSrc(compressedBase64)
      } catch (err) {
        console.error('Avatar compression error:', err)
      } finally {
        e.target.value = ''
      }
    }
  }

  const handleCoverUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    if (coverPhotos.length >= 4) {
      alert('You can only upload up to 4 background images.')
      e.target.value = ''
      return
    }
    try {
      const compressedBase64 = await compressImage(file, 800, 400, 0.7)
      const updated = [...coverPhotos, compressedBase64]
      setCoverPhotos(updated)
      localStorage.setItem('userCoverPhotos', JSON.stringify(updated))
      localStorage.setItem('userCoverPhoto', updated[0])
      setUser((prev) => ({ ...prev, coverPhoto: updated[0] }))
      await saveToMongoDB({ coverPhoto: updated[0], coverImage: updated[0], coverPhotos: updated })
    } catch (err) {
      console.error('Cover compression error:', err)
    } finally {
      e.target.value = ''
    }
  }

  const handleRemoveCoverPhoto = async (indexToRemove: number) => {
    const updated = coverPhotos.filter((_, i) => i !== indexToRemove)
    setCoverPhotos(updated)
    localStorage.setItem('userCoverPhotos', JSON.stringify(updated))
    const primary = updated[0] || ''
    localStorage.setItem('userCoverPhoto', primary)
    setUser((prev) => ({ ...prev, coverPhoto: primary }))
    if (currentCoverIndex >= updated.length) setCurrentCoverIndex(0)
    await saveToMongoDB({ coverPhoto: primary, coverImage: primary, coverPhotos: updated })
  }

  const handleTouchStart = (index: number) => {
    longPressTimer.current = setTimeout(() => {
      handlePinAlbumImage(index)
    }, 600)
  }

  const handleTouchEnd = () => {
    if (longPressTimer.current) {
      clearTimeout(longPressTimer.current)
      longPressTimer.current = null
    }
  }

  const handlePinAlbumImage = async (index: number) => {
    if (index === 0) return
    const updated = [...albumImages]
    const [pinnedImage] = updated.splice(index, 1)
    updated.unshift(pinnedImage)
    setAlbumImages(updated)
    localStorage.setItem('userAlbumImages', JSON.stringify(updated))
    await saveToMongoDB({ albumImages: updated, album: updated })
    if (typeof window !== 'undefined' && navigator.vibrate) {
      navigator.vibrate(50)
    }
  }

  const handleAlbumUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (file) {
      if (albumImages.length >= 7) {
        alert('You can only upload up to 7 images in the album.')
        return
      }
      try {
        const compressedBase64 = await compressImage(file, 600, 600, 0.7)
        const updatedAlbum = [...albumImages, compressedBase64]
        setAlbumImages(updatedAlbum)
        localStorage.setItem('userAlbumImages', JSON.stringify(updatedAlbum))
        await saveToMongoDB({ albumImages: updatedAlbum, album: updatedAlbum })
      } catch (err) {
        console.error('Album image compression error:', err)
      }
    }
  }

  const handleRemoveAlbumImage = async (indexToRemove: number) => {
    const updated = albumImages.filter((_, index) => index !== indexToRemove)
    setAlbumImages(updated)
    localStorage.setItem('userAlbumImages', JSON.stringify(updated))
    await saveToMongoDB({ albumImages: updated, album: updated })
  }

  const handleCropSave = async (croppedImage: string) => {
    setCropImageSrc(null)
    localStorage.setItem('userPhoto', croppedImage)
    setUser((prev) => ({ ...prev, photo: croppedImage }))
    await saveToMongoDB({ photo: croppedImage, image: croppedImage, photoURL: croppedImage })
    await saveProfileToDB({ ...user, photo: croppedImage, albumImages, coverPhotos })
  }

  const handleNameSave = async () => {
    const finalNewName = isValidName(editName) ? editName : user.displayAccountNumber
    localStorage.setItem('userName', finalNewName)
    const updatedUser = { ...user, name: finalNewName }
    setUser(updatedUser)
    setShowNameScreen(false)
    await saveToMongoDB({ name: finalNewName, displayName: finalNewName, userName: finalNewName })
    await saveProfileToDB({ ...updatedUser, albumImages, coverPhotos })
  }

  const handleAgeSave = async () => {
    const parsedAge = parseInt(editAge) || user.age
    localStorage.setItem('userAge', String(parsedAge))
    const updatedUser = { ...user, age: parsedAge }
    setUser(updatedUser)
    setShowAgeScreen(false)
    await saveToMongoDB({ age: parsedAge })
    await saveProfileToDB({ ...updatedUser, albumImages, coverPhotos })
  }

  const handleBirthdayConfirm = async () => {
    const year = birthYear;
    const month = birthMonth;
    const day = birthDay;
    const birthDate = new Date(year, month - 1, day);
    const today = new Date();
    
    let calculatedAge = today.getFullYear() - birthDate.getFullYear();
    const m = today.getMonth() - birthDate.getMonth();
    if (m < 0 || (m === 0 && today.getDate() < birthDate.getDate())) {
      calculatedAge--;
    }
    
    const finalAge = Math.max(0, calculatedAge);
    const ageString = String(finalAge);

    setEditAge(ageString);
    localStorage.setItem('userAge', ageString);
    const updatedUser = { ...user, age: finalAge };
    setUser(updatedUser);
    setShowAgeScreen(false);
    await saveToMongoDB({ age: finalAge });
    await saveProfileToDB({ ...updatedUser, albumImages, coverPhotos });
  };

  const handleBioSave = async () => {
    localStorage.setItem('userBio', editBio)
    const updatedUser = { ...user, bio: editBio };
    setUser(updatedUser);
    setShowBioScreen(false);
    await saveToMongoDB({ bio: editBio, about: editBio });
    await saveProfileToDB({ ...updatedUser, albumImages, coverPhotos });
  }

  const getDisplayID = () => user.displayAccountNumber

  const handleToggleFollow = () => {
    setIsFollowing((prev) => {
      const nextState = !prev
      setUser((u) => ({
        ...u,
        followers: nextState ? u.followers + 1 : Math.max(0, u.followers - 1),
      }))
      return nextState
    })
  }

  const getCurrentUserData = () => {
    const uid = typeof window !== 'undefined'
      ? localStorage.getItem('userUID') || localStorage.getItem('userPhone') || localStorage.getItem('userId') || 'N/A'
      : 'N/A'
    const name = typeof window !== 'undefined'
      ? localStorage.getItem('userName') || user.displayAccountNumber
      : user.displayAccountNumber
    const photo = typeof window !== 'undefined' ? localStorage.getItem('userPhoto') || '' : ''
    return { uid, name, photo }
  }

  const finalDisplayName = isValidName(user.name)
    ? user.name
    : (user.displayAccountNumber ? user.displayAccountNumber : 'User');

  const avatarLetter = finalDisplayName ? finalDisplayName.charAt(0).toUpperCase() : '?';

  const handleImageClick = (index: number) => {
    setFullImageIndex(index);
    setScale(1);
    setPosition({ x: 0, y: 0 });
  };

  const handleNextImage = () => {
    if (fullImageIndex !== null && fullImageIndex < albumImages.length - 1) {
      setFullImageIndex(fullImageIndex + 1);
      setScale(1);
      setPosition({ x: 0, y: 0 });
    }
  };

  const handlePrevImage = () => {
    if (fullImageIndex !== null && fullImageIndex > 0) {
      setFullImageIndex(fullImageIndex - 1);
      setScale(1);
      setPosition({ x: 0, y: 0 });
    }
  };

  const getTouchDistance = (touches: React.TouchList) => {
    const dx = touches[0].clientX - touches[1].clientX;
    const dy = touches[0].clientY - touches[1].clientY;
    return Math.sqrt(dx * dx + dy * dy);
  };

  const handleImageTouchStart = (e: React.TouchEvent) => {
    if (e.touches.length === 2) {
      isPinchingRef.current = true;
      lastTouchDistRef.current = getTouchDistance(e.touches);
      return;
    }

    if (e.touches.length === 1) {
      const now = Date.now();
      const DOUBLE_TAP_DELAY = 300;
      
      if (now - lastTapRef.current < DOUBLE_TAP_DELAY) {
        if (scale > 1) {
          setScale(1);
          setPosition({ x: 0, y: 0 });
        } else {
          setScale(2.5);
        }
        lastTapRef.current = 0;
        return;
      }
      lastTapRef.current = now;

      touchStartXRef.current = e.touches[0].clientX;
      touchEndXRef.current = 0;
      
      if (scale > 1) {
        isDraggingRef.current = true;
        dragStartRef.current = {
          x: e.touches[0].clientX - position.x,
          y: e.touches[0].clientY - position.y,
        };
      }
    }
  };

  const handleImageTouchMove = (e: React.TouchEvent) => {
    if (e.touches.length === 2) {
      e.preventDefault();
      const currentDist = getTouchDistance(e.touches);
      if (lastTouchDistRef.current > 0) {
        const delta = currentDist / lastTouchDistRef.current;
        const newScale = Math.min(Math.max(scale * delta, 1), 5);
        setScale(newScale);
        if (newScale === 1) setPosition({ x: 0, y: 0 });
      }
      lastTouchDistRef.current = currentDist;
      return;
    }

    if (e.touches.length === 1) {
      e.preventDefault();
      
      if (scale > 1 && isDraggingRef.current) {
        const newX = e.touches[0].clientX - dragStartRef.current.x;
        const newY = e.touches[0].clientY - dragStartRef.current.y;
        setPosition({ x: newX, y: newY });
        return;
      }

      touchEndXRef.current = e.touches[0].clientX;
    }
  };

  const handleImageTouchEnd = (e: React.TouchEvent) => {
    if (isPinchingRef.current) {
      isPinchingRef.current = false;
      lastTouchDistRef.current = 0;
      if (scale <= 1) {
        setScale(1);
        setPosition({ x: 0, y: 0 });
      }
      return;
    }

    isDraggingRef.current = false;

    if (scale > 1) return;

    const startX = touchStartXRef.current;
    const endX = touchEndXRef.current;
    
    if (startX && endX) {
      const distance = startX - endX;
      const minSwipeDistance = 50;

      if (distance > minSwipeDistance) {
        handleNextImage();
      } else if (distance < -minSwipeDistance) {
        handlePrevImage();
      }
    }
    
    touchStartXRef.current = 0;
    touchEndXRef.current = 0;
  };

  return (
    <div className={`w-full bg-white min-h-screen text-gray-900 relative ${isOtherUser ? 'pb-24' : 'pb-10'}`}>
      {/* Cover Image & Header Section */}
      <div className="relative w-full h-[350px] bg-gray-800 overflow-hidden">
        {coverPhotos.length > 0 ? (
          <div
            className="flex h-full transition-transform duration-700 ease-in-out"
            style={{ transform: `translateX(-${currentCoverIndex * 100}%)` }}
          >
            {coverPhotos.map((photo, idx) => (
              <img key={idx} src={photo} alt="" className="w-full h-full object-cover shrink-0" />
            ))}
          </div>
        ) : user.photo ? (
          <img src={user.photo} alt="" className="w-full h-full object-cover" />
        ) : (
          <div className="w-full h-full bg-gray-800 flex items-center justify-center text-white text-4xl font-bold">
            {avatarLetter}
          </div>
        )}

        {coverPhotos.length > 1 && (
          <div className="absolute bottom-4 left-1/2 transform -translate-x-1/2 flex gap-1.5 z-20">
            {coverPhotos.map((_, idx) => (
              <div
                key={idx}
                className={`h-1.5 rounded-full transition-all ${
                  idx === currentCoverIndex ? 'w-4 bg-white' : 'w-1.5 bg-white/50'
                }`}
              />
            ))}
          </div>
        )}

        <div className="absolute top-0 pt-[calc(env(safe-area-inset-top,0px)+24px)] mt-2 left-0 right-0 px-3 flex items-center justify-between z-10">
          <button onClick={onBack} className="text-white">
            <ArrowLeft size={28} />
          </button>

          {isOtherUser ? (
            <button onClick={() => setShowActionSheet(true)} className="text-white">
              <AlertTriangle size={24} />
            </button>
          ) : (
            <button onClick={handleOpenEditSheet} className="text-white">
              <Edit3 size={22} />
            </button>
          )}
        </div>

        <div className="absolute bottom-12 left-6 flex items-center z-30 pointer-events-none">
          <div className="relative w-24 h-24 rounded-full shadow-lg bg-gray-700">
            <div className="w-full h-full rounded-full overflow-hidden">
              {user.photo ? (
                <img src={user.photo} alt="" className="w-full h-full object-cover" />
              ) : (
                <div className="w-full h-full bg-gray-600 flex items-center justify-center text-4xl text-white font-bold">
                  {avatarLetter}
                </div>
              )}
            </div>

            <div className="absolute inset-0 pointer-events-none">
              <WhiteColorRemovalShader
                imageSrc="/1786867564769.png"
                threshold={0.85}
                className="w-full h-full"
                style={{
                  position: 'absolute',
                  top: '50%',
                  left: '50%',
                  transform: 'translate(-50%, -50%) scale(1.5)',
                  width: '100%',
                  height: '100%',
                }}
              />
            </div>
          </div>
        </div>
      </div>

      {/* Profile Info Details Section */}
      <div className="relative bg-white rounded-xl -mt-6 px-3 pt-6 z-20">
        <div className="flex flex-wrap items-center gap-0.5">
          <h1 className="text-2xl font-bold text-black tracking-wide">{finalDisplayName}</h1>
          <span className="bg-blue-500 text-white text-xs px-2 py-0.5 rounded-full font-bold inline-flex items-center gap-0.5 whitespace-nowrap">
            {user.gender} {user.age}
          </span>

          {user.adminTag && (
            <GreenColorRemovalShader imageSrc="/1788021461820~2.jpg" className="h-9 w-auto object-contain" />
          )}
          {user.officialTag && (
            <GreenColorRemovalShader imageSrc="/1788021468845~2.jpg" className="h-9 w-auto object-contain" />
          )}
          {user.vipTag && <img src="/1785469775751.png" alt="VIP" className="h-7 w-auto object-contain" />}
          {user.premiumTag && <img src="/1785469365805.png" alt="Premium" className="h-7 w-auto object-contain" />}
        </div>

        <div className="flex items-center gap-1 text-xs mt-0.5 font-medium">
          <div className="flex items-center gap-1">
            {isSpecialAccount ? (
              <>
                <span
                  className="relative font-bold rounded text-white -ml-2.5"
                  style={{
                    backgroundImage: 'url(/1785137282040.png)',
                    backgroundSize: 'cover', backgroundPosition: 'center',
                    minWidth: '90px', paddingLeft: '0px', paddingRight: '5px',
                    paddingTop: '2px', paddingBottom: '2px',
                  }}
                >
                  <span className="relative text-xs" style={{ paddingLeft: '32px' }}>
                    {user.displayAccountNumber}
                  </span>
                </span>
                <button onClick={handleCopyID} className="text-gray-400 hover:text-gray-600">
                  <Copy size={12} />
                </button>
              </>
            ) : (
              <>
                <span className="text-gray-500">ID:{getDisplayID()}</span>
                <button onClick={handleCopyID} className="text-gray-400 hover:text-gray-600">
                  <Copy size={12} />
                </button>
              </>
            )}
          </div>
          <span className="text-gray-300">|</span>
          <span className="text-gray-500">{user.followers} Fans</span>
        </div>

        <div className="mt-1 flex items-center gap-1 -ml-2">
          <div className="relative inline-flex items-center justify-center ml-0.5">
            <img src="/IMG_20260917_220530.png" alt="" className="h-6 w-auto object-contain" />
            <span
              className="absolute inset-0 flex items-center justify-center text-[10px] font-bold text-white drop-shadow-sm"
              style={{ paddingLeft: '10px' }}
            >
              Lv.1
            </span>
          </div>
          <img src="/1785486414756.png" alt="" className="h-6 w-auto object-contain" />
        </div>

        <div className="flex items-center gap-1.5 text-xs text-gray-600 mt-3">
          <MapPin size={14} className="text-gray-400" />
          <span className="text-base">{user.flag}</span>
          <span className="text-gray-500">{user.location || 'India'}</span>
        </div>

        <div className="flex items-start gap-2 mt-2">
          <button
            onClick={!isOtherUser ? handleOpenEditSheet : undefined}
            className={`mt-0.5 shrink-0 ${isOtherUser ? 'text-gray-400' : 'text-gray-400 hover:text-gray-600'}`}
          >
            <Edit3 size={14} />
          </button>
          {user.bio ? (
            <p className="text-xs text-gray-500 italic">{user.bio}</p>
          ) : (
            <p className="text-xs text-gray-400 italic">
              {isOtherUser ? 'No bio added yet' : 'Add bio...'}
            </p>
          )}
        </div>

        <div className="flex gap-0 mt-4 border-b border-gray-200">
          <button
            onClick={() => setActiveTab('profile')}
            className={`px-6 py-2 text-sm font-semibold transition-colors relative ${
              activeTab === 'profile' ? 'text-blue-500' : 'text-gray-500'
            }`}
          >
            Profile
            {activeTab === 'profile' && (
              <div className="absolute bottom-0 left-1/2 transform -translate-x-1/2 w-4 h-1 bg-blue-500 rounded-full"></div>
            )}
          </button>
        </div>
      </div>

      {/* Content Tabs Section */}
      <div className="px-5 mt-6 space-y-4">
        <div>
          <h3 className="text-sm font-bold text-gray-800 mb-2 flex justify-between items-center">
            Albums
            <span className="text-xs text-gray-400 font-normal">{albumImages.length}/4</span>
          </h3>
          {albumImages.length > 0 ? (
            <div className="grid grid-cols-4 gap-2">
              {albumImages.map((img, index) => (
                <div
                  key={index}
                  className="w-20 h-20 rounded-xl overflow-hidden bg-gray-100 shrink-0 cursor-pointer hover:opacity-90 transition-opacity"
                  onClick={() => handleImageClick(index)}
                >
                  <img src={img} alt="" className="w-full h-full object-cover" />
                </div>
              ))}
            </div>
          ) : (
            <div className="w-full h-28 rounded-2xl overflow-hidden bg-gray-100">
              <img src="/IMG_20260726_225835.jpg" alt="" className="w-full h-full object-cover" />
            </div>
          )}
        </div>

        <div>
          <h3 className="text-sm font-bold text-gray-800 mb-2">Vehicle</h3>
          <div className="w-full h-28 rounded-2xl overflow-hidden">
            <img src="/1785091443553.png" alt="" className="w-full h-full object-cover" />
          </div>
        </div>

        <div>
          <h3 className="text-sm font-bold text-gray-800 mb-2">Medal</h3>
          <div className="w-full h-28 rounded-2xl overflow-hidden">
            <img src="/1785091431545.png" alt="" className="w-full h-full object-cover" />
          </div>
        </div>

        <div>
          <h3 className="text-sm font-bold text-gray-800 mb-2">Frame</h3>
          {ownedFrameItems.length > 0 ? (
            <div className="grid grid-cols-4 gap-2">
              {ownedFrameItems.slice(0, 4).map((frame) => (
                <div key={frame.id} className="w-20">
                  <div className="w-20 h-20 overflow-hidden">
                    <GreenColorRemovalVideo
                      src={frame.image}
                      className="w-full h-full object-contain"
                    />
                  </div>
                  <div className="mt-1 text-center">
                    <div className="text-[11px] font-medium text-gray-800 truncate">{frame.name}</div>
                    <div className="flex items-center justify-center gap-0.5 text-[10px] text-yellow-500">
                      {Array.from({ length: 5 }).map((_, index) => <span key={index}>★</span>)}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="w-full h-28 rounded-2xl overflow-hidden">
              <img src="/1785091457562.png" alt="" className="w-full h-full object-cover" />
            </div>
          )}
        </div>

        <div>
          <h3 className="text-sm font-bold text-gray-800 mb-2">Gift</h3>
          {receivedGifts.length > 0 ? (
            <div className="grid grid-cols-4 gap-2">
              {receivedGifts.slice(0, 4).map((gift) => (
                <div key={gift.id} className="w-20 h-20 rounded-xl overflow-hidden bg-gray-100">
                  <img src={gift.image} alt={gift.name || ''} className="w-full h-full object-contain" />
                </div>
              ))}
            </div>
          ) : (
            <div className="w-full h-28 rounded-2xl overflow-hidden">
              <img src="/1785091520912.png" alt="" className="w-full h-full object-cover" />
            </div>
          )}
        </div>
      </div>

      {/* Bottom Action Bar for Other User */}
      {isOtherUser && (
        <div className="fixed bottom-0 left-0 right-0 z-40 bg-white/95 backdrop-blur-md px-6 py-3.5 border-t border-gray-100 flex items-center justify-between gap-4 max-w-md mx-auto shadow-lg">
          <button
            onClick={handleToggleFollow}
            className="flex-1 h-12 rounded-2xl bg-gradient-to-r from-[#ff5874] to-[#ff6b8b] active:scale-95 transition-all duration-200 flex items-center justify-center gap-2 text-white font-medium text-lg shadow-md shadow-pink-200"
          >
            <Heart className="w-6 h-6 fill-white stroke-none" />
            <span>{isFollowing ? 'Following' : 'Follow'}</span>
          </button>

          <button
            onClick={() => setShowChat(true)}
            className="flex-1 h-12 rounded-2xl bg-gradient-to-r from-[#1dc4e9] to-[#1de9b6] active:scale-95 transition-all duration-200 flex items-center justify-center gap-2 text-white font-medium text-lg shadow-md shadow-cyan-200"
          >
            <MessageCircle className="w-6 h-6 fill-white stroke-none" />
            <span>Chat</span>
          </button>
        </div>
      )}

      {/* ✅ Full Image View Modal with Swipe + Zoom + Drag */}
      {fullImageIndex !== null && (
        <div 
          className="fixed inset-0 z-[60] bg-black flex flex-col select-none overflow-hidden"
          style={{ touchAction: 'none' }}
          onTouchStart={handleImageTouchStart}
          onTouchMove={handleImageTouchMove}
          onTouchEnd={handleImageTouchEnd}
        >
          <div className="absolute top-0 left-0 right-0 z-20 flex items-start justify-between px-1 pt-1 pointer-events-none">
            <button
              onClick={() => setFullImageIndex(null)}
              className="text-white p-1 hover:bg-white/10 rounded-full transition-colors pointer-events-auto"
            >
              <ArrowLeft size={28} />
            </button>
            
            <span className="text-white font-medium text-base pt-2 pr-2 pointer-events-auto">
              {fullImageIndex + 1}/{albumImages.length}
            </span>
          </div>

          <div 
            ref={imageContainerRef}
            className="flex-1 flex items-center justify-center overflow-hidden"
          >
            <img
              src={albumImages[fullImageIndex]}
              alt=""
              className="max-w-full max-h-[85vh] object-contain select-none pointer-events-none"
              style={{
                transform: `translate(${position.x}px, ${position.y}px) scale(${scale})`,
                transition: isPinchingRef.current || isDraggingRef.current ? 'none' : 'transform 0.2s ease-out',
                transformOrigin: 'center center',
              }}
              draggable={false}
            />
          </div>

          {scale > 1 && (
            <div className="absolute bottom-6 left-1/2 transform -translate-x-1/2 bg-black/60 text-white text-xs px-3 py-1 rounded-full z-20 pointer-events-none">
              {Math.round(scale * 100)}%
            </div>
          )}

          {fullImageIndex > 0 && (
            <button 
              onClick={handlePrevImage}
              className="absolute left-4 top-1/2 transform -translate-y-1/2 text-white/60 hover:text-white bg-black/40 hover:bg-black/70 p-3 rounded-full transition-all hidden md:flex z-20"
            >
              <ArrowLeft size={28} />
            </button>
          )}
          {fullImageIndex < albumImages.length - 1 && (
            <button 
              onClick={handleNextImage}
              className="absolute right-4 top-1/2 transform -translate-y-1/2 text-white/60 hover:text-white bg-black/40 hover:bg-black/70 p-3 rounded-full transition-all hidden md:flex z-20"
            >
              <ArrowLeft size={28} className="rotate-180" />
            </button>
          )}
        </div>
      )}

      {/* ✅ Crop Screen Modal */}
      {cropImageSrc && (
        <CropScreen
          imageSrc={cropImageSrc}
          onBack={() => setCropImageSrc(null)}
          onSave={handleCropSave}
        />
      )}

      {/* ✅ Edit Profile Bottom Sheet */}
      {!isOtherUser && showEditSheet && (
        <div className="fixed inset-0 z-50 flex items-end justify-center">
          <div className="absolute inset-0 bg-black/50" onClick={handleCloseEditSheet}></div>

          <div className="relative bg-white w-full max-w-md rounded-[6px] animate-slide-up flex flex-col h-[70vh]">
            <div className="flex items-center justify-between px-3 py-4 shrink-0">
              <button onClick={handleCloseEditSheet}>
                <ArrowLeft size={24} className="text-gray-700" />
              </button>
              <h2 className="text-lg font-bold text-gray-900">Edit Information</h2>
              <div className="w-6"></div>
            </div>

            <div className="overflow-hidden px-5 py-4 space-y-6 flex-1">
              <input type="file" ref={avatarInputRef} accept="image/*" onChange={handleAvatarUpload} className="hidden" />
              <input type="file" ref={albumInputRef} accept="image/*" onChange={handleAlbumUpload} className="hidden" />
              <input type="file" ref={coverInputRef} accept="image/*" onChange={handleCoverUpload} className="hidden" />

              {/* 1. Avatar */}
              <div className="flex items-center justify-between">
                <span className="text-sm font-medium text-gray-700">Avatar</span>
                <div className="flex items-center gap-2">
                   <div
                    onClick={() => avatarInputRef.current?.click()}
                    className="w-14 h-14 rounded-full overflow-hidden bg-gray-200 cursor-pointer shrink-0"
                  >
                    {user.photo ? (
                      <img src={user.photo} alt="" className="w-full h-full object-cover" />
                    ) : (
                      <div className="w-full h-full bg-gray-600 flex items-center justify-center text-xl text-white font-bold">
                        {avatarLetter}
                      </div>
                    )}
                  </div>
                </div>
              </div>

              {/* 2. Nickname */}
              <div className="flex items-center justify-between">
                <span className="text-sm font-medium text-gray-700">Nickname</span>
                <button onClick={() => setShowNameScreen(true)} className="flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700 transition">
                  <span className="max-w-[150px] truncate">{editName || 'Enter name'}</span>
                  <ChevronRight size={16} className="text-gray-400" />
                </button>
              </div>

              {/* 3. Age */}
              <div className="flex items-center justify-between">
                <span className="text-sm font-medium text-gray-700">Age</span>
                <button onClick={() => setShowAgeScreen(true)} className="flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700 transition">
                  <span>{editAge || '0'}</span>
                  <ChevronRight size={16} className="text-gray-400" />
                </button>
              </div>

              {/* 4. BACKGROUND — max 4 */}
              <div className="space-y-3 pt-2">
                <div className="flex items-center justify-between">
                  <span className="text-sm font-medium text-gray-700">
                    Background ({coverPhotos.length}/4)
                  </span>
                </div>
                <div className="flex gap-2 overflow-x-auto pb-2 shrink-0">
                  {/* ✅ + icon PEHLE */}
                  {coverPhotos.length < 4 && (
                    <button
                      onClick={() => coverInputRef.current?.click()}
                      className="w-16 h-16 rounded-xl bg-gray-100 flex items-center justify-center text-gray-300 hover:bg-gray-200 hover:text-gray-400 transition-colors shrink-0"
                    >
                      <span className="text-3xl font-thin leading-none">+</span>
                    </button>
                  )}
                  {coverPhotos.map((photo, idx) => (
                    <div
                      key={idx}
                      className="relative w-16 h-16 rounded-md overflow-hidden border border-gray-200 group shrink-0"
                    >
                      <img src={photo} alt="" className="w-full h-full object-cover" />
                      {/* ✅ Trash icon (image jaisa) */}
                      <button
                        onClick={() => handleRemoveCoverPhoto(idx)}
                        className="absolute top-1 right-1 bg-black/60 text-white rounded-full w-6 h-6 flex items-center justify-center hover:bg-red-600 transition-colors shadow z-10"
                      >
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                          <path d="M4 7H20" stroke="white" strokeWidth="2" strokeLinecap="round"/>
                          <path d="M5 7L6 20C6 20.5304 6.21071 21.0391 6.58579 21.4142C6.96086 21.7893 7.46957 22 8 22H16C16.5304 22 17.0391 21.7893 17.4142 21.4142C17.7893 21.0391 18 20.5304 18 20L19 7" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                          <path d="M9 11V17" stroke="white" strokeWidth="2" strokeLinecap="round"/>
                          <path d="M15 11V17" stroke="white" strokeWidth="2" strokeLinecap="round"/>
                          <path d="M10 4V2H14V4" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                        </svg>
                      </button>
                    </div>
                  ))}
                </div>
              </div>

              {/* 5. Bio */}
              <div className="flex items-center justify-between">
                <span className="text-sm font-medium text-gray-700">Bio</span>
                <button onClick={() => setShowBioScreen(true)} className="flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700 transition">
                  <span className="max-w-[180px] truncate">{editBio || 'Add bio...'}</span>
                  <ChevronRight size={16} className="text-gray-400" />
                </button>
              </div>

              {/* 6. ALBUM Inline — max 4 with Pin button */}
              <div className="space-y-3 pt-2 pb-6">
                <div className="flex items-center justify-between">
                  <span className="text-sm font-medium text-gray-700">
                    Album Photos ({albumImages.length}/4)
                  </span>
                </div>
                <div className="flex gap-2 overflow-x-auto pb-2 shrink-0">
                  {/* ✅ + icon PEHLE, uske baad images */}
                  {albumImages.length < 4 && (
                    <button
                      onClick={() => albumInputRef.current?.click()}
                      className="w-16 h-16 rounded-xl bg-gray-100 flex items-center justify-center text-gray-300 hover:bg-gray-200 hover:text-gray-400 transition-colors shrink-0"
                    >
                      <span className="text-3xl font-thin leading-none">+</span>
                    </button>
                  )}
                  {albumImages.map((img, idx) => (
                    <div
                      key={idx}
                      className="relative w-16 h-16 rounded-md overflow-hidden border border-gray-200 group cursor-pointer shrink-0"
                      onTouchStart={() => handleTouchStart(idx)}
                      onTouchEnd={handleTouchEnd}
                      onMouseDown={() => handleTouchStart(idx)}
                      onMouseUp={handleTouchEnd}
                      onMouseLeave={handleTouchEnd}
                    >
                      <img src={img} alt="" className="w-full h-full object-cover pointer-events-none" />

                      {/* Pin button — sirf pinned image par (index 0) */}
                      {idx === 0 && (
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                          }}
                          className="absolute top-1 left-1 bg-blue-500/90 text-white rounded-full w-5 h-5 flex items-center justify-center shadow z-10"
                          title="Pinned"
                        >
                          <Pin size={11} className="fill-white" />
                        </button>
                      )}

                      {/* ✅ Trash icon (image jaisa) */}
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          handleRemoveAlbumImage(idx);
                        }}
                        className="absolute top-1 right-1 bg-black/60 text-white rounded-full w-6 h-6 flex items-center justify-center hover:bg-red-600 transition-colors shadow z-10"
                      >
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                          <path d="M4 7H20" stroke="white" strokeWidth="2" strokeLinecap="round"/>
                          <path d="M5 7L6 20C6 20.5304 6.21071 21.0391 6.58579 21.4142C6.96086 21.7893 7.46957 22 8 22H16C16.5304 22 17.0391 21.7893 17.4142 21.4142C17.7893 21.0391 18 20.5304 18 20L19 7" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                          <path d="M9 11V17" stroke="white" strokeWidth="2" strokeLinecap="round"/>
                          <path d="M15 11V17" stroke="white" strokeWidth="2" strokeLinecap="round"/>
                          <path d="M10 4V2H14V4" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                        </svg>
                      </button>
                    </div>
                  ))}
                </div>
              </div>

            </div>
          </div>
        </div>
      )}

      {/* Nickname Edit Screen */}
      {!isOtherUser && showNameScreen && (
        <div className="fixed inset-0 z-[65] flex items-end justify-center">
          <div className="absolute inset-0 bg-black/50" onClick={handleNameSave}></div>
          <div className="relative bg-white w-full max-w-md rounded-t-md animate-slide-up flex flex-col h-[40vh]">
            <div className="flex items-center justify-between px-5 py-4 shrink-0">
              <button onClick={handleNameSave}>
                <ArrowLeft size={24} className="text-gray-700" />
              </button>
              <h2 className="text-lg font-bold text-gray-900">Nickname</h2>
              <div className="w-6"></div>
            </div>

            <div className="flex-1 px-5 overflow-y-auto">
              <input
                type="text"
                value={editName}
                onChange={(e) => setEditName(e.target.value)}
                placeholder="Enter new name"
                className="w-full p-4 bg-gray-100 rounded-lg text-sm text-gray-900 outline-none border border-transparent focus:border-blue-500"
              />
            </div>
            
            <div className="px-5 pb-6 pt-2 shrink-0">
              <button
                onClick={handleNameSave}
                className="w-full bg-[#1dc4e9] text-white py-3.5 rounded-full font-semibold text-lg hover:bg-[#1de9b6] transition-colors"
              >
                Save
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Birthday Wheel Picker */}
      {!isOtherUser && showAgeScreen && (
        <div className="fixed inset-0 z-[65] flex items-end justify-center">
          <div className="absolute inset-0 bg-black/50" onClick={() => setShowAgeScreen(false)}></div>
          <div className="relative bg-white w-full max-w-md rounded-t-2xl animate-slide-up flex flex-col pt-2 pb-8">
            <div className="flex items-center justify-between px-5 py-4 shrink-0 border-b border-gray-100">
              <button onClick={() => setShowAgeScreen(false)} className="text-gray-500 text-base font-medium">
                Cancel
              </button>
              <h2 className="text-lg font-bold text-gray-900">Birthday</h2>
              <button onClick={handleBirthdayConfirm} className="text-[#1dc4e9] text-base font-bold">
                Confirm
              </button>
            </div>

            <div className="relative flex justify-center items-center h-[250px] overflow-hidden">
              <div className="absolute top-1/2 left-0 right-0 h-[50px] -mt-[25px] border-t border-b border-gray-200 bg-gray-50/50 pointer-events-none z-0"></div>
              
              <div className="flex w-full justify-between px-12 z-10 h-full">
                <div 
                  ref={dayRef}
                  className="flex-1 overflow-y-scroll snap-y snap-mandatory text-center h-full relative"
                  style={{ scrollbarWidth: 'none', msOverflowStyle: 'none' }}
                  onScroll={(e) => {
                    const el = e.currentTarget;
                    const index = Math.round(el.scrollTop / 50);
                    if (days[index] !== undefined) setBirthDay(days[index]);
                  }}
                >
                  <div className="h-[100px]"></div>
                  {days.map((d) => (
                    <div key={d} className="h-[50px] snap-center flex items-center justify-center text-xl font-medium text-gray-800">
                      {String(d).padStart(2, '0')}
                    </div>
                  ))}
                  <div className="h-[100px]"></div>
                </div>

                <div 
                  ref={monthRef}
                  className="flex-1 overflow-y-scroll snap-y snap-mandatory text-center h-full relative"
                  style={{ scrollbarWidth: 'none', msOverflowStyle: 'none' }}
                  onScroll={(e) => {
                    const el = e.currentTarget;
                    const index = Math.round(el.scrollTop / 50);
                    if (months[index] !== undefined) setBirthMonth(months[index]);
                  }}
                >
                  <div className="h-[100px]"></div>
                  {months.map((m) => (
                    <div key={m} className="h-[50px] snap-center flex items-center justify-center text-xl font-medium text-gray-800">
                      {String(m).padStart(2, '0')}
                    </div>
                  ))}
                  <div className="h-[100px]"></div>
                </div>

                <div 
                  ref={yearRef}
                  className="flex-1 overflow-y-scroll snap-y snap-mandatory text-center h-full relative"
                  style={{ scrollbarWidth: 'none', msOverflowStyle: 'none' }}
                  onScroll={(e) => {
                    const el = e.currentTarget;
                    const index = Math.round(el.scrollTop / 50);
                    if (years[index] !== undefined) setBirthYear(years[index]);
                  }}
                >
                  <div className="h-[100px]"></div>
                  {years.map((y) => (
                    <div key={y} className="h-[50px] snap-center flex items-center justify-center text-xl font-medium text-gray-800">
                      {y}
                    </div>
                  ))}
                  <div className="h-[100px]"></div>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Bio Edit Screen */}
      {!isOtherUser && showBioScreen && (
        <div className="fixed inset-0 z-[65] flex items-end justify-center">
          <div className="absolute inset-0 bg-black/50" onClick={handleBioSave}></div>
          <div className="relative bg-white w-full max-w-md rounded-t-md animate-slide-up flex flex-col h-[50vh]">
            <div className="flex items-center justify-between px-5 py-4 shrink-0">
              <button onClick={handleBioSave}>
                <ArrowLeft size={24} className="text-gray-700" />
              </button>
              <h2 className="text-lg font-bold text-gray-900">Bio</h2>
              <div className="w-6"></div>
            </div>

            <div className="flex-1 px-5 overflow-y-auto">
              <textarea
                value={editBio}
                onChange={(e) => {
                  if (e.target.value.length <= 50) {
                    setEditBio(e.target.value)
                  }
                }}
                placeholder="Write a bio..."
                className="w-full h-32 p-4 bg-gray-100 rounded-lg text-sm text-gray-900 outline-none resize-none border border-transparent focus:border-blue-500"
              />
              <div className="text-right text-xs text-gray-400 mt-1">
                {editBio.length}/50
              </div>
            </div>
            
            <div className="px-5 pb-6 pt-2 shrink-0">
              <button
                onClick={handleBioSave}
                className="w-full bg-[#1dc4e9] text-white py-3.5 rounded-full font-semibold text-lg hover:bg-[#1de9b6] transition-colors"
              >
                Save
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Action Sheet */}
      {showActionSheet && (
        <div className="fixed inset-0 z-50 flex items-end justify-center pointer-events-none">
          <div
            className="absolute inset-0 bg-transparent pointer-events-auto"
            onClick={() => setShowActionSheet(false)}
          ></div>

          <div className="relative bg-black w-full max-w-md rounded-t-md animate-slide-up flex flex-col pb-6 pt-4 shadow-2xl pointer-events-auto">
            <div className="flex flex-col text-white px-4">
              <button
                onClick={() => {
                  setShowActionSheet(false)
                  setShowUserReport(true)
                }}
                className="w-full text-center px-4 py-4 text-lg transition-colors font-medium active:bg-gray-900 rounded-md"
              >
                Report
              </button>

              <button
                onClick={() => {
                  setShowActionSheet(false)
                  alert('Block user')
                }}
                className="w-full text-center px-4 py-4 text-lg transition-colors font-medium active:bg-gray-900 mb-2"
              >
                Block
              </button>
            </div>

            <div className="px-4 mt-2">
              <button
                onClick={() => setShowActionSheet(false)}
                className="w-full bg-blue-500 text-white py-3.5 rounded-xl font-semibold hover:bg-blue-600 active:scale-[0.98] transition-all"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}

      {showReportToast && (
        <div className="fixed bottom-24 left-1/2 transform -translate-x-1/2 z-[110] bg-black/90 text-white text-sm font-medium px-6 py-2.5 rounded-full shadow-lg pointer-events-none animate-slide-up">
          Report Successful
        </div>
      )}

      {isOtherUser && showChat && targetUser && (
        <div className="fixed inset-0 z-[100]">
          <ChatScreen
            currentUser={getCurrentUserData()}
            targetUser={{
              uid: targetUser.uid || targetUser.id || '',
              accountId: targetUser.displayAccountNumber || targetUser.accountId,
              name: isValidName(targetUser.name) ? targetUser.name! : (targetUser.displayAccountNumber || 'User'),
              photo: targetUser.photo || targetUser.image || '',
            } as any}
            onClose={() => setShowChat(false)}
            onJoinRoom={onJoinRoom}
          />
        </div>
      )}

      {isOtherUser && showUserReport && targetUser && (
        <div className="fixed inset-0 ]">
          <UserReport
            currentUser={getCurrentUserData()}
            targetUser={targetUser}
            onClose={() => setShowUserReport(false)}
          />
        </div>
      )}

      <style jsx>{`
        @keyframes slideUp {
          from { transform: translateY(100%); }
          to { transform: translateY(0); }
        }
        .animate-slide-up {
          animation: slideUp 0.3s ease-out;
        }
        .overflow-y-scroll::-webkit-scrollbar {
          display: none;
        }
        .overflow-y-scroll {
          -ms-overflow-style: none;
          scrollbar-width: none;
        }
      `}</style>
    </div>
  )
}
