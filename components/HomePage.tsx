'use client'

import { apiUrl } from "../src/lib/api";

import { useState, useEffect, useRef, useCallback } from 'react'
import { socket } from '../src/lib/socket'
import MessagePage from './MessagePage'
import MePage from './MePage'
import { getOrCreateAccountNumber } from './MePage'
import RoomPage from './RoomPage'
import PublicProfile from './PublicProfile'
import MinePage from './MinePage'
import SearchPage from './SearchPage'
import { generateStableId } from '../lib/hash'
import { translations, getTranslation, LanguageCode } from '../lib/translations'
import DailyCheckInModal from '../components/DailyCheckInModal'
import InviteFriends from './InviteFriends'
import Leaderboard from './Leaderboard'

// ============ MONGODB / INDEXEDDB DATA HELPERS ============


const normalizeRoomImage = (value: any, roomId: string): string => {
  const image = String(value || '').trim();
  if (!image || image === 'undefined' || image === 'null') return '/default-avatar.png';
  if (image.startsWith('data:image/')) {
    return apiUrl(`/api/rooms/image?roomId=${encodeURIComponent(String(roomId || ''))}`);
  }
  return image;
};

const fetchAllRoomsFromMongoDB = async (): Promise<any[]> => {
  const response = await fetch("/api/rooms");
  if (!response.ok) {
    throw new Error(`MongoDB rooms fetch failed: ${response.status}`);
  }
  const result = await response.json();
  return Array.isArray(result?.rooms) ? result.rooms : [];
};

const fetchRoomFromMongoDB = async (roomId: string): Promise<any | null> => {
  if (!roomId) return null;
  const response = await fetch(`/api/rooms?roomId=${encodeURIComponent(roomId)}`);
  if (!response.ok) {
    if (response.status === 404) return null;
    throw new Error(`MongoDB room fetch failed: ${response.status}`);
  }
  const result = await response.json();
  return result?.room || null;
};

const saveRoomToMongoDB = async (roomData: any) => {
  const response = await fetch("/api/rooms", {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(roomData),
  });
  if (!response.ok) {
    throw new Error(`MongoDB room save failed: ${response.status}`);
  }
  return response.json();
};

const saveUserToMongoDB = async (userData: any) => {
  const uid = userData.uid || userData.id || userData.appLongId;
  if (!uid) {
    throw new Error("Missing user uid");
  }
  const response = await fetch(apiUrl("/api/users"), {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ ...userData, uid }),
  });
  if (!response.ok) {
    throw new Error(`MongoDB user save failed: ${response.status}`);
  }
  return response.json();
};

const loadHomeMessagesFromDB = async (userUID: string): Promise<any[]> => {
  if (!userUID || userUID === 'N/A') return [];
  try {
    const dbName = `ChatMessagesDB_${userUID}`;
    const db = await new Promise<IDBDatabase>((resolve, reject) => {
      const request = indexedDB.open(dbName, 1);
      request.onerror = () => reject(request.error);
      request.onsuccess = () => resolve(request.result);
      request.onupgradeneeded = () => {
        const database = request.result;
        if (!database.objectStoreNames.contains("messages")) {
          const store = database.createObjectStore("messages", { keyPath: "id" });
          store.createIndex("chatId", "chatId", { unique: false });
          store.createIndex("timestamp", "timestamp", { unique: false });
        }
      };
    });

    const transaction = db.transaction(["messages"], "readonly");
    const store = transaction.objectStore("messages");
    const messages = await new Promise<any[]>((resolve, reject) => {
      const request = store.getAll();
      request.onsuccess = () => resolve(request.result || []);
      request.onerror = () => reject(request.error);
    });
    db.close();
    return messages;
  } catch (error) {
    console.error("❌ Home messages IndexedDB load error:", error);
    return [];
  }
};

// ============ INDEXEDDB FUNCTIONS ============
const DB_NAME = 'HurryAppDB';
const DB_VERSION = 3;
const ROOM_STORE = 'rooms';
const USER_STORE = 'users';
const RECENT_STORE = 'recentRooms';
const FOLLOWING_STORE = 'followingRooms';
const GLOBAL_ROOMS_STORE = 'globalRooms';

const openDB = (): Promise<IDBDatabase> => {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION);
    request.onerror = () => reject(request.error);
    request.onsuccess = () => resolve(request.result);

    request.onupgradeneeded = (event) => {
      const db = request.result;
      const oldVersion = event.oldVersion;

      if (!db.objectStoreNames.contains(ROOM_STORE)) {
        db.createObjectStore(ROOM_STORE, { keyPath: 'accountId' });
      }
      if (!db.objectStoreNames.contains(USER_STORE)) {
        db.createObjectStore(USER_STORE, { keyPath: 'uid' });
      }
      if (!db.objectStoreNames.contains(RECENT_STORE)) {
        const recentStore = db.createObjectStore(RECENT_STORE, { keyPath: 'accountId' });
        recentStore.createIndex('timestamp', 'timestamp', { unique: false });
      }
      if (!db.objectStoreNames.contains(FOLLOWING_STORE)) {
        db.createObjectStore(FOLLOWING_STORE, { keyPath: 'accountId' });
      }
      if (!db.objectStoreNames.contains(GLOBAL_ROOMS_STORE)) {
        const globalRoomsStore = db.createObjectStore(GLOBAL_ROOMS_STORE, { keyPath: 'accountId' });
        globalRoomsStore.createIndex('createdAt', 'createdAt', { unique: false });
      }
    };
  });
};

const saveRoomToDB = async (roomData: any) => {
  try {
    const db = await openDB();
    const transaction = db.transaction([ROOM_STORE], 'readwrite');
    const store = transaction.objectStore(ROOM_STORE);
    await new Promise<void>((resolve, reject) => {
      const request = store.put({ ...roomData, cachedAt: Date.now() });
      request.onsuccess = () => resolve();
      request.onerror = () => reject(request.error);
    });
    db.close();
  } catch (error) {
    console.error('❌ Room save error:', error);
  }
};

const loadRoomFromDB = async (accountId: string): Promise<any | null> => {
  try {
    const db = await openDB();
    const transaction = db.transaction([ROOM_STORE], 'readonly');
    const store = transaction.objectStore(ROOM_STORE);
    const room = await new Promise<any | null>((resolve, reject) => {
      const request = store.get(accountId);
      request.onsuccess = () => resolve(request.result || null);
      request.onerror = () => reject(request.error);
    });
    db.close();
    return room;
  } catch (error) {
    console.error('❌ Load room error:', error);
    return null;
  }
};

const saveGlobalRoomsToDB = async (rooms: any[]) => {
  try {
    const db = await openDB();
    const transaction = db.transaction([GLOBAL_ROOMS_STORE], 'readwrite');
    const store = transaction.objectStore(GLOBAL_ROOMS_STORE);
    for (const room of rooms) {
      await new Promise<void>((resolve, reject) => {
        const request = store.put({ ...room, cachedAt: Date.now() });
        request.onsuccess = () => resolve();
        request.onerror = () => reject(request.error);
      });
    }
    db.close();
  } catch (error) {
    console.error('❌ Global rooms save error:', error);
  }
};

const loadGlobalRoomsFromDB = async (): Promise<any[]> => {
  try {
    const db = await openDB();
    const transaction = db.transaction([GLOBAL_ROOMS_STORE], 'readonly');
    const store = transaction.objectStore(GLOBAL_ROOMS_STORE);
    const rooms = await new Promise<any[]>((resolve, reject) => {
      const request = store.getAll();
      request.onsuccess = () => resolve(request.result || []);
      request.onerror = () => reject(request.error);
    });
    db.close();
    return rooms;
  } catch (error) {
    console.error('❌ Load global rooms error:', error);
    return [];
  }
};

const deleteGlobalRoomFromDB = async (accountId: string) => {
  try {
    const db = await openDB();
    const transaction = db.transaction([GLOBAL_ROOMS_STORE], 'readwrite');
    const store = transaction.objectStore(GLOBAL_ROOMS_STORE);
    await new Promise<void>((resolve, reject) => {
      const request = store.delete(accountId);
      request.onsuccess = () => resolve();
      request.onerror = () => reject(request.error);
    });
    db.close();
  } catch (error) {
    console.error('❌ Delete room error:', error);
  }
};

const saveRecentToDB = async (recentRooms: any[]) => {
  try {
    const db = await openDB();
    const transaction = db.transaction([RECENT_STORE], 'readwrite');
    const store = transaction.objectStore(RECENT_STORE);
    await new Promise<void>((resolve, reject) => {
      const clearRequest = store.clear();
      clearRequest.onsuccess = () => resolve();
      clearRequest.onerror = () => reject(clearRequest.error);
    });
    for (const room of recentRooms) {
      await new Promise<void>((resolve, reject) => {
        const request = store.put(room);
        request.onsuccess = () => resolve();
        request.onerror = () => reject(request.error);
      });
    }
    db.close();
  } catch (error) {
    console.error('❌ Recent save error:', error);
  }
};

const loadRecentFromDB = async (): Promise<any[]> => {
  try {
    const db = await openDB();
    const transaction = db.transaction([RECENT_STORE], 'readonly');
    const store = transaction.objectStore(RECENT_STORE);
    const index = store.index('timestamp');
    const recentRooms = await new Promise<any[]>((resolve, reject) => {
      const request = index.getAll();
      request.onsuccess = () => resolve(request.result || []);
      request.onerror = () => reject(request.error);
    });
    db.close();
    recentRooms.sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0));
    const now = Date.now();
    const fiveMinutesAgo = now - (5 * 60 * 1000);
    return recentRooms.filter(room => room.timestamp >= fiveMinutesAgo);
  } catch (error) {
    console.error('❌ Recent load error:', error);
    return [];
  }
};

const saveFollowingToDB = async (followingRooms: any[]) => {
  try {
    const db = await openDB();
    const transaction = db.transaction([FOLLOWING_STORE], 'readwrite');
    const store = transaction.objectStore(FOLLOWING_STORE);
    await new Promise<void>((resolve, reject) => {
      const clearRequest = store.clear();
      clearRequest.onsuccess = () => resolve();
      clearRequest.onerror = () => reject(clearRequest.error);
    });
    for (const room of followingRooms) {
      await new Promise<void>((resolve, reject) => {
        const request = store.put(room);
        request.onsuccess = () => resolve();
        request.onerror = () => reject(request.error);
      });
    }
    db.close();
  } catch (error) {
    console.error('❌ Following save error:', error);
  }
};

const loadFollowingFromDB = async (): Promise<any[]> => {
  try {
    const db = await openDB();
    const transaction = db.transaction([FOLLOWING_STORE], 'readonly');
    const store = transaction.objectStore(FOLLOWING_STORE);
    const followingRooms = await new Promise<any[]>((resolve, reject) => {
      const request = store.getAll();
      request.onsuccess = () => resolve(request.result || []);
      request.onerror = () => reject(request.error);
    });
    db.close();
    return followingRooms;
  } catch (error) {
    console.error('❌ Following load error:', error);
    return [];
  }
};

// ============ PASSWORD INPUT COMPONENT ============
function PasswordInput({ value, onChange }: { value: string; onChange: (value: string) => void }) {
  const inputRefs = useRef<(HTMLInputElement | null)[]>([])

  const handleInput = (index: number, inputValue: string) => {
    const numberValue = inputValue.replace(/[^0-9]/g, '')
    if (numberValue) {
      const newDigits = value.split('')
      newDigits[index] = numberValue.slice(-1)
      const newPassword = newDigits.join('').slice(0, 4)
      onChange(newPassword)
      if (index < 3 && numberValue) {
        inputRefs.current[index + 1]?.focus()
      }
    }
  }

  const handleKeyDown = (index: number, e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Backspace' && !value[index] && index > 0) {
      inputRefs.current[index - 1]?.focus()
    }
  }

  return (
    <div className="flex gap-3 justify-center">
      {[0, 1, 2, 3].map((index) => (
        <input
          key={index}
          ref={(el) => { inputRefs.current[index] = el }}
          type="text"
          inputMode="numeric"
          pattern="[0-9]*"
          maxLength={1}
          value={value[index] || ''}
          onChange={(e) => handleInput(index, e.target.value)}
          onKeyDown={(e) => handleKeyDown(index, e)}
          className="w-14 h-14 text-center text-2xl font-bold border-2 border-gray-300 rounded-lg focus:border-blue-500 focus:outline-none text-black"
        />
      ))}
    </div>
  )
}

// ============ INTERFACES ============
interface HomePageProps {
  onLogout?: () => void;
}

interface UserCard {
  id: string
  accountId?: string
  name: string
  country: string
  image: string
  isLocked?: boolean
  roomPassword?: string
}

interface KeptRoomData {
  name: string
  country?: string
  image: string
  accountId: string
  isLocked?: boolean
  roomPassword?: string
}

interface RecentRoom extends KeptRoomData {
  timestamp: number
}

interface GlobalRoom {
  id: string
  name: string
  country: string
  image: string
  accountId: string
  createdAt: number
  isLocked?: boolean
  roomPassword?: string
  isExplicitlyCreated?: boolean
  activeUserCount?: number;
}

// ============ CONSTANTS ============
const BANNERS = [
  { image: '/IMG-20260830-WA0081.jpg' },
  { image: '/file_0000000033348246b73ea85588488c24.png' },
  { image: '/IMG-20260818-WA0001.jpg' }
]

type Tab = 'mine' | 'popular'
type MineTab = 'following' | 'recent'
type Page = 'home' | 'message' | 'me' | 'room' | 'public_profile' | 'leaderboard'
type SearchTab = 'user' | 'room'

const CATEGORY_CARDS = [
  { label: 'Honour', tab: 'honour' as const, bgImage: '/IMG_20260912_144404.png' },
  { label: 'Charm', tab: 'charm' as const, bgImage: '/IMG_20260912_144324.png' },
  { label: 'Room', tab: 'room' as const, bgImage: '/IMG_20260912_144347.png' },
];

