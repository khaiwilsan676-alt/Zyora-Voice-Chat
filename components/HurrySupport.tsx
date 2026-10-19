"use client";

import React, { useState, useEffect, useRef } from "react";
import { ArrowLeft, Send } from "lucide-react";
import { socket } from "../src/lib/socket";

interface HurrySupportProps {
  onBack?: () => void;
}

interface MessageType {
  text?: string;
  image?: string;
  isBot: boolean;
  timestamp?: number;
}

export default function HurrySupport({ onBack }: HurrySupportProps) {
  const [message, setMessage] = useState("");
  const [messages, setMessages] = useState<Array<MessageType>>([]);
  const [isTyping, setIsTyping] = useState(false);
  const [userId, setUserId] = useState<string>("");
  const [userName, setUserName] = useState<string>("");
  const [userEmail, setUserEmail] = useState<string>("");
  const [userPhoto, setUserPhoto] = useState<string>("");
  const [userAccountId, setUserAccountId] = useState<string>("");
  const [chatDocId, setChatDocId] = useState<string>("");
  const chatEndRef = useRef<HTMLDivElement>(null);
  const chatLoadedRef = useRef(false);

  // Get current user info & initialize socket connection
  useEffect(() => {
    if (typeof window !== 'undefined') {
      const uid = localStorage.getItem('userUID') || localStorage.getItem('userPhone') || 'anonymous';
      const name = localStorage.getItem('userName') || 'User';
      const email = localStorage.getItem('userEmail') || '';
      const photo = localStorage.getItem('userPhoto') || '';
      const accNum = localStorage.getItem('accountNumber') || '';
      setUserId(uid);
      setUserName(name);
      setUserEmail(email);
      setUserPhoto(photo);
      setUserAccountId(accNum);

      if (!socket.connected) {
        socket.connect();
      }
      socket.emit('register', { userId: uid, accountId: accNum });
    }
  }, []);

  // Load existing support chat from IndexedDB
  useEffect(() => {
    if (!userId || chatLoadedRef.current) return;

    const DB_NAME = "HurrySupportDB";
    const STORE_NAME = "supportChats";

    const openSupportDB = (): Promise<IDBDatabase> => {
      return new Promise((resolve, reject) => {
        const request = indexedDB.open(DB_NAME, 1);

        request.onerror = () => reject(request.error);
        request.onsuccess = () => resolve(request.result);

        request.onupgradeneeded = () => {
          const db = request.result;

          if (!db.objectStoreNames.contains(STORE_NAME)) {
            db.createObjectStore(STORE_NAME, { keyPath: "userId" });
          }
        };
      });
    };

    const loadOrCreateChat = async () => {
      try {
        const db = await openSupportDB();
        const transaction = db.transaction([STORE_NAME], "readonly");
        const store = transaction.objectStore(STORE_NAME);

        const cachedChat = await new Promise<any>((resolve, reject) => {
          const request = store.get(userId);
          request.onsuccess = () => resolve(request.result);
          request.onerror = () => reject(request.error);
        });

        db.close();

        if (
          cachedChat &&
          Array.isArray(cachedChat.messages) &&
          cachedChat.messages.length > 0
        ) {
          setMessages(cachedChat.messages);
        } else {
          const welcomeMessage: MessageType = {
            text: "Welcome to Zaho Support 👋\\n\\nI am Daisy, your Official Customer Support Assistant.\\n\\nHow can I help you today? Aap Zaho App se juda koi bhi sawal pooch sakte hain! 😊",
            isBot: true,
            timestamp: Date.now()
          };

          setMessages([welcomeMessage]);

          const saveDB = await openSupportDB();
          const writeTransaction = saveDB.transaction(
            [STORE_NAME],
            "readwrite"
          );
          const writeStore = writeTransaction.objectStore(STORE_NAME);

          const chatPayload = {
            userId,
            userName,
            userEmail,
            userPhoto,
            userAccountId,
            messages: [welcomeMessage],
            timestamp: Date.now()
          };

          writeStore.put(chatPayload);
          saveDB.close();

          if (socket) {
            socket.emit("ai_support_message", {
              ...chatPayload,
              lastMessage: welcomeMessage
            });
          }
        }

        chatLoadedRef.current = true;
      } catch (error) {
        console.error("Error loading support chat from IndexedDB:", error);

        const welcomeMessage: MessageType = {
          text: "Welcome to Zaho Support 👋\\n\\nI am Daisy, your Official Customer Support Assistant.\\n\\nHow can I help you today? Aap Zaho App se juda koi bhi sawal pooch sakte hain! 😊",
          isBot: true,
          timestamp: Date.now()
        };

        setMessages([welcomeMessage]);
        chatLoadedRef.current = true;
      }
    };

    loadOrCreateChat();
  }, [userId, userName, userEmail]);

  // Save support chat to IndexedDB and sync via Socket.IO
  const saveChatToIndexedDB = async (updatedMessages: Array<MessageType>) => {
    if (!userId) return;

    try {
      const db = await new Promise<IDBDatabase>((resolve, reject) => {
        const request = indexedDB.open("HurrySupportDB", 1);

        request.onerror = () => reject(request.error);
        request.onsuccess = () => resolve(request.result);

        request.onupgradeneeded = () => {
          const database = request.result;

          if (!database.objectStoreNames.contains("supportChats")) {
            database.createObjectStore("supportChats", {
              keyPath: "userId"
            });
          }
        };
      });

      const transaction = db.transaction(["supportChats"], "readwrite");
      const store = transaction.objectStore("supportChats");

      const chatPayload = {
        userId,
        userName,
        userEmail,
        userPhoto,
        userAccountId,
        messages: updatedMessages,
        timestamp: Date.now()
      };

      store.put(chatPayload);
      db.close();

      // Emit real-time Socket.IO message event
      if (socket) {
        socket.emit("ai_support_message", {
          ...chatPayload,
          lastMessage: updatedMessages[updatedMessages.length - 1]
        });
      }
    } catch (error) {
      console.error("Error saving support chat to IndexedDB:", error);
    }
  };

  // Auto-scroll to bottom
  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  // Bad words filter
  const containsBadWords = (text: string) => {
    const badWords = [
      "gandu", "gaandu", "chutiya", "chutiye", 
      "bhosdike", "bhonsdike", "bhosdiwala", "bhonsdiwala",
      "madarchod", "maadar chod", 
      "behenchod", "bhenchod", "behanchod", "bahanchod",
      "lauda", "lund", "loda", "laude", "lodu",
      "chuchi", "chuchiya", "chuche",
      "chodu", "chodna", "choda", "chodi",
      "teri maa", "teri ma",
      "teri bhen", "teri behen",
      "hijda", "hijra", "hijde", "chhakka", "chhakke", "kinnar",
      "kutiya", "kutti", "kamine", "kamina", "kaminey",
      "harami", "haramkhor", "haraami", "haramzaada", "haramzada",
      "suar", "sawar", "suvar", "sowar", "bhains", "bhainsa",
      "bhak cho", 
      "lavda", "lavde", "lwda", "lwde", "lawda", "lawde",
      "randi", "raand", "randikhana", "randikhane",
      "randibaaz", "randibaz", "randiya", "randiyo",
      "fuck", "fucker", "fucking", "fucked",
      "shit", "shitty", "bullshit",
      "asshole", "arsehole",
      "bitch", "bitching", "bitchy", "bastard",
      "cunt", "dickhead", "whore", "slut", "motherfucker",
      "cock", "tits", "boobs", "nigger", "nigga",
      "porn", "porno", "nude", "naked",
      "horny", "blowjob", "handjob", "cum", "sperm", "orgasm",
      "rape", "molest", "pedo",
      "chudai", "nanga", "nangi",
    ];
    
    const lowerText = text.toLowerCase().trim();
    const words = lowerText.split(/\s+/);
    
    return badWords.some(badWord => {
      if (badWord.includes(' ')) {
        return lowerText.includes(badWord);
      }
      return words.some(word => {
        const cleanWord = word.replace(/[^a-zA-Z0-9\u0900-\u097F]/g, '');
        return cleanWord === badWord;
      });
    });
  };

  // Response generation logic
  const generateBotResponse = (userMsg: string): MessageType => {
    const msg = userMsg.toLowerCase().trim();
    const isHindi = /[\u0900-\u097F]/.test(msg);
    
    // Check if question asks for Yes / No confirmation
    const isYesNoQuestion = 
      msg.startsWith("can i") || msg.startsWith("can we") || msg.startsWith("is it") || 
      msg.startsWith("is there") || msg.startsWith("do you") || msg.startsWith("kya ") || 
      msg.includes("ho sakta h") || msg.includes("ho sakta hai") || msg.includes("kya main") ||
      msg.includes("kya room") || msg.includes("kya coin") || msg.includes("kya hum");

    // Helper for Yes / No Prefix
    const getYesPrefix = () => {
      if (isHindi) return "हाँ, बिल्कुल! ";
      if (msg.includes("kya") || msg.includes("ho sakta")) return "Yes, bilkul! ";
      return "Yes, absolutely! ";
    };

    // SELLER POLICY (Shows Policy Image)
    if (msg.includes("seller") || msg.includes("seller policy") || msg.includes("become seller") || 
        msg.includes("seller lena") || msg.includes("seller banna") || msg.includes("seller ki policy")) {
      let policyText = "Here is our Official Seller Policy. Please check the policy image below:";
      if (isHindi) {
        policyText = "यह हमारी ऑफिसियल सेलर पॉलिसी है। कृपया नीचे दी गई इमेज देखें:";
      } else if (msg.includes("kya") || msg.includes("hai")) {
        policyText = "Yeh hai hamari Official Seller Policy! Aap niche di gayi image check kar sakte hain:";
      }

      return {
        text: policyText,
        image: "/file_00000000a4a882089e08989f17098062.png",
        isBot: true
      };
    }

    // GREETINGS
    if (msg.includes("hi") || msg.includes("hello") || msg.includes("hlw") || 
        msg.includes("hey") || msg.includes("नमस्ते") || msg.includes("नमस्कार") ||
        msg.includes("good morning") || msg.includes("good evening")) {
      if (isHindi) {
        return {
          text: "नमस्ते! 🙏\n\nZyora Support में आपका स्वागत है। मैं Daisy हूँ, आपकी AI कस्टमर सपोर्ट असिस्टेंट।\n\nबताएं, मैं आज आपकी क्या सहायता कर सकती हूँ?",
          isBot: true
        };
      }
      return {
        text: "Hello! 👋\n\nWelcome to Zaho Support. I am Daisy, your AI Customer Support Assistant.\n\nKaise help kar sakti hoon aaj aapki? Please feel free to ask!",
        isBot: true
      };
    }

    // THANK YOU
    if (msg.includes("thank") || msg.includes("thanks") || msg.includes("thx") ||
        msg.includes("धन्यवाद") || msg.includes("शुक्रिया")) {
      return {
        text: isHindi 
          ? "आपका स्वागत है! 😊\n\nआपकी मदद करके ख़ुशी हुई। अगर कोई और सवाल हो तो बेझिझक पूछें।"
          : "You're most welcome! 😊\n\nHelp karke khushi hui. Agar koi aur doubt ho toh zaroor batayein!",
        isBot: true
      };
    }

    // COIN RATES & RECHARGE INFO
    if (msg.includes("rate") || msg.includes("coin rate") || msg.includes("price") || 
        msg.includes("1$") || msg.includes("dollar") || msg.includes("recharge") || 
        msg.includes("coin") || msg.includes("buy coin") || msg.includes("purchase") || 
        msg.includes("रिचार्ज") || msg.includes("सिक्का") || msg.includes("खरीद") || 
        msg.includes("payment") || msg.includes("pay") || msg.includes("google play") || 
        msg.includes("phonepay") || msg.includes("paytm")) {
      
      const prefix = isYesNoQuestion ? getYesPrefix() : "";
      
      return {
        text: prefix + "💎 Zaho App Coins Rates & Recharge Info\n\n" +
               "💰 Coins Exchange Rates:\n" +
               "• Offline (Through Official Seller):\n" +
               "  👉 1$ = 1,050,000 Coins\n\n" +
               "• Online Recharge (In-App Wallet):\n" +
               "  👉 1$ = 1,000,000 Coins (+30,000 Bonus Coins)\n" +
               "  (Total: 1,030,000 Coins aapke Wallet me show honge)\n\n" +
               "📲 Online Recharge Kaise Karein:\n" +
               "📌 Step 1: Profile section me jayein.\n" +
               "📌 Step 2: Coins / Wallet par tap karein.\n" +
               "📌 Step 3: Package & preferred payment method select karein.\n\n" +
               "💳 Payment Methods: Google Play, PhonePe, Paytm & Official Site.\n\n" +
               "🤝 Offline Seller se buy karne ke liye Official Team se contact karein.",
        isBot: true
      };
    }

    // ROOM MUSIC
    if (msg.includes("music") || msg.includes("song") || msg.includes("play music") ||
        msg.includes("music play") || msg.includes("room music") || msg.includes("संगीत") ||
        msg.includes("गाना") || msg.includes("म्यूजिक") || msg.includes("dj")) {
      
      const prefix = isYesNoQuestion ? getYesPrefix() : "";

      return {
        text: prefix + "🎵 Room Me Music Kaise Play Karein:\n\n" +
               "📌 Step 1: Apna room open karein.\n" +
               "📌 Step 2: Bottom me 4-Grid Option (4 squares icon) par tap karein.\n" +
               "📌 Step 3: Music Play option par click karein.\n" +
               "📌 Step 4: Song select karke add karein aur Play dabaayein.\n\n" +
               "🎶 Room members ke saath music enjoy karein!",
        isBot: true
      };
    }

    // PLAY GAMES
    if (msg.includes("game") || msg.includes("play game") || msg.includes("खेल") ||
        msg.includes("गेम") || msg.includes("khel") || msg.includes("gaming")) {
      
      const prefix = isYesNoQuestion ? getYesPrefix() : "";

      return {
        text: prefix + "🎮 Games Khelkar Coins Kaise Jeetein:\n\n" +
               "📌 Step 1: Room open karein.\n" +
               "📌 Step 2: Bottom me 4-Grid Option par tap karein.\n" +
               "📌 Step 3: Game Remote icon choose karein.\n" +
               "📌 Step 4: Kisi bhi game ko select karke dosto ke saath khele!",
        isBot: true
      };
    }

    // CREATE ROOM
    if (msg.includes("create room") || msg.includes("open room") || msg.includes("new room") ||
        msg.includes("रूम") || msg.includes("कमरा") || msg.includes("room banao") ||
        msg.includes("room open") || msg.includes("room create") || msg.includes("room kholo")) {
      
      const prefix = isYesNoQuestion ? getYesPrefix() : "";

      return {
        text: prefix + "🏠 Apna Room Kaise Banayein:\n\n" +
               "📌 Step 1: Homepage par jayein.\n" +
               "📌 Step 2: Top me Mine tab par tap karein.\n" +
               "📌 Step 3: + Icon Card par click karein.\n" +
               "📌 Step 4: Room me enter karke apne hisab se customize karein.",
        isBot: true
      };
    }

    // ABUSE / COMPLAINT
    if (msg.includes("abuse") || msg.includes("gali") || msg.includes("गाली") ||
        msg.includes("complain") || msg.includes("report") || msg.includes("शिकायत") ||
        msg.includes("bad word") || msg.includes("ban") || msg.includes("harass")) {
      return {
        text: "⚠️ Complaint & Report Kaise Karein:\n\n" +
               "📌 Step 1: Help & Feedback section me jayein.\n" +
               "📌 Step 2: Clear video ya screenshot proof upload karein.\n" +
               "📌 Step 3: Official Team ko complaint submit karein.\n\n" +
               "🚫 Rules & Warning:\n" +
               "Kripya khud abusive language use na karein. Agar dono side se gali di gayi ho, toh dono IDs ban ho sakti hain.",
        isBot: true
      };
    }

    // WHAT IS ZYORA APP
    if (msg.includes("hawa kya") || msg.includes("what is hawa") || msg.includes("about hawa") ||
        msg.includes("hawa app") || msg.includes("app kya") || msg.includes("what is zyora") || msg.includes("zyora app")) {
      return {
        text: "🌟 About Zyora App:\n\n" +
               "Zyora ek Social Dating & Live Party Platform hai jahan aap naye dosto se mil sakte hain, party rooms join kar sakte hain aur games khel sakte hain.\n\n" +
               "✨ Key Features:\n" +
               "• Live Audio Party Rooms\n" +
               "• Interactive Mini Games\n" +
               "• Family Creation System\n" +
               "• Social Dating & Chatting",
        isBot: true
      };
    }

    // OFFICIALS
    if (msg.includes("official") || msg.includes("admin") || msg.includes("contact") ||
        msg.includes("officer") || msg.includes("अधिकारी") || msg.includes("मालिक") ||
        msg.includes("owner") || msg.includes("team")) {
      return {
        text: "👑 Zyora Official Team Details:\n\n" +
               "• Zyora_IN\n" +
               "  🆔 ID: 100002\n\n" +
               "• Paras\n" +
               "  🆔 ID: 100003\n\n" +
               "Seller Registration, Payment issues ya Serious Complaints ke liye Official Team se contact karein.",
        isBot: true
      };
    }

    // FAMILY FEATURE
    if (msg.includes("family") || msg.includes("परिवार") || msg.includes("fam") ||
        msg.includes("family tag") || msg.includes("family create") || msg.includes("parivaar")) {
      
      const prefix = isYesNoQuestion ? getYesPrefix() : "";

      return {
        text: prefix + "👨‍👩‍👧‍👦 Family Kaise Banayein:\n\n" +
               "📌 Steps:\n" +
               "• Step 1: Profile me jayein.\n" +
               "• Step 2: Family option par tap karein.\n" +
               "• Step 3: Family Name & Banner Image set karein.\n" +
               "• Step 4: Confirm karke create karein.\n\n" +
               "💰 Cost: 1,300,000 Coins\n" +
               "🏷️ Benefits: Public Profile par Custom Family Tag milega aur exclusive privileges milenge.",
        isBot: true
      };
    }

    // PROFILE EDIT
    if (msg.includes("name change") || msg.includes("dp change") || msg.includes("profile picture") ||
        msg.includes("bio") || msg.includes("cover") || msg.includes("age") ||
        msg.includes("name badlo") || msg.includes("dp badlo") || msg.includes("edit profile") || 
        msg.includes("change name") || msg.includes("change dp")) {
      
      const prefix = isYesNoQuestion ? getYesPrefix() : "";

      return {
        text: prefix + "✏️ Profile Details Edit Kaise Karein:\n\n" +
               "📌 Step 1: Profile open karein.\n" +
               "📌 Step 2: Top Right Arrow click karke Public Profile me jayein.\n" +
               "📌 Step 3: Top-Right corner me Pencil Icon (✏️) par click karein.\n" +
               "📌 Step 4: Yahan se Name, DP, Bio, aur Age change karein.\n\n" +
               "🖼️ Cover Photo: Level 24 achieve hone par unlock hoti hai.",
        isBot: true
      };
    }

    // FALLBACK FOR YES / NO QUESTIONS
    if (isYesNoQuestion) {
      return {
        text: isHindi 
          ? "जी हाँ! इसके बारे में अधिक जानकारी पाने के लिए कृपया अपना सवाल विस्तार से पूछें। 😊"
          : "Yes! Aap aisa kar sakte hain. Thoda aur details me batayein taaki main sahi guide kar sakoon. 😊",
        isBot: true
      };
    }

    // DEFAULT RESPONSE
    return {
      text: "Thank you for reaching out! 😊\n\n" +
             "Main aapki kya help kar sakti hoon? Aap in topics ke bare me pooch sakte hain:\n\n" +
             "💎 • Coin Rates & Online Recharge\n" +
             "💼 • Official Seller Policy\n" +
             "🎵 • Room Me Song Play Karna\n" +
             "🎮 • Games Khelna\n" +
             "🏠 • Room Create Karna\n" +
             "⚠️ • Misconduct Report Karna\n" +
             "👑 • Official Team Support\n" +
             "👨‍👩‍👧‍👦 • Family Tag Option\n" +
             "✏️ • Profile Edit Options",
      isBot: true
    };
  };

  const handleSend = () => {
    if (!message.trim()) return;
    
    // Silent filter for abusive words
    if (containsBadWords(message)) {
      setMessage("");
      return;
    }
    
    const userMessage: MessageType = { text: message, isBot: false, timestamp: Date.now() };
    const updatedMessages = [...messages, userMessage];
    setMessages(updatedMessages);
    setMessage("");
    setIsTyping(true);

    saveChatToIndexedDB(updatedMessages);

    setTimeout(() => {
      const botResponse = generateBotResponse(userMessage.text || "");
      botResponse.timestamp = Date.now();
      
      const finalMessages = [...updatedMessages, botResponse];
      setMessages(finalMessages);
      setIsTyping(false);
      
      saveChatToIndexedDB(finalMessages);
    }, 1000);
  };

  return (
    <div className="flex flex-col h-[100dvh] bg-slate-50 absolute inset-0 z-50 w-full">
      {/* HEADER */}
      <header className="flex items-center justify-between px-4 py-3 bg-gradient-to-r from-blue-500 to-sky-400 text-white shadow-md pt-[calc(env(safe-area-inset-top,0px)+24px)]">
        <div className="flex items-center gap-3">
          <button onClick={onBack || (() => window.history.back())} className="p-1 hover:bg-white/20 rounded-full transition">
            <ArrowLeft className="w-6 h-6" />
          </button>

          <div className="w-10 h-10 rounded-full bg-gradient-to-r from-pink-400 to-purple-500 flex items-center justify-center border-2 border-white/30 shadow-lg overflow-hidden">
            <img 
              src="/1785612362650~2.jpg" 
              alt="Daisy" 
              className="w-full h-full object-cover"
            />
          </div>

          <div>
            <h1 className="font-semibold text-lg leading-tight">Daisy</h1>
            <span className="text-xs text-blue-100 flex items-center gap-1">
              <span className="w-2 h-2 rounded-full bg-green-400 inline-block"></span> Online
            </span>
          </div>
        </div>
      </header>

      {/* CHAT AREA */}
      <div className="flex-1 overflow-y-auto p-4 space-y-3 pb-4">
        {messages.map((msg, index) => (
          <div key={index} className={`flex items-start gap-2 ${msg.isBot ? '' : 'flex-row-reverse'}`}>
            {msg.isBot && (
              <div className="w-8 h-8 rounded-full bg-gradient-to-r from-pink-400 to-purple-500 flex items-center justify-center flex-shrink-0 shadow-md overflow-hidden">
                <img 
                  src="/1785612362650~2.jpg" 
                  alt="Daisy" 
                  className="w-full h-full object-cover"
                />
              </div>
            )}
            <div className={`max-w-[85%] ${msg.isBot ? 'bg-white rounded-2xl rounded-tl-none shadow-sm border border-slate-100' : 'bg-blue-500 text-white rounded-2xl rounded-tr-none'} p-3`}>
              {msg.text && (
                <p className={`text-sm whitespace-pre-line ${msg.isBot ? 'text-slate-700' : 'text-white'}`}>
                  {msg.text}
                </p>
              )}
              {msg.image && (
                <div className="mt-2 rounded-lg overflow-hidden border border-slate-200 shadow-sm">
                  <img 
                    src={msg.image} 
                    alt="Seller Policy" 
                    className="w-full max-h-80 object-contain bg-slate-100" 
                  />
                </div>
              )}
            </div>
          </div>
        ))}
        {isTyping && (
          <div className="flex items-start gap-2">
            <div className="w-8 h-8 rounded-full bg-gradient-to-r from-pink-400 to-purple-500 flex items-center justify-center flex-shrink-0 shadow-md overflow-hidden">
              <img 
                src="/1785612362650~2.jpg" 
                alt="Daisy" 
                className="w-full h-full object-cover"
              />
            </div>
            <div className="bg-white rounded-2xl rounded-tl-none shadow-sm border border-slate-100 p-3">
              <div className="flex gap-1">
                <span className="w-2 h-2 bg-slate-400 rounded-full animate-bounce" style={{ animationDelay: '0ms' }}></span>
                <span className="w-2 h-2 bg-slate-400 rounded-full animate-bounce" style={{ animationDelay: '150ms' }}></span>
                <span className="w-2 h-2 bg-slate-400 rounded-full animate-bounce" style={{ animationDelay: '300ms' }}></span>
              </div>
            </div>
          </div>
        )}
        <div ref={chatEndRef} />
      </div>

      {/* INPUT BAR */}
      <div className="bg-white border-t border-gray-200 shadow-lg">
        <div className="flex items-center gap-2 bg-gray-50 rounded-lg px-3 py-2 m-3 shadow-inner">
          <input
            type="text"
            value={message}
            onChange={(e) => setMessage(e.target.value)}
            placeholder="Type your message... | अपना संदेश लिखें..."
            className="flex-1 outline-none text-sm text-slate-800 bg-transparent"
            onKeyDown={(e) => e.key === "Enter" && handleSend()}
          />
          <button
            onClick={handleSend}
            className="p-2 bg-blue-500 hover:bg-blue-600 active:scale-95 text-white rounded-full transition-all flex items-center justify-center flex-shrink-0"
          >
            <Send className="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>
  );
}
