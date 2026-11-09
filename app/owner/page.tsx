'use client';

import React, { useState, useEffect, useRef } from 'react';
import { MoreVertical, X, ChevronDown, Search, Star, Sparkles, Gamepad2, Timer, Send, ImageIcon, Megaphone, CheckCircle2, MessageSquare, Bot, ArrowLeft, User, Key, Lock, Camera, ShieldAlert, Eye, EyeOff } from 'lucide-react';
import { auth } from '@/src/lib/firebase';
import { onAuthStateChanged } from 'firebase/auth';
import { socket } from '@/src/lib/socket';

import Reports from '@/components/Reports';
import OwnerBan from '@/components/ownerban';
import { apiUrl } from "@/src/lib/api";

// ==============================================================
// VALID LOGIN CREDENTIALS
// ==============================================================
const VALID_CREDENTIALS = {
  accountName: 'Zyora Owner',
  password: 'Hurry.in-owner & ceo',
  secretKey: '18 July 2026'
};

// ==============================================================
// INDEXEDDB HELPERS
// ==============================================================
const loadAllFeedbacksFromIndexedDB = async (): Promise<any[]> => {
  if (typeof window === 'undefined') return [];
  return new Promise((resolve) => {
    try {
      const request = indexedDB.open("HurryFeedbackDB", 1);
      request.onerror = () => resolve([]);
      request.onupgradeneeded = (e: any) => {
        const db = e.target.result;
        if (!db.objectStoreNames.contains("userFeedbacks")) {
          db.createObjectStore("userFeedbacks", { keyPath: "id" });
        }
      };
      request.onsuccess = () => {
        const db = request.result;
        if (!db.objectStoreNames.contains("userFeedbacks")) {
          db.close();
          resolve([]);
          return;
        }
        const transaction = db.transaction(["userFeedbacks"], "readonly");
        const store = transaction.objectStore("userFeedbacks");
        const getAllReq = store.getAll();
        getAllReq.onsuccess = () => {
          db.close();
          resolve(getAllReq.result || []);
        };
        getAllReq.onerror = () => {
          db.close();
          resolve([]);
        };
      };
    } catch (err) {
      resolve([]);
    }
  });
};

const saveFeedbackToIndexedDB = async (feedbackData: any) => {
  if (typeof window === 'undefined' || !feedbackData?.id) return;
  try {
    const db = await new Promise<IDBDatabase>((resolve, reject) => {
      const request = indexedDB.open("HurryFeedbackDB", 1);
      request.onerror = () => reject(request.error);
      request.onsuccess = () => resolve(request.result);
      request.onupgradeneeded = (e: any) => {
        const database = e.target.result;
        if (!database.objectStoreNames.contains("userFeedbacks")) {
          database.createObjectStore("userFeedbacks", { keyPath: "id" });
        }
      };
    });
    const transaction = db.transaction(["userFeedbacks"], "readwrite");
    const store = transaction.objectStore("userFeedbacks");
    store.put(feedbackData);
    db.close();
  } catch (err) {
    console.error("Error saving user feedback to IndexedDB:", err);
  }
};

const loadAllSupportChatsFromIndexedDB = async (): Promise<any[]> => {
  if (typeof window === 'undefined') return [];
  return new Promise((resolve) => {
    try {
      const request = indexedDB.open("HurrySupportDB", 1);
      request.onerror = () => resolve([]);
      request.onupgradeneeded = (e: any) => {
        const db = e.target.result;
        if (!db.objectStoreNames.contains("supportChats")) {
          db.createObjectStore("supportChats", { keyPath: "userId" });
        }
      };
      request.onsuccess = () => {
        const db = request.result;
        if (!db.objectStoreNames.contains("supportChats")) {
          db.close();
          resolve([]);
          return;
        }
        const transaction = db.transaction(["supportChats"], "readonly");
        const store = transaction.objectStore("supportChats");
        const getAllReq = store.getAll();
        getAllReq.onsuccess = () => {
          db.close();
          resolve(getAllReq.result || []);
        };
        getAllReq.onerror = () => {
          db.close();
          resolve([]);
        };
      };
    } catch (err) {
      resolve([]);
    }
  });
};

const saveSupportChatToIndexedDB = async (chatData: any) => {
  if (typeof window === 'undefined' || !chatData?.userId) return;
  try {
    const db = await new Promise<IDBDatabase>((resolve, reject) => {
      const request = indexedDB.open("HurrySupportDB", 1);
      request.onerror = () => reject(request.error);
      request.onsuccess = () => resolve(request.result);
      request.onupgradeneeded = (e: any) => {
        const database = e.target.result;
        if (!database.objectStoreNames.contains("supportChats")) {
          database.createObjectStore("supportChats", { keyPath: "userId" });
        }
      };
    });
    const transaction = db.transaction(["supportChats"], "readwrite");
    const store = transaction.objectStore("supportChats");
    store.put(chatData);
    db.close();
  } catch (err) {
    console.error("Error saving support chat to IndexedDB:", err);
  }
};

// ==============================================================
// TYPES & DATA
// ==============================================================
interface UserRecord {
  id: string;
  name: string;
  username: string;
  hurryId: string;
  email: string;
  role: string;
  image?: string;
}

const fallbackUsers: UserRecord[] = [];

const AVAILABLE_TAGS = [
  { id: 'adminTag', name: 'Admin', emoji: '🛡️' },
  { id: 'officialTag', name: 'Official', emoji: '✅' },
  { id: 'vipTag', name: 'VIP', emoji: '⭐' },
  { id: 'premiumTag', name: 'Premium', emoji: '💎' },
];

const ALL_STORE_THEMES = [
  { id: "t1", name: "Seafood Deluxe", image: "/IMG-20260904-WA0004.jpg", category: "Theme", stars: 4, price: "2,700,000", duration: "30D" },
  { id: "t2", name: "Night Sky Aurora", image: "/IMG-20260904-WA0005.jpg", category: "Theme", stars: 5, price: "2,400,000", duration: "30D" },
  { id: "t3", name: "Seafood Coral", image: "/IMG-20260904-WA0006.jpg", category: "Theme", stars: 4, price: "2,700,000", duration: "30D" },
  { id: "t4", name: "Night Sky Galaxy", image: "/IMG-20260904-WA0007.jpg", category: "Theme", stars: 5, price: "2,400,000", duration: "30D" },
  { id: "t5", name: "Seafood Lagoon", image: "/IMG-20260904-WA0040.jpg", category: "Theme", stars: 4, price: "2,700,000", duration: "30D" },
  { id: "t6", name: "Night Sky Midnight", image: "/IMG-20260904-WA0041.jpg", category: "Theme", stars: 5, price: "2,400,000", duration: "30D" },
];

const ALL_GIFTS = [
  { id: 1, name: "Rose", coins: 10, image: "/IMG_20260815_103351.jpg", type: "Hot" },
  { id: 2, name: "Heart", coins: 99, image: "/IMG_20260815_103351.jpg", type: "Hot" },
  { id: 3, name: "Car", coins: 500, image: "/IMG_20260815_103351.jpg", type: "Hot" },
  { id: 4, name: "Crown", coins: 1000, image: "/IMG_20260815_103351.jpg", type: "Hot" },
  { id: 5, name: "Rocket", coins: 2000, image: "/IMG_20260815_103351.jpg", type: "Hot" },
  { id: 6, name: "Castle", coins: 5000, image: "/IMG_20260815_103351.jpg", type: "Hot" },
  { id: 7, name: "Diamond", coins: 10000, image: "/IMG_20260815_103351.jpg", type: "Hot" },
  { id: 8, name: "Yacht", coins: 20000, image: "/IMG_20260815_103351.jpg", type: "Hot" },
  { id: 9, name: "Plane", coins: 50000, image: "/IMG_20260815_103351.jpg", type: "Hot" },
  { id: 10, name: "Island", coins: 100000, image: "/IMG_20260815_103351.jpg", type: "Hot" },
  { id: 11, name: "Star", coins: 500000, image: "/IMG_20260815_103351.jpg", type: "Hot" },
  { id: 12, name: "Galaxy", coins: 1000000, image: "/IMG_20260815_103351.jpg", type: "Hot" },
  { id: 101, name: "Kiss", coins: 1999, image: "/IMG_20260906_000443.png", type: "Lucky" },
  { id: 102, name: "Nut", coins: 3999, image: "/IMG_20260906_000508.png", type: "Lucky" },
  { id: 103, name: "Mahjong", coins: 5999, image: "/IMG_20260906_000521.png", type: "Lucky" },
  { id: 104, name: "Clover", coins: 4250, image: "/IMG_20260906_000541.png", type: "Lucky" },
  { id: 105, name: "Charm", coins: 7000, image: "/IMG_20260906_000624.png", type: "Lucky" },
  { id: 106, name: "Bouquet", coins: 10999, image: "/IMG_20260906_000643.png", type: "Lucky" },
  { id: 107, name: "Leaves", coins: 6799, image: "/IMG_20260906_000713.png", type: "Lucky" },
  { id: 108, name: "Crystal", coins: 2999, image: "/IMG_20260906_000756.png", type: "Lucky" },
  { id: 109, name: "Candy", coins: 15499, image: "/IMG_20260906_000814.png", type: "Lucky" },
  { id: 110, name: "Pop", coins: 4000, image: "/IMG_20260906_000832.png", type: "Lucky" },
  { id: 111, name: "Scarecrow", coins: 7500, image: "/IMG_20260906_000850.png", type: "Lucky" },
];

const FRUIT_DETAILS: Record<number, { name: string; emoji: string; img: string; mult: string }> = {
  0: { name: 'Apple', emoji: '🍎', img: '/IMG_20260908_192143.png', mult: '5x' },
  1: { name: 'Orange', emoji: '🍊', img: '/IMG_20260908_192120.png', mult: '10x' },
  2: { name: 'Grapes', emoji: '🍇', img: '/IMG_20260908_191941.png', mult: '5x' },
  3: { name: 'Watermelon', emoji: '🍉', img: '/IMG_20260908_192013.png', mult: '10x' },
  5: { name: 'Strawberry', emoji: '🍓', img: '/IMG_20260908_192050.png', mult: '15x' },
  6: { name: 'Banana', emoji: '🍌', img: '/IMG_20260908_191930.png', mult: '5x' },
  7: { name: 'Peach', emoji: '🍑', img: '/IMG_20260908_191906.png', mult: '15x' },
  8: { name: 'Pineapple', emoji: '🍍', img: '/IMG_20260908_192203.png', mult: '5x' },
  10: { name: 'BAR 100x', emoji: '🎰', img: '/IMG_20260910_114515.png', mult: '100x' },
  11: { name: 'BAR 50x', emoji: '💎', img: '/IMG_20260910_114613.png', mult: '50x' },
};

const WILD_DETAILS: Record<number, { name: string; emoji: string; img: string; mult: string }> = {
  0: { name: 'Lion', emoji: '🦁', img: '/IMG_20260908_192143.png', mult: '5x' },
  1: { name: 'Tiger', emoji: '🐯', img: '/IMG_20260908_192120.png', mult: '10x' },
  2: { name: 'Bear', emoji: '🐻', img: '/IMG_20260908_191941.png', mult: '5x' },
  3: { name: 'Wolf', emoji: '🐺', img: '/IMG_20260908_192013.png', mult: '10x' },
  5: { name: 'Elephant', emoji: '🐘', img: '/IMG_20260908_192050.png', mult: '15x' },
  6: { name: 'Eagle', emoji: '🦅', img: '/IMG_20260908_191930.png', mult: '5x' },
  7: { name: 'Rhino', emoji: '🦏', img: '/IMG_20260908_191906.png', mult: '15x' },
  8: { name: 'Leopard', emoji: '🐆', img: '/IMG_20260908_192203.png', mult: '5x' },
  10: { name: 'Golden Dragon', emoji: '🐉', img: '/IMG_20260910_114515.png', mult: '100x' },
  11: { name: 'Phoenix', emoji: '🔥', img: '/IMG_20260910_114613.png', mult: '50x' },
};

