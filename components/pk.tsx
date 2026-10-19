'use client';

import React, { useState, useEffect } from 'react';
import { createPortal } from 'react-dom';

interface PkScreenProps {
  onClose?: () => void;
}

export default function PkScreen({ onClose }: PkScreenProps) {
  const [selectedMode, setSelectedMode] = useState('');
  const [selectedTime, setSelectedTime] = useState('');
  const [showRules, setShowRules] = useState(false);
  const [showDetails, setShowDetails] = useState(false);
  const [showSelectPeople, setShowSelectPeople] = useState(false);
  const [peopleSheetColor, setPeopleSheetColor] = useState<'blue' | 'pink'>('blue');
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  const modes = ['Votes', 'Coins', 'Earning'];
  const times = ['15s', '1min', '3mins', '5mins', '10mins', '15mins'];

  const historyData = [
    { date: '2024-06-16 07:15:21', desc: 'Battle Bonus', amount: '+2' },
    { date: '2024-06-15 01:51:34', desc: 'Battle Bonus', amount: '+2239' },
    { date: '2024-05-11 01:27:59', desc: 'Battle Bonus', amount: '+27' },
    { date: '2022-11-20 15:04:30', desc: 'Battle Bonus', amount: '+130' },
    { date: '2022-09-07 05:18:39', desc: 'Battle Bonus', amount: '+12' },
    { date: '2022-06-10 12:03:59', desc: 'Battle Bonus', amount: '+6' },
    { date: '2022-05-11 12:03:59', desc: 'Battle Bonus', amount: '+16' },
    { date: '2022-04-10 10:01:59', desc: 'Battle Bonus', amount: '+26' },
  ];

  return (
    <div className="relative flex flex-col h-full bg-gradient-to-br from-[#6e004b] via-[#1c003f] to-[#000c4d] text-white px-3 py-2 overflow-hidden">
      
      {/* Header */}
      <div className="flex justify-between items-center mb-2">
        <div className="flex items-center gap-1.5">
          <div className="w-6 h-6 bg-yellow-400 rounded-full flex justify-center items-center">
            <span className="text-[10px]">🥊</span>
          </div>
          <h2 className="text-sm font-bold">Battle</h2>
        </div>
        <button
          type="button"
          onClick={() => setShowRules(true)}
          className="w-5 h-5 rounded-full border border-yellow-400 bg-yellow-400/20 flex justify-center items-center text-[10px] font-bold text-yellow-400 hover:bg-yellow-400/30 transition-colors"
        >
          ?
        </button>
      </div>

      {/* Profile Card */}
      <div className="bg-white/10 backdrop-blur-sm rounded-lg px-2 py-1.5 flex justify-between items-center mb-2 border border-white/10">
        <div className="flex items-center gap-2">
          <img
            src="https://via.placeholder.com/50"
            alt="Avatar"
            className="w-7 h-7 rounded-full object-cover"
          />
          <div>
            <h3 className="font-semibold text-[11px] leading-tight">A a r a v 💙</h3>
            <p className="text-white/60 text-[9px] leading-tight">
              Today's earning <span className="text-[9px]">🪙</span> 0
            </p>
          </div>
        </div>
        <button 
          type="button"
          onClick={() => setShowDetails(true)}
          className="bg-white/10 py-0.5 px-2 rounded-md text-[9px] hover:bg-white/20 transition-colors cursor-pointer z-10"
        >
          Details &gt;
        </button>
      </div>

      {/* Select People */}
      <h3 className="text-white/70 font-semibold text-[11px] mb-1.5">Select People</h3>
      <div className="flex justify-between gap-2 mb-3">
        <button 
          type="button"
          onClick={() => {
            setPeopleSheetColor('blue');
            setShowSelectPeople(true);
          }}
          className="flex-1 h-20 bg-[#1a3b9e]/80 backdrop-blur-sm rounded-xl flex justify-center items-center transition-transform hover:scale-[1.02] active:scale-95 border border-white/10"
        >
          <div className="w-8 h-8 rounded-full bg-white/20 flex justify-center items-center">
            <span className="text-xl font-light leading-none text-white pb-1">+</span>
          </div>
        </button>
        
        <button 
          type="button"
          onClick={() => {
            setPeopleSheetColor('pink');
            setShowSelectPeople(true);
          }}
          className="flex-1 h-20 bg-[#a81666]/80 backdrop-blur-sm rounded-xl flex justify-center items-center transition-transform hover:scale-[1.02] active:scale-95 border border-white/10"
        >
          <div className="w-8 h-8 rounded-full bg-white/20 flex justify-center items-center">
            <span className="text-xl font-light leading-none text-white pb-1">+</span>
          </div>
        </button>
      </div>

      {/* Mode */}
      <h3 className="text-white/70 font-semibold text-[11px] mb-1.5">Mode</h3>
      <div className="flex gap-2 mb-3">
        {modes.map((mode) => (
          <button
            type="button"
            key={mode}
            onClick={() => setSelectedMode(mode)}
            className={`relative flex-1 py-2 rounded-lg text-xs font-semibold transition-all border-2 ${
              selectedMode === mode
                ? 'bg-gradient-to-b from-[#0d4d5e] to-[#0a859b] border-[#00e5ff] text-white shadow-[0_0_10px_rgba(0,229,255,0.6)]'
                : 'bg-[#3b2b50]/80 border-transparent text-white/70 hover:bg-[#4a3763]/90'
            }`}
          >
            {mode}
            {mode === 'Earning' && (
              <div className="absolute -top-1.5 -right-1.5 bg-orange-500 w-4 h-4 rounded-full flex justify-center items-center text-[8px] border border-white/20">
                🔥
              </div>
            )}
          </button>
        ))}
      </div>

      {/* Time */}
      <h3 className="text-white/70 font-semibold text-[11px] mb-1.5">Time</h3>
      <div className="grid grid-cols-3 gap-2 mb-3">
        {times.map((time) => (
          <button
            type="button"
            key={time}
            onClick={() => setSelectedTime(time)}
            className={`py-2 rounded-lg text-xs font-semibold transition-all border-2 ${
              selectedTime === time
                ? 'bg-gradient-to-b from-[#0d4d5e] to-[#0a859b] border-[#00e5ff] text-white shadow-[0_0_10px_rgba(0,229,255,0.6)]'
                : 'bg-[#3b2b50]/80 border-transparent text-white/70 hover:bg-[#4a3763]/90'
            }`}
          >
            {time}
          </button>
        ))}
      </div>

      {/* Confirm Button */}
      <button
        type="button"
        onClick={onClose}
        className="w-full bg-[#1b5a7a]/90 backdrop-blur-sm py-3 rounded-full text-white font-bold text-sm opacity-90 hover:opacity-100 transition-opacity border border-white/10"
      >
        Confirm
      </button>

      {/* ================= SABHI SHEETS PORTAL KE THROUGH ================= */}
      {mounted && createPortal(
        <>
          {/* Rules Sheet */}
          {showRules && (
            <div
              className="fixed inset-0 z-[999] flex items-end justify-center bg-black/60"
              onClick={() => setShowRules(false)}
            >
              <div
                onClick={(e) => e.stopPropagation()}
                className="w-full max-w-md bg-gradient-to-r from-[#4B003D] via-[#1E0038] to-[#001C5E] border-t border-[#2a1e5c] rounded-t-2xl px-4 pt-4 pb-6 max-h-[45%] overflow-y-auto"
              >
                <div className="mb-4">
                  <h2 className="text-base font-bold text-white text-center">Rules</h2>
                </div>
                <div className="space-y-4 text-left text-[#E0D8FF]">
                  <div>
                    <h3 className="text-sm font-bold text-white mb-1">Vote Mode</h3>
                    <p className="text-xs leading-relaxed text-[#A098C0]">
                      The person with the most votes wins; 1 person, 1 vote.
                    </p>
                  </div>
                  <div>
                    <h3 className="text-sm font-bold text-white mb-1">Coin Mode</h3>
                    <ol className="text-xs leading-relaxed text-[#A098C0] space-y-1.5 list-decimal list-inside">
                      <li>The person who receives the most coins (as gifts) wins. 1 coin equals 1 point.</li>
                      <li>If a participant leaves the PK (battle) before the time runs out, they will be disqualified.</li>
                    </ol>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* Select People Sheet */}
          {showSelectPeople && (
            <div
              className="fixed inset-0 z-[999] flex items-end justify-center bg-black/60"
              onClick={() => setShowSelectPeople(false)}
            >
              <div
                onClick={(e) => e.stopPropagation()}
                className={`w-full max-w-md rounded-t-2xl px-4 pt-3 pb-6 max-h-[55%] overflow-y-auto transition-colors duration-300 ${
                  peopleSheetColor === 'blue' ? 'bg-[#1a3b9e]' : 'bg-[#a81666]'
                }`}
              >
                <div className="w-10 h-1 bg-white/30 rounded-full mx-auto mb-3" />
                <h2 className="text-base font-bold text-white text-center mb-4">Select people</h2>
                <div className="grid grid-cols-5 gap-y-2 gap-x-1 justify-items-center">
                  <div className="w-9 h-9 rounded-full overflow-hidden">
                    <img src="https://via.placeholder.com/50" alt="Avatar" className="w-full h-full object-cover" />
                  </div>
                  {Array.from({ length: 9 }).map((_, i) => (
                    <button type="button" key={i} className="w-9 h-9 rounded-full bg-white/20 flex justify-center items-center hover:bg-white/30 transition-colors">
                      <svg className="w-4 h-4 text-white/70" fill="currentColor" viewBox="0 0 24 24">
                        <path d="M4 10v7h3v-7H4zm13 0v7h3v-7h-3zm-9-4v11h8V6H8zm-3 6h2V8c0-1.1.9-2 2-2h6c1.1 0 2 .9 2 2v4h2c1.1 0 2 .9 2 2v7c0 .6-.4 1-1 1h-1c-.6 0-1-.4-1-1v-3H7v3c0 .6-.4 1-1 1H5c-.6 0-1-.4-1-1v-7c0-1.1.9-2 2-2z" />
                      </svg>
                    </button>
                  ))}
                </div>
              </div>
            </div>
          )}

          {/* Details Sheet — full width left-right */}
          {showDetails && (
            <div
              className="fixed inset-0 z-[999] bg-black/60"
              onClick={() => setShowDetails(false)}
            >
              <div
                onClick={(e) => e.stopPropagation()}
                style={{
                  position: 'fixed',
                  bottom: 0,
                  left: 0,
                  right: 0,
                  width: '100vw',
                  maxWidth: '100vw',
                  margin: 0,
                  padding: 0,
                }}
                className="bg-gradient-to-r from-[#4B003D] via-[#1E0038] to-[#001C5E] border-t border-[#2a1e5c] rounded-t-2xl max-h-[60%] overflow-y-auto"
              >
                <div className="px-2.5 pt-2 pb-3">
                  {/* Header with Back Button */}
                  <div className="relative flex items-center justify-center mb-1.5">
                    <button 
                      type="button"
                      onClick={() => setShowDetails(false)}
                      className="absolute left-0 text-white/80 hover:text-white transition-colors"
                    >
                      <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
                      </svg>
                    </button>
                    <h2 className="text-xs font-bold text-white text-center">Details</h2>
                  </div>

                  {/* Today's Earning Section */}
                  <div className="bg-white/5 backdrop-blur-sm border border-white/10 rounded-lg p-1.5 mb-1.5">
                    <h3 className="text-center text-white/70 text-[9px] mb-1">Today's earning</h3>
                    <div className="flex justify-between items-center text-center">
                      <div className="flex-1 flex flex-col items-center border-r border-white/10">
                        <div className="text-[8px] font-semibold mb-0.5">
                          <span className="text-pink-500">PK</span>
                        </div>
                        <div className="flex items-center gap-0.5 mb-0.5">
                          <span className="text-yellow-400 text-[10px]">$</span>
                          <span className="font-bold text-[10px]">0</span>
                        </div>
                        <span className="text-[7px] text-white/50">This month: 0</span>
                      </div>

                      <div className="flex-1 flex flex-col items-center border-r border-white/10">
                        <div className="text-[8px] font-semibold mb-0.5 flex items-center gap-1">
                          <span className="text-yellow-500">⚔️</span>
                          <span>Battle</span>
                        </div>
                        <div className="flex items-center gap-0.5 mb-0.5">
                          <span className="text-yellow-400 text-[10px]">🪙</span>
                          <span className="font-bold text-[10px]">0</span>
                        </div>
                        <span className="text-[7px] text-white/50">This month: 0</span>
                      </div>

                      <div className="flex-1 flex flex-col items-center">
                        <div className="text-[8px] font-semibold mb-0.5 flex items-center gap-1">
                          <span className="text-orange-400">🧮</span>
                          <span>Calculator</span>
                        </div>
                        <div className="flex items-center gap-0.5 mb-0.5">
                          <span className="text-yellow-400 text-[10px]">🪙</span>
                          <span className="font-bold text-[10px]">0</span>
                        </div>
                        <span className="text-[7px] text-white/50">This month: 0</span>
                      </div>
                    </div>
                  </div>

                  {/* History Section */}
                  <div>
                    <h3 className="text-white/80 font-semibold text-[10px] mb-1">History</h3>
                    <div className="flex flex-col">
                      {historyData.map((item, index) => (
                        <div
                          key={index}
                          className="flex justify-between items-center py-1 border-b border-white/5 last:border-0"
                        >
                          <span className="text-[8px] text-white/50 w-[42%]">{item.date}</span>
                          <span className="text-[9px] flex-1 text-white/90">{item.desc}</span>
                          <span className="text-[9px] font-medium text-yellow-400 text-right">
                            🪙 {item.amount}
                          </span>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          )}
        </>,
        document.body
      )}
    </div>
  );
}
