'use client';
import { apiUrl } from "../src/lib/api";
import React, { useState, useEffect, useRef, useCallback, memo } from 'react';
import EmojiPicker from './Emojipicker';
import CupIcon from './cupicon';
import GiftPicker from './GiftPicker';
import RoomSettingPage, { RoomSettingsData } from './RoomSettingPage';
import MessagePage from './MessagePage';
import RoomProfile from './RoomProfile';
import Fourgride from './Fourgride';
import Wildparty from './Wildparty';
import Fruitparty from './Fruitparty';
import WhiteColorRemovalShader from './WhiteColorRemovalShader';
import Roomtask from './Roomtask';
import StorePage from './StorePage';
import EntryEffect from './EntryEffect';
import { LuckyGiftAnimation } from './GiftPicker';
import { generateStableId } from '../lib/hash';
import socket from "../src/lib/socket";
import { addDiamondsToDB, recordTransaction } from "./Wallet";
import dynamic from "next/dynamic";

const JitsiMeeting = dynamic(
  () => import("@jitsi/react-sdk").then((mod) => mod.JitsiMeeting),
  { ssr: false, loading: () => null }
);

async function updateSharedWalletBalance(delta: number): Promise<void> {
  if (!Number.isFinite(delta) || typeof indexedDB === "undefined") return;
  await new Promise<void>((resolve) => {
    const req = indexedDB.open("FruitPartyDB", 3);
    req.onerror = () => resolve();
    req.onsuccess = () => {
      const db = req.result;
      const tx = db.transaction("GameState", "readwrite");
      const store = tx.objectStore("GameState");
      const getReq = store.get("user_data");
      getReq.onsuccess = () => {
        const data = getReq.result || {};
        store.put({ ...data, balance: Math.max(0, Number(data.balance || 0) + delta) }, "user_data");
      };
      tx.oncomplete = () => { db.close(); resolve(); };
      tx.onerror = () => { db.close(); resolve(); };
    };
  });
}

async function addCoinsToDB(amount: number): Promise<void> {
  return updateSharedWalletBalance(Math.max(0, Number(amount) || 0));
}

interface RoomPageProps {
  roomOwner: { id?: string; uid?: string; accountId?: string; name: string; image: string; country?: string; };
  currentUser: { id?: string; uid?: string; accountId: string; name: string; image: string; };
  onClose?: () => void;
  onBack?: () => void;
  onKeepRoom?: (roomData: { name: string; image: string; accountId: string }) => void;
  onFollowToggle?: (roomId: string, follow: boolean) => void;
}

interface Seat {
  number: number; isOccupied: boolean; isLocked?: boolean;
  user?: { name: string; image: string; accountId: string };
  isMuted?: boolean; isSpeaking?: boolean;
  gif?: { src: string; timestamp: number };
}

interface Message {
  id: string; text: string; sender: string; senderImage: string;
  senderAccountId?: string; timestamp: number;
  type?: 'message' | 'join' | 'leave';
  imageUrl?: string; equippedBubble?: string; equippedVehicle?: string;
}

interface RoomUser { accountId: string; name: string; image: string; }
interface MusicTrack { id: string; name: string; url: string; }
interface EntryToastData { id: string; name: string; image: string; }

const THEME_BACKGROUNDS: { [key: string]: string } = {
  'forest-night': '/1784875884052~2.jpg',
  'mood-light': '/1784533036732~2.jpg',
};

const ROOM_SETTINGS_DB_NAME = "HurryRoomSettingsDB";
const ROOM_SETTINGS_STORE = "roomSettings";

interface RoomSettingsCache {
  roomId: string; roomName: string; roomDp: string; announcement: string;
  micMode: number; theme: string; isLocked: boolean; roomPassword: string; updatedAt: number;
}

const openRoomSettingsDB = (): Promise<IDBDatabase> =>
  new Promise((resolve, reject) => {
    const request = indexedDB.open(ROOM_SETTINGS_DB_NAME, 1);
    request.onerror = () => reject(request.error);
    request.onsuccess = () => resolve(request.result);
    request.onupgradeneeded = () => {
      const db = request.result;
      if (!db.objectStoreNames.contains(ROOM_SETTINGS_STORE)) {
        db.createObjectStore(ROOM_SETTINGS_STORE, { keyPath: "roomId" });
      }
    };
  });

