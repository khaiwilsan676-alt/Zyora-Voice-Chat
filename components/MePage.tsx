'use client'

import { apiUrl } from "../src/lib/api";
import { socket } from "../src/lib/socket";

import React, { useEffect, useState, useRef } from 'react'
import { ChevronRight, Copy, ArrowLeft, X } from 'lucide-react'
import SettingPage from './settingpage'
import PublicProfile from './PublicProfile'
import HurrySupport from './HurrySupport'
import LanguagePage from './LanguagePage'
import { translations, getTranslation, LanguageCode } from '../lib/translations'
import Wallet from './Wallet'
import StorePage from './StorePage'
import InviteFriends from './InviteFriends'
import Family from './Family'
import Level from './Level'
import Medal from './Medal'
import SellerCenter from './sellercenter'
import FollowList, { VisitorsPage } from './followlist'
import Svip from './Svip'

// ============ IndexedDB Functions for User Data ============
const USER_DB_NAME = 'UserDataDB';
const USER_STORE = 'userData';
const FEEDBACK_STORE = 'feedbacks';

const openUserDB = (): Promise<IDBDatabase> => {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(USER_DB_NAME, 1);

    request.onerror = () => reject(request.error);
    request.onsuccess = () => resolve(request.result);

    request.onupgradeneeded = () => {
      const db = request.result;
      if (!db.objectStoreNames.contains(USER_STORE)) {
        const userStore = db.createObjectStore(USER_STORE, { keyPath: 'uid' });
        userStore.createIndex('name', 'name', { unique: false });
        userStore.createIndex('updatedAt', 'updatedAt', { unique: false });
      }
      if (!db.objectStoreNames.contains(FEEDBACK_STORE)) {
        const feedbackStore = db.createObjectStore(FEEDBACK_STORE, { keyPath: 'id', autoIncrement: true });
        feedbackStore.createIndex('timestamp', 'timestamp', { unique: false });
      }
    };
  });
};

const getUserFromMongoDB = async (uid: string) => {
  if (!uid) return null;

  const response = await fetch(
    apiUrl(`/api/users?uid=${encodeURIComponent(uid)}`)
  );

  if (response.status === 404) {
    return null;
  }

  if (!response.ok) {
    throw new Error(`MongoDB user fetch failed: ${response.status}`);
  }

  const result = await response.json();
  return result?.user || null;
};

const saveUserToDB = async (userData: any) => {
  try {
    const db = await openUserDB();
    const transaction = db.transaction([USER_STORE], 'readwrite');
    const store = transaction.objectStore(USER_STORE);
    
    const completeUserData = {
      ...userData,
      cachedAt: Date.now(),
      updatedAt: new Date().toISOString(),
    };
    
    await new Promise<void>((resolve, reject) => {
      const request = store.put(completeUserData);
      request.onsuccess = () => resolve();
      request.onerror = () => reject(request.error);
    });

    db.close();
  } catch (error) {
    console.error('❌ User save error:', error);
  }
};

const loadUserFromDB = async (uid: string): Promise<any> => {
  try {
    const db = await openUserDB();
    const transaction = db.transaction([USER_STORE], 'readonly');
    const store = transaction.objectStore(USER_STORE);

    const userData = await new Promise<any>((resolve, reject) => {
      const request = store.get(uid);
      request.onsuccess = () => resolve(request.result);
      request.onerror = () => reject(request.error);
    });

    db.close();
    return userData || null;
  } catch (error) {
    console.error('❌ User load error:', error);
    return null;
  }
};

const saveFeedbackToDB = async (feedbackData: any) => {
  try {
    const db = await openUserDB();
    const transaction = db.transaction([FEEDBACK_STORE], 'readwrite');
    const store = transaction.objectStore(FEEDBACK_STORE);
    
    store.add({
      ...feedbackData,
      cachedAt: Date.now(),
      synced: false,
    });

    db.close();
  } catch (error) {
    console.error('Feedback save error:', error);
  }
};

interface MenuItem {
  id: string
  labelKey: keyof typeof translations['en'] | string
  src?: string
  icon?: React.ReactNode
  action?: string
  badge?: string
}

interface MePageProps {
  onNavigate?: (page: "home" | "message" | "me") => void
  onLogout?: () => void
  onPublicProfileChange?: (isOpen: boolean) => void
}

const menuItems: MenuItem[] = [
  { id: '1', labelKey: 'inviteFriends', src: '/IMG_20260915_225333.png' },
  { id: '2', labelKey: 'family', src: '/IMG_20260915_225349.png' },
  { id: '3', labelKey: 'level', src: '/IMG_20260915_225404.png' },
  { id: '4', labelKey: 'medal', src: '/IMG_20260915_225426.png' },
  { id: '5', labelKey: 'store', src: '/IMG_20260915_225447.png' },
  { id: '6', labelKey: 'bag', src: '/IMG_20260915_225506.png' },
  { id: '11', labelKey: 'Seller Center', src: '/IMG_20260915_225536.png' }
]

