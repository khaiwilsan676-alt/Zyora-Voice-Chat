"use client";

import React, { useState, useRef, useEffect } from "react";

interface EmojiPickerProps {
  onClose?: () => void;
  onSelectEmoji?: (emoji: { id: string; name: string; src: string }) => void;
}

function PausedGifItem({
  src,
  name,
  onClick,
}: {
  src: string;
  name: string;
  onClick: () => void;
}) {
  const [isPlaying, setIsPlaying] = useState(false);
  const canvasRef = useRef<HTMLCanvasElement | null>(null);

  useEffect(() => {
    let isMounted = true;
    const img = new Image();
    img.crossOrigin = "anonymous";
    img.src = encodeURI(src);

    img.onload = () => {
      if (!isMounted || !canvasRef.current) return;
      const canvas = canvasRef.current;
      canvas.width = 120;
      canvas.height = 120;
      const ctx = canvas.getContext("2d");
      if (ctx) {
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
      }
    };

    return () => {
      isMounted = false;
    };
  }, [src]);

  return (
    <button
      type="button"
      onClick={onClick}
      onMouseEnter={() => setIsPlaying(true)}
      onMouseLeave={() => setIsPlaying(false)}
      onTouchStart={() => setIsPlaying(true)}
      onTouchEnd={() => setIsPlaying(false)}
      className="w-full flex flex-col items-center justify-center gap-1.5 p-1 transition-transform active:scale-90 bg-transparent border-0 outline-none"
    >
      <div className="w-12 h-12 flex items-center justify-center relative">
        <canvas
          ref={canvasRef}
          className={`w-full h-full object-contain pointer-events-none ${
            isPlaying ? "hidden" : "block"
          }`}
        />
        {isPlaying && (
          <img
            src={encodeURI(src)}
            alt={name}
            className="w-full h-full object-contain pointer-events-none select-none"
          />
        )}
      </div>
      <span className="text-[11px] font-medium text-white/90 text-center tracking-tight truncate w-full drop-shadow">
        {name}
      </span>
    </button>
  );
}

export default function EmojiPicker({ onClose, onSelectEmoji }: EmojiPickerProps) {
  const [activeTab, setActiveTab] = useState<"emojis" | "premium">("emojis");

  const gifStickers = [
    { id: "laugh", name: "Laugh", src: "/512.gif" },
    { id: "sad", name: "Sad", src: "/512 (6).gif" },
    { id: "love", name: "Sleep", src: "/512 (3).gif" },
    { id: "thinking", name: "Thinking", src: "/512 (2).gif" },
    { id: "party", name: "Party", src: "/512 (16).gif" },
    { id: "loving", name: "Loving", src: "/512 (15).gif" },
    { id: "smart", name: "Smart", src: "/512 (13).gif" },
    { id: "irritating", name: "Irritating", src: "/512 (12).gif" },
    { id: "rolling", name: "Rolling", src: "/512 (10).gif" },
    { id: "unamused", name: "Unamused", src: "/512 (11).gif" },
    { id: "pleading", name: "Pleading", src: "/512 (4).gif" },
    { id: "hug", name: "Hug", src: "/512 (8).gif" },
    { id: "kiss", name: "Kiss-R", src: "/512 (14).gif" },
    { id: "Angery", name: " Angery", src: "/512 (1).gif" }, 
  ];

  const handleGifClick = (gif: { id: string; name: string; src: string }) => {
    if (onSelectEmoji) {
      onSelectEmoji(gif);
    }
    if (onClose) {
      onClose();
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex flex-col justify-end bg-transparent pointer-events-none">
      {/* Outside click area */}
      <div 
        className="flex-1 w-full pointer-events-auto" 
        onClick={onClose} 
        aria-hidden="true" 
      />

      {/* Bottom Sheet Modal */}
      <div 
        className="h-[42vh] w-full text-white flex flex-col justify-between rounded-none shadow-2xl px-4 pt-3 pb-4 pointer-events-auto relative overflow-hidden bg-cover bg-center"
        style={{
          backgroundImage: "url('/1787514857468~2.jpg')",
          backgroundColor: "#121212",
        }}
      >
        <div className="absolute inset-0 bg-black/25 pointer-events-none" />

        {/* TOP MIDDLE TAB BAR ONLY (No bottom border line) */}
        <div className="relative z-10 flex items-center justify-center gap-8 pb-2 flex-shrink-0">
          <button
            type="button"
            onClick={() => setActiveTab("emojis")}
            className={`relative pb-1.5 text-sm font-bold transition-all ${
              activeTab === "emojis" ? "text-white scale-105" : "text-white/60 hover:text-white/80"
            }`}
          >
            Emojis
            {activeTab === "emojis" && (
              <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-white rounded-full transition-all" />
            )}
          </button>

          <button
            type="button"
            onClick={() => setActiveTab("premium")}
            className={`relative pb-1.5 text-sm font-bold transition-all ${
              activeTab === "premium" ? "text-white scale-105" : "text-white/60 hover:text-white/80"
            }`}
          >
            Premium
            {activeTab === "premium" && (
              <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-white rounded-full transition-all" />
            )}
          </button>
        </div>

        {/* Content Body */}
        <div className="relative z-10 flex-1 overflow-y-auto py-3 scrollbar-none">
          {activeTab === "emojis" ? (
            <div className="grid grid-cols-4 gap-y-4 gap-x-2 place-items-center">
              {gifStickers.map((gif) => (
                <PausedGifItem
                  key={gif.id}
                  src={gif.src}
                  name={gif.name}
                  onClick={() => handleGifClick(gif)}
                />
              ))}
            </div>
          ) : (
            <div className="h-full flex items-center justify-center text-white/70 text-xs font-semibold">
              Premium Emojis Coming Soon
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
