'use client'

// ============ MINE PAGE COMPONENT ============
// Extracted from HomePage's renderMineTab — UI same rahega

export interface UserCard {
  id: string
  accountId?: string
  name: string
  country: string
  image: string
  isLocked?: boolean
  roomPassword?: string
}

interface KeptRoomData {
  name: string
  country?: string
  image: string
  accountId: string
  isLocked?: boolean
  roomPassword?: string
}

interface MinePageProps {
  // Translations
  t: any

  // Room creation card
  isRoomCreated: boolean
  myRoom: UserCard | null
  userPhoto: string
  userName: string
  onCardClick: () => void

  // Tabs
  activeMineTab: 'following' | 'recent'
  setActiveMineTab: (tab: 'following' | 'recent') => void

  // Rooms data
  followingRooms: KeptRoomData[]
  recentRooms: any[]

  // Actions
  onUserCardClick: (user: UserCard) => void
}

export default function MinePage({
  t,
  isRoomCreated,
  myRoom,
  userPhoto,
  userName,
  onCardClick,
  activeMineTab,
  setActiveMineTab,
  followingRooms,
  recentRooms,
  onUserCardClick,
}: MinePageProps) {
  return (
    <div className="px-3 -mt-2">
      <div
        onClick={onCardClick}
        className="rounded-md p-6 flex items-center gap-4 cursor-pointer hover:shadow-lg transition-all mb-6"
        style={{
          background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
          boxShadow: '0 8px 32px rgba(102, 126, 234, 0.4)',
        }}
      >
        {!isRoomCreated ? (
          <>
            <div className="w-14 h-14 rounded-full bg-white/20 flex items-center justify-center flex-shrink-0">
              <svg width="32" height="32" viewBox="0 0 32 32" fill="none">
                <path
                  d="M16 8V24M8 16H24"
                  stroke="white"
                  strokeWidth="3"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
              </svg>
            </div>
            <div className="flex flex-col">
              <h3 className="text-white font-bold text-xl leading-tight">
                {t.createRoomTitle || 'Embark Your Hurry Journey!'}
              </h3>
              <p className="text-white/80 text-sm mt-1 font-medium">
                {t.createRoomSubtitle || 'Tap to create your room'}
              </p>
            </div>
          </>
        ) : (
          <>
            <div className="w-14 h-14 rounded-full bg-white/20 flex items-center justify-center flex-shrink-0 overflow-hidden border-2 border-white/50">
              <img
                src={
                  myRoom?.image && myRoom.image !== 'undefined' && myRoom.image !== 'null'
                    ? myRoom.image
                    : (userPhoto || '/default-avatar.png')
                }
                alt={myRoom?.name || 'Room Avatar'}
                className="w-full h-full object-cover"
                onError={(e) => {
                  (e.target as HTMLImageElement).src = userPhoto || '/default-avatar.png';
                }}
              />
            </div>
            <div className="flex flex-col">
              <h3 className="text-white font-bold text-xl leading-tight">
                {myRoom?.name && myRoom.name !== 'My Room' && myRoom.name !== 'My room'
                  ? myRoom.name
                  : ("Room")}
              </h3>
              <p className="text-white/80 text-sm mt-1 font-medium">
                {t.enterRoomSubtitle || 'Tap to enter your room'}
              </p>
            </div>
          </>
        )}
      </div>

      <div className="flex gap-3 -mt-5">
        <button
          type="button"
          onClick={() => setActiveMineTab('following')}
          className={`relative pb-1.5 text-[14px] font-medium transition-colors ${
            activeMineTab === 'following'
              ? 'text-gray-900'
              : 'text-gray-400 hover:text-gray-600'
          }`}
        >
          {t.following}
          {activeMineTab === 'following' && (
            <span className="absolute left-0 right-0 -bottom-0 h-0.5 bg-gray-900 rounded-full" />
          )}
        </button>
        <button
          type="button"
          onClick={() => setActiveMineTab('recent')}
          className={`relative pb-1.5 text-[14px] font-medium transition-colors ${
            activeMineTab === 'recent'
              ? 'text-gray-900'
              : 'text-gray-400 hover:text-gray-600'
          }`}
        >
          {t.recent}
          {activeMineTab === 'recent' && (
            <span className="absolute left-0 right-0 -bottom-0 h-0.5 bg-gray-900 rounded-full" />
          )}
        </button>
      </div>

      {activeMineTab === 'following' && (
        followingRooms.length > 0 ? (
          <div className="grid grid-cols-2 gap-2.5">
            {followingRooms.map(room => {
              const user: UserCard = {
                id: room.accountId,
                accountId: room.accountId,
                name: room.name,
                country: room.country || '🇮🇳',
                image: room.image,
                isLocked: room.isLocked
              }
              return (
                <div
                  key={room.accountId}
                  onClick={() => onUserCardClick(user)}
                  className="relative bg-gray-200 rounded-2xl overflow-hidden cursor-pointer hover:shadow-lg transition-all hover:scale-[1.02] active:scale-95"
                  style={{ height: '180px' }}
                >
                  <img
                    src={room.image}
                    alt={room.name}
                    className="w-full h-full object-cover"
                    style={{ objectFit: 'cover', width: '100%', height: '100%' }}
                  />
                  {room.isLocked && (
                    <div className="absolute top-2 right-2 bg-white/20 backdrop-blur-md rounded-full p-1.5 border border-white/50">
                      <svg viewBox="0 0 24 24" className="w-4 h-4 fill-white">
                        <path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zM9 6c0-1.66 1.34-3 3-3s3 1.34 3 3v2H9V6zm9 14H6V10h12v10zm-6-3c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2z"/>
                      </svg>
                    </div>
                  )}
                  <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent flex flex-col justify-end p-2.5">
                    <div className="flex items-center gap-1.5">
                      <span className="text-base">🇮🇳</span>
                      <div className="flex-1 min-w-0">
                        <div className="text-white font-semibold text-xs truncate">
                          {room.name}
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              )
            })}
          </div>
        ) : (
          <div className="flex flex-col items-center justify-center py-12">
            <img
              src="/file_0000000047308211a02722299d1fda2e.png"
              alt="No data"
              className="w-40 h-auto object-contain mb-3"
              draggable="false"
            />
            <p className="text-sm text-gray-400 font-medium">No data</p>
          </div>
        )
      )}

      {activeMineTab === 'recent' && (
        recentRooms.length > 0 ? (
          <div className="grid grid-cols-2 gap-2.5">
            {recentRooms.map(room => {
              const user: UserCard = {
                id: room.accountId,
                accountId: room.accountId,
                name: room.name,
                country: room.country || '🇮🇳',
                image: room.image,
                isLocked: room.isLocked
              }
              return (
                <div
                  key={room.accountId}
                  onClick={() => onUserCardClick(user)}
                  className="relative bg-gray-200 rounded-2xl overflow-hidden cursor-pointer hover:shadow-lg transition-all hover:scale-[1.02] active:scale-95"
                  style={{ height: '180px' }}
                >
                  <img
                    src={room.image}
                    alt={room.name}
                    className="w-full h-full object-cover"
                    style={{ objectFit: 'cover', width: '100%', height: '100%' }}
                  />
                  {room.isLocked && (
                    <div className="absolute top-2 right-2 bg-white/20 backdrop-blur-md rounded-full p-1.5 border border-white/50">
                      <svg viewBox="0 0 24 24" className="w-4 h-4 fill-white">
                        <path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zM9 6c0-1.66 1.34-3 3-3s3 1.34 3 3v2H9V6zm9 14H6V10h12v10zm-6-3c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2z"/>
                      </svg>
                    </div>
                  )}
                  <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent flex flex-col justify-end p-2.5">
                    <div className="flex items-center gap-1.5">
                      <span className="text-base">🇮🇳</span>
                      <div className="flex-1 min-w-0">
                        <div className="text-white font-semibold text-xs truncate">
                          {room.name}
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              )
            })}
          </div>
        ) : (
          <div className="flex flex-col items-center justify-center py-12">
            <img
              src="/file_0000000047308211a02722299d1fda2e.png"
              alt="No data"
              className="w-40 h-auto object-contain mb-3"
              draggable="false"
            />
            <p className="text-sm text-gray-400 font-medium">No data</p>
          </div>
        )
      )}
    </div>
  )
            }
