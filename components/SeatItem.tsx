
'use client';

import React from 'react';
import WhiteColorRemovalShader from './WhiteColorRemovalShader';
import { useLocalParticipant, useRemoteParticipants } from "@livekit/components-react";

interface Seat {
  number: number;
  isOccupied: boolean;
  isLocked?: boolean;
  user?: { name: string; image: string; accountId: string };
  isMuted?: boolean;
  isSpeaking?: boolean;
}

interface SeatItemProps {
  seatNumber: number;
  seatData?: Seat;
  onClick: (e: React.MouseEvent) => void;
  onAvatarClick?: (e: React.MouseEvent) => void;
  accountId: string;
  roomOwnerId: string;
}

export default function SeatItem({ 
  seatNumber, 
  seatData, 
  onClick, 
  onAvatarClick, 
  accountId, 
  roomOwnerId 
}: SeatItemProps) {
  const isLocked = seatData?.isLocked ?? false;
  const isOccupied = seatData?.isOccupied ?? false;
  const isSpeaking = seatData?.isSpeaking ?? false;
  const isMuted = seatData?.isMuted ?? false;
  const user = seatData?.user;
  const isRoomOwnerSeat = isOccupied && user?.accountId === roomOwnerId;

  // Remote participants hook to detect speaking status from LiveKit
  const remoteParticipants = useRemoteParticipants();
  const { localParticipant } = useLocalParticipant();

  const isUserSpeaking = React.useMemo(() => {
    if (!isOccupied || isMuted) return false;
    if (user?.accountId === accountId) {
      return localParticipant?.isSpeaking ?? false;
    } else {
      const p = remoteParticipants.find(rp => rp.identity === user?.accountId);
      return p?.isSpeaking ?? false;
    }
  }, [isOccupied, isMuted, user?.accountId, accountId, localParticipant, remoteParticipants]);

  const activeSpeaking = isSpeaking || isUserSpeaking;

  return (
    <div className="flex flex-col items-center gap-1 cursor-pointer" onClick={onClick}>
      <div className="relative overflow-visible">
        {activeSpeaking && (
          <>
            <div className="absolute rounded-full bg-blue-400 wave-ripple pointer-events-none" style={{ width: '60px', height: '60px', left: '50%', top: '50%', zIndex: 0 }} />
            <div className="absolute rounded-full bg-blue-500 wave-ripple-delayed pointer-events-none" style={{ width: '60px', height: '60px', left: '50%', top: '50%', zIndex: 0 }} />
            <div className="absolute rounded-full pointer-events-none" style={{ width: '64px', height: '64px', left: '50%', top: '50%', zIndex: 0, backgroundColor: 'rgba(59, 130, 246, 0.35)', filter: 'blur(6px)', animation: 'voicePulse 1.2s ease-in-out infinite' }} />
          </>
        )}
        <div className={`w-[60px] h-[60px] rounded-full flex items-center justify-center shrink-0 relative z-10 bg-[rgba(125,143,168,0.32)] backdrop-blur-[12px] border transition-all duration-300 hover:scale-105 pointer-events-auto ${activeSpeaking ? 'border-blue-400 shadow-[0_0_15px_rgba(59,130,246,0.8)]' : 'border-[rgba(210,220,235,0.55)] shadow-[inset_0_1px_1.5px_rgba(255,255,255,0.45),inset_0_-1px_1.5px_rgba(0,0,0,0.18),inset_0_0_22px_rgba(255,255,255,0.12),0_8px_32px_rgba(0,0,0,0.28)]'}`}>
          {isLocked ? (
            <div className="w-8 h-8 flex items-center justify-center">
              <svg viewBox="0 0 24 24" className="w-full h-full fill-none stroke-[#94a7be] stroke-[2] stroke-linecap-round stroke-linejoin-round">
                <rect x="5" y="11" width="14" height="10" rx="2.5" />
                <path d="M8 11V7a4 4 0 0 1 8 0v4" />
                <circle cx="12" cy="16" r="1.2" fill="#94a7be" />
              </svg>
            </div>
          ) : isOccupied && user ? (
            <>
              <div className="relative w-full h-full rounded-full overflow-visible">
                <img
                  src={user.image || "/default-avatar.png"}
                  alt={user.name}
                  className="w-full h-full rounded-full object-cover pointer-events-none"
                  draggable={false}
                  onError={(e) => { (e.target as HTMLImageElement).src = "/default-avatar.png" }}
                  onClick={onAvatarClick}
                  style={{ 
                    cursor: 'pointer', 
                    pointerEvents: 'auto',
                    borderRadius: '50%',
                    position: 'relative',
                    zIndex: 1
                  }}
                />
                <div 
                  className="absolute pointer-events-none"
                  style={{
                    top: '50%',
                    left: '50%',
                    transform: 'translate(-50%, -50%)',
                    width: '160%',
                    height: '160%',
                    zIndex: 2,
                    overflow: 'visible',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                  }}
                >
                  <WhiteColorRemovalShader
                    imageSrc="/1786867564769.png"
                    threshold={0.85}
                    className="w-full h-full"
                    style={{
                      width: '100%',
                      height: '100%',
                      objectFit: 'contain',
                      maxWidth: 'none',
                      maxHeight: 'none',
                      overflow: 'visible',
                    }}
                  />
                </div>
              </div>
              {isMuted && (
                <div className={`absolute -right-1 -bottom-1 w-5 h-5 rounded-full flex items-center justify-center shadow-md pointer-events-none z-10 ${user.accountId === accountId ? 'bg-gray-400' : 'bg-red-500'}`}>
                  <svg viewBox="0 0 24 24" className="w-3 h-3 fill-none stroke-white stroke-[3] stroke-linecap-round stroke-linejoin-round">
                    <line x1="1" y1="1" x2="23" y2="23" />
                    <path d="M9 9v3a3 3 0 0 0 5.12 2.12M15 9.34V4a3 3 0 0 0-5.94-.6" />
                    <path d="M17 16.95A7 7 0 0 1 5 12v-2m14 0v2a7 7 0 0 1-.11 1.23" />
                  </svg>
                </div>
              )}
            </>
          ) : (
            <div className="w-[58%] h-[58%] flex items-center justify-center pointer-events-none relative">
              <svg viewBox="0 0 100 100" width="100%" height="100%" xmlns="http://www.w3.org/2000/svg" style={{ overflow: "visible", display: "block" }}>
                <g fill="none" stroke="#94a7be" strokeWidth="7" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M 28 44 Q 28 74 50 74 Q 72 74 72 44" />
                  <path d="M 50 74 L 50 86" />
                  <path d="M 38 90 L 62 90" />
                </g>
                <g fill="#94a7be" stroke="#5a6d89" strokeWidth="2.8" strokeLinejoin="round" strokeLinecap="round" transform="translate(0, 6)">
                  <path d="M 36 18 Q 36 10 50 10 Q 64 10 64 18 L 64 42 Q 64 52 50 52 Q 36 52 36 42 Z" />
                </g>
              </svg>
              {isMuted && (
                <div className="absolute -right-2 -bottom-2 w-5 h-5 rounded-full bg-red-500 flex items-center justify-center shadow-md">
                  <svg viewBox="0 0 24 24" className="w-3 h-3 fill-none stroke-white stroke-[3] stroke-linecap-round stroke-linejoin-round">
                    <line x1="1" y1="1" x2="23" y2="23" />
                    <path d="M9 9v3a3 3 0 0 0 5.12 2.12M15 9.34V4a3 3 0 0 0-5.94-.6" />
                    <path d="M17 16.95A7 7 0 0 1 5 12v-2m14 0v2a7 7 0 0 1-.11 1.23" />
                  </svg>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
      <span className="text-[10px] font-medium text-white/80 pointer-events-none flex items-center gap-1">
        {isRoomOwnerSeat && (
          <span className="w-3 h-3 rounded-full bg-blue-500 flex items-center justify-center inline-flex">
            <svg viewBox="0 0 24 24" className="w-2 h-2 fill-white">
              <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
            </svg>
          </span>
        )}
        {isLocked ? `No ${seatNumber}` : (isOccupied && user ? user.name : `No ${seatNumber}`)}
      </span>
    </div>
  );
}