// ============ SEARCH FUNCTION ============
async function fetchSearchResults(
  queryRaw: string,
  globalRooms: GlobalRoom[]
): Promise<GlobalRoom[]> {
  const query = queryRaw.trim();
  if (!query) return [];

  const queryLower = query.toLowerCase();
  const foundList: GlobalRoom[] = [];
  const addedKeys = new Set<string>();

  try {
    const controller = new AbortController();
    const timeout = window.setTimeout(() => controller.abort(), 3500);

    const response = await fetch(
      apiUrl(`/api/users?search=${encodeURIComponent(query)}&accountId=${encodeURIComponent(query)}&q=${encodeURIComponent(query)}`),
      { cache: "no-store", signal: controller.signal }
    );

    window.clearTimeout(timeout);

    if (response.ok) {
      const data = await response.json();
      const rawUsers = Array.isArray(data)
        ? data
        : data?.users
        ? data.users
        : data?.user
        ? [data.user]
        : [];

      for (const user of rawUsers) {
        if (!user) continue;
        const userId = String(user.id || user.uid || user.appLongId || "");
        let accountId = String(
          user.accountId || user.accountNumber || user.displayUserNumber || user["Account Number"] || ""
        );
        if (!accountId || accountId === userId) {
          accountId = getOrCreateAccountNumber(userId).fullAccNum;
        }
        const key = accountId || userId;
        if (key && !addedKeys.has(key) && !addedKeys.has(userId)) {
          addedKeys.add(key);
          addedKeys.add(userId);
          if (accountId) addedKeys.add(accountId);
          foundList.push({
            id: userId || accountId,
            name: user.name || user.displayName || user.userName || "User",
            country: user.country || "🇮🇳",
            image: user.image || user.photo || user.photoURL || user.avatar || "/default-avatar.png",
            accountId: accountId,
            createdAt: user.createdAt || Date.now(),
            isLocked: Boolean(user.isLocked),
            roomPassword: user.roomPassword || null,
            isExplicitlyCreated: true,
            activeUserCount: 0,
          });
        }
      }
    }
  } catch (error) {
    console.error("MongoDB user search error:", error);
  }

  for (const room of globalRooms) {
    const roomId = String(room.id || "");
    const accountId = String(room.accountId || "");
    const name = String(room.name || "");

    const exactMatch = roomId.toLowerCase() === queryLower || accountId.toLowerCase() === queryLower;
    const partialIdMatch = (accountId && accountId.toLowerCase().includes(queryLower)) || (roomId && roomId.toLowerCase().includes(queryLower));
    const nameMatch = name.toLowerCase().includes(queryLower);

    if (exactMatch || partialIdMatch || nameMatch) {
      const key = `${accountId}_${roomId}`;
      if (!addedKeys.has(key) && (!accountId || !addedKeys.has(accountId)) && (!roomId || !addedKeys.has(roomId))) {
        addedKeys.add(key);
        if (accountId) addedKeys.add(accountId);
        if (roomId) addedKeys.add(roomId);
        foundList.push(room);
      }
    }
  }

  return foundList;
}

// ============ LIVE ROOM STATS COMPONENT ============
const LiveRoomStats = () => {
  const [count, setCount] = useState(() => Math.floor(Math.random() * 4000) + 1000);

  useEffect(() => {
    const interval = setInterval(() => {
      setCount((prev) => prev + (Math.floor(Math.random() * 7) - 3));
    }, Math.random() * 2000 + 1000);
    return () => clearInterval(interval);
  }, []);

  return (
    <div className="absolute bottom-2 right-2 flex items-center gap-1.5 z-10">
      <style>
        {`
          @keyframes trackEq {
            0% { height: 3px; }
            50% { height: 9px; }
            100% { height: 3px; }
          }
          .track-bar {
            width: 2.5px;
            background-color: #FFFFFF;
            border-radius: 2px;
            animation: trackEq infinite ease-in-out;
          }
        `}
      </style>

      <span className="text-white text-[11px] font-extrabold tracking-wider">
        {count}
      </span>

      <div className="flex items-end gap-[2px] h-[9px]">
        <div className="track-bar" style={{ animationDuration: '0.8s', animationDelay: '0s' }}></div>
        <div className="track-bar" style={{ animationDuration: '0.5s', animationDelay: '0.2s' }}></div>
        <div className="track-bar" style={{ animationDuration: '1s', animationDelay: '0.4s' }}></div>
      </div>
    </div>
  );
};

