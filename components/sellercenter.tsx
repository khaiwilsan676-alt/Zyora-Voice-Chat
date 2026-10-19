'use client';

import React, { useEffect, useState } from 'react';

interface SellerCenterProps {
  onBack?: () => void;
}

// ==========================================
// SHARED WALLET DB (Same as Wallet / WildParty / GiftPicker / Store)
// ==========================================
const SHARED_DB = 'FruitPartyDB';
const SHARED_STORE = 'GameState';
const DEFAULT_BALANCE = 0;

const initWalletDB = (): Promise<IDBDatabase> =>
  new Promise((resolve, reject) => {
    if (typeof window === 'undefined') return reject('No window');
    const request = indexedDB.open(SHARED_DB, 3);
    request.onupgradeneeded = (e: any) => {
      const db = e.target.result;
      if (!db.objectStoreNames.contains(SHARED_STORE)) {
        db.createObjectStore(SHARED_STORE);
      }
    };
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });

const loadWalletBalance = async (): Promise<number> => {
  try {
    const db = await initWalletDB();
    return new Promise((resolve) => {
      const tx = db.transaction(SHARED_STORE, 'readonly');
      const req = tx.objectStore(SHARED_STORE).get('user_data');
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
};

// delta positive = add, negative = deduct
const updateWalletBalance = async (delta: number): Promise<void> => {
  try {
    const db = await initWalletDB();
    return new Promise((resolve, reject) => {
      const tx = db.transaction(SHARED_STORE, 'readwrite');
      const store = tx.objectStore(SHARED_STORE);
      const req = store.get('user_data');
      req.onsuccess = () => {
        const data = req.result;
        const current = data?.balance ?? DEFAULT_BALANCE;
        const next = Math.max(0, current + delta);
        const putReq = store.put({ ...(data || {}), balance: next }, 'user_data');
        putReq.onsuccess = () => resolve();
        putReq.onerror = () => reject(putReq.error);
      };
      req.onerror = () => reject(req.error);
    });
  } catch (e) {
    console.error('Wallet update failed', e);
  }
};

// ==========================================
// Shared WebGL white-removal (single context, cached)
// ==========================================
const processedCache = new Map<string, Promise<string>>();

let glCanvas: HTMLCanvasElement | null = null;
let glCtx: WebGLRenderingContext | null = null;
let glTex: WebGLTexture | null = null;

const VS = `
attribute vec2 a_position;
attribute vec2 a_texCoord;
varying vec2 v_texCoord;
void main() {
  gl_Position = vec4(a_position, 0.0, 1.0);
  v_texCoord = a_texCoord;
}`;

const FS = `
precision mediump float;
uniform sampler2D u_image;
varying vec2 v_texCoord;
void main() {
  vec4 color = texture2D(u_image, v_texCoord);
  if (color.r > 0.9 && color.g > 0.9 && color.b > 0.9) {
    gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0);
  } else {
    gl_FragColor = color;
  }
}`;

function compile(gl: WebGLRenderingContext, type: number, src: string) {
  const s = gl.createShader(type)!;
  gl.shaderSource(s, src);
  gl.compileShader(s);
  return s;
}

function ensureGL(): WebGLRenderingContext | null {
  if (glCtx) return glCtx;
  if (typeof document === 'undefined') return null;

  const canvas = document.createElement('canvas');
  const gl = canvas.getContext('webgl', {
    premultipliedAlpha: false,
    alpha: true,
    preserveDrawingBuffer: true,
    antialias: false,
  }) as WebGLRenderingContext | null;
  if (!gl) return null;

  const program = gl.createProgram()!;
  gl.attachShader(program, compile(gl, gl.VERTEX_SHADER, VS));
  gl.attachShader(program, compile(gl, gl.FRAGMENT_SHADER, FS));
  gl.linkProgram(program);
  gl.useProgram(program);

  const posBuf = gl.createBuffer();
  gl.bindBuffer(gl.ARRAY_BUFFER, posBuf);
  gl.bufferData(
    gl.ARRAY_BUFFER,
    new Float32Array([-1, -1, 1, -1, -1, 1, -1, 1, 1, -1, 1, 1]),
    gl.STATIC_DRAW
  );
  const posLoc = gl.getAttribLocation(program, 'a_position');
  gl.enableVertexAttribArray(posLoc);
  gl.vertexAttribPointer(posLoc, 2, gl.FLOAT, false, 0, 0);

  const texBuf = gl.createBuffer();
  gl.bindBuffer(gl.ARRAY_BUFFER, texBuf);
  gl.bufferData(
    gl.ARRAY_BUFFER,
    new Float32Array([0, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 0]),
    gl.STATIC_DRAW
  );
  const texLoc = gl.getAttribLocation(program, 'a_texCoord');
  gl.enableVertexAttribArray(texLoc);
  gl.vertexAttribPointer(texLoc, 2, gl.FLOAT, false, 0, 0);

  const tex = gl.createTexture();
  gl.bindTexture(gl.TEXTURE_2D, tex);
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR);
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR);

  glCanvas = canvas;
  glCtx = gl;
  glTex = tex;
  return gl;
}

function processImage(src: string): Promise<string> {
  const hit = processedCache.get(src);
  if (hit) return hit;

  const p = new Promise<string>((resolve, reject) => {
    const gl = ensureGL();
    if (!gl || !glCanvas) return reject(new Error('no webgl'));

    const img = new Image();
    img.crossOrigin = 'anonymous';
    img.onload = () => {
      try {
        glCanvas!.width = img.width || 120;
        glCanvas!.height = img.height || 120;
        gl.viewport(0, 0, glCanvas!.width, glCanvas!.height);

        gl.bindTexture(gl.TEXTURE_2D, glTex);
        gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, img);

        gl.clearColor(0, 0, 0, 0);
        gl.clear(gl.COLOR_BUFFER_BIT);
        gl.drawArrays(gl.TRIANGLES, 0, 6);

        resolve(glCanvas!.toDataURL('image/png'));
      } catch (e) {
        reject(e);
      }
    };
    img.onerror = reject;
    img.src = src;
  });

  processedCache.set(src, p);
  return p;
}

