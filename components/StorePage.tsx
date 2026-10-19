"use client";

import React, { useState, useEffect, useRef } from "react";
import Image from "next/image";
import { ArrowLeft, Clock } from "lucide-react";
import { deductCoinsFromDB } from "./Wallet";

interface StoreItem {
  id: string;
  name: string;
  image: string;
  tryVideo?: string;
  removeGreen?: boolean;
  tab: string;
  stars: number;
  price: string;
  duration: string;
  isOwned?: boolean;
  dailyReward?: boolean;
}

// ==========================================
// SHARED WALLET DB
// ==========================================
const SHARED_DB = 'FruitPartyDB';
const SHARED_STORE = 'GameState';
const DEFAULT_BALANCE = 82927;

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

const loadWalletData = async (): Promise<{ balance: number; ownedItems: string[]; equippedItems: string[] }> => {
  try {
    const db = await initWalletDB();
    return new Promise((resolve) => {
      const tx = db.transaction(SHARED_STORE, 'readonly');
      const req = tx.objectStore(SHARED_STORE).get('user_data');
      req.onsuccess = () => {
        const data = req.result;
        resolve({
          balance: typeof data?.balance === 'number' ? data.balance : DEFAULT_BALANCE,
          ownedItems: Array.isArray(data?.ownedItems) ? data.ownedItems : [],
          equippedItems: Array.isArray(data?.equippedItems) ? data.equippedItems : [],
        });
      };
      req.onerror = () => resolve({ balance: DEFAULT_BALANCE, ownedItems: [], equippedItems: [] });
    });
  } catch {
    return { balance: DEFAULT_BALANCE, ownedItems: [], equippedItems: [] };
  }
};

const CHAT_BUBBLE_EXPIRY_PREFIX = "chatBubbleExpiry_";

const durationToMs = (duration: string): number => {
  const value = String(duration || "").trim();
  if (!value || /^all$/i.test(value)) return 0;
  const match = value.match(/^(\d+)\s*D$/i);
  return match ? Number(match[1]) * 24 * 60 * 60 * 1000 : 0;
};

const getExpiryKey = (itemId: string) => CHAT_BUBBLE_EXPIRY_PREFIX + itemId;

const getStoredExpiry = (itemId: string): number | null => {
  if (typeof window === "undefined") return null;
  const raw = localStorage.getItem(getExpiryKey(itemId));
  const value = raw ? Number(raw) : NaN;
  return Number.isFinite(value) ? value : null;
};

const isItemActive = (item: StoreItem): boolean => {
  const expiry = getStoredExpiry(item.id);
  return expiry === null || expiry > Date.now();
};

const saveItemExpiry = (item: StoreItem) => {
  const ms = durationToMs(item.duration);
  if (ms <= 0 || typeof window === "undefined") return;
  localStorage.setItem(getExpiryKey(item.id), String(Date.now() + ms));
};

const getExpiredStoreItemIds = (items: StoreItem[]): string[] => {
  if (typeof window === "undefined") return [];
  const now = Date.now();
  const expired: string[] = [];
  for (const item of items) {
    const key = getExpiryKey(item.id);
    const raw = localStorage.getItem(key);
    if (raw && Number(raw) <= now) {
      expired.push(item.id);
      localStorage.removeItem(key);
      if (item.tab === "Chat Bubble" && localStorage.getItem("equipped_Chat Bubble_expiresAt") === raw) {
        localStorage.removeItem("equipped_Chat Bubble");
        localStorage.removeItem("equipped_Chat Bubble_expiresAt");
      }
      if (item.tab === "Vehicle") {
        localStorage.removeItem("equipped_Vehicle");
        localStorage.removeItem("equipped_Vehicle_image");
      }
    }
  }
  return expired;
};

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

const addOwnedItemToDB = async (itemId: string): Promise<void> => {
  try {
    const db = await initWalletDB();
    return new Promise((resolve, reject) => {
      const tx = db.transaction(SHARED_STORE, 'readwrite');
      const store = tx.objectStore(SHARED_STORE);
      const req = store.get('user_data');
      req.onsuccess = () => {
        const data = req.result || {};
        const current: string[] = Array.isArray(data.ownedItems) ? data.ownedItems : [];
        if (!current.includes(itemId)) current.push(itemId);
        const putReq = store.put({ ...data, ownedItems: current }, 'user_data');
        putReq.onsuccess = () => resolve();
        putReq.onerror = () => reject(putReq.error);
      };
      req.onerror = () => reject(req.error);
    });
  } catch (e) {
    console.error('Owned item save failed', e);
  }
};

const saveEquippedItemsToDB = async (equipped: string[]): Promise<void> => {
  try {
    const db = await initWalletDB();
    return new Promise((resolve, reject) => {
      const tx = db.transaction(SHARED_STORE, 'readwrite');
      const store = tx.objectStore(SHARED_STORE);
      const req = store.get('user_data');
      req.onsuccess = () => {
        const data = req.result || {};
        const putReq = store.put({ ...data, equippedItems: equipped }, 'user_data');
        putReq.onsuccess = () => resolve();
        putReq.onerror = () => reject(putReq.error);
      };
      req.onerror = () => reject(req.error);
    });
  } catch (e) {
    console.error('Equipped save failed', e);
  }
};

// ==========================================
// STATIC DATA
// ==========================================
const tabData = [
  { id: "Vehicle", label: "Vehicle", icon: "/IMG_20260913_090019.png" },
  { id: "Avatar Frame", label: "Frame", icon: "/IMG_20260913_090057.png" },
  { id: "Theme", label: "Theme", icon: "/IMG_20260913_090137.png" },
  { id: "Chat Bubble", label: "Bubble", icon: "/IMG_20260913_090116.png" },
  { id: "ID", label: "ID", icon: "/IMG_20260913_090150.png" },
];