const bottomMenuItems: MenuItem[] = [
  {
    id: '7',
    labelKey: 'languageSetting',
    icon: (
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <circle cx="12" cy="12" r="10"></circle>
        <line x1="2" y1="12" x2="22" y2="12"></line>
        <path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10z"></path>
      </svg>
    )
  },
  {
    id: '8',
    labelKey: 'settings',
    icon: (
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M12 2l8 4.67v9.33L12 22l-8-4.67V6.67L12 2z"></path>
        <circle cx="12" cy="12" r="3"></circle>
      </svg>
    )
  },
  {
    id: '9',
    labelKey: 'customerService',
    icon: (
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#1E1E1E" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
        <path d="M5.5 11 V8.5 C5.5 5 8.2 3 12 3 C15.8 3 18.5 5 18.5 8.5 V15.2 C18.5 18.5 16.2 21 12 21"/>
        <path d="M3 10.2 V13.8 C3 13.8 3.5 15.2 4.2 15.2 H5.5 V9 H4.2 C3.5 9 3 9.5 3 10.2 Z"/>
        <path d="M18.5 9 V15.2 H19.8 C20.5 15.2 21 14.6 21 13.8 V10.2 C21 9.4 20.5 9 19.8 9 H18.5"/>
        <path d="M9.2 13.8 C9.2 15 10.3 16 12 16 C13.7 16 14.8 15 14.8 13.8"/>
      </svg>
    )
  },
  {
    id: '10',
    labelKey: 'helpFeedback',
    icon: (
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <circle cx="12" cy="12" r="10"></circle>
        <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"></path>
        <line x1="12" y1="17" x2="12.01" y2="17"></line>
      </svg>
    )
  }
]

const OFFICIAL_IDS = ['500001', '500002', '500003', '500004', '500005']
const ADMIN_IDS = ['700001', '700002', '700003']

const FEEDBACK_TYPES = [
  { id: 'bug', label: 'App Bug', icon: '' },
  { id: 'suggestion', label: 'Suggestion', icon: '' },
  { id: 'recharge', label: 'Recharge', icon: '' },
  { id: 'other', label: 'Others', icon: '' },
]

export const getOrCreateAccountNumber = (uid: string) => {
  if (!uid || uid === 'N/A') return { fullAccNum: '', displayAccNum: '' }

  if (OFFICIAL_IDS.includes(uid) || ADMIN_IDS.includes(uid)) {
    return { fullAccNum: uid, displayAccNum: uid }
  }

  if (typeof window !== 'undefined') {
    const savedAcc = localStorage.getItem('accountNumber')
    if (savedAcc) {
      return { fullAccNum: savedAcc, displayAccNum: savedAcc }
    }
  }

  let hash = 0
  for (let i = 0; i < uid.length; i++) {
    hash = (hash << 5) - hash + uid.charCodeAt(i)
    hash |= 0
  }
  const positiveHash = Math.abs(hash)
  const generated = String(10000000 + (positiveHash % 90000000))
  
  if (typeof window !== 'undefined') {
    localStorage.setItem('accountNumber', generated)
  }
  return { fullAccNum: generated, displayAccNum: generated }
}

const isValidName = (val?: string | null): boolean => {
  if (!val) return false;
  const clean = val.trim().toLowerCase();
  return clean !== '' && clean !== 'guest' && clean !== 'user' && clean !== 'null' && clean !== 'undefined';
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
      style={{
        ...style,
        opacity: isLoaded ? 1 : 0,
        transition: 'opacity 0.3s ease-in-out'
      }}
    />
  )
}

