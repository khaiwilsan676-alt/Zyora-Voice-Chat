'use client';

import { useState, useEffect, useRef } from 'react';
import { ArrowLeft, Send, ImageIcon, MoreHorizontal, LogIn, Trash2, Flag, Ban, X, Check, Copy } from 'lucide-react';
import { sendMessage, getMessages } from '../src/lib/googleSheets';

// ============ IndexedDB Functions for Messages ============
const MESSAGES_DB_NAME = 'ChatMessagesDB';
const MESSAGES_STORE = 'messages';

const openMessagesDB = (): Promise<IDBDatabase> => {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(MESSAGES_DB_NAME, 1);

    request.onerror = () => reject(request.error);
    request.onsuccess = () => resolve(request.result);

    request.onupgradeneeded = () => {
      const db = request.result;
      if (!db.objectStoreNames.contains(MESSAGES_STORE)) {
        const store = db.createObjectStore(MESSAGES_STORE, { keyPath: 'id' });
        store.createIndex('chatId', 'chatId', { unique: false });
        store.createIndex('timestamp', 'timestamp', { unique: false });
      }
    };
  });
};

// Messages save karo IndexedDB mein
const saveMessagesToDB = async (chatId: string, messages: Message[]) => {
  try {
    const db = await openMessagesDB();
    const transaction = db.transaction([MESSAGES_STORE], 'readwrite');
    const store = transaction.objectStore(MESSAGES_STORE);

    // Sirf is chat ke messages clear karo
    const index = store.index('chatId');
    const oldMessages = await new Promise<any[]>((resolve, reject) => {
      const request = index.getAll(chatId);
      request.onsuccess = () => resolve(request.result);
      request.onerror = () => reject(request.error);
    });

    oldMessages.forEach(msg => {
      store.delete(msg.id);
    });

    // Naye messages save karo
    messages.forEach(msg => {
      store.put({
        ...msg,
        chatId: chatId,
      });
    });

    db.close();
    console.log('Messages IndexedDB mein save ho gaye:', messages.length);
  } catch (error) {
    console.error('Messages save error:', error);
  }
};

// Messages load karo IndexedDB se
const loadMessagesFromDB = async (chatId: string): Promise<Message[]> => {
  try {
    const db = await openMessagesDB();
    const transaction = db.transaction([MESSAGES_STORE], 'readonly');
    const store = transaction.objectStore(MESSAGES_STORE);
    const index = store.index('chatId');

    const messages = await new Promise<Message[]>((resolve, reject) => {
      const request = index.getAll(chatId);
      request.onsuccess = () => resolve(request.result);
      request.onerror = () => reject(request.error);
    });

    db.close();
    return messages;
  } catch (error) {
    console.error('Messages load error:', error);
    return [];
  }
};

interface Message {
  id: string;
  text: string;
  sender: 'me' | 'other';
  timestamp: number;
  type?: 'message' | 'room_invite' | 'image';
  imageUrl?: string;
  roomData?: {
    roomId: string;
    roomName: string;
    roomImage: string;
  };
  replyTo?: {
    id: string;
    text: string;
    senderName: string;
  } | null;
}

interface ChatScreenProps {
  currentUser: { uid: string; name: string; photo: string };
  targetUser: { uid: string; name: string; photo: string };
  onClose: () => void;
  onJoinRoom?: (roomId: string) => void;
  sharedRoomData?: {
    roomId: string;
    roomName: string;
    roomImage: string;
  } | null;
}

const FIXED_CHAT_UIDS = ['hurry_team_official', 'hurry_system_official'];

