
'use client';

import React from 'react';

interface ActiveUsersProps {
  isOpen: boolean;
  onClose: () => void;
  roomUsers: Array<{
    accountId: string;
    name: string;
    image: string;
  }>;
  onOpenProfile: (user: { name: string; image: string; accountId: string }) => void;
  onCopyUserId: (accountId: string, e: React.MouseEvent) => void;
}

export default function ActiveUsers({
  isOpen,
  onClose,
  roomUsers,
  onOpenProfile,
  onCopyUserId
}: ActiveUsersProps) {
  if (!isOpen) return null;

  return (
    <div className="absolute inset-0 z-40 flex items-end justify-center">
      <div className="absolute inset-0 bg-black/30" onClick={onClose} />
      <div 
        className="relative bg-white w-full max-w-md rounded-t-3xl shadow-2xl animate-slide-up overflow-hidden" 
        style={{ height: '30vh' }} 
        onClick={(e) => e.stopPropagation()}
      >
        <div className="px-6 pt-6 pb-3 border-b border-gray-200">
          <h2 className="text-lg font-bold text-gray-800 text-center">Active Users</h2>
        </div>
        <div className="flex-1 overflow-y-auto px-4 py-3">
          {roomUsers.length > 0 ? (
            <div className="space-y-2">
              {roomUsers.map((user) => (
                <div key={user.accountId} className="flex items-center gap-3 bg-gray-50 rounded-lg px-3 py-2.5">
                  <div 
                    className="w-10 h-10 rounded-full overflow-hidden flex-shrink-0 cursor-pointer" 
                    onClick={() => onOpenProfile({ name: user.name, image: user.image, accountId: user.accountId })}
                  >
                    <img 
                      src={user.image || "/default-avatar.png"} 
                      alt={user.name} 
                      className="w-full h-full object-cover" 
                      onError={(e) => { (e.target as HTMLImageElement).src = "/default-avatar.png" }} 
                    />
                  </div>
                  <div className="flex-1 min-w-0">
                    <h4 className="text-sm font-semibold text-gray-800 truncate">{user.name}</h4>
                    <div className="flex items-center gap-1">
                      <p className="text-xs text-gray-400">ID: {user.accountId}</p>
                      <button 
                        onClick={(e) => onCopyUserId(user.accountId, e)} 
                        className="p-0.5 hover:bg-gray-200 rounded transition-colors cursor-pointer" 
                        title="Copy ID"
                      >
                        <svg viewBox="0 0 24 24" className="w-3.5 h-3.5 fill-none stroke-gray-400 stroke-[2] stroke-linecap-round stroke-linejoin-round">
                          <rect x="9" y="9" width="13" height="13" rx="2" ry="2" />
                          <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" />
                        </svg>
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="flex items-center justify-center h-full">
              <p className="text-gray-400 text-sm">No active users</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