const allStoreItems: StoreItem[] = [
  {
    id: "v1",
    name: "Leopard Roar",
    image: "/IMG_20260905_222412.jpg",
    tryVideo: "/VID_20260905_095157_269_bsl.mp4",
    removeGreen: true,
    tab: "Vehicle",
    stars: 5,
    price: "1,000,000",
    duration: "5D",
  },

  { id: "a1", name: "Crown Wings", image: "/VID_20260905_024534_955_bsl.mp4", tab: "Avatar Frame", stars: 5, price: "250,000", duration: "3D" },
  { id: "a2", name: "Host Wings", image: "/VID_20260905_024726_660_bsl.mp4", tab: "Avatar Frame", stars: 5, price: "500,000", duration: "3D" },
  { id: "a3", name: "Mystic Wings", image: "/VID_20260905_083446_619_bsl.mp4", tab: "Avatar Frame", stars: 5, price: "750,000", duration: "3D" },

  { id: "t1", name: "Seafood", image: "/IMG-20260904-WA0004.jpg", tab: "Theme", stars: 4, price: "2,700,000", duration: "30D" },
  { id: "t2", name: "Night Sky", image: "/IMG-20260904-WA0005.jpg ", tab: "Theme", stars: 5, price: "2,400,000", duration: "30D" },
  { id: "t3", name: "Seafood", image: "/IMG-20260904-WA0006.jpg", tab: "Theme", stars: 4, price: "2,700,000", duration: "30D" },
  { id: "t4", name: "Night Sky", image: "/IMG-20260904-WA0007.jpg ", tab: "Theme", stars: 5, price: "2,400,000", duration: "30D" },
  { id: "t5", name: "Seafood", image: "/IMG-20260904-WA0040.jpg", tab: "Theme", stars: 4, price: "2,700,000", duration: "30D" },
  { id: "t6", name: "Night Sky", image: "/IMG-20260904-WA0041.jpg ", tab: "Theme", stars: 5, price: "2,400,000", duration: "30D" },

  { id: "c1", name: "1", image: "/file_000000003d888211822aa6837fe5013c.png", tab: "Chat Bubble", stars: 4, price: "500,000", duration: "3D" },
  { id: "c2", name: "2", image: "/file_000000006044821186ff566329797142.png", tab: "Chat Bubble", stars: 4, price: "250,000", duration: "2D" },
  { id: "c3", name: "3", image: "/file_00000000c44c81f598f62ae8a45e13a7.png", tab: "Chat Bubble", stars: 5, price: "300,000", duration: "3D" },
  { id: "c4", name: "4", image: "/IMG_20260920_122831.png", tab: "Chat Bubble", stars: 4, price: "246,000", duration: "3D" },
  { id: "c5", name: "5", image: "/IMG_20260920_122743.png", tab: "Chat Bubble", stars: 4, price: "159,000", duration: "3D" },
  { id: "c6", name: "6", image: "/IMG_20260920_121752.png", tab: "Chat Bubble", stars: 4, price: "200,000", duration: "3D" },

  { id: "i1", name: "ID Badge 8", image: "/1784533036732~2.jpg", tab: "ID", stars: 5, price: "10,000,000", duration: "3D", isOwned: true },
];

// ==========================================
// WebGL WHITE-REMOVAL
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
  float lum = dot(color.rgb, vec3(0.299, 0.587, 0.114));
  if (lum > 0.85 && color.r > 0.8 && color.g > 0.8 && color.b > 0.8) {
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
    premultipliedAlpha: false, alpha: true, preserveDrawingBuffer: true, antialias: false,
  }) as WebGLRenderingContext | null;
  if (!gl) return null;
  const program = gl.createProgram()!;
  gl.attachShader(program, compile(gl, gl.VERTEX_SHADER, VS));
  gl.attachShader(program, compile(gl, gl.FRAGMENT_SHADER, FS));
  gl.linkProgram(program);
  gl.useProgram(program);
  const posBuf = gl.createBuffer();
  gl.bindBuffer(gl.ARRAY_BUFFER, posBuf);
  gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1,-1, 1,-1, -1,1, -1,1, 1,-1, 1,1]), gl.STATIC_DRAW);
  const posLoc = gl.getAttribLocation(program, 'a_position');
  gl.enableVertexAttribArray(posLoc);
  gl.vertexAttribPointer(posLoc, 2, gl.FLOAT, false, 0, 0);
  const texBuf = gl.createBuffer();
  gl.bindBuffer(gl.ARRAY_BUFFER, texBuf);
  gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([0,1, 1,1, 0,0, 0,0, 1,1, 1,0]), gl.STATIC_DRAW);
  const texLoc = gl.getAttribLocation(program, 'a_texCoord');
  gl.enableVertexAttribArray(texLoc);
  gl.vertexAttribPointer(texLoc, 2, gl.FLOAT, false, 0, 0);
  const tex = gl.createTexture();
  gl.bindTexture(gl.TEXTURE_2D, tex);
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR);
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR);
  glCanvas = canvas; glCtx = gl; glTex = tex;
  return gl;
}

function processWhiteRemoval(src: string): Promise<string> {
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
      } catch (e) { reject(e); }
    };
    img.onerror = reject;
    img.src = src;
  });
  processedCache.set(src, p);
  return p;
}

function WebGLCoinIcon({ src, className = 'w-full h-full object-contain' }: { src: string; className?: string }) {
  const [url, setUrl] = useState<string | null>(null);
  useEffect(() => {
    let alive = true;
    processWhiteRemoval(src).then((u) => alive && setUrl(u)).catch(() => alive && setUrl(src));
    return () => { alive = false; };
  }, [src]);
  if (!url) return <div className={className} aria-hidden />;
  return <img src={url} alt="Coin" className={className} draggable={false} />;
}

// ==========================================
// WebGL Image (green removal)
// ==========================================
const avatarCache = new Map<string, Promise<string>>();
let imgGlCanvas: HTMLCanvasElement | null = null;
let imgGlCtx: WebGLRenderingContext | null = null;
let imgGlTex: WebGLTexture | null = null;

const IMG_VS = `
attribute vec2 a_position;
attribute vec2 a_texCoord;
varying vec2 v_texCoord;
void main() {
  gl_Position = vec4(a_position, 0.0, 1.0);
  v_texCoord = a_texCoord;
}`;

const IMG_FS = `
precision mediump float;
varying vec2 v_texCoord;
uniform sampler2D u_image;
void main() {
  vec4 color = texture2D(u_image, v_texCoord);
  float maxRB = max(color.r, color.b);
  float greenness = color.g - maxRB;
  float blend = smoothstep(0.04, 0.15, greenness);
  vec4 despilled = color;
  despilled.g = min(despilled.g, maxRB + 0.05);
  gl_FragColor = mix(despilled, vec4(0.0, 0.0, 0.0, 0.0), blend);
}`;