export default function ChatScreen({
  currentUser,
  targetUser,
  onClose,
  onJoinRoom,
  sharedRoomData,
}: ChatScreenProps) {
  const [messages, setMessages] = useState<Message[]>([]);
  const [newMessage, setNewMessage] = useState('');
  const [connected, setConnected] = useState(false);
  const [online, setOnline] = useState(false);
  const [showOptions, setShowOptions] = useState(false);
  const [showBlockConfirm, setShowBlockConfirm] = useState(false);
  const [showReportConfirm, setShowReportConfirm] = useState(false);
  const [replyTo, setReplyTo] = useState<Message | null>(null);
  const [longPressMsg, setLongPressMsg] = useState<Message | null>(null);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [deleteMode, setDeleteMode] = useState(false);
  const [selectedMessages, setSelectedMessages] = useState<Set<string>>(new Set());
  const [showDeleteSelectedConfirm, setShowDeleteSelectedConfirm] = useState(false);
  const [copiedMessage, setCopiedMessage] = useState<string | null>(null);
  const [swipeStartX, setSwipeStartX] = useState<number | null>(null);
  const [swipeMsgId, setSwipeMsgId] = useState<string | null>(null);
  const [imageUploading, setImageUploading] = useState(false);
  const [isLoadingMessages, setIsLoadingMessages] = useState(true);
  const longPressTimerRef = useRef<NodeJS.Timeout | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const lastSentInviteRoomIdRef = useRef<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const isFixedChat = FIXED_CHAT_UIDS.includes(targetUser.uid);
  const chatId = [currentUser.uid, targetUser.uid].sort().join('_');

  // ========== Online status ==========
  useEffect(() => {
    if (isFixedChat) {
      setOnline(true);
      return;
    }

    const presenceRef = doc(db, 'presence', targetUser.uid);
    const unsubscribe = onSnapshot(presenceRef, (snap) => {
      if (snap.exists()) {
        setOnline(snap.data()?.online || false);
      } else {
        setOnline(false);
      }
    });

    return () => unsubscribe();
  }, [targetUser.uid, isFixedChat]);

  // ========== Pehle IndexedDB se messages load karo ==========
  useEffect(() => {
    const loadCachedMessages = async () => {
      try {
        const cachedMessages = await loadMessagesFromDB(chatId);
        if (cachedMessages.length > 0) {
          setMessages(cachedMessages);
          console.log('Chat messages IndexedDB se load huye:', cachedMessages.length);
        }
        setIsLoadingMessages(false);
      } catch (error) {
        console.error('Error loading cached messages:', error);
        setIsLoadingMessages(false);
      }
    };

    loadCachedMessages();
  }, [chatId]);

  // ========== Google Sheets messages polling ==========
  useEffect(() => {
    let isMounted = true;

    const fetchRemoteMessages = async () => {
      try {
        const rawMsgs = await getMessages(currentUser.uid, targetUser.uid);
        if (!isMounted || !Array.isArray(rawMsgs)) return;

        const loadedMessages: Message[] = rawMsgs
          .map((data: any) => {
            const senderId = data.senderId || data.USER_A_ID || data.userAId;
            const isMe = senderId === currentUser.uid;
            return {
              id: String(data.id || data.timestamp || data.createdAt || Date.now()),
              text: data.chat || data.text || '',
              sender: (isMe ? 'me' : 'other') as 'me' | 'other',
              timestamp: Number(data.createdAt || data.timestamp || Date.now()),
              type: data.type || 'message',
              imageUrl: data.imageUrl || undefined,
              roomData: data.roomData || undefined,
              replyTo: data.replyTo || null,
            };
          });

        loadedMessages.sort((a, b) => a.timestamp - b.timestamp);

        if (isMounted) {
          setMessages(loadedMessages);
          setConnected(true);
          await saveMessagesToDB(chatId, loadedMessages);
        }
      } catch (error) {
        console.error('Error fetching messages from Google Sheets:', error);
        setConnected(false);
      }
    };

    fetchRemoteMessages();
    const interval = setInterval(fetchRemoteMessages, 4000);

    return () => {
      isMounted = false;
      clearInterval(interval);
    };
  }, [chatId, currentUser.uid, targetUser.uid]);

  // ========== Auto-send room invite if sharedRoomData provided ==========
  useEffect(() => {
    if (sharedRoomData && lastSentInviteRoomIdRef.current !== sharedRoomData.roomId) {
      lastSentInviteRoomIdRef.current = sharedRoomData.roomId;
      sendRoomInvite(sharedRoomData);
    }
  }, [sharedRoomData]);

  // ========== Send room invite ==========
  const sendRoomInvite = async (roomData: { roomId: string; roomName: string; roomImage: string }) => {
    try {
      await sendMessage({
        senderId: currentUser.uid,
        receiverId: targetUser.uid,
        chat: `Joins our Party Room: ${roomData.roomName}`,
        dp: currentUser.photo,
        name: currentUser.name,
        type: 'room_invite',
        roomData
      });
    } catch (error) {
      console.error('Error sending room invite:', error);
    }
  };

  // ========== Send text message ==========
  const handleSend = async () => {
    if (!newMessage.trim()) return;
    const messageText = newMessage.trim();
    setNewMessage('');

    try {
      const messagesRef = collection(db, 'chats', chatId, 'messages');
      await addDoc(messagesRef, {
        text: messageText,
        senderUid: currentUser.uid,
        timestamp: serverTimestamp(),
        type: 'message',
        replyTo: replyTo
          ? {
              id: replyTo.id,
              text: replyTo.text,
              senderName: replyTo.sender === 'me' ? currentUser.name : targetUser.name,
            }
          : null,
      });

      await updateConversation(messageText);

      setReplyTo(null);
    } catch (error) {
      console.error('Error sending message:', error);
      setNewMessage(messageText);
    }
  };

  // ========== Send image message ==========
  const handleImageUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (file.size > 5 * 1024 * 1024) {
      alert('Image size should be less than 5MB');
      return;
    }

    setImageUploading(true);
    try {
      const base64 = await new Promise<string>((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve(reader.result as string);
        reader.onerror = reject;
        reader.readAsDataURL(file);
      });

      const messagesRef = collection(db, 'chats', chatId, 'messages');
      await addDoc(messagesRef, {
        text: '',
        senderUid: currentUser.uid,
        timestamp: serverTimestamp(),
        type: 'image',
        imageUrl: base64,
        replyTo: replyTo
          ? {
              id: replyTo.id,
              text: replyTo.text,
              senderName: replyTo.sender === 'me' ? currentUser.name : targetUser.name,
            }
          : null,
      });

      await updateConversation('Image');

      setReplyTo(null);
    } catch (error) {
      console.error('Error uploading image:', error);
      alert('Failed to upload image. Please try again.');
    } finally {
      setImageUploading(false);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  // ========== Clear Chat ==========
  const handleClearChat = async () => {
    try {
      const timestamp = Date.now();

      const chatDocRef = doc(db, 'chats', chatId);
      await setDoc(
        chatDocRef,
        {
          clearedAtRef: {
            [currentUser.uid]: timestamp,
          },
        },
        { merge: true }
      );

      setMessages([]);
      setShowOptions(false);
      setDeleteMode(false);
      setSelectedMessages(new Set());
    } catch (error) {
      console.error('Error clearing chat:', error);
    }
  };

  // ========== Delete single message ==========
  const handleDeleteMessage = async (messageId: string) => {
    try {
      const messageRef = doc(db, 'chats', chatId, 'messages', messageId);
      await deleteDoc(messageRef);
      setLongPressMsg(null);
      setShowDeleteConfirm(false);
      setDeleteMode(false);
      setSelectedMessages(new Set());
    } catch (error) {
      console.error('Error deleting message:', error);
    }
  };

  // ========== Delete selected messages (batch) ==========
  const handleDeleteSelectedMessages = async () => {
    try {
      const batch = writeBatch(db);
      selectedMessages.forEach((messageId) => {
        const messageRef = doc(db, 'chats', chatId, 'messages', messageId);
        batch.delete(messageRef);
      });
      await batch.commit();

      setDeleteMode(false);
      setSelectedMessages(new Set());
      setShowDeleteSelectedConfirm(false);
      setShowOptions(false);
    } catch (error) {
      console.error('Error deleting selected messages:', error);
      alert('Failed to delete messages. Please try again.');
    }
  };

  // ========== Block user ==========
  const handleBlockUser = async () => {
    try {
      const blockRef = doc(db, 'blocks', `${currentUser.uid}_${targetUser.uid}`);
      await setDoc(blockRef, {
        blockedBy: currentUser.uid,
        blockedUser: targetUser.uid,
        timestamp: serverTimestamp(),
      });
      setShowBlockConfirm(false);
      setShowOptions(false);
      onClose();
    } catch (error) {
      console.error('Error blocking user:', error);
    }
  };

  // ========== Report user ==========
  const handleReportUser = async () => {
    try {
      const reportRef = doc(db, 'reports', `${currentUser.uid}_${targetUser.uid}_${Date.now()}`);
      await setDoc(reportRef, {
        reportedBy: currentUser.uid,
        reportedUser: targetUser.uid,
        reportedUserName: targetUser.name,
        timestamp: serverTimestamp(),
        chatId: chatId,
      });

      setShowReportConfirm(false);
      setShowOptions(false);
      alert('User reported successfully.');
    } catch (error) {
      console.error('Error reporting user:', error);
      alert('Failed to report user. Please try again.');
    }
  };

  // ========== Reply to message ==========
  const handleReply = (msg: Message) => {
    setReplyTo(msg);
    setLongPressMsg(null);
    setSwipeMsgId(null);
  };

  // ========== Copy message text ==========
  const handleCopyMessage = (msg: Message) => {
    if (msg.text) {
      navigator.clipboard
        .writeText(msg.text)
        .then(() => {
          setCopiedMessage(msg.id);
          setTimeout(() => setCopiedMessage(null), 2000);
        })
        .catch((error) => {
          console.error('Error copying to clipboard:', error);
        });
    }
    setLongPressMsg(null);
  };

  // ========== Toggle selection in delete mode ==========
  const toggleMessageSelection = (messageId: string) => {
    const newSelected = new Set(selectedMessages);
    if (newSelected.has(messageId)) {
      newSelected.delete(messageId);
    } else {
      newSelected.add(messageId);
    }
    setSelectedMessages(newSelected);
  };

  // ========== Long press handlers ==========
  const handleTouchStart = (msg: Message) => {
    longPressTimerRef.current = setTimeout(() => {
      handleCopyMessage(msg);
    }, 1000);
  };

  const handleTouchEnd = () => {
    if (longPressTimerRef.current) {
      clearTimeout(longPressTimerRef.current);
      longPressTimerRef.current = null;
    }
  };

  const handleMouseDown = (msg: Message) => {
    longPressTimerRef.current = setTimeout(() => {
      handleCopyMessage(msg);
    }, 1000);
  };

  const handleMouseUp = () => {
    if (longPressTimerRef.current) {
      clearTimeout(longPressTimerRef.current);
      longPressTimerRef.current = null;
    }
  };

  // ========== Swipe handlers ==========
  const handleSwipeStart = (e: React.TouchEvent, msg: Message) => {
    setSwipeStartX(e.touches[0].clientX);
    setSwipeMsgId(msg.id);
  };

  const handleSwipeEnd = (e: React.TouchEvent) => {
    if (swipeStartX !== null && swipeMsgId) {
      const swipeEndX = e.changedTouches[0].clientX;
      const swipeDistance = swipeEndX - swipeStartX;

      if (Math.abs(swipeDistance) > 50) {
        const msg = messages.find((m) => m.id === swipeMsgId);
        if (msg) {
          handleReply(msg);
        }
      }
    }
    setSwipeStartX(null);
    setSwipeMsgId(null);
  };

  // ========== Enter key handler ==========
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  // ========== Join room from invite ==========
  const handleJoinRoom = (roomId: string) => {
    if (onJoinRoom) {
      onJoinRoom(roomId);
    }
  };

  // ========== Auto-scroll ==========
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  // ========== Format timestamp ==========
  const formatTime = (timestamp: number) => {
    return new Date(timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  // ========================= RENDER =========================
  return (
    <div className="fixed inset-0 z-50 bg-white flex flex-col">
      {/* ----- Header ----- */}
      <div
        className="px-4 pb-3 flex items-center gap-3 sticky top-0 z-10"
        style={{
          background: 'linear-gradient(to bottom, #3b82f6 0%, #eff6ff 70%, #ffffff 100%)',
          paddingTop: 'calc(env(safe-area-inset-top, 0px) + 12px)',
        }}
      >
        <button onClick={onClose} className="flex-shrink-0 hover:bg-white/30 rounded-full p-1">
          <ArrowLeft size={24} className="text-gray-800" />
        </button>

        <div className="w-10 h-10 rounded-full overflow-hidden flex-shrink-0">
          <img
            src={targetUser.photo || '/default-avatar.png'}
            alt={targetUser.name}
            className="w-full h-full object-cover"
          />
        </div>

        <div className="flex-1 min-w-0">
          <h2 className="text-lg font-bold text-gray-800 truncate">{targetUser.name}</h2>
          {!isFixedChat && (
            <span className={`text-xs ${online ? 'text-green-500' : 'text-gray-400'}`}>
              {online ? 'Online' : 'Offline'}
            </span>
          )}
        </div>

        {deleteMode ? (
          <div className="flex items-center gap-2">
            <span className="text-sm font-medium text-gray-700">
              {selectedMessages.size} selected
            </span>
            <button
              onClick={() => setShowDeleteSelectedConfirm(true)}
              disabled={selectedMessages.size === 0}
              className="px-3 py-1.5 bg-red-500 text-white rounded-full text-sm font-medium disabled:opacity-50"
            >
              Delete
            </button>
            <button
              onClick={() => {
                setDeleteMode(false);
                setSelectedMessages(new Set());
              }}
              className="p-1.5 hover:bg-white/30 rounded-full"
            >
              <X size={20} className="text-gray-800" />
            </button>
          </div>
        ) : (
          !isFixedChat && (
            <div className="relative">
              <button
                onClick={() => setShowOptions(!showOptions)}
                className="flex-shrink-0 hover:bg-white/30 rounded-full p-1"
              >
                <MoreHorizontal size={24} className="text-gray-800" />
              </button>

              {showOptions && (
                <>
                  <div className="fixed inset-0 z-40" onClick={() => setShowOptions(false)} />
                  <div className="absolute right-0 top-full mt-2 w-48 bg-white rounded-xl shadow-lg border border-gray-200 z-50 overflow-hidden">
                    <button
                      onClick={() => {
                        setShowOptions(false);
                        setShowReportConfirm(true);
                      }}
                      className="w-full px-4 py-3 text-left text-black hover:bg-gray-50 flex items-center gap-3 transition-colors"
                    >
                      <Flag size={18} className="text-orange-500" />
                      <span className="text-sm font-medium">Report</span>
                    </button>
                    <button
                      onClick={handleClearChat}
                      className="w-full px-4 py-3 text-left text-black hover:bg-gray-50 flex items-center gap-3 transition-colors border-t border-gray-100"
                    >
                      <Trash2 size={18} className="text-gray-500" />
                      <span className="text-sm font-medium">Clear Chat</span>
                    </button>
                    <button
                      onClick={() => {
                        setShowOptions(false);
                        setDeleteMode(true);
                      }}
                      className="w-full px-4 py-3 text-left text-black hover:bg-gray-50 flex items-center gap-3 transition-colors border-t border-gray-100"
                    >
                      <Trash2 size={18} className="text-red-500" />
                      <span className="text-sm font-medium">Delete Messages</span>
                    </button>
                    <button
                      onClick={() => {
                        setShowOptions(false);
                        setShowBlockConfirm(true);
                      }}
                      className="w-full px-4 py-3 text-left text-black hover:bg-red-50 flex items-center gap-3 transition-colors border-t border-gray-100"
                    >
                      <Ban size={18} className="text-red-500" />
                      <span className="text-sm font-medium">Block User</span>
                    </button>
                  </div>
                </>
              )}
            </div>
          )
        )}
      </div>

      {/* ----- Messages area ----- */}
      <div className="flex-1 overflow-y-auto px-4 py-4 space-y-3 bg-gray-50">
        {isLoadingMessages && messages.length === 0 && (
          <div className="text-center py-8">
            <div className="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
          </div>
        )}
        
        {!isLoadingMessages && messages.length === 0 && (
          <p className="text-center text-gray-400 mt-20">
            {isFixedChat
              ? 'No messages from ' + targetUser.name + ' yet'
              : 'No messages yet. Say hello!'}
          </p>
        )}
        
        {messages.map((msg) => {
          const isMine = msg.sender === 'me';
          const isSelected = selectedMessages.has(msg.id);

          // ---- Room invite ----
          if (msg.type === 'room_invite' && msg.roomData) {
            return (
              <div
                key={msg.id}
                className={`flex ${isMine ? 'justify-end' : 'justify-start'} ${
                  deleteMode ? 'cursor-pointer' : ''
                }`}
                onClick={() => deleteMode && toggleMessageSelection(msg.id)}
                onTouchStart={(e) => !deleteMode && handleSwipeStart(e, msg)}
                onTouchEnd={(e) => !deleteMode && handleSwipeEnd(e)}
              >
                <div
                  className={`max-w-[80%] overflow-hidden rounded-2xl shadow-md ${
                    isMine ? 'rounded-br-md' : 'rounded-bl-md'
                  } ${isSelected ? 'ring-2 ring-blue-500' : ''}`}
                >
                  <div className="relative h-40 bg-gray-200">
                    <img
                      src={msg.roomData.roomImage || '/default-avatar.png'}
                      alt={msg.roomData.roomName}
                      className="w-full h-full object-cover"
                    />
                    <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent" />
                    <div className="absolute bottom-2 left-3 right-3">
                      <p className="text-white font-bold text-sm truncate">{msg.roomData.roomName}</p>
                    </div>
                  </div>
                  <div className="bg-white p-3">
                    <p className="text-sm text-gray-700 mb-2">Joins our Party Room</p>
                    <button
                      onClick={() => handleJoinRoom(msg.roomData!.roomId)}
                      className="w-full bg-blue-500 hover:bg-blue-600 text-white font-semibold py-2.5 rounded-full flex items-center justify-center gap-2 transition-colors cursor-pointer"
                    >
                      <LogIn size={18} />
                      Enter
                    </button>
                    <p className="text-[10px] text-gray-400 mt-1 text-center">
                      {formatTime(msg.timestamp)}
                    </p>
                  </div>
                </div>
              </div>
            );
          }

          // ---- Image message ----
          if (msg.type === 'image' && msg.imageUrl) {
            return (
              <div
                key={msg.id}
                className={`flex ${isMine ? 'justify-end' : 'justify-start'} ${
                  deleteMode ? 'cursor-pointer' : ''
                }`}
                onClick={() => deleteMode && toggleMessageSelection(msg.id)}
                onTouchStart={(e) => !deleteMode && handleSwipeStart(e, msg)}
                onTouchEnd={(e) => !deleteMode && handleSwipeEnd(e)}
              >
                {!isMine && (
                  <div className="w-7 h-7 rounded-full overflow-hidden flex-shrink-0 mr-2 mt-auto">
                    <img
                      src={targetUser.photo || '/default-avatar.png'}
                      alt={targetUser.name}
                      className="w-full h-full object-cover"
                    />
                  </div>
                )}
                <div className={`flex flex-col ${isMine ? 'items-end' : 'items-start'} max-w-[75%]`}>
                  <div
                    className={`rounded-2xl overflow-hidden relative ${
                      isMine ? 'rounded-br-md' : 'rounded-bl-md'
                    } ${isSelected ? 'ring-2 ring-blue-500' : ''}`}
                  >
                    {msg.replyTo && (
                      <div className="px-3 pt-2 bg-white/95">
                        <div className="border-l-4 border-blue-400 pl-2 bg-black/5 rounded p-1">
                          <p className="text-[10px] font-semibold text-blue-600">
                            {msg.replyTo.senderName}
                          </p>
                          <p className="text-[11px] text-gray-600 truncate">{msg.replyTo.text}</p>
                        </div>
                      </div>
                    )}
                    <img
                      src={msg.imageUrl}
                      alt="Shared image"
                      className="max-w-full h-auto max-h-64 object-cover"
                    />
                    <div className={`px-2 py-1 ${isMine ? 'bg-[#dcf8c6]' : 'bg-white'}`}>
                      <p className={`text-[10px] text-right ${isMine ? 'text-gray-500' : 'text-gray-400'}`}>
                        {formatTime(msg.timestamp)}
                      </p>
                    </div>
                  </div>
                </div>
                {isMine && (
                  <div className="w-7 h-7 rounded-full overflow-hidden flex-shrink-0 ml-2 mt-auto">
                    <img
                      src={currentUser.photo || '/default-avatar.png'}
                      alt={currentUser.name}
                      className="w-full h-full object-cover"
                    />
                  </div>
                )}
              </div>
            );
          }

          // ---- Regular text message ----
          return (
            <div
              key={msg.id}
              className={`flex ${isMine ? 'justify-end' : 'justify-start'} ${
                deleteMode ? 'cursor-pointer' : ''
              }`}
              onClick={() => deleteMode && toggleMessageSelection(msg.id)}
              onTouchStart={(e) => {
                if (!deleteMode) {
                  handleTouchStart(msg);
                  handleSwipeStart(e, msg);
                }
              }}
              onTouchEnd={(e) => {
                if (!deleteMode) {
                  handleTouchEnd();
                  handleSwipeEnd(e);
                }
              }}
              onMouseDown={() => !deleteMode && handleMouseDown(msg)}
              onMouseUp={() => !deleteMode && handleMouseUp()}
            >
              {!isMine && (
                <div className="w-7 h-7 rounded-full overflow-hidden flex-shrink-0 mr-2 mt-auto">
                  <img
                    src={targetUser.photo || '/default-avatar.png'}
                    alt={targetUser.name}
                    className="w-full h-full object-cover"
                  />
                </div>
              )}
              <div className={`flex flex-col ${isMine ? 'items-end' : 'items-start'} max-w-[75%]`}>
                {!isMine && (
                  <span className="text-[10px] text-gray-500 ml-1 mb-0.5">{targetUser.name}</span>
                )}
                <div
                  className={`px-3 py-2 rounded-2xl break-words relative ${
                    isMine
                      ? 'bg-[#dcf8c6] text-gray-800 rounded-br-md'
                      : 'bg-white text-gray-800 rounded-bl-md shadow-sm'
                  } ${isSelected ? 'ring-2 ring-blue-500' : ''}`}
                >
                  {msg.replyTo && (
                    <div className="border-l-4 border-blue-400 pl-2 mb-1 bg-black/5 rounded p-1">
                      <p className="text-[10px] font-semibold text-blue-600">
                        {msg.replyTo.senderName}
                      </p>
                      <p className="text-[11px] text-gray-600 truncate">{msg.replyTo.text}</p>
                    </div>
                  )}
                  <p className="text-sm">{msg.text}</p>
                  <p className={`text-[10px] mt-1 ${isMine ? 'text-gray-500' : 'text-gray-400'}`}>
                    {formatTime(msg.timestamp)}
                  </p>
                </div>
                {copiedMessage === msg.id && (
                  <span className="text-[10px] text-green-600 mt-0.5">Copied!</span>
                )}
              </div>
              {isMine && (
                <div className="w-7 h-7 rounded-full overflow-hidden flex-shrink-0 ml-2 mt-auto">
                  <img
                    src={currentUser.photo || '/default-avatar.png'}
                    alt={currentUser.name}
                    className="w-full h-full object-cover"
                  />
                </div>
              )}
            </div>
          );
        })}
        <div ref={messagesEndRef} />
      </div>

      {/* ----- Reply bar ----- */}
      {replyTo && !deleteMode && (
        <div className="px-4 py-2 bg-gray-100 border-t border-gray-200 flex items-center gap-2">
          <div className="flex-1 border-l-4 border-blue-400 pl-2 bg-white rounded p-2">
            <p className="text-[10px] font-semibold text-blue-600">
              Replying to {replyTo.sender === 'me' ? 'yourself' : targetUser.name}
            </p>
            <p className="text-xs text-gray-600 truncate">{replyTo.text || 'Image'}</p>
          </div>
          <button onClick={() => setReplyTo(null)} className="p-1 hover:bg-gray-200 rounded-full">
            <X size={18} className="text-gray-500" />
          </button>
        </div>
      )}

      {/* ----- Input bar ----- */}
      {!isFixedChat && !deleteMode && (
        <div className="px-4 py-3 bg-white border-t border-gray-200 flex items-center gap-2">
          <input
            type="file"
            ref={fileInputRef}
            onChange={handleImageUpload}
            accept="image/*"
            className="hidden"
          />
          <button
            className="text-gray-500 hover:text-gray-700 disabled:opacity-50"
            onClick={() => fileInputRef.current?.click()}
            disabled={imageUploading}
          >
            {imageUploading ? (
              <div className="w-6 h-6 border-2 border-gray-300 border-t-blue-500 rounded-full animate-spin" />
            ) : (
              <ImageIcon size={24} />
            )}
          </button>
          <input
            type="text"
            value={newMessage}
            onChange={(e) => setNewMessage(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Type a message..."
            className="flex-1 bg-gray-100 rounded-full px-4 py-2.5 text-sm outline-none focus:ring-2 focus:ring-blue-300"
          />
          <button
            onClick={handleSend}
            disabled={!newMessage.trim()}
            className="text-blue-500 disabled:text-gray-300 hover:text-blue-600"
          >
            <Send size={24} />
          </button>
        </div>
      )}

      {isFixedChat && (
        <div className="px-4 py-3 bg-gray-50 border-t border-gray-200 text-center">
          <p className="text-xs text-gray-400">This is an official account. You cannot reply here.</p>
        </div>
      )}

      {/* ----- Block Confirmation ----- */}
      {showBlockConfirm && (
        <div
          className="absolute inset-0 z-[70] flex items-center justify-center bg-black/50"
          onClick={() => setShowBlockConfirm(false)}
        >
          <div
            className="bg-white rounded-2xl px-6 py-5 shadow-xl max-w-xs w-full text-center"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="text-4xl mb-3">🚫</div>
            <h3 className="text-lg font-bold text-gray-800 mb-1">Block {targetUser.name}?</h3>
            <p className="text-sm text-gray-500 mb-4">You won't receive messages from this user.</p>
            <div className="flex gap-2">
              <button
                onClick={() => setShowBlockConfirm(false)}
                className="flex-1 py-2.5 bg-gray-100 hover:bg-gray-200 text-gray-700 font-semibold rounded-full transition-colors cursor-pointer"
              >
                Cancel
              </button>
              <button
                onClick={handleBlockUser}
                className="flex-1 py-2.5 bg-red-500 hover:bg-red-600 text-white font-semibold rounded-full transition-colors cursor-pointer"
              >
                Block
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ----- Report Confirmation ----- */}
      {showReportConfirm && (
        <div
          className="absolute inset-0 z-[70] flex items-center justify-center bg-black/50"
          onClick={() => setShowReportConfirm(false)}
        >
          <div
            className="bg-white rounded-2xl px-6 py-5 shadow-xl max-w-xs w-full text-center"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="text-4xl mb-3">🚨</div>
            <h3 className="text-lg font-bold text-gray-800 mb-1">Report {targetUser.name}?</h3>
            <p className="text-sm text-gray-500 mb-4">This user will be reviewed by our team.</p>
            <div className="flex gap-2">
              <button
                onClick={() => setShowReportConfirm(false)}
                className="flex-1 py-2.5 bg-gray-100 hover:bg-gray-200 text-gray-700 font-semibold rounded-full transition-colors cursor-pointer"
              >
                Cancel
              </button>
              <button
                onClick={handleReportUser}
                className="flex-1 py-2.5 bg-orange-500 hover:bg-orange-600 text-white font-semibold rounded-full transition-colors cursor-pointer"
              >
                Report
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ----- Delete Selected Messages Confirmation ----- */}
      {showDeleteSelectedConfirm && (
        <div
          className="absolute inset-0 z-[70] flex items-center justify-center bg-black/50"
          onClick={() => setShowDeleteSelectedConfirm(false)}
        >
          <div
            className="bg-white rounded-2xl px-6 py-5 shadow-xl max-w-xs w-full text-center"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="text-4xl mb-3">🗑️</div>
            <h3 className="text-lg font-bold text-gray-800 mb-1">
              Delete {selectedMessages.size} Message{selectedMessages.size > 1 ? 's' : ''}?
            </h3>
            <p className="text-sm text-gray-500 mb-4">This action cannot be undone.</p>
            <div className="flex gap-2">
              <button
                onClick={() => setShowDeleteSelectedConfirm(false)}
                className="flex-1 py-2.5 bg-gray-100 hover:bg-gray-200 text-gray-700 font-semibold rounded-full transition-colors cursor-pointer"
              >
                Cancel
              </button>
              <button
                onClick={handleDeleteSelectedMessages}
                className="flex-1 py-2.5 bg-red-500 hover:bg-red-600 text-white font-semibold rounded-full transition-colors cursor-pointer"
              >
                Delete
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
    }
