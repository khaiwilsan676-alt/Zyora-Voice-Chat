'use client';

import React, { useState, useEffect, useRef } from 'react';
import socket from '../src/lib/socket';
import { recordTransaction } from './Wallet';

interface FruitpartyProps {
  onClose: () => void;
  onMinimize?: () => void;
  isMinimized?: boolean;
}

const GRID_ITEMS = [
  { id: 1, type: 'fruit', img: '/IMG_20260908_192143.png', multi: '×5',  move: 'translate-x-[4px] translate-y-[9px]', imgW: 50, imgH: 50 },  // Lemon (0)
  { id: 5, type: 'fruit', img: '/IMG_20260908_192120.png', multi: '×10', move: 'translate-y-[9px]',                   imgW: 55, imgH: 55 },  // Grapes (1)
  { id: 3, type: 'fruit', img: '/IMG_20260908_192203.png', multi: '×5',  move: '-translate-x-[4px] translate-y-[9px]', imgW: 50, imgH: 50 },   // Santra (2)
  { id: 8, type: 'fruit', img: '/IMG_20260908_192013.png', multi: '×45', move: 'translate-x-[4px]',                   imgW: 50, imgH: 50 },  // Cherry (3)
  { id: 9, type: 'timer', move: 'z-20' },                                                                                                    // CENTER (4) 
  { id: 2, type: 'fruit', img: '/IMG_20260908_192050.png', multi: '×25', move: '-translate-x-[4px]',                   imgW: 50, imgH: 50 },  // Apple (5)
  { id: 7, type: 'fruit', img: '/IMG_20260908_191941.png', multi: '×5',  move: 'translate-x-[4px] -translate-y-[9px]', imgW: 140, imgH: 140 }, // Aam (6)
  { id: 4, type: 'fruit', img: '/IMG_20260908_191906.png', multi: '×15', move: '-translate-y-[9px]',                   imgW: 50, imgH: 50 },  // Strawberry (7)
  { id: 6, type: 'fruit', img: '/IMG_20260908_191930.png', multi: '×5',  move: '-translate-x-[4px] -translate-y-[9px]', imgW: 50, imgH: 50 },  // Naspati (8)
];

const SPIN_PATH = [0, 1, 2, 5, 8, 7, 6, 3]; 

const DB_NAME = 'FruitPartyDB';
const STORE_NAME = 'GameState';

let isAppMinimizedSession = false;
let sessionBetsGlobal: Record<number, number> = {};

