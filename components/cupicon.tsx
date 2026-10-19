"use client";

import React from "react";
import Image from "next/image";

const listData = [
  { id: 4, name: "Buggu", avatar: "/default-avatar.png", level: 65, coins: 308100 },
  { id: 5, name: "yamat", avatar: "/default-avatar.png", level: 62, coins: 100000 },
  { id: 6, name: "SUFIYAN", avatar: "/default-avatar.png", level: 59, coins: 9000 },
  { id: 7, name: "SK MARK", avatar: "/default-avatar.png", level: 51, coins: 2000 },
];

const CoinIcon = () => (
  <div className="w-3.5 h-3.5 bg-yellow-400 rounded-full flex items-center justify-center border border-yellow-200 shadow-sm mr-1">
    <div className="w-2 h-2 bg-yellow-200 rounded-full flex items-center justify-center">
      <span className="text-[6px] text-yellow-600 font-bold">★</span>
    </div>
  </div>
);

const formatCount = (n: number): string => {
  if (n >= 1_000_000) return (n / 1_000_000).toFixed(1).replace(/\.0$/, '') + 'M';
  if (n >= 1_000) return (n / 1_000).toFixed(1).replace(/\.0$/, '') + 'K';
  return String(n);
};

interface CupIconProps {
  onBack?: () => void;
  count?: number;
}

