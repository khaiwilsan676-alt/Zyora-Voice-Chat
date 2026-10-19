
'use client';

import React from 'react';

interface ExitMenuProps {
  isOpen: boolean;
  onClose: () => void;
  onKeep: () => void;
  onExit: () => void;
}

export default function ExitMenu({ isOpen, onClose, onKeep, onExit }: ExitMenuProps) {
  if (!isOpen) return null;

  return (
    <div className="absolute inset-0 z-20 flex items-center justify-center bg-black/40" onClick={onClose}>
      <div className="flex flex-col items-center gap-8" onClick={(e) => e.stopPropagation()}>
        <div className="flex flex-col items-center gap-2">
          <button 
            onClick={onKeep} 
            className="w-20 h-20 rounded-full bg-blue-500 hover:bg-blue-600 flex items-center justify-center transition-all duration-200 shadow-lg shadow-blue-500/30 cursor-pointer"
          >
            <svg viewBox="0 0 24 24" className="h-8 w-8 fill-none stroke-white stroke-[2.5] stroke-linecap-round stroke-linejoin-round">
              <line x1="5" y1="12" x2="19" y2="12" />
            </svg>
          </button>
          <span className="text-white font-semibold text-base">Keep</span>
        </div>
        <div className="flex flex-col items-center gap-2">
          <button 
            onClick={onExit} 
            className="w-20 h-20 rounded-full bg-blue-500 hover:bg-blue-600 flex items-center justify-center transition-all duration-200 shadow-lg shadow-blue-500/30 cursor-pointer"
          >
            <svg viewBox="0 0 24 24" className="h-8 w-8 fill-none stroke-white stroke-[2] stroke-linecap-round stroke-linejoin-round">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
              <polyline points="16 17 21 12 16 7" />
              <line x1="21" y1="12" x2="9" y2="12" />
            </svg>
          </button>
          <span className="text-white/70 font-medium text-sm">Exit</span>
        </div>
      </div>
      <button 
        onClick={onClose} 
        className="absolute bottom-8 left-1/2 -translate-x-1/2 w-10 h-10 rounded-full bg-white/10 hover:bg-white/20 border border-white/20 flex items-center justify-center transition-all duration-200 cursor-pointer"
      >
        <svg viewBox="0 0 24 24" className="h-5 w-5 fill-none stroke-white stroke-[2.5] stroke-linecap-round stroke-linejoin-round">
          <line x1="18" y1="6" x2="6" y2="18" />
          <line x1="6" y1="6" x2="18" y2="18" />
        </svg>
      </button>
    </div>
  );
}
