
'use client';

import React from 'react';

interface SeatActionsProps {
  isOpen: boolean;
  onClose: () => void;
  seatNumber: number | null;
  seatData?: {
    isLocked?: boolean;
    isOccupied?: boolean;
    isMuted?: boolean;
  };
  isMySeat: boolean;
  isTakenByOther: boolean;
  onTakeSeat: () => void;
  onLeaveSeat: () => void;
  onToggleMute: () => void;
  onToggleLock: () => void;
  onInvite: () => void;
}

export default function SeatActions({
  isOpen,
  onClose,
  seatNumber,
  seatData,
  isMySeat,
  isTakenByOther,
  onTakeSeat,
  onLeaveSeat,
  onToggleMute,
  onToggleLock,
  onInvite
}: SeatActionsProps) {
  if (!isOpen || seatNumber === null) return null;

  return (
    <div className="absolute inset-0 z-30 flex items-end justify-center">
      <div className="absolute inset-0 bg-black/30" onClick={onClose} />
      <div 
        className="relative bg-white/95 backdrop-blur-xl w-full max-w-md rounded-t-3xl shadow-2xl px-6 py-4 animate-slide-up max-h-[25vh] overflow-y-auto" 
        onClick={(e) => e.stopPropagation()}
      >
        <div className="space-y-2">
          {!isTakenByOther && !isMySeat && (
            <button 
              onClick={onTakeSeat} 
              disabled={seatData?.isLocked && !seatData?.isOccupied} 
              className="w-full py-2.5 text-black font-medium text-base hover:bg-gray-100 rounded-lg transition-colors disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer"
            >
              Take Mic
            </button>
          )}
          {isMySeat && (
            <button 
              onClick={onLeaveSeat} 
              className="w-full py-2.5 text-black font-medium text-base hover:bg-gray-100 rounded-lg transition-colors cursor-pointer"
            >
              Leave Seat
            </button>
          )}
          {isMySeat && (
            <button 
              onClick={onToggleMute} 
              className="w-full py-2.5 text-black font-medium text-base hover:bg-gray-100 rounded-lg transition-colors cursor-pointer"
            >
              {seatData?.isMuted ? 'Unmute' : 'Mute'}
            </button>
          )}
          <button 
            onClick={onToggleLock} 
            className="w-full py-2.5 text-black font-medium text-base hover:bg-gray-100 rounded-lg transition-colors cursor-pointer"
          >
            {seatData?.isLocked ? 'Unlock Mic' : 'Lock Mic'}
          </button>
          <button 
            onClick={onInvite} 
            className="w-full py-2.5 text-black font-medium text-base hover:bg-gray-100 rounded-lg transition-colors cursor-pointer"
          >
            Invite
          </button>
        </div>
      </div>
    </div>
  );
}
