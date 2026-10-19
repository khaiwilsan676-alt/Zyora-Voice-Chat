'use client';

import { useState, useEffect, useRef } from 'react';
import { socket } from '../src/lib/socket';
import Image from 'next/image';

import ChatScreen from './ChatScreen';

// ============ Simple IndexedDB Functions ============
const STORE_NAME = 'conversations';

const getConversationsDBName = (userId: string) => `MessagesDB_${userId || 'guest'}`;
const getChatMessagesDBName = (userId: string) => `ChatMessagesDB_${userId || 'guest'}`;

const openDB = (userId: string): Promise<IDBDatabase> => {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(getConversationsDBName(userId), 2);
    request.onerror = () => reject(request.error);
    request.onsuccess = () => resolve(request.result);
    request.onupgradeneeded = () => {
      const db = request.result;
      if (!db.objectStoreNames.contains(STORE_NAME)) {
        db.createObjectStore(STORE_NAME, { keyPath: 'chatId' });
      }
    };
  });
};

const saveToDB = async (userId: string, conversations: ChatPreview[]) => {
  if (!userId || userId === 'N/A') return;
  try {
    const db = await openDB(userId);
    const transaction = db.transaction([STORE_NAME], 'readwrite');
    const store = transaction.objectStore(STORE_NAME);
    store.clear();
    conversations.forEach(chat => {
      store.put(chat);
    });
    db.close();
  } catch (error) {
    console.error('Save error:', error);
  }
};

const loadFromDB = async (userId: string): Promise<ChatPreview[]> => {
  if (!userId || userId === 'N/A') return [];
  try {
    const db = await openDB(userId);
    const transaction = db.transaction([STORE_NAME], 'readonly');
    const store = transaction.objectStore(STORE_NAME);
    const chats = await new Promise<ChatPreview[]>((resolve, reject) => {
      const request = store.getAll();
      request.onsuccess = () => resolve(request.result);
      request.onerror = () => reject(request.error);
    });
    db.close();
    return chats;
  } catch (error) {
    console.error('Load error:', error);
    return [];
  }
};

const loadAllChatMessagesDB = async (userId: string): Promise<any[]> => {
  if (!userId || userId === 'N/A') return [];
  try {
    const db = await new Promise<IDBDatabase>((resolve, reject) => {
      const request = indexedDB.open(getChatMessagesDBName(userId), 1);
      request.onerror = () => reject(request.error);
      request.onsuccess = () => resolve(request.result);
    });
    if (!db.objectStoreNames.contains('messages')) {
      db.close();
      return [];
    }
    const transaction = db.transaction(['messages'], 'readonly');
    const store = transaction.objectStore('messages');
    const messages = await new Promise<any[]>((resolve, reject) => {
      const request = store.getAll();
      request.onsuccess = () => resolve(request.result || []);
      request.onerror = () => reject(request.error);
    });
    db.close();
    return messages;
  } catch (error) {
    console.error('Load ChatMessagesDB error:', error);
    return [];
  }
};

// ============ Types ============
interface ChatPreview {
  chatId: string;
  otherUser: {
    uid: string;
    name: string;
    photo: string;
  };
  lastMessage: string;
  lastTimestamp: number;
  unreadCount: number;
}

interface FixedChat {
  id: string;
  name: string;
  image: string;
  uid: string;
  isFixed: boolean;
}

interface MessagePageProps {
  onNavigate?: (page: "home" | "message" | "me") => void;
  onChatOpen?: (open: boolean) => void;
  onJoinRoom?: (roomId: string) => void;
  sharedRoomData?: {
    roomId: string;
    roomName:    string;
    roomDp: string;
  } | null;
}

const FIXED_CHAT_UIDS = ['hurry_team_official', 'hurry_system_official'];