const saveRoomSettingsToIndexedDB = async (data: RoomSettingsCache) => {
  const db = await openRoomSettingsDB();
  await new Promise<void>((resolve, reject) => {
    const tx = db.transaction(ROOM_SETTINGS_STORE, "readwrite");
    tx.objectStore(ROOM_SETTINGS_STORE).put(data);
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
  db.close();
};

const loadRoomSettingsFromIndexedDB = async (roomId: string): Promise<RoomSettingsCache | null> => {
  if (!roomId) return null;
  const db = await openRoomSettingsDB();
  const result = await new Promise<RoomSettingsCache | null>((resolve, reject) => {
    const tx = db.transaction(ROOM_SETTINGS_STORE, "readonly");
    const request = tx.objectStore(ROOM_SETTINGS_STORE).get(roomId);
    request.onsuccess = () => resolve(request.result || null);
    request.onerror = () => reject(request.error);
  });
  db.close();
  return result;
};

const ROOM_MESSAGES_DB_NAME = "RoomMessagesDB";
const ROOM_MESSAGES_STORE = "messages";

const openRoomMessagesDB = (): Promise<IDBDatabase> =>
  new Promise((resolve, reject) => {
    const request = indexedDB.open(ROOM_MESSAGES_DB_NAME, 1);
    request.onerror = () => reject(request.error);
    request.onsuccess = () => resolve(request.result);
    request.onupgradeneeded = () => {
      const db = request.result;
      if (!db.objectStoreNames.contains(ROOM_MESSAGES_STORE)) {
        const store = db.createObjectStore(ROOM_MESSAGES_STORE, { keyPath: "id" });
        store.createIndex("roomId", "roomId", { unique: false });
        store.createIndex("timestamp", "timestamp", { unique: false });
      }
    };
  });

const HouseIcon = memo(function HouseIcon({
  size = 22, className = "", style = {},
}: { size?: number; className?: string; style?: React.CSSProperties; }) {
  const idRef = useRef<string>("");
  if (!idRef.current) idRef.current = `hm-${Math.random().toString(36).slice(2, 10)}`;
  const maskId = idRef.current;
  return (
    <svg width={size} height={size} viewBox="0 0 1024 1024" xmlns="http://www.w3.org/2000/svg" className={className} style={style} aria-hidden="true">
      <defs>
        <mask id={maskId}>
          <rect width="1024" height="1024" fill="white" />
          <rect x="465" y="565" width="94" height="135" rx="10" fill="black" />
        </mask>
      </defs>
      <circle cx="512" cy="512" r="330" fill="#008CFF" />
      <path
        d="M318 505 L492 348 Q512 330 532 348 L706 505 Q716 514 706 524 L681 524 L681 690 Q681 705 666 705 L358 705 Q343 705 343 690 L343 524 L318 524 Q308 514 318 505 Z"
        fill="#FFFFFF"
        mask={`url(#${maskId})`}
      />
    </svg>
  );
});

const SeatBubbleIconBlue = memo(function SeatBubbleIconBlue() {
  return (
    <svg viewBox="0 0 512 512" xmlns="http://www.w3.org/2000/svg" style={{ width: "100%", height: "100%", display: "block" }} preserveAspectRatio="xMidYMid meet">
      <defs>
        <linearGradient id="seatBubbleBlue" x1="100" y1="55" x2="420" y2="460" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor="#D9F7FF" />
          <stop offset="18%" stopColor="#91E1FF" />
          <stop offset="42%" stopColor="#4DBFEF" />
          <stop offset="70%" stopColor="#299DD6" />
          <stop offset="100%" stopColor="#167DB9" />
        </linearGradient>
        <linearGradient id="seatShineBlue" x1="0" y1="0" x2="1" y2="0">
          <stop offset="0%" stopColor="#FFFFFF" stopOpacity="0" />
          <stop offset="30%" stopColor="#FFFFFF" stopOpacity="0.75" />
          <stop offset="55%" stopColor="#FFFFFF" stopOpacity="0.95" />
          <stop offset="100%" stopColor="#FFFFFF" stopOpacity="0" />
        </linearGradient>
        <filter id="seatSoftGlowBlue" x="-30%" y="-30%" width="160%" height="160%">
          <feGaussianBlur stdDeviation="5" />
        </filter>
      </defs>
      <circle cx="256" cy="256" r="225" fill="none" stroke="#55C6F3" strokeWidth="15" opacity="0.25" filter="url(#seatSoftGlowBlue)" />
      <circle cx="256" cy="256" r="225" fill="none" stroke="url(#seatBubbleBlue)" strokeWidth="10" strokeLinecap="round" />
      <path d="M 98 134 C 113 108 132 92 153 79 C 174 67 196 59 218 56" fill="none" stroke="url(#seatShineBlue)" strokeWidth="7.5" strokeLinecap="round" />
      <path d="M 207 452 C 258 466 316 456 361 428 C 378 417 392 404 404 388" fill="none" stroke="url(#seatShineBlue)" strokeWidth="7" strokeLinecap="round" />
    </svg>
  );
});

const SeatBubbleIconGrey = memo(function SeatBubbleIconGrey() {
  return (
    <svg viewBox="0 0 512 512" xmlns="http://www.w3.org/2000/svg" style={{ width: "100%", height: "100%", display: "block" }} preserveAspectRatio="xMidYMid meet">
      <defs>
        <linearGradient id="seatBubbleGrey" x1="100" y1="55" x2="420" y2="460" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor="#F3F4F6" />
          <stop offset="18%" stopColor="#D1D5DB" />
          <stop offset="42%" stopColor="#9CA3AF" />
          <stop offset="70%" stopColor="#6B7280" />
          <stop offset="100%" stopColor="#4B5563" />
        </linearGradient>
        <linearGradient id="seatShineGrey" x1="0" y1="0" x2="1" y2="0">
          <stop offset="0%" stopColor="#FFFFFF" stopOpacity="0" />
          <stop offset="30%" stopColor="#FFFFFF" stopOpacity="0.75" />
          <stop offset="55%" stopColor="#FFFFFF" stopOpacity="0.95" />
          <stop offset="100%" stopColor="#FFFFFF" stopOpacity="0" />
        </linearGradient>
        <filter id="seatSoftGlowGrey" x="-30%" y="-30%" width="160%" height="160%">
          <feGaussianBlur stdDeviation="5" />
        </filter>
      </defs>
      <circle cx="256" cy="256" r="225" fill="none" stroke="#9CA3AF" strokeWidth="15" opacity="0.25" filter="url(#seatSoftGlowGrey)" />
      <circle cx="256" cy="256" r="225" fill="none" stroke="url(#seatBubbleGrey)" strokeWidth="10" strokeLinecap="round" />
      <path d="M 98 134 C 113 108 132 92 153 79 C 174 67 196 59 218 56" fill="none" stroke="url(#seatShineGrey)" strokeWidth="7.5" strokeLinecap="round" />
      <path d="M 207 452 C 258 466 316 456 361 428 C 378 417 392 404 404 388" fill="none" stroke="url(#seatShineGrey)" strokeWidth="7" strokeLinecap="round" />
    </svg>
  );
});

const MarqueeText = memo(function MarqueeText({
  text, className = "", style = {},
}: { text: string; className?: string; style?: React.CSSProperties }) {
  const containerRef = useRef<HTMLDivElement>(null);
  const innerRef = useRef<HTMLDivElement>(null);
  const [shouldScroll, setShouldScroll] = useState(false);

  useEffect(() => {
    const check = () => {
      if (containerRef.current && innerRef.current) {
        setShouldScroll(innerRef.current.scrollWidth > containerRef.current.clientWidth + 1);
      }
    };
    check();
    const t = setTimeout(check, 50);
    window.addEventListener("resize", check);
    return () => { clearTimeout(t); window.removeEventListener("resize", check); };
  }, [text]);

  const fadeStyle: React.CSSProperties = shouldScroll
    ? {
        WebkitMaskImage: "linear-gradient(to right, transparent 0%, #000 10%, #000 80%, transparent 100%)",
        maskImage: "linear-gradient(to right, transparent 0%, #000 10%, #000 80%, transparent 100%)",
      }
    : {};

  return (
    <div
      ref={containerRef}
      className={`overflow-hidden ${className}`}
      style={{ ...style, ...fadeStyle }}
    >
      <div
        ref={innerRef}
        className={`inline-flex whitespace-nowrap will-change-transform ${shouldScroll ? "marquee-inner" : ""}`}
      >
        <span className={shouldScroll ? "pr-8" : ""}>{text}</span>
        {shouldScroll && <span className="pr-8" aria-hidden="true">{text}</span>}
      </div>
    </div>
  );
});

const EntryToastStack = memo(function EntryToastStack({ toasts }: { toasts: EntryToastData[] }) {
  if (toasts.length === 0) return null;
  return (
    <div
      className="fixed z-[9998] pointer-events-none flex flex-col items-start pl-2"
      style={{ left: 0, bottom: '33vh' }}
    >
      {toasts.map((t) => (
        <div
          key={t.id}
          className="entry-toast mb-1 flex items-center gap-2 shadow-lg"
          style={{
            background: 'rgba(0,0,0,0.88)',
            borderRadius: '999px 0 0 999px',
            padding: '4px 48px 4px 6px',
            minWidth: '230px',
          }}
        >
          <img
            src={t.image || '/default-avatar.png'}
            alt={t.name}
            className="rounded-full object-cover shrink-0"
            style={{ width: '24px', height: '24px' }}
            loading="eager"
            decoding="async"
            onError={(e) => { (e.target as HTMLImageElement).src = '/default-avatar.png'; }}
          />
          <span className="text-white font-semibold truncate" style={{ fontSize: '12px', maxWidth: '95px' }}>
            {t.name}
          </span>
          <span className="text-white/95 whitespace-nowrap" style={{ fontSize: '11px' }}>
            Enter the Room
          </span>
        </div>
      ))}
    </div>
  );
});
export default function RoomPage({ roomOwner, currentUser, onClose, onBack, onKeepRoom, onFollowToggle }: RoomPageProps) {
  const [jitsiApi, setJitsiApi] = useState<any>(null);
  const roomId = roomOwner.id || roomOwner.accountId || 'default-room';
  const userAccountId = currentUser.accountId || currentUser.uid || currentUser.id || "guest";

  useEffect(() => {
    const creditedTransferIds = new Map<string, number>();
    const handleCoinTransferReceived = async (data: any = {}) => {
      const recipients = Array.isArray(data.recipientIds)
        ? data.recipientIds.map((id: any) => String(id)).filter(Boolean)
        : data.recipientId ? [String(data.recipientId)] : [];
      const currentIds = new Set(
        [currentUser.accountId, currentUser.uid, currentUser.id, localStorage.getItem("accountNumber")]
          .filter(Boolean).map(String)
      );
      const rawDiamondAmount = Number(data.diamondAmount);
      const rawCoinAmount = Number(data.amount);
      const isLucky = data.luckyGift === true || String(data.giftType || "") === "Lucky";
      const diamondAmount =
        Number.isFinite(rawDiamondAmount) && rawDiamondAmount > 0 ? Math.floor(rawDiamondAmount)
        : Number.isFinite(rawCoinAmount) && rawCoinAmount > 0 ? Math.floor(rawCoinAmount) : 0;
      const transferKey = String(
        data.transferId || data.eventId ||
        `${data.roomId || ""}:${data.senderId || ""}:${data.timestamp || ""}:${data.giftName || ""}:${diamondAmount}`
      );
      const now = Date.now();
      for (const [k, ts] of creditedTransferIds) {
        if (now - ts > 2 * 60 * 1000) creditedTransferIds.delete(k);
      }
      const isSender = currentIds.has(String(data.senderId || ""));
      const luckyReturnAmount = Math.max(0, Number(data.luckyReturnAmount) || 0);
      if (isLucky && isSender && luckyReturnAmount > 0 && !creditedTransferIds.has(`return:${transferKey}`)) {
        creditedTransferIds.set(`return:${transferKey}`, now);
        await addCoinsToDB(luckyReturnAmount);
        await recordTransaction("Lucky Gift return", luckyReturnAmount, "coin");
        window.dispatchEvent(new CustomEvent("hurry:coins-updated", { detail: { amount: luckyReturnAmount } }));
      }
      if (!recipients.some((id) => currentIds.has(id))) return;
      if (diamondAmount <= 0 || creditedTransferIds.has(transferKey)) return;
      creditedTransferIds.set(transferKey, now);
      await addDiamondsToDB(diamondAmount);
      const giftLabel = data.giftName ? " — " + String(data.giftName) : "";
      await recordTransaction("Diamonds received" + giftLabel, diamondAmount, "diamond");
      window.dispatchEvent(new CustomEvent("hurry:diamonds-updated", { detail: { amount: diamondAmount } }));
    };
    socket.on("coin_transfer_received", handleCoinTransferReceived);
    return () => {
      socket.off("coin_transfer_received", handleCoinTransferReceived);
      creditedTransferIds.clear();
    };
  }, [userAccountId]);

  return (
    <RoomVoiceJitsi roomId={roomId} userAccountId={userAccountId} userName={currentUser.name} userEmail={undefined} onApiReady={setJitsiApi}>
      <RoomContent
        roomOwner={roomOwner}
        currentUser={currentUser}
        onClose={onClose}
        onBack={onBack}
        onKeepRoom={onKeepRoom}
        onFollowToggle={onFollowToggle}
        jitsiApi={jitsiApi}
      />
    </RoomVoiceJitsi>
  );
}

function RoomVoiceJitsi({
  roomId, userAccountId, userName, userEmail, children, onApiReady,
}: {
  roomId: string; userAccountId: string; userName: string; userEmail?: string;
  children: React.ReactNode; onApiReady: (api: any) => void;
}) {
  return (
    <>
      <div className="fixed" style={{ width: "1px", height: "1px", left: "-10px", top: "-10px", opacity: 0, pointerEvents: "none", overflow: "hidden" }}>
        <JitsiMeeting
          domain="meet.jit.si"
          roomName={`HurryVoice-${roomId}`}
          configOverwrite={{ startAudioOnly: true, startWithAudioMuted: true, startWithVideoMuted: true, prejoinConfig: { enabled: false } }}
          interfaceConfigOverwrite={{ TOOLBAR_BUTTONS: [] }}
          userInfo={{ displayName: userName, email: userEmail || "" }}
          getIFrameRef={(iframeRef) => { iframeRef.style.width = "1px"; iframeRef.style.height = "1px"; }}
          onApiReady={(externalApi) => { onApiReady(externalApi); }}
        />
      </div>
      {children}
    </>
  );
}

function RoomContent({
  roomOwner, currentUser, onClose, onBack, onKeepRoom, onFollowToggle, jitsiApi,
}: RoomPageProps & { jitsiApi?: any }) {
  const isKeepingRef = useRef(false);
  const joinMessageSentRef = useRef(false);
  const clearedAtRef = useRef<number | null>(null);
  const desiredAudioStateRef = useRef<boolean | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const messagesContainerRef = useRef<HTMLDivElement>(null);
  const inputContainerRef = useRef<HTMLDivElement>(null);
  const musicAudioRef = useRef<HTMLAudioElement | null>(null);

  const [showExitMenu, setShowExitMenu] = useState(false);
  const [showEmojiPicker, setShowEmojiPicker] = useState(false);
  const [showGiftPicker, setShowGiftPicker] = useState(false);
  const [isSpeakerOn, setIsSpeakerOn] = useState(true);
  const [showMessageSheet, setShowMessageSheet] = useState(false);
  const [showSettingPage, setShowSettingPage] = useState(false);
  const [showRoomInfo, setShowRoomInfo] = useState(false);
  const [showActiveUsers, setShowActiveUsers] = useState(false);
  const [showFourGride, setShowFourGride] = useState(false);
  const [isFollowed, setIsFollowed] = useState(false);
  const [copied, setCopied] = useState(false);
  const [localUser, setLocalUser] = useState<{ name: string; image: string; accountId: string }>({ name: 'User', image: '/default-avatar.png', accountId: '' });

  const [showGameSheet, setShowGameSheet] = useState(false);
  const [showWildParty, setShowWildParty] = useState<boolean | 'minimized'>(false);
  const [showFruitParty, setShowFruitParty] = useState<boolean | 'minimized'>(false);
  const [showRoomTask, setShowRoomTask] = useState(false);
  const [showStore, setShowStore] = useState(false);
  const [storeInitialView, setStoreInitialView] = useState<"store" | "bag">("store");
  const [showCupIcon, setShowCupIcon] = useState(false);
  const [cupCount, setCupCount] = useState(0);

  const [musicControllerState, setMusicControllerState] = useState<'hidden' | 'full' | 'minimized'>('hidden');
  const [currentTrack, setCurrentTrack] = useState<MusicTrack | null>(null);
  const [isMusicPlaying, setIsMusicPlaying] = useState(false);
  const [musicVolume, setMusicVolume] = useState(1);
  const [musicPlaylist, setMusicPlaylist] = useState<MusicTrack[]>([]);
  const [currentTrackIndex, setCurrentTrackIndex] = useState(0);
  const [musicCurrentTime, setMusicCurrentTime] = useState(0);
  const [musicDuration, setMusicDuration] = useState(0);

  const [publicMsgOff, setPublicMsgOff] = useState(false);
  const [showPublicMsgModal, setShowPublicMsgModal] = useState(false);
  const [roomAdmins, setRoomAdmins] = useState<string[]>([]);

  const [showUserProfile, setShowUserProfile] = useState(false);
  const [profileUser, setProfileUser] = useState<{ name: string; image: string; accountId: string; isInSeat?: boolean; } | null>(null);

  const userAccountId = currentUser.accountId || currentUser.uid || currentUser.id || "guest";
  const roomOwnerId = roomOwner.accountId || roomOwner.uid || roomOwner.id || "";
  const isRoomOwner = userAccountId === roomOwnerId;
  const isRoomAdmin = roomAdmins.some(id => String(id) === String(userAccountId));
  const canSendWhenPublicMsgOff = isRoomOwner || isRoomAdmin;

  const [message, setMessage] = useState("");
  const [messages, setMessages] = useState<Message[]>([]);
  const [fullImageModal, setFullImageModal] = useState<string | null>(null);

  const [roomName, setRoomName] = useState<string>(roomOwner.name || "Room");
  const [roomAnnouncement, setRoomAnnouncement] = useState<string>("");
  const [isLocked, setIsLocked] = useState<boolean>(false);
  const [roomPassword, setRoomPassword] = useState<string>("");
  const [roomDp, setRoomDp] = useState<string>(roomOwner.image || "/default-avatar.png");
  const [roomCountry, setRoomCountry] = useState<string>(roomOwner.country || "");
  const [micMode, setMicMode] = useState<number>(15);
  const [roomInfoTab, setRoomInfoTab] = useState<'profile' | 'members'>('profile');
  const [backgroundImage, setBackgroundImage] = useState<string>("/1784533036732~2.jpg");

  const [showChatInput, setShowChatInput] = useState(false);
  const [roomUsers, setRoomUsers] = useState<RoomUser[]>([]);
  const [seats, setSeats] = useState<Seat[]>([]);
  const [selectedSeat, setSelectedSeat] = useState<number | null>(null);
  const [showSeatSheet, setShowSeatSheet] = useState(false);
  const [entryToasts, setEntryToasts] = useState<EntryToastData[]>([]);

  const roomId = roomOwner.id || roomOwner.accountId || 'default-room';
  const hasSeat = seats.some(s => s.isOccupied && s.user?.accountId === userAccountId);
  const currentUserSeat = seats.find(s => s.isOccupied && s.user?.accountId === userAccountId);

  const formatCupCount = (n: number): string => {
    if (n >= 1000000) return (n / 1000000).toFixed(1).replace('.0', '') + 'M';
    if (n >= 1000) return (n / 1000).toFixed(1).replace('.0', '') + 'K';
    return String(n);
  };

  useEffect(() => {
    const name = localStorage.getItem('userName') || 'User';
    const image = localStorage.getItem('userPhoto') || '/default-avatar.png';
    const storedAccNum = localStorage.getItem('accountNumber') || '';
    setLocalUser({ name, image, accountId: storedAccNum });
    if (!roomCountry) {
      const flag = localStorage.getItem('userCountry') || '';
      if (flag) setRoomCountry(flag);
    }
  }, []);

  useEffect(() => { localStorage.setItem('cupCount', String(cupCount)); }, [cupCount]);
  const getInitialSeats = useCallback((mode: number): Seat[] => {
  const s: Seat[] = [];
  for (let i = 1; i <= mode; i++) s.push({ number: i, isOccupied: false, isLocked: false, isMuted: false, isSpeaking: false });
  return s;
}, []);

useEffect(() => {
  setMessages([]);
  setEntryToasts([]);
  joinMessageSentRef.current = false;
  clearedAtRef.current = null;
}, [roomId]);

useEffect(() => {
  setSeats(prev => {
    const newSeats = getInitialSeats(micMode);
    return newSeats.map(newSeat => {
      const oldSeat = prev.find(s => s.number === newSeat.number);
      if (oldSeat && oldSeat.isOccupied) {
        return { ...newSeat, isOccupied: oldSeat.isOccupied, user: oldSeat.user, isMuted: oldSeat.isMuted, isSpeaking: oldSeat.isSpeaking, isLocked: oldSeat.isLocked, gif: oldSeat.gif };
      }
      return newSeat;
    });
  });
}, [micMode, getInitialSeats]);

useEffect(() => {
  const handleHardwareBack = (e: Event) => {
    e.preventDefault();
    if (showSettingPage) setShowSettingPage(false);
    else if (showSeatSheet) { setShowSeatSheet(false); setSelectedSeat(null); }
    else if (showEmojiPicker) setShowEmojiPicker(false);
    else if (showGiftPicker) setShowGiftPicker(false);
    else if (showFourGride) setShowFourGride(false);
    else if (showActiveUsers) setShowActiveUsers(false);
    else if (showRoomInfo) setShowRoomInfo(false);
    else if (showGameSheet) setShowGameSheet(false);
    else if (showStore) setShowStore(false);
    else if (showRoomTask) setShowRoomTask(false);
    else if (showUserProfile) setShowUserProfile(false);
    else if (showMessageSheet) setShowMessageSheet(false);
    else setShowExitMenu(true);
  };
  window.addEventListener('hardwareBackPress', handleHardwareBack);
  return () => window.removeEventListener('hardwareBackPress', handleHardwareBack);
}, [showSettingPage, showSeatSheet, showEmojiPicker, showGiftPicker, showFourGride, showActiveUsers, showRoomInfo, showGameSheet, showStore, showRoomTask, showUserProfile, showMessageSheet]);

useEffect(() => {
  if (!jitsiApi) return;
  const isMuted = currentUserSeat?.isMuted ?? true;
  const desiredState = hasSeat && !isMuted;
  if (desiredAudioStateRef.current !== desiredState || desiredAudioStateRef.current === null) {
    desiredAudioStateRef.current = desiredState;
    try { jitsiApi.executeCommand("setAudioMute", !desiredState); } catch (e) { console.error(e); }
  }
}, [currentUserSeat?.isMuted, hasSeat, jitsiApi]);

useEffect(() => {
  let mounted = true;
  const loadRoomSettings = async () => {
    if (!roomId) return;
    try {
      const cached = await loadRoomSettingsFromIndexedDB(String(roomId));
      if (!mounted) return;
      if (cached) {
        if (cached.roomName && cached.roomName !== "Room") setRoomName(cached.roomName);
        if (cached.roomDp && cached.roomDp !== "/default-avatar.png") setRoomDp(cached.roomDp);
        if (cached.announcement) setRoomAnnouncement(cached.announcement);
        if (cached.micMode) setMicMode(Number(cached.micMode));
        if (cached.theme && THEME_BACKGROUNDS[cached.theme]) setBackgroundImage(THEME_BACKGROUNDS[cached.theme]);
        if (cached.isLocked !== undefined) setIsLocked(Boolean(cached.isLocked));
        if (cached.roomPassword) setRoomPassword(cached.roomPassword);
      } else {
        setRoomName(roomOwner.name || "Room");
        setRoomDp(roomOwner.image || "/default-avatar.png");
      }
    } catch (err) { console.error("Room settings IndexedDB load error:", err); }

    try {
      const response = await fetch(apiUrl(`/api/rooms?roomId=${encodeURIComponent(String(roomOwner.accountId || roomOwner.id || roomId))}`));
      if (response.ok) {
        const result = await response.json();
        const dbRoom = result?.room;
        if (dbRoom && mounted) {
          const realName = dbRoom['Room Name'] || dbRoom.roomName || dbRoom.name;
          const realDp = dbRoom.dp || dbRoom['Room dp'] || dbRoom.roomDp || dbRoom.image;
          if (realName && realName !== "My Room" && realName !== "My room" && realName !== "User") setRoomName(realName);
          if (realDp && realDp !== 'undefined' && realDp !== 'null' && realDp !== "/default-avatar.png") setRoomDp(realDp);
          if (dbRoom.announcement) setRoomAnnouncement(dbRoom.announcement);
          if (dbRoom.micMode) setMicMode(Number(dbRoom.micMode));
          if (dbRoom.theme && THEME_BACKGROUNDS[dbRoom.theme]) setBackgroundImage(THEME_BACKGROUNDS[dbRoom.theme]);
          if (dbRoom.isLocked !== undefined) setIsLocked(Boolean(dbRoom.isLocked));
          if (dbRoom.roomPassword) setRoomPassword(dbRoom.roomPassword);
          if (dbRoom.country) setRoomCountry(String(dbRoom.country));
          await saveRoomSettingsToIndexedDB({
            roomId: String(roomId), roomName: realName || roomName, roomDp: realDp || roomDp,
            announcement: dbRoom.announcement || "", micMode: Number(dbRoom.micMode || 15),
            theme: dbRoom.theme || "mood-light", isLocked: Boolean(dbRoom.isLocked),
            roomPassword: dbRoom.roomPassword || "", updatedAt: Date.now()
          });
        }
      }
    } catch (e) { console.warn("Failed to fetch fresh room settings from MongoDB", e); }
  };
  loadRoomSettings();
  return () => { mounted = false; };
}, [roomId, roomOwner.name, roomOwner.image]);

useEffect(() => {
  let cancelled = false;
  (async () => {
    try {
      const response = await fetch(apiUrl(`/api/rooms?roomId=${encodeURIComponent(String(roomId))}`), { cache: "no-store" });
      if (!response.ok) return;
      const data = await response.json();
      const admins = Array.isArray(data?.room?.admin) ? data.room.admin.map(String) : [];
      if (!cancelled) setRoomAdmins(admins);
    } catch {}
  })();
  return () => { cancelled = true; };
}, [roomId]);

// ⭐ MAIN REAL-TIME SOCKET EFFECT
useEffect(() => {
  if (!roomId || userAccountId === "guest") return;

  const currentRoomUser: RoomUser = {
    accountId: String(userAccountId),
    name: currentUser.name || "User",
    image: currentUser.image || "/default-avatar.png",
  };

  const handleRoomPresence = (data: any) => {
    if (!data || String(data.roomId) !== String(roomId) || !Array.isArray(data.users)) return;
    const users: RoomUser[] = data.users
      .map((user: any) => ({
        accountId: String(user?.accountId || user?.userId || user?.id || ""),
        name: user?.name || "User",
        image: user?.image || user?.dp || "/default-avatar.png",
      }))
      .filter((user: RoomUser) => Boolean(user.accountId));
    setRoomUsers(users);
  };

  const handleRoomUserOnline = (data: any) => {
    if (!data || String(data.roomId) !== String(roomId) || !data.userId) return;
    const userId = String(data.userId);
    setRoomUsers(prev => {
      const existing = prev.find(u => String(u.accountId) === userId);
      if (existing) return prev.map(u => String(u.accountId) === userId ? { ...u, name: data.user?.name || u.name, image: data.user?.image || u.image } : u);
      return [...prev, { accountId: userId, name: data.user?.name || "User", image: data.user?.image || "/default-avatar.png" }];
    });
  };

  const handleRoomUserOffline = (data: any) => {
    if (!data || String(data.roomId) !== String(roomId) || !data.userId) return;
    setRoomUsers(prev => prev.filter(u => String(u.accountId) !== String(data.userId)));
  };

  const handleRoomMessage = async (data: any) => {
    if (!data || String(data.roomId) !== String(roomId)) return;
    const incoming: Message = {
      id: String(data.id || `${data.senderId || "user"}-${data.createdAt || Date.now()}`),
      text: data.text || "",
      sender: data.senderName || "Unknown",
      senderImage: data.senderAvatar || "/default-avatar.png",
      senderAccountId: data.senderAccountId || data.senderId || "",
      timestamp: Number(data.createdAt || Date.now()),
      type: data.type || "message",
      imageUrl: data.imageUrl || undefined,
      equippedBubble: data.equippedBubble || undefined,
      equippedVehicle: data.equippedVehicle || undefined,
    };

    if (incoming.type === 'join') {
      const toastId = incoming.id;
      setEntryToasts((prev) => {
        if (prev.some((t) => t.id === toastId)) return prev;
        return [...prev, { id: toastId, name: incoming.sender, image: incoming.senderImage }];
      });
      setTimeout(() => {
        setEntryToasts((prev) => prev.filter((t) => t.id !== toastId));
      }, 4000);
    }

    setMessages(prev => {
      if (prev.some(msg => msg.id === incoming.id)) return prev;
      return [...prev, incoming];
    });
    try {
      const db = await openRoomMessagesDB();
      const transaction = db.transaction([ROOM_MESSAGES_STORE], "readwrite");
      transaction.objectStore(ROOM_MESSAGES_STORE).put({ ...incoming, roomId });
      transaction.oncomplete = () => db.close();
      transaction.onerror = () => db.close();
    } catch (err) { console.error("IndexedDB room message save error:", err); }
  };

  const handleRoomChatCleared = async (data: any) => {
    if (!data || String(data.roomId) !== String(roomId)) return;
    clearedAtRef.current = Number(data.timestamp || Date.now());
    setMessages([]);
    try {
      const db = await openRoomMessagesDB();
      const transaction = db.transaction([ROOM_MESSAGES_STORE], "readwrite");
      const store = transaction.objectStore(ROOM_MESSAGES_STORE);
      const request = store.index("roomId").openCursor(roomId);
      request.onsuccess = () => {
        const cursor = request.result;
        if (cursor) { cursor.delete(); cursor.continue(); } else { db.close(); }
      };
      request.onerror = () => { db.close(); };
    } catch (err) { console.error("Room chat cleared socket error:", err); }
  };

  const handleRoomLocked = (data: any) => {
    if (!data || String(data.roomId) !== String(roomId)) return;
    const ownerId = String(data.ownerId || "");
    if (ownerId && ownerId === String(userAccountId)) return;
    setIsLocked(true);
    try { jitsiApi?.executeCommand("hangup"); } catch {}
    alert("This room is locked. You cannot enter or listen to the room voice.");
    if (onBack) onBack(); else if (onClose) onClose();
  };

  const handleSettingsUpdated = async (data: any) => {
    if (!data || String(data.roomId) !== String(roomId)) return;
    if (data.roomName) setRoomName(data.roomName);
    if (data.roomDp) setRoomDp(data.roomDp);
    if (data.announcement !== undefined) setRoomAnnouncement(data.announcement);
    if (data.micMode !== undefined) setMicMode(Number(data.micMode));
    if (data.theme && THEME_BACKGROUNDS[data.theme]) setBackgroundImage(THEME_BACKGROUNDS[data.theme]);
    if (data.isLocked !== undefined) setIsLocked(Boolean(data.isLocked));
    if (data.roomPassword !== undefined) setRoomPassword(data.roomPassword || "");
    try {
      await saveRoomSettingsToIndexedDB({
        roomId: String(roomId), roomName: data.roomName || roomName, roomDp: data.roomDp || roomDp,
        announcement: data.announcement || "", micMode: Number(data.micMode || 15),
        theme: data.theme || "mood-light", isLocked: Boolean(data.isLocked),
        roomPassword: data.roomPassword || "", updatedAt: Number(data.updatedAt || Date.now()),
      });
    } catch (err) { console.error("Realtime room settings IndexedDB save error:", err); }
  };

  const handleRoomSeats = (data: any) => {
    if (!data || String(data.roomId) !== String(roomId) || !Array.isArray(data.seats)) return;
    setSeats(() => {
      const base = getInitialSeats(micMode);
      const byNumber = new Map(base.map(seat => [seat.number, seat]));
      for (const incoming of data.seats) {
        const number = Number(incoming?.number);
        if (!Number.isFinite(number)) continue;
        const existing = byNumber.get(number);
        byNumber.set(number, { ...(existing || { number, isOccupied: false, isLocked: false, isMuted: false, isSpeaking: false }), ...incoming, number });
      }
      return Array.from(byNumber.values()).sort((a, b) => a.number - b.number);
    });
  };

  const joinRoom = () => {
    socket.emit("room_join", {
      roomId,
      userId: currentUser.uid || currentUser.id || userAccountId,
      accountId: userAccountId,
      roomOwnerId: roomOwner.uid || roomOwner.id || roomOwner.accountId || roomId,
      name: currentRoomUser.name,
      dp: currentRoomUser.image,
      email: (currentUser as any)?.email || (currentUser as any)?.emailPhone || "",
    });
    socket.emit("room_seats_request", { roomId });
  };

  socket.emit("room_presence_request", { roomId });
  socket.on("room_locked", handleRoomLocked);
  socket.on("room_settings_updated", handleSettingsUpdated);
  socket.on("room_seats", handleRoomSeats);
  socket.on("room_presence", handleRoomPresence);
  socket.on("room_user_online", handleRoomUserOnline);
  socket.on("room_user_offline", handleRoomUserOffline);
  socket.on("room_message", handleRoomMessage);
  socket.on("room_chat_cleared", handleRoomChatCleared);

  setRoomUsers(prev => prev.some(u => String(u.accountId) === String(userAccountId)) ? prev : [...prev, currentRoomUser]);

  if (socket.connected) joinRoom();
  else { socket.once("connect", joinRoom); socket.connect(); }

  return () => {
    socket.off("connect", joinRoom);
    socket.off("room_presence", handleRoomPresence);
    socket.off("room_user_online", handleRoomUserOnline);
    socket.off("room_user_offline", handleRoomUserOffline);
    socket.off("room_message", handleRoomMessage);
    socket.off("room_chat_cleared", handleRoomChatCleared);
    socket.off("room_locked", handleRoomLocked);
    socket.off("room_settings_updated", handleSettingsUpdated);
    socket.off("room_seats", handleRoomSeats);
    if (!isKeepingRef.current) socket.emit("room_leave", { roomId, userId: userAccountId });
  };
}, [roomId, userAccountId, currentUser.name, currentUser.image, getInitialSeats]);

const sendMessageToSocket = useCallback(async (
  text: string,
  imageUrl?: string,
  type: "message" | "join" | "leave" = "message"
) => {
  if (!roomId || userAccountId === "guest") return;
  const bubbleExpiry = Number(localStorage.getItem('equipped_Chat Bubble_expiresAt') || 0);
  const bubbleExpired = bubbleExpiry > 0 && bubbleExpiry <= Date.now();
  if (bubbleExpired) {
    localStorage.removeItem('equipped_Chat Bubble');
    localStorage.removeItem('equipped_Chat Bubble_expiresAt');
  }
  const equippedBubble = bubbleExpired ? undefined : (localStorage.getItem('equipped_Chat Bubble') || undefined);
  const equippedVehicle = localStorage.getItem('equipped_Vehicle') || undefined;
  const message = {
    id: `${userAccountId}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
    roomId, senderId: userAccountId, senderAccountId: userAccountId,
    senderName: currentUser.name || "User",
    senderAvatar: currentUser.image || "/default-avatar.png",
    text, type,
    imageUrl: imageUrl || null,
    equippedBubble: type === 'message' ? equippedBubble : undefined,
    equippedVehicle: type === 'join' ? equippedVehicle : undefined,
    createdAt: Date.now(),
  };
  setMessages(prev => {
    if (prev.some(m => m.id === message.id)) return prev;
    return [...prev, {
      id: message.id, text, sender: message.senderName,
      senderImage: message.senderAvatar, senderAccountId: message.senderAccountId,
      timestamp: message.createdAt, type, imageUrl,
      equippedBubble: message.equippedBubble, equippedVehicle: message.equippedVehicle,
    }];
  });
  if (!socket.connected) socket.connect();
  if (socket.connected) { socket.emit("room_message", message); return; }
  const sendAfterConnect = () => { socket.emit("room_message", message); socket.off("connect", sendAfterConnect); };
  socket.once("connect", sendAfterConnect);
}, [roomId, userAccountId, currentUser.name, currentUser.image]);

useEffect(() => {
  if (joinMessageSentRef.current || userAccountId === "guest") return;
  joinMessageSentRef.current = true;
  sendMessageToSocket('Enter the Room', undefined, 'join');
}, [userAccountId, roomId, sendMessageToSocket]);

useEffect(() => {
  if (messagesEndRef.current) messagesEndRef.current.scrollIntoView({ behavior: 'smooth' });
}, [messages]);

useEffect(() => {
  if (showChatInput && inputRef.current) {
    const timer = setTimeout(() => { if (inputRef.current) inputRef.current.focus(); }, 100);
    return () => clearTimeout(timer);
  }
}, [showChatInput]);

useEffect(() => {
  if (!showChatInput) return;
  const handleClickOutside = (e: MouseEvent) => {
    if (inputContainerRef.current && !inputContainerRef.current.contains(e.target as Node)) {
      setShowChatInput(false); setMessage("");
    }
  };
  const handleTouchOutside = (e: TouchEvent) => {
    if (inputContainerRef.current && !inputContainerRef.current.contains(e.target as Node)) {
      setShowChatInput(false); setMessage("");
    }
  };
  const timer = setTimeout(() => {
    document.addEventListener('mousedown', handleClickOutside);
    document.addEventListener('touchstart', handleTouchOutside);
  }, 300);
  return () => {
    clearTimeout(timer);
    document.removeEventListener('mousedown', handleClickOutside);
    document.removeEventListener('touchstart', handleTouchOutside);
  };
}, [showChatInput]);
  const openProfile = useCallback((user: { name: string; image: string; accountId: string }) => {
  const userInSeat = seats.some(s => s.isOccupied && s.user?.accountId === user.accountId);
  setProfileUser({ name: user.name, image: user.image, accountId: user.accountId, isInSeat: userInSeat });
  setShowUserProfile(true);
}, [seats]);

const handleCopyId = (e: React.MouseEvent) => {
  e.stopPropagation();
  navigator.clipboard.writeText(roomOwner.accountId || '');
  setCopied(true);
  setTimeout(() => setCopied(false), 2000);
};

const handleImageClick = (e: React.MouseEvent) => { e.stopPropagation(); if (fileInputRef.current) fileInputRef.current.click(); };

const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
  const file = e.target.files?.[0];
  if (!file) return;
  if (!file.type.startsWith('image/')) { alert('Please select an image file'); return; }
  if (file.size > 5 * 1024 * 1024) { alert('Image size should be less than 5MB'); return; }
  const reader = new FileReader();
  reader.onload = (event) => { sendMessageToSocket('', event.target?.result as string); };
  reader.readAsDataURL(file);
  if (fileInputRef.current) fileInputRef.current.value = '';
};

const handleSeatClick = (seatNumber: number) => (e: React.MouseEvent) => {
  e.preventDefault(); e.stopPropagation();
  setSelectedSeat(seatNumber); setShowSeatSheet(true);
};

const handleSeatAvatarClick = (seat: Seat) => (e: React.MouseEvent) => {
  e.preventDefault(); e.stopPropagation();
  if (seat.isOccupied && seat.user) openProfile({ name: seat.user.name, image: seat.user.image, accountId: seat.user.accountId });
};

const emitSeatAction = (action: "take" | "leave" | "mute" | "lock" | "emoji", seatNumber: number, extra: Record<string, any> = {}) => {
  if (!roomId || userAccountId === "guest") return;
  const send = () => socket.emit("room_seat_action", {
    roomId, userId: userAccountId, action, seatNumber,
    user: { name: currentUser.name || "User", image: currentUser.image || "/default-avatar.png", accountId: userAccountId },
    ...extra,
  });
  if (socket.connected) send(); else { socket.once("connect", send); socket.connect(); }
};

const handleTakeSeat = async (e?: React.MouseEvent) => {
  if (e) { e.preventDefault(); e.stopPropagation(); }
  if (selectedSeat === null) return;
  try {
    const targetSeat = seats.find(s => s.number === selectedSeat);
    if (targetSeat?.isLocked && !targetSeat.isOccupied) { alert("This seat is locked!"); return; }
    if (targetSeat?.isOccupied && targetSeat.user?.accountId !== userAccountId) { alert("This seat is already taken!"); return; }
    const updatedSeats = seats.map(s => {
      if (s.isOccupied && s.user?.accountId === userAccountId && s.number !== selectedSeat)
        return { ...s, isOccupied: false, user: undefined, isSpeaking: false, isMuted: false, gif: undefined };
      if (s.number === selectedSeat)
        return { ...s, isOccupied: true, user: { name: currentUser.name, image: currentUser.image, accountId: userAccountId }, isMuted: false, isSpeaking: false, gif: undefined };
      return s;
    });
    setSeats(updatedSeats);
    emitSeatAction("take", selectedSeat, { user: { name: currentUser.name || "User", image: currentUser.image || "/default-avatar.png", accountId: userAccountId } });
    setShowSeatSheet(false); setSelectedSeat(null);
  } catch (err) { console.error("Error taking seat:", err); }
};

const handleLeaveSeat = async (e?: React.MouseEvent) => {
  if (e) { e.preventDefault(); e.stopPropagation(); }
  if (selectedSeat === null) return;
  try {
    setSeats(seats.map(s => s.number === selectedSeat && s.user?.accountId === userAccountId
      ? { ...s, isOccupied: false, user: undefined, isSpeaking: false, isMuted: false, gif: undefined } : s));
    emitSeatAction("leave", selectedSeat);
    setShowSeatSheet(false); setSelectedSeat(null);
  } catch (err) { console.error("Error leaving seat:", err); }
};

const handleLeaveUserSeat = async (accountId: string) => {
  const targetSeat = seats.find(s => s.isOccupied && s.user?.accountId === accountId);
  if (!targetSeat) return;
  setSeats(seats.map(seat => seat.number === targetSeat.number
    ? { ...seat, isOccupied: false, user: undefined, isSpeaking: false, isMuted: false, gif: undefined } : seat));
  emitSeatAction("leave", targetSeat.number);
};

const handleBottomMicToggle = async (e?: React.MouseEvent) => {
  if (e) e.stopPropagation();
  if (!currentUserSeat) return;
  const newMuteState = !currentUserSeat.isMuted;
  setSeats(seats.map(seat => seat.number === currentUserSeat.number ? { ...seat, isMuted: newMuteState } : seat));
  emitSeatAction("mute", currentUserSeat.number, { isMuted: newMuteState });
};

const handleToggleMute = async (e?: React.MouseEvent) => {
  if (e) { e.preventDefault(); e.stopPropagation(); }
  if (selectedSeat === null) return;
  const target = seats.find(s => s.number === selectedSeat);
  if (!target) return;
  const newMuteState = !target.isMuted;
  setSeats(seats.map(s => s.number === selectedSeat ? { ...s, isMuted: newMuteState } : s));
  emitSeatAction("mute", target.number, { isMuted: newMuteState });
  setShowSeatSheet(false); setSelectedSeat(null);
};

const handleToggleLock = async (e?: React.MouseEvent) => {
  if (e) { e.preventDefault(); e.stopPropagation(); }
  if (selectedSeat === null) return;
  const target = seats.find(s => s.number === selectedSeat);
  if (!target) return;
  const newLockState = !target.isLocked;
  setSeats(seats.map(s => s.number === selectedSeat ? { ...s, isLocked: newLockState } : s));
  emitSeatAction("lock", target.number, { isLocked: newLockState });
  setShowSeatSheet(false); setSelectedSeat(null);
};

const handleInvite = (e?: React.MouseEvent) => {
  if (e) { e.preventDefault(); e.stopPropagation(); }
  if (selectedSeat === null) return;
  alert(`Invite sent to join seat ${selectedSeat}!`);
  setShowSeatSheet(false); setSelectedSeat(null);
};

const isCurrentUsersSeat = (seat?: Seat) => Boolean(seat && seat.isOccupied && seat.user?.accountId === userAccountId);
const showPublicMsgOffAlert = () => setShowPublicMsgModal(true);

const handleSendMessage = (e?: React.MouseEvent) => {
  if (e) e.stopPropagation();
  if (publicMsgOff && !canSendWhenPublicMsgOff) { showPublicMsgOffAlert(); return; }
  if (!message.trim()) return;
  sendMessageToSocket(message.trim());
  setMessage("");
  if (inputRef.current) inputRef.current.focus();
};

const handleKeyPress = (e: React.KeyboardEvent) => { if (e.key === 'Enter') { e.preventDefault(); handleSendMessage(); } };
const handleInputFocus = () => setShowChatInput(true);
const openChatInput = (e?: React.MouseEvent) => {
  if (e) e.stopPropagation();
  if (publicMsgOff && !isRoomOwner) { showPublicMsgOffAlert(); return; }
  setShowChatInput(true);
  setTimeout(() => { if (inputRef.current) inputRef.current.focus(); }, 100);
};

const closeBottomSheet = (e?: React.MouseEvent) => { if (e) e.stopPropagation(); setShowSeatSheet(false); setSelectedSeat(null); };
const closeExitMenu = (e?: React.MouseEvent) => { if (e) e.stopPropagation(); setShowExitMenu(false); };
const openExitMenu = (e?: React.MouseEvent) => { if (e) e.stopPropagation(); setShowExitMenu(true); };
const openSettings = (e?: React.MouseEvent) => { if (e) e.stopPropagation(); setShowSettingPage(true); };
const closeSettings = () => setShowSettingPage(false);

const handleSaveSettings = async (data: Partial<RoomSettingsData>) => {
  const nextRoomName = data.roomName !== undefined ? data.roomName : roomName;
  const nextRoomDp = data.roomDp !== undefined ? data.roomDp : roomDp;
  const nextAnnouncement = data.announcement !== undefined ? data.announcement : roomAnnouncement;
  const nextMicMode = data.micMode !== undefined ? data.micMode : micMode;
  const nextTheme = data.theme !== undefined ? data.theme : Object.keys(THEME_BACKGROUNDS).find(key => THEME_BACKGROUNDS[key] === backgroundImage);
  const nextLocked = data.isLocked !== undefined ? data.isLocked : isLocked;
  const nextPassword = data.roomPassword !== undefined ? data.roomPassword : roomPassword;

  setRoomName(nextRoomName); setRoomAnnouncement(nextAnnouncement); setRoomDp(nextRoomDp); setMicMode(nextMicMode);
  if (nextTheme && THEME_BACKGROUNDS[nextTheme]) setBackgroundImage(THEME_BACKGROUNDS[nextTheme]);
  setIsLocked(nextLocked); setRoomPassword(nextPassword);
  if (!roomId) return;

  const roomSettings: RoomSettingsCache = {
    roomId: String(roomId), roomName: nextRoomName || "Room", roomDp: nextRoomDp || "/default-avatar.png",
    announcement: nextAnnouncement || "", micMode: Number(nextMicMode || 0), theme: nextTheme || "mood-light",
    isLocked: Boolean(nextLocked), roomPassword: nextPassword || "", updatedAt: Date.now(),
  };
  await saveRoomSettingsToIndexedDB(roomSettings);

  const isOwnerOfRoom =
    String(roomId) === String(currentUser.id) || String(roomId) === String(currentUser.accountId) ||
    String(roomOwner.id) === String(currentUser.id) || String(roomOwner.accountId) === String(currentUser.accountId);

  try {
    const dbRoomData = {
      accountId: roomOwner.accountId || roomId, id: roomOwner.accountId || roomId, roomId: roomOwner.accountId || roomId,
      name: roomSettings.roomName, roomName: roomSettings.roomName, 'Room Name': roomSettings.roomName,
      image: roomSettings.roomDp, roomDp: roomSettings.roomDp, 'Room dp': roomSettings.roomDp,
      country: roomCountry || localStorage.getItem('userCountry') || '🇮🇳',
      message: roomSettings.announcement, announcement: roomSettings.announcement,
      theme: roomSettings.theme, micMode: roomSettings.micMode,
      isLocked: roomSettings.isLocked, roomPassword: roomSettings.roomPassword,
    };
    const res = await fetch(apiUrl('/api/rooms'), { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(dbRoomData) });
    if (!res.ok) console.warn('Room settings save failed:', res.status);
  } catch (error) { console.error("Failed to save room settings to MongoDB", error); }

  if (isOwnerOfRoom) {
    let existingMyRoom: any = {};
    try { const rawMyRoom = localStorage.getItem('myRoom'); if (rawMyRoom) existingMyRoom = JSON.parse(rawMyRoom); } catch (e) {}
    const updatedMyRoomCard = {
      ...existingMyRoom,
      id: currentUser.id || roomId,
      accountId: currentUser.accountId || roomOwner.accountId,
      name: roomSettings.roomName, image: roomSettings.roomDp,
      country: roomCountry || localStorage.getItem('userCountry') || existingMyRoom.country || '🇮🇳'
    };
    localStorage.setItem('myRoom', JSON.stringify(updatedMyRoomCard));
    window.dispatchEvent(new Event('storage'));
  }

  socket.emit("room_settings_update", {
    roomId: roomOwner.accountId || roomId, userId: userAccountId,
    roomOwnerId: roomOwner.accountId || roomOwner.uid || roomOwner.id || roomId,
    roomName: roomSettings.roomName, roomDp: roomSettings.roomDp,
    announcement: roomSettings.announcement, micMode: roomSettings.micMode,
    theme: roomSettings.theme, isLocked: roomSettings.isLocked,
    roomPassword: roomSettings.roomPassword, updatedAt: Date.now(),
  });
};

const handleExit = (e?: React.MouseEvent) => {
  if (e) e.stopPropagation();
  isKeepingRef.current = false;
  setShowExitMenu(false);
  localStorage.removeItem('keptRoom');
  setMessages([]);
  if (onBack) onBack(); if (onClose) onClose();
};

const handleKeep = (e?: React.MouseEvent) => {
  if (e) e.stopPropagation();
  isKeepingRef.current = true;
  const keptAccId = roomOwner.accountId || (roomId ? generateStableId(roomId) : "");
  const roomData = { name: roomName || "Room", image: roomDp || "/default-avatar.png", accountId: keptAccId, id: roomId, roomId };
  localStorage.setItem('keptRoom', JSON.stringify(roomData));
  setShowExitMenu(false);
  if (onKeepRoom) onKeepRoom(roomData);
  if (onBack) onBack();
};

const handleSeatEmoji = async (emojiData: any) => {
  if (!hasSeat || !currentUserSeat) return;
  const sendTimestamp = Date.now();
  const seatNum = currentUserSeat.number;
  setSeats(prev => prev.map(s => s.number === seatNum ? { ...s, gif: { src: emojiData.src, timestamp: sendTimestamp } } : s));
  emitSeatAction("emoji", seatNum, { src: emojiData.src, timestamp: sendTimestamp });
  setTimeout(() => { setSeats(prev => prev.map(s => s.number === seatNum ? { ...s, gif: undefined } : s)); }, 5000);
};

const handlePlayMusic = (track: MusicTrack, playlist?: MusicTrack[]) => {
  if (playlist && playlist.length > 0) {
    setMusicPlaylist(playlist);
    const index = playlist.findIndex(t => t.id === track.id);
    setCurrentTrackIndex(index >= 0 ? index : 0);
  } else { setMusicPlaylist([track]); setCurrentTrackIndex(0); }
  if (musicAudioRef.current) {
    musicAudioRef.current.pause();
    musicAudioRef.current.src = track.url;
    musicAudioRef.current.volume = musicVolume;
    musicAudioRef.current.play();
  } else {
    const audio = new Audio(track.url);
    audio.volume = musicVolume;
    musicAudioRef.current = audio;
    audio.play();
    audio.addEventListener('timeupdate', () => setMusicCurrentTime(audio.currentTime));
    audio.addEventListener('loadedmetadata', () => setMusicDuration(audio.duration));
    audio.addEventListener('ended', () => handleNextTrack());
  }
  setCurrentTrack(track); setIsMusicPlaying(true); setMusicControllerState('full');
  setMusicCurrentTime(0); setMusicDuration(0);
};

const handleToggleMusicPlay = () => {
  if (!musicAudioRef.current || !currentTrack) return;
  if (isMusicPlaying) { musicAudioRef.current.pause(); setIsMusicPlaying(false); }
  else { musicAudioRef.current.play(); setIsMusicPlaying(true); }
};

const handleVolumeChange = (e: React.ChangeEvent<HTMLInputElement>) => {
  const newVolume = parseFloat(e.target.value);
  setMusicVolume(newVolume);
  if (musicAudioRef.current) musicAudioRef.current.volume = newVolume;
};

const handleProgressChange = (e: React.ChangeEvent<HTMLInputElement>) => {
  const newTime = parseFloat(e.target.value);
  setMusicCurrentTime(newTime);
  if (musicAudioRef.current) musicAudioRef.current.currentTime = newTime;
};

const handleNextTrack = () => {
  if (musicPlaylist.length === 0) return;
  const nextIndex = (currentTrackIndex + 1) % musicPlaylist.length;
  const nextTrack = musicPlaylist[nextIndex];
  if (musicAudioRef.current) { musicAudioRef.current.src = nextTrack.url; musicAudioRef.current.volume = musicVolume; musicAudioRef.current.play(); }
  setCurrentTrackIndex(nextIndex); setCurrentTrack(nextTrack); setIsMusicPlaying(true);
  setMusicCurrentTime(0); setMusicDuration(0);
};

const handlePrevTrack = () => {
  if (musicPlaylist.length === 0) return;
  const prevIndex = (currentTrackIndex - 1 + musicPlaylist.length) % musicPlaylist.length;
  const prevTrack = musicPlaylist[prevIndex];
  if (musicAudioRef.current) { musicAudioRef.current.src = prevTrack.url; musicAudioRef.current.volume = musicVolume; musicAudioRef.current.play(); }
  setCurrentTrackIndex(prevIndex); setCurrentTrack(prevTrack); setIsMusicPlaying(true);
  setMusicCurrentTime(0); setMusicDuration(0);
};

const handleCloseMusicController = () => {
  if (musicAudioRef.current) { musicAudioRef.current.pause(); musicAudioRef.current = null; }
  setMusicControllerState('hidden'); setCurrentTrack(null); setIsMusicPlaying(false);
};

useEffect(() => () => { if (musicAudioRef.current) { musicAudioRef.current.pause(); musicAudioRef.current = null; } }, []);
useEffect(() => { if (musicAudioRef.current) musicAudioRef.current.volume = musicVolume; }, [musicVolume]);

const formatTime = (seconds: number) => {
  if (isNaN(seconds)) return '0:00';
  const mins = Math.floor(seconds / 60);
  const secs = Math.floor(seconds % 60);
  return `${mins}:${secs.toString().padStart(2, '0')}`;
};

const liveUserCount = roomUsers.length;
const selectedSeatData = selectedSeat !== null ? seats.find(s => s.number === selectedSeat) : null;
const isSelectedSeatMySeat = selectedSeatData ? isCurrentUsersSeat(selectedSeatData) : false;
const isSelectedSeatTakenByOther = selectedSeatData ? (selectedSeatData.isOccupied && !isSelectedSeatMySeat) : false;

const renderSeats = () => {
  const renderSeatItems = (seatNumbers: number[]) => seatNumbers.map(num => {
    const seat = seats.find(s => s.number === num);
    return (
      <MemoSeatItem key={num} seatNumber={num} seatData={seat} onClick={handleSeatClick(num)} onAvatarClick={handleSeatAvatarClick(seat!)} accountId={userAccountId} roomOwnerId={roomOwnerId} />
    );
  });
  if (micMode === 5) return (
    <div className="flex flex-col gap-2.5 w-full px-0 -mt-4" style={{ '--seat-size': '66px' } as React.CSSProperties}>
      <div className="flex justify-center">{renderSeatItems([1])}</div>
      <div className="flex justify-around items-center w-full px-0">{renderSeatItems([2, 3, 4, 5])}</div>
    </div>
  );
  if (micMode === 10) return (
    <div className="flex flex-col gap-2.5 w-full px-0">
      <div className="grid grid-cols-5 justify-items-center w-full px-0">{renderSeatItems([1, 2, 3, 4, 5])}</div>
      <div className="grid grid-cols-5 justify-items-center w-full px-0">{renderSeatItems([6, 7, 8, 9, 10])}</div>
    </div>
  );
  return (
    <div className="flex flex-col gap-2.5 w-full px-0">
      <div className="grid grid-cols-5 justify-items-center w-full px-0">{renderSeatItems([1, 2, 3, 4, 5])}</div>
      <div className="grid grid-cols-5 justify-items-center w-full px-0">{renderSeatItems([6, 7, 8, 9, 10])}</div>
      <div className="grid grid-cols-5 justify-items-center w-full px-0">{renderSeatItems([11, 12, 13, 14, 15])}</div>
    </div>
  );
};

if (showStore) return <StorePage onBack={() => setShowStore(false)} initialView={storeInitialView} />;
if (showSettingPage) return (
  <RoomSettingPage
    onBack={closeSettings}
    roomOwnerId={roomOwnerId}
    roomData={{ roomName, roomDp, announcement: roomAnnouncement, micMode, isLocked, roomPassword, theme: Object.keys(THEME_BACKGROUNDS).find(key => THEME_BACKGROUNDS[key] === backgroundImage) || 'mood-light' }}
    onSave={handleSaveSettings}
  />
);
  return (
  <div className="fixed inset-0 z-50 bg-black flex flex-col" style={{ height: '100dvh', maxHeight: '100dvh', paddingBottom: 'env(safe-area-inset-bottom, 0px)', boxSizing: 'border-box' }}>
    <img src={backgroundImage} alt="Room Background" className="absolute inset-0 w-full h-full object-cover opacity-60 pointer-events-none" draggable={false} />
    <input ref={fileInputRef} type="file" accept="image/*" onChange={handleFileChange} className="hidden" aria-label="Upload image" />

    <div className="relative z-10 flex flex-col flex-1 min-h-0" style={{ paddingTop: 'calc(env(safe-area-inset-top, 0px) + 30px)', paddingBottom: '8px', boxSizing: 'border-box' }} onClick={(e) => e.stopPropagation()}>

      {/* Top Header — pill card left edge chipka, DP square, bg-black/30 same as icons */}
      <div className="flex justify-between items-center text-white flex-shrink-0">
        <div className="flex items-center gap-2 rounded-r-full bg-black/30 py-1 pr-4 pl-2 border-none shadow-sm max-w-[220px]">
          <button
            onClick={() => { setRoomInfoTab('profile'); setShowRoomInfo(true); }}
            className="rounded-md overflow-hidden flex-shrink-0 cursor-pointer"
            style={{ width: '34px', height: '34px' }}
          >
            <img
              src={roomDp && roomDp !== "undefined" && roomDp !== "null" ? roomDp : "/default-avatar.png"}
              onError={(e) => { (e.target as HTMLImageElement).src = "/default-avatar.png"; }}
              alt="Room Cover"
              className="w-full h-full object-cover"
              draggable={false}
              loading="eager"
              decoding="async"
            />
          </button>
          <div className="flex flex-col min-w-0 text-left">
            <div className="flex items-center gap-1 min-w-0">
              <h2 className="font-bold leading-tight truncate" style={{ fontSize: '12px', maxWidth: '90px' }}>
                {(roomName || 'Room').length > 12
                  ? (roomName || 'Room').slice(0, 12) + '...'
                  : (roomName || 'Room')}
              </h2>
              {roomCountry && (
                <span className="text-[12px] leading-none shrink-0">{roomCountry}</span>
              )}
              {!isRoomOwner && (
                <button
                  onClick={(e) => { e.stopPropagation(); const nf = !isFollowed; setIsFollowed(nf); if (onFollowToggle) onFollowToggle(roomId, nf); }}
                  className="rounded-full flex items-center justify-center transition-all cursor-pointer bg-blue-500 shadow-md hover:bg-blue-600 shrink-0"
                  style={{ width: '20px', height: '20px', border: 'none' }}
                >
                  <svg viewBox="0 0 24 24" className="fill-white" style={{ width: '12px', height: '12px' }}><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" /></svg>
                </button>
              )}
            </div>
            <span className="text-white leading-tight mt-0.5" style={{ fontSize: '10px' }}>ID:{roomOwner.accountId || ''}</span>
          </div>
        </div>

        <div className="flex items-center gap-0.5 pr-2">
          <button onClick={(e) => { e.stopPropagation(); setShowActiveUsers(true); }} className="flex items-center gap-1 bg-black/30 rounded-full border-none hover:bg-black/30 transition-colors cursor-pointer shadow-sm" style={{ height: 'var(--header-btn-size)', padding: 'var(--header-btn-padding)' }}>
            <svg viewBox="0 0 24 24" className="fill-white" style={{ width: 'var(--header-icon-size)', height: 'var(--header-icon-size)' }}>
              <circle cx="9" cy="7" r="4.2" />
              <path d="M2 20.5 C 2 15.5 5.2 13 9 13 C 12.8 13 16 15.5 16 20.5 Z" />
              <rect x="17.5" y="7.5" width="4.5" height="1.8" rx="0.9" />
              <rect x="17.5" y="11.1" width="4.5" height="1.8" rx="0.9" />
              <rect x="17.5" y="14.7" width="3.5" height="1.8" rx="0.9" />
            </svg>
            <span className="text-white font-semibold leading-none" style={{ fontSize: 'var(--header-count-size)' }}>{liveUserCount}</span>
          </button>

          {isRoomOwner && (
            <button onClick={openSettings} aria-label="Settings" className="bg-black/30 rounded-full border-none hover:bg-black/30 transition-colors cursor-pointer flex items-center justify-center shadow-sm" style={{ width: 'var(--header-btn-size)', height: 'var(--header-btn-size)' }}>
              <svg viewBox="0 0 24 24" className="fill-none stroke-white stroke-[2.2] stroke-linecap-round stroke-linejoin-round" style={{ width: 'var(--header-icon-size)', height: 'var(--header-icon-size)' }}>
                <polygon points="12 2.5 20.2 7.25 20.2 16.75 12 21.5 3.8 16.75 3.8 7.25" />
                <circle cx="12" cy="12" r="2.8" />
              </svg>
            </button>
          )}

          <button onClick={openExitMenu} aria-label="Power" className="bg-black/30 rounded-full border-none hover:bg-black/30 transition-colors flex items-center justify-center cursor-pointer shadow-sm" style={{ width: 'var(--header-btn-size)', height: 'var(--header-btn-size)' }}>
            <svg viewBox="0 0 24 24" className="fill-none stroke-white stroke-[2.5] stroke-linecap-round stroke-linejoin-round" style={{ width: 'var(--header-icon-size)', height: 'var(--header-icon-size)' }}><path d="M12 4v8" /><path d="M18.36 6.64a9 9 0 1 1-12.72 0" /></svg>
          </button>
        </div>
      </div>

      {/* Trophy Card + Music Pill */}
      <div className="h-0 w-full relative z-20">
        <div className="absolute top-2 left-0 -ml-1 sm:-ml-2 flex items-center gap-2">
          <button onClick={() => setShowCupIcon(true)} className="bg-gradient-to-r from-[#242b35]/90 via-[#242b35]/60 to-transparent flex items-center pr-3 pl-3 py-1 cursor-pointer border-none">
            <div className="w-4 h-4 flex items-center justify-center shrink-0 relative overflow-visible mr-1.5">
              <GreenColorRemovalShader imageSrc="/1788258883971~2.jpg" threshold={0.5} className="w-full h-full" style={{ width: '115%', height: '115%', objectFit: 'contain', maxWidth: 'none', maxHeight: 'none', pointerEvents: 'none' }} />
            </div>
            <span className="font-bold text-[13px] leading-none tracking-tight" style={{ color: '#eef3a3' }}>{formatCupCount(cupCount)}</span>
            <svg viewBox="0 0 24 24" className="fill-none stroke-[3] ml-1 opacity-90" stroke="#eef3a3" style={{ width: '10px', height: '10px' }}><polyline points="9 18 15 12 9 6" /></svg>
          </button>

          {musicControllerState === 'minimized' && currentTrack && (
            <button
              type="button"
              onClick={(e) => { e.stopPropagation(); setMusicControllerState('full'); }}
              className="relative z-30 pointer-events-auto flex items-center gap-1 rounded-full bg-black/30 pl-0.5 pr-2 py-0.5 shadow-sm hover:bg-black/40 active:scale-95 transition-transform cursor-pointer border-none"
              style={{ maxWidth: '110px' }}
              aria-label="Open music player"
            >
              <img
                src="/file_00000000ee70820b9fd0dfe71739874e.png"
                alt="Music"
                className={`shrink-0 ${isMusicPlaying ? 'music-minimize-icon' : ''}`}
                style={{ width: '24px', height: '24px', objectFit: 'contain' }}
                draggable={false}
                loading="eager"
                decoding="async"
              />
              <MarqueeText
                text={currentTrack.name}
                className="text-white text-[11px] font-semibold"
                style={{ maxWidth: '68px' }}
              />
            </button>
          )}
        </div>
      </div>

      {/* Middle */}
      <div className="flex-1 flex flex-col min-h-0 px-1 sm:px-2">
        <div className="flex-shrink-0 flex flex-col gap-5 pt-12 sm:pt-10 w-full">{renderSeats()}</div>

        <div ref={messagesContainerRef} className="mx-1 mt-4 flex-1 overflow-y-auto scrollbar-none">
          <div className="mx-1 mb-3 flex justify-start">
            <div className="max-w-[75%] bg-black/30 border border-none shadow-sm" style={{ padding: '12px 14px', borderRadius: '8px' }}>
              <p className="leading-snug font-medium" style={{ fontSize: 'var(--announcement-text-size)', color: '#e2c67d' }}>
                Official announcement: Welcome to Hurry Any Content Realted to porn,Froud,Fake Official will Ban!
              </p>
              {roomAnnouncement && (
                <div className="mt-2 pt-2 border-t border-white/10">
                  <p className="leading-snug font-medium" style={{ fontSize: 'var(--announcement-text-size)', color: '#e2c67d' }}>
                    <span className="font-bold mr-1">ANNOUNCEMENT: </span>{roomAnnouncement}
                  </p>
                </div>
              )}
            </div>
          </div>

          <div className="space-y-0.5">
            {messages.map((msg) => (
              <div key={msg.id} className="leading-[1.8rem]">
                {msg.type === 'join' ? (
                  <>
                    <div className="flex items-start gap-1.5 px-2.5 py-1.5 w-fit bg-black/20 rounded-md">
                      <div className="rounded-full overflow-hidden flex-shrink-0 cursor-pointer" style={{ width: 'var(--msg-avatar-size)', height: 'var(--msg-avatar-size)' }} onClick={() => openProfile({ name: msg.sender, image: msg.senderImage, accountId: msg.senderAccountId || generateStableId(msg.sender) })}>
                        <img src={msg.senderImage || "/default-avatar.png"} alt={msg.sender} className="w-full h-full object-cover" draggable={false} loading="eager" decoding="async" onError={(e) => { (e.target as HTMLImageElement).src = "/default-avatar.png"; }} />
                      </div>
                      <div className="flex flex-col min-w-0">
                        <div className="flex items-center gap-0.5 min-w-0">
                          <span className="font-semibold text-white/95 leading-none truncate" style={{ fontSize: 'var(--msg-name-size)' }}>{msg.sender}</span>
                          <span className="flex items-center justify-center shrink-0" style={{ width: 'calc(var(--msg-avatar-size) * 0.7)', height: 'calc(var(--msg-avatar-size) * 0.7)' }}>
                            <HouseIcon size={14} style={{ width: '100%', height: '100%' }} />
                          </span>
                        </div>
                        <span className="text-white/80 leading-none mt-0.5" style={{ fontSize: 'var(--msg-jointime-size)' }}>Enter the Room</span>
                      </div>
                    </div>
                    {msg.equippedVehicle && <EntryEffect vehicleUrl={msg.equippedVehicle} userName={msg.sender} />}
                  </>
                ) : msg.imageUrl ? (
                  <div className="flex items-start gap-2 max-w-[75%]" style={{ height: 'calc(4 * 1.8rem)' }}>
                    <div className="rounded-full overflow-hidden flex-shrink-0 mt-0.5 cursor-pointer border border-black/10" style={{ width: 'var(--msg-avatar-size)', height: 'var(--msg-avatar-size)' }} onClick={() => openProfile({ name: msg.sender, image: msg.senderImage, accountId: msg.senderAccountId || generateStableId(msg.sender) })}>
                      <img src={msg.senderImage || "/default-avatar.png"} alt={msg.sender} className="w-full h-full object-cover" draggable={false} loading="eager" decoding="async" onError={(e) => { (e.target as HTMLImageElement).src = "/default-avatar.png" }} />
                    </div>
                    <div className="flex flex-col min-w-0">
                      <span className="font-semibold text-white/90 leading-tight drop-shadow-sm" style={{ fontSize: 'var(--msg-name-size)' }}>{msg.sender}</span>
                      <div onClick={() => setFullImageModal(msg.imageUrl || null)} className="rounded-xl overflow-hidden border border-black/10 cursor-pointer hover:opacity-90 transition-opacity bg-black/40 flex items-center justify-center mt-0.5 shadow-sm" style={{ height: 'calc(3.5 * 1.8rem)', width: 'calc(3.5 * 1.8rem)' }}>
                        <img src={msg.imageUrl} alt="Shared image" className="w-full h-full object-cover" draggable={false} loading="eager" decoding="async" />
                      </div>
                    </div>
                  </div>
                ) : (
                  <div className="flex items-start gap-2 max-w-[75%]">
                    <div className="rounded-full overflow-hidden flex-shrink-0 mt-0.5 cursor-pointer border border-white/10" style={{ width: 'var(--msg-avatar-size)', height: 'var(--msg-avatar-size)' }} onClick={() => openProfile({ name: msg.sender, image: msg.senderImage, accountId: msg.senderAccountId || generateStableId(msg.sender) })}>
                      <img src={msg.senderImage || "/default-avatar.png"} alt={msg.sender} className="w-full h-full object-cover" draggable={false} loading="eager" decoding="async" onError={(e) => { (e.target as HTMLImageElement).src = "/default-avatar.png" }} />
                    </div>
                    <div className="flex flex-col min-w-0">
                      <span className="font-semibold text-white/90 leading-tight drop-shadow-sm" style={{ fontSize: 'var(--msg-name-size)' }}>{msg.sender}</span>
                      {msg.equippedBubble ? (
                        <div className="px-3 py-2 mt-0.5 inline-flex w-fit max-w-full items-center justify-center relative" style={{ backgroundImage: `url(${msg.equippedBubble})`, backgroundSize: '100% 100%', backgroundRepeat: 'no-repeat', backgroundPosition: 'center', minHeight: '40px' }}>
                          <p className="break-words leading-tight text-white relative z-10" style={{ fontSize: 'var(--msg-text-size)' }}>{msg.text}</p>
                        </div>
                      ) : (
                        <div className="px-2 py-1.5 rounded-xl bg-black/30 text-white rounded-tl-sm mt-0.5 border border-black/10 shadow-sm">
                          <p className="break-words leading-tight" style={{ fontSize: 'var(--msg-text-size)' }}>{msg.text}</p>
                        </div>
                      )}
                    </div>
                  </div>
                )}
              </div>
            ))}
            <div ref={messagesEndRef} />
          </div>
        </div>
      </div>

      {/* Footer Controls */}
      <div className={`flex-shrink-0 pt-2 px-2 ${showChatInput ? 'hidden' : ''}`}>
        <div className="relative flex items-center justify-between w-full h-full gap-1 min-h-[var(--footer-btn-size)]">
          <div className="flex items-center justify-start gap-1 z-10">
            <button onClick={openChatInput} aria-label="Say Hi Chat" className="bg-black/30 rounded-full border-none hover:bg-black/30 transition-colors flex items-center justify-center shrink-0 cursor-pointer shadow-sm" style={{ width: 'var(--footer-btn-size)', height: 'var(--footer-btn-size)' }}>
              <svg viewBox="0 0 24 24" className="fill-white" style={{ width: 'calc(var(--footer-icon-size) + 4px)', height: 'calc(var(--footer-icon-size) + 4px)' }}>
                <path d="M12 2C6.48 2 2 5.92 2 10.75c0 2.8 1.5 5.29 3.82 6.84l-1.4 3.7c-.12.33.22.64.53.5l4-1.63c1 .3 2 .46 3.05.46 5.52 0 10-3.92 10-8.75S17.52 2 12 2zm-4 11.5c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5zm4 0c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5zm4 0c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5z" />
              </svg>
            </button>
            {hasSeat && (
              <button onClick={handleBottomMicToggle} className="bg-black/30 rounded-full border-none hover:bg-black/30 transition-all shrink-0 flex items-center justify-center cursor-pointer shadow-sm p-0 overflow-visible" style={{ width: 'var(--footer-btn-size)', height: 'var(--footer-btn-size)' }}>
                {currentUserSeat?.isMuted ? (
                  <svg viewBox="-2 -2 28 28" className="fill-white overflow-visible" style={{ width: '32px', height: '32px' }}>
                    <defs><mask id="mic-cut-muted"><rect x="-2" y="-2" width="32" height="32" fill="white" /><rect x="9" y="6" width="6" height="2" rx="1" fill="black" /></mask></defs>
                    <rect x="7.5" y="1" width="9" height="14" rx="4.5" fill="#ffffff" mask="url(#mic-cut-muted)" />
                    <path d="M4 11 a8 8 0 0 0 16 0 h-3 a5 5 0 0 1 -10 0 Z" fill="#ffffff" />
                    <rect x="10.5" y="18" width="3" height="5" fill="#ffffff" />
                    <line x1="1" y1="1" x2="23" y2="23" stroke="#000000" strokeWidth="3.5" strokeLinecap="round" />
                    <line x1="1" y1="1" x2="23" y2="23" stroke="#ffffff" strokeWidth="2.2" strokeLinecap="round" />
                  </svg>
                ) : (
                  <svg viewBox="-2 -2 28 28" className="fill-white overflow-visible" style={{ width: '32px', height: '32px' }}>
                    <defs><mask id="mic-cut-unmuted"><rect x="-2" y="-2" width="32" height="32" fill="white" /><rect x="9" y="6" width="6" height="2" rx="1" fill="black" /></mask></defs>
                    <rect x="7.5" y="1" width="9" height="14" rx="4.5" fill="#ffffff" mask="url(#mic-cut-unmuted)" />
                    <path d="M4 11 a8 8 0 0 0 16 0 h-3 a5 5 0 0 1 -10 0 Z" fill="#ffffff" />
                    <rect x="10.5" y="18" width="3" height="5" fill="#ffffff" />
                  </svg>
                )}
              </button>
            )}
            {hasSeat && (
              <button onClick={(e) => { e.stopPropagation(); setShowEmojiPicker(true); }} className="bg-black/30 rounded-full border-none hover:bg-black/30 transition-colors shrink-0 flex items-center justify-center cursor-pointer shadow-sm" style={{ width: 'var(--footer-btn-size)', height: 'var(--footer-btn-size)' }}>
                <svg viewBox="0 0 24 24" className="fill-white" style={{ width: 'var(--footer-icon-size)', height: 'var(--footer-icon-size)' }}>
                  <path fillRule="evenodd" clipRule="evenodd" d="M12 22C6.477 22 2 17.523 2 12S6.477 2 12 2s10 4.477 10 10-4.477 10-10 10zM8.5 7.5a1.5 1.5 0 1 0 0 3 1.5 1.5 0 0 0 0-3zm7 0a1.5 1.5 0 1 0 0 3 1.5 1.5 0 0 0 0-3zM7 14h10c0 3-2.5 5-5 5s-5-2-5-5z" />
                </svg>
              </button>
            )}
          </div>

          <div className="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 z-20 flex items-center justify-center">
            <img src="/file_0000000019c4821180028eebae10dbfc.png" alt="Gift" className="cursor-pointer active:scale-95 transition-transform select-none" style={{ width: 'calc(var(--footer-btn-size) + 9px)', height: 'auto' }} draggable={false} loading="eager" decoding="async" onClick={(e) => { e.stopPropagation(); setShowGiftPicker(true); }} />
          </div>

          <div className="flex items-center justify-end gap-1 z-10">
            <button onClick={(e) => { e.stopPropagation(); setShowMessageSheet(true); }} aria-label="Message Box Menu" className="bg-black/30 rounded-full border-none hover:bg-black/30 transition-colors flex items-center justify-center shrink-0 cursor-pointer shadow-sm" style={{ width: 'var(--footer-btn-size)', height: 'var(--footer-btn-size)' }}>
              <svg viewBox="0 0 24 24" style={{ width: 'calc(var(--footer-icon-size) + 2px)', height: 'calc(var(--footer-icon-size) + 2px)' }}>
                <defs><mask id="mail-v-cutout"><rect x="0" y="0" width="24" height="24" fill="white" /><path d="M3.5 7.5L12 13.5L20.5 7.5" stroke="black" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" fill="none" /></mask></defs>
                <rect x="2" y="4.5" width="20" height="15" rx="3.5" fill="#ffffff" mask="url(#mail-v-cutout)" />
              </svg>
            </button>

            <button
              onClick={(e) => { e.stopPropagation(); setShowFourGride(true); }}
              aria-label="Apps Menu"
              className="bg-black/30 rounded-full border-none hover:bg-black/30 transition-colors flex items-center justify-center shrink-0 cursor-pointer shadow-sm"
              style={{ width: 'var(--footer-btn-size)', height: 'var(--footer-btn-size)' }}
            >
              <svg viewBox="0 0 24 24" className="fill-white" style={{ width: 'var(--footer-icon-size)', height: 'var(--footer-icon-size)' }}>
                <rect x="3" y="3" width="8" height="8" rx="1.5" />
                <rect x="13" y="3" width="8" height="8" rx="1.5" />
                <rect x="3" y="13" width="8" height="8" rx="1.5" />
                <rect x="13.5" y="13.5" width="7" height="1.6" rx="0.8" />
                <rect x="13.5" y="16.2" width="7" height="1.6" rx="0.8" />
                <rect x="13.5" y="18.9" width="7" height="1.6" rx="0.8" />
              </svg>
            </button>
          </div>
        </div>
      </div>

      {showChatInput && (
        <div ref={inputContainerRef} className="fixed left-0 right-0 z-[10000] flex items-center w-full" style={{ bottom: 0, paddingBottom: 'env(safe-area-inset-bottom, 0px)', boxSizing: 'border-box' }}>
          <div className="flex-1 bg-white flex items-center px-3 py-2 w-full rounded-none">
            <button onMouseDown={(e) => e.preventDefault()} onClick={handleImageClick} className="hover:bg-gray-100 rounded-full transition-colors flex-shrink-0 cursor-pointer">
              <svg viewBox="0 0 24 24" className="fill-none stroke-gray-500 stroke-[2] stroke-linecap-round stroke-linejoin-round" style={{ width: 'var(--footer-icon-size)', height: 'var(--footer-icon-size)' }}>
                <rect x="3" y="3" width="18" height="18" rx="2" ry="2" /><circle cx="8.5" cy="8.5" r="1.5" /><polyline points="21 15 16 10 5 21" />
              </svg>
            </button>
            <input ref={inputRef} type="text" value={message} onChange={(e) => setMessage(e.target.value)} onKeyPress={handleKeyPress} onFocus={handleInputFocus} placeholder="Type a message..." className="flex-1 bg-transparent text-gray-800 placeholder-gray-400 px-2 py-1.5 outline-none border-none rounded-none" style={{ fontSize: 'var(--footer-input-text)' }} />
            <button onMouseDown={(e) => e.preventDefault()} onClick={handleSendMessage} className="hover:bg-blue-50 rounded-full transition-colors cursor-pointer flex-shrink-0">
              <svg viewBox="0 0 24 24" className="fill-none stroke-blue-500 stroke-[2] stroke-linecap-round stroke-linejoin-round" style={{ width: 'var(--footer-icon-size)', height: 'var(--footer-icon-size)' }}>
                <line x1="22" y1="2" x2="11" y2="13" /><polygon points="22 2 15 22 11 13 2 9 22 2" />
              </svg>
            </button>
          </div>
        </div>
      )}
    </div>

    {/* RIGHT-SIDE STACK */}
    <div
      className={`absolute z-20 flex flex-col items-center pointer-events-auto gap-0.5 ${showChatInput ? 'hidden' : ''}`}
      style={{ right: '10px', bottom: 'calc(env(safe-area-inset-bottom, 0px) + var(--footer-btn-size) + 24px)' }}
      onClick={(e) => e.stopPropagation()}
    >
      {showWildParty === 'minimized' && (
        <button onClick={() => setShowWildParty(true)} className="rounded-md overflow-hidden shadow-lg border-none bg-transparent transition-transform active:scale-95 cursor-pointer p-0" style={{ width: '56px', height: '56px' }} aria-label="Open Wild Party">
          <img src="/file_000000009d808211b8ffb7c2183b4ef5.png" alt="Wild Party" className="w-full h-full object-contain" draggable={false} loading="eager" decoding="async" />
        </button>
      )}
      {showFruitParty === 'minimized' && (
        <button onClick={() => setShowFruitParty(true)} className="rounded-md overflow-hidden shadow-lg border-none bg-transparent transition-transform active:scale-95 cursor-pointer p-0" style={{ width: '44px', height: '44px' }} aria-label="Open Fruit Party">
          <img src="/fruit-party-logo.jpg" alt="Fruit Party" className="w-full h-full object-contain rounded-md" draggable={false} loading="eager" decoding="async" />
        </button>
      )}

      <RoomSideBanner />

      <div onClick={() => setShowRoomTask(true)} className="relative cursor-pointer transition-transform hover:scale-105 flex items-center justify-center" style={{ width: 'calc(var(--footer-btn-size) * 1.35)', height: 'calc(var(--footer-btn-size) * 1.35)' }}>
        <GreenColorRemovalShader imageSrc="/IMG-20260902-WA0066.jpg" threshold={0.45} className="w-full h-full" style={{ width: '100%', height: '100%', objectFit: 'contain', pointerEvents: 'none' }} />
      </div>

      <div className="relative cursor-pointer transition-transform hover:scale-105" style={{ width: 'calc(var(--footer-btn-size) * 1.2)', height: 'calc(var(--footer-btn-size) * 1.2)' }} onClick={() => setShowGameSheet(true)}>
        <img
          src="/file_000000002a8c81fab8232bebeeeb45cc.png"
          alt="Games"
          className="w-full h-full object-contain select-none pointer-events-none"
          draggable={false}
          loading="eager"
          decoding="async"
        />
      </div>
    </div>
          {showPublicMsgModal && (
        <div className="absolute inset-0 z-[10000] flex items-center justify-center bg-black/50" onClick={() => setShowPublicMsgModal(false)}>
          <div className="bg-white rounded-2xl px-5 py-4 shadow-xl max-w-xs w-full text-center" onClick={(e) => e.stopPropagation()}>
            <h3 className="text-base font-bold text-gray-800 mb-1">Public msg are off</h3>
            <p className="text-xs text-gray-500 mb-3">Only the room owner can send messages right now.</p>
            <button onClick={() => setShowPublicMsgModal(false)} className="bg-blue-500 hover:bg-blue-600 text-white font-semibold py-1.5 px-5 rounded-full transition-colors cursor-pointer">OK</button>
          </div>
        </div>
      )}

      {/* ACTIVE USERS SHEET — no cards, black 0.92, 15px corner */}
      {showActiveUsers && (
        <div className="fixed inset-0 z-[9999] flex items-end justify-center" style={{ paddingBottom: 'env(safe-area-inset-bottom)' }}>
          <div className="absolute inset-0 bg-black/30" onClick={() => setShowActiveUsers(false)} />
          <div
            className="relative w-full max-w-md animate-slide-up overflow-hidden flex flex-col"
            style={{
              height: '40vh',
              maxHeight: '40vh',
              background: 'rgba(0,0,0,0.92)',
              borderTopLeftRadius: '15px',
              borderTopRightRadius: '15px',
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <div className="px-4 pt-4 pb-2 flex-shrink-0">
              <h2 className="text-base font-bold text-white text-center">Active Users</h2>
            </div>
            <div className="flex-1 overflow-y-auto px-3 py-2" style={{ minHeight: 0 }}>
              <div className="space-y-3 pb-4">
                <div className="flex items-center gap-2 px-1">
                  <div
                    className="rounded-full overflow-hidden flex-shrink-0 cursor-pointer"
                    style={{ width: 'var(--header-btn-size)', height: 'var(--header-btn-size)' }}
                    onClick={() => openProfile({ name: localUser.name, image: localUser.image, accountId: localUser.accountId || '' })}
                  >
                    <img
                      src={localUser.image || "/default-avatar.png"}
                      alt={localUser.name}
                      className="w-full h-full object-cover"
                      loading="eager"
                      decoding="async"
                      onError={(e) => { (e.target as HTMLImageElement).src = "/default-avatar.png"; }}
                    />
                  </div>
                  <div className="flex-1 min-w-0 flex items-center gap-1.5">
                    <h4 className="text-xs font-semibold text-white truncate">{localUser.name || 'Owner'}</h4>
                    <HouseIcon size={14} className="shrink-0" />
                  </div>
                </div>

                {roomUsers.filter(u => String(u.accountId) !== String(roomOwnerId)).map((user) => (
                  <div key={user.accountId} className="flex items-center gap-2 px-1">
                    <div className="rounded-full overflow-hidden flex-shrink-0 cursor-pointer" style={{ width: 'var(--header-btn-size)', height: 'var(--header-btn-size)' }} onClick={() => openProfile({ name: user.name, image: user.image, accountId: user.accountId })}>
                      <img src={user.image || "/default-avatar.png"} alt={user.name} className="w-full h-full object-cover" loading="eager" decoding="async" onError={(e) => { (e.target as HTMLImageElement).src = "/default-avatar.png"; }} />
                    </div>
                    <div className="flex-1 min-w-0">
                      <h4 className="text-xs font-semibold text-white truncate">{user.name}</h4>
                      <p className="text-[10px] text-white/60">ID: {user.accountId}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* ROOM INFO SHEET — black 0.92, 15px corner, no lines */}
      {showRoomInfo && (
        <div className="fixed inset-0 z-[9999] flex items-end justify-center" style={{ paddingBottom: 'env(safe-area-inset-bottom)' }}>
          <div className="absolute inset-0 bg-black/30" onClick={() => setShowRoomInfo(false)} />
          <div
            className="relative w-full max-w-md animate-slide-up overflow-hidden flex flex-col"
            style={{
              height: '50vh',
              maxHeight: '50vh',
              background: 'rgba(0,0,0,0.92)',
              borderTopLeftRadius: '15px',
              borderTopRightRadius: '15px',
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center gap-3 px-4 pt-4 pb-2 flex-shrink-0">
              <button onClick={() => setRoomInfoTab('profile')} className="flex-1 flex flex-col items-center py-1 cursor-pointer">
                <span className={`text-sm font-semibold transition-colors ${roomInfoTab === 'profile' ? 'text-white' : 'text-white/50'}`}>Room info</span>
                {roomInfoTab === 'profile' && (
                  <svg width="20" height="6" viewBox="0 0 20 6" fill="none" style={{ marginTop: '2px' }}>
                    <path d="M 1 1 Q 10 7 19 1" stroke="#fff" strokeWidth="1.5" strokeLinecap="round" fill="none" />
                  </svg>
                )}
              </button>
              <button onClick={() => setRoomInfoTab('members')} className="flex-1 flex flex-col items-center py-1 cursor-pointer">
                <span className={`text-sm font-semibold transition-colors ${roomInfoTab === 'members' ? 'text-white' : 'text-white/50'}`}>Members</span>
                {roomInfoTab === 'members' && (
                  <svg width="20" height="6" viewBox="0 0 20 6" fill="none" style={{ marginTop: '2px' }}>
                    <path d="M 1 1 Q 10 7 19 1" stroke="#fff" strokeWidth="1.5" strokeLinecap="round" fill="none" />
                  </svg>
                )}
              </button>
            </div>

            <div className="flex-1 overflow-y-auto px-4 py-3">
              {roomInfoTab === 'profile' ? (
                <div className="space-y-3">
                  <div className="flex items-center gap-2">
                    <div className="rounded-md overflow-hidden border border-white/15 flex-shrink-0" style={{ width: '80px', height: '80px' }}>
                      <img src={roomDp} alt="Room" className="w-full h-full object-cover" loading="eager" decoding="async" />
                    </div>
                    <div className="min-w-0 flex-1">
                      <h3 className="font-semibold text-white text-sm truncate">{roomName || 'Room'}</h3>
                      <div className="flex items-center gap-1 text-[10px] text-white/60">
                        <span className="truncate">ID: {roomOwner.accountId}</span>
                        <button onClick={handleCopyId} className="p-0.5 hover:bg-white/10 rounded transition-colors cursor-pointer shrink-0" title="Copy ID">
                          <svg viewBox="0 0 24 24" className="fill-none stroke-white/70 stroke-[2] stroke-linecap-round stroke-linejoin-round" style={{ width: '12px', height: '12px' }}>
                            <rect x="9" y="9" width="13" height="13" rx="2" ry="2" /><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" />
                          </svg>
                        </button>
                        {copied && <span className="text-green-400 text-xs shrink-0">Copied!</span>}
                      </div>
                    </div>
                  </div>
                  <div><span className="text-[10px] text-white/50 font-medium">Host</span><p className="text-xs font-medium text-white mt-0.5">{localUser.name || "Unknown"}</p></div>
                  <div><span className="text-[10px] text-white/50 font-medium">Announcement:</span><p className="text-xs text-white/90 mt-0.5 leading-relaxed">{roomAnnouncement || '—'}</p></div>
                </div>
              ) : (
                <div className="space-y-2">
                  <div className="flex items-center gap-2 px-1">
                    <div className="rounded-full overflow-hidden flex-shrink-0 cursor-pointer" style={{ width: 'var(--header-btn-size)', height: 'var(--header-btn-size)' }} onClick={() => openProfile({ name: localUser.name, image: localUser.image, accountId: localUser.accountId || '' })}>
                      <img src={localUser.image || "/default-avatar.png"} alt={localUser.name} className="w-full h-full object-cover" loading="eager" decoding="async" onError={(e) => { (e.target as HTMLImageElement).src = "/default-avatar.png" }} />
                    </div>
                    <div className="flex-1 min-w-0 flex items-center gap-1">
                      <h4 className="text-xs font-medium text-white truncate">{localUser.name}</h4>
                      <span className="flex items-center justify-center flex-shrink-0" style={{ width: 'calc(var(--header-follow-btn-size) + 4px)', height: 'calc(var(--header-follow-btn-size) + 4px)' }}>
                        <HouseIcon size={26} style={{ width: '100%', height: '100%' }} />
                      </span>
                    </div>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {showUserProfile && profileUser && (
        <RoomProfile
          user={{
            name: profileUser.name, image: profileUser.image, accountId: profileUser.accountId,
            isInSeat: profileUser.isInSeat,
            isMuted: seats.find(s => s.user?.accountId === profileUser.accountId)?.isMuted || false,
            isLocked: seats.find(s => s.user?.accountId === profileUser.accountId)?.isLocked || false
          }}
          isCurrentUser={profileUser.accountId === userAccountId}
          isRoomOwner={isRoomOwner}
          onClose={() => setShowUserProfile(false)}
          onFollow={() => console.log('Follow clicked')}
          onMessage={() => { setShowUserProfile(false); setShowChatInput(true); setTimeout(() => { if (inputRef.current) inputRef.current.focus(); }, 200); }}
          onCopyId={() => console.log('Copy ID')}
          onMention={(username?: string) => { setShowUserProfile(false); setShowChatInput(true); setMessage(`@${username} `); setTimeout(() => { if (inputRef.current) inputRef.current.focus(); }, 200); }}
          onLeaveSeat={() => { if (profileUser) handleLeaveUserSeat(profileUser.accountId); setShowUserProfile(false); }}
          onMute={() => { if (profileUser) { const seat = seats.find(s => s.isOccupied && s.user?.accountId === profileUser.accountId); if (seat) { const nm = !seat.isMuted; setSeats(seats.map(s => s.number === seat.number ? { ...s, isMuted: nm } : s)); emitSeatAction("mute", seat.number, { isMuted: nm }); } } setShowUserProfile(false); }}
          onLock={() => { if (profileUser) { const seat = seats.find(s => s.isOccupied && s.user?.accountId === profileUser.accountId); if (seat) { const nl = !seat.isLocked; setSeats(seats.map(s => s.number === seat.number ? { ...s, isLocked: nl } : s)); emitSeatAction("lock", seat.number, { isLocked: nl }); } } setShowUserProfile(false); }}
          onKickOut={() => { if (profileUser) handleLeaveUserSeat(profileUser.accountId); setShowUserProfile(false); }}
        />
      )}

      {showExitMenu && (
        <div className="fixed inset-x-0 top-0 z-[9999]" style={{ height: '26vh' }}>
          <div className="fixed inset-0 bg-transparent" onClick={closeExitMenu} />
          <div
            className="relative w-full h-full flex items-center justify-center animate-top-sheet"
            style={{
              background: 'linear-gradient(to bottom, #000000 0%, #000000 70%, rgba(0,0,0,0.5) 90%, rgba(0,0,0,0) 100%)',
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-center gap-16 -mt-2">
              <div className="flex flex-col items-center gap-2">
                <button onClick={handleKeep} className="cursor-pointer active:scale-95 transition-transform" aria-label="Keep">
                  <img src="/IMG_20261002_113229.png" alt="Keep" className="object-contain select-none pointer-events-none" style={{ width: '67px', height: '67px' }} draggable={false} loading="eager" decoding="async" />
                </button>
                <span className="text-white font-semibold" style={{ fontSize: 'var(--exit-text-size)' }}>Keep</span>
              </div>
              <div className="flex flex-col items-center gap-2">
                <button onClick={handleExit} className="cursor-pointer active:scale-95 transition-transform" aria-label="Exit">
                  <img src="/IMG_20261002_113213.png" alt="Exit" className="object-contain select-none pointer-events-none" style={{ width: '67px', height: '67px' }} draggable={false} loading="eager" decoding="async" />
                </button>
                <span className="text-white font-semibold" style={{ fontSize: 'var(--exit-text-size)' }}>Exit</span>
              </div>
            </div>
          </div>
        </div>
      )}

      {showSeatSheet && selectedSeat !== null && (
        <div className="fixed inset-0 z-[9999] flex items-end justify-center" style={{ paddingBottom: 'env(safe-area-inset-bottom)' }}>
          <div className="absolute inset-0 bg-black/30" onClick={closeBottomSheet} />
          <div className="relative bg-white/95 backdrop-blur-xl w-full max-w-md rounded-t-3xl shadow-2xl px-4 py-3 animate-slide-up max-h-[30vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
            <div className="space-y-1">
              {!isSelectedSeatTakenByOther && !isSelectedSeatMySeat && (
                <button onClick={handleTakeSeat} disabled={selectedSeatData?.isLocked && !selectedSeatData?.isOccupied} className="w-full py-2 text-black font-medium text-sm hover:bg-gray-100 rounded-lg transition-colors disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer">Take Mic</button>
              )}
              {isSelectedSeatMySeat && <button onClick={handleLeaveSeat} className="w-full py-2 text-black font-medium text-sm hover:bg-gray-100 rounded-lg transition-colors cursor-pointer">Leave Seat</button>}
              <button onClick={handleToggleMute} className="w-full py-2 text-black font-medium text-sm hover:bg-gray-100 rounded-lg transition-colors cursor-pointer">{selectedSeatData?.isMuted ? 'Unmute Seat' : 'Mute Seat'}</button>
              <button onClick={handleToggleLock} className="w-full py-2 text-black font-medium text-sm hover:bg-gray-100 rounded-lg transition-colors cursor-pointer">{selectedSeatData?.isLocked ? 'Unlock Mic' : 'Lock Mic'}</button>
              <button onClick={handleInvite} className="w-full py-2 text-black font-medium text-sm hover:bg-gray-100 rounded-lg transition-colors cursor-pointer">Invite</button>
            </div>
          </div>
        </div>
      )}

      {fullImageModal && (
        <div className="fixed inset-0 z-[10000] bg-black/90 flex items-center justify-center p-4 cursor-pointer" onClick={() => setFullImageModal(null)}>
          <div className="relative max-w-full max-h-full">
            <img src={fullImageModal} alt="Full preview" className="max-w-full max-h-[85vh] object-contain rounded-lg" loading="eager" decoding="async" />
            <button onClick={() => setFullImageModal(null)} className="absolute -top-8 right-0 text-white bg-white/20 rounded-full p-1.5 hover:bg-white/40">
              <svg viewBox="0 0 24 24" className="fill-none stroke-white stroke-[2.5]" style={{ width: 'var(--header-icon-size)', height: 'var(--header-icon-size)' }}><line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" /></svg>
            </button>
          </div>
        </div>
      )}

      {showMessageSheet && (
        <div className="fixed inset-0 z-[9999] flex items-end justify-center" style={{ paddingBottom: 'env(safe-area-inset-bottom)' }}>
          <div className="absolute inset-0 bg-black/30" onClick={() => setShowMessageSheet(false)} />
          <div className="relative bg-white w-full max-w-md shadow-2xl animate-slide-up overflow-hidden" style={{ height: '60vh', borderTopLeftRadius: '15px', borderTopRightRadius: '15px' }} onClick={(e) => e.stopPropagation()}>
            <div className="h-full overflow-y-auto">
              <MessagePage sharedRoomData={{ roomId: roomId, roomName: roomName || "Room", roomDp: roomDp || "/default-avatar.png" }} />
            </div>
          </div>
        </div>
      )}

      {showFourGride && (
        <Fourgride
          onClose={() => setShowFourGride(false)}
          onClearChat={() => { setMessages([]); socket.emit("room_chat_clear", { roomId, userId: userAccountId, timestamp: Date.now() }); }}
          publicMsgOff={publicMsgOff}
          onTogglePublicMsg={() => setPublicMsgOff(prev => !prev)}
          speaker={isSpeakerOn}
          onToggleSpeaker={() => setIsSpeakerOn(prev => !prev)}
          onOpenStore={(view) => { setStoreInitialView(view); setShowStore(true); setShowFourGride(false); }}
          onMusicPlay={(track) => {
            const idb = indexedDB.open('HurryMusicDB', 1);
            idb.onsuccess = () => {
              const request = idb.result.transaction('music', 'readonly').objectStore('music').getAll();
              request.onsuccess = () => {
                const allTracks = request.result.map((item: any) => ({ id: item.id, name: item.name, url: URL.createObjectURL(item.blob) }));
                handlePlayMusic(track, allTracks);
              };
            };
          }}
        />
      )}

      {showGameSheet && (
        <div className="fixed inset-0 z-[9999] flex items-end justify-center" style={{ paddingBottom: 'env(safe-area-inset-bottom)' }}>
          <div className="absolute inset-0 bg-black/30" onClick={() => setShowGameSheet(false)} />
          <div
            className="relative w-full max-w-md px-4 pt-3 pb-6 animate-slide-up"
            style={{
              height: '20vh',
              background: 'rgba(0,0,0,0.92)',
              borderTopLeftRadius: '15px',
              borderTopRightRadius: '15px',
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-start mb-3">
              <h2 className="text-base font-bold text-white">Games</h2>
            </div>
            <div className="flex items-start gap-5">
              <div className="flex flex-col items-center">
                <button onClick={() => { setShowGameSheet(false); setShowWildParty(true); }} className="transition-transform hover:scale-105">
                  <img src="/file_000000009d808211b8ffb7c2183b4ef5.png" alt="Wild party" className="w-14 h-14 object-contain" loading="eager" decoding="async" />
                </button>
                <span className="text-[10px] text-white/90 mt-1 whitespace-nowrap">Wild party</span>
              </div>
              <div className="flex flex-col items-center">
                <button onClick={() => { setShowGameSheet(false); setShowFruitParty(true); }} className="transition-transform hover:scale-105">
                  <img src="/file_00000000c9e08211ab3e8c4d511b7965.png" alt="Fruit party" className="w-12 h-12 object-contain rounded-md" loading="eager" decoding="async" />
                </button>
                <span className="text-[10px] text-white/90 mt-1 whitespace-nowrap">Fruit party</span>
              </div>
            </div>
          </div>
        </div>
      )}

      {showWildParty === true && <Wildparty onClose={() => setShowWildParty(false)} onMinimize={() => setShowWildParty('minimized')} />}
      {showFruitParty === true && <Fruitparty onClose={() => setShowFruitParty(false)} onMinimize={() => setShowFruitParty('minimized')} />}

      {showRoomTask && <div className="absolute inset-0 z-[11000] pointer-events-none"><Roomtask onBack={() => setShowRoomTask(false)} /></div>}
      {showCupIcon && <div className="absolute inset-0 z-[11000] pointer-events-none"><CupIcon onBack={() => setShowCupIcon(false)} count={cupCount} /></div>}

      {musicControllerState === 'full' && currentTrack && !showFourGride && (
        <div className="fixed left-1/2 transform -translate-x-1/2 z-[45] w-full max-w-sm px-3" style={{ bottom: 'var(--music-controller-bottom)' }} onClick={(e) => e.stopPropagation()}>
          <div className="relative rounded-md overflow-hidden bg-black/90 backdrop-blur-md border border-white/10" style={{ padding: 'var(--music-padding)', boxShadow: '0 8px 32px rgba(0,0,0,0.6)' }}>
            <button onClick={handleCloseMusicController} className="absolute top-1.5 left-1.5 p-1 rounded-full bg-white/10 hover:bg-white/20 transition-colors cursor-pointer z-10" aria-label="Close music controller">
              <svg viewBox="0 0 24 24" className="fill-none stroke-white stroke-[2] stroke-linecap-round stroke-linejoin-round" style={{ width: 'var(--music-icon-size)', height: 'var(--music-icon-size)' }}><line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" /></svg>
            </button>
            <button onClick={() => setMusicControllerState('minimized')} className="absolute top-1.5 right-1.5 p-1 rounded-full bg-white/10 hover:bg-white/20 transition-colors cursor-pointer z-10" aria-label="Minimize music controller">
              <svg viewBox="0 0 24 24" className="fill-none stroke-white stroke-[2.2] stroke-linecap-round stroke-linejoin-round" style={{ width: 'var(--music-icon-size)', height: 'var(--music-icon-size)' }}><polyline points="6 9 12 15 18 9" /></svg>
            </button>
            <div className="text-center mb-1.5 mt-4"><p className="text-white font-semibold truncate px-8" style={{ fontSize: 'var(--music-title-size)' }}>{currentTrack.name}</p></div>
            <div className="px-1 mb-1.5">
              <input type="range" min="0" max={musicDuration || 0} step="0.1" value={musicCurrentTime} onChange={handleProgressChange} className="w-full h-1 rounded-lg appearance-none cursor-pointer music-progress-slider" style={{ background: `linear-gradient(to right, #3b82f6 ${musicDuration ? (musicCurrentTime / musicDuration) * 100 : 0}%, rgba(255,255,255,0.3) ${musicDuration ? (musicCurrentTime / musicDuration) * 100 : 0}%)` }} />
              <div className="flex justify-between text-white/60 mt-0.5" style={{ fontSize: 'var(--music-time-size)' }}><span>{formatTime(musicCurrentTime)}</span><span>{formatTime(musicDuration)}</span></div>
            </div>
            <div className="flex items-center justify-center gap-4 mb-1.5">
              <button onClick={handlePrevTrack} className="p-1.5 rounded-full hover:bg-white/10 transition-colors cursor-pointer">
                <svg viewBox="0 0 24 24" className="fill-white" style={{ width: 'var(--music-control-size)', height: 'var(--music-control-size)' }}><path d="M6 6h2v12H6zm3.5 6l8.5 6V6z" /></svg>
              </button>
              <button onClick={handleToggleMusicPlay} className="p-2 rounded-full bg-white/10 hover:bg-white/20 transition-colors cursor-pointer">
                {isMusicPlaying ? (
                  <svg viewBox="0 0 24 24" className="fill-white" style={{ width: 'var(--music-play-size)', height: 'var(--music-play-size)' }}><rect x="6" y="4" width="4" height="16" rx="1" /><rect x="14" y="4" width="4" height="16" rx="1" /></svg>
                ) : (
                  <svg viewBox="0 0 24 24" className="fill-white" style={{ width: 'var(--music-play-size)', height: 'var(--music-play-size)' }}><polygon points="5 3 19 12 5 21 5 3" /></svg>
                )}
              </button>
              <button onClick={handleNextTrack} className="p-1.5 rounded-full hover:bg-white/10 transition-colors cursor-pointer">
                <svg viewBox="0 0 24 24" className="fill-white" style={{ width: 'var(--music-control-size)', height: 'var(--music-control-size)' }}><path d="M16 6h2v12h-2zm-2.5 6l-8.5 6V6z" /></svg>
              </button>
            </div>
            <div className="flex items-center gap-1.5 px-1">
              <svg viewBox="0 0 24 24" className="fill-white shrink-0" style={{ width: 'var(--music-icon-size)', height: 'var(--music-icon-size)' }}><path d="M3 9v6h4l5 5V4L7 9H3z" /></svg>
              <input type="range" min="0" max="1" step="0.01" value={musicVolume} onChange={handleVolumeChange} className="flex-1 h-1.5 rounded-lg appearance-none cursor-pointer music-volume-slider" style={{ background: `linear-gradient(to right, #3b82f6 ${musicVolume * 100}%, rgba(255,255,255,0.3) ${musicVolume * 100}%)` }} />
              <span className="text-white font-semibold text-right" style={{ fontSize: 'var(--music-time-size)', width: 'var(--music-volume-width)' }}>{Math.round(musicVolume * 100)}%</span>
            </div>
          </div>
        </div>
      )}

      <EntryToastStack toasts={entryToasts} />

      <style jsx global>{`
        :root {
          --seat-size: 56px; --header-btn-size: 42px; --header-btn-padding: 4px 8px; --header-icon-size: 26px;
          --header-room-img-size: 44px; --header-room-name-size: 16px; --header-id-size: 11px;
          --header-follow-btn-size: 22px; --header-follow-icon-size: 14px; --header-count-size: 11px;
          --footer-btn-size: 47px; --footer-icon-size: 30px; --footer-input-text: 13px;
          --announcement-text-size: 13px; --msg-avatar-size: 26px; --msg-name-size: 13px;
          --msg-text-size: 13px; --msg-jointime-size: 11px; --exit-btn-size: 67px;
          --exit-icon-size: 24px; --exit-text-size: 14px; --music-controller-bottom: 8vh;
          --music-padding: 10px; --music-icon-size: 16px; --music-title-size: 13px;
          --music-time-size: 9px; --music-control-size: 20px; --music-play-size: 24px; --music-volume-width: 28px;
        }
        @media (max-width: 400px) {
          :root {
            --seat-size: 56px; --header-btn-size: 38px; --header-icon-size: 22px;
            --header-room-img-size: 38px; --header-room-name-size: 15px;
            --footer-btn-size: 42px; --footer-icon-size: 26px;
          }
        }
        .music-volume-slider, .music-progress-slider { -webkit-appearance: none; appearance: none; }
        .music-volume-slider::-webkit-slider-thumb, .music-progress-slider::-webkit-slider-thumb { -webkit-appearance: none; appearance: none; width: 0px; height: 0px; background: transparent; }
        .music-volume-slider::-moz-range-thumb, .music-progress-slider::-moz-range-thumb { width: 0px; height: 0px; background: transparent; border: none; }
        .music-volume-slider::-webkit-slider-runnable-track { height: 8px; border-radius: 4px; }
        .music-volume-slider::-moz-range-track { height: 8px; border-radius: 4px; }
        .music-progress-slider::-webkit-slider-runnable-track { height: 6px; border-radius: 3px; }
        .music-progress-slider::-moz-range-track { height: 6px; border-radius: 3px; }
        @keyframes rotate-slow { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
        .music-minimize-icon { animation: rotate-slow 4s linear infinite; }
        @keyframes slideUp { from { transform: translateY(100%); } to { transform: translateY(0); } }
        .animate-slide-up { animation: slideUp 0.3s ease-out; }
        @keyframes slideDown { from { transform: translateY(-100%); opacity: 0; } to { transform: translateY(0); opacity: 1; } }
        .animate-top-sheet { animation: slideDown 0.28s ease-out; }
        @keyframes waveBehind { 0% { transform: translate(-50%, -50%) scale(0.85); opacity: 0.9; } 50% { transform: translate(-50%, -50%) scale(1.35); opacity: 0.4; } 100% { transform: translate(-50%, -50%) scale(1.6); opacity: 0; } }
        @keyframes voicePulse { 0%, 100% { transform: translate(-50%, -50%) scale(1); } 50% { transform: translate(-50%, -50%) scale(1.08); } }
        .wave-ripple { animation: waveBehind 1.2s ease-out infinite; }
        .wave-ripple-delayed { animation: waveBehind 1.2s ease-out 0.4s infinite; }
        .scrollbar-none { -ms-overflow-style: none; scrollbar-width: none; }
        .scrollbar-none::-webkit-scrollbar { display: none; }
        .marquee-inner { animation: marquee-scroll 14s linear infinite; }
        @keyframes marquee-scroll { from { transform: translateX(0); } to { transform: translateX(-50%); } }
        .entry-toast { animation: entryToastIn 0.55s cubic-bezier(0.22, 1, 0.36, 1); }
        @keyframes entryToastIn {
          from { transform: translateX(100vw); opacity: 0; }
          to   { transform: translateX(0);    opacity: 1; }
        }
      `}</style>

      <LuckyGiftAnimation roomId={String(roomId)} />
      {showEmojiPicker && <EmojiPicker onClose={() => setShowEmojiPicker(false)} onSelectEmoji={handleSeatEmoji} />}
      <GiftPicker
        open={showGiftPicker}
        onClose={() => setShowGiftPicker(false)}
        seats={seats}
        roomId={roomId}
        currentUserAccountId={userAccountId}
        currentUserName={currentUser.name}
        currentUserImage={currentUser.image}
        roomUsers={roomUsers}
        onSend={(count: number) => setCupCount((prev) => prev + count)}
      />
    </div>
  );
}

function SeatItemBase({ seatNumber, seatData, onClick, onAvatarClick, accountId, roomOwnerId }: {
  seatNumber: number; seatData?: Seat; onClick: (e: React.MouseEvent) => void;
  onAvatarClick?: (e: React.MouseEvent) => void; accountId: string; roomOwnerId: string;
}) {
  const isLocked = seatData?.isLocked ?? false;
  const isOccupied = seatData?.isOccupied ?? false;
  const isSpeaking = seatData?.isSpeaking ?? false;
  const isMuted = seatData?.isMuted ?? false;
  const user = seatData?.user;
  const isRoomOwnerSeat = isOccupied && user?.accountId === roomOwnerId;
  const gif = seatData?.gif;

  const isUserSpeaking = React.useMemo(() => {
    if (!isOccupied || isMuted) return false;
    return isSpeaking;
  }, [isOccupied, isMuted, isSpeaking]);

  const activeSpeaking = isSpeaking || isUserSpeaking;

  return (
    <div className="relative flex flex-col items-center cursor-pointer select-none" onClick={onClick}>
      {seatNumber === 1 && (
        <div className="absolute pointer-events-none hidden sm:flex bg-black/40 backdrop-blur-md border border-white/20 px-2 py-1 rounded-full shadow-lg" style={{ left: '-100px', top: '-15px', zIndex: 40, alignItems: 'center', gap: '6px' }}>
          <div className="relative overflow-visible" style={{ width: 'calc(var(--seat-size) * 0.33)', height: 'calc(var(--seat-size) * 0.33)', flexShrink: 0 }}>
            <WhiteColorRemovalShader imageSrc="/1787158869902.png" threshold={0.85} className="w-full h-full" style={{ width: '100%', height: '100%', objectFit: 'contain', maxWidth: 'none', maxHeight: 'none', pointerEvents: 'none' }} />
          </div>
          <span className="font-bold whitespace-nowrap" style={{ color: '#FFD700', textShadow: '0 0 4px rgba(255,215,0,0.8), 0 0 8px rgba(255,215,0,0.5)', letterSpacing: '0.2px', fontSize: 'calc(var(--seat-size) * 0.15)' }}>500K</span>
        </div>
      )}

      <div className="relative overflow-visible">
        {activeSpeaking && (
          <>
            <div className="absolute rounded-full bg-blue-400 wave-ripple pointer-events-none" style={{ width: 'var(--seat-size)', height: 'var(--seat-size)', left: '50%', top: '50%', zIndex: 0 }} />
            <div className="absolute rounded-full bg-blue-500 wave-ripple-delayed pointer-events-none" style={{ width: 'var(--seat-size)', height: 'var(--seat-size)', left: '50%', top: '50%', zIndex: 0 }} />
            <div className="absolute rounded-full pointer-events-none" style={{ width: 'calc(var(--seat-size) * 1.066)', height: 'calc(var(--seat-size) * 1.066)', left: '50%', top: '50%', zIndex: 0, backgroundColor: 'rgba(59, 130, 246, 0.35)', filter: 'blur(6px)', animation: 'voicePulse 1.2s ease-in-out infinite' }} />
          </>
        )}

        <div className={`relative w-[var(--seat-size)] h-[var(--seat-size)] flex items-center justify-center shrink-0 z-10 transition-all duration-300 hover:scale-105 pointer-events-auto overflow-visible ${activeSpeaking ? 'drop-shadow-[0_0_12px_rgba(85,198,243,0.9)]' : ''}`}>
          <div className="absolute inset-0 pointer-events-none z-0">
            {isLocked ? <SeatBubbleIconGrey /> : <SeatBubbleIconBlue />}
          </div>

          <div className="relative rounded-full overflow-hidden flex items-center justify-center z-10 pointer-events-auto" style={{ width: '91%', height: '91%' }}>
            {isLocked ? (
              <img src="/file_0000000015a48211b000ee447a786f7c.png" alt="Locked" className="object-contain select-none pointer-events-none" style={{ width: '62%', height: '62%' }} draggable={false} loading="eager" decoding="async" />
            ) : isOccupied && user ? (
              <img src={user.image || "/default-avatar.png"} alt={user.name} data-hurry-seat={seatNumber} data-hurry-account={user.accountId} className="w-full h-full object-cover select-none pointer-events-auto cursor-pointer" draggable={false} loading="eager" decoding="async" onError={(e) => { (e.target as HTMLImageElement).src = "/default-avatar.png"; }} onClick={onAvatarClick} />
            ) : (
              <img src="/file_000000003e7482309b7f6e7f2a922160.png" alt="Empty Seat" className="object-contain select-none pointer-events-none" style={{ width: '65%', height: '65%' }} draggable={false} loading="eager" decoding="async" />
            )}
          </div>

          {gif && (
            <div className="absolute pointer-events-none overflow-visible flex items-center justify-center z-30" style={{ top: '50%', left: '50%', transform: 'translate(-50%, -50%)', width: '100%', height: '100%' }}>
              <img key={`${gif.src}-${gif.timestamp}`} src={`${encodeURI(gif.src)}?t=${gif.timestamp}`} alt="Reaction" className="w-full h-full object-contain select-none pointer-events-none" style={{ maxWidth: 'none', maxHeight: 'none' }} loading="eager" decoding="async" />
            </div>
          )}

          {isMuted && (
            <div className="absolute right-0.5 bottom-1.5 rounded-full bg-red-500 flex items-center justify-center shadow-md z-30" style={{ width: 'calc(var(--seat-size) * 0.24)', height: 'calc(var(--seat-size) * 0.24)' }}>
              <svg viewBox="0 0 24 24" className="fill-none stroke-white stroke-[3] stroke-linecap-round stroke-linejoin-round" style={{ width: 'calc(var(--seat-size) * 0.15)', height: 'calc(var(--seat-size) * 0.15)' }}>
                <line x1="1" y1="1" x2="23" y2="23" /><path d="M9 9v3a3 3 0 0 0 5.12 2.12M15 9.34V4a3 3 0 0 0-5.94-.6" /><path d="M17 16.95A7 7 0 0 1 5 12v-2m14 0v2a7 7 0 0 1-.11 1.23" />
              </svg>
            </div>
          )}
        </div>
      </div>

      <span
        className="font-medium text-white/90 pointer-events-none flex flex-nowrap items-center justify-center gap-0.5 leading-none text-center w-[var(--seat-size)] overflow-hidden mt-0.5 shrink-0"
        style={{
          fontSize: 'calc(var(--seat-size) * 0.16)',
          height: 'calc(var(--seat-size) * 0.22)',
          lineHeight: 'calc(var(--seat-size) * 0.22)',
        }}
      >
        {isRoomOwnerSeat && (
          <span className="flex items-center justify-center shrink-0 inline-flex" style={{ width: 'calc(var(--seat-size) * 0.20)', height: 'calc(var(--seat-size) * 0.20)' }}>
            <HouseIcon size={22} style={{ width: '100%', height: '100%' }} />
          </span>
        )}
        <span className="truncate min-w-0 whitespace-nowrap">
          {isLocked ? `${seatNumber}` : (isOccupied && user ? user.name : `${seatNumber}`)}
        </span>
      </span>
    </div>
  );
}

const MemoSeatItem = memo(SeatItemBase, (prev, next) => {
  if (prev.seatNumber !== next.seatNumber) return false;
  if (prev.accountId !== next.accountId) return false;
  if (prev.roomOwnerId !== next.roomOwnerId) return false;
  const a = prev.seatData;
  const b = next.seatData;
  if (a === b) return true;
  if (!a || !b) return false;
  if (a.isOccupied !== b.isOccupied) return false;
  if (a.isLocked !== b.isLocked) return false;
  if (a.isMuted !== b.isMuted) return false;
  if (a.isSpeaking !== b.isSpeaking) return false;
  if (a.user?.accountId !== b.user?.accountId) return false;
  if (a.user?.image !== b.user?.image) return false;
  if (a.user?.name !== b.user?.name) return false;
  if (a.gif?.timestamp !== b.gif?.timestamp) return false;
  return true;
});

const RoomSideBanner = memo(function RoomSideBanner() {
  const bannerImages = ['/IMG_20261018_165235.png', '/1788339681679~2.jpg'];
  const [currentIndex, setCurrentIndex] = useState(0);
  useEffect(() => {
    const timer = setInterval(() => setCurrentIndex((prev) => (prev + 1) % bannerImages.length), 3000);
    return () => clearInterval(timer);
  }, [bannerImages.length]);
  return (
    <div className="flex flex-col items-center select-none">
      <div className="relative overflow-hidden shadow-lg" style={{ width: '60px', height: '84px', borderRadius: '8px' }}>
        <div className="flex h-full transition-transform duration-500 ease-out" style={{ transform: `translateX(-${currentIndex * 100}%)` }}>
          {bannerImages.map((src, i) => (<img key={i} src={src} alt={`Banner ${i + 1}`} className="w-full h-full object-cover flex-shrink-0 select-none pointer-events-none" draggable={false} loading="eager" decoding="async" />))}
        </div>
      </div>
      <div className="flex items-center justify-center gap-[2px] mt-1" style={{ height: '8px' }}>
        {bannerImages.map((_, i) => (<div key={i} className={`transition-all duration-300 rounded-md ${currentIndex === i ? 'w-1.5 h-1.5 bg-white shadow-[0_0_4px_rgba(255,255,255,0.8)]' : 'w-1 h-1 bg-white/40'}`} />))}
      </div>
    </div>
  );
});

const GreenColorRemovalShader = memo(function GreenColorRemovalShader({ imageSrc, threshold = 0.5, className = "", style = {} }: any) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d', { willReadFrequently: true });
    if (!ctx) return;
    const img = new Image();
    img.crossOrigin = "anonymous";
    img.onload = () => {
      canvas.width = img.width; canvas.height = img.height;
      ctx.drawImage(img, 0, 0);
      const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
      const data = imageData.data;
      for (let i = 0; i < data.length; i += 4) {
        const r = data[i]; const g = data[i + 1]; const b = data[i + 2];
        if (g > r * 1.2 && g > b * 1.2 && g > 70) data[i + 3] = 0;
      }
      ctx.putImageData(imageData, 0, 0);
    };
    img.src = imageSrc;
  }, [imageSrc, threshold]);
  return <canvas ref={canvasRef} className={className} style={style} />;
});