function ensureImageGL(): WebGLRenderingContext | null {
  if (imgGlCtx) return imgGlCtx;
  if (typeof document === 'undefined') return null;
  const canvas = document.createElement('canvas');
  const gl = canvas.getContext('webgl', {
    premultipliedAlpha: false, alpha: true, preserveDrawingBuffer: true, antialias: false,
  }) as WebGLRenderingContext | null;
  if (!gl) return null;
  const program = gl.createProgram()!;
  gl.attachShader(program, compile(gl, gl.VERTEX_SHADER, IMG_VS));
  gl.attachShader(program, compile(gl, gl.FRAGMENT_SHADER, IMG_FS));
  gl.linkProgram(program);
  gl.useProgram(program);
  const posBuf = gl.createBuffer();
  gl.bindBuffer(gl.ARRAY_BUFFER, posBuf);
  gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1,-1, 1,-1, -1,1, -1,1, 1,-1, 1,1]), gl.STATIC_DRAW);
  const posLoc = gl.getAttribLocation(program, 'a_position');
  gl.enableVertexAttribArray(posLoc);
  gl.vertexAttribPointer(posLoc, 2, gl.FLOAT, false, 0, 0);
  const texBuf = gl.createBuffer();
  gl.bindBuffer(gl.ARRAY_BUFFER, texBuf);
  gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([0,0, 1,0, 0,1, 0,1, 1,0, 1,1]), gl.STATIC_DRAW);
  const texLoc = gl.getAttribLocation(program, 'a_texCoord');
  gl.enableVertexAttribArray(texLoc);
  gl.vertexAttribPointer(texLoc, 2, gl.FLOAT, false, 0, 0);
  const tex = gl.createTexture();
  gl.bindTexture(gl.TEXTURE_2D, tex);
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR);
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR);
  imgGlCanvas = canvas; imgGlCtx = gl; imgGlTex = tex;
  return gl;
}

function processGreenRemovalImage(src: string): Promise<string> {
  const hit = avatarCache.get(src);
  if (hit) return hit;
  const p = new Promise<string>((resolve, reject) => {
    const gl = ensureImageGL();
    if (!gl || !imgGlCanvas) return reject(new Error('no webgl'));
    const img = new Image();
    img.crossOrigin = 'anonymous';
    img.onload = () => {
      try {
        imgGlCanvas!.width = img.naturalWidth || img.width || 256;
        imgGlCanvas!.height = img.naturalHeight || img.height || 256;
        gl.viewport(0, 0, imgGlCanvas!.width, imgGlCanvas!.height);
        gl.bindTexture(gl.TEXTURE_2D, imgGlTex);
        gl.pixelStorei(gl.UNPACK_FLIP_Y_WEBGL, true);
        gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, img);
        gl.clearColor(0, 0, 0, 0);
        gl.clear(gl.COLOR_BUFFER_BIT);
        gl.drawArrays(gl.TRIANGLES, 0, 6);
        resolve(imgGlCanvas!.toDataURL('image/png'));
      } catch (e) { reject(e); }
    };
    img.onerror = reject;
    img.src = src;
  });
  avatarCache.set(src, p);
  return p;
}

function WebGLImageAvatar({ src, className = 'w-full h-full object-contain' }: { src: string; className?: string }) {
  const [url, setUrl] = useState<string | null>(null);
  useEffect(() => {
    let alive = true;
    processGreenRemovalImage(src).then((u) => alive && setUrl(u)).catch(() => alive && setUrl(src));
    return () => { alive = false; };
  }, [src]);
  if (!url) return <div className={className} aria-hidden />;
  return <img src={url} alt="Avatar" className={className} draggable={false} />;
}

// ==========================================
// WebGL Video (green removal)
// ==========================================
function WebGLVideoAvatar({ src, isVehicleModal = false }: { src: string; isVehicleModal?: boolean }) {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const gl = canvas.getContext('webgl', { preserveDrawingBuffer: false, alpha: true });
    if (!gl) return;

    const video = document.createElement('video');
    video.src = src;
    video.crossOrigin = 'anonymous';
    video.loop = true;
    video.muted = !isVehicleModal;
    video.playsInline = true;
    video.play().catch((e) => console.log('Video autoplay prevented:', e));

    const vsSource = `
      attribute vec2 a_position;
      attribute vec2 a_texCoord;
      varying vec2 v_texCoord;
      void main() {
        gl_Position = vec4(a_position, 0.0, 1.0);
        v_texCoord = a_texCoord;
      }
    `;

    const fsSource = `
      precision mediump float;
      varying vec2 v_texCoord;
      uniform sampler2D u_image;
      void main() {
        vec4 color = texture2D(u_image, v_texCoord);
        float maxRB = max(color.r, color.b);
        float greenness = color.g - maxRB;
        float blend = smoothstep(0.04, 0.15, greenness);
        vec4 despilled = color;
        despilled.g = min(despilled.g, maxRB + 0.05);
        gl_FragColor = mix(despilled, vec4(0.0, 0.0, 0.0, 0.0), blend);
      }
    `;

    const createShader = (glCtx: WebGLRenderingContext, type: number, source: string) => {
      const shader = glCtx.createShader(type);
      if (!shader) return null;
      glCtx.shaderSource(shader, source);
      glCtx.compileShader(shader);
      return shader;
    };

    const vertexShader = createShader(gl, gl.VERTEX_SHADER, vsSource);
    const fragmentShader = createShader(gl, gl.FRAGMENT_SHADER, fsSource);
    if (!vertexShader || !fragmentShader) return;

    const program = gl.createProgram();
    if (!program) return;
    gl.attachShader(program, vertexShader);
    gl.attachShader(program, fragmentShader);
    gl.linkProgram(program);
    gl.useProgram(program);

    const positionLocation = gl.getAttribLocation(program, 'a_position');
    const texCoordLocation = gl.getAttribLocation(program, 'a_texCoord');

    const positionBuffer = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER, positionBuffer);
    gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1,-1, 1,-1, -1,1, -1,1, 1,-1, 1,1]), gl.STATIC_DRAW);
    gl.enableVertexAttribArray(positionLocation);
    gl.vertexAttribPointer(positionLocation, 2, gl.FLOAT, false, 0, 0);

    const texCoordBuffer = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER, texCoordBuffer);
    gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([0,0, 1,0, 0,1, 0,1, 1,0, 1,1]), gl.STATIC_DRAW);
    gl.enableVertexAttribArray(texCoordLocation);
    gl.vertexAttribPointer(texCoordLocation, 2, gl.FLOAT, false, 0, 0);

    const texture = gl.createTexture();
    gl.bindTexture(gl.TEXTURE_2D, texture);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR);
    gl.pixelStorei(gl.UNPACK_FLIP_Y_WEBGL, true);

    let animationFrameId: number;
    const render = () => {
      if (video.readyState >= video.HAVE_CURRENT_DATA) {
        gl.bindTexture(gl.TEXTURE_2D, texture);
        gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, video);
        gl.viewport(0, 0, canvas.width, canvas.height);
        gl.clearColor(0, 0, 0, 0);
        gl.clear(gl.COLOR_BUFFER_BIT);
        gl.drawArrays(gl.TRIANGLES, 0, 6);
      }
      animationFrameId = requestAnimationFrame(render);
    };
    render();

    return () => {
      cancelAnimationFrame(animationFrameId);
      video.pause();
      video.removeAttribute('src');
      video.load();
      gl.deleteTexture(texture);
      gl.deleteBuffer(positionBuffer);
      gl.deleteBuffer(texCoordBuffer);
      gl.deleteProgram(program);
    };
  }, [src, isVehicleModal]);

  return <canvas ref={canvasRef} width={512} height={512} className="w-full h-full object-contain" />;
}