// ==============================================================
// SIDEBAR ACCORDION COMPONENT
// ==============================================================
const SidebarCategory = ({ icon, title, items, activeItem, setActiveItem, setIsSidebarOpen }: any) => {
  const hasActiveChild = items.some((item: any) => item.id === activeItem);
  const [isExpanded, setIsExpanded] = useState(hasActiveChild);

  return (
    <div className="mb-2">
      <div
        onClick={() => setIsExpanded(!isExpanded)}
        className="flex items-center gap-3 px-6 py-2 text-[14px] font-bold text-white hover:bg-white/5 cursor-pointer transition-colors"
      >
        <span className="text-[16px] drop-shadow-md">{icon}</span>
        <span>{title}</span>
        <ChevronDown className={`w-4 h-4 ml-auto opacity-70 transition-transform duration-200 ${isExpanded ? 'rotate-180' : ''}`} />
      </div>

      {isExpanded && (
        <div className="flex flex-col mt-1">
          {items.map((item: any) => (
            <button
              key={item.id}
              onClick={() => {
                setActiveItem(item.id);
                if (window.innerWidth < 768) setIsSidebarOpen(false);
              }}
              className={`flex items-center gap-3 px-6 py-2.5 pl-[52px] text-[13px] font-bold transition-colors w-full text-left
                ${activeItem === item.id
                  ? 'text-white bg-[#8a92ff]/20 border-l-[3px] border-[#8a92ff]'
                  : 'text-gray-300 hover:text-white hover:bg-white/5 border-l-[3px] border-transparent'}
              `}
            >
              <span className="text-[14px] drop-shadow-sm">{item.icon}</span>
              {item.label}
            </button>
          ))}
        </div>
      )}
    </div>
  );
};

// ==============================================================
// LOGIN FORM COMPONENT
// ==============================================================
const LoginForm = ({ onLoginSuccess }: { onLoginSuccess: () => void }) => {
  const [accountName, setAccountName] = useState('');
  const [password, setPassword] = useState('');
  const [secretKey, setSecretKey] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (
      accountName.trim() === VALID_CREDENTIALS.accountName &&
      password === VALID_CREDENTIALS.password &&
      secretKey.trim() === VALID_CREDENTIALS.secretKey
    ) {
      setError('');
      onLoginSuccess();
    } else {
      setError('Invalid credentials. Please check and try again.');
    }
  };

  return (
    <div className="min-h-screen w-full flex items-center justify-center bg-gradient-to-br from-[#1a1c29] via-[#232641] to-[#1a1c29] p-4">
      <div className="w-full max-w-md bg-white/95 backdrop-blur-xl rounded-3xl shadow-2xl p-8 border border-white/20">
        <div className="text-center mb-8">
          <div className="w-16 h-16 mx-auto mb-3 rounded-2xl bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center shadow-lg">
            <ShieldAlert className="w-8 h-8 text-white" />
          </div>
          <h1 className="text-2xl font-black text-slate-800">Staff Panel Login</h1>
          <p className="text-xs text-slate-500 font-medium mt-1">Enter your credentials to continue</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-5">
          <div>
            <label className="block text-[11px] font-extrabold text-slate-600 uppercase tracking-wider mb-2">
              Account Name
            </label>
            <div className="relative">
              <div className="absolute left-3.5 top-3 text-slate-400">
                <User className="w-4 h-4" />
              </div>
              <input
                type="text"
                value={accountName}
                onChange={(e) => setAccountName(e.target.value)}
                placeholder="Zyora Owner"
                className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm font-medium text-slate-900 placeholder:text-slate-400 caret-slate-900 focus:outline-none focus:border-indigo-500 focus:bg-white transition-colors"
              />
            </div>
          </div>

          <div>
            <label className="block text-[11px] font-extrabold text-slate-600 uppercase tracking-wider mb-2">
              Password
            </label>
            <div className="relative">
              <div className="absolute left-3.5 top-3 text-slate-400">
                <Lock className="w-4 h-4" />
              </div>
              <input
                type={showPassword ? 'text' : 'password'}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Hurry.in-owner & ceo"
                className="w-full pl-10 pr-10 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm font-medium text-slate-900 placeholder:text-slate-400 caret-slate-900 focus:outline-none focus:border-indigo-500 focus:bg-white transition-colors"
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-3 top-3 text-slate-400 hover:text-slate-600"
              >
                {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
            </div>
          </div>

          <div>
            <label className="block text-[11px] font-extrabold text-slate-600 uppercase tracking-wider mb-2">
              Secret Key
            </label>
            <div className="relative">
              <div className="absolute left-3.5 top-3 text-slate-400">
                <Key className="w-4 h-4" />
              </div>
              <input
                type="text"
                value={secretKey}
                onChange={(e) => setSecretKey(e.target.value)}
                placeholder="18 July 2026"
                className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm font-medium text-slate-900 placeholder:text-slate-400 caret-slate-900 focus:outline-none focus:border-indigo-500 focus:bg-white transition-colors"
              />
            </div>
          </div>

          {error && (
            <div className="bg-red-50 border border-red-200 text-red-600 text-xs font-bold px-3 py-2.5 rounded-xl text-center">
              {error}
            </div>
          )}

          <button
            type="submit"
            className="w-full py-3 bg-gradient-to-r from-indigo-600 to-purple-600 text-white text-sm font-extrabold rounded-xl shadow-lg hover:from-indigo-700 hover:to-purple-700 active:scale-[0.98] transition-all"
          >
            Login
          </button>
        </form>
      </div>
    </div>
  );
};