export default function CupIcon({ onBack, count = 0 }: CupIconProps) {
  return (
    <div className="absolute inset-0 z-[11000] flex items-end justify-center pointer-events-none">

      {/* Invisible backdrop - click to close */}
      <div
        className="absolute inset-0 pointer-events-auto"
        style={{ background: 'transparent' }}
        onClick={onBack}
      />

      {/* BOTTOM 50vh SHEET — sirf background image, corner 3px */}
      <div
        className="relative w-full max-w-md h-[50vh] overflow-hidden pointer-events-auto"
        style={{ borderRadius: '3px 3px 0 0' }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* BACKGROUND IMAGE */}
        <Image
          src="/file_00000000403481f5ae20a522b191c8fa.png"
          alt="Background"
          fill
          className="object-cover object-top z-0"
          priority
        />
        <div className="absolute inset-0 bg-gradient-to-b from-[#8f28ff]/40 to-[#d25aff]/70 mix-blend-overlay z-0" />

        {/* CONTENT — image ke upar floating */}
        <div className="relative z-10 flex flex-col h-full">

          {/* ========================================== */}
          {/* HEADER — sabse upar */}
          {/* ========================================== */}
          <div className="flex items-center justify-between px-4 pt-4 pb-2 flex-shrink-0">
            <div className="flex items-center bg-white/20 rounded-full px-2 py-1 backdrop-blur-sm">
              <CoinIcon />
              <span className="text-yellow-300 text-xs font-bold">{formatCount(count)}</span>
            </div>

            <div className="flex items-center gap-1">
              <span className="text-white font-semibold text-sm drop-shadow-md">Contribution list</span>
            </div>

            <div className="flex items-center text-yellow-300 text-xs font-bold bg-white/20 rounded-full px-2 py-1 backdrop-blur-sm">
              TOP4 &gt;
            </div>
          </div>

          {/* ========================================== */}
          {/* TAB */}
          {/* ========================================== */}
          <div className="flex items-center justify-between px-4 flex-shrink-0">
            <button className="bg-white text-[#9922ff] font-bold text-xs px-4 py-1 rounded-full shadow-md">
              Daily list
            </button>
            <button className="text-white/90 text-xs font-medium flex items-center gap-1">
              Today <span className="text-xs">&lt;</span>
            </button>
          </div>

          {/* ========================================== */}
          {/* PODIUM (Top 3) — frames ke saath */}
          {/* ========================================== */}
          <div className="relative mt-3 mb-1 flex justify-center items-end px-2 h-32 flex-shrink-0">

            {/* RANK 3 (Left) */}
            <div className="flex flex-col items-center relative w-[30%] z-10 translate-x-3">
              <div className="flex items-center mb-0.5">
                <CoinIcon />
                <span className="text-yellow-300 font-bold text-[10px]">700000</span>
              </div>
              <div className="relative w-12 h-12 flex items-center justify-center">
                <div className="w-9 h-9 rounded-full overflow-hidden bg-gray-300 absolute">
                  <Image src="/default-avatar.png" alt="Rank 3" fill className="object-cover" />
                </div>
                <Image src="/IMG_20260923_115801.png" alt="Frame 3" fill className="object-contain z-10" />
              </div>
              <div className="bg-white/90 rounded text-[#9922ff] text-[8px] font-bold px-1.5 py-0.5 mt-0.5">HONEY</div>
              <div className="bg-gradient-to-r from-purple-800 to-indigo-900 border border-yellow-400 text-white text-[8px] px-1 rounded-sm mt-0.5">LV.56</div>
            </div>

            {/* RANK 1 (Center) */}
            <div className="flex flex-col items-center relative w-[40%] z-20 -translate-y-4">
              <div className="flex items-center mb-0.5">
                <CoinIcon />
                <span className="text-yellow-300 font-bold text-xs drop-shadow-md">175901000</span>
              </div>
              <div className="relative w-16 h-16 flex items-center justify-center">
                <div className="w-12 h-12 rounded-full overflow-hidden bg-gray-300 absolute">
                  <Image src="/default-avatar.png" alt="Rank 1" fill className="object-cover" />
                </div>
                <Image src="/IMG_20260923_115819.png" alt="Frame 1" fill className="object-contain z-10" />
              </div>
              <span className="text-yellow-400 font-bold text-[10px] drop-shadow-md mt-0.5">Guns</span>
              <div className="flex gap-1 mt-0.5">
                <div className="bg-gradient-to-r from-purple-800 to-indigo-900 border border-yellow-400 text-yellow-400 text-[8px] px-1 rounded-sm font-bold">LV.84</div>
                <div className="bg-teal-500 text-white text-[8px] px-1 rounded-sm">SVIP1</div>
              </div>
            </div>

            {/* RANK 2 (Right) */}
            <div className="flex flex-col items-center relative w-[30%] z-10 -translate-x-3">
              <div className="flex items-center mb-0.5">
                <CoinIcon />
                <span className="text-yellow-300 font-bold text-[10px]">9252800</span>
              </div>
              <div className="relative w-14 h-14 flex items-center justify-center">
                <div className="w-10 h-10 rounded-full overflow-hidden bg-gray-300 absolute">
                  <Image src="/default-avatar.png" alt="Rank 2" fill className="object-cover" />
                </div>
                <Image src="/IMG_20260923_115834.png" alt="Frame 2" fill className="object-contain z-10" />
              </div>
              <div className="flex items-center gap-1 mt-0.5">
                <span className="text-black text-[9px]">🖤</span>
                <span className="text-yellow-400 font-bold text-[10px] drop-shadow-md">Divya</span>
              </div>
              <div className="flex gap-1 mt-0.5">
                <div className="bg-gradient-to-r from-purple-800 to-indigo-900 border border-yellow-400 text-yellow-400 text-[8px] px-1 rounded-sm font-bold">LV.102</div>
                <div className="bg-red-500 text-white text-[8px] px-1 rounded-sm">SVIP5</div>
              </div>
            </div>

          </div>

          {/* ========================================== */}
          {/* LIST (Ranks 4+) — BINA LINE */}
          {/* ========================================== */}
          <div className="flex-1 overflow-y-auto scrollbar-none pt-2 px-4 pb-4">
            {listData.map((user) => (
              <div key={user.id} className="flex items-center justify-between py-2">
                <div className="flex items-center gap-3">
                  <span className="text-white/80 font-medium text-sm w-4 text-center drop-shadow-md">
                    {user.id}
                  </span>
                  <div className="relative w-9 h-9 rounded-full overflow-hidden border border-white/50">
                    <Image src={user.avatar} alt={user.name} fill className="object-cover" />
                  </div>
                  <div className="flex flex-col">
                    <span className="text-white font-bold text-xs drop-shadow-md">{user.name}</span>
                    <div className="bg-gradient-to-r from-green-800 to-green-600 border border-yellow-400 text-yellow-400 text-[8px] px-1.5 rounded-sm font-bold w-fit mt-0.5">
                      LV.{user.level}
                    </div>
                  </div>
                </div>
                <div className="flex items-center">
                  <CoinIcon />
                  <span className="text-yellow-300 font-semibold text-xs drop-shadow-md">{user.coins}</span>
                </div>
              </div>
            ))}
          </div>

        </div>

        {/* Safe area bottom */}
        <div
          className="absolute bottom-0 left-0 right-0 z-20"
          style={{ paddingBottom: 'env(safe-area-inset-bottom, 0px)' }}
        />
      </div>
    </div>
  );
}
