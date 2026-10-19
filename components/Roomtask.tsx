'use client';

import React, { useEffect, useState } from 'react';

interface RoomtaskProps {
  onBack?: () => void;
}

function useProcessedShaderImage(src: string) {
  const [processedSrc, setProcessedSrc] = useState<string>(src);

  useEffect(() => {
    const canvas = document.createElement('canvas');
    const gl = canvas.getContext('webgl', { preserveDrawingBuffer: true, premultipliedAlpha: false });
    if (!gl) return;

    const vsSource = `
      attribute vec2 a_position;
      attribute vec2 a_texCoord;
      varying vec2 v_texCoord;
      void main() {
        gl_Position = vec4(a_position, 0.0, 1.0);
        v_texCoord = a_texCoord;
      }
    `;

    const fsSource = `
      precision mediump float;
      varying vec2 v_texCoord;
      uniform sampler2D u_image;
      void main() {
        vec4 color = texture2D(u_image, v_texCoord);
        if (color.r > 0.9 && color.g > 0.9 && color.b > 0.9) {
          gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0);
        } else {
          gl_FragColor = color;
        }
      }
    `;

    const createShader = (gl: WebGLRenderingContext, type: number, source: string) => {
      const shader = gl.createShader(type);
      if (!shader) return null;
      gl.shaderSource(shader, source);
      gl.compileShader(shader);
      return shader;
    };

    const vertShader = createShader(gl, gl.VERTEX_SHADER, vsSource);
    const fragShader = createShader(gl, gl.FRAGMENT_SHADER, fsSource);
    if (!vertShader || !fragShader) return;

    const program = gl.createProgram();
    if (!program) return;
    gl.attachShader(program, vertShader);
    gl.attachShader(program, fragShader);
    gl.linkProgram(program);
    gl.useProgram(program);

    const positionBuffer = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER, positionBuffer);
    gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([
      -1, -1,  1, -1, -1,  1,
      -1,  1,  1, -1,  1,  1,
    ]), gl.STATIC_DRAW);

    const posAttrLocation = gl.getAttribLocation(program, 'a_position');
    gl.enableVertexAttribArray(posAttrLocation);
    gl.vertexAttribPointer(posAttrLocation, 2, gl.FLOAT, false, 0, 0);

    const texCoordBuffer = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER, texCoordBuffer);
    gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([
      0, 1,  1, 1,  0, 0,
      0, 0,  1, 1,  1, 0,
    ]), gl.STATIC_DRAW);

    const texCoordLocation = gl.getAttribLocation(program, 'a_texCoord');
    gl.enableVertexAttribArray(texCoordLocation);
    gl.vertexAttribPointer(texCoordLocation, 2, gl.FLOAT, false, 0, 0);

    const image = new Image();
    image.crossOrigin = 'anonymous';
    image.src = src;
    image.onload = () => {
      canvas.width = image.width;
      canvas.height = image.height;
      gl.viewport(0, 0, gl.canvas.width, gl.canvas.height);

      const texture = gl.createTexture();
      gl.bindTexture(gl.TEXTURE_2D, texture);
      gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
      gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
      gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR);
      gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR);
      gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, image);

      gl.clearColor(0, 0, 0, 0);
      gl.clear(gl.COLOR_BUFFER_BIT);
      gl.drawArrays(gl.TRIANGLES, 0, 6);

      const dataUrl = canvas.toDataURL('image/png');
      setProcessedSrc(dataUrl);
    };
  }, [src]);

  return processedSrc;
}

