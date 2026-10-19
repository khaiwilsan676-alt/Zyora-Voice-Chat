
'use client';

import React from 'react';

interface RoomInfoProps {
  isOpen: boolean;
  onClose: () => void;
  isRoomOwner: boolean;
  roomOwner: {
    id?: string;
    uid?: string;
    accountId?: string;
    name: string;
    image: string;
  };
  roomData: {
    roomName: string;
    roomDp: string;
    roomAnnouncement: string;
    roomId: string;
  };
  roomFollowers: Array<{
    accountId: string;
    name: string;
    image: string;
  }>;
  onOpenProfile: (user: { name: string; image: string; accountId: string }) => void;
  onCopyId: (text: string, e: React.MouseEvent) => void;
  copied: boolean;
}

export default function RoomInfo({
  isOpen,
  onClose,
  isRoomOwner,
  roomOwner,
  roomData,
  roomFollowers,
  onOpenProfile,
  onCopyId,
  copied
}: RoomInfoProps) {
  const [activeTab, setActiveTab] = React.useState<'profile' | 'members'>('profile');

  if (!isOpen) return null;

  return (
    <div className="absolute inset-0 z-40 flex items-end justify-center">
      <div className="absolute inset-0 bg-black/30" onClick={onClose} />
      <div 
        className="relative bg-white w-full max-w-md rounded-t-3xl shadow-2xl animate-slide-up overflow-hidden" 
        style={{ height: '50vh' }} 
        onClick={(e) => e.stopPropagation()}
      >
        {!isRoomOwner && (
          <svg 
            viewBox="0 0 24 24" 
            className="absolute top-3 left-3 w-6 h-6 fill-none stroke-black stroke-[2] stroke-linecap-round stroke-linejoin-round"
          >
            <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
            <line x1="12" y1="9" x2="12" y2="13" />
            <line x1="12" y1="17" x2="12.01" y2="17" />
          </svg>
        )}

        <div className="px-6 pt-6 pb-2 flex items-center justify-center">
          <h2 className="text-lg font-bold text-gray-800">Room Information</h2>
        </div>

        <div className="flex border-b border-gray-200 px-6">
          <button 
            onClick={() => setActiveTab('profile')} 
            className={`flex-1 py-3 text-sm font-semibold transition-all cursor-pointer ${
              activeTab === 'profile' 
                ? 'text-black border-b-2 border-black' 
                : 'text-gray-400 hover:text-gray-600'
            }`}
          >
            Profile
          </button>
          <button 
            onClick={() => setActiveTab('members')} 
            className={`flex-1 py-3 text-sm font-semibold transition-all cursor-pointer ${
              activeTab === 'members' 
                ? 'text-black border-b-2 border-black' 
                : 'text-gray-400 hover:text-gray-600'
            }`}
          >
            Members
          </button>
        </div>

        <div className="flex-1 overflow-y-auto px-6 py-4">
          {activeTab === 'profile' ? (
            <div className="space-y-4">
              <div className="flex items-center gap-3">
                <div className="w-14 h-14 rounded-xl overflow-hidden border border-gray-200 flex-shrink-0">
                  <img 
                    src={roomData.roomDp}
                    alt="Room" 
                    className="w-full h-full object-cover" 
                  />
                </div>
                <div>
                  <h3 className="font-semibold text-gray-800">
                    {roomData.roomName || 'Room'}
                  </h3>
                  <div className="flex items-center gap-1 text-xs text-gray-400">
                    <span>ID: {roomOwner.accountId}</span>
                    <button 
                      onClick={(e) => onCopyId(roomOwner.accountId || '', e)} 
                      className="p-0.5 hover:bg-gray-100 rounded transition-colors cursor-pointer" 
                      title="Copy ID"
                    >
                      <svg 
                        viewBox="0 0 24 24" 
                        className="w-4 h-4 fill-none stroke-gray-500 stroke-[2] stroke-linecap-round stroke-linejoin-round"
                      >
                        <rect x="9" y="9" width="13" height="13" rx="2" ry="2" />
                        <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" />
                      </svg>
                    </button>
                    {copied && <span className="text-green-500 text-xs">Copied!</span>}
                  </div>
                </div>
              </div>

              <div>
                <span className="text-xs text-gray-400 font-medium">Host</span>
                <p className="text-sm font-medium text-gray-800 mt-1">
                  {roomOwner.name || "Unknown"}
                </p>
              </div>

              <div>
                <span className="text-xs text-gray-400 font-medium">Announcement:</span>
                <p className="text-sm text-gray-700 mt-1 leading-relaxed">
                  {roomData.roomAnnouncement || '—'}
                </p>
              </div>
            </div>
          ) : (
            <div className="space-y-2">
              <div className="flex items-center gap-3 bg-gray-50 rounded-lg px-3 py-2.5">
                <div 
                  className="w-9 h-9 rounded-full overflow-hidden flex-shrink-0 cursor-pointer" 
                  onClick={() => onOpenProfile({ 
                    name: roomOwner.name, 
                    image: roomOwner.image, 
                    accountId: roomOwner.accountId || roomOwner.id || '' 
                  })}
                >
                  <img 
                    src={roomOwner.image || "/default-avatar.png"} 
                    alt={roomOwner.name} 
                    className="w-full h-full object-cover" 
                    onError={(e) => { (e.target as HTMLImageElement).src = "/default-avatar.png" }} 
                  />
                </div>
                <div className="flex-1 min-w-0 flex items-center gap-2">
                  <h4 className="text-sm font-medium text-gray-800 truncate">
                    {roomOwner.name}
                  </h4>
                  <span className="w-5 h-5 rounded-full bg-blue-500 flex items-center justify-center flex-shrink-0">
                    <svg viewBox="0 0 24 24" className="w-3 h-3 fill-white">
                      <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
                    </svg>
                  </span>
                </div>
              </div>

              {roomFollowers.map(follower => (
                <div 
                  key={follower.accountId} 
                  className="flex items-center gap-3 bg-gray-50 rounded-lg px-3 py-2.5"
                >
                  <div 
                    className="w-9 h-9 rounded-full overflow-hidden flex-shrink-0 cursor-pointer" 
                    onClick={() => onOpenProfile({ 
                      name: follower.name, 
                      image: follower.image || "/default-avatar.png", 
                      accountId: follower.accountId 
                    })}
                  >
                    <img 
                      src={follower.image || "/default-avatar.png"} 
                      alt={follower.name} 
                      className="w-full h-full object-cover" 
                      onError={(e) => { (e.target as HTMLImageElement).src = "/default-avatar.png" }} 
                    />
                  </div>
                  <div className="flex-1 min-w-0">
                    <h4 className="text-sm font-medium text-gray-800 truncate">
                      {follower.name}
                    </h4>
                  </div>
                </div>
              ))}

              {roomFollowers.length === 0 && (
                <p className="text-center text-gray-400 text-sm py-8">
                  No followers yet
                </p>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