export default function MessagePage({ onChatOpen, onJoinRoom, sharedRoomData, onNavigate }: MessagePageProps) {
  const [fixedChats] = useState<FixedChat[]>([
    { id: 'hawa-team', name: 'Zyora Team', image: '/logo.png', uid: 'hurry_team_official', isFixed: true },
    { id: 'hawa-system', name: 'Zyora System', image: '/file_00000000a66881f8aa9e15d2fe2b9a0c.png', uid: 'hurry_system_official', isFixed: true }
  ]);

  const [dynamicChats, setDynamicChats] = useState<ChatPreview[]>([]);
  const [activeChat, setActiveChat] = useState<{ uid: string; name: string; photo: string } | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [officialPreviews, setOfficialPreviews] = useState<Record<string, { lastMessage: string; lastTimestamp: number; unreadCount: number }>>({});

  const getCurrentUserData = () => {
    const uid = typeof window !== 'undefined' ? localStorage.getItem('userUID') || localStorage.getItem('userPhone') || 'N/A' : 'N/A';
    const name = typeof window !== 'undefined' ? localStorage.getItem('userName') || 'Me' : 'Me';
    const photo = typeof window !== 'undefined' ? localStorage.getItem('userPhoto') || '' : '';
    return { uid, name, photo };
  };

  const currentUserUid = getCurrentUserData().uid;

  useEffect(() => {
    let isMounted = true;

    const loadData = async () => {
      if (currentUserUid === 'N/A') {
        if (isMounted) setIsLoading(false);
        return;
      }

      const cachedChats = await loadFromDB(currentUserUid);
      const allMessages = await loadAllChatMessagesDB(currentUserUid);
      const chatMap = new Map<string, ChatPreview>();

      cachedChats.forEach((chat) => {
        if (chat && chat.chatId) {
          chatMap.set(chat.chatId, chat);
        }
      });

      allMessages.forEach((msg) => {
        if (!msg || !msg.chatId) return;

        const existing = chatMap.get(msg.chatId);
        const msgTime = Number(msg.timestamp || Date.now());
        const isMe = msg.senderId === currentUserUid || msg.sender === 'me';

        const otherUid = isMe
          ? (msg.receiverId || msg.targetUid)
          : (msg.senderId || msg.otherUid);

        const otherName = isMe
          ? (msg.targetUserName || msg.receiverName || msg.otherUserName || 'User')
          : (msg.senderName || msg.otherUserName || 'User');

        const otherPhoto = isMe
          ? (msg.targetUserPhoto || msg.receiverPhoto || msg.otherUserPhoto || '/default-avatar.png')
          : (msg.senderPhoto || msg.otherUserPhoto || '/default-avatar.png');

        if (!existing) {
          if (otherUid) {
            chatMap.set(msg.chatId, {
              chatId: msg.chatId,
              otherUser: {
                uid: otherUid,
                name: otherName,
                photo: otherPhoto,
              },
              lastMessage: msg.type === 'image' ? '📷 Image' : (msg.text || ''),
              lastTimestamp: msgTime,
              unreadCount: 0,
            });
          }
        } else if (msgTime > (existing.lastTimestamp || 0)) {
          existing.lastMessage = msg.type === 'image' ? '📷 Image' : (msg.text || existing.lastMessage);
          existing.lastTimestamp = msgTime;
          if (otherName && otherName !== 'User') existing.otherUser.name = otherName;
          if (otherPhoto && otherPhoto !== '/default-avatar.png') existing.otherUser.photo = otherPhoto;
        }
      });

      const nonFixedChats = Array.from(chatMap.values())
        .filter((chat) => !FIXED_CHAT_UIDS.includes(chat.otherUser.uid))
        .sort((a, b) => (b.lastTimestamp || 0) - (a.lastTimestamp || 0));

      const officialMap: Record<string, { lastMessage: string; lastTimestamp: number; unreadCount: number }> = {};
      fixedChats.forEach((fc) => {
        const cId = [currentUserUid, fc.uid].sort().join('_');
        const existing = chatMap.get(cId);
        let unread = 0;
        allMessages.forEach((msg) => {
          if (msg && msg.chatId === cId && msg.receiverId === currentUserUid && msg.isUnread) {
            unread++;
          }
        });
        if (existing) {
          officialMap[fc.uid] = {
            lastMessage: existing.lastMessage,
            lastTimestamp: existing.lastTimestamp,
            unreadCount: unread,
          };
        }
      });

      if (isMounted) {
        setDynamicChats(nonFixedChats);
        setOfficialPreviews(officialMap);
        await saveToDB(currentUserUid, nonFixedChats);
        setIsLoading(false);
      }
    };

    const handlePrivateMessage = async (data: any) => {
      if (!data?.senderId || !data?.receiverId) return;

      if (
        String(data.receiverId) !== String(currentUserUid) &&
        String(data.senderId) !== String(currentUserUid)
      ) return;

      const isMe = String(data.senderId) === String(currentUserUid);
      const otherUid = isMe ? String(data.receiverId) : String(data.senderId);
      const chatId = [currentUserUid, otherUid].sort().join('_');
      const timestamp = Number(data.timestamp || Date.now());
      const lastMessage = data.type === 'image' ? '📷 Image' : String(data.text || '');

      const localMessage = {
        id: String(data.id || `${data.senderId}_${timestamp}`),
        chatId,
        text: String(data.text || ''),
        sender: isMe ? 'me' : 'other',
        senderId: String(data.senderId),
        receiverId: String(data.receiverId),
        senderName: data.senderName || data.otherUserName || 'User',
        senderPhoto: data.senderPhoto || data.otherUserPhoto || '/default-avatar.png',
        receiverName: data.receiverName || 'User',
        receiverPhoto: data.receiverPhoto || '/default-avatar.png',
        targetUserName: data.targetUserName || (isMe ? data.receiverName : data.senderName) || 'User',
        targetUserPhoto: data.targetUserPhoto || (isMe ? data.receiverPhoto : data.senderPhoto) || '/default-avatar.png',
        timestamp,
        type: data.type || 'message',
        imageUrl: data.imageUrl || undefined,
        roomData: data.roomData || undefined,
        replyTo: data.replyTo || null,
      };

      try {
        const db = await new Promise<IDBDatabase>((resolve, reject) => {
          const request = indexedDB.open(getChatMessagesDBName(currentUserUid), 1);
          request.onerror = () => reject(request.error);
          request.onsuccess = () => resolve(request.result);
          request.onupgradeneeded = () => {
            const db = request.result;
            if (!db.objectStoreNames.contains('messages')) {
              const store = db.createObjectStore('messages', { keyPath: 'id' });
              store.createIndex('chatId', 'chatId', { unique: false });
              store.createIndex('timestamp', 'timestamp', { unique: false });
            }
          };
        });
        const tx = db.transaction(['messages'], 'readwrite');
        tx.objectStore('messages').put(localMessage);
        tx.oncomplete = () => db.close();
        tx.onerror = () => db.close();
      } catch (error) {
        console.error('Realtime local message save error:', error);
      }

      if (!isMounted) return;

      if (FIXED_CHAT_UIDS.includes(otherUid)) {
        setOfficialPreviews((prev) => ({
          ...prev,
          [otherUid]: {
            lastMessage,
            lastTimestamp: timestamp,
            unreadCount: isMe ? 0 : ((prev[otherUid]?.unreadCount || 0) + 1),
          },
        }));
        return;
      }

      setDynamicChats((prev) => {
        const existing = prev.find((chat) => chat.chatId === chatId);

        const otherUserNameFallback = isMe ? (data.receiverName || data.targetUserName) : (data.senderName || data.otherUserName);
        const otherUserPhotoFallback = isMe ? (data.receiverPhoto || data.targetUserPhoto) : (data.senderPhoto || data.otherUserPhoto);

        const updatedChat: ChatPreview = existing
          ? {
              ...existing,
              lastMessage,
              lastTimestamp: timestamp,
              unreadCount: isMe ? existing.unreadCount : (existing.unreadCount || 0) + 1,
            }
          : {
              chatId,
              otherUser: {
                uid: otherUid,
                name: otherUserNameFallback || 'User',
                photo: otherUserPhotoFallback || '/default-avatar.png',
              },
              lastMessage,
              lastTimestamp: timestamp,
              unreadCount: isMe ? 0 : 1,
            };

        const next = [
          ...prev.filter((chat) => chat.chatId !== chatId && !FIXED_CHAT_UIDS.includes(chat.otherUser.uid)),
          updatedChat,
        ].sort((a, b) => (b.lastTimestamp || 0) - (a.lastTimestamp || 0));

        saveToDB(currentUserUid, next).catch(() => {});
        return next;
      });
    };

    const handlePrivateMessageCleared = (data: any) => {
      const clearedChatId = String(data?.chatId || '');
      if (!clearedChatId || !isMounted) return;

      setDynamicChats((prev) => {
        const next = prev.map((chat) =>
          chat.chatId === clearedChatId
            ? { ...chat, lastMessage: '', lastTimestamp: 0, unreadCount: 0 }
            : chat
        );
        saveToDB(currentUserUid, next).catch(() => {});
        return next;
      });
    };

    const handleOfficialBroadcastMessage = async (data: any) => {
      if (!data?.senderId) return;
      const senderId = String(data.senderId);
      if (senderId !== 'hurry_team_official' && senderId !== 'hurry_system_official') return;

      const chatId = [currentUserUid, senderId].sort().join('_');
      const timestamp = Number(data.timestamp || Date.now());
      const lastMessage = data.type === 'image' ? '📷 Image' : String(data.text || '');

      const localMessage = {
        id: String(data.id || `official_${timestamp}`),
        chatId,
        text: String(data.text || ''),
        sender: 'other',
        senderId,
        receiverId: currentUserUid,
        senderName: data.senderName || (senderId === 'hurry_team_official' ? 'Zyora Team' : 'Zyora System'),
        senderPhoto: data.senderPhoto || (senderId === 'hurry_team_official' ? '/logo.png' : '/file_00000000a66881f8aa9e15d2fe2b9a0c.png'),
        timestamp,
        type: data.type || 'message',
        imageUrl: data.imageUrl || undefined,
      };

      try {
        const db = await new Promise<IDBDatabase>((resolve, reject) => {
          const request = indexedDB.open(getChatMessagesDBName(currentUserUid), 1);
          request.onerror = () => reject(request.error);
          request.onsuccess = () => resolve(request.result);
          request.onupgradeneeded = () => {
            const db = request.result;
            if (!db.objectStoreNames.contains('messages')) {
              const store = db.createObjectStore('messages', { keyPath: 'id' });
              store.createIndex('chatId', 'chatId', { unique: false });
              store.createIndex('timestamp', 'timestamp', { unique: false });
            }
          };
        });
        const tx = db.transaction(['messages'], 'readwrite');
        tx.objectStore('messages').put(localMessage);
        tx.oncomplete = () => db.close();
        tx.onerror = () => db.close();
      } catch (error) {
        console.error('Realtime official message save error:', error);
      }

      if (!isMounted) return;

      setOfficialPreviews((prev) => ({
        ...prev,
        [senderId]: {
          lastMessage,
          lastTimestamp: timestamp,
          unreadCount: (prev[senderId]?.unreadCount || 0) + 1,
        },
      }));
    };

    const handleOfficialHistoryResponse = async (data: any) => {
      if (!Array.isArray(data?.messages) || !currentUserUid || currentUserUid === 'N/A') return;

      const previews: Record<string, { lastMessage: string; lastTimestamp: number; unreadCount: number }> = {};

      try {
        const db = await new Promise<IDBDatabase>((resolve, reject) => {
          const request = indexedDB.open(getChatMessagesDBName(currentUserUid), 1);
          request.onerror = () => reject(request.error);
          request.onsuccess = () => resolve(request.result);
          request.onupgradeneeded = () => {
            const database = request.result;
            if (!database.objectStoreNames.contains('messages')) {
              const store = database.createObjectStore('messages', { keyPath: 'id' });
              store.createIndex('chatId', 'chatId', { unique: false });
              store.createIndex('timestamp', 'timestamp', { unique: false });
            }
          };
        });

        const tx = db.transaction(['messages'], 'readwrite');
        const store = tx.objectStore('messages');

        data.messages.forEach((msg: any) => {
          if (!msg?.senderId) return;
          const senderId = String(msg.senderId);
          if (senderId !== 'hurry_team_official' && senderId !== 'hurry_system_official') return;

          const chatId = [currentUserUid, senderId].sort().join('_');
          const timestamp = Number(msg.timestamp || Date.now());
          const lastMessage = msg.type === 'image' ? '📷 Image' : String(msg.text || '');

          store.put({
            id: String(msg.id || `official_${timestamp}`),
            chatId,
            text: String(msg.text || ''),
            sender: 'other',
            senderId,
            receiverId: currentUserUid,
            senderName: msg.senderName || (senderId === 'hurry_team_official' ? 'Zyora Team' : 'Zyora System'),
            senderPhoto: msg.senderPhoto || (senderId === 'hurry_team_official' ? '/logo.png' : '/file_00000000a66881f8aa9e15d2fe2b9a0c.png'),
            timestamp,
            type: msg.type || 'message',
            imageUrl: msg.imageUrl || undefined,
          });

          if (!previews[senderId] || timestamp > previews[senderId].lastTimestamp) {
            previews[senderId] = {
              lastMessage,
              lastTimestamp: timestamp,
              unreadCount: previews[senderId]?.unreadCount || 0,
            };
          }
        });

        tx.oncomplete = () => db.close();
        tx.onerror = () => db.close();
      } catch (error) {
        console.error('Official history save error:', error);
      }

      if (isMounted && Object.keys(previews).length > 0) {
        setOfficialPreviews((prev) => ({ ...prev, ...previews }));
      }
    };

    const registerUser = () => {
      const accNum = typeof window !== 'undefined' ? localStorage.getItem('accountNumber') || '' : '';
      socket.emit('register', { userId: currentUserUid, accountId: accNum });
      socket.emit('official_message_history_request');
    };

    socket.on('connect', registerUser);
    socket.on('private_message', handlePrivateMessage);
    socket.on('private_message_cleared', handlePrivateMessageCleared);
    socket.on('official_broadcast_message', handleOfficialBroadcastMessage);
    socket.on('official_message_history_response', handleOfficialHistoryResponse);

    if (!socket.connected) {
      socket.connect();
    } else {
      registerUser();
    }

    loadData();

    return () => {
      isMounted = false;
      socket.off('connect', registerUser);
      socket.off('private_message', handlePrivateMessage);
      socket.off('private_message_cleared', handlePrivateMessageCleared);
      socket.off('official_broadcast_message', handleOfficialBroadcastMessage);
      socket.off('official_message_history_response', handleOfficialHistoryResponse);
    };
  }, [currentUserUid]);

  // ---------- Helpers ----------
  const formatTime = (timestamp: number) => {
    if (!timestamp) return '';
    const date = new Date(timestamp);
    const now = new Date();
    const diff = now.getTime() - date.getTime();
    const days = Math.floor(diff / (1000 * 60 * 60 * 24));
    if (days === 0) return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    if (days === 1) return 'Yesterday';
    return date.toLocaleDateString();
  };

  const handleOpenFixedChat = (chat: FixedChat) => {
    setActiveChat({ uid: chat.uid, name: chat.name, photo: chat.image });
  };

  const handleOpenDynamicChat = (chat: ChatPreview) => {
    setDynamicChats((prev) => {
      const next = prev.map((item) =>
        item.chatId === chat.chatId ? { ...item, unreadCount: 0 } : item
      );
      saveToDB(currentUserUid, next).catch(() => {});
      return next;
    });

    loadAllChatMessagesDB(currentUserUid).then(async (messages) => {
      try {
        const db = await new Promise<IDBDatabase>((resolve, reject) => {
          const request = indexedDB.open(getChatMessagesDBName(currentUserUid), 1);
          request.onerror = () => reject(request.error);
          request.onsuccess = () => resolve(request.result);
        });
        if (db.objectStoreNames.contains('messages')) {
          const tx = db.transaction(['messages'], 'readwrite');
          const store = tx.objectStore('messages');
          messages.forEach((m) => {
            if (m.chatId === chat.chatId && m.isUnread) {
              store.put({ ...m, isUnread: false });
            }
          });
          tx.oncomplete = () => {
            db.close();
            if (typeof window !== 'undefined') {
              window.dispatchEvent(new CustomEvent('unread_count_updated'));
            }
          };
          tx.onerror = () => db.close();
        } else {
          db.close();
        }
      } catch (err) {
        console.error('Error clearing dynamic unread messages:', err);
      }
    });

    setActiveChat({
      uid: chat.otherUser.uid,
      name: chat.otherUser.name,
      photo: chat.otherUser.photo,
    });
  };

  const handleCloseChat = () => {
    setActiveChat(null);
  };

  useEffect(() => {
    if (onChatOpen) onChatOpen(!!activeChat);
  }, [activeChat, onChatOpen]);

  return (
    <div className="w-full min-h-screen bg-white relative overflow-hidden">
      {/* ============ TOP SHEET — Blue band + Message title ============ */}
      <div
        className="w-full sticky top-0 z-30"
        style={{
          background: 'linear-gradient(to bottom, #3b82f6 0%, #dbeafe 65%, #ffffff 100%)',
          paddingTop: 'calc(env(safe-area-inset-top, 0px) + 11px)',
          WebkitTapHighlightColor: 'transparent',
        }}
      >
        <div className="px-4 pt-4 pb-3 flex items-center gap-3">
          <div
            className="flex flex-col items-center cursor-pointer select-none outline-none"
            style={{ WebkitTapHighlightColor: 'transparent', textDecoration: 'none' }}
          >
            <h1
              className="text-2xl text-black font-extrabold outline-none"
              style={{ textDecoration: 'none' }}
            >
              Message
            </h1>
          </div>
        </div>
      </div>

      {/* ============ MAIN CONTENT (Message only) ============ */}
      <div className="pt-1 pb-24 flex flex-col gap-0.6">
        {/* Fixed chats */}
        {fixedChats.map((chat) => {
          const preview = officialPreviews[chat.uid];
          return (
            <div
              key={chat.id}
              onClick={() => {
                setOfficialPreviews((prev) => ({
                  ...prev,
                  [chat.uid]: prev[chat.uid]
                    ? { ...prev[chat.uid], unreadCount: 0 }
                    : { lastMessage: '', lastTimestamp: 0, unreadCount: 0 },
                }));
                handleOpenFixedChat(chat);
              }}
              className="flex items-center gap-2 px-3 py-2.5 cursor-pointer active:opacity-60 transition-opacity"
            >
              <div className="w-14 h-14 rounded-full flex items-center justify-center flex-shrink-0 overflow-hidden">
                <Image src={chat.image} alt={chat.name} width={56} height={56} className="object-cover" />
              </div>
              <div className="flex-1 min-w-0">
                <h3 className="font-semibold text-gray-800 text-base">{chat.name}</h3>
                {preview?.lastMessage && (
                  <p className="text-sm text-gray-500 truncate">{preview.lastMessage}</p>
                )}
              </div>
              <div className="flex flex-col items-end gap-1">
                {preview?.lastTimestamp ? (
                  <span className="text-xs text-gray-400">{formatTime(preview.lastTimestamp)}</span>
                ) : null}
                {preview?.unreadCount && preview.unreadCount > 0 ? (
                  <span className="bg-red-500 text-white text-xs font-bold rounded-full min-w-[22px] h-[22px] flex items-center justify-center px-1.5">
                    {preview.unreadCount > 99 ? '99+' : preview.unreadCount}
                  </span>
                ) : null}
              </div>
            </div>
          );
        })}

        {/* Dynamic chats */}
        {dynamicChats.map((chat) => (
          <div
            key={chat.chatId}
            onClick={() => handleOpenDynamicChat(chat)}
            className="flex items-center gap-2 px-3 py-2.5 cursor-pointer active:opacity-60 transition-opacity"
          >
            <div className="w-14 h-14 rounded-full flex items-center justify-center flex-shrink-0 overflow-hidden">
              <Image
                src={chat.otherUser.photo || '/default-avatar.png'}
                alt={chat.otherUser.name}
                width={56}
                height={56}
                className="object-cover"
              />
            </div>
            <div className="flex-1 min-w-0">
              <h3 className="font-semibold text-gray-800 text-base">{chat.otherUser.name}</h3>
              <p className="text-sm text-gray-500 truncate">{chat.lastMessage}</p>
            </div>
            <div className="flex flex-col items-end gap-1">
              <span className="text-xs text-gray-400">{formatTime(chat.lastTimestamp)}</span>
              {chat.unreadCount > 0 && (
                <span className="bg-red-500 text-white text-xs font-bold rounded-full min-w-[22px] h-[22px] flex items-center justify-center px-1.5">
                  {chat.unreadCount > 99 ? '99+' : chat.unreadCount}
                </span>
              )}
            </div>
          </div>
        ))}

        {/* Empty state */}
        {!isLoading && dynamicChats.length === 0 && fixedChats.length === 0 && (
          <div className="text-center py-12">
            <p className="text-gray-400 text-sm"></p>
          </div>
        )}
      </div>

      {/* Chat Screen Overlay */}
      {activeChat && (
        <ChatScreen
          currentUser={getCurrentUserData()}
          targetUser={activeChat}
          onClose={handleCloseChat}
          onJoinRoom={onJoinRoom}
          sharedRoomData={sharedRoomData}
        />
      )}
    </div>
  );
}