// ==============================================================
// MAIN PAGE
// ==============================================================
export default function StaffPanel() {
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [authChecked, setAuthChecked] = useState(false);

  useEffect(() => {
    if (typeof window !== 'undefined') {
      const saved = localStorage.getItem('hurry_staff_panel_auth');
      if (saved === 'true') {
        setIsLoggedIn(true);
      }
      setAuthChecked(true);
    }
  }, []);

  const handleLoginSuccess = () => {
    if (typeof window !== 'undefined') {
      localStorage.setItem('hurry_staff_panel_auth', 'true');
    }
    setIsLoggedIn(true);
  };

  const [activeTab, setActiveTab] = useState('manage_users');
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);

  const [users, setUsers] = useState<UserRecord[]>([]);
  const [loadingUsers, setLoadingUsers] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedRoleFilter, setSelectedRoleFilter] = useState('All Roles');

  const [giftTabFilter, setGiftTabFilter] = useState<'All' | 'Hot' | 'Lucky'>('All');
  const [giftSearchQuery, setGiftSearchQuery] = useState('');

  const [themeSearchQuery, setThemeSearchQuery] = useState('');
  const [enlargedThemeImage, setEnlargedThemeImage] = useState<string | null>(null);

  const [livePrediction, setLivePrediction] = useState({
    round: 0,
    winnerIdx: 0,
    winnerImg: '',
    wName: 'Apple',
    wEmoji: '🍎',
    wMult: '5x',
    countdown: 0,
    phase: 'Betting'
  });

  const [wildPrediction, setWildPrediction] = useState({
    round: 0,
    winnerIdx: 0,
    winnerImg: '',
    wName: 'Lion',
    wEmoji: '🦁',
    wMult: '5x',
    countdown: 0,
    phase: 'Betting'
  });

  const [isTagModalOpen, setIsTagModalOpen] = useState(false);
  const [selectedTagUserData, setSelectedTagUserData] = useState<UserRecord | null>(null);
  const [selectedTags, setSelectedTags] = useState<string[]>([]);
  const [tagSuccess, setTagSuccess] = useState('');

  const [feedbacks, setFeedbacks] = useState<any[]>([]);
  const [selectedFeedback, setSelectedFeedback] = useState<any | null>(null);
  const [feedbackSearchQuery, setFeedbackSearchQuery] = useState('');
  const [selectedFeedbackTypeFilter, setSelectedFeedbackTypeFilter] = useState('All');

  const [supportChats, setSupportChats] = useState<any[]>([]);

  const [privateMessages, setPrivateMessages] = useState<any[]>([]);
  const [filteredPrivateMessages, setFilteredPrivateMessages] = useState<any[]>([]);
  const [privateMessageSearchQuery, setPrivateMessageSearchQuery] = useState('');
  const [loadingPrivateMessages, setLoadingPrivateMessages] = useState(false);

  const [selectedSupportUserId, setSelectedSupportUserId] = useState<string | null>(null);
  const [supportSearchQuery, setSupportSearchQuery] = useState('');

  const [selectedPair, setSelectedPair] = useState<any | null>(null);

  const supportChatEndRef = useRef<HTMLDivElement>(null);
  const privateChatEndRef = useRef<HTMLDivElement>(null);

  const compressImage = (file: File, maxWidth = 1200, maxHeight = 1200, quality = 0.8): Promise<string> => {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = (e) => {
        const img = new Image();
        img.onload = () => {
          let width = img.width;
          let height = img.height;
          if (width > maxWidth || height > maxHeight) {
            if (width / height > maxWidth / maxHeight) {
              height = Math.round((height * maxWidth) / width);
              width = maxWidth;
            } else {
              width = Math.round((width * maxHeight) / height);
              height = maxHeight;
            }
          }
          const canvas = document.createElement('canvas');
          canvas.width = width;
          canvas.height = height;
          const ctx = canvas.getContext('2d');
          if (!ctx) {
            resolve(e.target?.result as string);
            return;
          }
          ctx.drawImage(img, 0, 0, width, height);
          const dataUrl = canvas.toDataURL('image/jpeg', quality);
          resolve(dataUrl);
        };
        img.onerror = reject;
        img.src = e.target?.result as string;
      };
      reader.onerror = reject;
      reader.readAsDataURL(file);
    });
  };

  const [officialSender, setOfficialSender] = useState<'hurry_team_official' | 'hurry_system_official'>('hurry_team_official');
  const [officialText, setOfficialText] = useState('');
  const [officialImage, setOfficialImage] = useState('');
  const [officialSending, setOfficialSending] = useState(false);
  const [officialSuccess, setOfficialSuccess] = useState('');
  const [officialHistory, setOfficialHistory] = useState<any[]>([]);
  const officialFileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (typeof window !== 'undefined' && window.innerWidth >= 768) {
      setIsSidebarOpen(true);
    }
  }, []);

  useEffect(() => {
    let isMounted = true;

    const initData = async () => {
      const localFeedbacks = await loadAllFeedbacksFromIndexedDB();
      if (isMounted && localFeedbacks.length > 0) {
        setFeedbacks(localFeedbacks.sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0)));
      }

      const localChats = await loadAllSupportChatsFromIndexedDB();
      if (isMounted && localChats.length > 0) {
        setSupportChats(localChats.sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0)));
      }
    };

    initData();

    if (typeof window !== 'undefined') {
      if (!socket.connected) {
        socket.connect();
      }

      socket.emit("user_feedback_history_request");
      socket.emit("ai_support_history_request");
      socket.emit("official_message_history_request");

      const handleFeedbackHistoryResponse = (data: any) => {
        if (!isMounted || !Array.isArray(data?.feedbacks)) return;
        setFeedbacks(prev => {
          const map = new Map<string, any>();
          prev.forEach(item => map.set(item.id, item));
          data.feedbacks.forEach((item: any) => {
            if (item.id) {
              map.set(item.id, { ...map.get(item.id), ...item });
              saveFeedbackToIndexedDB(item);
            }
          });
          return Array.from(map.values()).sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0));
        });
      };

      const handleUserFeedback = (data: any) => {
        if (!isMounted || !data?.id) return;
        setFeedbacks(prev => {
          const map = new Map<string, any>();
          prev.forEach(item => map.set(item.id, item));
          map.set(data.id, data);
          saveFeedbackToIndexedDB(data);
          return Array.from(map.values()).sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0));
        });
      };

      const handleHistoryResponse = (data: any) => {
        if (!isMounted || !Array.isArray(data?.chats)) return;
        setSupportChats(prev => {
          const map = new Map<string, any>();
          prev.forEach(item => map.set(item.userId, item));
          data.chats.forEach((item: any) => {
            if (item.userId) {
              map.set(item.userId, { ...map.get(item.userId), ...item });
              saveSupportChatToIndexedDB(item);
            }
          });
          return Array.from(map.values()).sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0));
        });
      };

      const handleAiSupportMessage = (data: any) => {
        if (!isMounted || !data?.userId) return;
        setSupportChats(prev => {
          const map = new Map<string, any>();
          prev.forEach(item => map.set(item.userId, item));
          map.set(data.userId, data);
          saveSupportChatToIndexedDB(data);
          return Array.from(map.values()).sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0));
        });
      };

      socket.on("user_feedback_history_response", handleFeedbackHistoryResponse);
      socket.on("user_feedback", handleUserFeedback);
      socket.on("ai_support_history_response", handleHistoryResponse);
      socket.on("ai_support_message", handleAiSupportMessage);

      const handleOfficialHistoryResponse = (data: any) => {
        if (!isMounted || !Array.isArray(data?.messages)) return;
        setOfficialHistory(data.messages.sort((a: any, b: any) => (b.timestamp || 0) - (a.timestamp || 0)));
      };

      const handleOfficialBroadcast = (data: any) => {
        if (!isMounted || !data?.id) return;
        setOfficialHistory(prev => {
          if (prev.some(m => m.id === data.id)) return prev;
          return [data, ...prev].sort((a: any, b: any) => (b.timestamp || 0) - (a.timestamp || 0));
        });
      };

      socket.on("official_message_history_response", handleOfficialHistoryResponse);
      socket.on("official_broadcast_message", handleOfficialBroadcast);

      return () => {
        isMounted = false;
        socket.off("user_feedback_history_response", handleFeedbackHistoryResponse);
        socket.off("user_feedback", handleUserFeedback);
        socket.off("ai_support_history_response", handleHistoryResponse);
        socket.off("ai_support_message", handleAiSupportMessage);
        socket.off("official_message_history_response", handleOfficialHistoryResponse);
        socket.off("official_broadcast_message", handleOfficialBroadcast);
      };
    }
  }, []);

  useEffect(() => {
    if (selectedSupportUserId) {
      supportChatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
  }, [supportChats, selectedSupportUserId]);

  useEffect(() => {
    if (selectedPair) {
      privateChatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
  }, [privateMessages, selectedPair]);

  useEffect(() => {
    let isMounted = true;

    const fetchPrivateMessages = async () => {
      try {
        setLoadingPrivateMessages(true);
        const res = await fetch(apiUrl('/api/privateMessages'));
        if (res.ok) {
          const data = await res.json();
          if (isMounted) {
            setPrivateMessages(data.messages || []);
          }
        }
      } catch (err) {
        console.error('Failed to fetch private messages:', err);
      } finally {
        if (isMounted) {
          setLoadingPrivateMessages(false);
        }
      }
    };

    fetchPrivateMessages();
    const intervalId = setInterval(fetchPrivateMessages, 10000);

    return () => {
      isMounted = false;
      clearInterval(intervalId);
    };
  }, []);

  useEffect(() => {
    if (!privateMessageSearchQuery.trim()) {
      setFilteredPrivateMessages(privateMessages);
      return;
    }

    const query = privateMessageSearchQuery.toLowerCase();
    const filtered = privateMessages.filter((msg) => {
      return (
        (msg.text && msg.text.toLowerCase().includes(query)) ||
        (msg.senderId && msg.senderId.toLowerCase().includes(query)) ||
        (msg.receiverId && msg.receiverId.toLowerCase().includes(query)) ||
        (msg.senderName && msg.senderName.toLowerCase().includes(query))
      );
    });

    setFilteredPrivateMessages(filtered);
  }, [privateMessages, privateMessageSearchQuery]);

  useEffect(() => {
    let isMounted = true;

    const loadRealUsers = async () => {
      try {
        setLoadingUsers(true);
        let fetchedList: UserRecord[] = [];

        try {
          const res = await fetch(apiUrl('/api/users'));
          if (res.ok) {
            const data = await res.json();
            const rawUsers = Array.isArray(data) ? data : (data?.users || []);
            fetchedList = rawUsers.map((u: any, index: number) => ({
              id: String(u._id || u.id || u.uid || index + 1),
              name: u.name || u.displayName || u.userName || 'User',
              username: u.username || u.userName || `@${(u.name || 'user').toLowerCase().replace(/\s+/g, '')}`,
              hurryId: String(u.accountId || u.displayUserNumber || u.appLongId || u.hurryId || u.id || '—'),
              email: u.email || u.gmail || u.emailPhone || '—',
              role: u.role || 'NORMAL',
              image: u.image || u.photo || u.avatar || u.photoURL || ''
            }));
          }
        } catch (err) {
          console.warn('API users fetch fallback:', err);
        }

        let currentUserEmail = '';
        let currentUserId = '';
        let currentUserName = '';
        let currentUserPhoto = '';

        if (auth?.currentUser) {
          currentUserEmail = auth.currentUser.email || '';
          currentUserId = auth.currentUser.uid;
          currentUserName = auth.currentUser.displayName || '';
          currentUserPhoto = auth.currentUser.photoURL || '';
        }

        if (typeof window !== 'undefined') {
          const storedEmail = localStorage.getItem('userEmail');
          if (storedEmail && !currentUserEmail) currentUserEmail = storedEmail;

          const localUserData = localStorage.getItem('userData') || localStorage.getItem('user');
          if (localUserData) {
            try {
              const parsed = JSON.parse(localUserData);
              if (parsed.email && !currentUserEmail) currentUserEmail = parsed.email;
              if (parsed.gmail && !currentUserEmail) currentUserEmail = parsed.gmail;
              if (parsed.name && !currentUserName) currentUserName = parsed.name;
              if (parsed.image && !currentUserPhoto) currentUserPhoto = parsed.image;
              if (parsed.accountId || parsed.uid) currentUserId = String(parsed.accountId || parsed.uid);
            } catch (e) {}
          }
        }

        if (currentUserEmail || currentUserId) {
          const exists = fetchedList.some(u =>
            (currentUserEmail && u.email === currentUserEmail) ||
            u.hurryId === currentUserId ||
            u.id === currentUserId
          );
          if (!exists) {
            fetchedList.unshift({
              id: currentUserId || 'auth-current-user',
              name: currentUserName || 'Verified User',
              username: currentUserName ? `@${currentUserName.toLowerCase().replace(/\s+/g, '')}` : '@active_user',
              hurryId: currentUserId || '88100293',
              email: currentUserEmail || '—',
              role: 'NORMAL',
              image: currentUserPhoto || ''
            });
          } else {
            fetchedList = fetchedList.map(u => {
              if (
                (u.id === currentUserId || u.hurryId === currentUserId) &&
                currentUserEmail &&
                (!u.email || u.email === '—' || u.email.endsWith('@hurry.app') || u.email === 'user@gmail.com')
              ) {
                return { ...u, email: currentUserEmail };
              }
              return u;
            });
          }
        }

        if (isMounted) {
          setUsers(fetchedList);
        }
      } catch (error) {
        console.error('Error loading users:', error);
        if (isMounted) {
          setUsers(fallbackUsers);
        }
      } finally {
        if (isMounted) {
          setLoadingUsers(false);
        }
      }
    };

    loadRealUsers();

    const unsubscribe = onAuthStateChanged(auth, (authUser) => {
      if (authUser?.email) {
        setUsers(prev => prev.map(u => {
          if (u.id === authUser.uid || u.hurryId === authUser.uid) {
            return { ...u, email: authUser.email || u.email };
          }
          return u;
        }));
      }
    });

    return () => {
      isMounted = false;
      unsubscribe();
    };
  }, []);

  useEffect(() => {
    const clock = setInterval(() => {
      const CYCLE_MS = 40000;
      const now = Date.now();
      const roundNumber = (Math.floor(now / CYCLE_MS) % 10000) + 1000;
      const elapsed = now % CYCLE_MS;
      const seed = Math.sin(roundNumber) * 10000;
      const randomVal = seed - Math.floor(seed);

      let winnerIdx = 0;
      if (randomVal < 0.04) winnerIdx = 10;
      else if (randomVal < 0.06) winnerIdx = 11;
      else if (randomVal < 0.80) winnerIdx = [0, 2, 8, 6][Math.floor(randomVal * 100) % 4];
      else winnerIdx = [1, 5, 7, 3][Math.floor(randomVal * 100) % 4];

      const details = FRUIT_DETAILS[winnerIdx] || FRUIT_DETAILS[0];

      let currentPhase = 'Betting', currentCountdown = 0;
      if (elapsed < 30000) { currentPhase = 'Betting Phase'; currentCountdown = 30 - Math.floor(elapsed / 1000); }
      else if (elapsed < 35000) { currentPhase = 'Spinning Phase'; currentCountdown = 5 - Math.floor((elapsed - 30000) / 1000); }
      else { currentPhase = 'Result Phase'; currentCountdown = 5 - Math.floor((elapsed - 35000) / 1000); }

      setLivePrediction({
        round: roundNumber,
        winnerIdx,
        winnerImg: details.img,
        wName: details.name,
        wEmoji: details.emoji,
        wMult: details.mult,
        countdown: currentCountdown,
        phase: currentPhase
      });
    }, 100);

    return () => clearInterval(clock);
  }, []);

  useEffect(() => {
    const clock = setInterval(() => {
      const CYCLE_MS = 45000;
      const now = Date.now();
      const roundNumber = (Math.floor(now / CYCLE_MS) % 10000) + 5000;
      const elapsed = now % CYCLE_MS;
      const seed = Math.cos(roundNumber) * 10000;
      const randomVal = seed - Math.floor(seed);

      let winnerIdx = 0;
      if (randomVal < 0.05) winnerIdx = 10;
      else if (randomVal < 0.08) winnerIdx = 11;
      else if (randomVal < 0.75) winnerIdx = [1, 3, 6, 8][Math.floor(randomVal * 100) % 4];
      else winnerIdx = [0, 2, 5, 7][Math.floor(randomVal * 100) % 4];

      const details = WILD_DETAILS[winnerIdx] || WILD_DETAILS[0];

      let currentPhase = 'Betting', currentCountdown = 0;
      if (elapsed < 35000) { currentPhase = 'Betting Phase'; currentCountdown = 35 - Math.floor(elapsed / 1000); }
      else if (elapsed < 40000) { currentPhase = 'Spinning Phase'; currentCountdown = 5 - Math.floor((elapsed - 35000) / 1000); }
      else { currentPhase = 'Result Phase'; currentCountdown = 5 - Math.floor((elapsed - 40000) / 1000); }

      setWildPrediction({
        round: roundNumber,
        winnerIdx,
        winnerImg: details.img,
        wName: details.name,
        wEmoji: details.emoji,
        wMult: details.mult,
        countdown: currentCountdown,
        phase: currentPhase
      });
    }, 100);

    return () => clearInterval(clock);
  }, []);

  const openTagModal = (user: UserRecord) => {
    setSelectedTagUserData(user);
    setIsTagModalOpen(true);
    setTagSuccess('');
    setSelectedTags([]);
  };

  const handleAssignTags = () => {
    setTagSuccess('Tags updated successfully!');
    setTimeout(() => setIsTagModalOpen(false), 1500);
  };

  const toggleTag = (tagId: string) => {
    setSelectedTags(prev =>
      prev.includes(tagId) ? prev.filter(t => t !== tagId) : [...prev, tagId]
    );
  };

  const handleSearchQueryChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = e.target.value;
    setSearchQuery(val);

    if (val.trim()) {
      try {
        const res = await fetch(apiUrl(`/api/users?search=${encodeURIComponent(val)}&accountId=${encodeURIComponent(val)}`));
        if (res.ok) {
          const data = await res.json();
          const rawUsers = Array.isArray(data) ? data : (data?.users || []);
          const fetchedList = rawUsers.map((u: any, index: number) => ({
            id: String(u._id || u.id || u.uid || index + 1),
            name: u.name || u.displayName || u.userName || 'User',
            username: u.username || u.userName || `@${(u.name || 'user').toLowerCase().replace(/\s+/g, '')}`,
            hurryId: String(u.accountId || u.displayUserNumber || u.appLongId || u.hurryId || u.id || '—'),
            email: u.email || u.gmail || u.emailPhone || '—',
            role: u.role || 'NORMAL',
            image: u.image || u.photo || u.avatar || u.photoURL || ''
          }));

          setUsers(prev => {
            const newUsers = [...prev];
            fetchedList.forEach((u: any) => {
              if (!newUsers.some(existing => existing.id === u.id)) {
                newUsers.push(u);
              }
            });
            return newUsers;
          });
        }
      } catch (err) {
        console.warn('API users fetch fallback:', err);
      }
    }
  };

  const filteredUsers = users.filter(u => {
    const q = searchQuery.toLowerCase();
    const matchesSearch =
      u.name.toLowerCase().includes(q) ||
      u.username.toLowerCase().includes(q) ||
      u.email.toLowerCase().includes(q) ||
      u.hurryId.toLowerCase().includes(q);

    const matchesRole =
      selectedRoleFilter === 'All Roles' ||
      u.role.toUpperCase() === selectedRoleFilter.toUpperCase();

    return matchesSearch && matchesRole;
  });

  const filteredGifts = ALL_GIFTS.filter(g => {
    const typeOk = giftTabFilter === 'All' || g.type === giftTabFilter;
    const searchOk = !giftSearchQuery.trim() || g.name.toLowerCase().includes(giftSearchQuery.toLowerCase());
    return typeOk && searchOk;
  });

  const filteredThemes = ALL_STORE_THEMES.filter(t =>
    !themeSearchQuery.trim() || t.name.toLowerCase().includes(themeSearchQuery.toLowerCase())
  );

  const filteredSupportChats = supportChats.filter((chat) => {
    const q = supportSearchQuery.toLowerCase();
    return (
      (chat.userName || '').toLowerCase().includes(q) ||
      (chat.userId || '').toLowerCase().includes(q) ||
      (chat.userAccountId || '').toLowerCase().includes(q) ||
      (chat.userEmail || '').toLowerCase().includes(q)
    );
  });

  const privateConversations = React.useMemo(() => {
    const pairMap = new Map<string, any>();
    filteredPrivateMessages.forEach((msg) => {
      const sId = msg.senderId || msg.senderAccountId || '';
      const rId = msg.receiverId || msg.receiverAccountId || '';
      if (!sId || !rId) return;
      const key = [String(sId), String(rId)].sort().join('|||');
      const existing = pairMap.get(key);
      const ts = msg.timestamp || 0;
      if (!existing || ts > (existing.timestamp || 0)) {
        pairMap.set(key, {
          key,
          senderId: String(sId),
          senderName: msg.senderName || 'User',
          senderPhoto: msg.senderPhoto || '',
          receiverId: String(rId),
          receiverName: msg.receiverName || 'User',
          receiverPhoto: msg.receiverPhoto || '',
          timestamp: ts,
          lastMessage: msg.text || (msg.imageUrl ? '📷 Image' : '')
        });
      }
    });
    return Array.from(pairMap.values()).sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0));
  }, [filteredPrivateMessages]);

  const selectedPrivateMessages = React.useMemo(() => {
    if (!selectedPair) return [];
    return privateMessages
      .filter((m) => {
        const sId = String(m.senderId || m.senderAccountId || '');
        const rId = String(m.receiverId || m.receiverAccountId || '');
        const a = selectedPair.senderId;
        const b = selectedPair.receiverId;
        return (sId === a && rId === b) || (sId === b && rId === a);
      })
      .sort((a, b) => (a.timestamp || 0) - (b.timestamp || 0));
  }, [privateMessages, selectedPair]);

  if (!authChecked) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-[#1a1c29]">
        <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-white"></div>
      </div>
    );
  }

  if (!isLoggedIn) {
    return <LoginForm onLoginSuccess={handleLoginSuccess} />;
  }

  return (
    <div className="flex h-screen bg-white font-sans overflow-hidden">

      {isSidebarOpen && (
        <div className="fixed inset-0 bg-black/50 z-40 md:hidden" onClick={() => setIsSidebarOpen(false)} />
      )}

      <aside className={`fixed inset-y-0 left-0 z-50 w-[260px] bg-[#1a1c29] flex flex-col flex-shrink-0 h-full overflow-y-auto border-r border-[#2a2d3e] transition-transform duration-300 ease-in-out ${isSidebarOpen ? 'translate-x-0' : '-translate-x-full'}`}>

        <div className="p-6 pb-4 flex items-center justify-between">
          <div>
            <h1 className="text-white text-[19px] font-extrabold tracking-wide drop-shadow-md">Zyora</h1>
            <p className="text-[10px] text-gray-300 font-bold tracking-widest mt-0.5">STAFF CONTROL PANEL</p>
          </div>
          <button onClick={() => setIsSidebarOpen(false)} className="text-gray-400 hover:text-white">
            <X className="w-5 h-5" />
          </button>
        </div>

        <nav className="flex-1 mt-2 space-y-1 pb-6">
          <div className="flex items-center gap-3 px-6 py-3 text-[14px] font-bold text-white hover:bg-white/5 cursor-pointer transition-colors" onClick={() => setActiveTab('dashboard')}>
            <span className="text-[16px] drop-shadow-md">🏠</span>
            <span>Dashboard</span>
          </div>

          <SidebarCategory
            icon="👥" title="User Center" activeItem={activeTab} setActiveItem={setActiveTab} setIsSidebarOpen={setIsSidebarOpen}
            items={[{ id: 'manage_users', label: 'Manage Users', icon: '👤' }, { id: 'host_apps', label: 'Host Applications', icon: '📝' }]}
          />
          <SidebarCategory
            icon="🎙️" title="Live Rooms" activeItem={activeTab} setActiveItem={setActiveTab} setIsSidebarOpen={setIsSidebarOpen}
            items={[{ id: 'manage_rooms', label: 'Manage Rooms', icon: '📻' }]}
          />
          <SidebarCategory
            icon="🛒" title="Store" activeItem={activeTab} setActiveItem={setActiveTab} setIsSidebarOpen={setIsSidebarOpen}
            items={[{ id: 'themes', label: 'Themes', icon: '🎨' }, { id: 'special_ids', label: 'Special IDs', icon: '💎' }, { id: 'gift_catalog', label: 'Gift Catalog', icon: '🎁' }]}
          />
          <SidebarCategory
            icon="💰" title="Economy" activeItem={activeTab} setActiveItem={setActiveTab} setIsSidebarOpen={setIsSidebarOpen}
            items={[{ id: 'wallet', label: 'Master Wallet', icon: '💳' }, { id: 'history', label: 'Send History', icon: '📜' }, { id: 'revenue', label: 'Bean Revenue', icon: '📈' }]}
          />
          <SidebarCategory
            icon="📁" title="Content" activeItem={activeTab} setActiveItem={setActiveTab} setIsSidebarOpen={setIsSidebarOpen}
            items={[{ id: 'events', label: 'Events', icon: '🎪' }, { id: 'banners', label: 'Banners', icon: '🖼️' }]}
          />
          <SidebarCategory
            icon="🛡️" title="Moderation" activeItem={activeTab} setActiveItem={setActiveTab} setIsSidebarOpen={setIsSidebarOpen}
            items={[
              { id: 'feedback', label: 'User Feedback', icon: '💬' },
              { id: 'ai_chats', label: 'AI Chats', icon: '🤖' },
              { id: 'reports', label: 'Reports', icon: '🚫' },
              { id: 'bans', label: 'Bans', icon: '⛔' },
              { id: 'tickets', label: 'Private Chat', icon: '🎫' },
              { id: 'official_msg', label: 'Official Msg', icon: '📢' }
            ]}
          />
          <SidebarCategory
            icon="⚙️" title="Platform" activeItem={activeTab} setActiveItem={setActiveTab} setIsSidebarOpen={setIsSidebarOpen}
            items={[{ id: 'analytics', label: 'Analytics', icon: '📊' }, { id: 'agency', label: 'Agency Mgmt', icon: '🏢' }]}
          />

          <SidebarCategory
            icon="🎮" title="Game Management" activeItem={activeTab} setActiveItem={setActiveTab} setIsSidebarOpen={setIsSidebarOpen}
            items={[
              { id: 'game_fruit_party', label: 'Fruit Party', icon: '🍓' },
              { id: 'game_wild_party', label: 'Wild Party', icon: '🦁' }
            ]}
          />

          <div className="flex items-center gap-3 px-6 py-3 text-[14px] font-bold text-white hover:bg-white/5 cursor-pointer transition-colors mt-2" onClick={() => setActiveTab('system')}>
            <span className="text-[16px] drop-shadow-md">🛠️</span>
            <span>System</span>
          </div>
        </nav>
      </aside>

      <main
        className={`flex-1 flex flex-col h-full bg-[#f8f9fa] overflow-hidden transition-all duration-300 ${isSidebarOpen ? 'md:pl-[260px]' : 'md:pl-0'}`}
        onClick={(e) => {
          const target = e.target as HTMLElement;
          if (target.closest('button, a, input, select, textarea, [role="button"]')) return;
          if (typeof window !== 'undefined' && window.innerWidth >= 768 && isSidebarOpen) {
            setIsSidebarOpen(false);
          }
        }}
      >

        <header className="bg-white p-3 border-b border-slate-200 flex items-center sticky top-0 z-30 shadow-sm">
          <button
            onClick={() => setIsSidebarOpen(!isSidebarOpen)}
            className="p-2 text-slate-600 hover:bg-slate-100 rounded-lg active:scale-95 transition-transform"
          >
            <MoreVertical className="w-6 h-6" />
          </button>
        </header>

        {activeTab === 'manage_users' && (
          <div className="flex flex-col h-full bg-white">
            <div className="px-8 py-6 pb-4 border-b border-gray-100 flex items-center justify-between">
              <div>
                <h2 className="text-xl font-bold text-slate-800">Users</h2>
                <p className="text-xs text-slate-400 mt-0.5">Real user database records with Firebase Gmail authentication</p>
              </div>
              <span className="text-xs font-semibold px-3 py-1 bg-slate-100 text-slate-600 rounded-full">
                Total Users: {users.length}
              </span>
            </div>

            <div className="p-8 flex-1 overflow-y-auto">
              <div className="flex flex-col gap-4 mb-6">
                <div className="relative w-full max-w-3xl">
                  <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
                  <input
                    type="text"
                    value={searchQuery}
                    onChange={handleSearchQueryChange}
                    placeholder="Search by real name, email, Zyora ID, username..."
                    className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-lg text-sm focus:outline-none focus:border-blue-400 font-medium text-slate-900 placeholder:text-slate-400 caret-slate-900"
                  />
                </div>
                <div className="flex flex-wrap gap-2 items-center">
                  <select
                    value={selectedRoleFilter}
                    onChange={(e) => setSelectedRoleFilter(e.target.value)}
                    className="px-3 py-2 bg-white border border-slate-200 rounded-lg text-xs font-semibold text-slate-700 outline-none hover:bg-slate-50 cursor-pointer"
                  >
                    <option>All Roles</option>
                    <option>NORMAL</option>
                    <option>HOST</option>
                    <option>AGENCY</option>
                    <option>ADMIN</option>
                  </select>
                </div>
              </div>

              {loadingUsers ? (
                <div className="py-12 text-center text-slate-400 font-medium text-sm">
                  Loading real user database...
                </div>
              ) : (
                <div className="w-full overflow-x-auto">
                  <table className="w-full text-left border-collapse min-w-[800px]">
                    <thead>
                      <tr className="border-b border-slate-100 text-[10px] font-bold text-slate-400 uppercase tracking-wider">
                        <th className="py-4 pl-4 pr-2 w-12">S.No</th>
                        <th className="py-4 px-2 w-1/3">User Profile & Avatar</th>
                        <th className="py-4 px-2">User ID (MongoDB)</th>
                        <th className="py-4 px-2">Email (Firebase Gmail)</th>
                        <th className="py-4 px-2">Role</th>
                        <th className="py-4 pr-4 text-right">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-50">
                      {filteredUsers.map((u, idx) => (
                        <tr key={u.id} className="hover:bg-slate-50/50 transition-colors group">
                          <td className="py-3 pl-4 pr-2 text-xs font-bold text-slate-500">{idx + 1}</td>
                          <td className="py-3 px-2">
                            <div className="flex items-center gap-3.5">
                              {u.image ? (
                                <div className="w-10 h-10 rounded-full overflow-hidden border border-slate-200 shrink-0">
                                  <img
                                    src={u.image}
                                    alt={u.name}
                                    className="w-full h-full object-cover"
                                    onError={(e) => {
                                      (e.target as HTMLImageElement).style.display = 'none';
                                      const parent = (e.target as HTMLImageElement).parentElement;
                                      if (parent && !parent.querySelector('.avatar-fallback')) {
                                        const div = document.createElement('div');
                                        div.className = 'avatar-fallback w-full h-full bg-indigo-100 text-indigo-600 font-bold flex items-center justify-center text-base';
                                        div.textContent = u.name.charAt(0).toUpperCase();
                                        parent.appendChild(div);
                                      }
                                    }}
                                  />
                                </div>
                              ) : (
                                <div className="w-10 h-10 rounded-full bg-indigo-100 text-indigo-600 flex items-center justify-center font-bold text-base drop-shadow-sm shrink-0">
                                  {u.name.charAt(0).toUpperCase()}
                                </div>
                              )}
                              <div className="flex flex-col">
                                <span className="text-sm font-bold text-slate-800">{u.name}</span>
                                <span className="text-[11px] text-slate-400 font-medium">{u.username}</span>
                              </div>
                            </div>
                          </td>
                          <td className="py-3 px-2">
                            <span className="text-sm font-bold text-[#8a92ff] tracking-wide">{u.hurryId}</span>
                          </td>
                          <td className="py-3 px-2">
                            <span className="text-xs font-semibold text-slate-600 bg-slate-100 px-2.5 py-1 rounded-md inline-block">
                              {u.email}
                            </span>
                          </td>
                          <td className="py-3 px-2">
                            <span className={`px-2 py-1 rounded-md text-[10px] font-bold shadow-sm ${
                              u.role === 'HOST' ? 'bg-purple-50 text-purple-600 border border-purple-100' :
                              u.role === 'AGENCY' ? 'bg-orange-50 text-orange-600 border border-orange-100' :
                              u.role === 'ADMIN' ? 'bg-red-50 text-red-600 border border-red-100' :
                              'bg-blue-50 text-blue-600 border border-blue-100'
                            }`}>
                              {u.role}
                            </span>
                          </td>
                          <td className="py-3 pr-4 text-right">
                            <button onClick={() => openTagModal(u)} className="p-1.5 text-slate-400 hover:text-blue-600 hover:bg-blue-50 rounded-md transition-colors">
                              <MoreVertical className="w-4 h-4" />
                            </button>
                          </td>
                        </tr>
                      ))}

                      {filteredUsers.length === 0 && (
                        <tr>
                          <td colSpan={6} className="py-8 text-center text-slate-400 font-medium text-sm">
                            No users found matching query
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>
        )}

        {activeTab === 'game_fruit_party' && (
          <div className="p-8 max-w-5xl mx-auto w-full h-full overflow-y-auto">
            <h2 className="text-2xl font-bold text-slate-800 mb-6 flex items-center gap-2">
              <span>🍓</span> Fruit Party Prediction
            </h2>

            <div className="bg-white p-8 rounded-3xl shadow-sm border border-slate-100 space-y-6">
              <div className="flex items-center gap-2 border-b border-slate-100 pb-4">
                <Gamepad2 className="w-6 h-6 text-indigo-600" />
                <h3 className="text-xl font-bold text-slate-800">
                  Live Real-Time Winner Prediction
                  <span className="text-[10px] bg-red-100 text-red-600 font-bold px-2.5 py-0.5 rounded-full ml-2 align-middle">Owner Advance View</span>
                </h3>
              </div>

              <div className="flex flex-col md:flex-row items-center justify-between gap-8 bg-[#f8f9fa] border border-slate-200 p-8 rounded-2xl">
                <div className="flex flex-col items-center md:items-start gap-3">
                  <span className="text-xs text-slate-500 font-bold uppercase tracking-wider">Current Round ID</span>
                  <span className="text-4xl font-black text-slate-800">#{livePrediction.round}</span>
                  <div className="flex items-center gap-2 mt-1 bg-indigo-50 border border-indigo-200 px-4 py-2 rounded-xl">
                    <Timer className="w-5 h-5 text-indigo-600" />
                    <span className="text-sm font-bold text-indigo-700 uppercase tracking-wide">
                      {livePrediction.phase} - {livePrediction.countdown}s
                    </span>
                  </div>
                </div>

                <div className="flex flex-col items-center gap-3">
                  <div className="flex items-center gap-1.5 bg-emerald-100 text-emerald-800 px-3 py-1 rounded-full font-extrabold text-xs tracking-wider animate-pulse border border-emerald-200">
                    <Sparkles className="w-3.5 h-3.5" />
                    <span>PREDICTED WINNER</span>
                  </div>

                  <div className="w-40 h-40 bg-gradient-to-br from-indigo-500 via-purple-500 to-pink-500 p-1 rounded-3xl shadow-2xl relative overflow-hidden flex flex-col items-center justify-center">
                    <div className="w-full h-full bg-white rounded-[22px] flex flex-col items-center justify-center p-3">
                      {livePrediction.winnerImg ? (
                        <img src={livePrediction.winnerImg} alt={livePrediction.wName} className="w-16 h-16 object-contain drop-shadow-md mb-1" />
                      ) : (
                        <span className="text-5xl drop-shadow-md mb-1">{livePrediction.wEmoji}</span>
                      )}
                      <span className="font-black text-slate-800 text-base">{livePrediction.wName}</span>
                      <span className="text-xs font-bold text-indigo-600 bg-indigo-50 px-2 py-0.5 rounded-md mt-1">
                        {livePrediction.wMult}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}

        {activeTab === 'game_wild_party' && (
          <div className="p-8 max-w-5xl mx-auto w-full h-full overflow-y-auto">
            <h2 className="text-2xl font-bold text-slate-800 mb-6 flex items-center gap-2">
              <span>🦁</span> Wild Party Prediction
            </h2>

            <div className="bg-white p-8 rounded-3xl shadow-sm border border-slate-100 space-y-6">
              <div className="flex items-center gap-2 border-b border-slate-100 pb-4">
                <Gamepad2 className="w-6 h-6 text-orange-600" />
                <h3 className="text-xl font-bold text-slate-800">
                  Live Real-Time Winner Prediction
                  <span className="text-[10px] bg-red-100 text-red-600 font-bold px-2.5 py-0.5 rounded-full ml-2 align-middle">Owner Advance View</span>
                </h3>
              </div>

              <div className="flex flex-col md:flex-row items-center justify-between gap-8 bg-[#f8f9fa] border border-slate-200 p-8 rounded-2xl">
                <div className="flex flex-col items-center md:items-start gap-3">
                  <span className="text-xs text-slate-500 font-bold uppercase tracking-wider">Current Round ID</span>
                  <span className="text-4xl font-black text-slate-800">#{wildPrediction.round}</span>
                  <div className="flex items-center gap-2 mt-1 bg-orange-50 border border-orange-200 px-4 py-2 rounded-xl">
                    <Timer className="w-5 h-5 text-orange-600" />
                    <span className="text-sm font-bold text-orange-700 uppercase tracking-wide">
                      {wildPrediction.phase} - {wildPrediction.countdown}s
                    </span>
                  </div>
                </div>

                <div className="flex flex-col items-center gap-3">
                  <div className="flex items-center gap-1.5 bg-emerald-100 text-emerald-800 px-3 py-1 rounded-full font-extrabold text-xs tracking-wider animate-pulse border border-emerald-200">
                    <Sparkles className="w-3.5 h-3.5" />
                    <span>PREDICTED WINNER</span>
                  </div>

                  <div className="w-40 h-40 bg-gradient-to-br from-amber-500 via-orange-500 to-red-500 p-1 rounded-3xl shadow-2xl relative overflow-hidden flex flex-col items-center justify-center">
                    <div className="w-full h-full bg-white rounded-[22px] flex flex-col items-center justify-center p-3">
                      {wildPrediction.winnerImg ? (
                        <img src={wildPrediction.winnerImg} alt={wildPrediction.wName} className="w-16 h-16 object-contain drop-shadow-md mb-1" />
                      ) : (
                        <span className="text-5xl drop-shadow-md mb-1">{wildPrediction.wEmoji}</span>
                      )}
                      <span className="font-black text-slate-800 text-base">{wildPrediction.wName}</span>
                      <span className="text-xs font-bold text-orange-600 bg-orange-50 px-2 py-0.5 rounded-md mt-1">
                        {wildPrediction.wMult}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}

        {activeTab === 'themes' && (
          <div className="p-8 max-w-6xl mx-auto w-full h-full overflow-y-auto">
            <div className="flex items-center justify-between mb-6 border-b border-slate-200 pb-4">
              <h2 className="text-2xl font-bold text-slate-800">All Store Themes</h2>
              <span className="text-xs font-bold bg-indigo-50 text-indigo-600 px-3 py-1 rounded-full">
                {ALL_STORE_THEMES.length} Themes Active
              </span>
            </div>

            <div className="mb-6">
              <div className="relative w-full max-w-3xl">
                <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
                <input
                  type="text"
                  value={themeSearchQuery}
                  onChange={(e) => setThemeSearchQuery(e.target.value)}
                  placeholder="Search themes by name..."
                  className="w-full pl-10 pr-4 py-2.5 bg-white border border-slate-200 rounded-lg text-sm focus:outline-none focus:border-indigo-400 font-medium text-slate-900 placeholder:text-slate-400 caret-slate-900"
                />
              </div>
            </div>

            <div className="hidden md:flex items-center gap-4 px-5 py-3 text-[11px] font-extrabold text-slate-500 uppercase tracking-wider bg-slate-100 rounded-xl mb-2">
              <span className="w-10">S.No</span>
              <span className="flex-1">Name</span>
              <span className="w-40">Prize</span>
              <span className="w-24">Star</span>
              <span className="w-20">Theme</span>
              <span className="w-20 text-right">Days</span>
            </div>

            <div className="flex flex-col gap-2">
              {filteredThemes.map((theme, idx) => (
                <div
                  key={theme.id}
                  className="flex flex-col md:flex-row md:items-center gap-3 md:gap-4 px-4 md:px-5 py-4 bg-white border border-slate-100 rounded-xl hover:shadow-sm transition-all"
                >
                  <div className="w-10 text-xs font-bold text-slate-500">{idx + 1}</div>
                  <div className="flex-1 flex items-center gap-3 min-w-0">
                    <span className="font-bold text-slate-800 text-sm truncate">{theme.name}</span>
                  </div>
                  <div className="md:w-40 flex items-center gap-1.5 text-sm font-bold text-slate-700">
                    <span>🪙 {theme.price}</span>
                  </div>
                  <div className="md:w-24 flex items-center gap-1">
                    <span className="flex items-center gap-0.5 text-yellow-400">
                      {Array.from({ length: theme.stars }).map((_, i) => (
                        <Star key={i} className="w-3.5 h-3.5 fill-yellow-400" />
                      ))}
                    </span>
                  </div>
                  <div className="md:w-20">
                    <button
                      onClick={() => setEnlargedThemeImage(theme.image)}
                      className="w-12 h-12 rounded-lg overflow-hidden border border-slate-200 bg-slate-100 shrink-0 hover:ring-2 hover:ring-indigo-400 transition-all cursor-pointer"
                    >
                      <img
                        src={theme.image}
                        alt={theme.name}
                        className="w-full h-full object-cover"
                        onError={(e) => { (e.target as HTMLImageElement).src = '/default-theme.png'; }}
                      />
                    </button>
                  </div>
                  <div className="md:w-20 text-sm font-semibold text-slate-600 md:text-right">
                    {theme.duration}
                  </div>
                </div>
              ))}

              {filteredThemes.length === 0 && (
                <div className="py-10 text-center text-slate-400 text-sm font-medium bg-white rounded-xl border border-slate-100">
                  No themes found
                </div>
              )}
            </div>
          </div>
        )}

        {activeTab === 'gift_catalog' && (
          <div className="p-8 max-w-6xl mx-auto w-full h-full overflow-y-auto">
            <div className="flex items-center justify-between mb-6 border-b border-slate-200 pb-4">
              <h2 className="text-2xl font-bold text-slate-800">All Gift Catalog</h2>
              <div className="flex items-center gap-2">
                {(['All', 'Hot', 'Lucky'] as const).map((tab) => (
                  <button
                    key={tab}
                    onClick={() => setGiftTabFilter(tab)}
                    className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-colors ${
                      giftTabFilter === tab
                        ? 'bg-pink-600 text-white shadow-sm'
                        : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                    }`}
                  >
                    {tab} Gifts
                  </button>
                ))}
              </div>
            </div>

            <div className="mb-6">
              <div className="relative w-full max-w-3xl">
                <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
                <input
                  type="text"
                  value={giftSearchQuery}
                  onChange={(e) => setGiftSearchQuery(e.target.value)}
                  placeholder="Search gifts by name..."
                  className="w-full pl-10 pr-4 py-2.5 bg-white border border-slate-200 rounded-lg text-sm focus:outline-none focus:border-pink-400 font-medium text-slate-900 placeholder:text-slate-400 caret-slate-900"
                />
              </div>
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-4">
              {filteredGifts.map((gift, idx) => (
                <div key={gift.id} className="bg-white rounded-2xl p-4 border border-slate-200 shadow-sm flex flex-col items-center hover:shadow-md transition-shadow relative">
                  <span className="absolute top-2 left-2 text-[9px] font-extrabold px-2 py-0.5 rounded-full bg-slate-100 text-slate-500">
                    #{idx + 1}
                  </span>
                  <span className={`absolute top-2 right-2 text-[9px] font-extrabold px-2 py-0.5 rounded-full ${
                    gift.type === 'Hot' ? 'bg-red-100 text-red-600' : 'bg-purple-100 text-purple-600'
                  }`}>
                    {gift.type}
                  </span>

                  <div className="w-16 h-16 my-2 relative flex items-center justify-center">
                    <img
                      src={gift.image}
                      alt={gift.name}
                      className="w-full h-full object-contain"
                      onError={(e) => { (e.target as HTMLImageElement).src = '/default-avatar.png'; }}
                    />
                  </div>

                  <span className="font-bold text-slate-800 text-xs text-center">{gift.name}</span>
                  <div className="flex items-center gap-1 mt-1 text-xs font-extrabold text-amber-500">
                    <span>🪙</span>
                    <span>{gift.coins.toLocaleString()}</span>
                  </div>
                </div>
              ))}

              {filteredGifts.length === 0 && (
                <div className="col-span-full py-10 text-center text-slate-400 text-sm font-medium bg-white rounded-xl border border-slate-100">
                  No gifts found
                </div>
              )}
            </div>
          </div>
        )}

        {activeTab === 'tickets' && (
          <div className="flex flex-col h-full bg-white overflow-hidden">
            {!selectedPair ? (
              <>
                <div className="px-6 py-4 border-b border-slate-200 shrink-0">
                  <h2 className="text-xl font-black text-slate-800">Private Chat</h2>
                </div>

                <div className="px-6 py-4 border-b border-slate-100 shrink-0">
                  <div className="relative w-full max-w-2xl">
                    <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
                    <input
                      type="text"
                      value={privateMessageSearchQuery}
                      onChange={(e) => setPrivateMessageSearchQuery(e.target.value)}
                      placeholder="Search by user name, ID..."
                      className="w-full pl-10 pr-4 py-2.5 bg-slate-50 rounded-lg text-sm focus:outline-none font-medium text-slate-900 placeholder:text-slate-400 caret-slate-900"
                    />
                  </div>
                </div>

                <div className="hidden md:flex items-center gap-4 px-6 py-3 text-[11px] font-extrabold text-slate-500 uppercase tracking-wider bg-slate-50 border-b border-slate-100 shrink-0">
                  <span className="w-10">S.No</span>
                  <span className="flex-1">Sender</span>
                  <span className="flex-1">Receiver</span>
                  <span className="w-48 text-right">Date Time</span>
                </div>

                <div className="flex-1 overflow-y-auto">
                  {loadingPrivateMessages && privateMessages.length === 0 ? (
                    <div className="flex justify-center items-center h-40">
                      <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-500"></div>
                    </div>
                  ) : privateConversations.length === 0 ? (
                    <div className="flex flex-col items-center justify-center h-40 text-slate-400">
                      <MessageSquare className="w-12 h-12 mb-3 text-slate-200" />
                      <p className="text-sm">No private messages found.</p>
                    </div>
                  ) : (
                    privateConversations.map((conv, idx) => (
                      <div
                        key={conv.key}
                        onClick={() => setSelectedPair(conv)}
                        className="flex items-center gap-4 px-6 py-3.5 border-b border-slate-100 hover:bg-slate-50 cursor-pointer transition-colors"
                      >
                        <div className="w-10 text-xs font-bold text-slate-500">{idx + 1}</div>

                        <div className="flex-1 flex items-center gap-2 min-w-0">
                          <div className="w-9 h-9 rounded-full overflow-hidden border border-slate-200 bg-slate-100 shrink-0">
                            {conv.senderPhoto ? (
                              <img src={conv.senderPhoto} alt={conv.senderName} className="w-full h-full object-cover" onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }} />
                            ) : (
                              <div className="w-full h-full flex items-center justify-center text-slate-500 font-bold text-xs">
                                {(conv.senderName || 'U').charAt(0).toUpperCase()}
                              </div>
                            )}
                          </div>
                          <div className="min-w-0">
                            <div className="font-bold text-sm text-slate-800 truncate">{conv.senderName}</div>
                            <div className="text-[10px] text-slate-400 font-semibold truncate">ID: {conv.senderId}</div>
                          </div>
                        </div>

                        <div className="flex-1 flex items-center gap-2 min-w-0">
                          <div className="w-9 h-9 rounded-full overflow-hidden border border-slate-200 bg-slate-100 shrink-0">
                            {conv.receiverPhoto ? (
                              <img src={conv.receiverPhoto} alt={conv.receiverName} className="w-full h-full object-cover" onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }} />
                            ) : (
                              <div className="w-full h-full flex items-center justify-center text-slate-500 font-bold text-xs">
                                {(conv.receiverName || 'U').charAt(0).toUpperCase()}
                              </div>
                            )}
                          </div>
                          <div className="min-w-0">
                            <div className="font-bold text-sm text-slate-800 truncate">{conv.receiverName}</div>
                            <div className="text-[10px] text-slate-400 font-semibold truncate">ID: {conv.receiverId}</div>
                          </div>
                        </div>

                        <div className="w-48 text-[11px] text-slate-400 font-medium text-right shrink-0">
                          {conv.timestamp ? new Date(conv.timestamp).toLocaleString([], { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }) : ''}
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </>
            ) : (
              <div className="flex flex-col h-full">
                <div className="px-4 py-3 border-b border-slate-200 flex items-center gap-3 shrink-0 bg-white">
                  <button
                    onClick={() => setSelectedPair(null)}
                    className="p-2 -ml-1 text-slate-600 hover:bg-slate-100 rounded-lg active:scale-95 transition-transform"
                  >
                    <ArrowLeft className="w-5 h-5" />
                  </button>

                  <div className="flex items-center -space-x-2">
                    <div className="w-9 h-9 rounded-full overflow-hidden border-2 border-white bg-slate-100 shadow-sm">
                      {selectedPair.senderPhoto ? (
                        <img src={selectedPair.senderPhoto} alt={selectedPair.senderName} className="w-full h-full object-cover" />
                      ) : (
                        <div className="w-full h-full flex items-center justify-center text-slate-500 font-bold text-xs">
                          {(selectedPair.senderName || 'U').charAt(0).toUpperCase()}
                        </div>
                      )}
                    </div>
                    <div className="w-9 h-9 rounded-full overflow-hidden border-2 border-white bg-slate-100 shadow-sm">
                      {selectedPair.receiverPhoto ? (
                        <img src={selectedPair.receiverPhoto} alt={selectedPair.receiverName} className="w-full h-full object-cover" />
                      ) : (
                        <div className="w-full h-full flex items-center justify-center text-slate-500 font-bold text-xs">
                          {(selectedPair.receiverName || 'U').charAt(0).toUpperCase()}
                        </div>
                      )}
                    </div>
                  </div>

                  <div className="flex-1 min-w-0">
                    <div className="font-bold text-sm text-slate-800 truncate">
                      {selectedPair.senderName} ↔ {selectedPair.receiverName}
                    </div>
                    <div className="text-[11px] text-indigo-600 font-semibold truncate">
                      {selectedPair.senderId} • {selectedPair.receiverId}
                    </div>
                  </div>
                </div>

                <div className="flex-1 overflow-y-auto p-4 space-y-2 bg-[#f8fafc]">
                  {selectedPrivateMessages.map((msg, idx) => {
                    const isSenderSide = String(msg.senderId || msg.senderAccountId || '') === selectedPair.senderId;
                    return (
                      <div
                        key={msg.id || idx}
                        className={`flex ${isSenderSide ? 'justify-start' : 'justify-end'}`}
                      >
                        <div className={`max-w-[75%] px-3 py-2 rounded-2xl shadow-sm ${
                          isSenderSide
                            ? 'bg-white border border-slate-200 text-slate-800 rounded-tl-none'
                            : 'bg-indigo-600 text-white rounded-tr-none'
                        }`}>
                          {msg.imageUrl && (
                            <div className="mb-1.5 rounded-lg overflow-hidden border border-white/20">
                              <img src={msg.imageUrl} alt="Attachment" className="max-w-full max-h-60 object-contain" />
                            </div>
                          )}
                          {msg.text && (
                            <p className="text-xs whitespace-pre-line leading-relaxed break-words">{msg.text}</p>
                          )}
                          <div className={`text-[9px] mt-1 ${isSenderSide ? 'text-slate-400' : 'text-indigo-200'}`}>
                            {msg.timestamp ? new Date(msg.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : ''}
                          </div>
                        </div>
                      </div>
                    );
                  })}
                  {selectedPrivateMessages.length === 0 && (
                    <div className="flex flex-col items-center justify-center h-full text-slate-400 text-sm">
                      <MessageSquare className="w-10 h-10 mb-2 text-slate-300" />
                      <p>No messages in this conversation.</p>
                    </div>
                  )}
                  <div ref={privateChatEndRef} />
                </div>
              </div>
            )}
          </div>
        )}

        {activeTab === 'ai_chats' && (
          <div className="flex flex-col h-full bg-white overflow-hidden">
            {!selectedSupportUserId ? (
              <>
                <div className="px-6 py-4 border-b border-slate-200 shrink-0">
                  <h2 className="text-xl font-black text-slate-800">AI Chat</h2>
                </div>

                <div className="px-6 py-4 border-b border-slate-100 shrink-0">
                  <div className="relative w-full max-w-2xl">
                    <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
                    <input
                      type="text"
                      value={supportSearchQuery}
                      onChange={(e) => setSupportSearchQuery(e.target.value)}
                      placeholder="Search AI chats by user name, ID, email..."
                      className="w-full pl-10 pr-4 py-2.5 bg-slate-50 rounded-lg text-sm focus:outline-none font-medium text-slate-900 placeholder:text-slate-400 caret-slate-900"
                    />
                  </div>
                </div>

                <div className="hidden md:flex items-center gap-4 px-6 py-3 text-[11px] font-extrabold text-slate-500 uppercase tracking-wider bg-slate-50 border-b border-slate-100 shrink-0">
                  <span className="w-10">S.No</span>
                  <span className="w-12">Avatar</span>
                  <span className="flex-1">Name</span>
                  <span className="w-48">ID</span>
                  <span className="w-48 text-right">Date Time</span>
                </div>

                <div className="flex-1 overflow-y-auto">
                  {filteredSupportChats.length === 0 ? (
                    <div className="flex flex-col items-center justify-center h-40 text-slate-400">
                      <Bot className="w-12 h-12 mb-3 text-slate-200" />
                      <p className="text-sm">No AI support chat sessions found.</p>
                    </div>
                  ) : (
                    filteredSupportChats.map((chat, idx) => (
                      <div
                        key={chat.userId}
                        onClick={() => setSelectedSupportUserId(chat.userId)}
                        className="flex items-center gap-4 px-6 py-3.5 border-b border-slate-100 hover:bg-slate-50 cursor-pointer transition-colors"
                      >
                        <div className="w-10 text-xs font-bold text-slate-500">{idx + 1}</div>
                        <div className="w-12 shrink-0">
                          <div className="w-9 h-9 rounded-full overflow-hidden border border-slate-200 bg-gradient-to-r from-blue-400 to-indigo-500">
                            {chat.userPhoto ? (
                              <img src={chat.userPhoto} alt={chat.userName} className="w-full h-full object-cover" onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }} />
                            ) : (
                              <div className="w-full h-full flex items-center justify-center text-white font-bold text-xs">
                                {(chat.userName || 'U').charAt(0).toUpperCase()}
                              </div>
                            )}
                          </div>
                        </div>
                        <div className="flex-1 min-w-0">
                          <div className="font-bold text-sm text-slate-800 truncate">{chat.userName || 'User'}</div>
                        </div>
                        <div className="w-48 shrink-0">
                          <div className="text-[11px] text-blue-600 font-bold truncate">ID: {chat.userAccountId || chat.userId}</div>
                        </div>
                        <div className="w-48 text-[11px] text-slate-400 font-medium text-right shrink-0">
                          {chat.timestamp ? new Date(chat.timestamp).toLocaleString([], { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }) : ''}
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </>
            ) : (() => {
              const activeChat = supportChats.find(c => c.userId === selectedSupportUserId);
              if (!activeChat) return null;
              const msgs = Array.isArray(activeChat.messages) ? activeChat.messages : [];

              return (
                <div className="flex flex-col h-full">
                  <div className="px-4 py-3 border-b border-slate-200 flex items-center gap-3 shrink-0 bg-white">
                    <button
                      onClick={() => setSelectedSupportUserId(null)}
                      className="p-2 -ml-1 text-slate-600 hover:bg-slate-100 rounded-lg active:scale-95 transition-transform"
                    >
                      <ArrowLeft className="w-5 h-5" />
                    </button>
                    <div className="w-10 h-10 rounded-full bg-gradient-to-r from-blue-400 to-indigo-500 flex items-center justify-center overflow-hidden border-2 border-white shadow-sm shrink-0">
                      {activeChat.userPhoto ? (
                        <img src={activeChat.userPhoto} alt={activeChat.userName} className="w-full h-full object-cover" />
                      ) : (
                        <span className="text-white font-bold text-sm">{(activeChat.userName || 'U').charAt(0).toUpperCase()}</span>
                      )}
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="font-bold text-sm text-slate-800 truncate">{activeChat.userName || 'User'}</div>
                      <div className="text-[11px] text-blue-600 font-semibold truncate">
                        ID: {activeChat.userAccountId || activeChat.userId}
                      </div>
                    </div>
                  </div>

                  <div className="flex-1 overflow-y-auto p-4 space-y-3 bg-[#f8fafc]">
                    {msgs.map((msg: any, idx: number) => {
                      const isBot = msg.isBot;
                      return (
                        <div key={idx} className={`flex items-start gap-2.5 ${isBot ? '' : 'flex-row-reverse'}`}>
                          <div className={`w-8 h-8 rounded-full flex items-center justify-center shrink-0 shadow-sm overflow-hidden border ${
                            isBot ? 'bg-gradient-to-r from-pink-400 to-purple-500 text-white border-pink-200' : 'bg-blue-600 text-white border-blue-400'
                          }`}>
                            {isBot ? (
                              <img src="/1785612362650~2.jpg" alt="Daisy AI" className="w-full h-full object-cover" />
                            ) : activeChat.userPhoto ? (
                              <img src={activeChat.userPhoto} alt="User" className="w-full h-full object-cover" />
                            ) : (
                              <span className="text-xs font-bold">{(activeChat.userName || 'U').charAt(0).toUpperCase()}</span>
                            )}
                          </div>

                          <div className={`max-w-[75%] rounded-2xl p-3 shadow-sm ${
                            isBot
                              ? 'bg-white rounded-tl-none border border-slate-200 text-slate-800'
                              : 'bg-blue-600 rounded-tr-none text-white'
                          }`}>
                            <div className="flex items-center justify-between gap-3 mb-1">
                              <span className={`text-[10px] font-extrabold ${isBot ? 'text-purple-600' : 'text-blue-200'}`}>
                                {isBot ? '🤖 Daisy AI' : `👤 ${activeChat.userName || 'User'}`}
                              </span>
                              {msg.timestamp && (
                                <span className={`text-[9px] font-medium ${isBot ? 'text-slate-400' : 'text-blue-100'}`}>
                                  {new Date(msg.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                                </span>
                              )}
                            </div>

                            {msg.text && (
                              <p className="text-xs whitespace-pre-line leading-relaxed">{msg.text}</p>
                            )}

                            {msg.image && (
                              <div className="mt-2 rounded-xl overflow-hidden border border-slate-200 shadow-sm">
                                <img src={msg.image} alt="Attachment" className="max-h-60 object-contain bg-slate-100" />
                              </div>
                            )}
                          </div>
                        </div>
                      );
                    })}
                    <div ref={supportChatEndRef} />
                  </div>
                </div>
              );
            })()}
          </div>
        )}

        {activeTab === 'reports' && (
          <Reports />
        )}

        {activeTab === 'bans' && (
          <div className="h-full w-full overflow-y-auto">
            <OwnerBan />
          </div>
        )}

        {activeTab === 'feedback' && (
          <div className="flex flex-col h-full bg-white overflow-hidden">
            <div className="px-8 py-6 border-b border-slate-200 shrink-0">
              <div className="relative w-full max-w-3xl">
                <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
                <input
                  type="text"
                  value={feedbackSearchQuery}
                  onChange={(e) => setFeedbackSearchQuery(e.target.value)}
                  placeholder="Search feedback..."
                  className="w-full pl-10 pr-4 py-2.5 bg-slate-50 rounded-lg text-sm focus:outline-none font-medium text-slate-900 placeholder:text-slate-400 caret-slate-900"
                />
              </div>
            </div>

            <div className="px-8 py-4 border-b border-slate-100 flex items-center justify-between shrink-0">
              <h2 className="text-lg font-black text-slate-800">User Feedback</h2>
              <span className="text-xs font-bold px-3 py-1 bg-indigo-50 text-indigo-700 rounded-full">
                Total: {feedbacks.length}
              </span>
            </div>

            <div className="hidden md:flex items-center gap-4 px-8 py-3 text-[11px] font-extrabold text-slate-500 uppercase tracking-wider bg-slate-50 border-b border-slate-100 shrink-0">
              <span className="w-10">S.No</span>
              <span className="w-12">Avatar</span>
              <span className="flex-1">Name</span>
              <span className="w-40">ID</span>
              <span className="w-28">Type</span>
              <span className="w-40 text-right">Date Time</span>
            </div>

            <div className="flex-1 overflow-y-auto">
              {feedbacks
                .filter((fb) => {
                  const q = feedbackSearchQuery.toLowerCase();
                  const matchesSearch =
                    (fb.userName || '').toLowerCase().includes(q) ||
                    (fb.userAccountId || '').toLowerCase().includes(q) ||
                    (fb.userId || '').toLowerCase().includes(q) ||
                    (fb.contactInfo || '').toLowerCase().includes(q) ||
                    (fb.description || '').toLowerCase().includes(q) ||
                    (fb.typeLabel || '').toLowerCase().includes(q);

                  const matchesType =
                    selectedFeedbackTypeFilter === 'All' ||
                    fb.type === selectedFeedbackTypeFilter;

                  return matchesSearch && matchesType;
                })
                .map((fb, idx) => (
                  <div
                    key={fb.id}
                    onClick={() => setSelectedFeedback(fb)}
                    className="flex items-center gap-4 px-8 py-3.5 border-b border-slate-100 hover:bg-slate-50 cursor-pointer transition-colors"
                  >
                    <div className="w-10 text-xs font-bold text-slate-500">{idx + 1}</div>
                    <div className="w-12 shrink-0">
                      <div className="w-10 h-10 rounded-full bg-gradient-to-r from-indigo-500 to-purple-600 text-white font-bold flex items-center justify-center shadow-sm overflow-hidden border border-indigo-100">
                        {fb.userPhoto ? (
                          <img src={fb.userPhoto} alt={fb.userName} className="w-full h-full object-cover" />
                        ) : (
                          <span className="text-sm">{(fb.userName || 'U').charAt(0).toUpperCase()}</span>
                        )}
                      </div>
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="font-bold text-sm text-slate-800 truncate">{fb.userName || 'User'}</div>
                    </div>
                    <div className="w-40 shrink-0">
                      <span className="text-xs font-bold text-indigo-600 truncate block">ID: {fb.userAccountId || fb.userId || 'N/A'}</span>
                    </div>
                    <div className="w-28 shrink-0">
                      <span className={`inline-block px-2.5 py-1 rounded-full text-[10px] font-extrabold uppercase tracking-wide border ${
                        fb.type === 'bug' ? 'bg-red-50 text-red-600 border-red-100' :
                        fb.type === 'account' ? 'bg-orange-50 text-orange-600 border-orange-100' :
                        fb.type === 'recharge' ? 'bg-emerald-50 text-emerald-600 border-emerald-100' :
                        'bg-blue-50 text-blue-600 border-blue-100'
                      }`}>
                        {fb.typeLabel || fb.type || 'Feedback'}
                      </span>
                    </div>
                    <div className="w-40 shrink-0 text-right">
                      <span className="text-[11px] text-slate-400 font-medium">
                        {fb.timestamp ? new Date(fb.timestamp).toLocaleString([], { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }) : ''}
                      </span>
                    </div>
                  </div>
                ))}

              {feedbacks.length === 0 && (
                <div className="py-16 text-center text-slate-400 text-sm font-medium">
                  <MessageSquare className="w-10 h-10 text-slate-300 mx-auto mb-2" />
                  <p className="font-bold text-slate-700">No user feedback submitted yet</p>
                </div>
              )}
            </div>

            {selectedFeedback && (
              <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
                <div className="bg-white rounded-3xl p-6 w-full max-w-lg shadow-2xl relative border border-slate-100">
                  <button
                    onClick={() => setSelectedFeedback(null)}
                    className="absolute top-4 right-4 text-slate-400 hover:text-slate-600 p-1 rounded-full hover:bg-slate-100 transition-colors"
                  >
                    <X className="w-5 h-5" />
                  </button>

                  <div className="flex items-center gap-3 mb-5 border-b border-slate-100 pb-4">
                    <div className="w-12 h-12 rounded-full bg-gradient-to-r from-indigo-500 to-purple-600 text-white font-bold flex items-center justify-center text-lg shadow-md overflow-hidden shrink-0">
                      {selectedFeedback.userPhoto ? (
                        <img src={selectedFeedback.userPhoto} alt={selectedFeedback.userName} className="w-full h-full object-cover" />
                      ) : (
                        <span>{(selectedFeedback.userName || 'U').charAt(0).toUpperCase()}</span>
                      )}
                    </div>
                    <div>
                      <h3 className="text-base font-bold text-slate-800">{selectedFeedback.userName || 'User'}</h3>
                      <div className="text-xs font-semibold text-slate-500 mt-0.5">
                        User ID: <strong className="text-indigo-600">{selectedFeedback.userAccountId || selectedFeedback.userId}</strong>
                      </div>
                    </div>
                  </div>

                  <div className="space-y-4">
                    <div>
                      <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider block mb-1">Issue Category</span>
                      <span className={`inline-block px-3 py-1 rounded-lg text-xs font-bold border ${
                        selectedFeedback.type === 'bug' ? 'bg-red-50 text-red-600 border-red-100' :
                        selectedFeedback.type === 'account' ? 'bg-orange-50 text-orange-600 border-orange-100' :
                        selectedFeedback.type === 'recharge' ? 'bg-emerald-50 text-emerald-600 border-emerald-100' :
                        'bg-blue-50 text-blue-600 border-blue-100'
                      }`}>
                        {selectedFeedback.typeLabel || selectedFeedback.type}
                      </span>
                    </div>

                    <div>
                      <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider block mb-1">Feedback Description</span>
                      <div className="bg-slate-50 p-4 rounded-2xl border border-slate-200 text-xs font-medium text-slate-800 leading-relaxed whitespace-pre-line max-h-60 overflow-y-auto">
                        {selectedFeedback.description}
                      </div>
                    </div>

                    <div className="grid grid-cols-2 gap-3 pt-2">
                      <div className="bg-slate-50 p-3 rounded-xl border border-slate-100">
                        <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block mb-0.5">Contact Info</span>
                        <span className="text-xs font-bold text-slate-700 break-all">{selectedFeedback.contactInfo || 'N/A'}</span>
                      </div>
                      <div className="bg-slate-50 p-3 rounded-xl border border-slate-100">
                        <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block mb-0.5">Submitted At</span>
                        <span className="text-xs font-bold text-slate-700">
                          {selectedFeedback.timestamp ? new Date(selectedFeedback.timestamp).toLocaleString() : 'N/A'}
                        </span>
                      </div>
                    </div>
                  </div>

                  <div className="mt-6 pt-4 border-t border-slate-100 flex justify-end">
                    <button
                      onClick={() => setSelectedFeedback(null)}
                      className="px-6 py-2.5 text-xs font-bold bg-indigo-600 text-white rounded-xl shadow-md hover:bg-indigo-700 transition-colors"
                    >
                      Close Details
                    </button>
                  </div>
                </div>
              </div>
            )}
          </div>
        )}

        {activeTab === 'official_msg' && (
          <div className="flex flex-col h-full bg-slate-50 overflow-y-auto p-6 md:p-8">
            <div className="max-w-3xl mx-auto w-full space-y-6">

              <div>
                <label className="block text-[11px] font-extrabold text-slate-600 uppercase tracking-wider mb-2">
                  Select ID
                </label>
                <select
                  value={officialSender}
                  onChange={(e) => setOfficialSender(e.target.value as any)}
                  className="w-full px-4 py-3 bg-white border border-slate-200 rounded-xl text-sm font-bold text-slate-900 focus:outline-none focus:border-indigo-500 cursor-pointer"
                >
                  <option value="hurry_team_official">Zyora Team (ID: hurry_team_official)</option>
                  <option value="hurry_system_official">Zyora System (ID: hurry_system_official)</option>
                </select>
              </div>

              <h2 className="text-2xl font-black text-slate-800">Official msg</h2>

              <div className="bg-white rounded-md border border-slate-200 p-5 space-y-4">
                <textarea
                  rows={3}
                  value={officialText}
                  onChange={(e) => setOfficialText(e.target.value)}
                  placeholder="Type your official message here..."
                  className="w-full p-3 bg-slate-50 border border-slate-200 rounded-md text-sm font-medium focus:outline-none focus:border-indigo-500 text-slate-900 placeholder:text-slate-400 caret-slate-900"
                />

                <div>
                  <span className="block text-[11px] font-extrabold text-slate-600 uppercase tracking-wider mb-2">
                    Image
                  </span>

                  <input
                    type="file"
                    ref={officialFileInputRef}
                    accept="image/*"
                    onChange={async (e) => {
                      const file = e.target.files?.[0];
                      if (!file) return;
                      try {
                        const base64 = await compressImage(file, 1200, 1200, 0.85);
                        setOfficialImage(base64);
                      } catch (err) {
                        console.error('Image compression error:', err);
                      }
                    }}
                    className="hidden"
                  />

                  {officialImage ? (
                    <div className="relative inline-block border border-slate-200 rounded-md overflow-hidden bg-slate-900 max-w-xs">
                      <img src={officialImage} alt="Attachment" className="max-h-48 object-contain" />
                      <button
                        onClick={() => {
                          setOfficialImage('');
                          if (officialFileInputRef.current) officialFileInputRef.current.value = '';
                        }}
                        className="absolute top-2 right-2 bg-black/70 text-white p-1.5 rounded-full hover:bg-red-600 transition-colors"
                      >
                        <X className="w-4 h-4" />
                      </button>
                    </div>
                  ) : (
                    <button
                      type="button"
                      onClick={() => officialFileInputRef.current?.click()}
                      className="p-3 bg-slate-100 hover:bg-slate-200 rounded-md transition-colors cursor-pointer"
                      title="Select Image"
                    >
                      <Camera className="w-5 h-5 text-slate-600" />
                    </button>
                  )}
                </div>

                <div className="flex justify-end pt-2">
                  <button
                    onClick={() => {
                      if (!officialText.trim() && !officialImage) {
                        alert('Please enter text or select an image.');
                        return;
                      }

                      setOfficialSending(true);

                      const senderName = officialSender === 'hurry_team_official' ? 'Zyora Team' : 'Zyora System';
                      const senderPhoto = officialSender === 'hurry_team_official' ? '/logo.png' : '/file_00000000a66881f8aa9e15d2fe2b9a0c.png';

                      const payload = {
                        id: `official_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`,
                        senderId: officialSender,
                        senderName,
                        senderPhoto,
                        text: officialText.trim(),
                        type: officialImage ? 'image' : 'message',
                        imageUrl: officialImage || undefined,
                        timestamp: Date.now()
                      };

                      socket.emit('send_official_message', payload);

                      setOfficialSuccess('Sent successfully!');
                      setOfficialText('');
                      setOfficialImage('');
                      if (officialFileInputRef.current) officialFileInputRef.current.value = '';
                      setOfficialSending(false);

                      setTimeout(() => setOfficialSuccess(''), 3000);
                    }}
                    disabled={officialSending || (!officialText.trim() && !officialImage)}
                    className="px-6 py-2.5 bg-gradient-to-r from-blue-600 to-indigo-600 text-white text-sm font-extrabold rounded-md shadow-md hover:from-blue-700 hover:to-indigo-700 disabled:opacity-50 transition-all flex items-center gap-2 cursor-pointer"
                  >
                    <Send className="w-4 h-4" />
                    <span>{officialSending ? 'Sending...' : 'Send'}</span>
                  </button>
                </div>

                {officialSuccess && (
                  <div className="text-xs font-bold text-emerald-600 text-right">{officialSuccess}</div>
                )}
              </div>

              <div className="bg-white rounded-md border border-slate-200 p-5">
                <h3 className="font-extrabold text-slate-800 text-base mb-4">History</h3>

                <div className="flex items-center gap-4 px-3 py-2 text-[11px] font-extrabold text-slate-500 uppercase tracking-wider bg-slate-50 rounded-md mb-2">
                  <span className="w-10">S.no</span>
                  <span className="flex-1">ID Type</span>
                  <span className="w-48 text-right">Date Time</span>
                </div>

                <div className="flex flex-col gap-1 max-h-96 overflow-y-auto">
                  {officialHistory.map((item, idx) => (
                    <div key={item.id} className="flex items-center gap-4 px-3 py-2.5 border-b border-slate-100 text-xs">
                      <span className="w-10 font-bold text-slate-500">{idx + 1}</span>
                      <span className="flex-1 font-bold text-slate-800">
                        {item.senderName || (item.senderId === 'hurry_team_official' ? 'Zyora Team' : 'Zyora System')}
                      </span>
                      <span className="w-48 text-right text-slate-400 font-medium">
                        {item.timestamp ? new Date(item.timestamp).toLocaleString([], { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }) : ''}
                      </span>
                    </div>
                  ))}
                  {officialHistory.length === 0 && (
                    <div className="py-8 text-center text-slate-400 text-xs font-medium">
                      No messages sent yet.
                    </div>
                  )}
                </div>
              </div>

            </div>
          </div>
        )}

        {activeTab !== 'manage_users' && activeTab !== 'game_fruit_party' && activeTab !== 'game_wild_party' && activeTab !== 'themes' && activeTab !== 'gift_catalog' && activeTab !== 'ai_chats' && activeTab !== 'feedback' && activeTab !== 'official_msg' && activeTab !== 'reports' && activeTab !== 'bans' && activeTab !== 'tickets' && (
          <div className="p-8 max-w-5xl mx-auto w-full">
            <div className="bg-white p-8 rounded-3xl shadow-sm border border-slate-100">
              <h2 className="text-xl font-bold text-slate-800 capitalize">
                {activeTab.replace(/_/g, ' ')}
              </h2>
              <p className="text-slate-500 mt-2">This section is active and ready for configuration.</p>
            </div>
          </div>
        )}
      </main>

      {isTagModalOpen && selectedTagUserData && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-3xl p-6 w-full max-w-lg shadow-2xl relative">
            <button onClick={() => setIsTagModalOpen(false)} className="absolute top-4 right-4 text-slate-400 hover:text-slate-600">
              <X className="w-5 h-5" />
            </button>
            <h3 className="text-lg font-bold text-slate-800 mb-4 flex items-center gap-2">
              <span>🏷️</span> Manage Tags for <span className="text-blue-600">{selectedTagUserData.name}</span>
            </h3>

            <div className="grid grid-cols-2 gap-3 mb-6">
              {AVAILABLE_TAGS.map((tag) => {
                const isSelected = selectedTags.includes(tag.id);
                return (
                  <div
                    key={tag.id}
                    onClick={() => toggleTag(tag.id)}
                    className={`p-3 rounded-2xl border-2 transition cursor-pointer flex items-center gap-3 ${
                      isSelected ? 'bg-blue-50 border-blue-500' : 'bg-white border-slate-200'
                    }`}
                  >
                    <span className="text-2xl">{tag.emoji}</span>
                    <span className="font-bold text-xs text-slate-800">{tag.name}</span>
                  </div>
                );
              })}
            </div>

            <div className="flex justify-end gap-3">
              <button onClick={() => setIsTagModalOpen(false)} className="px-5 py-2.5 text-sm font-bold text-slate-500 hover:bg-slate-100 rounded-xl transition-colors">
                Cancel
              </button>
              <button onClick={handleAssignTags} className="px-5 py-2.5 text-sm font-bold bg-blue-600 text-white rounded-xl shadow-md hover:bg-blue-700 transition-colors">
                Save Tags
              </button>
            </div>
            {tagSuccess && <p className="mt-4 text-center text-sm font-bold text-green-600">{tagSuccess}</p>}
          </div>
        </div>
      )}

      {enlargedThemeImage && (
        <div
          className="fixed inset-0 bg-black/80 flex items-center justify-center z-[60] p-4"
          onClick={() => setEnlargedThemeImage(null)}
        >
          <div className="relative max-w-4xl max-h-[90vh] w-full">
            <button
              onClick={() => setEnlargedThemeImage(null)}
              className="absolute -top-3 -right-3 bg-white text-slate-800 p-2 rounded-full shadow-lg hover:bg-red-500 hover:text-white transition-colors z-10"
            >
              <X className="w-5 h-5" />
            </button>
            <img
              src={enlargedThemeImage}
              alt="Theme preview"
              className="w-full h-auto max-h-[85vh] object-contain rounded-2xl shadow-2xl"
              onClick={(e) => e.stopPropagation()}
            />
          </div>
        </div>
      )}
    </div>
  );
        }
