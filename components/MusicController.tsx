'use client';

import React from 'react';

interface MusicTrack {
  id: string;
  name: string;
  url: string;
}

interface MusicControllerProps {
  state: 'hidden' | 'full' | 'minimized';
  currentTrack: MusicTrack | null;
  isPlaying: boolean;
  volume: number;
  currentTime: number;
  duration: number;
  onTogglePlay: () => void;
  onVolumeChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  onProgressChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  onNextTrack: () => void;
  onPrevTrack: () => void;
  onClose: () => void;
  onMinimize: () => void;
  onMaximize: () => void;
}

export default function MusicController({
  state,
  currentTrack,
  isPlaying,
  volume,
  currentTime,
  duration,
  onTogglePlay,
  onVolumeChange,
  onProgressChange,
  onNextTrack,
  onPrevTrack,
  onClose,
  onMinimize,
  onMaximize
}: MusicControllerProps) {
  const formatTime = (seconds: number) => {
    if (isNaN(seconds)) return '0:00';
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  if (state === 'hidden' || !currentTrack) return null;

  if (state === 'minimized') {
    return (
      <div
        className="fixed z-[45]"
        style={{ bottom: '8vh', right: '12px' }}
        onClick={(e) => e.stopPropagation()}
      >
        <button
          onClick={onMaximize}
          className="relative rounded-full overflow-hidden shadow-lg cursor-pointer transition-transform hover:scale-105"
          style={{
            width: '56px',
            height: '56px',
            border: '3px solid black',
            backgroundColor: 'black',
          }}
          aria-label="Maximize music controller"
        >
          <div className="absolute inset-0 flex items-center justify-center music-minimize-icon">
            <img src="/IMG_20260815_133309.png" alt="Music" className="w-15 h-15 object-contain" />
          </div>
        </button>
      </div>
    );
  }

  return (
    <div 
      className="fixed left-1/2 transform -translate-x-1/2 z-[45] w-full max-w-md px-4"
      style={{ bottom: '10vh' }}
      onClick={(e) => e.stopPropagation()}
    >
      <div 
        className="relative rounded-2xl overflow-hidden bg-black/90 backdrop-blur-md border border-white/10"
        style={{
          padding: '16px',
          boxShadow: '0 8px 32px rgba(0,0,0,0.6)',
        }}
      >
        {/* LEFT SIDE BUTTON - POWER ICON (Close) */}
        <button
          onClick={onClose}
          className="absolute top-2 left-2 p-1.5 rounded-full bg-white/10 hover:bg-white/20 transition-colors cursor-pointer z-10"
          aria-label="Close music controller"
        >
          <svg viewBox="0 0 24 24" className="w-5 h-5 fill-none stroke-white stroke-[2.5] stroke-linecap-round stroke-linejoin-round">
            <path d="M18.36 6.64a9 9 0 1 1-12.72 0" />
            <line x1="12" y1="2" x2="12" y2="12" />
          </svg>
        </button>

        {/* RIGHT SIDE BUTTON - DOUBLE ARROW ICON (Minimize) */}
        <button
          onClick={onMinimize}
          className="absolute top-2 right-2 p-1.5 rounded-full bg-white/10 hover:bg-white/20 transition-colors cursor-pointer z-10"
          aria-label="Minimize music controller"
        >
          <svg viewBox="0 0 24 24" className="w-5 h-5 fill-none stroke-white stroke-[2.5] stroke-linecap-round stroke-linejoin-round">
            <line x1="7" y1="17" x2="17" y2="7" />
            <polyline points="7 7 17 7 17 17" />
          </svg>
        </button>

        <div className="text-center mb-3 mt-6">
          <p className="text-white text-sm font-semibold truncate px-8">
            {currentTrack.name}
          </p>
        </div>

        <div className="px-2 mb-3">
          <input
            type="range"
            min="0"
            max={duration || 0}
            step="0.1"
            value={currentTime}
            onChange={onProgressChange}
            className="w-full h-1.5 rounded-lg appearance-none cursor-pointer music-progress-slider"
            style={{
              background: `linear-gradient(to right, #3b82f6 ${duration ? (currentTime / duration) * 100 : 0}%, rgba(255,255,255,0.3) ${duration ? (currentTime / duration) * 100 : 0}%)`,
            }}
          />
          <div className="flex justify-between text-[10px] text-white/60 mt-1">
            <span>{formatTime(currentTime)}</span>
            <span>{formatTime(duration)}</span>
          </div>
        </div>

        <div className="flex items-center justify-center gap-8 mb-3">
          <button
            onClick={onPrevTrack}
            className="p-2 rounded-full hover:bg-white/10 transition-colors cursor-pointer"
            aria-label="Previous track"
          >
            <svg viewBox="0 0 24 24" className="w-8 h-8 fill-white">
              <path d="M6 6h2v12H6zm3.5 6l8.5 6V6z" />
            </svg>
          </button>

          <button
            onClick={onTogglePlay}
            className="p-3 rounded-full bg-white/10 hover:bg-white/20 transition-colors cursor-pointer"
            aria-label={isPlaying ? 'Pause' : 'Play'}
          >
            {isPlaying ? (
              <svg viewBox="0 0 24 24" className="w-10 h-10 fill-white">
                <rect x="6" y="4" width="4" height="16" rx="1" />
                <rect x="14" y="4" width="4" height="16" rx="1" />
              </svg>
            ) : (
              <svg viewBox="0 0 24 24" className="w-10 h-10 fill-white">
                <polygon points="5 3 19 12 5 21 5 3" />
              </svg>
            )}
          </button>

          <button
            onClick={onNextTrack}
            className="p-2 rounded-full hover:bg-white/10 transition-colors cursor-pointer"
            aria-label="Next track"
          >
            <svg viewBox="0 0 24 24" className="w-8 h-8 fill-white">
              <path d="M16 6h2v12h-2zm-2.5 6l-8.5 6V6z" />
            </svg>
          </button>
        </div>

        <div className="flex items-center gap-3 px-2">
          <svg viewBox="0 0 24 24" className="w-5 h-5 fill-white shrink-0">
            <path d="M3 9v6h4l5 5V4L7 9H3z" />
            <path d="M16.5 12c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02z" />
            <path d="M14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z" />
          </svg>
          <input
            type="range"
            min="0"
            max="1"
            step="0.01"
            value={volume}
            onChange={onVolumeChange}
            className="flex-1 h-2 rounded-lg appearance-none cursor-pointer music-volume-slider"
            style={{
              background: `linear-gradient(to right, #3b82f6 ${volume * 100}%, rgba(255,255,255,0.3) ${volume * 100}%)`,
            }}
          />
          <span className="text-white text-xs font-semibold w-10 text-right">
            {Math.round(volume * 100)}%
          </span>
        </div>
      </div>
    </div>
  );
}
