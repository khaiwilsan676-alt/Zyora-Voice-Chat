"use client";

import React, { useState, useEffect, useRef, useMemo } from "react";
import { ChevronUp, ChevronRight } from "lucide-react";
import Image from "next/image";
import socket from "../src/lib/socket";
import Wallet from "./Wallet";

const SHARED_DB = "FruitPartyDB";
const SHARED_STORE = "GameState";
const DEFAULT_BALANCE = 0;

let dbPromise: Promise<IDBDatabase> | null = null;

const initWalletDB = (): Promise<IDBDatabase> => {
  if (typeof window === "undefined") return Promise.reject(new Error("No window"));
  if (dbPromise) return dbPromise;

  dbPromise = new Promise<IDBDatabase>((resolve, reject) => {
    const request = indexedDB.open(SHARED_DB, 3);
    request.onupgradeneeded = (e: any) => {
      const db = e.target.result;
      if (!db.objectStoreNames.contains(SHARED_STORE)) {
        db.createObjectStore(SHARED_STORE);
      }
      if (!db.objectStoreNames.contains("transactions")) {
        db.createObjectStore("transactions", { keyPath: "id", autoIncrement: true });
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
};

const loadWalletBalance = async (): Promise<number> => {
  try {
    const db = await initWalletDB();
    return new Promise((resolve) => {
      const tx = db.transaction(SHARED_STORE, "readonly");
      const req = tx.objectStore(SHARED_STORE).get("user_data");
      req.onsuccess = () => {
        if (req.result && typeof req.result.balance === "number") {
          resolve(req.result.balance);
        } else resolve(DEFAULT_BALANCE);
      };
      req.onerror = () => resolve(DEFAULT_BALANCE);
    });
  } catch {
    return DEFAULT_BALANCE;
  }
};

const updateWalletBalance = async (delta: number): Promise<void> => {
  try {
    const db = await initWalletDB();
    return new Promise((resolve, reject) => {
      const tx = db.transaction(SHARED_STORE, "readwrite");
      const store = tx.objectStore(SHARED_STORE);
      const req = store.get("user_data");
      req.onsuccess = () => {
        const data = req.result;
        const current = data?.balance ?? DEFAULT_BALANCE;
        const next = Math.max(0, current + delta);
        const putReq = store.put({ ...(data || {}), balance: next }, "user_data");
        putReq.onsuccess = () => resolve();
        putReq.onerror = () => reject(putReq.error);
      };
      req.onerror = () => reject(req.error);
    });
  } catch (e) {
    console.error("Wallet update failed", e);
  }
};

const addDiamondsToLocalDB = async (amount: number): Promise<void> => {
  try {
    const db = await initWalletDB();
    return new Promise<void>((resolve, reject) => {
      const tx = db.transaction(SHARED_STORE, "readwrite");
      const store = tx.objectStore(SHARED_STORE);
      const req = store.get("user_data");
      req.onsuccess = () => {
        const data = req.result || {};
        const current = Number(data.diamonds) || 0;
        const putReq = store.put({ ...data, diamonds: current + amount }, "user_data");
        putReq.onsuccess = () => resolve();
        putReq.onerror = () => reject(putReq.error);
      };
      req.onerror = () => reject(req.error);
    });
  } catch (e) {
    console.error("Local diamond add failed", e);
  }
};

const recordGiftTransaction = async (title: string, amount: number): Promise<void> => {
  try {
    const db = await initWalletDB();
    return new Promise<void>((resolve, reject) => {
      const tx = db.transaction("transactions", "readwrite");
      const store = tx.objectStore("transactions");

      const now = new Date();
      const dateStr = `${now.getFullYear()}.${String(now.getMonth() + 1).padStart(2, "0")}.${String(now.getDate()).padStart(2, "0")} ${String(now.getHours()).padStart(2, "0")}:${String(now.getMinutes()).padStart(2, "0")}`;

      const record = {
        title,
        amount,
        date: dateStr,
        timestamp: now.getTime(),
        type: "coin",
      };

      const req = store.add(record);
      req.onsuccess = () => resolve();
      req.onerror = () => reject(req.error);
    });
  } catch (e) {
    console.error("Failed to record gift transaction", e);
  }
};

const recordDiamondTransaction = async (title: string, amount: number): Promise<void> => {
  try {
    const db = await initWalletDB();
    return new Promise<void>((resolve, reject) => {
      const tx = db.transaction("transactions", "readwrite");
      const store = tx.objectStore("transactions");

      const now = new Date();
      const dateStr = `${now.getFullYear()}.${String(now.getMonth() + 1).padStart(2, "0")}.${String(now.getDate()).padStart(2, "0")} ${String(now.getHours()).padStart(2, "0")}:${String(now.getMinutes()).padStart(2, "0")}`;

      const record = {
        title,
        amount,
        date: dateStr,
        timestamp: now.getTime(),
        type: "diamond",
      };

      const req = store.add(record);
      req.onsuccess = () => resolve();
      req.onerror = () => reject(req.error);
    });
  } catch (e) {
    console.error("Failed to record diamond transaction", e);
  }
};

const SolidMicIcon = ({ className }: { className?: string }) => (
  <svg viewBox="0 0 24 24" className={className} fill="currentColor" stroke="none">
    <path d="M12 14c1.66 0 3-1.34 3-3V5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3zm5.91-3c-.49 0-.9.39-.9.88 0 2.76-2.24 5-5 5s-5-2.24-5-5c0-.49-.41-.88-.9-.88s-.9.39-.9.88c0 3.66 2.85 6.66 6.4 7.08V22h1.8v-2.92c3.55-.42 6.4-3.42 6.4-7.08 0-.49-.41-.88-.9-.88z" />
  </svg>
);

const HoundIcon = ({ className }: { className?: string }) => {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 512 512"
      className={className}
      fill="currentColor"
      stroke="none"
    >
      <path
        fill="currentColor"
        fillRule="evenodd"
        d="
          M 80 235
          L 222 105
          Q 256 75 290 105
          L 432 235
          Q 445 247 432 260
          L 405 290
          L 405 405
          Q 405 425 385 425
          L 127 425
          Q 107 425 107 405
          L 107 290
          L 80 260
          Q 67 247 80 235
          Z

          M 210 425
          L 210 300
          Q 210 280 230 280
          L 282 280
          Q 302 280 302 300
          L 302 425
          Z
        "
      />
    </svg>
  );
};

export interface Gift {
  id: number;
  name: string;
  coins: number;
  image: string;
  video?: string;
  videoStyle?: "fade" | "pure";
  noMask?: boolean;
  sideFade?: boolean;
}

interface Seat {
  number: number;
  isOccupied: boolean;
  user?: { name: string; image: string; accountId: string };
}

type RecipientMode = "mic" | "room" | "single";

function rollLuckyWin(): number {
  const luck = Math.random();
  if (luck > 0.999) return 30;
  if (luck > 0.980) return 15;
  if (luck > 0.966) return 10;
  if (luck > 0.900) return 5;
  if (luck > 0.910) return 2;
  return 0;
}

export default function GiftPicker({
  open = true,
  onClose,
  seats = [],
  onSend,
  roomId,
  currentUserAccountId,
  currentUserName = "User",
  currentUserImage = "/default-avatar.png",
  roomUsers = [],
}: {
  open?: boolean;
  onClose: () => void;
  seats?: Seat[];
  onSend?: (value: number) => void;
  roomId?: string;
  currentUserAccountId?: string;
  currentUserName?: string;
  currentUserImage?: string;
  roomUsers?: Array<{ accountId: string; name: string; image: string }>;
}) {
  const [activeTab, setActiveTab] = useState("Hot");
  const [selectedMultiplier, setSelectedMultiplier] = useState("1×");
  const [showMultipliers, setShowMultipliers] = useState(false);
  const [selectedGift, setSelectedGift] = useState<number | null>(null);
  const [walletBalance, setWalletBalance] = useState<number>(0);
  const [sending, setSending] = useState(false);
  const [isWalletOpen, setIsWalletOpen] = useState(false);

  const [showInsufficient, setShowInsufficient] = useState(false);
  const [insufficientText, setInsufficientText] = useState("Insufficient Balance");
  const insufficientTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const walletOpenTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const touchStartXRef = useRef<number | null>(null);
  const touchStartYRef = useRef<number | null>(null);

  const seatsKey = useMemo(
    () => (Array.isArray(seats) ? seats.map((s) => `${s.number}:${s.user?.accountId || ""}`).join("|") : ""),
    [seats]
  );

  useEffect(() => {
    if (!currentUserAccountId) return

    const handleReceivedGift = (data: any = {}) => {
      const recipientIds = Array.isArray(data.recipientIds) ? data.recipientIds.map(String) : []
      if (!recipientIds.includes(String(currentUserAccountId))) return

      const image = String(data.giftImage || data.luckyImage || "")
      if (!image) return

      const transferId = String(data.transferId || ("gift-" + Date.now() + "-" + Math.random().toString(36).slice(2)))
      const name = String(data.giftName || "Gift")
      const aliases = Array.from(new Set([
        String(currentUserAccountId),
        localStorage.getItem("userUID") || "",
        localStorage.getItem("userPhone") || "",
        localStorage.getItem("userId") || "",
        localStorage.getItem("accountNumber") || "",
      ].filter(Boolean)))

      for (const uid of aliases) {
        const key = "receivedGifts_" + uid
        let gifts: Array<{ id: string; name: string; image: string }> = []
        try {
          const stored = localStorage.getItem(key)
          const parsed = stored ? JSON.parse(stored) : []
          if (Array.isArray(parsed)) gifts = parsed
        } catch {}

        if (gifts.some((gift) => gift.id === transferId)) continue

        gifts.unshift({ id: transferId, name, image })
        localStorage.setItem(key, JSON.stringify(gifts.slice(0, 50)))
      }

      window.dispatchEvent(new CustomEvent("hurry:received-gifts-updated"))
    }

    socket.on("coin_transfer_received", handleReceivedGift)
    return () => socket.off("coin_transfer_received", handleReceivedGift)
  }, [currentUserAccountId])

  useEffect(() => {
    const originalEmit = socket.emit.bind(socket);
    const patchedEmit = ((event: string, ...args: any[]) => {
      if (event !== "coin_transfer" || !args[0]) return originalEmit(event, ...args);
      const data = { ...args[0] };
      const luckyImages: Record<string, string> = {
        Tiara: "/IMG_20260927_213855.png",
        "Lucky Clover": "/IMG_20260927_213917.png",
        Hi: "/IMG_20260927_213946.png",
        Rose: "/IMG_20260927_214121.png",
        Kiss: "/IMG_20260927_214139.png",
        Balloon: "/IMG_20260927_214220.png",
        Dragon: "/IMG_20260927_221521.png",
        "Nine Hands": "/IMG_20260927_221544.png",
        Coffin: "/IMG_20260927_221559.png",
        Sword: "/IMG_20260927_221615.png",
        "Love lock": "/IMG_20260927_221637.png",
        Lantern: "/IMG_20260927_221654.png",
        Ring: "/IMG_20260927_221707.png",
        "Dancing Girl": "/IMG_20260927_221722.png",
        Whale: "/IMG_20260927_221742.png",
        Star: "/file_0000000066f482118f772ed6fab4ad1f.png",
        "Fire Bird": "/file_00000000fe088211b7be0110e2d3f878.png",
      };
      const amount = Number(data.amount);
      const diamondAmount = Number.isFinite(amount) && amount > 0 ? Math.floor(amount) : 0;
      data.diamondAmount = diamondAmount;
      const image = luckyImages[String(data.giftName || "")];
      if (!image) return originalEmit(event, data);

      originalEmit("coin_transfer", { ...data, luckyGift: true, luckyImage: image });
      const recipientIds = new Set(Array.isArray(data.recipientIds) ? data.recipientIds.map(String) : []);
      for (const seat of seats) {
        const targetId = String(seat?.user?.accountId || "");
        if (!seat?.isOccupied || !targetId || !recipientIds.has(targetId)) continue;
        const luckyEvent = {
          eventId: `lucky-${Date.now()}-${Math.random().toString(36).slice(2)}`,
          roomId: String(data.roomId || ""),
          userId: String(data.senderId || ""),
          action: "lucky_image",
          seatNumber: Number(seat.number),
          src: image,
          timestamp: Date.now(),
          duration: 1100,
          luckyGift: true,
          targetName: String(seat.user?.name || ""),
          user: { name: "Lucky Gift", image, accountId: targetId },
        };
        window.dispatchEvent(new CustomEvent("hurry:lucky-image", { detail: luckyEvent }));
        originalEmit("room_seat_action", luckyEvent);
      }
      return socket;
    }) as typeof socket.emit;
    (socket as any).emit = patchedEmit;
    return () => { (socket as any).emit = originalEmit; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [seatsKey]);

  const [playingVideo, setPlayingVideo] = useState<
    { src: string; style: "fade" | "pure" } | null
  >(null);

  const sheetRef = useRef<HTMLDivElement>(null);
  const [selectedTargets, setSelectedTargets] = useState<string[]>([]);
  const [selectionLabel, setSelectionLabel] = useState<"All" | "All room">("All");
  const [recipientMode, setRecipientMode] = useState<RecipientMode>("mic");
  const videoTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const sendingSafetyRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const tabs = ["Hot", "Lucky", "Luxury", "Event"];
  const multipliers = ["1×", "10×", "299×", "599×", "999×"];

  const handleTouchStart = (e: React.TouchEvent) => {
    touchStartXRef.current = e.touches[0].clientX;
    touchStartYRef.current = e.touches[0].clientY;
  };

  const handleTouchEnd = (e: React.TouchEvent) => {
    if (touchStartXRef.current === null || touchStartYRef.current === null) return;

    const deltaX = e.changedTouches[0].clientX - touchStartXRef.current;
    const deltaY = e.changedTouches[0].clientY - touchStartYRef.current;

    touchStartXRef.current = null;
    touchStartYRef.current = null;

    if (Math.abs(deltaX) < 60) return;
    if (Math.abs(deltaY) > Math.abs(deltaX)) return;

    const currentIndex = tabs.indexOf(activeTab);

    if (deltaX < 0) {
      const nextIndex = (currentIndex + 1) % tabs.length;
      setActiveTab(tabs[nextIndex]);
    } else {
      const prevIndex = (currentIndex - 1 + tabs.length) % tabs.length;
      setActiveTab(tabs[prevIndex]);
    }
  };

  const hotGifts: Gift[] = [
    {
      id: 1,
      name: "Teddy",
      coins: 70000,
      image: "/IMG_20260922_142150.jpg",
      video: "/VID_20260921_011932.mp4",
      videoStyle: "fade",
    },
    {
      id: 2,
      name: "Autumn's Embrace",
      coins: 54900,
      image: "/IMG_20260922_182259.png",
      video: "/gemini_generated_video_89e836bd~2.mp4",
      videoStyle: "pure",
      noMask: true,
    },
    {
      id: 3,
      name: "Arab King",
      coins: 500000,
      image: "/image_d9df9625~2.jpg",
      video: "/gemini_generated_video_17e19680~2.mp4",
      videoStyle: "fade",
      sideFade: true,
    },
  ];

  const luckyGifts: Gift[] = [
    { id: 101, name: "Tiara", coins: 3000, image: "/IMG_20260927_213855.png", noMask: true },
    { id: 102, name: "Lucky Clover", coins: 1499, image: "/IMG_20260927_213917.png", noMask: true },
    { id: 103, name: "Hi", coins: 999, image: "/IMG_20260927_213946.png", noMask: true },
    { id: 104, name: "Rose", coins: 3999, image: "/IMG_20260927_214121.png", noMask: true },
    { id: 105, name: "Kiss", coins: 1600, image: "/IMG_20260927_214139.png", noMask: true },
    { id: 106, name: "Balloon", coins: 4000, image: "/IMG_20260927_214220.png", noMask: true },
    { id: 107, name: "Dragon", coins: 7000, image: "/IMG_20260927_221521.png", noMask: true },
    { id: 108, name: "Nine Hands", coins: 10999, image: "/IMG_20260927_221544.png", noMask: true },
    { id: 109, name: "Coffin", coins: 8999, image: "/IMG_20260927_221559.png", noMask: true },
    { id: 110, name: "Sword", coins: 9999, image: "/IMG_20260927_221615.png", noMask: true },
    { id: 111, name: "Love lock", coins: 5000, image: "/IMG_20260927_221637.png", noMask: true },
    { id: 112, name: "Lantern", coins: 6999, image: "/IMG_20260927_221654.png", noMask: true },
    { id: 113, name: "Ring", coins: 5999, image: "/IMG_20260927_221707.png", noMask: true },
    { id: 114, name: "Dancing Girl", coins: 12000, image: "/IMG_20260927_221722.png", noMask: true },
    { id: 115, name: "Whale", coins: 7899, image: "/IMG_20260927_221742.png", noMask: true },
    { id: 116, name: "Star", coins: 9800, image: "/file_0000000066f482118f772ed6fab4ad1f.png", noMask: true },
    { id: 117, name: "Fire Bird", coins: 13000, image: "/file_00000000fe088211b7be0110e2d3f878.png", noMask: true },
  ];

  const currentGifts: Gift[] =
    activeTab === "Lucky" ? luckyGifts : activeTab === "Hot" ? hotGifts : [];

  useEffect(() => {
    let alive = true;
    const fetchBal = async () => {
      const b = await loadWalletBalance();
      if (alive) setWalletBalance(b);
    };
    fetchBal();
    const id = setInterval(fetchBal, 1000);
    return () => {
      alive = false;
      clearInterval(id);
    };
  }, []);

  useEffect(() => {
    if (playingVideo) return;
    const handler = (e: MouseEvent) => {
      if (isWalletOpen) return;
      if (open && sheetRef.current && !sheetRef.current.contains(e.target as Node)) {
        onClose();
      }
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, [onClose, playingVideo, open, isWalletOpen]);

  useEffect(() => {
    if (!open) {
      if (insufficientTimerRef.current) {
        clearTimeout(insufficientTimerRef.current);
        insufficientTimerRef.current = null;
      }
      if (walletOpenTimerRef.current) {
        clearTimeout(walletOpenTimerRef.current);
        walletOpenTimerRef.current = null;
      }
      setShowInsufficient(false);
    }
  }, [open]);

  useEffect(() => {
    return () => {
      if (videoTimeoutRef.current) clearTimeout(videoTimeoutRef.current);
      if (insufficientTimerRef.current) clearTimeout(insufficientTimerRef.current);
      if (walletOpenTimerRef.current) clearTimeout(walletOpenTimerRef.current);
      if (sendingSafetyRef.current) clearTimeout(sendingSafetyRef.current);
    };
  }, []);

  const triggerInsufficient = (message: string = "Insufficient Balance", openWalletAfter: boolean = true) => {
    setInsufficientText(message);
    setShowInsufficient(true);
    if (insufficientTimerRef.current) clearTimeout(insufficientTimerRef.current);
    if (walletOpenTimerRef.current) clearTimeout(walletOpenTimerRef.current);

    insufficientTimerRef.current = setTimeout(() => {
      setShowInsufficient(false);
      if (openWalletAfter) {
        walletOpenTimerRef.current = setTimeout(() => setIsWalletOpen(true), 80);
      }
    }, 2000);
  };

  useEffect(() => {
    const handler = () => {
      if (!open) return;
      triggerInsufficient("Insufficient Balance", true);
    };
    window.addEventListener("hurry:insufficient", handler);
    return () => window.removeEventListener("hurry:insufficient", handler);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  const parseMultiplier = (m: string) => parseInt(m.replace("×", ""), 10) || 1;
  const selectedGiftObj = currentGifts.find((g) => g.id === selectedGift);
  const totalCost = selectedGiftObj
    ? selectedGiftObj.coins * parseMultiplier(selectedMultiplier)
    : 0;
  const recipientIds = selectedTargets.length
    ? selectedTargets
    : roomUsers.length > 0
      ? roomUsers.map((u) => u.accountId)
      : seats
          .filter((s) => s.isOccupied && s.user)
          .map((s) => s.user!.accountId);
  const recipientCount = recipientIds.length;
  const recipientUsers = recipientIds
    .map((id) => roomUsers.find((u) => String(u.accountId) === String(id)) || seats.find((s) => String(s.user?.accountId || "") === String(id))?.user)
    .filter(Boolean) as Array<{ accountId: string; name: string; image: string }>;
  const firstRecipient = recipientUsers[0];
  const totalSendCost = totalCost * Math.max(1, recipientCount);
  const canAfford = totalCost > 0 && recipientCount > 0 && totalSendCost <= walletBalance;
  const isLuckyGiftTab = activeTab === "Lucky";

  const finishVideo = () => {
    if (videoTimeoutRef.current) {
      clearTimeout(videoTimeoutRef.current);
      videoTimeoutRef.current = null;
    }
    setPlayingVideo(null);
    setSending(false);
    onClose();
  };

  useEffect(() => {
    const handleRemoteGiftVideo = (data: any = {}) => {
      if (!data?.roomId || String(data.roomId) !== String(roomId)) return;
      if (String(data.senderId || "") === String(currentUserAccountId || "")) return;
      if (!data.video) return;
      if (localStorage.getItem("hurry_gift_effect_enabled") !== "true") return;
      setPlayingVideo({ src: String(data.video), style: data.videoStyle === "fade" ? "fade" : "pure" });
      setSending(false);
      if (videoTimeoutRef.current) clearTimeout(videoTimeoutRef.current);
      videoTimeoutRef.current = setTimeout(finishVideo, 15000);
    };
    socket.on("gift_video_play", handleRemoteGiftVideo);
    return () => socket.off("gift_video_play", handleRemoteGiftVideo);
  }, [roomId, currentUserAccountId]);

  const handleSend = async () => {
    if (isWalletOpen) return;
    if (!selectedGiftObj || sending) return;

    if (recipientCount === 0) {
      triggerInsufficient("Select Recipient", false);
      return;
    }

    if (totalSendCost > walletBalance) {
      triggerInsufficient("Insufficient Balance", true);
      return;
    }

    if (!canAfford) return;
    setSending(true);

    if (sendingSafetyRef.current) clearTimeout(sendingSafetyRef.current);
    sendingSafetyRef.current = setTimeout(() => setSending(false), 10000);

    let luckyReturnAmount = 0;
    let luckyReturnPercent = 0;
    let netDeductionCost = totalSendCost;
    let winTimes = 0;

    if (isLuckyGiftTab) {
      winTimes = rollLuckyWin();
      luckyReturnAmount = winTimes > 0 ? (totalSendCost * winTimes) : 0;
      luckyReturnPercent = winTimes > 0 ? winTimes : 0;
      netDeductionCost = totalSendCost - luckyReturnAmount;
    }

    setWalletBalance((p) => Math.max(0, p - netDeductionCost));
    await updateWalletBalance(-netDeductionCost);
    await recordGiftTransaction(selectedGiftObj.name, -netDeductionCost);

    if (roomId && currentUserAccountId && recipientIds.length > 0) {
      const uniqueTransferId = `tx-${currentUserAccountId}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;

      socket.emit("coin_transfer", {
        roomId: String(roomId),
        senderId: String(currentUserAccountId),
        recipientIds: recipientIds.map(String),
        amount: totalCost,
        giftName: selectedGiftObj.name,
        giftImage: selectedGiftObj.image,
        giftType: activeTab,
        senderName: currentUserName,
        senderImage: currentUserImage,
        recipientName: firstRecipient?.name || "User",
        recipientImage: firstRecipient?.image || "/default-avatar.png",
        multiplier: parseMultiplier(selectedMultiplier),
        luckyGift: isLuckyGiftTab,
        luckyReturnAmount,
        luckyReturnPercent,
        luckyImage: selectedGiftObj.image,
        winTimes,
        transferId: uniqueTransferId,
        eventId: uniqueTransferId,
        timestamp: Date.now(),
        recipientMode,
      });

      const isSelfRecipient = recipientIds.some(id => String(id) === String(currentUserAccountId));
      if (isSelfRecipient && isLuckyGiftTab) {
        const diamondCredit = Math.floor(totalCost * 0.1);
        if (diamondCredit > 0) {
          await addDiamondsToLocalDB(diamondCredit);
          await recordDiamondTransaction("Diamonds received — " + selectedGiftObj.name, diamondCredit);
          window.dispatchEvent(new CustomEvent("hurry:diamonds-updated", { detail: { amount: diamondCredit } }));
        }
      }

      if (isLuckyGiftTab) {
        window.dispatchEvent(new CustomEvent("hurry:lucky-slider", {
          detail: {
            roomId: String(roomId),
            senderId: String(currentUserAccountId),
            senderName: currentUserName,
            senderImage: currentUserImage,
            recipientName: firstRecipient?.name || "User",
            giftImage: selectedGiftObj.image,
            multiplier: parseMultiplier(selectedMultiplier),
            luckyGift: true,
            luckyReturnAmount: luckyReturnAmount,
            winTimes: winTimes,
            recipientMode,
          }
        }));
      }

      if (selectedGiftObj.video && !isLuckyGiftTab && localStorage.getItem("hurry_gift_effect_enabled") === "true") {
        socket.emit("gift_video_play", {
          roomId: String(roomId),
          senderId: String(currentUserAccountId),
          giftName: selectedGiftObj.name,
          video: String(selectedGiftObj.video),
          videoStyle: selectedGiftObj.videoStyle ?? "fade",
          timestamp: Date.now(),
        });
      }
    }

    if (onSend) {
      onSend(totalCost);
    }

    if (isLuckyGiftTab) {
      window.dispatchEvent(new CustomEvent("hurry:lucky-combo", {
        detail: {
          roomId: String(roomId || ""),
          senderId: String(currentUserAccountId || ""),
          senderName: currentUserName,
          senderImage: currentUserImage,
          recipientIds: recipientIds.map(String),
          recipientName: firstRecipient?.name || "User",
          recipientImage: firstRecipient?.image || "/default-avatar.png",
          giftName: selectedGiftObj.name,
          giftImage: selectedGiftObj.image,
          giftCoins: selectedGiftObj.coins,
          initialMultiplier: parseMultiplier(selectedMultiplier),
          recipientMode,
        },
      }));
      if (sendingSafetyRef.current) clearTimeout(sendingSafetyRef.current);
      setSending(false);
      onClose();
      return;
    }

    if (selectedGiftObj.video && localStorage.getItem("hurry_gift_effect_enabled") === "true") {
      setPlayingVideo({
        src: selectedGiftObj.video,
        style: selectedGiftObj.videoStyle ?? "fade",
      });
      if (videoTimeoutRef.current) clearTimeout(videoTimeoutRef.current);
      videoTimeoutRef.current = setTimeout(() => {
        finishVideo();
      }, 10000);
    } else {
      if (sendingSafetyRef.current) clearTimeout(sendingSafetyRef.current);
      setSending(false);
      onClose();
    }
  };

  const handleAllOnMic = () => {
    const micUsers = seats
      .filter((s) => s.isOccupied && s.user)
      .map((s) => s.user!.accountId);
    setSelectedTargets(micUsers);
    setSelectionLabel("All");
    setRecipientMode("mic");
  };

  const handleAllInRoom = () => {
    setSelectedTargets([]);
    setSelectionLabel("All room");
    setRecipientMode("room");
  };

  const handleOpenWallet = () => {
    setIsWalletOpen(true);
  };

  const handleAvatarClick = (accountId: string) => {
    setSelectedTargets((prev) => {
      if (prev.includes(accountId)) return prev.filter((id) => id !== accountId);
      return [...prev, accountId];
    });
    setRecipientMode("single");
    if (selectionLabel === "All room") setSelectionLabel("All");
  };

  if (playingVideo) {
    const isFade = playingVideo.style === "fade";
    const isArabKing = playingVideo.src.includes("17e19680");

    const teddyVideoMask =
      "linear-gradient(to bottom, transparent 0%, transparent 18%, black 26%, black 70%, transparent 83%, transparent 100%)";

    const kingVideoMask =
      "linear-gradient(to bottom, transparent 0%, transparent 10%, black 16%, black 85%, transparent 94%, transparent 100%)";

    return (
      <>
        {!isFade && (
          <svg style={{ width: 0, height: 0, position: "absolute" }} aria-hidden="true">
            <filter id="remove-black" colorInterpolationFilters="sRGB">
              <feColorMatrix
                type="matrix"
                values="1 0 0 0 0  0 1 0 0 0  0 0 1 0 0  1.5 1.5 1.5 0 -0.2"
              />
            </filter>
          </svg>
        )}

        <div
          className="fixed inset-0 z-[100] flex items-center justify-center pointer-events-none"
          style={{ background: "transparent" }}
        >
          <video
            autoPlay
            loop
            muted
            playsInline
            ref={(el) => {
              if (el) {
                el.controls = false;
                el.removeAttribute("controls");
                el.setAttribute("controlsList", "nodownload noplaybackrate noremoteplayback");
                el.setAttribute("disablePictureInPicture", "");
                el.setAttribute("disableRemotePlayback", "");
                el.play().catch(() => {});
              }
            }}
            src={playingVideo.src}
            autoPlay
            playsInline
            controls={false}
            disablePictureInPicture
            disableRemotePlayback
            poster={playingVideo.src.includes("17e19680") ? "/image_d9df9625~2.jpg" : "/IMG_20260922_142150.jpg"}
            onLoadedData={(e) => {
              setSending(false);
              e.currentTarget.play().catch(() => {});
            }}
            onCanPlay={(e) => {
              e.currentTarget.play().catch(() => {});
            }}
            onPlaying={() => setSending(false)}
            onEnded={finishVideo}
            onError={finishVideo}
            className={
              isFade
                ? "w-full h-full object-cover"
                : "w-auto h-auto max-w-[72vw] max-h-[72vh] object-contain"
            }
            style={
              isFade
                ? isArabKing
                  ? {
                      WebkitMaskImage: kingVideoMask,
                      maskImage: kingVideoMask,
                      transform: "translateY(2%)",
                      transformOrigin: "center",
                    }
                  : {
                      WebkitMaskImage: teddyVideoMask,
                      maskImage: teddyVideoMask,
                    }
                : {
                    mixBlendMode: "screen",
                    backgroundColor: "transparent",
                    filter: "url(#remove-black)",
                  }
            }
          />
        </div>
      </>
    );
  }

  return (
    <div>
      <div
        className="fixed inset-0 z-50 flex items-end justify-center"
        style={{ display: open ? "flex" : "none" }}
      >
      <svg style={{ position: "absolute", width: 0, height: 0 }}>
        <filter id="removeWhite" x="0%" y="0%" width="100%" height="100%">
          <feColorMatrix
            type="matrix"
            values="1 0 0 0 0  0 1 0 0 0  0 0 1 0 0  -0.333 -0.333 -0.333 1 0"
          />
        </filter>
      </svg>

      <style>{`
        video::-webkit-media-controls,
        video::-webkit-media-controls-enclosure,
        video::-webkit-media-controls-panel,
        video::-webkit-media-controls-overlay-play-button,
        video::-webkit-media-controls-start-playback-button { display: none !important; opacity: 0 !important; }
        .main-container { 
          background: rgba(0, 0, 0, 0.92); 
        }
        .gift-item {
          background: transparent;
          border: 2px solid transparent;
          transition: all 0.2s ease;
          padding: 6px 2px;
          border-radius: 8px;
          width: 100%;
        }
        .gift-item.selected {
          border-color: #3b82f6;
          background: rgba(59, 130, 246, 0.05);
        }
        .coin-image {
          filter: drop-shadow(0 0 4px rgba(255, 215, 0, 0.4));
        }
        .scrollbar-none::-webkit-scrollbar { display: none; }
        .scrollbar-none { -ms-overflow-style: none; scrollbar-width: none; }

        @keyframes insufficientIn {
          0% { transform: scale(0.85) translateY(10px); opacity: 0; }
          100% { transform: scale(1) translateY(0); opacity: 1; }
        }
        @keyframes insufficientOut {
          0% { transform: scale(1) translateY(0); opacity: 1; }
          100% { transform: scale(0.9) translateY(-8px); opacity: 0; }
        }
      `}</style>

      <div
        ref={sheetRef}
        className="main-container h-[50vh] w-full max-w-md mx-auto text-white flex flex-col rounded-t-md border-t border-white/10 shadow-2xl relative px-4 pt-3 pb-2"
      >

        {/* 🎤 MIC + 🏠 HOUSE */}
        <div className="absolute top-3 right-3 z-[60] flex items-center gap-2">
          <button
            onClick={handleAllOnMic}
            className={`relative w-[38px] h-[38px] rounded-full border-2 flex items-center justify-center transition-all -mt-2 ${
              recipientMode === "mic"
                ? "border-[#3b82f6] bg-[#3b82f6]/20 text-[#3b82f6]"
                : "border-gray-500 bg-[#282d32] text-gray-400"
            }`}
          >
            <SolidMicIcon className="w-[18px] h-[18px]" />
            <div
              className={`absolute -bottom-1 -right-1.5 px-1.5 py-0 rounded-full text-[9px] font-bold leading-[1.2] ${
                recipientMode === "mic" ? "bg-[#3b82f6] text-white" : "bg-gray-400 text-white"
              }`}
            >
              All
            </div>
          </button>

          <button
            onClick={handleAllInRoom}
            className={`relative w-[38px] h-[38px] rounded-full border-2 flex items-center justify-center transition-all -mt-2 ${
              recipientMode === "room"
                ? "border-[#3b82f6] bg-[#3b82f6]/20 text-[#3b82f6]"
                : "border-gray-500 bg-[#282d32] text-gray-400"
            }`}
          >
            <HoundIcon
              className={`w-[20px] h-[20px] transition-colors ${
                recipientMode === "room" ? "text-[#3b82f6]" : "text-gray-400"
              }`}
            />
            <div
              className={`absolute -bottom-1 -right-1.5 px-1.5 py-0 rounded-full text-[9px] font-bold leading-[1.2] ${
                recipientMode === "room" ? "bg-[#3b82f6] text-white" : "bg-gray-400 text-white"
              }`}
            >
              All
            </div>
          </button>
        </div>

        <div className="flex items-center gap-3 overflow-x-auto scrollbar-none pr-28 pt-0 pb-2 -mt-2 min-h-[52px]">
          {seats
            .filter((seat) => seat.isOccupied && seat.user)
            .map((seat) => {
              const isSelected = selectedTargets.includes(seat.user!.accountId);
              return (
                <div
                  key={seat.user!.accountId}
                  onClick={() => handleAvatarClick(seat.user!.accountId)}
                  className={`relative w-11 h-11 flex-shrink-0 rounded-full cursor-pointer transition-all duration-200 border-[2.5px] ${
                    isSelected ? "border-[#3b82f6]" : "border-transparent"
                  }`}
                >
                  <img
                    src={seat.user!.image || "/default-avatar.png"}
                    alt={seat.user!.name}
                    className="w-full h-full object-cover rounded-full"
                  />
                </div>
              );
            })}
        </div>

        <div className="w-full h-px bg-white/10 mb-1 -mt-2" />

        <div className="flex items-center gap-4 py-2 px-1">
          {tabs.map((tab) => {
            const isActive = activeTab === tab;
            return (
              <button
                key={tab}
                onClick={() => setActiveTab(tab)}
                className={`text-[14px] font-semibold transition-all ${
                  isActive
                    ? "text-white font-bold scale-105"
                    : "text-gray-400 hover:text-gray-200"
                }`}
              >
                {tab}
              </button>
            );
          })}
        </div>

        <div
          className="flex-1 overflow-y-auto py-2 scrollbar-none"
          onTouchStart={handleTouchStart}
          onTouchEnd={handleTouchEnd}
        >
          <div className="grid grid-cols-4 gap-1 content-start">
            {currentGifts.map((gift) => (
              <div
                key={gift.id}
                onClick={() => setSelectedGift(gift.id)}
                className={`gift-item relative flex flex-col items-center justify-center transition cursor-pointer active:scale-95 ${
                  selectedGift === gift.id ? "selected" : ""
                }`}
              >
                {/* 🏷️ Lucky Tab — har card ke Top-Left corner me badge */}
                {activeTab === "Lucky" && (
                  <img
                    src="/IMG_20260928_172454.png"
                    alt=""
                    draggable={false}
                    className="absolute top-0 left-0 w-4 h-4 object-contain pointer-events-none z-10"
                  />
                )}

                {/* 🏷️ Hot Tab — sirf Arab King (id:3) card ke Top-Left corner me badge */}
                {activeTab === "Hot" && gift.id === 3 && (
                  <img
                    src="/IMG_20260928_172516.png"
                    alt=""
                    draggable={false}
                    className="absolute top-0 left-0 w-4 h-4 object-contain pointer-events-none z-10"
                  />
                )}

                <div
                  className={`relative mb-1 overflow-hidden ${
                    gift.noMask ? "w-15 h-15" : "w-16 h-16"
                  } ${gift.sideFade ? "rounded-xl" : ""}`}
                  style={
                    activeTab === "Hot" && !gift.noMask
                      ? gift.sideFade
                        ? {
                            WebkitMaskImage: "radial-gradient(ellipse 50% 50% at center, black 35%, transparent 100%)",
                            maskImage: "radial-gradient(ellipse 50% 50% at center, black 35%, transparent 100%)",
                          }
                        : {
                            WebkitMaskImage: "radial-gradient(circle, black 40%, transparent 80%)",
                            maskImage: "radial-gradient(circle, black 40%, transparent 80%)",
                          }
                      : {}
                  }
                >
                  <img
                    src={gift.image}
                    alt={gift.name}
                    className={`w-full h-full ${gift.noMask ? "object-contain" : "object-cover"} ${gift.sideFade ? "rounded-xl" : ""}`}
                  />
                </div>

                <span className="text-white font-medium text-[10px] truncate w-full text-center">
                  {gift.name}
                </span>

                <span className="text-gray-400 flex items-center justify-center gap-0.5 mt-0.5 text-[9px]">
                  <div className="w-2.5 h-2.5 relative overflow-hidden rounded-full">
                    <img
                      src="/file_00000000e56882119c217d508b6733dc.png"
                      alt="Coins"
                      className="coin-image w-full h-full object-cover"
                    />
                  </div>
                  {gift.coins.toLocaleString()}
                </span>
              </div>
            ))}
          </div>
        </div>

        <div className="flex items-center justify-between pt-2 relative">
          <div
            className="flex items-center gap-1.5 cursor-pointer active:scale-95 transition-transform"
            onClick={handleOpenWallet}
          >
            <div className="w-5 h-5 relative overflow-hidden rounded-full flex-shrink-0">
              <img
                src="/file_00000000e56882119c217d508b6733dc.png"
                alt="Coins"
                className="w-full h-full object-cover filter drop-shadow-[0_0_4px_rgba(255,215,0,0.4)]"
              />
            </div>
            <span className="text-[11px] font-bold text-white tracking-wide">
              {walletBalance.toLocaleString()}
            </span>
            <ChevronRight className="w-4 h-4 text-white" />
          </div>

          <div className="flex items-center gap-2 relative">
            {showMultipliers && (
              <div className="absolute bottom-[calc(100%+8px)] right-0 rounded-md p-1 shadow-xl flex flex-col gap-1 z-50 bg-[#0c1418] border border-white/10 min-w-[50px]">
                {multipliers.map((num) => (
                  <button
                    key={num}
                    onClick={() => {
                      setSelectedMultiplier(num);
                      setShowMultipliers(false);
                    }}
                    className={`px-2.5 py-0.5 text-xs rounded-md text-center font-medium transition ${
                      selectedMultiplier === num
                        ? "bg-blue-600 text-white"
                        : "hover:bg-white/10 text-gray-300"
                    }`}
                  >
                    {num}
                  </button>
                ))}
              </div>
            )}

            <div
              className="flex items-center rounded-full border border-[#3b82f6] overflow-hidden relative"
              style={{ background: "transparent" }}
            >
              <button
                onClick={() => setShowMultipliers(!showMultipliers)}
                className="flex items-center gap-1 px-3 py-1.5 text-xs font-bold text-white bg-transparent transition-transform"
              >
                <span>{selectedMultiplier}</span>
                <ChevronUp className={`w-3 h-3 transition-transform ${showMultipliers ? "rotate-180" : ""}`} />
              </button>

              <button
                onClick={handleSend}
                disabled={!selectedGift || sending}
                className="text-white font-bold text-xs px-6 py-1.5 rounded-r-full transition-all active:scale-95"
                style={{
                  background: "linear-gradient(135deg, #3b82f6, #2563eb)",
                  boxShadow: "0 2px 10px rgba(59,130,246,0.35)",
                  opacity: !selectedGift || sending ? 0.55 : 1,
                  cursor: !selectedGift || sending ? "not-allowed" : "pointer",
                }}
              >
                {sending ? "send" : "Send"}
              </button>
            </div>
          </div>
        </div>
       </div>
      </div>

      {open && showInsufficient && (
        <div className="fixed inset-0 z-[2147483646] flex items-center justify-center pointer-events-none">
          <div
            className="rounded-full px-4 py-1.5 flex items-center"
            style={{
              background: "rgba(0,0,0,0.75)",
              animation: "insufficientIn 0.25s ease-out forwards",
            }}
          >
            <span className="text-white font-bold text-[13px] tracking-wide">
              {insufficientText}
            </span>
          </div>
        </div>
      )}

      {isWalletOpen && (
        <div
          className="fixed inset-0 z-[2147483647] bg-black/80 flex items-center justify-center"
          onMouseDown={(e) => e.stopPropagation()}
        >
          <Wallet onClose={() => setIsWalletOpen(false)} />
        </div>
      )}

      <LuckyGiftNotificationSlider roomId={String(roomId || "")} />
      <LuckyComboButton />
    </div>
  );
}

// ==========================================================
// 🎲 Lucky Gift Notification Slider
interface LuckyNotice {
  id: string;
  senderId: string;
  senderName: string;
  recipientName: string;
  senderImage: string;
  giftImage: string;
  multiplier: number;
  totalWinAmount: number;
  maxWinTimes: number;
  isExiting: boolean;
  recipientMode: RecipientMode;
}

export function LuckyGiftNotificationSlider({ roomId }: { roomId: string }) {
  const [notice, setNotice] = useState<LuckyNotice | null>(null);
  const [floatingWins, setFloatingWins] = useState<{ id: string; amount: number }[]>([]);
  const [popTick, setPopTick] = useState(0);
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    const processNotice = (data: any = {}) => {
      if (String(data.roomId || "") !== String(roomId || "")) return;
      if (data.luckyGift !== true) return;

      const senderId = String(data.senderId || "");
      const senderName = String(data.senderName || "User");
      const recipientName = String(data.recipientName || "User");
      const senderImage = String(data.senderImage || "/default-avatar.png");
      const giftImage = String(data.luckyImage || data.giftImage || "");
      const multiplier = Math.max(1, Number(data.multiplier) || 1);
      const incomingWin = Math.floor(Number(data.luckyReturnAmount) || 0);
      const incomingWinTimes = Number(data.winTimes) || 0;
      const rawMode = String(data.recipientMode || "single");
      const recipientMode: RecipientMode =
        rawMode === "mic" || rawMode === "room" || rawMode === "single" ? (rawMode as RecipientMode) : "single";

      setNotice((prev) => {
        if (prev && prev.senderId === senderId && prev.giftImage === giftImage) {
          return {
            ...prev,
            multiplier: Math.max(multiplier, prev.multiplier),
            maxWinTimes: Math.max(incomingWinTimes, prev.maxWinTimes || 0),
            recipientMode,
            recipientName,
            isExiting: false,
          };
        }
        return {
          id: `notify-${Date.now()}-${Math.random()}`,
          senderId,
          senderName,
          recipientName,
          senderImage,
          giftImage,
          multiplier,
          totalWinAmount: 0,
          maxWinTimes: incomingWinTimes,
          isExiting: false,
          recipientMode,
        };
      });

      setPopTick((t) => t + 1);

      if (incomingWin > 0) {
        const floatId = `float-${Date.now()}-${Math.random()}`;
        setFloatingWins((prev) => [...prev, { id: floatId, amount: incomingWin }]);

        setTimeout(() => {
          setNotice((currentNotice) => {
            if (!currentNotice) return currentNotice;
            return {
              ...currentNotice,
              totalWinAmount: currentNotice.totalWinAmount + incomingWin,
            };
          });
        }, 600);

        setTimeout(() => {
          setFloatingWins((prev) => prev.filter((f) => f.id !== floatId));
        }, 1400);
      }

      if (timerRef.current) clearTimeout(timerRef.current);
      timerRef.current = setTimeout(() => {
        setNotice((prev) => (prev ? { ...prev, isExiting: true } : null));
      }, 4500);
    };

    socket.on("coin_transfer_received", processNotice);

    const localHandler = (e: any) => processNotice(e.detail);
    window.addEventListener("hurry:lucky-slider", localHandler);

    const handleClose = () => {
      setNotice((prev) => (prev ? { ...prev, isExiting: true } : null));
    };
    window.addEventListener("hurry:lucky-slider-close", handleClose);

    return () => {
      socket.off("coin_transfer_received", processNotice);
      window.removeEventListener("hurry:lucky-slider", localHandler);
      window.removeEventListener("hurry:lucky-slider-close", handleClose);
      if (timerRef.current) clearTimeout(timerRef.current);
    };
  }, [roomId]);

  useEffect(() => {
    if (notice?.isExiting) {
      const t = setTimeout(() => {
        setNotice(null);
        setFloatingWins([]);
      }, 400);
      return () => clearTimeout(t);
    }
  }, [notice?.isExiting]);

  if (!notice) return null;

  const isBroadcast = notice.recipientMode === "mic" || notice.recipientMode === "room";
  const topText = notice.recipientMode === "mic"
    ? "Sent to All Mic"
    : notice.recipientMode === "room"
      ? "Sent to All in room"
      : notice.senderName;
  const bottomText = isBroadcast
    ? notice.senderName
    : `To ${notice.recipientName}`;

  return (
    <>
      <style>{`
        @keyframes slideInLeft {
          0% { transform: translateX(-100%); opacity: 0; }
          100% { transform: translateX(0); opacity: 1; }
        }
        @keyframes slideOutLeft {
          0% { transform: translateX(0); opacity: 1; }
          100% { transform: translateX(-120%); opacity: 0; }
        }
        @keyframes popGift {
          0% { transform: scale(1); }
          50% { transform: scale(1.4); }
          100% { transform: scale(1); }
        }
        /* YEH NAYI ANIMATION HAI JISSE SIZE KHARAB NAHI HOGA */
        @keyframes dropWinCoinsLeft {
          0% { transform: translateY(-15px) scale(0.8); opacity: 0; }
          20% { transform: translateY(0) scale(1); opacity: 1; }
          80% { transform: translateY(0) scale(1); opacity: 1; }
          100% { transform: translateY(15px) scale(0.8); opacity: 0; }
        }
      `}</style>

      <div
        key={notice.id}
        style={{
          position: "fixed",
          left: 0,
          bottom: "36vh",
          zIndex: 2147483000,
          pointerEvents: "none",
          display: "flex",
          alignItems: "center",
          animation: notice.isExiting
            ? "slideOutLeft 0.4s ease-in forwards"
            : "slideInLeft 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275) forwards",
        }}
      >
        <div
          style={{
            position: "relative",
            display: "flex",
            alignItems: "center",
            background:
              "linear-gradient(90deg, #ffc107 0%, #ffc107 22%, rgba(255,193,7,0.85) 42%, rgba(255,193,7,0.45) 65%, rgba(255,193,7,0.15) 85%, rgba(255,193,7,0) 100%)",
            borderRadius: "50px 0 0 50px",
            padding: "4px 60px 4px 4px",
            minWidth: "280px",
            maxWidth: "90vw",
          }}
        >
          <div style={{ flexShrink: 0, zIndex: 1 }}>
            <img
              src={notice.senderImage}
              alt={notice.senderName}
              style={{
                width: 44,
                height: 44,
                borderRadius: "50%",
                objectFit: "cover",
                display: "block",
              }}
              draggable={false}
            />
          </div>

          <div style={{ marginLeft: 8, display: "flex", flexDirection: "column", maxWidth: 110, zIndex: 1 }}>
            <span
              style={{
                color: "#fff",
                fontSize: 13,
                fontWeight: "bold",
                whiteSpace: "nowrap",
                overflow: "hidden",
                textOverflow: "ellipsis",
                lineHeight: 1.2,
                textShadow: "0 1px 2px rgba(0,0,0,0.4)",
              }}
            >
              {topText}
            </span>
            <span
              style={{
                color: "rgba(255,255,255,0.9)",
                fontSize: 11,
                whiteSpace: "nowrap",
                overflow: "hidden",
                textOverflow: "ellipsis",
                lineHeight: 1.2,
                textShadow: "0 1px 2px rgba(0,0,0,0.3)",
              }}
            >
              {bottomText}
            </span>
          </div>

          <div
            key={`${notice.multiplier}-${popTick}`}
            style={{
              display: "flex",
              alignItems: "center",
              marginLeft: 8,
              animation: "popGift 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275)",
              zIndex: 1,
            }}
          >
            {notice.giftImage && (
              <img
                src={notice.giftImage}
                alt=""
                style={{
                  width: 38,
                  height: 38,
                  objectFit: "contain",
                  display: "block",
                }}
                draggable={false}
              />
            )}
            <span
              style={{
                color: "#fff",
                fontWeight: 900,
                fontSize: 22,
                fontStyle: "italic",
                textShadow: "0 2px 4px rgba(0,0,0,0.5)",
                marginLeft: 4,
                lineHeight: 1,
              }}
            >
              ×{notice.multiplier}
            </span>
          </div>

          {/* 🏆 BIG WIN & COINS OVERLAY — Left main shift kiya, Name ke upar */}
          <div
            style={{
              position: "absolute",
              left: 35,
              bottom: "100%",
              marginBottom: -33,
              zIndex: 22,
              pointerEvents: "none",
            }}
          >
            {floatingWins.map((fw) => (
              <div
                key={fw.id}
                style={{
                  position: "absolute",
                  bottom: 0,
                  left: 0,
                  width: 140,
                  height: 140,
                  display: "flex",
                  flexDirection: "column",
                  alignItems: "center",
                  justifyContent: "center",
                  whiteSpace: "nowrap",
                  animation: "dropWinCoinsLeft 1.4s forwards",
                }}
              >
                {notice.maxWinTimes >= 10 && (
                  <img
                    src="/file_000000004e18820b810ae49258003b98.png"
                    alt="Big Win"
                    draggable={false}
                    style={{
                      position: "absolute",
                      width: "100%",
                      height: "100%",
                      objectFit: "contain",
                      zIndex: 1,
                    }}
                  />
                )}

                <div
                  style={{
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    gap: "1.5px",
                    position: "relative",
                    zIndex: 10,
                    top: 25,
                  }}
                >
                  <img
                    src="/file_00000000e56882119c217d508b6733dc.png"
                    alt="Coins"
                    style={{
                      width: 22,
                      height: 22,
                      objectFit: "cover",
                      borderRadius: "50%",
                      filter: "drop-shadow(0 0 2px rgba(0,0,0,0.5))",
                    }}
                  />
                  <span
                    style={{
                      color: "#fff0b3",
                      fontWeight: "900",
                      fontSize: 20,
                      textShadow:
                        "-1px -1px 0 #7a0000, 1px -1px 0 #7a0000, -1px 1px 0 #7a0000, 1px 1px 0 #7a0000, 0px 3px 5px rgba(0,0,0,1)",
                    }}
                  >
                    {fw.amount}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </>
  );
}

// ==========================================================
// ⭕ COMBO BUTTON
export function LuckyComboButton() {
  const [comboData, setComboData] = useState<any>(null);
  const [comboMultiplier, setComboMultiplier] = useState(1);
  const [timeLeft, setTimeLeft] = useState(0);
  const [busy, setBusy] = useState(false);
  const countdownRef = useRef<ReturnType<typeof setInterval> | null>(null);

  useEffect(() => {
    const handleLuckyCombo = (e: any) => {
      const data = e.detail;
      setComboData(data);
      setComboMultiplier(data.initialMultiplier || 1);
      startTimer();
    };

    window.addEventListener("hurry:lucky-combo", handleLuckyCombo);
    return () => window.removeEventListener("hurry:lucky-combo", handleLuckyCombo);
  }, []);

  const startTimer = () => {
    setTimeLeft(5);
    if (countdownRef.current) clearInterval(countdownRef.current);

    countdownRef.current = setInterval(() => {
      setTimeLeft((prev) => {
        if (prev <= 1) {
          window.dispatchEvent(new CustomEvent("hurry:lucky-slider-close"));
          setComboData(null);
          if (countdownRef.current) clearInterval(countdownRef.current);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
  };

  const handleComboClick = async () => {
    if (!comboData || busy) return;
    setBusy(true);

    startTimer();

    const nextMultiplier = comboMultiplier + 1;
    setComboMultiplier(nextMultiplier);

    const baseCost = comboData.giftCoins;
    const recipientCount = comboData.recipientIds.length;
    const totalCost = baseCost * recipientCount;

    let luckyReturnAmount = 0;
    let luckyReturnPercent = 0;
    let winTimes = 0;

    winTimes = rollLuckyWin();

    luckyReturnAmount = winTimes > 0 ? totalCost * winTimes : 0;
    luckyReturnPercent = winTimes > 0 ? winTimes : 0;
    const finalDeductionCost = totalCost - luckyReturnAmount;

    const bal = await loadWalletBalance();
    if (bal < finalDeductionCost) {
      window.dispatchEvent(new CustomEvent("hurry:insufficient"));
      setBusy(false);
      setComboData(null);
      return;
    }

    await updateWalletBalance(-finalDeductionCost);
    await recordGiftTransaction(comboData.giftName, -finalDeductionCost);

    const comboTransferId = `combo-${comboData.senderId}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;

    socket.emit("coin_transfer", {
      roomId: comboData.roomId,
      senderId: comboData.senderId,
      recipientIds: comboData.recipientIds,
      amount: totalCost,
      giftName: comboData.giftName,
      giftType: "Lucky",
      senderName: comboData.senderName,
      senderImage: comboData.senderImage,
      recipientName: comboData.recipientName,
      recipientImage: comboData.recipientImage,
      multiplier: nextMultiplier,
      luckyGift: true,
      luckyReturnAmount,
      luckyReturnPercent,
      luckyImage: comboData.giftImage,
      winTimes,
      transferId: comboTransferId,
      eventId: comboTransferId,
      timestamp: Date.now(),
      recipientMode: comboData.recipientMode || "single",
    });

    const isSelfRecipient = comboData.recipientIds.some((id: string) => String(id) === String(comboData.senderId));
    if (isSelfRecipient) {
      const diamondCredit = Math.floor(totalCost * 0.1);
      if (diamondCredit > 0) {
        await addDiamondsToLocalDB(diamondCredit);
        await recordDiamondTransaction("Diamonds received — " + comboData.giftName, diamondCredit);
        window.dispatchEvent(new CustomEvent("hurry:diamonds-updated", { detail: { amount: diamondCredit } }));
      }
    }

    window.dispatchEvent(new CustomEvent("hurry:lucky-slider", {
      detail: {
        roomId: comboData.roomId,
        senderId: comboData.senderId,
        senderName: comboData.senderName,
        senderImage: comboData.senderImage,
        recipientName: comboData.recipientName,
        giftImage: comboData.giftImage,
        multiplier: nextMultiplier,
        luckyGift: true,
        luckyReturnAmount: luckyReturnAmount,
        winTimes: winTimes,
        recipientMode: comboData.recipientMode || "single",
      },
    }));

    setBusy(false);
  };

  if (!comboData) return null;

  return (
    <>
      <style>{`
        @keyframes comboPulseRed {
          0% { transform: scale(1); opacity: 0.5; }
          100% { transform: scale(1.5); opacity: 0; }
        }
        .combo-wave-red {
          position: absolute;
          inset: 0;
          border-radius: 50%;
          background: radial-gradient(circle, rgba(255, 51, 102, 0.6) 0%, rgba(255, 51, 102, 0.1) 100%);
          animation: comboPulseRed 1.8s infinite cubic-bezier(0.2, 0.8, 0.4, 1);
          pointer-events: none;
          z-index: 1;
        }
      `}</style>

      <div
        style={{
          position: "fixed",
          bottom: "6vh",
          right: "5vw",
          zIndex: 2147483001,
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          gap: 4,
        }}
      >
        <div style={{ position: "relative", width: 104, height: 104, display: "flex", alignItems: "center", justifyContent: "center" }}>

          <div className="combo-wave-red" style={{ animationDelay: "0s" }} />
          <div className="combo-wave-red" style={{ animationDelay: "0.6s" }} />

          <button
            onClick={handleComboClick}
            className="active:scale-95 transition-transform"
            style={{
              position: "relative",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              width: 96,
              height: 96,
              borderRadius: "50%",
              border: "none",
              outline: "none",
              cursor: "pointer",
              background: "radial-gradient(circle at center, #ff4d79 0%, #ff1a4d 100%)",
              boxShadow: "0 6px 26px rgba(255, 26, 77, 0.75)",
              zIndex: 2,
            }}
          >
            <span
              style={{
                color: "#ffffff",
                fontWeight: 700,
                fontSize: 18,
                textShadow: "0 1px 3px rgba(0,0,0,0.5)",
                letterSpacing: "0.5px",
                zIndex: 3,
              }}
            >
              Combo
            </span>

            <svg
              viewBox="0 0 96 96"
              style={{
                position: "absolute",
                inset: 0,
                width: "100%",
                height: "100%",
                transform: "rotate(-90deg)",
                pointerEvents: "none",
                zIndex: 3,
                padding: 8,
                boxSizing: "border-box",
              }}
            >
              <circle
                cx="48"
                cy="48"
                r="42"
                fill="none"
                stroke="rgba(255, 255, 255, 0.35)"
                strokeWidth="1.5"
              />
              <circle
                cx="48"
                cy="48"
                r="42"
                fill="none"
                stroke="#ffffff"
                strokeWidth="1.5"
                strokeDasharray="263.89"
                strokeDashoffset={263.89 - (263.89 * timeLeft) / 5}
                strokeLinecap="round"
                style={{ transition: "stroke-dashoffset 1s linear" }}
              />
            </svg>
          </button>
        </div>
      </div>
    </>
  );
}

// ==========================================================
// EMBEDDED LUCKY GIFT FLY
type LuckyGiftAnimationProps = { roomId: string };

function findTargetAvatar(data: any): HTMLImageElement | null {
  const seatNumber = Number(data?.seatNumber || 0);
  const accountId = String(data?.user?.accountId || "");
  const name = String(data?.targetName || data?.user?.name || "");

  if (Number.isFinite(seatNumber) && seatNumber > 0) {
    const el = document.querySelector(`img[data-hurry-seat="${seatNumber}"]`) as HTMLImageElement | null;
    if (el) return el;
  }

  if (accountId) {
    const els = document.querySelectorAll("img[data-hurry-account]");
    for (const el of Array.from(els)) {
      if (String(el.getAttribute("data-hurry-account") || "") === accountId) {
        return el as HTMLImageElement;
      }
    }
  }

  if (name) {
    const els = document.querySelectorAll("img[data-hurry-account]");
    for (const el of Array.from(els)) {
      if (String(el.getAttribute("alt") || "") === name) return el as HTMLImageElement;
    }
  }
  return null;
}

const embeddedSeenLuckyEvents = new Set<string>();

function animateEmbeddedLuckyGift(data: any, roomId: string) {
  if (typeof document === "undefined") return;
  if (String(data?.roomId || "") !== String(roomId)) return;
  if (data?.action !== "lucky_image") return;

  const eventId = String(
    data?.eventId ||
      `${data?.roomId || ""}-${data?.seatNumber || ""}-${data?.timestamp || ""}-${data?.src || ""}`
  );
  if (embeddedSeenLuckyEvents.has(eventId)) return;
  embeddedSeenLuckyEvents.add(eventId);
  window.setTimeout(() => embeddedSeenLuckyEvents.delete(eventId), 5000);

  const src = String(data?.src || "");
  if (!src) return;

  let attempts = 0;
  let cancelled = false;

  const findAndRun = () => {
    if (cancelled) return;
    const target = findTargetAvatar(data);
    if (!target) {
      if (attempts++ < 20) window.setTimeout(findAndRun, 75);
      return;
    }

    const rect = target.getBoundingClientRect();
    if (rect.width <= 0 || rect.height <= 0) return;

    const flyer = document.createElement("img");
    flyer.src = src;
    flyer.alt = "";
    flyer.setAttribute("aria-hidden", "true");
    flyer.draggable = false;

    const size = Math.max(48, Math.min(82, Math.round(rect.width * 1.35)));
    const half = size / 2;

    const startX = window.innerWidth / 2;
    const startY = window.innerHeight - Math.max(18, size / 2);
    const middleX = window.innerWidth / 2;
    const middleY = window.innerHeight * 0.52;
    const endX = rect.left + rect.width / 2;
    const endY = rect.top + rect.height / 2;

    const travelDuration =
      Number(data?.duration) > 0
        ? Math.min(1800, Math.max(600, Number(data.duration)))
        : 900;

    Object.assign(flyer.style, {
      position: "fixed",
      left: "0",
      top: "0",
      width: `${size}px`,
      height: `${size}px`,
      objectFit: "contain",
      pointerEvents: "none",
      userSelect: "none",
      zIndex: "2147483647",
      opacity: "1",
      transformOrigin: "bottom center",
      transform: `translate3d(${startX - half}px,${startY - half}px,0) scale(1)`,
      willChange: "transform",
    });

    const start = () => {
      if (cancelled) {
        flyer.remove();
        return;
      }
      document.documentElement.appendChild(flyer);

      requestAnimationFrame(() => {
        requestAnimationFrame(() => {
          flyer.style.transition = `transform ${Math.round(travelDuration / 2)}ms cubic-bezier(.18,.72,.32,1)`;
          flyer.style.transform =
            `translate3d(${middleX - half}px,${middleY - half}px,0) scale(1)`;

          window.setTimeout(() => {
            if (cancelled) {
              flyer.remove();
              return;
            }

            flyer.style.transition = `transform ${Math.round(travelDuration / 2)}ms cubic-bezier(.18,.72,.32,1)`;
            flyer.style.transform =
              `translate3d(${endX - half}px,${endY - half}px,0) scale(0.55)`;

            window.setTimeout(() => flyer.remove(), Math.round(travelDuration / 2) + 120);
          }, 700);
        });
      });
    };

    flyer.onload = start;
    flyer.onerror = () => flyer.remove();
    if (flyer.complete && flyer.naturalWidth > 0) start();
  };

  requestAnimationFrame(findAndRun);
}

export function LuckyGiftAnimation({ roomId }: LuckyGiftAnimationProps) {
  const roomIdRef = useRef(roomId);
  roomIdRef.current = roomId;

  useEffect(() => {
    const handler = (data: any) => animateEmbeddedLuckyGift(data, roomIdRef.current);
    const localHandler = (event: Event) => {
      animateEmbeddedLuckyGift((event as CustomEvent).detail, roomIdRef.current);
    };

    socket.on("room_seat_action", handler);
    window.addEventListener("hurry:lucky-image", localHandler);

    return () => {
      socket.off("room_seat_action", handler);
      window.removeEventListener("hurry:lucky-image", localHandler);
    };
  }, []);

  return null;
  }