function CleanedCoinImage({ src, className = '' }: { src: string; className?: string }) {
  const [url, setUrl] = useState<string>(src);
  useEffect(() => {
    let alive = true;
    processImage(src)
      .then((u) => alive && setUrl(u))
      .catch(() => alive && setUrl(src));
    return () => {
      alive = false;
    };
  }, [src]);
  return <img src={url} alt="Coin" className={className} draggable={false} />;
}

// ==========================================
// Main Component
// ==========================================
export default function SellerCenter({ onBack }: SellerCenterProps) {
  const [currentView, setCurrentView] = useState<'seller' | 'record'>('seller');
  const [salesMethod, setSalesMethod] = useState<'user' | 'seller'>('user');

  // Shared wallet balance
  const [balance, setBalance] = useState<number>(0);

  // Transfer form
  const [targetId, setTargetId] = useState('');
  const [amount, setAmount] = useState('');
  const [transferring, setTransferring] = useState(false);

  // Real-time sync from shared wallet
  useEffect(() => {
    let alive = true;
    const sync = async () => {
      const bal = await loadWalletBalance();
      if (alive) setBalance(bal);
    };
    sync();
    const id = setInterval(sync, 1500);
    return () => {
      alive = false;
      clearInterval(id);
    };
  }, []);

  // Transfer handler — deducts from shared wallet
  const handleTransfer = async () => {
    if (transferring) return;

    const amt = parseInt(amount.replace(/,/g, ''), 10);

    if (!targetId.trim()) {
      alert('Please enter the ID');
      return;
    }
    if (!amt || amt <= 0) {
      alert('Please enter a valid amount');
      return;
    }
    if (balance < amt) {
      alert('Insufficient balance');
      return;
    }

    setTransferring(true);
    setBalance((b) => b - amt); // optimistic UI
    await updateWalletBalance(-amt);
    setTransferring(false);

    setTargetId('');
    setAmount('');
    alert(`Transferred ${amt.toLocaleString()} coins to ${targetId}`);
  };

  const amountNum = parseInt(amount, 10) || 0;
  const isTransferDisabled =
    transferring || !targetId.trim() || amountNum <= 0 || amountNum > balance;

  // Dummy transactions
  const transactions = [
    {
      id: 1,
      transferTo: '💫FAKE SMILE🦅',
      nameColor: 'text-yellow-500',
      date: '09/04/2026 19:16',
      amount: '-6,450,000',
      balance: '2,204,251,179',
      userId: '116943047',
    },
    {
      id: 2,
      transferTo: '🇮🇳Indian_Tiger🇮🇳',
      nameColor: 'text-green-500',
      date: '09/04/2026 19:10',
      amount: '-2,150,000',
      balance: '2,210,701,179',
      userId: '116943047',
    },
    {
      id: 3,
      transferTo: '🖤KING—⭐',
      nameColor: 'text-yellow-400',
      date: '09/04/2026 19:06',
      amount: '-2,150,000',
      balance: '2,212,851,179',
      userId: '116943047',
    },
    {
      id: 4,
      transferTo: 'Asael🖤',
      nameColor: 'text-yellow-500',
      date: '09/04/2026 18:52',
      amount: '-2,150,000',
      balance: '2,215,001,179',
      userId: '116943047',
    },
  ];

  // ================= RECORD VIEW =================
  if (currentView === 'record') {
    return (
      <div className="w-full min-h-screen bg-white font-sans text-gray-800 flex flex-col">
        <div className="bg-white sticky top-0 z-50">
          <div className="w-full h-[env(safe-area-inset-top)] bg-white"></div>
          <div className="flex items-center px-4 pt-[18px] pb-3">
            <button onClick={() => setCurrentView('seller')} className="p-1 cursor-pointer">
              <svg viewBox="0 0 24 24" className="w-6 h-6 stroke-black fill-none stroke-[2.5] stroke-linecap-round stroke-linejoin-round">
                <line x1="19" y1="12" x2="5" y2="12"></line>
                <polyline points="12 19 5 12 12 5"></polyline>
              </svg>
            </button>
            <h1 className="text-[17px] font-bold text-gray-800 tracking-wide flex-1 text-center pr-6">
              Details
            </h1>
          </div>
        </div>

        <div className="px-4 py-2">
          <div className="relative flex items-center">
            <input
              type="text"
              placeholder="Please input the user id"
              className="w-full bg-[#f2f2f2] rounded-full px-5 py-2.5 text-sm outline-none text-gray-800 placeholder-gray-500"
            />
            <svg viewBox="0 0 24 24" className="w-5 h-5 absolute right-4 stroke-gray-500 fill-none stroke-[2] stroke-linecap-round stroke-linejoin-round">
              <circle cx="11" cy="11" r="8"></circle>
              <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
            </svg>
          </div>
        </div>

        <div className="px-4 py-3 flex items-center">
          <span className="text-[15px] font-bold text-gray-800">Order Type</span>
          <span className="text-[15px] font-bold text-gray-800 ml-2 cursor-pointer flex items-center">
            All
            <svg viewBox="0 0 24 24" className="w-4 h-4 ml-0.5 fill-black">
              <path d="M7 10l5 5 5-5z"></path>
            </svg>
          </span>
        </div>

        <div className="flex flex-col px-4 pb-8">
          {transactions.map((tx) => (
            <div key={tx.id} className="flex justify-between items-start border-b border-gray-100 py-4">
              <div className="flex flex-col">
                <span className="inline-block bg-blue-100 text-blue-600 text-[10px] font-bold px-1.5 py-0.5 rounded w-max mb-3">
                  Transfer
                </span>
                <div className="flex items-center space-x-2 mb-1">
                  <span className="text-[14px] font-bold text-gray-800 w-16">To User</span>
                  <img src="https://i.pravatar.cc/150?u=nawab" alt="Profile" className="w-8 h-8 rounded-full border border-gray-200" />
                  <div className="flex flex-col">
                    <span className="text-[13px] font-bold text-gray-800">{tx.transferTo}</span>
                    <span className="text-[10px] text-gray-400 leading-tight">{tx.userId}</span>
                  </div>
                </div>
                <span className="text-[12px] text-gray-400 mt-1">{tx.date}</span>
              </div>

              <div className="flex items-center space-x-1 pt-8">
                <CleanedCoinImage src="/file_00000000e56882119c217d508b6733dc.png" className="w-4 h-4 object-contain" />
                <span className="text-[14px] font-bold text-gray-800">{tx.amount}</span>
              </div>
            </div>
          ))}
        </div>
      </div>
    );
  }

  // ================= SELLER CENTER VIEW =================
  return (
    <div className="w-full min-h-screen bg-white font-sans text-gray-800 flex flex-col">
      <div className="bg-white sticky top-0 z-50">
        <div className="w-full h-[env(safe-area-inset-top)] bg-white"></div>
        <div className="flex items-center px-4 pt-[18px] pb-3">
          <button onClick={onBack} className="p-1 cursor-pointer">
            <svg viewBox="0 0 24 24" className="w-6 h-6 stroke-black fill-none stroke-[2.5] stroke-linecap-round stroke-linejoin-round">
              <line x1="19" y1="12" x2="5" y2="12"></line>
              <polyline points="12 19 5 12 12 5"></polyline>
            </svg>
          </button>
          <h1 className="text-[17px] font-bold tracking-wide flex-1 text-center pr-6">Coin Seller Center</h1>
        </div>
      </div>

      <div className="px-4 pb-8 space-y-2">
        {/* Profile Info */}
        <div className="w-full py-4 flex flex-col">
          <div className="flex items-center space-x-3 mb-4">
            <div className="w-12 h-12 rounded-full overflow-hidden border border-gray-200 flex-shrink-0">
              <img src="https://i.pravatar.cc/150?u=nawab" alt="Profile" className="w-full h-full object-cover" />
            </div>

            <div className="flex flex-col">
              <span className="text-[15px] font-bold text-gray-800">꧁Ks༒Prad...</span>
              <div className="flex items-center space-x-1 mt-0.5">
                <span className="text-[12px] text-gray-400">ID:116943047</span>
                <svg viewBox="0 0 24 24" className="w-3 h-3 fill-gray-400">
                  <path d="M16 1H4C2.9 1 2 1.9 2 3v14h2V3h12V1zm3 4H8C6.9 5 6 5.9 6 7v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z" />
                </svg>
              </div>
            </div>
          </div>

          <div className="w-full h-px bg-gray-100 mb-3"></div>

          <div className="flex items-center justify-between py-1.5">
            <span className="text-[14px] text-gray-800 font-medium">WhatsApp</span>
            <div className="flex items-center text-gray-800 text-[14px] font-medium">
              +91 9837152239
              <svg viewBox="0 0 24 24" className="w-4 h-4 ml-1 stroke-black fill-none stroke-[2] stroke-linecap-round stroke-linejoin-round">
                <path d="M9 5l7 7-7 7" />
              </svg>
            </div>
          </div>

          <div className="flex items-center justify-between py-1.5 mt-1">
            <span className="text-[14px] text-gray-800 font-medium">Payment Method</span>
            <div className="flex items-center text-gray-800 text-[14px] font-medium">
              <span className="mr-1 text-[18px] leading-none">🇮🇳</span>
              <svg viewBox="0 0 24 24" className="w-4 h-4 ml-1 stroke-black fill-none stroke-[2] stroke-linecap-round stroke-linejoin-round">
                <path d="M9 5l7 7-7 7" />
              </svg>
            </div>
          </div>
        </div>

        <div className="w-full h-2 bg-gray-50 rounded-full my-2"></div>

        {/* Transfer Section */}
        <div className="w-full py-4 flex flex-col">
          {/* Balance */}
          <div className="flex items-start justify-between mb-4">
            <div className="flex flex-col">
              <div className="flex items-center space-x-1.5 mb-1">
                <CleanedCoinImage src="/file_00000000e56882119c217d508b6733dc.png" className="w-6 h-6 object-contain" />
                <span className="text-[22px] font-extrabold text-gray-800">
                  {balance.toLocaleString()}
                </span>
              </div>
              <span className="text-[12px] text-gray-400 font-medium">Available Balance</span>
            </div>
            <button
              onClick={() => setCurrentView('record')}
              className="flex items-center text-blue-400 text-[13px] font-bold cursor-pointer"
            >
              <svg viewBox="0 0 24 24" className="w-4 h-4 mr-0.5 fill-current">
                <path d="M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z" />
              </svg>
              Details
            </button>
          </div>

          <div className="w-full h-px bg-gray-100 mb-4"></div>

          <div className="flex items-center justify-between mb-6">
            <span className="text-[13px] text-gray-500 font-medium">Total Balance:</span>
            <div className="flex items-center space-x-1">
              <CleanedCoinImage src="/file_00000000e56882119c217d508b6733dc.png" className="w-4 h-4 object-contain" />
              <span className="text-[14px] font-bold text-gray-800">
                {balance.toLocaleString()}
              </span>
            </div>
          </div>

          <div className="flex items-center space-x-6 mb-6">
            <span className="text-[13px] text-gray-800 font-bold">Sales method:</span>

            <div className="flex items-center space-x-4">
              <label
                className="flex items-center space-x-1.5 cursor-pointer"
                onClick={() => setSalesMethod('user')}
              >
                <div className={`w-4 h-4 rounded-full border-2 flex items-center justify-center transition-colors ${salesMethod === 'user' ? 'border-blue-400' : 'border-gray-300'}`}>
                  {salesMethod === 'user' && <div className="w-2 h-2 bg-blue-400 rounded-full"></div>}
                </div>
                <span className="text-[14px] text-gray-800 font-medium">User</span>
              </label>

              <label
                className="flex items-center space-x-1.5 cursor-pointer"
                onClick={() => setSalesMethod('seller')}
              >
                <div className={`w-4 h-4 rounded-full border-2 flex items-center justify-center transition-colors ${salesMethod === 'seller' ? 'border-blue-400' : 'border-gray-300'}`}>
                  {salesMethod === 'seller' && <div className="w-2 h-2 bg-blue-400 rounded-full"></div>}
                </div>
                <span className="text-[14px] text-gray-800 font-medium">Seller</span>
              </label>
            </div>
          </div>

          {/* User/Seller ID */}
          <div className="flex flex-col mb-4 space-y-2">
            <label className="text-[13px] font-bold text-gray-800">
              {salesMethod === 'user' ? 'User ID:' : 'Seller ID:'}
            </label>
            <div className="relative flex items-center bg-[#f7f8fa] rounded-xl overflow-hidden px-4 py-3.5">
              <input
                type="text"
                value={targetId}
                onChange={(e) => setTargetId(e.target.value)}
                placeholder="Please input the id"
                className="flex-1 bg-transparent text-[14px] outline-none text-gray-800 placeholder-gray-400"
              />
              <button className="text-blue-400 font-medium text-[14px] cursor-pointer ml-2">
                Check
              </button>
            </div>
          </div>

          {/* Amount */}
          <div className="flex flex-col mb-8 space-y-2">
            <label className="text-[13px] font-bold text-gray-800">Amount:</label>
            <div className="relative flex items-center bg-[#f7f8fa] rounded-xl overflow-hidden px-4 py-3.5">
              <input
                type="number"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                placeholder="Please input the number"
                className="flex-1 bg-transparent text-[14px] outline-none text-gray-800 placeholder-gray-400"
              />
            </div>
            {amount && amountNum > balance && (
              <span className="text-[11px] text-red-500 font-medium px-1">
                Insufficient balance
              </span>
            )}
          </div>

          <button
            onClick={handleTransfer}
            disabled={isTransferDisabled}
            className="w-full bg-blue-300 hover:bg-blue-400 disabled:bg-blue-200 disabled:cursor-not-allowed text-white font-bold text-[16px] py-3.5 rounded-full transition-colors cursor-pointer"
          >
            {transferring ? 'Transferring...' : 'Transfer'}
          </button>
        </div>
      </div>
    </div>
  );
    }
