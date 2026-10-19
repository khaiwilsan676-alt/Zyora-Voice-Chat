'use client'

import { useState, useEffect, useRef } from 'react'

// ==========================================
// Shared Wallet DB (Same as GiftPicker / WildParty / Store / SellerCenter)
// ==========================================
const DB_NAME = 'FruitPartyDB';
const STORE_NAME = 'GameState';
const DEFAULT_BALANCE = 0;

// Single cached connection — avoids leaking an IndexedDB connection on every poll
let dbPromise: Promise<IDBDatabase> | null = null;

async function initDB(): Promise<IDBDatabase> {
  if (typeof window === 'undefined') return Promise.reject(new Error('No window'));
  if (dbPromise) return dbPromise;

  dbPromise = new Promise<IDBDatabase>((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, 3);
    request.onupgradeneeded = (e: any) => {
      const db = e.target.result;
      if (!db.objectStoreNames.contains(STORE_NAME)) {
        db.createObjectStore(STORE_NAME);
      }
      if (!db.objectStoreNames.contains('transactions')) {
        db.createObjectStore('transactions', { keyPath: 'id', autoIncrement: true });
      }
    };
    request.onsuccess = () => {
      const db = request.result;
      db.onversionchange = () => { db.close(); dbPromise = null; };
      db.onclose = () => { dbPromise = null; };
      resolve(db);
    };
    request.onerror = () => {
      dbPromise = null;
      reject(request.error);
    };
  });

  return dbPromise;
}

async function loadBalanceFromDB(): Promise<number> {
  try {
    const db = await initDB();
    return new Promise((resolve) => {
      const tx = db.transaction(STORE_NAME, 'readonly');
      const req = tx.objectStore(STORE_NAME).get('user_data');
      req.onsuccess = () => {
        if (req.result && typeof req.result.balance === 'number') {
          resolve(req.result.balance);
        } else {
          resolve(DEFAULT_BALANCE);
        }
      };
      req.onerror = () => resolve(DEFAULT_BALANCE);
    });
  } catch {
    return DEFAULT_BALANCE;
  }
}

export async function addDiamondsToDB(amountToAdd: number): Promise<void> {
  if (!Number.isFinite(amountToAdd) || amountToAdd <= 0) return;
  try {
    const db = await initDB();
    return new Promise((resolve, reject) => {
      const tx = db.transaction(STORE_NAME, 'readwrite');
      const store = tx.objectStore(STORE_NAME);
      const req = store.get('user_data');
      req.onsuccess = () => {
        const data = req.result || {};
        const current = Number(data.diamonds) || 0;
        const putReq = store.put({ ...data, diamonds: Math.max(0, current + amountToAdd) }, 'user_data');
        putReq.onsuccess = () => resolve();
        putReq.onerror = () => reject(putReq.error);
      };
      req.onerror = () => reject(req.error);
    });
  } catch (e) {
    console.error('Failed to update diamonds in DB', e);
  }
}

async function subtractDiamondsFromDB(amount: number): Promise<boolean> {
  if (!Number.isFinite(amount) || amount <= 0) return false;
  try {
    const db = await initDB();
    return new Promise((resolve) => {
      const tx = db.transaction(STORE_NAME, 'readwrite');
      const store = tx.objectStore(STORE_NAME);
      const req = store.get('user_data');
      req.onsuccess = () => {
        const data = req.result || {};
        const current = Number(data.diamonds) || 0;
        if (current < amount) return resolve(false);
        const putReq = store.put({ ...data, diamonds: current - amount }, 'user_data');
        putReq.onsuccess = () => resolve(true);
        putReq.onerror = () => resolve(false);
      };
      req.onerror = () => resolve(false);
    });
  } catch {
    return false;
  }
}

async function loadDiamondBalanceFromDB(): Promise<number> {
  try {
    const db = await initDB();
    return new Promise((resolve) => {
      const tx = db.transaction(STORE_NAME, 'readonly');
      const req = tx.objectStore(STORE_NAME).get('user_data');
      req.onsuccess = () => resolve(Math.max(0, Number(req.result?.diamonds) || 0));
      req.onerror = () => resolve(0);
    });
  } catch {
    return 0;
  }
}

async function addCoinsToDB(amountToAdd: number): Promise<void> {
  try {
    const db = await initDB();
    return new Promise((resolve, reject) => {
      const tx = db.transaction(STORE_NAME, 'readwrite');
      const store = tx.objectStore(STORE_NAME);
      const req = store.get('user_data');

      req.onsuccess = () => {
        const data = req.result;
        const current = data?.balance ?? DEFAULT_BALANCE;
        const next = Math.max(0, current + amountToAdd);
        const putReq = store.put({ ...(data || {}), balance: next }, 'user_data');
        putReq.onsuccess = () => resolve();
        putReq.onerror = () => reject(putReq.error);
      };
      req.onerror = () => reject(req.error);
    });
  } catch (e) {
    console.error('Failed to update coins in DB', e);
  }
}