function TaskItem({ 
  title, 
  reward,
  iconSrc
}: { 
  title: string; 
  reward: string;
  iconSrc: string;
}) {
  
  const lowerTitle = title.toLowerCase();
  
  let leftIconSrc = iconSrc; 
  let iconSize = "w-10 h-10"; 

  if (lowerTitle.includes('mic')) {
    leftIconSrc = '/file_00000000f8d88211ba5ff45c06383e5f.png';
    iconSize = "w-20 h-20";
  } else if (lowerTitle.includes('share')) {
    leftIconSrc = '/file_0000000019a0821193463686d6fc9184.png';
    iconSize = "w-14.5 h-14.5 ml-2";
  } else if (lowerTitle.includes('gift')) {
    leftIconSrc = '/file_0000000081f48211afe58f6348196b55.png';
    iconSize = "w-20 h-20";
  } else if (lowerTitle.includes('user') || lowerTitle.includes('follower')) {
    leftIconSrc = '/file_00000000858082118b5c81b85cd6d2a8.png';
    iconSize = "w-20 h-20";
  }

  const rewardValue = reward.replace(/coins/gi, '').trim();

  return (
    <div className="relative z-20 w-[100%] max-w-[410px] h-[175px] flex items-center rounded-none">
      <img 
        src="/file_000000004fd0821198ed4e26d5008b16.png"
        alt="Task Background"
        className="absolute inset-0 w-full h-full object-fill cursor-pointer transition-transform hover:scale-105 active:scale-95 select-none z-0 rounded-none"
        draggable={false}
      />

      <div className="relative z-30 w-full pl-2 pr-4 flex items-center justify-between pointer-events-none rounded-none">
        
        <div className="flex items-center space-x-0.5 flex-1">
          <img 
            src={leftIconSrc} 
            alt="Task Icon" 
            className={`${iconSize} object-contain flex-shrink-0 ${leftIconSrc === iconSrc ? '' : 'drop-shadow-md'} select-none rounded-none`}
            draggable={false}
          />
          <div className="flex flex-col justify-center">
            <span className="text-[15px] sm:text-[16px] font-bold text-white leading-tight drop-shadow-md line-clamp-3">
              {title}
            </span>
          </div>
        </div>

        <div className="flex-shrink-0 pointer-events-auto flex flex-col items-center justify-center space-y-1.5 pl-1">
          
          <div className="flex items-center space-x-1">
            <img 
              src={iconSrc} 
              alt="Coins" 
              className="w-6 h-6 object-contain flex-shrink-0 select-none rounded-none"
              draggable={false}
            />
            <span className="text-[15px] font-extrabold text-[#ffd700] drop-shadow-md whitespace-nowrap">
              {rewardValue}
            </span>
          </div>

          <button 
            onClick={() => {}}
            className="w-[85px] h-[28px] rounded-full font-black text-[11px] uppercase tracking-wider
                       bg-gradient-to-b from-[#f7dfa0] to-[#e0a92a]
                       text-[#4a1f00]
                       border-2 border-[#f5e6a8]
                       shadow-[0_0_6px_rgba(255,215,0,0.5),0_3px_0_#9a5f00,inset_0_2px_4px_rgba(255,255,255,0.7)]
                       active:translate-y-[2px] active:shadow-[0_0_5px_rgba(255,215,0,0.5),0_1px_0_#9a5f00,inset_0_2px_4px_rgba(255,255,255,0.7)]
                       transition-all duration-100 cursor-pointer outline-none flex items-center justify-center"
          >
            Claim
          </button>

        </div>
      </div>
    </div>
  );
}

export default function Roomtask({ onBack }: RoomtaskProps) {
  
  const cleanedIconSrc = useProcessedShaderImage('/file_00000000e56882119c217d508b6733dc.png');

  const [timeLeft, setTimeLeft] = useState({ days: 0, hours: 0, minutes: 0, seconds: 0 });
  const [showRules, setShowRules] = useState(false);

  useEffect(() => {
    const timer = setInterval(() => {
      const now = new Date();
      const dayOfWeek = now.getDay();
      const daysUntilNextMonday = dayOfWeek === 0 ? 1 : 8 - dayOfWeek;
      
      const nextTarget = new Date(now);
      nextTarget.setDate(now.getDate() + daysUntilNextMonday);
      nextTarget.setHours(0, 0, 0, 0);

      const diff = nextTarget.getTime() - now.getTime();

      if (diff > 0) {
        setTimeLeft({
          days: Math.floor(diff / (1000 * 60 * 60 * 24)),
          hours: Math.floor((diff / (1000 * 60 * 60)) % 24),
          minutes: Math.floor((diff / 1000 / 60) % 60),
          seconds: Math.floor((diff / 1000) % 60),
        });
      }
    }, 1000);

    return () => clearInterval(timer);
  }, []);

  const tasks = [
    { title: "10 Users Enter the Room", reward: "10,000" },
    { title: "5 Users Enter the Room for Two Consecutive Days", reward: "20,000" },
    { title: "Successfully Share on WhatsApp", reward: "5,000" }, 
    { title: "Successfully Share on Facebook", reward: "5,000" },
    { title: "3 New Users Enter the Room", reward: "10,000" },
    { title: "1 New Follower of the Room", reward: "2,000" },
    { title: "3 New Users Follow the Room", reward: "10,000" },
    { title: "Spend 10 Minutes on Mic", reward: "5,000" },
    { title: "Spend 30 Minutes on Mic", reward: "25,000" },
    { title: "Spend 60 Minutes on Mic", reward: "50,000" },
    { title: "10 New Users on Mic for 10 Minutes at the Same Time", reward: "50,000" }, 
    { title: "5 New Followers of the Room", reward: "25,000" },
    { title: "3 Users on Mic at the Same Time", reward: "10,000" },
    { title: "Send a Gift of 500", reward: "2,000" },
    { title: "Send Gifts of 5,000", reward: "10,000" },
    { title: "Successfully Invite 1 User on Mic", reward: "500" },
    { title: "Successfully Invite 3 New Users on Mic", reward: "10,000" },
    { title: "Send 10 Gifts on Mic", reward: "20,000" },
    { title: "New User Sends a Gift of 1,000 on Mic", reward: "5,000" },
    { title: "Successfully Invite 10 People to the Mic", reward: "25,000" },
    { title: "3 Users on Mic at the Same Time for 10 Minutes", reward: "10,000" },
    { title: "Send 1 Gift", reward: " 1,000" },
  ];

  return (
    <div className="relative w-full h-[100dvh] overflow-y-auto overflow-x-hidden bg-[#380308] scrollbar-none select-none m-0 p-0 pointer-events-auto">
      
      <div className="relative w-full min-h-full flex flex-col m-0 p-0">
        
        <div 
          className="fixed top-0 left-0 w-full z-50 pointer-events-none"
          style={{ height: 'env(safe-area-inset-top, 0px)' }}
        />

        <div 
          className="absolute top-0 left-0 w-full h-[60vh] z-2 pointer-events-none bg-cover bg-top overflow-hidden"
          style={{
            backgroundImage: 'url(/file_00000000cb748211bf0120855b80f449.png)',
            maskImage: 'linear-gradient(to bottom, black 40%, rgba(0,0,0,0.6) 65%, rgba(0,0,0,0.2) 85%, transparent 100%)',
            WebkitMaskImage: 'linear-gradient(to bottom, black 40%, rgba(0,0,0,0.6) 65%, rgba(0,0,0,0.2) 85%, transparent 100%)',
          }}
        >
          <div 
            className="absolute inset-0 w-full h-full pointer-events-none"
            style={{
              background: 'linear-gradient(to bottom, rgba(56,3,8,0) 0%, rgba(56,3,8,0.2) 50%, rgba(56,3,8,0.7) 75%, #380308 100%)'
            }}
          />
        </div>

        <div 
          className="absolute top-[50vh] left-0 w-full h-[260vh] z-0 pointer-events-none bg-[#380308]"
        />

        {/* ===== BACK BUTTON - MATCHING QUESTION MARK ICON STYLE ===== */}
        <button 
          onClick={onBack} 
          className="fixed z-50 p-1 flex items-center justify-center cursor-pointer transition-transform hover:scale-110 active:scale-90"
          style={{
            top: 'calc(env(safe-area-inset-top, 0px) + 2px)',
            left: '2px'
          }}
          aria-label="Back"
        >
          <svg viewBox="0 0 24 24" className="w-8 h-8 drop-shadow-[0_2px_4px_rgba(0,0,0,0.8)]">
            <circle cx="12" cy="12" r="10" fill="rgba(0,0,0,0.5)" stroke="#ffd700" strokeWidth="1.5" />
            <line x1="15" y1="12" x2="8" y2="12" stroke="#ffd700" strokeWidth="2" strokeLinecap="round" />
            <polyline points="11.5 8.5 8 12 11.5 15.5" fill="none" stroke="#ffd700" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        </button>

        {/* ===== QUESTION MARK ICON - TOP RIGHT ===== */}
        <button
          onClick={() => setShowRules(true)}
          className="fixed z-50 p-1 flex items-center justify-center cursor-pointer transition-transform hover:scale-110 active:scale-90"
          style={{
            top: 'calc(env(safe-area-inset-top, 0px) + 2px)',
            right: '8px'
          }}
          aria-label="Rules"
        >
          <svg viewBox="0 0 24 24" className="w-8 h-8 drop-shadow-[0_2px_4px_rgba(0,0,0,0.8)]">
            <circle cx="12" cy="12" r="10" fill="rgba(0,0,0,0.5)" stroke="#ffd700" strokeWidth="1.5" />
            <text x="12" y="17" textAnchor="middle" fontSize="14" fontWeight="bold" fill="#ffd700">?</text>
          </svg>
        </button>

        <div className="relative z-10 w-full flex flex-col items-center">
          
          <div className="w-full" style={{ height: 'calc(50vh - 45px)' }}></div>

          <div className="w-full flex justify-center px-4 flex-col items-center">
            <div className="relative w-[100%] max-w-[450px] flex flex-col items-center justify-center mt-2">
              
              <div className="relative w-full flex items-center justify-center">
                <img 
                  src="/file_00000000f2908208a7b6a2b73c3bbf36.png" 
                  alt="Middle Decoration" 
                  className="w-full h-auto object-contain drop-shadow-2xl select-none rounded-none"
                  draggable={false}
                />
                
                <div 
                  className="absolute top-1/2 -translate-y-1/2 flex items-center z-20 pointer-events-none"
                  style={{ left: '10%' }} 
                >
                  <img 
                    src={cleanedIconSrc}
                    alt="Cleaned Coin Icon" 
                    className="w-7 h-7 object-contain select-none rounded-none"
                    draggable={false}
                  />
                </div>

                <div className="absolute inset-0 flex items-center justify-center z-20 pointer-events-none pr-4">
                  <span className="text-2xl font-extrabold text-[#ffd700] drop-shadow-[0_2px_4px_rgba(0,0,0,0.8)]">
                    0
                  </span>
                </div>

                <div 
                  className="absolute top-1/2 -translate-y-1/2 z-20 -mt-1"
                  style={{ right: '9%' }} 
                >
                  <button 
                    onClick={() => {}}
                    className="w-[85px] h-[28px] rounded-full bg-transparent border-2 border-transparent cursor-pointer outline-none"
                    aria-label="Top Action"
                  ></button>
                </div>
              </div>

              <div className="relative w-full flex justify-center -mt-4 z-30">
                <button 
                  onClick={() => {}}
                  className="w-[90px] h-[28px] rounded-full font-black text-[11px] uppercase tracking-widest
                             bg-gradient-to-b from-[#f7dfa0] to-[#e0a92a]
                             text-[#4a1f00]
                             border-2 border-[#f5e6a8]
                             shadow-[0_0_7px_rgba(255,215,0,0.55),0_3px_0_#9a5f00,inset_0_2px_4px_rgba(255,255,255,0.7)]
                             active:translate-y-[2px] active:shadow-[0_0_6px_rgba(255,215,0,0.55),0_1px_0_#9a5f00,inset_0_2px_4px_rgba(255,255,255,0.7)]
                             transition-all duration-100 cursor-pointer outline-none flex items-center justify-center"
                >
                  Collect
                </button>
              </div>

            </div>
          </div>

          <p className="w-full text-center text-white text-[11px] sm:text-[12px] font-medium leading-snug mt-4 px-3 relative z-40">
            *If anyone sends gifts in the room, the room owner can get <span className="text-[#ffd700] font-bold">5%coins</span> worth the gift value.
          </p>

          <div className="relative w-full flex flex-col items-center mt-1">
            
            <div className="relative w-full flex-shrink-0 z-30 flex justify-center items-center overflow-hidden">
              <img 
                src="/file_00000000680881faa3dfdb17cce60858.png"
                alt="Frame Top Border"
                className="w-full h-auto object-fill block select-none pointer-events-none rounded-none"
                style={{ transform: 'scaleX(1.12)' }}
                draggable={false}
              />

              <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
                <h1 className="text-white text-base sm:text-lg font-black tracking-wider uppercase drop-shadow-[0_2px_4px_rgba(0,0,0,0.9)] -mt-4 sm:-mt-6">
                  Room Task
                </h1>
              </div>
            </div>

            <div className="flex items-center justify-center gap-1 -mt-5 z-40 select-none">
              <span className="text-[#f5b8b8] text-[11px] font-medium">Countdown</span>

              <div className="w-[20px] h-[20px] rounded-[4px] border border-[#d85858] bg-gradient-to-b from-[#8f1a1a] to-[#470a0a] flex items-center justify-center shadow-inner">
                <span className="text-[#ffe0e0] text-[11px] font-bold">{timeLeft.hours}</span>
              </div>
              <span className="text-[#f5b8b8] text-[11px] font-medium">Hour</span>

              <div className="min-w-[20px] px-1 h-[20px] rounded-[4px] border border-[#d85858] bg-gradient-to-b from-[#8f1a1a] to-[#470a0a] flex items-center justify-center shadow-inner">
                <span className="text-[#ffe0e0] text-[11px] font-bold">{timeLeft.minutes}</span>
              </div>
              <span className="text-[#f5b8b8] text-[11px] font-medium">Minute</span>

              <div className="w-[20px] h-[20px] rounded-[4px] border border-[#d85858] bg-gradient-to-b from-[#8f1a1a] to-[#470a0a] flex items-center justify-center shadow-inner">
                <span className="text-[#ffe0e0] text-[11px] font-bold">{timeLeft.seconds}</span>
              </div>
              <span className="text-[#f5b8b8] text-[11px] font-medium">Second</span>
            </div>

            <div className="relative w-full flex flex-col items-center mt-1 -mb-3">
              
              <div 
                className="absolute -top-14 bottom-0 w-12 sm:w-16 z-10 pointer-events-none"
                style={{
                  backgroundImage: 'url(/IMG_20260903_105647.png)',
                  backgroundRepeat: 'repeat-y',
                  backgroundSize: '100% auto',
                  backgroundPosition: 'left top',
                  left: '-15px'
                }}
              />

              <div 
                className="absolute -top-14 bottom-0 w-12 sm:w-16 z-10 pointer-events-none"
                style={{
                  backgroundImage: 'url(/IMG_20260903_105705.png)',
                  backgroundRepeat: 'repeat-y',
                  backgroundSize: '100% auto',
                  backgroundPosition: 'right top',
                  right: '-15px'
                }}
              />

              <div className="w-full max-w-[390px] flex flex-col items-center -space-y-[60px] px-6 z-20 -mt-2">
                {tasks.map((task, index) => (
                  <TaskItem 
                    key={index}
                    title={task.title}
                    reward={task.reward}
                    iconSrc={cleanedIconSrc}
                  />
                ))}
              </div>
            </div>

            <div className="w-full flex-shrink-0 z-30 pointer-events-none overflow-hidden -mt-12">
              <img 
                src="/file_0000000066c88211aa777b1f6da8683f.png"
                alt="Frame Bottom Border"
                className="w-full h-auto object-fill block select-none rounded-none"
                style={{ transform: 'scaleX(1.12)' }}
                draggable={false}
              />
            </div>

          </div>

        </div>

      </div>

      {/* ===== RULES SHEET MODAL - NO BLUR ===== */}
      {showRules && (
        <div 
          className="fixed inset-0 z-[999] flex items-end justify-center"
          onClick={() => setShowRules(false)}
        >
          {/* Backdrop - sirf dark, koi blur nahi */}
          <div className="absolute inset-0 bg-black/70" />

          <div 
            className="relative w-full max-w-[500px] max-h-[85vh] overflow-y-auto rounded-t-2xl bg-[#380308] border-t-2 border-x-2 border-[#d85858] shadow-[0_-10px_40px_rgba(0,0,0,0.8)]"
            onClick={(e) => e.stopPropagation()}
            style={{
              backgroundImage: 'linear-gradient(to bottom, #4a0a10 0%, #380308 100%)',
            }}
          >
            <div className="sticky top-0 z-10 flex justify-center pt-3 pb-2 bg-gradient-to-b from-[#4a0a10] to-transparent">
              <div className="w-10 h-1 rounded-full bg-[#d85858]/60" />
            </div>

            <div className="px-5 pb-8">
              <h2 className="text-center text-white text-lg font-black tracking-wider uppercase mb-5 drop-shadow-[0_2px_4px_rgba(0,0,0,0.8)]">
                Room Task Rules
              </h2>

              <div className="space-y-4 text-[#f5d0d0] text-[13px] leading-relaxed">
                
                <div className="flex gap-3">
                  <span className="flex-shrink-0 w-6 h-6 rounded-full bg-gradient-to-b from-[#f7dfa0] to-[#e0a92a] text-[#4a1f00] text-[12px] font-black flex items-center justify-center border border-[#f5e6a8]">1</span>
                  <p>Each user can complete a 'Room Owner' task only once per day.</p>
                </div>

                <div className="flex gap-3">
                  <span className="flex-shrink-0 w-6 h-6 rounded-full bg-gradient-to-b from-[#f7dfa0] to-[#e0a92a] text-[#4a1f00] text-[12px] font-black flex items-center justify-center border border-[#f5e6a8]">2</span>
                  <p>Each user can complete a daily task once per day. Tasks reset at 5:30+GMT.</p>
                </div>

                <div className="flex gap-3">
                  <span className="flex-shrink-0 w-6 h-6 rounded-full bg-gradient-to-b from-[#f7dfa0] to-[#e0a92a] text-[#4a1f00] text-[12px] font-black flex items-center justify-center border border-[#f5e6a8]">3</span>
                  <p>For gift-giving tasks, only gifts sent within the room are counted; gifts sent via private message are not included.</p>
                </div>

                <div className="flex gap-3">
                  <span className="flex-shrink-0 w-6 h-6 rounded-full bg-gradient-to-b from-[#f7dfa0] to-[#e0a92a] text-[#4a1f00] text-[12px] font-black flex items-center justify-center border border-[#f5e6a8]">4</span>
                  <div>
                    <p className="font-bold text-white mb-1">Treasure Chest</p>
                    <p>When a gift is received in a personal room, the room owner can earn a 5% coin reward of the gift's value. There is no limit on the coin bonus; the more gifts received in the room, the more coin rewards you earn.</p>
                  </div>
                </div>

                <div className="flex gap-3">
                  <span className="flex-shrink-0 w-6 h-6 rounded-full bg-gradient-to-b from-[#f7dfa0] to-[#e0a92a] text-[#4a1f00] text-[12px] font-black flex items-center justify-center border border-[#f5e6a8]">5</span>
                  <p>If you create multiple accounts using the same device or SIM card, you can only claim rewards once for each task.</p>
                </div>

              </div>

              <p className="mt-6 text-right text-[#ffd700] text-[13px] font-bold tracking-wide">
                Regards, Zyora Team
              </p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
