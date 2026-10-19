'use client'

import React, { useState } from 'react'

// ============ TYPES ============
interface FollowListProps {
  onBack: () => void
  type: 'followers' | 'following' | 'visitors' | 'friends'
  onNavigate?: (page: "home" | "message" | "me") => void;
  activePage?: "home" | "message" | "me";
}

interface VisitorsProps {
  onBack: () => void
  onNavigate?: (page: "home" | "message" | "me") => void;
  activePage?: "home" | "message" | "me";
}

// ============ CONSTANTS ============
const LABELS: { [key: string]: string } = {
  friends: 'Friends',
  followers: 'Followers',
  following: 'Following',
}

const SINGULAR: { [key: string]: string } = {
  friends: 'Friend',
  followers: 'Follower',
  following: 'Following',
}

const VISITOR_TABS = [
  { id: 'visitors', label: 'Visitors' },
  { id: 'visited', label: 'Who I Have Visited' },
]

// ============ SHARED BACK ARROW ============
function BackArrow({ onBack }: { onBack: () => void }) {
  return (
    <button
      onClick={onBack}
      className="absolute left-2 top-0 h-full flex items-center justify-center active:opacity-70 transition-opacity"
      aria-label="Back"
    >
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
        <path
          d="M19 12H5"
          stroke="#1E1E1E"
          strokeWidth="2.5"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
        <path
          d="M12 19L5 12L12 5"
          stroke="#1E1E1E"
          strokeWidth="2.5"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>
    </button>
  )
}

// ============ FOLLOW LIST (no tabs) ============
export function FollowList({ onBack, type }: FollowListProps) {
  const heading = LABELS[type] || 'Friends'
  const singularLabel = SINGULAR[type] || 'User'

  const count = type === 'followers' ? 10 : type === 'following' ? 7 : 12
  const users = Array.from({ length: count }, (_, i) => i + 1)

  return (
    <div
      className="h-screen flex flex-col select-none overflow-hidden"
      style={{
        background:
          'linear-gradient(to bottom, #3b82f6 0%, #dbeafe 15%, #f3f4f6 29%, #f3f4f6 100%)',
        paddingTop: 'calc(env(safe-area-inset-top, 0px) + 8px)',
        paddingBottom: 'env(safe-area-inset-bottom, 0px)',
        touchAction: 'manipulation',
        WebkitUserSelect: 'none',
      }}
    >
      {/* Header — Back arrow left, Heading middle */}
      <div className="relative flex items-center justify-center w-full h-[58px] shrink-0 pl-2 pr-3">
        <BackArrow onBack={onBack} />
        <h1 className="text-[24px] font-bold text-[#1E1E1E]">{heading}</h1>
      </div>

      {/* User List — SIRF YAHI SCROLL HOGA */}
      <div className="flex-1 overflow-y-auto px-3 pb-4">
        {users.map((item) => (
          <div key={item} className="flex items-center gap-2 py-2">
            <div className="w-10 h-10 rounded-full bg-gray-300 flex-shrink-0" />
            <div className="flex-1 min-w-0">
              <div className="font-semibold text-gray-900 text-sm truncate">
                {singularLabel} {item}
              </div>
              <div className="text-xs text-gray-500 truncate">
                @{type}
                {item}
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

// ============ VISITORS (with tabs) ============
export function VisitorsPage({ onBack }: VisitorsProps) {
  const [activeTab, setActiveTab] = useState<'visitors' | 'visited'>('visitors')

  return (
    <div
      className="h-screen flex flex-col select-none overflow-hidden"
      style={{
        background:
          'linear-gradient(to bottom, #3b82f6 0%, #dbeafe 15%, #f3f4f6 29%, #f3f4f6 100%)',
        paddingTop: 'calc(env(safe-area-inset-top, 0px) + 8px)',
        paddingBottom: 'env(safe-area-inset-bottom, 0px)',
        touchAction: 'manipulation',
        WebkitUserSelect: 'none',
      }}
    >
      {/* Header — Back arrow left, Heading middle */}
      <div className="relative flex items-center justify-center w-full h-[58px] shrink-0 pl-2 pr-3">
        <BackArrow onBack={onBack} />
        <h1 className="text-[24px] font-bold text-[#1E1E1E]">Visitors</h1>
      </div>

      {/* Tabs Bar — only on Visitors page */}
      <div className="px-3 mb-3 shrink-0 bg-transparent">
        <div className="flex items-center bg-transparent">
          {VISITOR_TABS.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id as 'visitors' | 'visited')}
              className="flex-1 flex flex-col items-center justify-center h-[42px] bg-transparent"
            >
              <span
                className={`text-[12px] font-semibold transition-all ${
                  activeTab === tab.id ? 'text-black' : 'text-gray-600'
                }`}
              >
                {tab.label}
              </span>
              <span
                className={`mt-1 h-[2px] rounded-full transition-all ${
                  activeTab === tab.id ? 'w-6 bg-black' : 'w-0 bg-transparent'
                }`}
              />
            </button>
          ))}
        </div>
      </div>

      {/* Content — SIRF YAHI SCROLL HOGA (empty) */}
      <div className="flex-1 overflow-y-auto px-3 pb-4" />
    </div>
  )
}

// ============ DEFAULT EXPORT ============
export default FollowList