// type: 'coin' | 'diamond'
export async function recordTransaction(
  title: string,
  amount: number,
  type: 'coin' | 'diamond' = 'coin'
): Promise<void> {
  try {
    const db = await initDB();
    return new Promise((resolve, reject) => {
      const tx = db.transaction('transactions', 'readwrite');
      const store = tx.objectStore('transactions');

      const now = new Date();
      const dateStr = `${now.getFullYear()}.${String(now.getMonth() + 1).padStart(2, '0')}.${String(now.getDate()).padStart(2, '0')} ${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`;

      const record = { title, amount, date: dateStr, timestamp: now.getTime(), type };

      const putReq = store.add(record);
      putReq.onsuccess = () => resolve();
      putReq.onerror = () => reject(putReq.error);
    });
  } catch (e) {
    console.error('Failed to record transaction in DB', e);
  }
}

// ==========================================
// Shared single-context WebGL white-removal processor
// ==========================================
const processedCache = new Map<string, Promise<string>>()

let glCanvas: HTMLCanvasElement | null = null
let glCtx: WebGLRenderingContext | null = null
let glTex: WebGLTexture | null = null
let glThresholdLoc: WebGLUniformLocation | null = null

const VS = `
attribute vec2 a_position;
attribute vec2 a_texCoord;
varying vec2 v_texCoord;
void main() {
  gl_Position = vec4(a_position, 0.0, 1.0);
  v_texCoord = a_texCoord;
}`

const FS = `
precision mediump float;
uniform sampler2D u_image;
uniform float u_threshold;
varying vec2 v_texCoord;
void main() {
  vec4 color = texture2D(u_image, v_texCoord);
  if (color.r > u_threshold && color.g > u_threshold && color.b > u_threshold) {
    discard;
  }
  float brightness = (color.r + color.g + color.b) / 3.0;
  float a = color.a;
  if (brightness > u_threshold - 0.08) {
    a *= clamp((u_threshold - brightness) / 0.08, 0.0, 1.0);
  }
  gl_FragColor = vec4(color.rgb * a, a);
}`

function compile(gl: WebGLRenderingContext, type: number, src: string) {
  const s = gl.createShader(type)!
  gl.shaderSource(s, src)
  gl.compileShader(s)
  return s
}

function ensureGL(): WebGLRenderingContext | null {
  if (glCtx) return glCtx
  if (typeof document === 'undefined') return null

  const canvas = document.createElement('canvas')
  const gl = canvas.getContext('webgl', {
    premultipliedAlpha: true,
    alpha: true,
    preserveDrawingBuffer: true,
    antialias: false,
  }) as WebGLRenderingContext | null
  if (!gl) return null

  const program = gl.createProgram()!
  gl.attachShader(program, compile(gl, gl.VERTEX_SHADER, VS))
  gl.attachShader(program, compile(gl, gl.FRAGMENT_SHADER, FS))
  gl.linkProgram(program)
  gl.useProgram(program)

  const posBuf = gl.createBuffer()
  gl.bindBuffer(gl.ARRAY_BUFFER, posBuf)
  gl.bufferData(
    gl.ARRAY_BUFFER,
    new Float32Array([-1, -1, 1, -1, -1, 1, -1, 1, 1, -1, 1, 1]),
    gl.STATIC_DRAW,
  )
  const posLoc = gl.getAttribLocation(program, 'a_position')
  gl.enableVertexAttribArray(posLoc)
  gl.vertexAttribPointer(posLoc, 2, gl.FLOAT, false, 0, 0)

  const texBuf = gl.createBuffer()
  gl.bindBuffer(gl.ARRAY_BUFFER, texBuf)
  gl.bufferData(
    gl.ARRAY_BUFFER,
    new Float32Array([0, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 0]),
    gl.STATIC_DRAW,
  )
  const texLoc = gl.getAttribLocation(program, 'a_texCoord')
  gl.enableVertexAttribArray(texLoc)
  gl.vertexAttribPointer(texLoc, 2, gl.FLOAT, false, 0, 0)

  const tex = gl.createTexture()
  gl.bindTexture(gl.TEXTURE_2D, tex)
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE)
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE)
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR)
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR)
  gl.pixelStorei(gl.UNPACK_PREMULTIPLY_ALPHA_WEBGL, false)

  glCanvas = canvas
  glCtx = gl
  glTex = tex
  glThresholdLoc = gl.getUniformLocation(program, 'u_threshold')
  return gl
}

function processImage(src: string, threshold: number): Promise<string> {
  const key = `${src}|${threshold}`
  const hit = processedCache.get(key)
  if (hit) return hit

  const p = new Promise<string>((resolve, reject) => {
    const gl = ensureGL()
    if (!gl || !glCanvas) return reject(new Error('no webgl'))

    const img = new Image()
    img.crossOrigin = 'anonymous'
    img.onload = () => {
      try {
        glCanvas!.width = img.width || 120
        glCanvas!.height = img.height || 120
        gl.viewport(0, 0, glCanvas!.width, glCanvas!.height)

        gl.bindTexture(gl.TEXTURE_2D, glTex)
        gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, img)
        gl.uniform1f(glThresholdLoc, threshold)

        gl.clearColor(0, 0, 0, 0)
        gl.clear(gl.COLOR_BUFFER_BIT)
        gl.drawArrays(gl.TRIANGLES, 0, 6)

        resolve(glCanvas!.toDataURL('image/png'))
      } catch (e) {
        reject(e)
      }
    }
    img.onerror = reject
    img.src = src
  })

  processedCache.set(key, p)
  return p
}