export default function MePage({ onLogout, onPublicProfileChange, onNavigate }: MePageProps) {
  const [currentView, setCurrentView] = useState<'me' | 'settings' | 'public_profile' | 'customer_service' | 'language'>('me')
  const [appLang, setAppLang] = useState<LanguageCode>('en')
  
  const [showFeedbackPage, setShowFeedbackPage] = useState(false)
  const [showWallet, setShowWallet] = useState(false)
  const [walletTab, setWalletTab] = useState<'wallet' | 'diamonds'>('wallet')
  const [showStore, setShowStore] = useState(false)
  const [storeInitialView, setStoreInitialView] = useState<'store' | 'bag'>('store')
  
  const [showInviteFriends, setShowInviteFriends] = useState(false)
  const [showFamily, setShowFamily] = useState(false)
  const [showLevel, setShowLevel] = useState(false)
  const [showMedal, setShowMedal] = useState(false)
  const [showSellerCenter, setShowSellerCenter] = useState(false)
  const [showSvip, setShowSvip] = useState(false)
  
  const [showFollowList, setShowFollowList] = useState(false)
  const [followListType, setFollowListType] = useState<'followers' | 'following' | 'visitors' | 'friends'>('followers')

  useEffect(() => {
    if (onPublicProfileChange) {
      onPublicProfileChange(
        currentView !== 'me' || 
        showFeedbackPage || 
        showWallet || 
        showStore ||
        showInviteFriends ||
        showFamily ||
        showLevel ||
        showMedal ||
        showSellerCenter ||
        showFollowList ||
        showSvip
      )
    }
  }, [showFeedbackPage, currentView, showWallet, showStore, showInviteFriends, showFamily, showLevel, showMedal, showSellerCenter, showFollowList, showSvip])
  
  const [selectedType, setSelectedType] = useState<string>('')
  const [problemDescription, setProblemDescription] = useState('')
  const [contactInfo, setContactInfo] = useState('')
  const [feedbackSubmitting, setFeedbackSubmitting] = useState(false)
  const [feedbackSuccess, setFeedbackSuccess] = useState(false)
  const [feedbackError, setFeedbackError] = useState<string | null>(null)

  // ✅ NEW: For image/video upload
  const [evidenceFile, setEvidenceFile] = useState<File | null>(null)
  const [evidencePreview, setEvidencePreview] = useState<string | null>(null)
  const [evidenceType, setEvidenceType] = useState<'image' | 'video' | null>(null)
  const evidenceInputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
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

  const t = getTranslation(appLang)

  const [user, setUser] = useState(() => {
    if (typeof window === 'undefined') {
      return { name: "", uid: "", accountNumber: "", displayAccountNumber: "", phone: "", photo: "" }
    }
    const uid = localStorage.getItem("userUID") || localStorage.getItem("userPhone") || localStorage.getItem("userId") || ""
    const localName = localStorage.getItem("userName")
    const validName = isValidName(localName) ? localName! : ""
    const acc = localStorage.getItem("accountNumber") || (uid ? getOrCreateAccountNumber(uid).fullAccNum : "")
    const photo = localStorage.getItem("userPhoto") || ""
    const phone = localStorage.getItem("userPhone") || ""

    return {
      name: validName,
      uid: uid,
      accountNumber: acc,
      displayAccountNumber: acc,
      phone: phone,
      photo: photo,
    }
  })

  useEffect(() => {
    const syncLocalInterval = setInterval(() => {
      const currentStoredName = localStorage.getItem("userName") || "";
      const currentStoredPhoto = localStorage.getItem("userPhoto") || "";
      
      setUser(prev => {
        if (prev.name !== currentStoredName || prev.photo !== currentStoredPhoto) {
          return {
            ...prev,
            name: isValidName(currentStoredName) ? currentStoredName : prev.name,
            photo: currentStoredPhoto || prev.photo
          };
        }
        return prev;
      });
    }, 500);

    return () => clearInterval(syncLocalInterval);
  }, []);

  const switchView = (view: 'me' | 'settings' | 'public_profile' | 'customer_service' | 'language') => {
    setCurrentView(view)
    if (onPublicProfileChange) {
      onPublicProfileChange(
        view !== 'me' || 
        showFeedbackPage || 
        showWallet || 
        showStore || 
        showInviteFriends || 
        showFamily || 
        showLevel || 
        showMedal ||
        showSellerCenter ||
        showFollowList ||
        showSvip
      )
    }
  }

  // ✅ NEW: Handle evidence file selection
  const handleEvidenceSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return

    if (file.type.startsWith('image/')) {
      setEvidenceType('image')
    } else if (file.type.startsWith('video/')) {
      setEvidenceType('video')
    } else {
      alert('Please select an image or video file')
      return
    }

    setEvidenceFile(file)

    // Create preview URL
    const reader = new FileReader()
    reader.onloadend = () => {
      setEvidencePreview(reader.result as string)
    }
    reader.readAsDataURL(file)
  }

  // ✅ NEW: Remove selected evidence
  const handleRemoveEvidence = () => {
    setEvidenceFile(null)
    setEvidencePreview(null)
    setEvidenceType(null)
    if (evidenceInputRef.current) {
      evidenceInputRef.current.value = ''
    }
  }

  const handleFeedbackSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFeedbackError(null);

    if (!selectedType) {
      setFeedbackError("Please select a type of issue");
      return;
    }
    if (!problemDescription.trim()) {
      setFeedbackError("Please describe your problem");
      return;
    }
    if (!contactInfo.trim()) {
      setFeedbackError("Please enter your contact information");
      return;
    }

    setFeedbackSubmitting(true);

    const feedbackData = {
      id: `fb_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`,
      userId: user.uid || '',
      userName: user.name || 'User',
      userAccountId: user.displayAccountNumber || user.accountNumber || '',
      userPhoto: user.photo || '',
      type: selectedType,
      typeLabel: FEEDBACK_TYPES.find(t => t.id === selectedType)?.label || selectedType,
      description: problemDescription.trim(),
      contactInfo: contactInfo.trim(),
      // ✅ NEW: include evidence info
      evidenceFileName: evidenceFile?.name || null,
      evidenceFileType: evidenceType,
      evidencePreview: evidencePreview || null,
      createdAt: new Date().toISOString(),
      timestamp: Date.now(),
      status: 'pending'
    };

    try {
      await saveFeedbackToDB(feedbackData);

      if (typeof window !== 'undefined') {
        if (!socket.connected) {
          socket.connect();
        }
        socket.emit("user_feedback", feedbackData);
      }
      
      setFeedbackSuccess(true);
      setSelectedType('');
      setProblemDescription('');
      setContactInfo('');
      handleRemoveEvidence();
      
      setTimeout(() => {
        setShowFeedbackPage(false);
        setFeedbackSuccess(false);
      }, 2000);

    } catch (error) {
      console.error("Error submitting feedback to Google Sheets:", error);
      try {
        await saveFeedbackToDB(feedbackData);
        setFeedbackSuccess(true);
        setSelectedType('');
        setProblemDescription('');
        setContactInfo('');
        handleRemoveEvidence();
        
        setTimeout(() => {
          setShowFeedbackPage(false);
          setFeedbackSuccess(false);
        }, 2000);
      } catch (dbError) {
        setFeedbackError("Failed to submit feedback. Please try again.");
      }
    } finally {
      setFeedbackSubmitting(false);
    }
  };

  useEffect(() => {
    const fetchUserData = async () => {
      try {
        const uid = localStorage.getItem("userUID") || 
                    localStorage.getItem("userPhone") || 
                    localStorage.getItem("userId") || 
                    "";

        if (!uid || uid === "N/A") return;

        const cachedUser = await loadUserFromDB(uid);
        if (cachedUser && (isValidName(cachedUser.name) || cachedUser.accountNumber || cachedUser.photo)) {
          const localStoredName = localStorage.getItem("userName");
          const finalName = isValidName(localStoredName) ? localStoredName! : (isValidName(cachedUser.name) ? cachedUser.name : "");
          const finalPhoto = localStorage.getItem("userPhoto") || cachedUser.photo || "";

          const lockedData = {
            ...cachedUser,
            name: finalName,
            photo: finalPhoto,
            accountNumber: cachedUser.accountNumber || localStorage.getItem("accountNumber") || getOrCreateAccountNumber(uid).fullAccNum,
            displayAccountNumber: cachedUser.accountNumber || localStorage.getItem("accountNumber") || getOrCreateAccountNumber(uid).fullAccNum,
          };
          setUser(lockedData);
          return;
        }

        try {
          const res = await getUserFromMongoDB(uid);
          const sheetData = res && (res.user || res.data || res);
          
          let resolvedName = localStorage.getItem("userName") || "";
          let photo = localStorage.getItem("userPhoto") || "";
          let phone = localStorage.getItem("userPhone") || "";
          let accNum = localStorage.getItem("accountNumber") || "";

          if (sheetData && (sheetData.id || sheetData.AppLongId || sheetData['App long ID'] || sheetData.Name || sheetData.name)) {
            const sName = sheetData.name || sheetData.Name;
            const sPhoto = sheetData.photo || sheetData.avatar || sheetData.Avtar || sheetData.image;
            const sAcc = sheetData.accountId || sheetData.accountNumber || sheetData['Account Number'];
            if (isValidName(sName) && !isValidName(resolvedName)) resolvedName = sName;
            if (sPhoto && !photo) photo = sPhoto;
            if (sAcc && !accNum) accNum = String(sAcc);
          }

          if (!accNum) {
            accNum = getOrCreateAccountNumber(uid).fullAccNum;
          }

          const finalLockedUser = {
            name: isValidName(resolvedName) ? resolvedName : "",
            uid: uid,
            accountNumber: accNum,
            displayAccountNumber: accNum,
            phone: phone,
            photo: photo,
          };

          await saveUserToDB(finalLockedUser);
          if (isValidName(finalLockedUser.name)) localStorage.setItem("userName", finalLockedUser.name);
          if (accNum) localStorage.setItem("accountNumber", accNum);
          if (photo) localStorage.setItem("userPhoto", photo);

          setUser(finalLockedUser);
        } catch (sheetError) {
          console.warn('Google Sheets user fetch error:', sheetError);
        }
      } catch (error) {
        console.error('Error locking user data:', error);
      }
    };

    fetchUserData();
  }, []);

  const handleCopyAccountNumber = () => {
    if (user.displayAccountNumber) {
      navigator.clipboard.writeText(user.displayAccountNumber)
      alert("ID Copied to clipboard!")
    }
  }

  const isSpecialUID = user.uid === 'HUSxSvQnabgU029dWYt1TUV04hd2' || user.uid === 'ADqW31RGBMaosOzy0HiqexKSD7h1'

  // ============================================================
  // ✅ FOLLOW LIST
  // ============================================================
  if (showFollowList) {
    if (followListType === 'visitors') {
      return (
        <VisitorsPage
          onBack={() => setShowFollowList(false)}
          onNavigate={onNavigate}
        />
      )
    }
    return (
      <FollowList
        activePage="me"
        onNavigate={onNavigate}
        onBack={() => setShowFollowList(false)}
        type={followListType}
      />
    )
  }

  if (showSvip) return <Svip onBack={() => setShowSvip(false)} />
  if (showInviteFriends) return <InviteFriends onBack={() => setShowInviteFriends(false)} />
  if (showFamily) return <Family onBack={() => setShowFamily(false)} />
  if (showLevel) return <Level onBack={() => setShowLevel(false)} />
  if (showMedal) return <Medal onBack={() => setShowMedal(false)} />
  if (showWallet) return <Wallet onBack={() => setShowWallet(false)} initialTab={walletTab} />
  if (showStore) return <StorePage onBack={() => setShowStore(false)} initialView={storeInitialView} />
  if (showSellerCenter) return <SellerCenter onBack={() => setShowSellerCenter(false)} />

  if (showFeedbackPage) {
    return (
      <div className="h-screen bg-gray-50 flex flex-col overflow-hidden">
        {/* Header */}
        <div
          className="flex items-center p-4 bg-white safe-top shrink-0"
          style={{ paddingTop: 'calc(env(safe-area-inset-top, 0px) + 24px)' }}
        >
          <button
            onClick={() => {
              setShowFeedbackPage(false);
              setFeedbackSuccess(false);
              setFeedbackError(null);
              setSelectedType('');
              setProblemDescription('');
              setContactInfo('');
              handleRemoveEvidence();
            }}
            className="p-2 hover:bg-gray-100 rounded-full transition-colors cursor-pointer"
          >
            <ArrowLeft size={24} className="text-gray-700" />
          </button>
          <h1 className="text-lg font-semibold text-gray-900 ml-3">{t.helpFeedback}</h1>
        </div>

        {/* Form Area */}
        <div className="flex-1 px-4 pb-4 overflow-y-auto">
          <div className="max-w-md mx-auto flex flex-col gap-4">
            {feedbackSuccess ? (
              <div className="bg-green-50 border border-green-200 rounded-md p-8 text-center mt-6">
                <div className="text-4xl mb-4">✅</div>
                <h2 className="text-xl font-bold text-green-700 mb-2">Thank You!</h2>
                <p className="text-green-600">Your feedback has been submitted successfully.</p>
              </div>
            ) : (
              <form onSubmit={handleFeedbackSubmit} className="flex flex-col gap-4">
                {/* 1. Type of Issue */}
                <div>
                  <h2 className="text-base font-semibold text-gray-800 mb-2">Type of Issue</h2>
                  <div className="grid grid-cols-2 gap-3">
                    {FEEDBACK_TYPES.map((type) => (
                      <button
                        key={type.id}
                        type="button"
                        onClick={() => setSelectedType(type.id)}
                        className={`py-3.5 px-3 rounded-md border-2 transition-all cursor-pointer ${
                          selectedType === type.id
                            ? 'border-blue-500 bg-blue-50 shadow-md'
                            : 'border-gray-200 bg-white hover:border-gray-300'
                        }`}
                      >
                        <div className={`text-sm font-medium ${
                          selectedType === type.id ? 'text-blue-700' : 'text-gray-700'
                        }`}>
                          {type.label}
                        </div>
                      </button>
                    ))}
                  </div>
                </div>

                {/* 2. Problem description - FIXED OVERLAP */}
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <h2 className="text-base font-semibold text-gray-800">Problem description</h2>
                    <span className="text-xs text-gray-400">{problemDescription.length}/300</span>
                  </div>
                  <textarea
                    value={problemDescription}
                    onChange={(e) => {
                      if (e.target.value.length <= 300) {
                        setProblemDescription(e.target.value);
                      }
                    }}
                    placeholder="Problem description"
                    maxLength={300}
                    rows={5}
                    className="w-full px-4 py-3 border border-gray-200 rounded-md focus:outline-none focus:border-blue-500 transition-colors text-sm text-gray-900 placeholder-gray-400 bg-gray-50 resize-none"
                  />
                </div>

                {/* 3. Image Evidence (Optional) - WITH FILE PICKER */}
                <div>
                  <h2 className="text-base font-semibold text-gray-800 mb-2">
                    Image Evidence<span className="text-gray-400 font-normal">(Optional)</span>
                  </h2>

                  {/* Hidden file input */}
                  <input
                    ref={evidenceInputRef}
                    type="file"
                    accept="image/*,video/*"
                    onChange={handleEvidenceSelect}
                    className="hidden"
                  />

                  {/* If no file selected -> show + button */}
                  {!evidencePreview ? (
                    <div
                      onClick={() => evidenceInputRef.current?.click()}
                      className="w-16 h-16 bg-gray-100 rounded-md border border-gray-200 flex items-center justify-center cursor-pointer hover:bg-gray-200 transition-colors"
                    >
                      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#9ca3af" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <line x1="12" y1="5" x2="12" y2="19"></line>
                        <line x1="5" y1="12" x2="19" y2="12"></line>
                      </svg>
                    </div>
                  ) : (
                    /* If file selected -> show preview with remove button */
                    <div className="relative w-16 h-16">
                      {evidenceType === 'image' ? (
                        <img
                          src={evidencePreview}
                          alt="Evidence preview"
                          className="w-16 h-16 rounded-md object-cover border border-gray-200"
                        />
                      ) : (
                        <video
                          src={evidencePreview}
                          className="w-16 h-16 rounded-md object-cover border border-gray-200"
                          muted
                        />
                      )}
                      <button
                        type="button"
                        onClick={handleRemoveEvidence}
                        className="absolute -top-2 -right-2 w-5 h-5 bg-red-500 rounded-full flex items-center justify-center shadow-md cursor-pointer hover:bg-red-600 transition-colors"
                      >
                        <X size={12} className="text-white" />
                      </button>
                    </div>
                  )}
                </div>

                {/* 4. Contact Information (Optional) */}
                <div>
                  <h2 className="text-base font-semibold text-gray-800 mb-2">
                    Contact Information<span className="text-gray-400 font-normal">(Optional)</span>
                  </h2>
                  <input
                    type="text"
                    value={contactInfo}
                    onChange={(e) => setContactInfo(e.target.value)}
                    placeholder="Phone or Email"
                    className="w-full px-5 py-3.5 border border-gray-200 rounded-full focus:outline-none focus:border-blue-500 transition-colors text-sm text-gray-900 placeholder-gray-400 bg-gray-50"
                  />
                </div>

                {/* Error */}
                {feedbackError && (
                  <div className="bg-red-50 border border-red-200 text-red-600 px-4 py-2 rounded-xl text-xs">
                    {feedbackError}
                  </div>
                )}

                {/* 5. Submit Button */}
                <div className="pt-1">
                  <button
                    type="submit"
                    disabled={feedbackSubmitting}
                    className="w-full bg-blue-600 text-white font-semibold py-3.5 rounded-full transition-all hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer shadow-lg shadow-blue-600/20 text-base"
                  >
                    {feedbackSubmitting ? (
                      <span className="flex items-center justify-center gap-2">
                        <svg className="animate-spin h-5 w-5" viewBox="0 0 24 24">
                          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" />
                          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                        </svg>
                        Submitting...
                      </span>
                    ) : (
                      'Submit'
                    )}
                  </button>
                  <p className="text-center text-gray-800 font-semibold text-sm mt-3">
                    Thank you! Feel free to give us any feedback.
                  </p>
                </div>
              </form>
            )}
          </div>
        </div>
      </div>
    );
  }

  if (currentView === 'language') return <LanguagePage onBack={() => switchView('me')} />
  if (currentView === 'customer_service') return <HurrySupport onBack={() => switchView('me')} />
  if (currentView === 'settings') return <SettingPage onBack={() => switchView('me')} onLogout={onLogout} />
  if (currentView === 'public_profile') return <PublicProfile onBack={() => switchView('me')} />

  const lockedNameDisplay = isValidName(user.name) 
    ? user.name 
    : (user.displayAccountNumber ? user.displayAccountNumber : (user.uid ? user.uid.substring(0, 8) : ''));

  const lockedAvatarLetter = lockedNameDisplay ? lockedNameDisplay.charAt(0).toUpperCase() : '';

  return (
    <div className="w-full min-h-screen bg-gray-50 pb-16 overflow-y-auto">
      {/* Profile Header */}
      <div
        className="px-4 pb-4 relative"
        style={{
          background: 'linear-gradient(to bottom, #3b82f6 0%, #eff6ff 70%, #f9fafb 100%)',
          paddingTop: '8vh'
        }}
      >
        <div className="flex items-start justify-between mb-6">
          <div className="flex items-center gap-4">
            <div className="relative w-20 h-20 ml-3 shrink-0">
              {user.photo ? (
                <img
                  src={user.photo}
                  className="w-20 h-20 rounded-full object-cover shadow-sm"
                  alt="Profile"
                  onError={(e) => {
                    e.currentTarget.style.display = 'none';
                    e.currentTarget.nextElementSibling?.classList.remove('hidden');
                  }}
                />
              ) : null}
              
              <div className={`w-20 h-20 bg-gray-600 rounded-full flex items-center justify-center text-4xl text-white font-bold shadow-sm ${user.photo ? 'hidden' : ''}`}>
                {lockedAvatarLetter}
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

            <div className="flex flex-col">
              <h2 className="text-2xl font-bold text-gray-900 mb-0.5">
                {lockedNameDisplay}
              </h2>

              <div className="flex items-center gap-1 mt-1">
                {isSpecialUID ? (
                  <div className="relative inline-block w-22">
                    <img
                      src="/1785138451098~2.jpg"
                      alt=""
                      className="absolute inset-0 w-full h-full object-contain rounded-md"
                    />
                    <p className="relative text-white font-bold px-3 py-1.5 z-10 text-xs" style={{ paddingLeft: '30px' }}>
                      {user.accountNumber || user.displayAccountNumber}
                    </p>
                  </div>
                ) : (
                  (user.displayAccountNumber || user.accountNumber) && (
                    <p className="text-gray-700 text-xs font-semibold">
                      ID: {user.displayAccountNumber || user.accountNumber}
                    </p>
                  )
                )}

                {(user.accountNumber || user.displayAccountNumber) && (
                  <button
                    onClick={handleCopyAccountNumber}
                    className="text-gray-600 hover:text-blue-900 transition-colors p-1 cursor-pointer"
                    title="Copy ID"
                  >
                    <Copy size={14} />
                  </button>
                )}
              </div>

              {user.phone && (
                <p className="text-gray-600 text-xs mt-0.5 font-semibold">
                  {user.phone}
                </p>
              )}
            </div>
          </div>

          <button
            onClick={() => switchView('public_profile')}
            className="p-2 hover:bg-white/20 rounded-full transition-colors mt-2 cursor-pointer"
            title="View Public Profile"
          >
            <ChevronRight className="text-gray-700" size={24} />
          </button>
        </div>

        {/* Stats Row */}
        <div className="flex items-center justify-between mt-2 px-0.5">
          <div
            className="flex-1 text-center cursor-pointer active:scale-95 transition-transform"
            onClick={() => {
              setFollowListType('friends');
              setShowFollowList(true);
            }}
          >
            <div className="text-xl font-bold text-gray-900">0</div>
            <div className="text-[11px] text-gray-600 mt-0.5">Friends</div>
          </div>
          
          <div className="text-gray-300 text-sm">|</div>

          <div 
            className="flex-1 text-center cursor-pointer active:scale-95 transition-transform"
            onClick={() => {
              setFollowListType('followers');
              setShowFollowList(true);
            }}
          >
            <div className="text-xl font-bold text-gray-900">1</div>
            <div className="text-[11px] text-gray-600 mt-0.5">{t.followers || 'Followers'}</div>
          </div>

          <div className="text-gray-300 text-sm">|</div>

          <div 
            className="flex-1 text-center cursor-pointer active:scale-95 transition-transform"
            onClick={() => {
              setFollowListType('following');
              setShowFollowList(true);
            }}
          >
            <div className="text-xl font-bold text-gray-900">0</div>
            <div className="text-[11px] text-gray-600 mt-0.5">{t.following || 'Following'}</div>
          </div>

          <div className="text-gray-300 text-sm">|</div>

          <div 
            className="flex-1 text-center cursor-pointer active:scale-95 transition-transform"
            onClick={() => {
              setFollowListType('visitors');
              setShowFollowList(true);
            }}
          >
            <div className="text-xl font-bold text-gray-900">1</div>
            <div className="text-[11px] text-gray-600 mt-0.5">{t.visitors || 'Visitors'}</div>
          </div>
        </div>
      </div>

      {/* Coins Images */}
      <div className="px-3 flex gap-1.5 mt-0 mb-3">
        <div 
          className="flex-1 rounded-lg overflow-hidden cursor-pointer active:scale-95 transition-transform"
          onClick={() => {
            setWalletTab('wallet');
            setShowWallet(true);
          }}
        >
          <img
            src="/file_00000000f26c81f88083bc494a0f229b.png"
            alt="Feature 1"
            className="w-full h-14 object-cover"
          />
        </div>
        <div 
          className="flex-1 rounded-lg overflow-hidden cursor-pointer active:scale-95 transition-transform"
          onClick={() => {
            setWalletTab('diamonds');
            setShowWallet(true);
          }}
        >
          <img
            src="/file_00000000fe848207abf557a118ff8a5b.png"
            alt="Feature 2"
            className="w-full h-14 object-cover"
          />
        </div>
      </div>

      {/* SVIP Banner */}
      <div 
        className="px-3 -mt-10 mb-6 cursor-pointer active:scale-95 transition-transform"
        onClick={() => setShowSvip(true)}
      >
        <img
          src="/file_00000000a25081fbb57574619596eed8.png"
          alt="SVIP Banner"
          className="w-full h-auto object-cover rounded-lg"
        />
      </div>

      {/* Top Menu Cards */}
      <div className="px-4 -mt-8">
        <div className="bg-white rounded-md overflow-hidden shadow-sm">
          {menuItems.map((item) => (
            <div 
              key={item.id}
              className="flex items-center gap-4 p-4 cursor-pointer hover:bg-gray-50 transition-colors"
              onClick={() => {
                if (item.id === '1') setShowInviteFriends(true);
                else if (item.id === '2') setShowFamily(true);
                else if (item.id === '3') setShowLevel(true);
                else if (item.id === '4') setShowMedal(true);
                else if (item.id === '5') {
                  setStoreInitialView('store');
                  setShowStore(true);
                }
                else if (item.id === '6') {
                  setStoreInitialView('bag');
                  setShowStore(true);
                }
                else if (item.id === '11') {
                  setShowSellerCenter(true);
                }
              }}
            >
              <div className={`flex items-center justify-center shrink-0 ${item.id === '11' ? 'w-10 h-10 -ml-1' : 'w-8 h-8'}`}>
                <img
                  src={item.src}
                  alt={typeof item.labelKey === 'string' && t[item.labelKey as keyof typeof translations['en']] ? t[item.labelKey as keyof typeof translations['en']] : String(item.labelKey)}
                  className={`w-full h-full object-cover ${item.id === '11' ? 'rounded-md' : ''}`}
                />
              </div>

              <div className="flex-1">
                <p className="font-semibold text-gray-900">
                  {typeof item.labelKey === 'string' && t[item.labelKey as keyof typeof translations['en']] 
                    ? t[item.labelKey as keyof typeof translations['en']] 
                    : String(item.labelKey)}
                </p>
              </div>
              {item.action && (
                <span className="text-sm font-medium text-gray-500">{item.action}</span>
              )}
              {item.badge && (
                <span className="bg-blue-300 text-xs font-bold px-2 py-1 rounded-full text-gray-900">
                  {item.badge}
                </span>
              )}
              <ChevronRight size={20} className="text-gray-400" />
            </div>
          ))}
        </div>
      </div>

      {/* Bottom Menu Cards */}
      <div className="px-4 mt-4 mb-8">
        <div className="bg-white rounded-md overflow-hidden shadow-sm">
          {bottomMenuItems.map((item) => (
            <div
              key={item.id}
              onClick={() => {
                if (item.id === '7') switchView('language')
                if (item.id === '8') switchView('settings')
                if (item.id === '9') switchView('customer_service')
                if (item.id === '10') setShowFeedbackPage(true)
              }}
              className="flex items-center gap-4 p-4 cursor-pointer hover:bg-gray-50 transition-colors"
            >
              <div className="w-8 h-8 flex items-center justify-center shrink-0 text-gray-700">
                {item.icon}
              </div>

              <div className="flex-1">
                <p className="font-semibold text-gray-900">
                  {typeof item.labelKey === 'string' && t[item.labelKey as keyof typeof translations['en']] 
                    ? t[item.labelKey as keyof typeof translations['en']] 
                    : String(item.labelKey)}
                </p>
              </div>
              {item.action && (
                <span className="text-sm font-medium text-gray-500">{item.action}</span>
              )}
              {item.badge && (
                <span className="bg-blue-300 text-xs font-bold px-2 py-1 rounded-full text-gray-900">
                  {item.badge}
                </span>
              )}
              <ChevronRight size={20} className="text-gray-400" />
            </div>
          ))}
        </div>
      </div>

    </div>
  )
}