// ============ MAIN COMPONENT ============
const getDailyDateKey = () => {
  const now = new Date()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return now.getFullYear() + '-' + month + '-' + day
}
export default function HomePage({ onLogout }: HomePageProps) {
  const [activeTab, setActiveTab] = useState<Tab>('popular')
  const [appLang, setAppLang] = useState<LanguageCode>('en')

  useEffect(() => {
    const savedLang = localStorage.getItem('appLanguage') as LanguageCode
    if (savedLang) setAppLang(savedLang)
    const handleLangChange = (e: CustomEvent) => {
      if (e.detail && e.detail.lang) setAppLang(e.detail.lang)
    }
    window.addEventListener('languageChange', handleLangChange as EventListener)
    return () => window.removeEventListener('languageChange', handleLangChange as EventListener)
  }, [])

  const t = getTranslation(appLang)

  const [activeMineTab, setActiveMineTab] = useState<MineTab>('following')
  const [currentPage, setCurrentPage] = useState<Page>('home')
  const [leaderboardTab, setLeaderboardTab] = useState<'honour' | 'charm' | 'room'>('honour')
  const [mounted, setMounted] = useState(false)
  const [currentBanner, setCurrentBanner] = useState(0)
  const [isChatOpen, setIsChatOpen] = useState(false)
  const [selectedUser, setSelectedUser] = useState<UserCard | null>(null)

  const [isSearchOpen, setIsSearchOpen] = useState(false)
  const [searchQuery, setSearchQuery] = useState('')
  const [activeSearchTab, setActiveSearchTab] = useState<SearchTab>('user')
  const [searchResults, setSearchResults] = useState<GlobalRoom[]>([])
  const [hasSearched, setHasSearched] = useState(false)
  const [isSearching, setIsSearching] = useState(false)

  const [isPublicProfileActive, setIsPublicProfileActive] = useState(false)

  const [isRoomCreated, setIsRoomCreated] = useState(false)
  const [myRoom, setMyRoom] = useState<UserCard | null>(null)
  const [userName, setUserName] = useState('')
  const [userPhoto, setUserPhoto] = useState('')
  const [userUID, setUserUID] = useState('')
  const [userPresence, setUserPresence] = useState<Record<string, boolean>>({})
  const [totalUnreadCount, setTotalUnreadCount] = useState(0)
  const [topNotification, setTopNotification] = useState<{
    id: string;
    senderName: string;
    senderPhoto: string;
    text: string;
    senderId: string;
  } | null>(null);
  const notificationTimerRef = useRef<NodeJS.Timeout | null>(null);

  const [globalRooms, setGlobalRooms] = useState<GlobalRoom[]>([])

  const globalRoomsRef = useRef<GlobalRoom[]>([]);
  useEffect(() => { globalRoomsRef.current = globalRooms; }, [globalRooms]);

  const [keptRoom, setKeptRoom] = useState<KeptRoomData | null>(null)
  const [enteredFromKept, setEnteredFromKept] = useState(false)

  const [showRoomPasswordCard, setShowRoomPasswordCard] = useState(false)
  const [selectedLockedRoom, setSelectedLockedRoom] = useState<UserCard | null>(null)
  const [enteredRoomPassword, setEnteredRoomPassword] = useState('')

  const [recentRooms, setRecentRooms] = useState<RecentRoom[]>([])
  const [followingRooms, setFollowingRooms] = useState<KeptRoomData[]>([])

  const [isSignInModalOpen, setIsSignInModalOpen] = useState(false)
  const [isInviteFriendsOpen, setIsInviteFriendsOpen] = useState(false)
  const [currentSignInDay, setCurrentSignInDay] = useState(1)
  const [claimedToday, setClaimedToday] = useState(false)
  const signInClaimingRef = useRef(false)

  useEffect(() => {
    const dayKey = `signInDay_${userUID}`
    const claimKey = `signInClaimDate_${userUID}`
    const savedDay = Number(localStorage.getItem(dayKey) || '1')
    const today = getDailyDateKey()
    const localDay = savedDay >= 1 && savedDay <= 7 ? savedDay : 1
    const localClaimed = localStorage.getItem(claimKey) === today

    setCurrentSignInDay(localDay)
    setClaimedToday(localClaimed)

    if (!userUID || userUID === 'N/A') return

    // Browser/WebView localStorage is the fast cache. The backend copy
    // restores the check-in state after an APK uninstall/reinstall.
    fetch(apiUrl(`/api/daily-checkin?userId=${encodeURIComponent(userUID)}`), { cache: 'no-store' })
      .then(res => res.ok ? res.json() : null)
      .then(data => {
        if (!data?.success) return
        const serverDay = Number(data.currentDay)
        if (serverDay >= 1 && serverDay <= 7) {
          setCurrentSignInDay(serverDay)
          localStorage.setItem(dayKey, String(serverDay))
        }
        if (String(data.claimedDate || '') === today) {
          setClaimedToday(true)
          localStorage.setItem(claimKey, today)
        }
      })
      .catch(() => {})
  }, [userUID])

  const [isDragging, setIsDragging] = useState(false)
  const [dragPosition, setDragPosition] = useState({ x: 0, y: 0 })
  const [showDeleteZone, setShowDeleteZone] = useState(false)
  const [isOverDeleteZone, setIsOverDeleteZone] = useState(false)
  const dragStartPos = useRef({ x: 0, y: 0 })
  const circleStartPos = useRef({ x: 16, y: typeof window !== 'undefined' ? window.innerHeight * 0.4 : 300 })
  const deleteZoneRef = useRef<HTMLDivElement>(null)
  const circleRef = useRef<HTMLDivElement>(null)

  const [viewportHeight, setViewportHeight] = useState(0)
  const [isAndroid, setIsAndroid] = useState(false)
  const [categoryOffset, setCategoryOffset] = useState(0)

  const bannerRef = useRef<HTMLDivElement>(null)
  const bannerContainerRef = useRef<HTMLDivElement>(null)
  const bannerDotsRef = useRef<HTMLDivElement>(null)
  const categoryCardsRef = useRef<HTMLDivElement>(null)

  const touchStartX = useRef<number>(0)
  const touchEndX = useRef<number>(0)
  const [isSwiping, setIsSwiping] = useState(false)
  const [swipeOffset, setSwipeOffset] = useState(0)

  const jitsiContainerRef = useRef<HTMLDivElement>(null)
  const jitsiApiRef = useRef<any>(null)
  const [jitsiLoaded, setJitsiLoaded] = useState(false)
  const jitsiJoinedRef = useRef(false)
  const [isJitsiJoined, setIsJitsiJoined] = useState(false)

  // ============ UNREAD COUNT & GLOBAL REAL-TIME MESSAGES ============
  useEffect(() => {
    if (!userUID || userUID === 'N/A') return;

    let isMounted = true;
    const fetchUnread = async () => {
      const messages = await loadHomeMessagesFromDB(userUID);
      if (!isMounted) return;
      if (Array.isArray(messages)) {
        let count = 0;
        messages.forEach((msg: any) => {
          if (msg.receiverId === userUID && msg.isUnread) {
            count++;
          }
        });
        setTotalUnreadCount(count);
      }
    };

    fetchUnread();
    const interval = setInterval(fetchUnread, 5000);

    const handleUnreadUpdated = () => { fetchUnread(); };
    window.addEventListener('unread_count_updated', handleUnreadUpdated);

    const saveIncomingMessageToDB = async (msgData: any) => {
      try {
        const dbName = `ChatMessagesDB_${userUID}`;
        const db = await new Promise<IDBDatabase>((resolve, reject) => {
          const request = indexedDB.open(dbName, 1);
          request.onerror = () => reject(request.error);
          request.onsuccess = () => resolve(request.result);
          request.onupgradeneeded = () => {
            const database = request.result;
            if (!database.objectStoreNames.contains("messages")) {
              const store = database.createObjectStore("messages", { keyPath: "id" });
              store.createIndex("chatId", "chatId", { unique: false });
              store.createIndex("timestamp", "timestamp", { unique: false });
            }
          };
        });

        const tx = db.transaction(["messages"], "readwrite");
        tx.objectStore("messages").put(msgData);
        tx.oncomplete = () => {
          db.close();
          window.dispatchEvent(new CustomEvent('unread_count_updated'));
        };
        tx.onerror = () => db.close();
      } catch (err) {
        console.error("Error saving incoming message to IndexedDB in HomePage:", err);
      }
    };

    const handleIncomingPrivateMsg = (data: any) => {
      if (!data) return;
      const rId = String(data.receiverId || '');
      if (rId !== String(userUID)) return;

      const senderId = String(data.senderId || '');
      const chatId = [userUID, senderId].sort().join('_');
      const timestamp = Number(data.timestamp || Date.now());

      const msgObj = {
        id: String(data.id || `${senderId}_${timestamp}_${Math.random().toString(36).slice(2, 7)}`),
        chatId,
        text: String(data.text || ''),
        sender: 'other',
        senderId,
        receiverId: userUID,
        senderName: data.senderName || 'User',
        senderPhoto: data.senderPhoto || '/default-avatar.png',
        timestamp,
        type: data.type || (data.imageUrl ? 'image' : 'message'),
        imageUrl: data.imageUrl || undefined,
        isUnread: true,
      };

      saveIncomingMessageToDB(msgObj);

      if (senderId !== userUID) {
        setTopNotification({
          id: String(data.id || Date.now()),
          senderName: data.senderName || 'User',
          senderPhoto: data.senderPhoto || '/default-avatar.png',
          text: data.type === 'image' ? '📷 Image' : String(data.text || ''),
          senderId,
        });

        if (notificationTimerRef.current) clearTimeout(notificationTimerRef.current);
        notificationTimerRef.current = setTimeout(() => {
          setTopNotification(null);
          notificationTimerRef.current = null;
        }, 3800);
      }
    };

    const handleIncomingOfficialBroadcast = (data: any) => {
      if (!data?.senderId) return;
      const senderId = String(data.senderId);
      if (senderId !== 'hurry_team_official' && senderId !== 'hurry_system_official') return;

      const chatId = [userUID, senderId].sort().join('_');
      const timestamp = Number(data.timestamp || Date.now());

      const msgObj = {
        id: String(data.id || `official_${timestamp}_${Math.random().toString(36).slice(2, 7)}`),
        chatId,
        text: String(data.text || ''),
        sender: 'other',
        senderId,
        receiverId: userUID,
        senderName: data.senderName || (senderId === 'hurry_team_official' ? 'Zyora Team' : 'Zyora System'),
        senderPhoto: data.senderPhoto || (senderId === 'hurry_team_official' ? '/logo.png' : '/file_00000000a66881f8aa9e15d2fe2b9a0c.png'),
        timestamp,
        type: data.type || (data.imageUrl ? 'image' : 'message'),
        imageUrl: data.imageUrl || undefined,
        isUnread: true,
      };

      saveIncomingMessageToDB(msgObj);

      setTopNotification({
        id: String(data.id || Date.now()),
        senderName: msgObj.senderName,
        senderPhoto: msgObj.senderPhoto,
        text: data.type === 'image' ? '📷 Image' : String(data.text || ''),
        senderId,
      });

      if (notificationTimerRef.current) clearTimeout(notificationTimerRef.current);
      notificationTimerRef.current = setTimeout(() => {
        setTopNotification(null);
        notificationTimerRef.current = null;
      }, 3800);
    };

    const handleRoomSettingsUpdated = (data: any) => {
      if (!data || !data.roomId) return;
      const roomId = String(data.roomId);

      setGlobalRooms(prev => prev.map(room => {
        const roomKeys = [
          room.id,
          room.accountId,
          room.roomId,
          room['Room Admin'],
        ].filter(Boolean).map(String);

        if (roomKeys.includes(roomId)) {
          return {
            ...room,
            name: data.roomName || data['Room Name'] || data.name || room.name,
            roomName: data.roomName || data['Room Name'] || data.name || room.roomName,
            image: data.dp || data.image || data.roomDp || data['Room dp'] || room.image,
            dp: data.dp || data.image || data.roomDp || data['Room dp'] || room.dp,
            roomDp: data.roomDp || data['Room dp'] || data.dp || data.image || room.roomDp,
            isLocked: data.isLocked !== undefined ? Boolean(data.isLocked) : room.isLocked,
            roomPassword: data.roomPassword !== undefined ? data.roomPassword : room.roomPassword,
            updatedAt: data.updatedAt || Date.now(),
          };
        }
        return room;
      }));

      setMyRoom(prev => {
        if (!prev) return prev;
        const isMine =
          String(prev.id) === roomId ||
          String(prev.accountId) === roomId;
        if (!isMine) return prev;

        const updated = {
          ...prev,
          name: data.roomName || data['Room Name'] || prev.name,
          image: data.roomDp || data['Room dp'] || prev.image,
          isLocked: data.isLocked !== undefined ? Boolean(data.isLocked) : prev.isLocked,
          roomPassword: data.roomPassword !== undefined ? data.roomPassword : prev.roomPassword,
        };
        localStorage.setItem('myRoom', JSON.stringify(updated));
        return updated;
      });
    };

    socket.on('private_message', handleIncomingPrivateMsg);
    socket.on('official_broadcast_message', handleIncomingOfficialBroadcast);
    socket.on('room_settings_updated', handleRoomSettingsUpdated);

    return () => {
      isMounted = false;
      if (interval) clearInterval(interval);
      if (notificationTimerRef.current) {
        clearTimeout(notificationTimerRef.current);
        notificationTimerRef.current = null;
      }
      window.removeEventListener('unread_count_updated', handleUnreadUpdated);
      socket.off('private_message', handleIncomingPrivateMsg);
      socket.off('official_broadcast_message', handleIncomingOfficialBroadcast);
      socket.off('room_settings_updated', handleRoomSettingsUpdated);
    };
  }, [userUID]);

  // ============ DYNAMIC OFFSET CALCULATION ============
  useEffect(() => {
    const userAgent = navigator.userAgent.toLowerCase();
    const isAndroidDevice = userAgent.includes('android');
    setIsAndroid(isAndroidDevice);

    const adjustOffset = () => {
      const dotsEl = bannerDotsRef.current;
      const cardsEl = categoryCardsRef.current;
      if (!dotsEl || !cardsEl) return;

      const dotsBottom = dotsEl.getBoundingClientRect().bottom;
      const cardsTop = cardsEl.getBoundingClientRect().top;
      const currentGap = cardsTop - dotsBottom;
      const desiredGap = 2;
      const deltaOffset = desiredGap - currentGap;
      setCategoryOffset(deltaOffset);
    };

    const timeoutId = setTimeout(adjustOffset, 100);

    const handleResize = () => {
      clearTimeout(timeoutId);
      setTimeout(adjustOffset, 50);
    };

    window.addEventListener('resize', handleResize);
    window.addEventListener('orientationchange', handleResize);

    return () => {
      clearTimeout(timeoutId);
      window.removeEventListener('resize', handleResize);
      window.removeEventListener('orientationchange', handleResize);
    };
  }, []);

  // ============ MOUNTED ============
  useEffect(() => {
    const id = setTimeout(() => setMounted(true), 30)
    return () => clearTimeout(id)
  }, [])

  // ============ VIEWPORT HEIGHT ============
  useEffect(() => {
    const setHeight = () => {
      const vh = window.innerHeight * 0.01
      document.documentElement.style.setProperty('--vh', `${vh}px`)
      setViewportHeight(window.innerHeight)
    }
    setHeight()
    window.addEventListener('resize', setHeight)
    window.addEventListener('orientationchange', setHeight)

    const isAndroidDevice = navigator.userAgent.toLowerCase().includes('android');
    if (isAndroidDevice) {
      setTimeout(setHeight, 100);
      setTimeout(setHeight, 300);
    }

    return () => {
      window.removeEventListener('resize', setHeight)
      window.removeEventListener('orientationchange', setHeight)
    }
  }, [])

  // ============ JITSI LOAD ============
  useEffect(() => {
    if (!document.getElementById('jitsi-script')) {
      const script = document.createElement('script')
      script.id = 'jitsi-script'
      script.src = 'https://meet.jit.si/external_api.js'
      script.async = true
      script.onload = () => { setJitsiLoaded(true) }
      document.body.appendChild(script)
    } else {
      setJitsiLoaded(true)
    }
  }, [])

  const initializeJitsiForListening = useCallback(() => {
    if (!jitsiLoaded || !jitsiContainerRef.current || jitsiApiRef.current) return

    const domain = 'meet.jit.si'
    const options = {
      roomName: 'hurry-global-lobby',
      width: '100%',
      height: '100%',
      parentNode: jitsiContainerRef.current,
      userInfo: {
        displayName: userName || 'Guest',
        email: (userUID || 'guest') + '@hurry.app'
      },
      configOverrides: {
        startWithAudioMuted: true,
        startWithVideoMuted: true,
        startAudioOnly: true,
        disableDeepLinking: true,
        prejoinPageEnabled: false,
        toolbarButtons: [],
        disableInviteFunctions: true,
        disablePolls: true,
        hideConferenceSubject: true,
        hideConferenceTimer: true,
        doNotStoreRoom: true,
        resolution: 180,
        constraints: { video: { height: { ideal: 180, max: 180, min: 180 } } },
      },
      interfaceConfigOverrides: {
        filmStripOnly: false,
        SHOW_JITSI_WATERMARK: false,
        SHOW_WATERMARK_FOR_GUESTS: false,
        SHOW_BRAND_WATERMARK: false,
        SHOW_POWERED_BY: false,
        SHOW_PROMOTIONAL_CLOSE_PAGE: false,
        TOOLBAR_ALWAYS_VISIBLE: false,
        DISABLE_VIDEO_BACKGROUND: true,
        HIDE_INVITE_MORE_HEADER: true,
        MOBILE_APP_PROMO: false,
        APP_NAME: 'Zyora',
        NATIVE_APP_NAME: 'Zyora',
        PROVIDER_NAME: 'Zyora'
      }
    }

    try {
      const JitsiMeetExternalAPI = (window as any).JitsiMeetExternalAPI;
      const api = new JitsiMeetExternalAPI(domain, options)
      jitsiApiRef.current = api
      jitsiJoinedRef.current = false

      api.addListener('videoConferenceJoined', () => {
        jitsiJoinedRef.current = true
        setIsJitsiJoined(true)
      })

      api.addListener('participantLeft', () => {})
    } catch (error) {
      console.error('Error initializing Jitsi:', error)
    }
  }, [jitsiLoaded, userName, userUID])

  useEffect(() => {
    if (jitsiLoaded && !jitsiApiRef.current && userName !== 'Guest') {
      initializeJitsiForListening()
    }
  }, [jitsiLoaded, userName, initializeJitsiForListening])

  useEffect(() => {
    return () => {
      if (jitsiApiRef.current) {
        jitsiApiRef.current.dispose()
        jitsiApiRef.current = null
      }
      jitsiJoinedRef.current = false
      setIsJitsiJoined(false)
    }
  }, [])

  // ============ GLOBAL ROOMS FETCHING ============
  useEffect(() => {
    let isMounted = true;

    loadGlobalRoomsFromDB().then(cachedRooms => {
      if (!isMounted) return;
      if (cachedRooms.length > 0) {
        const validRooms = cachedRooms.filter(room =>
          room &&
          room.name &&
          room.accountId !== 'undefined' &&
          room.accountId !== 'null' &&
          room.accountId !== '' &&
          room.accountId !== null &&
          room.name !== 'User'
        );
        setGlobalRooms(prev => {
          const liveCounts = new Map(
            prev.map(room => [
              String(room.id || room.accountId || ''),
              Number(room.activeUserCount || 0)
            ])
          );
          return validRooms.map(room => ({
            ...room,
            image: normalizeRoomImage(room.image || room.dp || room.roomDp, String(room.id || room.accountId || '')),
            activeUserCount:
              liveCounts.get(String(room.id || room.accountId || '')) ?? 0
          }));
        });
      }
    });

    const loadRooms = async () => {
      try {
        const rawRooms = await fetchAllRoomsFromMongoDB();
        if (!isMounted) return;
        if (Array.isArray(rawRooms)) {
          const rooms: GlobalRoom[] = rawRooms.map((data: any) => {
            const roomId = String(data.ID || data.id || data.roomId || '');
            const accId = String(data['Room Admin'] || data.accountId || generateStableId(roomId));
            return {
              id: roomId,
              name: data.name || data.roomName || data['Room Name'] || 'Room',
              country: data.Country || data.country || '🇮🇳',
              image: normalizeRoomImage(data.dp || data.roomDp || data['Room dp'] || data.image, roomId),
              accountId: accId,
              createdAt: data.createdAt || Date.now(),
              isLocked: Boolean(data.isLocked),
              roomPassword: data.roomPassword || null,
              isExplicitlyCreated: true,
              activeUserCount: Number(data.activeUserCount || 0)
            };
          });

          const validRooms = rooms.filter(room =>
            room.id &&
            room.accountId !== 'undefined' &&
            room.accountId !== 'null' &&
            room.accountId !== '' &&
            room.accountId !== null &&
            room.name &&
            room.name !== 'User'
          );

          setGlobalRooms(prev => {
            const liveCounts = new Map(
              prev.map(room => [
                String(room.id || room.accountId || ''),
                Number(room.activeUserCount || 0)
              ])
            );
            return validRooms.map(room => {
              let updatedCount = liveCounts.get(String(room.id || room.accountId || ''));
              return {
                ...room,
                activeUserCount: updatedCount !== undefined ? updatedCount : 0
              }
            });
          });

          saveGlobalRoomsToDB(validRooms);
        }
      } catch (err) {
        console.warn('Error fetching rooms from API:', err);
      }
    };

    let requestGeneration = 0;
    const loadRoomsSafe = async () => {
      const generation = ++requestGeneration;
      await loadRooms();
      // loadRooms is guarded by isMounted; generation prevents an older
      // in-flight refresh from being treated as the latest refresh.
      return generation;
    };

    void loadRoomsSafe();
    const interval = setInterval(() => { void loadRoomsSafe(); }, 12000);

    const fetchRoomsWithTimeout = async () => {
      try {
        const timeoutPromise = new Promise((_, reject) =>
          setTimeout(() => reject(new Error('Timeout')), 3000)
        );
        const res: any = await Promise.race([fetchAllRoomsFromMongoDB(), timeoutPromise]);
        if (!isMounted) return;
        const roomsArr: any[] = Array.isArray(res) ? res : (res?.rooms || []);
        if (roomsArr.length > 0) {
          const validRooms = roomsArr
            .map((data: any) => {
              const roomId = String(data.ID || data.id || data.roomId || '');
              const accId = String(data['Room Admin'] || data.accountId || generateStableId(roomId));
              return {
                id: roomId,
                name: data.name || data.roomName || data['Room Name'] || 'Room',
                country: data.Country || data.country || '🇮🇳',
                image: normalizeRoomImage(data.dp || data.roomDp || data['Room dp'] || data.image, roomId),
                accountId: accId,
                createdAt: data.createdAt || Date.now(),
                isLocked: Boolean(data.isLocked),
                roomPassword: data.roomPassword || null,
                isExplicitlyCreated: true,
                activeUserCount: Number(data.activeUserCount || 0)
              } as GlobalRoom;
            })
            .filter((room: GlobalRoom) =>
              room &&
              room.name !== 'User' &&
              room.name !== 'Zyora Room' &&
              room.accountId !== 'undefined' &&
              room.accountId !== 'null' &&
              room.accountId !== ''
            );
          setGlobalRooms(prev => {
            const liveCounts = new Map(
              prev.map(room => [
                String(room.id || room.accountId || ''),
                Number(room.activeUserCount || 0)
              ])
            );
            return validRooms.map(room => {
              let updatedCount = liveCounts.get(String(room.id || room.accountId || ''));
              return {
                ...room,
                activeUserCount: updatedCount !== undefined ? updatedCount : 0
              }
            });
          });
          validRooms.forEach((room: GlobalRoom) => saveRoomToDB(room));
        }
      } catch (err) {
        console.warn('Error/timeout syncing rooms:', err);
      }
    };
    if (!Capacitor.isNativePlatform()) fetchRoomsWithTimeout();

    return () => {
      isMounted = false;
      clearInterval(interval);
    };
  }, []);

  // ============ LIVE USER ONLINE OFFLINE PRESENCE ============
  useEffect(() => {
    const handlePresenceStatus = ({ userId, accountId, online }: { userId?: string; accountId?: string; online?: boolean }) => {
      const key = String(accountId || userId || '');
      if (!key) return;
      setUserPresence((prev) => ({ ...prev, [key]: Boolean(online) }));
    };

    const handleUserOnline = (presenceId: string) => {
      const key = String(presenceId || '');
      if (!key) return;
      setUserPresence((prev) => ({ ...prev, [key]: true }));
    };

    const handleUserOffline = (presenceId: string) => {
      const key = String(presenceId || '');
      if (!key) return;
      setUserPresence((prev) => ({ ...prev, [key]: false }));
    };

    socket.on('presence_status', handlePresenceStatus);
    socket.on('user_online', handleUserOnline);
    socket.on('user_offline', handleUserOffline);

    return () => {
      socket.off('presence_status', handlePresenceStatus);
      socket.off('user_online', handleUserOnline);
      socket.off('user_offline', handleUserOffline);
    };
  }, []);

  // ============ GLOBAL SOCKET.IO ROOM PRESENCE ============
  useEffect(() => {
    if (!userUID || userUID === 'N/A') return;

    const applyGlobalPresence = ({ rooms }: { rooms?: Array<{ roomId: string; roomName?: string; roomDp?: string; users?: Array<{ accountId?: string; userId?: string; name?: string; image?: string; email?: string }>; activeUserCount?: number }> }) => {
      if (!Array.isArray(rooms)) return;

      const activeRooms = rooms.filter(
        (room) => room && String(room.roomId || "") && Number(room.activeUserCount || 0) > 0
      );

      setGlobalRooms((prev) => {
        const merged = prev.map((room) => ({ ...room, activeUserCount: 0 }));

        activeRooms.forEach((liveRoom) => {
          const roomId = String(liveRoom.roomId || "");
          if (!roomId) return;

          const liveUsers = Array.isArray(liveRoom.users) ? liveRoom.users : [];
          const firstUser = liveUsers[0];

          const existing = merged.find((room) =>
            String(room.id || '') === roomId ||
            String(room.accountId || '') === roomId
          );

          if (existing) {
            existing.activeUserCount = Number(liveRoom.activeUserCount || liveUsers.length || 0);
          } else {
            merged.push({
              id: roomId,
              accountId: firstUser?.accountId || roomId,
              name: liveRoom.roomName || "Room",
              country: "🇮🇳",
              image: liveRoom.roomDp || "/default-avatar.png",
              createdAt: Date.now(),
              isLocked: false,
              roomPassword: undefined,
              isExplicitlyCreated: true,
              activeUserCount: Number(liveRoom.activeUserCount || liveUsers.length || 0),
            });
          }
        });

        return merged;
      });

      setSearchResults((prev) =>
        prev.map((room) => {
          const roomId = String(room.id || "");
          const accountId = String(room.accountId || "");
          const liveRoom = activeRooms.find((item) => {
            const liveId = String(item.roomId || "");
            return liveId === roomId || liveId === accountId;
          });
          return {
            ...room,
            activeUserCount: liveRoom
              ? Number(liveRoom.activeUserCount || (Array.isArray(liveRoom.users) ? liveRoom.users.length : 0))
              : 0,
          };
        })
      );
    };

    const handleSocketConnect = () => {
      const accountId = localStorage.getItem('accountNumber') || '';
      socket.emit('register', { userId: String(userUID), accountId: String(accountId) });
      socket.emit('global_room_presence_request');
    };

    socket.on('connect', handleSocketConnect);
    socket.on('global_room_presence', applyGlobalPresence);

    if (socket.connected) {
      handleSocketConnect();
    } else {
      socket.connect();
    }

    return () => {
      socket.off('connect', handleSocketConnect);
      socket.off('global_room_presence', applyGlobalPresence);
    };
  }, [userUID]);

  // ============ DRAG CIRCLE INITIAL POSITION ============
  useEffect(() => {
    if (typeof window !== 'undefined') {
      circleStartPos.current = {
        x: window.innerWidth - 16 - 48,
        y: window.innerHeight * 0.6
      }
      setDragPosition(circleStartPos.current)
    }
  }, [])

  const checkOverlap = useCallback((circleX: number, circleY: number) => {
    if (!deleteZoneRef.current) return false
    const deleteRect = deleteZoneRef.current.getBoundingClientRect()
    const circleSize = 48
    const circleCenter = { x: circleX + circleSize / 2, y: circleY + circleSize / 2 }
    const deleteCenter = { x: deleteRect.left + deleteRect.width / 2, y: deleteRect.top + deleteRect.height / 2 }
    const distance = Math.sqrt(
      Math.pow(circleCenter.x - deleteCenter.x, 2) +
      Math.pow(circleCenter.y - deleteCenter.y, 2)
    )
    return distance < 60
  }, [])

  // ============ LOAD PROFILE ============
  useEffect(() => {
    const loadProfile = async () => {
      const name = localStorage.getItem('userName') || ''
      const uid = localStorage.getItem('userUID') || localStorage.getItem('userPhone') || ''
      const storedAccNum = localStorage.getItem('accountNumber') || ''

      let storedProfile: any = {}
      try {
        const rawProfile = localStorage.getItem('userData') || localStorage.getItem('user')
        if (rawProfile) storedProfile = JSON.parse(rawProfile)
      } catch {}

      const photo =
        localStorage.getItem('userPhoto') ||
        storedProfile.image ||
        storedProfile.photo ||
        storedProfile.avatar ||
        storedProfile.photoURL ||
        '/default-avatar.png'

      setUserName(name)
      setUserPhoto(photo)
      setUserUID(uid)

      const roomCreated = localStorage.getItem('isRoomCreated')
      const roomData = localStorage.getItem('myRoom')

      if (roomCreated === 'true' && roomData) {
        try {
          const parsed = JSON.parse(roomData)
          let finalAccNum = storedAccNum || parsed.accountId;
          if (!finalAccNum && uid) {
            const accObj = getOrCreateAccountNumber(uid);
            finalAccNum = accObj.fullAccNum;
          }

          const roomBelongsToCurrentUser =
            (parsed.id && String(parsed.id) === String(uid)) ||
            (parsed.accountId && String(parsed.accountId) === String(finalAccNum));

          if (roomBelongsToCurrentUser) {
            setIsRoomCreated(true)

            let actualName = parsed.name;
            let actualDp = parsed.image || parsed.roomDp || '/default-avatar.png';

            if (uid) {
              try {
                const mongoRoom = await fetchRoomFromMongoDB(uid);
                if (mongoRoom) {
                  const mName = mongoRoom.name || mongoRoom.roomName || mongoRoom['Room Name'];
                  const mDp = mongoRoom.image || mongoRoom.dp || mongoRoom.roomDp || mongoRoom['Room dp'];
                  if (mName && mName !== 'My Room' && mName !== 'My room') actualName = mName;
                  if (mDp && mDp !== 'undefined' && mDp !== 'null') actualDp = mDp;
                }
              } catch (err) {
                console.warn('Error fetching room from MongoDB in loadProfile:', err);
              }
            }

            if (!actualName || actualName === 'My Room' || actualName === 'My room') {
              actualName = 'Room';
            }

            const updatedRoom = {
              ...parsed,
              id: parsed.id || uid,
              accountId: finalAccNum,
              name: actualName,
              image: actualDp
            };
            setMyRoom(updatedRoom);
            localStorage.setItem('myRoom', JSON.stringify(updatedRoom));
            await saveRoomToDB(updatedRoom);
          } else {
            setIsRoomCreated(false);
            setMyRoom(null);
            localStorage.removeItem('isRoomCreated');
            localStorage.removeItem('myRoom');
          }
        } catch (e) {
          setIsRoomCreated(false)
          setMyRoom(null)
        }
      } else {
        setIsRoomCreated(false)
        setMyRoom(null)

        if (storedAccNum) {
          const indexedRoom = await loadRoomFromDB(storedAccNum);
          if (
            indexedRoom &&
            ((indexedRoom.id && String(indexedRoom.id) === String(uid)) ||
              (indexedRoom.accountId && String(indexedRoom.accountId) === String(storedAccNum)))
          ) {
            const restoredRoom = {
              ...indexedRoom,
              id: indexedRoom.id || uid,
              accountId: indexedRoom.accountId || storedAccNum,
              image: indexedRoom.image || indexedRoom.roomDp || '/default-avatar.png'
            };
            setIsRoomCreated(true);
            setMyRoom(restoredRoom);
            localStorage.setItem('isRoomCreated', 'true');
            localStorage.setItem('myRoom', JSON.stringify(restoredRoom));
            await saveRoomToDB(restoredRoom);
          }
        }
      }

      const keptRoomData = localStorage.getItem('keptRoom')
      if (keptRoomData) {
        try { setKeptRoom(JSON.parse(keptRoomData)) } catch (e) { setKeptRoom(null) }
      }

      const storedRecent = localStorage.getItem('recentRooms')
      if (storedRecent) {
        try {
          const parsed = JSON.parse(storedRecent);
          const now = Date.now();
          const fiveMinutesAgo = now - (5 * 60 * 1000);
          const validRecent = parsed.filter((room: RecentRoom) => room.timestamp >= fiveMinutesAgo);
          setRecentRooms(validRecent);
          if (validRecent.length !== parsed.length) {
            localStorage.setItem('recentRooms', JSON.stringify(validRecent));
          }
          saveRecentToDB(validRecent);
        } catch {
          const indexedRecent = await loadRecentFromDB();
          setRecentRooms(indexedRecent);
        }
      } else {
        const indexedRecent = await loadRecentFromDB();
        setRecentRooms(indexedRecent);
      }

      const storedFollowing = localStorage.getItem('followingRooms')
      if (storedFollowing) {
        try {
          const parsed = JSON.parse(storedFollowing);
          setFollowingRooms(parsed);
          saveFollowingToDB(parsed);
        } catch {
          const indexedFollowing = await loadFollowingFromDB();
          setFollowingRooms(indexedFollowing);
        }
      } else {
        const indexedFollowing = await loadFollowingFromDB();
        setFollowingRooms(indexedFollowing);
      }
    }

    loadProfile()
    window.addEventListener('storage', loadProfile)
    return () => window.removeEventListener('storage', loadProfile)
  }, [])

  // ============ SAVE RECENT ROOMS ============
  useEffect(() => {
    localStorage.setItem('recentRooms', JSON.stringify(recentRooms))
    saveRecentToDB(recentRooms);
  }, [recentRooms])

  // ============ SAVE FOLLOWING ROOMS ============
  useEffect(() => {
    localStorage.setItem('followingRooms', JSON.stringify(followingRooms))
    saveFollowingToDB(followingRooms);
  }, [followingRooms])

  // ============ RECENT ROOMS 5 MIN AUTO REMOVE ============
  useEffect(() => {
    const checkRecentRoomsExpiry = () => {
      const now = Date.now();
      const fiveMinutesAgo = now - (5 * 60 * 1000);
      setRecentRooms(prev => prev.filter(room => room.timestamp >= fiveMinutesAgo));
    };
    const interval = setInterval(checkRecentRoomsExpiry, 30000);
    checkRecentRoomsExpiry();
    return () => clearInterval(interval);
  }, []);

  // ============ KEPT ROOM STORAGE CHANGE ============
  useEffect(() => {
    const handleStorageChange = (e: StorageEvent) => {
      if (e.key === 'keptRoom') {
        if (!e.newValue) {
          setKeptRoom(null)
          setEnteredFromKept(false)
        } else {
          try { setKeptRoom(JSON.parse(e.newValue)) } catch { setKeptRoom(null) }
        }
      }
    }
    window.addEventListener('storage', handleStorageChange)
    return () => window.removeEventListener('storage', handleStorageChange)
  }, [])

  // ============ BANNER AUTO ROTATE ============
  useEffect(() => {
    const interval = setInterval(() => {
      setCurrentBanner((prev) => (prev + 1) % BANNERS.length)
    }, 5000)
    return () => clearInterval(interval)
  }, [])

  // ============ INVITE FRIENDS HISTORY BACK HANDLING ============
  useEffect(() => {
    if (!isInviteFriendsOpen) return
    const handlePopState = () => { setIsInviteFriendsOpen(false) }
    window.history.pushState({ inviteFriendsModal: true }, '')
    window.addEventListener('popstate', handlePopState)
    return () => { window.removeEventListener('popstate', handlePopState) }
  }, [isInviteFriendsOpen])

  // Listen to hardware back press
  useEffect(() => {
    const handleHardwareBack = (e: Event) => {
      if (currentPage === 'home') {
        if (isSearchOpen) { e.preventDefault(); setIsSearchOpen(false); return; }
        if (isInviteFriendsOpen) { e.preventDefault(); setIsInviteFriendsOpen(false); return; }
        return;
      }
      if (currentPage !== 'room') {
        e.preventDefault();
        if (currentPage === 'public_profile') {
          handleBackFromPublicProfile();
        } else if (currentPage === 'leaderboard') {
          setCurrentPage('home');
        } else {
          setCurrentPage('home');
        }
      }
    };
    window.addEventListener('hardwareBackPress', handleHardwareBack);
    return () => { window.removeEventListener('hardwareBackPress', handleHardwareBack); };
  }, [currentPage, isSearchOpen, isInviteFriendsOpen]);

  // ============ SIGN IN DAY ============
  useEffect(() => {
    const savedDay = localStorage.getItem('signInDay')
    if (savedDay) setCurrentSignInDay(parseInt(savedDay))
  }, [])

  // ============ ADD TO RECENT ============
  const addToRecent = (room: KeptRoomData) => {
    setRecentRooms(prev => {
      const filtered = prev.filter(r => r.accountId !== room.accountId)
      const updated = [{ ...room, timestamp: Date.now() }, ...filtered].slice(0, 20);
      saveRecentToDB(updated);
      return updated;
    })
  }

  // ============ FOLLOW ROOM ============
  const handleFollowRoom = (room: KeptRoomData) => {
    setFollowingRooms(prev => {
      if (prev.some(r => r.accountId === room.accountId)) return prev
      const updated = [...prev, room];
      saveFollowingToDB(updated);
      return updated;
    })
  }

  // ============ UNFOLLOW ROOM ============
  const handleUnfollowRoom = (roomId: string) => {
    setFollowingRooms(prev => {
      const updated = prev.filter(r => r.accountId !== roomId);
      saveFollowingToDB(updated);
      return updated;
    })
  }

  // ============ DRAG HANDLERS ============
  const handleCircleMouseDown = (e: React.MouseEvent) => {
    e.preventDefault()
    e.stopPropagation()
    setIsDragging(true)
    setShowDeleteZone(true)
    dragStartPos.current = { x: e.clientX, y: e.clientY }
    circleStartPos.current = { x: dragPosition.x, y: dragPosition.y }
  }

  const handleCircleTouchStart = (e: React.TouchEvent) => {
    e.stopPropagation()
    const touch = e.touches[0]
    setIsDragging(true)
    setShowDeleteZone(true)
    dragStartPos.current = { x: touch.clientX, y: touch.clientY }
    circleStartPos.current = { x: dragPosition.x, y: dragPosition.y }
  }

  useEffect(() => {
    const handleMouseMove = (e: MouseEvent) => {
      if (!isDragging) return
      const deltaX = e.clientX - dragStartPos.current.x
      const deltaY = e.clientY - dragStartPos.current.y
      const newX = circleStartPos.current.x + deltaX
      const newY = circleStartPos.current.y + deltaY
      setDragPosition({ x: newX, y: newY })
      setIsOverDeleteZone(checkOverlap(newX, newY))
    }

    const handleMouseUp = () => {
      if (!isDragging) return
      const isOverlap = checkOverlap(dragPosition.x, dragPosition.y)
      if (isOverlap) {
        localStorage.removeItem('keptRoom')
        setKeptRoom(null)
        setEnteredFromKept(false)
      } else {
        setDragPosition(circleStartPos.current)
      }
      setIsDragging(false)
      setShowDeleteZone(false)
      setIsOverDeleteZone(false)
    }

    const handleTouchMove = (e: TouchEvent) => {
      if (!isDragging) return
      const touch = e.touches[0]
      const deltaX = touch.clientX - dragStartPos.current.x
      const deltaY = touch.clientY - dragStartPos.current.y
      const newX = circleStartPos.current.x + deltaX
      const newY = circleStartPos.current.y + deltaY
      setDragPosition({ x: newX, y: newY })
      setIsOverDeleteZone(checkOverlap(newX, newY))
    }

    const handleTouchEnd = () => {
      if (!isDragging) return
      const isOverlap = checkOverlap(dragPosition.x, dragPosition.y)
      if (isOverlap) {
        localStorage.removeItem('keptRoom')
        setKeptRoom(null)
        setEnteredFromKept(false)
      } else {
        setDragPosition(circleStartPos.current)
      }
      setIsDragging(false)
      setShowDeleteZone(false)
      setIsOverDeleteZone(false)
    }

    if (isDragging) {
      window.addEventListener('mousemove', handleMouseMove)
      window.addEventListener('mouseup', handleMouseUp)
      window.addEventListener('touchmove', handleTouchMove, { passive: true })
      window.addEventListener('touchend', handleTouchEnd)
    }

    return () => {
      window.removeEventListener('mousemove', handleMouseMove)
      window.removeEventListener('mouseup', handleMouseUp)
      window.removeEventListener('touchmove', handleTouchMove)
      window.removeEventListener('touchend', handleTouchEnd)
    }
  }, [isDragging, dragPosition, checkOverlap])

  // ============ BANNER SWIPE ============
  const handleTouchStart = (e: React.TouchEvent) => {
    touchStartX.current = e.touches[0].clientX
    touchEndX.current = e.touches[0].clientX
    setIsSwiping(true)
  }

  const handleTouchMove = (e: React.TouchEvent) => {
    if (!isSwiping) return
    touchEndX.current = e.touches[0].clientX
    const diff = touchEndX.current - touchStartX.current
    setSwipeOffset(diff)
  }

  const handleTouchEnd = () => {
    if (!isSwiping) return
    const diff = touchEndX.current - touchStartX.current
    const threshold = 50
    if (Math.abs(diff) > threshold) {
      if (diff > 0) {
        setCurrentBanner((prev) => (prev - 1 + BANNERS.length) % BANNERS.length)
      } else {
        setCurrentBanner((prev) => (prev + 1) % BANNERS.length)
      }
    }
    setIsSwiping(false)
    setSwipeOffset(0)
    touchStartX.current = 0
    touchEndX.current = 0
  }

  const handleMouseDown = (e: React.MouseEvent) => {
    touchStartX.current = e.clientX
    touchEndX.current = e.clientX
    setIsSwiping(true)
    e.preventDefault()
  }

  const handleMouseMove = (e: React.MouseEvent) => {
    if (!isSwiping) return
    touchEndX.current = e.clientX
    const diff = touchEndX.current - touchStartX.current
    setSwipeOffset(diff)
  }

  const handleMouseUp = () => {
    if (!isSwiping) return
    const diff = touchEndX.current - touchStartX.current
    const threshold = 50
    if (Math.abs(diff) > threshold) {
      if (diff > 0) {
        setCurrentBanner((prev) => (prev - 1 + BANNERS.length) % BANNERS.length)
      } else {
        setCurrentBanner((prev) => (prev + 1) % BANNERS.length)
      }
    }
    setIsSwiping(false)
    setSwipeOffset(0)
    touchStartX.current = 0
    touchEndX.current = 0
  }

  const handleMouseLeave = () => {
    if (isSwiping) handleMouseUp()
  }

  // ============ KEEP ROOM ============
  const handleKeepRoom = (roomData: KeptRoomData) => {
    setKeptRoom(roomData)
    setEnteredFromKept(false)
    localStorage.setItem('keptRoom', JSON.stringify(roomData))
    if (typeof window !== 'undefined') {
      circleStartPos.current = {
        x: window.innerWidth - 16 - 48,
        y: window.innerHeight * 0.6
      }
      setDragPosition(circleStartPos.current)
    }
  }

  // ============ KEPT ROOM CLICK ============
  const handleKeptRoomClick = () => {
    if (isDragging) return
    if (keptRoom) {
      addToRecent(keptRoom)
      setEnteredFromKept(true)
      const roomUser: UserCard = {
        id: keptRoom.accountId,
        accountId: keptRoom.accountId,
        name: keptRoom.name,
        country: keptRoom.country || '🇮🇳',
        image: keptRoom.image
      }
      setSelectedUser(roomUser)
      setCurrentPage('room')
    }
  }

  // ============ CREATE ROOM ============
  const handleCardClick = async () => {
    setEnteredFromKept(false);

    const rawAccNum = localStorage.getItem('accountNumber') || getOrCreateAccountNumber(userUID)
    const storedAccNum = typeof rawAccNum === 'string' ? rawAccNum : (rawAccNum as any).fullAccNum

    if (isRoomCreated && myRoom) {
      let currentRoomName = myRoom.name;
      if (!currentRoomName || currentRoomName === 'My Room' || currentRoomName === 'My room') {
        currentRoomName = 'Room';
      }
      let currentRoomDp = myRoom.image;
      if (!currentRoomDp || currentRoomDp === 'undefined' || currentRoomDp === 'null' || currentRoomDp === '/default-avatar.png') {
        currentRoomDp = '/default-avatar.png';
      }

      const updatedMyRoom = {
        ...myRoom,
        name: currentRoomName,
        image: currentRoomDp,
        accountId: myRoom.accountId || storedAccNum
      };

      setMyRoom(updatedMyRoom);
      localStorage.setItem('myRoom', JSON.stringify(updatedMyRoom));

      addToRecent({
        name: updatedMyRoom.name,
        image: updatedMyRoom.image,
        accountId: updatedMyRoom.accountId
      })
      setSelectedUser(updatedMyRoom)
      setCurrentPage('room')
      return;
    }

    const defaultRoomName = 'Room'

    const createdRoomCard: UserCard = {
      id: userUID,
      accountId: storedAccNum,
      name: defaultRoomName,
      country: localStorage.getItem('userCountry') || '🇮🇳',
      image: '/default-avatar.png'
    }

    localStorage.setItem('isRoomCreated', 'true')
    localStorage.setItem('myRoom', JSON.stringify(createdRoomCard))
    setIsRoomCreated(true)
    setMyRoom(createdRoomCard)

    await saveRoomToDB({
      ...createdRoomCard,
      isExplicitlyCreated: true,
      createdAt: Date.now()
    });

    const roomData = {
      id: userUID,
      name: defaultRoomName,
      country: localStorage.getItem("userCountry") || "🇮🇳",
      countryCode: localStorage.getItem("userCountryCode") || "IN",
      image: userPhoto || '/default-avatar.png',
      accountId: storedAccNum,
      createdAt: Date.now(),
      isLocked: false,
      roomPassword: null,
      isExplicitlyCreated: true,
      createdFromMineTab: true
    };

    try {
      await saveRoomToMongoDB({
        roomId: userUID,
        id: userUID, 
        roomName: defaultRoomName,
        roomDp: '/default-avatar.png',
        country: localStorage.getItem("userCountry") || "🇮🇳",
        roomAdmin: storedAccNum,
        message: `${defaultRoomName}'s Room Notice`,
        theme: 'default'
      });

      await saveUserToMongoDB({
        id: userUID,
        appLongId: userUID,
        name: userName || defaultRoomName,
        country: localStorage.getItem("userCountry") || "🇮🇳",
        image: '/default-avatar.png',
        accountId: storedAccNum
      });
    } catch (e) {
      console.warn('Error saving room/user to Google Sheets:', e);
    }

    setGlobalRooms(prev => {
      const filtered = prev.filter(r => r.accountId !== storedAccNum);
      const updated = [...filtered, roomData as unknown as GlobalRoom];
      saveGlobalRoomsToDB(updated);
      return updated;
    });

    addToRecent({
      name: createdRoomCard.name,
      image: createdRoomCard.image,
      accountId: storedAccNum
    })
    setSelectedUser(createdRoomCard)
    setCurrentPage('room')
  }

  const handleHouseClick = () => {
    handleCardClick()
  }

  // ============ USER CARD CLICK ============
  const handleUserCardClick = async (user: UserCard) => {
    const rawAccNum = localStorage.getItem('accountNumber') || getOrCreateAccountNumber(userUID)
    const currentAccountId = typeof rawAccNum === 'string' ? rawAccNum : (rawAccNum as any).fullAccNum

    const isOwner =
      (user.id && String(user.id) === String(userUID)) ||
      (user.accountId && String(user.accountId) === String(currentAccountId)) ||
      (myRoom && (user.id === myRoom.id || user.accountId === myRoom.accountId));

    if (isOwner) {
      let mongoOwnerRoom: any = null;
      try {
        mongoOwnerRoom = await fetchRoomFromMongoDB(String(currentAccountId || userUID));
      } catch (error) {
        console.warn('Owner room MongoDB fetch failed:', error);
      }

      let ownerName =
        mongoOwnerRoom?.['Room Name'] ||
        mongoOwnerRoom?.roomName ||
        mongoOwnerRoom?.name ||
        myRoom?.name ||
        "Room";
      if (!ownerName || ownerName === 'My Room' || ownerName === 'My room') {
        ownerName = 'Room';
      }
      let ownerDp =
        mongoOwnerRoom?.dp ||
        mongoOwnerRoom?.roomDp ||
        mongoOwnerRoom?.['Room dp'] ||
        mongoOwnerRoom?.image ||
        myRoom?.image ||
        "/default-avatar.png";
      if (!ownerDp || ownerDp === 'undefined' || ownerDp === 'null' || ownerDp === '/default-avatar.png') {
        ownerDp = '/default-avatar.png';
      }

      const ownerRoomUser: UserCard = {
        id: userUID,
        accountId: currentAccountId,
        name: ownerName,
        image: ownerDp,
        country: localStorage.getItem('userCountry') || '🇮🇳'
      };

      setEnteredFromKept(false);
      addToRecent({
        name: ownerRoomUser.name,
        image: ownerRoomUser.image,
        accountId: ownerRoomUser.accountId || ownerRoomUser.id,
      });
      setSelectedUser(ownerRoomUser);
      setCurrentPage('room');
      if (isSearchOpen) setIsSearchOpen(false);
      return;
    }

    const foundRoom = globalRooms.find(
      (r) =>
        String(r.id || '') === String(user.id || '') ||
        String(r.accountId || '') === String(user.accountId || '')
    )

    const canonicalRoomId = String(foundRoom?.id || user.id || user.accountId || '')

    if (!canonicalRoomId) {
      console.error('Room ID missing')
      return
    }

    const roomUser: UserCard = {
      ...user,
      id: canonicalRoomId,
      accountId: String(foundRoom?.accountId || user.accountId || canonicalRoomId),
      name: (foundRoom?.name && foundRoom.name !== 'My Room' && foundRoom.name !== 'My room' && foundRoom.name !== 'User')
          ? foundRoom.name
          : 'Room',
      image: (foundRoom?.image && foundRoom.image !== 'undefined' && foundRoom.image !== 'null' && foundRoom.image !== '/default-avatar.png')
          ? foundRoom.image
          : '/default-avatar.png',
      isLocked: foundRoom?.isLocked ?? user.isLocked,
    }

    try {
      const timeoutPromise = new Promise((_, reject) =>
        setTimeout(() => reject(new Error('Room fetch timeout')), 1200)
      );

      const roomData: any = await Promise.race([
        fetchRoomFromMongoDB(canonicalRoomId),
        timeoutPromise,
      ]);

      if (roomData) {
        if (
          roomData.isLocked &&
          String(roomData['Room Admin'] || roomData.accountId || '') !== String(currentAccountId)
        ) {
          setSelectedLockedRoom(roomUser)
          setShowRoomPasswordCard(true)
          setEnteredRoomPassword('')
          return
        }

        const rName = roomData.name || roomData.roomName || roomData['Room Name'];
        if (rName && rName !== 'My Room' && rName !== 'My room' && rName !== 'User') {
          roomUser.name = rName;
        } else {
          roomUser.name = 'Room';
        }

        const rDp = roomData.dp || roomData.roomDp || roomData['Room dp'] || roomData.image;
        if (rDp && rDp !== 'undefined' && rDp !== 'null' && rDp !== '/default-avatar.png') {
          roomUser.image = rDp;
        } else {
          roomUser.image = '/default-avatar.png';
        }

        roomUser.isLocked = Boolean(roomData.isLocked)
      }
    } catch (e) {
      console.warn('Failed to fetch room data or timed out:', e)
      if (foundRoom && foundRoom.isLocked && String(foundRoom.accountId) !== String(currentAccountId)) {
        setSelectedLockedRoom(roomUser)
        setShowRoomPasswordCard(true)
        setEnteredRoomPassword('')
        return
      }
    }

    setEnteredFromKept(false)
    addToRecent({
      name: roomUser.name,
      image: roomUser.image,
      accountId: roomUser.accountId || roomUser.id,
      isLocked: roomUser.isLocked
    })
    setSelectedUser(roomUser)
    setCurrentPage('room')
    if (isSearchOpen) setIsSearchOpen(false)
  };

  // ============ ROOM PASSWORD SUBMIT ============
  const handleRoomPasswordSubmit = async () => {
    if (!selectedLockedRoom) return;
    try {
      const roomData = await fetchRoomFromMongoDB(selectedLockedRoom.id || selectedLockedRoom.accountId || '');
      if (roomData) {
        if (roomData.isLocked && roomData.roomPassword === enteredRoomPassword) {
          setShowRoomPasswordCard(false)
          setEnteredFromKept(false)
          addToRecent({ name: selectedLockedRoom.name, image: selectedLockedRoom.image, accountId: selectedLockedRoom.accountId || selectedLockedRoom.id, isLocked: true })
          setSelectedUser(selectedLockedRoom)
          setCurrentPage('room')
          if (isSearchOpen) setIsSearchOpen(false)
          return;
        } else if (roomData.isLocked) {
          alert('Incorrect Password')
          return;
        }
      }
    } catch (e) {
      console.warn("Error verifying password via API:", e);
    }

    const foundRoom = globalRooms.find(r => r.id === selectedLockedRoom.id || (r.accountId && r.accountId === selectedLockedRoom.accountId));
    if (foundRoom) {
      if (foundRoom.isLocked && foundRoom.roomPassword === enteredRoomPassword) {
        setShowRoomPasswordCard(false)
        setEnteredFromKept(false)
        addToRecent({ name: selectedLockedRoom.name, image: selectedLockedRoom.image, accountId: selectedLockedRoom.accountId || selectedLockedRoom.id, isLocked: true })
        setSelectedUser(selectedLockedRoom)
        setCurrentPage('room')
        if (isSearchOpen) setIsSearchOpen(false)
      } else {
        alert('Incorrect Password')
      }
    } else {
      alert('Room not found')
    }
  }

  // ============ USER PROFILE CLICK ============
  const handleUserProfileClick = (user: UserCard) => {
    setSelectedUser(user)
    setIsPublicProfileActive(true)
    setCurrentPage('public_profile')
    if (isSearchOpen) setIsSearchOpen(false)
  }

  // ============ BACK FROM ROOM ============
  const handleBackFromRoom = async () => {
    if (enteredFromKept) {
      localStorage.removeItem('keptRoom')
      setKeptRoom(null)
      setEnteredFromKept(false)
    }
    setCurrentPage('home')
    setSelectedUser(null)
  }

  // ============ BACK FROM PUBLIC PROFILE ============
  const handleBackFromPublicProfile = () => {
    setIsPublicProfileActive(false)
    setCurrentPage('home')
    setSelectedUser(null)
  }

  // ============ JOIN ROOM FROM CHAT ============
  const handleJoinRoomFromChat = async (roomId: string) => {
    try {
      const id = String(roomId || "");
      const foundRoom = globalRooms.find(
        (r) => String(r.id || "") === id || String(r.accountId || "") === id
      );

      if (foundRoom) {
        handleUserCardClick({
          id: foundRoom.id || id,
          accountId: foundRoom.accountId || id,
          name: foundRoom.name || "User",
          country: foundRoom.country || "🇮🇳",
          image: foundRoom.image || "/default-avatar.png",
        });
        return;
      }

      const roomData = await fetchRoomFromMongoDB(id);
      if (roomData) {
        handleUserCardClick({
          id: roomData.ID || roomData.id || roomData.roomId || id,
          accountId: roomData["Room Admin"] || roomData.accountId || id,
          name: roomData.name || roomData.roomName || roomData["Room Name"] || "Room",
          country: roomData.Country || roomData.country || "🇮🇳",
          image: roomData.image || roomData.dp || roomData.roomDp || roomData["Room dp"] || "/default-avatar.png",
        });
        return;
      }
      console.error("Room not found:", id);
    } catch (error) {
      console.error("Error joining room:", error);
    }
  };

    // ============ REAL-TIME LIVE SEARCH ============
  const handlePerformSearch = useCallback(async (queryParam?: string) => {
    const queryRaw = (typeof queryParam === 'string' ? queryParam : searchQuery).trim();
    if (!queryRaw) {
      setSearchResults([])
      setHasSearched(false)
      setIsSearching(false)
      return
    }

    setIsSearching(true)
    setHasSearched(false)

    try {
      const results = await fetchSearchResults(queryRaw, globalRoomsRef.current)
      setSearchResults(results)
      setHasSearched(true)
    } catch (err) {
      console.error("Search error:", err)
      setSearchResults([])
      setHasSearched(true)
    } finally {
      setIsSearching(false)
    }
  }, [searchQuery]);

  useEffect(() => {
    if (!isSearchOpen) return;

    const trimmed = searchQuery.trim();
    if (!trimmed) {
      setSearchResults([]);
      setHasSearched(false);
      setIsSearching(false);
      return;
    }

    const queryLower = trimmed.toLowerCase();
    const immediateLocal = globalRoomsRef.current.filter(room => {
      const acc = String(room.accountId || "").toLowerCase();
      const id = String(room.id || "").toLowerCase();
      const name = String(room.name || "").toLowerCase();
      return acc.includes(queryLower) || id.includes(queryLower) || name.includes(queryLower);
    });

    if (immediateLocal.length > 0) {
      setSearchResults(immediateLocal);
      setHasSearched(true);
    } else {
      setIsSearching(true);
    }

    const timer = setTimeout(() => {
      handlePerformSearch(trimmed);
    }, 200);

    return () => clearTimeout(timer);
  }, [searchQuery, isSearchOpen, handlePerformSearch]);

  // ============ SIGN IN MODAL ============
  const handleImageClick = () => { setIsSignInModalOpen(true) }
  const handleCloseModal = () => { setIsSignInModalOpen(false) }
  const handleSignIn = async () => {
    if (claimedToday || signInClaimingRef.current) return
    signInClaimingRef.current = true

    const rewardByDay: Record<number, { coins: number; itemIds: string[] }> = {
      1: { coins: 5000, itemIds: [] },
      2: { coins: 5000, itemIds: [] },
      3: { coins: 0, itemIds: ['daily_d3_frame'] },
      4: { coins: 10000, itemIds: [] },
      5: { coins: 10000, itemIds: ['daily_d5_frame'] },
      6: { coins: 10000, itemIds: ['daily_d6_theme'] },
      7: { coins: 15000, itemIds: ['daily_d7_frame', 'daily_d7_theme'] },
    }

    const reward = rewardByDay[currentSignInDay]
    if (!reward) {
      signInClaimingRef.current = false
      return
    }

    try {
      const db = await new Promise<IDBDatabase>((resolve, reject) => {
        const request = indexedDB.open('FruitPartyDB', 3)
        request.onupgradeneeded = () => {
          const db = request.result
          if (!db.objectStoreNames.contains('GameState')) db.createObjectStore('GameState')
        }
        request.onsuccess = () => resolve(request.result)
        request.onerror = () => reject(request.error)
      })

      await new Promise<void>((resolve, reject) => {
        const tx = db.transaction('GameState', 'readwrite')
        const store = tx.objectStore('GameState')
        const req = store.get('user_data')
        req.onsuccess = () => {
          const data = req.result || {}
          const currentBalance = typeof data.balance === 'number' ? data.balance : 82927
          const ownedItems = Array.isArray(data.ownedItems) ? [...data.ownedItems] : []
          for (const itemId of reward.itemIds) {
            if (!ownedItems.includes(itemId)) ownedItems.push(itemId)
          }
          const putReq = store.put({
            ...data,
            balance: currentBalance + reward.coins,
            ownedItems,
          }, 'user_data')
          putReq.onsuccess = () => resolve()
          putReq.onerror = () => reject(putReq.error)
        }
        req.onerror = () => reject(req.error)
        tx.onerror = () => reject(tx.error)
      })
      db.close()

      // Keep the reward state in browser/WebView storage and mirror the
      // successful claim to the backend so APK reinstall does not reset it.
      const today = getDailyDateKey()
      try {
        const response = await fetch(apiUrl('/api/daily-checkin'), {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            userId: userUID,
            dateKey: today,
            currentDay: currentSignInDay,
          }),
        })
        if (response.ok) {
          const serverData = await response.json()
          if (serverData?.alreadyClaimed) {
            const serverDay = Number(serverData.currentDay)
            if (serverDay >= 1 && serverDay <= 7) {
              setCurrentSignInDay(serverDay)
              localStorage.setItem(`signInDay_${userUID}`, String(serverDay))
            }
            setClaimedToday(true)
            localStorage.setItem(`signInClaimDate_${userUID}`, today)
            setIsSignInModalOpen(false)
            return
          }
        }
      } catch {}

      const nextDay = currentSignInDay < 7 ? currentSignInDay + 1 : 1
      localStorage.setItem(`signInDay_${userUID}`, String(nextDay))
      localStorage.setItem(`signInClaimDate_${userUID}`, today)
      setCurrentSignInDay(nextDay)
      setClaimedToday(true)
      setIsSignInModalOpen(false)
      alert(`Day ${currentSignInDay} reward claimed! 🎉`)
    } catch (error) {
      console.error('Daily check-in reward claim failed:', error)
      alert('Reward claim failed. Please try again.')
    } finally {
      signInClaimingRef.current = false
    }
  }

  // ============ VIEWPORT META ============
  useEffect(() => {
    const existingMeta = document.querySelector('meta[name="viewport"]')
    if (existingMeta) existingMeta.remove()
    const meta = document.createElement('meta')
    meta.name = 'viewport'
    meta.content = 'width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover'
    document.head.appendChild(meta)
    return () => {
      const metaTag = document.querySelector('meta[name="viewport"]')
      if (metaTag && metaTag.getAttribute('content') === 'width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover') {
        metaTag.remove()
      }
    }
  }, [])

  // ============ PAGE EFFECTS ============
  useEffect(() => {
    if (currentPage !== 'message') setIsChatOpen(false)
  }, [currentPage])

  useEffect(() => {
    if (currentPage !== 'me' && currentPage !== 'public_profile') {
      setIsPublicProfileActive(false)
    }
  }, [currentPage])

  // ============ ALL ROOMS FILTER ============
  const allRooms = (() => {
    const seenKeys = new Set<string>();
    const result: GlobalRoom[] = [];

    for (const room of globalRooms) {
      if (!room || !room.name || !room.image) continue;
      if (room.name === 'My Room' || room.name === 'My room' || room.name === 'User') continue;
      if (/jiys/i.test(room.name)) continue;
      if (
        room.accountId === 'undefined' || room.accountId === 'null' ||
        room.accountId === '' || room.accountId === null
      ) continue;

      if (!(Number(room.activeUserCount || 0) > 0)) continue;

      const rid = String(room.id || '');
      const racc = String(room.accountId || '');

      if (rid && seenKeys.has(rid)) continue;
      if (racc && seenKeys.has(racc)) continue;

      if (rid) seenKeys.add(rid);
      if (racc) seenKeys.add(racc);

      result.push(room);
    }

    return result;
  })();

  // ============ RENDER POPULAR TAB ============
  const renderPopularTab = () => {
    return (
      <>
        <div
          ref={categoryCardsRef}
          className="px-3"
          style={{
            transform: `translateY(${categoryOffset}px)`,
            marginBottom: '0px',
            position: 'relative',
            zIndex: 10,
            willChange: 'transform'
          }}
        >
          <div
            className="flex flex-row justify-between items-center gap-1 select-none"
            style={{ fontFamily: 'Nunito, Inter, sans-serif', marginBottom: '0px' }}
          >
            {CATEGORY_CARDS.map((card, i) => {
              const isHonour = card.label?.toLowerCase().includes('honour');
              return (
                <div
                  key={card.label}
                  onClick={() => {
                    setLeaderboardTab(card.tab);
                    setCurrentPage('leaderboard');
                  }}
                  className="group flex-1 cursor-pointer"
                  style={{
                    height: '92px',
                    minHeight: '92px',
                    maxHeight: '92px',
                    minWidth: 0,
                    borderRadius: '16px',
                    opacity: mounted ? 1 : 0,
                    transform: mounted ? 'translateY(0) scale(1)' : 'translateY(14px) scale(0.96)',
                    transition: 'transform 420ms cubic-bezier(0.34,1.56,0.64,1), box-shadow 280ms ease, opacity 420ms ease',
                    animation: mounted ? 'cardIn 560ms cubic-bezier(0.22,1,0.36,1) both' : 'none',
                    animationDelay: `${i * 100}ms`,
                    position: 'relative',
                    overflow: 'hidden',
                    zIndex: 30
                  }}
                >
                  <div
                    className="relative w-full text-center font-black uppercase tracking-wider select-none z-50 pointer-events-none"
                    style={{
                      paddingTop: '16px',
                      fontSize: '11px',
                      lineHeight: '1.2',
                      background: 'linear-gradient(180deg, #FFFFFF 0%, #FFF8B5 35%, #FFD700 70%, #F5B000 100%)',
                      WebkitBackgroundClip: 'text',
                      WebkitTextFillColor: 'transparent',
                      WebkitTextStroke: '0.4px rgba(100, 60, 0, 0.7)',
                      filter: 'drop-shadow(0px 2px 2px rgba(0, 0, 0, 0.9)) drop-shadow(0px 0px 5px rgba(255, 215, 0, 0.6))',
                    }}
                  >
                    {card.label}
                  </div>

                  <img
                    src={card.bgImage}
                    alt={card.label}
                    className={`absolute inset-0 w-full h-full object-contain z-0 pointer-events-none translate-y-1 ${
                      isHonour ? 'scale-[1.08]' : 'scale-102'
                    }`}
                    draggable="false"
                  />

                  <div className="absolute left-0 right-0 bottom-4 w-full z-40 pointer-events-none block translate-y-2">
                    <div
                      className="relative w-[85%] mx-auto flex items-center justify-center z-10"
                      style={{
                        animation: 'none',
                        marginBottom: '0px',
                        transformOrigin: 'center'
                      }}
                    >
                      <img src="/file_00000000048882118276c7215012963f.png" alt="Frame" className="w-full h-auto block z-30" draggable="false" />

                      <div className="absolute inset-0 flex flex-row items-center justify-center z-20">
                        <img
                          src="/logo.png"
                          alt="Left"
                          className="rounded-full object-cover shadow-sm relative shrink-0"
                          style={{ width: '24%', height: 'auto', aspectRatio: '1/1', marginTop: '4%', marginRight: '3%' }}
                        />
                        <img
                          src="/logo.png"
                          alt="Middle"
                          className="rounded-full object-cover shadow-md border-[1.5px] border-white/80 relative shrink-0 z-10"
                          style={{ width: '32%', height: 'auto', aspectRatio: '1/1', marginBottom: '3%' }}
                        />
                        <img
                          src="/logo.png"
                          alt="Right"
                          className="rounded-full object-cover shadow-sm relative shrink-0"
                          style={{ width: '24%', height: 'auto', aspectRatio: '1/1', marginTop: '4%', marginLeft: '3%' }}
                        />
                      </div>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {allRooms.length > 0 ? (
          <div className="px-3" style={{ marginTop: '0.0vh' }}>
            <div className="grid grid-cols-2 gap-x-1.5 gap-y-1 items-start">
              {allRooms.map((room, index) => (
                <div
                  key={room.accountId}
                  onClick={() =>
                    handleUserCardClick({
                      id: room.id,
                      accountId: room.accountId,
                      name: room.name,
                      country: room.country,
                      image: room.image,
                      isLocked: room.isLocked,
                    })
                  }
                >
                  <div
                    className="relative cursor-pointer group hover:shadow-lg transition-all hover:scale-[1.02] active:scale-95"
                    style={{ width: '100%', aspectRatio: '1 / 1', minWidth: 0, minHeight: 0 }}
                  >
                    <div className="w-full h-full bg-gray-200 rounded-md overflow-hidden relative"
                      style={{ width: '100%', height: '100%', aspectRatio: '1 / 1' }}>
                      <img
                        src={
                          room.image && room.image !== "undefined" && room.image !== "null"
                            ? room.image
                            : "/default-avatar.png"
                        }
                        onError={(e) => {
                          (e.target as HTMLImageElement).src = "/default-avatar.png";
                        }}
                        alt={room.name}
                        className="w-full h-full object-cover"
                        draggable="false"
                      />
                    </div>

                    {(index === 0 || index === 1 || index === 2) && (
                      <img
                        src={
                          index === 0
                            ? "/IMG_20260922_112156.png"
                            : index === 1
                            ? "/IMG_20260922_112129.png"
                            : "/IMG_20260922_112110.png"
                        }
                        alt={`Rank ${index + 1} badge`}
                        className="absolute pointer-events-none z-50"
                        style={{
                          top: '-11px',
                          right: '-6px',
                          width: '55px',
                          height: '55px',
                          objectFit: 'contain',
                        }}
                        draggable="false"
                      />
                    )}

                    {room.isLocked && (
                      <div className="absolute top-2 right-2 bg-white/20 backdrop-blur-md rounded-full p-1.5 border border-white/50 z-50">
                        <svg viewBox="0 0 24 24" className="w-4 h-4 fill-white">
                          <path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zM9 6c0-1.66 1.34-3 3-3s3 1.34 3 3v2H9V6zm9 14H6V10h12v10zm-6-3c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2z" />
                        </svg>
                      </div>
                    )}

                    <LiveRoomStats />
                  </div>

                  <div className="mt-0.5 px-1">
                    <div className="flex items-center gap-0.5">
                      <span className="text-sm">{room.country}</span>
                      <span className="font-semibold text-gray-900 text-sm truncate">
                        {room.name}
                      </span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        ) : null}
      </>
    );
  };

  // ============ MAIN RETURN ============
  return (
    <div
      className="min-h-screen bg-gradient-to-b from-blue-400 via-blue-100 to-white"
      style={{
        minHeight: viewportHeight ? `calc(var(--vh, 1vh) * 100)` : '100vh',
        paddingBottom: '0px',
        WebkitTapHighlightColor: 'transparent',
        touchAction: 'manipulation',
        WebkitUserSelect: 'none',
        userSelect: 'none',
        WebkitTouchCallout: 'none'
      }}
    >
      <div
        ref={jitsiContainerRef}
        className="absolute inset-0 z-0 opacity-0 pointer-events-none"
        style={{ width: '1px', height: '1px' }}
      />

      <style>{`
        @import url('https://fonts.googleapis.com/css2?family=Nunito:wght@600;700&display=swap');
        * { -webkit-text-size-adjust: 100%; -ms-text-size-adjust: 100%; touch-action: manipulation; }
        button, a, div, span { touch-action: manipulation; }
        @keyframes cardIn { 0% { opacity: 0; transform: translateY(14px) scale(0.96); } 100% { opacity: 1; transform: translateY(0) scale(1); } }
        @keyframes fadeInBanner { 0% { opacity: 0; } 100% { opacity: 1; } }
        @keyframes pulseGlow { 0%, 100% { box-shadow: 0 0 0 0 rgba(59, 130, 246, 0.4); } 50% { box-shadow: 0 0 0 8px rgba(59, 130, 246, 0); } }
        @keyframes deletePulse { 0%, 100% { transform: scale(1); } 50% { transform: scale(1.05); } }
        @keyframes modalFadeIn { 0% { opacity: 0; transform: scale(0.9); } 100% { opacity: 1; transform: scale(1); } }
        @keyframes modalOverlayIn { 0% { opacity: 0; } 100% { opacity: 1; } }
        @keyframes slideDownNotif { 0% { transform: translateY(-100%) scale(0.95); opacity: 0; } 100% { transform: translateY(0) scale(1); opacity: 1; } }
        .animate-slide-down { animation: slideDownNotif 0.35s cubic-bezier(0.16, 1, 0.3, 1) forwards; }
        @keyframes slideUpSheet { 0% { transform: translateY(100%); } 100% { transform: translateY(0); } }
        @keyframes popIconAnim { 0% { transform: scale(0.7); opacity: 0.5; } 60% { transform: scale(1.15); opacity: 1; } 100% { transform: scale(1); opacity: 1; } }
        .icon-active-anim { animation: popIconAnim 0.35s cubic-bezier(0.175, 0.885, 0.32, 1.275) forwards; transform-origin: center; }
        .hide-scrollbar { -ms-overflow-style: none; scrollbar-width: none; }
        .hide-scrollbar::-webkit-scrollbar { display: none; }
        .truncate { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
      `}</style>

      {isInviteFriendsOpen && (
        <div className="fixed inset-0 z-[200]">
          <InviteFriends
            onBack={() => {
              setIsInviteFriendsOpen(false)
              if (window.history.state?.inviteFriendsModal) window.history.back()
            }}
            onClose={() => {
              setIsInviteFriendsOpen(false)
              if (window.history.state?.inviteFriendsModal) window.history.back()
            }}
          />
        </div>
      )}

      {showRoomPasswordCard && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center">
          <div className="absolute inset-0 bg-black/50 backdrop-blur-sm" onClick={() => setShowRoomPasswordCard(false)} />
          <div className="relative bg-white w-80 rounded-3xl shadow-2xl p-6 mx-4 animate-scale-up">
            <h3 className="text-xl font-bold text-gray-800 text-center mb-2">Locked Room</h3>
            <p className="text-sm text-gray-500 text-center mb-6">Enter password to join</p>
            <PasswordInput value={enteredRoomPassword} onChange={setEnteredRoomPassword} />
            <button
              onClick={handleRoomPasswordSubmit}
              disabled={enteredRoomPassword.length !== 4}
              className={`w-full mt-6 py-3.5 rounded-2xl font-semibold text-white transition-all ${
                enteredRoomPassword.length === 4
                  ? 'bg-blue-500 hover:bg-blue-600 active:scale-[0.98]'
                  : 'bg-gray-200 text-gray-400 cursor-not-allowed'
              }`}
            >
              Enter Room
            </button>
            <button
              onClick={() => {
                setShowRoomPasswordCard(false)
                setEnteredRoomPassword('')
              }}
              className="w-full mt-3 py-3 text-gray-500 font-medium text-center hover:bg-gray-50 rounded-2xl active:scale-[0.98] transition-all"
            >
              Cancel
            </button>
          </div>
        </div>
      )}

      {isSearchOpen && (
        <SearchPage
          onClose={() => setIsSearchOpen(false)}
          searchQuery={searchQuery}
          setSearchQuery={setSearchQuery}
          activeSearchTab={activeSearchTab}
          setActiveSearchTab={setActiveSearchTab}
          searchResults={searchResults}
          hasSearched={hasSearched}
          isSearching={isSearching}
          onPerformSearch={handlePerformSearch}
          onUserProfileClick={handleUserProfileClick}
          onUserCardClick={handleUserCardClick}
          viewportHeight={viewportHeight}
        />
      )}

      <DailyCheckInModal
        isOpen={isSignInModalOpen}
        onClose={handleCloseModal}
        currentDay={currentSignInDay}
        onSignIn={handleSignIn}
        claimedToday={claimedToday}
      />

      {showDeleteZone && keptRoom && (
        <div
          ref={deleteZoneRef}
          className="fixed bottom-4 right-4 z-[60] transition-all duration-300"
          style={{ animation: isOverDeleteZone ? 'deletePulse 0.5s ease-in-out infinite' : 'none' }}
        >
          <div
            className={`flex items-center justify-center rounded-full transition-all duration-300 ${
              isOverDeleteZone
                ? 'w-16 h-16 bg-red-600 shadow-lg shadow-red-500/50 scale-110'
                : 'w-14 h-14 bg-red-500/60'
            }`}
          >
            <svg
              viewBox="0 0 24 24"
              className={`transition-all duration-300 ${isOverDeleteZone ? 'w-8 h-8' : 'w-6 h-6'}`}
              fill="none"
              stroke="white"
              strokeWidth="2.5"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <path d="M3 6h18" />
              <path d="M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
              <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6" />
              <line x1="10" y1="11" x2="10" y2="17" />
              <line x1="14" y1="11" x2="14" y2="17" />
            </svg>
          </div>
        </div>
      )}

      {keptRoom && currentPage === 'home' && !isSearchOpen && (
        <div
          ref={circleRef}
          className={`fixed z-50 cursor-grab active:cursor-grabbing group ${
            isDragging ? 'transition-none' : 'transition-all duration-300'
          } ${isOverDeleteZone ? 'opacity-50 scale-75' : 'opacity-100'}`}
          style={{ left: `${dragPosition.x}px`, top: `${dragPosition.y}px`, touchAction: 'none' }}
          onClick={handleKeptRoomClick}
          onMouseDown={handleCircleMouseDown}
          onTouchStart={handleCircleTouchStart}
        >
          <div className="relative">
            <div
              className="w-12 h-12 rounded-full overflow-hidden border-2 border-white shadow-lg bg-white"
              style={{ animation: isDragging ? 'none' : 'pulseGlow 2s infinite' }}
            >
              <img
                src={keptRoom.image}
                alt={keptRoom.name}
                className="w-full h-full object-cover pointer-events-none"
                draggable="false"
              />
            </div>
            <div className="absolute -bottom-1 -right-1 w-4 h-4 bg-green-500 rounded-full border-2 border-white pointer-events-none"></div>
          </div>
          {!isDragging && (
            <div className="absolute -bottom-6 left-1/2 -translate-x-1/2 bg-black/70 text-white text-[10px] px-2 py-0.5 rounded-full whitespace-nowrap opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none">
              {keptRoom.name}
            </div>
          )}
        </div>
      )}

      {!isChatOpen && currentPage !== 'room' && !isPublicProfileActive && !isSearchOpen && currentPage !== 'leaderboard' && (
        <div
          className="fixed right-4 z-40"
          style={{ bottom: 'calc(75px + max(env(safe-area-inset-bottom, 0px),16px))' }}
        >
          <img
            src="/IMG_20260916_002115.png"
            alt="Corner decoration"
            className="rounded-none object-contain cursor-pointer hover:scale-105 transition-transform active:scale-95"
            style={{ width: '70px', height: '70px' }}
            onClick={handleImageClick}
          />
        </div>
      )}

      <div
        className="w-full"
        style={{
          paddingBottom:
            !isChatOpen && currentPage !== 'room' && !isPublicProfileActive && !isSearchOpen && currentPage !== 'leaderboard'
              ? 'calc(65px + env(safe-area-inset-bottom, 0px))'
              : '0px',
          boxSizing: 'border-box',
        }}
      >
        {currentPage === 'home' && (
          <div
            className="w-full bg-white"
            style={{ minHeight: viewportHeight ? `calc(var(--vh, 1vh) * 100)` : '100vh' }}
          >
            <div
              ref={bannerContainerRef}
              className="w-full px-3 pt-2"
              style={{
                height: 'auto',
                minHeight: 'auto',
                background: activeTab === 'mine'
                  ? 'linear-gradient(to bottom, #3b82f6 0%, #eff6ff 60%, #ffffff 100%)'
                  : 'linear-gradient(to bottom, #3b82f6 0%, #eff6ff 70%, #ffffff 100%)',
                paddingBottom: '0.7vh',
                paddingTop: '35px'
              }}
            >
              <div className="w-full flex justify-between items-center py-1 box-border mb-1 px-1">
                <div className="flex items-center gap-3">
                  <button
                    type="button"
                    onClick={() => setActiveTab('mine')}
                    className={`font-bold text-[22px] tracking-[0.2px] transition-colors relative pb-1.5 ${
                      activeTab === 'mine' ? 'font-extrabold text-[#1E1E1E]' : 'text-[#6E6E6E]'
                    }`}
                  >
                    {t.mine || 'Mine'}
                    {activeTab === 'mine' && (
                      <svg className="absolute -bottom-1 left-1/2 -translate-x-1/2" width="16" height="7" viewBox="0 0 16 7" fill="none">
                        <path d="M2 1.5 Q 8 6 14 1.5" stroke="#1E1E1E" strokeWidth="3" strokeLinecap="round" />
                      </svg>
                    )}
                  </button>

                  <button
                    type="button"
                    onClick={() => setActiveTab('popular')}
                    className={`font-bold text-[22px] tracking-[0.2px] transition-colors relative pb-1.5 ${
                      activeTab === 'popular' ? 'font-extrabold text-[#1E1E1E]' : 'text-[#6E6E6E]'
                    }`}
                  >
                    {t.popular || 'Popular'}
                    {activeTab === 'popular' && (
                      <svg className="absolute bottom-0 left-1/2 -translate-x-1/2" width="16" height="7" viewBox="0 0 16 7" fill="none">
                        <path d="M2 1.5 Q 8 6 14 1.5" stroke="#1E1E1E" strokeWidth="3" strokeLinecap="round" />
                      </svg>
                    )}
                  </button>
                </div>

                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => setIsSearchOpen(true)}
                    className="flex items-center justify-center cursor-pointer active:scale-95 transition-transform p-1"
                    aria-label="Search"
                  >
                    <svg width="26" height="26" viewBox="0 0 28 28" fill="none">
                      <circle cx="12.5" cy="12.5" r="7" stroke="#2D2D2D" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
                      <path d="M18.2 18.2 L24 24" stroke="#2D2D2D" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
                    </svg>
                  </button>

                  <button
                    type="button"
                    onClick={handleHouseClick}
                    className="flex items-center justify-center cursor-pointer p-0"
                    aria-label="Home"
                  >
                    <svg width="32" height="32" viewBox="0 0 32 32" fill="none">
                      <path
                        d="M16 3.5 C 14.5 3.5, 3 8, 3 13.5 L 3 21.5 C 3 25.5, 6 28.5, 10.5 28.5 H 21.5 C 26 28.5, 29 25.5, 29 21.5 L 29 13.5 C 29 8, 17.5 3.5, 16 3.5 Z"
                        stroke="#2D2D2D"
                        strokeWidth="2.2"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                      />
                      <rect x="9" y="14.5" width="3.5" height="6" rx="1.5" fill="#2D2D2D" />
                      <rect x="14.2" y="11.5" width="3.5" height="9" rx="1.5" fill="#2D2D2D" />
                      <rect x="19.5" y="14" width="3.5" height="6.5" rx="1.5" fill="#2D2D2D" />
                    </svg>
                  </button>
                </div>
              </div>

              {activeTab === 'popular' && (
                <>
                  <div className="relative">
                    <div
                      ref={bannerRef}
                      className="rounded-md relative overflow-hidden cursor-pointer select-none"
                      style={{
                        height: '13.5vh',
                        width: '100%',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        transform: isSwiping ? `translateX(${swipeOffset}px)` : 'translateX(0)',
                        transition: isSwiping ? 'none' : 'transform 0.3s ease-out',
                        touchAction: 'pan-y',
                      }}
                      onClick={() => {
                        if (Math.abs(swipeOffset) < 10) {
                          if (currentBanner === 0 || BANNERS[currentBanner]?.image.includes('file_00000000a8b08211bd12c4102d0f9d77')) {
                            setIsInviteFriendsOpen(true)
                          }
                        }
                      }}
                      onTouchStart={handleTouchStart}
                      onTouchMove={handleTouchMove}
                      onTouchEnd={handleTouchEnd}
                      onMouseDown={handleMouseDown}
                      onMouseMove={handleMouseMove}
                      onMouseUp={handleMouseUp}
                      onMouseLeave={handleMouseLeave}
                    >
                      <div
                        key={currentBanner}
                        className="w-full h-full"
                        style={{ animation: isSwiping ? 'none' : 'fadeInBanner 400ms ease-out' }}
                      >
                        <img
                          src={BANNERS[currentBanner].image}
                          alt="Banner"
                          className="w-full h-full object-cover rounded-md pointer-events-none"
                          draggable="false"
                        />
                      </div>
                      <div
                        ref={bannerDotsRef}
                        className="absolute left-0 right-0 bottom-2 flex justify-center items-center gap-1.5 z-[60] pointer-events-none"
                        style={{ minHeight: '7px' }}
                      >
                        {BANNERS.map((_, index) => (
                          <div
                            key={index}
                            className={`rounded-full transition-all duration-200 ${
                              index === currentBanner
                                ? 'w-3 h-1.5 bg-white'
                                : 'w-1.5 h-1.5 bg-white/70'
                            }`}
                          />
                        ))}
                      </div>
                    </div>
                  </div>
                </>
              )}
            </div>

            {activeTab === 'mine' ? (
              <MinePage
                t={t}
                isRoomCreated={isRoomCreated}
                myRoom={myRoom}
                userPhoto={userPhoto}
                userName={userName}
                onCardClick={handleCardClick}
                activeMineTab={activeMineTab}
                setActiveMineTab={setActiveMineTab}
                followingRooms={followingRooms}
                recentRooms={recentRooms}
                onUserCardClick={handleUserCardClick}
              />
            ) : renderPopularTab()}
          </div>
        )}

        {currentPage === 'message' && (
          <MessagePage onChatOpen={setIsChatOpen} onJoinRoom={handleJoinRoomFromChat} onNavigate={(page) => setCurrentPage(page)} />
        )}

        {currentPage === 'me' && (
          <MePage
            onLogout={onLogout}
            onPublicProfileChange={(active: boolean) => setIsPublicProfileActive(active)}
            onNavigate={(page) => setCurrentPage(page)}
          />
        )}

        {currentPage === 'room' && selectedUser && (
          <RoomPage
            roomOwner={selectedUser}
            currentUser={{
              id: userUID,
              uid: userUID,
              accountId: (() => {
                const rawAccNum = localStorage.getItem('accountNumber') || getOrCreateAccountNumber(userUID)
                return typeof rawAccNum === 'string' ? rawAccNum : (rawAccNum as any).fullAccNum
              })(),
              name: userName,
              image: userPhoto
            }}
            onBack={handleBackFromRoom}
            onKeepRoom={handleKeepRoom}
            onFollowToggle={(roomId: string, follow: boolean) => {
              if (follow) {
                const room = selectedUser
                if (room) handleFollowRoom({ name: room.name, image: room.image, accountId: room.accountId || room.id })
              } else {
                handleUnfollowRoom(roomId)
              }
            }}
          />
        )}

        {currentPage === 'public_profile' && (
          <PublicProfile
            onBack={handleBackFromPublicProfile}
            onJoinRoom={handleJoinRoomFromChat}
            isOtherUser={true}
            targetUser={selectedUser ? {
              id: selectedUser.id,
              uid: selectedUser.id,
              accountId: selectedUser.accountId,
              displayAccountNumber: selectedUser.accountId,
              name: selectedUser.name,
              country: selectedUser.country,
              photo: selectedUser.image,
              image: selectedUser.image
            } : null}
          />
        )}

        {currentPage === 'leaderboard' && (
          <Leaderboard initialTab={leaderboardTab} onBack={() => setCurrentPage('home')} />
        )}
      </div>

      {!isChatOpen && currentPage !== 'room' && !isPublicProfileActive && !isSearchOpen && currentPage !== 'leaderboard' && (
        <div
          className="fixed bottom-0 left-0 right-0 flex justify-center z-30 bg-white"
          style={{ paddingBottom: 'env(safe-area-inset-bottom, 0px)' }}
        >
          <div className="flex justify-around items-center bg-white border-t border-zinc-100 shadow-lg px-3 py-1.5 w-full h-[65px]">
            <button
              onClick={() => setCurrentPage('home')}
              className="group flex flex-col items-center gap-1 w-16 transition-all duration-200 active:scale-95 origin-center"
            >
              <svg
                key={currentPage === 'home' ? 'home-active' : 'home-inactive'}
                className={currentPage === 'home' ? 'icon-active-anim' : ''}
                width="30" height="30" viewBox="0 0 36 36" fill="none"
              >
                <path
                  d="M18 2.8C20.2 2.8 30.2 8.2 30.2 12.6V23.2C30.2 27.8 28 31 18 31C8 31 5.8 27.8 5.8 23.2V12.6C5.8 8.2 15.8 2.8 18 2.8Z"
                  fill={currentPage === 'home' ? '#3b82f6' : 'white'}
                  stroke="#1D1D1F"
                  strokeWidth="2.4"
                  strokeLinejoin="round"
                />
                <path d="M12.2 14.2C13.3 12.6 14.9 12.1 16.8 13.4" stroke="#1D1D1F" strokeWidth="1.8" strokeLinecap="round" />
                <path d="M11.2 20.8C12.5 24.2 21 25.6 24.3 20.2" stroke="#1D1D1F" strokeWidth="1.8" strokeLinecap="round" />
              </svg>
              <span className={`text-[12px] ${currentPage === 'home' ? 'font-semibold text-black' : 'text-gray-500'}`}>
                {t.home}
              </span>
            </button>

            <button
              onClick={() => setCurrentPage('message')}
              className="group flex flex-col items-center gap-1 w-16 transition-all duration-200 active:scale-95 origin-center"
            >
              <div className="relative">
                <svg
                  key={currentPage === 'message' ? 'message-active' : 'message-inactive'}
                  className={currentPage === 'message' ? 'icon-active-anim' : ''}
                  width="30" height="30" viewBox="0 0 36 36" fill="none"
                >
                  <path
                    d="M6 10.5C6 7 8.3 5 12.2 5H23.8C27.7 5 30 7 30 10.5V16.5C30 20 27.7 22 23.8 22H21 L17.5 27.2C17 28 15.8 28 15.2 27.2L12.2 22C8.3 22 6 20 6 16.5V10.5Z"
                    fill={currentPage === 'message' ? '#3b82f6' : 'white'}
                    stroke="#1D1D1F"
                    strokeWidth="2.4"
                  />
                  <path
                    d="M12 14.5C13.5 12.5 15.5 14.5 19.5 12.5C21.5 14.5 24 14.5 24 14.5"
                    stroke="#1D1D1F"
                    strokeWidth="1.8"
                    strokeLinecap="round"
                  />
                </svg>
                {totalUnreadCount > 0 && (
                  <div className="absolute -top-1 -right-1 bg-red-500 text-white text-[10px] font-bold rounded-full min-w-[24px] h-[24px] flex items-center justify-center px-1 shadow-sm">
                    {totalUnreadCount > 99 ? '99+' : totalUnreadCount}
                  </div>
                )}
              </div>
              <span className={`text-[12px] ${currentPage === 'message' ? 'font-semibold text-black' : 'text-gray-500'}`}>
                {t.message}
              </span>
            </button>

            <button
              onClick={() => setCurrentPage('me')}
              className="group flex flex-col items-center gap-1 w-16 transition-all duration-200 active:scale-95 origin-center"
            >
              <svg
                key={currentPage === 'me' ? 'me-active' : 'me-inactive'}
                className={currentPage === 'me' ? 'icon-active-anim' : ''}
                width="30" height="30" viewBox="0 0 36 36" fill="none"
              >
                <path
                  d="M18 4.5C23.5 4.5 28 8.5 27.2 13.8L26.2 19.8C26 21.2 27.2 22.5 28.6 23.1C30.6 24 31 26.2 29 27.5C27.5 28.5 25 28.8 22 28.8H14C11 28.8 8.5 28.5 7 27.5C5 26.2 5.4 24 7.4 23.1C8.8 22.5 10 21.2 9.8 19.8L8.8 13.8C8 8.5 12.5 4.5 18 4.5Z"
                  fill={currentPage === 'me' ? '#3b82f6' : 'white'}
                  stroke="#1D1D1F"
                  strokeWidth="2.4"
                />
                <circle cx="14" cy="15" r="1.6" fill="#1D1D1F" />
                <circle cx="22" cy="15" r="1.6" fill="#1D1D1F" />
              </svg>
              <span className={`text-[12px] ${currentPage === 'me' ? 'font-semibold text-black' : 'text-gray-500'}`}>
                {t.me}
              </span>
            </button>
          </div>
        </div>
      )}
    </div>
  )
      }