function WhiteColorRemovalShader({
  imageSrc,
  className = '',
  threshold = 0.88,
  alt = '',
}: {
  imageSrc: string
  className?: string
  threshold?: number
  alt?: string
}) {
  const [url, setUrl] = useState<string | null>(null)

  useEffect(() => {
    let alive = true
    processImage(imageSrc, threshold)
      .then((u) => alive && setUrl(u))
      .catch(() => alive && setUrl(imageSrc))
    return () => {
      alive = false
    }
  }, [imageSrc, threshold])

  if (!url) return <div className={className} aria-hidden />
  return <img src={url} alt={alt} className={className} draggable={false} />
}

// ==========================================
// Details Page — split by type (Coins / Diamonds)
// History Limit & 24hr Reset Logic
// ==========================================
function DetailsPage({
  onBack,
  type,
}: {
  onBack: () => void;
  type: 'coin' | 'diamond';
}) {
  const [transactions, setTransactions] = useState<any[]>([]);

  useEffect(() => {
    let isMounted = true;
    const fetchTransactions = async () => {
      try {
        const db = await initDB();
        const tx = db.transaction('transactions', 'readwrite');
        const store = tx.objectStore('transactions');
        const req = store.getAll();
        
        req.onsuccess = () => {
          const now = Date.now();
          const cutoffTime = now - 24 * 60 * 60 * 1000;
          const allData = req.result || [];

          allData.forEach((t: any) => {
            if (t.timestamp < cutoffTime && t.id) {
              store.delete(t.id);
            }
          });

          if (isMounted) {
            let validData = allData.filter((t: any) => t.timestamp >= cutoffTime && (t.type ?? 'coin') === type);
            validData.sort((a: any, b: any) => b.timestamp - a.timestamp);
            validData = validData.slice(0, 20);
            setTransactions(validData);
          }
        };
      } catch (e) {
        console.error('Failed to load transactions', e);
      }
    };
    fetchTransactions();
    return () => { isMounted = false; };
  }, [type]);

  const valueColor = type === 'diamond' ? 'text-blue-500' : 'text-amber-500';

  return (
    <div className="fixed inset-0 h-[100dvh] w-full overflow-hidden flex flex-col bg-white pt-[calc(env(safe-area-inset-top,0px)+24px)] pb-[env(safe-area-inset-bottom,12px)]">
      <div className="w-full relative flex-shrink-0 flex items-center justify-between pl-1 pr-4 z-20 h-12 bg-white">
        <button
          onClick={onBack}
          className="w-10 h-10 flex items-center justify-center active:scale-90 transition-all text-gray-900 ml-1"
          aria-label="Back"
        >
          <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
            <line x1="19" y1="12" x2="5" y2="12" />
            <polyline points="12 19 5 12 12 5" />
          </svg>
        </button>
        <h1 className="text-base font-bold text-gray-950 tracking-tight absolute left-1/2 -translate-x-1/2">
          Details
        </h1>
        <div className="w-10 h-10" />
      </div>

      <div className="flex-1 overflow-y-auto px-4 pt-2 pb-6">
        <div className="flex flex-col">
          {transactions.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-10 text-gray-400 text-sm font-medium">
              No recent history
            </div>
          ) : (
            transactions.map((tx, index) => (
              <div
                key={tx.id}
                className={`flex justify-between items-start py-4 ${
                  index !== transactions.length - 1 ? 'border-b border-gray-100' : ''
                }`}
              >
                <div className="flex flex-col gap-1">
                  <span className="text-[15px] font-semibold text-gray-900">{tx.title}</span>
                  <span className="text-[13px] text-gray-400">{tx.date}</span>
                </div>
                <span className={`text-[15px] font-bold ${valueColor}`}>
                  {tx.amount > 0 ? `+${tx.amount.toLocaleString()}` : tx.amount.toLocaleString()}
                </span>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  )
}

// ==========================================
// Main Wallet Component
// ==========================================
type TabType = 'wallet' | 'diamonds' | 'agent';

interface WalletProps {
  onBack: () => void
  initialTab?: TabType
}

// Reusable Original Google G Logo SVG
const GPaySvg = () => (
  <svg viewBox="0 0 24 24" width="100%" height="100%" xmlns="http://www.w3.org/2000/svg">
    <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4"/>
    <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/>
    <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05"/>
    <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335"/>
  </svg>
)

// Reusable PhonePe Logo (proper squircle with white "पे")
const PhonePeSvg = ({ size = 'md' }: { size?: 'sm' | 'md' }) => {
  const textSize = size === 'sm' ? 'text-[11px]' : 'text-[26px]'
  const radius = size === 'sm' ? 'rounded-[4px]' : 'rounded-[12px]'
  return (
    <div className={`w-full h-full bg-[#5f259f] ${radius} flex items-center justify-center`}>
      <span
        className={`text-white font-bold ${textSize} leading-none`}
        style={{ fontFamily: 'sans-serif', paddingBottom: size === 'sm' ? '1px' : '3px' }}
      >
        पे
      </span>
    </div>
  )
}

const TABS: TabType[] = ['wallet', 'diamonds', 'agent'];

export default function Wallet({ onBack, initialTab = 'wallet' }: WalletProps) {
  const [activeTab, setActiveTab] = useState<TabType>(initialTab)
  
  const [diamonds, setDiamonds] = useState('')
  const [coins, setCoins] = useState('')
  const [selectedPercentage, setSelectedPercentage] = useState('100%')
  const [showDetails, setShowDetails] = useState<'coin' | 'diamond' | null>(null)

  const [walletBalance, setWalletBalance] = useState<number | null>(null)
  const [diamondBalance, setDiamondBalance] = useState<number | null>(null)

  // Sheet States
  const [showPaymentSheet, setShowPaymentSheet] = useState(false)
  const [showPayUsingSheet, setShowPayUsingSheet] = useState(false)
  const [selectedAmountToBuy, setSelectedAmountToBuy] = useState<{coins: number, price: number} | null>(null)

  // Swipe support
  const touchStartX = useRef<number | null>(null)
  const touchStartY = useRef<number | null>(null)
  const isSwiping = useRef(false)

  useEffect(() => {
    let isMounted = true

    const fetchBalance = async () => {
      const bal = await loadBalanceFromDB()
      const diamondBal = await loadDiamondBalanceFromDB()
      if (isMounted) {
        setWalletBalance(bal)
        setDiamondBalance(diamondBal)
      }
    }

    fetchBalance()
    const intervalId = setInterval(fetchBalance, 1000)

    return () => {
      isMounted = false
      clearInterval(intervalId)
    }
  }, [])

  // Reset activeTab if initialTab changes
  useEffect(() => {
    setActiveTab(initialTab)
  }, [initialTab])

  const goToTab = (tab: TabType) => {
    setActiveTab(tab)
  }

  const handleTouchStart = (e: React.TouchEvent) => {
    // Ignore if touch starts inside an input, button, or scrollable input area
    const target = e.target as HTMLElement
    if (
      target.closest('input') ||
      target.closest('button') ||
      target.closest('select') ||
      target.closest('textarea')
    ) {
      touchStartX.current = null
      touchStartY.current = null
      return
    }
    touchStartX.current = e.touches[0].clientX
    touchStartY.current = e.touches[0].clientY
    isSwiping.current = false
  }

  const handleTouchMove = (e: React.TouchEvent) => {
    if (touchStartX.current === null || touchStartY.current === null) return
    const deltaX = e.touches[0].clientX - touchStartX.current
    const deltaY = e.touches[0].clientY - touchStartY.current
    if (Math.abs(deltaX) > Math.abs(deltaY) && Math.abs(deltaX) > 10) {
      isSwiping.current = true
    }
  }

  const handleTouchEnd = (e: React.TouchEvent) => {
    if (touchStartX.current === null || touchStartY.current === null) return
    const deltaX = e.changedTouches[0].clientX - touchStartX.current
    const deltaY = e.changedTouches[0].clientY - touchStartY.current
    touchStartX.current = null
    touchStartY.current = null
    isSwiping.current = false

    // Only trigger on mostly-horizontal swipes
    if (Math.abs(deltaX) < 50 || Math.abs(deltaX) < Math.abs(deltaY)) return

    const currentIndex = TABS.indexOf(activeTab)
    if (deltaX < 0) {
      // swipe left → next tab
      const nextIndex = Math.min(currentIndex + 1, TABS.length - 1)
      if (nextIndex !== currentIndex) goToTab(TABS[nextIndex])
    } else {
      // swipe right → previous tab
      const prevIndex = Math.max(currentIndex - 1, 0)
      if (prevIndex !== currentIndex) goToTab(TABS[prevIndex])
    }
  }

  const handleDiamondChange = (value: string) => {
    setDiamonds(value)
    const diamondNum = parseFloat(value) || 0
    const coinValue = ((diamondNum * 33) / 100).toFixed(0)
    setCoins(coinValue)
  }

  const handleCoinChange = (value: string) => {
    setCoins(value)
    const coinNum = parseFloat(value) || 0
    const diamondValue = ((coinNum * 100) / 33).toFixed(0)
    setDiamonds(diamondValue)
  }

  const handlePercentageSelect = (pct: string) => {
    setSelectedPercentage(pct)
    const percentage = parseFloat(pct) || 0
    const availableDiamonds = diamondBalance ?? 0
    const selectedDiamonds = Math.floor((availableDiamonds * percentage) / 100)
    const selectedCoins = Math.floor((selectedDiamonds * 33) / 100)

    setDiamonds(selectedDiamonds > 0 ? String(selectedDiamonds) : '')
    setCoins(selectedCoins > 0 ? String(selectedCoins) : '')
  }

  const executeBuyCoins = async () => {
    if(!selectedAmountToBuy) return;
    const amount = selectedAmountToBuy.coins;
    
    setWalletBalance((prev) => (prev ?? 0) + amount)
    await addCoinsToDB(amount)
    await recordTransaction('Buy Coins', amount, 'coin')
    setWalletBalance(await loadBalanceFromDB())

    setShowPayUsingSheet(false)
    setShowPaymentSheet(false)
    setSelectedAmountToBuy(null)
  }

  const handleExchange = async () => {
    const diamondNum = parseFloat(diamonds) || 0
    const coinNum = parseFloat(coins) || 0
    if (diamondNum <= 0 || coinNum <= 0) return

    const availableDiamonds = await loadDiamondBalanceFromDB()
    if (diamondNum > availableDiamonds) return

    const removed = await subtractDiamondsFromDB(diamondNum)
    if (!removed) return

    await recordTransaction('Diamond Exchange', -diamondNum, 'diamond')
    await addCoinsToDB(coinNum)

    setDiamonds('')
    setCoins('')
    setWalletBalance(await loadBalanceFromDB())
    setDiamondBalance(await loadDiamondBalanceFromDB())
  }

  if (showDetails) {
    return <DetailsPage type={showDetails} onBack={() => setShowDetails(null)} />
  }

  return (
    <div
      className="fixed inset-0 h-[100dvh] w-full overflow-hidden flex flex-col pt-[calc(env(safe-area-inset-top,0px)+24px)] pb-[env(safe-area-inset-bottom,12px)] transition-all duration-300 relative"
      style={{
        touchAction: 'pan-y',
        WebkitUserSelect: 'none',
        userSelect: 'none',
        WebkitTouchCallout: 'none',
        background: 'linear-gradient(180deg, #1A66FF 0%, #1A66FF 15vh, #F3F4F6 30vh, #F3F4F6 100%)',
      }}
      onTouchStart={handleTouchStart}
      onTouchMove={handleTouchMove}
      onTouchEnd={handleTouchEnd}
    >
      <style>{`
        * {
          -webkit-text-size-adjust: 100%;
          -ms-text-size-adjust: 100%;
        }
      `}</style>

      {/* TOP HEADER */}
      <div className="w-full relative flex-shrink-0 flex items-center justify-between pl-1 pr-4 z-20 h-12">
        <button
          onClick={onBack}
          className="w-10 h-10 flex items-center justify-center active:scale-90 transition-all text-white ml-1"
          aria-label="Back"
        >
          <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
            <line x1="19" y1="12" x2="5" y2="12" />
            <polyline points="12 19 5 12 12 5" />
          </svg>
        </button>

        <h1 className="text-[17px] font-bold text-white tracking-tight">
          Recharge
        </h1>

        <button
          onClick={() => setShowDetails(activeTab === 'wallet' ? 'coin' : 'diamond')}
          className="w-10 h-10 flex items-center justify-center active:scale-90 transition-all text-white"
          aria-label="History"
        >
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
            <polyline points="14 2 14 8 20 8" />
            <line x1="16" y1="13" x2="8" y2="13" />
            <line x1="16" y1="17" x2="8" y2="17" />
            <polyline points="10 9 9 9 8 9" />
          </svg>
        </button>
      </div>

      {/* SCROLLABLE BODY */}
      <div className="flex-1 overflow-y-auto px-4 pt-1 pb-6 relative z-10">
        
        {/* Dynamic Banners - Coins Banner for Wallet AND Agent */}
        {(activeTab === 'wallet' || activeTab === 'agent') && (
          <div className="relative w-[calc(100%+2rem)] -mx-4 overflow-hidden mt-0 shadow-none border-0 outline-none">
            <img
              src="/file_00000000f3d88211964f0057da4bc797.png"
              alt="Coins Banner Background"
              className="w-full h-auto block"
              draggable={false}
            />
            <div className="absolute inset-0 pl-10 pt-8 pr-4 flex flex-col justify-start">
              <span className="text-gray-200 font-medium text-sm">
                My Coins
              </span>
              <div className="flex items-center gap-2 mt-0.5">
                <span className="text-[22px] font-bold text-yellow-400 tracking-tight drop-shadow-sm">
                  {walletBalance === null ? '—' : walletBalance.toLocaleString()}
                </span>
                <img
                  src="/file_00000000e56882119c217d508b6733dc.png"
                  className="w-5 h-5 object-contain drop-shadow-sm"
                  alt="coin icon"
                  draggable={false}
                />
              </div>
            </div>
          </div>
        )}

        {/* Diamonds Banner — slightly reduced width */}
        {activeTab === 'diamonds' && (
          <div className="relative w-[calc(100%+1rem)] -mx-2 overflow-hidden mt-0 shadow-none border-0 outline-none">
            <img
              src="/file_0000000085a482088fb089cb76f3d1af.png"
              alt="Diamonds Banner Background"
              className="w-full h-auto block"
              draggable={false}
            />
            <div className="absolute inset-0 pl-10 pt-8 pr-4 flex flex-col justify-start">
              <span className="text-gray-200 font-medium text-sm">
                My Diamonds
              </span>
              <div className="flex items-center gap-2 mt-0.5">
                <span className="text-[22px] font-bold text-yellow-400 tracking-tight drop-shadow-sm">
                  {diamondBalance === null ? '—' : diamondBalance.toLocaleString()}
                </span>
                <WhiteColorRemovalShader
                  imageSrc="/1787321690452.png"
                  className="w-5 h-5 object-contain"
                  threshold={0.88}
                />
              </div>
            </div>
          </div>
        )}

        {/* Pill Tabs — BLACK bg + BLACK text */}
        <div className="bg-black/20 p-1 mt-5 rounded-full flex relative items-center">
          {[
            { id: 'wallet', label: 'Coins' },
            { id: 'diamonds', label: 'Diamonds' },
            { id: 'agent', label: 'Agent' }
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id as TabType)}
              className={`flex-1 py-2.5 rounded-full text-[13px] font-bold transition-all whitespace-nowrap ${
                activeTab === tab.id 
                  ? 'bg-white text-black shadow-sm' 
                  : 'text-black'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {/* TAB SPECIFIC CONTENT */}
        {activeTab === 'wallet' && (
          <div className="flex flex-col mt-5">
            <div className="flex justify-start">
              <button
                onClick={() => {
                  setSelectedAmountToBuy({ coins: 1030000, price: 100 });
                  setShowPaymentSheet(true);
                }}
                className="w-[120px] aspect-square bg-white rounded-md flex flex-col items-center justify-center outline-none active:scale-95 transition-transform"
              >
                <img
                  src="/file_00000000e56882119c217d508b6733dc.png"
                  className="w-8 h-8 object-contain mb-1.5"
                  alt="coin"
                  draggable={false}
                />
                <span className="text-gray-900 font-bold text-[17px] leading-none mb-2">
                  1,000,000
                </span>
                <div className="bg-red-500 text-white text-[10px] font-bold px-2 py-0.5 rounded-[4px] mb-2 leading-none shadow-sm">
                  +Bounce 30,000
                </div>
                <span className="text-gray-500 font-medium text-[14px] leading-none">
                  ₹ 100
                </span>
              </button>
            </div>
          </div>
        )}

        {/* Agent Specific Content */}
        {activeTab === 'agent' && (
          <div className="flex flex-col mt-5">
            <div className="w-full bg-transparent p-2 flex items-center justify-between border-0 shadow-none">
              <div className="flex items-center gap-3">
                <div className="w-12 h-12 rounded-full overflow-hidden bg-gray-200 border-2 border-white flex items-center justify-center flex-shrink-0 text-gray-500 shadow-sm">
                  <svg viewBox="0 0 36 36" fill="none" xmlns="http://www.w3.org/2000/svg" className="w-full h-full">
                    <circle cx="18" cy="12" r="6" fill="currentColor"/>
                    <path d="M18 20C11.3726 20 6 25.3726 6 32H30C30 25.3726 24.6274 20 18 20Z" fill="currentColor"/>
                  </svg>
                </div>
                <span className="text-gray-900 font-bold text-[17px]">
                  Agent Anmol
                </span>
              </div>
              
              {/* Chat icon — blue circle with white circular chat icon */}
              <button
                className="flex-shrink-0 w-11 h-11 rounded-full flex items-center justify-center active:scale-90 transition-transform"
              >
                <svg
                  width="32"
                  height="32"
                  viewBox="0 0 48 48"
                  fill="none"
                  xmlns="http://www.w3.org/2000/svg"
                >
                  {/* Blue outer circle */}
                  <circle
                    cx="24"
                    cy="24"
                    r="23"
                    fill="#0044FF"
                  />

                  {/* White circular chat bubble */}
                  <circle
                    cx="24"
                    cy="24"
                    r="14"
                    fill="white"
                  />

                  {/* Small chat-tail */}
                  <path
                    d="M14 32L13 38L19 34"
                    fill="white"
                  />

                  {/* Three blue dots */}
                  <circle
                    cx="18"
                    cy="24"
                    r="2"
                    fill="#0044FF"
                  />

                  <circle
                    cx="24"
                    cy="24"
                    r="2"
                    fill="#0044FF"
                  />

                  <circle
                    cx="30"
                    cy="24"
                    r="2"
                    fill="#0044FF"
                  />
                </svg>
              </button>
            </div>
          </div>
        )}

        {activeTab === 'diamonds' && (
          <div className="flex flex-col mt-5 space-y-4">
            <div
              className="rounded-xl p-4"
              style={{
                background: 'linear-gradient(180deg, #F0F7FF 0%, #FFFFFF 100%)',
                border: '1px solid #E0EFFF',
                boxShadow: '0 2px 10px rgba(59, 130, 246, 0.1)',
              }}
            >
              <div className="flex justify-between items-center mb-3">
                <h3 className="text-xs font-bold text-gray-800">Exchange</h3>
                <div className="text-[11px] font-semibold text-gray-500 flex items-center gap-1">
                  <span>100 =</span>
                  <div className="w-3.5 h-3.5 inline-block align-middle">
                    <img
                      src="/file_00000000e56882119c217d508b6733dc.png"
                      className="w-full h-full object-contain"
                      alt=""
                      draggable={false}
                    />
                  </div>
                  <span>33</span>
                </div>
              </div>

              <div className="flex items-center gap-2">
                <div className="flex-1 bg-gray-50/80 rounded-xl p-2.5 flex items-center gap-2 border border-blue-100 shadow-inner">
                  <div className="w-4 h-4 flex-shrink-0">
                    <WhiteColorRemovalShader
                      imageSrc="/1787321690452.png"
                      className="w-full h-full object-contain"
                      threshold={0.88}
                    />
                  </div>
                  <input
                    type="number"
                    value={diamonds}
                    onChange={(e) => handleDiamondChange(e.target.value)}
                    className="bg-transparent outline-none w-full font-medium text-gray-700 text-xs placeholder:text-gray-400"
                    placeholder="Input multiple"
                  />
                  <span className="text-[11px] font-bold text-gray-400">x100</span>
                </div>

                <span className="text-gray-300 font-bold">=</span>

                <div className="flex-1 bg-gray-50/80 rounded-xl p-2.5 flex items-center justify-between border border-gray-200 shadow-inner">
                  <input
                    type="number"
                    value={coins}
                    onChange={(e) => handleCoinChange(e.target.value)}
                    className="bg-transparent outline-none w-full font-medium text-gray-700 text-xs text-right placeholder:text-gray-400"
                    placeholder="Coins"
                  />
                  <div className="w-4 h-4 flex-shrink-0 ml-1.5">
                    <img
                      src="/file_00000000e56882119c217d508b6733dc.png"
                      className="w-full h-full object-contain"
                      alt=""
                      draggable={false}
                    />
                  </div>
                </div>
              </div>
            </div>

            <div className="space-y-2 pt-1">
              <h4 className="text-[11px] font-bold text-gray-500">exchange rate</h4>
              <div className="grid grid-cols-3 gap-2">
                {['20%', '40%', '60%', '80%', '100%'].map((pct) => (
                  <button
                    key={pct}
                    onClick={() => handlePercentageSelect(pct)}
                    className={`py-2 rounded-xl text-xs font-bold transition-all border ${
                      selectedPercentage === pct
                        ? 'bg-[#0044FF] text-white border-[#0044FF] shadow-xs'
                        : 'bg-white text-[#0044FF] border-blue-200'
                    }`}
                  >
                    {pct}
                  </button>
                ))}
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Exchange Button — fixed to bottom, only shown on diamonds tab */}
      {activeTab === 'diamonds' && (
        <div className="flex-shrink-0 w-full px-4 pb-2 pt-2 z-20 flex justify-center">
          <button
            onClick={handleExchange}
            className="w-[75%] py-4 rounded-full font-bold text-white bg-[#0044FF] hover:bg-blue-700 text-[15px] shadow-md active:scale-95 transition-transform"
          >
            Exchange
          </button>
        </div>
      )}

      {/* OVERLAY FOR SHEETS */}
      {(showPaymentSheet || showPayUsingSheet) && (
        <div 
          className="fixed inset-0 bg-black/40 z-40 transition-opacity"
          onClick={() => {
            setShowPaymentSheet(false);
            setShowPayUsingSheet(false);
          }}
        />
      )}

      {/* ================================================== */}
      {/* 1. PAYMENT METHOD SHEET */}
      {/* ================================================== */}
      <div 
        className={`fixed bottom-0 left-0 right-0 bg-white rounded-t-3xl z-50 transition-transform duration-300 ease-out flex flex-col`}
        style={{
          transform: showPaymentSheet ? 'translateY(0)' : 'translateY(100%)',
          minHeight: '35vh' 
        }}
      >
        <div className="flex items-center justify-center py-4 relative border-b border-gray-100">
          <h2 className="text-lg font-bold text-gray-900">Payment Method</h2>
          
          <div className="absolute right-5 flex items-center gap-1.5 bg-gray-100 px-2 py-1 rounded-md">
            <span className="text-[12px]">🇮🇳</span>
            <span className="text-[13px] font-medium text-gray-700">in</span>
          </div>
        </div>

        <div className="px-5 py-4 flex-1">
          <div className="bg-[#F8F9FA] rounded-xl p-4 flex justify-between items-center mb-6">
            <div className="flex flex-col gap-1">
              <span className="text-[13px] text-gray-500 font-medium">Coins</span>
              <div className="flex items-center gap-1.5">
                <img src="/file_00000000e56882119c217d508b6733dc.png" className="w-5 h-5 object-contain" alt="" />
                <span className="text-lg font-bold text-gray-900">
                  {selectedAmountToBuy?.coins.toLocaleString()}
                </span>
              </div>
            </div>
            <div className="flex flex-col items-end gap-1">
              <span className="text-[13px] text-gray-500 font-medium">Price</span>
              <span className="text-lg font-bold text-gray-900">
                ₹{selectedAmountToBuy?.price.toLocaleString('en-IN', {minimumFractionDigits: 2})}
              </span>
            </div>
          </div>

          <div className="flex flex-col gap-3 mb-6">
            <span className="text-[13px] text-gray-500 font-medium">Select payment method</span>
            
            <div className="flex items-center justify-between">
              
              {/* Updated Logo Presentation Line */}
              <div className="flex items-center gap-2.5">
                 <span className="font-black italic text-gray-800 text-lg tracking-tighter mr-1">UPI</span>
                 
                 {/* Google G Logo */}
                 <div className="w-[18px] h-[18px] flex-shrink-0">
                    <GPaySvg />
                 </div>
                 
                 {/* PhonePe Logo — fixed */}
                 <div className="w-[18px] h-[18px] flex-shrink-0">
                    <PhonePeSvg size="sm" />
                 </div>
                 
                 {/* Paytm Exact Style Text */}
                 <div className="flex font-black text-[13px] italic tracking-tight">
                    <span className="text-[#002970]">Pay</span>
                    <span className="text-[#00BAF2]">tm</span>
                 </div>
              </div>

              <div className="flex items-center gap-3">
                <div className="flex items-center gap-1">
                  <img src="/file_00000000e56882119c217d508b6733dc.png" className="w-4 h-4 object-contain" alt="" />
                  <span className="text-sm font-semibold text-gray-900">
                    {selectedAmountToBuy?.coins.toLocaleString()}
                  </span>
                </div>
                <div className="w-5 h-5 rounded-full border-2 border-green-500 flex items-center justify-center">
                  <div className="w-2.5 h-2.5 bg-green-500 rounded-full"></div>
                </div>
              </div>
            </div>
          </div>

          <button
            onClick={() => {
              setShowPaymentSheet(false);
              setTimeout(() => setShowPayUsingSheet(true), 300);
            }}
            className="w-full bg-[#0044FF] text-white font-bold text-[16px] py-3.5 rounded-xl active:scale-95 transition-transform shadow-md"
          >
            Recharge
          </button>
        </div>
      </div>

      {/* ================================================== */}
      {/* 2. PAY USING SHEET */}
      {/* ================================================== */}
      <div 
        className={`fixed bottom-0 left-0 right-0 bg-white rounded-t-3xl z-50 transition-transform duration-300 ease-out flex flex-col`}
        style={{
          transform: showPayUsingSheet ? 'translateY(0)' : 'translateY(100%)',
          minHeight: '40vh' 
        }}
      >
        <div className="flex items-center justify-center py-4 relative border-b border-gray-100">
          <h2 className="text-[17px] font-bold text-gray-900">Pay Using</h2>
          <button 
            onClick={() => setShowPayUsingSheet(false)}
            className="absolute right-4 p-2"
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </button>
        </div>

        <div className="px-5 py-2 flex-1 flex flex-col">
          {[
            { 
              id: 'gpay', 
              name: 'GPay', 
              icon: <div className="w-[22px] h-[22px]"><GPaySvg /></div>, 
              bg: 'bg-white border border-gray-200',
              shape: 'rounded-full'
            },
            { 
              id: 'phonepe', 
              name: 'PhonePe', 
              icon: <PhonePeSvg size="md" />, 
              bg: '',
              shape: 'rounded-full'
            },
            { 
              id: 'paytm', 
              name: 'Paytm', 
              icon: (
                <div className="flex font-black text-[15px] italic tracking-tight">
                  <span className="text-[#002970]">Pay</span>
                  <span className="text-[#00BAF2]">tm</span>
                </div>
              ), 
              bg: 'bg-white border border-gray-200',
              shape: 'rounded-full'
            },
            { 
              id: 'other', 
              name: 'Other', 
              icon: <span className="font-black text-gray-400 tracking-widest leading-none mb-2">...</span>, 
              bg: 'bg-gray-100',
              shape: 'rounded-full'
            },
          ].map((app, idx) => (
            <button
              key={app.id}
              onClick={executeBuyCoins} 
              className={`flex items-center gap-4 py-4 w-full text-left active:bg-gray-50 transition-colors ${
                idx !== 3 ? 'border-b border-gray-50' : ''
              }`}
            >
              <div className={`w-11 h-11 ${app.shape} flex items-center justify-center flex-shrink-0 shadow-sm overflow-hidden ${app.bg}`}>
                {app.icon}
              </div>
              <span className="text-[16px] font-medium text-gray-900">{app.name}</span>
            </button>
          ))}
        </div>
      </div>
      
    </div>
  )
}