async function initDB(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    if (typeof window === 'undefined') return reject("No window");
    const request = indexedDB.open(DB_NAME, 3);
    request.onupgradeneeded = (e: any) => {
      const db = e.target.result;
      if (!db.objectStoreNames.contains(STORE_NAME)) {
        db.createObjectStore(STORE_NAME);
      }
    };
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

async function loadStateFromDB() {
  try {
    const db = await initDB();
    return new Promise((resolve) => {
      const tx = db.transaction(STORE_NAME, 'readonly');
      const req = tx.objectStore(STORE_NAME).get('user_data');
      req.onsuccess = () => resolve(req.result);
      req.onerror = () => resolve(null);
    });
  } catch (e) { return null; }
}

async function saveStateToDB(state: any) {
  try {
    const db = await initDB();
    const tx = db.transaction(STORE_NAME, 'readwrite');
    tx.objectStore(STORE_NAME).put(state, 'user_data');
  } catch (e) {}
}

function WebGLShaderImage({ src }: { src: string }) {
  return <img src={src} className="w-full h-full object-contain" alt="" />;
}

const getDynamicTextSize = (val: number) => {
  const len = val.toString().length;
  if (len > 11) return 'text-[7px]';
  if (len > 9) return 'text-[8px]';
  if (len > 7) return 'text-[10px]';
  if (len > 5) return 'text-[12px]';
  return 'text-sm'; 
};

type HistoryItem = {
  round: number;
  winnerImg: string;
  winnerId: number;
  won: boolean;
  bets: Record<number, number>;
  totalWonAmount: number;
};

export default function Fruitparty({ onClose, onMinimize, isMinimized = false }: FruitpartyProps) {
  const [loading, setLoading] = useState(!isAppMinimizedSession);
  const [progress, setProgress] = useState(isAppMinimizedSession ? 100 : 0);
  const [isLoadedFromDB, setIsLoadedFromDB] = useState(false);
  
  const [gameState, setGameState] = useState({
    phase: 'betting' as 'betting' | 'spinning' | 'result',
    countdown: 30,
    round: 0,
    winnerIndex: 0,
    highlight: null as number | null,
    handPointer: SPIN_PATH[0],
    showResultPopup: false 
  });

  const [winners, setWinners] = useState<string[]>([]);
  const [roundHistory, setRoundHistory] = useState<HistoryItem[]>([]);
  
  const [balance, setBalance] = useState(82927);
  const [totalWon, setTotalWon] = useState(0);
  const [bets, setBets] = useState<Record<number, number>>(isAppMinimizedSession ? sessionBetsGlobal : {});
  
  const [lastRoundStats, setLastRoundStats] = useState({ bet: 0, won: 0 });
  const [processedRound, setProcessedRound] = useState(-1);
  const [realWinners, setRealWinners] = useState<{name: string, win: number, avatar: string}[]>([]);
  const stateRefs = useRef({ balance, totalWon, bets });

  const [showHistory, setShowHistory] = useState(false);
  const [showRules, setShowRules] = useState(false);
  const [activeBtn, setActiveBtn] = useState<number | null>(null);
  
  const [isMuted, setIsMuted] = useState(false);
  const [isVibrationOn, setIsVibrationOn] = useState(true);
  
  const bgAudioRef = useRef<HTMLAudioElement | null>(null);
  const tickAudioRef = useRef<HTMLAudioElement | null>(null);

  const [scale, setScale] = useState(1);

  const handleFullClose = () => {
    isAppMinimizedSession = false;
    sessionBetsGlobal = {};
    onClose();
  };

  const handleMinimizeClick = () => {
    isAppMinimizedSession = true;
    sessionBetsGlobal = bets;
    if (onMinimize) {
      onMinimize();
    }
  };
  
  useEffect(() => {
    const checkAndClearRecords = () => {
      const now = new Date();
      const resetTime = new Date();
      resetTime.setHours(5, 30, 0, 0);

      if (now < resetTime) {
        resetTime.setDate(resetTime.getDate() - 1);
      }

      const resetKey = resetTime.getTime().toString();
      const lastCleared = localStorage.getItem('fruitparty_last_clear');

      if (lastCleared !== resetKey) {
        setRoundHistory([]);
        setWinners([]);
        localStorage.setItem('fruitparty_last_clear', resetKey);
      }
    };

    checkAndClearRecords();
    const clearTimer = setInterval(checkAndClearRecords, 60000); 
    return () => clearInterval(clearTimer);
  }, []);
  
  useEffect(() => {
    const handleResize = () => {
      const width = window.innerWidth;
      if (width < 390) { setScale(width / 390); } else { setScale(1); }
    };
    handleResize();
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  useEffect(() => {
    loadStateFromDB().then((data: any) => {
      if (data) {
        if (data.balance !== undefined) setBalance(data.balance);
        if (data.totalWon !== undefined) setTotalWon(data.totalWon);
        
        const resetTime = new Date();
        resetTime.setHours(5, 30, 0, 0);
        if (new Date() < resetTime) resetTime.setDate(resetTime.getDate() - 1);
        const resetKey = resetTime.getTime().toString();
        const lastCleared = localStorage.getItem('fruitparty_last_clear');
        
        if (lastCleared === resetKey) {
          if (data.roundHistory !== undefined) setRoundHistory(data.roundHistory);
          if (data.winners !== undefined) setWinners(data.winners);
        }
      }
      setIsLoadedFromDB(true);
    });
  }, []);

  useEffect(() => {
    if (isLoadedFromDB) {
      saveStateToDB({ balance, totalWon, roundHistory, winners });
    }
  }, [balance, totalWon, roundHistory, winners, isLoadedFromDB]);

  useEffect(() => {
    stateRefs.current = { balance, totalWon, bets };
    if (isAppMinimizedSession) {
      sessionBetsGlobal = bets;
    }
  }, [balance, totalWon, bets]);

  useEffect(() => {
    const handleFruitWinner = (data: {name: string, win: number, avatar: string}[]) => {
      setRealWinners(data);
    };

    socket.on('fruitparty_top_winners', handleFruitWinner);

    return () => {
      socket.off('fruitparty_top_winners', handleFruitWinner);
    };
  }, []);

  useEffect(() => {
    if (gameState.phase === 'betting') {
      setRealWinners([]);
    }
  }, [gameState.phase]);

  useEffect(() => {
    if (isAppMinimizedSession) {
      setLoading(false);
      return;
    }
    const interval = setInterval(() => {
      setProgress((prev) => {
        if (prev >= 100) {
          clearInterval(interval);
          setTimeout(() => {
            setLoading(false);
          }, 200);
          return 100;
        }
        return prev + 10;
      });
    }, 150);
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    bgAudioRef.current = new Audio('/VID_20260909_230819_175_bsl.mp4'); 
    bgAudioRef.current.loop = true;
    tickAudioRef.current = new Audio('https://assets.mixkit.co/active_storage/sfx/2571/2571-preview.mp3');
    tickAudioRef.current.volume = 1.0; 
    
    return () => {
      if (bgAudioRef.current) {
        bgAudioRef.current.pause();
        bgAudioRef.current = null;
      }
      if (tickAudioRef.current) {
        tickAudioRef.current.pause();
        tickAudioRef.current = null;
      }
    };
  }, []);

  useEffect(() => {
    if (isMinimized) {
      bgAudioRef.current?.pause();
      return;
    }
    if (bgAudioRef.current) bgAudioRef.current.muted = isMuted;
    if (tickAudioRef.current) tickAudioRef.current.muted = isMuted;
    if (!isMuted) bgAudioRef.current?.play().catch(() => {});
    else bgAudioRef.current?.pause();
  }, [isMuted, isMinimized]);

  useEffect(() => {
    if (isMinimized) return;
    if (gameState.phase === 'spinning' && !isMuted && tickAudioRef.current) {
      const tickClone = tickAudioRef.current.cloneNode() as HTMLAudioElement;
      tickClone.volume = 1.0;
      tickClone.play().catch(() => {});
    }
  }, [gameState.highlight, gameState.phase, isMuted, isMinimized]);

  useEffect(() => {
    if (isMinimized || !isVibrationOn) return;
    if (gameState.phase === 'spinning' && navigator.vibrate) {
      navigator.vibrate(30);
    }
  }, [gameState.highlight, gameState.phase, isVibrationOn, isMinimized]);

  useEffect(() => {
    const clock = setInterval(() => {
      const CYCLE_MS = 41000; 
      const now = Date.now();
      
      const roundNumber = (Math.floor(now / CYCLE_MS) % 10000) + 1000;
      const elapsed = now % CYCLE_MS;

      const seed = Math.sin(roundNumber) * 10000;
      const randomVal = seed - Math.floor(seed);
      
      let winnerIdx = 0;
      if (randomVal < 0.025) {
        winnerIdx = 10; 
      } else if (randomVal < 0.05) {
        winnerIdx = 11; 
      } else if (randomVal < 0.85) {
        const lowPayouts = [0, 2, 8, 6]; 
        winnerIdx = lowPayouts[Math.floor(randomVal * 100) % lowPayouts.length];
      } else {
        const highPayouts = [1, 5, 7, 3];
        winnerIdx = highPayouts[Math.floor(randomVal * 100) % highPayouts.length];
      }

      let currentPhase: 'betting' | 'spinning' | 'result' = 'betting';
      let currentCountdown = 0;
      let currentHighlight = null;
      let currentHandPointer = SPIN_PATH[0];
      let currentShowResultPopup = false;

      if (elapsed < 30000) {
        currentPhase = 'betting';
        currentCountdown = 30 - Math.floor(elapsed / 1000);
        currentHandPointer = SPIN_PATH[Math.floor(elapsed / 1000) % SPIN_PATH.length];
      } else if (elapsed < 36000) {
        currentPhase = 'spinning';
        currentCountdown = 6 - Math.floor((elapsed - 30000) / 1000);
        
        const spinElapsed = elapsed - 31000;
        
        if (spinElapsed < 0) {
          currentHighlight = SPIN_PATH[0];
        } else {
          let targetPathIndex = SPIN_PATH.indexOf(winnerIdx);
          if (targetPathIndex === -1) targetPathIndex = 0; 
          
          const spinDuration = 5000; 
          let t = spinElapsed / spinDuration; 
          
          let easeOut = 1 - Math.pow(1 - t, 3);
          
          const maxSteps = 55; 
          const step = Math.round(maxSteps * easeOut);
          
          const startOffset = (targetPathIndex - (maxSteps % SPIN_PATH.length) + SPIN_PATH.length * 10) % SPIN_PATH.length;
          currentHighlight = SPIN_PATH[(startOffset + step) % SPIN_PATH.length];
        }

      } else {
        currentPhase = 'result';
        currentCountdown = 5 - Math.floor((elapsed - 36000) / 1000);
        currentHighlight = winnerIdx;
        
        if (elapsed >= 36500) {
          currentShowResultPopup = true;
        }
      }

      setGameState(prev => {
        if (
          prev.phase === currentPhase &&
          prev.countdown === currentCountdown &&
          prev.round === roundNumber &&
          prev.highlight === currentHighlight &&
          prev.handPointer === currentHandPointer &&
          prev.showResultPopup === currentShowResultPopup
        ) {
          return prev; 
        }
        return {
          phase: currentPhase,
          countdown: currentCountdown,
          round: roundNumber,
          winnerIndex: winnerIdx,
          highlight: currentHighlight,
          handPointer: currentHandPointer,
          showResultPopup: currentShowResultPopup
        };
      });

    }, 50); 

    return () => clearInterval(clock);
  }, []);

  useEffect(() => {
    if (gameState.phase === 'result' && gameState.round !== processedRound && isLoadedFromDB) {
      
      const currentBets = { ...stateRefs.current.bets };
      let earned = 0;
      let totalBetThisRound = 0;

      Object.values(currentBets).forEach(val => totalBetThisRound += val);

      let winnerImgToSave = '';
      let winnerIdToSave = 0;

      if (gameState.winnerIndex === 10) {
        winnerImgToSave = '/IMG_20260910_114515.png';
        winnerIdToSave = 10;
        
        if (currentBets[10] > 0) earned += currentBets[10] * 20;
        if (currentBets[1] > 0) earned += currentBets[1] * 5;
        if (currentBets[3] > 0) earned += currentBets[3] * 5;
        if (currentBets[7] > 0) earned += currentBets[7] * 5;
        if (currentBets[6] > 0) earned += currentBets[6] * 5;

      } else if (gameState.winnerIndex === 11) {
        winnerImgToSave = '/IMG_20260910_114613.png';
        winnerIdToSave = 11;
        
        if (currentBets[11] > 0) earned += currentBets[11] * 95;
        if (currentBets[5] > 0) earned += currentBets[5] * 10;
        if (currentBets[4] > 0) earned += currentBets[4] * 15;
        if (currentBets[2] > 0) earned += currentBets[2] * 25;
        if (currentBets[8] > 0) earned += currentBets[8] * 45;

      } else {
        const winnerItem = GRID_ITEMS[gameState.winnerIndex];
        if (winnerItem && winnerItem.type === 'fruit') {
          winnerImgToSave = winnerItem.img as string;
          winnerIdToSave = winnerItem.id;

          const betOnWinner = currentBets[winnerItem.id] || 0;
          if (betOnWinner > 0) {
            const mult = parseInt((winnerItem.multi || '×0').replace('×', ''));
            earned = betOnWinner * mult;
          }
        }
      }

      if (winnerImgToSave) {
        const nextBalance = stateRefs.current.balance + earned;
        const nextTotalWon = stateRefs.current.totalWon + earned;

        setBalance(nextBalance);
        setTotalWon(nextTotalWon);
        setLastRoundStats({ bet: totalBetThisRound, won: earned });
        if (earned > 0) {
            recordTransaction('Won in Fruit Party', earned);

            // Use the same profile name + DP shown on Me/Public Profile.
            let currentUserName = localStorage.getItem('userName') || "User";
            let currentUserAvatar = localStorage.getItem('userPhoto') || "/default-avatar.png";
            try {
                const userData = localStorage.getItem('currentUser');
                if (userData) {
                    const parsed = JSON.parse(userData);
                    currentUserName = localStorage.getItem('userName') || parsed.name || "User";
                    currentUserAvatar =
                      localStorage.getItem('userPhoto') ||
                      parsed.photo ||
                      parsed.avatar ||
                      parsed.image ||
                      parsed.photoUrl ||
                      parsed.photoURL ||
                      "/default-avatar.png";
                }
            } catch (e) {}

            socket.emit('fruitparty_winner_update', {
              eventId: `fruit_1790417048350_dc2wz1r9`,
                name: currentUserName,
                win: earned,
                avatar: currentUserAvatar,
                round: gameState.round
            });
        }
        
        setBets({});
        sessionBetsGlobal = {};
        setProcessedRound(gameState.round);
        setWinners(w => [...w, winnerImgToSave].slice(-18));
        
        if (totalBetThisRound > 0) {
          setRoundHistory(prev => [{ 
            round: gameState.round, 
            winnerImg: winnerImgToSave, 
            winnerId: winnerIdToSave,
            won: earned > 0,
            bets: currentBets,
            totalWonAmount: earned
          }, ...prev].slice(0, 50)); 
        }
      }
    }
  }, [gameState.phase, gameState.round, processedRound, isLoadedFromDB]);

  const handleBetClick = (fruitId: number) => {
    if (gameState.phase === 'betting' && activeBtn !== null) {
      const betValues = { 1: 1000, 2: 50000, 3: 1000000, 4: 5000000 };
      const betAmt = betValues[activeBtn as keyof typeof betValues];
      
      if (balance >= betAmt) {
        setBalance(prev => prev - betAmt);
        recordTransaction('Bet placed on Fruit Party', -betAmt);
        setBets(prev => {
          const updated = { ...prev, [fruitId]: (prev[fruitId] || 0) + betAmt };
          sessionBetsGlobal = updated;
          return updated;
        });
      }
    }
  };

  let popupWinnerImg = '';
  if (gameState.winnerIndex === 10) popupWinnerImg = '/IMG_20260910_114515.png';
  else if (gameState.winnerIndex === 11) popupWinnerImg = '/IMG_20260910_114613.png';
  else popupWinnerImg = GRID_ITEMS[gameState.winnerIndex]?.img || '';

  return (
    <div className={`fixed inset-0 z-[70] items-end justify-center ${isMinimized ? 'hidden' : 'flex'}`}>
      <img src="/file_00000000ced481fa9117afc4fa91791e.png" className="hidden" alt="preload1" />
      <img src="/file_00000000eb0081f4885ade7d7db3bef8.png" className="hidden" alt="preload2" />

      <div className="absolute inset-0 bg-black/50" onClick={handleFullClose} />

      {!loading && (
        <div 
          className="fixed z-[75] flex flex-col items-center pointer-events-none" 
          style={{
            top: '36.5vh',
            bottom: '30vh',
            left: '8vh',
            right: '8vh'
          }}
        >
          <div className="grid grid-cols-3 gap-0 w-full h-full mx-auto max-w-md pointer-events-auto">
            {GRID_ITEMS.map((item, index) => {
              const isBettingHighlight = gameState.phase === 'betting' && gameState.handPointer === index;
              
              const isSpinningHighlight = (gameState.phase === 'spinning' || gameState.phase === 'result') && (
                gameState.highlight === index ||
                (gameState.phase === 'result' && gameState.winnerIndex === 10 && [0, 2, 6, 8].includes(index)) ||
                (gameState.phase === 'result' && gameState.winnerIndex === 11 && [1, 3, 5, 7].includes(index))
              );
              
              const isFinalWinner = gameState.phase === 'result' && (
                gameState.winnerIndex === index ||
                (gameState.winnerIndex === 10 && [0, 2, 6, 8].includes(index)) ||
                (gameState.winnerIndex === 11 && [1, 3, 5, 7].includes(index))
              );

              const applyGreen = isBettingHighlight || isSpinningHighlight;
              // Ekdum subtle/halki vibration apply hogi jab box ghum raha hoga
              const applySubtleShake = gameState.phase === 'spinning' && isSpinningHighlight && isVibrationOn;

              return (
                <div 
                  key={item.id || index} 
                  onClick={() => { if (item.type === 'fruit') handleBetClick(item.id); }}
                  className={`relative w-full h-full flex items-center justify-center transition-transform ${item.move || ''} ${item.type === 'fruit' ? 'cursor-pointer' : ''} ${applyGreen ? '!z-[999]' : ''} ${isFinalWinner ? 'animate-single-blink' : ''} ${applySubtleShake ? 'animate-subtle-shake' : ''}`}
                >
                  {item.type === 'fruit' ? (
                    <>
                      <img src="/file_00000000d0ec820ba666eab8bea30204.png" className="absolute inset-0 w-full h-full object-fill pointer-events-none z-0" style={applyGreen ? { filter: 'hue-rotate(-150deg) saturate(200%) drop-shadow(0px 0px 8px lime)' } : {}} />
                      {isBettingHighlight && (
                        <img src="/file_000000000f0c820b95490c9d927692d9.png" className="absolute -bottom-[15%] -right-[15%] w-[65%] h-[65%] max-w-[55px] max-h-[55px] z-[999] object-contain pointer-events-none -rotate-[45deg] drop-shadow-xl" />
                      )}
                      <div className="absolute inset-0 z-10 flex flex-col items-center justify-center gap-0.5">
                        <img src={item.img} className={`object-contain pointer-events-none drop-shadow-md mb-2 ${item.id === 7 ? 'w-[65px] h-[65px]' : 'w-[55%] h-[55%]'}`} />
                      </div>
                      {(bets[item.id] || 0) > 0 && (
                        <div className="absolute bottom-[30%] left-1/2 -translate-x-1/2 w-[85%] h-[16px] max-h-[25%] bg-gradient-to-r from-blue-500/80 to-pink-500/80 flex items-center justify-center gap-[2px] rounded z-20 pointer-events-none shadow-md border border-white/20 overflow-hidden">
                          <div className="w-[10px] h-[10px] flex-shrink-0"><WebGLShaderImage src="/file_00000000e56882119c217d508b6733dc.png" /></div>
                          <span className="text-white text-[clamp(6px,2vw,9px)] font-bold leading-none mt-[1px]">{bets[item.id]}</span>
                        </div>
                      )}
                      <span className="absolute bottom-[18%] left-1/2 -translate-x-1/2 text-white text-[clamp(8px,2.5vw,13px)] font-black drop-shadow-[0_2px_2px_rgba(0,0,0,1)] leading-none z-20 pointer-events-none">{item.multi}</span>
                    </>
                  ) : (
                    <>
                      <img src="/file_0000000023cc8230baec62632d74e698.png" className="absolute inset-0 w-full h-full object-fill z-0" />
                      <div className="absolute inset-0 z-10 flex flex-col items-center justify-center">
                        <span className="text-white text-[clamp(6px,2vw,9px)] font-bold tracking-wider drop-shadow-md mb-0.5">
                          {gameState.phase === 'betting' ? 'BETTING' : (gameState.phase === 'spinning' ? 'SPINNING' : 'RESULT')}
                        </span>
                        <span className="text-amber-400 font-extrabold text-[clamp(14px,4vw,20px)] tracking-wide drop-shadow-[0_4px_12px_rgba(0,0,0,0.9)] leading-none">{gameState.countdown}s</span>
                      </div>
                    </>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      )}

      {!loading && (
        <div
          className="fixed z-[75] flex flex-row items-center justify-center gap-1 pointer-events-none"
          style={{
            top: '69vh',
            bottom: '21vh',
            left: '11vh',
            right: '11vh'
          }}
        >
          {/* Left Mix (ID 10) - VIBRATION REMOVED */}
          <div className={`relative w-full h-full flex items-center justify-center pointer-events-auto transition-transform ${gameState.phase === 'result' && gameState.winnerIndex === 10 ? 'animate-single-blink' : ''}`}>
            <img 
              src="/IMG_20260910_114625.png" 
              className="absolute inset-0 w-full h-full object-fill transition-all duration-300"
              style={(gameState.phase === 'result' && gameState.winnerIndex === 10) ? { filter: 'hue-rotate(-150deg) saturate(200%) drop-shadow(0px 0px 8px lime)' } : {}} 
            />
            <img src="/IMG_20260910_114515.png" className="relative z-10 w-[60%] h-[60%] object-contain pointer-events-none drop-shadow-md" alt="Small Fruit" />
            
            {(bets[10] || 0) > 0 && (
              <div className="absolute bottom-[10%] left-1/2 -translate-x-1/2 w-[80%] h-[16px] max-h-[25%] bg-gradient-to-r from-blue-500/80 to-pink-500/80 flex items-center justify-center gap-[2px] rounded z-20 pointer-events-none shadow-md border border-white/20 overflow-hidden">
                <div className="w-[10px] h-[10px] flex-shrink-0"><WebGLShaderImage src="/file_00000000e56882119c217d508b6733dc.png" /></div>
                <span className="text-white text-[clamp(6px,2vw,9px)] font-bold leading-none mt-[1px]">{bets[10]}</span>
              </div>
            )}
          </div>

          {/* Right Mix (ID 11) - VIBRATION REMOVED */}
          <div className={`relative w-full h-full flex items-center justify-center pointer-events-auto transition-transform ${gameState.phase === 'result' && gameState.winnerIndex === 11 ? 'animate-single-blink' : ''}`}>
            <img 
              src="/IMG_20260910_114625.png" 
              className="absolute inset-0 w-full h-full object-fill transition-all duration-300"
              style={(gameState.phase === 'result' && gameState.winnerIndex === 11) ? { filter: 'hue-rotate(-150deg) saturate(200%) drop-shadow(0px 0px 8px lime)' } : {}} 
            />
            <img src="/IMG_20260910_114613.png" className="relative z-10 w-[60%] h-[60%] object-contain pointer-events-none drop-shadow-md" alt="Big Fruit" />
            
            {(bets[11] || 0) > 0 && (
              <div className="absolute bottom-[10%] left-1/2 -translate-x-1/2 w-[80%] h-[16px] max-h-[25%] bg-gradient-to-r from-blue-500/80 to-pink-500/80 flex items-center justify-center gap-[2px] rounded z-20 pointer-events-none shadow-md border border-white/20 overflow-hidden">
                <div className="w-[10px] h-[10px] flex-shrink-0"><WebGLShaderImage src="/file_00000000e56882119c217d508b6733dc.png" /></div>
                <span className="text-white text-[clamp(6px,2vw,9px)] font-bold leading-none mt-[1px]">{bets[11]}</span>
              </div>
            )}
          </div>
        </div>
      )}

      <div
        className="relative bg-[#330c36] w-full max-w-md shadow-2xl overflow-hidden animate-slide-up flex flex-col rounded-none"
        style={{ height: '68vh' }}
        onClick={(e) => e.stopPropagation()}
      >
        {!loading && (
          <>
            <div className="absolute bottom-[64vh] left-7 z-30 flex items-center gap-0.5">
              <button onClick={() => setIsVibrationOn(!isVibrationOn)} className="w-6 h-6 rounded-full border-[2px] border-[#4a2810] bg-transparent flex items-center justify-center hover:bg-black/10 active:scale-95 transition-all p-0.5">
                {isVibrationOn ? (
                  <svg viewBox="0 0 24 24" className="w-full h-full fill-[#4a2810]">
                    <path d="M16 1H8C6.9 1 6 1.9 6 3v18c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V3c0-1.1-.9-2-2-2zm0 18H8V5h8v14zM22 7v10h-2V7h2zM4 7v10H2V7h2z"/>
                  </svg>
                ) : (
                  <svg viewBox="0 0 24 24" className="w-full h-full fill-[#4a2810]">
                    <path d="M16 1H8C6.9 1 6 1.9 6 3v18c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V3c0-1.1-.9-2-2-2zm0 18H8V5h8v14z"/>
                    <line x1="2" y1="2" x2="22" y2="22" stroke="#4a2810" strokeWidth="2" strokeLinecap="round"/>
                  </svg>
                )}
              </button>
              
              <button onClick={() => setIsMuted(!isMuted)} className="w-6 h-6 rounded-full border-[2px] border-[#4a2810] bg-transparent flex items-center justify-center hover:bg-black/10 active:scale-95 transition-all p-0.5">
                <svg viewBox="0 0 24 24" className="w-full h-full fill-[#4a2810] stroke-[#4a2810] stroke-[1.5]">
                  {!isMuted ? (
                    <path d="M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z"/>
                  ) : (
                    <path d="M16.5 12c0-1.77-1.02-3.29-2.5-4.03v2.21l2.45 2.45c.03-.2.05-.41.05-.63zm2.5 0c0 .94-.2 1.82-.54 2.64l1.51 1.51C20.63 14.91 21 13.5 21 12c0-4.28-2.99-7.86-7-8.77v2.06c2.89.86 5 3.54 5 6.71zM4.27 3L3 4.27 7.73 9H3v6h4l5 5v-6.73l4.25 4.25c-.67.52-1.42.93-2.25 1.18v2.06c1.38-.31 2.63-.95 3.69-1.81L19.73 21 21 19.73l-9-9L4.27 3zM12 4L9.91 6.09 12 8.18V4z"/>
                  )}
                </svg>
              </button>
              <button onClick={() => setShowRules(true)} className="w-6 h-6 rounded-full border-[2px] border-[#4a2810] bg-transparent flex items-center justify-center hover:bg-black/10 active:scale-95 transition-all p-0.5">
                <span className="text-[#4a2810] font-black text-[18px] leading-none font-serif">?</span>
              </button>
            </div>

            <div className="absolute bottom-[64vh] left-1/2 -translate-x-1/2 z-30">
              <span className="text-white font-bold text-[10px] drop-shadow-md tracking-wide">
                Round of Today {gameState.round}
              </span>
            </div>

            <div className="absolute bottom-[64vh] right-7 z-30 flex items-center gap-0.5">
              <button
                onClick={handleMinimizeClick}
                className="w-6 h-6 rounded-full border-[2px] border-[#4a2810] bg-transparent flex items-center justify-center hover:bg-black/10 active:scale-95 transition-all p-0.5"
              >
                <svg viewBox="0 0 24 24" className="w-[18px] h-[18px] fill-none stroke-[#4a2810] stroke-[4]" strokeLinecap="round" strokeLinejoin="round"><polyline points="6 9 12 15 18 9"></polyline></svg>
              </button>
              <button onClick={() => setShowHistory(true)} className="w-6 h-6 rounded-full border-[2px] border-[#4a2810] bg-transparent flex items-center justify-center hover:bg-black/10 active:scale-95 transition-all p-0.5">
                <svg viewBox="0 0 24 24" className="w-full h-full fill-[#4a2810]"><path fillRule="evenodd" clipRule="evenodd" d="M12 22C17.5228 22 22 17.5228 22 12C22 6.47715 17.5228 2 12 2C6.47715 2 2 6.47715 2 12C2 17.5228 6.47715 22 12 22ZM12.5 7V12.25L17 14.92L16.25 16.15L11 13V7H12.5Z" /></svg>
              </button>
              <button onClick={handleFullClose} className="w-6 h-6 rounded-full border-[2px] border-[#4a2810] bg-transparent flex items-center justify-center hover:bg-black/10 active:scale-95 transition-all p-0.5">
                <svg viewBox="0 0 24 24" className="w-full h-full fill-[#4a2810] stroke-[#4a2810] stroke-[1.5]"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg>
              </button>
            </div>
          </>
        )}

        {loading ? (
          <div className="w-full h-full bg-gradient-to-b from-[#4A154B] via-[#330c36] to-[#1e0520] flex flex-col items-center justify-center px-6">
            <div className="w-32 h-32 flex items-center justify-center mb-6">
              <img src="/fruit-party-logo.jpg" alt="Fruit Party Loading" className="w-full h-full object-contain rounded-[20px]" />
            </div>
            <div className="w-48 bg-black/40 rounded-full h-3 p-0.5 border border-yellow-300/40 shadow-inner">
              <div className="bg-gradient-to-r from-yellow-400 to-amber-300 h-full rounded-full transition-all duration-150 ease-out shadow-[0_0_8px_rgba(250,204,21,0.7)]" style={{ width: `${progress}%` }} />
            </div>
            <span className="text-yellow-300 text-xs font-semibold mt-2 tracking-wider">LOADING {progress}%</span>
          </div>
        ) : (
          <div className="relative w-full h-full flex items-center justify-center overflow-hidden">
            <img src="/1787413631876~2.jpg" alt="Fruit Party Background" className="absolute inset-0 w-full h-full object-fill pointer-events-none" />

            {/* BUTTONS: Reduced gap to 'gap-1' (4px) and increased width to 100px & height to 120px slightly */}
            <div className="absolute bottom-[14vh] left-1/2 z-30 flex flex-row items-end -space-x-2.5 w-max" style={{ transform: `translateX(-50%) scale(${scale})`, transformOrigin: 'bottom center' }}>
              <button onClick={() => setActiveBtn(1)} className="relative flex flex-col items-center w-[100px] h-[120px] cursor-pointer">
                <img src="/file_00000000d9b08211b0304c61b802348b.png" className={`absolute left-1/2 -translate-x-1/2 w-[105px] h-auto object-contain transition-all duration-150 ${activeBtn === 1 ? 'top-[44px] hue-rotate-[120deg] brightness-110 saturate-150 z-0' : 'top-[36px] z-10'}`} />
                <img src="/file_000000003d24821182882f8ca412d2b6.png" className={`absolute top-[42px] left-1/2 -translate-x-1/2 w-[115px] h-auto object-contain pointer-events-none ${activeBtn === 1 ? 'z-10' : 'z-0'}`} />
                <span className="absolute bottom-[42px] text-white font-bold text-[14px] drop-shadow-[0_2px_4px_rgba(0,0,0,0.8)] z-20 pointer-events-none">1K</span>
              </button>
              <button onClick={() => setActiveBtn(2)} className="relative flex flex-col items-center w-[100px] h-[120px] cursor-pointer">
                <img src="/file_00000000d9b08211b0304c61b802348b.png" className={`absolute left-1/2 -translate-x-1/2 w-[105px] h-auto object-contain transition-all duration-150 ${activeBtn === 2 ? 'top-[44px] hue-rotate-[120deg] brightness-110 saturate-150 z-0' : 'top-[36px] z-10'}`} />
                <img src="/file_000000003d24821182882f8ca412d2b6.png" className={`absolute top-[42px] left-1/2 -translate-x-1/2 w-[115px] h-auto object-contain pointer-events-none ${activeBtn === 2 ? 'z-10' : 'z-0'}`} />
                <span className="absolute bottom-[42px] text-white font-bold text-[14px] drop-shadow-[0_2px_4px_rgba(0,0,0,0.8)] z-20 pointer-events-none">50K</span>
              </button>
              <button onClick={() => setActiveBtn(3)} className="relative flex flex-col items-center w-[100px] h-[120px] cursor-pointer">
                <img src="/file_00000000d9b08211b0304c61b802348b.png" className={`absolute left-1/2 -translate-x-1/2 w-[105px] h-auto object-contain transition-all duration-150 ${activeBtn === 3 ? 'top-[44px] hue-rotate-[120deg] brightness-110 saturate-150 z-0' : 'top-[36px] z-10'}`} />
                <img src="/file_000000003d24821182882f8ca412d2b6.png" className={`absolute top-[42px] left-1/2 -translate-x-1/2 w-[115px] h-auto object-contain pointer-events-none ${activeBtn === 3 ? 'z-10' : 'z-0'}`} />
                <span className="absolute bottom-[42px] text-white font-bold text-[14px] drop-shadow-[0_2px_4px_rgba(0,0,0,0.8)] z-20 pointer-events-none">1M</span>
              </button>
              <button onClick={() => setActiveBtn(4)} className="relative flex flex-col items-center w-[100px] h-[120px] cursor-pointer">
                <img src="/file_00000000d9b08211b0304c61b802348b.png" className={`absolute left-1/2 -translate-x-1/2 w-[105px] h-auto object-contain transition-all duration-150 ${activeBtn === 4 ? 'top-[44px] hue-rotate-[120deg] brightness-110 saturate-150 z-0' : 'top-[36px] z-10'}`} />
                <img src="/file_000000003d24821182882f8ca412d2b6.png" className={`absolute top-[42px] left-1/2 -translate-x-1/2 w-[115px] h-auto object-contain pointer-events-none ${activeBtn === 4 ? 'z-10' : 'z-0'}`} />
                <span className="absolute bottom-[42px] text-white font-bold text-[14px] drop-shadow-[0_2px_4px_rgba(0,0,0,0.8)] z-20 pointer-events-none">5M</span>
              </button>
            </div>

            <div className="absolute z-40 flex flex-row gap-2 overflow-x-auto no-scrollbar items-center" style={{ bottom: '2vh', left: '7vh', right: '2vh' }}>
              {winners.slice().reverse().map((imgUrl, i) => {
                return (
                  <div key={i} className="relative animate-fade-in-up flex items-center justify-center w-4 h-4 bg-[#4a2810] rounded-full shadow-md border-[1.5px] border-[#3a1d09] flex-shrink-0">
                    <img src={imgUrl} className="w-3 h-3 object-contain drop-shadow-[0_2px_4px_rgba(0,0,0,0.8)]" alt="winner" />

                    {i === 0 && (
                      <div className="absolute -bottom-[2px] left-1/2 -translate-x-1/2 bg-green-500 text-white text-[5px] font-black px-1 py-[0.5px] rounded-[2px] leading-tight z-10 shadow-md border-[0.5px] border-green-300">
                        NEW
                      </div>
                    )}
                  </div>
                );
              })}
            </div>

            <div className="absolute bottom-[6vh] z-30 flex items-center justify-start gap-1 w-[110px]" style={{ left: '8vh' }}>
              <div className="w-5 h-5 flex-shrink-0"><WebGLShaderImage src="/file_00000000e56882119c217d508b6733dc.png" /></div>
              <span className={`text-white font-bold drop-shadow-md truncate pt-0.5 leading-none ${getDynamicTextSize(balance)}`}>{balance}</span>
            </div>

            <div className="absolute bottom-[6vh] z-30 flex items-center justify-start gap-1 w-[110px]" style={{ right: '4vh' }}>
              <div className="w-5 h-5 flex-shrink-0"><WebGLShaderImage src="/file_00000000e56882119c217d508b6733dc.png" /></div>
              <span className={`text-white font-bold drop-shadow-md truncate pt-0.5 leading-none ${getDynamicTextSize(totalWon)}`}>{totalWon}</span>
            </div>

            {gameState.phase === 'result' && gameState.showResultPopup && (
              <div className="absolute bottom-0 left-0 w-full h-[44vh] z-[75] animate-slide-up overflow-hidden rounded-md">
                <img src="/file_00000000ced481fa9117afc4fa91791e.png" className="absolute inset-0 w-full h-full object-fill z-0" />
                
                <div className="absolute left-0 w-full px-4 flex justify-center items-center z-10" style={{ bottom: '40vh' }}>
                  <span className="text-white font-bold text-lg drop-shadow-lg tracking-wide">
                    Round {gameState.round}
                  </span>
                  <span className="absolute right-4 text-amber-400 font-extrabold text-xl drop-shadow-[0_2px_4px_rgba(0,0,0,0.8)]">
                    {gameState.countdown}s
                  </span>
                </div>

                <div className="absolute left-1/2 -translate-x-1/2 z-10 flex items-center justify-center w-[200px] h-[190px]" style={{ top: '1vh' }}>
                  <img src="/file_00000000eb0081f4885ade7d7db3bef8.png" className="absolute inset-0 w-full h-full object-contain pointer-events-none drop-shadow-2xl" />
                  <img src={popupWinnerImg} className="w-[50px] h-[50px] object-contain z-10 pointer-events-none drop-shadow-md" />
                </div>

                <div className="absolute left-1/2 -translate-x-1/2 z-10 flex flex-col items-center w-full" style={{ bottom: '16.5vh' }}>
                  <div className="flex items-center gap-1.5 text-white text-[16px] font-bold drop-shadow-md">
                    <span>Your Winning Amount</span>
                    <div className="w-[16px] h-[16px] flex-shrink-0"><WebGLShaderImage src="/file_00000000e56882119c217d508b6733dc.png" /></div>
                    <span className="text-green-400">{lastRoundStats.won}</span>
                  </div>
                  <div className="flex items-center gap-1.5 text-white text-[16px] font-bold drop-shadow-md mt-0.5">
                    <span>Your Bet Amount</span>
                    <div className="w-[16px] h-[16px] flex-shrink-0"><WebGLShaderImage src="/file_00000000e56882119c217d508b6733dc.png" /></div>
                    <span className="text-yellow-300">{lastRoundStats.bet}</span>
                  </div>
                </div>

                <div className="absolute left-0 w-full z-10 flex flex-col items-center" style={{ bottom: '2vh' }}>
                  <div className="w-[85%] h-[1px] bg-white/20 mb-1" />
                  <span className="text-yellow-100 font-extrabold text-[10px] mb-1 drop-shadow-md uppercase tracking-wider">Top winner Of this Round</span>
                  
                  <div className="flex flex-row items-end justify-center gap-8 w-full px-2">
                    {realWinners.slice(0, 3).map((winner, idx) => {
                      const colors = [
                        { ring: 'from-cyan-400 to-blue-600', border: 'border-amber-400', shadow: 'shadow-[0_0_8px_rgba(251,191,36,0.6)]', badge: 'from-yellow-300 to-yellow-600', text: 'text-yellow-300' },
                        { ring: 'from-pink-400 to-red-500', border: 'border-gray-300', shadow: 'shadow-[0_0_8px_rgba(209,213,219,0.5)]', badge: 'from-gray-200 to-gray-500', text: 'text-gray-200' },
                        { ring: 'from-green-400 to-teal-500', border: 'border-orange-500', shadow: 'shadow-[0_0_8px_rgba(249,115,22,0.5)]', badge: 'from-orange-300 to-orange-600', text: 'text-orange-300' }
                      ][idx];

                      const sizes = [
                        { container: 'w-[38px] h-[38px]', badge: 'w-[16px] h-[16px]' },
                        { container: 'w-[34px] h-[34px]', badge: 'w-[14px] h-[14px]' },
                        { container: 'w-[32px] h-[32px]', badge: 'w-[14px] h-[14px]' },
                      ][idx];

                      return (
                        <div key={idx} className="flex flex-col items-center gap-0.5 w-[60px] animate-fade-in-up" style={{ animationDelay: `${0.1 * (idx + 1)}s` }}>
                          <div className={`relative ${sizes.container}`}>
                            <div className={`w-full h-full rounded-full bg-gradient-to-tr ${colors.ring} border-[2px] ${colors.border} ${colors.shadow} overflow-hidden`}>
                              <img src={winner.avatar || '/default-avatar.png'} className="w-full h-full object-cover" />
                            </div>
                            <div className={`absolute -top-1 -left-1 ${sizes.badge} bg-gradient-to-br ${colors.badge} rounded-full flex items-center justify-center border border-white shadow-md`}>
                              <span className={`text-[${idx === 0 ? '9' : '8'}px] font-black text-black leading-none mt-[1px]`}>{idx + 1}</span>
                            </div>
                          </div>
                          <span className="text-white text-[10px] font-bold drop-shadow-md truncate w-full text-center mt-0.5">{winner.name}</span>
                          <div className="flex items-center justify-center gap-1 w-full">
                            <div className="w-3 h-3 flex-shrink-0"><WebGLShaderImage src="/file_00000000e56882119c217d508b6733dc.png" /></div>
                            <span className={`${colors.text} text-[9px] font-extrabold drop-shadow-md truncate`}>{winner.win}</span>
                          </div>
                        </div>
                      );
                    })}
                    {realWinners.length === 0 && (
                      <span className="text-white text-xs font-bold my-4">No winners yet.</span>
                    )}
                  </div>
                </div>
              </div>
            )}
          </div>
        )}
      </div>

      {showHistory && (
        <div className="fixed inset-0 z-[80] flex items-end justify-center">
          <div className="absolute inset-0 bg-transparent" onClick={() => setShowHistory(false)} />
          <div className="relative bg-black/95 w-full max-w-md h-[45vh] rounded-t-md shadow-2xl flex flex-col overflow-hidden text-white animate-slide-up" onClick={(e) => e.stopPropagation()}>
            <div className="flex items-center justify-between px-4 py-3 border-b border-gray-800">
              <button onClick={() => setShowHistory(false)} className="w-6 h-6 flex items-center justify-center active:scale-95 transition-all">
                <svg viewBox="0 0 24 24" className="w-5 h-5 fill-white"><path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/></svg>
              </button>
              <span className="font-bold text-base tracking-wide">My Records</span>
              <div className="w-6" /> 
            </div>
            
            <div className="flex-1 overflow-y-auto px-4 py-2 flex flex-col">
              {
                roundHistory.filter(item => Object.values(item.bets || {}).some(val => val > 0)).length === 0 ? (
                  <div className="flex items-center justify-center h-full text-gray-400 text-xs font-medium">No played records available yet.</div>
                ) : (
                  roundHistory
                    .filter(item => Object.values(item.bets || {}).some(val => val > 0)) 
                    .slice(0, 15) 
                    .map((item, index) => {
                      const betFruitIds = Object.keys(item.bets).map(Number).filter(id => item.bets[id] > 0);

                      const getBetItemInfo = (id: number) => {
                         if (id === 10) return { id: 10, img: '/IMG_20260910_114515.png', multi: '×20' };
                         if (id === 11) return { id: 11, img: '/IMG_20260910_114613.png', multi: '×95' };
                         return GRID_ITEMS.find(f => f.id === id);
                      };

                      return (
                        <div key={index} className="flex flex-col mb-4 pb-4 border-b border-gray-800">
                          <div className="text-sm font-bold text-gray-200">Round {item.round}</div>
                          <div className="flex items-center gap-2 mt-1 mb-3">
                            <span className="text-[11px] text-gray-400 font-medium">Award Results:</span>
                            <img src={item.winnerImg} className="w-5 h-5 object-contain drop-shadow-md" alt="Winner Fruit" />
                          </div>

                          <div className="grid grid-cols-3 gap-2 text-center text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-2">
                            <span>Fruit</span>
                            <span>Bet</span>
                            <span>Award</span>
                          </div>

                          <div className="flex flex-col gap-1.5">
                            {betFruitIds.map(fruitId => {
                              const fruit = getBetItemInfo(fruitId);
                              if (!fruit) return null;

                              const betAmt = item.bets[fruitId];
                              let awardAmt = 0;
                              
                              if (item.winnerId === 10) {
                                 if (fruitId === 10) awardAmt = betAmt * 20; 
                                 if ([1,3,7,6].includes(fruitId)) awardAmt = betAmt * 5;
                              } else if (item.winnerId === 11) {
                                 if (fruitId === 11) awardAmt = betAmt * 95; 
                                 if (fruitId === 5) awardAmt = betAmt * 10;
                                 if (fruitId === 4) awardAmt = betAmt * 15;
                                 if (fruitId === 2) awardAmt = betAmt * 25;
                                 if (fruitId === 8) awardAmt = betAmt * 45;
                              } else {
                                 if (fruitId === item.winnerId) {
                                    const mult = parseInt(fruit.multi?.replace('×', '') || '0');
                                    awardAmt = betAmt * mult;
                                 }
                              }

                              return (
                                <div key={fruit.id} className="grid grid-cols-3 gap-2 items-center text-center">
                                  <div className="flex justify-center">
                                    <img src={fruit.img} className="w-5 h-5 object-contain drop-shadow-md" alt="Bet Fruit" />
                                  </div>
                                  <div className="flex justify-center items-center gap-1">
                                    <div className="w-3.5 h-3.5 flex-shrink-0"><WebGLShaderImage src="/file_00000000e56882119c217d508b6733dc.png" /></div>
                                    <span className="text-gray-200 text-xs font-semibold">{betAmt}</span>
                                  </div>
                                  <div className="flex justify-center items-center gap-1">
                                    <div className="w-3.5 h-3.5 flex-shrink-0"><WebGLShaderImage src="/file_00000000e56882119c217d508b6733dc.png" /></div>
                                    <span className={`text-xs font-semibold ${awardAmt > 0 ? 'text-green-400' : 'text-gray-400'}`}>{awardAmt}</span>
                                  </div>
                                </div>
                              );
                            })}
                          </div>
                        </div>
                      );
                    })
                )
              }
            </div>
          </div>
        </div>
      )}

      {showRules && (
        <div className="fixed inset-0 z-[80] flex items-end justify-center">
          <div className="absolute inset-0 bg-transparent" onClick={() => setShowRules(false)} />
          <div className="relative bg-black w-full max-w-md h-[40vh] rounded-t-md shadow-2xl flex flex-col overflow-hidden text-white animate-slide-up" onClick={(e) => e.stopPropagation()}>
            <div className="flex items-center justify-between px-4 py-3">
              <button onClick={() => setShowRules(false)} className="w-6 h-6 flex items-center justify-center active:scale-95 transition-all">
                <svg viewBox="0 0 24 24" className="w-5 h-5 fill-white"><path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/></svg>
              </button>
              <span className="font-bold text-base tracking-wide">Rules</span>
              <div className="w-6" /> 
            </div>
            <div className="flex-1 overflow-y-auto px-6 py-2 pb-6">
              <ul className="list-disc space-y-3 text-sm text-gray-200 font-medium">
                <li>You can place bet by Clicking on the Red button and Place the bet On the Fruit Card</li>
                <li>10,15,25,45 give you hight coins</li>
                <li>You will Receive Coins according to ( Your Bet × Multipler)</li>
                <li>Left mix Card Give you all 5 times Coins ( Your Bet × all ×5 Times )</li>
                <li>Right Mix card Give you all High Cards ( Your Bet × 10,15,25,45 Times)</li>
              </ul>
            </div>
          </div>
        </div>
      )}

      <style jsx>{`
        @keyframes slideUp { from { transform: translateY(100%); } to { transform: translateY(0); } }
        @keyframes fadeInUp { from { opacity: 0; transform: translateY(10px) scale(0.8); } to { opacity: 1; transform: translateY(0) scale(1); } }
        @keyframes singleBlink {
          0% { opacity: 1; filter: brightness(1); }
          50% { opacity: 0.15; filter: brightness(2.2); }
          100% { opacity: 1; filter: brightness(1); }
        }
        
        /* EKDUM SUBTLE SHAKE (Pata chalega but bahut smooth hoga) */
        @keyframes subtleShake {
          0% { transform: translate(0, 0) rotate(0deg); }
          25% { transform: translate(-0.5px, 0.5px) rotate(-0.5deg); }
          50% { transform: translate(0.5px, -0.5px) rotate(0.5deg); }
          75% { transform: translate(0.5px, 0.5px) rotate(0deg); }
          100% { transform: translate(0, 0) rotate(0deg); }
        }
        
        .animate-slide-up { animation: slideUp 0.3s ease-out; }
        .animate-fade-in-up { animation: fadeInUp 0.4s ease-out forwards; }
        .animate-single-blink { animation: singleBlink 0.5s ease-in-out 1; }
        .animate-subtle-shake { animation: subtleShake 0.1s infinite; }
        
        /* HIDDEN SCROLLBAR FOR HISTORY PATTI */
        .no-scrollbar::-webkit-scrollbar { display: none; }
        .no-scrollbar { -ms-overflow-style: none; scrollbar-width: none; }
      `}</style>
    </div>
  );
}