// ==========================================
// Main Component
// ==========================================
export default function StorePage({
  onBack,
  initialView = "store",
}: {
  onBack: () => void;
  initialView?: "store" | "bag";
}) {
  const [currentView, setCurrentView] = useState<"store" | "bag">(initialView);
  const [activeTab, setActiveTab] = useState("Vehicle");
  const [tryThemeItem, setTryThemeItem] = useState<StoreItem | null>(null);
  const [tryCenterItem, setTryCenterItem] = useState<StoreItem | null>(null);
  const [tryChatBubbleItem, setTryChatBubbleItem] = useState<StoreItem | null>(null);
  const [giftItem, setGiftItem] = useState<StoreItem | null>(null);

  const [balance, setBalance] = useState<number>(0);
  const [ownedIds, setOwnedIds] = useState<Set<string>>(
    new Set(allStoreItems.filter((i) => i.isOwned).map((i) => i.id))
  );
  const [equippedIds, setEquippedIds] = useState<Set<string>>(new Set());
  const [buying, setBuying] = useState<string | null>(null);

  const [userPhoto, setUserPhoto] = useState<string>("");
  const [userName, setUserName] = useState<string>("");

  // SWIPE SUPPORT
  const touchStartX = useRef<number | null>(null);
  const touchEndX = useRef<number | null>(null);

  useEffect(() => {
    const readUser = () => {
      if (typeof window === "undefined") return;
      const photo = localStorage.getItem("userPhoto") || "";
      const name = localStorage.getItem("userName") || "";
      setUserPhoto(photo);
      setUserName(name);
    };
    readUser();
    const id = setInterval(readUser, 800);
    return () => clearInterval(id);
  }, []);

  useEffect(() => {
    let alive = true;
    const sync = async () => {
      const expiredIds = getExpiredStoreItemIds(allStoreItems);
      for (const expiredId of expiredIds) {
        await removeOwnedItemFromDB(expiredId);
      }
      const { balance: bal, ownedItems, equippedItems } = await loadWalletData();
      if (!alive) return;
      setBalance(bal);
      setOwnedIds((prev) => {
        const next = new Set(prev);
        ownedItems.forEach((id) => next.add(id));
        return next;
      });
      const activeEquipped = equippedItems.filter((id) => {
        const item = allStoreItems.find((it) => it.id === id);
        return !item || isItemActive(item);
      });
      setEquippedIds(new Set(activeEquipped));
      const activeBubbleId = activeEquipped.find((id) => allStoreItems.find((it) => it.id === id)?.tab === "Chat Bubble");
      if (!activeBubbleId) {
        localStorage.removeItem("equipped_Chat Bubble");
        localStorage.removeItem("equipped_Chat Bubble_expiresAt");
      }
    };
    sync();
    const id = setInterval(sync, 1500);
    const expiryTimer = setInterval(async () => {
      const expiredIds = getExpiredStoreItemIds(allStoreItems);
      if (expiredIds.length) {
        for (const expiredId of expiredIds) await removeOwnedItemFromDB(expiredId);
        setOwnedIds((prev) => {
          const next = new Set(prev);
          expiredIds.forEach((id) => next.delete(id));
          return next;
        });
        setEquippedIds((prev) => {
          const next = new Set(prev);
          expiredIds.forEach((id) => next.delete(id));
          return next;
        });
      }
    }, 1000);
    return () => {
      alive = false;
      clearInterval(id);
      clearInterval(expiryTimer);
    };
  }, []);

  const parsePrice = (p: string) => parseInt(p.replace(/,/g, ''), 10) || 0;

  const removeOwnedItemFromDB = async (itemId: string): Promise<void> => {
    try {
      const db = await initWalletDB();
      await new Promise<void>((resolve, reject) => {
        const tx = db.transaction(SHARED_STORE, 'readwrite');
        const store = tx.objectStore(SHARED_STORE);
        const req = store.get('user_data');
        req.onsuccess = () => {
          const data = req.result || {};
          const ownedItems = Array.isArray(data.ownedItems) ? data.ownedItems.filter((id: string) => String(id) !== String(itemId)) : [];
          const equippedItems = Array.isArray(data.equippedItems) ? data.equippedItems.filter((id: string) => String(id) !== String(itemId)) : [];
          const putReq = store.put({ ...data, ownedItems, equippedItems }, 'user_data');
          putReq.onsuccess = () => resolve();
          putReq.onerror = () => reject(putReq.error);
        };
        req.onerror = () => reject(req.error);
      });
    } catch (e) {
      console.error('Expired item removal failed', e);
    }
  };

  const handleBuy = async (item: StoreItem) => {
    if (buying) return;
    const cost = parsePrice(item.price);
    if (balance < cost) {
      alert('Insufficient balance');
      return;
    }
    setBuying(item.id);
    setBalance((b) => b - cost);
    setOwnedIds((prev) => new Set(prev).add(item.id));
    saveItemExpiry(item);
    await updateWalletBalance(-cost);
    await addOwnedItemToDB(item.id);
    recordTransaction(`Purchased ${item.name}`, -cost);
    setBuying(null);
  };

  const handleEquipToggle = async (item: StoreItem) => {
    const next = new Set(equippedIds);
    const wasEquipped = next.has(item.id);

    if (wasEquipped) {
      next.delete(item.id);
    } else {
      allStoreItems.forEach((it) => {
        if (it.tab === item.tab && next.has(it.id)) next.delete(it.id);
      });
      next.add(item.id);
    }

    setEquippedIds(next);
    await saveEquippedItemsToDB(Array.from(next));

    if (item.tab === "Chat Bubble") {
      if (wasEquipped) {
        localStorage.removeItem("equipped_Chat Bubble");
        localStorage.removeItem("equipped_Chat Bubble_expiresAt");
      } else {
        localStorage.setItem("equipped_Chat Bubble", item.image);
        const expiry = getStoredExpiry(item.id);
        if (expiry) {
          localStorage.setItem("equipped_Chat Bubble_expiresAt", String(expiry));
        }
      }
      window.dispatchEvent(new Event("hurry-chat-bubble-equipped"));
    }

    if (item.tab === "Vehicle") {
      if (wasEquipped) {
        localStorage.removeItem("equipped_Vehicle");
        localStorage.removeItem("equipped_Vehicle_image");
      } else {
        const vehicleAsset = String(item.tryVideo || "").trim();
        if (vehicleAsset && /\.mp4(?:[?#].*)?$/i.test(vehicleAsset)) {
          localStorage.setItem("equipped_Vehicle", vehicleAsset);
          localStorage.setItem("equipped_Vehicle_image", String(item.image || ""));
        } else {
          localStorage.removeItem("equipped_Vehicle");
          localStorage.removeItem("equipped_Vehicle_image");
          console.warn("Vehicle equip skipped: missing playable MP4", item.id);
          return;
        }
      }
      window.dispatchEvent(new Event("hurry-vehicle-equipped"));
    }
  };

  useEffect(() => {
    const equippedVehicleId = Array.from(equippedIds).find((id) => {
      const item = allStoreItems.find((it) => it.id === id);
      return item?.tab === "Vehicle";
    });
    const equippedVehicle = equippedVehicleId
      ? allStoreItems.find((it) => it.id === equippedVehicleId)
      : null;
    if (equippedVehicle) {
      localStorage.setItem(
        "equipped_Vehicle",
        equippedVehicle.tryVideo || equippedVehicle.image
      );
    } else {
      localStorage.removeItem("equipped_Vehicle");
    }
  }, [equippedIds]);

  const displayedItems = allStoreItems.filter((item) => {
    const isOwned = ownedIds.has(item.id);
    const isActive = isItemActive(item);
    if (currentView === "bag") {
      return isOwned && isActive && item.tab === activeTab;
    }
    return !item.dailyReward && item.tab === activeTab;
  });

  const renderStars = (count: number) => {
    return Array.from({ length: 5 }).map((_, i) => (
      <span key={i} className={`text-[16px] leading-none ${i < count ? 'text-yellow-400' : 'text-gray-200'}`}>
        ★
      </span>
    ));
  };

  const avatarLetter = (userName && userName.trim().charAt(0).toUpperCase()) || "U";

  // ========== SWIPE HANDLERS ==========
  const SWIPE_THRESHOLD = 60;

  const handleTouchStart = (e: React.TouchEvent) => {
    touchStartX.current = e.targetTouches[0].clientX;
    touchEndX.current = null;
  };

  const handleTouchMove = (e: React.TouchEvent) => {
    touchEndX.current = e.targetTouches[0].clientX;
  };

  const handleTouchEnd = () => {
    if (touchStartX.current === null || touchEndX.current === null) return;
    const distance = touchStartX.current - touchEndX.current;
    if (Math.abs(distance) < SWIPE_THRESHOLD) return;

    const currentIndex = tabData.findIndex((t) => t.id === activeTab);
    if (currentIndex === -1) return;

    if (distance > 0) {
      const nextIndex = currentIndex + 1;
      if (nextIndex < tabData.length) {
        setActiveTab(tabData[nextIndex].id);
      }
    } else {
      const prevIndex = currentIndex - 1;
      if (prevIndex >= 0) {
        setActiveTab(tabData[prevIndex].id);
      }
    }

    touchStartX.current = null;
    touchEndX.current = null;
  };
  // ====================================

  return (
    <div className="h-screen bg-[#f5f6f8] text-gray-800 select-none font-sans relative flex flex-col overflow-hidden">
      <div className="max-w-md mx-auto w-full h-full flex flex-col relative">

        {/* Sticky Header */}
        <div
          className="sticky top-0 left-0 w-full z-40 bg-[#f5f6f8] flex flex-col"
          style={{ paddingTop: 'calc(env(safe-area-inset-top, 0px) + 15px)' }}
        >
          <div className="flex items-center justify-between px-3 pt-3 pb-2 w-full">
            <button
              type="button"
              onClick={() => {
                if (currentView === "bag") {
                  setCurrentView("store");
                } else {
                  onBack();
                }
              }}
              className="p-1 -ml-2 text-black hover:bg-black/5 rounded-full transition-colors z-10"
            >
              <ArrowLeft size={26} strokeWidth={2} />
            </button>

            <h1 className="text-[18px] font-bold text-black absolute left-1/2 -translate-x-1/2 z-0">
              {currentView === "store" ? "Store" : "Bag"}
            </h1>

            <button
              type="button"
              onClick={() => setCurrentView(currentView === "store" ? "bag" : "store")}
              className="font-bold text-[#1d4ed8] text-[15px] z-10 hover:opacity-80 transition-opacity pr-1"
            >
              {currentView === "store" ? "Bag" : "Store"}
            </button>
          </div>

          <div className="flex items-center gap-1 pl-3 pr-[2vh] mt-1 mb-3 overflow-hidden shrink-0 w-full">
            {tabData.map((tab) => {
              const isActive = activeTab === tab.id;
              return (
                <button
                  key={tab.id}
                  type="button"
                  onClick={() => setActiveTab(tab.id)}
                  className={`relative w-[63px] h-[80px] rounded-md flex flex-col items-center justify-center shrink-0 overflow-hidden transition-all ${
                    isActive ? "bg-transparent" : "bg-black/20"
                  }`}
                >
                  {isActive && (
                    <div className="absolute inset-0 z-0">
                      <Image src="/IMG_20260913_090226.png" alt="Active Bg" fill className="object-contain" />
                    </div>
                  )}
                  <div className="relative z-10 flex flex-col items-center gap-1 mt-1">
                    <div className="relative w-10 h-10">
                      <Image src={tab.icon} alt={tab.label} fill className="object-contain" />
                    </div>
                    <span className="text-[12px] font-medium text-white tracking-wide">
                      {tab.label}
                    </span>
                  </div>
                </button>
              );
            })}
          </div>
        </div>

        {/* Scrollable Body — SWIPE ENABLED HERE */}
        <div
          className="flex-1 overflow-y-auto no-scrollbar w-full pb-10"
          onTouchStart={handleTouchStart}
          onTouchMove={handleTouchMove}
          onTouchEnd={handleTouchEnd}
        >
          {activeTab === "ID" ? (
            <div className="px-3 py-2 flex flex-col gap-3 h-full">
              <div className="bg-white rounded-2xl p-5 shadow-sm flex flex-col gap-4">
                <h2 className="text-[17px] font-bold text-gray-900">Customize ID</h2>
                <div className="w-full bg-[#f3f4f6] text-gray-400 text-[14px] font-medium py-3 px-4 rounded-2xl text-center">
                  Start Your Customization Journey
                </div>
              </div>
              <button type="button" className="w-full bg-[#f3f4f6] text-gray-400 font-semibold py-3.5 rounded-2xl text-[15px] shadow-sm text-center">
                Customize
              </button>
            </div>
          ) : (
            <div className="grid grid-cols-2 gap-2 px-3 py-1 content-start">
              {displayedItems.map((item) => {
                const isTheme = item.tab === "Theme";
                const isVehicle = item.tab === "Vehicle";
                const isAvatarFrame = item.tab === "Avatar Frame";
                const isChatBubble = item.tab === "Chat Bubble";
                const isOwned = ownedIds.has(item.id);
                const isEquipped = equippedIds.has(item.id);
                const shouldRemoveGreen = isVehicle || isAvatarFrame;

                return (
                  <div
                    key={item.id}
                    className={`relative bg-white rounded-md p-1 flex flex-col items-center justify-between shadow-sm overflow-hidden ${
                      isTheme ? "min-h-[200px]" : "h-auto"
                    }`}
                  >
                    {isTheme && (
                      <div className="absolute inset-0 w-full h-full z-0">
                        <Image src={item.image} alt={item.name} fill className="object-cover" />
                        <div className="absolute inset-0 bg-black/20"></div>
                      </div>
                    )}

                    <div className="flex items-center justify-between w-full z-10 mb-2">
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          if (isTheme) {
                            setTryThemeItem(item);
                          } else if (isChatBubble) {
                            setTryChatBubbleItem(item);
                          } else {
                            setTryCenterItem(item);
                          }
                        }}
                        className={`px-3 py-[2px] rounded-full text-[11px] font-medium border ${
                          isTheme ? "text-white border-white bg-white/20" : "text-[#1d4ed8] border-[#1d4ed8]"
                        }`}
                      >
                        Try
                      </button>
                      <div className={`flex items-center gap-1 text-[11px] font-medium ${isTheme ? "text-white drop-shadow-md" : "text-gray-500"}`}>
                        <Clock size={12} strokeWidth={2.5} />
                        <span>{item.duration}</span>
                      </div>
                    </div>

                    {!isTheme && (
                      <div className="relative w-full h-[80px] my-2 flex items-center justify-center z-10">
                        <div className={`absolute flex items-center justify-center ${
                          isVehicle || isAvatarFrame ? "w-[120px] h-[120px]" : "w-full h-full"
                        }`}>
                          {item.image.endsWith('.mp4') ? (
                            <WebGLVideoAvatar src={item.image} />
                          ) : shouldRemoveGreen ? (
                            <WebGLImageAvatar src={item.image} />
                          ) : (
                            <Image src={item.image} alt={item.name} fill className="object-contain" sizes="50vw" />
                          )}
                        </div>
                      </div>
                    )}

                    {isTheme && <div className="flex-1 w-full min-h-[80px]"></div>}

                    <div className="flex items-center justify-center gap-0.5 mt-2 mb-1 w-full z-10">
                      {renderStars(item.stars)}
                    </div>

                    <div className="flex items-center justify-center gap-1.5 mb-3 w-full z-10">
                      <div className="relative w-4 h-4 flex items-center justify-center shrink-0">
                        <WebGLCoinIcon src="/file_00000000e56882119c217d508b6733dc.png" />
                      </div>
                      <span className={`text-[14px] font-bold tracking-tight truncate ${isTheme ? "text-white drop-shadow-md" : "text-gray-900"}`}>
                        {item.price}
                      </span>
                    </div>

                    <div className="flex items-center w-full rounded-full border border-[#1d4ed8] overflow-hidden h-[30px] z-10 bg-white">
                      <button
                        type="button"
                        onClick={() => setGiftItem(item)}
                        className="flex-1 h-full bg-white text-[#1d4ed8] text-[12px] font-bold flex items-center justify-center transition-colors hover:bg-gray-50"
                      >
                        Send
                      </button>
                      <button
                        type="button"
                        onClick={() => {
                          if (isOwned) handleEquipToggle(item);
                          else handleBuy(item);
                        }}
                        disabled={buying === item.id}
                        className="flex-1 h-full bg-[#1d4ed8] text-white text-[12px] font-bold flex items-center justify-center transition-colors hover:bg-blue-800 disabled:opacity-40 disabled:cursor-not-allowed"
                      >
                        {buying === item.id ? '...' : isOwned ? (isEquipped ? 'Equipped' : 'Equip') : 'Buy'}
                      </button>
                    </div>
                  </div>
                );
              })}

              {displayedItems.length === 0 && (
                <div className="col-span-2 flex flex-col items-center justify-center min-h-[50vh] w-full">
                  {currentView === "bag" ? (
                    <div className="flex flex-col items-center">
                      <div className="relative w-[120px] h-[120px] mb-2">
                        <Image src="/file_0000000047308211a02722299d1fda2e.png" alt="No data" fill className="object-contain" />
                      </div>
                      <span className="text-gray-400 text-sm font-medium">No data</span>
                    </div>
                  ) : (
                    <span className="text-gray-400 text-sm font-medium">No items found</span>
                  )}
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      {/* ============ Vehicle Try Modal ============ */}
      {tryCenterItem && tryCenterItem.tab !== "Avatar Frame" && (
        <div
          className="fixed inset-0 z-50 flex flex-col items-center justify-center bg-black/10 p-0 cursor-pointer"
          onClick={() => setTryCenterItem(null)}
        >
          <div className={`relative flex items-center justify-center pointer-events-none ${tryCenterItem.tab === "Vehicle" ? "w-full h-[60vh]" : "w-[280px] h-[280px]"}`}>
            {tryCenterItem.tryVideo ? (
              <WebGLVideoAvatar src={tryCenterItem.tryVideo} isVehicleModal={tryCenterItem.tab === "Vehicle"} />
            ) : tryCenterItem.image.endsWith('.mp4') ? (
              <WebGLVideoAvatar src={tryCenterItem.image} isVehicleModal={tryCenterItem.tab === "Vehicle"} />
            ) : tryCenterItem.tab === "Vehicle" ? (
              <WebGLImageAvatar src={tryCenterItem.image} />
            ) : (
              <Image src={tryCenterItem.image} alt={tryCenterItem.name} fill className="object-contain" />
            )}
          </div>
          <div className="flex items-center justify-center gap-1 mt-4 pointer-events-none">
            {renderStars(tryCenterItem.stars)}
          </div>
          <div className="mt-2 text-[20px] font-bold text-gray-900 pointer-events-none">
            {tryCenterItem.name}
          </div>
        </div>
      )}

      {/* ============ Theme Try Modal ============ */}
      {tryThemeItem && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4"
          onClick={() => setTryThemeItem(null)}
        >
          <div
            className="relative w-full max-w-[260px] flex flex-col items-center mt-12"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="relative w-[230px] h-[480px] rounded-3xl border-[4px] border-yellow-300 overflow-hidden shadow-2xl bg-black">
              <Image src={tryThemeItem.image} alt={tryThemeItem.name} fill className="object-cover" />
            </div>

            <div className="flex items-center justify-center gap-1 mt-4">
              {renderStars(tryThemeItem.stars)}
            </div>

            <div className="mt-2 text-[20px] font-bold text-white tracking-wide text-center drop-shadow-md">
              {tryThemeItem.name}
            </div>
          </div>
        </div>
      )}

      {/* ============ Frame Try Card ============ */}
      {tryCenterItem && tryCenterItem.tab === "Avatar Frame" && (() => {
        const item = tryCenterItem;
        const isOwned = ownedIds.has(item.id);
        const isEquipped = equippedIds.has(item.id);
        return (
          <div
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-6"
            onClick={() => setTryCenterItem(null)}
          >
            <div
              className="relative bg-white rounded-2xl w-[300px] h-[300px] max-w-[85vw] max-h-[85vw] shadow-2xl"
              onClick={(e) => e.stopPropagation()}
            >
              <div className="absolute left-1/2 -translate-x-1/2 -top-[55px] w-[110px] h-[110px]">
                <div className="absolute inset-0 flex items-center justify-center">
                  <div className="relative w-[80px] h-[80px] rounded-full overflow-hidden bg-gray-200 flex items-center justify-center shadow-md z-10">
                    {userPhoto ? (
                      <img
                        src={userPhoto}
                        alt="User"
                        className="w-full h-full object-cover"
                        onError={(e) => {
                          e.currentTarget.style.display = 'none';
                          const next = e.currentTarget.nextElementSibling as HTMLElement | null;
                          if (next) next.classList.remove('hidden');
                        }}
                      />
                    ) : null}
                    <div className={`w-full h-full flex items-center justify-center bg-gray-500 text-white text-[32px] font-bold ${userPhoto ? 'hidden' : ''}`}>
                      {avatarLetter}
                    </div>
                  </div>
                </div>

                <div className="absolute inset-0 flex items-center justify-center z-20 pointer-events-none">
                  {item.tryVideo ? (
                    <WebGLVideoAvatar src={item.tryVideo} />
                  ) : item.image.endsWith('.mp4') ? (
                    <WebGLVideoAvatar src={item.image} />
                  ) : (
                    <WebGLImageAvatar src={item.image} />
                  )}
                </div>
              </div>

              <div className="absolute inset-x-0 top-[70px] bottom-0 flex flex-col items-center px-4">
                <div className="text-[17px] font-bold text-gray-900 text-center leading-tight">
                  {item.name}
                </div>

                <div className="mt-1 flex items-center justify-center gap-1 text-[13px] font-medium text-gray-500">
                  <Clock size={13} strokeWidth={2.5} />
                  <span>{item.duration}</span>
                </div>

                <div className="mt-2 flex items-center justify-center gap-1.5">
                  <div className="relative w-4 h-4 flex items-center justify-center shrink-0">
                    <WebGLCoinIcon src="/file_00000000e56882119c217d508b6733dc.png" />
                  </div>
                  <span className="text-[15px] font-bold text-gray-900 tracking-tight">
                    {item.price}
                  </span>
                </div>

                <button
                  type="button"
                  disabled={buying === item.id}
                  onClick={async () => {
                    if (isOwned) {
                      await handleEquipToggle(item);
                    } else {
                      if (balance < parsePrice(item.price)) {
                        alert('Insufficient balance');
                        return;
                      }
                      await handleBuy(item);
                    }
                    setTryCenterItem(null);
                  }}
                  className="mt-3 w-full h-[42px] rounded-full bg-[#1d4ed8] text-white text-[14px] font-bold flex items-center justify-center transition-colors hover:bg-blue-800 disabled:opacity-40 disabled:cursor-not-allowed"
                >
                  {buying === item.id ? '...' : isOwned ? (isEquipped ? 'Equipped' : 'Equip') : 'Buy'}
                </button>

                <button
                  type="button"
                  onClick={() => setTryCenterItem(null)}
                  className="mt-2 w-full h-[42px] rounded-full bg-white border border-gray-300 text-gray-600 text-[14px] font-bold flex items-center justify-center transition-colors hover:bg-gray-50"
                >
                  Cancel
                </button>
              </div>
            </div>
          </div>
        );
      })()}

      {/* ============ Chat Bubble Try Modal — FIXED: NO BLACK BORDER + SMALLER CENTERED ============ */}
      {tryChatBubbleItem && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4"
          onClick={() => setTryChatBubbleItem(null)}
        >
          <div
            className="w-full max-w-[260px] bg-white rounded-[22px] flex flex-col items-center pt-5 pb-4 px-4 shadow-2xl"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-center gap-2 mb-3">
              <div className="w-4 h-4 rounded-full border-[5px] border-gray-300 bg-white flex items-center justify-center">
                <div className="w-1.5 h-1.5 rounded-full bg-gray-300"></div>
              </div>
              <span className="text-[16px] font-semibold text-gray-400 tracking-wide">3D</span>
            </div>

            {/* Chat bubble preview — ADDED ZYORA TEXT */}
            <div className="relative w-[120px] h-[120px] mb-3 flex items-center justify-center">
              <Image
                src={tryChatBubbleItem.image}
                alt={tryChatBubbleItem.name}
                fill
                className="object-contain"
              />
              <span className="absolute text-white font-bold text-[24px] tracking-wide drop-shadow-[0_0_8px_rgba(255,215,0,0.9)] z-10 font-sans">
                Zyora
              </span>
            </div>

            <div className="flex flex-col items-center justify-center mb-3">
              <div className="flex items-center justify-center gap-1.5 mb-0.5">
                <div className="relative w-7 h-7 flex items-center justify-center shrink-0">
                  <WebGLCoinIcon src="/file_00000000e56882119c217d508b6733dc.png" />
                </div>
                <span className="text-[28px] font-extrabold text-black tracking-tight leading-none">
                  {tryChatBubbleItem.price.replace(/,/g, '')}
                </span>
              </div>
              <div className="flex items-center justify-center gap-1 opacity-60">
                <div className="relative w-3.5 h-3.5 flex items-center justify-center shrink-0">
                  <WebGLCoinIcon src="/file_00000000e56882119c217d508b6733dc.png" />
                </div>
                <span className="text-[14px] font-bold text-gray-400 line-through decoration-gray-400 decoration-2">
                  {String(parseInt(tryChatBubbleItem.price.replace(/,/g, ''), 10) * 2)}
                </span>
              </div>
            </div>

            <button
              type="button"
              onClick={() => {
                handleBuy(tryChatBubbleItem);
                setTryChatBubbleItem(null);
              }}
              disabled={buying === tryChatBubbleItem.id}
              className="w-full h-[42px] rounded-full bg-[#00c88c] text-white text-[16px] font-bold flex items-center justify-center transition-colors hover:bg-[#00b07a] disabled:opacity-50 disabled:cursor-not-allowed shadow-md mb-2"
            >
              Buy
            </button>

            <button
              type="button"
              onClick={() => setTryChatBubbleItem(null)}
              className="w-full h-[42px] rounded-full bg-[#f2f2f2] text-[#8a8a8a] text-[16px] font-bold flex items-center justify-center transition-colors hover:bg-gray-200"
            >
              Cancel
            </button>
          </div>
        </div>
      )}

      {/* ============ Gift (Send) Modal ============ */}
      {giftItem && (
        <div
          className="fixed inset-0 z-[60] flex items-center justify-center bg-black/50 p-4"
          onClick={() => setGiftItem(null)}
        >
          <div
            className="w-full max-w-[330px] bg-white rounded-[24px] relative flex flex-col items-center pt-8 pb-6 px-5 shadow-2xl"
            onClick={(e) => e.stopPropagation()}
          >
            <h2 className="text-[22px] font-bold text-black mb-5">gift</h2>

            <div className="relative w-[140px] h-[140px] mb-3">
              {giftItem.tab === "Avatar Frame" ? (
                <div className="relative w-full h-full flex items-center justify-center">
                  <div className="relative w-[90px] h-[90px] rounded-full overflow-hidden flex items-center justify-center">
                    {userPhoto ? (
                      <img
                        src={userPhoto}
                        alt="User"
                        className="w-full h-full object-cover"
                        onError={(e) => {
                          e.currentTarget.style.display = 'none';
                          const next = e.currentTarget.nextElementSibling as HTMLElement | null;
                          if (next) next.classList.remove('hidden');
                        }}
                      />
                    ) : null}
                    <div className={`w-full h-full flex items-center justify-center bg-gray-400 text-white text-[32px] font-bold ${userPhoto ? 'hidden' : ''}`}>
                      {avatarLetter}
                    </div>
                  </div>
                  <div className="absolute inset-0 pointer-events-none flex items-center justify-center">
                    {giftItem.tryVideo ? (
                      <WebGLVideoAvatar src={giftItem.tryVideo} />
                    ) : giftItem.image.endsWith('.mp4') ? (
                      <WebGLVideoAvatar src={giftItem.image} />
                    ) : (
                      <WebGLImageAvatar src={giftItem.image} />
                    )}
                  </div>
                </div>
              ) : giftItem.tab === "Chat Bubble" ? (
                <div className="relative w-full h-full flex items-center justify-center">
                  <Image src={giftItem.image} alt={giftItem.name} fill className="object-contain" />
                  {/* ADDED ZYORA TEXT */}
                  <span className="absolute text-white font-bold text-[28px] tracking-wide drop-shadow-[0_0_10px_rgba(255,215,0,0.9)] z-10 font-sans">
                    Zyora
                  </span>
                </div>
              ) : giftItem.image.endsWith('.mp4') ? (
                <WebGLVideoAvatar src={giftItem.image} />
              ) : giftItem.tab === "Vehicle" ? (
                <WebGLImageAvatar src={giftItem.image} />
              ) : (
                <Image src={giftItem.image} alt={giftItem.name} fill className="object-contain" />
              )}

              <div className="absolute top-0 left-0 bg-[#ff7a3d] text-white text-[10px] font-bold px-1.5 py-[1px] rounded-md shadow-md leading-tight">
                {giftItem.duration}
              </div>
            </div>

            <div className="flex items-center justify-center gap-0.5 mb-2">
              {renderStars(giftItem.stars)}
            </div>

            <div className="text-[16px] font-medium text-gray-400 mb-4">Send To</div>

            <input
              type="text"
              placeholder="Enter Recipient's ID"
              className="w-full h-[56px] rounded-full bg-[#f3f4f6] text-[16px] px-5 outline-none border-none text-gray-700 placeholder-gray-400 mb-5 focus:ring-2 focus:ring-[#00c88c]/40"
            />

            <button
              type="button"
              className="w-full h-[56px] rounded-full bg-[#f3f4f6] text-white text-[18px] font-bold flex items-center justify-center transition-colors hover:bg-gray-200"
            >
              Search
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
